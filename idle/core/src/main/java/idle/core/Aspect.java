package idle.core;

/** Un aspect du jeu que les éléments du tableau périodique peuvent améliorer. */
public enum Aspect {

    /** Particules obtenues à chaque création. */
    PARTICLES("Particules par création"),
    /** Vitesse de création des générateurs. */
    SPEED("Vitesse de création"),
    /** Coût des générateurs (réduit). */
    GENERATOR_COST("Coût des générateurs"),
    /** Coût des améliorations de vitesse (réduit). */
    SPEED_COST("Coût de la vitesse"),
    /** Atomes gagnés à chaque fusion. */
    ATOMS("Atomes par fusion"),
    /** Délai entre deux actions des automatismes (réduit). */
    AUTOMATION("Délai des automatismes"),
    /** Tous les aspects ci-dessus à la fois. */
    ALL("Tous les aspects");

    private final String label;

    Aspect(String label) {
        this.label = label;
    }

    /** Nom affiché au joueur. */
    public String label() {
        return label;
    }

    /**
     * Vrai si le bonus se lit comme une réduction (coût, délai) : la puissance {@code p}
     * divise la valeur au lieu de la multiplier.
     */
    public boolean isReduction() {
        return this == GENERATOR_COST || this == SPEED_COST || this == AUTOMATION;
    }
}
