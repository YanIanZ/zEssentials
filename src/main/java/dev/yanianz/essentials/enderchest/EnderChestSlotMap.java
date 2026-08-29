package dev.yanianz.essentials.enderchest;

public final class EnderChestSlotMap {

    private EnderChestSlotMap() {}

    public static final int CONTENT_SLOTS = 45;
    public static final int NAV_ROW_START = 45;
    public static final int INVENTORY_SIZE = 54;
    public static final int SLOT_PREV = 45;
    public static final int SLOT_CLOSE = 49;
    public static final int SLOT_NEXT = 53;

    public static int toFlatIndex(int page, int slot) {
        return page * CONTENT_SLOTS + slot;
    }

    public static int toPage(int flatIndex) {
        return flatIndex / CONTENT_SLOTS;
    }

    public static int toSlot(int flatIndex) {
        return flatIndex % CONTENT_SLOTS;
    }

    public static int totalSize(int pages) {
        return pages * CONTENT_SLOTS;
    }

    public static boolean isContentSlot(int slot) {
        return slot >= 0 && slot < CONTENT_SLOTS;
    }

    public static boolean isNavSlot(int slot) {
        return slot >= NAV_ROW_START && slot < INVENTORY_SIZE;
    }
}
