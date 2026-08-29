package fr.maxlego08.essentials.api.configuration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("ReplacePlaceholderType Enum Tests")
class ReplacePlaceholderTypeTest {

    @Test
    @DisplayName("Should contain exactly 2 enum constants")
    void shouldContainExactlyTwoConstants() {
        ReplacePlaceholderType[] values = ReplacePlaceholderType.values();
        assertEquals(2, values.length, "ReplacePlaceholderType should have exactly 2 constants");
        assertArrayEquals(
                new ReplacePlaceholderType[]{
                        ReplacePlaceholderType.NUMBER,
                        ReplacePlaceholderType.STRING
                },
                values,
                "ReplacePlaceholderType constants should match expected values and order"
        );
    }

    @ParameterizedTest(name = "Constant {0} exists and is not null")
    @EnumSource(ReplacePlaceholderType.class)
    @DisplayName("Should verify each constant is defined and non-null")
    void shouldVerifyConstantsExist(ReplacePlaceholderType type) {
        assertNotNull(type, "Enum constant should not be null");
    }

    @ParameterizedTest(name = "valueOf(\"{0}\") should return correct enum constant")
    @ValueSource(strings = {"NUMBER", "STRING"})
    @DisplayName("Should perform valueOf() round-trip for all valid constant names")
    void shouldRoundTripValueOf(String name) {
        ReplacePlaceholderType type = ReplacePlaceholderType.valueOf(name);
        assertNotNull(type);
        assertEquals(name, type.name());
    }

    @Test
    @DisplayName("valueOf() should throw IllegalArgumentException for invalid constant names")
    void shouldThrowExceptionForInvalidName() {
        assertThrows(IllegalArgumentException.class, () -> ReplacePlaceholderType.valueOf("BOOLEAN"));
        assertThrows(IllegalArgumentException.class, () -> ReplacePlaceholderType.valueOf("number"));
        assertThrows(NullPointerException.class, () -> ReplacePlaceholderType.valueOf(null));
    }
}
