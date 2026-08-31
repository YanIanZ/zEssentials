package dev.yanianz.essentials.stash;

import dev.yanianz.essentials.util.ColorUtil;
import fr.maxlego08.essentials.ZEssentialsPlugin;
import fr.maxlego08.essentials.module.modules.StashModule;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class StashPickerGui {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private StashPickerGui() {}

    public static void open(ZEssentialsPlugin plugin, Player player, StashModule module) {
        Inventory inventory = Bukkit.createInventory(null, 27,
                LEGACY.deserialize(ColorUtil.sections(module.getPickerTitle())));

        ItemStack filler = namedItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) inventory.setItem(i, filler);

        ConfigurationSection itemCfg = module.getConfiguration().getConfigurationSection("picker-item-stash");
        if (itemCfg != null) {
            inventory.setItem(11, namedItem(
                    parseMaterial(itemCfg.getString("icon", "DIAMOND_SWORD"), Material.DIAMOND_SWORD),
                    itemCfg.getString("name", "&d&lItem Stash"),
                    itemCfg.getStringList("lore")));
        }

        ConfigurationSection matCfg = module.getConfiguration().getConfigurationSection("picker-material-stash");
        if (matCfg != null) {
            inventory.setItem(15, namedItem(
                    parseMaterial(matCfg.getString("icon", "COBBLESTONE"), Material.COBBLESTONE),
                    matCfg.getString("name", "&e&lMaterial Stash"),
                    matCfg.getStringList("lore")));
        }

        player.openInventory(inventory);

        plugin.getScheduler().runNextTick(task -> {
            StashPickerListener.register(plugin, player, module);
        });
    }

    static ItemStack namedItem(Material material, String nameLegacy) {
        return namedItem(material, nameLegacy, List.of());
    }

    static ItemStack namedItem(Material material, String nameLegacy, List<String> loreLegacy) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(LEGACY.deserialize(ColorUtil.sections(nameLegacy)));
            if (loreLegacy != null && !loreLegacy.isEmpty()) {
                meta.lore(loreLegacy.stream().map(l -> LEGACY.deserialize(ColorUtil.sections(l))).toList());
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    static Material parseMaterial(String name, Material fallback) {
        try { return Material.valueOf(name.toUpperCase()); }
        catch (Exception e) { return fallback; }
    }
}