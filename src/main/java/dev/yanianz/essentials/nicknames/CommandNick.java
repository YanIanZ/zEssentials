package dev.yanianz.essentials.nicknames;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.commands.CommandResultType;
import fr.maxlego08.essentials.api.commands.Permission;
import fr.maxlego08.essentials.api.messages.Message;
import fr.maxlego08.essentials.zutils.utils.commands.VCommand;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

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
        this.addOptionalArg("nickname");
        this.setExtendedArgs(true);
    }

    @Override
    protected CommandResultType perform(EssentialsPlugin plugin) {

        NicknamesModule module = plugin.getModuleManager().getModule(NicknamesModule.class);

        Player target = this.player;
        String nickname = null;

        if (this.args.length == 0) {
            this.syntaxMessage();
            return CommandResultType.SUCCESS;
        }

        if (this.args.length >= 2) {
            // Admin usage: /nick <player> <nickname|off>
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
            message(sender, Message.NICK_REMOVED, "%player%",
                    target == this.player ? Message.YOU.getMessageAsString() : target.getName());
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
            message(sender, errorMessage,
                    "%max%", String.valueOf(module.maxLengthValue()),
                    "%nickname%", nickname);
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

        message(sender, Message.NICK_SET, "%player%",
                        target == this.player ? Message.YOU.getMessageAsString() : target.getName(),
                "%nickname%", nickname);
        return CommandResultType.SUCCESS;
    }
}
