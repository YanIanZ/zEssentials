package dev.yanianz.essentials.reports;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.commands.CommandResultType;
import fr.maxlego08.essentials.api.commands.Permission;
import fr.maxlego08.essentials.api.messages.Message;
import fr.maxlego08.essentials.zutils.utils.commands.VCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

/**
 * Players report others and staff review the reports.
 * /reports lists the open ones for staff,
 * resolve and teleport subcommands take a numeric report id.
 */
public class CommandReport extends VCommand {

    public CommandReport(EssentialsPlugin plugin) {
        super(plugin);
        this.setModule(ReportsModule.class);
        this.setPermission(Permission.ESSENTIALS_REPORT_USE);
        this.setDescription(Message.DESCRIPTION_REPORT);
        this.addRequirePlayerNameArg();
        this.setExtendedArgs(true);
    }

    @Override
    protected CommandResultType perform(EssentialsPlugin plugin) {

        ReportsModule module = plugin.getModuleManager().getModule(ReportsModule.class);

        String first = this.argAsString(0);

        if (first.equalsIgnoreCase("list") || first.equalsIgnoreCase("resolve")
                || first.equalsIgnoreCase("reopen") || first.equalsIgnoreCase("tp")) {

            if (!hasPermission(sender, Permission.ESSENTIALS_REPORT_VIEW)) {
                return CommandResultType.NO_PERMISSION;
            }

            switch (first.toLowerCase()) {
                case "list" -> sendReportList(module, sender);
                case "tp" -> {
                    int id = this.argAsInteger(1, -1);
                    var targetUuid = module.getTargetUuid(id);
                    Player player = getPlayer();
                    if (targetUuid == null || player == null) {
                        message(sender, Message.REPORT_UNKNOWN_ID, "%id%", String.valueOf(id));
                        return CommandResultType.SUCCESS;
                    }
                    var targetPlayer = Bukkit.getPlayer(targetUuid);
                    if (targetPlayer == null) {
                        message(sender, Message.REPORT_TARGET_OFFLINE);
                        return CommandResultType.SUCCESS;
                    }
                    player.teleport(targetPlayer);
                    message(sender, Message.REPORT_TELEPORTED, "%player%", targetPlayer.getName());
                }
                default -> {
                    int id = this.argAsInteger(1, -1);
                    boolean resolved = !first.equalsIgnoreCase("reopen");
                    module.setResolved(id, resolved);
                    message(sender, resolved ? Message.REPORT_RESOLVED : Message.REPORT_REOPENED,
                            "%id%", String.valueOf(id));
                }
            }
            return CommandResultType.SUCCESS;
        }

        // Default usage: /report <player> <reason...>
        if (this.args.length < 2) {
            this.syntaxMessage();
            return CommandResultType.SUCCESS;
        }

        Player target = Bukkit.getPlayerExact(first);
        if (target == null) {
            message(sender, Message.PLAYER_NOT_FOUND, "%player%", first);
            return CommandResultType.SUCCESS;
        }

        Player reporter = getPlayer();
        if (reporter == null) return CommandResultType.SYNTAX_ERROR;

        StringBuilder reason = new StringBuilder();
        for (int index = 1; index < this.args.length; index++) {
            reason.append(this.args[index]).append(index == this.args.length - 1 ? "" : " ");
        }

        ReportsModule.Result result = module.create(reporter, target.getUniqueId(), target.getName(), reason.toString());
        switch (result) {
            case SELF -> message(sender, Message.REPORT_SELF);
            case COOLDOWN -> {
            } // the storage already sent the wait message
            default -> {
            }
        }
        return CommandResultType.SUCCESS;
    }

    private void sendReportList(ReportsModule module, CommandSender sender) {

        var open = module.getOpenReports().stream()
                .filter(report -> !report.resolved)
                .toList();

        message(sender, Message.REPORT_LIST_HEADER, "%count%", String.valueOf(open.size()));
        for (ReportsModule.Report report : open.stream().limit(15).toList()) {
            java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("MM-dd HH:mm");
            Component line = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection()
                    .deserialize(colorize("#" + report.id + " &f" + report.targetName
                            + " §7by &f" + report.reporterName
                            + " &8(" + format.format(new java.util.Date(report.createdAt)) + ")&7: &f" + report.reason));
            line = line.append(net.kyori.adventure.text.Component.text(" §8[§a✔§8]")
                            .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(
                                    net.kyori.adventure.text.Component.text("§aMark resolved")))
                            .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand(
                                    "/essentials:reports resolve " + report.id)))
                    .append(net.kyori.adventure.text.Component.text(" §8[§b➤§8]")
                            .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(
                                    net.kyori.adventure.text.Component.text("§bTeleport to target")))
                            .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand(
                                    "/essentials:reports tp " + report.id)));
            sender.sendMessage(line);
        }
    }

    private String colorize(String text) {
        return text == null ? "" : text.replace("&", "§");
    }
}
