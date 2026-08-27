package fr.maxlego08.essentials.commands.commands.chat;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.commands.CommandResultType;
import fr.maxlego08.essentials.api.commands.Permission;
import fr.maxlego08.essentials.api.messages.Message;
import fr.maxlego08.essentials.api.user.Option;
import fr.maxlego08.essentials.api.user.User;
import fr.maxlego08.essentials.module.modules.chat.ChatModule;
import fr.maxlego08.essentials.zutils.utils.commands.VCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Toggles the do not disturb mode: while enabled the player no longer
 * receives mention notifications in the chat.
 */
public class CommandChatDnd extends VCommand {

    public CommandChatDnd(EssentialsPlugin plugin) {
        super(plugin);
        this.setModule(ChatModule.class);
        this.setPermission(Permission.ESSENTIALS_CHAT_DND);
        this.setDescription(Message.DESCRIPTION_CHAT_DND);
        this.addOptionalArg("player");
    }

    @Override
    protected CommandResultType perform(EssentialsPlugin plugin) {

        Player player = this.argAsPlayer(0, this.player);
        if (player == null) return CommandResultType.SYNTAX_ERROR;

        if (player == this.player || !hasPermission(sender, Permission.ESSENTIALS_CHAT_DND_OTHER)) {
            toggle(player, this.user, sender);
        } else {
            User otherUser = getUser(player);
            toggle(player, otherUser, sender);
        }

        return CommandResultType.SUCCESS;
    }

    private void toggle(Player player, User user, CommandSender sender) {

        user.setOption(Option.CHAT_DND, !user.getOption(Option.CHAT_DND));
        boolean enabled = user.getOption(Option.CHAT_DND);

        Message messageKey = enabled ? Message.COMMAND_CHAT_DND_ENABLE : Message.COMMAND_CHAT_DND_DISABLE;
        message(sender, messageKey, "%player%", user == this.user ? Message.YOU.getMessageAsString() : player.getName());
    }
}
