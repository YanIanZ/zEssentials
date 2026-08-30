package fr.maxlego08.essentials.storage.mongodb.repos;

import com.mongodb.client.MongoDatabase;
import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.dto.ChatMessageDTO;
import fr.maxlego08.essentials.storage.mongodb.MongoRepository;
import org.bson.Document;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class MongoChatMessagesRepository extends MongoRepository {
    public MongoChatMessagesRepository(EssentialsPlugin plugin, MongoDatabase database) {
        super(plugin, database, "chat_messages");
    }

    public List<ChatMessageDTO> getMessages(UUID uuid) {
        return find(new Document("unique_id", uuid.toString()), ChatMessageDTO.class).stream()
                .sorted(Comparator.comparing(ChatMessageDTO::created_at, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    public void insertMessages(List<ChatMessageDTO> messages) {
        if (messages.isEmpty()) return;
        insertMany(messages.stream().map(this::toDocument).toList());
    }

    public int deleteMessages(UUID uuid, String content) {
        return (int) collection().deleteMany(new Document("unique_id", uuid.toString()).append("content", content)).getDeletedCount();
    }
}
