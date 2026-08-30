# Database Backends Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Activate the existing Redis hook for cross-server messaging and add MongoDB as a full IStorage storage backend.

**Architecture:** Redis activation is a 4-line uncomment + paper-api version fix. MongoDB uses a generic `MongoRepository` base class with Gson-based DTO↔Document mapping, wrapped by 25 thin specific repositories aggregated in `MongoRepositories`, and a `MongoStorage` class that implements all 90+ `IStorage` methods by delegating to those repositories. Same caching pattern as `SqlStorage` via `StorageHelper`.

**Tech Stack:** Java 21, MongoDB Java Driver (sync) 5.2.1, Jedis 4.3.1, Gson, JUnit 5 + Mockito for tests.

**Spec:** `docs/superpowers/specs/2026-08-30-database-backends-design.md`

## Global Constraints

- Java 21 bytecode target. Paper API is `compileOnly`.
- No comments in code unless explicitly requested.
- Folia-supported: use `plugin.getScheduler()` for async work.
- MongoDB driver is a runtime library in `plugin.yml` (downloaded by Paper, not shaded).
- Config self-healing: `config-version` key in `config.yml`.
- Package convention: new MongoDB storage code goes under `fr.maxlego08.essentials.storage.mongodb.*` (matching existing `storage.database.*` for SQL).
- Build: `./gradlew build -x test --console=plain`
- Test: `./gradlew test --console=plain --no-daemon`
- Working directory: `/Users/rheninxy/Sourby/zEssentials`

---

## File Structure

| File | Responsibility |
|------|---------------|
| `Hooks/Redis/build.gradle.kts` | Fix stale paper-api version |
| `src/main/java/fr/maxlego08/essentials/ZEssentialsPlugin.java` | Uncomment Redis activation |
| `API/.../storage/StorageType.java` | Add `MONGO` enum value |
| `src/main/resources/config.yml` | Add `mongo-configuration` section |
| `src/main/java/fr/maxlego08/essentials/MainConfiguration.java` | Add MongoConfiguration parsing |
| `API/.../server/MongoConfiguration.java` | New record for MongoDB config |
| `src/main/java/fr/maxlego08/essentials/storage/mongodb/MongoConnection.java` | MongoDB client wrapper |
| `src/main/java/fr/maxlego08/essentials/storage/mongodb/MongoRepository.java` | Generic CRUD base class |
| `src/main/java/fr/maxlego08/essentials/storage/mongodb/MongoRepositories.java` | Aggregates all mongo repos |
| `src/main/java/fr/maxlego08/essentials/storage/mongodb/MongoStorage.java` | IStorage implementation |
| `src/main/java/fr/maxlego08/essentials/storage/mongodb/repos/*.java` | 25 thin repository wrappers |
| `src/main/java/fr/maxlego08/essentials/storage/ZStorageManager.java` | Add MONGO to switch |
| `src/main/resources/plugin.yml` | Add mongodb-driver library |
| `src/test/java/dev/yanianz/essentials/mongodb/MongoConfigTest.java` | Config parsing tests |

---

### Task 1: Redis Activation

**Files:**
- Modify: `Hooks/Redis/build.gradle.kts`
- Modify: `src/main/java/fr/maxlego08/essentials/ZEssentialsPlugin.java`

**Interfaces:**
- Produces: Working `server-type: REDIS` activation path

- [ ] **Step 1: Fix Redis hook paper-api version**

In `Hooks/Redis/build.gradle.kts`, change `1.21.5-R0.1-SNAPSHOT` to `26.2.build.119-stable` to match the root project:

```kotlin
    compileOnly("io.papermc.paper:paper-api:26.2.build.119-stable")
```

- [ ] **Step 2: Uncomment Redis activation in ZEssentialsPlugin**

In `ZEssentialsPlugin.java`, lines 202-205, uncomment the Redis activation block:

```java
        // Essentials Server
        if (this.configuration.getServerType() == ServerType.REDIS) {
            this.essentialsServer = new RedisServer(this);
            this.getLogger().info("Using Redis server.");
        }
```

Remove the `/*` before line 202 and the `*/` after line 205. Add the import if needed:
```java
import fr.maxlego08.essentials.hooks.redis.RedisServer;
```

- [ ] **Step 3: Build to verify compilation**

Run: `./gradlew build -x test --console=plain`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Run all tests**

Run: `./gradlew test --console=plain --no-daemon`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add Hooks/Redis/build.gradle.kts src/main/java/fr/maxlego08/essentials/ZEssentialsPlugin.java
git commit -m "feat: activate Redis server hook for cross-server messaging"
```

---

### Task 2: MongoDB Config Infrastructure

**Files:**
- Modify: `API/src/main/java/fr/maxlego08/essentials/api/storage/StorageType.java`
- Create: `API/src/main/java/fr/maxlego08/essentials/api/server/MongoConfiguration.java`
- Modify: `src/main/resources/config.yml`
- Modify: `src/main/java/fr/maxlego08/essentials/MainConfiguration.java`
- Modify: `src/main/resources/plugin.yml`

**Interfaces:**
- Produces: `StorageType.MONGO`, `MongoConfiguration` record, `MainConfiguration.getMongoConfiguration()`, `config.yml` mongo section, `plugin.yml` mongo driver library

- [ ] **Step 1: Add MONGO to StorageType enum**

In `StorageType.java`, add `MONGO` after `HIKARICP`:

```java
public enum StorageType {
    JSON,
    MYSQL,
    MARIADB,
    SQLITE,
    HIKARICP,
    MONGO,
}
```

- [ ] **Step 2: Create MongoConfiguration record**

Create `API/src/main/java/fr/maxlego08/essentials/api/server/MongoConfiguration.java`:

```java
package fr.maxlego08.essentials.api.server;

public record MongoConfiguration(
    String uri,
    String host,
    int port,
    String user,
    String password,
    String database
) {
    public boolean useUri() {
        return uri != null && !uri.isBlank();
    }

    public boolean hasAuth() {
        return user != null && !user.isBlank();
    }
}
```

- [ ] **Step 3: Add mongo-configuration to config.yml**

In `config.yml`, after the `redis-configuration` section, add:

```yaml
# Configuration for your MongoDB server
# Set storage-type to MONGO to use MongoDB instead of SQL/JSON
mongo-configuration:
  # Full connection URI (overrides individual fields if non-empty)
  uri: ""
  # Individual fields (used when uri is empty)
  host: localhost
  port: 27017
  user: ""
  password: ""
  database: zessentials
```

Also update the `storage-type` comment block to mention MONGO:

```
# MONGO - MongoDB NoSQL database, ideal for cross-server data sharing
```

- [ ] **Step 4: Add MongoConfiguration parsing to MainConfiguration**

In `MainConfiguration.java`, add a field and getter. Look at how `RedisConfiguration` is defined and mirror that pattern:

```java
    private MongoConfiguration mongoConfiguration;
```

In the configuration loading section (look for where `redisConfiguration` is loaded), add:

```java
        ConfigurationSection mongoSection = config.getConfigurationSection("mongo-configuration");
        if (mongoSection != null) {
            this.mongoConfiguration = new MongoConfiguration(
                mongoSection.getString("uri", ""),
                mongoSection.getString("host", "localhost"),
                mongoSection.getInt("port", 27017),
                mongoSection.getString("user", ""),
                mongoSection.getString("password", ""),
                mongoSection.getString("database", "zessentials")
            );
        }
```

Add the getter:
```java
    public MongoConfiguration getMongoConfiguration() {
        return mongoConfiguration;
    }
```

Add imports: `import fr.maxlego08.essentials.api.server.MongoConfiguration;` and `import org.bukkit.configuration.ConfigurationSection;` (if not already present).

- [ ] **Step 5: Add MongoDB driver to plugin.yml**

In `plugin.yml`, add to the `libraries` list:

```yaml
libraries:
  - 'org.mariadb.jdbc:mariadb-java-client:3.5.6'
  - 'org.mongodb:mongodb-driver-sync:5.2.1'
```

- [ ] **Step 6: Build to verify compilation**

Run: `./gradlew build -x test --console=plain`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
git add API/src/main/java/fr/maxlego08/essentials/api/storage/StorageType.java \
  API/src/main/java/fr/maxlego08/essentials/api/server/MongoConfiguration.java \
  src/main/resources/config.yml \
  src/main/java/fr/maxlego08/essentials/MainConfiguration.java \
  src/main/resources/plugin.yml
git commit -m "feat(mongo): config infrastructure — StorageType.MONGO, MongoConfiguration, plugin.yml driver"
```

---

### Task 3: MongoConnection + MongoRepository Base Class

**Files:**
- Create: `src/main/java/fr/maxlego08/essentials/storage/mongodb/MongoConnection.java`
- Create: `src/main/java/fr/maxlego08/essentials/storage/mongodb/MongoRepository.java`
- Test: `src/test/java/dev/yanianz/essentials/mongodb/MongoConfigTest.java`

**Interfaces:**
- Consumes: `MongoConfiguration`
- Produces: `MongoConnection(MongoConfiguration)`, `MongoConnection.getDatabase()`, `MongoConnection.isValid()`, `MongoConnection.close()`, `MongoRepository(plugin, database, collectionName)`, `MongoRepository.collection()`, `MongoRepository.upsert(filter, document)`, `MongoRepository.insert(document)`, `MongoRepository.delete(filter)`, `MongoRepository.findOne(filter, type)`, `MongoRepository.find(filter, type)`, `MongoRepository.findAll(type)`, `MongoRepository.count(filter)`

- [ ] **Step 1: Write the failing test**

```java
package dev.yanianz.essentials.mongodb;

import fr.maxlego08.essentials.api.server.MongoConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MongoConfigTest {

    @Test
    @DisplayName("useUri returns true when uri is non-blank")
    void testUseUriTrue() {
        MongoConfiguration config = new MongoConfiguration(
            "mongodb://localhost:27017", "localhost", 27017, "", "", "test");
        assertTrue(config.useUri());
    }

    @Test
    @DisplayName("useUri returns false when uri is blank")
    void testUseUriFalse() {
        MongoConfiguration config = new MongoConfiguration(
            "", "localhost", 27017, "", "", "test");
        assertFalse(config.useUri());
    }

    @Test
    @DisplayName("useUri returns false when uri is null")
    void testUseUriNull() {
        MongoConfiguration config = new MongoConfiguration(
            null, "localhost", 27017, "", "", "test");
        assertFalse(config.useUri());
    }

    @Test
    @DisplayName("hasAuth returns true when user is non-blank")
    void testHasAuthTrue() {
        MongoConfiguration config = new MongoConfiguration(
            "", "localhost", 27017, "admin", "secret", "test");
        assertTrue(config.hasAuth());
    }

    @Test
    @DisplayName("hasAuth returns false when user is blank")
    void testHasAuthFalse() {
        MongoConfiguration config = new MongoConfiguration(
            "", "localhost", 27017, "", "", "test");
        assertFalse(config.hasAuth());
    }

    @Test
    @DisplayName("Individual fields are accessible")
    void testFieldAccess() {
        MongoConfiguration config = new MongoConfiguration(
            "", "mongo.example.com", 27018, "user", "pass", "zessentials");
        assertEquals("mongo.example.com", config.host());
        assertEquals(27018, config.port());
        assertEquals("user", config.user());
        assertEquals("pass", config.password());
        assertEquals("zessentials", config.database());
    }

    @Test
    @DisplayName("URI field is accessible")
    void testUriAccess() {
        MongoConfiguration config = new MongoConfiguration(
            "mongodb://user:pass@host:27017", "", 0, "", "", "db");
        assertEquals("mongodb://user:pass@host:27017", config.uri());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :API:test --tests "dev.yanianz.essentials.mongodb.MongoConfigTest" --console=plain --no-daemon`
Expected: FAIL — MongoConfiguration not found (wrong module — this test is in root, not API)

Wait — `MongoConfiguration` is in the API module. The test needs to be in the API module or the root module needs the API dependency. Let me fix the test location:

Actually, `MongoConfiguration` is created in Task 2. The test should run after Task 2. Place the test in the API module:

`API/src/test/java/fr/maxlego08/essentials/api/server/MongoConfigTest.java`

Change the package:
```java
package fr.maxlego08.essentials.api.server;
```

Run: `./gradlew :API:test --tests "fr.maxlego08.essentials.api.server.MongoConfigTest" --console=plain --no-daemon`

- [ ] **Step 3: Write MongoConnection**

```java
package fr.maxlego08.essentials.storage.mongodb;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoException;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import fr.maxlego08.essentials.api.server.MongoConfiguration;

import java.util.Collections;

public class MongoConnection {

    private final MongoClient client;
    private final MongoDatabase database;

    public MongoConnection(MongoConfiguration config) {
        MongoClientSettings settings;

        if (config.useUri()) {
            settings = MongoClientSettings.builder()
                    .applyConnectionString(new com.mongodb.ConnectionString(config.uri()))
                    .build();
        } else if (config.hasAuth()) {
            settings = MongoClientSettings.builder()
                    .applyToClusterSettings(builder -> builder.hosts(
                            Collections.singletonList(new ServerAddress(config.host(), config.port()))))
                    .credential(com.mongodb.MongoCredential.createCredential(
                            config.user(), config.database(), config.password().toCharArray()))
                    .build();
        } else {
            settings = MongoClientSettings.builder()
                    .applyToClusterSettings(builder -> builder.hosts(
                            Collections.singletonList(new ServerAddress(config.host(), config.port()))))
                    .build();
        }

        this.client = MongoClients.create(settings);
        this.database = client.getDatabase(config.database());
    }

    public MongoDatabase getDatabase() {
        return database;
    }

    public boolean isValid() {
        try {
            this.client.listDatabaseNames().first();
            return true;
        } catch (MongoException e) {
            return false;
        }
    }

    public void close() {
        if (client != null) {
            client.close();
        }
    }
}
```

- [ ] **Step 4: Write MongoRepository base class**

```java
package fr.maxlego08.essentials.storage.mongodb;

import com.google.gson.Gson;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.zutils.utils.ZUtils;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

public abstract class MongoRepository extends ZUtils {

    protected final EssentialsPlugin plugin;
    private final MongoDatabase database;
    private final String collectionName;
    protected final Gson gson;

    public MongoRepository(EssentialsPlugin plugin, MongoDatabase database, String collectionName) {
        this.plugin = plugin;
        this.database = database;
        this.collectionName = collectionName;
        this.gson = new Gson();
    }

    public MongoCollection<Document> collection() {
        return database.getCollection(collectionName);
    }

    public String getCollectionName() {
        return collectionName;
    }

    protected void upsert(Document filter, Document document) {
        collection().replaceOne(filter, document, new ReplaceOptions().upsert(true));
    }

    protected void insert(Document document) {
        collection().insertOne(document);
    }

    protected void insertMany(List<Document> documents) {
        collection().insertMany(documents);
    }

    protected void delete(Document filter) {
        collection().deleteOne(filter);
    }

    protected void deleteMany(Document filter) {
        collection().deleteMany(filter);
    }

    protected <T> T findOne(Document filter, Class<T> type) {
        Document doc = collection().find(filter).first();
        return doc != null ? gson.fromJson(doc.toJson(), type) : null;
    }

    protected <T> List<T> find(Document filter, Class<T> type) {
        List<T> result = new ArrayList<>();
        FindIterable<Document> docs = collection().find(filter);
        for (Document doc : docs) {
            result.add(gson.fromJson(doc.toJson(), type));
        }
        return result;
    }

    protected <T> List<T> findAll(Class<T> type) {
        return find(new Document(), type);
    }

    protected long count(Document filter) {
        return collection().countDocuments(filter);
    }

    protected long countAll() {
        return collection().countDocuments();
    }

    protected Document byUuid(java.util.UUID uuid) {
        return new Document("uuid", uuid.toString());
    }

    protected Document byField(String field, Object value) {
        return new Document(field, value);
    }

    protected Document toDocument(Object dto) {
        return Document.parse(gson.toJson(dto));
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :API:test --tests "fr.maxlego08.essentials.api.server.MongoConfigTest" --console=plain --no-daemon`
Expected: PASS — 7 tests

- [ ] **Step 6: Build to verify compilation**

Run: `./gradlew build -x test --console=plain`
Expected: BUILD SUCCESSFUL

Note: MongoDB driver classes (`com.mongodb.*`, `org.bson.*`) are `compileOnly`. If compilation fails due to missing MongoDB classes, add `compileOnly("org.mongodb:mongodb-driver-sync:5.2.1")` to the root `build.gradle.kts` dependencies section.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/fr/maxlego08/essentials/storage/mongodb/MongoConnection.java \
  src/main/java/fr/maxlego08/essentials/storage/mongodb/MongoRepository.java \
  API/src/test/java/fr/maxlego08/essentials/api/server/MongoConfigTest.java
git commit -m "feat(mongo): connection wrapper + generic repository base class with 7 config tests"
```

---

### Task 4: MongoStorage Skeleton + MongoRepositories + Wire into ZStorageManager

**Files:**
- Create: `src/main/java/fr/maxlego08/essentials/storage/mongodb/MongoRepositories.java`
- Create: `src/main/java/fr/maxlego08/essentials/storage/mongodb/MongoStorage.java`
- Modify: `src/main/java/fr/maxlego08/essentials/storage/ZStorageManager.java`
- Modify: `build.gradle.kts` (add MongoDB driver compileOnly if needed)

**Interfaces:**
- Consumes: `MongoConnection`, `MongoRepository`, `StorageType.MONGO`, `StorageHelper`
- Produces: `MongoStorage implements IStorage`, `MongoRepositories`, `ZStorageManager` handling MONGO

This is a large task. The `MongoStorage` class implements all 90+ `IStorage` methods by delegating to `MongoRepositories`. Since the SQL repos use a `with(Class)` pattern, the Mongo approach uses a simpler direct delegation — each `MongoRepositories` field is public and accessed directly.

**Strategy:** `MongoStorage` will start with ALL methods throwing `UnsupportedOperationException` as stubs. Then Tasks 5-7 fill them in batch by batch. This keeps each task focused and independently testable.

- [ ] **Step 1: Add MongoDB driver to build.gradle.kts**

In `build.gradle.kts`, in the `dependencies` block (after the paper-api line), add:

```kotlin
    compileOnly("org.mongodb:mongodb-driver-sync:5.2.1")
```

- [ ] **Step 2: Create MongoRepositories**

```java
package fr.maxlego08.essentials.storage.mongodb;

import com.mongodb.client.MongoDatabase;
import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.storage.mongodb.repos.MongoUserRepository;
import fr.maxlego08.essentials.storage.mongodb.repos.MongoUserEconomyRepository;
import fr.maxlego08.essentials.storage.mongodb.repos.MongoUserHomeRepository;
import fr.maxlego08.essentials.storage.mongodb.repos.MongoUserHomeShareRepository;
import fr.maxlego08.essentials.storage.mongodb.repos.MongoUserSanctionRepository;
import fr.maxlego08.essentials.storage.mongodb.repos.MongoUserCooldownsRepository;
import fr.maxlego08.essentials.storage.mongodb.repos.MongoUserIgnoreRepository;
import fr.maxlego08.essentials.storage.mongodb.repos.MongoUserOptionRepository;
import fr.maxlego08.essentials.storage.mongodb.repos.MongoUserMailBoxRepository;
import fr.maxlego08.essentials.storage.mongodb.repos.MongoMailMessageRepository;
import fr.maxlego08.essentials.storage.mongodb.repos.MongoEconomyTransactionsRepository;
import fr.maxlego08.essentials.storage.mongodb.repos.MongoChatMessagesRepository;
import fr.maxlego08.essentials.storage.mongodb.repos.MongoPrivateMessagesRepository;
import fr.maxlego08.essentials.storage.mongodb.repos.MongoCommandsRepository;
import fr.maxlego08.essentials.storage.mongodb.repos.MongoUserPlayTimeRepository;
import fr.maxlego08.essentials.storage.mongodb.repos.MongoUserPowerToolsRepository;
import fr.maxlego08.essentials.storage.mongodb.repos.MongoServerStorageRepository;
import fr.maxlego08.essentials.storage.mongodb.repos.MongoVoteSiteRepository;
import fr.maxlego08.essentials.storage.mongodb.repos.MongoPlayerSlotRepository;
import fr.maxlego08.essentials.storage.mongodb.repos.MongoVaultItemRepository;
import fr.maxlego08.essentials.storage.mongodb.repos.MongoVaultRepository;
import fr.maxlego08.essentials.storage.mongodb.repos.MongoLinkAccountRepository;
import fr.maxlego08.essentials.storage.mongodb.repos.MongoLinkCodeRepository;
import fr.maxlego08.essentials.storage.mongodb.repos.MongoLinkHistoryRepository;
import fr.maxlego08.essentials.storage.mongodb.repos.MongoUserStepRepository;

public class MongoRepositories {

    public final MongoUserRepository users;
    public final MongoUserEconomyRepository economy;
    public final MongoUserHomeRepository homes;
    public final MongoUserHomeShareRepository homeShares;
    public final MongoUserSanctionRepository sanctions;
    public final MongoUserCooldownsRepository cooldowns;
    public final MongoUserIgnoreRepository ignores;
    public final MongoUserOptionRepository options;
    public final MongoUserMailBoxRepository mailBox;
    public final MongoMailMessageRepository mailMessages;
    public final MongoEconomyTransactionsRepository transactions;
    public final MongoChatMessagesRepository chatMessages;
    public final MongoPrivateMessagesRepository privateMessages;
    public final MongoCommandsRepository commands;
    public final MongoUserPlayTimeRepository playTime;
    public final MongoUserPowerToolsRepository powerTools;
    public final MongoServerStorageRepository serverStorage;
    public final MongoVoteSiteRepository votes;
    public final MongoPlayerSlotRepository playerSlots;
    public final MongoVaultItemRepository vaultItems;
    public final MongoVaultRepository vaults;
    public final MongoLinkAccountRepository linkAccounts;
    public final MongoLinkCodeRepository linkCodes;
    public final MongoLinkHistoryRepository linkHistory;
    public final MongoUserStepRepository steps;

    public MongoRepositories(EssentialsPlugin plugin, MongoDatabase database) {
        this.users = new MongoUserRepository(plugin, database);
        this.economy = new MongoUserEconomyRepository(plugin, database);
        this.homes = new MongoUserHomeRepository(plugin, database);
        this.homeShares = new MongoUserHomeShareRepository(plugin, database);
        this.sanctions = new MongoUserSanctionRepository(plugin, database);
        this.cooldowns = new MongoUserCooldownsRepository(plugin, database);
        this.ignores = new MongoUserIgnoreRepository(plugin, database);
        this.options = new MongoUserOptionRepository(plugin, database);
        this.mailBox = new MongoUserMailBoxRepository(plugin, database);
        this.mailMessages = new MongoMailMessageRepository(plugin, database);
        this.transactions = new MongoEconomyTransactionsRepository(plugin, database);
        this.chatMessages = new MongoChatMessagesRepository(plugin, database);
        this.privateMessages = new MongoPrivateMessagesRepository(plugin, database);
        this.commands = new MongoCommandsRepository(plugin, database);
        this.playTime = new MongoUserPlayTimeRepository(plugin, database);
        this.powerTools = new MongoUserPowerToolsRepository(plugin, database);
        this.serverStorage = new MongoServerStorageRepository(plugin, database);
        this.votes = new MongoVoteSiteRepository(plugin, database);
        this.playerSlots = new MongoPlayerSlotRepository(plugin, database);
        this.vaultItems = new MongoVaultItemRepository(plugin, database);
        this.vaults = new MongoVaultRepository(plugin, database);
        this.linkAccounts = new MongoLinkAccountRepository(plugin, database);
        this.linkCodes = new MongoLinkCodeRepository(plugin, database);
        this.linkHistory = new MongoLinkHistoryRepository(plugin, database);
        this.steps = new MongoUserStepRepository(plugin, database);
    }
}
```

**IMPORTANT:** Before this compiles, all 25 repo classes must exist. To keep the build green, create all 25 as STUBS first (just the constructor calling `super(plugin, database, "collectionName")`), then fill in their methods in Tasks 5-7.

- [ ] **Step 3: Create all 25 stub repository classes**

Create `src/main/java/fr/maxlego08/essentials/storage/mongodb/repos/` directory. For each repository, create a stub:

Template (example for `MongoUserRepository`):
```java
package fr.maxlego08.essentials.storage.mongodb.repos;

import com.mongodb.client.MongoDatabase;
import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.storage.mongodb.MongoRepository;

public class MongoUserRepository extends MongoRepository {
    public MongoUserRepository(EssentialsPlugin plugin, MongoDatabase database) {
        super(plugin, database, "users");
    }
}
```

Create all 25 stubs with these collection names:
- `MongoUserRepository` → `"users"`
- `MongoUserEconomyRepository` → `"user_economy"`
- `MongoUserHomeRepository` → `"homes"`
- `MongoUserHomeShareRepository` → `"home_shares"`
- `MongoUserSanctionRepository` → `"sanctions"`
- `MongoUserCooldownsRepository` → `"cooldowns"`
- `MongoUserIgnoreRepository` → `"ignores"`
- `MongoUserOptionRepository` → `"user_options"`
- `MongoUserMailBoxRepository` → `"mails"`
- `MongoMailMessageRepository` → `"mail_messages"`
- `MongoEconomyTransactionsRepository` → `"economy_transactions"`
- `MongoChatMessagesRepository` → `"chat_messages"`
- `MongoPrivateMessagesRepository` → `"private_messages"`
- `MongoCommandsRepository` → `"commands"`
- `MongoUserPlayTimeRepository` → `"play_time"`
- `MongoUserPowerToolsRepository` → `"power_tools"`
- `MongoServerStorageRepository` → `"server_storage"`
- `MongoVoteSiteRepository` → `"vote_sites"`
- `MongoPlayerSlotRepository` → `"player_slots"`
- `MongoVaultItemRepository` → `"vault_items"`
- `MongoVaultRepository` → `"vaults"`
- `MongoLinkAccountRepository` → `"link_accounts"`
- `MongoLinkCodeRepository` → `"link_codes"`
- `MongoLinkHistoryRepository` → `"link_history"`
- `MongoUserStepRepository` → `"steps"`

- [ ] **Step 4: Create MongoStorage skeleton**

```java
package fr.maxlego08.essentials.storage.mongodb;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.server.MongoConfiguration;
import fr.maxlego08.essentials.zutils.utils.StorageHelper;
import org.bukkit.Bukkit;

public class MongoStorage extends StorageHelper {

    private final MongoConnection connection;
    private final MongoRepositories repositories;

    public MongoStorage(EssentialsPlugin plugin) {
        super(plugin);
        MongoConfiguration config = plugin.getConfiguration().getMongoConfiguration();
        if (config == null) {
            plugin.getLogger().severe("MongoDB configuration not found! Set storage-type to MONGO and configure mongo-configuration.");
            Bukkit.getPluginManager().disablePlugin(plugin);
            this.connection = null;
            this.repositories = null;
            return;
        }

        this.connection = new MongoConnection(config);

        if (!this.connection.isValid()) {
            plugin.getLogger().severe("Unable to connect to MongoDB!");
            Bukkit.getPluginManager().disablePlugin(plugin);
            this.repositories = null;
            return;
        }

        plugin.getLogger().info("MongoDB connection established to " + config.database());
        this.repositories = new MongoRepositories(plugin, connection.getDatabase());
    }

    // TODO: Implement all IStorage methods in Tasks 5-7
}
```

Note: `MongoStorage` extends `StorageHelper` but does NOT yet implement `IStorage`. The `IStorage` interface implementation comes in Tasks 5-7. For now, this skeleton just establishes the connection.

- [ ] **Step 5: Wire MONGO into ZStorageManager**

In `ZStorageManager.java`, add `MONGO` to the switch:

```java
        this.iStorage = switch (this.storageType) {
            case HIKARICP, SQLITE, MYSQL, MARIADB -> new SqlStorage(plugin, this.storageType);
            case MONGO -> new MongoStorage(plugin);
            default -> new JsonStorage(plugin);
        };
```

Add import: `import fr.maxlego08.essentials.storage.mongodb.MongoStorage;`

- [ ] **Step 6: Build to verify compilation**

Run: `./gradlew build -x test --console=plain`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Run all tests**

Run: `./gradlew test --console=plain --no-daemon`
Expected: BUILD SUCCESSFUL

- [ ] **Step 8: Commit**

```bash
git add build.gradle.kts \
  src/main/java/fr/maxlego08/essentials/storage/mongodb/MongoRepositories.java \
  src/main/java/fr/maxlego08/essentials/storage/mongodb/MongoStorage.java \
  src/main/java/fr/maxlego08/essentials/storage/mongodb/repos/*.java \
  src/main/java/fr/maxlego08/essentials/storage/ZStorageManager.java
git commit -m "feat(mongo): storage skeleton, 25 stub repos, wired into ZStorageManager"
```

---

### Task 5: Implement Core Repositories + MongoStorage IStorage (Part 1)

This task implements the most critical repositories (users, economy, homes, sanctions, cooldowns) and their corresponding `IStorage` methods in `MongoStorage`.

**Files:**
- Modify: `src/main/java/fr/maxlego08/essentials/storage/mongodb/repos/MongoUserRepository.java`
- Modify: `src/main/java/fr/maxlego08/essentials/storage/mongodb/repos/MongoUserEconomyRepository.java`
- Modify: `src/main/java/fr/maxlego08/essentials/storage/mongodb/repos/MongoUserHomeRepository.java`
- Modify: `src/main/java/fr/maxlego08/essentials/storage/mongodb/repos/MongoUserSanctionRepository.java`
- Modify: `src/main/java/fr/maxlego08/essentials/storage/mongodb/repos/MongoUserCooldownsRepository.java`
- Modify: `src/main/java/fr/maxlego08/essentials/storage/mongodb/MongoStorage.java` (add `implements IStorage`)

**Approach:** Read each corresponding SQL repository to understand the methods, then implement the MongoDB equivalent. The `MongoStorage` class adds `implements IStorage` and implements the methods that correspond to these core repos. Methods for repos not yet implemented throw `UnsupportedOperationException` — they'll be filled in Tasks 6-7.

For each repository, read the corresponding SQL repo in `src/main/java/fr/maxlego08/essentials/storage/database/repositeries/` and implement the MongoDB equivalent using `MongoRepository`'s CRUD helpers.

Key patterns:
- SQL `upsert(table -> { table.uuid("unique_id", uuid).primary(); ... })` → MongoDB `upsert(byUuid(uuid), toDocument(dto))`
- SQL `select(DTO.class, table -> table.where("unique_id", uuid))` → MongoDB `find(byUuid(uuid), DTO.class)`
- SQL `update(table -> { table.string("field", value); table.where("unique_id", uuid); })` → MongoDB `collection().updateOne(byUuid(uuid), new Document("$set", new Document("field", value)))`
- SQL `delete(table -> table.where("unique_id", uuid))` → MongoDB `delete(byUuid(uuid))`

- [ ] **Step 1: Implement MongoUserRepository**

Read `src/main/java/fr/maxlego08/essentials/storage/database/repositeries/UserRepository.java` to understand the methods. Implement the MongoDB equivalents: `upsert(UUID, String)`, `upsert(User)`, `selectUUIDs()`, `clearExpiredSanctions()`, `getName(UUID)`, `getUniqueId(String)`, `selectCount()`.

- [ ] **Step 2: Implement MongoUserEconomyRepository**

Read `src/main/java/fr/maxlego08/essentials/storage/database/repositeries/UserEconomyRepository.java`. Implement: `upsert(UUID, Economy, BigDecimal)`, `reset(Economy, BigDecimal)`, `select(UUID)`, `getAll()`.

- [ ] **Step 3: Implement MongoUserHomeRepository**

Read `src/main/java/fr/maxlego08/essentials/storage/database/repositeries/UserHomeRepository.java`. Implement: `upsert(UUID, Home)`, `delete(UUID, String)`, `select(UUID)`, `selectPublic()`, `updateSocial(UUID, Home)`.

- [ ] **Step 4: Implement MongoUserSanctionRepository**

Read `src/main/java/fr/maxlego08/essentials/storage/database/repositeries/UserSanctionRepository.java`. Implement: `insert(Sanction)`, `updateBan(UUID, int)`, `updateMute(UUID, int)`, `getActiveBan()`, `getActiveMute()`, `getSanctions(UUID)`, `isBan(UUID)`, `isMute(UUID)`.

- [ ] **Step 5: Implement MongoUserCooldownsRepository**

Read `src/main/java/fr/maxlego08/essentials/storage/database/repositeries/UserCooldownsRepository.java`. Implement: `upsert(UUID, String, long)`, `delete(UUID, String)`, `deleteExpiredCooldowns()`, `select(UUID)`.

- [ ] **Step 6: Add `implements IStorage` to MongoStorage and implement core methods**

Add `implements IStorage` to the class declaration. Read `SqlStorage.java` to find all methods that delegate to the core repos (users, economy, homes, sanctions, cooldowns). Implement those methods by delegating to `this.repositories.users`, `.economy`, `.homes`, `.sanctions`, `.cooldowns`.

For methods that use repos not yet implemented, add:
```java
    throw new UnsupportedOperationException("Not yet implemented for MongoDB: " + "methodName");
```

- [ ] **Step 7: Build to verify compilation**

Run: `./gradlew build -x test --console=plain`
Expected: BUILD SUCCESSFUL

- [ ] **Step 8: Commit**

```bash
git add src/main/java/fr/maxlego08/essentials/storage/mongodb/repos/MongoUser*.java \
  src/main/java/fr/maxlego08/essentials/storage/mongodb/MongoStorage.java
git commit -m "feat(mongo): core repos (users, economy, homes, sanctions, cooldowns) + IStorage implementation"
```

---

### Task 6: Implement Secondary Repositories + MongoStorage IStorage (Part 2)

This task implements the secondary repositories and their corresponding `IStorage` methods.

**Files:**
- Modify: `src/main/java/fr/maxlego08/essentials/storage/mongodb/repos/MongoUserIgnoreRepository.java`
- Modify: `src/main/java/fr/maxlego08/essentials/storage/mongodb/repos/MongoUserOptionRepository.java`
- Modify: `src/main/java/fr/maxlego08/essentials/storage/mongodb/repos/MongoUserMailBoxRepository.java`
- Modify: `src/main/java/fr/maxlego08/essentials/storage/mongodb/repos/MongoMailMessageRepository.java`
- Modify: `src/main/java/fr/maxlego08/essentials/storage/mongodb/repos/MongoEconomyTransactionsRepository.java`
- Modify: `src/main/java/fr/maxlego08/essentials/storage/mongodb/repos/MongoChatMessagesRepository.java`
- Modify: `src/main/java/fr/maxlego08/essentials/storage/mongodb/repos/MongoPrivateMessagesRepository.java`
- Modify: `src/main/java/fr/maxlego08/essentials/storage/mongodb/repos/MongoCommandsRepository.java`
- Modify: `src/main/java/fr/maxlego08/essentials/storage/mongodb/repos/MongoUserPlayTimeRepository.java`
- Modify: `src/main/java/fr/maxlego08/essentials/storage/mongodb/repos/MongoUserPowerToolsRepository.java`
- Modify: `src/main/java/fr/maxlego08/essentials/storage/mongodb/MongoStorage.java`

**Approach:** Same as Task 5 — read each corresponding SQL repo, implement the MongoDB equivalent, then wire the methods into MongoStorage.

- [ ] **Step 1: Read and implement each secondary repository**

For each of the 10 repos listed above, read the corresponding SQL repo in `src/main/java/fr/maxlego08/essentials/storage/database/repositeries/` and implement the MongoDB equivalent.

- [ ] **Step 2: Implement the corresponding IStorage methods in MongoStorage**

Find all IStorage methods that delegate to these repos and implement them, replacing `UnsupportedOperationException` stubs.

- [ ] **Step 3: Build to verify compilation**

Run: `./gradlew build -x test --console=plain`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add src/main/java/fr/maxlego08/essentials/storage/mongodb/repos/*.java \
  src/main/java/fr/maxlego08/essentials/storage/mongodb/MongoStorage.java
git commit -m "feat(mongo): secondary repos (ignores, options, mails, messages, commands, playtime, powertools)"
```

---

### Task 7: Implement Remaining Repositories + Wire + Test + Changelog

**Files:**
- Modify: All remaining stub repos in `src/main/java/fr/maxlego08/essentials/storage/mongodb/repos/`
- Modify: `src/main/java/fr/maxlego08/essentials/storage/mongodb/MongoStorage.java`
- Modify: `changelog.md`

- [ ] **Step 1: Read and implement all remaining repositories**

The remaining repos (10): `MongoServerStorageRepository`, `MongoVoteSiteRepository`, `MongoPlayerSlotRepository`, `MongoVaultItemRepository`, `MongoVaultRepository`, `MongoLinkAccountRepository`, `MongoLinkCodeRepository`, `MongoLinkHistoryRepository`, `MongoUserStepRepository`, `MongoUserHomeShareRepository`.

For each, read the corresponding SQL repo and implement the MongoDB equivalent.

- [ ] **Step 2: Implement all remaining IStorage methods in MongoStorage**

Replace ALL remaining `UnsupportedOperationException` stubs with real implementations. After this step, NO method should throw `UnsupportedOperationException`.

- [ ] **Step 3: Build to verify compilation**

Run: `./gradlew build -x test --console=plain`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Run all tests**

Run: `./gradlew test --console=plain --no-daemon`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Verify no UnsupportedOperationException stubs remain**

Search for `UnsupportedOperationException` in `MongoStorage.java`:
Run: `grep -n "UnsupportedOperationException" src/main/java/fr/maxlego08/essentials/storage/mongodb/MongoStorage.java`
Expected: no output (all methods implemented)

- [ ] **Step 6: Add changelog entry**

In `changelog.md`, under `# 1.2.0.0`, add:

```markdown
## Database backends

- **Redis activated** — `server-type: REDIS` now activates the `RedisServer` for cross-server messaging (chat, kicks, cooldowns, private messages, player list sync); previously the activation code was commented out
- **Redis hook paper-api fixed** — updated from stale `1.21.5` to `26.2.build.119-stable` matching the root project
- **MongoDB storage backend** — `storage-type: MONGO` now supported as a full `IStorage` implementation with 25 MongoDB repositories mirroring the SQL Repository pattern
- **MongoDB driver** — `org.mongodb:mongodb-driver-sync:5.2.1` added to `plugin.yml` libraries (runtime download by Paper, not shaded)
- **MongoDB config** — `mongo-configuration` section in `config.yml` supports full URI or individual `host`/`port`/`user`/`password`/`database` fields
- **Config self-healing** — `mongo-configuration` section auto-appended by `ConfigHealer` on version mismatch
```

- [ ] **Step 7: Full build + test**

Run: `./gradlew build --console=plain --no-daemon`
Expected: BUILD SUCCESSFUL — all tests pass

- [ ] **Step 8: Commit**

```bash
git add src/main/java/fr/maxlego08/essentials/storage/mongodb/repos/*.java \
  src/main/java/fr/maxlego08/essentials/storage/mongodb/MongoStorage.java \
  changelog.md
git commit -m "feat(mongo): remaining repos, full IStorage implementation, changelog"
```
