# Versioning

Four digit scheme, continuing from release 1.0.4.0:

- **First digit** — significant milestone releases
- **Second digit** — new feature batch / new modules
- **Third digit** — small changes & polish
- **Fourth digit** — bug fixes

Every change lands in this changelog immediately under its bumped heading.

# Idée:

- ~~Ajouter un broadcast de message centré avec le support des \n pour ajouter plusieurs lignes~~ ✅ Done in 1.3.4.0
- ~~Ajouter une option pour désactiver la tabulation des joueurs hors ligne~~ ✅ Already existed (config: `enable-offline-player-names`), default changed to `false` in 1.3.4.0
- ~~Ajouter un placeholder pour transformed les caractères en lettre spécial~~ ✅ Done in 1.3.4.0

# 1.3.8.0

## Bug fixes

- **Fix UPSERT crash on SQLite** — all 7 new repositories (Nickname, Disguise, ChatPreference, Reputation, EnderChest, ItemStash, MaterialStash) were missing `.primary()` on the UUID column in their upsert methods, causing `IllegalStateException: UPSERT requires at least one non-auto-increment primary key or unique constraint for SQLite` when opening enderchest, using disguise/nick, saving chat preferences, or accessing stash
- **Crafting table reverted to vanilla** — removed the CRAFTING_TABLE right-click interception from `PlayerListener`; crafting tables now open the vanilla crafting GUI; crafting module config default changed to `enable: false`

# 1.3.7.0

## GUI title icons — Unicode emoji for all inventories

Added fitting Unicode icons to all 31 GUI inventory titles for better visual identification:

| Icon | GUI |
|------|-----|
| 📦 | EnderChest (+ overview, config title/info/page) |
| 💎 | Item Stash |
| 🔨 | Crafting Table |
| 🎁 | Kits |
| 📂 | Kit Categories |
| ⛏ | Tools Category |
| ⚔ | Combat Kits |
| 👁 | Kit Preview |
| 🗄 | Vault (+ admin) |
| ⚙ | Vault Configuration |
| 🌐 | Player Warps |
| 🗳 | Vote |
| 💰 | Baltop |
| ✅ | Confirm Pay |
| 🏠 | Homes (+ donut layout) |
| 🌍 | Public Homes |
| 🗑 | Home Delete |
| 🔮 | Warps |
| ✉ | Mailbox (+ admin) |
| 📜 | Rules |
| ⚖ | Sanction (+ sanctions list) |
| 📋 | Sanction History |
| 🤝 | Confirm Teleport Request (+ here variant) |

All titles now follow the pattern: `#3f3f3f<icon> <small-caps text>`

# 1.3.6.0

## UI/UX polish — messages + GUI

### Messages
- **Separator modernization** — replaced `&8&m--------` strikethrough dash separators in `warnings-header`, `notes-header`, `report-list-header` with clean Unicode vertical bars `#656665│` in all 7 message files (en + fr/de/es/it/nl/zh)
- **`notes-line` bug fix** — replaced legacy `§` section signs with `&` ampersand color codes (consistency with all other messages)
- **`messages_it.yml` prefix fix** — replaced hardcoded hex `#03fcb6zEssentials #656665•` with theme tokens `<primary>zEssentials <secondary>•` (matches all other language files)

### GUI titles modernized (10 files)
- `enderchest/config.yml` — `&5&lEnder Chest` → `#3f3f3fᴇɴᴅᴇʀ ᴄʜᴇꜱᴛ` (title, info-text, page-text)
- `stash/config.yml` — `&d&lItem Stash` → `#3f3f3fɪᴛᴇᴍ sᴛᴀsʜ`
- `crafting/config.yml` — `&8&lCrafting Table` → `#3f3f3fᴄʀᴀꜰᴛɪɴɢ ᴛᴀʙʟᴇ`
- `kits/kits_categories.yml` — `&8&l« &e&lKit Categories &8&l»` → `#3f3f3fᴋɪᴛ ᴄᴀᴛᴇɢᴏʀɪᴇs`
- `kits/kits_category_default.yml` — `&6&l⛏ &e&lTOOLS CATEGORY &6&l⛏` → `#3f3f3fᴛᴏᴏʟs ᴄᴀᴛᴇɢᴏʀʏ`
- `kits/kits_category_combat.yml` — `&4&l⚔ &c&lCOMBAT KITS &4&l⚔` → `#3f3f3fᴄᴏᴍʙᴀᴛ ᴋɪᴛs`
- `worldedit/pw-help.yml` — `&8Help Player Warp` → `#3f3f3fᴘʟᴀʏᴇʀ ᴡᴀʀᴘs`
- `vault/vault.yml` — `&f%vault-name%` → `#3f3f3f%vault-name%`
- `vault/vault-admin.yml` — `&f%player% - %vault-name%` → `#3f3f3f%player% - %vault-name%`
- `vault/vault-configuration.yml` — `&8ᴄᴏɴғɪɢᴜʀᴀᴛɪᴏɴ` → `#3f3f3fᴄᴏɴғɪɢᴜʀᴀᴛɪᴏɴ`

All GUI titles now use consistent `#3f3f3f` hex + Unicode small-caps style matching the enderchest/crafting/stash inventories.

# 1.3.5.0

## CMILib-inspired small features (4 new)

- **`/toast [player] <message>`** — show a custom advancement toast popup to yourself or another player; optional icon via `&&` separator (e.g. `/toast Hello && diamond`); permissions `essentials.toast` / `essentials.toast.other`; `ToastSender` uses NMS reflection to load a temporary advancement
- **`%zessentials_roman_<number>%`** — convert integers to Roman numerals (I, II, III... XLII... max 3999, fallback to plain number above)
- **`%zessentials_gametime_<ticks>%`** — convert game ticks to 24h time format (HH:MM, 0 ticks = 06:00, 6000 = 12:00)
- **`%zessentials_compactnum_<value>%`** — format large numbers compactly (1500 → 1.5K, 2300000 → 2.3M, 3B, 4T)

# 1.3.4.0

## Changelog Idée features (all 3 completed)

- **Centered multi-line broadcast** — `/broadcast` now always centers each line using the pixel-width centering algorithm, and splits on `\n` for multi-line broadcasts (e.g. `/broadcast Line1\nLine2` sends two centered lines)
- **Offline tab-completion default** — `config.yml` `enable-offline-player-names` default changed from `true` to `false` with a performance warning comment; on large servers, loading all player names from the database on every tab completion causes latency
- **Fancy text placeholders** — new `TextPlaceholders` class with 6 Unicode text styles:
  - `%zessentials_fancy_<text>%` — mathematical script (𝓯𝓪𝓷𝓬𝔂)
  - `%zessentials_bold_<text>%` — mathematical bold (𝐛𝐨𝐥𝐝)
  - `%zessentials_italic_<text>%` — mathematical italic (𝑖𝑡𝑎𝑙𝑖𝑐)
  - `%zessentials_smallcaps_<text>%` — small caps (sᴍᴀʟʟ ᴄᴀᴘs)
  - `%zessentials_mono_<text>%` — monospace (𝚖𝚘𝚗𝚘)
  - `%zessentials_double_<text>%` — double-struck (𝕕𝕠𝕦𝕓𝕝𝕖)

# 1.3.3.0

## Messages & translations upgrade

- **6 new message keys** — disguise baby/adult/profession/size feedback, villager type, stash withdraw-all success + empty
- **160 missing translation keys** filled in all 6 language files (fr/de/es/it/nl/zh) — 960 total new translations across all languages, preserving format tokens (`<success>`, `<error>`, `%player%`, etc.)
- All 7 message files now have 1029 keys (100% parity)

## Small features

- **Disguise feedback** — `/disguise mob ZOMBIE baby`, `/disguise mob VILLAGER LIBRARIAN`, `/disguise mob SLIME 3` now send user-facing success/error messages instead of silently swallowing invalid args
- **Stash withdraw-all feedback** — clicking the withdraw-all button now sends a success message with item count, or "stash empty" if nothing to withdraw; added `withdraw-all-button`/`text`/`lore` config keys to `modules/stash/config.yml`
- **Command aliases** — `craft`→`wb`/`workbench`, `enchanting`→`etable`/`enchanttable`, `heal`→`healme`, `anvil`→`av`
- **`/eat <player>`** — target argument (staff permission `essentials.eat.other`), `/eat *` for all players (permission `essentials.eat.all`), mirrors the `/feed` pattern
- **`/near <player>`** — specific target mode: shows distance + compass direction to one player (uses existing `getDirection` helper)

# 1.3.2.0

## Global chat deletion sync

- **Cross-server message deletion** — when a staff member deletes a chat message via `/chathistory <player> delete <index>`, the full `ChatMessageDTO` (UUID, content, timestamp) is broadcast to all other servers via the `zessentials:relay` channel (`deletechat` sub-channel)
- **Receive handler** — on receipt, the receiving server clears its `chatMessagesCache` for the target UUID, deletes the message from local storage (idempotent for shared databases), and notifies all online moderators with `CHAT_MESSAGE_DELETED`
- **Wire format** — `zessentials:deletechat` payload: `uuid|targetName|content|createdAtMillis`
- **NetworkManager** wired into `ChatModule` (init + listener registration in `loadConfiguration`)

# 1.3.1.0

## Disguise metadata polish — LibsDisguises-inspired

- **FlagWatcher hierarchy** — new `dev.yanianz.essentials.disguise.watcher` package with `MetaIndex`, `FlagWatcher` base class, and per-mob subclasses: `LivingWatcher`, `AgeableWatcher`, `InsentientWatcher`, `ZombieWatcher`, `VillagerWatcher`, `SlimeWatcher`, plus `MobWatcherFactory` that returns the right watcher for each `EntityType`
- **Baby variants** — `/disguise mob ZOMBIE baby` (also HUSK, DROWNED, ZOMBIE_VILLAGER, ZOMBIFIED_PIGLIN); `AgeableWatcher` for cow/pig/sheep/etc.
- **Villager professions** — `/disguise mob VILLAGER LIBRARIAN` sets profession; biome types (DESERT, JUNGLE, PLAINS, etc.) also supported; level configurable
- **Slime size** — `/disguise mob SLIME 3` sets size (1-50, clamped)
- **Mob-specific metadata** — `handleMetadata()` in `PacketMobDisguiseListener` now calls `watcher.buildWatcher()` to produce the full `WrappedDataWatcher` with both generic Entity indices (flags, name, health) AND mob-specific indices (zombie baby at 15, slime size at 16, villager data at 18, ageable age at 15)
- **Flicker-free refresh** — `refreshDisguise()` replaced `hidePlayer`/`showPlayer` (which caused visible flicker) with raw ProtocolLib packet batching: `ENTITY_DESTROY` + `SPAWN_ENTITY` + `ENTITY_METADATA` sent per viewer in one scheduled task — eliminates the gap between destroy and respawn
- **Fallback** — if no watcher is available, the old minimal metadata (flags + name + health) is used as fallback

# 1.3.0.0

## Cross-server storage migration

Migrated 9 features from per-module JSON files to IStorage (SQL + Mongo) so player data syncs across a proxy network.

- **IStorage interface** — 30+ new domain-specific method signatures (nicknames, disguises, chat preferences, reports, notes, reputation, enderchest, stash items, stash materials)
- **ReportDTO + NoteDTO** — new API record types for structured queries
- **SQL** — 9 new migrations + 9 repositories following the existing `sarah` pattern, registered in `SqlStorage`
- **Mongo** — 9 new `MongoRepository` subclasses, wired into `MongoRepositories`
- **JsonStorage** — `UnsupportedOperationException` stubs (use MYSQL/MONGO)
- **Complex objects** (disguises, chat preferences, reputation, enderchest, stash) stored as JSON strings via existing serializers (Gson + Base64 ItemStack encoding)
- **Auto-migration** — on startup, each module checks for legacy JSON files, bulk-inserts into the database, and renames to `.migrated`
- **Module updates** — all 9 modules switched from `Files.readString`/`Files.writeString` to `IStorage` calls; in-memory caches and data structures unchanged

Features migrated:
1. Nicknames — `getAllNicknames()` / `upsertNickname()` / `deleteNickname()`
2. Disguises — `getAllDisguises()` / `upsertDisguise()` / `deleteDisguise()` (JSON string)
3. Chat preferences — `getAllChatPreferences()` / `upsertChatPreference()` (JSON string)
4. Reports — `getReports()` / `upsertReport(ReportDTO)` / `deleteReport(int)`
5. Notes — `getNotes(UUID)` / `upsertNote(NoteDTO)` / `clearNotes(UUID)` (lazy per-player load)
6. Reputation — `getAllReputations()` / `upsertReputation()` (JSON string)
7. EnderChest — `getEnderChest()` / `upsertEnderChest()` (JSON string, per-player)
8. Stash items — `getItemStash()` / `upsertItemStash()` (JSON string, per-player)
9. Stash materials — `getMaterialStash()` / `upsertMaterialStash()` (JSON string, per-player)

# 1.2.2.0

## zMenu GUI — Hypixel bottom row polish

- **Crafting bottom row** — craft button at slot 48, red filler glass across 45-53 (except close at 49); `CraftingGuiItems.craftButton()` + `redFiller()` helpers; `crafting.yml` slots updated
- **Stash withdraw-all** — emerald button at slot 48 in the stash nav row, iterates all pages and moves items to player inventory with overflow drop; `namedItem` 3-arg overload added for lore support
- **Enderchest overview cleanup** — removed redundant back/close buttons (slots 48/50) that duplicated the decorations config; cleaned unused imports
- **Enderchest content fix** — reverted erroneous close/back placement at content slots 0/1 that caused the first two items of every page to be skipped (flatIndex off-by-two)

## Roadmap

- Velocity-native proxy plugin marked done (`:ProxyVelocity` module, commit 989c326d)
- Craft/enderchest zMenu GUI bottom-row TODO marked done

# 1.2.1.0

## Chat upgrade — LPC reference (https://github.com/ThePM2/LPC-Pro-Wiki)

- **`%prefix%` / `%suffix%` placeholders** — resolved through configurable PlaceholderAPI keys (`lpc.prefix-placeholder` / `lpc.suffix-placeholder`, LuckPerms by default); unresolved placeholders degrade to empty instead of leaking raw
- **`%name-color%` placeholder** — permission-based name colors (`lpc.name-colors`: red/gold/aqua/green defaults) with `default-name-color` fallback
- **Per-world chat formats** — new `world-chat-formats` section overrides group formats in listed worlds

## Folia threading + performance

- **Removed every raw `Bukkit.getScheduler()` usage** — StepModule step create/finish, public homes inventory, hologram event updates and kick now run on the player's region thread (`runAtEntity`); stash picker listener registration, warning escalation commands and the dependency auto-restart moved to Folia-safe schedulers (`runNextTick` / `Bukkit.getAsyncScheduler()` with daemon-thread fallback)
- **Entity-id lookup cache** — `PacketMobDisguiseListener` cached entity-id → UUID map (rebuilt every 5s or on miss) replaces the per-packet O(n) online-player scan for every `ENTITY_METADATA`/`ENTITY_EQUIPMENT` packet
- **SkinCache eviction** — expired entries are removed every 10 minutes instead of lingering until accessed (memory leak)

## zMenu GUI — Crafting & Enderchest rebuilt

- **Crafting GUI on zMenu** — new `ZESSENTIALS_CRAFT_INTERACTIVE` button owns every interactive slot of the Hypixel layout (3x3 grid, result, quick-craft, close); grid clicks implement vanilla semantics (place-all / place-one / pickup / merge / swap) through cursor handling; result computed through the shared recipe matcher; shift-click = craft multiple; `modules/crafting/crafting.yml` config-driven
- **Quick Craft** — quick-craft slot renders for players with `essentials.crafting.quickcraft`; clicking repeats craft-all until the grid is exhausted
- **Enderchest GUI on zMenu** — `ZESSENTIALS_ENDERCHEST_CONTENT` PaginateButton paginates the flat content (pages × 45) over the content slots with native `pagination_previous`/`pagination_next` buttons + `%openInventory.getPage%`/`%openInventory.getMaxPage%` indicator; every interaction writes through to `EnderChestData` (cross-page persistence); read-only enforced for `/endersee`
- **Enderchest overview on zMenu** — `ZESSENTIALS_ENDERCHEST_OVERVIEW` page selector with locked pages grayed out; page click opens the chest on that page
- **Per-player session state** — `CraftingSession` (grid + quick-craft permission) and `EnderChestSession` (target, page, read-only, visible/allowed pages)
- **Removed Bukkit GUI stack** — CraftingGui/Holder/Listener and EnderChestGui/Holder/Listener deleted; block right-click, `/craft`, `/enderchest`, `/endersee` and the ProtocolLib OPEN_WINDOW interception all open the zMenu inventories now

## Chat & identity bug fixes

- **Fixed nick never showing in chat** — `setNickname(uuid, null)` overwrote the display name back to the real name immediately after `applyDisguise` set the nickname; `setNickname` now respects an active disguise and the `/nick` apply path no longer calls it (the disguise map is the single source of truth)
- **Fixed tag rendering literally (`&⚔`)** — tag text from config with an orphan color prefix rendered raw in chat; `sanitizeTagText()` strips orphan ampersands/section signs, applied when saving and when resolving
- **Honest skin messages** — new `nick-set-no-skin` message instead of the misleading "with a matching skin" when the Mojang fetch fails; `disguise.fallback-own-skin` (default true) applies the player's own skin as fallback

## Bug fixes

- **Fixed** `MongoConfiguration` NPE on startup — `createInstanceFromMap` in `ZUtils` used `Number.class.isAssignableFrom()` which returns `false` for primitive types (`int.class`, `long.class`, etc.), so empty config maps caused `null` to be passed for primitive `int port`, triggering `NullPointerException` on unbox. Now handles all primitive types explicitly.

## Disguise system

- **Full disguise suite** — `/disguise <player>` changes name + skin at the packet level via ProtocolLib; `/disguise random` picks from config pool; `/disguise skin <texture> [signature]` sets custom skin; `/disguise mob <type>` transforms into a mob entity; `/disguise off` and `/undisguise [player]` remove disguise
- **Mob disguise** — `/disguise mob <type>` transforms the player into a mob (ZOMBIE, SKELETON, CREEPER, etc.) via `PacketMobDisguiseListener` which intercepts NAMED_ENTITY_SPAWN + ENTITY_METADATA packets to swap the entity type in-flight; 25 default allowed mobs configurable in `disguise.allowed-mobs`
- **Skin sources** — online players (instant, reads player profile), offline players (Mojang API async + 24h cache), custom texture strings, random pool
- **Packet interception** — `PacketDisguiseListener` intercepts PLAYER_INFO for name+skin; `PacketMobDisguiseListener` intercepts NAMED_ENTITY_SPAWN + ENTITY_METADATA for mob disguise; self-view config toggle
- **Persistence** — disguise state stored in `disguises.json`; auto-migrates from old `nicknames.json`; re-applied on join
- **Full sync** — NameTagModule uses DisguiseManager for scoreboard team entries, tab list name, and below-name objective; chat format uses `%displayName%` (nickname/disguise) for visible text and `%realname%` for click commands
- **`/realname`** — staff command to reveal the real name of a disguised/nicknamed player
- **Config v2** — `modules/nicknames/config.yml` with `disguise` section: enable, self-view, cooldown-seconds, skin-cache-hours, block-staff, random-pool, allowed-mobs
- **5 new permissions** — `ESSENTIALS_DISGUISE_USE`, `ESSENTIALS_DISGUISE_OTHER`, `ESSENTIALS_DISGUISE_RANDOM`, `ESSENTIALS_DISGUISE_SKIN`, `ESSENTIALS_DISGUISE_BYPASS_COOLDOWN`
- **12 new tests** — DisguiseData (5), SkinCache (7)

## Bug fixes

- **Fixed** Folia thread violations opening crafting/enderchest GUIs — `PacketCraftingListener` opened the custom crafting GUI directly from the ProtocolLib Netty packet thread (`Cannot init menu async`); the open is now scheduled on the player's region scheduler via `runAtEntity`. Same for the enderchest overview page click (was on the global scheduler)
- **Fixed** disguise and /nick never applying on Folia — the async Mojang skin fetch callbacks ran `applyDisguise` on the global scheduler thread; now scheduled on the target player's region thread
- **Network relay channel changed** from `BungeeCord` to `zessentials:relay` — the BungeeCord proxy drops unknown subchannels on its own channel, so cross-server relay never worked without a proxy plugin

## Proxy module (new)

- **`zEssentialsProxy`** — new standalone BungeeCord plugin jar (`target-proxy/zEssentials-proxy-<version>.jar`) that relays the `zessentials:relay` plugin channel between every server behind the proxy; enables `/gchat`, and later friends/guild/party messaging, across the network; Velocity legacy-compatible (relays plugin messages of registered channels)
- **Gradle `:Proxy` module** — `Proxy/` subproject with `bungeecord-api` (Maven Central), separate jar output
- **NetworkManager** — registers outgoing + incoming `zessentials:relay` channel; message format unchanged (`zessentials:<sub>` + UTF payload)

## Scoreboard & TabList — TAB parity (https://github.com/NEZNAMY/TAB)

- **`hidden-numbers`** — hides the score numbers on the right side of the scoreboard (TAB feature); implemented through FastBoard custom scores on 1.20.3+ servers, applied on board create and every update
- **`dynamic-lines`** — scoreboard lines whose placeholders resolve to empty text are removed entirely instead of shown as blanks (TAB dynamic lines)
- **`disabled-worlds`** — players in listed worlds get no scoreboard; board is removed/re-created on world change (TAB disable-in-worlds for scoreboards)
- **`per-world` scoreboard mapping** — force a specific scoreboard per world, overriding join/task conditions (TAB per-world scoreboards)
- **World change handling** — `PlayerChangedWorldEvent` re-evaluates the scoreboard: swaps to the world's board or removes it in disabled worlds
- **TabList `disable-in-worlds`** — worlds listed in tablist config get an empty header/footer instead of the default one (TAB disable-in-worlds)
- **API addition** — `EssentialsScoreboard.setDynamicLines(boolean)`; existing features (toggle /sb with persisted choice, animations, per-group/world header+footer, team sorting, nametags) already cover the rest of the TAB feature set

## Nick & Enderchest — Hypixel SkyBlock reference

- **Hypixel-style `/nick`** — `/nick` with no arguments now gives a random anonymous identity: random plausible username from `random-nick-pool` (20 defaults) + matching skin fetched from Mojang; identity applies everywhere (chat, tab, name tag, packet skin) via the disguise system; `/nick clear` removes it; `/nick <name>` now applies the matching skin too; `/nick random` re-rolls
- **`SkinFetcher`** — shared Mojang API helper (uuid lookup + textures fetch) extracted from CommandDisguise, reused by `/nick` with 24h SkinCache
- **Hypixel-style Ender Chest** — `/enderchest` now opens a page-selector overview (info icon, clickable page buttons with locked pages grayed out, close button) before the page view; page view nav row upgraded to Hypixel layout: `« First (45)`, `← Previous (46)`, indicator (48), Close (49), `Next → (52)`, `Last » (53)`; overview fully config-driven in `modules/enderchest/config.yml` `overview` section
- **EnderChestHolder** — carries overview state + allowed pages for the page selector

## Disguise system — LibsDisguises-style engine

- **Packet engine rewrite** — `PacketMobDisguiseListener` now covers 4 packet types: `SPAWN_ENTITY` (rewrite entity type in-flight via `EntityTypeModifier`), `ENTITY_METADATA` (replace watcher with mob defaults + colorized custom name above the mob), `ENTITY_EQUIPMENT` (cancelled for mob-disguised players — mobs don't wear player gear), `PLAYER_INFO` (mob-disguised players removed from tab list, `hide-from-tab` config, own entry always preserved)
- **Refresh system (LibsDisguises respawn pattern)** — `refreshDisguise()` runs a hide/show cycle per viewer so the server resends the full spawn sequence, which ProtocolLib listeners rewrite in-flight to the disguise; called on apply, remove, and 40 ticks after join for persisted disguises
- **Self-view (F5 third-person)** — destroys the player's own entity client-side and respawns it as the mob using the same entity id, so movement prediction keeps the fake mob in sync; restore on undisguise via PLAYER-type spawn packet
- **LibsDisguises-style commands** — bare mob names work (`/disguise zombie` → mob disguise, case-insensitive, validated against `allowed-mobs` config); `/disguise player <name>` and `/disguise player <target> <name>` (LD player-disguise syntax); tab completion includes all allowed mob types + off/random/skin/player/mob/list
- **`isValidMob()`** — rejects non-living entity types and PLAYER; respects the `allowed-mobs` allowlist (25 default mobs)
- **Root build.gradle.kts** — added `compileOnly("net.dmulloy2:ProtocolLib:5.4.0")` so the main module can send self-view packets directly

## Crafting & Enderchest — no vanilla fallback

- **Crafting table block** — right-click opens custom crafting GUI only; no vanilla workbench fallback when module disabled (shows "module disabled" message instead)
- **Enderchest block** — right-click opens custom enderchest GUI only; no vanilla enderchest fallback when module disabled
- **Sneak bypass** — holding sneak while right-clicking bypasses the custom GUI for vanilla behavior

## Chat & scoreboard fixes

- **Fixed** chat showing real name instead of nickname — `%player%` now resolves to nickname/disguise name for visible text, new `%realname%` placeholder for `/report` and `/msg` click commands
- **Fixed** NameTagModule NPE on null `fallbackTabFormat` — YAML loader overwrites field with null when config key missing; added null guard
- **Fixed** tab list and below-name objective not showing nickname — NameTagModule now checks both disguise name and nickname for `effectiveName`, uses it for tab name, team entry, and below-name score
- **Fixed** tab format rules with `%player%` placeholder not resolving to nickname

# 1.2.0.0

## Item Tooltips & Pricing

- **Real-time price tooltips** — sell/buy/NPC prices from shop plugins shown directly in inventory item tooltips via ProtocolLib packet interception
- **Multi-shop support** — separate hooks for RoyaleEconomy, EconomyShopGUI, QuickShop; PriceResolver aggregates all active providers and returns the best price
- **Packet-only modification** — actual server-side items are never modified; tooltips are injected in-transit only
- **Idempotent injection** — marker component prevents lore duplication on packet re-send
- **Per-player toggle** — `/pricing` command lets players turn price display on/off for themselves
- **Config-driven** — lore line templates, marker, toggle permission all in `modules/pricing/config.yml`

## Stash system

- **Item Stash** — personal storage for up to 720 non-stackable items (weapons, tools, rare drops) across 16 paginated pages of 45 slots
- **Material Stash** — unlimited stackable material storage (cobblestone, crops); displays one slot per distinct material with total count
- **Permission-based pages** — `essentials.stash.item.pages.<n>` grants additional Item Stash pages (up to `max-item-pages`)
- **Category picker** — `/stash` opens a GUI to choose between Item Stash and Material Stash; `/stash item` and `/stash material` open directly
- **Vanilla migration** — first open migrates non-stackable items from player inventory into Item Stash (config-toggleable)
- **JSON persistence** — lossless `serializeAsBytes()` + Base64 in `modules/stash/data/{items,materials}/<uuid>.json`
- **Config-driven** — titles, nav row, picker icons, migration toggle in `modules/stash/config.yml`

## Bug fixes

- **Fixed** TP effects not appearing for tpa/tp/tphere — the direct trigger was only in the countdown `teleport()` path; `teleportNow()` (the universal funnel) now calls `playDirectTeleport` so ALL teleport paths trigger the particle ring
- **Fixed** from/to ring spawning on the wrong region thread on Folia — both rings now scheduled via `runAtLocation` for correct region ownership
- **Fixed** DND not suppressing the ping sound — `PlayerPingDisplay` now also checks `CHAT_DND` (previously only `MentionDisplay` respected DND)
- **Fixed** belowname objective not showing on Folia — `belowNameObjective()` in `apply()` was not wrapped in `runNextTick`, silently failing on region threads

## Global chat relay

- **NetworkManager** — BungeeCord plugin messaging transport with sub-channel routing; reusable for friends/guild/party
- **GlobalChatModule** — cross-server public chat relay; messages from one server appear in all servers with configurable format
- **/g toggle** — per-player toggle to opt out of global chat relay
- **Config-driven format** — `%server%`, `%player%`, `%message%` placeholders
- **Fixed** `/reports` screen GUI not opening — `addRequirePlayerNameArg()` caused a `SYNTAX_ERROR` before `perform()` ran; changed to `addOptionalArg` so 0-arg `/reports` reaches the staff screen
- **Fixed** tab completion missing on 7 commands (`/notes`, `/poll`, `/reputation`, `/nick`, `/baltopgui`, `/warpgui`, `/reports`) — all now use `addOptionalArg(String, TabCompletion)` with meaningful suggestions
- **Fixed** tablist animations not working — `resolveAnimations` was a stub returning input unchanged; now resolves `%anim_<name>%` tokens to the current frame on each refresh tick
- **Fixed** `[pos]` chat keyword shadowed by a duplicate `CustomDisplay` entry that used unavailable PAPI placeholders — removed the duplicate, native `PositionDisplay` now handles `[pos]`
- **Fixed** `/xyz` clipboard copying plain text — now copies formatted colored coordinates
- **Fixed** `directStamps` in EffectsModule was a plain `HashMap` accessed from multiple threads — changed to `ConcurrentHashMap`
- **Fixed** `@NonLoadable` missing on static finals in `ChatCustomizationModule`, `NicknamesModule`, `ChatBubblesModule`, `TabListModule`, `ReportsModule` — eliminated "An error with loading field" console errors
- **Fixed** chat history delete and report tp/resolve click commands using `/essentials:` namespaced prefix that didn't resolve — now use direct paths
- **Fixed** freeze/unfreeze `Cannot register new team async` on Folia — scoreboard ops wrapped in `runNextTick`

## Friends system

- **Friend requests** — `/friend add <player>` sends a request; `/friend accept <player>` and `/friend decline <player>` respond
- **Friend list** — `/friend list` shows all current friends (UUIDs shown as short names when offline)
- **Remove friends** — `/friend remove <player>` drops the friendship on both sides
- **Request expiry** — pending requests auto-prune after the configured number of days (default 7)
- **Online notifications** — `/friend` listener notifies online friends when a player joins (Adventure Component)
- **Friend cap** — `max-friends` config (default 50); enforced in `sendRequest` before adding the pending request
- **Config-driven** — `modules/friends/config.yml`; `FriendStorage` POJO keeps the friend graph for clean unit testing

## Guild system

- **Guild create/disband** — /guild create <name> creates; /guild disband destroys (leader only)
- **Guild join/leave** — /guild join <id> | /guild leave
- **Guild info** — /guild info shows name and member count
- **Rank system** — MEMBER, OFFICER, LEADER hierarchy
- **Guild chat** — /gc <message> sends to all members (format in config)
- **Permission cap** — `max-members` in config (default 25)

## Party system

- **Create/disband** — /party create | /party disband (leader only)
- **Invite/leave** — /party invite <player> | /party leave
- **Leader transfer** — auto-transfer to longest-tenured member when leader leaves
- **Party chat** — /pc <message> sends to all party members
- **Max size** — configurable cap (default 8)
- **Auto-disband** — when last member leaves

## Polish pass

- **Fixed hex color rendering** — 17 files used a broken `colorize()` pattern (`text.replace("&", "§")`) that corrupted hex colors like `&#ff0000` into `§#ff0000` instead of the proper `§x§f§f§0§0§0§0` sequence. All replaced with `ColorUtil.sections()` which handles both legacy and hex codes correctly
- **Nicknames polish** — proper tab completion (off + online players), cooldown feedback message, validation error messages, confirmation messages; `validate()` now uses `PlainTextComponentSerializer` for proper plain text extraction
- **Vanish polish** — `COMMAND_VANISH_TOGGLED_OTHER` message for targeting other players (previously reused self message)
- **4 new NicknamesModule tests** — validate rejects too long / accepts valid / rejects invalid chars / cooldown window
- **Cleaned unused imports** — `Modifier`, `Locale` removed from NicknamesModule and ReputationModule

## Disguise system

- **Full disguise suite** — `/disguise <player>` changes name + skin at the packet level via ProtocolLib; `/disguise random` picks from config pool; `/disguise skin <texture> [signature]` sets custom skin; `/disguise off` and `/undisguise [player]` remove disguise
- **Skin sources** — online players (instant, reads player profile), offline players (Mojang API async + 24h cache), custom texture strings, random pool
- **Packet interception** — `PacketDisguiseListener` intercepts outgoing `PLAYER_INFO` packets, replaces `WrappedGameProfile` with disguise name + texture properties in-flight; self-view config toggle controls whether disguised player sees their own disguise
- **Persistence** — disguise state stored in `disguises.json`; auto-migrates from old `nicknames.json`; re-applied on join
- **Full sync** — NameTagModule uses `DisguiseManager.getDisplayName()` for scoreboard team entries; tablist displayName set to disguise name; chat name uses disguise name
- **Config v2** — `modules/nicknames/config.yml` bumped to config-version 2 with `disguise` section: enable, self-view, cooldown-seconds, skin-cache-hours, block-staff, random-pool
- **5 new permissions** — `ESSENTIALS_DISGUISE_USE`, `ESSENTIALS_DISGUISE_OTHER`, `ESSENTIALS_DISGUISE_RANDOM`, `ESSENTIALS_DISGUISE_SKIN`, `ESSENTIALS_DISGUISE_BYPASS_COOLDOWN`
- **12 new tests** — DisguiseData (5), SkinCache (7)

## Network/Social Layer

- **NetworkManager** — BungeeCord plugin messaging transport with sub-channel routing
- **Global chat** — cross-server public chat relay with configurable format
- **Friends** — friend requests, accept/decline, list, remove, online notifications
- **Guild** — create/disband/join/leave, rank system, guild chat, configurable max members
- **Party** — create/disband/invite/leave, auto leader transfer, party chat, configurable max size

## Chat v2

- **Mention system** with configurable title, action bar and boss bar notifications — each toggle independently in `mention-placeholder` config (title-enabled, actionbar-enabled, bossbar-enabled); DND suppresses all notification types; `%player%` placeholder in all texts
- **Quick replies** — typed shortcuts expand to full phrases (`:brb:` → "Be right back!", 6 defaults shipped)
- **Interactive player names** — when enabled, the sender name in chat gets hover text and click action (suggest `/msg` by default), fully config-driven
- **`[item]` display polish** — amount hidden when 1, `itemName()` fallback for 1.21+ custom item names, format updated

## TAB parity

- **Per-group header/footer** — permission-based group entries checked before world entries, first match wins
- **Team priority ladder** — `ladder-sort` config option: when true, each player gets a unique team combining group priority + player name for a fully ordered tab list
- **TAB slot layouts** — `layout.enable` + `layout.fixed-slots` config; `PacketTabLayoutListener` intercepts `PLAYER_INFO` packets and injects fake `PlayerInfoData` entries via ProtocolLib for custom slot arrangements

## CMILib parity

- **Advanced item editing** — `/itemglow` (toggle enchantment glint override), `/itemunbreakable` (toggle unbreakable), `/itemmodeldata <id|clear>` (set custom model data)
- **Kit claim button** in the preview screen — new `ZESSENTIALS_KIT_CLAIM` button type at slot 53
- **Anvil text input** — reusable `AnvilTextInput` utility opens a native anvil GUI, captures `getRenameText()`, calls a consumer callback

## Placeholders

- **Statistics placeholders** — 50+ placeholders for all Bukkit `Statistic` enum values (`%statistic_<name>%` and `%statistic_<name>_formatted%`), plus convenience aliases for playtime, jumps, deaths, kills, damage, distances

## Infrastructure

- **ProtocolLib auto-install** — added to `PluginDependencyResolver.REQUIRED`; auto-downloaded from Hangar (external URL fallback fix)
- **ProtocolLib hook activated** — `loadHooks()` now instantiates `PacketListener` and registers chat + tab layout packet listeners
- **JUnit 5 + Mockito test suite** — 18 API test classes + 3 root test classes, 856 tests; CI test workflows (build.yml test job, standalone test.yml, nightly test + artifact upload)
- **Gson test classpath fix** — `DiscordWebhookTest` no longer fails with `NoClassDefFoundError`

## Screens

- **`/homesgui`** — paginated ScreenFactory screen listing all homes with left-click teleport, right-click delete, per-home material, public/favorite indicators
- **`/kitsgui`** — paginated ScreenFactory screen listing all kits with cooldown status, left-click claim, right-click preview

## Small features

- `/rules` now opens the terms dialog screen when the terms module is enabled
- `/chatslowmode` tab completion suggests common values (0/3/5/10/30)

## UX polish

- **Command descriptions** — `/itemglow`, `/itemunbreakable`, `/itemmodeldata`, `/condense`, `/homesgui`, `/kitsgui`, `/warpgui`, `/baltopgui` now show proper descriptions instead of raw enum names
- **Tab completion** — `/baltop <page>`, `/chathistory <page>`, `/enchant <player>` now suggest meaningful values instead of empty completions
- **Empty-state feedback** — `/reports` with no open reports shows a message instead of opening an empty screen; `ScreenFactory.open()` shows a BARRIER "No entries" placeholder when the item list is empty
- **`/condense` feedback** — success and empty messages now use the Message enum (`COMMAND_CONDENSE_SUCCESS`, `COMMAND_CONDENSE_EMPTY`)
- **`/near` invalid radius** — non-numeric or out-of-range radius now falls back to the default with a feedback message instead of silent failure
- **`/trash` confirmation** — opening message added (`COMMAND_TRASH_OPENED`)
- **Duplicate `/condense` alias** — removed the duplicate registration that caused the "command already registered" warning
- **Config self-healing** — `config-version` key added to all 40+ module configs; `ConfigHealer` detects schema version mismatch and appends missing keys/sections while preserving user comments and existing values
- **Test coverage** — 5 new test classes: `ConfigHealerEdgeCaseTest` (8 tests), `ConfigVersionCoverageTest` (2 tests), `MessageCompletenessTest` (3 tests), `EconomyEdgeCaseTest` (5 tests), `ChatDisplayKeywordTest` (7 tests)

## Enderchest custom GUI

- **Custom paginated enderchest** (`/ec`, `/enderchest`) — Hypixel SkyBlock-style 54-slot GUI with bottom navigation row, replacing the vanilla 27-slot enderchest
- **Permission-based page count** — `essentials.enderchest.pages.2` and `.pages.3` grant 2/3 pages (90/135 usable slots); `default-pages` and `max-pages` config control defaults and caps
- **Vanilla migration** — first open copies existing vanilla enderchest contents into the new backing store and clears the vanilla chest to prevent duplication
- **Immediate sync** — every click syncs the affected slot back to the backing array; close saves to JSON; no item loss on crash/disconnect
- **Read-only `/endersee`** — viewing another player's enderchest cancels all clicks
- **Config-driven nav row** — filler material/color, prev/next/close buttons, page indicator text all configurable in `modules/enderchest/config.yml`
- **Self-healing config** — `config-version: 1` key included; `ConfigHealer` appends missing keys on load

### Post-review hardening

- **Fixed** migrated items lost on crash/reload between first open and first close — `getData` now persists to JSON immediately after vanilla migration (before the GUI opens), closing the data-loss window
- **Fixed** `dataCache` `HashMap` corruption on Folia — switched to `ConcurrentHashMap` (per-player region threads mutate the cache concurrently)
- **Fixed** `/endersee` silently migrating and clearing the target's vanilla enderchest — migration now only runs on the owner's own `/ec` path (`getData` takes a `migrate` flag; `/endersee` passes `false`)
- **Fixed** `EnderChestHolder.playerId` holding the viewer's UUID on `/endersee` — now set to the data owner's UUID so `onClose` always saves to the correct file
- **Switched** ItemStack persistence to lossless `serializeAsBytes()`/`deserializeBytes()` Base64 (was `serialize()`+Gson, version-shape-coupled); added `EnderChestSerializerTest` round-trip coverage (5 tests)
- **Fixed** `default-pages` not clamped to `max-pages` — misconfig could bypass the hard cap; now `Math.min(defaultPages, maxPages)` in `loadConfiguration`

## Database backends

- **Redis activated** — `server-type: REDIS` now activates the `RedisServer` for cross-server messaging (chat, kicks, cooldowns, private messages, player list sync); previously the activation code was commented out
- **Redis hook paper-api fixed** — updated from stale `1.21.5` to `26.2.build.119-stable` matching the root project
- **MongoDB storage backend** — `storage-type: MONGO` now supported as a full `IStorage` implementation with 25 MongoDB repositories mirroring the SQL Repository pattern
- **MongoDB driver** — `org.mongodb:mongodb-driver-sync:5.2.1` added to `plugin.yml` libraries (runtime download by Paper, not shaded)
- **MongoDB config** — `mongo-configuration` section in `config.yml` supports full URI or individual `host`/`port`/`user`/`password`/`database` fields

## Custom Crafting GUI + Quick Crafting

- **Custom crafting GUI** — replaces the vanilla 3x3 crafting table with a Hypixel SkyBlock-style 54-slot custom GUI (3x3 grid, result slot, player inventory, decorative fill)
- **Bukkit recipe matching** — reuses Bukkit's built-in recipe registry (shaped + shapeless), no custom recipe database needed
- **Quick Crafting** — VIP+ players (`essentials.crafting.quickcraft` permission) can craft as many items as possible in one click
- **Crafting table interception** — ProtocolLib intercepts `OPEN_WINDOW` packets and redirects to the custom GUI
- **Items return on close** — grid items return to player inventory on close, overflow drops at feet
- **Config-driven** — title, quick craft button material/text/lore, close button, filler color in `modules/crafting/config.yml`

# 1.1.0.0

- **Fixed** nicknames not showing in chat — the display name is now passed through the MiniMessage renderer which converts legacy and hex codes correctly
- **Fixed** tags not showing correctly in chat — tag text is no longer pre-converted to § format, the MiniMessage renderer handles the conversion
- **Fixed** chat history delete button using a namespaced command path that was not recognized — now uses the direct command path
- **Fixed** report alert teleport command using a namespaced path that was not recognized
- Added `/warn <player> <reason...>` command that was missing from the command registry
- **Fixed** teleport effects: the from/to ring now fires before the teleport executes instead of after

- Belowname PLACEHOLDER mode now updates scores on a configurable timer (`belowname.refresh-seconds`)

- Added **below name objective** (`modules/nametags/config.yml`): shows health or a PAPI placeholder below every player nametag
- Added **spectator fix**: players in spectator mode get a configurable gray tab name
- Added **tab list animations**: named frame lists in `modules/tablist/config.yml` cycle on the refresh timer, use `%anim_<name>%` inside header/footer lines

- Added a **nametags & tab sorting** module (`modules/nametags/config.yml`): scoreboard teams apply group prefixes/suffixes above the player head and order the tab list by group priority — the same mechanism the TAB plugin uses; every field supports hex, MiniMessage and PlaceholderAPI with the player context, an apply delay gives LuckPerms time to resolve

- Added `/eat` — fills hunger, saturation and stops burning instantly
- Added `/xyz` — copies your formatted coordinates to the clipboard with a clickable preview
- Added a **tab list** module (`modules/tablist/config.yml`): per world header and footer with PlaceholderAPI support and a configurable refresh interval, the first TAB-parity feature
- `/near <radius>` now accepts an optional radius argument between 1 and 200 blocks

- **Sleep acceleration** (`modules/sleep/config.yml`): when the configured percentage of a world players are in bed the night moves forward smoothly (`accelerate-ticks` per second) until dawn instead of skipping instantly; dawn and start broadcasts are configurable
- Reports for staff now open as a **screen** (`/reports`): each entry is clickable paper, left click resolves it and right click teleports to the reported player
- Chat bubbles: the newest message stays at the head while older ones are pushed upwards; bubbles follow the player exactly (passenger mount)
- Teleport effects are triggered directly on plugin teleports so tpa/rtp/warp/home always show them
- Fixed a list of hex color rendering spots (reputation broadcast, terms kick screen, nicknames) plus `[ec]` handling

## New modules & systems

- **Custom screens** (`modules/customscreens/config.yml`) — create your own inventory screens opened by a command (
  `/screen`); zMenu layout format with actions, patterns, pagination and PlaceholderAPI support, runtime registered
  commands removed safely on reload
- **Runtime dependency loader** (`dev.yanianz.essentials.dependency`, modeled after Intave) — detects classpath
  availability, caches in `libs/` (maven layout), downloads from Maven Central with SHA-256/SHA-1/MD5 verification and
  injects into the running classloader without restart; resolves the JDBC driver automatically before database connect
- **Auto plugin installer** — zMenu & PlaceholderAPI are no longer hard dependencies; PlaceholderAPI hot-installs from
  Hangar without any restart, zMenu stages from Modrinth (one restart required, Paper forbids hot-loading bootstrapper
  plugins); idempotent downloads, clean self-disable when impossible
- **Effects** (`modules/effects/config.yml`) — particle rings & sounds on teleports (tpa/warp/spawn/home/rtp/tp),
  gamemode changes, flight toggles, plus blessing sparkles on `/heal` and `/god`
- **Terms of service** (`modules/terms/config.yml`) — native **Mojang dialog screen** (chest fallback) with clickable
  accept/refuse buttons, chat+command lock until answered, timeout kick, persistent acceptances (
  `/terms accept|deny|reload|reset <player>`)
- **Chat games** (`modules/chatgames/config.yml`) — six types: math race, word scramble, fast typing, reverse word,
  trivia, hot letter; automatic random rounds, console reward commands, `/chatgames <type|stop|reload>`
- **Polls** — `/poll create <seconds> <question> | option 1 | option 2 ...`, clickable one-vote-per-player options, live
  percentage bars, winner announcement, `/poll stop`
- **Reputation** — `/rep <player>` (+1 with 24h cooldown per giver), `/reputation [player]`, persisted in json
- **Raid protection** — identical chat spam from several players inside a rolling window gets cancelled, moderators
  alerted once, configurable console actions
- **Warning escalation** — `warning-escalation` thresholds run console commands when a player reaches N warnings (
  default 3→1d ban, 5→30d); `/warnings <player>` lists stored warnings
- **Staff notes** — `/note add <player> <text>`, `/notes <player>`, `/notes clear <player>` persisted in json
- **Nicknames** — `/nick <name|off>` with colors, length/character validation, impersonation guard, cooldown, json
  persistence re-applied on join; staff: `/nick <player> <name|off>`
- **Reports** — `/report <player> <reason>` with cooldown; staff get clickable sound alerts; `/reports` lists open ones
  with resolve/teleport buttons
- **Chat customization** (`/chatcolor`, `/tags`) — color gui (16 colors + bold/italic), tag gui from config, preferences
  persist and render live in chat

## Screen framework

- Reusable `ScreenFactory` (`dev.yanianz.essentials.screens`): paginated list inventories, control row, slot-bound
  clicks, categories picker via `openCategorized`; public api for addons through `EssentialsScreens.get().factory()`
- New screens built on it: `/baltopgui [economy]` (paginated heads) and `/warpgui` (permission filtered warps)

## New commands

- `/tpaall` — send your teleport request to everyone online at once (`essentials.tpa.all`)
- `/list` — online player list excluding hidden vanished players (`essentials.list`)
- `/itemdb` — material, namespaced key, amount and stack size of your held item (`essentials.itemdb`)
- `/chatcolor`, `/tags`, `/chatgames`, `/poll`, `/rep`, `/reputation`, `/dnd [player]`, `/chatslowmode <seconds>`,
  `/warn`, `/warnings <player>`, `/note`, `/notes`, `/nick`, `/report`, `/reports`, `/baltopgui`, `/warpgui`

## Chat module v2

- `[inv]` / `[ender]` / `[pos]` display keywords with item hovers, copy-to-clipboard and click-to-suggest `/tp`
- Custom interactive keywords (config `chat-placeholders`) shipped with 14 defaults including health, food, store link,
  ip...
- `@mention` highlighting per viewer with notification sound for the target; hover/click built in
- Emoji shortcuts: ten defaults like `:heart:` replaced in messages (`emoji-shortcuts`)
- Slowmode: `/chatslowmode <seconds>` flat cooldown, bypass permission supported
- Do not disturb: `/dnd [player]` suppresses mention sounds
- Staff message deletion: clickable `[✖]` inside `/chathistory <player>` removes the stored message instantly
- Full MiniMessage rendering pipeline shared by every display

## Integrations

- **Discord bridge** (`modules/discordbridge/config.yml`, off by default) — outbound chat forwarding to the main Discord
  channel through DiscordSRV reflection, zero hard dependency
- **Network relay** (`modules/bungeechat/config.yml`, off by default) — public chat broadcast across BungeeCord/Velocity
  networks via plugin messaging with `%server% %player% %message%` format

## Fixes

- Scoreboard crash on modern Paper (`IllegalAccessException` from FastBoard) — upgraded FastBoard to 2.2.1 + implemented
  `customScoresSupported()`
- Chat crash when displaying an item (`NoSuchFieldError ClickEvent.Action.RUN_COMMAND`) — version-stable adventure
  factory
- Expired sanction cleanup failing on SQLite (`no such column: expired_at`) — now selects the sanctions table then
  clears references with an IN clause
- Effects module silently disabled: runtime fields are protected from the configuration loader (also restored missing
  tpa/rtp effects)
- Fly stuck after unfreeze — walk/fly speeds restored correctly; freezes are session-only by default (
  `freeze.persist-across-restarts`, default false), stale flags clean themselves on join
- Secure chat profile key warning — login checks moved from PlayerLoginEvent to AsyncPlayerPreLoginEvent so Paper keeps
  its configuration api available
- Duplicate message keys in the default configuration no longer print warnings on start
- Chat games auto task, poll close task and discord forward hop moved to the folia aware scheduler — region threaded servers no longer throw `UnsupportedOperationException`
- Notes module ships its missing `config.yml` resource that aborted the enable
- Login checks moved to AsyncPlayerPreLoginEvent so Paper keeps its profile public key available (secure chat warning fixed)
- Reports staff alert teleport command aligned with the registered handler
- Custom screens example layout now extracts from the jar on first launch
- `/r` answers with a clear message instead of failing silently when nothing was received

## Internal

- Raised the build to **paper-api 26.2** (adventure 5): ClickEvent payload/name migration, jetbrains annotations swap
- Duration arguments parse combinations everywhere: `30s 15m 12h 7d 2w 6mo 3y 1d12h30m` (plain numbers stay seconds)
- Economy amounts accept compact magnitudes: `1k`, `1.5m`, `2b`, `3t`
- Startup/shutdown console banners rendered as adventure components with an rgb gradient title
- Repetitive startup logs demoted to fine level for a cleaner console
- Main command renamed to `/essentials` (`/zessentials` kept as alias)

# 1.0.3.9

- **New configuration option**: enable the "First join teleport" in the `config.yml` file; it is now disabled by
  default.
- **Added** Faststats https://faststats.dev/project/zessentials/.
- **Changed** method signature in the configuration interface.
- **Fixed** MySQL error on create user home tables.
- **Fixed** Teleport world command permission.

# 1.0.3.8

- Added **text mails** to the mailbox module, to send a message to a player who is not connected (
  `modules/mailbox/config.yml`):
    - `/mail send <player> <message>` works with an online or offline player, `/mail read` displays the received mails
      and marks them as read, `/mail clearmessages [player]` deletes them and `/mail sendall <message>` sends a mail to
      every online player
    - A player who connects with unread mails is notified with a clickable message (`message-notify-on-join`,
      `message-notify-delay`)
    - Configurable limits: `message-max-amount` per player, `message-max-length`, `message-cooldown` between two mails (
      bypassed with `essentials.bypass.cooldown`) and `message-date-format`
    - A muted player cannot send a mail, and a player who used `/ignore` no longer receives the mails of the ignored
      player
    - Persistence via a new `user_mail_messages` table (SQLite and MySQL) and inside the user file for the JSON storage,
      unlike the item mailbox which stays MySQL only
    - New permissions `essentials.mail.send`, `essentials.mail.read`, `essentials.mail.send.all` and
      `essentials.mail.clear.messages`
- Fixed the build of the `NMS:V26_2` module: since Minecraft 26.1 the built in entity types are declared in
  `EntityTypes` and no longer in `EntityType`, so `EntityType.BLOCK_DISPLAY`, `ITEM_DISPLAY` and `TEXT_DISPLAY` could
  not be resolved anymore
- Fixed `shadowJar` failing with `Unsupported class file major version 69` — the `NMS:V26_2` module still builds with a
  Java 25 toolchain (needed to read the 26.2 dev bundle) but now emits Java 21 bytecode, which the ASM version bundled
  with the shadow plugin can remap
- Added a new **custom commands** module (`modules/customcommands/config.yml`) to create your own commands without any
  other plugin, for `/discord`, `/map`, `/vote`, `/store`, `/updates`...
    - Each command can define `aliases`, a `permission`, a `description`, a `cooldown` in seconds (bypassed with
      `essentials.bypass.cooldown`) and a list of `messages`
    - `type` selects how the content is displayed: `TCHAT`, `CENTER`, `ACTION`, `TITLE`, `BOSSBAR` or `NONE`
    - MiniMessage, legacy colors and PlaceholderAPI placeholders are supported, so `<click:open_url:'...'>` can be used
      to display clickable links
    - zMenu `actions` can be run after the messages (sound, command, inventory, ...)
    - Commands are registered at runtime and `/ezreload` updates them without duplicating anything; a custom command
      that would override an existing zEssentials command is refused with a message in the console
- Added display options for `/seen` in `modules/sanction/config.yml` — `seen-show-uuid`, `seen-show-ip`,
  `seen-show-last-location`, `seen-show-created-at` and `seen-show-playtime`. The IP address can now be hidden globally,
  even from operators: until now it was only protected by the `essentials.seen.show.ip` permission, which an operator
  always has
- Fixed several configuration options being silently ignored: they were declared as `private final` fields with a
  constant initializer, so javac inlined them at compile time and the value read from the configuration file was never
  used
    - Sanction module: `date-format`, `kick-default-reason`, `ban-default-reason`, `mute-default-reason`,
      `unmute-default-reason`, `unban-default-reason`
    - Spawn module: `respawn-listener-priority` and `spawn-join-listener-priority` — the respawn and join listeners were
      always registered with the `NORMAL` priority instead of the configured one (`HIGHEST` by default)
    - Worldedit module: `enable-color-visualisation` and `open-help-inventory`
- Clarified the `/compact` and `/compactall` descriptions to mention their existing `/condense` and `/condenseall`
  aliases
- Fixed the chat ping sound not playing on Paper 1.21.3+ — `org.bukkit.Sound` became an interface, so the ping sound is
  now resolved cross-version through the zMenu XSound API (like the teleportation sounds)
- Fixed countdown/teleport placeholders (`%name%`, `%seconds%`, ...) showing as raw text when the message `type` is set
  to `TITLE` or `BOSSBAR` — internal placeholders are now resolved for every message type
- Added `/pingsound` command (`/pingsounds` alias) to toggle the chat ping sound per player; the
  `enable-player-ping-sound` global toggle is now honored
- Added `/tp <player1> <player2>` — teleport one player to another player
- Added a player ignore system with persistence (`user_ignores` table):
    - `/ignore <player>` blocks a player's private messages and teleport requests (`/tpa`, `/tpahere`)
    - `/unignore <player>` and `/ignorelist` (`/ignores` alias)
    - Works for online and offline targets, persists across restarts (MySQL and JSON storage)
- Added `/delhome-other <player> <home>` (`/delhomeother`, `/hdelother` aliases) — admin command to delete a specific
  home of another player (online or offline), with permission `essentials.del.home.other`
- Added persistence for `/ptime` and `/pweather` — the per-player time and weather are now saved and re-applied
  automatically when the player reconnects
- Fixed private messages to a vanished player revealing their presence when they had ignored the sender — the vanish
  check now takes precedence over the ignore check
- Removed the non-functional `itemadders-font-regex` chat config options (the feature was never wired) and corrected the
  `/sc` reference in the chat config comment (it is `/chathistory`)
- Added a Homes system enhancement (see `modules/home/config.yml`):
    - **Public homes** — `/homepublic <home>` makes a home visitable by everyone, `/publichomes [player]` lists them (in
      chat or a paginated GUI via `public-homes-display: CHAT|INVENTORY`); visit with `/home <player>:<home>` (
      permissions `essentials.home.public`, `essentials.home.visit`, configurable `max-public-homes`)
    - **Shared homes** — `/homeshare <home> <player>`, `/homeunshare`, `/homeshares` to share a home with specific
      players (online or offline); shares are purged when the home is deleted (permission `essentials.home.share`,
      `max-shared-per-home`)
    - **Categories** — `/homecategory <home> <category>` to organise homes (permission `essentials.home.category`),
      placeholder `%category%`
    - **Favorites** — `/homefavorite <home>` to mark a home as favorite; `favorite-first` shows favorites at the top (
      permission `essentials.home.favorite`), placeholder `%favorite%`
    - **Preview** — optional `enable-home-preview` shows a clickable confirmation before teleporting
    - **Import** — `/homeimport essentialsx` imports homes from EssentialsX (permission `essentials.home.import`)
    - Persistence via new `is_public`/`category`/`is_favorite` columns on `user_homes` and a new `user_home_shares`
      table (MySQL and JSON storage)
- Updated zMenu to `1.1.1.6` and added support for Minecraft/Paper **26.2**:
    - Added a new `NMS:V26_2` module (built against the `26.2.build.+` dev bundle, compiled with Java 25 which Minecraft
      26.x requires)
    - Migrated the whole plugin to **Mojang mappings** — Paper 26.1+ removed Spigot reobfuscation, so every NMS module
      now uses `MOJANG_PRODUCTION` and the shaded jar is marked `paperweight-mappings-namespace: mojang`
    - **zEssentials is now Paper-only and requires Paper 1.20.5+ — Minecraft 1.20.4 is no longer supported** (
      Mojang-mapped plugins only load on 1.20.5+)
    - Replaced the removed zMenu `NmsVersion` enum with the new `MinecraftVersion` API for version detection and NMS
      package resolution (`NmsVersionUtils`)
    - Bumped `paperweight-userdev` to `2.0.0-beta.21`

# 1.0.3.7

- Added player list placeholders for retrieving online player information by index (1-based, sorted alphabetically,
  excludes vanished players):
    - `%zessentials_playerlist_count%` Returns the number of visible online players (excludes vanished)
    - `%zessentials_playerlist_<index>_name%` Returns the player's name at the given index
    - `%zessentials_playerlist_<index>_uuid%` Returns the player's UUID at the given index
    - `%zessentials_playerlist_<index>_ping%` Returns the player's ping in milliseconds
    - `%zessentials_playerlist_<index>_colored_ping%` Returns the player's ping with color based on quality
    - `%zessentials_playerlist_<index>_level%` Returns the player's experience level
    - `%zessentials_playerlist_<index>_health%` Returns the player's current health
    - `%zessentials_playerlist_<index>_max_health%` Returns the player's max health
    - `%zessentials_playerlist_<index>_food_level%` Returns the player's food level
    - `%zessentials_playerlist_<index>_gamemode%` Returns the player's game mode
    - `%zessentials_playerlist_<index>_world%` Returns the player's world name
    - `%zessentials_playerlist_<index>_x%` Returns the player's X coordinate
    - `%zessentials_playerlist_<index>_y%` Returns the player's Y coordinate
    - `%zessentials_playerlist_<index>_z%` Returns the player's Z coordinate
    - `%zessentials_playerlist_<index>_displayname%` Returns the player's display name
    - `%zessentials_playerlist_<index>_is_flying%` Returns true if the player is flying
    - `%zessentials_playerlist_<index>_is_op%` Returns true if the player is operator
    - `%zessentials_playerlist_<index>_is_sneaking%` Returns true if the player is sneaking
    - `%zessentials_playerlist_<index>_is_afk%` Returns true if the player is AFK

# 1.0.3.6

- Updated zMenu to version 1.1.1.2
- Updated Sarah to version 1.23
- Added generic Bukkit event-based permission checker for WorldEdit module — blocks in protected claims (HuskClaims,
  GriefPrevention, Lands, Towny, etc.) are now automatically skipped without needing a specific
  hook [#237](https://github.com/Maxlego08/zEssentials/issues/237)
- Added configurable sounds for teleportation countdown and completion (`countdown-sound` and `complete-sound` in
  `modules/teleportation/config.yml`), supports custom sounds via the zMenu XSound API
- Added warp lookup cache with O(1) HashMap for improved
  performance [#239](https://github.com/Maxlego08/zEssentials/pull/239)
- Fixed home deletion from donut GUI showing "The home ? does not exist." — `/delhome` now opens the confirmation GUI
  when `homeDeleteConfirm` is enabled
- Fixed cancelled TPA requests still being accepted — `/tpacancel` now properly removes the request from the target
  player's incoming requests
- Fixed vault admin command permission check bypassed for admin access
- Fixed chat URL pattern not matching URLs with special characters (`~`, `+`, `#`)
- Fixed chat link transform regex replacement error with special characters

# 1.0.3.5

- Added `/itemframe` command (`/iframe` alias), toggles visibility of the item frame you're looking at
- Added Death Message module (`modules/death_message/config.yml`):
    - Three modes: DISABLE (no messages), DEFAULT (vanilla), CUSTOM (configurable)
    - Support for player kills, vanilla mobs, and MythicMobs creatures
    - Custom messages per death cause (FALL, DROWNING, FIRE, LAVA, etc.)
    - Random message selection when multiple messages are configured
    - Placeholders: `%player%`, `%displayName%`, `%killer%`, `%mob%`, `%cause%`, `%weapon%`
    - Permission `essentials.silent.death` for silent deaths
    - `/deathmessage` command to toggle death message visibility (`/dm`, `/deathmsg` aliases)
- Added MythicMobs hook for custom mob death messages
- Added `/tptoggle` command to toggle receiving teleport
  requests [#226](https://github.com/Maxlego08/zEssentials/pull/226)
- Added TPA queue system - accept/deny all requests at once [#228](https://github.com/Maxlego08/zEssentials/pull/228)
- Added weapon display in death messages with hover event [#229](https://github.com/Maxlego08/zEssentials/pull/229)
- Fixed Discord pings from Minecraft chat - prevents @everyone and @here
  mentions [#227](https://github.com/Maxlego08/zEssentials/pull/227)
- Fixed first spawn not working reliably - now uses `hasPlayedBefore()` for accurate detection
- Added all missing messages in all language files (EN, FR, DE, ES, IT, NL)

# 1.0.3.4

- Changed `enable-cooldown-bypass` default value to `true` in `config.yml`
- Added PayToggle placeholders:
    - `%zessentials_user_is_pay_disabled%` Returns true if the player has disabled payments (paytoggle)
    - `%zessentials_user_pay_status%` Returns the configured placeholder text for pay status
- Added PayToggle placeholder configuration in `modules/economy/config.yml`:
    - `paytoggle-placeholder-enabled` - Text shown when pay is enabled
    - `paytoggle-placeholder-disabled` - Text shown when pay is disabled
- Fixed permission registration conflict when reloading - now removes existing permission before re-registering
- Fixed duplicate player name detection - Mojang API lookup now only runs in online mode to prevent issues on offline
  servers

# 1.0.3.3

- Updated zMenu to version 1.1.0.9
- Added `force-commands` option in `config.yml`, allows commands to work even if their module is disabled
- Added automatic messages module (`modules/automessage/config.yml`), broadcasts configurable announcements at intervals
  with sequential or random order
- Added RTP cooldown configuration with permission-based overrides in `command-cooldowns`
- Fixed duplicated users in database when a player changes their name
- Fixed `/endersee` error handling with proper error message (`COMMAND_ENDERSEE_ERROR`)
- Fixed `/endersee` offline permission check returning wrong result type
- Fixed `/home-list` admin command displaying wrong message when the player has no homes
- Fixed user lookup query to sort by `updated_at` for correct results with duplicate names
- Fixed build compatibility with Java 25 by applying `options.release = 21` to all subprojects
- Added 66 new placeholders across 3 categories:

### Nearest Player Placeholders

- `%zessentials_nearest_player_name%` Returns the name of the nearest visible player
- `%zessentials_nearest_player_distance%` Returns the distance to the nearest player
- `%zessentials_nearest_player_direction%` Returns the direction arrow (↑↗→↘↓↙←↖) to the nearest player

### Player Placeholders

- `%zessentials_player_health%` Returns the player's current health
- `%zessentials_player_max_health%` Returns the player's max health
- `%zessentials_player_health_rounded%` Returns the player's health rounded to nearest integer
- `%zessentials_player_absorption%` Returns the player's absorption hearts
- `%zessentials_player_food_level%` Returns the player's food level
- `%zessentials_player_saturation%` Returns the player's saturation level
- `%zessentials_player_exhaustion%` Returns the player's exhaustion level
- `%zessentials_player_level%` Returns the player's experience level
- `%zessentials_player_exp%` Returns the player's experience progress (0.0 to 1.0)
- `%zessentials_player_exp_percentage%` Returns the player's experience progress as percentage
- `%zessentials_player_total_exp%` Returns the player's total experience points
- `%zessentials_player_exp_to_level%` Returns the experience required for the next level
- `%zessentials_player_displayname%` Returns the player's display name
- `%zessentials_player_uuid%` Returns the player's UUID
- `%zessentials_player_locale%` Returns the player's client locale
- `%zessentials_player_client_brand%` Returns the player's client brand name
- `%zessentials_player_gamemode%` Returns the player's game mode
- `%zessentials_player_is_flying%` Returns true if the player is currently flying
- `%zessentials_player_allow_flight%` Returns true if the player is allowed to fly
- `%zessentials_player_is_sneaking%` Returns true if the player is sneaking
- `%zessentials_player_is_sprinting%` Returns true if the player is sprinting
- `%zessentials_player_is_sleeping%` Returns true if the player is sleeping
- `%zessentials_player_is_op%` Returns true if the player is operator
- `%zessentials_player_is_dead%` Returns true if the player is dead
- `%zessentials_player_is_swimming%` Returns true if the player is in water
- `%zessentials_player_is_blocking%` Returns true if the player is blocking with a shield
- `%zessentials_player_is_gliding%` Returns true if the player is gliding with elytra
- `%zessentials_player_ping%` Returns the player's ping in milliseconds
- `%zessentials_player_colored_ping%` Returns the player's ping with color based on quality
- `%zessentials_player_fly_speed%` Returns the player's fly speed
- `%zessentials_player_walk_speed%` Returns the player's walk speed
- `%zessentials_player_remaining_air%` Returns the player's remaining air in ticks
- `%zessentials_player_max_air%` Returns the player's maximum air in ticks
- `%zessentials_player_compass%` Returns the player's compass direction (N, NE, E, SE, S, SW, W, NW)
- `%zessentials_player_yaw%` Returns the player's yaw rotation
- `%zessentials_player_pitch%` Returns the player's pitch rotation
- `%zessentials_player_first_played%` Returns the date when the player first joined
- `%zessentials_player_last_played%` Returns the date when the player last joined
- `%zessentials_player_ticks_lived%` Returns the number of ticks the player has lived
- `%zessentials_player_empty_slots%` Returns the number of empty inventory slots
- `%zessentials_player_item_in_hand%` Returns the material type of the item in main hand
- `%zessentials_player_item_in_offhand%` Returns the material type of the item in off hand
- `%zessentials_player_world_time%` Returns the time of the player's world in ticks
- `%zessentials_player_world_time_12%` Returns the world time in 12-hour format
- `%zessentials_player_world_time_24%` Returns the world time in 24-hour format
- `%zessentials_player_world_weather%` Returns the weather of the player's world
- `%zessentials_player_has_bed%` Returns true if the player has a respawn location set
- `%zessentials_player_bed_world%` Returns the world name of the player's respawn location
- `%zessentials_player_bed_x%` Returns the X coordinate of the player's respawn location
- `%zessentials_player_bed_y%` Returns the Y coordinate of the player's respawn location
- `%zessentials_player_bed_z%` Returns the Z coordinate of the player's respawn location

### Server Placeholders

- `%zessentials_server_online%` Returns the number of online players
- `%zessentials_server_max_players%` Returns the maximum number of players
- `%zessentials_server_safe_online%` Returns the number of non-vanished online players
- `%zessentials_server_unique_joins%` Returns the total number of unique players
- `%zessentials_server_tps%` Returns the server TPS (1 minute average)
- `%zessentials_server_tps_5%` Returns the server TPS (5 minute average)
- `%zessentials_server_tps_15%` Returns the server TPS (15 minute average)
- `%zessentials_server_tps_colored%` Returns the server TPS with color indicator
- `%zessentials_server_free_memory%` Returns the free memory in MB
- `%zessentials_server_max_memory%` Returns the max memory in MB
- `%zessentials_server_used_memory%` Returns the used memory in MB
- `%zessentials_server_total_memory%` Returns the total allocated memory in MB
- `%zessentials_server_world_players_<world>%` Returns the number of players in a specific world
- `%zessentials_server_world_time_<world>%` Returns the time of a specific world in ticks
- `%zessentials_server_world_weather_<world>%` Returns the weather of a specific world

### User Placeholders

- `%zessentials_user_is_vanished%` Returns true if the player is vanished
- `%zessentials_user_is_frozen%` Returns true if the player is frozen
- `%zessentials_user_is_ban%` Returns true if the player is banned
- `%zessentials_user_ban_reason%` Returns the ban reason
- `%zessentials_user_ban_duration%` Returns the remaining ban duration in seconds
- `%zessentials_user_ban_duration_formatted%` Returns the remaining ban duration formatted
- `%zessentials_user_mute_reason%` Returns the mute reason
- `%zessentials_user_fly_formatted%` Returns the remaining fly time formatted
- `%zessentials_user_afk_duration%` Returns the AFK duration in seconds
- `%zessentials_user_afk_duration_formatted%` Returns the AFK duration formatted
- `%zessentials_user_home_list%` Returns a comma-separated list of home names
- `%zessentials_user_home_<index>%` Returns the home name by index (1-based)
- `%zessentials_user_home_<index>_<w/x/y/z>%` Returns the home location info by index
- `%zessentials_user_vote_offline%` Returns the number of offline votes
- `%zessentials_user_pm_recipient%` Returns the name of the last private message recipient

# 1.0.3.2

- Updated zMenu to version 1.1.0.8
- Added `/lightning` command to strike players with lightning
- Added `*` argument for `/heal` and `/feed` commands to target all players
- Added player name tab completion for `/heal` and `/feed` commands
- Added relative directions (north, south, etc.) to `/near` command
- Added smooth time transition for `/day` and `/night` commands
- `/feed` command now restores saturation
- `/ext` command can now target other players
- Fixed temporary fly when changing world through portals
- Fixed personal time from progressing

# 1.0.3.1

- Added NMS support for Minecraft 1.21.9, 1.21.10 and 1.21.11

# 1.0.3.0

- Fix some errors (npe and economy check)
- Added option for kits
- Added new default kits

# 1.0.2.9

- Added the `/vanish` command allowing players to become invisible to others.
- Added the `/eco reset-all <economy>` command allowing a full reset of a specific economy.
- Added the `/vault get <player> <vault id> <slot id> [<give item>]` command allowing retrieval of an item from a
  player’s vault.
- Added the `/vault delete <player> <vault id> <slot id>` command allowing deletion of an item from a player’s vault.
- Added the `/mail give-hand <player>` command allowing the player to give the item in their hand to another player.
- Added the `/mail giveall-hand <player>` command allowing the player to give the item in their hand to all players.
- Added the `/afk` command allowing players to set themselves as AFK.
- Added a message informing the sender when the recipient is AFK upon receiving a private message.
- Added the `%zessentials_user_is_afk%` placeholder returning whether a player is AFK (true or false).
- Added the `%zessentials_user_status%` placeholder returning the player’s status value defined in the configuration.
- Improved placeholder configuration.
- Fixed the Discord bot Gradle module.
- Fixed offline player handling, ensuring players who have never joined are properly created when receiving an action.
- Fixed the `/spawn <player>` command which now works without a cooldown.
- Fixed translation files.
- Fixed the vault system.
- Fixed vote party behavior.

# 1.0.2.8

- Update to Sarah 1.20. Added MARIADB support
- Added command ``/lag``, allow you to see the lag of the server
- Added command ``/flyspeed <speed>``, change player's fly speed
- Added command ``/walkspeed <speed>``, change player's walk speed
- Added command ``/vault info <player>``, display player's vault information
- Added command ``/vault show <player>``, open player's vault
- Added `command-restrictions`, Allows disabling commands in specific worlds or areas (cuboids)
- Fix vault item slot button
- Fix fly on world change
- Fix give command when the player is not specified
- Fix scoreboard module
- Improve performance

# 1.0.2.7

- Added support of 1.21.5, 1.21.6, 1.21.7 and 1.21.8 [#182](https://github.com/Maxlego08/zEssentials/issues/182)
- Added permission for each element in the ``/seen`` command [#160](https://github.com/Maxlego08/zEssentials/issues/160)
- Added teleport damage protection
- Added ``%zessentials_user_custom_balence_<economy>_<price format>%`` placeholder
- Added ``%zessentials_custom_formatted_number_<price format>%`` placeholder
- Added itemsadder support for economy font
- Added PlaceholderAPI support for private messages
- Added WayPoint helper, only for developers now
- Added ``/phantoms``, allow you to disable phantom for only you
- Fix placeholders errors
- Fix god command [#184](https://github.com/Maxlego08/zEssentials/issues/184)
- Fix kit module with armor slots
- Fix fly task if the player is in creative or spectator mode
- Fix discord webhook errors
- Fix error with death message if spawn location is not
  found [#167](https://github.com/Maxlego08/zEssentials/issues/167)
- Prevent teleport to coordinates outside world bounds [#186](https://github.com/Maxlego08/zEssentials/issues/186)

# 1.0.2.6

- Update to zMenu 1.1.0.0
- Added WorldGuard hook
- Fix error with scoreboards
- Fix hologram when world is not loaded

# 1.0.2.5

- Fix vault economy with player's name
- Fix discord webhook configuration
- Rework steps module. The information is more complete and accurate.
- Added NChat Hook
- Added default option value in config.yml

# 1.0.2.4

- Rework kit. A kit now has its own configuration file present in the modules/kits/kits doser.
- You can set a cooldown by permission with `permission-cooldowns`
- You can define a permission with ``permission``, by default the permission is : `essentials.kit.<kit name>`
- You can define the items that will be present in players' armor slots
- Updated kit documentation to include all necessary information.

# 1.0.2.3

- Added the ability to add color to sign text.
- Added `/experience take <player> <amount> <level/experience>`, Take experience to player.
- Added a message to tell the player account of flight time they have left.
- Added ``/kitgive <player> <kit>``.
- Added world back blacklist.
- Added NuVotifier hook.
- Fixed scoreboard in 1.21.5 [#172](https://github.com/Maxlego08/zEssentials/issues/172)
- Fixed offline give money.
- Fixed offline players name for tabulation.
- Fixed heal command when you have night vision.
- Update Hologram API (change `ZHologramLine` to `HologramLine`).
- Change command ``/cooldown <player>`` to `/cooldown show <player>`.
- Disable ``/invsee`` with offline players.

# 1.0.2.2

- Added [AFK](https://zessentials.groupez.dev/modules/afk) module.
- Added `/experience grand-random <player> <min> <max> <level/experience>`, Grant experience to player.
- Added first spawn location (`/setfirstspawn`) [#168](https://github.com/Maxlego08/zEssentials/issues/168)
- Added default reason for economy commands
- Added /w aliases for private message command
- Added ``%zessentials_armor_name_<armor slot>%``, Returns the name of the player’s armor, without the color
- Added default home icon configuration [#170](https://github.com/Maxlego08/zEssentials/issues/170)
- Added a message for ``/rtp`` command
- Fixed spigot attribute
- Fixed discord webhook
- Fixed default messages files

# 1.0.2.1

- Fixed Vault.
- Added `/home-list <player> [<home name>]`, Allows you to view a player’s home list.
- Added `/mailbox clear <player>`, Removes items from the players mailbox.
- Added the permission record when creating orders. This allows players not to access commands that do not have
  permission with the tab. This also allows plugins like LuckPerms to retrieve the plugin’s permissions list.
- Added a kit list to give players when they first log in.

# 1.0.2.0

- Fixed Vault implementation [#164](https://github.com/Maxlego08/zEssentials/issues/164)
- Added global database configuration (Allows you to have a single database configuration file for multiple plugins)
- Added item display transformation for holograms
- Added option type button. Allows toggling an option
- Fixed multi-line support for the scoreboard
- Added support for the HuskHome database for data conversion
- Moved modules into the modules package
- Added custom model data for open and closed vault items
- Fixed the Gradle project. This allows for proper use of NMS
- Fixed permission to teleport to a warp

# 1.0.1.9

- Added 1.21.4 NMS support
- Added [BlockTracker](https://modrinth.com/plugin/blocktracker) for Player WorldEdit.
- Added a cache system for updating certain data in batches. This greatly reduces the number of SQL queries executed..
- Added a list of UUID blacklist from your server. It will no longer be able to connect.
- Added newline support for scoreboard lines with ``\n``.
- Fixed command `/vault give`, using the correct value for the player name.
- Fixed loading of messages with placeholder replacement.
- Added the ``/pub`` command, allowing you to send a message to the chat.
- Added a reason for economy commands. A default value is added and can be configured.
- Added a history module for private messages between players.
- Added a module ``step`` which allows you to save player data (statistics and custom data). This allows you to analyze
  your players' behavior more effectively, such as how long it took them to reach a key milestone in your server.
- Fixed messages for time display. Removed the non-configurable space.
- Improved SQL queries for private message history, command history, chat history and transaction history. Using a
  single SQL query instead of multiple ones for the same action.
- Improved vote module configuration. You can define different rewards based on the number of player votes.

# 1.0.1.8

- Added a cache for the nicknames of offline players.
- Added an option to disable the list of offline player usernames in the completion tab for certain commands.
- Added `vault-slot-type`, allowing you to define how vault slots are counted.
- Added `teleport-at-spawn-on-join`, enabling player teleportation to spawn upon joining.
- Fixed placeholders in messages.
- Fixed the `/skull` command and added support for hexadecimal format.
- Fixed default vault slot assignment by permission.
- Fixed duplicate keys in power tools.
- Fixed the SQL table for player slots.
- Fixed the button to reset vault names.
- Fixed title messages.

# 1.0.1.7

- Added a bot discord. Se bot allows linking your account discord to your minecraft account. Download the bot
  here: https://groupez.dev/resources/zessentials-discord-bot.340
- Added command ``/link <code>``, allows linking your minecraft account
- Added command ``/unlink``, allows unlinking your minecraft account
- Added placeholder ``%zessentials_user_has_discord_linked%``
- Fixed locations that could not be loaded if the world loaded after zEssentials
- You can use placeholders in the join and quit message

# 1.0.1.6

- You are required to use java 21
- Added ``%zessentials_can_repair_all%`` placeholder, indicates whether the player can fix everything
- Added ``%zessentials_count_repair_all%`` placeholder, counting the items to be repaired
- Added ``/repairall [<player>]``
- Added ``/tpahere <player>`` [#103](https://github.com/Maxlego08/zEssentials/issues/103)
- Fixed commands that could not be used from the console
- Fixed docs files
- Fixed warps inventory
- `zessentials_iteminhand_amount%` Returns the amount of items in the main hand
- `zessentials_iteminhand_custommodeldata%` Returns the custom model data of the item in hand
- `zessentials_iteminhand_displayname%` Returns the display name of the item in hand
- `zessentials_iteminhand_durability%` Returns the amount of durability left of the item in hand
- `zessentials_iteminhand_enchantmentlevel_%` Returns the level of a specific enchantment on the item in hand
- `zessentials_iteminhand_enchantments%` Returns the enchantments of the item in hand with their level
- `zessentials_iteminhand_fire_resistant%` Returns true if the item in hand is fire resistant
- `zessentials_iteminhand_glint%` Returns true if the item in hand has the glint enchantment
- `zessentials_iteminhand_hasenchantment_%` Returns true if the item in hand has at least one enchantment
- `zessentials_iteminhand_hasitemflag_%` Returns true if the item in hand has a specific itemflag
- `zessentials_iteminhand_hide_tooltip%` Returns true if the item in hand has its tooltip hidden
- `zessentials_iteminhand_hide_unbreakable%` Returns true if the tooltip unbreakable of the item in hand is hidden
- `zessentials_iteminhand_itemflags%` Returns the itemflags of the item in hand
- `zessentials_iteminhand_lore%` Returns the lore of the item in hand
- `zessentials_iteminhand_maxdurability%` Returns the maximum durability of the item in hand
- `zessentials_iteminhand_maxstacksize%` Returns the max stack size of the item in hand
- `zessentials_iteminhand_rarity%` Returns the rarity of the item in hand
- `zessentials_iteminhand_realname%` Returns the formatted material name of the item in hand
- `zessentials_iteminhand_repaircost%` Returns the repair cost of the item in hand
- `zessentials_iteminhand_type%` Returns the material name of the item in hand
- `zessentials_iteminhand_unbreakable%` Returns true if the item in hand is unbreakable
- `%zessentials_user_world%` Returns the name of the world the player is currently in
- `%zessentials_user_x%` Returns the x coordinate of the player
- `%zessentials_user_y%` Returns the y coordinate of the player
- `%zessentials_user_z%` Returns the z coordinate of the player
- `%zessentials_user_biome%` Returns the biome of the player
- `%zessentials_user_block_x%` Returns the block x coordinate of the player
- `%zessentials_user_block_y%` Returns the block y coordinate of the player
- `%zessentials_user_block_z%` Returns the block z coordinate of the player
- `%zessentials_server_name%` Returns the server name
- `%zessentials_server_uptime%` Returns the server update in format day, hour, minutes and seconds
- `%zessentials_server_uptime_in_second%` Returns the server update in second
- `%zessentials_last_random_number_<player name>%` Returns the last random number generated for the player within the
  last hour
- `%zessentials_last_random_player%` Returns the last random player name online
- `%zessentials_random_number_<from>_<to>%` Returns a random number between the two given arguments
- `%zessentials_random_player%` Returns a random player name online

# 1.0.1.5

- Added ``/clearinventory [<player>]`` [#101](https://github.com/Maxlego08/zEssentials/issues/124)
- Improve economy module with offline players
- Fixed teleport command with relative coordinates [#142](https://github.com/Maxlego08/zEssentials/issues/142)
- Added a method in the API to retrieve player’s transaction history
- Added a reason for each transaction made by the player
- Fixed sql port [#144](https://github.com/Maxlego08/zEssentials/issues/144)
- Fixed rtp with folia [#138](https://github.com/Maxlego08/zEssentials/issues/138)
- Fixed kit permission
- Fixed method ``stringToDuration`` [#143](https://github.com/Maxlego08/zEssentials/pull/143)
- Fixed the cooldown command for commands that don’t come from
  zEssentials [#137](https://github.com/Maxlego08/zEssentials/pull/137)
- Fixed teleport request [#134](https://github.com/Maxlego08/zEssentials/pull/134)
- Added ``/suicide`` [#135](https://github.com/Maxlego08/zEssentials/pull/135)
- Move ``commands.md``, `placeholders.md` and `permissions.md` in docs folder
- Added ``/eco give <economy> <player> <min amount> <max amount>``
  command [#120](https://github.com/Maxlego08/zEssentials/pull/120)
- Fixed teleport command if player doesn't exit [#112](https://github.com/Maxlego08/zEssentials/pull/112)

# 1.0.1.4

- Fixed auto update task for hologram module
- Fixed autocompletion for cooldown commands
- Fixed the cooldown system that could be applied to commands even if an error occurred
- Fixed folia on player join [#124](https://github.com/Maxlego08/zEssentials/issues/124)
- Debug player first joins at spawn location [#125](https://github.com/Maxlego08/zEssentials/issues/125)

# 1.0.1.3

- Add global commands for VoteParty [#115](https://github.com/Maxlego08/zEssentials/issues/115)
- Changing the commands of the vote party by zMenu actions, **you must update your configuration**.
- Fixed the appearance of holograms in other worlds
- Added command ``/fly get <player>``
- Added command ``/fly info``
- Added command ``/ess delete-world <world>``, allows you to delete data related to a world
- Fixed delete home sql request [#119](https://github.com/Maxlego08/zEssentials/issues/119)
- Added permission ``essentials.fly.safelogin``, Players with this permission will automatically enter fly mode upon
  logging in if they are suspended in the air. [#117](https://github.com/Maxlego08/zEssentials/issues/117)
- If the player does not have the `essentials.speed` permission, the walk and fly speed will be reset to default values
- Tab completion for editing hologram and itemrename [#116](https://github.com/Maxlego08/zEssentials/issues/116)

# 1.0.1.2

- Added a temporary fly with the `/fly` command
- Added permission `essentials.fly.unlimited`, allows fly without time restriction
- Added permission `essentials.fly.other`, allows to activate the fly to another player
- Added command ``/fly add <player> <seconds>``
- Added command ``/fly remove <player> <seconds>``
- Added command ``/fly set <player> <seconds>``
- Fixed hologram despawning [#99](https://github.com/Maxlego08/zEssentials/issues/99)
- Fixed holograms are in every world [#100](https://github.com/Maxlego08/zEssentials/issues/100)
- Added auto update for holograms
- Added placeholder ``%zessentials_user_fly_seconds%``, returns the number of seconds for temporary fly
- Added default money when player join [#105](https://github.com/Maxlego08/zEssentials/issues/105)
- Fixed seen command [#102](https://github.com/Maxlego08/zEssentials/issues/102)
- Fixed teleportation delay glitch [#96](https://github.com/Maxlego08/zEssentials/issues/96)
- Fixed vault register when economy is disable [#95](https://github.com/Maxlego08/zEssentials/issues/95)
- Added disable fly in certain worlds [#91](https://github.com/Maxlego08/zEssentials/issues/91)

# 1.0.1.1

- Added ``/spawn <player>`` (Permission: `essentials.spawn.other`)
- Added checking if player is vanished for various commands
- Fixed message when the player leave server
- Added permission ``essentials.back.death``, the player must have this permission to return to the place of his death
- Fixed teleport task when player is offline [#92](https://github.com/Maxlego08/zEssentials/issues/92)
- Fixed command ``/heal <player>`` and `/feed <player>` if you use it in the
  console [#90](https://github.com/Maxlego08/zEssentials/issues/90)
- Added two types of home usage MAX and STACK [#84](https://github.com/Maxlego08/zEssentials/issues/84)
- Fixed home delete inventory [#89](https://github.com/Maxlego08/zEssentials/issues/89)
- Fixed item display when you try to display an empty item
- Fixed tabulation with no argument needed
- Fixed message when you create a home
- Fixed economy give all command from console [#79](https://github.com/Maxlego08/zEssentials/issues/79)
- Fixed error when you die in another world [#76](https://github.com/Maxlego08/zEssentials/issues/76)

# 1.0.1.0

- Fixed command cooldown if permission was not set
- Fixed the system of economy that did not work with offline players

# 1.0.0.9

- Added the ability to change default vault names
- Added NMS support for 1.21.1
- Fixed an SQL query for updating homes with SQLITE
- Fixed an SQL query for updating cooldown with SQLITE [#74](https://github.com/Maxlego08/zEssentials/issues/74)
- Fixed creating homes that executed an SQL query for no reason
- Added command ``/freeze <player>``
- Added a command to confirm the overwrite of an already existing home. You can disable this option in
  `modules/homes/config.yml`
- Added a command to confirm the deletion of an home. You can disable this option in `modules/homes/config.yml`
- Fixed commands ``/sethome <player>:<home name>`` and ``/delhome <player>:<home name>`` which did not work if the
  player was online.
- Fixed command ``/wtp`` with folia

# 1.0.0.8

- Added error exception when you try to load a home if the world doesn't
  exist [#67](https://github.com/Maxlego08/zEssentials/issues/67)
- Implementation of the method ``boolean hasMoney(OfflinePlayer player, Economy economy, BigDecimal amount)`` and
  `BigDecimal getBalance(OfflinePlayer player, Economy economy)` in
  `EconomyModule` [#66](https://github.com/Maxlego08/zEssentials/issues/66)
- Fix sarah migration
- Fixed invsee command [#72](https://github.com/Maxlego08/zEssentials/issues/72)

# 1.0.0.7

- Updated the command `/endersee` to be compatible with offline players, added permission `essentials.endersee.offline`
- Updated the command `/invsee` to be compatible with offline players, added permission `essentials.invsee.offline`
- Fixed error with loading data [#59](https://github.com/Maxlego08/zEssentials/issues/59)
- Fixed night vision [#56](https://github.com/Maxlego08/zEssentials/issues/56)
- Changed aliases for PlayerWorldEdit from `pw` to `pwe` [#58](https://github.com/Maxlego08/zEssentials/issues/58)

# 1.0.0.6

- Added the command ``/showitem <code>``, Allows you to see the item that the player has in his hand. This command is
  used with the chat placeholder `[item]` [#43](https://github.com/Maxlego08/zEssentials/issues/43)
- Added the command ``/money <player>``, Shows the money of other players.
- Fixed messages [#39](https://github.com/Maxlego08/zEssentials/issues/39)
- Fixed default configuration for economy [#38](https://github.com/Maxlego08/zEssentials/issues/38)
- Fixed bug with ``/tp`` command [#37](https://github.com/Maxlego08/zEssentials/issues/37)
- Fixed nightvision messages
- Add the feature to manage vaults slots with permissions

```yaml
vault-permissions:
  - permission: zessentials.vault.size.player
    slots: 45
  - permission: zessentials.vault.size.vip
    slots: 90
  - permission: zessentials.vault.size.admin
    slots: 500
```

- Fixed title message placeholders
- Fixed somes messages [#47](https://github.com/Maxlego08/zEssentials/issues/47)

# 1.0.0.5

- Added the command `/ess convert HuskHomes`, allows converting the database from HuskHomes to zEssentials.
- Added the command `/ess convert AxVaults`, allows converting the database from AxVaults to zEssentials.
- Added the command `/mailbox give <player> <item> [<amount>]`, Add an item to a player’s mailbox.
- Added the command `/mailbox giveall <player> <item> [<amount>]`, Add an item to online player’s mailbox.
- Added the command `/vault give <player> <item> [<amount>]`, Add an item to player’s vault.
- Fixed CMI convert with invalid location for homes
- Fixed the bug that allowed adding items in the mailbox even if the module is disabled.
- Fixed command /rules who are not using the correct module.
- Added the module `WorldEdit`, module allows players to have access to a **player worldedit**. They will be able to use
  the commands like `/pw set`, `/pw cut` to place or break blocks. Each block placed must be paid, by default 5$ per
  blocks. You can configure the item worldedit, the number of blocks that the player can change at the same time and
  many other things, more information [here](https://zessentials.groupez.dev/modules/worldedit).

# 1.0.0.4

- Added the command `/ess convert PlauerVaultX`, allows converting the database from PlayerVaultX to zEssentials.
- Added the command `/ess convert Sunlight`, allows converting the database from Sunlight to zEssentials.
- Added the command `/ess convert CoinsEngine`, allows converting the database from CoinsEngine to zEssentials.
- Items in player vault will be displayed as a single item.
- Fixed night vision messages
- Fixed duplicate lines [#33](https://github.com/Maxlego08/zEssentials/issues/33)
- Upgrade to [Sarah](https://github.com/Maxlego08/Sarah/) version 1.10
- Fixed issue where savings by default could not be removed [#34](https://github.com/Maxlego08/zEssentials/issues/34).
  You must update your configuration like this
  Before

````yaml
economies:
  money:
    display-name: Money
````

After

````yaml
economies:
  - name: money
    display-name: Money
````

- Fixed permissions for night vision and vault commands
- Added command ``/sudo`` [#36](https://github.com/Maxlego08/zEssentials/issues/36)

# 1.0.0.3

- Change ``AsyncPlayerPreLoginEvent`` to ``PlayerLoginEvent``
- Change /tp command for adding coordinate. You have now ``/tp <x> <y> <z> <yaw> <pitch>`` and
  ``/tp <player> <x> <y> <z> <yaw> <pitch>``
- Fixed various messages in multiple languages
- Fixed text hologram default text [#25](https://github.com/Maxlego08/zEssentials/issues/25)
- Added command ``/nightvision`` (`/nv`), Provides a night vision effect

# 1.0.0.2

- Added the command `/ess convert EssentialsX`, allows converting the database from EssentialsX to zEssentials.
  Documentation: https://zessentials.groupez.dev/getting-started/convert#essentialsx
- Fixed the scoreboard title not appearing
- Added `/homes` alias for `/home` command for default configurations
- Added `/enchant` command
- Added enchantments list with aliases
- Added dutch translation [#19](https://github.com/Maxlego08/zEssentials/pull/19)

# 1.0.0.1

- Added the command `/ess convert CMI`, allows converting the database from CMI to zEssentials.
  Documentation: https://zessentials.groupez.dev/getting-started/convert#cmi