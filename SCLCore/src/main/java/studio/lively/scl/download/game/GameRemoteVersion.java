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
package studio.lively.scl.download.game;

import studio.lively.scl.download.DefaultDependencyManager;
import studio.lively.scl.download.LibraryAnalyzer;
import studio.lively.scl.download.RemoteVersion;
import studio.lively.scl.game.ReleaseType;
import studio.lively.scl.game.Version;
import studio.lively.scl.task.Task;
import studio.lively.scl.util.Immutable;
import studio.lively.scl.util.versioning.GameVersionNumber;

import java.time.Instant;
import java.util.List;

/**
 *
 * @author lively-Studio
 */
@Immutable
public final class GameRemoteVersion extends RemoteVersion {

    private final ReleaseType type;

    public GameRemoteVersion(String gameVersion, String selfVersion, List<String> url, ReleaseType type, Instant releaseDate) {
        super(LibraryAnalyzer.LibraryType.MINECRAFT.getPatchId(), gameVersion, selfVersion, releaseDate, getReleaseType(type), url);
        this.type = type;
    }

    public ReleaseType getType() {
        return type;
    }

    @Override
    public Task<Version> getInstallTask(DefaultDependencyManager dependencyManager, Version baseVersion) {
        return new GameInstallTask(dependencyManager, baseVersion, this);
    }

    @Override
    public int compareTo(RemoteVersion o) {
        if (!(o instanceof GameRemoteVersion)) {
            return 0;
        }

        int dateCompare = o.getReleaseDate().compareTo(getReleaseDate());
        if (dateCompare != 0) {
            return dateCompare;
        }

        return GameVersionNumber.compare(o.getSelfVersion(), getSelfVersion());
    }

    private static Type getReleaseType(ReleaseType type) {
        if (type == null) return Type.UNCATEGORIZED;
        return switch (type) {
            case RELEASE -> Type.RELEASE;
            case SNAPSHOT -> Type.SNAPSHOT;
            case UNKNOWN -> Type.UNCATEGORIZED;
            case PENDING -> Type.PENDING;
            case UNOBFUSCATED -> Type.UNOBFUSCATED;
            default -> Type.OLD;
        };
    }
}
