package fr.maxlego08.essentials.module.modules.chat;

import fr.maxlego08.essentials.ZEssentialsPlugin;
import fr.maxlego08.essentials.api.cache.ExpiringCache;
import fr.maxlego08.essentials.api.chat.ChatCooldown;
import fr.maxlego08.essentials.api.chat.ChatDisplay;
import fr.maxlego08.essentials.api.chat.ChatDisplayException;
import fr.maxlego08.essentials.api.chat.ChatFormat;
import fr.maxlego08.essentials.api.chat.ChatPlaceholder;
import fr.maxlego08.essentials.api.chat.ChatResult;
import fr.maxlego08.essentials.api.chat.CustomRules;
import fr.maxlego08.essentials.api.chat.ShowItem;
import fr.maxlego08.essentials.api.commands.Permission;
import fr.maxlego08.essentials.api.dto.ChatMessageDTO;
import fr.maxlego08.essentials.api.event.events.user.UserJoinEvent;
import fr.maxlego08.essentials.api.messages.Message;
import fr.maxlego08.essentials.api.messages.MessageUtils;
import fr.maxlego08.essentials.api.user.User;
import fr.maxlego08.essentials.api.utils.DynamicCooldown;
import fr.maxlego08.essentials.module.ZModule;
import fr.maxlego08.essentials.storage.ConfigStorage;
import fr.maxlego08.essentials.zutils.utils.TimerBuilder;
import fr.maxlego08.essentials.zutils.utils.paper.PaperComponent;
import fr.maxlego08.menu.api.engine.Pagination;
import fr.maxlego08.menu.api.sound.SoundOption;
import fr.maxlego08.menu.hooks.xseries.XSound;
import fr.maxlego08.menu.sound.ZSoundOption;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

public class ChatModule extends ZModule {

    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.legacySection();

    private final List<ShowItem> showItems = new ArrayList<>();
    private final List<ChatDisplay> chatDisplays = new ArrayList<>();

    @fr.maxlego08.essentials.api.configuration.NonLoadable
    private final java.util.Map<String, String> emojiShortcuts = new java.util.LinkedHashMap<>();

    @fr.maxlego08.essentials.api.configuration.NonLoadable
    private final java.util.ArrayDeque<RaidEntry> raidWindow = new java.util.ArrayDeque<>();

    @fr.maxlego08.essentials.api.configuration.NonLoadable
    private volatile long lastRaidAlertAt = 0;

    private boolean raidProtectionEnabled;
    private int raidSimilarMessages;
    private int raidWithinSeconds;
    private List<String> raidCommands;

    /** One normalized chat message inside the raid detection window. */
    private record RaidEntry(java.util.UUID playerId, String normalized, long at) {
    }
    private final ExpiringCache<UUID, List<ChatMessageDTO>> chatMessagesCache = new ExpiringCache<>(1000 * 60);
    private final Pattern urlPattern = Pattern.compile("(https?://[\\w-\\.]+(\\:[0-9]+)?(/[\\w-./?%&=~+#]*)?)", Pattern.CASE_INSENSITIVE);
    private final List<ChatCooldown> chatCooldowns = new ArrayList<>();
    private final List<ChatFormat> chatFormats = new ArrayList<>();
    private final List<ChatPlaceholder> chatPlaceholders = new ArrayList<>();
    private final List<CustomRules> customRules = new ArrayList<>();
    private ChatDisplay pingDisplay;
    private fr.maxlego08.essentials.module.modules.chat.MentionDisplay mentionDisplay;
    private String alphanumericRegex;
    private String linkRegex;
    private String pubRegex;
    private String defaultChatFormat;
    private String moderatorAction;
    private String linkTransform;
    private String dateFormat;
    private String antiFloodRegex;
    private SimpleDateFormat simpleDateFormat;
    private Pattern playerNamePattern;
    private Pattern alphanumericPattern;
    private Pattern linkPattern;
    private Pattern floodRegex;
    private Pattern pubPattern;
    private boolean enableAlphanumericRegex;
    private boolean enableLinkRegex;
    private boolean enableChatDynamicCooldown;

    @fr.maxlego08.essentials.api.configuration.NonLoadable
    private int slowmodeSeconds;

    @fr.maxlego08.essentials.api.configuration.NonLoadable
    private final java.util.Map<java.util.UUID, Long> slowmodeTimestamps = new java.util.HashMap<>();
    private boolean enableSameMessageCancel;
    private boolean enableChatFormat;
    private boolean enablePing;
    private boolean enableAntiFlood;
    private boolean enableLinkTransform;
    private boolean enableChatMessages;
    private int chatCooldownMax;
    private boolean enableCaps;
    private double capsThreshold;
    private long[] chatCooldownArray;
    private boolean enablePlayerPingSound;
    private String playerPingSound;
    private String playerPingColor;
    private String playerPingColorOther;
    private float playerPingSoundVolume;
    private float playerPingSoundPitch;
    private boolean enableLocalChat;
    private double localChatDistance;


    public ChatModule(ZEssentialsPlugin plugin) {
        super(plugin, "chat");
    }

    @Override
    public void loadConfiguration() {
        super.loadConfiguration();

        this.alphanumericPattern = Pattern.compile(or(this.alphanumericRegex, "^[a-zA-Z0-9_.?!^¨%ù*&é\"#'{(\\[-|èêë`\\\\çà)\\]=}ûî+<>:²€$/\\-,-â@;ô ]+$"));
        this.linkPattern = Pattern.compile(or(this.linkRegex, "[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_+.~#?&/=]*)"));
        this.floodRegex = Pattern.compile(or(this.antiFloodRegex, "(.)\\1{3,}"));
        this.pubPattern = Pattern.compile(or(this.pubRegex, ".*(§[0-9a-fk-or]|#[0-9a-fA-F]{6}|%[^%]+%|<[^>]+>).*"));
        this.chatCooldownArray = this.chatCooldowns.stream().flatMapToLong(cooldown -> LongStream.of(cooldown.cooldown(), cooldown.messages())).toArray();
        this.simpleDateFormat = new SimpleDateFormat(or(this.dateFormat, "yyyy-MM-dd HH:mm:ss"));

        this.chatDisplays.clear();
        if (this.enablePing) {
            SoundOption pingSoundOption = null;
            // Resolve the sound cross-version via XSound. Since Paper 1.21.3+ org.bukkit.Sound is an interface
            // (no longer an enum), the reflection config loader can no longer map it, so we resolve it manually.
            if (this.enablePlayerPingSound && this.playerPingSound != null && !this.playerPingSound.isEmpty()) {
                Optional<XSound> xSound = XSound.of(this.playerPingSound);
                pingSoundOption = new ZSoundOption(xSound.orElse(null), "MASTER", this.playerPingSound, this.playerPingSoundPitch, this.playerPingSoundVolume, xSound.isEmpty());
            }
            this.pingDisplay = new PlayerPingDisplay(this.plugin, this.playerPingColor, this.playerPingColorOther, pingSoundOption);
        }

        YamlConfiguration mentionConfig = getConfiguration();
        if (mentionConfig.getBoolean("mention-placeholder.enable", true)) {
            this.mentionDisplay = new fr.maxlego08.essentials.module.modules.chat.MentionDisplay(
                    mentionConfig.getBoolean("mention-placeholder.sound-enabled", true),
                    mentionConfig.getString("mention-placeholder.sound", "ENTITY_EXPERIENCE_ORB_PICKUP"),
                    mentionConfig.getString("mention-placeholder.hover-self", "&6Someone mentioned you!"),
                    mentionConfig.getString("mention-placeholder.hover-other", "&7Click to message them")
            );
            this.mentionDisplay.setDndCheck(viewer -> {
                var dndUser = this.plugin.getUser(viewer.getUniqueId());
                return dndUser != null && dndUser.getOption(fr.maxlego08.essentials.api.user.Option.CHAT_DND);
            });
        }

        // Emoji shortcuts like :heart: replaced with their symbol
        this.emojiShortcuts.clear();
        ConfigurationSection emojiSection = getConfiguration().getConfigurationSection("emoji-shortcuts");
        if (emojiSection != null) {
            for (String key : emojiSection.getKeys(false)) {
                this.emojiShortcuts.put(java.util.regex.Pattern.quote(key), emojiSection.getString(key, key));
            }
        }

        // Raid protection configuration
        var raidConfig = getConfiguration();
        this.raidProtectionEnabled = raidConfig.getBoolean("raid-protection.enabled", true);
        this.raidSimilarMessages = raidConfig.getInt("raid-protection.similar-messages", 3);
        this.raidWithinSeconds = raidConfig.getInt("raid-protection.within-seconds", 10);
        this.raidCommands = raidConfig.getStringList("raid-protection.actions");

        Pattern pattern = Pattern.compile("[!?#]?[a-z0-9_-]*");
        this.chatPlaceholders.forEach(chatPlaceholder -> {
            Matcher matcher = pattern.matcher(chatPlaceholder.name());
            if (matcher.find()) {
                this.chatDisplays.add(new CustomDisplay(chatPlaceholder.name(), chatPlaceholder.regex(), chatPlaceholder.result(), chatPlaceholder.permission()));
            } else {
                plugin.getLogger().severe("Custom Placeholders name must match pattern [!?#]?[a-z0-9_-]*, was " + chatPlaceholder.name() + ", Possible correction: " + MessageUtils.removeNonAlphanumeric(chatPlaceholder.name()));
            }
        });

        YamlConfiguration configuration = getConfiguration();
        if (configuration.getBoolean("item-placeholder.enable")) {
            this.chatDisplays.add(new ItemDisplay(
                    this.plugin,
                    configuration.getString("item-placeholder.regex"),
                    configuration.getString("item-placeholder.result"),
                    configuration.getString("item-placeholder.permission"),
                    configuration.getString("item-placeholder.item-name-regex", configuration.getString("alphanumeric-regex"))
            ));
        }

        if (configuration.getBoolean("inventory-placeholder.enable", true)) {
            this.chatDisplays.add(new ItemListDisplay(
                    configuration.getString("inventory-placeholder.regex", "(?i)\\[inv\\]|\\[inventory\\]"),
                    "inventory_display",
                    "INVENTORY",
                    configuration.getString("inventory-placeholder.permission", "zessentials.chat.placeholder.inventory"),
                    player -> player.getInventory().getStorageContents()
            ));
        }

        if (configuration.getBoolean("enderchest-placeholder.enable", true)) {
            this.chatDisplays.add(new ItemListDisplay(
                    configuration.getString("enderchest-placeholder.regex", "(?i)\\[ender\\]|\\[ec\\]"),
                    "ender_display",
                    "ENDER CHEST",
                    configuration.getString("enderchest-placeholder.permission", "zessentials.chat.placeholder.enderchest"),
                    player -> player.getEnderChest().getContents()
            ));
        }

        if (configuration.getBoolean("position-placeholder.enable", true)) {
            this.chatDisplays.add(new PositionDisplay(
                    configuration.getString("position-placeholder.regex", "(?i)\\[pos\\]|\\[position\\]"),
                    configuration.getString("position-placeholder.label", "[<coords>]"),
                    configuration.getString("position-placeholder.permission", "zessentials.chat.placeholder.position")
            ));
        }

        if (configuration.getBoolean("command-placeholder.enable")) {
            this.chatDisplays.add(new CommandDisplay(configuration.getString("command-placeholder.result"), configuration.getString("command-placeholder.permission")));
        }

        this.customRules.removeIf(CustomRules::isNotValid);
    }

    @EventHandler
    public void onJoin(UserJoinEvent event) {
        User user = event.getUser();
        user.getDynamicCooldown().setSamples(this.chatCooldownMax);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onTalk(AsyncChatEvent event) {

        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        User user = plugin.getUser(player.getUniqueId());

        if (user == null) {
            cancelEvent(event, Message.CHAT_ERROR);
            return;
        }

        if (!ConfigStorage.chatEnable && !hasPermission(player, Permission.ESSENTIALS_CHAT_BYPASS_DISABLE)) {
            cancelEvent(event, Message.CHAT_DISABLE);
            return;
        }

        String message = PlainTextComponentSerializer.plainText().serialize(event.originalMessage());
        boolean isGlobalChat = false;

        if (this.enableLocalChat && message.startsWith("!")) {
            isGlobalChat = true;
            message = message.substring(1).stripLeading();
        }
        for (java.util.Map.Entry<String, String> entry : this.emojiShortcuts.entrySet()) {
            message = message.replaceAll(entry.getKey(), java.util.regex.Matcher.quoteReplacement(entry.getValue()));
        }
        final String minecraftMessage = message;
        // Raid protection: identical messages from different players in a short window
        if (this.raidProtectionEnabled && !hasPermission(player, Permission.ESSENTIALS_CHAT_MODERATOR)) {
            String normalized = minecraftMessage.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]", "");
            long now = System.currentTimeMillis();

            synchronized (this.raidWindow) {
                while (!this.raidWindow.isEmpty() && now - this.raidWindow.peekFirst().at() > this.raidWithinSeconds * 1000L) {
                    this.raidWindow.removeFirst();
                }
                this.raidWindow.addLast(new RaidEntry(player.getUniqueId(), normalized, now));

                long matches = this.raidWindow.stream()
                        .filter(entry -> entry.normalized().equals(normalized))
                        .map(RaidEntry::playerId)
                        .distinct()
                        .count();

                if (matches >= this.raidSimilarMessages && normalized.length() >= 4) {
                    event.setCancelled(true);

                    // Alert staff once per window
                    if (now - this.lastRaidAlertAt > this.raidWithinSeconds * 1000L) {
                        this.lastRaidAlertAt = now;
                        String alert = getMessage(Message.RAID_ALERT,
                                "%amount%", String.valueOf(matches),
                                "%message%", minecraftMessage.length() > 40 ? minecraftMessage.substring(0, 40) + "..." : minecraftMessage);
                        for (Player mod : Bukkit.getOnlinePlayers()) {
                            if (hasPermission(mod, Permission.ESSENTIALS_CHAT_MODERATOR)) {
                                mod.sendMessage(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize(alert.replace('&', '§')));
                            }
                        }
                        for (String command : this.raidCommands) {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%message%", normalized));
                        }
                    }
                    return;
                }
            }
        }


        Optional<CustomRules> optional = this.customRules.stream().filter(rule -> rule.match(player, minecraftMessage)).findFirst();
        if (optional.isPresent()) {
            message(player, optional.get().message());
            event.setCancelled(true);
            return;
        }

        ChatResult chatResult = analyzeMessage(user, message);
        if (!chatResult.isValid()) {
            cancelEvent(event, chatResult.message(), chatResult.arguments());
            return;
        }

        user.setLastMessage(message);

        if (!this.enableChatFormat) return;

        if (this.enableLinkTransform && hasPermission(player, Permission.ESSENTIALS_CHAT_LINK)) {
            message = transformUrlsToMiniMessage(message);
        }

        PaperComponent paperComponent = (PaperComponent) this.componentMessage;
        String chatFormat = papi(getChatFormat(player), player);

        TagResolver.Builder builder = TagResolver.builder();
        try {
            for (ChatDisplay chatDisplay : this.chatDisplays) {
                message = chatDisplay.display(paperComponent, builder, player, null, message);
            }
        } catch (ChatDisplayException exception) {
            cancelEvent(event, exception.getChatMessage(), exception.getArguments());
            return;
        }

        String finalMessage = message;
        double maxDistanceSquared = this.localChatDistance * this.localChatDistance;
        if (this.enableLocalChat && !isGlobalChat) {
            event.viewers().removeIf(viewer -> viewer instanceof Player playerViewer && (!playerViewer.getWorld().equals(player.getWorld()) || playerViewer.getLocation().distanceSquared(player.getLocation()) > maxDistanceSquared));
        }
        event.renderer((source, sourceDisplayName, ignoredMessage, viewer) -> {

            String localMessage = finalMessage;

            boolean isModerator = viewer instanceof Player playerViewer && hasPermission(playerViewer, Permission.ESSENTIALS_CHAT_MODERATOR);

            TagResolver.Builder localBuilder = TagResolver.builder().resolver(builder.build());
            String renderMessage = localMessage;

            if (viewer instanceof Player playerViewer) {
                if (this.pingDisplay != null) {
                    renderMessage = this.pingDisplay.display(paperComponent, localBuilder, player, playerViewer, renderMessage);
                }
                if (this.mentionDisplay != null && this.mentionDisplay.hasPermission(playerViewer)) {
                    StringBuilder rewritten = new StringBuilder(renderMessage);
                    this.mentionDisplay.process(player, playerViewer, renderMessage, rewritten, localBuilder);
                    renderMessage = rewritten.toString();
                }
            }

            // Per player color, decorations and tag from the customization module
            String messagePrefix = "";
            try {
                var customizationModule = this.plugin.getModuleManager()
                        .getModule(dev.yanianz.essentials.chatcustomization.ChatCustomizationModule.class);
                if (customizationModule != null) {
                    var preference = customizationModule.getPreference(player.getUniqueId());
                    String colorPrefix = preference.colorCode();
                    for (String decoration : preference.decorations()) {
                        colorPrefix += "§" + decoration;
                    }
                    if (!colorPrefix.isEmpty()) {
                        renderMessage = colorPrefix + renderMessage + "§r";
                    }
                    if (!preference.tagText().isEmpty()) {
                        messagePrefix = preference.tagText();
                    }
                }
            } catch (Exception ignored) {
            }

            Tag tag = Tag.inserting(paperComponent.translateText(player,
                    messagePrefix + renderMessage, localBuilder.build()));
            String moderatorAction = (isModerator && viewer instanceof Player playerMod) ? papi(getMessage(this.moderatorAction, "%player%", player.getName()), playerMod) : "";
            String displayName = resolveDisplayName(player);

            // Customization tag renders before ranks and names as a symbol prefix
            String chatFormatWithTag = chatFormat;
            try {
                var customizationModule = this.plugin.getModuleManager()
                        .getModule(dev.yanianz.essentials.chatcustomization.ChatCustomizationModule.class);
                if (customizationModule != null) {
                    String tagText = customizationModule.resolveTagText(player.getUniqueId());
                    if (!tagText.isEmpty()) {
                        chatFormatWithTag = dev.yanianz.essentials.util.ColorUtil.sections(tagText)
                                + " " + chatFormat;
                    }
                }
            } catch (Exception ignored) {
            }

            return paperComponent.getComponentMessage(chatFormatWithTag, TagResolver.resolver("message", tag), "%displayName%", displayName, "%player%", player.getName(), "%moderator_action%", moderatorAction);
        });

        if (this.enableChatMessages) {
            this.plugin.getStorageManager().getStorage().insertChatMessage(player.getUniqueId(), minecraftMessage);
            this.chatMessagesCache.clear(player.getUniqueId());
        }
    }

    /**
     * Legacy string of the nickname when the player has one through the
     * nicknames module, otherwise the plain display name of the player.
     */
    private String resolveDisplayName(Player player) {
        try {
            var module = this.plugin.getModuleManager()
                    .getModule(dev.yanianz.essentials.nicknames.NicknamesModule.class);
            if (module != null) {
                String nickname = module.getNickname(player.getUniqueId());
                if (nickname != null && !nickname.isEmpty()) {
                    return dev.yanianz.essentials.util.ColorUtil.sections(nickname);
                }
            }
        } catch (Exception ignored) {
        }
        return LEGACY.serialize(player.displayName());
    }

    /** Emoji shortcut patterns from the configuration, reused by other features. */
    public java.util.Map<String, String> getEmojiShortcuts() {
        return java.util.Collections.unmodifiableMap(this.emojiShortcuts);
    }

    public int getSlowmodeSeconds() {
        return this.slowmodeSeconds;
    }

    public void setSlowmodeSeconds(int seconds) {
        this.slowmodeSeconds = Math.max(0, seconds);
        this.slowmodeTimestamps.clear();
    }

    public ChatResult analyzeMessage(User user, String message) {
        Player player = user.getPlayer();

        if (this.enableAlphanumericRegex && !this.alphanumericPattern.matcher(message).find() && !hasPermission(player, Permission.ESSENTIALS_CHAT_BYPASS_ALPHANUMERIC)) {
            return new ChatResult(false, Message.CHAT_ALPHANUMERIC_REGEX);
        }

        if (this.enableLinkRegex && this.linkPattern.matcher(message.replace(" ", "")).find() && !hasPermission(player, Permission.ESSENTIALS_CHAT_BYPASS_LINK)) {
            return new ChatResult(false, Message.CHAT_LINK);
        }

        double cooldown = handleCooldown(user);
        if (this.enableChatDynamicCooldown && cooldown > 0 && !hasPermission(player, Permission.ESSENTIALS_CHAT_BYPASS_DYNAMIC_COOLDOWN)) {
            return new ChatResult(false, Message.CHAT_COOLDOWN, "%cooldown%", TimerBuilder.getStringTime(cooldown));
        }

        // Flat slowmode set by staff with /chatslowmode
        if (this.slowmodeSeconds > 0 && !hasPermission(player, Permission.ESSENTIALS_CHAT_BYPASS_SLOWMODE)) {
            long now = System.currentTimeMillis();
            long last = this.slowmodeTimestamps.getOrDefault(player.getUniqueId(), 0L);
            long wait = this.slowmodeSeconds * 1000L - (now - last);
            if (wait > 0) {
                return new ChatResult(false, Message.COMMAND_CHAT_SLOWMODE_WAIT,
                        "%cooldown%", TimerBuilder.getStringTime(wait));
            }
            this.slowmodeTimestamps.put(player.getUniqueId(), now);
        }

        String lastMessage = user.getLastMessage();
        if (this.enableSameMessageCancel && message.equalsIgnoreCase(lastMessage) && !hasPermission(player, Permission.ESSENTIALS_CHAT_BYPASS_SAME_MESSAGE)) {
            return new ChatResult(false, Message.CHAT_SAME);
        }

        if (message.length() > 3 && this.enableCaps && containsTooManyCaps(message) && !hasPermission(player, Permission.ESSENTIALS_CHAT_BYPASS_CAPS)) {
            return new ChatResult(false, Message.CHAT_CAPS);
        }

        if (this.enableAntiFlood && containsFlood(message) && !hasPermission(player, Permission.ESSENTIALS_CHAT_BYPASS_FLOOD)) {
            return new ChatResult(false, Message.CHAT_FLOOD);
        }

        return new ChatResult(true, null);
    }

    private String getChatFormat(Player player) {
        return this.chatFormats.stream().filter(chatFormat -> player.hasPermission(chatFormat.permission())).sorted(Comparator.comparingInt(ChatFormat::priority).reversed()).map(ChatFormat::format).findFirst().orElse(this.defaultChatFormat);
    }

    private double handleCooldown(User user) {

        long wait;

        DynamicCooldown dynamicCooldown = user.getDynamicCooldown();

        wait = dynamicCooldown.limited(user.getUniqueId(), this.chatCooldownArray);
        if (wait == 0L) dynamicCooldown.add(user.getUniqueId());

        return wait != 0L ? wait : 0.0;
    }

    private void cancelEvent(AsyncChatEvent event, Message message, Object... objects) {
        event.setCancelled(true);
        message(event.getPlayer(), message, objects);
    }

    private String transformUrlsToMiniMessage(String input) {
        Matcher matcher = urlPattern.matcher(input);

        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String url = matcher.group(1);
            matcher.appendReplacement(result, Matcher.quoteReplacement(getMessage(this.linkTransform, "%url%", url)));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    public void sendChatHistory(CommandSender sender, UUID targetUuid, String targetName, int targetPage) {
        this.plugin.getScheduler().runAsync(wrappedTask -> {

            List<ChatMessageDTO> messages = this.chatMessagesCache.get(targetUuid, () -> this.plugin.getStorageManager().getStorage().getMessages(targetUuid));
            if (messages.isEmpty()) {
                message(sender, Message.CHAT_MESSAGES_EMPTY, "%player%", targetName);
                return;
            }

            Pagination<ChatMessageDTO> pagination = new Pagination<>();
            int maxPage = getMaxPage(messages, 10);
            int page = targetPage > maxPage ? maxPage : targetPage < 0 ? 1 : targetPage;

            List<ChatMessageDTO> pageMessages = new Pagination<ChatMessageDTO>().paginate(messages, 10, page);
            boolean moderator = sender instanceof Player playerSender && hasPermission(playerSender, Permission.ESSENTIALS_CHAT_MODERATOR);

            for (int localIndex = 0; localIndex < pageMessages.size(); localIndex++) {
                ChatMessageDTO dto = pageMessages.get(localIndex);
                if (!moderator) {
                    message(sender, Message.CHAT_MESSAGES_LINE, "%date%", format(dto.created_at()), "%message%", dto.content());
                    continue;
                }
                int globalIndex = (page - 1) * 10 + localIndex;
                Component line = LegacyComponentSerializer.legacySection().deserialize(
                        plain(getMessage(Message.CHAT_MESSAGES_LINE, "%date%", format(dto.created_at()), "%message%", dto.content())));
                line = line.append(Component.text(" §8[§c✖§8]")
                        .hoverEvent(HoverEvent.showText(Component.text("§cDelete this message")))
                        .clickEvent(ClickEvent.runCommand("/essentials:chathistory " + targetName + " delete " + globalIndex + " " + page)));
                sender.sendMessage(line);
            }

            message(sender, Message.CHAT_MESSAGES_FOOTER, "%page%", page, "%nextPage%", page + 1, "%previousPage%", page - 1, "%maxPage%", maxPage, "%player%", targetName);
        });
    }


    /**
     * Deletes one history message of a player using its index inside the full
     * list, then re-sends the page so staff sees the result immediately.
     */
    public void deleteHistoryMessage(CommandSender sender, UUID targetUuid, String targetName, int globalIndex, int page) {
        this.plugin.getScheduler().runAsync(wrappedTask -> {

            List<ChatMessageDTO> messages = this.chatMessagesCache.get(targetUuid,
                    () -> this.plugin.getStorageManager().getStorage().getMessages(targetUuid));

            if (globalIndex < 0 || globalIndex >= messages.size()) {
                message(sender, Message.CHAT_MESSAGE_DELETED, "%count%", "0", "%player%", targetName);
                return;
            }

            ChatMessageDTO dto = messages.get(globalIndex);
            int removed = this.plugin.getStorageManager().getStorage().deleteChatMessage(dto.unique_id(), dto.content());

            this.chatMessagesCache.clear(targetUuid);
            message(sender, Message.CHAT_MESSAGE_DELETED, "%count%", String.valueOf(removed), "%player%", targetName);
            sendChatHistory(sender, targetUuid, targetName, page);
        });
    }

    private String plain(String legacy) {
        return legacy == null ? "" : legacy;
    }

    private String format(Date date){
        if (date == null) return "";
        return this.simpleDateFormat.format(date);
    }

    private void updatePlayerNamePattern() {
        String patternString = Bukkit.getOnlinePlayers().stream().map(Player::getName).map(Pattern::quote).collect(Collectors.joining("|", "\\b(", ")\\b"));
        this.playerNamePattern = Pattern.compile(patternString);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        updatePlayerNamePattern();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        updatePlayerNamePattern();
    }

    private boolean containsTooManyCaps(String message) {
        if (this.playerNamePattern != null) {
            Matcher matcher = this.playerNamePattern.matcher(message);
            message = matcher.replaceAll("");  // Remove nicknames from players
        }

        int upperCaseCount = 0;
        int totalLetterCount = 0;

        // Count the number of capital letters and total letters
        for (char c : message.toCharArray()) {
            if (Character.isLetter(c)) {
                totalLetterCount++;
                if (Character.isUpperCase(c)) {
                    upperCaseCount++;
                }
            }
        }

        // If the message does not contain letters, it cannot be considered capitalized spam
        if (totalLetterCount == 0) {
            return false;
        }

        // Calculate percentage of caps
        double upperCasePercentage = (double) upperCaseCount / totalLetterCount;

        // Check if the percentage of caps exceeds the threshold
        return upperCasePercentage > capsThreshold;
    }


    public boolean containsFlood(String message) {
        return this.floodRegex.matcher(message).find();
    }

    public String createHoverItemStack(Player player, ItemStack itemStack) {

        this.showItems.removeIf(ShowItem::isExpired);

        String code = generateRandomString(16);

        ShowItem showItem = new ShowItem(player, itemStack, System.currentTimeMillis() + (1000 * 300), code);
        this.showItems.add(showItem);

        return code;
    }

    public void openShowItem(Player player, String code) {

        this.showItems.removeIf(ShowItem::isExpired);
        Optional<ShowItem> optional = this.showItems.stream().filter(showItem -> showItem.code().equals(code)).findFirst();
        if (optional.isEmpty()) {
            message(player, Message.CODE_NOT_FOUND);
            return;
        }

        new ShowItemInventory(optional.get(), player);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof ShowItemInventory) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClick(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof ShowItemInventory) {
            event.setCancelled(true);
        }
    }

    public Pattern getPubPattern() {
        return pubPattern;
    }
}
