/*
 * Slime Craft Launcher
 * Copyright (C) 2025  lively-Studio <X_CODER_ocs2008@126.com> and contributors
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
package studio.lively.scl.ui.terracotta;

import com.jfoenix.controls.JFXPopup;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ChangeListener;
import studio.lively.scl.setting.*;
import studio.lively.scl.terracotta.TerracottaMetadata;
import studio.lively.scl.ui.Controllers;
import studio.lively.scl.ui.FXUtils;
import studio.lively.scl.ui.SVG;
import studio.lively.scl.ui.account.AccountAdvancedListItem;
import studio.lively.scl.ui.account.AccountListPopupMenu;
import studio.lively.scl.ui.animation.TransitionPane;
import studio.lively.scl.ui.construct.*;
import studio.lively.scl.ui.decorator.DecoratorAnimatedPage;
import studio.lively.scl.ui.decorator.DecoratorPage;
import studio.lively.scl.ui.main.MainPage;
import studio.lively.scl.ui.versions.GameListPopupMenu;
import studio.lively.scl.ui.versions.Versions;
import studio.lively.scl.util.Lang;
import studio.lively.scl.util.StringUtils;

import static studio.lively.scl.setting.SettingsManager.userState;
import static studio.lively.scl.util.i18n.I18n.i18n;

public class TerracottaPage extends DecoratorAnimatedPage implements DecoratorPage, PageAware {
    private static final int TERRACOTTA_AGREEMENT_VERSION = 2;

    private final ReadOnlyObjectWrapper<State> state = new ReadOnlyObjectWrapper<>(State.fromTitle(i18n("terracotta.terracotta")));
    private final TabHeader tab;
    private final TabHeader.Tab<TerracottaControllerPage> statusPage = new TabHeader.Tab<>("statusPage");
    private final TransitionPane transitionPane = new TransitionPane();

    @SuppressWarnings("unused")
    private ChangeListener<String> instanceChangeListenerHolder;

    public TerracottaPage() {
        statusPage.setNodeSupplier(TerracottaControllerPage::new);
        tab = new TabHeader(transitionPane, statusPage);
        tab.select(statusPage);

        AdvancedListBox sideBar = new AdvancedListBox(true /* horizontal */);
        sideBar.addNavigationDrawerTab(tab, statusPage, i18n("terracotta.status"), SVG.TUNE);

        AccountAdvancedListItem accountListItem = new AccountAdvancedListItem();
        accountListItem.setOnAction(e -> Controllers.navigate(Controllers.getAccountListPage()));
        accountListItem.accountProperty().bind(Accounts.selectedAccountProperty());
        FXUtils.onSecondaryButtonClicked(accountListItem, () -> AccountListPopupMenu.show(accountListItem, JFXPopup.PopupVPosition.BOTTOM, JFXPopup.PopupHPosition.LEFT, accountListItem.getWidth(), 0));

        sideBar.add(accountListItem)
                .addNavigationDrawerItem(i18n("version.launch"), SVG.ROCKET_LAUNCH, () -> {
                    var repository = GameDirectoryManager.getSelectedRepository();
                    Versions.launch(repository, repository.getSelectedInstance(), launcherHelper -> {
                        launcherHelper.setKeep();
                        launcherHelper.setDisableOfflineSkin();
                    });
                }, item -> {
                    instanceChangeListenerHolder = FXUtils.onWeakChangeAndOperate(GameDirectoryManager.selectedInstanceProperty(),
                            instanceName -> item.setSubtitle(StringUtils.isNotBlank(instanceName) ? instanceName : i18n("version.empty"))
                    );

                    MainPage mainPage = Controllers.getRootPage().getMainPage();
                    FXUtils.onScroll(item, mainPage.getVersions(), list -> {
                        String currentId = mainPage.getCurrentGame();
                        return Lang.indexWhere(list, instance -> instance.getId().equals(currentId));
                    }, it -> mainPage.getRepository().setSelectedInstance(it.getId()));

                    FXUtils.onSecondaryButtonClicked(item, () -> GameListPopupMenu.show(item,
                            JFXPopup.PopupVPosition.BOTTOM,
                            JFXPopup.PopupHPosition.LEFT,
                            item.getWidth(),
                            0,
                            mainPage.getRepository(), mainPage.getVersions()));
                })
        setLeft(sideBar);

        setCenter(transitionPane);
    }

    @Override
    public void onPageShown() {
        tab.onPageShown();

        if (SettingsManager.userState().terracottaAgreementVersionProperty().get() < TERRACOTTA_AGREEMENT_VERSION) {
            Controllers.confirmWithCountdown(i18n("terracotta.confirm.desc"), i18n("terracotta.confirm.title"), 5, MessageDialogPane.MessageType.INFO, () -> {
                UserState userState = userState();
                userState.terracottaAgreementVersionProperty().set(TERRACOTTA_AGREEMENT_VERSION);
            }, () -> fireEvent(new PageCloseEvent()));
        }
    }

    @Override
    public void onPageHidden() {
        tab.onPageHidden();
    }

    @Override
    public ReadOnlyObjectProperty<State> stateProperty() {
        return state.getReadOnlyProperty();
    }
}
