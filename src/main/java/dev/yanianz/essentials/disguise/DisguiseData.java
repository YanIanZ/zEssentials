package dev.yanianz.essentials.disguise;

import dev.yanianz.essentials.disguise.watcher.FlagWatcher;
import dev.yanianz.essentials.disguise.watcher.MobWatcherFactory;
import org.bukkit.entity.EntityType;

import java.util.UUID;

public class DisguiseData {

    private UUID playerId;
    private String disguiseName;
    private String textureValue;
    private String textureSignature;
    private String entityType;
    private long appliedAt;
    private boolean active;
    private FlagWatcher watcher;

    public UUID getPlayerId() { return playerId; }
    public void setPlayerId(UUID playerId) { this.playerId = playerId; }

    public String getDisguiseName() { return disguiseName; }
    public void setDisguiseName(String disguiseName) { this.disguiseName = disguiseName; }

    public String getTextureValue() { return textureValue; }
    public void setTextureValue(String textureValue) { this.textureValue = textureValue; }

    public String getTextureSignature() { return textureSignature; }
    public void setTextureSignature(String textureSignature) { this.textureSignature = textureSignature; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) {
        this.entityType = entityType;
        this.watcher = null;
        if (entityType != null) {
            try {
                EntityType type = EntityType.valueOf(entityType.toUpperCase());
                this.watcher = MobWatcherFactory.createWatcher(type);
            } catch (IllegalArgumentException ignored) {}
        }
    }

    public long getAppliedAt() { return appliedAt; }
    public void setAppliedAt(long appliedAt) { this.appliedAt = appliedAt; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public FlagWatcher getWatcher() { return watcher; }
    public void setWatcher(FlagWatcher watcher) { this.watcher = watcher; }

    public boolean isFullDisguise() { return disguiseName != null && textureValue != null; }
    public boolean isNameOnly() { return disguiseName != null && textureValue == null && entityType == null; }
    public boolean hasSkin() { return textureValue != null; }
    public boolean isMobDisguise() { return entityType != null; }
}
