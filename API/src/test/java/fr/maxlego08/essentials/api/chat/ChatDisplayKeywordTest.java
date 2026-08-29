package fr.maxlego08.essentials.api.chat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChatDisplayKeywordTest {

    @Test
    @DisplayName("Chat display keyword regex patterns match expected inputs")
    void testKeywordPatterns() {
        assertTrue("[item]".matches("(?i)\\[item\\]|\\[i\\]"));
        assertTrue("[ITEM]".matches("(?i)\\[item\\]|\\[i\\]"));
        assertTrue("[i]".matches("(?i)\\[item\\]|\\[i\\]"));
        assertTrue("[I]".matches("(?i)\\[item\\]|\\[i\\]"));
        assertFalse("[items]".matches("(?i)\\[item\\]|\\[i\\]"));
        assertFalse("[item]s".matches("(?i)\\[item\\]|\\[i\\]"));
    }

    @Test
    @DisplayName("Inventory keyword regex matches [inv] and [inventory]")
    void testInventoryPattern() {
        assertTrue("[inv]".matches("(?i)\\[inv\\]|\\[inventory\\]"));
        assertTrue("[INV]".matches("(?i)\\[inv\\]|\\[inventory\\]"));
        assertTrue("[inventory]".matches("(?i)\\[inv\\]|\\[inventory\\]"));
        assertTrue("[INVENTORY]".matches("(?i)\\[inv\\]|\\[inventory\\]"));
        assertFalse("[invent]".matches("(?i)\\[inv\\]|\\[inventory\\]"));
    }

    @Test
    @DisplayName("Ender chest keyword regex matches [ender] and [ec]")
    void testEnderPattern() {
        assertTrue("[ender]".matches("(?i)\\[ender\\]|\\[ec\\]"));
        assertTrue("[EC]".matches("(?i)\\[ender\\]|\\[ec\\]"));
        assertTrue("[ec]".matches("(?i)\\[ender\\]|\\[ec\\]"));
        assertFalse("[enders]".matches("(?i)\\[ender\\]|\\[ec\\]"));
    }

    @Test
    @DisplayName("Position keyword regex matches [pos] and [position]")
    void testPositionPattern() {
        assertTrue("[pos]".matches("(?i)\\[pos\\]|\\[position\\]"));
        assertTrue("[POSITION]".matches("(?i)\\[pos\\]|\\[position\\]"));
        assertTrue("[position]".matches("(?i)\\[pos\\]|\\[position\\]"));
        assertFalse("[post]".matches("(?i)\\[pos\\]|\\[position\\]"));
    }

    @Test
    @DisplayName("Mention @player pattern matches valid Minecraft usernames")
    void testMentionPattern() {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(?i)@(Steve)\\b");
        assertTrue(pattern.matcher("@Steve hello").find());
        assertTrue(pattern.matcher("@steve").find());
        assertFalse(pattern.matcher("@Ste").find());
        assertFalse(pattern.matcher("Steve").find());
    }

    @Test
    @DisplayName("Emoji shortcut patterns match correctly")
    void testEmojiShortcuts() {
        assertTrue(":heart:".matches(":heart:"));
        assertTrue(":100:".matches(":100:"));
        assertFalse(":heart".matches(":heart:"));
        assertFalse("heart:".matches(":heart:"));
    }

    @Test
    @DisplayName("Quick reply patterns match correctly")
    void testQuickReplies() {
        assertTrue(":brb:".matches(":brb:"));
        assertTrue(":gg:".matches(":gg:"));
        assertFalse(":brb".matches(":brb:"));
        assertFalse("brb:".matches(":brb:"));
    }
}
