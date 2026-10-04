package idle.core;

/**
 * Description d'une amélioration : une donnée pure, sans état.
 * Le niveau possédé par le joueur est stocké dans {@link GameState}.
 *
 * @param id         identifiant stable, utilisé dans les sauvegardes (ne jamais le renommer)
 * @param name       nom affiché au joueur
 * @param resource   ressource avec laquelle on la paie
 * @param baseCost   coût du premier niveau
 * @param costGrowth facteur appliqué au coût à chaque niveau acheté (1.15 = +15 %)
 * @param maxLevel   nombre maximal de niveaux, ou {@link #NO_LIMIT}
 * @param effect     ce que rapporte chaque niveau
 */
public record Upgrade(String id, String name, Resource resource, BigNum baseCost, double costGrowth,
                      int maxLevel, Effect effect) {

    /** Valeur de {@code maxLevel} pour une amélioration achetable sans limite. */
    public static final int NO_LIMIT = Integer.MAX_VALUE;

    public Upgrade {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id manquant");
        if (resource == null) throw new IllegalArgumentException("ressource manquante");
        if (baseCost.sign() <= 0) throw new IllegalArgumentException("Le coût de base doit être positif");
        if (costGrowth < 1) throw new IllegalArgumentException("Le coût ne peut pas diminuer avec le niveau");
        if (maxLevel < 1) throw new IllegalArgumentException("Il faut au moins un niveau achetable");
    }

    public boolean hasLimit() {
        return maxLevel != NO_LIMIT;
    }

    /**
     * Coût du prochain achat quand on possède déjà {@code level} niveaux,
     * arrondi à l'unité supérieure.
     */
    public BigNum costAt(int level) {
        return roundUp(baseCost.multiply(BigNum.of(costGrowth).pow(level)));
    }

    /** Arrondit un coût à l'unité supérieure : on ne paie jamais une fraction de ressource. */
    static BigNum roundUp(BigNum cost) {
        // Au-delà de 1e15, un double n'a plus de partie décimale : rien à arrondir.
        // Le petit retrait absorbe les erreurs d'arrondi des double (12,0000001 reste 12).
        return cost.exponent() < 15 ? BigNum.of(Math.ceil(cost.toDouble() - 1e-9)) : cost;
    }
}
