package fr.maxlego08.essentials.module.modules.chat;

import fr.maxlego08.essentials.api.chat.ChatDisplay;
import fr.maxlego08.essentials.api.utils.component.AdventureComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Replaces the [pos] keyword with the position of the player, hovering shows
 * more details and clicking suggests a teleport command to that position.
 */
public class PositionDisplay implements ChatDisplay {

    private final Pattern pattern;
    private final String label;
    private final String permission;

    public PositionDisplay(String regex, String label, String permission) {
        this.pattern = Pattern.compile(regex);
        this.label = label;
        this.permission = permission;
    }

    @Override
    public String display(AdventureComponent adventureComponent, TagResolver.Builder builder, Player sender, Player receiver, String message) {

        Matcher matcher = this.pattern.matcher(message);
        if (!matcher.find()) return message;

        StringBuilder formattedMessage = new StringBuilder();
        boolean found = false;
        while (matcher.find()) {
            matcher.appendReplacement(formattedMessage, "<position_display>");
            found = true;
        }
        if (!found) return message;
        matcher.appendTail(formattedMessage);

        Location location = sender.getLocation();
        String coordinates = String.format("%d, %d, %d",
                location.getBlockX(), location.getBlockY(), location.getBlockZ());

        Component hover = Component.empty()
                .append(Component.text("§b§l" + sender.getName() + "§7's position"))
                .append(Component.newline())
                .append(Component.text("§f" + coordinates + " §7in §f" + location.getWorld().getName()))
                .append(Component.newline())
                .append(Component.text("§8Click to suggest §f/tp " + coordinates));

        Component component = Component.text("§e§l" + this.label.replace("<coords>", coordinates))
                .hoverEvent(HoverEvent.showText(hover))
                .clickEvent(ClickEvent.suggestCommand("/tp " + coordinates));

        builder.resolver(Placeholder.component("position_display", component));
        return formattedMessage.toString();
    }

    @Override
    public boolean hasPermission(Permissible permissible) {
        return permissible.hasPermission(this.permission);
    }
}
