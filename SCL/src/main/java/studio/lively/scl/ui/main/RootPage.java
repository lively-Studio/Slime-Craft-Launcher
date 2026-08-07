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
package studio.lively.scl.ui.main;

import com.jfoenix.controls.JFXPopup;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.scene.layout.Region;
import studio.lively.scl.Metadata;
import studio.lively.scl.event.EventBus;
import studio.lively.scl.event.RefreshedVersionsEvent;
import studio.lively.scl.game.SCLGameRepository;
import studio.lively.scl.game.ModpackHelper;
import studio.lively.scl.game.Version;
import studio.lively.scl.setting.Accounts;
import studio.lively.scl.setting.GameDirectory;
import studio.lively.scl.setting.GameDirectoryManager;
import studio.lively.scl.task.Schedulers;
import studio.lively.scl.task.Task;
import studio.lively.scl.terracotta.TerracottaMetadata;
import studio.lively.scl.ui.Controllers;
import studio.lively.scl.ui.FXUtils;
import studio.lively.scl.ui.SVG;
import studio.lively.scl.ui.account.AccountAdvancedListItem;
import studio.lively.scl.ui.account.AccountListPopupMenu;
import studio.lively.scl.ui.animation.AnimationUtils;
import studio.lively.scl.ui.construct.AdvancedListBox;
import studio.lively.scl.ui.construct.AdvancedListItem;
import studio.lively.scl.ui.construct.MessageDialogPane;
import studio.lively.scl.ui.decorator.DecoratorAnimatedPage;
import studio.lively.scl.ui.decorator.DecoratorPage;
import studio.lively.scl.ui.download.ModpackInstallWizardProvider;
import studio.lively.scl.ui.nbt.NBTEditorPage;
import studio.lively.scl.ui.nbt.NBTFileType;
import studio.lively.scl.ui.versions.GameAdvancedListItem;
import studio.lively.scl.ui.versions.GameListPopupMenu;
import studio.lively.scl.ui.versions.Versions;
import studio.lively.scl.upgrade.UpdateChecker;
import studio.lively.scl.util.Lang;
import studio.lively.scl.util.StringUtils;
import studio.lively.scl.util.TaskCancellationAction;
import studio.lively.scl.util.io.CompressingUtils;
import studio.lively.scl.util.io.FileUtils;
import studio.lively.scl.util.platform.*;
import studio.lively.scl.util.versioning.VersionNumber;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static studio.lively.scl.ui.FXUtils.runInFX;
import static studio.lively.scl.util.i18n.I18n.i18n;
import static studio.lively.scl.util.logging.Logger.LOG;

public class RootPage extends DecoratorAnimatedPage implements DecoratorPage {
    private MainPage mainPage = null;

    public RootPage() {
        EventBus.EVENT_BUS.channel(RefreshedVersionsEvent.class)
                .register(event -> onRefreshedVersions((SCLGameRepository) event.getSource()));

        SCLGameRepository repository = GameDirectoryManager.getSelectedRepository();
        if (repository.isLoaded())
            onRefreshedVersions(GameDirectoryManager.getSelectedRepository());

        getStyleClass().remove("gray-background");
        getLeft().getStyleClass().add("gray-background");
    }

    @Override
    public ReadOnlyObjectProperty<State> stateProperty() {
        return getMainPage().stateProperty();
    }

    @Override
    protected Skin createDefaultSkin() {
        return new Skin(this);
    }

    public MainPage getMainPage() {
        if (mainPage == null) {
            MainPage mainPage = new MainPage();
            FXUtils.applyDragListener(mainPage,
                    file -> ModpackHelper.isFileModpackByExtension(file) || NBTFileType.isNBTFileByExtension(file) || "json".equalsIgnoreCase(FileUtils.getExtension(file)),
                    modpacks -> {
                        Path file = modpacks.get(0);
                        if (ModpackHelper.isFileModpackByExtension(file)) {
                            Controllers.getDecorator().startWizard(
                                    new ModpackInstallWizardProvider(GameDirectoryManager.getSelectedRepository(), file),
                                    i18n("install.modpack"));
                        } else if (NBTFileType.isNBTFileByExtension(file)) {
                            try {
                                Controllers.navigate(new NBTEditorPage(file));
                            } catch (Throwable e) {
                                LOG.warning("Fail to open nbt file", e);
                                Controllers.dialog(i18n("nbt.open.failed") + "\n\n" + StringUtils.getStackTrace(e),
                                        i18n("message.error"), MessageDialogPane.MessageType.ERROR);
                            }
                        } else if ("json".equalsIgnoreCase(FileUtils.getExtension(file))) {
                            Versions.installFromJson(GameDirectoryManager.getSelectedRepository(), file);
                        }
                    });

            FXUtils.onChangeAndOperate(GameDirectoryManager.selectedInstanceProperty(), mainPage::setCurrentGame);
            mainPage.latestVersionProperty().bind(UpdateChecker.latestVersionProperty());

            GameDirectoryManager.registerVersionsListener(repository -> {
                GameDirectory gameDirectory = repository.getGameDirectory();
                List<Version> children = repository.getVersions().parallelStream()
                        .filter(version -> !version.isHidden())
                        .sorted(Comparator
                                .comparing((Version version) -> Lang.requireNonNullElse(version.getReleaseTime(), Instant.EPOCH))
                                .thenComparing(version -> VersionNumber.asVersion(repository.getGameVersion(version).orElse(version.getId()))))
                        .collect(Collectors.toList());
                runInFX(() -> {
                    if (gameDirectory == GameDirectoryManager.getSelectedGameDirectory())
                        mainPage.initVersions(repository, children);
                });
            });
            this.mainPage = mainPage;
        }
        return mainPage;
    }

    private static class Skin extends DecoratorAnimatedPageSkin<RootPage> {

        protected Skin(RootPage control) {
            super(control);

            // first item in left sidebar
            AccountAdvancedListItem accountListItem = new AccountAdvancedListItem();
            accountListItem.setOnAction(e -> Controllers.navigate(Controllers.getAccountListPage()));
            FXUtils.onSecondaryButtonClicked(accountListItem, () -> AccountListPopupMenu.show(accountListItem, JFXPopup.PopupVPosition.TOP, JFXPopup.PopupHPosition.LEFT, accountListItem.getWidth(), 0));
            accountListItem.accountProperty().bind(Accounts.selectedAccountProperty());

            // second item in left sidebar
            GameAdvancedListItem gameListItem = new GameAdvancedListItem();
            gameListItem.setOnAction(e -> {
                String version = GameDirectoryManager.getSelectedRepository().getSelectedInstance();
                if (version == null) {
                    Controllers.navigate(Controllers.getGameListPage());
                } else {
                    Versions.modifyGameSettings(GameDirectoryManager.getSelectedRepository(), version);
                }
            });
            FXUtils.onScroll(gameListItem, getSkinnable().getMainPage().getVersions(), list -> {
                String currentId = getSkinnable().getMainPage().getCurrentGame();
                return Lang.indexWhere(list, instance -> instance.getId().equals(currentId));
            }, it -> getSkinnable().getMainPage().getRepository().setSelectedInstance(it.getId()));
            if (AnimationUtils.isAnimationEnabled()) {
                FXUtils.prepareOnMouseEnter(gameListItem, Controllers::prepareVersionPage);
            }
            FXUtils.onSecondaryButtonClicked(gameListItem, () -> showGameListPopupMenu(gameListItem));

            // third item in left sidebar
            AdvancedListItem gameItem = new AdvancedListItem();
            gameItem.setLeftIcon(SVG.FORMAT_LIST_BULLETED);
            gameItem.setTitle(i18n("version.manage"));
            gameItem.setOnAction(e -> Controllers.navigate(Controllers.getGameListPage()));
            FXUtils.onSecondaryButtonClicked(gameItem, () -> showGameListPopupMenu(gameItem));

            // forth item in left sidebar
            AdvancedListItem downloadItem = new AdvancedListItem();
            downloadItem.setLeftIcon(SVG.DOWNLOAD);
            downloadItem.setTitle(i18n("download"));
            downloadItem.setOnAction(e -> {
                Controllers.getDownloadPage().showGameDownloads();
                Controllers.navigate(Controllers.getDownloadPage());
            });
            if (AnimationUtils.isAnimationEnabled()) {
                FXUtils.prepareOnMouseEnter(downloadItem, Controllers::prepareDownloadPage);
            }

            // fifth item in left sidebar
            AdvancedListItem launcherSettingsItem = new AdvancedListItem();
            launcherSettingsItem.setLeftIcon(SVG.SETTINGS);
            launcherSettingsItem.setTitle(i18n("settings"));
            launcherSettingsItem.setOnAction(e -> {
                Controllers.getSettingsPage().showGameSettings(GameDirectoryManager.getSelectedRepository());
                Controllers.navigate(Controllers.getSettingsPage());
            });
            if (AnimationUtils.isAnimationEnabled()) {
                FXUtils.prepareOnMouseEnter(launcherSettingsItem, Controllers::prepareSettingsPage);
            }

            // sixth item in left sidebar
            AdvancedListItem terracottaItem = new AdvancedListItem();
            terracottaItem.setLeftIcon(SVG.GRAPH2);
            terracottaItem.setTitle(i18n("terracotta"));
            terracottaItem.setOnAction(e -> {
                if (TerracottaMetadata.PROVIDER != null) {
                    Controllers.navigate(Controllers.getTerracottaPage());
                } else {
                    String message;
                    if (Architecture.SYSTEM_ARCH.getBits() == Bits.BIT_32)
                        message = i18n("terracotta.unsupported.arch.32bit");
                    else if (OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS
                            && !OperatingSystem.SYSTEM_VERSION.isAtLeast(OSVersion.WINDOWS_10))
                        message = i18n("terracotta.unsupported.os.windows.old");
                    else if (Platform.SYSTEM_PLATFORM.equals(OperatingSystem.LINUX, Architecture.LOONGARCH64_OW))
                        message = i18n("terracotta.unsupported.arch.loongarch64_ow");
                    else
                        message = i18n("terracotta.unsupported");

                    Controllers.dialog(message, null, MessageDialogPane.MessageType.WARNING);
                }
            });

            // seventh item in left sidebar
            AdvancedListItem nbtItem = new AdvancedListItem();
            nbtItem.setLeftIcon(SVG.DEPLOYED_CODE);
            nbtItem.setTitle(i18n("nbt.viewer"));
            nbtItem.setOnAction(e -> Controllers.openNbtEditor());

            // the bottom navigation bar
            AdvancedListBox sideBar = new AdvancedListBox(true /* horizontal */);
            sideBar.setSpacing(4);
                    .startCategory(i18n("account").toUpperCase(Locale.ROOT))
                    .add(accountListItem)
                    .startCategory(i18n("version").toUpperCase(Locale.ROOT))
                    .add(gameListItem)
                    .add(gameItem)
                    .add(downloadItem)
                    .startCategory(i18n("settings.launcher.general").toUpperCase(Locale.ROOT))
                    .add(launcherSettingsItem)
                    .add(terracottaItem)
                    .add(nbtItem)
                    .addNavigationDrawerItem(i18n("contact.chat"), SVG.CHAT, () -> {
                        Controllers.getSettingsPage().showFeedback();
                        Controllers.navigate(Controllers.getSettingsPage());
                    });

            // the root page, with the sidebar in left, navigator in center.
            setLeft(sideBar);
            setCenter(getSkinnable().getMainPage());
        }

        public void showGameListPopupMenu(Region gameListItem) {
            GameListPopupMenu.show(gameListItem,
                    JFXPopup.PopupVPosition.TOP,
                    JFXPopup.PopupHPosition.LEFT,
                    gameListItem.getWidth(),
                    0,
                    getSkinnable().getMainPage().getRepository(),
                    getSkinnable().getMainPage().getVersions());
        }
    }

    private boolean checkedModpack = false;

    private void onRefreshedVersions(SCLGameRepository repository) {
        runInFX(() -> {
            if (!checkedModpack) {
                checkedModpack = true;

                if (repository.getVersionCount() == 0) {
                    Path zipModpack = Metadata.CURRENT_DIRECTORY.resolve("modpack.zip");
                    Path mrpackModpack = Metadata.CURRENT_DIRECTORY.resolve("modpack.mrpack");

                    Path modpackFile;
                    if (Files.exists(zipModpack)) {
                        modpackFile = zipModpack;
                    } else if (Files.exists(mrpackModpack)) {
                        modpackFile = mrpackModpack;
                    } else {
                        modpackFile = null;
                    }

                    if (modpackFile != null) {
                        Task.supplyAsync(() -> CompressingUtils.findSuitableEncoding(modpackFile))
                                .thenApplyAsync(encoding -> ModpackHelper.readModpackManifest(modpackFile, encoding))
                                .thenApplyAsync(modpack -> ModpackHelper
                                        .getInstallTask(repository, modpackFile, modpack.getName(), modpack, null)
                                        .executor())
                                .thenAcceptAsync(Schedulers.javafx(), executor -> {
                                    Controllers.taskDialog(executor, i18n("modpack.installing"), TaskCancellationAction.NO_CANCEL);
                                    executor.start();
                                }).start();
                    }
                }
            }
        });
    }
}
