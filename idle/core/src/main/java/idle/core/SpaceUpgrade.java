package idle.core;

/**
 * Une amélioration liée à l'espace : elle se prend quand l'expansion de la matière a créé assez
 * d'espace en tout ({@link Game#buySpaceUpgrade(String)}). C'est l'espace gagné depuis le premier
 * Big Bang qui compte, pas celui qui reste libre : la prendre ne dépense rien, et remplir l'espace
 * de molécules ne l'éloigne pas.
 *
 * @param id       identifiant stable (sert de clé dans la sauvegarde)
 * @param name     nom affiché
 * @param space    espace que l'expansion doit avoir créé en tout
 * @param requires identifiant de l'amélioration qu'il faut déjà posséder, ou {@code null}
 * @param effect   ce qu'elle ouvre
 */
public record SpaceUpgrade(String id, String name, BigNum space, String requires, Effect effect) {

    /**
     * Ce qu'ouvre une amélioration d'espace.
     *
     * <p>Interface scellée : pour ajouter une sorte d'effet, on ajoute un record ici, puis le
     * compilateur signale les {@code switch} à compléter.
     */
    public sealed interface Effect {}

    /** Ouvre un rayon du catalogue de molécules. */
    public record OpenKind(Molecule.Kind kind) implements Effect {}

    /** Ouvre le rassemblement des molécules selon leur état : gaz, liquides, solides, cristaux et métaux. */
    public record OpenStates() implements Effect {}

    /** Ouvre les assemblages : des roches, des minerais, des eaux, des gaz, faits de plusieurs sortes de molécules. */
    public record OpenAssemblies() implements Effect {}

    /** Ouvre les astres : des amas de roches aux planètes, faits d'assemblages, de matière et de molécules. */
    public record OpenBodies() implements Effect {}

    public SpaceUpgrade {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Amélioration d'espace sans identifiant");
        if (space == null || space.sign() <= 0) throw new IllegalArgumentException("Espace demandé par " + id + " invalide : " + space);
        if (effect == null) throw new IllegalArgumentException("Amélioration d'espace sans effet : " + id);
    }
}
