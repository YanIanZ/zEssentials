# Disguise System Design

## Overview

Upgrade the existing NicknamesModule into a full disguise system. Players can change just their name (`/nick`) or fully disguise as another player — name + skin (`/disguise`). Skins are fetched from online players (instant), offline players (Mojang API async + cache), custom texture strings, or a random pool. All disguise state persists and syncs across NameTag, TabList, chat, and command feedback.

## Commands

| Command | Permission | Description |
|---|---|---|
| `/nick <nickname\|off>` | `ESSENTIALS_NICKNAMES_USE` | Change own display name (existing) |
| `/nick <player> <nickname\|off>` | `ESSENTIALS_NICKNAMES_OTHER` | Admin: change other's name (existing) |
| `/disguise <player>` | `ESSENTIALS_DISGUISE_USE` | Disguise as online player (name + skin) |
| `/disguise <target> <player>` | `ESSENTIALS_DISGUISE_OTHER` | Admin: disguise target as player |
| `/disguise random` | `ESSENTIALS_DISGUISE_RANDOM` | Random disguise from config pool |
| `/disguise skin <texture> [signature]` | `ESSENTIALS_DISGUISE_SKIN` | Custom skin from texture string |
| `/disguise off` | `ESSENTIALS_DISGUISE_USE` | Remove own disguise |
| `/disguise list` | `ESSENTIALS_DISGUISE_USE` | List random pool |
| `/undisguise [player]` | `ESSENTIALS_DISGUISE_USE` / `OTHER` | Remove disguise (self or admin) |

New permissions: `ESSENTIALS_DISGUISE_USE`, `ESSENTIALS_DISGUISE_OTHER`, `ESSENTIALS_DISGUISE_RANDOM`, `ESSENTIALS_DISGUISE_SKIN`, `ESSENTIALS_DISGUISE_BYPASS_COOLDOWN`.

## Data Model

### DisguiseData POJO

```java
public class DisguiseData {
    private UUID playerId;
    private String disguiseName;       // null = no name change
    private String textureValue;       // null = no skin change
    private String textureSignature;   // null = unsigned
    private long appliedAt;
    private boolean active;

    public boolean isFullDisguise() { return disguiseName != null && textureValue != null; }
    public boolean isNameOnly()     { return disguiseName != null && textureValue == null; }
    public boolean hasSkin()        { return textureValue != null; }
}
```

### Storage

Single file `disguises.json` (replaces `nicknames.json`). Migration: on load, read `nicknames.json` if it exists, convert entries to `DisguiseData` with `textureValue=null`, then write `disguises.json` and delete `nicknames.json`.

```json
{
  "entries": {
    "uuid-string": {
      "disguiseName": "&cSteve",
      "textureValue": "eyJ0ZXh0dXJlcyI6...",
      "textureSignature": null,
      "appliedAt": 1234567890,
      "active": true
    }
  }
}
```

### Skin Cache

`ConcurrentHashMap<UUID, CachedProfile>` with 24h TTL. `CachedProfile` stores the game profile name + texture properties (value + signature).

- **Online players:** read `player.getGameProfileProperties()` directly (instant, no API call).
- **Offline players:** fetch async from Mojang API:
  - `https://sessionserver.mojang.com/session/minecraft/profile/<uuid>?unsigned=false`
  - Parse JSON `properties[]` where `name == "textures"` → extract `value` + `signature`
  - Cache result in `SkinCache` with timestamp; expire after `skin-cache-hours`
- **Custom texture strings:** provided directly via `/disguise skin <texture> [signature]`, no API call.

## Packet Interception

### PacketDisguiseListener (Hooks/ProtocolLib)

Registers on `PacketType.Play.Server.PLAYER_INFO` at `ListenerPriority.HIGH` (lower than TabLayout's HIGHEST so tab layout can further modify).

#### onPacketSending (PLAYER_INFO)

For each `PlayerInfoData` in the packet:

1. Extract the `WrappedGameProfile` and UUID.
2. Check if UUID matches a disguised player via `DisguiseManager.getDisguise(uuid)`.
3. If disguised:
   - Build a new `WrappedGameProfile` with:
     - Same UUID
     - Disguise name (or real name if only skin disguise)
     - Texture properties from `DisguiseData` (if `hasSkin()`)
   - Build a new `PlayerInfoData` with the new profile + disguise display name component
   - Replace the entry in the list
4. **Self-view filter:** if `self-view: false` and the packet recipient IS the disguised player, skip replacement (player sees their real profile).

#### Refresh Mechanism

When a disguise is applied or removed, for all online viewers:

1. Send a `PLAYER_INFO` packet with action `REMOVE_PLAYER` for the disguised player's UUID.
2. Send a `PLAYER_INFO` packet with action `ADD_PLAYER` with the new (disguised or real) profile.
3. This forces client to re-render the 3D skin model + update tab list entry.

No `ENTITY_DESTROY` / `NAMED_ENTITY_SPAWN` cycle needed (avoids flicker). The PLAYER_INFO remove+re-add is sufficient for skin refresh in modern Minecraft.

No `ENTITY_METADATA` interception needed — name above head is handled by NameTagModule's scoreboard team, which is updated separately.

## Module Integration (Full Sync)

### DisguiseManager (inside NicknamesModule, exposed via module getter)

```java
public String getDisplayName(UUID uuid)        // disguise name or null
public String getDisplayName(Player player)    // same, convenience
public boolean isDisguised(UUID uuid)
public DisguiseData getDisguise(UUID uuid)
public void applyDisguise(Player player, DisguiseData data)
public void removeDisguise(UUID uuid)
```

### Integration Points

1. **NameTagModule** — `getPlayerDisplayName(player)` checks `DisguiseManager.getDisplayName()` first, falls back to real name. When disguise changes, trigger NameTagModule team prefix refresh.
2. **TabListModule** — already uses `player.displayName()` which we set. Skin in tab handled by packet listener.
3. **Chat** — `player.displayName()` is set to disguise name. Commands that use `player.getName()` for display switch to `DisguiseManager.getDisplayName()`.
4. **Join** — re-apply disguise on join: set displayName, trigger skin refresh via packet listener.
5. **Quit** — disguise persists (not removed on quit).

**Key principle:** all places that display a player name go through `DisguiseManager.getDisplayName()`, not `player.getName()` directly.

## Config

`modules/nicknames/config.yml`, bumped to `config-version: 2`:

```yaml
config-version: 2
enable: true

# Name settings (existing)
max-length: 16
regex: "^[a-zA-Z0-9_&#§xﬀ0-9a-f]{1,32}$"
block-impersonation: true
allow-colors: true
cooldown-seconds: 60

# Disguise settings (new)
disguise:
  enable: true
  self-view: false
  cooldown-seconds: 120
  skin-cache-hours: 24
  block-staff: false
  random-pool:
    - Notch
    - jeb_
    - Dinnerbone
```

## Files

### New files
- `src/main/java/dev/yanianz/essentials/disguise/DisguiseData.java` — POJO
- `src/main/java/dev/yanianz/essentials/disguise/SkinCache.java` — async Mojang API + cache
- `src/main/java/dev/yanianz/essentials/disguise/CommandDisguise.java` — /disguise command tree
- `src/main/java/dev/yanianz/essentials/disguise/CommandUnDisguise.java` — /undisguise
- `Hooks/ProtocolLib/src/main/java/fr/maxlego08/essentials/hooks/protocollib/PacketDisguiseListener.java` — packet interception
- `src/test/java/dev/yanianz/essentials/disguise/DisguiseDataTest.java`
- `src/test/java/dev/yanianz/essentials/disguise/SkinCacheTest.java`
- `src/test/java/dev/yanianz/essentials/disguise/DisguiseManagerTest.java`

### Modified files
- `src/main/java/dev/yanianz/essentials/nicknames/NicknamesModule.java` — add DisguiseManager logic, storage migration, disguise config parsing
- `src/main/java/dev/yanianz/essentials/nicknames/CommandNick.java` — use DisguiseManager for name changes
- `API/src/main/java/fr/maxlego08/essentials/api/messages/Message.java` — new disguise messages
- `API/src/main/java/fr/maxlego08/essentials/api/commands/Permission.java` — new disguise permissions
- `src/main/resources/messages/messages.yml` — new message entries
- `src/main/resources/modules/nicknames/config.yml` — bump to config-version 2, add disguise section
- `src/main/resources/plugin.yml` — register /disguise and /undisguise commands
- `Hooks/ProtocolLib/src/main/java/fr/maxlego08/essentials/hooks/protocollib/PacketListener.java` — register PacketDisguiseListener
- `src/main/java/dev/yanianz/essentials/nametags/NameTagModule.java` — use DisguiseManager.getDisplayName()

## Testing

| Test class | Tests |
|---|---|
| `DisguiseDataTest` | serialization round-trip, isFullDisguise/isNameOnly/hasSkin, null fields |
| `SkinCacheTest` | cache hit/miss/expire, mock HTTP fetch (mock URL connection) |
| `DisguiseManagerTest` | getDisplayName fallback, isDisguised, apply/remove disguise |
| `DisguiseValidationTest` | name validation reuse from NicknamesModule.validate() |

Target: 8-10 new tests.

## Invariants

- Disguise state persists across restarts (storage migration from nicknames.json handled).
- Self-view config toggle controls whether disguised player sees their own disguise.
- Skin cache has TTL; expired entries are re-fetched on next access.
- Name above head (NameTag) always matches disguise name when disguised.
- PLAYER_INFO packet replacement is transparent to other modules (TabLayout runs at HIGHEST, can further modify).
- `/nick` only changes name (textureValue stays null); `/disguise` can change name + skin.
- Removing a disguise restores real name + clears texture; triggers refresh for all viewers.
