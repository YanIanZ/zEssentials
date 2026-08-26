package fr.maxlego08.essentials.module.modules.effects;

import fr.maxlego08.essentials.ZEssentialsPlugin;
import fr.maxlego08.essentials.module.ZModule;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;

import java.util.Locale;

/**
 * Plays configurable particle and sound effects around players for
 * teleports and key actions like game mode changes, flight, heal and god.
 */
public class EffectsModule extends ZModule {

    private boolean teleportEnabled;
    private Particle teleportParticle;
    private Sound teleportSound;

    private boolean gamemodeEnabled;
    private Particle gamemodeParticle;
    private Sound gamemodeSound;

    private boolean flyEnabled;
    private Particle flyParticle;
    private Sound flySound;

    private boolean blessingEnabled;
    private Particle blessingParticle;
    private Sound blessingSound;

    public EffectsModule(ZEssentialsPlugin plugin) {
        super(plugin, "effects");
    }

    @Override
    public void loadConfiguration() {
        super.loadConfiguration();

        var configuration = getConfiguration();

        this.teleportEnabled = configuration.getBoolean("teleport.enabled", true);
        this.teleportParticle = particle(configuration.getString("teleport.particle", "PORTAL"));
        this.teleportSound = sound(configuration.getString("teleport.sound", "ENTITY_ENDERMAN_TELEPORT"));

        this.gamemodeEnabled = configuration.getBoolean("gamemode.enabled", true);
        this.gamemodeParticle = particle(configuration.getString("gamemode.particle", "HAPPY_VILLAGER"));
        this.gamemodeSound = sound(configuration.getString("gamemode.sound", "ENTITY_PLAYER_LEVELUP"));

        this.flyEnabled = configuration.getBoolean("fly.enabled", true);
        this.flyParticle = particle(configuration.getString("fly.particle", "CLOUD"));
        this.flySound = sound(configuration.getString("fly.sound", "ENTITY_BREEZE_IDLE_GROUND"));

        this.blessingEnabled = configuration.getBoolean("blessing.enabled", true);
        this.blessingParticle = particle(configuration.getString("blessing.particle", "TOTEM_OF_UNDYING"));
        this.blessingSound = sound(configuration.getString("blessing.sound", "ENTITY_PLAYER_LEVELUP"));
    }

    /**
     * Ring of particles played at both the departure and the destination of a teleport.
     */
    @EventHandler(ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {

        if (!this.isEnable || !this.teleportEnabled) return;
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL
                || event.getCause() == PlayerTeleportEvent.TeleportCause.CHORUS_FRUIT) return;

        Location from = event.getFrom().clone();
        spawnRing(from);

        Location to = event.getTo() == null ? null : event.getTo().clone();
        if (to == null || to.getWorld() == null) return;

        // The destination can be in another world, run on the region owning it
        this.plugin.getScheduler().runAtLocation(to, wrappedTask -> spawnRing(to));
    }

    @EventHandler(ignoreCancelled = true)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        if (!this.isEnable || !this.gamemodeEnabled) return;
        burst(event.getPlayer(), this.gamemodeParticle, this.gamemodeSound);
    }

    @EventHandler(ignoreCancelled = true)
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        if (!this.isEnable || !this.flyEnabled) return;
        burst(event.getPlayer(), this.flyParticle, this.flySound);
    }

    /**
     * Sparkles when a player is healed.
     */
    public void playHeal(Player player) {
        if (!this.isEnable || !this.blessingEnabled) return;
        burst(player, this.blessingParticle, this.blessingSound);
    }

    /**
     * Sparkles when god mode is toggled on.
     */
    public void playGod(Player player) {
        if (!this.isEnable || !this.blessingEnabled) return;
        burst(player, this.blessingParticle, this.blessingSound);
    }

    private void spawnRing(Location center) {

        World world = center.getWorld();
        if (world == null || this.teleportParticle == null) return;

        double radius = 1.1;
        for (int i = 0; i < 20; i++) {
            double angle = (Math.PI * 2 / 20) * i;
            world.spawnParticle(this.teleportParticle,
                    center.getX() + Math.cos(angle) * radius,
                    center.getY() + 0.1,
                    center.getZ() + Math.sin(angle) * radius,
                    1, 0, 0, 0, 0);
            world.spawnParticle(this.teleportParticle,
                    center.getX() + Math.cos(angle) * radius,
                    center.getY() + 1.4,
                    center.getZ() + Math.sin(angle) * radius,
                    1, 0, 0, 0, 0);
        }

        if (this.teleportSound != null) {
            world.playSound(center, this.teleportSound, 1.0f, 1.0f);
        }
    }

    private void burst(Player player, Particle particle, Sound sound) {

        Location location = player.getLocation().add(0, 1, 0);
        World world = location.getWorld();
        if (world != null && particle != null) {
            world.spawnParticle(particle, location, 25, 0.4, 0.5, 0.4, 0.05);
        }

        if (sound != null) {
            player.playSound(location, sound, 1.0f, 1.0f);
        }
    }

    private Particle particle(String name) {
        try {
            return Particle.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (Exception exception) {
            this.plugin.getLogger().warning("Unknown particle " + name + ", this effect will not play.");
            return null;
        }
    }

    private Sound sound(String name) {
        try {
            return Sound.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (Exception exception) {
            this.plugin.getLogger().warning("Unknown sound " + name + ", this effect will be silent.");
            return null;
        }
    }
}
