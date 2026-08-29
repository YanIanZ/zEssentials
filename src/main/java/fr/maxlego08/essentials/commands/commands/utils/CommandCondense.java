package fr.maxlego08.essentials.commands.commands.utils;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.commands.CommandResultType;
import fr.maxlego08.essentials.api.commands.Permission;
import fr.maxlego08.essentials.api.messages.Message;
import fr.maxlego08.essentials.zutils.utils.commands.VCommand;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Map;

public class CommandCondense extends VCommand {

    private static final Map<Material, Material> RECIPES = Map.ofEntries(
            Map.entry(Material.IRON_INGOT, Material.IRON_BLOCK),
            Map.entry(Material.GOLD_INGOT, Material.GOLD_BLOCK),
            Map.entry(Material.DIAMOND, Material.DIAMOND_BLOCK),
            Map.entry(Material.EMERALD, Material.EMERALD_BLOCK),
            Map.entry(Material.REDSTONE, Material.REDSTONE_BLOCK),
            Map.entry(Material.COAL, Material.COAL_BLOCK),
            Map.entry(Material.LAPIS_LAZULI, Material.LAPIS_BLOCK),
            Map.entry(Material.QUARTZ, Material.QUARTZ_BLOCK),
            Map.entry(Material.NETHERITE_INGOT, Material.NETHERITE_BLOCK),
            Map.entry(Material.BRICK, Material.BRICKS),
            Map.entry(Material.NETHER_BRICK, Material.NETHER_BRICKS),
            Map.entry(Material.COPPER_INGOT, Material.COPPER_BLOCK),
            Map.entry(Material.AMETHYST_SHARD, Material.AMETHYST_BLOCK),
            Map.entry(Material.GLOWSTONE_DUST, Material.GLOWSTONE),
            Map.entry(Material.SUGAR_CANE, Material.PAPER),
            Map.entry(Material.CLAY_BALL, Material.CLAY),
            Map.entry(Material.SNOWBALL, Material.SNOW_BLOCK),
            Map.entry(Material.MELON_SLICE, Material.MELON),
            Map.entry(Material.WHEAT, Material.HAY_BLOCK),
            Map.entry(Material.BONE, Material.BONE_BLOCK)
    );

    public CommandCondense(EssentialsPlugin plugin) {
        super(plugin);
        this.setPermission(Permission.ESSENTIALS_CONDENSE);
        this.setDescription(Message.DESCRIPTION_CONDENSE);
        this.onlyPlayers();
    }

    @Override
    protected CommandResultType perform(EssentialsPlugin plugin) {
        PlayerInventory inv = this.player.getInventory();
        int condensed = 0;

        for (int slot = 0; slot < inv.getSize(); slot++) {
            ItemStack item = inv.getItem(slot);
            if (item == null || item.getType().isAir()) continue;

            Material blockType = RECIPES.get(item.getType());
            if (blockType == null) continue;

            int fullStacks = item.getAmount() / 9;
            if (fullStacks <= 0) continue;

            int remainder = item.getAmount() % 9;
            if (remainder == 0) {
                inv.setItem(slot, new ItemStack(blockType, fullStacks));
            } else {
                inv.setItem(slot, new ItemStack(item.getType(), remainder));
                ItemStack blocks = new ItemStack(blockType, fullStacks);
                Map<Integer, ItemStack> overflow = inv.addItem(blocks);
                if (!overflow.isEmpty()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), blocks);
                }
            }
            condensed += fullStacks;
        }

        if (condensed == 0) {
            message(sender, Message.COMMAND_CONDENSE_EMPTY);
        } else {
            message(sender, Message.COMMAND_CONDENSE_SUCCESS, "%amount%", String.valueOf(condensed));
        }
        return CommandResultType.SUCCESS;
    }
}
