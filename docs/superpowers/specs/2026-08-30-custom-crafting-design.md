# Custom Crafting GUI + Quick Crafting — Design Spec

## §G — Goal

Replace the vanilla 3x3 crafting table GUI with a Hypixel SkyBlock-style custom
54-slot GUI: a 3x3 crafting grid, a result slot, the player's inventory, and
a nav/decoration row. Recipe matching reuses Bukkit's built-in recipe registry.
Quick Crafting (craft as many as possible in one click) is gated behind a
VIP+ permission.

## §C — Context

### Existing infrastructure

- **CommandCraft** — `commands/utils/CommandCraft.java` opens `player.openWorkbench()`.
  Registered as `register("craft", CommandCraft.class)`. Permission: `ESSENTIALS_CRAFT`.
- **ProtocolLib hook** — `Hooks/ProtocolLib/` with `PacketListener.registerPackets()`.
  Already intercepts `WINDOW_ITEMS`/`SET_SLOT` (pricing tooltips) and `SYSTEM_CHAT`
  (chat) and `PLAYER_INFO` (tab layout). Can intercept `OPEN_WINDOW` to redirect
  crafting table opens to the custom GUI.
- **Enderchest pattern (#2)** — `EnderChestHolder` + `EnderChestGui` + `EnderChestListener`
  establishes the custom inventory holder pattern: a holder carries state, a
  listener handles clicks, the GUI builds the inventory. This sub-project reuses
  that pattern.
- **Bukkit recipe registry** — `Bukkit.getRecipesFor(ItemStack)` and
  `Bukkit.recipeIterator()` provide access to all shaped/shapeless recipes.
  ` CraftingInventory` handles result placement in vanilla workbenches.

### Key constraint

The custom GUI must use a fake `InventoryHolder`. Bukkit's recipe matching via
`Bukkit.getCraftingRecipe()` or manual grid matching must be done in Java since
the custom inventory is not a `CraftingInventory`. The grid is 3x3 (slots 0-8),
result is slot 9 (or a custom position), player inventory fills the rest.

## §I — Interfaces

### CraftingHolder (dev.yanianz.essentials.crafting)

```java
class CraftingHolder implements InventoryHolder {
    UUID playerId;
    Inventory inventory;    // 54-slot custom inventory
    boolean quickCraftAllowed; // from permission

    static final int GRID_START = 0;    // slots 0-8 = 3x3 grid
    static final int GRID_SIZE = 9;
    static final int SLOT_RESULT = 13;  // centered result slot
    static final int SLOT_QUICK_CRAFT = 22; // quick craft button
    static final int SLOT_CLOSE = 49;
    static final int PLAYER_INV_START = 27; // slots 27-53 = player inventory
}
```

### CraftingModule (fr.maxlego08.essentials.module.modules)

```java
class CraftingModule extends ZModule {
    boolean enable;
    String quickCraftPermission;
    String title;
    String quickCraftText;
    String closeText;

    void openCrafting(Player player);
}
```

### CraftingListener (dev.yanianz.essentials.crafting)

```java
class CraftingListener implements Listener {
    // InventoryClickEvent: handle grid clicks, result slot pickup, quick craft button
    // InventoryCloseEvent: return grid items to player
    // InventoryDragEvent: allow drags in grid + player area
}
```

### RecipeMatcher (dev.yanianz.essentials.crafting)

```java
class RecipeMatcher {
    static ItemStack matchRecipe(ItemStack[] grid);  // 3x3 grid → result or null
    static int maxCraftable(ItemStack[] grid, Player player); // how many can be crafted
}
```

## §V — Invariants

1. **Crafting grid is slots 0-8 (3x3).** Result is slot 13. Quick Craft button
   is slot 22 (only shown if player has permission). Close is slot 49. Player
   inventory is slots 27-53. Slots 9-12, 14-21, 23-26 are decorative glass panes.
2. **Recipe matching uses Bukkit's recipe registry** — no custom recipe database.
   The 3x3 grid is matched against `Bukkit.getCraftingRecipe()` or equivalent.
3. **Result slot is read-only** — clicking it gives the result to the player
   and consumes the ingredients from the grid. Shift-click on result gives as
   many as possible (same as vanilla).
4. **Quick Craft crafts as many as possible in one click** — up to 64 or until
   materials run out. Gated behind `essentials.crafting.quickcraft` permission.
   Button is hidden for players without the permission.
5. **Grid items return on close** — when the inventory closes, any items left
   in the grid are returned to the player's main inventory. Overflow drops at
   their feet.
6. **Vanilla crafting table open is intercepted** — when a player right-clicks
   a crafting table, a ProtocolLib `OPEN_WINDOW` packet listener cancels the
   vanilla GUI and opens the custom GUI instead (if module is enabled).
7. **`/craft` command opens the custom GUI** — `CommandCraft` delegates to
   `CraftingModule.openCrafting()` instead of `player.openWorkbench()`.
8. **Config is self-healing** — `config-version: 1` key present.
9. **Folia-safe** — all inventory operations on the region thread.

## §T — Tasks

| # | Task | Files | Tests |
|---|------|-------|-------|
| 1 | Config + CraftingModule scaffold + ZModuleManager registration | `config.yml`, `CraftingModule.java`, `ZModuleManager.java` | Build passes |
| 2 | CraftingHolder + CraftingGui (54-slot inventory builder) | `CraftingHolder.java`, `CraftingGui.java` | Build passes |
| 3 | RecipeMatcher (Bukkit recipe registry matching) | `RecipeMatcher.java` | Unit test: grid matching |
| 4 | CraftingListener (click/close/drag handling) | `CraftingListener.java` | Build passes |
| 5 | CommandCraft update + OPEN_WINDOW interception | `CommandCraft.java`, `PacketCraftingListener.java`, `PacketListener.java` | Build passes |
| 6 | Messages + changelog + final verification | `Message.java`, `messages.yml`, `changelog.md` | All pass |

## §B — Bugs prevented

| # | Bug | Invariant |
|---|-----|-----------|
| 1 | Items lost when closing GUI with items in grid | §V.5 — return on close |
| 2 | Non-VIP players can quick craft | §V.4 — permission gate |
| 3 | Vanilla crafting table opens vanilla GUI instead of custom | §V.6 — packet interception |
| 4 | Duplicate result items from race condition | §V.3 — consume ingredients before giving result |
| 5 | Result slot accepts items | §V.1 — result slot is read-only |

## Config schema (modules/crafting/config.yml)

```yaml
config-version: 1
enable: true

# Title of the custom crafting GUI
title: "&8&lCrafting Table"

# Quick Craft button (shown only for VIP+ players)
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

# Decorative glass pane color
filler-material: GRAY_STAINED_GLASS_PANE
filler-color: "&8"

# Result slot item (shown when no recipe matches)
empty-result-material: AIR
```

## GUI Layout (54 slots)

```
Row 0 (slots 0-8):   [G][G][G]  Crafting grid 3x3 (slots 0-2)
Row 1 (slots 9-17):  [G][G][G]  Crafting grid 3x3 (slots 9-11)
Wait — 3x3 grid is 9 slots, not 9 per row. Let me lay it out properly:

Slot layout:
  0  1  2   = grid row 0
  9  10 11  = grid row 1
  18 19 20 = grid row 2
  13       = result slot (centered)
  22       = quick craft button
  4        = ? (filler)
  49       = close button
  27-53    = player inventory (27 slots = 36 - hotbar offset)
```

Actually, let me use a simpler layout matching Hypixel:

```
Slot  0-8:  Crafting grid (3x3, top-left)
Slot  13:   Result slot
Slot  22:   Quick Craft button
Slot  49:   Close button
Slots 27-53: Player inventory (bottom 3 rows)
All other slots: gray glass pane filler
```

## Data flow

```
Player right-clicks crafting table
  → Bukkit sends OPEN_WINDOW packet (type: CRAFTING)
  → ProtocolLib intercepts → cancel + open custom GUI
  → CraftingGui.open(player)
    → Create 54-slot Inventory with CraftingHolder
    → Fill decorative slots with glass panes
    → Fill player inventory slots 27-53 from player's inventory
    → If player has quickcraft permission, show button at slot 22
    → player.openInventory(inventory)

Player places items in grid (slots 0-8):
  → InventoryClickEvent → CraftingListener
  → Allow click in grid slots
  → After click, call RecipeMatcher.matchRecipe(grid)
  → If match: set result item at slot 13
  → If no match: clear slot 13

Player clicks result slot (slot 13):
  → InventoryClickEvent → CraftingListener
  → Cancel event (result slot is read-only)
  → Give result to player (add to inventory or drop)
  → Consume one ingredient from each non-empty grid slot
  → Re-match recipe → update result slot
  → If shift-click: repeat until grid empty or inventory full

Player clicks Quick Craft (slot 22):
  → InventoryClickEvent → CraftingListener
  → Cancel event
  → Check permission (belt and suspenders)
  → maxCraftable = RecipeMatcher.maxCraftable(grid, player)
  → Craft maxCraftable items, consuming ingredients
  → Give all results to player
  → Re-match recipe → update result slot

Player closes inventory:
  → InventoryCloseEvent → CraftingListener
  → Return all grid items to player's inventory
  → Overflow drops at player's location
```
