package fr.maxlego08.essentials.api.commands;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Permission Enum Tests")
class PermissionTest {

    @Test
    @DisplayName("asPermission() should convert ESSENTIALS_USE to essentials.use")
    void testAsPermissionEssentialsUse() {
        assertEquals("essentials.use", Permission.ESSENTIALS_USE.asPermission());
    }

    @Test
    @DisplayName("asPermission() should convert ESSENTIALS_GAMEMODE to essentials.gamemode")
    void testAsPermissionEssentialsGamemode() {
        assertEquals("essentials.gamemode", Permission.ESSENTIALS_GAMEMODE.asPermission());
    }

    @Test
    @DisplayName("asPermission(String) should append suffix")
    void testAsPermissionWithSuffix() {
        assertEquals("essentials.warp.spawn", Permission.ESSENTIALS_WARP_.asPermission("spawn"));
        assertEquals("essentials.use.sub", Permission.ESSENTIALS_USE.asPermission(".sub"));
    }

    @Test
    @DisplayName("toPermission() for ESSENTIALS_WARP_ should include '<warp name>' suffix")
    void testToPermissionWithArgs() {
        assertEquals("essentials.warp.<warp name>", Permission.ESSENTIALS_WARP_.toPermission());
    }

    @Test
    @DisplayName("toPermission() for permission without args should equal asPermission()")
    void testToPermissionWithoutArgs() {
        assertEquals("essentials.use", Permission.ESSENTIALS_USE.toPermission());
        assertEquals(Permission.ESSENTIALS_USE.asPermission(), Permission.ESSENTIALS_USE.toPermission());
    }

    @Test
    @DisplayName("getDescription() should return empty string for no-arg permissions")
    void testGetDescriptionDefault() {
        assertEquals("", Permission.ESSENTIALS_USE.getDescription());
        assertEquals("", Permission.ESSENTIALS_GAMEMODE.getDescription());
    }

    @Test
    @DisplayName("getDescription() should return proper description when provided")
    void testGetDescriptionCustom() {
        assertEquals("Allows to heal another player", Permission.ESSENTIALS_HEAL_ALL.getDescription());
        assertEquals("Allows to teleport to a specific warp", Permission.ESSENTIALS_WARP_.getDescription());
    }

    @Test
    @DisplayName("getArgs() should return empty array for permissions without args")
    void testGetArgsEmpty() {
        assertNotNull(Permission.ESSENTIALS_USE.getArgs());
        assertEquals(0, Permission.ESSENTIALS_USE.getArgs().length);
    }

    @Test
    @DisplayName("getArgs() should return array of args when provided")
    void testGetArgsNotEmpty() {
        assertArrayEquals(new String[]{"warp name"}, Permission.ESSENTIALS_WARP_.getArgs());
    }

    @ParameterizedTest
    @EnumSource(Permission.class)
    @DisplayName("All enum constants should have non-null asPermission()")
    void testAllPermissionsNonNull(Permission permission) {
        assertNotNull(permission.asPermission(), "asPermission() must not be null for " + permission.name());
        assertFalse(permission.asPermission().isEmpty(), "asPermission() must not be empty for " + permission.name());
    }

    @ParameterizedTest
    @EnumSource(Permission.class)
    @DisplayName("All asPermission() results should contain only lowercase letters, digits, and dots")
    void testAllPermissionsPattern(Permission permission) {
        String permStr = permission.asPermission();
        assertTrue(permStr.matches("^[a-z0-9.]+$"),
                () -> "Permission " + permission.name() + " format is invalid: '" + permStr + "'");
    }
}
