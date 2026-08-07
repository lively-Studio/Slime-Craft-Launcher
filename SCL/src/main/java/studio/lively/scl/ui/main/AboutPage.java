/*
 * Slime Craft Launcher
 * Copyright (C) 2022  lively-Studio <X_CODER_ocs2008@126.com> and contributors
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

import com.google.gson.*;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import studio.lively.scl.Metadata;
import studio.lively.scl.theme.Themes;
import studio.lively.scl.ui.FXUtils;
import studio.lively.scl.ui.SVG;
import studio.lively.scl.ui.WeakListenerHolder;
import studio.lively.scl.ui.construct.ComponentList;
import studio.lively.scl.ui.construct.ImageContainer;
import studio.lively.scl.ui.construct.LineButton;
import studio.lively.scl.ui.Controllers;
import studio.lively.scl.ui.construct.MessageDialogPane.MessageType;
import studio.lively.scl.setting.GameDirectoryManager;
import studio.lively.scl.game.SCLGameRepository;
import studio.lively.scl.ui.construct.SpinnerPane;
import studio.lively.scl.ui.versions.Versions;
import studio.lively.scl.util.gson.JsonUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

import static studio.lively.scl.util.i18n.I18n.i18n;
import static studio.lively.scl.util.logging.Logger.LOG;

public final class AboutPage extends SpinnerPane {

    private final WeakListenerHolder holder = new WeakListenerHolder();
    private int clickCount = 0;
    private static final int EASTER_EGG_COUNT = 14;
    private static final Random RANDOM = new Random();

    public AboutPage() {
        VBox content = new VBox();
        content.getStyleClass().add("spinner-pane-content");
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        FXUtils.smoothScrolling(scrollPane);
        setContent(scrollPane);

        ComponentList about = new ComponentList();
        {
            var launcher = new LineButton();
            launcher.setLargeTitle(true);
            launcher.setLeading(FXUtils.newBuiltinImage("/assets/img/icon.png"));
            launcher.setTitle("Slime Craft Launcher");
            launcher.setSubtitle(Metadata.VERSION);
            launcher.setOnMouseClicked(e -> {
                clickCount++;
                if (clickCount % 3 == 0) {
                    int idx = RANDOM.nextInt(EASTER_EGG_COUNT) + 1;
                    String msg = i18n("about.easter_egg." + idx);
                    Controllers.dialog(msg, "SCL", MessageType.INFO);
                }
            });

            var author = LineButton.createExternalLinkButton("https://space.bilibili.com/3706963399542957?spm_id_from=333.1007.0.0");
            author.setLargeTitle(true);
            author.setLeading(FXUtils.newBuiltinImage("/assets/img/icon.png"));
            author.setTitle("仓颉- OCS");

            about.getContent().setAll(launcher, author);
        }

        ComponentList thanks = loadIconedTwoLineList("/assets/about/thanks.json");

        ComponentList deps = loadIconedTwoLineList("/assets/about/deps.json");

        ComponentList legal = new ComponentList();
        {
            var copyright = new LineButton();
            copyright.setLargeTitle(true);
            copyright.setTitle(i18n("about.copyright"));
            copyright.setSubtitle(i18n("about.copyright.statement"));

            var openSource = LineButton.createExternalLinkButton("https://github.com/lively-Studio/Slime-Craft-Launcher");
            openSource.setLargeTitle(true);
            openSource.setTitle(i18n("about.open_source"));
            openSource.setSubtitle(i18n("about.open_source.statement"));

            legal.getContent().setAll(copyright, openSource);
        }

        content.getChildren().setAll(
                ComponentList.createComponentListTitle(i18n("about")),
                about,
                ComponentList.createComponentListTitle(i18n("about.thanks_to")),
                thanks,
                ComponentList.createComponentListTitle(i18n("about.dependency")),
                deps,
                ComponentList.createComponentListTitle(i18n("about.legal")),
                legal
        );

        if (studio.lively.scl.util.AprilFools.isLaunchButtonPage(0)) {
            javafx.scene.control.Button aprilBtn = new javafx.scene.control.Button(i18n("version.launch"));
            aprilBtn.getStyleClass().add("launch-button");
            aprilBtn.setStyle("-fx-min-height: 48px; -fx-min-width: 200px; -fx-font-size: 16px;");
            aprilBtn.setOnAction(e -> {
                SCLGameRepository repo = GameDirectoryManager.getSelectedRepository();
                Versions.launch(repo, repo.getSelectedInstance());
            });
            javafx.scene.layout.HBox aprilBox = new javafx.scene.layout.HBox(aprilBtn);
            aprilBox.setPadding(new javafx.geometry.Insets(16, 0, 16, 0));
            aprilBox.setAlignment(javafx.geometry.Pos.CENTER);
            content.getChildren().add(aprilBox);
        }
    }

    private static Image loadImage(String url) {
        return url.startsWith("/")
                ? FXUtils.newBuiltinImage(url)
                : new Image(url);
    }

    private ComponentList loadIconedTwoLineList(String path) {
        ComponentList componentList = new ComponentList();

        InputStream input = FXUtils.class.getResourceAsStream(path);
        if (input == null) {
            LOG.warning("Resources not found: " + path);
            return componentList;
        }

        try {
            JsonArray array = JsonUtils.fromJsonFully(input, JsonArray.class);

            for (JsonElement element : array) {
                JsonObject obj = element.getAsJsonObject();

                var button = new LineButton();
                button.setLargeTitle(true);

                if (obj.get("externalLink") instanceof JsonPrimitive externalLink) {
                    button.setTrailingIcon(SVG.OPEN_IN_NEW);

                    String link = externalLink.getAsString();
                    button.setOnAction(event -> FXUtils.openLink(link));
                }

                if (obj.has("image")) {
                    JsonElement image = obj.get("image");
                    if (image.isJsonPrimitive()) {
                        var imageView = new ImageContainer(32, 32);
                        imageView.setImage(loadImage(image.getAsString()));
                        imageView.setMouseTransparent(true);

                        String imagePath = image.getAsString();
                        if (imagePath.contains("lively_studio")) {
                            imageView.setMouseTransparent(false);
                            button.setOnMouseClicked(e -> {
                                javafx.animation.RotateTransition rt = new javafx.animation.RotateTransition(
                                    javafx.util.Duration.millis(300), imageView);
                                rt.setByAngle(90);
                                rt.play();
                            });
                        }

                        button.setLeading(imageView);
                    } else if (image.isJsonObject()) {
                        holder.add(FXUtils.onWeakChangeAndOperate(Themes.darkModeProperty(), darkMode -> button.setLeading(darkMode
                                ? loadImage(image.getAsJsonObject().get("dark").getAsString())
                                : loadImage(image.getAsJsonObject().get("light").getAsString())
                        )));
                    }
                }

                if (obj.get("title") instanceof JsonPrimitive title)
                    button.setTitle(title.getAsString());
                else if (obj.get("titleLocalized") instanceof JsonPrimitive titleLocalized)
                    button.setTitle(i18n(titleLocalized.getAsString()));

                if (obj.get("subtitle") instanceof JsonPrimitive subtitle)
                    button.setSubtitle(subtitle.getAsString());
                else if (obj.get("subtitleLocalized") instanceof JsonPrimitive subtitleLocalized)
                    button.setSubtitle(i18n(subtitleLocalized.getAsString()));

                componentList.getContent().add(button);
            }
        } catch (IOException | JsonParseException e) {
            LOG.warning("Failed to load list: " + path, e);
        }

        return componentList;
    }
}
