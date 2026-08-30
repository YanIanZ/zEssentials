package fr.maxlego08.essentials.storage.mongodb.repos;

import com.mongodb.client.MongoDatabase;
import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.dto.VaultItemDTO;
import fr.maxlego08.essentials.storage.mongodb.MongoRepository;
import org.bson.Document;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class MongoVaultItemRepository extends MongoRepository {
    public MongoVaultItemRepository(EssentialsPlugin plugin, MongoDatabase database) {
        super(plugin, database, "vault_items");
    }

    public List<VaultItemDTO> select() {
        return findAll(VaultItemDTO.class);
    }

    public Optional<VaultItemDTO> select(UUID uniqueId, int vaultId, int slot) {
        return find(filter(uniqueId, vaultId, slot), VaultItemDTO.class).stream().findFirst();
    }

    public void updateQuantity(UUID uniqueId, int vaultId, int slot, long quantity) {
        collection().updateOne(filter(uniqueId, vaultId, slot),
                new Document("$set", new Document("quantity", quantity)));
    }

    public void createNewItem(UUID uniqueId, int vaultId, int slot, long quantity, String item) {
        VaultItemDTO dto = new VaultItemDTO(uniqueId, vaultId, slot, item, quantity);
        insert(toDocument(dto));
    }

    public void removeItem(UUID uniqueId, int vaultId, int slot) {
        delete(filter(uniqueId, vaultId, slot));
    }

    public boolean forceRemove(UUID uniqueId, int vaultId, int slot) {
        return collection().deleteOne(filter(uniqueId, vaultId, slot)).getDeletedCount() > 0;
    }

    private Document filter(UUID uniqueId, int vaultId, int slot) {
        return new Document("unique_id", uniqueId.toString())
                .append("vault_id", vaultId)
                .append("slot", slot);
    }
}
