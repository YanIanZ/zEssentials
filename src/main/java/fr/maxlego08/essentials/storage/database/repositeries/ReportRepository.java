package fr.maxlego08.essentials.storage.database.repositeries;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.dto.ReportDTO;
import fr.maxlego08.essentials.storage.database.Repository;
import fr.maxlego08.sarah.DatabaseConnection;

import java.util.List;
import java.util.UUID;

public class ReportRepository extends Repository {
    public ReportRepository(EssentialsPlugin plugin, DatabaseConnection connection) {
        super(plugin, connection, "reports");
    }
    public void upsert(ReportDTO report) {
        if (report.id() <= 0) {
            insert(table -> {
                table.uuid("reporter_uuid", report.reporterUuid());
                table.string("reporter_name", report.reporterName());
                table.uuid("target_uuid", report.targetUuid());
                table.string("target_name", report.targetName());
                table.string("reason", report.reason());
                table.bigInt("created_at", report.createdAt());
                table.bool("resolved", report.resolved());
            });
        } else {
            update(table -> {
                table.uuid("reporter_uuid", report.reporterUuid());
                table.string("reporter_name", report.reporterName());
                table.uuid("target_uuid", report.targetUuid());
                table.string("target_name", report.targetName());
                table.string("reason", report.reason());
                table.bigInt("created_at", report.createdAt());
                table.bool("resolved", report.resolved());
                table.where("id", report.id());
            });
        }
    }
    public void delete(int id) {
        delete(table -> table.where("id", id));
    }
    public List<ReportDTO> selectAll() {
        return selectAll(ReportDTO.class);
    }
    public List<ReportDTO> selectByTarget(UUID targetUuid) {
        return select(ReportDTO.class, table -> table.where("target_uuid", targetUuid.toString()));
    }
}
