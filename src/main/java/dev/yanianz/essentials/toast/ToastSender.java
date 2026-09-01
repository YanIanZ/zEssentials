package dev.yanianz.essentials.toast;

import fr.maxlego08.essentials.ZEssentialsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.entity.Player;

import java.util.Iterator;

public final class ToastSender {

    private ToastSender() {}

    private static final String TOAST_CRITERIA = "impossible";

    public static void send(Player player, String title, String iconMaterial) {
        if (player == null || title == null) return;
        String material = iconMaterial == null || iconMaterial.isEmpty() ? "minecraft:stone" : iconMaterial.toLowerCase();
        if (!material.startsWith("minecraft:")) material = "minecraft:" + material;

        String json = "{"
                + "\"criteria\":{\"zessentials_toast\":{\"trigger\":\"minecraft:impossible\"}},"
                + "\"display\":{"
                + "\"icon\":{\"item\":\"" + material + "\"},"
                + "\"title\":\"" + escapeJson(title) + "\","
                + "\"description\":\"\","
                + "\"background\":\"minecraft:textures/gui/advancements/backgrounds/adventure.png\","
                + "\"frame\":\"goal\","
                + "\"announce_to_chat\":false,"
                + "\"show_toast\":true,"
                + "\"hidden\":true"
                + "}}";

        try {
            org.bukkit.advancement.Advancement advancement = findOrCreateAdvancement(player, json);
            if (advancement != null) {
                player.getAdvancementProgress(advancement).awardCriteria(TOAST_CRITERIA);
            }
        } catch (Exception e) {
            player.sendMessage(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection()
                    .deserialize(dev.yanianz.essentials.util.ColorUtil.sections(title)));
        }
    }

    private static Advancement findOrCreateAdvancement(Player player, String json) {
        try {
            Iterator<Advancement> it = Bukkit.getServer().advancementIterator();
            while (it.hasNext()) {
                Advancement adv = it.next();
                if (adv.getKey().toString().contains("zessentials:toast")) {
                    return adv;
                }
            }
        } catch (Exception ignored) {}
        return loadAdvancement(player, json);
    }

    private static Advancement loadAdvancement(Player player, String json) {
        try {
            return loadViaReflection(player, json);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Advancement loadViaReflection(Player player, String json) throws Exception {
        Class<?> craftServerClass = Class.forName("org.bukkit.craftbukkit.CraftServer");
        Object craftServer = craftServerClass.cast(Bukkit.getServer());
        Object mcServer = craftServerClass.getMethod("getServer").invoke(craftServer);
        Object advancements = mcServer.getClass().getMethod("getAdvancements").invoke(mcServer);
        NamespacedKey key = new NamespacedKey("zessentials", "toast_" + System.currentTimeMillis());
        return (Advancement) advancements.getClass()
                .getMethod("loadAdvancement", NamespacedKey.class, String.class)
                .invoke(advancements, key, json);
    }

    private static String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
}
