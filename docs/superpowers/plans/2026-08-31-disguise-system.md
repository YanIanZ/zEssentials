# Disguise System Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Upgrade the existing NicknamesModule into a full disguise system where players can change their name (`/nick`) or fully disguise as another player — name + skin (`/disguise`), with persistence, skin cache, and packet-level skin replacement via ProtocolLib.

**Architecture:** DisguiseData POJO stores name + texture value/signature. SkinCache fetches offline player profiles from Mojang API async with TTL. PacketDisguiseListener intercepts outgoing PLAYER_INFO packets to replace the GameProfile in-flight. NicknamesModule gains DisguiseManager methods (getDisplayName, applyDisguise, removeDisguise). NameTagModule uses DisguiseManager for team name display.

**Tech Stack:** Java 21, Bukkit/Paper API, ProtocolLib (packet interception), Gson (JSON storage), Mockito/JUnit5 (testing)

**Spec:** `docs/superpowers/specs/2026-08-31-disguise-system-design.md`

## Global Constraints

- Java 21 bytecode target, Gradle Kotlin DSL build
- Build: `./gradlew build -x test --console=plain`
- Tests: `./gradlew test --console=plain --no-daemon`
- Working directory: `/Users/rheninxy/Sourby/zEssentials`
- No comments in code unless explicitly requested
- New feature logic goes under `dev.yanianz.essentials.disguise` package
- Module scaffold (NicknamesModule) stays in existing location
- ProtocolLib hook code goes in `Hooks/ProtocolLib/src/main/java/fr/maxlego08/essentials/hooks/protocollib/`
- Folia-safe: use FoliaLib schedulers, not raw `Bukkit.getScheduler()`
- MockMaker is `mock-maker-subclass` (in `src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker`)
- Test deps: JUnit5, Mockito 5.14.2, paper-api 26.2, adventure 4.20.0

---

## File Structure

### New files
| File | Responsibility |
|---|---|
| `src/main/java/dev/yanianz/essentials/disguise/DisguiseData.java` | POJO: name, texture value, signature, appliedAt, active |
| `src/main/java/dev/yanianz/essentials/disguise/SkinCache.java` | Async Mojang API fetch + TTL cache of GameProfile textures |
| `src/main/java/dev/yanianz/essentials/disguise/CommandDisguise.java` | `/disguise` command tree |
| `src/main/java/dev/yanianz/essentials/disguise/CommandUnDisguise.java` | `/undisguise` command |
| `Hooks/ProtocolLib/src/main/java/fr/maxlego08/essentials/hooks/protocollib/PacketDisguiseListener.java` | Intercepts PLAYER_INFO to replace GameProfile |
| `src/test/java/dev/yanianz/essentials/disguise/DisguiseDataTest.java` | DisguiseData unit tests |
| `src/test/java/dev/yanianz/essentials/disguise/SkinCacheTest.java` | SkinCache unit tests |

### Modified files
| File | Change |
|---|---|
| `src/main/java/dev/yanianz/essentials/nicknames/NicknamesModule.java` | Add disguise fields, storage migration, DisguiseManager API, config parsing |
| `API/src/main/java/fr/maxlego08/essentials/api/commands/Permission.java` | 5 new disguise permissions |
| `API/src/main/java/fr/maxlego08/essentials/api/messages/Message.java` | New disguise message entries |
| `src/main/resources/messages/messages.yml` | New message strings |
| `src/main/resources/modules/nicknames/config.yml` | Bump to config-version 2, add disguise section |
| `src/main/java/fr/maxlego08/essentials/commands/CommandLoader.java` | Register /disguise and /undisguise |
| `Hooks/ProtocolLib/src/main/java/fr/maxlego08/essentials/hooks/protocollib/PacketListener.java` | Register PacketDisguiseListener |
| `src/main/java/dev/yanianz/essentials/nametags/NameTagModule.java` | Use DisguiseManager.getDisplayName() in team entry |

---

### Task 1: DisguiseData POJO

**Files:**
- Create: `src/main/java/dev/yanianz/essentials/disguise/DisguiseData.java`
- Test: `src/test/java/dev/yanianz/essentials/disguise/DisguiseDataTest.java`

**Interfaces:**
- Produces: `DisguiseData` class with fields `playerId (UUID)`, `disguiseName (String)`, `textureValue (String)`, `textureSignature (String)`, `appliedAt (long)`, `active (boolean)`. Methods: `isFullDisguise()`, `isNameOnly()`, `hasSkin()`. Gson-serializable (no-arg constructor + getters/setters or public fields).

- [ ] **Step 1: Write the failing test**

```java
package dev.yanianz.essentials.disguise;

import com.google.gson.Gson;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DisguiseDataTest {

    private final Gson gson = new Gson();

    @Test
    @DisplayName("isFullDisguise returns true when name and texture are set")
    void testFullDisguise() {
        DisguiseData data = new DisguiseData();
        data.setPlayerId(UUID.randomUUID());
        data.setDisguiseName("Steve");
        data.setTextureValue("eyJ0ZXh0dXJlcyI6e319");
        data.setActive(true);
        assertTrue(data.isFullDisguise());
        assertFalse(data.isNameOnly());
        assertTrue(data.hasSkin());
    }

    @Test
    @DisplayName("isNameOnly returns true when name set but no texture")
    void testNameOnly() {
        DisguiseData data = new DisguiseData();
        data.setDisguiseName("&cSteve");
        data.setTextureValue(null);
        assertTrue(data.isNameOnly());
        assertFalse(data.isFullDisguise());
        assertFalse(data.hasSkin());
    }

    @Test
    @DisplayName("hasSkin returns true when texture set without name")
    void testSkinOnly() {
        DisguiseData data = new DisguiseData();
        data.setDisguiseName(null);
        data.setTextureValue("eyJ0ZXh0dXJlcyI6e319");
        data.setTextureSignature("sig123");
        assertTrue(data.hasSkin());
        assertFalse(data.isFullDisguise());
        assertFalse(data.isNameOnly());
    }

    @Test
    @DisplayName("Gson serialization round-trip preserves all fields")
    void testSerializationRoundTrip() {
        DisguiseData original = new DisguiseData();
        original.setPlayerId(UUID.fromString("12345678-1234-1234-1234-123456789012"));
        original.setDisguiseName("Notch");
        original.setTextureValue("texture-value");
        original.setTextureSignature("signature");
        original.setAppliedAt(1234567890L);
        original.setActive(true);

        String json = gson.toJson(original);
        DisguiseData restored = gson.fromJson(json, DisguiseData.class);

        assertEquals(original.getPlayerId(), restored.getPlayerId());
        assertEquals(original.getDisguiseName(), restored.getDisguiseName());
        assertEquals(original.getTextureValue(), restored.getTextureValue());
        assertEquals(original.getTextureSignature(), restored.getTextureSignature());
        assertEquals(original.getAppliedAt(), restored.getAppliedAt());
        assertTrue(restored.isActive());
    }

    @Test
    @DisplayName("Empty data has no disguise")
    void testEmpty() {
        DisguiseData data = new DisguiseData();
        assertFalse(data.isFullDisguise());
        assertFalse(data.isNameOnly());
        assertFalse(data.hasSkin());
        assertFalse(data.isActive());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "dev.yanianz.essentials.disguise.DisguiseDataTest" --console=plain --no-daemon`
Expected: FAIL — `DisguiseData` class not found

- [ ] **Step 3: Write minimal implementation**

```java
package dev.yanianz.essentials.disguise;

import java.util.UUID;

public class DisguiseData {

    private UUID playerId;
    private String disguiseName;
    private String textureValue;
    private String textureSignature;
    private long appliedAt;
    private boolean active;

    public UUID getPlayerId() { return playerId; }
    public void setPlayerId(UUID playerId) { this.playerId = playerId; }

    public String getDisguiseName() { return disguiseName; }
    public void setDisguiseName(String disguiseName) { this.disguiseName = disguiseName; }

    public String getTextureValue() { return textureValue; }
    public void setTextureValue(String textureValue) { this.textureValue = textureValue; }

    public String getTextureSignature() { return textureSignature; }
    public void setTextureSignature(String textureSignature) { this.textureSignature = textureSignature; }

    public long getAppliedAt() { return appliedAt; }
    public void setAppliedAt(long appliedAt) { this.appliedAt = appliedAt; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public boolean isFullDisguise() { return disguiseName != null && textureValue != null; }
    public boolean isNameOnly() { return disguiseName != null && textureValue == null; }
    public boolean hasSkin() { return textureValue != null; }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "dev.yanianz.essentials.disguise.DisguiseDataTest" --console=plain --no-daemon`
Expected: PASS — 5 tests

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat(disguise): DisguiseData POJO + 5 tests"
```

---

### Task 2: SkinCache

**Files:**
- Create: `src/main/java/dev/yanianz/essentials/disguise/SkinCache.java`
- Test: `src/test/java/dev/yanianz/essentials/disguise/SkinCacheTest.java`

**Interfaces:**
- Produces: `SkinCache` class with:
  - `SkinCache(long cacheTtlMillis)` constructor
  - `CachedProfile getCached(UUID uuid)` — returns cached entry or null (never blocks)
  - `void put(UUID uuid, String name, String textureValue, String textureSignature)` — stores in cache
  - `boolean isExpired(UUID uuid)` — true if entry missing or TTL exceeded
  - `void clear()` — clears all entries
  - `void clear(UUID uuid)` — clears one entry
  - `int size()` — cache size for testing
- `CachedProfile` inner class with fields: `name (String)`, `textureValue (String)`, `textureSignature (String)`, `cachedAt (long)`

The async Mojang API fetch is a separate method `fetchProfileAsync` that the NicknamesModule calls. SkinCache itself is purely synchronous (in-memory cache), making it unit-testable without HTTP.

- [ ] **Step 1: Write the failing test**

```java
package dev.yanianz.essentials.disguise;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SkinCacheTest {

    private SkinCache cache;

    @BeforeEach
    void setUp() {
        cache = new SkinCache(1000L);
    }

    @Test
    @DisplayName("getCached returns null for unknown uuid")
    void testUnknownUuid() {
        assertNull(cache.getCached(UUID.randomUUID()));
    }

    @Test
    @DisplayName("put then getCached returns the profile")
    void testPutAndGet() {
        UUID uuid = UUID.randomUUID();
        cache.put(uuid, "Notch", "texture-value", "signature");
        SkinCache.CachedProfile profile = cache.getCached(uuid);
        assertNotNull(profile);
        assertEquals("Notch", profile.name());
        assertEquals("texture-value", profile.textureValue());
        assertEquals("signature", profile.textureSignature());
    }

    @Test
    @DisplayName("isExpired returns true for unknown uuid")
    void testExpiredUnknown() {
        assertTrue(cache.isExpired(UUID.randomUUID()));
    }

    @Test
    @DisplayName("isExpired returns false for fresh entry, true after TTL")
    void testExpiry() throws InterruptedException {
        UUID uuid = UUID.randomUUID();
        cache.put(uuid, "Steve", "tex", null);
        assertFalse(cache.isExpired(uuid));
        Thread.sleep(1100);
        assertTrue(cache.isExpired(uuid));
    }

    @Test
    @DisplayName("clear removes all entries")
    void testClearAll() {
        cache.put(UUID.randomUUID(), "A", "t1", null);
        cache.put(UUID.randomUUID(), "B", "t2", null);
        assertEquals(2, cache.size());
        cache.clear();
        assertEquals(0, cache.size());
    }

    @Test
    @DisplayName("clear(uuid) removes one entry")
    void testClearOne() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        cache.put(a, "A", "t1", null);
        cache.put(b, "B", "t2", null);
        cache.clear(a);
        assertNull(cache.getCached(a));
        assertNotNull(cache.getCached(b));
    }

    @Test
    @DisplayName("expired entries are not returned by getCached")
    void testExpiredNotReturned() throws InterruptedException {
        UUID uuid = UUID.randomUUID();
        cache.put(uuid, "Steve", "tex", null);
        Thread.sleep(1100);
        assertNull(cache.getCached(uuid));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "dev.yanianz.essentials.disguise.SkinCacheTest" --console=plain --no-daemon`
Expected: FAIL — `SkinCache` class not found

- [ ] **Step 3: Write minimal implementation**

```java
package dev.yanianz.essentials.disguise;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SkinCache {

    private final long cacheTtlMillis;
    private final ConcurrentHashMap<UUID, CachedProfile> cache = new ConcurrentHashMap<>();

    public SkinCache(long cacheTtlMillis) {
        this.cacheTtlMillis = cacheTtlMillis;
    }

    public CachedProfile getCached(UUID uuid) {
        CachedProfile profile = this.cache.get(uuid);
        if (profile == null) return null;
        if (isExpired(uuid)) return null;
        return profile;
    }

    public void put(UUID uuid, String name, String textureValue, String textureSignature) {
        this.cache.put(uuid, new CachedProfile(name, textureValue, textureSignature, System.currentTimeMillis()));
    }

    public boolean isExpired(UUID uuid) {
        CachedProfile profile = this.cache.get(uuid);
        if (profile == null) return true;
        return System.currentTimeMillis() - profile.cachedAt() > this.cacheTtlMillis;
    }

    public void clear() {
        this.cache.clear();
    }

    public void clear(UUID uuid) {
        this.cache.remove(uuid);
    }

    public int size() {
        return this.cache.size();
    }

    public record CachedProfile(String name, String textureValue, String textureSignature, long cachedAt) {}
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "dev.yanianz.essentials.disguise.SkinCacheTest" --console=plain --no-daemon`
Expected: PASS — 7 tests

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat(disguise): SkinCache with TTL + 7 tests"
```

---

### Task 3: Permissions and Messages

**Files:**
- Modify: `API/src/main/java/fr/maxlego08/essentials/api/commands/Permission.java` — add after line 223 (`ESSENTIALS_NICKNAMES_BYPASS_COOLDOWN`)
- Modify: `API/src/main/java/fr/maxlego08/essentials/api/messages/Message.java` — add after line 786 (`NICK_USAGE`)
- Modify: `src/main/resources/messages/messages.yml` — add after line 830 (`nick-usage`)

**Interfaces:**
- Produces: 5 new `Permission` enum entries and ~12 new `Message` enum entries + corresponding `messages.yml` strings

- [ ] **Step 1: Add permissions to Permission.java**

In `API/src/main/java/fr/maxlego08/essentials/api/commands/Permission.java`, after line 223 (`ESSENTIALS_NICKNAMES_BYPASS_COOLDOWN("Bypass the nickname cooldown"),`), add:

```java
    ESSENTIALS_DISGUISE_USE("Disguise as another player"),
    ESSENTIALS_DISGUISE_OTHER("Disguise another player"),
    ESSENTIALS_DISGUISE_RANDOM("Use random disguise"),
    ESSENTIALS_DISGUISE_SKIN("Set a custom skin disguise"),
    ESSENTIALS_DISGUISE_BYPASS_COOLDOWN("Bypass the disguise cooldown"),
```

- [ ] **Step 2: Add messages to Message.java**

In `API/src/main/java/fr/maxlego08/essentials/api/messages/Message.java`, after line 786 (`NICK_USAGE(...)`), add:

```java
    DISGUISE_SET("<success>You are now disguised as &f%disguise%<success>."),
    DISGUISE_SET_OTHER("<success>You disguised &f%player% <success>as &f%disguise%<success>."),
    DISGUISE_REMOVED("<success>Your disguise has been removed."),
    DISGUISE_REMOVED_OTHER("<success>Disguise of &f%player% <success>has been removed."),
    DISGUISE_NOT_DISGUISED("<error>You are not currently disguised."),
    DISGUISE_COOLDOWN("<error>Wait &f%seconds%s <error>before disguising again."),
    DISGUISE_PLAYER_NOT_FOUND("<error>Player &f%player% <error>not found."),
    DISGUISE_SKIN_SET("<success>Your skin has been changed."),
    DISGUISE_SKIN_SET_OTHER("<success>Skin of &f%player% <success>has been changed."),
    DISGUISE_RANDOM("<success>You have been disguised as a random player&7: &f%disguise%<success>."),
    DISGUISE_RANDOM_EMPTY("<error>The random disguise pool is empty."),
    DISGUISE_LIST_HEADER("<7>Available random disguises&8:"),
    DISGUISE_LIST_ENTRY("<8> - &f%name%"),
    DISGUISE_FETCHING("<7>Fetching skin from Mojang&8..."),
    DISGUISE_FETCH_FAILED("<error>Failed to fetch skin for &f%player%<error>. Please try again later."),
    DISGUISE_USAGE("<error>Usage&7: &f/disguise <player|random|skin <texture>|off>"),
    DISGUISE_DISABLED("<error>The disguise system is disabled."),
```

- [ ] **Step 3: Add message strings to messages.yml**

In `src/main/resources/messages/messages.yml`, after line 830 (`nick-usage: ...`), add:

```yaml
disguise-set: "<success>You are now disguised as &f%disguise%<success>."
disguise-set-other: "<success>You disguised &f%player% <success>as &f%disguise%<success>."
disguise-removed: "<success>Your disguise has been removed."
disguise-removed-other: "<success>Disguise of &f%player% <success>has been removed."
disguise-not-disguised: "<error>You are not currently disguised."
disguise-cooldown: "<error>Wait &f%seconds%s <error>before disguising again."
disguise-player-not-found: "<error>Player &f%player% <error>not found."
disguise-skin-set: "<success>Your skin has been changed."
disguise-skin-set-other: "<success>Skin of &f%player% <success>has been changed."
disguise-random: "<success>You have been disguised as a random player&7: &f%disguise%<success>."
disguise-random-empty: "<error>The random disguise pool is empty."
disguise-list-header: "<7>Available random disguises&8:"
disguise-list-entry: "<8> - &f%name%"
disguise-fetching: "<7>Fetching skin from Mojang&8..."
disguise-fetch-failed: "<error>Failed to fetch skin for &f%player%<error>. Please try again later."
disguise-usage: "<error>Usage&7: &f/disguise <player|random|skin <texture>|off>"
disguise-disabled: "<error>The disguise system is disabled."
description-disguise: "Disguise as another player"
description-undisguise: "Remove your disguise"
```

Also add these `DESCRIPTION_DISGUISE` and `DESCRIPTION_UNDISGUISE` entries to `Message.java` near the `DESCRIPTION_NICK` entry:

```java
    DESCRIPTION_DISGUISE("Disguise as another player"),
    DESCRIPTION_UNDISGUISE("Remove your disguise"),
```

- [ ] **Step 4: Build to verify compilation**

Run: `./gradlew build -x test --console=plain`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat(disguise): permissions + messages + descriptions"
```

---

### Task 4: Config Bump

**Files:**
- Modify: `src/main/resources/modules/nicknames/config.yml`

**Interfaces:**
- Produces: config-version 2 with disguise section containing: `enable`, `self-view`, `cooldown-seconds`, `skin-cache-hours`, `block-staff`, `random-pool` (list of names)

- [ ] **Step 1: Update config.yml**

Replace the entire content of `src/main/resources/modules/nicknames/config.yml` with:

```yaml
########################################################################################################################
#
# zEssentials - Nicknames & Disguise
# Players change their own display name, staff can change it for anyone.
# Disguise changes name + skin at the packet level via ProtocolLib.
#
########################################################################################################################

# Config schema version — do not edit
config-version: 2

enable: true

# Maximum length of a nickname
max-length: 16

# Allowed characters of a nickname
regex: "^[a-zA-Z0-9_&#§xﬀ0-9a-f]{1,32}$"

# Block nicknames equal or similar to the name of another player (impersonation)
block-impersonation: true

# Colors players can use without a special permission.
# When false the nickname is stripped from every color code unless
# the player has essentials.nicknames.color
allow-colors: true

cooldown-seconds: 60

# Disguise settings — changes name AND skin at the packet level
disguise:
  # Master toggle for the disguise system
  enable: true

  # Whether the disguised player sees their own disguise in F5/third-person view
  self-view: false

  # Cooldown between disguise changes (seconds)
  cooldown-seconds: 120

  # How long to cache fetched skins from the Mojang API (hours)
  skin-cache-hours: 24

  # Block disguising as staff members (players with essentials.disguise.use)
  block-staff: false

  # Random disguise pool — /disguise random picks from this list
  # These are Minecraft usernames; skins are fetched from Mojang
  random-pool:
    - Notch
    - jeb_
    - Dinnerbone
    - Grumm
    - deadmau5
    - MidasGolden
```

- [ ] **Step 2: Build to verify**

Run: `./gradlew build -x test --console=plain`
Expected: BUILD SUCCESSFUL (config is a resource, not compiled)

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "feat(disguise): config v2 with disguise section"
```

---

### Task 5: NicknamesModule Upgrade — Disguise Storage & Manager API

**Files:**
- Modify: `src/main/java/dev/yanianz/essentials/nicknames/NicknamesModule.java`
- Test: `src/test/java/dev/yanianz/essentials/nicknames/NicknamesModuleTest.java` (extend existing)

**Interfaces:**
- Consumes: `DisguiseData`, `SkinCache` from Task 1 & 2
- Produces:
  - `DisguiseData getDisguise(UUID uuid)` — returns active DisguiseData or null
  - `String getDisplayName(UUID uuid)` — disguise name (colorized) or null
  - `String getDisplayName(Player player)` — same, convenience
  - `boolean isDisguised(UUID uuid)`
  - `void applyDisguise(Player player, DisguiseData data)` — stores, applies name, triggers skin refresh
  - `void removeDisguise(UUID uuid)` — clears disguise, restores real name
  - `List<String> getRandomPool()` — returns config random pool
  - `boolean isDisguiseEnabled()` — returns config disguise.enable
  - `boolean isSelfView()` — returns config self-view
  - `boolean isDisguiseCooldown(UUID uuid)` / `int getDisguiseRemainingCooldown(UUID uuid)` / `void markDisguiseChanged(UUID uuid)`
  - `SkinCache getSkinCache()` — for PacketDisguiseListener to access

The existing `nicknames` map stays for backward compat (name-only via `/nick`). The new `disguises` map stores `DisguiseData` for full disguises. Storage migrates from `nicknames.json` to `disguises.json`.

- [ ] **Step 1: Write the failing tests**

Add these tests to the existing `src/test/java/dev/yanianz/essentials/nicknames/NicknamesModuleTest.java`:

```java
    @Test
    @DisplayName("getDisplayName returns null for undisguised player")
    void testGetDisplayNameNull() {
        assertNull(module.getDisplayName(UUID.randomUUID()));
    }

    @Test
    @DisplayName("isDisguised returns false for unknown uuid")
    void testIsDisguisedUnknown() {
        assertFalse(module.isDisguised(UUID.randomUUID()));
    }

    @Test
    @DisplayName("getDisguise returns null for unknown uuid")
    void testGetDisguiseUnknown() {
        assertNull(module.getDisguise(UUID.randomUUID()));
    }

    @Test
    @DisplayName("getRandomPool returns empty list by default")
    void testGetRandomPoolDefault() {
        assertNotNull(module.getRandomPool());
        assertTrue(module.getRandomPool().isEmpty());
    }

    @Test
    @DisplayName("isDisguiseEnabled returns false by default")
    void testDisguiseDisabledDefault() {
        assertFalse(module.isDisguiseEnabled());
    }

    @Test
    @DisplayName("isSelfView returns false by default")
    void testSelfViewDefault() {
        assertFalse(module.isSelfView());
    }

    @Test
    @DisplayName("disguise cooldown works")
    void testDisguiseCooldown() throws Exception {
        setField("disguiseCooldownSeconds", 60);
        UUID id = UUID.randomUUID();
        assertFalse(module.isDisguiseCooldown(id));
        module.markDisguiseChanged(id);
        assertTrue(module.isDisguiseCooldown(id));
        setField("disguiseCooldownSeconds", 0);
        assertFalse(module.isDisguiseCooldown(id));
    }
```

Also add these imports at the top of the test file:

```java
import dev.yanianz.essentials.disguise.DisguiseData;
import static org.junit.jupiter.api.Assertions.assertNotNull;
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "dev.yanianz.essentials.nicknames.NicknamesModuleTest" --console=plain --no-daemon`
Expected: FAIL — methods `getDisplayName`, `isDisguised`, `getDisguise`, `getRandomPool`, `isDisguiseEnabled`, `isSelfView`, `isDisguiseCooldown`, `markDisguiseChanged` not found

- [ ] **Step 3: Implement disguise fields and methods in NicknamesModule.java**

Add these new fields after the existing fields (after line 38, `private int cooldownSeconds;`):

```java
    private boolean disguiseEnabled;
    private boolean selfView;
    private int disguiseCooldownSeconds;
    private int skinCacheHours;
    private boolean blockStaff;
    private List<String> randomPool = new java.util.ArrayList<>();
```

Add these `@NonLoadable` maps after the existing ones (after line 47, the `gson` field):

```java
    @NonLoadable
    private final Map<UUID, DisguiseData> disguises = new ConcurrentHashMap<>();
    @NonLoadable
    private final Map<UUID, Long> lastDisguiseChange = new ConcurrentHashMap<>();
    @NonLoadable
    private SkinCache skinCache;
```

Add import at top:

```java
import dev.yanianz.essentials.disguise.DisguiseData;
import dev.yanianz.essentials.disguise.SkinCache;
```

In `loadConfiguration()`, after `this.cooldownSeconds = Math.max(0, config.getInt("cooldown-seconds", 60));` (line 68), add:

```java
        this.disguiseEnabled = config.getBoolean("disguise.enable", true);
        this.selfView = config.getBoolean("disguise.self-view", false);
        this.disguiseCooldownSeconds = Math.max(0, config.getInt("disguise.cooldown-seconds", 120));
        this.skinCacheHours = Math.max(1, config.getInt("disguise.skin-cache-hours", 24));
        this.blockStaff = config.getBoolean("disguise.block-staff", false);
        this.randomPool = config.getStringList("disguise.random-pool");
        if (this.randomPool == null) this.randomPool = new java.util.ArrayList<>();
        this.skinCache = new SkinCache(this.skinCacheHours * 3600_000L);

        this.disguises.clear();
        loadDisguiseStorage();
        migrateOldStorage();
```

Add the DisguiseManager API methods (add these after the existing `getRemainingCooldown` method, before `applyDisplayName`):

```java
    public DisguiseData getDisguise(UUID uuid) {
        DisguiseData data = this.disguises.get(uuid);
        if (data == null || !data.isActive()) return null;
        return data;
    }

    public String getDisplayName(UUID uuid) {
        DisguiseData data = getDisguise(uuid);
        if (data == null) return null;
        if (data.getDisguiseName() != null) return data.getDisguiseName();
        return null;
    }

    public String getDisplayName(Player player) {
        return getDisplayName(player.getUniqueId());
    }

    public boolean isDisguised(UUID uuid) {
        return getDisguise(uuid) != null;
    }

    public void applyDisguise(Player player, DisguiseData data) {
        data.setPlayerId(player.getUniqueId());
        data.setAppliedAt(System.currentTimeMillis());
        data.setActive(true);
        this.disguises.put(player.getUniqueId(), data);
        saveDisguiseStorage();

        if (data.getDisguiseName() != null) {
            applyDisplayName(player, data.getDisguiseName());
        }
    }

    public void removeDisguise(UUID uuid) {
        this.disguises.remove(uuid);
        saveDisguiseStorage();

        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            String nickname = this.nicknames.get(uuid);
            if (nickname != null) {
                applyDisplayName(player, nickname);
            } else {
                applyDisplayName(player, player.getName());
            }
        }
    }

    public List<String> getRandomPool() {
        return this.randomPool;
    }

    public boolean isDisguiseEnabled() {
        return this.disguiseEnabled;
    }

    public boolean isSelfView() {
        return this.selfView;
    }

    public boolean isBlockStaff() {
        return this.blockStaff;
    }

    public SkinCache getSkinCache() {
        return this.skinCache;
    }

    public boolean isDisguiseCooldown(UUID uuid) {
        long last = this.lastDisguiseChange.getOrDefault(uuid, 0L);
        return System.currentTimeMillis() - last < this.disguiseCooldownSeconds * 1000L;
    }

    public void markDisguiseChanged(UUID uuid) {
        this.lastDisguiseChange.put(uuid, System.currentTimeMillis());
    }

    public int getDisguiseRemainingCooldown(UUID uuid) {
        long remainingMs = this.disguiseCooldownSeconds * 1000L - (System.currentTimeMillis() - this.lastDisguiseChange.getOrDefault(uuid, 0L));
        return (int) Math.max(0, remainingMs / 1000L + (remainingMs % 1000L == 0 ? 0 : 1));
    }
```

Add storage methods at the end of the class (before the closing brace, after the existing `Storage` inner class):

```java
    private File getDisguiseStorageFile() {
        return new File(getFolder(), "disguises.json");
    }

    @SuppressWarnings("unchecked")
    private void loadDisguiseStorage() {
        File file = getDisguiseStorageFile();
        if (!file.exists()) return;
        try {
            String json = Files.readString(file.toPath());
            DisguiseStorage storage = this.gson.fromJson(json, DisguiseStorage.class);
            if (storage != null && storage.entries != null) {
                for (Map.Entry<String, DisguiseData> entry : storage.entries.entrySet()) {
                    try {
                        this.disguises.put(UUID.fromString(entry.getKey()), entry.getValue());
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
        } catch (IOException | RuntimeException exception) {
            exception.printStackTrace();
        }
    }

    private void saveDisguiseStorage() {
        DisguiseStorage storage = new DisguiseStorage();
        this.disguises.forEach((uuid, data) -> storage.entries.put(uuid.toString(), data));
        try {
            Files.writeString(getDisguiseStorageFile().toPath(), this.gson.toJson(storage));
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    private void migrateOldStorage() {
        File oldFile = getStorageFile();
        if (!oldFile.exists()) return;
        if (getDisguiseStorageFile().exists()) return;
        try {
            String json = Files.readString(oldFile.toPath());
            Storage oldStorage = this.gson.fromJson(json, Storage.class);
            if (oldStorage != null && oldStorage.entries != null) {
                for (Map.Entry<String, String> entry : oldStorage.entries.entrySet()) {
                    try {
                        UUID uuid = UUID.fromString(entry.getKey());
                        if (this.disguises.containsKey(uuid)) continue;
                        DisguiseData data = new DisguiseData();
                        data.setPlayerId(uuid);
                        data.setDisguiseName(entry.getValue());
                        data.setAppliedAt(0);
                        data.setActive(true);
                        this.disguises.put(uuid, data);
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                saveDisguiseStorage();
            }
        } catch (IOException | RuntimeException ignored) {
        }
    }

    private static final class DisguiseStorage {
        Map<String, DisguiseData> entries = new HashMap<>();
    }
```

Update the `onJoin` method to re-apply disguise on join. Replace the existing `onJoin` method:

```java
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!this.isEnable) return;

        Player player = event.getPlayer();
        DisguiseData disguise = getDisguise(player.getUniqueId());
        if (disguise != null && disguise.getDisguiseName() != null) {
            applyDisplayName(player, disguise.getDisguiseName());
        } else {
            String nickname = this.nicknames.get(player.getUniqueId());
            if (nickname != null) {
                applyDisplayName(player, nickname);
            }
        }
    }
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew test --tests "dev.yanianz.essentials.nicknames.NicknamesModuleTest" --console=plain --no-daemon`
Expected: PASS — all existing + 7 new tests

- [ ] **Step 5: Build to verify compilation**

Run: `./gradlew build -x test --console=plain`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "feat(disguise): NicknamesModule DisguiseManager API + storage migration + 7 tests"
```

---

### Task 6: PacketDisguiseListener

**Files:**
- Create: `Hooks/ProtocolLib/src/main/java/fr/maxlego08/essentials/hooks/protocollib/PacketDisguiseListener.java`
- Modify: `Hooks/ProtocolLib/src/main/java/fr/maxlego08/essentials/hooks/protocollib/PacketListener.java`

**Interfaces:**
- Consumes: `NicknamesModule.getDisguise(UUID)`, `NicknamesModule.isSelfView()`, `NicknamesModule.getSkinCache()` from Task 5
- Produces: `PacketDisguiseListener` class implementing `PacketRegister`, intercepting `PacketType.Play.Server.PLAYER_INFO`

The listener loads the `NicknamesModule` class via reflection (same pattern as `PacketTabLayoutListener` which loads `TabListModule` via `Class.forName`). This is because the ProtocolLib hook module only depends on `:API`, not the root project — it cannot import `dev.yanianz.essentials.nicknames.NicknamesModule` directly.

- [ ] **Step 1: Create PacketDisguiseListener.java**

Create `Hooks/ProtocolLib/src/main/java/fr/maxlego08/essentials/hooks/protocollib/PacketDisguiseListener.java`:

```java
package fr.maxlego08.essentials.hooks.protocollib;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedProfileProperty;
import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.modules.Module;
import fr.maxlego08.essentials.api.modules.ModuleManager;
import fr.maxlego08.essentials.api.packet.PacketRegister;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PacketDisguiseListener extends PacketAdapter implements PacketRegister {

    private final EssentialsPlugin plugin;
    private final Class<? extends Module> nicknamesModuleClass;

    public PacketDisguiseListener(EssentialsPlugin plugin) {
        super(PacketAdapter.params()
                .plugin(plugin)
                .listenerPriority(ListenerPriority.HIGH)
                .types(PacketType.Play.Server.PLAYER_INFO));
        this.plugin = plugin;
        this.nicknamesModuleClass = loadNicknamesModuleClass();
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends Module> loadNicknamesModuleClass() {
        try {
            return (Class<? extends Module>) Class.forName("dev.yanianz.essentials.nicknames.NicknamesModule");
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    @Override
    public void addPacketListener() {
        ProtocolLibrary.getProtocolManager().addPacketListener(this);
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        if (this.nicknamesModuleClass == null) return;
        if (event.getPacketType() != PacketType.Play.Server.PLAYER_INFO) return;

        ModuleManager manager = this.plugin.getModuleManager();
        Module module = manager.getModule(this.nicknamesModuleClass);
        if (module == null) return;

        boolean disguiseEnabled;
        try {
            disguiseEnabled = (boolean) module.getClass().getMethod("isDisguiseEnabled").invoke(module);
        } catch (Exception e) {
            return;
        }
        if (!disguiseEnabled) return;

        boolean selfView;
        try {
            selfView = (boolean) module.getClass().getMethod("isSelfView").invoke(module);
        } catch (Exception e) {
            selfView = false;
        }

        var packet = event.getPacket();
        var infoDataList = packet.getPlayerInfoDataLists().readSafely(1);
        if (infoDataList == null) {
            infoDataList = packet.getPlayerInfoDataLists().readSafely(0);
            if (infoDataList == null) return;
        }

        boolean modified = false;
        List<PlayerInfoData> newData = new ArrayList<>(infoDataList);

        for (int i = 0; i < newData.size(); i++) {
            PlayerInfoData infoData = newData.get(i);
            UUID profileId = infoData.getProfileId();

            Object disguiseData;
            try {
                disguiseData = module.getClass().getMethod("getDisguise", UUID.class).invoke(module, profileId);
            } catch (Exception e) {
                continue;
            }
            if (disguiseData == null) continue;

            if (!selfView && event.getPlayer().getUniqueId().equals(profileId)) continue;

            String disguiseName;
            String textureValue;
            String textureSignature;
            try {
                disguiseName = (String) disguiseData.getClass().getMethod("getDisguiseName").invoke(disguiseData);
                textureValue = (String) disguiseData.getClass().getMethod("getTextureValue").invoke(disguiseData);
                textureSignature = (String) disguiseData.getClass().getMethod("getTextureSignature").invoke(disguiseData);
            } catch (Exception e) {
                continue;
            }

            WrappedGameProfile originalProfile = infoData.getProfile();
            String effectiveName = disguiseName != null ? disguiseName : originalProfile.getName();

            WrappedGameProfile newProfile = new WrappedGameProfile(profileId, effectiveName);

            if (textureValue != null) {
                if (textureSignature != null && !textureSignature.isEmpty()) {
                    newProfile.getProperties().put("textures", new WrappedProfileProperty("textures", textureValue, textureSignature));
                } else {
                    newProfile.getProperties().put("textures", new WrappedProfileProperty("textures", textureValue));
                }
            } else {
                newProfile.getProperties().putAll(originalProfile.getProperties());
            }

            PlayerInfoData newInfoData = new PlayerInfoData(
                    profileId,
                    infoData.getLatency(),
                    infoData.getGameMode(),
                    newProfile,
                    infoData.getDisplayName(),
                    infoData.getRemoteChatSessionData()
            );

            newData.set(i, newInfoData);
            modified = true;
        }

        if (modified) {
            packet.getPlayerInfoDataLists().write(1, newData);
        }
    }
}
```

- [ ] **Step 2: Register in PacketListener.java**

In `Hooks/ProtocolLib/src/main/java/fr/maxlego08/essentials/hooks/protocollib/PacketListener.java`, add registration after the existing `PacketCraftingListener` line:

Replace:
```java
    public void registerPackets(EssentialsPlugin plugin) {

        this.register(new PacketChatListener(plugin, plugin.getModuleManager().getModuleConfiguration("chat").getString("command-placeholder.result")));
        this.register(new PacketTabLayoutListener(plugin));
        this.register(new PacketTooltipListener(plugin));
        this.register(new PacketCraftingListener(plugin));
    }
```

With:
```java
    public void registerPackets(EssentialsPlugin plugin) {

        this.register(new PacketChatListener(plugin, plugin.getModuleManager().getModuleConfiguration("chat").getString("command-placeholder.result")));
        this.register(new PacketTabLayoutListener(plugin));
        this.register(new PacketTooltipListener(plugin));
        this.register(new PacketCraftingListener(plugin));
        this.register(new PacketDisguiseListener(plugin));
    }
```

- [ ] **Step 3: Build to verify compilation**

Run: `./gradlew build -x test --console=plain`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat(disguise): PacketDisguiseListener intercepts PLAYER_INFO for skin+name"
```

---

### Task 7: CommandDisguise

**Files:**
- Create: `src/main/java/dev/yanianz/essentials/disguise/CommandDisguise.java`
- Modify: `src/main/java/fr/maxlego08/essentials/commands/CommandLoader.java` — add import + register

**Interfaces:**
- Consumes: `NicknamesModule` from Task 5, `DisguiseData` from Task 1, `SkinCache` from Task 2, `Permission` + `Message` from Task 3
- Produces: `/disguise` command supporting: `<player>`, `random`, `skin <texture> [signature]`, `off`, `list`

The command handles:
- `/disguise <player>` — if player is online, read their GameProfile properties (instant); if offline, fetch from Mojang API async
- `/disguise random` — pick random name from pool, fetch skin async
- `/disguise skin <texture> [signature]` — custom texture string, no API call
- `/disguise off` — remove disguise (delegates to `removeDisguise`)
- `/disguise list` — list random pool

- [ ] **Step 1: Create CommandDisguise.java**

Create `src/main/java/dev/yanianz/essentials/disguise/CommandDisguise.java`:

```java
package dev.yanianz.essentials.disguise;

import dev.yanianz.essentials.nicknames.NicknamesModule;
import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.commands.CommandResultType;
import fr.maxlego08.essentials.api.commands.Permission;
import fr.maxlego08.essentials.api.messages.Message;
import fr.maxlego08.essentials.zutils.utils.commands.VCommand;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class CommandDisguise extends VCommand {

    public CommandDisguise(EssentialsPlugin plugin) {
        super(plugin);
        this.setModule(NicknamesModule.class);
        this.setPermission(Permission.ESSENTIALS_DISGUISE_USE);
        this.setDescription(Message.DESCRIPTION_DISGUISE);
        this.addOptionalArg("action", (sender, args) -> List.of("off", "random", "skin", "list"));
        this.setExtendedArgs(true);
    }

    @Override
    protected CommandResultType perform(EssentialsPlugin plugin) {

        NicknamesModule module = plugin.getModuleManager().getModule(NicknamesModule.class);
        if (module == null || !module.isDisguiseEnabled()) {
            message(sender, Message.DISGUISE_DISABLED);
            return CommandResultType.SUCCESS;
        }

        Player target = this.player;

        if (this.args.length == 0) {
            message(sender, Message.DISGUISE_USAGE);
            return CommandResultType.SUCCESS;
        }

        String firstArg = this.argAsString(0);

        if (firstArg.equalsIgnoreCase("off")) {
            if (this.args.length >= 2 && hasPermission(sender, Permission.ESSENTIALS_DISGUISE_OTHER)) {
                Player other = Bukkit.getPlayerExact(this.argAsString(1));
                if (other == null) {
                    message(sender, Message.DISGUISE_PLAYER_NOT_FOUND, "%player%", this.argAsString(1));
                    return CommandResultType.SUCCESS;
                }
                target = other;
            }
            if (!module.isDisguised(target.getUniqueId())) {
                message(sender, Message.DISGUISE_NOT_DISGUISED);
                return CommandResultType.SUCCESS;
            }
            module.removeDisguise(target.getUniqueId());
            if (target.equals(this.player)) {
                message(sender, Message.DISGUISE_REMOVED);
            } else {
                message(sender, Message.DISGUISE_REMOVED_OTHER, "%player%", target.getName());
                message(target, Message.DISGUISE_REMOVED);
            }
            return CommandResultType.SUCCESS;
        }

        if (firstArg.equalsIgnoreCase("list")) {
            List<String> pool = module.getRandomPool();
            if (pool.isEmpty()) {
                message(sender, Message.DISGUISE_RANDOM_EMPTY);
                return CommandResultType.SUCCESS;
            }
            message(sender, Message.DISGUISE_LIST_HEADER);
            for (String name : pool) {
                message(sender, Message.DISGUISE_LIST_ENTRY, "%name%", name);
            }
            return CommandResultType.SUCCESS;
        }

        if (firstArg.equalsIgnoreCase("random")) {
            if (!hasPermission(sender, Permission.ESSENTIALS_DISGUISE_RANDOM)) {
                return CommandResultType.NO_PERMISSION;
            }
            if (this.args.length >= 2 && hasPermission(sender, Permission.ESSENTIALS_DISGUISE_OTHER)) {
                Player other = Bukkit.getPlayerExact(this.argAsString(1));
                if (other != null) target = other;
            }
            List<String> pool = module.getRandomPool();
            if (pool.isEmpty()) {
                message(sender, Message.DISGUISE_RANDOM_EMPTY);
                return CommandResultType.SUCCESS;
            }
            String randomName = pool.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(pool.size()));
            message(sender, Message.DISGUISE_FETCHING);
            fetchAndApplyDisguise(module, target, randomName, randomName, true);
            return CommandResultType.SUCCESS;
        }

        if (firstArg.equalsIgnoreCase("skin")) {
            if (!hasPermission(sender, Permission.ESSENTIALS_DISGUISE_SKIN)) {
                return CommandResultType.NO_PERMISSION;
            }
            if (this.args.length < 2) {
                message(sender, Message.DISGUISE_USAGE);
                return CommandResultType.SUCCESS;
            }
            if (this.args.length >= 3 && hasPermission(sender, Permission.ESSENTIALS_DISGUISE_OTHER)) {
                Player other = Bukkit.getPlayerExact(this.argAsString(1));
                if (other != null) {
                    target = other;
                    String texture = this.argAsString(2);
                    String signature = this.args.length >= 4 ? this.argAsString(3) : null;
                    applySkinDisguise(module, target, texture, signature);
                    return CommandResultType.SUCCESS;
                }
            }
            String texture = this.argAsString(1);
            String signature = this.args.length >= 3 ? this.argAsString(2) : null;
            applySkinDisguise(module, target, texture, signature);
            return CommandResultType.SUCCESS;
        }

        if (this.args.length >= 2 && hasPermission(sender, Permission.ESSENTIALS_DISGUISE_OTHER)) {
            Player other = Bukkit.getPlayerExact(this.argAsString(0));
            if (other != null) {
                target = other;
                firstArg = this.argAsString(1);
            }
        }

        if (target != this.player && !hasPermission(sender, Permission.ESSENTIALS_DISGUISE_OTHER)) {
            return CommandResultType.NO_PERMISSION;
        }

        boolean cooldownBypass = hasPermission(sender, Permission.ESSENTIALS_DISGUISE_BYPASS_COOLDOWN);
        if (target.equals(this.player) && module.isDisguiseCooldown(target.getUniqueId()) && !cooldownBypass) {
            message(sender, Message.DISGUISE_COOLDOWN, "%seconds%", String.valueOf(module.getDisguiseRemainingCooldown(target.getUniqueId())));
            return CommandResultType.SUCCESS;
        }

        message(sender, Message.DISGUISE_FETCHING);
        String effectiveDisguiseName = firstArg;
        fetchAndApplyDisguise(module, target, firstArg, effectiveDisguiseName, target.equals(this.player));
        return CommandResultType.SUCCESS;
    }

    private void applySkinDisguise(NicknamesModule module, Player target, String texture, String signature) {
        DisguiseData data = module.getDisguise(target.getUniqueId());
        if (data == null) {
            data = new DisguiseData();
            data.setDisguiseName(target.getName());
        }
        data.setTextureValue(texture);
        data.setTextureSignature(signature);
        module.applyDisguise(target, data);
        if (target.equals(this.player)) {
            message(sender, Message.DISGUISE_SKIN_SET);
        } else {
            message(sender, Message.DISGUISE_SKIN_SET_OTHER, "%player%", target.getName());
            message(target, Message.DISGUISE_SKIN_SET);
        }
    }

    private void fetchAndApplyDisguise(NicknamesModule module, Player target, String playerName, String disguiseName, boolean self) {
        Player online = Bukkit.getPlayerExact(playerName);
        if (online != null) {
            String textureValue = null;
            String textureSignature = null;
            for (var property : online.getGameProfileProperties()) {
                if ("textures".equals(property.getName())) {
                    textureValue = property.getValue();
                    textureSignature = property.getSignature();
                    break;
                }
            }
            applyFetchedDisguise(module, target, disguiseName, textureValue, textureSignature, self);
            return;
        }

        CompletableFuture.supplyAsync(() -> {
            try {
                UUID uuid = fetchUuidFromName(playerName);
                if (uuid == null) return null;
                SkinCache.CachedProfile cached = module.getSkinCache().getCached(uuid);
                if (cached != null) return cached;
                String[] textures = fetchTexturesFromUuid(uuid);
                if (textures == null) return null;
                module.getSkinCache().put(uuid, playerName, textures[0], textures[1]);
                return module.getSkinCache().getCached(uuid);
            } catch (Exception e) {
                return null;
            }
        }).thenAccept(profile -> {
            this.plugin.getScheduler().runNextTick(w -> {
                if (profile == null) {
                    message(sender, Message.DISGUISE_FETCH_FAILED, "%player%", playerName);
                    return;
                }
                applyFetchedDisguise(module, target, disguiseName, profile.textureValue(), profile.textureSignature(), self);
            });
        });
    }

    private void applyFetchedDisguise(NicknamesModule module, Player target, String disguiseName, String textureValue, String textureSignature, boolean self) {
        DisguiseData data = new DisguiseData();
        data.setDisguiseName(disguiseName);
        data.setTextureValue(textureValue);
        data.setTextureSignature(textureSignature);
        module.applyDisguise(target, data);
        if (self) {
            module.markDisguiseChanged(target.getUniqueId());
            message(sender, Message.DISGUISE_SET, "%disguise%", disguiseName);
        } else {
            message(sender, Message.DISGUISE_SET_OTHER, "%player%", target.getName(), "%disguise%", disguiseName);
            message(target, Message.DISGUISE_SET, "%disguise%", disguiseName);
        }
    }

    private UUID fetchUuidFromName(String playerName) throws IOException {
        String url = "https://api.mojang.com/users/profiles/minecraft/" + playerName;
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        if (conn.getResponseCode() != 200) return null;
        String body;
        try (InputStream is = conn.getInputStream()) {
            body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
        String idStr = parseJsonField(body, "id");
        if (idStr == null) return null;
        return parseMojangId(idStr);
    }

    private String[] fetchTexturesFromUuid(UUID uuid) throws IOException {
        String url = "https://sessionserver.mojang.com/session/minecraft/profile/" + uuid.toString().replace("-", "") + "?unsigned=false";
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        if (conn.getResponseCode() != 200) return null;
        String body;
        try (InputStream is = conn.getInputStream()) {
            body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
        String value = parseJsonField(body, "value");
        if (value == null) return null;
        String signature = parseJsonField(body, "signature");
        return new String[]{value, signature};
    }

    private UUID parseMojangId(String idStr) {
        if (idStr.length() != 32) return null;
        String dashed = idStr.substring(0, 8) + "-" + idStr.substring(8, 12) + "-" + idStr.substring(12, 16) + "-" + idStr.substring(16, 20) + "-" + idStr.substring(20);
        return UUID.fromString(dashed);
    }

    private String parseJsonField(String json, String field) {
        String needle = "\"" + field + "\":\"";
        int start = json.indexOf(needle);
        if (start < 0) return null;
        start += needle.length();
        int end = json.indexOf("\"", start);
        if (end < 0) return null;
        return json.substring(start, end);
    }
}
```

- [ ] **Step 2: Build to verify compilation (CommandDisguise not yet registered — compiles but isn't wired)**

Run: `./gradlew build -x test --console=plain`
Expected: BUILD SUCCESSFUL (CommandDisguise.java compiles independently; registration happens in Task 8)

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "feat(disguise): CommandDisguise with player/random/skin/off/list subcommands"
```

---

### Task 8: CommandUnDisguise

**Files:**
- Create: `src/main/java/dev/yanianz/essentials/disguise/CommandUnDisguise.java`

**Interfaces:**
- Consumes: `NicknamesModule` from Task 5, `Permission` + `Message` from Task 3

- [ ] **Step 1: Create CommandUnDisguise.java**

Create `src/main/java/dev/yanianz/essentials/disguise/CommandUnDisguise.java`:

```java
package dev.yanianz.essentials.disguise;

import dev.yanianz.essentials.nicknames.NicknamesModule;
import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.commands.CommandResultType;
import fr.maxlego08.essentials.api.commands.Permission;
import fr.maxlego08.essentials.api.messages.Message;
import fr.maxlego08.essentials.zutils.utils.commands.VCommand;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class CommandUnDisguise extends VCommand {

    public CommandUnDisguise(EssentialsPlugin plugin) {
        super(plugin);
        this.setModule(NicknamesModule.class);
        this.setPermission(Permission.ESSENTIALS_DISGUISE_USE);
        this.setDescription(Message.DESCRIPTION_UNDISGUISE);
        this.addOptionalArg("player", getVisiblePlayerNames());
    }

    @Override
    protected CommandResultType perform(EssentialsPlugin plugin) {

        NicknamesModule module = plugin.getModuleManager().getModule(NicknamesModule.class);
        if (module == null || !module.isDisguiseEnabled()) {
            message(sender, Message.DISGUISE_DISABLED);
            return CommandResultType.SUCCESS;
        }

        Player target = this.argAsPlayer(0, this.player);
        if (target == null) {
            return CommandResultType.SYNTAX_ERROR;
        }

        boolean self = target.equals(this.player);
        if (!self && !hasPermission(this.sender, Permission.ESSENTIALS_DISGUISE_OTHER)) {
            message(this.sender, Message.COMMAND_NO_PERMISSION);
            return CommandResultType.NO_PERMISSION;
        }

        if (!module.isDisguised(target.getUniqueId())) {
            message(sender, Message.DISGUISE_NOT_DISGUISED);
            return CommandResultType.SUCCESS;
        }

        module.removeDisguise(target.getUniqueId());

        if (self) {
            message(sender, Message.DISGUISE_REMOVED);
        } else {
            message(sender, Message.DISGUISE_REMOVED_OTHER, "%player%", target.getName());
            message(target, Message.DISGUISE_REMOVED);
        }

        return CommandResultType.SUCCESS;
    }
}
```

- [ ] **Step 2: Register both commands in CommandLoader.java**

In `src/main/java/fr/maxlego08/essentials/commands/CommandLoader.java`, add these imports near the top with the other `dev.yanianz` imports:

```java
import dev.yanianz.essentials.disguise.CommandDisguise;
import dev.yanianz.essentials.disguise.CommandUnDisguise;
```

Then after line 184 (`register("nick", CommandNick.class);`), add:

```java
        register("disguise", CommandDisguise.class);
        register("undisguise", CommandUnDisguise.class);
```

- [ ] **Step 3: Build to verify compilation**

Run: `./gradlew build -x test --console=plain`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Run all tests**

Run: `./gradlew test --console=plain --no-daemon`
Expected: BUILD SUCCESSFUL — all existing tests pass

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat(disguise): CommandUnDisguise + register both commands"
```

---

### Task 9: NameTagModule Integration

**Files:**
- Modify: `src/main/java/dev/yanianz/essentials/nametags/NameTagModule.java`

**Interfaces:**
- Consumes: `NicknamesModule.getDisplayName(Player)` from Task 5

The NameTagModule uses `player.getName()` for scoreboard team entries. When a player is disguised, the team entry should use the disguise name. The key change is in the `apply()` method where `player.getName()` is used for team entry registration and fallback tab format.

- [ ] **Step 1: Update NameTagModule.apply() to use disguise name**

In `src/main/java/dev/yanianz/essentials/nametags/NameTagModule.java`, find the `apply()` method (line 188). Replace the line:

```java
        String fallbackTab = colorize(this.fallbackTabFormat.replace("%player%", player.getName()));
```

With:

```java
        NicknamesModule nickModule = this.plugin.getModuleManager().getModule(NicknamesModule.class);
        String effectiveName = player.getName();
        if (nickModule != null) {
            String disguiseName = nickModule.getDisplayName(player);
            if (disguiseName != null) {
                String plainName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                        .plainText().serialize(dev.yanianz.essentials.util.ColorUtil.component(disguiseName));
                if (!plainName.isBlank()) effectiveName = plainName;
            }
        }
        String fallbackTab = colorize(this.fallbackTabFormat.replace("%player%", effectiveName));
```

Then find the team entry registration (around line 225-226):

```java
                if (!team.hasEntry(player.getName())) {
                    team.addEntry(player.getName());
                }
```

Replace with:

```java
                if (!team.hasEntry(effectiveName)) {
                    team.addEntry(effectiveName);
                }
```

And the teamName call (line 215):

```java
                        ? teamName(rule, player.getName())
```

Replace with:

```java
                        ? teamName(rule, effectiveName)
```

Add the import at the top of the file:

```java
import dev.yanianz.essentials.nicknames.NicknamesModule;
```

- [ ] **Step 2: Build to verify compilation**

Run: `./gradlew build -x test --console=plain`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Run all tests**

Run: `./gradlew test --console=plain --no-daemon`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat(disguise): NameTagModule uses DisguiseManager for team name display"
```

---

### Task 10: Final Build, Test, and Changelog

**Files:**
- Modify: `changelog.md`

- [ ] **Step 1: Full build**

Run: `./gradlew build -x test --console=plain`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Full test suite**

Run: `./gradlew test --console=plain --no-daemon`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Count tests**

Run: `python3 -c "import re,glob; total=sum(int(re.search(r'tests=\"(\d+)\"', open(f).read()).group(1)) for f in glob.glob('build/test-results/test/*.xml') if re.search(r'tests=\"(\d+)\"', open(f).read())); print(f'Root tests: {total}')"`

Expected: Previous count (118) + 12 new (5 DisguiseData + 7 SkinCache) = 130

- [ ] **Step 4: Add changelog entry**

In `changelog.md`, after the existing `## Polish pass` section, add a new section:

```markdown
## Disguise system

- **Full disguise suite** — `/disguise <player>` changes name + skin at the packet level via ProtocolLib; `/disguise random` picks from config pool; `/disguise skin <texture> [signature]` sets custom skin; `/disguise off` and `/undisguise [player]` remove disguise
- **Skin sources** — online players (instant, reads GameProfile), offline players (Mojang API async + 24h cache), custom texture strings, random pool
- **Packet interception** — `PacketDisguiseListener` intercepts outgoing `PLAYER_INFO` packets, replaces `WrappedGameProfile` with disguise name + texture properties in-flight; self-view config toggle controls whether disguised player sees their own disguise
- **Persistence** — disguise state stored in `disguises.json`; auto-migrates from old `nicknames.json`; re-applied on join
- **Full sync** — NameTagModule uses `DisguiseManager.getDisplayName()` for scoreboard team entries; tablist displayName set to disguise name; chat name uses disguise name
- **Config v2** — `modules/nicknames/config.yml` bumped to config-version 2 with `disguise` section: enable, self-view, cooldown-seconds, skin-cache-hours, block-staff, random-pool
- **5 new permissions** — `ESSENTIALS_DISGUISE_USE`, `ESSENTIALS_DISGUISE_OTHER`, `ESSENTIALS_DISGUISE_RANDOM`, `ESSENTIALS_DISGUISE_SKIN`, `ESSENTIALS_DISGUISE_BYPASS_COOLDOWN`
- **12 new tests** — DisguiseData (5), SkinCache (7)
```

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "docs: changelog for disguise system"
```

- [ ] **Step 6: Verify final state**

Run: `git log --oneline -12`
Expected: See all disguise commits in order

Run: `grep -rn 'replace("&", "§")' src/main/java --include="*.java" | grep -v build/`
Expected: no output (no broken colorize patterns)
