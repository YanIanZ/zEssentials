package dev.yanianz.essentials.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

class ConfigHealerTest {

    @TempDir
    File tempDir;

    @Test
    @DisplayName("Missing keys from default are merged into existing user config")
    void testMissingKeysMerged() throws IOException {
        String userConfig = "enable: true\nold-key: 42\n";
        String defaultConfig = "config-version: 2\nenable: true\nnew-key: hello\nold-key: 42\n";

        File target = new File(tempDir, "config.yml");
        Files.writeString(target.toPath(), userConfig);

        boolean changed = ConfigHealer.heal(new ByteArrayInputStream(defaultConfig.getBytes()), target);

        assertTrue(changed, "File should be updated with missing keys");
        String result = Files.readString(target.toPath());
        assertTrue(result.contains("new-key: hello"), "New key should be added");
        assertTrue(result.contains("old-key: 42"), "Existing key should be preserved");
        assertTrue(result.contains("enable: true"), "Existing key should be preserved");
        assertTrue(result.contains("config-version: 2"), "Version should be added");
    }

    @Test
    @DisplayName("Config with matching version is not modified")
    void testVersionMatchSkipsHeal() throws IOException {
        String userConfig = "config-version: 2\nenable: true\n";
        String defaultConfig = "config-version: 2\nenable: true\nnew-key: hello\n";

        File target = new File(tempDir, "config.yml");
        Files.writeString(target.toPath(), userConfig);

        boolean changed = ConfigHealer.heal(new ByteArrayInputStream(defaultConfig.getBytes()), target);

        assertFalse(changed, "File should not be updated when version matches");
    }

    @Test
    @DisplayName("Comments in user config are preserved")
    void testCommentsPreserved() throws IOException {
        String userConfig = "# My custom comment\nenable: true\n# Another comment\nold-key: 42\n";
        String defaultConfig = "config-version: 1\nenable: true\nnew-key: hello\n";

        File target = new File(tempDir, "config.yml");
        Files.writeString(target.toPath(), userConfig);

        boolean changed = ConfigHealer.heal(new ByteArrayInputStream(defaultConfig.getBytes()), target);

        assertTrue(changed);
        String result = Files.readString(target.toPath());
        assertTrue(result.contains("# My custom comment"), "User comments should be preserved");
        assertTrue(result.contains("# Another comment"), "User comments should be preserved");
        assertTrue(result.contains("new-key: hello"), "New key should be added");
    }

    @Test
    @DisplayName("User customizations are never overwritten")
    void testUserValuesPreserved() throws IOException {
        String userConfig = "enable: false\ncustom-setting: my-value\n";
        String defaultConfig = "config-version: 1\nenable: true\ncustom-setting: default-value\nnew-key: hello\n";

        File target = new File(tempDir, "config.yml");
        Files.writeString(target.toPath(), userConfig);

        boolean changed = ConfigHealer.heal(new ByteArrayInputStream(defaultConfig.getBytes()), target);

        assertTrue(changed);
        String result = Files.readString(target.toPath());
        assertTrue(result.contains("enable: false"), "User value should be preserved, not overwritten with default");
        assertTrue(result.contains("custom-setting: my-value"), "User value should be preserved");
        assertFalse(result.contains("custom-setting: default-value"), "Default should not overwrite user value");
        assertTrue(result.contains("new-key: hello"), "Missing key should be added");
    }
}
