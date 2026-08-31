package fr.maxlego08.essentials.storage.mongodb.repos;

import com.mongodb.client.MongoDatabase;
import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.dto.ReportDTO;
import fr.maxlego08.essentials.storage.mongodb.MongoRepository;
import org.bson.Document;

import java.util.List;
import java.util.UUID;

public class MongoReportRepository extends MongoRepository {
    public MongoReportRepository(EssentialsPlugin plugin, MongoDatabase database) {
        super(plugin, database, "reports");
    }

    public void upsert(ReportDTO report) {
        Document filter = new Document("id", report.id());
        Document doc = toDocument(report);
        upsert(filter, doc);
    }

    public void delete(int id) {
        delete(new Document("id", id));
    }

    public List<ReportDTO> selectAll() {
        return findAll(ReportDTO.class);
    }

    public List<ReportDTO> selectByTarget(UUID targetUuid) {
        return find(new Document("targetUuid", targetUuid.toString()), ReportDTO.class);
    }
}
