package fr.maxlego08.essentials.module.modules.chat;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.chat.ChatDisplay;
import fr.maxlego08.essentials.api.user.Option;
import fr.maxlego08.essentials.api.user.User;
import fr.maxlego08.essentials.api.utils.component.AdventureComponent;
import fr.maxlego08.menu.api.sound.SoundOption;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlayerPingDisplay implements ChatDisplay {

    private final Pattern pingPattern = Pattern.compile("@[a-zA-Z0-9_]{3,16}");
    private final EssentialsPlugin plugin;
    private final String playerPingColor;
    private final String playerPingColorOther;
    private final SoundOption playerPingSound;

    public PlayerPingDisplay(EssentialsPlugin plugin, String playerPingColor, String playerPingColorOther, SoundOption playerPingSound) {
        this.plugin = plugin;
        this.playerPingColor = playerPingColor;
        this.playerPingColorOther = playerPingColorOther;
        this.playerPingSound = playerPingSound;
    }

    @Override
    public String display(AdventureComponent adventureComponent, TagResolver.Builder builder, Player sender, Player receiver, String message) {

        Matcher matcher = this.pingPattern.matcher(message);
        StringBuilder formattedMessage = new StringBuilder();
        boolean shouldSendSound = false;

        while (matcher.find()) {
            String match = matcher.group();
            String playerName = match.substring(1);  // Assuming match starts with a special character (e.g., '@')
            boolean isReceiver = playerName.equalsIgnoreCase(receiver.getName());
            if (isReceiver) {
                shouldSendSound = true;
            }

            String placeholderTag = "ping_" + playerName.toLowerCase();
            String resolvedName = isReceiver ? this.playerPingColor : this.playerPingColorOther;
            resolvedName = resolvedName.replace("%name%", match);

            builder.resolver(Placeholder.component(placeholderTag, adventureComponent.getComponent(resolvedName)));
            matcher.appendReplacement(formattedMessage, "<" + placeholderTag + ">");
        }
        matcher.appendTail(formattedMessage);
        message = formattedMessage.toString();

        if (shouldSendSound && this.playerPingSound != null) {
            // Respect the per-player toggle (/pingsound). The global enable flag is handled in ChatModule
            // where the SoundOption is only built when enable-player-ping-sound is true.
            User receiverUser = this.plugin.getUser(receiver.getUniqueId());
            if (receiverUser == null || (!receiverUser.getOption(Option.PLAYER_PING_SOUND_DISABLE) && !receiverUser.getOption(Option.CHAT_DND))) {
                this.playerPingSound.play(receiver);
            }
        }

        return message;
    }

    @Override
    public boolean hasPermission(Permissible permissible) {
        return true;
    }
}
