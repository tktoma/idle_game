package idle.core;

/**
 * Ce que fait une amélioration, par niveau acheté.
 *
 * <p>Interface scellée : la liste des effets possibles est fermée, donc le
 * compilateur signale tout {@code switch} qui oublierait un cas. Pour ajouter
 * un nouveau type d'effet, on ajoute un record ici et on le traite dans {@link Game}.
 */
public sealed interface Effect {

    /** Multiplie la vitesse de création de chaque générateur par {@code perLevel} à chaque niveau. */
    record MultiplySpeed(double perLevel) implements Effect {}

    /** Ajoute un générateur par niveau, qui forme ses particules en parallèle des autres. */
    record AddGenerator() implements Effect {}
}