package fr.maxlego08.essentials.buttons.enderchest;

import dev.yanianz.essentials.enderchest.EnderChestModule;
import dev.yanianz.essentials.enderchest.EnderChestSession;
import fr.maxlego08.essentials.ZEssentialsPlugin;
import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.menu.api.button.PaginateButton;
import fr.maxlego08.menu.api.engine.InventoryEngine;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/**
 * zMenu pagination button rendering the ender chest content. The flat
 * content list (visiblePages x 45 items) is paginated over the content
 * slots; every interaction is written through to {@code EnderChestData}.
 */
public class ButtonEnderChestContent extends PaginateButton {

    private final EssentialsPlugin plugin;
    private static final int SLOTS_PER_PAGE = 45;

    public ButtonEnderChestContent(Plugin plugin) {
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
    public void onInventoryOpen(Player player, InventoryEngine inventory, fr.maxlego08.menu.api.utils.Placeholders placeholders) {
        EnderChestSession session = session(player);
        inventory.setDisablePlayerInventoryClick(session == null || session.isReadOnly());
    }

    @Override
    public void onRender(Player player, InventoryEngine inventory) {
        EnderChestSession session = session(player);
        if (session == null) return;

        int page = inventory.getPage();
        int slotIndex = 0;
        for (int slot : this.slots) {
            int flatIndex = page * SLOTS_PER_PAGE + slotIndex;
            slotIndex++;

            ItemStack item = flatItem(session, flatIndex);
            ItemStack display = item == null ? new ItemStack(Material.AIR) : item;

            if (session.isReadOnly()) {
                inventory.addItem(slot, display).setClick(event -> event.setCancelled(true));
                continue;
            }

            final int fFlatIndex = flatIndex;
            final int fSlot = slot;
            inventory.addItem(slot, display).setClick(event -> {
                event.setCancelled(true);
                handleContentClick(player, session, fFlatIndex, fSlot, event, inventory);
            });
        }
    }

    private ItemStack flatItem(EnderChestSession session, int flatIndex) {
        int page = flatIndex / SLOTS_PER_PAGE;
        int slot = flatIndex % SLOTS_PER_PAGE;
        return session.getData().getContent(page, slot);
    }

    private void handleContentClick(Player player, EnderChestSession session, int flatIndex, int slot,
                                    org.bukkit.event.inventory.InventoryClickEvent event, InventoryEngine inventory) {
        int page = flatIndex / SLOTS_PER_PAGE;
        int contentSlot = flatIndex % SLOTS_PER_PAGE;

        ItemStack cursor = event.getCursor() == null || event.getCursor().getType().isAir() ? null : event.getCursor().clone();
        ItemStack slotItem = session.getData().getContent(page, contentSlot);

        if (cursor == null) {
            session.getData().setContent(page, contentSlot, null);
            event.getView().setCursor(slotItem == null ? new ItemStack(Material.AIR) : slotItem);
        } else if (slotItem == null) {
            if (event.getClick() == ClickType.RIGHT) {
                ItemStack one = cursor.clone();
                one.setAmount(1);
                session.getData().setContent(page, contentSlot, one);
                cursor.setAmount(cursor.getAmount() - 1);
                event.getView().setCursor(cursor.getAmount() <= 0 ? new ItemStack(Material.AIR) : cursor);
            } else {
                session.getData().setContent(page, contentSlot, cursor);
                event.getView().setCursor(new ItemStack(Material.AIR));
            }
        } else if (cursor.isSimilar(slotItem)) {
            int max = slotItem.getMaxStackSize();
            int movable = event.getClick() == ClickType.RIGHT ? 1 : cursor.getAmount();
            int merged = Math.min(max - slotItem.getAmount(), movable);
            if (merged > 0) {
                slotItem.setAmount(slotItem.getAmount() + merged);
                cursor.setAmount(cursor.getAmount() - merged);
                session.getData().setContent(page, contentSlot, slotItem);
                event.getView().setCursor(cursor.getAmount() <= 0 ? new ItemStack(Material.AIR) : cursor);
            }
        } else {
            session.getData().setContent(page, contentSlot, cursor);
            event.getView().setCursor(slotItem);
        }

        persist(player, session);

        ItemStack updated = session.getData().getContent(page, contentSlot);
        inventory.addItem(slot, updated == null ? new ItemStack(Material.AIR) : updated).setClick(e -> {
            e.setCancelled(true);
            handleContentClick(player, session, flatIndex, slot, e, inventory);
        });
    }

    private void persist(Player player, EnderChestSession session) {
        EnderChestModule module = module();
        if (module != null) module.saveData(session.getData().getPlayerId());
    }

    @Override
    public int getPaginationSize(Player player) {
        EnderChestSession session = session(player);
        return session == null ? 0 : session.getVisiblePages() * SLOTS_PER_PAGE;
    }
}
