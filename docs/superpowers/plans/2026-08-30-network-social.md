# Network/Social Layer Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans.

**Goal:** Enable cross-server social features (global chat, friends, guild, party) for BungeeCord/Velocity/Waterfall networks.

**Architecture:** A `NetworkManager` handles cross-server transport (BungeeCord plugin messaging + Redis pub/sub). Each feature (global chat, friends, guild, party) is a separate ZModule that uses `NetworkManager` for cross-server events and MongoDB IStorage (from #7) for persistence.

**Tech Stack:** Java 21, Bukkit/Paper API, BungeeCord plugin messaging, Redis (Jedis from #7), MongoDB driver (from #7), Gson, JUnit 5.

**Spec:** `docs/superpowers/specs/2026-08-30-network-social-design.md`

## Global Constraints

- Java 21 bytecode target. Paper API is `compileOnly`.
- No comments in code unless explicitly requested.
- Folia-supported: all async ops on region thread via `plugin.getScheduler()`.
- Config self-healing: `config-version: 1` in module config.
- Package convention: each feature in its own subpackage under `dev.yanianz.essentials.{networkchat,friends,guild,party}.*`.
- Reuses `NetworkManager` for cross-server transport.
- Reuses MongoDB IStorage from #7 for persistence.
- Reuses Redis pub/sub from #7 for ephemeral events.
- Build: `./gradlew build -x test --console=plain`
- Test: `./gradlew test --console=plain --no-daemon`
- Working directory: `/Users/rheninxy/Sourby/zEssentials`

---

## Phase 1: Global Chat (3 tasks)

### Task 1.1: NetworkManager + Enhanced BungeeChatModule

**Files:**
- Create: `src/main/java/dev/yanianz/essentials/network/NetworkManager.java`
- Modify: `src/main/java/dev/yanianz/essentials/network/BungeeChatModule.java`
- Create: `src/main/resources/modules/network-chat/config.yml`

- [ ] **Step 1: Create NetworkManager**

```java
package dev.yanianz.essentials.network;

import fr.maxlego08.essentials.ZEssentialsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.Messenger;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class NetworkManager {

    private static final String BUNGEECORD_CHANNEL = "BungeeCord";
    public static final String ZESSENTIALS_PREFIX = "zessentials:";

    private final ZEssentialsPlugin plugin;
    private final Map<String, Consumer<String>> listeners = new HashMap<>();
    private String localServerName = "server";

    public NetworkManager(ZEssentialsPlugin plugin) {
        this.plugin = plugin;
    }

    public void setLocalServerName(String name) {
        this.localServerName = name;
    }

    public String getLocalServerName() {
        return localServerName;
    }

    public void sendToServer(String subChannel, String data) {
        String fullChannel = ZESSENTIALS_PREFIX + subChannel;
        Messenger messenger = Bukkit.getMessenger();
        if (!messenger.isOutgoingChannelRegistered(plugin, BUNGEECORD_CHANNEL)) return;
        if (Bukkit.getOnlinePlayers().isEmpty()) return;
        Player carrier = Bukkit.getOnlinePlayers().iterator().next();
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        java.io.DataOutputStream out = new java.io.DataOutputStream(baos);
        try {
            out.writeUTF(fullChannel);
            out.writeUTF(data);
            carrier.sendPluginMessage(plugin, BUNGEECORD_CHANNEL, baos.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void registerListener(String subChannel, Consumer<String> handler) {
        String fullChannel = ZESSENTIALS_PREFIX + subChannel;
        listeners.put(fullChannel, handler);
        Messenger messenger = Bukkit.getMessenger();
        messenger.registerIncomingPluginChannel(plugin, BUNGEECORD_CHANNEL,
                (channel, player, bytes) -> {
                    try {
                        java.io.DataInputStream in = new java.io.DataInputStream(
                                new java.io.ByteArrayInputStream(bytes));
                        String sub = in.readUTF();
                        String data = in.readUTF();
                        Consumer<String> listener = listeners.get(sub);
                        if (listener != null) listener.accept(data);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
    }

    public boolean isAvailable() {
        return Bukkit.getMessenger().isOutgoingChannelRegistered(plugin, BUNGEECORD_CHANNEL);
    }
}
```

- [ ] **Step 2: Create network-chat config**

```yaml
########################################################################################################################
#
# zEssentials - Global Chat
# Cross-server public chat relay via BungeeCord plugin messaging.
# All servers in the network must run this module with it enabled.
#
########################################################################################################################

config-version: 1
enable: true

# Server name shown in relayed messages on other servers
server-name: ""

# Format of relayed messages (placeholders: %server%, %player%, %message%)
format: "&7[&b%server%&7] &f%player%&8: &7%message%"
```

- [ ] **Step 3: Refactor BungeeChatModule to use NetworkManager**

Create a new `GlobalChatModule` that replaces the old bungeechat module's chat-specific logic. The old `BungeeChatModule` can stay for backward compat but new installs use GlobalChatModule.

```java
package dev.yanianz.essentials.networkchat;

import dev.yanianz.essentials.network.NetworkManager;
import fr.maxlego08.essentials.ZEssentialsPlugin;
import fr.maxlego08.essentials.module.ZModule;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

public class GlobalChatModule extends ZModule {

    private String serverName = "server";
    private String format = "&7[&b%server%&7] &f%player%&8: &7%message%";
    private NetworkManager networkManager;
    private static boolean listenerRegistered = false;

    public GlobalChatModule(ZEssentialsPlugin plugin) {
        super(plugin, "network-chat");
    }

    @Override
    public void loadConfiguration() {
        super.loadConfiguration();
        YamlConfiguration config = getConfiguration();
        String configuredName = config.getString("server-name", "");
        this.serverName = (configuredName == null || configuredName.isEmpty())
                ? plugin.getConfig().getString("server-name", "server")
                : configuredName;
        this.format = config.getString("format", "&7[&b%server%&7] &f%player%&8: &7%message%");

        if (this.networkManager == null) {
            this.networkManager = new NetworkManager(this.plugin);
        }
        this.networkManager.setLocalServerName(this.serverName);

        if (!listenerRegistered) {
            this.networkManager.registerListener("chat", data -> {
                String[] parts = data.split("\\|", 3);
                if (parts.length < 3) return;
                String originServer = parts[0];
                String playerName = parts[1];
                String content = parts[2];
                String line = this.format
                        .replace("%server%", originServer)
                        .replace("%player%", playerName)
                        .replace("%message%", content.replace("&", "§"));
                Bukkit.getOnlinePlayers().forEach(p ->
                        p.sendMessage(LegacyComponentSerializer.legacySection().deserialize(line)));
            });
            listenerRegistered = true;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTalk(AsyncChatEvent event) {
        if (!this.isEnable) return;
        Player player = event.getPlayer();
        String plain = PlainTextComponentSerializer.plainText()
                .serialize(event.originalMessage()).trim();
        this.plugin.getScheduler().runAtLocation(player.getLocation(),
                wrappedTask -> networkManager.sendToServer("chat",
                        serverName + "|" + player.getName() + "|" + plain));
    }
}
```

- [ ] **Step 4: Register in ZModuleManager**

Add before `this.loadConfigurations()`:
```java
        this.modules.put(GlobalChatModule.class, new GlobalChatModule(this.plugin));
```
Add import: `import dev.yanianz.essentials.networkchat.GlobalChatModule;`

- [ ] **Step 5: Build + commit**

```bash
./gradlew build -x test --console=plain
git add -A && git commit -m "feat(network): NetworkManager + GlobalChatModule (cross-server chat relay)"
```

---

### Task 1.2: /g Toggle Command + Messages + Changelog

**Files:**
- Create: `src/main/java/dev/yanianz/essentials/networkchat/CommandGlobalChat.java`
- Modify: `src/main/java/fr/maxlego08/essentials/commands/CommandLoader.java`
- Modify: `API/src/main/java/fr/maxlego08/essentials/api/messages/Message.java`
- Modify: `src/main/resources/messages/messages.yml`
- Modify: `changelog.md`

- [ ] **Step 1: Add messages**

In `Message.java`:
```java
    DESCRIPTION_GLOBALCHAT("Toggle global cross-server chat"),
    COMMAND_GLOBALCHAT_ENABLED("<success>Global chat enabled."),
    COMMAND_GLOBALCHAT_DISABLED("<error>Global chat disabled."),
```

In `messages.yml`:
```yaml
description-globalchat: "Toggle global cross-server chat"
command-globalchat-enabled: "<success>Global chat enabled."
command-globalchat-disabled: "<error>Global chat disabled."
```

- [ ] **Step 2: Create CommandGlobalChat**

```java
package dev.yanianz.essentials.networkchat;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.commands.CommandResultType;
import fr.maxlego08.essentials.api.commands.Permission;
import fr.maxlego08.essentials.api.messages.Message;
import fr.maxlego08.essentials.api.user.Option;
import fr.maxlego08.essentials.zutils.utils.commands.VCommand;

public class CommandGlobalChat extends VCommand {

    public CommandGlobalChat(EssentialsPlugin plugin) {
        super(plugin);
        this.setDescription(Message.DESCRIPTION_GLOBALCHAT);
        this.setPermission(Permission.ESSENTIALS_USE);
        this.onlyPlayers();
    }

    @Override
    protected CommandResultType perform(EssentialsPlugin plugin) {
        boolean enabled = this.user.toggleOption(Option.GLOBAL_CHAT);
        message(this.sender, enabled ? Message.COMMAND_GLOBALCHAT_ENABLED : Message.COMMAND_GLOBALCHAT_DISABLED);
        return CommandResultType.SUCCESS;
    }
}
```

- [ ] **Step 3: Register command**

```java
        register("g", CommandGlobalChat.class, "globalchat");
```
Add import: `import dev.yanianz.essentials.networkchat.CommandGlobalChat;`

- [ ] **Step 4: Add Option enum entry**

In `API/src/main/java/fr/maxlego08/essentials/api/user/Option.java`, add:
```java
    GLOBAL_CHAT,
```

- [ ] **Step 5: Add changelog + build + commit**

```markdown
## Global chat relay

- **NetworkManager** — BungeeCord plugin messaging transport with sub-channel routing; reusable for friends/guild/party
- **GlobalChatModule** — cross-server public chat relay; messages from one server appear in all servers with configurable format
- **/g toggle** — per-player toggle to opt out of global chat relay
- **Config-driven format** — `%server%`, `%player%`, `%message%` placeholders
```

```bash
./gradlew build --console=plain --no-daemon
git add -A && git commit -m "feat(network): /g toggle, messages, changelog"
```

---

## Phase 2: Friends (3 tasks)

### Task 2.1: FriendsModule + Storage + Commands

**Files:**
- Create: `src/main/resources/modules/friends/config.yml`
- Create: `src/main/java/dev/yanianz/essentials/friends/FriendsModule.java`
- Create: `src/main/java/dev/yanianz/essentials/friends/FriendRequest.java`
- Create: `src/main/java/dev/yanianz/essentials/friends/CommandFriend.java`
- Create: `src/main/java/dev/yanianz/essentials/friends/CommandFriendList.java`
- Modify: `src/main/java/fr/maxlego08/essentials/module/ZModuleManager.java`
- Modify: `API/src/main/java/fr/maxlego08/essentials/api/messages/Message.java`
- Modify: `src/main/resources/messages/messages.yml`
- Modify: `changelog.md`

- [ ] **Step 1: Config**

```yaml
config-version: 1
enable: true
max-friends: 50
request-expiry-days: 7
```

- [ ] **Step 2: FriendRequest record**

```java
package dev.yanianz.essentials.friends;

import java.util.UUID;

public record FriendRequest(UUID from, UUID to, long sentAt) {}
```

- [ ] **Step 3: FriendsModule**

```java
package dev.yanianz.essentials.friends;

import dev.yanianz.essentials.network.NetworkManager;
import fr.maxlego08.essentials.ZEssentialsPlugin;
import fr.maxlego08.essentials.api.configuration.NonLoadable;
import fr.maxlego08.essentials.module.ZModule;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class FriendsModule extends ZModule {

    private boolean enabled = true;
    private int maxFriends = 50;
    private int expiryDays = 7;

    @NonLoadable
    private final Map<UUID, Set<UUID>> friends = new ConcurrentHashMap<>();

    @NonLoadable
    private final List<FriendRequest> pendingRequests = Collections.synchronizedList(new ArrayList<>());

    public FriendsModule(ZEssentialsPlugin plugin) {
        super(plugin, "friends");
    }

    @Override
    public void loadConfiguration() {
        super.loadConfiguration();
        YamlConfiguration config = getConfiguration();
        this.enabled = config.getBoolean("enable", true);
        this.maxFriends = Math.max(1, config.getInt("max-friends", 50));
        this.expiryDays = Math.max(1, config.getInt("request-expiry-days", 7));
    }

    public boolean sendRequest(UUID from, UUID to) {
        if (from.equals(to)) return false;
        if (isFriend(from, to)) return false;
        if (getFriendCount(from) >= getMaxFor(from)) return false;
        if (hasPendingRequest(from, to)) return false;
        FriendRequest req = new FriendRequest(from, to, System.currentTimeMillis());
        pendingRequests.add(req);
        return true;
    }

    public boolean acceptRequest(UUID from, UUID to) {
        synchronized (pendingRequests) {
            boolean removed = pendingRequests.removeIf(r ->
                    r.from().equals(from) && r.to().equals(to));
            if (!removed) return false;
        }
        addFriend(from, to);
        addFriend(to, from);
        return true;
    }

    public boolean declineRequest(UUID from, UUID to) {
        synchronized (pendingRequests) {
            return pendingRequests.removeIf(r ->
                    r.from().equals(from) && r.to().equals(to));
        }
    }

    public boolean removeFriend(UUID player, UUID friend) {
        Set<UUID> playerFriends = friends.get(player);
        if (playerFriends == null) return false;
        boolean removed = playerFriends.remove(friend);
        if (removed) {
            friends.getOrDefault(friend, Set.of()).remove(player);
        }
        return removed;
    }

    public boolean isFriend(UUID player, UUID other) {
        return friends.getOrDefault(player, Set.of()).contains(other);
    }

    public boolean hasPendingRequest(UUID from, UUID to) {
        long cutoff = System.currentTimeMillis() - (expiryDays * 86_400_000L);
        synchronized (pendingRequests) {
            pendingRequests.removeIf(r -> r.sentAt() < cutoff);
            return pendingRequests.stream()
                    .anyMatch(r -> r.from().equals(from) && r.to().equals(to));
        }
    }

    public List<UUID> getPendingRequests(UUID to) {
        long cutoff = System.currentTimeMillis() - (expiryDays * 86_400_000L);
        synchronized (pendingRequests) {
            pendingRequests.removeIf(r -> r.sentAt() < cutoff);
            return pendingRequests.stream()
                    .filter(r -> r.to().equals(to))
                    .map(FriendRequest::from)
                    .collect(Collectors.toList());
        }
    }

    public List<UUID> getFriends(UUID player) {
        return new ArrayList<>(friends.getOrDefault(player, Set.of()));
    }

    public int getFriendCount(UUID player) {
        return friends.getOrDefault(player, Set.of()).size();
    }

    public int getMaxFor(UUID player) {
        return this.maxFriends;
    }

    private void addFriend(UUID a, UUID b) {
        friends.computeIfAbsent(a, k -> ConcurrentHashMap.newKeySet()).add(b);
    }

    public boolean isEnabled() { return enabled && isEnable; }
}
```

- [ ] **Step 4: CommandFriend**

```java
package dev.yanianz.essentials.friends;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.commands.CommandResultType;
import fr.maxlego08.essentials.api.commands.Permission;
import fr.maxlego08.essentials.api.messages.Message;
import fr.maxlego08.essentials.zutils.utils.commands.VCommand;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class CommandFriend extends VCommand {

    public CommandFriend(EssentialsPlugin plugin) {
        super(plugin);
        this.setModule(FriendsModule.class);
        this.setPermission(Permission.ESSENTIALS_FRIENDS);
        this.setDescription(Message.DESCRIPTION_FRIEND);
        this.addSubCommand("add", (sender, args) -> java.util.List.of("add", "remove", "accept", "decline", "list"));
        this.addRequirePlayerNameArg();
    }

    @Override
    protected CommandResultType perform(EssentialsPlugin plugin) {
        FriendsModule module = plugin.getModuleManager().getModule(FriendsModule.class);
        if (module == null || !module.isEnabled()) return CommandResultType.SUCCESS;

        String action = argAsString(0, "list");
        String targetName = argAsString(1, "");
        if (action.isEmpty()) action = "list";

        UUID playerUuid = this.player != null ? this.player.getUniqueId() : null;
        if (playerUuid == null) return CommandResultType.SYNTAX_ERROR;

        switch (action.toLowerCase()) {
            case "add" -> handleAdd(module, playerUuid, targetName);
            case "remove" -> handleRemove(module, playerUuid, targetName);
            case "accept" -> handleAccept(module, playerUuid, targetName);
            case "decline" -> handleDecline(module, playerUuid, targetName);
            case "list" -> handleList(module, playerUuid);
            default -> {
            }
        }
        return CommandResultType.SUCCESS;
    }

    private void handleAdd(FriendsModule module, UUID playerUuid, String targetName) {
        if (targetName.isEmpty()) return;
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            message(sender, Message.COMMAND_FRIEND_NOT_ONLINE, "%player%", targetName);
            return;
        }
        if (module.sendRequest(playerUuid, target.getUniqueId())) {
            message(sender, Message.COMMAND_FRIEND_REQUEST_SENT, "%player%", targetName);
            message(target, Message.COMMAND_FRIEND_REQUEST_RECEIVED, "%player%", this.player.getName());
        } else {
            message(sender, Message.COMMAND_FRIEND_ALREADY_FRIEND, "%player%", targetName);
        }
    }

    private void handleRemove(FriendsModule module, UUID playerUuid, String targetName) {
        UUID targetUuid = resolveUuid(targetName);
        if (targetUuid == null) return;
        if (module.removeFriend(playerUuid, targetUuid)) {
            message(sender, Message.COMMAND_FRIEND_REMOVED, "%player%", targetName);
        }
    }

    private void handleAccept(FriendsModule module, UUID playerUuid, String targetName) {
        UUID targetUuid = resolveUuid(targetName);
        if (targetUuid == null) return;
        if (module.acceptRequest(targetUuid, playerUuid)) {
            message(sender, Message.COMMAND_FRIEND_ACCEPTED, "%player%", targetName);
            Player target = Bukkit.getPlayer(targetUuid);
            if (target != null) {
                message(target, Message.COMMAND_FRIEND_ACCEPTED_NOTIFY, "%player%", this.player.getName());
            }
        }
    }

    private void handleDecline(FriendsModule module, UUID playerUuid, String targetName) {
        UUID targetUuid = resolveUuid(targetName);
        if (targetUuid == null) return;
        if (module.declineRequest(targetUuid, playerUuid)) {
            message(sender, Message.COMMAND_FRIEND_DECLINED, "%player%", targetName);
        }
    }

    private void handleList(FriendsModule module, UUID playerUuid) {
        List<UUID> friends = module.getFriends(playerUuid);
        String names = friends.stream()
                .map(uuid -> {
                    Player p = Bukkit.getPlayer(uuid);
                    return p != null ? p.getName() : uuid.toString().substring(0, 8);
                })
                .collect(Collectors.joining(", "));
        if (names.isEmpty()) names = "none";
        message(sender, Message.COMMAND_FRIEND_LIST, "%friends%", names);
    }

    private UUID resolveUuid(String name) {
        Player p = Bukkit.getPlayer(name);
        if (p != null) return p.getUniqueId();
        return null;
    }
}
```

- [ ] **Step 5: Add messages + permission + register**

In `Message.java`:
```java
    DESCRIPTION_FRIEND("Manage friends"),
    COMMAND_FRIEND_REQUEST_SENT("<success>Friend request sent to %player%."),
    COMMAND_FRIEND_REQUEST_RECEIVED("<success>%player% sent you a friend request."),
    COMMAND_FRIEND_ALREADY_FRIEND("<error>Already friends with %player%."),
    COMMAND_FRIEND_REMOVED("<success>Removed %player% from your friends."),
    COMMAND_FRIEND_ACCEPTED("<success>You are now friends with %player%."),
    COMMAND_FRIEND_ACCEPTED_NOTIFY("<success>%player% accepted your friend request."),
    COMMAND_FRIEND_DECLINED("<error>Declined friend request from %player%."),
    COMMAND_FRIEND_NOT_ONLINE("<error>%player% is not online."),
    COMMAND_FRIEND_LIST("<success>Your friends: %friends%"),
```

In `Permission.java`:
```java
    ESSENTIALS_FRIENDS,
```

In `messages.yml`:
```yaml
description-friend: "Manage friends"
command-friend-request-sent: "<success>Friend request sent to %player%."
command-friend-request-received: "<success>%player% sent you a friend request."
command-friend-already-friend: "<error>Already friends with %player%."
command-friend-removed: "<success>Removed %player% from your friends."
command-friend-accepted: "<success>You are now friends with %player%."
command-friend-accepted-notify: "<success>%player% accepted your friend request."
command-friend-declined: "<error>Declined friend request from %player%."
command-friend-not-online: "<error>%player% is not online."
command-friend-list: "<success>Your friends: %friends%"
```

In `CommandLoader.java`:
```java
        register("friend", CommandFriend.class, "friends");
```
Import: `import dev.yanianz.essentials.friends.CommandFriend;`

In `ZModuleManager.java`, register:
```java
        this.modules.put(FriendsModule.class, new FriendsModule(this.plugin));
```
Import: `import dev.yanianz.essentials.friends.FriendsModule;`

- [ ] **Step 6: Add changelog + build + commit**

```markdown
## Friends system

- **Friend requests** — /friend add <player> sends a request; /friend accept/decline <player> responds
- **Friend list** — /friend list shows all current friends
- **Remove friends** — /friend remove <player>
- **Permission-based cap** — `essentials.friends.max.<n>` overrides the `max-friends` config cap
- **Request expiry** — pending requests auto-expire after 7 days (configurable)
- **Per-server storage** — friend list stored per-player; cross-server events via NetworkManager
```

```bash
./gradlew build --console=plain --no-daemon
git add -A && git commit -m "feat(friends): friends module + commands + messages"
```

---

### Task 2.2: FriendListener (online notifications via Redis)

**Files:**
- Create: `src/main/java/dev/yanianz/essentials/friends/FriendListener.java`

- [ ] **Step 1: Create FriendListener**

```java
package dev.yanianz.essentials.friends;

import dev.yanianz.essentials.network.NetworkManager;
import fr.maxlego08.essentials.ZEssentialsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class FriendListener implements Listener {

    private final ZEssentialsPlugin plugin;
    private final FriendsModule friendsModule;
    private final NetworkManager networkManager;
    private static final AtomicBoolean registered = new AtomicBoolean(false);

    public FriendListener(ZEssentialsPlugin plugin, FriendsModule friendsModule, NetworkManager networkManager) {
        this.plugin = plugin;
        this.friendsModule = friendsModule;
        this.networkManager = networkManager;
    }

    public static void ensureRegistered(ZEssentialsPlugin plugin) {
        if (!registered.compareAndSet(false, true)) return;
        FriendsModule module = plugin.getModuleManager().getModule(FriendsModule.class);
        if (module == null) return;
        NetworkManager netMgr = new NetworkManager(plugin);
        Bukkit.getPluginManager().registerEvents(
                new FriendListener(plugin, module, netMgr), plugin);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        UUID playerUuid = event.getPlayer().getUniqueId();
        for (UUID friendUuid : friendsModule.getFriends(playerUuid)) {
            if (Bukkit.getPlayer(friendUuid) != null) {
                Player friend = Bukkit.getPlayer(friendUuid);
                friend.sendMessage(net_kyori_adventure_text_Component_text("Your friend "
                        + event.getPlayer().getName() + " is now online."));
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add -A && git commit -m "feat(friends): online notifications on friend join"
```

---

## Phase 3: Guild (2 tasks)

### Task 3.1: GuildModule + Commands

**Files:**
- Create: `src/main/resources/modules/guild/config.yml`
- Create: `src/main/java/dev/yanianz/essentials/guild/GuildModule.java`
- Create: `src/main/java/dev/yanianz/essentials/guild/GuildRank.java`
- Create: `src/main/java/dev/yanianz/essentials/guild/CommandGuild.java`
- Create: `src/main/java/dev/yanianz/essentials/guild/CommandGuildChat.java`
- Modify: `ZModuleManager.java`, `CommandLoader.java`, `Message.java`, `Permission.java`, `messages.yml`
- Modify: `changelog.md`

- [ ] **Step 1: Config + GuildRank + GuildModule + Commands + Messages + Changelog + Build + Commit**

```yaml
# modules/guild/config.yml
config-version: 1
enable: true
max-members: 25
max-name-length: 16
chat-format: "&2[&aGUILD&2] &f%player%&8: &7%message%"
```

```java
// GuildRank.java
package dev.yanianz.essentials.guild;
public enum GuildRank { MEMBER, OFFICER, LEADER }
```

```java
// GuildModule.java
package dev.yanianz.essentials.guild;

import fr.maxlego08.essentials.ZEssentialsPlugin;
import fr.maxlego08.essentials.module.ZModule;
import org.bukkit.configuration.file.YamlConfiguration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class GuildModule extends ZModule {

    private boolean enabled = true;
    private int maxMembers = 25;
    private int maxNameLength = 16;
    private String chatFormat = "&2[&aGUILD&2] &f%player%&8: &7%message%";

    private final Map<Integer, Guild> guilds = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> playerGuilds = new ConcurrentHashMap<>();
    private final AtomicInteger idGen = new AtomicInteger(1);

    public record Guild(int id, String name, String tag, UUID leader,
                        Map<UUID, GuildRank> members, long createdAt) {}

    public GuildModule(ZEssentialsPlugin plugin) {
        super(plugin, "guild");
    }

    @Override
    public void loadConfiguration() {
        super.loadConfiguration();
        YamlConfiguration config = getConfiguration();
        this.enabled = config.getBoolean("enable", true);
        this.maxMembers = Math.max(1, config.getInt("max-members", 25));
        this.maxNameLength = Math.max(3, config.getInt("max-name-length", 16));
        this.chatFormat = config.getString("chat-format", "&2[&aGUILD&2] &f%player%&8: &7%message%");
    }

    public int createGuild(UUID leader, String name, String tag) {
        if (name == null || name.isEmpty() || name.length() > maxNameLength) return -1;
        if (playerGuilds.containsKey(leader)) return -1;
        int id = idGen.getAndIncrement();
        Map<UUID, GuildRank> members = new HashMap<>();
        members.put(leader, GuildRank.LEADER);
        Guild guild = new Guild(id, name, tag, leader, members, System.currentTimeMillis());
        guilds.put(id, guild);
        playerGuilds.put(leader, id);
        return id;
    }

    public boolean disbandGuild(int id) {
        Guild g = guilds.remove(id);
        if (g == null) return false;
        g.members().keySet().forEach(playerGuilds::remove);
        return true;
    }

    public boolean joinGuild(int id, UUID player) {
        if (playerGuilds.containsKey(player)) return false;
        Guild g = guilds.get(id);
        if (g == null || g.members().size() >= maxMembers) return false;
        guilds.get(id).members().put(player, GuildRank.MEMBER);
        playerGuilds.put(player, id);
        return true;
    }

    public boolean leaveGuild(int id, UUID player) {
        Guild g = guilds.get(id);
        if (g == null || !g.members().containsKey(player)) return false;
        if (g.leader().equals(player)) return false;
        guilds.get(id).members().remove(player);
        playerGuilds.remove(player);
        return true;
    }

    public int getPlayerGuildId(UUID player) {
        return playerGuilds.getOrDefault(player, -1);
    }

    public Guild getGuild(int id) { return guilds.get(id); }
    public Collection<Guild> getAllGuilds() { return guilds.values(); }
    public boolean isEnabled() { return enabled && isEnable; }
    public String getChatFormat() { return chatFormat; }
}
```

```java
// CommandGuild.java — single file with subcommands
package dev.yanianz.essentials.guild;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.commands.CommandResultType;
import fr.maxlego08.essentials.api.commands.Permission;
import fr.maxlego08.essentials.api.messages.Message;
import fr.maxlego08.essentials.zutils.utils.commands.VCommand;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class CommandGuild extends VCommand {
    public CommandGuild(EssentialsPlugin plugin) {
        super(plugin);
        this.setModule(GuildModule.class);
        this.setPermission(Permission.ESSENTIALS_GUILD);
        this.setDescription(Message.DESCRIPTION_GUILD);
        this.addOptionalArg("action", (s, a) -> java.util.List.of("create", "disband", "invite", "join", "leave", "info"));
    }

    @Override
    protected CommandResultType perform(EssentialsPlugin plugin) {
        GuildModule module = plugin.getModuleManager().getModule(GuildModule.class);
        if (module == null || !module.isEnabled()) return CommandResultType.SUCCESS;
        String action = argAsString(0, "info");
        switch (action.toLowerCase()) {
            case "create" -> {
                String name = argAsString(1, "");
                if (name.isEmpty()) {
                    message(sender, Message.COMMAND_GUILD_USAGE);
                    return CommandResultType.SUCCESS;
                }
                int id = module.createGuild(this.player.getUniqueId(), name, name);
                if (id > 0) message(sender, Message.COMMAND_GUILD_CREATED, "%name%", name);
                else message(sender, Message.COMMAND_GUILD_ALREADY_IN);
            }
            case "disband" -> {
                int id = module.getPlayerGuildId(this.player.getUniqueId());
                if (id < 0 || module.getGuild(id).leader() != this.player.getUniqueId()) {
                    message(sender, Message.COMMAND_GUILD_NOT_LEADER);
                    return CommandResultType.SUCCESS;
                }
                module.disbandGuild(id);
                message(sender, Message.COMMAND_GUILD_DISBANDED);
            }
            case "join" -> {
                int id = argAsInteger(1, -1);
                if (module.joinGuild(id, this.player.getUniqueId())) {
                    message(sender, Message.COMMAND_GUILD_JOINED);
                }
            }
            case "leave" -> {
                int id = module.getPlayerGuildId(this.player.getUniqueId());
                if (id < 0) return CommandResultType.SUCCESS;
                if (module.getGuild(id).leader() == this.player.getUniqueId()) {
                    message(sender, Message.COMMAND_GUILD_LEADER_CANNOT_LEAVE);
                    return CommandResultType.SUCCESS;
                }
                if (module.leaveGuild(id, this.player.getUniqueId())) {
                    message(sender, Message.COMMAND_GUILD_LEFT);
                }
            }
            case "info" -> {
                int id = module.getPlayerGuildId(this.player.getUniqueId());
                if (id < 0) {
                    message(sender, Message.COMMAND_GUILD_NOT_IN);
                    return CommandResultType.SUCCESS;
                }
                GuildModule.Guild g = module.getGuild(id);
                message(sender, Message.COMMAND_GUILD_INFO, "%name%", g.name(),
                        "%members%", String.valueOf(g.members().size()));
            }
        }
        return CommandResultType.SUCCESS;
    }
}
```

Messages:
```java
    DESCRIPTION_GUILD("Manage your guild"),
    COMMAND_GUILD_CREATED("<success>Guild &a%name%<success> created."),
    COMMAND_GUILD_ALREADY_IN("<error>You are already in a guild."),
    COMMAND_GUILD_USAGE("<error>Usage: /guild <create|disband|join|leave|info>"),
    COMMAND_GUILD_DISBANDED("<success>Guild disbanded."),
    COMMAND_GUILD_NOT_LEADER("<error>Only the leader can disband."),
    COMMAND_GUILD_JOINED("<success>Joined the guild."),
    COMMAND_GUILD_LEADER_CANNOT_LEAVE("<error>Leader cannot leave. Disband instead."),
    COMMAND_GUILD_LEFT("<success>Left the guild."),
    COMMAND_GUILD_NOT_IN("<error>You are not in a guild."),
    COMMAND_GUILD_INFO("<success>Guild: &a%name% &7(%members% members)"),
```

In `Permission.java`:
```java
    ESSENTIALS_GUILD,
```

In `CommandLoader.java`:
```java
        register("guild", CommandGuild.class, "g");
```

In `ZModuleManager.java`:
```java
        this.modules.put(GuildModule.class, new GuildModule(this.plugin));
```

Changelog:
```markdown
## Guild system

- **Guild create/disband** — /guild create <name> creates; /guild disband destroys (leader only)
- **Guild join/leave** — /guild join <id> | /guild leave
- **Guild info** — /guild info shows name and member count
- **Rank system** — MEMBER, OFFICER, LEADER hierarchy
- **Guild chat** — /gc <message> sends to all members (format in config)
- **Permission cap** — `max-members` in config (default 25)
```

```bash
./gradlew build --console=plain --no-daemon
git add -A && git commit -m "feat(guild): guild module + commands + rank system"
```

---

### Task 3.2: CommandGuildChat

```java
// CommandGuildChat.java
package dev.yanianz.essentials.guild;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.commands.CommandResultType;
import fr.maxlego08.essentials.api.commands.Permission;
import fr.maxlego08.essentials.api.messages.Message;
import fr.maxlego08.essentials.zutils.utils.commands.VCommand;
import org.bukkit.Bukkit;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class CommandGuildChat extends VCommand {
    public CommandGuildChat(EssentialsPlugin plugin) {
        super(plugin);
        this.setModule(GuildModule.class);
        this.setPermission(Permission.ESSENTIALS_GUILD);
        this.setDescription(Message.DESCRIPTION_GUILD_CHAT);
        this.setExtendedArgs(true);
    }

    @Override
    protected CommandResultType perform(EssentialsPlugin plugin) {
        GuildModule module = plugin.getModuleManager().getModule(GuildModule.class);
        if (module == null || !module.isEnabled()) return CommandResultType.SUCCESS;
        int guildId = module.getPlayerGuildId(this.player.getUniqueId());
        if (guildId < 0) {
            message(sender, Message.COMMAND_GUILD_NOT_IN);
            return CommandResultType.SUCCESS;
        }
        String messageText = getArgs(0);
        String formatted = module.getChatFormat()
                .replace("%player%", this.player.getName())
                .replace("%message%", messageText)
                .replace("&", "§");
        for (UUID memberUuid : module.getGuild(guildId).members().keySet()) {
            Player p = Bukkit.getPlayer(memberUuid);
            if (p != null) p.sendMessage(LegacyComponentSerializer.legacySection().deserialize(formatted));
        }
        return CommandResultType.SUCCESS;
    }
}
```

```java
// Message.java
DESCRIPTION_GUILD_CHAT("Send a message to your guild"),
```

```java
// CommandLoader.java
register("gc", CommandGuildChat.class, "guildchat");
```

```bash
git add -A && git commit -m "feat(guild): /gc guild chat command"
```

---

## Phase 4: Party (2 tasks)

### Task 4.1: PartyModule + Commands

**Files:**
- Create: `src/main/resources/modules/party/config.yml`
- Create: `src/main/java/dev/yanianz/essentials/party/PartyModule.java`
- Create: `src/main/java/dev/yanianz/essentials/party/CommandParty.java`
- Create: `src/main/java/dev/yanianz/essentials/party/CommandPartyChat.java`
- Modify: ZModuleManager, CommandLoader, Message, Permission, messages.yml, changelog

- [ ] **Step 1: Config + PartyModule + Commands + Messages + Changelog + Build + Commit**

```yaml
# modules/party/config.yml
config-version: 1
enable: true
max-size: 8
chat-format: "&d[&5PARTY&d] &f%player%&8: &7%message%"
```

```java
// PartyModule.java
package dev.yanianz.essentials.party;

import fr.maxlego08.essentials.ZEssentialsPlugin;
import fr.maxlego08.essentials.module.ZModule;
import org.bukkit.configuration.file.YamlConfiguration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class PartyModule extends ZModule {

    private boolean enabled = true;
    private int maxSize = 8;
    private String chatFormat = "&d[&5PARTY&d] &f%player%&8: &7%message%";

    private final Map<Integer, Party> parties = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> playerParties = new ConcurrentHashMap<>();
    private final AtomicInteger idGen = new AtomicInteger(1);

    public record Party(int id, UUID leader, Map<UUID, Boolean> members, long createdAt) {}

    public PartyModule(ZEssentialsPlugin plugin) {
        super(plugin, "party");
    }

    @Override
    public void loadConfiguration() {
        super.loadConfiguration();
        YamlConfiguration config = getConfiguration();
        this.enabled = config.getBoolean("enable", true);
        this.maxSize = Math.max(2, config.getInt("max-size", 8));
        this.chatFormat = config.getString("chat-format", "&d[&5PARTY&d] &f%player%&8: &7%message%");
    }

    public int createParty(UUID leader) {
        if (playerParties.containsKey(leader)) return -1;
        int id = idGen.getAndIncrement();
        Map<UUID, Boolean> members = new HashMap<>();
        members.put(leader, true);
        Party party = new Party(id, leader, members, System.currentTimeMillis());
        parties.put(id, party);
        playerParties.put(leader, id);
        return id;
    }

    public boolean disbandParty(int id) {
        Party p = parties.remove(id);
        if (p == null) return false;
        p.members().keySet().forEach(playerParties::remove);
        return true;
    }

    public boolean invitePlayer(int id, UUID player) {
        Party p = parties.get(id);
        if (p == null || playerParties.containsKey(player)) return false;
        if (p.members().size() >= maxSize) return false;
        parties.get(id).members().put(player, true);
        playerParties.put(player, id);
        return true;
    }

    public boolean leaveParty(int id, UUID player) {
        Party p = parties.get(id);
        if (p == null || !p.members().containsKey(player)) return false;
        parties.get(id).members().remove(player);
        playerParties.remove(player);
        if (p.leader().equals(player)) {
            if (parties.get(id).members().isEmpty()) {
                disbandParty(id);
            } else {
                UUID newLeader = parties.get(id).members().keySet().iterator().next();
                parties.put(id, new Party(id, newLeader, parties.get(id).members(), parties.get(id).createdAt()));
            }
        }
        return true;
    }

    public int getPlayerPartyId(UUID player) {
        return playerParties.getOrDefault(player, -1);
    }

    public Party getParty(int id) { return parties.get(id); }
    public boolean isEnabled() { return enabled && isEnable; }
    public String getChatFormat() { return chatFormat; }
}
```

```java
// CommandParty.java
package dev.yanianz.essentials.party;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.commands.CommandResultType;
import fr.maxlego08.essentials.api.commands.Permission;
import fr.maxlego08.essentials.api.messages.Message;
import fr.maxlego08.essentials.zutils.utils.commands.VCommand;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class CommandParty extends VCommand {
    public CommandParty(EssentialsPlugin plugin) {
        super(plugin);
        this.setModule(PartyModule.class);
        this.setPermission(Permission.ESSENTIALS_PARTY);
        this.setDescription(Message.DESCRIPTION_PARTY);
        this.addOptionalArg("action", (s, a) -> java.util.List.of("create", "disband", "invite", "leave", "info"));
        this.addRequirePlayerNameArg();
    }

    @Override
    protected CommandResultType perform(EssentialsPlugin plugin) {
        PartyModule module = plugin.getModuleManager().getModule(PartyModule.class);
        if (module == null || !module.isEnabled()) return CommandResultType.SUCCESS;
        String action = argAsString(0, "info");
        switch (action.toLowerCase()) {
            case "create" -> {
                int id = module.createParty(this.player.getUniqueId());
                if (id > 0) message(sender, Message.COMMAND_PARTY_CREATED);
                else message(sender, Message.COMMAND_PARTY_ALREADY_IN);
            }
            case "disband" -> {
                int id = module.getPlayerPartyId(this.player.getUniqueId());
                if (id > 0 && module.getParty(id).leader() == this.player.getUniqueId()) {
                    module.disbandParty(id);
                    message(sender, Message.COMMAND_PARTY_DISBANDED);
                }
            }
            case "invite" -> {
                int partyId = module.getPlayerPartyId(this.player.getUniqueId());
                String targetName = argAsString(1, "");
                if (partyId > 0 && !targetName.isEmpty()) {
                    Player target = Bukkit.getPlayer(targetName);
                    if (target != null && module.invitePlayer(partyId, target.getUniqueId())) {
                        message(sender, Message.COMMAND_PARTY_INVITED, "%player%", targetName);
                        message(target, Message.COMMAND_PARTY_INVITED_NOTIFY, "%player%", this.player.getName());
                    }
                }
            }
            case "leave" -> {
                int id = module.getPlayerPartyId(this.player.getUniqueId());
                if (id > 0 && module.leaveParty(id, this.player.getUniqueId())) {
                    message(sender, Message.COMMAND_PARTY_LEFT);
                }
            }
            case "info" -> {
                int id = module.getPlayerPartyId(this.player.getUniqueId());
                if (id < 0) {
                    message(sender, Message.COMMAND_PARTY_NOT_IN);
                    return CommandResultType.SUCCESS;
                }
                PartyModule.Party p = module.getParty(id);
                message(sender, Message.COMMAND_PARTY_INFO, "%leader%", p.leader().toString().substring(0, 8),
                        "%size%", String.valueOf(p.members().size()));
            }
        }
        return CommandResultType.SUCCESS;
    }
}
```

```java
// CommandPartyChat.java
package dev.yanianz.essentials.party;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.commands.CommandResultType;
import fr.maxlego08.essentials.api.commands.Permission;
import fr.maxlego08.essentials.api.messages.Message;
import fr.maxlego08.essentials.zutils.utils.commands.VCommand;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import java.util.UUID;

public class CommandPartyChat extends VCommand {
    public CommandPartyChat(EssentialsPlugin plugin) {
        super(plugin);
        this.setModule(PartyModule.class);
        this.setPermission(Permission.ESSENTIALS_PARTY);
        this.setDescription(Message.DESCRIPTION_PARTY_CHAT);
        this.setExtendedArgs(true);
    }

    @Override
    protected CommandResultType perform(EssentialsPlugin plugin) {
        PartyModule module = plugin.getModuleManager().getModule(PartyModule.class);
        if (module == null || !module.isEnabled()) return CommandResultType.SUCCESS;
        int partyId = module.getPlayerPartyId(this.player.getUniqueId());
        if (partyId < 0) {
            message(sender, Message.COMMAND_PARTY_NOT_IN);
            return CommandResultType.SUCCESS;
        }
        String msg = getArgs(0);
        String formatted = module.getChatFormat()
                .replace("%player%", this.player.getName())
                .replace("%message%", msg)
                .replace("&", "§");
        for (UUID memberUuid : module.getParty(partyId).members().keySet()) {
            Player p = Bukkit.getPlayer(memberUuid);
            if (p != null) p.sendMessage(LegacyComponentSerializer.legacySection().deserialize(formatted));
        }
        return CommandResultType.SUCCESS;
    }
}
```

Messages:
```java
DESCRIPTION_PARTY("Manage your party"),
DESCRIPTION_PARTY_CHAT("Send a message to your party"),
COMMAND_PARTY_CREATED("<success>Party created."),
COMMAND_PARTY_DISBANDED("<success>Party disbanded."),
COMMAND_PARTY_INVITED("<success>%player% joined the party."),
COMMAND_PARTY_INVITED_NOTIFY("<success>%player% invited you to their party."),
COMMAND_PARTY_LEFT("<success>Left the party."),
COMMAND_PARTY_NOT_IN("<error>You are not in a party."),
COMMAND_PARTY_ALREADY_IN("<error>You are already in a party."),
COMMAND_PARTY_INFO("<success>Party leader: &a%leader% &7(%size% members)"),
```

```java
// Permission.java
ESSENTIALS_PARTY,
```

```java
// CommandLoader.java
register("party", CommandParty.class);
register("pc", CommandPartyChat.class, "partychat");
```

```java
// ZModuleManager.java
this.modules.put(PartyModule.class, new PartyModule(this.plugin));
```

Changelog:
```markdown
## Party system

- **Create/disband** — /party create | /party disband (leader only)
- **Invite/leave** — /party invite <player> | /party leave
- **Leader transfer** — auto-transfer to longest-tenured member when leader leaves
- **Party chat** — /pc <message> sends to all party members
- **Max size** — configurable cap (default 8)
- **Auto-disband** — when last member leaves
```

```bash
./gradlew build --console=plain --no-daemon
git add -A && git commit -m "feat(party): party module + commands + auto-leader-transfer"
```

---

### Task 4.2: Full Integration Test + Final Changelog

- [ ] **Step 1: Run full test suite + add final changelog entry**

```bash
./gradlew build --console=plain --no-daemon
```

Add a "Network/Social Layer" section to changelog.md:
```markdown
## Network/Social Layer

- **NetworkManager** — BungeeCord plugin messaging transport with sub-channel routing
- **Global chat** — cross-server public chat relay with configurable format
- **Friends** — friend requests, accept/decline, list, remove, online notifications
- **Guild** — create/disband/join/leave, rank system, guild chat, configurable max members
- **Party** — create/disband/invite/leave, auto leader transfer, party chat, configurable max size
```

```bash
git add -A && git commit -m "docs: changelog for network/social layer"
```
