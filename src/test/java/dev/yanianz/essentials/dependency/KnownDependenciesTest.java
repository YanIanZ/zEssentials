package dev.yanianz.essentials.dependency;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class KnownDependenciesTest {

    private static final String MAVEN_CENTRAL = "https://repo1.maven.org/maven2";

    @Test
    @DisplayName("MARIADB_DRIVER has correct coordinate properties and testClass")
    void testMariaDbDriverProperties() {
        MavenDependency mariaDb = KnownDependencies.MARIADB_DRIVER;

        assertNotNull(mariaDb);
        assertEquals("org.mariadb.jdbc", mariaDb.groupId());
        assertEquals("mariadb-java-client", mariaDb.artifactId());
        assertEquals("3.5.6", mariaDb.version());
        assertEquals("org.mariadb.jdbc.Driver", mariaDb.testClass());
    }

    @Test
    @DisplayName("POSTGRESQL_DRIVER has correct coordinate properties and testClass")
    void testPostgreSqlDriverProperties() {
        MavenDependency postgreSql = KnownDependencies.POSTGRESQL_DRIVER;

        assertNotNull(postgreSql);
        assertEquals("org.postgresql", postgreSql.groupId());
        assertEquals("postgresql", postgreSql.artifactId());
        assertEquals("42.7.4", postgreSql.version());
        assertEquals("org.postgresql.Driver", postgreSql.testClass());
    }

    @Test
    @DisplayName("Both known dependencies use Maven Central as their repository")
    void testRepositories() {
        assertEquals(List.of(MAVEN_CENTRAL), KnownDependencies.MARIADB_DRIVER.repositories());
        assertEquals(List.of(MAVEN_CENTRAL), KnownDependencies.POSTGRESQL_DRIVER.repositories());
    }

    @Test
    @DisplayName("Both known dependencies generate valid file names and relative paths")
    void testFileNamesAndRelativePaths() {
        MavenDependency mariaDb = KnownDependencies.MARIADB_DRIVER;
        assertEquals("mariadb-java-client-3.5.6.jar", mariaDb.fileName());
        assertEquals(
                "org/mariadb/jdbc/mariadb-java-client/3.5.6/mariadb-java-client-3.5.6.jar",
                mariaDb.relativePath()
        );

        MavenDependency postgreSql = KnownDependencies.POSTGRESQL_DRIVER;
        assertEquals("postgresql-42.7.4.jar", postgreSql.fileName());
        assertEquals(
                "org/postgresql/postgresql/42.7.4/postgresql-42.7.4.jar",
                postgreSql.relativePath()
        );
    }

    @Test
    @DisplayName("Both known dependencies generate correct download URLs")
    void testDownloadUrls() {
        assertEquals(
                "https://repo1.maven.org/maven2/org/mariadb/jdbc/mariadb-java-client/3.5.6/mariadb-java-client-3.5.6.jar",
                KnownDependencies.MARIADB_DRIVER.url(MAVEN_CENTRAL)
        );
        assertEquals(
                "https://repo1.maven.org/maven2/org/postgresql/postgresql/42.7.4/postgresql-42.7.4.jar",
                KnownDependencies.POSTGRESQL_DRIVER.url(MAVEN_CENTRAL)
        );
    }

    @Test
    @DisplayName("Both known dependencies format toString() correctly")
    void testToString() {
        assertEquals("org.mariadb.jdbc:mariadb-java-client:3.5.6", KnownDependencies.MARIADB_DRIVER.toString());
        assertEquals("org.postgresql:postgresql:42.7.4", KnownDependencies.POSTGRESQL_DRIVER.toString());
    }
}
