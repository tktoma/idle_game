package idle.core.sky;

import idle.core.molecules.Molecule;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Les trois dernières échelles du jeu, au-delà des astres : la galaxie, l'amas de galaxies,
 * l'univers. Elles se forment l'une après l'autre ({@link Game#formCosmos(Cosmos)}), ne consomment
 * rien, et chacune multiplie toutes les grandeurs que la matière augmente ({@link #factor()}) :
 * rien ne les défait, ni l'explosion ni le Big Bang. L'univers clôt la partie.
 *
 * <p>Jusqu'à la version 0.067, chacune <em>ajoutait</em> son gain (1 024, 2 048, 4 096) aux bonus de
 * la matière ; mais à la galaxie, la matière multiplie déjà l'espace par plus de cent mille, et
 * ces milliers ne pesaient plus qu'un pour cent. Depuis la 0.068, elles multiplient.
 *
 * <p>La galaxie demande tous les astres du catalogue ({@link Bodies}). Les deux suivantes
 * demandent la précédente et de la matière rassemblée, comptée par état comme pour un astre
 * ({@link Game#gatheredInState(Molecule.State)}) : la matière de plusieurs galaxies pour un amas,
 * quatre fois plus encore pour l'univers (dix fois moins avant la version 0.068 : la réserve d'espace
 * accumulée en attendant les derniers astres payait tout en quelques minutes) ; et de la variété : un nombre de sortes rassemblées dans
 * chaque état ({@link #sorts()}), pour que la fin de partie ne se joue pas avec quatre molécules.
 * Chaque état réuni pour l'amas, puis pour l'univers, est un jalon ({@link idle.core.Game#cosmosMilestones()})
 * qui accélère l'espace. Les quantités sont calées par la simulation (version 0.068) : le joueur
 * arrive à la galaxie avec une grosse réserve d'espace, que les astres ne lui demandaient plus.
 */
public enum Cosmos {
    // Version 0.178 : trois états au lieu de cinq (le solide et le métal rejoignent le cristal), et les sortes
    // demandées suivent le catalogue de 73 molécules (18 gaz, 15 liquides, 40 cristaux) : les deux tiers pour
    // l'amas, les six septièmes pour l'univers. La matière demandée vaut 60 % de celle de la 0.177 (100 / 10 / 23 et
    // 400 / 40 / 92 millions) : sans les collections de rayon, qui triplaient l'espace à la fin du premier univers, la
    // formation de l'univers 1 attendait la matière 5 h 19 après l'amas au lieu de 4 h 04 (mesuré, graine 201).
    GALAXY("Galaxie", "la galaxie", 4, matter(0, 0, 0), matter(0, 0, 0)),
    CLUSTER("Amas de galaxies", "l'amas de galaxies", 2, matter(60_000_000, 6_000_000, 13_750_000), matter(12, 10, 26)),
    UNIVERSE("Univers", "l'univers", 2, matter(240_000_000, 24_000_000, 55_000_000), matter(15, 13, 34));

    private final String label;
    private final String phrase;
    private final double factor;
    private final Map<Molecule.State, Integer> matter;
    private final Map<Molecule.State, Integer> sorts;

    Cosmos(String label, String phrase, double factor, Map<Molecule.State, Integer> matter, Map<Molecule.State, Integer> sorts) {
        this.sorts = sorts;
        this.label = label;
        this.phrase = phrase;
        this.factor = factor;
        this.matter = matter;
    }

    private static Map<Molecule.State, Integer> matter(int gas, int liquid, int crystal) {
        Map<Molecule.State, Integer> needs = new EnumMap<>(Molecule.State.class);
        if (gas > 0) needs.put(Molecule.State.GAS, gas);
        if (liquid > 0) needs.put(Molecule.State.LIQUID, liquid);
        if (crystal > 0) needs.put(Molecule.State.CRYSTAL, crystal);
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

    /** Ce par quoi cette échelle multiplie chacune des grandeurs de la matière une fois formée : ×4, puis ×2, puis ×2. */
    public double factor() {
        return factor;
    }

    /** La matière rassemblée qu'elle demande, par état, dans l'ordre des états. Vide pour la galaxie, qui demande les astres. */
    public Map<Molecule.State, Integer> matter() {
        return matter;
    }

    /**
     * Le nombre de sortes rassemblées qu'elle demande dans chaque état ({@link Game#sortsInState}) :
     * la variété, à côté de la quantité. Vide pour la galaxie. Le catalogue compte 18 gaz, 15 liquides
     * et 40 cristaux (version 0.178) : l'amas de galaxies en demande les deux tiers, l'univers presque tout.
     */
    public Map<Molecule.State, Integer> sorts() {
        return sorts;
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
