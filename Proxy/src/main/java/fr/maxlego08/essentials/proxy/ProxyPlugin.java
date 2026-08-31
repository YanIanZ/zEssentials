package fr.maxlego08.essentials.proxy;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

/**
 * BungeeCord/Velocity(legacy) side relay for zEssentials. Backend servers
 * send messages on the "zessentials:relay" plugin channel; this plugin
 * forwards them to every other server behind the proxy so features like
 * the global chat work across the network.
 */
public class ProxyPlugin extends Plugin implements Listener {

    public static final String RELAY_CHANNEL = "zessentials:relay";

    @Override
    public void onEnable() {
        getProxy().registerChannel(RELAY_CHANNEL);
        getProxy().getPluginManager().registerListener(this, this);
        getLogger().info("zEssentials relay enabled on channel " + RELAY_CHANNEL);
    }

    @Override
    public void onDisable() {
        getProxy().unregisterChannel(RELAY_CHANNEL);
    }

    @EventHandler
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getTag().equals(RELAY_CHANNEL)) return;

        event.setCancelled(true);

        if (!(event.getReceiver() instanceof ProxiedPlayer receiver)) return;
        Server origin = receiver.getServer();
        if (origin == null) return;
        String originServerName = origin.getInfo() == null ? "" : origin.getInfo().getName();

        byte[] message = event.getData();

        for (ProxiedPlayer target : ProxyServer.getInstance().getPlayers()) {
            Server targetServer = target.getServer();
            if (targetServer == null) continue;
            String targetName = targetServer.getInfo() == null ? "" : targetServer.getInfo().getName();
            if (targetName.equals(originServerName)) continue;
            targetServer.sendData(RELAY_CHANNEL, message);
        }

        getLogger().fine("Relayed " + message.length + " bytes from " + originServerName);
    }
}
