package idle.core;

import java.util.List;

/**
 * L'échelle des grandeurs : des repères connus, du proton à l'univers observable, rangés du
 * plus petit au plus grand. Elle sert à situer la taille de la matière noire à mesure qu'elle grossit.
 *
 * <p>Les tailles sont des ordres de grandeur, en mètres : assez justes pour se repérer, pas
 * pour faire de la physique.
 */
public final class SizeScale {

    /** Une année-lumière, en mètres. */
    public static final BigNum LIGHT_YEAR = BigNum.of(9.46, 15);

    public static final List<Landmark> LANDMARKS = List.of(
            new Landmark("proton", BigNum.of(1, -15)),
            new Landmark("noyau d'uranium", BigNum.of(1.5, -14)),
            new Landmark("atome d'hydrogène", BigNum.of(1, -10)),
            new Landmark("hélice d'ADN", BigNum.of(2, -9)),
            new Landmark("virus", BigNum.of(1, -7)),
            new Landmark("bactérie", BigNum.of(2, -6)),
            new Landmark("globule rouge", BigNum.of(7, -6)),
            new Landmark("cheveu", BigNum.of(8, -5)),
            new Landmark("grain de sable", BigNum.of(1, -3)),
            new Landmark("fourmi", BigNum.of(5, -3)),
            new Landmark("balle de tennis", BigNum.of(6.7, -2)),
            new Landmark("être humain", BigNum.of(1.7)),
            new Landmark("baleine bleue", BigNum.of(30)),
            new Landmark("tour Eiffel", BigNum.of(330)),
            new Landmark("mont Everest", BigNum.of(8849)),
            new Landmark("Lune", BigNum.of(3.47, 6)),
            new Landmark("Terre", BigNum.of(1.27, 7)),
            new Landmark("Jupiter", BigNum.of(1.4, 8)),
            new Landmark("Soleil", BigNum.of(1.39, 9)),
            new Landmark("distance de la Terre au Soleil", BigNum.of(1.5, 11)),
            new Landmark("système solaire", BigNum.of(9, 12)),
            new Landmark("nuage de Oort", BigNum.of(1.5, 16)),
            new Landmark("distance à l'étoile la plus proche", BigNum.of(4, 16)),
            new Landmark("Voie lactée", BigNum.of(9.5, 20)),
            new Landmark("distance à la galaxie d'Andromède", BigNum.of(2.4, 22)),
            new Landmark("superamas de la Vierge", BigNum.of(1, 24)),
            new Landmark("univers observable", BigNum.of(8.8, 26)));

    /** Taille de départ de la matière noire, en mètres : celle d'un proton ({@link Game#DARK_MATTER_START_SIZE}). */
    static final BigNum START_SIZE = BigNum.of(1, -15);

    /** Ce que rapporte un palier de taille. */
    public enum Bonus {
        /** Particules de chaque création multipliées. */
        PARTICLES,
        /** Atomes de chaque fusion augmentés. */
        ATOMS,
        /** Croissance de la matière noire multipliée. */
        EXPANSION
    }

    /**
     * Les paliers de taille : tous les repères plus grands que la taille de départ de la matière
     * noire (le proton n'en est pas un, elle part de là). Chacun, une fois atteint, donne un
     * bonus définitif ; les trois sortes de bonus alternent le long de l'échelle.
     */
    public static final List<Landmark> MILESTONES = LANDMARKS.stream()
            .filter(landmark -> landmark.size().gt(START_SIZE)).toList();

    /** Le bonus du palier de rang {@code index} dans {@link #MILESTONES} : particules, atomes, croissance, et ainsi de suite. */
    public static Bonus bonusOf(int index) {
        if (index < 0 || index >= MILESTONES.size()) throw new IllegalArgumentException("Palier inconnu : " + index);
        return Bonus.values()[index % Bonus.values().length];
    }

    /** Nombre de paliers atteints ou dépassés par cette taille. */
    public static int milestonesReached(BigNum size) {
        int reached = 0;
        for (Landmark landmark : MILESTONES) {
            if (landmark.size().lte(size)) reached++;
        }
        return reached;
    }

    /** Le plus grand repère atteint ou dépassé par cette taille, ou {@code null} si elle est plus petite que tous. */
    public static Landmark reached(BigNum size) {
        Landmark reached = null;
        for (Landmark landmark : LANDMARKS) {
            if (landmark.size().lte(size)) reached = landmark;
        }
        return reached;
    }

    /** Le prochain repère à atteindre, ou {@code null} quand ils sont tous dépassés. */
    public static Landmark next(BigNum size) {
        for (Landmark landmark : LANDMARKS) {
            if (landmark.size().gt(size)) return landmark;
        }
        return null;
    }

    private SizeScale() {}
}
