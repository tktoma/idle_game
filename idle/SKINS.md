# Les skins à venir : tout ce qui peut changer d'apparence

Version 0.180, triée en 0.184 : onze idées retirées (fruit, serpents, anneau de Saturne, case du Big Bang, sucre, bestiaire, mécanique, skins par échelle, cartes de butin, arbre-monde, thème d'après le skin), l'horlogerie refaite en engrenages par particule ; 167 visuels restent. Une liste de travail, pas un engagement : chaque ligne est un visuel du jeu qui pourrait recevoir une
apparence de rechange, avec des idées de skins, ce que chacun changerait, et une façon de l'obtenir dans l'esprit des
skins existants (un record, une recherche, un défi, un palier). Le mécanisme ne change pas : un skin débloqué donne
+2 % à toutes les ressources d'en dessous de l'univers (`Skin.BONUS`), le choisir ne change que l'apparence, et
« Classique » reste toujours là.

Ce qui existe (version 0.177) : huit visuels (`SkinVisual`), dix-neuf skins (`Skin`), cinq thèmes et trois notations
à débloquer (`Cosmetic`). Tout le reste de cette liste est sans skin aujourd'hui.

Conventions : une ligne par skin, « nom — ce qui change (formes, couleurs, mouvement) ; comment on l'obtient ».
Les couleurs sont données en hex quand elles comptent. Les noms des fichiers sont ceux de `ui/src/main/java/idle/ui`.

## 1. Ce qui est déjà skinné, et ce qu'on peut y ajouter

### La fusion des particules (`ParticleView`, visuel PARTICLE_FUSION : Éclair, Prisme, Solitaire)
- Braise — des poussières orange `#ffa53a` qui rougissent en approchant, un noyau de charbon craquelé qui luit par les fentes ; dix minutes sans acheter de générateur, la production en marche.
- Encre — fond blanc crème dans la carte, poussières noires qui coulent comme des gouttes d'encre vers le centre, noyau en tache ; mille fusions.
- Essaim — les poussières deviennent de petits traits qui volent en banc, changent de cap d'un coup, le noyau est une ruche hexagonale ; cent générateurs formés d'un seul coup (touche M tenue).
- Horloge — le noyau est un cadran, les anneaux d'accrétion des aiguilles, chaque particule créée fait avancer la trotteuse ; une heure d'affilée au régime lent.
- Verre soufflé — poussières en bulles irisées (teinte HSB lente), noyau en goutte de verre avec un reflet blanc ; la recherche « Fusion prismatique » et le skin Prisme déjà choisis une heure.
- Pixel — tout en carrés de 4 px, couleurs à 8 valeurs, ondes carrées ; un record de reconstruction en moins de trente secondes.

### Le grand atome (`AtomModelView`, visuel ATOM : Noyau dense, Noyau stellaire)
- Modèle de Bohr au tableau noir — fond ardoise `#1d2a24`, orbites tracées à la craie (traits irréguliers, poussière), électrons en ronds de craie blanche ; cinquante atomes.
- Nuage de probabilité — plus d'orbites : des nuages de points `#ffe9c2` plus denses là où l'électron a des chances d'être, qui respirent ; cent atomes.
- Système à anneaux — le noyau est une planète à bandes, les couches des anneaux de glace vus de biais ; le 118ᵉ atome une première fois.
- Horlogerie — chaque particule de l'atome est un petit engrenage : les protons et les neutrons du noyau en roues dentées de laiton `#c9a24a` serrées les unes contre les autres, les électrons en pignons minuscules qui tournent sur leur couche et s'engrènent au passage ; une heure de synthèse automatique sans intervention.
- Vitrail — couches en plomb noir, électrons en verres colorés par famille du tableau, lumière qui passe au travers ; toutes les familles possédées une fois.

### Le tableau périodique (`PeriodicTablePage`, visuel TABLE : Plomb, Or)
- Bois gravé — cases en planchettes `#8a6440` au grain visible, symbole brûlé au fer, possédées vernies ; cinquante éléments possédés.
- Cristal — cases en facettes translucides de la couleur de la famille, bordures blanches, les cases au maximum scintillent ; une famille complète au maximum.
- Rouille — métal piqué `#6e4a3a`, une case possédée est polie, une case au maximum chromée ; le défi « Rouille » réussi.
- Néon — cases noires au trait de néon de la famille, texte qui grésille ; le défi « Sans surchauffe » réussi (comme Néon du collisionneur, pour faire une paire).
- Carte au trésor — parchemin `#d8c49a`, cases dessinées à l'encre brune, les possédées marquées d'une croix rouge, les uniques d'une étoile dorée ; dix voyages de sondes.
- Minuscule — cases réduites à leurs lettres, sans fond, alignées comme une partition : pour les grands écrans ; option libre, dès le premier élément.

### La boule de matière noire (`DarkMatterView`, visuel DARK_BALL : Obsidienne, Nébuleuse, Contractée)
- Goutte de mercure — sphère miroir `#c0c8d0` qui reflète la règle graduée, tremble à chaque appui ; une explosion sans aucune matière noire gaspillée (tout dépensé avant).
- Trou noir — disque noir net, anneau de lumière lentillée orange en haut, l'échelle des grandeurs se déforme autour ; dix explosions.
- Œuf — coquille `#efe4ff` tachetée qui se fend à mesure que la boule grossit, l'explosion la fait éclore ; le défi « Contraction douce » réussi deux fois.
- Nœud — une corde violette `#c9a6ff` nouée sur elle-même qui se resserre quand on appuie ; cent appuis à la main dans une heure.
- Lanterne — papier translucide, flamme à l'intérieur qui grandit avec la matière, l'explosion est un éclat de papier brûlé ; une heure d'appui automatique.

### L'animation de la matière noire (visuel DARK_ANIMATION : Vortex, Pulsar)
- Pluie — des gouttes qui tombent droit, flaques en cercles au sol de la vue ; quinze explosions.
- Neige — flocons lents `#efe4ff` qui tourbillonnent et se posent ; une explosion déclenchée entre minuit et six heures (heure de l'ordinateur).
- Sablier — des grains qui coulent par un goulot vers la boule, un sablier dessiné en filigrane ; une heure sans explosion alors qu'elle était possible.
- Feu d'artifice — l'explosion plein écran devient une gerbe de fusées de six couleurs qui retombent ; cent explosions.

### Le Big Bang (`SpaceView`, `BurstView`, visuel BIG_BANG : Aurore, Supernova, Cristal)
- Encre de Chine — fond blanc cassé `#f4efe4`, étoiles en points d'encre, disque d'espace en lavis gris, bord au pinceau ; cinq Big Bangs.
- Vitrail — fond noir, ciel en verres colorés cernés de plomb, le disque est une rosace ; trois systèmes solaires complets.
- Abysses — fond `#02060e`, étoiles qui deviennent des points de plancton lumineux, le disque une méduse qui pulse, le bord des tentacules ; la planète océan formée.
- Papier plié — ciel en origami aux plis visibles, disque à facettes planes, étoiles en confettis de papier ; le défi « Big Bang à froid » réussi deux fois.
- Carte ancienne — fond parchemin, étoiles reliées en constellations nommées en latin, disque en rose des vents ; vingt-cinq comètes attrapées.
- Néon de ville — fond asphalte mouillé, disque en enseigne qui clignote, étoiles en fenêtres allumées ; cent molécules de la chimie du carbone.

### Le faisceau du collisionneur (`EnergyView`, visuel COLLIDER_BEAM : Faisceau doré)
- Comète — deux boules à longue queue de glace `#bfe8ff`, les croisements font neiger ; vingt-cinq comètes attrapées.
- Encre — deux traits de plume noire sur fond clair, le croisement une tache ; mille croisements captés.
- Laser — deux rayons fins et droits par segments, sans traînée, étincelles en pixels ; une heure à la plus haute énergie.
- Lucioles — nuées de points verts clignotants au lieu de boules, le croisement les fait toutes s'allumer ; cent captures à la main.

### L'anneau du collisionneur (visuel COLLIDER : Néon, Plasma, Glace)
- Bois et cuivre — anneau en lattes vernies cerclées de cuivre `#b87333`, poussières en sciure ; dix générateurs du collisionneur.
- Circuit imprimé — anneau en piste verte `#1f5a2a` avec pastilles dorées, poussières en bits qui passent ; tous les générateurs achetés.
- Stade — anneau en piste d'athlétisme à huit couloirs, détecteur en ligne d'arrivée ; cent tours du faisceau rapide.
- Ruban de Möbius — l'anneau se tord sur lui-même, les faisceaux passent dessus puis dessous ; le défi « Un seul générateur » réussi deux fois.

## 2. Le chrome : fenêtre, onglets, cartes

### Le fond et les barres (`GameAppBase`, `GameAppTabs`, `GameAppWindow`)
- Papier — fond `#f4efe4` et texte sombre : un vrai thème clair dessiné, pas l'inversion ; une heure de jeu en thème Clair.
- Ardoise — fond `#1d2a24` à la craie, bordures en traits de craie ; cinquante atomes.
- Nuit étoilée — un fond qui laisse voir un ciel très sombre avec quelques étoiles fixes derrière les pages ; la galaxie formée.
- Terminal — fond noir pur, texte vert `#6dff8f`, bordures en caractères ASCII ; la notation « Logarithme » débloquée.
- Blueprint — fond bleu de plan `#1a3a6b`, lignes blanches fines, quadrillage discret ; dix assemblages formés.

### Les onglets (`tabStyle`, couleurs par onglet)
- Onglets en onglets de classeur — forme trapèze, papier coloré, le courant dépasse ; cinq onglets ouverts.
- Boutons physiques — relief, ombre portée, le courant enfoncé ; dix heures de jeu.
- Pastilles — l'onglet n'est qu'un rond coloré avec l'icône, le nom apparaît au survol : gagne de la place ; tous les onglets ouverts.
- Rubans — bandes verticales à gauche, la couleur de l'onglet en pleine hauteur ; l'univers 2 formé.

### Les icônes d'onglet (`TabIcon`, 13 icônes animées, et `AtomView`)
- Jeu complet « trait de craie » — toutes les icônes redessinées en un seul trait blanc tremblé ; cinquante atomes.
- Jeu « pixel art » — icônes en 12×12 pixels, animation en deux images ; un record de reconstruction en moins d'une minute.
- Jeu « constellation » — chaque icône est un groupe d'étoiles reliées ; la galaxie formée.
- Jeu « emoji » — icônes pleines et rondes, couleurs vives ; la bulle du bonsaï à droite.
- Immobiles — les mêmes icônes sans animation, pour les écrans lents : c'est déjà un réglage (TAB_ICONS), il pourrait devenir un skin « Figées ».

### Les cartes (`Card`, le bouton universel du jeu)
- Fiche cartonnée — fond crème, coins écornés, texte à la machine à écrire ; cent achats.
- Tuile de verre — fond translucide qui laisse voir le fond animé, bord lumineux à l'état prêt ; le skin Cristal du Big Bang choisi.
- Carte à jouer — bord blanc, coin avec le signe d'état comme une valeur, dos rouge quand fermée ; dix défis réussis.
- Étiquette — un rectangle avec un œillet et un trou, pendu à un fil, qui se balance au survol ; dix sondes revenues.
- Signes d'état — le jeu ● ○ ◐ ▶ □ ✓ × ! peut changer de famille : « runes », « formes géométriques pleines », « lettres » (P, A, E, M, C, F, V, A) ; chacun un skin, un succès de chaque onglet.

### Les briques communes (`SubTabs`, `BuyAmountRow`, `FilterBar`, `OrderPane`, `PlanBar`, `Breakdown`, `MatterText`)
- Signes des états de la matière (`MatterText` : ∴ ≈ ◆) — « lettres » (G L C), « gouttes et grains » (nuage, goutte, cube), « alchimie » (symboles alchimiques de l'air, de l'eau, de la terre) ; cent sortes rassemblées.
- Info-bulles (`Breakdown`) en parchemin, en post-it jaune `#f9eeda`, en terminal ; dix heures de jeu.

## 3. Particules et atomes

### Le fond animé des particules (`AmbientView`, 90 poussières)
- Neige, pluie, pollen (grains jaunes), lucioles, bulles qui montent, poussière d'or, cendres : sept skins du fond seulement, chacun un palier de particules (1e6, 1e9, 1e12…).
- Vide — aucun fond : pour les écrans lents, aujourd'hui un réglage (AMBIENT), pourrait devenir le skin « Silence ».

### Le rangement et la fusion des générateurs (`GeneratorPane`)
- Fusion en tourbillon — les générateurs tournent autour du centre avant de se confondre ; cent fusions.
- Fusion en aspiration — ils s'étirent vers le centre comme de la pâte ; mille fusions.
- Fusion en éclat — ils se brisent et les morceaux se rassemblent ; le skin Éclair choisi.

### La page du tableau périodique, hors cases (légende, synthèse, ensembles)
- Les tuiles des ensembles d'éléments en médailles de bronze, d'argent et d'or selon l'avancée ; un ensemble complet.
- La carte de synthèse en creuset d'alchimiste qui fume ; cent synthèses.
- La légende des familles en bandes de spectre (raies d'émission) ; toutes les familles possédées.

## 4. La matière noire

### L'échelle des grandeurs et la règle (`DarkMatterView`)
- Règle de bois — graduations gravées, chiffres au fer ; dix explosions.
- Règle d'architecte — plastique jaune translucide ; vingt explosions.
- Repères célestes — les cercles des objets connus deviennent les objets eux-mêmes dessinés en silhouette (un grain, une bille, la Terre, le Soleil, la galaxie) ; chaque objet dépassé une fois.

### L'arbre de matière noire (`DarkMatterTreePane`)
- Racines — les traits deviennent des racines brunes qui s'épaississent avec le niveau, les cases des bulbes ; trente cases achetées.
- Circuit — traits en pistes de cuivre, cases en puces ; l'arbre complet une fois.
- Constellation — traits en fils d'étoiles, cases en étoiles plus ou moins brillantes ; la galaxie formée.
- Métro — traits en lignes de métro colorées par branche, cases en stations ; cinq branches ouvertes.

### Les défis (`ChallengesPane`, l'arrivée de « la fuite »)
- Cartes de tarot — chaque défi une lame, l'arrivée les retourne ; trois défis réussis.
- Affiches « wanted » — papier déchiré, portrait du défi ; six défis.
- Sceaux — le défi réussi porte un sceau de cire rouge ; tous les défis.

## 5. Le Big Bang : espace, ciel, galaxie

### Les molécules (`MoleculeArt`, boules et bâtons)
- Boules pleines (space-filling) — pas de bâtons, atomes qui se touchent ; cinquante sortes.
- Traits (squelette) — seulement les liaisons, atomes en lettres ; cent sortes.
- Perles — atomes en perles nacrées, bâtons en fil ; vingt sortes rassemblées.
- Lego — atomes en briques à tenons, couleurs vives ; mille molécules.
- Palette CPK alternative : « pastel », « daltonien sûr » (une palette vérifiée), « monochrome par rayon » ; chacun dès le premier Big Bang (option d'accessibilité, sans condition).

### Les amas rassemblés, les blocs d'assemblage, le gaz (`SpaceView`, `SpaceViewPlacement`, `SpaceViewPacking`)
- Amas en flaques, en cristaux à facettes, en nuages de coton, selon l'état, avec une palette au choix : « minéraux » (vrai gris du quartz, bleu du cuivre…), « néon », « pastel » ; cinq, vingt, cinquante sortes rassemblées.
- Blocs d'assemblage en briques de pierre taillée, en lingots, en cristaux bruts, en pains de glace ; dix, vingt, trente assemblages.
- Le gaz flouté au loin en voile de brume, en fumée de cheminée, en aurore ; mille molécules de gaz.

### Les astres (`BodyArt` : 35 astres, 10 échelles, 12 aspects)
- Planétarium — tous les astres en sphères de laiton et de verre dépoli, sur des tiges ; dix astres.
- Gravure — astres en hachures noir et blanc façon gravure du XIXᵉ siècle ; vingt astres.
- Cartographie — astres en globes à méridiens et parallèles, nommés en lettres capitales ; trente-cinq astres.

### Le ciel des astres (`SkyView`)
- Planche naturaliste — fond parchemin, astres épinglés avec étiquette manuscrite ; quinze astres.
- Mobile — astres suspendus à des fils qui se balancent ; vingt-cinq astres.
- Vitrine — étagères de verre éclairées par le bas ; trente-cinq astres.

### La galaxie, l'amas de galaxies, la toile (`GalaxyView`, `Spiral`)
- Galaxie en coquillage (spirale de nautile), en pelote de laine, en empreinte digitale, en tourbillon d'évier, en roue de feu d'artifice ; la galaxie formée, puis chaque univers formé en débloque un.
- Amas en grappe de raisin, en guirlande de lampions, en essaim d'abeilles ; l'amas de galaxies formé.
- Toile de l'univers en réseau de neurones, en dentelle, en toile d'araignée au matin (gouttes), en carte de métro ; l'univers formé.

### Les systèmes solaires (`SystemView`)
- Orrery — orbites en cercles de laiton, planètes sur tiges ; un système complet.
- Dessin d'enfant — orbites au crayon de couleur, Soleil à rayons ; deux systèmes.
- Radar — fond vert, orbites en cercles de balayage, planètes en échos ; trois systèmes.

### Les sondes (`ProbesView`, `LootPane`)
- Sondes en avions de papier, en montgolfières, en bouteilles à la mer, en pigeons voyageurs ; dix, vingt-cinq, cinquante, cent voyages.
- Trajectoires en pointillés de carte au trésor, en traînées de condensation, en fil d'Ariane ; chacun avec le skin de sonde.

## 6. Collisionneur, hadrons, vide

### Le reste du collisionneur (détecteur, injecteur, éclairs, flammes, étincelles, quarks)
- Détecteur en œil, en lanterne, en cloche, en appareil photo ancien (le flash au croisement) ; cent captures.
- Éclairs en fils d'or, en fissures de glace, en racines ; mille croisements.
- Étincelles en pétales, en confettis, en braises ; le défi du détecteur.
- Quarks qui s'envolent en ballons, en papillons, en lettres de scrabble (la saveur) ; dix hadrons liés.

### Les hadrons (`BindingView`)
- Quarks en billes de verre, en fruits (la saveur en couleur de fruit), en notes de musique (la liaison joue un accord), en pièces de puzzle qui s'emboîtent ; dix, cinquante, cent, tous les hadrons.
- Gluons en ressorts de métal, en élastiques, en fils de laine, en éclairs ; cent gluons.
- Fond en tableau noir, en table de laboratoire, en ciel d'orage ; chacun un palier des hadrons.

### Le vide (`VoidView`, le trou noir du vide)
- Écume quantique en bulles de savon, en mousse de bière, en crépitement de pellicule, en neige de télévision ; le vide ouvert, puis chaque niveau d'amélioration du vide.
- Paires matière-antimatière en jumeaux qui se tiennent la main, en aimants, en yin et yang ; cent paires annulées.
- Le trou noir du vide en bouche d'égout, en puits de pierre, en iris, en nœud de bois, en tourbillon de thé ; chaque fois qu'il est nourri d'un trou noir.

## 7. L'univers

### Les univers en bulles (`UniverseView`, la naissance)
- Bulles de savon réalistes (irisation), ballons de baudruche noués, boules à neige, globes en verre d'un orrery, planètes-fruits, œufs ; chaque univers formé en débloque un.
- La naissance en éclosion, en bourgeon qui s'ouvre, en feu d'artifice, en page qui se tourne ; cinq univers.
- Les liens entre bulles en fil, en lierre, en circuit, en constellation ; dix univers.

### Le lancement de l'univers suivant (`LaunchView`)
- Fusée de papier, graine qui germe, pierre jetée dans l'eau (ondes), lampe qu'on allume, rideau de théâtre ; chaque univers lancé après le cinquième.

### La graine et son établi (`SeedView` : graine, germe, arrosoir, étagère, tuyau, chrono, rond, météo, saisons)
- La graine en noix, en gland, en noyau d'avocat, en pépin de pomme, en graine de tournesol, en œuf (fantaisie), en pierre précieuse brute ; chaque palier de la graine en débloque un.
- L'arrosoir en théière, en pichet, en nuage qu'on presse, en bec d'oiseau, en coquillage ; dix, cent, mille arrosages, le tuyau, la terre fertile.
- L'étagère en pierre, en métal d'atelier, en bambou, en nuage ; l'étage du chrono.
- Le tuyau en liane, en fontaine, en rigole creusée, en pipette de laboratoire ; dix niveaux du tuyau.
- Le chrono en sablier, en montre à gousset, en cadran solaire, en métronome ; le chrono plein une fois.
- Le rond de croissance en anneau de fée (champignons), en halo doré, en soleil levant ; vingt ronds attrapés.
- La météo en pictogrammes de bulletin télévisé, en gravure japonaise (pluie en traits), en pastel ; dix orages.
- Les saisons en cartes à jouer, en roue de couleurs, en vitrail ; un cycle complet des saisons.

### Les arbres, du bonsaï à l'arbre-monde (`SeedTree`)
- Essences — chêne, bouleau (écorce blanche), saule pleureur, érable rouge, cerisier en fleur, baobab, séquoia, pin, palmier ; chaque essence un skin, débloqué par un univers formé à partir du quatrième.
- Saisons figées — l'arbre toujours en automne (roux), en hiver (nu, givré), au printemps (fleurs) ; les saisons débloquées.
- Fantaisie — arbre de cristal, arbre de corail, arbre en circuit lumineux, arbre de nuages, arbre inversé (les racines en haut), arbre en pixel art ; chaque bulle d'un palier différent.
- Le sol — herbe, sable, neige, mousse, dallage, eau (l'arbre pousse sur une île) ; chaque sol par un palier de la graine.

### Les bulles de l'arbre (`SeedView`, arbre d'améliorations)
- Bulles en fruits (achetée : mûre ; trop chère : verte ; fermée : bourgeon), en lanternes, en gouttes de rosée, en pierres précieuses, en graines ; cinq, dix, quinze, toutes les bulles.
- Les liens entre bulles en branches, en fils de lumière, en racines ; dix bulles.

### La carte des dimensions et le défi final (`DimensionsView`)
- Les onze points en planètes d'un système, en perles d'un collier, en pierres d'un cercle de menhirs, en fenêtres d'une tour, en cases d'un cadran ; chaque dimension ouverte en débloque un.
- Le remplissage vert du défi final en eau qui monte, en sable, en lumière, en lierre qui grimpe ; le défi final fini.

### Les dix mini-jeux (`DimensionView`, `DimensionWindow`, `VolumeCamera`)
- La ligne — rails et wagons, cordes de guitare et médiator, fil et perles d'abaque, piste d'athlétisme, portée musicale et notes ; le seuil, puis chaque niveau d'amélioration en débloque un.
- Le plan — damier, feuille de cahier, carte de jeu vidéo vue de haut, surface d'eau avec ronds, tapis persan ; le seuil du plan.
- Le volume — aquarium (boules en poissons), boîte de Rubik, espace infini étoilé, intérieur d'une lanterne de papier, caverne de cristaux ; le seuil du volume.
- La durée — spirales en vinyle, en coquillage, en escalier à vis, en horloge, en galaxie ; le seuil de la durée.
- La boucle — cibles de tir à l'arc, anneaux de Saturne, pistes de patinage, cadrans d'horloges imbriqués, disques de DJ ; le seuil de la boucle.
- La brane — drap tendu, surface d'eau, membrane de tambour, toile d'araignée, nappe de pique-nique ; le seuil de la brane.
- Le clicker — taupinières, bulles de savon, lanternes, champignons, cibles de fête foraine ; le seuil du clicker.
- La grille — grille de bataille navale, de mots croisés, de démineur, de jardin potager, de ruche ; le seuil de la grille.
- Le miroir — miroir de bal, lac au reflet, vitrail symétrique, test de Rorschach, papillon ; le seuil du miroir.
- Le mélange — kaléidoscope, cuisine (chaque dimension un ingrédient), orchestre, atelier d'alchimiste, carte du monde ; le seuil du mélange.
- Les cadres (`DimensionWindow`) — bordure de hublot, de cadre doré, d'écran cathodique, de livre ouvert ; dix dimensions ouvertes.
- La palette des dix dimensions (PALETTE) en bloc : « pastel », « daltonien sûr », « monochrome », « arc-en-ciel saturé », « sépia » ; options d'accessibilité, sans condition.

## 8. Profil, succès, statistiques, réglages

### Les avatars (`Avatar`, `AvatarView`, 12 avatars + 1 importé)
- Cadres — rond, hexagone, écusson, cadre doré baroque, timbre-poste dentelé, polaroid ; chaque titre obtenu en débloque un.
- Styles — chaque avatar en version « trait », « pixel », « craie », « vitrail » ; un jeu complet par palier de succès (25, 50, 75, tous).
- Nouveaux avatars à gagner — l'arrosoir, le chrono, la sonde, le hadron, la bulle, le miroir, le kaléidoscope, la toile de l'univers ; chacun par le titre correspondant.

### Les albums (`AlbumPane`, aujourd'hui du texte)
- Les cases d'album avec le dessin de la molécule (`MoleculeArt`), de l'astre (`BodyArt`), de l'assemblage (son bloc) ; cinquante entrées.
- Styles d'album : vignettes autocollantes de collection, timbres, cartes de collection avec dos, fiches de herbier ; chaque album complet.

### Les succès (`AchievementsPage`)
- Tuiles en médailles (bronze, argent, or selon la rareté), en badges de scout brodés, en étoiles, en trophées, en timbres ; vingt-cinq, cinquante, soixante-quinze, tous.
- Le succès non obtenu en silhouette grise, en « ? », en pièce de puzzle manquante ; dix succès.

### Les statistiques et graphiques (`ChartPane`, `TimelinePane`, `MultiplierPane`)
- Courbes en « papier millimétré », en « oscilloscope » (vert sur noir, rémanence), en « crayon », en « aquarelle » (aires lavées) ; dix heures de jeu.
- Palette des séries : « daltonien sûr » (l'actuelle), « pastel », « néon », « niveaux de gris » ; sans condition.
- La frise des univers en frise chronologique de musée, en pellicule de film, en train avec un wagon par univers ; cinq univers.

### Notifications, panneaux, transitions (`ToastPane`, `Overlay`, `TransitionPane`, `EndPane`, `FinalePane`)
- Toasts en post-it, en télégramme, en bulle de bande dessinée, en ticket de caisse, en parchemin déroulé ; cent notifications.
- Panneaux (aide, fin, absence) en page de livre, en écran de cinéma avec rideau, en tableau d'affichage ; dix heures.
- La transition entre univers en fondu au blanc, en page tournée, en diaphragme d'appareil photo, en trou noir qui avale ; chaque univers formé.
- L'écran de fin de partie en générique de film qui défile, en certificat encadré, en photo souvenir ; chaque fin de partie.

### Le bouton de démarrage et le premier écran (`GameAppWindow.showStartButton`)
- Bouton en interrupteur à bascule, en graine qu'on plante, en allumette qu'on frotte, en détonateur ; chaque fin de partie en débloque un.

### Curseurs (aujourd'hui seulement les curseurs système)
- Curseur en main de verre, en étoile filante (traînée), en arrosoir sur la graine, en loupe sur l'espace, en baguette ; chaque onglet pourrait avoir le sien, débloqué par un succès de l'onglet.

### Sons (`Sound`, synthétisés)
- Banques de sons : « bois » (xylophone, claves), « verre » (carillon), « 8 bits » (ondes carrées), « orchestre » (cordes), « nature » (gouttes, vent, oiseaux), « silence partiel » (seulement les Big Bangs) ; chacun un palier de temps de jeu.
- Le Big Bang : grondement actuel, carillon géant, souffle de vent, orgue ; chaque palier de Big Bang.
- Le métronome de la boucle : bois, métal, goutte d'eau, battement de cœur ; le seuil de la boucle.

### Thèmes (`ThemeFilter`, 11 thèmes dont 5 à débloquer)
- Nouveaux thèmes en filtre : « Encre » (noir et blanc tranché), « Cyanotype » (bleu de Prusse `#1a3a6b` et blanc), « Vieux film » (sépia granuleux), « Thermique » (palette fausses couleurs), « Daltonien » (saturation et teintes vérifiées), « Lune » (gris bleuté), « Forêt » (verts), « Brasier » (rouges et oranges) ; chacun par un succès à trouver, dans l'esprit des cinq existants (une énigme dans sa description).

### Notations (`Format`, 8 notations dont 3 à débloquer)
- « Binaire » (puissances de 2), « Hexadécimale », « Chiffres japonais » (万, 億, 兆…), « Morse » (fantaisie), « Préfixes SI jusqu'au quetta », « Durées en battements de cœur » pour les temps ; chacune une énigme comme les trois existantes.

## 9. Comment ranger tout ça dans le code

- Chaque élément ci-dessus qui n'est pas encore dans `SkinVisual` y gagnerait une entrée (AMBIENT, TAB_ICONS, CARD, DARK_TREE, MOLECULE, BODY, SKY, GALAXY, SYSTEM, PROBE, LOOT, HADRON, VOID, UNIVERSE_BUBBLE, LAUNCH, SEED, WATERING_CAN, TREE, TREE_BUBBLE, DIMENSIONS_MAP, DIMENSION_1 à DIMENSION_10, AVATAR_FRAME, ALBUM, ACHIEVEMENT, CHART, TOAST, TRANSITION, CURSOR, SOUND) ; les skins eux-mêmes dans `Skin`, avec leur `source`, et la page Skins les range déjà par visuel.
- Les palettes d'accessibilité (daltonien, pastel, monochrome) et les versions « figées » ou « silencieuses » devraient être libres dès le début : ce sont des réglages déguisés en skins, sans bonus.
- L'ordre proposé pour commencer, par ce que le joueur regarde le plus longtemps : les arbres (7 essences), les astres (planétarium, gravure, bestiaire), les dix mini-jeux (un skin chacun), les molécules (boules pleines, perles), les avatars (cadres), puis le chrome (cartes, onglets, fonds).
