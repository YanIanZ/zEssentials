package fr.maxlego08.essentials.api.economy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("PriceFormat Enum Tests")
class PriceFormatTest {

    @Test
    @DisplayName("Should contain exactly 3 enum constants")
    void shouldContainExactlyThreeConstants() {
        PriceFormat[] values = PriceFormat.values();
        assertEquals(3, values.length, "PriceFormat should have exactly 3 constants");
        assertArrayEquals(
                new PriceFormat[]{
                        PriceFormat.PRICE_RAW,
                        PriceFormat.PRICE_WITH_DECIMAL_FORMAT,
                        PriceFormat.PRICE_WITH_REDUCTION
                },
                values,
                "PriceFormat constants should match expected values and order"
        );
    }

    @ParameterizedTest(name = "Constant {0} exists and is not null")
    @EnumSource(PriceFormat.class)
    @DisplayName("Should verify each constant is defined and non-null")
    void shouldVerifyConstantsExist(PriceFormat format) {
        assertNotNull(format, "Enum constant should not be null");
    }

    @ParameterizedTest(name = "valueOf(\"{0}\") should return correct enum constant")
    @ValueSource(strings = {"PRICE_RAW", "PRICE_WITH_DECIMAL_FORMAT", "PRICE_WITH_REDUCTION"})
    @DisplayName("Should perform valueOf() round-trip for all valid constant names")
    void shouldRoundTripValueOf(String name) {
        PriceFormat format = PriceFormat.valueOf(name);
        assertNotNull(format);
        assertEquals(name, format.name());
    }

    @Test
    @DisplayName("valueOf() should throw IllegalArgumentException for invalid constant name")
    void shouldThrowExceptionForInvalidName() {
        assertThrows(IllegalArgumentException.class, () -> PriceFormat.valueOf("INVALID_FORMAT"));
        assertThrows(IllegalArgumentException.class, () -> PriceFormat.valueOf("price_raw"));
        assertThrows(NullPointerException.class, () -> PriceFormat.valueOf(null));
    }
}
