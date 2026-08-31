# Cross-Server Storage Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Migrate 9 JSON-stored features to IStorage (SQL + Mongo) with auto-migration.

**Architecture:** Add domain-specific IStorage methods per feature, implement in SqlStorage (sarah repos + migrations) and MongoStorage (MongoRepository), stub JsonStorage, update each module to call IStorage instead of JSON file I/O, add JSON→DB auto-migration on startup.

**Tech Stack:** Java 21, Bukkit/Paper API, sarah query builder (SQL), MongoDB driver, Gson, FoliaLib schedulers

**Spec:** `docs/superpowers/specs/2026-09-01-cross-server-storage-design.md`

## Global Constraints

- Java 21 bytecode target
- Follow existing repository patterns (`Repository` base for SQL, `MongoRepository` base for Mongo)
- Match the "repositeries" (sic) directory name for SQL repos
- `sarah` query builder for SQL, `Document` for Mongo
- ItemStacks serialized as Base64 strings via `ItemStack.serializeAsBytes()`
- Folia-safe: use `runNextTick` for any Bukkit API calls from async contexts
- Auto-migration: rename JSON to `.migrated` after successful import

---

### Task 1: IStorage interface method signatures

**Files:**
- Modify: `API/src/main/java/fr/maxlego08/essentials/api/storage/IStorage.java`

**Interfaces:**
- Produces: all new method signatures that Tasks 2-7 implement

- [ ] **Step 1: Add method signatures for all 9 features**

Add to `IStorage.java`:

```java
// Nicknames
void upsertNickname(UUID uuid, String nickname);
void deleteNickname(UUID uuid);
String getNickname(UUID uuid);
Map<UUID, String> getAllNicknames();

// Disguises
void upsertDisguise(UUID uuid, dev.yanianz.essentials.disguise.DisguiseData data);
void deleteDisguise(UUID uuid);
dev.yanianz.essentials.disguise.DisguiseData getDisguise(UUID uuid);
Map<UUID, dev.yanianz.essentials.disguise.DisguiseData> getAllDisguises();

// Chat preferences
void upsertChatPreference(UUID uuid, dev.yanianz.essentials.chatcustomization.ChatCustomizationModule.Preference preference);
void deleteChatPreference(UUID uuid);
dev.yanianz.essentials.chatcustomization.ChatCustomizationModule.Preference getChatPreference(UUID uuid);
Map<UUID, dev.yanianz.essentials.chatcustomization.ChatCustomizationModule.Preference> getAllChatPreferences();

// Reports
void upsertReport(dev.yanianz.essentials.reports.ReportsModule.Report report);
void deleteReport(int id);
List<dev.yanianz.essentials.reports.ReportsModule.Report> getReports();
List<dev.yanianz.essentials.reports.ReportsModule.Report> getReports(UUID targetUuid);

// Notes
void upsertNote(UUID playerUuid, dev.yanianz.essentials.notes.NotesModule.StaffNote note);
void deleteNote(UUID playerUuid, int noteIndex);
List<dev.yanianz.essentials.notes.NotesModule.StaffNote> getNotes(UUID playerUuid);
void clearNotes(UUID playerUuid);

// Reputation
void upsertReputation(UUID playerUuid, dev.yanianz.essentials.reputation.ReputationModule.PlayerReputation reputation);
dev.yanianz.essentials.reputation.ReputationModule.PlayerReputation getReputation(UUID playerUuid);
Map<UUID, dev.yanianz.essentials.reputation.ReputationModule.PlayerReputation> getAllReputations();

// EnderChest
void upsertEnderChest(UUID uuid, dev.yanianz.essentials.enderchest.EnderChestData data);
dev.yanianz.essentials.enderchest.EnderChestData getEnderChest(UUID uuid);

// Stash items
void upsertItemStash(UUID uuid, dev.yanianz.essentials.stash.ItemStashData data);
dev.yanianz.essentials.stash.ItemStashData getItemStash(UUID uuid);

// Stash materials
void upsertMaterialStash(UUID uuid, dev.yanianz.essentials.stash.MaterialStashData data);
dev.yanianz.essentials.stash.MaterialStashData getMaterialStash(UUID uuid);
```

- [ ] **Step 2: Build API module to verify compilation**

Run: `./gradlew :API:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add API/src/main/java/fr/maxlego08/essentials/api/storage/IStorage.java
git commit -m "feat(storage): add IStorage method signatures for 9 JSON-stored features"
```

---

### Task 2: SQL migrations + repositories

**Files:**
- Create: `src/main/java/fr/maxlego08/essentials/migrations/create/CreateNicknamesMigration.java` (and 8 more)
- Create: `src/main/java/fr/maxlego08/essentials/storage/database/repositeries/NicknameRepository.java` (and 8 more)
- Modify: `src/main/java/fr/maxlego08/essentials/storage/storages/SqlStorage.java` (register repos + migration calls)

**Interfaces:**
- Consumes: IStorage method signatures from Task 1
- Produces: 9 SQL repository classes + 9 migration classes, all registered in SqlStorage

- [ ] **Step 1: Create 9 migration classes**

Each migration follows the pattern of existing migrations (e.g., `CreateUserStepMigration`). Example for nicknames:

```java
package fr.maxlego08.essentials.migrations.create;

import fr.maxlego08.essentials.migrations.Migration;
import fr.maxlego08.essentials.storage.database.DatabaseConnection;

public class CreateNicknamesMigration extends Migration {
    public CreateNicknamesMigration() {
        super("Create nicknames table");
    }
    @Override
    public void up(DatabaseConnection connection) {
        connection.execute("CREATE TABLE IF NOT EXISTS " + connection.getDatabaseConfiguration().getTablePrefix() + "nicknames (uuid VARCHAR(36) PRIMARY KEY, nickname TEXT)");
    }
}
```

Create similar for: disguises, chat_prefs, reports, notes, reputations, enderchest, stash_items, stash_materials. Table definitions from the spec.

- [ ] **Step 2: Create 9 SQL repository classes**

Each extends `Repository`. Example for nicknames:

```java
package fr.maxlego08.essentials.storage.database.repositeries;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.storage.database.DatabaseConnection;
import fr.maxlego08.essentials.storage.database.Repository;
import fr.maxlego08.essentials.storage.database.sarah.Schema;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NicknameRepository extends Repository {
    public NicknameRepository(EssentialsPlugin plugin, DatabaseConnection connection) {
        super(plugin, connection, "nicknames");
    }
    public void upsert(UUID uuid, String nickname) {
        upsert(table -> {
            table.uuid("uuid").value(uuid.toString());
            table.text("nickname").value(nickname);
        });
    }
    public void delete(UUID uuid) {
        delete(table -> table.uuid("uuid").value(uuid.toString()));
    }
    public String get(UUID uuid) {
        var results = select(String.class, table -> table.uuid("uuid").value(uuid.toString()));
        // ... return nickname from results
    }
    public Map<UUID, String> getAll() {
        // selectAll and map to UUID->String
    }
}
```

- [ ] **Step 3: Register migrations and repositories in SqlStorage**

In `SqlStorage.java`, add migration calls in the migration block (lines 87-124) and `repositories.register()` calls (lines 127-152).

- [ ] **Step 4: Build to verify**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat(storage): SQL migrations + repositories for 9 JSON-stored features"
```

---

### Task 3: SqlStorage IStorage implementations

**Files:**
- Modify: `src/main/java/fr/maxlego08/essentials/storage/storages/SqlStorage.java`

- [ ] **Step 1: Implement all IStorage methods in SqlStorage**

For each of the 9 features, implement the IStorage methods using `with(XxxRepository.class).method(...)`. Example:

```java
@Override
public void upsertNickname(UUID uuid, String nickname) {
    with(NicknameRepository.class).upsert(uuid, nickname);
}
```

- [ ] **Step 2: Build to verify**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

---

### Task 4: Mongo repositories + MongoStorage implementations

**Files:**
- Create: `src/main/java/fr/maxlego08/essentials/storage/mongodb/repositories/MongoNicknameRepository.java` (and 8 more)
- Modify: `src/main/java/fr/maxlego08/essentials/storage/mongodb/MongoRepositories.java`
- Modify: `src/main/java/fr/maxlego08/essentials/storage/mongodb/MongoStorage.java`

- [ ] **Step 1: Create 9 Mongo repository classes**

Each extends `MongoRepository`. Example:

```java
package fr.maxlego08.essentials.storage.mongodb.repositories;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.storage.mongodb.MongoRepository;
import com.mongodb.client.MongoDatabase;
import java.util.UUID;

public class MongoNicknameRepository extends MongoRepository {
    public MongoNicknameRepository(EssentialsPlugin plugin, MongoDatabase database) {
        super(plugin, database, "nicknames");
    }
    // upsert, delete, get, getAll methods
}
```

- [ ] **Step 2: Add 9 fields to MongoRepositories**

- [ ] **Step 3: Implement IStorage methods in MongoStorage**

- [ ] **Step 4: Build to verify**

- [ ] **Step 5: Commit**

---

### Task 5: JsonStorage stubs

**Files:**
- Modify: `src/main/java/fr/maxlego08/essentials/storage/storages/JsonStorage.java`

- [ ] **Step 1: Add UnsupportedOperationException stubs for all 9 features**

- [ ] **Step 2: Build to verify**

- [ ] **Step 3: Commit**

---

### Task 6: Module updates + auto-migration

**Files:**
- Modify: 9 module files (NicknamesModule, ChatCustomizationModule, ReportsModule, NotesModule, ReputationModule, EnderChestModule, StashModule)
- Each module: replace JSON load/save with IStorage calls + add auto-migration

- [ ] **Step 1: Update NicknamesModule** — replace loadStorage/saveStorage with IStorage, add migration
- [ ] **Step 2: Update ChatCustomizationModule** — same
- [ ] **Step 3: Update ReportsModule** — same
- [ ] **Step 4: Update NotesModule** — same
- [ ] **Step 5: Update ReputationModule** — same
- [ ] **Step 6: Update EnderChestModule** — replace per-player JSON with IStorage, add migration
- [ ] **Step 7: Update StashModule** — same for items + materials
- [ ] **Step 8: Build to verify**

Run: `./gradlew build -x test`
Expected: BUILD SUCCESSFUL

- [ ] **Step 9: Commit**

---

### Task 7: Final build + changelog + readme

- [ ] **Step 1: Full build**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Update changelog.md + readme.md**

- [ ] **Step 3: Commit + push**
