package dev.yanianz.essentials.reputation;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.commands.CommandResultType;
import fr.maxlego08.essentials.api.commands.Permission;
import fr.maxlego08.essentials.api.messages.Message;
import fr.maxlego08.essentials.zutils.utils.commands.VCommand;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * /reputation [player] displays the reputation score of a player.
 */
public class CommandReputationView extends VCommand {

    public CommandReputationView(EssentialsPlugin plugin) {
        super(plugin);
        this.setModule(ReputationModule.class);
        this.setPermission(Permission.ESSENTIALS_REPUTATION_USE);
        this.setDescription(Message.DESCRIPTION_REPUTATION);
        this.addOptionalArg("player");
    }

    @Override
    protected CommandResultType perform(EssentialsPlugin plugin) {

        ReputationModule module = plugin.getModuleManager().getModule(ReputationModule.class);

        Player target = this.args.length == 0 ? this.player : Bukkit.getPlayerExact(this.argAsString(0));

        if (target == null) {
            message(sender, Message.PLAYER_NOT_FOUND, "%player%", this.argAsString(0));
            return CommandResultType.SUCCESS;
        }

        String displayName = target == this.player ? Message.YOU.getMessageAsString() : target.getName();
        message(sender, Message.REPUTATION_SCORE, "%player%", displayName,
                "%score%", String.valueOf(module.getScore(target.getUniqueId())));

        return CommandResultType.SUCCESS;
    }
}
