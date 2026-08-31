package fr.maxlego08.essentials.api.dto;

import java.util.UUID;

public record ReportDTO(int id, UUID reporterUuid, String reporterName, UUID targetUuid, String targetName, String reason, long createdAt, boolean resolved) {
}
