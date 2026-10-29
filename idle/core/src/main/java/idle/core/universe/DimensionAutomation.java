package idle.core.universe;

/**
 * L'automatisation des dimensions (version 0.150), que la bulle du sommet de l'arbre-monde ouvre. Chaque dimension a
 * ses automatismes, qui s'achètent l'un après l'autre, dans l'ordre de cette liste : d'abord celui qui joue à la place
 * du joueur quand la dimension se joue à la main (la visée du plan, la plongée du volume, l'appui de la durée), puis un
 * par amélioration, qui l'achète dès qu'elle est payable. La ligne avance seule : elle n'a que ses améliorations.
 *
 * <p>Ceux de la dimension 1 se paient en graines d'univers ; ceux de la dimension n, avec la ressource de la dimension
 * n − 1 : les segments de la ligne pour le plan, les facettes du plan pour le volume, les sphères du volume pour la durée,
 * les instants de la durée pour la boucle (version 0.153).
 */
public enum DimensionAutomation {
    LINE_YIELD(1, Kind.UPGRADE, 0, "Densité"),
    LINE_TICKS(1, Kind.UPGRADE, 1, "Graduation"),
    LINE_SPEED(1, Kind.UPGRADE, 2, "Élan"),
    LINE_BALLS(1, Kind.UPGRADE, 3, "Boules"),
    LINE_LINES(1, Kind.UPGRADE, 4, "Lignes"),
    PLANE_AIM(2, Kind.PLAY, -1, "Visée automatique"),
    PLANE_VALUE(2, Kind.UPGRADE, 0, "Valeur"),
    PLANE_SPAWN(2, Kind.UPGRADE, 1, "Réapparition"),
    PLANE_COUNT(2, Kind.UPGRADE, 2, "Nombre"),
    PLANE_SPEED(2, Kind.UPGRADE, 3, "Vitesse du point"),
    PLANE_SIZE(2, Kind.UPGRADE, 4, "Taille du point"),
    VOLUME_DIVE(3, Kind.PLAY, -1, "Plongée automatique"),
    VOLUME_TIME(3, Kind.UPGRADE, 0, "Durée"),
    VOLUME_VALUE(3, Kind.UPGRADE, 1, "Valeur"),
    VOLUME_SPAWN(3, Kind.UPGRADE, 2, "Réapparition"),
    VOLUME_COUNT(3, Kind.UPGRADE, 3, "Nombre"),
    VOLUME_SIZE(3, Kind.UPGRADE, 4, "Taille des boules"),
    DURATION_HOLD(4, Kind.PLAY, -1, "Appui automatique"),
    DURATION_SPEED(4, Kind.UPGRADE, 0, "Vitesse des spirales"),
    DURATION_COUNT(4, Kind.UPGRADE, 1, "Spirales"),
    DURATION_VALUE(4, Kind.UPGRADE, 2, "Valeur"),
    DURATION_TIME(4, Kind.UPGRADE, 3, "Temps au plus"),
    // Version 0.153 : la boucle, payée en instants de la durée.
    LOOP_PLAY(5, Kind.PLAY, -1, "Rythme automatique"),
    LOOP_SPEED(5, Kind.UPGRADE, 0, "Rapidité"),
    LOOP_ZONES(5, Kind.UPGRADE, 1, "Zones"),
    LOOP_GAIN(5, Kind.UPGRADE, 2, "Gain par zone"),
    LOOP_PRECISION(5, Kind.UPGRADE, 3, "Précision"),
    LOOP_SIZE(5, Kind.UPGRADE, 4, "Taille des zones"),
    LOOP_RINGS(5, Kind.UPGRADE, 5, "Cercles");

    /** Ce que fait l'automatisme : jouer la dimension, ou acheter une de ses améliorations. */
    public enum Kind { PLAY, UPGRADE }

    /** Le prix du premier automatisme de chaque dimension, dans la ressource qui le paie (graines, segments, facettes, sphères). */
    private static final double[] FIRST_COST = {1, 100_000, 500, 1_000, 50_000};
    /** Ce que chaque automatisme suivant de la même dimension coûte de plus : ×2 (les graines : +1 tous les deux). */
    public static final double COST_GROWTH = 2;

    private final int dimension;
    private final Kind kind;
    private final int upgrade;
    private final String label;

    DimensionAutomation(int dimension, Kind kind, int upgrade, String label) {
        this.dimension = dimension;
        this.kind = kind;
        this.upgrade = upgrade;
        this.label = label;
    }

    /** La dimension qu'il automatise : 1 la ligne, 2 le plan, 3 le volume, 4 la durée, 5 la boucle. */
    public int dimension() {
        return dimension;
    }

    public Kind kind() {
        return kind;
    }

    /** Le rang de l'amélioration qu'il achète, dans l'ordre de son enum ({@code Game.LineUpgrade}…) ; −1 pour jouer. */
    public int upgrade() {
        return upgrade;
    }

    /** « Visée automatique », ou le nom de l'amélioration qu'il achète. */
    public String label() {
        return label;
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
     * Son prix : en graines d'univers pour la ligne (1, 1, 2, 2, 3), sinon le premier prix de sa dimension ×2 par
     * automatisme d'avant.
     */
    public double cost() {
        if (dimension == 1) return 1 + step() / 2;
        return FIRST_COST[dimension - 1] * Math.pow(COST_GROWTH, step());
    }

    /** La dimension dont la ressource le paie : 0 pour les graines d'univers. */
    public int paidWith() {
        return dimension - 1;
    }
}
