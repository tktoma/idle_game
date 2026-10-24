# Les 24 profils historiques

Les IDs sont transmis avec `python run.py profils --id N ...` ou `python run.py fresh --id N ...` (l'ID est obligatoire).

Le **nom** est celui du code (`IdleProfiles24.PROFILES`), des sauvegardes (`victoires_historiques/01_Speedrunner_winner.save`…)
et des journaux : c'est lui qu'emploient les autres documents. La **description** le dit en français.

| ID | Nom | Description | Comportement |
|---:|---|---|---|
| 1 | Speedrunner | Optimiseur | Intervient toutes les 20 s, achats immédiats, deux passages éventuels sur les recettes; jeu toujours ouvert. |
| 2 | Veteran | Vétéran du genre | Intervient toutes les 20 s, suit presque toujours les objectifs et exploite les techniques. |
| 3 | Regular | Joueur régulier | Intervient en moyenne à la minute, suit les objectifs la plupart du temps et gère ses achats sans optimisation parfaite. |
| 4 | Methodical | Méthodique prudent | Décisions espacées de deux minutes, planifie soigneusement mais dépense moins volontiers. |
| 5 | AchievementHunter | Chasseur de succès | Interventions à 40 s, priorité générale à la progression ; les succès à faire exprès sont visés par `RobotCosmosPlanner.hunt()` (0.86), comme pour tous les profils. |
| 6 | Collector | Collectionneur | Crée parfois des molécules hors recette avant de traiter sa liste de besoins; cherche à compléter le catalogue. |
| 7 | Chemist | Chimiste | Donne plus d’effort aux recettes et peut traiter la liste de besoins deux fois à une décision. |
| 8 | Astronomer | Astronome | Revient régulièrement sur les systèmes solaires et les prérequis des astres. |
| 9 | Explorer | Explorateur technologique | Traite très souvent le collisionneur et les sondes, parfois au détriment des autres objectifs. |
| 10 | PassiveOptimist | Automatisation d’abord | Privilégie les automatismes, y compris l’automatisation moléculaire, et n’intervient que toutes les deux minutes. |
| 11 | PrestigeAddict | Fan de prestige | Cherche à déclencher les explosions accessibles, joue activement toutes les 40 secondes. |
| 12 | Hoarder | Accumulateur / avare | Évite souvent les achats et retarde les dépenses, même si les ressources sont présentes. |
| 13 | BulkBuyer | Acheteur en lots | Attend cinq minutes puis effectue de grosses séries d’améliorations. |
| 14 | OneClick | Cliqueur occasionnel | Interventions à la minute, traite une fraction des achats possibles à chaque passage (approximation des petits achats). |
| 15 | RandomClicker | Cliqueur désordonné | Interventions fréquentes, achète souvent mais suit rarement les recettes/objectifs explicites. |
| 16 | Beginner | Débutant | Interventions toutes les trois minutes, faible suivi des objectifs, achats irréguliers. |
| 17 | AFKOpen | AFK jeu ouvert | Laisse le jeu tourner au premier plan, ne revient faire des décisions qu’une fois par heure. |
| 18 | BackgroundWorker | Multitâche sur PC | Laisse le jeu ouvert et interagit avec lui environ toutes les dix minutes. |
| 19 | MobileSnacker | Micro-sessions | Ouvre le jeu cinq minutes par heure, puis le ferme cinquante-cinq minutes. |
| 20 | LunchBreak | Pause déjeuner | Ouvre le jeu trente minutes toutes les quatre heures. |
| 21 | EveningPlayer | Joueur du soir | Joue deux heures chaque jour; le reste du temps est une absence à progression réduite. |
| 22 | WeekendPlayer | Joueur de week-end | Joue quatre heures puis s’absente jusqu’au prochain créneau de quarante-huit heures. |
| 23 | Forgetful | Joueur distrait | Ouvre le jeu vingt minutes par heure, manque souvent les occasions d’agir. |
| 24 | ReturningVeteran | Expert intermittent | Joue quinze minutes toutes les trois heures; stratégies précises pendant ses connexions. |

## Le deuxième univers et le ciel (version 0.131)

Dans le deuxième univers, chaque profil fait ses propres choix (`IdleProfiles24.STYLES`, lu par `NextUniversePolicy`) :

- **Cycle** : son choix de cycle à chaque Big Bang (fusion +25 % d'atomes, synthèse −20 % sur son prix, réserve
  +20 % de matière noire ; « au hasard » : tiré à chaque cycle).
- **Régime** : la chance, à chaque décision où il est prêt, de relancer le régime rapide (0 % : il n'y pense jamais).
- **Choix de cycle** : la chance de faire ce choix dans un cycle ; sinon, il l'oublie jusqu'au Big Bang suivant.
- **Famille préférée** : il la fait suivre la famille qui manque aux recettes, la choisit une fois pour toutes, ou
  ne s'en sert pas.
- **Éléments offerts** : la chance de prendre une fournée (sinon il la laisse passer), et lequel il prend (le plus
  utile : celui qui manque à la liste, sinon le plus lourd ; le premier : le plus léger pas encore découvert).
- **Anomalies du ciel** (les deux univers) : à chaque décision où une anomalie est visible, la chance de la saisir,
  qui est son attention au ciel.

Le Speedrunner et le Vétéran revenant jouent en experts (tout, toujours, au mieux), sans tirer de hasard pour cela.
Les choix des autres viennent de leur caractère, pas d'une mesure (voir le README pour les durées).

| ID | Nom | Cycle | Régime | Choix de cycle | Famille préférée | Éléments offerts | Anomalies du ciel | Pourquoi |
|---:|---|---|---|---|---|---|---|---|
| 1 | Speedrunner | fusion | 100 % | 100 % | suit ce qui manque | 100 %, le plus utile | 100 % | l'expert |
| 2 | Veteran | fusion | 95 % | 100 % | suit ce qui manque | 100 %, le plus utile | 100 % | presque l'expert |
| 3 | Regular | fusion | 70 % | 90 % | suit ce qui manque | 90 %, le plus utile | 90 % | le bon choix, pas toujours le geste |
| 4 | Methodical | réserve | 90 % | 100 % | suit ce qui manque | 100 %, le plus utile | 100 % | l'économe planifie et met en réserve |
| 5 | AchievementHunter | fusion | 80 % | 90 % | suit ce qui manque | 90 %, le plus utile | 90 % | attentif, sans tout optimiser |
| 6 | Collector | synthèse | 60 % | 90 % | suit ce qui manque | 100 %, le plus utile | 90 % | des éléments pour ses molécules |
| 7 | Chemist | synthèse | 80 % | 100 % | suit ce qui manque | 100 %, le plus utile | 100 % | la synthèse |
| 8 | Astronomer | synthèse | 60 % | 90 % | une fois | 90 %, le plus utile | 100 % | les molécules du ciel |
| 9 | Explorer | fusion | 60 % | 90 % | une fois | 90 %, le plus utile | 100 % | ses gestes vont aux sondes |
| 10 | PassiveOptimist | réserve | 0 % | 90 % | une fois | 90 %, le plus utile | 80 % | compte sur les automatismes, jamais de régime (geste répété à la main) |
| 11 | PrestigeAddict | réserve | 80 % | 100 % | une fois | 90 %, le plus utile | 85 % | plus de matière noire par explosion |
| 12 | Hoarder | réserve | 40 % | 80 % | une fois | 100 %, le plus utile | 85 % | la réserve ; ce qui est gratuit, il le prend |
| 13 | BulkBuyer | fusion | 50 % | 80 % | une fois | 100 %, le plus utile | 90 % | passe rarement : le régime une fois sur deux |
| 14 | OneClick | fusion | 50 % | 80 % | une fois | 80 %, le premier | 75 % | prend le premier élément de la liste |
| 15 | RandomClicker | au hasard | 45 % | 70 % | — | 70 %, au hasard | 45 % | au hasard |
| 16 | Beginner | fusion | 0 % | 60 % | — | 60 %, le premier | 55 % | le premier choix, oublie le régime |
| 17 | AFKOpen | réserve | 0 % | 100 % | une fois | 100 %, le plus utile | 70 % | une visite par heure, le régime ne sert à rien |
| 18 | BackgroundWorker | réserve | 10 % | 90 % | une fois | 90 %, le plus utile | 80 % | une décision toutes les dix minutes : le régime, rarement |
| 19 | MobileSnacker | fusion | 50 % | 90 % | une fois | 90 %, le plus utile | 85 % | cinq minutes par heure |
| 20 | LunchBreak | fusion | 60 % | 90 % | suit ce qui manque | 90 %, le plus utile | 90 % | des sessions de trente minutes bien remplies |
| 21 | EveningPlayer | fusion | 90 % | 100 % | suit ce qui manque | 100 %, le plus utile | 100 % | deux heures d'expert par jour |
| 22 | WeekendPlayer | fusion | 70 % | 90 % | suit ce qui manque | 90 %, le plus utile | 85 % | longues sessions, un peu moins appliqué |
| 23 | Forgetful | synthèse | 20 % | 50 % | — | 50 %, le premier | 55 % | oublie souvent |
| 24 | ReturningVeteran | fusion | 100 % | 100 % | suit ce qui manque | 100 %, le plus utile | 100 % | l'expert, peu présent |

**Attention :** les modes historiques ne sont pas des humains. L'ancien défaut de fusion et de synthèse après le Big Bang, qui bloquait les profils 11, 12 et 17, est corrigé depuis le 19 octobre 2026 (voir README, « Les correctifs des bots »).
