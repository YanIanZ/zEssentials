package dev.yanianz.essentials.stash;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.Material;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class MaterialStashSerializer {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private MaterialStashSerializer() {}

    public static String serialize(MaterialStashData data) {
        SerializedData raw = new SerializedData();
        raw.player_uuid = data.getPlayerId().toString();
        raw.quantities = new LinkedHashMap<>();
        for (Map.Entry<Material, Long> entry : data.getQuantities().entrySet()) {
            raw.quantities.put(entry.getKey().name(), entry.getValue());
        }
        return GSON.toJson(raw);
    }

    public static MaterialStashData deserialize(String json, UUID playerId) {
        SerializedData raw = GSON.fromJson(json, SerializedData.class);
        if (raw == null) return null;
        MaterialStashData data = new MaterialStashData(playerId);
        if (raw.quantities != null) {
            for (Map.Entry<String, Long> entry : raw.quantities.entrySet()) {
                try {
                    data.set(Material.valueOf(entry.getKey()), entry.getValue());
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        return data;
    }

    private static final class SerializedData {
        String player_uuid;
        Map<String, Long> quantities;
    }
}