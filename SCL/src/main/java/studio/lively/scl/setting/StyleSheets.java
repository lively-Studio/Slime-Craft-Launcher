/*
 * Slime Craft Launcher
 * Copyright (C) 2025 lively-Studio <X_CODER_ocs_2026@126.com> and contributors
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
package studio.lively.scl.setting;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import org.glavo.monetfx.Brightness;
import org.glavo.monetfx.ColorRole;
import org.glavo.monetfx.ColorScheme;
import studio.lively.scl.theme.Themes;
import org.jetbrains.annotations.NotNullByDefault;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Locale;

/// Manages the launcher scene stylesheets, including the dynamic Monet accent override.
@NotNullByDefault
public final class StyleSheets {
    private static final int FONT_STYLE_SHEET_INDEX = 0;
    private static final int THEME_STYLE_SHEET_INDEX = 1;
    private static final int ACCENT_OVERRIDE_SHEET_INDEX = 2;
    private static final int BRIGHTNESS_SHEET_INDEX = 3;
    private static final int ROOT_STYLE_SHEET_INDEX = 4;

    private static final ObservableList<String> stylesheets;

    static {
        String[] array = new String[]{
                getFontStyleSheet(),
                getThemeStyleSheet(),
                getAccentOverrideStyleSheet(),
                getBrightnessStyleSheet(),
                "/assets/css/root.css"
        };
        stylesheets = FXCollections.observableList(Arrays.asList(array));

        FontManager.fontProperty().addListener(o -> stylesheets.set(FONT_STYLE_SHEET_INDEX, getFontStyleSheet()));
        Themes.colorSchemeProperty().addListener(o -> {
            stylesheets.set(ACCENT_OVERRIDE_SHEET_INDEX, getAccentOverrideStyleSheet());
            stylesheets.set(BRIGHTNESS_SHEET_INDEX, getBrightnessStyleSheet());
        });
    }

    private static String toStyleSheetUri(String styleSheet) {
        return "data:text/css;charset=UTF-8;base64," + Base64.getEncoder().encodeToString(styleSheet.getBytes(StandardCharsets.UTF_8));
    }

    private static String getFontStyleSheet() {
        final String defaultCss = "/assets/css/font.css";
        final FontManager.FontReference font = FontManager.getFont();

        if (font == null || "System".equals(font.family()))
            return defaultCss;

        String fontFamily = font.family();
        String style = font.style();
        String weight = null;
        String posture = null;

        if (style != null) {
            style = style.toLowerCase(Locale.ROOT);

            if (style.contains("thin"))
                weight = "100";
            else if (style.contains("extralight") || style.contains("extra light") || style.contains("ultralight") | style.contains("ultra light"))
                weight = "200";
            else if (style.contains("medium"))
                weight = "500";
            else if (style.contains("semibold") || style.contains("semi bold") || style.contains("demibold") || style.contains("demi bold"))
                weight = "600";
            else if (style.contains("extrabold") || style.contains("extra bold") || style.contains("ultrabold") || style.contains("ultra bold"))
                weight = "800";
            else if (style.contains("black") || style.contains("heavy"))
                weight = "900";
            else if (style.contains("light"))
                weight = "lighter";
            else if (style.contains("bold"))
                weight = "bold";

            posture = style.contains("italic") || style.contains("oblique") ? "italic" : null;
        }

        StringBuilder builder = new StringBuilder();
        builder.append(".root {");
        builder.append("-fx-font-family:\"").append(fontFamily).append("\";");

        if (weight != null)
            builder.append("-fx-font-weight:").append(weight).append(";");

        if (posture != null)
            builder.append("-fx-font-style:").append(posture).append(";");

        builder.append('}');

        return toStyleSheetUri(builder.toString());
    }

    private static String getBrightnessStyleSheet() {
        return Themes.getColorScheme().getBrightness() == Brightness.LIGHT
                ? "/assets/css/brightness-light.css"
                : "/assets/css/brightness-dark.css";
    }

    /// Always returns the static NOTHING-UI-1 token stylesheet.
    private static String getThemeStyleSheet() {
        return "/assets/css/nothing-ui.css";
    }

    /// Formats a JavaFX color as an opaque CSS hex color (`#RRGGBB`).
    private static String toCssColor(Color c) {
        return String.format("#%02X%02X%02X",
                (int) Math.round(c.getRed() * 255.0),
                (int) Math.round(c.getGreen() * 255.0),
                (int) Math.round(c.getBlue() * 255.0));
    }

    /// Formats a JavaFX color as a CSS `rgba(r,g,b,a)` string with the given alpha.
    private static String toCssColorAlpha(Color c, double alpha) {
        return String.format("rgba(%d,%d,%d,%.2f)",
                (int) Math.round(c.getRed() * 255.0),
                (int) Math.round(c.getGreen() * 255.0),
                (int) Math.round(c.getBlue() * 255.0),
                alpha);
    }

    /// Builds a dynamic stylesheet that overrides the NOTHING-UI accent tokens with the
    /// current MonetFX primary color, so user-selected custom theme colors take effect.
    ///
    /// Selector note: `nothing-ui.css` declares all accent tokens via the universal
    /// `* { ... }` selector, which attaches the variables directly on every scene node
    /// and therefore wins over inherited values from a parent `.root` declaration. To
    /// actually override those, we use the same `*` selector here — our override
    /// stylesheet is inserted after `nothing-ui.css` in the application stylesheet
    /// list, so equal-specificity rules resolve in our favour.
    private static String getAccentOverrideStyleSheet() {
        ColorScheme scheme = Themes.getColorScheme();
        Color primary = scheme.getColor(ColorRole.PRIMARY);
        Color onPrimary = scheme.getColor(ColorRole.ON_PRIMARY);
        Color error = scheme.getColor(ColorRole.ERROR);
        Color onError = scheme.getColor(ColorRole.ON_ERROR);
        Color errorContainer = scheme.getColor(ColorRole.ERROR_CONTAINER);

        StringBuilder builder = new StringBuilder();
        builder.append("* {");
        // Redirect the built-in cyan accent slot to the live primary color so every
        // component that references `-nothing-accent-cyan` follows the user's theme color.
        builder.append("-nothing-accent-cyan:").append(toCssColor(primary)).append(';');
        builder.append("-nothing-accent:").append(toCssColor(primary)).append(';');
        builder.append("-nothing-accent-fill:").append(toCssColor(primary)).append(';');
        builder.append("-nothing-accent-selected-bg:").append(toCssColorAlpha(primary, 0.15)).append(';');
        builder.append("-nothing-accent-track-bg:").append(toCssColorAlpha(primary, 0.20)).append(';');
        // Error tokens also follow the Monet error role for consistency.
        builder.append("-nothing-error:").append(toCssColor(error)).append(';');
        builder.append("-nothing-error-container:").append(toCssColorAlpha(errorContainer, 0.12)).append(';');
        builder.append("-nothing-on-error:").append(toCssColor(onError)).append(';');
        builder.append('}');
        return toStyleSheetUri(builder.toString());
    }

    public static void init(Scene scene) {
        Bindings.bindContent(scene.getStylesheets(), stylesheets);
    }

    private StyleSheets() {
    }
}
