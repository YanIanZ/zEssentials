package dev.yanianz.essentials.nicknames;

import dev.yanianz.essentials.disguise.DisguiseData;
import dev.yanianz.essentials.disguise.SkinFetcher;
import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.commands.CommandResultType;
import fr.maxlego08.essentials.api.commands.Permission;
import fr.maxlego08.essentials.api.messages.Message;
import fr.maxlego08.essentials.zutils.utils.commands.VCommand;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Hypixel SkyBlock style nickname: /nick gives a random anonymous identity
 * (random username + matching skin), /nick clear removes it.
 */
public class CommandNick extends VCommand {

    public CommandNick(EssentialsPlugin plugin) {
        super(plugin);
        this.setModule(NicknamesModule.class);
        this.setPermission(Permission.ESSENTIALS_NICKNAMES_USE);
        this.setDescription(Message.DESCRIPTION_NICK);
        this.addOptionalArg("nickname", (sender, args) -> nickCompletion(args));
        this.addOptionalArg("nickname", (sender, args) -> nickCompletionAdmin(args));
        this.setExtendedArgs(true);
        this.onlyPlayers();
    }

    private List<String> nickCompletion(String[] args) {
        List<String> out = new ArrayList<>();
        out.add("clear");
        out.add("off");
        out.addAll(plugin.getEssentialsServer().getVisiblePlayerNames(this.sender));
        return out;
    }

    private List<String> nickCompletionAdmin(String[] args) {
        return plugin.getEssentialsServer().getVisiblePlayerNames(this.sender);
    }

    @Override
    protected CommandResultType perform(EssentialsPlugin plugin) {

        NicknamesModule module = plugin.getModuleManager().getModule(NicknamesModule.class);

        Player target = this.player;
        String nickname = null;
        boolean randomNick = false;

        if (this.args.length == 0) {
            randomNick = true;
        } else if (this.args.length >= 2) {
            if (!hasPermission(sender, Permission.ESSENTIALS_NICKNAMES_OTHER)) {
                return CommandResultType.NO_PERMISSION;
            }
            Player possibleTarget = Bukkit.getPlayerExact(this.argAsString(0));
            if (possibleTarget != null) {
                target = possibleTarget;
                String second = this.args[1];
                if (second.equalsIgnoreCase("clear") || second.equalsIgnoreCase("off")) {
                    nickname = null;
                } else if (second.equalsIgnoreCase("random")) {
                    randomNick = true;
                } else {
                    nickname = String.join(" ", java.util.Arrays.copyOfRange(this.args, 1, this.args.length));
                }
            } else {
                nickname = String.join(" ", this.args);
            }
        } else {
            String argument = this.argAsString(0);
            if (argument.equalsIgnoreCase("clear") || argument.equalsIgnoreCase("off")) {
                nickname = null;
            } else if (argument.equalsIgnoreCase("random")) {
                randomNick = true;
            } else if (Bukkit.getPlayerExact(argument) != null && !argument.equalsIgnoreCase(target.getName())
                    && hasPermission(sender, Permission.ESSENTIALS_NICKNAMES_OTHER)) {
                target = Bukkit.getPlayerExact(argument);
                randomNick = true;
            } else {
                nickname = argument;
            }
        }

        UUID targetUuid = target.getUniqueId();

        if (!randomNick && nickname == null) {
            module.removeDisguise(targetUuid);
            module.setNickname(targetUuid, null);
            if (target == this.player || sender.equals(target)) {
                message(sender, Message.NICK_REMOVED, "%player%", Message.YOU.getMessageAsString());
            } else {
                message(sender, Message.NICK_REMOVED, "%player%", target.getName());
            }
            return CommandResultType.SUCCESS;
        }

        if (target != this.player && !hasPermission(sender, Permission.ESSENTIALS_NICKNAMES_OTHER)) {
            return CommandResultType.NO_PERMISSION;
        }

        if (randomNick) {
            List<String> pool = module.getRandomNickPool();
            if (pool.isEmpty()) {
                message(sender, Message.NICK_RANDOM_EMPTY);
                return CommandResultType.SUCCESS;
            }
            String randomName = pool.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(pool.size()));
            message(sender, Message.NICK_RANDOMIZING);
            fetchAndApplyNick(module, target, randomName, target.equals(this.player));
            return CommandResultType.SUCCESS;
        }

        NicknamesModule.NickError error = module.validate(target, nickname);
        if (error != null) {
            Message errorMessage = switch (error) {
                case TOO_LONG -> Message.NICK_TOO_LONG;
                case INVALID_CHARACTERS -> Message.NICK_INVALID_CHARACTERS;
                case COLORS_NOT_ALLOWED -> Message.NICK_COLORS_NOT_ALLOWED;
                case IMPERSONATION -> Message.NICK_IMPERSONATION;
            };
            message(sender, errorMessage,
                    "%max%", String.valueOf(module.maxLengthValue()),
                    "%nickname%", nickname);
            return CommandResultType.SUCCESS;
        }

        boolean selfChange = target.equals(this.player) || sender.equals(target);
        boolean cooldownBypass = hasPermission(sender, Permission.ESSENTIALS_NICKNAMES_BYPASS_COOLDOWN);

        if (selfChange && module.isOnCooldown(targetUuid) && !cooldownBypass) {
            message(sender, Message.NICK_COOLDOWN, "%seconds%", String.valueOf(module.getRemainingCooldown(targetUuid)));
            return CommandResultType.SUCCESS;
        }

        message(sender, Message.NICK_RANDOMIZING);
        fetchAndApplyNick(module, target, nickname, selfChange);
        return CommandResultType.SUCCESS;
    }

    /**
     * Hypixel style: a nick is a full identity — the name AND the matching
     * skin of that username, applied through the disguise system so it
     * shows everywhere (chat, tab, name tag, packets).
     */
    private void fetchAndApplyNick(NicknamesModule module, Player target, String nickName, boolean self) {
        Player online = Bukkit.getPlayerExact(nickName);
        String textureValue = null;
        String textureSignature = null;

        if (online != null) {
            for (var property : online.getPlayerProfile().getProperties()) {
                if ("textures".equals(property.getName())) {
                    textureValue = property.getValue();
                    textureSignature = property.getSignature();
                    break;
                }
            }
        }

        String finalTextureValue = textureValue;
        String finalTextureSignature = textureSignature;

        CompletableFuture.supplyAsync(() -> {
            try {
                if (finalTextureValue != null) return new String[]{finalTextureValue, finalTextureSignature};
                UUID uuid = SkinFetcher.fetchUuidFromName(nickName);
                if (uuid == null) return null;
                dev.yanianz.essentials.disguise.SkinCache.CachedProfile cached = module.getSkinCache().getCached(uuid);
                if (cached != null) return new String[]{cached.textureValue(), cached.textureSignature()};
                String[] textures = SkinFetcher.fetchTexturesFromUuid(uuid);
                if (textures == null) return null;
                module.getSkinCache().put(uuid, nickName, textures[0], textures[1]);
                return textures;
            } catch (Exception e) {
                return null;
            }
        }).thenAccept(textures -> {
            this.plugin.getScheduler().runNextTick(w -> {
                if (textures == null) {
                    if (self) {
                        message(sender, Message.NICK_FETCH_FAILED, "%player%", nickName);
                    }
                    return;
                }
                DisguiseData data = new DisguiseData();
                data.setDisguiseName(nickName);
                data.setTextureValue(textures[0]);
                data.setTextureSignature(textures[1]);
                module.applyDisguise(target, data);
                module.setNickname(targetUuidQuiet(target), null);

                if (self) {
                    module.markChanged(target.getUniqueId());
                    message(sender, Message.NICK_HYPIXEL_SET, "%nickname%", nickName);
                } else {
                    message(sender, Message.NICK_HYPIXEL_SET_OTHER, "%player%", target.getName(), "%nickname%", nickName);
                    message(target, Message.NICK_HYPIXEL_SET, "%nickname%", nickName);
                }
            });
        });
    }

    private UUID targetUuidQuiet(Player target) {
        return target.getUniqueId();
    }
}
