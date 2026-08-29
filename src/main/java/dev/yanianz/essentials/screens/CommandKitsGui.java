package dev.yanianz.essentials.screens;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.commands.CommandResultType;
import fr.maxlego08.essentials.api.commands.Permission;
import fr.maxlego08.essentials.api.kit.Kit;
import fr.maxlego08.essentials.api.messages.Message;
import fr.maxlego08.essentials.api.user.User;
import fr.maxlego08.essentials.module.modules.kit.KitModule;
import fr.maxlego08.essentials.zutils.utils.TimerBuilder;
import fr.maxlego08.essentials.zutils.utils.commands.VCommand;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CommandKitsGui extends VCommand {

    public CommandKitsGui(EssentialsPlugin plugin) {
        super(plugin);
        this.setModule(KitModule.class);
        this.setPermission(Permission.ESSENTIALS_KIT);
        this.setDescription(Message.DESCRIPTION_KITS_GUI);
        this.onlyPlayers();
    }

    @Override
    protected CommandResultType perform(EssentialsPlugin plugin) {
        User user = this.user;
        if (user == null) return CommandResultType.SYNTAX_ERROR;

        KitModule kitModule = plugin.getModuleManager().getModule(KitModule.class);
        List<Kit> kits = kitModule.getKits(player);
        if (kits.isEmpty()) {
            player.sendMessage(net.kyori.adventure.text.Component.text("No kits available."));
            return CommandResultType.SUCCESS;
        }

        List<ScreenFactory.ScreenItem> items = new ArrayList<>();
        for (Kit kit : kits) {
            boolean onCooldown = user.isKitCooldown(kit);
            String name = onCooldown ? "&7" + kit.getName() : "&a" + kit.getName();

            List<String> lore = new ArrayList<>();
            lore.add("&7Cooldown&8: &f" + (kit.getCooldown(player) > 0 ? TimerBuilder.getStringTime((double) kit.getCooldown(player) * 1000L) : "None"));
            if (onCooldown) {
                long remaining = user.getKitCooldown(kit) - System.currentTimeMillis();
                lore.add("&cOn cooldown&8: &f" + TimerBuilder.getStringTime((double) remaining));
            } else {
                lore.add("&aAvailable!");
            }
            lore.add("");
            lore.add("&aLeft click &7claim");
            lore.add("&bRight click &7preview");

            Material material = Material.CHEST;
            try {
                if (kit.getMenuItemStacks() != null && !kit.getMenuItemStacks().isEmpty()) {
                    material = kit.getMenuItemStacks().get(0).build(player, false).getType();
                }
            } catch (Exception ignored) {
            }

            items.add(new ScreenFactory.ScreenItem(material, name, lore, (viewer, event) -> {
                if (event.getClick().isRightClick()) {
                    user.openKitPreview(kit);
                } else {
                    plugin.giveKit(user, kit, false);
                }
            }));
        }

        EssentialsScreens.get().factory().open(player, "&8Kits", 6, items);
        return CommandResultType.SUCCESS;
    }
}
