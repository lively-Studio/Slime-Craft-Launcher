/*
 * Slime Craft Launcher
 * Copyright (C) 2022  lively-Studio <X_CODER_ocs2008@126.com> and contributors
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
package studio.lively.scl.game;

import com.google.gson.JsonParseException;
import kala.compress.archivers.zip.ZipArchiveReader;
import studio.lively.scl.download.DefaultDependencyManager;
import studio.lively.scl.modpack.MismatchedModpackTypeException;
import studio.lively.scl.modpack.Modpack;
import studio.lively.scl.modpack.ModpackProvider;
import studio.lively.scl.modpack.ModpackUpdateTask;
import studio.lively.scl.task.Task;
import studio.lively.scl.util.StringUtils;
import studio.lively.scl.util.gson.JsonUtils;
import studio.lively.scl.util.io.CompressingUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;

public final class SCLModpackProvider implements ModpackProvider {
    public static final SCLModpackProvider INSTANCE = new SCLModpackProvider();

    @Override
    public String getName() {
        return "SCL";
    }

    @Override
    public Task<?> createCompletionTask(DefaultDependencyManager dependencyManager, String version) {
        return null;
    }

    @Override
    public Task<?> createUpdateTask(DefaultDependencyManager dependencyManager, String name, Path zipFile, Modpack modpack) throws MismatchedModpackTypeException {
        if (!(modpack.getManifest() instanceof SCLModpackManifest))
            throw new MismatchedModpackTypeException(getName(), modpack.getManifest().getProvider().getName());

        if (!(dependencyManager.getGameRepository() instanceof SCLGameRepository repository)) {
            throw new IllegalArgumentException("SCLModpackProvider requires SCLGameRepository");
        }

        return new ModpackUpdateTask(dependencyManager.getGameRepository(), name, new SCLModpackInstallTask(repository, zipFile, modpack, name));
    }

    @Override
    public Modpack readManifest(ZipArchiveReader file, Path path, Charset encoding) throws IOException, JsonParseException {
        String manifestJson = CompressingUtils.readTextZipEntry(file, "modpack.json");
        Modpack manifest = JsonUtils.fromNonNullJson(manifestJson, SCLModpack.class).setEncoding(encoding);
        String gameJson = CompressingUtils.readTextZipEntry(file, "minecraft/pack.json");
        Version game = JsonUtils.fromNonNullJson(gameJson, Version.class);
        if (game.getJar() == null)
            if (StringUtils.isBlank(manifest.getVersion()))
                throw new JsonParseException("Cannot recognize the game version of modpack " + file + ".");
            else
                manifest.setManifest(SCLModpackManifest.INSTANCE);
        else
            manifest.setManifest(SCLModpackManifest.INSTANCE).setGameVersion(game.getJar());
        return manifest;
    }

    private final static class SCLModpack extends Modpack {
        @Override
        public Task<?> getInstallTask(DefaultDependencyManager dependencyManager, Path zipFile, String name, String iconUrl) {
            return new SCLModpackInstallTask((SCLGameRepository) dependencyManager.getGameRepository(), zipFile, this, name);
        }
    }

}
