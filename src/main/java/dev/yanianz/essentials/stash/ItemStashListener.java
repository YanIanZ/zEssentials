package dev.yanianz.essentials.stash;

import dev.yanianz.essentials.enderchest.EnderChestSlotMap;
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

public class ItemStashListener implements Listener {

    private final ZEssentialsPlugin plugin;
    private static boolean registered = false;

    public ItemStashListener(ZEssentialsPlugin plugin) {
        this.plugin = plugin;
    }

    public static void ensureRegistered(ZEssentialsPlugin plugin) {
        if (!registered) {
            Bukkit.getPluginManager().registerEvents(new ItemStashListener(plugin), plugin);
            registered = true;
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory inventory = event.getInventory();
        if (!(inventory.getHolder() instanceof ItemStashHolder holder)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        int slot = event.getRawSlot();

        if (EnderChestSlotMap.isNavSlot(slot)) {
            event.setCancelled(true);
            handleNavClick(player, holder, slot);
            return;
        }

        if (holder.isReadOnly()) {
            event.setCancelled(true);
            return;
        }

        if (EnderChestSlotMap.isContentSlot(slot)) {
            plugin.getScheduler().runNextTick(wrappedTask -> syncSlot(holder, slot));
            return;
        }

        if (slot < 0) return;
        if (event.getClick().isShiftClick()) {
            plugin.getScheduler().runNextTick(wrappedTask -> syncAllSlots(holder));
        }
    }

    private void handleNavClick(Player player, ItemStashHolder holder, int slot) {
        if (slot == EnderChestSlotMap.SLOT_CLOSE) {
            player.closeInventory();
        } else if (slot == EnderChestSlotMap.SLOT_PREV) {
            plugin.getScheduler().runNextTick(wrappedTask -> ItemStashGui.switchPage(
                    plugin, player, holder, holder.getCurrentPage() - 1));
        } else if (slot == EnderChestSlotMap.SLOT_NEXT) {
            plugin.getScheduler().runNextTick(wrappedTask -> ItemStashGui.switchPage(
                    plugin, player, holder, holder.getCurrentPage() + 1));
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        Inventory inventory = event.getInventory();
        if (!(inventory.getHolder() instanceof ItemStashHolder holder)) return;
        if (holder.isReadOnly()) {
            event.setCancelled(true);
            return;
        }
        for (int slot : event.getRawSlots()) {
            if (EnderChestSlotMap.isNavSlot(slot)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();
        if (!(inventory.getHolder() instanceof ItemStashHolder holder)) return;
        if (holder.isReadOnly()) return;
        syncAllSlots(holder);
        plugin.getModuleManager().getModule(fr.maxlego08.essentials.module.modules.StashModule.class)
                .saveItemData(holder.getPlayerId());
    }

    private void syncSlot(ItemStashHolder holder, int slot) {
        Inventory inventory = holder.getInventory();
        ItemStack item = inventory.getItem(slot);
        holder.getData().setContent(holder.getCurrentPage(), slot,
                item == null || item.getType().isAir() ? null : item);
    }

    private void syncAllSlots(ItemStashHolder holder) {
        Inventory inventory = holder.getInventory();
        for (int slot = 0; slot < EnderChestSlotMap.CONTENT_SLOTS; slot++) {
            ItemStack item = inventory.getItem(slot);
            holder.getData().setContent(holder.getCurrentPage(), slot,
                    item == null || item.getType().isAir() ? null : item);
        }
    }
}