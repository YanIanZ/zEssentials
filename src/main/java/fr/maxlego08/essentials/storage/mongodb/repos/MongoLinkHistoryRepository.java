package fr.maxlego08.essentials.storage.mongodb.repos;

import com.mongodb.client.MongoDatabase;
import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.storage.mongodb.MongoRepository;

public class MongoLinkHistoryRepository extends MongoRepository {
    public MongoLinkHistoryRepository(EssentialsPlugin plugin, MongoDatabase database) {
        super(plugin, database, "link_history");
    }
}
