package fr.maxlego08.essentials.storage.database.repositeries;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.storage.database.Repository;
import fr.maxlego08.sarah.DatabaseConnection;
import fr.maxlego08.sarah.database.Schema;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class NicknameRepository extends Repository {
    public NicknameRepository(EssentialsPlugin plugin, DatabaseConnection connection) {
        super(plugin, connection, "nicknames");
    }
    public void upsert(UUID uuid, String nickname) {
        upsert(table -> {
            table.uuid("uuid", uuid).primary();
            table.string("nickname", nickname);
        });
    }
    public void delete(UUID uuid) {
        delete(table -> table.where("uuid", uuid.toString()));
    }
    public String get(UUID uuid) {
        List<NicknameEntry> results = select(NicknameEntry.class, table -> table.where("uuid", uuid.toString()));
        return results.isEmpty() ? null : results.getFirst().nickname;
    }
    public Map<UUID, String> getAll() {
        List<NicknameEntry> results = selectAll(NicknameEntry.class);
        Map<UUID, String> map = new HashMap<>();
        for (NicknameEntry entry : results) {
            try { map.put(UUID.fromString(entry.uuid), entry.nickname); }
            catch (IllegalArgumentException ignored) {}
        }
        return map;
    }
    public static class NicknameEntry {
        public String uuid;
        public String nickname;
    }
}
