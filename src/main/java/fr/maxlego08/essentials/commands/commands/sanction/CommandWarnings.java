package fr.maxlego08.essentials.commands.commands.sanction;

import fr.maxlego08.essentials.ZEssentialsPlugin;
import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.commands.CommandResultType;
import fr.maxlego08.essentials.api.commands.Permission;
import fr.maxlego08.essentials.api.messages.Message;
import fr.maxlego08.essentials.zutils.utils.TimerBuilder;
import fr.maxlego08.essentials.zutils.utils.commands.VCommand;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.UUID;

/**
 * Lists every stored warning of a player with its reason and date.
 */
public class CommandWarnings extends VCommand {

    public CommandWarnings(EssentialsPlugin plugin) {
        super(plugin);
        this.setPermission(Permission.ESSENTIALS_WARNINGS_VIEW);
        this.setDescription(Message.DESCRIPTION_WARNINGS);
        this.addRequireOfflinePlayerNameArg();
    }

    @Override
    protected CommandResultType perform(EssentialsPlugin plugin) {

        String username = this.argAsString(0);

        fetchUniqueId(username, uniqueId -> {
            if (uniqueId == null) {
                message(sender, Message.PLAYER_NOT_FOUND, "%player%", username);
                return;
            }
            displayWarnings((ZEssentialsPlugin) plugin, sender, uniqueId, username);
        });

        return CommandResultType.SUCCESS;
    }

    private void displayWarnings(ZEssentialsPlugin plugin, CommandSender sender, UUID uuid, String username) {

        plugin.getScheduler().runAsync(wrappedTask -> {
            List<fr.maxlego08.essentials.api.dto.SanctionDTO> warnings = plugin.getStorageManager().getStorage()
                    .getSanctions(uuid).stream()
                    .filter(dto -> dto.sanction_type() == fr.maxlego08.essentials.api.sanction.SanctionType.WARN)
                    .toList();

            message(sender, Message.WARNINGS_HEADER, "%player%", username, "%count%", String.valueOf(warnings.size()));
            for (fr.maxlego08.essentials.api.dto.SanctionDTO dto : warnings) {
                String dateText = new java.text.SimpleDateFormat(
                        plugin.getModuleManager().getModule(fr.maxlego08.essentials.module.modules.SanctionModule.class)
                                .getDateFormatValue()).format(dto.created_at());
                message(sender, Message.WARNINGS_LINE, "%date%", dateText, "%reason%", dto.reason());
            }
        });
    }
}
