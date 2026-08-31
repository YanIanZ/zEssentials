package fr.maxlego08.essentials.buttons.craft;

import dev.yanianz.essentials.crafting.CraftingGuiItems;
import dev.yanianz.essentials.crafting.CraftingSession;
import dev.yanianz.essentials.crafting.CraftingSlotMap;
import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.module.modules.CraftingModule;
import fr.maxlego08.menu.api.button.Button;
import fr.maxlego08.menu.api.engine.InventoryEngine;
import fr.maxlego08.menu.api.utils.Placeholders;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.Map;

/**
 * zMenu button owning every interactive slot of the Hypixel-style crafting
 * GUI: the 3x3 grid, the result slot and the quick-craft button. Slot
 * dispatching is driven by {@link CraftingSlotMap} constants.
 */
public class ButtonCraft extends Button {

    private final EssentialsPlugin plugin;

    public ButtonCraft(Plugin plugin) {
        this.plugin = (EssentialsPlugin) plugin;
    }

    @Override
    public boolean hasSpecialRender() {
        return true;
    }

    @Override
    public void onInventoryOpen(Player player, InventoryEngine inventory, Placeholders placeholders) {
        inventory.setDisablePlayerInventoryClick(false);
    }

    @Override
    public void onRender(Player player, InventoryEngine inventory) {
        CraftingModule module = module();
        CraftingSession session = module == null ? null : module.getSession(player);
        if (session == null) return;

        for (int slot : CraftingSlotMap.GRID_SLOTS) {
            int gridIndex = CraftingSlotMap.gridIndex(slot);
            ItemStack item = session.getGrid(gridIndex);
            ItemStack display = item == null ? new ItemStack(Material.AIR) : item;
            inventory.addItem(slot, display).setClick(event -> {
                event.setCancelled(true);
                handleGridClick(player, session, gridIndex, event, inventory);
            });
        }

        renderResult(inventory, session);

        if (session.isQuickCraftAllowed()) {
            inventory.addItem(CraftingSlotMap.SLOT_QUICK_CRAFT, CraftingGuiItems.quickCraft(module)).setClick(event -> {
                event.setCancelled(true);
                craftMultiple(player, session, inventory);
            });
        } else {
            inventory.addItem(CraftingSlotMap.SLOT_QUICK_CRAFT, new ItemStack(Material.AIR)).setClick(event -> event.setCancelled(true));
        }

        inventory.addItem(CraftingSlotMap.SLOT_CLOSE, CraftingGuiItems.close(module)).setClick(event -> {
            event.setCancelled(true);
            player.closeInventory();
        });
    }

    private void renderResult(InventoryEngine inventory, CraftingSession session) {
        ItemStack result = session.computeResult();
        ItemStack display = result == null ? new ItemStack(Material.AIR) : result.clone();
        inventory.addItem(CraftingSlotMap.SLOT_RESULT, display);
    }

    private void handleGridClick(Player player, CraftingSession session, int gridIndex, InventoryClickEvent event, InventoryEngine inventory) {
        ItemStack cursor = event.getCursor() == null || event.getCursor().getType().isAir() ? null : event.getCursor().clone();
        ItemStack slotItem = session.getGrid(gridIndex);

        if (cursor == null) {
            session.setGrid(gridIndex, null);
            event.getView().setCursor(slotItem == null ? new ItemStack(Material.AIR) : slotItem);
        } else if (slotItem == null) {
            if (event.getClick() == ClickType.RIGHT) {
                ItemStack one = cursor.clone();
                one.setAmount(1);
                session.setGrid(gridIndex, one);
                cursor.setAmount(cursor.getAmount() - 1);
                event.getView().setCursor(cursor.getAmount() <= 0 ? new ItemStack(Material.AIR) : cursor);
            } else {
                session.setGrid(gridIndex, cursor);
                event.getView().setCursor(new ItemStack(Material.AIR));
            }
        } else if (cursor.isSimilar(slotItem)) {
            int max = slotItem.getMaxStackSize();
            int movable = event.getClick() == ClickType.RIGHT ? 1 : cursor.getAmount();
            int merged = Math.min(max - slotItem.getAmount(), movable);
            if (merged > 0) {
                slotItem.setAmount(slotItem.getAmount() + merged);
                cursor.setAmount(cursor.getAmount() - merged);
                session.setGrid(gridIndex, slotItem);
                event.getView().setCursor(cursor.getAmount() <= 0 ? new ItemStack(Material.AIR) : cursor);
            }
        } else {
            session.setGrid(gridIndex, cursor);
            event.getView().setCursor(slotItem);
        }

        refreshGridSlot(inventory, session, gridIndex);
        refreshResult(player, session, inventory);
    }

    private void handleResultClick(Player player, CraftingSession session, InventoryClickEvent event, InventoryEngine inventory) {
        ItemStack result = session.computeResult();
        if (result == null) return;
        if (event.getClick().isShiftClick()) {
            craftMultiple(player, session, inventory);
        } else {
            craftOne(player, session, inventory);
        }
    }

    private void craftOne(Player player, CraftingSession session, InventoryEngine inventory) {
        ItemStack result = session.computeResult();
        if (result == null) return;

        Map<Integer, ItemStack> overflow = player.getInventory().addItem(result.clone());
        for (ItemStack drop : overflow.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
        }
        consumeGrid(session, 1);
        refreshResult(player, session, inventory);
    }

    private void craftMultiple(Player player, CraftingSession session, InventoryEngine inventory) {
        while (true) {
            ItemStack result = session.computeResult();
            if (result == null) break;

            int max = session.maxCraftable();
            int craftable = Math.min(max, result.getMaxStackSize());
            ItemStack crafted = result.clone();
            crafted.setAmount(craftable);

            Map<Integer, ItemStack> overflow = player.getInventory().addItem(crafted);
            for (ItemStack drop : overflow.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
            consumeGrid(session, craftable);
            if (max <= 1) break;
        }
        refreshResult(player, session, inventory);
    }

    private void consumeGrid(CraftingSession session, int amount) {
        for (int i = 0; i < 9; i++) {
            ItemStack item = session.getGrid(i);
            if (item != null) {
                item.setAmount(item.getAmount() - amount);
                session.setGrid(i, item);
            }
        }
    }

    private void refreshGridSlot(InventoryEngine inventory, CraftingSession session, int gridIndex) {
        ItemStack item = session.getGrid(gridIndex);
        int slot = CraftingSlotMap.GRID_SLOTS[gridIndex];
        inventory.addItem(slot, item == null ? new ItemStack(Material.AIR) : item).setClick(event -> {
            event.setCancelled(true);
            Player player = (Player) event.getWhoClicked();
            handleGridClick(player, session, gridIndex, event, inventory);
        });
    }

    private void refreshResult(Player player, CraftingSession session, InventoryEngine inventory) {
        ItemStack result = session.computeResult();
        ItemStack display = result == null ? new ItemStack(Material.AIR) : result.clone();
        inventory.addItem(CraftingSlotMap.SLOT_RESULT, display).setClick(event -> {
            event.setCancelled(true);
            handleResultClick(player, session, event, inventory);
        });
    }

    private CraftingModule module() {
        return this.plugin.getModuleManager().getModule(CraftingModule.class);
    }
}
