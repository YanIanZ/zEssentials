package fr.maxlego08.essentials.commands.commands.sanction;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.commands.CommandResultType;
import fr.maxlego08.essentials.api.commands.Permission;
import fr.maxlego08.essentials.api.messages.Message;
import fr.maxlego08.essentials.module.modules.SanctionModule;
import fr.maxlego08.essentials.zutils.utils.commands.VCommand;

public class CommandUnfreeze extends VCommand {

    public CommandUnfreeze(EssentialsPlugin plugin) {
        super(plugin);
        this.setModule(SanctionModule.class);
        this.setPermission(Permission.ESSENTIALS_FREEZE);
        this.setDescription(Message.DESCRIPTION_UN_FREEZE);
        this.addRequirePlayerNameArg();
    }

    @Override
    protected CommandResultType perform(EssentialsPlugin plugin) {

        SanctionModule sanctionModule = plugin.getModuleManager().getModule(SanctionModule.class);
        String userName = this.argAsString(0);

        isOnline(userName, () -> fetchUniqueId(userName, uuid -> sanctionModule.unfreeze(sender, uuid, userName)));

        return CommandResultType.SUCCESS;
    }
}
