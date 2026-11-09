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
    PAGE_SPECTRUM(SkinVisual.TABLE_PAGE, "Spectre", "Toutes les familles possédées");

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
