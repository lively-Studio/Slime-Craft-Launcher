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
package studio.lively.scl.ui.account;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Tooltip;
import studio.lively.scl.auth.Account;
import studio.lively.scl.auth.authlibinjector.AuthlibInjectorAccount;
import studio.lively.scl.auth.authlibinjector.AuthlibInjectorServer;
import studio.lively.scl.game.TexturesLoader;
import studio.lively.scl.setting.Accounts;
import studio.lively.scl.ui.FXUtils;
import studio.lively.scl.ui.construct.AdvancedListItem;
import studio.lively.scl.util.StringUtils;
import studio.lively.scl.util.javafx.BindingMapping;

import static javafx.beans.binding.Bindings.createStringBinding;
import static studio.lively.scl.setting.Accounts.getAccountFactory;
import static studio.lively.scl.setting.Accounts.getLocalizedLoginTypeName;
import static studio.lively.scl.util.i18n.I18n.i18n;

public class AccountAdvancedListItem extends AdvancedListItem {
    private final Tooltip tooltip;
    private final Canvas canvas;
    private boolean tooltipInstalled;

    private final ObjectProperty<Account> account = new SimpleObjectProperty<Account>() {

        @Override
        protected void invalidated() {
            Account account = get();
            if (account == null) {
                titleProperty().unbind();
                subtitleProperty().unbind();
                setTitle(i18n("account.missing"));
                setSubtitle(i18n("account.missing.add"));
                tooltip.setText(i18n("account.create"));
                installTooltip();

                TexturesLoader.unbindAvatar(canvas);
                TexturesLoader.drawAvatar(canvas, TexturesLoader.getDefaultSkinImage());

            } else {
                titleProperty().bind(createStringBinding(() -> {
                    String profileName = account.getProfileName();
                    return StringUtils.isBlank(profileName) ? account.getProfileID().toString() : profileName;
                }, account));
                subtitleProperty().bind(accountSubtitle(account));
                uninstallTooltip();
                TexturesLoader.bindAvatar(canvas, account);
            }
        }
    };

    public AccountAdvancedListItem() {
        this(null);
    }

    public AccountAdvancedListItem(Account account) {
        tooltip = new Tooltip();

        canvas = new Canvas(32, 32);
        canvas.setMouseTransparent(true);
        AdvancedListItem.setAlignment(canvas, Pos.CENTER);

        setLeftGraphic(canvas);

        if (account != null) {
            this.accountProperty().set(account);
        } else {
            FXUtils.onScroll(this, Accounts.getAccounts(),
                    accounts -> accounts.indexOf(accountProperty().get()),
                    Accounts::setSelectedAccount);
        }
    }

    public ObjectProperty<Account> accountProperty() {
        return account;
    }

    private static ObservableValue<String> accountSubtitle(Account account) {
        if (account instanceof AuthlibInjectorAccount) {
            return BindingMapping.of(((AuthlibInjectorAccount) account).getServer(), AuthlibInjectorServer::getName);
        } else {
            return createStringBinding(() -> getLocalizedLoginTypeName(getAccountFactory(account)));
        }
    }

    private void installTooltip() {
        if (!tooltipInstalled) {
            FXUtils.installFastTooltip(this, tooltip);
            tooltipInstalled = true;
        }
    }

    private void uninstallTooltip() {
        if (tooltipInstalled) {
            Tooltip.uninstall(this, tooltip);
            tooltipInstalled = false;
        }
    }

}
