package dev.yanianz.essentials.guild;

import fr.maxlego08.essentials.ZEssentialsPlugin;
import fr.maxlego08.essentials.module.ZModule;
import org.bukkit.configuration.file.YamlConfiguration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class GuildModule extends ZModule {

    private boolean enabled = true;
    private int maxMembers = 25;
    private int maxNameLength = 16;
    private String chatFormat = "&2[&aGUILD&2] &f%player%&8: &7%message%";

    private final Map<Integer, Guild> guilds = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> playerGuilds = new ConcurrentHashMap<>();
    private final AtomicInteger idGen = new AtomicInteger(1);

    public record Guild(int id, String name, String tag, UUID leader,
                        Map<UUID, GuildRank> members, long createdAt) {}

    public GuildModule(ZEssentialsPlugin plugin) {
        super(plugin, "guild");
    }

    @Override
    public void loadConfiguration() {
        super.loadConfiguration();
        YamlConfiguration config = getConfiguration();
        this.enabled = config.getBoolean("enable", true);
        this.maxMembers = Math.max(1, config.getInt("max-members", 25));
        this.maxNameLength = Math.max(3, config.getInt("max-name-length", 16));
        this.chatFormat = config.getString("chat-format", "&2[&aGUILD&2] &f%player%&8: &7%message%");
    }

    public int createGuild(UUID leader, String name, String tag) {
        if (name == null || name.isEmpty() || name.length() > maxNameLength) return -1;
        if (playerGuilds.containsKey(leader)) return -1;
        int id = idGen.getAndIncrement();
        Map<UUID, GuildRank> members = new HashMap<>();
        members.put(leader, GuildRank.LEADER);
        Guild guild = new Guild(id, name, tag, leader, members, System.currentTimeMillis());
        guilds.put(id, guild);
        playerGuilds.put(leader, id);
        return id;
    }

    public boolean disbandGuild(int id) {
        Guild g = guilds.remove(id);
        if (g == null) return false;
        g.members().keySet().forEach(playerGuilds::remove);
        return true;
    }

    public boolean joinGuild(int id, UUID player) {
        if (playerGuilds.containsKey(player)) return false;
        Guild g = guilds.get(id);
        if (g == null || g.members().size() >= maxMembers) return false;
        guilds.get(id).members().put(player, GuildRank.MEMBER);
        playerGuilds.put(player, id);
        return true;
    }

    public boolean leaveGuild(int id, UUID player) {
        Guild g = guilds.get(id);
        if (g == null || !g.members().containsKey(player)) return false;
        if (g.leader().equals(player)) return false;
        guilds.get(id).members().remove(player);
        playerGuilds.remove(player);
        return true;
    }

    public int getPlayerGuildId(UUID player) {
        return playerGuilds.getOrDefault(player, -1);
    }

    public Guild getGuild(int id) { return guilds.get(id); }
    public Collection<Guild> getAllGuilds() { return guilds.values(); }
    public boolean isEnabled() { return enabled && isEnable; }
    public String getChatFormat() { return chatFormat; }
}