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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.*;
import javafx.beans.value.ObservableBooleanValue;
import org.jetbrains.annotations.Nullable;
import studio.lively.scl.Metadata;
import studio.lively.scl.util.gson.JsonUtils;
import studio.lively.scl.util.io.NetworkUtils;
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

    /// GitHub releases API endpoint for the SCL repository.
    private static final String RELEASES_API =
            "https://api.github.com/repos/lively-Studio/Slime-Craft-Launcher/releases";

    /// Queries the GitHub Releases API and returns the newest release matching the
    /// given channel. For DEVELOPMENT/NIGHTLY channels the first release whose tag
    /// starts with {@code DEV} is returned; for STABLE the first {@code V}-prefixed
    /// release is returned.
    ///
    /// @param channel the update channel to filter by
    /// @param preview whether preview releases are acceptable
    /// @return the newest matching remote version, or {@code null} if none found
    private static @Nullable RemoteVersion checkUpdate(UpdateChannel channel, boolean preview) throws IOException {
        String response = NetworkUtils.doGet(RELEASES_API);
        JsonArray releases = JsonUtils.fromNonNullJson(response, JsonArray.class);

        // NIGHTLY channel: prefer DEV releases first, fall back to STABLE if none found.
        boolean tryDevFirst = (channel == UpdateChannel.NIGHTLY);
        RemoteVersion stableFallback = null;

        for (JsonElement element : releases) {
            if (!element.isJsonObject()) continue;
            JsonObject release = element.getAsJsonObject();
            JsonElement tagEl = release.get("tag_name");
            if (tagEl == null || tagEl.isJsonNull()) continue;
            String tagName = tagEl.getAsString();

            boolean isDevRelease = tagName.startsWith("DEV");
            boolean isStableRelease = tagName.startsWith("V");

            // Channel filtering: DEV channel wants DEV tags, STABLE wants V tags.
            // NIGHTLY channel accepts either (prefers DEV for preview builds).
            if (channel == UpdateChannel.DEVELOPMENT && !isDevRelease) continue;
            if (channel == UpdateChannel.STABLE && !isStableRelease) continue;

            // Find the plain JAR asset (not platform-specific packages).
            JsonElement assetsEl = release.get("assets");
            if (assetsEl == null || !assetsEl.isJsonArray()) continue;
            String jarUrl = null;
            for (JsonElement assetEl : assetsEl.getAsJsonArray()) {
                if (!assetEl.isJsonObject()) continue;
                JsonObject asset = assetEl.getAsJsonObject();
                JsonElement nameEl = asset.get("name");
                if (nameEl == null) continue;
                String name = nameEl.getAsString();
                // The plain JAR ends with .jar and doesn't contain platform identifiers.
                if (name.endsWith(".jar") && !name.contains("-macos-") && !name.contains("-windows-") && !name.contains("-linux-")) {
                    JsonElement urlEl = asset.get("browser_download_url");
                    if (urlEl != null && !urlEl.isJsonNull()) {
                        jarUrl = urlEl.getAsString();
                        break;
                    }
                }
            }

            if (jarUrl == null) continue;

            UpdateChannel remoteChannel = isDevRelease ? UpdateChannel.DEVELOPMENT : UpdateChannel.STABLE;
            RemoteVersion candidate = new RemoteVersion(remoteChannel, tagName, jarUrl, RemoteVersion.Type.JAR, null, preview, false);

            if (tryDevFirst) {
                if (isDevRelease) {
                    // Found a DEV release — return it immediately (prefers DEV for NIGHTLY)
                    return candidate;
                } else if (stableFallback == null) {
                    // Save the first STABLE release as fallback
                    stableFallback = candidate;
                }
            } else {
                // DEVELOPMENT or STABLE channel: return the first matching release
                return candidate;
            }
        }

        // NIGHTLY: if no DEV release found, fall back to STABLE
        if (tryDevFirst && stableFallback != null) {
            return stableFallback;
        }

        return null;
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
