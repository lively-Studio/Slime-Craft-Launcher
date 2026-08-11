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

import studio.lively.scl.launch.ProcessListener;
import studio.lively.scl.util.i18n.I18n;
import studio.lively.scl.util.platform.ManagedProcess;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static studio.lively.scl.util.logging.Logger.LOG;

/// Monitors Minecraft log output and fires toast notifications.
///
/// All detection regex patterns are loaded from the I18N locale files,
/// allowing each language version to define its own patterns for
/// achievements, deaths, recipes, and game mode changes.
///
/// ## I18N keys
///
/// - `toast.death.regex` — player death pattern
/// - `toast.gamemode.regex` — game mode change pattern
/// - `toast.achievement.regex` — achievement/recipe bracket pattern
/// - `toast.challenge.regex` — challenge advancement keyword
/// - `toast.goal.regex` — goal advancement keyword
///
/// The `toast.achievement.regex` pattern must have one capturing group
/// for the bracketed name.
public final class AchievementListener implements ProcessListener {

    private final ProcessListener delegate;

    public AchievementListener(ProcessListener delegate) {
        this.delegate = delegate;
    }

    @Override
    public void setProcess(ManagedProcess process) {
        if (delegate != null) delegate.setProcess(process);
    }

    @Override
    public void onLog(String log, boolean isErrorStream) {
        if (delegate != null) delegate.onLog(log, isErrorStream);
        if (isErrorStream) return;
        if (!log.contains("[Server thread/INFO]:")) return;

        // 1. Player death
        Pattern deathPattern = cached("toast.death.regex");
        Matcher dm = deathPattern.matcher(log);
        if (dm.find()) {
            String player = dm.groupCount() >= 1 ? dm.group(1) : "?";
            LOG.info("Detected death: " + player);
            AchievementToast.show(player, AchievementToast.ToastType.DEATH);
            return;
        }

        // 2. Game mode change
        Pattern gamemodePattern = cached("toast.gamemode.regex");
        if (gamemodePattern.matcher(log).find()) {
            String detail = log.replaceFirst(".*\\[Server thread/INFO\\]:\\s*", "");
            if (detail.length() > 50) detail = detail.substring(0, 47) + "...";
            LOG.info("Detected gamemode change: " + detail);
            AchievementToast.show(detail, AchievementToast.ToastType.GAMEMODE);
            return;
        }

        // 3. Achievement / Recipe (bracketed name pattern)
        Pattern achievePattern = cached("toast.achievement.regex");
        Matcher am = achievePattern.matcher(log);
        if (!am.find()) return;

        String name = am.group(1).trim();
        if (name.isEmpty()) return;

        // Recipe: contains namespace separator
        if (name.contains(":")) {
            LOG.info("Detected recipe unlock: [" + name + "]");
            AchievementToast.show(name, AchievementToast.ToastType.RECIPE);
            return;
        }

        // Advancement type detection
        AchievementToast.ToastType type;
        if (cached("toast.challenge.regex").matcher(log).find()) {
            type = AchievementToast.ToastType.CHALLENGE;
        } else if (cached("toast.goal.regex").matcher(log).find()) {
            type = AchievementToast.ToastType.GOAL;
        } else {
            type = AchievementToast.ToastType.ACHIEVEMENT;
        }

        LOG.info("Detected advancement: [" + name + "] type=" + type);
        AchievementToast.show(name, type);
    }

    /// Cached compiled patterns loaded from I18N.
    private static Pattern cached(String key) {
        return I18n.getPattern(key);
    }

    @Override
    public void onExit(int exitCode, ExitType exitType) {
        if (delegate != null) delegate.onExit(exitCode, exitType);
    }
}
