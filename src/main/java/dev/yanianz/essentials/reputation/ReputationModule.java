package dev.yanianz.essentials.reputation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fr.maxlego08.essentials.ZEssentialsPlugin;
import fr.maxlego08.essentials.api.configuration.NonLoadable;
import fr.maxlego08.essentials.api.messages.Message;
import fr.maxlego08.essentials.module.ZModule;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple reputation system: players give +1 points to each other with a
 * per target cooldown, the scores are stored in a small json file.
 */
public class ReputationModule extends ZModule {

    private long cooldownHours;
    private String givePermission;
    private String broadcastLine;

    @NonLoadable
    private final Map<UUID, PlayerReputation> reputations = new ConcurrentHashMap<>();
    @NonLoadable
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public ReputationModule(ZEssentialsPlugin plugin) {
        super(plugin, "reputation");
    }

    @Override
    public void loadConfiguration() {
        super.loadConfiguration();

        var config = getConfiguration();
        this.cooldownHours = config.getLong("cooldown-hours", 24);
        this.givePermission = config.getString("give-permission", "");
        this.broadcastLine = config.getString("broadcast", "");

        this.reputations.clear();
        loadStorage();
    }

    /**
     * Gives one reputation point to the target player.
     *
     * @return SUCCESS when given, ALREADY when on cooldown, SELF when targeting self.
     */
    public Result give(UUID giver, UUID target) {

        if (giver.equals(target)) return Result.SELF;

        PlayerReputation reputation = this.reputations.computeIfAbsent(target,
                uniqueId -> new PlayerReputation());

        if (this.cooldownHours > 0) {
            long lastGiven = reputation.lastGivenBy.getOrDefault(giver, 0L);
            long cooldownMillis = this.cooldownHours * 3600_000L;
            if (System.currentTimeMillis() - lastGiven < cooldownMillis) {
                return Result.ALREADY;
            }
            reputation.lastGivenBy.put(giver, System.currentTimeMillis());
        }

        reputation.score++;
        saveStorage();
        return Result.SUCCESS;
    }

    public String givePermission() {
        return this.givePermission;
    }

    public long cooldownHours() {
        return this.cooldownHours;
    }

    public String broadcastLine() {
        return this.broadcastLine;
    }

    public int getScore(UUID uniqueId) {
        PlayerReputation reputation = this.reputations.get(uniqueId);
        return reputation == null ? 0 : reputation.score;
    }

    private File getStorageFile() {
        return new File(getFolder(), "reputations.json");
    }

    private void loadStorage() {

        File file = getStorageFile();
        if (!file.exists()) return;

        try {
            String json = Files.readString(file.toPath());
            RawStorage raw = this.gson.fromJson(json, RawStorage.class);
            if (raw == null || raw.entries == null) return;

            for (Map.Entry<String, RawEntry> entry : raw.entries.entrySet()) {
                try {
                    UUID uniqueId = UUID.fromString(entry.getKey());
                    PlayerReputation reputation = new PlayerReputation();
                    reputation.score = entry.getValue().score;
                    if (entry.getValue().last_given_by != null) {
                        for (Map.Entry<String, Long> giverEntry : entry.getValue().last_given_by.entrySet()) {
                            reputation.lastGivenBy.put(UUID.fromString(giverEntry.getKey()), giverEntry.getValue());
                        }
                    }
                    this.reputations.put(uniqueId, reputation);
                } catch (IllegalArgumentException ignored) {
                }
            }
        } catch (IOException | RuntimeException exception) {
            exception.printStackTrace();
        }
    }

    private void saveStorage() {

        RawStorage raw = new RawStorage();
        for (Map.Entry<UUID, PlayerReputation> entry : this.reputations.entrySet()) {
            RawEntry rawEntry = new RawEntry();
            rawEntry.score = entry.getValue().score;
            rawEntry.last_given_by = new HashMap<>();
            entry.getValue().lastGivenBy.forEach((uuid, millis) -> rawEntry.last_given_by.put(uuid.toString(), millis));
            raw.entries.put(entry.getKey().toString(), rawEntry);
        }

        try {
            Files.writeString(getStorageFile().toPath(), this.gson.toJson(raw));
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    public enum Result {
        SUCCESS,
        ALREADY,
        SELF
    }

    private static final class PlayerReputation {
        int score;
        final Map<UUID, Long> lastGivenBy = new HashMap<>();
    }

    private static final class RawStorage {
        Map<String, RawEntry> entries = new HashMap<>();
    }

    private static final class RawEntry {
        int score;
        Map<String, Long> last_given_by;
    }
}
