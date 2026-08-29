package fr.maxlego08.essentials.commands.commands.chat;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.commands.CommandResultType;
import fr.maxlego08.essentials.api.commands.Permission;
import fr.maxlego08.essentials.api.messages.Message;
import fr.maxlego08.essentials.module.modules.chatgames.ChatGamesModule;
import fr.maxlego08.essentials.zutils.utils.commands.VCommand;

/**
 * Admin control of the chat games: force start a type, stop the running
 * game or reload the configuration.
 */
public class CommandChatGames extends VCommand {

    public CommandChatGames(EssentialsPlugin plugin) {
        super(plugin);
        this.setModule(ChatGamesModule.class);
        this.setPermission(Permission.ESSENTIALS_CHATGAMES_ADMIN);
        this.setDescription(Message.DESCRIPTION_CHAT_GAMES);
        this.addOptionalArg("type", (s,a) -> java.util.List.of("math", "scramble", "fast-type", "reverse", "trivia", "hot-letter", "stop", "reload"));
    }

    @Override
    protected CommandResultType perform(EssentialsPlugin plugin) {

        ChatGamesModule module = plugin.getModuleManager().getModule(ChatGamesModule.class);

        String type = this.args.length == 0 ? null : this.argAsString(0);

        if (type != null && type.equalsIgnoreCase("stop")) {
            module.stop();
            return CommandResultType.SUCCESS;
        }
        if (type != null && type.equalsIgnoreCase("reload")) {
            module.loadConfiguration();
            message(sender, Message.COMMAND_RELOAD_MODULE, "%module%", "chatgames");
            return CommandResultType.SUCCESS;
        }

        boolean started = module.startRandom(type);
        if (!started) {
            message(sender, Message.CHAT_GAME_ALREADY_RUNNING);
        }
        return CommandResultType.SUCCESS;
    }
}
