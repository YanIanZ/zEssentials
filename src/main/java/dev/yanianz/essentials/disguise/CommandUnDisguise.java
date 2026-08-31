package dev.yanianz.essentials.disguise;

import dev.yanianz.essentials.nicknames.NicknamesModule;
import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.commands.CommandResultType;
import fr.maxlego08.essentials.api.commands.Permission;
import fr.maxlego08.essentials.api.messages.Message;
import fr.maxlego08.essentials.zutils.utils.commands.VCommand;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class CommandUnDisguise extends VCommand {

    public CommandUnDisguise(EssentialsPlugin plugin) {
        super(plugin);
        this.setModule(NicknamesModule.class);
        this.setPermission(Permission.ESSENTIALS_DISGUISE_USE);
        this.setDescription(Message.DESCRIPTION_UNDISGUISE);
        this.addOptionalArg("player", getVisiblePlayerNames());
        this.onlyPlayers();
    }

    @Override
    protected CommandResultType perform(EssentialsPlugin plugin) {

        NicknamesModule module = plugin.getModuleManager().getModule(NicknamesModule.class);
        if (module == null || !module.isDisguiseEnabled()) {
            message(sender, Message.DISGUISE_DISABLED);
            return CommandResultType.SUCCESS;
        }

        Player target = this.argAsPlayer(0, this.player);
        if (target == null) {
            return CommandResultType.SYNTAX_ERROR;
        }

        boolean self = target.equals(this.player);
        if (!self && !hasPermission(this.sender, Permission.ESSENTIALS_DISGUISE_OTHER)) {
            message(this.sender, Message.COMMAND_NO_PERMISSION);
            return CommandResultType.NO_PERMISSION;
        }

        if (!module.isDisguised(target.getUniqueId())) {
            message(sender, Message.DISGUISE_NOT_DISGUISED);
            return CommandResultType.SUCCESS;
        }

        module.removeDisguise(target.getUniqueId());

        if (self) {
            message(sender, Message.DISGUISE_REMOVED);
        } else {
            message(sender, Message.DISGUISE_REMOVED_OTHER, "%player%", target.getName());
            message(target, Message.DISGUISE_REMOVED);
        }

        return CommandResultType.SUCCESS;
    }
}
