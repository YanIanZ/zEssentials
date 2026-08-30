package fr.maxlego08.essentials.storage.mongodb.repos;

import com.mongodb.client.MongoDatabase;
import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.dto.EconomyDTO;
import fr.maxlego08.essentials.api.dto.UserEconomyDTO;
import fr.maxlego08.essentials.api.economy.Economy;
import fr.maxlego08.essentials.storage.mongodb.MongoRepository;
import org.bson.Document;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class MongoUserEconomyRepository extends MongoRepository {
    public MongoUserEconomyRepository(EssentialsPlugin plugin, MongoDatabase database) {
        super(plugin, database, "user_economy");
    }

    public void upsert(UUID uuid, Economy economy, BigDecimal bigDecimal) {
        Document filter = new Document("unique_id", uuid.toString()).append("economy_name", economy.getName());
        Document doc = new Document("unique_id", uuid.toString())
                .append("economy_name", economy.getName())
                .append("amount", bigDecimal);
        upsert(filter, doc);
    }

    public void reset(Economy economy, BigDecimal amount) {
        collection().updateMany(new Document("economy_name", economy.getName()), new Document("$set", new Document("amount", amount)));
    }

    public List<EconomyDTO> select(UUID uuid) {
        return find(new Document("unique_id", uuid.toString()), EconomyDTO.class);
    }

    public List<UserEconomyDTO> getAll() {
        return findAll(UserEconomyDTO.class);
    }
}
