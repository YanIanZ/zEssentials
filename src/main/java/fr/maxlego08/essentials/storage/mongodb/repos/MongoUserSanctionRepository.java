package fr.maxlego08.essentials.storage.mongodb.repos;

import com.mongodb.client.MongoDatabase;
import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.storage.mongodb.MongoRepository;

public class MongoUserSanctionRepository extends MongoRepository {
    public MongoUserSanctionRepository(EssentialsPlugin plugin, MongoDatabase database) {
        super(plugin, database, "sanctions");
    }
}
