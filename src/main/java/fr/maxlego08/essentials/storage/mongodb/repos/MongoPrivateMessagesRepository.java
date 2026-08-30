package fr.maxlego08.essentials.storage.mongodb.repos;

import com.mongodb.client.MongoDatabase;
import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.dto.PrivateMessageDTO;
import fr.maxlego08.essentials.storage.mongodb.MongoRepository;
import org.bson.Document;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class MongoPrivateMessagesRepository extends MongoRepository {
    public MongoPrivateMessagesRepository(EssentialsPlugin plugin, MongoDatabase database) {
        super(plugin, database, "private_messages");
    }

    public List<PrivateMessageDTO> getMessages(UUID uuid) {
        return find(new Document("sender_unique_id", uuid.toString()), PrivateMessageDTO.class).stream()
                .sorted(Comparator.comparing(PrivateMessageDTO::created_at, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    public void insertMessages(List<PrivateMessageDTO> privateMessages) {
        if (privateMessages.isEmpty()) return;
        insertMany(privateMessages.stream().map(this::toDocument).toList());
    }
}
