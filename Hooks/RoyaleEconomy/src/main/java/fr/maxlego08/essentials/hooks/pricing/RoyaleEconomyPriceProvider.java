package fr.maxlego08.essentials.hooks.pricing;

import fr.maxlego08.essentials.api.pricing.PriceProvider;
import org.bukkit.inventory.ItemStack;

public class RoyaleEconomyPriceProvider implements PriceProvider {

    @Override
    public String getName() {
        return "RoyaleEconomy";
    }

    @Override
    public boolean isAvailable() {
        return org.bukkit.Bukkit.getPluginManager().isPluginEnabled("RoyaleEconomy");
    }

    @Override
    public PriceResult getPrice(ItemStack item) {
        return null;
    }
}
