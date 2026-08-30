package fr.maxlego08.essentials.storage.mongodb.repos;

import com.mongodb.client.MongoDatabase;
import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.storage.mongodb.MongoRepository;

public class MongoEconomyTransactionsRepository extends MongoRepository {
    public MongoEconomyTransactionsRepository(EssentialsPlugin plugin, MongoDatabase database) {
        super(plugin, database, "economy_transactions");
    }
}
