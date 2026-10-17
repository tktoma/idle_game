# Registre de validation des expériences

## Exécutions réellement effectuées

- **50/50** premières explosions sur des graines distinctes avec `RobotPhase3`.
- **12/12** robots V2 avec une seule graine, **9 victoires** sur la dernière portion (7BB → univers).
- **30/30** essais rapides (6 robots × 5 graines) atteignant la **galaxie**.
- **8/8** comparaisons Java A/B (2 graines × 4 versions), victoires confirmées.
- **3** états bloqués à 72 h reproduits ; **6** branches contrôle/conseil (+48 h), **3** branches avec coach initial et **3** avec coach corrigé (+12 h).
- **2** victoires de secours validées après rechargement : IDs 12 et 17.
- **1** audit de rentabilité locale et **1** extraction du catalogue de dépendances réels.
- **5** scénarios hors ligne validés sur une fenêtre de 4 h, sauvegarde avancée, taux hors ligne déjà à 100 %.
- **Compilation des 12 fichiers `code/*.java`** : validée avec Java 21 et classes du moteur.
- **Comparaison de régression A/B**, même graine : le programme retourne `PASS`.

## Préparé uniquement (aucun résultat à inventer)

- 24 × 20 graines (480 parties), script `lancer_campagne.py --tier standard` ; **non exécuté**.
- 24 × 50 graines (1 200 parties), script `lancer_campagne.py --tier approfondi` ; **non exécuté**.
- Optimiseur exhaustif sur tout le jeu de zéro à univers : **non construit**. Les politiques V2 sont des heuristiques de planification.
- Journalisation des vrais clics JavaFX / playtests humains : **non réalisée**.

## Vérification de deux victoires de secours

```text
VICTORY_VERIFIED,timeHours=190.261111139,bigbangs=8,cosmos=3,molecules=532039222,substances=353,assemblies=58,bodies=35,mature=35,systems=3,achievements=57,elements=118
VICTORY_VERIFIED,timeHours=190.588888917,bigbangs=8,cosmos=3,molecules=532066906,substances=359,assemblies=58,bodies=35,mature=35,systems=3,achievements=57,elements=118
```

La valeur `timeHours` est le **temps de jeu cumulé dans la sauvegarde chargée**, et **non** la durée supplémentaire de secours (+2 h 21 / +2 h 41). Il faut la distinguer du chronomètre externe expérimental.

## Références et biais de mesure

- **Moteur :** code compilé depuis l'archive initiale, non synchronisé automatiquement avec le dépôt GitHub.
- **Documents :** PDF utilisateur de 107 pages (versions successives), dans `DOCUMENTATION/`.
- **Première explosion :** état neuf + pas de la politique de `RobotPhase3`.
- **Acte cosmique :** état à 7 Big Bangs + pas de 20 secondes en règle générale.
- **Échantillonnage :** mesures temporelles et évènements macro, pas une mesure des émotions / perceptions.
- **Ancien robot :** bug de désactivation de fusion/synthèse après 8 BB, corrigé dans `CorrectedCoach.java`; cela rend trompeuse une lecture directe des 3 « perdants » historiques.
- **Compatibilité :** Java 21 vérifié dans l'environnement ; la partie JavaFX graphique n'a pas été lancée ici.
