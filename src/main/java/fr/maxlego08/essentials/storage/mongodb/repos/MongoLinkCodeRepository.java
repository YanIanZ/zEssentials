package fr.maxlego08.essentials.storage.mongodb.repos;

import com.mongodb.client.MongoDatabase;
import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.dto.DiscordCodeDTO;
import fr.maxlego08.essentials.storage.mongodb.MongoRepository;
import org.bson.Document;

import java.util.Optional;

public class MongoLinkCodeRepository extends MongoRepository {
    public MongoLinkCodeRepository(EssentialsPlugin plugin, MongoDatabase database) {
        super(plugin, database, "link_codes");
    }

    public Optional<DiscordCodeDTO> getCode(String code) {
        return Optional.ofNullable(findOne(new Document("code", code), DiscordCodeDTO.class));
    }

    public void clearCode(DiscordCodeDTO code) {
        delete(new Document("code", code.code()));
    }
}
