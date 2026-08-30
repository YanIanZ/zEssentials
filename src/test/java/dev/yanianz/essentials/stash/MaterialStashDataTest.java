package dev.yanianz.essentials.stash;

import org.bukkit.Material;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MaterialStashDataTest {

    private final UUID playerId = UUID.randomUUID();

    @Test
    @DisplayName("New data is empty")
    void testNewData() {
        MaterialStashData data = new MaterialStashData(playerId);
        assertEquals(0, data.get(Material.COBBLESTONE));
        assertEquals(0, data.totalItems());
    }

    @Test
    @DisplayName("Add and get material quantity")
    void testAddGet() {
        MaterialStashData data = new MaterialStashData(playerId);
        data.add(Material.COBBLESTONE, 64);
        data.add(Material.COBBLESTONE, 64);
        assertEquals(128, data.get(Material.COBBLESTONE));
    }

    @Test
    @DisplayName("Remove decrements quantity")
    void testRemove() {
        MaterialStashData data = new MaterialStashData(playerId);
        data.add(Material.DIRT, 100);
        assertTrue(data.remove(Material.DIRT, 30));
        assertEquals(70, data.get(Material.DIRT));
    }

    @Test
    @DisplayName("Remove insufficient returns false")
    void testRemoveInsufficient() {
        MaterialStashData data = new MaterialStashData(playerId);
        data.add(Material.DIRT, 10);
        assertFalse(data.remove(Material.DIRT, 20));
        assertEquals(10, data.get(Material.DIRT));
    }

    @Test
    @DisplayName("Set to zero removes material")
    void testSetZero() {
        MaterialStashData data = new MaterialStashData(playerId);
        data.set(Material.STONE, 50);
        assertEquals(50, data.get(Material.STONE));
        data.set(Material.STONE, 0);
        assertEquals(0, data.get(Material.STONE));
        assertFalse(data.getQuantities().containsKey(Material.STONE));
    }

    @Test
    @DisplayName("Total items sums all quantities")
    void testTotalItems() {
        MaterialStashData data = new MaterialStashData(playerId);
        data.add(Material.COBBLESTONE, 1000);
        data.add(Material.DIRT, 500);
        data.add(Material.WHEAT, 100);
        assertEquals(1600, data.totalItems());
    }
}