package fr.maxlego08.essentials.commands.commands.teleport;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.commands.CommandResultType;
import fr.maxlego08.essentials.api.commands.Permission;
import fr.maxlego08.essentials.api.messages.Message;
import fr.maxlego08.essentials.api.user.Option;
import fr.maxlego08.essentials.api.user.User;
import fr.maxlego08.essentials.module.modules.TeleportationModule;
import fr.maxlego08.essentials.zutils.utils.commands.VCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandTpaHereToggle extends VCommand {

    public CommandTpaHereToggle(EssentialsPlugin plugin) {
        super(plugin);
        this.setModule(TeleportationModule.class);
        this.setPermission(Permission.ESSENTIALS_TPA_HERE_TOGGLE);
        this.setDescription(Message.DESCRIPTION_TPA_HERE_TOGGLE);
        this.addOptionalArg("player", getVisiblePlayerNames());
    }

    @Override
    protected CommandResultType perform(EssentialsPlugin plugin) {

        Player player = this.argAsPlayer(0, this.player);

        if (player == null) {
            return CommandResultType.SYNTAX_ERROR;
        }

        if (player.equals(this.player) || !hasPermission(sender, Permission.ESSENTIALS_TPA_HERE_TOGGLE_OTHER)) {
            toggleTeleportHereRequest(player, this.user, sender);
        } else {
            User otherUser = getUser(player);
            if (otherUser == null) return CommandResultType.SYNTAX_ERROR;
            toggleTeleportHereRequest(player, otherUser, sender);
        }

        return CommandResultType.SUCCESS;
    }

    private void toggleTeleportHereRequest(Player player, User user, CommandSender sender) {

        user.setOption(Option.TELEPORT_HERE_REQUEST_DISABLE, !user.getOption(Option.TELEPORT_HERE_REQUEST_DISABLE));
        boolean isTeleportHereRequestDisable = user.getOption(Option.TELEPORT_HERE_REQUEST_DISABLE);

        Message messageKey = isTeleportHereRequestDisable ? Message.COMMAND_TPA_HERE_TOGGLE_DISABLE : Message.COMMAND_TPA_HERE_TOGGLE_ENABLE;
        message(sender, messageKey, "%player%", user == this.user ? Message.YOU.getMessageAsString() : player.getName());
    }
}
