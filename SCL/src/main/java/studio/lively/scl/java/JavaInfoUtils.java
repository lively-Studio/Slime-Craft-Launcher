/*
 * Slime Craft Launcher
 * Copyright (C) 2025 lively-Studio <X_CODER_ocs2008@126.com> and contributors
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
package studio.lively.scl.java;

import com.google.gson.annotations.SerializedName;
import studio.lively.scl.util.gson.JsonSerializable;
import studio.lively.scl.util.gson.JsonUtils;
import studio.lively.scl.util.io.JarUtils;
import studio.lively.scl.util.platform.Architecture;
import studio.lively.scl.util.platform.OperatingSystem;
import studio.lively.scl.util.platform.Platform;
import studio.lively.scl.util.platform.SystemUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static studio.lively.scl.util.logging.Logger.LOG;

/**
 * @author Glavo
 * @see <a href="https://github.com/Glavo/java-info">Glavo/java-info</a>
 */
public final class JavaInfoUtils {

    private JavaInfoUtils() {
    }

    /// Auto-detects whether the current JVM can spawn child processes.
    /// On some JDK builds (e.g., certain Zulu/Microsoft builds on macOS),
    /// posix_spawn fails intermittently. When broken, we fall back to
    /// reading the ``release`` file instead of spawning the target JVM.
    ///
    /// The detection is cached so we only test once per JVM lifetime.
    private static volatile Boolean canSpawnProcess;

    /// Tests whether the current JVM can spawn child processes.
    /// On some JDK builds (e.g., certain Zulu builds on macOS),
    /// posix_spawn fails, making any child process impossible.
    /// When this returns false, no Minecraft instance can be launched.
    public static boolean canSpawnProcess() {
        if (canSpawnProcess != null) {
            return canSpawnProcess;
        }
        try {
            ProcessBuilder pb = new ProcessBuilder("true");
            Process p = pb.start();
            int exitCode = p.waitFor();
            canSpawnProcess = exitCode == 0;
        } catch (Throwable e) {
            LOG.warning("posix_spawn detection failed: " + e + ". Will use release file fallback for Java detection.");
            canSpawnProcess = false;
        }
        return canSpawnProcess;
    }

    public static @NotNull JavaInfo fromExecutable(Path executable) throws IOException {
        assert executable.isAbsolute();

        Path thisPath = JarUtils.thisJarPath();
        if (thisPath == null) {
            throw new IOException("Failed to find current SCL location");
        }

        // Try process-based detection first (faster and more accurate)
        if (canSpawnProcess()) {
            try {
                Result result = JsonUtils.GSON.fromJson(SystemUtils.run(
                        executable.toString(),
                        "-classpath",
                        thisPath.toString(),
                        org.glavo.info.Main.class.getName()
                ), Result.class);

                if (result != null && result.javaVersion != null && result.osArch != null) {
                    Architecture architecture = Architecture.parseArchName(result.osArch);
                    Platform platform = Platform.getPlatform(OperatingSystem.CURRENT_OS,
                            architecture != Architecture.UNKNOWN
                                    ? architecture
                                    : Architecture.SYSTEM_ARCH);
                    return new JavaInfo(platform, result.javaVersion, result.javaVendor);
                }
            } catch (Exception e) {
                LOG.warning("Process-based Java detection failed for " + executable
                        + ": " + e + ". Falling back to release file.");
            }
        }

        // Fallback: read the JDK release file
        return fromReleaseFile(executable);
    }

    /// Reads Java version info from the ``release`` file located relative
    /// to the given executable.
    ///
    /// Supports these layouts:
    /// - ``$JAVA_HOME/bin/java`` → ``$JAVA_HOME/release`` (Linux, standard JDK)
    /// - ``$JAVA_HOME/Contents/Home/bin/java`` → ``$JAVA_HOME/Contents/Home/release`` (macOS)
    static @NotNull JavaInfo fromReleaseFile(Path executable) throws IOException {
        Path home = executable.getParent(); // .../bin
        if (home != null && "bin".equals(home.getFileName().toString())) {
            home = home.getParent(); // potential JAVA_HOME
            if (home != null) {
                Path releaseFile = home.resolve("release");
                if (Files.isRegularFile(releaseFile)) {
                    return parseReleaseFile(releaseFile, executable);
                }
            }
        }

        throw new IOException("Cannot determine Java info: no release file found at "
                + executable + " and process spawning is unavailable");
    }

    private static @NotNull JavaInfo parseReleaseFile(Path releaseFile, Path executable) throws IOException {
        Properties props = new Properties();
        try (Reader reader = Files.newBufferedReader(releaseFile)) {
            props.load(reader);
        }

        String javaVersion = props.getProperty("JAVA_VERSION");
        if (javaVersion == null || javaVersion.isEmpty()) {
            throw new IOException("JAVA_VERSION not found in " + releaseFile);
        }
        // Strip surrounding quotes
        if (javaVersion.startsWith("\"") && javaVersion.endsWith("\"")) {
            javaVersion = javaVersion.substring(1, javaVersion.length() - 1);
        }

        String osArch = props.getProperty("OS_ARCH");
        if (osArch != null && osArch.startsWith("\"") && osArch.endsWith("\"")) {
            osArch = osArch.substring(1, osArch.length() - 1);
        }

        String javaVendor = props.getProperty("IMPLEMENTOR");
        if (javaVendor != null && javaVendor.startsWith("\"") && javaVendor.endsWith("\"")) {
            javaVendor = javaVendor.substring(1, javaVendor.length() - 1);
        }

        Architecture architecture = osArch != null
                ? Architecture.parseArchName(osArch)
                : Architecture.SYSTEM_ARCH;
        Platform platform = Platform.getPlatform(OperatingSystem.CURRENT_OS,
                architecture != Architecture.UNKNOWN
                        ? architecture
                        : Architecture.SYSTEM_ARCH);

        LOG.info("Detected Java info from release file " + releaseFile
                + ": version=" + javaVersion + ", arch=" + osArch + ", vendor=" + javaVendor);

        return new JavaInfo(platform, javaVersion, javaVendor != null ? javaVendor : "");
    }

    @JsonSerializable
    private record Result(@SerializedName("os.name") String osName, @SerializedName("os.arch") String osArch,
                          @SerializedName("java.version") String javaVersion,
                          @SerializedName("java.vendor") String javaVendor) {
    }
}
