package dev.yanianz.essentials.screens;

import fr.maxlego08.essentials.ZEssentialsPlugin;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Bootstrap of the screen factory with the built in screens of the plugin,
 * other plugins can reuse the same factory through this entry point.
 */
public final class EssentialsScreens {

    private static EssentialsScreens instance;

    private final ZEssentialsPlugin plugin;
    private final ScreenFactory factory;

    public EssentialsScreens(ZEssentialsPlugin plugin) {
        this.plugin = plugin;
        this.factory = new ScreenFactory(plugin);
    }

    public static void register(ZEssentialsPlugin plugin) {
        instance = new EssentialsScreens(plugin);
    }

    public static EssentialsScreens get() {
        return instance;
    }

    public ScreenFactory factory() {
        return this.factory;
    }

    /**
     * Opens the balance top screen for an economy using player heads.
     *
     * @param economyName the economy name registered in the economy manager
     */
    public void openBaltop(Player player, String economyName, int rows) {

        var economyManager = this.plugin.getEconomyManager();
        var economyOptional = economyManager.getEconomy(economyName);
        if (economyOptional.isEmpty()) {
            player.sendMessage(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                    .legacySection().deserialize(colorize("&cUnknown economy: &f" + economyName)));
            return;
        }

        var baltop = economyManager.getBaltop(economyOptional.get());
        List<ScreenFactory.ScreenItem> items = new ArrayList<>();

        for (int position = 1; position <= 100; position++) {
            var entryOptional = baltop.getPosition(position);
            if (entryOptional.isEmpty()) break;

            var entry = entryOptional.get();
            java.math.BigDecimal amount = entry.getAmount();
            String formatted = String.format(java.util.Locale.US, "%,.2f", amount);
            items.add(new ScreenFactory.ScreenItem(
                    Material.PLAYER_HEAD,
                    "&e#" + position + " &f" + entry.getName(),
                    List.of(colorize("&7Balance&8: &#00d4ff" + formatted)),
                    null));
        }

        if (items.isEmpty()) {
            items.add(new ScreenFactory.ScreenItem(Material.BARRIER, "&7No data yet.", List.of(), null));
        }
        this.factory.open(player, "&#00d4ff&lBaltop", Math.max(3, Math.min(6, rows)), items);
    }

    private String colorize(String text) {
        return dev.yanianz.essentials.util.ColorUtil.sections(text);
    }
}
