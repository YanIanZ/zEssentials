package fr.maxlego08.essentials.commands.commands.items;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.commands.CommandResultType;
import fr.maxlego08.essentials.api.commands.Permission;
import fr.maxlego08.essentials.api.messages.Message;
import fr.maxlego08.essentials.zutils.utils.commands.VCommand;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class CommandItemModelData extends VCommand {

    public CommandItemModelData(EssentialsPlugin plugin) {
        super(plugin);
        this.setModule(fr.maxlego08.essentials.module.modules.ItemModule.class);
        this.setPermission(Permission.ESSENTIALS_ITEM_NAME);
        this.setDescription(Message.DESCRIPTION_ITEM_NAME);
        this.addOptionalArg("modeldata");
        this.onlyPlayers();
    }

    @Override
    protected CommandResultType perform(EssentialsPlugin plugin) {
        ItemStack itemStack = this.player.getInventory().getItemInMainHand();
        if (itemStack.getType().isAir()) {
            message(sender, Message.COMMAND_ITEM_EMPTY);
            return CommandResultType.DEFAULT;
        }
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return CommandResultType.DEFAULT;

        if (this.args.length == 0 || this.argAsString(0).equalsIgnoreCase("clear")) {
            meta.setCustomModelData(null);
            itemStack.setItemMeta(meta);
            message(sender, Message.COMMAND_ITEM_CLEAR, "%name%", "model data");
            return CommandResultType.SUCCESS;
        }

        try {
            int modelData = Integer.parseInt(this.argAsString(0));
            meta.setCustomModelData(modelData);
            itemStack.setItemMeta(meta);
            message(sender, Message.COMMAND_ITEM_SET, "%name%", "model data: " + modelData);
        } catch (NumberFormatException e) {
            this.syntaxMessage();
        }
        return CommandResultType.SUCCESS;
    }
}
