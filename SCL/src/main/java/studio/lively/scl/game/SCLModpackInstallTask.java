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
package studio.lively.scl.game;

import com.google.gson.JsonParseException;
import studio.lively.scl.download.DefaultDependencyManager;
import studio.lively.scl.download.LibraryAnalyzer;
import studio.lively.scl.modpack.MinecraftInstanceTask;
import studio.lively.scl.modpack.Modpack;
import studio.lively.scl.modpack.ModpackConfiguration;
import studio.lively.scl.modpack.ModpackInstallTask;
import studio.lively.scl.task.Task;
import studio.lively.scl.util.gson.JsonUtils;
import studio.lively.scl.util.io.CompressingUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SCLModpackInstallTask extends Task<Void> {
    private final Path zipFile;
    private final String name;
    private final SCLGameRepository repository;
    private final DefaultDependencyManager dependency;
    private final Modpack modpack;
    private final List<Task<?>> dependencies = new ArrayList<>(1);
    private final List<Task<?>> dependents = new ArrayList<>(4);

    public SCLModpackInstallTask(SCLGameRepository repository, Path zipFile, Modpack modpack, String name) {
        this.repository = repository;
        this.dependency = repository.getDependency();
        this.zipFile = zipFile;
        this.name = name;
        this.modpack = modpack;

        Path run = repository.getRunDirectory(name);
        Path json = repository.getModpackConfiguration(name);
        if (repository.hasVersion(name) && Files.notExists(json))
            throw new IllegalArgumentException("Version " + name + " already exists");

        dependents.add(dependency.gameBuilder().name(name).gameVersion(modpack.getGameVersion()).buildAsync());

        onDone().register(event -> {
            if (event.isFailed()) repository.removeVersionFromDisk(name);
        });

        ModpackConfiguration<Modpack> config = null;
        try {
            if (Files.exists(json)) {
                config = JsonUtils.fromJsonFile(json, ModpackConfiguration.typeOf(Modpack.class));

                if (!SCLModpackProvider.INSTANCE.getName().equals(config.getType()))
                    throw new IllegalArgumentException("Version " + name + " is not a SCL modpack. Cannot update this version.");
            }
        } catch (JsonParseException | IOException ignore) {
        }
        dependents.add(new ModpackInstallTask<>(zipFile, run, modpack.getEncoding(), Collections.singletonList("/minecraft"), it -> !"pack.json".equals(it), config));
        dependents.add(new MinecraftInstanceTask<>(zipFile, modpack.getEncoding(), Collections.singletonList("/minecraft"), modpack, SCLModpackProvider.INSTANCE, modpack.getName(), modpack.getVersion(), repository.getModpackConfiguration(name)).withStage("scl.modpack"));
    }

    @Override
    public List<Task<?>> getDependencies() {
        return dependencies;
    }

    @Override
    public List<Task<?>> getDependents() {
        return dependents;
    }

    @Override
    public void execute() throws Exception {
        String json = CompressingUtils.readTextZipEntry(zipFile, "minecraft/pack.json");
        Version originalVersion = JsonUtils.GSON.fromJson(json, Version.class).setId(name).setJar(null);
        LibraryAnalyzer analyzer = LibraryAnalyzer.analyze(originalVersion, null);
        Task<Version> libraryTask = Task.supplyAsync(() -> originalVersion);
        // reinstall libraries
        // libraries of Forge and OptiFine should be obtained by installation.
        for (LibraryAnalyzer.LibraryMark mark : analyzer) {
            if (LibraryAnalyzer.LibraryType.MINECRAFT.getPatchId().equals(mark.getLibraryId()))
                continue;
            libraryTask = libraryTask.thenComposeAsync(version -> dependency.installLibraryAsync(modpack.getGameVersion(), version, mark.getLibraryId(), mark.getLibraryVersion()));
        }

        dependencies.add(libraryTask.thenComposeAsync(repository::saveAsync));
    }
}
