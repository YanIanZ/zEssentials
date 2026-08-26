package dev.yanianz.essentials.dependency;

import java.util.List;
import java.util.Locale;

/**
 * A maven artifact that can be downloaded at runtime and pushed to the classpath.
 * Modeled after the runtime library system of Intave (https://github.com/intave/intave).
 */
public final class MavenDependency {

    private static final String MAVEN_CENTRAL = "https://repo1.maven.org/maven2";

    private final String groupId;
    private final String artifactId;
    private final String version;
    private final String classifier;

    /**
     * A class used to detect if the dependency is already available on the classpath.
     * When null, only the cache file is checked.
     */
    private final String testClass;

    private final List<String> repositories;

    public MavenDependency(String groupId, String artifactId, String version) {
        this(groupId, artifactId, version, "", null, List.of(MAVEN_CENTRAL));
    }

    public MavenDependency(String groupId, String artifactId, String version, String classifier, String testClass, List<String> repositories) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.classifier = classifier == null ? "" : classifier;
        this.testClass = testClass;
        this.repositories = repositories.isEmpty() ? List.of(MAVEN_CENTRAL) : repositories;
    }

    public String groupId() {
        return this.groupId;
    }

    public String artifactId() {
        return this.artifactId;
    }

    public String version() {
        return this.version;
    }

    public String testClass() {
        return this.testClass;
    }

    public List<String> repositories() {
        return this.repositories;
    }

    /**
     * The file name of the jar, following the maven naming scheme.
     */
    public String fileName() {
        return this.artifactId + "-" + this.version + (this.classifier.isEmpty() ? "" : "-" + this.classifier) + ".jar";
    }

    /**
     * The path of the artifact inside a repository or cache folder,
     * e.g org/mariadb/jdbc/mariadb-java-client/3.5.6/mariadb-java-client-3.5.6.jar.
     */
    public String relativePath() {
        return this.groupId.replace('.', '/') + "/" + this.artifactId + "/" + this.version + "/" + this.fileName();
    }

    /**
     * The download url of the jar inside the given repository.
     */
    public String url(String repository) {
        return trimTrailingSlash(repository) + "/" + this.relativePath();
    }

    /**
     * The url of the checksum file of the jar inside the given repository,
     * used to verify the integrity of the downloaded jar.
     */
    public String checksumUrl(String repository, String extension) {
        return this.url(repository) + "." + extension.toLowerCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return this.groupId + ":" + this.artifactId + ":" + this.version + (this.classifier.isEmpty() ? "" : ":" + this.classifier);
    }

    private static String trimTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
