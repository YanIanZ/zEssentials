package fr.maxlego08.essentials.buttons.kit;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.kit.Kit;
import fr.maxlego08.essentials.api.user.User;
import fr.maxlego08.menu.api.utils.Placeholders;
import fr.maxlego08.menu.api.button.Button;
import fr.maxlego08.menu.api.engine.InventoryEngine;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;

public class ButtonKitClaim extends Button {

    private final EssentialsPlugin plugin;

    public ButtonKitClaim(EssentialsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event, InventoryEngine inventory, int slot, Placeholders placeholders) {
        User user = this.plugin.getUser(player.getUniqueId());
        if (user == null) return;
        Kit kit = user.getKitPreview();
        if (kit == null) return;
        this.plugin.giveKit(user, kit, false);
        super.onClick(player, event, inventory, slot, placeholders);
    }

    @Override
    public @NonNull ItemStack getCustomItemStack(@NonNull Player player, boolean useCache, @NonNull Placeholders placeholders) {
        return this.getItemStack().build(player, false, placeholders);
    }

    @Override
    public boolean hasPermission() {
        return true;
    }
}
