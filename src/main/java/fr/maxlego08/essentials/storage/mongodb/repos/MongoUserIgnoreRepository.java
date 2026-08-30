package fr.maxlego08.essentials.storage.mongodb.repos;

import com.mongodb.client.MongoDatabase;
import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.dto.IgnoreDTO;
import fr.maxlego08.essentials.storage.mongodb.MongoRepository;
import org.bson.Document;

import java.util.List;
import java.util.UUID;

public class MongoUserIgnoreRepository extends MongoRepository {
    public MongoUserIgnoreRepository(EssentialsPlugin plugin, MongoDatabase database) {
        super(plugin, database, "ignores");
    }

    public void upsert(UUID uuid, UUID ignoredId) {
        Document filter = new Document("unique_id", uuid.toString()).append("ignored_id", ignoredId.toString());
        Document doc = new Document("unique_id", uuid.toString()).append("ignored_id", ignoredId.toString());
        upsert(filter, doc);
    }

    public List<IgnoreDTO> select(UUID uuid) {
        return find(new Document("unique_id", uuid.toString()), IgnoreDTO.class);
    }

    public void delete(UUID uuid, UUID ignoredId) {
        delete(new Document("unique_id", uuid.toString()).append("ignored_id", ignoredId.toString()));
    }
}
