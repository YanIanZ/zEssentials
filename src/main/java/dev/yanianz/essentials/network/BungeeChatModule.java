package dev.yanianz.essentials.network;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import fr.maxlego08.essentials.ZEssentialsPlugin;
import fr.maxlego08.essentials.api.configuration.NonLoadable;
import fr.maxlego08.essentials.module.ZModule;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.plugin.messaging.Messenger;


/**
 * Relays public chat messages between the servers of a network through
 * the BungeeCord plugin messaging channel, so every server sees the chat.
 * Both ends must run this module with it enabled.
 */
public class BungeeChatModule extends ZModule {

    @fr.maxlego08.essentials.api.configuration.NonLoadable
    private static final String CHANNEL = "BungeeCord";
    @fr.maxlego08.essentials.api.configuration.NonLoadable
    private static final String SUB_CHANNEL = "zessentials:chat";

    private String serverName;
    private String format;

    @NonLoadable
    private volatile boolean registered;

    public BungeeChatModule(ZEssentialsPlugin plugin) {
        super(plugin, "bungeechat");
    }

    @Override
    public void loadConfiguration() {
        super.loadConfiguration();

        var config = getConfiguration();
        String configuredName = config.getString("server-name", "");
        this.serverName = (configuredName == null || configuredName.isEmpty())
                ? plugin.getConfig().getString("server-name", "server")
                : configuredName;
        this.format = config.getString("format", "&#00d4ff[%server%] &f%player% &8» &7%message%");

        registerChannels();
    }

    /**
     * Registers or unregisters the incoming channel depending on the module state.
     * The outgoing channel stays registered for the whole lifetime, sending to an
     * unregistered incoming channel on the other servers is harmless.
     */
    private void registerChannels() {

        Messenger messenger = Bukkit.getMessenger();

        if (!registered) {
            messenger.registerOutgoingPluginChannel(this.plugin, CHANNEL);
            messenger.registerIncomingPluginChannel(this.plugin, CHANNEL,
                    (channel, player, bytes) -> {

                        if (!this.isEnable) return;
                        if (!channel.equals(CHANNEL)) return;

                        ByteArrayDataInput input = ByteStreams.newDataInput(bytes);
                        if (!input.readUTF().equals(SUB_CHANNEL)) return;

                        String originServer = input.readUTF();
                        String playerName = input.readUTF();
                        String content = input.readUTF();

                        String line = this.format
                                .replace("%server%", originServer)
                                .replace("%player%", playerName)
                                .replace("%message%", colorize(content));

                        Component component = LegacyComponentSerializer.legacySection().deserialize(line);
                        Bukkit.getOnlinePlayers().forEach(online -> online.sendMessage(component));
                    });
            this.registered = true;
        }
    }

    /**
     * Broadcasts the public chat message of a player to every other server.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTalk(AsyncChatEvent event) {

        if (!this.isEnable) return;

        Player player = event.getPlayer();
        String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                .plainText().serialize(event.originalMessage()).trim();

        // The write needs the main thread on some platforms
        this.plugin.getScheduler().runAtLocation(player.getLocation(), wrappedTask -> send(player, plain));
    }

    private void send(Player player, String content) {

        Messenger messenger = Bukkit.getMessenger();
        if (!messenger.isOutgoingChannelRegistered(this.plugin, CHANNEL)) return;
        if (Bukkit.getOnlinePlayers().isEmpty()) return; // nobody to carry the message

        ByteArrayDataOutput output = ByteStreams.newDataOutput();
        output.writeUTF(SUB_CHANNEL);
        output.writeUTF(this.serverName);
        output.writeUTF(player.getName());
        output.writeUTF(content);

        byte[] data = toByteArray(output);
        player.sendPluginMessage(this.plugin, CHANNEL, data);
    }

    private static byte[] toByteArray(ByteArrayDataOutput output) {
        return output.toByteArray();
    }

    private String colorize(String text) {
        return text == null ? "" : text.replace("&", "§");
    }
}
