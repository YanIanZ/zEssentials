package dev.yanianz.essentials.friends;

import dev.yanianz.essentials.network.NetworkManager;
import fr.maxlego08.essentials.ZEssentialsPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class FriendListener implements Listener {

    private final ZEssentialsPlugin plugin;
    private final FriendsModule friendsModule;
    private final NetworkManager networkManager;
    private static final AtomicBoolean registered = new AtomicBoolean(false);

    public FriendListener(ZEssentialsPlugin plugin, FriendsModule friendsModule, NetworkManager networkManager) {
        this.plugin = plugin;
        this.friendsModule = friendsModule;
        this.networkManager = networkManager;
    }

    public static void ensureRegistered(ZEssentialsPlugin plugin) {
        if (!registered.compareAndSet(false, true)) return;
        FriendsModule module = plugin.getModuleManager().getModule(FriendsModule.class);
        if (module == null) return;
        NetworkManager netMgr = new NetworkManager(plugin);
        Bukkit.getPluginManager().registerEvents(
                new FriendListener(plugin, module, netMgr), plugin);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        UUID playerUuid = event.getPlayer().getUniqueId();
        for (UUID friendUuid : friendsModule.getFriends(playerUuid)) {
            Player friend = Bukkit.getPlayer(friendUuid);
            if (friend != null) {
                friend.sendMessage(Component.text("Your friend "
                        + event.getPlayer().getName() + " is now online."));
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
    }
}