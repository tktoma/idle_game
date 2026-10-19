# Laboratoire de tests Idle — rapport V2

**Date :** 17 octobre 2026  
**Objet :** instrumentation, nouveaux robots, statistiques de pacing, expériences A/B, graines aléatoires, secours aux profils bloqués, audits économiques, graphes de dépendances et préparation de campagnes reproductibles.  
**Support :** code Java réel de l'archive Idle fourni précédemment, sauvegardes de la campagne de 24 profils et document utilisateur *Idle — le jeu pas à pas, du premier clic à l'univers* (107 pages).  
**Point de méthode :** **ne pas confondre un résultat observé sur un robot avec une propriété garantie du jeu ou un comportement humain**. Les profils historiques comportaient une erreur de politique qui a été identifiée pendant cet audit.

## 0. Résumé pour décider

- **50 simulations indépendantes du premier acte** : médiane **3 h 06**, moyenne **3 h 07**, minimum **2 h 26 min 50 s**, maximum **4 h 21 min 02 s**, écart-type **21 min 29 s**. **6/50** dépassent 3 h 30 ; **1/50** dépasse 4 h. La chance en début de partie mérite de vrais tests, mais la politique du robot influence aussi ces écarts.
- **12 politiques de robot V2** essayées sur une sauvegarde identique à **7 Big Bangs**, horizon de **12 h supplémentaires**, pas **20 s**, graine **42**. **9 victoires sur 12**. La stratégie de **dépendances** est la plus rapide : **7 h 37 min 40 s** pour l'univers ; le robot expert généraliste atteint **7 h 39 min 20 s**.
- **6 profils × 5 graines = 30 essais rapides** jusqu'à la galaxie, **30/30 réussis** avant 6 h ; la médiane de la stratégie de dépendances est **4 h 51**. *Ces 30 essais ne mesurent pas la fin de l'univers.*
- **Deux variantes d'équilibrage réelles**, testées séparément et ensemble : durée de formation cosmique **45 → 30 min** ; cadence de création automatique de molécules **5 → 3 s**. Avec le robot guidé et les graines **42 et 43**, aucune des variantes ne change les **8 h 55 min 20 s** jusqu'à l'univers. Résultat **négatif local**, pas démonstration que ces paramètres ne servent jamais.
- **Trois profils bloqués** repris exactement depuis leurs sauvegardes de la campagne à 72 h. Un conseil ciblé fait atteindre le **8e Big Bang au fan de prestige en +1 h 17**. Deux autres stratégies généralistes ne terminent pas l'amas sur +48 h. Mais un **robot expert corrigé** termine l'univers depuis l'état de l'accumulateur en **+2 h 21 min 20 s** et depuis l'état AFK en **+2 h 41 min**. *Les deux victoires sont vérifiées par rechargement de sauvegarde.*
- **Défaut majeur du précédent bot détecté :** son mode « préparation de Big Bang » désactivait fusion et synthèse après le huitième Big Bang, alors qu'il fallait regarnir le tableau périodique pour fabriquer les molécules. Cette erreur biaise les anciennes interprétations des « profils incapables de gagner ».
- **Catalogue audité :** 526 objets (430 molécules, 58 assemblages, 35 corps, 3 échelles cosmiques), **528 dépendances**, profondeur maximale **11**. *Eau* alimente **27 dépendances directes / 50 en cascade** ; *quartz* **17 / 45**. Les ressources rassemblées servant de conditions et non de consommables, ces degrés ne mesurent **pas** des coûts de fabrication.
- **Analyse d'achat immédiate :** dans l'état de test de début, un générateur à 20 particules a un remboursement théorique en **9,9 s** ; la vitesse à 4 en **19,8 s** ; un couplage à 60 n'offre aucun gain immédiat avec un seul générateur. Ces calculs sont **locaux**, non un plan d'optimisation global.
- **Progression hors ligne :** une comparaison de **4 h calendaires** montre que la sauvegarde avancée reçoit déjà **100 % du temps hors ligne**. La différence vient alors des **décisions non automatiques**, pas d'une pénalité de vitesse. Les taux sont différents plus tôt dans le jeu ; le PDF utilisateur décrit 10 %, 40 %, puis 70 % initialement suivant les actes, avec améliorations ultérieures.

### Ce que ces résultats ne permettent PAS de conclure

1. Aucun robot n'a démontré un optimum global de zéro à l'univers. Le « 19 h 30 » du document utilisateur est un **repère de sa propre version/méthode**, pas une durée validée par le benchmark V2.
2. Les **24 profils historiques**, bien que utiles comme stress test, **ne sont pas des utilisateurs observés** ; deux de leurs défaites provenaient ou étaient amplifiées par des erreurs de script.
3. Les parties V2 commencent principalement à **7 Big Bangs**. Les temps de 7 à 12 h ne doivent **jamais** être additionnés mécaniquement à des temps d'une autre expérience pour obtenir une partie complète.
4. Les compteurs `sans_progres` et `temps_stagnation_30min_h` reposent sur **des jalons détectés**, non sur l'absence absolue de production. Le « robot sans impulsion » n'est pas forcément une ablation efficace si l'impulsion n'est pas sur le chemin critique.
5. Les échantillons sont limités : 50 graines sur **un seul robot début**, 5 graines par politique sur **six robots** pour la galaxie, 2 graines par variante A/B, et souvent une seule pour l'univers. **480/1 200 parties ne sont pas exécutées** : leurs scripts sont fournis pour lancement ultérieur.

---

## 1. Contexte de conception : éviter les recommandations déjà intégrées

Selon le PDF fourni :

- Les outils d'aide existent **déjà** : **liste de courses**, quantités manquantes, **meilleur achat**, filtres « utiles au prochain astre », indicateurs de progression du Big Bang, chronomètre et estimation d'espace manquant, pastilles d'action, journal d'annonces (pages 44–46).
- L'automatisation avancée existe déjà : ordre d'achat, préservation d'espace, rassemblement et formation automatiques, créations suivies par la liste de courses (pages 42–44 et 55).
- Le jeu est structuré en **trois actes** ; la progression cosmique tardive impose **8 Big Bangs**, la chimie, des astres mûrs, des systèmes solaires et la diversité d'états de matière (pages 1, 59–61, 47).
- Le PDF décrit aussi des premières estimations historiques de parties de **19–22 h**, avec des variantes anciennes de règles au fil des versions. Les données de ce laboratoire proviennent de **l'archive Java effectivement testée** ; les anciennes valeurs du PDF sont utilisées comme **contexte**, pas comme preuve sur notre version.

**Conséquence pratique :** il faut tester l'utilité et la visibilité des aides existantes, pas « ajouter une liste de courses » en doublon. Une amélioration UI serait plutôt un avertissement conditionnel : « Votre tableau manque d'éléments pour les recettes, mais la synthèse automatique est coupée. »

---

## 2. Les dix expériences demandées

### 2.1. Robot optimiseur économique

**Réalisation :** robot `ECONOMISTE` dans `code/AdvancedLab.java`, qui consulte `Game.bestMolecule()`, cherche à améliorer le rendement des molécules, mais accepte aussi des achats hors chemin critique. Robot `DEPENDANCES` priorisant directement les constructions de l'astre suivant ; robot `EXPERT_COMPLET` agissant plus souvent.

**Observé (graine 42, sauvegarde 7BB) :** économiste **8 h 55**, dépendances **7 h 38**, expert **7 h 39**, guide UI **8 h 55**. L'économiste **n'est pas le plus rapide**, parce que la rentabilité marginale d'une molécule n'intègre pas forcément son rôle dans la prochaine construction. Les résultats plaident pour un score combinant **gain économique + déblocage de prérequis + coût d'opportunité**.

**Limite :** ce robot est une **heuristique**, pas une recherche exhaustive par arbre Monte-Carlo, programmation dynamique ou renforcement appris. Un véritable optimum reste à construire.

### 2.2. Temps morts et périodes sans jalons

**Réalisation :** logs `events.csv` toutes les transitions enregistrées (Big Bang, astres, assemblages, substances, systèmes, cosmos), `timeline.csv` toutes les 10 minutes, et analyse historique horaire `data/periodes_sans_jalon_celeste.csv`.

**Observé :** le meilleur robot de dépendances cumule **4 h 26 min** de fenêtres dans lesquelles le dernier jalon a plus de 30 minutes d'ancienneté, mais continue à produire ; la plus grande fenêtre sans jalon enregistré vaut **1 h 30**. Dans l'historique 24 profils, on recense **35 passages** de 4 h ou plus sans nouveau *jalon céleste* à résolution horaire.

**Attention :** « aucune nouvelle étoile/galaxie » ne veut pas dire « rien d'intéressant à l'écran ». Il faut encore journaliser les décisions de joueur et les découvertes mineures. Les chiffres sont des **indicateurs de risque d'ennui**, pas une mesure psychologique de l'ennui.

### 2.3. Plusieurs graines aléatoires

**Réalisation :** 50 premières explosions issues d'une partie neuve par `code/RobotPhase3.java` avec graines distinctes, même politique. Sources : `data/multigraines_acte1/50_graines.csv`, histogramme `graphs/01_distribution_50_graines.png`.

**Résultat :** médiane **3 h 06**, moyenne **3 h 07**, P10 ≈ **2 h 41**, P90 ≈ **3 h 35**, min **2 h 26 min 50 s**, max **4 h 21 min 02 s**. L'amplitude extrême est **1 h 54 min 12 s**. 12 % des parties dépassent 3 h 30.

**À améliorer :** mesurer par graine le moment d'obtention des rares éléments à effet de production (gaz nobles, actinides, collections, etc.). Ne pas modifier au hasard le garde-fou qui garantit les inconnus avant d'avoir identifié la corrélation exacte.

### 2.4. Tests A/B de l'équilibrage

**Réalisation :** compilation de quatre variantes Java indépendantes dans `ab_tests/` : `base`, `settle30` (45 → 30 min d'attente cosmique), `automation3` (création moléculaire 5 → 3 s), `both`. Deux graines comparables **42 et 43**, soit **8 parties**, 8 victoires.

**Résultat :** **8 h 55 min 20 s** pour la victoire dans chacun des huit essais ; valeurs identiques pour galaxie, amas et univers. L'agent de comparaison est `GUIDE_UI`. Cela indique seulement que **ces modifications ne débloquent pas le goulot d'étranglement de cette politique**. On ne peut ni conclure à leur inutilité, ni en déduire un effet nul chez un humain.

**Contrôle de patch :** `comparer_patches.py` compare graines communes, temps, échecs nouveaux et seuil de régression. Exemple : `python comparer_patches.py data/ab/base data/ab/settle30 15`.

### 2.5. Secours aux robots bloqués

**Réalisation :** trois sauvegardes reconstruites à partir des règles **strictement inchangées** des profils historiques après 72 h ; comparaison sans et avec une correction, horizon +48 h. Contrôle expert supplémentaire +12 h et sauvegarde de victoire vérifiée.

| Profil | Sans intervention | Conseil simple | Intervention experte |
|---|---|---|---|
| Fan de prestige, ID 11 | 7BB / 27 astres après +48 h | **8BB en +1 h 17**, 33 astres après +48 h | Coach généraliste encore bloqué (7BB, 27 astres) |
| Accumulateur, ID 12 | Toujours galaxie après +48 h | Toujours galaxie | **Univers en +2 h 21 min 20 s** avec coach corrigé |
| AFK, ID 17 | Toujours galaxie après +48 h | Toujours galaxie | **Univers en +2 h 41 min** avec coach corrigé |

Le fan de prestige demande un **conseil particulier sur le 28e astre** ; les deux autres possèdent déjà des ressources cosmologiques massives, mais n'appliquent pas les dernières recettes.

**Découverte majeure :** le robot historique pouvait laisser `auto_fusion` et `auto_synthesis` coupés **après le 8e Big Bang**, alors que la chimie nécessitait de nouveaux éléments. C'est un défaut **de notre robot**, et non du jeu. Voir `code/CorrectedCoach.java` et `data/coaching_corrige/`. L'effet exact de cette erreur sur chacune des 24 durées anciennes n'est pas quantifié : il faudrait les relancer avec le correctif.

### 2.6. Coût de microgestion

**Réalisation :** compter les interventions des robots, les achats assistés par `bestMolecule`, les impulsions, comètes, changements de recette et actions de planification. Voir `data/bots_v2_compiles.csv` et `data/statistiques_24_profils_detail.csv`.

**Exemple :** `DEPENDANCES` gagne en **1 373 passages de décision de 20 s**, mais envoie aussi **9 159 impulsions** durant ce parcours. Ce nombre représente des appels simulés, pas nécessairement 9 159 clics humains réels (une touche tenue, les commandes groupées et les automatismes modifient le coût d'interface).

**À instrumenter dans le jeu :** compter `clics effectifs`, `ouvertures d'onglet`, `achats groupés`, `erreurs/refus d'achat`, `commandes répétées`, `temps au premier choix après notification`. Aucune des données actuelles ne mesure les mouvements de souris ni l'effort cognitif.

### 2.7. Comportements hors ligne

**Réalisation :** cinq variantes sur quatre heures calendaires depuis la même sauvegarde 7BB : présence active, présence sans décision, micro-sessions 15 minutes/3 heures, 2 h puis absence, 1 h/jour. Mesure `data/hors_ligne_4h.csv`.

**Observé :** le checkpoint démarre déjà avec `taux_hors_ligne_depart = 1`, soit **100 %** de temps simulé récupéré : la variable discriminante est surtout la présence de décisions. Le joueur actif et « 2 h puis absence » arrivent à 8BB/33 astres à 4 h ; le robot AFK reste 7BB/26 astres. Le scénario 15 min/3 h réussit 8BB/28 astres.

**Limite :** cette étude ne couvre pas le hors ligne plus sévère du **premier acte** ; il faudra reproduire la même matrice à 0BB, après 1re explosion et après 1er BB pour évaluer les taux intermédiaires. Le contexte du PDF décrit ces taux, mais la sauvegarde testée n'y est plus soumise.

### 2.8. Graphe de prérequis de la chimie et des astres

**Réalisation :** lecture directe des catalogues Java réels (430 molécules, 58 assemblages, 35 corps et 3 niveaux cosmologiques) ; **526 objets**, **528 arêtes**, profondeur **11** ; fichiers `data/dependances/` et graphe `graphs/04_centralite_composants.png`.

**Premier classement :** eau 27 dépendances directes/50 en cascade, quartz 17/45, CO₂ 12/41. Le graphe permet de tester une stratégie qui **prépare les composants centraux à l'avance**, avant de se lancer dans les astres les plus hauts.

**Limite :** les quantités de matière par état, l'espace réservé, la maturation, les bonus et le fait que certains ingrédients ne sont que des conditions **non consommées** ne sont pas encodés comme des coûts. La centralité n'est pas une mesure complète de difficulté.

### 2.9. Audit des améliorations et ablations

**Réalisation :** `code/EconomyAudit.java` recharge le moteur, essaie des améliorations achetables dans des états connus et compare la production avant/après. Ablations V2 : sans impulsions, sans comètes, sans aides technologiques, sans liste de courses.

**Résultats :** sur la sauvegarde tardive avec une graine, enlever les comètes **ou** les impulsions ne change pas les **8 h 55** du robot guidé ; enlever les aides technologiques ajoute environ **1 min 20 s**. Enlever la liste casse la progression avant le 8e Big Bang dans ce robot. **Attention :** l'ablation de la liste retire aussi une **politique de choix**, ce n'est pas l'équivalent exact de cacher le widget à un humain.

Début de partie : générateur +100 % de production pour 20 particules, retour ≈9,9 s ; vitesse +10 % pour 4 particules, retour ≈19,8 s ; couplage sans effet instantané avec 1 générateur. Les effets de palier ultérieurs ne sont pas inclus dans ces ROI.

### 2.10. Détection automatique de régressions après patch

**Réalisation :** compilations différentes des sources A/B, résultats sur mêmes graines, comparaison avec `comparer_patches.py`, scripts de benchmark et structure d'archivage par version.

**Contrôles prévus :** nouveaux échecs là où référence gagnait, variation relative de durée, étapes devenues inaccessibles, maximum d'attente, dispersion multi-graines et respect des règles de prestige.

**Limite :** le script de comparaison est **livré et exploitable**, mais son intégration dans le dépôt GitHub et l'exécution automatique en CI n'ont **pas été mises en place**. Aucun commit n'a été poussé sur ton dépôt.

---

## 3. Statistiques détaillées et définitions

| Mesure | Mode de calcul / fichier | Niveau de confiance |
|---|---|---|
| Heures jusqu'à la première explosion | 50 graines, `premiere_explosion_secondes / 3600` | Élevé **pour ce robot** |
| P10, médiane, P90, écart-type | Statistiques sur les 50 temps de première explosion | Élevé sur cet échantillon, pas population humaine |
| Big Bangs, astres mûrs, molécules, substances | Snapshot du moteur toutes les 10 min par agent V2 | Élevé, échantillonné |
| Plus longue durée sans événement | Différence entre événements macro suivis par `AdvancedLab` | Moyen : événements mineurs omis |
| Temps « stagnation 30 minutes » | Temps cumulé où aucun événement macro depuis 30 min | Moyen ; **pas** temps totalement inactif |
| Microgestion / décisions | Nombre de passages dans la politique robot, impulsions et aides | Bon pour le bot, mauvais proxy pour clics humains |
| Politique de reconstitution du tableau | État des automatismes, nombre d'éléments, `canSynthesize()` | Élevé pour diagnose moteur |
| Retour sur investissement | Valeur instantanée `coût / Δproduction` | Bon localement ; ignore les futurs multiplicateurs |
| Importance d'une recette | Centralité, nombre de descendants, profondeur des prérequis | Bon pour la structure ; ignore quantités et espace |
| Temps actif et calendaire | Profil de sessions + moteur de reprise | Élevé seulement sous la politique utilisée |
| Écart A/B patch | Même graine, même checkpoint, moteur compilé différemment | Élevé **sur cette paire**, faible généralisation |
| Temps de victoire historique | 24 stratégies sur 7BB, censures à 12/72/336 h | **À requalifier** en raison du bug robot post-8BB |

Le dossier contient les CSV bruts **et** les agrégats, afin d'éviter les chiffres impossibles à reproduire.

### Réconciliation des anciennes statistiques

Les anciennes 24 politiques donnaient 21 univers à 14 jours et 3 non-gagnants. Il serait tentant de dire que le jeu soft-locke ces trois joueurs. **Ce serait prématuré.** La relecture des sauvegardes révèle que deux d'entre eux peuvent finir rapidement avec une politique corrigée ; la troisième avait un blocage ciblé d'astre. Cette campagne mesure **les conséquences des comportements codés**, pas le pourcentage de joueurs humains qui finiront réellement le jeu.

---

## 4. Huit robots nouveaux / comparateurs

| Robot | Politique effectivement implémentée | Graine 42, horizon 12 h, état 7BB |
|---|---|---|
| `ECONOMISTE` | Optimise opportunément la production et la chimie selon `bestMolecule()` | **Univers +8 h 55** |
| `PLAN_PRESTIGE` | Recherche trop fréquemment une explosion / prestige | **Échec** : 7BB, 27 astres après 12 h |
| `DEPENDANCES` | Priorité stricte aux objectifs et aux recettes, plusieurs passes par décision | **Univers +7 h 38** |
| `ADAPTATIF` | Augmente la probabilité de planification quand le prochain jalon tarde | **Univers +9 h 35** |
| `EXPERT_COMPLET` | Fréquence et intensité renforcées, recours à toutes les aides | **Univers +7 h 39** |
| `DEBUTANT_APPRENANT` | Probabilité de bonne décision croissante dans le temps | **Univers +9 h 20** |
| `GUIDE_UI` | Priorité aux cibles de la liste de courses et aux assistances existantes | **Univers +8 h 55** |
| `RECUPERATION` | En cas de plateau, multiplie les actions de planification et achats ciblés | **Univers +10 h 07** |

**Précision essentielle :** les termes « apprentissage » et « adaptatif » décrivent ici **des heuristiques codées** (probabilités et règles) ; aucun modèle d'apprentissage automatique n'a été entraîné. Le résultat du `PLAN_PRESTIGE` fournit un contrôle de mauvaise stratégie, pas une preuve d'impossibilité intrinsèque.

### Les quatre ablations supplémentaires

- `SANS_IMPULSIONS` : univers **+8 h 55** ; pas de différence observée avec `GUIDE_UI` sur la graine 42.
- `SANS_COMETES` : univers **+8 h 55** ; même réserve.
- `SANS_TECH` : univers **+8 h 56** ; faible coût local.
- `SANS_LISTE` : non gagnant en 12 h, 7BB/26 astres ; ce robot abandonne effectivement une politique de préparation des recettes.

Ces contrôles sont distincts d'un essai A/B dans l'interface graphique : ils désactivent une **capacité du robot**, pas nécessairement la fonctionnalité du joueur.

---

## 5. Cas détaillés des trois robots secourus

### 5.1. Fan de prestige (11)

- État initial à 72 h : **7 Big Bangs, 27 astres mûrs**, trois systèmes solaires.
- Dépendance précise : **28 astres mûrs pour le huitième Big Bang**.
- Conseil : conserver l'état, préparer explicitement les astres manquants, différer les explosions.
- Résultat : **8e Big Bang obtenu après +1 h 17**, contre aucun dans le contrôle à +48 h.
- Restant : l'univers n'est pas obtenu sur cette expérience de sauvetage ; le bot complet a besoin d'améliorer aussi sa chimie.

### 5.2. Accumulateur / avare (12)

- État initial : **8BB / 35 astres mûrs / galaxie**, espace très abondant mais seulement **22 éléments actuellement accessibles**.
- Symptôme : presque aucune recette moléculaire créable, alors que `canSynthesize()` reste vrai.
- Cause testée : le mode pré-Big Bang historique neutralise fusion/synthèse automatique après le huitième Big Bang.
- Simple incitation à dépenser davantage : **inefficace** en +48 h.
- Expert avec le correctif post-8BB : **univers en +2 h 21 min 20 s** ; sauvegarde validée.

### 5.3. AFK une intervention / heure (17)

- État initial : **8BB / 35 astres mûrs / galaxie** et progression non terminée.
- Conseil limité à sa fréquence normale : **inefficace** en +48 h.
- Intervention d'un expert continu : **univers en +2 h 41 min** ; sauvegarde validée.
- Ce résultat **ne valide pas** une victoire AFK avec sa fréquence initiale : on a **changé le décideur et l'intensité de ses décisions**.

### Proposition de modification du jeu issue des secours

Le document explique que les aides de progression et la liste de courses existent déjà. Il serait utile d'en tester **l'usage réel** : détecter un plateau, afficher un objectif prioritaire concret (le 28e astre ; la synthèse désactivée ; les catégories manquantes d'état de matière), puis mesurer si un utilisateur comprend et suit ce conseil. Une aide trop bavarde pourrait au contraire enlever le plaisir de découverte : playtest humain nécessaire.

---

## 6. Trois niveaux de benchmark et protocole de patch

| Niveau | Périmètre | Exécuté réellement ? | Résultat |
|---|---|---|---|
| **Rapide** | 6 politiques × 5 graines = **30 essais** ; 6 h, objectif galaxie | **OUI, 30/30** | Galaxie atteinte partout, médianes 4 h 51 à 4 h 56 |
| **Standard** | 24 profils historiques × 20 graines = **480 essais** | **NON** ; lanceur inclus | Aucun chiffre extrapolé |
| **Approfondi** | 24 profils historiques × 50 graines = **1 200 essais** | **NON** ; lanceur inclus | Aucun chiffre extrapolé |

Les campagnes standard et approfondie sont coûteuses : plusieurs milliers d'heures **virtuelles** à simuler et beaucoup d'exécutions Java réelles. Les lanceurs permettent de limiter `WORKERS`, `HOURS`, `STEP` et les graines. Le petit benchmark « rapide » est une étude **sur six nouveaux robots**, pas sur 24.

**Exemple depuis le dossier extrait :**

```bash
# Java 21, répertoire du projet V2
java -version
javac -cp work/full/moteur_core/classes -d bin code/*.java
bash run_benchmark_24.sh standard     # 24 x 20 graines, non encore exécuté
bash run_benchmark_24.sh approfondi  # 24 x 50 graines, non encore exécuté
python comparer_patches.py data/ab/base data/ab/settle30 15
```

**Exigences pour chaque futur patch :** identifier version du moteur, conserver les mêmes graines, conserver les états de départ, imposer le même pas de temps, isoler les modifications Java dans un répertoire temporaire, conserver les sauvegardes terminales, vérifier les victoires par `VerifyVictory`, noter le nombre de parties censurées, afficher P10/P50/P90 plutôt que seulement la moyenne.

---

## 7. Actions concrètes, hiérarchisées

### Avant de modifier les règles

1. **Corriger les robots historiques** et refaire au minimum les 3 perdants à comportement constant, puis une petite cohorte représentative. Sans cette correction, la distribution des victoires est trompeuse.
2. **Vérifier la lisibilité des aides déjà en jeu**. Dans un playtest : à 27/28 astres, l'utilisateur sait-il lequel manque ? Après le huitième BB, voit-il immédiatement que ses ingrédients périodiques ont été réinitialisés ? Pour l'amas, sait-il quels *états de matière et combien de sortes* manquent ?
3. **Instrumenter la production d'événements mineurs** : découverte d'une nouvelle sorte, recette commencée/complétée, nouveau rayon débloqué, premier système solaire, attente d'une maturation. Les seules étapes galaxie/amas/univers sont trop espacées pour juger l'ennui.
4. **Tester la chance avant tout nerf/buff massif**. En priorité, relier le temps du premier acte à l'ordre d'acquisition des éléments rares.
5. **Ne pas accélérer aveuglément la maturation** : l'A/B local n'a rien changé au délai de victoire ; le bottleneck semble être ailleurs pour cette politique.

### Optimisation des robots de test

6. **Score multicritère** : achat utile = rentabilité de production + contribution au prochain prérequis + temps avant disponibilité + perte potentielle au prestige.
7. **Logique spécifique aux transitions** : ne plus employer la routine de préparation du Big Bang après le dernier Big Bang nécessaire à la campagne. Ajouter un test de non-régression sur la synthèse post-8BB.
8. **Contrôle différentiel `tick(dt)`** avec `dt = 0,1 ; 1 ; 20 secondes` sur les états où le robot change de stratégie : observer si les résultats divergent de manière problématique.
9. **Exécuter davantage de graines** pour les étapes déjà identifiées comme aléatoires, et garder `50` comme minimum du premier acte.
10. **Faire des tests humains** sur la compréhension de l'UI et le sentiment de progression : le code et les bots ne peuvent pas mesurer le plaisir directement.

---

## 8. Inventaire et reproductibilité

Tous les scripts Java sont dans `code/`, les sources compilables du moteur dans `work/full/moteur_core/`, les variantes A/B dans `ab_tests/`, les résultats dans `data/`, les figures dans `graphs/`. L'archive précédente des campagnes historiques est rangée sous `HISTORIQUE/` dans l'archive maître.

### Commandes de contrôle

```bash
javac -cp work/full/moteur_core/classes -d bin code/*.java
java -cp "work/full/moteur_core/classes:bin" AdvancedLab DEPENDANCES work/full/sauvegardes/checkpoint_before_eighth.save 12 20 42 output_dependances
java -cp "work/full/moteur_core/classes:bin" CorrectedCoach data/rescue/original_12/etat_72h.save 12 20 42 output_secours
java -cp "work/full/moteur_core/classes:bin" VerifyVictory data/coaching_corrige/12/winner.save
python comparer_patches.py data/ab/base data/ab/automation3 15
```

Les exemples sont rédigés pour Linux. Sous Windows, utiliser `;` au lieu de `:` dans le classpath et adapter les barres obliques. Les programmes de benchmark relancent l'état initial **à l'identique** et écrivent les fichiers `result.csv`, `timeline.csv`, `events.csv` dans leurs répertoires de sortie.

Le registre `MANIFEST_SHA256.csv` inclus dans l'archive finale donne le hash de chaque fichier source et résultat. L'archive peut ainsi servir d'**étalon après patch**.

### Statuts de vérification

- **Compilation moteur et robots :** oui dans l'environnement Java 21 de simulation.
- **30 essais rapides multi-graines :** oui, 30/30 terminés.
- **50 essais première explosion :** oui, 50/50 terminés.
- **8 simulations A/B :** oui, 8/8 gagnantes.
- **12 robots V2 :** 12/12 exécutés, 9 victoires validées.
- **3 sauvegardes bloquées :** recréées, 3 contrôles et 3 traitements ciblés, contrôles terminés.
- **2 sauvetages avec le coach corrigé :** sauvegardes de victoire rechargées et reconnues.
- **Tests standard 480 parties :** préparés mais **NON exécutés**.
- **Tests approfondis 1 200 parties :** préparés mais **NON exécutés**.
- **Rendu graphique JavaFX / tests utilisateurs humains :** **NON réalisés** dans ce laboratoire.

---

## 9. Conclusion

**La plus grande découverte n'est pas un multiplicateur à ajuster : c'est la différence entre la difficulté du jeu et les erreurs de décision du bot.** Un robot de prestige peut tourner en rond à 27/28 astres. Un bot qui conserve la matière peut rester bloqué après le huitième Big Bang s'il a, par erreur de script, désactivé la fusion et la synthèse dont dépend toute la chimie. Deux sauvegardes historiquement « incapables de terminer en 14 jours » ont obtenu l'univers en **2 h 21** et **2 h 41** après transfert de contrôle à une politique réparée.

Il faut maintenant utiliser ce laboratoire pour **mesurer le parcours du joueur qui comprend les aides**, analyser la variance du premier acte et tester les changements sur **exactement les mêmes graines**. Le dossier fournit les bases de cette démarche, y compris les expériences à résultat négatif et les protocoles encore non exécutés.
