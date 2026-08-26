package fr.maxlego08.essentials.module.modules.scoreboard.board;

import org.bukkit.Bukkit;

/**
 * Custom scoreboard scores are only supported by minecraft 1.20.3+ servers.
 */
public final class CustomScoresSupport {

    private static final boolean SUPPORTED = detect();

    private CustomScoresSupport() {
    }

    public static boolean isSupported() {
        return SUPPORTED;
    }

    private static boolean detect() {
        try {
            String version = Bukkit.getBukkitVersion().split("-")[0];
            String[] parts = version.split("\\.");
            int major = Integer.parseInt(parts[0]);
            if (major > 1) return true;
            int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            if (minor > 20) return true;
            if (minor < 20) return false;
            int patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
            return patch >= 3;
        } catch (Exception exception) {
            return false;
        }
    }
}
