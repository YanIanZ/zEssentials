package dev.yanianz.essentials.enderchest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;

public final class EnderChestSerializer {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private EnderChestSerializer() {
    }

    public static String serialize(EnderChestData data) {
        SerializedData raw = new SerializedData();
        raw.player_uuid = data.getPlayerId().toString();
        raw.pages = data.getPages();
        ItemStack[] contents = data.rawContents();
        raw.contents = new Object[contents.length];
        for (int i = 0; i < contents.length; i++) {
            raw.contents[i] = contents[i] == null ? null : contents[i].serialize();
        }
        return GSON.toJson(raw);
    }

    @SuppressWarnings("unchecked")
    public static EnderChestData deserialize(String json, UUID playerId) {
        SerializedData raw = GSON.fromJson(json, SerializedData.class);
        if (raw == null) return null;
        int pages = Math.max(1, raw.pages);
        ItemStack[] contents = new ItemStack[EnderChestSlotMap.totalSize(pages)];
        if (raw.contents != null) {
            for (int i = 0; i < Math.min(raw.contents.length, contents.length); i++) {
                if (raw.contents[i] != null) {
                    contents[i] = ItemStack.deserialize((Map<String, Object>) raw.contents[i]);
                }
            }
        }
        EnderChestData data = new EnderChestData(playerId, pages);
        data.setRawContents(contents, pages);
        return data;
    }

    private static final class SerializedData {
        String player_uuid;
        int pages;
        Object[] contents;
    }
}
