package fr.maxlego08.essentials.commands.commands.enderchest;

import dev.yanianz.essentials.enderchest.EnderChestModule;
import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.commands.CommandResultType;
import fr.maxlego08.essentials.api.commands.Permission;
import fr.maxlego08.essentials.api.messages.Message;
import fr.maxlego08.essentials.zutils.utils.commands.VCommand;
import org.bukkit.OfflinePlayer;

public class CommandEnderSee extends VCommand {

    public CommandEnderSee(EssentialsPlugin plugin) {
        super(plugin);
        this.setPermission(Permission.ESSENTIALS_ENDERSEE);
        this.setDescription(Message.DESCRIPTION_ENDERSEE);
        this.addRequireOfflinePlayerNameArg();
        this.onlyPlayers();
    }

    @Override
    protected CommandResultType perform(EssentialsPlugin plugin) {

        OfflinePlayer offlinePlayer = this.argAsOfflinePlayer(0);
        if (offlinePlayer.isOnline()) {

            var targetPlayer = offlinePlayer.getPlayer();
            if (targetPlayer == null) return CommandResultType.SYNTAX_ERROR;

            EnderChestModule module = plugin.getModuleManager().getModule(EnderChestModule.class);
            module.openEnderChestFor(this.player, offlinePlayer);
            message(this.sender, Message.COMMAND_ENDERSEE_OPENED, "%player%", offlinePlayer.getName());
            message(this.sender, Message.COMMAND_ENDERSEE_READONLY);

        } else {

            if (!hasPermission(sender, Permission.ESSENTIALS_ENDERSEE_OFFLINE)) return CommandResultType.NO_PERMISSION;

            EnderChestModule module = plugin.getModuleManager().getModule(EnderChestModule.class);
            module.openEnderChestFor(this.player, offlinePlayer);
            message(this.sender, Message.COMMAND_ENDERSEE_OPENED, "%player%", offlinePlayer.getName());
            message(this.sender, Message.COMMAND_ENDERSEE_READONLY);
        }
        return CommandResultType.SUCCESS;
    }
}
