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

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXPopup;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import studio.lively.scl.Metadata;
import studio.lively.scl.auth.Account;
import studio.lively.scl.event.EventBus;
import studio.lively.scl.event.RefreshedVersionsEvent;
import studio.lively.scl.game.SCLGameRepository;
import studio.lively.scl.game.ModpackHelper;
import studio.lively.scl.game.TexturesLoader;
import studio.lively.scl.game.Version;
import studio.lively.scl.setting.Accounts;
import studio.lively.scl.setting.GameDirectory;
import studio.lively.scl.setting.GameDirectoryManager;
import studio.lively.scl.task.Schedulers;
import studio.lively.scl.task.Task;
import studio.lively.scl.ui.Controllers;
import studio.lively.scl.ui.FXUtils;
import studio.lively.scl.ui.SVG;
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
import studio.lively.scl.util.javafx.BindingMapping;
import studio.lively.scl.util.platform.*;
import studio.lively.scl.util.versioning.VersionNumber;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static javafx.beans.binding.Bindings.createStringBinding;
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

            // ===== Center: Account avatar + name in the middle, launch button at the bottom =====
            VBox centerContent = new VBox();
            centerContent.setAlignment(Pos.CENTER);
            centerContent.setSpacing(24);
            centerContent.setPadding(new Insets(40, 20, 20, 20));

            // Player avatar canvas (72x72, larger than sidebar version)
            Canvas avatarCanvas = new Canvas(72, 72);
            avatarCanvas.getStyleClass().add("avatar-large");
            TexturesLoader.drawAvatar(avatarCanvas, TexturesLoader.getDefaultSkinImage());

            // Player name label
            Label playerNameLabel = new Label(i18n("account.missing"));
            playerNameLabel.getStyleClass().add("player-name-label");
            playerNameLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

            // Bind avatar and name to selected account
            FXUtils.onChangeAndOperate(Accounts.selectedAccountProperty(), account -> {
                if (account == null) {
                    TexturesLoader.unbindAvatar(avatarCanvas);
                    TexturesLoader.drawAvatar(avatarCanvas, TexturesLoader.getDefaultSkinImage());
                    playerNameLabel.setText(i18n("account.missing"));
                } else {
                    TexturesLoader.bindAvatar(avatarCanvas, account);
                    playerNameLabel.textProperty().bind(createStringBinding(() -> {
                        String profileName = account.getProfileName();
                        return StringUtils.isBlank(profileName) ? account.getProfileID().toString() : profileName;
                    }, account));
                }
            });

            // Account button: avatar + name, click to navigate to account page
            VBox accountButton = new VBox();
            accountButton.setAlignment(Pos.CENTER);
            accountButton.setSpacing(12);
            accountButton.getStyleClass().add("account-button");
            accountButton.getChildren().setAll(avatarCanvas, playerNameLabel);
            accountButton.setOnMouseClicked(e -> Controllers.navigate(Controllers.getAccountListPage()));

            // Launch button
            JFXButton launchButton = new JFXButton();
            launchButton.getStyleClass().add("launch-button");
            launchButton.setDefaultButton(true);
            launchButton.setMaxWidth(250);
            {
                VBox graphic = new VBox();
                graphic.setAlignment(Pos.CENTER);
                Label launchLabel = new Label();
                launchLabel.setStyle("-fx-font-size: 18px;");
                Label currentLabel = new Label();
                currentLabel.setStyle("-fx-font-size: 12px;");

                FXUtils.onChangeAndOperate(getSkinnable().getMainPage().currentGameProperty(), currentGame -> {
                    if (currentGame == null) {
                        launchLabel.setText(i18n("version.launch.empty"));
                        currentLabel.setText(null);
                        graphic.getChildren().setAll(launchLabel);
                        FXUtils.setOnActionWithCooldown(launchButton, getSkinnable().getMainPage()::launchNoGame);
                    } else {
                        launchLabel.setText(i18n("version.launch"));
                        currentLabel.setText(currentGame);
                        graphic.getChildren().setAll(launchLabel, currentLabel);
                        FXUtils.setOnActionWithCooldown(launchButton, getSkinnable().getMainPage()::launch);
                    }
                });

                launchButton.setGraphic(graphic);
            }

            centerContent.getChildren().addAll(accountButton, launchButton);
            VBox.setVgrow(accountButton, javafx.scene.layout.Priority.ALWAYS);

            // ===== Bottom navigation bar =====
            GameAdvancedListItem gameListItem = new GameAdvancedListItem();
            gameListItem.getStyleClass().add("navigation-drawer-item");
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
            FXUtils.installFastTooltip(gameListItem, new Tooltip(i18n("version.manage")));

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
            FXUtils.installFastTooltip(downloadItem, new Tooltip(i18n("download")));

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
            FXUtils.installFastTooltip(launcherSettingsItem, new Tooltip(i18n("settings")));

            AdvancedListBox bottomNav = new AdvancedListBox(true /* horizontal */);
            bottomNav.setSpacing(4);
            bottomNav.add(gameListItem)
                    .add(downloadItem)
                    .add(launcherSettingsItem);

            setLeft(bottomNav);
            setCenter(centerContent);
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
