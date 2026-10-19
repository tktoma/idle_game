# Contrôles effectués le 17 octobre 2026

- `python run.py build` : **13 fichiers Java compilés**, exit 0, JDK 21.
- `python run.py verify --save sauvegardes/reference/checkpoint_universe_5s.save` : `VICTORY_VERIFIED` confirmé (8 BB, 35 astres mûrs, 3 systèmes, 3 formations cosmiques).
- `python run.py v2 --bot DEPENDANCES --hours 0.016667 --step 20 --seed 42` : exécution OK.
- `python run.py fresh --id 1 --hours 0.016667 --step 20 --seed 42` : exécution OK.
- `python run.py profils --id 19 --hours 0.016667 --step 20 --seed 42` : exécution OK.
- `python run.py coach --checkpoint sauvegardes/profils_bloques/profil_12_72h.save --hours 0.016667 --seed 42` : exécution OK.
- `python run.py batch --tier rapide --workers 2 --hours 0.016667 --seed-start 901` : 30 sous-tests ont terminé sans code d'erreur Java.

Ces tests ne reproduisent **pas** les 12/72/336 heures complètes : ils vérifient le **fonctionnement du kit portable et des points d'entrée**. Les résultats longs se trouvent dans `donnees/` et proviennent des campagnes antérieures.

## Vérification historique supplémentaire

Les **21 sauvegardes victorieuses historiques** du dossier `sauvegardes/victoires_historiques` ont été rechargées avec `VerifyVictory` : **21/21 vérifiées**, Java 21. Résultats individuels sous `docs/VALIDATION_21_SAUVEGARDES.csv`.

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
