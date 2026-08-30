package fr.maxlego08.essentials.module.modules;

import fr.maxlego08.essentials.ZEssentialsPlugin;
import fr.maxlego08.essentials.module.ZModule;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public class CraftingModule extends ZModule {

    private boolean enabled = true;
    private String title = "&8&lCrafting Table";
    private String quickCraftMaterial = "ANVIL";
    private String quickCraftText = "&6&lQuick Craft";
    private java.util.List<String> quickCraftLore = java.util.List.of();
    private String quickCraftPermission = "essentials.crafting.quickcraft";
    private String closeMaterial = "BARRIER";
    private String closeText = "&cClose";
    private String fillerMaterial = "GRAY_STAINED_GLASS_PANE";
    private String fillerColor = "&8";

    public CraftingModule(ZEssentialsPlugin plugin) {
        super(plugin, "crafting");
    }

    @Override
    public void loadConfiguration() {
        super.loadConfiguration();

        YamlConfiguration config = getConfiguration();
        this.enabled = config.getBoolean("enable", true);
        this.title = config.getString("title", "&8&lCrafting Table");
        this.fillerMaterial = config.getString("filler-material", "GRAY_STAINED_GLASS_PANE");
        this.fillerColor = config.getString("filler-color", "&8");
        this.closeMaterial = config.getString("close.material", "BARRIER");
        this.closeText = config.getString("close.text", "&cClose");

        ConfigurationSection qc = config.getConfigurationSection("quick-craft");
        if (qc != null) {
            this.quickCraftMaterial = qc.getString("material", "ANVIL");
            this.quickCraftText = qc.getString("text", "&6&lQuick Craft");
            this.quickCraftLore = qc.getStringList("lore");
            this.quickCraftPermission = qc.getString("permission", "essentials.crafting.quickcraft");
        }
    }

    public void openCrafting(org.bukkit.entity.Player player) {
        if (!this.enabled || !this.isEnable) return;
        dev.yanianz.essentials.crafting.CraftingGui.open(this.plugin, player, this);
    }

    public boolean isEnabled() { return this.enabled && this.isEnable; }
    public String getTitle() { return title; }
    public String getQuickCraftMaterial() { return quickCraftMaterial; }
    public String getQuickCraftText() { return quickCraftText; }
    public java.util.List<String> getQuickCraftLore() { return quickCraftLore; }
    public String getQuickCraftPermission() { return quickCraftPermission; }
    public String getCloseMaterial() { return closeMaterial; }
    public String getCloseText() { return closeText; }
    public String getFillerMaterial() { return fillerMaterial; }
    public String getFillerColor() { return fillerColor; }
}
