package dev.yanianz.essentials.party;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.commands.CommandResultType;
import fr.maxlego08.essentials.api.commands.Permission;
import fr.maxlego08.essentials.api.messages.Message;
import fr.maxlego08.essentials.zutils.utils.commands.VCommand;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import java.util.UUID;

public class CommandPartyChat extends VCommand {
    public CommandPartyChat(EssentialsPlugin plugin) {
        super(plugin);
        this.setModule(PartyModule.class);
        this.setPermission(Permission.ESSENTIALS_PARTY);
        this.setDescription(Message.DESCRIPTION_PARTY_CHAT);
        this.setExtendedArgs(true);
    }

    @Override
    protected CommandResultType perform(EssentialsPlugin plugin) {
        PartyModule module = plugin.getModuleManager().getModule(PartyModule.class);
        if (module == null || !module.isEnabled()) return CommandResultType.SUCCESS;
        int partyId = module.getPlayerPartyId(this.player.getUniqueId());
        if (partyId < 0) {
            message(sender, Message.COMMAND_PARTY_NOT_IN);
            return CommandResultType.SUCCESS;
        }
        String msg = getArgs(0);
        String formatted = dev.yanianz.essentials.util.ColorUtil.sections(
                module.getChatFormat()
                        .replace("%player%", this.player.getName())
                        .replace("%message%", msg));
        for (UUID memberUuid : module.getParty(partyId).members().keySet()) {
            Player p = Bukkit.getPlayer(memberUuid);
            if (p != null) p.sendMessage(LegacyComponentSerializer.legacySection().deserialize(formatted));
        }
        return CommandResultType.SUCCESS;
    }
}