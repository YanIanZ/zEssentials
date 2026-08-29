package fr.maxlego08.essentials.module.modules;

import dev.yanianz.essentials.screens.EssentialsScreens;
import dev.yanianz.essentials.screens.ScreenFactory;
import fr.maxlego08.essentials.ZEssentialsPlugin;
import fr.maxlego08.essentials.api.messages.Message;
import fr.maxlego08.essentials.api.utils.RuleType;
import fr.maxlego08.essentials.module.ZModule;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class RuleModule extends ZModule {

    private RuleType ruleType = RuleType.MESSAGE;

    public RuleModule(ZEssentialsPlugin plugin) {
        super(plugin, "rules");
    }

    @Override
    public void loadConfiguration() {
        super.loadConfiguration();

        this.ruleType = RuleType.valueOf(getConfiguration().getString("rule-type", "MESSAGE").toUpperCase());

        this.loadInventory("rules");
    }

    public void sendRule(Player player) {
        if (this.ruleType == RuleType.INVENTORY) {
            this.plugin.getInventoryManager().openInventory(player, this.plugin, "rules");
        } else if (this.ruleType == RuleType.SCREEN) {
            sendRuleScreen(player);
        } else {
            message(player, Message.RULES);
        }
    }

    /**
     * Paginated screen listing every rule line of {@link Message#RULES} as a
     * single book item.
     */
    private void sendRuleScreen(Player player) {

        List<String> lines = Message.RULES.getMessageAsStringList();
        List<ScreenFactory.ScreenItem> items = new ArrayList<>();

        for (String line : lines) {
            items.add(new ScreenFactory.ScreenItem(
                    Material.BOOK,
                    color(line),
                    List.of(),
                    null));
        }

        if (items.isEmpty()) {
            items.add(new ScreenFactory.ScreenItem(
                    Material.BARRIER, color("&7No rules configured."), List.of(), null));
        }

        EssentialsScreens.get().factory().open(player, "&6&lServer Rules", 6, items);
    }
}
