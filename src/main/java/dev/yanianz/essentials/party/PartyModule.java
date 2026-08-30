package dev.yanianz.essentials.party;

import fr.maxlego08.essentials.ZEssentialsPlugin;
import fr.maxlego08.essentials.module.ZModule;
import org.bukkit.configuration.file.YamlConfiguration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class PartyModule extends ZModule {

    private boolean enabled = true;
    private int maxSize = 8;
    private String chatFormat = "&d[&5PARTY&d] &f%player%&8: &7%message%";

    private final Map<Integer, Party> parties = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> playerParties = new ConcurrentHashMap<>();
    private final AtomicInteger idGen = new AtomicInteger(1);

    public record Party(int id, UUID leader, Map<UUID, Boolean> members, long createdAt) {}

    public PartyModule(ZEssentialsPlugin plugin) {
        super(plugin, "party");
    }

    @Override
    public void loadConfiguration() {
        super.loadConfiguration();
        YamlConfiguration config = getConfiguration();
        this.enabled = config.getBoolean("enable", true);
        this.maxSize = Math.max(2, config.getInt("max-size", 8));
        this.chatFormat = config.getString("chat-format", "&d[&5PARTY&d] &f%player%&8: &7%message%");
    }

    public int createParty(UUID leader) {
        if (playerParties.containsKey(leader)) return -1;
        int id = idGen.getAndIncrement();
        Map<UUID, Boolean> members = new HashMap<>();
        members.put(leader, true);
        Party party = new Party(id, leader, members, System.currentTimeMillis());
        parties.put(id, party);
        playerParties.put(leader, id);
        return id;
    }

    public boolean disbandParty(int id) {
        Party p = parties.remove(id);
        if (p == null) return false;
        p.members().keySet().forEach(playerParties::remove);
        return true;
    }

    public boolean invitePlayer(int id, UUID player) {
        Party p = parties.get(id);
        if (p == null || playerParties.containsKey(player)) return false;
        if (p.members().size() >= maxSize) return false;
        parties.get(id).members().put(player, true);
        playerParties.put(player, id);
        return true;
    }

    public boolean leaveParty(int id, UUID player) {
        Party p = parties.get(id);
        if (p == null || !p.members().containsKey(player)) return false;
        parties.get(id).members().remove(player);
        playerParties.remove(player);
        if (p.leader().equals(player)) {
            if (parties.get(id).members().isEmpty()) {
                disbandParty(id);
            } else {
                UUID newLeader = parties.get(id).members().keySet().iterator().next();
                parties.put(id, new Party(id, newLeader, parties.get(id).members(), parties.get(id).createdAt()));
            }
        }
        return true;
    }

    public int getPlayerPartyId(UUID player) {
        return playerParties.getOrDefault(player, -1);
    }

    public Party getParty(int id) { return parties.get(id); }
    public boolean isEnabled() { return enabled && isEnable; }
    public String getChatFormat() { return chatFormat; }
}