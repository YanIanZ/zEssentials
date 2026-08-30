package dev.yanianz.essentials.stash;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public class ItemStashHolder implements InventoryHolder {

    private final UUID playerId;
    private final ItemStashData data;
    private final int pages;
    private final boolean readOnly;
    private int currentPage;
    private Inventory inventory;

    public ItemStashHolder(UUID playerId, ItemStashData data, int currentPage, int pages, boolean readOnly) {
        this.playerId = playerId;
        this.data = data;
        this.currentPage = currentPage;
        this.pages = pages;
        this.readOnly = readOnly;
    }

    public UUID getPlayerId() { return playerId; }
    public ItemStashData getData() { return data; }
    public int getCurrentPage() { return currentPage; }
    public void setCurrentPage(int page) { this.currentPage = page; }
    public int getPages() { return pages; }
    public boolean isReadOnly() { return readOnly; }
    void setInventory(Inventory inventory) { this.inventory = inventory; }

    @Override
    public Inventory getInventory() { return inventory; }
}