package dev.yanianz.essentials.crafting;

import dev.yanianz.essentials.util.ColorUtil;
import fr.maxlego08.essentials.module.modules.CraftingModule;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class CraftingGuiItems {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private CraftingGuiItems() {
    }

    public static ItemStack quickCraft(CraftingModule module) {
        if (module == null) return new ItemStack(Material.AIR);
        return namedItem(parse(module.getQuickCraftMaterial(), Material.ANVIL),
                module.getQuickCraftText(), module.getQuickCraftLore());
    }

    public static ItemStack close(CraftingModule module) {
        if (module == null) return new ItemStack(Material.AIR);
        return namedItem(parse(module.getCloseMaterial(), Material.BARRIER),
                module.getCloseText(), null);
    }

    public static ItemStack craftButton() {
        return namedItem(Material.BONE, "&a&lCraft Item", java.util.List.of(
                "&7Click to craft the result",
                "&7into your inventory."));
    }

    public static ItemStack redFiller() {
        return namedItem(Material.RED_STAINED_GLASS_PANE, "&e", null);
    }

    private static Material parse(String name, Material fallback) {
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (Exception e) {
            return fallback;
        }
    }

    private static ItemStack namedItem(Material material, String nameLegacy, List<String> loreLegacy) {
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
}
