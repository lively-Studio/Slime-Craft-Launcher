/*
 * Slime Craft Launcher
 * Copyright (C) 2026  lively-Studio <X_CODER_ocs_2026@126.com> and contributors
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
package studio.lively.scl.game;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.jetbrains.annotations.Nullable;
import studio.lively.scl.theme.ThemeColor;

import java.util.LinkedList;
import java.util.Queue;

import static studio.lively.scl.setting.SettingsManager.settings;

/// Xbox-style achievement toast popup displayed at bottom-center of the screen.
///
/// Uses the launcher's current theme color as the accent and Material SVG icons.
public final class AchievementToast {

    private static final double TOAST_WIDTH = 400;
    private static final double TOAST_HEIGHT = 68;
    private static final double BOTTOM_OFFSET = 60;
    private static final Duration FADE_IN = Duration.millis(400);
    private static final Duration FADE_OUT = Duration.millis(500);
    private static final Duration SHOW_DURATION = Duration.seconds(5);
    private static final Duration STAGGER = Duration.millis(200);

    private static final Queue<Runnable> pending = new LinkedList<>();
    private static boolean showing;

    private AchievementToast() {
    }

    /// Returns whether achievement toasts are enabled in settings.
    public static boolean isEnabled() {
        return settings().achievementToastEnabledProperty().get();
    }

    /// Shows a toast with the given name and type. No-op if disabled in settings.
    public static void show(String name, ToastType type) {
        if (!isEnabled()) return;
        Platform.runLater(() -> {
            pending.add(() -> doShow(name, type));
            if (!showing) {
                processNext();
            }
        });
    }

    private static void processNext() {
        Runnable next = pending.poll();
        if (next == null) {
            showing = false;
            return;
        }
        showing = true;
        next.run();
    }

    private static void doShow(String name, ToastType type) {
        Stage stage = new Stage();
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setAlwaysOnTop(true);

        // Resolve the current theme accent color
        @Nullable ThemeColor themeColor = settings().customThemeColorProperty().get();
        Color accent = themeColor != null ? themeColor.color() : Color.web("#5C6BC0");
        String accentHex = rgbToHex(accent);

        // Build toast layout
        HBox root = new HBox(12);
        root.setAlignment(Pos.CENTER_LEFT);
        root.setPadding(new Insets(10, 20, 10, 16));
        root.setPrefWidth(TOAST_WIDTH);
        root.setPrefHeight(TOAST_HEIGHT);
        root.setMaxWidth(TOAST_WIDTH);
        root.setMaxHeight(TOAST_HEIGHT);

        root.setStyle("""
                -fx-background-color: rgba(18, 18, 22, 0.92);
                -fx-background-radius: 34;
                -fx-border-color: %s40;
                -fx-border-width: 1;
                -fx-border-radius: 34;
                -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.55), 16, 0, 0, 4);
                """.formatted(accentHex));

        // Left accent bar
        Region accentBar = new Region();
        accentBar.setPrefWidth(4);
        accentBar.setPrefHeight(44);
        accentBar.setStyle("-fx-background-color: " + accentHex + "; -fx-background-radius: 2;");

        // SVG icon
        SVGPath svg = new SVGPath();
        svg.setContent(type.iconPath);
        svg.setFill(accent);
        svg.setStroke(accent);
        svg.setStrokeWidth(0.5);

        StackPane iconPane = new StackPane(svg);
        iconPane.setMinSize(32, 32);
        iconPane.setPrefSize(32, 32);
        iconPane.setMaxSize(32, 32);
        iconPane.setAlignment(Pos.CENTER);

        // Scale the SVG to fit the icon area
        double svgW = svg.getBoundsInLocal().getWidth();
        double svgH = svg.getBoundsInLocal().getHeight();
        // Wait for layout to compute bounds
        svg.boundsInLocalProperty().addListener((obs, old, newBounds) -> {
            if (newBounds.getWidth() > 0 && newBounds.getHeight() > 0) {
                double scale = Math.min(22.0 / newBounds.getWidth(), 22.0 / newBounds.getHeight());
                svg.setScaleX(scale);
                svg.setScaleY(scale);
            }
        });

        // Text
        VBox textBox = new VBox(2);
        Label headerLabel = new Label(type.label);
        headerLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + accentHex + ";");
        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: white;");
        textBox.getChildren().addAll(headerLabel, nameLabel);

        HBox.setHgrow(textBox, Priority.ALWAYS);
        root.getChildren().addAll(accentBar, iconPane, textBox);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);

        // Position: bottom center of primary screen
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        double x = bounds.getMinX() + (bounds.getWidth() - TOAST_WIDTH) / 2;
        double y = bounds.getMinY() + bounds.getHeight() - TOAST_HEIGHT - BOTTOM_OFFSET;
        stage.setX(x);
        stage.setY(y);

        // Animation
        root.setOpacity(0);
        root.setTranslateY(30);
        stage.show();

        Timeline animIn = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(root.opacityProperty(), 0, Interpolator.EASE_OUT),
                        new KeyValue(root.translateYProperty(), 30, Interpolator.EASE_OUT)),
                new KeyFrame(FADE_IN,
                        new KeyValue(root.opacityProperty(), 1, Interpolator.EASE_OUT),
                        new KeyValue(root.translateYProperty(), 0, Interpolator.EASE_OUT))
        );

        Timeline animOut = new Timeline(
                new KeyFrame(FADE_OUT,
                        new KeyValue(root.opacityProperty(), 0, Interpolator.EASE_IN),
                        new KeyValue(root.translateYProperty(), -20, Interpolator.EASE_IN))
        );

        animIn.setOnFinished(e -> {
            PauseTransition pause = new PauseTransition(SHOW_DURATION);
            pause.setOnFinished(e2 -> animOut.play());
            pause.play();
        });

        animOut.setOnFinished(e -> {
            stage.close();
            PauseTransition stagger = new PauseTransition(STAGGER);
            stagger.setOnFinished(e2 -> processNext());
            stagger.play();
        });

        animIn.play();
    }

    private static String rgbToHex(Color c) {
        return String.format("#%02X%02X%02X",
                (int) (c.getRed() * 255),
                (int) (c.getGreen() * 255),
                (int) (c.getBlue() * 255));
    }

    /// Toast type with header label and SVG icon path.
    public enum ToastType {
        ACHIEVEMENT("ACHIEVEMENT UNLOCKED!",
                "M9.25 12L4.75 16.5L1.75 13.5L3.15 12.1L4.75 13.65L7.85 10.5L9.25 12ZM12 22Q9.925 22 8.1 21.2125T4.925 19.075Q3.575 17.725 2.7875 15.9T2 12Q2 9.925 2.7875 8.1T4.925 4.925Q6.275 3.575 8.1 2.7875T12 2Q14.075 2 15.9 2.7875T19.075 4.925Q20.425 6.275 21.2125 8.1T22 12Q22 14.075 21.2125 15.9T19.075 19.075Q17.725 20.425 15.9 21.2125T12 22Z"),
        CHALLENGE("CHALLENGE COMPLETED!",
                "M5.65 10.025L7.6 10.85Q7.95 10.15 8.325 9.5T9.15 8.2L7.75 7.925L5.65 10.025ZM9.2 12.1L12.05 14.925Q13.1 14.525 14.3 13.7T16.55 11.825Q18.3 10.075 19.2875 7.9375T20.15 4Q18.35 3.875 16.2 4.8625T12.3 7.6Q11.25 8.65 10.425 9.85T9.2 12.1ZM13.65 10.475Q13.075 9.9 13.075 9.0625T13.65 7.65Q14.225 7.075 15.075 7.075T16.5 7.65Q17.075 8.225 17.075 9.0625T16.5 10.475Q15.925 11.05 15.075 11.05T13.65 10.475ZM14.125 18.5L16.225 16.4L15.95 15Q15.3 15.45 14.65 15.8125T13.3 16.525L14.125 18.5ZM21.95 2.175Q22.425 5.2 21.3625 8.0625T17.7 13.525L18.2 16Q18.3 16.5 18.15 16.975T17.65 17.8L13.45 22L11.35 17.075L7.075 12.8L2.15 10.7L6.325 6.5Q6.675 6.15 7.1625 6T8.15 5.95L10.625 6.45Q13.225 3.85 16.075 2.775T21.95 2.175Z"),
        GOAL("GOAL REACHED!",
                "M12 22Q9.925 22 8.1 21.2125T4.925 19.075Q3.575 17.725 2.7875 15.9T2 12Q2 9.925 2.7875 8.1T4.925 4.925Q6.275 3.575 8.1 2.7875T12 2Q14.075 2 15.9 2.7875T19.075 4.925Q20.425 6.275 21.2125 8.1T22 12Q22 14.075 21.2125 15.9T19.075 19.075Q17.725 20.425 15.9 21.2125T12 22ZM12 12Z"),
        RECIPE("RECIPE UNLOCKED!",
                "M3.075 13Q3.05 12.75 3.0375 12.5T3.025 12Q3.025 10.125 3.725 8.4875T5.65 5.6375Q6.875 4.425 8.5 3.7125T12 3Q13.875 3 15.5125 3.7125T18.3625 5.6375Q19.575 6.85 20.2875 8.4875T21 12Q21 12.25 20.9875 12.5T20.95 13H18.925Q18.975 12.75 18.9875 12.5T19 12Q19 11.75 18.9875 11.5T18.925 11H15.975Q16 11.25 16 11.5V12.5Q16 12.75 15.975 13H14V12.175Q14 11.875 13.9875 11.575T13.95 11H10.075Q10.05 11.275 10.0375 11.575T10.025 12.175V13H8.05Q8.025 12.75 8.025 12.5V11.5Q8.025 11.25 8.05 11H5.1Q5.05 11.25 5.0375 11.5T5.025 12Q5.025 12.25 5.0375 12.5T5.1 13H3.075ZM5.7 9H8.275Q8.475 7.925 8.775 7.0625T9.425 5.5Q8.225 5.95 7.25 6.8625T5.7 9ZM10.35 9H13.65Q13.4 7.925 13.025 6.9T12 5Q11.35 5.875 10.9625 6.9T10.35 9ZM15.75 9H18.325Q17.75 7.775 16.7625 6.8625T14.575 5.5Q14.925 6.25 15.2375 7.0875T15.75 9ZM11 21V20Q11 18.75 10.125 17.875T8 17H2V15H8Q9.2 15 10.2375 15.525T12 17Q12.725 16.05 13.7625 15.525T16 15H22V17H16Q14.75 17 13.875 17.875T13 20V21H11Z"),
        DEATH("PLAYER DIED!",
                "M13 13.5Q13.625 13.5 14.0625 13.0625T14.5 12Q14.5 11.375 14.0625 10.9375T13 10.5Q12.375 10.5 11.9375 10.9375T11.5 12Q11.5 12.625 11.9375 13.0625T13 13.5ZM8.25 13.5Q8.875 13.5 9.3125 13.0625T9.75 12Q9.75 11.375 9.3125 10.9375T8.25 10.5Q7.625 10.5 7.1875 10.9375T6.75 12Q6.75 12.625 7.1875 13.0625T8.25 13.5ZM13 19Q14.375 19 15.5375 18.2375T17.35 16.6Q17.5 17.3 17.6375 17.9375T17.8 18.8Q14.825 20.85 11.15 20.5L13 19ZM12 22Q9.925 22 8.1 21.2125T4.925 19.075Q3.575 17.725 2.7875 15.9T2 12Q2 9.925 2.7875 8.1T4.925 4.925Q6.275 3.575 8.1 2.7875T12 2Q14.075 2 15.9 2.7875T19.075 4.925Q20.425 6.275 21.2125 8.1T22 12Q22 14.075 21.2125 15.9T19.075 19.075Q17.725 20.425 15.9 21.2125T12 22Z"),
        GAMEMODE("GAMEMODE CHANGED!",
                "M12 7.65ZM16.35 12ZM7.65 12ZM12 16.35ZM12 10.5 9 7.5V2H15V7.5L12 10.5ZM16.5 15 13.5 12 16.5 9H22V15H16.5ZM2 15V9H7.5L10.5 12 7.5 15H2ZM9 22V16.5L12 13.5 15 16.5V22H9ZM12 7.65 13 6.65V4H11V6.65L12 7.65ZM4 13H6.65L7.65 12 6.65 11H4V13ZM11 20H13V17.35L12 16.35 11 17.35V20ZM17.35 13H20V11H17.35L16.35 12 17.35 13Z");

        final String label;
        final String iconPath;

        ToastType(String label, String iconPath) {
            this.label = label;
            this.iconPath = iconPath;
        }
    }
}
