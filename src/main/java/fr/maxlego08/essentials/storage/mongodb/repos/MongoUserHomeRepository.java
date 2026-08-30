package fr.maxlego08.essentials.storage.mongodb.repos;

import com.mongodb.client.MongoDatabase;
import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.storage.mongodb.MongoRepository;

public class MongoUserHomeRepository extends MongoRepository {
    public MongoUserHomeRepository(EssentialsPlugin plugin, MongoDatabase database) {
        super(plugin, database, "homes");
    }
}
