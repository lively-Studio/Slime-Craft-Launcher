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
package studio.lively.scl.upgrade;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.*;
import javafx.beans.value.ObservableBooleanValue;
import studio.lively.scl.Metadata;
import studio.lively.scl.util.versioning.VersionNumber;

import java.io.IOException;

import static studio.lively.scl.setting.SettingsManager.settings;
import static studio.lively.scl.util.Lang.*;
import static studio.lively.scl.util.logging.Logger.LOG;

public final class UpdateChecker {
    private UpdateChecker() {
    }

    private static final ObjectProperty<RemoteVersion> latestVersion = new SimpleObjectProperty<>();
    private static final BooleanBinding outdated = Bindings.createBooleanBinding(
            () -> {
                RemoteVersion latest = latestVersion.get();
                if (latest == null) {
                    return false;
                }

                // Channel-based filtering: development versions match DEV/DEV,
                // stable versions match V/V. Cross-channel requires NIGHTLY.
                boolean currentDev = isDevelopmentVersion(Metadata.VERSION);
                boolean latestDev = isDevelopmentVersion(latest.version());
                UpdateChannel channel = UpdateChannel.getChannel();

                // Same type always OK (DEV→DEV or V→V)
                if (currentDev == latestDev) {
                    // pass
                }
                // Cross-type: only when channel is NIGHTLY (preview/dev)
                else if (channel != UpdateChannel.NIGHTLY) {
                    return false;
                }

                if (latest.force()
                        || Metadata.isNightly()
                        || latest.channel() == UpdateChannel.NIGHTLY
                        || latest.channel() != channel) {
                    return !latest.version().equals(Metadata.VERSION);
                } else {
                    return VersionNumber.compare(Metadata.VERSION, latest.version()) < 0;
                }
            },
            latestVersion);
    private static final ReadOnlyBooleanWrapper checkingUpdate = new ReadOnlyBooleanWrapper(false);

    public static void init() {
        requestCheckUpdate(UpdateChannel.getChannel(), settings().acceptPreviewUpdateProperty().get());
    }

    public static RemoteVersion getLatestVersion() {
        return latestVersion.get();
    }

    public static ReadOnlyObjectProperty<RemoteVersion> latestVersionProperty() {
        return latestVersion;
    }

    public static boolean isOutdated() {
        return outdated.get();
    }

    public static ObservableBooleanValue outdatedProperty() {
        return outdated;
    }

    public static boolean isCheckingUpdate() {
        return checkingUpdate.get();
    }

    public static ReadOnlyBooleanProperty checkingUpdateProperty() {
        return checkingUpdate.getReadOnlyProperty();
    }

    private static RemoteVersion checkUpdate(UpdateChannel channel, boolean preview) throws IOException {
        throw new IOException("Update checking disabled");
    }

    /// Returns true if the version string indicates a development build.
    /// Development versions start with "DEV" (e.g. "DEV2026.1.0-SNAPSHOT-2"),
    /// or contain "@" (unbuilt, e.g. "@develop@").
    private static boolean isDevelopmentVersion(String version) {
        return version.contains("@")  // eg. @develop@
                || version.startsWith("DEV"); // eg. DEV2026.1.0-SNAPSHOT-2
    }

    /// Returns true if the version string indicates a stable release.
    /// Stable versions start with "V" (e.g. "V3.17.1").
    private static boolean isStableVersion(String version) {
        return version.startsWith("V");
    }

    public static void requestCheckUpdate(UpdateChannel channel, boolean preview) {
        Platform.runLater(() -> {
            if (isCheckingUpdate())
                return;
            checkingUpdate.set(true);

            thread(() -> {
                RemoteVersion result = null;
                try {
                    result = checkUpdate(channel, preview);
                    LOG.info("Latest version (" + channel + ", preview=" + preview + ") is " + result);
                } catch (Throwable e) {
                    LOG.warning("Failed to check for update", e);
                }

                RemoteVersion finalResult = result;
                Platform.runLater(() -> {
                    if (finalResult != null) {
                        latestVersion.set(finalResult);
                    }
                    checkingUpdate.set(false);
                });
            }, "Update Checker", true);
        });
    }
}
