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
package studio.lively.scl.event;

/**
 * This event gets fired when all the versions in .minecraft folder are loaded.
 * <br>
 * This event is fired on the {@link studio.lively.scl.event.EventBus#EVENT_BUS}
 *
 * @author lively-Studio
 */
public final class RefreshedVersionsEvent extends Event {

    /**
     * Constructor.
     *
     * @param source {@link studio.lively.scl.game.GameRepository}
     */
    public RefreshedVersionsEvent(Object source) {
        super(source);
    }

}
