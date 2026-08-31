package fr.maxlego08.essentials.hooks.protocollib;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.packet.PacketRegister;

public class PacketListener {

    public void registerPackets(EssentialsPlugin plugin) {

        this.register(new PacketChatListener(plugin, plugin.getModuleManager().getModuleConfiguration("chat").getString("command-placeholder.result")));
        this.register(new PacketTabLayoutListener(plugin));
        this.register(new PacketTooltipListener(plugin));
        this.register(new PacketCraftingListener(plugin));
        this.register(new PacketDisguiseListener(plugin));
    }

    private void register(PacketRegister packetRegister) {
        packetRegister.addPacketListener();
    }

}
