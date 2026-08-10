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
package studio.lively.scl.ui.main;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import studio.lively.scl.game.SCLGameRepository;
import studio.lively.scl.setting.GameSettings;
import studio.lively.scl.setting.GameDirectoryManager;
import studio.lively.scl.ui.FXUtils;
import studio.lively.scl.ui.SVG;
import studio.lively.scl.ui.animation.TransitionPane;
import studio.lively.scl.ui.construct.*;
import studio.lively.scl.ui.decorator.DecoratorAnimatedPage;
import studio.lively.scl.ui.decorator.DecoratorPage;
import studio.lively.scl.ui.game.GameSettingsPage;

import static studio.lively.scl.util.i18n.I18n.i18n;

public class LauncherSettingsPage extends DecoratorAnimatedPage implements DecoratorPage, PageAware {
    private final ReadOnlyObjectWrapper<State> state = new ReadOnlyObjectWrapper<>(State.fromTitle(i18n("settings")));
    private final TabHeader tab;
    private final TabHeader.Tab<GameSettingsPage<GameSettings.Preset>> gameTab = new TabHeader.Tab<>("versionSettingsPage");
    private final TabControl.Tab<JavaManagementPage> javaManagementTab = new TabControl.Tab<>("javaManagementPage");
    private final TabHeader.Tab<SettingsPage> settingsTab = new TabHeader.Tab<>("settingsPage");
    private final TabHeader.Tab<PersonalizationPage> personalizationTab = new TabHeader.Tab<>("personalizationPage");
    private final TabHeader.Tab<DownloadSettingsPage> downloadTab = new TabHeader.Tab<>("downloadSettingsPage");
    private final TabHeader.Tab<HelpPage> helpTab = new TabHeader.Tab<>("helpPage");
    private final TabHeader.Tab<AboutPage> aboutTab = new TabHeader.Tab<>("aboutPage");
    private final TabHeader.Tab<FeedbackPage> feedbackTab = new TabHeader.Tab<>("feedbackPage");
    private final TransitionPane transitionPane = new TransitionPane();

    public LauncherSettingsPage() {
        gameTab.setNodeSupplier(() -> new GameSettingsPage<>(GameSettings.Preset.class));
        javaManagementTab.setNodeSupplier(JavaManagementPage::new);
        settingsTab.setNodeSupplier(SettingsPage::new);
        personalizationTab.setNodeSupplier(PersonalizationPage::new);
        downloadTab.setNodeSupplier(DownloadSettingsPage::new);
        helpTab.setNodeSupplier(HelpPage::new);
        feedbackTab.setNodeSupplier(FeedbackPage::new);
        aboutTab.setNodeSupplier(AboutPage::new);
        tab = new TabHeader(transitionPane, gameTab, javaManagementTab, settingsTab, personalizationTab, downloadTab, helpTab, feedbackTab, aboutTab);

        tab.select(gameTab);
        addEventHandler(Navigator.NavigationEvent.NAVIGATED, event -> gameTab.getNode().loadInstance(GameDirectoryManager.getSelectedRepository(), null));

        AdvancedListBox sideBar = new AdvancedListBox(true /* horizontal */);
        sideBar.setSpacing(4);
        sideBar.addNavigationDrawerTab(tab, gameTab, i18n("settings.type.global.manage"), SVG.STADIA_CONTROLLER, SVG.STADIA_CONTROLLER_FILL)
                .addNavigationDrawerTab(tab, javaManagementTab, i18n("java.management"), SVG.LOCAL_CAFE, SVG.LOCAL_CAFE_FILL)
                .addNavigationDrawerTab(tab, settingsTab, i18n("settings.launcher.general"), SVG.TUNE)
                .addNavigationDrawerTab(tab, personalizationTab, i18n("settings.launcher.appearance"), SVG.STYLE, SVG.STYLE_FILL)
                .addNavigationDrawerTab(tab, downloadTab, i18n("download"), SVG.DOWNLOAD)
                .addNavigationDrawerTab(tab, feedbackTab, i18n("contact"), SVG.FEEDBACK, SVG.FEEDBACK_FILL)
                .addNavigationDrawerTab(tab, aboutTab, i18n("about"), SVG.INFO, SVG.INFO_FILL);
        setLeft(sideBar);

        setCenter(transitionPane);
    }

    @Override
    public void onPageShown() {
        tab.onPageShown();
    }

    @Override
    public void onPageHidden() {
        tab.onPageHidden();
    }

    public void showGameSettings(SCLGameRepository repository) {
        gameTab.getNode().loadInstance(repository, null);
        tab.select(gameTab, false);
    }

    public void showFeedback() {
        tab.select(feedbackTab, false);
    }

    @Override
    public ReadOnlyObjectProperty<State> stateProperty() {
        return state.getReadOnlyProperty();
    }
}
