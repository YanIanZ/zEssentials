package dev.yanianz.essentials.screens;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.commands.CommandResultType;
import fr.maxlego08.essentials.api.commands.Permission;
import fr.maxlego08.essentials.api.home.Home;
import fr.maxlego08.essentials.api.user.User;
import fr.maxlego08.essentials.module.modules.HomeModule;
import fr.maxlego08.essentials.zutils.utils.commands.VCommand;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CommandHomesGui extends VCommand {

    public CommandHomesGui(EssentialsPlugin plugin) {
        super(plugin);
        this.setModule(HomeModule.class);
        this.setPermission(Permission.ESSENTIALS_HOME);
        this.setDescription("Open the homes screen");
        this.onlyPlayers();
    }

    @Override
    protected CommandResultType perform(EssentialsPlugin plugin) {
        User user = this.user;
        if (user == null) return CommandResultType.SYNTAX_ERROR;

        List<Home> homes = user.getHomes();
        if (homes.isEmpty()) {
            player.sendMessage(net.kyori.adventure.text.Component.text("You don't have any homes."));
            return CommandResultType.SUCCESS;
        }

        HomeModule homeModule = plugin.getModuleManager().getModule(HomeModule.class);
        final int max = homeModule.getMaxHome(player);
        Material defaultMat = Material.matchMaterial(homeModule.getDefaultHomeMaterial());
        final Material defaultMaterial = defaultMat != null ? defaultMat : Material.BLUE_BED;

        List<ScreenFactory.ScreenItem> items = new ArrayList<>();
        for (Home home : homes) {
            Material mat = home.getMaterial() != null ? home.getMaterial() : defaultMaterial;
            String name = "&f" + home.getName();
            List<String> lore = new ArrayList<>();
            lore.add("&7World&8: &f" + (home.getLocation().getWorld() != null ? home.getLocation().getWorld().getName() : "?"));
            lore.add("&7X&8: &f" + home.getLocation().getBlockX() + " &7Y&8: &f" + home.getLocation().getBlockY() + " &7Z&8: &f" + home.getLocation().getBlockZ());
            if (home.isPublic()) lore.add("&aPublic");
            if (home.isFavorite()) lore.add("&6★ Favorite");
            lore.add("");
            lore.add("&aLeft click &7teleport");
            lore.add("&cRight click &7delete");

            items.add(new ScreenFactory.ScreenItem(mat, name, lore, (viewer, event) -> {
                if (event.getClick().isRightClick()) {
                    homeModule.deleteHome(viewer, user, home.getName());
                    EssentialsScreens.get().factory().open(viewer, "&8Homes", 6, rebuildItems(user, homeModule, max, defaultMaterial));
                } else {
                    viewer.closeInventory();
                    homeModule.teleport(user, home);
                }
            }));
        }

        EssentialsScreens.get().factory().open(player, "&8Homes &7(" + homes.size() + "/" + max + ")", 6, items);
        return CommandResultType.SUCCESS;
    }

    private List<ScreenFactory.ScreenItem> rebuildItems(User user, HomeModule homeModule, int max, Material defaultMaterial) {
        List<Home> homes = user.getHomes();
        List<ScreenFactory.ScreenItem> items = new ArrayList<>();
        for (Home home : homes) {
            Material mat = home.getMaterial() != null ? home.getMaterial() : defaultMaterial;
            String name = "&f" + home.getName();
            List<String> lore = new ArrayList<>();
            lore.add("&7World&8: &f" + (home.getLocation().getWorld() != null ? home.getLocation().getWorld().getName() : "?"));
            lore.add("&aLeft click &7teleport");
            lore.add("&cRight click &7delete");
            items.add(new ScreenFactory.ScreenItem(mat, name, lore, (viewer, event) -> {
                if (event.getClick().isRightClick()) {
                    homeModule.deleteHome(viewer, user, home.getName());
                    EssentialsScreens.get().factory().open(viewer, "&8Homes", 6, rebuildItems(user, homeModule, max, defaultMaterial));
                } else {
                    viewer.closeInventory();
                    homeModule.teleport(user, home);
                }
            }));
        }
        return items;
    }
}
