// Le projet racine : rien à compiler ici. Il porte la tâche qui lance tous les tests d'un coup.
//
//   ./gradlew toutTester        (ou tester-tout.bat / tester-tout.sh, ou « Tous les tests » dans IntelliJ)
//
// Gradle trouve lui-même chaque classe de test dans le dossier src/test/java de chaque module :
// un test ajouté demain, ou un module ajouté à settings.gradle.kts, est lancé sans rien déclarer ici.

// Le numéro de version du jeu, le même que Version.CURRENT dans l'interface (version 0.126 : avant, chaque module
// déclarait « 1.0-SNAPSHOT »). Le test VersionTest de l'interface vérifie que les deux disent la même chose.
version = "0.192"

val toutTester = tasks.register("toutTester") {
    group = "verification"
    description = "Lance tous les tests de tous les modules, ceux d'aujourd'hui et ceux à venir."
}

subprojects {
    tasks.withType<Test>().configureEach {
        // Version 0.170 : les classes de tests se répartissent sur plusieurs JVM, une par deux cœurs (au moins deux
        // dès quatre cœurs). Les tests des bots prennent une fois le plus long d'entre eux (ExpertPremierUniversTest,
        // 150 s seul) au lieu de la somme de tous ; pour le moteur et l'interface, c'est quelques secondes de moins.
        // Chaque JVM a ses propres variables statiques et sa propre sortie standard : rien n'est partagé entre elles.
        // Version 0.172 : les modules se testent aussi en même temps (org.gradle.parallel dans gradle.properties), et
        // les campagnes des bots jouent leurs 24 profils en parallèle dans leur JVM (junit-platform.properties).
        maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
        // « Tout tester » veut dire tout : les tests repartent même si rien n'a changé depuis la dernière fois, et
        // le cache de construction (version 0.172) ne les remplace pas par un résultat gardé.
        if (gradle.startParameter.taskNames.any { it.substringAfterLast(':') == "toutTester" }) {
            outputs.upToDateWhen { false }
            outputs.cacheIf { false }
        }
        // Le bilan de chaque module, une ligne à la fin de ses tests. Le nom du module est lu ici, à la
        // configuration : Gradle 10 refusera qu'une tâche touche à son projet pendant qu'elle s'exécute.
        val moduleName = project.name
        addTestListener(object : TestListener {
            override fun beforeSuite(suite: TestDescriptor) {}
            override fun beforeTest(testDescriptor: TestDescriptor) {}
            override fun afterTest(testDescriptor: TestDescriptor, result: TestResult) {}
            override fun afterSuite(suite: TestDescriptor, result: TestResult) {
                if (suite.parent == null && result.testCount > 0) {
                    println("Tests de « $moduleName » : ${result.testCount} lancés, ${result.successfulTestCount} réussis, " +
                            "${result.failedTestCount} échoués, ${result.skippedTestCount} ignorés")
                }
            }
        })
    }
    // Les tâches de test de chaque module, quand le module les aura déclarées : la liste se lit au dernier moment.
    toutTester.configure { dependsOn(tasks.withType<Test>()) }
}
