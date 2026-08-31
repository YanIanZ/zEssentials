package dev.yanianz.essentials.nicknames;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.yanianz.essentials.disguise.DisguiseData;
import dev.yanianz.essentials.disguise.SkinCache;
import fr.maxlego08.essentials.ZEssentialsPlugin;
import fr.maxlego08.essentials.api.configuration.NonLoadable;
import fr.maxlego08.essentials.module.ZModule;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Players change their own display name with /nick, everything is persisted
 * and re-applied on join.
 */
public class NicknamesModule extends ZModule {

    @NonLoadable
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private int maxLength;
    private String regexSource;
    private boolean blockImpersonation;
    private boolean allowColors;
    private int cooldownSeconds;

    private boolean disguiseEnabled;
    private boolean selfView;
    private boolean hideFromTab;
    private boolean fallbackOwnSkin;
    private int disguiseCooldownSeconds;
    private int skinCacheHours;
    private boolean blockStaff;
    private List<String> randomPool = new java.util.ArrayList<>();
    private List<String> randomNickPool = new java.util.ArrayList<>();
    private List<String> allowedMobs = new java.util.ArrayList<>();

    @NonLoadable
    private Pattern allowedPattern;
    @NonLoadable
    private final Map<UUID, String> nicknames = new ConcurrentHashMap<>();
    @NonLoadable
    private final Map<UUID, Long> lastChange = new ConcurrentHashMap<>();
    @NonLoadable
    private final Gson gson = new GsonBuilder().create();
    @NonLoadable
    private final Map<UUID, DisguiseData> disguises = new ConcurrentHashMap<>();
    @NonLoadable
    private final Map<UUID, Long> lastDisguiseChange = new ConcurrentHashMap<>();
    @NonLoadable
    private SkinCache skinCache;
    @NonLoadable
    private com.tcoded.folialib.wrapper.task.WrappedTask skinCacheEvictTask;

    public NicknamesModule(ZEssentialsPlugin plugin) {
        super(plugin, "nicknames");
    }

    @Override
    public void loadConfiguration() {
        super.loadConfiguration();

        var config = getConfiguration();
        this.maxLength = Math.max(3, config.getInt("max-length", 16));
        this.regexSource = config.getString("regex", "^[a-zA-Z0-9_]{1,32}$");
        try {
            this.allowedPattern = Pattern.compile(this.regexSource);
        } catch (Exception exception) {
            this.plugin.getLogger().warning("Invalid nicknames regex: " + this.regexSource);
            this.allowedPattern = Pattern.compile("^[a-zA-Z0-9_]{1,32}$");
        }
        this.blockImpersonation = config.getBoolean("block-impersonation", true);
        this.allowColors = config.getBoolean("allow-colors", true);
        this.cooldownSeconds = Math.max(0, config.getInt("cooldown-seconds", 60));

        this.disguiseEnabled = config.getBoolean("disguise.enable", true);
        this.selfView = config.getBoolean("disguise.self-view", false);
        this.hideFromTab = config.getBoolean("disguise.hide-from-tab", true);
        this.fallbackOwnSkin = config.getBoolean("disguise.fallback-own-skin", true);
        this.disguiseCooldownSeconds = Math.max(0, config.getInt("disguise.cooldown-seconds", 120));
        this.skinCacheHours = Math.max(1, config.getInt("disguise.skin-cache-hours", 24));
        this.blockStaff = config.getBoolean("disguise.block-staff", false);
        this.randomPool = config.getStringList("disguise.random-pool");
        this.randomNickPool = config.getStringList("random-nick-pool");
        if (this.randomNickPool == null || this.randomNickPool.isEmpty()) this.randomNickPool = this.randomPool;
        if (this.randomPool == null) this.randomPool = new java.util.ArrayList<>();
        this.allowedMobs = config.getStringList("disguise.allowed-mobs");
        if (this.allowedMobs == null) this.allowedMobs = new java.util.ArrayList<>();
        this.skinCache = new SkinCache(this.skinCacheHours * 3600_000L);
        if (this.skinCacheEvictTask != null) {
            this.skinCacheEvictTask.cancel();
        }
        this.skinCacheEvictTask = this.plugin.getScheduler().runTimer(this.skinCache::evictExpired, 600L * 20L, 600L * 20L);

        this.disguises.clear();
        loadDisguiseStorage();
        migrateOldStorage();

        this.nicknames.clear();
        loadStorage();
    }

    /**
     * Applies the stored nickname of the joining player.
     */
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

        if (disguise != null) {
            this.plugin.getScheduler().runAtEntityLater(player, wrappedTask -> refreshDisguise(player), 40L);
        }
    }

    /**
     * Validates a nickname against the configuration rules.
     *
     * @return an error message key when invalid, or null when valid.
     */
    public NickError validate(Player player, String rawNickname) {

        String stripped = rawNickname == null ? "" : plain(rawNickname);

        if (stripped.length() > this.maxLength) return NickError.TOO_LONG;
        if (stripped.isBlank()) return NickError.INVALID_CHARACTERS;

        if (!this.allowColors && !player.hasPermission("essentials.nicknames.color")
                && (rawNickname.contains("&") || rawNickname.contains("§"))) {
            return NickError.COLORS_NOT_ALLOWED;
        }

        for (char c : stripped.toCharArray()) {
            if (!Character.isLetterOrDigit(c) && c != '_') {
                return NickError.INVALID_CHARACTERS;
            }
        }

        if (this.blockImpersonation) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (!online.equals(player) && online.getName().equalsIgnoreCase(stripped)) {
                    return NickError.IMPERSONATION;
                }
            }
        }

        return null;
    }

    private static String plain(String text) {
        if (text == null || text.isEmpty()) return "";
        String withSections = dev.yanianz.essentials.util.ColorUtil.sections(text);
        return PlainTextComponentSerializer.plainText().serialize(LEGACY.deserialize(withSections));
    }

    /**
     * Changes or removes the nickname of a target uuid.
     *
     * @param nickname null removes the nickname.
     */
    public void setNickname(UUID uniqueId, String nickname) {

        if (nickname == null || nickname.isBlank()) {
            this.nicknames.remove(uniqueId);
        } else {
            this.nicknames.put(uniqueId, nickname);
        }
        saveStorage();

        Player player = Bukkit.getPlayer(uniqueId);
        if (player != null) {
            if (isDisguised(uniqueId)) {
                DisguiseData disguise = getDisguise(uniqueId);
                if (disguise != null && disguise.getDisguiseName() != null) {
                    applyDisplayName(player, disguise.getDisguiseName());
                    return;
                }
            }
            if (nickname == null) {
                applyDisplayName(player, player.getName());
            } else {
                applyDisplayName(player, nickname);
            }
        }
    }

    public int maxLengthValue() {
        return this.maxLength;
    }

    public String getNickname(UUID uniqueId) {
        return this.nicknames.get(uniqueId);
    }

    public boolean isOnCooldown(UUID uniqueId) {
        long last = this.lastChange.getOrDefault(uniqueId, 0L);
        return System.currentTimeMillis() - last < this.cooldownSeconds * 1000L;
    }

    public void markChanged(UUID uniqueId) {
        this.lastChange.put(uniqueId, System.currentTimeMillis());
    }

    public int getRemainingCooldown(UUID uniqueId) {
        long remainingMs = this.cooldownSeconds * 1000L - (System.currentTimeMillis() - lastChange.getOrDefault(uniqueId, 0L));
        return (int) Math.max(0, remainingMs / 1000L + (remainingMs % 1000L == 0 ? 0 : 1));
    }

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
        refreshDisguise(player);
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
            refreshDisguise(player);
        }
    }

    /**
     * LibsDisguises-style refresh: forces every viewer to receive a fresh
     * spawn sequence for the disguised player. The hide/show cycle makes the
     * server resend the spawn + metadata packets, which the ProtocolLib
     * listeners rewrite in-flight to the disguise.
     */
    public void refreshDisguise(Player player) {
        if (!this.isEnable) return;

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.getUniqueId().equals(player.getUniqueId())) continue;
            this.plugin.getScheduler().runAtEntity(viewer, wrappedTask -> {
                if (!viewer.isOnline() || !player.isOnline()) return;
                try {
                    viewer.hidePlayer(this.plugin, player);
                    viewer.showPlayer(this.plugin, player);
                } catch (Exception ignored) {
                }
            });
        }

        if (this.selfView) {
            this.plugin.getScheduler().runAtEntity(player, wrappedTask -> sendSelfViewPackets(player, isDisguised(player.getUniqueId())));
        }
    }

    /**
     * Self-view: destroys the player's own entity client-side and respawns it
     * as the mob so F5 third person shows the disguise. Movement stays in sync
     * because the fake entity reuses the real entity id (client predicts it).
     */
    private void sendSelfViewPackets(Player player, boolean disguised) {
        try {
            if (!Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")) return;
            var manager = com.comphenix.protocol.ProtocolLibrary.getProtocolManager();

            var destroy = manager.createPacket(com.comphenix.protocol.PacketType.Play.Server.ENTITY_DESTROY);
            destroy.getIntegerArrays().write(0, new int[]{player.getEntityId()});
            manager.sendServerPacket(player, destroy);

            var spawn = manager.createPacket(com.comphenix.protocol.PacketType.Play.Server.SPAWN_ENTITY);
            spawn.getIntegers().write(0, player.getEntityId());
            spawn.getUUIDs().write(0, player.getUniqueId());
            spawn.getDoubles().write(0, player.getLocation().getX());
            spawn.getDoubles().write(1, player.getLocation().getY());
            spawn.getDoubles().write(2, player.getLocation().getZ());

            if (disguised) {
                DisguiseData data = getDisguise(player.getUniqueId());
                if (data != null && data.getEntityType() != null) {
                    org.bukkit.entity.EntityType type = org.bukkit.entity.EntityType.valueOf(data.getEntityType().toUpperCase());
                    try {
                        spawn.getEntityTypeModifier().write(0, type);
                    } catch (Exception e) {
                        spawn.getIntegers().write(1, (int) type.getTypeId());
                    }
                } else {
                    try {
                        spawn.getEntityTypeModifier().write(0, org.bukkit.entity.EntityType.PLAYER);
                    } catch (Exception e) {
                        return;
                    }
                }
            } else {
                try {
                    spawn.getEntityTypeModifier().write(0, org.bukkit.entity.EntityType.PLAYER);
                } catch (Exception e) {
                    return;
                }
            }

            manager.sendServerPacket(player, spawn);
        } catch (Exception ignored) {
        }
    }

    public List<String> getRandomPool() {
        return this.randomPool;
    }

    public List<String> getRandomNickPool() {
        return this.randomNickPool;
    }

    public List<String> getAllowedMobs() {
        return this.allowedMobs;
    }

    public boolean isDisguiseEnabled() {
        return this.disguiseEnabled;
    }

    public boolean isSelfView() {
        return this.selfView;
    }

    public boolean isHideFromTab() {
        return this.hideFromTab;
    }

    public boolean isFallbackOwnSkin() {
        return this.fallbackOwnSkin;
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

    private void applyDisplayName(Player player, String legacyName) {
        Component component = dev.yanianz.essentials.util.ColorUtil.component(legacyName);
        player.displayName(component);
        player.playerListName(component);
    }

    public enum NickError {
        TOO_LONG,
        INVALID_CHARACTERS,
        COLORS_NOT_ALLOWED,
        IMPERSONATION
    }

    private File getStorageFile() {
        return new File(getFolder(), "nicknames.json");
    }

    private void loadStorage() {
        File file = getStorageFile();
        if (!file.exists()) return;
        try {
            String json = Files.readString(file.toPath());
            Storage storage = this.gson.fromJson(json, Storage.class);
            if (storage != null && storage.entries != null) {
                for (Map.Entry<String, String> entry : storage.entries.entrySet()) {
                    try {
                        this.nicknames.put(UUID.fromString(entry.getKey()), entry.getValue());
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
        } catch (IOException | RuntimeException exception) {
            exception.printStackTrace();
        }
    }

    private void saveStorage() {
        Storage storage = new Storage();
        this.nicknames.forEach((uuid, nickname) -> storage.entries.put(uuid.toString(), nickname));
        try {
            Files.writeString(getStorageFile().toPath(), this.gson.toJson(storage));
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    private static final class Storage {
        Map<String, String> entries = new HashMap<>();
    }

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
}
