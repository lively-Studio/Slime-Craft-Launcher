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

import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import studio.lively.scl.theme.Themes;
import studio.lively.scl.ui.FXUtils;
import studio.lively.scl.ui.WeakListenerHolder;
import studio.lively.scl.game.SCLGameRepository;
import studio.lively.scl.setting.GameDirectoryManager;
import studio.lively.scl.ui.construct.ComponentList;
import studio.lively.scl.ui.construct.LineButton;
import studio.lively.scl.ui.versions.Versions;
import studio.lively.scl.ui.construct.SpinnerPane;

import static studio.lively.scl.util.i18n.I18n.i18n;

import studio.lively.scl.Metadata;

public class FeedbackPage extends SpinnerPane {

    private final WeakListenerHolder holder = new WeakListenerHolder();

    public FeedbackPage() {
        VBox content = new VBox();
        content.getStyleClass().add("spinner-pane-content");
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        FXUtils.smoothScrolling(scrollPane);
        setContent(scrollPane);

        ComponentList groups = new ComponentList();
        {
            var users = LineButton.createExternalLinkButton(Metadata.GROUPS_URL);
            users.setLargeTitle(true);
            users.setLeading(FXUtils.newBuiltinImage("/assets/img/icon.png"));
            users.setTitle(i18n("contact.chat.qq_group"));
            users.setSubtitle(i18n("contact.chat.qq_group.statement"));

            var discord = LineButton.createExternalLinkButton("https://discord.gg/FdEkjJR9P");
            discord.setLargeTitle(true);
            discord.setLeading(FXUtils.newBuiltinImage("/assets/img/discord.png"));
            discord.setTitle(i18n("contact.chat.discord"));
            discord.setSubtitle(i18n("contact.chat.discord.statement"));

            groups.getContent().setAll(users, discord);
        }

        ComponentList feedback = new ComponentList();
        {
            var github = LineButton.createExternalLinkButton("https://github.com/lively-Studio/Slime-Craft-Launcher/issues/new/choose");
            github.setLargeTitle(true);
            github.setTitle(i18n("contact.feedback.github"));
            github.setSubtitle(i18n("contact.feedback.github.statement"));

            holder.add(FXUtils.onWeakChangeAndOperate(Themes.darkModeProperty(), darkMode -> {
                github.setLeading(darkMode
                        ? FXUtils.newBuiltinImage("/assets/img/github-white.png")
                        : FXUtils.newBuiltinImage("/assets/img/github.png"));
            }));

            feedback.getContent().setAll(github);
        }

        content.getChildren().addAll(
                ComponentList.createComponentListTitle(i18n("contact.chat")),
                groups,
                ComponentList.createComponentListTitle(i18n("contact.feedback")),
                feedback
        );

        if (studio.lively.scl.util.AprilFools.isLaunchButtonPage(1)) {
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
}
