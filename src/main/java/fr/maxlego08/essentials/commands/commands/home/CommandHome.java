package fr.maxlego08.essentials.commands.commands.home;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.commands.CommandResultType;
import fr.maxlego08.essentials.api.commands.Permission;
import fr.maxlego08.essentials.api.home.Home;
import fr.maxlego08.essentials.api.home.HomeManager;
import fr.maxlego08.essentials.api.messages.Message;
import fr.maxlego08.essentials.api.user.User;
import fr.maxlego08.essentials.module.modules.HomeModule;
import fr.maxlego08.essentials.zutils.utils.commands.VCommand;
import org.bukkit.Location;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Optional;

public class CommandHome extends VCommand {

    public CommandHome(EssentialsPlugin plugin) {
        super(plugin);
        this.setModule(HomeModule.class);
        this.setPermission(Permission.ESSENTIALS_HOME);
        this.setDescription(Message.DESCRIPTION_HOME);
        this.addOptionalArg("name", (sender, args) -> {
            if (sender instanceof Player player) {
                User user = plugin.getUser(player.getUniqueId());
                if (user == null) return new ArrayList<>();
                return user.getHomes().stream().map(Home::getName).toList();
            }
            return new ArrayList<>();
        });
        this.addOptionalArg("confirm", (s,a) -> java.util.List.of("confirm"));
    }

    @Override
    protected CommandResultType perform(EssentialsPlugin plugin) {

        String homeName = this.argAsString(0, null);
        HomeManager homeManager = plugin.getHomeManager();

        if (homeName == null) {

            if (this.sender instanceof ConsoleCommandSender) return CommandResultType.SYNTAX_ERROR;

            homeManager.sendHomes(player, user);
            return CommandResultType.SUCCESS;
        }

        // For /home Maxlego08:<home name> (admin teleport or public/shared visit)
        if (homeName.contains(":")) {

            if (this.sender instanceof ConsoleCommandSender) return CommandResultType.SYNTAX_ERROR;

            String[] values = homeName.split(":", 2);
            String username = values[0];
            String home = values[1];

            if (hasPermission(sender, Permission.ESSENTIALS_HOME_OTHER)) {
                homeManager.teleport(this.user, username, home);
            } else {
                homeManager.visitHome(this.user, username, home);
            }
            return CommandResultType.DEFAULT;
        }

        Optional<Home> optional = this.user.getHome(homeName);
        if (optional.isEmpty()) {
            message(sender, Message.COMMAND_HOME_DOESNT_EXIST, "%name%", homeName);
            return CommandResultType.DEFAULT;
        }

        Home home = optional.get();

        // Optional preview before teleporting
        HomeModule homeModule = plugin.getModuleManager().getModule(HomeModule.class);
        String confirm = this.argAsString(1, null);
        if (homeModule.isEnableHomePreview() && !"confirm".equalsIgnoreCase(confirm)) {
            Location location = home.getLocation();
            if (location != null && location.getWorld() != null) {
                message(sender, Message.COMMAND_HOME_PREVIEW, "%name%", home.getName(), "%world%", location.getWorld().getName(), "%x%", location.getBlockX(), "%y%", location.getBlockY(), "%z%", location.getBlockZ());
                // DEFAULT so the preview does not consume the /home cooldown (which would block the confirm step)
                return CommandResultType.DEFAULT;
            }
        }

        homeManager.teleport(user, home);

        return CommandResultType.SUCCESS;
    }
}
