package dev.yanianz.essentials.enderchest;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public class EnderChestHolder implements InventoryHolder {

    private final UUID playerId;
    private final EnderChestData data;
    private final int pages;
    private final boolean readOnly;
    private int currentPage;
    private Inventory inventory;

    public EnderChestHolder(UUID playerId, EnderChestData data, int currentPage, int pages, boolean readOnly) {
        this.playerId = playerId;
        this.data = data;
        this.currentPage = currentPage;
        this.pages = pages;
        this.readOnly = readOnly;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public EnderChestData getData() {
        return data;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int page) {
        this.currentPage = page;
    }

    public int getPages() {
        return pages;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }
}
