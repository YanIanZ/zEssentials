package fr.maxlego08.essentials.storage.mongodb.repos;

import com.mongodb.client.MongoDatabase;
import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.dto.ServerStorageDTO;
import fr.maxlego08.essentials.storage.mongodb.MongoRepository;
import org.bson.Document;

import java.util.List;

public class MongoServerStorageRepository extends MongoRepository {
    public MongoServerStorageRepository(EssentialsPlugin plugin, MongoDatabase database) {
        super(plugin, database, "server_storage");
    }

    public void upsert(String key, Object value) {
        Document filter = new Document("name", key);
        Document doc = new Document("name", key).append("content", value == null ? null : value.toString());
        upsert(filter, doc);
    }

    public List<ServerStorageDTO> select() {
        return findAll(ServerStorageDTO.class);
    }
}
