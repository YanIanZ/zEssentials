package fr.maxlego08.essentials.api.economy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@DisplayName("Economy Default format() Method Tests")
class EconomyFormatTest {

    private Economy economy;

    @BeforeEach
    void setUp() {
        economy = Mockito.mock(Economy.class, Mockito.CALLS_REAL_METHODS);
    }

    @Test
    @DisplayName("Should replace %price% placeholder with price string")
    void shouldReplacePricePlaceholder() {
        when(economy.getFormat()).thenReturn("%price%$");

        String formatted = economy.format("100", 1);

        assertEquals("100$", formatted);
    }

    @Nested
    @DisplayName("Pluralization (%s%) Tests")
    class PluralizationTests {

        @ParameterizedTest(name = "amount = {0} should replace %s% with \"s\"")
        @ValueSource(longs = {2, 3, 10, 100, 1000000L, Long.MAX_VALUE})
        @DisplayName("Should replace %s% with 's' when amount > 1")
        void shouldAppendSWhenAmountGreaterThanOne(long amount) {
            when(economy.getFormat()).thenReturn("%price% coin%s%");

            String result = economy.format(String.valueOf(amount), amount);

            assertEquals(amount + " coins", result);
        }

        @Test
        @DisplayName("Should replace %s% with empty string when amount == 1")
        void shouldNotAppendSWhenAmountEqualsOne() {
            when(economy.getFormat()).thenReturn("%price% coin%s%");

            String result = economy.format("1", 1);

            assertEquals("1 coin", result);
        }

        @Test
        @DisplayName("Should replace %s% with empty string when amount == 0")
        void shouldNotAppendSWhenAmountEqualsZero() {
            when(economy.getFormat()).thenReturn("%price% coin%s%");

            String result = economy.format("0", 0);

            assertEquals("0 coin", result);
        }

        @ParameterizedTest(name = "amount = {0} should replace %s% with empty string")
        @ValueSource(longs = {-1, -5, -100, Long.MIN_VALUE})
        @DisplayName("Should replace %s% with empty string when amount < 0")
        void shouldNotAppendSWhenAmountIsNegative(long amount) {
            when(economy.getFormat()).thenReturn("%price% coin%s%");

            String result = economy.format(String.valueOf(amount), amount);

            assertEquals(amount + " coin", result);
        }
    }

    @Nested
    @DisplayName("Complete Format Patterns")
    class CompleteFormatPatternTests {

        @ParameterizedTest(name = "Format \"$%price% coin%s%\" with price \"{0}\" and amount {1} -> \"{2}\"")
        @CsvSource({
                "10, 1, $10 coin",
                "10, 2, $10 coins",
                "0, 0, $0 coin",
                "100.50, 100, $100.50 coins",
                "1.0k, 1000, $1.0k coins"
        })
        @DisplayName("Should format correctly with '$%price% coin%s%' pattern")
        void shouldFormatWithDollarAndCoinsPattern(String price, long amount, String expected) {
            when(economy.getFormat()).thenReturn("$%price% coin%s%");

            String result = economy.format(price, amount);

            assertEquals(expected, result);
        }

        @Test
        @DisplayName("Should handle format with multiple %price% and %s% placeholders")
        void shouldHandleMultiplePlaceholders() {
            when(economy.getFormat()).thenReturn("[%price%] item%s% for %price% dollar%s%");

            String result = economy.format("5", 5);

            assertEquals("[5] items for 5 dollars", result);
        }

        @Test
        @DisplayName("Should return format string unchanged when no placeholders exist")
        void shouldReturnUnchangedWhenNoPlaceholders() {
            when(economy.getFormat()).thenReturn("Fixed currency text");

            String result = economy.format("100", 5);

            assertEquals("Fixed currency text", result);
        }
    }

    @Test
    @DisplayName("Should work with anonymous implementation of Economy interface")
    void shouldWorkWithAnonymousImplementation() {
        Economy anonEconomy = new Economy() {
            @Override
            public String getName() { return "coins"; }
            @Override
            public String getDisplayName() { return "Coins"; }
            @Override
            public String getSymbol() { return "©"; }
            @Override
            public String getFormat() { return "%price% %s%piece"; }
            @Override
            public boolean isVaultEconomy() { return false; }
            @Override
            public java.math.BigDecimal getMinValue() { return java.math.BigDecimal.ZERO; }
            @Override
            public java.math.BigDecimal getMaxValue() { return java.math.BigDecimal.TEN; }
            @Override
            public java.math.BigDecimal getMinPayValue() { return java.math.BigDecimal.ONE; }
            @Override
            public java.math.BigDecimal getMaxPayValue() { return java.math.BigDecimal.TEN; }
            @Override
            public java.math.BigDecimal getMinConfirmInventory() { return java.math.BigDecimal.ONE; }
            @Override
            public boolean isPaymentEnabled() { return true; }
            @Override
            public boolean isConfirmInventoryEnabled() { return false; }
            @Override
            public PriceFormat getPriceFormat() { return PriceFormat.PRICE_RAW; }
        };

        assertEquals("5 spiece", anonEconomy.format("5", 5));
        assertEquals("1 piece", anonEconomy.format("1", 1));
    }
}
