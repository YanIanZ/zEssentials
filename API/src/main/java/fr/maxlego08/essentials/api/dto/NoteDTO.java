package fr.maxlego08.essentials.api.dto;

import java.util.UUID;

public record NoteDTO(UUID playerUuid, UUID staffUuid, String staffName, long createdAt, String content) {
}
