package fr.maxlego08.essentials.loader;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.buttons.craft.ButtonCraft;
import fr.maxlego08.menu.api.button.Button;
import fr.maxlego08.menu.api.button.DefaultButtonValue;
import fr.maxlego08.menu.api.loader.ButtonLoader;
import org.bukkit.configuration.file.YamlConfiguration;

public class ButtonCraftLoader extends ButtonLoader {

    private final EssentialsPlugin plugin;

    public ButtonCraftLoader(EssentialsPlugin plugin) {
        super(plugin, "ZESSENTIALS_CRAFT_INTERACTIVE");
        this.plugin = plugin;
    }

    @Override
    public Button load(YamlConfiguration yamlConfiguration, String path, DefaultButtonValue defaultButtonValue) {
        return new ButtonCraft(this.plugin);
    }
}
