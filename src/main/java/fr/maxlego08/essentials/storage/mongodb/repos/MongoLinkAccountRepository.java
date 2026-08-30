package fr.maxlego08.essentials.storage.mongodb.repos;

import com.mongodb.client.MongoDatabase;
import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.dto.DiscordAccountDTO;
import fr.maxlego08.essentials.storage.mongodb.MongoRepository;
import org.bson.Document;

import java.util.Optional;
import java.util.UUID;

public class MongoLinkAccountRepository extends MongoRepository {
    public MongoLinkAccountRepository(EssentialsPlugin plugin, MongoDatabase database) {
        super(plugin, database, "link_accounts");
    }

    public void insert(UUID uniqueId, String minecraftName, String discordName, long userId) {
        DiscordAccountDTO dto = new DiscordAccountDTO(userId, uniqueId, minecraftName, discordName, new java.sql.Timestamp(System.currentTimeMillis()));
        insert(toDocument(dto));
    }

    public Optional<DiscordAccountDTO> select(UUID uniqueId) {
        return Optional.ofNullable(findOne(new Document("unique_id", uniqueId.toString()), DiscordAccountDTO.class));
    }

    public void delete(UUID uniqueId) {
        delete(new Document("unique_id", uniqueId.toString()));
    }
}
