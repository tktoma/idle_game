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
- Depuis la 0.182, les bots mettent le chrono à fond dès son étage acheté (`UniverseTreePolicy.line`) ; les automatismes des dimensions s'ouvrent par les paliers de la graine et les univers (`isDimensionAutomationOpen`), la bulle du sommet ne fait plus que les rendre moitié prix : le bot les achète toujours dès que `canBuyDimensionAutomation` le permet.
- Depuis la 0.181, le premier univers demande moins à ses Big Bangs (10⁸⁵ ⁰⁰⁰ particules et 42 000 matières noires pour le premier, 10³⁰ ⁰⁰⁰ particules et 10¹¹ atomes pour les suivants, élan ×1,6 après le premier Big Bang) : rien à régler dans les bots, les politiques lisent `bigBangParticles()`, `bigBangAtoms()` et `bigBangDarkMatter()`. Mesuré en rejouant l'acte 2 du Speedrunner (graine 201) depuis sa première explosion : premier Big Bang à 7:55:45 au lieu de 9:30:15, dernier Big Bang préparé à 13:36:50 au lieu de 16:22:05.
- Depuis la 0.179, le Speedrunner tient l'arrosoir dès l'univers 1 formé (les paliers de la graine demandent des heures) et joue une dimension à la main avant chaque pas du jeu (`UniverseTreePolicy.tick`) : le plan, le volume ou la durée, celle dont les éclats manquent le plus à la prochaine amélioration de la graine, avec le joueur appliqué de `DimensionBots`. Le Régulier, qui ne tient pas la touche M, ne les joue pas.
- Depuis la 0.178, la création automatique du jeu suit toujours la liste de courses, puis la matière et la variété qui manquent, puis les amas confiés : `SANS_LISTE` la laisse coupée, et `RobotCosmosPlanner.automateMatter` confie en plus l'amas qu'il juge le meilleur.
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
| `idle.sansSobriete` | `RobotCosmosPlanner` | `true` : la boucle des particules, des atomes et des éléments ne s'éteint jamais, comme avant la 0.202 : la partie d'avant au bit près (la sobriété décale le hasard de la synthèse pour les univers suivants), le calcul 10 à 15 % plus long. | `false` |
| `idle.appuiM` | `IdleProfiles24` | L'écart maximal entre deux décisions, en secondes, pour qu'un profil à l'attention entière tienne la touche M. `5` : le Speedrunner et le Vétéran des audits ne la tiennent plus, comme avant la 0.206. | `20` |
| `idle.pasM` | `IdleProfiles24` | Le morceau de pas, en secondes, des profils qui tiennent M en décidant à plus de 5 s d'écart (un « tout acheter » après chaque morceau). | `2` |
| `idle.sansLacherM` | `IdleProfiles24` | `true` : M reste tenue après l'amas formé, comme avant la 0.206. | `false` |
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
| `idle.cheminBulles` | `UniverseTreePolicy` | `0173` : l'expert achète ses bulles dans l'ordre de la 0.173 (le jeune arbre à droite et la couronne du milieu tôt, l'arbre mûr à gauche et le sommet tard). | l'ordre de la 0.174 |
| `idle.attenteBulles` | `UniverseTreePolicy` | `false` : l'expert achète la première bulle de sa liste qu'il peut payer au lieu d'attendre la suivante. | `true` |
| `idle.collisionneur0173` | `RobotCosmosPlanner` | `true` : le collisionneur s'achète dans l'ordre de la 0.173 (générateurs du bas vers le haut). | `false` |
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

## Le bot analyste (version 0.161)

`BotAnalyste` joue comme le Speedrunner, mais à vitesse humaine (une décision toutes les 5 s, pas de 5 s), et note tout à la seconde de jeu près. Un double-clic sur `analyste.bat` (ou `./analyste.sh`, ou `./gradlew :bots:analyste`) lance onze univers. Options : `-Punivers=1 -Pdossier=sorties/essai -Pheures=60 -Pgraine=201 -Ppas=5 -Pestimation=30`.

Ce qu'il écrit dans le dossier (`sorties/analyste` par défaut) :

| Fichier | Contenu |
|---|---|
| `journal.txt` | Tout, ligne par ligne, avec le chrono (hh:mm:ss de jeu) : achats, choses obtenues, succès, défis, automatismes allumés ou coupés, explosions, Big Bangs, paliers, estimations, blocage. |
| `achats.csv` | Chaque achat : quoi, combien de niveaux, payé en quoi, combien, la part de la réserve, son poids sur chaque grandeur et le gain de temps estimé. |
| `paliers.csv` | Chaque palier : quand, depuis le précédent, le moment que l'estimation annonçait et l'écart. |
| `estimations.csv` | Chaque estimation : le palier visé, le temps estimé, le goulot. |
| `rapport.html` | La page : résultat, frise, résumé par étape, paliers, courbes des grandeurs, estimations, achats regroupés et les 200 plus lourds, journal filtrable. |
| `sauvegardes/` | Une sauvegarde à chaque palier, pour reprendre une partie à ce moment. |

**Comment il voit ce qu'il fait.** Il prend deux photos de la partie, juste avant et juste après les décisions d'un pas, sans que le temps passe entre elles. Ce qui monte est acheté ou obtenu, ce qui baisse est payé. Quand plusieurs achats tombent dans la même seconde (un lot), chaque monnaie se répartit au prorata de leur prix estimé, et le poids sur les grandeurs est celui du lot.

**Comment il estime.** Toutes les 30 minutes de jeu et à chaque palier, il joue la suite sur une copie de la partie, avec la mémoire du bot, jusqu'au palier suivant. Sans cette mémoire, une copie prise à 2 h n'explosait plus. Sur le premier univers, les annonces tombent à quelques minutes près.

**Comparer les façons de jouer.** Avec `-Pcomparer=true`, à chaque palier principal, six façons de jouer jouent chacune la suite sur une copie jusqu'au palier suivant : celles du Speedrunner, du Méthodique, du Chimiste, de l'Astronome, de l'Explorateur et du Prestige, toutes sans faute et à vitesse humaine. Le bot garde la plus rapide, et le journal (et `choix.csv`) donne le classement avec ce que chaque autre aurait coûté. Mesuré à la 0.161 entre le premier et le dernier Big Bang du premier univers, elles finissent toutes entre 6 h 50 et 6 h 55, le Speedrunner devant. C'est six parties d'essai par palier, donc bien plus long : l'option est coupée par défaut.

**Ce qui n'est pas un achat.** Seules les clés d'achat comptent comme achats : améliorations, automatismes, arbre, générateurs, bulles, dimensions, graine. Les compteurs d'actions (fusions, synthèses, impulsions, molécules) restent dans les grandeurs. Les réglages (part du vide, réserve des molécules, choix de cycle…) se notent comme des choix, et les éléments synthétisés comme des choses obtenues.

**Quand il s'arrête.** Il s'arrête si l'estimation dépasse 24 h, si plus rien ne bouge pendant 30 minutes (ni palier, ni achat, ni compteur, ni grandeur qui monte) ou si le garde-fou des profils juge la partie bloquée. Il note alors le diagnostic : le palier visé, le goulot (conditions du Big Bang manquantes, échelle du cosmos, grandeur qui fait avancer) et les grandeurs du moment.

## L'énergie du vide gardée jusqu'à la formation (version 0.177)

`VoidPolicy.formingSoon` : quand l'énergie du vide se garde pour les bulles (`savingForBubbles`) et que l'univers se forme dans moins de `SAVING_LAST_SECONDS` (15 minutes, l'amas formé et le délai de l'univers en cours), le bot n'achète plus de raccourci du vide. Un niveau d'assise acheté là ne raccourcit que ce qui reste du délai, une minute ou deux, et prend l'énergie qui paie les bulles à la formation.

Trouvé en mesurant la 0.177 : le Régulier achetait l'assise (260 860 d'énergie du vide) à 154:51, une minute avant la formation de son univers 7, et n'achetait alors aucune bulle ; la galaxie de l'univers 8 attendait 6 h 17. Le même achat de dernière minute se voit dans les mesures 0.174 et 0.176 (assise à 146:26 et 146:45 chez le Régulier).

| Moment | Étape (Régulier 0.177, repris de son univers 4 à 126:15 ; entre parenthèses, sans la règle) |
|---|---|
| 136:12 | Univers 5 formé (136:12) |
| 146:15 | Univers 6 formé (146:15) : neuf bulles d'un coup, de l'arbre mûr à l'arbre-monde (une seule) |
| 146:44 | Galaxie de l'univers 7 (152:16) |
| 149:08 | Univers 7 formé (154:52) |
| 151:49 | Univers 8 formé (163:46) : 11 h 57 plus tôt |

Le Speedrunner ne fait jamais cet achat de dernière minute entre ses univers 4 et 8 : sa partie est identique à la seconde jusqu'à la galaxie de l'univers 8 (78:37).

## Le collisionneur, le vide et les bulles repensés (version 0.174)

Un audit du bot, fait sur les sources, les journaux d'achats et les sauvegardes du Speedrunner 0.171 (graine 201), a cherché où partaient les 13 h 58 d'attente de la galaxie aux univers 4 à 7. Réponse : la galaxie attend les 10¹⁵ d'énergie produite du collisionneur, et le bot gérait mal ce collisionneur. Tout ce qui suit vaut dès le deuxième univers ; le premier ne change pas.

- **Le collisionneur s'achète par valeur** (`RobotCosmosPlanner.colliderByValue`) : la production d'énergie et les générateurs renforcés d'abord (ils doublent tout), puis les générateurs du haut vers le bas. Avant, le premier achetable de la liste : 384 faisceaux croisés pour 42 collisionneurs de Planck. Depuis zéro, à l'univers 3, l'énergie produite en 2 h passe de 1,00 × 10¹⁰ à 1,27 × 10¹⁰ (+27 %) ; `CollisionneurParValeurTest` le garde.
- **La réserve pour le vide et les hadrons n'attend qu'un prix proche** (`VoidPolicy.nearPrice`) : moins de 10 min de production. Avant, les achats du collisionneur gelaient jusqu'à 2 h pour les 10⁹ d'ouverture, et le gel finissait sans les avoir payés (univers 5 à 7).
- **La réserve de quarks ne garde que pour une amélioration à portée** : avant, elle gardait 7 680 quarks u pour un seul niveau, et les protons et les neutrons restaient figés.
- **Le quark top se joue dès qu'il arrive** : avant, il expirait sans servir.
- **Les défis de Big Bang** : une fois la maturation supprimée par l'arbre ancien à droite, la barre haute ne passe plus en premier.
- **Les recherches de l'onglet Skins** (noyau dense, prisme) s'achètent dès qu'elles se paient.
- **Les bulles de l'expert suivent un nouvel ordre** (`UniverseTreePolicy.BEST_ORDER`), tiré du coût et de l'utilité mesurés : l'arbre mûr à gauche (le collisionneur traverse les univers) arrive tôt, par l'arbuste à gauche ; le jeune arbre à droite (plafond de générateurs que personne n'atteint) et l'arbre géant tout à gauche (5 % du vide vers les bulles, quand il n'en reste plus à acheter) passent en dernier.

Trois variantes mesurées, depuis la formation de l'univers 3, avec les propriétés du tableau plus haut :

| Univers formé | Régulier 0.171 | Vide fermé pendant l'attente (référence des variantes) | Ancien ordre (`idle.cheminBulles=0173`) | Sans attente (`idle.attenteBulles=false`) | Bot livré |
|---|---|---|---|---|---|
| 4 | 127:09 | 126:24 | 126:24 | 126:24 | 126:15 |
| 6 | 150:00 | 148:13 | 146:47 | 148:13 | 146:41 |
| 8 | 172:34 | 154:04 | 163:11 | 154:04 | 152:18 |

Les deux variantes ont tourné avec le vide fermé pendant l'attente du collisionneur (voir plus bas) : elles se comparent à la troisième colonne. L'ancien ordre perd 9 h 07, parce qu'il ne prend jamais l'arbre mûr à gauche (8 bulles à l'univers 8, contre 15). Sans attente, la partie est identique à la seconde : le bot ne prend que deux bulles de plus, qui ne changent rien.

Essayé puis écarté : fermer le vide tant que la galaxie attend le collisionneur. La galaxie n'arrivait pas plus tôt, et le vide, moins acheté, allongeait le rassemblement. L'univers 8 se formait 35 min plus tard chez le Speedrunner et 1 h 46 plus tard chez le Régulier.

Le Speedrunner (graine 201) avec le bot livré, en heure de formation (entre parenthèses, la 0.171) : univers 1 à 25:59 (25:59), 2 à 40:28 (40:38), 3 à 51:32 (52:12), 4 à 59:34 (60:59), 5 à 67:05 (70:02), 6 à 74:04 (78:04), 7 à 80:57 (85:57), 8 à 83:24 (88:41), soit 5 h 17 plus tôt. Les univers 4 à 8 sont repris de la formation de l'univers 3 de la 0.171, comme les variantes ; la galaxie y attend 4 h 53 à 5 h 07 au lieu de 5 h 53 à 7 h 03. Le Régulier forme son univers 8 à 152:18 au lieu de 172:34 (mesuré avant la correction de la puissance des succès : ses heures sont un peu pessimistes).

Ce que le bot oublie encore, laissé pour plus tard parce que ce n'est pas sur le chemin du Speedrunner : le seuil de fusion jamais réglé, les épreuves jamais tentées, les dimensions 2 à 10 jamais jouées à la main avant leurs automatismes, la densité plafonnée par le nombre d'achats d'une décision, deux planètes par système au plus.

## Le Speedrunner tient la touche M, et la lâche à l'amas (version 0.206)

Demande : que le bot appuie sur M passivement, et s'arrête au bon moment. Avant, seuls les profils qui décident au moins toutes les 5 s tenaient M ; le Speedrunner des audits (20 s) ne la tenait pas, et son univers 1 prenait 35 h au lieu de 25 (voir la 0.205).

**1. Tenir une touche ne demande pas de décider vite.** La limite passe de 5 à 20 s (`idle.appuiM`) : le Speedrunner et le Vétéran, attention entière, la tiennent. Les 22 autres profils ne changent pas, ni le bot analyste.

**2. Passivement : un « tout acheter » toutes les 2 s, pas toutes les demi-secondes.** Tenir M coupe le pas de jeu en morceaux, et c'est ce découpage qui coûte du calcul. Essayé sur l'univers 1 (graine 201) :

| Morceau de pas | Univers 1 formé | Calcul |
|---|---|---|
| sans M | 35 h 26 | 80 s |
| 0,5 s (celui du bot analyste) | 25 h 05 | 107 s |
| 1 s | 26 h 27 | 84 s |
| 1,5 s | 24 h 48 | 87 s |
| **2 s (retenu)** | **24 h 58** | **79 s** |
| 3 s | 26 h 42 | 89 s |
| 5 s | 28 h 42 | 86 s |
| 10 s | 33 h 10 | 79 s |

Jusqu'à 2 s, l'heure d'arrivée ne bouge pas plus que le hasard (une heure et demie d'un essai à l'autre) ; au-delà, elle recule. À 2 s, l'univers 1 se forme 10 h 28 plus tôt pour le même temps de calcul que sans M. Les temps de calcul sont à dix secondes près. Les profils qui décident au moins toutes les 5 s gardent leurs morceaux de 0,5 s : leurs parties sont celles d'avant.

Vérifié aussi : couper le pas sans appuyer plus souvent ne sert à rien (35 h 32). C'est bien l'appui répété qui fait gagner les 10 h, pas la finesse du pas.

**3. Le bon moment pour lâcher : l'amas formé.** Mesuré avec `AnalyseArrets` (M lâchée à chaque jalon, deux hasards) :

| M lâchée à partir de | Retard de l'univers |
|---|---|
| début, explosion 1 | plafond (plus de 30 min) |
| Big Bang 1 | −1 min / +31 min 40 |
| Big Bang 5 | −17 min / +25 min |
| Big Bang 8 | +18 min 20 / +17 min 40 |
| galaxie | +9 min 40 / +11 min 40 |
| **amas** | **+1 min 20 / +1 min** |

M sert donc presque jusqu'au bout, bien après que les automatismes et les améliorations ont cessé de compter : le bot la lâche une fois l'amas formé (`idle.sansLacherM=true` la garde tenue). Avec des morceaux de 0,5 s, la lâcher à l'amas fait passer le calcul de 107 à 102 s pour 1 min 20 de jeu ; avec des morceaux de 2 s, le gain de calcul est dans le bruit.

**Ce que ça change pour les mesures.** La partie de référence du Speedrunner n'est plus la même : univers 1 à 24 h 58 au lieu de 35 h 26. `-Didle.appuiM=5` rend l'ancienne. Les tableaux des versions 0.204 et 0.205 ont été mesurés sans M ; avec M tenue, les jalons où un automatisme ou une amélioration cesse de servir sont à remesurer (`./gradlew :bots:arrets`), et le plan de `ChargeArrets` avec eux. Seul l'univers 1 est mesuré : la règle « lâcher à l'amas » s'applique aussi aux univers suivants sans y avoir été vérifiée.

## Ce que pèse ce qui ne sert plus, et pourquoi l'univers 1 dure 35 h (version 0.205)

Deux questions après la 0.204 : combien de calcul gagne une simulation qui coupe ce que la mesure dit inutile, et pourquoi l'univers 1 du Speedrunner se forme à 35 h 25 alors que d'anciennes mesures donnaient 22 à 26 h.

### 1. La charge : 40 % de calcul en moins

`ChargeArrets` (`./gradlew :bots:charge`, ou `-Pplan=automatismes`, `ameliorations`, `mecaniques`) joue l'univers 1 deux fois, à la suite, sur le même hasard : le témoin, puis la même partie où chaque chose est coupée au jalon où le tableau de la 0.204 dit qu'elle ne sert plus. Une partie est jouée avant, pour rien, afin que la JVM soit chaude. La sobriété de la 0.202 est allumée dans les deux parties : le gain vient en plus du sien.

Tout le plan (14 automatismes, les 12 améliorations, les défis et le collisionneur) :

| Tronçon | Calcul du témoin | Calcul coupé | Gain |
|---|---|---|---|
| début → explosion 1 | 1,8 s | 1,5 s | 15 % |
| explosion 1 → Big Bang 1 | 8,0 s | 7,8 s | 3 % |
| Big Bang 1 → Big Bang 5 | 8,5 s | 9,6 s | −13 % |
| Big Bang 5 → Big Bang 8 | 20,0 s | 11,8 s | 41 % |
| Big Bang 8 → galaxie | 20,7 s | 12,7 s | 38 % |
| galaxie → amas | 7,1 s | 1,8 s | 74 % |
| amas → univers | 11,8 s | 1,8 s | 85 % |
| **Univers 1 entier** | **77,8 s** | **47,0 s** | **40 %** |

L'univers se forme à 35 h 38 au lieu de 35 h 36 : deux minutes de jeu perdues pour 40 % de calcul gagné. Entre le Big Bang 1 et le Big Bang 5, la partie coupée prend 44 min de retard (les Big Bangs 2 à 5 arrivent plus tard) et le rattrape ensuite : c'est la zone où le hasard pèse, voir la 0.204.

Famille par famille :

| Plan | Calcul | Gain | Arrivée (témoin 35:36:00) |
|---|---|---|---|
| Automatismes seuls | 80,0 → 62,1 s | 22 % | 35:32:20 |
| Améliorations seules | 78,4 → 61,0 s | 22 % | 35:22:00 |
| Mécaniques seules (défis, collisionneur) | 79,4 → 78,8 s | 1 % | 35:37:20 |
| Tout | 77,8 → 47,0 s | 40 % | 35:38:00 |

Les deux gains viennent du même endroit : après le Big Bang 5, les automatismes et le « tout acheter » du bot continuent d'empiler des niveaux de particules et d'atomes qui ne changent plus rien. Les mécaniques ne pèsent presque rien dans le calcul. Rien n'est branché dans le bot : c'est une mesure, la coupure reste à décider (elle ne vaut que pour l'univers 1 et pour ce profil).

### 2. Pourquoi 35 h : le Speedrunner des audits ne tient pas la touche M

L'univers 1 du Speedrunner (graine 201, `FreshUniverseAudit`, pas de 20 s), version par version :

| Version (commit) | Univers 1 formé |
|---|---|
| 0.160 à 0.166 (`26a74a2`, `72e226b`, `07babb8`) | 26 h 47 à 26 h 53 |
| **0.167** (`82e24f7`) | **41 h 14** |
| 0.168 à 0.177 (`43a6331` … `64abfa3`) | 41 h 14 |
| acte 3 facilité (`9bbc8ff`) | 40 h 31 |
| 0.184 (`dfa6388`, avec les seuils de la 0.181) à 0.204 | 35 h 26 |

Le saut est à la 0.167 : depuis, **un automatisme n'achète qu'un niveau par action** (ou son lot, payé en matière noire), et le joueur attentif tient la touche M. Le bot ne tient M que si son profil décide au moins toutes les 5 s (`IdleProfiles24.HOLD_MAX_ACTION_SECONDS`). Le bot analyste (5 s) la tient ; le profil Speedrunner des audits décide toutes les 20 s et ne la tient pas : il appuie une fois par décision. Vérifié : le même audit avec la limite passée à 20 s forme l'univers 1 à **25 h 05** au lieu de 35 h 26 (4 514 décisions au lieu de 6 377 ; 109 s de calcul au lieu de 80).

Les durées de 21 h 50 à 26 h des notes 0.167 à 0.181 viennent du bot analyste, qui tient M ; les 35 h viennent des audits et d'`AnalyseArrets`, qui ne la tiennent pas. Ce sont deux joueurs différents, pas une régression du jeu : la 0.181 a bien raccourci l'univers 1 (41 h 14 → 35 h 26 sans M).

Conséquence pour la 0.204 : ses tableaux décrivent un joueur qui ne tient pas M. Pour un joueur qui la tient, les automatismes de particules comptent sans doute encore moins.

## À partir de quand une chose ne sert plus (version 0.204)

Demande : couper les automatismes, puis les améliorations, puis les mécaniques, et noter à partir de quand il devient inutile de les avoir. L'outil `AnalyseArrets` le mesure au lieu de le supposer.

**La méthode.** Une partie de référence (Speedrunner, graine 201, une décision toutes les 20 s, depuis zéro jusqu'à l'univers 1 formé : **35 h 25 min 40**) est photographiée à sept jalons. Depuis chaque jalon, la suite est rejouée avec une seule chose coupée, et son heure d'arrivée est comparée à celle d'un **témoin** : la même suite, même hasard, sans rien couper. Une coupure « ne sert plus » à un jalon quand elle ne retarde pas l'arrivée de plus que la tolérance (deux minutes, ou 0,5 % du temps restant). Un écart de plus de 30 s est rejoué avec un second hasard ; le verdict garde le pire des deux. Une suite trop en retard (plus de 30 min, ou 5 % du reste) est arrêtée : c'est le « plafond ».

| Jalon | Heure | Tolérance |
|---|---|---|
| 1 début | 00:00:00 | 10 min 26 |
| 2 explosion 1 | 14:24:40 | 6 min 11 |
| 3 Big Bang 1 | 18:54:40 | 4 min 57 |
| 4 Big Bang 5 | 22:21:40 | 3 min 55 |
| 5 Big Bang 8 | 26:02:20 | 2 min 49 |
| 6 galaxie | 29:44:40 | 2 min 00 |
| 7 amas | 31:44:40 | 2 min 00 |

Ce que coupe chaque famille : un **automatisme** reste éteint (acheté ou offert, il ne se rallume pas) ; une **amélioration** ne s'achète plus (ses niveaux déjà pris restent) ; une **mécanique** n'est plus jouée par le bot.

**À lire avant les tableaux.**
- *Avant le Big Bang 5, le hasard pèse jusqu'à 50 minutes.* Deux témoins qui ne diffèrent que par le hasard arrivent à 23 min d'écart depuis l'explosion 1 et 17 min depuis le Big Bang 1 ; une coupure sans effet réel y montre des écarts de −1 h 27 à +1 h 42. Aux jalons 1 à 3, seul un « plafond » sur les deux hasards est une preuve ; les « gains » affichés par l'outil à ces jalons (Patience, Compression) sont dans ce bruit et ne sont pas retenus. À partir du Big Bang 5, les deux témoins arrivent à la même seconde et les écarts se lisent tels quels.
- *C'est le Speedrunner.* Il achète à la main à chaque décision, toutes les 20 s, sans jamais s'absenter : un automatisme lui sert moins qu'à un joueur qui ferme le jeu. « Ne sert plus » veut dire « à ce bot, sur l'univers 1 ».
- *Un seul univers.* Une amélioration durable « inutile » après le Big Bang 5 de l'univers 1 est gardée pour les univers suivants, non mesurés ici.

### 1. Les automatismes

| Automatisme | Ne sert plus à partir de | Ce qui le montre |
|---|---|---|
| Couplage | Big Bang 1 | −40 s / −1 min 20 au Big Bang 1, 0 s ensuite |
| Automatismes offerts (matière noire) | Big Bang 1 | −3 min / +2 min 20, 0 s ensuite |
| Automates du collisionneur | Big Bang 1 | 0 s dès le Big Bang 5 ; le bot règle le collisionneur à la main |
| Rassemblement automatique | Big Bang 1 | +40 s / +20 s, 0 s ensuite |
| Formation automatique des astres | Big Bang 1 | +20 s, 0 s ensuite |
| Appui automatique | Big Bang 1 | 0 s dès le Big Bang 5 |
| Vitesse de création | Big Bang 5 | plafond depuis le début et l'explosion 1 ; 0 s dès le Big Bang 8 |
| Nouveau générateur | Big Bang 5 | plafond jusqu'au Big Bang 1, 0 s ensuite |
| Améliorations en atomes (matière noire) | Big Bang 5 | plafond depuis l'explosion 1, 0 s dès le Big Bang 8 |
| Achat des automatismes (matière noire) | Big Bang 5 | plafond depuis l'explosion 1, +20 s au Big Bang 5 |
| Arbre de matière noire (automatisme) | Big Bang 5 | +6 min 40 sur un hasard au Big Bang 1, 0 s ensuite |
| Fusion des générateurs | galaxie | plafond jusqu'au Big Bang 8, −40 s à la galaxie |
| Explosion (matière noire) | galaxie | plafond jusqu'au Big Bang 8, −40 s à la galaxie |
| Création automatique des molécules | amas | +26 min du Big Bang 5 à la galaxie, 0 s à l'amas |
| Synthèse d'éléments | **jamais** | plafond jusqu'à la galaxie, encore +3 min 20 / +2 min 20 à l'amas |

En clair : dès le Big Bang 5, les trois automatismes des particules (vitesse, couplage, générateur) et tous ceux de la matière noire sauf l'explosion peuvent s'éteindre sans rien coûter. La fusion et l'explosion comptent jusqu'à la galaxie formée. Les molécules comptent jusqu'à l'amas. La synthèse compte jusqu'au bout. La sobriété de la 0.202 éteint la boucle après le dernier Big Bang ; cette mesure ne dit pas qu'elle peut s'allumer plus tôt, parce qu'ici le bot continue d'acheter à la main ce que l'automatisme coupé n'achète plus.

### 2. Les améliorations

| Amélioration | Ne sert plus à partir de | Ce qui le montre |
|---|---|---|
| Couplage | Big Bang 1 | plafond depuis le début ; −40 s / −40 s au Big Bang 1 |
| Dédoublement | Big Bang 1 | plafond depuis le début ; rien de net ensuite |
| Masse atomique | Big Bang 1 | plafond depuis le début ; −20 s au Big Bang 1, 0 s ensuite |
| Compression | Big Bang 1 | dans le bruit à tous les jalons, 0 s dès le Big Bang 5 |
| Achat groupé | Big Bang 1 | plafond sur un hasard depuis l'explosion 1 ; 0 s dès le Big Bang 5 |
| Vitesse de création | Big Bang 5 | plafond jusqu'au Big Bang 1 |
| Nouveau générateur | Big Bang 5 | plafond jusqu'au Big Bang 1, 0 s ensuite |
| Rendement | Big Bang 5 | plafond jusqu'au Big Bang 1, 0 s ensuite |
| Catalyseur | Big Bang 5 | plafond jusqu'à l'explosion 1 ; +8 min 40 sur un hasard au Big Bang 1 |
| Persistance | Big Bang 5 | plafond jusqu'à l'explosion 1 ; +13 min 40 sur un hasard au Big Bang 1 |
| Patience | Big Bang 5 | +7 min 40 sur un hasard au Big Bang 1 ; jamais de plafond |
| Surcharge | Big Bang 5 | +9 min 20 sur un hasard au Big Bang 1 ; jamais de plafond |

En clair : **à partir du Big Bang 5, plus aucun achat d'amélioration en particules ou en atomes ne change l'heure de l'univers**, à vingt secondes près. Les niveaux pris avant suffisent. Trois améliorations ne passent jamais le plafond, même coupées depuis le début : Patience, Compression et Surcharge. Leur effet sur ce bot est plus petit que le bruit du hasard ; ce sont les trois à regarder si l'on veut rééquilibrer.

### 3. Les mécaniques

| Mécanique | Ne sert plus à partir de | Ce qui le montre |
|---|---|---|
| Défis de matière noire | Big Bang 5 | plafond jusqu'au Big Bang 1, 0 s ensuite |
| Collisionneur (achats et réglages) | Big Bang 8 | +27 min 40 / +14 min 20 au Big Bang 5, puis +1 min |
| Anomalies du ciel | **jamais** | +4 min 40 à l'amas (−2 min 40 à la galaxie : dans la tolérance) |
| Comètes | **jamais** | +8 à +12 min du Big Bang 5 à l'amas |
| Sondes | **jamais** | plafond à la galaxie et à l'amas |
| Impulsions d'espace | **jamais** | plafond à la galaxie et à l'amas |
| Touche M tenue | sans objet | le Speedrunner au pas de 20 s ne la tient pas : 0 s partout |

### Refaire la mesure

```
./gradlew :bots:arrets -Psortie=sorties/arrets                        les automatismes, univers 1
./gradlew :bots:arrets -Psortie=sorties/arrets -Pids=ameliorations    ou mecaniques, arbre, dimensions, ou des identifiants séparés par des virgules
./gradlew :bots:arrets -Pdepart=une.save -Punivers=4 -Pids=dimensions  depuis une sauvegarde, jusqu'à l'univers 4
```

La sortie tient en trois fichiers, réécrits après chaque coupure : `arrets.txt` (le tableau), `arrets.csv` (une ligne par coupure et par jalon) et `journal.txt` (l'avancement). Comptez 4 à 5 minutes par coupure sur deux cœurs : 67 min pour les 15 automatismes, 31 min pour les 7 mécaniques, environ 50 min pour les 12 améliorations.

**Pas encore mesurés** : les 62 cases de l'arbre de matière noire (`-Pids=arbre`, environ 4 h 30) et les automatismes des dimensions, qui n'existent qu'aux univers suivants (`-Pids=dimensions`, depuis une sauvegarde).

**Dans le moteur.** Pour qu'un automatisme reste éteint alors que le jeu le rallume à l'achat, `GameState.testBlock(clé)` le tient éteint le temps d'une mesure. Ce réglage n'est pas enregistré dans la sauvegarde et le jeu ne l'appelle jamais.

## Des simulations 40 % plus rapides, et la sobriété des bots (version 0.202)

Demande : que le bot ne fasse plus grandir une ressource qui n'en a plus besoin, pour moins d'actions et une simulation plus rapide. Mesuré d'abord (Speedrunner, graine 201, profil JFR) : les **décisions du bot ne pèsent que 9 %** du calcul ; les 91 % restants sont le moteur entre deux décisions, surtout les automatismes que le bot laisse allumés (fusion 37 %, synthèse 23 %, achats 6 %, matière noire 10 %). Trois changements en sortent.

**1. Le compte des skins, gardé (moteur, au bit près).** Le bonus des skins se relit à chaque fusion et à chaque calcul de production ; depuis les patchs des skins (~370), le compter skin par skin prenait **10 à 32 % du calcul**. Il se garde maintenant tant que les skins débloqués ne changent pas (un numéro de version dans l'état, changé à chaque déblocage, verrouillage, remise à zéro ou relecture). Deux univers depuis zéro : **414 s → 260 s**, historique horaire et sauvegarde finale identiques octet pour octet.

**2. La chance des éléments, à part (moteur, au bit près).** À chaque synthèse, le tirage lisait la chance des familles rares et le double tirage dans *tous* les bonus des 118 éléments, recalculés puisque la synthèse d'avant venait de changer le tableau : **42 % du calcul** d'un univers tardif. Ces deux nombres se calculent maintenant seuls (`ElementBonuses.luckAndDoubleDraw`), avec les mêmes termes dans le même ordre : les mêmes nombres au bit près (`ChanceDesElementsTest`). Univers 3 et 4 depuis la sauvegarde « univers 2 formé » : **845 s → 514 s (−39 %)**, étapes et historique horaire identiques ; deux univers depuis zéro : 260 s → 239 s, identiques.

**3. La sobriété (bot, allumée par défaut).** Après le dernier Big Bang de l'univers, quand plus rien ne demande d'éléments — ni la liste de courses, ni la matière ou la variété d'une échelle du cosmos à venir — ni de particules ou d'atomes (palier de Big Bang, case finie de l'arbre), le bot éteint les automatismes ordinaires allumés (achats, fusion, synthèse), ne fait plus ses propres achats ni son « tout acheter », et rallume le tout tel quel dès qu'un besoin revient (`RobotCosmosPlanner.loopIdle`, `applySobriety`). Rien ne s'éteint pendant un défi, ni quand l'arbre mûr, à droite, fait passer la matière noire à l'univers suivant. `-Didle.sansSobriete=true` la coupe.

| Mesure | Sans | Avec |
|---|---|---|
| Deux univers depuis zéro, Speedrunner | 260 s | 234 s, toutes les étapes à la seconde |
| Deux univers depuis zéro, Régulier | 319 s | 301 s, toutes les étapes à la seconde |
| Univers 3 et 4 (moteur 0.202) | 514 s | 438 s ; univers 3 à la seconde, univers 4 **40 à 76 s plus tard** |

**Ce qu'elle change.** L'univers où elle agit finit à la même seconde. Mais le hasard de la synthèse continue d'un univers à l'autre : des synthèses en moins pendant la pause décalent les tirages de toute la suite, et l'univers suivant se forme une minute plus tard ou plus tôt sur huit heures — l'ordre d'un changement de graine. Une partie avec sobriété n'est donc plus identique au bit près à celle d'avant la 0.202 au-delà du premier univers où elle agit.

**Essayé et écarté, en mesurant.**
- *Une limite des atomes seuls* (couper la fusion dès qu'il y a de quoi payer les synthèses) : la fusion fait aussi grandir la matière noire, qui accélère les automatismes ; sans elle, la synthèse tombait de 21 à 1,1 par seconde et deux univers prenaient 2 h 34 de plus. Le compter sur le rythme mesuré de la synthèse n'y changeait rien.
- *Couper pendant que la galaxie mûrit* (3 h 30 de pur temps d'attente) : les éléments synthétisés pendant cette attente font la matière de l'amas (60 millions de gaz) ; l'amas partait à vide, +3 h 49. D'où la règle : les besoins comptés sont ceux de tout le reste de l'univers, pas du seul prochain pas.
- *Les cases sans fin de l'arbre* (Densité, achetée jusqu'au niveau 99 000 ; Noyau lourd) ne comptent pas comme un besoin : elles ne font que nourrir la boucle.
- *Le « tout acheter » du bot pendant la pause* : sans fusion, les générateurs s'empilaient jusqu'à cent, et le skin Essaim tombait à l'univers 3 (+2 % pour toute la suite).

## Des tests qui prennent le temps du plus long (version 0.172)

Où partait le temps de `./gradlew toutTester` en local, mesuré sur la 0.171 : le moteur, 1 223 tests en 16 s ; l'interface, 152 tests en 38 s ; les bots hors campagnes, 112 tests en 169 s dont 150 s pour le seul test de l'expert ; et les deux campagnes des 24 profils (`Profiles24CampaignTest`, `Profiles24LongCampaignTest`), que Gradle joue en entier en local (la CI les découpe en quatre segments) : **2 158 s (36 min) d'un trait** : 1 836 s pour les 24 profils avancés (16 h chacun), 321 s pour les quatre profils longs, moins d'une seconde pour les 24 parties neuves (2 h de début de partie, presque rien à calculer). Trois leviers indépendants, qui ne changent ni les parties jouées ni ce que les tests vérifient :

**1. Un test par profil, joués en même temps.** Les deux campagnes bouclaient sur les 24 profils dans un seul test. Chaque profil est maintenant un test à lui (`@TestFactory` : une ligne par profil dans le rapport, « seize heures avancées : 07 Casual », et un profil qui échoue n'empêche plus les autres de finir), et les méthodes de ces deux classes portent `@Execution(CONCURRENT)` : JUnit les joue sur autant de fils que de cœurs (`src/test/resources/junit-platform.properties`). Chaque profil a sa partie, sa graine et son dossier ; le moteur ne partage rien entre deux parties (c'est ce que font déjà les « et si » de l'analyste et les univers en parallèle). L'annotation est sur les méthodes, pas sur les classes : sur une classe, JUnit la jouerait en même temps que les autres classes, dont `AnalyseTest`, qui règle un état partagé des bots (`UniverseTreePolicy.withoutBubbles`) ; sur les méthodes, la classe suivante attend la fin de toutes les parties. Vérifié : les 722 lignes `PROFIL24_*`, `RESULT` et `EVENT` de la campagne complète sont les mêmes, à la ligne près, jouées d'un trait ou en parallèle (l'ordre seul change) ; sur deux cœurs, la campagne passe de **2 158 s à 1 156 s** (×1,87 ; le plafond pour deux cœurs est ×2, le reste est la concurrence des deux parties pour la mémoire) ; sur une machine à huit cœurs, elle ne peut pas descendre sous le profil le plus long de chaque classe (138 s pour le WeekendPlayer avancé, 123 s pour l'AFK long), soit **environ 6 min au lieu de 36** (quatre cœurs : environ 10 min).

**2. Les modules en même temps, et seulement ceux qui ont changé.** `gradle.properties` allume `org.gradle.parallel` : les tâches de test de `core`, `ui` et `bots` sont indépendantes et tournent ensemble, « tout tester » dure le temps des bots, pas la somme des trois. Il allume aussi le cache de construction : `./gradlew test` (ou `tester-change.sh` / `.bat`, à la racine du projet) saute un module dont ni le code, ni les tests, ni les sauvegardes lues par les bots n'ont bougé depuis le dernier passage vert — les sauvegardes de `sauvegardes/` sont déclarées comme entrée de la tâche de test des bots, une sauvegarde de référence modifiée relance donc bien leurs tests. `./gradlew toutTester` rejoue toujours tout (il refuse aussi le cache). La tâche de test des bots a 2 Go de tas, pour autant de parties avancées que de cœurs. Pas mesuré ici (Gradle n'est pas disponible dans l'environnement de travail) : ce sont des réglages standards de Gradle, à constater sur ta machine.

**3. Le reste était déjà réparti.** Depuis la 0.170, les classes de tests se partagent plusieurs JVM (`maxParallelForks`), et le test de l'expert (150 s seul sur la machine) a sa classe : il tourne pendant la campagne, pas après.

**Ce qui n'a pas été fait, et pourquoi.** Jouer les tests du moteur en parallèle dans leur JVM : deux de ses tests règlent un drapeau statique (`GameParticles.powersOff`, remis après coup), un troisième le premier élan ; 16 s ne valent pas ce risque. Raccourcir le test de l'expert ou les campagnes (pas plus grand, moins d'heures) : ils ne vérifieraient plus la même chose. Réutiliser les 16 premières heures des quatre profils du contrôle long (ce sont exactement les parties de la campagne avancée, mêmes graines) : un gain de 13 % des heures jouées pour deux classes qui dépendraient l'une de l'autre ; écarté.

## Compresser le temps des simulations (version 0.170)

Une partie de onze univers du Speedrunner (graine 201, pas de 20 s) demande plus d'une heure de calcul ; le bot analyste, à vitesse humaine avec ses estimations, bien davantage. La 0.170 s'attaque au temps de calcul par quatre côtés indépendants, qui se cumulent, **sans changer un seul nombre du jeu** : chaque méthode est vérifiée sur des sauvegardes et des mémoires de bot identiques octet pour octet, ou sur l'empreinte du moteur.

**1. Le moteur, au bit près.** Un profil d'une partie entière (JFR) montrait que la moitié du temps passait dans le tirage des éléments et le cumul de leurs bonus, refait à chaque synthèse : des listes construites à chaque tirage (`availableElements`), des recherches dans des tables (le plafond de chaque élément, les familles, l'identifiant des skins recalculé à chaque appel). Le tirage lit maintenant le tableau des exemplaires et un tableau des plafonds relevés, sans construire de liste ; le cumul des bonus part du tableau que le jeu tient à jour (`ElementBonuses.of(int[])`) ; l'identifiant d'un skin est calculé une fois. Les tirages au sort sont les mêmes, dans le même ordre (la famille au poids, puis le n-ième élément disponible par numéro croissant) : vérifié sur quatre heures du bot expert depuis un Big Bang neuf (`events.csv`, `timeline.csv` identiques), sur deux univers d'un trait (événements à la seconde identiques à la référence 0.169), sur l'empreinte du moteur et sur cinq sauvegardes de référence (même texte de sauvegarde après 1 800 s). Et sur la longue partie de référence : les huit premiers univers du Speedrunner (graine 201) se lancent aux mêmes heures de jeu que sur le moteur 0.169 (41,2278 ; 64,3556 ; 85,3000 ; 106,2000 ; 126,6444 ; 144,5444 ; 154,6111 ; 164,0444 h), et deux heures de l'univers 9 rejouées depuis le jalon 8 par les deux moteurs donnent la même sauvegarde et la même mémoire de bot, octet pour octet (`Croise`, un lanceur de dix lignes autour de `UniversParalleles.tranche` et `avancer`). Le gain : 10 à 20 % du temps de calcul quand le tableau se remplit (synthèse sans délai) ; peu quand il est plein. Les 1 215 tests du moteur passent, dont les nouveaux de `Moteur170Test` (bonus identiques depuis la table ou le tableau, plafonds respectés, cible qui donne d'abord les inconnus).

**2. Les univers en parallèle (`UniversParalleles`).** La grande compression. Une partie jouée d'un trait écrit un *jalon* à chaque univers lancé : la sauvegarde du jeu et la mémoire du bot (`AnalyseMemoire`, 0.168). Rejouée, chaque tranche d'univers repart de son jalon, et comme les tranches sont indépendantes, elles se jouent toutes en même temps, une par cœur : onze univers coûtent le temps du plus long, pas la somme des onze. Après un changement du jeu ou des bots, les anciens jalons servent de *paris* : toutes les tranches partent en parallèle, puis le jalon que produit la tranche k est comparé octet pour octet (sauvegarde et mémoire) à celui dont la tranche k + 1 est partie. Identiques : la tranche k + 1 était la bonne, son résultat est acquis. Différents : elle est rejouée depuis le nouveau jalon, et la vérification reprend jusqu'à ce que la chaîne tienne. Un changement qui n'agit qu'à l'univers 8 ne coûte que les univers 8 à 11, ensemble ; un changement dès le début ne coûte pas plus qu'une partie d'un trait (chaque tranche repart dès que la précédente finit, ce que l'outil fait aussi sans aucun jalon). `chaine.txt` dit, pour chaque univers, s'il est identique à la référence et sinon ce qui diffère (lignes de la sauvegarde, champs de la mémoire). Vérifié : deux univers d'un trait et deux tranches en parallèle donnent les mêmes jalons, octet pour octet, événements à la seconde près. Mesuré au passage : les huit premiers univers du Speedrunner se jouent en 41 minutes, et le neuvième seul en demande plus de 80 (les nombres y sont énormes et les décisions nombreuses) ; c'est pour les derniers univers, rejoués chacun sur son cœur depuis son jalon, que l'outil rapporte le plus.

```bash
python run.py jalons --out sorties/jalons --univers 11                              # la référence, d'un trait (une fois)
python run.py paralleles --reference sorties/jalons --out sorties/jalons2 --univers 11   # après un changement : tous les univers en parallèle
```

Les nouveaux jalons (`sorties/jalons2`) deviennent la référence du changement suivant. Ce que les tranches écrivent (événements, garde-fou) part dans un tampon par fil (`SortieParFil`), pour que des parties jouées en même temps ne mêlent pas leurs lignes.

**3. Le bot analyste espace ses estimations.** Il jouait la suite sur une copie toutes les 30 minutes de jeu, jusqu'au palier suivant : un palier à 20 h se mesurait quarante fois, soit quatre cents heures de copie pour vingt heures de partie, le gros du temps de l'analyste en fin de partie. Une estimation qui annonce le palier dans d fait maintenant attendre la suivante d/2 au moins (`PART_ESTIMATION`) : un palier à 20 h se mesure à 0, 10, 15, 17 h 30, 18 h 45…, cinq ou six fois, et les copies pèsent au plus deux fois le temps du palier. La partie elle-même ne change pas : une estimation ne décide rien, elle annonce et mesure (vérifié : `fin.save` et `fin.bot` identiques avec et sans). Ce qui change : `estimations.csv` et la courbe des estimations du rapport ont moins de points, et un blocage réel se détecte un peu plus tard (au plus la moitié du temps annoncé).

**4. Les tests répartis.** Gradle lance maintenant les classes de tests sur plusieurs JVM (`maxParallelForks`, une par deux cœurs), et le plus long test des bots (l'expert du septième Big Bang à l'univers : 150 s seul sur la machine, un quart d'heure quand d'autres calculs tournent ; les 111 autres tests des bots tiennent en 20 s) vit dans sa propre classe, `ExpertPremierUniversTest` : les tests des bots prennent le temps de celui-là, pas la somme de tous.

**Ce qui a été essayé et écarté.** Un pas de simulation plus grand ou des phases d'attente sautées changent les nombres : pas pour une mesure de référence. Les estimations de l'analyste sur un autre fil cacheraient tout leur coût sur plusieurs cœurs, mais une estimation en retard change le journal et le moment d'un arrêt sur blocage : à voir une autre fois, la sortie par fil est prête pour ça.

## Une partie en plusieurs tranches, les bulles et l'arbre « sans » (version 0.168)

**La mémoire du bot à côté de chaque sauvegarde.** À chaque palier, et à la fin (`fin.save`), le bot écrit un fichier `.bot` du même nom : les champs de jeu du bot et de ses politiques, leurs compteurs, leurs hasards, et le hasard de la partie elle-même (`AnalyseMemoire`). Reparti de cette sauvegarde (`depart=…`), le bot les relit : la tranche suivante continue la même partie. Vérifié : une heure jouée d'un trait et deux demi-heures reprises donnent la même sauvegarde, champ pour champ. Ce qui sert à l'analyse (journal, relevés) n'en fait pas partie : chaque tranche écrit ses fichiers, à mettre bout à bout.

**Ce que le journal note de plus.**
- Les achats faits au pas d'une remise à zéro (l'univers lancé, une explosion, un Big Bang) : les bulles de l'arbre, souvent prises juste avant de lancer l'univers suivant, manquaient dans `achats.csv`. Leur poids s'écrit « poids non mesuré, remise à zéro au même pas ».
- Le palier final de chaque dimension, un palier à lui (« Palier final de la dimension 3 atteint »), en plus du compte du défi final.
- L'attente d'une seule condition de la galaxie, de l'amas ou de l'univers dit laquelle : l'astre qui n'est pas formé ou mûrit encore, les systèmes solaires, l'état de la matière qui manque (`Game.cosmosMissing`).

**Les « sans » de l'arbre et des bulles.** `sans` accepte les cases de l'arbre de matière noire (déjà en 0.164) et maintenant les bulles de l'arbre : `bulle:LEAVES_LEFT`. `AnalyseDurables <sortie> arbre` mesure les huit cases les plus achetées dans l'univers 1 ; `AnalyseDurables <sortie> bulles <sauvegarde> <univers> [bulle:NOM,…]` mesure les bulles depuis une sauvegarde, univers formé par univers formé.

**L'ordre des bulles imposé.** Un douzième argument du bot analyste, `meilleur` (l'ordre de l'expert) ou `moins-cher`, impose l'ordre des bulles à tous les profils : avec `AnalyseGraines` (plusieurs graines), de quoi mesurer la dispersion d'une partie à l'autre.

## La touche M tenue, l'achat en lot et la Persistance par niveaux (version 0.167)

**La touche M.** Depuis la 0.167, les automatismes n'achètent qu'un niveau par action, ou leur lot. Le joueur attentif tient donc la touche M (« tout acheter avec les particules », que le jeu répète tant qu'elle est tenue). Les profils qui décident au moins toutes les 5 s et ne relâchent jamais leur attention la tiennent : le pas de jeu se coupe en morceaux d'une demi-seconde, avec un « tout acheter » après chacun (`IdleProfiles24.holdsBuyAll`). Les autres, comme le Régulier, l'appuient une fois à chaque décision.

**L'achat en lot.** Le bot achète les niveaux d'achat en lot des automatismes (×2 à ×64, payés en matière noire, gardés tout l'univers) avant le reste, le moins cher d'abord, sans entamer la réserve du Big Bang quand elle se garde (`FirstUniversePolicy.automationBatches`). Il prend aussi la bulle du bonsaï à droite, qui supprime le délai des automatismes : sans elle, ils n'achètent qu'un niveau par action.

**La Persistance par niveaux.** Ses cinq niveaux (8 à 118 atomes) passent avant le reste, un par un, une fois l'automatisation lancée ; la réserve d'atomes posée pour le prochain niveau est rendue telle quelle ensuite (`RobotCosmosPlanner.prioritizePersistence`).

**Ce que mesure la 0.167** (graine 201). Les parties ne tiennent plus en une tranche de 2 h de calcul : en fin de partie, le jeu simule environ 400 fusions et 400 synthèses par seconde, et le calcul avance de 3 à 5 h de jeu par heure réelle. Elles ont été jouées en tranches, chacune reprise de la dernière sauvegarde de la précédente, puis mises bout à bout. Une tranche coupée n'écrit ni `illogismes.csv` ni `rapport.html`.

| Partie | Fin | Univers formés | Univers 1 formé | Minuteur de rassemblement |
| --- | --- | --- | --- | --- |
| Speedrunner 0.166 | 110:44:10 | 15 | 23:05:00 | 18 h 26 (16,7 %) |
| Speedrunner 0.167 | 87:23:20 | 18 | 26:08:55 | 23 h 07 (26,5 %) |
| Régulier 0.166 | 186:24:35 | 16 | 35:02:05 | 21 h 07 (11,4 %) |
| Régulier 0.167 | pas finie (univers 14 à 234:37) | 13 | 54:03:05 | — |

Le Speedrunner décide 271 fois par heure au lieu de 471. Le Régulier, qui ne tient pas M, met 11 à 14 h pour l'acte 1 des univers 2 à 5 (3 à 4 h en 0.166) : l'achat en lot ne vient qu'avec l'acte 2.

À savoir : les achats faits en tenant M ne sont pas notés un par un dans `achats.csv` (3 043 lignes « Amélioration » au lieu de 41 379). L'achat en lot se note dans `achats.csv` depuis une correction du moteur faite après ces parties.

## Profils, causes, charge de décisions, dimensions et graine (version 0.166)

**D'autres joueurs que le Speedrunner.** `-Pprofil=regulier` joue le Régulier des profils (une décision par minute, 90 % d'attention) ; `-Pprofil=absent` joue le Speedrunner, mais cinq minutes par heure, le jeu fermé entre deux visites (l'absence compte au taux hors ligne, 24 h au plus par absence). Le blocage (« plus rien ne bouge depuis 30 min ») se juge maintenant sur le temps en ligne.

**Ce qui débloque chaque palier.** `paliers.csv` gagne quatre colonnes : le temps réel, le temps en ligne, le temps de jeu compté hors ligne, et la cause : la dernière chose faite dans la minute d'avant (un achat, une chose obtenue, un défi), ou « le temps qui passe ». Pour un Big Bang, la dernière de ses conditions à se remplir, et combien de temps elle a manqué seule.

**La charge de décisions.** `decisions.csv` et le rapport donnent, univers par univers, les décisions par heure (un pas où le bot achète quelque chose ; un lot compte pour une) et les décisions utiles (celles qui font gagner au moins une seconde vers un palier).

**Les dimensions et la graine, même traitement.** Leurs améliorations se jugent sur leur propre palier (`AnalyseSuivi`) : le seuil de leur dimension, puis le palier du défi final ; le palier suivant de la graine. Le gain se mesure sur le rythme des cinq minutes d'avant et des cinq minutes d'après l'achat ; le coût en temps de production, au rythme de la dimension qui paie. Le rapport a une section « Dimensions, graine et défi final ».

**Deux détecteurs de plus.**

| N° | Détecteur | La règle |
|---|---|---|
| 11 | Réserve qui dort | la matière noire, ou l'énergie du vide gardée pour les bulles, plus de dix fois au-dessus de ce qu'elle peut payer (la réserve du Big Bang ou la case de l'arbre la moins chère ; la bulle ouverte la moins chère) pendant plus d'une heure |
| 12 | Une seule condition qui manque | un Big Bang, ou une échelle du cosmos, à qui il ne manque qu'une condition pendant plus d'une heure |

**Le vide se garde pour les bulles.** La galaxie formée, et tant que la bulle du géant n'a pas rendu le vide permanent, le bot n'achète plus que les raccourcis du vide (maturation, rassemblement, allègement) : ce qui reste à la formation paie les bulles (`VoidPolicy.savingForBubbles`). Il gardait 5 076 d'énergie à la formation de l'univers 4 avec les prix 0.166 ; il en garde 46 707.

**Ce que vaut chaque amélioration durable.** `./gradlew :bots:durables` joue le premier univers avec tout, puis sans chacune des neuf améliorations payées en atomes (celles que la fusion garde) ; `durables.txt` et `durables.csv` donnent l'écart à chaque grand palier. C'est la mesure qui remplace le gain estimé là où il se trompe (la Persistance).

**Ce que mesure la 0.166** (graine 201, une décision toutes les 5 s pour le Speedrunner, toutes les 60 s pour le Régulier) :

| Partie | Fin | Univers formés | Rassemblement | Calcul |
|---|---|---|---|---|
| Speedrunner 0.165 | 82:22:00 | 18 | 21 h 00 (25,6 %) | — |
| Speedrunner 0.166 | 110:44:10 | 15 | 18 h 26 (16,7 %) | 67 min |
| Régulier 0.166 | 186:24:35 | 16 | 21 h 07 (11,4 %) | 43 min |
| Absent 0.166 | arrêté à 817 h réelles (univers 5) | 4 en 751 h réelles, dont 63 h en ligne | — | 2 h (limite) |

Les améliorations durables du premier univers (`:bots:durables`), ce qu'il faut de plus sans elles pour former l'univers 1 (23:05:00 avec tout) : Rendement +5 h 08, Persistance +4 h 52, Dédoublement +1 h 50, Catalyseur +1 h 05, Masse atomique +59 min, Patience +39 min, Surcharge +17 min, Achat groupé +2 min ; Compression −30 s (et le premier Big Bang 10 min plus tôt sans elle).

À savoir : une partie reprise d'une sauvegarde repart avec un autre hasard et une mémoire neuve. En 0.166, cela change beaucoup : reprise à 49 h 31, la partie du Speedrunner finit à 90:40:00 au lieu de 110:44:10 (elle achète ses bulles dans un autre ordre). Les achats faits au pas qui lance un univers (les bulles, souvent) ne sont pas dans achats.csv.

**La réserve du Big Bang se garde plus tard.** Avec la réserve de 120 000 du premier Big Bang (moteur 0.166), le bot ne gardait sa matière noire qu'en arrêtant ses achats dans l'arbre pendant 3 h : le Big Bang reculait de 42 min. Il ne la garde plus qu'à 90 % des ordres de grandeur de particules (`FirstUniversePolicy.particlesNear`).

## La lecture des achats passe dans le moteur (version 0.165)

Le jeu note maintenant les achats du joueur comme le bot analyste (sous-page « Achats » des statistiques), et dit dans combien de temps vient la prochaine étape. Pour qu'ils lisent les achats de la même façon, ce qui était au bot vit dans le moteur, paquet `idle.core.history` :

| Avant (bots) | Maintenant (moteur) |
|---|---|
| `AnalyseEtat` | `Photo` : la partie clé par clé, et ce qui a changé entre deux photos |
| `AnalyseNoms` | `Noms` : les noms en français des clés et des choses |
| `BotAnalyste.achat`, `monnaie`, `coutRelatif`, `REGLAGES` et la répartition des prix | `Achats` : ce qui est un achat, sa monnaie, et `Achats.lire` qui rend les achats d'un diff avec leur prix |
| (nouveau) | `Pace` : la prochaine étape et le temps qu'elle demande au rythme des dix dernières minutes |
| (nouveau) | `PurchaseHistory` : les achats du joueur entre une action et le pas de temps suivant |

Le bot analyste joue exactement pareil : sur dix heures de jeu, `achats.csv` est identique à l'octet près avant et après. Le journal gagne les lignes « Éléments synthétisés », que l'ancien code ne pouvait pas atteindre (les éléments étaient pris pour une ressource juste avant). À supprimer : `src/main/java/AnalyseEtat.java` et `src/main/java/AnalyseNoms.java`.

## Illogismes, comparaisons, sans une amélioration, plusieurs graines (version 0.164)

**Les détecteurs d'illogismes** relisent chaque partie du bot analyste à la fin (`AnalyseIllogismes`), et l'écrivent dans `illogismes.txt`, `illogismes.csv` et en tête du rapport. Un signalement n'est pas une preuve : c'est un endroit où regarder.

| N° | Détecteur | La règle |
|---|---|---|
| 1 | Joueur moins présent plus rapide | un « et si » (une fois par minute, toutes les dix minutes) bat le bot de plus de 20 % sur une étape de plus de 5 min |
| 2 | Achat jamais rentable | 5 achats ou plus, au moins une minute de production en tout, moins d'un dixième rendu |
| 3 | Amélioration jamais achetée | une amélioration ordinaire ou une case de l'arbre que le bot n'achète jamais |
| 4 | Défi trivial | un défi réussi en moins d'une minute |
| 5 | Rafale de succès | cinq succès ou plus au même instant |
| 6 | Étape éclair ou interminable | une étape principale des onze premiers univers en moins de 30 s, ou en plus de 6 h |
| 7 | Estimation ratée | l'annonce se trompe de plus d'un quart de l'étape, et d'au moins 5 min |
| 8 | Automatisme en va-et-vient | coupé par le bot plus de 20 fois (pas par une remise à zéro) |
| 9 | Ressource jamais dépensée | gagnée, jamais payée dans un achat du bot |
| 10 | Gros multiplicateur sans effet | trois achats ou plus à ×1 000 ou plus, et pas une seconde gagnée en tout |

**Sans une amélioration** : `-Psans=atom_keep` (identifiants des améliorations ordinaires ou de l'arbre de matière noire, séparés par des virgules). L'amélioration garde son nom, mais prend un prix hors d'atteinte : personne ne l'achète, et les politiques la sautent au lieu de l'attendre (`Bots.interdite`). Ce que le jeu offre lui-même (la Persistance d'une bulle de l'arbre) reste offert.

**Comparer deux parties** : `./gradlew :bots:comparer -Pa=sorties/analyste -Pb=sorties/sans-persistance` écrit `comparaison.txt` et `comparaison.html` : pour chaque palier, le moment dans A, dans B, et l'écart. Pour deux versions du jeu : la même commande sur deux dossiers.

**Plusieurs graines** : `./gradlew :bots:graines -Pgraines=201,202,203 -Punivers=fin` joue le bot sur chaque graine (sans « et si »), chacune dans `graine-…`, et résume chaque palier : atteint combien de fois, le plus tôt, la médiane, le plus tard, et la dispersion ((plus tard − plus tôt) / médiane).

## Le bot analyste va jusqu'à la fin (version 0.163)

Par défaut, `analyste.bat` joue maintenant jusqu'à la fin de la partie : les dix paliers du défi final. Après l'univers 11, le bot continue de former des univers (leur énergie du vide paie les bulles), prend la bulle du sommet (l'automatisation des dimensions) puis celle des racines (le défi final), achète les automatismes de dimension et les améliorations de la graine dès qu'ils sont payables, et laisse les automatismes jouer les dimensions. `-Punivers=11` garde l'ancien arrêt à onze univers ; `-Pdepart=chemin/une.save` repart d'une sauvegarde (par exemple un palier d'une partie d'avant, dans `sauvegardes/`).

Mesure (graine 201, une décision toutes les 5 s) : la fin à **82:22:00** de jeu, après 18 univers formés.

| Moment | Étape |
|---|---|
| 76:12:25 | Univers 11 formé |
| 77:03:15 | Univers 12 formé |
| 78:34:55 | Bulle des racines : défi final ouvert, son premier palier (la ligne) déjà atteint |
| 79:07:55 | Les dix seuils des dimensions atteints |
| 82:22:00 | Dixième palier du défi final : fin de la partie |

**Ce qui bloquait.** Quatre choses empêchaient d'aller au bout, trois dans le jeu et une dans les bots :

- le plan automatique : une absence se rejoue par pas de 10 s, et un pas ne valait jamais plus d'un quart de seconde de course du point. En absence, il avançait quarante fois moins vite ; les bots, qui jouent par pas de 5 s, ne le voyaient presque pas ramasser. Le plan, le volume et la brane automatiques se jouent maintenant par morceaux de 0,05 s sur un grand pas : sur dix minutes, mêmes gains à 0,05 s, 5 s ou 10 s (le volume perdait la moitié à 10 s, la brane variait de −17 % à +46 %) ;
- la brane automatique tirait au milieu des points : quand ils étaient plus écartés que la portée, elle ne touchait plus rien, pour toujours (13,7 ondes, puis plus rien en deux heures). Elle vise maintenant le point d'où l'onde en touche le plus ;
- la bulle des racines s'ouvre avec une amélioration de la graine (payée en fragments du mélange), que les bots n'achetaient pas : ils ne prenaient que le débit de l'arrosoir. Ils achètent maintenant toutes les améliorations de la graine payables, celle des racines d'abord ;
- l'ordre d'achat des bulles de l'expert ne contient pas la bulle des racines, et l'expert n'achetait rien hors de sa liste. Sa liste finie, il prend la bulle payable la moins chère.

**Ce qui ne change pas.** Les références (pas de 20 s) restent les mêmes : Speedrunner univers 1 à 26 h 53 et univers 2 à 42 h 19, Régulier 34 h 30. Les univers 1 à 11 du bot analyste tombent aux mêmes moments qu'en 0.162 : les bots ne jouent pas les dimensions à la main, et les automatismes de dimension ne viennent qu'avec la bulle du sommet.

**Ce qui reste.** Le bot ne joue pas les dimensions à la main : le seuil du plan (ouvert à 35:53) n'est atteint qu'à 74:46, quand ses automatismes arrivent ; un joueur le ferait en un quart d'heure. La boucle automatique compte ses clics sur un grand pas au lieu de les jouer (22 % de moins sur dix minutes, une approximation voulue).

## Le bot analyste dit pourquoi (version 0.162)

Ce qu'il ajoute au journal et au rapport :

| Quoi | Ce qu'il dit |
|---|---|
| Chaque achat | Sa **rentabilité** : le temps de production qu'il a coûté (son prix divisé par ce que la ressource rapportait par seconde au pas d'avant, mesuré sans décision ; pour la matière noire, sur la dernière explosion), contre le temps qu'il fait gagner vers le palier suivant. Au-dessus de ×1, il rend plus de temps qu'il n'en coûte. |
| `classement.csv` et le rapport | Tout ce qui a été acheté, du plus rentable au moins rentable, avec le coût et le gain cumulés. |
| Chaque défi lancé | Sa contrainte, sa récompense, s'il est une condition du Big Bang, et le temps qu'il prendra, joué sur une copie. À la fin : réussi ou abandonné, en combien de temps, contre l'annonce. |
| Chaque succès | Ce qu'il demandait, son bonus propre, le bonus commun, s'il compte pour le Big Bang, et le poids des succès du moment sur chaque grandeur. |
| Chaque automatisme | Ce qu'il fait, et pourquoi il est mis en marche ou coupé (ce que le bot met de côté, le défi en cours, le goulot). |
| Chaque explosion | La matière noire gagnée, par heure depuis la précédente, et pourquoi maintenant. |
| `etsi.csv` et le rapport | À chaque palier principal, la suite jouée sur des copies jusqu'au palier suivant par un joueur qui passe une fois par minute, puis toutes les dix minutes, à côté du temps du bot. |

Les « et si » ajoutent deux parties d'essai par palier : `-Petsi=false` les coupe et va environ deux fois plus vite.

**Les limites.** Le temps gagné reste une borne haute (tout le multiplicateur gagné est compté comme de la vitesse), et ne regarde que le palier suivant : un achat sous ×1 peut servir plus loin. Le « pourquoi » d'un automatisme coupé lit la situation (la visée, le goulot, le défi), pas la règle exacte de la politique qui l'a coupé.

**Le moteur se rejoue à l'identique.** Deux parties analystes jouées pareil finissaient à 76:06:20 et 76:12:25. La cause : certaines copies de l'état (`Map.copyOf`, `Set.copyOf`) se parcouraient dans un ordre tiré au sort à chaque lancement de Java, et le rassemblement automatique parcourt les molécules. Elles se parcourent maintenant dans l'ordre alphabétique (`OrdreTest`).

**Plus rapide.** Les « et si » jouent leurs copies au pas d'une minute, les deux en même temps. Le moteur lit le nombre d'exemplaires de chaque élément dans un tableau plutôt que dans une table triée (c'était sa première dépense), et les photos du bot n'écrivent plus l'historique des statistiques. Sur dix heures de jeu sans « et si » : 915 s de calcul avant, 708 s après, pour une partie identique à l'octet près.

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
