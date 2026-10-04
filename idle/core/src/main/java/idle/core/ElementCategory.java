package idle.core;

/**
 * Les familles du tableau périodique. Chacune a sa probabilité d'être tirée à la synthèse,
 * et son bonus : plus la famille est rare, plus chaque exemplaire rapporte.
 *
 * <p>Les probabilités totalisent 100 %. Au sein d'une famille, tous les éléments ont la même chance.
 */
public enum ElementCategory {

    TRANSITION_METAL("Métaux de transition", 0.30, Aspect.PARTICLES, 0.02),
    POST_TRANSITION_METAL("Métaux pauvres", 0.15, Aspect.GENERATOR_COST, 0.03),
    NONMETAL("Non-métaux", 0.12, Aspect.SPEED, 0.02),
    ALKALI_METAL("Métaux alcalins", 0.09, Aspect.SPEED_COST, 0.04),
    ALKALINE_EARTH_METAL("Métaux alcalino-terreux", 0.09, Aspect.AUTOMATION, 0.05),
    METALLOID("Métalloïdes", 0.08, Aspect.PARTICLES, 0.08),
    HALOGEN("Halogènes", 0.06, Aspect.ATOMS, 0.05),
    LANTHANIDE("Lanthanides", 0.06, Aspect.SPEED, 0.06),
    NOBLE_GAS("Gaz nobles", 0.03, Aspect.ATOMS, 0.15),
    ACTINIDE("Actinides", 0.02, Aspect.ALL, 0.10);

    private final String label;
    private final double chance;
    private final Aspect aspect;
    private final double bonusPerCopy;

    ElementCategory(String label, double chance, Aspect aspect, double bonusPerCopy) {
        this.label = label;
        this.chance = chance;
        this.aspect = aspect;
        this.bonusPerCopy = bonusPerCopy;
    }

    /** Nom affiché au joueur. */
    public String label() {
        return label;
    }

    /** Probabilité qu'une synthèse tombe dans cette famille (0.30 = 30 %). */
    public double chance() {
        return chance;
    }

    /** L'aspect du jeu que cette famille améliore. */
    public Aspect aspect() {
        return aspect;
    }

    /** Ce que chaque exemplaire possédé ajoute à la puissance de l'aspect (0.02 = +2 %). */
    public double bonusPerCopy() {
        return bonusPerCopy;
    }
}
