package fr.maxlego08.essentials.loader;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.buttons.enderchest.ButtonEnderChestContent;
import fr.maxlego08.menu.api.button.Button;
import fr.maxlego08.menu.api.button.DefaultButtonValue;
import fr.maxlego08.menu.api.loader.ButtonLoader;
import org.bukkit.configuration.file.YamlConfiguration;

public class ButtonEnderChestContentLoader extends ButtonLoader {

    private final EssentialsPlugin plugin;

    public ButtonEnderChestContentLoader(EssentialsPlugin plugin) {
        super(plugin, "ZESSENTIALS_ENDERCHEST_CONTENT");
        this.plugin = plugin;
    }

    @Override
    public Button load(YamlConfiguration yamlConfiguration, String path, DefaultButtonValue defaultButtonValue) {
        return new ButtonEnderChestContent(this.plugin);
    }
}
