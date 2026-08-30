package fr.maxlego08.essentials.storage.mongodb.repos;

import com.mongodb.client.MongoDatabase;
import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.dto.OptionDTO;
import fr.maxlego08.essentials.api.user.Option;
import fr.maxlego08.essentials.storage.mongodb.MongoRepository;
import org.bson.Document;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class MongoUserOptionRepository extends MongoRepository {
    public MongoUserOptionRepository(EssentialsPlugin plugin, MongoDatabase database) {
        super(plugin, database, "user_options");
    }

    public void upsert(UUID uuid, Option option, boolean optionValue) {
        Document filter = new Document("unique_id", uuid.toString()).append("option_name", option.name());
        Document doc = new Document("unique_id", uuid.toString())
                .append("option_name", option.name())
                .append("option_value", optionValue);
        upsert(filter, doc);
    }

    public List<OptionDTO> select(UUID uuid) {
        return find(new Document("unique_id", uuid.toString()), OptionDTO.class);
    }

    public void select(UUID uuid, Option option, Consumer<Boolean> consumer) {
        OptionDTO dto = findOne(new Document("unique_id", uuid.toString()).append("option_name", option.name()), OptionDTO.class);
        consumer.accept(dto != null && dto.option_value());
    }
}
