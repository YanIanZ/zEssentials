package dev.yanianz.essentials.customscreens;

import fr.maxlego08.essentials.ZEssentialsPlugin;
import fr.maxlego08.essentials.api.configuration.NonLoadable;
import fr.maxlego08.essentials.api.messages.Message;
import fr.maxlego08.essentials.commands.ZCommandManager;
import fr.maxlego08.essentials.module.ZModule;
import fr.maxlego08.menu.api.Inventory;
import fr.maxlego08.menu.api.InventoryManager;
import fr.maxlego08.menu.api.exceptions.InventoryException;
import fr.maxlego08.menu.api.sound.SoundOption;
import fr.maxlego08.menu.hooks.xseries.XSound;
import fr.maxlego08.menu.sound.ZSoundOption;
import org.bukkit.GameMode;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Allows the server owner to create his own screens (inventories) without any other plugin.
 * Each entry of modules/customscreens/config.yml registers a command that opens the
 * zMenu inventory stored in modules/customscreens/screens/{@code <name>}.yml.
 */
public class CustomScreenModule extends ZModule {

    @NonLoadable
    private final List<ZCustomScreen> registeredCommands = new ArrayList<>();

    public CustomScreenModule(ZEssentialsPlugin plugin) {
        super(plugin, "customscreens");
    }

    @Override
    public void loadConfiguration() {
        super.loadConfiguration();

        ZCommandManager commandManager = (ZCommandManager) this.plugin.getCommandManager();

        // Commands of the previous load must be removed, /ezreload must not duplicate them
        this.registeredCommands.forEach(commandManager::unregisterCommand);
        this.registeredCommands.clear();

        if (!this.isEnable) return;

        ConfigurationSection screensSection = getConfiguration().getConfigurationSection("screens");
        if (screensSection == null) return;

        for (String name : screensSection.getKeys(false)) {
            ConfigurationSection section = screensSection.getConfigurationSection(name);
            if (section == null) continue;
            loadCustomScreen(commandManager, name, section);
        }

        commandManager.refreshPlayerCommands();
    }

    public List<ZCustomScreen> getRegisteredCommands() {
        return this.registeredCommands;
    }

    /**
     * Opens the screen for the player after checking its world/gamemode conditions.
     *
     * @param player the player who will see the screen
     * @param screen the screen to open
     */
    public void openScreen(Player player, CustomScreen screen) {

        if (!screen.canOpen(player.getWorld().getName(), player.getGameMode())) {
            message(player, Message.MESSAGE_SCREEN_CONDITION);
            return;
        }

        InventoryManager inventoryManager = this.plugin.getInventoryManager();
        Optional<Inventory> optionalInventory = inventoryManager.getInventory(this.plugin, screen.name());
        if (optionalInventory.isEmpty()) {
            message(player, Message.COMMAND_SCREEN_NOT_FOUND, "%screen%", screen.name());
            return;
        }

        inventoryManager.openInventory(player, optionalInventory.get());

        SoundOption openSound = screen.openSound();
        if (openSound != null) {
            openSound.play(player);
        }
    }

    private void loadCustomScreen(ZCommandManager commandManager, String name, ConfigurationSection section) {

        String path = "screens." + name;

        File file = new File(getFolder(), "screens/" + name + ".yml");
        if (!file.exists()) {
            // Extract the bundled layout when we ship one, custom entries have to be created manually
            String resourcePath = "modules/" + getName() + "/screens/" + name + ".yml";
            if (this.plugin.resourceExist(resourcePath)) {
                this.plugin.saveResource(resourcePath, false);
            }
        }
        if (!file.exists()) {
            this.plugin.getLogger().severe("The screen " + path + " has no layout file (" + file.getPath() + "), it will be ignored.");
            return;
        }

        try {
            this.plugin.getInventoryManager().loadInventory(this.plugin, file);
        } catch (InventoryException exception) {
            this.plugin.getLogger().severe("Impossible to load the screen " + path + ": " + exception.getMessage());
            exception.printStackTrace();
            return;
        }

        String command = sanitizeCommand(section.getString("command"));
        if (command == null) {
            this.plugin.getLogger().severe("The screen " + path + " has no valid 'command', it will be ignored.");
            return;
        }

        if (commandManager.isEssentialsCommand(command)) {
            this.plugin.getLogger().severe("The screen command /" + command + " is already a zEssentials command, it will be ignored.");
            return;
        }

        List<String> aliases = new ArrayList<>();
        for (String alias : section.getStringList("aliases")) {
            String sanitizedAlias = sanitizeCommand(alias);
            if (sanitizedAlias == null || sanitizedAlias.equals(command) || aliases.contains(sanitizedAlias)) continue;
            if (commandManager.isEssentialsCommand(sanitizedAlias)) {
                this.plugin.getLogger().severe("The alias /" + sanitizedAlias + " of the screen " + path + " is already a zEssentials command, it will be ignored.");
                continue;
            }
            aliases.add(sanitizedAlias);
        }

        List<String> worlds = section.getStringList("worlds");
        List<GameMode> gameModes = new ArrayList<>();
        for (String gamemode : section.getStringList("gamemodes")) {
            try {
                gameModes.add(GameMode.valueOf(gamemode.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException exception) {
                this.plugin.getLogger().severe("The gamemode " + gamemode + " of the screen " + path + " doesn't exist, it will be ignored.");
            }
        }

        CustomScreen customScreen = new CustomScreen(
                name,
                command,
                aliases,
                section.getString("permission"),
                section.getString("description"),
                worlds,
                gameModes,
                loadSoundOption(section)
        );

        ZCustomScreen zCustomScreen = new ZCustomScreen(this.plugin, this, customScreen);
        this.registeredCommands.add(zCustomScreen);

        // commands.yml is indexed by class name, every custom screen shares the same class, so the
        // configuration file must not be used here
        commandManager.registerCommand(this.plugin, command, zCustomScreen, aliases, false);
    }

    /**
     * Removes the leading slash and the case of a command name.
     *
     * @param command The command name written in the configuration
     * @return The command name, or null if it cannot be used
     */
    private String sanitizeCommand(String command) {
        if (command == null) return null;

        String result = command.toLowerCase(Locale.ROOT).trim();
        while (result.startsWith("/")) {
            result = result.substring(1).trim();
        }

        if (result.isEmpty() || result.contains(" ")) return null;
        return result;
    }

    private SoundOption loadSoundOption(ConfigurationSection section) {
        ConfigurationSection soundSection = section.getConfigurationSection("open-sound");
        if (soundSection == null || !soundSection.getBoolean("enabled", false)) return null;

        String soundName = soundSection.getString("sound", "");
        if (soundName == null || soundName.isEmpty()) return null;

        float volume = (float) soundSection.getDouble("volume", 1.0);
        float pitch = (float) soundSection.getDouble("pitch", 1.0);
        Optional<XSound> xSound = XSound.of(soundName);

        return new ZSoundOption(xSound.orElse(null), "MASTER", soundName, pitch, volume, xSound.isEmpty());
    }
}
