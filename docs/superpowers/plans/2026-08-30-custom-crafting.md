# Custom Crafting GUI + Quick Crafting Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans.

**Goal:** Replace the vanilla 3x3 crafting table GUI with a custom 54-slot GUI with Quick Crafting for VIP+ players.

**Architecture:** A `CraftingHolder` InventoryHolder carries crafting state. `CraftingGui` builds the 54-slot inventory with a 3x3 grid, result slot, quick craft button, and player inventory. `RecipeMatcher` uses Bukkit's recipe registry to match the grid contents. `CraftingListener` handles clicks (grid manipulation, result pickup, quick craft, close-to-return-items). ProtocolLib intercepts `OPEN_WINDOW` packets to redirect crafting table opens to the custom GUI.

**Tech Stack:** Java 21, Bukkit/Paper API, ProtocolLib (packet interception), JUnit 5.

**Spec:** `docs/superpowers/specs/2026-08-30-custom-crafting-design.md`

## Global Constraints

- Java 21 bytecode target. Paper API is `compileOnly`.
- No comments in code unless explicitly requested.
- Folia-supported: all inventory operations on the region thread.
- Config self-healing: `config-version: 1` key in module config.
- Package convention: feature logic in `dev.yanianz.essentials.crafting.*`, scaffold in `fr.maxlego08.essentials.module.modules.CraftingModule`.
- Reuses the EnderChestHolder/GUI/Listener pattern from sub-project #2.
- Build: `./gradlew build -x test --console=plain`
- Test: `./gradlew test --console=plain --no-daemon`
- Working directory: `/Users/rheninxy/Sourby/zEssentials`

---

### Task 1: Config + CraftingModule + Registration

**Files:**
- Create: `src/main/resources/modules/crafting/config.yml`
- Create: `src/main/java/fr/maxlego08/essentials/module/modules/CraftingModule.java`
- Modify: `src/main/java/fr/maxlego08/essentials/module/ZModuleManager.java`

- [ ] **Step 1: Create config file**

```yaml
########################################################################################################################
#
# zEssentials - Custom Crafting GUI
# Replaces the vanilla crafting table with a Hypixel SkyBlock-style custom GUI.
# Requires ProtocolLib for crafting table interception.
#
########################################################################################################################

# Config schema version — do not edit
config-version: 1

enable: true

# Title of the custom crafting GUI
title: "&8&lCrafting Table"

# Quick Craft button (shown only for players with the permission)
quick-craft:
  material: ANVIL
  text: "&6&lQuick Craft"
  lore:
    - "&7Click to craft as many"
    - "&7as possible at once"
  permission: "essentials.crafting.quickcraft"

# Close button
close:
  material: BARRIER
  text: "&cClose"

# Decorative glass pane
filler-material: GRAY_STAINED_GLASS_PANE
filler-color: "&8"
```

- [ ] **Step 2: Create CraftingModule**

```java
package fr.maxlego08.essentials.module.modules;

import fr.maxlego08.essentials.ZEssentialsPlugin;
import fr.maxlego08.essentials.module.ZModule;
import org.bukkit.configuration.file.YamlConfiguration;

public class CraftingModule extends ZModule {

    private boolean enabled = true;
    private String title = "&8&lCrafting Table";
    private String quickCraftMaterial = "ANVIL";
    private String quickCraftText = "&6&lQuick Craft";
    private java.util.List<String> quickCraftLore = java.util.List.of();
    private String quickCraftPermission = "essentials.crafting.quickcraft";
    private String closeMaterial = "BARRIER";
    private String closeText = "&cClose";
    private String fillerMaterial = "GRAY_STAINED_GLASS_PANE";
    private String fillerColor = "&8";

    public CraftingModule(ZEssentialsPlugin plugin) {
        super(plugin, "crafting");
    }

    @Override
    public void loadConfiguration() {
        super.loadConfiguration();

        YamlConfiguration config = getConfiguration();
        this.enabled = config.getBoolean("enable", true);
        this.title = config.getString("title", "&8&lCrafting Table");
        this.fillerMaterial = config.getString("filler-material", "GRAY_STAINED_GLASS_PANE");
        this.fillerColor = config.getString("filler-color", "&8");
        this.closeMaterial = config.getString("close.material", "BARRIER");
        this.closeText = config.getString("close.text", "&cClose");

        YamlConfiguration qc = config.getConfigurationSection("quick-craft");
        if (qc != null) {
            this.quickCraftMaterial = qc.getString("material", "ANVIL");
            this.quickCraftText = qc.getString("text", "&6&lQuick Craft");
            this.quickCraftLore = qc.getStringList("lore");
            this.quickCraftPermission = qc.getString("permission", "essentials.crafting.quickcraft");
        }
    }

    public void openCrafting(org.bukkit.entity.Player player) {
        if (!this.enabled || !this.isEnable) return;
        dev.yanianz.essentials.crafting.CraftingGui.open(this.plugin, player, this);
    }

    public boolean isEnabled() { return this.enabled && this.isEnable; }
    public String getTitle() { return title; }
    public String getQuickCraftMaterial() { return quickCraftMaterial; }
    public String getQuickCraftText() { return quickCraftText; }
    public java.util.List<String> getQuickCraftLore() { return quickCraftLore; }
    public String getQuickCraftPermission() { return quickCraftPermission; }
    public String getCloseMaterial() { return closeMaterial; }
    public String getCloseText() { return closeText; }
    public String getFillerMaterial() { return fillerMaterial; }
    public String getFillerColor() { return fillerColor; }
}
```

- [ ] **Step 3: Register in ZModuleManager**

Add before `this.loadConfigurations()`:
```java
        this.modules.put(CraftingModule.class, new CraftingModule(this.plugin));
```
Add import: `import fr.maxlego08.essentials.module.modules.CraftingModule;`

- [ ] **Step 4: Build + commit**

```bash
./gradlew build -x test --console=plain
git add -A && git commit -m "feat(crafting): module scaffold + config"
```

---

### Task 2: CraftingHolder + CraftingGui + CraftingListener + RecipeMatcher + Command update + Messages + Changelog

This task creates the full crafting GUI system. It's a single large task because the pieces are tightly coupled.

**Files:**
- Create: `src/main/java/dev/yanianz/essentials/crafting/CraftingSlotMap.java`
- Create: `src/main/java/dev/yanianz/essentials/crafting/CraftingHolder.java`
- Create: `src/main/java/dev/yanianz/essentials/crafting/CraftingGui.java`
- Create: `src/main/java/dev/yanianz/essentials/crafting/RecipeMatcher.java`
- Create: `src/main/java/dev/yanianz/essentials/crafting/CraftingListener.java`
- Modify: `src/main/java/fr/maxlego08/essentials/commands/commands/utils/CommandCraft.java`
- Modify: `Hooks/ProtocolLib/src/main/java/fr/maxlego08/essentials/hooks/protocollib/PacketCraftingListener.java` (new)
- Modify: `Hooks/ProtocolLib/src/main/java/fr/maxlego08/essentials/hooks/protocollib/PacketListener.java`
- Modify: `API/src/main/java/fr/maxlego08/essentials/api/messages/Message.java`
- Modify: `src/main/resources/messages/messages.yml`
- Modify: `changelog.md`
- Test: `src/test/java/dev/yanianz/essentials/crafting/CraftingSlotMapTest.java`

- [ ] **Step 1: Create CraftingSlotMap**

```java
package dev.yanianz.essentials.crafting;

public final class CraftingSlotMap {
    private CraftingSlotMap() {}

    public static final int INVENTORY_SIZE = 54;
    public static final int GRID_START = 0;
    public static final int GRID_SIZE = 9;
    public static final int SLOT_RESULT = 13;
    public static final int SLOT_QUICK_CRAFT = 22;
    public static final int SLOT_CLOSE = 49;
    public static final int PLAYER_INV_START = 27;

    public static boolean isGridSlot(int slot) {
        return slot >= GRID_START && slot < GRID_START + GRID_SIZE;
    }

    public static boolean isResultSlot(int slot) {
        return slot == SLOT_RESULT;
    }

    public static boolean isQuickCraftSlot(int slot) {
        return slot == SLOT_QUICK_CRAFT;
    }

    public static boolean isCloseSlot(int slot) {
        return slot == SLOT_CLOSE;
    }

    public static boolean isPlayerInvSlot(int slot) {
        return slot >= PLAYER_INV_START && slot < INVENTORY_SIZE;
    }

    public static boolean isFillerSlot(int slot) {
        return !isGridSlot(slot) && !isResultSlot(slot) && !isQuickCraftSlot(slot)
                && !isCloseSlot(slot) && !isPlayerInvSlot(slot);
    }
}
```

- [ ] **Step 2: Write CraftingSlotMap test**

```java
package dev.yanianz.essentials.crafting;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CraftingSlotMapTest {

    @Test
    @DisplayName("Grid slots are 0-8")
    void testGridSlots() {
        for (int i = 0; i < 9; i++) assertTrue(CraftingSlotMap.isGridSlot(i));
        assertFalse(CraftingSlotMap.isGridSlot(9));
    }

    @Test
    @DisplayName("Result slot is 13")
    void testResultSlot() {
        assertTrue(CraftingSlotMap.isResultSlot(13));
        assertFalse(CraftingSlotMap.isResultSlot(0));
    }

    @Test
    @DisplayName("Quick craft slot is 22")
    void testQuickCraftSlot() {
        assertTrue(CraftingSlotMap.isQuickCraftSlot(22));
    }

    @Test
    @DisplayName("Close slot is 49")
    void testCloseSlot() {
        assertTrue(CraftingSlotMap.isCloseSlot(49));
    }

    @Test
    @DisplayName("Player inventory slots are 27-53")
    void testPlayerInvSlots() {
        for (int i = 27; i < 54; i++) assertTrue(CraftingSlotMap.isPlayerInvSlot(i));
        assertFalse(CraftingSlotMap.isPlayerInvSlot(26));
    }

    @Test
    @DisplayName("Filler slots are everything else")
    void testFillerSlots() {
        assertTrue(CraftingSlotMap.isFillerSlot(9));
        assertTrue(CraftingSlotMap.isFillerSlot(14));
        assertTrue(CraftingSlotMap.isFillerSlot(26));
        assertFalse(CraftingSlotMap.isFillerSlot(0));
        assertFalse(CraftingSlotMap.isFillerSlot(27));
    }
}
```

- [ ] **Step 3: Create CraftingHolder**

```java
package dev.yanianz.essentials.crafting;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public class CraftingHolder implements InventoryHolder {

    private final UUID playerId;
    private final boolean quickCraftAllowed;
    private Inventory inventory;

    public CraftingHolder(UUID playerId, boolean quickCraftAllowed) {
        this.playerId = playerId;
        this.quickCraftAllowed = quickCraftAllowed;
    }

    public UUID getPlayerId() { return playerId; }
    public boolean isQuickCraftAllowed() { return quickCraftAllowed; }
    void setInventory(Inventory inventory) { this.inventory = inventory; }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
```

- [ ] **Step 4: Create CraftingGui**

```java
package dev.yanianz.essentials.crafting;

import dev.yanianz.essentials.util.ColorUtil;
import fr.maxlego08.essentials.ZEssentialsPlugin;
import fr.maxlego08.essentials.module.modules.CraftingModule;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class CraftingGui {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private CraftingGui() {}

    public static CraftingHolder open(ZEssentialsPlugin plugin, Player player, CraftingModule module) {
        boolean quickCraft = player.hasPermission(module.getQuickCraftPermission());
        CraftingHolder holder = new CraftingHolder(player.getUniqueId(), quickCraft);

        Inventory inventory = Bukkit.createInventory(holder, 54,
                LEGACY.deserialize(ColorUtil.sections(module.getTitle())));
        holder.setInventory(inventory);

        ItemStack filler = namedItem(Material.valueOf(module.getFillerMaterial()), module.getFillerColor() + " ");
        for (int i = 0; i < 54; i++) {
            if (CraftingSlotMap.isFillerSlot(i)) {
                inventory.setItem(i, filler);
            }
        }

        if (quickCraft) {
            inventory.setItem(CraftingSlotMap.SLOT_QUICK_CRAFT,
                    namedItem(Material.valueOf(module.getQuickCraftMaterial()),
                            module.getQuickCraftText(), module.getQuickCraftLore()));
        } else {
            inventory.setItem(CraftingSlotMap.SLOT_QUICK_CRAFT, filler);
        }

        inventory.setItem(CraftingSlotMap.SLOT_CLOSE,
                namedItem(Material.valueOf(module.getCloseMaterial()), module.getCloseText()));

        for (int i = 0; i < 36; i++) {
            ItemStack playerItem = player.getInventory().getItem(i);
            if (playerItem != null) {
                inventory.setItem(CraftingSlotMap.PLAYER_INV_START + i, playerItem);
            }
        }

        player.openInventory(inventory);
        return holder;
    }

    static ItemStack namedItem(Material material, String nameLegacy) {
        return namedItem(material, nameLegacy, null);
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
}
```

- [ ] **Step 5: Create RecipeMatcher**

```java
package dev.yanianz.essentials.crafting;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import java.util.Iterator;

public final class RecipeMatcher {

    private RecipeMatcher() {}

    public static ItemStack matchRecipe(ItemStack[] grid) {
        if (grid == null || grid.length != 9) return null;

        boolean hasItem = false;
        for (ItemStack item : grid) {
            if (item != null && !item.getType().isAir()) { hasItem = true; break; }
        }
        if (!hasItem) return null;

        ItemStack[][] matrix = new ItemStack[3][3];
        for (int i = 0; i < 9; i++) {
            matrix[i / 3][i % 3] = grid[i];
        }

        Iterator<Recipe> iterator = Bukkit.recipeIterator();
        while (iterator.hasNext()) {
            Recipe recipe = iterator.next();
            if (recipe instanceof org.bukkit.inventory.ShapedRecipe shaped) {
                ItemStack result = tryShaped(shaped, matrix);
                if (result != null) return result;
            } else if (recipe instanceof org.bukkit.inventory.ShapelessRecipe shapeless) {
                ItemStack result = tryShapeless(shapeless, matrix);
                if (result != null) return result;
            }
        }
        return null;
    }

    private static ItemStack tryShaped(org.bukkit.inventory.ShapedRecipe recipe, ItemStack[][] grid) {
        String[] shape = recipe.getShape();
        var map = recipe.getIngredientMap();

        for (int rowOffset = 0; rowOffset <= 3 - shape.length; rowOffset++) {
            for (int colOffset = 0; colOffset <= 3 - shape[0].length(); colOffset++) {
                boolean match = true;
                for (int r = 0; r < shape.length && match; r++) {
                    for (int c = 0; c < shape[r].length() && match; c++) {
                        char key = shape[r].charAt(c);
                        ItemStack expected = map.get(key);
                        ItemStack actual = grid[rowOffset + r][colOffset + c];
                        if (!itemsMatch(expected, actual)) match = false;
                    }
                }
                if (match) {
                    for (int r = 0; r < shape.length; r++) {
                        for (int c = 0; c < shape[r].length(); c++) {
                            if (shape[r].charAt(c) != ' ') continue;
                            if (grid[rowOffset + r][colOffset + c] != null
                                    && !grid[rowOffset + r][colOffset + c].getType().isAir()) {
                                match = false;
                            }
                        }
                    }
                    if (match) return recipe.getResult().clone();
                }
            }
        }
        return null;
    }

    private static ItemStack tryShapeless(org.bukkit.inventory.ShapelessRecipe recipe, ItemStack[][] grid) {
        var ingredients = new java.util.ArrayList<>(recipe.getIngredientList());
        java.util.List<ItemStack> gridItems = new java.util.ArrayList<>();
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                if (grid[r][c] != null && !grid[r][c].getType().isAir()) {
                    gridItems.add(grid[r][c]);
                }
            }
        }
        if (gridItems.size() != ingredients.size()) return null;

        boolean[] used = new boolean[ingredients.size()];
        for (ItemStack gridItem : gridItems) {
            boolean found = false;
            for (int i = 0; i < ingredients.size(); i++) {
                if (used[i]) continue;
                if (itemsMatch(ingredients.get(i), gridItem)) {
                    used[i] = true;
                    found = true;
                    break;
                }
            }
            if (!found) return null;
        }
        return recipe.getResult().clone();
    }

    private static boolean itemsMatch(ItemStack expected, ItemStack actual) {
        if (expected == null || expected.getType() == Material.AIR) {
            return actual == null || actual.getType().isAir();
        }
        if (actual == null || actual.getType().isAir()) return false;
        return expected.getType() == actual.getType();
    }

    public static int maxCraftable(ItemStack[] grid) {
        ItemStack result = matchRecipe(grid);
        if (result == null) return 0;

        int minCount = Integer.MAX_VALUE;
        for (ItemStack item : grid) {
            if (item == null || item.getType().isAir()) continue;
            minCount = Math.min(minCount, item.getAmount());
        }
        return minCount == Integer.MAX_VALUE ? 0 : minCount;
    }
}
```

- [ ] **Step 6: Create CraftingListener**

```java
package dev.yanianz.essentials.crafting;

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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CraftingListener implements Listener {

    private final ZEssentialsPlugin plugin;
    private static boolean registered = false;

    public CraftingListener(ZEssentialsPlugin plugin) {
        this.plugin = plugin;
    }

    public static void ensureRegistered(ZEssentialsPlugin plugin) {
        if (!registered) {
            Bukkit.getPluginManager().registerEvents(new CraftingListener(plugin), plugin);
            registered = true;
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory inventory = event.getInventory();
        if (!(inventory.getHolder() instanceof CraftingHolder holder)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        int slot = event.getRawSlot();

        if (CraftingSlotMap.isFillerSlot(slot)) {
            event.setCancelled(true);
            return;
        }

        if (CraftingSlotMap.isCloseSlot(slot)) {
            event.setCancelled(true);
            player.closeInventory();
            return;
        }

        if (CraftingSlotMap.isQuickCraftSlot(slot)) {
            event.setCancelled(true);
            if (!holder.isQuickCraftAllowed()) return;
            performQuickCraft(inventory, player);
            updateResult(inventory);
            return;
        }

        if (CraftingSlotMap.isResultSlot(slot)) {
            event.setCancelled(true);
            ItemStack result = inventory.getItem(CraftingSlotMap.SLOT_RESULT);
            if (result == null || result.getType().isAir()) return;

            if (event.getClick().isShiftClick()) {
                craftMultiple(inventory, player, result);
            } else {
                craftOne(inventory, player, result);
            }
            updateResult(inventory);
            return;
        }

        if (CraftingSlotMap.isGridSlot(slot)) {
            plugin.getScheduler().runNextTick(wrappedTask -> updateResult(inventory));
            return;
        }

        if (CraftingSlotMap.isPlayerInvSlot(slot)) {
            return;
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        Inventory inventory = event.getInventory();
        if (!(inventory.getHolder() instanceof CraftingHolder)) return;

        for (int slot : event.getRawSlots()) {
            if (CraftingSlotMap.isFillerSlot(slot) || CraftingSlotMap.isResultSlot(slot)
                    || CraftingSlotMap.isQuickCraftSlot(slot) || CraftingSlotMap.isCloseSlot(slot)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();
        if (!(inventory.getHolder() instanceof CraftingHolder holder)) return;
        if (!(event.getPlayer() instanceof Player player)) return;

        for (int i = 0; i < 9; i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && !item.getType().isAir()) {
                Map<Integer, ItemStack> overflow = player.getInventory().addItem(item);
                for (ItemStack drop : overflow.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
                inventory.setItem(i, null);
            }
        }
    }

    private void updateResult(Inventory inventory) {
        ItemStack[] grid = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            ItemStack item = inventory.getItem(i);
            grid[i] = (item != null && !item.getType().isAir()) ? item : null;
        }
        ItemStack result = RecipeMatcher.matchRecipe(grid);
        inventory.setItem(CraftingSlotMap.SLOT_RESULT, result != null ? result : null);
    }

    private void craftOne(Inventory inventory, Player player, ItemStack result) {
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(result);
        for (ItemStack drop : overflow.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
        }
        for (int i = 0; i < 9; i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && !item.getType().isAir()) {
                item.setAmount(item.getAmount() - 1);
                if (item.getAmount() <= 0) inventory.setItem(i, null);
            }
        }
    }

    private void craftMultiple(Inventory inventory, Player player, ItemStack baseResult) {
        while (true) {
            ItemStack[] grid = new ItemStack[9];
            for (int i = 0; i < 9; i++) {
                ItemStack item = inventory.getItem(i);
                grid[i] = (item != null && !item.getType().isAir()) ? item : null;
            }
            ItemStack result = RecipeMatcher.matchRecipe(grid);
            if (result == null) break;

            int max = RecipeMatcher.maxCraftable(grid);
            int craftable = Math.min(max, result.getMaxStackSize());
            result.setAmount(craftable);
            Map<Integer, ItemStack> overflow = player.getInventory().addItem(result);
            for (ItemStack drop : overflow.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
            for (int i = 0; i < 9; i++) {
                ItemStack item = inventory.getItem(i);
                if (item != null && !item.getType().isAir()) {
                    item.setAmount(item.getAmount() - craftable);
                    if (item.getAmount() <= 0) inventory.setItem(i, null);
                }
            }
            if (max <= 1) break;
        }
    }

    private void performQuickCraft(Inventory inventory, Player player) {
        craftMultiple(inventory, player, inventory.getItem(CraftingSlotMap.SLOT_RESULT));
    }
}
```

- [ ] **Step 7: Update CommandCraft**

Replace the `perform` method:

```java
    @Override
    protected CommandResultType perform(EssentialsPlugin plugin) {
        fr.maxlego08.essentials.module.modules.CraftingModule module =
                plugin.getModuleManager().getModule(fr.maxlego08.essentials.module.modules.CraftingModule.class);
        if (module != null && module.isEnabled()) {
            dev.yanianz.essentials.crafting.CraftingListener.ensureRegistered((fr.maxlego08.essentials.ZEssentialsPlugin) plugin);
            module.openCrafting(this.player);
        } else {
            this.player.openWorkbench(this.player.getLocation(), true);
        }
        return CommandResultType.SUCCESS;
    }
```

- [ ] **Step 8: Create PacketCraftingListener (ProtocolLib)**

```java
package fr.maxlego08.essentials.hooks.protocollib;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.packet.PacketRegister;
import org.bukkit.entity.Player;

public class PacketCraftingListener extends PacketAdapter implements PacketRegister {

    private final EssentialsPlugin plugin;

    public PacketCraftingListener(EssentialsPlugin plugin) {
        super(PacketAdapter.params()
                .plugin(plugin)
                .listenerPriority(ListenerPriority.HIGHEST)
                .types(PacketType.Play.Server.OPEN_WINDOW));
        this.plugin = plugin;
    }

    @Override
    public void addPacketListener() {
        com.comphenix.protocol.ProtocolLibrary.getProtocolManager().addPacketListener(this);
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        try {
            var packet = event.getPacket();
            int windowType = packet.getIntegers().read(0);
            if (windowType != 2) return;

            var moduleClass = Class.forName("fr.maxlego08.essentials.module.modules.CraftingModule");
            var module = plugin.getModuleManager().getModule(moduleClass);
            if (module == null) return;

            var isEnabledMethod = moduleClass.getMethod("isEnabled");
            boolean enabled = (boolean) isEnabledMethod.invoke(module);
            if (!enabled) return;

            event.setCancelled(true);

            var openMethod = moduleClass.getMethod("openCrafting", Player.class);
            openMethod.invoke(module, event.getPlayer());

            var listenerClass = Class.forName("dev.yanianz.essentials.crafting.CraftingListener");
            var ensureMethod = listenerClass.getMethod("ensureRegistered", fr.maxlego08.essentials.ZEssentialsPlugin.class);
            ensureMethod.invoke(null, (fr.maxlego08.essentials.ZEssentialsPlugin) plugin);
        } catch (Exception ignored) {
        }
    }
}
```

- [ ] **Step 9: Register in PacketListener**

In `PacketListener.registerPackets()`, add:

```java
        this.register(new PacketCraftingListener(plugin));
```

- [ ] **Step 10: Add messages**

In `Message.java`, add:
```java
    DESCRIPTION_QUICKCRAFT("Quick craft items from the crafting grid"),
```

In `messages.yml`:
```yaml
description-quickcraft: "Quick craft items from the crafting grid"
```

- [ ] **Step 11: Build + test**

Run: `./gradlew build --console=plain --no-daemon`
Expected: BUILD SUCCESSFUL

- [ ] **Step 12: Add changelog**

```markdown
## Custom Crafting GUI + Quick Crafting

- **Custom crafting GUI** — replaces the vanilla 3x3 crafting table with a Hypixel SkyBlock-style 54-slot custom GUI (3x3 grid, result slot, player inventory, decorative fill)
- **Bukkit recipe matching** — reuses Bukkit's built-in recipe registry (shaped + shapeless), no custom recipe database needed
- **Quick Crafting** — VIP+ players (`essentials.crafting.quickcraft` permission) can craft as many items as possible in one click
- **Crafting table interception** — ProtocolLib intercepts `OPEN_WINDOW` packets and redirects to the custom GUI
- **Items return on close** — grid items return to player inventory on close, overflow drops at feet
- **Config-driven** — title, quick craft button material/text/lore, close button, filler color in `modules/crafting/config.yml`
```

- [ ] **Step 13: Commit**

```bash
git add -A && git commit -m "feat(crafting): custom 54-slot GUI + quick craft + ProtocolLib interception"
```
