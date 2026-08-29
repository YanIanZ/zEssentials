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
import org.bukkit.Material;
import java.util.ArrayList;
import java.util.List;
import dev.yanianz.essentials.screens.EssentialsScreens;

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
        this.addOptionalArg("player", (sender, args) ->
                List.of("list", "resolve", "reopen", "tp"));
        this.setExtendedArgs(true);
    }

    @Override
    protected CommandResultType perform(EssentialsPlugin plugin) {

        ReportsModule module = plugin.getModuleManager().getModule(ReportsModule.class);

                if (this.args.length == 0) {
            if (hasPermission(sender, Permission.ESSENTIALS_REPORT_VIEW)) {
                sendReportScreen(module, player != null ? player : sender instanceof Player p ? p : null);
                return CommandResultType.SUCCESS;
            }
            message(sender, Message.DESCRIPTION_REPORT);
            return CommandResultType.SUCCESS;
        }

        String first = this.args.length > 0 ? this.argAsString(0) : "";

        if (first.equalsIgnoreCase("list") || first.equalsIgnoreCase("resolve")
                || first.equalsIgnoreCase("reopen") || first.equalsIgnoreCase("tp")) {

            if (!hasPermission(sender, Permission.ESSENTIALS_REPORT_VIEW)) {
                return CommandResultType.NO_PERMISSION;
            }

            switch (first.toLowerCase()) {
                case "list" -> {
                    if (sender instanceof Player playerView) sendReportScreen(module, playerView);
                    else sendReportChatList(module, sender);
                }
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

    /**
     * Staff screen: every open report is one clickable paper resolving it,
     * with the teleport action explained in the lore.
     */
    private void sendReportScreen(ReportsModule module, Player viewer) {

        List<ReportsModule.Report> open = module.getOpenReports().stream()
                .filter(report -> !report.resolved)
                .toList();

        java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("MM-dd HH:mm");
        List<dev.yanianz.essentials.screens.ScreenFactory.ScreenItem> items = new ArrayList<>();

        for (ReportsModule.Report report : open) {
            int id = report.id;
            String reason = report.reason;
            items.add(new dev.yanianz.essentials.screens.ScreenFactory.ScreenItem(
                    Material.PAPER,
                    "&#ff4d4d#" + id + " &f" + report.targetName,
                    List.of(colorize("&7By: &f" + report.reporterName),
                            colorize("&7Date&8: &f" + format.format(new java.util.Date(report.createdAt))),
                            colorize("&7Reason&8: &f" + reason),
                            "",
                            colorize("&aLeft click &7resolve"),
                            colorize("&bRight click &7teleport to target")),
                    (playerView, clickEvent) -> {
                        if (clickEvent != null && clickEvent.getClick().isRightClick()) {
                            var targetUuid = module.getTargetUuid(id);
                            var online = targetUuid == null ? null : Bukkit.getPlayer(targetUuid);
                            if (online != null) playerView.teleport(online);
                            else playerView.sendMessage(net.kyori.adventure.text.Component.text("Target offline."));
                            return;
                        }
                        // Left click resolves
                        module.setResolved(id, true);
                        playerView.sendMessage(dev.yanianz.essentials.util.ColorUtil.component(
                                "&aReport #" + id + " resolved."));
                        sendReportScreen(module, playerView); // refresh
                    }));
        }

        EssentialsScreens.get().factory().open(viewer, "&4&lREPORTS", 6, items);

        if (open.isEmpty()) {
            viewer.sendMessage(net.kyori.adventure.text.Component.text("No open reports."));
        }
    }

    private void sendReportChatList(ReportsModule module, CommandSender sender) {

        message(sender, Message.REPORT_LIST_HEADER, "%count%", "0");
    }

    private String colorize(String text) {
        return text == null ? "" : text.replace("&", "§");
    }
}
