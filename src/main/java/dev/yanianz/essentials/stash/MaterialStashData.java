package dev.yanianz.essentials.stash;

import org.bukkit.Material;

import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

public class MaterialStashData {

    private final UUID playerId;
    private final Map<Material, Long> quantities;

    public MaterialStashData(UUID playerId) {
        this.playerId = playerId;
        this.quantities = new TreeMap<>();
    }

    public UUID getPlayerId() { return playerId; }
    public Map<Material, Long> getQuantities() { return quantities; }

    public long get(Material material) {
        return quantities.getOrDefault(material, 0L);
    }

    public void set(Material material, long amount) {
        if (amount <= 0) {
            quantities.remove(material);
        } else {
            quantities.put(material, amount);
        }
    }

    public void add(Material material, long amount) {
        if (amount <= 0 || material == null) return;
        quantities.merge(material, (long) amount, Long::sum);
    }

    public boolean remove(Material material, long amount) {
        if (amount <= 0) return true;
        long current = get(material);
        if (current < amount) return false;
        set(material, current - amount);
        return true;
    }

    public long totalItems() {
        return quantities.values().stream().mapToLong(Long::longValue).sum();
    }
}