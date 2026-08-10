/*
 * Slime Craft Launcher
 * Copyright (C) 2021  lively-Studio <X_CODER_ocs_2026@126.com> and contributors
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
package studio.lively.scl;

import studio.lively.scl.util.StringUtils;
import studio.lively.scl.util.io.JarUtils;
import studio.lively.scl.util.platform.Architecture;
import studio.lively.scl.util.platform.OperatingSystem;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.EnumSet;

/**
 * Stores metadata about this application.
 *
 * <h3>Version Format (since 2026)</h3>
 * <pre>{@code
 * DEV{year}.{major}.{minor}-SNAPSHOT-{build}  (development)
 * V{major}.{minor}.{patch}                     (stable)
 * }</pre>
 * The prefix identifies the version type:
 * <ul>
 *   <li><b>DEV</b> — Development version (开发版)</li>
 *   <li><b>V</b>    — Stable/regular release (普通版)</li>
 * </ul>
 * <h4>DEV segments</h4>
 * <table>
 *   <tr><td>DEV</td><td>Development version (开发版)</td></tr>
 *   <tr><td>{year}</td><td>Development year (开发时间-年)</td></tr>
 *   <tr><td>{major}</td><td>Major version of the year (该年第几版)</td></tr>
 *   <tr><td>{minor}</td><td>Minor version, 0 if none (没有小版本信息)</td></tr>
 *   <tr><td>SNAPSHOT</td><td>Advanced development build (高级开发版)</td></tr>
 *   <tr><td>{build}</td><td>Build package version (高级开发版包版本号)</td></tr>
 * </table>
 * <h4>V segments (stable)</h4>
 * <table>
 *   <tr><td>V</td><td>Stable release (普通版)</td></tr>
 *   <tr><td>{major}</td><td>Major version</td></tr>
 *   <tr><td>{minor}</td><td>Minor version</td></tr>
 *   <tr><td>{patch}</td><td>Patch version</td></tr>
 * </table>
 * <p>Example DEV: {@code DEV2026.1.0-SNAPSHOT-2}</p>
 * <p>Example V:   {@code V3.17.1}</p>
 */
public final class Metadata {
    private Metadata() {
    }

    public static final String NAME = "SCL";
    public static final String FULL_NAME = "Slime Craft Launcher";

    /// Version format: DEV{year}.{major}.{minor}-SNAPSHOT-{build} or V{major}.{minor}.{patch}
    /// See class-level Javadoc for detailed explanation of each segment.
    public static final String VERSION = System.getProperty("scl.version.override", JarUtils.getAttribute("scl.version", "@develop@"));

    public static final String TITLE = NAME + " " + VERSION;
    public static final String FULL_TITLE = FULL_NAME + " v" + VERSION;

    public static final int MINIMUM_REQUIRED_JAVA_VERSION = 17;
    public static final int MINIMUM_SUPPORTED_JAVA_VERSION = 17;
    public static final int RECOMMENDED_JAVA_VERSION = 21;

    public static final String MANUAL_UPDATE_URL = "https://github.com/lively-Studio/Slime-Craft-Launcher/releases";

    public static final String GROUPS_URL = "https://qm.qq.com/q/RGrOTIdLq4";

    public static final String BUILD_CHANNEL = JarUtils.getAttribute("scl.version.type", "nightly");
    public static final String GITHUB_SHA = JarUtils.getAttribute("scl.version.hash", null);

    public static final Path CURRENT_DIRECTORY = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
    public static final Path MINECRAFT_DIRECTORY = OperatingSystem.getWorkingDirectory("minecraft");
    public static final Path SCL_USER_HOME;
    public static final Path SCL_LOCAL_HOME;
    public static final Path DEPENDENCIES_DIRECTORY;

    static {
        String sclHome = System.getProperty("scl.home", System.getenv("SCL_USER_HOME"));
        if (StringUtils.isBlank(sclHome)) {
            if (OperatingSystem.CURRENT_OS.isLinuxOrBSD()) {
                String xdgData = System.getenv("XDG_DATA_HOME");
                if (StringUtils.isNotBlank(xdgData)) {
                    SCL_USER_HOME = Path.of(xdgData, "scl").toAbsolutePath().normalize();
                } else {
                    SCL_USER_HOME = Path.of(System.getProperty("user.home"), ".local", "share", "scl").toAbsolutePath().normalize();
                }
            } else {
                SCL_USER_HOME = OperatingSystem.getWorkingDirectory("scl");
            }
        } else {
            SCL_USER_HOME = Path.of(sclHome).toAbsolutePath().normalize();
        }

        String sclCurrentDir = System.getProperty("scl.dir", System.getenv("SCL_LOCAL_HOME"));
        SCL_LOCAL_HOME = StringUtils.isNotBlank(sclCurrentDir)
                ? Path.of(sclCurrentDir).toAbsolutePath().normalize()
                : CURRENT_DIRECTORY.resolve(".scl");

        String sclDependencies = System.getProperty("scl.dependencies.dir", System.getenv("SCL_DEPENDENCIES_DIR"));
        DEPENDENCIES_DIRECTORY = StringUtils.isNotBlank(sclDependencies)
                ? Path.of(sclDependencies).toAbsolutePath().normalize()
                : SCL_LOCAL_HOME.resolve("dependencies");
    }

    public static boolean isStable() {
        return "stable".equals(BUILD_CHANNEL);
    }

    public static boolean isDev() {
        return "dev".equals(BUILD_CHANNEL);
    }

    public static boolean isNightly() {
        return !isStable() && !isDev();
    }

    public static @Nullable String getSuggestedJavaDownloadLink() {
        if (OperatingSystem.CURRENT_OS == OperatingSystem.LINUX && Architecture.SYSTEM_ARCH == Architecture.LOONGARCH64_OW)
            return "https://www.loongnix.cn/zh/api/java/downloads-jdk21/index.html";
        else {
            EnumSet<Architecture> supportedArchitectures;
            if (OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS)
                supportedArchitectures = EnumSet.of(Architecture.X86_64, Architecture.X86, Architecture.ARM64);
            else if (OperatingSystem.CURRENT_OS == OperatingSystem.LINUX)
                supportedArchitectures = EnumSet.of(
                        Architecture.X86_64, Architecture.X86,
                        Architecture.ARM64, Architecture.ARM32,
                        Architecture.RISCV64, Architecture.LOONGARCH64
                );
            else if (OperatingSystem.CURRENT_OS == OperatingSystem.MACOS)
                supportedArchitectures = EnumSet.of(Architecture.X86_64, Architecture.ARM64);
            else
                supportedArchitectures = EnumSet.noneOf(Architecture.class);
            if (supportedArchitectures.contains(Architecture.SYSTEM_ARCH))
                return null;
            else
                return null;
        }
    }
}
