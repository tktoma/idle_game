# Idle Game — Tous les robots et laboratoire de tests

> Kit autonome : scripts de simulation, **24 profils historiques**, **12 politiques/ablations V2**, robots expérimentaux des phases précédentes, moteur Java 21, sauvegardes et résultats CSV.
> Le jeu GUI JavaFX n'est pas lancé : les robots utilisent directement le moteur Java. Aucun fichier de ton dépôt GitHub n'est modifié.

## Sommaire

| Dossier | Contenu |
|---|---|
| `bots/v2/` | 12 classes Java de laboratoire (AdvancedLab, CorrectedCoach, audits, RobotPhase3, VerifyVictory...). Les **12 modes** de AdvancedLab sont configurés par argument, pas 12 classes indépendantes. |
| `bots/profils_24/` | 24 profils disponibles dans `IdleProfiles24.java` + `FreshPhaseProfiles.java` (début). |
| `bots/historique/` | ~30 anciens robots, diagnostics et sondes historiques. Certaines versions ont des points d'entrée expérimentaux. |
| `moteur/src` + `moteur/classes` | Source et binaire **de la version archivée** du moteur Java. |
| `sauvegardes/reference` | Partie avant le 8e Big Bang, partie avant/après le Big Bang, univers terminé. |
| `sauvegardes/profils_bloques` | Trois checkpoints à 72 h pour les profils 11, 12 et 17. |
| `donnees/` | Données brutes des expériences (CSV), sans retraitement. |
| `docs/` | Rapport consolidé, conclusions et limites des anciennes campagnes. |
| `sorties/` | Résultats des nouveaux lancements (créés lors des tests). |

## Prérequis

- **Java JDK 21** : les commandes `java -version` et `javac -version` doivent fonctionner.
- **Python 3.10+** pour `run.py` (bibliothèque standard seulement). Aucun accès Internet ni Gradle requis.
- RAM recommandée : 2 Go pour une simulation, davantage si plusieurs robots simultanés.

Sous Windows, extraire le ZIP dans un dossier sans caractères spéciaux, par exemple `C:\idle_bots`, et lancer **PowerShell** dans ce dossier. Sous Linux/macOS, ouvrir un terminal dans la racine du dossier.

## Démarrage rapide : vérification de la victoire

```bash
python run.py build
python run.py verify --save sauvegardes/reference/checkpoint_universe_5s.save
```

Sur certaines machines, Python s'appelle `py` ou `python3` : remplacer `python` dans les exemples. Le premier lancement `build` recompilera tous les bots modernes à partir des sources.

### Bot V2 planificateur (partie avancée)

```bash
python run.py v2 --bot DEPENDANCES --hours 12 --step 20 --seed 42
```

Le programme commence au checkpoint **à sept Big Bangs**. Il ne recommence pas une nouvelle partie ; les heures sont **supplémentaires depuis ce checkpoint**. Les fichiers `timeline.csv`, `events.csv`, `result.csv` et éventuellement `winner.save` apparaissent sous `sorties/v2_.../`.

Autres modes : `EXPERT_COMPLET`, `ECONOMISTE`, `PLAN_PRESTIGE`, `ADAPTATIF`, `DEBUTANT_APPRENANT`, `GUIDE_UI`, `RECUPERATION`, `SANS_IMPULSIONS`, `SANS_COMETES`, `SANS_TECH`, `SANS_LISTE`.

### Les 24 profils historiques : fin cosmique

```bash
python run.py profils --id 1 --hours 72 --step 20 --seed 42
python run.py profils --id 19 --hours 336 --step 20 --seed 42
```

`id=1` est l'optimiseur historique ; `id=19` le joueur en micro-sessions (5 minutes actives par heure). Tous les 24 IDs sont décrits dans `PROFILS_24.md`.

**Avertissement critique :** cette famille de robots comportait un bug d'automatisation : après le 8e Big Bang, fusion/synthèse pouvaient rester coupées. Ne pas interpréter l'absence de victoire comme un bug du jeu. Préférer `CorrectedCoach` pour diagnostiquer les checkpoints bloqués.

### Les 24 profils sur une partie neuve

```bash
python run.py fresh --id 1 --hours 4 --step 20 --seed 42
```

`fresh` part d'un `GameState` vide. Cette famille de robots ne sait pas planifier **tous** les défis / conditions du Big Bang : elle sert au benchmark du début, pas à prouver une partie A → Z.

### Sauvetage des profils bloqués

```bash
python run.py coach --checkpoint sauvegardes/profils_bloques/profil_12_72h.save --hours 12 --seed 42
python run.py coach --checkpoint sauvegardes/profils_bloques/profil_17_72h.save --hours 12 --seed 42
```

Le coach corrigé a atteint l'univers en +2 h 21 et +2 h 41 sur les checkpoints 12 et 17, respectivement. Le bot 11 (prestige) requiert un correctif spécial de priorité pour atteindre le 8e Big Bang ; l'univers n'a pas été démontré dans ce test de secours.

### Première explosion / hasard

```bash
python run.py phase3 --explosions 1 --hours 8 --seed 42
```

Cet ancien simulateur génère des logs en sortie standard et utilise la stratégie correspondante, pas nécessairement celle d'AdvancedLab.

## Lancer de longues campagnes

Le lanceur d'origine `scripts_originals/lancer_campagne.py` attend son dossier historique spécifique. Ici, la version **portable** est `run.py` :

```bash
# 5 graines, 6 bots V2, jusqu'à la galaxie : 30 runs
python run.py batch --tier rapide --workers 2

# 24 profils x 20 graines (480 runs), NON exécutés dans le laboratoire historique
python run.py batch --tier standard --workers 2

# 24 profils x 50 graines (1 200 runs), NON exécutés historiquement
python run.py batch --tier approfondi --workers 2
```

`--hours`, `--step`, `--seed-start` permettent de changer l'horizon, la discrétisation et les graines. Le lancer peut prendre des heures ou plus selon CPU/stratégie. Les résultats existants sont réutilisés par défaut pour chaque job ; `--replay` force le recalcul. Garder moteur, seed, checkpoint, durée, step et politique fixes pour comparer deux patchs.

## Deux systèmes de sauvegarde et de temps à distinguer

- `heures actives` : profil présent devant le jeu.
- `heures calendaires` : durée totale dans la simulation, absences comprises.
- `heures en jeu` : temps conservé dans la sauvegarde, pouvant différer du temps supplémentaire d'expérience.
- Les benchmarks cosmiques démarrent **à 7 BB**, les essais du début **depuis zéro**. Ne pas les additionner comme une mesure d'une seule partie.
- `victoire=0` au bout de 12, 72 ou 336 heures veut dire **non atteint dans la fenêtre**, pas impossible.

## Compatibilité des vieux bots

`bots/historique/` contient les fichiers originaux des expériences RobotPhase et Probe. Ils ont été conçus à des dates / API différentes : ils sont conservés comme sources reproductibles, mais **leur compilation groupée n'est pas garantie**. Pour les relancer, compiler une classe ciblée contre le moteur archivé dans un dossier de sortie isolé, et adapter l'API si cette classe est issue d'une version plus ancienne.

Les dossiers `bots/v2` et `bots/profils_24` sont compilés ensemble par `python run.py build` et ont été testés dans cet environnement.

## Fichiers de résultats et contrôle de fiabilité

Les CSV déjà mesurés dans `donnees/` sont des données de tests, pas des données de vrais utilisateurs. Les campagnes A/B ne portent que sur **2 graines** et les benchmarks 480/1 200 sont seulement préparés. La phase de 50 graines porte uniquement sur la **première explosion**.

Les commandes de vérification renvoient un échec si Java, un checkpoint ou une source manque. Aucune installation en ligne n'est nécessaire. Les fichiers `.save` sont générés par le jeu dans son format natif.

## Rapport consolidé et graphiques

- `docs/RAPPORT_CONSOLIDE_IDLE_GAME.pdf` : **rapport de 25 pages**, 25 graphiques et tableaux des campagnes.
- `docs/RAPPORT_CONSOLIDE_IDLE_GAME.docx` : même rapport, modifiable dans Word.
- `docs/graphiques/` : PNG de chaque graphique (25 fichiers).
- `docs/DOCUMENTATION_JEU_107_PAGES.pdf` : documentation initiale fournie pour le contexte.
- `sauvegardes/victoires_historiques/` : **21 sauvegardes**, correspondant aux 21 victoires historiques à 14 jours maximum.

## Tableaux Excel déjà constitués

Le sous-dossier `docs/tableaux_excel/` conserve les trois classeurs historiques (24 profils globaux, laboratoire V2 et prolongations 14 jours). Ils complètent les CSV bruts et le rapport consolidé.
