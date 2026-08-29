package fr.maxlego08.essentials.api.commands;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("CommandResultType Enum Tests")
class CommandResultTypeTest {

    @Test
    @DisplayName("values() should return exactly 7 elements")
    void testValuesCount() {
        CommandResultType[] values = CommandResultType.values();
        assertEquals(7, values.length);
    }

    @Test
    @DisplayName("All expected constants should exist in CommandResultType enum")
    void testAllConstantsExist() {
        assertNotNull(CommandResultType.SUCCESS);
        assertNotNull(CommandResultType.SYNTAX_ERROR);
        assertNotNull(CommandResultType.NO_PERMISSION);
        assertNotNull(CommandResultType.DEFAULT);
        assertNotNull(CommandResultType.CONTINUE);
        assertNotNull(CommandResultType.MODULE_DISABLE);
        assertNotNull(CommandResultType.COOLDOWN);
    }

    @ParameterizedTest
    @EnumSource(CommandResultType.class)
    @DisplayName("valueOf() should round-trip for every enum constant")
    void testValueOfRoundTrip(CommandResultType type) {
        CommandResultType result = CommandResultType.valueOf(type.name());
        assertSame(type, result);
    }

    @ParameterizedTest
    @ValueSource(strings = {"SUCCESS", "SYNTAX_ERROR", "NO_PERMISSION", "DEFAULT", "CONTINUE", "MODULE_DISABLE", "COOLDOWN"})
    @DisplayName("valueOf() should resolve each valid constant name")
    void testValueOfValidStrings(String name) {
        assertNotNull(CommandResultType.valueOf(name));
    }

    @Test
    @DisplayName("valueOf() should throw IllegalArgumentException for invalid constant name")
    void testValueOfInvalid() {
        assertThrows(IllegalArgumentException.class, () -> CommandResultType.valueOf("INVALID_CONSTANT"));
    }
}
