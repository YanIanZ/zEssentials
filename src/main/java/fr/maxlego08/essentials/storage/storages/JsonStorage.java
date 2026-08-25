package fr.maxlego08.essentials.storage.storages;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.discord.DiscordAction;
import fr.maxlego08.essentials.api.dto.*;
import fr.maxlego08.essentials.api.economy.Economy;
import fr.maxlego08.essentials.api.home.Home;
import fr.maxlego08.essentials.api.mailbox.MailBoxItem;
import fr.maxlego08.essentials.api.mailbox.MailMessage;
import fr.maxlego08.essentials.api.sanction.Sanction;
import fr.maxlego08.essentials.api.steps.Step;
import fr.maxlego08.essentials.api.storage.IStorage;
import fr.maxlego08.essentials.api.storage.Persist;
import fr.maxlego08.essentials.api.user.Option;
import fr.maxlego08.essentials.api.user.User;
import fr.maxlego08.essentials.api.user.UserRecord;
import fr.maxlego08.essentials.api.vault.Vault;
import fr.maxlego08.essentials.user.ZUser;
import fr.maxlego08.essentials.zutils.utils.StorageHelper;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import fr.maxlego08.menu.common.utils.nms.ItemStackUtils;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.LinkedHashMap;
import java.util.Objects;

public class JsonStorage extends StorageHelper implements IStorage {

    private JsonStorageState state;

    public JsonStorage(EssentialsPlugin plugin) {
        super(plugin);
    }

    public File getFolder() {
        return new File(this.plugin.getDataFolder(), "users");
    }

    private void createFolder() {
        File folder = getFolder();
        if (!folder.exists() && !folder.mkdirs()) {
            this.plugin.getLogger().warning("Unable to create JSON storage folder: " + folder.getAbsolutePath());
        }
    }

    @Override
    public void onEnable() {
        this.createFolder();

        File folder = getFolder();
        this.totalUser = folder == null ? 0 : Optional.ofNullable(folder.listFiles()).map(e -> e.length).orElse(0);
        this.state = loadState();
        this.state.sanctions.stream()
                .filter(sanctionDTO -> sanctionDTO.sanction_type() == fr.maxlego08.essentials.api.sanction.SanctionType.BAN && sanctionDTO.expired_at() != null && sanctionDTO.expired_at().getTime() > System.currentTimeMillis())
                .forEach(sanctionDTO -> this.banSanctions.put(sanctionDTO.player_unique_id(), Sanction.fromDTO(sanctionDTO)));
        this.plugin.getLogger().severe("Please use MYSQL storage, the JSON is only to enable the for the first installation of the plugin.");
    }

    @Override
    public void onDisable() {
        this.createFolder();
        Bukkit.getOnlinePlayers().forEach(player -> {
            UUID uniqueId = player.getUniqueId();
            User user = getUser(uniqueId);
            Persist persist = this.plugin.getPersist();
            persist.save(user, getUserFile(uniqueId));
        });
        saveState();
    }

    private File getUserFile(UUID uniqueId) {
        return new File(getFolder(), uniqueId + ".json");
    }

    private File getStateFile() {
        return new File(this.plugin.getDataFolder(), "json-storage-state.json");
    }

    private synchronized JsonStorageState loadState() {
        JsonStorageState loaded = this.plugin.getPersist().load(JsonStorageState.class, getStateFile());
        return loaded == null ? new JsonStorageState() : loaded;
    }

    private synchronized JsonStorageState state() {
        if (this.state == null) {
            this.state = loadState();
        }
        return this.state;
    }

    private synchronized void saveState() {
        this.plugin.getPersist().save(state(), getStateFile());
    }

    private User loadUser(UUID uniqueId) {
        User user = this.users.get(uniqueId);
        if (user != null) return user;
        return this.plugin.getPersist().load(User.class, getUserFile(uniqueId));
    }

    private void saveUser(User user) {
        if (user == null) return;
        this.plugin.getPersist().save(user, getUserFile(user.getUniqueId()));
    }

    private void saveUser(UUID uniqueId) {
        saveUser(loadUser(uniqueId));
    }

    private void updateUser(UUID uniqueId, Consumer<User> consumer) {
        User user = loadUser(uniqueId);
        if (user == null) return;
        consumer.accept(user);
        saveUser(user);
        this.users.put(uniqueId, user);
    }

    private List<User> getAllUsers() {
        Map<UUID, User> result = new LinkedHashMap<>(this.users);

        File[] files = getFolder().listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));
        if (files != null) {
            for (File file : files) {
                try {
                    UUID uuid = UUID.fromString(file.getName().substring(0, file.getName().length() - 5));
                    result.computeIfAbsent(uuid, key -> this.plugin.getPersist().load(User.class, file));
                } catch (Exception ignored) {
                }
            }
        }

        return result.values().stream().filter(Objects::nonNull).toList();
    }

    private UserDTO toUserDTO(User user) {
        return new UserDTO(
                user.getUniqueId(),
                user.getName(),
                locationAsString(user.getLastLocation()),
                user.getActiveBanId() == 0 ? null : user.getActiveBanId(),
                user.getActiveMuteId() == 0 ? null : user.getActiveMuteId(),
                user.getPlayTime(),
                new Date(),
                new Date(),
                user.getVote(),
                user.getOfflineVotes(),
                user.isFrozen(),
                user.getFlySeconds(),
                user.getPlayerTime(),
                user.getPlayerWeather()
        );
    }

    private void upsertVaultRecord(UUID uniqueId, int vaultId, String name, String icon) {
        JsonStorageState backend = state();
        backend.vaults.removeIf(vaultDTO -> vaultDTO.unique_id().equals(uniqueId) && vaultDTO.vault_id() == vaultId);
        backend.vaults.add(new VaultDTO(uniqueId, vaultId, name, icon));
        saveState();
    }

    private void upsertPlayerSlot(UUID uniqueId, int slots) {
        JsonStorageState backend = state();
        backend.playerSlots.removeIf(playerSlotDTO -> playerSlotDTO.unique_id().equals(uniqueId));
        if (slots > 0) {
            backend.playerSlots.add(new PlayerSlotDTO(uniqueId, slots));
        }
        saveState();
    }

    @Override
    public User createOrLoad(UUID uniqueId, String playerName) {

        this.createFolder();

        File file = getUserFile(uniqueId);
        Persist persist = this.plugin.getPersist();
        User user = persist.load(User.class, file);

        // If user is null, we need to create a new user
        if (user == null) {

            user = new ZUser(plugin, uniqueId);
            user.setName(playerName);
            this.firstJoin(user);

            persist.save(user, file);
        }

        this.users.put(uniqueId, user);
        return user;
    }

    private void saveFileAsync(UUID uniqueId) {
        User user = getUser(uniqueId);
        if (user == null) return;
        this.plugin.getScheduler().runAsync(wrappedTask -> {
            Persist persist = this.plugin.getPersist();
            persist.save(user, getUserFile(uniqueId));
        });
    }

    @Override
    public void onPlayerQuit(UUID uniqueId) {
        this.saveFileAsync(uniqueId);
        this.users.remove(uniqueId);
    }

    @Override
    public User getUser(UUID uniqueId) {
        return this.users.get(uniqueId);
    }

    @Override
    public void updateOption(UUID uniqueId, Option option, boolean value) {
        this.saveFileAsync(uniqueId);
    }

    @Override
    public void updateCooldown(UUID uniqueId, String key, long expiredAt) {
        this.saveFileAsync(uniqueId);
    }

    @Override
    public void updateEconomy(UUID uniqueId, Economy economy, BigDecimal bigDecimal) {
        this.saveFileAsync(uniqueId);
    }

    @Override
    public void resetEconomy(Economy economy, BigDecimal amount) {
        for (User user : getAllUsers()) {
            user.getBalances().put(economy.getName(), amount);
            saveUser(user);
        }
    }

    @Override
    public void deleteCooldown(UUID uniqueId, String key) {
        this.saveFileAsync(uniqueId);
    }

    @Override
    public User updateUserMoney(UUID uniqueId) {
        return createOrLoad(uniqueId, "offline");
    }

    @Override
    public void upsertUser(User user) {
        this.saveFileAsync(user.getUniqueId());
    }

    @Override
    public void getUserEconomy(String userName, Consumer<List<EconomyDTO>> consumer) {
        async(() -> {

            List<EconomyDTO> economyDTOS = getLocalEconomyDTO(userName);
            if (!economyDTOS.isEmpty()) {
                consumer.accept(economyDTOS);
                return;
            }

            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(userName);
            User loadUser = createOrLoad(offlinePlayer.getUniqueId(), "offline");
            consumer.accept(loadUser.getBalances().entrySet().stream().map(e -> new EconomyDTO(e.getKey(), e.getValue())).toList());
        });
    }

    @Override
    public void fetchUniqueId(String userName, Consumer<UUID> consumer) {

        if (this.localUUIDS.containsKey(userName)) {
            consumer.accept(this.localUUIDS.get(userName));
            return;
        }

        // User plugin cache first
        getLocalUniqueId(userName).ifPresentOrElse(uuid -> {
            this.localUUIDS.put(userName, uuid);
            consumer.accept(uuid);
        }, () -> {

            // User server cache
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayerIfCached(userName);
            if (offlinePlayer != null) {
                this.localUUIDS.put(userName, offlinePlayer.getUniqueId());
                consumer.accept(offlinePlayer.getUniqueId());
                return;
            }

            // Try load offline player
            offlinePlayer = Bukkit.getOfflinePlayer(userName);
            this.localUUIDS.put(userName, offlinePlayer.getUniqueId());
            consumer.accept(offlinePlayer.getUniqueId());
        });
    }

    @Override
    public void storeTransactions(UUID fromUuid, UUID toUuid, Economy economy, BigDecimal fromAmount, BigDecimal toAmount, String reason) {
        state().transactions.add(new EconomyTransactionDTO(fromUuid, toUuid, economy.getName(), reason, toAmount.subtract(fromAmount), fromAmount, toAmount, new Date(), new Date()));
        saveState();
    }

    @Override
    public List<EconomyTransactionDTO> getTransactions(UUID toUuid, Economy economy) {
        return state().transactions.stream()
                .filter(transaction -> transaction.to_unique_id().equals(toUuid) && transaction.economy_name().equalsIgnoreCase(economy.getName()))
                .toList();
    }

    @Override
    public void upsertStorage(String key, Object value) {
        state().serverStorage.put(key, this.plugin.getGson().toJson(value));
        saveState();
    }

    @Override
    public void upsertHome(UUID uniqueId, Home home) {
        User user = getUser(uniqueId);
        if (user == null) {
            user = createOrLoad(uniqueId, "");
        }
        // Ensure the home is present in the in-memory list before saving.
        // The normal /sethome path already added it; the import path calls this directly without pre-adding.
        if (user.getHome(home.getName()).isEmpty()) {
            user.getHomes().add(home);
        }
        this.saveFileAsync(uniqueId);
    }

    @Override
    public void deleteHome(UUID uniqueId, String name) {
        User user = getUser(uniqueId);
        if (user == null) {
            user = createOrLoad(uniqueId, "");
            user.removeHome(name);
            User finalUser = user;
            this.plugin.getScheduler().runAsync(wrappedTask -> {
                Persist persist = this.plugin.getPersist();
                persist.save(finalUser, getUserFile(uniqueId));
            });
            return;
        }
        this.saveFileAsync(uniqueId);
    }

    @Override
    public void getHome(UUID uuid, String homeName, Consumer<Optional<Home>> consumer) {
        consumer.accept(createOrLoad(uuid, "").getHomes().stream().filter(home -> home.getName().equalsIgnoreCase(homeName)).findFirst());
    }

    @Override
    public void getHomes(UUID uuid, Consumer<List<Home>> consumer) {
        consumer.accept(createOrLoad(uuid, "").getHomes());
    }

    @Override
    public void updateHomeSocial(UUID uniqueId, Home home) {
        this.saveFileAsync(uniqueId);
    }

    @Override
    public void addHomeShare(UUID owner, String homeName, UUID target) {
        this.saveFileAsync(owner);
    }

    @Override
    public void removeHomeShare(UUID owner, String homeName, UUID target) {
        this.saveFileAsync(owner);
    }

    @Override
    public void removeAllHomeShares(UUID owner, String homeName) {
        this.saveFileAsync(owner);
    }

    @Override
    public void getPublicHomes(Consumer<List<PublicHomeDTO>> consumer) {
        async(() -> {
            List<PublicHomeDTO> result = new ArrayList<>();
            java.util.Set<UUID> seen = new java.util.HashSet<>();

            // Online / cached users first (freshest data)
            for (User user : this.users.values()) {
                seen.add(user.getUniqueId());
                collectPublicHomes(user, result);
            }

            // Then scan the offline user files on disk
            File[] files = getFolder().listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));
            if (files != null) {
                for (File file : files) {
                    try {
                        UUID uuid = UUID.fromString(file.getName().substring(0, file.getName().length() - 5));
                        if (!seen.add(uuid)) continue;
                        User user = this.plugin.getPersist().load(User.class, file);
                        if (user != null) collectPublicHomes(user, result);
                    } catch (Exception ignored) {
                    }
                }
            }

            consumer.accept(result);
        });
    }

    private void collectPublicHomes(User user, List<PublicHomeDTO> result) {
        for (Home home : user.getHomes()) {
            if (home.isPublic()) {
                result.add(new PublicHomeDTO(user.getUniqueId(), home.getName(), locationAsString(home.getLocation()), home.getMaterial() == null ? null : home.getMaterial().name(), home.getCategory()));
            }
        }
    }

    @Override
    public void isHomeSharedWith(UUID owner, String homeName, UUID target, Consumer<Boolean> consumer) {
        User user = createOrLoad(owner, "");
        consumer.accept(user.getHomeShares(homeName).contains(target));
    }

    @Override
    public void addIgnore(UUID uniqueId, UUID ignoredId) {
        this.saveFileAsync(uniqueId);
    }

    @Override
    public void removeIgnore(UUID uniqueId, UUID ignoredId) {
        this.saveFileAsync(uniqueId);
    }

    @Override
    public void insertSanction(Sanction sanction, Consumer<Integer> consumer) {
        JsonStorageState backend = state();
        sanction.setId((int) backend.nextSanctionId++);
        backend.sanctions.add(new SanctionDTO(sanction.getId(), sanction.getPlayerUniqueId(), sanction.getSenderUniqueId(), sanction.getReason(), sanction.getCreatedAt(), sanction.getExpiredAt(), sanction.getSanctionType(), sanction.getDuration()));

        if (sanction.getSanctionType() == fr.maxlego08.essentials.api.sanction.SanctionType.BAN) {
            this.banSanctions.put(sanction.getPlayerUniqueId(), sanction);
        } else if (sanction.getSanctionType() == fr.maxlego08.essentials.api.sanction.SanctionType.UNBAN) {
            this.banSanctions.remove(sanction.getPlayerUniqueId());
        }

        saveState();
        consumer.accept(sanction.getId());
    }

    @Override
    public void updateUserBan(UUID uuid, Integer index) {
        updateUser(uuid, user -> user.setSanction(index, user.getActiveMuteId()));
    }

    @Override
    public void updateUserMute(UUID uuid, Integer index) {
        updateUser(uuid, user -> user.setSanction(user.getActiveBanId(), index));
    }

    @Override
    public boolean isMute(UUID uuid) {
        Sanction sanction = getMute(uuid);
        return sanction != null && sanction.isActive();
    }

    @Override
    public Sanction getMute(UUID uuid) {
        User user = loadUser(uuid);
        if (user == null || user.getActiveMuteId() <= 0) return null;
        return state().sanctions.stream()
                .filter(sanctionDTO -> sanctionDTO.id() == user.getActiveMuteId())
                .findFirst()
                .map(Sanction::fromDTO)
                .orElse(null);
    }

    @Override
    public List<SanctionDTO> getSanctions(UUID uuid) {
        return state().sanctions.stream().filter(sanctionDTO -> sanctionDTO.player_unique_id().equals(uuid)).toList();
    }

    @Override
    public void insertChatMessage(UUID uuid, String content) {
        state().chatMessages.add(new ChatMessageDTO(uuid, content, new Date()));
        saveState();
    }

    @Override
    public void insertPrivateMessage(UUID sender, UUID receiver, String content) {
        state().privateMessages.add(new PrivateMessageDTO(sender, receiver, content, new Date()));
        saveState();
    }

    @Override
    public void insertCommand(UUID uuid, String command) {
        state().commands.add(new CommandDTO(uuid, command, new Date()));
        saveState();
    }

    @Override
    public void insertPlayTime(UUID uniqueId, long sessionPlayTime, long playtime, String address) {
        if (sessionPlayTime > 0) {
            state().playTimes.add(new PlayTimeDTO(uniqueId, sessionPlayTime, address, new Date()));
        }
        updateUser(uniqueId, user -> {
            user.setPlayTime(playtime);
            user.setAddress(address);
        });
        saveState();
    }

    @Override
    public UserRecord fetchUserRecord(UUID uuid) {
        User user = loadUser(uuid);
        if (user == null) {
            user = createOrLoad(uuid, Bukkit.getOfflinePlayer(uuid).getName() == null ? uuid.toString() : Bukkit.getOfflinePlayer(uuid).getName());
        }
        List<PlayTimeDTO> playTimeDTOS = state().playTimes.stream().filter(playTimeDTO -> playTimeDTO.unique_id().equals(uuid)).toList();
        return new UserRecord(toUserDTO(user), playTimeDTOS);
    }

    @Override
    public List<UserDTO> getUsers(String ip) {
        return getAllUsers().stream().filter(user -> ip.equalsIgnoreCase(user.getAddress())).map(this::toUserDTO).toList();
    }

    @Override
    public List<ChatMessageDTO> getMessages(UUID targetUuid) {
        return state().chatMessages.stream().filter(chatMessageDTO -> chatMessageDTO.unique_id().equals(targetUuid)).toList();
    }

    @Override
    public Map<Option, Boolean> getOptions(UUID uuid) {
        if (this.users.containsKey(uuid)) {
            return this.users.get(uuid).getOptions();
        }
        return new HashMap<>();
    }

    @Override
    public void getOption(UUID uuid, Option option, Consumer<Boolean> consumer) {
        consumer.accept(getOptions(uuid).getOrDefault(option, false));
    }

    @Override
    public List<CooldownDTO> getCooldowns(UUID uniqueId) {
        User user = loadUser(uniqueId);
        if (user == null) return new ArrayList<>();
        return user.getCooldowns().entrySet().stream().map(entry -> new CooldownDTO(entry.getKey(), entry.getValue(), new Date())).toList();
    }

    @Override
    public void setPowerTools(UUID uniqueId, Material type, String command) {
        this.saveFileAsync(uniqueId);
    }

    @Override
    public void deletePowerTools(UUID uniqueId, Material material) {
        this.saveFileAsync(uniqueId);
    }

    @Override
    public void addMailBoxItem(MailBoxItem mailBoxItem) {
        mailBoxItem.setId((int) state().nextMailBoxId++);
        updateUser(mailBoxItem.getUniqueId(), user -> user.getMailBoxItems().add(mailBoxItem));
        saveState();
    }

    @Override
    public void clearMailBox(UUID uuid) {
        updateUser(uuid, user -> user.getMailBoxItems().clear());
    }

    @Override
    public void removeMailBoxItem(int id) {
        for (User user : getAllUsers()) {
            if (user.getMailBoxItems().removeIf(item -> item.getId() == id)) {
                saveUser(user);
            }
        }
    }

    @Override
    public void addMailMessage(MailMessage mailMessage) {
        UUID uniqueId = mailMessage.getUniqueId();

        User user = this.users.get(uniqueId);
        if (user != null) {
            // The message was already added to the online user, only the file has to be written
            this.saveFileAsync(uniqueId);
            return;
        }

        // The receiver is offline, the file is loaded, updated and saved without caching the user
        async(() -> updateOfflineUser(uniqueId, offlineUser -> offlineUser.addMailMessage(mailMessage)));
    }

    @Override
    public void markMailMessagesAsRead(UUID uniqueId) {
        if (this.users.containsKey(uniqueId)) {
            this.saveFileAsync(uniqueId);
            return;
        }
        async(() -> updateOfflineUser(uniqueId, offlineUser -> offlineUser.getMailMessages().forEach(mailMessage -> mailMessage.setRead(true))));
    }

    @Override
    public void clearMailMessages(UUID uniqueId) {
        if (this.users.containsKey(uniqueId)) {
            this.saveFileAsync(uniqueId);
            return;
        }
        async(() -> updateOfflineUser(uniqueId, offlineUser -> offlineUser.getMailMessages().clear()));
    }

    @Override
    public List<MailMessageDTO> getMailMessages(UUID uniqueId) {

        User user = this.users.get(uniqueId);
        if (user == null) {
            user = this.plugin.getPersist().load(User.class, getUserFile(uniqueId));
            if (user == null) return new ArrayList<>();
        }

        return user.getMailMessages().stream().map(mailMessage -> new MailMessageDTO(mailMessage.getId(), mailMessage.getUniqueId(), mailMessage.getSenderId(), mailMessage.getSenderName(), mailMessage.getContent(), mailMessage.isRead(), mailMessage.getCreatedAt())).toList();
    }

    /**
     * Loads the file of an offline user, applies the given change and saves the file back.
     * The user is never put in the online users cache.
     *
     * @param uniqueId the UUID of the offline user
     * @param consumer the change to apply
     */
    private void updateOfflineUser(UUID uniqueId, Consumer<User> consumer) {
        File file = getUserFile(uniqueId);
        Persist persist = this.plugin.getPersist();

        User offlineUser = persist.load(User.class, file);
        if (offlineUser == null) return; // The player never joined the server

        consumer.accept(offlineUser);
        persist.save(offlineUser, file);
    }

    @Override
    public List<UserEconomyRankingDTO> getEconomyRanking(Economy economy) {
        return getAllUsers().stream()
                .map(user -> new UserEconomyRankingDTO(user.getUniqueId(), user.getName(), user.getBalance(economy)))
                .sorted(Comparator.comparing(UserEconomyRankingDTO::amount).reversed())
                .toList();
    }

    @Override
    public List<MailBoxDTO> getMailBox(UUID uuid) {
        User user = loadUser(uuid);
        if (user == null) return new ArrayList<>();
        return user.getMailBoxItems().stream().map(item -> new MailBoxDTO(item.getId(), item.getUniqueId(), ItemStackUtils.serializeItemStack(item.getItemStack()), item.getExpiredAt(), new Date())).toList();
    }

    @Override
    public void fetchOfflinePlayerEconomies(Consumer<List<UserEconomyDTO>> consumer) {
        consumer.accept(getAllUsers().stream().flatMap(user -> user.getBalances().entrySet().stream().map(entry -> new UserEconomyDTO(user.getUniqueId(), entry.getKey(), entry.getValue()))).toList());
    }

    @Override
    public void setVote(UUID uuid, long vote, long offline) {
        updateUser(uuid, user -> user.setWithDTO(new UserDTO(user.getUniqueId(), user.getName(), locationAsString(user.getLastLocation()), user.getActiveBanId() == 0 ? null : user.getActiveBanId(), user.getActiveMuteId() == 0 ? null : user.getActiveMuteId(), user.getPlayTime(), new Date(), new Date(), vote, offline, user.isFrozen(), user.getFlySeconds(), user.getPlayerTime(), user.getPlayerWeather())));
    }

    @Override
    public UserVoteDTO getVote(UUID uniqueId) {
        User user = loadUser(uniqueId);
        return user == null ? new UserVoteDTO(uniqueId, 0, 0) : new UserVoteDTO(uniqueId, user.getVote(), user.getOfflineVotes());
    }

    @Override
    public void updateServerStorage(String key, Object object) {
        upsertStorage(key, object);
    }

    @Override
    public void setLastVote(UUID uniqueId, String site) {
        saveUser(uniqueId);
    }

    @Override
    public void resetVotes() {
        for (User user : getAllUsers()) {
            user.setWithDTO(new UserDTO(user.getUniqueId(), user.getName(), locationAsString(user.getLastLocation()), user.getActiveBanId() == 0 ? null : user.getActiveBanId(), user.getActiveMuteId() == 0 ? null : user.getActiveMuteId(), user.getPlayTime(), new Date(), new Date(), 0, 0, user.isFrozen(), user.getFlySeconds(), user.getPlayerTime(), user.getPlayerWeather()));
            saveUser(user);
        }
    }

    @Override
    public void updateVaultQuantity(UUID uniqueId, int vaultId, int slot, long quantity) {
        state().vaultItems.stream()
                .filter(vaultItemDTO -> vaultItemDTO.unique_id().equals(uniqueId) && vaultItemDTO.vault_id() == vaultId && vaultItemDTO.slot() == slot)
                .findFirst()
                .ifPresent(vaultItemDTO -> {
                    state().vaultItems.remove(vaultItemDTO);
                    state().vaultItems.add(new VaultItemDTO(uniqueId, vaultId, slot, vaultItemDTO.item(), quantity));
                    saveState();
                });
    }

    @Override
    public void removeVaultItem(UUID uniqueId, int vaultId, int slot) {
        if (state().vaultItems.removeIf(vaultItemDTO -> vaultItemDTO.unique_id().equals(uniqueId) && vaultItemDTO.vault_id() == vaultId && vaultItemDTO.slot() == slot)) {
            saveState();
        }
    }

    @Override
    public void createVaultItem(UUID uniqueId, int vaultId, int slot, long quantity, String item) {
        state().vaultItems.removeIf(vaultItemDTO -> vaultItemDTO.unique_id().equals(uniqueId) && vaultItemDTO.vault_id() == vaultId && vaultItemDTO.slot() == slot);
        state().vaultItems.add(new VaultItemDTO(uniqueId, vaultId, slot, item, quantity));
        saveState();
    }

    @Override
    public Optional<VaultItemDTO> getVaultItem(UUID uniqueId, int vaultId, int slot) {
        return state().vaultItems.stream().filter(vaultItemDTO -> vaultItemDTO.unique_id().equals(uniqueId) && vaultItemDTO.vault_id() == vaultId && vaultItemDTO.slot() == slot).findFirst();
    }

    @Override
    public boolean forceRemoveVaultItem(UUID uniqueId, int vaultId, int slot) {
        boolean removed = state().vaultItems.removeIf(vaultItemDTO -> vaultItemDTO.unique_id().equals(uniqueId) && vaultItemDTO.vault_id() == vaultId && vaultItemDTO.slot() == slot);
        if (removed) saveState();
        return removed;
    }

    @Override
    public void setVaultSlot(UUID uniqueId, int slots) {
        upsertPlayerSlot(uniqueId, slots);
    }

    @Override
    public List<VaultDTO> getVaults() {
        return state().vaults;
    }

    @Override
    public List<VaultItemDTO> getVaultItems() {
        return state().vaultItems;
    }

    @Override
    public List<PlayerSlotDTO> getPlayerVaultSlots() {
        return state().playerSlots;
    }

    @Override
    public void updateVault(UUID uniqueId, Vault vault) {
        upsertVaultRecord(uniqueId, vault.getVaultId(), vault.getName(), vault.getIconItemStack() == null ? null : ItemStackUtils.serializeItemStack(vault.getIconItemStack()));
    }

    @Override
    public void updateUserFrozen(UUID uuid, boolean frozen) {
        this.saveFileAsync(uuid);
    }

    @Override
    public void upsertFlySeconds(UUID uniqueId, long flySeconds) {
        saveUser(uniqueId);
    }

    @Override
    public long getFlySeconds(UUID uniqueId) {
        User user = loadUser(uniqueId);
        return user == null ? 0 : user.getFlySeconds();
    }

    @Override
    public void updatePlayerTimeWeather(UUID uniqueId, long playerTime, String playerWeather) {
        this.saveFileAsync(uniqueId);
    }

    @Override
    public void deleteWorldData(String worldName) {
        for (User user : getAllUsers()) {
            boolean changed = false;
            if (user.getLastLocation() != null && user.getLastLocation().getWorld() != null && worldName.equalsIgnoreCase(user.getLastLocation().getWorld().getName())) {
                user.setLastLocation(null);
                changed = true;
            }
            if (user.getHomes().removeIf(home -> home.getLocation() != null && home.getLocation().getWorld() != null && worldName.equalsIgnoreCase(home.getLocation().getWorld().getName()))) {
                changed = true;
            }
            if (changed) saveUser(user);
        }
    }

    @Override
    public void linkDiscordAccount(UUID uniqueId, String minecraftName, String discordName, long userId) {
        JsonStorageState backend = state();
        backend.discordAccounts.removeIf(discordAccountDTO -> discordAccountDTO.unique_id().equals(uniqueId));
        backend.discordAccounts.add(new DiscordAccountDTO(userId, uniqueId, minecraftName, discordName, new java.sql.Timestamp(System.currentTimeMillis())));
        saveState();
    }

    @Override
    public Optional<DiscordAccountDTO> selectDiscordAccount(UUID uniqueId) {
        return state().discordAccounts.stream().filter(discordAccountDTO -> discordAccountDTO.unique_id().equals(uniqueId)).findFirst();
    }

    @Override
    public Optional<DiscordCodeDTO> selectCode(String code) {
        return state().discordCodes.stream().filter(discordCodeDTO -> discordCodeDTO.code().equals(code)).findFirst();
    }

    @Override
    public void clearCode(DiscordCodeDTO code) {
        if (state().discordCodes.removeIf(discordCodeDTO -> discordCodeDTO.code().equals(code.code()))) {
            saveState();
        }
    }

    @Override
    public void insertDiscordLog(DiscordAction action, UUID uniqueId, String minecraftName, String discordName, long userId, String data) {
        state().discordLogs.add(new DiscordLogEntry(action, uniqueId, minecraftName, discordName, userId, data, new Date()));
        saveState();
    }

    @Override
    public void unlinkDiscordAccount(UUID uniqueId) {
        if (state().discordAccounts.removeIf(discordAccountDTO -> discordAccountDTO.unique_id().equals(uniqueId))) {
            saveState();
        }
    }

    @Override
    public StepDTO selectStep(UUID uniqueId, Step step) {
        return state().steps.stream().filter(stepDTO -> stepDTO.unique_id().equals(uniqueId) && stepDTO.step_name().equalsIgnoreCase(step.name())).findFirst().orElse(null);
    }

    @Override
    public void createStep(UUID uniqueId, Step step, long playTime) {
        JsonStorageState backend = state();
        backend.steps.removeIf(stepDTO -> stepDTO.unique_id().equals(uniqueId) && stepDTO.step_name().equalsIgnoreCase(step.name()));
        backend.steps.add(new StepDTO(uniqueId, step.name(), null, new Date(), new Date(), playTime, 0, 0, null));
        saveState();
    }

    @Override
    public void finishStep(UUID uniqueId, Step step, String data, long playTimeEnd, long playTimeBetween) {
        JsonStorageState backend = state();
        backend.steps.stream()
                .filter(stepDTO -> stepDTO.unique_id().equals(uniqueId) && stepDTO.step_name().equalsIgnoreCase(step.name()))
                .findFirst()
                .ifPresent(stepDTO -> {
                    backend.steps.remove(stepDTO);
                    backend.steps.add(new StepDTO(uniqueId, step.name(), data, stepDTO.created_at(), new Date(), stepDTO.play_time_start(), playTimeEnd, playTimeBetween, new Date()));
                    saveState();
                });
    }

    @Override
    public List<String> getPlayerNames() {
        return getAllUsers().stream().map(User::getName).filter(name -> name != null && !name.isBlank()).distinct().sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }

    private static class JsonStorageState {
        private List<EconomyTransactionDTO> transactions = new ArrayList<>();
        private List<CommandDTO> commands = new ArrayList<>();
        private List<ChatMessageDTO> chatMessages = new ArrayList<>();
        private List<PrivateMessageDTO> privateMessages = new ArrayList<>();
        private List<PlayTimeDTO> playTimes = new ArrayList<>();
        private List<SanctionDTO> sanctions = new ArrayList<>();
        private List<MailBoxDTO> mailBoxes = new ArrayList<>();
        private List<UserVoteDTO> votes = new ArrayList<>();
        private List<VaultDTO> vaults = new ArrayList<>();
        private List<VaultItemDTO> vaultItems = new ArrayList<>();
        private List<PlayerSlotDTO> playerSlots = new ArrayList<>();
        private List<DiscordAccountDTO> discordAccounts = new ArrayList<>();
        private List<DiscordCodeDTO> discordCodes = new ArrayList<>();
        private List<DiscordLogEntry> discordLogs = new ArrayList<>();
        private List<StepDTO> steps = new ArrayList<>();
        private Map<String, String> serverStorage = new HashMap<>();
        private long nextSanctionId = 1;
        private long nextMailBoxId = 1;
    }

    private record DiscordLogEntry(DiscordAction action, UUID uniqueId, String minecraftName, String discordName, long userId, String data, Date createdAt) {
    }
}
