# Idle — les bots de simulation

Ce dossier est le module `bots` du projet `idle`. Il vient du kit séparé `Idle_Tous_Les_Bots` : les robots et leurs sauvegardes. (Les données brutes, le rapport consolidé et les anciens robots du kit ont été retirés du dépôt le 20 octobre 2026.)

Les bots pilotent **le vrai moteur du jeu** (`idle/core`), sans interface. Ils ne font pas partie du jeu livré aux joueurs : depuis la version 0.126, seul le profil de test (`runTest`) les ajoute, ni `run` ni la distribution. Ce sont des politiques de jeu programmées, pas des joueurs réels.

**Une seule victoire, celle du jeu :** l'univers formé (`game.hasCosmos(Cosmos.UNIVERSE)`, ce que vérifie l'écran de fin). `VerifyVictory`, `FreshUniverseAudit` et les campagnes s'en tiennent là depuis la 0.126.

**Les noms des profils** sont ceux du code (`IdleProfiles24`) et des fichiers de sauvegarde : `Speedrunner`, `Veteran`, `Regular`… `PROFILS_24.md` en donne la description en français.

## Ce qui a changé à la fusion (19 octobre 2026)

| Avant (kit séparé) | Maintenant (module `idle/bots`) |
|---|---|
| `moteur/` : une copie du moteur, à recopier à chaque version du jeu | supprimé : les bots se compilent contre `idle/core`, le moteur du jeu lui-même |
| `bin/` : des fichiers `.class` déjà compilés | supprimé : `run.py` ou Gradle recompilent |
| `bots/v2/` et `bots/profils_24/` (avec `IdleProfiles24.java` en double) | `src/main/java/` : les sources compilées, une seule fois chacune (13 à la fusion, 21 aujourd'hui) |
| `bots/historique/` | gardé à la fusion, puis retiré du dépôt le 20 octobre 2026 |
| `docs/DOCUMENTATION_JEU_107_PAGES.pdf` | supprimé (le PDF qui était à la racine du dépôt a été retiré le 21 octobre 2026) |
| `INVENTAIRE_SHA256.csv` | supprimé : les chemins ont changé, et git garde déjà l'empreinte de chaque fichier |
| chemins `/mnt/data/...` dans quatre bots | chemins relatifs à ce dossier (`sauvegardes/reference/...`, `sorties/`) |

À la fusion, `donnees/`, `sauvegardes/`, `docs/` et `scripts_originals/` étaient gardés tels quels. Seul `sauvegardes/` reste : les trois autres ont été retirés du dépôt le 20 octobre 2026.

## Ce que garantit la fusion

- **Les bots suivent le jeu.** `run.py` recompile le moteur depuis `idle/core/src/main/java` dès qu'une source a changé. Une modification du jeu est donc prise en compte au lancement suivant.
- **Des tests à chaque envoi.** Les tests de `src/test/java` tournent avec ceux du projet (`./gradlew toutTester`, et l'action GitHub). `BotsTest` vérifie notamment que :
  - les bots compilent contre le moteur actuel ;
  - les 34 sauvegardes se relisent ;
  - les 24 sauvegardes de victoire sont toujours des victoires, et qu'une victoire écrite par le moteur actuel (univers formé, partie remise à zéro) en est une aussi ;
  - le bot V2, un des 24 profils (le 19) et le profil 1 parti de zéro tournent quelques minutes sans planter.

  Les autres classes de test gardent les politiques (ordre d'ouverture, réserve de matière noire, garde-fou des profils) et, à part, les campagnes longues des 24 profils (`Profiles24CampaignTest`, `Profiles24LongCampaignTest`), qui tournent dans leurs propres jobs. Si une évolution du jeu casse un bot ou une sauvegarde, l'envoi le signale.

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
| `src/main/java/` | Les 21 bots et outils compilés. Les politiques : `AdvancedLab` (les 12 politiques V2), `RobotCosmosPlanner` (le planificateur qu'elles partagent), `IdleProfiles24` (les 24 profils de fin de partie), `FreshPhaseProfiles` (les mêmes depuis une partie neuve), `FirstUniversePolicy` (l'économie des trois actes, l'ordre d'ouverture), `NextUniversePolicy` (ce que le deuxième univers ajoute, version 0.130), `DarkMatterReservePolicy`, `ProfileProgressGuard` (le garde-fou des blocages), `CorrectedCoach` et `RobotPhase3`. Les campagnes et contrôles : `FreshUniverseAudit`, `VerifyVictory`, `Empreinte`, `OfflineBenchmark`, `AutoMatterBench` et `MakeAutoMatterCheckpoint` (pour `outils/ab_matiere.py`). Les audits ponctuels, lancés à la main avec `./gradlew :bots:bot -Pclasse=…` : `DependencyAudit`, `EconomyAudit`, `DiagnoseState`, `RescueBench` et `RecreateLosers`. |
| `src/test/java/` | Les 8 classes de test : `BotsTest` (cohérence avec le jeu), `NextUniverseTest` (l'univers 2, version 0.130), `BotsExtendedValidationTest`, `DarkMatterReservePolicyTest`, `ProfileProgressGuardTest`, `ProfileProgressGuardFailureTest`, et les campagnes `Profiles24CampaignTest` et `Profiles24LongCampaignTest`. |
| `sauvegardes/` | `reference/` : 8 points de départ, dont la partie à 7 Big Bangs, une partie neuve au 5e Big Bang (`checkpoint_fresh_fifth_bang.save`, profil 1, graine 201) et le premier univers formé par le même Speedrunner (`checkpoint_universe1_speedrunner.save`, version 0.130), départ de l'univers 2. `profils_bloques/` : les profils 11, 12 et 17 à 72 h. `victoires_corrigees/` : 2 parties (12 et 17 ; le profil 11 gagne aussi depuis les correctifs, mais sa victoire n'a pas été rangée ici). `victoires_historiques/` : les 21 victoires. |
| `outils/` | `comparer_sauvegardes.py` (utilisé par `run.py comparer`), `ab_matiere.py` (création automatique de la matière, avec et sans) et `benchmark_actualise.py`. |

## Les correctifs des bots (19 octobre 2026)

Trois défauts des bots faussaient les anciennes campagnes. Ils sont corrigés dans `RobotCosmosPlanner`, que tous les profils et le bot V2 utilisent.

1. **La fusion et la synthèse restent coupées après le Big Bang.** Le robot les coupait pour préparer le Big Bang, puis ne les rallumait jamais. C'est ce qui bloquait les profils 11, 12 et 17. Il les rallume maintenant dès qu'il n'en a plus besoin.
2. **La matière ne se créait qu'aux décisions du robot.** La création automatique était toujours coupée, donc la matière des derniers astres et du cosmos ne se créait jamais pendant l'absence ni entre deux décisions. Le robot fait maintenant ce que ferait un joueur avisé : pour chaque état dont le prochain pas demande de la matière, il confie à la création automatique l'amas rassemblé le moins encombrant (« amas choisis »), et la coupe une fois la matière réunie.
3. **L'absence n'était pas plafonnée.** Elle compte maintenant comme dans le jeu : au taux hors ligne, et 24 h au plus par absence (`RobotCosmosPlanner.away`).

Résultats depuis la partie à 7 Big Bangs, graine 42, horizon 14 jours :

| Profil | Avant | Après les correctifs | Dont temps actif |
|---|---|---|---|
| Bot V2 `DEPENDANCES` | 7 h 38 | 6 h 42 | tout |
| 1 Speedrunner | 8 h 17 | 6 h 54 | tout |
| 11 PrestigeAddict | bloqué | 15 h 10 | tout |
| 12 Hoarder | bloqué | 10 h 02 | tout |
| 17 AFKOpen | bloqué | 27 h 00 | tout |
| 19 MobileSnacker | 184 h 00 | 12 h 01 | 1 h 01 |
| 21 EveningPlayer | 145 h 09 | 72 h 00 | 6 h 00 |
| 22 WeekendPlayer | 145 h 51 | 51 h 26 | 7 h 26 |
| 23 Forgetful | 254 h 20 | 15 h 00 | 5 h 00 |
| 24 ReturningVeteran | 156 h 00 | 18 h 00 | 1 h 30 |

**Le rapport consolidé et les CSV de l'ancien dossier `donnees/` (retirés du dépôt depuis) dataient d'avant ces correctifs.** Leurs durées de fin de partie, surtout celles des profils intermittents, mesuraient les défauts des bots plus que le jeu.

Le correctif 2 a un revers : il montre qu'un joueur qui laisse la création automatique sur « suivre la liste » reste lent. Le jeu ne lui crée pas la matière des astres et du cosmos. Seul celui qui choisit lui-même ses amas avance seul.

## Les bots partis de zéro suivent le modèle 0.123 (22 octobre 2026)

*Les durées de cette section ont été mesurées avant le rééquilibrage de la version 0.125 : elles ne valent plus pour le
jeu actuel. Les durées actuelles sont dans la section « Le jeu réglé sur le Speedrunner (version 0.125) » plus bas.*

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

`FreshUniverseAudit` sur 10 des 24 profils, partie neuve, plafond de 168 h réelles (22 octobre 2026, avant la 0.125) :

| Profil | Verdict | Univers formé (heures réelles) |
|---|---|---|
| 1 Speedrunner | WIN | 16 h 26 |
| 2 Veteran | WIN | 17 h 37 |
| 3 Regular | WIN | 25 h 49 |
| 5 AchievementHunter | WIN | 24 h 49 |
| 11 PrestigeAddict | WIN | 22 h 36 |
| 12 Hoarder | WIN | 77 h 26 |
| 16 Beginner | WIN | 90 h 06 |
| 24 ReturningVeteran | WIN | 153 h 00 (75 h de jeu) |
| 21 EveningPlayer | LIMIT | galaxie formée, 8 Big Bangs, 35 astres à 168 h (14 h en ligne) |
| 17 AFKOpen | LIMIT | aucun Big Bang : une visite par heure ne permet qu'une fusion par visite |

## Le jeu réglé sur le Speedrunner (version 0.125)

Depuis la version 0.125, l'équilibrage du premier univers suit le profil 1 (Speedrunner) de `FreshUniverseAudit`,
partie neuve, graine 201, une décision toutes les 20 s. En 0.124, il formait l'univers en 14 h 28
(6 h 03 + 1 h 37 + 6 h 48) : le deuxième acte ne durait plus qu'une heure et demie. Les réglages de la 0.125
(tempo 12, Big Bangs à 10¹⁹⁰⁰⁰⁰ puis 10⁵⁰⁰⁰⁰ particules, vitesse essoufflée dès 300 niveaux, puissances plus fortes,
maturation ×3 et impulsions à 18 s, rassemblements de 2 h) donnent :

| Profil | Premier acte | Deuxième acte | Troisième acte | Univers formé |
|---|---|---|---|---|
| 1 Speedrunner | 5 h 08 | 6 h 14 | 16 h 01 | 27 h 23 |
| 2 Veteran | 5 h 19 | 6 h 15 | 16 h 55 | 28 h 28 |
| 3 Regular | | | | 34 h 50 |
| 11 PrestigeAddict | | | | 33 h 42 |

Parts moyennes de la production du Speedrunner sur tout le premier univers : ciel 31 %, tableau périodique 23 %,
améliorations 15 %, matière noire 15 %, collisionneur 5 %, succès 2 %. Chaque palier de Big Bang ouvert est payé
après 4 à 10 minutes.

Le profil 17 bute sur la règle du jeu plus que sur sa politique : sans automatisme avant 30 atomes créés, une
fusion par visite horaire demande environ 88 h pour ouvrir le tableau ; l'explosion efface ensuite les
automatismes, et 0,8 matière noire ne paie ni « Départ lancé » (2) ni les automatismes offerts (3).

## L'univers 2 (version 0.130)

Les mêmes bots jouent maintenant l'univers suivant, sans code en double : un **nombre d'univers visé**
(`targetUniverses` dans `RobotCosmosPlanner`) décide quand la partie s'arrête. Il vaut **1 par défaut** : le premier
univers se joue et se mesure exactement comme avant (Speedrunner, graine 201 : toujours 27 h 23, 4 928 décisions).

- **À 2**, dès que l'univers se forme, le bot lance le suivant (`NextUniversePolicy.launchNext`) et rejoue les trois
  actes. La victoire est alors l'univers 2 formé.
- **Ce que le deuxième univers ajoute** est dans `NextUniversePolicy`, que tous les bots appellent à chaque décision
  (rien ne s'y passe dans le premier univers) : le régime rapide relancé dès qu'il est prêt, un choix de cycle par
  Big Bang (fusion par défaut, `-Didle.cycle=FUSION|SYNTHESIS|RESERVE` pour essayer les autres), la famille préférée
  qui suit la famille qui manque aux recettes, et les éléments offerts (d'abord ceux qui manquent à la liste, sinon le
  plus lourd). La Fuite, onzième défi, se tente avec les autres, mais jamais quand elle retarderait un Big Bang
  prêt ou en préparation : le Big Bang ne demande que les dix de toujours (les bots comptent donc
  `baseChallengesDone()`). Au lancement de l'univers suivant, les défis abandonnés se retentent normalement (le
  compteur d'explosions repart de zéro).
- **Ce que les bots ne jouent pas, pour l'instant** : les dimensions, la graine et l'onglet Univers (améliorations,
  recherches, épreuves, skins). Comme la graine n'est pas jouée, l'univers suivant se lance comme l'outil d'essai
  « Rouvrir les paliers », **sans attendre le palier 2 de la graine** qu'un joueur doit atteindre (30 arrosages et le
  seuil de la ligne) : le temps de la graine n'est pas compté.

Pour lancer :

```bash
python run.py audit --de 1 --a 1 --univers 2            # partie neuve, univers 1 puis univers 2
python run.py profils --id 3 --univers 2 --checkpoint sauvegardes/reference/checkpoint_universe1_speedrunner.save --hours 80
python run.py v2 --bot DEPENDANCES --univers 2 --checkpoint sauvegardes/reference/checkpoint_universe1_speedrunner.save --hours 60
./gradlew :bots:bot -Pclasse=FreshUniverseAudit --args="1 3 168 20 sorties/audit 2"     # 7e argument : univers
```

L'audit des 24 profils de l'action GitHub a la même option (`univers`, 1 par défaut). Les sorties gagnent les colonnes
`univers_vises`, `univers_formes` et `univers_h` (instant de formation de chaque univers, en heures réelles) ; la ligne
`RESULT` des profils gagne `universes`, `target`, `won` (la cible atteinte), `universeTimes` et ce que la politique a
fait (`regimes`, `cycles`, `families`, `freeElements`). Les jalons `BB8at`, `galaxyAt`, `clusterAt` et `universeAt`
restent ceux du premier univers formé pendant la partie : avec `--univers 2`, c'est `won` qui dit la victoire.
`univers_h` et `universeTimes` ne listent que les univers formés pendant la partie (depuis la sauvegarde de
l'univers 1, seul l'univers 2), `univers_formes` compte tous ceux de la partie.

Mesures (version 0.130, une décision toutes les 20 s, univers 2 compté depuis son lancement ; sauf mention, départ
de `checkpoint_universe1_speedrunner.save`) :

| Bot | Choix de cycle | Premier Big Bang | 8e Big Bang | Galaxie | Univers 2 formé |
|---|---|---|---|---|---|
| 1 Speedrunner | synthèse | 6 h 02 | 13 h 22 | 17 h 08 | 21 h 08 |
| 1 Speedrunner | fusion (défaut) | | 13 h 26 | 17 h 19 | **21 h 20** |
| 1 Speedrunner | réserve | | 13 h 39 | 17 h 25 | 21 h 26 |
| 1 Speedrunner | sans les nouveautés | 6 h 03 | 13 h 20 | 17 h 06 | 21 h 07 |
| 1 Speedrunner, partie neuve d'une traite | synthèse | 5 h 45 | 13 h 10 | 17 h 04 | 21 h 04 (48 h 27 en tout) |
| 3 Regular, graines 203, 204, 205 | fusion (défaut) | 7 h 51 (203) | 15 h 30 (203) | 19 h 22 (203) | **23 h 26, 23 h 51, 23 h 40** |
| 3 Regular, graines 203, 204, 205 | synthèse | | | | 25 h 12, 24 h 43, 23 h 34 |
| 3 Regular, graine 203 | réserve | | 18 h 19 | 22 h 16 | 26 h 19 |
| Bot V2 `DEPENDANCES` | synthèse | 5 h 48 | 21 h 37 | 32 h 37 | 36 h 38 |

Ce que montrent ces mesures :

- L'univers 2 prend environ **21 h** au Speedrunner (23 h 30 au Régulier), contre 27 h 23 (34 h 50) pour le
  premier : le premier Big Bang vient en 6 h au lieu de 11 h 22, mais le troisième acte dure encore 15 h (16 h dans
  le premier).
- **Les nouveautés du deuxième univers ne changent presque rien** au Speedrunner : 21 h 08 avec, 21 h 07 sans. Son
  troisième acte est réglé par des délais (maturation des astres, deux rassemblements de 2 h), pas par la production
  que le régime, le cycle ou les éléments offerts accélèrent.
- Pour le Speedrunner, les trois choix de cycle se tiennent en 20 minutes, dans le bruit du hasard. Pour le Régulier,
  la fusion est la plus régulière (23 h 26 à 23 h 51 sur trois graines, contre 23 h 34 à 25 h 12 avec la synthèse) :
  c'est le défaut.

## Chaque profil à sa façon, et les anomalies du ciel (version 0.131)

Dans le deuxième univers, chaque profil fait maintenant ses propres choix au lieu de jouer en expert : choix de
cycle, régime rapide, famille préférée, éléments offerts (`IdleProfiles24.STYLES`, détaillé profil par profil dans
`PROFILS_24.md`). Le thésauriseur, le méthodique et l'accro au prestige mettent en réserve ; le chimiste, le
collectionneur et l'astronome prennent la synthèse ; le débutant, l'optimiste passif et l'AFK n'utilisent jamais le
régime rapide ; le cliqueur désordonné tire son cycle au hasard ; le débutant, le cliqueur occasionnel et le distrait
prennent le premier élément offert. Un oubli de choix de cycle vaut pour tout le cycle, et un oubli d'éléments
offerts pour toute la fournée, pas seulement pour une décision. Chaque profil a son propre hasard pour ces gestes :
ils ne déplacent pas ses autres tirages. Le Speedrunner (et le vétéran revenant) jouent en experts et ne tirent
aucun hasard pour cela : leurs parties de l'univers 2 sont celles de la 0.130.

Les **anomalies du ciel** (version 0.127) sont saisies, dans les deux univers : par chaque profil, à une décision où
une anomalie est visible, avec la chance de son attention au ciel (un hasard à part) ; par le bot V2 à chaque
décision, sauf en mode `SANS_COMETES`. `-Didle.sansAnomalies=true` les fait ignorer, comme avant.

Effet sur le premier univers (Speedrunner, partie neuve, graine 201) : **27 h 01** avec les anomalies (53 saisies),
27 h 23 sans, exactement comme avant. Deuxième univers, depuis `checkpoint_universe1_speedrunner.save`, graine
200 + id :

| Profil | Façon de jouer | Univers 2 formé | Anomalies |
|---|---|---|---|
| 1 Speedrunner | expert, fusion | 21 h 16 | 55 |
| 7 Chemist | synthèse, régime 80 % | 22 h 27 | 54 |
| 3 Regular | fusion, régime 70 %, oublie un cycle sur dix | 24 h 20 | 55 |
| 10 PassiveOptimist | réserve, jamais de régime | 36 h 22 | 61 |
| 12 Hoarder | réserve, régime 40 % | 63 h 50 | 79 |
| 16 Beginner | fusion, jamais de régime, premier élément | 73 h 42 | 62 |
| 23 Forgetful | synthèse, oublie la moitié des cycles | pas en 150 h (premier Big Bang à 71 h) | 25 |

Le distrait reste lent pour lui-même, pas à cause de ses choix : sans les nouveautés du deuxième univers ni les
anomalies, il en est au même point (premier Big Bang à 71 h, 7 Big Bangs à 150 h). Il n'est en ligne qu'un tiers du
temps, décide toutes les trois minutes et laisse passer près d'une occasion sur deux.

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

Le lanceur ajoute le module à l'exécution, et seulement pour `runTest` (configuration `testProfileBots` de `idle/app/build.gradle.kts`, depuis la 0.126 ; avant, `runtimeOnly(project(":bots"))` les mettait aussi dans `run` et dans la distribution). L'interface trouve le bot par son nom (`idle.ui.BotBridge`), sans dépendre de ce module. Sans les bots, les outils le disent et ne font rien.

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

Elle écrit ensuite leurs sauvegardes finales, et `outils/comparer_sauvegardes.py` les compare. Les textes et les entiers doivent être identiques, et les nombres réels égaux à un milliardième près, quel que soit leur exposant. (Avant la 0.126, deux nombres dont les exposants différaient de 5 ou plus passaient pour égaux : 1.5e10 et 1.5e100 sortaient « IDENTIQUES ». Une empreinte comparée avec l'ancien outil est à refaire.)

Pourquoi pas au bit près : `Math.pow`, `Math.log`… peuvent changer de dernier chiffre selon le moment où la JVM compile le code. Deux exécutions du même moteur ne donnent donc pas toujours les mêmes bits.

## À savoir avant de lire les résultats

- **Version du moteur.** Les campagnes de l'ancien dossier `donnees/` ont tourné sur le moteur copié dans l'ancien kit. Ce moteur est identique au cœur du jeu en 0.79 et 0.80 ; la 0.81 n'a rien changé avant l'univers.
- **Résultats reproduits sur la version actuelle.** Le 19 octobre 2026, ces résultats sont retombés exactement sur les mêmes valeurs :
  - `DEPENDANCES` : 7 h 38 ;
  - profil 1 : 8 h 17 ;
  - profil 21 : 145 h 09 ;
  - profil 22 : 145 h 51.
- **La graine ne change presque rien après 7 Big Bangs.** La fin du jeu n'y dépend plus du hasard (7 h 38 avec la graine 42, 7 h 39 avec la 43). Plusieurs graines ne valent donc pas plusieurs essais à ce stade.
- **Les anciennes campagnes ont tourné avant les correctifs du 19 octobre** (voir plus haut) : fusion coupée après le Big Bang, matière jamais créée en l'absence du robot, absence sans plafond. Les reproductions de ces chiffres datent d'avant les correctifs.
- **Les profils partis de zéro.** Jusqu'au 21 octobre 2026, ils ne savaient pas faire le premier Big Bang. Depuis le 22 (`FirstUniversePolicy`, voir plus haut), la plupart forment l'univers ; les profils 17 et 21 restent au plafond de 168 h (`LIMIT`).
