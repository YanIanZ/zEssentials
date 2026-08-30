package fr.maxlego08.essentials.hooks.pricing;

import fr.maxlego08.essentials.api.pricing.PriceProvider;
import org.bukkit.inventory.ItemStack;

public class EconomyShopGUIPriceProvider implements PriceProvider {

    @Override
    public String getName() {
        return "EconomyShopGUI";
    }

    @Override
    public boolean isAvailable() {
        return org.bukkit.Bukkit.getPluginManager().isPluginEnabled("EconomyShopGUI");
    }

    @Override
    public PriceResult getPrice(ItemStack item) {
        return null;
    }
}
