group = "Proxy"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("net.md-5:bungeecord-api:1.21-R0.4")
}

tasks.jar {
    archiveBaseName.set(rootProject.name)
    archiveAppendix.set("proxy")
    archiveClassifier.set("")
    destinationDirectory.set(rootProject.file("target-proxy/"))
}

tasks.processResources {
    inputs.property("pluginVersion", project.version.toString())
    filesMatching("plugin.yml") {
        expand("version" to project.version.toString())
    }
}
