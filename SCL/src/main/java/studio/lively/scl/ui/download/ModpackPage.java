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
package studio.lively.scl.ui.download;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextField;
import javafx.beans.property.StringProperty;
import javafx.geometry.Pos;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import studio.lively.scl.game.SCLGameRepository;
import studio.lively.scl.setting.GameDirectory;
import studio.lively.scl.ui.FXUtils;
import studio.lively.scl.ui.construct.ComponentList;
import studio.lively.scl.ui.construct.LinePane;
import studio.lively.scl.ui.construct.LineTextPane;
import studio.lively.scl.ui.construct.SpinnerPane;
import studio.lively.scl.ui.wizard.WizardController;
import studio.lively.scl.ui.wizard.WizardPage;
import studio.lively.scl.util.SettingsMap;

import static javafx.beans.binding.Bindings.createBooleanBinding;
import static studio.lively.scl.util.i18n.I18n.i18n;

public abstract class ModpackPage extends SpinnerPane implements WizardPage {
    public static final SettingsMap.Key<GameDirectory> GAME_DIRECTORY = new SettingsMap.Key<>("GAME_DIRECTORY");
    public static final SettingsMap.Key<SCLGameRepository> REPOSITORY = new SettingsMap.Key<>("REPOSITORY");

    protected final WizardController controller;

    protected final StringProperty nameProperty;
    protected final StringProperty versionProperty;
    protected final StringProperty authorProperty;
    protected final JFXTextField txtModpackName;
    protected final JFXButton btnInstall;
    protected final JFXButton btnDescription;

    protected ModpackPage(WizardController controller) {
        this.controller = controller;

        VBox borderPane = new VBox();
        borderPane.setAlignment(Pos.CENTER);
        FXUtils.setLimitWidth(borderPane, 500);

        ComponentList componentList = new ComponentList();
        {
            var archiveNamePane = new LinePane();
            {
                FXUtils.setLimitHeight(archiveNamePane, 75);

                archiveNamePane.setTitle(i18n("version.name"));

                txtModpackName = new JFXTextField();
                txtModpackName.setPrefWidth(300);
                // BorderPane.setMargin(txtModpackName, new Insets(0, 0, 8, 32));
                BorderPane.setAlignment(txtModpackName, Pos.CENTER_RIGHT);
                archiveNamePane.setRight(txtModpackName);
            }

            var modpackNamePane = new LineTextPane();
            {
                modpackNamePane.setTitle(i18n("modpack.name"));
                nameProperty = modpackNamePane.textProperty();
            }

            var versionPane = new LineTextPane();
            {
                versionPane.setTitle(i18n("archive.version"));
                versionProperty = versionPane.textProperty();
            }

            var authorPane = new LineTextPane();
            {
                authorPane.setTitle(i18n("archive.author"));
                authorProperty = authorPane.textProperty();
            }

            var descriptionPane = new BorderPane();
            {
                btnDescription = FXUtils.newBorderButton(i18n("modpack.description"));
                btnDescription.setOnAction(e -> onDescribe());
                descriptionPane.setLeft(btnDescription);

                btnInstall = FXUtils.newRaisedButton(i18n("button.install"));
                btnInstall.setOnAction(e -> onInstall());
                btnInstall.disableProperty().bind(createBooleanBinding(() -> !txtModpackName.validate(), txtModpackName.textProperty()));
                descriptionPane.setRight(btnInstall);
            }

            componentList.getContent().setAll(
                    archiveNamePane, modpackNamePane, versionPane, authorPane, descriptionPane);
        }

        borderPane.getChildren().setAll(componentList);
        this.setContent(borderPane);
    }

    protected abstract void onInstall();

    protected abstract void onDescribe();

    @Override
    public String getTitle() {
        return i18n("modpack.task.install");
    }
}
