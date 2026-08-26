package dev.yanianz.essentials.dependency;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.Optional;

/**
 * Downloads a dependency from its repositories into the cache folder.
 * The download is verified against the repository checksums and written
 * atomically, a failed download never leaves a broken jar behind.
 */
public final class DependencyDownloader {

    private static final int CONNECT_TIMEOUT = 5000;
    private static final int READ_TIMEOUT = 30000;

    private DependencyDownloader() {
    }

    /**
     * Downloads the dependency into the given cache folder, trying every repository in order.
     *
     * @return the path of the downloaded jar, or empty if every repository failed or
     * a published checksum didn't match the downloaded content.
     */
    public static Optional<Path> download(Path cacheFolder, MavenDependency dependency) {
        return dependency.repositories().stream()
                .map(repository -> tryRepository(cacheFolder, dependency, repository))
                .flatMap(Optional::stream)
                .findFirst();
    }

    private static Optional<Path> tryRepository(Path cacheFolder, MavenDependency dependency, String repository) {
        Path target = cacheFolder.resolve(dependency.relativePath());
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");

        try {
            Files.createDirectories(target.getParent());

            downloadToFile(dependency.url(repository), temporary);
            verifyOrDelete(temporary, dependency);

            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            return Optional.of(target);
        } catch (Exception exception) {
            deleteSilently(temporary);
            return Optional.empty();
        }
    }

    /**
     * Rejects the file when the repository publishes a checksum that doesn't match it,
     * repositories without any checksum are accepted like in Intave's loader.
     */
    private static void verifyOrDelete(Path file, MavenDependency dependency) throws IOException {
        if (DependencyVerifier.verify(file, dependency) == DependencyVerifier.Result.NO_MATCH) {
            deleteSilently(file);
            throw new IOException("Checksum mismatch for " + dependency);
        }
    }

    private static void downloadToFile(String url, Path target) throws IOException {
        URLConnection connection = URI.create(url).toURL().openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT);
        connection.setReadTimeout(READ_TIMEOUT);
        connection.setRequestProperty("User-Agent", "zEssentials-DependencyLoader");

        try (InputStream inputStream = connection.getInputStream()) {
            byte[] buffer = new byte[8192];
            int length;
            try (var output = Files.newOutputStream(target)) {
                while ((length = inputStream.read(buffer)) > 0) {
                    output.write(buffer, 0, length);
                }
            }
        }
    }

    private static void deleteSilently(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }
}
