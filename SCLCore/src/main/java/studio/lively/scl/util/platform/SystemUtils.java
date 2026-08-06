/*
 * Slime Craft Launcher
 * Copyright (C) 2020  lively-Studio <X_CODER_ocs2008@126.com> and contributors
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

import studio.lively.scl.task.Schedulers;
import studio.lively.scl.util.Lang;
import studio.lively.scl.util.function.ExceptionalFunction;
import studio.lively.scl.util.io.IOUtils;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static studio.lively.scl.util.logging.Logger.LOG;
import java.util.concurrent.TimeoutException;

import static studio.lively.scl.util.logging.Logger.LOG;

public final class SystemUtils {
    private SystemUtils() {
    }

    public static @Nullable Path which(String command) {
        String path = System.getenv("PATH");
        if (path == null)
            return null;

        try {
            for (String item : path.split(File.pathSeparator)) {
                try {
                    Path program = Paths.get(item, command);
                    if (Files.isExecutable(program))
                        return program.toRealPath();
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }

        return null;
    }

    public static int callExternalProcess(String... command) throws IOException, InterruptedException {
        return callExternalProcess(Arrays.asList(command));
    }

    public static int callExternalProcess(List<String> command) throws IOException, InterruptedException {
        return callExternalProcess(new ProcessBuilder(command));
    }

    public static int callExternalProcess(ProcessBuilder processBuilder) throws IOException, InterruptedException {
        ManagedProcess managedProcess = new ManagedProcess(processBuilder);
        managedProcess.pumpInputStream(SystemUtils::onLogLine);
        managedProcess.pumpErrorStream(SystemUtils::onLogLine);
        return managedProcess.getProcess().waitFor();
    }

    private static final Duration DEFAULT_MAX_WAIT_TIME = Duration.ofSeconds(15);

    public static String run(String... command) throws Exception {
        return run(List.of(command), DEFAULT_MAX_WAIT_TIME);
    }

    public static String run(List<String> command, Duration maxWaitTime) throws Exception {
        return run(command, inputStream -> IOUtils.readFullyAsString(inputStream, OperatingSystem.NATIVE_CHARSET), maxWaitTime);
    }

    public static <T> T run(List<String> command, ExceptionalFunction<InputStream, T, ?> convert) throws Exception {
        return run(command, convert, DEFAULT_MAX_WAIT_TIME);
    }

    /// Maximum retries when posix_spawn fails.
    /// Reduced to 1 now that fork/exec fallback is in place.
    private static final int POSIX_SPAWN_MAX_RETRIES = 1;
    /// Delay between retries in milliseconds.
    private static final long POSIX_SPAWN_RETRY_DELAY_MS = 200;

    private static Process startWithRetry(ProcessBuilder builder) throws IOException {
        IOException lastException = null;
        boolean isPosixSpawn = false;
        for (int attempt = 0; attempt < POSIX_SPAWN_MAX_RETRIES; attempt++) {
            try {
                return builder.start();
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
                    break;
                }
            }
        }

        if (isPosixSpawn && PosixSpawnFallback.isAvailable()) {
            LOG.warning("posix_spawn retries exhausted. "
                    + "Attempting native fork/exec fallback.");
            try {
                return PosixSpawnFallback.start(builder.command(),
                        builder.directory() != null ? builder.directory().toPath() : null);
            } catch (IOException fallbackError) {
                lastException.addSuppressed(fallbackError);
            }
        }

        throw lastException != null ? lastException : new IOException("Process start failed");
    }

    public static <T> T run(List<String> command, ExceptionalFunction<InputStream, T, ?> convert, Duration maxWaitTime) throws Exception {
        Process process = startWithRetry(new ProcessBuilder(command)
                .redirectError(ProcessBuilder.Redirect.DISCARD));
        try {
            InputStream inputStream = process.getInputStream();
            CompletableFuture<T> future = CompletableFuture.supplyAsync(
                    Lang.wrap(() -> convert.apply(inputStream)),
                    Schedulers.io());

            if (!process.waitFor(maxWaitTime.toMillis(), TimeUnit.MILLISECONDS))
                throw new TimeoutException();

            if (process.exitValue() != 0)
                throw new IOException("Bad exit code: " + process.exitValue());

            return future.get();
        } finally {
            if (process.isAlive())
                process.destroy();
        }
    }

    public static boolean supportJVMAttachment() {
        return Thread.currentThread().getContextClassLoader().getResource("com/sun/tools/attach/VirtualMachine.class") != null;
    }

    public static void onLogLine(String log) {
        LOG.info(log);
    }
}
