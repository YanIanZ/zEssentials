package dev.yanianz.essentials.crafting;

import dev.yanianz.essentials.util.ColorUtil;
import fr.maxlego08.essentials.ZEssentialsPlugin;
import fr.maxlego08.essentials.module.modules.CraftingModule;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class CraftingGui {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private CraftingGui() {
    }

    public static CraftingHolder open(ZEssentialsPlugin plugin, Player player, CraftingModule module) {
        boolean quickCraft = player.hasPermission(module.getQuickCraftPermission());
        CraftingHolder holder = new CraftingHolder(player.getUniqueId(), quickCraft);

        Inventory inventory = Bukkit.createInventory(holder, 54,
                LEGACY.deserialize(ColorUtil.sections(module.getTitle())));
        holder.setInventory(inventory);

        ItemStack filler = namedItem(Material.valueOf(module.getFillerMaterial()), module.getFillerColor() + " ");
        for (int i = 0; i < 54; i++) {
            if (CraftingSlotMap.isFillerSlot(i)) {
                inventory.setItem(i, filler);
            }
        }

        if (quickCraft) {
            for (int slot : CraftingSlotMap.QUICK_CRAFT_SLOTS) {
                inventory.setItem(slot,
                        namedItem(Material.valueOf(module.getQuickCraftMaterial()),
                                module.getQuickCraftText(), module.getQuickCraftLore()));
            }
        }

        inventory.setItem(CraftingSlotMap.SLOT_CLOSE,
                namedItem(Material.valueOf(module.getCloseMaterial()), module.getCloseText()));

        player.openInventory(inventory);
        return holder;
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
}
