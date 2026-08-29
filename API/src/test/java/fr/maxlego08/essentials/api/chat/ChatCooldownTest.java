package fr.maxlego08.essentials.api.chat;

import fr.maxlego08.essentials.api.modules.Loadable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ChatCooldown Record Tests")
class ChatCooldownTest {

    @Test
    @DisplayName("Should implement Loadable interface")
    void shouldImplementLoadable() {
        ChatCooldown cooldown = new ChatCooldown(5, 1000);
        assertInstanceOf(Loadable.class, cooldown, "ChatCooldown should implement Loadable");
    }

    @Test
    @DisplayName("Should construct with valid parameters and return values via accessors")
    void shouldConstructAndReturnAccessors() {
        long messages = 3;
        long cooldownDuration = 5000;

        ChatCooldown cooldown = new ChatCooldown(messages, cooldownDuration);

        assertEquals(messages, cooldown.messages());
        assertEquals(cooldownDuration, cooldown.cooldown());
    }

    @Nested
    @DisplayName("Boundary and Edge Case Values")
    class BoundaryValueTests {

        @Test
        @DisplayName("Should handle zero values")
        void shouldHandleZeroValues() {
            ChatCooldown cooldown = new ChatCooldown(0, 0);

            assertEquals(0, cooldown.messages());
            assertEquals(0, cooldown.cooldown());
        }

        @Test
        @DisplayName("Should handle large long values (Long.MAX_VALUE)")
        void shouldHandleLargeValues() {
            ChatCooldown cooldown = new ChatCooldown(Long.MAX_VALUE, Long.MAX_VALUE);

            assertEquals(Long.MAX_VALUE, cooldown.messages());
            assertEquals(Long.MAX_VALUE, cooldown.cooldown());
        }

        @Test
        @DisplayName("Should handle negative long values")
        void shouldHandleNegativeValues() {
            ChatCooldown cooldown = new ChatCooldown(-1, -500);

            assertEquals(-1, cooldown.messages());
            assertEquals(-500, cooldown.cooldown());
        }

        @ParameterizedTest(name = "messages={0}, cooldown={1}")
        @CsvSource({
                "1, 100",
                "5, 3000",
                "10, 60000",
                "0, 5000",
                "5, 0"
        })
        @DisplayName("Should correctly retain various combinations of messages and cooldown")
        void shouldRetainVariousValuePairs(long messages, long cooldownMs) {
            ChatCooldown cooldown = new ChatCooldown(messages, cooldownMs);

            assertEquals(messages, cooldown.messages());
            assertEquals(cooldownMs, cooldown.cooldown());
        }
    }

    @Nested
    @DisplayName("Equality and HashCode Tests")
    class EqualityAndHashCodeTests {

        @Test
        @DisplayName("Should be equal and share hashCode for identical records")
        void shouldBeEqualForIdenticalRecords() {
            ChatCooldown c1 = new ChatCooldown(5, 1000);
            ChatCooldown c2 = new ChatCooldown(5, 1000);

            assertEquals(c1, c2);
            assertEquals(c2, c1);
            assertEquals(c1.hashCode(), c2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal when fields differ")
        void shouldNotBeEqualWhenFieldsDiffer() {
            ChatCooldown base = new ChatCooldown(5, 1000);
            ChatCooldown diffMessages = new ChatCooldown(6, 1000);
            ChatCooldown diffCooldown = new ChatCooldown(5, 2000);

            assertNotEquals(base, diffMessages);
            assertNotEquals(base, diffCooldown);
            assertNotEquals(base, null);
            assertNotEquals(base, new Object());
        }
    }

    @Test
    @DisplayName("toString should contain message count and cooldown duration")
    void shouldContainFieldValuesInToString() {
        ChatCooldown cooldown = new ChatCooldown(7, 4500);
        String toString = cooldown.toString();

        assertNotNull(toString);
        assertTrue(toString.contains("7"), "toString should contain messages count");
        assertTrue(toString.contains("4500"), "toString should contain cooldown duration");
    }
}
