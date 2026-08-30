package fr.maxlego08.essentials.storage.mongodb.repos;

import com.mongodb.client.MongoDatabase;
import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.discord.DiscordAction;
import fr.maxlego08.essentials.storage.mongodb.MongoRepository;
import org.bson.Document;

import java.util.UUID;

public class MongoLinkHistoryRepository extends MongoRepository {
    public MongoLinkHistoryRepository(EssentialsPlugin plugin, MongoDatabase database) {
        super(plugin, database, "link_history");
    }

    public void insertLog(DiscordAction action, UUID uniqueId, String minecraftName, String discordName, long userId, String data) {
        Document doc = new Document("action", action.name());
        if (uniqueId != null) doc.append("minecraft_id", uniqueId.toString());
        if (minecraftName != null) doc.append("minecraft_name", minecraftName);
        if (discordName != null) doc.append("discord_name", discordName);
        if (userId != -1) doc.append("discord_id", userId);
        if (data != null) doc.append("data", data);
        insert(doc);
    }
}
