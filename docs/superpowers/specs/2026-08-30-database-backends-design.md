# Database Backends — Design Spec

## §G — Goal

Activate the existing but commented-out Redis hook for cross-server messaging,
and add MongoDB as a full storage backend (StorageType.MONGO) implementing
the IStorage interface, so the network/social layer (#6) can use either Redis
pub/sub for messaging and MongoDB for persistent cross-server data.

## §C — Context

### Redis (existing, commented out)

- `Hooks/Redis/` — full implementation exists with `RedisServer implements EssentialsServer`
- `ZEssentialsPlugin.java:202-205` — activation code is COMMENTED OUT
- `config.yml` has `server-type: PAPER|REDIS` and `redis-configuration` section
- `Hooks/Redis/build.gradle.kts` uses paper-api `1.21.5` (stale — root uses `26.2`)
- Jedis 4.3.1 is the Redis client library
- Handles: chat messages, private messages, kicks, cooldowns, chat clear/toggle,
  player list sync across servers

### Storage (existing)

- `StorageType` enum: `JSON, MYSQL, MARIADB, SQLITE, HIKARICP`
- `ZStorageManager` creates `SqlStorage` or `JsonStorage` based on type
- `IStorage` interface — 90+ methods defining all data operations
- `SqlStorage implements IStorage` — delegates to `Repositories` which wraps
  25 `Repository` subclasses, each using Sarah ORM (SQL-only)
- `Repository` base class: wraps `DatabaseConnection`, provides `upsert/update/
  insert/delete/select` helpers via `SchemaBuilder`
- DTOs: `UserDTO`, `EconomyDTO`, `HomeDTO`, `SanctionDTO`, etc. — simple POJOs/records
- `plugin.yml` libraries: `org.mariadb.jdbc:mariadb-java-client:3.5.6` (runtime download)

### MongoDB (new)

- MongoDB Java Driver (sync) — `org.mongodb:mongodb-driver-sync`
- NoSQL: collections (≈ tables) with BSON documents (≈ rows)
- Sarah ORM is SQL-only, so `MongoStorage` cannot reuse `Repository`
- A new `MongoRepository` base class mirrors the pattern but uses
  `MongoCollection<Document>` instead of `SchemaBuilder`

## §I — Interfaces

### StorageType (modify existing enum)

```java
public enum StorageType {
    JSON,
    MYSQL,
    MARIADB,
    SQLITE,
    HIKARICP,
    MONGO,  // new
}
```

### MongoConfiguration (new, in API)

```java
public record MongoConfiguration(
    String uri,        // "mongodb://user:pass@host:port/" or null
    String host,
    int port,
    String user,       // empty = no auth
    String password,
    String database
) {}
```

### MongoStorage (new, implements IStorage)

```java
public class MongoStorage extends StorageHelper implements IStorage {
    // Mirrors SqlStorage but uses MongoRepository instead of Repositories
    MongoConnection connection;
    MongoRepositories repositories;

    // All 90+ IStorage methods delegated to repositories
    // Same caching pattern as SqlStorage (TypeSafeCache)
}
```

### MongoConnection (new)

```java
public class MongoConnection {
    MongoClient client;
    MongoDatabase database;

    MongoConnection(MongoConfiguration config) // creates MongoClient
    MongoDatabase getDatabase()
    boolean isValid()
    void close()
}
```

### MongoRepository (new base class)

```java
public abstract class MongoRepository extends ZUtils {
    MongoDatabase database;
    String collectionName;  // e.g. "users", "homes", "sanctions"

    MongoCollection<Document> collection()  // returns database.getCollection(name)

    // Helper methods mirroring Repository:
    void upsert(Document filter, Document update)  // replaceOne with upsert
    void insert(Document document)
    void delete(Document filter)
    Document findOne(Document filter)
    List<Document> find(Document filter)
    List<Document> findAll()
    long count(Document filter)
}
```

### MongoRepositories (new, aggregates all mongo repos)

```java
public class MongoRepositories {
    MongoUserRepository users;
    MongoUserEconomyRepository economy;
    MongoUserHomeRepository homes;
    MongoUserSanctionRepository sanctions;
    // ... one per existing SQL repository (25 total)
}
```

## §V — Invariants

1. **MONGO is a full IStorage implementation** — every method in IStorage is
   implemented. No "not supported" exceptions. If a method exists in SqlStorage,
   it has a MongoDB equivalent.
2. **Each SQL table maps to one MongoDB collection** — collection name = table
   name without prefix (e.g. `zessentials_users` → `users`). The table prefix
   concept is replaced by the database name in MongoDB.
3. **UUIDs are the primary key** — stored as a String field named `uuid` in
   every document that represents a user-owned entity. Indexed for fast lookups.
4. **MongoStorage uses the same caching as SqlStorage** — `TypeSafeCache` for
   in-memory caching to minimize database round-trips.
5. **MongoDB connection is validated on startup** — if the connection fails,
   the plugin disables itself (same behavior as SqlStorage).
6. **Redis activation is backward-compatible** — setting `server-type: PAPER`
   (the default) changes nothing; setting `server-type: REDIS` activates
   `RedisServer` for cross-server messaging.
7. **Redis hook paper-api version is updated to match root** — `26.2.build.119-stable`
   (currently stale at `1.21.5`).
8. **MongoDB driver is a runtime library** — declared in `plugin.yml` libraries,
   downloaded by Paper's library loader (same pattern as MariaDB). Not shaded.
9. **MongoConfiguration is parsed from config.yml** — `mongo-configuration`
   section with `uri` (optional connection string) or individual fields
   (`host`, `port`, `user`, `password`, `database`).
10. **Config is self-healing** — `config-version` key updated; `mongo-configuration`
    section added to `config.yml` by ConfigHealer.

## §T — Tasks

| # | Task | Files | Tests |
|---|------|-------|-------|
| 1 | Redis activation | `ZEssentialsPlugin.java`, `Hooks/Redis/build.gradle.kts` | Build passes |
| 2 | StorageType + config + MongoConfiguration | `StorageType.java`, `config.yml`, `MainConfiguration.java`, `MongoConfiguration.java` | ConfigVersionCoverageTest auto-covers |
| 3 | MongoConnection + MongoRepository base | `MongoConnection.java`, `MongoRepository.java` | Unit test: connection config parsing |
| 4 | MongoStorage skeleton + MongoRepositories | `MongoStorage.java`, `MongoRepositories.java` | Build passes |
| 5 | Implement core repositories (users, economy, homes, sanctions) | `MongoUserRepository.java`, `MongoUserEconomyRepository.java`, `MongoUserHomeRepository.java`, `MongoUserSanctionRepository.java` | Build passes |
| 6 | Implement remaining repositories (20+) | One `Mongo*.java` per existing SQL repo | Build passes |
| 7 | Wire MongoStorage into ZStorageManager + plugin.yml libraries | `ZStorageManager.java`, `plugin.yml` | Build + test pass |
| 8 | Tests + changelog | `MongoConfigTest.java`, `changelog.md` | All pass |

## §B — Bugs prevented

| # | Bug | Invariant |
|---|-----|-----------|
| 1 | MongoDB connection not validated, plugin runs with broken DB | §V.5 |
| 2 | Redis activated without config, crashes on missing redis-configuration | §V.6 |
| 3 | Stale paper-api in Redis hook causes classpath mismatch | §V.7 |
| 4 | MongoStorage missing IStorage methods, runtime AbstractMethodError | §V.1 |
| 5 | UUIDs stored as non-indexed fields, slow lookups | §V.3 |

## Config schema additions (config.yml)

```yaml
storage-type: MONGO  # new option in the existing storage-type comment

mongo-configuration:
  # Full connection URI (overrides individual fields if set)
  uri: ""
  # Individual fields (used when uri is empty)
  host: localhost
  port: 27017
  user: ""
  password: ""
  database: zessentials
```

## plugin.yml additions

```yaml
libraries:
  - 'org.mariadb.jdbc:mariadb-java-client:3.5.6'
  - 'org.mongodb:mongodb-driver-sync:5.2.1'
```

## Data mapping (SQL table → MongoDB collection)

| SQL Table (with prefix) | Collection | Key Fields | Indexes |
|--------------------------|------------|------------|---------|
| zessentials_users | users | uuid (unique) | uuid, name |
| zessentials_user_economy | user_economy | uuid, economy | uuid, economy |
| zessentials_user_homes | homes | uuid, name | uuid, (uuid, name) |
| zessentials_user_home_shares | home_shares | owner_uuid, home_name, target_uuid | owner_uuid |
| zessentials_user_sanctions | sanctions | uuid, type, active | uuid, active |
| zessentials_user_cooldowns | cooldowns | uuid, name | uuid |
| zessentials_user_ignores | ignores | uuid, ignored_uuid | uuid |
| zessentials_user_options | user_options | uuid, option | uuid |
| zessentials_user_mails | mails | uuid | uuid |
| zessentials_mail_messages | mail_messages | receiver_uuid | receiver_uuid |
| zessentials_economy_transactions | economy_transactions | from_uuid, to_uuid | to_uuid |
| zessentials_chat_messages | chat_messages | uuid | uuid |
| zessentials_private_messages | private_messages | sender, receiver | receiver |
| zessentials_commands | commands | uuid | uuid |
| zessentials_user_power_tools | power_tools | uuid | uuid |
| zessentials_user_play_time | play_time | uuid | uuid |
| zessentials_user_steps | steps | uuid, step | uuid |
| zessentials_vote_sites | vote_sites | uuid | uuid |
| zessentials_vaults | vaults | uuid | uuid |
| zessentials_vault_items | vault_items | uuid, vault_id, slot | uuid |
| zessentials_player_slots | player_slots | uuid | uuid |
| zessentials_server_storage | server_storage | key (unique) | key |
| zessentials_link_accounts | link_accounts | uuid | uuid |
| zessentials_link_codes | link_codes | code | code |
| zessentials_link_history | link_history | uuid | uuid |

### Document shape example (users collection)

```json
{
  "_id": ObjectId("..."),
  "uuid": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Steve",
  "first_join": 1693502400000,
  "last_join": 1694000000000,
  "playtime": 3600000,
  "options": { "fly": true, "god": false },
  "cooldowns": { "home": 1693502500000 }
}
```

### Approach for embedded vs. separate collections

Simple one-to-one relationships (user → options, user → cooldowns) are embedded
as sub-documents within the user document for faster reads. One-to-many
relationships (user → homes, user → sanctions) use separate collections with
a `uuid` foreign key field.
