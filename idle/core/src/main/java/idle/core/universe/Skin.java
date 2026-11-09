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
    PROBE_PIGEON(SkinVisual.PROBE, "Pigeon", "100 voyages de sonde");

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
