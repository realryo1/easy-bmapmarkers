plugins {
    `java-library`
    id("com.github.johnrengelman.shadow") version "8.0.0"
}

group = "dev.deimoslabs"
version = "0.9.4"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    implementation("net.kyori:adventure-text-minimessage:4.26.1")
    implementation("net.kyori:adventure-text-serializer-legacy:4.26.1")
    compileOnly("org.spigotmc:spigot-api:1.20.1-R0.1-SNAPSHOT")
    compileOnly("com.github.BlueMap-Minecraft:BlueMapAPI:v2.4.0")
}

tasks.compileJava {
    options.encoding = Charsets.UTF_8.name()
    options.release.set(17)
}

tasks.processResources {
    filteringCharset = "UTF-8"
    filesMatching(listOf("plugin.yml", "**/*.yml", "**/*.yaml", "**/*.properties", "**/*.txt", "**/*.md")) {
        expand(
            mapOf(
                "version" to project.version,
                "name" to project.name
            )
        )
    }
    inputs.property("version", project.version)
}

tasks.shadowJar {
    archiveClassifier.set("")
    mergeServiceFiles()
}

tasks.jar {
    enabled = false
}

tasks.assemble {
    dependsOn(tasks.shadowJar)
}
