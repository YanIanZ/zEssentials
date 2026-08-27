package dev.yanianz.essentials.nametags;

import fr.maxlego08.essentials.ZEssentialsPlugin;
import fr.maxlego08.essentials.api.configuration.NonLoadable;
import fr.maxlego08.essentials.module.ZModule;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Applies scoreboard teams to players so their nametag shows the group
 * prefix/suffix above the head and the tab list orders players by group
 * priority — the same mechanism the TAB plugin uses.
 */
public class NameTagModule extends ZModule {

    private long applyDelayTicks = 20;
    private String fallbackTabFormat = "&7%player%";

    private boolean belowNameEnabled;
    private String belowNameMode;
    private Component belowNameDisplay;
    private String belowNamePlaceholder;

    private boolean spectatorEnabled;
    private Component spectatorTabName;
    private long belowNameRefreshSeconds = 3;

    @NonLoadable
    private Object belowNameRefreshTask;

    @NonLoadable
    private final List<GroupRule> rules = new ArrayList<>();

    public NameTagModule(ZEssentialsPlugin plugin) {
        super(plugin, "nametags");
    }

    /** One group rule, first match in the configuration wins. */
    private record GroupRule(int priority, int orderIndex, String permission,
                             Component prefix, Component suffix, Component tabFormat) {
    }

    @Override
    public void loadConfiguration() {
        super.loadConfiguration();

        var config = getConfiguration();
        this.applyDelayTicks = Math.max(0, config.getInt("apply-delay-ticks", 20));

        this.belowNameEnabled = config.getBoolean("belowname.enabled", false);
        this.belowNameMode = config.getString("belowname.mode", "HEALTH").toUpperCase(Locale.ROOT);
        this.belowNameDisplay = dev.yanianz.essentials.util.ColorUtil.component(
                config.getString("belowname.display-name", "&#ff4d4d♥ Health"));
        this.belowNamePlaceholder = config.getString("belowname.placeholder", "%player_health%");

        this.spectatorEnabled = config.getBoolean("spectator.enabled", false);
        this.spectatorTabName = dev.yanianz.essentials.util.ColorUtil.component(
                config.getString("spectator.tab-name-format", "&8&oSpectator"));
        this.belowNameRefreshSeconds = config.getLong("belowname.refresh-seconds", 3);

        // Belowname placeholder mode needs periodic score updates
        if (this.belowNameRefreshTask != null) {
            ((com.tcoded.folialib.wrapper.task.WrappedTask) this.belowNameRefreshTask).cancel();
            this.belowNameRefreshTask = null;
        }
        if (this.belowNameEnabled && "PLACEHOLDER".equals(this.belowNameMode)) {
            this.belowNameRefreshTask = this.plugin.getScheduler().runTimer(
                    this::updateBelowNameScores, 20L, this.belowNameRefreshSeconds * 20L);
        }

        this.rules.clear();
        for (Object obj : config.getMapList("groups")) {
            if (!(obj instanceof Map<?, ?> raw)) continue;

            String permission = raw.get("permission") == null ? "" : String.valueOf(raw.get("permission"));
            int priority = raw.get("priority") instanceof Number number ? number.intValue() : 500;
            String prefixLegacy = raw.get("prefix") == null ? "" : String.valueOf(raw.get("prefix"));
            String suffixLegacy = raw.get("suffix") == null ? "" : String.valueOf(raw.get("suffix"));
            String tabFormat = raw.get("tab-name-format") == null ? "&7%player%" : String.valueOf(raw.get("tab-name-format"));

            this.rules.add(new GroupRule(
                    priority,
                    this.rules.size(),
                    permission,
                    dev.yanianz.essentials.util.ColorUtil.component(prefixLegacy),
                    dev.yanianz.essentials.util.ColorUtil.component(suffixLegacy),
                    dev.yanianz.essentials.util.ColorUtil.component(tabFormat)));
        }
    }

    /**
     * Applies the matching group rule after other plugins loaded prefixes.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {

        if (!this.isEnable) return;

        Player player = event.getPlayer();
        this.plugin.getScheduler().runAtLocationLater(player.getLocation(),
                () -> apply(player), this.applyDelayTicks);
    }

    /**
     * Applies the spectator tab name override when the gamemode changes.
     */
    @EventHandler
    public void onGameModeChange(org.bukkit.event.player.PlayerGameModeChangeEvent event) {
        if (!this.isEnable || !this.spectatorEnabled) return;
        applySpectatorFix(event.getPlayer(), event.getNewGameMode());
    }

    private void applySpectatorFix(Player player, org.bukkit.GameMode gameMode) {
        if (gameMode == org.bukkit.GameMode.SPECTATOR) {
            player.playerListName(this.spectatorTabName);
        } else {
            this.plugin.getScheduler().runAtLocationLater(player.getLocation(),
                    () -> apply(player), 2L);
        }
    }

    /**
     * Ensures the below name objective exists on the main scoreboard.
     */
    private org.bukkit.scoreboard.Objective belowNameObjective() {
        try {
            Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
            org.bukkit.scoreboard.Objective objective = scoreboard.getObjective("zessentials_bn");
            if (objective == null) {
                org.bukkit.scoreboard.Criteria criteria = "HEALTH".equals(this.belowNameMode)
                        ? org.bukkit.scoreboard.Criteria.HEALTH
                        : org.bukkit.scoreboard.Criteria.DUMMY;
                objective = scoreboard.registerNewObjective("zessentials_bn", criteria,
                        this.belowNameDisplay, org.bukkit.scoreboard.RenderType.INTEGER);
            }
            objective.setDisplaySlot(org.bukkit.scoreboard.DisplaySlot.BELOW_NAME);
            return objective;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private void updateBelowNameScoresSafe() {
        this.plugin.getScheduler().runNextTick(wrappedTask -> {
            org.bukkit.scoreboard.Objective objective = belowNameObjective();
            if (objective == null) return;
            for (Player player : Bukkit.getOnlinePlayers()) {
                try {
                    String value = papi(this.belowNamePlaceholder, player).replaceAll("[^0-9.-]", "");
                    objective.getScore(player.getName()).setScore((int) Double.parseDouble(value));
                } catch (NumberFormatException | NullPointerException ignored) {
                }
            }
        });
    }

    /**
     * Cleans the tab name on quit so the next join starts fresh.
     */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (!this.isEnable) return;
        try {
            event.getPlayer().playerListName(Component.text(event.getPlayer().getName()));
        } catch (Throwable ignored) {
        }
    }

    /**
     * Applies the first matching group rule to the player.
     */
    public void apply(Player player) {

        if (!this.isEnable || !player.isOnline()) return;

        GroupRule rule = firstMatch(player);
        String fallbackTab = colorize(this.fallbackTabFormat.replace("%player%", player.getName()));

        Component tabName = rule == null
                ? dev.yanianz.essentials.util.ColorUtil.component(fallbackTab)
                : rule.tabFormat();

        // PlaceholderAPI support inside the tab name
        try {
            String legacy = toLegacy(tabName);
            String resolved = papi(legacy, player);
            tabName = dev.yanianz.essentials.util.ColorUtil.component(resolved);
        } catch (Exception ignored) {
        }

        player.playerListName(tabName);

        if (rule == null) return;

        this.plugin.getScheduler().runNextTick(wrappedTask -> {
            try {
                Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
                String teamName = teamName(rule);

                Team team = scoreboard.getTeam(teamName);
                if (team == null) {
                    team = scoreboard.registerNewTeam(teamName);
                    team.prefix(rule.prefix());
                    team.suffix(rule.suffix());
                }

                if (!team.hasEntry(player.getName())) {
                    team.addEntry(player.getName());
                }
            } catch (Throwable ignored) {
                // Scoreboard manager unavailable during early joins
            }
        });

        // Below name objective
        if (this.belowNameEnabled) {
            org.bukkit.scoreboard.Objective objective = belowNameObjective();
            if (objective != null && "PLACEHOLDER".equals(this.belowNameMode)) {
                String value = papi(this.belowNamePlaceholder, player).replaceAll("[^0-9.-]", "");
                try {
                    objective.getScore(player.getName()).setScore((int) Double.parseDouble(value));
                } catch (NumberFormatException ignored) {
                }
            }
        }

        // Spectator tab name override
        if (this.spectatorEnabled && player.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
            player.playerListName(this.spectatorTabName);
        }
    }

    /**
     * Updates below name scores for all online players when using PLACEHOLDER mode.
     */
    private void updateBelowNameScores() {

        if (!this.belowNameEnabled || !"PLACEHOLDER".equals(this.belowNameMode)) return;
        updateBelowNameScoresSafe();
    }

    private GroupRule firstMatch(Player player) {
        for (GroupRule rule : this.rules) {
            if (rule.permission().isEmpty() || player.hasPermission(rule.permission())) {
                return rule;
            }
        }
        return null;
    }

    /**
     * Zero padded priority keeps the tab list ordered by group.
     */
    private String teamName(GroupRule rule) {
        return String.format("%05d_nt%d", rule.priority(), rule.orderIndex());
    }

    private String toLegacy(Component component) {
        return net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                .legacySection().serialize(component);
    }

    private String colorize(String text) {
        return text == null ? "" : text.replace("&", "§");
    }
}
