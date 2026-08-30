package dev.yanianz.essentials.stash;

import dev.yanianz.essentials.util.ColorUtil;
import fr.maxlego08.essentials.ZEssentialsPlugin;
import fr.maxlego08.essentials.module.modules.StashModule;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class MaterialStashGui {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();
    private static final int MAX_SLOTS = 45;

    private MaterialStashGui() {}

    public static MaterialStashHolder open(ZEssentialsPlugin plugin, Player player,
                                          MaterialStashData data, boolean readOnly) {
        StashModule module = plugin.getModuleManager().getModule(StashModule.class);
        MaterialStashHolder holder = new MaterialStashHolder(data.getPlayerId(), data, readOnly);

        Inventory inventory = Bukkit.createInventory(holder, 54,
                LEGACY.deserialize(ColorUtil.sections(module.getMaterialTitle())));
        holder.setInventory(inventory);

        fillMaterials(inventory, data, module);
        player.openInventory(inventory);
        return holder;
    }

    static void fillMaterials(Inventory inventory, MaterialStashData data, StashModule module) {
        String fillerMat = module.getConfiguration().getString("material-filler-material", "BLACK_STAINED_GLASS_PANE");
        ItemStack filler = namedItem(parseMaterial(fillerMat, Material.BLACK_STAINED_GLASS_PANE), "&8 ");
        for (int i = 0; i < 54; i++) inventory.setItem(i, filler);

        int slot = 0;
        List<Map.Entry<Material, Long>> entries = new ArrayList<>(data.getQuantities().entrySet());
        for (Map.Entry<Material, Long> entry : entries) {
            if (slot >= MAX_SLOTS) break;
            Material material = entry.getKey();
            long qty = entry.getValue();
            int maxStack = material.getMaxStackSize();
            ItemStack display = new ItemStack(material, Math.min((int) Math.min(qty, maxStack), maxStack));
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                meta.displayName(LEGACY.deserialize(ColorUtil.sections("&e" + niceName(material))));
                meta.lore(List.of(
                        LEGACY.deserialize(ColorUtil.sections("&7Total: &f" + formatNumber(qty))),
                        LEGACY.deserialize(ColorUtil.sections("&7Stack size: &f" + maxStack)),
                        LEGACY.deserialize(ColorUtil.sections("&aLeft-click: withdraw 1 stack")),
                        LEGACY.deserialize(ColorUtil.sections("&aRight-click: withdraw half")),
                        LEGACY.deserialize(ColorUtil.sections("&aShift-click: withdraw all"))
                ));
                display.setItemMeta(meta);
            }
            inventory.setItem(slot++, display);
        }

        String closeMat = module.getConfiguration().getString("material-close-material", "BARRIER");
        inventory.setItem(49, namedItem(parseMaterial(closeMat, Material.BARRIER), "&cClose"));
    }

    private static String niceName(Material material) {
        String name = material.name().toLowerCase().replace('_', ' ');
        StringBuilder sb = new StringBuilder();
        for (String word : name.split(" ")) {
            if (!word.isEmpty()) sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }

    private static String formatNumber(long n) {
        if (n >= 1_000_000L) return String.format(java.util.Locale.US, "%.1fM", n / 1_000_000.0);
        if (n >= 1_000L) return String.format(java.util.Locale.US, "%.1fK", n / 1_000.0);
        return String.valueOf(n);
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