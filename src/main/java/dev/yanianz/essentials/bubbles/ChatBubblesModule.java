package dev.yanianz.essentials.bubbles;

import fr.maxlego08.essentials.ZEssentialsPlugin;
import fr.maxlego08.essentials.api.configuration.NonLoadable;
import net.kyori.adventure.text.Component;
import fr.maxlego08.essentials.module.ZModule;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerQuitEvent;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shows the chat message of a player as a text display floating above the
 * head, messages stack on top of each other and fade out automatically.
 */
public class ChatBubblesModule extends ZModule {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private int durationSeconds;
    private double yOffset;
    private double stackOffset;
    private String backgroundHex;
    private float scale;
    private float viewDistance;

    @NonLoadable
    private final Map<UUID, Deque<TextDisplay>> activeBubbles = new ConcurrentHashMap<>();

    public ChatBubblesModule(ZEssentialsPlugin plugin) {
        super(plugin, "bubbles");
    }

    @Override
    public void loadConfiguration() {
        super.loadConfiguration();

        var config = getConfiguration();
        this.durationSeconds = Math.max(1, config.getInt("duration-seconds", 6));
        this.yOffset = config.getDouble("y-offset", 2.0);
        this.stackOffset = config.getDouble("stack-offset", 0.35);
        this.backgroundHex = config.getString("background", "#B0000000");
        this.scale = (float) config.getDouble("scale", 0.9);
        this.viewDistance = (float) config.getInt("view-distance", 64) / 16f;

        clearAll();
    }

    /**
     * Spawns the bubble when the player talks, after every other chat feature ran.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {

        if (!this.isEnable) return;

        Player player = event.getPlayer();
        String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                .plainText().serialize(event.originalMessage()).trim();

        // Show the message with its own colors inside the bubble
        Component component = LEGACY.deserialize(colorize(plain));

        // The event is async, spawning belongs to the region owning the player
        this.plugin.getScheduler().runAtLocation(player.getLocation(),
                wrappedTask -> spawnBubble(player, component));
    }

    private void spawnBubble(Player player, Component content) {

        Deque<TextDisplay> bubbles = this.activeBubbles
                .computeIfAbsent(player.getUniqueId(), k -> new ArrayDeque<>());

        // Purge dead displays so stacking stays correct
        bubbles.removeIf(display -> !display.isValid());

        Location spawnLocation = player.getLocation().add(
                0, this.yOffset + bubbles.size() * this.stackOffset, 0);

        TextDisplay display = player.getWorld().spawn(spawnLocation, TextDisplay.class, textDisplay -> {
            textDisplay.text(content);
            textDisplay.setBillboard(Billboard.CENTER);
            textDisplay.setBackgroundColor(parseColor(this.backgroundHex));
            textDisplay.setSeeThrough(false);
            textDisplay.setShadowed(true);
            textDisplay.setAlignment(TextDisplay.TextAlignment.CENTER);
            textDisplay.setPersistent(false);
            textDisplay.setViewRange(this.viewRange());

            float finalScale = this.scale;
            textDisplay.setTransformation(new org.bukkit.util.Transformation(
                    new Vector3f(0, 0, 0),
                    new Quaternionf(new AxisAngle4f()),
                    new Vector3f(finalScale, finalScale, finalScale),
                    new Quaternionf(new AxisAngle4f())));
            textDisplay.setInterpolationDelay(0);
            textDisplay.setInterpolationDuration(6);
        });

        bubbles.addLast(display);

        this.plugin.getScheduler().runLater(() -> {
            Deque<TextDisplay> stillActive = this.activeBubbles.get(player.getUniqueId());
            if (stillActive != null) stillActive.remove(display);
            removeEntity(display);
        }, this.durationSeconds * 20L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {

        Deque<TextDisplay> bubbles = this.activeBubbles.remove(event.getPlayer().getUniqueId());
        if (bubbles == null) return;

        for (TextDisplay display : bubbles) {
            removeEntity(display);
        }
    }

    private void removeEntity(Entity entity) {
        this.plugin.getScheduler().runAtLocation(entity.getLocation(), wrappedTask -> {
            if (entity.isValid()) entity.remove();
        });
    }

    private void clearAll() {
        this.activeBubbles.values().forEach(bubbles -> bubbles.forEach(this::removeEntity));
        this.activeBubbles.clear();
    }

    private Color parseColor(String hex) {
        try {
            String cleaned = hex.replace("#", "");
            long argb = Long.parseLong(cleaned, 16);
            int alpha = (int) ((argb >> 24) & 0xFF);
            int red = (int) ((argb >> 16) & 0xFF);
            int green = (int) ((argb >> 8) & 0xFF);
            int blue = (int) (argb & 0xFF);
            return Color.fromARGB(alpha, red, green, blue);
        } catch (Exception exception) {
            return Color.fromARGB(176, 0, 0, 0);
        }
    }

    private float viewRange() {
        return this.viewDistance;
    }

    private String colorize(String text) {
        return text == null ? "" : text.replace("&", "§");
    }
}
