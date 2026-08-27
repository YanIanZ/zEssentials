package dev.yanianz.essentials.chatcustomization;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.commands.CommandResultType;
import fr.maxlego08.essentials.api.commands.Permission;
import fr.maxlego08.essentials.api.messages.Message;
import fr.maxlego08.essentials.zutils.utils.commands.VCommand;
import org.bukkit.entity.Player;

/**
 * /chatcolor opens the color selection gui.
 */
public class CommandChatColor extends VCommand {

    public CommandChatColor(EssentialsPlugin plugin) {
        super(plugin);
        this.setModule(ChatCustomizationModule.class);
        this.setPermission(Permission.ESSENTIALS_CHATCOLOR_USE);
        this.setDescription(Message.DESCRIPTION_CHATCOLOR);
        this.onlyPlayers();
    }

    @Override
    protected CommandResultType perform(EssentialsPlugin plugin) {
        plugin.getModuleManager().getModule(ChatCustomizationModule.class).openColorGui(this.player);
        return CommandResultType.SUCCESS;
    }
}
