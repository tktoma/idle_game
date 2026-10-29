package idle.ui.universe;

/**
 * Les dix dimensions de la sous-page du même nom (version 0.081) : un nom et une phrase chacune,
 * de la ligne au point qui contient tout. Chacune ouvre sa fenêtre ({@link DimensionWindow}) ; les sept
 * premières se jouent ({@link #PLAYABLE}), les autres restent une tentative de visuel ({@link DimensionView}).
 *
 * <p>Version 0.153 : la cinquième devient la boucle, que le jeu a créée ; les embranchements, qui n'étaient qu'un
 * visuel, lui laissent la place. Version 0.154 : la sixième devient la brane, à la place de « tous les possibles ».
 * Version 0.155 : la septième devient le clicker, à la place de « tous les débuts ».
 */
public enum Dimension {
    LINE("La ligne", "Un point qui avance : une seule direction, en avant ou en arrière."),
    PLANE("Le plan", "La ligne prend de la largeur : on peut tourner."),
    VOLUME("Le volume", "Le plan prend de l'épaisseur : l'espace où tout se tient."),
    DURATION("La durée", "Le volume à chaque instant, de sa naissance à sa fin, mis bout à bout : le temps."),
    LOOP("La boucle", "La durée se referme sur elle-même : ce qui passe repasse, au même endroit, en rythme."),
    BRANE("La brane", "La boucle se tend en une membrane : tirée puis relâchée, elle ondule jusqu'à ses bords, et rebondit."),
    CLICKER("Le clicker", "La membrane se resserre en une surface qui répond au doigt : chaque clic y fait jaillir une étincelle."),
    WEAVE("La toile des débuts", "Tous les débuts et tous leurs possibles, reliés en une seule toile."),
    LAWS("Toutes les lois", "D'autres règles : des univers où la lumière, la masse ou le temps ne vont pas pareil."),
    POINT("Le point", "Tout cela à la fois, en un seul point.");

    /** Les dimensions qui existent dans le jeu, avec leur logique et leur ressource : de la ligne au clicker. */
    public static final int PLAYABLE = 7;

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
