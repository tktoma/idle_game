package idle.core;

/**
 * Description d'une amélioration : une donnée pure, sans état.
 * Le niveau possédé par le joueur est stocké dans {@link GameState}.
 *
 * @param id         identifiant stable, utilisé dans les sauvegardes (ne jamais le renommer)
 * @param name       nom affiché au joueur
 * @param baseCost   coût du premier niveau
 * @param costGrowth facteur appliqué au coût à chaque niveau acheté (1.15 = +15 %)
 * @param effect     ce que rapporte chaque niveau
 */
public record Upgrade(String id, String name, BigNum baseCost, double costGrowth, Effect effect) {

    public Upgrade {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id manquant");
        if (baseCost.sign() <= 0) throw new IllegalArgumentException("Le coût de base doit être positif");
        if (costGrowth < 1) throw new IllegalArgumentException("Le coût ne peut pas diminuer avec le niveau");
    }

    /** Coût du prochain achat quand on possède déjà {@code level} niveaux. */
    public BigNum costAt(int level) {
        return baseCost.multiply(BigNum.of(costGrowth).pow(level));
    }
}