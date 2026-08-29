package dev.yanianz.essentials.notes;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.commands.CommandResultType;
import fr.maxlego08.essentials.api.commands.Permission;
import fr.maxlego08.essentials.api.messages.Message;
import fr.maxlego08.essentials.zutils.utils.commands.VCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Manage the private staff notes attached to players: display them with the
 * player name as argument, add one with a text, or clear everything.
 */

public class CommandNotes extends VCommand {

    public CommandNotes(EssentialsPlugin plugin) {
        super(plugin);
        this.setModule(NotesModule.class);
        this.setPermission(Permission.ESSENTIALS_NOTES);
        this.setDescription(Message.DESCRIPTION_NOTES);
        this.addOptionalArg("action", (sender, args) -> List.of("add", "clear"));
        this.addOptionalArg("player", getVisiblePlayerNames());
        this.setExtendedArgs(true);
    }

    @Override
    protected CommandResultType perform(EssentialsPlugin plugin) {

        NotesModule module = plugin.getModuleManager().getModule(NotesModule.class);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");

        // /notes <player> : display the notes
        if (this.args.length == 1 && !this.argAsString(0).equalsIgnoreCase("clear")) {
            String username = this.argAsString(0);
            fetchUniqueId(username, uniqueId -> {
                if (uniqueId == null) {
                    message(sender, Message.PLAYER_NOT_FOUND, "%player%", username);
                    return;
                }
                var notes = module.getNotes(uniqueId);
                message(sender, Message.NOTES_HEADER, "%player%", username, "%count%", String.valueOf(notes.size()));
                for (var note : notes) {
                    String dateText = format.format(new java.util.Date(note.createdAt()));
                    message(sender, Message.NOTES_LINE, "%date%", dateText,
                            "%staff%", note.staffName(), "%note%", note.content());
                }
            });
            return CommandResultType.SUCCESS;
        }

        if (!hasPermission(sender, Permission.ESSENTIALS_NOTES_MANAGE)) {
            return CommandResultType.NO_PERMISSION;
        }

        String action = this.args.length == 0 ? "" : this.argAsString(0);

        if (action.equalsIgnoreCase("add")) {
            Player player = getPlayer();
            if (player == null || this.args.length < 4) {
                this.syntaxMessage();
                return CommandResultType.SUCCESS;
            }
            String username = this.argAsString(1);
            StringBuilder content = new StringBuilder();
            for (int index = 3; index < this.args.length; index++) {
                content.append(this.args[index]).append(index == this.args.length - 1 ? "" : " ");
            }

            fetchUniqueId(username, uniqueId -> {
                if (uniqueId == null) {
                    message(sender, Message.PLAYER_NOT_FOUND, "%player%", username);
                    return;
                }
                module.addNote(player.getUniqueId(), player.getName(), uniqueId, content.toString());
                message(sender, Message.NOTE_ADDED, "%player%", username);
            });
            return CommandResultType.SUCCESS;
        }

        if (action.equalsIgnoreCase("clear")) {
            if (this.args.length < 2) return CommandResultType.SYNTAX_ERROR;
            String username = this.argAsString(1);
            fetchUniqueId(username, uniqueId -> {
                if (uniqueId == null) {
                    message(sender, Message.PLAYER_NOT_FOUND, "%player%", username);
                    return;
                }
                int removed = module.clearNotes(uniqueId);
                message(sender, Message.NOTES_CLEARED, "%player%", username, "%count%", String.valueOf(removed));
            });
            return CommandResultType.SUCCESS;
        }

        this.syntaxMessage();
        return CommandResultType.SUCCESS;
    }
}
