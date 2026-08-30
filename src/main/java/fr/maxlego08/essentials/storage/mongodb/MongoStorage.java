package fr.maxlego08.essentials.storage.mongodb;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.discord.DiscordAction;
import fr.maxlego08.essentials.api.dto.*;
import fr.maxlego08.essentials.api.economy.Economy;
import fr.maxlego08.essentials.api.home.Home;
import fr.maxlego08.essentials.api.mailbox.MailBoxItem;
import fr.maxlego08.essentials.api.mailbox.MailMessage;
import fr.maxlego08.essentials.api.sanction.Sanction;
import fr.maxlego08.essentials.api.server.MongoConfiguration;
import fr.maxlego08.essentials.api.steps.Step;
import fr.maxlego08.essentials.api.storage.IStorage;
import fr.maxlego08.essentials.api.user.Option;
import fr.maxlego08.essentials.api.user.User;
import fr.maxlego08.essentials.api.user.UserRecord;
import fr.maxlego08.essentials.api.vault.Vault;
import fr.maxlego08.essentials.zutils.utils.StorageHelper;
import org.bukkit.Bukkit;
import org.bukkit.Material;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public class MongoStorage extends StorageHelper implements IStorage {

    private final MongoConnection connection;
    private final MongoRepositories repositories;

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
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onDisable() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public User createOrLoad(UUID uniqueId, String playerName) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onPlayerQuit(UUID uniqueId) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public User getUser(UUID uniqueId) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void updateOption(UUID uniqueId, Option option, boolean value) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void updateCooldown(UUID uniqueId, String key, long expiredAt) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void updateEconomy(UUID uniqueId, Economy economy, BigDecimal bigDecimal) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void resetEconomy(Economy economy, BigDecimal amount) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void deleteCooldown(UUID uniqueId, String key) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public User updateUserMoney(UUID uniqueId) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void getUserEconomy(String userName, Consumer<List<EconomyDTO>> consumer) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void fetchUniqueId(String userName, Consumer<UUID> consumer) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void storeTransactions(UUID fromUuid, UUID toUuid, Economy economy, BigDecimal fromAmount, BigDecimal toAmount, String reason) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public List<EconomyTransactionDTO> getTransactions(UUID toUuid, Economy economy) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void upsertUser(User user) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void upsertStorage(String key, Object value) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void upsertHome(UUID uniqueId, Home home) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void deleteHome(UUID uniqueId, String name) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void getHome(UUID uuid, String homeName, Consumer<Optional<Home>> consumer) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void getHomes(UUID uuid, Consumer<List<Home>> consumer) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void updateHomeSocial(UUID uniqueId, Home home) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void addHomeShare(UUID owner, String homeName, UUID target) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void removeHomeShare(UUID owner, String homeName, UUID target) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void removeAllHomeShares(UUID owner, String homeName) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void getPublicHomes(Consumer<List<PublicHomeDTO>> consumer) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void isHomeSharedWith(UUID owner, String homeName, UUID target, Consumer<Boolean> consumer) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void addIgnore(UUID uniqueId, UUID ignoredId) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void removeIgnore(UUID uniqueId, UUID ignoredId) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void insertSanction(Sanction sanction, Consumer<Integer> consumer) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void updateUserBan(UUID uuid, Integer index) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void updateUserMute(UUID uuid, Integer index) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public boolean isMute(UUID uuid) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public Sanction getMute(UUID uuid) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public List<SanctionDTO> getSanctions(UUID uuid) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void insertChatMessage(UUID uuid, String content) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void insertPrivateMessage(UUID sender, UUID receiver, String content) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public List<ChatMessageDTO> getMessages(UUID targetUuid) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int deleteChatMessage(UUID playerUuid, String content) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public Map<Option, Boolean> getOptions(UUID uuid) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void getOption(UUID uuid, Option option, Consumer<Boolean> consumer) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void insertCommand(UUID uuid, String command) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void insertPlayTime(UUID uniqueId, long sessionPlayTime, long playtime, String address) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public UserRecord fetchUserRecord(UUID uuid) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public List<UserDTO> getUsers(String ip) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public List<CooldownDTO> getCooldowns(UUID uniqueId) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void setPowerTools(UUID uniqueId, Material material, String command) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void deletePowerTools(UUID uniqueId, Material material) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void addMailBoxItem(MailBoxItem mailBoxItem) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void clearMailBox(UUID uuid) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void removeMailBoxItem(int id) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void addMailMessage(MailMessage mailMessage) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void markMailMessagesAsRead(UUID uniqueId) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void clearMailMessages(UUID uniqueId) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public List<MailMessageDTO> getMailMessages(UUID uniqueId) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public List<UserEconomyRankingDTO> getEconomyRanking(Economy economy) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public List<MailBoxDTO> getMailBox(UUID uniqueId) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void fetchOfflinePlayerEconomies(Consumer<List<UserEconomyDTO>> consumer) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void setVote(UUID uniqueId, long vote, long offline_vote) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public UserVoteDTO getVote(UUID uniqueId) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void updateServerStorage(String key, Object object) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void setLastVote(UUID uniqueId, String site) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void resetVotes() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void updateVaultQuantity(UUID uniqueId, int vaultId, int slot, long quantity) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void removeVaultItem(UUID uniqueId, int vaultId, int slot) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void createVaultItem(UUID uniqueId, int vaultId, int slot, long quantity, String item) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public Optional<VaultItemDTO> getVaultItem(UUID uniqueId, int vaultId, int slot) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public boolean forceRemoveVaultItem(UUID uniqueId, int vaultId, int slot) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void setVaultSlot(UUID uniqueId, int slots) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public List<VaultDTO> getVaults() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public List<VaultItemDTO> getVaultItems() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public List<PlayerSlotDTO> getPlayerVaultSlots() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void updateVault(UUID uniqueId, Vault vault) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void updateUserFrozen(UUID uuid, boolean frozen) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void upsertFlySeconds(UUID uniqueId, long flySeconds) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public long getFlySeconds(UUID uniqueId) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void updatePlayerTimeWeather(UUID uniqueId, long playerTime, String playerWeather) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void deleteWorldData(String worldName) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void linkDiscordAccount(UUID uniqueId, String minecraftName, String discordName, long userId) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public Optional<DiscordAccountDTO> selectDiscordAccount(UUID uniqueId) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public Optional<DiscordCodeDTO> selectCode(String code) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void clearCode(DiscordCodeDTO code) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void insertDiscordLog(DiscordAction action, UUID uniqueId, String minecraftName, String discordName, long userId, String data) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void unlinkDiscordAccount(UUID uniqueId) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public StepDTO selectStep(UUID uniqueId, Step step) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void createStep(UUID uniqueId, Step step, long playTime) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void finishStep(UUID uniqueId, Step step, String data, long playTimeEnd, long playTimeBetween) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public List<String> getPlayerNames() {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
