package dev.yanianz.essentials.crafting;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CraftingSlotMapTest {

    @Test
    @DisplayName("Grid slots are 0-8")
    void testGridSlots() {
        for (int i = 0; i < 9; i++) assertTrue(CraftingSlotMap.isGridSlot(i));
        assertFalse(CraftingSlotMap.isGridSlot(9));
    }

    @Test
    @DisplayName("Result slot is 13")
    void testResultSlot() {
        assertTrue(CraftingSlotMap.isResultSlot(13));
        assertFalse(CraftingSlotMap.isResultSlot(0));
    }

    @Test
    @DisplayName("Quick craft slot is 22")
    void testQuickCraftSlot() {
        assertTrue(CraftingSlotMap.isQuickCraftSlot(22));
    }

    @Test
    @DisplayName("Close slot is 49")
    void testCloseSlot() {
        assertTrue(CraftingSlotMap.isCloseSlot(49));
    }

    @Test
    @DisplayName("Player inventory slots are 27-53")
    void testPlayerInvSlots() {
        for (int i = 27; i < 54; i++) assertTrue(CraftingSlotMap.isPlayerInvSlot(i));
        assertFalse(CraftingSlotMap.isPlayerInvSlot(26));
    }

    @Test
    @DisplayName("Filler slots are everything else")
    void testFillerSlots() {
        assertTrue(CraftingSlotMap.isFillerSlot(9));
        assertTrue(CraftingSlotMap.isFillerSlot(14));
        assertTrue(CraftingSlotMap.isFillerSlot(26));
        assertFalse(CraftingSlotMap.isFillerSlot(0));
        assertFalse(CraftingSlotMap.isFillerSlot(27));
    }
}
