package fr.maxlego08.essentials.storage.mongodb.repos;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.dto.FlyDTO;
import fr.maxlego08.essentials.api.dto.SanctionDTO;
import fr.maxlego08.essentials.api.dto.UserDTO;
import fr.maxlego08.essentials.api.dto.UserVoteDTO;
import fr.maxlego08.essentials.api.user.User;
import fr.maxlego08.essentials.storage.mongodb.MongoRepository;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

public class MongoUserRepository extends MongoRepository {
    public MongoUserRepository(EssentialsPlugin plugin, MongoDatabase database) {
        super(plugin, database, "users");
    }

    public void upsert(UUID uuid, String name) {
        collection().updateOne(
                new Document("unique_id", uuid.toString()),
                new Document("$set", new Document("name", name)),
                new UpdateOptions().upsert(true)
        );
    }

    public void upsert(User user) {
        Document set = new Document("name", user.getName());
        if (user.getLastLocation() != null) {
            set.append("last_location", locationAsString(user.getLastLocation()));
        }
        collection().updateOne(
                new Document("unique_id", user.getUniqueId().toString()),
                new Document("$set", set),
                new UpdateOptions().upsert(true)
        );
    }

    public void updateName(UUID uuid, String name) {
        collection().updateOne(new Document("unique_id", uuid.toString()), new Document("$set", new Document("name", name)));
    }

    public void updateBanId(UUID uuid, Integer index) {
        collection().updateOne(new Document("unique_id", uuid.toString()), new Document("$set", new Document("ban_sanction_id", index)));
    }

    public void updateMuteId(UUID uuid, Integer index) {
        collection().updateOne(new Document("unique_id", uuid.toString()), new Document("$set", new Document("mute_sanction_id", index)));
    }

    public void updatePlayTime(UUID uniqueId, long playTime) {
        collection().updateOne(new Document("unique_id", uniqueId.toString()), new Document("$set", new Document("play_time", playTime)));
    }

    public void updateFrozen(UUID uuid, boolean frozen) {
        collection().updateOne(new Document("unique_id", uuid.toString()), new Document("$set", new Document("frozen", frozen)));
    }

    public void updateFly(UUID uniqueId, long flySeconds) {
        collection().updateOne(new Document("unique_id", uniqueId.toString()), new Document("$set", new Document("fly_seconds", flySeconds)));
    }

    public void upsertFly(List<FlyDTO> flights) {
        flights.forEach(e -> updateFly(e.unique_id(), e.fly_seconds()));
    }

    public long selectFly(UUID uniqueId) {
        var users = selectUser(uniqueId);
        return users.isEmpty() ? 0 : users.getFirst().fly_seconds();
    }

    public void updatePlayerTimeWeather(UUID uniqueId, long playerTime, String playerWeather) {
        collection().updateOne(new Document("unique_id", uniqueId.toString()),
                new Document("$set", new Document("player_time", playerTime).append("player_weather", playerWeather)));
    }

    public void setVote(UUID uniqueId, long vote, long offline) {
        Document set = new Document();
        if (vote >= 0) set.append("vote", vote);
        if (offline >= 0) set.append("vote_offline", offline);
        if (!set.isEmpty()) {
            collection().updateOne(new Document("unique_id", uniqueId.toString()), new Document("$set", set));
        }
    }

    public void resetVotes() {
        collection().updateMany(new Document(), new Document("$set", new Document("vote", 0)));
    }

    public List<UserDTO> selectUser(UUID uniqueId) {
        return find(new Document("unique_id", uniqueId.toString()), UserDTO.class);
    }

    public List<UserDTO> selectUsers(String userName) {
        List<UserDTO> users = find(new Document("name", userName), UserDTO.class);
        users.sort(Comparator.comparing(UserDTO::updated_at, Comparator.nullsLast(Comparator.<Date>reverseOrder())));
        return users;
    }

    public List<UserDTO> getUsers(String ip) {
        MongoCollection<Document> playTimeCol = database().getCollection("play_time");
        List<String> uuidStrings = new ArrayList<>();
        for (Document doc : playTimeCol.find(new Document("address", ip))) {
            String uuid = doc.getString("unique_id");
            if (uuid != null && !uuidStrings.contains(uuid)) uuidStrings.add(uuid);
        }
        if (uuidStrings.isEmpty()) return new ArrayList<>();
        return find(new Document("unique_id", new Document("$in", uuidStrings)), UserDTO.class);
    }

    public List<UserVoteDTO> selectVoteUser(UUID uniqueId) {
        return find(new Document("unique_id", uniqueId.toString()), UserVoteDTO.class);
    }

    public boolean exists(UUID uniqueId) {
        return !selectUser(uniqueId).isEmpty();
    }

    public long totalUsers() {
        return countAll();
    }

    public List<UserDTO> selectAll() {
        return findAll(UserDTO.class);
    }

    public List<String> getPlayerNames() {
        return findAll(UserDTO.class).stream().map(UserDTO::name).toList();
    }

    public Collection<UUID> selectUUIDs() {
        return findAll(UserDTO.class).stream().map(UserDTO::unique_id).toList();
    }

    public void deleteWorldData(String worldName) {
        collection().updateMany(
                new Document("last_location", new Document("$regex", Pattern.quote(worldName))),
                new Document("$set", new Document("last_location", null))
        );
    }

    public void clearExpiredSanctions() {
        MongoCollection<Document> sanctionsCol = database().getCollection("sanctions");
        List<Integer> expiredIds = new ArrayList<>();
        for (Document doc : sanctionsCol.find()) {
            SanctionDTO dto = gson.fromJson(doc.toJson(), SanctionDTO.class);
            if (dto != null && !dto.isActive()) expiredIds.add(dto.id());
        }
        if (expiredIds.isEmpty()) return;
        for (Integer id : expiredIds) {
            collection().updateMany(new Document("ban_sanction_id", id), new Document("$set", new Document("ban_sanction_id", null)));
            collection().updateMany(new Document("mute_sanction_id", id), new Document("$set", new Document("mute_sanction_id", null)));
        }
    }
}
