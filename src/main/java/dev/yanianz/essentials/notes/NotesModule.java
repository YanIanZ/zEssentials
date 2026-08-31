package dev.yanianz.essentials.notes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fr.maxlego08.essentials.ZEssentialsPlugin;
import fr.maxlego08.essentials.api.configuration.NonLoadable;
import fr.maxlego08.essentials.api.dto.NoteDTO;
import fr.maxlego08.essentials.module.ZModule;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Private staff notes attached to players, persisted via IStorage.
 */
public class NotesModule extends ZModule {

    @NonLoadable
    private final Map<UUID, List<StaffNote>> notes = new ConcurrentHashMap<>();
    @NonLoadable
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public NotesModule(ZEssentialsPlugin plugin) {
        super(plugin, "notes");
    }

    @Override
    public void loadConfiguration() {
        super.loadConfiguration();
        this.notes.clear();
        migrateOldStorage();
        // Notes are loaded lazily per-player via getNotes()
    }

    /**
     * Adds a note to a player, written by a staff member.
     */
    public void addNote(UUID staffUuid, String staffName, UUID target, String content) {
        long now = System.currentTimeMillis();
        this.notes.computeIfAbsent(target, k -> new ArrayList<>())
                .add(new StaffNote(staffUuid, staffName, now, content));
        getStorage().upsertNote(new NoteDTO(target, staffUuid, staffName, now, content));
    }

    public List<StaffNote> getNotes(UUID target) {
        return new ArrayList<>(this.notes.computeIfAbsent(target, k -> {
            List<StaffNote> list = new ArrayList<>();
            for (NoteDTO dto : getStorage().getNotes(target)) {
                list.add(new StaffNote(dto.staffUuid(), dto.staffName(), dto.createdAt(), dto.content()));
            }
            return list;
        }));
    }

    /**
     * Removes every note of a player.
     *
     * @return how many notes were removed.
     */
    public int clearNotes(UUID target) {
        List<StaffNote> removed = this.notes.remove(target);
        int count = removed != null ? removed.size() : 0;
        getStorage().clearNotes(target);
        return count;
    }

    private File getStorageFile() {
        return new File(getFolder(), "notes.json");
    }

    private void migrateOldStorage() {
        File file = getStorageFile();
        if (!file.exists()) return;
        try {
            String json = Files.readString(file.toPath());
            RawStorage raw = this.gson.fromJson(json, RawStorage.class);
            if (raw != null && raw.entries != null) {
                for (Map.Entry<String, List<RawNote>> entry : raw.entries.entrySet()) {
                    try {
                        UUID playerUuid = UUID.fromString(entry.getKey());
                        for (RawNote rawNote : entry.getValue()) {
                            getStorage().upsertNote(new NoteDTO(playerUuid,
                                    UUID.fromString(rawNote.staff_uuid), rawNote.staff_name,
                                    rawNote.created_at, rawNote.content));
                        }
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
            File migrated = new File(getFolder(), "notes.json.migrated");
            if (!file.renameTo(migrated)) {
                file.delete();
            }
        } catch (IOException | RuntimeException exception) {
            exception.printStackTrace();
        }
    }

    public record StaffNote(UUID staffUuid, String staffName, long createdAt, String content) {
    }

    private static final class RawStorage {
        Map<String, List<RawNote>> entries = new HashMap<>();
    }

    private static final class RawNote {
        String staff_uuid;
        String staff_name;
        long created_at;
        String content;
    }
}
