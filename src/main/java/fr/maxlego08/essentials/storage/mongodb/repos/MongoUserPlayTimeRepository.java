package fr.maxlego08.essentials.storage.mongodb.repos;

import com.mongodb.client.MongoDatabase;
import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.dto.PlayTimeDTO;
import fr.maxlego08.essentials.storage.mongodb.MongoRepository;
import org.bson.Document;

import java.util.Date;
import java.util.List;
import java.util.UUID;

public class MongoUserPlayTimeRepository extends MongoRepository {
    public MongoUserPlayTimeRepository(EssentialsPlugin plugin, MongoDatabase database) {
        super(plugin, database, "play_time");
    }

    public void insert(UUID uuid, long playtime, String address) {
        PlayTimeDTO dto = new PlayTimeDTO(uuid, playtime, address, new Date());
        insert(toDocument(dto));
    }

    public List<PlayTimeDTO> select(UUID uuid) {
        return find(new Document("unique_id", uuid.toString()), PlayTimeDTO.class);
    }
}
