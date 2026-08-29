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
    private Map<String, List<String>> worldEntries = new HashMap<>();
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

    private record GroupHeaderFooter(String permission, List<String> header, List<String> footer) {
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
                this.worldEntries.put(key.toLowerCase(Locale.ROOT), section.getStringList("header"));
                this.worldEntries.put(key.toLowerCase(Locale.ROOT) + "_footer", section.getStringList("footer"));
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
                this.groupEntries.add(new GroupHeaderFooter(permission,
                        section.getStringList("header"), section.getStringList("footer")));
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

        List<String> headerLines = null;
        List<String> footerLines = null;

        for (GroupHeaderFooter group : this.groupEntries) {
            if (player.hasPermission(group.permission())) {
                headerLines = group.header();
                footerLines = group.footer();
                break;
            }
        }

        if (headerLines == null) {
            String worldKey = player.getWorld().getName().toLowerCase(Locale.ROOT);
            if (this.worldEntries.containsKey(worldKey)) {
                headerLines = this.worldEntries.get(worldKey);
                footerLines = this.worldEntries.get(worldKey + "_footer");
            } else {
                headerLines = this.worldEntries.get("default");
                footerLines = this.worldEntries.get("default_footer");
            }
        }

        if (headerLines == null) return;

        player.sendPlayerListHeader(buildComponent(headerLines));
        if (footerLines != null) player.sendPlayerListFooter(buildComponent(footerLines));
    }

    private Component buildComponent(List<String> lines) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < lines.size(); index++) {
            String line = colorize(lines.get(index));
            builder.append(resolveAnimTokens(line));
            if (index < lines.size() - 1) builder.append("\n");
        }
        return LEGACY.deserialize(builder.toString());
    }

    private String resolveAnimTokens(String text) {
        if (!text.contains("%anim_") || this.animations.isEmpty()) return text;
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("%anim_([a-zA-Z0-9_-]+)%").matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String name = matcher.group(1);
            List<Component> frames = this.animations.get(name);
            if (frames == null || frames.isEmpty()) {
                matcher.appendReplacement(sb, matcher.group());
                continue;
            }
            int counter = this.animationCounters.getOrDefault(name, 0);
            Component frame = frames.get(counter % frames.size());
            String frameText = LEGACY.serialize(frame);
            matcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(frameText));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String colorize(String text) {
        return text == null ? "" : text.replace("&", "§");
    }
}
