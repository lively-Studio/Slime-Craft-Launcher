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
package studio.lively.scl.download.cleanroom;

import studio.lively.scl.download.DefaultDependencyManager;
import studio.lively.scl.download.LibraryAnalyzer;
import studio.lively.scl.download.RemoteVersion;
import studio.lively.scl.game.Version;
import studio.lively.scl.task.Task;

import java.time.Instant;
import java.util.List;

public class CleanroomRemoteVersion extends RemoteVersion {
    public CleanroomRemoteVersion(String gameVersion, String selfVersion, Instant releaseDate, List<String> url) {
        super(LibraryAnalyzer.LibraryType.CLEANROOM.getPatchId(), gameVersion, selfVersion, releaseDate, url);
    }

    @Override
    public Task<Version> getInstallTask(DefaultDependencyManager dependencyManager, Version baseVersion) {
        return new CleanroomInstallTask(dependencyManager, baseVersion, this);
    }
}
