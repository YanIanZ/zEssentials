package fr.maxlego08.essentials.storage.mongodb.repos;

import com.mongodb.client.MongoDatabase;
import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.storage.mongodb.MongoRepository;
import org.bson.Document;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MongoNicknameRepository extends MongoRepository {
    public MongoNicknameRepository(EssentialsPlugin plugin, MongoDatabase database) {
        super(plugin, database, "nicknames");
    }

    public void upsert(UUID uuid, String nickname) {
        Document filter = byUuid(uuid);
        Document doc = new Document("uuid", uuid.toString()).append("nickname", nickname);
        upsert(filter, doc);
    }

    public void delete(UUID uuid) {
        delete(byUuid(uuid));
    }

    public String get(UUID uuid) {
        Document doc = collection().find(byUuid(uuid)).first();
        return doc != null ? doc.getString("nickname") : null;
    }

    public Map<UUID, String> getAll() {
        Map<UUID, String> map = new HashMap<>();
        for (Document doc : collection().find()) {
            try {
                map.put(UUID.fromString(doc.getString("uuid")), doc.getString("nickname"));
            } catch (Exception ignored) {
            }
        }
        return map;
    }
}
