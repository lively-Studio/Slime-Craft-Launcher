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
package studio.lively.scl.ui.main;

import com.google.gson.annotations.SerializedName;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import studio.lively.scl.game.SCLGameRepository;
import studio.lively.scl.setting.GameDirectoryManager;
import studio.lively.scl.ui.FXUtils;
import studio.lively.scl.ui.construct.SpinnerPane;
import studio.lively.scl.ui.versions.Versions;
import studio.lively.scl.util.gson.JsonSerializable;

import java.util.List;

import static studio.lively.scl.util.gson.JsonUtils.listTypeOf;
import static studio.lively.scl.util.i18n.I18n.i18n;

public class HelpPage extends SpinnerPane {

    private final VBox content;

    public HelpPage() {
        content = new VBox();
        content.getStyleClass().add("spinner-pane-content");
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        FXUtils.smoothScrolling(scrollPane);
        setContent(scrollPane);

        if (studio.lively.scl.util.AprilFools.isLaunchButtonPage(2)) {
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

    @JsonSerializable
    private record HelpCategory(
            @SerializedName("title") String title,
            @SerializedName("items") List<Help> items) {
    }

    @JsonSerializable
    private record Help(
            @SerializedName("title") String title,
            @SerializedName("subtitle") String subtitle,
            @SerializedName("url") String url) {
    }
}
