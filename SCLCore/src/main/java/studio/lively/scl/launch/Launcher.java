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
package studio.lively.scl.launch;

import studio.lively.scl.auth.AuthInfo;
import studio.lively.scl.game.GameRepository;
import studio.lively.scl.game.LaunchOptions;
import studio.lively.scl.game.Version;
import studio.lively.scl.util.platform.ManagedProcess;

import java.io.IOException;
import java.nio.file.Path;

/**
 *
 * @author lively-Studio
 */
public abstract class Launcher {

    protected final GameRepository repository;
    protected final Version version;
    protected final AuthInfo authInfo;
    protected final LaunchOptions options;
    protected final ProcessListener listener;
    protected final boolean daemon;

    public Launcher(GameRepository repository, Version version, AuthInfo authInfo, LaunchOptions options) {
        this(repository, version, authInfo, options, null);
    }

    public Launcher(GameRepository repository, Version version, AuthInfo authInfo, LaunchOptions options, ProcessListener listener) {
        this(repository, version, authInfo, options, listener, true);
    }

    public Launcher(GameRepository repository, Version version, AuthInfo authInfo, LaunchOptions options, ProcessListener listener, boolean daemon) {
        this.repository = repository;
        this.version = version;
        this.authInfo = authInfo;
        this.options = options;
        this.listener = listener;
        this.daemon = daemon;
    }

    /**
     * @param file the file path.
     */
    public abstract void makeLaunchScript(Path file) throws IOException;

    public abstract ManagedProcess launch() throws IOException, InterruptedException;

}
