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
import java.nio.file.Files;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EnderChestModule extends ZModule {

    @NonLoadable
    private final Map<UUID, EnderChestData> dataCache = new ConcurrentHashMap<>();
    @NonLoadable
    private final Map<UUID, EnderChestSession> sessions = new ConcurrentHashMap<>();
    private int defaultPages = 1;
    private int maxPages = 3;
    private String title = "&5&lEnder Chest &8(&f%page%&8/&f%total%&8)";
    private String navFillerMaterial = "GRAY_STAINED_GLASS_PANE";
    private String navFillerColor = "&8";
    private String navFirstButton = "SPECTRAL_ARROW";
    private String navFirstText = "&7« First Page";
    private String navPrevButton = "ARROW";
    private String navPrevText = "&7← Previous Page";
    private String navNextButton = "ARROW";
    private String navNextText = "&7Next Page →";
    private String navLastButton = "SPECTRAL_ARROW";
    private String navLastText = "&7Last Page »";
    private String navCloseButton = "BARRIER";
    private String navCloseText = "&cClose";
    private boolean pageIndicator = true;
    private String pageIndicatorText = "&fPage &e%current% &7/ &e%total%";

    private boolean overviewEnabled = true;
    private String overviewTitle = "&5&lEnder Chest";
    private String overviewInfoMaterial = "ENDER_CHEST";
    private String overviewInfoText = "&5&lEnder Chest";
    private java.util.List<String> overviewInfoLore = java.util.List.of();
    private String overviewPageMaterial = "ENDER_EYE";
    private String overviewPageText = "&5Ender Chest Page &e%page%";
    private java.util.List<String> overviewPageLore = java.util.List.of();
    private String overviewLockedMaterial = "GRAY_DYE";
    private String overviewLockedText = "&cPage &e%page% &c(locked)";
    private java.util.List<String> overviewLockedLore = java.util.List.of();

    public EnderChestModule(ZEssentialsPlugin plugin) {
        super(plugin, "enderchest");
    }

    @Override
    public void loadConfiguration() {
        super.loadConfiguration();

        YamlConfiguration config = getConfiguration();
        this.defaultPages = Math.max(1, config.getInt("default-pages", 1));
        this.maxPages = Math.max(1, config.getInt("max-pages", 3));
        this.defaultPages = Math.min(this.defaultPages, this.maxPages);
        this.title = config.getString("title", "&5&lEnder Chest &8(&f%page%&8/&f%total%&8)");

        ConfigurationSection nav = config.getConfigurationSection("nav-row");
        if (nav != null) {
            this.navFillerMaterial = nav.getString("filler-material", "GRAY_STAINED_GLASS_PANE");
            this.navFillerColor = nav.getString("filler-color", "&8");
            this.navFirstButton = nav.getString("first-button", "SPECTRAL_ARROW");
            this.navFirstText = nav.getString("first-text", "&7« First Page");
            this.navPrevButton = nav.getString("prev-button", "ARROW");
            this.navPrevText = nav.getString("prev-text", "&7← Previous Page");
            this.navNextButton = nav.getString("next-button", "ARROW");
            this.navNextText = nav.getString("next-text", "&7Next Page →");
            this.navLastButton = nav.getString("last-button", "SPECTRAL_ARROW");
            this.navLastText = nav.getString("last-text", "&7Last Page »");
            this.navCloseButton = nav.getString("close-button", "BARRIER");
            this.navCloseText = nav.getString("close-text", "&cClose");
            this.pageIndicator = nav.getBoolean("page-indicator", true);
            this.pageIndicatorText = nav.getString("page-indicator-text", "&fPage &e%current% &7/ &e%total%");
        }

        ConfigurationSection overview = config.getConfigurationSection("overview");
        if (overview != null) {
            this.overviewEnabled = overview.getBoolean("enable", true);
            this.overviewTitle = overview.getString("title", "&5&lEnder Chest");
            this.overviewInfoMaterial = overview.getString("info-material", "ENDER_CHEST");
            this.overviewInfoText = overview.getString("info-text", "&5&lEnder Chest");
            this.overviewInfoLore = overview.getStringList("info-lore");
            this.overviewPageMaterial = overview.getString("page-material", "ENDER_EYE");
            this.overviewPageText = overview.getString("page-text", "&5Ender Chest Page &e%page%");
            this.overviewPageLore = overview.getStringList("page-lore");
            this.overviewLockedMaterial = overview.getString("locked-material", "GRAY_DYE");
            this.overviewLockedText = overview.getString("locked-text", "&cPage &e%page% &8(Locked)");
            this.overviewLockedLore = overview.getStringList("locked-lore");
        }

        this.loadInventory("enderchest");
        this.loadInventory("enderchest_overview");

        migrateJsonToDatabase();
    }

    public EnderChestSession getSession(Player viewer) {
        EnderChestSession session = this.sessions.get(viewer.getUniqueId());
        if (session == null) {
            int allowed = getAllowedPages(viewer);
            EnderChestData data = getData(viewer.getUniqueId(), true);
            if (data.getPages() < allowed) data.resize(allowed);
            int visible = Math.min(data.getPages(), allowed);
            session = new EnderChestSession(viewer.getUniqueId(), data, false, visible, allowed);
            this.sessions.put(viewer.getUniqueId(), session);
        }
        return session;
    }

    public EnderChestSession startSession(Player viewer, EnderChestData data, boolean readOnly, int visiblePages, int allowedPages) {
        EnderChestSession session = new EnderChestSession(viewer.getUniqueId(), data, readOnly, visiblePages, allowedPages);
        this.sessions.put(viewer.getUniqueId(), session);
        return session;
    }

    public void clearSession(Player viewer) {
        this.sessions.remove(viewer.getUniqueId());
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

    public EnderChestData getData(UUID playerId, boolean migrate) {
        EnderChestData data = dataCache.get(playerId);
        if (data != null) return data;

        String json = getStorage().getEnderChest(playerId);
        if (json != null) {
            data = EnderChestSerializer.deserialize(json, playerId);
        }
        if (data == null) {
            data = new EnderChestData(playerId, maxPages);
            if (migrate) {
                Player online = Bukkit.getPlayer(playerId);
                if (online != null) {
                    migrateFromVanilla(data, online);
                    getStorage().upsertEnderChest(playerId, EnderChestSerializer.serialize(data));
                }
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
        getStorage().upsertEnderChest(playerId, EnderChestSerializer.serialize(data));
    }

    private void migrateJsonToDatabase() {
        File dir = new File(getFolder(), "data");
        if (!dir.exists() || !dir.isDirectory()) return;
        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (files == null || files.length == 0) return;
        int count = 0;
        for (File file : files) {
            String fileName = file.getName();
            String uuidStr = fileName.substring(0, fileName.length() - ".json".length());
            try {
                UUID uuid = UUID.fromString(uuidStr);
                String json = Files.readString(file.toPath());
                EnderChestData data = EnderChestSerializer.deserialize(json, uuid);
                if (data != null) {
                    getStorage().upsertEnderChest(uuid, EnderChestSerializer.serialize(data));
                    File migrated = new File(dir, fileName + ".migrated");
                    file.renameTo(migrated);
                    count++;
                }
            } catch (Exception e) {
                this.plugin.getLogger().warning("Failed to migrate enderchest data for " + uuidStr + ": " + e.getMessage());
            }
        }
        if (count > 0) {
            this.plugin.getLogger().info("Migrated enderchest data for " + count + " players from JSON to database");
        }
    }

    public void openEnderChest(Player player) {
        int allowed = getAllowedPages(player);
        EnderChestData data = getData(player.getUniqueId(), true);
        if (data.getPages() < allowed) {
            data.resize(allowed);
        }
        int visiblePages = Math.min(data.getPages(), allowed);
        startSession(player, data, false, visiblePages, allowed);
        dev.yanianz.essentials.dependency.ZMenuBridge.openInventory(this.plugin, player, "enderchest_overview");
    }

    public void openEnderChestFor(Player viewer, OfflinePlayer target) {
        EnderChestData data = getData(target.getUniqueId(), false);
        int pages = data.getPages();
        startSession(viewer, data, true, pages, pages);
        dev.yanianz.essentials.dependency.ZMenuBridge.openInventory(this.plugin, viewer, "enderchest");
    }

    public org.bukkit.inventory.ItemStack buildOverviewPageItem(int pageNumber, boolean locked) {
        org.bukkit.Material material = org.bukkit.Material.ENDER_EYE;
        String name;
        java.util.List<String> lore;
        try {
            material = org.bukkit.Material.valueOf((locked ? overviewLockedMaterial : overviewPageMaterial).toUpperCase());
        } catch (Exception ignored) {
        }
        if (locked) {
            name = overviewLockedText.replace("%page%", String.valueOf(pageNumber));
            lore = overviewLockedLore;
        } else {
            name = overviewPageText.replace("%page%", String.valueOf(pageNumber));
            lore = overviewPageLore;
        }
        ItemStack item = new ItemStack(material);
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection()
                    .deserialize(dev.yanianz.essentials.util.ColorUtil.sections(name)));
            if (lore != null && !lore.isEmpty()) {
                meta.lore(lore.stream().map(l -> net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                        .legacySection().deserialize(dev.yanianz.essentials.util.ColorUtil.sections(l))).toList());
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public String getTitle() {
        return title;
    }

    public String getNavFirstButton() {
        return navFirstButton;
    }

    public String getNavFirstText() {
        return navFirstText;
    }

    public String getNavLastButton() {
        return navLastButton;
    }

    public String getNavLastText() {
        return navLastText;
    }

    public boolean isOverviewEnabled() {
        return overviewEnabled;
    }

    public String getOverviewTitle() {
        return overviewTitle;
    }

    public String getOverviewInfoMaterial() {
        return overviewInfoMaterial;
    }

    public String getOverviewInfoText() {
        return overviewInfoText;
    }

    public java.util.List<String> getOverviewInfoLore() {
        return overviewInfoLore;
    }

    public String getOverviewPageMaterial() {
        return overviewPageMaterial;
    }

    public String getOverviewPageText() {
        return overviewPageText;
    }

    public java.util.List<String> getOverviewPageLore() {
        return overviewPageLore;
    }

    public String getOverviewLockedMaterial() {
        return overviewLockedMaterial;
    }

    public String getOverviewLockedText() {
        return overviewLockedText;
    }

    public java.util.List<String> getOverviewLockedLore() {
        return overviewLockedLore;
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
