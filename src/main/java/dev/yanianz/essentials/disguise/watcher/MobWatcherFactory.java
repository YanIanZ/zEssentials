package dev.yanianz.essentials.disguise.watcher;

import org.bukkit.entity.EntityType;

public final class MobWatcherFactory {
    private MobWatcherFactory() {}

    public static FlagWatcher createWatcher(EntityType type) {
        if (type == null) return new FlagWatcher();
        String name = type.name();
        return switch (name) {
            case "ZOMBIE", "HUSK", "DROWNED", "ZOMBIE_VILLAGER", "ZOMBIFIED_PIGLIN" -> new ZombieWatcher();
            case "VILLAGER", "WANDERING_TRADER" -> new VillagerWatcher();
            case "SLIME", "MAGMA_CUBE" -> new SlimeWatcher();
            default -> {
                // Check if it's an Ageable mob
                Class<?> entityClass = type.getEntityClass();
                if (entityClass != null && org.bukkit.entity.Ageable.class.isAssignableFrom(entityClass)) {
                    yield new AgeableWatcher();
                }
                yield new LivingWatcher();
            }
        };
    }
}
