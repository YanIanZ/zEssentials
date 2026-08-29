# Enderchest Custom GUI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the vanilla enderchest with a Hypixel SkyBlock-style custom paginated GUI with a decorative bottom navigation bar and permission-based page count.

**Architecture:** Custom backing store (`ItemStack[]` per player, JSON-persisted) decouples content from the on-screen 54-slot inventory. Slots 0-44 are interactive content; slots 45-53 are a locked nav row (prev/close/next + glass panes). A listener syncs content slots back to the backing array on every click and saves on close. First open migrates vanilla enderchest contents then clears the vanilla chest to prevent duplication.

**Tech Stack:** Java 21, Bukkit/Paper API (compileOnly), Folia-safe scheduling via `plugin.getScheduler()`, Gson for JSON persistence, JUnit 5 for tests.

**Spec:** `docs/superpowers/specs/2026-08-30-enderchest-gui-design.md`

## Global Constraints

- Java 21 bytecode target. Paper API is `compileOnly` — tests cannot instantiate Bukkit objects (ItemStack, Inventory, Player). Test only pure-logic classes that have no Bukkit runtime dependency.
- Package convention: feature logic goes in `dev.yanianz.essentials.enderchest.*` (matching ReportsModule, SleepModule, etc.).
- Folia-supported: use `plugin.getScheduler()` for any async/region work, not raw `Bukkit.getScheduler()`.
- Config self-healing: `config-version: 1` key required in config.yml.
- Build command: `./gradlew build -x test --console=plain`
- Test command: `./gradlew test --console=plain --no-daemon`
- No comments in code unless explicitly requested.

---

## File Structure

| File | Responsibility |
|------|---------------|
| `src/main/java/dev/yanianz/essentials/enderchest/EnderChestSlotMap.java` | Pure-logic slot index math (page↔flat index). Testable without Bukkit. |
| `src/main/java/dev/yanianz/essentials/enderchest/EnderChestData.java` | Backing store: `ItemStack[]`, page get/set, JSON serialize/deserialize. |
| `src/main/java/dev/yanianz/essentials/enderchest/EnderChestHolder.java` | `InventoryHolder` carrying owner UUID, data ref, current page, page count. |
| `src/main/java/dev/yanianz/essentials/enderchest/EnderChestGui.java` | Builds the 54-slot inventory, fills content + nav row, handles page switching. |
| `src/main/java/dev/yanianz/essentials/enderchest/EnderChestListener.java` | `InventoryClickEvent` + `InventoryCloseEvent` + `InventoryDragEvent` handler. |
| `src/main/java/dev/yanianz/essentials/enderchest/EnderChestModule.java` | ZModule scaffold: config loading, data cache, permission-based page count, open/save entry points. |
| `src/main/resources/modules/enderchest/config.yml` | Module config with self-healing `config-version: 1`. |
| `API/.../Permission.java` | No new enum needed — uses `ESSENTIALS_ENDERCHEST.asPermission(".pages.2")` |
| `API/.../Message.java` | 3 new message entries. |
| `src/main/resources/messages/messages.yml` | 3 new message keys. |
| `src/main/java/fr/maxlego08/essentials/module/ZModuleManager.java` | Register `EnderChestModule`. |
| `src/main/java/fr/maxlego08/essentials/commands/CommandLoader.java` | No change — existing `/ec` alias stays. |
| `src/main/java/fr/maxlego08/essentials/commands/commands/enderchest/CommandEnderChest.java` | Delegate to `EnderChestModule.openEnderChest(player)`. |
| `src/main/java/fr/maxlego08/essentials/commands/commands/enderchest/CommandEnderSee.java` | Delegate to `EnderChestModule.openEnderChestFor(viewer, target)`. |
| `src/test/java/dev/yanianz/essentials/enderchest/EnderChestSlotMapTest.java` | Unit tests for slot mapping math. |
| `src/test/java/dev/yanianz/essentials/enderchest/EnderChestDataTest.java` | Unit tests for data get/set with mock ItemStacks. |

---

### Task 1: EnderChestSlotMap — pure slot math

**Files:**
- Create: `src/main/java/dev/yanianz/essentials/enderchest/EnderChestSlotMap.java`
- Test: `src/test/java/dev/yanianz/essentials/enderchest/EnderChestSlotMapTest.java`

**Interfaces:**
- Produces: `EnderChestSlotMap.CONTENT_SLOTS` (45), `EnderChestSlotMap.NAV_ROW_START` (45), `EnderChestSlotMap.toFlatIndex(int page, int slot)`, `EnderChestSlotMap.toPage(int flatIndex)`, `EnderChestSlotMap.toSlot(int flatIndex)`, `EnderChestSlotMap.totalSize(int pages)`

- [ ] **Step 1: Write the failing test**

```java
package dev.yanianz.essentials.enderchest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EnderChestSlotMapTest {

    @Test
    @DisplayName("Constants are correct")
    void testConstants() {
        assertEquals(45, EnderChestSlotMap.CONTENT_SLOTS);
        assertEquals(45, EnderChestSlotMap.NAV_ROW_START);
        assertEquals(54, EnderChestSlotMap.INVENTORY_SIZE);
    }

    @Test
    @DisplayName("Page 0 slot 0 maps to flat index 0")
    void testPage0Slot0() {
        assertEquals(0, EnderChestSlotMap.toFlatIndex(0, 0));
    }

    @Test
    @DisplayName("Page 0 slot 44 maps to flat index 44")
    void testPage0Slot44() {
        assertEquals(44, EnderChestSlotMap.toFlatIndex(0, 44));
    }

    @Test
    @DisplayName("Page 1 slot 0 maps to flat index 45")
    void testPage1Slot0() {
        assertEquals(45, EnderChestSlotMap.toFlatIndex(1, 0));
    }

    @Test
    @DisplayName("Page 2 slot 44 maps to flat index 134")
    void testPage2Slot44() {
        assertEquals(134, EnderChestSlotMap.toFlatIndex(2, 44));
    }

    @Test
    @DisplayName("Reverse: flat index 0 → page 0, slot 0")
    void testReverse0() {
        assertEquals(0, EnderChestSlotMap.toPage(0));
        assertEquals(0, EnderChestSlotMap.toSlot(0));
    }

    @Test
    @DisplayName("Reverse: flat index 45 → page 1, slot 0")
    void testReverse45() {
        assertEquals(1, EnderChestSlotMap.toPage(45));
        assertEquals(0, EnderChestSlotMap.toSlot(45));
    }

    @Test
    @DisplayName("Reverse: flat index 134 → page 2, slot 44")
    void testReverse134() {
        assertEquals(2, EnderChestSlotMap.toPage(134));
        assertEquals(44, EnderChestSlotMap.toSlot(134));
    }

    @Test
    @DisplayName("totalSize for 1 page = 45, 2 pages = 90, 3 pages = 135")
    void testTotalSize() {
        assertEquals(45, EnderChestSlotMap.totalSize(1));
        assertEquals(90, EnderChestSlotMap.totalSize(2));
        assertEquals(135, EnderChestSlotMap.totalSize(3));
    }

    @Test
    @DisplayName("isContentSlot returns true for 0-44, false for 45-53")
    void testIsContentSlot() {
        for (int i = 0; i < 45; i++) assertTrue(EnderChestSlotMap.isContentSlot(i));
        for (int i = 45; i < 54; i++) assertFalse(EnderChestSlotMap.isContentSlot(i));
    }

    @Test
    @DisplayName("isNavSlot returns false for 0-44, true for 45-53")
    void testIsNavSlot() {
        for (int i = 0; i < 45; i++) assertFalse(EnderChestSlotMap.isNavSlot(i));
        for (int i = 45; i < 54; i++) assertTrue(EnderChestSlotMap.isNavSlot(i));
    }

    @Test
    @DisplayName("Nav button slots: prev=45, close=49, next=53")
    void testNavButtonSlots() {
        assertEquals(45, EnderChestSlotMap.SLOT_PREV);
        assertEquals(49, EnderChestSlotMap.SLOT_CLOSE);
        assertEquals(53, EnderChestSlotMap.SLOT_NEXT);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :test --tests "dev.yanianz.essentials.enderchest.EnderChestSlotMapTest" --console=plain --no-daemon`
Expected: FAIL — class not found

- [ ] **Step 3: Write minimal implementation**

```java
package dev.yanianz.essentials.enderchest;

public final class EnderChestSlotMap {

    private EnderChestSlotMap() {}

    public static final int CONTENT_SLOTS = 45;
    public static final int NAV_ROW_START = 45;
    public static final int INVENTORY_SIZE = 54;
    public static final int SLOT_PREV = 45;
    public static final int SLOT_CLOSE = 49;
    public static final int SLOT_NEXT = 53;

    public static int toFlatIndex(int page, int slot) {
        return page * CONTENT_SLOTS + slot;
    }

    public static int toPage(int flatIndex) {
        return flatIndex / CONTENT_SLOTS;
    }

    public static int toSlot(int flatIndex) {
        return flatIndex % CONTENT_SLOTS;
    }

    public static int totalSize(int pages) {
        return pages * CONTENT_SLOTS;
    }

    public static boolean isContentSlot(int slot) {
        return slot >= 0 && slot < CONTENT_SLOTS;
    }

    public static boolean isNavSlot(int slot) {
        return slot >= NAV_ROW_START && slot < INVENTORY_SIZE;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :test --tests "dev.yanianz.essentials.enderchest.EnderChestSlotMapTest" --console=plain --no-daemon`
Expected: PASS — 12 tests

- [ ] **Step 5: Commit**

```bash
git add src/main/java/dev/yanianz/essentials/enderchest/EnderChestSlotMap.java src/test/java/dev/yanianz/essentials/enderchest/EnderChestSlotMapTest.java
git commit -m "feat(enderchest): slot mapping math with 12 tests"
```

---

### Task 2: EnderChestData — backing store

**Files:**
- Create: `src/main/java/dev/yanianz/essentials/enderchest/EnderChestData.java`
- Test: `src/test/java/dev/yanianz/essentials/enderchest/EnderChestDataTest.java`

**Interfaces:**
- Consumes: `EnderChestSlotMap.toFlatIndex(page, slot)`, `EnderChestSlotMap.totalSize(pages)`
- Produces: `EnderChestData(UUID, int pages)`, `getContent(int page, int slot)`, `setContent(int page, int slot, ItemStack)`, `getPageContents(int page)`, `setPageContents(int page, List<ItemStack>)`, `getPages()`, `getPlayerId()`, `resize(int newPages)`, `toJson(Gson)`, `fromJson(Gson, String, UUID)`

Note: `EnderChestData` uses `ItemStack[]` internally but all get/set methods take/return `ItemStack`. The test uses Mockito to mock `ItemStack` since Paper API is compileOnly and `ItemStack` is a concrete class that can't be instantiated in unit tests. Actually, `ItemStack` has a public no-arg constructor in the API but calling methods on it may fail without a server. Instead, the test uses `null` for empty slots and a Mockito-mocked `ItemStack` for non-empty slots — the data class only stores/retrieves references, it never calls methods on the ItemStack.

- [ ] **Step 1: Write the failing test**

```java
package dev.yanianz.essentials.enderchest;

import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class EnderChestDataTest {

    private final UUID playerId = UUID.randomUUID();

    @Test
    @DisplayName("New data has correct page count and all-null contents")
    void testNewData() {
        EnderChestData data = new EnderChestData(playerId, 3);
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
    void testSetGetPage0Slot0() {
        EnderChestData data = new EnderChestData(playerId, 1);
        ItemStack item = mock(ItemStack.class);
        data.setContent(0, 0, item);
        assertSame(item, data.getContent(0, 0));
    }

    @Test
    @DisplayName("Set and get content on page 2 slot 44")
    void testSetGetPage2Slot44() {
        EnderChestData data = new EnderChestData(playerId, 3);
        ItemStack item = mock(ItemStack.class);
        data.setContent(2, 44, item);
        assertSame(item, data.getContent(2, 44));
        assertNull(data.getContent(2, 43));
    }

    @Test
    @DisplayName("getPageContents returns 45 items for one page")
    void testGetPageContents() {
        EnderChestData data = new EnderChestData(playerId, 2);
        ItemStack item = mock(ItemStack.class);
        data.setContent(0, 0, item);
        data.setContent(0, 44, item);
        List<ItemStack> page = data.getPageContents(0);
        assertEquals(45, page.size());
        assertSame(item, page.get(0));
        assertSame(item, page.get(44));
        assertNull(page.get(1));
    }

    @Test
    @DisplayName("setPageContents replaces entire page")
    void testSetPageContents() {
        EnderChestData data = new EnderChestData(playerId, 1);
        ItemStack item = mock(ItemStack.class);
        ItemStack[] items = new ItemStack[45];
        items[0] = item;
        items[44] = item;
        data.setPageContents(0, List.of(items));
        assertSame(item, data.getContent(0, 0));
        assertSame(item, data.getContent(0, 44));
        assertNull(data.getContent(0, 1));
    }

    @Test
    @DisplayName("Resize preserves existing items within new bounds")
    void testResizeShrink() {
        EnderChestData data = new EnderChestData(playerId, 3);
        ItemStack item = mock(ItemStack.class);
        data.setContent(0, 0, item);
        data.setContent(1, 0, item);
        data.resize(1);
        assertEquals(1, data.getPages());
        assertSame(item, data.getContent(0, 0));
    }

    @Test
    @DisplayName("Resize grow adds null slots")
    void testResizeGrow() {
        EnderChestData data = new EnderChestData(playerId, 1);
        ItemStack item = mock(ItemStack.class);
        data.setContent(0, 0, item);
        data.resize(3);
        assertEquals(3, data.getPages());
        assertSame(item, data.getContent(0, 0));
        assertNull(data.getContent(1, 0));
        assertNull(data.getContent(2, 0));
    }

    @Test
    @DisplayName("Out of bounds page throws IndexOutOfBoundsException")
    void testOutOfBoundsPage() {
        EnderChestData data = new EnderChestData(playerId, 2);
        assertThrows(IndexOutOfBoundsException.class, () -> data.getContent(2, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> data.getContent(-1, 0));
    }

    @Test
    @DisplayName("Out of bounds slot throws IndexOutOfBoundsException")
    void testOutOfBoundsSlot() {
        EnderChestData data = new EnderChestData(playerId, 1);
        assertThrows(IndexOutOfBoundsException.class, () -> data.getContent(0, 45));
        assertThrows(IndexOutOfBoundsException.class, () -> data.getContent(0, -1));
    }

    @Test
    @DisplayName("setContent null clears a slot")
    void testSetNullClears() {
        EnderChestData data = new EnderChestData(playerId, 1);
        ItemStack item = mock(ItemStack.class);
        data.setContent(0, 5, item);
        assertSame(item, data.getContent(0, 5));
        data.setContent(0, 5, null);
        assertNull(data.getContent(0, 5));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :test --tests "dev.yanianz.essentials.enderchest.EnderChestDataTest" --console=plain --no-daemon`
Expected: FAIL — class not found

- [ ] **Step 3: Write minimal implementation**

```java
package dev.yanianz.essentials.enderchest;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class EnderChestData {

    private final UUID playerId;
    private ItemStack[] contents;
    private int pages;

    public EnderChestData(UUID playerId, int pages) {
        this.playerId = playerId;
        this.pages = Math.max(1, pages);
        this.contents = new ItemStack[EnderChestSlotMap.totalSize(this.pages)];
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public int getPages() {
        return pages;
    }

    public ItemStack getContent(int page, int slot) {
        checkBounds(page, slot);
        return contents[EnderChestSlotMap.toFlatIndex(page, slot)];
    }

    public void setContent(int page, int slot, ItemStack item) {
        checkBounds(page, slot);
        contents[EnderChestSlotMap.toFlatIndex(page, slot)] = item;
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

    ItemStack[] rawContents() {
        return contents;
    }

    void setRawContents(ItemStack[] contents, int pages) {
        this.contents = contents;
        this.pages = pages;
    }

    private void checkBounds(int page, int slot) {
        if (page < 0 || page >= pages) throw new IndexOutOfBoundsException("page " + page);
        if (slot < 0 || slot >= EnderChestSlotMap.CONTENT_SLOTS)
            throw new IndexOutOfBoundsException("slot " + slot);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :test --tests "dev.yanianz.essentials.enderchest.EnderChestDataTest" --console=plain --no-daemon`
Expected: PASS — 10 tests

- [ ] **Step 5: Commit**

```bash
git add src/main/java/dev/yanianz/essentials/enderchest/EnderChestData.java src/test/java/dev/yanianz/essentials/enderchest/EnderChestDataTest.java
git commit -m "feat(enderchest): backing store with 10 tests"
```

---

### Task 3: Config, Module scaffold, and Permission-based page count

**Files:**
- Create: `src/main/resources/modules/enderchest/config.yml`
- Create: `src/main/java/dev/yanianz/essentials/enderchest/EnderChestModule.java`
- Modify: `src/main/java/fr/maxlego08/essentials/module/ZModuleManager.java` (add registration)

**Interfaces:**
- Consumes: `EnderChestData`, `ZModule`, `Permission.ESSENTIALS_ENDERCHEST.asPermission(".pages." + n)`
- Produces: `EnderChestModule.getAllowedPages(Player)`, `EnderChestModule.getData(UUID)`, `EnderChestModule.saveData(UUID)`, `EnderChestModule.openEnderChest(Player)`, `EnderChestModule.openEnderChestFor(Player, OfflinePlayer)`

- [ ] **Step 1: Create config file**

```yaml
########################################################################################################################
#
# zEssentials - Ender Chest
# Custom Hypixel SkyBlock-style enderchest with pagination and permission-sized pages.
#
########################################################################################################################

# Config schema version — do not edit
config-version: 1

enable: true

# Default number of pages for players without a page permission.
default-pages: 1

# Maximum pages any player can have (hard cap).
max-pages: 3

# Title shown at the top of the enderchest GUI.
# %page% and %total% placeholders are available.
title: "&5&lEnder Chest &8(&f%page%&8/&f%total%&8)"

# Navigation row configuration (bottom row, slots 45-53).
nav-row:
  filler-material: GRAY_STAINED_GLASS_PANE
  filler-color: "&8"
  prev-button: ARROW
  prev-text: "&7« Previous Page"
  next-button: ARROW
  next-text: "&7Next Page »"
  close-button: BARRIER
  close-text: "&cClose"
  page-indicator: true
  page-indicator-text: "&fPage &e%current% &7/ &e%total%"
```

- [ ] **Step 2: Create EnderChestModule scaffold**

```java
package dev.yanianz.essentials.enderchest;

import fr.maxlego08.essentials.ZEssentialsPlugin;
import fr.maxlego08.essentials.api.configuration.NonLoadable;
import fr.maxlego08.essentials.api.commands.Permission;
import fr.maxlego08.essentials.module.ZModule;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EnderChestModule extends ZModule {

    private int defaultPages = 1;
    private int maxPages = 3;
    private String title = "&5&lEnder Chest &8(&f%page%&8/&f%total%&8)";
    private String navFillerMaterial = "GRAY_STAINED_GLASS_PANE";
    private String navFillerColor = "&8";
    private String navPrevButton = "ARROW";
    private String navPrevText = "&7« Previous Page";
    private String navNextButton = "ARROW";
    private String navNextText = "&7Next Page »";
    private String navCloseButton = "BARRIER";
    private String navCloseText = "&cClose";
    private boolean pageIndicator = true;
    private String pageIndicatorText = "&fPage &e%current% &7/ &e%total%";

    @NonLoadable
    private final Map<UUID, EnderChestData> dataCache = new HashMap<>();

    public EnderChestModule(ZEssentialsPlugin plugin) {
        super(plugin, "enderchest");
    }

    @Override
    public void loadConfiguration() {
        super.loadConfiguration();

        YamlConfiguration config = getConfiguration();
        this.defaultPages = Math.max(1, config.getInt("default-pages", 1));
        this.maxPages = Math.max(1, config.getInt("max-pages", 3));
        this.title = config.getString("title", "&5&lEnder Chest &8(&f%page%&8/&f%total%&8)");

        YamlConfiguration nav = config.getConfigurationSection("nav-row");
        if (nav != null) {
            this.navFillerMaterial = nav.getString("filler-material", "GRAY_STAINED_GLASS_PANE");
            this.navFillerColor = nav.getString("filler-color", "&8");
            this.navPrevButton = nav.getString("prev-button", "ARROW");
            this.navPrevText = nav.getString("prev-text", "&7« Previous Page");
            this.navNextButton = nav.getString("next-button", "ARROW");
            this.navNextText = nav.getString("next-text", "&7Next Page »");
            this.navCloseButton = nav.getString("close-button", "BARRIER");
            this.navCloseText = nav.getString("close-text", "&cClose");
            this.pageIndicator = nav.getBoolean("page-indicator", true);
            this.pageIndicatorText = nav.getString("page-indicator-text", "&fPage &e%current% &7/ &e%total%");
        }
    }

    public int getAllowedPages(Player player) {
        int best = defaultPages;
        for (int n = maxPages; n >= 1; n--) {
            if (player.hasPermission(Permission.ESSENTIALS_ENDERCHEST.asPermission(".pages." + n))) {
                best = Math.min(n, maxPages);
                break;
            }
        }
        return Math.max(1, best);
    }

    public EnderChestData getData(UUID playerId) {
        EnderChestData data = dataCache.get(playerId);
        if (data != null) return data;

        File file = getDataFile(playerId);
        if (file.exists()) {
            data = loadFromFile(file, playerId);
        }
        if (data == null) {
            data = new EnderChestData(playerId, maxPages);
            Player online = Bukkit.getPlayer(playerId);
            if (online != null) {
                migrateFromVanilla(data, online);
            }
        }
        dataCache.put(playerId, data);
        return data;
    }

    private void migrateFromVanilla(EnderChestData data, Player player) {
        ItemStack[] vanilla = player.getEnderChest().getContents();
        for (int i = 0; i < Math.min(vanilla.length, EnderChestSlotMap.CONTENT_SLOTS); i++) {
            if (vanilla[i] != null && !vanilla[i].getType().isAir()) {
                data.setContent(0, i, vanilla[i]);
            }
        }
        player.getEnderChest().clear();
    }

    public void saveData(UUID playerId) {
        EnderChestData data = dataCache.get(playerId);
        if (data == null) return;
        saveToFile(data);
    }

    private File getDataFile(UUID playerId) {
        File dir = new File(getFolder(), "data");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, playerId + ".json");
    }

    private EnderChestData loadFromFile(File file, UUID playerId) {
        try {
            String json = Files.readString(file.toPath());
            return EnderChestSerializer.deserialize(json, playerId);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void saveToFile(EnderChestData data) {
        try {
            String json = EnderChestSerializer.serialize(data);
            Files.writeString(getDataFile(data.getPlayerId()).toPath(), json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void openEnderChest(Player player) {
        int allowed = getAllowedPages(player);
        EnderChestData data = getData(player.getUniqueId());
        if (data.getPages() < allowed) {
            data.resize(allowed);
        }
        int visiblePages = Math.min(data.getPages(), allowed);
        EnderChestGui.open(this.plugin, player, data, visiblePages, 0, false);
    }

    public void openEnderChestFor(Player viewer, OfflinePlayer target) {
        EnderChestData data = getData(target.getUniqueId());
        int pages = data.getPages();
        EnderChestGui.open(this.plugin, viewer, data, pages, 0, true);
    }

    public String getTitle() { return title; }
    public String getNavFillerMaterial() { return navFillerMaterial; }
    public String getNavFillerColor() { return navFillerColor; }
    public String getNavPrevButton() { return navPrevButton; }
    public String getNavPrevText() { return navPrevText; }
    public String getNavNextButton() { return navNextButton; }
    public String getNavNextText() { return navNextText; }
    public String getNavCloseButton() { return navCloseButton; }
    public String getNavCloseText() { return navCloseText; }
    public boolean isPageIndicator() { return pageIndicator; }
    public String getPageIndicatorText() { return pageIndicatorText; }
    public int getMaxPages() { return maxPages; }
}
```

- [ ] **Step 3: Create EnderChestSerializer**

```java
package dev.yanianz.essentials.enderchest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public final class EnderChestSerializer {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private EnderChestSerializer() {}

    public static String serialize(EnderChestData data) {
        SerializedData raw = new SerializedData();
        raw.player_uuid = data.getPlayerId().toString();
        raw.pages = data.getPages();
        ItemStack[] contents = data.rawContents();
        raw.contents = new Object[contents.length];
        for (int i = 0; i < contents.length; i++) {
            raw.contents[i] = contents[i] == null ? null : contents[i].serialize();
        }
        return GSON.toJson(raw);
    }

    public static EnderChestData deserialize(String json, UUID playerId) {
        SerializedData raw = GSON.fromJson(json, SerializedData.class);
        if (raw == null) return null;
        int pages = Math.max(1, raw.pages);
        ItemStack[] contents = new ItemStack[EnderChestSlotMap.totalSize(pages)];
        if (raw.contents != null) {
            for (int i = 0; i < Math.min(raw.contents.length, contents.length); i++) {
                if (raw.contents[i] != null) {
                    contents[i] = ItemStack.deserialize(raw.contents[i]);
                }
            }
        }
        EnderChestData data = new EnderChestData(playerId, pages);
        data.setRawContents(contents, pages);
        return data;
    }

    private static final class SerializedData {
        String player_uuid;
        int pages;
        Object[] contents;
    }
}
```

- [ ] **Step 4: Create EnderChestGui**

```java
package dev.yanianz.essentials.enderchest;

import dev.yanianz.essentials.util.ColorUtil;
import fr.maxlego08.essentials.ZEssentialsPlugin;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class EnderChestGui {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private EnderChestGui() {}

    public static EnderChestHolder open(ZEssentialsPlugin plugin, Player player,
                                        EnderChestData data, int pages, int startPage, boolean readOnly) {
        EnderChestModule module = plugin.getModuleManager().getModule(EnderChestModule.class);
        int currentPage = Math.max(0, Math.min(startPage, pages - 1));

        EnderChestHolder holder = new EnderChestHolder(player.getUniqueId(), data, currentPage, pages, readOnly);
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

    static void fillContent(Inventory inventory, EnderChestData data, int page) {
        List<ItemStack> contents = data.getPageContents(page);
        for (int slot = 0; slot < EnderChestSlotMap.CONTENT_SLOTS; slot++) {
            ItemStack item = contents.get(slot);
            if (item != null) inventory.setItem(slot, item);
        }
    }

    static void fillNavRow(Inventory inventory, EnderChestModule module,
                           int currentPage, int pages, EnderChestHolder holder) {
        Material filler = parseMaterial(module.getNavFillerMaterial(), Material.GRAY_STAINED_GLASS_PANE);
        ItemStack fillerItem = namedItem(filler, module.getNavFillerColor() + " ");

        for (int slot = EnderChestSlotMap.NAV_ROW_START; slot < 54; slot++) {
            inventory.setItem(slot, fillerItem);
        }

        if (currentPage > 0) {
            inventory.setItem(EnderChestSlotMap.SLOT_PREV,
                    namedItem(parseMaterial(module.getNavPrevButton(), Material.ARROW), module.getNavPrevText()));
        }
        inventory.setItem(EnderChestSlotMap.SLOT_CLOSE,
                namedItem(parseMaterial(module.getNavCloseButton(), Material.BARRIER), module.getNavCloseText()));

        if (module.isPageIndicator()) {
            String indicator = module.getPageIndicatorText()
                    .replace("%current%", String.valueOf(currentPage + 1))
                    .replace("%total%", String.valueOf(pages));
            inventory.setItem(47, namedItem(filler, indicator));
        }

        if (currentPage < pages - 1) {
            inventory.setItem(EnderChestSlotMap.SLOT_NEXT,
                    namedItem(parseMaterial(module.getNavNextButton(), Material.ARROW), module.getNavNextText()));
        }
    }

    static void switchPage(ZEssentialsPlugin plugin, Player player, EnderChestHolder holder, int newPage) {
        if (newPage < 0 || newPage >= holder.getPages()) return;
        holder.setCurrentPage(newPage);
        EnderChestModule module = plugin.getModuleManager().getModule(EnderChestModule.class);
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
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (Exception e) {
            return fallback;
        }
    }
}
```

- [ ] **Step 5: Create EnderChestHolder**

```java
package dev.yanianz.essentials.enderchest;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public class EnderChestHolder implements InventoryHolder {

    private final UUID playerId;
    private final EnderChestData data;
    private final int pages;
    private final boolean readOnly;
    private int currentPage;
    private Inventory inventory;

    public EnderChestHolder(UUID playerId, EnderChestData data, int currentPage, int pages, boolean readOnly) {
        this.playerId = playerId;
        this.data = data;
        this.currentPage = currentPage;
        this.pages = pages;
        this.readOnly = readOnly;
    }

    public UUID getPlayerId() { return playerId; }
    public EnderChestData getData() { return data; }
    public int getCurrentPage() { return currentPage; }
    public void setCurrentPage(int page) { this.currentPage = page; }
    public int getPages() { return pages; }
    public boolean isReadOnly() { return readOnly; }

    void setInventory(Inventory inventory) { this.inventory = inventory; }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
```

- [ ] **Step 6: Register module in ZModuleManager**

Add after line ~120 (before `this.loadConfigurations();`):
```java
        this.modules.put(EnderChestModule.class, new EnderChestModule(this.plugin));
```

Add import at top:
```java
import dev.yanianz.essentials.enderchest.EnderChestModule;
```

- [ ] **Step 7: Build to verify compilation**

Run: `./gradlew build -x test --console=plain`
Expected: BUILD SUCCESSFUL

- [ ] **Step 8: Run existing tests to verify no regression**

Run: `./gradlew test --console=plain --no-daemon`
Expected: BUILD SUCCESSFUL — all existing tests pass (new SlotMap + Data tests pass too)

- [ ] **Step 9: Commit**

```bash
git add src/main/resources/modules/enderchest/config.yml \
  src/main/java/dev/yanianz/essentials/enderchest/EnderChestModule.java \
  src/main/java/dev/yanianz/essentials/enderchest/EnderChestSerializer.java \
  src/main/java/dev/yanianz/essentials/enderchest/EnderChestGui.java \
  src/main/java/dev/yanianz/essentials/enderchest/EnderChestHolder.java \
  src/main/java/fr/maxlego08/essentials/module/ZModuleManager.java
git commit -m "feat(enderchest): module scaffold, config, GUI, holder, serializer"
```

---

### Task 4: EnderChestListener — click/close/drag handling

**Files:**
- Create: `src/main/java/dev/yanianz/essentials/enderchest/EnderChestListener.java`
- Modify: `src/main/java/dev/yanianz/essentials/enderchest/EnderChestModule.java` (register listener in loadConfiguration)

**Interfaces:**
- Consumes: `EnderChestHolder`, `EnderChestSlotMap`, `EnderChestGui.switchPage()`, `EnderChestModule.saveData()`

- [ ] **Step 1: Create EnderChestListener**

```java
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
            plugin.getScheduler().runNextTick(() -> syncSlot(holder, slot));
        } else {
            int rawSlot = event.getRawSlot();
            if (rawSlot < 0) return;
            if (event.getClick().isShiftClick()) {
                plugin.getScheduler().runNextTick(() -> syncAllSlots(holder));
            }
        }
    }

    private void handleNavClick(Player player, EnderChestHolder holder, int slot) {
        if (slot == EnderChestSlotMap.SLOT_CLOSE) {
            player.closeInventory();
        } else if (slot == EnderChestSlotMap.SLOT_PREV) {
            EnderChestGui.switchPage(plugin, player, holder, holder.getCurrentPage() - 1);
        } else if (slot == EnderChestSlotMap.SLOT_NEXT) {
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

        plugin.getScheduler().runNextTick(() -> {
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
```

- [ ] **Step 2: Register listener in EnderChestModule.loadConfiguration()**

Add at the end of `loadConfiguration()` in `EnderChestModule.java`:

```java
        Bukkit.getPluginManager().registerEvents(
                new EnderChestListener(this.plugin), this.plugin);
```

Wait — the module system already registers event listeners for modules that implement `Listener` and have `isRegisterEvent()` returning true. But `EnderChestListener` is not the module itself. Let me check how ScreenFactory does it — it registers its own listener in the constructor.

Actually, the cleanest approach is to make `EnderChestModule` implement `Listener` itself and put the event handlers directly in it, like how `ReportsModule` does it (it has `@EventHandler onJoin`). But that makes the module file large. Alternatively, register the listener in the module constructor or loadConfiguration.

Looking at `ScreenFactory` constructor: `Bukkit.getPluginManager().registerEvents(new ScreenClickListener(), plugin);`

I'll register the listener in the module constructor, guarded to avoid double registration on reload:

Add to `EnderChestModule` constructor:

```java
    private static boolean listenerRegistered = false;

    public EnderChestModule(ZEssentialsPlugin plugin) {
        super(plugin, "enderchest");
        if (!listenerRegistered) {
            Bukkit.getPluginManager().registerEvents(new EnderChestListener(plugin), plugin);
            listenerRegistered = true;
        }
    }
```

- [ ] **Step 3: Build to verify compilation**

Run: `./gradlew build -x test --console=plain`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Run all tests**

Run: `./gradlew test --console=plain --no-daemon`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add src/main/java/dev/yanianz/essentials/enderchest/EnderChestListener.java \
  src/main/java/dev/yanianz/essentials/enderchest/EnderChestModule.java
git commit -m "feat(enderchest): click/close/drag listeners with immediate sync"
```

---

### Task 5: Update commands and add messages

**Files:**
- Modify: `src/main/java/fr/maxlego08/essentials/commands/commands/enderchest/CommandEnderChest.java`
- Modify: `src/main/java/fr/maxlego08/essentials/commands/commands/enderchest/CommandEnderSee.java`
- Modify: `API/src/main/java/fr/maxlego08/essentials/api/messages/Message.java`
- Modify: `src/main/resources/messages/messages.yml`

**Interfaces:**
- Consumes: `EnderChestModule.openEnderChest()`, `EnderChestModule.openEnderChestFor()`

- [ ] **Step 1: Add Message enum entries**

In `Message.java`, after `COMMAND_ENDERSEE_ERROR` (line 59):

```java
    COMMAND_ENDERSEE_ERROR("<error>Unable to load the ender chest of &f%player%<error>."),
    COMMAND_ENDERCHEST_OPENED("<success>You opened your ender chest."),
    COMMAND_ENDERSEE_OPENED("<success>You opened &f%player%<success>'s ender chest."),
    COMMAND_ENDERSEE_READONLY("<7>You are viewing this ender chest in read-only mode."),
```

- [ ] **Step 2: Add messages.yml entries**

After `command-endersee-error:` (search for it, add the 3 new keys):

```yaml
command-enderchest-opened: "<success>You opened your ender chest."
command-endersee-opened: "<success>You opened &f%player%<success>'s ender chest."
command-endersee-readonly: "<7>You are viewing this ender chest in read-only mode."
```

- [ ] **Step 3: Update CommandEnderChest**

Replace the entire `perform` method:

```java
    @Override
    protected CommandResultType perform(EssentialsPlugin plugin) {
        EnderChestModule module = plugin.getModuleManager().getModule(EnderChestModule.class);
        module.openEnderChest(this.player);
        message(this.sender, Message.COMMAND_ENDERCHEST_OPENED);
        return CommandResultType.SUCCESS;
    }
```

Add imports:
```java
import dev.yanianz.essentials.enderchest.EnderChestModule;
```

- [ ] **Step 4: Update CommandEnderSee**

Replace the `perform` method's online branch (the `targetPlayer != null` case) to use the custom GUI:

```java
    @Override
    protected CommandResultType perform(EssentialsPlugin plugin) {

        OfflinePlayer offlinePlayer = this.argAsOfflinePlayer(0);
        if (offlinePlayer.isOnline()) {

            var targetPlayer = offlinePlayer.getPlayer();
            if (targetPlayer == null) return CommandResultType.SYNTAX_ERROR;

            EnderChestModule module = plugin.getModuleManager().getModule(EnderChestModule.class);
            module.openEnderChestFor(this.player, offlinePlayer);
            message(this.sender, Message.COMMAND_ENDERSEE_OPENED, "%player%", offlinePlayer.getName());
            message(this.sender, Message.COMMAND_ENDERSEE_READONLY);

        } else {

            if (!hasPermission(sender, Permission.ESSENTIALS_ENDERSEE_OFFLINE)) return CommandResultType.NO_PERMISSION;

            EnderChestModule module = plugin.getModuleManager().getModule(EnderChestModule.class);
            module.openEnderChestFor(this.player, offlinePlayer);
            message(this.sender, Message.COMMAND_ENDERSEE_OPENED, "%player%", offlinePlayer.getName());
            message(this.sender, Message.COMMAND_ENDERSEE_READONLY);
        }
        return CommandResultType.SUCCESS;
    }
```

Add imports:
```java
import dev.yanianz.essentials.enderchest.EnderChestModule;
```

- [ ] **Step 5: Build to verify compilation**

Run: `./gradlew build -x test --console=plain`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Run all tests**

Run: `./gradlew test --console=plain --no-daemon`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
git add API/src/main/java/fr/maxlego08/essentials/api/messages/Message.java \
  src/main/resources/messages/messages.yml \
  src/main/java/fr/maxlego08/essentials/commands/commands/enderchest/CommandEnderChest.java \
  src/main/java/fr/maxlego08/essentials/commands/commands/enderchest/CommandEnderSee.java
git commit -m "feat(enderchest): wire commands to custom GUI, add messages"
```

---

### Task 6: Changelog and final verification

**Files:**
- Modify: `changelog.md`

- [ ] **Step 1: Add changelog entry**

In `changelog.md`, under the `# 1.2.0.0` heading, add a new `## Enderchest custom GUI` section after the `## UX polish` section:

```markdown
## Enderchest custom GUI

- **Custom paginated enderchest** (`/ec`, `/enderchest`) — Hypixel SkyBlock-style 54-slot GUI with bottom navigation row, replacing the vanilla 27-slot enderchest
- **Permission-based page count** — `essentials.enderchest.pages.2` and `.pages.3` grant 2/3 pages (90/135 usable slots); `default-pages` and `max-pages` config control defaults and caps
- **Vanilla migration** — first open copies existing vanilla enderchest contents into the new backing store and clears the vanilla chest to prevent duplication
- **Immediate sync** — every click syncs the affected slot back to the backing array; close saves to JSON; no item loss on crash/disconnect
- **Read-only `/endersee`** — viewing another player's enderchest cancels all clicks
- **Config-driven nav row** — filler material/color, prev/next/close buttons, page indicator text all configurable in `modules/enderchest/config.yml`
- **Self-healing config** — `config-version: 1` key included; `ConfigHealer` appends missing keys on load
```

- [ ] **Step 2: Full build + test**

Run: `./gradlew build --console=plain --no-daemon`
Expected: BUILD SUCCESSFUL — all tests pass

- [ ] **Step 3: Commit**

```bash
git add changelog.md
git commit -m "docs: changelog for enderchest custom GUI"
```
