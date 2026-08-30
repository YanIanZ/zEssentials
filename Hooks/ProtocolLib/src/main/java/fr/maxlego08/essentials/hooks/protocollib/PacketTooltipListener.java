package fr.maxlego08.essentials.hooks.protocollib;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.modules.Module;
import fr.maxlego08.essentials.api.modules.ModuleManager;
import fr.maxlego08.essentials.api.packet.PacketRegister;
import fr.maxlego08.essentials.api.pricing.PriceProvider;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class PacketTooltipListener extends PacketAdapter implements PacketRegister {

    private final EssentialsPlugin plugin;
    private final Class<? extends Module> pricingModuleClass;

    public PacketTooltipListener(EssentialsPlugin plugin) {
        super(PacketAdapter.params()
                .plugin(plugin)
                .listenerPriority(ListenerPriority.NORMAL)
                .types(PacketType.Play.Server.WINDOW_ITEMS, PacketType.Play.Server.SET_SLOT));
        this.plugin = plugin;
        this.pricingModuleClass = loadPricingModuleClass();
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends Module> loadPricingModuleClass() {
        try {
            return (Class<? extends Module>) Class.forName("fr.maxlego08.essentials.module.modules.PricingModule");
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    @Override
    public void addPacketListener() {
        com.comphenix.protocol.ProtocolLibrary.getProtocolManager().addPacketListener(this);
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        if (this.pricingModuleClass == null) return;
        ModuleManager manager = this.plugin.getModuleManager();
        Module module = manager.getModule(this.pricingModuleClass);
        if (module == null || !invokeBoolean(module, "isEnabled")) return;

        Player player = event.getPlayer();
        if (!invokeBoolean(module, "isToggleEnabled", player.getUniqueId())) return;

        if (event.getPacketType() == PacketType.Play.Server.WINDOW_ITEMS) {
            List<ItemStack> items = event.getPacket().getItemListModifier().read(0);
            if (items == null) return;
            for (int i = 0; i < items.size(); i++) {
                ItemStack modified = injectPrice(items.get(i), module);
                if (modified != null) items.set(i, modified);
            }
        } else if (event.getPacketType() == PacketType.Play.Server.SET_SLOT) {
            ItemStack item = event.getPacket().getItemModifier().read(0);
            if (item == null) return;
            ItemStack modified = injectPrice(item, module);
            if (modified != null) event.getPacket().getItemModifier().write(0, modified);
        }
    }

    private ItemStack injectPrice(ItemStack item, Module module) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return null;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        String marker = (String) invoke(module, "getMarker");
        if (marker == null) return null;
        if (lore.stream().anyMatch(line -> line.contains(marker))) return null;

        PriceProvider.PriceResult result = (PriceProvider.PriceResult) invoke(module, "resolvePrice", item);
        if (result == null || !result.hasAny()) return null;

        @SuppressWarnings("unchecked")
        List<String> loreLines = (List<String>) invoke(module, "getLoreLines");
        if (loreLines == null) return null;

        List<String> injectedLines = buildLoreLines(loreLines, result);
        if (injectedLines.isEmpty()) return null;

        lore.addAll(injectedLines);
        meta.setLore(lore);
        ItemStack clone = item.clone();
        clone.setItemMeta(meta);
        return clone;
    }

    private List<String> buildLoreLines(List<String> template, PriceProvider.PriceResult result) {
        List<String> lines = new ArrayList<>();
        for (String line : template) {
            String formatted = line;
            boolean hasPrice = false;

            if (result.sellPrice() != null) {
                formatted = formatted.replace("%sell%", formatPrice(result.sellPrice()));
                if (line.contains("%sell%")) hasPrice = true;
            } else {
                if (line.contains("%sell%")) continue;
            }

            if (result.buyPrice() != null) {
                formatted = formatted.replace("%buy%", formatPrice(result.buyPrice()));
                if (line.contains("%buy%")) hasPrice = true;
            } else {
                if (line.contains("%buy%")) continue;
            }

            if (result.npcPrice() != null) {
                formatted = formatted.replace("%npc%", formatPrice(result.npcPrice()));
                if (line.contains("%npc%")) hasPrice = true;
            } else {
                if (line.contains("%npc%")) continue;
            }

            formatted = formatted.replace("&", "§");
            lines.add(formatted);
        }
        return lines;
    }

    private String formatPrice(double price) {
        if (price >= 1_000_000_000) return String.format(java.util.Locale.US, "%.1fB", price / 1_000_000_000);
        if (price >= 1_000_000) return String.format(java.util.Locale.US, "%.1fM", price / 1_000_000);
        if (price >= 1_000) return String.format(java.util.Locale.US, "%.1fK", price / 1_000);
        return String.format(java.util.Locale.US, "%.2f", price);
    }

    private static Object invoke(Object target, String method, Object... args) {
        try {
            Class<?>[] types = new Class<?>[args.length];
            for (int i = 0; i < args.length; i++) types[i] = args[i].getClass();
            return target.getClass().getMethod(method, types).invoke(target, args);
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean invokeBoolean(Object target, String method, Object... args) {
        Object result = invoke(target, method, args);
        return result instanceof Boolean b && b;
    }
}
