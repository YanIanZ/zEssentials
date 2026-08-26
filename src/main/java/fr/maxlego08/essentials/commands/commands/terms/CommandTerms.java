package fr.maxlego08.essentials.commands.commands.terms;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.commands.CommandResultType;
import fr.maxlego08.essentials.api.commands.Permission;
import fr.maxlego08.essentials.api.messages.Message;
import fr.maxlego08.essentials.module.modules.terms.TermsModule;
import fr.maxlego08.essentials.zutils.utils.commands.VCommand;
import org.bukkit.entity.Player;

public class CommandTerms extends VCommand {

    public CommandTerms(EssentialsPlugin plugin) {
        super(plugin);
        this.setModule(TermsModule.class);
        this.setDescription(Message.DESCRIPTION_TERMS);
        this.addSubCommand("accept", "a");
        this.addSubCommand("deny", "refuse", "d");
        this.addSubCommand("reload", "r");
        this.addSubCommand("reset");

        this.setExtendedArgs(true);
    }

    @Override
    protected CommandResultType perform(EssentialsPlugin plugin) {

        TermsModule termsModule = plugin.getModuleManager().getModule(TermsModule.class);

        if (this.args.length == 0) {
            this.syntaxMessage();
            return CommandResultType.SUCCESS;
        }

        switch (this.args[0].toLowerCase()) {
            case "accept" -> {
                Player player = getPlayer();
                if (player == null) return CommandResultType.SYNTAX_ERROR;
                termsModule.accept(player);
            }
            case "deny" -> {
                Player player = getPlayer();
                if (player == null) return CommandResultType.SYNTAX_ERROR;
                termsModule.deny(player);
            }
            case "reload" -> {
                if (!hasPermission(sender, Permission.ESSENTIALS_TERMS_ADMIN)) {
                    return CommandResultType.NO_PERMISSION;
                }
                termsModule.loadConfiguration();
                message(sender, Message.COMMAND_RELOAD_MODULE, "%module%", "terms");
            }
            case "reset" -> {
                if (!hasPermission(sender, Permission.ESSENTIALS_TERMS_ADMIN)) {
                    return CommandResultType.NO_PERMISSION;
                }
                if (this.args.length < 2) {
                    this.syntaxMessage();
                    return CommandResultType.SUCCESS;
                }
                String targetName = getArgs(1);
                fetchUniqueId(targetName, uniqueId -> {
                    if (uniqueId == null) {
                        message(sender, Message.PLAYER_NOT_FOUND, "%player%", targetName);
                        return;
                    }
                    termsModule.reset(uniqueId);
                    message(sender, Message.COMMAND_TERMS_RESET, "%player%", targetName);
                });
            }
            default -> syntaxMessage();
        }

        return CommandResultType.SUCCESS;
    }
}
