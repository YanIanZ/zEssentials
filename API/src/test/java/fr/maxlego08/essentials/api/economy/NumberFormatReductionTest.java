package fr.maxlego08.essentials.api.economy;

import fr.maxlego08.essentials.api.modules.Loadable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("NumberFormatReduction Record Tests")
class NumberFormatReductionTest {

    @Test
    @DisplayName("Should correctly implement Loadable interface")
    void shouldImplementLoadable() {
        NumberFormatReduction reduction = new NumberFormatReduction("#.##", new BigDecimal("1000"), "k");
        assertInstanceOf(Loadable.class, reduction, "NumberFormatReduction must implement Loadable");
    }

    @Test
    @DisplayName("Should construct with valid arguments and return them via accessors")
    void shouldConstructAndProvideAccessors() {
        String format = "#,##0.00";
        BigDecimal maxAmount = new BigDecimal("1000000");
        String display = "M";

        NumberFormatReduction reduction = new NumberFormatReduction(format, maxAmount, display);

        assertEquals(format, reduction.format());
        assertEquals(maxAmount, reduction.maxAmount());
        assertEquals(display, reduction.display());
    }

    @Nested
    @DisplayName("BigDecimal Handling Tests")
    class BigDecimalHandlingTests {

        @Test
        @DisplayName("Should handle BigDecimal.ZERO")
        void shouldHandleZero() {
            NumberFormatReduction reduction = new NumberFormatReduction("#", BigDecimal.ZERO, "");
            assertEquals(BigDecimal.ZERO, reduction.maxAmount());
        }

        @Test
        @DisplayName("Should handle large BigDecimal values")
        void shouldHandleLargeBigDecimal() {
            BigDecimal largeValue = new BigDecimal("1000000000000000000000000.999");
            NumberFormatReduction reduction = new NumberFormatReduction("#.##", largeValue, "Q");
            assertEquals(largeValue, reduction.maxAmount());
        }

        @Test
        @DisplayName("Should handle negative BigDecimal values")
        void shouldHandleNegativeBigDecimal() {
            BigDecimal negativeValue = new BigDecimal("-500.50");
            NumberFormatReduction reduction = new NumberFormatReduction("#.##", negativeValue, "-k");
            assertEquals(negativeValue, reduction.maxAmount());
        }

        @ParameterizedTest(name = "Should support string representation {0}")
        @ValueSource(strings = {"0.00001", "100", "9999999999.9999", "-0.01"})
        @DisplayName("Should construct correctly with various BigDecimal values")
        void shouldHandleVariousBigDecimalValues(String val) {
            BigDecimal amount = new BigDecimal(val);
            NumberFormatReduction reduction = new NumberFormatReduction("###,###", amount, "X");
            assertEquals(amount, reduction.maxAmount());
        }
    }

    @Nested
    @DisplayName("Equality and HashCode Tests")
    class EqualityAndHashCodeTests {

        @Test
        @DisplayName("Should be equal and share hashCode for identical records")
        void shouldBeEqualForIdenticalRecords() {
            NumberFormatReduction r1 = new NumberFormatReduction("#.##", new BigDecimal("1000"), "k");
            NumberFormatReduction r2 = new NumberFormatReduction("#.##", new BigDecimal("1000"), "k");

            assertEquals(r1, r2);
            assertEquals(r2, r1);
            assertEquals(r1.hashCode(), r2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal when fields differ")
        void shouldNotBeEqualWhenFieldsDiffer() {
            NumberFormatReduction base = new NumberFormatReduction("#.##", new BigDecimal("1000"), "k");
            NumberFormatReduction diffFormat = new NumberFormatReduction("#.00", new BigDecimal("1000"), "k");
            NumberFormatReduction diffAmount = new NumberFormatReduction("#.##", new BigDecimal("2000"), "k");
            NumberFormatReduction diffDisplay = new NumberFormatReduction("#.##", new BigDecimal("1000"), "M");

            assertNotEquals(base, diffFormat);
            assertNotEquals(base, diffAmount);
            assertNotEquals(base, diffDisplay);
            assertNotEquals(base, null);
            assertNotEquals(base, "some string");
        }
    }

    @Nested
    @DisplayName("Edge Cases and Null Handling")
    class EdgeCasesAndNullTests {

        @Test
        @DisplayName("Should allow null values in fields")
        void shouldAllowNullFields() {
            NumberFormatReduction reduction = new NumberFormatReduction(null, null, null);
            assertNull(reduction.format());
            assertNull(reduction.maxAmount());
            assertNull(reduction.display());
        }

        @Test
        @DisplayName("toString should contain all record component values")
        void shouldContainComponentValuesInToString() {
            NumberFormatReduction reduction = new NumberFormatReduction("#,##0", new BigDecimal("5000"), "k");
            String toString = reduction.toString();

            assertNotNull(toString);
            assertTrue(toString.contains("#,##0"), "toString should contain format");
            assertTrue(toString.contains("5000"), "toString should contain maxAmount");
            assertTrue(toString.contains("k"), "toString should contain display");
        }
    }
}
