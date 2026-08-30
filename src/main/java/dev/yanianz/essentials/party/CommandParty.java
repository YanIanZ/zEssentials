package dev.yanianz.essentials.party;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.commands.CommandResultType;
import fr.maxlego08.essentials.api.commands.Permission;
import fr.maxlego08.essentials.api.messages.Message;
import fr.maxlego08.essentials.zutils.utils.commands.VCommand;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class CommandParty extends VCommand {
    public CommandParty(EssentialsPlugin plugin) {
        super(plugin);
        this.setModule(PartyModule.class);
        this.setPermission(Permission.ESSENTIALS_PARTY);
        this.setDescription(Message.DESCRIPTION_PARTY);
        this.addOptionalArg("action", (s, a) -> java.util.List.of("create", "disband", "invite", "leave", "info"));
        this.addRequirePlayerNameArg();
    }

    @Override
    protected CommandResultType perform(EssentialsPlugin plugin) {
        PartyModule module = plugin.getModuleManager().getModule(PartyModule.class);
        if (module == null || !module.isEnabled()) return CommandResultType.SUCCESS;
        String action = argAsString(0, "info");
        switch (action.toLowerCase()) {
            case "create" -> {
                int id = module.createParty(this.player.getUniqueId());
                if (id > 0) message(sender, Message.COMMAND_PARTY_CREATED);
                else message(sender, Message.COMMAND_PARTY_ALREADY_IN);
            }
            case "disband" -> {
                int id = module.getPlayerPartyId(this.player.getUniqueId());
                if (id > 0 && module.getParty(id).leader() == this.player.getUniqueId()) {
                    module.disbandParty(id);
                    message(sender, Message.COMMAND_PARTY_DISBANDED);
                }
            }
            case "invite" -> {
                int partyId = module.getPlayerPartyId(this.player.getUniqueId());
                String targetName = argAsString(1, "");
                if (partyId > 0 && !targetName.isEmpty()) {
                    Player target = Bukkit.getPlayer(targetName);
                    if (target != null && module.invitePlayer(partyId, target.getUniqueId())) {
                        message(sender, Message.COMMAND_PARTY_INVITED, "%player%", targetName);
                        message(target, Message.COMMAND_PARTY_INVITED_NOTIFY, "%player%", this.player.getName());
                    }
                }
            }
            case "leave" -> {
                int id = module.getPlayerPartyId(this.player.getUniqueId());
                if (id > 0 && module.leaveParty(id, this.player.getUniqueId())) {
                    message(sender, Message.COMMAND_PARTY_LEFT);
                }
            }
            case "info" -> {
                int id = module.getPlayerPartyId(this.player.getUniqueId());
                if (id < 0) {
                    message(sender, Message.COMMAND_PARTY_NOT_IN);
                    return CommandResultType.SUCCESS;
                }
                PartyModule.Party p = module.getParty(id);
                message(sender, Message.COMMAND_PARTY_INFO, "%leader%", p.leader().toString().substring(0, 8),
                        "%size%", String.valueOf(p.members().size()));
            }
        }
        return CommandResultType.SUCCESS;
    }
}