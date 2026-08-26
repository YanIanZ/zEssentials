package fr.maxlego08.essentials.module.modules.terms;

import fr.maxlego08.essentials.ZEssentialsPlugin;
import fr.maxlego08.essentials.api.configuration.NonLoadable;
import fr.maxlego08.essentials.module.ZModule;
import fr.maxlego08.essentials.zutils.utils.paper.PaperComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shows the server rules to every new player, they have to accept them to play.
 * Players who refuse or ignore the terms are kicked.
 */
public class TermsModule extends ZModule {

    private int timeout;
    private String bypassPermission;
    private String title;
    private List<String> rules = new ArrayList<>();
    private String question;
    private String acceptButton;
    private String denyButton;
    private String acceptHover;
    private String denyHover;
    private String acceptedMessage;
    private List<String> refuseKick = new ArrayList<>();
    private List<String> timeoutKick = new ArrayList<>();

    @NonLoadable
    private final Set<UUID> pending = ConcurrentHashMap.newKeySet();
    @NonLoadable
    private final List<UUID> accepted = new ArrayList<>();
    @NonLoadable
    private final PaperComponent paperComponent = new PaperComponent();

    public TermsModule(ZEssentialsPlugin plugin) {
        super(plugin, "terms");
    }

    @Override
    public void loadConfiguration() {
        super.loadConfiguration();

        var configuration = getConfiguration();
        this.timeout = configuration.getInt("timeout", 60);
        this.bypassPermission = configuration.getString("bypass-permission", "essentials.terms.bypass");
        this.title = configuration.getString("title", "&lTERMS OF SERVICE");
        this.rules = configuration.getStringList("rules");
        this.question = configuration.getString("question", "");
        this.acceptButton = configuration.getString("accept-button", "&a[ACCEPT]");
        this.denyButton = configuration.getString("deny-button", "&c[REFUSE]");
        this.acceptHover = configuration.getString("accept-hover", "Accept the terms");
        this.denyHover = configuration.getString("deny-hover", "Refuse the terms");
        this.acceptedMessage = configuration.getString("accepted-message", "&aTerms accepted!");
        this.refuseKick = configuration.getStringList("refuse-kick");
        this.timeoutKick = configuration.getStringList("timeout-kick");

        loadAccepted();
    }

    /**
     * Sends the terms to the player on join when they never accepted them.
     */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {

        if (!this.isEnable) return;

        Player player = event.getPlayer();
        if (this.accepted.contains(player.getUniqueId())) return;
        if (this.pending.contains(player.getUniqueId())) return;
        if (this.bypassPermission != null && player.hasPermission(this.bypassPermission)) return;

        this.pending.add(player.getUniqueId());
        sendTerms(player);

        // When the timeout expires without an answer, kick the player
        this.plugin.getScheduler().runLater(() -> {
            if (this.pending.remove(player.getUniqueId()) && player.isOnline()) {
                kick(player, this.timeoutKick);
            }
        }, this.timeout * 20L);
    }

    /**
     * Blocks commands while the terms are pending, only /terms is allowed.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!isPending(event.getPlayer().getUniqueId())) return;

        if (!event.getMessage().toLowerCase().startsWith("/terms")) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(colorize(this.title + " &7» &cAccept the terms first with &f/terms accept&c."));
        }
    }

    /**
     * Blocks chat while the terms are pending.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        if (isPending(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    private boolean isPending(UUID uniqueId) {
        return this.isEnable && this.pending.contains(uniqueId);
    }

    private void sendTerms(Player player) {

        player.sendMessage(Component.empty());
        player.sendMessage(component(this.title));
        for (String rule : this.rules) {
            player.sendMessage(component(rule));
        }
        player.sendMessage(component(this.question));
        player.sendMessage(Component.empty());

        // Two clickable buttons, one accepts the terms, the other kicks the player
        Component accept = component(this.acceptButton)
                .clickEvent(ClickEvent.runCommand("/terms accept"))
                .hoverEvent(HoverEvent.showText(component(this.acceptHover)));
        Component deny = component(this.denyButton)
                .clickEvent(ClickEvent.runCommand("/terms deny"))
                .hoverEvent(HoverEvent.showText(component(this.denyHover)));

        player.sendMessage(Component.empty().append(accept).append(Component.space()).append(deny));
        player.sendMessage(Component.empty());
    }

    /**
     * Accepts the terms of the player, remembered forever.
     */
    public void accept(Player player) {
        if (!this.pending.remove(player.getUniqueId())) return;

        this.accepted.add(player.getUniqueId());
        saveAccepted();

        player.sendMessage(component(this.acceptedMessage));
    }

    /**
     * Kicks the player for refusing the terms.
     */
    public void deny(Player player) {
        if (!this.pending.remove(player.getUniqueId())) return;
        kick(player, this.refuseKick);
    }

    /**
     * Forgets that a player accepted the terms, admin command.
     */
    public void reset(UUID uniqueId) {
        this.accepted.remove(uniqueId);
        saveAccepted();
    }

    public boolean hasAccepted(UUID uniqueId) {
        return this.accepted.contains(uniqueId);
    }

    private void kick(Player player, List<String> lines) {

        StringBuilder builder = new StringBuilder();
        for (String line : lines) {
            builder.append(colorize(line)).append("\n");
        }

        this.plugin.getScheduler().runAtLocation(player.getLocation(), wrappedTask -> player.kickPlayer(builder.toString()));
    }

    /**
     * Parses a config line supporting legacy colors, hex colors, MiniMessage and placeholders.
     */
    private Component component(String text) {
        return this.paperComponent.getComponent(colorize(text));
    }

    private String colorize(String text) {
        return text == null ? "" : text.replace("&", "§");
    }

    private File getStorageFile() {
        return new File(getFolder(), "accepted.yml");
    }

    private void loadAccepted() {
        this.accepted.clear();
        File file = getStorageFile();
        if (!file.exists()) return;

        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
        for (String uuid : configuration.getStringList("accepted")) {
            try {
                this.accepted.add(UUID.fromString(uuid));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private void saveAccepted() {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("accepted", this.accepted.stream().map(UUID::toString).toList());
        try {
            configuration.save(getStorageFile());
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }
}
