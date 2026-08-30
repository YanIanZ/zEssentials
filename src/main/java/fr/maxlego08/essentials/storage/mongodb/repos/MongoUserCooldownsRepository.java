package fr.maxlego08.essentials.storage.mongodb.repos;

import com.mongodb.client.MongoDatabase;
import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.storage.mongodb.MongoRepository;

public class MongoUserCooldownsRepository extends MongoRepository {
    public MongoUserCooldownsRepository(EssentialsPlugin plugin, MongoDatabase database) {
        super(plugin, database, "cooldowns");
    }
}
