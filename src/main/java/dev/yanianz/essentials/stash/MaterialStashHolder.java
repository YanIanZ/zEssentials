package dev.yanianz.essentials.stash;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public class MaterialStashHolder implements InventoryHolder {

    private final UUID playerId;
    private final MaterialStashData data;
    private final boolean readOnly;
    private Inventory inventory;

    public MaterialStashHolder(UUID playerId, MaterialStashData data, boolean readOnly) {
        this.playerId = playerId;
        this.data = data;
        this.readOnly = readOnly;
    }

    public UUID getPlayerId() { return playerId; }
    public MaterialStashData getData() { return data; }
    public boolean isReadOnly() { return readOnly; }
    void setInventory(Inventory inventory) { this.inventory = inventory; }

    @Override
    public Inventory getInventory() { return inventory; }
}