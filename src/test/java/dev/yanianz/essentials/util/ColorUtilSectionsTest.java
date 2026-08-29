package dev.yanianz.essentials.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ColorUtilSectionsTest {

    @Test
    @DisplayName("Legacy ampersand codes convert to section codes")
    void testLegacyCodes() {
        assertEquals("§aHello", ColorUtil.sections("&aHello"));
        assertEquals("§c§lBold Red", ColorUtil.sections("&c&lBold Red"));
        assertEquals("§r§7Gray", ColorUtil.sections("&r&7Gray"));
    }

    @Test
    @DisplayName("Hex colors &#RRGGBB convert to §x§r§r§g§g§b§b")
    void testHexCodes() {
        String result = ColorUtil.sections("&#ff4d4d");
        assertEquals("§x§f§f§4§d§4§d", result);
    }

    @Test
    @DisplayName("Report alert pattern renders hex prefix correctly")
    void testReportAlertPattern() {
        String input = "&8[&#ff4d4d&lREPORT&8] &fPlayer &7reported &fTarget";
        String result = ColorUtil.sections(input);
        assertTrue(result.contains("§x§f§f§4§d§4§d"), "Hex red should be expanded");
        assertTrue(result.contains("§lREPORT"), "Bold REPORT should follow hex");
        assertTrue(result.contains("§8["), "Dark gray bracket preserved");
        assertFalse(result.contains("&#"), "No raw &# should remain");
    }

    @Test
    @DisplayName("Multiple hex codes in one string all convert")
    void testMultipleHex() {
        String input = "&#ff4d4d and &#00ff00";
        String result = ColorUtil.sections(input);
        assertTrue(result.contains("§x§f§f§4§d§4§d"));
        assertTrue(result.contains("§x§0§0§f§f§0§0"));
        assertFalse(result.contains("&#"));
    }

    @Test
    @DisplayName("Mixed hex and legacy codes")
    void testMixedCodes() {
        String input = "&#ff4d4d&lHello &7World";
        String result = ColorUtil.sections(input);
        assertTrue(result.contains("§x§f§f§4§d§4§d"));
        assertTrue(result.contains("§lHello"));
        assertTrue(result.contains("§7World"));
    }

    @Test
    @DisplayName("Null input returns empty string")
    void testNull() {
        assertEquals("", ColorUtil.sections(null));
    }

    @Test
    @DisplayName("Empty string returns empty")
    void testEmpty() {
        assertEquals("", ColorUtil.sections(""));
    }

    @Test
    @DisplayName("Plain text without codes is unchanged")
    void testPlainText() {
        assertEquals("Hello World", ColorUtil.sections("Hello World"));
    }

    @Test
    @DisplayName("Uppercase hex codes convert with lowercase output")
    void testUppercaseHex() {
        String result = ColorUtil.sections("&#FF4D4D");
        assertEquals("§x§f§f§4§d§4§d", result);
    }

    @Test
    @DisplayName("Broken colorize (simple & to §) would fail this — ColorUtil must not produce §#")
    void testNoBrokenSectionHash() {
        String result = ColorUtil.sections("&#ff4d4d test");
        assertFalse(result.contains("§#"), "§# is invalid legacy hex and must never appear in output");
    }
}
