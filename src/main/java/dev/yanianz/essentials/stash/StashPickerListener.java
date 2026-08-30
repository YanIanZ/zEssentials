package dev.yanianz.essentials.stash;

import fr.maxlego08.essentials.ZEssentialsPlugin;
import fr.maxlego08.essentials.module.modules.StashModule;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.WeakHashMap;

public class StashPickerListener implements Listener {

    private static final Map<Player, ZEssentialsPlugin> PLAYER_PLUGIN = new WeakHashMap<>();
    private static final Map<Player, StashModule> PLAYER_MODULE = new WeakHashMap<>();

    public static void register(ZEssentialsPlugin plugin, Player player, StashModule module) {
        PLAYER_PLUGIN.put(player, plugin);
        PLAYER_MODULE.put(player, module);
        org.bukkit.Bukkit.getPluginManager().registerEvents(new StashPickerListener(), plugin);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!PLAYER_PLUGIN.containsKey(player)) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) return;

        if (clicked.getType().toString().contains("SWORD")) {
            event.setCancelled(true);
            player.closeInventory();
            PLAYER_MODULE.get(player).openItemStash(player);
        } else if (clicked.getType().toString().contains("COBBLESTONE") || clicked.getType().toString().contains("STONE")) {
            event.setCancelled(true);
            player.closeInventory();
            PLAYER_MODULE.get(player).openMaterialStash(player);
        }
    }
}