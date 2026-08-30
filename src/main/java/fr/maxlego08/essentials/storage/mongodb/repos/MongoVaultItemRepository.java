package fr.maxlego08.essentials.storage.mongodb.repos;

import com.mongodb.client.MongoDatabase;
import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.storage.mongodb.MongoRepository;

public class MongoVaultItemRepository extends MongoRepository {
    public MongoVaultItemRepository(EssentialsPlugin plugin, MongoDatabase database) {
        super(plugin, database, "vault_items");
    }
}
