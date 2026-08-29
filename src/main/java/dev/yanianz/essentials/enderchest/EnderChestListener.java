package dev.yanianz.essentials.enderchest;

import fr.maxlego08.essentials.ZEssentialsPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class EnderChestListener implements Listener {

    private final ZEssentialsPlugin plugin;

    public EnderChestListener(ZEssentialsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory inventory = event.getInventory();
        if (!(inventory.getHolder() instanceof EnderChestHolder holder)) return;
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
        } else {
            int rawSlot = event.getRawSlot();
            if (rawSlot < 0) return;
            plugin.getScheduler().runNextTick(wrappedTask -> syncAllSlots(holder));
        }
    }

    private void handleNavClick(Player player, EnderChestHolder holder, int slot) {
        if (slot == EnderChestSlotMap.SLOT_CLOSE) {
            player.closeInventory();
        } else if (slot == EnderChestSlotMap.SLOT_PREV) {
            syncAllSlots(holder);
            EnderChestGui.switchPage(plugin, player, holder, holder.getCurrentPage() - 1);
        } else if (slot == EnderChestSlotMap.SLOT_NEXT) {
            syncAllSlots(holder);
            EnderChestGui.switchPage(plugin, player, holder, holder.getCurrentPage() + 1);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        Inventory inventory = event.getInventory();
        if (!(inventory.getHolder() instanceof EnderChestHolder holder)) return;

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

        plugin.getScheduler().runNextTick(wrappedTask -> {
            for (int slot : event.getRawSlots()) {
                if (EnderChestSlotMap.isContentSlot(slot)) syncSlot(holder, slot);
            }
        });
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();
        if (!(inventory.getHolder() instanceof EnderChestHolder holder)) return;
        if (holder.isReadOnly()) return;

        syncAllSlots(holder);
        EnderChestModule module = plugin.getModuleManager().getModule(EnderChestModule.class);
        module.saveData(holder.getPlayerId());
    }

    private void syncSlot(EnderChestHolder holder, int slot) {
        Inventory inventory = holder.getInventory();
        ItemStack item = inventory.getItem(slot);
        holder.getData().setContent(holder.getCurrentPage(), slot,
                item == null || item.getType().isAir() ? null : item);
    }

    private void syncAllSlots(EnderChestHolder holder) {
        Inventory inventory = holder.getInventory();
        for (int slot = 0; slot < EnderChestSlotMap.CONTENT_SLOTS; slot++) {
            ItemStack item = inventory.getItem(slot);
            holder.getData().setContent(holder.getCurrentPage(), slot,
                    item == null || item.getType().isAir() ? null : item);
        }
    }
}
