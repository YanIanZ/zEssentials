package dev.yanianz.essentials.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConfigVersionCoverageTest {

    @Test
    @DisplayName("All module config files have a config-version key")
    void testAllModuleConfigsHaveVersion() {
        File modulesDir = new File("src/main/resources/modules");
        assertTrue(modulesDir.exists(), "modules directory should exist");

        List<String> missing = new ArrayList<>();
        collectConfigsWithoutVersion(modulesDir, missing);

        assertTrue(missing.isEmpty(),
                "These module configs are missing config-version:\n" + String.join("\n", missing));
    }

    @Test
    @DisplayName("Main config.yml has a config-version key")
    void testMainConfigHasVersion() {
        File mainConfig = new File("src/main/resources/config.yml");
        assertTrue(mainConfig.exists(), "config.yml should exist");

        try {
            String content = java.nio.file.Files.readString(mainConfig.toPath());
            assertTrue(content.contains("config-version:"),
                    "Main config.yml must have a config-version key for self-healing");
        } catch (Exception e) {
            fail("Failed to read config.yml: " + e.getMessage());
        }
    }

    private void collectConfigsWithoutVersion(File dir, List<String> missing) {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                collectConfigsWithoutVersion(file, missing);
            } else if (file.getName().equals("config.yml")) {
                try {
                    String content = java.nio.file.Files.readString(file.toPath());
                    if (!content.contains("config-version:")) {
                        missing.add(file.getPath());
                    }
                } catch (Exception e) {
                    missing.add(file.getPath() + " (read error: " + e.getMessage() + ")");
                }
            }
        }
    }
}
