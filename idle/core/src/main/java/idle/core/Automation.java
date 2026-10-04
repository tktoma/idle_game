package idle.core;

/**
 * Description d'un automatisme : une tâche que le jeu fait à la place du joueur, une fois
 * achetée avec des atomes. Donnée pure ; ce que le joueur possède et active est dans {@link GameState}.
 *
 * <p>Un automatisme n'agit pas en continu : il attend un délai entre deux actions. Ce délai
 * se réduit de moitié à chaque niveau de cadence acheté, jusqu'à {@code maxSpeedLevel}. Il ne
 * disparaît jamais : pour aller plus vite encore, il faut des éléments du tableau périodique.
 *
 * @param id              identifiant stable, utilisé dans les sauvegardes (ne jamais le renommer)
 * @param name            nom affiché au joueur
 * @param kind            ce que fait l'automatisme
 * @param cost            prix d'achat, en atomes, payé une seule fois
 * @param upgradeId       amélioration achetée automatiquement, pour le type {@link Kind#UPGRADE}
 * @param baseInterval    délai entre deux actions au niveau de cadence 0, en secondes
 * @param maxSpeedLevel   nombre de niveaux de cadence achetables
 * @param speedBaseCost   prix du premier niveau de cadence, en atomes
 * @param speedCostGrowth facteur appliqué à ce prix à chaque niveau de cadence
 */
public record Automation(String id, String name, Kind kind, BigNum cost, String upgradeId,
                         double baseInterval, int maxSpeedLevel, BigNum speedBaseCost, double speedCostGrowth) {

    /** Ce que fait un automatisme. */
    public enum Kind {
        /** Achète une amélioration dès que possible. */
        UPGRADE,
        /** Fusionne les générateurs dès que possible. */
        FUSION,
        /** Synthétise un élément du tableau périodique dès que possible. */
        SYNTHESIS
    }

    /** Délai de départ par défaut : une action toutes les 16 secondes. */
    public static final double DEFAULT_INTERVAL = 16;
    /** Par défaut, trois niveaux de cadence : 16 s, puis 8 s, 4 s et 2 s. */
    public static final int DEFAULT_MAX_SPEED_LEVEL = 3;
    /** Par défaut, les niveaux de cadence coûtent 5, 20 puis 80 atomes. */
    public static final BigNum DEFAULT_SPEED_COST = BigNum.of(5);
    public static final double DEFAULT_SPEED_COST_GROWTH = 4;

    public Automation {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id manquant");
        if (kind == null) throw new IllegalArgumentException("type manquant");
        if ((kind == Kind.UPGRADE) != (upgradeId != null && !upgradeId.isBlank())) {
            throw new IllegalArgumentException("Seul un automatisme d'achat désigne une amélioration");
        }
        if (cost.sign() <= 0) throw new IllegalArgumentException("Le coût doit être positif");
        if (baseInterval < 0) throw new IllegalArgumentException("Délai négatif");
        if (maxSpeedLevel < 0) throw new IllegalArgumentException("Nombre de niveaux négatif");
        if (speedBaseCost.sign() <= 0) throw new IllegalArgumentException("Le coût de cadence doit être positif");
        if (speedCostGrowth < 1) throw new IllegalArgumentException("Le coût de cadence ne peut pas diminuer");
    }

    /** Automatisme qui achète une amélioration, avec les délais et les coûts de cadence par défaut. */
    public static Automation buying(String id, String name, BigNum cost, String upgradeId) {
        return new Automation(id, name, Kind.UPGRADE, cost, upgradeId,
                DEFAULT_INTERVAL, DEFAULT_MAX_SPEED_LEVEL, DEFAULT_SPEED_COST, DEFAULT_SPEED_COST_GROWTH);
    }

    /** Automatisme qui fusionne les générateurs, avec les délais et les coûts de cadence par défaut. */
    public static Automation fusing(String id, String name, BigNum cost) {
        return new Automation(id, name, Kind.FUSION, cost, null,
                DEFAULT_INTERVAL, DEFAULT_MAX_SPEED_LEVEL, DEFAULT_SPEED_COST, DEFAULT_SPEED_COST_GROWTH);
    }

    /** Automatisme qui synthétise des éléments, avec les délais et les coûts de cadence par défaut. */
    public static Automation synthesizing(String id, String name, BigNum cost) {
        return new Automation(id, name, Kind.SYNTHESIS, cost, null,
                DEFAULT_INTERVAL, DEFAULT_MAX_SPEED_LEVEL, DEFAULT_SPEED_COST, DEFAULT_SPEED_COST_GROWTH);
    }

    /** Le même automatisme avec une autre cadence : délai de départ, nombre de niveaux et leurs prix. */
    public Automation withCadence(double baseInterval, int maxSpeedLevel, BigNum speedBaseCost, double speedCostGrowth) {
        return new Automation(id, name, kind, cost, upgradeId, baseInterval, maxSpeedLevel, speedBaseCost, speedCostGrowth);
    }

    /** Délai entre deux actions à un niveau de cadence donné, en secondes, avant les bonus des éléments. */
    public double intervalAt(int speedLevel) {
        return baseInterval / Math.pow(2, Math.min(speedLevel, maxSpeedLevel));
    }

    /** Prix du prochain niveau de cadence quand on en possède déjà {@code speedLevel}. */
    public BigNum speedCostAt(int speedLevel) {
        return Upgrade.roundUp(speedBaseCost.multiply(BigNum.of(speedCostGrowth).pow(speedLevel)));
    }
}
