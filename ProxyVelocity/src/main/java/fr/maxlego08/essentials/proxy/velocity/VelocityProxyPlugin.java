package fr.maxlego08.essentials.proxy.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;

import java.util.Optional;

/**
 * Velocity-native relay for zEssentials. Backend servers send messages on
 * the "zessentials:relay" plugin channel; this plugin forwards them to
 * every other server behind the proxy so features like the global chat
 * work across the network.
 */
public class VelocityProxyPlugin {

    public static final MinecraftChannelIdentifier RELAY_CHANNEL =
            MinecraftChannelIdentifier.create("zessentials", "relay");

    private final ProxyServer proxyServer;
    private final ComponentLogger logger;

    @Inject
    public VelocityProxyPlugin(ProxyServer proxyServer, ComponentLogger logger) {
        this.proxyServer = proxyServer;
        this.logger = logger;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        this.proxyServer.getChannelRegistrar().register(RELAY_CHANNEL);
        this.logger.info("zEssentials relay enabled on channel {}", RELAY_CHANNEL.getId());
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        this.logger.info("zEssentials relay disabled");
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getIdentifier().equals(RELAY_CHANNEL)) return;

        event.setResult(PluginMessageEvent.ForwardResult.handled());

        if (!(event.getSource() instanceof ServerConnection sourceConnection)) return;
        String originServerName = sourceConnection.getServerInfo() == null
                ? "" : sourceConnection.getServerInfo().getName();

        byte[] message = event.getData();

        this.proxyServer.getAllServers().forEach(registeredServer -> {
            String serverName = registeredServer.getServerInfo().getName();
            if (serverName.equals(originServerName)) return;
            registeredServer.sendPluginMessage(RELAY_CHANNEL, message);
        });

        this.logger.debug("Relayed {} bytes from {}", message.length, originServerName);
    }
}
