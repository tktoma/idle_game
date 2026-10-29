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
| `bots/v2/` et `bots/profils_24/` (avec `IdleProfiles24.java` en double) | `src/main/java/` : les sources compilées, une seule fois chacune (13 à la fusion, 13 aujourd'hui après le ménage de la 0.141) |
| `bots/historique/` | gardé à la fusion, puis retiré du dépôt le 20 octobre 2026 |
| `docs/DOCUMENTATION_JEU_107_PAGES.pdf` | supprimé (le PDF qui était à la racine du dépôt a été retiré le 21 octobre 2026) |
| `INVENTAIRE_SHA256.csv` | supprimé : les chemins ont changé, et git garde déjà l'empreinte de chaque fichier |
| chemins `/mnt/data/...` dans quatre bots | chemins relatifs à ce dossier (`sauvegardes/reference/...`, `sorties/`) |

À la fusion, `donnees/`, `sauvegardes/`, `docs/` et `scripts_originals/` étaient gardés tels quels. Seul `sauvegardes/` reste : les trois autres ont été retirés du dépôt le 20 octobre 2026.

## Ce que garantit la fusion

- **Les bots suivent le jeu.** `run.py` recompile le moteur depuis `idle/core/src/main/java` dès qu'une source a changé. Une modification du jeu est donc prise en compte au lancement suivant.
- **Des tests à chaque envoi.** Les tests de `src/test/java` tournent avec ceux du projet (`./gradlew toutTester`, et l'action GitHub). `BotsTest` vérifie notamment que :
  - les bots compilent contre le moteur actuel ;
  - les 35 sauvegardes se relisent ;
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
python run.py fresh --id 1 --hours 4 --step 20 --seed 42       # les deux premiers actes seulement, depuis zéro
python run.py audit --de 1 --a 1 --univers 11 --hours 300      # partie neuve jusqu'à onze univers (version 0.152)
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
| `src/main/java/` | Les 14 bots et outils compilés. Les politiques : `AdvancedLab` (les 12 politiques V2), `RobotCosmosPlanner` (le planificateur qu'elles partagent), `IdleProfiles24` (les 24 profils de fin de partie), `FreshPhaseProfiles` (les mêmes depuis une partie neuve, les deux premiers actes seulement), `FirstUniversePolicy` (l'économie des trois actes, l'ordre d'ouverture), `NextUniversePolicy` (ce que le deuxième univers ajoute, version 0.130), `VoidPolicy` (le vide du troisième univers, version 0.133), `UniverseTreePolicy` (la graine, les bulles de l'arbre, les hadrons et la ligne, à partir de l'univers 4, version 0.152), `DimensionBots` (un bot par dimension, à part, version 0.136), `ProfileProgressGuard` (le garde-fou des blocages) et `Bots` (le chargement d'une partie et les choix « le moins cher » partagés). Les campagnes et contrôles : `FreshUniverseAudit`, `VerifyVictory` et `Empreinte`. Les outils d'un diagnostic passé (`CorrectedCoach`, `RescueBench`, `RecreateLosers`, `RobotPhase3`, `DependencyAudit`, `DiagnoseState`, `EconomyAudit`, `OfflineBenchmark`, `AutoMatterBench`, `MakeAutoMatterCheckpoint`, `DarkMatterReservePolicy`) ont été retirés en 0.141 : rien ne les lançait plus, et les défauts qu'ils corrigeaient ou mesuraient sont réglés dans `RobotCosmosPlanner` (git les garde). |
| `src/test/java/` | Les 12 classes de test : `UniverseTreePolicyTest` (les gestes des univers 4 et suivants, version 0.152), `BotsTest` (cohérence avec le jeu, et les correctifs de la 0.141), `NextUniverseTest` (l'univers 2, version 0.130), `VoidPolicyTest` (le vide, version 0.133), `EarlyStyleTest` (les styles des trois actes, version 0.135), `DimensionBotsTest` (les dimensions, version 0.136), `BotsExtendedValidationTest`, `ProfileProgressGuardTest`, `ProfileProgressGuardFailureTest`, et les campagnes `Profiles24CampaignTest` et `Profiles24LongCampaignTest`, et `ElevenUniversesLongTest` (le Speedrunner jusqu'à onze univers depuis zéro, de l'ordre d'une heure : il ne tourne qu'avec `-PonzeUnivers=true`). |
| `sauvegardes/` | `reference/` : 9 points de départ, dont la partie à 7 Big Bangs, une partie neuve au 5e Big Bang (`checkpoint_fresh_fifth_bang.save`, profil 1, graine 201), le premier univers formé par le même Speedrunner (`checkpoint_universe1_speedrunner.save`, version 0.130), départ de l'univers 2, et le deuxième (`checkpoint_universe2_speedrunner.save`, version 0.133), départ de l'univers 3. `profils_bloques/` : les profils 11, 12 et 17 à 72 h. `victoires_corrigees/` : 2 parties (12 et 17 ; le profil 11 gagne aussi depuis les correctifs, mais sa victoire n'a pas été rangée ici). `victoires_historiques/` : les 21 victoires. |
| `outils/` | `comparer_sauvegardes.py` (utilisé par `run.py comparer`) et `benchmark_actualise.py` (les 24 profils depuis la partie à 7 Big Bangs). `ab_matiere.py`, l'A/B du suivi automatique de la matière, a été retiré en 0.141 : l'option est dans le jeu depuis la 0.12x (page des automatismes). |

## Les propriétés système des bots

Toutes se passent à la JVM (`java -Didle.cycle=FUSION -cp … IdleProfiles24 …` ; avec Gradle, dans `--args` elles ne passent pas : les poser dans `JAVA_TOOL_OPTIONS`). Aucune n'est lue par le jeu lui-même.

| Propriété | Classe | Effet | Défaut |
|---|---|---|---|
| `idle.cycle` | `NextUniversePolicy` | Le choix de cycle de tous les profils dans l'univers 2 : `FUSION`, `SYNTHESIS` ou `RESERVE` ; autre valeur refusée. | celui du profil (`RESERVE` pour l'expert) |
| `idle.sansNouveautesUnivers2` | `NextUniversePolicy` | `true` : l'univers 2 se rejoue sans ses nouveautés (régime, cycle, famille, éléments offerts). | `false` |
| `idle.sansAnomalies` | `RobotCosmosPlanner` | `true` : les anomalies du ciel ne sont pas saisies, comme avant la 0.131. | `false` |
| `idle.defisBB` | `RobotCosmosPlanner` | L'ordre des défis de Big Bang à partir de l'univers 2 : `maturation`, `gele` ou `avant` ; autre valeur refusée (0.141 : avant, elle jouait `gele` en silence). | `maturation` |
| `idle.defisBBPremier` | `RobotCosmosPlanner` | `true` : la même règle des défis de Big Bang dès le premier univers (change la partie de référence). | `false` |
| `idle.sansVide` | `VoidPolicy` | `true` : le vide reste fermé dans l'univers 3. | `false` |
| `idle.traceVide` | `VoidPolicy` | `true` : chaque geste du vide s'écrit (`VIDE,…`). | `false` |
| `idle.partVide` | `VoidPolicy` | La part du collisionneur et de l'espace libre laissée au vide par tous les profils, de 0 à 1 ; autre valeur refusée (0.141). | celle du profil |
| `idle.croissanceVide` | `VoidPolicy` | La part du prochain raccourci qu'une amélioration de croissance du vide peut coûter, pour l'expert. | `0.5` |
| `idle.masseTrouNoir` | `VoidPolicy` | La masse du trou noir au-delà de laquelle on ne le nourrit plus (`0` : jamais). | `10` |
| `idle.sansArbre` | `UniverseTreePolicy` | `true` : rien de l'univers 4 et suivants (ni graine, ni bulles, ni hadrons) ; l'univers suivant se lance dès l'univers formé, comme avant la 0.152. | `false` |
| `idle.traceArbre` | `UniverseTreePolicy` | `true` : chaque bulle, ouverture des hadrons et automatisme des dimensions s'écrit (`ARBRE,…`). | `false` |
| `idle.bulleSansDelai` | `UniverseTreePolicy` | `true` : les bots achètent aussi la bulle du bonsaï, à droite (automatismes sans délai), qui rend la simulation plus de vingt fois plus lente. | `false` |
| `idle.onzeUnivers.heures`, `idle.onzeUnivers.univers` | `ElevenUniversesLongTest` | Le plafond en heures simulées et le nombre d'univers visés du test long. | `1000` et `11` |
| `idle.stage` | `IdleProfiles24` | Le suffixe du CSV d'un profil (`01_Speedrunner_<stage>.csv`) ; le 7e argument de `IdleProfiles24` le pose. | `late` |
| `idle.lenteurDimension` | `DimensionBots` | Le joueur des dimensions est autant de fois plus lent à réagir, viser et revenir. | `1` |
| `idle.achatDimension` | `DimensionBots` | `moinsCher` : il achète le moins cher au lieu du meilleur rapport. | meilleur rapport |
| `idle.objectifDimension` | `DimensionBots` | Les éclats à gagner au lieu du seuil du jeu, ou `infini` (jouer jusqu'au temps donné). | le seuil du jeu |
| `idle.testProfilesStart`, `idle.testProfilesEnd` | `Profiles24CampaignTest`, `Profiles24LongCampaignTest` | Le segment de profils d'une campagne de test (la CI en lance quatre). | `1` et `24` |

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
  Big Bang (la réserve par défaut depuis la 0.135, `-Didle.cycle=FUSION|SYNTHESIS|RESERVE` pour essayer les autres ; un
  défi de Big Bang qui remet à zéro rend aussi le choix, et l'expert le refait, correctif 0.141), la famille préférée
  qui suit la famille qui manque aux recettes, et les éléments offerts (d'abord ceux qui manquent à la liste, sinon le
  plus lourd). La Fuite, onzième défi, se tente avec les autres, mais jamais quand elle retarderait un Big Bang
  prêt ou en préparation : le Big Bang ne demande que les dix de toujours (les bots comptent donc
  `baseChallengesDone()`). Au lancement de l'univers suivant, les défis abandonnés se retentent normalement (le
  compteur d'explosions repart de zéro).
- **Ce que les bots ne jouent pas, pour l'instant** : les dimensions, la graine et l'onglet Univers (améliorations,
  recherches, épreuves, skins). Comme la graine n'est pas jouée, l'univers suivant se lance comme l'outil d'essai
  « Rouvrir les paliers », **sans attendre le palier 2 de la graine** qu'un joueur doit atteindre (30 arrosages ; depuis
  la 0.139, le seuil de la ligne n'y entre plus) : le temps de la graine n'est pas compté.

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
fait (`regimes`, `cycles`, `families`, `freeElements`), puis `skyEvents` (anomalies du ciel saisies, 0.131), `voidOpened`,
`voidBought` et `sacrifices` (le vide, 0.133). Dans `result.csv` du bot V2, la colonne `univers_h` est l'instant du
premier univers formé et `instants_univers_h` (nommée `univers_h` en double avant la 0.141) la liste de tous.
Les jalons `BB8at`, `galaxyAt`, `clusterAt` et `universeAt`
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

## L'univers 3 et le vide (version 0.133)

Les bots jouent aussi l'univers 3, de la même manière : `--univers 3` (ou le 7e argument de `FreshUniverseAudit`,
ou l'entrée « univers » de l'action GitHub) enchaîne les trois univers, et `checkpoint_universe2_speedrunner.save`
(le deuxième univers formé par le Speedrunner, graine 201) part directement de l'univers 3. Ce que le troisième univers
ajoute, le vide, est dans `VoidPolicy`, que tous les bots appellent à chaque décision ; rien ne s'y passe tant que le
vide ne se propose pas, et les univers 1 et 2 restent exactement ceux d'avant (Speedrunner : 27 h 01 puis 48 h 13 en
tout, mêmes 8 679 décisions).

- **Ouvrir le vide** dès qu'il est payable (selon le profil) ; tant qu'il se propose, le collisionneur garde son
  énergie pour lui : ses achats et ses automates attendent, deux heures de jeu au plus.
- **La part** laissée au vide : 50 % pour les joueurs qui le règlent, 25 % (le réglage du jeu) pour les autres,
  75 % pour l'Explorateur. Mesuré sur le Speedrunner : 50 % est le meilleur (14 h 22, contre 14 h 55 à 25 % et
  14 h 41 à 75 %).
- **Les achats** : au mieux (d'abord maturation, rassemblement et allègement, le moins cher des trois ; une croissance
  quand elle est bon marché à côté ou que le prochain raccourci est hors de portée de l'équilibre), le moins cher, ou
  au hasard.
- **Le trou noir** : une fois les huit Big Bangs faits, le joueur qui le nourrit lui donne ses particules ; comme la
  fusion les vide sans cesse, il coupe la fusion automatique jusqu'à la décision suivante, puis la relance.
- La façon de jouer de chaque profil est dans `PROFILS_24.md` ; `-Didle.traceVide=true` écrit chaque geste du vide ; `-Didle.sansVide=true` laisse le vide fermé,
  `-Didle.partVide=0.25` impose une part à tous.

Ce que les bots ne jouent toujours pas dans une partie : la graine et l'onglet Univers (améliorations, recherches,
épreuves, skins, chrono). Les dimensions se jouent à part, par `DimensionBots` (0.136, voir plus bas), sans que leurs
éclats servent à la graine. L'univers suivant se lance donc sans les paliers 2 et 3 de la graine qu'un joueur doit
atteindre. (Depuis la 0.152, la graine et l'arbre se jouent à partir de l'univers 4 : voir « Les univers 4 à 11 ».)

Mesures, départ `checkpoint_universe2_speedrunner.save`, une décision toutes les 20 s, univers 3 compté depuis son
lancement :

| Bot | Premier Big Bang | 8e Big Bang | Galaxie | Univers 3 formé | Vide |
|---|---|---|---|---|---|
| 1 Speedrunner (part 50 %) | 4 h 01 | 10 h 47 | 12 h 37 | **14 h 22** | 48 achats, 2 sacrifices |
| 1 Speedrunner, sans le vide | 4 h 01 | 10 h 53 | 14 h 38 | 18 h 39 | — |
| 3 Regular (part 25 %) | 5 h 07 | 14 h 49 | 17 h 08 | **20 h 49** | 38 achats, 1 sacrifice |
| Bot V2 `DEPENDANCES` | | 15 h 46 | 20 h 40 | 22 h 21 | |
| 12 Hoarder (jamais de sacrifice) | 16 h 46 | | | 66 h 16 | |
| 16 Beginner | 16 h 15 | 59 h 21 | 64 h 24 | 69 h 36 | 47 achats, 1 sacrifice |

Le vide fait gagner 4 h 17 au Speedrunner sur son troisième univers (14 h 22 au lieu de 18 h 39) : la galaxie vient 2 h
plus tôt (maturation plus courte), l'amas et l'univers 1 h chacun plus tôt (rassemblements plus courts). Pour le
Speedrunner, les trois univers prennent 27 h 01, 21 h 12 et 14 h 22.

## Chaque profil à sa façon dès le premier univers, et l'audit des trois univers (version 0.135)

**Les choix des trois actes.** Dès le premier univers, chaque profil fait ses propres choix
(`IdleProfiles24.EARLY_STYLES`, table dans `PROFILS_24.md`) : l'ordre d'achat des atomes (celui du modèle, ou le
moins cher), l'arbre de matière noire (au mieux, ou la case la moins chère), les défis de Big Bang (tentés ou non)
et la durée des voyages des sondes (jusqu'à son retour, ou les sauts courts). Le Speedrunner joue en expert, comme
avant : son premier univers reste **27 h 01**. Le joueur « le moins cher » achète d'abord les automatismes, et garde
ses atomes pour eux tant qu'il ne peut pas les payer. Sans cela, le distrait n'avait aucun Big Bang en 168 h. Il garde
aussi la matière noire du Big Bang, sans dépasser 5 % de sa matière noire par niveau répétable.

**Les défis de Big Bang à partir du deuxième univers.** Avant, un bot ne commençait pas de défi de Big Bang sur une
partie prête à Big Bang. Or dans les univers suivants, le ciel se réunit en même temps que le reste : le Speedrunner
n'en faisait aucun, ou un seul. Pourtant, chaque défi réussi y rend la maturation quatre fois plus rapide et double
les particules et les molécules. Les bots les jouent maintenant aussi quand le Big Bang est prêt, en commençant par
les deux qui ne remettent rien à zéro : le ciel figé, gratuit à ce moment-là, puis la barre haute (maturation ×2).
`-Didle.defisBB=avant` remet la règle d'avant, `gele` met seulement le ciel figé en tête.

Mesuré sur cinq parties (Speedrunner et Régulier), l'univers 2 passe de 20 h 33–21 h 15 à 14 h 57–19 h 23. L'univers
3 passe de 12 h 08–20 h 18 à 10 h 40–12 h 31. Après le huitième Big Bang, les sept derniers astres mûrissaient en
quatre heures ; ils mûrissent maintenant en dix minutes.

**Hors défaut, à décider.** La même règle dans le premier univers (`-Didle.defisBBPremier=true`) fait passer le
Speedrunner de 27 h 01 à 24 h 44, et le Régulier de 34 h 26 à 31 h 59. Ce n'est pas le défaut, parce que le premier
univers du Speedrunner sert de référence à l'équilibrage.

**Réglages mesurés** (plusieurs graines, `reglages` dans l'historique de la 0.135) :

- **Cycle de l'expert : la réserve**, au lieu de la fusion. Elle gagne les quatre paires de parties, avant comme après
  les défis de Big Bang. Le Vétéran, le Régulier et les joueurs à sessions appliqués la prennent aussi.
- **Croissance du vide** (`VoidPolicy.GROWTH_SHARE`) : 0,5 au lieu de 0,25. Sur quatre parties de l'univers 3 :
  49,9 h en tout avec 0,25, 47,2 h avec 0,5, 47,4 h avec 1.
- **Trou noir** : un sacrifice fait gagner 8 à 20 minutes. Avec une borne à 15 ou sans borne, les parties sont les
  mêmes qu'à 10.

**Corrections trouvées par l'audit et par une relecture indépendante :**

- Le collectionneur et le cliqueur au hasard remplissaient l'espace de molécules hors liste, et bloquaient au premier
  astre.
- Avant le huitième Big Bang, le bot visait la naine blanche même quand sa géante rouge manquait. La liste restait
  alors sur une nébuleuse verrouillée : le débutant et le vétéran revenant restaient bloqués à 25 astres. Le bot vise
  maintenant d'abord les astres plus petits qui manquent.
- Le garde-fou ne compte plus la matière rassemblée seule comme un progrès.
- Le choix du voyage des sondes prend la durée réelle du voyage, moteurs compris.
- Le joueur sans ordre d'ouverture n'achète plus Persistance à part. Le succès « compte juste » se prend dans les
  deux styles.

**L'audit des 24 profils sur les trois univers** : parties neuves, graine 200 + id, une décision toutes les 20 s, au
plus 168 h réelles par univers. Chaque univers part de la fin du précédent quand le profil l'a gagné ; sinon, il part
de la sauvegarde de référence (¹). Cette mesure a été faite univers par univers (`run.py profils --checkpoint …`
pour les reprises) : ce n'est pas ce que fait `FreshUniverseAudit --univers 3` (ou l'action GitHub), dont l'horizon est
global à l'essai, sans repli sur une sauvegarde quand un univers manque. La colonne de gauche de chaque univers est
la 0.133.

| Profil | Univers 1 (0.133 → 0.135) | Univers 2 (0.133 → 0.135) | Univers 3 (0.133 → 0.135) |
|---|---|---|---|
| 1 Speedrunner | 27 h 01 | 21 h 05 → **15 h 29** | 11 h 17 → **10 h 37** |
| 2 Veteran | 31 h 02 | 21 h 11 → **15 h 44** | 11 h 55 → **11 h 01** |
| 3 Regular | 34 h 26 | 25 h 10 → **17 h 56** | 13 h 51 → **12 h 34** |
| 4 Methodical | 39 h 46 | 32 h 46 → **25 h 06** | 24 h 56 → **17 h 58** |
| 5 AchievementHunter | 34 h 02 | 23 h 26 → **18 h 48** | 19 h 42 → **12 h 28** |
| 6 Collector | pas en 168 h (168 h en ligne) → **81 h 22** | pas en 168 h (168 h en ligne) → **20 h 38** | pas en 168 h (168 h en ligne) → **16 h 36** |
| 7 Chemist | 33 h 02 | 22 h 59 → **17 h 34** | 15 h 49 → **11 h 03** |
| 8 Astronomer | 34 h 29 | 23 h 15 → **18 h 18** | 16 h 10 → **12 h 38** |
| 9 Explorer | 33 h 34 | 22 h 44 → **17 h 14** | 16 h 07 → **10 h 58** |
| 10 PassiveOptimist | 51 h 24 → **53 h 22** | 32 h 08 → **39 h 16** | 27 h 16 → **36 h 02** |
| 11 PrestigeAddict | 33 h 52 | 22 h 46 → **17 h 12** | 15 h 24 → **10 h 36** |
| 12 Hoarder | 79 h 26 | 59 h 56 → **62 h 08** | 56 h 16 → **50 h 28** |
| 13 BulkBuyer | 66 h 35 → **75 h 25** | 53 h 10 → **49 h 35** | 41 h 25 → **37 h 10** |
| 14 OneClick | 45 h 32 → **48 h 15** | 29 h 01 → **45 h 46** | 26 h 00 → **29 h 12** |
| 15 RandomClicker | pas en 168 h (168 h en ligne) → **97 h 12** | pas en 168 h (168 h en ligne) → **112 h 00** | pas en 168 h (168 h en ligne) → **98 h 38** |
| 16 Beginner | 101 h 57 → **121 h 06** | 87 h 03 → **89 h 48** | 68 h 36 → **82 h 21** |
| 17 AFKOpen | pas en 168 h (168 h en ligne) | pas en 168 h (168 h en ligne) | pas en 168 h (168 h en ligne) |
| 18 BackgroundWorker | 122 h 10 | 74 h 00 → **73 h 50** | 68 h 20 → **64 h 30** |
| 19 MobileSnacker | pas en 168 h (14 h en ligne) | 112 h 01 (9.4 h en ligne) ¹ → **91 h 01 (7.6 h en ligne) ¹** | 91 h 00 (7.6 h en ligne) → **73 h 00 (6.1 h en ligne)** |
| 20 LunchBreak | 148 h 00 (18.5 h en ligne) | 84 h 00 (10.5 h en ligne) → **72 h 00 (9.0 h en ligne)** | 64 h 00 (8.0 h en ligne) → **56 h 24 (7.4 h en ligne)** |
| 21 EveningPlayer | pas en 168 h (14 h en ligne) | pas en 168 h (14 h en ligne) → **144 h 00 (12.0 h en ligne) ¹** | 120 h 24 (10.4 h en ligne) ¹ → **96 h 24 (8.4 h en ligne)** |
| 22 WeekendPlayer | pas en 168 h (16 h en ligne) | pas en 168 h (16 h en ligne) → **144 h 00 (12.0 h en ligne) ¹** | 144 h 00 (12.0 h en ligne) ¹ → **97 h 41 (9.7 h en ligne)** |
| 23 Forgetful | pas en 168 h (56 h en ligne) | pas en 168 h (56 h en ligne) | pas en 168 h (56 h en ligne) |
| 24 ReturningVeteran | pas en 168 h (14 h en ligne) | pas en 168 h (14 h en ligne) → **108 h 00 (9.0 h en ligne) ¹** | 99 h 00 (8.3 h en ligne) ¹ → **78 h 00 (6.5 h en ligne)** |

Ce que montre l'audit :

- **Les joueurs appliqués** gagnent 4 h 30 à 7 h 40 dans l'univers 2, et jusqu'à 7 h dans l'univers 3. Le
  Speedrunner fait ses trois univers en 53 h 07 au lieu de 59 h 23.
- **Les joueurs « le moins cher »** (optimiste passif, acheteur en lots, cliqueur occasionnel, débutant) sont le plus
  souvent plus lents qu'avant, jusqu'à 19 h, et parfois plus rapides (l'acheteur en lots dans les univers 2 et 3).
  C'est leur façon de jouer, plus la variance de leurs choix au hasard : une seule graine par profil ne mesure pas un
  écart de quelques heures. Trois d'entre eux ne font pas de défis de Big Bang, donc ne gagnent rien de la nouvelle
  règle.
- **Toujours au plafond** : l'AFK (une visite par heure) n'avance presque pas. L'ouverture demande une trentaine de
  fusions à la main avant les automatismes, et il en fait une par visite ; chaque explosion efface ensuite ses
  automatismes. Le distrait (20 minutes par heure, une occasion sur deux manquée) reste au premier univers, avec un
  Big Bang en 168 h ; il en faisait trois en 0.133, quand il suivait l'ordre d'achat du modèle (il l'oublie
  maintenant). Partis d'une sauvegarde, il atteint 4 Big Bangs dans l'univers 2 et 7 dans l'univers 3. C'est un constat d'équilibrage : le jeu ne laisse presque rien à ces rythmes-là.
- Les joueurs intermittents (mobile, soir, week-end, vétéran revenant) jouent 14 à 16 h en 168 h. Ils restent au
  plafond du premier univers, mais finissent les univers 2 et 3 quand ils partent d'une sauvegarde.

## Les univers 4 à 11 (version 0.152)

Les bots vont maintenant jusqu'à **onze univers** (`--univers 1` à `11`, le 7e argument de `FreshUniverseAudit`,
l'entrée « univers » de l'action GitHub ; 3 au plus avant). Ce que les univers 4 et suivants demandent est dans
`UniverseTreePolicy`, appelée à chaque décision par `RobotCosmosPlanner` ; rien ne s'y passe avant trois univers formés,
aucun hasard n'y est tiré, et les univers 1 à 3 restent exactement ceux d'avant (vérifié : Speedrunner 26,8889, 42,3111
et 52,4056 h ; Regular 34,1056, 52,1889 et 66,0222 h, comme sans elle). Le contenu du jeu : quatre dimensions jouables,
les dimensions 5 à 10 s'ouvrent sans rien à jouer, ce qui ne gêne rien (la graine ne dépend plus des dimensions depuis
la 0.139).

- **La graine** : l'arrosoir (gratuit) est débloqué et tenu penché jusqu'au palier qui ouvre l'univers suivant (le
  palier N ouvre l'univers N), puis repris. Dès l'univers 4, l'univers suivant ne se lance qu'avec ce palier, comme
  dans l'interface (les univers 2 et 3 se lancent toujours sans attendre, pour garder les parties de référence). Le
  seul arrêt mesuré : 100 s (90 arrosages, plus celui qui fait germer) entre l'univers 3 formé et le lancement du 4 ;
  ensuite la graine est toujours prête.
- **Les bulles**, payées avec l'énergie du vide accumulée : l'expert suit une liste (`UniverseTreePolicy.BEST_ORDER`) et
  économise pour la suivante ; d'abord la base et ×2 sur l'énergie du vide, puis le chemin vers l'arbre mûr à droite
  (garder matière noire, arbre, explosions et défis) et l'arbre ancien à droite (plus de maturation), le reste ensuite,
  le cœur et le sommet de l'arbre-monde quand la graine les fait naître. Les joueurs moins appliqués prennent la moins
  chère. La bulle des racines (« à venir ») n'est pas achetée, ni **celle du bonsaï, à droite** (automatismes sans délai) :
  achetée, chaque heure de jeu coûte plus de dix minutes de calcul au lieu d'une demi-minute (la synthèse automatique
  fait cent synthèses par pas, et chacune recalcule les bonus des éléments) ; le réseau s'en passe
  (`-Didle.bulleSansDelai=true` la remet).
- **Les hadrons** : l'onglet ouvert dès qu'il est payable (le collisionneur se garde pour lui, deux heures au plus) ;
  les quarks en trop sont liés, la sorte la moins fournie d'abord, sans toucher à ceux que demandent les prochaines
  améliorations du collisionneur.
- **La ligne** (elle tourne seule) : ses améliorations au meilleur rapport, le débit de l'arrosoir, et les
  automatismes des dimensions dès la bulle du sommet (ceux de la ligne en graines d'univers, puis ceux du plan en
  segments). Le plan, le volume et la durée se jouent au dixième de seconde : pas avec un pas de 20 s (voir
  `DimensionBots`) ; leurs éclats ne servent pas à former un univers. Le chrono (1,5 million d'instants) n'est pas acheté.

Mesures, parties neuves, une décision toutes les 20 s (`FreshUniverseAudit 1 1 2000 20 … false 11`, et `3 3`) :

| Univers | Speedrunner (graine 201) formé à | durée | Regular (graine 203) formé à | durée |
|---:|---|---|---|---|
| 1 | 26 h 53 | 26 h 53 | 34 h 06 | 34 h 06 |
| 2 | 42 h 19 | 15 h 25 | 52 h 11 | 18 h 05 |
| 3 | 52 h 24 | 10 h 06 | 66 h 01 | 13 h 50 |
| 4 | 61 h 49 | 9 h 25 | 75 h 54 | 9 h 53 |
| 5 | 71 h 53 | 10 h 04 | 89 h 47 | 13 h 53 |
| 6 | 77 h 53 | 6 h 00 | 97 h 53 | 8 h 06 |
| 7 | 78 h 55 | 1 h 02 | 99 h 12 | 1 h 19 |
| 8 | 79 h 52 | 0 h 57 | 100 h 24 | 1 h 12 |
| 9 | 80 h 50 | 0 h 57 | 101 h 37 | 1 h 13 |
| 10 | 81 h 47 | 0 h 58 | 102 h 50 | 1 h 13 |
| 11 | **82 h 45** | 0 h 57 | **104 h 08** | 1 h 18 |

(La durée va d'une formation à la suivante : celle du 4 compte les 100 s d'arrosage avant son lancement.) Où passe le temps : les
univers 4 et 5 rejouent les trois actes presque comme le 3 (premier Big Bang en 2 h 10 à 4 h 20, huitième en 6 h 15 à
12 h 50) ; ensuite la galaxie attend les trous noirs, qui demandent de l'espace (de 1e14 à 2e16) : 3 h et 4 h 20 pour le
Speedrunner dans les univers 5 et 6, 5 h 47 pour le Regular dans le 6. Les bulles
achetées au début de l'univers 6 (arbre mûr et ancien à droite, géant de gauche en bas : le vide et ses allègements
traversent les univers) ramènent les univers 7 à 11 à moins d'une heure : huit Big Bangs en 8 minutes, puis deux fois
24 minutes de stabilisation (amas, univers), le plancher que laisse l'amélioration de rassemblement du vide.
Le Regular suit l'une de ses deux trajectoires connues du premier univers (34,1056 h ; l'autre, 34,5056 h, s'est aussi
vue pendant les essais).

## Un bot par dimension (version 0.136)

Les dimensions se jouent chacune à part : leurs éclats ne servent qu'à elles, et rien d'autre dans la partie ne les
lit. `DimensionBots` a donc un bot par dimension, sans lien avec les autres bots. Chacun part de la dimension qui
vient de s'ouvrir (`checkpoint_universe1_speedrunner.save`, partie secondaire remise à zéro, univers ajoutés par
l'outil d'essai). Il joue comme un joueur appliqué, et mesure le temps jusqu'au rond vert :

- **la ligne** tourne seule ; le joueur passe acheter toutes les 5 s ;
- **le plan** : il file droit vers l'éclat le plus proche, et repart 0,25 s après chaque éclat ramassé ;
- **le volume** : il vise chaque boule en 0,3 s + 0,012 / rayon (0,78 s au départ), replonge 1 s après la fin d'une
  plongée et achète entre deux ;
- **la durée** : il tient jusqu'au centre, relâche, et rappuie 0,3 s après.

Il achète au meilleur rapport : le gain d'éclats par seconde d'un niveau, divisé par son prix, d'après un modèle simple
de chaque dimension. Le modèle choisit ; c'est la partie réelle qui mesure.

```bash
python run.py dimensions              # les quatre ; ou : python run.py dimensions plan --minutes 30
java -Didle.achatDimension=moinsCher -Didle.lenteurDimension=2 -cp … DimensionBots toutes 60
```

`-Didle.achatDimension=moinsCher` achète le moins cher. `-Didle.lenteurDimension=2` rend le joueur deux fois plus lent
à réagir, viser et revenir. `-Didle.objectifDimension=infini` joue jusqu'au temps donné et écrit les éclats gagnés
minute par minute : c'est ce qui a servi à placer les seuils.

**Avant la 0.136**, le rond vert venait en 2 min 58 (ligne), 1 min 33 (plan), 55 s (volume) et 31 s (durée), avec
100 éclats pour chacune. Les seuils sont maintenant ce qu'un joueur appliqué gagne en un quart d'heure : 250 000
éclats de la ligne, 3 500 du plan, 8 000 du volume et 15 millions de la durée. La durée a aussi des améliorations
plus chères : ×3,4, ×4, ×4 et ×3,2 par niveau, au lieu de ×2,4, ×3, ×3 et ×2,2. Sinon ses trois améliorations
plafonnées étaient au plus haut en trois minutes, et le reste du quart d'heure ne se jouait qu'à la valeur.

| Dimension | Appliqué | Le moins cher | Deux fois plus lent | Les deux |
|---|---|---|---|---|
| Ligne | 14 min 29 | 14 min 46 | 14 min 52 | 15 min 07 |
| Plan | 14 min 36 | 17 min 04 | 14 min 39 | 17 min 05 |
| Volume | 14 min 35 | 16 min 21 | 24 min 59 | 29 min 50 |
| Durée | 14 min 19 | 15 min 13 | 14 min 31 | 15 min 25 |

Le volume dépend le plus du joueur : on y vise chaque boule, et un joueur deux fois plus lent y met 25 minutes. La
ligne avance seule, même en son absence ; elle ne dépend presque que du temps. `DimensionBotsTest` garde chaque
dimension entre 12 et 18 minutes pour le joueur appliqué.

**Dans le profil de test (version 0.140)**, le même joueur appliqué (`DimensionBots.Player`, qui décide avant chaque
pas du jeu) joue la partie en cours : groupe « Bots dimension », à côté de « Dimensions ». On choisit la dimension, puis
« En direct » (il joue à chaque image : ouvrir la fenêtre de la dimension pour le voir), « +1 min » ou « +5 min avec le
bot », ou « Jusqu'au rond vert (30 min au plus) », en avance rapide annulable. Les mesures passent par le même
joueur, et donnent exactement les mêmes temps et les mêmes achats qu'avant.

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
- `winner.save` (et, avant la 0.141, `checkpoint_universe_finished.save` du `main` de `RobotCosmosPlanner`, retiré) sont des parties remises à zéro, dans l'univers ;
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
