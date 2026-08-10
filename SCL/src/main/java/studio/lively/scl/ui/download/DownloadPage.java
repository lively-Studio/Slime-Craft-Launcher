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
package studio.lively.scl.ui.download;

import com.jfoenix.controls.JFXButton;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.Node;
import studio.lively.scl.addon.RemoteAddon;
import studio.lively.scl.addon.repository.CurseForgeRemoteAddonRepository;
import studio.lively.scl.download.*;
import studio.lively.scl.download.game.GameRemoteVersion;
import studio.lively.scl.game.SCLGameRepository;
import studio.lively.scl.setting.DownloadProviders;
import studio.lively.scl.setting.GameDirectoryManager;
import studio.lively.scl.task.FileDownloadTask;
import studio.lively.scl.task.Schedulers;
import studio.lively.scl.task.Task;
import studio.lively.scl.ui.Controllers;
import studio.lively.scl.ui.FXUtils;
import studio.lively.scl.ui.SVG;
import studio.lively.scl.ui.WeakListenerHolder;
import studio.lively.scl.ui.animation.TransitionPane;
import studio.lively.scl.ui.construct.AdvancedListBox;
import studio.lively.scl.ui.construct.MessageDialogPane;
import studio.lively.scl.ui.construct.TabHeader;
import studio.lively.scl.ui.construct.Validator;
import studio.lively.scl.ui.decorator.DecoratorAnimatedPage;
import studio.lively.scl.ui.decorator.DecoratorPage;
import studio.lively.scl.ui.versions.DownloadListPage;
import studio.lively.scl.ui.versions.SCLLocalizedDownloadListPage;
import studio.lively.scl.ui.versions.VersionPage;
import studio.lively.scl.ui.versions.Versions;
import studio.lively.scl.ui.wizard.Navigation;
import studio.lively.scl.ui.wizard.WizardController;
import studio.lively.scl.ui.wizard.WizardProvider;
import studio.lively.scl.util.SettingsMap;
import studio.lively.scl.util.TaskCancellationAction;
import studio.lively.scl.util.io.FileUtils;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static studio.lively.scl.ui.FXUtils.runInFX;
import static studio.lively.scl.util.i18n.I18n.i18n;
import static studio.lively.scl.util.logging.Logger.LOG;

public class DownloadPage extends DecoratorAnimatedPage implements DecoratorPage {
    public static final studio.lively.scl.ui.versions.DownloadPage.DownloadCallback FOR_MOD =
            (downloadProvider, repository, version, mod, file) -> download(downloadProvider, repository, version, file, "mods");
    public static final studio.lively.scl.ui.versions.DownloadPage.DownloadCallback FOR_RESOURCE_PACK =
            (downloadProvider, repository, version, pack, file) -> download(downloadProvider, repository, version, file, "resourcepacks");
    public static final studio.lively.scl.ui.versions.DownloadPage.DownloadCallback FOR_SHADER =
            (downloadProvider, repository, version, shader, file) -> download(downloadProvider, repository, version, file, "shaderpacks");

    private final ReadOnlyObjectWrapper<DecoratorPage.State> state = new ReadOnlyObjectWrapper<>(DecoratorPage.State.fromTitle(i18n("download"), -1));
    private final TabHeader tab;
    private final TabHeader.Tab<VersionsPage> newGameTab = new TabHeader.Tab<>("newGameTab");
    private final TabHeader.Tab<DownloadListPage> modTab = new TabHeader.Tab<>("modTab");
    private final TabHeader.Tab<DownloadListPage> modpackTab = new TabHeader.Tab<>("modpackTab");
    private final TabHeader.Tab<DownloadListPage> resourcePackTab = new TabHeader.Tab<>("resourcePackTab");
    private final TabHeader.Tab<DownloadListPage> shaderTab = new TabHeader.Tab<>("shaderTab");
    private final TabHeader.Tab<DownloadListPage> worldTab = new TabHeader.Tab<>("worldTab");
    private final TransitionPane transitionPane = new TransitionPane();
    private final DownloadNavigator versionPageNavigator = new DownloadNavigator();

    private WeakListenerHolder listenerHolder;

    public DownloadPage() {
        this(null);
    }

    public DownloadPage(String uploadVersion) {
        newGameTab.setNodeSupplier(loadVersionFor(() -> new VersionsPage(versionPageNavigator, i18n("install.installer.choose", i18n("install.installer.game")), "", DownloadProviders.getDownloadProvider(),
                "game", versionPageNavigator::onGameSelected)));
        modpackTab.setNodeSupplier(loadVersionFor(() -> {
            DownloadListPage page = SCLLocalizedDownloadListPage.ofModPack((downloadProvider, repository, __, modpack, file) -> {
                Versions.downloadModpackImpl(downloadProvider, repository, uploadVersion, modpack, file);
            }, false);

            JFXButton installLocalModpackButton = FXUtils.newRaisedButton(i18n("install.modpack"));
            installLocalModpackButton.setOnAction(e -> Versions.importModpack());

            page.getActions().add(installLocalModpackButton);
            return page;
        }));
        modTab.setNodeSupplier(loadVersionFor(() -> SCLLocalizedDownloadListPage.ofMod(FOR_MOD, true)));
        resourcePackTab.setNodeSupplier(loadVersionFor(() -> SCLLocalizedDownloadListPage.ofResourcePack(FOR_RESOURCE_PACK, true)));
        shaderTab.setNodeSupplier(loadVersionFor(() -> SCLLocalizedDownloadListPage.ofShaderPack(FOR_SHADER, true)));
        worldTab.setNodeSupplier(loadVersionFor(() -> new DownloadListPage(CurseForgeRemoteAddonRepository.WORLDS)));
        tab = new TabHeader(transitionPane, newGameTab, modpackTab, modTab, resourcePackTab, shaderTab, worldTab);

        GameDirectoryManager.registerVersionsListener(this::loadVersions);

        tab.select(newGameTab);

        AdvancedListBox sideBar = new AdvancedListBox(true /* horizontal */);
        sideBar.setSpacing(4);
        sideBar.addNavigationDrawerTab(tab, newGameTab, i18n("game"), SVG.STADIA_CONTROLLER, SVG.STADIA_CONTROLLER_FILL)
                .addNavigationDrawerTab(tab, modpackTab, i18n("modpack"), SVG.PACKAGE2, SVG.PACKAGE2_FILL)
                .addNavigationDrawerTab(tab, modTab, i18n("mods"), SVG.EXTENSION, SVG.EXTENSION_FILL)
                .addNavigationDrawerTab(tab, resourcePackTab, i18n("resourcepack"), SVG.TEXTURE)
                .addNavigationDrawerTab(tab, shaderTab, i18n("download.shader"), SVG.WB_SUNNY, SVG.WB_SUNNY_FILL)
                .addNavigationDrawerTab(tab, worldTab, i18n("world"), SVG.PUBLIC);
        setLeft(sideBar);

        setCenter(transitionPane);
    }

    private static <T extends Node> Supplier<T> loadVersionFor(Supplier<T> nodeSupplier) {
        return () -> {
            T node = nodeSupplier.get();
            if (node instanceof VersionPage.GameInstanceLoadable loadable) {
                loadable.loadInstance(GameDirectoryManager.getSelectedRepository(), null);
            }
            return node;
        };
    }

    public static void download(DownloadProvider downloadProvider, SCLGameRepository repository, @Nullable String version, RemoteAddon.Version file, String subdirectoryName) {
        if (version == null) version = repository.getSelectedInstance();

        Path runDirectory = repository.hasVersion(version) ? repository.getRunDirectory(version) : repository.getBaseDirectory();

        Set<String> existingFiles;

        try (var list = Files.list(runDirectory.resolve(subdirectoryName))) {
            existingFiles = list.map(Path::getFileName)
                    .map(Path::toString)
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            LOG.warning("Failed to list files in " + runDirectory.resolve(subdirectoryName), e);
            existingFiles = Set.of();
        }

        Set<String> finalExistingFiles = existingFiles;

        Controllers.prompt(i18n("archive.file.name"), (result, handler) -> {
            Path dest = runDirectory.resolve(subdirectoryName).resolve(result);

            Controllers.taskDialog(Task.composeAsync(() -> {
                var task = new FileDownloadTask(downloadProvider.injectURLWithCandidates(file.file().url()), dest);
                task.setName(file.name());
                return task;
            }).whenComplete(Schedulers.javafx(), exception -> {
                if (exception != null) {
                    if (exception instanceof CancellationException) {
                        Controllers.showToast(i18n("message.cancelled"));
                    } else {
                        Controllers.dialog(DownloadProviders.localizeErrorMessage(exception), i18n("install.failed.downloading"), MessageDialogPane.MessageType.ERROR);
                    }
                } else {
                    Controllers.showToast(i18n("install.success"));
                }
            }), i18n("message.downloading"), TaskCancellationAction.NORMAL);
            handler.resolve();
        }, file.file().filename(), new Validator(i18n("install.new_game.malformed"), FileUtils::isNameValid), new Validator(i18n("game_directory.already_exists"), (it) -> !finalExistingFiles.contains(it)));

    }

    private void loadVersions(SCLGameRepository repository) {
        listenerHolder = new WeakListenerHolder();
        runInFX(() -> {
            if (repository.getGameDirectory() == GameDirectoryManager.getSelectedGameDirectory()) {
                listenerHolder.add(FXUtils.onWeakChangeAndOperate(GameDirectoryManager.selectedInstanceProperty(), version -> {
                    if (modTab.isInitialized()) {
                        modTab.getNode().loadInstance(repository, null);
                    }
                    if (modpackTab.isInitialized()) {
                        modpackTab.getNode().loadInstance(repository, null);
                    }
                    if (resourcePackTab.isInitialized()) {
                        resourcePackTab.getNode().loadInstance(repository, null);
                    }
                    if (shaderTab.isInitialized()) {
                        shaderTab.getNode().loadInstance(repository, null);
                    }
                    if (worldTab.isInitialized()) {
                        worldTab.getNode().loadInstance(repository, null);
                    }
                }));
            }
        });
    }

    @Override
    public ReadOnlyObjectProperty<State> stateProperty() {
        return state.getReadOnlyProperty();
    }

    public void showGameDownloads() {
        tab.select(newGameTab, false);
    }

    public void showModpackDownloads() {
        tab.select(modpackTab, false);
    }

    public DownloadListPage showResourcePackDownloads() {
        tab.select(resourcePackTab, false);
        return resourcePackTab.getNode();
    }

    public DownloadListPage showModDownloads() {
        tab.select(modTab, false);
        return modTab.getNode();
    }

    public void showWorldDownloads() {
        tab.select(worldTab, false);
    }

    private static final class DownloadNavigator implements Navigation {
        private final SettingsMap settings = new SettingsMap();

        @Override
        public void onStart() {

        }

        @Override
        public void onNext() {

        }

        @Override
        public void onPrev(boolean cleanUp) {
        }

        @Override
        public boolean canPrev() {
            return false;
        }

        @Override
        public void onFinish() {

        }

        @Override
        public void onEnd() {

        }

        @Override
        public void onCancel() {

        }

        @Override
        public SettingsMap getSettings() {
            return settings;
        }

        public void onGameSelected() {
            SCLGameRepository repository = GameDirectoryManager.getSelectedRepository();
            if (repository.isLoaded()) {
                Controllers.getDecorator().startWizard(new VanillaInstallWizardProvider(repository, (GameRemoteVersion) settings.get("game")), i18n("install.new_game"));
            }
        }

    }

    private static class VanillaInstallWizardProvider implements WizardProvider {
        private final SCLGameRepository repository;
        private final DefaultDependencyManager dependencyManager;
        private final DownloadProvider downloadProvider;
        private final GameRemoteVersion gameVersion;

        public VanillaInstallWizardProvider(SCLGameRepository repository, GameRemoteVersion gameVersion) {
            this.repository = repository;
            this.gameVersion = gameVersion;
            this.downloadProvider = DownloadProviders.getDownloadProvider();
            this.dependencyManager = repository.getDependency(downloadProvider);
        }

        @Override
        public void start(SettingsMap settings) {
            settings.put(ModpackPage.GAME_DIRECTORY, repository.getGameDirectory());
            settings.put(ModpackPage.REPOSITORY, repository);
            settings.put(LibraryAnalyzer.LibraryType.MINECRAFT.getPatchId(), gameVersion);
        }

        private Task<Void> finishVersionDownloadingAsync(SettingsMap settings) {
            GameBuilder builder = dependencyManager.gameBuilder();

            String name = (String) settings.get("name");
            builder.name(name);
            builder.gameVersion(((RemoteVersion) settings.get(LibraryAnalyzer.LibraryType.MINECRAFT.getPatchId())).getGameVersion());

            settings.asStringMap().forEach((key, value) -> {
                if (!LibraryAnalyzer.LibraryType.MINECRAFT.getPatchId().equals(key)
                        && value instanceof RemoteVersion remoteVersion)
                    builder.version(remoteVersion);
            });

            repository.applyDefaultIsolationSettingForNewInstance(name, settings.isInstallingModdedVersion());
            return builder.buildAsync().whenComplete(any -> {
                repository.refreshVersions();
            }).thenRunAsync(Schedulers.javafx(), () -> repository.setSelectedInstance(name));
        }

        @Override
        public Object finish(SettingsMap settings) {
            settings.put("title", i18n("install.new_game.installation"));
            settings.put("success_message", i18n("install.success"));
            settings.put(FailureCallback.KEY, (settings1, exception, next) -> UpdateInstallerWizardProvider.alertFailureMessage(exception, next));

            return finishVersionDownloadingAsync(settings);
        }

        @Override
        public Node createPage(WizardController controller, int step, SettingsMap settings) {
            switch (step) {
                case 0:
                    return new InstallersPage(controller, repository, ((RemoteVersion) controller.getSettings().get("game")).getGameVersion(), downloadProvider);
                default:
                    throw new IllegalStateException("error step " + step + ", settings: " + settings + ", pages: " + controller.getPages());
            }
        }

        @Override
        public boolean cancel() {
            return true;
        }
    }
}
