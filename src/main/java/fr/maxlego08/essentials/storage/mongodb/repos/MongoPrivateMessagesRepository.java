package fr.maxlego08.essentials.storage.mongodb.repos;

import com.mongodb.client.MongoDatabase;
import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.storage.mongodb.MongoRepository;

public class MongoPrivateMessagesRepository extends MongoRepository {
    public MongoPrivateMessagesRepository(EssentialsPlugin plugin, MongoDatabase database) {
        super(plugin, database, "private_messages");
    }
}
