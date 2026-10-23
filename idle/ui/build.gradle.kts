plugins {
    `java-library`
    id("org.openjfx.javafxplugin") version "0.1.0"
}

java {
    toolchain { languageVersion = JavaLanguageVersion.of(21) }
}

group = "org.example"
// La version du jeu (version 0.126) : la même partout, posée une fois dans le projet racine.
version = rootProject.version

repositories {
    mavenCentral()
}

dependencies {
    api(project(":core"))
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

javafx {
    version = "21"
    modules("javafx.controls")
}

tasks.test {
    useJUnitPlatform()
    // Pour VersionTest (version 0.126) : la version déclarée à Gradle, à comparer avec Version.CURRENT.
    systemProperty("idle.version", project.version.toString())
}