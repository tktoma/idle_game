package idle.core;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Les trois dernières échelles du jeu, au-delà des astres : la galaxie, l'amas de galaxies,
 * l'univers. Elles se forment l'une après l'autre ({@link Game#formCosmos(Cosmos)}), ne consomment
 * rien, et chacune ajoute son gain à toutes les grandeurs que la matière augmente, le double de la
 * précédente : rien ne les défait, ni l'explosion ni le Big Bang. L'univers clôt la partie.
 *
 * <p>La galaxie demande tous les astres du catalogue ({@link Bodies}). Les deux suivantes
 * demandent la précédente et de la matière rassemblée, comptée par état comme pour un astre
 * ({@link Game#gatheredInState(Molecule.State)}) : la matière de plusieurs galaxies pour un amas,
 * dix fois plus encore pour l'univers. La galaxie formée multiplie tout par mille, et un amas de
 * molécules attire d'autant plus qu'il est gros : ces nombres, très au-dessus de ceux d'une étoile,
 * sont passés par la simulation du troisième acte. Un joueur présent forme l'amas de galaxies
 * deux heures après la galaxie et l'univers cinq heures et demie après l'amas : c'est l'espace, que
 * les gaz dévorent, qui fixe ces durées.
 */
public enum Cosmos {
    GALAXY("Galaxie", "la galaxie", 1024, matter(0, 0, 0, 0, 0)),
    CLUSTER("Amas de galaxies", "l'amas de galaxies", 2048, matter(10_000_000, 1_000_000, 200_000, 2_000_000, 100_000)),
    UNIVERSE("Univers", "l'univers", 4096, matter(100_000_000, 10_000_000, 2_000_000, 20_000_000, 1_000_000));

    private final String label;
    private final String phrase;
    private final double gain;
    private final Map<Molecule.State, Integer> matter;

    Cosmos(String label, String phrase, double gain, Map<Molecule.State, Integer> matter) {
        this.label = label;
        this.phrase = phrase;
        this.gain = gain;
        this.matter = matter;
    }

    private static Map<Molecule.State, Integer> matter(int gas, int liquid, int solid, int crystal, int metal) {
        Map<Molecule.State, Integer> needs = new EnumMap<>(Molecule.State.class);
        if (gas > 0) needs.put(Molecule.State.GAS, gas);
        if (liquid > 0) needs.put(Molecule.State.LIQUID, liquid);
        if (solid > 0) needs.put(Molecule.State.SOLID, solid);
        if (crystal > 0) needs.put(Molecule.State.CRYSTAL, crystal);
        if (metal > 0) needs.put(Molecule.State.METAL, metal);
        return Collections.unmodifiableMap(needs);
    }

    /** Nom affiché : « Amas de galaxies ». */
    public String label() {
        return label;
    }

    /** Le nom dans une phrase, avec son article : « l'amas de galaxies ». */
    public String phrase() {
        return phrase;
    }

    /** Ce que cette échelle ajoute à chacune des grandeurs une fois formée (1 024 = +102 400 %). */
    public double gain() {
        return gain;
    }

    /** La matière rassemblée qu'elle demande, par état, dans l'ordre des états. Vide pour la galaxie, qui demande les astres. */
    public Map<Molecule.State, Integer> matter() {
        return matter;
    }

    /** L'échelle qu'il faut avoir formée avant celle-ci, ou {@code null} pour la galaxie. */
    public Cosmos previous() {
        return ordinal() == 0 ? null : values()[ordinal() - 1];
    }

    /** L'échelle suivante, ou {@code null} pour l'univers. */
    public Cosmos next() {
        return ordinal() == values().length - 1 ? null : values()[ordinal() + 1];
    }
}
