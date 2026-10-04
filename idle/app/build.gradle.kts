plugins {
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
}

java {
    toolchain { languageVersion = JavaLanguageVersion.of(21) }
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":ui"))
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

javafx {
    version = "21"
    modules("javafx.controls")
}

application {
    mainClass = "idle.app.Main"
}

tasks.test {
    useJUnitPlatform()
}

// Profil de test : « runTest » lance le jeu comme « run », avec en plus l'argument --test,
// qui affiche la barre d'outils de test dans la fenêtre.
tasks.register("runTest") {
    group = "application"
    description = "Lance le jeu avec le profil de test (temps accéléré, ressources gratuites)."
    dependsOn("run")
}

tasks.named<JavaExec>("run") {
    if (gradle.startParameter.taskNames.any { it.substringAfterLast(':') == "runTest" }) {
        args("--test")
    }
}