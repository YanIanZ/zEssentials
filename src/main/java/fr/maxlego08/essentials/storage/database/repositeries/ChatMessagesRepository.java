package fr.maxlego08.essentials.storage.database.repositeries;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.dto.ChatMessageDTO;
import fr.maxlego08.essentials.storage.database.Repository;
import fr.maxlego08.sarah.DatabaseConnection;

import java.util.List;
import java.util.UUID;

public class ChatMessagesRepository extends Repository {
    public ChatMessagesRepository(EssentialsPlugin plugin, DatabaseConnection connection) {
        super(plugin, connection, "chat_message");
    }

    /*public void insert(ChatMessageDTO chatMessage) {
        insert(table -> {
            table.uuid("unique_id", chatMessage.unique_id());
            table.string("content", chatMessage.content());
        });
    }*/

    public List<ChatMessageDTO> getMessages(UUID uuid) {
        return this.select(ChatMessageDTO.class, table -> {
            table.uuid("unique_id", uuid);
            table.orderByDesc("created_at");
        });
    }

    public void insertMessages(List<ChatMessageDTO> messages) {
        insert(messages.stream().map(dto -> schema(table -> {
            table.uuid("unique_id", dto.unique_id());
            table.string("content", dto.content());
        })).toList());
    }

    /**
     * Deletes every stored message of a player matching the exact content,
     * used by the staff message deletion.
     */
    public int deleteMessages(UUID uuid, String content) {
        return this.delete(table -> {
            table.uuid("unique_id", uuid);
            table.string("content", content);
        });
    }
}
