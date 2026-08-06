/*
 * Slime Craft Launcher
 * Copyright (C) 2026 lively-Studio <X_CODER_ocs2008@126.com> and contributors
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
package studio.lively.scl.util;

import studio.lively.scl.util.i18n.LocaleUtils;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Random;

import static studio.lively.scl.setting.SettingsManager.settings;

public final class AprilFools {

    private static final boolean ENABLED;
    private static final int LAUNCH_BUTTON_PAGE;

    static {
        var date = LocalDate.now();

        // Some countries/regions may oppose April Fools' Day for various reasons.
        // Therefore, we use a regional whitelist to avoid risks.
        // Currently, we have only listed a limited set of countries/regions for testing.
        // We will investigate more countries/regions in the future to expand this list.
        boolean supportedRegion = List.of(
                "CN", "TW", "HK", "MO", "JP", "KR", "VN", "SG", "MY",
                "ES", "DE", "FR", "GB", "RU", "UA", "US"
        ).contains(LocaleUtils.SYSTEM_DEFAULT.getCountry());

        boolean aprilFoolsMode;
        String value = System.getProperty("scl.april_fools", System.getenv("SCL_APRIL_FOOLS"));
        if ("true".equalsIgnoreCase(value))
            aprilFoolsMode = true;
        else if ("false".equalsIgnoreCase(value) || !supportedRegion)
            aprilFoolsMode = false;
        else
            aprilFoolsMode = date.getMonth() == Month.APRIL && date.getDayOfMonth() == 1;

        ENABLED = aprilFoolsMode && !settings().disableAprilFoolsProperty().get();
        LAUNCH_BUTTON_PAGE = new Random().nextInt(4); // 0=About, 1=Feedback, 2=Help, 3=Downloads
    }

    /// Whether April Fools is enabled.
    public static boolean isEnabled() {
        return ENABLED;
    }

    /// Returns whether the launch button should appear on the page with given index.
    public static boolean isLaunchButtonPage(int index) {
        return ENABLED && LAUNCH_BUTTON_PAGE == index;
    }

    private AprilFools() {
    }
}
