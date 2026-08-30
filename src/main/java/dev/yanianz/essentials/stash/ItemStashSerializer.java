package dev.yanianz.essentials.stash;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.yanianz.essentials.enderchest.EnderChestSlotMap;
import org.bukkit.inventory.ItemStack;

import java.util.Base64;
import java.util.UUID;

public final class ItemStashSerializer {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private ItemStashSerializer() {}

    public static String serialize(ItemStashData data) {
        SerializedData raw = new SerializedData();
        raw.player_uuid = data.getPlayerId().toString();
        raw.pages = data.getPages();
        ItemStack[] contents = data.rawContents();
        raw.contents = new String[contents.length];
        for (int i = 0; i < contents.length; i++) {
            raw.contents[i] = contents[i] == null ? null
                    : Base64.getEncoder().encodeToString(contents[i].serializeAsBytes());
        }
        return GSON.toJson(raw);
    }

    public static ItemStashData deserialize(String json, UUID playerId) {
        SerializedData raw = GSON.fromJson(json, SerializedData.class);
        if (raw == null) return null;
        int pages = Math.max(1, raw.pages);
        ItemStack[] contents = new ItemStack[EnderChestSlotMap.totalSize(pages)];
        if (raw.contents != null) {
            for (int i = 0; i < Math.min(raw.contents.length, contents.length); i++) {
                if (raw.contents[i] != null) {
                    contents[i] = ItemStack.deserializeBytes(
                            Base64.getDecoder().decode(raw.contents[i]));
                }
            }
        }
        ItemStashData data = new ItemStashData(playerId, pages);
        data.setRawContents(contents, pages);
        return data;
    }

    private static final class SerializedData {
        String player_uuid;
        int pages;
        String[] contents;
    }
}