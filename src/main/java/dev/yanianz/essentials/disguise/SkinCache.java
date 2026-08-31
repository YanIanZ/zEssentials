package dev.yanianz.essentials.disguise;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SkinCache {

    private final long cacheTtlMillis;
    private final ConcurrentHashMap<UUID, CachedProfile> cache = new ConcurrentHashMap<>();

    public SkinCache(long cacheTtlMillis) {
        this.cacheTtlMillis = cacheTtlMillis;
    }

    public CachedProfile getCached(UUID uuid) {
        CachedProfile profile = this.cache.get(uuid);
        if (profile == null) return null;
        if (isExpired(uuid)) return null;
        return profile;
    }

    public void put(UUID uuid, String name, String textureValue, String textureSignature) {
        this.cache.put(uuid, new CachedProfile(name, textureValue, textureSignature, System.currentTimeMillis()));
    }

    public boolean isExpired(UUID uuid) {
        CachedProfile profile = this.cache.get(uuid);
        if (profile == null) return true;
        return System.currentTimeMillis() - profile.cachedAt() > this.cacheTtlMillis;
    }

    /**
     * Removes expired entries from the cache. Called periodically so the
     * map does not grow unbounded when players are looked up over time.
     */
    public void evictExpired() {
        long now = System.currentTimeMillis();
        this.cache.values().removeIf(profile -> now - profile.cachedAt() > this.cacheTtlMillis);
    }

    public void clear() {
        this.cache.clear();
    }

    public void clear(UUID uuid) {
        this.cache.remove(uuid);
    }

    public int size() {
        return this.cache.size();
    }

    public record CachedProfile(String name, String textureValue, String textureSignature, long cachedAt) {}
}
