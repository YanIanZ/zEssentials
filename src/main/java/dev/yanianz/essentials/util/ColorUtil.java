package dev.yanianz.essentials.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts config color conventions into adventure components,
 * centralizing the formatting every new feature uses.
 */

public final class ColorUtil {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();
    private static final Pattern AMP_HEX = Pattern.compile("&#([0-9a-fA-F]{6})");

    private ColorUtil() {
    }

    /**
     * Turns a raw config line into a component: hex codes written with an
     * ampersand hash prefix become rgb, legacy ampersand codes become
     * section codes, mini message is untouched.
     */

    public static Component component(String text) {
        return LEGACY.deserialize(sections(text));
    }

    /**
     * Returns the line ready for plain string apis expecting legacy codes,
     * including proper §x§r§r§g§g§b§b hex sequences.
     */
    public static String sections(String text) {

        if (text == null) return "";

        // Convert the #hex convention first, then map remaining & codes
        Matcher matcher = AMP_HEX.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            StringBuilder escaped = new StringBuilder("§x");
            for (char c : matcher.group(1).toCharArray()) {
                escaped.append('§').append(Character.toLowerCase(c));
            }
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(escaped.toString()));
        }
        matcher.appendTail(buffer);

        return buffer.toString().replace('&', '§');
    }
}
