# Idle

Un jeu incrémental en Java 21 et JavaFX : des particules aux atomes, puis aux Big Bangs, aux astres et aux galaxies,
jusqu'à onze univers et le défi final. L'historique de chaque version est dans le jeu (onglet Réglages, bouton « Notes de version »)
et dans `ui/src/main/java/idle/ui/Version.java`, la note la plus récente en tête.

## Les modules

| Module | Ce qu'il contient |
|---|---|
| `core` | Le moteur : les règles, les nombres (`BigNum`), la sauvegarde. Aucune dépendance à JavaFX. |
| `ui` | L'interface JavaFX : les onglets, les vues dessinées, les skins, les sons. |
| `app` | Le lanceur (`idle.app.Main`). |
| `bots` | Des politiques de jeu qui pilotent le vrai moteur, sans interface, pour tester et équilibrer. Voir `bots/README.md`. |

## Construire et lancer

Il faut **Java 21**. Gradle n'a pas besoin d'être installé : `gradlew` (ou `gradlew.bat` sous Windows) le télécharge.

```bash
./gradlew :app:run          # le jeu
./gradlew runTest           # le jeu avec le profil de test : temps accéléré, ressources gratuites, section Bots
./gradlew runTestUi         # le profil test UI : chaque combinaison de skins d'un onglet, sans gains ni sauvegarde
```

`profil-test-ui.sh` / `.bat` lance le profil test UI d'un double-clic.

## Les tests

| Script (`.sh` et `.bat`) | Ce qu'il lance |
|---|---|
| `tester-change` | Les tests des modules où quelque chose a changé depuis le dernier passage vert. |
| `tester-tout` | Tous les tests de tous les modules (`./gradlew toutTester`), sans rien réutiliser. |
| `test-interface` | Les tests de l'interface seulement (`:ui:test`). Sous Linux sans écran, ils passent par `xvfb-run` s'il est installé. |
| `run-all-bots` | Les tests des bots, campagnes des 24 profils et onze univers depuis zéro compris (plusieurs heures). `run-all-bots court` saute les onze univers. |

Un test qui échoue dit pourquoi dans la console ; le rapport complet est dans `<module>/build/reports/tests/test/index.html`.

Deux garde-fous tournent avec les autres tests :

- `DeterminismeTest` (moteur) : une partie rejouée donne la même sauvegarde, et le moteur ne parcourt aucun ensemble dont
  l'ordre change d'un lancement de Java à l'autre (`Set.copyOf`, `Map.of` non vide…). Deux parties de même graine
  doivent rester identiques : c'est ce qui rend les mesures des bots comparables.
- `HistoriqueDesVersionsTest` (interface) : chaque version a sa note, de la 0.034 à la version en cours.

## Mesurer l'équilibrage

**La référence de l'équilibrage est le bot analyste** (`BotAnalyste`) avec ses réglages par défaut : il joue comme le
profil Speedrunner, une décision toutes les **5 s** de jeu, graine **201**, une estimation du prochain palier toutes les
**30 min**. Les « et si » de son rapport rejouent la suite pour un joueur qui passe une fois par minute (le profil
Régulier, une décision toutes les **60 s**), puis toutes les dix minutes. Quand une note de version donne une durée
« mesurée », c'est celle-là, sauf mention contraire.

Le Speedrunner est aussi le seul profil qui ne joue pas les mécaniques ajoutées en 0.294 (calme du Big Bang, épreuves,
rond de croissance) : il reste la référence, inchangée. `FreshUniverseAudit` et les jalons (`./gradlew :bots:jalons`)
jouent le même Speedrunner, graine 201, au pas de 20 s : ce sont les points de départ des mesures, pas la mesure.

| Script (`.sh`, et `.bat` sauf le dernier) | Ce qu'il fait |
|---|---|
| `analyste` | Le bot analyste jusqu'à la fin de la partie (plusieurs heures) ; le rapport va dans `bots/sorties/analyste/rapport.html`. |
| `measure-univers` | Un seul univers, depuis le jalon de l'univers d'avant : `./measure-univers.sh 5`, ou `./measure-univers.sh 5 -Psans=atom_keep` pour la même mesure sans une amélioration. |
| `profile` | Une partie du bot analyste enregistrée par Java Flight Recorder (`bots/sorties/profil/partie.jfr`), puis les méthodes les plus coûteuses. À lancer avant et après une optimisation. |
| `compare-versions` | Deux versions (un commit, une étiquette, ou `ici`) jouées par le bot analyste depuis la même sauvegarde, et l'écart palier par palier : `./compare-versions.sh 0.295 ici 4`. |

Tous les outils des bots acceptent `-Pjfr=chemin.jfr` pour enregistrer un profil. Les autres (`comparer`, `graines`,
`arrets`, `bulles`, `durables`, `charge`) sont décrits en tête de `bots/build.gradle.kts` et dans `bots/README.md`.

## Les règles du projet

- Une ancienne sauvegarde doit toujours se relire (`SaveTest`, et les 35 sauvegardes de `bots/sauvegardes`).
- Le moteur reste déterministe (voir `DeterminismeTest`).
- Chaque livraison ajoute sa note en tête de `Version.java` et avance `version` dans `build.gradle.kts` : `VersionTest`
  vérifie qu'ils disent la même chose.
- Aucun fichier source au-delà de 1 000 lignes : une classe trop longue se découpe en une chaîne de classes abstraites
  (`GameBase` → … → `Game`, `DimensionWindowBase` → … → `DimensionWindow`).

## Les autres documents

- `bots/README.md` : les bots, leurs outils et l'histoire de leurs réglages ; `bots/PROFILS_24.md` : les 24 profils.
- `SKINS.md` : la liste de travail des skins.
- Le modèle mathématique et le pas-à-pas des livraisons sont tenus dans le projet Claude.
