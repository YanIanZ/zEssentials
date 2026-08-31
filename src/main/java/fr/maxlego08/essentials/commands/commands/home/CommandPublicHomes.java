package fr.maxlego08.essentials.commands.commands.home;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.commands.CommandResultType;
import fr.maxlego08.essentials.api.commands.Permission;
import fr.maxlego08.essentials.api.dto.PublicHomeDTO;
import fr.maxlego08.essentials.api.home.PublicHomesDisplay;
import fr.maxlego08.essentials.api.messages.Message;
import fr.maxlego08.essentials.api.storage.IStorage;
import fr.maxlego08.essentials.module.modules.HomeModule;
import fr.maxlego08.essentials.zutils.utils.commands.VCommand;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;

public class CommandPublicHomes extends VCommand {

    public CommandPublicHomes(EssentialsPlugin plugin) {
        super(plugin);
        this.setModule(HomeModule.class);
        this.setPermission(Permission.ESSENTIALS_HOME_VISIT);
        this.setDescription(Message.DESCRIPTION_PUBLIC_HOMES);
        this.onlyPlayers();
        this.addOptionalArg("player", getVisiblePlayerNames());
    }

    @Override
    protected CommandResultType perform(EssentialsPlugin plugin) {

        HomeModule homeModule = plugin.getModuleManager().getModule(HomeModule.class);
        if (!homeModule.isEnablePublicHomes()) {
            message(sender, Message.COMMAND_HOME_FEATURE_DISABLED);
            return CommandResultType.DEFAULT;
        }

        String filter = this.argAsString(0, null);
        IStorage iStorage = plugin.getStorageManager().getStorage();

        iStorage.getPublicHomes(publicHomes -> {

            List<PublicHomeDTO> filtered = new ArrayList<>();
            for (PublicHomeDTO dto : publicHomes) {
                String ownerName = Bukkit.getOfflinePlayer(dto.unique_id()).getName();
                if (ownerName == null) continue;
                if (filter != null && !ownerName.equalsIgnoreCase(filter)) continue;
                filtered.add(dto);
            }

            if (filtered.isEmpty()) {
                message(sender, Message.COMMAND_PUBLIC_HOMES_EMPTY);
                return;
            }

            if (homeModule.getPublicHomesDisplay() == PublicHomesDisplay.INVENTORY) {
                homeModule.setCachedPublicHomes(this.player.getUniqueId(), filtered);
                plugin.getScheduler().runAtEntity(this.player, wrappedTask -> homeModule.openPublicHomesInventory(this.player));
                return;
            }

            message(sender, Message.COMMAND_PUBLIC_HOMES_HEADER, "%count%", filtered.size());
            for (PublicHomeDTO dto : filtered) {
                String ownerName = Bukkit.getOfflinePlayer(dto.unique_id()).getName();
                message(sender, Message.COMMAND_PUBLIC_HOMES_LINE, "%player%", ownerName, "%name%", dto.name());
            }
        });

        return CommandResultType.SUCCESS;
    }
}
