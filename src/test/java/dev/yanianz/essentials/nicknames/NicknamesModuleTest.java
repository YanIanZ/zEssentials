package dev.yanianz.essentials.nicknames;

import dev.yanianz.essentials.disguise.DisguiseData;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class NicknamesModuleTest {

    private NicknamesModule module;
    private Player player;

    @BeforeEach
    void setUp() throws Exception {
        module = new NicknamesModule(null);
        setField("maxLength", 16);
        setField("blockImpersonation", false);
        setField("cooldownSeconds", 0);

        player = Mockito.mock(Player.class);
        when(player.hasPermission(anyString())).thenReturn(false);
    }

    private void setField(String name, Object value) throws Exception {
        java.lang.reflect.Field f = NicknamesModule.class.getDeclaredField(name);
        f.setAccessible(true);
        if (value instanceof Integer i) f.setInt(module, i);
        else if (value instanceof Boolean b) f.setBoolean(module, b);
        else f.set(module, value);
    }

    @Test
    @DisplayName("validate rejects nicknames longer than max-length")
    void testValidateRejectsTooLong() throws Exception {
        setField("maxLength", 8);
        NicknamesModule.NickError error = module.validate(player, "thisNameIsFarTooLong");
        assertNotNull(error);
        assertTrue(error == NicknamesModule.NickError.TOO_LONG);
    }

    @Test
    @DisplayName("validate accepts valid nicknames")
    void testValidateAcceptsValid() {
        NicknamesModule.NickError error = module.validate(player, "Steve");
        assertNull(error);
    }

    @Test
    @DisplayName("validate rejects nicknames with invalid characters")
    void testValidateRejectsInvalidCharacters() {
        NicknamesModule.NickError error = module.validate(player, "bad!name");
        assertNotNull(error);
        assertTrue(error == NicknamesModule.NickError.INVALID_CHARACTERS);
    }

    @Test
    @DisplayName("isOnCooldown returns true within window, false after")
    void testIsOnCooldown() throws Exception {
        setField("cooldownSeconds", 60);
        UUID id = UUID.randomUUID();
        assertFalse(module.isOnCooldown(id), "fresh player should not be on cooldown");

        module.markChanged(id);
        assertTrue(module.isOnCooldown(id), "player just changed should be on cooldown");

        setField("cooldownSeconds", 0);
        assertFalse(module.isOnCooldown(id), "zero-second cooldown should never be active");
    }

    @Test
    @DisplayName("getDisplayName returns null for undisguised player")
    void testGetDisplayNameNull() {
        assertNull(module.getDisplayName(UUID.randomUUID()));
    }

    @Test
    @DisplayName("isDisguised returns false for unknown uuid")
    void testIsDisguisedUnknown() {
        assertFalse(module.isDisguised(UUID.randomUUID()));
    }

    @Test
    @DisplayName("getDisguise returns null for unknown uuid")
    void testGetDisguiseUnknown() {
        assertNull(module.getDisguise(UUID.randomUUID()));
    }

    @Test
    @DisplayName("getRandomPool returns empty list by default")
    void testGetRandomPoolDefault() {
        assertNotNull(module.getRandomPool());
        assertTrue(module.getRandomPool().isEmpty());
    }

    @Test
    @DisplayName("isDisguiseEnabled returns false by default")
    void testDisguiseDisabledDefault() {
        assertFalse(module.isDisguiseEnabled());
    }

    @Test
    @DisplayName("isSelfView returns false by default")
    void testSelfViewDefault() {
        assertFalse(module.isSelfView());
    }

    @Test
    @DisplayName("disguise cooldown works")
    void testDisguiseCooldown() throws Exception {
        setField("disguiseCooldownSeconds", 60);
        UUID id = UUID.randomUUID();
        assertFalse(module.isDisguiseCooldown(id));
        module.markDisguiseChanged(id);
        assertTrue(module.isDisguiseCooldown(id));
        setField("disguiseCooldownSeconds", 0);
        assertFalse(module.isDisguiseCooldown(id));
    }
}