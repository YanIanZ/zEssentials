package dev.yanianz.essentials.guild;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.commands.CommandResultType;
import fr.maxlego08.essentials.api.commands.Permission;
import fr.maxlego08.essentials.api.messages.Message;
import fr.maxlego08.essentials.zutils.utils.commands.VCommand;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.UUID;

public class CommandGuildChat extends VCommand {
    public CommandGuildChat(EssentialsPlugin plugin) {
        super(plugin);
        this.setModule(GuildModule.class);
        this.setPermission(Permission.ESSENTIALS_GUILD);
        this.setDescription(Message.DESCRIPTION_GUILD_CHAT);
        this.setExtendedArgs(true);
    }

    @Override
    protected CommandResultType perform(EssentialsPlugin plugin) {
        GuildModule module = plugin.getModuleManager().getModule(GuildModule.class);
        if (module == null || !module.isEnabled()) return CommandResultType.SUCCESS;
        int guildId = module.getPlayerGuildId(this.player.getUniqueId());
        if (guildId < 0) {
            message(sender, Message.COMMAND_GUILD_NOT_IN);
            return CommandResultType.SUCCESS;
        }
        String messageText = getArgs(0);
        String formatted = module.getChatFormat()
                .replace("%player%", this.player.getName())
                .replace("%message%", messageText)
                .replace("&", "§");
        for (UUID memberUuid : module.getGuild(guildId).members().keySet()) {
            Player p = Bukkit.getPlayer(memberUuid);
            if (p != null) p.sendMessage(LegacyComponentSerializer.legacySection().deserialize(formatted));
        }
        return CommandResultType.SUCCESS;
    }
}