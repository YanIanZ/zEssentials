package fr.maxlego08.essentials.hooks.protocollib;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.modules.Module;
import fr.maxlego08.essentials.api.packet.PacketRegister;
import org.bukkit.entity.Player;

public class PacketCraftingListener extends PacketAdapter implements PacketRegister {

    private final EssentialsPlugin plugin;
    private final Class<? extends Module> craftingModuleClass;

    public PacketCraftingListener(EssentialsPlugin plugin) {
        super(PacketAdapter.params()
                .plugin(plugin)
                .listenerPriority(ListenerPriority.HIGHEST)
                .types(PacketType.Play.Server.OPEN_WINDOW));
        this.plugin = plugin;
        this.craftingModuleClass = loadCraftingModuleClass();
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends Module> loadCraftingModuleClass() {
        try {
            return (Class<? extends Module>) Class.forName("fr.maxlego08.essentials.module.modules.CraftingModule");
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
        if (this.craftingModuleClass == null) return;
        try {
            var packet = event.getPacket();
            int windowType = packet.getIntegers().read(0);
            if (windowType != 2) return;

            var module = plugin.getModuleManager().getModule(this.craftingModuleClass);
            if (module == null) return;

            var isEnabledMethod = this.craftingModuleClass.getMethod("isEnabled");
            boolean enabled = (boolean) isEnabledMethod.invoke(module);
            if (!enabled) return;

            event.setCancelled(true);

            Player player = event.getPlayer();
            var openMethod = this.craftingModuleClass.getMethod("openCrafting", Player.class);
            plugin.getScheduler().runAtEntity(player, w -> {
                try {
                    openMethod.invoke(module, player);
                } catch (Exception ignored) {
                }
            });

            var zEssentialsPluginClass = Class.forName("fr.maxlego08.essentials.ZEssentialsPlugin");
            var listenerClass = Class.forName("dev.yanianz.essentials.crafting.CraftingListener");
            var ensureMethod = listenerClass.getMethod("ensureRegistered", zEssentialsPluginClass);
            ensureMethod.invoke(null, plugin);
        } catch (Exception ignored) {
        }
    }
}
