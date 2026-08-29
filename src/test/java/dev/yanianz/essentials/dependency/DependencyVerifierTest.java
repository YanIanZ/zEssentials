package dev.yanianz.essentials.dependency;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.security.MessageDigest;
import java.util.regex.Matcher;

import static org.junit.jupiter.api.Assertions.*;

class DependencyVerifierTest {

    @Test
    @DisplayName("Result enum contains exactly MATCH, NO_HASH, and NO_MATCH")
    void testResultEnumConstants() {
        DependencyVerifier.Result[] results = DependencyVerifier.Result.values();
        assertEquals(3, results.length);
        assertEquals(DependencyVerifier.Result.MATCH, DependencyVerifier.Result.valueOf("MATCH"));
        assertEquals(DependencyVerifier.Result.NO_HASH, DependencyVerifier.Result.valueOf("NO_HASH"));
        assertEquals(DependencyVerifier.Result.NO_MATCH, DependencyVerifier.Result.valueOf("NO_MATCH"));
    }

    @Test
    @DisplayName("Algorithm enum contains SHA_256, SHA_1, and MD5")
    void testAlgorithmEnumConstants() {
        DependencyVerifier.Algorithm[] algorithms = DependencyVerifier.Algorithm.values();
        assertEquals(3, algorithms.length);
        assertEquals(DependencyVerifier.Algorithm.SHA_256, DependencyVerifier.Algorithm.valueOf("SHA_256"));
        assertEquals(DependencyVerifier.Algorithm.SHA_1, DependencyVerifier.Algorithm.valueOf("SHA_1"));
        assertEquals(DependencyVerifier.Algorithm.MD5, DependencyVerifier.Algorithm.valueOf("MD5"));
    }

    @Test
    @DisplayName("SHA_256 pattern matches valid 64-character hex strings")
    void testSha256PatternMatchesValid() {
        String hexLower = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
        String hexUpper = "E3B0C44298FC1C149AFBF4C8996FB92427AE41E4649B934CA495991B7852B855";
        String inChecksumFile = hexLower + "  mariadb-java-client-3.5.6.jar\n";

        Matcher lowerMatcher = DependencyVerifier.Algorithm.SHA_256.pattern().matcher(hexLower);
        assertTrue(lowerMatcher.find());
        assertEquals(hexLower, lowerMatcher.group());

        Matcher upperMatcher = DependencyVerifier.Algorithm.SHA_256.pattern().matcher(hexUpper);
        assertTrue(upperMatcher.find());
        assertEquals(hexUpper, upperMatcher.group());

        Matcher fileMatcher = DependencyVerifier.Algorithm.SHA_256.pattern().matcher(inChecksumFile);
        assertTrue(fileMatcher.find());
        assertEquals(hexLower, fileMatcher.group());
    }

    @Test
    @DisplayName("SHA_256 pattern does not match 63 or 65-character hex strings or invalid hex")
    void testSha256PatternRejectsInvalid() {
        String hex63 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b85";
        String hex65 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855a";
        String nonHex = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b85z";

        assertFalse(DependencyVerifier.Algorithm.SHA_256.pattern().matcher(hex63).find());
        assertFalse(DependencyVerifier.Algorithm.SHA_256.pattern().matcher(hex65).find());
        assertFalse(DependencyVerifier.Algorithm.SHA_256.pattern().matcher(nonHex).find());
    }

    @Test
    @DisplayName("SHA_1 pattern matches valid 40-character hex strings")
    void testSha1PatternMatchesValid() {
        String hexLower = "da39a3ee5e6b4b0d3255bfef95601890afd80709";
        String hexUpper = "DA39A3EE5E6B4B0D3255BFEF95601890AFD80709";
        String inChecksumFile = hexLower + "  artifact-1.0.jar";

        Matcher lowerMatcher = DependencyVerifier.Algorithm.SHA_1.pattern().matcher(hexLower);
        assertTrue(lowerMatcher.find());
        assertEquals(hexLower, lowerMatcher.group());

        Matcher upperMatcher = DependencyVerifier.Algorithm.SHA_1.pattern().matcher(hexUpper);
        assertTrue(upperMatcher.find());
        assertEquals(hexUpper, upperMatcher.group());

        Matcher fileMatcher = DependencyVerifier.Algorithm.SHA_1.pattern().matcher(inChecksumFile);
        assertTrue(fileMatcher.find());
        assertEquals(hexLower, fileMatcher.group());
    }

    @Test
    @DisplayName("SHA_1 pattern does not match invalid length or non-hex characters")
    void testSha1PatternRejectsInvalid() {
        String hex39 = "da39a3ee5e6b4b0d3255bfef95601890afd8070";
        String hex41 = "da39a3ee5e6b4b0d3255bfef95601890afd80709a";
        String nonHex = "da39a3ee5e6b4b0d3255bfef95601890afd8070g";

        assertFalse(DependencyVerifier.Algorithm.SHA_1.pattern().matcher(hex39).find());
        assertFalse(DependencyVerifier.Algorithm.SHA_1.pattern().matcher(hex41).find());
        assertFalse(DependencyVerifier.Algorithm.SHA_1.pattern().matcher(nonHex).find());
    }

    @Test
    @DisplayName("MD5 pattern matches valid 32-character hex strings")
    void testMd5PatternMatchesValid() {
        String hexLower = "d41d8cd98f00b204e9800998ecf8427e";
        String hexUpper = "D41D8CD98F00B204E9800998ECF8427E";
        String inChecksumFile = hexLower + " *test.jar\n";

        Matcher lowerMatcher = DependencyVerifier.Algorithm.MD5.pattern().matcher(hexLower);
        assertTrue(lowerMatcher.find());
        assertEquals(hexLower, lowerMatcher.group());

        Matcher upperMatcher = DependencyVerifier.Algorithm.MD5.pattern().matcher(hexUpper);
        assertTrue(upperMatcher.find());
        assertEquals(hexUpper, upperMatcher.group());

        Matcher fileMatcher = DependencyVerifier.Algorithm.MD5.pattern().matcher(inChecksumFile);
        assertTrue(fileMatcher.find());
        assertEquals(hexLower, fileMatcher.group());
    }

    @Test
    @DisplayName("MD5 pattern does not match invalid length or non-hex characters")
    void testMd5PatternRejectsInvalid() {
        String hex31 = "d41d8cd98f00b204e9800998ecf8427";
        String hex33 = "d41d8cd98f00b204e9800998ecf8427ea";
        String nonHex = "d41d8cd98f00b204e9800998ecf8427z";

        assertFalse(DependencyVerifier.Algorithm.MD5.pattern().matcher(hex31).find());
        assertFalse(DependencyVerifier.Algorithm.MD5.pattern().matcher(hex33).find());
        assertFalse(DependencyVerifier.Algorithm.MD5.pattern().matcher(nonHex).find());
    }

    @ParameterizedTest
    @EnumSource(DependencyVerifier.Algorithm.class)
    @DisplayName("digest() returns a valid MessageDigest instance for all algorithms")
    void testDigest(DependencyVerifier.Algorithm algorithm) {
        MessageDigest digest = algorithm.digest();
        assertNotNull(digest);
    }

    @Test
    @DisplayName("digest() algorithm names match expected standard algorithms")
    void testDigestAlgorithmNames() {
        assertEquals("SHA-256", DependencyVerifier.Algorithm.SHA_256.digest().getAlgorithm());
        assertEquals("SHA-1", DependencyVerifier.Algorithm.SHA_1.digest().getAlgorithm());
        assertEquals("MD5", DependencyVerifier.Algorithm.MD5.digest().getAlgorithm());
    }

    @Test
    @DisplayName("extension() returns expected file extensions")
    void testExtension() {
        assertEquals("sha256", DependencyVerifier.Algorithm.SHA_256.extension());
        assertEquals("sha1", DependencyVerifier.Algorithm.SHA_1.extension());
        assertEquals("md5", DependencyVerifier.Algorithm.MD5.extension());
    }

    @Test
    @DisplayName("repositoryName() matches extension()")
    void testRepositoryName() {
        assertEquals("sha256", DependencyVerifier.Algorithm.SHA_256.repositoryName());
        assertEquals("sha1", DependencyVerifier.Algorithm.SHA_1.repositoryName());
        assertEquals("md5", DependencyVerifier.Algorithm.MD5.repositoryName());
    }

    @Test
    @DisplayName("lowerName() returns lowercased algorithm name")
    void testLowerName() {
        assertEquals("sha-256", DependencyVerifier.Algorithm.SHA_256.lowerName());
        assertEquals("sha-1", DependencyVerifier.Algorithm.SHA_1.lowerName());
        assertEquals("md5", DependencyVerifier.Algorithm.MD5.lowerName());
    }
}
