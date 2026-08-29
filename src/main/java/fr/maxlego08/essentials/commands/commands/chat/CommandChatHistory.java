package fr.maxlego08.essentials.commands.commands.chat;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.commands.CommandResultType;
import fr.maxlego08.essentials.api.commands.Permission;
import fr.maxlego08.essentials.api.messages.Message;
import fr.maxlego08.essentials.module.modules.chat.ChatModule;
import fr.maxlego08.essentials.zutils.utils.commands.VCommand;

public class CommandChatHistory extends VCommand {

    public CommandChatHistory(EssentialsPlugin plugin) {
        super(plugin);
        this.setModule(ChatModule.class);
        this.setPermission(Permission.ESSENTIALS_CHAT_HISTORY);
        this.setDescription(Message.DESCRIPTION_CHAT_HISTORY);
        this.addRequireOfflinePlayerNameArg();
        this.addOptionalArg("page", (s, a) -> java.util.List.of("1", "2", "3"));
    }

    @Override
    protected CommandResultType perform(EssentialsPlugin plugin) {
        String username = this.argAsString(0);
        if (this.args.length >= 2 && this.args[1].equalsIgnoreCase("delete")) {
            int index = this.argAsInteger(2, -1);
            int page = this.argAsInteger(3, 1);
            fetchUniqueId(username, uuid -> plugin.getModuleManager().getModule(ChatModule.class)
                    .deleteHistoryMessage(sender, uuid, username, index, page));
            return CommandResultType.SUCCESS;
        }

        int page = this.argAsInteger(1, 1);

        fetchUniqueId(username, uuid -> plugin.getModuleManager().getModule(ChatModule.class).sendChatHistory(sender, uuid, username, page));

        return CommandResultType.SUCCESS;
    }
}
