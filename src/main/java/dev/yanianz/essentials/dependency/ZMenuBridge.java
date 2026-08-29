package dev.yanianz.essentials.dependency;

import fr.maxlego08.essentials.ZEssentialsPlugin;
import fr.maxlego08.menu.api.loader.NoneLoader;
import fr.maxlego08.menu.api.utils.version.MinecraftVersion;
import fr.maxlego08.essentials.buttons.*;
import fr.maxlego08.essentials.buttons.kit.ButtonKitPreview;
import fr.maxlego08.essentials.buttons.kit.ButtonKitClaim;
import fr.maxlego08.essentials.buttons.mail.ButtonMailBox;
import fr.maxlego08.essentials.buttons.mail.ButtonMailBoxAdmin;
import fr.maxlego08.essentials.buttons.sanction.ButtonSanctionInformation;
import fr.maxlego08.essentials.buttons.sanction.ButtonSanctions;
import fr.maxlego08.essentials.buttons.vault.*;
import fr.maxlego08.essentials.loader.*;
import org.bukkit.entity.Player;

/**
 * Holds every direct call into the zMenu api that used to live in the main
 * plugin class. The JVM verifies all method bodies of a class when it links it,
 * so a single reference to an absent zMenu type inside {@code ZEssentialsPlugin}
 * would fail its verification before the dependency resolver ever gets the
 * chance to install zMenu. Keeping these calls here moves their verification
 * after the resolver ran.
 */
public final class ZMenuBridge {

    private ZMenuBridge() {
    }

    /**
     * Registers every custom button loader of the plugin.
     */
    public static void registerButtons(ZEssentialsPlugin plugin) {

        plugin.getButtonManager().register(new NoneLoader(plugin, ButtonTeleportationConfirmHere.class, "ZESSENTIALS_TELEPORTATION_CONFIRM_HERE"));
        plugin.getButtonManager().register(new NoneLoader(plugin, ButtonTeleportationConfirm.class, "ZESSENTIALS_TELEPORTATION_CONFIRM"));
        plugin.getButtonManager().register(new NoneLoader(plugin, ButtonPayConfirm.class, "ZESSENTIALS_PAY_CONFIRM"));
        plugin.getButtonManager().register(new NoneLoader(plugin, ButtonHomes.class, "ZESSENTIALS_HOMES"));
        plugin.getButtonManager().register(new NoneLoader(plugin, ButtonPublicHomes.class, "ZESSENTIALS_PUBLIC_HOMES"));
        plugin.getButtonManager().register(new NoneLoader(plugin, ButtonSanctionInformation.class, "ZESSENTIALS_SANCTION_INFORMATION"));
        plugin.getButtonManager().register(new NoneLoader(plugin, ButtonSanctions.class, "ZESSENTIALS_SANCTIONS"));
        plugin.getButtonManager().register(new NoneLoader(plugin, ButtonKitPreview.class, "ZESSENTIALS_KIT_PREVIEW"));
        plugin.getButtonManager().register(new NoneLoader(plugin, ButtonKitClaim.class, "ZESSENTIALS_KIT_CLAIM"));
        plugin.getButtonManager().register(new NoneLoader(plugin, ButtonMailBox.class, "ZESSENTIALS_MAILBOX"));
        plugin.getButtonManager().register(new NoneLoader(plugin, ButtonMailBoxAdmin.class, "ZESSENTIALS_MAILBOX_ADMIN"));
        plugin.getButtonManager().register(new NoneLoader(plugin, ButtonVaultSlotDisable.class, "ZESSENTIALS_VAULT_SLOTS_DISABLE"));
        plugin.getButtonManager().register(new NoneLoader(plugin, ButtonVaultSlotItems.class, "ZESSENTIALS_VAULT_SLOTS_ITEMS"));
        plugin.getButtonManager().register(new NoneLoader(plugin, ButtonVaultIcon.class, "ZESSENTIALS_VAULT_CHANGE_ICON"));
        plugin.getButtonManager().register(new NoneLoader(plugin, ButtonVaultRename.class, "ZESSENTIALS_VAULT_CHANGE_NAME"));
        plugin.getButtonManager().register(new ButtonWarpLoader(plugin));
        plugin.getButtonManager().register(new ButtonSanctionLoader(plugin));
        plugin.getButtonManager().register(new ButtonKitCooldownLoader(plugin));
        plugin.getButtonManager().register(new ButtonKitGetLoader(plugin));
        plugin.getButtonManager().register(new ButtonVaultOpenLoader(plugin));
        plugin.getButtonManager().register(new ButtonVaultNoPermissionLoader(plugin));
        plugin.getButtonManager().register(new ButtonVaultOpenAdminLoader(plugin));
        plugin.getButtonManager().register(new ButtonVaultNoPermissionAdminLoader(plugin));
        plugin.getButtonManager().register(new ButtonOptionLoader(plugin));
    }

    /**
     * Opens a loaded inventory for a player, keeping track of the previously
     * opened inventory for the back action.
     */
    public static void openInventory(ZEssentialsPlugin plugin, Player player, String inventoryName) {
        plugin.getInventoryManager().getInventory(plugin, inventoryName).ifPresent(inventory ->
                plugin.getScheduler().runAtLocation(player.getLocation(), wrappedTask -> {
                    plugin.getInventoryManager().getCurrentPlayerInventory(player).ifPresentOrElse(
                            oldInventory -> plugin.getInventoryManager().openInventory(player, inventory, 1, oldInventory),
                            () -> plugin.getInventoryManager().openInventory(player, inventory));
                }));
    }

    /**
     * Checks if the server is running at least minecraft 1.21.
     */
    public static boolean isAtLeast121() {
        return MinecraftVersion.getCurrentVersion().isAtLeast(MinecraftVersion.parse("1.21"));
    }
}
