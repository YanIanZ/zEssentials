package dev.yanianz.essentials.disguise;

import dev.yanianz.essentials.nicknames.NicknamesModule;
import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.commands.CommandResultType;
import fr.maxlego08.essentials.api.commands.Permission;
import fr.maxlego08.essentials.api.messages.Message;
import fr.maxlego08.essentials.zutils.utils.commands.VCommand;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class CommandDisguise extends VCommand {

    public CommandDisguise(EssentialsPlugin plugin) {
        super(plugin);
        this.setModule(NicknamesModule.class);
        this.setPermission(Permission.ESSENTIALS_DISGUISE_USE);
        this.setDescription(Message.DESCRIPTION_DISGUISE);
        this.addOptionalArg("action", (sender, args) -> completions());
        this.setExtendedArgs(true);
        this.onlyPlayers();
    }

    private List<String> completions() {
        NicknamesModule module = plugin.getModuleManager().getModule(NicknamesModule.class);
        List<String> out = new ArrayList<>(List.of("off", "random", "skin", "player", "mob", "list"));
        if (module != null) {
            out.addAll(module.getAllowedMobs().stream().map(String::toLowerCase).toList());
        }
        return out;
    }

    @Override
    protected CommandResultType perform(EssentialsPlugin plugin) {

        NicknamesModule module = plugin.getModuleManager().getModule(NicknamesModule.class);
        if (module == null || !module.isDisguiseEnabled()) {
            message(sender, Message.DISGUISE_DISABLED);
            return CommandResultType.SUCCESS;
        }

        Player target = this.player;

        if (this.args.length == 0) {
            message(sender, Message.DISGUISE_USAGE);
            return CommandResultType.SUCCESS;
        }

        String firstArg = this.argAsString(0);

        if (firstArg.equalsIgnoreCase("off")) {
            if (this.args.length >= 2 && hasPermission(sender, Permission.ESSENTIALS_DISGUISE_OTHER)) {
                Player other = Bukkit.getPlayerExact(this.argAsString(1));
                if (other == null) {
                    message(sender, Message.DISGUISE_PLAYER_NOT_FOUND, "%player%", this.argAsString(1));
                    return CommandResultType.SUCCESS;
                }
                target = other;
            }
            if (!module.isDisguised(target.getUniqueId())) {
                message(sender, Message.DISGUISE_NOT_DISGUISED);
                return CommandResultType.SUCCESS;
            }
            module.removeDisguise(target.getUniqueId());
            if (target.equals(this.player)) {
                message(sender, Message.DISGUISE_REMOVED);
            } else {
                message(sender, Message.DISGUISE_REMOVED_OTHER, "%player%", target.getName());
                message(target, Message.DISGUISE_REMOVED);
            }
            return CommandResultType.SUCCESS;
        }

        if (firstArg.equalsIgnoreCase("list")) {
            List<String> pool = module.getRandomPool();
            if (pool.isEmpty()) {
                message(sender, Message.DISGUISE_RANDOM_EMPTY);
                return CommandResultType.SUCCESS;
            }
            message(sender, Message.DISGUISE_LIST_HEADER);
            for (String name : pool) {
                message(sender, Message.DISGUISE_LIST_ENTRY, "%name%", name);
            }
            return CommandResultType.SUCCESS;
        }

        if (firstArg.equalsIgnoreCase("random")) {
            if (!hasPermission(sender, Permission.ESSENTIALS_DISGUISE_RANDOM)) {
                return CommandResultType.NO_PERMISSION;
            }
            if (this.args.length >= 2 && hasPermission(sender, Permission.ESSENTIALS_DISGUISE_OTHER)) {
                Player other = Bukkit.getPlayerExact(this.argAsString(1));
                if (other != null) target = other;
            }
            List<String> pool = module.getRandomPool();
            if (pool.isEmpty()) {
                message(sender, Message.DISGUISE_RANDOM_EMPTY);
                return CommandResultType.SUCCESS;
            }
            String randomName = pool.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(pool.size()));
            message(sender, Message.DISGUISE_FETCHING);
            fetchAndApplyDisguise(module, target, randomName, randomName, true);
            return CommandResultType.SUCCESS;
        }

        if (firstArg.equalsIgnoreCase("skin")) {
            if (!hasPermission(sender, Permission.ESSENTIALS_DISGUISE_SKIN)) {
                return CommandResultType.NO_PERMISSION;
            }
            if (this.args.length < 2) {
                message(sender, Message.DISGUISE_USAGE);
                return CommandResultType.SUCCESS;
            }
            if (this.args.length >= 3 && hasPermission(sender, Permission.ESSENTIALS_DISGUISE_OTHER)) {
                Player other = Bukkit.getPlayerExact(this.argAsString(1));
                if (other != null) {
                    target = other;
                    String texture = this.argAsString(2);
                    String signature = this.args.length >= 4 ? this.argAsString(3) : null;
                    applySkinDisguise(module, target, texture, signature);
                    return CommandResultType.SUCCESS;
                }
            }
            String texture = this.argAsString(1);
            String signature = this.args.length >= 3 ? this.argAsString(2) : null;
            applySkinDisguise(module, target, texture, signature);
            return CommandResultType.SUCCESS;
        }

        if (firstArg.equalsIgnoreCase("mob")) {
            if (!hasPermission(sender, Permission.ESSENTIALS_DISGUISE_USE)) {
                return CommandResultType.NO_PERMISSION;
            }
            if (this.args.length < 2) {
                message(sender, Message.DISGUISE_USAGE);
                return CommandResultType.SUCCESS;
            }
            String mobType = this.argAsString(1);
            if (this.args.length >= 3 && hasPermission(sender, Permission.ESSENTIALS_DISGUISE_OTHER)) {
                Player other = Bukkit.getPlayerExact(this.argAsString(1));
                if (other != null) {
                    target = other;
                    mobType = this.argAsString(2);
                }
            }
            if (!isValidMob(module, mobType)) {
                message(sender, Message.DISGUISE_INVALID_MOB, "%type%", mobType);
                return CommandResultType.SUCCESS;
            }
            applyMobDisguise(module, target, mobType);
            return CommandResultType.SUCCESS;
        }

        if (firstArg.equalsIgnoreCase("player")) {
            if (this.args.length < 2) {
                message(sender, Message.DISGUISE_USAGE);
                return CommandResultType.SUCCESS;
            }
            String skinName = this.argAsString(1);
            if (this.args.length >= 3 && hasPermission(sender, Permission.ESSENTIALS_DISGUISE_OTHER)) {
                Player other = Bukkit.getPlayerExact(this.argAsString(1));
                if (other != null) {
                    target = other;
                    skinName = this.argAsString(2);
                }
            }
            if (target != this.player && !hasPermission(sender, Permission.ESSENTIALS_DISGUISE_OTHER)) {
                return CommandResultType.NO_PERMISSION;
            }
            boolean bypass = hasPermission(sender, Permission.ESSENTIALS_DISGUISE_BYPASS_COOLDOWN);
            if (target.equals(this.player) && module.isDisguiseCooldown(target.getUniqueId()) && !bypass) {
                message(sender, Message.DISGUISE_COOLDOWN, "%seconds%", String.valueOf(module.getDisguiseRemainingCooldown(target.getUniqueId())));
                return CommandResultType.SUCCESS;
            }
            message(sender, Message.DISGUISE_FETCHING);
            fetchAndApplyDisguise(module, target, skinName, skinName, target.equals(this.player));
            return CommandResultType.SUCCESS;
        }

        if (isValidMob(module, firstArg)) {
            if (target != this.player && !hasPermission(sender, Permission.ESSENTIALS_DISGUISE_OTHER)) {
                return CommandResultType.NO_PERMISSION;
            }
            boolean bypass = hasPermission(sender, Permission.ESSENTIALS_DISGUISE_BYPASS_COOLDOWN);
            if (target.equals(this.player) && module.isDisguiseCooldown(target.getUniqueId()) && !bypass) {
                message(sender, Message.DISGUISE_COOLDOWN, "%seconds%", String.valueOf(module.getDisguiseRemainingCooldown(target.getUniqueId())));
                return CommandResultType.SUCCESS;
            }
            applyMobDisguise(module, target, firstArg);
            return CommandResultType.SUCCESS;
        }

        if (this.args.length >= 2 && hasPermission(sender, Permission.ESSENTIALS_DISGUISE_OTHER)) {
            Player other = Bukkit.getPlayerExact(this.argAsString(0));
            if (other != null) {
                target = other;
                firstArg = this.argAsString(1);
            }
        }

        if (target != this.player && !hasPermission(sender, Permission.ESSENTIALS_DISGUISE_OTHER)) {
            return CommandResultType.NO_PERMISSION;
        }

        boolean cooldownBypass = hasPermission(sender, Permission.ESSENTIALS_DISGUISE_BYPASS_COOLDOWN);
        if (target.equals(this.player) && module.isDisguiseCooldown(target.getUniqueId()) && !cooldownBypass) {
            message(sender, Message.DISGUISE_COOLDOWN, "%seconds%", String.valueOf(module.getDisguiseRemainingCooldown(target.getUniqueId())));
            return CommandResultType.SUCCESS;
        }

        message(sender, Message.DISGUISE_FETCHING);
        String effectiveDisguiseName = firstArg;
        fetchAndApplyDisguise(module, target, firstArg, effectiveDisguiseName, target.equals(this.player));
        return CommandResultType.SUCCESS;
    }

    private void applySkinDisguise(NicknamesModule module, Player target, String texture, String signature) {
        DisguiseData data = module.getDisguise(target.getUniqueId());
        if (data == null) {
            data = new DisguiseData();
            data.setDisguiseName(target.getName());
        }
        data.setTextureValue(texture);
        data.setTextureSignature(signature);
        data.setEntityType(null);
        module.applyDisguise(target, data);
        if (target.equals(this.player)) {
            message(sender, Message.DISGUISE_SKIN_SET);
        } else {
            message(sender, Message.DISGUISE_SKIN_SET_OTHER, "%player%", target.getName());
            message(target, Message.DISGUISE_SKIN_SET);
        }
    }

    private boolean isValidMob(NicknamesModule module, String mobType) {
        if (mobType == null || mobType.isEmpty()) return false;
        try {
            org.bukkit.entity.EntityType type = org.bukkit.entity.EntityType.valueOf(mobType.toUpperCase());
            if (!type.isAlive() || type == org.bukkit.entity.EntityType.PLAYER) return false;
        } catch (IllegalArgumentException e) {
            return false;
        }
        return module.getAllowedMobs().isEmpty() || module.getAllowedMobs().contains(mobType.toUpperCase());
    }

    private void applyMobDisguise(NicknamesModule module, Player target, String mobType) {
        DisguiseData data = new DisguiseData();
        data.setEntityType(mobType);
        data.setDisguiseName(null);
        data.setTextureValue(null);
        module.applyDisguise(target, data);
        if (target.equals(this.player)) {
            module.markDisguiseChanged(target.getUniqueId());
            message(sender, Message.DISGUISE_MOB, "%type%", mobType);
        } else {
            message(sender, Message.DISGUISE_MOB_OTHER, "%player%", target.getName(), "%type%", mobType);
            message(target, Message.DISGUISE_MOB, "%type%", mobType);
        }
    }

    private void fetchAndApplyDisguise(NicknamesModule module, Player target, String playerName, String disguiseName, boolean self) {
        Player online = Bukkit.getPlayerExact(playerName);
        if (online != null) {
            String textureValue = null;
            String textureSignature = null;
            for (var property : online.getPlayerProfile().getProperties()) {
                if ("textures".equals(property.getName())) {
                    textureValue = property.getValue();
                    textureSignature = property.getSignature();
                    break;
                }
            }
            applyFetchedDisguise(module, target, disguiseName, textureValue, textureSignature, self);
            return;
        }

        CompletableFuture.supplyAsync(() -> {
            try {
                UUID uuid = fetchUuidFromName(playerName);
                if (uuid == null) return null;
                SkinCache.CachedProfile cached = module.getSkinCache().getCached(uuid);
                if (cached != null) return cached;
                String[] textures = fetchTexturesFromUuid(uuid);
                if (textures == null) return null;
                module.getSkinCache().put(uuid, playerName, textures[0], textures[1]);
                return module.getSkinCache().getCached(uuid);
            } catch (Exception e) {
                return null;
            }
        }).thenAccept(profile -> {
            this.plugin.getScheduler().runNextTick(w -> {
                if (profile == null) {
                    message(sender, Message.DISGUISE_FETCH_FAILED, "%player%", playerName);
                    return;
                }
                applyFetchedDisguise(module, target, disguiseName, profile.textureValue(), profile.textureSignature(), self);
            });
        });
    }

    private void applyFetchedDisguise(NicknamesModule module, Player target, String disguiseName, String textureValue, String textureSignature, boolean self) {
        DisguiseData data = new DisguiseData();
        data.setDisguiseName(disguiseName);
        data.setTextureValue(textureValue);
        data.setTextureSignature(textureSignature);
        module.applyDisguise(target, data);
        if (self) {
            module.markDisguiseChanged(target.getUniqueId());
            message(sender, Message.DISGUISE_SET, "%disguise%", disguiseName);
        } else {
            message(sender, Message.DISGUISE_SET_OTHER, "%player%", target.getName(), "%disguise%", disguiseName);
            message(target, Message.DISGUISE_SET, "%disguise%", disguiseName);
        }
    }

    private UUID fetchUuidFromName(String playerName) throws IOException {
        String url = "https://api.mojang.com/users/profiles/minecraft/" + playerName;
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        if (conn.getResponseCode() != 200) return null;
        String body;
        try (InputStream is = conn.getInputStream()) {
            body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
        String idStr = parseJsonField(body, "id");
        if (idStr == null) return null;
        return parseMojangId(idStr);
    }

    private String[] fetchTexturesFromUuid(UUID uuid) throws IOException {
        String url = "https://sessionserver.mojang.com/session/minecraft/profile/" + uuid.toString().replace("-", "") + "?unsigned=false";
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        if (conn.getResponseCode() != 200) return null;
        String body;
        try (InputStream is = conn.getInputStream()) {
            body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
        String value = parseJsonField(body, "value");
        if (value == null) return null;
        String signature = parseJsonField(body, "signature");
        return new String[]{value, signature};
    }

    private UUID parseMojangId(String idStr) {
        if (idStr.length() != 32) return null;
        String dashed = idStr.substring(0, 8) + "-" + idStr.substring(8, 12) + "-" + idStr.substring(12, 16) + "-" + idStr.substring(16, 20) + "-" + idStr.substring(20);
        return UUID.fromString(dashed);
    }

    private String parseJsonField(String json, String field) {
        String needle = "\"" + field + "\":\"";
        int start = json.indexOf(needle);
        if (start < 0) return null;
        start += needle.length();
        int end = json.indexOf("\"", start);
        if (end < 0) return null;
        return json.substring(start, end);
    }
}
