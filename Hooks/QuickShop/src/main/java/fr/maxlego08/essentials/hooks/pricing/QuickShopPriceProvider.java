package fr.maxlego08.essentials.hooks.pricing;

import fr.maxlego08.essentials.api.pricing.PriceProvider;
import org.bukkit.inventory.ItemStack;

public class QuickShopPriceProvider implements PriceProvider {

    @Override
    public String getName() {
        return "QuickShop";
    }

    @Override
    public boolean isAvailable() {
        return org.bukkit.Bukkit.getPluginManager().isPluginEnabled("QuickShop")
                || org.bukkit.Bukkit.getPluginManager().isPluginEnabled("QuickShop-Hikari");
    }

    @Override
    public PriceResult getPrice(ItemStack item) {
        return null;
    }
}
