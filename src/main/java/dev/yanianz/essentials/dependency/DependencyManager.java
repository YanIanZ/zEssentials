package dev.yanianz.essentials.dependency;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Detects, downloads and loads runtime dependencies without restarting the server.
 * A dependency that is already available on the classpath or already present in the
 * cache folder is never downloaded again.
 */
public final class DependencyManager {

    private static final DependencyManager INSTANCE = new DependencyManager();

    private final java.util.logging.Logger logger = java.util.logging.Logger.getLogger("zEssentials");

    private DependencyManager() {
    }

    public static DependencyManager getInstance() {
        return INSTANCE;
    }

    public enum Status {
        /**
         * The test class of the dependency is already loadable, nothing was done.
         */
        ALREADY_AVAILABLE,

        /**
         * The jar was found in the cache folder and pushed to the classpath.
         */
        LOADED_FROM_CACHE,

        /**
         * The jar was downloaded from a repository and pushed to the classpath.
         */
        DOWNLOADED,

        /**
         * Every repository failed or no published checksum matched the download.
         */
        FAILED
    }

    /**
     * Makes the dependency usable: detects it on the classpath, restores it from
     * the cache folder or downloads it from its repositories, then injects the jar
     * into the classloader of the plugin so it can be used immediately.
     *
     * @param dataFolder  the plugin data folder, used as cache location (libs subfolder)
     * @param classLoader the classloader of the plugin
     * @param dependency  the dependency to resolve
     * @return what happened, never null
     */
    public Status resolve(java.nio.file.Path dataFolder, ClassLoader classLoader, MavenDependency dependency) {

        // 1. Detection: the classes are already available through another source
        //    (plugin.yml libraries, shaded jar or another plugin), skip everything
        if (isClassPresent(classLoader, dependency)) {
            return Status.ALREADY_AVAILABLE;
        }

        Path cacheFolder = cacheFolder(dataFolder);
        File cacheFile = cacheFolder.resolve(dependency.relativePath()).toFile();

        // 2. The jar is not cached yet, download and verify it
        boolean downloaded = false;
        if (!cacheFile.exists() || cacheFile.length() <= 0) {
            this.logger.info("Downloading dependency " + dependency + "...");
            Path downloadedPath = DependencyDownloader.download(cacheFolder, dependency).orElse(null);

            if (downloadedPath == null) {
                this.logger.severe("Unable to download dependency " + dependency + ", check your internet connection.");
                return Status.FAILED;
            }
            downloaded = true;
        }

        // 3. Push the cached or downloaded jar into the running classloader
        if (ClassPathInjector.inject(classLoader, cacheFile)) {
            return downloaded ? Status.DOWNLOADED : Status.LOADED_FROM_CACHE;
        }

        this.logger.severe("Unable to push " + dependency + " into the classpath of the server.");
        return Status.FAILED;
    }

    private boolean isClassPresent(ClassLoader classLoader, MavenDependency dependency) {
        String testClass = dependency.testClass();
        if (testClass == null || testClass.isEmpty()) return false;

        try {
            Class.forName(testClass, false, classLoader);
            return true;
        } catch (ClassNotFoundException exception) {
            return false;
        }
    }

    private Path cacheFolder(Path dataFolder) {
        Path folder = dataFolder.resolve("libs");
        try {
            Files.createDirectories(folder);
        } catch (Exception ignored) {
        }
        return folder;
    }
}
