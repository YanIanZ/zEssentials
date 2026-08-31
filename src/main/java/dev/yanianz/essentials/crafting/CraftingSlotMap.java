package dev.yanianz.essentials.crafting;

public final class CraftingSlotMap {
    public static final int INVENTORY_SIZE = 54;

    public static final int[] GRID_SLOTS = {10, 11, 12, 19, 20, 21, 28, 29, 30};
    public static final int SLOT_RESULT = 15;
    public static final int[] QUICK_CRAFT_SLOTS = {16, 25, 34};
    public static final int SLOT_QUICK_CRAFT = 16;
    public static final int SLOT_CLOSE = 49;

    private CraftingSlotMap() {
    }

    public static boolean isGridSlot(int slot) {
        for (int g : GRID_SLOTS) if (slot == g) return true;
        return false;
    }

    public static int gridIndex(int slot) {
        for (int i = 0; i < GRID_SLOTS.length; i++) if (GRID_SLOTS[i] == slot) return i;
        return -1;
    }

    public static boolean isResultSlot(int slot) {
        return slot == SLOT_RESULT;
    }

    public static boolean isQuickCraftSlot(int slot) {
        for (int q : QUICK_CRAFT_SLOTS) if (slot == q) return true;
        return false;
    }

    public static boolean isCloseSlot(int slot) {
        return slot == SLOT_CLOSE;
    }

    public static boolean isFillerSlot(int slot) {
        return !isGridSlot(slot) && !isResultSlot(slot) && !isQuickCraftSlot(slot) && !isCloseSlot(slot);
    }
}
