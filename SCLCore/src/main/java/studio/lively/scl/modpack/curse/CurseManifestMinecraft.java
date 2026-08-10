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
package studio.lively.scl.modpack.curse;

import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import studio.lively.scl.util.StringUtils;
import studio.lively.scl.util.gson.JsonSerializable;
import studio.lively.scl.util.gson.Validation;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// @author lively-Studio
@JsonSerializable
public record CurseManifestMinecraft(@SerializedName("version") String gameVersion,
                                     @SerializedName("modLoaders") @Unmodifiable List<CurseManifestModLoader> modLoaders) implements Validation {

    public CurseManifestMinecraft(String gameVersion, List<CurseManifestModLoader> modLoaders) {
        this.gameVersion = gameVersion;
        this.modLoaders = Collections.unmodifiableList(new ArrayList<>(modLoaders)); // TODO: Is the modLoaders nullable?
    }

    @Override
    public List<CurseManifestModLoader> modLoaders() {
        return Collections.unmodifiableList(modLoaders);
    }

    @Override
    public void validate() throws JsonParseException {
        if (StringUtils.isBlank(gameVersion))
            throw new JsonParseException("CurseForge Manifest.gameVersion cannot be blank.");
    }

}
