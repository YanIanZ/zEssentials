package fr.maxlego08.essentials.storage.mongodb.repos;

import com.mongodb.client.MongoDatabase;
import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.dto.PlayerSlotDTO;
import fr.maxlego08.essentials.storage.mongodb.MongoRepository;
import org.bson.Document;

import java.util.List;
import java.util.UUID;

public class MongoPlayerSlotRepository extends MongoRepository {
    public MongoPlayerSlotRepository(EssentialsPlugin plugin, MongoDatabase database) {
        super(plugin, database, "player_slots");
    }

    public List<PlayerSlotDTO> select() {
        return findAll(PlayerSlotDTO.class);
    }

    public void setSlot(UUID uniqueId, int slot) {
        if (slot == 0) {
            deleteSlot(uniqueId);
            return;
        }
        Document filter = new Document("unique_id", uniqueId.toString());
        Document doc = new Document("unique_id", uniqueId.toString()).append("slots", slot);
        upsert(filter, doc);
    }

    public void deleteSlot(UUID uniqueId) {
        delete(new Document("unique_id", uniqueId.toString()));
    }
}
