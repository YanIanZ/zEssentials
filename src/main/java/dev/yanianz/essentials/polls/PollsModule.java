package dev.yanianz.essentials.polls;

import fr.maxlego08.essentials.ZEssentialsPlugin;
import fr.maxlego08.essentials.api.configuration.NonLoadable;
import fr.maxlego08.essentials.module.ZModule;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Staff created chat polls: every option is a clickable line, one vote per
 * player and the results with percentage bars broadcast when it ends.
 */
public class PollsModule extends ZModule {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private int maxDuration;
    private int defaultDuration;
    private String votePermission;

    private String headerStyle;
    private String questionStyle;
    private String optionLine;
    private String votedMark;
    private String footerStyle;
    private String resultBar;
    private String winnerLine;
    private String noVotesLine;
    private String barFilled;
    private String barEmpty;
    private int barLength;

    @NonLoadable
    private ActivePoll activePoll;
    @NonLoadable
    private com.tcoded.folialib.wrapper.task.WrappedTask closeTask;

    public PollsModule(ZEssentialsPlugin plugin) {
        super(plugin, "polls");
    }

    private static final class ActivePoll {
        final String question;
        final List<String> options;
        final Map<Integer, Set<UUID>> votes = new HashMap<>();
        final long endAtMillis;

        ActivePoll(String question, List<String> options, long endAtMillis) {
            this.question = question;
            this.options = options;
            this.endAtMillis = endAtMillis;
        }
    }

    @Override
    public void loadConfiguration() {
        super.loadConfiguration();

        var config = getConfiguration();
        this.maxDuration = config.getInt("max-duration", 600);
        this.defaultDuration = config.getInt("default-duration", 60);
        this.votePermission = config.getString("vote-permission", "");
        this.headerStyle = config.getString("style.header", "&lPOLL");
        this.questionStyle = config.getString("style.question", "&e%question%");
        this.optionLine = config.getString("style.option-line", "  [%index%] %option% (%votes%)");
        this.votedMark = config.getString("style.voted-mark", " ✔");
        this.footerStyle = config.getString("style.footer", "");
        this.resultBar = config.getString("style.result-bar", "  [%index%] %option% %percent%%");
        this.winnerLine = config.getString("style.winner", "&aWinner: %option%");
        this.noVotesLine = config.getString("style.no-votes", "&7The poll ended without any vote.");
        this.barFilled = config.getString("colors.bar-filled", "|");
        this.barEmpty = config.getString("colors.bar-empty", "¦");
        this.barLength = Math.max(4, config.getInt("colors.bar-length", 20));

        // A reload cancels the running poll
        cancelCloseTask();
        this.activePoll = null;
    }

    /**
     * Creates a new poll replacing the previous one.
     *
     * @param durationSeconds poll length in seconds, clamped by the configuration
     * @return false when the arguments are invalid.
     */
    public boolean createPoll(String question, List<String> options, int durationSeconds) {

        if (!this.isEnable) return false;
        if (question == null || question.isBlank()) return false;
        if (options == null || options.size() < 2) return false;

        cancelCloseTask();
        long seconds = Math.min(Math.max(10, durationSeconds <= 0 ? this.defaultDuration : durationSeconds), this.maxDuration);

        ActivePoll poll = new ActivePoll(question.trim(), List.copyOf(options), System.currentTimeMillis() + seconds * 1000L);
        for (int index = 0; index < poll.options.size(); index++) poll.votes.put(index, new HashSet<>());

        this.activePoll = poll;

        scheduleClose(seconds);
        broadcastPoll();
        return true;
    }

    private void scheduleClose(long seconds) {
        this.closeTask = this.plugin.getScheduler().runLater(this::finishPoll, seconds * 20L + 10L);
    }

    private void cancelCloseTask() {
        if (this.closeTask != null) {
            this.closeTask.cancel();
            this.closeTask = null;
        }
    }

    /**
     * Registers the vote of a player on an option, one vote per player.
     */
    public void vote(Player player, int optionIndex) {

        ActivePoll poll = this.activePoll;
        if (poll == null) {
            player.sendMessage(legacy("&cThere is no open poll."));
            return;
        }
        optionIndex -= 1; // commands are one based for the players
        if (optionIndex < 0 || optionIndex >= poll.options.size()) {
            player.sendMessage(legacy("&cUnknown poll option."));
            return;
        }
        if (!this.votePermission.isEmpty() && !player.hasPermission(this.votePermission)) {
            player.sendMessage(legacy("&cYou cannot vote on this poll."));
            return;
        }

        poll.votes.values().forEach(set -> set.remove(player.getUniqueId()));
        poll.votes.get(optionIndex).add(player.getUniqueId());

        long remaining = Math.max(0, (poll.endAtMillis - System.currentTimeMillis()) / 1000L);
        player.sendMessage(legacy("&aVote registered for &f" + poll.options.get(optionIndex) + "&a! &7" + remaining + "s left"));
        sendPollView(player);
    }

    /**
     * Broadcasts the current state of the poll to everyone.
     */
    public void broadcastPoll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            sendPollView(player);
        }
        console(this.headerStyle.replace("%question%", currentQuestion()));
    }

    /**
     * Ends the poll and announces the results.
     */
    public void finishPoll() {

        ActivePoll poll = this.activePoll;
        if (poll == null) return;
        this.activePoll = null;
        cancelCloseTask();

        announce(Component.empty());

        int totalVotes = poll.votes.values().stream().mapToInt(Set::size).sum();

        int bestIndex = -1;
        int bestCount = -1;
        boolean tie = false;

        for (int index = 0; index < poll.options.size(); index++) {
            int count = poll.votes.get(index).size();
            double percent = totalVotes == 0 ? 0 : count * 100.0 / totalVotes;
            int filled = (int) Math.round(percent / 100.0 * this.barLength);

            String bar = this.barFilled.repeat(filled)
                    + colorize("&8") + this.barEmpty.repeat(Math.max(0, this.barLength - filled));
            String lineText = this.resultBar
                    .replace("%index%", String.valueOf(index + 1))
                    .replace("%option%", poll.options.get(index))
                    .replace("%bar%", bar)
                    .replace("%percent%", String.format(Locale.US, "%.0f", percent))
                    .replace("%votes%", String.valueOf(count));

            announce(legacy(lineText));

            if (count > bestCount) {
                bestCount = count;
                bestIndex = index;
                tie = false;
            } else if (count == bestCount && count > 0) {
                tie = true;
            }
        }

        Component outcome;
        if (totalVotes == 0) {
            outcome = legacy(this.noVotesLine);
        } else if (tie) {
            outcome = legacy(colorize("&e&lPOLL RESULT &8» &fIt is a tie!"));
        } else {
            outcome = legacy(colorize(this.winnerLine
                    .replace("%option%", poll.options.get(bestIndex))
                    .replace("%percent%", String.format(Locale.US, "%.0f", bestCount * 100.0 / totalVotes))));
        }

        announce(outcome);
        announce(Component.empty());
    }

    public boolean hasActivePoll() {
        return this.activePoll != null;
    }

    private void sendPollView(Player player) {

        ActivePoll poll = this.activePoll;
        if (poll == null) return;

        player.sendMessage(Component.empty());
        player.sendMessage(legacy(this.headerStyle));
        player.sendMessage(legacy(this.questionStyle.replace("%question%", poll.question)));

        for (int index = 0; index < poll.options.size(); index++) {
            int count = poll.votes.get(index).size();
            String text = colorize(this.optionLine
                    .replace("%index%", String.valueOf(index + 1))
                    .replace("%option%", poll.options.get(index))
                    .replace("%votes%", String.valueOf(count)));

            Component component = LEGACY.deserialize(text)
                    .clickEvent(ClickEvent.runCommand("/poll vote " + (index + 1)))
                    .hoverEvent(HoverEvent.showText(
                            legacyRaw("&7Click to vote for &f" + poll.options.get(index))));

            if (poll.votes.get(index).contains(player.getUniqueId())) {
                component = component.append(LEGACY.deserialize(colorize(this.votedMark)));
            }
            player.sendMessage(component);
        }

        long remaining = Math.max(0, (poll.endAtMillis - System.currentTimeMillis()) / 1000L);
        player.sendMessage(legacy(this.footerStyle
                .replace("%seconds%", String.valueOf(remaining))));
        player.sendMessage(Component.empty());
    }

    private String currentQuestion() {
        return this.activePoll == null ? "" : this.activePoll.question;
    }

    private void announce(Component component) {
        Bukkit.getConsoleSender().sendMessage(component);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(component);
        }
    }

    private void console(String legacy) {
        Bukkit.getConsoleSender().sendMessage(legacyRaw(legacy));
    }

    private Component legacy(String text) {
        return LEGACY.deserialize(colorize(text));
    }

    private Component legacyRaw(String text) {
        return LEGACY.deserialize(colorize(text));
    }

    private String colorize(String text) {
        return dev.yanianz.essentials.util.ColorUtil.sections(text);
    }
}
