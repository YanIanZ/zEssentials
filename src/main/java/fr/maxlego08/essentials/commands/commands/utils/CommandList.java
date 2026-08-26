package fr.maxlego08.essentials.commands.commands.utils;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.commands.CommandResultType;
import fr.maxlego08.essentials.api.commands.Permission;
import fr.maxlego08.essentials.api.messages.Message;
import fr.maxlego08.essentials.zutils.utils.commands.VCommand;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class CommandList extends VCommand {

    public CommandList(EssentialsPlugin plugin) {
        super(plugin);
        this.setPermission(Permission.ESSENTIALS_LIST);
        this.setDescription(Message.DESCRIPTION_LIST);
    }

    @Override
    protected CommandResultType perform(EssentialsPlugin plugin) {

        List<? extends Player> players = Bukkit.getOnlinePlayers().stream()
                .filter(target -> this.player == null || !isVanishedFor(target, this.player))
                .sorted(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        String playersInfo = players.stream()
                .map(target -> getMessage(Message.COMMAND_LIST_PLAYER, "%player%", target.getName()))
                .collect(Collectors.joining(", "));

        message(this.sender, Message.COMMAND_LIST_HEADER,
                "%amount%", String.valueOf(players.size()),
                "%max%", String.valueOf(Bukkit.getMaxPlayers()),
                "%players%", playersInfo);

        return CommandResultType.SUCCESS;
    }
}
