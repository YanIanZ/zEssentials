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
                        PacketType.Play.Server.ENTITY_METADATA,
                        PacketType.Play.Server.ENTITY_DESTROY,
                        PacketType.Play.Server.NAMED_ENTITY_SPAWN,
                        PacketType.Play.Server.REL_ENTITY_MOVE,
                        PacketType.Play.Server.REL_ENTITY_MOVE_LOOK,
                        PacketType.Play.Server.ENTITY_LOOK,
                        PacketType.Play.Server.ENTITY_TELEPORT
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

        if (event.getPacketType() == PacketType.Play.Server.NAMED_ENTITY_SPAWN) {
            handleNamedEntitySpawn(event, module);
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

    private void handleNamedEntitySpawn(PacketEvent event, Module module) {
        UUID spawnUuid = event.getPacket().getUUIDs().read(0);
        if (spawnUuid == null) return;

        Object disguiseData = getDisguiseData(module, spawnUuid);
        if (disguiseData == null) return;

        String entityType = getEntityType(disguiseData);
        if (entityType == null) return;

        if (!isSelfView(module) && event.getPlayer().getUniqueId().equals(spawnUuid)) return;

        int entityId = event.getPacket().getIntegers().read(0);
        event.setCancelled(true);

        spawnMobEntity(event, entityId, spawnUuid, entityType);
    }

    private void handleEntityMetadata(PacketEvent event, Module module) {
        int entityId = event.getPacket().getIntegers().read(0);
        Player targetPlayer = lookupPlayerByEntityId(entityId);
        if (targetPlayer == null) return;

        Object disguiseData = getDisguiseData(module, targetPlayer.getUniqueId());
        if (disguiseData == null) return;

        String entityType = getEntityType(disguiseData);
        if (entityType == null) return;

        if (!isSelfView(module) && event.getPlayer().getUniqueId().equals(targetPlayer.getUniqueId())) return;

        EntityType type;
        try {
            type = EntityType.valueOf(entityType.toUpperCase());
        } catch (IllegalArgumentException e) {
            return;
        }

        WrappedDataWatcher watcher = new WrappedDataWatcher();
        watcher.setObject(0, Byte.valueOf((byte) 0));
        watcher.setObject(1, Short.valueOf((short) 300));
        watcher.setObject(2, "");
        watcher.setObject(3, Boolean.FALSE);
        watcher.setObject(4, Byte.valueOf((byte) 0));
        watcher.setObject(5, Boolean.FALSE);
        watcher.setObject(6, 20.0f);
        watcher.setObject(7, Integer.valueOf(0));
        watcher.setObject(8, Integer.valueOf(0));
        watcher.setObject(9, Float.valueOf(0.0f));
        watcher.setObject(10, Integer.valueOf(0));

        event.getPacket().getDataWatcherModifier().write(0, watcher);
    }

    private void spawnMobEntity(PacketEvent event, int entityId, UUID uuid, String entityTypeStr) {
        EntityType type;
        try {
            type = EntityType.valueOf(entityTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return;
        }

        var spawnPacket = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.SPAWN_ENTITY);

        Player target = lookupPlayerByEntityId(entityId);
        if (target == null) return;

        spawnPacket.getIntegers().write(0, entityId);
        spawnPacket.getUUIDs().write(0, uuid);
        spawnPacket.getDoubles().write(0, target.getLocation().getX());
        spawnPacket.getDoubles().write(1, target.getLocation().getY());
        spawnPacket.getDoubles().write(2, target.getLocation().getZ());

        int typeId = type.getTypeId();
        spawnPacket.getIntegers().write(1, typeId);

        spawnPacket.getIntegers().write(2, 0);

        try {
            ProtocolLibrary.getProtocolManager().sendServerPacket(event.getPlayer(), spawnPacket);
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
