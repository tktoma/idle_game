# Idle — recommandations d'équilibrage après les campagnes de tests

## Les constats les plus solides

| Question | Réponse mesurée | Ce qu'il faut faire |
|---|---|---|
| La première explosion est-elle stable ? | 50 graines : médiane 3 h 06, extrêmes 2 h 27 à 4 h 21 | Examiner l'ordre d'obtention des bonus rares, avant de toucher aux coûts |
| Quelle est la meilleure politique V2 observée ? | Planification des dépendances : univers en +7 h 38 depuis 7 BB | Consolider le planificateur multi-prérequis, sans prétendre à l'optimum |
| Raccourcir la maturation accélère-t-il le jeu ? | Non dans les 8 essais A/B locaux sur 2 graines | Ne pas réduire ce délai sur la seule base du benchmark |
| Les trois profils historiques sont-ils victimes d'un « softlock » moteur ? | Non démontré. Deux se terminent en +2 h 21 / +2 h 41 après transfert au coach corrigé | Corriger les robots et tester la compréhension des aides déjà présentes |
| La galaxie est-elle stable selon les graines ? | 6 profils × 5 graines : 30/30 galaxies, à ~4 h 51–4 h 56 | Élargir avant de conclure sur le hasard de l'acte cosmique |
| Quelle recette sert le plus de constructions ? | Eau : 27 dépendances directes, 50 descendants ; quartz : 17/45 | Vérifier que les achats automatiques préparent tôt ces ingrédients |

## Priorités de développement produit

1. **Alerte contextuelle de blocage** : après N minutes sans étape utile, afficher le premier prérequis encore contrôlable (et le lien vers sa carte), sans divulguer toute la stratégie. Les pastilles et la liste de courses existent déjà : s'en servir.
2. **Alerte « chimie impossible »** : si 8 Big Bangs ou plus, moins de 118 éléments disponibles et synthèse automatique désactivée, proposer de la réactiver. À tester d'abord sur le robot et sur des humains.
3. **Expliciter « 27/28 astres mûrs »** : indiquer lequel est disponible, en maturation, ou encore verrouillé, et pourquoi.
4. **Journaliser les décisions humaines** : clics, achats multiples, onglets, aides consultées, déblocages, temps entre deux découvertes, pour séparer les attentes normales de la confusion.
5. **Analyser la variance des éléments précoces** : l'écart de 1 h 54 entre graines extrêmes est suffisant pour mériter un examen, pas forcément un nerf.
6. **Installer le benchmark rapide après chaque patch** (6 × 5 graines), puis étendre à une cohorte plus grande si le résultat bouge.

## Ce que je ne changerais pas tout de suite

- Ne pas ajouter une deuxième liste de courses : le jeu en a déjà une.
- Ne pas réduire d'office les durées de galaxie/amas/univers : deux tests A/B de maturation n'ont pas accéléré le robot testé.
- Ne pas déduire « 21/24 joueurs gagneront » des 24 robots : ils ne constituent pas un échantillon humain représentatif et un de leurs scripts avait un défaut.
- Ne pas assimiler les appels d'impulsion automatisés du benchmark à des clics manuels d'utilisateurs.

## Informations manquantes pour une évaluation définitive

- Une partie complète de zéro à l'univers **par robot stable**, sans changer sa stratégie entre les actes.
- Un test différentiel du pas de simulation (0,1 s, 1 s, 20 s) sur les états de prestige et de chimie.
- Un échantillon de playtests humains enregistrés (5 à 10 volontaires minimum pour des observations qualitatives).
- Les 480/1 200 parties multi-graines, **préparées mais non exécutées** à ce jour.
- Une métrique réelle du temps de microgestion côté interface JavaFX.

**Décision recommandée :** corriger le robot historique post-8BB et vérifier que les aides de planification sont faciles à comprendre avant d'entreprendre un rééquilibrage lourd des coûts.
