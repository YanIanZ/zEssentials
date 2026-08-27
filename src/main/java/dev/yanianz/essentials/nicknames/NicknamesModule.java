package dev.yanianz.essentials.nicknames;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fr.maxlego08.essentials.ZEssentialsPlugin;
import fr.maxlego08.essentials.api.configuration.NonLoadable;
import fr.maxlego08.essentials.module.ZModule;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Players change their own display name with /nick, everything is persisted
 * and re-applied on join.
 */
public class NicknamesModule extends ZModule {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private int maxLength;
    private String regexSource;
    private boolean blockImpersonation;
    private boolean allowColors;
    private int cooldownSeconds;

    @NonLoadable
    private Pattern allowedPattern;
    @NonLoadable
    private final Map<UUID, String> nicknames = new ConcurrentHashMap<>();
    @NonLoadable
    private final Map<UUID, Long> lastChange = new ConcurrentHashMap<>();
    @NonLoadable
    private final Gson gson = new GsonBuilder().create();

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
        String nickname = this.nicknames.get(player.getUniqueId());
        if (nickname == null) return;

        applyDisplayName(player, nickname);
    }

    /**
     * Validates a nickname against the configuration rules.
     *
     * @return an error message key when invalid, or null when valid.
     */
    public NickError validate(Player player, String rawNickname) {

        String stripped = rawNickname == null ? "" : rawNickname
                .replace("§x", "").replaceAll("§[0-9a-fk-orA-FK-OR]", "");

        if (stripped.length() > this.maxLength) return NickError.TOO_LONG;
        if (stripped.isBlank()) return NickError.INVALID_CHARACTERS;

        if (!this.allowColors && !player.hasPermission("essentials.nicknames.color")
                && (rawNickname.contains("&") || rawNickname.contains("§"))) {
            return NickError.COLORS_NOT_ALLOWED;
        }

        // Every visible character must match the configured whitelist
        for (char c : stripped.toCharArray()) {
            if (!Character.isLetterOrDigit(c) && c != '_' && !"§&".contains(String.valueOf(c))) {
                return NickError.INVALID_CHARACTERS;
            }
        }

        if (this.blockImpersonation) {
            String plain = PlainTextSupport.plain(LEGACY.deserialize(colorize(rawNickname)));
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (!online.equals(player) && online.getName().equalsIgnoreCase(plain)) {
                    return NickError.IMPERSONATION;
                }
                if (!online.equals(player) && online.getName().equalsIgnoreCase(stripped)) {
                    return NickError.IMPERSONATION;
                }
            }
        }

        return null;
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

    private void applyDisplayName(Player player, String legacyName) {
        Component component = LEGACY.deserialize(colorize(legacyName));
        player.displayName(component);
        player.playerListName(component);
    }

    private String colorize(String text) {
        return text == null ? "" : text.replace("&", "§");
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

    private static final class PlainTextSupport {
        static String plain(Component component) {
            return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(component);
        }
    }
}
