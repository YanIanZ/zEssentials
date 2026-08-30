package fr.maxlego08.essentials.storage.mongodb.repos;

import com.mongodb.client.MongoDatabase;
import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.storage.mongodb.MongoRepository;

public class MongoLinkAccountRepository extends MongoRepository {
    public MongoLinkAccountRepository(EssentialsPlugin plugin, MongoDatabase database) {
        super(plugin, database, "link_accounts");
    }
}
