package fr.maxlego08.essentials.api.utils;

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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("MessageColor Record Tests")
class MessageColorTest {

    @Test
    @DisplayName("Should implement Loadable interface")
    void shouldImplementLoadable() {
        MessageColor color = new MessageColor("primary", "#FF5555");
        assertInstanceOf(Loadable.class, color, "MessageColor should implement Loadable");
    }

    @Test
    @DisplayName("Should construct with valid arguments and return them via accessors")
    void shouldConstructAndReturnAccessors() {
        String key = "prefix_color";
        String colorCode = "&a";

        MessageColor messageColor = new MessageColor(key, colorCode);

        assertEquals(key, messageColor.key());
        assertEquals(colorCode, messageColor.color());
    }

    @ParameterizedTest(name = "key=\"{0}\", color=\"{1}\"")
    @CsvSource({
            "error, <red>",
            "success, #00FF00",
            "warning, &e",
            "info, <gradient:#ff0000:#0000ff>"
    })
    @DisplayName("Should retain various color key and value patterns")
    void shouldRetainVariousColorPatterns(String key, String color) {
        MessageColor messageColor = new MessageColor(key, color);

        assertEquals(key, messageColor.key());
        assertEquals(color, messageColor.color());
    }

    @Nested
    @DisplayName("Equality and HashCode Tests")
    class EqualityAndHashCodeTests {

        @Test
        @DisplayName("Should be equal and share hashCode for identical records")
        void shouldBeEqualForIdenticalRecords() {
            MessageColor c1 = new MessageColor("primary", "&6");
            MessageColor c2 = new MessageColor("primary", "&6");

            assertEquals(c1, c2);
            assertEquals(c2, c1);
            assertEquals(c1.hashCode(), c2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal when fields differ")
        void shouldNotBeEqualWhenFieldsDiffer() {
            MessageColor base = new MessageColor("primary", "&6");
            MessageColor diffKey = new MessageColor("secondary", "&6");
            MessageColor diffColor = new MessageColor("primary", "&7");

            assertNotEquals(base, diffKey);
            assertNotEquals(base, diffColor);
            assertNotEquals(base, null);
            assertNotEquals(base, "other");
        }
    }

    @Nested
    @DisplayName("Edge Cases and Null Handling")
    class EdgeCasesAndNullTests {

        @Test
        @DisplayName("Should allow null values for key and color")
        void shouldAllowNullFields() {
            MessageColor messageColor = new MessageColor(null, null);
            assertNull(messageColor.key());
            assertNull(messageColor.color());
        }

        @Test
        @DisplayName("toString should contain key and color values")
        void shouldContainFieldsInToString() {
            MessageColor messageColor = new MessageColor("accent", "#AABBCC");
            String toString = messageColor.toString();

            assertNotNull(toString);
            assertTrue(toString.contains("accent"), "toString should contain key");
            assertTrue(toString.contains("#AABBCC"), "toString should contain color");
        }
    }
}
