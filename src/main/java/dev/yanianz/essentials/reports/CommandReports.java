package dev.yanianz.essentials.reports;

import dev.yanianz.essentials.screens.EssentialsScreens;
import dev.yanianz.essentials.screens.ScreenFactory;
import dev.yanianz.essentials.util.ColorUtil;
import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.commands.CommandResultType;
import fr.maxlego08.essentials.api.commands.Permission;
import fr.maxlego08.essentials.api.messages.Message;
import fr.maxlego08.essentials.zutils.utils.commands.VCommand;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Staff facing review of the reports opened by {@link CommandReport}.
 * /reports opens the screen, /reports list reopens it,
 * /reports resolve {@code <id>}, /reports reopen {@code <id>} and /reports tp {@code <id>}
 * act on a single report.
 */
public class CommandReports extends VCommand {

    public CommandReports(EssentialsPlugin plugin) {
        super(plugin);
        this.setModule(ReportsModule.class);
        this.setPermission(Permission.ESSENTIALS_REPORT_VIEW);
        this.setDescription(Message.DESCRIPTION_REPORT);
        this.addOptionalArg("action", (sender, args) ->
                List.of("list", "resolve", "reopen", "tp"));
        this.setExtendedArgs(true);
    }

    @Override
    protected CommandResultType perform(EssentialsPlugin plugin) {

        ReportsModule module = plugin.getModuleManager().getModule(ReportsModule.class);

        if (this.args.length == 0) {
            if (sender instanceof Player playerView) sendReportScreen(module, playerView);
            else sendReportChatList(module, sender);
            return CommandResultType.SUCCESS;
        }

        String first = this.argAsString(0);

        switch (first == null ? "" : first.toLowerCase()) {
            case "list" -> {
                if (sender instanceof Player playerView) sendReportScreen(module, playerView);
                else sendReportChatList(module, sender);
            }
            case "tp" -> {
                int id = this.argAsInteger(1, -1);
                Player teleporter = getPlayer();
                if (teleporter == null) {
                    message(sender, Message.COMMAND_NO_CONSOLE);
                    return CommandResultType.SUCCESS;
                }
                var targetUuid = module.getTargetUuid(id);
                if (targetUuid == null) {
                    message(sender, Message.REPORT_UNKNOWN_ID, "%id%", String.valueOf(id));
                    return CommandResultType.SUCCESS;
                }
                var targetPlayer = Bukkit.getPlayer(targetUuid);
                if (targetPlayer == null) {
                    message(sender, Message.REPORT_TARGET_OFFLINE);
                    return CommandResultType.SUCCESS;
                }
                teleporter.closeInventory();
                this.plugin.getScheduler().teleportAsync(teleporter, targetPlayer.getLocation());
                message(sender, Message.REPORT_TELEPORTED, "%player%", targetPlayer.getName());
            }
            case "resolve", "reopen" -> {
                int id = this.argAsInteger(1, -1);
                boolean resolved = first.equalsIgnoreCase("resolve");
                module.setResolved(id, resolved);
                message(sender, resolved ? Message.REPORT_RESOLVED : Message.REPORT_REOPENED,
                        "%id%", String.valueOf(id));
            }
            default -> {
                if (sender instanceof Player playerView) sendReportScreen(module, playerView);
                else sendReportChatList(module, sender);
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
        List<ScreenFactory.ScreenItem> items = new ArrayList<>();

        for (ReportsModule.Report report : open) {
            int id = report.id;
            String reason = report.reason;
            items.add(new ScreenFactory.ScreenItem(
                    Material.PAPER,
                    "&#ff4d4d#" + id + " &f" + report.targetName,
                    List.of("&7By: &f" + report.reporterName,
                            "&7Date&8: &f" + format.format(new java.util.Date(report.createdAt)),
                            "&7Reason&8: &f" + reason,
                            "",
                            "&aLeft click &7resolve",
                            "&bRight click &7teleport to target"),
                    (playerView, clickEvent) -> {
                        if (clickEvent != null && clickEvent.getClick().isRightClick()) {
                            var targetUuid = module.getTargetUuid(id);
                            var online = targetUuid == null ? null : Bukkit.getPlayer(targetUuid);
                            if (online != null) {
                                playerView.closeInventory();
                                plugin.getScheduler().teleportAsync(playerView, online.getLocation());
                                playerView.sendMessage(ColorUtil.component("&bTeleported to &f" + online.getName() + "&b."));
                            } else {
                                playerView.sendMessage(ColorUtil.component("&cThe reported player is offline."));
                            }
                            return;
                        }
                        // Left click resolves
                        module.setResolved(id, true);
                        playerView.sendMessage(ColorUtil.component("&aReport #" + id + " resolved."));
                        sendReportScreen(module, playerView); // refresh
                    }));
        }

        if (open.isEmpty()) {
            viewer.sendMessage(ColorUtil.component("&7No open reports."));
            return;
        }

        EssentialsScreens.get().factory().open(viewer, "&4&lREPORTS", 6, items);
    }

    private void sendReportChatList(ReportsModule module, CommandSender sender) {
        message(sender, Message.REPORT_LIST_HEADER, "%count%", "0");
    }
}
