package fr.maxlego08.essentials.api.pricing;

import org.bukkit.inventory.ItemStack;

public interface PriceProvider {

    String getName();

    boolean isAvailable();

    PriceResult getPrice(ItemStack item);

    record PriceResult(
        Double sellPrice,
        Double buyPrice,
        Double npcPrice,
        String source
    ) {
        public boolean hasAny() {
            return sellPrice != null || buyPrice != null || npcPrice != null;
        }
    }
}
