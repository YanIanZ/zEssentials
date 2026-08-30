package dev.yanianz.essentials.stash;

import dev.yanianz.essentials.enderchest.EnderChestSlotMap;
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

public final class ItemStashGui {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private ItemStashGui() {}

    public static ItemStashHolder open(ZEssentialsPlugin plugin, Player player,
                                       ItemStashData data, int pages, int startPage, boolean readOnly) {
        StashModule module = plugin.getModuleManager().getModule(StashModule.class);
        int currentPage = Math.max(0, Math.min(startPage, pages - 1));

        ItemStashHolder holder = new ItemStashHolder(data.getPlayerId(), data, currentPage, pages, readOnly);
        String title = module.getTitle()
                .replace("%page%", String.valueOf(currentPage + 1))
                .replace("%total%", String.valueOf(pages));
        Inventory inventory = Bukkit.createInventory(holder, 54,
                LEGACY.deserialize(ColorUtil.sections(title)));
        holder.setInventory(inventory);

        fillContent(inventory, data, currentPage);
        fillNavRow(inventory, module, currentPage, pages, holder);

        player.openInventory(inventory);
        return holder;
    }

    static void fillContent(Inventory inventory, ItemStashData data, int page) {
        List<ItemStack> contents = data.getPageContents(page);
        for (int slot = 0; slot < EnderChestSlotMap.CONTENT_SLOTS; slot++) {
            ItemStack item = contents.get(slot);
            if (item != null) inventory.setItem(slot, item);
        }
    }

    static void fillNavRow(Inventory inventory, StashModule module, int currentPage, int pages, ItemStashHolder holder) {
        ConfigurationSection nav = module.getConfiguration().getConfigurationSection("nav-row");
        if (nav == null) nav = module.getConfiguration().createSection("nav-row");
        String fillerMat = nav.getString("filler-material", "GRAY_STAINED_GLASS_PANE");
        String fillerColor = nav.getString("filler-color", "&8");
        ItemStack filler = namedItem(parseMaterial(fillerMat, Material.GRAY_STAINED_GLASS_PANE), fillerColor + " ");

        for (int slot = EnderChestSlotMap.NAV_ROW_START; slot < 54; slot++) {
            inventory.setItem(slot, filler);
        }

        if (currentPage > 0) {
            inventory.setItem(EnderChestSlotMap.SLOT_PREV,
                    namedItem(parseMaterial(nav.getString("prev-button", "ARROW"), Material.ARROW),
                            nav.getString("prev-text", "&7« Previous Page")));
        }
        inventory.setItem(EnderChestSlotMap.SLOT_CLOSE,
                namedItem(parseMaterial(nav.getString("close-button", "BARRIER"), Material.BARRIER),
                        nav.getString("close-text", "&cClose")));
        if (currentPage < pages - 1) {
            inventory.setItem(EnderChestSlotMap.SLOT_NEXT,
                    namedItem(parseMaterial(nav.getString("next-button", "ARROW"), Material.ARROW),
                            nav.getString("next-text", "&7Next Page »")));
        }
    }

    static void switchPage(ZEssentialsPlugin plugin, Player player, ItemStashHolder holder, int newPage) {
        if (newPage < 0 || newPage >= holder.getPages()) return;
        holder.setCurrentPage(newPage);
        StashModule module = plugin.getModuleManager().getModule(StashModule.class);
        Inventory inventory = holder.getInventory();
        inventory.clear();
        fillContent(inventory, holder.getData(), newPage);
        fillNavRow(inventory, module, newPage, holder.getPages(), holder);
    }

    static ItemStack namedItem(Material material, String nameLegacy) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(LEGACY.deserialize(ColorUtil.sections(nameLegacy)));
            item.setItemMeta(meta);
        }
        return item;
    }

    static Material parseMaterial(String name, Material fallback) {
        try { return Material.valueOf(name.toUpperCase()); }
        catch (Exception e) { return fallback; }
    }
}