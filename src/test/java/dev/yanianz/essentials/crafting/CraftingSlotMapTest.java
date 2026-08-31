package dev.yanianz.essentials.crafting;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CraftingSlotMapTest {

    @Test
    @DisplayName("Grid slots are the 3x3 centered grid (10-12, 19-21, 28-30)")
    void testGridSlots() {
        for (int slot : new int[]{10, 11, 12, 19, 20, 21, 28, 29, 30}) {
            assertTrue(CraftingSlotMap.isGridSlot(slot), "Slot " + slot + " should be a grid slot");
        }
        assertFalse(CraftingSlotMap.isGridSlot(9));
        assertFalse(CraftingSlotMap.isGridSlot(13));
        assertFalse(CraftingSlotMap.isGridSlot(0));
    }

    @Test
    @DisplayName("Result slot is 15")
    void testResultSlot() {
        assertTrue(CraftingSlotMap.isResultSlot(15));
        assertFalse(CraftingSlotMap.isResultSlot(0));
        assertFalse(CraftingSlotMap.isResultSlot(10));
    }

    @Test
    @DisplayName("Quick craft slots are 16, 25, 34")
    void testQuickCraftSlot() {
        assertTrue(CraftingSlotMap.isQuickCraftSlot(16));
        assertTrue(CraftingSlotMap.isQuickCraftSlot(25));
        assertTrue(CraftingSlotMap.isQuickCraftSlot(34));
        assertFalse(CraftingSlotMap.isQuickCraftSlot(15));
    }

    @Test
    @DisplayName("Close slot is 49")
    void testCloseSlot() {
        assertTrue(CraftingSlotMap.isCloseSlot(49));
        assertFalse(CraftingSlotMap.isCloseSlot(0));
    }

    @Test
    @DisplayName("Filler slots are everything else")
    void testFillerSlots() {
        assertTrue(CraftingSlotMap.isFillerSlot(0));
        assertTrue(CraftingSlotMap.isFillerSlot(9));
        assertTrue(CraftingSlotMap.isFillerSlot(13));
        assertTrue(CraftingSlotMap.isFillerSlot(17));
        assertTrue(CraftingSlotMap.isFillerSlot(53));
        assertFalse(CraftingSlotMap.isFillerSlot(10));
        assertFalse(CraftingSlotMap.isFillerSlot(15));
        assertFalse(CraftingSlotMap.isFillerSlot(16));
        assertFalse(CraftingSlotMap.isFillerSlot(49));
    }
}
