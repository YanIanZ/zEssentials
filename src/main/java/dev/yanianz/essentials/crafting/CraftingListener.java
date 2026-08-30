package dev.yanianz.essentials.crafting;

import fr.maxlego08.essentials.ZEssentialsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class CraftingListener implements Listener {

    private static boolean registered = false;
    private final ZEssentialsPlugin plugin;

    public CraftingListener(ZEssentialsPlugin plugin) {
        this.plugin = plugin;
    }

    public static void ensureRegistered(ZEssentialsPlugin plugin) {
        if (!registered) {
            Bukkit.getPluginManager().registerEvents(new CraftingListener(plugin), plugin);
            registered = true;
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory inventory = event.getInventory();
        if (!(inventory.getHolder() instanceof CraftingHolder holder)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        int slot = event.getRawSlot();

        if (CraftingSlotMap.isFillerSlot(slot)) {
            event.setCancelled(true);
            return;
        }

        if (CraftingSlotMap.isCloseSlot(slot)) {
            event.setCancelled(true);
            player.closeInventory();
            return;
        }

        if (CraftingSlotMap.isQuickCraftSlot(slot)) {
            event.setCancelled(true);
            if (!holder.isQuickCraftAllowed()) return;
            performQuickCraft(inventory, player);
            updateResult(inventory);
            return;
        }

        if (CraftingSlotMap.isResultSlot(slot)) {
            event.setCancelled(true);
            ItemStack result = inventory.getItem(CraftingSlotMap.SLOT_RESULT);
            if (result == null || result.getType().isAir()) return;

            if (event.getClick().isShiftClick()) {
                craftMultiple(inventory, player, result);
            } else {
                craftOne(inventory, player, result);
            }
            updateResult(inventory);
            return;
        }

        if (CraftingSlotMap.isGridSlot(slot)) {
            plugin.getScheduler().runNextTick(wrappedTask -> updateResult(inventory));
            return;
        }

        if (CraftingSlotMap.isPlayerInvSlot(slot)) {
            return;
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        Inventory inventory = event.getInventory();
        if (!(inventory.getHolder() instanceof CraftingHolder)) return;

        for (int slot : event.getRawSlots()) {
            if (CraftingSlotMap.isFillerSlot(slot) || CraftingSlotMap.isResultSlot(slot)
                    || CraftingSlotMap.isQuickCraftSlot(slot) || CraftingSlotMap.isCloseSlot(slot)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();
        if (!(inventory.getHolder() instanceof CraftingHolder holder)) return;
        if (!(event.getPlayer() instanceof Player player)) return;

        for (int i = 0; i < 9; i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && !item.getType().isAir()) {
                Map<Integer, ItemStack> overflow = player.getInventory().addItem(item);
                for (ItemStack drop : overflow.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
                inventory.setItem(i, null);
            }
        }
    }

    private void updateResult(Inventory inventory) {
        ItemStack[] grid = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            ItemStack item = inventory.getItem(i);
            grid[i] = (item != null && !item.getType().isAir()) ? item : null;
        }
        ItemStack result = RecipeMatcher.matchRecipe(grid);
        inventory.setItem(CraftingSlotMap.SLOT_RESULT, result != null ? result : null);
    }

    private void craftOne(Inventory inventory, Player player, ItemStack result) {
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(result);
        for (ItemStack drop : overflow.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
        }
        for (int i = 0; i < 9; i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && !item.getType().isAir()) {
                item.setAmount(item.getAmount() - 1);
                if (item.getAmount() <= 0) inventory.setItem(i, null);
            }
        }
    }

    private void craftMultiple(Inventory inventory, Player player, ItemStack baseResult) {
        while (true) {
            ItemStack[] grid = new ItemStack[9];
            for (int i = 0; i < 9; i++) {
                ItemStack item = inventory.getItem(i);
                grid[i] = (item != null && !item.getType().isAir()) ? item : null;
            }
            ItemStack result = RecipeMatcher.matchRecipe(grid);
            if (result == null) break;

            int max = RecipeMatcher.maxCraftable(grid);
            int craftable = Math.min(max, result.getMaxStackSize());
            result.setAmount(craftable);
            Map<Integer, ItemStack> overflow = player.getInventory().addItem(result);
            for (ItemStack drop : overflow.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
            for (int i = 0; i < 9; i++) {
                ItemStack item = inventory.getItem(i);
                if (item != null && !item.getType().isAir()) {
                    item.setAmount(item.getAmount() - craftable);
                    if (item.getAmount() <= 0) inventory.setItem(i, null);
                }
            }
            if (max <= 1) break;
        }
    }

    private void performQuickCraft(Inventory inventory, Player player) {
        craftMultiple(inventory, player, inventory.getItem(CraftingSlotMap.SLOT_RESULT));
    }
}
