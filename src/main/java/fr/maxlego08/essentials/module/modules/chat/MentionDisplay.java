package fr.maxlego08.essentials.module.modules.chat;

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
 * the mentioned player additionally receives a notification sound.
 */
public class MentionDisplay implements ChatDisplay {

    private final boolean notify;
    private final String soundName;
    private final String hoverSelf;
    private final String hoverOther;

    public MentionDisplay(boolean notify, String soundName, String hoverSelf, String hoverOther) {
        this.notify = notify;
        this.soundName = soundName;
        this.hoverSelf = hoverSelf;
        this.hoverOther = hoverOther;
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

        if (foundAny && this.notify && viewer != null
                && buffer.toString().toLowerCase(Locale.ROOT).contains("<mention_" + viewer.getName().toLowerCase(Locale.ROOT) + ">")) {
            viewer.playSound(viewer.getLocation(), resolveSound(), 1f, 1.4f);
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
