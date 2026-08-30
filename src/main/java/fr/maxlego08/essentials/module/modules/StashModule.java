package fr.maxlego08.essentials.module.modules;

import dev.yanianz.essentials.stash.ItemStashData;
import dev.yanianz.essentials.stash.MaterialStashData;
import fr.maxlego08.essentials.ZEssentialsPlugin;
import fr.maxlego08.essentials.api.configuration.NonLoadable;
import fr.maxlego08.essentials.api.commands.Permission;
import fr.maxlego08.essentials.module.ZModule;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StashModule extends ZModule {

    private boolean enabled = true;
    private int maxItemPages = 16;
    private String title = "&d&lItem Stash &8(&f%page%&8/&f%total%&8)";
    private String materialTitle = "&e&lMaterial Stash";
    private String pickerTitle = "&6&lStash";
    private boolean migrateFromVanilla = true;

    @NonLoadable
    private final Map<UUID, ItemStashData> itemDataCache = new HashMap<>();
    @NonLoadable
    private final Map<UUID, MaterialStashData> materialDataCache = new HashMap<>();

    public StashModule(ZEssentialsPlugin plugin) {
        super(plugin, "stash");
    }

    @Override
    public void loadConfiguration() {
        super.loadConfiguration();
        YamlConfiguration config = getConfiguration();
        this.enabled = config.getBoolean("enable", true);
        this.maxItemPages = Math.max(1, config.getInt("max-item-pages", 16));
        this.title = config.getString("title", "&d&lItem Stash &8(&f%page%&8/&f%total%&8)");
        this.materialTitle = config.getString("material-title", "&e&lMaterial Stash");
        this.pickerTitle = config.getString("picker-title", "&6&lStash");
        this.migrateFromVanilla = config.getBoolean("migrate-from-vanilla-inventory", true);
    }

    public int getAllowedItemPages(Player player) {
        int best = 1;
        for (int n = maxItemPages; n >= 1; n--) {
            if (player.hasPermission(Permission.ESSENTIALS_STASH.asPermission(".item.pages." + n))) {
                best = Math.min(n, maxItemPages);
                break;
            }
        }
        return best;
    }

    public void openCategoryPicker(Player player) {
        if (!isEnabled()) return;
        dev.yanianz.essentials.stash.StashPickerGui.open(this.plugin, player, this);
    }

    public void openItemStash(Player player) {
        if (!isEnabled()) return;
        int allowed = getAllowedItemPages(player);
        ItemStashData data = getItemData(player.getUniqueId());
        if (data.getPages() < allowed) data.resize(allowed);
        int visiblePages = Math.min(data.getPages(), allowed);
        dev.yanianz.essentials.stash.ItemStashGui.open(this.plugin, player, data, visiblePages, 0, false);
    }

    public void openMaterialStash(Player player) {
        if (!isEnabled()) return;
        MaterialStashData data = getMaterialData(player.getUniqueId());
        dev.yanianz.essentials.stash.MaterialStashGui.open(this.plugin, player, data, false);
    }

    public ItemStashData getItemData(UUID playerId) {
        return itemDataCache.computeIfAbsent(playerId, id -> {
            File file = getItemFile(id);
            if (file.exists()) {
                ItemStashData loaded = dev.yanianz.essentials.stash.ItemStashSerializer.deserialize(
                        readFile(file), id);
                if (loaded != null) return loaded;
            }
            ItemStashData data = new ItemStashData(id, maxItemPages);
            if (migrateFromVanilla) {
                Player online = org.bukkit.Bukkit.getPlayer(id);
                if (online != null) migrateFromInventory(data, online);
            }
            return data;
        });
    }

    private void migrateFromInventory(ItemStashData data, Player player) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            org.bukkit.inventory.ItemStack item = player.getInventory().getItem(i);
            if (item == null || item.getType().isAir()) continue;
            if (item.getMaxStackSize() > 1) continue;
            int slot = data.addToFirstAvailable(item);
            if (slot < 0) break;
            player.getInventory().setItem(i, null);
        }
    }

    public void saveItemData(UUID playerId) {
        ItemStashData data = itemDataCache.get(playerId);
        if (data == null) return;
        writeFile(getItemFile(playerId), dev.yanianz.essentials.stash.ItemStashSerializer.serialize(data));
    }

    public MaterialStashData getMaterialData(UUID playerId) {
        return materialDataCache.computeIfAbsent(playerId, id -> {
            File file = getMaterialFile(id);
            if (file.exists()) {
                MaterialStashData loaded = dev.yanianz.essentials.stash.MaterialStashSerializer.deserialize(
                        readFile(file), id);
                if (loaded != null) return loaded;
            }
            return new MaterialStashData(id);
        });
    }

    public void saveMaterialData(UUID playerId) {
        MaterialStashData data = materialDataCache.get(playerId);
        if (data == null) return;
        writeFile(getMaterialFile(playerId), dev.yanianz.essentials.stash.MaterialStashSerializer.serialize(data));
    }

    private File getItemFile(UUID playerId) {
        File dir = new File(getFolder(), "data/items");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, playerId + ".json");
    }

    private File getMaterialFile(UUID playerId) {
        File dir = new File(getFolder(), "data/materials");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, playerId + ".json");
    }

    private String readFile(File file) {
        try { return new String(java.nio.file.Files.readAllBytes(file.toPath())); }
        catch (Exception e) { return ""; }
    }

    private void writeFile(File file, String content) {
        try { java.nio.file.Files.writeString(file.toPath(), content); }
        catch (Exception e) { e.printStackTrace(); }
    }

    public boolean isEnabled() { return enabled && isEnable; }
    public int getMaxItemPages() { return maxItemPages; }
    public String getTitle() { return title; }
    public String getMaterialTitle() { return materialTitle; }
    public String getPickerTitle() { return pickerTitle; }
    public boolean isMigrateFromVanilla() { return migrateFromVanilla; }
}