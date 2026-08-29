package fr.maxlego08.essentials.api.commands;

import fr.maxlego08.essentials.api.modules.Loadable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("CommandCooldown Record Tests")
class CommandCooldownTest {

    @Test
    @DisplayName("Record construction and accessors should return correct values")
    void testConstructionAndAccessors() {
        List<Map<String, Object>> permissions = List.of(Map.of("permission", "essentials.fly", "cooldown", 10));
        CommandCooldown cooldown = new CommandCooldown("fly", 30, permissions);

        assertEquals("fly", cooldown.command());
        assertEquals(30, cooldown.cooldown());
        assertEquals(permissions, cooldown.permissions());
    }

    @Test
    @DisplayName("Record should allow null command and null permissions")
    void testNullValues() {
        CommandCooldown cooldown = new CommandCooldown(null, 0, null);

        assertNull(cooldown.command());
        assertEquals(0, cooldown.cooldown());
        assertNull(cooldown.permissions());
    }

    @Test
    @DisplayName("Record should handle empty permissions list")
    void testEmptyPermissionsList() {
        CommandCooldown cooldown = new CommandCooldown("heal", 60, Collections.emptyList());

        assertEquals("heal", cooldown.command());
        assertEquals(60, cooldown.cooldown());
        assertNotNull(cooldown.permissions());
        assertTrue(cooldown.permissions().isEmpty());
    }

    @Test
    @DisplayName("equals() and hashCode() should follow Java record contract")
    void testEqualsAndHashCode() {
        List<Map<String, Object>> perms1 = List.of(Map.of("perm", "essentials.warp"));
        List<Map<String, Object>> perms2 = List.of(Map.of("perm", "essentials.warp"));

        CommandCooldown cd1 = new CommandCooldown("warp", 15, perms1);
        CommandCooldown cd2 = new CommandCooldown("warp", 15, perms2);
        CommandCooldown cdDifferentCommand = new CommandCooldown("spawn", 15, perms1);
        CommandCooldown cdDifferentCooldown = new CommandCooldown("warp", 20, perms1);
        CommandCooldown cdDifferentPerms = new CommandCooldown("warp", 15, Collections.emptyList());

        // Equals
        assertEquals(cd1, cd2);
        assertEquals(cd1.hashCode(), cd2.hashCode());
        assertEquals(cd1, cd1);

        // Not equals
        assertNotEquals(cd1, cdDifferentCommand);
        assertNotEquals(cd1, cdDifferentCooldown);
        assertNotEquals(cd1, cdDifferentPerms);
        assertNotEquals(cd1, null);
        assertNotEquals(cd1, new Object());
    }

    @Test
    @DisplayName("CommandCooldown should implement Loadable interface")
    void testImplementsLoadable() {
        CommandCooldown cooldown = new CommandCooldown("feed", 10, List.of());
        assertInstanceOf(Loadable.class, cooldown);
    }

    @Test
    @DisplayName("toString() should contain class name and component values")
    void testToString() {
        CommandCooldown cooldown = new CommandCooldown("tp", 5, List.of());
        String str = cooldown.toString();

        assertNotNull(str);
        assertTrue(str.contains("CommandCooldown"));
        assertTrue(str.contains("command=tp") || str.contains("tp"));
        assertTrue(str.contains("5"));
    }
}
