package fr.maxlego08.essentials.storage.mongodb.repos;

import com.mongodb.client.MongoDatabase;
import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.dto.VaultDTO;
import fr.maxlego08.essentials.storage.mongodb.MongoRepository;
import org.bson.Document;

import java.util.List;
import java.util.UUID;

public class MongoVaultRepository extends MongoRepository {
    public MongoVaultRepository(EssentialsPlugin plugin, MongoDatabase database) {
        super(plugin, database, "vaults");
    }

    public List<VaultDTO> select() {
        return findAll(VaultDTO.class);
    }

    public void update(UUID uniqueId, int vaultId, String name, String icon) {
        Document filter = new Document("unique_id", uniqueId.toString()).append("vault_id", vaultId);
        Document doc = new Document("unique_id", uniqueId.toString())
                .append("vault_id", vaultId)
                .append("name", name)
                .append("icon", icon);
        upsert(filter, doc);
    }
}
