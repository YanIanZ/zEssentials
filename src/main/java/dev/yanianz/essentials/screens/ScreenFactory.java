package dev.yanianz.essentials.screens;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Tiny generic screen factory for list style inventories with built in
 * pagination and slot bound click callbacks. List guis of the plugin build
 * on this instead of hand rolling an inventory holder each time.
 */
public final class ScreenFactory {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private final JavaPlugin plugin;

    public ScreenFactory(JavaPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(new ScreenClickListener(), plugin);
    }

    /** One clickable entry of a screen. */
    public record ScreenItem(Material material, String nameLegacy, List<String> loreLegacy,
                             Consumer<Player> clickAction) {
    }

    /** Holder carrying the slot actions and pagination state of one open page. */
    public static final class Screen implements InventoryHolder {

        private final UUID playerId;
        private final List<Inventory> allPages;
        private final int pageIndex;
        private final int size;
        private Inventory inventory;
        private final Map<Integer, Consumer<Player>> actions = new HashMap<>();

        Screen(UUID playerId, List<Inventory> allPages, int pageIndex, int size) {
            this.playerId = playerId;
            this.allPages = allPages;
            this.pageIndex = pageIndex;
            this.size = size;
        }

        void action(int slot, Consumer<Player> consumer) {
            if (consumer != null) this.actions.put(slot, consumer);
        }

        Consumer<Player> actionAt(int slot) {
            return this.actions.get(slot);
        }

        @Override
        public Inventory getInventory() {
            return this.inventory;
        }
    }

    /**
     * Opens a paginated screen for the player.
     *
     * @param titleLegacy legacy colored inventory title
     * @param rows        total rows between 3 and 6, the last row holds controls
     */
    public void open(Player player, String titleLegacy, int rows, List<ScreenItem> items) {

        rows = Math.max(3, Math.min(6, rows));
        int size = rows * 9;
        int contentSlots = size - 9;

        List<List<ScreenItem>> chunks = new ArrayList<>();
        for (int index = 0; index < Math.max(1, items.size()); index += contentSlots) {
            chunks.add(items.subList(index, Math.min(items.size(), index + contentSlots)));
        }
        if (chunks.isEmpty()) chunks.add(List.of());

        UUID uniqueId = player.getUniqueId();
        List<Inventory> inventories = new ArrayList<>();
        List<Screen> screens = new ArrayList<>();

        for (int pageIndex = 0; pageIndex < chunks.size(); pageIndex++) {
            Screen screen = new Screen(uniqueId, inventories, pageIndex, size);
            Inventory inventory = Bukkit.createInventory(screen, size,
                    LEGACY.deserialize(colorize(titleLegacy)
                            + (chunks.size() > 1 ? " §8(" + (pageIndex + 1) + "/" + chunks.size() + ")" : "")));
            screen.inventory = inventory;
            inventories.add(inventory);
            screens.add(screen);

            fill(inventory);

            int slot = 0;
            for (ScreenItem item : chunks.get(pageIndex)) {
                ItemStack itemStack = item(material(item.material()), item.nameLegacy(), item.loreLegacy());
                inventory.setItem(slot, itemStack);
                screen.action(slot, item.clickAction());
                if (++slot >= contentSlots) break;
            }

            ItemStack filler = button(Material.GRAY_STAINED_GLASS_PANE, " ", null);
            for (int controlSlot = size - 9; controlSlot < size; controlSlot++) {
                if (inventory.getItem(controlSlot) == null || inventory.getItem(controlSlot).getType().isAir()) {
                    inventory.setItem(controlSlot, filler);
                }
            }

            final int current = pageIndex;
            if (pageIndex > 0) {
                inventory.setItem(size - 9, button(Material.ARROW, "&7« Previous", null));
                screen.action(size - 9, viewer -> viewer.openInventory(inventories.get(current - 1)));
            }
            inventory.setItem(size - 5, button(Material.BARRIER, "&cClose", null));
            screen.action(size - 5, Player::closeInventory);
            if (pageIndex < chunks.size() - 1) {
                inventory.setItem(size - 1, button(Material.ARROW, "&7Next »", null));
                screen.action(size - 1, viewer -> viewer.openInventory(inventories.get(current + 1)));
            }
        }

        player.openInventory(inventories.get(0));
    }

    private Material material(Material material) {
        return material == null ? Material.PAPER : material;
    }

    private void fill(Inventory inventory) {
        ItemStack filler = button(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack existing = inventory.getItem(slot);
            if (existing == null || existing.getType().isAir()) inventory.setItem(slot, filler);
        }
    }

    private ItemStack button(Material material, String nameLegacy, List<String> loreLegacy) {

        ItemStack itemStack = new ItemStack(material);
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.displayName(LEGACY.deserialize(colorize(nameLegacy)));
            if (loreLegacy != null && !loreLegacy.isEmpty()) {
                meta.lore(loreLegacy.stream().map(line -> LEGACY.deserialize(colorize(line))).toList());
            }
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }

    private ItemStack item(Material material, String nameLegacy, List<String> loreLegacy) {
        return button(material, nameLegacy, loreLegacy);
    }

    private final class ScreenClickListener implements org.bukkit.event.Listener {

        @EventHandler
        public void onClick(org.bukkit.event.inventory.InventoryClickEvent event) {
            if (!(event.getInventory().getHolder() instanceof Screen screen)) return;

            event.setCancelled(true);
            if (!(event.getWhoClicked() instanceof Player player)) return;

            ItemStack current = event.getCurrentItem();
            if (current == null || current.getType().isAir()) return;
            if (event.getClickedInventory() != event.getInventory()) return;

            Consumer<Player> action = screen.actionAt(event.getSlot());
            if (action != null) action.accept(player);
        }
    }

    private String colorize(String text) {
        return text == null ? "" : text.replace("&", "§");
    }
}
