package idle.core.universe;

import idle.core.GameUniverse;

/**
 * L'automatisation des dimensions, que les paliers de la graine et les univers ouvrent dimension par
 * dimension. Chaque dimension a ses automatismes, qui s'achètent l'un après l'autre, dans l'ordre de cette liste.
 *
 * <p>D'abord un automatisme par amélioration, qui l'achète dès qu'elle est payable ; puis, en dernier et
 * le plus cher, ceux qui jouent la dimension à la place du joueur, en cinq paliers de cadence : une action toutes les
 * 5 s, puis 2 s, 1 s et 0,5 s, puis sans attente ({@link #playPeriod()}). Chaque palier remplace le précédent (le plus
 * rapide acheté et en marche joue). Leur cadence ne dépend d'aucune bulle ni de la matière noire. La ligne avance seule :
 * elle n'a que ses améliorations. Avant, l'automatisme qui joue venait en premier et coûtait le moins.
 *
 * <p>Ceux de la dimension n se paient avec la ressource de la dimension n (les segments pour la ligne, les
 * facettes pour le plan…), au lieu de celle de la dimension n − 1 (et des graines d'univers pour la ligne) : il faut avoir
 * joué la dimension à la main pour l'automatiser. Les prix se comptent en seuils de la dimension, un quart d'heure de jeu
 * appliqué ({@link #cost()}).
 */
public enum DimensionAutomation {
    LINE_YIELD(1, Kind.UPGRADE, 0, "Densité"),
    LINE_TICKS(1, Kind.UPGRADE, 1, "Graduation"),
    LINE_SPEED(1, Kind.UPGRADE, 2, "Élan"),
    LINE_BALLS(1, Kind.UPGRADE, 3, "Boules"),
    LINE_LINES(1, Kind.UPGRADE, 4, "Lignes"),
    PLANE_VALUE(2, Kind.UPGRADE, 0, "Valeur"),
    PLANE_SPAWN(2, Kind.UPGRADE, 1, "Réapparition"),
    PLANE_COUNT(2, Kind.UPGRADE, 2, "Nombre"),
    PLANE_SPEED(2, Kind.UPGRADE, 3, "Vitesse du point"),
    PLANE_SIZE(2, Kind.UPGRADE, 4, "Taille du point"),
    PLANE_AIM_5S(2, 5, "Visée automatique"),
    PLANE_AIM_2S(2, 2, "Visée automatique"),
    PLANE_AIM_1S(2, 1, "Visée automatique"),
    PLANE_AIM_05S(2, 0.5, "Visée automatique"),
    PLANE_AIM(2, 0, "Visée automatique"),
    VOLUME_TIME(3, Kind.UPGRADE, 0, "Durée"),
    VOLUME_VALUE(3, Kind.UPGRADE, 1, "Valeur"),
    VOLUME_SPAWN(3, Kind.UPGRADE, 2, "Réapparition"),
    VOLUME_COUNT(3, Kind.UPGRADE, 3, "Nombre"),
    VOLUME_SIZE(3, Kind.UPGRADE, 4, "Taille des boules"),
    VOLUME_DIVE_5S(3, 5, "Plongée automatique"),
    VOLUME_DIVE_2S(3, 2, "Plongée automatique"),
    VOLUME_DIVE_1S(3, 1, "Plongée automatique"),
    VOLUME_DIVE_05S(3, 0.5, "Plongée automatique"),
    VOLUME_DIVE(3, 0, "Plongée automatique"),
    DURATION_SPEED(4, Kind.UPGRADE, 0, "Vitesse des spirales"),
    DURATION_COUNT(4, Kind.UPGRADE, 1, "Spirales"),
    DURATION_VALUE(4, Kind.UPGRADE, 2, "Valeur"),
    DURATION_TIME(4, Kind.UPGRADE, 3, "Temps au plus"),
    DURATION_HOLD_5S(4, 5, "Appui automatique"),
    DURATION_HOLD_2S(4, 2, "Appui automatique"),
    DURATION_HOLD_1S(4, 1, "Appui automatique"),
    DURATION_HOLD_05S(4, 0.5, "Appui automatique"),
    DURATION_HOLD(4, 0, "Appui automatique"),
    // La boucle.
    LOOP_SPEED(5, Kind.UPGRADE, 0, "Rapidité"),
    LOOP_ZONES(5, Kind.UPGRADE, 1, "Zones"),
    LOOP_GAIN(5, Kind.UPGRADE, 2, "Gain par zone"),
    LOOP_PRECISION(5, Kind.UPGRADE, 3, "Précision"),
    LOOP_SIZE(5, Kind.UPGRADE, 4, "Taille des zones"),
    LOOP_RINGS(5, Kind.UPGRADE, 5, "Cercles"),
    LOOP_PLAY_5S(5, 5, "Rythme automatique"),
    LOOP_PLAY_2S(5, 2, "Rythme automatique"),
    LOOP_PLAY_1S(5, 1, "Rythme automatique"),
    LOOP_PLAY_05S(5, 0.5, "Rythme automatique"),
    LOOP_PLAY(5, 0, "Rythme automatique"),
    // La brane.
    BRANE_GAIN(6, Kind.UPGRADE, 0, "Gain"),
    BRANE_ELASTICITY(6, Kind.UPGRADE, 1, "Élasticité"),
    BRANE_SIZE(6, Kind.UPGRADE, 2, "Grandeur des ondes"),
    BRANE_BOUNCE(6, Kind.UPGRADE, 3, "Rebond"),
    BRANE_COMBO(6, Kind.UPGRADE, 4, "Points en une onde"),
    BRANE_PULL_5S(6, 5, "Tirée automatique"),
    BRANE_PULL_2S(6, 2, "Tirée automatique"),
    BRANE_PULL_1S(6, 1, "Tirée automatique"),
    BRANE_PULL_05S(6, 0.5, "Tirée automatique"),
    BRANE_PULL(6, 0, "Tirée automatique"),
    // Le clicker.
    CLICKER_GAIN(7, Kind.UPGRADE, 0, "Gain"),
    CLICKER_SPEED(7, Kind.UPGRADE, 1, "Multiplicateur de cadence"),
    CLICKER_ZONES(7, Kind.UPGRADE, 2, "Zones ×2"),
    CLICKER_ZONE_MULT(7, Kind.UPGRADE, 3, "Multiplicateur des zones"),
    CLICKER_ZONE_TIME(7, Kind.UPGRADE, 4, "Durée des zones"),
    CLICKER_ZONE_COUNT(7, Kind.UPGRADE, 5, "Nombre de zones"),
    CLICKER_ZONE_SIZE(7, Kind.UPGRADE, 6, "Taille des zones"),
    CLICKER_STACK(7, Kind.UPGRADE, 7, "Zones superposées"),
    CLICKER_PLAY_5S(7, 5, "Clics automatiques"),
    CLICKER_PLAY_2S(7, 2, "Clics automatiques"),
    CLICKER_PLAY_1S(7, 1, "Clics automatiques"),
    CLICKER_PLAY_05S(7, 0.5, "Clics automatiques"),
    CLICKER_PLAY(7, 0, "Clics automatiques"),
    // La grille.
    GRID_DIVISION(8, Kind.UPGRADE, 0, "Réduction de la grille"),
    GRID_GAIN(8, Kind.UPGRADE, 1, "Gain"),
    GRID_REVEAL(8, Kind.UPGRADE, 2, "Affichage du point"),
    GRID_POINTS(8, Kind.UPGRADE, 3, "Nombre de points"),
    GRID_PROXIMITY(8, Kind.UPGRADE, 4, "Multiplicateur de proximité"),
    GRID_ATTEMPTS(8, Kind.UPGRADE, 5, "Essais"),
    GRID_PROBE_5S(8, 5, "Essais automatiques"),
    GRID_PROBE_2S(8, 2, "Essais automatiques"),
    GRID_PROBE_1S(8, 1, "Essais automatiques"),
    GRID_PROBE_05S(8, 0.5, "Essais automatiques"),
    GRID_PROBE(8, 0, "Essais automatiques"),
    // Le miroir.
    MIRROR_GAIN(9, Kind.UPGRADE, 0, "Gain"),
    MIRROR_LENGTH(9, Kind.UPGRADE, 1, "Multiplicateur de longueur"),
    MIRROR_MEMORY(9, Kind.UPGRADE, 2, "Temps de mémorisation"),
    MIRROR_MISTAKES(9, Kind.UPGRADE, 3, "Seconde chance"),
    MIRROR_BOARD(9, Kind.UPGRADE, 4, "Plateau plus vaste"),
    MIRROR_PLAY_5S(9, 5, "Miroir automatique"),
    MIRROR_PLAY_2S(9, 2, "Miroir automatique"),
    MIRROR_PLAY_1S(9, 1, "Miroir automatique"),
    MIRROR_PLAY_05S(9, 0.5, "Miroir automatique"),
    MIRROR_PLAY(9, 0, "Miroir automatique"),
    // Le mélange ; 0.159, une amélioration par mécanique mêlée.
    MIX_GAIN(10, Kind.UPGRADE, 0, "Gain"),
    MIX_MALUS(10, Kind.UPGRADE, 1, "Réduction du malus"),
    MIX_TICKS(10, Kind.UPGRADE, 2, "Graduations"),
    MIX_SPEED(10, Kind.UPGRADE, 3, "Vitesse du point"),
    MIX_DEPTH(10, Kind.UPGRADE, 4, "Profondeur"),
    MIX_CHARGE(10, Kind.UPGRADE, 5, "Charge"),
    MIX_RHYTHM(10, Kind.UPGRADE, 6, "Zone du rythme"),
    MIX_REACH(10, Kind.UPGRADE, 7, "Portée de l'onde"),
    MIX_ZONES(10, Kind.UPGRADE, 8, "Zones ×2"),
    MIX_TREASURE(10, Kind.UPGRADE, 9, "Trésor"),
    MIX_PAIRS(10, Kind.UPGRADE, 10, "Paires"),
    MIX_AUTO_5S(10, 5, "Mélange automatique"),
    MIX_AUTO_2S(10, 2, "Mélange automatique"),
    MIX_AUTO_1S(10, 1, "Mélange automatique"),
    MIX_AUTO_05S(10, 0.5, "Mélange automatique"),
    MIX_AUTO(10, 0, "Mélange automatique");

    /** Ce que fait l'automatisme : jouer la dimension, ou acheter une de ses améliorations. */
    public enum Kind { PLAY, UPGRADE }

    /**
     * Le temps entre deux actions du palier « sans attente » : le pas le plus fin des automatismes de jeu,
     * celui du plan sur un grand pas. Vingt actions par seconde au plus, pour toutes les dimensions.
     */
    public static final double INSTANT_PERIOD = 0.05;

    /**
     * Le prix du premier automatisme de chaque dimension, en seuils de la dimension, et ce que chaque
     * automatisme suivant coûte de plus. Réglés avec {@code CalibrageAutomatismes} (bots) : dimension neuve, joueur
     * appliqué qui la joue seule et achète chaque automatisme dès qu'il le peut ; le premier palier qui joue s'achète
     * vers dix minutes (9 à 14), le dernier vers une demi-heure (26 à 31 min). La ligne, sans palier : ses cinq
     * automatismes de 6 à 14 min.
     */
    private static final double[] FIRST_SHARE = {0.02, 0.01651, 0.02084, 0.01362, 0.0002, 0.01928, 0.006591, 0.1305, 0.01307, 0.0002};
    private static final double[] GROWTH = {1.8, 1.46, 1.42, 1.47, 1.88, 1.51, 1.35, 1.44, 1.52, 1.33};

    private final int dimension;
    private final Kind kind;
    private final int upgrade;
    private final String label;
    private final double period;

    DimensionAutomation(int dimension, Kind kind, int upgrade, String label) {
        this.dimension = dimension;
        this.kind = kind;
        this.upgrade = upgrade;
        this.label = label;
        this.period = Double.POSITIVE_INFINITY;
    }

    /** Un palier de l'automatisme qui joue : une action toutes les {@code period} secondes, 0 sans attente. */
    DimensionAutomation(int dimension, double period, String label) {
        this.dimension = dimension;
        this.kind = Kind.PLAY;
        this.upgrade = -1;
        this.label = label;
        this.period = period > 0 ? period : INSTANT_PERIOD;
    }

    /** La dimension qu'il automatise : 1 la ligne, 2 le plan, 3 le volume, 4 la durée, 5 la boucle, 6 la brane, 7 le clicker, 8 la grille, 9 le miroir, 10 le mélange. */
    public int dimension() { return dimension; }

    public Kind kind() { return kind; }

    /** Le rang de l'amélioration qu'il achète, dans l'ordre de son enum ({@code Game.LineUpgrade}…) ; −1 pour jouer. */
    public int upgrade() { return upgrade; }

    /**
     * Le temps entre deux actions de celui qui joue : 5, 2, 1, 0,5 s, ou {@link #INSTANT_PERIOD} pour le
     * dernier palier ; l'infini pour ceux qui achètent.
     */
    public double playPeriod() { return period; }

    /** Vrai pour le dernier palier de celui qui joue, sans attente. */
    public boolean isInstant() { return kind == Kind.PLAY && period == INSTANT_PERIOD; }

    /** « Visée automatique », ou le nom de l'amélioration qu'il achète. */
    public String label() { return label; }

    /** Le nom de la carte : celui qui joue dit sa cadence, « Visée automatique, toutes les 5 s ». */
    public String title() { return kind == Kind.UPGRADE ? label : label + ", " + cadence(); }

    /** Sa cadence en mots : « toutes les 5 s », « chaque seconde », « sans attente ». */
    public String cadence() {
        if (kind != Kind.PLAY) return "";
        if (isInstant()) return "sans attente";
        if (period == 1) return "chaque seconde";
        return "toutes les " + (period == Math.rint(period) ? String.valueOf((long) period) : String.valueOf(period).replace('.', ',')) + " s";
    }

    /** Son rang parmi ceux de sa dimension : 0 pour le premier. */
    public int step() {
        int step = 0;
        for (DimensionAutomation other : values()) {
            if (other == this) return step;
            if (other.dimension == dimension) step++;
        }
        return step;
    }

    /**
     * Les paliers de celui qui joue cette dimension, du plus lent au plus rapide (version 0.268 : une seule carte les
     * achète l'un après l'autre) ; vide pour la ligne, qui n'en a pas.
     */
    public static java.util.List<DimensionAutomation> playTiers(int dimension) {
        java.util.List<DimensionAutomation> tiers = new java.util.ArrayList<>();
        for (DimensionAutomation automation : values()) {
            if (automation.dimension == dimension && automation.kind == Kind.PLAY) tiers.add(automation);
        }
        return tiers;
    }

    /** Celui d'avant dans sa dimension, qu'il faut avoir acheté ; {@code null} pour le premier. */
    public DimensionAutomation previous() {
        DimensionAutomation before = null;
        for (DimensionAutomation other : values()) {
            if (other == this) return before;
            if (other.dimension == dimension) before = other;
        }
        return before;
    }

    /**
     * Son prix, dans la ressource de sa dimension : {@link #FIRST_SHARE} du seuil de la dimension pour le premier, puis
     * {@link #GROWTH} fois plus à chaque automatisme d'avant. Les paliers qui jouent, en dernier, coûtent donc le plus.
     */
    public double cost() {
        double threshold = GameUniverse.dimensionThreshold(dimension);
        double raw = threshold * FIRST_SHARE[dimension - 1] * Math.pow(GROWTH[dimension - 1], step());
        // Entier à partir de 100 ; au dixième en dessous (la boucle et le mélange commencent à un nœud, un fragment).
        return raw >= 100 ? Math.round(raw) : Math.round(raw * 10) / 10.0;
    }

    /** La dimension dont la ressource le paie : la sienne (version 0.209 ; la précédente avant, les graines pour la ligne). */
    public int paidWith() { return dimension; }
}
