package idle.core;

/**
 * Description d'une amélioration de matière noire : une case de l'arbre. Donnée pure ; le niveau
 * possédé est dans {@link GameState}. Ces améliorations sont définitives : ni la fusion ni
 * l'explosion ne les reprennent.
 *
 * @param id         identifiant stable, utilisé dans les sauvegardes (ne jamais le renommer)
 * @param name       nom affiché au joueur
 * @param branch     branche de l'arbre : c'est elle qui dit en quoi se compte le prix
 * @param baseCost   prix du premier niveau, dans l'unité de la branche
 * @param costGrowth facteur appliqué au prix à chaque niveau acheté
 * @param maxLevel   nombre maximal de niveaux, ou {@link Upgrade#NO_LIMIT}
 * @param requires   amélioration à posséder avant celle-ci (la case du dessus), ou {@code null}
 * @param darkMatter matière noire qu'il faut posséder pour y avoir accès ; elle n'est pas dépensée.
 *                   C'est ce qui étale l'arbre sur plusieurs explosions
 * @param effect     ce que rapporte chaque niveau
 */
public record DarkUpgrade(String id, String name, Branch branch, BigNum baseCost, double costGrowth,
                          int maxLevel, String requires, int darkMatter, DarkEffect effect) {

    /** Les trois branches de l'arbre. */
    public enum Branch {
        /** Se paie en particules : elles sont dépensées. */
        PARTICLES,
        /** Se paie en atomes : ils sont dépensés. */
        ATOMS,
        /**
         * Se débloque par la taille de la matière noire, en mètres : il faut l'avoir atteinte,
         * mais rien n'est dépensé, la matière noire ne rétrécit pas.
         */
        SIZE
    }

    public DarkUpgrade {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id manquant");
        if (branch == null) throw new IllegalArgumentException("branche manquante");
        if (baseCost.sign() <= 0) throw new IllegalArgumentException("Le prix de base doit être positif");
        if (costGrowth < 1) throw new IllegalArgumentException("Le prix ne peut pas diminuer avec le niveau");
        if (maxLevel < 1) throw new IllegalArgumentException("Il faut au moins un niveau achetable");
        if (darkMatter < 0) throw new IllegalArgumentException("Seuil de matière noire négatif");
        if (effect == null) throw new IllegalArgumentException("effet manquant");
    }

    public boolean hasLimit() {
        return maxLevel != Upgrade.NO_LIMIT;
    }

    /**
     * Prix du prochain niveau quand on en possède déjà {@code level}. Les particules et les atomes
     * s'arrondissent à l'unité supérieure ; une taille à atteindre ne s'arrondit pas.
     */
    public BigNum costAt(int level) {
        BigNum cost = baseCost.multiply(BigNum.of(costGrowth).pow(level));
        return branch == Branch.SIZE ? cost : Upgrade.roundUp(cost);
    }
}
