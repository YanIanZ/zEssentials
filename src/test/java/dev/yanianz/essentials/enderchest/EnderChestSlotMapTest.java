package dev.yanianz.essentials.enderchest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EnderChestSlotMapTest {

    @Test
    @DisplayName("Constants are correct")
    void testConstants() {
        assertEquals(45, EnderChestSlotMap.CONTENT_SLOTS);
        assertEquals(45, EnderChestSlotMap.NAV_ROW_START);
        assertEquals(54, EnderChestSlotMap.INVENTORY_SIZE);
    }

    @Test
    @DisplayName("Page 0 slot 0 maps to flat index 0")
    void testPage0Slot0() {
        assertEquals(0, EnderChestSlotMap.toFlatIndex(0, 0));
    }

    @Test
    @DisplayName("Page 0 slot 44 maps to flat index 44")
    void testPage0Slot44() {
        assertEquals(44, EnderChestSlotMap.toFlatIndex(0, 44));
    }

    @Test
    @DisplayName("Page 1 slot 0 maps to flat index 45")
    void testPage1Slot0() {
        assertEquals(45, EnderChestSlotMap.toFlatIndex(1, 0));
    }

    @Test
    @DisplayName("Page 2 slot 44 maps to flat index 134")
    void testPage2Slot44() {
        assertEquals(134, EnderChestSlotMap.toFlatIndex(2, 44));
    }

    @Test
    @DisplayName("Reverse: flat index 0 → page 0, slot 0")
    void testReverse0() {
        assertEquals(0, EnderChestSlotMap.toPage(0));
        assertEquals(0, EnderChestSlotMap.toSlot(0));
    }

    @Test
    @DisplayName("Reverse: flat index 45 → page 1, slot 0")
    void testReverse45() {
        assertEquals(1, EnderChestSlotMap.toPage(45));
        assertEquals(0, EnderChestSlotMap.toSlot(45));
    }

    @Test
    @DisplayName("Reverse: flat index 134 → page 2, slot 44")
    void testReverse134() {
        assertEquals(2, EnderChestSlotMap.toPage(134));
        assertEquals(44, EnderChestSlotMap.toSlot(134));
    }

    @Test
    @DisplayName("totalSize for 1 page = 45, 2 pages = 90, 3 pages = 135")
    void testTotalSize() {
        assertEquals(45, EnderChestSlotMap.totalSize(1));
        assertEquals(90, EnderChestSlotMap.totalSize(2));
        assertEquals(135, EnderChestSlotMap.totalSize(3));
    }

    @Test
    @DisplayName("isContentSlot returns true for 0-44, false for 45-53")
    void testIsContentSlot() {
        for (int i = 0; i < 45; i++) assertTrue(EnderChestSlotMap.isContentSlot(i));
        for (int i = 45; i < 54; i++) assertFalse(EnderChestSlotMap.isContentSlot(i));
    }

    @Test
    @DisplayName("isNavSlot returns false for 0-44, true for 45-53")
    void testIsNavSlot() {
        for (int i = 0; i < 45; i++) assertFalse(EnderChestSlotMap.isNavSlot(i));
        for (int i = 45; i < 54; i++) assertTrue(EnderChestSlotMap.isNavSlot(i));
    }

    @Test
    @DisplayName("Nav button slots: prev=45, close=49, next=53")
    void testNavButtonSlots() {
        assertEquals(45, EnderChestSlotMap.SLOT_PREV);
        assertEquals(49, EnderChestSlotMap.SLOT_CLOSE);
        assertEquals(53, EnderChestSlotMap.SLOT_NEXT);
    }
}
