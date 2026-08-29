package dev.yanianz.essentials.enderchest;

import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class EnderChestDataTest {

    private final UUID playerId = UUID.randomUUID();

    @Test
    @DisplayName("New data has correct page count and all-null contents")
    void testNewData() {
        EnderChestData data = new EnderChestData(playerId, 3);
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
    void testSetGetPage0Slot0() {
        EnderChestData data = new EnderChestData(playerId, 1);
        ItemStack item = mock(ItemStack.class);
        data.setContent(0, 0, item);
        assertSame(item, data.getContent(0, 0));
    }

    @Test
    @DisplayName("Set and get content on page 2 slot 44")
    void testSetGetPage2Slot44() {
        EnderChestData data = new EnderChestData(playerId, 3);
        ItemStack item = mock(ItemStack.class);
        data.setContent(2, 44, item);
        assertSame(item, data.getContent(2, 44));
        assertNull(data.getContent(2, 43));
    }

    @Test
    @DisplayName("getPageContents returns 45 items for one page")
    void testGetPageContents() {
        EnderChestData data = new EnderChestData(playerId, 2);
        ItemStack item = mock(ItemStack.class);
        data.setContent(0, 0, item);
        data.setContent(0, 44, item);
        List<ItemStack> page = data.getPageContents(0);
        assertEquals(45, page.size());
        assertSame(item, page.get(0));
        assertSame(item, page.get(44));
        assertNull(page.get(1));
    }

    @Test
    @DisplayName("setPageContents replaces entire page")
    void testSetPageContents() {
        EnderChestData data = new EnderChestData(playerId, 1);
        ItemStack item = mock(ItemStack.class);
        ItemStack[] items = new ItemStack[45];
        items[0] = item;
        items[44] = item;
        data.setPageContents(0, Arrays.asList(items));
        assertSame(item, data.getContent(0, 0));
        assertSame(item, data.getContent(0, 44));
        assertNull(data.getContent(0, 1));
    }

    @Test
    @DisplayName("Resize preserves existing items within new bounds")
    void testResizeShrink() {
        EnderChestData data = new EnderChestData(playerId, 3);
        ItemStack item = mock(ItemStack.class);
        data.setContent(0, 0, item);
        data.setContent(1, 0, item);
        data.resize(1);
        assertEquals(1, data.getPages());
        assertSame(item, data.getContent(0, 0));
    }

    @Test
    @DisplayName("Resize grow adds null slots")
    void testResizeGrow() {
        EnderChestData data = new EnderChestData(playerId, 1);
        ItemStack item = mock(ItemStack.class);
        data.setContent(0, 0, item);
        data.resize(3);
        assertEquals(3, data.getPages());
        assertSame(item, data.getContent(0, 0));
        assertNull(data.getContent(1, 0));
        assertNull(data.getContent(2, 0));
    }

    @Test
    @DisplayName("Out of bounds page throws IndexOutOfBoundsException")
    void testOutOfBoundsPage() {
        EnderChestData data = new EnderChestData(playerId, 2);
        assertThrows(IndexOutOfBoundsException.class, () -> data.getContent(2, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> data.getContent(-1, 0));
    }

    @Test
    @DisplayName("Out of bounds slot throws IndexOutOfBoundsException")
    void testOutOfBoundsSlot() {
        EnderChestData data = new EnderChestData(playerId, 1);
        assertThrows(IndexOutOfBoundsException.class, () -> data.getContent(0, 45));
        assertThrows(IndexOutOfBoundsException.class, () -> data.getContent(0, -1));
    }

    @Test
    @DisplayName("setContent null clears a slot")
    void testSetNullClears() {
        EnderChestData data = new EnderChestData(playerId, 1);
        ItemStack item = mock(ItemStack.class);
        data.setContent(0, 5, item);
        assertSame(item, data.getContent(0, 5));
        data.setContent(0, 5, null);
        assertNull(data.getContent(0, 5));
    }
}
