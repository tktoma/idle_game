package idle.core;

/** La ressource avec laquelle on paie une amélioration. */
public enum Resource {

    /** Ressource de base. Les améliorations payées en particules sont perdues à chaque fusion. */
    PARTICLES,

    /** Ressource gagnée par fusion. Les améliorations payées en atomes sont définitives. */
    ATOMS
}
