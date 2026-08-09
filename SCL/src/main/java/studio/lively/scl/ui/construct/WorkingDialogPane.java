/*
 * Slime Craft Launcher
 * Copyright (C) 2025 lively-Studio <X_CODER_ocs2008@126.com> and contributors
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
package studio.lively.scl.ui.construct;

import javafx.animation.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/// A simple "working" dialog with an animated themed bar.
public class WorkingDialogPane extends VBox {

    public WorkingDialogPane(String message) {
        getStyleClass().add("working-dialog-pane");
        setSpacing(16);
        setPadding(new javafx.geometry.Insets(16, 24, 24, 24));
        setMaxWidth(320);

        // Title
        javafx.scene.control.Label title = new javafx.scene.control.Label(message);
        title.getStyleClass().add("working-title");
        title.setStyle("-fx-font-size: 14; -fx-text-fill: -nothing-on-surface; -fx-font-weight: bold;");

        // Animated bar container
        StackPane barTrack = new StackPane();
        barTrack.setMinHeight(4);
        barTrack.setPrefHeight(4);
        barTrack.setStyle("-fx-background-color: -nothing-outline; -fx-background-radius: 2;");

        // Animated bar
        Rectangle bar = new Rectangle(80, 4);
        bar.setArcWidth(4);
        bar.setArcHeight(4);
        bar.setFill(javafx.scene.paint.Color.web("#00BCD4"));

        barTrack.getChildren().add(bar);

        // Animation: bar slides left ↔ right
        Timeline timeline = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(bar.translateXProperty(), -80),
                new KeyValue(bar.widthProperty(), 80)),
            new KeyFrame(Duration.millis(800),
                new KeyValue(bar.translateXProperty(), 240),
                new KeyValue(bar.widthProperty(), 60)),
            new KeyFrame(Duration.millis(1600),
                new KeyValue(bar.translateXProperty(), -80),
                new KeyValue(bar.widthProperty(), 80))
        );
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.setInterpolator(Interpolator.EASE_BOTH);
        timeline.play();

        // Clean up animation when removed
        sceneProperty().addListener((obs, old, val) -> {
            if (val == null) timeline.stop();
        });

        getChildren().addAll(title, barTrack);
    }
}
