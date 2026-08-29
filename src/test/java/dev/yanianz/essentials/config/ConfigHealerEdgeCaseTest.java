package dev.yanianz.essentials.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConfigHealerEdgeCaseTest {

    @TempDir
    File tempDir;

    @Test
    @DisplayName("Older config version triggers heal even when all keys exist")
    void testOlderVersionTriggersHeal() throws IOException {
        String userConfig = "config-version: 1\nenable: true\nkey: value\n";
        String defaultConfig = "config-version: 2\nenable: true\nkey: value\n";

        File target = new File(tempDir, "config.yml");
        Files.writeString(target.toPath(), userConfig);

        boolean changed = ConfigHealer.heal(new ByteArrayInputStream(defaultConfig.getBytes()), target);

        assertTrue(changed, "Should heal when user version is lower than default");
        String result = Files.readString(target.toPath());
        assertTrue(result.contains("config-version: 2"), "Version should be updated to 2");
    }

    @Test
    @DisplayName("No version in either file — heal adds missing keys and version")
    void testNoVersionEitherSide() throws IOException {
        String userConfig = "enable: true\nexisting-key: kept\n";
        String defaultConfig = "enable: true\nexisting-key: kept\nnew-key: added\n";

        File target = new File(tempDir, "config.yml");
        Files.writeString(target.toPath(), userConfig);

        boolean changed = ConfigHealer.heal(new ByteArrayInputStream(defaultConfig.getBytes()), target);

        assertTrue(changed);
        String result = Files.readString(target.toPath());
        assertTrue(result.contains("new-key: added"), "Missing key should be appended");
        assertTrue(result.contains("existing-key: kept"), "Existing key preserved");
    }

    @Test
    @DisplayName("Multiple missing sections are all appended")
    void testMultipleMissingSections() throws IOException {
        String userConfig = "enable: true\n";
        String defaultConfig = "config-version: 1\nenable: true\nsection1:\n  key1: val1\nsection2:\n  key2: val2\nsection3:\n  key3: val3\n";

        File target = new File(tempDir, "config.yml");
        Files.writeString(target.toPath(), userConfig);

        boolean changed = ConfigHealer.heal(new ByteArrayInputStream(defaultConfig.getBytes()), target);

        assertTrue(changed);
        String result = Files.readString(target.toPath());
        assertTrue(result.contains("section1:"), "Section1 should be added");
        assertTrue(result.contains("section2:"), "Section2 should be added");
        assertTrue(result.contains("section3:"), "Section3 should be added");
        assertTrue(result.contains("key1: val1"), "Nested key preserved in section1");
    }

    @Test
    @DisplayName("Comments in default config are included in appended sections")
    void testCommentsInAppendedSections() throws IOException {
        String userConfig = "enable: true\n";
        String defaultConfig = "config-version: 1\nenable: true\nnew-section:\n  # Comment inside section\n  key: value\n";

        File target = new File(tempDir, "config.yml");
        Files.writeString(target.toPath(), userConfig);

        boolean changed = ConfigHealer.heal(new ByteArrayInputStream(defaultConfig.getBytes()), target);

        assertTrue(changed);
        String result = Files.readString(target.toPath());
        assertTrue(result.contains("new-section:"), "New section should be added");
        assertTrue(result.contains("# Comment inside section"), "Comments inside appended sections should be preserved");
        assertTrue(result.contains("key: value"), "Keys in appended section should be present");
    }

    @Test
    @DisplayName("User config with only comments is handled correctly")
    void testOnlyComments() throws IOException {
        String userConfig = "# Just a comment\n# Another comment\n";
        String defaultConfig = "config-version: 1\nenable: true\n";

        File target = new File(tempDir, "config.yml");
        Files.writeString(target.toPath(), userConfig);

        boolean changed = ConfigHealer.heal(new ByteArrayInputStream(defaultConfig.getBytes()), target);

        assertTrue(changed);
        String result = Files.readString(target.toPath());
        assertTrue(result.contains("# Just a comment"), "Original comments preserved");
        assertTrue(result.contains("enable: true"), "Missing key added");
    }

    @Test
    @DisplayName("Empty user file gets all default content")
    void testEmptyUserFile() throws IOException {
        String userConfig = "";
        String defaultConfig = "config-version: 1\nenable: true\nkey: value\n";

        File target = new File(tempDir, "config.yml");
        Files.writeString(target.toPath(), userConfig);

        boolean changed = ConfigHealer.heal(new ByteArrayInputStream(defaultConfig.getBytes()), target);

        assertTrue(changed);
        String result = Files.readString(target.toPath());
        assertTrue(result.contains("enable: true"));
        assertTrue(result.contains("key: value"));
        assertTrue(result.contains("config-version: 1"));
    }

    @Test
    @DisplayName("Identical configs with matching version — no change")
    void testIdenticalConfigsWithVersion() throws IOException {
        String config = "config-version: 3\nenable: true\nkey: value\n";
        File target = new File(tempDir, "config.yml");
        Files.writeString(target.toPath(), config);

        boolean changed = ConfigHealer.heal(new ByteArrayInputStream(config.getBytes()), target);

        assertFalse(changed, "Should not change when configs are identical with matching version");
    }

    @Test
    @DisplayName("Higher user version than default — no change (downgrade protection)")
    void testHigherUserVersion() throws IOException {
        String userConfig = "config-version: 5\nenable: true\n";
        String defaultConfig = "config-version: 3\nenable: true\nnew-key: hello\n";

        File target = new File(tempDir, "config.yml");
        Files.writeString(target.toPath(), userConfig);

        boolean changed = ConfigHealer.heal(new ByteArrayInputStream(defaultConfig.getBytes()), target);

        assertFalse(changed, "Should not heal when user version is higher than default");
    }
}
