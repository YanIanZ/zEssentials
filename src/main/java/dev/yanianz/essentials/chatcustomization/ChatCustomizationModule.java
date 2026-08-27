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
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per player chat color, decorations and prefix tags selected in small
 * inventories, applied while the chat renders.
 */
public class ChatCustomizationModule extends ZModule {

    @fr.maxlego08.essentials.api.configuration.NonLoadable
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    @fr.maxlego08.essentials.api.configuration.NonLoadable
    private static final String[] COLOR_CODES = {
            "&0", "&1", "&2", "&3", "&4", "&5", "&6", "&7",
            "&8", "&9", "&a", "&b", "&c", "&d", "&e", "&f"
    };

    private String usePermission;
    private String decorationsPermission;
    private List<TagEntry> tags = new ArrayList<>();

    @NonLoadable
    private final Map<UUID, Preference> preferences = new ConcurrentHashMap<>();

    public ChatCustomizationModule(ZEssentialsPlugin plugin) {
        super(plugin, "chatcustomization");
    }

    public record TagEntry(String name, String text, String permission) {
    }

    /** Chat rendering preference of one player. */
    public record Preference(String colorCode, List<String> decorations, String tagText) {

        public static final Preference DEFAULT = new Preference("", List.of(), "");
    }

    private static final class CustomizationHolder implements InventoryHolder {

        enum Kind {
            COLOR,
            TAGS
        }

        private Inventory inventory;
        private Kind kind;
        private final List<String> clickData = new ArrayList<>();
        private final List<String[]> tagData = new ArrayList<>();
        // clickData entries for colors: full legacy color code; tags: config text

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
            if (!(obj instanceof Map)) continue;
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) obj;
            this.tags.add(new TagEntry(
                    String.valueOf(map.getOrDefault("name", "tag")),
                    String.valueOf(map.getOrDefault("text", "")),
                    map.get("permission") == null ? "" : String.valueOf(map.get("permission"))));
        }

        loadStorage();
    }

    @NonLoadable
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Returns the chat preference of a player, or the default one.
     */
    public Preference getPreference(UUID uniqueId) {
        return this.preferences.getOrDefault(uniqueId, Preference.DEFAULT);
    }

    /**
     * Opens the color selection gui.
     */
    public void openColorGui(Player player) {
        if (!check(player)) return;

        CustomizationHolder holder = new CustomizationHolder();
        holder.kind = CustomizationHolder.Kind.COLOR;
        Inventory inventory = Bukkit.createInventory(holder, 27,
                LEGACY.deserialize(colorize("&bChat &lCOLORS")));
        holder.inventory = inventory;

        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        for (int slot = 0; slot < 27; slot++) inventory.setItem(slot, filler);

        int slot = 10;
        for (String code : COLOR_CODES) {
            if (slot % 9 == 8) slot++;
            holder.clickData.add(slot, code);
            inventory.setItem(slot++, item(Material.WHITE_WOOL, code + "&lSample " + code.toUpperCase(Locale.ROOT),
                    List.of(colorize("&7Click to select this color"))));
        }

        boolean canDecorate = player.hasPermission(this.decorationsPermission);
        Preference pref = getPreference(player.getUniqueId());
        if (canDecorate) {
            inventory.setItem(17, item(Material.GOLD_INGOT, "&e&lBOLD",
                    List.of(colorize("&7Toggle bold"), "", "&7Current: " + (pref.decorations().contains("l") ? "§aon" : "§coff"))));
            inventory.setItem(26, item(Material.IRON_INGOT, "&e&oITALIC",
                    List.of(colorize("&7Toggle italic"), "", "&7Current: " + (pref.decorations().contains("o") ? "§aon" : "§coff"))));
        }

        player.sendMessage(LEGACY.deserialize(colorize("&7Pick your chat color!")));
        player.openInventory(inventory);
    }

    /**
     * Opens the tag selection gui.
     */
    public void openTagsGui(Player player) {
        if (!check(player)) return;

        CustomizationHolder holder = new CustomizationHolder();
        holder.kind = CustomizationHolder.Kind.TAGS;
        Inventory inventory = Bukkit.createInventory(holder, 27,
                LEGACY.deserialize(colorize("&6Chat &lTAGS")));
        holder.inventory = inventory;

        ItemStack filler = item(Material.BLUE_STAINED_GLASS_PANE, " ", null);
        for (int slot = 0; slot < 27; slot++) inventory.setItem(slot, filler);

        int slot = 10;
        for (TagEntry tag : this.tags) {
            boolean allowed = tag.permission().isEmpty() || player.hasPermission(tag.permission());

            if (slot >= 25) break;
            holder.clickData.add(slot, allowed ? tag.text() : null);
            holder.tagData.add(new String[]{String.valueOf(slot), tag.name(), tag.text(), tag.permission()});
            inventory.setItem(slot++, item(allowed ? Material.NAME_TAG : Material.BARRIER,
                    tag.name(),
                    allowed ? List.of(colorize("&7Click to select"),
                            colorize("&7Preview: &f" + tag.text() + "message"))
                            : List.of(colorize("&cYou cannot use this tag."))));
        }

        player.openInventory(inventory);
    }

    private boolean check(Player player) {
        if (!this.isEnable) return false;
        if (!this.usePermission.isEmpty() && !player.hasPermission(this.usePermission)) {
            player.sendMessage(legacy("&cYou cannot customize your chat."));
            return false;
        }
        return true;
    }

    private ItemStack item(Material material, String name, List<String> lore) {
        ItemStack itemStack = new ItemStack(material);
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.displayName(LEGACY.deserialize(colorize(name)));
            if (lore != null && !lore.isEmpty()) {
                meta.lore(lore.stream().map(line -> LEGACY.deserialize(colorize(line))).toList());
            }
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }

    /**
     * Handles every click of both guis through the stored click data.
     */
    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof CustomizationHolder holder)) return;

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack current = event.getCurrentItem();
        if (current == null || current.getType().isAir()) return;
        if (event.getClickedInventory() != event.getInventory()) return;

        int slot = event.getSlot();
        String data = slot < holder.clickData.size() ? holder.clickData.get(slot) : null;
        if (data == null) return;

        switch (holder.kind) {
            case COLOR -> handleColorClick(player, slot, data);
            case TAGS -> handleTagClick(player, data);
        }
    }

    private void handleColorClick(Player player, int slot, String code) {

        Preference pref = getPreference(player.getUniqueId());

        // Decoration buttons share the same gui, slots 17 and 26
        if (slot == 17 || slot == 26) {
            if (!player.hasPermission(this.decorationsPermission)) return;
            String decoration = slot == 17 ? "l" : "o";
            List<String> decorations = new ArrayList<>(pref.decorations());
            toggle(decorations, decoration);
            save(player, pref.colorCode(), decorations, pref.tagText());
            openColorGui(player); // reopen with fresh states
            return;
        }

        String cleanedCode = code.equals("&f") ? "" : code;
        save(player, cleanedCode, pref.decorations(), pref.tagText());

        Component feedback = cleanedCode.isEmpty()
                ? Component.text("§aChat color reset to default.")
                : LEGACY.deserialize(cleanedCode + "&7Chat color set to that one.");
        player.sendMessage(feedback);
        player.closeInventory();
    }

    private void handleTagClick(Player player, String tagText) {

        Preference pref = getPreference(player.getUniqueId());
        save(player, pref.colorCode(), pref.decorations(), tagText);
        player.sendMessage(LEGACY.deserialize(colorize(
                "&aTag updated! Messages now start with: &f" + (tagText.isEmpty() ? "(no tag)" : tagText.trim()))));
        player.closeInventory();
    }

    private void toggle(List<String> decorations, String code) {
        if (decorations.contains(code)) decorations.remove(code);
        else decorations.add(code);
    }

    private void save(Player player, String colorCode, List<String> decorations, String tagText) {
        this.preferences.put(player.getUniqueId(),
                new Preference(colorCode, List.copyOf(decorations), tagText));
    }

    private Component legacy(String text) {
        return LEGACY.deserialize(colorize(text));
    }

    private String colorize(String text) {
        return text == null ? "" : text.replace("&", "§");
    }

    /* ── persistence ─────────────────────────────────────────── */

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
                                    entry.decorations == null ? java.util.List.<String>of() : java.util.Arrays.asList(entry.decorations),
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
