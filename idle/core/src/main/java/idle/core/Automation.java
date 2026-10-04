package idle.core;

/**
 * Description d'un automatisme : une tâche que le jeu fait à la place du joueur, une fois
 * achetée avec des atomes. Donnée pure ; ce que le joueur possède et active est dans {@link GameState}.
 *
 * <p>Un automatisme n'agit pas en continu : il attend un délai entre deux actions. Ce délai
 * se réduit de moitié à chaque niveau de cadence acheté, et disparaît complètement à partir
 * de {@code instantLevel} : l'automatisme agit alors dès qu'il le peut.
 *
 * @param id               identifiant stable, utilisé dans les sauvegardes (ne jamais le renommer)
 * @param name             nom affiché au joueur
 * @param cost             prix d'achat, en atomes, payé une seule fois
 * @param upgradeId        amélioration achetée automatiquement, ou {@code null} pour la fusion automatique
 * @param baseInterval     délai entre deux actions au niveau de cadence 0, en secondes
 * @param instantLevel     niveau de cadence à partir duquel il n'y a plus de délai (0 = jamais de délai)
 * @param speedBaseCost    prix du premier niveau de cadence, en atomes
 * @param speedCostGrowth  facteur appliqué à ce prix à chaque niveau de cadence
 */
public record Automation(String id, String name, BigNum cost, String upgradeId,
                         double baseInterval, int instantLevel, BigNum speedBaseCost, double speedCostGrowth) {

    /** Délai de départ par défaut : une action toutes les 4 secondes. */
    public static final double DEFAULT_INTERVAL = 4;
    /** Par défaut : 4 s, 2 s, 1 s, 0,5 s, puis plus aucun délai. */
    public static final int DEFAULT_INSTANT_LEVEL = 4;
    /** Par défaut, les niveaux de cadence coûtent 2, 4, 8 puis 16 atomes. */
    public static final BigNum DEFAULT_SPEED_COST = BigNum.of(2);
    public static final double DEFAULT_SPEED_COST_GROWTH = 2;

    public Automation {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id manquant");
        if (cost.sign() <= 0) throw new IllegalArgumentException("Le coût doit être positif");
        if (baseInterval < 0) throw new IllegalArgumentException("Délai négatif");
        if (instantLevel < 0) throw new IllegalArgumentException("Niveau instantané négatif");
        if (speedBaseCost.sign() <= 0) throw new IllegalArgumentException("Le coût de cadence doit être positif");
        if (speedCostGrowth < 1) throw new IllegalArgumentException("Le coût de cadence ne peut pas diminuer");
    }

    /** Automatisme qui achète une amélioration, avec les délais et les coûts de cadence par défaut. */
    public static Automation buying(String id, String name, BigNum cost, String upgradeId) {
        if (upgradeId == null || upgradeId.isBlank()) throw new IllegalArgumentException("amélioration manquante");
        return new Automation(id, name, cost, upgradeId,
                DEFAULT_INTERVAL, DEFAULT_INSTANT_LEVEL, DEFAULT_SPEED_COST, DEFAULT_SPEED_COST_GROWTH);
    }

    /** Automatisme qui fusionne les générateurs, avec les délais et les coûts de cadence par défaut. */
    public static Automation fusing(String id, String name, BigNum cost) {
        return new Automation(id, name, cost, null,
                DEFAULT_INTERVAL, DEFAULT_INSTANT_LEVEL, DEFAULT_SPEED_COST, DEFAULT_SPEED_COST_GROWTH);
    }

    /** Le même automatisme, sans aucun délai dès l'achat. */
    public Automation instant() {
        return new Automation(id, name, cost, upgradeId, baseInterval, 0, speedBaseCost, speedCostGrowth);
    }

    public boolean isFusion() {
        return upgradeId == null;
    }

    /** Délai entre deux actions à un niveau de cadence donné, en secondes ; 0 = sans délai. */
    public double intervalAt(int speedLevel) {
        if (speedLevel >= instantLevel) return 0;
        return baseInterval / Math.pow(2, speedLevel);
    }

    /** Prix du prochain niveau de cadence quand on en possède déjà {@code speedLevel}. */
    public BigNum speedCostAt(int speedLevel) {
        return Upgrade.roundUp(speedBaseCost.multiply(BigNum.of(speedCostGrowth).pow(speedLevel)));
    }
}
