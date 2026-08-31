package fr.maxlego08.essentials.storage.mongodb.repos;

import com.mongodb.client.MongoDatabase;
import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.storage.mongodb.MongoRepository;
import org.bson.Document;

import java.util.UUID;

public class MongoMaterialStashRepository extends MongoRepository {
    public MongoMaterialStashRepository(EssentialsPlugin plugin, MongoDatabase database) {
        super(plugin, database, "stash_materials");
    }

    public void upsert(UUID uuid, String json) {
        Document filter = byUuid(uuid);
        Document doc = new Document("uuid", uuid.toString()).append("data_json", json);
        upsert(filter, doc);
    }

    public void delete(UUID uuid) {
        delete(byUuid(uuid));
    }

    public String get(UUID uuid) {
        Document doc = collection().find(byUuid(uuid)).first();
        return doc != null ? doc.getString("data_json") : null;
    }
}
