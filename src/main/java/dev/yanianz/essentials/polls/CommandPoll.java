package dev.yanianz.essentials.polls;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.commands.CommandResultType;
import fr.maxlego08.essentials.api.commands.Permission;
import fr.maxlego08.essentials.api.messages.Message;
import fr.maxlego08.essentials.zutils.utils.commands.VCommand;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Creates and manages the chat polls:
 * create with a duration seconds argument followed by the question and the
 * options separated by pipes, vote with an index, stop to end it early.
 */

public class CommandPoll extends VCommand {

    public CommandPoll(EssentialsPlugin plugin) {
        super(plugin);
        this.setModule(dev.yanianz.essentials.polls.PollsModule.class);
        this.setPermission(Permission.ESSENTIALS_POLLS_USE);
        this.setDescription(Message.DESCRIPTION_POLL);
        this.addOptionalArg("action", (sender, args) -> List.of("create", "vote", "stop"));
        this.addOptionalArg("value");
        this.setExtendedArgs(true);
    }

    @Override
    protected CommandResultType perform(EssentialsPlugin plugin) {

        PollsModule pollsModule = plugin.getModuleManager().getModule(PollsModule.class);

        String action = this.args.length == 0 ? "vote" : this.argAsString(0).toLowerCase();

        switch (action) {
            case "create" -> {
                if (!hasPermission(sender, Permission.ESSENTIALS_POLLS_ADMIN)) {
                    return CommandResultType.NO_PERMISSION;
                }
                if (this.args.length < 2) return CommandResultType.SYNTAX_ERROR;

                int duration = args.length >= 3 ? parseSeconds(this.argAsString(1)) : 0;
                String payload = String.join(" ", java.util.Arrays.copyOfRange(this.args, duration > 0 ? 2 : 1, this.args.length));

                String[] segments = payload.split("\\|");
                if (segments.length < 3) {
                    message(sender, Message.POLL_INVALID_USAGE);
                    return CommandResultType.SUCCESS;
                }
                String question = segments[0].trim();
                var options = new java.util.ArrayList<String>();
                for (int index = 1; index < segments.length; index++) {
                    options.add(segments[index].trim());
                }

                boolean created = pollsModule.createPoll(question, options, duration);
                if (!created) {
                    message(sender, Message.POLL_CREATE_FAILED);
                }
            }
            case "stop" -> {
                if (!hasPermission(sender, Permission.ESSENTIALS_POLLS_ADMIN)) {
                    return CommandResultType.NO_PERMISSION;
                }
                pollsModule.finishPoll();
            }
            case "vote" -> {
                Player player = getPlayer();
                if (player == null) return CommandResultType.SYNTAX_ERROR;
                if (this.args.length < 2) return CommandResultType.SYNTAX_ERROR;
                try {
                    pollsModule.vote(player, Integer.parseInt(this.argAsString(1)));
                } catch (NumberFormatException exception) {
                    this.syntaxMessage();
                }
            }
            default -> {
                Player player = getPlayer();
                if (player == null) return CommandResultType.SYNTAX_ERROR;
                try {
                    // Bare `/poll 2` also counts as a vote
                    pollsModule.vote(player, Integer.parseInt(action));
                } catch (NumberFormatException exception) {
                    this.syntaxMessage();
                }
            }
        }

        return CommandResultType.SUCCESS;
    }

    private int parseSeconds(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (Exception exception) {
            return -1;
        }
    }
}
