// Le projet racine : rien à compiler ici. Il porte la tâche qui lance tous les tests d'un coup.
//
//   ./gradlew toutTester        (ou tester-tout.bat / tester-tout.sh, ou « Tous les tests » dans IntelliJ)
//
// Gradle trouve lui-même chaque classe de test dans le dossier src/test/java de chaque module :
// un test ajouté demain, ou un module ajouté à settings.gradle.kts, est lancé sans rien déclarer ici.

val toutTester = tasks.register("toutTester") {
    group = "verification"
    description = "Lance tous les tests de tous les modules, ceux d'aujourd'hui et ceux à venir."
}

subprojects {
    tasks.withType<Test>().configureEach {
        // « Tout tester » veut dire tout : les tests repartent même si rien n'a changé depuis la dernière fois.
        if (gradle.startParameter.taskNames.any { it.substringAfterLast(':') == "toutTester" }) {
            outputs.upToDateWhen { false }
        }
        // Le bilan de chaque module, une ligne à la fin de ses tests.
        afterSuite(KotlinClosure2<TestDescriptor, TestResult, Unit>({ suite, result ->
            if (suite.parent == null && result.testCount > 0) {
                println("Tests de « ${project.name} » : ${result.testCount} lancés, ${result.successfulTestCount} réussis, " +
                        "${result.failedTestCount} échoués, ${result.skippedTestCount} ignorés")
            }
        }))
    }
    // Les tâches de test de chaque module, quand le module les aura déclarées : la liste se lit au dernier moment.
    toutTester.configure { dependsOn(tasks.withType<Test>()) }
}
