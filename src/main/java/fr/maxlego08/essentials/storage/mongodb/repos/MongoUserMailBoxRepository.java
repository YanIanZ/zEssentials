package fr.maxlego08.essentials.storage.mongodb.repos;

import com.mongodb.client.MongoDatabase;
import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.dto.MailBoxDTO;
import fr.maxlego08.essentials.api.mailbox.MailBoxItem;
import fr.maxlego08.essentials.storage.mongodb.MongoRepository;
import fr.maxlego08.menu.common.utils.nms.ItemStackUtils;
import org.bson.Document;

import java.util.Date;
import java.util.List;
import java.util.UUID;

public class MongoUserMailBoxRepository extends MongoRepository {
    public MongoUserMailBoxRepository(EssentialsPlugin plugin, MongoDatabase database) {
        super(plugin, database, "mails");
    }

    public List<MailBoxDTO> select(UUID uuid) {
        return find(new Document("unique_id", uuid.toString()), MailBoxDTO.class);
    }

    public void insert(MailBoxItem mailBoxItem) {
        int id = getNextId();
        MailBoxDTO dto = new MailBoxDTO(id, mailBoxItem.getUniqueId(),
                ItemStackUtils.serializeItemStack(mailBoxItem.getItemStack()),
                mailBoxItem.getExpiredAt(), new Date());
        insert(toDocument(dto));
        mailBoxItem.setId(id);
    }

    public void delete(int id) {
        delete(new Document("id", id));
    }

    public void deleteExpiredItems() {
        List<MailBoxDTO> items = findAll(MailBoxDTO.class);
        Date now = new Date();
        List<Integer> expiredIds = items.stream()
                .filter(item -> item.expired_at() != null && item.expired_at().before(now))
                .map(MailBoxDTO::id)
                .toList();
        if (!expiredIds.isEmpty()) {
            deleteMany(new Document("id", new Document("$in", expiredIds)));
        }
    }

    public void clear(UUID uuid) {
        deleteMany(new Document("unique_id", uuid.toString()));
    }

    private int getNextId() {
        Document doc = collection().find().sort(new Document("id", -1)).first();
        return doc == null ? 1 : doc.getInteger("id", 0) + 1;
    }
}
