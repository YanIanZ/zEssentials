package dev.yanianz.essentials.crafting;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public class CraftingHolder implements InventoryHolder {

    private final UUID playerId;
    private final boolean quickCraftAllowed;
    private Inventory inventory;

    public CraftingHolder(UUID playerId, boolean quickCraftAllowed) {
        this.playerId = playerId;
        this.quickCraftAllowed = quickCraftAllowed;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public boolean isQuickCraftAllowed() {
        return quickCraftAllowed;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }
}
