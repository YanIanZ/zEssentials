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
    private final Map<PickerKey, java.util.function.BiConsumer<Player, InventoryClickEvent>> pickerActions = new ConcurrentHashMap<>();

    private record PickerKey(Inventory inventory, int slot) {
    }

    private void pickerAction(Inventory inventory, int slot,
                              java.util.function.BiConsumer<Player, InventoryClickEvent> action) {
        this.pickerActions.put(new PickerKey(inventory, slot), action);
    }

    public ScreenFactory(JavaPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(new ScreenClickListener(), plugin);
    }

    /** One clickable entry of a screen. */
    public record ScreenItem(Material material, String nameLegacy, List<String> loreLegacy,
                             java.util.function.BiConsumer<Player, org.bukkit.event.inventory.InventoryClickEvent> clickAction) {
    }

    /** Holder carrying the slot actions and pagination state of one open page. */
    public static final class Screen implements InventoryHolder {

        private final UUID playerId;
        private final List<Inventory> allPages;
        private final int pageIndex;
        private final int size;
        private Inventory inventory;
        private final Map<Integer, java.util.function.BiConsumer<Player, InventoryClickEvent>> actions = new HashMap<>();

        Screen(UUID playerId, List<Inventory> allPages, int pageIndex, int size) {
            this.playerId = playerId;
            this.allPages = allPages;
            this.pageIndex = pageIndex;
            this.size = size;
        }

        void action(int slot, java.util.function.BiConsumer<Player, InventoryClickEvent> consumer) {
            if (consumer != null) this.actions.put(slot, consumer);
        }

        java.util.function.BiConsumer<Player, InventoryClickEvent> actionAt(int slot) {
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
                ItemStack itemStack = button(item.material(), item.nameLegacy(), item.loreLegacy());
                inventory.setItem(slot, itemStack);
                var clickAction = item.clickAction();
                if (clickAction != null) {
                    screen.action(slot, (viewer, ev) -> clickAction.accept(viewer, ev));
                }
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
                screen.action(size - 9, (viewer, ev) -> viewer.openInventory(inventories.get(current - 1)));
            }
            inventory.setItem(size - 5, button(Material.BARRIER, "&cClose", null));
            screen.action(size - 5, (viewer, ev) -> viewer.closeInventory());
            if (pageIndex < chunks.size() - 1) {
                inventory.setItem(size - 1, button(Material.ARROW, "&7Next »", null));
                screen.action(size - 1, (viewer, ev) -> viewer.openInventory(inventories.get(current + 1)));
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
        public void onClick(InventoryClickEvent event) {

            // Category picker: cancel blindly and run the stored action
            if (event.getInventory().getHolder() instanceof PickerHolder) {
                event.setCancelled(true);
                if (!(event.getWhoClicked() instanceof Player player)) return;

                var pickerActionValue = this.pickerActions(event);
                if (pickerActionValue != null) pickerActionValue.accept(player, event);
                return;
            }

            if (!(event.getInventory().getHolder() instanceof Screen screen)) return;

            event.setCancelled(true);
            if (!(event.getWhoClicked() instanceof Player player)) return;

            ItemStack current = event.getCurrentItem();
            if (current == null || current.getType().isAir()) return;
            if (event.getClickedInventory() != event.getInventory()) return;

            java.util.function.BiConsumer<Player, InventoryClickEvent> action = screen.actionAt(event.getSlot());
            if (action != null) action.accept(player, event);
        }

        private java.util.function.BiConsumer<Player, InventoryClickEvent> pickerActions(InventoryClickEvent event) {
            return ScreenFactory.this.pickerActions
                    .get(new PickerKey(event.getInventory(), event.getSlot()));
        }
    }

    /** Holder marker for the category picker inventory. */
    private static final class PickerHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null; // clicks are cancelled blindly for this holder type
        }
    }

    /**
     * Opens a category picker followed by one paginated screen per category.
     *
     * @param categories ordered map of category title to its items
     */
    public void openCategorized(Player player, String titleLegacy, int rows,
                                java.util.LinkedHashMap<String, List<ScreenItem>> categories) {

        if (categories.isEmpty()) {
            player.sendMessage(LEGACY.deserialize(colorize("&7Nothing to show yet.")));
            return;
        }

        UUID uniqueId = player.getUniqueId();
        int size = Math.max(3, Math.min(6, rows)) * 9;

        PickerHolder pickerHolder = new PickerHolder();
        Inventory picker = Bukkit.createInventory(pickerHolder, 27,
                LegacyComponentSerializer.legacySection().deserialize(colorize(titleLegacy)));

        ItemStack filler = button(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        for (int slot = 0; slot < 27; slot++) picker.setItem(slot, filler);

        int slot = 10;
        for (Map.Entry<String, List<ScreenItem>> entry : categories.entrySet()) {

            if (slot > 16) break;

            List<Inventory> chain = new ArrayList<>();
            List<Screen> chainScreens = new ArrayList<>();
            buildChain(uniqueId, titleLegacy + " §8» " + entry.getKey(), entry.getValue(), size, chain, chainScreens);

            Material icon = entry.getValue().isEmpty()
                    ? Material.BOOK
                    : entry.getValue().get(0).material();
            Inventory firstPage = chain.get(0);

            picker.setItem(slot, button(icon, "&b" + entry.getKey(),
                    List.of(colorize("&7Open this category"))));
            final Inventory target = firstPage;
            pickerAction(picker, slot, (viewer, ev) -> viewer.openInventory(target));

            // First page of the chain closes back to the categories instead of a dead arrow
            if (chain.size() == 1) {
                chainScreens.get(0).action(size - 9, (viewer, ev) -> viewer.closeInventory());
                chain.get(0).setItem(size - 9, button(Material.BARRIER, "&cClose", null));
            }

            slot += 2;
        }

        picker.setItem(26, button(Material.BARRIER, "&cClose", null));
        pickerAction(picker, 26, (viewer, ev) -> viewer.closeInventory());

        player.openInventory(picker);
    }

    private void buildChain(UUID uniqueId, String titleLegacy, List<ScreenItem> items, int size,
                            List<Inventory> inventories, List<Screen> screens) {

        int contentSlots = size - 9;
        List<List<ScreenItem>> chunks = new ArrayList<>();
        for (int index = 0; index < Math.max(1, items.size()); index += contentSlots) {
            chunks.add(items.subList(index, Math.min(items.size(), index + contentSlots)));
        }
        if (chunks.isEmpty()) chunks.add(List.of());

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
                ItemStack itemStack = button(item.material(), item.nameLegacy(), item.loreLegacy());
                inventory.setItem(slot, itemStack);
                var clickAction = item.clickAction();
                if (clickAction != null) {
                    screen.action(slot, (viewer, ev) -> clickAction.accept(viewer, ev));
                }
                if (++slot >= contentSlots) break;
            }

            ItemStack controlFiller = button(Material.GRAY_STAINED_GLASS_PANE, " ", null);
            for (int controlSlot = size - 9; controlSlot < size; controlSlot++) {
                if (inventory.getItem(controlSlot) == null) inventory.setItem(controlSlot, controlFiller);
            }

            final int current = pageIndex;
            if (pageIndex > 0) {
                inventory.setItem(size - 9, button(Material.ARROW, "&7« Previous", null));
                screen.action(size - 9, (viewer, ev) -> viewer.openInventory(inventories.get(current - 1)));
            }
            inventory.setItem(size - 5, button(Material.BARRIER, "&cClose", null));
            screen.action(size - 5, (viewer, ev) -> viewer.closeInventory());
            if (pageIndex < chunks.size() - 1) {
                inventory.setItem(size - 1, button(Material.ARROW, "&7Next »", null));
                screen.action(size - 1, (viewer, ev) -> viewer.openInventory(inventories.get(current + 1)));
            }
        }
    }

    

    private String colorize(String text) {
        return text == null ? "" : text.replace("&", "§");
    }
}
