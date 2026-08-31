# Cross-Server Storage Migration

## Goal

Migrate 9 features from per-module JSON files to `IStorage` (SQL + Mongo) so player data syncs across a proxy network.

## Features

| # | Feature | Current JSON path | Data shape |
|---|---------|-------------------|------------|
| 1 | Nicknames | `modules/nicknames/nicknames.json` | `Map<UUID, String>` |
| 2 | Disguises | `modules/nicknames/disguises.json` | `Map<UUID, DisguiseData>` |
| 3 | Chat preferences | `modules/chatcustomization/preferences.json` | `Map<UUID, Preference>` |
| 4 | Reports | `modules/reports/reports.json` | `Map<Integer, Report>` |
| 5 | Notes | `modules/notes/notes.json` | `Map<UUID, List<StaffNote>>` |
| 6 | Reputation | `modules/reputation/reputations.json` | `Map<UUID, PlayerReputation>` |
| 7 | EnderChest | `modules/enderchest/data/<uuid>.json` | `ItemStack[pages×45]` (Base64) |
| 8 | Stash items | `modules/stash/data/items/<uuid>.json` | `ItemStack[pages×45]` (Base64) |
| 9 | Stash materials | `modules/stash/data/materials/<uuid>.json` | `Map<Material, int>` (Base64 items) |

## Design decisions

- **IStorage pattern**: domain-specific methods per feature (e.g., `upsertReport`, `getReports`), matching the existing ~70-method interface
- **All 9 at once** in a single implementation pass
- **Auto-migration**: on module `onEnable()`, if legacy JSON exists → bulk insert → rename to `.migrated`
- **Both backends**: each feature gets a SQL repository (migration + `Repository` subclass) and a Mongo repository (`MongoRepository` subclass)
- **JsonStorage fallback**: methods throw `UnsupportedOperationException` with a "use MYSQL/MONGO" message (same as existing JsonStorage behavior)

## IStorage method signatures (per feature)

### Nicknames
```java
void upsertNickname(UUID uuid, String nickname);
void deleteNickname(UUID uuid);
String getNickname(UUID uuid);
Map<UUID, String> getAllNicknames();
```

### Disguises
```java
void upsertDisguise(UUID uuid, DisguiseData data);
void deleteDisguise(UUID uuid);
DisguiseData getDisguise(UUID uuid);
Map<UUID, DisguiseData> getAllDisguises();
```

### Chat preferences
```java
void upsertChatPreference(UUID uuid, Preference preference);
void deleteChatPreference(UUID uuid);
Preference getChatPreference(UUID uuid);
Map<UUID, Preference> getAllChatPreferences();
```

### Reports
```java
void upsertReport(Report report);
void deleteReport(int id);
List<ReportDTO> getReports();
List<ReportDTO> getReports(UUID targetUuid);
```

### Notes
```java
void upsertNote(UUID playerUuid, StaffNote note);
void deleteNote(UUID playerUuid, int noteIndex);
List<StaffNote> getNotes(UUID playerUuid);
void clearNotes(UUID playerUuid);
```

### Reputation
```java
void upsertReputation(UUID playerUuid, PlayerReputation rep);
PlayerReputation getReputation(UUID playerUuid);
Map<UUID, PlayerReputation> getAllReputations();
```

### EnderChest
```java
void upsertEnderChest(UUID uuid, EnderChestData data);
EnderChestData getEnderChest(UUID uuid);
```

### Stash items
```java
void upsertItemStash(UUID uuid, ItemStashData data);
ItemStashData getItemStash(UUID uuid);
```

### Stash materials
```java
void upsertMaterialStash(UUID uuid, MaterialStashData data);
MaterialStashData getMaterialStash(UUID uuid);
```

## SQL tables

```sql
-- Nicknames
CREATE TABLE zess_nicknames (uuid VARCHAR(36) PRIMARY KEY, nickname TEXT);

-- Disguises
CREATE TABLE zess_disguises (uuid VARCHAR(36) PRIMARY KEY, type VARCHAR(32), data_json TEXT);

-- Chat preferences
CREATE TABLE zess_chat_prefs (uuid VARCHAR(36) PRIMARY KEY, preference_json TEXT);

-- Reports
CREATE TABLE zess_reports (id INT AUTO_INCREMENT, reporter_uuid VARCHAR(36), target_uuid VARCHAR(36), reason TEXT, timestamp BIGINT, resolved BOOLEAN);

-- Notes
CREATE TABLE zess_notes (id INT AUTO_INCREMENT, player_uuid VARCHAR(36), staff_uuid VARCHAR(36), text TEXT, timestamp BIGINT);

-- Reputation
CREATE TABLE zess_reputations (player_uuid VARCHAR(36), giver_uuid VARCHAR(36), amount INT, timestamp BIGINT, PRIMARY KEY(player_uuid, giver_uuid));

-- EnderChest
CREATE TABLE zess_enderchest (uuid VARCHAR(36), page INT, slot INT, item_base64 TEXT, PRIMARY KEY(uuid, page, slot));

-- Stash items
CREATE TABLE zess_stash_items (uuid VARCHAR(36), page INT, slot INT, item_base64 TEXT, PRIMARY KEY(uuid, page, slot));

-- Stash materials
CREATE TABLE zess_stash_materials (uuid VARCHAR(36), material VARCHAR(64), amount INT, item_base64 TEXT, PRIMARY KEY(uuid, material));
```

## Mongo collections

Same names, documents keyed by `uuid` field. ItemStacks stored as Base64 strings. Complex objects (DisguiseData, Preference, PlayerReputation) stored as nested BSON via Gson serialization.

## Migration flow (per module)

1. On `onEnable()`, check if `modules/<feature>/<file>.json` exists
2. Read + parse with Gson (existing deserialization code)
3. Bulk insert/upsert into DB via `IStorage` methods
4. Rename file to `<file>.json.migrated`
5. Log: `"Migrated N records from JSON to <storage-type>"`

## Module changes

Each module's `load()` and `save()` methods switch from JSON file I/O to `IStorage` calls. The in-memory data structures stay the same — only the persistence layer changes.

## Files touched

- `API/src/.../api/storage/IStorage.java` — new method signatures
- `src/.../storage/storages/SqlStorage.java` — implementations + repository registrations + migrations
- `src/.../storage/mongodb/MongoStorage.java` + `MongoRepositories.java` — implementations + new repo fields
- `src/.../storage/storages/JsonStorage.java` — UnsupportedOperationException stubs
- `src/.../storage/database/repositeries/` — 9 new SQL repository classes
- `src/.../storage/mongodb/repositories/` — 9 new Mongo repository classes
- `src/.../migrations/create/` — 9 new migration classes
- 9 module files — switch from JSON to IStorage
- New DTO classes as needed
