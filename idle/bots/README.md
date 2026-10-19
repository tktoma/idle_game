# Idle — les bots de simulation

Ce dossier est le module `bots` du projet `idle`. Il regroupe ce qui était le kit séparé `Idle_Tous_Les_Bots` : les robots, leurs sauvegardes, leurs données et le rapport consolidé.

Les bots pilotent **le vrai moteur du jeu** (`idle/core`), sans interface. Ils ne font pas partie du jeu livré aux joueurs. Ce sont des politiques de jeu programmées, pas des joueurs réels.

## Ce qui a changé à la fusion (19 octobre 2026)

| Avant (kit séparé) | Maintenant (module `idle/bots`) |
|---|---|
| `moteur/` : une copie du moteur, à recopier à chaque version du jeu | supprimé : les bots se compilent contre `idle/core`, le moteur du jeu lui-même |
| `bin/` : des fichiers `.class` déjà compilés | supprimé : `run.py` ou Gradle recompilent |
| `bots/v2/` et `bots/profils_24/` (avec `IdleProfiles24.java` en double) | `src/main/java/` : les 13 sources compilées, une seule fois chacune |
| `bots/historique/` | `historique/` : archive, non compilée (voir plus bas) |
| `docs/DOCUMENTATION_JEU_107_PAGES.pdf` | supprimé : identique au PDF à la racine du dépôt |
| `INVENTAIRE_SHA256.csv` | supprimé : les chemins ont changé, et git garde déjà l'empreinte de chaque fichier |
| chemins `/mnt/data/...` dans quatre bots | chemins relatifs à ce dossier (`sauvegardes/reference/...`, `sorties/`) |

Le contenu des `donnees/`, `sauvegardes/`, `docs/` et `scripts_originals/` est inchangé.

## Ce que garantit la fusion

- **Les bots suivent le jeu.** `run.py` recompile le moteur depuis `idle/core/src/main/java` dès qu'une source a changé. Une modification du jeu est donc prise en compte au lancement suivant.
- **Un test à chaque envoi.** `BotsTest` (dans `src/test/java`) tourne avec les autres tests du projet (`./gradlew toutTester`, et l'action GitHub). Il vérifie quatre choses :
  - les bots compilent contre le moteur actuel ;
  - les 32 sauvegardes se relisent ;
  - les 24 sauvegardes de victoire sont toujours des victoires ;
  - les bots V2, les 24 profils et les profils de début tournent quelques minutes sans planter.

  Si une évolution du jeu casse un bot ou une sauvegarde, l'envoi le signale.

## Lancer les bots

Il faut **Java 21**, et **Python 3.10+** pour `run.py` (bibliothèque standard seulement). Gradle n'est pas nécessaire pour `run.py`. Toutes les commandes se lancent depuis ce dossier (`idle/bots`).

```bash
python run.py build                      # compile le moteur du jeu, puis les bots, dans build/kit/
python run.py verify --save sauvegardes/reference/checkpoint_universe_5s.save
python run.py v2 --bot DEPENDANCES --hours 12 --step 20 --seed 42
python run.py profils --id 21 --hours 200 --step 20 --seed 42
python run.py fresh --id 1 --hours 4 --step 20 --seed 42
python run.py coach --checkpoint sauvegardes/profils_bloques/profil_12_72h.save --hours 12 --seed 42
python run.py batch --tier rapide --workers 2
```

- Les résultats vont dans `sorties/`, que git ignore.
- `TEST_RAPIDE.sh` et `TEST_RAPIDE.ps1` font une vérification rapide du kit.
- Les modes V2 sont : `EXPERT_COMPLET`, `DEPENDANCES`, `ECONOMISTE`, `PLAN_PRESTIGE`, `ADAPTATIF`, `DEBUTANT_APPRENANT`, `GUIDE_UI`, `RECUPERATION`, `SANS_IMPULSIONS`, `SANS_COMETES`, `SANS_TECH`, `SANS_LISTE`.
- Les 24 profils sont décrits dans `PROFILS_24.md`.

Avec Gradle, depuis `idle/` :

```bash
./gradlew :bots:test
./gradlew :bots:bot -Pclasse=AdvancedLab --args="DEPENDANCES sauvegardes/reference/checkpoint_before_eighth.save 12 20 42 sorties/essai"
```

## Le contenu

| Dossier | Contenu |
|---|---|
| `src/main/java/` | Les 13 bots compilés. `AdvancedLab` contient les 12 politiques V2, `IdleProfiles24` les 24 profils de fin de partie et `FreshPhaseProfiles` les mêmes profils depuis une partie neuve. Il y a aussi `CorrectedCoach`, les audits (`DependencyAudit`, `EconomyAudit`, `DiagnoseState`), `OfflineBenchmark`, `RescueBench`, `RecreateLosers`, `RobotPhase3` et `VerifyVictory`. |
| `src/test/java/` | `BotsTest` : la vérification de cohérence avec le jeu. |
| `historique/` | Une trentaine d'anciens robots et sondes, écrits pour d'anciennes versions du jeu. Ils sont gardés pour mémoire et ne sont **pas compilés** : leur API n'est plus garantie. Pour en relancer un, il faut le copier dans `src/main/java` et l'adapter. |
| `sauvegardes/` | `reference/` : 6 points de départ, dont la partie à 7 Big Bangs. `profils_bloques/` : les profils 11, 12 et 17 à 72 h. `victoires_corrigees/` : 2 parties. `victoires_historiques/` : les 21 victoires. |
| `donnees/` | Les CSV bruts des campagnes, sans retraitement. |
| `docs/` | Le rapport consolidé (PDF et Word), le rapport du laboratoire V2, les synthèses, 25 graphiques et 3 classeurs Excel. |
| `scripts_originals/` | Les scripts Python et shell d'origine. Ils sont gardés pour mémoire et pointent vers les anciens dossiers `/mnt/data/...` ; `run.py` les remplace. |

## À savoir avant de lire les résultats

- **Version du moteur.** Les campagnes de `donnees/` ont tourné sur le moteur copié dans l'ancien kit. Ce moteur est identique au cœur du jeu en 0.79 et 0.80 ; la 0.81 n'a rien changé avant l'univers.
- **Résultats reproduits sur la version actuelle.** Le 19 octobre 2026, ces résultats sont retombés exactement sur les mêmes valeurs :
  - `DEPENDANCES` : 7 h 38 ;
  - profil 1 : 8 h 17 ;
  - profil 21 : 145 h 09 ;
  - profil 22 : 145 h 51.
- **La graine ne change presque rien après 7 Big Bangs.** La fin du jeu n'y dépend plus du hasard (7 h 38 avec la graine 42, 7 h 39 avec la 43). Plusieurs graines ne valent donc pas plusieurs essais à ce stade.
- **Les profils historiques ont un bug.** Après le 8e Big Bang, leur politique de fin peut couper définitivement la fusion et la synthèse. Les trois profils perdants (11, 12 et 17) viennent de là ; `CorrectedCoach` les débloque.
- **Le planificateur coupe la création automatique de molécules** (`RobotCosmosPlanner.cosmos()`). Ça ne change rien aux résultats : en fin d'acte 3, la création automatique « suivre la liste » ne crée pas la matière demandée par les astres et le cosmos, même allumée.
- **L'absence des profils n'est pas plafonnée.** Les profils intermittents créditent toute leur absence, alors que le jeu la plafonne à 24 h (`Absence.MAX_SECONDS`).
- **Les profils partis de zéro ne savent pas faire le premier Big Bang.** Ils ne planifient ni les défis ni ses conditions. Leurs nombres d'explosions (une par décision, toutes les 20 s) ne mesurent pas le jeu.
