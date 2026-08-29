package fr.maxlego08.essentials.api.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SafeLocationTest {

    @Test
    @DisplayName("Constructor sets all fields correctly")
    void testConstructorAndGetters() {
        SafeLocation location = new SafeLocation("world_nether", 123.45, 64.0, -789.12, 180.0f, -45.0f);

        assertEquals("world_nether", location.getWorld());
        assertEquals(123.45, location.getX(), 0.0001);
        assertEquals(64.0, location.getY(), 0.0001);
        assertEquals(-789.12, location.getZ(), 0.0001);
        assertEquals(180.0f, location.getYaw(), 0.0001f);
        assertEquals(-45.0f, location.getPitch(), 0.0001f);
    }

    @Test
    @DisplayName("getWorld() returns the expected world name")
    void testGetWorld() {
        SafeLocation location = new SafeLocation("custom_world", 0.0, 0.0, 0.0, 0.0f, 0.0f);

        assertEquals("custom_world", location.getWorld());
    }

    @Test
    @DisplayName("setX, setY, setZ update coordinates individually")
    void testCoordinateSetters() {
        SafeLocation location = new SafeLocation("world", 0.0, 0.0, 0.0, 0.0f, 0.0f);

        location.setX(10.5);
        assertEquals(10.5, location.getX(), 0.0001);

        location.setY(20.5);
        assertEquals(20.5, location.getY(), 0.0001);

        location.setZ(30.5);
        assertEquals(30.5, location.getZ(), 0.0001);
    }

    @Test
    @DisplayName("setYaw and setPitch update rotation individually")
    void testRotationSetters() {
        SafeLocation location = new SafeLocation("world", 0.0, 0.0, 0.0, 0.0f, 0.0f);

        location.setYaw(90.0f);
        assertEquals(90.0f, location.getYaw(), 0.0001f);

        location.setPitch(-30.0f);
        assertEquals(-30.0f, location.getPitch(), 0.0001f);
    }

    @Test
    @DisplayName("getBlockX, getBlockY, getBlockZ truncate decimal coordinates to integer values")
    void testBlockCoordinatesTruncation() {
        SafeLocation positiveLoc = new SafeLocation("world", 10.7, 64.9, 100.2, 0.0f, 0.0f);
        assertEquals(10, positiveLoc.getBlockX());
        assertEquals(64, positiveLoc.getBlockY());
        assertEquals(100, positiveLoc.getBlockZ());

        SafeLocation negativeLoc = new SafeLocation("world", -0.3, -1.7, -10.9, 0.0f, 0.0f);
        assertEquals(0, negativeLoc.getBlockX());
        assertEquals(-1, negativeLoc.getBlockY());
        assertEquals(-10, negativeLoc.getBlockZ());

        SafeLocation zeroLoc = new SafeLocation("world", 0.0, 0.0, 0.0, 0.0f, 0.0f);
        assertEquals(0, zeroLoc.getBlockX());
        assertEquals(0, zeroLoc.getBlockY());
        assertEquals(0, zeroLoc.getBlockZ());
    }

    @Test
    @DisplayName("set(x, y, z) batch updates all three coordinates")
    void testBatchSetCoordinates() {
        SafeLocation location = new SafeLocation("world", 1.0, 2.0, 3.0, 10.0f, 20.0f);

        location.set(100.25, 200.5, 300.75);

        assertEquals(100.25, location.getX(), 0.0001);
        assertEquals(200.5, location.getY(), 0.0001);
        assertEquals(300.75, location.getZ(), 0.0001);
        assertEquals(10.0f, location.getYaw(), 0.0001f);
        assertEquals(20.0f, location.getPitch(), 0.0001f);
    }

    @Test
    @DisplayName("toString() contains world name, coordinates, and rotation")
    void testToString() {
        SafeLocation location = new SafeLocation("my_world", 12.34, 56.78, 90.12, 45.5f, -15.5f);
        String stringResult = location.toString();

        assertNotNull(stringResult);
        assertTrue(stringResult.contains("my_world"));
        assertTrue(stringResult.contains("12.34"));
        assertTrue(stringResult.contains("56.78"));
        assertTrue(stringResult.contains("90.12"));
        assertTrue(stringResult.contains("45.5"));
        assertTrue(stringResult.contains("-15.5"));
    }
}
