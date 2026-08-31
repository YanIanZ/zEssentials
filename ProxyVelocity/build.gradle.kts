group = "ProxyVelocity"

repositories {
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    compileOnly("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
}

tasks.processResources {
    inputs.property("pluginVersion", project.version.toString())
    filesMatching("velocity-plugin.json") {
        expand("version" to project.version.toString())
    }
}

tasks.jar {
    archiveBaseName.set(rootProject.name)
    archiveAppendix.set("velocity-proxy")
    archiveClassifier.set("")
    destinationDirectory.set(rootProject.file("target-proxy/"))
}
