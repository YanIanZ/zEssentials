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
import fr.maxlego08.menu.common.utils.nms.ItemStackUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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
        repositories.mailBox.deleteExpiredItems();
        this.existingUUIDs.addAll(repositories.users.selectUUIDs());
        setActiveSanctions(repositories.sanctions.getActiveBan());
        this.plugin.getServerStorage().setContents(repositories.serverStorage.select());
    }

    @Override
    public void onDisable() {
        if (this.connection != null) this.connection.close();
    }

    @Override
    public User createOrLoad(UUID uniqueId, String playerName) {
        User user = new ZUser(this.plugin, uniqueId);
        user.setName(playerName);
        this.users.put(uniqueId, user);

        async(() -> {
            var optional = repositories.users.selectUser(uniqueId).stream().findFirst();
            if (optional.isEmpty()) {
                this.firstJoin(user);
            }

            repositories.users.upsert(uniqueId, playerName);

            this.localUUIDS.entrySet().removeIf(entry -> entry.getValue().equals(uniqueId) && !entry.getKey().equals(playerName));
            this.localUUIDS.put(playerName, uniqueId);

            if (Bukkit.getOnlineMode()) {
                List<UserDTO> duplicates = repositories.users.selectUsers(playerName);
                for (UserDTO duplicate : duplicates) {
                    if (duplicate.unique_id().equals(uniqueId)) continue;
                    String currentName = fetchNameFromMojang(duplicate.unique_id());
                    if (currentName != null && !currentName.equals(playerName)) {
                        repositories.users.updateName(duplicate.unique_id(), currentName);
                        this.localUUIDS.remove(playerName);
                        this.localUUIDS.put(currentName, duplicate.unique_id());
                        this.plugin.getLogger().info("Updated player name for UUID " + duplicate.unique_id() + " from '" + playerName + "' to '" + currentName + "' (detected duplicate name).");
                    } else if (currentName == null) {
                        this.plugin.getLogger().warning("Could not fetch current name from Mojang for UUID " + duplicate.unique_id() + " (duplicate name '" + playerName + "'). The player may have plugin inconsistencies.");
                    }
                }
            }

            if (optional.isPresent()) {
                UserDTO userDTO = optional.get();
                if (userDTO.mute_sanction_id() != null) {
                    SanctionDTO sanction = repositories.sanctions.getSanction(userDTO.mute_sanction_id());
                    if (sanction != null && sanction.isActive()) {
                        user.setMuteSanction(Sanction.fromDTO(sanction));
                    }
                }
                user.setSanction(userDTO.ban_sanction_id(), userDTO.mute_sanction_id());
                user.setWithDTO(userDTO);
                user.setOptions(repositories.options.select(uniqueId));
                user.setCooldowns(repositories.cooldowns.select(uniqueId));
                user.setEconomies(repositories.economy.select(uniqueId));
                user.setHomes(repositories.homes.select(uniqueId));
                user.setHomeShares(repositories.homeShares.selectByOwner(uniqueId));
                user.setIgnoredPlayers(repositories.ignores.select(uniqueId));
                user.setPowerTools(repositories.powerTools.select(uniqueId).stream().collect(Collectors.toMap(PowerToolsDTO::material, PowerToolsDTO::command, (a, b) -> b, LinkedHashMap::new)));
                user.setMailBoxItems(repositories.mailBox.select(uniqueId));
                user.setMailMessages(repositories.mailMessages.select(uniqueId));
                user.setVoteSites(repositories.votes.select(uniqueId));
                repositories.linkAccounts.select(uniqueId).ifPresent(user::setDiscordAccount);
            }
        });

        return user;
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
        async(() -> repositories.options.upsert(uniqueId, option, value));
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
        EconomyTransactionDTO transaction = new EconomyTransactionDTO(fromUuid, toUuid, economy.getName(), reason,
                toAmount.subtract(fromAmount), fromAmount, toAmount, new Date(), new Date());
        async(() -> repositories.transactions.insertTransactions(List.of(transaction)));
    }

    @Override
    public List<EconomyTransactionDTO> getTransactions(UUID toUuid, Economy economy) {
        return repositories.transactions.selectTransactions(toUuid, economy);
    }

    @Override
    public void upsertUser(User user) {
        async(() -> repositories.users.upsert(user));
    }

    @Override
    public void upsertStorage(String key, Object value) {
        async(() -> repositories.serverStorage.upsert(key, value));
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
        async(() -> repositories.homeShares.upsert(owner, homeName, target));
    }

    @Override
    public void removeHomeShare(UUID owner, String homeName, UUID target) {
        async(() -> repositories.homeShares.delete(owner, homeName, target));
    }

    @Override
    public void removeAllHomeShares(UUID owner, String homeName) {
        async(() -> repositories.homeShares.deleteAll(owner, homeName));
    }

    @Override
    public void getPublicHomes(Consumer<List<PublicHomeDTO>> consumer) {
        async(() -> consumer.accept(repositories.homes.selectPublicHomes()));
    }

    @Override
    public void isHomeSharedWith(UUID owner, String homeName, UUID target, Consumer<Boolean> consumer) {
        async(() -> consumer.accept(repositories.homeShares.isSharedWith(owner, homeName, target)));
    }

    @Override
    public void addIgnore(UUID uniqueId, UUID ignoredId) {
        async(() -> repositories.ignores.upsert(uniqueId, ignoredId));
    }

    @Override
    public void removeIgnore(UUID uniqueId, UUID ignoredId) {
        async(() -> repositories.ignores.delete(uniqueId, ignoredId));
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
        async(() -> repositories.chatMessages.insertMessages(List.of(new ChatMessageDTO(uuid, content, new Date()))));
    }

    @Override
    public void insertPrivateMessage(UUID sender, UUID receiver, String content) {
        async(() -> repositories.privateMessages.insertMessages(List.of(new PrivateMessageDTO(sender, receiver, content, new Date()))));
    }

    @Override
    public List<ChatMessageDTO> getMessages(UUID targetUuid) {
        return repositories.chatMessages.getMessages(targetUuid);
    }

    @Override
    public int deleteChatMessage(UUID playerUuid, String content) {
        return repositories.chatMessages.deleteMessages(playerUuid, content);
    }

    @Override
    public Map<Option, Boolean> getOptions(UUID uuid) {
        if (this.users.containsKey(uuid)) {
            return this.users.get(uuid).getOptions();
        }
        return repositories.options.select(uuid).stream().collect(Collectors.toMap(OptionDTO::option_name, OptionDTO::option_value));
    }

    @Override
    public void getOption(UUID uuid, Option option, Consumer<Boolean> consumer) {
        var user = getUser(uuid);
        if (user != null) consumer.accept(user.getOption(option));
        else async(() -> repositories.options.select(uuid, option, consumer));
    }

    @Override
    public void insertCommand(UUID uuid, String command) {
        async(() -> repositories.commands.insertCommands(List.of(new CommandDTO(uuid, command, new Date()))));
    }

    @Override
    public void insertPlayTime(UUID uniqueId, long sessionPlayTime, long playtime, String address) {
        async(() -> {
            if (sessionPlayTime > 0) {
                repositories.playTime.insert(uniqueId, sessionPlayTime, address);
            }
            repositories.users.updatePlayTime(uniqueId, playtime);
        });
    }

    @Override
    public UserRecord fetchUserRecord(UUID uuid) {
        UserDTO userDTO = repositories.users.selectUser(uuid).getFirst();
        List<PlayTimeDTO> playTimeDTOS = repositories.playTime.select(uuid);
        return new UserRecord(userDTO, playTimeDTOS);
    }

    @Override
    public List<UserDTO> getUsers(String ip) {
        return repositories.users.getUsers(ip);
    }

    @Override
    public List<CooldownDTO> getCooldowns(UUID uniqueId) {
        return repositories.cooldowns.select(uniqueId);
    }

    @Override
    public void setPowerTools(UUID uniqueId, Material material, String command) {
        async(() -> repositories.powerTools.upsert(uniqueId, material, command));
    }

    @Override
    public void deletePowerTools(UUID uniqueId, Material material) {
        async(() -> repositories.powerTools.delete(uniqueId, material));
    }

    @Override
    public void addMailBoxItem(MailBoxItem mailBoxItem) {
        async(() -> repositories.mailBox.insert(mailBoxItem));
    }

    @Override
    public void clearMailBox(UUID uuid) {
        async(() -> repositories.mailBox.clear(uuid));
    }

    @Override
    public void removeMailBoxItem(int id) {
        async(() -> repositories.mailBox.delete(id));
    }

    @Override
    public void addMailMessage(MailMessage mailMessage) {
        async(() -> repositories.mailMessages.insert(mailMessage));
    }

    @Override
    public void markMailMessagesAsRead(UUID uniqueId) {
        async(() -> repositories.mailMessages.markAsRead(uniqueId));
    }

    @Override
    public void clearMailMessages(UUID uniqueId) {
        async(() -> repositories.mailMessages.clear(uniqueId));
    }

    @Override
    public List<MailMessageDTO> getMailMessages(UUID uniqueId) {
        return repositories.mailMessages.select(uniqueId);
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
        return repositories.mailBox.select(uuid);
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
        async(() -> repositories.serverStorage.upsert(key, object));
    }

    @Override
    public void setLastVote(UUID uniqueId, String site) {
        async(() -> repositories.votes.setLastVote(uniqueId, site));
    }

    @Override
    public void resetVotes() {
        async(() -> repositories.users.resetVotes());
    }

    @Override
    public void updateVaultQuantity(UUID uniqueId, int vaultId, int slot, long quantity) {
        async(() -> repositories.vaultItems.updateQuantity(uniqueId, vaultId, slot, quantity));
    }

    @Override
    public void removeVaultItem(UUID uniqueId, int vaultId, int slot) {
        async(() -> repositories.vaultItems.removeItem(uniqueId, vaultId, slot));
    }

    @Override
    public void createVaultItem(UUID uniqueId, int vaultId, int slot, long quantity, String item) {
        async(() -> repositories.vaultItems.createNewItem(uniqueId, vaultId, slot, quantity, item));
    }

    @Override
    public Optional<VaultItemDTO> getVaultItem(UUID uniqueId, int vaultId, int slot) {
        return repositories.vaultItems.select(uniqueId, vaultId, slot);
    }

    @Override
    public boolean forceRemoveVaultItem(UUID uniqueId, int vaultId, int slot) {
        return repositories.vaultItems.forceRemove(uniqueId, vaultId, slot);
    }

    @Override
    public void setVaultSlot(UUID uniqueId, int slots) {
        async(() -> repositories.playerSlots.setSlot(uniqueId, slots));
    }

    @Override
    public List<VaultDTO> getVaults() {
        return repositories.vaults.select();
    }

    @Override
    public List<VaultItemDTO> getVaultItems() {
        return repositories.vaultItems.select();
    }

    @Override
    public List<PlayerSlotDTO> getPlayerVaultSlots() {
        return repositories.playerSlots.select();
    }

    @Override
    public void updateVault(UUID uniqueId, Vault vault) {
        async(() -> repositories.vaults.update(uniqueId, vault.getVaultId(), vault.getName(), vault.getIconItemStack() == null ? null : ItemStackUtils.serializeItemStack(vault.getIconItemStack())));
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
        async(() -> repositories.linkAccounts.insert(uniqueId, minecraftName, discordName, userId));
    }

    @Override
    public Optional<DiscordAccountDTO> selectDiscordAccount(UUID uniqueId) {
        return repositories.linkAccounts.select(uniqueId);
    }

    @Override
    public Optional<DiscordCodeDTO> selectCode(String code) {
        return repositories.linkCodes.getCode(code);
    }

    @Override
    public void clearCode(DiscordCodeDTO code) {
        repositories.linkCodes.clearCode(code);
    }

    @Override
    public void insertDiscordLog(DiscordAction action, UUID uniqueId, String minecraftName, String discordName, long userId, String data) {
        async(() -> repositories.linkHistory.insertLog(action, uniqueId, minecraftName, discordName, userId, data));
    }

    @Override
    public void unlinkDiscordAccount(UUID uniqueId) {
        async(() -> repositories.linkAccounts.delete(uniqueId));
    }

    @Override
    public StepDTO selectStep(UUID uniqueId, Step step) {
        return repositories.steps.selectStep(uniqueId, step);
    }

    @Override
    public void createStep(UUID uniqueId, Step step, long playTime) {
        async(() -> repositories.steps.createStep(uniqueId, step, playTime));
    }

    @Override
    public void finishStep(UUID uniqueId, Step step, String data, long playTimeEnd, long playTimeBetween) {
        async(() -> repositories.steps.finishStep(uniqueId, step, data, playTimeBetween, playTimeEnd));
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

    private String fetchNameFromMojang(UUID uuid) {
        String uuidString = uuid.toString().replace("-", "");
        String url = "https://sessionserver.mojang.com/session/minecraft/profile/" + uuidString;
        try {
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            if (connection.getResponseCode() == 200) {
                try (java.io.InputStreamReader reader = new java.io.InputStreamReader(connection.getInputStream())) {
                    com.google.gson.JsonObject jsonObject = com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
                    return jsonObject.get("name").getAsString();
                }
            }
        } catch (Exception exception) {
            this.plugin.getLogger().warning("Failed to fetch player name from Mojang API for UUID " + uuid + ": " + exception.getMessage());
        }
        return null;
    }
}
