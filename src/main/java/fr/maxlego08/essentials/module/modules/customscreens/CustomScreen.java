package fr.maxlego08.essentials.module.modules.customscreens;

import fr.maxlego08.menu.api.sound.SoundOption;
import org.bukkit.GameMode;

import java.util.List;

/**
 * A custom screen defined in modules/customscreens/config.yml. The screen layout itself
 * lives in modules/customscreens/screens/{@code <name>}.yml using the zMenu inventory format,
 * this record only holds the registry information and the open conditions.
 */
public record CustomScreen(
        String name,
        String command,
        List<String> aliases,
        String permission,
        String description,
        List<String> worlds,
        List<GameMode> gameModes,
        SoundOption openSound
) {

    public boolean hasPermission() {
        return this.permission != null && !this.permission.isEmpty();
    }

    /**
     * Checks if the player can open this screen in its current world and gamemode.
     * Empty lists mean every world/gamemode is allowed.
     */
    public boolean canOpen(String worldName, GameMode gameMode) {
        if (!this.worlds.isEmpty() && !this.worlds.contains(worldName)) return false;
        if (!this.gameModes.isEmpty() && !this.gameModes.contains(gameMode)) return false;
        return true;
    }
}
