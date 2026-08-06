/*
 * Slime Craft Launcher
 * Copyright (C) 2021  lively-Studio <X_CODER_ocs2008@126.com> and contributors
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
package studio.lively.scl.download.liteloader;

import studio.lively.scl.download.BMCLAPIDownloadProvider;
import studio.lively.scl.download.RemoteVersion;
import studio.lively.scl.download.VersionList;
import studio.lively.scl.task.GetTask;
import studio.lively.scl.task.Task;
import studio.lively.scl.util.gson.JsonUtils;
import studio.lively.scl.util.io.NetworkUtils;

import java.util.Collections;
import java.util.Map;

/**
 * @author lively-Studio
 */
public final class LiteLoaderBMCLVersionList extends VersionList<LiteLoaderRemoteVersion> {
    private final BMCLAPIDownloadProvider downloadProvider;

    public LiteLoaderBMCLVersionList(BMCLAPIDownloadProvider downloadProvider) {
        this.downloadProvider = downloadProvider;
    }

    @Override
    public boolean hasType() {
        return false;
    }

    private static final class LiteLoaderBMCLVersion {

        private final LiteLoaderVersion build;
        private final String version;

        public LiteLoaderBMCLVersion(LiteLoaderVersion build, String version) {
            this.build = build;
            this.version = version;
        }
    }

    @Override
    public Task<?> refreshAsync() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Task<?> refreshAsync(String gameVersion) {
        return new GetTask(
                NetworkUtils.withQuery(downloadProvider.getApiRoot() + "/liteloader/list", Map.of(
                        "mcversion", gameVersion
                )))
                .thenApplyAsync(json -> JsonUtils.fromMaybeMalformedJson(json, LiteLoaderBMCLVersion.class))
                .thenAcceptAsync(v -> {
                    lock.writeLock().lock();
                    try {
                        versions.clear();
                        if (v == null)
                            return;
                        versions.put(gameVersion, new LiteLoaderRemoteVersion(
                                gameVersion, v.version, RemoteVersion.Type.UNCATEGORIZED,
                                Collections.singletonList(NetworkUtils.withQuery(
                                        downloadProvider.getApiRoot() + "/liteloader/download",
                                        Collections.singletonMap("version", v.version)
                                )),
                                v.build.getTweakClass(), v.build.getLibraries()
                        ));
                    } finally {
                        lock.writeLock().unlock();
                    }
                });
    }
}
