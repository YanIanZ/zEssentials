package fr.maxlego08.essentials.module.modules.terms;

import fr.maxlego08.essentials.ZEssentialsPlugin;
import fr.maxlego08.essentials.api.configuration.NonLoadable;
import fr.maxlego08.essentials.module.ZModule;
import dev.yanianz.essentials.util.ColorUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shows the server rules inside a custom screen to every new player, they have
 * to accept them to play. Players who refuse or ignore the terms are kicked.
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
        this.acceptButton = configuration.getString("accept-button", "&a&l[ ✔ I ACCEPT ]");
        this.denyButton = configuration.getString("deny-button", "&c&l[ ✘ I REFUSE ]");
        this.acceptHover = configuration.getString("accept-hover", "Accept the terms");
        this.denyHover = configuration.getString("deny-hover", "Refuse the terms");
        this.acceptedMessage = configuration.getString("accepted-message", "&aTerms accepted!");
        this.refuseKick = configuration.getStringList("refuse-kick");
        this.timeoutKick = configuration.getStringList("timeout-kick");

        loadAccepted();
    }

    /**
     * Opens the terms screen when a player who never accepted them joins.
     */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {

        if (!this.isEnable) return;

        Player player = event.getPlayer();
        if (this.accepted.contains(player.getUniqueId())) return;
        if (this.pending.contains(player.getUniqueId())) return;
        if (this.bypassPermission != null && player.hasPermission(this.bypassPermission)) return;

        this.pending.add(player.getUniqueId());

        // Small delay so the whole spawn sequence finished before showing the dialog,
        // opening it during the join itself can get stomped by login packets
        this.plugin.getScheduler().runAtLocationLater(player.getLocation(), () -> {
            if (this.pending.contains(player.getUniqueId()) && player.isOnline()) {
                openScreen(player);
            }
        }, 3L);

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
            event.getPlayer().sendMessage(ColorUtil.sections(this.title + " &7» &cAccept the terms first with &f/terms accept&c."));
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

    /**
     * Handles the accept and deny buttons of the screen.
     */
    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof TermsHolder)) return;

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!isPending(player.getUniqueId())) return;

        switch (event.getSlot()) {
            case SLOT_ACCEPT -> accept(player);
            case SLOT_DENY -> deny(player);
            default -> {
            }
        }
    }

    /**
     * The terms cannot be closed without an answer, reopening them instantly.
     */
    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof TermsHolder)) return;
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!isPending(player.getUniqueId())) return;

        // Reopen one tick later so the closing process finished
        this.plugin.getScheduler().runAtLocationLater(player.getLocation(),
                () -> {
                    if (isPending(player.getUniqueId()) && player.isOnline()) {
                        openScreen(player);
                    }
                }, 1L);
    }

    private boolean isPending(UUID uniqueId) {
        return this.isEnable && this.pending.contains(uniqueId);
    }

    @NonLoadable
    private static final int SLOT_ACCEPT = 20;
    @NonLoadable
    private static final int SLOT_DENY = 24;

    private final class TermsHolder implements InventoryHolder {

        private Inventory inventory;

        @Override
        public Inventory getInventory() {
            return this.inventory;
        }
    }

    /**
     * Builds and opens the terms screen for the player.
     * Uses the native minecraft dialog screen when supported,
     * falling back to the chest interface otherwise.
     */
    public void openScreen(Player player) {

        boolean shown;
        try {
            // Dialogs only exist on paper 1.21.7+, older runtimes use the chest interface
            shown = dev.yanianz.essentials.terms.TermsDialogs.show(player, this.title, this.rules, this.question,
                    this.acceptButton, this.denyButton, this.acceptHover, this.denyHover,
                    () -> {
                        accept(player);
                        return true;
                    },
                    () -> deny(player));
        } catch (Throwable throwable) {
            this.plugin.getLogger().warning("Unable to show the terms dialog: " + throwable);
            shown = false;
        }

        if (shown) return;

        TermsHolder holder = new TermsHolder();
        Inventory inventory = Bukkit.createInventory(holder, 45, component(this.title));
        holder.inventory = inventory;

        ItemStack filler = buildItem(Material.GRAY_STAINED_GLASS_PANE, "&7", null);
        for (int slot = 0; slot < 45; slot++) {
            inventory.setItem(slot, filler);
        }

        List<String> bookLore = new ArrayList<>();
        if (!this.question.isEmpty()) {
            bookLore.add(ColorUtil.sections(this.question));
            bookLore.add("");
        }
        for (String rule : this.rules) {
            String colorized = ColorUtil.sections(rule);
            if (!colorized.isBlank()) bookLore.add(colorized);
        }
        inventory.setItem(22, buildItem(Material.WRITABLE_BOOK, this.title, bookLore));

        inventory.setItem(SLOT_ACCEPT, buildItem(Material.LIME_STAINED_GLASS,
                this.acceptButton, List.of(ColorUtil.sections(this.acceptHover))));
        inventory.setItem(SLOT_DENY, buildItem(Material.RED_STAINED_GLASS,
                this.denyButton, List.of(ColorUtil.sections(this.denyHover))));

        player.openInventory(inventory);
    }

    private ItemStack buildItem(Material material, String name, List<String> lore) {

        ItemStack itemStack = new ItemStack(material);
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return itemStack;

        meta.displayName(component(name));
        if (lore != null && !lore.isEmpty()) {
            meta.lore(lore.stream().filter(line -> !line.isBlank()).map(this::component).toList());
        }
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    /**
     * Accepts the terms of the player, remembered forever.
     */
    public void accept(Player player) {
        if (!this.pending.remove(player.getUniqueId())) return;

        this.accepted.add(player.getUniqueId());
        saveAccepted();

        player.sendMessage(component(this.acceptedMessage));
        player.closeInventory();
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

        Component screen = Component.empty();
        for (String lineText : lines) {
            screen = screen.append(ColorUtil.component(lineText)).append(Component.newline());
        }
        Component finalScreen = screen;

        this.plugin.getScheduler().runAtLocation(player.getLocation(),
                wrappedTask -> player.kick(finalScreen));
    }

    private Component component(String text) {
        return LegacyComponentSerializer.legacySection().deserialize(ColorUtil.sections(text));
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
