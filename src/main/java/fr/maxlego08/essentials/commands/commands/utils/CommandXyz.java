package fr.maxlego08.essentials.commands.commands.utils;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.commands.CommandResultType;
import fr.maxlego08.essentials.api.commands.Permission;
import fr.maxlego08.essentials.api.messages.Message;
import fr.maxlego08.essentials.zutils.utils.commands.VCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;

import java.util.Locale;

/**
 * /xyz copies the formatted coordinates of the player to the clipboard
 * and shows them in the chat with a hover preview.
 */
public class CommandXyz extends VCommand {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    public CommandXyz(EssentialsPlugin plugin) {
        super(plugin);
        this.setPermission(Permission.ESSENTIALS_XYZ);
        this.setDescription(Message.DESCRIPTION_XYZ);
        this.onlyPlayers();
    }

    @Override
    protected CommandResultType perform(EssentialsPlugin plugin) {

        Location location = this.player.getLocation();
        String worldName = location.getWorld() == null ? "world" : location.getWorld().getName();

        String shortForm = String.format(Locale.US, "%d %d %d",
                location.getBlockX(), location.getBlockY(), location.getBlockZ());
        String coords = worldName + " " + shortForm;

        Component message = LEGACY.deserialize(colorize(
                "&#00d4ff&lXYZ &8» &f" + shortForm + " &7in &f" + worldName))
                .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(
                        LEGACY.deserialize(colorize("&7Click to copy"))))
                .clickEvent(ClickEvent.copyToClipboard(coords));

        this.player.sendMessage(message);
        message(sender, Message.COMMAND_XYZ_COPIED, "%coords%", coords);

        return CommandResultType.SUCCESS;
    }

    private String colorize(String text) {
        return text == null ? "" : text.replace("&", "§");
    }
}
