package fr.maxlego08.essentials.commands.commands.utils;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.commands.CommandResultType;
import fr.maxlego08.essentials.api.commands.Permission;
import fr.maxlego08.essentials.api.messages.Message;
import fr.maxlego08.essentials.zutils.utils.commands.VCommand;
import org.bukkit.entity.Player;

import java.util.ArrayList;

public class CommandEat extends VCommand {

    public CommandEat(EssentialsPlugin plugin) {
        super(plugin);
        this.setPermission(Permission.ESSENTIALS_EAT);
        this.setDescription(Message.DESCRIPTION_EAT);
        this.addOptionalArg("player", (a, b) -> new ArrayList<>(plugin.getEssentialsServer().getVisiblePlayerNames(this.sender)) {{
            add("*");
        }});
    }

    @Override
    protected CommandResultType perform(EssentialsPlugin plugin) {
        String arg = this.argAsString(0);

        if ("*".equals(arg)) {
            return eatAllPlayers(plugin);
        }

        return eatSinglePlayer();
    }

    private CommandResultType eatAllPlayers(EssentialsPlugin plugin) {
        if (!hasPermission(sender, Permission.ESSENTIALS_EAT_ALL)) {
            return CommandResultType.NO_PERMISSION;
        }

        for (Player target : plugin.getServer().getOnlinePlayers()) {
            if (!target.isValid()) continue;
            eatPlayer(target);
            message(target, Message.COMMAND_EAT_SUCCESS);
        }

        message(sender, Message.COMMAND_EAT_SUCCESS);
        return CommandResultType.SUCCESS;
    }

    private CommandResultType eatSinglePlayer() {
        Player player = this.argAsPlayer(0, this.player);
        if (player == null) return CommandResultType.SYNTAX_ERROR;

        if (player != this.player && !hasPermission(sender, Permission.ESSENTIALS_EAT_OTHER)) {
            player = this.player;
        }

        if (!player.isValid()) {
            message(sender, Message.COMMAND_EAT_SUCCESS);
            return CommandResultType.DEFAULT;
        }

        eatPlayer(player);

        if (player == sender) {
            message(sender, Message.COMMAND_EAT_SUCCESS);
        } else {
            message(sender, Message.COMMAND_EAT_SUCCESS);
            message(player, Message.COMMAND_EAT_SUCCESS);
        }

        return CommandResultType.SUCCESS;
    }

    private void eatPlayer(Player player) {
        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.setFireTicks(0);
    }
}
