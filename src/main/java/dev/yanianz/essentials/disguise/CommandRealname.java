package dev.yanianz.essentials.disguise;

import dev.yanianz.essentials.nicknames.NicknamesModule;
import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.commands.CommandResultType;
import fr.maxlego08.essentials.api.commands.Permission;
import fr.maxlego08.essentials.api.messages.Message;
import fr.maxlego08.essentials.zutils.utils.commands.VCommand;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class CommandRealname extends VCommand {

    public CommandRealname(EssentialsPlugin plugin) {
        super(plugin);
        this.setModule(NicknamesModule.class);
        this.setPermission(Permission.ESSENTIALS_DISGUISE_OTHER);
        this.setDescription(Message.DESCRIPTION_REALNAME);
        this.addOptionalArg("player", getVisiblePlayerNames());
    }

    @Override
    protected CommandResultType perform(EssentialsPlugin plugin) {

        NicknamesModule module = plugin.getModuleManager().getModule(NicknamesModule.class);
        if (module == null) return CommandResultType.SUCCESS;

        Player target = this.argAsPlayer(0, this.player);
        if (target == null) {
            return CommandResultType.SYNTAX_ERROR;
        }

        String disguiseName = module.getDisplayName(target);
        if (disguiseName == null) {
            String nick = module.getNickname(target.getUniqueId());
            if (nick != null && !nick.isEmpty()) {
                message(sender, Message.REALNAME_REVEALED, "%disguise%", nick, "%realname%", target.getName());
                return CommandResultType.SUCCESS;
            }
            message(sender, Message.REALNAME_NOT_DISGUISED);
            return CommandResultType.SUCCESS;
        }

        message(sender, Message.REALNAME_REVEALED, "%disguise%", disguiseName, "%realname%", target.getName());
        return CommandResultType.SUCCESS;
    }
}
