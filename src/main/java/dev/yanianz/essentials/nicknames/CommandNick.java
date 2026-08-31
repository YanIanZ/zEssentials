package dev.yanianz.essentials.nicknames;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.commands.CommandResultType;
import fr.maxlego08.essentials.api.commands.Permission;
import fr.maxlego08.essentials.api.messages.Message;
import fr.maxlego08.essentials.zutils.utils.commands.VCommand;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Changes the display name of a player. Without arguments prints the usage,
 * off removes the nickname and staff can target another player.
 */

public class CommandNick extends VCommand {

    public CommandNick(EssentialsPlugin plugin) {
        super(plugin);
        this.setModule(NicknamesModule.class);
        this.setPermission(Permission.ESSENTIALS_NICKNAMES_USE);
        this.setDescription(Message.DESCRIPTION_NICK);
        this.addOptionalArg("nickname", (sender, args) -> nickCompletion(args));
        this.addOptionalArg("nickname", (sender, args) -> nickCompletionAdmin(args));
        this.setExtendedArgs(true);
        this.onlyPlayers();
    }

    private List<String> nickCompletion(String[] args) {
        List<String> out = new ArrayList<>();
        out.add("off");
        out.addAll(plugin.getEssentialsServer().getVisiblePlayerNames(this.sender));
        return out;
    }

    private List<String> nickCompletionAdmin(String[] args) {
        return plugin.getEssentialsServer().getVisiblePlayerNames(this.sender);
    }

    @Override
    protected CommandResultType perform(EssentialsPlugin plugin) {

        NicknamesModule module = plugin.getModuleManager().getModule(NicknamesModule.class);

        Player target = this.player;
        String nickname = null;

        if (this.args.length == 0) {
            message(sender, Message.NICK_USAGE);
            return CommandResultType.SUCCESS;
        }

        if (this.args.length >= 2) {
            if (!hasPermission(sender, Permission.ESSENTIALS_NICKNAMES_OTHER)) {
                return CommandResultType.NO_PERMISSION;
            }
            Player possibleTarget = Bukkit.getPlayerExact(this.argAsString(0));
            if (possibleTarget != null) {
                target = possibleTarget;
                nickname = this.args[1].equalsIgnoreCase("off") ? null
                        : String.join(" ", java.util.Arrays.copyOfRange(this.args, 1, this.args.length));
            } else {
                nickname = String.join(" ", this.args);
            }
        } else {
            String argument = this.argAsString(0);
            if (argument.equalsIgnoreCase("off")) {
                nickname = null;
            } else if (Bukkit.getPlayerExact(argument) != null && !argument.equalsIgnoreCase(target.getName())
                    && hasPermission(sender, Permission.ESSENTIALS_NICKNAMES_OTHER)) {
                target = Bukkit.getPlayerExact(argument);
            } else {
                nickname = argument;
            }
        }

        UUID targetUuid = target.getUniqueId();

        if (nickname == null) {
            module.setNickname(targetUuid, null);
            if (target == this.player || sender.equals(target)) {
                message(sender, Message.NICK_REMOVED, "%player%", Message.YOU.getMessageAsString());
            } else {
                message(sender, Message.NICK_REMOVED, "%player%", target.getName());
            }
            return CommandResultType.SUCCESS;
        }

        if (target != this.player && !hasPermission(sender, Permission.ESSENTIALS_NICKNAMES_OTHER)) {
            return CommandResultType.NO_PERMISSION;
        }

        NicknamesModule.NickError error = module.validate(target, nickname);
        if (error != null) {
            Message errorMessage = switch (error) {
                case TOO_LONG -> Message.NICK_TOO_LONG;
                case INVALID_CHARACTERS -> Message.NICK_INVALID_CHARACTERS;
                case COLORS_NOT_ALLOWED -> Message.NICK_COLORS_NOT_ALLOWED;
                case IMPERSONATION -> Message.NICK_IMPERSONATION;
            };
            String errorText = switch (error) {
                case TOO_LONG -> "too long (max " + module.maxLengthValue() + ")";
                case INVALID_CHARACTERS -> "contains forbidden characters";
                case COLORS_NOT_ALLOWED -> "colors are not allowed";
                case IMPERSONATION -> "you cannot impersonate another player";
            };
            message(sender, errorMessage,
                    "%max%", String.valueOf(module.maxLengthValue()),
                    "%nickname%", nickname);
            message(sender, Message.NICK_INVALID, "%error%", errorText);
            return CommandResultType.SUCCESS;
        }

        boolean selfChange = target.equals(this.player) || sender.equals(target);
        boolean cooldownBypass = hasPermission(sender, Permission.ESSENTIALS_NICKNAMES_BYPASS_COOLDOWN);

        if (selfChange && module.isOnCooldown(targetUuid) && !cooldownBypass) {
            message(sender, Message.NICK_COOLDOWN, "%seconds%", String.valueOf(module.getRemainingCooldown(targetUuid)));
            return CommandResultType.SUCCESS;
        }

        module.setNickname(targetUuid, nickname);
        if (selfChange) module.markChanged(targetUuid);

        if (selfChange) {
            message(sender, Message.NICK_CHANGED, "%nickname%", nickname);
        } else {
            message(sender, Message.NICK_SET, "%player%", target.getName(), "%nickname%", nickname);
            message(target, Message.NICK_CHANGED, "%nickname%", nickname);
        }
        return CommandResultType.SUCCESS;
    }
}
