# Contrôles effectués le 17 octobre 2026

- `python run.py build` : **13 fichiers Java compilés**, exit 0, JDK 21.
- `python run.py verify --save sauvegardes/reference/checkpoint_universe_5s.save` : `VICTORY_VERIFIED` confirmé (8 BB, 35 astres mûrs, 3 systèmes, 3 formations cosmiques).
- `python run.py v2 --bot DEPENDANCES --hours 0.016667 --step 20 --seed 42` : exécution OK.
- `python run.py fresh --id 1 --hours 0.016667 --step 20 --seed 42` : exécution OK.
- `python run.py profils --id 19 --hours 0.016667 --step 20 --seed 42` : exécution OK.
- `python run.py coach --checkpoint sauvegardes/profils_bloques/profil_12_72h.save --hours 0.016667 --seed 42` : exécution OK.
- `python run.py batch --tier rapide --workers 2 --hours 0.016667 --seed-start 901` : 30 sous-tests ont terminé sans code d'erreur Java.

Ces tests ne reproduisent **pas** les 12/72/336 heures complètes : ils vérifient le **fonctionnement du kit portable et des points d'entrée**. Les résultats longs se trouvaient dans `donnees/` (retiré du dépôt le 20 octobre 2026, voir plus bas) et provenaient des campagnes antérieures. (`run.py coach`, cité ci-dessus, a été retiré en 0.141 avec `CorrectedCoach`.)

## Vérification historique supplémentaire

Les **21 sauvegardes victorieuses historiques** du dossier `sauvegardes/victoires_historiques` ont été rechargées avec `VerifyVictory` : **21/21 vérifiées**, Java 21. Les résultats individuels étaient sous `docs/VALIDATION_21_SAUVEGARDES.csv` (dossier retiré du dépôt le 20 octobre 2026 ; `BotsTest` refait cette vérification à chaque envoi).

# Contrôles après la fusion dans le projet (19 octobre 2026)

Le kit est devenu le module `idle/bots`. Il est compilé contre le cœur du jeu en 0.81, et la copie du moteur a disparu.

- `python run.py build` : 75 sources du moteur, puis 13 bots compilés.
- `sh TEST_RAPIDE.sh` : tout a réussi (vérification de victoire, bot V2, profil de début).
- `run.py v2`, `profils`, `fresh` et `coach` sur 3 minutes de jeu : code de sortie 0 partout.
- `BotsTest` (4 tests : sauvegardes, victoires, bot V2, profils) : 4/4 réussis.
- Mêmes valeurs qu'avant la fusion :

  | Essai | Résultat |
  |---|---|
  | `DEPENDANCES`, graine 42 | 7 h 38 |
  | `DEPENDANCES`, graine 43 | 7 h 39 |
  | Profil 1 | 8 h 17 |
  | Profil 21 | 145 h 09 |
  | Profil 22 | 145 h 51 |

# Correctifs des bots et optimisation (19 octobre 2026, suite)

- Trois correctifs dans `RobotCosmosPlanner` (fusion rallumée, matière confiée à la création automatique, absence plafonnée à 24 h). Avec eux, les 9 profils relancés atteignent l'univers, et non plus 6 sur 9 (voir le README).
- Optimisations du moteur (0.82) :
  - bonus des générateurs lus dans un tableau ;
  - compte des succès gardé en mémoire.

  `python run.py empreinte` donne des sauvegardes identiques avant et après pour les 4 parties.
- `BotsTest` : 4/4. Les 869 tests du jeu passent.

# Contrôles de la version 0.126 (23 octobre 2026)

Ce fichier est un journal daté : les chiffres des sections précédentes sont ceux de leur jour (13 bots, 75 sources du
moteur, 869 tests…). Les dossiers `donnees/` et `docs/` qu'elles citent ont été retirés du dépôt le 20 octobre 2026 ;
les 21 victoires historiques se revérifient avec `BotsTest` (ou `python run.py verify`).

Gradle ne pouvait pas tourner (pas d'accès à services.gradle.org) : tout a été compilé avec `javac` (JDK 21) et lancé
avec la console JUnit 5, JavaFX 11 pour l'interface, sous Xvfb.

- Moteur : 946 tests, tous réussis (932 avant la 0.126, plus ceux de `CoherenceTest`).
- Interface : 9 tests, tous réussis (dont `GuiInteractionsTest`, de nouveau vert : la copie de la 0.125 avait effacé le
  paiement des paliers dans son test, et il échouait sur `master`).
- Bots : 34 tests, tous réussis (33 avant la 0.126, plus la victoire écrite par le moteur actuel), hors campagnes des
  24 profils (comme le job CI général).
- Campagne longue (`Profiles24LongCampaignTest`, profils 1, 11, 12 et 17 depuis la partie à 7 Big Bangs) : elle échouait
  déjà sur `master` en 0.125, et donne les mêmes chiffres, au bit près, avec ou sans les correctifs. Le profil 17 (AFKOpen,
  une décision par heure) n'avait que 34 astres mûrs sur 35 après 24 h : depuis le rééquilibrage de la 0.125, il forme la
  galaxie vers 26 h (l'amas vers 36 h, l'univers vers 42 h). Il a maintenant 30 h, avec le même jalon ; le test passe.
- Les 24 sauvegardes de victoire passent `VerifyVictory`. Une victoire écrite par le moteur actuel aussi : l'ancienne
  `VerifyVictory` la refusait (« Galaxie absente »), puisque former l'univers remet tout à zéro.
- Référence de l'équilibrage, `FreshUniverseAudit` profil 1 (Speedrunner), graine 201, une décision toutes les 20 s :
  univers formé en **27 h 23**, comme en 0.125. Les correctifs ne changent pas le premier univers.
