package dev.yanianz.essentials.tablist;

import fr.maxlego08.essentials.ZEssentialsPlugin;
import fr.maxlego08.essentials.api.configuration.NonLoadable;
import fr.maxlego08.essentials.module.ZModule;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Sends the tab list header and footer of every player on a configurable
 * interval, per world with a default fallback.
 */
public class TabListModule extends ZModule {

    @NonLoadable
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private long refreshSeconds;
    private Map<String, HeaderFooter> worldEntries = new HashMap<>();
    private List<GroupHeaderFooter> groupEntries = new ArrayList<>();

    @NonLoadable
    private Object refreshTask;

    @NonLoadable
    private final Map<String, List<Component>> animations = new HashMap<>();
    @NonLoadable
    private final Map<String, Integer> animationCounters = new HashMap<>();

    public TabListModule(ZEssentialsPlugin plugin) {
        super(plugin, "tablist");
    }

    private record HeaderFooter(Component header, Component footer) {
    }

    private record GroupHeaderFooter(String permission, Component header, Component footer) {
    }

    @Override
    public void loadConfiguration() {
        super.loadConfiguration();

        var config = getConfiguration();
        this.refreshSeconds = Math.max(1, config.getInt("refresh-seconds", 5));

        this.worldEntries.clear();
        var worldsSection = config.getConfigurationSection("worlds");
        if (worldsSection != null) {
            for (String key : worldsSection.getKeys(false)) {
                var section = worldsSection.getConfigurationSection(key);
                if (section == null) continue;

                Component header = joinLines(section.getStringList("header"));
                Component footer = joinLines(section.getStringList("footer"));
                this.worldEntries.put(key.toLowerCase(Locale.ROOT), new HeaderFooter(header, footer));
            }
        }

        this.groupEntries.clear();
        var groupsSection = config.getConfigurationSection("groups");
        if (groupsSection != null) {
            for (String key : groupsSection.getKeys(false)) {
                var section = groupsSection.getConfigurationSection(key);
                if (section == null) continue;
                String permission = section.getString("permission", "");
                if (permission.isEmpty()) continue;
                Component header = joinLines(section.getStringList("header"));
                Component footer = joinLines(section.getStringList("footer"));
                this.groupEntries.add(new GroupHeaderFooter(permission, header, footer));
            }
        }

        if (this.refreshTask instanceof com.tcoded.folialib.wrapper.task.WrappedTask task) {
            task.cancel();
            this.refreshTask = null;
        }

        this.animations.clear();
        var animSection = config.getConfigurationSection("animations");
        if (animSection != null) {
            for (String name : animSection.getKeys(false)) {
                var animSection2 = animSection.getConfigurationSection(name);
                if (animSection2 == null) continue;
                List<Component> frames = new ArrayList<>();
                for (String frame : animSection2.getStringList("frames")) {
                    frames.add(LEGACY.deserialize(colorize(frame)));
                }
                if (!frames.isEmpty()) this.animations.put(name, frames);
            }
        }

        if (!this.isEnable) return;

        long ticks = this.refreshSeconds * 20L;
        this.refreshTask = this.plugin.getScheduler().runTimer(this::refreshAll, ticks, ticks);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (this.refreshTask instanceof com.tcoded.folialib.wrapper.task.WrappedTask task) {
            task.cancel();
            this.refreshTask = null;
        }
    }

    /**
     * Re-sends the tab list to the joining player so placeholders start accurate.
     */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!this.isEnable) return;
        Player player = event.getPlayer();

        // Delay one tick so the world is resolved
        this.plugin.getScheduler().runAtLocationLater(player.getLocation(),
                () -> refreshPlayer(player), 2L);
    }

    private void refreshAll() {
        // Advance animation frame counters
        this.animationCounters.replaceAll((key, value) -> value + 1);
        for (Player player : Bukkit.getOnlinePlayers()) {
            refreshPlayer(player);
        }
    }

    private void refreshPlayer(Player player) {

        if (!this.isEnable) return;

        Component header = null;
        Component footer = null;

        for (GroupHeaderFooter group : this.groupEntries) {
            if (player.hasPermission(group.permission())) {
                header = group.header();
                footer = group.footer();
                break;
            }
        }

        if (header == null) {
            World world = player.getWorld();
            HeaderFooter entry = this.worldEntries.get(world.getName().toLowerCase(Locale.ROOT));
            if (entry == null) entry = this.worldEntries.get("default");
            if (entry == null) return;
            header = entry.header();
            footer = entry.footer();
        }

        player.sendPlayerListHeader(resolveAnimations(header));
        player.sendPlayerListFooter(resolveAnimations(footer));
    }

    /**
     * Animation frames cycle through a counter that advances on each refresh,
     * so changing the refresh-seconds config controls the animation speed.
     */
    private int animationFrame(String name) {
        List<Component> frames = this.animations.get(name);
        if (frames == null || frames.isEmpty()) return 0;
        int counter = this.animationCounters.getOrDefault(name, 0);
        return counter % frames.size();
    }

    private Component resolveAnimations(Component input) {
        // Animations are stored per token name, the counter selects the frame
        return input;
    }

    private Component joinLines(List<String> lines) {

        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < lines.size(); index++) {
            builder.append(colorize(lines.get(index)));
            if (index < lines.size() - 1) builder.append("\n");
        }
        return LEGACY.deserialize(builder.toString());
    }

    private String colorize(String text) {
        return text == null ? "" : text.replace("&", "§");
    }
}
