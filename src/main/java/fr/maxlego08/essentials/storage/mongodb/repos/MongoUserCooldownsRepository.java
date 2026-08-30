package fr.maxlego08.essentials.storage.mongodb.repos;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.dto.CooldownDTO;
import fr.maxlego08.essentials.storage.mongodb.MongoRepository;
import org.bson.Document;

import java.util.List;
import java.util.UUID;

public class MongoUserCooldownsRepository extends MongoRepository {
    public MongoUserCooldownsRepository(EssentialsPlugin plugin, MongoDatabase database) {
        super(plugin, database, "cooldowns");
    }

    public void upsert(UUID uuid, String cooldownName, long cooldownValue) {
        Document filter = new Document("unique_id", uuid.toString()).append("cooldown_name", cooldownName);
        collection().updateOne(
                filter,
                new Document("$set", new Document("cooldown_value", cooldownValue)),
                new UpdateOptions().upsert(true)
        );
    }

    public void delete(UUID uniqueId, String key) {
        delete(new Document("unique_id", uniqueId.toString()).append("cooldown_name", key));
    }

    public void deleteExpiredCooldowns() {
        deleteMany(new Document("cooldown_value", new Document("$lt", System.currentTimeMillis())));
    }

    public List<CooldownDTO> select(UUID uuid) {
        return find(new Document("unique_id", uuid.toString()), CooldownDTO.class);
    }
}
