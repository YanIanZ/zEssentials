package fr.maxlego08.essentials.hooks.protocollib;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.modules.Module;
import fr.maxlego08.essentials.api.modules.ModuleManager;
import fr.maxlego08.essentials.api.packet.PacketRegister;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * LibsDisguises-style mob disguise engine. Rewrites spawn packets,
 * replaces entity metadata with mob-appropriate watcher values,
 * suppresses player equipment on mobs and optionally removes the
 * disguised player from the tab list.
 */
public class PacketMobDisguiseListener extends PacketAdapter implements PacketRegister {

    private final EssentialsPlugin plugin;
    private final Class<? extends Module> nicknamesModuleClass;

    public PacketMobDisguiseListener(EssentialsPlugin plugin) {
        super(PacketAdapter.params()
                .plugin(plugin)
                .listenerPriority(ListenerPriority.HIGH)
                .types(
                        PacketType.Play.Server.SPAWN_ENTITY,
                        PacketType.Play.Server.ENTITY_METADATA,
                        PacketType.Play.Server.ENTITY_EQUIPMENT,
                        PacketType.Play.Server.PLAYER_INFO
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

    private record ModuleState(boolean enabled, boolean selfView, boolean hideFromTab) {}

    private ModuleState state(Module module) {
        try {
            boolean enabled = (boolean) module.getClass().getMethod("isDisguiseEnabled").invoke(module);
            boolean selfView = (boolean) module.getClass().getMethod("isSelfView").invoke(module);
            boolean hideFromTab = (boolean) module.getClass().getMethod("isHideFromTab").invoke(module);
            return new ModuleState(enabled, selfView, hideFromTab);
        } catch (Exception e) {
            return new ModuleState(false, false, false);
        }
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        if (this.nicknamesModuleClass == null) return;
        ModuleManager manager = this.plugin.getModuleManager();
        Module module = manager.getModule(this.nicknamesModuleClass);
        if (module == null) return;

        ModuleState state = state(module);
        if (!state.enabled()) return;

        if (event.getPacketType() == PacketType.Play.Server.SPAWN_ENTITY) {
            handleSpawn(event, module, state);
        } else if (event.getPacketType() == PacketType.Play.Server.ENTITY_METADATA) {
            handleMetadata(event, module, state);
        } else if (event.getPacketType() == PacketType.Play.Server.ENTITY_EQUIPMENT) {
            handleEquipment(event, module, state);
        } else if (event.getPacketType() == PacketType.Play.Server.PLAYER_INFO) {
            handleTab(event, module, state);
        }
    }

    private Object getDisguiseData(Module module, UUID uuid) {
        try {
            return module.getClass().getMethod("getDisguise", UUID.class).invoke(module, uuid);
        } catch (Exception e) {
            return null;
        }
    }

    private EntityType mobType(Object disguiseData) {
        try {
            String entityType = (String) disguiseData.getClass().getMethod("getEntityType").invoke(disguiseData);
            if (entityType == null) return null;
            return EntityType.valueOf(entityType.toUpperCase());
        } catch (Exception e) {
            return null;
        }
    }

    private String displayName(Object disguiseData) {
        try {
            return (String) disguiseData.getClass().getMethod("getDisguiseName").invoke(disguiseData);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean viewerBlocked(PacketEvent event, UUID disguisedId, ModuleState state) {
        return !state.selfView() && event.getPlayer().getUniqueId().equals(disguisedId);
    }

    private void handleSpawn(PacketEvent event, Module module, ModuleState state) {
        UUID spawnUuid = event.getPacket().getUUIDs().readSafely(0);
        if (spawnUuid == null) return;

        Object disguiseData = getDisguiseData(module, spawnUuid);
        if (disguiseData == null) return;
        if (viewerBlocked(event, spawnUuid, state)) return;

        EntityType type = mobType(disguiseData);
        if (type == null || !type.isAlive()) return;

        try {
            event.getPacket().getEntityTypeModifier().write(0, type);
        } catch (Exception e) {
            try {
                event.getPacket().getIntegers().write(1, (int) type.getTypeId());
            } catch (Exception ignored) {
            }
        }
    }

    private void handleMetadata(PacketEvent event, Module module, ModuleState state) {
        int entityId = event.getPacket().getIntegers().read(0);
        Player target = lookupPlayerByEntityId(entityId);
        if (target == null) return;

        Object disguiseData = getDisguiseData(module, target.getUniqueId());
        if (disguiseData == null) return;
        if (viewerBlocked(event, target.getUniqueId(), state)) return;

        EntityType type = mobType(disguiseData);
        if (type == null || !type.isAlive()) return;

        WrappedDataWatcher watcher = new WrappedDataWatcher();
        try {
            watcher.setObject(0, (byte) 0);
        } catch (Exception ignored) {
        }

        String name = displayName(disguiseData);
        if (name != null && !name.isEmpty()) {
            try {
                String json = net.kyori.adventure.text.serializer.gson.GsonComponentSerializer.gson()
                        .serialize(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                                .legacySection().deserialize(colorize(name)));
                watcher.setObject(2, com.comphenix.protocol.wrappers.WrappedChatComponent.fromJson(json));
                watcher.setObject(3, true);
            } catch (Exception ignored) {
            }
        }

        try {
            watcher.setObject(6, 20.0f);
        } catch (Exception ignored) {
        }

        try {
            event.getPacket().getDataWatcherModifier().write(0, watcher);
        } catch (Exception ignored) {
        }
    }

    private void handleEquipment(PacketEvent event, Module module, ModuleState state) {
        int entityId = event.getPacket().getIntegers().read(0);
        Player target = lookupPlayerByEntityId(entityId);
        if (target == null) return;

        Object disguiseData = getDisguiseData(module, target.getUniqueId());
        if (disguiseData == null) return;
        if (viewerBlocked(event, target.getUniqueId(), state)) return;

        EntityType type = mobType(disguiseData);
        if (type == null || !type.isAlive()) return;

        event.setCancelled(true);
    }

    @SuppressWarnings("unchecked")
    private void handleTab(PacketEvent event, Module module, ModuleState state) {
        if (!state.hideFromTab()) return;

        var packet = event.getPacket();
        var infoDataList = packet.getPlayerInfoDataLists().readSafely(1);
        if (infoDataList == null) {
            infoDataList = packet.getPlayerInfoDataLists().readSafely(0);
            if (infoDataList == null) return;
        }

        List<PlayerInfoData> kept = new ArrayList<>(infoDataList);
        boolean modified = false;

        for (PlayerInfoData infoData : infoDataList) {
            UUID profileId = infoData.getProfileId();
            if (profileId == null) continue;
            if (profileId.equals(event.getPlayer().getUniqueId())) continue;

            Object disguiseData = getDisguiseData(module, profileId);
            if (disguiseData == null) continue;

            EntityType type = mobType(disguiseData);
            if (type == null || !type.isAlive()) continue;

            kept.remove(infoData);
            modified = true;
        }

        if (modified) {
            packet.getPlayerInfoDataLists().writeSafely(1, kept);
        }
    }

    private Player lookupPlayerByEntityId(int entityId) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getEntityId() == entityId) return player;
        }
        return null;
    }

    private static String colorize(String text) {
        if (text == null) return "";
        StringBuilder out = new StringBuilder();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("&#([0-9a-fA-F]{6})").matcher(text);
        while (m.find()) {
            StringBuilder hex = new StringBuilder("§x");
            for (char c : m.group(1).toCharArray()) hex.append('§').append(Character.toLowerCase(c));
            m.appendReplacement(out, java.util.regex.Matcher.quoteReplacement(hex.toString()));
        }
        m.appendTail(out);
        return out.toString().replace('&', '§');
    }
}
