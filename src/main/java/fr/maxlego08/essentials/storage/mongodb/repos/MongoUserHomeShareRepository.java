package fr.maxlego08.essentials.storage.mongodb.repos;

import com.mongodb.client.MongoDatabase;
import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.dto.HomeShareDTO;
import fr.maxlego08.essentials.storage.mongodb.MongoRepository;
import org.bson.Document;

import java.util.List;
import java.util.UUID;

public class MongoUserHomeShareRepository extends MongoRepository {
    public MongoUserHomeShareRepository(EssentialsPlugin plugin, MongoDatabase database) {
        super(plugin, database, "home_shares");
    }

    public void upsert(UUID owner, String homeName, UUID target) {
        Document filter = new Document("owner_id", owner.toString())
                .append("home_name", homeName)
                .append("target_id", target.toString());
        Document doc = new Document("owner_id", owner.toString())
                .append("home_name", homeName)
                .append("target_id", target.toString());
        upsert(filter, doc);
    }

    public void delete(UUID owner, String homeName, UUID target) {
        delete(new Document("owner_id", owner.toString())
                .append("home_name", homeName)
                .append("target_id", target.toString()));
    }

    public void deleteAll(UUID owner, String homeName) {
        deleteMany(new Document("owner_id", owner.toString()).append("home_name", homeName));
    }

    public List<HomeShareDTO> selectByOwner(UUID owner) {
        return find(new Document("owner_id", owner.toString()), HomeShareDTO.class);
    }

    public List<HomeShareDTO> selectSharedWith(UUID target) {
        return find(new Document("target_id", target.toString()), HomeShareDTO.class);
    }

    public boolean isSharedWith(UUID owner, String homeName, UUID target) {
        return !find(new Document("owner_id", owner.toString())
                .append("home_name", homeName)
                .append("target_id", target.toString()), HomeShareDTO.class).isEmpty();
    }
}
