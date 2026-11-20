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
    // Version 0.283 : les tests qui dessinent chaque skin à chaque palier tiennent beaucoup d'images à la fois ; avec les
    // 512 Mo par défaut, la CI manquait de mémoire (OutOfMemoryError dans les fils des sons).
    // Version 0.285 : 2 Go (1 Go ne suffisait pas sous Windows : AvatarStyleTest). Un test tourne d'un bloc sur le fil
    // de JavaFX, sans image affichée entre deux dessins : ce qu'un canevas a à dessiner s'empile jusqu'à la fin du test.
    maxHeapSize = "2g"
    // Version 0.284 : une JVM neuve toutes les classes de tests (25 en 0.284, 10 depuis la 0.285) : ce qu'une JVM garde
    // d'un test à l'autre (le toolkit, les images, les caches) ne s'accumule plus.
    forkEvery = 10
    // Et au plus deux JVM de test de l'interface à la fois : 2 Go chacune, la machine garde de la place pour le reste.
    maxParallelForks = maxParallelForks.coerceAtMost(2)
    // Pour VersionTest (version 0.126) : la version déclarée à Gradle, à comparer avec Version.CURRENT.
    systemProperty("idle.version", project.version.toString())
}