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
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import studio.lively.scl.Metadata;
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
import studio.lively.scl.terracotta.TerracottaMetadata;
import studio.lively.scl.ui.Controllers;
import studio.lively.scl.ui.FXUtils;
import studio.lively.scl.ui.SVG;
import studio.lively.scl.ui.SVGContainer;
import studio.lively.scl.ui.animation.AnimationUtils;
import studio.lively.scl.ui.construct.AdvancedListBox;
import studio.lively.scl.ui.construct.AdvancedListItem;
import studio.lively.scl.ui.construct.MessageDialogPane;
import studio.lively.scl.ui.construct.WorkingDialogPane;
import studio.lively.scl.ui.decorator.DecoratorAnimatedPage;
import studio.lively.scl.ui.decorator.DecoratorPage;
import studio.lively.scl.ui.download.ModpackInstallWizardProvider;
import studio.lively.scl.ui.nbt.NBTEditorPage;
import studio.lively.scl.ui.nbt.NBTFileType;
import studio.lively.scl.ui.versions.GameAdvancedListItem;
import studio.lively.scl.ui.versions.GameListPopupMenu;
import studio.lively.scl.ui.versions.Versions;
import studio.lively.scl.util.AprilFools;
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
    public ReadOnlyObjectProperty<State> stateProperty() { return getMainPage().stateProperty(); }

    @Override
    protected Skin createDefaultSkin() { return new Skin(this); }

    public MainPage getMainPage() {
        if (mainPage == null) {
            MainPage mainPage = new MainPage();
            FXUtils.applyDragListener(mainPage,
                    file -> ModpackHelper.isFileModpackByExtension(file) || NBTFileType.isNBTFileByExtension(file) || "json".equalsIgnoreCase(FileUtils.getExtension(file)),
                    modpacks -> {
                        Path file = modpacks.get(0);
                        if (ModpackHelper.isFileModpackByExtension(file)) {
                            Controllers.getDecorator().startWizard(new ModpackInstallWizardProvider(GameDirectoryManager.getSelectedRepository(), file), i18n("install.modpack"));
                        } else if (NBTFileType.isNBTFileByExtension(file)) {
                            try { Controllers.navigate(new NBTEditorPage(file)); }
                            catch (Throwable e) { LOG.warning("Fail to open nbt file", e); Controllers.dialog(i18n("nbt.open.failed") + "\n\n" + StringUtils.getStackTrace(e), i18n("message.error"), MessageDialogPane.MessageType.ERROR); }
                        } else if ("json".equalsIgnoreCase(FileUtils.getExtension(file))) {
                            Versions.installFromJson(GameDirectoryManager.getSelectedRepository(), file);
                        }
                    });
            FXUtils.onChangeAndOperate(GameDirectoryManager.selectedInstanceProperty(), mainPage::setCurrentGame);
            mainPage.latestVersionProperty().bind(UpdateChecker.latestVersionProperty());
            GameDirectoryManager.registerVersionsListener(repository -> {
                GameDirectory gd = repository.getGameDirectory();
                List<Version> children = repository.getVersions().parallelStream().filter(v -> !v.isHidden())
                        .sorted(Comparator.comparing((Version v) -> Lang.requireNonNullElse(v.getReleaseTime(), Instant.EPOCH))
                                .thenComparing(v -> VersionNumber.asVersion(repository.getGameVersion(v).orElse(v.getId()))))
                        .collect(Collectors.toList());
                runInFX(() -> { if (gd == GameDirectoryManager.getSelectedGameDirectory()) mainPage.initVersions(repository, children); });
            });
            this.mainPage = mainPage;
        }
        return mainPage;
    }

    private boolean checkedModpack = false;

    private void onRefreshedVersions(SCLGameRepository repository) {
        runInFX(() -> {
            if (!checkedModpack) { checkedModpack = true;
                if (repository.getVersionCount() == 0) {
                    Path zip = Metadata.CURRENT_DIRECTORY.resolve("modpack.zip");
                    Path mrpack = Metadata.CURRENT_DIRECTORY.resolve("modpack.mrpack");
                    Path f = Files.exists(zip) ? zip : Files.exists(mrpack) ? mrpack : null;
                    if (f != null) {
                        Task.supplyAsync(() -> CompressingUtils.findSuitableEncoding(f))
                                .thenApplyAsync(e -> ModpackHelper.readModpackManifest(f, e))
                                .thenApplyAsync(m -> ModpackHelper.getInstallTask(repository, f, m.getName(), m, null).executor())
                                .thenAcceptAsync(Schedulers.javafx(), ex -> { Controllers.taskDialog(ex, i18n("modpack.installing"), TaskCancellationAction.NO_CANCEL); ex.start(); }).start();
                    }
                }
            }
        });
    }

    // ── Skin: Modern Layout ──
    private static class Skin extends DecoratorAnimatedPageSkin<RootPage> {
        protected Skin(RootPage control) {
            super(control);
            control.getStylesheets().add("assets/css/new-root.css");

            Canvas av = new Canvas(56, 56);
            TexturesLoader.drawAvatar(av, TexturesLoader.getDefaultSkinImage());
            Label nm = new Label(i18n("account.missing")); nm.getStyleClass().add("profile-name");
            Label ht = new Label(i18n("account").toUpperCase(Locale.ROOT)); ht.getStyleClass().add("profile-hint");
            FXUtils.onChangeAndOperate(Accounts.selectedAccountProperty(), a -> {
                if (a == null) { TexturesLoader.unbindAvatar(av); TexturesLoader.drawAvatar(av, TexturesLoader.getDefaultSkinImage()); nm.setText(i18n("account.missing")); }
                else { TexturesLoader.bindAvatar(av, a); nm.textProperty().bind(createStringBinding(() -> { String n = a.getProfileName(); return StringUtils.isBlank(n) ? a.getProfileID().toString() : n; }, a)); }
            });
            VBox card = new VBox(8, new VBox(av){{getStyleClass().add("avatar-box");}}, nm, ht);
            card.getStyleClass().add("profile-card"); card.setAlignment(Pos.CENTER);
            card.setOnMouseClicked(e -> Controllers.navigate(Controllers.getAccountListPage()));

            HBox qa = new HBox(10); qa.setAlignment(Pos.CENTER);
            qa.getChildren().addAll(
                actCard(SVG.FORMAT_LIST_BULLETED, i18n("version.manage"), () -> runAction(() -> runInFX(() -> Controllers.navigate(Controllers.getGameListPage())))),
                actCard(SVG.PUBLIC, i18n("terracotta"), () -> runAction(() -> { if (TerracottaMetadata.PROVIDER != null) Controllers.navigate(Controllers.getTerracottaPage()); else Controllers.dialog(i18n("terracotta.unsupported"), null, MessageDialogPane.MessageType.WARNING); })),
                actCard(SVG.SETTINGS, i18n("settings"), () -> runAction(() -> { Controllers.getSettingsPage().showGameSettings(GameDirectoryManager.getSelectedRepository()); Controllers.navigate(Controllers.getSettingsPage()); }))
            );

            JFXButton lb = new JFXButton(); lb.getStyleClass().add("launch-btn"); lb.setMaxWidth(260); lb.setDefaultButton(true); if (AprilFools.isEnabled()) { lb.setVisible(false); lb.setManaged(false); }
            Label ls = new Label(); ls.getStyleClass().add("launch-version");
            VBox lg = new VBox(2, new Label(i18n("version.launch")), ls); lg.setAlignment(Pos.CENTER); lb.setGraphic(lg);
            FXUtils.onChangeAndOperate(getSkinnable().getMainPage().currentGameProperty(), g -> {
                lg.getChildren().clear();
                if (g == null) { lg.getChildren().add(new Label(i18n("version.launch.empty"))); FXUtils.setOnActionWithCooldown(lb, getSkinnable().getMainPage()::launchNoGame); }
                else { lg.getChildren().addAll(new Label(i18n("version.launch")), new Label(g)); FXUtils.setOnActionWithCooldown(lb, getSkinnable().getMainPage()::launch); }
            });

            AdvancedListBox nav = new AdvancedListBox(true); nav.setSpacing(4);
            GameAdvancedListItem ga = new GameAdvancedListItem(); ga.setCompact(true); ga.getStyleClass().add("navigation-drawer-item");
            ga.setOnAction(e -> { String v = GameDirectoryManager.getSelectedRepository().getSelectedInstance(); if (v == null) Controllers.navigate(Controllers.getGameListPage()); else Versions.modifyGameSettings(GameDirectoryManager.getSelectedRepository(), v); });
            if (AnimationUtils.isAnimationEnabled()) FXUtils.prepareOnMouseEnter(ga, Controllers::prepareVersionPage);
            FXUtils.installFastTooltip(ga, new Tooltip(i18n("version.manage")));
            nav.add(ga).add(navItem(SVG.DOWNLOAD, i18n("download"), () -> { Controllers.getDownloadPage().showGameDownloads(); Controllers.navigate(Controllers.getDownloadPage()); })).add(navItem(SVG.SETTINGS, i18n("settings"), () -> { Controllers.getSettingsPage().showGameSettings(GameDirectoryManager.getSelectedRepository()); Controllers.navigate(Controllers.getSettingsPage()); }));

            VBox mc = new VBox(8,
                new VBox(card){{getStyleClass().add("profile-section");}},
                new VBox(6, new Label(i18n("version.manage").toUpperCase(Locale.ROOT)){{getStyleClass().add("actions-title");}}, qa){{getStyleClass().add("actions-section");}},
                new VBox(lb){{getStyleClass().add("launch-section");}}
            );
            mc.setAlignment(Pos.TOP_CENTER);
            VBox.setVgrow(mc.getChildren().get(2), Priority.ALWAYS);
            setCenter(mc);
            setLeft(nav);
        }

        /// Show working dialog only if enabled in settings.
        private void runAction(Runnable action) {
            if (studio.lively.scl.setting.SettingsManager.settings().showWorkingDialogProperty().get())
                Controllers.dialog(new WorkingDialogPane(i18n("message.working"), action));
            else
                action.run();
        }

        private VBox actCard(SVG ico, String t, Runnable r) {
            SVGContainer c = new SVGContainer(ico, 20); c.getStyleClass().add("action-icon");
            VBox card = new VBox(4, c, new Label(t){{getStyleClass().add("action-label");}});
            card.getStyleClass().add("action-card"); card.setAlignment(Pos.CENTER);
            card.setOnMouseClicked(e -> r.run()); return card;
        }

        private AdvancedListItem navItem(SVG ico, String t, Runnable r) {
            AdvancedListItem it = new AdvancedListItem(); it.setLeftIcon(ico); it.setTitle(t); it.setOnAction(e -> r.run());
            return it;
        }

        public void showGameListPopupMenu(Region item) {
            GameListPopupMenu.show(item, JFXPopup.PopupVPosition.TOP, JFXPopup.PopupHPosition.LEFT, item.getWidth(), 0,
                getSkinnable().getMainPage().getRepository(), getSkinnable().getMainPage().getVersions());
        }
    }
}
