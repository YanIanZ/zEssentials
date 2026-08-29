package dev.yanianz.essentials.screens;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.commands.CommandResultType;
import fr.maxlego08.essentials.zutils.utils.commands.VCommand;

/**
 * /baltopgui [economy] opens the balance top as a head screen.
 */
public class CommandBaltopGui extends VCommand {

    public CommandBaltopGui(EssentialsPlugin plugin) {
        super(plugin);
        this.setDescription("Open the balance top screen");
        this.setPermission(fr.maxlego08.essentials.api.commands.Permission.ESSENTIALS_BALANCE_TOP);
        this.addOptionalArg("economy", (sender, args) ->
                plugin.getEconomyManager().getEconomies().stream()
                        .map(e -> e.getName()).toList());
        this.onlyPlayers();
    }

    @Override
    protected CommandResultType perform(EssentialsPlugin plugin) {

        String economy = this.args.length == 0 ? "money" : this.argAsString(0);
        EssentialsScreens.get().openBaltop(this.player, economy, 6);
        return CommandResultType.SUCCESS;
    }
}
