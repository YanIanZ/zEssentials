package dev.yanianz.essentials.chatcustomization;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fr.maxlego08.essentials.ZEssentialsPlugin;
import fr.maxlego08.essentials.api.configuration.NonLoadable;
import fr.maxlego08.essentials.module.ZModule;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per player chat color, decorations and prefix tags selected in small
 * inventories, applied while the chat renders.
 */
public class ChatCustomizationModule extends ZModule {

    @NonLoadable
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private static final String[] COLOR_CODES = {
            "&0", "&1", "&2", "&3", "&4", "&5", "&6", "&7",
            "&8", "&9", "&a", "&b", "&c", "&d", "&e", "&f"
    };
    private static final String[] COLOR_NAMES = {
            "Black", "Dark Blue", "Dark Green", "Dark Aqua", "Dark Red", "Purple", "Gold", "Gray",
            "Dark Gray", "Blue", "Green", "Aqua", "Red", "Pink", "Yellow", "White"
    };

    // Stable clickable slots
    @NonLoadable
    private static final int SLOT_RESET = 4;
    @NonLoadable
    private static final int SLOT_CLOSE_COLOR = 40;
    @NonLoadable
    private static final int SLOT_BOLD = 28;
    @NonLoadable
    private static final int SLOT_ITALIC = 30;

    private String usePermission;
    private String decorationsPermission;
    private List<TagEntry> tags = new ArrayList<>();

    @NonLoadable
    private final Map<UUID, Preference> preferences = new ConcurrentHashMap<>();
    @NonLoadable
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public ChatCustomizationModule(ZEssentialsPlugin plugin) {
        super(plugin, "chatcustomization");
    }

    public record TagEntry(String name, String text, String permission) {
    }

    /** Chat rendering preference of one player. */
    public record Preference(String colorCode, List<String> decorations, String tagText) {

        public static final Preference DEFAULT = new Preference("", List.of(), "");
    }

    /** Marker holder carrying which gui an open inventory belongs to. */
    private static final class Holder implements InventoryHolder {

        enum Kind {
            COLOR,
            TAGS
        }

        private Kind kind;
        // Tag gui: parallel lists of slot and tag index inside this.tags
        private final List<Integer> tagSlots = new ArrayList<>();
        private final List<Integer> tagIndexes = new ArrayList<>();
        // Color gui: slot -> color index
        private final Map<Integer, Integer> colorSlots = new HashMap<>();

        private Inventory inventory;

        @Override
        public Inventory getInventory() {
            return this.inventory;
        }
    }

    @Override
    public void loadConfiguration() {
        super.loadConfiguration();

        var config = getConfiguration();
        this.usePermission = config.getString("use-permission", "essentials.chatcustomization.use");
        this.decorationsPermission = config.getString("decorations-permission", "essentials.chatcustomization.decorations");

        this.tags.clear();
        for (Object obj : config.getMapList("tags")) {
            if (!(obj instanceof java.util.Map<?, ?> raw)) continue;
            Object nameObj = ((Map<?, ?>) raw).get("name");
            Object textObj = ((Map<?, ?>) raw).get("text");
            Object permObj = ((Map<?, ?>) raw).get("permission");
            this.tags.add(new TagEntry(
                    nameObj == null ? "tag" : String.valueOf(nameObj),
                    textObj == null ? "" : String.valueOf(textObj),
                    permObj == null ? "" : String.valueOf(permObj)));
        }

        loadStorage();
    }

    /**
     * Returns the chat preference of a player, or the default one.
     */
    public Preference getPreference(UUID uniqueId) {
        return this.preferences.getOrDefault(uniqueId, Preference.DEFAULT);
    }

    /**
     * Legacy text of the selected tag of a player, empty when none is chosen.
     */
    public String resolveTagText(UUID uniqueId) {
        return getPreference(uniqueId).tagText();
    }

    /* ── Color gui ─────────────────────────────────────────── */

    /**
     * Opens the color and decoration gui (5 rows).
     */
    public void openColorGui(Player player) {

        if (!check(player)) return;

        Holder holder = new Holder();
        holder.kind = Holder.Kind.COLOR;
        Inventory inventory = Bukkit.createInventory(holder, 45,
                LEGACY.deserialize(colorize("&b&lChat Colors")));
        holder.inventory = inventory;

        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, " ", null, false);
        for (int slot = 0; slot < 45; slot++) inventory.setItem(slot, filler);

        inventory.setItem(SLOT_RESET, item(Material.WATER_BUCKET, "&b&lReset All",
                List.of(colorize("&7Removes your chat color"), colorize("&7and every decoration.")), false));

        Preference pref = getPreference(player.getUniqueId());

        // Sixteen colors over two centered rows
        int firstRowStart = 10;
        int secondRowStart = 19;
        for (int index = 0; index < COLOR_CODES.length; index++) {
            boolean selected = COLOR_CODES[index].equals(pref.colorCode());
            int slot = index < 8 ? firstRowStart + index : secondRowStart + (index - 8);
            holder.colorSlots.put(slot, index);

            List<String> lore = new ArrayList<>();
            lore.add(colorize("&7Sample&8: " + COLOR_CODES[index] + colorize(COLOR_NAMES[index])));
            lore.add(colorize(selected ? "&aSelected" : "&7Click to select"));

            ItemStack itemStack = item(Material.WHITE_WOOL,
                    COLOR_CODES[index] + COLOR_NAMES[index], lore, selected);
            inventory.setItem(slot, itemStack);
        }

        boolean canDecorate = canDecorate(player);
        placeToggle(inventory, SLOT_BOLD, Material.GOLD_INGOT, "&eBold",
                pref.decorations().contains("l"), canDecorate);
        placeToggle(inventory, SLOT_ITALIC, Material.IRON_INGOT, "&e&oItalic",
                pref.decorations().contains("o"), canDecorate);

        inventory.setItem(SLOT_CLOSE_COLOR, item(Material.BARRIER, "&cClose", null, false));

        player.openInventory(inventory);
        clickSound(player);
    }

    private void placeToggle(Inventory inventory, int slot, Material material,
                             String legacyName, boolean active, boolean allowed) {

        List<String> lore = new ArrayList<>();
        lore.add(colorize(allowed ? "&7Click to toggle"
                : "&cRequires " + this.decorationsPermission));
        lore.add(colorize("&7State&8: " + (active ? "&aON" : "&cOFF")));

        ItemStack itemStack = item(material, legacyName, lore, active && allowed);
        inventory.setItem(slot, itemStack);
    }

    /* ── Tags gui ─────────────────────────────────────────── */

    /**
     * Opens the tag selection gui with a leading none entry.
     */
    public void openTagsGui(Player player) {

        if (!check(player)) return;

        int entries = Math.min(this.tags.size(), 21);
        int size = ((entries - 1) / 7 + 3) * 9;

        Holder holder = new Holder();
        holder.kind = Holder.Kind.TAGS;
        Inventory inventory = Bukkit.createInventory(holder, size,
                LEGACY.deserialize(colorize("&6&lChat Tags")));
        holder.inventory = inventory;

        ItemStack filler = item(Material.BLUE_STAINED_GLASS_PANE, " ", null, false);
        for (int slot = 0; slot < size; slot++) inventory.setItem(slot, filler);

        Preference pref = getPreference(player.getUniqueId());

        int slot = 10;
        for (int index = 0; index < this.tags.size(); index++, slot++) {

            TagEntry tag = this.tags.get(index);
            boolean allowed = tag.permission().isEmpty() || player.hasPermission(tag.permission());
            boolean selected = tag.text().equals(pref.tagText());

            List<String> lore = new ArrayList<>();
            if (!tag.text().isBlank()) lore.add(colorize("&7Preview&8: &f" + tag.text().trim() + "hello"));
            else lore.add(colorize("&7No tag before your messages."));
            lore.add("");
            lore.add(colorize(allowed ? (selected ? "&aSelected ✔" : "&7Click to select") : "&cLocked"));

            ItemStack itemStack = item(allowed ? Material.WRITABLE_BOOK : Material.GRAY_DYE,
                    colorize(tag.name()) + (selected && allowed ? " &a✔" : ""), lore, selected && allowed);
            inventory.setItem(slot, itemStack);
            holder.tagSlots.add(slot);
            holder.tagIndexes.add(index);

            if ((slot + 2) % 9 == 0) slot += 2;
            if (slot >= size - 9) break;
        }

        inventory.setItem(size - 5, item(Material.BARRIER, "&cClose", null, false));

        player.openInventory(inventory);
        clickSound(player);
    }

    /* ── Clicks ───────────────────────────────────────────── */

    /**
     * Handles every click of both guis through stable slot identifiers.
     */
    @EventHandler
    public void onClick(InventoryClickEvent event) {

        if (!(event.getInventory().getHolder() instanceof Holder holder)) return;

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack current = event.getCurrentItem();
        if (current == null || current.getType().isAir()) return;
        if (event.getClickedInventory() != event.getInventory()) return;

        Preference pref = getPreference(player.getUniqueId());
        int slot = event.getSlot();

        if (holder.kind == Holder.Kind.COLOR) {

            switch (slot) {
                case SLOT_CLOSE_COLOR -> player.closeInventory();
                case SLOT_RESET -> {
                    save(player, "", List.of(), pref.tagText());
                    playSelectSound(player);
                    openColorGui(player);
                }
                case SLOT_BOLD -> {
                    List<String> decorations = new ArrayList<>(pref.decorations());
                    toggle(decorations, "l");
                    save(player, pref.colorCode(), decorations, pref.tagText());
                    playSelectSound(player);
                    openColorGui(player);
                }
                case SLOT_ITALIC -> {
                    List<String> decorations = new ArrayList<>(pref.decorations());
                    toggle(decorations, "o");
                    save(player, pref.colorCode(), decorations, pref.tagText());
                    playSelectSound(player);
                    openColorGui(player);
                }
                default -> {
                    Integer colorIndex = holder.colorSlots.get(slot);
                    if (colorIndex != null) {
                        save(player, COLOR_CODES[colorIndex], pref.decorations(), pref.tagText());
                        playSelectSound(player);
                        player.sendMessage(LEGACY.deserialize(colorize("&aChat color selected&7: "
                                + COLOR_CODES[colorIndex] + COLOR_NAMES[colorIndex])));
                        player.closeInventory();
                    }
                }
            }
            return;
        }

        // Tags gui: resolve through the recorded slot mapping
        int tagIndexPosition = holder.tagSlots.indexOf(slot);
        if (tagIndexPosition < 0 || tagIndexPosition >= holder.tagIndexes.size()) return;

        TagEntry tag = this.tags.get(holder.tagIndexes.get(tagIndexPosition));
        boolean allowed = tag.permission().isEmpty() || player.hasPermission(tag.permission());
        if (!allowed) {
            player.sendMessage(LEGACY.deserialize(colorize("&cYou cannot use this tag.")));
            return;
        }

        save(player, pref.colorCode(), pref.decorations(), tag.text());
        playSelectSound(player);
        player.sendMessage(LEGACY.deserialize(colorize("&aTag updated! Messages now start with&7: &f"
                + (tag.text().isBlank() ? "(no tag)" : tag.text().trim()))));
        player.closeInventory();
    }

    /* ── Shared helpers ───────────────────────────────────── */

    private boolean check(Player player) {
        if (!this.isEnable) return false;
        if (!this.usePermission.isEmpty() && !player.hasPermission(this.usePermission)) {
            player.sendMessage(LEGACY.deserialize(colorize("&cYou cannot customize your chat.")));
            return false;
        }
        return true;
    }

    private ItemStack item(Material material, String name, List<String> lore, boolean shine) {
        ItemStack itemStack = new ItemStack(material);
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.displayName(LEGACY.deserialize(colorize(name)));
            if (lore != null && !lore.isEmpty()) {
                meta.lore(lore.stream().map(line -> LEGACY.deserialize(colorize(line))).toList());
            }
            meta.setEnchantmentGlintOverride(shine);
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }

    private void toggle(List<String> decorations, String code) {
        if (decorations.contains(code)) decorations.remove(code);
        else decorations.add(code);
    }

    private String colorize(String text) {
        return text == null ? "" : text.replace("&", "§");
    }

    private void playSelectSound(Player player) {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.6f);
    }

    private void clickSound(Player player) {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.4f);
    }

    private boolean canDecorate(Player player) {
        return this.decorationsPermission.isEmpty() || player.hasPermission(this.decorationsPermission);
    }

    private void save(Player player, String colorCode, List<String> decorations, String tagText) {
        this.preferences.put(player.getUniqueId(),
                new Preference(colorCode, List.copyOf(decorations), tagText));
    }

    /* ── persistence ──────────────────────────────────────── */

    private File getStorageFile() {
        return new File(getFolder(), "preferences.json");
    }

    @Override
    public void onDisable() {
        super.onDisable();
        saveStorage();
    }

    private void loadStorage() {
        File file = getStorageFile();
        if (!file.exists()) return;
        try {
            String json = Files.readString(file.toPath());
            Storage storage = this.gson.fromJson(json, Storage.class);
            if (storage == null || storage.entries == null) return;

            for (StorageEntry entry : storage.entries) {
                try {
                    this.preferences.put(UUID.fromString(entry.uuid),
                            new Preference(entry.color == null ? "" : entry.color,
                                    entry.decorations == null ? List.<String>of() : java.util.Arrays.asList(entry.decorations),
                                    entry.tag == null ? "" : entry.tag));
                } catch (IllegalArgumentException ignored) {
                }
            }
        } catch (IOException | RuntimeException exception) {
            exception.printStackTrace();
        }
    }

    private void saveStorage() {
        Storage storage = new Storage();
        for (Map.Entry<UUID, Preference> entry : this.preferences.entrySet()) {
            StorageEntry storageEntry = new StorageEntry();
            storageEntry.uuid = entry.getKey().toString();
            storageEntry.color = entry.getValue().colorCode();
            storageEntry.decorations = entry.getValue().decorations().toArray(new String[0]);
            storageEntry.tag = entry.getValue().tagText();
            storage.entries.add(storageEntry);
        }
        try {
            Files.writeString(getStorageFile().toPath(), this.gson.toJson(storage));
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    private static final class Storage {
        List<StorageEntry> entries = new ArrayList<>();
    }

    private static final class StorageEntry {
        String uuid;
        String color;
        String[] decorations;
        String tag;
    }
}
