package fr.maxlego08.essentials.module.modules.chat;

import fr.maxlego08.essentials.api.chat.ChatDisplay;
import fr.maxlego08.essentials.api.utils.component.AdventureComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.Permissible;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Replaces a keyword like [inv] or [ender] with a summary badge whose hover
 * lists every non empty item of the source inventory and whose click copies
 * the content as plain text.
 */
public class ItemListDisplay implements ChatDisplay {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    private final Pattern pattern;
    private final String placeholderName;
    private final String label;
    private final String permission;
    private final Function<Player, ItemStack[]> source;

    public ItemListDisplay(String regex, String placeholderName, String label, String permission,
                           Function<Player, ItemStack[]> source) {
        this.pattern = Pattern.compile(regex);
        this.placeholderName = placeholderName;
        this.label = label;
        this.permission = permission;
        this.source = source;
    }

    @Override
    public String display(AdventureComponent adventureComponent, TagResolver.Builder builder, Player sender, Player receiver, String message) {

        Matcher matcher = this.pattern.matcher(message);
        if (!matcher.find()) return message;

        StringBuilder formattedMessage = new StringBuilder();
        boolean found = false;
        while (matcher.find()) {
            matcher.appendReplacement(formattedMessage, "<" + this.placeholderName + ">");
            found = true;
        }
        if (!found) return message;
        matcher.appendTail(formattedMessage);

        List<Component> hoverLines = new ArrayList<>();
        hoverLines.add(Component.text("§b§l" + this.label));

        List<String> plainItems = new ArrayList<>();
        int empty = 0;

        for (ItemStack item : this.source.apply(sender)) {
            if (item == null || item.getType().isAir()) {
                empty++;
                continue;
            }
            Component itemName = item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                    ? item.getItemMeta().displayName()
                    : Component.translatable(item);

            hoverLines.add(Component.empty()
                    .append(Component.text("§8• "))
                    .append(itemName)
                    .append(Component.text(" §f×" + item.getAmount())));
            plainItems.add(item.getAmount() + "x " + PLAIN.serialize(itemName));
        }

        if (plainItems.isEmpty()) {
            hoverLines.add(Component.text("§7Empty."));
        } else if (empty > 0) {
            hoverLines.add(Component.text("§8" + empty + " empty slots"));
        }

        Component component = Component.text("§b§l" + this.label)
                .hoverEvent(HoverEvent.showText(Component.join(JoinConfiguration.separator(Component.newline()), hoverLines)));

        if (!plainItems.isEmpty()) {
            component = component.clickEvent(ClickEvent.copyToClipboard(String.join(", ", plainItems)));
        }

        builder.resolver(Placeholder.component(this.placeholderName, component));
        return formattedMessage.toString();
    }

    @Override
    public boolean hasPermission(Permissible permissible) {
        return permissible.hasPermission(this.permission);
    }
}
