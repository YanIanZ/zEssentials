package fr.maxlego08.essentials.storage.mongodb.repos;

import com.mongodb.client.MongoDatabase;
import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.dto.HomeDTO;
import fr.maxlego08.essentials.api.dto.PublicHomeDTO;
import fr.maxlego08.essentials.api.home.Home;
import fr.maxlego08.essentials.storage.mongodb.MongoRepository;
import fr.maxlego08.essentials.user.ZHome;
import org.bson.Document;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

public class MongoUserHomeRepository extends MongoRepository {
    public MongoUserHomeRepository(EssentialsPlugin plugin, MongoDatabase database) {
        super(plugin, database, "homes");
    }

    public void upsert(UUID uuid, Home home) {
        Document filter = new Document("unique_id", uuid.toString()).append("name", home.getName());
        Document doc = new Document("unique_id", uuid.toString())
                .append("name", home.getName())
                .append("location", locationAsString(home.getLocation()))
                .append("material", home.getMaterial() != null ? home.getMaterial().name() : null)
                .append("is_public", home.isPublic())
                .append("category", home.getCategory())
                .append("is_favorite", home.isFavorite());
        upsert(filter, doc);
    }

    public void updateSocial(UUID uuid, Home home) {
        Document filter = new Document("unique_id", uuid.toString()).append("name", home.getName());
        Document set = new Document("is_public", home.isPublic())
                .append("category", home.getCategory())
                .append("is_favorite", home.isFavorite());
        collection().updateOne(filter, new Document("$set", set));
    }

    public List<HomeDTO> select(UUID uuid) {
        return find(new Document("unique_id", uuid.toString()), HomeDTO.class);
    }

    public List<PublicHomeDTO> selectPublicHomes() {
        return find(new Document("is_public", true), PublicHomeDTO.class);
    }

    public void deleteHome(UUID uuid, String name) {
        delete(new Document("unique_id", uuid.toString()).append("name", name));
    }

    public List<Home> getHomes(UUID uuid) {
        return select(uuid).stream().map(this::toHome).toList();
    }

    public List<Home> getHomes(UUID uuid, String homeName) {
        return getHomes(uuid).stream().filter(home -> home.getName().equalsIgnoreCase(homeName)).toList();
    }

    public void deleteWorldData(String worldName) {
        deleteMany(new Document("location", new Document("$regex", Pattern.quote(worldName))));
    }

    private Home toHome(HomeDTO homeDTO) {
        org.bukkit.Material material = null;
        if (homeDTO.material() != null) {
            try {
                material = org.bukkit.Material.valueOf(homeDTO.material());
            } catch (IllegalArgumentException ignored) {
            }
        }
        boolean isPublic = homeDTO.is_public() != null && homeDTO.is_public();
        boolean favorite = homeDTO.is_favorite() != null && homeDTO.is_favorite();
        return new ZHome(stringAsLocation(homeDTO.location()), homeDTO.name(), material, isPublic, homeDTO.category(), favorite);
    }
}
