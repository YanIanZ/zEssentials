package dev.yanianz.essentials.enderchest;

import dev.yanianz.essentials.util.ColorUtil;
import fr.maxlego08.essentials.ZEssentialsPlugin;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class EnderChestGui {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private EnderChestGui() {
    }

    public static EnderChestHolder open(ZEssentialsPlugin plugin, Player player,
                                        EnderChestData data, int pages, int startPage, boolean readOnly) {
        EnderChestModule module = plugin.getModuleManager().getModule(EnderChestModule.class);
        int currentPage = Math.max(0, Math.min(startPage, pages - 1));

        EnderChestHolder holder = new EnderChestHolder(data.getPlayerId(), data, currentPage, pages, readOnly);
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

    static void fillContent(Inventory inventory, EnderChestData data, int page) {
        List<ItemStack> contents = data.getPageContents(page);
        for (int slot = 0; slot < EnderChestSlotMap.CONTENT_SLOTS; slot++) {
            ItemStack item = contents.get(slot);
            if (item != null) inventory.setItem(slot, item);
        }
    }

    static void fillNavRow(Inventory inventory, EnderChestModule module,
                           int currentPage, int pages, EnderChestHolder holder) {
        Material filler = parseMaterial(module.getNavFillerMaterial(), Material.GRAY_STAINED_GLASS_PANE);
        ItemStack fillerItem = namedItem(filler, module.getNavFillerColor() + " ");

        for (int slot = EnderChestSlotMap.NAV_ROW_START; slot < 54; slot++) {
            inventory.setItem(slot, fillerItem);
        }

        if (currentPage > 0) {
            inventory.setItem(EnderChestSlotMap.SLOT_FIRST,
                    namedItem(parseMaterial(module.getNavFirstButton(), Material.SPECTRAL_ARROW), module.getNavFirstText()));
            inventory.setItem(EnderChestSlotMap.SLOT_PREV,
                    namedItem(parseMaterial(module.getNavPrevButton(), Material.ARROW), module.getNavPrevText()));
        }
        inventory.setItem(EnderChestSlotMap.SLOT_CLOSE,
                namedItem(parseMaterial(module.getNavCloseButton(), Material.BARRIER), module.getNavCloseText()));

        if (module.isPageIndicator()) {
            String indicator = module.getPageIndicatorText()
                    .replace("%current%", String.valueOf(currentPage + 1))
                    .replace("%total%", String.valueOf(pages));
            inventory.setItem(EnderChestSlotMap.SLOT_INDICATOR, namedItem(filler, indicator));
        }

        if (currentPage < pages - 1) {
            inventory.setItem(EnderChestSlotMap.SLOT_NEXT,
                    namedItem(parseMaterial(module.getNavNextButton(), Material.ARROW), module.getNavNextText()));
            inventory.setItem(EnderChestSlotMap.SLOT_LAST,
                    namedItem(parseMaterial(module.getNavLastButton(), Material.SPECTRAL_ARROW), module.getNavLastText()));
        }
    }

    /**
     * Hypixel SkyBlock style overview: ender chest info icon at the top,
     * one clickable button per page with locked pages grayed out.
     */
    public static void openOverview(ZEssentialsPlugin plugin, Player player,
                                    EnderChestData data, int visiblePages, int allowedPages) {
        EnderChestModule module = plugin.getModuleManager().getModule(EnderChestModule.class);
        EnderChestHolder holder = new EnderChestHolder(data.getPlayerId(), data, 0, visiblePages, false, true, allowedPages);

        Inventory inventory = Bukkit.createInventory(holder, 54,
                LEGACY.deserialize(ColorUtil.sections(module.getOverviewTitle())));
        holder.setInventory(inventory);

        Material filler = parseMaterial(module.getNavFillerMaterial(), Material.GRAY_STAINED_GLASS_PANE);
        ItemStack fillerItem = namedItem(filler, module.getNavFillerColor() + " ");
        for (int i = 0; i < 54; i++) inventory.setItem(i, fillerItem);

        inventory.setItem(EnderChestSlotMap.OVERVIEW_INFO_SLOT,
                namedItem(parseMaterial(module.getOverviewInfoMaterial(), Material.ENDER_CHEST),
                        module.getOverviewInfoText(), module.getOverviewInfoLore()));

        for (int i = 0; i < Math.max(visiblePages, allowedPages); i++) {
            int slot = EnderChestSlotMap.OVERVIEW_PAGE_START + i;
            if (slot > EnderChestSlotMap.OVERVIEW_CLOSE_SLOT) break;
            String pageStr = String.valueOf(i + 1);
            if (i < visiblePages) {
                inventory.setItem(slot, namedItem(
                        parseMaterial(module.getOverviewPageMaterial(), Material.ENDER_EYE),
                        module.getOverviewPageText().replace("%page%", pageStr),
                        module.getOverviewPageLore()));
            } else {
                inventory.setItem(slot, namedItem(
                        parseMaterial(module.getOverviewLockedMaterial(), Material.GRAY_DYE),
                        module.getOverviewLockedText().replace("%page%", pageStr),
                        module.getOverviewLockedLore()));
            }
        }

        inventory.setItem(EnderChestSlotMap.OVERVIEW_CLOSE_SLOT,
                namedItem(parseMaterial(module.getNavCloseButton(), Material.BARRIER), module.getNavCloseText()));

        player.openInventory(inventory);
    }

    static void switchPage(ZEssentialsPlugin plugin, Player player, EnderChestHolder holder, int newPage) {
        if (newPage < 0 || newPage >= holder.getPages()) return;
        holder.setCurrentPage(newPage);
        EnderChestModule module = plugin.getModuleManager().getModule(EnderChestModule.class);
        Inventory inventory = holder.getInventory();
        inventory.clear();
        fillContent(inventory, holder.getData(), newPage);
        fillNavRow(inventory, module, newPage, holder.getPages(), holder);
    }

    static ItemStack namedItem(Material material, String nameLegacy) {
        return namedItem(material, nameLegacy, null);
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
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (Exception e) {
            return fallback;
        }
    }
}
