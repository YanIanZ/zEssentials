package dev.yanianz.essentials.network;

import fr.maxlego08.essentials.ZEssentialsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.Messenger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class NetworkManager {

    private static final String BUNGEECORD_CHANNEL = "BungeeCord";
    public static final String ZESSENTIALS_PREFIX = "zessentials:";

    private final ZEssentialsPlugin plugin;
    private final Map<String, Consumer<String>> listeners = new HashMap<>();
    private String localServerName = "server";

    public NetworkManager(ZEssentialsPlugin plugin) {
        this.plugin = plugin;
    }

    public void setLocalServerName(String name) {
        this.localServerName = name;
    }

    public String getLocalServerName() {
        return localServerName;
    }

    public void sendToServer(String subChannel, String data) {
        String fullChannel = ZESSENTIALS_PREFIX + subChannel;
        Messenger messenger = Bukkit.getMessenger();
        if (!messenger.isOutgoingChannelRegistered(plugin, BUNGEECORD_CHANNEL)) return;
        if (Bukkit.getOnlinePlayers().isEmpty()) return;
        Player carrier = Bukkit.getOnlinePlayers().iterator().next();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);
        try {
            out.writeUTF(fullChannel);
            out.writeUTF(data);
            carrier.sendPluginMessage(plugin, BUNGEECORD_CHANNEL, baos.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void registerListener(String subChannel, Consumer<String> handler) {
        String fullChannel = ZESSENTIALS_PREFIX + subChannel;
        listeners.put(fullChannel, handler);
        Messenger messenger = Bukkit.getMessenger();
        messenger.registerIncomingPluginChannel(plugin, BUNGEECORD_CHANNEL,
                (channel, player, bytes) -> {
                    try {
                        DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
                        String sub = in.readUTF();
                        String data = in.readUTF();
                        Consumer<String> listener = listeners.get(sub);
                        if (listener != null) listener.accept(data);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
    }

    public boolean isAvailable() {
        return Bukkit.getMessenger().isOutgoingChannelRegistered(plugin, BUNGEECORD_CHANNEL);
    }
}