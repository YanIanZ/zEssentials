package fr.maxlego08.essentials.storage.mongodb.repos;

import com.mongodb.client.MongoDatabase;
import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.dto.MailMessageDTO;
import fr.maxlego08.essentials.api.mailbox.MailMessage;
import fr.maxlego08.essentials.storage.mongodb.MongoRepository;
import org.bson.Document;

import java.util.List;
import java.util.UUID;

public class MongoMailMessageRepository extends MongoRepository {
    public MongoMailMessageRepository(EssentialsPlugin plugin, MongoDatabase database) {
        super(plugin, database, "mail_messages");
    }

    public List<MailMessageDTO> select(UUID uuid) {
        return find(new Document("unique_id", uuid.toString()), MailMessageDTO.class);
    }

    public void insert(MailMessage mailMessage) {
        int id = getNextId();
        MailMessageDTO dto = new MailMessageDTO(id, mailMessage.getUniqueId(), mailMessage.getSenderId(),
                mailMessage.getSenderName(), mailMessage.getContent(), mailMessage.isRead(), mailMessage.getCreatedAt());
        insert(toDocument(dto));
        mailMessage.setId(id);
    }

    public void markAsRead(UUID uuid) {
        collection().updateMany(new Document("unique_id", uuid.toString()),
                new Document("$set", new Document("is_read", true)));
    }

    public void clear(UUID uuid) {
        deleteMany(new Document("unique_id", uuid.toString()));
    }

    private int getNextId() {
        Document doc = collection().find().sort(new Document("id", -1)).first();
        return doc == null ? 1 : doc.getInteger("id", 0) + 1;
    }
}
