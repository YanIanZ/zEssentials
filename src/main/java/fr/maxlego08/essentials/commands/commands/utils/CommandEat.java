package fr.maxlego08.essentials.commands.commands.utils;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.commands.CommandResultType;
import fr.maxlego08.essentials.api.commands.Permission;
import fr.maxlego08.essentials.api.messages.Message;
import fr.maxlego08.essentials.zutils.utils.commands.VCommand;

/**
 * /eat fills hunger, saturation and stops burning for the player.
 */
public class CommandEat extends VCommand {

    public CommandEat(EssentialsPlugin plugin) {
        super(plugin);
        this.setPermission(Permission.ESSENTIALS_EAT);
        this.setDescription(Message.DESCRIPTION_EAT);
        this.onlyPlayers();
    }

    @Override
    protected CommandResultType perform(EssentialsPlugin plugin) {

        this.player.setFoodLevel(20);
        this.player.setSaturation(20f);
        this.player.setFireTicks(0);

        message(sender, Message.COMMAND_EAT_SUCCESS);
        return CommandResultType.SUCCESS;
    }
}
