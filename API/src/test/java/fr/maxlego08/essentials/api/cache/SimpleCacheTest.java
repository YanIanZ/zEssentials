package fr.maxlego08.essentials.api.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("SimpleCache Tests")
class SimpleCacheTest {

    private SimpleCache<String, String> cache;

    @BeforeEach
    void setUp() {
        cache = new SimpleCache<>();
    }

    @Test
    @DisplayName("get() should load and cache value on first call")
    void testGetLoadsAndCachesValue() {
        AtomicInteger loaderCallCount = new AtomicInteger(0);
        String key = "testKey";

        String result = cache.get(key, () -> {
            loaderCallCount.incrementAndGet();
            return "testValue";
        });

        assertEquals("testValue", result);
        assertEquals(1, loaderCallCount.get());
    }

    @Test
    @DisplayName("get() should return cached value on subsequent calls without reloading")
    void testGetReturnsCachedValueOnSubsequentCalls() {
        AtomicInteger loaderCallCount = new AtomicInteger(0);
        String key = "testKey";
        SimpleCache.Loader<String> loader = () -> {
            loaderCallCount.incrementAndGet();
            return "cachedValue";
        };

        String first = cache.get(key, loader);
        String second = cache.get(key, loader);
        String third = cache.get(key, loader);

        assertEquals("cachedValue", first);
        assertEquals("cachedValue", second);
        assertEquals("cachedValue", third);
        assertEquals(1, loaderCallCount.get(), "Loader should only be called once");
    }

    @Test
    @DisplayName("clear() should empty the cache so subsequent get() reloads value")
    void testClearEmptiesCache() {
        AtomicInteger loaderCallCount = new AtomicInteger(0);
        String key = "testKey";
        SimpleCache.Loader<String> loader = () -> {
            int count = loaderCallCount.incrementAndGet();
            return "value" + count;
        };

        String first = cache.get(key, loader);
        assertEquals("value1", first);
        assertEquals(1, loaderCallCount.get());

        cache.clear();

        String second = cache.get(key, loader);
        assertEquals("value2", second);
        assertEquals(2, loaderCallCount.get(), "Loader should be called again after clear()");
    }

    @Test
    @DisplayName("get() should throw IllegalStateException when loader returns null")
    void testGetThrowsIllegalStateExceptionOnNullLoaderResult() {
        String key = "nullKey";
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            cache.get(key, () -> null);
        });

        assertTrue(exception.getMessage().contains("Cache loader returned null for key: " + key));
    }

    @Test
    @DisplayName("get() should handle concurrent access from multiple threads correctly")
    void testConcurrentAccess() throws InterruptedException {
        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);
        AtomicInteger loaderCallCount = new AtomicInteger(0);
        String key = "concurrentKey";

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    String val = cache.get(key, () -> {
                        loaderCallCount.incrementAndGet();
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException ignored) {
                        }
                        return "concurrentValue";
                    });
                    assertEquals("concurrentValue", val);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = finishLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "All threads should complete within timeout");
        assertEquals(1, loaderCallCount.get(), "Loader should be invoked exactly once even under concurrency");
        assertEquals("concurrentValue", cache.get(key, () -> "other"));
    }

    @Test
    @DisplayName("get() should cache different keys independently")
    void testMultipleKeysIndependent() {
        AtomicInteger key1Count = new AtomicInteger(0);
        AtomicInteger key2Count = new AtomicInteger(0);

        String val1 = cache.get("k1", () -> {
            key1Count.incrementAndGet();
            return "v1";
        });
        String val2 = cache.get("k2", () -> {
            key2Count.incrementAndGet();
            return "v2";
        });

        assertEquals("v1", val1);
        assertEquals("v2", val2);
        assertEquals(1, key1Count.get());
        assertEquals(1, key2Count.get());
    }
}
