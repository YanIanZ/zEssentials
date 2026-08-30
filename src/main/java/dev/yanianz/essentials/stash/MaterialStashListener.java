package dev.yanianz.essentials.stash;

import fr.maxlego08.essentials.ZEssentialsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class MaterialStashListener implements Listener {

    private final ZEssentialsPlugin plugin;
    private static boolean registered = false;

    public MaterialStashListener(ZEssentialsPlugin plugin) { this.plugin = plugin; }

    public static void ensureRegistered(ZEssentialsPlugin plugin) {
        if (!registered) {
            Bukkit.getPluginManager().registerEvents(new MaterialStashListener(plugin), plugin);
            registered = true;
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory inventory = event.getInventory();
        if (!(inventory.getHolder() instanceof MaterialStashHolder holder)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 54) return;

        ItemStack clicked = inventory.getItem(slot);
        if (clicked == null || clicked.getType().isAir()) return;
        if (clicked.getType() == Material.BLACK_STAINED_GLASS_PANE
                || clicked.getType() == Material.BARRIER) {
            event.setCancelled(true);
            if (slot == 49) player.closeInventory();
            return;
        }

        event.setCancelled(true);
        if (holder.isReadOnly()) return;

        Material material = clicked.getType();
        long available = holder.getData().get(material);
        if (available <= 0) return;

        int maxStack = material.getMaxStackSize();
        int toWithdraw;

        if (event.getClick().isShiftClick()) {
            toWithdraw = (int) Math.min(available, (long) maxStack * 36);
        } else if (event.getClick().isRightClick()) {
            toWithdraw = (int) Math.min(available, (long) maxStack / 2);
            if (toWithdraw < 1) toWithdraw = 1;
        } else {
            toWithdraw = (int) Math.min(available, maxStack);
        }

        if (toWithdraw <= 0) return;

        holder.getData().remove(material, toWithdraw);

        int stacks = toWithdraw / maxStack;
        int remainder = toWithdraw % maxStack;
        for (int i = 0; i < stacks; i++) {
            player.getInventory().addItem(new ItemStack(material, maxStack));
        }
        if (remainder > 0) {
            player.getInventory().addItem(new ItemStack(material, remainder));
        }

        MaterialStashGui.fillMaterials(inventory, holder.getData(),
                plugin.getModuleManager().getModule(
                        fr.maxlego08.essentials.module.modules.StashModule.class));
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();
        if (!(inventory.getHolder() instanceof MaterialStashHolder holder)) return;
        if (holder.isReadOnly()) return;
        plugin.getModuleManager().getModule(fr.maxlego08.essentials.module.modules.StashModule.class)
                .saveMaterialData(holder.getPlayerId());
    }
}