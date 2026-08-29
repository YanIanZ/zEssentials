package dev.yanianz.essentials.reports;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.commands.CommandResultType;
import fr.maxlego08.essentials.api.commands.Permission;
import fr.maxlego08.essentials.api.messages.Message;
import fr.maxlego08.essentials.zutils.utils.commands.VCommand;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Players report others with /report &lt;player&gt; &lt;reason...&gt;.
 * Staff review of the opened reports lives in /reports, see
 * {@link CommandReports}.
 */
public class CommandReport extends VCommand {

    public CommandReport(EssentialsPlugin plugin) {
        super(plugin);
        this.setModule(ReportsModule.class);
        this.setPermission(Permission.ESSENTIALS_REPORT_USE);
        this.setDescription(Message.DESCRIPTION_REPORT);
        this.addRequirePlayerNameArg();
        this.setExtendedArgs(true);
    }

    @Override
    protected CommandResultType perform(EssentialsPlugin plugin) {

        if (this.args.length < 2) {
            this.syntaxMessage();
            return CommandResultType.SUCCESS;
        }

        String targetName = this.argAsString(0);
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            message(sender, Message.PLAYER_NOT_FOUND, "%player%", targetName);
            return CommandResultType.SUCCESS;
        }

        Player reporter = getPlayer();
        if (reporter == null) return CommandResultType.SYNTAX_ERROR;

        StringBuilder reason = new StringBuilder();
        for (int index = 1; index < this.args.length; index++) {
            reason.append(this.args[index]).append(index == this.args.length - 1 ? "" : " ");
        }

        ReportsModule module = plugin.getModuleManager().getModule(ReportsModule.class);
        ReportsModule.Result result = module.create(reporter, target.getUniqueId(), target.getName(), reason.toString());
        switch (result) {
            case SELF -> message(sender, Message.REPORT_SELF);
            case COOLDOWN -> {
            } // the storage already sent the wait message
            default -> {
            }
        }
        return CommandResultType.SUCCESS;
    }
}
