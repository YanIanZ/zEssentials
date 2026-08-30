# Network/Social Layer — Design Spec

## §G — Goal

Enable cross-server social features for the zEssentials plugin running behind
BungeeCord/Velocity/Waterfall proxies:
- **Global chat relay** — public chat visible across all servers
- **Friends** — friend requests, accept/decline, online status, cross-server notifications
- **Guild** — create/join/leave, members, ranks, guild chat, cross-server
- **Party** — create/invite/join/leave, leader, party chat, max size, cross-server

Cross-server transport: BungeeCord plugin messaging channel for chat relay,
Redis pub/sub for social events. Cross-server data: MongoDB (uses the full
IStorage backend from sub-project #7).

## §C — Context

### Existing infrastructure reused

- **BungeeChatModule** (`dev.yanianz.essentials.network`) — already implements
  BungeeCord plugin messaging for chat relay. Needs activation + Redis support
  added.
- **Redis pub/sub** — activated in #7. `RedisServer` already handles chat,
  kick, cooldown messages via pub/sub. Can be extended for friends/guild/party.
- **MongoDB IStorage** — full implementation from #7. New collections
  (`friends`, `guilds`, `guild_members`, `parties`) can be added.
- **Module system** — each feature is a separate `ZModule`.
- **Message enum** — new messages added per feature.
- **Hook pattern** — events fire on friend add/remove, guild join/leave, party invite.

### Key constraints

- All cross-server data must go through MongoDB (or Redis for ephemeral state).
- All cross-server events use BungeeCord plugin messaging or Redis pub/sub.
- Each feature (friends, guild, party) is self-contained but shares the
  `NetworkManager` for cross-server event dispatch.
- Folia-safe: all async operations on the region thread.

## §I — Interfaces

### NetworkManager (dev.yanianz.essentials.network)

```java
class NetworkManager {
    void sendToServer(String server, String channel, String data);
    void broadcastToNetwork(String channel, String data);
    void registerListener(String channel, Consumer<String> handler);
    boolean isOnNetwork();  // true if bungeechat or redis is enabled
    String getLocalServerName();
}
```

### GlobalChatModule (dev.yanianz.essentials.network.chat)

Extends existing BungeeChatModule. Activates by default when bungeechat is enabled.
Supports both BungeeCord plugin messaging and Redis pub/sub.

### FriendsModule (dev.yanianz.essentials.friends)

```java
class FriendsModule extends ZModule {
    boolean enable;
    int maxFriends;  // default 50
    
    void sendRequest(Player from, UUID to);
    void acceptRequest(UUID from, UUID to);
    void declineRequest(UUID from, UUID to);
    void removeFriend(UUID player, UUID friend);
    List<UUID> getFriends(UUID player);
    boolean isFriend(UUID player, UUID other);
    boolean hasPendingRequest(UUID from, UUID to);
}
```

### GuildModule (dev.yanianz.essentials.guild)

```java
class GuildModule extends ZModule {
    boolean enable;
    int maxMembers;  // default 25
    int maxNameLength;
    
    void createGuild(Player leader, String name, String tag);
    void disbandGuild(int guildId);
    void invitePlayer(int guildId, UUID player);
    void joinGuild(int guildId, UUID player);
    void leaveGuild(int guildId, UUID player);
    void kickMember(int guildId, UUID player);
    void setRank(int guildId, UUID player, GuildRank rank);
    void sendGuildChat(int guildId, UUID sender, String message);
    int getPlayerGuildId(UUID player);
}
```

### PartyModule (dev.yanianz.essentials.party)

```java
class PartyModule extends ZModule {
    boolean enable;
    int maxSize;  // default 8
    
    void createParty(Player leader);
    void disbandParty(int partyId);
    void invitePlayer(int partyId, UUID player);
    void joinParty(int partyId, UUID player);
    void leaveParty(int partyId, UUID player);
    void kickMember(int partyId, UUID player);
    void transferLeader(int partyId, UUID newLeader);
    void sendPartyChat(int partyId, UUID sender, String message);
    int getPlayerPartyId(UUID player);
}
```

## §V — Invariants

1. **Cross-server chat uses both BungeeCord messaging and Redis.** BungeeCord
   is tried first (lower latency, direct); Redis is the fallback.
2. **Friends/guild/party data is stored in MongoDB** via the IStorage interface
   from #7. New collections: `friends`, `guilds`, `guild_members`, `parties`.
3. **Online status is tracked via Redis** (ephemeral). When a player joins,
   `network:online:<uuid>` is set with a TTL. When they leave, it's deleted.
4. **Cross-server friend notifications** — when a friend comes online, the
   notification is sent via Redis pub/sub. Offline notifications stored in
   MongoDB until the player logs in.
5. **Guild chat is cross-server** — `/gc <message>` sends to all guild members
   on any server, delivered via Redis pub/sub.
6. **Party chat is cross-server** — `/pc <message>` sends to all party members
   on any server, delivered via Redis pub/sub.
7. **Friend requests expire after 7 days** — stored in MongoDB with timestamp.
8. **Guild names are unique** — validated on creation, case-insensitive.
9. **Party leader transfer** — when leader leaves, leadership transfers to
   longest-tenured member, or party disbands if empty.
10. **Permission-based caps** — VIP+ players can have more friends/larger guilds
    via permissions.

## §T — Tasks (4 phases)

### Phase 1: Global Chat (3 tasks)
| # | Task | Files | Tests |
|---|------|-------|-------|
| 1.1 | NetworkManager + BungeeCord/Redis transport | `NetworkManager.java`, `BungeeChatModule.java` update | Build passes |
| 1.2 | /globalchat toggle command + messages | `CommandGlobalChat.java`, `Message.java`, `messages.yml` | Build passes |
| 1.3 | Phase 1 changelog | `changelog.md` | All pass |

### Phase 2: Friends (4 tasks)
| # | Task | Files | Tests |
|---|------|-------|-------|
| 2.1 | MongoDB friends collections + storage methods | `MongoFriendRepository.java`, `IStorage.java` update | Build passes |
| 2.2 | FriendsModule + cross-server online notifications | `FriendsModule.java`, `FriendRequest.java`, `FriendEvent.java` | Unit tests |
| 2.3 | FriendListener (events: join, quit, request accept/decline) | `FriendListener.java` | Build passes |
| 2.4 | /friend add/remove/list/accept/decline commands + messages | `CommandFriend.java`, `CommandFriendList.java`, `Message.java`, `messages.yml` | Build passes |

### Phase 3: Guild (4 tasks)
| # | Task | Files | Tests |
|---|------|-------|-------|
| 3.1 | MongoDB guild collections + storage methods | `MongoGuildRepository.java`, `MongoGuildMemberRepository.java`, `IStorage.java` update | Build passes |
| 3.2 | GuildModule + GuildRank enum + cross-server chat | `GuildModule.java`, `GuildRank.java`, `GuildEvent.java` | Unit tests |
| 3.3 | GuildListener (events: create, disband, join, leave, kick, rank) | `GuildListener.java` | Build passes |
| 3.4 | /guild create/disband/invite/join/leave/chat commands + messages | `CommandGuild.java`, `CommandGuildChat.java`, `Message.java`, `messages.yml` | Build passes |

### Phase 4: Party (4 tasks)
| # | Task | Files | Tests |
|---|------|-------|-------|
| 4.1 | MongoDB party collections + storage methods | `MongoPartyRepository.java`, `IStorage.java` update | Build passes |
| 4.2 | PartyModule + cross-server chat + leader transfer | `PartyModule.java`, `PartyEvent.java` | Unit tests |
| 4.3 | PartyListener (events: create, disband, join, leave, kick) | `PartyListener.java` | Build passes |
| 4.4 | /party create/disband/invite/join/leave/chat commands + messages | `CommandParty.java`, `CommandPartyChat.java`, `Message.java`, `messages.yml` | Build passes |

### Phase 5: Final
| # | Task | Files | Tests |
|---|------|-------|-------|
| 5.1 | Full integration test + final changelog | `changelog.md` | All pass |

## §B — Bugs prevented

| # | Bug | Invariant |
|---|-----|-----------|
| 1 | Cross-server chat message lost | §V.1 — dual transport (BungeeCord + Redis fallback) |
| 2 | Friend online notification not received | §V.3 — Redis online status + §V.4 — cross-server notification |
| 3 | Guild chat message not delivered to off-network members | §V.5 — Redis pub/sub |
| 4 | Friend request accepted twice | §V.7 — request expiry + one-time accept |
| 5 | Party leader leaves, party stuck | §V.9 — auto leader transfer or disband |
| 6 | Guild name collision | §V.8 — unique validation |

## Config schema (modules/network/config.yml)

```yaml
config-version: 1
enable: true

# Cross-server transport
transport: bungeecord  # bungeecord, redis, both

# Global chat
global-chat:
  enable: true
  format: "&7[&b%server%&7] &f%player%&8: &7%message%"

# Friends
friends:
  enable: true
  max-friends: 50
  request-expiry-days: 7

# Guild
guild:
  enable: true
  max-members: 25
  max-name-length: 16
  chat-format: "&2[&aGUILD&2] &f%player%&8: &7%message%"

# Party
party:
  enable: true
  max-size: 8
  chat-format: "&d[&5PARTY&d] &f%player%&8: &7%message%"
```

## Data flow (Friends example)

```
Player A runs /friend add PlayerB
  → FriendsModule.sendRequest(A, B)
  → MongoDB: insert into friends collection with status=PENDING, expires=now+7d
  → Redis pub/sub: publish "friend-request" to server where B is online
  → If B online: FriendListener receives, sends notification to B
  → If B offline: B sees notification on next login

B runs /friend accept A
  → FriendsModule.acceptRequest(A, B)
  → MongoDB: update status=ACCEPTED for both directions
  → Redis pub/sub: publish "friend-accepted" to server where A is online
  → A sees notification

A logs in
  → NetworkManager publishes "player-online" to Redis with A's UUID
  → All servers' FriendListener receives
  → For each friend of A, if they're online, notify them
```

## Permission nodes

- `essentials.network` — base for /g, /friend, /guild, /party
- `essentials.network.globalchat` — /g toggle
- `essentials.friends.max.<n>` — override max friends
- `essentials.guild.create` — create guild
- `essentials.guild.max.<n>` — override max members
- `essentials.party.create` — create party
- `essentials.party.max.<n>` — override max size
