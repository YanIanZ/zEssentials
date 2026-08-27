package dev.yanianz.essentials.reputation;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.commands.CommandResultType;
import fr.maxlego08.essentials.api.commands.Permission;
import fr.maxlego08.essentials.api.messages.Message;
import fr.maxlego08.essentials.zutils.utils.commands.VCommand;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Gives one reputation point to the target player.
 */
public class CommandRepGive extends VCommand {

    public CommandRepGive(EssentialsPlugin plugin) {
        super(plugin);
        this.setModule(ReputationModule.class);
        this.setPermission(Permission.ESSENTIALS_REPUTATION_USE);
        this.setDescription(Message.DESCRIPTION_REPUTATION_GIVE);
        this.addRequirePlayerNameArg();
        this.onlyPlayers();
    }

    @Override
    protected CommandResultType perform(EssentialsPlugin plugin) {

        ReputationModule module = plugin.getModuleManager().getModule(ReputationModule.class);

        if (module.givePermission() != null && !module.givePermission().isEmpty()
                && !player.hasPermission(module.givePermission())) {
            message(sender, Message.COMMAND_NO_PERMISSION);
            return CommandResultType.SUCCESS;
        }

        Player target = this.argAsPlayer(0);
        if (target == null) {
            message(sender, Message.PLAYER_NOT_FOUND, "%player%", this.argAsString(0));
            return CommandResultType.SUCCESS;
        }

        ReputationModule.Result result = module.give(this.player.getUniqueId(), target.getUniqueId());
        switch (result) {
            case SELF -> message(sender, Message.REPUTATION_SELF);
            case ALREADY -> message(sender, Message.REPUTATION_ALREADY, "%hours%", String.valueOf(module.cooldownHours()));
            case SUCCESS -> {
                int score = module.getScore(target.getUniqueId());
                message(sender, Message.REPUTATION_GIVEN, "%player%", target.getName(), "%score%", String.valueOf(score));

                if (!module.broadcastLine().isEmpty()) {
                    java.util.function.Function<String, net.kyori.adventure.text.Component> legacy =
                            text -> net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection()
                                    .deserialize(text == null ? "" : text.replace("&", "§"));
                    String line = module.broadcastLine()
                            .replace("%player%", target.getName())
                            .replace("%total%", String.valueOf(score));
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        online.sendMessage(legacy.apply(line));
                    }
                }
            }
        }
        return CommandResultType.SUCCESS;
    }
}
