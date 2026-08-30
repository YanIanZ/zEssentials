package dev.yanianz.essentials.friends;

import dev.yanianz.essentials.network.NetworkManager;
import fr.maxlego08.essentials.ZEssentialsPlugin;
import fr.maxlego08.essentials.api.configuration.NonLoadable;
import fr.maxlego08.essentials.module.ZModule;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class FriendsModule extends ZModule {

    private boolean enabled = true;
    private int maxFriends = 50;
    private int expiryDays = 7;

    @NonLoadable
    private final FriendStorage storage = new FriendStorage();

    @NonLoadable
    private NetworkManager networkManager;

    public FriendsModule(ZEssentialsPlugin plugin) {
        super(plugin, "friends");
    }

    @Override
    public void loadConfiguration() {
        super.loadConfiguration();
        YamlConfiguration config = getConfiguration();
        this.enabled = config.getBoolean("enable", true);
        this.maxFriends = Math.max(1, config.getInt("max-friends", 50));
        this.expiryDays = Math.max(1, config.getInt("request-expiry-days", 7));
    }

    public boolean sendRequest(UUID from, UUID to) {
        if (from.equals(to)) return false;
        if (storage.isFriend(from, to)) return false;
        if (storage.getFriendCount(from) >= getMaxFor(from)) return false;
        return storage.sendRequest(from, to);
    }

    public boolean acceptRequest(UUID from, UUID to) {
        return storage.acceptRequest(from, to);
    }

    public boolean declineRequest(UUID from, UUID to) {
        return storage.declineRequest(from, to);
    }

    public boolean removeFriend(UUID player, UUID friend) {
        return storage.removeFriend(player, friend);
    }

    public boolean isFriend(UUID player, UUID other) {
        return storage.isFriend(player, other);
    }

    public boolean hasPendingRequest(UUID from, UUID to) {
        return storage.hasPendingRequest(from, to);
    }

    public List<UUID> getPendingRequests(UUID to) {
        return storage.getPendingRequests(to);
    }

    public List<UUID> getFriends(UUID player) {
        return storage.getFriends(player);
    }

    public int getFriendCount(UUID player) {
        return storage.getFriendCount(player);
    }

    public int getMaxFor(UUID player) {
        return this.maxFriends;
    }

    public boolean isEnabled() { return enabled && isEnable; }

    public NetworkManager getNetworkManager() { return networkManager; }

    public void setNetworkManager(NetworkManager networkManager) { this.networkManager = networkManager; }
}