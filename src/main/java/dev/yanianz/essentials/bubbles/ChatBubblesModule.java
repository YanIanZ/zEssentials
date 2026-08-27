package dev.yanianz.essentials.bubbles;

import fr.maxlego08.essentials.ZEssentialsPlugin;
import org.bukkit.Color;
import dev.yanianz.essentials.util.ColorUtil;
import fr.maxlego08.essentials.api.configuration.NonLoadable;
import fr.maxlego08.essentials.module.ZModule;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
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
import java.util.regex.Pattern;

/**
 * Shows the chat message of a player as a text display floating above the
 * head, the bubble follows the player, messages stack on top of each other
 * and disappear after a configurable duration.
 */
public class ChatBubblesModule extends ZModule {

    private long durationMillis = 6000;
    private double yOffset = 2.0;
    private double stackOffset = 0.35;
    private String backgroundHex = "#B0000000";
    private float scale = 0.9f;

    @NonLoadable
    private final Map<UUID, Deque<ActiveBubble>> activeBubbles = new ConcurrentHashMap<>();

    public ChatBubblesModule(ZEssentialsPlugin plugin) {
        super(plugin, "bubbles");
    }

    /** One floating bubble with its follow task. */
    private static final class ActiveBubble {
        final TextDisplay display;
        final com.tcoded.folialib.wrapper.task.WrappedTask followTask;

        ActiveBubble(TextDisplay display, com.tcoded.folialib.wrapper.task.WrappedTask followTask) {
            this.display = display;
            this.followTask = followTask;
        }
    }

    @Override
    public void loadConfiguration() {
        super.loadConfiguration();

        var config = getConfiguration();
        this.durationMillis = parseDuration(config.getString("duration", "6s"), 6000);
        this.yOffset = config.getDouble("y-offset", 2.0);
        this.stackOffset = config.getDouble("stack-offset", 0.35);
        this.backgroundHex = config.getString("background", "#B0000000");
        this.scale = (float) config.getDouble("scale", 0.9);

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

        if (plain.isEmpty()) return;

        // The event is async, spawning belongs to the region owning the player
        this.plugin.getScheduler().runAtLocation(player.getLocation(),
                wrappedTask -> spawnBubble(player, plain));
    }

    private void spawnBubble(Player player, String plainContent) {

        Deque<ActiveBubble> bubbles = this.activeBubbles
                .computeIfAbsent(player.getUniqueId(), k -> new ArrayDeque<>());

        // Purge dead displays so stacking stays correct
        bubbles.removeIf(bubble -> !bubble.display.isValid());

        // Replaced display keywords have no meaning inside a bubble
        String content = plainContent.replaceAll("(?i)\\[(item|i|inv|inventory|ender|ec|pos|position)]", " ");
        content = applyEmojis(content).trim();
        if (content.isEmpty()) return;
        final String finalContent = content;

        try {
            var chatModule = this.plugin.getModuleManager().getModule(fr.maxlego08.essentials.module.modules.chat.ChatModule.class);
            if (chatModule != null) {
                for (Map.Entry<String, String> entry : chatModule.getEmojiShortcuts().entrySet()) {
                    content = content.replaceAll(entry.getKey(), java.util.regex.Matcher.quoteReplacement(entry.getValue()));
                }
            }
        } catch (Exception ignored) {
        }
        if (content.isEmpty()) return;

        float height = (float) (this.yOffset + bubbles.size() * this.stackOffset);
        Location startLocation = headLocation(player);

        TextDisplay display = player.getWorld().spawn(startLocation, TextDisplay.class, textDisplay -> {
            textDisplay.text(ColorUtil.component(finalContent));
            textDisplay.setBillboard(Billboard.CENTER);
            textDisplay.setBackgroundColor(parseColor(this.backgroundHex));
            textDisplay.setSeeThrough(false);
            textDisplay.setShadowed(true);
            textDisplay.setAlignment(TextDisplay.TextAlignment.CENTER);
            textDisplay.setPersistent(false);
            textDisplay.setViewRange(1.0f);

            textDisplay.setTransformation(new org.bukkit.util.Transformation(
                    new Vector3f(-(float)(finalContent.length() * 0.15f / 2), 0, 0),
                    new Quaternionf(new AxisAngle4f()),
                    new Vector3f(this.scale, this.scale, this.scale),
                    new Quaternionf(new AxisAngle4f())));
        });

        // Follow task: glue the bubble to the player head until it expires
        final float finalHeight = height;
        var followTask = this.plugin.getScheduler().runAtLocationTimer(player.getLocation(), new Runnable() {
            @Override
            public void run() {
                if (!display.isValid() || !player.isOnline()) return;
                Location target = headLocation(player).add(0, finalHeight, 0);
                display.teleport(target);
            }
        }, 2L, 2L);

        bubbles.addLast(new ActiveBubble(display, followTask));

        this.plugin.getScheduler().runLater(() -> removeBubble(player.getUniqueId(), bubbles, bubbles.peekLast()), this.durationMillis * 20L);
    }

    private void removeBubble(UUID uniqueId, Deque<ActiveBubble> bubbles, ActiveBubble bubble) {
        if (bubble == null) return;
        bubbles.remove(bubble);
        bubble.followTask.cancel();
        removeEntity(bubble.display);
    }

    private Location headLocation(Player player) {
        Location location = player.getLocation().clone();
        location.setY(location.getY() + 1.8);
        return location;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {

        Deque<ActiveBubble> bubbles = this.activeBubbles.remove(event.getPlayer().getUniqueId());
        if (bubbles == null) return;

        for (ActiveBubble bubble : bubbles) {
            bubble.followTask.cancel();
            removeEntity(bubble.display);
        }
    }

    private void removeEntity(Entity entity) {
        if (!entity.isValid()) return;
        this.plugin.getScheduler().runAtLocation(entity.getLocation(), wrappedTask -> {
            if (entity.isValid()) entity.remove();
        });
    }

    private void clearAll() {
        this.activeBubbles.values().forEach(bubbles -> bubbles.forEach(bubble -> {
            bubble.followTask.cancel();
            removeEntity(bubble.display);
        }));
        this.activeBubbles.clear();
    }

    private String applyEmojis(String input) {
        String current = input;
        try {
            var chatModule = this.plugin.getModuleManager().getModule(fr.maxlego08.essentials.module.modules.chat.ChatModule.class);
            if (chatModule != null) {
                for (Map.Entry<String, String> entry : chatModule.getEmojiShortcuts().entrySet()) {
                    current = current.replaceAll(entry.getKey(), java.util.regex.Matcher.quoteReplacement(entry.getValue()));
                }
            }
        } catch (Exception ignored) {
        }
        return current.trim();
    }

    /**
     * Accepts either a plain number of seconds or a duration string like 30s, 5m or 10d.
     */
    private long parseDuration(String raw, long defaultSeconds) {
        try {
            return Long.parseLong(raw.trim());
        } catch (Exception ignored) {
        }
        try {
            return stringToDuration(raw).getSeconds();
        } catch (Exception exception) {
            return defaultSeconds;
        }
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
}
