package dev.yanianz.essentials.notes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fr.maxlego08.essentials.ZEssentialsPlugin;
import fr.maxlego08.essentials.api.configuration.NonLoadable;
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
 * Private staff notes attached to players, persisted in a json file.
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
        loadStorage();
    }

    /**
     * Adds a note to a player, written by a staff member.
     */
    public void addNote(UUID staffUuid, String staffName, UUID target, String content) {
        this.notes.computeIfAbsent(target, k -> new ArrayList<>())
                .add(new StaffNote(staffUuid, staffName, System.currentTimeMillis(), content));
        saveStorage();
    }

    public List<StaffNote> getNotes(UUID target) {
        return new ArrayList<>(this.notes.getOrDefault(target, new ArrayList<>()));
    }

    /**
     * Removes every note of a player.
     *
     * @return how many notes were removed.
     */
    public int clearNotes(UUID target) {
        List<StaffNote> removed = this.notes.remove(target);
        if (removed != null && !removed.isEmpty()) {
            saveStorage();
            return removed.size();
        }
        return 0;
    }

    private File getStorageFile() {
        return new File(getFolder(), "notes.json");
    }

    private void loadStorage() {
        File file = getStorageFile();
        if (!file.exists()) return;
        try {
            String json = Files.readString(file.toPath());
            RawStorage raw = this.gson.fromJson(json, RawStorage.class);
            if (raw == null || raw.entries == null) return;

            for (Map.Entry<String, List<RawNote>> entry : raw.entries.entrySet()) {
                try {
                    UUID uniqueId = UUID.fromString(entry.getKey());
                    List<StaffNote> list = new ArrayList<>();
                    for (RawNote rawNote : entry.getValue()) {
                        list.add(new StaffNote(UUID.fromString(rawNote.staff_uuid),
                                rawNote.staff_name, rawNote.created_at, rawNote.content));
                    }
                    this.notes.put(uniqueId, list);
                } catch (IllegalArgumentException ignored) {
                }
            }
        } catch (IOException | RuntimeException exception) {
            exception.printStackTrace();
        }
    }

    private void saveStorage() {
        RawStorage raw = new RawStorage();
        for (Map.Entry<UUID, List<StaffNote>> entry : this.notes.entrySet()) {
            List<RawNote> list = new ArrayList<>();
            for (StaffNote note : entry.getValue()) {
                RawNote rawNote = new RawNote();
                rawNote.staff_uuid = note.staffUuid().toString();
                rawNote.staff_name = note.staffName();
                rawNote.created_at = note.createdAt();
                rawNote.content = note.content();
                list.add(rawNote);
            }
            raw.entries.put(entry.getKey().toString(), list);
        }
        try {
            Files.writeString(getStorageFile().toPath(), this.gson.toJson(raw));
        } catch (IOException exception) {
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
