# Les skins à venir : tout ce qui peut changer d'apparence

Version 0.180, triée en 0.184, réalisation commencée en 0.185 (les lignes « fait en » le disent) : onze idées retirées (fruit, serpents, anneau de Saturne, case du Big Bang, sucre, bestiaire, mécanique, skins par échelle, cartes de butin, arbre-monde, thème d'après le skin), l'horlogerie refaite en engrenages par particule ; 167 visuels restent. Une liste de travail, pas un engagement : chaque ligne est un visuel du jeu qui pourrait recevoir une
apparence de rechange, avec des idées de skins, ce que chacun changerait, et une façon de l'obtenir dans l'esprit des
skins existants (un record, une recherche, un défi, un palier). Le mécanisme ne change pas : un skin débloqué donne
+2 % à toutes les ressources d'en dessous de l'univers (`Skin.BONUS`), le choisir ne change que l'apparence, et
« Classique » reste toujours là.

Ce qui existait (version 0.177) : huit visuels (`SkinVisual`), dix-neuf skins (`Skin`), cinq thèmes et trois notations
à débloquer (`Cosmetic`). Depuis la 0.200, toute la liste est réalisée : 92 visuels, 369 skins, dix-neuf thèmes et quatorze notations (section 9).

Conventions : une ligne par skin, « nom — ce qui change (formes, couleurs, mouvement) ; comment on l'obtient ».
Les couleurs sont données en hex quand elles comptent. Les noms des fichiers sont ceux de `ui/src/main/java/idle/ui`.

## 1. Ce qui est déjà skinné, et ce qu'on peut y ajouter

### La fusion des particules (`ParticleView`, visuel PARTICLE_FUSION : Éclair, Prisme, Solitaire)
- Braise — **fait en 0.191** (condition retenue : 100 fusions) : des poussières orange `#ffa53a` qui rougissent en approchant, un noyau de charbon craquelé qui luit par les fentes ; dix minutes sans acheter de générateur, la production en marche.
- Encre — **fait en 0.191** (condition retenue : 1 000 fusions) : fond blanc crème dans la carte, poussières noires qui coulent comme des gouttes d'encre vers le centre, noyau en tache ; mille fusions.
- Essaim — **fait en 0.191** (condition retenue : 100 générateurs en même temps) : les poussières deviennent de petits traits qui volent en banc, changent de cap d'un coup, le noyau est une ruche hexagonale ; cent générateurs formés d'un seul coup (touche M tenue).
- Horloge — **fait en 0.191** (condition retenue : 10 000 fusions) : le noyau est un cadran, les anneaux d'accrétion des aiguilles, chaque particule créée fait avancer la trotteuse ; une heure d'affilée au régime lent.
- Verre soufflé — **fait en 0.191** (condition retenue : le skin Prisme débloqué) : poussières en bulles irisées (teinte HSB lente), noyau en goutte de verre avec un reflet blanc ; la recherche « Fusion prismatique » et le skin Prisme déjà choisis une heure.
- Pixel — **fait en 0.191** (condition retenue : un record de reconstruction en moins de 30 s) : tout en carrés de 4 px, couleurs à 8 valeurs, ondes carrées ; un record de reconstruction en moins de trente secondes.

### Le grand atome (`AtomModelView`, visuel ATOM : Noyau dense, Noyau stellaire)
- Modèle de Bohr au tableau noir — **fait en 0.191** (condition retenue : 50 atomes) : fond ardoise `#1d2a24`, orbites tracées à la craie (traits irréguliers, poussière), électrons en ronds de craie blanche ; cinquante atomes.
- Nuage de probabilité — **fait en 0.191** (condition retenue : 1 000 atomes) : plus d'orbites : des nuages de points `#ffe9c2` plus denses là où l'électron a des chances d'être, qui respirent ; cent atomes.
- Système à anneaux — **fait en 0.191** (condition retenue : les 118 éléments) : le noyau est une planète à bandes, les couches des anneaux de glace vus de biais ; le 118ᵉ atome une première fois.
- Horlogerie — **fait en 0.191** (condition retenue : 1 000 synthèses) : chaque particule de l'atome est un petit engrenage : les protons et les neutrons du noyau en roues dentées de laiton `#c9a24a` serrées les unes contre les autres, les électrons en pignons minuscules qui tournent sur leur couche et s'engrènent au passage ; une heure de synthèse automatique sans intervention.
- Vitrail — **fait en 0.191** (condition retenue : 100 éléments) : couches en plomb noir, électrons en verres colorés par famille du tableau, lumière qui passe au travers ; toutes les familles possédées une fois.

### Le tableau périodique (`PeriodicTablePage`, visuel TABLE : Plomb, Or)
- Bois gravé — **fait en 0.191** (condition retenue : 50 éléments) : cases en planchettes `#8a6440` au grain visible, symbole brûlé au fer, possédées vernies ; cinquante éléments possédés.
- Cristal — **fait en 0.191** (condition retenue : 10 000 synthèses) : cases en facettes translucides de la couleur de la famille, bordures blanches, les cases au maximum scintillent ; une famille complète au maximum.
- Rouille — **fait en 0.191** (condition retenue : le défi Rouille) : métal piqué `#6e4a3a`, une case possédée est polie, une case au maximum chromée ; le défi « Rouille » réussi.
- Néon — **fait en 0.191** (condition retenue : le skin Néon du collisionneur) : cases noires au trait de néon de la famille, texte qui grésille ; le défi « Sans surchauffe » réussi (comme Néon du collisionneur, pour faire une paire).
- Carte au trésor — **fait en 0.191** (condition retenue : dix sondes) : parchemin `#d8c49a`, cases dessinées à l'encre brune, les possédées marquées d'une croix rouge, les uniques d'une étoile dorée ; dix voyages de sondes.
- Minuscule — **fait en 0.191** (condition retenue : le premier élément) : cases réduites à leurs lettres, sans fond, alignées comme une partition : pour les grands écrans ; option libre, dès le premier élément.

### La boule de matière noire (`DarkMatterView`, visuel DARK_BALL : Obsidienne, Nébuleuse, Contractée)
- Goutte de mercure — **fait en 0.191** (condition retenue : 25 explosions) : sphère miroir `#c0c8d0` qui reflète la règle graduée, tremble à chaque appui ; une explosion sans aucune matière noire gaspillée (tout dépensé avant).
- Trou noir — **fait en 0.191** (condition retenue : 10 explosions) : disque noir net, anneau de lumière lentillée orange en haut, l'échelle des grandeurs se déforme autour ; dix explosions.
- Œuf — **fait en 0.191** (condition retenue : le skin Contractée) : coquille `#efe4ff` tachetée qui se fend à mesure que la boule grossit, l'explosion la fait éclore ; le défi « Contraction douce » réussi deux fois.
- Nœud — **fait en 0.191** (condition retenue : 200 explosions) : une corde violette `#c9a6ff` nouée sur elle-même qui se resserre quand on appuie ; cent appuis à la main dans une heure.
- Lanterne — **fait en 0.191** (condition retenue : 500 explosions) : papier translucide, flamme à l'intérieur qui grandit avec la matière, l'explosion est un éclat de papier brûlé ; une heure d'appui automatique.

### L'animation de la matière noire (visuel DARK_ANIMATION : Vortex, Pulsar)
- Pluie — **fait en 0.191** (condition retenue : 15 explosions) : des gouttes qui tombent droit, flaques en cercles au sol de la vue ; quinze explosions.
- Neige — **fait en 0.191** (condition retenue : 50 explosions) : flocons lents `#efe4ff` qui tourbillonnent et se posent ; une explosion déclenchée entre minuit et six heures (heure de l'ordinateur).
- Sablier — **fait en 0.191** (condition retenue : 1 000 explosions) : des grains qui coulent par un goulot vers la boule, un sablier dessiné en filigrane ; une heure sans explosion alors qu'elle était possible.
- Feu d'artifice — **fait en 0.191** (condition retenue : 100 explosions) : l'explosion plein écran devient une gerbe de fusées de six couleurs qui retombent ; cent explosions.

### Le Big Bang (`SpaceView`, `BurstView`, visuel BIG_BANG : Aurore, Supernova, Cristal)
- Encre de Chine — **fait en 0.191** (condition retenue : 5 Big Bangs) : fond blanc cassé `#f4efe4`, étoiles en points d'encre, disque d'espace en lavis gris, bord au pinceau ; cinq Big Bangs.
- Vitrail — **fait en 0.191** (condition retenue : 3 systèmes complets) : fond noir, ciel en verres colorés cernés de plomb, le disque est une rosace ; trois systèmes solaires complets.
- Abysses — **fait en 0.191** (condition retenue : la planète océan) : fond `#02060e`, étoiles qui deviennent des points de plancton lumineux, le disque une méduse qui pulse, le bord des tentacules ; la planète océan formée.
- Papier plié — **fait en 0.191** (condition retenue : 10 Big Bangs) : ciel en origami aux plis visibles, disque à facettes planes, étoiles en confettis de papier ; le défi « Big Bang à froid » réussi deux fois.
- Carte ancienne — **fait en 0.191** (condition retenue : 25 comètes) : fond parchemin, étoiles reliées en constellations nommées en latin, disque en rose des vents ; vingt-cinq comètes attrapées.
- Néon de ville — **fait en 0.191** (condition retenue : 20 Big Bangs) : fond asphalte mouillé, disque en enseigne qui clignote, étoiles en fenêtres allumées ; cent molécules de la chimie du carbone.

### Le faisceau du collisionneur (`EnergyView`, visuel COLLIDER_BEAM : Faisceau doré)
- Comète — **fait en 0.191** (condition retenue : 50 comètes) : deux boules à longue queue de glace `#bfe8ff`, les croisements font neiger ; vingt-cinq comètes attrapées.
- Encre — **fait en 0.191** (condition retenue : 1 000 collisions) : deux traits de plume noire sur fond clair, le croisement une tache ; mille croisements captés.
- Laser — **fait en 0.191** (condition retenue : 10 000 collisions) : deux rayons fins et droits par segments, sans traînée, étincelles en pixels ; une heure à la plus haute énergie.
- Lucioles — **fait en 0.191** (condition retenue : 100 collisions) : nuées de points verts clignotants au lieu de boules, le croisement les fait toutes s'allumer ; cent captures à la main.

### L'anneau du collisionneur (visuel COLLIDER : Néon, Plasma, Glace)
- Bois et cuivre — **fait en 0.191** (condition retenue : 10 générateurs du collisionneur) : anneau en lattes vernies cerclées de cuivre `#b87333`, poussières en sciure ; dix générateurs du collisionneur.
- Circuit imprimé — **fait en 0.191** (condition retenue : 100 générateurs) : anneau en piste verte `#1f5a2a` avec pastilles dorées, poussières en bits qui passent ; tous les générateurs achetés.
- Stade — **fait en 0.191** (condition retenue : 1 000 générateurs) : anneau en piste d'athlétisme à huit couloirs, détecteur en ligne d'arrivée ; cent tours du faisceau rapide.
- Ruban de Möbius — **fait en 0.191** (condition retenue : le skin Glace) : l'anneau se tord sur lui-même, les faisceaux passent dessus puis dessous ; le défi « Un seul générateur » réussi deux fois.

## 2. Le chrome : fenêtre, onglets, cartes

### Le fond et les barres (`GameAppBase`, `GameAppTabs`, `GameAppWindow`)
- Papier — **fait en 0.190** (visuel « Le fond de la fenêtre ») : fond `#f4efe4` et texte sombre ; une heure de jeu.
- Ardoise — **fait en 0.190** : fond `#1d2a24` à la craie, bordures en traits de craie ; cinquante éléments découverts.
- Nuit étoilée — **fait en 0.190** : un ciel très sombre avec quelques étoiles fixes derrière les pages ; la galaxie formée.
- Terminal — **fait en 0.190** : fond noir pur, texte vert `#6dff8f`, bordures en caractères ASCII ; cent explosions.
- Blueprint — **fait en 0.190** : fond bleu de plan `#1a3a6b`, lignes blanches fines, quadrillage discret ; dix assemblages formés.

### Les onglets (`tabStyle`, couleurs par onglet)
- Onglets en onglets de classeur — **fait en 0.190** (visuel « Les onglets ») : forme trapèze, papier coloré, le courant dépasse ; le Big Bang débloqué.
- Boutons physiques — **fait en 0.190** : relief, ombre portée, le courant enfoncé ; dix heures de jeu.
- Pastilles — **fait en 0.190** : l'onglet n'est qu'un rond coloré avec l'icône, le nom apparaît au survol ; l'univers 1 formé.
- Rubans — **fait en 0.190** : bandes verticales à gauche, la couleur de l'onglet en pleine hauteur ; l'univers 2 formé.

### Les icônes d'onglet (`TabIcon`, 13 icônes animées, et `AtomView`)
- Jeu complet « trait de craie » — **fait en 0.192** (condition retenue : 50 atomes créés) : toutes les icônes redessinées en un seul trait blanc tremblé ; cinquante atomes.
- Jeu « pixel art » — **fait en 0.192** (condition retenue : un record de reconstruction en moins d'une minute) : icônes en 12×12 pixels, animation en deux images ; un record de reconstruction en moins d'une minute.
- Jeu « constellation » — **fait en 0.192** (condition retenue : la galaxie formée) : chaque icône est un groupe d'étoiles reliées ; la galaxie formée.
- Jeu « emoji » — **fait en 0.192** (condition retenue : la bulle du bonsaï à droite achetée) : icônes pleines et rondes, couleurs vives ; la bulle du bonsaï à droite.
- Immobiles — **fait en 0.192** (skin libre « Figées », sans condition ; le réglage garde la main) : les mêmes icônes sans animation, pour les écrans lents : c'est déjà un réglage (TAB_ICONS), il pourrait devenir un skin « Figées ».

### Les cartes (`Card`, le bouton universel du jeu)
- Fiche cartonnée — **fait en 0.190** (visuel « Les cartes ») : fond crème, coins écornés, texte à la machine à écrire ; cent fusions.
- Tuile de verre — **fait en 0.190** : fond translucide, bord lumineux à l'état prêt ; le skin Cristal du Big Bang débloqué.
- Carte à jouer — **fait en 0.190** : bord blanc, coin avec le signe d'état comme une valeur, dos rouge quand fermée ; les dix défis réussis.
- Étiquette — **fait en 0.190** : un rectangle avec un œillet et un trou, pendu à un fil, qui se balance au survol ; dix sondes revenues.
- Signes d'état — **fait en 0.192** (visuel « Les signes d'état des cartes » ; conditions retenues : les succès Cent fusions, Ensemble complet, Dix explosions) : le jeu ● ○ ◐ ▶ □ ✓ × ! peut changer de famille : « runes », « formes géométriques pleines », « lettres » (P, A, E, M, C, F, V, A) ; chacun un skin, un succès de chaque onglet.

### Les briques communes (`SubTabs`, `BuyAmountRow`, `FilterBar`, `OrderPane`, `PlanBar`, `Breakdown`, `MatterText`)
- Signes des états de la matière (`MatterText` : ∴ ≈ ◆) — **fait en 0.192** (visuel « Les signes des états de la matière » ; conditions retenues : 20, 50 et toutes les 73 sortes rassemblées, le catalogue n'en ayant pas cent) : « lettres » (G L C), « gouttes et grains » (nuage, goutte, cube), « alchimie » (symboles alchimiques de l'air, de l'eau, de la terre) ; cent sortes rassemblées.
- Info-bulles (`Breakdown`) — **fait en 0.192** (visuel « Les info-bulles » ; conditions retenues : 1 h, 5 h, 10 h de jeu) : en parchemin, en post-it jaune `#f9eeda`, en terminal ; dix heures de jeu.

## 3. Particules et atomes

### Le fond animé des particules (`AmbientView`, 90 poussières)
- Neige, pluie, pollen (grains jaunes), lucioles, bulles qui montent, poussière d'or, cendres — **fait en 0.192** (visuel « Le fond animé des particules » ; conditions retenues : 1e6, 1e9, 1e12, 1e15, 1e18, 1e21, 1e24 particules créées) : sept skins du fond seulement, chacun un palier de particules (1e6, 1e9, 1e12…).
- Vide — **fait en 0.192** (skin libre « Silence », sans condition ; le réglage garde la main) : aucun fond : pour les écrans lents, aujourd'hui un réglage (AMBIENT), pourrait devenir le skin « Silence ».

### Le rangement et la fusion des générateurs (`GeneratorPane`)
- Fusion en tourbillon — **fait en 0.192** (visuel « La fusion des générateurs » ; condition retenue : 100 fusions) : les générateurs tournent autour du centre avant de se confondre ; cent fusions.
- Fusion en aspiration — **fait en 0.192** (condition retenue : 1 000 fusions) : ils s'étirent vers le centre comme de la pâte ; mille fusions.
- Fusion en éclat — **fait en 0.192** (condition retenue : le skin Éclair débloqué) : ils se brisent et les morceaux se rassemblent ; le skin Éclair choisi.

### La page du tableau périodique, hors cases (légende, synthèse, ensembles)
- Les tuiles des ensembles d'éléments — **fait en 0.192** (visuel « La page du tableau périodique », skin « Médailles » ; condition retenue : un ensemble complet) : en médailles de bronze, d'argent et d'or selon l'avancée ; un ensemble complet.
- La carte de synthèse — **fait en 0.192** (skin « Creuset » ; condition retenue : 100 synthèses) : en creuset d'alchimiste qui fume ; cent synthèses.
- La légende des familles — **fait en 0.192** (skin « Spectre » ; condition retenue : toutes les familles possédées) : en bandes de spectre (raies d'émission) ; toutes les familles possédées.

## 4. La matière noire

### L'échelle des grandeurs et la règle (`DarkMatterView`)
- Règle de bois — **fait en 0.193** (condition retenue : 10 explosions) : graduations gravées, chiffres au fer ; dix explosions.
- Règle d'architecte — **fait en 0.193** (condition retenue : 20 explosions) : plastique jaune translucide ; vingt explosions.
- Repères célestes — **fait en 0.193** (condition retenue : le succès « Plus grand que tout », les 26 paliers de taille atteints) : les cercles des objets connus deviennent les objets eux-mêmes dessinés en silhouette (un grain, une bille, la Terre, le Soleil, la galaxie) ; chaque objet dépassé une fois.

### L'arbre de matière noire (`DarkMatterTreePane`)
- Racines — **fait en 0.193** (condition retenue : 30 cases de l'arbre achetées) : les traits deviennent des racines brunes qui s'épaississent avec le niveau, les cases des bulbes ; trente cases achetées.
- Circuit — **fait en 0.193** (condition retenue : 100 niveaux achetés dans l'arbre, en tout ; « l'arbre complet » n'existe pas, bifurcations et cases sans limite obligent) : traits en pistes de cuivre, cases en puces ; l'arbre complet une fois.
- Constellation — **fait en 0.193** (condition retenue : la galaxie formée) : traits en fils d'étoiles, cases en étoiles plus ou moins brillantes ; la galaxie formée.
- Métro — **fait en 0.193** (condition retenue : les quatre branches entamées, l'arbre n'en ayant pas cinq) : traits en lignes de métro colorées par branche, cases en stations ; cinq branches ouvertes.

### Les défis (`ChallengesPane`, l'arrivée de « la fuite »)
- Cartes de tarot — **fait en 0.193** (skin « Tarot » ; condition retenue : 3 défis réussis) : chaque défi une lame, l'arrivée les retourne ; trois défis réussis.
- Affiches « wanted » — **fait en 0.193** (skin « Wanted » ; condition retenue : 6 défis réussis) : papier déchiré, portrait du défi ; six défis.
- Sceaux — **fait en 0.193** (condition retenue : les dix défis réussis) : le défi réussi porte un sceau de cire rouge ; tous les défis.

## 5. Le Big Bang : espace, ciel, galaxie

### Les molécules (`MoleculeArt`, boules et bâtons)
- Boules pleines (space-filling) — **fait en 0.188** (visuel « Les molécules ») : pas de bâtons, atomes qui se touchent ; cinquante sortes créées.
- Traits (squelette) — **fait en 0.188** : seulement les liaisons, atomes en lettres ; toutes les sortes créées (73).
- Perles — **fait en 0.188** : atomes en perles nacrées, bâtons en fil ; vingt sortes rassemblées.
- Lego — **fait en 0.188** : atomes en briques à tenons, couleurs vives ; mille molécules créées.
- Palette CPK alternative — **fait en 0.194** (visuel « La palette des molécules », trois skins libres, combinés avec la forme) : « pastel », « daltonien sûr » (une palette vérifiée), « monochrome par rayon » ; chacun dès le premier Big Bang (option d'accessibilité, sans condition).

### Les amas rassemblés, les blocs d'assemblage, le gaz (`SpaceView`, `SpaceViewPlacement`, `SpaceViewPacking`)
- Amas — **fait en 0.194** (visuel « Les amas rassemblés » ; condition retenue : 5, 20, 50 sortes rassemblées) : en flaques, en cristaux à facettes, en nuages de coton, selon l'état, avec une palette au choix : « minéraux » (vrai gris du quartz, bleu du cuivre…), « néon », « pastel » ; cinq, vingt, cinquante sortes rassemblées.
- Blocs d'assemblage — **fait en 0.194** (condition retenue : 10, 15, 20 assemblages, et les 30 du catalogue pour la glace) : en briques de pierre taillée, en lingots, en cristaux bruts, en pains de glace ; dix, vingt, trente assemblages.
- Le gaz flouté au loin — **fait en 0.194** (condition retenue : 1 000, 10 000, 100 000 molécules de gaz rassemblées) : en voile de brume, en fumée de cheminée, en aurore ; mille molécules de gaz.

### Les astres (`BodyArt` : 35 astres, 10 échelles, 12 aspects)
- Planétarium — **fait en 0.186** (visuel « Les astres ») : tous les astres en sphères de laiton et de verre dépoli, sur des tiges ; dix astres formés dans un univers.
- Gravure — **fait en 0.186** : astres en hachures noir et blanc façon gravure du XIXᵉ siècle ; vingt astres formés dans un univers.
- Cartographie — **fait en 0.186** : astres en globes à méridiens et parallèles, nommés en lettres capitales ; les trente-cinq astres formés dans un univers.

### Le ciel des astres (`SkyView`)
- Planche naturaliste — **fait en 0.194** (visuel « Le ciel des astres » ; condition retenue : 15 astres formés dans un univers) : fond parchemin, astres épinglés avec étiquette manuscrite ; quinze astres.
- Mobile — **fait en 0.194** (condition retenue : 25 astres) : astres suspendus à des fils qui se balancent ; vingt-cinq astres.
- Vitrine — **fait en 0.194** (condition retenue : les 35 astres) : étagères de verre éclairées par le bas ; trente-cinq astres.

### La galaxie, l'amas de galaxies, la toile (`GalaxyView`, `Spiral`)
- Galaxie — **fait en 0.194** (condition retenue : Nautile avec la galaxie formée, puis un skin par univers formé, du deuxième au cinquième) : en coquillage (spirale de nautile), en pelote de laine, en empreinte digitale, en tourbillon d'évier, en roue de feu d'artifice ; la galaxie formée, puis chaque univers formé en débloque un.
- Amas — **fait en 0.194** (condition retenue : la grappe avec l'amas formé, les lampions à l'univers 2, l'essaim à l'univers 3) : en grappe de raisin, en guirlande de lampions, en essaim d'abeilles ; l'amas de galaxies formé.
- Toile de l'univers — **fait en 0.194** (condition retenue : un skin par univers formé, du premier au quatrième) : en réseau de neurones, en dentelle, en toile d'araignée au matin (gouttes), en carte de métro ; l'univers formé.

### Les systèmes solaires (`SystemView`)
- Orrery — **fait en 0.194** (condition retenue : 1 système complet) : orbites en cercles de laiton, planètes sur tiges ; un système complet.
- Dessin d'enfant — **fait en 0.194** (condition retenue : 2 systèmes complets) : orbites au crayon de couleur, Soleil à rayons ; deux systèmes.
- Radar — **fait en 0.194** (condition retenue : 3 systèmes complets ; les planètes y sont remplacées par des échos) : fond vert, orbites en cercles de balayage, planètes en échos ; trois systèmes.

### Les sondes (`ProbesView`, `LootPane`)
- Sondes — **fait en 0.194** (condition retenue : 10, 25, 50, 100 voyages revenus) : en avions de papier, en montgolfières, en bouteilles à la mer, en pigeons voyageurs ; dix, vingt-cinq, cinquante, cent voyages.
- Trajectoires — **fait en 0.194** (pas de visuel à part : chaque skin de sonde a la sienne, les plumes pour le pigeon) : en pointillés de carte au trésor, en traînées de condensation, en fil d'Ariane ; chacun avec le skin de sonde.

## 6. Collisionneur, hadrons, vide

### Le reste du collisionneur (détecteur, injecteur, éclairs, flammes, étincelles, quarks)
- Détecteur — **fait en 0.195** (condition retenue : 100, 250, 500, 1 000 collisions ; le jeu ne compte pas les captures à part) : en œil, en lanterne, en cloche, en appareil photo ancien (le flash au croisement) ; cent captures.
- Éclairs — **fait en 0.195** (condition retenue : 1 000, 2 500, 5 000 croisements des faisceaux) : en fils d'or, en fissures de glace, en racines ; mille croisements.
- Étincelles — **fait en 0.195** (condition retenue : le défi du détecteur, puis les épreuves « Sans surchauffe » et « Pleine charge ») : en pétales, en confettis, en braises ; le défi du détecteur.
- Quarks qui s'envolent — **fait en 0.195** (condition retenue : 10, 25, 50 hadrons liés) : en ballons, en papillons, en lettres de scrabble (la saveur) ; dix hadrons liés.

### Les hadrons (`BindingView`)
- Quarks — **fait en 0.195** (condition retenue : 10, 50, 100 hadrons liés, puis chaque sorte de hadron liée au moins une fois ; l'accord est dessiné, pas joué) : en billes de verre, en fruits (la saveur en couleur de fruit), en notes de musique (la liaison joue un accord), en pièces de puzzle qui s'emboîtent ; dix, cinquante, cent, tous les hadrons.
- Gluons — **fait en 0.195** (condition retenue : 100, 250, 500, 1 000 gluons) : en ressorts de métal, en élastiques, en fils de laine, en éclairs ; cent gluons.
- Fond — **fait en 0.195** (condition retenue : les paliers de gluons « Mémoire des liaisons », « Automatismes liés », « Éléments liés ») : en tableau noir, en table de laboratoire, en ciel d'orage ; chacun un palier des hadrons.

### Le vide (`VoidView`, le trou noir du vide)
- Écume quantique — **fait en 0.195** (condition retenue : le vide ouvert, puis une amélioration du vide au niveau 1, 2, 3) : en bulles de savon, en mousse de bière, en crépitement de pellicule, en neige de télévision ; le vide ouvert, puis chaque niveau d'amélioration du vide.
- Paires matière-antimatière — **fait en 0.195** (condition retenue : 100, 1 000, 10 000 paires nées depuis l'ouverture de la fenêtre ; ce compte n'est pas sauvegardé, les skins le sont) : en jumeaux qui se tiennent la main, en aimants, en yin et yang ; cent paires annulées.
- Le trou noir du vide — **fait en 0.195** (condition retenue : le trou noir du vide ouvert quand il compte 1 à 5 univers, soit les univers 4 à 8 formés ; le jeu ne compte pas les trous noirs qui l'ont nourri) : en bouche d'égout, en puits de pierre, en iris, en nœud de bois, en tourbillon de thé ; chaque fois qu'il est nourri d'un trou noir.

## 7. L'univers

### Les univers en bulles (`UniverseView`, la naissance)
- Bulles de savon réalistes (irisation), ballons de baudruche noués, boules à neige, globes en verre d'un orrery, planètes-fruits, œufs — **fait en 0.196** (condition retenue : un par univers formé, du premier au sixième) : chaque univers formé en débloque un.
- La naissance en éclosion, en bourgeon qui s'ouvre, en feu d'artifice, en page qui se tourne — **fait en 0.196** (condition retenue : les univers 5, 6, 7, 8 formés) : cinq univers.
- Les liens entre bulles en fil, en lierre, en circuit, en constellation — **fait en 0.196** (condition retenue : les univers 7, 8, 9, 10 formés) : dix univers.

### Le lancement de l'univers suivant (`LaunchView`)
- Fusée de papier, graine qui germe, pierre jetée dans l'eau (ondes), lampe qu'on allume, rideau de théâtre — **fait en 0.196** (condition retenue : les univers 5 à 9 formés, le jeu ne comptant pas les lancements) : chaque univers lancé après le cinquième.

### La graine et son établi (`SeedView` : graine, germe, arrosoir, étagère, tuyau, chrono, rond, météo, saisons)
- La graine en noix, en gland, en noyau d'avocat, en pépin de pomme, en graine de tournesol, en œuf (fantaisie), en pierre précieuse brute — **fait en 0.196** (condition retenue : la graine aux paliers 1 à 7, l'univers formé) : chaque palier de la graine en débloque un.
- L'arrosoir en théière, en pichet, en nuage qu'on presse, en bec d'oiseau, en coquillage — **fait en 0.196** (condition retenue : 10, 100, 1 000 arrosages de la graine en cours, qui repartent de zéro à chaque univers ; le tuyau acheté ; la terre fertile achetée) : dix, cent, mille arrosages, le tuyau, la terre fertile.
- L'étagère en pierre, en métal d'atelier, en bambou, en nuage — **fait en 0.196** (condition retenue : l'étage du chrono acheté, avec 3, 4, 5, 6 univers formés) : l'étage du chrono.
- Le tuyau en liane, en fontaine, en rigole creusée, en pipette de laboratoire — **fait en 0.196** (condition retenue : le tuyau aux niveaux 2, 4, 6 et 10, son maximum) : dix niveaux du tuyau.
- Le chrono en sablier, en montre à gousset, en cadran solaire, en métronome — **fait en 0.196** (condition retenue : le chrono plein, puis le chrono plein avec 3, 4, 5 univers formés) : le chrono plein une fois.
- Le rond de croissance en anneau de fée (champignons), en halo doré, en soleil levant — **fait en 0.196** (condition retenue : 5, 20, 50 ronds cueillis depuis l'ouverture de la fenêtre ; ce compte n'est pas sauvegardé, les skins le sont) : vingt ronds attrapés.
- La météo en pictogrammes de bulletin télévisé, en gravure japonaise (pluie en traits), en pastel — **fait en 0.196** (condition retenue : 1, 5, 10 orages depuis l'ouverture de la fenêtre ; ce compte n'est pas sauvegardé) : dix orages.
- Les saisons en cartes à jouer, en roue de couleurs, en vitrail — **fait en 0.196** (condition retenue : 1, 2, 3 cycles complets des saisons depuis l'ouverture de la fenêtre ; ce compte n'est pas sauvegardé) : un cycle complet des saisons.

### Les arbres, du bonsaï à l'arbre-monde (`SeedTree`)
- Essences — **fait en 0.185** (visuel « L'arbre ») : bouleau (écorce blanche), saule pleureur, érable rouge, cerisier en fleur, baobab, séquoia, pin, palmier ; chaque essence un skin, débloqué par un univers formé à partir du quatrième (4 à 11). Le chêne est le Classique.
- Saisons figées — **fait en 0.197** (condition retenue : les saisons débloquées, soit le miroir ouvert à 9 univers, puis 10 et 11 univers formés) : l'arbre toujours en automne (roux), en hiver (nu, givré), au printemps (fleurs) ; les saisons débloquées.
- Fantaisie — **fait en 0.197** (condition retenue : des bulles de l'arbre de 1 à 6 paliers différents de la graine achetées) : arbre de cristal, arbre de corail, arbre en circuit lumineux, arbre de nuages, arbre inversé (les racines en haut), arbre en pixel art ; chaque bulle d'un palier différent.
- Le sol — **fait en 0.197** (condition retenue : la graine aux paliers 2 à 7, l'univers formé) : herbe, sable, neige, mousse, dallage, eau (l'arbre pousse sur une île) ; chaque sol par un palier de la graine.

### Les bulles de l'arbre (`SeedView`, arbre d'améliorations)
- Bulles — **fait en 0.197** (condition retenue : 5, 10, 15, 18 bulles achetées, puis les dix-neuf ; l'arbre n'a que dix-neuf bulles) : en fruits (achetée : mûre ; trop chère : verte ; fermée : bourgeon), en lanternes, en gouttes de rosée, en pierres précieuses, en graines ; cinq, dix, quinze, toutes les bulles.
- Les liens entre bulles — **fait en 0.197** (condition retenue : 10, 15 bulles achetées, puis les dix-neuf) : en branches, en fils de lumière, en racines ; dix bulles.

### La carte des dimensions et le défi final (`DimensionsView`)
- Les onze points — **fait en 0.197** (condition retenue : 2, 4, 6, 8, 10 dimensions ouvertes) : en planètes d'un système, en perles d'un collier, en pierres d'un cercle de menhirs, en fenêtres d'une tour, en cases d'un cadran ; chaque dimension ouverte en débloque un.
- Le remplissage vert du défi final — **fait en 0.197** (condition retenue : le défi final fini, puis fini avec 9, 10, 11 univers formés) : en eau qui monte, en sable, en lumière, en lierre qui grimpe ; le défi final fini.

### Les dix mini-jeux (`DimensionView`, `DimensionWindow`, `VolumeCamera`)
- La ligne — **rails et wagons, fait en 0.187** (visuel « La ligne », au seuil) ; restent à faire : cordes de guitare et médiator, fil et perles d'abaque, piste d'athlétisme, portée musicale et notes ; le seuil, puis chaque niveau d'amélioration en débloque un.
- Le plan — **feuille de cahier, fait en 0.187** (au seuil) ; restent à faire : damier, carte de jeu vidéo vue de haut, surface d'eau avec ronds, tapis persan ; le seuil du plan.
- Le volume — **aquarium (boules en poissons), fait en 0.187** (au seuil) ; restent à faire : boîte de Rubik, espace infini étoilé, intérieur d'une lanterne de papier, caverne de cristaux ; le seuil du volume.
- La durée — **vinyle, fait en 0.187** (au seuil) ; restent à faire : spirales en coquillage, en escalier à vis, en horloge, en galaxie ; le seuil de la durée.
- La boucle — **cible de tir à l'arc, fait en 0.187** (au seuil) ; restent à faire : anneaux de Saturne, pistes de patinage, cadrans d'horloges imbriqués, disques de DJ ; le seuil de la boucle.
- La brane — **toile d'araignée, fait en 0.187** (au seuil) ; restent à faire : drap tendu, surface d'eau, membrane de tambour, nappe de pique-nique ; le seuil de la brane.
- Le clicker — **bulles de savon, fait en 0.187** (au seuil) ; restent à faire : taupinières, lanternes, champignons, cibles de fête foraine ; le seuil du clicker.
- La grille — **démineur, fait en 0.187** (au seuil) ; restent à faire : bataille navale, mots croisés, de jardin potager, de ruche ; le seuil de la grille.
- Le miroir — **vitrail symétrique, fait en 0.187** (au seuil) ; restent à faire : miroir de bal, lac au reflet, test de Rorschach, papillon ; le seuil du miroir.
- Le mélange — **kaléidoscope, fait en 0.187** (au seuil) ; restent à faire : cuisine (chaque dimension un ingrédient), orchestre, atelier d'alchimiste, carte du monde ; le seuil du mélange.
- Les cadres (`DimensionWindow`) — **fait en 0.197** (condition retenue : 4, 6, 8, 10 dimensions ouvertes) : bordure de hublot, de cadre doré, d'écran cathodique, de livre ouvert ; dix dimensions ouvertes.
- La palette des dix dimensions (PALETTE) — **fait en 0.197** (libres ; la palette suit dans l'arène, la fenêtre et la carte skinnée) : en bloc : « pastel », « daltonien sûr », « monochrome », « arc-en-ciel saturé », « sépia » ; options d'accessibilité, sans condition.

## 8. Profil, succès, statistiques, réglages

### Les avatars (`Avatar`, `AvatarView`, 12 avatars + 1 importé)
- Cadres — **fait en 0.189** (visuel « Le cadre de l'avatar ») : rond (Alchimiste), hexagone (Lecteur du tableau), écusson (Briseur de défis), cadre doré baroque (Faiseur de Big Bang), timbre-poste dentelé (Chasseur de comètes), polaroid (Créateur d'univers).
- Styles — **fait en 0.198** (condition retenue : 25, 50, 75 succès obtenus, puis tous ceux du catalogue ; visuel « Le style des avatars », sur l'en-tête et la galerie, pas l'image importée) : chaque avatar en version « trait », « pixel », « craie », « vitrail » ; un jeu complet par palier de succès (25, 50, 75, tous).
- Nouveaux avatars à gagner — **fait en 0.198** (condition retenue : le titre quand il existe : hadron « Tisseur de gluons », bulle « Tisseur de multivers », miroir « Gardien du miroir », kaléidoscope « Maître du mélange », toile « Créateur d'univers » ; sinon : sonde 10 voyages de sondes, arrosoir le tuyau d'arrosage acheté, chrono le chrono acheté) : l'arrosoir, le chrono, la sonde, le hadron, la bulle, le miroir, le kaléidoscope, la toile de l'univers ; chacun par le titre correspondant.

### Les albums (`AlbumPane`, aujourd'hui du texte)
- Les cases d'album — **fait en 0.198** (condition retenue : 50 entrées trouvées dans les quatre albums ensemble ; visuel « Les albums », skin « Illustré ») : avec le dessin de la molécule (`MoleculeArt`), de l'astre (`BodyArt`), de l'assemblage (son bloc) ; cinquante entrées.
- Styles d'album — **fait en 0.198** (condition retenue : 1, 2, 3 albums complets, puis les 4 ; chaque style inclut l'illustration) : vignettes autocollantes de collection, timbres, cartes de collection avec dos, fiches de herbier ; chaque album complet.

### Les succès (`AchievementsPage`)
- Tuiles — **fait en 0.198** (condition retenue : 25, 50, 75, 90 succès obtenus, puis tous ceux du catalogue ; visuel « Les tuiles des succès », un insigne en tête de tuile ; le métal suit le palier du succès dans sa liste, or pour les succès à bonus propre) : en médailles (bronze, argent, or selon la rareté), en badges de scout brodés, en étoiles, en trophées, en timbres ; vingt-cinq, cinquante, soixante-quinze, tous.
- Le succès non obtenu — **fait en 0.198** (condition retenue : 10, 20, 30 succès obtenus ; visuel « Les succès non obtenus ») : en silhouette grise, en « ? », en pièce de puzzle manquante ; dix succès.

### Les statistiques et graphiques (`ChartPane`, `TimelinePane`, `MultiplierPane`)
- Courbes — **fait en 0.198** (condition retenue : 1, 5, 10, 20 h de jeu ; visuel « Les courbes des statistiques », tous les graphiques) : en « papier millimétré », en « oscilloscope » (vert sur noir, rémanence), en « crayon », en « aquarelle » (aires lavées) ; dix heures de jeu.
- Palette des séries — **fait en 0.198** (libres ; visuel « La palette des courbes » ; la daltonien-sûre reste le Classique) : « daltonien sûr » (l'actuelle), « pastel », « néon », « niveaux de gris » ; sans condition.
- La frise des univers — **fait en 0.198** (condition retenue : 3, 5, 7 univers formés ; visuel « La frise des univers ») : en frise chronologique de musée, en pellicule de film, en train avec un wagon par univers ; cinq univers.

### Notifications, panneaux, transitions (`ToastPane`, `Overlay`, `TransitionPane`, `EndPane`, `FinalePane`)
- Toasts — **fait en 0.199** (condition retenue : 20, 50, 100, 200, 500 notifications affichées depuis l'ouverture du jeu, compte tenu par la fenêtre et non sauvegardé ; visuel « Les notifications ») : en post-it, en télégramme, en bulle de bande dessinée, en ticket de caisse, en parchemin déroulé ; cent notifications.
- Panneaux (aide, fin, absence) — **fait en 0.199** (condition retenue : 5, 10, 20 h de jeu ; visuel « Les panneaux » ; la fin suit le visuel de l'écran de fin quand un de ses skins est choisi) : en page de livre, en écran de cinéma avec rideau, en tableau d'affichage ; dix heures.
- La transition entre univers — **fait en 0.199** (condition retenue : 1, 2, 3, 4 univers formés ; visuel « La transition entre univers » : la carte du guide et un rideau joué à la confirmation) : en fondu au blanc, en page tournée, en diaphragme d'appareil photo, en trou noir qui avale ; chaque univers formé.
- L'écran de fin de partie — **fait en 0.199** (condition retenue : la partie finie, puis finie et 10, 11 univers formés, le jeu ne comptant qu'une fin ; visuel « L'écran de fin », sur `EndPane` et `FinalePane`) : en générique de film qui défile, en certificat encadré, en photo souvenir ; chaque fin de partie.

### Le bouton de démarrage et le premier écran (`GameAppWindow.showStartButton`)
- Bouton — **fait en 0.199** (condition retenue : la partie finie, puis finie et 9, 10, 11 univers formés ; visuel « Le bouton de démarrage » ; « Recommencer » efface les skins, le bouton de la partie neuve garde celui qui était choisi le temps de cette ouverture du jeu) : en interrupteur à bascule, en graine qu'on plante, en allumette qu'on frotte, en détonateur ; chaque fin de partie en débloque un.

### Curseurs (`CursorSkinArt`, version 0.199 ; avant, seulement les curseurs système)
- Curseur — **fait en 0.199** (condition retenue : les succès « Premier automatisme », « Attrapée au vol », « Jeune pousse », « À mi-chemin », « Dix explosions », un onglet chacun ; visuel « Le curseur », image dessinée au code posée sur la fenêtre, l'arrosoir seulement sur l'onglet Univers et la loupe sur l'onglet Big Bang ; le curseur système reste le Classique) : en main de verre, en étoile filante (traînée), en arrosoir sur la graine, en loupe sur l'espace, en baguette ; chaque onglet pourrait avoir le sien, débloqué par un succès de l'onglet.

### Sons (`Sound`, synthétisés)
- Banques de sons — **fait en 0.200** (condition retenue : 1, 2, 5, 10, 20 h de jeu ; « Silence partiel » libre ; visuel « La banque de sons », synthèse au code dans `SoundBanks`, chaque banque change le timbre de tous les sons) : « bois » (xylophone, claves), « verre » (carillon), « 8 bits » (ondes carrées), « orchestre » (cordes), « nature » (gouttes, vent, oiseaux), « silence partiel » (seulement les Big Bangs) ; chacun un palier de temps de jeu.
- Le Big Bang — **fait en 0.200** (condition retenue : 5, 10, 20 Big Bangs ; visuel « Le son du Big Bang » ; le grondement actuel reste le Classique) : grondement actuel, carillon géant, souffle de vent, orgue ; chaque palier de Big Bang.
- Le métronome de la boucle — **fait en 0.200** (condition retenue : le seuil de la boucle, puis le seuil et 6, 7, 8 univers formés ; visuel « Le métronome de la boucle ») : bois, métal, goutte d'eau, battement de cœur ; le seuil de la boucle.

### Thèmes (`ThemeFilter`, 11 thèmes dont 5 à débloquer avant la 0.200 ; 19 dont 12 depuis)
- Nouveaux thèmes en filtre — **fait en 0.200** (condition retenue : des énigmes dans `Cosmetic` : 1 000 fusions, le défi Rouille réussi, un Big Bang entre minuit et 6 h, 3 systèmes complets, 10 comètes, la galaxie formée, 100 explosions ; « Daltonien » libre ; un voile « écran » nouveau colore les noirs ; « Vieux film » sans grain) : « Encre » (noir et blanc tranché), « Cyanotype » (bleu de Prusse `#1a3a6b` et blanc), « Vieux film » (sépia granuleux), « Thermique » (palette fausses couleurs), « Daltonien » (saturation et teintes vérifiées), « Lune » (gris bleuté), « Forêt » (verts), « Brasier » (rouges et oranges) ; chacun par un succès à trouver, dans l'esprit des cinq existants (une énigme dans sa description).

### Notations (`Format`, 8 notations dont 3 à débloquer avant la 0.200 ; 14 dont 9 depuis)
- **fait en 0.200** (condition retenue : des énigmes dans `Cosmetic` : exactement 1 024 particules, exactement 255 de matière noire, 10 000 molécules créées, 100 notifications reçues d'une traite, 1e30 particules, le chrono plein ; « Battements de cœur » laisse les nombres en scientifique et écrit les durées en battements ; au-delà de leur portée, les notations replient sur ×10^… ou la scientifique) : « Binaire » (puissances de 2), « Hexadécimale », « Chiffres japonais » (万, 億, 兆…), « Morse » (fantaisie), « Préfixes SI jusqu'au quetta », « Durées en battements de cœur » pour les temps ; chacune une énigme comme les trois existantes.

## 9. Où tout cela est rangé dans le code

- Tout est réalisé (versions 0.185 à 0.200) : chaque ligne des sections 1 à 8 porte sa mention « fait en » et la condition retenue ; 369 skins (`Skin`, 92 visuels dans `SkinVisual`), dix-neuf thèmes et quatorze notations (`Settings.Theme`, `Settings.Notation`, les énigmes dans `Cosmetic`).
- Les skins se débloquent dans `GameUniverse.noteSkinUnlocks()` et ses méthodes `noteXxxSkins()` ; les skins libres (accessibilité, versions figées ou silencieuses) sont marqués `true` dans `Skin` et ne comptent pas dans le bonus (plafond `Skin.MAX_COUNTED`) ; la page Skins les range par visuel.
- Les dessins par skin vivent dans des classes voisines (`XxxSkinArt`, `XxxSkinStyles`, `SoundBanks`, `Notations`) ; les thèmes et notations à débloquer se guettent dans `Cosmetics` et se choisissent dans les réglages.
