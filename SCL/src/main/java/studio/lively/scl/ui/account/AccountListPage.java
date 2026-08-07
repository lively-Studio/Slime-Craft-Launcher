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
package studio.lively.scl.ui.account;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXDialogLayout;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Label;
import javafx.scene.control.Skin;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import studio.lively.scl.auth.Account;
import studio.lively.scl.auth.authlibinjector.AuthlibInjectorServer;
import studio.lively.scl.setting.Accounts;
import studio.lively.scl.setting.SettingsManager;
import studio.lively.scl.ui.Controllers;
import studio.lively.scl.ui.DialogUtils;
import studio.lively.scl.ui.FXUtils;
import studio.lively.scl.ui.SVG;
import studio.lively.scl.ui.construct.AdvancedListItem;
import studio.lively.scl.ui.decorator.DecoratorAnimatedPage;
import studio.lively.scl.ui.decorator.DecoratorPage;
import studio.lively.scl.util.i18n.LocaleUtils;
import studio.lively.scl.util.io.NetworkUtils;
import studio.lively.scl.util.javafx.BindingMapping;
import studio.lively.scl.util.javafx.MappedObservableList;


import static studio.lively.scl.setting.SettingsManager.userSettings;
import static studio.lively.scl.util.i18n.I18n.i18n;
import static studio.lively.scl.util.javafx.ExtendedProperties.createSelectedItemPropertyFor;
import static studio.lively.scl.util.logging.Logger.LOG;

public final class AccountListPage extends DecoratorAnimatedPage implements DecoratorPage {
    static final BooleanProperty RESTRICTED = new SimpleBooleanProperty(true);

    static {
        String property = System.getProperty("scl.offline.auth.restricted", "auto");

        if ("false".equals(property)
                || "auto".equals(property) && LocaleUtils.IS_CHINA_MAINLAND
                || SettingsManager.userSettings().enableOfflineAccountProperty().get())
            RESTRICTED.set(false);
        else
            userSettings().enableOfflineAccountProperty().addListener(new ChangeListener<Boolean>() {
                @Override
                public void changed(ObservableValue<? extends Boolean> o, Boolean oldValue, Boolean newValue) {
                    if (newValue) {
                        userSettings().enableOfflineAccountProperty().removeListener(this);
                        RESTRICTED.set(false);
                    }
                }
            });
    }

    private final ObservableList<AccountListItem> items;
    private final ReadOnlyObjectWrapper<State> state = new ReadOnlyObjectWrapper<>(State.fromTitle(i18n("account.manage")));
    private final ListProperty<Account> accounts = new SimpleListProperty<>(this, "accounts", FXCollections.observableArrayList());
    private final ListProperty<AuthlibInjectorServer> authServers = new SimpleListProperty<>(this, "authServers", FXCollections.observableArrayList());
    private final ObjectProperty<Account> selectedAccount;

    public AccountListPage() {
        items = MappedObservableList.create(accounts, AccountListItem::new);
        selectedAccount = createSelectedItemPropertyFor(items, Account.class);
    }

    public ObjectProperty<Account> selectedAccountProperty() {
        return selectedAccount;
    }

    public ListProperty<Account> accountsProperty() {
        return accounts;
    }

    @Override
    public ReadOnlyObjectProperty<State> stateProperty() {
        return state.getReadOnlyProperty();
    }

    public ListProperty<AuthlibInjectorServer> authServersProperty() {
        return authServers;
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new AccountListPageSkin(this);
    }

    private static class AccountListPageSkin extends DecoratorAnimatedPageSkin<AccountListPage> {

        public AccountListPageSkin(AccountListPage skinnable) {
            super(skinnable);

            {
                VBox boxMethods = new VBox();
                boxMethods.getStyleClass().add("advanced-list-box-content");

                // Single "Add Account" button opens popup to choose type
                AdvancedListItem addAccountItem = new AdvancedListItem();
                {
                    addAccountItem.getStyleClass().add("navigation-drawer-item");
                    addAccountItem.setTitle(i18n("account.create"));
                    addAccountItem.setLeftIcon(SVG.ADD_CIRCLE);
                    addAccountItem.setOnAction(e -> showAddAccountPopup(skinnable));
                    VBox.setMargin(addAccountItem, new Insets(0, 0, 12, 0));
                }

                boxMethods.getChildren().setAll(addAccountItem);

                ScrollPane scrollPane = new ScrollPane(boxMethods);
                VBox.setVgrow(scrollPane, Priority.ALWAYS);
                setLeft(scrollPane);
            }

            ScrollPane scrollPane = new ScrollPane();
            VBox list = new VBox();
            {
                scrollPane.setFitToWidth(true);

                list.maxWidthProperty().bind(scrollPane.widthProperty());
                list.setSpacing(10);
                list.getStyleClass().add("card-list");

                Bindings.bindContent(list.getChildren(), skinnable.items);

                scrollPane.setContent(list);
                FXUtils.smoothScrolling(scrollPane);

                setCenter(scrollPane);
            }
        }

        /// Shows a popup dialog to choose account type (Microsoft / Offline / Authlib-Injector servers)
        private void showAddAccountPopup(AccountListPage skinnable) {
            JFXDialogLayout layout = new JFXDialogLayout();
            layout.setHeading(new Label(i18n("account.create")));

            VBox body = new VBox(4);

            // Microsoft
            AdvancedListItem microsoftOption = new AdvancedListItem();
            microsoftOption.setLeftIcon(SVG.MICROSOFT);
            microsoftOption.setTitle(i18n("account.methods.microsoft"));
            microsoftOption.setOnAction(e -> openMicrosoftLogin());

            // Offline
            AdvancedListItem offlineOption = new AdvancedListItem();
            offlineOption.setLeftIcon(SVG.PERSON);
            offlineOption.setTitle(i18n("account.methods.offline"));
            offlineOption.setOnAction(e -> openOfflineAccount());

            // Existing authlib-injector servers
            body.getChildren().addAll(microsoftOption, offlineOption);
            for (AuthlibInjectorServer server : skinnable.authServersProperty().get()) {
                AdvancedListItem serverItem = new AdvancedListItem();
                serverItem.setLeftIcon(SVG.DRESSER);
                serverItem.titleProperty().bind(BindingMapping.of(server, AuthlibInjectorServer::getName));
                serverItem.setOnAction(e -> openAuthlibInjectorAccount(server));
                body.getChildren().add(serverItem);
            }

            // Add new authlib-injector server
            AdvancedListItem newAuthlibOption = new AdvancedListItem();
            newAuthlibOption.setLeftIcon(SVG.ADD_CIRCLE);
            newAuthlibOption.setTitle(i18n("account.injector.add"));
            newAuthlibOption.setSubtitle(i18n("account.methods.authlib_injector"));
            newAuthlibOption.setOnAction(e -> openAddAuthlibInjectorServer());
            body.getChildren().add(newAuthlibOption);

            if (RESTRICTED.get()) {
                offlineOption.setDisable(true);
                newAuthlibOption.setDisable(true);
                HBox restrictedHint = new HBox();
                restrictedHint.setPadding(new Insets(8, 0, 0, 0));
                restrictedHint.getChildren().add(new Label(i18n("account.login.restricted")));
                body.getChildren().add(restrictedHint);
            }

            // Cancel button
            JFXButton cancelButton = new JFXButton(i18n("button.cancel"));
            cancelButton.getStyleClass().add("dialog-accept");
            cancelButton.setOnAction(e -> DialogUtils.close(layout));

            layout.setBody(body);
            layout.setActions(cancelButton);
            Controllers.dialog(layout);
        }

        private void openMicrosoftLogin() {
            if (SettingsManager.isUserGameAccountsReadOnly()) {
                confirmOverwriteUserAccounts(() -> Controllers.dialog(new MicrosoftAccountLoginPane()));
            } else {
                Controllers.dialog(new MicrosoftAccountLoginPane());
            }
        }

        private void openOfflineAccount() {
            if (SettingsManager.isUserGameAccountsReadOnly()) {
                confirmOverwriteUserAccounts(() -> Controllers.dialog(new CreateAccountPane(Accounts.FACTORY_OFFLINE)));
            } else {
                Controllers.dialog(new CreateAccountPane(Accounts.FACTORY_OFFLINE));
            }
        }

        private void openAuthlibInjectorAccount(AuthlibInjectorServer server) {
            if (SettingsManager.isUserGameAccountsReadOnly()) {
                confirmOverwriteUserAccounts(() -> Controllers.dialog(new CreateAccountPane(server)));
            } else {
                Controllers.dialog(new CreateAccountPane(server));
            }
        }

        private void openAddAuthlibInjectorServer() {
            if (SettingsManager.isAuthlibInjectorServersReadOnly()) {
                confirmOverwriteAuthlibInjectorServers(
                        () -> Controllers.dialog(new AddAuthlibInjectorServerPane()));
            } else {
                Controllers.dialog(new AddAuthlibInjectorServerPane());
            }
        }

        /// Confirms overwriting the user account files before continuing the account operation.
        private static void confirmOverwriteUserAccounts(Runnable action) {
            Controllers.confirmBackupAndOverwrite(i18n("account.storage.read_only"), () -> {
                SettingsManager.forceOverwriteUserGameAccounts();
                action.run();
            });
        }

        /// Confirms overwriting the authlib-injector server list before continuing the server operation.
        private static void confirmOverwriteAuthlibInjectorServers(Runnable action) {
            Controllers.confirmBackupAndOverwrite(i18n("account.injector.server.storage.read_only"), () -> {
                SettingsManager.forceOverwriteAuthlibInjectorServers();
                action.run();
            });
        }

        /// Asks the user to confirm removing an authlib-injector server.
        private static void confirmRemoveAuthlibInjectorServer(
                AccountListPage skinnable,
                AuthlibInjectorServer server) {
            Controllers.confirm(i18n("button.remove.confirm"), i18n("button.remove"), () -> {
                skinnable.authServersProperty().remove(server);
            }, null);
        }
    }
}
