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
  - les 33 sauvegardes se relisent ;
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
| `sauvegardes/` | `reference/` : 7 points de départ, dont la partie à 7 Big Bangs et une partie neuve au 5e Big Bang (`checkpoint_fresh_fifth_bang.save`, profil 1, graine 201). `profils_bloques/` : les profils 11, 12 et 17 à 72 h. `victoires_corrigees/` : 2 parties. `victoires_historiques/` : les 21 victoires. |
| `donnees/` | Les CSV bruts des campagnes, sans retraitement. |
| `docs/` | Le rapport consolidé (PDF et Word), le rapport du laboratoire V2, les synthèses, 25 graphiques et 3 classeurs Excel. |
| `scripts_originals/` | Les scripts Python et shell d'origine. Ils sont gardés pour mémoire et pointent vers les anciens dossiers `/mnt/data/...` ; `run.py` les remplace. |

## Les correctifs des bots (19 octobre 2026)

Trois défauts des bots faussaient les anciennes campagnes. Ils sont corrigés dans `RobotCosmosPlanner`, que tous les profils et le bot V2 utilisent.

1. **La fusion et la synthèse restent coupées après le Big Bang.** Le robot les coupait pour préparer le Big Bang, puis ne les rallumait jamais. C'est ce qui bloquait les profils 11, 12 et 17. Il les rallume maintenant dès qu'il n'en a plus besoin.
2. **La matière ne se créait qu'aux décisions du robot.** La création automatique était toujours coupée, donc la matière des derniers astres et du cosmos ne se créait jamais pendant l'absence ni entre deux décisions. Le robot fait maintenant ce que ferait un joueur avisé : pour chaque état dont le prochain pas demande de la matière, il confie à la création automatique l'amas rassemblé le moins encombrant (« amas choisis »), et la coupe une fois la matière réunie.
3. **L'absence n'était pas plafonnée.** Elle compte maintenant comme dans le jeu : au taux hors ligne, et 24 h au plus par absence (`RobotCosmosPlanner.away`).

Résultats depuis la partie à 7 Big Bangs, graine 42, horizon 14 jours :

| Profil | Avant | Après les correctifs | Dont temps actif |
|---|---|---|---|
| Bot V2 `DEPENDANCES` | 7 h 38 | 6 h 42 | tout |
| 1 Optimiseur | 8 h 17 | 6 h 54 | tout |
| 11 Fan de prestige | bloqué | 15 h 10 | tout |
| 12 Accumulateur | bloqué | 10 h 02 | tout |
| 17 AFK jeu ouvert | bloqué | 27 h 00 | tout |
| 19 Micro-sessions | 184 h 00 | 12 h 01 | 1 h 01 |
| 21 Joueur du soir | 145 h 09 | 72 h 00 | 6 h 00 |
| 22 Joueur de week-end | 145 h 51 | 51 h 26 | 7 h 26 |
| 23 Joueur distrait | 254 h 20 | 15 h 00 | 5 h 00 |
| 24 Expert intermittent | 156 h 00 | 18 h 00 | 1 h 30 |

**Le rapport consolidé et les CSV de `donnees/` datent d'avant ces correctifs.** Leurs durées de fin de partie, surtout celles des profils intermittents, mesuraient les défauts des bots plus que le jeu.

Le correctif 2 a un revers : il montre qu'un joueur qui laisse la création automatique sur « suivre la liste » reste lent. Le jeu ne lui crée pas la matière des astres et du cosmos. Seul celui qui choisit lui-même ses amas avance seul.

## Les bots partis de zéro suivent le modèle 0.123 (22 octobre 2026)

Le document « Idle : modèle mathématique du jeu pour les bots » (version 0.123) donne l'ordre d'achat
mesuré le meilleur et les pièges d'un bot. Deux écarts empêchaient les bots partis de zéro de finir l'univers.

1. **Acte 1 : la synthèse attendait Persistance** (§4, §14.1, §14.2). Le tableau s'ouvrait vers 3 h 10, mais
   rien ne se synthétisait avant 4 h 30 ; ensuite les atomes partaient dans la Surcharge. `FirstUniversePolicy`
   suit maintenant l'ordre du §14.1 (`OPENING` : Dédoublement, Patience, Masse, Dédoublement 2, Catalyseur,
   Compression, automatismes générateurs et fusion, deux cadences des générateurs, Persistance, automatisme
   de vitesse, cadence de la fusion, 3e cadence des générateurs, Rendement), synthétise dès l'ouverture du
   tableau en gardant les atomes de la prochaine étape, pèse chaque amélioration répétable contre une synthèse
   et limite la Surcharge à 13 niveaux sous le plafond.
2. **Acte 3 : le tableau était vidé en permanence** (§8.3, §15). Après le 5e Big Bang, le bot et l'explosion
   automatique explosaient environ 450 fois toutes les 10 minutes. Chaque explosion efface les éléments que
   les molécules consomment : le carbone ne dépassait pas 6 exemplaires, le Bitume (C14H10, 14 carbones)
   ne se créait pas, et le ciel restait à 3 astres mûrs plus de 7 h. `FirstUniversePolicy.holdExplosions`
   suspend les explosions (et `dark_auto_explosion`) tant que la matière noire en main suffit au Big Bang et
   que la liste de courses demande des molécules.

Partie neuve, graine 200 + id, une décision toutes les 20 s, moteur de `master` au 22 octobre :

| Étape | Document (bot 0.123) | Profil 1 avant | Profil 1 après | Profil 2 après |
|---|---|---|---|---|
| Première explosion | 5 h 12 | 7 h 00 | 6 h 20 | |
| Premier Big Bang | 11 h 10 | 11 h 30 | 10 h 30 | |
| 5e Big Bang | 13 h 54 | vers 13 h | 12 h 00 | |
| 8e Big Bang | 18 h 38 | jamais (5 BB, 3 astres à 21 h) | 13 h 52 | 14 h 36 |
| Univers formé | 27 h 02 | jamais | **16 h 26** | **17 h 37** |

`FreshUniverseAudit` sur 10 des 24 profils, partie neuve, plafond de 168 h réelles (22 octobre 2026) :

| Profil | Verdict | Univers formé (heures réelles) |
|---|---|---|
| 1 Speedrunner | WIN | 16 h 26 |
| 2 Vétéran | WIN | 17 h 37 |
| 3 Régulier | WIN | 25 h 49 |
| 5 Chasseur de succès | WIN | 24 h 49 |
| 11 Fan de prestige | WIN | 22 h 36 |
| 12 Avare | WIN | 77 h 26 |
| 16 Débutant | WIN | 90 h 06 |
| 24 Expert intermittent | WIN | 153 h 00 (75 h de jeu) |
| 21 Joueur du soir | LIMIT | galaxie formée, 8 Big Bangs, 35 astres à 168 h (14 h en ligne) |
| 17 AFK jeu ouvert | LIMIT | aucun Big Bang : une visite par heure ne permet qu'une fusion par visite |

## Le jeu réglé sur le Speedrunner (version 0.125)

Depuis la version 0.125, l'équilibrage du premier univers suit le profil 1 (Speedrunner) de `FreshUniverseAudit`,
partie neuve, graine 201, une décision toutes les 20 s. En 0.124, il formait l'univers en 14 h 28
(6 h 03 + 1 h 37 + 6 h 48) : le deuxième acte ne durait plus qu'une heure et demie. Les réglages de la 0.125
(tempo 12, Big Bangs à 10¹⁹⁰⁰⁰⁰ puis 10⁵⁰⁰⁰⁰ particules, vitesse essoufflée dès 300 niveaux, puissances plus fortes,
maturation ×3 et impulsions à 18 s, rassemblements de 2 h) donnent :

| Profil | Premier acte | Deuxième acte | Troisième acte | Univers formé |
|---|---|---|---|---|
| 1 Speedrunner | 5 h 08 | 6 h 14 | 16 h 01 | 27 h 23 |
| 2 Vétéran | 5 h 19 | 6 h 15 | 16 h 55 | 28 h 28 |
| 3 Régulier | | | | 34 h 50 |
| 11 Fan de prestige | | | | 33 h 42 |

Parts moyennes de la production du Speedrunner sur tout le premier univers : ciel 31 %, tableau périodique 23 %,
améliorations 15 %, matière noire 15 %, collisionneur 5 %, succès 2 %. Chaque palier de Big Bang ouvert est payé
après 4 à 10 minutes.

Le profil 17 bute sur la règle du jeu plus que sur sa politique : sans automatisme avant 30 atomes créés, une
fusion par visite horaire demande environ 88 h pour ouvrir le tableau ; l'explosion efface ensuite les
automatismes, et 0,8 matière noire ne paie ni « Départ lancé » (2) ni les automatismes offerts (3).

## Les bots partis de zéro : succès et défis (0.86)

Dans le profil de test, un bot lancé sur une partie neuve restait bloqué : 37 succès et aucun défi après 24 h de jeu, alors que le premier Big Bang en demande 40 et les 10 défis. Quatre correctifs dans `RobotCosmosPlanner`, qui servent à tous les bots :

1. **Les succès à faire exprès** (`hunt()`). Le bot verrouille l'appui, achète Persistance, vise un actinide, tient une réserve de synthèse de 50 atomes, achète en gros et « tout acheter ». Chaque geste n'a lieu que tant que son succès manque.
2. **Les défis, un par un** (`challenges()`), dès 10 explosions. Un défi pas réussi en 2 h de jeu est abandonné, ce qui donne au passage le succès « Demi-tour », puis retenté 25 explosions plus tard. Avant, une condition sur une case de l'arbre empêchait de les lancer.
3. **La réserve de matière noire** (`dark()`) ne bloque plus que ce qui se paie en matière noire. Avant, elle bloquait aussi les cases payées en particules, en atomes ou en taille.
4. **La patience pour l'arbre** (`waitsForTree()`). Une fois les défis faits, le bot laisse grandir la partie tant qu'une case ouverte de l'arbre reste trop chère et que sa production progresse encore, 6 h au plus.

Résultat, bot V2 `DEPENDANCES` depuis une partie neuve : 44 succès et les 10 défis vers 11 h de jeu.

**Ce qui restait (dépassé au 22 octobre 2026, voir plus haut) :** sa production plafonne vers 1e63 par seconde, et le premier Big Bang en demande 1e32000. Depuis zéro, le bot n'atteint donc toujours pas le premier Big Bang ; il faudrait une vraie stratégie de l'arbre et des explosions. Les durées de fin de partie, depuis la partie à 7 Big Bangs, ne bougent pas : 6 h 42 pour le bot V2, 6 h 53 pour le profil 1.

## Former l'univers remet tout à zéro (0.83)

Depuis la 0.83, former l'univers remet à zéro tout ce qui y a mené : Big Bangs, matière noire, molécules, astres, cosmos. L'univers, lui, reste formé (`g.hasCosmos(Cosmos.UNIVERSE)`), mais `g.cosmosFormed()` retombe à 0. Pour cette raison :
- les bots repèrent l'univers par `RobotCosmosPlanner.cosmosStage(g)`, qui vaut 3 une fois l'univers formé ;
- le bilan du bot V2 (`result.csv`) garde les Big Bangs et les astres d'avant la remise à zéro ;
- la ligne `RESULT` des 24 profils montre, elle, l'état d'après : `bb=0`, `bodies=0`. Le bilan d'avant est dans `g.stats().universeTally()` ;
- `winner.save` et `checkpoint_universe_finished.save` sont désormais des parties remises à zéro, dans l'univers ;
- la partie « fin de partie » de l'empreinte change donc entre la 0.82 et la 0.83 : c'est voulu.

Les durées jusqu'à l'univers ne changent pas : 6 h 42 pour `DEPENDANCES` et 6 h 54 pour le profil 1, comme en 0.82.

## Les bots dans le jeu (0.84)

Le profil de test (`./gradlew :app:runTest`) a un groupe « Bots ». Il fait jouer le bot V2 (`AdvancedLab`) sur la partie en cours, avec la politique choisie dans une liste (`DEPENDANCES` d'abord) :
- **en direct** : une décision toutes les 20 s de jeu, vitesse du temps comprise ;
- **en avance rapide** : 10 min, 1 h, 6 h, ou jusqu'au prochain Big Bang (12 h au plus) ;
- **une décision** à la fois.

Le lanceur ajoute le module à l'exécution (`runtimeOnly(project(":bots"))`). L'interface trouve le bot par son nom (`idle.ui.BotBridge`), sans dépendre de ce module. Sans les bots, les outils le disent et ne font rien.

## L'empreinte du moteur : prouver qu'une optimisation ne change rien

```bash
python run.py empreinte --out sorties/avant          # sur la version de référence du jeu
# … modifier le moteur …
python run.py empreinte --out sorties/apres
python run.py comparer sorties/avant sorties/apres   # « IDENTIQUES » pour les 4 parties, code 0
```

`Empreinte` joue quatre parties fixes avec les bots :
- le début : 10 h depuis zéro ;
- le premier Big Bang : 6 h ;
- la fin de partie : le bot V2 jusqu'à l'univers ;
- un joueur absent : 30 h, à raison de 30 minutes toutes les 4 h.

Elle écrit ensuite leurs sauvegardes finales, et `outils/comparer_sauvegardes.py` les compare. Les textes et les entiers doivent être identiques, et les nombres réels égaux à un milliardième près.

Pourquoi pas au bit près : `Math.pow`, `Math.log`… peuvent changer de dernier chiffre selon le moment où la JVM compile le code. Deux exécutions du même moteur ne donnent donc pas toujours les mêmes bits.

## À savoir avant de lire les résultats

- **Version du moteur.** Les campagnes de `donnees/` ont tourné sur le moteur copié dans l'ancien kit. Ce moteur est identique au cœur du jeu en 0.79 et 0.80 ; la 0.81 n'a rien changé avant l'univers.
- **Résultats reproduits sur la version actuelle.** Le 19 octobre 2026, ces résultats sont retombés exactement sur les mêmes valeurs :
  - `DEPENDANCES` : 7 h 38 ;
  - profil 1 : 8 h 17 ;
  - profil 21 : 145 h 09 ;
  - profil 22 : 145 h 51.
- **La graine ne change presque rien après 7 Big Bangs.** La fin du jeu n'y dépend plus du hasard (7 h 38 avec la graine 42, 7 h 39 avec la 43). Plusieurs graines ne valent donc pas plusieurs essais à ce stade.
- **Les anciennes campagnes ont tourné avant les correctifs du 19 octobre** (voir plus haut) : fusion coupée après le Big Bang, matière jamais créée en l'absence du robot, absence sans plafond. Les reproductions de ces chiffres datent d'avant les correctifs.
- **Les profils partis de zéro ne savent pas faire le premier Big Bang.** Ils ne planifient ni les défis ni ses conditions. Leurs nombres d'explosions (une par décision, toutes les 20 s) ne mesurent pas le jeu.
