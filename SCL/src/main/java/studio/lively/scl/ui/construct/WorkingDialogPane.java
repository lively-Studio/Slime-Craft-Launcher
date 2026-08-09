/*
 * Slime Craft Launcher
 * Copyright (C) 2025 lively-Studio <X_CODER_ocs2008@126.com> and contributors
 */
package studio.lively.scl.ui.construct;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import studio.lively.scl.ui.DialogUtils;

/// Working dialog: shows animated bar, runs action, then auto-closes.
public class WorkingDialogPane extends VBox {

    public WorkingDialogPane(String message, Runnable action) {
        getStyleClass().add("working-dialog-pane");
        setSpacing(16);
        setPadding(new javafx.geometry.Insets(20, 28, 20, 28));
        setMaxWidth(300);

        // Title
        javafx.scene.control.Label title = new javafx.scene.control.Label(message);
        title.setStyle("-fx-font-size: 14; -fx-alignment: center;");

        // Animated bar container
        StackPane barTrack = new StackPane();
        barTrack.setMinHeight(4);
        barTrack.setPrefHeight(4);
        barTrack.setMaxWidth(250);
        barTrack.getStyleClass().add("working-bar-track");
        // Clip: bar never overflows the track
        Rectangle clipRect = new Rectangle(250, 4);
        clipRect.setArcWidth(4); clipRect.setArcHeight(4);
        barTrack.setClip(clipRect);
        barTrack.widthProperty().addListener((o, old, w) -> clipRect.setWidth(w.doubleValue()));

        // Animated bar — uses CSS fill so `-nothing-accent` applies
        Rectangle bar = new Rectangle(60, 4);
        bar.setArcWidth(4);
        bar.setArcHeight(4);
        bar.getStyleClass().add("working-bar");
        bar.setFill(Color.web("#00BCD4"));

        barTrack.getChildren().add(bar);

        // Animation
        Timeline timeline = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(bar.translateXProperty(), -60, Interpolator.EASE_BOTH),
                new KeyValue(bar.widthProperty(), 60, Interpolator.EASE_BOTH)),
            new KeyFrame(Duration.millis(700),
                new KeyValue(bar.translateXProperty(), 190, Interpolator.EASE_BOTH),
                new KeyValue(bar.widthProperty(), 50, Interpolator.EASE_BOTH)),
            new KeyFrame(Duration.millis(1400),
                new KeyValue(bar.translateXProperty(), -60, Interpolator.EASE_BOTH),
                new KeyValue(bar.widthProperty(), 60, Interpolator.EASE_BOTH))
        );
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

        getChildren().addAll(title, barTrack);

        // Run action in background, close dialog when done
        new Thread(() -> {
            action.run();
            Platform.runLater(() -> {
                timeline.stop();
                DialogUtils.close(this);
            });
        }, "SCL-Working").start();
    }
}
