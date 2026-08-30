# Stash System — Design Spec

## §G — Goal

Add two personal storage systems inspired by Hypixel SkyBlock:
- **Item Stash** — holds up to 720 non-stackable items (weapons, tools, rare drops)
  across 16 paginated pages of 45 slots each.
- **Material Stash** — unlimited stackable items (cobblestone, crops), stored as
  material type + total count.

Both open via `/stash` with a category picker. Items are persisted as JSON files
per player using `ItemStack.serializeAsBytes()` (lossless).

## §C — Context

### Existing infrastructure reused

- **Enderchest pattern (#2)** — `EnderChestHolder` + `EnderChestGui` +
  `EnderChestListener` + `EnderChestData` + `EnderChestSerializer` establishes
  the custom inventory holder pattern with pagination and backing store.
  This sub-project reuses that pattern with modifications for stash semantics.
- **`ItemStack.serializeAsBytes()` / `deserializeBytes()`** — lossless item
  serialization used in #2's enderchest serializer.
- **SlotMap pattern** — `EnderChestSlotMap` defines slot constants and helpers.
  Reused for Item Stash pagination.
- **CommandLoader** — registers commands with aliases.
- **Module system** — ZModule scaffold at `fr.maxlego08.essentials.module.modules`.

### Key constraint

Item Stash uses 16 pages × 45 = 720 slots (4× the enderchest's default).
Material Stash has no fixed size — it stores material counts, not individual
slots. The Material Stash GUI shows one slot per material type with the total
count displayed in the lore, sorted alphabetically.

## §I — Interfaces

### ItemStashData (dev.yanianz.essentials.stash)

```java
class ItemStashData {
    UUID playerId;
    ItemStack[] contents;  // sized pages * 45, null = empty
    int pages;             // number of pages this player has access to

    ItemStack getContent(int page, int slot);
    void setContent(int page, int slot, ItemStack item);
    List<ItemStack> getPageContents(int page);
    void resize(int newPages);
}
```

### MaterialStashData (dev.yanianz.essentials.stash)

```java
class MaterialStashData {
    UUID playerId;
    Map<Material, Long> quantities;  // material → total count

    long get(Material material);
    void add(Material material, long amount);
    boolean remove(Material material, long amount);  // returns false if insufficient
    Set<Material> materials();
    long totalItems();  // sum of all quantities
}
```

### ItemStashHolder (dev.yanianz.essentials.stash)

```java
class ItemStashHolder implements InventoryHolder {
    UUID playerId;
    ItemStashData data;
    int currentPage;
    int pages;
    boolean readOnly;
    Inventory inventory;
}
```

### MaterialStashHolder (dev.yanianz.essentials.stash)

```java
class MaterialStashHolder implements InventoryHolder {
    UUID playerId;
    MaterialStashData data;
    boolean readOnly;
    Inventory inventory;
}
```

### StashModule (fr.maxlego08.essentials.module.modules)

```java
class StashModule extends ZModule {
    boolean enable;
    int maxItemPages;      // from config, default 16
    String title;

    void openItemStash(Player player);
    void openMaterialStash(Player player);
    void openCategoryPicker(Player player);
    ItemStashData getItemData(UUID playerId);
    MaterialStashData getMaterialData(UUID playerId);
    void saveItemData(UUID playerId);
    void saveMaterialData(UUID playerId);
}
```

## §V — Invariants

1. **Item Stash is 16 pages × 45 slots = 720 usable items.** Same slot layout
   as EnderChest: slots 0-44 interactive content, slots 45-53 locked nav row
   (prev/close/next + glass panes). Permission-based page count via
   `essentials.stash.item.pages.<n>`.
2. **Material Stash has no fixed slot count.** One slot per distinct material
   type. The GUI shows up to 54 material types on a single page (no pagination
   needed since most players have <20 unique materials). If a player has more,
   show "and X more..." in the bottom row.
3. **Non-stackable items go to Item Stash only.** Stackable items go to
   Material Stash. When a player shift-clicks an item from their inventory
   into the stash GUI, it's routed based on `item.getMaxStackSize() == 1`.
4. **Material Stash quantities are unbounded.** A cobblestone stash can hold
   millions of cobblestone. Withdrawing creates full stacks (64 or
   `material.getMaxStackSize()`).
5. **JSON persistence uses `serializeAsBytes()`** — same lossless approach
   as enderchest. File paths: `modules/stash/data/items/<uuid>.json` and
   `modules/stash/data/materials/<uuid>.json`.
6. **Immediate sync on click** — every content slot click syncs back to the
   backing array immediately (not just on close) to prevent item loss on crash.
7. **Close saves to disk** — `InventoryCloseEvent` writes the backing data
   to JSON before the listener returns.
8. **Permission downgrade preserves extra items** — same fix as enderchest:
   `resize()` is grow-only when opening, never shrinks the backing array.
9. **Vanilla migration** — first open migrates existing vanilla enderchest
   items into the Item Stash (same logic as enderchest #2 but targets stash).
10. **Config self-healing** — `config-version: 1` key present.

## §T — Tasks

| # | Task | Files | Tests |
|---|------|-------|-------|
| 1 | Config + StashModule scaffold + registration | `config.yml`, `StashModule.java`, `ZModuleManager.java` | Build passes |
| 2 | ItemStashData + MaterialStashData + serializers | `ItemStashData.java`, `MaterialStashData.java`, `serializers` | Unit tests |
| 3 | ItemStashHolder + ItemStashGui + ItemStashListener | `holder/gui/listener` | Build passes |
| 4 | MaterialStashHolder + MaterialStashGui + MaterialStashListener | `holder/gui/listener` | Build passes |
| 5 | CommandStash (category picker + subcommands) + messages | `CommandStash.java`, `Message.java`, `messages.yml` | Build passes |
| 6 | Changelog + final verification | `changelog.md` | All pass |

## §B — Bugs prevented

| # | Bug | Invariant |
|---|-----|-----------|
| 1 | Items lost on crash/disconnect | §V.6 — immediate sync + §V.7 — close save |
| 2 | Stackable items accidentally stored as individual slots | §V.3 — routing by maxStackSize |
| 3 | Non-stackable items sent to Material Stash (can't store there) | §V.3 — routing by maxStackSize |
| 4 | Vanilla duplication on first open | §V.9 — migrate then clear vanilla |
| 5 | Permission downgrade loses items | §V.8 — grow-only resize |

## Config schema (modules/stash/config.yml)

```yaml
config-version: 1
enable: true

# Maximum pages any player can have in Item Stash
max-item-pages: 16

# Title shown at the top of the Item Stash GUI
# %page% and %total% placeholders available
title: "&d&lItem Stash &8(&f%page%&8/&f%total%&8)"

# Navigation row configuration (same as enderchest)
nav-row:
  filler-material: GRAY_STAINED_GLASS_PANE
  filler-color: "&8"
  prev-button: ARROW
  prev-text: "&7« Previous Page"
  next-button: ARROW
  next-text: "&7Next Page »"
  close-button: BARRIER
  close-text: "&cClose"

# Material Stash GUI
material-title: "&e&lMaterial Stash"
material-filler-material: BLACK_STAINED_GLASS_PANE
material-close-material: BARRIER

# Category picker
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

# Migration on first open
migrate-from-vanilla-inventory: true
```

## Data flow

### Item Stash open
```
Player runs /stash item
  → StashModule.openItemStash(player)
  → Load/create ItemStashData (with vanilla migration on first load)
  → ItemStashGui.open(holder, data, currentPage=0)
    → Create 54-slot Inventory with ItemStashHolder
    → Fill content from data.getPageContents(page)
    → Fill nav row (glass + buttons)
    → player.openInventory(inventory)

Player clicks grid slot:
  → InventoryClickEvent → ItemStashListener
  → Allow normal item manipulation
  → Sync affected slot back to data.contents[]

Player switches page:
  → Nav button click → switchPage()
  → syncAllSlots before switching (§V.6)
  → Clear + refill with new page

Player closes:
  → InventoryCloseEvent → syncAllSlots + saveData(uuid)
```

### Material Stash open
```
Player runs /stash material
  → StashModule.openMaterialStash(player)
  → Load/create MaterialStashData
  → MaterialStashGui.open(holder, data)
    → Create 54-slot Inventory with MaterialStashHolder
    → For each material in data.materials():
      → Slot N: ItemStack(material, maxStackSize)
      → Lore: "&7Total: &f<quantity>"
    → Close button at slot 49
    → Filler elsewhere
    → player.openInventory(inventory)

Player shift-clicks item FROM inventory INTO Material Stash:
  → Listener routes to MaterialStashData.add(material, amount)
  → Updates GUI display

Player shift-clicks item FROM Material Stash TO inventory:
  → Listener computes how many to give (full stacks up to free space)
  → Removes from MaterialStashData
  → Gives items to player
  → Updates GUI display
```

## Permission nodes

- `essentials.stash` — base permission for /stash command
- `essentials.stash.item` — access to Item Stash
- `essentials.stash.material` — access to Material Stash
- `essentials.stash.item.pages.2` ... `.pages.16` — additional Item Stash pages
