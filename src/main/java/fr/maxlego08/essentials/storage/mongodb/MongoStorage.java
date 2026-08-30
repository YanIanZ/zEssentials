package fr.maxlego08.essentials.storage.mongodb;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.discord.DiscordAction;
import fr.maxlego08.essentials.api.dto.*;
import fr.maxlego08.essentials.api.economy.Economy;
import fr.maxlego08.essentials.api.home.Home;
import fr.maxlego08.essentials.api.mailbox.MailBoxItem;
import fr.maxlego08.essentials.api.mailbox.MailMessage;
import fr.maxlego08.essentials.api.sanction.Sanction;
import fr.maxlego08.essentials.api.sanction.SanctionType;
import fr.maxlego08.essentials.api.server.MongoConfiguration;
import fr.maxlego08.essentials.api.steps.Step;
import fr.maxlego08.essentials.api.storage.IStorage;
import fr.maxlego08.essentials.api.user.Option;
import fr.maxlego08.essentials.api.user.User;
import fr.maxlego08.essentials.api.user.UserRecord;
import fr.maxlego08.essentials.api.vault.Vault;
import fr.maxlego08.essentials.user.ZUser;
import fr.maxlego08.essentials.zutils.utils.StorageHelper;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public class MongoStorage extends StorageHelper implements IStorage {

    private final MongoConnection connection;
    private final MongoRepositories repositories;
    private final Set<UUID> existingUUIDs = new HashSet<>();

    public MongoStorage(EssentialsPlugin plugin) {
        super(plugin);
        MongoConfiguration config = plugin.getConfiguration().getMongoConfiguration();
        if (config == null) {
            plugin.getLogger().severe("MongoDB configuration not found! Set storage-type to MONGO and configure mongo-configuration.");
            Bukkit.getPluginManager().disablePlugin(plugin);
            this.connection = null;
            this.repositories = null;
            return;
        }

        this.connection = new MongoConnection(config);

        if (!this.connection.isValid()) {
            plugin.getLogger().severe("Unable to connect to MongoDB!");
            Bukkit.getPluginManager().disablePlugin(plugin);
            this.repositories = null;
            return;
        }

        plugin.getLogger().info("MongoDB connection established to " + config.database());
        this.repositories = new MongoRepositories(plugin, connection.getDatabase());
    }

    @Override
    public void onEnable() {
        this.totalUser = repositories.users.totalUsers();
        repositories.cooldowns.deleteExpiredCooldowns();
        repositories.users.clearExpiredSanctions();
        this.existingUUIDs.addAll(repositories.users.selectUUIDs());
        setActiveSanctions(repositories.sanctions.getActiveBan());
    }

    @Override
    public void onDisable() {
        if (this.connection != null) this.connection.close();
    }

    @Override
    public User createOrLoad(UUID uniqueId, String playerName) {
        throw new UnsupportedOperationException("Not yet implemented for MongoDB: createOrLoad");
    }

    @Override
    public void onPlayerQuit(UUID uniqueId) {
        this.users.remove(uniqueId);
    }

    @Override
    public User getUser(UUID uniqueId) {
        return this.users.get(uniqueId);
    }

    @Override
    public void updateOption(UUID uniqueId, Option option, boolean value) {
        throw new UnsupportedOperationException("Not yet implemented for MongoDB: updateOption");
    }

    @Override
    public void updateCooldown(UUID uniqueId, String key, long expiredAt) {
        async(() -> repositories.cooldowns.upsert(uniqueId, key, expiredAt));
    }

    @Override
    public void updateEconomy(UUID uniqueId, Economy economy, BigDecimal bigDecimal) {
        async(() -> {
            ensureUserExists(uniqueId);
            repositories.economy.upsert(uniqueId, economy, bigDecimal);
        });
    }

    @Override
    public void resetEconomy(Economy economy, BigDecimal amount) {
        async(() -> repositories.economy.reset(economy, amount));
    }

    @Override
    public void deleteCooldown(UUID uniqueId, String key) {
        async(() -> repositories.cooldowns.delete(uniqueId, key));
    }

    @Override
    public User updateUserMoney(UUID uniqueId) {
        User fakeUser = new ZUser(this.plugin, uniqueId);
        fakeUser.setEconomies(repositories.economy.select(uniqueId));
        return fakeUser;
    }

    @Override
    public void getUserEconomy(String userName, Consumer<List<EconomyDTO>> consumer) {
        async(() -> {
            List<EconomyDTO> economyDTOS = getLocalEconomyDTO(userName);
            if (!economyDTOS.isEmpty()) {
                consumer.accept(economyDTOS);
                return;
            }
            fetchUniqueId(userName, uuid -> {
                if (uuid == null) {
                    consumer.accept(new ArrayList<>());
                    return;
                }
                consumer.accept(repositories.economy.select(uuid));
            });
        });
    }

    @Override
    public void fetchUniqueId(String userName, Consumer<UUID> consumer) {
        if (this.localUUIDS.containsKey(userName)) {
            consumer.accept(this.localUUIDS.get(userName));
            return;
        }
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayerIfCached(userName);
        if (offlinePlayer != null) {
            this.localUUIDS.put(userName, offlinePlayer.getUniqueId());
            consumer.accept(offlinePlayer.getUniqueId());
            return;
        }
        async(() -> getLocalUniqueId(userName).ifPresentOrElse(uuid -> {
            this.localUUIDS.put(userName, uuid);
            consumer.accept(uuid);
        }, () -> {
            List<UserDTO> userDTOS = repositories.users.selectUsers(userName);
            if (userDTOS.isEmpty()) {
                consumer.accept(null);
                return;
            }
            UserDTO userDTO = userDTOS.getFirst();
            this.localUUIDS.put(userName, userDTO.unique_id());
            consumer.accept(userDTO.unique_id());
        }));
    }

    @Override
    public void storeTransactions(UUID fromUuid, UUID toUuid, Economy economy, BigDecimal fromAmount, BigDecimal toAmount, String reason) {
        throw new UnsupportedOperationException("Not yet implemented for MongoDB: storeTransactions");
    }

    @Override
    public List<EconomyTransactionDTO> getTransactions(UUID toUuid, Economy economy) {
        throw new UnsupportedOperationException("Not yet implemented for MongoDB: getTransactions");
    }

    @Override
    public void upsertUser(User user) {
        async(() -> repositories.users.upsert(user));
    }

    @Override
    public void upsertStorage(String key, Object value) {
        throw new UnsupportedOperationException("Not yet implemented for MongoDB: upsertStorage");
    }

    @Override
    public void upsertHome(UUID uniqueId, Home home) {
        async(() -> repositories.homes.upsert(uniqueId, home));
    }

    @Override
    public void deleteHome(UUID uniqueId, String name) {
        async(() -> repositories.homes.deleteHome(uniqueId, name));
    }

    @Override
    public void getHome(UUID uuid, String homeName, Consumer<Optional<Home>> consumer) {
        async(() -> consumer.accept(repositories.homes.getHomes(uuid, homeName).stream().findFirst()));
    }

    @Override
    public void getHomes(UUID uuid, Consumer<List<Home>> consumer) {
        async(() -> consumer.accept(repositories.homes.getHomes(uuid)));
    }

    @Override
    public void updateHomeSocial(UUID uniqueId, Home home) {
        async(() -> repositories.homes.updateSocial(uniqueId, home));
    }

    @Override
    public void addHomeShare(UUID owner, String homeName, UUID target) {
        throw new UnsupportedOperationException("Not yet implemented for MongoDB: addHomeShare");
    }

    @Override
    public void removeHomeShare(UUID owner, String homeName, UUID target) {
        throw new UnsupportedOperationException("Not yet implemented for MongoDB: removeHomeShare");
    }

    @Override
    public void removeAllHomeShares(UUID owner, String homeName) {
        throw new UnsupportedOperationException("Not yet implemented for MongoDB: removeAllHomeShares");
    }

    @Override
    public void getPublicHomes(Consumer<List<PublicHomeDTO>> consumer) {
        async(() -> consumer.accept(repositories.homes.selectPublicHomes()));
    }

    @Override
    public void isHomeSharedWith(UUID owner, String homeName, UUID target, Consumer<Boolean> consumer) {
        throw new UnsupportedOperationException("Not yet implemented for MongoDB: isHomeSharedWith");
    }

    @Override
    public void addIgnore(UUID uniqueId, UUID ignoredId) {
        throw new UnsupportedOperationException("Not yet implemented for MongoDB: addIgnore");
    }

    @Override
    public void removeIgnore(UUID uniqueId, UUID ignoredId) {
        throw new UnsupportedOperationException("Not yet implemented for MongoDB: removeIgnore");
    }

    @Override
    public void insertSanction(Sanction sanction, Consumer<Integer> consumer) {
        if (sanction.getSanctionType() == SanctionType.BAN) {
            this.banSanctions.put(sanction.getPlayerUniqueId(), sanction);
        } else if (sanction.getSanctionType() == SanctionType.UNBAN) {
            this.banSanctions.remove(sanction.getPlayerUniqueId());
        }
        async(() -> repositories.sanctions.insert(sanction, consumer));
    }

    @Override
    public void updateUserBan(UUID uuid, Integer index) {
        if (index == null) this.banSanctions.remove(uuid);
        async(() -> repositories.users.updateBanId(uuid, index));
    }

    @Override
    public void updateUserMute(UUID uuid, Integer index) {
        async(() -> repositories.users.updateMuteId(uuid, index));
    }

    @Override
    public boolean isMute(UUID uuid) {
        Sanction sanction = getMute(uuid);
        return sanction != null && sanction.isActive();
    }

    @Override
    public Sanction getMute(UUID uuid) {
        List<UserDTO> userDTOS = repositories.users.selectUser(uuid);
        if (userDTOS.isEmpty()) return null;
        UserDTO userDTO = userDTOS.getFirst();
        if (userDTO.mute_sanction_id() != null) {
            SanctionDTO sanction = repositories.sanctions.getSanction(userDTO.mute_sanction_id());
            return sanction == null ? null : Sanction.fromDTO(sanction);
        }
        return null;
    }

    @Override
    public List<SanctionDTO> getSanctions(UUID uuid) {
        return repositories.sanctions.getSanctions(uuid);
    }

    @Override
    public void insertChatMessage(UUID uuid, String content) {
        throw new UnsupportedOperationException("Not yet implemented for MongoDB: insertChatMessage");
    }

    @Override
    public void insertPrivateMessage(UUID sender, UUID receiver, String content) {
        throw new UnsupportedOperationException("Not yet implemented for MongoDB: insertPrivateMessage");
    }

    @Override
    public List<ChatMessageDTO> getMessages(UUID targetUuid) {
        throw new UnsupportedOperationException("Not yet implemented for MongoDB: getMessages");
    }

    @Override
    public int deleteChatMessage(UUID playerUuid, String content) {
        throw new UnsupportedOperationException("Not yet implemented for MongoDB: deleteChatMessage");
    }

    @Override
    public Map<Option, Boolean> getOptions(UUID uuid) {
        throw new UnsupportedOperationException("Not yet implemented for MongoDB: getOptions");
    }

    @Override
    public void getOption(UUID uuid, Option option, Consumer<Boolean> consumer) {
        throw new UnsupportedOperationException("Not yet implemented for MongoDB: getOption");
    }

    @Override
    public void insertCommand(UUID uuid, String command) {
        throw new UnsupportedOperationException("Not yet implemented for MongoDB: insertCommand");
    }

    @Override
    public void insertPlayTime(UUID uniqueId, long sessionPlayTime, long playtime, String address) {
        throw new UnsupportedOperationException("Not yet implemented for MongoDB: insertPlayTime");
    }

    @Override
    public UserRecord fetchUserRecord(UUID uuid) {
        throw new UnsupportedOperationException("Not yet implemented for MongoDB: fetchUserRecord");
    }

    @Override
    public List<UserDTO> getUsers(String ip) {
        throw new UnsupportedOperationException("Not yet implemented for MongoDB: getUsers");
    }

    @Override
    public List<CooldownDTO> getCooldowns(UUID uniqueId) {
        return repositories.cooldowns.select(uniqueId);
    }

    @Override
    public void setPowerTools(UUID uniqueId, Material material, String command) {
        throw new UnsupportedOperationException("Not yet implemented for MongoDB: setPowerTools");
    }

    @Override
    public void deletePowerTools(UUID uniqueId, Material material) {
        throw new UnsupportedOperationException("Not yet implemented for MongoDB: deletePowerTools");
    }

    @Override
    public void addMailBoxItem(MailBoxItem mailBoxItem) {
        throw new UnsupportedOperationException("Not yet implemented for MongoDB: addMailBoxItem");
    }

    @Override
    public void clearMailBox(UUID uuid) {
        throw new UnsupportedOperationException("Not yet implemented for MongoDB: clearMailBox");
    }

    @Override
    public void removeMailBoxItem(int id) {
        throw new UnsupportedOperationException("Not yet implemented for MongoDB: removeMailBoxItem");
    }

    @Override
    public void addMailMessage(MailMessage mailMessage) {
        throw new UnsupportedOperationException("Not yet implemented for MongoDB: addMailMessage");
    }

    @Override
    public void markMailMessagesAsRead(UUID uniqueId) {
        throw new UnsupportedOperationException("Not yet implemented for MongoDB: markMailMessagesAsRead");
    }

    @Override
    public void clearMailMessages(UUID uniqueId) {
        throw new UnsupportedOperationException("Not yet implemented for MongoDB: clearMailMessages");
    }

    @Override
    public List<MailMessageDTO> getMailMessages(UUID uniqueId) {
        throw new UnsupportedOperationException("Not yet implemented for MongoDB: getMailMessages");
    }

    @Override
    public List<UserEconomyRankingDTO> getEconomyRanking(Economy economy) {
        List<UserEconomyDTO> all = repositories.economy.getAll();
        Map<UUID, BigDecimal> amountByUuid = new HashMap<>();
        for (UserEconomyDTO e : all) {
            if (e.economy_name().equals(economy.getName())) amountByUuid.put(e.unique_id(), e.amount());
        }
        List<UserEconomyRankingDTO> ranking = new ArrayList<>();
        for (UserDTO user : repositories.users.selectAll()) {
            BigDecimal amount = amountByUuid.getOrDefault(user.unique_id(), BigDecimal.ZERO);
            ranking.add(new UserEconomyRankingDTO(user.unique_id(), user.name(), amount));
        }
        ranking.sort(Comparator.comparing(UserEconomyRankingDTO::amount).reversed());
        return ranking;
    }

    @Override
    public List<MailBoxDTO> getMailBox(UUID uuid) {
        throw new UnsupportedOperationException("Not yet implemented for MongoDB: getMailBox");
    }

    @Override
    public void fetchOfflinePlayerEconomies(Consumer<List<UserEconomyDTO>> consumer) {
        async(() -> consumer.accept(repositories.economy.getAll()));
    }

    @Override
    public void setVote(UUID uniqueId, long vote, long offline_vote) {
        async(() -> repositories.users.setVote(uniqueId, vote, offline_vote));
    }

    @Override
    public UserVoteDTO getVote(UUID uniqueId) {
        var user = getUser(uniqueId);
        if (user != null) return new UserVoteDTO(uniqueId, user.getVote(), 0);
        var users = repositories.users.selectVoteUser(uniqueId);
        return users.isEmpty() ? new UserVoteDTO(uniqueId, 0, 0) : users.getFirst();
    }

    @Override
    public void updateServerStorage(String key, Object object) {
        throw new UnsupportedOperationException("Not yet implemented for MongoDB: updateServerStorage");
    }

    @Override
    public void setLastVote(UUID uniqueId, String site) {
        throw new UnsupportedOperationException("Not yet implemented for MongoDB: setLastVote");
    }

    @Override
    public void resetVotes() {
        async(() -> repositories.users.resetVotes());
    }

    @Override
    public void updateVaultQuantity(UUID uniqueId, int vaultId, int slot, long quantity) {
        throw new UnsupportedOperationException("Not yet implemented for MongoDB: updateVaultQuantity");
    }

    @Override
    public void removeVaultItem(UUID uniqueId, int vaultId, int slot) {
        throw new UnsupportedOperationException("Not yet implemented for MongoDB: removeVaultItem");
    }

    @Override
    public void createVaultItem(UUID uniqueId, int vaultId, int slot, long quantity, String item) {
        throw new UnsupportedOperationException("Not yet implemented for MongoDB: createVaultItem");
    }

    @Override
    public Optional<VaultItemDTO> getVaultItem(UUID uniqueId, int vaultId, int slot) {
        throw new UnsupportedOperationException("Not yet implemented for MongoDB: getVaultItem");
    }

    @Override
    public boolean forceRemoveVaultItem(UUID uniqueId, int vaultId, int slot) {
        throw new UnsupportedOperationException("Not yet implemented for MongoDB: forceRemoveVaultItem");
    }

    @Override
    public void setVaultSlot(UUID uniqueId, int slots) {
        throw new UnsupportedOperationException("Not yet implemented for MongoDB: setVaultSlot");
    }

    @Override
    public List<VaultDTO> getVaults() {
        throw new UnsupportedOperationException("Not yet implemented for MongoDB: getVaults");
    }

    @Override
    public List<VaultItemDTO> getVaultItems() {
        throw new UnsupportedOperationException("Not yet implemented for MongoDB: getVaultItems");
    }

    @Override
    public List<PlayerSlotDTO> getPlayerVaultSlots() {
        throw new UnsupportedOperationException("Not yet implemented for MongoDB: getPlayerVaultSlots");
    }

    @Override
    public void updateVault(UUID uniqueId, Vault vault) {
        throw new UnsupportedOperationException("Not yet implemented for MongoDB: updateVault");
    }

    @Override
    public void updateUserFrozen(UUID uuid, boolean frozen) {
        async(() -> repositories.users.updateFrozen(uuid, frozen));
    }

    @Override
    public void upsertFlySeconds(UUID uniqueId, long flySeconds) {
        async(() -> repositories.users.updateFly(uniqueId, flySeconds));
    }

    @Override
    public long getFlySeconds(UUID uniqueId) {
        return repositories.users.selectFly(uniqueId);
    }

    @Override
    public void updatePlayerTimeWeather(UUID uniqueId, long playerTime, String playerWeather) {
        async(() -> repositories.users.updatePlayerTimeWeather(uniqueId, playerTime, playerWeather));
    }

    @Override
    public void deleteWorldData(String worldName) {
        repositories.users.deleteWorldData(worldName);
        repositories.homes.deleteWorldData(worldName);
    }

    @Override
    public void linkDiscordAccount(UUID uniqueId, String minecraftName, String discordName, long userId) {
        throw new UnsupportedOperationException("Not yet implemented for MongoDB: linkDiscordAccount");
    }

    @Override
    public Optional<DiscordAccountDTO> selectDiscordAccount(UUID uniqueId) {
        throw new UnsupportedOperationException("Not yet implemented for MongoDB: selectDiscordAccount");
    }

    @Override
    public Optional<DiscordCodeDTO> selectCode(String code) {
        throw new UnsupportedOperationException("Not yet implemented for MongoDB: selectCode");
    }

    @Override
    public void clearCode(DiscordCodeDTO code) {
        throw new UnsupportedOperationException("Not yet implemented for MongoDB: clearCode");
    }

    @Override
    public void insertDiscordLog(DiscordAction action, UUID uniqueId, String minecraftName, String discordName, long userId, String data) {
        throw new UnsupportedOperationException("Not yet implemented for MongoDB: insertDiscordLog");
    }

    @Override
    public void unlinkDiscordAccount(UUID uniqueId) {
        throw new UnsupportedOperationException("Not yet implemented for MongoDB: unlinkDiscordAccount");
    }

    @Override
    public StepDTO selectStep(UUID uniqueId, Step step) {
        throw new UnsupportedOperationException("Not yet implemented for MongoDB: selectStep");
    }

    @Override
    public void createStep(UUID uniqueId, Step step, long playTime) {
        throw new UnsupportedOperationException("Not yet implemented for MongoDB: createStep");
    }

    @Override
    public void finishStep(UUID uniqueId, Step step, String data, long playTimeEnd, long playTimeBetween) {
        throw new UnsupportedOperationException("Not yet implemented for MongoDB: finishStep");
    }

    @Override
    public List<String> getPlayerNames() {
        return repositories.users.getPlayerNames();
    }

    private void ensureUserExists(UUID uniqueId) {
        if (this.users.containsKey(uniqueId) || this.existingUUIDs.contains(uniqueId)) return;
        if (repositories.users.exists(uniqueId)) {
            this.existingUUIDs.add(uniqueId);
            return;
        }
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uniqueId);
        String name = offlinePlayer != null && offlinePlayer.getName() != null ? offlinePlayer.getName() : uniqueId.toString();
        repositories.users.upsert(uniqueId, name);
        this.existingUUIDs.add(uniqueId);
    }
}
