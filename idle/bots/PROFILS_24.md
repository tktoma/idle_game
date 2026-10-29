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

## Les trois actes selon la personnalité (version 0.135)

Dès le premier univers, et dans chaque univers ensuite, chaque profil fait ses propres choix dans les trois actes
(`IdleProfiles24.EARLY_STYLES`) :

- **Achats en atomes** : l'ordre d'ouverture mesuré du modèle (§14.1, avec Persistance au bon moment), ou
  l'amélioration la moins chère qu'il peut payer.
- **Arbre de matière noire** : au mieux (liaisons et nouveaux nœuds d'abord), ou la case la moins chère, comptée
  en part de ce qu'il a dans la monnaie de la case. Dans les deux cas, la matière noire du Big Bang se garde (le
  double pendant la barre haute : depuis la 0.141 pour l'expert aussi, qui gardait la constante de 500) et un niveau
  répétable ne prend pas plus de 5 % de la matière noire (1 % de sa branche dans l'arbre). Chez le joueur « le moins
  cher » seulement, les cases payées en atomes ou en particules attendent pendant la préparation du dernier Big Bang ;
  l'expert les prend quand il le peut. Le succès « compte juste » se prend quand l'occasion passe, dans les deux cas.
- **Défis de Big Bang** : tentés ou ignorés. Dans le premier univers, dans l'ordre du catalogue et jamais sur une
  partie prête à Big Bang ; à partir du deuxième, aussi quand le Big Bang est prêt, le ciel figé et la barre haute
  d'abord (ils ne remettent rien à zéro).
- **Sondes** : la durée du voyage réglée sur le temps jusqu'à son retour (le plus long voyage qui revient avant
  lui, en durée réelle, moteurs et bonus compris), ou les sauts courts. Un joueur toujours là qui décide souvent
  garde donc les sauts courts : il revient avant la fin d'un voyage d'une heure.

Le Speedrunner joue comme avant (l'expert). Le rythme (attention, décisions, présence) reste celui de la table du haut.

| ID | Nom | Atomes | Arbre | Défis de Big Bang | Sondes | Pourquoi |
|---:|---|---|---|---|---|---|
| 1 | Speedrunner | ordre du modèle | au mieux | oui | courts | l'expert, toujours là |
| 2 | Veteran | ordre du modèle | au mieux | oui | selon son absence |  |
| 3 | Regular | ordre du modèle | au mieux | oui | selon son absence |  |
| 4 | Methodical | ordre du modèle | au mieux | oui | selon son absence |  |
| 5 | AchievementHunter | ordre du modèle | au mieux | oui | selon son absence | les défis de Big Bang donnent des succès |
| 6 | Collector | ordre du modèle | la moins chère | oui | selon son absence | prend toutes les cases, la moins chère d'abord |
| 7 | Chemist | ordre du modèle | au mieux | oui | selon son absence |  |
| 8 | Astronomer | ordre du modèle | au mieux | oui | selon son absence |  |
| 9 | Explorer | ordre du modèle | au mieux | oui | selon son absence | ses sondes partent jusqu'à son retour |
| 10 | PassiveOptimist | le moins cher | la moins chère | non | selon son absence | compte sur les automatismes, pas d'effort de plus |
| 11 | PrestigeAddict | ordre du modèle | au mieux | oui | selon son absence |  |
| 12 | Hoarder | ordre du modèle | au mieux | non | selon son absence | ne risque pas un défi de Big Bang |
| 13 | BulkBuyer | le moins cher | la moins chère | oui | selon son absence | achète en séries, le moins cher d'abord |
| 14 | OneClick | le moins cher | la moins chère | non | courts | un achat, le moins cher |
| 15 | RandomClicker | le moins cher | la moins chère | non | courts |  |
| 16 | Beginner | le moins cher | la moins chère | non | courts | ne connaît ni l'ordre d'achat ni les défis de Big Bang |
| 17 | AFKOpen | ordre du modèle | au mieux | oui | selon son absence | planifie bien, une visite par heure |
| 18 | BackgroundWorker | ordre du modèle | au mieux | oui | selon son absence |  |
| 19 | MobileSnacker | ordre du modèle | au mieux | oui | selon son absence | sondes d'une heure avant de fermer le jeu |
| 20 | LunchBreak | ordre du modèle | au mieux | oui | selon son absence |  |
| 21 | EveningPlayer | ordre du modèle | au mieux | oui | selon son absence | sondes en expédition pour la nuit |
| 22 | WeekendPlayer | ordre du modèle | au mieux | oui | selon son absence |  |
| 23 | Forgetful | le moins cher | la moins chère | non | courts | oublie l'ordre et les sondes |
| 24 | ReturningVeteran | ordre du modèle | au mieux | oui | selon son absence | l'expert, peu présent |

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
Leur cycle, la réserve, vient d'une mesure (0.135 : elle bat la fusion sur les quatre parties essayées) ; le Vétéran,
le Régulier et les joueurs à sessions appliqués la prennent aussi. Les choix des autres viennent de leur caractère,
pas d'une mesure (voir le README pour les durées).

| ID | Nom | Cycle | Régime | Choix de cycle | Famille préférée | Éléments offerts | Anomalies du ciel | Pourquoi |
|---:|---|---|---|---|---|---|---|---|
| 1 | Speedrunner | réserve | 100 % | 100 % | suit ce qui manque | 100 %, le plus utile | 100 % | l'expert |
| 2 | Veteran | réserve | 95 % | 100 % | suit ce qui manque | 100 %, le plus utile | 100 % | presque l'expert |
| 3 | Regular | réserve | 70 % | 90 % | suit ce qui manque | 90 %, le plus utile | 90 % | le bon choix, pas toujours le geste |
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
| 20 | LunchBreak | réserve | 60 % | 90 % | suit ce qui manque | 90 %, le plus utile | 90 % | des sessions de trente minutes bien remplies |
| 21 | EveningPlayer | réserve | 90 % | 100 % | suit ce qui manque | 100 %, le plus utile | 100 % | deux heures d'expert par jour |
| 22 | WeekendPlayer | réserve | 70 % | 90 % | suit ce qui manque | 90 %, le plus utile | 85 % | longues sessions, un peu moins appliqué |
| 23 | Forgetful | synthèse | 20 % | 50 % | — | 50 %, le premier | 55 % | oublie souvent |
| 24 | ReturningVeteran | réserve | 100 % | 100 % | suit ce qui manque | 100 %, le plus utile | 100 % | l'expert, peu présent |

## Le vide du troisième univers (version 0.133)

Le vide se propose au premier Big Bang du troisième univers. Chaque profil le joue à sa façon
(`IdleProfiles24.VOID_STYLES`, lu par `VoidPolicy`) :

- **Ouverture** : la chance, à chaque décision où il est payable, de l'ouvrir. Tant qu'il se propose, le
  collisionneur garde son énergie pour lui (achats et automates en attente, deux heures au plus).
- **Part** : la part du collisionneur et de l'espace libre laissée au vide (25 % : le réglage du jeu, que gardent
  ceux qui n'y touchent pas).
- **Achats** : au mieux (d'abord maturation, rassemblement et allègement, qui raccourcissent le troisième acte ; une
  amélioration de croissance seulement si elle est bon marché à côté, ou si le prochain raccourci est hors de portée),
  le moins cher, ou au hasard ; et la chance de passer par le vide à une décision pour y acheter.
- **Trou noir** : la chance de lui sacrifier les particules, une fois les huit Big Bangs faits et quand sa masse y
  gagne (0 % : jamais).

Les profils 1, 17, 21 et 24 jouent le vide en experts, sans tirer de hasard pour cela.

| ID | Nom | Ouverture | Part | Achats | Passage | Trou noir | Pourquoi |
|---:|---|---|---|---|---|---|---|
| 1 | Speedrunner | 100 % | 50 % | au mieux | 100 % | 100 % | l'expert |
| 2 | Veteran | 100 % | 50 % | au mieux | 95 % | 100 % | presque l'expert |
| 3 | Regular | 90 % | 25 % | au mieux | 70 % | 80 % | garde le réglage du jeu |
| 4 | Methodical | 100 % | 50 % | au mieux | 90 % | 100 % | planifie |
| 5 | AchievementHunter | 90 % | 25 % | le moins cher | 80 % | 100 % | achète tout ce qui s'offre |
| 6 | Collector | 90 % | 25 % | le moins cher | 80 % | 80 % | le moins cher, pour tout avoir |
| 7 | Chemist | 90 % | 25 % | au mieux | 80 % | 80 % |  |
| 8 | Astronomer | 100 % | 50 % | au mieux | 90 % | 100 % | ce qui raccourcit le ciel d'abord |
| 9 | Explorer | 100 % | 75 % | au mieux | 90 % | 90 % | le collisionneur et le vide sont son terrain |
| 10 | PassiveOptimist | 80 % | 25 % | le moins cher | 50 % | 30 % | peu de gestes |
| 11 | PrestigeAddict | 100 % | 50 % | le moins cher | 80 % | 100 % |  |
| 12 | Hoarder | 50 % | 25 % | au mieux | 30 % | 0 % | hésite à payer, ne sacrifie jamais ses particules |
| 13 | BulkBuyer | 80 % | 25 % | le moins cher | 50 % | 50 % | de grosses séries, rarement |
| 14 | OneClick | 70 % | 25 % | le moins cher | 60 % | 50 % |  |
| 15 | RandomClicker | 60 % | 25 % | au hasard | 60 % | 50 % | au hasard |
| 16 | Beginner | 50 % | 25 % | le moins cher | 50 % | 20 % | le premier venu |
| 17 | AFKOpen | 100 % | 50 % | au mieux | 100 % | 100 % | une visite par heure, mais complète |
| 18 | BackgroundWorker | 90 % | 50 % | au mieux | 80 % | 80 % |  |
| 19 | MobileSnacker | 90 % | 25 % | le moins cher | 90 % | 80 % | vite, le moins cher |
| 20 | LunchBreak | 90 % | 50 % | au mieux | 90 % | 90 % |  |
| 21 | EveningPlayer | 100 % | 50 % | au mieux | 100 % | 100 % |  |
| 22 | WeekendPlayer | 90 % | 50 % | au mieux | 90 % | 90 % |  |
| 23 | Forgetful | 40 % | 25 % | le moins cher | 40 % | 30 % | oublie souvent |
| 24 | ReturningVeteran | 100 % | 50 % | au mieux | 100 % | 100 % | l'expert, peu présent |

## L'arbre, à partir de l'univers 4 (version 0.152)

À partir de trois univers formés, chaque profil joue aussi la graine, les bulles de l'arbre, les hadrons et la ligne
(`UniverseTreePolicy`, voir README, « Les univers 4 à 11 »). Pas de table à part : la façon de jouer se déduit du
profil (`IdleProfiles24.treeStyle`).

- **Soin** : la chance, à chaque décision, de s'occuper de l'arbre (arroser, acheter, lier) : 100 % pour le
  Speedrunner, sinon le sens de l'organisation du profil (`planning`, de 35 % pour le RandomClicker à 100 %).
- **Bulles** : dans l'ordre de l'expert pour ceux qui suivent l'ordre d'achat du modèle (les mêmes que « ordre
  d'ouverture » plus haut), la moins chère sinon (PassiveOptimist, BulkBuyer, OneClick, RandomClicker, Beginner,
  Forgetful).

Tous attendent le palier de la graine avant de lancer l'univers suivant (dès l'univers 4) ; personne n'achète la bulle
du bonsaï, à droite (voir README). Mesuré depuis zéro, une décision toutes les 20 s : le Speedrunner (graine 201) forme
son onzième univers à 82 h 45, le Regular (graine 203) à 104 h 08.

**Attention :** les modes historiques ne sont pas des humains. L'ancien défaut de fusion et de synthèse après le Big Bang, qui bloquait les profils 11, 12 et 17, est corrigé depuis le 19 octobre 2026 (voir README, « Les correctifs des bots »).
