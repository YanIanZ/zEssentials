package dev.yanianz.essentials.stash;

import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ItemStashDataTest {

    private final UUID playerId = UUID.randomUUID();

    @Test
    @DisplayName("New data has correct page count and all-null contents")
    void testNewData() {
        ItemStashData data = new ItemStashData(playerId, 3);
        assertEquals(3, data.getPages());
        assertEquals(playerId, data.getPlayerId());
        for (int page = 0; page < 3; page++) {
            for (int slot = 0; slot < 45; slot++) {
                assertNull(data.getContent(page, slot));
            }
        }
    }

    @Test
    @DisplayName("Set and get content on page 0 slot 0")
    void testSetGet() {
        ItemStashData data = new ItemStashData(playerId, 1);
        ItemStack item = mock(ItemStack.class);
        data.setContent(0, 0, item);
        assertSame(item, data.getContent(0, 0));
    }

    @Test
    @DisplayName("addToFirstAvailable finds the first empty slot")
    void testAddToFirstAvailable() {
        ItemStashData data = new ItemStashData(playerId, 2);
        ItemStack item1 = mock(ItemStack.class);
        ItemStack item2 = mock(ItemStack.class);
        assertEquals(0, data.addToFirstAvailable(item1));
        assertEquals(1, data.addToFirstAvailable(item2));
        assertSame(item1, data.getContent(0, 0));
        assertSame(item2, data.getContent(0, 1));
    }

    @Test
    @DisplayName("Resize grow adds null slots, preserves items")
    void testResizeGrow() {
        ItemStashData data = new ItemStashData(playerId, 1);
        ItemStack item = mock(ItemStack.class);
        data.setContent(0, 0, item);
        data.resize(3);
        assertEquals(3, data.getPages());
        assertSame(item, data.getContent(0, 0));
        assertNull(data.getContent(1, 0));
    }

    @Test
    @DisplayName("Out of bounds throws IndexOutOfBoundsException")
    void testOutOfBounds() {
        ItemStashData data = new ItemStashData(playerId, 1);
        assertThrows(IndexOutOfBoundsException.class, () -> data.getContent(5, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> data.getContent(0, 45));
    }
}