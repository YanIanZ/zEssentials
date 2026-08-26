package dev.yanianz.essentials.dependency;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Verifies the integrity of a downloaded dependency against the checksum files
 * published next to it inside the repository (.sha256, .sha1 and .md5).
 */
public final class DependencyVerifier {

    private static final int CONNECT_TIMEOUT = 5000;
    private static final int READ_TIMEOUT = 10000;

    private static final Pattern SHA256_PATTERN = Pattern.compile("(?i)(?<![0-9a-f])[0-9a-f]{64}(?![0-9a-f])");
    private static final Pattern SHA1_PATTERN = Pattern.compile("(?i)(?<![0-9a-f])[0-9a-f]{40}(?![0-9a-f])");
    private static final Pattern MD5_PATTERN = Pattern.compile("(?i)(?<![0-9a-f])[0-9a-f]{32}(?![0-9a-f])");

    private DependencyVerifier() {
    }

    public enum Result {
        MATCH,
        NO_HASH,
        NO_MATCH
    }

    /**
     * Verifies the file against the checksums published in the repository.
     *
     * @return MATCH if one of the checksums matches the file content,
     * NO_HASH if the repository publishes no checksum at all,
     * NO_MATCH if a checksum exists but doesn't match.
     */
    public static Result verify(Path file, MavenDependency dependency) {
        for (Algorithm algorithm : Algorithm.values()) {
            String expected = fetchChecksum(dependency.checksumUrl(algorithm.repositoryName(), algorithm.extension()), algorithm);
            if (expected == null) continue;

            try {
                String actual = digestHex(algorithm.digest(), file);
                return expected.equalsIgnoreCase(actual) ? Result.MATCH : Result.NO_MATCH;
            } catch (IOException exception) {
                return Result.NO_MATCH;
            }
        }
        return Result.NO_HASH;
    }

    /**
     * Fetches a checksum from the repository and extracts its hexadecimal value,
     * maven checksum files may contain the filename after the hash.
     */
    private static String fetchChecksum(String url, Algorithm algorithm) {
        try (InputStream inputStream = openStream(url)) {
            String content = new String(inputStream.readAllBytes(), StandardCharsets.US_ASCII);
            Matcher matcher = algorithm.pattern().matcher(content);
            return matcher.find() ? matcher.group() : null;
        } catch (Exception exception) {
            return null;
        }
    }

    private static InputStream openStream(String url) throws IOException {
        URLConnection connection = URI.create(url).toURL().openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT);
        connection.setReadTimeout(READ_TIMEOUT);
        return connection.getInputStream();
    }

    private static String digestHex(MessageDigest digest, Path file) throws IOException {
        try (InputStream inputStream = Files.newInputStream(file)) {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                digest.update(buffer, 0, length);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    public enum Algorithm {
        SHA_256("sha256", "SHA-256", SHA256_PATTERN),
        SHA_1("sha1", "SHA-1", SHA1_PATTERN),
        MD5("md5", "MD5", MD5_PATTERN);

        private final String extension;
        private final String name;
        private final Pattern pattern;

        Algorithm(String extension, String name, Pattern pattern) {
            this.extension = extension;
            this.name = name;
            this.pattern = pattern;
        }

        public String extension() {
            return this.extension;
        }

        public String repositoryName() {
            return this.extension;
        }

        public Pattern pattern() {
            return this.pattern;
        }

        public MessageDigest digest() {
            try {
                return MessageDigest.getInstance(this.name);
            } catch (NoSuchAlgorithmException exception) {
                throw new IllegalStateException(exception);
            }
        }

        public String lowerName() {
            return this.name.toLowerCase(Locale.ROOT);
        }
    }
}
