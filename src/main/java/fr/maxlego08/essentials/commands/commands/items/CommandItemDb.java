package fr.maxlego08.essentials.commands.commands.items;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.commands.CommandResultType;
import fr.maxlego08.essentials.api.commands.Permission;
import fr.maxlego08.essentials.api.messages.Message;
import fr.maxlego08.essentials.zutils.utils.commands.VCommand;
import org.bukkit.inventory.ItemStack;

public class CommandItemDb extends VCommand {

    public CommandItemDb(EssentialsPlugin plugin) {
        super(plugin);
        this.setPermission(Permission.ESSENTIALS_ITEMDB);
        this.setDescription(Message.DESCRIPTION_ITEMDB);
        this.onlyPlayers();
    }

    @Override
    protected CommandResultType perform(EssentialsPlugin plugin) {

        ItemStack itemStack = this.player.getInventory().getItemInMainHand();
        if (itemStack.getType().isAir()) {
            message(this.sender, Message.COMMAND_ITEMDB_EMPTY);
            return CommandResultType.DEFAULT;
        }

        message(this.sender, Message.COMMAND_ITEMDB,
                "%material%", itemStack.getType().name(),
                "%key%", itemStack.getType().getKey().toString(),
                "%amount%", String.valueOf(itemStack.getAmount()),
                "%stack%", String.valueOf(itemStack.getType().getMaxStackSize()));

        return CommandResultType.SUCCESS;
    }
}
