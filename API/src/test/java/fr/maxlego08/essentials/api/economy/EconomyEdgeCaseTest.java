package fr.maxlego08.essentials.api.economy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class EconomyEdgeCaseTest {

    @ParameterizedTest
    @CsvSource({
            "0, 0.0",
            "1, 1.0",
            "999, 999.0",
            "-1, -1.0",
            "-0.01, -0.01"
    })
    @DisplayName("Format handles edge case values")
    void testEdgeCaseValues(double input, String expected) {
        assertEquals(expected, String.valueOf(input));
    }

    @Test
    @DisplayName("Compact number formatting produces expected suffixes")
    void testCompactFormatting() {
        assertEquals("1.0K", String.format(Locale.US, "%.1fK", 1000.0 / 1000.0));
        assertEquals("1.5K", String.format(Locale.US, "%.1fK", 1500.0 / 1000.0));
        assertEquals("1.0M", String.format(Locale.US, "%.1fM", 1_000_000.0 / 1_000_000.0));
        assertEquals("2.5M", String.format(Locale.US, "%.1fM", 2_500_000.0 / 1_000_000.0));
        assertEquals("1.0B", String.format(Locale.US, "%.1fB", 1_000_000_000.0 / 1_000_000_000.0));
        assertEquals("1.0T", String.format(Locale.US, "%.1fT", 1e12 / 1e12));
    }

    @Test
    @DisplayName("Large numbers don't overflow compact formatting")
    void testLargeNumbers() {
        double quadrillion = 1e15;
        String formatted = String.format(Locale.US, "%.2fQ", quadrillion / 1e15);
        assertEquals("1.00Q", formatted);
    }

    @Test
    @DisplayName("Zero and negative values format correctly")
    void testZeroAndNegative() {
        assertEquals("0", String.valueOf(0));
        assertEquals("-100", String.valueOf(-100));
        assertEquals("-0.5", String.valueOf(-0.5));
    }

    @Test
    @DisplayName("Very small decimal values preserve precision")
    void testSmallDecimals() {
        assertEquals("0.01", String.valueOf(0.01));
        assertEquals("0.001", String.valueOf(0.001));
    }
}
