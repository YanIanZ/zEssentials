package fr.maxlego08.essentials.commands.commands.utils;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.commands.CommandResultType;
import fr.maxlego08.essentials.api.commands.Permission;
import fr.maxlego08.essentials.api.messages.Message;
import fr.maxlego08.essentials.zutils.utils.commands.VCommand;

public class CommandCraft extends VCommand {
    public CommandCraft(EssentialsPlugin plugin) {
        super(plugin);
        this.setPermission(Permission.ESSENTIALS_CRAFT);
        this.setDescription(Message.DESCRIPTION_CRAFT);
        this.onlyPlayers();
    }

    @Override
    protected CommandResultType perform(EssentialsPlugin plugin) {
        fr.maxlego08.essentials.module.modules.CraftingModule module =
                plugin.getModuleManager().getModule(fr.maxlego08.essentials.module.modules.CraftingModule.class);
        if (module != null && module.isEnabled()) {
            dev.yanianz.essentials.crafting.CraftingListener.ensureRegistered((fr.maxlego08.essentials.ZEssentialsPlugin) plugin);
            module.openCrafting(this.player);
        } else {
            this.player.openWorkbench(this.player.getLocation(), true);
        }
        return CommandResultType.SUCCESS;
    }
}
