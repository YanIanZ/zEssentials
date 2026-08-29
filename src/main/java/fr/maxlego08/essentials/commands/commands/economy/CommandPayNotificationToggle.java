package fr.maxlego08.essentials.commands.commands.economy;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.commands.CommandResultType;
import fr.maxlego08.essentials.api.commands.Permission;
import fr.maxlego08.essentials.api.messages.Message;
import fr.maxlego08.essentials.api.user.Option;
import fr.maxlego08.essentials.api.user.User;
import fr.maxlego08.essentials.module.modules.economy.EconomyModule;
import fr.maxlego08.essentials.zutils.utils.commands.VCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandPayNotificationToggle extends VCommand {

    public CommandPayNotificationToggle(EssentialsPlugin plugin) {
        super(plugin);
        this.setModule(EconomyModule.class);
        this.setPermission(Permission.ESSENTIALS_PAY_NOTIFICATION_TOGGLE);
        this.setDescription(Message.DESCRIPTION_PAY_NOTIFICATION_TOGGLE);
        this.onlyPlayers();
        this.addOptionalArg("player", getVisiblePlayerNames());
    }

    @Override
    protected CommandResultType perform(EssentialsPlugin plugin) {

        Player player = this.argAsPlayer(0, this.player);

        if (player == null) {
            return CommandResultType.SYNTAX_ERROR;
        }

        if (player.equals(this.player) || !hasPermission(sender, Permission.ESSENTIALS_PAY_NOTIFICATION_TOGGLE_OTHER)) {
            togglePayNotification(player, this.user, sender);
        } else {
            User otherUser = getUser(player);
            if (otherUser == null) return CommandResultType.SYNTAX_ERROR;
            togglePayNotification(player, otherUser, sender);
        }

        return CommandResultType.SUCCESS;
    }

    private void togglePayNotification(Player player, User user, CommandSender sender) {

        user.setOption(Option.PAY_NOTIFICATION_DISABLE, !user.getOption(Option.PAY_NOTIFICATION_DISABLE));
        boolean isPayNotificationDisable = user.getOption(Option.PAY_NOTIFICATION_DISABLE);

        Message messageKey = isPayNotificationDisable ? Message.COMMAND_PAY_NOTIFICATION_TOGGLE_DISABLE : Message.COMMAND_PAY_NOTIFICATION_TOGGLE_ENABLE;
        message(sender, messageKey, "%player%", user == this.user ? Message.YOU.getMessageAsString() : player.getName());
    }
}
