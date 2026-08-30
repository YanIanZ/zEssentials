package fr.maxlego08.essentials.storage.mongodb.repos;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.dto.SanctionDTO;
import fr.maxlego08.essentials.api.sanction.Sanction;
import fr.maxlego08.essentials.storage.mongodb.MongoRepository;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class MongoUserSanctionRepository extends MongoRepository {
    public MongoUserSanctionRepository(EssentialsPlugin plugin, MongoDatabase database) {
        super(plugin, database, "sanctions");
    }

    public void insert(Sanction sanction, Consumer<Integer> consumer) {
        int id = getNextId();
        SanctionDTO dto = new SanctionDTO(id, sanction.getPlayerUniqueId(), sanction.getSenderUniqueId(),
                sanction.getReason(), sanction.getCreatedAt(), sanction.getExpiredAt(),
                sanction.getSanctionType(), sanction.getDuration());
        insert(toDocument(dto));
        consumer.accept(id);
    }

    public SanctionDTO getSanction(Integer id) {
        return findOne(new Document("id", id), SanctionDTO.class);
    }

    public List<SanctionDTO> getActiveBan() {
        return getActiveLinkedSanctions("ban_sanction_id");
    }

    public List<SanctionDTO> getActiveMute() {
        return getActiveLinkedSanctions("mute_sanction_id");
    }

    public List<SanctionDTO> getSanctions(UUID uuid) {
        return find(new Document("player_unique_id", uuid.toString()), SanctionDTO.class);
    }

    private List<SanctionDTO> getActiveLinkedSanctions(String userLinkField) {
        MongoCollection<Document> usersCol = database().getCollection("users");
        List<SanctionDTO> active = new ArrayList<>();
        for (Document userDoc : usersCol.find(new Document(userLinkField, new Document("$exists", true).append("$ne", null)))) {
            Integer sanctionId = userDoc.getInteger(userLinkField);
            if (sanctionId == null) continue;
            SanctionDTO dto = findOne(new Document("id", sanctionId), SanctionDTO.class);
            if (dto != null && dto.isActive()) active.add(dto);
        }
        return active;
    }

    private int getNextId() {
        Document doc = collection().find().sort(new Document("id", -1)).first();
        return doc == null ? 1 : doc.getInteger("id", 0) + 1;
    }
}
