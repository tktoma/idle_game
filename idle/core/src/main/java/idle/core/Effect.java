package idle.core;

/**
 * Ce que fait une amélioration, par niveau acheté.
 *
 * <p>Interface scellée : la liste des effets possibles est fermée, donc le
 * compilateur signale tout {@code switch} qui oublierait un cas. Pour ajouter
 * un nouveau type d'effet, on ajoute un record ici et on le traite dans
 * {@link Game#productionPerSecond()}.
 */
public sealed interface Effect {

    /** Ajoute {@code perLevel} matière/seconde par niveau. */
    record AddProduction(BigNum perLevel) implements Effect {}

    /** Multiplie la production totale par {@code perLevel} à chaque niveau. */
    record MultiplyProduction(double perLevel) implements Effect {}
}
 