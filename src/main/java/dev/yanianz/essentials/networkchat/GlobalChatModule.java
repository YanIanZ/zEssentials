package dev.yanianz.essentials.networkchat;

import dev.yanianz.essentials.network.NetworkManager;
import fr.maxlego08.essentials.ZEssentialsPlugin;
import fr.maxlego08.essentials.module.ZModule;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

public class GlobalChatModule extends ZModule {

    private String serverName = "server";
    private String format = "&7[&b%server%&7] &f%player%&8: &7%message%";
    private NetworkManager networkManager;
    private static boolean listenerRegistered = false;

    public GlobalChatModule(ZEssentialsPlugin plugin) {
        super(plugin, "network-chat");
    }

    @Override
    public void loadConfiguration() {
        super.loadConfiguration();
        YamlConfiguration config = getConfiguration();
        String configuredName = config.getString("server-name", "");
        this.serverName = (configuredName == null || configuredName.isEmpty())
                ? plugin.getConfig().getString("server-name", "server")
                : configuredName;
        this.format = config.getString("format", "&7[&b%server%&7] &f%player%&8: &7%message%");

        if (this.networkManager == null) {
            this.networkManager = new NetworkManager(this.plugin);
        }
        this.networkManager.setLocalServerName(this.serverName);

        if (!listenerRegistered) {
            this.networkManager.registerListener("chat", data -> {
                String[] parts = data.split("\\|", 3);
                if (parts.length < 3) return;
                String originServer = parts[0];
                String playerName = parts[1];
                String content = parts[2];
                String line = dev.yanianz.essentials.util.ColorUtil.sections(
                        this.format
                                .replace("%server%", originServer)
                                .replace("%player%", playerName)
                                .replace("%message%", content));
                Bukkit.getOnlinePlayers().forEach(p ->
                        p.sendMessage(LegacyComponentSerializer.legacySection().deserialize(line)));
            });
            listenerRegistered = true;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTalk(AsyncChatEvent event) {
        if (!this.isEnable) return;
        Player player = event.getPlayer();
        String plain = PlainTextComponentSerializer.plainText()
                .serialize(event.originalMessage()).trim();
        this.plugin.getScheduler().runAtLocation(player.getLocation(),
                wrappedTask -> networkManager.sendToServer("chat",
                        serverName + "|" + player.getName() + "|" + plain));
    }
}