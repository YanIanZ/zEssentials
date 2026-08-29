package dev.yanianz.essentials.enderchest;

import fr.maxlego08.essentials.ZEssentialsPlugin;
import fr.maxlego08.essentials.api.commands.Permission;
import fr.maxlego08.essentials.api.configuration.NonLoadable;
import fr.maxlego08.essentials.module.ZModule;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EnderChestModule extends ZModule {

    private static boolean listenerRegistered = false;

    @NonLoadable
    private final Map<UUID, EnderChestData> dataCache = new HashMap<>();
    private int defaultPages = 1;
    private int maxPages = 3;
    private String title = "&5&lEnder Chest &8(&f%page%&8/&f%total%&8)";
    private String navFillerMaterial = "GRAY_STAINED_GLASS_PANE";
    private String navFillerColor = "&8";
    private String navPrevButton = "ARROW";
    private String navPrevText = "&7« Previous Page";
    private String navNextButton = "ARROW";
    private String navNextText = "&7Next Page »";
    private String navCloseButton = "BARRIER";
    private String navCloseText = "&cClose";
    private boolean pageIndicator = true;
    private String pageIndicatorText = "&fPage &e%current% &7/ &e%total%";

    public EnderChestModule(ZEssentialsPlugin plugin) {
        super(plugin, "enderchest");
        if (!listenerRegistered) {
            Bukkit.getPluginManager().registerEvents(new EnderChestListener(plugin), plugin);
            listenerRegistered = true;
        }
    }

    @Override
    public void loadConfiguration() {
        super.loadConfiguration();

        YamlConfiguration config = getConfiguration();
        this.defaultPages = Math.max(1, config.getInt("default-pages", 1));
        this.maxPages = Math.max(1, config.getInt("max-pages", 3));
        this.title = config.getString("title", "&5&lEnder Chest &8(&f%page%&8/&f%total%&8)");

        ConfigurationSection nav = config.getConfigurationSection("nav-row");
        if (nav != null) {
            this.navFillerMaterial = nav.getString("filler-material", "GRAY_STAINED_GLASS_PANE");
            this.navFillerColor = nav.getString("filler-color", "&8");
            this.navPrevButton = nav.getString("prev-button", "ARROW");
            this.navPrevText = nav.getString("prev-text", "&7« Previous Page");
            this.navNextButton = nav.getString("next-button", "ARROW");
            this.navNextText = nav.getString("next-text", "&7Next Page »");
            this.navCloseButton = nav.getString("close-button", "BARRIER");
            this.navCloseText = nav.getString("close-text", "&cClose");
            this.pageIndicator = nav.getBoolean("page-indicator", true);
            this.pageIndicatorText = nav.getString("page-indicator-text", "&fPage &e%current% &7/ &e%total%");
        }
    }

    public int getAllowedPages(Player player) {
        int best = defaultPages;
        for (int n = maxPages; n >= 1; n--) {
            if (player.hasPermission(Permission.ESSENTIALS_ENDERCHEST.asPermission(".pages." + n))) {
                best = Math.min(n, maxPages);
                break;
            }
        }
        return Math.max(1, best);
    }

    public EnderChestData getData(UUID playerId) {
        EnderChestData data = dataCache.get(playerId);
        if (data != null) return data;

        File file = getDataFile(playerId);
        if (file.exists()) {
            data = loadFromFile(file, playerId);
        }
        if (data == null) {
            data = new EnderChestData(playerId, maxPages);
            Player online = Bukkit.getPlayer(playerId);
            if (online != null) {
                migrateFromVanilla(data, online);
            }
        }
        dataCache.put(playerId, data);
        return data;
    }

    private void migrateFromVanilla(EnderChestData data, Player player) {
        ItemStack[] vanilla = player.getEnderChest().getContents();
        for (int i = 0; i < Math.min(vanilla.length, EnderChestSlotMap.CONTENT_SLOTS); i++) {
            if (vanilla[i] != null && !vanilla[i].getType().isAir()) {
                data.setContent(0, i, vanilla[i]);
            }
        }
        player.getEnderChest().clear();
    }

    public void saveData(UUID playerId) {
        EnderChestData data = dataCache.get(playerId);
        if (data == null) return;
        saveToFile(data);
    }

    private File getDataFile(UUID playerId) {
        File dir = new File(getFolder(), "data");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, playerId + ".json");
    }

    private EnderChestData loadFromFile(File file, UUID playerId) {
        try {
            String json = Files.readString(file.toPath());
            return EnderChestSerializer.deserialize(json, playerId);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void saveToFile(EnderChestData data) {
        try {
            String json = EnderChestSerializer.serialize(data);
            Files.writeString(getDataFile(data.getPlayerId()).toPath(), json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void openEnderChest(Player player) {
        int allowed = getAllowedPages(player);
        EnderChestData data = getData(player.getUniqueId());
        if (data.getPages() < allowed) {
            data.resize(allowed);
        }
        int visiblePages = Math.min(data.getPages(), allowed);
        EnderChestGui.open(this.plugin, player, data, visiblePages, 0, false);
    }

    public void openEnderChestFor(Player viewer, OfflinePlayer target) {
        EnderChestData data = getData(target.getUniqueId());
        int pages = data.getPages();
        EnderChestGui.open(this.plugin, viewer, data, pages, 0, true);
    }

    public String getTitle() {
        return title;
    }

    public String getNavFillerMaterial() {
        return navFillerMaterial;
    }

    public String getNavFillerColor() {
        return navFillerColor;
    }

    public String getNavPrevButton() {
        return navPrevButton;
    }

    public String getNavPrevText() {
        return navPrevText;
    }

    public String getNavNextButton() {
        return navNextButton;
    }

    public String getNavNextText() {
        return navNextText;
    }

    public String getNavCloseButton() {
        return navCloseButton;
    }

    public String getNavCloseText() {
        return navCloseText;
    }

    public boolean isPageIndicator() {
        return pageIndicator;
    }

    public String getPageIndicatorText() {
        return pageIndicatorText;
    }

    public int getMaxPages() {
        return maxPages;
    }
}
