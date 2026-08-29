package dev.yanianz.essentials.enderchest;

import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EnderChestSerializerTest {

    private final UUID playerId = UUID.randomUUID();

    private static List<Byte> byteList(byte[] arr) {
        List<Byte> list = new ArrayList<>(arr.length);
        for (byte b : arr) list.add(b);
        return list;
    }

    @Test
    @DisplayName("Round-trip: serialize then deserialize preserves items and slots")
    void testRoundTrip() {
        EnderChestData data = new EnderChestData(playerId, 2);
        byte[] item1Bytes = {1, 2, 3};
        byte[] item2Bytes = {4, 5};

        ItemStack item1 = mock(ItemStack.class);
        ItemStack item2 = mock(ItemStack.class);
        when(item1.serializeAsBytes()).thenReturn(item1Bytes);
        when(item2.serializeAsBytes()).thenReturn(item2Bytes);

        data.setContent(0, 0, item1);
        data.setContent(1, 5, item2);

        Map<List<Byte>, ItemStack> registry = new HashMap<>();
        registry.put(byteList(item1Bytes), item1);
        registry.put(byteList(item2Bytes), item2);

        EnderChestSerializer serializer = new EnderChestSerializer() {
            @Override
            protected byte[] encodeItem(ItemStack item) {
                return item.serializeAsBytes();
            }

            @Override
            protected ItemStack decodeItem(byte[] bytes) {
                return registry.get(byteList(bytes));
            }
        };

        String json = serializer.serializeData(data);

        assertTrue(json.contains("\"player_uuid\": \"" + playerId + "\""));
        assertTrue(json.contains("\"pages\": 2"));
        assertTrue(json.contains(Base64.getEncoder().encodeToString(item1Bytes)));
        assertTrue(json.contains(Base64.getEncoder().encodeToString(item2Bytes)));

        EnderChestData loaded = serializer.deserializeData(json, playerId);

        assertEquals(2, loaded.getPages());
        assertSame(item1, loaded.getContent(0, 0));
        assertSame(item2, loaded.getContent(1, 5));
        assertNull(loaded.getContent(0, 1));
        assertNull(loaded.getContent(1, 0));
    }

    @Test
    @DisplayName("Round-trip: empty data (all nulls) survives serialization")
    void testEmptyRoundTrip() {
        EnderChestData data = new EnderChestData(playerId, 1);
        String json = EnderChestSerializer.serialize(data);
        assertNotNull(json);

        EnderChestData loaded = EnderChestSerializer.deserialize(json, playerId);
        assertEquals(1, loaded.getPages());
        assertNull(loaded.getContent(0, 0));
        assertNull(loaded.getContent(0, 44));
    }

    @Test
    @DisplayName("deserialize returns null for null JSON")
    void testNullJson() {
        assertNull(EnderChestSerializer.deserialize("null", playerId));
    }

    @Test
    @DisplayName("deserialize clamps non-positive pages to 1")
    void testClampsPages() {
        String json = "{\"player_uuid\":\"" + playerId + "\",\"pages\":0,\"contents\":null}";
        EnderChestData loaded = EnderChestSerializer.deserialize(json, playerId);
        assertEquals(1, loaded.getPages());
    }

    @Test
    @DisplayName("Contents are stored as Base64 strings (not legacy serialize maps)")
    void testUsesBase64Bytes() {
        EnderChestData data = new EnderChestData(playerId, 1);
        ItemStack item = mock(ItemStack.class);
        byte[] bytes = {7, 7, 7, 7};
        when(item.serializeAsBytes()).thenReturn(bytes);
        data.setContent(0, 10, item);

        String json = EnderChestSerializer.serialize(data);
        String expected = Base64.getEncoder().encodeToString(bytes);
        assertTrue(json.contains("\"" + expected + "\""));
        assertFalse(json.contains("schema_version"));
    }
}
