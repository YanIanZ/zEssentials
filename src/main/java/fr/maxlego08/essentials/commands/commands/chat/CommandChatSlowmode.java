package fr.maxlego08.essentials.commands.commands.chat;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.commands.CommandResultType;
import fr.maxlego08.essentials.api.commands.Permission;
import fr.maxlego08.essentials.api.messages.Message;
import fr.maxlego08.essentials.module.modules.chat.ChatModule;
import fr.maxlego08.essentials.zutils.utils.commands.VCommand;

/**
 * Staff command setting a flat chat slowmode in seconds, 0 disables it.
 * Players with essentials.chat.bypass.slowmode are not affected.
 */
public class CommandChatSlowmode extends VCommand {

    public CommandChatSlowmode(EssentialsPlugin plugin) {
        super(plugin);
        this.setModule(ChatModule.class);
        this.setPermission(Permission.ESSENTIALS_CHAT_SLOWMODE);
        this.setDescription(Message.DESCRIPTION_CHAT_SLOWMODE);
        this.addOptionalArg("seconds");
    }

    @Override
    protected CommandResultType perform(EssentialsPlugin plugin) {

        ChatModule chatModule = plugin.getModuleManager().getModule(ChatModule.class);

        if (this.args.length == 0) {
            int current = chatModule.getSlowmodeSeconds();
            message(sender, current > 0 ? Message.COMMAND_CHAT_SLOWMODE_SET : Message.COMMAND_CHAT_SLOWMODE_OFF,
                    "%seconds%", String.valueOf(current));
            return CommandResultType.SUCCESS;
        }

        int seconds;
        try {
            seconds = Integer.parseInt(this.argAsString(0));
        } catch (NumberFormatException exception) {
            this.syntaxMessage();
            return CommandResultType.SUCCESS;
        }

        if (seconds <= 0) {
            chatModule.setSlowmodeSeconds(0);
            message(sender, Message.COMMAND_CHAT_SLOWMODE_OFF);
            return CommandResultType.SUCCESS;
        }

        chatModule.setSlowmodeSeconds(seconds);
        message(sender, Message.COMMAND_CHAT_SLOWMODE_SET, "%seconds%", String.valueOf(seconds));
        return CommandResultType.SUCCESS;
    }
}
