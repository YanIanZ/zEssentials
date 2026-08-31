package fr.maxlego08.essentials.loader;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.buttons.enderchest.ButtonEnderChestOverview;
import fr.maxlego08.menu.api.button.Button;
import fr.maxlego08.menu.api.button.DefaultButtonValue;
import fr.maxlego08.menu.api.loader.ButtonLoader;
import org.bukkit.configuration.file.YamlConfiguration;

public class ButtonEnderChestOverviewLoader extends ButtonLoader {

    private final EssentialsPlugin plugin;

    public ButtonEnderChestOverviewLoader(EssentialsPlugin plugin) {
        super(plugin, "ZESSENTIALS_ENDERCHEST_OVERVIEW");
        this.plugin = plugin;
    }

    @Override
    public Button load(YamlConfiguration yamlConfiguration, String path, DefaultButtonValue defaultButtonValue) {
        return new ButtonEnderChestOverview(this.plugin);
    }
}
