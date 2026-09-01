package fr.maxlego08.essentials.commands.commands.chat;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.commands.CommandResultType;
import fr.maxlego08.essentials.api.commands.Permission;
import fr.maxlego08.essentials.api.messages.Message;
import fr.maxlego08.essentials.module.modules.chat.ChatModule;
import fr.maxlego08.essentials.zutils.utils.commands.VCommand;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;

import dev.yanianz.essentials.util.ColorUtil;

public class CommandChatBroadcast extends VCommand {

    public CommandChatBroadcast(EssentialsPlugin plugin) {
        super(plugin);
        this.setModule(ChatModule.class);
        this.setPermission(Permission.ESSENTIALS_CHAT_BROADCAST);
        this.setDescription(Message.DESCRIPTION_CHAT_BROADCAST);
        this.addRequireArg("message");
        this.setExtendedArgs(true);
    }

    @Override
    protected CommandResultType perform(EssentialsPlugin plugin) {
        String message = getArgs(0);
        String[] lines = message.split("\\\\n");
        for (String line : lines) {
            String colored = ColorUtil.sections(line);
            String centered = getCenteredMessage(colored);
            var component = LegacyComponentSerializer.legacySection().deserialize(centered);
            Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(component));
            Bukkit.getConsoleSender().sendMessage(component);
        }
        return CommandResultType.SUCCESS;
    }
}
