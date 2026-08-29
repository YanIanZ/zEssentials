package dev.yanianz.essentials.dependency;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MavenDependencyTest {

    private static final String MAVEN_CENTRAL = "https://repo1.maven.org/maven2";

    @Test
    @DisplayName("Simple constructor uses Maven Central as default repository and null testClass")
    void testSimpleConstructor() {
        MavenDependency dependency = new MavenDependency("org.mariadb.jdbc", "mariadb-java-client", "3.5.6");

        assertEquals("org.mariadb.jdbc", dependency.groupId());
        assertEquals("mariadb-java-client", dependency.artifactId());
        assertEquals("3.5.6", dependency.version());
        assertNull(dependency.testClass());
        assertEquals(List.of(MAVEN_CENTRAL), dependency.repositories());
    }

    @Test
    @DisplayName("Full constructor sets all fields correctly")
    void testFullConstructor() {
        List<String> repos = List.of("https://repo.papermc.io/repository/maven-public/");
        MavenDependency dependency = new MavenDependency(
                "io.papermc.paper",
                "paper-api",
                "1.21.4-R0.1-SNAPSHOT",
                "all",
                "io.papermc.paper.ServerBuildInfo",
                repos
        );

        assertEquals("io.papermc.paper", dependency.groupId());
        assertEquals("paper-api", dependency.artifactId());
        assertEquals("1.21.4-R0.1-SNAPSHOT", dependency.version());
        assertEquals("io.papermc.paper.ServerBuildInfo", dependency.testClass());
        assertEquals(repos, dependency.repositories());
    }

    @Test
    @DisplayName("fileName() without classifier returns artifactId-version.jar")
    void testFileNameWithoutClassifier() {
        MavenDependency dependency = new MavenDependency("org.mariadb.jdbc", "mariadb-java-client", "3.5.6");

        assertEquals("mariadb-java-client-3.5.6.jar", dependency.fileName());
    }

    @Test
    @DisplayName("fileName() with classifier returns artifactId-version-classifier.jar")
    void testFileNameWithClassifier() {
        MavenDependency dependency = new MavenDependency(
                "org.postgresql",
                "postgresql",
                "42.7.4",
                "jre7",
                "org.postgresql.Driver",
                List.of(MAVEN_CENTRAL)
        );

        assertEquals("postgresql-42.7.4-jre7.jar", dependency.fileName());
    }

    @Test
    @DisplayName("relativePath() converts dots in groupId to slashes")
    void testRelativePath() {
        MavenDependency dependency = new MavenDependency("org.mariadb.jdbc", "mariadb-java-client", "3.5.6");

        assertEquals("org/mariadb/jdbc/mariadb-java-client/3.5.6/mariadb-java-client-3.5.6.jar", dependency.relativePath());
    }

    @Test
    @DisplayName("relativePath() with classifier includes classifier in path")
    void testRelativePathWithClassifier() {
        MavenDependency dependency = new MavenDependency(
                "org.mariadb.jdbc",
                "mariadb-java-client",
                "3.5.6",
                "shaded",
                null,
                List.of(MAVEN_CENTRAL)
        );

        assertEquals("org/mariadb/jdbc/mariadb-java-client/3.5.6/mariadb-java-client-3.5.6-shaded.jar", dependency.relativePath());
    }

    @Test
    @DisplayName("url() concatenates repository and relativePath")
    void testUrl() {
        MavenDependency dependency = new MavenDependency("org.mariadb.jdbc", "mariadb-java-client", "3.5.6");

        assertEquals(
                "https://repo1.maven.org/maven2/org/mariadb/jdbc/mariadb-java-client/3.5.6/mariadb-java-client-3.5.6.jar",
                dependency.url("https://repo1.maven.org/maven2")
        );
    }

    @Test
    @DisplayName("url() trims trailing slash from repository URL")
    void testUrlTrimsTrailingSlash() {
        MavenDependency dependency = new MavenDependency("org.mariadb.jdbc", "mariadb-java-client", "3.5.6");

        assertEquals(
                "https://repo1.maven.org/maven2/org/mariadb/jdbc/mariadb-java-client/3.5.6/mariadb-java-client-3.5.6.jar",
                dependency.url("https://repo1.maven.org/maven2/")
        );
    }

    @Test
    @DisplayName("checksumUrl() appends lowercase extension to artifact URL")
    void testChecksumUrl() {
        MavenDependency dependency = new MavenDependency("org.mariadb.jdbc", "mariadb-java-client", "3.5.6");
        String baseUrl = "https://repo1.maven.org/maven2";

        assertEquals(
                "https://repo1.maven.org/maven2/org/mariadb/jdbc/mariadb-java-client/3.5.6/mariadb-java-client-3.5.6.jar.sha256",
                dependency.checksumUrl(baseUrl, "sha256")
        );
        assertEquals(
                "https://repo1.maven.org/maven2/org/mariadb/jdbc/mariadb-java-client/3.5.6/mariadb-java-client-3.5.6.jar.sha256",
                dependency.checksumUrl(baseUrl, "SHA256")
        );
        assertEquals(
                "https://repo1.maven.org/maven2/org/mariadb/jdbc/mariadb-java-client/3.5.6/mariadb-java-client-3.5.6.jar.sha1",
                dependency.checksumUrl(baseUrl, "SHA1")
        );
        assertEquals(
                "https://repo1.maven.org/maven2/org/mariadb/jdbc/mariadb-java-client/3.5.6/mariadb-java-client-3.5.6.jar.md5",
                dependency.checksumUrl(baseUrl, "MD5")
        );
    }

    @Test
    @DisplayName("toString() returns groupId:artifactId:version without classifier")
    void testToStringWithoutClassifier() {
        MavenDependency dependency = new MavenDependency("org.mariadb.jdbc", "mariadb-java-client", "3.5.6");

        assertEquals("org.mariadb.jdbc:mariadb-java-client:3.5.6", dependency.toString());
    }

    @Test
    @DisplayName("toString() returns groupId:artifactId:version:classifier with classifier")
    void testToStringWithClassifier() {
        MavenDependency dependency = new MavenDependency(
                "org.mariadb.jdbc",
                "mariadb-java-client",
                "3.5.6",
                "all",
                null,
                List.of(MAVEN_CENTRAL)
        );

        assertEquals("org.mariadb.jdbc:mariadb-java-client:3.5.6:all", dependency.toString());
    }

    @Test
    @DisplayName("Null classifier is treated as empty string")
    void testNullClassifier() {
        MavenDependency dependency = new MavenDependency(
                "org.example",
                "example-lib",
                "1.0.0",
                null,
                "org.example.Test",
                List.of(MAVEN_CENTRAL)
        );

        assertEquals("example-lib-1.0.0.jar", dependency.fileName());
        assertEquals("org.example:example-lib:1.0.0", dependency.toString());
    }

    @Test
    @DisplayName("Empty repositories list defaults to Maven Central")
    void testEmptyRepositoriesDefaultsToMavenCentral() {
        MavenDependency dependency = new MavenDependency(
                "org.example",
                "example-lib",
                "1.0.0",
                "",
                "org.example.Test",
                List.of()
        );

        assertEquals(List.of(MAVEN_CENTRAL), dependency.repositories());
    }

    @Test
    @DisplayName("testClass() returns null for simple constructor and configured value for full constructor")
    void testTestClassProperty() {
        MavenDependency simple = new MavenDependency("org.example", "simple", "1.0.0");
        assertNull(simple.testClass());

        MavenDependency withTestClass = new MavenDependency(
                "org.example",
                "custom",
                "1.0.0",
                "",
                "org.example.Driver",
                List.of(MAVEN_CENTRAL)
        );
        assertEquals("org.example.Driver", withTestClass.testClass());
    }
}
