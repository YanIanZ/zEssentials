package dev.yanianz.essentials.networkchat;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.commands.CommandResultType;
import fr.maxlego08.essentials.api.commands.Permission;
import fr.maxlego08.essentials.api.messages.Message;
import fr.maxlego08.essentials.api.user.Option;
import fr.maxlego08.essentials.zutils.utils.commands.VCommand;

public class CommandGlobalChat extends VCommand {

    public CommandGlobalChat(EssentialsPlugin plugin) {
        super(plugin);
        this.setDescription(Message.DESCRIPTION_GLOBALCHAT);
        this.setPermission(Permission.ESSENTIALS_USE);
        this.onlyPlayers();
    }

    @Override
    protected CommandResultType perform(EssentialsPlugin plugin) {
        boolean enabled = this.user.toggleOption(Option.GLOBAL_CHAT);
        message(this.sender, enabled ? Message.COMMAND_GLOBALCHAT_ENABLED : Message.COMMAND_GLOBALCHAT_DISABLED);
        return CommandResultType.SUCCESS;
    }
}