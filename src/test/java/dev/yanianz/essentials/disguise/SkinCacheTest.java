package dev.yanianz.essentials.disguise;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SkinCacheTest {

    private SkinCache cache;

    @BeforeEach
    void setUp() {
        cache = new SkinCache(1000L);
    }

    @Test
    @DisplayName("getCached returns null for unknown uuid")
    void testUnknownUuid() {
        assertNull(cache.getCached(UUID.randomUUID()));
    }

    @Test
    @DisplayName("put then getCached returns the profile")
    void testPutAndGet() {
        UUID uuid = UUID.randomUUID();
        cache.put(uuid, "Notch", "texture-value", "signature");
        SkinCache.CachedProfile profile = cache.getCached(uuid);
        assertNotNull(profile);
        assertEquals("Notch", profile.name());
        assertEquals("texture-value", profile.textureValue());
        assertEquals("signature", profile.textureSignature());
    }

    @Test
    @DisplayName("isExpired returns true for unknown uuid")
    void testExpiredUnknown() {
        assertTrue(cache.isExpired(UUID.randomUUID()));
    }

    @Test
    @DisplayName("isExpired returns false for fresh entry, true after TTL")
    void testExpiry() throws InterruptedException {
        UUID uuid = UUID.randomUUID();
        cache.put(uuid, "Steve", "tex", null);
        assertFalse(cache.isExpired(uuid));
        Thread.sleep(1100);
        assertTrue(cache.isExpired(uuid));
    }

    @Test
    @DisplayName("clear removes all entries")
    void testClearAll() {
        cache.put(UUID.randomUUID(), "A", "t1", null);
        cache.put(UUID.randomUUID(), "B", "t2", null);
        assertEquals(2, cache.size());
        cache.clear();
        assertEquals(0, cache.size());
    }

    @Test
    @DisplayName("clear(uuid) removes one entry")
    void testClearOne() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        cache.put(a, "A", "t1", null);
        cache.put(b, "B", "t2", null);
        cache.clear(a);
        assertNull(cache.getCached(a));
        assertNotNull(cache.getCached(b));
    }

    @Test
    @DisplayName("expired entries are not returned by getCached")
    void testExpiredNotReturned() throws InterruptedException {
        UUID uuid = UUID.randomUUID();
        cache.put(uuid, "Steve", "tex", null);
        Thread.sleep(1100);
        assertNull(cache.getCached(uuid));
    }
}
