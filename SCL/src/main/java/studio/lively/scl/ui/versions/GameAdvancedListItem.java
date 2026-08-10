/*
 * Slime Craft Launcher
 * Copyright (C) 2020  lively-Studio <X_CODER_ocs_2026@126.com> and contributors
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
package studio.lively.scl.ui.versions;

import javafx.geometry.Pos;
import studio.lively.scl.event.Event;
import studio.lively.scl.event.EventBus;
import studio.lively.scl.event.RefreshedVersionsEvent;
import studio.lively.scl.game.SCLGameRepository;
import studio.lively.scl.setting.GameDirectoryManager;
import studio.lively.scl.setting.VersionIconType;
import studio.lively.scl.ui.FXUtils;
import studio.lively.scl.ui.WeakListenerHolder;
import studio.lively.scl.ui.construct.AdvancedListItem;
import studio.lively.scl.ui.construct.ImageContainer;

import java.util.function.Consumer;

import static studio.lively.scl.util.i18n.I18n.i18n;

public class GameAdvancedListItem extends AdvancedListItem {
    private final ImageContainer imageContainer;
    private final WeakListenerHolder holder = new WeakListenerHolder();
    private SCLGameRepository repository;
    @SuppressWarnings({"unused", "FieldCanBeLocal"})
    private Consumer<Event> onVersionIconChangedListener;

    @SuppressWarnings({"unused", "FieldCanBeLocal"})
    private Consumer<RefreshedVersionsEvent> onRefreshedVersionsListener;

    public GameAdvancedListItem() {
        this.imageContainer = new ImageContainer(LEFT_GRAPHIC_SIZE);
        imageContainer.setMouseTransparent(true);
        AdvancedListItem.setAlignment(imageContainer, Pos.CENTER);
        setLeftGraphic(imageContainer);

        holder.add(FXUtils.onWeakChangeAndOperate(GameDirectoryManager.selectedInstanceProperty(), it -> this.loadVersion()));
    }

    private void loadVersion() {
        String version = GameDirectoryManager.getSelectedInstance();

        boolean repositoryChanged = GameDirectoryManager.getSelectedRepository() != repository;
        if (repositoryChanged) {
            repository = GameDirectoryManager.getSelectedRepository();
            onVersionIconChangedListener = repository.onVersionIconChanged.registerWeak(event -> {
                FXUtils.runInFX(this::loadVersion);
            });

            if (!repository.isLoaded()) {
                onRefreshedVersionsListener = EventBus.EVENT_BUS.channel(RefreshedVersionsEvent.class)
                        .registerWeak(event -> FXUtils.runInFX(this::loadVersion));
                return;
            }
        }
        if (version != null && repository != null && repository.hasVersion(version)) {
            setTitle(i18n("version.manage.manage"));
            setSubtitle(version);
            imageContainer.setImage(repository.getVersionIconImage(version));
        } else {
            setTitle(i18n("version.empty"));
            setSubtitle(i18n("version.empty.add"));
            imageContainer.setImage(VersionIconType.DEFAULT.getIcon());
        }
    }
}
