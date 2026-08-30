package dev.yanianz.essentials.friends;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.commands.CommandResultType;
import fr.maxlego08.essentials.api.commands.Permission;
import fr.maxlego08.essentials.api.messages.Message;
import fr.maxlego08.essentials.zutils.utils.commands.VCommand;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class CommandFriend extends VCommand {

    public CommandFriend(EssentialsPlugin plugin) {
        super(plugin);
        this.setModule(FriendsModule.class);
        this.setPermission(Permission.ESSENTIALS_FRIENDS);
        this.setDescription(Message.DESCRIPTION_FRIEND);
        this.addOptionalArg("action", (sender, args) -> List.of("add", "remove", "accept", "decline", "list"));
        this.addRequirePlayerNameArg();
    }

    @Override
    protected CommandResultType perform(EssentialsPlugin plugin) {
        FriendsModule module = plugin.getModuleManager().getModule(FriendsModule.class);
        if (module == null || !module.isEnabled()) return CommandResultType.SUCCESS;

        String action = argAsString(0, "list");
        String targetName = argAsString(1, "");
        if (action.isEmpty()) action = "list";

        UUID playerUuid = this.player != null ? this.player.getUniqueId() : null;
        if (playerUuid == null) return CommandResultType.SYNTAX_ERROR;

        switch (action.toLowerCase()) {
            case "add" -> handleAdd(module, playerUuid, targetName);
            case "remove" -> handleRemove(module, playerUuid, targetName);
            case "accept" -> handleAccept(module, playerUuid, targetName);
            case "decline" -> handleDecline(module, playerUuid, targetName);
            case "list" -> handleList(module, playerUuid);
            default -> {
            }
        }
        return CommandResultType.SUCCESS;
    }

    private void handleAdd(FriendsModule module, UUID playerUuid, String targetName) {
        if (targetName.isEmpty()) return;
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            message(sender, Message.COMMAND_FRIEND_NOT_ONLINE, "%player%", targetName);
            return;
        }
        if (module.sendRequest(playerUuid, target.getUniqueId())) {
            message(sender, Message.COMMAND_FRIEND_REQUEST_SENT, "%player%", targetName);
            message(target, Message.COMMAND_FRIEND_REQUEST_RECEIVED, "%player%", this.player.getName());
        } else {
            message(sender, Message.COMMAND_FRIEND_ALREADY_FRIEND, "%player%", targetName);
        }
    }

    private void handleRemove(FriendsModule module, UUID playerUuid, String targetName) {
        UUID targetUuid = resolveUuid(targetName);
        if (targetUuid == null) return;
        if (module.removeFriend(playerUuid, targetUuid)) {
            message(sender, Message.COMMAND_FRIEND_REMOVED, "%player%", targetName);
        }
    }

    private void handleAccept(FriendsModule module, UUID playerUuid, String targetName) {
        UUID targetUuid = resolveUuid(targetName);
        if (targetUuid == null) return;
        if (module.acceptRequest(targetUuid, playerUuid)) {
            message(sender, Message.COMMAND_FRIEND_ACCEPTED, "%player%", targetName);
            Player target = Bukkit.getPlayer(targetUuid);
            if (target != null) {
                message(target, Message.COMMAND_FRIEND_ACCEPTED_NOTIFY, "%player%", this.player.getName());
            }
        }
    }

    private void handleDecline(FriendsModule module, UUID playerUuid, String targetName) {
        UUID targetUuid = resolveUuid(targetName);
        if (targetUuid == null) return;
        if (module.declineRequest(targetUuid, playerUuid)) {
            message(sender, Message.COMMAND_FRIEND_DECLINED, "%player%", targetName);
        }
    }

    private void handleList(FriendsModule module, UUID playerUuid) {
        List<UUID> friends = module.getFriends(playerUuid);
        String names = friends.stream()
                .map(uuid -> {
                    Player p = Bukkit.getPlayer(uuid);
                    return p != null ? p.getName() : uuid.toString().substring(0, 8);
                })
                .collect(Collectors.joining(", "));
        if (names.isEmpty()) names = "none";
        message(sender, Message.COMMAND_FRIEND_LIST, "%friends%", names);
    }

    private UUID resolveUuid(String name) {
        Player p = Bukkit.getPlayer(name);
        if (p != null) return p.getUniqueId();
        return null;
    }
}