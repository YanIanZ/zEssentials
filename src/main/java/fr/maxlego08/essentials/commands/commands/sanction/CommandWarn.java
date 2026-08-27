package fr.maxlego08.essentials.commands.commands.sanction;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.commands.CommandResultType;
import fr.maxlego08.essentials.api.commands.Permission;
import fr.maxlego08.essentials.api.messages.Message;
import fr.maxlego08.essentials.module.modules.SanctionModule;
import fr.maxlego08.essentials.zutils.utils.commands.VCommand;

/**
 * Issues a warning sanction to the target player with a reason.
 */
public class CommandWarn extends VCommand {

    public CommandWarn(EssentialsPlugin plugin) {
        super(plugin);
        this.setModule(SanctionModule.class);
        this.setPermission(Permission.ESSENTIALS_WARN);
        this.setDescription(Message.DESCRIPTION_WARN);
        this.addRequirePlayerNameArg();
        this.setExtendedArgs(true);
    }

    @Override
    protected CommandResultType perform(EssentialsPlugin plugin) {

        SanctionModule module = plugin.getModuleManager().getModule(SanctionModule.class);
        String userName = this.argAsString(0);

        StringBuilder reason = new StringBuilder();
        for (int index = 1; index < this.args.length; index++) {
            reason.append(this.args[index]).append(index == this.args.length - 1 ? "" : " ");
        }

        String finalReason = reason.toString().isEmpty() ? "No reason specified" : reason.toString();

        isOnline(userName, () -> fetchUniqueId(userName, uniqueId ->
                module.warn(sender, uniqueId, userName, finalReason)));

        return CommandResultType.SUCCESS;
    }
}
