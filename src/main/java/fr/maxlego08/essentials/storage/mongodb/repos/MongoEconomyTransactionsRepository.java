package fr.maxlego08.essentials.storage.mongodb.repos;

import com.mongodb.client.MongoDatabase;
import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.dto.EconomyTransactionDTO;
import fr.maxlego08.essentials.api.economy.Economy;
import fr.maxlego08.essentials.storage.mongodb.MongoRepository;
import org.bson.Document;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class MongoEconomyTransactionsRepository extends MongoRepository {
    public MongoEconomyTransactionsRepository(EssentialsPlugin plugin, MongoDatabase database) {
        super(plugin, database, "economy_transactions");
    }

    public List<EconomyTransactionDTO> selectTransactions(UUID toUuid, Economy economy) {
        return find(new Document("to_unique_id", toUuid.toString()).append("economy_name", economy.getName()), EconomyTransactionDTO.class)
                .stream()
                .map(dto -> new EconomyTransactionDTO(dto.from_unique_id(), dto.to_unique_id(), dto.economy_name(),
                        dto.reason() == null ? "No reason" : dto.reason(), dto.amount(), dto.from_amount(),
                        dto.to_amount(), dto.created_at(), dto.updated_at()))
                .collect(Collectors.toList());
    }

    public void insertTransactions(List<EconomyTransactionDTO> transactions) {
        if (transactions.isEmpty()) return;
        insertMany(transactions.stream().map(transaction -> {
            if (transaction.reason() == null) {
                transaction = new EconomyTransactionDTO(transaction.from_unique_id(), transaction.to_unique_id(),
                        transaction.economy_name(), "No reason", transaction.amount(), transaction.from_amount(),
                        transaction.to_amount(), transaction.created_at(), transaction.updated_at());
            }
            return toDocument(transaction);
        }).toList());
    }
}
