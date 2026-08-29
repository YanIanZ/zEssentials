package fr.maxlego08.essentials.user.placeholders;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.placeholders.Placeholder;
import fr.maxlego08.essentials.api.placeholders.PlaceholderRegister;
import fr.maxlego08.essentials.zutils.utils.ZUtils;
import org.bukkit.Statistic;

public class StatisticPlaceholders extends ZUtils implements PlaceholderRegister {

    @Override
    public void register(Placeholder placeholder, EssentialsPlugin plugin) {

        for (Statistic statistic : Statistic.values()) {
            String name = statistic.name().toLowerCase();
            placeholder.register("statistic_" + name, player -> {
                try {
                    return String.valueOf(player.getStatistic(statistic));
                } catch (Exception e) {
                    return "0";
                }
            }, "Returns the player statistic " + name);

            placeholder.register("statistic_" + name + "_formatted", player -> {
                try {
                    return formatNumber(player.getStatistic(statistic));
                } catch (Exception e) {
                    return "0";
                }
            }, "Returns the formatted player statistic " + name);
        }

        placeholder.register("statistic_playtime_ticks", player -> String.valueOf(player.getStatistic(Statistic.PLAY_ONE_MINUTE)), "Returns playtime in ticks");
        placeholder.register("statistic_playtime_minutes", player -> String.valueOf(player.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20 / 60), "Returns playtime in minutes");
        placeholder.register("statistic_playtime_hours", player -> String.format("%.1f", player.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20.0 / 3600.0), "Returns playtime in hours");

        placeholder.register("statistic_jumps", player -> String.valueOf(player.getStatistic(Statistic.JUMP)), "Returns total jumps");
        placeholder.register("statistic_deaths", player -> String.valueOf(player.getStatistic(Statistic.DEATHS)), "Returns total deaths");
        placeholder.register("statistic_mob_kills", player -> String.valueOf(player.getStatistic(Statistic.MOB_KILLS)), "Returns total mob kills");
        placeholder.register("statistic_player_kills", player -> String.valueOf(player.getStatistic(Statistic.PLAYER_KILLS)), "Returns total player kills");
        placeholder.register("statistic_damage_dealt", player -> String.valueOf(player.getStatistic(Statistic.DAMAGE_DEALT)), "Returns total damage dealt");
        placeholder.register("statistic_damage_taken", player -> String.valueOf(player.getStatistic(Statistic.DAMAGE_TAKEN)), "Returns total damage taken");
        placeholder.register("statistic_fish_caught", player -> String.valueOf(player.getStatistic(Statistic.FISH_CAUGHT)), "Returns total fish caught");
        placeholder.register("statistic_times_slept", player -> String.valueOf(player.getStatistic(Statistic.SLEEP_IN_BED)), "Returns times slept in a bed");
        placeholder.register("statistic_aviate_cm", player -> String.valueOf(player.getStatistic(Statistic.AVIATE_ONE_CM)), "Returns distance flown with elytra in cm");
        placeholder.register("statistic_walk_cm", player -> String.valueOf(player.getStatistic(Statistic.WALK_ONE_CM)), "Returns distance walked in cm");
        placeholder.register("statistic_sprint_cm", player -> String.valueOf(player.getStatistic(Statistic.SPRINT_ONE_CM)), "Returns distance sprinted in cm");
        placeholder.register("statistic_swim_cm", player -> String.valueOf(player.getStatistic(Statistic.SWIM_ONE_CM)), "Returns distance swum in cm");
    }

    private String formatNumber(int value) {
        if (value >= 1_000_000_000) return String.format("%.2fB", value / 1_000_000_000.0);
        if (value >= 1_000_000) return String.format("%.2fM", value / 1_000_000.0);
        if (value >= 1_000) return String.format("%.1fK", value / 1_000.0);
        return String.valueOf(value);
    }
}
