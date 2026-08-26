package fr.maxlego08.essentials.module.modules.customscreens;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.commands.CommandResultType;
import fr.maxlego08.essentials.api.messages.Message;
import fr.maxlego08.essentials.zutils.utils.commands.VCommand;

/**
 * The command executed when a player runs the open command of a custom screen.
 * One instance is created for each entry of modules/customscreens/config.yml.
 */
public class ZCustomScreen extends VCommand {

    private final CustomScreenModule module;
    private final CustomScreen screen;

    public ZCustomScreen(EssentialsPlugin plugin, CustomScreenModule module, CustomScreen screen) {
        super(plugin);
        this.module = module;
        this.screen = screen;
        this.setModule(CustomScreenModule.class);

        if (screen.hasPermission()) {
            this.setPermission(screen.permission());
        }
        if (screen.description() != null && !screen.description().isEmpty()) {
            this.setDescription(screen.description());
        }
        this.onlyPlayers();
    }

    public CustomScreen getScreen() {
        return this.screen;
    }

    @Override
    protected CommandResultType perform(EssentialsPlugin plugin) {
        this.module.openScreen(this.player, this.screen);
        return CommandResultType.SUCCESS;
    }
}
