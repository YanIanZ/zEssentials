package fr.maxlego08.essentials.storage.database.repositeries;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.dto.NoteDTO;
import fr.maxlego08.essentials.storage.database.Repository;
import fr.maxlego08.sarah.DatabaseConnection;

import java.util.List;
import java.util.UUID;

public class NoteRepository extends Repository {
    public NoteRepository(EssentialsPlugin plugin, DatabaseConnection connection) {
        super(plugin, connection, "notes");
    }
    public void upsert(NoteDTO note) {
        insert(table -> {
            table.uuid("player_uuid", note.playerUuid());
            table.uuid("staff_uuid", note.staffUuid());
            table.string("staff_name", note.staffName());
            table.bigInt("created_at", note.createdAt());
            table.string("content", note.content());
        });
    }
    public void delete(UUID playerUuid, int noteIndex) {
        // Delete by row index for this player (ordered by created_at)
        List<NoteDTO> notes = select(NoteDTO.class, table -> table.where("player_uuid", playerUuid.toString()));
        if (noteIndex < 0 || noteIndex >= notes.size()) return;
        NoteDTO toDelete = notes.get(noteIndex);
        delete(table -> {
            table.where("player_uuid", playerUuid.toString());
            table.where("created_at", toDelete.createdAt());
            table.where("staff_uuid", toDelete.staffUuid().toString());
        });
    }
    public List<NoteDTO> selectByPlayer(UUID playerUuid) {
        return select(NoteDTO.class, table -> table.where("player_uuid", playerUuid.toString()));
    }
    public void clearByPlayer(UUID playerUuid) {
        delete(table -> table.where("player_uuid", playerUuid.toString()));
    }
}
