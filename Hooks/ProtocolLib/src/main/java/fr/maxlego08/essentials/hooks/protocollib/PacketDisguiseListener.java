package fr.maxlego08.essentials.hooks.protocollib;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedSignedProperty;
import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.modules.Module;
import fr.maxlego08.essentials.api.modules.ModuleManager;
import fr.maxlego08.essentials.api.packet.PacketRegister;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PacketDisguiseListener extends PacketAdapter implements PacketRegister {

    private final EssentialsPlugin plugin;
    private final Class<? extends Module> nicknamesModuleClass;

    public PacketDisguiseListener(EssentialsPlugin plugin) {
        super(PacketAdapter.params()
                .plugin(plugin)
                .listenerPriority(ListenerPriority.HIGH)
                .types(PacketType.Play.Server.PLAYER_INFO));
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
        if (event.getPacketType() != PacketType.Play.Server.PLAYER_INFO) return;

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

        boolean selfView;
        try {
            selfView = (boolean) module.getClass().getMethod("isSelfView").invoke(module);
        } catch (Exception e) {
            selfView = false;
        }

        var packet = event.getPacket();
        var infoDataList = packet.getPlayerInfoDataLists().readSafely(1);
        if (infoDataList == null) {
            infoDataList = packet.getPlayerInfoDataLists().readSafely(0);
            if (infoDataList == null) return;
        }

        boolean modified = false;
        List<PlayerInfoData> newData = new ArrayList<>(infoDataList);

        for (int i = 0; i < newData.size(); i++) {
            PlayerInfoData infoData = newData.get(i);
            UUID profileId = infoData.getProfileId();

            Object disguiseData;
            try {
                disguiseData = module.getClass().getMethod("getDisguise", UUID.class).invoke(module, profileId);
            } catch (Exception e) {
                continue;
            }
            if (disguiseData == null) continue;

            if (!selfView && event.getPlayer().getUniqueId().equals(profileId)) continue;

            String disguiseName;
            String textureValue;
            String textureSignature;
            try {
                disguiseName = (String) disguiseData.getClass().getMethod("getDisguiseName").invoke(disguiseData);
                textureValue = (String) disguiseData.getClass().getMethod("getTextureValue").invoke(disguiseData);
                textureSignature = (String) disguiseData.getClass().getMethod("getTextureSignature").invoke(disguiseData);
            } catch (Exception e) {
                continue;
            }

            WrappedGameProfile originalProfile = infoData.getProfile();
            String effectiveName = disguiseName != null ? disguiseName : originalProfile.getName();

            WrappedGameProfile newProfile = new WrappedGameProfile(profileId, effectiveName);

            if (textureValue != null) {
                if (textureSignature != null && !textureSignature.isEmpty()) {
                    newProfile.getProperties().put("textures", new WrappedSignedProperty("textures", textureValue, textureSignature));
                } else {
                    newProfile.getProperties().put("textures", new WrappedSignedProperty("textures", textureValue, null));
                }
            } else {
                newProfile.getProperties().putAll(originalProfile.getProperties());
            }

            PlayerInfoData newInfoData = new PlayerInfoData(
                    profileId,
                    infoData.getLatency(),
                    infoData.isListed(),
                    infoData.getGameMode(),
                    newProfile,
                    infoData.getDisplayName(),
                    infoData.getRemoteChatSessionData()
            );

            newData.set(i, newInfoData);
            modified = true;
        }

        if (modified) {
            packet.getPlayerInfoDataLists().write(1, newData);
        }
    }
}
