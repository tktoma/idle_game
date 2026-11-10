package idle.core.universe;

/**
 * Les skins du deuxième univers (version 0.087 pour l'onglet, 0.88 pour la façon de les obtenir) :
 * des apparences de rechange pour les visuels du jeu. Chacun débloqué ajoute
 * {@link #BONUS} à toutes les ressources d'en dessous de l'univers ; le choisir ne change que l'apparence.
 *
 * @param visual ce qu'il habille
 * @param label  son nom
 * @param source comment on l'obtient, en quelques mots
 */
public enum Skin {
    FUSION_FLASH(SkinVisual.PARTICLE_FUSION, "Éclair", "Record de reconstruction : de 1 à 10 générateurs en moins d'une minute"),
    FUSION_PRISM(SkinVisual.PARTICLE_FUSION, "Prisme", "Recherche « Fusion prismatique », payée en particules"),
    FUSION_LONE(SkinVisual.PARTICLE_FUSION, "Solitaire", "Défi volontaire « Premier générateur seulement »"),
    ATOM_DENSE(SkinVisual.ATOM, "Noyau dense", "Recherche sur le noyau dense, palier 1"),
    TABLE_LEAD(SkinVisual.TABLE, "Plomb", "Recherche sur le noyau dense, palier 2"),
    ATOM_STELLAR(SkinVisual.ATOM, "Noyau stellaire", "Recherche sur le noyau dense, palier 3"),
    TABLE_GOLD(SkinVisual.TABLE, "Or", "Recherche sur le noyau dense, palier 4"),
    DARK_OBSIDIAN(SkinVisual.DARK_BALL, "Obsidienne", "Matière noire, palier 1"),
    DARK_VORTEX(SkinVisual.DARK_ANIMATION, "Vortex", "Matière noire, palier 2"),
    DARK_NEBULA(SkinVisual.DARK_BALL, "Nébuleuse", "Matière noire, palier 3"),
    DARK_PULSAR(SkinVisual.DARK_ANIMATION, "Pulsar", "Matière noire, palier 4"),
    DARK_CONTRACTED(SkinVisual.DARK_BALL, "Contractée", "Défi « Contraction douce »"),
    BANG_AURORA(SkinVisual.BIG_BANG, "Aurore", "Big Bang, palier 1"),
    BANG_SUPERNOVA(SkinVisual.BIG_BANG, "Supernova", "Big Bang, palier 2"),
    BANG_CRYSTAL(SkinVisual.BIG_BANG, "Cristal", "Défi « Big Bang à froid »"),
    BEAM_GOLD(SkinVisual.COLLIDER_BEAM, "Faisceau doré", "Défi du détecteur"),
    COLLIDER_NEON(SkinVisual.COLLIDER, "Néon", "Défi « Sans surchauffe »"),
    COLLIDER_PLASMA(SkinVisual.COLLIDER, "Plasma", "Défi « Pleine charge »"),
    COLLIDER_ICE(SkinVisual.COLLIDER, "Glace", "Défi « Un seul générateur »"),
    // Version 0.185 : les essences de l'arbre, une par univers formé à partir du quatrième (SKINS.md).
    TREE_BIRCH(SkinVisual.TREE, "Bouleau", "Univers 4 formé"),
    TREE_WILLOW(SkinVisual.TREE, "Saule pleureur", "Univers 5 formé"),
    TREE_MAPLE(SkinVisual.TREE, "Érable rouge", "Univers 6 formé"),
    TREE_CHERRY(SkinVisual.TREE, "Cerisier en fleur", "Univers 7 formé"),
    TREE_BAOBAB(SkinVisual.TREE, "Baobab", "Univers 8 formé"),
    TREE_SEQUOIA(SkinVisual.TREE, "Séquoia", "Univers 9 formé"),
    TREE_PINE(SkinVisual.TREE, "Pin", "Univers 10 formé"),
    TREE_PALM(SkinVisual.TREE, "Palmier", "Univers 11 formé"),
    // Version 0.186 : les astres, par le nombre d'astres formés dans un univers (SKINS.md).
    BODY_PLANETARIUM(SkinVisual.BODY, "Planétarium", "10 astres formés dans un univers"),
    BODY_ENGRAVING(SkinVisual.BODY, "Gravure", "20 astres formés dans un univers"),
    BODY_CARTOGRAPHY(SkinVisual.BODY, "Cartographie", "Les 35 astres formés dans un univers"),
    // Version 0.187 : un skin par mini-jeu, débloqué par le seuil de sa dimension (SKINS.md).
    LINE_RAILS(SkinVisual.LINE, "Rails et wagons", "Seuil de la ligne atteint"),
    PLANE_NOTEBOOK(SkinVisual.PLANE, "Feuille de cahier", "Seuil du plan atteint"),
    VOLUME_AQUARIUM(SkinVisual.VOLUME, "Aquarium", "Seuil du volume atteint"),
    DURATION_VINYL(SkinVisual.DURATION, "Vinyle", "Seuil de la durée atteint"),
    LOOP_ARCHERY(SkinVisual.LOOP, "Cible de tir à l'arc", "Seuil de la boucle atteint"),
    BRANE_WEB(SkinVisual.BRANE, "Toile d'araignée", "Seuil de la brane atteint"),
    CLICKER_BUBBLES(SkinVisual.CLICKER, "Bulles de savon", "Seuil du clicker atteint"),
    GRID_MINESWEEPER(SkinVisual.GRID, "Démineur", "Seuil de la grille atteint"),
    MIRROR_STAINED(SkinVisual.MIRROR, "Vitrail", "Seuil du miroir atteint"),
    MIX_KALEIDOSCOPE(SkinVisual.MIX, "Kaléidoscope", "Seuil du mélange atteint"),
    // Version 0.188 : les molécules (SKINS.md ; « Traits » demandait cent sortes, le catalogue en a 73 : toutes).
    MOLECULE_SPACE_FILLING(SkinVisual.MOLECULE, "Boules pleines", "50 sortes de molécules créées"),
    MOLECULE_SKELETON(SkinVisual.MOLECULE, "Traits", "Les 73 sortes de molécules créées"),
    MOLECULE_PEARLS(SkinVisual.MOLECULE, "Perles", "20 sortes rassemblées"),
    MOLECULE_BRICKS(SkinVisual.MOLECULE, "Lego", "1 000 molécules créées"),
    // Version 0.189 : les cadres de l'avatar, un par titre (SKINS.md).
    FRAME_ROUND(SkinVisual.AVATAR_FRAME, "Rond", "Titre « Alchimiste »"),
    FRAME_HEXAGON(SkinVisual.AVATAR_FRAME, "Hexagone", "Titre « Lecteur du tableau »"),
    FRAME_SHIELD(SkinVisual.AVATAR_FRAME, "Écusson", "Titre « Briseur de défis »"),
    FRAME_BAROQUE(SkinVisual.AVATAR_FRAME, "Cadre doré baroque", "Titre « Faiseur de Big Bang »"),
    FRAME_STAMP(SkinVisual.AVATAR_FRAME, "Timbre-poste", "Titre « Chasseur de comètes »"),
    FRAME_POLAROID(SkinVisual.AVATAR_FRAME, "Polaroid", "Titre « Créateur d'univers »"),
    // Version 0.190 : le chrome (SKINS.md).
    AMBIENT_PAPER(SkinVisual.AMBIENT, "Papier", "Une heure de jeu"),
    AMBIENT_SLATE(SkinVisual.AMBIENT, "Ardoise", "50 éléments découverts"),
    AMBIENT_NIGHT(SkinVisual.AMBIENT, "Nuit étoilée", "La galaxie formée"),
    AMBIENT_TERMINAL(SkinVisual.AMBIENT, "Terminal", "100 explosions"),
    AMBIENT_BLUEPRINT(SkinVisual.AMBIENT, "Blueprint", "10 assemblages formés"),
    TABS_BINDER(SkinVisual.TABS, "Onglets de classeur", "Le Big Bang débloqué"),
    TABS_BUTTONS(SkinVisual.TABS, "Boutons physiques", "Dix heures de jeu"),
    TABS_DOTS(SkinVisual.TABS, "Pastilles", "L'univers 1 formé"),
    TABS_RIBBONS(SkinVisual.TABS, "Rubans", "L'univers 2 formé"),
    CARD_INDEX(SkinVisual.CARD, "Fiche cartonnée", "100 fusions"),
    CARD_GLASS(SkinVisual.CARD, "Tuile de verre", "Skin « Cristal » du Big Bang débloqué"),
    CARD_PLAYING(SkinVisual.CARD, "Carte à jouer", "Les dix défis réussis"),
    CARD_TAG(SkinVisual.CARD, "Étiquette", "Dix sondes revenues"),
    // Version 0.191 : des skins de plus pour les huit visuels d'origine (SKINS.md, section 1).
    FUSION_EMBER(SkinVisual.PARTICLE_FUSION, "Braise", "100 fusions"),
    FUSION_INK(SkinVisual.PARTICLE_FUSION, "Encre", "1 000 fusions"),
    FUSION_SWARM(SkinVisual.PARTICLE_FUSION, "Essaim", "100 générateurs en même temps"),
    FUSION_CLOCK(SkinVisual.PARTICLE_FUSION, "Horloge", "10 000 fusions"),
    FUSION_GLASS(SkinVisual.PARTICLE_FUSION, "Verre soufflé", "Skin « Prisme » débloqué"),
    FUSION_PIXEL(SkinVisual.PARTICLE_FUSION, "Pixel", "Record de reconstruction en moins de 30 s"),
    ATOM_CHALK(SkinVisual.ATOM, "Modèle de Bohr au tableau noir", "50 atomes créés"),
    ATOM_CLOUD(SkinVisual.ATOM, "Nuage de probabilité", "1 000 atomes créés"),
    ATOM_RINGS(SkinVisual.ATOM, "Système à anneaux", "Les 118 éléments découverts"),
    ATOM_GEARS(SkinVisual.ATOM, "Horlogerie", "1 000 synthèses"),
    ATOM_STAINED(SkinVisual.ATOM, "Vitrail", "100 éléments découverts"),
    TABLE_WOOD(SkinVisual.TABLE, "Bois gravé", "50 éléments découverts"),
    TABLE_CRYSTAL(SkinVisual.TABLE, "Cristal", "10 000 synthèses"),
    TABLE_RUST(SkinVisual.TABLE, "Rouille", "Défi « Rouille » réussi"),
    TABLE_NEON(SkinVisual.TABLE, "Néon", "Skin « Néon » du collisionneur débloqué"),
    TABLE_MAP(SkinVisual.TABLE, "Carte au trésor", "Dix sondes revenues"),
    TABLE_TINY(SkinVisual.TABLE, "Minuscule", "Le premier élément découvert"),
    DARK_MERCURY(SkinVisual.DARK_BALL, "Goutte de mercure", "25 explosions"),
    DARK_HOLE(SkinVisual.DARK_BALL, "Trou noir", "10 explosions"),
    DARK_EGG(SkinVisual.DARK_BALL, "Œuf", "Skin « Contractée » débloqué"),
    DARK_KNOT(SkinVisual.DARK_BALL, "Nœud", "200 explosions"),
    DARK_LANTERN(SkinVisual.DARK_BALL, "Lanterne", "500 explosions"),
    DARK_RAIN(SkinVisual.DARK_ANIMATION, "Pluie", "15 explosions"),
    DARK_SNOW(SkinVisual.DARK_ANIMATION, "Neige", "50 explosions"),
    DARK_HOURGLASS(SkinVisual.DARK_ANIMATION, "Sablier", "1 000 explosions"),
    DARK_FIREWORKS(SkinVisual.DARK_ANIMATION, "Feu d'artifice", "100 explosions"),
    BANG_INK(SkinVisual.BIG_BANG, "Encre de Chine", "5 Big Bangs"),
    BANG_STAINED(SkinVisual.BIG_BANG, "Vitrail", "3 systèmes solaires complets"),
    BANG_ABYSS(SkinVisual.BIG_BANG, "Abysses", "La planète océan formée"),
    BANG_ORIGAMI(SkinVisual.BIG_BANG, "Papier plié", "10 Big Bangs"),
    BANG_MAP(SkinVisual.BIG_BANG, "Carte ancienne", "25 comètes attrapées"),
    BANG_NEON(SkinVisual.BIG_BANG, "Néon de ville", "20 Big Bangs"),
    BEAM_COMET(SkinVisual.COLLIDER_BEAM, "Comète", "50 comètes attrapées"),
    BEAM_INK(SkinVisual.COLLIDER_BEAM, "Encre", "1 000 collisions"),
    BEAM_LASER(SkinVisual.COLLIDER_BEAM, "Laser", "10 000 collisions"),
    BEAM_FIREFLIES(SkinVisual.COLLIDER_BEAM, "Lucioles", "100 collisions"),
    COLLIDER_WOOD(SkinVisual.COLLIDER, "Bois et cuivre", "10 générateurs du collisionneur achetés"),
    COLLIDER_CIRCUIT(SkinVisual.COLLIDER, "Circuit imprimé", "100 générateurs du collisionneur achetés"),
    COLLIDER_STADIUM(SkinVisual.COLLIDER, "Stade", "1 000 générateurs du collisionneur achetés"),
    COLLIDER_MOBIUS(SkinVisual.COLLIDER, "Ruban de Möbius", "Skin « Glace » du collisionneur débloqué"),
    // Version 0.192 : les icônes d'onglet, les signes, les info-bulles, le fond des particules, leur fusion, la page du tableau
    // (SKINS.md, sections 2 et 3). « Figées » et « Silence » sont libres : des réglages déguisés en skins.
    ICONS_CHALK(SkinVisual.TAB_ICONS, "Trait de craie", "50 atomes créés"),
    ICONS_PIXEL(SkinVisual.TAB_ICONS, "Pixel art", "Record de reconstruction en moins d'une minute"),
    ICONS_CONSTELLATION(SkinVisual.TAB_ICONS, "Constellation", "La galaxie formée"),
    ICONS_EMOJI(SkinVisual.TAB_ICONS, "Emoji", "La bulle du bonsaï, à droite, achetée"),
    ICONS_STILL(SkinVisual.TAB_ICONS, "Figées", "Libre : les mêmes icônes, sans animation", true),
    SIGNS_RUNES(SkinVisual.CARD_SIGNS, "Runes", "Succès « Cent fusions »"),
    SIGNS_SHAPES(SkinVisual.CARD_SIGNS, "Formes pleines", "Succès « Ensemble complet »"),
    SIGNS_LETTERS(SkinVisual.CARD_SIGNS, "Lettres", "Succès « Dix explosions »"),
    MATTER_LETTERS(SkinVisual.MATTER_SIGNS, "Lettres", "20 sortes rassemblées"),
    MATTER_DROPS(SkinVisual.MATTER_SIGNS, "Gouttes et grains", "50 sortes rassemblées"),
    MATTER_ALCHEMY(SkinVisual.MATTER_SIGNS, "Alchimie", "Toutes les sortes de molécules rassemblées"),
    TOOLTIP_PARCHMENT(SkinVisual.TOOLTIP, "Parchemin", "Une heure de jeu"),
    TOOLTIP_POSTIT(SkinVisual.TOOLTIP, "Post-it", "Cinq heures de jeu"),
    TOOLTIP_TERMINAL(SkinVisual.TOOLTIP, "Terminal", "Dix heures de jeu"),
    DUST_SNOW(SkinVisual.AMBIENT_DUST, "Neige", "1e6 particules créées"),
    DUST_RAIN(SkinVisual.AMBIENT_DUST, "Pluie", "1e9 particules créées"),
    DUST_POLLEN(SkinVisual.AMBIENT_DUST, "Pollen", "1e12 particules créées"),
    DUST_FIREFLIES(SkinVisual.AMBIENT_DUST, "Lucioles", "1e15 particules créées"),
    DUST_BUBBLES(SkinVisual.AMBIENT_DUST, "Bulles", "1e18 particules créées"),
    DUST_GOLD(SkinVisual.AMBIENT_DUST, "Poussière d'or", "1e21 particules créées"),
    DUST_ASH(SkinVisual.AMBIENT_DUST, "Cendres", "1e24 particules créées"),
    DUST_SILENCE(SkinVisual.AMBIENT_DUST, "Silence", "Libre : aucun fond", true),
    GENERATORS_SWIRL(SkinVisual.GENERATOR_FUSION, "Tourbillon", "100 fusions"),
    GENERATORS_SUCTION(SkinVisual.GENERATOR_FUSION, "Aspiration", "1 000 fusions"),
    GENERATORS_SHATTER(SkinVisual.GENERATOR_FUSION, "Éclat", "Skin « Éclair » débloqué"),
    PAGE_MEDALS(SkinVisual.TABLE_PAGE, "Médailles", "Un ensemble complet"),
    PAGE_CRUCIBLE(SkinVisual.TABLE_PAGE, "Creuset", "100 synthèses"),
    PAGE_SPECTRUM(SkinVisual.TABLE_PAGE, "Spectre", "Toutes les familles possédées"),
    // Version 0.193 : la matière noire hors de la boule (SKINS.md, section 4) : l'échelle et la règle, l'arbre, les défis.
    // « Métro » demandait cinq branches, l'arbre en a quatre ; « Circuit » demandait l'arbre complet, que les bifurcations
    // et les cases sans limite rendent impossible : cent niveaux achetés en tout le remplacent.
    RULER_WOOD(SkinVisual.DARK_RULER, "Règle de bois", "10 explosions"),
    RULER_ARCHITECT(SkinVisual.DARK_RULER, "Règle d'architecte", "20 explosions"),
    RULER_CELESTIAL(SkinVisual.DARK_RULER, "Repères célestes", "Succès « Plus grand que tout » : les 26 paliers de taille"),
    DARK_TREE_ROOTS(SkinVisual.DARK_TREE, "Racines", "30 cases de l'arbre achetées"),
    DARK_TREE_CIRCUIT(SkinVisual.DARK_TREE, "Circuit", "100 niveaux achetés dans l'arbre, en tout"),
    DARK_TREE_CONSTELLATION(SkinVisual.DARK_TREE, "Constellation", "La galaxie formée"),
    DARK_TREE_METRO(SkinVisual.DARK_TREE, "Métro", "Les quatre branches de l'arbre entamées"),
    CHALLENGES_TAROT(SkinVisual.CHALLENGES, "Tarot", "3 défis réussis"),
    CHALLENGES_WANTED(SkinVisual.CHALLENGES, "Wanted", "6 défis réussis"),
    CHALLENGES_SEALS(SkinVisual.CHALLENGES, "Sceaux", "Les dix défis réussis"),
    // Version 0.194 : le Big Bang au-delà des molécules (SKINS.md, section 5). Les trois palettes sont libres : des réglages
    // d'accessibilité. Les blocs demandaient 10, 20, 30 et 40 assemblages, le catalogue en a 30 : 10, 15, 20 et tous. Le gaz se
    // compte en molécules de gaz rassemblées ; la galaxie, l'amas et la toile s'échelonnent sur les univers formés.
    PALETTE_PASTEL(SkinVisual.MOLECULE_PALETTE, "Pastel", "Libre : les couleurs CPK adoucies", true),
    PALETTE_SAFE(SkinVisual.MOLECULE_PALETTE, "Daltonien sûr", "Libre : la palette d'Okabe et Ito", true),
    PALETTE_MONO(SkinVisual.MOLECULE_PALETTE, "Monochrome par rayon", "Libre : des gris, le plus clair pour le plus gros atome", true),
    CLUMPS_MINERALS(SkinVisual.CLUMPS, "Minéraux", "5 sortes rassemblées"),
    CLUMPS_NEON(SkinVisual.CLUMPS, "Néon", "20 sortes rassemblées"),
    CLUMPS_PASTEL(SkinVisual.CLUMPS, "Pastel", "50 sortes rassemblées"),
    BLOCKS_STONE(SkinVisual.BLOCKS, "Pierre taillée", "10 assemblages formés"),
    BLOCKS_INGOTS(SkinVisual.BLOCKS, "Lingots", "15 assemblages formés"),
    BLOCKS_CRYSTALS(SkinVisual.BLOCKS, "Cristaux bruts", "20 assemblages formés"),
    BLOCKS_ICE(SkinVisual.BLOCKS, "Pains de glace", "Les 30 assemblages formés"),
    GAS_HAZE(SkinVisual.GAS, "Brume", "1 000 molécules de gaz rassemblées"),
    GAS_SMOKE(SkinVisual.GAS, "Fumée", "10 000 molécules de gaz rassemblées"),
    GAS_AURORA(SkinVisual.GAS, "Aurore", "100 000 molécules de gaz rassemblées"),
    SKY_PLATE(SkinVisual.SKY, "Planche naturaliste", "15 astres formés dans un univers"),
    SKY_MOBILE(SkinVisual.SKY, "Mobile", "25 astres formés dans un univers"),
    SKY_SHOWCASE(SkinVisual.SKY, "Vitrine", "Les 35 astres formés dans un univers"),
    GALAXY_NAUTILUS(SkinVisual.GALAXY, "Nautile", "La galaxie formée"),
    GALAXY_YARN(SkinVisual.GALAXY, "Pelote de laine", "L'univers 2 formé"),
    GALAXY_FINGERPRINT(SkinVisual.GALAXY, "Empreinte digitale", "L'univers 3 formé"),
    GALAXY_WHIRLPOOL(SkinVisual.GALAXY, "Tourbillon", "L'univers 4 formé"),
    GALAXY_FIREWORK(SkinVisual.GALAXY, "Roue de feu d'artifice", "L'univers 5 formé"),
    CLUSTER_GRAPES(SkinVisual.GALAXY_CLUSTER, "Grappe de raisin", "L'amas de galaxies formé"),
    CLUSTER_LANTERNS(SkinVisual.GALAXY_CLUSTER, "Lampions", "L'univers 2 formé"),
    CLUSTER_BEES(SkinVisual.GALAXY_CLUSTER, "Essaim d'abeilles", "L'univers 3 formé"),
    WEB_NEURONS(SkinVisual.COSMIC_WEB, "Neurones", "L'univers 1 formé"),
    WEB_LACE(SkinVisual.COSMIC_WEB, "Dentelle", "L'univers 2 formé"),
    WEB_DEW(SkinVisual.COSMIC_WEB, "Toile au matin", "L'univers 3 formé"),
    WEB_METRO(SkinVisual.COSMIC_WEB, "Métro", "L'univers 4 formé"),
    SYSTEM_ORRERY(SkinVisual.SYSTEM, "Orrery", "1 système solaire complet"),
    SYSTEM_CRAYON(SkinVisual.SYSTEM, "Dessin d'enfant", "2 systèmes solaires complets"),
    SYSTEM_RADAR(SkinVisual.SYSTEM, "Radar", "3 systèmes solaires complets"),
    PROBE_PAPER(SkinVisual.PROBE, "Avion de papier", "10 voyages de sonde"),
    PROBE_BALLOON(SkinVisual.PROBE, "Montgolfière", "25 voyages de sonde"),
    PROBE_BOTTLE(SkinVisual.PROBE, "Bouteille à la mer", "50 voyages de sonde"),
    PROBE_PIGEON(SkinVisual.PROBE, "Pigeon", "100 voyages de sonde"),
    // Version 0.195 : le reste du collisionneur.
    DETECTOR_EYE(SkinVisual.DETECTOR, "Œil", "100 collisions"),
    DETECTOR_LANTERN(SkinVisual.DETECTOR, "Lanterne", "250 collisions"),
    DETECTOR_BELL(SkinVisual.DETECTOR, "Cloche", "500 collisions"),
    DETECTOR_CAMERA(SkinVisual.DETECTOR, "Appareil photo", "1 000 collisions"),
    LIGHTNING_GOLD(SkinVisual.LIGHTNING, "Fils d'or", "1 000 croisements des faisceaux"),
    LIGHTNING_ICE(SkinVisual.LIGHTNING, "Fissures de glace", "2 500 croisements des faisceaux"),
    LIGHTNING_ROOTS(SkinVisual.LIGHTNING, "Racines", "5 000 croisements des faisceaux"),
    SPARKS_PETALS(SkinVisual.SPARKS, "Pétales", "Défi du détecteur"),
    SPARKS_CONFETTI(SkinVisual.SPARKS, "Confettis", "Épreuve « Sans surchauffe »"),
    SPARKS_EMBERS(SkinVisual.SPARKS, "Braises", "Épreuve « Pleine charge »"),
    QUARK_FLIGHT_BALLOONS(SkinVisual.QUARK_FLIGHT, "Ballons", "10 hadrons liés"),
    QUARK_FLIGHT_BUTTERFLIES(SkinVisual.QUARK_FLIGHT, "Papillons", "25 hadrons liés"),
    QUARK_FLIGHT_SCRABBLE(SkinVisual.QUARK_FLIGHT, "Lettres de Scrabble", "50 hadrons liés"),
    // Version 0.195 : les hadrons.
    QUARKS_MARBLES(SkinVisual.QUARKS, "Billes de verre", "10 hadrons liés"),
    QUARKS_FRUITS(SkinVisual.QUARKS, "Fruits", "50 hadrons liés"),
    QUARKS_NOTES(SkinVisual.QUARKS, "Notes de musique", "100 hadrons liés"),
    QUARKS_PUZZLE(SkinVisual.QUARKS, "Puzzle", "Chaque sorte de hadron liée au moins une fois"),
    GLUONS_SPRINGS(SkinVisual.GLUONS, "Ressorts", "100 gluons"),
    GLUONS_RUBBER(SkinVisual.GLUONS, "Élastiques", "250 gluons"),
    GLUONS_WOOL(SkinVisual.GLUONS, "Laine", "500 gluons"),
    GLUONS_LIGHTNING(SkinVisual.GLUONS, "Éclairs", "1 000 gluons"),
    HADRON_BACK_BLACKBOARD(SkinVisual.HADRON_BACKGROUND, "Tableau noir", "Palier « Mémoire des liaisons » (1 000 gluons)"),
    HADRON_BACK_LAB(SkinVisual.HADRON_BACKGROUND, "Table de laboratoire", "Palier « Automatismes liés » (10 000 gluons)"),
    HADRON_BACK_STORM(SkinVisual.HADRON_BACKGROUND, "Ciel d'orage", "Palier « Éléments liés » (50 000 gluons)"),
    // Version 0.195 : le vide.
    FOAM_SOAP(SkinVisual.VOID_FOAM, "Bulles de savon", "Le vide ouvert"),
    FOAM_BEER(SkinVisual.VOID_FOAM, "Mousse de bière", "Une amélioration du vide au niveau 1"),
    FOAM_FILM(SkinVisual.VOID_FOAM, "Pellicule", "Une amélioration du vide au niveau 2"),
    FOAM_TV(SkinVisual.VOID_FOAM, "Neige de télévision", "Une amélioration du vide au niveau 3"),
    PAIRS_TWINS(SkinVisual.VOID_PAIRS, "Jumeaux", "100 paires nées dans la fenêtre"),
    PAIRS_MAGNETS(SkinVisual.VOID_PAIRS, "Aimants", "1 000 paires nées dans la fenêtre"),
    PAIRS_YINYANG(SkinVisual.VOID_PAIRS, "Yin et yang", "10 000 paires nées dans la fenêtre"),
    VOID_HOLE_MANHOLE(SkinVisual.VOID_HOLE, "Bouche d'égout", "Le trou noir du vide ouvert, 1 univers compté"),
    VOID_HOLE_WELL(SkinVisual.VOID_HOLE, "Puits de pierre", "Le trou noir du vide ouvert, 2 univers comptés"),
    VOID_HOLE_IRIS(SkinVisual.VOID_HOLE, "Iris", "Le trou noir du vide ouvert, 3 univers comptés"),
    VOID_HOLE_KNOT(SkinVisual.VOID_HOLE, "Nœud de bois", "Le trou noir du vide ouvert, 4 univers comptés"),
    VOID_HOLE_TEA(SkinVisual.VOID_HOLE, "Tourbillon de thé", "Le trou noir du vide ouvert, 5 univers comptés"),
    // Version 0.196 : l'univers : les bulles, leur naissance, leurs liens, le lancement, la graine et son établi.
    BUBBLE_SOAP(SkinVisual.UNIVERSE_BUBBLE, "Bulle de savon", "Univers 1 formé"),
    BUBBLE_BALLOON(SkinVisual.UNIVERSE_BUBBLE, "Ballon noué", "Univers 2 formé"),
    BUBBLE_SNOWGLOBE(SkinVisual.UNIVERSE_BUBBLE, "Boule à neige", "Univers 3 formé"),
    BUBBLE_ORRERY(SkinVisual.UNIVERSE_BUBBLE, "Globe d'orrery", "Univers 4 formé"),
    BUBBLE_FRUIT(SkinVisual.UNIVERSE_BUBBLE, "Planète-fruit", "Univers 5 formé"),
    BUBBLE_EGG(SkinVisual.UNIVERSE_BUBBLE, "Œuf", "Univers 6 formé"),
    BIRTH_HATCHING(SkinVisual.UNIVERSE_BIRTH, "Éclosion", "Univers 5 formé"),
    BIRTH_BUD(SkinVisual.UNIVERSE_BIRTH, "Bourgeon", "Univers 6 formé"),
    BIRTH_FIREWORKS(SkinVisual.UNIVERSE_BIRTH, "Feu d'artifice", "Univers 7 formé"),
    BIRTH_PAGE(SkinVisual.UNIVERSE_BIRTH, "Page tournée", "Univers 8 formé"),
    LINKS_THREAD(SkinVisual.UNIVERSE_LINKS, "Fil", "Univers 7 formé"),
    LINKS_IVY(SkinVisual.UNIVERSE_LINKS, "Lierre", "Univers 8 formé"),
    LINKS_CIRCUIT(SkinVisual.UNIVERSE_LINKS, "Circuit", "Univers 9 formé"),
    LINKS_CONSTELLATION(SkinVisual.UNIVERSE_LINKS, "Constellation", "Univers 10 formé"),
    LAUNCH_ROCKET(SkinVisual.LAUNCH, "Fusée de papier", "Univers 5 formé"),
    LAUNCH_SPROUT(SkinVisual.LAUNCH, "Graine qui germe", "Univers 6 formé"),
    LAUNCH_STONE(SkinVisual.LAUNCH, "Pierre dans l'eau", "Univers 7 formé"),
    LAUNCH_LAMP(SkinVisual.LAUNCH, "Lampe", "Univers 8 formé"),
    LAUNCH_CURTAIN(SkinVisual.LAUNCH, "Rideau de théâtre", "Univers 9 formé"),
    SEED_WALNUT(SkinVisual.SEED, "Noix", "La graine au palier 1"),
    SEED_ACORN(SkinVisual.SEED, "Gland", "La graine au palier 2"),
    SEED_AVOCADO(SkinVisual.SEED, "Noyau d'avocat", "La graine au palier 3"),
    SEED_PIP(SkinVisual.SEED, "Pépin", "La graine au palier 4"),
    SEED_SUNFLOWER(SkinVisual.SEED, "Tournesol", "La graine au palier 5"),
    SEED_EGG(SkinVisual.SEED, "Œuf", "La graine au palier 6"),
    SEED_GEM(SkinVisual.SEED, "Pierre brute", "La graine au palier 7"),
    CAN_TEAPOT(SkinVisual.WATERING_CAN, "Théière", "10 arrosages de la graine en cours"),
    CAN_PITCHER(SkinVisual.WATERING_CAN, "Pichet", "100 arrosages de la graine en cours"),
    CAN_CLOUD(SkinVisual.WATERING_CAN, "Nuage", "1 000 arrosages de la graine en cours"),
    CAN_BEAK(SkinVisual.WATERING_CAN, "Bec d'oiseau", "Le tuyau d'arrosage acheté"),
    CAN_SHELL(SkinVisual.WATERING_CAN, "Coquillage", "La terre fertile achetée"),
    SHELF_STONE(SkinVisual.SHELF, "Pierre", "L'étage du chrono acheté, 3 univers formés"),
    SHELF_WORKSHOP(SkinVisual.SHELF, "Atelier", "L'étage du chrono acheté, 4 univers formés"),
    SHELF_BAMBOO(SkinVisual.SHELF, "Bambou", "L'étage du chrono acheté, 5 univers formés"),
    SHELF_CLOUD(SkinVisual.SHELF, "Nuage", "L'étage du chrono acheté, 6 univers formés"),
    HOSE_VINE(SkinVisual.HOSE, "Liane", "Le tuyau d'arrosage au niveau 2"),
    HOSE_FOUNTAIN(SkinVisual.HOSE, "Fontaine", "Le tuyau d'arrosage au niveau 4"),
    HOSE_CHANNEL(SkinVisual.HOSE, "Rigole", "Le tuyau d'arrosage au niveau 6"),
    HOSE_PIPETTE(SkinVisual.HOSE, "Pipette", "Le tuyau d'arrosage au niveau 10"),
    CHRONO_HOURGLASS(SkinVisual.CHRONO, "Sablier", "Le chrono plein"),
    CHRONO_POCKET_WATCH(SkinVisual.CHRONO, "Montre à gousset", "Le chrono plein, 3 univers formés"),
    CHRONO_SUNDIAL(SkinVisual.CHRONO, "Cadran solaire", "Le chrono plein, 4 univers formés"),
    CHRONO_METRONOME(SkinVisual.CHRONO, "Métronome", "Le chrono plein, 5 univers formés"),
    RING_FAIRY(SkinVisual.GROWTH_RING, "Anneau de fée", "5 ronds de croissance cueillis depuis l'ouverture de la fenêtre"),
    RING_HALO(SkinVisual.GROWTH_RING, "Halo doré", "20 ronds de croissance cueillis depuis l'ouverture de la fenêtre"),
    RING_SUNRISE(SkinVisual.GROWTH_RING, "Soleil levant", "50 ronds de croissance cueillis depuis l'ouverture de la fenêtre"),
    WEATHER_TV(SkinVisual.WEATHER, "Bulletin télé", "1 orage depuis l'ouverture de la fenêtre"),
    WEATHER_UKIYOE(SkinVisual.WEATHER, "Gravure japonaise", "5 orages depuis l'ouverture de la fenêtre"),
    WEATHER_PASTEL(SkinVisual.WEATHER, "Pastel", "10 orages depuis l'ouverture de la fenêtre"),
    SEASONS_CARDS(SkinVisual.SEASONS, "Cartes à jouer", "Un cycle complet des saisons depuis l'ouverture de la fenêtre"),
    SEASONS_WHEEL(SkinVisual.SEASONS, "Roue de couleurs", "2 cycles complets des saisons depuis l'ouverture de la fenêtre"),
    SEASONS_STAINED(SkinVisual.SEASONS, "Vitrail", "3 cycles complets des saisons depuis l'ouverture de la fenêtre"),
    // Version 0.197 : l'arbre au-delà de ses essences, ses bulles, la carte des dimensions, le défi final, les mini-jeux.
    TREE_AUTUMN(SkinVisual.TREE_SEASON, "Automne figé", "Les saisons débloquées"),
    TREE_WINTER(SkinVisual.TREE_SEASON, "Hiver figé", "Les saisons débloquées, 10 univers formés"),
    TREE_SPRING(SkinVisual.TREE_SEASON, "Printemps figé", "Les saisons débloquées, 11 univers formés"),
    TREE_CRYSTAL(SkinVisual.TREE_FANTASY, "Cristal", "Une bulle de l'arbre achetée"),
    TREE_CORAL(SkinVisual.TREE_FANTASY, "Corail", "Des bulles de l'arbre de 2 paliers différents achetées"),
    TREE_CIRCUIT(SkinVisual.TREE_FANTASY, "Circuit lumineux", "Des bulles de l'arbre de 3 paliers différents achetées"),
    TREE_CLOUDS(SkinVisual.TREE_FANTASY, "Nuages", "Des bulles de l'arbre de 4 paliers différents achetées"),
    TREE_INVERTED(SkinVisual.TREE_FANTASY, "Inversé", "Des bulles de l'arbre de 5 paliers différents achetées"),
    TREE_PIXEL(SkinVisual.TREE_FANTASY, "Pixel art", "Des bulles de l'arbre de 6 paliers différents achetées"),
    GROUND_GRASS(SkinVisual.TREE_GROUND, "Herbe", "La graine au palier 2"),
    GROUND_SAND(SkinVisual.TREE_GROUND, "Sable", "La graine au palier 3"),
    GROUND_SNOW(SkinVisual.TREE_GROUND, "Neige", "La graine au palier 4"),
    GROUND_MOSS(SkinVisual.TREE_GROUND, "Mousse", "La graine au palier 5"),
    GROUND_PAVING(SkinVisual.TREE_GROUND, "Dallage", "La graine au palier 6"),
    GROUND_WATER(SkinVisual.TREE_GROUND, "Eau", "La graine au palier 7"),
    BUBBLES_FRUITS(SkinVisual.TREE_BUBBLES, "Fruits", "5 bulles de l'arbre achetées"),
    BUBBLES_LANTERNS(SkinVisual.TREE_BUBBLES, "Lanternes", "10 bulles de l'arbre achetées"),
    BUBBLES_DEW(SkinVisual.TREE_BUBBLES, "Gouttes de rosée", "15 bulles de l'arbre achetées"),
    BUBBLES_GEMS(SkinVisual.TREE_BUBBLES, "Pierres précieuses", "18 bulles de l'arbre achetées"),
    BUBBLES_SEEDS(SkinVisual.TREE_BUBBLES, "Graines", "Toutes les bulles de l'arbre achetées"),
    BUBBLE_LINKS_BRANCHES(SkinVisual.TREE_BUBBLE_LINKS, "Branches", "10 bulles de l'arbre achetées"),
    BUBBLE_LINKS_LIGHT(SkinVisual.TREE_BUBBLE_LINKS, "Fils de lumière", "15 bulles de l'arbre achetées"),
    BUBBLE_LINKS_ROOTS(SkinVisual.TREE_BUBBLE_LINKS, "Racines", "Toutes les bulles de l'arbre achetées"),
    MAP_PLANETS(SkinVisual.DIMENSIONS_MAP, "Planètes", "2 dimensions ouvertes"),
    MAP_PEARLS(SkinVisual.DIMENSIONS_MAP, "Perles", "4 dimensions ouvertes"),
    MAP_MENHIRS(SkinVisual.DIMENSIONS_MAP, "Menhirs", "6 dimensions ouvertes"),
    MAP_TOWER(SkinVisual.DIMENSIONS_MAP, "Fenêtres d'une tour", "8 dimensions ouvertes"),
    MAP_DIAL(SkinVisual.DIMENSIONS_MAP, "Cadran", "10 dimensions ouvertes"),
    FINALE_WATER(SkinVisual.FINALE_FILL, "Eau", "Le défi final fini"),
    FINALE_SAND(SkinVisual.FINALE_FILL, "Sable", "Le défi final fini, 9 univers formés"),
    FINALE_LIGHT(SkinVisual.FINALE_FILL, "Lumière", "Le défi final fini, 10 univers formés"),
    FINALE_IVY(SkinVisual.FINALE_FILL, "Lierre", "Le défi final fini, 11 univers formés"),
    FRAME_PORTHOLE(SkinVisual.DIMENSION_FRAME, "Hublot", "4 dimensions ouvertes"),
    FRAME_GILDED(SkinVisual.DIMENSION_FRAME, "Cadre doré", "6 dimensions ouvertes"),
    FRAME_CRT(SkinVisual.DIMENSION_FRAME, "Écran cathodique", "8 dimensions ouvertes"),
    FRAME_BOOK(SkinVisual.DIMENSION_FRAME, "Livre ouvert", "10 dimensions ouvertes"),
    DIM_PALETTE_PASTEL(SkinVisual.DIMENSION_PALETTE, "Pastel", "Libre : une option d'accessibilité", true),
    DIM_PALETTE_SAFE(SkinVisual.DIMENSION_PALETTE, "Daltonien sûr", "Libre : la palette d'Okabe et Ito", true),
    DIM_PALETTE_MONO(SkinVisual.DIMENSION_PALETTE, "Monochrome", "Libre : une option d'accessibilité", true),
    DIM_PALETTE_RAINBOW(SkinVisual.DIMENSION_PALETTE, "Arc-en-ciel saturé", "Libre : une option d'accessibilité", true),
    DIM_PALETTE_SEPIA(SkinVisual.DIMENSION_PALETTE, "Sépia", "Libre : une option d'accessibilité", true),
    // Version 0.198 : le profil, les succès et les statistiques (SKINS.md, section 8, première partie).
    AVATAR_LINE(SkinVisual.AVATAR_STYLE, "Trait", "25 succès obtenus"),
    AVATAR_PIXEL(SkinVisual.AVATAR_STYLE, "Pixel", "50 succès obtenus"),
    AVATAR_CHALK(SkinVisual.AVATAR_STYLE, "Craie", "75 succès obtenus"),
    AVATAR_STAINED(SkinVisual.AVATAR_STYLE, "Vitrail", "Tous les succès obtenus"),
    ALBUM_ILLUSTRATED(SkinVisual.ALBUM, "Illustré", "50 entrées dans les albums, tous ensemble"),
    ALBUM_STICKERS(SkinVisual.ALBUM, "Vignettes autocollantes", "Un album complet"),
    ALBUM_STAMPS(SkinVisual.ALBUM, "Timbres", "2 albums complets"),
    ALBUM_CARDS(SkinVisual.ALBUM, "Cartes de collection", "3 albums complets"),
    ALBUM_HERBARIUM(SkinVisual.ALBUM, "Herbier", "Les 4 albums complets"),
    TILES_MEDALS(SkinVisual.ACHIEVEMENT_TILE, "Médailles", "25 succès obtenus"),
    TILES_BADGES(SkinVisual.ACHIEVEMENT_TILE, "Badges brodés", "50 succès obtenus"),
    TILES_STARS(SkinVisual.ACHIEVEMENT_TILE, "Étoiles", "75 succès obtenus"),
    TILES_TROPHIES(SkinVisual.ACHIEVEMENT_TILE, "Trophées", "90 succès obtenus"),
    TILES_STAMPS(SkinVisual.ACHIEVEMENT_TILE, "Timbres", "Tous les succès obtenus"),
    LOCKED_SILHOUETTE(SkinVisual.ACHIEVEMENT_LOCKED, "Silhouette grise", "10 succès obtenus"),
    LOCKED_QUESTION(SkinVisual.ACHIEVEMENT_LOCKED, "Point d'interrogation", "20 succès obtenus"),
    LOCKED_PUZZLE(SkinVisual.ACHIEVEMENT_LOCKED, "Pièce manquante", "30 succès obtenus"),
    CHART_GRAPH_PAPER(SkinVisual.CHART, "Papier millimétré", "1 h de jeu"),
    CHART_OSCILLOSCOPE(SkinVisual.CHART, "Oscilloscope", "5 h de jeu"),
    CHART_PENCIL(SkinVisual.CHART, "Crayon", "10 h de jeu"),
    CHART_WATERCOLOR(SkinVisual.CHART, "Aquarelle", "20 h de jeu"),
    CHART_PASTEL(SkinVisual.CHART_PALETTE, "Pastel", "Libre : une option d'accessibilité", true),
    CHART_NEON(SkinVisual.CHART_PALETTE, "Néon", "Libre : une option d'accessibilité", true),
    CHART_GRAYSCALE(SkinVisual.CHART_PALETTE, "Niveaux de gris", "Libre : une option d'accessibilité", true),
    TIMELINE_MUSEUM(SkinVisual.TIMELINE, "Musée", "3 univers formés"),
    TIMELINE_FILM(SkinVisual.TIMELINE, "Pellicule", "5 univers formés"),
    TIMELINE_TRAIN(SkinVisual.TIMELINE, "Train", "7 univers formés"),
    // Version 0.199 : les notifications, les panneaux, la transition entre univers, l'écran de fin, le bouton de démarrage, le curseur (SKINS.md, section 8).
    TOAST_POSTIT(SkinVisual.TOAST, "Post-it", "20 notifications reçues depuis l'ouverture du jeu"),
    TOAST_TELEGRAM(SkinVisual.TOAST, "Télégramme", "50 notifications reçues depuis l'ouverture du jeu"),
    TOAST_COMIC(SkinVisual.TOAST, "Bulle de BD", "100 notifications reçues depuis l'ouverture du jeu"),
    TOAST_RECEIPT(SkinVisual.TOAST, "Ticket de caisse", "200 notifications reçues depuis l'ouverture du jeu"),
    TOAST_SCROLL(SkinVisual.TOAST, "Parchemin déroulé", "500 notifications reçues depuis l'ouverture du jeu"),
    PANEL_BOOK(SkinVisual.PANEL, "Page de livre", "5 h de jeu"),
    PANEL_CINEMA(SkinVisual.PANEL, "Écran de cinéma", "10 h de jeu"),
    PANEL_BOARD(SkinVisual.PANEL, "Tableau d'affichage", "20 h de jeu"),
    TRANSITION_WHITE(SkinVisual.TRANSITION, "Fondu au blanc", "1 univers formé"),
    TRANSITION_PAGE(SkinVisual.TRANSITION, "Page tournée", "2 univers formés"),
    TRANSITION_IRIS(SkinVisual.TRANSITION, "Diaphragme", "3 univers formés"),
    TRANSITION_HOLE(SkinVisual.TRANSITION, "Trou noir", "4 univers formés"),
    ENDING_CREDITS(SkinVisual.ENDING, "Générique qui défile", "La partie finie"),
    ENDING_CERTIFICATE(SkinVisual.ENDING, "Certificat encadré", "La partie finie, 10 univers formés"),
    ENDING_PHOTO(SkinVisual.ENDING, "Photo souvenir", "La partie finie, 11 univers formés"),
    START_SWITCH(SkinVisual.START_BUTTON, "Interrupteur", "La partie finie"),
    START_SEED(SkinVisual.START_BUTTON, "Graine qu'on plante", "La partie finie, 9 univers formés"),
    START_MATCH(SkinVisual.START_BUTTON, "Allumette", "La partie finie, 10 univers formés"),
    START_DETONATOR(SkinVisual.START_BUTTON, "Détonateur", "La partie finie, 11 univers formés"),
    CURSOR_GLASS_HAND(SkinVisual.CURSOR, "Main de verre", "Succès « Premier automatisme »"),
    CURSOR_SHOOTING_STAR(SkinVisual.CURSOR, "Étoile filante", "Succès « Attrapée au vol »"),
    CURSOR_WATERING_CAN(SkinVisual.CURSOR, "Arrosoir", "Succès « Jeune pousse » ; se montre sur l'onglet Univers"),
    CURSOR_MAGNIFIER(SkinVisual.CURSOR, "Loupe", "Succès « À mi-chemin » ; se montre sur l'onglet Big Bang"),
    CURSOR_WAND(SkinVisual.CURSOR, "Baguette", "Succès « Dix explosions »"),
    // Version 0.200 : les sons : la banque de sons, le son du Big Bang, le métronome de la boucle (SKINS.md, section 8).
    SOUND_WOOD(SkinVisual.SOUND_BANK, "Bois", "1 h de jeu"),
    SOUND_GLASS(SkinVisual.SOUND_BANK, "Verre", "2 h de jeu"),
    SOUND_BITS(SkinVisual.SOUND_BANK, "8 bits", "5 h de jeu"),
    SOUND_ORCHESTRA(SkinVisual.SOUND_BANK, "Orchestre", "10 h de jeu"),
    SOUND_NATURE(SkinVisual.SOUND_BANK, "Nature", "20 h de jeu"),
    SOUND_SILENCE(SkinVisual.SOUND_BANK, "Silence partiel", "Libre : seuls les Big Bangs sonnent", true),
    BANG_SOUND_CHIME(SkinVisual.BIG_BANG_SOUND, "Carillon géant", "5 Big Bangs"),
    BANG_SOUND_WIND(SkinVisual.BIG_BANG_SOUND, "Souffle de vent", "10 Big Bangs"),
    BANG_SOUND_ORGAN(SkinVisual.BIG_BANG_SOUND, "Orgue", "20 Big Bangs"),
    METRONOME_WOOD(SkinVisual.METRONOME, "Bois", "Le seuil de la boucle atteint"),
    METRONOME_METAL(SkinVisual.METRONOME, "Métal", "Le seuil de la boucle atteint, 6 univers formés"),
    METRONOME_DROP(SkinVisual.METRONOME, "Goutte d'eau", "Le seuil de la boucle atteint, 7 univers formés"),
    METRONOME_HEART(SkinVisual.METRONOME, "Battement de cœur", "Le seuil de la boucle atteint, 8 univers formés");

    /** Les banques de sons à débloquer (version 0.200) et les heures de jeu qui les ouvrent ; le silence partiel est libre. */
    public static final Skin[] SOUND_BANKS = {SOUND_WOOD, SOUND_GLASS, SOUND_BITS, SOUND_ORCHESTRA, SOUND_NATURE};
    public static final int[] SOUND_BANK_HOURS = {1, 2, 5, 10, 20};
    /** Les sons du Big Bang (version 0.200) et les Big Bangs qui les ouvrent. */
    public static final Skin[] BANG_SOUNDS = {BANG_SOUND_CHIME, BANG_SOUND_WIND, BANG_SOUND_ORGAN};
    public static final int[] BANG_SOUND_BANGS = {5, 10, 20};
    /** Les tics du métronome (version 0.200) : le seuil de la boucle, le premier dès là, les suivants à {@code FIRST_METRONOME_UNIVERSE + i - 1} univers. */
    public static final Skin[] METRONOMES = {METRONOME_WOOD, METRONOME_METAL, METRONOME_DROP, METRONOME_HEART};
    public static final int FIRST_METRONOME_UNIVERSE = 6;

    /** Les skins des notifications (version 0.199) et les notifications reçues depuis l'ouverture du jeu qui les débloquent (compte non sauvegardé). */
    public static final Skin[] TOASTS = {TOAST_POSTIT, TOAST_TELEGRAM, TOAST_COMIC, TOAST_RECEIPT, TOAST_SCROLL};
    public static final int[] TOAST_NOTIFICATIONS = {20, 50, 100, 200, 500};
    /** Les skins des panneaux (version 0.199) et les heures de jeu qui les débloquent. */
    public static final Skin[] PANELS = {PANEL_BOOK, PANEL_CINEMA, PANEL_BOARD};
    public static final int[] PANEL_HOURS = {5, 10, 20};
    /** Les skins de la transition entre univers (version 0.199) : {@code TRANSITIONS[n - 1]} à n univers formés. */
    public static final Skin[] TRANSITIONS = {TRANSITION_WHITE, TRANSITION_PAGE, TRANSITION_IRIS, TRANSITION_HOLE};
    /** Les skins de l'écran de fin (version 0.199) : la partie finie, le premier dès là, les suivants à {@code FIRST_ENDING_UNIVERSE + i - 1} univers. */
    public static final Skin[] ENDINGS = {ENDING_CREDITS, ENDING_CERTIFICATE, ENDING_PHOTO};
    public static final int FIRST_ENDING_UNIVERSE = 10;
    /** Les skins du bouton de démarrage (version 0.199) : la partie finie, le premier dès là, les suivants à {@code FIRST_START_UNIVERSE + i - 1} univers. */
    public static final Skin[] START_BUTTONS = {START_SWITCH, START_SEED, START_MATCH, START_DETONATOR};
    public static final int FIRST_START_UNIVERSE = 9;
    /** Les skins du curseur (version 0.199) et les succès, d'un onglet différent chacun, qui les débloquent. */
    public static final Skin[] CURSORS = {CURSOR_GLASS_HAND, CURSOR_SHOOTING_STAR, CURSOR_WATERING_CAN, CURSOR_MAGNIFIER, CURSOR_WAND};
    public static final String[] CURSOR_ACHIEVEMENTS = {"step_automation", "feat_comet", "late_seed_half", "step_half_table", "step_explosions"};

    /** Le repère « tous les succès » des paliers de la version 0.198 : le jeu le remplace par la taille de son catalogue. */
    public static final int ALL_ACHIEVEMENTS = -1;
    /** Les styles des avatars (version 0.198) et les succès obtenus qui les débloquent ; le dernier demande tous les succès ({@code ALL_ACHIEVEMENTS}). */
    public static final Skin[] AVATAR_STYLES = {AVATAR_LINE, AVATAR_PIXEL, AVATAR_CHALK, AVATAR_STAINED};
    public static final int[] AVATAR_STYLE_ACHIEVEMENTS = {25, 50, 75, ALL_ACHIEVEMENTS};
    /** Les skins des albums (version 0.198) : le premier à {@code ALBUM_ENTRIES} entrées trouvées, puis {@code ALBUMS[n]} à n albums complets. */
    public static final Skin[] ALBUMS = {ALBUM_ILLUSTRATED, ALBUM_STICKERS, ALBUM_STAMPS, ALBUM_CARDS, ALBUM_HERBARIUM};
    public static final int ALBUM_ENTRIES = 50;
    /** Les tuiles des succès (version 0.198) et les succès obtenus qui les débloquent ; le dernier demande tous les succès. */
    public static final Skin[] TILES = {TILES_MEDALS, TILES_BADGES, TILES_STARS, TILES_TROPHIES, TILES_STAMPS};
    public static final int[] TILE_ACHIEVEMENTS = {25, 50, 75, 90, ALL_ACHIEVEMENTS};
    /** Les succès non obtenus (version 0.198) et les succès obtenus qui les débloquent. */
    public static final Skin[] LOCKED_SKINS = {LOCKED_SILHOUETTE, LOCKED_QUESTION, LOCKED_PUZZLE};
    public static final int[] LOCKED_ACHIEVEMENTS = {10, 20, 30};
    /** Les courbes des statistiques (version 0.198) et les heures de jeu qui les débloquent. */
    public static final Skin[] CHARTS = {CHART_GRAPH_PAPER, CHART_OSCILLOSCOPE, CHART_PENCIL, CHART_WATERCOLOR};
    public static final int[] CHART_HOURS = {1, 5, 10, 20};
    /** La frise des univers (version 0.198) et les univers formés qui la débloquent. */
    public static final Skin[] TIMELINES = {TIMELINE_MUSEUM, TIMELINE_FILM, TIMELINE_TRAIN};
    public static final int[] TIMELINE_UNIVERSES = {3, 5, 7};

    /** Les saisons figées de l'arbre (version 0.197) : les saisons débloquées, puis {@code FIRST_TREE_SEASON_UNIVERSE + i} univers formés. */
    public static final Skin[] TREE_SEASONS = {TREE_AUTUMN, TREE_WINTER, TREE_SPRING};
    public static final int FIRST_TREE_SEASON_UNIVERSE = 9;
    /** Les arbres de fantaisie (version 0.197) : {@code TREE_FANTASIES[n - 1]} avec des bulles de n paliers différents achetées. */
    public static final Skin[] TREE_FANTASIES = {TREE_CRYSTAL, TREE_CORAL, TREE_CIRCUIT, TREE_CLOUDS, TREE_INVERTED, TREE_PIXEL};
    /** Les sols de l'arbre (version 0.197) : {@code GROUNDS[n - 2]} à la graine au palier n. */
    public static final Skin[] GROUNDS = {GROUND_GRASS, GROUND_SAND, GROUND_SNOW, GROUND_MOSS, GROUND_PAVING, GROUND_WATER};
    public static final int FIRST_GROUND_STAGE = 2;
    /** Les skins des bulles de l'arbre (version 0.197) et les bulles achetées qui les débloquent ; le dernier demande toutes les bulles (19). */
    public static final Skin[] TREE_BUBBLE_SKINS = {BUBBLES_FRUITS, BUBBLES_LANTERNS, BUBBLES_DEW, BUBBLES_GEMS, BUBBLES_SEEDS};
    public static final int[] TREE_BUBBLE_COUNTS = {5, 10, 15, 18, 19};
    /** Les skins des liens entre les bulles (version 0.197) et les bulles achetées qui les débloquent. */
    public static final Skin[] BUBBLE_LINK_SKINS = {BUBBLE_LINKS_BRANCHES, BUBBLE_LINKS_LIGHT, BUBBLE_LINKS_ROOTS};
    public static final int[] BUBBLE_LINK_COUNTS = {10, 15, 19};
    /** Les skins de la carte des dimensions (version 0.197) et les dimensions ouvertes qui les débloquent. */
    public static final Skin[] MAPS = {MAP_PLANETS, MAP_PEARLS, MAP_MENHIRS, MAP_TOWER, MAP_DIAL};
    public static final int[] MAP_DIMENSIONS = {2, 4, 6, 8, 10};
    /** Les remplissages du défi final (version 0.197) : le défi fini, le premier dès là, les suivants à {@code FIRST_FINALE_UNIVERSE + i - 1} univers. */
    public static final Skin[] FINALES = {FINALE_WATER, FINALE_SAND, FINALE_LIGHT, FINALE_IVY};
    public static final int FIRST_FINALE_UNIVERSE = 9;
    /** Les cadres des mini-jeux (version 0.197) et les dimensions ouvertes qui les débloquent. */
    public static final Skin[] FRAMES = {FRAME_PORTHOLE, FRAME_GILDED, FRAME_CRT, FRAME_BOOK};
    public static final int[] FRAME_DIMENSIONS = {4, 6, 8, 10};

    /** Les skins des bulles des univers (version 0.196) : {@code UNIVERSE_BUBBLES[n - 1]} à l'univers n formé. */
    public static final Skin[] UNIVERSE_BUBBLES = {BUBBLE_SOAP, BUBBLE_BALLOON, BUBBLE_SNOWGLOBE, BUBBLE_ORRERY, BUBBLE_FRUIT, BUBBLE_EGG};
    /** Les skins de la naissance (version 0.196), un par univers formé à partir du cinquième. */
    public static final Skin[] BIRTHS = {BIRTH_HATCHING, BIRTH_BUD, BIRTH_FIREWORKS, BIRTH_PAGE};
    public static final int FIRST_BIRTH_UNIVERSE = 5;
    /** Les skins des liens entre les univers (version 0.196), un par univers formé à partir du septième. */
    public static final Skin[] LINKS = {LINKS_THREAD, LINKS_IVY, LINKS_CIRCUIT, LINKS_CONSTELLATION};
    public static final int FIRST_LINKS_UNIVERSE = 7;
    /** Les skins du lancement (version 0.196), un par univers formé à partir du cinquième. */
    public static final Skin[] LAUNCHES = {LAUNCH_ROCKET, LAUNCH_SPROUT, LAUNCH_STONE, LAUNCH_LAMP, LAUNCH_CURTAIN};
    public static final int FIRST_LAUNCH_UNIVERSE = 5;
    /** Les skins de la graine (version 0.196) : {@code SEEDS[n - 1]} à son palier n. */
    public static final Skin[] SEEDS = {SEED_WALNUT, SEED_ACORN, SEED_AVOCADO, SEED_PIP, SEED_SUNFLOWER, SEED_EGG, SEED_GEM};
    /** Les trois premiers skins de l'arrosoir (version 0.196) et les arrosages de la graine en cours qui les débloquent. */
    public static final Skin[] CANS = {CAN_TEAPOT, CAN_PITCHER, CAN_CLOUD};
    public static final long[] CAN_WATERINGS = {10, 100, 1_000};
    /** Les skins de l'étagère (version 0.196) : l'étage du chrono acheté, et {@code FIRST_SHELF_UNIVERSE + i} univers formés. */
    public static final Skin[] SHELVES = {SHELF_STONE, SHELF_WORKSHOP, SHELF_BAMBOO, SHELF_CLOUD};
    public static final int FIRST_SHELF_UNIVERSE = 3;
    /** Les skins du tuyau (version 0.196) et ses niveaux qui les débloquent. */
    public static final Skin[] HOSES = {HOSE_VINE, HOSE_FOUNTAIN, HOSE_CHANNEL, HOSE_PIPETTE};
    public static final int[] HOSE_LEVELS = {2, 4, 6, 10};
    /** Les skins du chrono (version 0.196) : le chrono plein, le premier dès là, les suivants à {@code FIRST_CHRONO_UNIVERSE + i - 1} univers. */
    public static final Skin[] CHRONOS = {CHRONO_HOURGLASS, CHRONO_POCKET_WATCH, CHRONO_SUNDIAL, CHRONO_METRONOME};
    public static final int FIRST_CHRONO_UNIVERSE = 3;
    /** Les skins du rond de croissance (version 0.196) et les ronds cueillis qui les débloquent. */
    public static final Skin[] RINGS = {RING_FAIRY, RING_HALO, RING_SUNRISE};
    public static final long[] RING_CAUGHT = {5, 20, 50};
    /** Les skins de la météo (version 0.196) et les orages qui les débloquent. */
    public static final Skin[] WEATHERS = {WEATHER_TV, WEATHER_UKIYOE, WEATHER_PASTEL};
    public static final long[] WEATHER_STORMS = {1, 5, 10};
    /** Les skins des saisons (version 0.196) : {@code SEASON_SKINS[n - 1]} au n-ième cycle complet. */
    public static final Skin[] SEASON_SKINS = {SEASONS_CARDS, SEASONS_WHEEL, SEASONS_STAINED};

    /** Les skins du détecteur (version 0.195) et les nombres de collisions qui les débloquent. */
    public static final Skin[] DETECTORS = {DETECTOR_EYE, DETECTOR_LANTERN, DETECTOR_BELL, DETECTOR_CAMERA};
    public static final int[] DETECTOR_COLLISIONS = {100, 250, 500, 1_000};
    /** Les skins des éclairs du croisement (version 0.195) et les nombres de croisements qui les débloquent. */
    public static final Skin[] LIGHTNINGS = {LIGHTNING_GOLD, LIGHTNING_ICE, LIGHTNING_ROOTS};
    public static final int[] LIGHTNING_CROSSINGS = {1_000, 2_500, 5_000};
    /** Les skins des quarks qui s'envolent (version 0.195) et les nombres de hadrons liés qui les débloquent. */
    public static final Skin[] QUARK_FLIGHTS = {QUARK_FLIGHT_BALLOONS, QUARK_FLIGHT_BUTTERFLIES, QUARK_FLIGHT_SCRABBLE};
    public static final int[] QUARK_FLIGHT_HADRONS = {10, 25, 50};
    /** Les trois premiers skins des quarks de la liaison (version 0.195) et les nombres de hadrons liés qui les débloquent ; le Puzzle demande chaque sorte. */
    public static final Skin[] QUARK_SKINS = {QUARKS_MARBLES, QUARKS_FRUITS, QUARKS_NOTES};
    public static final int[] QUARK_SKIN_HADRONS = {10, 50, 100};
    /** Les skins des gluons (version 0.195) et les nombres de gluons qui les débloquent. */
    public static final Skin[] GLUON_SKINS = {GLUONS_SPRINGS, GLUONS_RUBBER, GLUONS_WOOL, GLUONS_LIGHTNING};
    public static final int[] GLUON_SKIN_GLUONS = {100, 250, 500, 1_000};
    /** Les skins de l'écume du vide (version 0.195) : le premier au vide ouvert, les suivants à une amélioration du vide au niveau 1, 2, 3. */
    public static final Skin[] FOAMS = {FOAM_SOAP, FOAM_BEER, FOAM_FILM, FOAM_TV};
    /** Les skins des paires du vide (version 0.195) et les nombres de paires nées qui les débloquent. */
    public static final Skin[] PAIR_SKINS = {PAIRS_TWINS, PAIRS_MAGNETS, PAIRS_YINYANG};
    public static final int[] PAIR_SKIN_BORN = {100, 1_000, 10_000};
    /** Les skins du trou noir du vide (version 0.195) : {@code VOID_HOLES[n - 1]} quand il est ouvert et compte n univers. */
    public static final Skin[] VOID_HOLES = {VOID_HOLE_MANHOLE, VOID_HOLE_WELL, VOID_HOLE_IRIS, VOID_HOLE_KNOT, VOID_HOLE_TEA};

    /** Les skins de la galaxie (version 0.194), dans l'ordre : le premier avec la galaxie formée, puis un par univers formé à partir du deuxième. */
    public static final Skin[] GALAXIES = {GALAXY_NAUTILUS, GALAXY_YARN, GALAXY_FINGERPRINT, GALAXY_WHIRLPOOL, GALAXY_FIREWORK};
    /** Les skins de l'amas de galaxies (version 0.194) : le premier avec l'amas formé, puis un par univers formé à partir du deuxième. */
    public static final Skin[] CLUSTERS = {CLUSTER_GRAPES, CLUSTER_LANTERNS, CLUSTER_BEES};
    /** Les skins de la toile de l'univers (version 0.194) : un par univers formé, du premier au quatrième. */
    public static final Skin[] WEBS = {WEB_NEURONS, WEB_LACE, WEB_DEW, WEB_METRO};
    /** Les skins des blocs d'assemblage (version 0.194) et les nombres d'assemblages formés qui les débloquent. */
    public static final Skin[] BLOCKS = {BLOCKS_STONE, BLOCKS_INGOTS, BLOCKS_CRYSTALS, BLOCKS_ICE};
    public static final int[] BLOCKS_FORMED = {10, 15, 20, 30};
    /** Les skins des sondes (version 0.194) et les nombres de voyages qui les débloquent. */
    public static final Skin[] PROBES = {PROBE_PAPER, PROBE_BALLOON, PROBE_BOTTLE, PROBE_PIGEON};
    public static final int[] PROBE_RUNS = {10, 25, 50, 100};

    /** Les skins du fond des particules (version 0.192), dans l'ordre des paliers de particules créées qui les débloquent : 1e6, 1e9… 1e24. */
    public static final Skin[] DUSTS = {DUST_SNOW, DUST_RAIN, DUST_POLLEN, DUST_FIREFLIES, DUST_BUBBLES, DUST_GOLD, DUST_ASH};
    /** L'exposant du premier palier de {@link #DUSTS}, et l'écart entre deux paliers. */
    public static final int FIRST_DUST_EXPONENT = 6;
    public static final int DUST_EXPONENT_STEP = 3;

    /** Les skins des dix dimensions (version 0.187), dans l'ordre : {@code DIMENSION_SKINS[n - 1]} pour la dimension n. */
    public static final Skin[] DIMENSION_SKINS = {LINE_RAILS, PLANE_NOTEBOOK, VOLUME_AQUARIUM, DURATION_VINYL, LOOP_ARCHERY, BRANE_WEB,
        CLICKER_BUBBLES, GRID_MINESWEEPER, MIRROR_STAINED, MIX_KALEIDOSCOPE};

    /** Les skins des astres (version 0.186) et les nombres d'astres formés qui les débloquent. */
    public static final Skin[] BODIES = {BODY_PLANETARIUM, BODY_ENGRAVING, BODY_CARTOGRAPHY};
    public static final int[] BODIES_FORMED = {10, 20, 35};

    /** Les essences de l'arbre (version 0.185), dans l'ordre des univers qui les débloquent : la première au quatrième. */
    public static final Skin[] TREES = {TREE_BIRCH, TREE_WILLOW, TREE_MAPLE, TREE_CHERRY, TREE_BAOBAB, TREE_SEQUOIA, TREE_PINE, TREE_PALM};
    /** L'univers formé qui débloque la première essence. */
    public static final int FIRST_TREE_UNIVERSE = 4;

    /** Ce qu'ajoute chaque skin débloqué à toutes les ressources d'en dessous de l'univers : +2 %. */
    public static final double BONUS = 0.02;

    /**
     * Version 0.192 : le bonus des skins s'arrête à cent skins comptés (×3 au plus), pour que les centaines de skins
     * ajoutés depuis la 0.185 ne déséquilibrent pas la partie ; les skins libres ({@link #isFree()}) ne comptent pas.
     */
    public static final int MAX_COUNTED = 100;

    private final SkinVisual visual;
    private final String label;
    private final String source;
    private final boolean free;

    /** L'identifiant, calculé une fois (version 0.170) : le jeu le demande à chaque pas pour chaque skin. */
    private final String id;

    Skin(SkinVisual visual, String label, String source) {
        this(visual, label, source, false);
    }

    /** Version 0.192 : {@code free} vrai pour un skin libre, dès le début et sans bonus (un réglage déguisé en skin). */
    Skin(SkinVisual visual, String label, String source, boolean free) {
        this.visual = visual;
        this.label = label;
        this.source = source;
        this.free = free;
        this.id = name().toLowerCase(java.util.Locale.ROOT);
    }

    /** Vrai pour un skin libre (version 0.192) : ouvert dès l'onglet Skins, sans bonus (palettes d'accessibilité, versions figées ou silencieuses). */
    public boolean isFree() {
        return free;
    }

    /** L'identifiant gardé dans la sauvegarde. */
    public String id() {
        return id;
    }

    public SkinVisual visual() {
        return visual;
    }

    public String label() {
        return label;
    }

    /** Comment on l'obtient. */
    public String source() {
        return source;
    }

    /** Le skin de cet identifiant, ou {@code null}. */
    public static Skin byId(String id) {
        for (Skin skin : values()) if (skin.id().equals(id)) return skin;
        return null;
    }
}
