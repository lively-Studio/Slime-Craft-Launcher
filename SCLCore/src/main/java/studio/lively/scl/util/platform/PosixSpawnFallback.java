package studio.lively.scl.util.platform;

import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.*;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/// Fallback process launcher using native ``fork()``/``execvp()``
/// when the JVM's ``posix_spawn`` implementation is broken.
///
/// Uses JNA to call POSIX APIs directly, bypassing ``ProcessBuilder.start()``.
/// Only effective on macOS / Linux.
public final class PosixSpawnFallback {

    private static final boolean AVAILABLE;
    static {
        boolean ok = false;
        try {
            if (OperatingSystem.CURRENT_OS != OperatingSystem.WINDOWS) {
                CLib.INSTANCE.getpid();
                ok = true;
            }
        } catch (Throwable ignored) {
        }
        AVAILABLE = ok;
    }

    public static boolean isAvailable() {
        return AVAILABLE;
    }

    /// Starts a process using ``fork()`` + ``execvp()``, bypassing posix_spawn.
    /// Stdout and stderr are captured via pipes and fed into the returned Process.
    public static Process start(List<String> command, Path directory) throws IOException {
        if (!AVAILABLE) {
            throw new IOException("PosixSpawnFallback not available");
        }

        String[] argv = command.toArray(new String[0]);

        // Pre-compute directory path BEFORE fork() to avoid heap allocations in child.
        // malloc/new in child after fork() can deadlock if JVM heap locks are held.
        String dirPath = directory != null ? directory.toString() : null;

        // Create pipes for stdout and stderr  
        IntByReference outPipe = new IntByReference();
        IntByReference errPipe = new IntByReference();
        if (CLib.INSTANCE.pipe(outPipe) != 0) {
            throw new IOException("pipe(stdout) failed: " + getLastError());
        }
        if (CLib.INSTANCE.pipe(errPipe) != 0) {
            closeFd(outPipe.getValue()); closeFd(outPipe.getValue() + 1);
            throw new IOException("pipe(stderr) failed: " + getLastError());
        }

        int pid = CLib.INSTANCE.fork();
        if (pid < 0) {
            closeFd(outPipe.getValue()); closeFd(outPipe.getValue() + 1);
            closeFd(errPipe.getValue()); closeFd(errPipe.getValue() + 1);
            throw new IOException("fork() failed: " + getLastError());
        }

        // Pre-allocate native strings BEFORE fork().
        // JNA memory allocations after fork() can deadlock if JVM heap locks are held.
        Memory nativeDirPath = null;
        if (dirPath != null) {
            nativeDirPath = new Memory(dirPath.length() + 1);
            nativeDirPath.setString(0, dirPath);
        }

        // Pre-allocate execvp arguments as native Memory
        Memory nativeProg = new Memory(argv[0].length() + 1);
        nativeProg.setString(0, argv[0]);
        Memory[] nativeArgs = new Memory[argv.length];
        Pointer[] nativeArgv = new Pointer[argv.length + 1];
        for (int i = 0; i < argv.length; i++) {
            nativeArgs[i] = new Memory(argv[i].length() + 1);
            nativeArgs[i].setString(0, argv[i]);
            nativeArgv[i] = nativeArgs[i];
        }
        nativeArgv[argv.length] = null; // null terminator

        if (pid == 0) {
            // === Child (NO Java heap allocations beyond this point) ===
            if (nativeDirPath != null) {
                CLib.INSTANCE.chdirByPointer(nativeDirPath);
            }
            CLib.INSTANCE.dup2(outPipe.getValue() + 1, 1);
            CLib.INSTANCE.dup2(errPipe.getValue() + 1, 2);
            closeFd(outPipe.getValue()); closeFd(outPipe.getValue() + 1);
            closeFd(errPipe.getValue()); closeFd(errPipe.getValue() + 1);
            CLib.INSTANCE.execv(nativeProg, nativeArgv);
            CLib.INSTANCE._exit(127);
        }

        // === Parent ===
        int outReadFd = outPipe.getValue();
        int errReadFd = errPipe.getValue();
        closeFd(outPipe.getValue() + 1); // close write ends
        closeFd(errPipe.getValue() + 1);

        // Create non-blocking streams: threads read from pipe fds and
        // feed into queue-based InputStreams. No PipedStream deadlock.
        QueueInputStream outStream = new QueueInputStream();
        QueueInputStream errStream = new QueueInputStream();
        QueueInputStream.QueueOutputStream outQueue = outStream.getOutputStream();
        QueueInputStream.QueueOutputStream errQueue = errStream.getOutputStream();

        Thread outThread = new Thread(() -> pumpFd(outReadFd, outQueue), "fork-stdout-pump");
        Thread errThread = new Thread(() -> pumpFd(errReadFd, errQueue), "fork-stderr-pump");
        outThread.setDaemon(true);
        errThread.setDaemon(true);
        outThread.start();
        errThread.start();

        return new ForkedProcess(pid, outStream, errStream, outReadFd, errReadFd, outQueue, errQueue, outThread, errThread);
    }

    /// Reads from a native fd and feeds into the queue-based stream.
    private static void pumpFd(int fd, QueueInputStream.QueueOutputStream out) {
        byte[] buf = new byte[8192];
        try {
            while (true) {
                int n = CLib.INSTANCE.read(fd, buf, buf.length);
                if (n <= 0) break;
                out.write(buf, 0, n);
            }
        } catch (Throwable ignored) {
        } finally {
            out.close();
        }
    }

    private static int getLastError() {
        return Native.getLastError();
    }

    private static void closeFd(int fd) {
        if (fd > 2) CLib.INSTANCE.close(fd);
    }

    private static class ForkedProcess extends Process {
        private final int pid;
        private final InputStream stdout, stderr;
        private final int outFd, errFd;
        private final QueueInputStream.QueueOutputStream outQueue, errQueue;
        private final Thread outThread, errThread;
        private final AtomicBoolean exited = new AtomicBoolean(false);
        private int exitCode = -1;

        ForkedProcess(int pid, InputStream stdout, InputStream stderr,
                      int outFd, int errFd,
                      QueueInputStream.QueueOutputStream outQueue, QueueInputStream.QueueOutputStream errQueue,
                      Thread outThread, Thread errThread) {
            this.pid = pid;
            this.stdout = stdout;
            this.stderr = stderr;
            this.outFd = outFd;
            this.errFd = errFd;
            this.outQueue = outQueue;
            this.errQueue = errQueue;
            this.outThread = outThread;
            this.errThread = errThread;
        }

        @Override public OutputStream getOutputStream() { return OutputStream.nullOutputStream(); }
        @Override public InputStream getInputStream() { return stdout; }
        @Override public InputStream getErrorStream() { return stderr; }

        @Override
        public int waitFor() throws InterruptedException {
            IntByReference status = new IntByReference();
            CLib.INSTANCE.waitpid(pid, status, 0);
            finish(status.getValue());
            return exitCode;
        }

        @Override
        public boolean waitFor(long timeout, TimeUnit unit) throws InterruptedException {
            long deadline = System.nanoTime() + unit.toNanos(timeout);
            while (System.nanoTime() < deadline) {
                IntByReference s = new IntByReference();
                if (CLib.INSTANCE.waitpid(pid, s, 1) > 0) { finish(s.getValue()); return true; }
                Thread.sleep(10);
            }
            return false;
        }

        @Override public int exitValue() {
            if (!exited.get()) throw new IllegalThreadStateException("not exited");
            return exitCode;
        }

        @Override public void destroy() { CLib.INSTANCE.kill(pid, 15); }
        @Override public Process destroyForcibly() { CLib.INSTANCE.kill(pid, 9); return this; }
        @Override public long pid() { return pid; }
        @Override public java.lang.ProcessHandle.Info info() { return java.lang.ProcessHandle.of(pid).map(h -> h.info()).orElse(null); }
        @Override public java.lang.ProcessHandle toHandle() { return java.lang.ProcessHandle.of(pid).orElseThrow(); }
        @Override public boolean supportsNormalTermination() { return true; }

        @Override
        public boolean isAlive() {
            if (exited.get()) return false;
            IntByReference s = new IntByReference();
            if (CLib.INSTANCE.waitpid(pid, s, 1) > 0) { finish(s.getValue()); return false; }
            return true;
        }

        private void finish(int status) {
            if (!exited.compareAndSet(false, true)) return;
            if (CLib.WIFEXITED(status)) exitCode = CLib.WEXITSTATUS(status);
            else if (CLib.WIFSIGNALED(status)) exitCode = 128 + CLib.WTERMSIG(status);
            else exitCode = 1;
            outQueue.close();
            errQueue.close();
            closeFd(outFd);
            closeFd(errFd);
        }
    }

    /// A non-blocking InputStream fed from a background thread.
    /// Unlike PipedInputStream, write() never blocks — data is
    /// queued in memory. This prevents deadlocks when the consumer
    /// hasn't attached yet.
    private static class QueueInputStream extends InputStream {
        private final ConcurrentLinkedQueue<byte[]> queue = new ConcurrentLinkedQueue<>();
        private byte[] current;
        private int pos;
        private volatile boolean closed;

        QueueOutputStream getOutputStream() { return new QueueOutputStream(); }

        class QueueOutputStream extends OutputStream {
            @Override public void write(int b) { write(new byte[]{(byte) b}, 0, 1); }
            @Override public void write(byte[] b, int off, int len) {
                if (closed || len <= 0) return;
                byte[] copy = new byte[len];
                System.arraycopy(b, off, copy, 0, len);
                queue.offer(copy);
            }
            @Override public void close() {
                closed = true;
            }
        }

        @Override
        public int read() throws IOException {
            byte[] b = new byte[1];
            int n = read(b, 0, 1);
            return n < 0 ? -1 : b[0] & 0xff;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (current == null || pos >= current.length) {
                while ((current = queue.poll()) == null) {
                    if (closed) return -1;
                    try { Thread.sleep(5); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return -1; }
                }
                pos = 0;
            }
            int n = Math.min(len, current.length - pos);
            System.arraycopy(current, pos, b, off, n);
            pos += n;
            return n;
        }

        @Override
        public int available() {
            int total = 0;
            for (byte[] chunk : queue) total += chunk.length;
            if (current != null) total += current.length - pos;
            return total;
        }
    }

    /// POSIX C library via JNA
    public interface CLib extends Library {
        CLib INSTANCE = Native.load("c", CLib.class);

        int getpid();
        int fork();
        int execv(Pointer path, Pointer[] argv);
        int pipe(IntByReference pipefd);
        int dup2(int oldfd, int newfd);
        int close(int fd);
        int chdirByPointer(Pointer path);
        int waitpid(int pid, IntByReference status, int options);
        int kill(int pid, int sig);
        int read(int fd, byte[] buf, int count);
        void _exit(int status);
        String strerror(int errnum);

        static boolean WIFEXITED(int s) { return (s & 0x7f) == 0; }
        static int WEXITSTATUS(int s) { return (s >> 8) & 0xff; }
        static boolean WIFSIGNALED(int s) { return ((s & 0x7f) + 1) >> 1 > 0; }
        static int WTERMSIG(int s) { return s & 0x7f; }
    }
}
