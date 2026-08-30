package fr.maxlego08.essentials.storage.mongodb.repos;

import com.mongodb.client.MongoDatabase;
import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.dto.VoteSiteDTO;
import fr.maxlego08.essentials.storage.mongodb.MongoRepository;
import org.bson.Document;

import java.util.Date;
import java.util.List;
import java.util.UUID;

public class MongoVoteSiteRepository extends MongoRepository {
    public MongoVoteSiteRepository(EssentialsPlugin plugin, MongoDatabase database) {
        super(plugin, database, "vote_sites");
    }

    public List<VoteSiteDTO> select(UUID uniqueId) {
        return find(new Document("player_id", uniqueId.toString()), VoteSiteDTO.class);
    }

    public void setLastVote(UUID uniqueId, String site) {
        Document filter = new Document("player_id", uniqueId.toString()).append("site", site);
        Document doc = new Document("player_id", uniqueId.toString())
                .append("site", site)
                .append("last_vote_at", new Date());
        upsert(filter, doc);
    }
}
