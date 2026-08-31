package fr.maxlego08.essentials.commands;

import fr.maxlego08.essentials.ZEssentialsPlugin;
import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.commands.CommandManager;
import dev.yanianz.essentials.disguise.CommandDisguise;
import dev.yanianz.essentials.disguise.CommandUnDisguise;
import dev.yanianz.essentials.disguise.CommandRealname;
import dev.yanianz.essentials.nicknames.CommandNick;
import dev.yanianz.essentials.networkchat.CommandGlobalChat;
import dev.yanianz.essentials.friends.CommandFriend;
import dev.yanianz.essentials.guild.CommandGuild;
import dev.yanianz.essentials.guild.CommandGuildChat;
import dev.yanianz.essentials.party.CommandParty;
import dev.yanianz.essentials.party.CommandPartyChat;
import dev.yanianz.essentials.reports.CommandReport;
import dev.yanianz.essentials.reports.CommandReports;
import dev.yanianz.essentials.notes.CommandNotes;
import dev.yanianz.essentials.polls.CommandPoll;
import dev.yanianz.essentials.pricing.CommandPricing;
import dev.yanianz.essentials.reputation.CommandRepGive;
import dev.yanianz.essentials.reputation.CommandReputationView;
import fr.maxlego08.essentials.commands.commands.chat.*;
import fr.maxlego08.essentials.commands.commands.clearinventory.ClearInventoryCommand;
import fr.maxlego08.essentials.commands.commands.cooldown.CommandCooldown;
import fr.maxlego08.essentials.commands.commands.deathmessage.CommandDeathMessageToggle;
import fr.maxlego08.essentials.commands.commands.discord.CommandLink;
import fr.maxlego08.essentials.commands.commands.discord.CommandUnLink;
import fr.maxlego08.essentials.commands.commands.economy.*;
import fr.maxlego08.essentials.commands.commands.enderchest.CommandEnderChest;
import fr.maxlego08.essentials.commands.commands.enderchest.CommandEnderSee;
import fr.maxlego08.essentials.commands.commands.fly.CommandFly;
import fr.maxlego08.essentials.commands.commands.gamemode.*;
import fr.maxlego08.essentials.commands.commands.hologram.CommandHologram;
import fr.maxlego08.essentials.commands.commands.home.*;
import fr.maxlego08.essentials.commands.commands.items.*;
import fr.maxlego08.essentials.commands.commands.kits.*;
import fr.maxlego08.essentials.commands.commands.mail.CommandMail;
import fr.maxlego08.essentials.commands.commands.messages.*;
import fr.maxlego08.essentials.commands.commands.sanction.*;
import fr.maxlego08.essentials.commands.commands.scoreboard.CommandScoreboard;
import fr.maxlego08.essentials.commands.commands.spawn.CommandFirstSpawn;
import fr.maxlego08.essentials.commands.commands.spawn.CommandSetFirstSpawn;
import fr.maxlego08.essentials.commands.commands.spawn.CommandSetSpawn;
import fr.maxlego08.essentials.commands.commands.spawn.CommandSpawn;
import fr.maxlego08.essentials.commands.commands.step.CommandStep;
import fr.maxlego08.essentials.commands.commands.terms.CommandTerms;
import fr.maxlego08.essentials.commands.commands.teleport.*;
import fr.maxlego08.essentials.commands.commands.teleport.random.CommandTeleportRandom;
import fr.maxlego08.essentials.commands.commands.utils.*;
import fr.maxlego08.essentials.commands.commands.utils.admins.*;
import fr.maxlego08.essentials.commands.commands.utils.blocks.*;
import fr.maxlego08.essentials.commands.commands.utils.experience.CommandExperience;
import fr.maxlego08.essentials.commands.commands.utils.lag.CommandLag;
import fr.maxlego08.essentials.commands.commands.vault.CommandVault;
import fr.maxlego08.essentials.commands.commands.vote.CommandVote;
import fr.maxlego08.essentials.commands.commands.vote.CommandVoteParty;
import fr.maxlego08.essentials.commands.commands.warp.CommandDelWarp;
import fr.maxlego08.essentials.commands.commands.warp.CommandSetWarp;
import fr.maxlego08.essentials.commands.commands.warp.CommandWarp;
import fr.maxlego08.essentials.commands.commands.warp.CommandWarps;
import fr.maxlego08.essentials.commands.commands.weather.*;
import fr.maxlego08.essentials.commands.commands.worldedit.CommandWorldEdit;
import fr.maxlego08.essentials.zutils.utils.commands.VCommand;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandLoader {

    private final ZEssentialsPlugin plugin;
    private final List<RegisterCommand> commands = new ArrayList<>();

    public CommandLoader(ZEssentialsPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadCommands(CommandManager commandManager) {

        register("gamemode", CommandGameMode.class, "gm");
        register("gmc", CommandGameModeCreative.class, "creat");
        register("gma", CommandGameModeAdventure.class, "advent");
        register("gms", CommandGameModeSurvival.class, "survi");
        register("gmsp", CommandGameModeSpectator.class, "spec");

        register("day", CommandDay.class);
        register("night", CommandNight.class);
        register("sun", CommandSun.class);
        register("player-weather", CommandPlayerWeather.class, "pweather");
        register("player-time", CommandPlayerTime.class, "ptime");

        register("enderchest", CommandEnderChest.class, "ec");
        register("endersee", CommandEnderSee.class, "ecsee");

        register("top", CommandTop.class);
        register("bottom", CommandBottom.class);
        register("speed", CommandSpeed.class);
        register("walkspeed", CommandWalkSpeed.class, "wspeed");
        register("flyspeed", CommandFlySpeed.class, "fspeed");
        register("god", CommandGod.class);
        register("vanish", CommandVanish.class, "v");
        register("heal", CommandHeal.class);
        register("lightning", CommandLightning.class, "strike");
        register("more", CommandMore.class);
        register("worldtp", CommandTeleportWorld.class, "wtp");
        register("trash", CommandTrash.class, "poubelle");
        register("condense", CommandCondense.class);
        register("feed", CommandFeed.class, "eat");
        register("craft", CommandCraft.class);
        register("enchanting", CommandEnchanting.class);
        register("invsee", CommandInvsee.class);
        register("clearinventory", ClearInventoryCommand.class, "clear", "ci");
        register("afk", CommandAfk.class);
        register("compact", CommandCompact.class, "blocks");
        register("compactall", CommandCompactAll.class, "blocksall", "condenseall");
        register("hat", CommandHat.class);
        register("fly", CommandFly.class);
        register("anvil", CommandAnvil.class);
        register("cartographytable", CommandCartographyTable.class);
        register("grindstone", CommandGrindStone.class);
        register("loom", CommandLoom.class);
        register("stonecutter", CommandStoneCutter.class);
        register("smithingtable", CommandSmithingTable.class);
        register("furnace", CommandFurnace.class, "burn");
        register("skull", CommandSkull.class);
        register("rules", CommandRules.class, "?", "help", "aide");
        register("terms", CommandTerms.class);

        register("tp", CommandTeleport.class);
        register("tpall", CommandTeleportAll.class);
        register("tphere", CommandTeleportHere.class, "s");
        register("tpa", CommandTeleportTo.class);
        register("tpahere", CommandTeleportToHere.class);
        register("tpaall", CommandTpaAll.class);
        register("tpaccept", CommandTeleportAccept.class, "tpyes");
        register("tpdeny", CommandTeleportDeny.class, "tpno");
        register("tpacancel", CommandTeleportCancel.class);
        register("back", CommandTeleportBack.class);
        register("tpr", CommandTeleportRandom.class, "rtp");
        register("g", CommandGlobalChat.class, "globalchat");
        register("friend", CommandFriend.class, "friends");
        register("guild", CommandGuild.class, "g");
        register("gc", CommandGuildChat.class, "guildchat");
        register("party", CommandParty.class);
        register("pc", CommandPartyChat.class, "partychat");

        register("balancetop", CommandBalanceTop.class, "baltop");
        register("economy", CommandEconomy.class, "eco");
        register("money", CommandMoney.class, "balance");
        register("pay", CommandPay.class);
        register("paytoggle", CommandPayToggle.class);
        register("paynotificationtoggle", CommandPayNotificationToggle.class, "paynotify");

        register("setfirstspawn", CommandSetFirstSpawn.class);
        register("setspawn", CommandSetSpawn.class);
        register("spawn", CommandSpawn.class);
        register("firstspawn", CommandFirstSpawn.class);

        register("setwarp", CommandSetWarp.class, "wcreate");
        register("warp", CommandWarp.class, "w");
        register("warps", CommandWarps.class, "wlist");
        register("delwarp", CommandDelWarp.class, "wdelete");

        register("sethome", CommandSetHome.class, "hcreate", "hc");
        register("sethomeconfirm", CommandSetHomeConfirm.class);
        register("delhomeconfirm", CommandDelHomeConfirm.class);
        register("delhome", CommandDelHome.class, "hdelete", "hd");
        register("home", CommandHome.class, "h", "homes");
        register("home-list", CommandHomeList.class, "hlist");
        register("delhome-other", CommandDelHomeOther.class, "delhomeother", "hdelother");
        register("homepublic", CommandHomePublic.class, "publichome");
        register("publichomes", CommandPublicHomes.class, "phomes");
        register("homeshare", CommandHomeShare.class, "hshare");
        register("homeunshare", CommandHomeUnshare.class, "hunshare");
        register("homeshares", CommandHomeShares.class, "hshares");
        register("homecategory", CommandHomeCategory.class, "homecat", "hcat");
        register("homefavorite", CommandHomeFavorite.class, "homefav", "hfav");
        register("homeimport", CommandHomeImport.class);

        register("freeze", CommandFreeze.class);
        register("unfreeze", CommandUnfreeze.class);
        register("warn", CommandWarn.class);
        register("warnings", CommandWarnings.class);
        register("note", CommandNotes.class);
        register("notes", CommandNotes.class, "notelist");
        register("nick", CommandNick.class);
        register("disguise", CommandDisguise.class);
        register("undisguise", CommandUnDisguise.class);
        register("realname", CommandRealname.class);
        register("report", CommandReport.class);
        register("reports", CommandReports.class);

        register("chatcolor", dev.yanianz.essentials.chatcustomization.CommandChatColor.class);
        register("tags", dev.yanianz.essentials.chatcustomization.CommandTags.class);
        register("ban", CommandBan.class);
        register("mute", CommandMute.class);
        register("unmute", CommandUnMute.class);
        register("unban", CommandUnBan.class);
        register("kick", CommandKick.class);
        register("kickall", CommandKickAll.class);
        register("sanction", CommandSanction.class, "sc");

        register("kittycannon", CommandKittyCannon.class);

        register("chathistory", CommandChatHistory.class, "ct");
        register("chatclear", CommandChatClear.class, "cl");
        register("chatenable", CommandChatEnable.class, "ce");
        register("chatdisable", CommandChatDisable.class, "cd");
        register("broadcast", CommandChatBroadcast.class, "bc");
        register("dnd", CommandChatDnd.class);
        register("chatslowmode", CommandChatSlowmode.class);
        register("chatgames", CommandChatGames.class);
        register("poll", CommandPoll.class);

        register("baltopgui", dev.yanianz.essentials.screens.CommandBaltopGui.class);
        register("warpgui", dev.yanianz.essentials.screens.CommandWarpsGui.class);
        register("homesgui", dev.yanianz.essentials.screens.CommandHomesGui.class);
        register("kitsgui", dev.yanianz.essentials.screens.CommandKitsGui.class);
        register("rep", CommandRepGive.class);
        register("reputation", CommandReputationView.class);
        register("showitem", CommandShowItem.class);
        register("pingsound", CommandPingSound.class, "pingsounds");

        register("message", CommandMessage.class, "msg", "tell", "whisper", "m", "w");
        register("reply", CommandReply.class, "r");
        register("messagetoggle", CommandMessageToggle.class, "msgtoggle", "mtg");
        register("tptoggle", CommandTpToggle.class);
        register("tpaheretoggle", CommandTpaHereToggle.class);
        register("socialspy", CommandSocialSpy.class);
        register("ignore", CommandIgnore.class);
        register("unignore", CommandUnIgnore.class, "unignor");
        register("ignorelist", CommandIgnoreList.class, "ignores");

        register("repair", CommandRepair.class, "fix");
        register("repairall", CommandRepairAll.class, "fixall");
        register("ext", CommandExt.class);
        register("near", CommandNear.class);
        register("eat", CommandEat.class);
        register("xyz", CommandXyz.class);
        register("list", CommandList.class);
        register("playtime", CommandPlayTime.class);
        register("essversion", CommandVersion.class, "ev");
        register("killall", CommandKillAll.class);
        register("lag", CommandLag.class);
        register("seen", CommandSeen.class, "whois");
        register("seenip", CommandSeenIp.class, "whoisip");
        register("enchant", CommandEnchant.class, "enchantment");
        register("nightvision", CommandNightVision.class, "nv");
        register("phantoms", CommandPhantoms.class);
        register("sudo", CommandSudo.class, "su");

        register("kit", CommandKit.class, "kits");
        register("showkit", CommandShowKit.class);
        register("kiteditor", CommandKitEditor.class, "keditor");
        register("kitcreate", CommandKitCreate.class, "kcreate");
        register("kitdelete", CommandKitDelete.class, "kdelete");
        register("kitgive", CommandKitGive.class, "kgive");

        register("cooldown", CommandCooldown.class);
        register("itemname", CommandItemName.class, "iname", "itemrename", "irename");
        register("itemglow", CommandItemGlow.class, "iglow");
        register("itemunbreakable", CommandItemUnbreakable.class, "iunbreakable");
        register("itemmodeldata", CommandItemModelData.class, "imodeldata", "imodel");
        register("itemdb", CommandItemDb.class);
        register("itemlore", CommandItemLore.class, "ilore", "itemlore", "lore");
        register("mail", CommandMail.class, "mailbox", "mb");
        register("give", CommandGive.class);
        register("giveall", CommandGiveAll.class);
        register("powertools", CommandPowerTools.class, "pt");
        register("powertools-toggle", CommandPowerToolsToggle.class, "pt-toggle");

        register("experience", CommandExperience.class, "xp", "exp", "level", "levels");

        register("hologram", CommandHologram.class, "holo", "ho");
        register("sb", CommandScoreboard.class);

        register("voteparty", CommandVoteParty.class, "vp");
        register("vote", CommandVote.class);
        register("vault", CommandVault.class, "sac", "bag", "b", "coffre", "chest");
        register("player-worldedit", CommandWorldEdit.class, "pwe", "ess-worldedit", "eworldedit", "ew");

        register("suicide", CommandSuicide.class);
        register("link", CommandLink.class, "lier");
        register("unlink", CommandUnLink.class, "delier");

        register("pub", CommandPub.class);
        register("step", CommandStep.class);
        register("itemframe", CommandItemFrame.class, "iframe");
        register("deathmessage", CommandDeathMessageToggle.class, "dm", "deathmsg");
        register("pricing", CommandPricing.class);
        register("stash", dev.yanianz.essentials.stash.CommandStash.class, "itemstash", "materialstash");

        for (RegisterCommand registerCommand : this.commands) {
            try {
                commandManager.registerCommand(this.plugin, registerCommand.command, registerCommand.commandClass.getConstructor(EssentialsPlugin.class).newInstance(this.plugin), registerCommand.aliases);
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }

        commandManager.saveCommands();
    }

    private void register(String command, Class<? extends VCommand> commandClass, String... aliases) {
        this.commands.add(new RegisterCommand(command, commandClass, Arrays.asList(aliases)));
    }

    public record RegisterCommand(String command, Class<? extends VCommand> commandClass, List<String> aliases) {

    }
}