package idle.ui;

import java.util.List;

/** Les notes de version de la 0.199 à la 0.124 (séparées de {@link Version} en 0.293 pour rester sous 1 000 lignes). */
final class VersionNotes100 {
    static final List<Version.Note> NOTES = List.of(
            new Version.Note("0.199", "Vingt-quatre skins pour la fenêtre autour du jeu : notifications, panneaux, transition, fin, démarrage, curseur", List.of(
                    "Six visuels de plus dans l'onglet Skins. Les notifications : Post-it (jaune, collé de travers), Télégramme (en capitales, "
                            + "terminé par STOP), Bulle de BD, Ticket de caisse (étroit, bords crantés), Parchemin déroulé, à 20, 50, 100, 200 et "
                            + "500 notifications reçues depuis l'ouverture du jeu ; ce compte n'est pas sauvegardé, il repart à chaque lancement, "
                            + "mais les skins obtenus restent.",
                    "Les panneaux par-dessus la fenêtre (l'aide, l'absence, la fin de l'univers et la fin de partie faute d'un skin de fin) : "
                            + "Page de livre à cinq heures de jeu, Écran de cinéma (rideau rouge en voile, écran blanc) à dix, Tableau d'affichage "
                            + "(liège et feuille punaisée) à vingt. Sur un fond clair, les textes passent à l'encre en gardant leur teinte.",
                    "Le guide de transition entre univers et le rideau joué quand on confirme : Fondu au blanc, Page tournée, Diaphragme, Trou "
                            + "noir, à un, deux, trois et quatre univers formés.",
                    "L'écran de fin (l'univers formé et la partie finie) : Générique qui défile (tout noir, la carte monte) à la partie finie, "
                            + "Certificat encadré à dix univers formés, Photo souvenir (polaroïd de travers) à onze.",
                    "Le bouton de démarrage : Interrupteur à la partie finie, Graine qu'on plante, Allumette, Détonateur à neuf, dix et onze "
                            + "univers formés. « Recommencer » efface les skins avec le reste ; le bouton de la partie neuve garde pourtant celui "
                            + "qui était choisi, le temps de cette ouverture du jeu.",
                    "Le curseur, dessiné au code et posé sur toute la fenêtre : Main de verre (succès « Premier automatisme »), Étoile filante "
                            + "avec sa traînée d'étincelles (« Attrapée au vol »), Arrosoir sur l'onglet Univers seulement (« Jeune pousse »), "
                            + "Loupe sur l'onglet Big Bang seulement (« À mi-chemin »), Baguette (« Dix explosions »). Le curseur système reste "
                            + "le Classique.",
                    "Le rendu réel n'a pas été vu pendant le développement.")),
            new Version.Note("0.198", "Vingt-sept skins et huit avatars pour le profil, les succès et les statistiques", List.of(
                    "Sept visuels de plus dans l'onglet Skins. Le style des avatars du jeu, qui redessine l'en-tête du profil et toute la "
                            + "galerie, pas l'image importée : Trait (les contours à l'encre sur du papier), Pixel, Craie (sur une ardoise), "
                            + "Vitrail (des panneaux de verre entre des plombs), à 25, 50, 75 succès obtenus, puis tous.",
                    "Huit avatars de plus dans la galerie : le hadron (titre « Tisseur de gluons »), la sonde (dix voyages de sondes), la "
                            + "toile de l'univers (« Créateur d'univers »), la bulle d'univers (« Tisseur de multivers »), l'arrosoir (le "
                            + "tuyau d'arrosage acheté), le chrono (le chrono acheté), le miroir (« Gardien du miroir »), le kaléidoscope "
                            + "(« Maître du mélange »).",
                    "Les albums du profil : Illustré met sur chaque case trouvée le dessin de la molécule, de l'astre ou du bloc de "
                            + "l'assemblage, dans les skins du Big Bang choisis (50 entrées dans les albums, tous ensemble) ; Vignettes "
                            + "autocollantes, Timbres, Cartes de collection (avec leur dos sur les cases vides) et Herbier l'habillent (un, "
                            + "deux, trois albums complets, puis les quatre).",
                    "Les tuiles des succès obtenus portent un insigne : Médailles (bronze, argent, or selon le palier du succès dans sa "
                            + "liste ; or pour les succès à bonus propre), Badges brodés, Étoiles, Trophées, Timbres (25, 50, 75, 90 succès "
                            + "obtenus, puis tous) ; les succès non obtenus le leur : Silhouette grise, Point d'interrogation, Pièce manquante "
                            + "(10, 20, 30 succès).",
                    "Les courbes des statistiques : Papier millimétré, Oscilloscope (vert sur noir, un réticule, la rémanence du trait), "
                            + "Crayon (graphite à main levée), Aquarelle (l'aire sous la courbe lavée), à 1, 5, 10 et 20 heures de jeu ; la "
                            + "palette des séries, libre et sans bonus : Pastel, Néon, Niveaux de gris, la daltonien-sûre restant celle "
                            + "d'origine ; la frise des univers de la chronologie : Musée, Pellicule, Train (une locomotive, un wagon par "
                            + "univers), à 3, 5 et 7 univers formés.",
                    "Le rendu réel n'a pas été vu pendant le développement.")),
            new Version.Note("0.197", "Quarante-un skins pour l'arbre, ses bulles, la carte des dimensions, le défi final et les mini-jeux", List.of(
                    "Neuf visuels de plus dans l'onglet Skins, qui se combinent avec l'essence de l'arbre. La saison de l'arbre, figée quel "
                            + "que soit le temps : Automne figé (les saisons débloquées), Hiver figé (les saisons débloquées, 10 univers formés), "
                            + "Printemps figé (11 univers formés) ; l'arbre de fantaisie, qui remplace le feuillage et le bois : Cristal, Corail, "
                            + "Circuit lumineux, Nuages, Inversé (les racines en haut), Pixel art (des bulles de l'arbre de 1 à 6 paliers "
                            + "différents achetées) ; le sol de l'arbre : Herbe, Sable, Neige, Mousse, Dallage, Eau (l'arbre sur une île), par "
                            + "la graine aux paliers 2 à 7.",
                    "Les bulles de l'arbre : Fruits (achetée : mûre ; verte sinon ; fermée : un bourgeon), Lanternes, Gouttes de rosée, "
                            + "Pierres précieuses, Graines (5, 10, 15, 18 bulles achetées, puis toutes les dix-neuf) ; leurs liens : Branches, "
                            + "Fils de lumière, Racines (10, 15 bulles, puis toutes).",
                    "La carte des dimensions : Planètes, Perles, Menhirs, Fenêtres d'une tour, Cadran (2, 4, 6, 8, 10 dimensions ouvertes) ; "
                            + "le remplissage du défi final : Eau, Sable, Lumière, Lierre (le défi final fini, puis avec 9, 10, 11 univers formés) ; "
                            + "le cadre des mini-jeux : Hublot, Cadre doré, Écran cathodique, Livre ouvert (4, 6, 8, 10 dimensions ouvertes) ; la "
                            + "palette des dix dimensions, en bloc et partout (arène, fenêtre, carte skinnée) : Pastel, Daltonien sûr (Okabe et "
                            + "Ito), Monochrome, Arc-en-ciel saturé, Sépia, cinq options libres, sans condition ni bonus.",
                    "Le rendu réel n'a pas été vu pendant le développement.")),
            new Version.Note("0.196", "Cinquante-deux skins pour les univers en bulles, le lancement, la graine et son établi", List.of(
                    "Douze visuels de plus dans l'onglet Skins. Les bulles des univers : Bulle de savon, Ballon noué, Boule à neige, Globe "
                            + "d'orrery, Planète-fruit, Œuf (un par univers formé, du premier au sixième) ; la naissance d'un univers : Éclosion, "
                            + "Bourgeon, Feu d'artifice, Page tournée (les univers 5 à 8 formés) ; les liens entre les univers : Fil, Lierre, "
                            + "Circuit, Constellation (les univers 7 à 10 formés) ; le lancement de l'univers suivant : Fusée de papier, Graine "
                            + "qui germe, Pierre dans l'eau, Lampe, Rideau de théâtre (les univers 5 à 9 formés).",
                    "La graine : Noix, Gland, Noyau d'avocat, Pépin, Tournesol, Œuf, Pierre brute (ses paliers 1 à 7) ; l'arrosoir : Théière "
                            + "(10 arrosages de la graine en cours), Pichet (100), Nuage (1 000), Bec d'oiseau (le tuyau acheté), Coquillage (la "
                            + "terre fertile achetée) ; l'étagère : Pierre, Atelier, Bambou, Nuage (l'étage du chrono acheté, avec 3 à 6 univers "
                            + "formés) ; le tuyau : Liane, Fontaine, Rigole, Pipette (le tuyau aux niveaux 2, 4, 6, 10) ; le chrono : Sablier (le "
                            + "chrono plein), Montre à gousset, Cadran solaire, Métronome (le chrono plein avec 3, 4, 5 univers formés).",
                    "Le rond de croissance : Anneau de fée, Halo doré, Soleil levant (5, 20, 50 ronds cueillis depuis l'ouverture de la "
                            + "fenêtre) ; la météo : Bulletin télé, Gravure japonaise, Pastel (1, 5, 10 orages depuis l'ouverture) ; les saisons : "
                            + "Cartes à jouer, Roue de couleurs, Vitrail (1, 2, 3 cycles complets des saisons depuis l'ouverture). Ces trois "
                            + "comptes ne sont pas sauvegardés ; les skins gagnés le sont.",
                    "Le rendu réel n'a pas été vu pendant le développement.")),
            new Version.Note("0.195", "Trente-six skins pour le reste du collisionneur, les hadrons et le vide", List.of(
                    "Dix visuels de plus dans l'onglet Skins. Dans le collisionneur, le détecteur : Œil (100 collisions), Lanterne (250), "
                            + "Cloche (500), Appareil photo (1 000 ; le flash au croisement) ; les éclairs du croisement : Fils d'or (1 000 "
                            + "croisements des faisceaux), Fissures de glace (2 500), Racines (5 000) ; les étincelles : Pétales (le défi du "
                            + "détecteur), Confettis (l'épreuve « Sans surchauffe »), Braises (« Pleine charge ») ; les quarks qui s'envolent : "
                            + "Ballons (10 hadrons liés), Papillons (25), Lettres de Scrabble (50, la saveur en lettre et sa valeur).",
                    "Dans la liaison des hadrons, les quarks : Billes de verre (10 hadrons liés), Fruits (50, la saveur en couleur de fruit), "
                            + "Notes de musique (100 ; la liaison fait monter un accord de notes, dessiné et non joué), Puzzle (chaque sorte de "
                            + "hadron liée au moins une fois) ; les gluons : Ressorts (100 gluons), Élastiques (250), Laine (500), Éclairs "
                            + "(1 000) ; le fond : Tableau noir, Table de laboratoire, Ciel d'orage (les paliers de gluons « Mémoire des "
                            + "liaisons », « Automatismes liés », « Éléments liés »).",
                    "Dans le vide, l'écume : Bulles de savon (le vide ouvert), Mousse de bière, Pellicule, Neige de télévision (une "
                            + "amélioration du vide au niveau 1, 2, 3) ; les paires : Jumeaux (100 paires nées depuis l'ouverture de la "
                            + "fenêtre), Aimants (1 000), Yin et yang (10 000) ; le trou noir du vide : Bouche d'égout, Puits de pierre, Iris, "
                            + "Nœud de bois, Tourbillon de thé (le trou noir du vide ouvert quand il compte 1 à 5 univers, soit les univers 4 à 8 "
                            + "formés).",
                    "Le rendu réel n'a pas été vu pendant le développement.")),
            new Version.Note("0.194", "Trente-cinq skins pour le Big Bang au-delà des molécules : palette, amas, blocs, gaz, ciel, cosmos, systèmes, sondes", List.of(
                    "Dix visuels de plus dans l'onglet Skins. La palette des molécules, à côté de leur forme et combinée avec elle : Pastel, "
                            + "Daltonien sûr (la palette d'Okabe et Ito) et Monochrome par rayon, trois skins libres, sans bonus ; la couleur "
                            + "dominante des molécules, dans l'espace et la galaxie, la suit.",
                    "Dans l'expansion de la matière : les amas rassemblés (Minéraux à 5 sortes rassemblées, Néon à 20, Pastel à 50 ; flaques, "
                            + "cristaux à facettes ou nuages de coton selon l'état), les blocs d'assemblage (Pierre taillée à 10 assemblages, "
                            + "Lingots à 15, Cristaux bruts à 20, Pains de glace aux 30 : le catalogue n'en a pas 40) et le gaz au loin (Brume à "
                            + "1 000 molécules de gaz rassemblées, Fumée à 10 000, Aurore à 100 000).",
                    "Le ciel des astres : Planche naturaliste (15 astres formés dans un univers), Mobile (25), Vitrine (35). La galaxie : "
                            + "Nautile (la galaxie formée), Pelote de laine, Empreinte digitale, Tourbillon, Roue de feu d'artifice (les univers 2 "
                            + "à 5 formés). L'amas de galaxies : Grappe de raisin (l'amas formé), Lampions (univers 2), Essaim d'abeilles (univers "
                            + "3). La toile de l'univers : Neurones, Dentelle, Toile au matin, Métro (les univers 1 à 4 formés).",
                    "Les systèmes solaires : Orrery (1 système complet), Dessin d'enfant (2), Radar (3, les planètes en échos). Les sondes : "
                            + "Avion de papier (10 voyages), Montgolfière (25), Bouteille à la mer (50), Pigeon (100) ; la trajectoire suit le "
                            + "skin (pointillés de carte au trésor, condensation, fil d'Ariane, plumes), comme le dos de la carte du butin.",
                    "Le rendu réel n'a pas été vu pendant le développement.")),
            new Version.Note("0.193", "Dix skins pour la matière noire hors de la boule : l'échelle des grandeurs, l'arbre, les défis", List.of(
                    "Trois visuels de plus dans l'onglet Skins. L'échelle des grandeurs : Règle de bois (10 explosions), Règle "
                            + "d'architecte (20 explosions), Repères célestes (le succès « Plus grand que tout », les 26 paliers de "
                            + "taille), où les cercles des objets connus deviennent leur silhouette, du proton à l'univers observable.",
                    "L'arbre de matière noire : Racines (30 cases de l'arbre achetées), Circuit (100 niveaux achetés dans l'arbre, en "
                            + "tout, à la place de « l'arbre complet », que les bifurcations et les cases sans limite rendent impossible), "
                            + "Constellation (la galaxie formée), Métro (les quatre branches entamées : l'arbre n'en a pas cinq). Les "
                            + "traits changent d'allure et chaque case porte une vignette qui suit son niveau.",
                    "Les défis : Tarot (3 défis réussis ; les lames se retournent à l'arrivée de la fuite), Wanted (6 défis), Sceaux "
                            + "(les dix défis : un sceau de cire rouge sur chaque défi réussi).",
                    "Le rendu réel n'a pas été vu pendant le développement.")),
            new Version.Note("0.192", "Vingt-huit skins pour le chrome et les particules : icônes, signes, bulles, fond, fusion, page du tableau", List.of(
                    "Sept visuels de plus dans l'onglet Skins. Les icônes d'onglet, l'atome compris : Trait de craie (50 atomes créés), "
                            + "Pixel art (une reconstruction en moins d'une minute), Constellation (la galaxie formée), Emoji (la bulle du "
                            + "bonsaï de droite), et Figées, libre, qui les arrête comme le réglage. Les signes d'état des cartes : Runes "
                            + "(succès Cent fusions), Formes pleines (succès Ensemble complet), Lettres (succès Dix explosions). Les signes "
                            + "des états de la matière : Lettres (20 sortes rassemblées), Gouttes et grains (50), Alchimie (toutes). Les "
                            + "info-bulles des compteurs : Parchemin (une heure de jeu), Post-it (cinq), Terminal (dix).",
                    "Le fond de poussières des particules : Neige, Pluie, Pollen, Lucioles, Bulles, Poussière d'or, Cendres, un palier de "
                            + "particules créées chacun (1e6, 1e9, 1e12, 1e15, 1e18, 1e21, 1e24), et Silence, libre, qui le laisse vide "
                            + "comme le réglage. La fusion des générateurs : Tourbillon (100 fusions), Aspiration (1 000), Éclat (le skin "
                            + "Éclair). La page du tableau périodique hors cases : Médailles sur les tuiles des ensembles (un ensemble "
                            + "complet), Creuset sur la carte de synthèse (100 synthèses), Spectre sur la légende des familles (toutes les "
                            + "familles possédées).",
                    "Le bonus des skins s'arrête à cent skins comptés (×3 au plus) ; les skins libres ne comptent pas.",
                    "Le rendu réel n'a pas été vu pendant le développement.")),
            new Version.Note("0.191", "Quarante skins de plus pour les huit visuels d'origine", List.of(
                    "La fusion des particules : Braise (100 fusions), Encre (1 000), Essaim (100 générateurs), Horloge (10 000 "
                            + "fusions), Verre soufflé (le skin Prisme), Pixel (une reconstruction en moins de 30 s). L'atome : "
                            + "modèle de Bohr au tableau noir (50 atomes), nuage de probabilité (1 000), système à anneaux (les 118 "
                            + "éléments), horlogerie (1 000 synthèses), vitrail (100 éléments). Le tableau : bois gravé (50 "
                            + "éléments), cristal (10 000 synthèses), rouille (le défi Rouille), néon (le skin Néon du collisionneur), "
                            + "carte au trésor (dix sondes), minuscule (le premier élément).",
                    "La boule de matière noire : goutte de mercure (25 explosions), trou noir (10), œuf (le skin Contractée), nœud "
                            + "(200), lanterne (500). Son animation : pluie (15 explosions), neige (50), feu d'artifice (100), "
                            + "sablier (1 000). Le Big Bang : encre de Chine (5 Big Bangs), vitrail (3 systèmes complets), abysses "
                            + "(la planète océan), papier plié (10 Big Bangs), carte ancienne (25 comètes), néon de ville (20 Big "
                            + "Bangs). Le faisceau : lucioles (100 collisions), encre (1 000), laser (10 000), comète (50 comètes). "
                            + "L'anneau : bois et cuivre (10 générateurs du collisionneur), circuit imprimé (100), stade (1 000), "
                            + "ruban de Möbius (le skin Glace).",
                    "Le rendu réel n'a pas été vu pendant le développement.")),
            new Version.Note("0.190", "Le chrome a ses skins : fonds, onglets, cartes", List.of(
                    "Trois visuels de plus dans l'onglet Skins. Le fond de la fenêtre : Papier (une heure de jeu), Ardoise (50 "
                            + "éléments découverts), Nuit étoilée (la galaxie formée), Terminal (100 explosions), Blueprint (10 "
                            + "assemblages formés). Les onglets : onglets de classeur (le Big Bang débloqué), boutons physiques (dix "
                            + "heures de jeu), pastilles (l'univers 1 formé), rubans (l'univers 2 formé). Les cartes : fiche cartonnée "
                            + "(100 fusions), tuile de verre (le skin Cristal du Big Bang), carte à jouer (les dix défis), étiquette "
                            + "(dix sondes revenues).",
                    "Le rendu réel n'a pas été vu pendant le développement.")),
            new Version.Note("0.189", "Les cadres de l'avatar", List.of(
                    "Six cadres pour l'avatar du profil, un par titre : rond (Alchimiste), hexagone (Lecteur du tableau), écusson "
                            + "(Briseur de défis), cadre doré baroque (Faiseur de Big Bang), timbre-poste dentelé (Chasseur de "
                            + "comètes), polaroid (Créateur d'univers). Visuel « Le cadre de l'avatar » dans l'onglet Skins.",
                    "Le rendu réel n'a pas été vu pendant le développement.")),
            new Version.Note("0.188", "Les skins des molécules", List.of(
                    "Quatre apparences pour les molécules, dans l'espace et sur leurs cartes : Boules pleines (50 sortes créées), "
                            + "Traits (toutes les sortes créées), Perles (20 sortes rassemblées), Lego (1 000 molécules créées). "
                            + "Visuel « Les molécules » dans l'onglet Skins.",
                    "Le rendu réel n'a pas été vu pendant le développement.")),
            new Version.Note("0.187", "Un skin par mini-jeu", List.of(
                    "Chaque dimension a son apparence de rechange, débloquée à son seuil : la ligne en rails et wagons, le plan en "
                            + "feuille de cahier, le volume en aquarium, la durée en vinyle, la boucle en cible de tir à l'arc, la "
                            + "brane en toile d'araignée, le clicker en bulles de savon, la grille en démineur, le miroir en vitrail, "
                            + "le mélange en kaléidoscope. Dix visuels de plus dans l'onglet Skins, chacun avec son Classique ; "
                            + "chaque skin débloqué donne toujours +2 % à toutes les ressources d'en dessous de l'univers.",
                    "Le rendu réel n'a pas été vu pendant le développement.")),
            new Version.Note("0.186", "Les skins des astres", List.of(
                    "Trois apparences pour les trente-cinq astres, dans l'espace, le ciel, les systèmes et les cartes : Planétarium "
                            + "(sphères de laiton et de verre dépoli sur leur tige) à dix astres formés dans un univers, Gravure "
                            + "(hachures noir et blanc) à vingt, Cartographie (globes à méridiens et parallèles, nommés en "
                            + "capitales) aux trente-cinq. Les couches de détail de la 0.183 restent propres au Classique.",
                    "Le rendu réel n'a pas été vu pendant le développement.")),
            new Version.Note("0.185", "Les essences de l'arbre", List.of(
                    "L'arbre de la graine, du bonsaï à l'arbre-monde, a huit essences à choisir dans l'onglet Skins, une débloquée "
                            + "par univers formé à partir du quatrième : bouleau, saule pleureur, érable rouge, cerisier en fleur, "
                            + "baobab, séquoia, pin, palmier. Chacune change l'écorce, les feuilles et la silhouette ; le Classique "
                            + "ne bouge pas.",
                    "Le rendu réel n'a pas été vu pendant le développement.")),
            new Version.Note("0.184", "La liste des skins, triée", List.of(
                    "SKINS.md relu et trié : onze idées retirées (le fruit de l'atome, les serpents du faisceau, l'anneau de Saturne du "
                            + "collisionneur, la case du Big Bang, les astres en sucre, en bestiaire et en mécanique, les skins par échelle, "
                            + "les cartes de butin, l'arbre-monde, le thème d'après le skin), et l'horlogerie de l'atome refaite : chaque "
                            + "particule est un petit engrenage. 167 visuels restent. Rien dans le jeu ne change.")),
            new Version.Note("0.183", "Des astres plus détaillés, un espace lisible de loin", List.of(
                    "Assez grands à l'écran (14 pixels de rayon et plus), les astres gagnent des couches de détail : limbe "
                            + "assombri, reflet sur les mers et les glaces, liseré d'atmosphère, ombres des nuages, bords éclairés "
                            + "des cratères, remous entre les bandes et œil de la tempête des géantes, ombre des anneaux sur la "
                            + "planète et de la planète sur ses anneaux, granulation, protubérances et rayons des étoiles, jets et "
                            + "stries des comètes, facettes ombrées des astéroïdes, disque d'un trou noir plus brillant d'un côté et "
                            + "anneau de photons. Les cartes d'achat des astres sont plus hautes (84 pixels) pour que cela se voie, "
                            + "et le ciel dessine un peu plus grandes les comètes et les naines blanches.",
                    "Dans l'espace vu de loin, le gaz ne cache plus la structure : chaque bloc d'assemblage reste un disque plein "
                            + "et net de sa couleur, chaque amas garde un anneau fin et une lueur de la couleur de son état, le "
                            + "tout d'autant plus marqué qu'on est loin. De près, rien ne change ; la galaxie enroulée non plus.",
                    "Le rendu réel n'a pas été vu pendant le développement : si un détail déborde ou se trouve du mauvais côté de la "
                            + "lumière (qui vient d'en haut à gauche), c'est à signaler.")),
            new Version.Note("0.182", "Le chrono dès le deuxième univers, les automatismes des dimensions palier par palier", List.of(
                    "L'étage du chrono est sur la branche Plan de la graine : il se paie en facettes (1 167, un tiers du seuil du "
                            + "plan) et vient donc avec le deuxième univers, au lieu de la durée et du cinquième. Le tuyau d'arrosage "
                            + "passe au volume (1 600 sphères), la terre fertile à la durée (3 000 000 d'instants) ; les réglages du "
                            + "chrono restent dans les quatre premières dimensions. Les niveaux déjà achetés sont gardés tels quels.",
                    "Le chrono, une fois son étage acheté, se règle aussi depuis une petite carte en bas à gauche de la fenêtre, "
                            + "sur tous les onglets ; elle se cache sur la sous-page Graine, où il est déjà.",
                    "Les automatismes des dimensions ne s'ouvrent plus d'un coup par la bulle du sommet de l'arbre-monde, mais "
                            + "dimension par dimension : ceux de la ligne avec la graine craquelée (palier 3) et deux univers formés, "
                            + "ceux du plan avec la graine brisée et trois univers, puis un palier et un univers de plus par dimension "
                            + "(ceux du miroir et du mélange avec l'arbre-monde, dix et onze univers). La bulle du sommet les fait "
                            + "moitié prix. L'onglet Automatisation dit pour chaque dimension ce qui lui manque.",
                    "Les bots mettent le chrono à fond dès son étage acheté : le temps de jeu mesuré ne change pas, le temps réel si.",
                    "Les anciennes sauvegardes se chargent telles quelles : rien de nouveau n'est enregistré.")),
            new Version.Note("0.181", "Un premier univers plus court, des défis qui se corsent", List.of(
                    "Le premier univers demande moins à ses Big Bangs : 10⁸⁵ ⁰⁰⁰ particules et 42 000 matières noires en réserve pour "
                            + "le tout premier (10¹⁹⁰ ⁰⁰⁰ et 120 000 avant), 10³⁰ ⁰⁰⁰ particules pour les suivants (10⁵⁰ ⁰⁰⁰ avant), "
                            + "10¹¹ atomes au lieu de 10¹² ; et l'élan de sa matière noire est multiplié par 1.6 après son premier "
                            + "Big Bang. Les univers suivants gardent les seuils d'avant. Mesuré sur le Speedrunner (graine 201) : le "
                            + "deuxième acte passe de 5 h 43 à 4 h 09, les sept Big Bangs de 6 h 52 à 5 h 41 ; le premier acte et "
                            + "le troisième ne bougent pas.",
                    "Les défis d'explosion se corsent un peu à chaque univers formé : la masse de leur tableau est multipliée "
                            + "par 1.3 par univers (×2.9 au quatrième, ×13.8 au onzième). Au premier univers, rien ne change ; "
                            + "ensuite ils demandent de nouveau quelques minutes au lieu de moins d'une, sans jamais barrer la "
                            + "route. L'aide de la matière noire le dit.",
                    "Les anciennes sauvegardes se chargent telles quelles : rien de nouveau n'est enregistré.")),
            new Version.Note("0.180", "Les skins à venir : la liste", List.of(
                    "SKINS.md, à la racine du dépôt : tout ce que le jeu dessine ou joue et qui pourrait changer d'apparence, "
                            + "des huit visuels déjà skinnés au chrome de la fenêtre, aux astres, aux arbres, aux dix mini-jeux, "
                            + "aux avatars, aux sons et aux thèmes ; pour chacun, des idées de skins, ce qu'ils changeraient et "
                            + "comment on les obtiendrait. Une liste de travail : rien dans le jeu ne change.")),
            new Version.Note("0.179", "La graine se paie en temps de jeu des dimensions", List.of(
                    "Les prix de la graine se mesurent en temps de jeu : chaque amélioration coûte au premier niveau trois "
                            + "minutes de jeu de sa dimension (un cinquième de son seuil, qui vaut un quart d'heure), puis "
                            + "×1.6 par niveau ; l'étage du chrono et la bulle des racines coûtent cinq minutes. Avant, les prix "
                            + "allaient de 350 à 1 500 000 sans rapport entre eux et grandissaient de ×2.5 à ×4 par niveau : "
                            + "au-delà du troisième niveau, plus rien ne s'achetait avant les automatismes des dimensions.",
                    "Les paliers de la graine demandent de plus en plus d'arrosages (30, 60, 900, 2 700, 5 400, 10 800, "
                            + "16 200, 21 600, 32 400, 43 200 de plus à chaque palier ; 300 en tout avant) : à l'arrosoir seul, le "
                            + "bonsaï vient en un quart d'heure, le jeune arbre en trois heures, l'arbre-monde en douze. Les "
                            + "éclats gagnés dans les dimensions pendant les attentes de l'amas et de la formation paient les "
                            + "améliorations qui raccourcissent ce chemin : c'est la prochaine phase de l'arbre qu'on accélère.",
                    "La graine ne prend pas plus de deux paliers d'avance sur les univers formés : les arrosages font venir la "
                            + "prochaine phase plus tôt, pas plus loin, et ceux versés en trop comptent pour la suivante.",
                    "La terre fertile compte un arrosage de plus par niveau (×2 par niveau avant : ×1 024 au dixième).",
                    "L'amas de galaxies et l'univers demandent 60 % de la matière rassemblée d'avant (60 / 6 / 13.75 et 240 / 24 / 55 "
                            + "millions de gaz, de liquide et de cristal) : sans les collections de rayon, qui triplaient l'espace à la "
                            + "fin du premier univers, la formation attendait la matière plus d'une heure de plus.",
                    "Les sauvegardes d'avant chargent telles quelles : les niveaux et les arrosages sont gardés, et un palier déjà "
                            + "atteint peut redescendre si ses arrosages ne suffisent plus ; les bulles achetées restent.")),
            new Version.Note("0.178", "Catalogue resserré : 73 molécules, 4 rayons, 3 états", List.of(
                    "Catalogue resserré : 73 molécules en 4 rayons (petites molécules, acides et sels, minéraux et "
                            + "matériaux, chimie du carbone), 30 assemblages (un par astre), 3 états de la matière (gaz, "
                            + "liquide, cristal). Les sels ont rejoint les acides, les matériaux les minéraux ; le vivant, "
                            + "les terres rares, le solide et le métal disparaissent.",
                    "Les collections de rayon disparaissent, et les assemblages comptent double : 100 % par 100 molécules. "
                            + "« Chimiste » demande 30 sortes. Trois rayons à ouvrir dans les améliorations d'espace, à 2 000, "
                            + "20 000 et 200 000 ; les sondes ont une destination par rayon.",
                    "Page des molécules allégée : une phrase, la recherche et deux filtres (état, usage), le prochain pas, les "
                            + "rayons, l'impulsion, le meilleur achat et le tri, les cartes. Chaque carte tient en trois lignes au "
                            + "plus : dessin, formule, nom et compte ; bonus et prix ; ce que le prochain pas en demande. L'espace "
                            + "occupé, le rendement et la formule détaillée sont dans l'info-bulle. Finis « Au hasard dans ce "
                            + "rayon », la réservation par recette et le badge « inutile ».",
                    "Création automatique refaite : elle suit la liste de courses, puis la matière et la variété qui manquent "
                            + "par état, puis les amas confiés ; plus rien à régler à part sa réserve d'espace et ses trois "
                            + "plafonds par état. L'onglet Automatisation nomme les amas confiés, et un clic en retire un.",
                    "Les sauvegardes d'avant chargent, ce qui a disparu est écarté : les solides et les métaux rejoignent les "
                            + "cristaux, les molécules et les assemblages retirés ne comptent plus.")),
            new Version.Note("0.177", "Le trou noir du vide fait mûrir les trous noirs plus vite", List.of(
                    "Le trou noir du vide, ouvert, raccourcit la maturation du trou noir et du trou noir supermassif : "
                            + "÷ (1 + 10 % par univers formé à partir du quatrième). ÷1.1 quand quatre univers sont formés, "
                            + "÷1.5 quand huit le sont ; les trous noirs qui mûrissent déjà à son ouverture voient leur reste "
                            + "raccourci d'autant.",
                    "Le gain du collisionneur (l'arbuste, à gauche) ne compte plus le premier univers formé : ×1.6 quand "
                            + "quatre sont formés, puis ×1.8, ×2, ×2.2.",
                    "Le trou noir du vide le dit dans l'onglet Vide, et l'aide de la maturation aussi.",
                    "Les bots gardent l'énergie du vide pour les bulles dans le dernier quart d'heure avant la formation "
                            + "de l'univers : le Régulier achetait l'assise une minute avant et n'avait plus de quoi payer ses bulles.",
                    "Mesuré depuis la formation du quatrième univers : le Speedrunner forme ses univers 5 à 8 à 66:18, "
                            + "72:24, 78:22 et 80:49 (66:28, 73:06, 79:16 et 81:42 en 0.176), soit 53 min de mieux à "
                            + "l'univers 8. Le Régulier forme son huitième univers à 151:49 au lieu de 160:00 : il prend neuf "
                            + "bulles d'un coup dès la formation du sixième.")),
            new Version.Note("0.176", "Le gain du collisionneur passe à l'arbuste, à gauche", List.of(
                    "Les 20 % d'énergie et de hadrons par univers formé de la 0.175 passent de l'arbre mûr à l'arbuste, à "
                            + "gauche (21 400), la bulle qui ouvre le collisionneur dès le début de l'univers : ×1.8 à "
                            + "l'achat, à la formation du quatrième univers, puis ×2, ×2.2, ×2.4.",
                    "Chaque univers doit encore remplir l'énergie du trou noir supermassif : le gain l'accélère dans "
                            + "chacun. L'arbre mûr, à gauche, garde le collisionneur d'un univers à l'autre, comme avant la 0.175.",
                    "La décomposition de l'énergie du collisionneur montre « Arbuste, à gauche : N univers formés ».",
                    "Mesuré depuis la formation du quatrième univers : le Speedrunner forme ses univers 5 à 8 à 66:28, "
                            + "73:06, 79:16 et 81:42, au lieu de 67:05, 74:04, 80:57 et 83:24 (1 h 42 d'avance, 39 min de "
                            + "plus qu'avec l'arbre mûr). Le Régulier forme son huitième univers à 160:00 au lieu de 159:38 : "
                            + "son collisionneur plus rapide change l'ordre de ses achats, et les écarts se compensent.")),
            new Version.Note("0.175", "Le collisionneur traverse les univers et va plus vite à chacun", List.of(
                    "La bulle de l'arbre mûr, à gauche (32 100) donne au collisionneur 20 % d'énergie et de hadrons de "
                            + "plus par univers formé : ×2.2 au septième univers, ×3 au onzième. Chaque univers va plus vite "
                            + "que le précédent.",
                    "Les hadrons assemblés en reçoivent leur part : un à un ou d'un coup, le même nombre vient. Ceux "
                            + "gardés d'un univers à l'autre prennent le gain du moment au prochain assemblage.",
                    "La décomposition de l'énergie du collisionneur montre le gain, sous Univers.",
                    "Mesuré depuis la formation de l'univers 6 : le Speedrunner forme son univers 7 à 79:55 au lieu de "
                            + "80:57 (la galaxie attend 3 h 51 au lieu de 4 h 53), puis garde 1 h 03 d'avance (univers 10 "
                            + "à 86:26 au lieu de 87:29). Ensuite, chaque univers dure 2 h, l'amas et la formation, comme "
                            + "avant. Le Régulier, dont le collisionneur n'attendait plus, ne change pas.")),
            new Version.Note("0.174", "Les nouveautés du deuxième univers s'ouvrent par des succès", List.of(
                    "Dix-sept succès du deuxième univers. Douze ouvrent chacun une nouveauté au moment logique de la remontée : "
                            + "le régime des générateurs au troisième générateur, la synergie des rangs au dixième, l'onglet "
                            + "Skins après cinq fusions, le noyau dense et les épreuves à la première explosion, le petit arbre du "
                            + "cycle au premier Big Bang, la prime de cycle parfait, les éléments gratuits, la famille préférée, "
                            + "les épreuves du collisionneur, la fuite, les planètes dans deux systèmes. Les cinq autres, un par défi de Big Bang, portent ses bonus d'univers. Rien ne "
                            + "tombe avant la formation du premier univers ; une ancienne sauvegarde reçoit à la seconde les succès "
                            + "de ce qu'elle avait déjà ouvert.",
                    "Quinze succès jalonnent la suite jusqu'à la fin de la partie (le vide, les hadrons, dix univers, "
                            + "l'arbre-monde, les sommets des dimensions, la fin des temps) ; six portent un petit bonus de 2 %. "
                            + "Le catalogue passe de 67 à 99 succès, chacun +1 % de particules. La puissance des succès reste "
                            + "pleine à 67 succès, comme avant : le premier univers se forme à la même seconde.",
                    "Les bots (dès le deuxième univers ; le premier ne change pas) : la réserve pour le vide et les hadrons "
                            + "ne gèle plus le collisionneur pour un prix à plus de dix minutes de production, et le vide reste "
                            + "ouvert pendant que la galaxie attend le collisionneur. Le collisionneur s'achète par valeur : "
                            + "énergie et puissance d'abord, puis les générateurs du haut vers le bas. L'expert achète ses bulles "
                            + "dans un nouvel ordre, le moins utile en dernier. Mesuré depuis le troisième univers : l'univers 8 "
                            + "se forme 5 h 17 plus tôt chez le Speedrunner (83:24 au lieu de 88:41) et 20 h 16 plus tôt chez le "
                            + "Régulier (152:18 au lieu de 172:34), qui prend enfin la bulle qui garde le collisionneur.")),
            new Version.Note("0.173", "L'effet des racines tient dans sa bulle", List.of(
                    "L'effet de la bulle des racines tient de nouveau sur deux lignes dans l'arbre : « Le défi final : un palier "
                            + "par dimension, les dix finissent la partie ; les gluons l'abaissent jusqu'à moitié ». Depuis la 0.169, "
                            + "le texte faisait plus de deux cents caractères et débordait de la bulle ; le détail du jalon des "
                            + "racines reste dans les paliers des hadrons.",
                    "Les tests de l'interface passent tous : la page Statistiques du collisionneur était vérifiée avec les "
                            + "mauvais hadrons.")),
            new Version.Note("0.172", "Des tests qui prennent le temps du plus long, pas la somme", List.of(
                    "Rien ne change dans le jeu. Les tests : les trois modules se testent en même temps, les campagnes des "
                            + "vingt-quatre profils de bots jouent un profil par cœur (un test par profil dans le rapport, les mêmes "
                            + "parties), et « tester-change » ne rejoue que les modules où quelque chose a bougé ; « tout tester » "
                            + "rejoue toujours tout.")),
            new Version.Note("0.171", "Un vide qui se gagne d'un univers à l'autre, une maturation ×4", List.of(
                    "Les améliorations du vide repartent de zéro à chaque univers : leur premier niveau coûte maintenant dix fois "
                            + "moins (50 à 500 énergie du vide), à la portée d'un seul univers, et leur dernier trente fois plus (2.1 à "
                            + "18 millions), à la portée de l'équilibre du vide en fin de partie. Chaque niveau coûte de 1.9 à 8.1 fois "
                            + "le précédent : les achats s'étalent sur toute la partie au lieu de tomber dans le même univers.",
                    "Depuis le deuxième univers, chaque défi de Big Bang réussi fait mûrir les astres 1.26 fois plus vite (la racine "
                            + "cubique de 2) au lieu de 1.36 : avec les trois défis et la barre haute, ×4 au lieu de ×5, et le trou noir "
                            + "supermassif met 90 minutes au lieu de 72.")),
            new Version.Note("0.170", "Un moteur plus rapide au bit près, des simulations compressées", List.of(
                    "Le tirage des éléments et le cumul de leurs bonus se calculent sans construire de listes ni rechercher dans "
                            + "des tables : les mêmes nombres exactement, de 10 à 20 % de temps de calcul en moins quand le tableau se "
                            + "remplit vite (synthèse sans délai, rattrapage d'absence). Vérifié sur des parties entières : événements "
                            + "à la seconde et sauvegardes identiques.",
                    "Les bots : les univers d'une partie de référence se rejouent en parallèle depuis des jalons (sauvegarde et "
                            + "mémoire du bot à chaque univers lancé), la chaîne se vérifie octet pour octet et seule la partie "
                            + "changée se rejoue ; l'analyste espace ses estimations selon le temps qu'elles annoncent (deux fois moins "
                            + "de copies jouées au plus, la partie inchangée) ; les tests se répartissent sur plusieurs JVM.")),
            new Version.Note("0.169", "Des bulles qui agissent tout de suite, un vide plafonné, les racines liées", List.of(
                    "Une bulle de l'arbre achetée après la formation de l'univers agit déjà sur celui qu'on lance : jusqu'au bouton "
                            + "« univers suivant », le jeu garde en mémoire ce que l'univers formé laisse (collisionneur, vide, matière "
                            + "noire, hadrons, défis du Big Bang), et chaque bulle achetée entre-temps en garde sa part. Avant, elle "
                            + "n'agissait qu'à l'univers d'après.",
                    "Le rassemblement plus court du vide retire 10 % par niveau (cinq niveaux), la maturation plus courte 5 % "
                            + "(dix niveaux) : −50 % au plus chacun, au lieu de −80 % et −70 %.",
                    "Les prix du vide suivent ceux des bulles : le premier niveau de chaque amélioration coûte de 500 à 5 000 "
                            + "énergie du vide, comme les premières bulles, le dernier de 70 000 à 600 000, comme les dernières. Le "
                            + "gain par paire s'arrête à quinze niveaux.",
                    "Le jalon des racines : avec la bulle des racines, dix paliers de gluons, de 80 000 à 400 000, chacun abaisse "
                            + "les paliers du défi final d'environ 7 %, de moitié avec les dix. Il s'affiche avec les paliers de "
                            + "gluons, et les deux derniers sont longs à atteindre.",
                    "La sous-page « Collisionneur » des statistiques dit ce que le collisionneur, les hadrons et le vide font au "
                            + "reste du jeu : ce qu'ils multiplient, ce que le vide raccourcit, ce qu'il prend au collisionneur et à "
                            + "l'espace, et ce qu'il a payé dans l'arbre.")),
            new Version.Note("0.168", "La chronologie de la partie, la valeur durable des achats", List.of(
                    "Une sous-page « Chronologie » dans les statistiques : la durée de chaque univers formé et de ses cinq phases, en "
                            + "barres à la même échelle ; quand chaque dimension s'est ouverte, combien de temps sa fenêtre est restée "
                            + "ouverte, son premier automatisme, son seuil, son palier final et sa part de la partie ; les paliers de la "
                            + "graine et le défi final. Le journal commence avec cette version.",
                    "Dans la sous-page « Achats », chaque achat dit jusqu'où il tient (la prochaine fusion, explosion, le prochain Big "
                            + "Bang, l'univers, toute la partie) et ce qu'il fait gagner sur toute cette durée, estimée par la dernière "
                            + "partie du même genre.",
                    "Corrigé : deux tests de l'interface attendaient encore le prix du bonsaï d'avant la 0.166, et une bulle des "
                            + "racines ouverte sans la graine à son dernier palier ni bulle voisine achetée.",
                    "Le bot analyste garde sa mémoire et son hasard à côté de chaque sauvegarde : une partie jouée en plusieurs "
                            + "tranches reste la même partie. Il note les achats faits au pas qui lance un univers, le palier final de "
                            + "chaque dimension, la condition qui manque seule à la galaxie, et mesure aussi les cases de l'arbre et les "
                            + "bulles « sans », avec un ordre des bulles imposé pour mesurer la dispersion.")),
            new Version.Note("0.167", "Persistance par paliers, un espace à sa mesure", List.of(
                    "Persistance a cinq niveaux, de 8 à 118 atomes : la fusion garde 10 %, 25 %, 50 %, 75 % puis 100 % des "
                            + "niveaux de vitesse et de couplage. La bulle de l'arbuste, à droite, se rachète cinq fois : un niveau "
                            + "de Persistance offert par niveau.",
                    "Les automatismes achètent un seul niveau par action, générateurs compris. Dès la matière noire, chacun "
                            + "peut acheter en lot : ×2 par niveau payé en matière noire (2, 8, 32… 2 048), jusqu'à 64 par action, pour "
                            + "tout l'univers, et l'historique des achats le note. "
                            + "La bulle du bonsaï, à droite, les fait agir sans délai et acheter tout ce qu'ils peuvent.",
                    "Au-delà de 10 000 par seconde, l'espace gagné ne croît plus que comme la racine cubique de ses bonus : "
                            + "il n'en vient plus des milliards de fois plus que ce qui sert.",
                    "Chaque univers formé ajoute 1 au multiplicateur de la matière noire des explosions et de sa croissance "
                            + "(×2, ×3, ×4…), au lieu de le doubler.",
                    "« Couper tous les automatismes » arrête aussi les automates du collisionneur, et un bouton les coupe ou "
                            + "les relance tous dans l'onglet Automatisation.",
                    "Le bot analyste sait payer les niveaux de Persistance un à un, achète le lot de chaque automatisme et la "
                            + "bulle sans délai, et le Speedrunner tient la touche M (tout acheter) entre deux décisions.")),
            new Version.Note("0.166", "Une galaxie qui se joue, des bulles qui se choisissent", List.of(
                    "Depuis le deuxième univers, les défis de Big Bang font mûrir les astres cinq fois plus vite, et non plus cent "
                            + "vingt-huit : le trou noir supermassif met 72 minutes au lieu de 3, la galaxie redevient une étape, et "
                            + "le vide, les places de maturation et les impulsions comptent de nouveau.",
                    "Les bulles de l'arbre coûtent ensemble environ 1.4 fois l'énergie du vide qu'un univers laisse : il faut "
                            + "choisir lesquelles prendre d'abord. Les deux bulles qui se rachètent coûtent 2.5 fois plus à chaque niveau.",
                    "Le tout premier Big Bang demande une réserve de 120 000 matières noires : le joueur n'attend plus quatre "
                            + "heures les seules particules, mais une heure, sans que le Big Bang vienne plus tard.",
                    "Le compteur de la prochaine étape ne se tait plus avant le Big Bang : une condition qui se perd un instant "
                            + "(les atomes, à chaque explosion) ne le remet plus à zéro.",
                    "Le bot analyste joue aussi le Régulier et un joueur absent, dit ce qui a débloqué chaque palier, compte "
                            + "les décisions par heure, mesure les améliorations des dimensions et de la graine, et repère les réserves "
                            + "qui dorment et les paliers bloqués par une seule condition.")),
            new Version.Note("0.165", "Vos achats et la prochaine étape", List.of(
                    "La ligne d'objectif dit dans combien de temps vient la prochaine étape (première fusion, explosion, Big "
                            + "Bang, galaxie, amas, univers), au rythme des dix dernières minutes. Quand l'avancée ne bouge pas assez "
                            + "pour en tirer un temps, elle n'en donne pas. Elle se cache dans les réglages, comme les autres "
                            + "morceaux de l'objectif.",
                    "Dans les statistiques, la sous-page « Achats » garde vos achats de la session : ce qu'ils ont coûté et la "
                            + "part de la réserve, ce qu'ils multiplient, et le temps qu'ils font gagner vers la prochaine étape ; "
                            + "puis ce qui a fait gagner le plus de temps. Ceux des automatismes n'y sont pas.",
                    "Le jeu et le bot analyste lisent les achats de la même façon : la lecture vit maintenant dans le moteur.")),
            new Version.Note("0.164", "Les détecteurs d'illogismes", List.of(
                    "Le bot analyste relit sa partie avec dix détecteurs : joueur moins présent plus rapide, achat jamais rentable, "
                            + "amélioration jamais achetée, défi trivial, rafale de succès, étape éclair ou interminable, estimation "
                            + "ratée, automatisme en va-et-vient, ressource jamais dépensée, gros multiplicateur sans effet. Ils "
                            + "s'écrivent dans illogismes.txt et en tête du rapport.",
                    "Une partie sans une amélioration : elle garde son nom mais prend un prix impossible ; comparée à la partie "
                            + "normale, elle dit ce que l'amélioration apporte.",
                    "Deux parties se comparent palier par palier (comparaison.html), et le bot se joue sur plusieurs graines pour "
                            + "voir ce qui tient au hasard (graines.html).")),
            new Version.Note("0.163", "Jusqu'à la fin", List.of(
                    "Le plan, le volume et la brane automatiques rapportent pendant une absence autant que fenêtre ouverte. "
                            + "Avant, une absence se rejouait par pas de 10 secondes : le point du plan avançait quarante fois "
                            + "moins vite, la plongée du volume expirait avant de replonger (moitié moins de sphères), et la brane "
                            + "rapportait de −17 % à +46 % selon le pas.",
                    "La tirée automatique de la brane vise le point d'où l'onde en touche le plus. Avant, elle tirait au milieu "
                            + "des points : quand ils étaient trop écartés, l'onde n'en touchait aucun, aucun ne partait, et la "
                            + "brane s'arrêtait pour de bon.",
                    "Le bot analyste joue jusqu'à la fin de la partie : il continue de former des univers après le onzième, "
                            + "prend les bulles du sommet et des racines et les améliorations de la graine, laisse les automatismes "
                            + "jouer les dimensions, et s'arrête aux dix paliers du défi final. Il peut aussi repartir d'une "
                            + "sauvegarde.")),
            new Version.Note("0.162", "Le bot analyste dit pourquoi", List.of(
                    "Chaque achat a sa rentabilité : le temps de production qu'il a coûté (son prix divisé par ce que la ressource "
                            + "rapportait par seconde), contre le temps qu'il fait gagner vers le palier suivant. Le rapport classe "
                            + "tout ce qui a été acheté, du plus rentable au moins rentable.",
                    "Chaque défi lancé dit sa contrainte, sa récompense, s'il est une condition du Big Bang, et combien de temps il "
                            + "prendra (joué sur une copie de la partie) ; à la fin, réussi ou abandonné, en combien de temps.",
                    "Chaque succès dit ce qu'il demandait, son bonus et son poids sur chaque grandeur ; chaque automatisme, ce qu'il "
                            + "fait et pourquoi il est mis en marche ou coupé ; chaque explosion, la matière noire qu'elle rapporte par heure.",
                    "À chaque palier principal, « et si » : la suite jouée par un joueur qui passe une fois par minute, puis toutes "
                            + "les dix minutes, à côté du temps du bot.",
                    "Les parties se rejouent à l'identique : le rassemblement automatique parcourait les molécules dans un ordre "
                            + "qui changeait à chaque lancement du jeu, et deux parties jouées pareil divergeaient.",
                    "Le moteur compte les éléments possédés sans chercher dans une table : c'était sa première dépense. Une partie "
                            + "de bot se calcule environ 17 % plus vite ; le jeu devrait en profiter aussi, surtout quand la synthèse "
                            + "automatique tourne.")),
            new Version.Note("0.161", "Le bot analyste", List.of(
                    "Un nouveau bot pour comprendre le jeu : il joue comme le meilleur profil, à vitesse humaine (une décision "
                            + "toutes les cinq secondes), et note tout à la seconde de jeu près. Un double-clic sur analyste.bat le "
                            + "lance pour onze univers.",
                    "Chaque achat est noté : quoi, combien, payé en quoi, combien, la part de la réserve, son poids sur chaque "
                            + "grandeur du jeu et le temps qu'il fait gagner. Les succès, les défis, les automatismes allumés ou "
                            + "coupés, les explosions et les Big Bangs aussi.",
                    "Chaque palier est noté avec son chrono : les trois actes de chaque univers et leurs étapes, chaque Big Bang, "
                            + "les paliers de la graine, les dimensions et leurs seuils, le défi final et la fin. Une sauvegarde est "
                            + "gardée à chaque palier.",
                    "Toutes les 30 minutes de jeu, il estime le temps jusqu'au prochain palier en jouant la suite sur une copie de "
                            + "la partie, et dit ce qui freine. S'il n'avance plus, il s'arrête et explique pourquoi.",
                    "Tout s'écrit en texte brut, en tableaux CSV et dans une page HTML : frise, résumé par étape, paliers, courbes, "
                            + "achats regroupés, journal filtrable.",
                    "En option, il compare six façons de jouer à chaque palier (chacune joue la suite sur une copie) et garde la plus "
                            + "rapide, avec le classement et ce que chaque autre aurait coûté.")),
            new Version.Note("0.160", "Le défi final", List.of(
                    "Le défi final, comme le joueur l'avait demandé : la bulle des racines achetée, chaque dimension a un "
                            + "palier à atteindre, en ressource gagnée en tout (cent fois son seuil ; dix millions de fragments "
                            + "pour le mélange).",
                    "Sur la carte des dimensions, chaque point se remplit de vert, du bas vers le haut, selon l'avancée vers "
                            + "son palier, comptée en puissances de dix ; il devient tout vert au palier atteint. Le rond vert du "
                            + "seuil devient un anneau autour du point ; l'avancée s'écrit sous chaque nom, et les paliers "
                            + "atteints au milieu.",
                    "Les dix paliers atteints, la partie est finie : les traits de la carte deviennent verts en vague, dans "
                            + "une aura dorée, puis le point du milieu grandit jusqu'à couvrir la vue, et l'écran de fin s'ouvre "
                            + "(temps de jeu, univers formés, chiffres de toute la partie). « Continuer » : le jeu continue "
                            + "derrière. « Revoir la fin » la rejoue ; une partie déjà finie au chargement ne la rejoue pas seule.",
                    "Le titre « Racines du monde » vient avec la fin. Les notifications ont leur catégorie « Défi final » "
                            + "(un palier atteint, la fin) ; les statistiques, l'aide et le profil de test ont le défi final.")),
            new Version.Note("0.159", "Le mélange refait", List.of(
                    "Le mélange est refait de fond en comble, à la demande du joueur : plus de tirage au hasard entre les "
                            + "mini-jeux. C'est maintenant un seul mini-jeu, sur une grande arène, qui mêle étape par étape les "
                            + "mécaniques des autres dimensions : la ligne seule à l'étape 1, puis le plan, le volume, la durée, "
                            + "la boucle, la brane, le clicker, la grille et le miroir, jusqu'à l'étape 9 où tout est mêlé.",
                    "Au-dessus de l'arène, l'étape (« Étape 4 sur 9 : ligne + plan + volume + durée ») et sa barre de "
                            + "progression ; « Mêler la dimension suivante » s'allume quand l'étape est gagnée. Chaque dimension "
                            + "mêlée ajoute 50 % au gain ; un malus de 10 % pèse au départ de chaque étape.",
                    "On y joue à la souris (clic ou bouton tenu pour mener le point, molette pour la profondeur, bouton droit "
                            + "tenu pour la charge, clics rapides pour la cadence) ou au clavier (Z Q S D et les flèches, A et E, "
                            + "la barre d'espace). Le halo du point dit si le trésor caché est proche, sans jamais le montrer ; "
                            + "l'axe du miroir et un trait entre les deux éclats d'une paire guident les reflets.",
                    "Onze améliorations, payées en fragments, chacune ouverte à l'étape de sa mécanique (grisées avant, avec "
                            + "l'étape qui les ouvre) : elles repartent de zéro à chaque changement d'étape. Les améliorations du "
                            + "mélange de la 0.158 ne sont pas gardées.",
                    "Le mélange automatique mène le point vers l'éclat le plus proche, plonge, tient et relâche la charge en "
                            + "rythme, clique et passe seul les étapes ; avec un automatisme par amélioration, il y en a "
                            + "maintenant soixante-neuf.",
                    "Les statistiques ont l'étape du mélange, ses charges, ses trésors et ses paires ; l'aide le décrit ; le "
                            + "profil de test a de quoi gagner une étape, y passer ou revenir à la première.")),
            new Version.Note("0.158", "Le mélange", List.of(
                    "La dixième dimension s'ouvre au dixième univers : le mélange, à la place du « point », qui n'était qu'un "
                            + "visuel. Il passe au hasard d'un mini-jeu des neuf autres dimensions à un autre, toutes les vingt "
                            + "secondes ; sa fenêtre montre le mini-jeu du moment, où l'on joue comme dans sa propre fenêtre, et "
                            + "« Ouvrir » l'ouvre en grand (« Retour » ramène au mélange).",
                    "Ce que vous gagnez dans le mini-jeu du moment devient des fragments, la ressource du mélange, en part du "
                            + "seuil de sa dimension, avec un énorme malus au départ (1 %). Le compte à rebours, le malus, le "
                            + "multiplicateur et les changements en réserve s'affichent sous les mini-jeux.",
                    "Six améliorations, payées en fragments : meilleur gain, réduction du malus, changements (« Changer » relance "
                            + "un mini-jeu), mélange (jusqu'à trois mini-jeux à la fois, côte à côte), multiplicateur selon leur "
                            + "nombre, et durée. Le seuil du mélange (5 000 fragments) marque la dimension d'un rond vert et "
                            + "donne le titre « Maître du mélange ».",
                    "L'automatisation des dimensions gagne la section du mélange, payée en reflets du miroir : le mélange "
                            + "automatique ne tire plus que des dimensions qui se jouent seules, puis un automatisme par "
                            + "amélioration ; il y en a maintenant soixante-quatre.",
                    "La bulle des racines : la branche « Mélange » des améliorations de la graine, payée en fragments, l'ouvre ; "
                            + "jusque-là, la bulle le dit. Son effet reste le défi final, à venir.",
                    "La grille, comme le joueur l'avait demandé : quand un point se cache ailleurs (trouvé, ou ses essais "
                            + "épuisés), les couleurs des cases essayées contre lui s'effacent ; le dernier essai le dit encore en "
                            + "toutes lettres.",
                    "Le miroir, comme le joueur l'avait demandé : le plateau s'agrandit seul toutes les trois réussites, d'une "
                            + "colonne de chaque côté et d'une ligne, et le gain ×1.5 de plus, trois fois au plus ; la fenêtre dit "
                            + "les réussites qu'il manque, et un éclat salue l'agrandissement. « Plateau plus vaste » permet un "
                            + "agrandissement de plus par niveau.",
                    "Les statistiques de l'univers ont le mélange, sa courbe, ses mélanges et le plateau du miroir ; l'aide les "
                            + "décrit ; le profil de test a la rangée du mélange.")),
            new Version.Note("0.157", "Le miroir", List.of(
                    "La neuvième dimension s'ouvre au neuvième univers : le miroir, à la place de « toutes les lois », qui "
                            + "n'était qu'un visuel. Un plateau coupé en deux par un trait de lumière ; un motif s'allume case "
                            + "après case sur la moitié gauche, puis s'éteint, et il faut le refaire de mémoire, en miroir, sur "
                            + "la moitié droite, dans le même ordre. Seules les cases déjà reproduites restent visibles.",
                    "Réussi, un motif donne des reflets, la ressource du miroir, d'autant plus qu'il est long, et le suivant "
                            + "a une case de plus (jusqu'à vingt) ; manqué, il en donne un peu, et le suivant a une case de "
                            + "moins (jamais moins de trois). La longueur, le record et les erreurs s'affichent au-dessus du plateau.",
                    "Cinq améliorations, payées en reflets : gain, multiplicateur de longueur, temps de mémorisation, seconde "
                            + "chance (jusqu'à trois erreurs permises) et plateau plus grand (le gain ×1.5 par niveau). Le seuil "
                            + "du miroir (5 000 reflets) marque la dimension d'un rond vert et donne le titre « Gardien du miroir ».",
                    "L'automatisation des dimensions gagne la section du miroir, payée en repères de la grille : le miroir "
                            + "automatique refait le motif, une case toutes les 0.4 s, sans faute jusqu'à six cases, puis un "
                            + "automatisme par amélioration ; il y en a maintenant cinquante-sept.",
                    "La grille est refaite, comme le joueur l'avait demandé : énorme dès le départ, mille cases par côté (un "
                            + "milliard de cases), et un seul essai par point caché avant qu'il se cache ailleurs. On vise une "
                            + "case d'un clic sur la carte de la couche (le zoom la précise) ou par ses coordonnées x, y, z, et on "
                            + "l'essaie. La « Réduction de la grille » la rend plus petite, ×0.7 par niveau jusqu'à deux cases "
                            + "par côté, pour toucher plus souvent ; petite, elle se montre case par case. La nouvelle "
                            + "amélioration « Essais » en ajoute un par point, jusqu'à quatre ; les essais automatiques trouvent "
                            + "rarement sur la grande grille, souvent sur une grille réduite.",
                    "Les saisons de la graine : avec le miroir, printemps, été, automne et hiver passent, deux minutes chacune. "
                            + "La branche « Miroir » des améliorations de la graine, payée en reflets, fait remplir le chrono à "
                            + "chaque arrosage, davantage au printemps, moins en hiver ; la saison se lit près du chrono.",
                    "Les statistiques de l'univers ont le miroir, sa courbe, ses motifs réussis et manqués, son record et la "
                            + "saison ; l'aide les décrit ; le profil de test a la rangée du miroir.")),
            new Version.Note("0.156", "La grille", List.of(
                    "La huitième dimension s'ouvre au huitième univers : la grille, à la place de « la toile des débuts », "
                            + "qui n'était qu'un visuel. Un cube de 1 000 de côté, découpé en cases (deux par côté au départ), "
                            + "où des points se cachent. On choisit une couche (les boutons, la molette ou les flèches haut et "
                            + "bas) et l'on sonde une case d'un clic : plus son centre est près d'un point, plus elle donne de "
                            + "repères, la ressource de la grille.",
                    "La case qui contient un point le trouve : un éclat doré, « Trouvé ! ×5 », et le point se cache ailleurs. "
                            + "Après chaque sondage, une demi-seconde d'attente. À côté de la couche, le cube vu de biais montre "
                            + "la couche choisie et les cases déjà sondées.",
                    "Cinq améliorations, payées en repères : division de la grille (jusqu'à 24 cases par côté), gain, "
                            + "affichage du point, nombre de points (jusqu'à sept) et multiplicateur de proximité. L'affichage "
                            + "montre d'abord la distance de chaque sondage (les cases vont du froid au chaud), puis la couche, "
                            + "et enfin la colonne du point le plus proche du dernier sondage ; avant lui, seul le gain se voit.",
                    "Le seuil de la grille (5 000 repères) marque la dimension d'un rond vert et donne le titre « Arpenteur de "
                            + "la grille ». L'automatisation des dimensions gagne la section de la grille, payée en étincelles "
                            + "du clicker : le sondage automatique sonde une case près d'un point toutes les deux secondes, puis "
                            + "un automatisme par amélioration ; il y en a maintenant cinquante.",
                    "La graine profonde : la branche « Grille » des améliorations de la graine, payée en repères, multiplie "
                            + "tout l'arrosage, d'un dixième par niveau de la racine carrée de la puissance de dix de l'énergie "
                            + "du vide récupérée ; la ligne de la graine dit son effet.",
                    "Les statistiques de l'univers ont la grille, sa courbe, ses sondages, ses points trouvés et la graine "
                            + "profonde ; l'aide les décrit ; le profil de test a la rangée de la grille.")),
            new Version.Note("0.155", "Le clicker", List.of(
                    "La septième dimension s'ouvre au septième univers : le clicker, à la place de « tous les débuts », qui "
                            + "n'était qu'un visuel. Chaque clic sur sa surface donne des étincelles, et d'autant plus que vous "
                            + "cliquez vite : la cadence, comptée sur les deux dernières secondes jusqu'à vingt clics par seconde, "
                            + "multiplie chaque clic ; elle s'écrit au-dessus de la surface. Chaque clic fait une onde, et son "
                            + "gain monte du point.",
                    "Huit améliorations, payées en étincelles : gain, multiplicateur de cadence, et les zones ×2, qui en "
                            + "débloquent cinq autres (multiplicateur, durée, nombre et taille des zones, zones superposées). "
                            + "Les zones apparaissent une toutes les trois secondes et s'éteignent au bout d'un temps, que leur "
                            + "anneau montre en se vidant ; elles peuvent se superposer, et chaque endroit prend la couleur du "
                            + "nombre de zones qui le couvrent. Sous la souris s'écrit le multiplicateur (×N).",
                    "Le seuil du clicker (5 000 étincelles) marque la dimension d'un rond vert et donne le titre « Virtuose du "
                            + "clic ». L'automatisation des dimensions gagne la section du clicker, payée en ondes de la brane : "
                            + "les clics automatiques cliquent six fois par seconde au meilleur endroit, de vrais clics qui se "
                            + "voient sur la surface, puis un automatisme par amélioration ; il y en a maintenant quarante-quatre.",
                    "La météo de la graine : avec le clicker, le ciel est variable, parfois beau (+30 % sur tout l'arrosage "
                            + "par seconde), parfois orageux (−10 %). Il se voit en haut de la vue de la graine (soleil, nuage, "
                            + "pluie et éclairs), avec son effet et le temps qu'il lui reste. La branche « Clicker » des "
                            + "améliorations de la graine allonge le beau temps, raccourcit les orages et renforce le soleil.",
                    "La brane : une seule onde à la fois, mais elle rebondit plusieurs fois, et la grandeur des ondes va "
                            + "maintenant jusqu'à vingt-cinq niveaux, +30 % chacun. Les rebonds se voient mieux : chaque front "
                            + "prend une couleur et une épaisseur selon ses rebonds, un éclat marque le bord où il rebondit, et "
                            + "le gain dit ses rebonds, plus gros (« +9, 2 rebonds »). Le point touché reste doré et affiche ses "
                            + "passages jusqu'à partir avec l'onde ; les statistiques ont le record de rebonds en un passage.",
                    "Les statistiques de l'univers ont le clicker, sa courbe, ses clics, sa cadence, le record de zones "
                            + "superposées et la météo ; l'aide les décrit ; le profil de test a la rangée du clicker et deux "
                            + "outils de météo.")),
            new Version.Note("0.154", "La brane", List.of(
                    "La sixième dimension s'ouvre au sixième univers : la brane, à la place de « tous les possibles », qui "
                            + "n'était qu'un visuel. Une membrane carrée : on l'appuie en un point, on glisse pour la tendre, "
                            + "et un cercle en pointillé montre jusqu'où ira l'onde ; au relâchement, l'onde part du point "
                            + "appuyé, grandit, et rebondit sur les bords.",
                    "Des points apparaissent sur la membrane, un toutes les deux secondes, six au plus. Chaque passage du "
                            + "front sur un point, en direct puis après chaque rebond, donne des ondes, la ressource de la "
                            + "brane : davantage après un rebond, et davantage pour chaque passage de plus de la même onde. Un "
                            + "point touché se dore et reste tant que l'onde court, puis part avec elle. Trois ondes au plus à la fois.",
                    "Cinq améliorations, payées en ondes : gain, élasticité (une tirée plus courte porte déjà à fond), "
                            + "grandeur des ondes, rebond, et points en une onde. Le seuil de la brane (5 000 ondes) marque la "
                            + "dimension d'un rond vert et donne le titre « Surfeur de la brane » ; l'oméga renforce aussi ses ondes.",
                    "La lumière astrale : la branche « Brane » des améliorations de la graine, payée en ondes, multiplie "
                            + "l'arrosage par seconde de l'arrosoir et du tuyau, +20 % par niveau.",
                    "L'automatisation des dimensions gagne la section de la brane, payée en nœuds de la boucle : la tirée "
                            + "automatique tire la membrane à fond au milieu des points toutes les trois secondes, puis un "
                            + "automatisme par amélioration ; il y en a maintenant trente-cinq.",
                    "La boucle : le rythme automatique joue vraiment, clic par clic, fenêtre ouverte : chacun de ses clics "
                            + "éclaire son cercle, son mot monte du point, et la zone touchée part ailleurs, comme pour un clic "
                            + "du joueur. Les zones d'un même cercle ne se superposent plus.",
                    "Les statistiques de l'univers ont la brane, sa courbe, ses ondes tirées, ses points touchés et la lumière "
                            + "astrale ; l'aide les décrit ; le profil de test a la rangée de la brane.")),
            new Version.Note("0.153", "La boucle", List.of(
                    "La cinquième dimension s'ouvre au cinquième univers : la boucle, à la place des embranchements, qui "
                            + "n'étaient qu'un visuel. Sur un cercle, un point tourne, très lentement au début ; une zone y "
                            + "attend, en trois parts : le bord « mauvais », puis « bien », le centre « excellent ».",
                    "Un clic sur le cercle (ou sa touche) quand le point est dans la zone donne des nœuds, d'autant plus "
                            + "que le clic est juste, et la zone se pose ailleurs. Après chaque clic, juste ou non, le cercle "
                            + "attend un court instant, qu'un anneau autour du point montre : cliquer sans arrêt ne rapporte rien. "
                            + "Le mot du clic monte du point : « Excellent », « Bien », « Mauvais » ou « Raté ».",
                    "Six améliorations, payées en nœuds : rapidité du point, nombre de zones (jusqu'à quatre par cercle), "
                            + "gain par zone, précision, taille des zones, et des cercles de plus, jusqu'à sept. Chaque cercle "
                            + "a sa touche, S D F Espace J K L d'origine, à changer dans les réglages, sous-onglet Clavier.",
                    "Un métronome, à brancher dans la fenêtre de la boucle, fait un tic à chaque passage du point du premier "
                            + "cercle sur le centre d'une zone ; il suit le réglage des sons et leur volume.",
                    "Le rond de croissance : avec la boucle, un rond vert pousse de temps en temps près de la graine, avec son "
                            + "compte à rebours. Cueilli d'un clic, il accélère l'arrosage de 25 % pendant 30 s ; la ligne "
                            + "« Croissance +25 % : 30 s » le dit au-dessus de l'étagère. La branche « Boucle » des "
                            + "améliorations de la graine allonge sa durée et renforce son effet.",
                    "L'automatisation des dimensions gagne la section de la boucle, payée en instants de la durée : le rythme "
                            + "automatique clique « bien » à chaque passage d'un point dans une zone, puis un automatisme par "
                            + "amélioration ; il y en a maintenant vingt-neuf. Le seuil de la boucle (5 000 nœuds) marque la "
                            + "dimension d'un rond vert et donne le titre « Danseur de la boucle » ; l'oméga renforce aussi ses nœuds.",
                    "Les statistiques de l'univers ont la boucle, sa courbe et le rond de croissance ; l'aide les décrit ; le "
                            + "profil de test a la rangée de la boucle et un outil qui fait pousser le rond de croissance.")),
            new Version.Note("0.152", "Onze univers à l'essai", List.of(
                    "Profil de test : les bots jouent maintenant depuis zéro jusqu'au onzième univers. Ils arrosent la graine, "
                            + "achètent les bulles, lient des hadrons et prennent les automatismes des dimensions ; le Speedrunner "
                            + "forme son onzième univers vers 82 h 45 de jeu.",
                    "La bulle du géant de gauche, en bas : l'énergie du vide gardée d'un univers à l'autre ne compte plus au "
                            + "cumul qu'une fois ; seule celle gagnée depuis la formation d'avant s'y ajoute (avant, elle était "
                            + "recomptée à chaque formation et rendait toutes les bulles gratuites).",
                    "La bulle du bonsaï, à droite : les automatismes sans délai font avancer le jeu par quarts de seconde, et "
                            + "plus au délai le plus court ; ils achètent toujours tout ce qu'ils peuvent, sans recalculer les "
                            + "bonus des éléments des centaines de fois par seconde.")),
            new Version.Note("0.151", "Ce que font les hadrons", List.of(
                    "Chaque sorte de hadrons a maintenant son effet, qui grandit avec leur nombre : ×(1 + 0.25 × log₁₀(1 + n)) "
                            + "pour n hadrons, ×1.25 à 9, ×1.5 à 99. Le proton renforce les atomes par fusion, le neutron "
                            + "la matière noire par explosion, le pion l'énergie du collisionneur, le kaon l'espace par seconde, "
                            + "le lambda les particules par création, l'oméga les éclats des quatre dimensions, le méson D "
                            + "l'énergie du vide.",
                    "Trois font baisser un prix, divisé d'autant : le J/ψ celui des améliorations du collisionneur, le méson B "
                            + "celui des générateurs, l'upsilon celui des générateurs du collisionneur.",
                    "Sous chaque carte du sous-onglet « Gluons », son bonus : ce qu'il vaut, ce qu'il vaudrait avec un de plus "
                            + "et avec « Lier tout ». En tête de l'onglet, les gluons : chaque quark lié en compte un.",
                    "Nouveau sous-onglet « Paliers » : cinq paliers de gluons, de 1 000 à 400 000, font garder les "
                            + "améliorations, les automatismes et les éléments à l'explosion, l'arbre de matière noire et les "
                            + "défis réussis au Big Bang. Ils agissent dès qu'ils sont atteints, et s'annoncent dans la "
                            + "catégorie « Collisionneur ».",
                    "Nouveau sous-onglet « Liaison » : les quarks en main y flottent, et chaque hadron lié s'y joue, ses "
                            + "quarks rapprochés, reliés par des ressorts de gluons, puis rangés avec les hadrons formés. Il "
                            + "suit le réglage « Animations du collisionneur ».",
                    "Les statistiques des multiplicateurs ont un aspect « Hadrons » ; celles de l'univers comptent les gluons "
                            + "et les paliers atteints. L'aide décrit les bonus, leur calcul et les paliers.")),
            new Version.Note("0.150", "L'automatisation des dimensions", List.of(
                    "La bulle du sommet de l'arbre-monde prend effet : l'onglet Automatisation gagne deux sous-onglets, "
                            + "« Automatismes », tout ce qu'il montrait déjà, et « Dimensions », avec la bulle.",
                    "Vingt-deux automatismes, une carte chacun, rangés par dimension : ils s'achètent l'un après l'autre, "
                            + "ceux de la ligne en graines d'univers, ceux du plan en segments, ceux du volume en facettes, "
                            + "ceux de la durée en sphères. Un clic sur une carte achetée la met en pause ou la relance.",
                    "La visée automatique fait filer le point du plan vers l'éclat le plus proche quand vous ne tenez aucune "
                            + "touche ; la plongée automatique plonge seule dans le volume et y cueille une boule toutes les "
                            + "0.4 s ; l'appui automatique tient le bouton de la durée jusqu'au centre, relâche et recommence. "
                            + "Les autres achètent chacun une amélioration dès qu'elle est payable. Pendant l'appui "
                            + "automatique, les améliorations de la durée s'achètent bouton tenu.",
                    "Ils jouent même fenêtre fermée ; dans la fenêtre de la dimension, une mention « automatique » le dit, "
                            + "la vue suit la plongée et les relâchements, et vos clics ne cassent pas l'appui automatique.",
                    "« Couper tous les automatismes » les arrête aussi ; achetés, ils traversent les univers. Dans la partie "
                            + "secondaire, l'onglet Automatisation revient pour eux seuls.",
                    "L'aide a sa section, et les statistiques de l'univers comptent les automatismes des dimensions achetés.")),
            new Version.Note("0.149", "Les hadrons", List.of(
                    "Nouvel onglet « Hadrons » : avec la bulle de l'arbuste, au milieu, il se propose dans le collisionneur "
                            + "et s'ouvre contre dix minutes de sa production, comme le vide. La bulle du jeune arbre, à gauche, "
                            + "l'ouvre dès le début de l'univers.",
                    "Sous-onglet « Gluons » : les quarks en main se lient en protons (u u d) et en neutrons (u d d), un à un "
                            + "ou tous d'un coup ; chaque hadron a sa carte, avec sa formule, sa recette et le nombre possédé.",
                    "La bulle du cœur de l'arbre-monde ouvre huit particules exotiques, du pion à l'upsilon ; un antiquark "
                            + "coûte un quark de sa saveur. La bulle de l'arbre ancien, à gauche, garde les hadrons d'un univers "
                            + "à l'autre.",
                    "Les hadrons n'ont pas encore d'effet : on les assemble, on les compte.",
                    "Deux titres de plus au profil : « Tisseur de gluons », au premier hadron, et « Chasseur d'exotiques », "
                            + "à la première particule exotique.",
                    "L'offre s'annonce dans la catégorie « Collisionneur » ; une pastille « Hadrons », qui se coupe dans "
                            + "« Notifications et signes », dit quand des quarks attendent d'être liés. L'aide a sa section, "
                            + "et les statistiques de l'univers comptent les hadrons assemblés.")),
            new Version.Note("0.148", "Les bulles de l'arbre prennent effet", List.of(
                    "Chaque bulle de l'arbre a maintenant son effet, qui vaut pour tous les univers suivants ; au survol, elle "
                            + "dit ce qu'elle fait.",
                    "Graine : une place de maturation des astres de plus par dimension ouverte. Bonsaï : à gauche, ×2 sur "
                            + "l'énergie du vide gagnée, cinq fois ; à droite, les automatismes agissent sans délai et achètent "
                            + "tout ce qu'ils peuvent.",
                    "Arbuste : à gauche, le collisionneur ouvert dès le début de l'univers ; à droite, Persistance offerte "
                            + "et une part de fusion en atomes chaque seconde, sans fusionner. Jeune arbre, à droite : plus de "
                            + "plafond de générateurs, mais au-delà de l'ancien, leur prix grimpe très vite.",
                    "Arbre mûr : à gauche, le collisionneur traverse les univers ; à droite, la matière noire, son arbre, "
                            + "les explosions et les défis réussis aussi. Arbre ancien, à droite : les astres n'ont plus "
                            + "besoin de mûrir.",
                    "Arbre géant : tout à gauche, 5 % du gain du vide vont aussi au cumul des bulles, cinq fois ; à gauche "
                            + "en bas, le vide reste ouvert d'un univers à l'autre, avec son énergie et ses améliorations ; "
                            + "tout à droite, de la matière noire chaque seconde selon les atomes, sans exploser ; à droite en "
                            + "bas, tous les paliers du Big Bang ouverts et les défis du Big Bang gardés.",
                    "Les bulles des hadrons, de l'automatisation des dimensions et des racines s'achètent déjà : leur "
                            + "effet viendra avec leur onglet.",
                    "Le guide de formation de l'univers dit ce que les bulles font garder ; les statistiques de l'univers "
                            + "montrent ce qu'elles produisent seules.")),
            new Version.Note("0.147", "Le profil du joueur, et tous les automatismes au même endroit", List.of(
                    "Nouvel onglet « Profil », dès le premier générateur : le nom du joueur, son avatar, le titre qu'il porte, "
                            + "son temps de jeu, ses univers et ses Big Bang. Le journal y déménage, dans son propre sous-onglet.",
                    "Trente-deux titres, de « Étincelle » à « Éternel », en passant par « Apprenti alchimiste », « Forgeron "
                            + "d'étoiles », « Architecte de galaxies » et « Tisseur de multivers » : chacun tombe quand sa condition "
                            + "est vraie, s'annonce dans le journal et reste acquis pour toujours. On porte celui qu'on veut.",
                    "Avatar : une galerie de douze dessins, débloqués par les titres, ou une image à soi, choisie sur l'ordinateur.",
                    "Après le premier Big Bang, quatre albums dans le profil : les molécules, les états de la matière, les "
                            + "assemblages et les astres, avec ce qui a déjà été découvert. Les pages du Big Bang ne changent pas.",
                    "Le profil, les titres et les albums traversent le Big Bang, l'univers suivant et « Recommencer de zéro », "
                            + "comme les records. Ils ne donnent aucun bonus. Leurs annonces ont leur catégorie dans « Notifications "
                            + "et signes », « Titres du profil ».",
                    "Onglet Automatisation : les huit automates du collisionneur (achat, marche et pause), son "
                            + "déclencheur automatique et le tuyau d'arrosage de la graine y sont aussi. « Tout couper » ne les arrête pas : ils gardent "
                            + "chacun leur propre interrupteur, comme avant.")),
            new Version.Note("0.146", "Notifications et signes, réglés par catégorie", List.of(
                    "Réglages : un sous-onglet « Notifications et signes » réunit les notifications, l'objectif du moment et "
                            + "les signes d'état, qui étaient dans « Affichage ».",
                    "Les notifications se coupent par catégorie, sous leur grand interrupteur : dix-neuf catégories, du premier "
                            + "élan aux succès, aux records et aux skins, avec « Toutes affichées » et « Toutes coupées ». Une "
                            + "catégorie coupée ne s'affiche pas et ne sonne pas ; le journal garde tout.",
                    "L'objectif se règle morceau par morceau : l'objectif, le temps qu'il demande, le prochain Big Bang, le "
                            + "troisième acte et le chrono, qui quitte le sous-onglet « Jeu ».",
                    "Les pastilles ● des onglets se cachent par sorte (arbre, défis, collisionneur, paliers, ciel, comète, "
                            + "sondes, systèmes solaires, vide) ; cachée, une pastille ne s'allume ni sur son sous-onglet ni sur "
                            + "l'onglet qui le porte.",
                    "Quand beaucoup de nouvelles arrivent d'un coup, le résumé « … et N autres » ne compte plus que celles qui "
                            + "s'affichent, et le journal les garde une par une.")),
            new Version.Note("0.145", "Les racines de l'arbre-monde, de retour", List.of(
                    "À l'arbre-monde, les deux bulles du bas, celle de la graine et celle des racines, remontent un peu et "
                            + "s'écartent à nouveau, et la bulle des racines retrouve son aura dorée : le grand ménage de la 0.144 "
                            + "les avait effacées.",
                    "Tests : celui de l'arbre de la graine vérifie le vrai centre du feuillage, celui des touffes de feuilles, "
                            + "au lieu d'une position fixe qu'il n'a jamais eue.")),
            new Version.Note("0.144", "Grand ménage : moins de code, mêmes règles", List.of(
                    "Statistiques : nouvelle sous-page « Univers » (univers formés, graine et arrosoir, les quatre dimensions et "
                            + "leurs seuils, le vide et ses paires, les bulles de l'arbre, les règles du deuxième univers), avec "
                            + "ses courbes ; l'énergie du vide entre dans le graphique de toutes les ressources ; les lignes du "
                            + "vide quittent la sous-page Big Bang.",
                    "Multiplicateurs : trois grandeurs de plus se décomposent, l'énergie du collisionneur par seconde, l'énergie "
                            + "du vide par seconde (la part laissée au vide y apparaît en frein) et les segments de la ligne par seconde.",
                    "Le moteur, l'interface et les bots perdent environ 900 lignes sans changer une règle ni un pixel : niveaux "
                            + "d'améliorations en tableaux, boucles sur les paliers et barres de sous-onglets partagées, méthodes "
                            + "et outils que plus rien n'appelait ; les textes de l'aide et des popups lisent les constantes du jeu.",
                    "Profil de test : les bots gardent leur choix de cycle pendant un défi de Big Bang, jouent la « Barre haute » "
                            + "avec ses vrais seuils, retentent les défis abandonnés au bon moment, écrivent leurs CSV avec le point "
                            + "décimal ; onze outils d'essai obsolètes retirés, README et PROFILS_24 remis d'accord avec le code.",
                    "Tests : doublons retirés, cinq tests qui ne pouvaient pas échouer rendus effectifs, suite du moteur plus rapide, "
                            + "et une centaine de vérifications ajoutées (prix des améliorations des dimensions, bulles, planètes "
                            + "en double, épreuves, collisionneur, sondes, fenêtres de la ligne et du plan).")),
            new Version.Note("0.143", "Le réseau des bulles, jusqu'à l'arbre-monde", List.of(
                    "Les bulles de l'arbre continuent jusqu'au dernier palier de la graine : deux de plus au jeune arbre, deux "
                            + "à l'arbre mûr, deux à l'arbre ancien, quatre à l'arbre géant, et trois à l'arbre-monde, dont une "
                            + "dans les racines, sous la graine. Dix-neuf en tout, reliées comme un réseau.",
                    "Une bulle s'ouvre quand une bulle reliée à elle est achetée, d'un côté comme de l'autre du lien : on "
                            + "commence toujours par la base, puis on avance de proche en proche.",
                    "Les bulles portent le nom du palier où elles naissent (« Bulle du bonsaï, à gauche »…) et coûtent "
                            + "environ une fois et demie plus à chaque palier, de 1 000 à 30 000 d'énergie du vide.",
                    "Le jeune arbre est un peu plus grand : il paraissait plus petit que l'arbuste d'avant.")),
            new Version.Note("0.142", "Les bulles de l'arbre", List.of(
                    "Au palier de la graine qui ouvre l'univers 4, à la fin de la dimension 3, une bulle naît à la base de "
                            + "l'arbre, sur la graine. Au palier suivant, deux bulles sur le feuillage ; au palier d'après, trois "
                            + "à la cime. Elles sont reliées : chacune s'ouvre quand une bulle reliée en dessous est achetée, et "
                            + "il faut celle de la base pour les autres.",
                    "Un clic sur une bulle l'achète avec l'énergie du vide accumulée, celle qu'il restait à la formation de "
                            + "chaque univers, affichée sur la page de la graine avec ce qu'il en reste à dépenser. Au survol, "
                            + "une bulle dit son nom et son prix. Achetée, elle s'allume ; elle n'a pas encore d'effet.",
                    "Le sous-onglet « Vide » de l'onglet Univers est retiré : le vide se joue dans son onglet, et son cumul "
                            + "est sur la page de la graine.")),
            new Version.Note("0.141", "Les améliorations de la graine s'achètent au premier clic", List.of(
                    "Les cartes des améliorations de la graine se rafraîchissaient quatre fois par seconde en étant retirées "
                            + "puis remises : un clic tombé entre l'appui et le relâché était perdu, et beaucoup de cartes "
                            + "semblaient ne pas s'acheter. Elles restent maintenant en place ; seul leur contenu change.",
                    "Le curseur du chrono va par quarts et ne saute plus pendant qu'on le glisse.")),
            new Version.Note("0.140", "Des améliorations qui se voient", List.of(
                    "Les cartes des améliorations de la graine gardent leur format, mais se voient mieux : une carte qui "
                            + "s'achète a une bordure dorée et son prix en vert ; s'il manque de quoi payer, le prix passe en "
                            + "orange et dit ce qui manque ; une carte fermée reste lisible ; au maximum, elle passe au vert.",
                    "L'onglet d'une branche où quelque chose s'achète porte une pastille.",
                    "Les boutons d'amélioration des dimensions suivent : achetables, ils prennent la couleur de leur "
                            + "dimension ; sinon ils restent lisibles au lieu de s'éteindre.",
                    "Profil de test : les outils des dimensions sont refaits, une rangée par dimension (l'ouvrir, +10 % du "
                            + "seuil, le seuil, ses améliorations au maximum ou à zéro), plus la graine et son chrono. À côté, "
                            + "« Bots dimension » fait jouer le bot d'une dimension, en direct ou en avance rapide.")),
            new Version.Note("0.139", "Les ressources des dimensions font grandir la graine", List.of(
                    "La graine ne dépend plus des seuils des dimensions : seuls les arrosages la font passer d'un palier à "
                            + "l'autre, et chaque palier ouvre toujours un univers. Le rond vert marque toujours le seuil d'une dimension.",
                    "Les éclats des dimensions ont un nom : les segments de la ligne, les facettes du plan, les sphères du "
                            + "volume, les instants de la durée.",
                    "Nouvelles améliorations de la graine, une branche par dimension : le débit de l'arrosoir (segments, +1 "
                            + "arrosage par seconde), le tuyau d'arrosage qui arrose seul 10 % de l'arrosoir et plus (facettes), la "
                            + "terre fertile qui fait compter chaque arrosage double (sphères), l'étage du chrono (instants).",
                    "Le chrono, sur l'étage du haut de l'étagère, se remplit de temps tout seul, même en absence. Réglé au-dessus "
                            + "de ×1, il fait passer tout le jeu plus vite, et plus il va vite, plus il perd de temps. Ses "
                            + "améliorations : remplissage (segments), réserve (facettes), accélération (sphères), pertes (instants).",
                    "Les branches des dimensions 5 à 10 sont déjà là, verrouillées, en attendant leurs dimensions.")),
            new Version.Note("0.138", "Le prochain astre n'est plus une matière exotique trop tôt", List.of(
                    "Avant le huitième Big Bang, la liste de courses et l'objectif ne visent plus une matière exotique : "
                            + "une nébuleuse planétaire dont la matière était déjà réunie passait devant le dernier astre "
                            + "ordinaire, sans pouvoir être formée, et la liste restait vide.",
                    "Profil de test : le Thésauriseur, bloqué à 27 astres pendant 24 h depuis la 0.131, repasse le huitième "
                            + "Big Bang et forme l'univers en 19 h ; le Prestige passe de 14 h 54 à 11 h 46 ; le Speedrunner forme l'univers 1 en 26 h 53 au lieu de 27 h 01.")),
            new Version.Note("0.137", "L'arrosoir de la graine", List.of(
                    "La graine ne s'arrose plus d'un bouton : un arrosoir attend sur une étagère de bois, en bas à droite de la "
                            + "sous-page « Graine d'univers ». Un bouton posé sur l'étagère le débloque, une fois.",
                    "On prend l'arrosoir à la souris et on le glisse sur la graine : il se penche, l'eau coule du bec, "
                            + "et la graine reçoit un arrosage par seconde. Le premier la fait germer.",
                    "Relâché, l'arrosoir se redresse et revient tout seul sur l'étagère. Une absence le repose aussi.",
                    "Outil d'essai : « Reprendre l'arrosoir », pour revoir le bouton de l'étagère.")),
            new Version.Note("0.136", "Un quart d'heure par dimension", List.of(
                    "Le rond vert de chaque dimension demande environ un quart d'heure de jeu appliqué, au lieu d'une à trois "
                            + "minutes : 250 000 éclats de la ligne, 3 500 du plan, 8 000 du volume et 15 millions de la "
                            + "durée, au lieu de 100 chacune.",
                    "Les améliorations de la durée montent plus vite de prix (×3.4, ×4, ×4 et ×3.2 par niveau au lieu de "
                            + "×2.4, ×3, ×3 et ×2.2) : elles ne sont plus toutes au plus haut en trois minutes.",
                    "Outils d'essai : « +10 % du seuil » remplace « +100 éclats » pour chaque dimension.",
                    "Profil de test : un bot par dimension mesure le temps jusqu'au rond vert.")),
            new Version.Note("0.135", "Les bots jouent mieux, chacun à sa façon", List.of(
                    "Profil de test : dès le premier univers, chaque profil de bot fait ses propres choix (ordre d'achat, "
                            + "arbre de matière noire, défis de Big Bang, durée des voyages des sondes), et à partir du "
                            + "deuxième univers les bots font les défis de Big Bang même quand le Big Bang est prêt.")),
            new Version.Note("0.134", "La durée : la quatrième dimension se joue", List.of(
                    "Au quatrième univers, la dimension 4, la durée, se joue : un rond, et un bouton au milieu.",
                    "Tant qu'on tient le bouton (clic tenu, ou la barre d'espace), les spirales avancent du bord vers le "
                            + "centre ; au relâchement, elles donnent des éclats de la durée, propres à cette dimension.",
                    "Plus on tient, plus chaque tour rapporte : le premier tour vaut 1, le deuxième 2, le troisième 3… Les "
                            + "spirales atteignent le centre en 6 s au départ ; tenir plus ne rapporte plus rien, sans rien faire perdre.",
                    "Quatre améliorations, bouton relâché : vitesse des spirales (+20 %), nombre de spirales (jusqu'à 10), "
                            + "valeur, et temps au plus (+2 s par niveau, jusqu'à 46 s).",
                    "Le seuil de la durée, cent éclats gagnés, ouvre le palier 5 de la graine, donc l'univers 5.",
                    "L'aide (H), page « Dimensions », explique la durée.")),
            new Version.Note("0.133", "Les bots jouent le troisième univers", List.of(
                    "Rien ne change en jeu. Les bots enchaînent trois univers (--univers 3) ; une nouvelle sauvegarde de "
                            + "référence part de l'univers 3 (le deuxième univers formé par le Speedrunner, graine 201).",
                    "Le vide est joué par tous les bots, chacun à sa façon : ouvert dès qu'il est payable, une part laissée au "
                            + "vide (50 % pour ceux qui la règlent, mesurée la meilleure : 14 h 22 contre 14 h 55 à 25 %), ses "
                            + "achats, et le trou noir nourri une fois les huit Big Bangs faits.",
                    "Les univers 1 et 2 restent exactement ceux d'avant. (Note reconstituée en 0.296 depuis la documentation des "
                            + "bots : elle manquait à l'historique.)")),
            new Version.Note("0.132", "Le vide raccourcit le troisième acte", List.of(
                    "Nouvelle amélioration du vide, « Rassemblement plus court » : −16 % par niveau du délai de deux heures de "
                            + "l'amas de galaxies et de l'univers, cinq niveaux, jusqu'à −80 %.",
                    "« Maturation plus courte » va bien plus loin : −7 % par niveau au lieu de −3 %, jusqu'à −70 % du temps de "
                            + "maturation des astres (le trou noir supermassif mûrit en 1 h 48 au lieu de 6 h).",
                    "Les trois améliorations qui raccourcissent le troisième acte (maturation, rassemblement, allègement du ciel) "
                            + "coûtent davantage au départ mais montent moins vite : elles se gagnent au fil de l'acte.",
                    "Les cinq améliorations qui font grandir le vide (gain, rythme, coût en énergie, espace, fonte) montent bien plus "
                            + "vite de prix (×3.7 à ×4.1 par niveau) : le vide ne se remplit plus en une heure.",
                    "Le journal annonce quand le vide se propose, quand son trou noir s'ouvre et quand une amélioration du "
                            + "troisième acte est au plus haut.",
                    "Les statistiques du Big Bang (Avancée) montrent l'énergie du vide et son équilibre, ses niveaux "
                            + "d'amélioration, ce qu'il raccourcit et la masse de son trou noir.",
                    "Les bots jouent le vide : ils l'ouvrent, lui laissent la moitié du collisionneur et de l'espace libre, "
                            + "achètent d'abord ce qui raccourcit le troisième acte et nourrissent le trou noir.")),
            new Version.Note("0.131", "Les bots ont chacun leur façon de jouer", List.of(
                    "Profil de test : dans le deuxième univers, chaque profil de bot fait ses propres choix. Le "
                            + "thésauriseur met en réserve, le chimiste prend la synthèse, le débutant oublie le régime rapide "
                            + "et prend le premier élément offert, le distrait oublie souvent.",
                    "Les bots saisissent les anomalies du ciel, chacun selon son attention au ciel.")),
            new Version.Note("0.130", "Les bots jouent l'univers 2", List.of(
                    "Profil de test : les bots savent rejouer les trois actes du deuxième univers, avec le régime rapide, "
                            + "le choix de cycle, la famille préférée et les éléments offerts. Les dimensions, la graine, "
                            + "l'onglet Univers et le vide restent hors de leur portée pour l'instant.")),
            new Version.Note("0.129", "Le vide se joue", List.of(
                    "Au premier Big Bang du troisième univers, le vide se propose dans l'onglet Collisionneur : il s'ouvre contre "
                            + "dix minutes de production du collisionneur, comptées à son arrivée, une fois par univers.",
                    "Ouvert, le collisionneur y fait naître des paires de particules : chacune coûte de son énergie, tient de "
                            + "l'espace libre pendant ses 6 secondes de vie, et donne de l'énergie du vide à sa naissance.",
                    "Un réglage, dans l'onglet du vide, dit quelle part du collisionneur et de l'espace libre il peut prendre.",
                    "L'énergie du vide fond de 0.2 % par seconde : elle monte vers un équilibre, affiché à côté de la réserve.",
                    "Sept améliorations, en énergie du vide, dans leur sous-onglet : gain par paire, rythme, coût en énergie, "
                            + "espace par paire, fonte plus lente, allègement du ciel (jusqu'à −30 % des besoins des astres, de l'amas "
                            + "et de l'univers) et maturation plus courte (jusqu'à −30 % du temps de maturation des astres).",
                    "Le premier trou noir formé ouvre le trou noir du vide : les particules qu'on lui sacrifie raccourcissent la "
                            + "vie des paires et ralentissent la fonte.",
                    "À la formation de l'univers, l'énergie du vide qui reste rejoint un cumul, gardé et affiché dans le "
                            + "sous-onglet Graine ; le vide repart de zéro dans l'univers suivant.",
                    "L'aide (H) a une page « Le vide ».")),
            new Version.Note("0.128", "Le volume : on plonge dans le cube", List.of(
                    "La troisième dimension se joue : un clic sur le cube qui tourne fait plonger la vue jusqu'en son centre.",
                    "Dedans, le clic gauche tenu fait regarder autour de soi, et le clic droit cueille la boule visée : chaque "
                            + "boule donne des éclats du volume, propres à cette dimension.",
                    "Une plongée dure 12 secondes au départ, puis le cube rejette le joueur ; les boules qui restaient s'en vont.",
                    "Les boules sont petites au départ : il faut viser, et l'amélioration de taille les grossit jusqu'à quatre fois.",
                    "Caméra libre, au choix dans la fenêtre du volume : le regard suit la souris sans clic, une croix vise au "
                            + "centre et un clic cueille ; le choix est gardé dans les réglages.",
                    "En caméra libre, la souris reste retenue dans la vue du cube ; Échap la rend sans finir la plongée, "
                            + "un clic dans la vue la reprend, et elle n'est jamais retenue quand le jeu n'a pas la main.",
                    "Cinq améliorations s'achètent hors du cube : durée (+3 s), valeur, réapparition, nombre et taille des boules.",
                    "Le seuil du volume, cent éclats gagnés, ouvre le palier 4 de la graine, donc l'univers 4, qui restait fermé.",
                    "L'aide (H) a une page « Dimensions ».")),
            new Version.Note("0.127", "Anomalies du ciel, guide d'univers, points de reprise", List.of(
                    "Anomalies du ciel, facultatives : de temps en temps, un bouton apparaît trois minutes dans l'onglet "
                            + "Big Bang. Le vent de particules double la création de particules pendant deux minutes ; la "
                            + "fenêtre d'exploration avance de dix minutes les sondes en vol. Rien ne se perd à les ignorer, "
                            + "et un réglage les masque tout à fait.",
                    "Avant de former l'univers, et avant de lancer l'univers suivant, un guide dit exactement ce qui repart "
                            + "de zéro, ce qui reste (succès, records, réglages, graines, dimensions, skins…) et ce que vous "
                            + "gagnez. « Plus tard » ne change rien.",
                    "Points de reprise : cinq emplacements nommés dans les réglages, en plus de la sauvegarde automatique. "
                            + "Reprendre un point met d'abord la partie en cours de côté, pour revenir en arrière en cas d'erreur.",
                    "L'export dit la version du jeu qui l'a écrit ; à l'import, un premier clic montre ce que contient la "
                            + "partie collée (temps de jeu, Big Bangs, succès, univers), le second l'importe.",
                    "Le rapport de bug dit aussi la graine du hasard de la session, l'onglet ouvert, l'étape atteinte, les "
                            + "paliers payés ou en attente et le défi en cours, avec le début de la première erreur. Il ne "
                            + "contient plus le chemin du fichier de sauvegarde ni le nom du compte.")),
            new Version.Note("0.126", "Ce que le jeu dit, il le fait", List.of(
                    "Dans les univers suivants, l'amas de galaxies et l'univers attendent de nouveau deux heures de "
                            + "rassemblement chacun : le délai se compte depuis la galaxie de la partie en cours, plus depuis "
                            + "la toute première.",
                    "Les records et la « prochaine étape » valent pour chaque nouvelle partie : leur temps se compte "
                            + "depuis le début de la partie en cours. Le chrono revient donc dans les univers suivants.",
                    "Pendant l'épreuve « Premier générateur seulement », l'automatisme des générateurs n'en achète plus.",
                    "Une fois le premier univers formé, tout le temps d'absence est rejoué, aussi dans les parties suivantes.",
                    "Vos réglages (coupure générale des automatismes, rassemblement et formation automatiques, ordres "
                            + "d'achat, seuil de fusion, verrou de l'appui, part d'espace laissée libre, molécules favorites, "
                            + "automatismes de matière noire allumés, pauses du collisionneur) "
                            + "tiennent d'une remise à zéro à l'autre, quelle qu'elle soit. Seul « Recommencer de zéro » les efface.",
                    "« Rouvrir les bifurcations » rend la matière noire payée pour les liaisons, en réserve, et une fenêtre "
                            + "dit combien.",
                    "Sur une longue absence, la surchauffe, le régime rapide, la prime de cycle parfait et le sursaut des "
                            + "sondes comptent exactement le temps qu'il leur reste.",
                    "Une sauvegarde qui nomme une famille d'éléments inconnue se relit quand même : la synthèse repart sans cible.",
                    "Textes : le décompte du calme du Big Bang suit le bon compteur, et sa récompense dit que les achats des "
                            + "automatismes le rompent ; l'aide décrit les paliers payants, les conditions du Big Bang (six "
                            + "pour les derniers), ce qu'il garde, les places de maturation et la touche réglée pour les "
                            + "sondes ; « une nébuleuse » ; les crans des isotopes et le départ lancé suivent le moteur.",
                    "Profil de test et bots : les bots ne partent plus avec le jeu livré, seulement avec « runTest ». La "
                            + "victoire d'un bot est celle du jeu, l'univers formé. L'outil de comparaison des sauvegardes ne "
                            + "confond plus 1.5e10 et 1.5e100.")),
            new Version.Note("0.125", "Le premier univers réglé sur le meilleur joueur", List.of(
                    "L'équilibrage suit maintenant le bot « Speedrunner » du dépôt, parti d'une partie neuve : environ "
                            + "5 h pour le premier acte, 6 h pour le deuxième et 16 h pour le troisième, 27 h en tout. Un joueur "
                            + "moins appliqué mettra davantage (35 h environ pour le profil « Régulier »).",
                    "Le jeu va 1.5 fois plus vite au départ : 3 créations par seconde au lieu de 2.",
                    "Le premier Big Bang demande 10¹⁹⁰⁰⁰⁰ particules, les suivants 10⁵⁰⁰⁰⁰.",
                    "La vitesse de création s'essouffle à partir de 300 niveaux, mais bien plus fort.",
                    "Les puissances montent : tableau périodique +2, ciel +4.5, collisionneur +0.35. Le tableau compte "
                            + "surtout ses éléments différents, plus seulement ses exemplaires.",
                    "Les astres mûrissent trois fois plus lentement (12 minutes pour un amas de roches, 6 heures pour le "
                            + "trou noir supermassif) ; une impulsion n'en retire plus que 18 secondes.",
                    "L'amas de galaxies et l'univers demandent deux heures de rassemblement chacun.",
                    "Les derniers paliers coûtent 10⁵⁵⁰⁰⁰ (Univers mûr), 10¹⁶⁰⁰⁰⁰ (Systèmes solaires), 10²⁰⁰⁰⁰⁰ "
                            + "(Ateliers du ciel, en particules désormais) et 10²³⁰⁰⁰⁰ particules (Matières exotiques).")));

    private VersionNotes100() {
    }
}
