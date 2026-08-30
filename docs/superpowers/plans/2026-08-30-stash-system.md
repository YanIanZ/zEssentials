# Stash System Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans.

**Goal:** Add Item Stash (720 non-stackable items, 16 pages) + Material Stash (unlimited stackable materials) with JSON persistence.

**Architecture:** Reuses the EnderChestHolder/GUI/Listener pattern from #2. ItemStashData stores an ItemStack[] array; MaterialStashData stores a Map<Material, Long>. Both persist as JSON files using `ItemStack.serializeAsBytes()` + Base64.

**Tech Stack:** Java 21, Bukkit/Paper API, Gson, JUnit 5.

**Spec:** `docs/superpowers/specs/2026-08-30-stash-system-design.md`

## Global Constraints

- Java 21 bytecode target. Paper API is `compileOnly`.
- No comments in code unless explicitly requested.
- Folia-supported.
- Config self-healing: `config-version: 1` key in module config.
- Package convention: feature logic in `dev.yanianz.essentials.stash.*`, scaffold in `fr.maxlego08.essentials.module.modules.StashModule`.
- Reuses EnderChestSlotMap constants (CONTENT_SLOTS=45, NAV_ROW_START=45, etc.).
- Build: `./gradlew build -x test --console=plain`
- Test: `./gradlew test --console=plain --no-daemon`
- Working directory: `/Users/rheninxy/Sourby/zEssentials`

---

### Task 1: Config + StashModule + Registration

**Files:**
- Create: `src/main/resources/modules/stash/config.yml`
- Create: `src/main/java/fr/maxlego08/essentials/module/modules/StashModule.java`
- Modify: `src/main/java/fr/maxlego08/essentials/module/ZModuleManager.java`

- [ ] **Step 1: Create config file**

```yaml
########################################################################################################################
#
# zEssentials - Stash System
# Personal storage inspired by Hypixel SkyBlock: Item Stash for non-stackables,
# Material Stash for unlimited stackables.
#
########################################################################################################################

config-version: 1
enable: true

max-item-pages: 16

title: "&d&lItem Stash &8(&f%page%&8/&f%total%&8)"

nav-row:
  filler-material: GRAY_STAINED_GLASS_PANE
  filler-color: "&8"
  prev-button: ARROW
  prev-text: "&7« Previous Page"
  next-button: ARROW
  next-text: "&7Next Page »"
  close-button: BARRIER
  close-text: "&cClose"

material-title: "&e&lMaterial Stash"
material-filler-material: BLACK_STAINED_GLASS_PANE
material-close-material: BARRIER

picker-title: "&6&lStash"
picker-item-stash-icon: DIAMOND_SWORD
picker-item-stash-name: "&d&lItem Stash"
picker-item-stash-lore:
  - "&7Store weapons, tools,"
  - "&7and rare drops."
picker-material-stash-icon: COBBLESTONE
picker-material-stash-name: "&e&lMaterial Stash"
picker-material-stash-lore:
  - "&7Store cobblestone, crops,"
  - "&7and other stackables."

migrate-from-vanilla-inventory: true
```

- [ ] **Step 2: Create StashModule**

```java
package fr.maxlego08.essentials.module.modules;

import dev.yanianz.essentials.stash.ItemStashData;
import dev.yanianz.essentials.stash.MaterialStashData;
import fr.maxlego08.essentials.ZEssentialsPlugin;
import fr.maxlego08.essentials.api.configuration.NonLoadable;
import fr.maxlego08.essentials.api.commands.Permission;
import fr.maxlego08.essentials.module.ZModule;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StashModule extends ZModule {

    private boolean enabled = true;
    private int maxItemPages = 16;
    private String title = "&d&lItem Stash &8(&f%page%&8/&f%total%&8)";
    private String materialTitle = "&e&lMaterial Stash";
    private String pickerTitle = "&6&lStash";
    private boolean migrateFromVanilla = true;

    @NonLoadable
    private final Map<UUID, ItemStashData> itemDataCache = new HashMap<>();
    @NonLoadable
    private final Map<UUID, MaterialStashData> materialDataCache = new HashMap<>();

    public StashModule(ZEssentialsPlugin plugin) {
        super(plugin, "stash");
    }

    @Override
    public void loadConfiguration() {
        super.loadConfiguration();
        YamlConfiguration config = getConfiguration();
        this.enabled = config.getBoolean("enable", true);
        this.maxItemPages = Math.max(1, config.getInt("max-item-pages", 16));
        this.title = config.getString("title", "&d&lItem Stash &8(&f%page%&8/&f%total%&8)");
        this.materialTitle = config.getString("material-title", "&e&lMaterial Stash");
        this.pickerTitle = config.getString("picker-title", "&6&lStash");
        this.migrateFromVanilla = config.getBoolean("migrate-from-vanilla-inventory", true);
    }

    public int getAllowedItemPages(Player player) {
        int best = 1;
        for (int n = maxItemPages; n >= 1; n--) {
            if (player.hasPermission(Permission.ESSENTIALS_STASH.asPermission(".item.pages." + n))) {
                best = Math.min(n, maxItemPages);
                break;
            }
        }
        return best;
    }

    public void openCategoryPicker(Player player) {
        if (!isEnabled()) return;
        dev.yanianz.essentials.stash.StashPickerGui.open(this.plugin, player, this);
    }

    public void openItemStash(Player player) {
        if (!isEnabled()) return;
        int allowed = getAllowedItemPages(player);
        ItemStashData data = getItemData(player.getUniqueId());
        if (data.getPages() < allowed) data.resize(allowed);
        int visiblePages = Math.min(data.getPages(), allowed);
        dev.yanianz.essentials.stash.ItemStashGui.open(this.plugin, player, data, visiblePages, 0, false);
    }

    public void openMaterialStash(Player player) {
        if (!isEnabled()) return;
        MaterialStashData data = getMaterialData(player.getUniqueId());
        dev.yanianz.essentials.stash.MaterialStashGui.open(this.plugin, player, data, false);
    }

    public ItemStashData getItemData(UUID playerId) {
        return itemDataCache.computeIfAbsent(playerId, id -> {
            File file = getItemFile(id);
            if (file.exists()) {
                ItemStashData loaded = dev.yanianz.essentials.stash.ItemStashSerializer.deserialize(
                        readFile(file), id);
                if (loaded != null) return loaded;
            }
            ItemStashData data = new ItemStashData(id, maxItemPages);
            if (migrateFromVanilla) {
                Player online = org.bukkit.Bukkit.getPlayer(id);
                if (online != null) migrateFromInventory(data, online);
            }
            return data;
        });
    }

    private void migrateFromInventory(ItemStashData data, Player player) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            org.bukkit.inventory.ItemStack item = player.getInventory().getItem(i);
            if (item == null || item.getType().isAir()) continue;
            if (item.getMaxStackSize() > 1) continue;
            int slot = data.addToFirstAvailable(item);
            if (slot < 0) break;
            player.getInventory().setItem(i, null);
        }
    }

    public void saveItemData(UUID playerId) {
        ItemStashData data = itemDataCache.get(playerId);
        if (data == null) return;
        writeFile(getItemFile(playerId), dev.yanianz.essentials.stash.ItemStashSerializer.serialize(data));
    }

    public MaterialStashData getMaterialData(UUID playerId) {
        return materialDataCache.computeIfAbsent(playerId, id -> {
            File file = getMaterialFile(id);
            if (file.exists()) {
                MaterialStashData loaded = dev.yanianz.essentials.stash.MaterialStashSerializer.deserialize(
                        readFile(file), id);
                if (loaded != null) return loaded;
            }
            return new MaterialStashData(id);
        });
    }

    public void saveMaterialData(UUID playerId) {
        MaterialStashData data = materialDataCache.get(playerId);
        if (data == null) return;
        writeFile(getMaterialFile(playerId), dev.yanianz.essentials.stash.MaterialStashSerializer.serialize(data));
    }

    private File getItemFile(UUID playerId) {
        File dir = new File(getFolder(), "data/items");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, playerId + ".json");
    }

    private File getMaterialFile(UUID playerId) {
        File dir = new File(getFolder(), "data/materials");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, playerId + ".json");
    }

    private String readFile(File file) {
        try { return new String(java.nio.file.Files.readAllBytes(file.toPath())); }
        catch (Exception e) { return ""; }
    }

    private void writeFile(File file, String content) {
        try { java.nio.file.Files.writeString(file.toPath(), content); }
        catch (Exception e) { e.printStackTrace(); }
    }

    public boolean isEnabled() { return enabled && isEnable; }
    public int getMaxItemPages() { return maxItemPages; }
    public String getTitle() { return title; }
    public String getMaterialTitle() { return materialTitle; }
    public String getPickerTitle() { return pickerTitle; }
    public boolean isMigrateFromVanilla() { return migrateFromVanilla; }
}
```

- [ ] **Step 3: Register in ZModuleManager**

Add before `this.loadConfigurations()`:
```java
        this.modules.put(StashModule.class, new StashModule(this.plugin));
```
Add import: `import fr.maxlego08.essentials.module.modules.StashModule;`

- [ ] **Step 4: Add permission**

In `Permission.java`, add after `ESSENTIALS_STASH` (or wherever it makes sense — find the right location):
```java
    ESSENTIALS_STASH,
```

- [ ] **Step 5: Build + commit**

```bash
./gradlew build -x test --console=plain
git add -A && git commit -m "feat(stash): module scaffold + config + registration"
```

---

### Task 2: Data Classes + Serializers + Tests

**Files:**
- Create: `src/main/java/dev/yanianz/essentials/stash/ItemStashData.java`
- Create: `src/main/java/dev/yanianz/essentials/stash/MaterialStashData.java`
- Create: `src/main/java/dev/yanianz/essentials/stash/ItemStashSerializer.java`
- Create: `src/main/java/dev/yanianz/essentials/stash/MaterialStashSerializer.java`
- Test: `src/test/java/dev/yanianz/essentials/stash/ItemStashDataTest.java`
- Test: `src/test/java/dev/yanianz/essentials/stash/MaterialStashDataTest.java`

- [ ] **Step 1: Create ItemStashData**

```java
package dev.yanianz.essentials.stash;

import dev.yanianz.essentials.enderchest.EnderChestSlotMap;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ItemStashData {

    private final UUID playerId;
    private ItemStack[] contents;
    private int pages;

    public ItemStashData(UUID playerId, int pages) {
        this.playerId = playerId;
        this.pages = Math.max(1, pages);
        this.contents = new ItemStack[EnderChestSlotMap.totalSize(this.pages)];
    }

    public UUID getPlayerId() { return playerId; }
    public int getPages() { return pages; }
    public ItemStack[] rawContents() { return contents; }
    public void setRawContents(ItemStack[] contents, int pages) {
        this.contents = contents;
        this.pages = pages;
    }

    public ItemStack getContent(int page, int slot) {
        checkBounds(page, slot);
        return contents[EnderChestSlotMap.toFlatIndex(page, slot)];
    }

    public void setContent(int page, int slot, ItemStack item) {
        checkBounds(page, slot);
        contents[EnderChestSlotMap.toFlatIndex(page, slot)] = item;
    }

    public int addToFirstAvailable(ItemStack item) {
        if (item == null || item.getType().isAir()) return -1;
        for (int page = 0; page < pages; page++) {
            for (int slot = 0; slot < EnderChestSlotMap.CONTENT_SLOTS; slot++) {
                if (contents[EnderChestSlotMap.toFlatIndex(page, slot)] == null) {
                    contents[EnderChestSlotMap.toFlatIndex(page, slot)] = item;
                    return slot;
                }
            }
        }
        return -1;
    }

    public List<ItemStack> getPageContents(int page) {
        if (page < 0 || page >= pages) throw new IndexOutOfBoundsException("page " + page);
        List<ItemStack> result = new ArrayList<>(EnderChestSlotMap.CONTENT_SLOTS);
        for (int slot = 0; slot < EnderChestSlotMap.CONTENT_SLOTS; slot++) {
            result.add(contents[EnderChestSlotMap.toFlatIndex(page, slot)]);
        }
        return result;
    }

    public void setPageContents(int page, List<ItemStack> items) {
        if (page < 0 || page >= pages) throw new IndexOutOfBoundsException("page " + page);
        for (int slot = 0; slot < EnderChestSlotMap.CONTENT_SLOTS; slot++) {
            contents[EnderChestSlotMap.toFlatIndex(page, slot)] =
                    slot < items.size() ? items.get(slot) : null;
        }
    }

    public void resize(int newPages) {
        newPages = Math.max(1, newPages);
        ItemStack[] resized = new ItemStack[EnderChestSlotMap.totalSize(newPages)];
        int copyLen = Math.min(contents.length, resized.length);
        System.arraycopy(contents, 0, resized, 0, copyLen);
        this.contents = resized;
        this.pages = newPages;
    }

    private void checkBounds(int page, int slot) {
        if (page < 0 || page >= pages) throw new IndexOutOfBoundsException("page " + page);
        if (slot < 0 || slot >= EnderChestSlotMap.CONTENT_SLOTS)
            throw new IndexOutOfBoundsException("slot " + slot);
    }
}
```

- [ ] **Step 2: Create MaterialStashData**

```java
package dev.yanianz.essentials.stash;

import org.bukkit.Material;

import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

public class MaterialStashData {

    private final UUID playerId;
    private final Map<Material, Long> quantities;

    public MaterialStashData(UUID playerId) {
        this.playerId = playerId;
        this.quantities = new TreeMap<>();
    }

    public UUID getPlayerId() { return playerId; }
    public Map<Material, Long> getQuantities() { return quantities; }

    public long get(Material material) {
        return quantities.getOrDefault(material, 0L);
    }

    public void set(Material material, long amount) {
        if (amount <= 0) {
            quantities.remove(material);
        } else {
            quantities.put(material, amount);
        }
    }

    public void add(Material material, long amount) {
        if (amount <= 0 || material == null) return;
        quantities.merge(material, (long) amount, Long::sum);
    }

    public boolean remove(Material material, long amount) {
        if (amount <= 0) return true;
        long current = get(material);
        if (current < amount) return false;
        set(material, current - amount);
        return true;
    }

    public long totalItems() {
        return quantities.values().stream().mapToLong(Long::longValue).sum();
    }
}
```

- [ ] **Step 3: Create ItemStashSerializer**

```java
package dev.yanianz.essentials.stash;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.yanianz.essentials.enderchest.EnderChestSlotMap;
import org.bukkit.inventory.ItemStack;

import java.util.Base64;
import java.util.UUID;

public final class ItemStashSerializer {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private ItemStashSerializer() {}

    public static String serialize(ItemStashData data) {
        SerializedData raw = new SerializedData();
        raw.player_uuid = data.getPlayerId().toString();
        raw.pages = data.getPages();
        ItemStack[] contents = data.rawContents();
        raw.contents = new String[contents.length];
        for (int i = 0; i < contents.length; i++) {
            raw.contents[i] = contents[i] == null ? null
                    : Base64.getEncoder().encodeToString(contents[i].serializeAsBytes());
        }
        return GSON.toJson(raw);
    }

    public static ItemStashData deserialize(String json, UUID playerId) {
        SerializedData raw = GSON.fromJson(json, SerializedData.class);
        if (raw == null) return null;
        int pages = Math.max(1, raw.pages);
        ItemStack[] contents = new ItemStack[EnderChestSlotMap.totalSize(pages)];
        if (raw.contents != null) {
            for (int i = 0; i < Math.min(raw.contents.length, contents.length); i++) {
                if (raw.contents[i] != null) {
                    contents[i] = ItemStack.deserializeBytes(
                            Base64.getDecoder().decode(raw.contents[i]));
                }
            }
        }
        ItemStashData data = new ItemStashData(playerId, pages);
        data.setRawContents(contents, pages);
        return data;
    }

    private static final class SerializedData {
        String player_uuid;
        int pages;
        String[] contents;
    }
}
```

- [ ] **Step 4: Create MaterialStashSerializer**

```java
package dev.yanianz.essentials.stash;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.Material;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class MaterialStashSerializer {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private MaterialStashSerializer() {}

    public static String serialize(MaterialStashData data) {
        SerializedData raw = new SerializedData();
        raw.player_uuid = data.getPlayerId().toString();
        raw.quantities = new LinkedHashMap<>();
        for (Map.Entry<Material, Long> entry : data.getQuantities().entrySet()) {
            raw.quantities.put(entry.getKey().name(), entry.getValue());
        }
        return GSON.toJson(raw);
    }

    public static MaterialStashData deserialize(String json, UUID playerId) {
        SerializedData raw = GSON.fromJson(json, SerializedData.class);
        if (raw == null) return null;
        MaterialStashData data = new MaterialStashData(playerId);
        if (raw.quantities != null) {
            for (Map.Entry<String, Long> entry : raw.quantities.entrySet()) {
                try {
                    data.set(Material.valueOf(entry.getKey()), entry.getValue());
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        return data;
    }

    private static final class SerializedData {
        String player_uuid;
        Map<String, Long> quantities;
    }
}
```

- [ ] **Step 5: Write ItemStashData tests**

```java
package dev.yanianz.essentials.stash;

import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ItemStashDataTest {

    private final UUID playerId = UUID.randomUUID();

    @Test
    @DisplayName("New data has correct page count and all-null contents")
    void testNewData() {
        ItemStashData data = new ItemStashData(playerId, 3);
        assertEquals(3, data.getPages());
        assertEquals(playerId, data.getPlayerId());
        for (int page = 0; page < 3; page++) {
            for (int slot = 0; slot < 45; slot++) {
                assertNull(data.getContent(page, slot));
            }
        }
    }

    @Test
    @DisplayName("Set and get content on page 0 slot 0")
    void testSetGet() {
        ItemStashData data = new ItemStashData(playerId, 1);
        ItemStack item = mock(ItemStack.class);
        data.setContent(0, 0, item);
        assertSame(item, data.getContent(0, 0));
    }

    @Test
    @DisplayName("addToFirstAvailable finds the first empty slot")
    void testAddToFirstAvailable() {
        ItemStashData data = new ItemStashData(playerId, 2);
        ItemStack item1 = mock(ItemStack.class);
        ItemStack item2 = mock(ItemStack.class);
        assertEquals(0, data.addToFirstAvailable(item1));
        assertEquals(1, data.addToFirstAvailable(item2));
        assertSame(item1, data.getContent(0, 0));
        assertSame(item2, data.getContent(0, 1));
    }

    @Test
    @DisplayName("Resize grow adds null slots, preserves items")
    void testResizeGrow() {
        ItemStashData data = new ItemStashData(playerId, 1);
        ItemStack item = mock(ItemStack.class);
        data.setContent(0, 0, item);
        data.resize(3);
        assertEquals(3, data.getPages());
        assertSame(item, data.getContent(0, 0));
        assertNull(data.getContent(1, 0));
    }

    @Test
    @DisplayName("Out of bounds throws IndexOutOfBoundsException")
    void testOutOfBounds() {
        ItemStashData data = new ItemStashData(playerId, 1);
        assertThrows(IndexOutOfBoundsException.class, () -> data.getContent(5, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> data.getContent(0, 45));
    }
}
```

- [ ] **Step 6: Write MaterialStashData tests**

```java
package dev.yanianz.essentials.stash;

import org.bukkit.Material;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MaterialStashDataTest {

    private final UUID playerId = UUID.randomUUID();

    @Test
    @DisplayName("New data is empty")
    void testNewData() {
        MaterialStashData data = new MaterialStashData(playerId);
        assertEquals(0, data.get(Material.COBBLESTONE));
        assertEquals(0, data.totalItems());
    }

    @Test
    @DisplayName("Add and get material quantity")
    void testAddGet() {
        MaterialStashData data = new MaterialStashData(playerId);
        data.add(Material.COBBLESTONE, 64);
        data.add(Material.COBBLESTONE, 64);
        assertEquals(128, data.get(Material.COBBLESTONE));
    }

    @Test
    @DisplayName("Remove decrements quantity")
    void testRemove() {
        MaterialStashData data = new MaterialStashData(playerId);
        data.add(Material.DIRT, 100);
        assertTrue(data.remove(Material.DIRT, 30));
        assertEquals(70, data.get(Material.DIRT));
    }

    @Test
    @DisplayName("Remove insufficient returns false")
    void testRemoveInsufficient() {
        MaterialStashData data = new MaterialStashData(playerId);
        data.add(Material.DIRT, 10);
        assertFalse(data.remove(Material.DIRT, 20));
        assertEquals(10, data.get(Material.DIRT));
    }

    @Test
    @DisplayName("Set to zero removes material")
    void testSetZero() {
        MaterialStashData data = new MaterialStashData(playerId);
        data.set(Material.STONE, 50);
        assertEquals(50, data.get(Material.STONE));
        data.set(Material.STONE, 0);
        assertEquals(0, data.get(Material.STONE));
        assertFalse(data.getQuantities().containsKey(Material.STONE));
    }

    @Test
    @DisplayName("Total items sums all quantities")
    void testTotalItems() {
        MaterialStashData data = new MaterialStashData(playerId);
        data.add(Material.COBBLESTONE, 1000);
        data.add(Material.DIRT, 500);
        data.add(Material.WHEAT, 100);
        assertEquals(1600, data.totalItems());
    }
}
```

- [ ] **Step 7: Build + test + commit**

```bash
./gradlew build --console=plain --no-daemon
git add -A && git commit -m "feat(stash): data classes + JSON serializers + 11 tests"
```

---

### Task 3: Item Stash GUI System (Holder + Gui + Listener)

**Files:**
- Create: `src/main/java/dev/yanianz/essentials/stash/ItemStashHolder.java`
- Create: `src/main/java/dev/yanianz/essentials/stash/ItemStashGui.java`
- Create: `src/main/java/dev/yanianz/essentials/stash/ItemStashListener.java`

- [ ] **Step 1: Create ItemStashHolder**

```java
package dev.yanianz.essentials.stash;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public class ItemStashHolder implements InventoryHolder {

    private final UUID playerId;
    private final ItemStashData data;
    private final int pages;
    private final boolean readOnly;
    private int currentPage;
    private Inventory inventory;

    public ItemStashHolder(UUID playerId, ItemStashData data, int currentPage, int pages, boolean readOnly) {
        this.playerId = playerId;
        this.data = data;
        this.currentPage = currentPage;
        this.pages = pages;
        this.readOnly = readOnly;
    }

    public UUID getPlayerId() { return playerId; }
    public ItemStashData getData() { return data; }
    public int getCurrentPage() { return currentPage; }
    public void setCurrentPage(int page) { this.currentPage = page; }
    public int getPages() { return pages; }
    public boolean isReadOnly() { return readOnly; }
    void setInventory(Inventory inventory) { this.inventory = inventory; }

    @Override
    public Inventory getInventory() { return inventory; }
}
```

- [ ] **Step 2: Create ItemStashGui**

```java
package dev.yanianz.essentials.stash;

import dev.yanianz.essentials.enderchest.EnderChestSlotMap;
import dev.yanianz.essentials.util.ColorUtil;
import fr.maxlego08.essentials.ZEssentialsPlugin;
import fr.maxlego08.essentials.module.modules.StashModule;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class ItemStashGui {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private ItemStashGui() {}

    public static ItemStashHolder open(ZEssentialsPlugin plugin, Player player,
                                       ItemStashData data, int pages, int startPage, boolean readOnly) {
        StashModule module = plugin.getModuleManager().getModule(StashModule.class);
        int currentPage = Math.max(0, Math.min(startPage, pages - 1));

        ItemStashHolder holder = new ItemStashHolder(data.getPlayerId(), data, currentPage, pages, readOnly);
        String title = module.getTitle()
                .replace("%page%", String.valueOf(currentPage + 1))
                .replace("%total%", String.valueOf(pages));
        Inventory inventory = Bukkit.createInventory(holder, 54,
                LEGACY.deserialize(ColorUtil.sections(title)));
        holder.setInventory(inventory);

        fillContent(inventory, data, currentPage);
        fillNavRow(inventory, module, currentPage, pages, holder);

        player.openInventory(inventory);
        return holder;
    }

    static void fillContent(Inventory inventory, ItemStashData data, int page) {
        List<ItemStack> contents = data.getPageContents(page);
        for (int slot = 0; slot < EnderChestSlotMap.CONTENT_SLOTS; slot++) {
            ItemStack item = contents.get(slot);
            if (item != null) inventory.setItem(slot, item);
        }
    }

    static void fillNavRow(Inventory inventory, StashModule module, int currentPage, int pages, ItemStashHolder holder) {
        ConfigurationSection nav = getNavSection(module);
        String fillerMat = nav.getString("filler-material", "GRAY_STAINED_GLASS_PANE");
        String fillerColor = nav.getString("filler-color", "&8");
        ItemStack filler = namedItem(parseMaterial(fillerMat, Material.GRAY_STAINED_GLASS_PANE), fillerColor + " ");

        for (int slot = EnderChestSlotMap.NAV_ROW_START; slot < 54; slot++) {
            inventory.setItem(slot, filler);
        }

        if (currentPage > 0) {
            inventory.setItem(EnderChestSlotMap.SLOT_PREV,
                    namedItem(parseMaterial(nav.getString("prev-button", "ARROW"), Material.ARROW),
                            nav.getString("prev-text", "&7« Previous Page")));
        }
        inventory.setItem(EnderChestSlotMap.SLOT_CLOSE,
                namedItem(parseMaterial(nav.getString("close-button", "BARRIER"), Material.BARRIER),
                        nav.getString("close-text", "&cClose")));
        if (currentPage < pages - 1) {
            inventory.setItem(EnderChestSlotMap.SLOT_NEXT,
                    namedItem(parseMaterial(nav.getString("next-button", "ARROW"), Material.ARROW),
                            nav.getString("next-text", "&7Next Page »")));
        }
    }

    static void switchPage(ZEssentialsPlugin plugin, Player player, ItemStashHolder holder, int newPage) {
        if (newPage < 0 || newPage >= holder.getPages()) return;
        holder.setCurrentPage(newPage);
        StashModule module = plugin.getModuleManager().getModule(StashModule.class);
        Inventory inventory = holder.getInventory();
        inventory.clear();
        fillContent(inventory, holder.getData(), newPage);
        fillNavRow(inventory, module, newPage, holder.getPages(), holder);
    }

    static ItemStack namedItem(Material material, String nameLegacy) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(LEGACY.deserialize(ColorUtil.sections(nameLegacy)));
            item.setItemMeta(meta);
        }
        return item;
    }

    static Material parseMaterial(String name, Material fallback) {
        try { return Material.valueOf(name.toUpperCase()); }
        catch (Exception e) { return fallback; }
    }

    private static ConfigurationSection getNavSection(StashModule module) {
        YamlConfigAccessor accessor = new YamlConfigAccessor(module);
        return accessor.getConfigurationSection("nav-row");
    }

    private static class YamlConfigAccessor {
        private final StashModule module;
        YamlConfigAccessor(StashModule module) { this.module = module; }
        org.bukkit.configuration.ConfigurationSection getConfigurationSection(String path) {
            return module.getConfiguration().getConfigurationSection(path);
        }
    }
}
```

- [ ] **Step 3: Create ItemStashListener**

```java
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
```

- [ ] **Step 4: Build + commit**

```bash
./gradlew build -x test --console=plain
git add -A && git commit -m "feat(stash): Item Stash holder + GUI + listener"
```

---

### Task 4: Material Stash GUI System + Category Picker

**Files:**
- Create: `src/main/java/dev/yanianz/essentials/stash/MaterialStashHolder.java`
- Create: `src/main/java/dev/yanianz/essentials/stash/MaterialStashGui.java`
- Create: `src/main/java/dev/yanianz/essentials/stash/MaterialStashListener.java`
- Create: `src/main/java/dev/yanianz/essentials/stash/StashPickerGui.java`

- [ ] **Step 1: Create MaterialStashHolder**

```java
package dev.yanianz.essentials.stash;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public class MaterialStashHolder implements InventoryHolder {

    private final UUID playerId;
    private final MaterialStashData data;
    private final boolean readOnly;
    private Inventory inventory;

    public MaterialStashHolder(UUID playerId, MaterialStashData data, boolean readOnly) {
        this.playerId = playerId;
        this.data = data;
        this.readOnly = readOnly;
    }

    public UUID getPlayerId() { return playerId; }
    public MaterialStashData getData() { return data; }
    public boolean isReadOnly() { return readOnly; }
    void setInventory(Inventory inventory) { this.inventory = inventory; }

    @Override
    public Inventory getInventory() { return inventory; }
}
```

- [ ] **Step 2: Create MaterialStashGui**

```java
package dev.yanianz.essentials.stash;

import dev.yanianz.essentials.util.ColorUtil;
import fr.maxlego08.essentials.ZEssentialsPlugin;
import fr.maxlego08.essentials.module.modules.StashModule;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class MaterialStashGui {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();
    private static final int MAX_SLOTS = 45;

    private MaterialStashGui() {}

    public static MaterialStashHolder open(ZEssentialsPlugin plugin, Player player,
                                          MaterialStashData data, boolean readOnly) {
        StashModule module = plugin.getModuleManager().getModule(StashModule.class);
        MaterialStashHolder holder = new MaterialStashHolder(data.getPlayerId(), data, readOnly);

        Inventory inventory = Bukkit.createInventory(holder, 54,
                LEGACY.deserialize(ColorUtil.sections(module.getMaterialTitle())));
        holder.setInventory(inventory);

        fillMaterials(inventory, data, module);
        player.openInventory(inventory);
        return holder;
    }

    static void fillMaterials(Inventory inventory, MaterialStashData data, StashModule module) {
        String fillerMat = module.getConfiguration().getString("material-filler-material", "BLACK_STAINED_GLASS_PANE");
        ItemStack filler = namedItem(parseMaterial(fillerMat, Material.BLACK_STAINED_GLASS_PANE), "&8 ");
        for (int i = 0; i < 54; i++) inventory.setItem(i, filler);

        int slot = 0;
        List<Map.Entry<Material, Long>> entries = new ArrayList<>(data.getQuantities().entrySet());
        for (Map.Entry<Material, Long> entry : entries) {
            if (slot >= MAX_SLOTS) break;
            Material material = entry.getKey();
            long qty = entry.getValue();
            int maxStack = material.getMaxStackSize();
            ItemStack display = new ItemStack(material, Math.min((int) Math.min(qty, maxStack), maxStack));
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                meta.displayName(LEGACY.deserialize(ColorUtil.sections("&e" + niceName(material))));
                meta.lore(List.of(
                        LEGACY.deserialize(ColorUtil.sections("&7Total: &f" + formatNumber(qty))),
                        LEGACY.deserialize(ColorUtil.sections("&7Stack size: &f" + maxStack)),
                        LEGACY.deserialize(ColorUtil.sections("&aLeft-click: withdraw 1 stack")),
                        LEGACY.deserialize(ColorUtil.sections("&aRight-click: withdraw half")),
                        LEGACY.deserialize(ColorUtil.sections("&aShift-click: withdraw all"))
                ));
                display.setItemMeta(meta);
            }
            inventory.setItem(slot++, display);
        }

        String closeMat = module.getConfiguration().getString("material-close-material", "BARRIER");
        inventory.setItem(49, namedItem(parseMaterial(closeMat, Material.BARRIER), "&cClose"));
    }

    private static String niceName(Material material) {
        String name = material.name().toLowerCase().replace('_', ' ');
        StringBuilder sb = new StringBuilder();
        for (String word : name.split(" ")) {
            if (!word.isEmpty()) sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }

    private static String formatNumber(long n) {
        if (n >= 1_000_000L) return String.format(java.util.Locale.US, "%.1fM", n / 1_000_000.0);
        if (n >= 1_000L) return String.format(java.util.Locale.US, "%.1fK", n / 1_000.0);
        return String.valueOf(n);
    }

    static ItemStack namedItem(Material material, String nameLegacy) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(LEGACY.deserialize(ColorUtil.sections(nameLegacy)));
            item.setItemMeta(meta);
        }
        return item;
    }

    static Material parseMaterial(String name, Material fallback) {
        try { return Material.valueOf(name.toUpperCase()); }
        catch (Exception e) { return fallback; }
    }
}
```

- [ ] **Step 3: Create MaterialStashListener**

```java
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

import java.util.Map;

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
```

- [ ] **Step 4: Create StashPickerGui**

```java
package dev.yanianz.essentials.stash;

import dev.yanianz.essentials.util.ColorUtil;
import fr.maxlego08.essentials.ZEssentialsPlugin;
import fr.maxlego08.essentials.module.modules.StashModule;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class StashPickerGui {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private StashPickerGui() {}

    public static void open(ZEssentialsPlugin plugin, Player player, StashModule module) {
        Inventory inventory = Bukkit.createInventory(null, 27,
                LEGACY.deserialize(ColorUtil.sections(module.getPickerTitle())));

        ItemStack filler = namedItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) inventory.setItem(i, filler);

        ConfigurationSection itemCfg = module.getConfiguration().getConfigurationSection("picker-item-stash");
        if (itemCfg != null) {
            inventory.setItem(11, namedItem(
                    parseMaterial(itemCfg.getString("icon", "DIAMOND_SWORD"), Material.DIAMOND_SWORD),
                    itemCfg.getString("name", "&d&lItem Stash"),
                    itemCfg.getStringList("lore")));
        }

        ConfigurationSection matCfg = module.getConfiguration().getConfigurationSection("picker-material-stash");
        if (matCfg != null) {
            inventory.setItem(15, namedItem(
                    parseMaterial(matCfg.getString("icon", "COBBLESTONE"), Material.COBBLESTONE),
                    matCfg.getString("name", "&e&lMaterial Stash"),
                    matCfg.getStringList("lore")));
        }

        player.openInventory(inventory);

        Bukkit.getScheduler().runTask(plugin, () -> {
            StashPickerListener.register(plugin, player, module);
        });
    }

    static ItemStack namedItem(Material material, String nameLegacy) {
        return namedItem(material, nameLegacy, List.of());
    }

    static ItemStack namedItem(Material material, String nameLegacy, List<String> loreLegacy) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(LEGACY.deserialize(ColorUtil.sections(nameLegacy)));
            if (loreLegacy != null && !loreLegacy.isEmpty()) {
                meta.lore(loreLegacy.stream().map(l -> LEGACY.deserialize(ColorUtil.sections(l))).toList());
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    static Material parseMaterial(String name, Material fallback) {
        try { return Material.valueOf(name.toUpperCase()); }
        catch (Exception e) { return fallback; }
    }
}
```

- [ ] **Step 5: Create StashPickerListener**

```java
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
```

- [ ] **Step 6: Build + commit**

```bash
./gradlew build -x test --console=plain
git add -A && git commit -m "feat(stash): Material Stash GUI + category picker"
```

---

### Task 5: CommandStash + Messages + Changelog

**Files:**
- Create: `src/main/java/dev/yanianz/essentials/stash/CommandStash.java`
- Modify: `src/main/java/fr/maxlego08/essentials/commands/CommandLoader.java`
- Modify: `API/src/main/java/fr/maxlego08/essentials/api/messages/Message.java`
- Modify: `src/main/resources/messages/messages.yml`
- Modify: `changelog.md`

- [ ] **Step 1: Create CommandStash**

```java
package dev.yanianz.essentials.stash;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.commands.CommandResultType;
import fr.maxlego08.essentials.api.commands.Permission;
import fr.maxlego08.essentials.api.messages.Message;
import fr.maxlego08.essentials.module.modules.StashModule;
import fr.maxlego08.essentials.zutils.utils.commands.VCommand;

public class CommandStash extends VCommand {

    public CommandStash(EssentialsPlugin plugin) {
        super(plugin);
        this.setModule(StashModule.class);
        this.setPermission(Permission.ESSENTIALS_STASH);
        this.setDescription(Message.DESCRIPTION_STASH);
        this.addOptionalArg("type", (sender, args) -> java.util.List.of("item", "material", "i", "m"));
        this.onlyPlayers();
    }

    @Override
    protected CommandResultType perform(EssentialsPlugin plugin) {
        StashModule module = plugin.getModuleManager().getModule(StashModule.class);
        if (module == null || !module.isEnabled()) return CommandResultType.SUCCESS;

        ItemStashListener.ensureRegistered((fr.maxlego08.essentials.ZEssentialsPlugin) plugin);
        MaterialStashListener.ensureRegistered((fr.maxlego08.essentials.ZEssentialsPlugin) plugin);

        String type = argAsString(0, "");
        switch (type.toLowerCase()) {
            case "item", "i" -> module.openItemStash(this.player);
            case "material", "m" -> module.openMaterialStash(this.player);
            default -> module.openCategoryPicker(this.player);
        }
        return CommandResultType.SUCCESS;
    }
}
```

- [ ] **Step 2: Register command**

In `CommandLoader.java`, add:
```java
        register("stash", CommandStash.class, "itemstash", "materialstash");
```
Add import: `import dev.yanianz.essentials.stash.CommandStash;`

- [ ] **Step 3: Add messages**

In `Message.java`:
```java
    DESCRIPTION_STASH("Open your personal stash"),
```

In `messages.yml`:
```yaml
description-stash: "Open your personal stash"
```

- [ ] **Step 4: Add changelog**

```markdown
## Stash system

- **Item Stash** — personal storage for up to 720 non-stackable items (weapons, tools, rare drops) across 16 paginated pages of 45 slots
- **Material Stash** — unlimited stackable material storage (cobblestone, crops); displays one slot per distinct material with total count
- **Permission-based pages** — `essentials.stash.item.pages.<n>` grants additional Item Stash pages (up to `max-item-pages`)
- **Category picker** — `/stash` opens a GUI to choose between Item Stash and Material Stash; `/stash item` and `/stash material` open directly
- **Vanilla migration** — first open migrates non-stackable items from player inventory into Item Stash (config-toggleable)
- **JSON persistence** — lossless `serializeAsBytes()` + Base64 in `modules/stash/data/{items,materials}/<uuid>.json`
- **Config-driven** — titles, nav row, picker icons, migration toggle in `modules/stash/config.yml`
```

- [ ] **Step 5: Build + test + commit**

```bash
./gradlew build --console=plain --no-daemon
git add -A && git commit -m "feat(stash): /stash command, messages, changelog"
```
