package fr.maxlego08.essentials.storage.mongodb.repos;

import com.mongodb.client.MongoDatabase;
import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.storage.mongodb.MongoRepository;

public class MongoUserStepRepository extends MongoRepository {
    public MongoUserStepRepository(EssentialsPlugin plugin, MongoDatabase database) {
        super(plugin, database, "steps");
    }
}
