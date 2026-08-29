package fr.maxlego08.essentials.commands.commands.items;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.commands.CommandResultType;
import fr.maxlego08.essentials.api.commands.Permission;
import fr.maxlego08.essentials.api.messages.Message;
import fr.maxlego08.essentials.zutils.utils.commands.VCommand;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class CommandItemUnbreakable extends VCommand {

    public CommandItemUnbreakable(EssentialsPlugin plugin) {
        super(plugin);
        this.setModule(fr.maxlego08.essentials.module.modules.ItemModule.class);
        this.setPermission(Permission.ESSENTIALS_ITEM_NAME);
        this.setDescription(Message.DESCRIPTION_ITEM_NAME);
        this.addBooleanOptionalArg("toggle");
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

        boolean enable = this.args.length == 0 ? !meta.isUnbreakable() : Boolean.parseBoolean(this.argAsString(0));

        meta.setUnbreakable(enable);
        itemStack.setItemMeta(meta);
        message(sender, enable ? Message.COMMAND_ITEM_SET : Message.COMMAND_ITEM_CLEAR,
                "%name%", "unbreakable");
        return CommandResultType.SUCCESS;
    }
}
