package dev.yanianz.essentials.disguise;

import com.google.gson.Gson;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DisguiseDataTest {

    private final Gson gson = new Gson();

    @Test
    @DisplayName("isFullDisguise returns true when name and texture are set")
    void testFullDisguise() {
        DisguiseData data = new DisguiseData();
        data.setPlayerId(UUID.randomUUID());
        data.setDisguiseName("Steve");
        data.setTextureValue("eyJ0ZXh0dXJlcyI6e319");
        data.setActive(true);
        assertTrue(data.isFullDisguise());
        assertFalse(data.isNameOnly());
        assertTrue(data.hasSkin());
    }

    @Test
    @DisplayName("isNameOnly returns true when name set but no texture")
    void testNameOnly() {
        DisguiseData data = new DisguiseData();
        data.setDisguiseName("&cSteve");
        data.setTextureValue(null);
        assertTrue(data.isNameOnly());
        assertFalse(data.isFullDisguise());
        assertFalse(data.hasSkin());
    }

    @Test
    @DisplayName("hasSkin returns true when texture set without name")
    void testSkinOnly() {
        DisguiseData data = new DisguiseData();
        data.setDisguiseName(null);
        data.setTextureValue("eyJ0ZXh0dXJlcyI6e319");
        data.setTextureSignature("sig123");
        assertTrue(data.hasSkin());
        assertFalse(data.isFullDisguise());
        assertFalse(data.isNameOnly());
    }

    @Test
    @DisplayName("Gson serialization round-trip preserves all fields")
    void testSerializationRoundTrip() {
        DisguiseData original = new DisguiseData();
        original.setPlayerId(UUID.fromString("12345678-1234-1234-1234-123456789012"));
        original.setDisguiseName("Notch");
        original.setTextureValue("texture-value");
        original.setTextureSignature("signature");
        original.setAppliedAt(1234567890L);
        original.setActive(true);

        String json = gson.toJson(original);
        DisguiseData restored = gson.fromJson(json, DisguiseData.class);

        assertEquals(original.getPlayerId(), restored.getPlayerId());
        assertEquals(original.getDisguiseName(), restored.getDisguiseName());
        assertEquals(original.getTextureValue(), restored.getTextureValue());
        assertEquals(original.getTextureSignature(), restored.getTextureSignature());
        assertEquals(original.getAppliedAt(), restored.getAppliedAt());
        assertTrue(restored.isActive());
    }

    @Test
    @DisplayName("Empty data has no disguise")
    void testEmpty() {
        DisguiseData data = new DisguiseData();
        assertFalse(data.isFullDisguise());
        assertFalse(data.isNameOnly());
        assertFalse(data.hasSkin());
        assertFalse(data.isActive());
    }
}
