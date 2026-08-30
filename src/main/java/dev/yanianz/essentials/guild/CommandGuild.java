package dev.yanianz.essentials.guild;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.commands.CommandResultType;
import fr.maxlego08.essentials.api.commands.Permission;
import fr.maxlego08.essentials.api.messages.Message;
import fr.maxlego08.essentials.zutils.utils.commands.VCommand;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class CommandGuild extends VCommand {
    public CommandGuild(EssentialsPlugin plugin) {
        super(plugin);
        this.setModule(GuildModule.class);
        this.setPermission(Permission.ESSENTIALS_GUILD);
        this.setDescription(Message.DESCRIPTION_GUILD);
        this.addOptionalArg("action", (s, a) -> java.util.List.of("create", "disband", "invite", "join", "leave", "info"));
    }

    @Override
    protected CommandResultType perform(EssentialsPlugin plugin) {
        GuildModule module = plugin.getModuleManager().getModule(GuildModule.class);
        if (module == null || !module.isEnabled()) return CommandResultType.SUCCESS;
        String action = argAsString(0, "info");
        switch (action.toLowerCase()) {
            case "create" -> {
                String name = argAsString(1, "");
                if (name.isEmpty()) {
                    message(sender, Message.COMMAND_GUILD_USAGE);
                    return CommandResultType.SUCCESS;
                }
                int id = module.createGuild(this.player.getUniqueId(), name, name);
                if (id > 0) message(sender, Message.COMMAND_GUILD_CREATED, "%name%", name);
                else message(sender, Message.COMMAND_GUILD_ALREADY_IN);
            }
            case "disband" -> {
                int id = module.getPlayerGuildId(this.player.getUniqueId());
                if (id < 0 || module.getGuild(id).leader() != this.player.getUniqueId()) {
                    message(sender, Message.COMMAND_GUILD_NOT_LEADER);
                    return CommandResultType.SUCCESS;
                }
                module.disbandGuild(id);
                message(sender, Message.COMMAND_GUILD_DISBANDED);
            }
            case "join" -> {
                int id = argAsInteger(1, -1);
                if (module.joinGuild(id, this.player.getUniqueId())) {
                    message(sender, Message.COMMAND_GUILD_JOINED);
                }
            }
            case "leave" -> {
                int id = module.getPlayerGuildId(this.player.getUniqueId());
                if (id < 0) return CommandResultType.SUCCESS;
                if (module.getGuild(id).leader() == this.player.getUniqueId()) {
                    message(sender, Message.COMMAND_GUILD_LEADER_CANNOT_LEAVE);
                    return CommandResultType.SUCCESS;
                }
                if (module.leaveGuild(id, this.player.getUniqueId())) {
                    message(sender, Message.COMMAND_GUILD_LEFT);
                }
            }
            case "info" -> {
                int id = module.getPlayerGuildId(this.player.getUniqueId());
                if (id < 0) {
                    message(sender, Message.COMMAND_GUILD_NOT_IN);
                    return CommandResultType.SUCCESS;
                }
                GuildModule.Guild g = module.getGuild(id);
                message(sender, Message.COMMAND_GUILD_INFO, "%name%", g.name(),
                        "%members%", String.valueOf(g.members().size()));
            }
        }
        return CommandResultType.SUCCESS;
    }
}