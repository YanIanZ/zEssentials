package fr.maxlego08.essentials.storage;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.event.events.user.UserJoinEvent;
import fr.maxlego08.essentials.api.event.events.user.UserQuitEvent;
import fr.maxlego08.essentials.api.messages.Message;
import fr.maxlego08.essentials.api.sanction.Sanction;
import fr.maxlego08.essentials.api.storage.IStorage;
import fr.maxlego08.essentials.api.storage.StorageManager;
import fr.maxlego08.essentials.api.storage.StorageType;
import fr.maxlego08.essentials.module.modules.SpawnModule;
import fr.maxlego08.essentials.storage.mongodb.MongoStorage;
import fr.maxlego08.essentials.storage.storages.JsonStorage;
import fr.maxlego08.essentials.storage.storages.SqlStorage;
import fr.maxlego08.essentials.zutils.utils.TimerBuilder;
import fr.maxlego08.essentials.zutils.utils.ZUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.time.Duration;
import org.bukkit.entity.Player;
import fr.maxlego08.essentials.zutils.utils.paper.PaperComponent;
import java.util.UUID;

public class ZStorageManager extends ZUtils implements StorageManager {

    private final IStorage iStorage;
    private final EssentialsPlugin plugin;
    private final StorageType storageType;

    public ZStorageManager(EssentialsPlugin plugin) {
        this.plugin = plugin;
        this.storageType = plugin.getConfiguration().getStorageType();
        this.iStorage = switch (this.storageType) {
            case HIKARICP, SQLITE, MYSQL, MARIADB -> new SqlStorage(plugin, this.storageType);
            case MONGO -> new MongoStorage(plugin);
            default -> new JsonStorage(plugin);
        };
    }

    @Override
    public void onEnable() {
        this.iStorage.onEnable();

        Bukkit.getOnlinePlayers().forEach(player -> this.iStorage.createOrLoad(player.getUniqueId(), player.getName()));
    }

    @Override
    public void onDisable() {
        this.iStorage.onDisable();
    }

    @Override
    public IStorage getStorage() {
        return this.iStorage;
    }

    @Override
    public StorageType getType() {
        return this.storageType;
    }

    /**
     * Runs the login checks on the authentication thread instead of the
     * {@link org.bukkit.event.player.PlayerLoginEvent}, listening to that
     * event makes Paper disable its re-configuration api which breaks the
     * profile public key of secure chat.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {

        if (this.plugin.getConfiguration().getBlacklistUuids().contains(event.getUniqueId())) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                    Component.text("Unable to verify your connection, please try again."));
            this.plugin.getLogger().info("A blacklist player try to connect: " + event.getName() + " (" + event.getUniqueId() + ")");
            return;
        }

        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) return;

        UUID playerUuid = event.getUniqueId();
        String playerName = event.getName();

        if (this.iStorage.isBan(playerUuid)) {
            Sanction sanction = this.iStorage.getBan(playerUuid);
            Duration duration = sanction == null ? Duration.ZERO : sanction.getDurationRemaining();
            PaperComponent paperComponent = new PaperComponent();
            Component kickMessage = paperComponent.getComponentMessage(Message.MESSAGE_BAN_JOIN,
                    "%reason%", sanction == null ? "" : sanction.getReason(),
                    "%remaining%", TimerBuilder.getStringTime(duration.toMillis()));
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, kickMessage);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onLogin(org.bukkit.event.player.PlayerJoinEvent event) {

        Player player = event.getPlayer();
        UUID playerUuid = player.getUniqueId();

        var user = this.iStorage.getUser(playerUuid);
        if (user == null) {
            user = this.iStorage.createOrLoad(playerUuid, player.getName());
        }
        if (player.getAddress() != null) {
            user.setAddress(player.getAddress().getAddress().getHostAddress());
        }

        if (user.isFirstJoin()) {
            this.plugin.getModuleManager().getModule(SpawnModule.class).onPlayerFirstJoin(player);
        }

        var userEvent = new UserJoinEvent(user);
        this.plugin.getScheduler().runNextTick(wrappedTask -> userEvent.callEvent());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDisconnect(PlayerQuitEvent event) {

        var uuid = event.getPlayer().getUniqueId();

        var user = this.iStorage.getUser(uuid);
        if (user != null) {
            var userQuitEvent = new UserQuitEvent(user);
            userQuitEvent.callEvent();
        }

        this.iStorage.onPlayerQuit(uuid);
    }
}
