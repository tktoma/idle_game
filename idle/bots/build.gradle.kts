// Les bots de simulation (fusionnés dans le projet le 19 octobre 2026) : des politiques de jeu qui
// pilotent le VRAI moteur (:core), sans interface. Ils ne font pas partie des règles du jeu ; depuis la 0.84,
// le lanceur (:app) les ajoute à l'exécution pour le groupe « Bots » du profil de test (runTest).
//
//   ./gradlew :bots:test                       vérifie que les bots compilent contre le moteur actuel,
//                                              que les sauvegardes de référence se rechargent et que
//                                              chaque famille de bots tourne quelques minutes de jeu
//   ./gradlew :bots:bot -Pclasse=AdvancedLab --args="DEPENDANCES sauvegardes/reference/checkpoint_before_eighth.save 12 20 42 sorties/essai"
//
// Pour les campagnes longues, run.py (Python + Java 21, sans Gradle) reste l'outil prévu : voir README.md.

plugins {
    java
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
    implementation(project(":core"))
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.test {
    useJUnitPlatform()
    // Les bots lisent leurs sauvegardes et écrivent leurs sorties en chemins relatifs à ce dossier.
    workingDir = projectDir
}

// Lance un bot : -Pclasse=NomDeLaClasse et --args="…" (les arguments du main de cette classe).
tasks.register<JavaExec>("bot") {
    group = "application"
    description = "Lance un bot sur le moteur du jeu : -Pclasse=AdvancedLab --args=\"…\""
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = providers.gradleProperty("classe").orElse("VerifyVictory")
    workingDir = projectDir
    maxHeapSize = "1g"
}
