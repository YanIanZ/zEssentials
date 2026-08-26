package fr.maxlego08.essentials.commands.commands.teleport;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.commands.CommandResultType;
import fr.maxlego08.essentials.api.commands.Permission;
import fr.maxlego08.essentials.api.messages.Message;
import fr.maxlego08.essentials.api.user.User;
import fr.maxlego08.essentials.module.modules.TeleportationModule;
import fr.maxlego08.essentials.zutils.utils.commands.VCommand;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;

public class CommandTpaAll extends VCommand {

    public CommandTpaAll(EssentialsPlugin plugin) {
        super(plugin);
        this.setModule(TeleportationModule.class);
        this.setPermission(Permission.ESSENTIALS_TPA_ALL);
        this.setDescription(Message.DESCRIPTION_TPA_ALL);
        this.onlyPlayers();
    }

    @Override
    protected CommandResultType perform(EssentialsPlugin plugin) {

        List<User> targetUsers = Bukkit.getOnlinePlayers().stream()
                .filter(target -> !target.equals(this.player))
                .filter(target -> !isVanishedFor(target, this.player))
                .map(target -> plugin.getStorageManager().getStorage().getUser(target.getUniqueId()))
                .filter(Objects::nonNull)
                .toList();

        this.user.sendTeleportHereRequestToAll(targetUsers);

        return CommandResultType.SUCCESS;
    }
}
