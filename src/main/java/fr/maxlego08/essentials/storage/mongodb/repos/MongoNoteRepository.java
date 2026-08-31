package fr.maxlego08.essentials.storage.mongodb.repos;

import com.mongodb.client.MongoDatabase;
import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.dto.NoteDTO;
import fr.maxlego08.essentials.storage.mongodb.MongoRepository;
import org.bson.Document;

import java.util.List;
import java.util.UUID;

public class MongoNoteRepository extends MongoRepository {
    public MongoNoteRepository(EssentialsPlugin plugin, MongoDatabase database) {
        super(plugin, database, "notes");
    }

    public void upsert(NoteDTO note) {
        collection().insertOne(toDocument(note));
    }

    public void delete(UUID playerUuid, int noteIndex) {
        List<NoteDTO> notes = find(new Document("playerUuid", playerUuid.toString()), NoteDTO.class);
        if (noteIndex < 0 || noteIndex >= notes.size()) return;
        NoteDTO toDelete = notes.get(noteIndex);
        collection().deleteOne(new Document("playerUuid", playerUuid.toString())
                .append("staffUuid", toDelete.staffUuid().toString())
                .append("createdAt", toDelete.createdAt()));
    }

    public List<NoteDTO> selectByPlayer(UUID playerUuid) {
        return find(new Document("playerUuid", playerUuid.toString()), NoteDTO.class);
    }

    public void clearByPlayer(UUID playerUuid) {
        collection().deleteMany(new Document("playerUuid", playerUuid.toString()));
    }
}
