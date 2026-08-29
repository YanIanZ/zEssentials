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

@DisplayName("NumberMultiplicationFormat Record Tests")
class NumberMultiplicationFormatTest {

    @Test
    @DisplayName("Should correctly implement Loadable interface")
    void shouldImplementLoadable() {
        NumberMultiplicationFormat format = new NumberMultiplicationFormat("k", new BigDecimal("1000"));
        assertInstanceOf(Loadable.class, format, "NumberMultiplicationFormat must implement Loadable");
    }

    @Test
    @DisplayName("Should construct with valid arguments and return them via accessors")
    void shouldConstructAndProvideAccessors() {
        String formatString = "M";
        BigDecimal multiplication = new BigDecimal("1000000");

        NumberMultiplicationFormat format = new NumberMultiplicationFormat(formatString, multiplication);

        assertEquals(formatString, format.format());
        assertEquals(multiplication, format.multiplication());
    }

    @Nested
    @DisplayName("BigDecimal Handling Tests")
    class BigDecimalHandlingTests {

        @Test
        @DisplayName("Should handle BigDecimal.ZERO")
        void shouldHandleZero() {
            NumberMultiplicationFormat format = new NumberMultiplicationFormat("zero", BigDecimal.ZERO);
            assertEquals(BigDecimal.ZERO, format.multiplication());
        }

        @Test
        @DisplayName("Should handle large BigDecimal values")
        void shouldHandleLargeBigDecimal() {
            BigDecimal large = new BigDecimal("1000000000000000000");
            NumberMultiplicationFormat format = new NumberMultiplicationFormat("E", large);
            assertEquals(large, format.multiplication());
        }

        @Test
        @DisplayName("Should handle negative BigDecimal values")
        void shouldHandleNegativeBigDecimal() {
            BigDecimal negative = new BigDecimal("-100");
            NumberMultiplicationFormat format = new NumberMultiplicationFormat("neg", negative);
            assertEquals(negative, format.multiplication());
        }

        @ParameterizedTest(name = "Should support multiplication factor {0}")
        @ValueSource(strings = {"0.5", "1", "10", "1000", "1000000", "0.001"})
        @DisplayName("Should construct correctly with various multiplication values")
        void shouldHandleVariousMultiplications(String factor) {
            BigDecimal factorVal = new BigDecimal(factor);
            NumberMultiplicationFormat format = new NumberMultiplicationFormat(factor, factorVal);
            assertEquals(factorVal, format.multiplication());
        }
    }

    @Nested
    @DisplayName("Equality and HashCode Tests")
    class EqualityAndHashCodeTests {

        @Test
        @DisplayName("Should be equal and share hashCode for identical records")
        void shouldBeEqualForIdenticalRecords() {
            NumberMultiplicationFormat f1 = new NumberMultiplicationFormat("k", new BigDecimal("1000"));
            NumberMultiplicationFormat f2 = new NumberMultiplicationFormat("k", new BigDecimal("1000"));

            assertEquals(f1, f2);
            assertEquals(f2, f1);
            assertEquals(f1.hashCode(), f2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal when fields differ")
        void shouldNotBeEqualWhenFieldsDiffer() {
            NumberMultiplicationFormat base = new NumberMultiplicationFormat("k", new BigDecimal("1000"));
            NumberMultiplicationFormat diffFormat = new NumberMultiplicationFormat("m", new BigDecimal("1000"));
            NumberMultiplicationFormat diffMult = new NumberMultiplicationFormat("k", new BigDecimal("2000"));

            assertNotEquals(base, diffFormat);
            assertNotEquals(base, diffMult);
            assertNotEquals(base, null);
            assertNotEquals(base, new Object());
        }
    }

    @Nested
    @DisplayName("Edge Cases and Null Handling")
    class EdgeCasesAndNullTests {

        @Test
        @DisplayName("Should allow null values in fields")
        void shouldAllowNullFields() {
            NumberMultiplicationFormat format = new NumberMultiplicationFormat(null, null);
            assertNull(format.format());
            assertNull(format.multiplication());
        }

        @Test
        @DisplayName("toString should contain format and multiplication values")
        void shouldContainFieldsInToString() {
            NumberMultiplicationFormat format = new NumberMultiplicationFormat("B", new BigDecimal("1000000000"));
            String toString = format.toString();

            assertNotNull(toString);
            assertTrue(toString.contains("B"), "toString should contain format name");
            assertTrue(toString.contains("1000000000"), "toString should contain multiplication value");
        }
    }
}
