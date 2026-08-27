package dev.yanianz.essentials.discordbridge;

import fr.maxlego08.essentials.ZEssentialsPlugin;
import fr.maxlego08.essentials.api.configuration.NonLoadable;
import fr.maxlego08.essentials.module.ZModule;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;

import java.lang.reflect.Method;

/**
 * Forwards every public chat message to the main Discord text channel
 * through the DiscordSRV plugin when that one is installed.
 *
 * Everything runs through reflection so the module keeps working with any
 * DiscordSRV version and without a compile time dependency. The reverse
 * direction, Discord messages shown in game, stays handled by DiscordSRV
 * itself through its own format configuration.
 */
public class DiscordBridgeModule extends ZModule {

    private String format;
    private int minLength;

    @NonLoadable
    private boolean discordSrvPresent;

    public DiscordBridgeModule(ZEssentialsPlugin plugin) {
        super(plugin, "discordbridge");
    }

    @Override
    public void loadConfiguration() {
        super.loadConfiguration();

        var config = getConfiguration();
        this.format = config.getString("format", "**%player%**: %message%");
        this.minLength = Math.max(1, config.getInt("min-length", 1));

        this.discordSrvPresent = detectDiscordSrv();
        if (this.isEnable && !this.discordSrvPresent) {
            this.plugin.getLogger().info("DiscordSRV was not found, the chat bridge stays idle.");
        }
    }

    private boolean detectDiscordSrv() {
        try {
            Class.forName("github.scarsz.discordsrv.DiscordSRV");
            return Bukkit.getPluginManager().getPlugin("DiscordSRV") != null;
        } catch (ClassNotFoundException exception) {
            return false;
        }
    }

    /**
     * Forwarding reminder on staff joins when DiscordSRV is missing but enabled.
     */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("essentials.discordbridge.notify")) return;
        if (!this.isEnable || this.discordSrvPresent) return;
        player.sendMessage(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                .legacySection().deserialize(colorize("&7[Discord bridge] &cDiscordSRV is not installed, forwarding disabled.")));
    }

    /**
     * Sends every non cancelled public message to the main discord channel.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {

        if (!this.isEnable || !this.discordSrvPresent) return;

        Player player = event.getPlayer();
        String plain = PlainTextComponentSerializer.plainText().serialize(event.originalMessage()).trim();
        if (plain.length() < this.minLength) return;

        String payload = this.format
                .replace("%server%", this.plugin.getServer().getName())
                .replace("%player%", player.getName())
                .replace("%message%", plain);

        // Everything on the main thread, matching how DiscordSRV expects its api usage
        this.plugin.getScheduler().runNextTick(wrappedTask -> {
            try {
                Class<?> mainClass = Class.forName("github.scarsz.discordsrv.DiscordSRV");
                Object pluginInstance = mainClass.getMethod("getPlugin").invoke(null);
                Object channel = mainClass.getMethod("getMainTextChannel").invoke(pluginInstance);

                Class<?> utilClass = Class.forName("github.scarsz.discordsrv.util.DiscordUtil");
                Method sendMessage = utilClass.getMethod("sendMessage",
                        channel.getClass(), String.class);
                sendMessage.invoke(null, channel, payload);
            } catch (Throwable throwable) {
                this.plugin.getLogger().warning("Discord forward failed: " + throwable.getMessage());
            }
        });
    }


    private String colorize(String text) {
        return text == null ? "" : text.replace("&", "§");
    }
}
