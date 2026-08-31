package dev.yanianz.essentials.disguise;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class SkinFetcher {

    private SkinFetcher() {
    }

    public static UUID fetchUuidFromName(String playerName) throws IOException {
        String url = "https://api.mojang.com/users/profiles/minecraft/" + playerName;
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        if (conn.getResponseCode() != 200) return null;
        String body;
        try (InputStream is = conn.getInputStream()) {
            body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
        String idStr = parseJsonField(body, "id");
        if (idStr == null) return null;
        return parseMojangId(idStr);
    }

    public static String[] fetchTexturesFromUuid(UUID uuid) throws IOException {
        String url = "https://sessionserver.mojang.com/session/minecraft/profile/" + uuid.toString().replace("-", "") + "?unsigned=false";
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        if (conn.getResponseCode() != 200) return null;
        String body;
        try (InputStream is = conn.getInputStream()) {
            body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
        String value = parseJsonField(body, "value");
        if (value == null) return null;
        String signature = parseJsonField(body, "signature");
        return new String[]{value, signature};
    }

    private static UUID parseMojangId(String idStr) {
        if (idStr.length() != 32) return null;
        String dashed = idStr.substring(0, 8) + "-" + idStr.substring(8, 12) + "-" + idStr.substring(12, 16) + "-" + idStr.substring(16, 20) + "-" + idStr.substring(20);
        return UUID.fromString(dashed);
    }

    public static String parseJsonField(String json, String field) {
        String needle = "\"" + field + "\":\"";
        int start = json.indexOf(needle);
        if (start < 0) return null;
        start += needle.length();
        int end = json.indexOf("\"", start);
        if (end < 0) return null;
        return json.substring(start, end);
    }
}
