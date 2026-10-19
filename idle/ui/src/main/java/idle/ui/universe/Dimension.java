package idle.ui.universe;

/**
 * Les dix dimensions de la sous-page du même nom (version 0.81) : un nom et une phrase chacune,
 * de la ligne au point qui contient tout. Sans logique pour l'instant : chacune ouvre sa fenêtre,
 * une tentative de visuel ({@link DimensionView}).
 */
public enum Dimension {
    LINE("La ligne", "Un point qui avance : une seule direction, en avant ou en arrière."),
    PLANE("Le plan", "La ligne prend de la largeur : on peut tourner."),
    VOLUME("Le volume", "Le plan prend de l'épaisseur : l'espace où tout se tient."),
    DURATION("La durée", "Le volume à chaque instant, de sa naissance à sa fin, mis bout à bout : le temps."),
    BRANCHES("Les embranchements", "À chaque choix, le temps se sépare en plusieurs suites possibles."),
    POSSIBLES("Tous les possibles", "Toutes les suites d'un même début, côte à côte, et les passages de l'une à l'autre."),
    BEGINNINGS("Tous les débuts", "D'autres commencements, chacun avec tous ses possibles."),
    WEAVE("La toile des débuts", "Tous les débuts et tous leurs possibles, reliés en une seule toile."),
    LAWS("Toutes les lois", "D'autres règles : des univers où la lumière, la masse ou le temps ne vont pas pareil."),
    POINT("Le point", "Tout cela à la fois, en un seul point.");

    private final String label;
    private final String line;

    Dimension(String label, String line) {
        this.label = label;
        this.line = line;
    }

    /** Son numéro, de 1 à 10. */
    public int number() {
        return ordinal() + 1;
    }

    public String label() {
        return label;
    }

    /** Une phrase qui dit ce qu'on y voit. */
    public String line() {
        return line;
    }

    /** La dimension de ce numéro (1 à 10). */
    public static Dimension of(int number) {
        return values()[Math.max(1, Math.min(values().length, number)) - 1];
    }
}
