package idle.core;

/**
 * Les familles du tableau périodique. Chacune a sa probabilité d'être tirée à la synthèse et
 * son type d'effet ; au sein d'une famille, tous les éléments ont la même chance, mais chacun
 * a un effet qui lui est propre ({@link Element#effect()}).
 *
 * <p>Les probabilités totalisent 100 %. Chaque famille limite le nombre d'exemplaires qu'on peut
 * posséder d'un même élément ({@link #maxCopies()}) : un élément au maximum ne sort plus, et sa
 * chance profite aux autres. Les deux familles les plus rares sont uniques : chacun de leurs
 * éléments ne s'obtient qu'une fois, et le premier obtenu débloque la synthèse automatique.
 *
 * <p>Les maximums sont des carrés, parce que la force d'un élément est la racine carrée de son
 * nombre d'exemplaires : 9 exemplaires comptent pour 3, 4 pour 2.
 */
public enum ElementCategory {

    TRANSITION_METAL("Métaux de transition", 0.30, 9, false,
            "Chacun multiplie les particules ou la vitesse d'un seul générateur, celui de sa colonne"),
    POST_TRANSITION_METAL("Métaux pauvres", 0.15, 9, false,
            "Réduisent les prix : vitesse de création, améliorations en atomes, synthèse"),
    NONMETAL("Non-métaux", 0.12, 9, false,
            "Chacun renforce une amélioration existante"),
    ALKALI_METAL("Métaux alcalins", 0.09, 9, false,
            "Chacun accélère un automatisme"),
    ALKALINE_EARTH_METAL("Métaux alcalino-terreux", 0.09, 9, false,
            "Plus d'atomes à chaque fusion"),
    METALLOID("Métalloïdes", 0.08, 9, false,
            "Synergies : des bonus qui grandissent avec le reste du jeu"),
    HALOGEN("Halogènes", 0.06, 9, false,
            "Améliorent la synthèse : tirages doubles et familles rares plus fréquentes"),
    LANTHANIDE("Lanthanides", 0.06, 4, true,
            "Multiplicateurs : ils se multiplient entre eux au lieu de s'additionner"),
    NOBLE_GAS("Gaz nobles", 0.03, 1, true,
            "Uniques. Chacun apporte un bonus majeur"),
    ACTINIDE("Actinides", 0.02, 1, true,
            "Uniques. Chacun double à la fois les particules et la vitesse");

    private final String label;
    private final double chance;
    private final int maxCopies;
    private final boolean rare;
    private final String description;

    ElementCategory(String label, double chance, int maxCopies, boolean rare, String description) {
        this.label = label;
        this.chance = chance;
        this.maxCopies = maxCopies;
        this.rare = rare;
        this.description = description;
    }

    /** Nom affiché au joueur. */
    public String label() {
        return label;
    }

    /** Probabilité de base qu'une synthèse tombe dans cette famille (0.30 = 30 %). */
    public double chance() {
        return chance;
    }

    /** Nombre maximal d'exemplaires d'un même élément de la famille. */
    public int maxCopies() {
        return maxCopies;
    }

    /** Vrai si chaque élément de la famille ne s'obtient qu'une fois. */
    public boolean unique() {
        return maxCopies == 1;
    }

    /** Vrai pour les familles dont la chance est augmentée par les effets {@link ElementEffect.Luck}. */
    public boolean rare() {
        return rare;
    }

    /** Ce que fait la famille, en une phrase, pour la légende. */
    public String description() {
        return description;
    }
}
