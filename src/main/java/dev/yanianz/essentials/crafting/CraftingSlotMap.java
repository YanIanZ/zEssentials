package dev.yanianz.essentials.crafting;

public final class CraftingSlotMap {
    public static final int INVENTORY_SIZE = 54;
    public static final int GRID_START = 0;
    public static final int GRID_SIZE = 9;
    public static final int SLOT_RESULT = 13;
    public static final int SLOT_QUICK_CRAFT = 22;
    public static final int SLOT_CLOSE = 49;
    public static final int PLAYER_INV_START = 27;
    public static final int PLAYER_INV_END = 53;
    public static final int PLAYER_INV_SLOTS = 27;
    private CraftingSlotMap() {
    }

    public static boolean isGridSlot(int slot) {
        return slot >= GRID_START && slot < GRID_START + GRID_SIZE;
    }

    public static boolean isResultSlot(int slot) {
        return slot == SLOT_RESULT;
    }

    public static boolean isQuickCraftSlot(int slot) {
        return slot == SLOT_QUICK_CRAFT;
    }

    public static boolean isCloseSlot(int slot) {
        return slot == SLOT_CLOSE;
    }

    public static boolean isPlayerInvSlot(int slot) {
        return slot >= PLAYER_INV_START && slot <= PLAYER_INV_END;
    }

    public static boolean isFillerSlot(int slot) {
        return !isGridSlot(slot) && !isResultSlot(slot) && !isQuickCraftSlot(slot)
                && !isCloseSlot(slot) && !isPlayerInvSlot(slot);
    }
}
