plugins {
    `java-library`
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
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    // Version 0.95 : les tests de règles coupent le premier élan (création ÷13 avant le premier atome) ;
    // les tests de rythme le remettent eux-mêmes.
    systemProperty("idle.sansPremierElan", "true")
    // Version 0.124 : ils coupent aussi les puissances (tableau, ciel, collisionneur, succès) ;
    // les tests des puissances et du détail des multiplicateurs les remettent eux-mêmes.
    systemProperty("idle.sansPuissances", "true")
}
