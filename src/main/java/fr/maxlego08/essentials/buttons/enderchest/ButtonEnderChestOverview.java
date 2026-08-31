package fr.maxlego08.essentials.buttons.enderchest;

import dev.yanianz.essentials.dependency.ZMenuBridge;
import dev.yanianz.essentials.enderchest.EnderChestModule;
import dev.yanianz.essentials.enderchest.EnderChestSession;
import fr.maxlego08.essentials.ZEssentialsPlugin;
import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.menu.api.button.Button;
import fr.maxlego08.menu.api.engine.InventoryEngine;
import fr.maxlego08.menu.api.utils.Placeholders;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.List;

/**
 * zMenu button rendering the Hypixel-style ender chest overview: one
 * clickable button per unlocked page, locked pages grayed out.
 */
public class ButtonEnderChestOverview extends Button {

    private final EssentialsPlugin plugin;

    public ButtonEnderChestOverview(Plugin plugin) {
        this.plugin = (EssentialsPlugin) plugin;
    }

    private EnderChestSession session(Player player) {
        EnderChestModule module = module();
        return module == null ? null : module.getSession(player);
    }

    private EnderChestModule module() {
        if (this.plugin instanceof ZEssentialsPlugin zPlugin) {
            return zPlugin.getModuleManager().getModule(EnderChestModule.class);
        }
        return null;
    }

    @Override
    public boolean hasSpecialRender() {
        return true;
    }

    @Override
    public void onRender(Player player, InventoryEngine inventory) {
        EnderChestModule module = module();
        EnderChestSession session = session(player);
        if (module == null || session == null) return;

        int index = 0;
        for (int slot : this.slots) {
            int page = index++;

            if (page < session.getVisiblePages()) {
                inventory.addItem(slot, module.buildOverviewPageItem(page + 1, false)).setClick(event -> {
                    event.setCancelled(true);
                    session.setPage(page);
                    ZMenuBridge.openInventory((fr.maxlego08.essentials.ZEssentialsPlugin) this.plugin, player, "enderchest");
                });
            } else {
                inventory.addItem(slot, module.buildOverviewPageItem(page + 1, true)).setClick(event -> event.setCancelled(true));
            }
        }
    }
}
