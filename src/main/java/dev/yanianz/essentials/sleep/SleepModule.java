package dev.yanianz.essentials.sleep;

import fr.maxlego08.essentials.ZEssentialsPlugin;
import fr.maxlego08.essentials.api.configuration.NonLoadable;
import fr.maxlego08.essentials.module.ZModule;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Accelerates the night while enough players of a world are sleeping
 * instead of skipping it instantly.
 */
public class SleepModule extends ZModule {

    private int percentage = 50;
    private int minimumPlayers = 1;
    private long accelerateTicks = 100;
    private long checkIntervalSeconds = 1;
    private String broadcastStart;
    private String broadcastDawn;

    @NonLoadable
    private final Map<String, Boolean> acceleratingWorlds = new HashMap<>();

    public SleepModule(ZEssentialsPlugin plugin) {
        super(plugin, "sleep");
    }

    @Override
    public void loadConfiguration() {
        super.loadConfiguration();

        var config = getConfiguration();
        this.percentage = Math.min(100, Math.max(1, config.getInt("percentage", 50)));
        this.minimumPlayers = Math.max(1, config.getInt("minimum-players", 1));
        this.accelerateTicks = config.getLong("accelerate-ticks", 100);
        this.checkIntervalSeconds = Math.max(1, config.getLong("check-interval-seconds", 1));
        this.broadcastStart = config.getString("broadcast-start", "");
        this.broadcastDawn = config.getString("broadcast-dawn", "");

        this.acceleratingWorlds.clear();

        // One world independent ticker drives every normal environment world
        this.plugin.getScheduler().runTimer(this::tickWorlds, checkIntervalSeconds * 20L, checkIntervalSeconds * 20L);
    }

    /**
     * Night acceleration state machine, one entry per overworld style world.
     */
    private void tickWorlds() {

        if (!this.isEnable) return;

        for (World world : Bukkit.getWorlds()) {
            if (world.getEnvironment() != World.Environment.NORMAL) continue;

            List<Player> sleepers = new java.util.ArrayList<>();
            for (Player player : world.getPlayers()) {
                if (player.isSleeping()) sleepers.add(player);
            }

            boolean night = isNight(world);
            int required = night ? Math.max(this.minimumPlayers,
                    (int) Math.ceil(world.getPlayers().size() * this.percentage / 100.0)) : 0;

            String key = world.getName();
            boolean accelerating = Boolean.TRUE.equals(this.acceleratingWorlds.get(key));

            if (!night) {
                if (accelerating) finishWorld(world);
                continue;
            }

            if (sleepers.size() >= required && required > 0) {
                if (!accelerating) {
                    this.acceleratingWorlds.put(key, true);
                    announce(world, this.broadcastStart.replace("%count%", String.valueOf(sleepers.size()))
                            .replace("%needed%", String.valueOf(required)));
                }
                // Move the time forward smoothly instead of teleporting to the morning
                long next = world.getTime() + this.accelerateTicks;
                boolean crossedDawn = crossedDawn(world.getTime(), next);
                world.setTime(next);

                if (crossedDawn) {
                    finishWorld(world);
                }
            }
        }
    }

    private void finishWorld(World world) {
        this.acceleratingWorlds.remove(world.getName());
        if (world.getTime() > 23000 || world.getTime() < 500) world.setTime(0);
        announce(world, this.broadcastDawn);
    }

    /** Vanilla night window used by the skipping logic. */
    private boolean isNight(World world) {
        long t = world.getTime() % 24000L;
        return t >= 12542 && t <= 23458;
    }

    /** Detects a dawn crossing between two times regardless of day wrap. */
    private boolean crossedDawn(long previous, long next) {
        long prevDayTime = previous % 24000L;
        for (long t = prevDayTime + 1; t <= prevDayTime + (next - previous); t++) {
            long inDay = t % 24000L;
            if (inDay >= 0 && inDay <= 200) return true;
        }
        return false;
    }

    private void announce(World world, String legacyLine) {
        if (legacyLine == null || legacyLine.isEmpty() || "false".equalsIgnoreCase(legacyLine)) return;
        Component component = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                .legacySection().deserialize(colorize(legacyLine));
        world.getPlayers().forEach(player -> player.sendMessage(component));
    }

    private String colorize(String text) {
        return dev.yanianz.essentials.util.ColorUtil.sections(text);
    }
}
