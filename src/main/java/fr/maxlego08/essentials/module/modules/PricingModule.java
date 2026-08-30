package fr.maxlego08.essentials.module.modules;

import dev.yanianz.essentials.pricing.PriceResolver;
import fr.maxlego08.essentials.ZEssentialsPlugin;
import fr.maxlego08.essentials.api.configuration.NonLoadable;
import fr.maxlego08.essentials.api.pricing.PriceProvider;
import fr.maxlego08.essentials.module.ZModule;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class PricingModule extends ZModule {

    private boolean enabled = true;
    private List<String> loreLines = new ArrayList<>();
    private String marker = "&8&lzE Pricing";
    private String togglePermission = "essentials.pricing.toggle";
    private PriceResolver resolver;

    @NonLoadable
    private final List<PriceProvider> providers = new ArrayList<>();

    @NonLoadable
    private final Set<UUID> disabledPlayers = new HashSet<>();

    public PricingModule(ZEssentialsPlugin plugin) {
        super(plugin, "pricing");
    }

    @Override
    public void loadConfiguration() {
        super.loadConfiguration();

        YamlConfiguration config = getConfiguration();
        this.enabled = config.getBoolean("enable", true);
        this.loreLines = config.getStringList("lore-lines");
        this.marker = config.getString("marker", "&8&lzE Pricing");
        this.togglePermission = config.getString("toggle-permission", "essentials.pricing.toggle");

        this.resolver = new PriceResolver(this.providers);
    }

    public void addProvider(PriceProvider provider) {
        this.providers.add(provider);
        this.resolver = new PriceResolver(this.providers);
    }

    public PriceProvider.PriceResult resolvePrice(ItemStack item) {
        if (!this.enabled || this.resolver == null) return null;
        return this.resolver.resolveBest(item);
    }

    public boolean isEnabled() {
        return this.enabled && this.isEnable;
    }

    public boolean isToggleEnabled(UUID playerId) {
        return !this.disabledPlayers.contains(playerId);
    }

    public boolean togglePlayer(UUID playerId) {
        if (this.disabledPlayers.contains(playerId)) {
            this.disabledPlayers.remove(playerId);
            return true;
        } else {
            this.disabledPlayers.add(playerId);
            return false;
        }
    }

    public List<String> getLoreLines() { return loreLines; }
    public String getMarker() { return marker; }
    public String getTogglePermission() { return togglePermission; }
    public List<PriceProvider> getProviders() { return providers; }
}
