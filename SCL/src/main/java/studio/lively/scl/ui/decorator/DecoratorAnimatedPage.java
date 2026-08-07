/*
 * Slime Craft Launcher
 * Copyright (C) 2020  lively-Studio <X_CODER_ocs2008@126.com> and contributors
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
package studio.lively.scl.ui.decorator;

import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;
import javafx.scene.control.SkinBase;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;

public class DecoratorAnimatedPage extends Control {

    protected final HBox bottomNav = new HBox();
    protected final StackPane center = new StackPane();

    {
        getStyleClass().add("gray-background");
        bottomNav.getStyleClass().add("bottom-navigation");
        bottomNav.setFillHeight(true);
        HBox.setHgrow(bottomNav, Priority.ALWAYS);
    }

    protected void setLeft(Node... children) {
        bottomNav.getChildren().setAll(children);
    }

    protected void setCenter(Node... children) {
        center.getChildren().setAll(children);
    }

    public HBox getLeft() {
        return bottomNav;
    }

    public StackPane getCenter() {
        return center;
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new DecoratorAnimatedPageSkin<>(this);
    }

    public static class DecoratorAnimatedPageSkin<T extends DecoratorAnimatedPage> extends SkinBase<T> {

        protected DecoratorAnimatedPageSkin(T control) {
            super(control);

            BorderPane pane = new BorderPane();
            pane.setBottom(control.bottomNav);
            pane.setCenter(control.center);
            getChildren().setAll(pane);
        }

        protected void setLeft(Node... children) {
            getSkinnable().setLeft(children);
        }

        protected void setCenter(Node... children) {
            getSkinnable().setCenter(children);
        }

    }

}
