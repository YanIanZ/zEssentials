package dev.yanianz.essentials.enderchest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.inventory.ItemStack;

import java.util.Base64;
import java.util.UUID;

public class EnderChestSerializer {

    private static final EnderChestSerializer INSTANCE = new EnderChestSerializer();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    EnderChestSerializer() {
    }

    public static String serialize(EnderChestData data) {
        return INSTANCE.serializeData(data);
    }

    public static EnderChestData deserialize(String json, UUID playerId) {
        return INSTANCE.deserializeData(json, playerId);
    }

    protected byte[] encodeItem(ItemStack item) {
        return item.serializeAsBytes();
    }

    protected ItemStack decodeItem(byte[] bytes) {
        return ItemStack.deserializeBytes(bytes);
    }

    String serializeData(EnderChestData data) {
        SerializedData raw = new SerializedData();
        raw.player_uuid = data.getPlayerId().toString();
        raw.pages = data.getPages();
        ItemStack[] contents = data.rawContents();
        raw.contents = new String[contents.length];
        for (int i = 0; i < contents.length; i++) {
            raw.contents[i] = contents[i] == null ? null
                    : Base64.getEncoder().encodeToString(encodeItem(contents[i]));
        }
        return GSON.toJson(raw);
    }

    EnderChestData deserializeData(String json, UUID playerId) {
        SerializedData raw = GSON.fromJson(json, SerializedData.class);
        if (raw == null) return null;
        int pages = Math.max(1, raw.pages);
        ItemStack[] contents = new ItemStack[EnderChestSlotMap.totalSize(pages)];
        if (raw.contents != null) {
            for (int i = 0; i < Math.min(raw.contents.length, contents.length); i++) {
                if (raw.contents[i] != null) {
                    contents[i] = decodeItem(Base64.getDecoder().decode(raw.contents[i]));
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
        String[] contents;
    }
}
