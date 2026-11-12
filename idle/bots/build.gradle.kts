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
// La version du jeu (version 0.126) : la même partout, posée une fois dans le projet racine.
version = rootProject.version

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
    // Conserver le bilan des 24 profils dans les journaux GitHub Actions,
    // même lorsque tous les tests sont verts : les succès et les blocages
    // doivent rester consultables, et pas seulement le nombre de tests.
    testLogging {
        showStandardStreams = true
    }
    // Les campagnes restent exécutées en totalité avec ':bots:test' en local.
    // En CI, le job général exécute les autres tests et les campagnes tournent
    // dans quatre JVM indépendantes sur quatre runners (sans état partagé).
    if (providers.gradleProperty("ciWithoutProfiles").orNull == "true") {
        exclude("**/Profiles24CampaignTest.class", "**/Profiles24LongCampaignTest.class")
    }
    // Version 0.152 : le Speedrunner jusqu'à onze univers depuis zéro (de l'ordre d'une heure) ne tourne qu'à la
    // demande : ./gradlew :bots:test --tests ElevenUniversesLongTest -PonzeUnivers=true (job « onze-univers » en CI).
    if (providers.gradleProperty("onzeUnivers").orNull != "true") {
        exclude("**/ElevenUniversesLongTest.class")
    }
    val profileStart = providers.gradleProperty("ciProfilesStart").orNull
    val profileEnd = providers.gradleProperty("ciProfilesEnd").orNull
    check((profileStart == null) == (profileEnd == null)) {
        "Un segment de profils doit avoir une borne de début et de fin"
    }
    if (profileStart != null) {
        val first = profileStart.toInt()
        val last = profileEnd!!.toInt()
        require(first in 1..24 && last in first..24) {
            "Segment de profils invalide : $first-$last"
        }
        systemProperty("idle.testProfilesStart", first)
        systemProperty("idle.testProfilesEnd", last)
    }
    // Les bots lisent leurs sauvegardes et écrivent leurs sorties en chemins relatifs à ce dossier.
    workingDir = projectDir
    // Version 0.172 : les sauvegardes que lisent les tests sont une entrée de la tâche : une sauvegarde de référence
    // modifiée relance les tests des bots, même quand aucune classe n'a changé (./gradlew test ne saute un module que
    // si rien de ce qu'il lit n'a bougé).
    inputs.dir("sauvegardes").withPathSensitivity(PathSensitivity.RELATIVE)
    // Les campagnes des 24 profils jouent plusieurs parties en même temps (un test par profil, voir
    // src/test/resources/junit-platform.properties) : de la place pour autant de parties avancées que de cœurs.
    maxHeapSize = "2g"
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

// Version 0.161 : le bot analyste. Il joue comme le Speedrunner, à vitesse humaine (une décision toutes les 5 s), et écrit
// dans le dossier choisi un journal à la seconde, ses tableaux (achats, paliers, estimations) et une page HTML.
//   ./gradlew :bots:analyste                         jusqu'à la fin (0.163 ; onze univers avant), dans sorties/analyste
//   ./gradlew :bots:analyste -Punivers=1 -Pdossier=sorties/essai -Pheures=60 -Pgraine=201 -Ppas=5 -Pestimation=30
//   ./gradlew :bots:analyste -Pcomparer=true         compare aussi six façons de jouer à chaque palier (bien plus long)
// Version 0.162 : la rentabilité de chaque achat et son classement, le pourquoi des défis, succès, automatismes et
// explosions, et les « et si » à chaque palier principal (un joueur qui passe une fois par minute, toutes les dix minutes).
//   ./gradlew :bots:analyste -Petsi=false            sans les « et si » (environ deux fois plus rapide)
// Version 0.163 : par défaut, il joue jusqu'à la fin de la partie (les dix paliers du défi final) ; -Punivers=11 s'arrête
// à onze univers. -Pdepart=chemin/vers/une.save repart d'une sauvegarde (un palier d'une partie d'avant, par exemple).
tasks.register<JavaExec>("analyste") {
    group = "application"
    description = "Le bot analyste : journal, achats, paliers, estimations et rapport HTML"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "BotAnalyste"
    workingDir = projectDir
    maxHeapSize = "2500m"
    args = listOf(
        providers.gradleProperty("dossier").orElse("sorties/analyste").get(),
        providers.gradleProperty("univers").orElse("fin").get(),
        providers.gradleProperty("heures").orElse("400").get(),
        providers.gradleProperty("graine").orElse("201").get(),
        providers.gradleProperty("pas").orElse("5").get(),
        providers.gradleProperty("estimation").orElse("30").get(),
        providers.gradleProperty("comparer").orElse("false").get(),
        providers.gradleProperty("etsi").orElse("true").get(),
        providers.gradleProperty("depart").orElse("").get(),
        providers.gradleProperty("sans").orElse("").get(),
        providers.gradleProperty("profil").orElse("speedrunner").get(),
        providers.gradleProperty("ordre").orElse("").get(),
    )
}

// Version 0.164 : sans une amélioration (-Psans=identifiant,identifiant), puis la comparaison de deux parties, et le bot
// sur plusieurs graines.
//   ./gradlew :bots:analyste -Psans=atom_keep -Pdossier=sorties/sans-persistance -Punivers=2
//   ./gradlew :bots:comparer -Pa=sorties/analyste -Pb=sorties/sans-persistance -Psortie=sorties/comparaison
//   ./gradlew :bots:graines -Pgraines=201,202,203 -Punivers=11 -Psortie=sorties/graines
tasks.register<JavaExec>("comparer") {
    group = "application"
    description = "Compare deux parties du bot analyste, palier par palier"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "AnalyseComparaison"
    workingDir = projectDir
    args = listOf(
        providers.gradleProperty("a").orElse("sorties/analyste").get(),
        providers.gradleProperty("b").orElse("sorties/analyste-b").get(),
        providers.gradleProperty("sortie").orElse("sorties/comparaison").get(),
    )
}

tasks.register<JavaExec>("graines") {
    group = "application"
    description = "Le bot analyste sur plusieurs graines, et le résumé de chaque palier"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "AnalyseGraines"
    workingDir = projectDir
    maxHeapSize = "2500m"
    args = listOf(providers.gradleProperty("sortie").orElse("sorties/graines").get(), providers.gradleProperty("univers").orElse("11").get()) +
        providers.gradleProperty("graines").orElse("201,202,203").get().split(",").map { it.trim() }
}

// Version 0.168 : -Pordre=meilleur ou -Pordre=moins-cher impose l'ordre des bulles ; -Psans accepte les bulles
// (-Psans=bulle:LEAVES_LEFT) ; chaque sauvegarde garde la mémoire du bot (.bot), et -Pdepart=… la reprend.
//   ./gradlew :bots:graines -Pgraines=201,202,201:moins-cher -Punivers=4 -Psortie=sorties/dispersion
//   ./gradlew :bots:durables -Psortie=sorties/arbre -Pids=arbre        les huit cases de l'arbre les plus achetées
//   ./gradlew :bots:bulles -Psortie=sorties/bulles -Pdepart=…/Univers_3_forme.save -Punivers=8
tasks.register<JavaExec>("bulles") {
    group = "application"
    description = "Depuis une sauvegarde, la partie sans chaque bulle de l'arbre : ce que vaut chacune, univers par univers"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "AnalyseDurables"
    workingDir = projectDir
    maxHeapSize = "2500m"
    args = listOf(
        providers.gradleProperty("sortie").orElse("sorties/bulles").get(),
        "bulles",
        providers.gradleProperty("depart").orElse("").get(),
        providers.gradleProperty("univers").orElse("8").get(),
    ) + listOfNotNull(providers.gradleProperty("ids").orNull)
}

// Version 0.204 : à partir de quand chaque chose ne sert plus, mesuré en rejouant la suite sans elle depuis chaque jalon.
//   ./gradlew :bots:arrets -Psortie=sorties/arrets                       les automatismes, sur le premier univers
//   ./gradlew :bots:arrets -Psortie=sorties/arrets -Pids=ameliorations   ou arbre, mecaniques, dimensions, ou des identifiants
//   ./gradlew :bots:arrets -Pdepart=…/une.save -Punivers=4 -Pids=dimensions   depuis une sauvegarde, jusqu'à l'univers 4
//   ./gradlew :bots:arrets -Pdepart=…/apres_univers_5.save -Punivers=6 -Pids=lots,mecaniques -Pcourt=true -Pjalons=3   (0.207)
tasks.register<JavaExec>("arrets") {
    group = "application"
    description = "À partir de quel jalon un automatisme, une amélioration ou une mécanique ne sert plus"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "AnalyseArrets"
    workingDir = projectDir
    maxHeapSize = "2500m"
    args = listOf(
        providers.gradleProperty("sortie").orElse("sorties/arrets").get(),
        providers.gradleProperty("depart").orElse("neuf").get(),
        providers.gradleProperty("univers").orElse("1").get(),
        providers.gradleProperty("ids").orElse("automatismes").get(),
    ) + listOfNotNull(providers.gradleProperty("fils").orNull)
    // Version 0.207 : -Pcourt=true s'arrête au premier jalon où la coupe retarde ; -Pjalons=3 ne garde que les 3 derniers.
    providers.gradleProperty("court").orNull?.let { systemProperty("idle.arretsCourt", it) }
    providers.gradleProperty("jalons").orNull?.let { systemProperty("idle.arretsJalons", it) }
}

// Version 0.207 : la partie de référence jouée d'un trait, une sauvegarde (et la mémoire du bot) à chaque univers formé.
// Ces fichiers servent de départ à « arrets » pour mesurer un univers sans rejouer les précédents.
//   ./gradlew :bots:jalons -Psortie=sorties/jalons -Punivers=11
tasks.register<JavaExec>("jalons") {
    group = "application"
    description = "Joue jusqu'à l'univers demandé et écrit une sauvegarde à chaque univers formé"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "UniversParalleles"
    workingDir = projectDir
    maxHeapSize = "2500m"
    args = listOf(
        "jalons",
        providers.gradleProperty("sortie").orElse("sorties/jalons").get(),
        providers.gradleProperty("univers").orElse("11").get(),
    )
}

// Version 0.205 : ce que pèse dans le calcul tout ce que « arrets » a mesuré inutile (témoin, puis la même partie coupée).
//   ./gradlew :bots:charge                    tout le plan
//   ./gradlew :bots:charge -Pplan=automatismes   ou ameliorations, mecaniques
tasks.register<JavaExec>("charge") {
    group = "application"
    description = "Le temps de calcul de l'univers 1 avec et sans ce qui ne sert plus"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "ChargeArrets"
    workingDir = projectDir
    maxHeapSize = "2500m"
    jvmArgs("-Dstdout.encoding=UTF-8")
    args = listOf(providers.gradleProperty("plan").orElse("tout").get())
}

// Version 0.166 : ce que vaut chaque amélioration durable du premier univers, mesuré par une partie sans elle.
//   ./gradlew :bots:durables -Psortie=sorties/durables                 les neuf améliorations payées en atomes
//   ./gradlew :bots:durables -Psortie=sorties/durables -Pids=atom_keep  une seule
// Et les autres joueurs du bot analyste : -Pprofil=regulier (une décision par minute) ou -Pprofil=absent (cinq minutes
// par heure, le jeu fermé entre deux).
tasks.register<JavaExec>("durables") {
    group = "application"
    description = "Le premier univers sans chaque amélioration durable : ce que vaut chacune"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "AnalyseDurables"
    workingDir = projectDir
    maxHeapSize = "2500m"
    args = listOf(providers.gradleProperty("sortie").orElse("sorties/durables").get(), providers.gradleProperty("ids").orElse("").get())
}
