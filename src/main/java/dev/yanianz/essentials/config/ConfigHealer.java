package dev.yanianz.essentials.config;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Self-healing config system that merges new keys from a jar resource into an
 * existing on-disk config file while preserving user customizations and comments.
 *
 * Instead of loading both files into YamlConfiguration (which strips comments
 * on save), this does a text-based merge: it reads the default config line by
 * line and appends any top-level or nested section that is missing from the
 * user's file. Existing user keys and comments are never modified or removed.
 *
 * A {@code config-version} key tracks the schema version. When the version in
 * the user's file is lower than the jar's default, the merge runs. When equal,
 * it is skipped (fast path).
 */
public final class ConfigHealer {

    private ConfigHealer() {}

    private static final Pattern SECTION_HEADER = Pattern.compile("^(\\s*)([a-zA-Z0-9_-]+):\\s*(.*)$", Pattern.MULTILINE);
    private static final Pattern CONFIG_VERSION = Pattern.compile("^(\\s*)config-version:\\s*(\\d+)\\s*$", Pattern.MULTILINE);

    /**
     * Merges the default config from the jar into the user's file on disk.
     *
     * @param defaultStream  the jar resource input stream
     * @param targetFile     the on-disk config file (must exist)
     * @return true if the file was updated
     */
    public static boolean heal(InputStream defaultStream, File targetFile) throws IOException {
        String defaultContent = readStream(defaultStream);
        String userContent = readFile(targetFile);

        int defaultVersion = extractVersion(defaultContent);
        int userVersion = extractVersion(userContent);

        if (defaultVersion > 0 && userVersion > 0 && userVersion >= defaultVersion) {
            return false;
        }

        String merged = mergeText(defaultContent, userContent, defaultVersion, userVersion);

        if (!merged.equals(userContent)) {
            writeFile(targetFile, merged);
            return true;
        }

        return false;
    }

    private static int extractVersion(String content) {
        Matcher m = CONFIG_VERSION.matcher(content);
        if (m.find()) {
            try { return Integer.parseInt(m.group(2)); } catch (Exception e) { return 0; }
        }
        return 0;
    }

    /**
     * Text-based merge: appends any missing top-level keys or sections from the
     * default config to the end of the user's file, preserving all existing
     * content and comments.
     */
    private static String mergeText(String defaultContent, String userContent, int defaultVersion, int userVersion) {
        Map<String, String> defaultSections = extractTopLevelSections(defaultContent);
        Map<String, String> userSections = extractTopLevelSections(userContent);

        StringBuilder result = new StringBuilder(userContent);

        if (!result.toString().endsWith("\n") && !result.toString().isEmpty()) {
            result.append("\n");
        }

        boolean added = false;
        for (Map.Entry<String, String> entry : defaultSections.entrySet()) {
            String key = entry.getKey();
            if (!userSections.containsKey(key)) {
                if (added) result.append("\n");
                result.append(entry.getValue());
                added = true;
            }
        }

        if (defaultVersion > 0) {
            String versionLine = "config-version: " + defaultVersion;
            Matcher m = CONFIG_VERSION.matcher(result);
            if (m.find()) {
                result = new StringBuilder(m.replaceFirst(versionLine));
            } else {
                result.insert(0, versionLine + "\n\n");
            }
        }

        return result.toString();
    }

    /**
     * Splits a YAML file into top-level sections keyed by their root key name.
     * Each section includes its preceding comments and the full block of text
     * until the next top-level key.
     */
    private static Map<String, String> extractTopLevelSections(String content) {
        Map<String, String> sections = new LinkedHashMap<>();
        String[] lines = content.split("\n");
        StringBuilder current = new StringBuilder();
        String currentKey = null;
        boolean inComment = false;

        for (String line : lines) {
            boolean isTopLevel = isTopLevelKey(line);

            if (isTopLevel) {
                if (currentKey != null) {
                    sections.put(currentKey, current.toString());
                }
                current = new StringBuilder();
                Matcher m = SECTION_HEADER.matcher(line);
                if (m.find()) {
                    currentKey = m.group(2);
                }
                inComment = false;
            }

            current.append(line).append("\n");
        }

        if (currentKey != null) {
            sections.put(currentKey, current.toString());
        }

        return sections;
    }

    private static boolean isTopLevelKey(String line) {
        if (line.isEmpty() || line.startsWith(" ") || line.startsWith("\t") || line.startsWith("#")) {
            return false;
        }
        Matcher m = SECTION_HEADER.matcher(line);
        return m.matches();
    }

    private static String readStream(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        return out.toString(StandardCharsets.UTF_8);
    }

    private static String readFile(File file) throws IOException {
        return Files.readString(file.toPath(), StandardCharsets.UTF_8);
    }

    private static void writeFile(File file, String content) throws IOException {
        Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
    }
}
