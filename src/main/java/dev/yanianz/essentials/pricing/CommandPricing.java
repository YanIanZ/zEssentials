package dev.yanianz.essentials.pricing;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.commands.CommandResultType;
import fr.maxlego08.essentials.api.commands.Permission;
import fr.maxlego08.essentials.api.messages.Message;
import fr.maxlego08.essentials.module.modules.PricingModule;
import fr.maxlego08.essentials.zutils.utils.commands.VCommand;

public class CommandPricing extends VCommand {

    public CommandPricing(EssentialsPlugin plugin) {
        super(plugin);
        this.setModule(PricingModule.class);
        this.setPermission(Permission.ESSENTIALS_USE);
        this.setDescription(Message.DESCRIPTION_PRICING);
        this.onlyPlayers();
    }

    @Override
    protected CommandResultType perform(EssentialsPlugin plugin) {
        PricingModule module = plugin.getModuleManager().getModule(PricingModule.class);
        if (module == null) return CommandResultType.SUCCESS;

        boolean enabled = module.togglePlayer(this.player.getUniqueId());
        message(this.sender, enabled ? Message.COMMAND_PRICING_ENABLED : Message.COMMAND_PRICING_DISABLED);
        return CommandResultType.SUCCESS;
    }
}
