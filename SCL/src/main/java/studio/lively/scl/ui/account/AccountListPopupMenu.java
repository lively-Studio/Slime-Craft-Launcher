/*
 * Slime Craft Launcher
 * Copyright (C) 2026 lively-Studio <X_CODER_ocs_2026@126.com> and contributors
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

import com.jfoenix.controls.JFXPopup;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import studio.lively.scl.auth.Account;
import studio.lively.scl.setting.Accounts;
import studio.lively.scl.ui.FXUtils;
import studio.lively.scl.ui.construct.AdvancedListBox;

import static studio.lively.scl.util.i18n.I18n.i18n;

public final class AccountListPopupMenu extends StackPane {
    public static void show(Node owner, JFXPopup.PopupVPosition vAlign, JFXPopup.PopupHPosition hAlign,
                            double initOffsetX, double initOffsetY) {
        var menu = new AccountListPopupMenu();
        JFXPopup popup = new JFXPopup(menu);
        popup.show(owner, vAlign, hAlign, initOffsetX, initOffsetY);
    }

    @SuppressWarnings("FieldCanBeLocal")
    private final BooleanBinding isEmpty = Bindings.isEmpty(Accounts.getAccounts());
    @SuppressWarnings("FieldCanBeLocal")
    private final InvalidationListener listener;

    public AccountListPopupMenu() {
        AdvancedListBox box = new AdvancedListBox();
        box.getStyleClass().add("no-padding");
        box.setPrefWidth(220);
        box.setPrefHeight(-1);
        box.setMaxHeight(260);

        listener = o -> {
            box.clear();

            for (Account account : Accounts.getAccounts()) {
                AccountAdvancedListItem item = new AccountAdvancedListItem(account);
                if (Accounts.getSelectedAccount() == account) item.setActive(true);
                item.setOnAction(e -> {
                    Accounts.setSelectedAccount(account);
                    if (getScene().getWindow() instanceof JFXPopup popup)
                        popup.hide();
                });
                box.add(item);
            }

            // https://github.com/SCL-dev/SCL/issues/5343
            Platform.runLater(() -> box.setVvalue(0.0));
        };
        listener.invalidated(null);
        Accounts.getAccounts().addListener(new WeakInvalidationListener(listener));

        Label placeholder = new Label(i18n("account.empty"));
        placeholder.setStyle("-fx-padding: 10px; -fx-text-fill: -nothing-text-tertiary; -fx-font-style: italic;");

        FXUtils.onChangeAndOperate(isEmpty, empty -> {
            getChildren().setAll(empty ? placeholder : box);
        });
    }

}
