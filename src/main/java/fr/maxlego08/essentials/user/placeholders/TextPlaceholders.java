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
}
