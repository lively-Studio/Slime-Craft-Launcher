/*
 * Slime Craft Launcher
 * Copyright (C) 2020  lively-Studio <X_CODER_ocs_2026@126.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package studio.lively.scl.util.platform;

import studio.lively.scl.launch.StreamPump;
import studio.lively.scl.util.Lang;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static studio.lively.scl.util.logging.Logger.LOG;

/// The managed process.
///
/// @author lively-Studio
/// <!-- @see studio.lively.scl.launch.ExitWaiter -->
/// @see studio.lively.scl.launch.StreamPump
public final class ManagedProcess {

    /// Maximum retry count when posix_spawn fails.
    /// Reduced to 1 now that fork/exec fallback is in place.
    private static final int POSIX_SPAWN_MAX_RETRIES = 1;
    /// Delay between retry attempts in milliseconds.
    private static final long POSIX_SPAWN_RETRY_DELAY_MS = 200;

    private final ReentrantLock lock = new ReentrantLock();
    private final Process process;
    private final List<String> commands;
    private final String classpath;
    private final Map<String, Object> properties = new HashMap<>();
    private final List<String> lines = new ArrayList<>();
    private final List<Thread> relatedThreads = new ArrayList<>();

    /// Launches a process with auto-retry and fork/exec fallback.
    /// Use this instead of ``ProcessBuilder.start()`` to survive
    /// defective JDK builds where posix_spawn is broken.
    public static Process launchProcess(ProcessBuilder builder) throws IOException {
        return startWithRetry(builder);
    }

    public ManagedProcess(ProcessBuilder processBuilder) throws IOException {
        this.process = launchProcess(processBuilder);
        this.commands = processBuilder.command();
        this.classpath = null;
    }

    /// Starts the process with automatic retry. When posix_spawn keeps
    /// failing, falls back to native fork/exec via JNA bypass.
    private static Process startWithRetry(ProcessBuilder processBuilder) throws IOException {
        IOException lastException = null;
        boolean isPosixSpawn = false;
        for (int attempt = 0; attempt < POSIX_SPAWN_MAX_RETRIES; attempt++) {
            try {
                return processBuilder.start();
            } catch (IOException e) {
                lastException = e;
                String msg = e.getMessage();
                if (msg != null && msg.contains("posix_spawn")) {
                    isPosixSpawn = true;
                    if (attempt < POSIX_SPAWN_MAX_RETRIES - 1) {
                        LOG.warning("posix_spawn failed (attempt " + (attempt + 1)
                                + "/" + POSIX_SPAWN_MAX_RETRIES + "), retrying in "
                                + POSIX_SPAWN_RETRY_DELAY_MS + "ms: " + e.getLocalizedMessage());
                        try {
                            TimeUnit.MILLISECONDS.sleep(POSIX_SPAWN_RETRY_DELAY_MS);
                        } catch (InterruptedException ignored) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                } else {
                    // Not a posix_spawn error, don't retry
                    break;
                }
            }
        }

        // posix_spawn is broken — try native fork/exec bypass via JNA
        if (isPosixSpawn && PosixSpawnFallback.isAvailable()) {
            // fork/exec with inheritIO is unsafe: child inherits parent fds,
            // but JVM state can cause deadlocks. Skip fallback for inheritIO.
            if (processBuilder.redirectOutput() == ProcessBuilder.Redirect.INHERIT
                    || processBuilder.redirectError() == ProcessBuilder.Redirect.INHERIT) {
                LOG.warning("Skipping fork/exec fallback for inheritIO process.");
            } else {
                LOG.warning("posix_spawn retries exhausted. "
                        + "Attempting native fork/exec fallback to bypass JVM bug.");
                try {
                    return PosixSpawnFallback.start(processBuilder.command(),
                            processBuilder.directory() != null ? processBuilder.directory().toPath() : null);
                } catch (IOException fallbackError) {
                    lastException.addSuppressed(fallbackError);
                }
            }
        }

        throw lastException != null ? lastException : new IOException("Process start failed");
    }

    /**
     * Constructor.
     *
     * @param process  the raw system process that this instance manages.
     * @param commands the command line of {@code process}.
     */
    public ManagedProcess(Process process, List<String> commands) {
        this.process = process;
        this.commands = List.copyOf(commands);
        this.classpath = null;
    }

    /**
     * Constructor.
     *
     * @param process   the raw system process that this instance manages.
     * @param commands  the command line of {@code process}.
     * @param classpath the classpath of java process
     */
    public ManagedProcess(Process process, List<String> commands, String classpath) {
        this.process = process;
        this.commands = List.copyOf(commands);
        this.classpath = classpath;
    }

    /**
     * The raw system process that this instance manages.
     *
     * @return process
     */
    public Process getProcess() {
        return process;
    }

    /**
     * The command line.
     *
     * @return the list of each part of command line separated by spaces.
     */
    public List<String> getCommands() {
        return commands;
    }

    /**
     * The classpath.
     *
     * @return classpath
     */
    public String getClasspath() {
        return classpath;
    }

    /**
     * To save some information you need.
     */
    public Map<String, Object> getProperties() {
        return properties;
    }

    /**
     * The (unmodifiable) standard output/error lines.
     * If you want to add lines, use {@link #addLine}
     *
     * @see #addLine
     */
    public List<String> getLines(Predicate<String> lineFilter) {
        lock.lock();
        try {
            if (lineFilter == null)
                return List.copyOf(lines);

            ArrayList<String> res = new ArrayList<>();
            for (String line : this.lines) {
                if (lineFilter.test(line))
                    res.add(line);
            }
            return Collections.unmodifiableList(res);
        } finally {
            lock.unlock();
        }
    }

    public void addLine(String line) {
        lock.lock();
        try {
            lines.add(line);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Add related thread.
     * <p>
     * If a thread is monitoring this raw process,
     * you are required to add the instance by this method.
     */
    public void addRelatedThread(Thread thread) {
        lock.lock();
        try {
            relatedThreads.add(thread);
        } finally {
            lock.unlock();
        }
    }

    public void pumpInputStream(Consumer<String> onLogLine) {
        addRelatedThread(Lang.thread(new StreamPump(process.getInputStream(), onLogLine, OperatingSystem.NATIVE_CHARSET), "ProcessInputStreamPump", true));
    }

    public void pumpErrorStream(Consumer<String> onLogLine) {
        addRelatedThread(Lang.thread(new StreamPump(process.getErrorStream(), onLogLine, OperatingSystem.NATIVE_CHARSET), "ProcessErrorStreamPump", true));
    }

    /**
     * True if the managed process is running.
     */
    public boolean isRunning() {
        try {
            process.exitValue();
            return false;
        } catch (IllegalThreadStateException e) {
            return true;
        }
    }

    /**
     * The exit code of raw process.
     */
    public int getExitCode() {
        return process.exitValue();
    }

    /**
     * Destroys the raw process and other related threads that are monitoring this raw process.
     */
    public void stop() {
        process.destroy();
        destroyRelatedThreads();
    }

    public void destroyRelatedThreads() {
        lock.lock();
        try {
            relatedThreads.forEach(Thread::interrupt);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String toString() {
        return "ManagedProcess[commands=" + commands + ", isRunning=" + isRunning() + "]";
    }

}
