package dev.yanianz.essentials.dependency;

import java.util.List;

/**
 * The runtime dependencies zEssentials can resolve on its own,
 * used when they are not provided by the server or the plugin.yml libraries.
 */
public final class KnownDependencies {

    private static final String MAVEN_CENTRAL = "https://repo1.maven.org/maven2";

    /**
     * The MariaDB driver, compatible with MySQL and MariaDB servers.
     * Must match the version declared in plugin.yml.
     */
    public static final MavenDependency MARIADB_DRIVER = new MavenDependency(
            "org.mariadb.jdbc",
            "mariadb-java-client",
            "3.5.6",
            "",
            "org.mariadb.jdbc.Driver",
            List.of(MAVEN_CENTRAL)
    );

    /**
     * The PostgreSQL driver.
     */
    public static final MavenDependency POSTGRESQL_DRIVER = new MavenDependency(
            "org.postgresql",
            "postgresql",
            "42.7.4",
            "",
            "org.postgresql.Driver",
            List.of(MAVEN_CENTRAL)
    );

    private KnownDependencies() {
    }
}
