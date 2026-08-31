package fr.maxlego08.essentials.storage.database.repositeries;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.storage.database.Repository;
import fr.maxlego08.sarah.DatabaseConnection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ChatPreferenceRepository extends Repository {
    public ChatPreferenceRepository(EssentialsPlugin plugin, DatabaseConnection connection) {
        super(plugin, connection, "chat_preferences");
    }
    public void upsert(UUID uuid, String json) {
        upsert(table -> {
            table.uuid("uuid", uuid);
            table.string("data_json", json);
        });
    }
    public void delete(UUID uuid) {
        delete(table -> table.where("uuid", uuid.toString()));
    }
    public String get(UUID uuid) {
        List<JsonEntry> results = select(JsonEntry.class, table -> table.where("uuid", uuid.toString()));
        return results.isEmpty() ? null : results.getFirst().data_json;
    }
    public Map<UUID, String> getAll() {
        List<JsonEntry> results = selectAll(JsonEntry.class);
        Map<UUID, String> map = new HashMap<>();
        for (JsonEntry entry : results) {
            try { map.put(UUID.fromString(entry.uuid), entry.data_json); }
            catch (IllegalArgumentException ignored) {}
        }
        return map;
    }
    public static class JsonEntry {
        public String uuid;
        public String data_json;
    }
}
