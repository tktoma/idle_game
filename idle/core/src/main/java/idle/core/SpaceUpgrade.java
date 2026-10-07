package idle.core;

/**
 * Une amélioration liée à l'espace : elle est acquise d'elle-même dès que l'expansion de la
 * matière a créé assez d'espace en tout. C'est l'espace gagné depuis le premier Big Bang qui
 * compte, pas celui qui reste libre : elle ne dépense rien, et remplir l'espace de molécules ne
 * l'éloigne pas.
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

    /**
     * Donne l'automatisme « Appui automatique » : la matière noire grossit comme si le joueur
     * tenait le clic sur elle, sans qu'il ait à le faire ({@link Game#isAutoHolding()}). Il se
     * règle dans l'onglet Automatisation.
     */
    public record AutoHold() implements Effect {}

    /**
     * Tenir le clic sur une molécule répète sa création, tant que les éléments et l'espace suivent
     * ({@link Game#isHoldCreateUnlocked()}). L'effet est dans l'interface.
     */
    public record HoldCreate() implements Effect {}

    /**
     * Donne les gestes groupés : tout rassembler, tout assembler, former tous les astres prêts, d'un
     * seul clic chacun ({@link Game#formAllSubstances()}, {@link Game#formAllAssemblies()},
     * {@link Game#formAllBodies()}).
     */
    public record BulkForm() implements Effect {}

    /**
     * Donne l'automatisme « Rassemblement automatique » : toute sorte qui atteint le nombre de
     * molécules voulu se rassemble d'elle-même, si l'espace libre suffit
     * ({@link Game#isAutoGathering()}). Il se règle dans l'onglet Automatisation.
     */
    public record AutoGather() implements Effect {}

    /**
     * Donne l'automatisme « Formation automatique » : les assemblages et les astres dont tout est
     * réuni se forment d'eux-mêmes ({@link Game#isAutoForming()}). Les échelles du cosmos (galaxie,
     * amas de galaxies, univers) restent au joueur. Il se règle dans l'onglet Automatisation.
     */
    public record AutoForm() implements Effect {}

    /**
     * Raccourcit le délai de la création automatique des molécules ({@link Game#moleculeAutomationInterval()}).
     *
     * @param seconds le nouveau délai entre deux passages, en secondes
     */
    public record CreationPace(double seconds) implements Effect {
        public CreationPace {
            if (!(seconds > 0)) throw new IllegalArgumentException("Délai invalide : " + seconds);
        }
    }

    /**
     * Multiplie pour de bon une grandeur du jeu : les atomes de chaque fusion, les particules de
     * chaque création, ou l'espace que l'expansion ajoute chaque seconde
     * ({@link Game#spaceUpgradeBoost(Molecule.Stat)}). Les premières rendent court le chemin du Big
     * Bang suivant, que le joueur doit refaire depuis un seul générateur.
     *
     * @param stat   la grandeur multipliée
     * @param factor ce par quoi elle l'est (2 = le double)
     */
    public record Boost(Molecule.Stat stat, double factor) implements Effect {
        public Boost {
            if (stat == null) throw new IllegalArgumentException("Amélioration sans grandeur");
            if (!(factor > 1)) throw new IllegalArgumentException("Facteur invalide : " + factor);
        }
    }

    public SpaceUpgrade {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Amélioration d'espace sans identifiant");
        if (space == null || space.sign() <= 0) throw new IllegalArgumentException("Espace demandé par " + id + " invalide : " + space);
        if (effect == null) throw new IllegalArgumentException("Amélioration d'espace sans effet : " + id);
    }
}
