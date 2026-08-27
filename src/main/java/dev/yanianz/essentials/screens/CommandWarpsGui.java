package dev.yanianz.essentials.screens;

import fr.maxlego08.essentials.ZEssentialsPlugin;
import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.commands.CommandResultType;
import fr.maxlego08.essentials.api.commands.Permission;
import fr.maxlego08.essentials.api.user.User;
import fr.maxlego08.essentials.api.utils.Warp;
import fr.maxlego08.essentials.module.modules.WarpModule;
import fr.maxlego08.essentials.zutils.utils.commands.VCommand;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * /warpgui opens every warp as a clickable head list.
 */
public class CommandWarpsGui extends VCommand {

    public CommandWarpsGui(EssentialsPlugin plugin) {
        super(plugin);
        this.setDescription("Open the warp selection screen");
        this.setPermission(Permission.ESSENTIALS_WARPS);
        this.addOptionalArg("player");
        this.onlyPlayers();
    }

    @Override
    protected CommandResultType perform(EssentialsPlugin plugin) {

        Player player = this.argAsPlayer(0, this.player);
        if (player == null) return CommandResultType.SYNTAX_ERROR;

        WarpModule warpModule = plugin.getModuleManager().getModule(WarpModule.class);
        User user = plugin.getStorageManager().getStorage().getUser(player.getUniqueId());

        List<ScreenFactory.ScreenItem> items = new ArrayList<>();
        for (Warp warp : plugin.getWarps()) {
            if (!warp.hasPermission(player)) continue;
            items.add(new ScreenFactory.ScreenItem(
                    Material.ENDER_PEARL,
                    "&b&l" + warp.name(),
                    java.util.List.of(colorize("&7Click to teleport to &f" + warp.name())),
                    (viewer, ev) -> warpModule.teleport(user, warp)));
        }

        if (items.isEmpty()) {
            player.sendMessage(colorize("&cNo warps available."));
            return CommandResultType.SUCCESS;
        }

        EssentialsScreens.get().factory().open(player, "&5&lWARPS", 6, items);
        return CommandResultType.SUCCESS;
    }

    private String colorize(String text) {
        return text == null ? "" : text.replace("&", "§");
    }
}
