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

**Attention :** les modes historiques ne sont pas des humains. L'ancien défaut de fusion et de synthèse après le Big Bang, qui bloquait les profils 11, 12 et 17, est corrigé depuis le 19 octobre 2026 (voir README, « Les correctifs des bots »).
