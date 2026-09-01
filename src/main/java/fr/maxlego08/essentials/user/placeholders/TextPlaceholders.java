package fr.maxlego08.essentials.user.placeholders;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.placeholders.Placeholder;
import fr.maxlego08.essentials.api.placeholders.PlaceholderRegister;

public class TextPlaceholders implements PlaceholderRegister {

    private static final String[] FANCY = buildMap(
        "𝓪𝓫𝓬𝓭𝓮𝓯𝓰𝓱𝓲𝓳𝓴𝓵𝓶𝓷𝓸𝓹𝓺𝓻𝓼𝓽𝓾𝓿𝔀𝔁𝔂𝔃",
        "𝓐𝓑𝓒𝓓𝓔𝓕𝓖𝓗𝓘𝓙𝓚𝓛𝓜𝓝𝓞𝓟𝓠𝓡𝓢𝓣𝓤𝓥𝓦𝓧𝓨𝓩",
        "𝟎𝟏𝟐𝟑𝟒𝟓𝟔𝟕𝟖𝟗");

    private static final String[] BOLD = buildMap(
        "𝐚𝐛𝐜𝐝𝐞𝐟𝐠𝐡𝐢𝐣𝐤𝐥𝐦𝐧𝐨𝐩𝐪𝐫𝐬𝐭𝐮𝐯𝐰𝐱𝐲𝐳",
        "𝐀𝐁𝐂𝐃𝐄𝐅𝐆𝐇𝐈𝐉𝐊𝐋𝐌𝐍𝐎𝐏𝐐𝐑𝐒𝐓𝐔𝐕𝐖𝐗𝐘𝐙",
        "𝟎𝟏𝟐𝟑𝟒𝟓𝟔𝟕𝟖𝟗");

    private static final String[] ITALIC = buildMap(
        "𝑎𝑏𝑐𝑑𝑒𝑓𝑔ℎ𝑖𝑗𝑘𝑙𝑚𝑛𝑜𝑝𝑞𝑟𝑠𝑡𝑢𝑣𝑤𝑥𝑦𝑧",
        "𝐴𝐵𝐶𝐷𝐸𝐹𝐺𝐻𝐼𝐽𝐾𝐿𝑀𝑁𝑂𝑃𝑄𝑅𝑆𝑇𝑈𝑉𝑊𝑋𝑌𝑍",
        "𝟎𝟏𝟐𝟑𝟒𝟓𝟔𝟕𝟖𝟗");

    private static final String[] SMALLCAPS = buildMap(
        "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘǫʀsᴛᴜᴠᴡxʏᴢ",
        "ABCDEFGHIJKLMNOPQRSTUVWXYZ",
        "0123456789");

    private static final String[] MONOSPACE = buildMap(
        "𝚊𝚋𝚌𝚍𝚎𝚏𝚐𝚑𝚒𝚓𝚔𝚕𝚖𝚗𝚘𝚙𝚚𝚛𝚜𝚝𝚞𝚟𝚠𝚡𝚢𝚣",
        "𝙰𝙱𝙲𝙳𝙴𝙵𝙶𝙷𝙸𝙹𝙺𝙻𝙼𝙽𝙾𝙿𝚀𝚁𝚂𝚃𝚄𝚅𝚆𝚇𝚈𝚉",
        "𝟶𝟷𝟸𝟹𝟺𝟻𝟼𝟽𝟾𝟿");

    private static final String[] DOUBLE_STRUCK = buildMap(
        "𝕒𝕓𝕔𝕕𝕖𝕗𝕘𝕙𝕚𝕛𝕜𝕝𝕞𝕟𝕠𝕡𝕢𝕣𝕤𝕥𝕦𝕧𝕨𝕩𝕪𝕫",
        "𝔸𝔹ℂ𝔻𝔼𝔽𝔾ℍ𝕀𝕁𝕂𝕃𝕄ℕ𝕆ℙℚℝ𝕊𝕋𝕌𝕍𝕎𝕏𝕐ℤ",
        "𝟘𝟙𝟚𝟛𝟜𝟝𝟞𝟟𝟠𝟡");

    private static String[] buildMap(String lower, String upper, String digits) {
        String[] map = new String[62];
        for (int i = 0; i < 26; i++) map[i] = String.valueOf(lower.charAt(i));
        for (int i = 0; i < 26; i++) map[26 + i] = String.valueOf(upper.charAt(i));
        for (int i = 0; i < 10; i++) map[52 + i] = String.valueOf(digits.charAt(i));
        return map;
    }

    @Override
    public void register(Placeholder placeholder, EssentialsPlugin plugin) {
        placeholder.register("fancy_", (player, text) -> transform(text, FANCY),
                "Returns the text in fancy script Unicode letters", "text");
        placeholder.register("bold_", (player, text) -> transform(text, BOLD),
                "Returns the text in bold Unicode letters", "text");
        placeholder.register("italic_", (player, text) -> transform(text, ITALIC),
                "Returns the text in italic Unicode letters", "text");
        placeholder.register("smallcaps_", (player, text) -> transform(text, SMALLCAPS),
                "Returns the text in small caps Unicode letters", "text");
        placeholder.register("mono_", (player, text) -> transform(text, MONOSPACE),
                "Returns the text in monospace Unicode letters", "text");
        placeholder.register("double_", (player, text) -> transform(text, DOUBLE_STRUCK),
                "Returns the text in double-struck Unicode letters", "text");
        placeholder.register("roman_", (player, number) -> toRoman(safeInt(number)),
                "Converts a number to Roman numerals (I, II, III... max 3999)", "number");
        placeholder.register("gametime_", (player, ticks) -> ticksToTime(ticks),
                "Converts game ticks to 24h time format (HH:MM, 0 ticks = 06:00)", "ticks");
        placeholder.register("compactnum_", (player, value) -> compactNumber(value),
                "Formats large numbers compactly (1.5K, 2.3M, 3B)", "value");
    }

    private int safeInt(String s) {
        try { return Integer.parseInt(s.trim()); }
        catch (NumberFormatException e) { return 0; }
    }

    private String transform(String text, String[] mapping) {
        if (text == null || text.isEmpty()) return "";
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= 'a' && c <= 'z') {
                out.append(mapping[c - 'a']);
            } else if (c >= 'A' && c <= 'Z') {
                out.append(mapping[26 + c - 'A']);
            } else if (c >= '0' && c <= '9') {
                out.append(mapping[52 + c - '0']);
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    // ── Roman numeral placeholder ──
    private static final java.util.TreeMap<Integer, String> ROMAN_MAP = new java.util.TreeMap<>();
    static {
        ROMAN_MAP.put(1000, "M"); ROMAN_MAP.put(900, "CM"); ROMAN_MAP.put(500, "D"); ROMAN_MAP.put(400, "CD");
        ROMAN_MAP.put(100, "C");  ROMAN_MAP.put(90, "XC");  ROMAN_MAP.put(50, "L");  ROMAN_MAP.put(40, "XL");
        ROMAN_MAP.put(10, "X");   ROMAN_MAP.put(9, "IX");    ROMAN_MAP.put(5, "V");   ROMAN_MAP.put(4, "IV");
        ROMAN_MAP.put(1, "I");
    }

    private static String toRoman(int number) {
        if (number <= 0 || number > 3999) return String.valueOf(number);
        Integer key = ROMAN_MAP.floorKey(number);
        if (key == null) return String.valueOf(number);
        if (number == key) return ROMAN_MAP.get(key);
        return ROMAN_MAP.get(key) + toRoman(number - key);
    }

    // ── Game time placeholder (ticks → HH:MM 24h format) ──
    private static String ticksToTime(String ticksStr) {
        try {
            long ticks = Long.parseLong(ticksStr);
            ticks = ((ticks % 24000) + 24000) % 24000;
            long totalMinutes = (ticks * 24 * 60) / 24000;
            long hours = (6 + totalMinutes / 60) % 24;
            long minutes = totalMinutes % 60;
            return String.format("%02d:%02d", hours, minutes);
        } catch (NumberFormatException e) {
            return "00:00";
        }
    }

    // ── Compact number placeholder ──
    private static final String[] COMPACT_SUFFIXES = {"", "K", "M", "B", "T", "Q"};
    private static String compactNumber(String valueStr) {
        try {
            double value = Double.parseDouble(valueStr);
            if (value < 1000) return String.valueOf((long) value);
            int tier = 0;
            while (value >= 1000 && tier < COMPACT_SUFFIXES.length - 1) {
                value /= 1000;
                tier++;
            }
            return String.format("%.1f%s", value, COMPACT_SUFFIXES[tier]);
        } catch (NumberFormatException e) {
            return valueStr;
        }
    }
}
