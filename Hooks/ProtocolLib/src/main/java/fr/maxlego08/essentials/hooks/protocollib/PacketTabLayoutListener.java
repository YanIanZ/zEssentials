package fr.maxlego08.essentials.hooks.protocollib;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedRemoteChatSessionData;
import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.packet.PacketRegister;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Intercepts outgoing PLAYER_INFO packets to inject fake player entries
 * for TAB slot layouts. When the tablist layout module is enabled, this
 * listener adds configured fake entries (slot padding, header text, etc.)
 * to the player info list so the tab list displays a custom layout.
 */
public class PacketTabLayoutListener extends PacketAdapter implements PacketRegister {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private final EssentialsPlugin plugin;

    public PacketTabLayoutListener(EssentialsPlugin plugin) {
        super(PacketAdapter.params().plugin(plugin).listenerPriority(ListenerPriority.HIGHEST)
                .types(PacketType.Play.Server.PLAYER_INFO));
        this.plugin = plugin;
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.PLAYER_INFO) {
            handlePlayerInfo(event);
        }
    }

    @SuppressWarnings("unchecked")
    private void handlePlayerInfo(PacketEvent event) {
        var packet = event.getPacket();
        var infoDataList = packet.getPlayerInfoDataLists().readSafely(1);
        if (infoDataList == null) {
            infoDataList = packet.getPlayerInfoDataLists().readSafely(0);
            if (infoDataList == null) return;
        }

        Object tablistModule;
        try {
            @SuppressWarnings("unchecked")
            Class<? extends fr.maxlego08.essentials.api.modules.Module> moduleClass =
                    (Class<? extends fr.maxlego08.essentials.api.modules.Module>)
                            Class.forName("dev.yanianz.essentials.tablist.TabListModule");
            tablistModule = this.plugin.getModuleManager().getModule(moduleClass);
        } catch (ClassNotFoundException e) {
            return;
        }
        if (tablistModule == null) return;

        boolean layoutEnabled;
        try {
            layoutEnabled = (boolean) tablistModule.getClass().getMethod("isLayoutEnabled").invoke(tablistModule);
        } catch (Exception e) {
            return;
        }
        if (!layoutEnabled) return;

        List<String[]> fakeEntries;
        try {
            fakeEntries = (List<String[]>) tablistModule.getClass()
                    .getMethod("getLayoutFakeEntries").invoke(tablistModule);
        } catch (Exception e) {
            return;
        }
        if (fakeEntries == null || fakeEntries.isEmpty()) return;

        List<PlayerInfoData> newData = new ArrayList<>(infoDataList);

        for (String[] fakeEntry : fakeEntries) {
            UUID fakeUuid = UUID.nameUUIDFromBytes((fakeEntry[0] + event.getPlayer().getUniqueId()).getBytes());
            boolean exists = newData.stream().anyMatch(d -> d.getProfileId().equals(fakeUuid));
            if (!exists) {
                WrappedGameProfile profile = new WrappedGameProfile(fakeUuid, fakeEntry[0]);
                WrappedChatComponent displayName = WrappedChatComponent.fromJson(
                        net.kyori.adventure.text.serializer.gson.GsonComponentSerializer.gson().serialize(
                                LEGACY.deserialize(fakeEntry[1])));
                newData.add(new PlayerInfoData(
                        fakeUuid, 0, true,
                        EnumWrappers.NativeGameMode.SURVIVAL,
                        profile, displayName, (WrappedRemoteChatSessionData) null));
            }
        }

        packet.getPlayerInfoDataLists().write(1, newData);
    }

    @Override
    public void addPacketListener() {
        ProtocolLibrary.getProtocolManager().addPacketListener(this);
    }
}
