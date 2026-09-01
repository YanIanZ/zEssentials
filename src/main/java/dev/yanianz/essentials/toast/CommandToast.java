package dev.yanianz.essentials.toast;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.commands.CommandResultType;
import fr.maxlego08.essentials.api.commands.Permission;
import fr.maxlego08.essentials.api.messages.Message;
import fr.maxlego08.essentials.zutils.utils.commands.VCommand;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class CommandToast extends VCommand {

    public CommandToast(EssentialsPlugin plugin) {
        super(plugin);
        this.setPermission(Permission.ESSENTIALS_TOAST);
        this.setDescription(Message.DESCRIPTION_TOAST);
        this.addRequireArg("message");
        this.setExtendedArgs(true);
    }

    @Override
    protected CommandResultType perform(EssentialsPlugin plugin) {
        String text = getArgs(0);

        Player target = this.player;
        int startIdx = 0;

        if (this.args.length > 1 && hasPermission(sender, Permission.ESSENTIALS_TOAST_OTHER)) {
            Player other = Bukkit.getPlayerExact(this.argAsString(0));
            if (other != null) {
                target = other;
                startIdx = 1;
                text = this.args.length > 1 ? getArgsFrom(1) : "";
            }
        }

        if (target == null) {
            return CommandResultType.SYNTAX_ERROR;
        }

        String icon = "minecraft:diamond";
        String message = text;
        if (text.contains("&&")) {
            String[] parts = text.split("&&", 2);
            message = parts[0];
            icon = parts[1].trim();
        }

        ToastSender.send(target, message, icon);

        if (target.equals(this.player)) {
            message(sender, Message.COMMAND_TOAST_SENT);
        } else {
            message(sender, Message.COMMAND_TOAST_SENT_OTHER, "%player%", target.getName());
            message(target, Message.COMMAND_TOAST_RECEIVED, "%player%", sender.getName());
        }

        return CommandResultType.SUCCESS;
    }

    private String getArgsFrom(int start) {
        if (start >= this.args.length) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < this.args.length; i++) {
            if (i > start) sb.append(" ");
            sb.append(this.args[i]);
        }
        return sb.toString();
    }
}
