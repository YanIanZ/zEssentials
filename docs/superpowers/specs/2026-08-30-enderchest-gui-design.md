# Enderchest Custom GUI — Design Spec

## §G — Goal

Replace the vanilla enderchest with a Hypixel SkyBlock-style custom GUI:
paginated, permission-sized, with a decorative bottom navigation bar. The
`/ec` (alias `/enderchest`) command opens the custom GUI instead of the raw
vanilla inventory. `/endersee` is updated to view other players' custom
enderchest.

This sub-project also establishes the reusable "paginated interactive GUI
with bottom nav row" pattern that #4 (Custom Crafting) and #5 (Stash) will
build on.

## §C — Context

### What exists today

- `CommandEnderChest.java` — opens `player.getEnderChest()` (vanilla 27 slots)
- `CommandEnderSee.java` — opens other players' enderchests (online: direct,
  offline: via NMS `PlayerUtil.openEnderChest`)
- `ScreenFactory` — paginated read-only screen (cancels ALL clicks); not
  suitable because the enderchest needs interactive item slots
- No enderchest module exists — no config, no `EnderChestModule`
- Module system: `ZModuleManager.loadModules()` registers modules; config at
  `modules/<name>/config.yml`; modules extend `ZModule`
- Commands: registered in `CommandLoader.register("enderchest", ..., "ec")`

### Key constraint

Vanilla `player.getEnderChest()` is a fixed 27-slot `Inventory`. The custom GUI
needs up to 135 usable slots (3 pages × 45). A custom backing store is required.

### Package convention

Per `AGENTS.md`: the module scaffold class goes under
`fr.maxlego08.essentials.module.modules.EnderChestModule.java`; the feature
logic goes under `dev.yanianz.essentials.enderchest.*`.

## §I — Interfaces

### EnderChestModule (fr.maxlego08.essentials.module.modules)

```
class EnderChestModule extends ZModule:
    int defaultPages           // from config, default 1
    int maxPages               // from config, default 3
    Map<UUID, EnderChestData> dataCache   // in-memory cache, @NonLoadable

    int getAllowedPages(Player player)   // permission-based: 
                                          // essentials.enderchest.pages.<n>
                                          // returns highest <n> ≤ maxPages,
                                          // or defaultPages if no permission
    EnderChestData getData(UUID playerId) // load from file or cache
    void saveData(UUID playerId)         // persist to JSON file
    void openEnderChest(Player player)    // opens the custom GUI
    void openEnderChestFor(Player viewer, OfflinePlayer target)
```

### EnderChestData (dev.yanianz.essentials.enderchest)

```
class EnderChestData:
    UUID playerId
    ItemStack[] contents   // sized pages * 45, null = empty
    int pages              // number of pages this player has

    ItemStack getContent(int page, int slot)   // slot 0-44
    void setContent(int page, int slot, ItemStack item)
    List<ItemStack> getPageContents(int page)   // 45 items
    void setPageContents(int page, List<ItemStack> items)
    void migrateFromVanilla(Player player)      // copy vanilla 27 slots
```

### EnderChestHolder (dev.yanianz.essentials.enderchest)

```
class EnderChestHolder implements InventoryHolder:
    UUID playerId
    EnderChestData data
    int currentPage
    int pages
    Inventory inventory       // the 54-slot Inventory

    int CONTENT_SLOTS = 45    // slots 0-44 are interactive
    int NAV_ROW_START = 45    // slots 45-53 are locked nav
```

## §V — Invariants

1. **Content area is slots 0-44; nav row is slots 45-53.** No item can be
   placed in the nav row. Clicks in the nav row are always cancelled.
2. **The backing `ItemStack[]` is always the source of truth.** The on-screen
   inventory is a view of one page slice. On close, slots 0-44 are synced back
   to the backing array before saving.
3. **`pages` is determined by permission at open time.** If a player's
   permission grants fewer pages than they currently have items in, the extra
   items are preserved in the backing array but not accessible (no data loss).
4. **Every `InventoryClickEvent` in a `EnderChestHolder` inventory syncs the
   affected content slot back to the backing array immediately** (not just on
   close) so that crash/disconnect never loses items.
5. **`InventoryCloseEvent` saves the backing data to disk.** No item is ever
   lost by closing the GUI.
6. **First open migrates vanilla enderchest contents.** If the backing file
   doesn't exist and the player is online, the 27 vanilla slots are copied
   into slots 0-26 of the new backing array. The vanilla enderchest is then
   cleared to avoid duplication.
7. **Filler glass panes in the nav row are named `" "` (a space)** so they
   appear blank and don't interfere with item tooltips.
8. **The bottom nav row contains exactly:**
   - slot 45: previous page arrow (if page > 1, else glass pane)
   - slot 49: close button (barrier)
   - slot 53: next page arrow (if page < pages, else glass pane)
   - all other nav slots: colored glass pane (configurable color)
9. **`/endersee` on a player with the custom enderchest opens the custom GUI
   in read-only mode** — the viewer can see items but not move them. This is
   implemented by cancelling ALL clicks when the holder's `playerId` does not
   match the viewer's UUID.
10. **Config is self-healing** — `config-version` key is present; missing keys
    are appended by `ConfigHealer` on load.
11. **ItemStack serialization uses Bukkit's `ConfigurationSerialization`** —
    `item.serialize()` produces a `Map<String,Object>` that Gson can persist;
    `ItemStack.deserialize(map)` restores it. Empty/air slots are stored as
    `null` in the JSON array to keep the file small.

## §T — Tasks

| # | Task | Files | Tests |
|---|------|-------|-------|
| 1 | Create `EnderChestModule` scaffold + config | `EnderChestModule.java`, `modules/enderchest/config.yml` | ConfigVersionCoverageTest auto-covers |
| 2 | Create `EnderChestData` backing store + JSON persistence | `EnderChestData.java` | Unit test: get/set content, page bounds, migration |
| 3 | Create `EnderChestHolder` + GUI builder | `EnderChestHolder.java`, `EnderChestGui.java` | Unit test: slot mapping, nav row layout |
| 4 | Create click/close listeners | `EnderChestListener.java` | Unit test: nav-row cancel, content sync, close save |
| 5 | Register module + update commands | `ZModuleManager.java`, `CommandLoader.java`, `CommandEnderChest.java`, `CommandEnderSee.java` | Build passes |
| 6 | Add permissions + messages | `Permission.java`, `Message.java`, `plugin.yml` | Build passes |
| 7 | Write integration tests | `EnderChestDataTest.java`, `EnderChestSlotMappingTest.java` | All pass |
| 8 | Update changelog | `changelog.md` | — |

## §B — Bugs prevented

| # | Bug | Invariant |
|---|-----|-----------|
| 1 | Items lost on page switch (only synced on close) | §V.4 — sync on every click |
| 2 | Items lost on crash/disconnect | §V.4 — immediate sync, §V.5 — close save |
| 3 | Vanilla enderchest duplication | §V.6 — clear vanilla after migration |
| 4 | Nav row accepts items | §V.1 — cancel all nav clicks |
| 5 | Player loses items when permission downgrades | §V.3 — extra items preserved in backing array |
| 6 | Shift-click bypasses nav row lock | §V.1 — cancel shift-click into nav slots |

## Config schema (modules/enderchest/config.yml)

```yaml
config-version: 1
enable: true
default-pages: 1
max-pages: 3
nav-row:
  filler-material: GRAY_STAINED_GLASS_PANE
  filler-color: "&7"
  prev-button: ARROW
  prev-text: "&7« Previous Page"
  next-button: ARROW
  next-text: "&7Next Page »"
  close-button: BARRIER
  close-text: "&cClose"
  page-indicator: true
  page-indicator-text: "&fPage &e%current% &7/&e %total%"
title: "&5&lEnder Chest"
```

### Permission nodes

- `essentials.enderchest` — open own enderchest (exists)
- `essentials.enderchest.pages.2` — access to 2 pages
- `essentials.enderchest.pages.3` — access to 3 pages
- `essentials.endersee` — view others' enderchest (exists)
- `essentials.endersee.offline` — view offline players' enderchest (exists)

### Message enum additions

- `COMMAND_ENDERCHEST_OPENED` — "Opened your enderchest."
- `COMMAND_ENDERSEE_OPENED` — "Opened %player%'s enderchest."
- `COMMAND_ENDERSEE_READONLY` — "Viewing in read-only mode."

### Data flow

```
Player runs /ec
  → EnderChestModule.getAllowedPages(player) → pages
  → EnderChestModule.getData(playerUuid) → load or create EnderChestData
    → if no file: migrateFromVanilla(player)
  → EnderChestGui.open(player, data, pages)
    → create 54-slot Inventory with EnderChestHolder
    → fill slots 0-44 from data.getPageContents(currentPage)
    → fill slots 45-53 with nav row (glass + buttons)
    → player.openInventory(inventory)

Player clicks slot 0-44:
  → InventoryClickEvent → EnderChestListener
  → allow click (don't cancel)
  → sync affected slots back to data.contents[]
  → (no save yet — save on close)

Player clicks slot 45-53:
  → InventoryClickEvent → EnderChestListener
  → cancel event
  → if prev/next/close button → handle navigation

Player closes inventory:
  → InventoryCloseEvent → EnderChestListener
  → sync slots 0-44 → data.contents[]
  → EnderChestModule.saveData(uuid) → write JSON file
```

### JSON storage format

File: `modules/enderchest/data/<uuid>.json`

```json
{
  "player_uuid": "...",
  "pages": 3,
  "contents": [
    null, {"material":"STONE","amount":64}, ...
  ]
}
```

The `contents` array is flat — `pages * 45` entries. Page `p` slot `s` maps to
index `p * 45 + s`. This is simple to serialize and test.
