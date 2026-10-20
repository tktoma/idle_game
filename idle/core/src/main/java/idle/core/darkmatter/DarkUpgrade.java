package idle.core.darkmatter;

import idle.core.math.BigNum;
import idle.core.upgrades.Upgrade;

/**
 * Description d'une amélioration de matière noire : une case de l'arbre, ou une amélioration
 * payée en matière noire. Donnée pure ; le niveau
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
 * @param effect     ce que rapporte chaque niveau
 * @param fork       la bifurcation dont elle est une des deux voies (version 0.069), ou {@code null} :
 *                   prendre une voie ferme l'autre jusqu'au prochain Big Bang. Une case qui suit une
 *                   bifurcation la désigne par {@code requires = "fork:<nom>"} : l'une ou l'autre voie suffit
 */
public record DarkUpgrade(String id, String name, Branch branch, BigNum baseCost, double costGrowth,
                          int maxLevel, String requires, DarkEffect effect, String fork) {

    /** Préfixe d'une case du dessus qui est une bifurcation : l'une de ses voies suffit. */
    public static final String FORK_PREFIX = "fork:";

    /**
     * Préfixe d'une liaison (version 0.070) : {@code requires = "any:<id>|<id>"}. La case s'ouvre dès que
     * l'une des voies nommées est prise. Les liaisons se paient en matière noire et se dessinent entre
     * deux branches de l'arbre.
     */
    public static final String LINK_PREFIX = "any:";

    /**
     * Préfixe d'un accord (version 0.070) : {@code requires = "all:<id>|<id>|<id>"}. C'est une liaison qui
     * demande toutes les voies nommées à la fois : un ensemble cohérent de voies, plutôt qu'un tirage.
     */
    public static final String ALL_PREFIX = "all:";

    /** Une case ordinaire, hors de toute bifurcation. */
    public DarkUpgrade(String id, String name, Branch branch, BigNum baseCost, double costGrowth,
                       int maxLevel, String requires, DarkEffect effect) {
        this(id, name, branch, baseCost, costGrowth, maxLevel, requires, effect, null);
    }

    /** Vrai si la case est l'une des deux voies d'une bifurcation. */
    public boolean isFork() {
        return fork != null;
    }

    /** Le nom de la bifurcation dont dépend cette case, ou {@code null} si sa case du dessus est une case ordinaire. */
    public String requiredFork() {
        return requires != null && requires.startsWith(FORK_PREFIX) ? requires.substring(FORK_PREFIX.length()) : null;
    }

    /** Vrai pour une liaison entre deux branches (version 0.070) : elle demande l'une ou l'autre de ses voies. */
    public boolean isLink() {
        return requires != null && (requires.startsWith(LINK_PREFIX) || requires.startsWith(ALL_PREFIX));
    }

    /** Vrai pour un accord : une liaison qui demande toutes ses voies, et non l'une ou l'autre. */
    public boolean isAccord() {
        return requires != null && requires.startsWith(ALL_PREFIX);
    }

    /** Les voies dont l'une suffit à ouvrir cette liaison, dans l'ordre ; vide pour une case ordinaire. */
    public java.util.List<String> linkedIds() {
        if (!isLink()) return java.util.List.of();
        return java.util.List.of(requires.substring(isAccord() ? ALL_PREFIX.length() : LINK_PREFIX.length()).split("\\|"));
    }

    /** Vrai pour ce qui se dessine dans l'arbre : ses trois branches et les liaisons entre elles. */
    public boolean inTree() {
        return branch.inTree() || isLink();
    }

    /** Les trois branches de l'arbre, et les améliorations payées en matière noire. */
    public enum Branch {
        /** Se paie en particules : elles sont dépensées. */
        PARTICLES,
        /** Se paie en atomes : ils sont dépensés. */
        ATOMS,
        /**
         * Se débloque par la taille de la matière noire, en mètres : il faut l'avoir atteinte,
         * mais rien n'est dépensé, la matière noire ne rétrécit pas.
         */
        SIZE,
        /**
         * Se paie en matière noire : elle est dépensée. Ces améliorations ne sont pas dans
         * l'arbre, elles ont leur propre sous-onglet.
         */
        DARK_MATTER;

        /** Vrai pour les trois branches dessinées dans l'arbre. */
        public boolean inTree() {
            return this != DARK_MATTER;
        }
    }

    public DarkUpgrade {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id manquant");
        if (branch == null) throw new IllegalArgumentException("branche manquante");
        if (baseCost.sign() <= 0) throw new IllegalArgumentException("Le prix de base doit être positif");
        if (costGrowth < 1) throw new IllegalArgumentException("Le prix ne peut pas diminuer avec le niveau");
        if (maxLevel < 1) throw new IllegalArgumentException("Il faut au moins un niveau achetable");
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
