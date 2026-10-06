package idle.core;

/**
 * Ce qu'il faut réunir, tout en même temps, pour déclencher le Big Bang ({@link Game#bigBang()}) :
 * les trois ressources du jeu, puis ce qui prouve que les trois premiers actes ont été joués
 * jusqu'au bout.
 */
public enum BigBangCondition {
    /** Des particules en main, comptées comme dans l'arbre : une seconde de production en vaut autant. */
    PARTICLES,
    /** Des atomes disponibles. Il faut avoir levé le plafond d'atomes pour en tenir autant. */
    ATOMS,
    /** De la matière noire à dépenser : celle qui reste, pas celle gagnée depuis le début. */
    DARK_MATTER,
    /** Assez de succès obtenus. */
    ACHIEVEMENTS,
    /** Tous les défis d'explosion réussis au moins une fois. */
    CHALLENGES
}
