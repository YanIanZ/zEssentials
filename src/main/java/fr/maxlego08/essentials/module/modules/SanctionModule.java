package fr.maxlego08.essentials.module.modules;

import fr.maxlego08.essentials.ZEssentialsPlugin;
import fr.maxlego08.essentials.api.cache.ExpiringCache;
import fr.maxlego08.essentials.api.commands.Permission;
import fr.maxlego08.essentials.api.dto.UserDTO;
import fr.maxlego08.essentials.api.messages.Message;
import fr.maxlego08.essentials.api.sanction.Sanction;
import fr.maxlego08.essentials.api.sanction.SanctionManager;
import fr.maxlego08.essentials.api.sanction.SanctionType;
import fr.maxlego08.essentials.api.server.EssentialsServer;
import fr.maxlego08.essentials.api.storage.IStorage;
import fr.maxlego08.essentials.api.user.Option;
import fr.maxlego08.essentials.api.user.User;
import fr.maxlego08.essentials.api.user.UserRecord;
import fr.maxlego08.essentials.api.utils.SafeLocation;
import fr.maxlego08.essentials.listener.paper.ChatListener;
import fr.maxlego08.essentials.module.ZModule;
import fr.maxlego08.essentials.user.ZUser;
import fr.maxlego08.essentials.zutils.utils.TimerBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scoreboard.Team;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class SanctionModule extends ZModule implements SanctionManager {

    private final ExpiringCache<UUID, User> expiringCache = new ExpiringCache<>(1000 * 60 * 60); // 1 hour cache

    @fr.maxlego08.essentials.api.configuration.NonLoadable
    private static final String FROZEN_TEAM_NAME = "zessentials_frozen";

    @fr.maxlego08.essentials.api.configuration.NonLoadable
    private final Map<UUID, com.tcoded.folialib.wrapper.task.WrappedTask> particleTasks = new HashMap<>();

    @fr.maxlego08.essentials.api.configuration.NonLoadable
    private boolean persistFreeze;
    // Default messages for kick and ban
    // Do not make those fields final, javac would inline the constant and the configuration would be ignored
    private String kickDefaultReason = "";
    private String banDefaultReason = "";
    private String muteDefaultReason = "";
    private String unmuteDefaultReason = "";
    private String unbanDefaultReason = "";
    private String dateFormat = "yyyy-MM-dd HH:mm:ss";
    private boolean seenShowUuid = true;
    private boolean seenShowIp = true;
    private boolean seenShowLastLocation = true;
    private boolean seenShowCreatedAt = true;
    private boolean seenShowPlaytime = true;
    private final Material kickMaterial = Material.BOOK;
    private final Material banMaterial = Material.BOOK;
    private final Material muteMaterial = Material.BOOK;
    private final Material unbanMaterial = Material.BOOK;
    private final Material unmuteMaterial = Material.BOOK;
    private final Material warnMaterial = Material.BOOK;
    private final Material freezeMaterial = Material.BOOK;
    private final Material currentMuteMaterial = Material.BOOKSHELF;
    private final Material currentBanMaterial = Material.BOOKSHELF;
    private final List<String> protections = new ArrayList<>();
    private SimpleDateFormat simpleDateFormat;


    public SanctionModule(ZEssentialsPlugin plugin) {
        super(plugin, "sanction");
        Bukkit.getPluginManager().registerEvents(isPaperVersion() ? new ChatListener(plugin) : new fr.maxlego08.essentials.listener.spigot.ChatListener(plugin), plugin);
    }

    @Override
    public void loadConfiguration() {
        super.loadConfiguration();

        this.loadInventory("sanction");
        this.loadInventory("sanction_history");
        this.loadInventory("sanctions");
        this.simpleDateFormat = new SimpleDateFormat(this.dateFormat);
        this.persistFreeze = getConfiguration().getBoolean("freeze.persist-across-restarts", false);
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public String getKickDefaultReason() {
        return kickDefaultReason;
    }

    public String getBanDefaultReason() {
        return banDefaultReason;
    }

    public String getMuteDefaultReason() {
        return muteDefaultReason;
    }

    public String getUnbanDefaultReason() {
        return unbanDefaultReason;
    }

    public String getUnmuteDefaultReason() {
        return unmuteDefaultReason;
    }

    public Material getSanctionMaterial(SanctionType sanctionType, boolean isActive) {
        return switch (sanctionType) {
            case KICK -> kickMaterial;
            case MUTE -> isActive ? currentMuteMaterial : muteMaterial;
            case BAN -> isActive ? currentBanMaterial : banMaterial;
            case UNBAN -> unbanMaterial;
            case UNMUTE -> unmuteMaterial;
            case WARN -> warnMaterial;
            case FREEZE -> freezeMaterial;
        };
    }


    @Override
    public UUID getSenderUniqueId(CommandSender sender) {
        return sender instanceof Player player ? player.getUniqueId() : this.plugin.getConsoleUniqueId();
    }

    @Override
    public void kick(CommandSender sender, UUID uuid, String playerName, String reason) {

        if (isProtected(playerName)) {
            message(sender, Message.COMMAND_SANCTION_ERROR);
            return;
        }

        EssentialsServer server = plugin.getEssentialsServer();
        IStorage iStorage = plugin.getStorageManager().getStorage();

        // Create and save the sanction
        Sanction sanction = Sanction.kick(uuid, getSenderUniqueId(sender), reason);
        iStorage.insertSanction(sanction, sanction::setId);
        this.expiringCache.clear(uuid);

        // Kick the player with the specified reason
        server.kickPlayer(uuid, Message.MESSAGE_KICK, "%reason%", reason);

        // Broadcast a notification message to players with the kick notify permission
        server.broadcastMessage(Permission.ESSENTIALS_KICK_NOTIFY, Message.COMMAND_KICK_NOTIFY, "%player%", sender.getName(), "%target%", playerName, "%reason%", reason, "%sender%", getSanctionBy(sender), "%created_at%", this.simpleDateFormat.format(new Date()));
    }

    @Override
    public void warn(CommandSender sender, UUID uuid, String playerName, String reason) {

        if (isProtected(playerName)) {
            message(sender, Message.COMMAND_SANCTION_ERROR);
            return;
        }

        EssentialsServer server = plugin.getEssentialsServer();
        IStorage iStorage = plugin.getStorageManager().getStorage();

        // Create and save the sanction
        Sanction sanction = Sanction.warn(uuid, getSenderUniqueId(sender), reason);
        iStorage.insertSanction(sanction, sanction::setId);
        this.expiringCache.clear(uuid);

        // Warn the player with the specified reason
        server.sendMessage(uuid, Message.MESSAGE_WARN, "%reason%", reason, "%player%", sender.getName());

        // Broadcast a notification message to players with the warn notify permission
        server.broadcastMessage(Permission.ESSENTIALS_WARN_NOTIFY, Message.COMMAND_WARN_NOTIFY, "%player%", sender.getName(), "%target%", playerName, "%reason%", reason, "%sender%", getSanctionBy(sender), "%created_at%", this.simpleDateFormat.format(new Date()));
    }

    @Override
    public void ban(CommandSender sender, UUID uuid, String playerName, Duration duration, String reason) {

        if (isProtected(playerName)) {
            message(sender, Message.COMMAND_SANCTION_ERROR);
            return;
        }

        EssentialsServer server = plugin.getEssentialsServer();
        IStorage iStorage = plugin.getStorageManager().getStorage();

        // Check if the ban duration is valid
        if (duration.isZero()) {
            message(sender, Message.COMMAND_BAN_ERROR_DURATION);
            return;
        }

        // Calculate the ban finish date
        Date finishAt = new Date(System.currentTimeMillis() + duration.toMillis());

        // Create and save the sanction
        Sanction sanction = Sanction.ban(uuid, getSenderUniqueId(sender), reason, duration, finishAt);
        iStorage.insertSanction(sanction, index -> {
            sanction.setId(index);
            iStorage.updateUserBan(uuid, index);
        });
        this.expiringCache.clear(uuid);

        String durationString = TimerBuilder.getStringTime(duration.toMillis());
        // Ban the player with the specified reason and duration
        server.kickPlayer(uuid, Message.MESSAGE_BAN, "%reason%", reason, "%duration%", TimerBuilder.getStringTime(duration.toMillis()));

        // Broadcast a notification message to players with the ban notify permission
        server.broadcastMessage(Permission.ESSENTIALS_BAN_NOTIFY, Message.COMMAND_BAN_NOTIFY, "%player%", sender.getName(), "%target%", playerName, "%reason%", reason, "%duration%", durationString, "%sender%", getSanctionBy(sender), "%created_at%", this.simpleDateFormat.format(new Date()), "%expired_at%", this.simpleDateFormat.format(sanction.getExpiredAt()));
    }

    @Override
    public void mute(CommandSender sender, UUID uuid, String playerName, Duration duration, String reason) {

        if (isProtected(playerName)) {
            message(sender, Message.COMMAND_SANCTION_ERROR);
            return;
        }

        EssentialsServer server = plugin.getEssentialsServer();
        IStorage iStorage = plugin.getStorageManager().getStorage();

        // Check if the mute duration is valid
        if (duration.isZero()) {
            message(sender, Message.COMMAND_MUTE_ERROR_DURATION);
            return;
        }

        // Calculate the mute finish date
        Date finishAt = new Date(System.currentTimeMillis() + duration.toMillis());

        // Create and save the sanction
        Sanction sanction = Sanction.mute(uuid, getSenderUniqueId(sender), reason, duration, finishAt);
        iStorage.insertSanction(sanction, index -> {
            sanction.setId(index);
            iStorage.updateUserMute(uuid, index);

            User user = iStorage.getUser(uuid);
            if (user != null) {// If user is online, update cache
                user.setMuteSanction(sanction);
            }
        });
        this.expiringCache.clear(uuid);

        // Mute the player with the specified reason and duration
        server.sendMessage(uuid, Message.MESSAGE_MUTE, "%reason%", reason, "%duration%", TimerBuilder.getStringTime(duration.toMillis()));

        // Broadcast a notification message to players with the mute notify permission
        server.broadcastMessage(Permission.ESSENTIALS_MUTE_NOTIFY, Message.COMMAND_MUTE_NOTIFY, "%player%", sender.getName(), "%target%", playerName, "%reason%", reason, "%duration%", TimerBuilder.getStringTime(duration.toMillis()), "%sender%", getSanctionBy(sender), "%created_at%", this.simpleDateFormat.format(new Date()), "%expired_at%", this.simpleDateFormat.format(sanction.getExpiredAt()));
    }

    @Override
    public void unmute(CommandSender sender, UUID uuid, String playerName, String reason) {

        if (isProtected(playerName)) {
            message(sender, Message.COMMAND_SANCTION_ERROR);
            return;
        }

        IStorage iStorage = plugin.getStorageManager().getStorage();

        User user = iStorage.getUser(uuid);
        if (user == null) {
            // Check is user is mute
            this.plugin.getScheduler().runAsync(wrappedTask -> {
                if (!iStorage.isMute(uuid)) {
                    message(sender, Message.COMMAND_UN_MUTE_ERROR, "%player%", playerName);
                    return;
                }

                processUnmute(sender, uuid, playerName, reason);
            });
        } else {
            // Check is user is mute
            if (!user.isMute()) {
                message(sender, Message.COMMAND_UN_MUTE_ERROR, "%player%", playerName);
                return;
            }

            processUnmute(sender, uuid, playerName, reason);
        }
    }

    @Override
    public void processUnmute(CommandSender sender, UUID uuid, String playerName, String reason) {

        EssentialsServer server = plugin.getEssentialsServer();
        IStorage iStorage = plugin.getStorageManager().getStorage();

        // Create and save the sanction
        Sanction sanction = Sanction.unmute(uuid, getSenderUniqueId(sender), reason);
        iStorage.insertSanction(sanction, index -> {
            sanction.setId(index);
            iStorage.updateUserMute(uuid, null);

            User user = iStorage.getUser(uuid);
            if (user != null) { // If user is online, update cache
                user.setMuteSanction(null);
            }
        });
        this.expiringCache.clear(uuid);

        // Mute the player with the specified reason and duration
        server.sendMessage(uuid, Message.MESSAGE_UNMUTE, "%reason%", reason);

        // Broadcast a notification message to players with the mute notify permission
        server.broadcastMessage(Permission.ESSENTIALS_UNMUTE_NOTIFY, Message.COMMAND_UNMUTE_NOTIFY, "%player%", sender.getName(), "%target%", playerName, "%reason%", reason, "%sender%", getSanctionBy(sender), "%created_at%", this.simpleDateFormat.format(new Date()), "%duration%", "0");
    }

    @Override
    public void unban(CommandSender sender, UUID uuid, String playerName, String reason) {

        if (isProtected(playerName)) {
            message(sender, Message.COMMAND_SANCTION_ERROR);
            return;
        }

        EssentialsServer server = plugin.getEssentialsServer();
        IStorage iStorage = plugin.getStorageManager().getStorage();
        if (!iStorage.isBan(uuid)) {
            message(sender, Message.COMMAND_UN_BAN_ERROR, "%player%", playerName);
            return;
        }

        // Create and save the sanction
        Sanction sanction = Sanction.unban(uuid, getSenderUniqueId(sender), reason);
        iStorage.insertSanction(sanction, index -> {
            sanction.setId(index);
            iStorage.updateUserBan(uuid, null);
        });
        this.expiringCache.clear(uuid);

        // Broadcast a notification message to players with the mute notify permission
        server.broadcastMessage(Permission.ESSENTIALS_UNBAN_NOTIFY, Message.COMMAND_UNBAN_NOTIFY, "%player%", sender.getName(), "%target%", playerName, "%reason%", reason, "%sender%", getSanctionBy(sender), "%created_at%", this.simpleDateFormat.format(new Date()), "%duration%", "0");
    }

    @Override
    public void openSanction(User user, UUID uuid, String userName) {

        IStorage iStorage = this.plugin.getStorageManager().getStorage();
        this.plugin.getScheduler().runAsync(wrappedTask -> {

            user.setTargetUser(expiringCache.get(uuid, () -> {
                User fakeUser = ZUser.fakeUser(this.plugin, uuid, userName);
                Sanction muteSanction = iStorage.getMute(uuid);
                fakeUser.setFakeOption(Option.BAN, iStorage.isBan(uuid));
                fakeUser.setFakeOption(Option.MUTE, muteSanction != null && muteSanction.isActive());
                fakeUser.setMuteSanction(muteSanction);
                fakeUser.setBanSanction(iStorage.getBan(uuid));
                fakeUser.setFakeSanctions(iStorage.getSanctions(uuid));
                return fakeUser;
            }));

            this.plugin.openInventory(user.getPlayer(), "sanction");
        });
    }

    @Override
    public String getSanctionBy(UUID senderUniqueId) {
        return senderUniqueId.equals(this.plugin.getConsoleUniqueId()) ? Message.CONSOLE.getMessageAsString() : Bukkit.getOfflinePlayer(senderUniqueId).getName();
    }

    @Override
    public String getSanctionBy(CommandSender sender) {
        return sender instanceof Player player ? player.getName() : Message.CONSOLE.getMessageAsString();
    }

    @Override
    public boolean isProtected(String username) {
        return this.protections.stream().anyMatch(name -> name.equalsIgnoreCase(username));
    }

    @Override
    public void seen(CommandSender sender, UUID uuid) {

        IStorage iStorage = this.plugin.getStorageManager().getStorage();
        UserRecord record = iStorage.fetchUserRecord(uuid);
        UserDTO user = record.userDTO();

        boolean isOnline = Bukkit.getPlayer(user.unique_id()) != null;
        if (isOnline) sendOnline(sender, record);
        else sendOffline(sender, record);

        if (this.seenShowUuid) {
            message(sender, Message.COMMAND_SEEN_UUID, "%uuid%", uuid.toString());
        }

        if (this.seenShowIp && hasPermission(sender, Permission.ESSENTIALS_SEEN_SHOW_IP)) {
            message(sender, Message.COMMAND_SEEN_IP, "%ips%", record.playTimeDTOS().stream().map(timeDTO -> getMessage(Message.COMMAND_SEEN_ADDRESS, "%ip%", timeDTO.address())).distinct().collect(Collectors.joining(",")));
        }

        if (this.seenShowLastLocation && user.last_location() != null && hasPermission(sender, Permission.ESSENTIALS_SEEN_SHOW_LAST_LOCATION)) {
            SafeLocation location = stringAsLocation(user.last_location());
            message(sender, Message.COMMAND_SEEN_LAST_LOCATION, "%x%", location.getBlockX(), "%z%", location.getBlockZ(), "%y%", location.getBlockY(), "%world%", location.getWorld());
        }

        if (this.seenShowCreatedAt && user.created_at() != null && hasPermission(sender, Permission.ESSENTIALS_SEEN_SHOW_CREATED_AT)) {
            message(sender, Message.COMMAND_SEEN_FIRST_JOIN, "%created_at%", this.simpleDateFormat.format(user.created_at()));
        }
    }

    private void sendOnline(CommandSender sender, UserRecord record) {
        User user = this.plugin.getUser(record.userDTO().unique_id());
        if (user == null) {
            sendOffline(sender, record);
            return;
        }
        message(sender, Message.COMMAND_SEEN_ONLINE, "%player%", record.userDTO().name(), "%date%", TimerBuilder.getStringTime(System.currentTimeMillis() - user.getCurrentSessionPlayTime()));
        if (this.seenShowPlaytime) {
            message(sender, Message.COMMAND_SEEN_PLAYTIME, "%playtime%", TimerBuilder.getStringTime(user.getPlayTime() * 1000));
        }
    }

    private void sendOffline(CommandSender sender, UserRecord record) {
        message(sender, Message.COMMAND_SEEN_OFFLINE, "%player%", record.userDTO().name(), "%date%", this.simpleDateFormat.format(record.userDTO().updated_at()));
        if (this.seenShowPlaytime) {
            message(sender, Message.COMMAND_SEEN_PLAYTIME, "%playtime%", TimerBuilder.getStringTime(record.userDTO().play_time() * 1000));
        }
    }

    @Override
    public void seen(CommandSender sender, String ip) {

        IStorage iStorage = this.plugin.getStorageManager().getStorage();
        List<UserDTO> userDTOS = iStorage.getUsers(ip);

        if (userDTOS.isEmpty()) {
            message(sender, Message.COMMAND_SEEN_IP_EMPTY, "%ip%", ip);
            return;
        }

        message(sender, Message.COMMAND_SEEN_IP_LINE, "%ip%", ip, "%players%", userDTOS.stream().map(user -> getMessage(Message.COMMAND_SEEN_IP_INFO, "%name%", user.name())).collect(Collectors.joining(",")));
    }

    @Override
    public void freeze(CommandSender sender, UUID uuid, String userName) {
        this.setFrozenState(sender, uuid, userName, true);
    }

    @Override
    public void unfreeze(CommandSender sender, UUID uuid, String userName) {
        this.setFrozenState(sender, uuid, userName, false);
    }

    @Override
    public void toggleFreeze(CommandSender sender, UUID uuid, String userName) {
        User current = this.plugin.getUser(uuid);
        this.setFrozenState(sender, uuid, userName, current == null || !current.isFrozen());
    }

    /**
     * Freezes or unfreezes a player. A frozen player cannot move, chat or run commands,
     * glows blue and is surrounded by a continuous circle of snowflake particles.
     */
    private void setFrozenState(CommandSender sender, UUID uuid, String userName, boolean frozen) {

        if (isProtected(userName)) {
            message(sender, Message.COMMAND_SANCTION_ERROR);
            return;
        }

        User user = this.plugin.getUser(uuid);
        if (user == null) {
            message(sender, Message.PLAYER_NOT_FOUND, "%player%", userName);
            return;
        }

        if (user.isFrozen() == frozen && sender != null) {
            message(sender, frozen ? Message.COMMAND_FREEZE_SUCCESS : Message.COMMAND_UN_FREEZE_SUCCESS, "%player%", userName);
            return;
        }

        user.setFrozen(frozen);

        // Audit the freeze in the database, unfreezes are not stored as sanctions
        if (frozen) {
            IStorage iStorage = this.plugin.getStorageManager().getStorage();
            Sanction sanction = Sanction.freeze(uuid, getSenderUniqueId(sender));
            iStorage.insertSanction(sanction, index -> {
                sanction.setId(index);
                iStorage.updateUserFrozen(uuid, true);
            });
        } else {
            IStorage iStorage = this.plugin.getStorageManager().getStorage();
            iStorage.updateUserFrozen(uuid, false);
        }

        Player player = user.getPlayer();
        if (player != null) {
            this.applyFrozenVisuals(player, frozen);
        }

        if (frozen) {
            message(sender, Message.COMMAND_FREEZE_SUCCESS, "%player%", userName);
            this.plugin.getEssentialsServer().sendMessage(uuid, Message.MESSAGE_FREEZE);
        } else {
            message(sender, Message.COMMAND_UN_FREEZE_SUCCESS, "%player%", userName);
            this.plugin.getEssentialsServer().sendMessage(uuid, Message.MESSAGE_UN_FREEZE);
        }
    }

    /**
     * Applies or removes every visual and movement restriction of the freeze.
     */
    private void applyFrozenVisuals(Player player, boolean frozen) {

        player.setWalkSpeed(frozen ? 0f : 0.2f);
        player.setFlySpeed(frozen ? 0f : 0.1f);

        // Blue glow through a colored scoreboard team
        Team team = getFrozenTeam();
        if (frozen) {
            team.addEntry(player.getName());
            player.setGlowing(true);
        } else {
            team.removeEntry(player.getName());
            player.setGlowing(false);
        }

        // Continuous circle of blue particles around the target
        UUID uniqueId = player.getUniqueId();
        com.tcoded.folialib.wrapper.task.WrappedTask existing = this.particleTasks.remove(uniqueId);
        if (existing != null) existing.cancel();

        if (!frozen) return;

        Location center = player.getLocation().clone();
        var task = this.plugin.getScheduler().runAtLocationTimer(center, new Runnable() {

            private double angle = 0;

            @Override
            public void run() {
                if (!player.isOnline() || !player.isFrozen()) {
                    com.tcoded.folialib.wrapper.task.WrappedTask self = particleTasks.remove(uniqueId);
                    if (self != null) self.cancel();
                    return;
                }

                angle += Math.PI / 16;
                Location base = player.getLocation();
                World world = base.getWorld();
                if (world == null) return;

                // Two opposite points orbiting the player, at three heights
                for (int level = 0; level < 3; level++) {
                    double y = 0.2 + level * 0.7;
                    double offset = level % 2 == 0 ? angle : -angle + Math.PI;
                    world.spawnParticle(Particle.SNOWFLAKE,
                            base.getX() + Math.cos(offset) * 1.0,
                            base.getY() + y,
                            base.getZ() + Math.sin(offset) * 1.0,
                            2, 0, 0, 0, 0);
                    world.spawnParticle(Particle.SNOWFLAKE,
                            base.getX() + Math.cos(offset + Math.PI) * 1.0,
                            base.getY() + y,
                            base.getZ() + Math.sin(offset + Math.PI) * 1.0,
                            2, 0, 0, 0, 0);
                }
            }
        }, 4L, 4L);

        this.particleTasks.put(uniqueId, task);
    }

    private Team getFrozenTeam() {

        Team team = Bukkit.getScoreboardManager().getMainScoreboard().getTeam(FROZEN_TEAM_NAME);
        if (team != null) return team;

        team = Bukkit.getScoreboardManager().getMainScoreboard().registerNewTeam(FROZEN_TEAM_NAME);
        team.setColor(org.bukkit.ChatColor.BLUE);
        team.setCanSeeFriendlyInvisibles(false);
        return team;
    }

    /**
     * A frozen player cannot move at all, rotations stay possible.
     */
    @EventHandler(ignoreCancelled = true)
    public void onMove(org.bukkit.event.player.PlayerMoveEvent event) {

        User user = this.getUser(event.getPlayer());
        if (user == null || !user.isFrozen()) return;

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        if (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()) {
            event.setTo(from);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        User user = this.getUser(event.getPlayer());
        if (user == null || !user.isFrozen()) return;

        // Freezes are session-only by default, stale flags from a previous
        // run are cleaned up so nobody stays stuck unable to move
        if (!this.persistFreeze) {
            user.setFrozen(false);
            this.plugin.getStorageManager().getStorage().updateUserFrozen(user.getUniqueId(), false);
            return;
        }

        Player player = user.getPlayer();
        if (player != null) {
            this.applyFrozenVisuals(player, true);
        }
        this.plugin.getEssentialsServer().sendMessage(user.getUniqueId(), Message.MESSAGE_FREEZE);
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        User user = this.getUser(event.getPlayer());
        if (user != null && user.isFrozen()) {
            event.setCancelled(true);
            this.plugin.getEssentialsServer().sendMessage(user.getUniqueId(), Message.MESSAGE_FREEZE);
        }
    }

    @Override
    public void cancelChatEvent(Cancellable event, Player player) {
        User user = getUser(player);
        if (user != null && user.isMute()) {
            event.setCancelled(true);
            Sanction sanction = user.getMuteSanction();
            if (sanction != null) {
                Duration duration = sanction.getDurationRemaining();
                message(player, Message.MESSAGE_MUTE_TALK, "%reason%", sanction.getReason(), "%duration%", TimerBuilder.getStringTime(duration.toMillis()));
            } else {
                message(player, Message.MESSAGE_MUTE_TALK, "%reason%", "", "%duration%", "");
            }
        }
    }
}
