package fr.maxlego08.essentials.storage.mongodb;

import com.mongodb.client.MongoDatabase;
import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.storage.mongodb.repos.MongoUserRepository;
import fr.maxlego08.essentials.storage.mongodb.repos.MongoUserEconomyRepository;
import fr.maxlego08.essentials.storage.mongodb.repos.MongoUserHomeRepository;
import fr.maxlego08.essentials.storage.mongodb.repos.MongoUserHomeShareRepository;
import fr.maxlego08.essentials.storage.mongodb.repos.MongoUserSanctionRepository;
import fr.maxlego08.essentials.storage.mongodb.repos.MongoUserCooldownsRepository;
import fr.maxlego08.essentials.storage.mongodb.repos.MongoUserIgnoreRepository;
import fr.maxlego08.essentials.storage.mongodb.repos.MongoUserOptionRepository;
import fr.maxlego08.essentials.storage.mongodb.repos.MongoUserMailBoxRepository;
import fr.maxlego08.essentials.storage.mongodb.repos.MongoMailMessageRepository;
import fr.maxlego08.essentials.storage.mongodb.repos.MongoEconomyTransactionsRepository;
import fr.maxlego08.essentials.storage.mongodb.repos.MongoChatMessagesRepository;
import fr.maxlego08.essentials.storage.mongodb.repos.MongoPrivateMessagesRepository;
import fr.maxlego08.essentials.storage.mongodb.repos.MongoCommandsRepository;
import fr.maxlego08.essentials.storage.mongodb.repos.MongoUserPlayTimeRepository;
import fr.maxlego08.essentials.storage.mongodb.repos.MongoUserPowerToolsRepository;
import fr.maxlego08.essentials.storage.mongodb.repos.MongoServerStorageRepository;
import fr.maxlego08.essentials.storage.mongodb.repos.MongoVoteSiteRepository;
import fr.maxlego08.essentials.storage.mongodb.repos.MongoPlayerSlotRepository;
import fr.maxlego08.essentials.storage.mongodb.repos.MongoVaultItemRepository;
import fr.maxlego08.essentials.storage.mongodb.repos.MongoVaultRepository;
import fr.maxlego08.essentials.storage.mongodb.repos.MongoLinkAccountRepository;
import fr.maxlego08.essentials.storage.mongodb.repos.MongoLinkCodeRepository;
import fr.maxlego08.essentials.storage.mongodb.repos.MongoLinkHistoryRepository;
import fr.maxlego08.essentials.storage.mongodb.repos.MongoUserStepRepository;

public class MongoRepositories {

    public final MongoUserRepository users;
    public final MongoUserEconomyRepository economy;
    public final MongoUserHomeRepository homes;
    public final MongoUserHomeShareRepository homeShares;
    public final MongoUserSanctionRepository sanctions;
    public final MongoUserCooldownsRepository cooldowns;
    public final MongoUserIgnoreRepository ignores;
    public final MongoUserOptionRepository options;
    public final MongoUserMailBoxRepository mailBox;
    public final MongoMailMessageRepository mailMessages;
    public final MongoEconomyTransactionsRepository transactions;
    public final MongoChatMessagesRepository chatMessages;
    public final MongoPrivateMessagesRepository privateMessages;
    public final MongoCommandsRepository commands;
    public final MongoUserPlayTimeRepository playTime;
    public final MongoUserPowerToolsRepository powerTools;
    public final MongoServerStorageRepository serverStorage;
    public final MongoVoteSiteRepository votes;
    public final MongoPlayerSlotRepository playerSlots;
    public final MongoVaultItemRepository vaultItems;
    public final MongoVaultRepository vaults;
    public final MongoLinkAccountRepository linkAccounts;
    public final MongoLinkCodeRepository linkCodes;
    public final MongoLinkHistoryRepository linkHistory;
    public final MongoUserStepRepository steps;

    public MongoRepositories(EssentialsPlugin plugin, MongoDatabase database) {
        this.users = new MongoUserRepository(plugin, database);
        this.economy = new MongoUserEconomyRepository(plugin, database);
        this.homes = new MongoUserHomeRepository(plugin, database);
        this.homeShares = new MongoUserHomeShareRepository(plugin, database);
        this.sanctions = new MongoUserSanctionRepository(plugin, database);
        this.cooldowns = new MongoUserCooldownsRepository(plugin, database);
        this.ignores = new MongoUserIgnoreRepository(plugin, database);
        this.options = new MongoUserOptionRepository(plugin, database);
        this.mailBox = new MongoUserMailBoxRepository(plugin, database);
        this.mailMessages = new MongoMailMessageRepository(plugin, database);
        this.transactions = new MongoEconomyTransactionsRepository(plugin, database);
        this.chatMessages = new MongoChatMessagesRepository(plugin, database);
        this.privateMessages = new MongoPrivateMessagesRepository(plugin, database);
        this.commands = new MongoCommandsRepository(plugin, database);
        this.playTime = new MongoUserPlayTimeRepository(plugin, database);
        this.powerTools = new MongoUserPowerToolsRepository(plugin, database);
        this.serverStorage = new MongoServerStorageRepository(plugin, database);
        this.votes = new MongoVoteSiteRepository(plugin, database);
        this.playerSlots = new MongoPlayerSlotRepository(plugin, database);
        this.vaultItems = new MongoVaultItemRepository(plugin, database);
        this.vaults = new MongoVaultRepository(plugin, database);
        this.linkAccounts = new MongoLinkAccountRepository(plugin, database);
        this.linkCodes = new MongoLinkCodeRepository(plugin, database);
        this.linkHistory = new MongoLinkHistoryRepository(plugin, database);
        this.steps = new MongoUserStepRepository(plugin, database);
    }
}
