# Idle

Un jeu incrémental en Java 21 et JavaFX, inspiré d'*Antimatter Dimensions* : des particules aux atomes, de la matière noire au Big Bang, puis jusqu'à l'univers.

## Le dépôt

Tout le projet est dans `idle/`, un projet Gradle à quatre modules :

| Module | Contenu |
|---|---|
| `idle/core` | Le moteur : les règles, les nombres (`BigNum`), la sauvegarde. Sans interface, et testé à part (`src/test/java`). |
| `idle/ui` | L'interface JavaFX : la fenêtre, les pages, les réglages, les notes de version (`Version.java`). |
| `idle/app` | Le lanceur (`idle.app.Main`). |
| `idle/bots` | Les bots de simulation, qui jouent le vrai moteur sans interface. Ils ne sont pas livrés avec le jeu ; voir `idle/bots/README.md`. |

`.github/workflows/` lance les tests à chaque envoi (`tests.yml`) et, à la demande, l'audit des 24 profils de bots depuis une partie neuve (`fresh-universe-audit.yml`).

## Jouer, tester

Il faut un JDK 21. Depuis `idle/` :

```bash
./gradlew :app:run          # le jeu
./gradlew :app:runTest      # le jeu avec le profil de test (temps accéléré, outils, bots)
./gradlew toutTester        # tous les tests de tous les modules (ou tester-tout.sh / tester-tout.bat)
```

Sous Windows, `gradlew.bat` remplace `./gradlew`. Si `./gradlew` répond « permission denied », `sh ./gradlew` marche aussi.

## Version

Le numéro du jeu est `Version.CURRENT` (`idle/ui/src/main/java/idle/ui/Version.java`), avec ses notes de version, affichées dans les réglages du jeu. `idle/build.gradle.kts` déclare le même numéro ; un test vérifie qu'ils restent égaux.
