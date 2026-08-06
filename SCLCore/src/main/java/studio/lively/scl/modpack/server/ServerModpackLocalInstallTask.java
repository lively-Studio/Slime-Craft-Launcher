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
package studio.lively.scl.modpack.server;

import com.google.gson.JsonParseException;
import studio.lively.scl.download.DefaultDependencyManager;
import studio.lively.scl.download.GameBuilder;
import studio.lively.scl.game.DefaultGameRepository;
import studio.lively.scl.modpack.MinecraftInstanceTask;
import studio.lively.scl.modpack.Modpack;
import studio.lively.scl.modpack.ModpackConfiguration;
import studio.lively.scl.modpack.ModpackInstallTask;
import studio.lively.scl.task.Task;
import studio.lively.scl.util.gson.JsonUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ServerModpackLocalInstallTask extends Task<Void> {

    private final Path zipFile;
    private final Modpack modpack;
    private final ServerModpackManifest manifest;
    private final String name;
    private final DefaultGameRepository repository;
    private final List<Task<?>> dependencies = new ArrayList<>();
    private final List<Task<?>> dependents = new ArrayList<>(4);

    public ServerModpackLocalInstallTask(DefaultDependencyManager dependencyManager, Path zipFile, Modpack modpack, ServerModpackManifest manifest, String name) {
        this.zipFile = zipFile;
        this.modpack = modpack;
        this.manifest = manifest;
        this.name = name;
        this.repository = dependencyManager.getGameRepository();
        Path run = repository.getRunDirectory(name);

        Path json = repository.getModpackConfiguration(name);
        if (repository.hasVersion(name) && Files.notExists(json))
            throw new IllegalArgumentException("Version " + name + " already exists.");

        GameBuilder builder = dependencyManager.gameBuilder().name(name);
        for (ServerModpackManifest.Addon addon : manifest.getAddons()) {
            builder.version(addon.getId(), addon.getVersion());
        }

        dependents.add(builder.buildAsync());
        onDone().register(event -> {
            if (event.isFailed())
                repository.removeVersionFromDisk(name);
        });

        ModpackConfiguration<ServerModpackManifest> config = null;
        try {
            if (Files.exists(json)) {
                config = JsonUtils.fromJsonFile(json, ModpackConfiguration.typeOf(ServerModpackManifest.class));

                if (!ServerModpackProvider.INSTANCE.getName().equals(config.getType()))
                    throw new IllegalArgumentException("Version " + name + " is not a Server modpack. Cannot update this version.");
            }
        } catch (JsonParseException | IOException ignore) {
        }
        dependents.add(new ModpackInstallTask<>(zipFile, run, modpack.getEncoding(), Collections.singletonList("/overrides"), any -> true, config).withStage("scl.modpack"));
        dependents.add(new MinecraftInstanceTask<>(zipFile, modpack.getEncoding(), Collections.singletonList("/overrides"), manifest, ServerModpackProvider.INSTANCE, modpack.getName(), modpack.getVersion(), repository.getModpackConfiguration(name)).withStage("scl.modpack"));
    }

    @Override
    public List<Task<?>> getDependents() {
        return dependents;
    }

    @Override
    public List<Task<?>> getDependencies() {
        return dependencies;
    }

    @Override
    public void execute() throws Exception {
    }
}
