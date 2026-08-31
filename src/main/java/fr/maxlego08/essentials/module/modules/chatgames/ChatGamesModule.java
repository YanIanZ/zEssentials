package fr.maxlego08.essentials.module.modules.chatgames;

import fr.maxlego08.essentials.ZEssentialsPlugin;
import fr.maxlego08.essentials.api.configuration.NonLoadable;
import fr.maxlego08.essentials.api.messages.Message;
import fr.maxlego08.essentials.module.ZModule;
import net.kyori.adventure.text.Component;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import io.papermc.paper.event.player.AsyncChatEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Automatic chat mini games: math races, word scrambles, fast typing,
 * reversed words, trivia and hot letter rounds with console rewards.
 */
public class ChatGamesModule extends ZModule {

    private int autoIntervalMinutes;
    private List<String> rewardCommands = new ArrayList<>();
    private String winMessage;

    private final List<String> enabledTypes = new ArrayList<>();

    @NonLoadable
    private String mathQuestionFormat;
    private int mathMin = 2;
    private int mathMax = 99;
    private List<String> mathOperators = new ArrayList<>();

    @NonLoadable
    private List<String> scrambleWords = new ArrayList<>();
    private List<String> fastTypeSentences = new ArrayList<>();
    private List<String> reverseWords = new ArrayList<>();
    private List<TriviaQuestion> triviaQuestions = new ArrayList<>();
    private int hotLetterMinLength = 6;

    @NonLoadable
    private ActiveGame activeGame;
    @NonLoadable
    private com.tcoded.folialib.wrapper.task.WrappedTask autoTask;
    @NonLoadable
    private final Random random = ThreadLocalRandom.current();

    public ChatGamesModule(ZEssentialsPlugin plugin) {
        super(plugin, "chatgames");
    }

    private record TriviaQuestion(String question, List<String> answers) {
    }

    private static final class ActiveGame {
        final String typeLabel;
        final String display;
        final List<String> answers;
        final Runnable onTimeout;

        ActiveGame(String typeLabel, String display, List<String> answers, Runnable onTimeout) {
            this.typeLabel = typeLabel;
            this.display = display;
            this.answers = answers;
            this.onTimeout = onTimeout;
        }
    }

    @Override
    public void loadConfiguration() {
        super.loadConfiguration();

        var config = getConfiguration();
        this.autoIntervalMinutes = config.getInt("auto-interval-minutes", 30);
        this.rewardCommands = config.getStringList("reward-commands");
        this.winMessage = config.getString("win-message", "&a%player% &7won!");

        this.enabledTypes.clear();
        if (config.getBoolean("math.enabled", true)) this.enabledTypes.add("math");
        if (config.getBoolean("scramble.enabled", true)) this.enabledTypes.add("scramble");
        if (config.getBoolean("fast-type.enabled", true)) this.enabledTypes.add("fast-type");
        if (config.getBoolean("reverse.enabled", true)) this.enabledTypes.add("reverse");
        if (config.getBoolean("trivia.enabled", true)) this.enabledTypes.add("trivia");
        if (config.getBoolean("hot-letter.enabled", true)) this.enabledTypes.add("hot-letter");

        this.mathMin = config.getInt("math.min-number", 2);
        this.mathMax = config.getInt("math.max-number", 99);
        this.mathOperators = config.getStringList("math.operators");
        this.mathQuestionFormat = "§b§lCHAT GAME §8» §fWhat is §e%s §f?";
        this.scrambleWords = config.getStringList("scramble.words");
        this.fastTypeSentences = config.getStringList("fast-type.sentences");
        this.reverseWords = config.getStringList("reverse.words");
        this.hotLetterMinLength = config.getInt("hot-letter.minimum-length", 6);

        this.triviaQuestions.clear();
        var section = config.getConfigurationSection("trivia.questions");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                var questionSection = section.getConfigurationSection(key);
                if (questionSection == null) continue;
                this.triviaQuestions.add(new TriviaQuestion(
                        questionSection.getString("question", ""),
                        questionSection.getStringList("answers")));
            }
        }

        restartAutoTask();
        // A config reload invalidates the running game
        this.activeGame = null;
    }

    /**
     * Restarts the automatic game task, cancelling the previous one.
     */
    private void restartAutoTask() {

        if (this.autoTask != null) {
            this.autoTask.cancel();
            this.autoTask = null;
        }

        if (this.autoIntervalMinutes <= 0 || this.enabledTypes.isEmpty()) return;

        long periodTicks = this.autoIntervalMinutes * 60L * 20L;
        // Region aware scheduler, plain bukkit timers are unsupported on folia based servers
        this.autoTask = this.plugin.getScheduler().runTimer(() -> {
            if (this.activeGame == null && !Bukkit.getOnlinePlayers().isEmpty()) {
                startRandom(null);
            }
        }, periodTicks, periodTicks);
    }

    /**
     * Starts a random enabled game, or the forced one when given.
     *
     * @param forcedType the type to force, or null for random
     * @return true when a game started
     */
    public boolean startRandom(String forcedType) {

        if (!this.isEnable || this.activeGame != null) return false;

        String type = forcedType == null || forcedType.isBlank()
                ? this.enabledTypes.get(this.random.nextInt(this.enabledTypes.size()))
                : forcedType.toLowerCase(Locale.ROOT);
        if (!this.enabledTypes.contains(type)) return false;

        ActiveGame game = switch (type) {
            case "math" -> createMathGame();
            case "scramble" -> createScrambleGame();
            case "fast-type" -> createFastTypeGame();
            case "reverse" -> createReverseGame();
            case "trivia" -> createTriviaGame();
            case "hot-letter" -> createHotLetterGame();
            default -> null;
        };

        if (game == null) return false;

        this.activeGame = game;
        broadcast(game.display);
        broadcast(colorize("&7Type the answer in the chat to win!"));
        return true;
    }

    /**
     * Stops the current game without a winner.
     */
    public void stop() {
        this.activeGame = null;
        broadcast(colorize("&cThe chat game was cancelled."));
    }

    public boolean hasActiveGame() {
        return this.activeGame != null;
    }

    /**
     * Evaluates the chat message of a player against the running game.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {

        if (!this.isEnable || this.activeGame == null) return;
        Player player = event.getPlayer();
        String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                .plainText().serialize(event.originalMessage()).trim();

        ActiveGame game = this.activeGame;
        boolean correct;
        if (game.typeLabel.equals("hot-letter") && !game.answers.isEmpty()
                && game.answers.get(0).startsWith("__HOT_LETTER__:")) {
            char letter = game.answers.get(0).charAt("__HOT_LETTER__:".length());
            correct = plain.length() >= this.hotLetterMinLength
                    && Character.toLowerCase(plain.charAt(0)) == letter
                    && plain.chars().allMatch(c -> Character.isLetter(c));
            if (correct) {
                // Avoid the same word twice in one round by requiring uniqueness against recent chat? keep simple
            }
        } else {
            correct = game.answers.stream().anyMatch(answer -> answer.equalsIgnoreCase(plain));
        }

        if (!correct) return;

        event.setCancelled(true);
        this.activeGame = null;

        String answer = game.typeLabel.equals("hot-letter") ? plain : game.answers.get(0);
        broadcast(colorize(this.winMessage
                .replace("%player%", player.getName())
                .replace("%answer%", answer)));

        for (String command : this.rewardCommands) {
            String finalCommand = command.replace("%player%", player.getName());
            this.plugin.getScheduler().runNextTick(wrappedTask ->
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand));
        }
    }

    private ActiveGame createMathGame() {

        int a = this.mathMin + this.random.nextInt(Math.max(1, this.mathMax - this.mathMin + 1));
        int b = this.mathMin + this.random.nextInt(Math.max(1, this.mathMax - this.mathMin + 1));
        String operator = this.mathOperators.isEmpty() ? "+"
                : this.mathOperators.get(this.random.nextInt(this.mathOperators.size()));

        int result = switch (operator) {
            case "-" -> a - b;
            case "*", "x" -> a * b;
            default -> a + b;
        };

        String display = colorize(String.format(this.mathQuestionFormat, a + " " + operator + " " + b));
        return new ActiveGame("math", display, List.of(String.valueOf(result)), null);
    }

    private ActiveGame createScrambleGame() {
        if (this.scrambleWords.isEmpty()) return null;

        String word = this.scrambleWords.get(this.random.nextInt(this.scrambleWords.size()));
        List<Character> characters = new ArrayList<>();
        for (char c : word.toCharArray()) characters.add(c);

        String shuffled = word;
        int guard = 0;
        while (shuffled.equals(word) && guard++ < 10) {
            java.util.Collections.shuffle(characters);
            StringBuilder builder = new StringBuilder();
            characters.forEach(builder::append);
            shuffled = builder.toString();
        }

        String display = colorize("§b§lCHAT GAME §8» §fUnscramble this word: §e§l" + shuffled.toUpperCase(Locale.ROOT));
        return new ActiveGame("scramble", display, List.of(word), null);
    }

    private ActiveGame createFastTypeGame() {
        if (this.fastTypeSentences.isEmpty()) return null;

        String sentence = this.fastTypeSentences.get(this.random.nextInt(this.fastTypeSentences.size()));
        String display = colorize("§b§lCHAT GAME §8» §fFirst one to type: §e" + sentence);
        return new ActiveGame("fast-type", display, List.of(sentence), null);
    }

    private ActiveGame createReverseGame() {
        if (this.reverseWords.isEmpty()) return null;

        String word = this.reverseWords.get(this.random.nextInt(this.reverseWords.size()));
        String reversed = new StringBuilder(word).reverse().toString();

        String display = colorize("§b§lCHAT GAME §8» §fUn-reverse this word: §e§l" + reversed);
        return new ActiveGame("reverse", display, List.of(word), null);
    }

    private ActiveGame createTriviaGame() {
        if (this.triviaQuestions.isEmpty()) return null;

        TriviaQuestion trivia = this.triviaQuestions.get(this.random.nextInt(this.triviaQuestions.size()));
        String display = colorize(trivia.question());
        return new ActiveGame("trivia", display, trivia.answers(), null);
    }

    private ActiveGame createHotLetterGame() {

        String letters = "abcdefghijklmnopqrstuvwxyz";
        char letter = letters.charAt(this.random.nextInt(letters.length()));

        String display = colorize("§b§lCHAT GAME §8» §fFirst word starting with §e§l"
                + Character.toUpperCase(letter)
                + " §fwith at least §e" + this.hotLetterMinLength + "§f letters wins!");
        return new ActiveGame("hot-letter", display,
                List.of("__HOT_LETTER__:" + letter), null);
    }

    private void broadcast(String legacyLine) {
        Component component = LegacyComponentSerializerSupport.deserialize(legacyLine);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(component);
        }
        Bukkit.getConsoleSender().sendMessage(legacyLine);
    }

    private String colorize(String text) {
        return dev.yanianz.essentials.util.ColorUtil.sections(text);
    }

    private static final class LegacyComponentSerializerSupport {
        private static final net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer S =
                net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection();

        static Component deserialize(String input) {
            return S.deserialize(input == null ? "" : input);
        }
    }
}
