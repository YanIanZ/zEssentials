package dev.yanianz.essentials.enderchest;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class EnderChestData {

    private final UUID playerId;
    private ItemStack[] contents;
    private int pages;

    public EnderChestData(UUID playerId, int pages) {
        this.playerId = playerId;
        this.pages = Math.max(1, pages);
        this.contents = new ItemStack[EnderChestSlotMap.totalSize(this.pages)];
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public int getPages() {
        return pages;
    }

    public ItemStack getContent(int page, int slot) {
        checkBounds(page, slot);
        return contents[EnderChestSlotMap.toFlatIndex(page, slot)];
    }

    public void setContent(int page, int slot, ItemStack item) {
        checkBounds(page, slot);
        contents[EnderChestSlotMap.toFlatIndex(page, slot)] = item;
    }

    public List<ItemStack> getPageContents(int page) {
        if (page < 0 || page >= pages) throw new IndexOutOfBoundsException("page " + page);
        List<ItemStack> result = new ArrayList<>(EnderChestSlotMap.CONTENT_SLOTS);
        for (int slot = 0; slot < EnderChestSlotMap.CONTENT_SLOTS; slot++) {
            result.add(contents[EnderChestSlotMap.toFlatIndex(page, slot)]);
        }
        return result;
    }

    public void setPageContents(int page, List<ItemStack> items) {
        if (page < 0 || page >= pages) throw new IndexOutOfBoundsException("page " + page);
        for (int slot = 0; slot < EnderChestSlotMap.CONTENT_SLOTS; slot++) {
            contents[EnderChestSlotMap.toFlatIndex(page, slot)] =
                    slot < items.size() ? items.get(slot) : null;
        }
    }

    public void resize(int newPages) {
        newPages = Math.max(1, newPages);
        ItemStack[] resized = new ItemStack[EnderChestSlotMap.totalSize(newPages)];
        int copyLen = Math.min(contents.length, resized.length);
        System.arraycopy(contents, 0, resized, 0, copyLen);
        this.contents = resized;
        this.pages = newPages;
    }

    ItemStack[] rawContents() {
        return contents;
    }

    void setRawContents(ItemStack[] contents, int pages) {
        this.contents = contents;
        this.pages = pages;
    }

    private void checkBounds(int page, int slot) {
        if (page < 0 || page >= pages) throw new IndexOutOfBoundsException("page " + page);
        if (slot < 0 || slot >= EnderChestSlotMap.CONTENT_SLOTS)
            throw new IndexOutOfBoundsException("slot " + slot);
    }
}
