package dev.yanianz.essentials.stash;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.commands.CommandResultType;
import fr.maxlego08.essentials.api.commands.Permission;
import fr.maxlego08.essentials.api.messages.Message;
import fr.maxlego08.essentials.module.modules.StashModule;
import fr.maxlego08.essentials.zutils.utils.commands.VCommand;

public class CommandStash extends VCommand {

    public CommandStash(EssentialsPlugin plugin) {
        super(plugin);
        this.setModule(StashModule.class);
        this.setPermission(Permission.ESSENTIALS_STASH);
        this.setDescription(Message.DESCRIPTION_STASH);
        this.addOptionalArg("type", (sender, args) -> java.util.List.of("item", "material", "i", "m"));
        this.onlyPlayers();
    }

    @Override
    protected CommandResultType perform(EssentialsPlugin plugin) {
        StashModule module = plugin.getModuleManager().getModule(StashModule.class);
        if (module == null || !module.isEnabled()) return CommandResultType.SUCCESS;

        ItemStashListener.ensureRegistered((fr.maxlego08.essentials.ZEssentialsPlugin) plugin);
        MaterialStashListener.ensureRegistered((fr.maxlego08.essentials.ZEssentialsPlugin) plugin);

        String type = argAsString(0, "");
        switch (type.toLowerCase()) {
            case "item", "i" -> module.openItemStash(this.player);
            case "material", "m" -> module.openMaterialStash(this.player);
            default -> module.openCategoryPicker(this.player);
        }
        return CommandResultType.SUCCESS;
    }
}