package idle.core.collider;

/**
 * Les huit générateurs du collisionneur (version 0.57, moins chers en 0.60 : les huit en trois heures environ). Le premier fait se croiser les faisceaux,
 * et chaque croisement donne de l'énergie ; chacun des suivants produit, peu à peu, des exemplaires
 * du précédent. Ils s'achètent avec l'énergie, et rien ne s'y perd ni à l'explosion ni au Big Bang.
 */
public enum ColliderGenerator {
    BEAM("Faisceaux croisés", 10, 1.4, 1),
    MAGNET("Aimant supraconducteur", 100, 1.6, 0.02),
    CAVITY("Cavité accélératrice", 1e3, 1.8, 0.01),
    SYNCHROTRON("Synchrotron", 2e4, 2.0, 0.005),
    STORAGE("Anneau de stockage", 4e5, 2.4, 0.0025),
    LINAC("Accélérateur linéaire", 1e7, 2.8, 0.0015),
    SUPERCOLLIDER("Supercollisionneur", 3e8, 3.5, 0.001),
    PLANCK("Collisionneur de Planck", 1e10, 4.5, 0.0005);

    private final String label;
    private final double baseCost;
    private final double growth;
    private final double power;

    ColliderGenerator(String label, double baseCost, double growth, double power) {
        this.label = label;
        this.baseCost = baseCost;
        this.growth = growth;
        this.power = power;
    }

    public String label() {
        return label;
    }

    /** Prix du suivant, quand {@code bought} sont déjà achetés. */
    public double cost(int bought) {
        return baseCost * Math.pow(growth, bought);
    }

    /** Ce qu'un exemplaire produit par seconde : de l'énergie pour le premier, des exemplaires du précédent pour les autres. */
    public double power() {
        return power;
    }

    /** Le générateur que celui-ci produit, ou {@code null} pour le premier. */
    public ColliderGenerator previous() {
        return ordinal() == 0 ? null : values()[ordinal() - 1];
    }
}
