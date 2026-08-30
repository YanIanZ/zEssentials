package dev.yanianz.essentials.pricing;

import fr.maxlego08.essentials.api.pricing.PriceProvider;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class PriceResolver {

    private final List<PriceProvider> providers;

    public PriceResolver(List<PriceProvider> providers) {
        this.providers = providers;
    }

    public PriceProvider.PriceResult resolveBest(ItemStack item) {
        if (item == null || item.getType().isAir()) return null;

        PriceProvider.PriceResult best = null;
        double bestSell = Double.MIN_VALUE;
        double bestBuy = Double.MAX_VALUE;
        Double npcPrice = null;

        for (PriceProvider provider : providers) {
            if (!provider.isAvailable()) continue;
            PriceProvider.PriceResult result = provider.getPrice(item);
            if (result == null || !result.hasAny()) continue;

            if (result.sellPrice() != null && result.sellPrice() > bestSell) {
                bestSell = result.sellPrice();
            }
            if (result.buyPrice() != null && result.buyPrice() < bestBuy) {
                bestBuy = result.buyPrice();
            }
            if (result.npcPrice() != null) {
                npcPrice = result.npcPrice();
            }
        }

        Double sell = bestSell == Double.MIN_VALUE ? null : bestSell;
        Double buy = bestBuy == Double.MAX_VALUE ? null : bestBuy;

        if (sell == null && buy == null && npcPrice == null) return null;

        return new PriceProvider.PriceResult(sell, buy, npcPrice, "resolved");
    }
}
