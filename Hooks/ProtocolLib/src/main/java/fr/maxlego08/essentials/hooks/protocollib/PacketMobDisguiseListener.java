package fr.maxlego08.essentials.hooks.protocollib;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.modules.Module;
import fr.maxlego08.essentials.api.modules.ModuleManager;
import fr.maxlego08.essentials.api.packet.PacketRegister;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.UUID;

public class PacketMobDisguiseListener extends PacketAdapter implements PacketRegister {

    private final EssentialsPlugin plugin;
    private final Class<? extends Module> nicknamesModuleClass;

    public PacketMobDisguiseListener(EssentialsPlugin plugin) {
        super(PacketAdapter.params()
                .plugin(plugin)
                .listenerPriority(ListenerPriority.HIGH)
                .types(
                        PacketType.Play.Server.SPAWN_ENTITY,
                        PacketType.Play.Server.ENTITY_METADATA
                ));
        this.plugin = plugin;
        this.nicknamesModuleClass = loadNicknamesModuleClass();
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends Module> loadNicknamesModuleClass() {
        try {
            return (Class<? extends Module>) Class.forName("dev.yanianz.essentials.nicknames.NicknamesModule");
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    @Override
    public void addPacketListener() {
        ProtocolLibrary.getProtocolManager().addPacketListener(this);
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        if (this.nicknamesModuleClass == null) return;

        ModuleManager manager = this.plugin.getModuleManager();
        Module module = manager.getModule(this.nicknamesModuleClass);
        if (module == null) return;

        boolean disguiseEnabled;
        try {
            disguiseEnabled = (boolean) module.getClass().getMethod("isDisguiseEnabled").invoke(module);
        } catch (Exception e) {
            return;
        }
        if (!disguiseEnabled) return;

        if (event.getPacketType() == PacketType.Play.Server.SPAWN_ENTITY) {
            handleSpawnEntity(event, module);
        } else if (event.getPacketType() == PacketType.Play.Server.ENTITY_METADATA) {
            handleEntityMetadata(event, module);
        }
    }

    private Object getDisguiseData(Module module, UUID uuid) {
        try {
            return module.getClass().getMethod("getDisguise", UUID.class).invoke(module, uuid);
        } catch (Exception e) {
            return null;
        }
    }

    private String getEntityType(Object disguiseData) {
        try {
            return (String) disguiseData.getClass().getMethod("getEntityType").invoke(disguiseData);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isSelfView(Module module) {
        try {
            return (boolean) module.getClass().getMethod("isSelfView").invoke(module);
        } catch (Exception e) {
            return false;
        }
    }

    private void handleSpawnEntity(PacketEvent event, Module module) {
        try {
            UUID spawnUuid = event.getPacket().getUUIDs().read(0);
            if (spawnUuid == null) return;

            Object disguiseData = getDisguiseData(module, spawnUuid);
            if (disguiseData == null) return;

            String entityTypeStr = getEntityType(disguiseData);
            if (entityTypeStr == null) return;

            if (!isSelfView(module) && event.getPlayer().getUniqueId().equals(spawnUuid)) return;

            EntityType mobType;
            try {
                mobType = EntityType.valueOf(entityTypeStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                return;
            }

            int mobTypeId = mobType.getTypeId();
            if (mobTypeId < 0) return;

            event.getPacket().getIntegers().write(1, mobTypeId);
        } catch (Exception ignored) {
        }
    }

    private void handleEntityMetadata(PacketEvent event, Module module) {
        int entityId = event.getPacket().getIntegers().read(0);
        Player targetPlayer = lookupPlayerByEntityId(entityId);
        if (targetPlayer == null) return;

        Object disguiseData = getDisguiseData(module, targetPlayer.getUniqueId());
        if (disguiseData == null) return;

        String entityTypeStr = getEntityType(disguiseData);
        if (entityTypeStr == null) return;

        if (!isSelfView(module) && event.getPlayer().getUniqueId().equals(targetPlayer.getUniqueId())) return;

        try {
            WrappedDataWatcher watcher = event.getPacket().getDataWatcherModifier().read(0);
            if (watcher == null) watcher = new WrappedDataWatcher();

            if (watcher.hasIndex(0)) watcher.setObject(0, Byte.valueOf((byte) 0));
            if (watcher.hasIndex(2)) watcher.setObject(2, "");
            if (watcher.hasIndex(3)) watcher.setObject(3, Boolean.FALSE);
            if (watcher.hasIndex(6)) watcher.setObject(6, 20.0f);
            if (watcher.hasIndex(8)) watcher.setObject(8, Integer.valueOf(0));
            if (watcher.hasIndex(9)) watcher.setObject(9, Float.valueOf(0.0f));

            event.getPacket().getDataWatcherModifier().write(0, watcher);
        } catch (Exception ignored) {
        }
    }

    private Player lookupPlayerByEntityId(int entityId) {
        for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
            if (player.getEntityId() == entityId) return player;
        }
        return null;
    }
}
