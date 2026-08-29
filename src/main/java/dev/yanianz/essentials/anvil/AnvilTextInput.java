package dev.yanianz.essentials.anvil;

import fr.maxlego08.essentials.ZEssentialsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import net.kyori.adventure.text.Component;

public class AnvilTextInput implements Listener {

    private final ZEssentialsPlugin plugin;
    private final Map<UUID, Consumer<String>> sessions = new HashMap<>();

    public AnvilTextInput(ZEssentialsPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player player, String title, String placeholder, Consumer<String> callback) {
        Inventory anvil = Bukkit.createInventory(player, InventoryType.ANVIL,
                Component.text(placeholder != null ? placeholder : title));

        ItemStack paper = new ItemStack(Material.PAPER);
        ItemMeta meta = paper.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(title));
            paper.setItemMeta(meta);
        }
        anvil.setItem(0, paper);

        this.sessions.put(player.getUniqueId(), callback);
        player.openInventory(anvil);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory() instanceof AnvilInventory anvil)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Consumer<String> callback = this.sessions.get(player.getUniqueId());
        if (callback == null) return;

        if (event.getSlot() == 2) {
            event.setCancelled(true);
            String text = anvil.getRenameText();
            this.sessions.remove(player.getUniqueId());
            player.closeInventory();
            callback.accept(text != null ? text.trim() : "");
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        this.sessions.remove(event.getPlayer().getUniqueId());
    }
}
