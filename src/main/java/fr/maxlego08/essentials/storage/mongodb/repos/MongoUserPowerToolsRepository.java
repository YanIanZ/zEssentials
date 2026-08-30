package fr.maxlego08.essentials.storage.mongodb.repos;

import com.mongodb.client.MongoDatabase;
import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.dto.PowerToolsDTO;
import fr.maxlego08.essentials.storage.mongodb.MongoRepository;
import org.bson.Document;
import org.bukkit.Material;

import java.util.List;
import java.util.UUID;

public class MongoUserPowerToolsRepository extends MongoRepository {
    public MongoUserPowerToolsRepository(EssentialsPlugin plugin, MongoDatabase database) {
        super(plugin, database, "power_tools");
    }

    public void upsert(UUID uuid, Material material, String command) {
        Document filter = new Document("unique_id", uuid.toString()).append("material", material.name());
        Document doc = new Document("unique_id", uuid.toString())
                .append("material", material.name())
                .append("command", command);
        upsert(filter, doc);
    }

    public List<PowerToolsDTO> select(UUID uuid) {
        return find(new Document("unique_id", uuid.toString()), PowerToolsDTO.class);
    }

    public void delete(UUID uuid, Material material) {
        delete(new Document("unique_id", uuid.toString()).append("material", material.name()));
    }
}
