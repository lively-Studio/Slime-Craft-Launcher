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
package studio.lively.scl.ui;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXDialogLayout;
import com.jfoenix.controls.JFXSpinner;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;

import org.glavo.url.WebURL;
import studio.lively.scl.Metadata;
import studio.lively.scl.task.Schedulers;
import studio.lively.scl.task.Task;
import studio.lively.scl.ui.construct.DialogCloseEvent;
import studio.lively.scl.ui.construct.JFXHyperlink;
import studio.lively.scl.upgrade.RemoteVersion;
import studio.lively.scl.util.StringUtils;
import studio.lively.scl.util.versioning.VersionNumber;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;

import static studio.lively.scl.ui.FXUtils.onEscPressed;
import static studio.lively.scl.util.i18n.I18n.i18n;
import static studio.lively.scl.util.logging.Logger.LOG;

public final class UpgradeDialog extends JFXDialogLayout {

    public UpgradeDialog(RemoteVersion remoteVersion, Runnable updateRunnable) {
        maxWidthProperty().bind(Controllers.windowWidthProperty().multiply(0.7));
        maxHeightProperty().bind(Controllers.windowHeightProperty().multiply(0.7));

        setHeading(new Label(i18n("update.changelog")));
        setBody(new JFXSpinner());

        String url = null;

        Task.supplyAsync(Schedulers.io(), () -> {
            VersionNumber targetVersion = VersionNumber.asVersion(remoteVersion.version());
            VersionNumber currentVersion = VersionNumber.asVersion(Metadata.VERSION);
            if (targetVersion.compareTo(currentVersion) <= 0)
                // Downgrade update, no need to display changelog
                return null;

            Document document = Jsoup.parse(WebURL.toURL(url), 30 * 1000);
            Node node = document.selectFirst("h1[data-version=\"%s\"]".formatted(targetVersion));

            if (node == null || !"h1".equals(node.nodeName())) {
                LOG.warning("Changelog not found");
                return null;
            }

            HTMLRenderer renderer = new HTMLRenderer(uri -> {
                LOG.info("Open link: " + uri);
                FXUtils.openLink(uri.toString());
            });

            do {
                if ("h1".equals(node.nodeName())) {
                    String changelogVersion = node.attr("data-version");
                    if (StringUtils.isBlank(changelogVersion) || currentVersion.compareTo(changelogVersion) >= 0) {
                        break;
                    }
                }
                renderer.appendNode(node);
                node = node.nextSibling();
            } while (node != null);

            renderer.mergeLineBreaks();
            return renderer.render();
        }).whenComplete(Schedulers.javafx(), (result, exception) -> {
            if (exception == null) {
                if (result != null) {
                    ScrollPane scrollPane = new ScrollPane(result);
                    scrollPane.setFitToWidth(true);
                    FXUtils.smoothScrolling(scrollPane);
                    setBody(scrollPane);
                } else {
                    setBody();
                }
            } else {
                LOG.warning("Failed to load update log, trying to open it in browser");
                FXUtils.openLink(url);
                setBody();
            }
        }).start();

        JFXHyperlink openInBrowser = new JFXHyperlink(i18n("web.view_in_browser"));
        openInBrowser.setExternalLink(url);

        JFXButton updateButton = new JFXButton(i18n("update.accept"));
        updateButton.getStyleClass().add("dialog-accept");
        updateButton.setOnAction(e -> updateRunnable.run());

        JFXButton cancelButton = new JFXButton(i18n("button.cancel"));
        cancelButton.getStyleClass().add("dialog-cancel");
        cancelButton.setOnAction(e -> fireEvent(new DialogCloseEvent()));

        setActions(openInBrowser, updateButton, cancelButton);
        onEscPressed(this, cancelButton::fire);
    }
}
