/*
 * Slime Craft Launcher
 * Copyright (C) 2026 lively-Studio <X_CODER_ocs2008@126.com> and contributors
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
package studio.lively.scl.addon.resourcepack;

import javafx.scene.image.Image;
import studio.lively.scl.download.DownloadProvider;
import studio.lively.scl.addon.RemoteAddon;
import studio.lively.scl.addon.meta.PackMcMeta;
import studio.lively.scl.util.io.FileUtils;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static studio.lively.scl.util.logging.Logger.LOG;

final class ResourcePackFolder extends ResourcePackFile {
    private final PackMcMeta meta;
    private final @Nullable Image icon;

    public ResourcePackFolder(ResourcePackManager manager, Path path) {
        super(manager, path);

        PackMcMeta meta = null;
        try {
            meta = PackMcMeta.fromNonNullJsonFile(path.resolve("pack.mcmeta"));
        } catch (Exception e) {
            LOG.warning("Failed to parse resource pack meta", e);
        }
        this.meta = meta;

        byte[] iconData = null;
        Image iconTemp = null;
        try {
            iconData = Files.readAllBytes(path.resolve("pack.png"));
        } catch (IOException e) {
            LOG.warning("Failed to read resource pack icon", e);
        }
        if (iconData != null) {
            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(iconData)) {
                iconTemp = new Image(inputStream, 64, 64, true, true);
            } catch (Exception e) {
                LOG.warning("Failed to load resource pack icon", e);
            }
        }
        this.icon = iconTemp;
    }

    @Override
    public PackMcMeta getMeta() {
        return meta;
    }

    @Override
    public @Nullable Image getIcon() {
        return icon;
    }

    @Override
    public void delete() throws IOException {
        FileUtils.deleteDirectory(file);
    }

    @Override
    public AddonUpdate checkUpdates(DownloadProvider downloadProvider, String gameVersion, RemoteAddon.Source source) {
        return null;
    }
}
