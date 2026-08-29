package fr.maxlego08.essentials.module.modules.chat;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.chat.ChatDisplay;
import fr.maxlego08.essentials.api.utils.component.AdventureComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Highlights @player mentions inside the chat message. The highlighted name
 * is per viewer: hovering explains the mention and clicking suggests a /msg,
 * the mentioned player additionally receives a notification sound, title,
 * action bar or boss bar depending on configuration.
 */
public class MentionDisplay implements ChatDisplay {

    private final EssentialsPlugin plugin;
    private final boolean notify;
    private final String soundName;
    private final String hoverSelf;
    private final String hoverOther;
    private final boolean titleEnabled;
    private final String titleText;
    private final String subtitleText;
    private final boolean actionbarEnabled;
    private final String actionbarText;
    private final boolean bossbarEnabled;
    private final String bossbarText;
    private final int bossbarSeconds;

    private java.util.function.Predicate<Player> dndCheck = p -> false;

    public MentionDisplay(boolean notify, String soundName, String hoverSelf, String hoverOther) {
        this(null, notify, soundName, hoverSelf, hoverOther, false, "", "", false, "", false, "", 3);
    }

    public MentionDisplay(EssentialsPlugin plugin, boolean notify, String soundName, String hoverSelf, String hoverOther,
                          boolean titleEnabled, String titleText, String subtitleText,
                          boolean actionbarEnabled, String actionbarText,
                          boolean bossbarEnabled, String bossbarText, int bossbarSeconds) {
        this.plugin = plugin;
        this.notify = notify;
        this.soundName = soundName;
        this.hoverSelf = hoverSelf;
        this.hoverOther = hoverOther;
        this.titleEnabled = titleEnabled;
        this.titleText = titleText;
        this.subtitleText = subtitleText;
        this.actionbarEnabled = actionbarEnabled;
        this.actionbarText = actionbarText;
        this.bossbarEnabled = bossbarEnabled;
        this.bossbarText = bossbarText;
        this.bossbarSeconds = bossbarSeconds;
    }

    public void setDndCheck(java.util.function.Predicate<Player> dndCheck) {
        this.dndCheck = dndCheck;
    }

    /**
     * Rewrites every @name of an online player into a mini-message tag
     * {@code <mention_name>} and registers the matching component in the
     * given builder.
     *
     * @return true when at least one mention was found.
     */
    public boolean process(Player sender, Player viewer, String message, StringBuilder rewritten,
                           net.kyori.adventure.text.minimessage.tag.resolver.TagResolver.Builder builder) {

        if (!message.contains("@")) return false;

        List<String> quotedNames = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.equals(sender)) quotedNames.add(Pattern.quote(online.getName()));
        }
        if (quotedNames.isEmpty()) return false;

        Pattern pattern = Pattern.compile("(?i)@(" + String.join("|", quotedNames) + ")\\b");
        Matcher matcher = pattern.matcher(message);
        if (!matcher.find()) return false;

        matcher.reset();
        while (matcher.find()) {
            String name = matcher.group(1);
            boolean isViewerMention = viewer != null && name.equalsIgnoreCase(viewer.getName());

            Component component = Component.text("§6§l@" + name)
                    .hoverEvent(HoverEvent.showText(Component.text(colorize(
                            isViewerMention ? this.hoverSelf : this.hoverOther))))
                    .clickEvent(ClickEvent.suggestCommand("/msg " + name + " "));

            builder.resolver(net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.component(
                    "mention_" + name.toLowerCase(Locale.ROOT), component));
        }

        // Rewrite the raw text with resolvable tags
        matcher.reset();
        boolean foundAny = false;
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            foundAny = true;
            String replacement = "<mention_" + matcher.group(1).toLowerCase(Locale.ROOT) + ">";
            matcher.appendReplacement(buffer, java.util.regex.Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        rewritten.setLength(0);
        rewritten.append(foundAny ? buffer : message);

        boolean mentioned = foundAny && viewer != null
                && buffer.toString().toLowerCase(Locale.ROOT)
                        .contains("<mention_" + viewer.getName().toLowerCase(Locale.ROOT) + ">");

        if (mentioned && this.notify && !this.dndCheck.test(viewer)) {
            viewer.playSound(viewer.getLocation(), resolveSound(), 1f, 1.4f);

            if (this.titleEnabled && !this.titleText.isEmpty()) {
                Component title = Component.text(colorize(this.titleText.replace("%player%", sender.getName())));
                Component subtitle = this.subtitleText.isEmpty()
                        ? Component.empty()
                        : Component.text(colorize(this.subtitleText.replace("%player%", sender.getName())));
                viewer.showTitle(net.kyori.adventure.title.Title.title(title, subtitle,
                        net.kyori.adventure.title.Title.Times.times(
                                java.time.Duration.ofMillis(300),
                                java.time.Duration.ofMillis(2000),
                                java.time.Duration.ofMillis(500))));
            }

            if (this.actionbarEnabled && !this.actionbarText.isEmpty()) {
                viewer.sendActionBar(Component.text(colorize(
                        this.actionbarText.replace("%player%", sender.getName()))));
            }

            if (this.bossbarEnabled && !this.bossbarText.isEmpty() && this.plugin != null) {
                var bossBar = net.kyori.adventure.bossbar.BossBar.bossBar(
                        Component.text(colorize(this.bossbarText.replace("%player%", sender.getName()))),
                        1f,
                        net.kyori.adventure.bossbar.BossBar.Color.YELLOW,
                        net.kyori.adventure.bossbar.BossBar.Overlay.NOTCHED_10);
                viewer.showBossBar(bossBar);
                this.plugin.getScheduler().runAtEntityLater(viewer, () -> viewer.hideBossBar(bossBar),
                        this.bossbarSeconds * 20L);
            }
        }

        return true;
    }

    private Sound resolveSound() {
        try {
            return Sound.valueOf(this.soundName.toUpperCase(Locale.ROOT));
        } catch (Exception exception) {
            return Sound.ENTITY_EXPERIENCE_ORB_PICKUP;
        }
    }

    private String colorize(String text) {
        return text == null ? "" : text.replace("&", "§");
    }

    @Override
    public String display(AdventureComponent adventureComponent, net.kyori.adventure.text.minimessage.tag.resolver.TagResolver.Builder builder,
                          Player sender, Player receiver, String message) {
        throw new UnsupportedOperationException("Use process(sender, viewer, message, rewritten, builder)");
    }

    @Override
    public boolean hasPermission(Permissible permissible) {
        return permissible.hasPermission("zessentials.chat.mention.view");
    }
}
