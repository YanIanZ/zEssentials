package fr.maxlego08.essentials.api.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@DisplayName("ExpiringCache Tests")
class ExpiringCacheTest {

    private ExpiringCache<String, String> cache;
    private final long expiryMillis = 100L;

    @BeforeEach
    void setUp() {
        cache = new ExpiringCache<>(expiryMillis);
    }

    @Test
    @DisplayName("get() should load and return value on first call")
    void testGetLoadsAndReturnsValue() {
        AtomicInteger loaderCalls = new AtomicInteger(0);
        String key = "testKey";

        String result = cache.get(key, () -> {
            loaderCalls.incrementAndGet();
            return "loadedValue";
        });

        assertEquals("loadedValue", result);
        assertEquals(1, loaderCalls.get());
    }

    @Test
    @DisplayName("get() should return cached value before expiry without invoking loader again")
    void testGetReturnsCachedValueBeforeExpiry() {
        AtomicInteger loaderCalls = new AtomicInteger(0);
        String key = "testKey";
        ExpiringCache.Loader<String> loader = () -> {
            loaderCalls.incrementAndGet();
            return "cachedValue";
        };

        String first = cache.get(key, loader);
        String second = cache.get(key, loader);
        String third = cache.get(key, loader);

        assertEquals("cachedValue", first);
        assertEquals("cachedValue", second);
        assertEquals("cachedValue", third);
        assertEquals(1, loaderCalls.get(), "Loader should only be called once before expiry");
    }

    @Test
    @DisplayName("get() should reload value after expiry duration has elapsed")
    void testGetReloadsAfterExpiry() throws InterruptedException {
        ExpiringCache<String, String> shortCache = new ExpiringCache<>(50L);
        AtomicInteger loaderCalls = new AtomicInteger(0);
        String key = "expiringKey";
        ExpiringCache.Loader<String> loader = () -> {
            int count = loaderCalls.incrementAndGet();
            return "value_" + count;
        };

        String first = shortCache.get(key, loader);
        assertEquals("value_1", first);
        assertEquals(1, loaderCalls.get());

        // Wait for entry to expire (>50ms)
        Thread.sleep(70L);

        String second = shortCache.get(key, loader);
        assertEquals("value_2", second);
        assertEquals(2, loaderCalls.get(), "Loader should be re-invoked after expiry");
    }

    @Test
    @DisplayName("clear(key) should remove specific entry from cache")
    void testClearRemovesSpecificEntry() {
        AtomicInteger key1Calls = new AtomicInteger(0);
        AtomicInteger key2Calls = new AtomicInteger(0);

        String k1 = "key1";
        String k2 = "key2";

        cache.get(k1, () -> {
            key1Calls.incrementAndGet();
            return "val1";
        });
        cache.get(k2, () -> {
            key2Calls.incrementAndGet();
            return "val2";
        });

        assertEquals(1, key1Calls.get());
        assertEquals(1, key2Calls.get());

        // Clear only key1
        cache.clear(k1);

        // Access key2 -> should NOT reload
        String val2 = cache.get(k2, () -> {
            key2Calls.incrementAndGet();
            return "val2_new";
        });
        assertEquals("val2", val2);
        assertEquals(1, key2Calls.get(), "Key 2 should still be cached");

        // Access key1 -> should reload
        String val1 = cache.get(k1, () -> {
            key1Calls.incrementAndGet();
            return "val1_new";
        });
        assertEquals("val1_new", val1);
        assertEquals(2, key1Calls.get(), "Key 1 should be reloaded after clear()");
    }

    @Test
    @DisplayName("Different keys should be cached independently")
    void testDifferentKeysAreIndependent() {
        AtomicInteger callCount = new AtomicInteger(0);

        String resultA = cache.get("A", () -> {
            callCount.incrementAndGet();
            return "resultA";
        });
        String resultB = cache.get("B", () -> {
            callCount.incrementAndGet();
            return "resultB";
        });

        assertEquals("resultA", resultA);
        assertEquals("resultB", resultB);
        assertEquals(2, callCount.get());

        // Subsequent gets should not call loader
        assertEquals("resultA", cache.get("A", () -> {
            fail("Should not be called");
            return "fail";
        }));
        assertEquals("resultB", cache.get("B", () -> {
            fail("Should not be called");
            return "fail";
        }));
    }
}
