package fr.maxlego08.essentials.storage.mongodb.repos;

import com.mongodb.client.MongoDatabase;
import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.dto.CommandDTO;
import fr.maxlego08.essentials.storage.mongodb.MongoRepository;
import org.bson.Document;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class MongoCommandsRepository extends MongoRepository {
    public MongoCommandsRepository(EssentialsPlugin plugin, MongoDatabase database) {
        super(plugin, database, "commands");
    }

    public List<CommandDTO> getCommands(UUID uuid) {
        return find(new Document("unique_id", uuid.toString()), CommandDTO.class).stream()
                .sorted(Comparator.comparing(CommandDTO::created_at, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    public void insertCommands(List<CommandDTO> commands) {
        if (commands.isEmpty()) return;
        insertMany(commands.stream().map(this::toDocument).toList());
    }
}
