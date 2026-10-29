package idle.ui.universe;

/**
 * Les dix dimensions de la sous-page du même nom (version 0.081) : un nom et une phrase chacune,
 * de la ligne au mélange. Chacune ouvre sa fenêtre ({@link DimensionWindow}), avec sa vue ({@link DimensionView}) ;
 * toutes se jouent depuis la version 0.158 ({@link #PLAYABLE}) : avant, les dernières n'étaient qu'un visuel.
 *
 * <p>Version 0.153 : la cinquième devient la boucle, que le jeu a créée ; les embranchements, qui n'étaient qu'un
 * visuel, lui laissent la place. Version 0.154 : la sixième devient la brane, à la place de « tous les possibles ».
 * Version 0.155 : la septième devient le clicker, à la place de « tous les débuts ». Version 0.156 : la huitième devient
 * la grille, à la place de « la toile des débuts ». Version 0.157 : la neuvième devient le miroir, à la place de « toutes
 * les lois ». Version 0.158 : la dixième devient le mélange, à la place du « point » ; refait en 0.159, il mêle étape
 * par étape les mécaniques des neuf autres en un seul mini-jeu.
 */
public enum Dimension {
    LINE("La ligne", "Un point qui avance : une seule direction, en avant ou en arrière."),
    PLANE("Le plan", "La ligne prend de la largeur : on peut tourner."),
    VOLUME("Le volume", "Le plan prend de l'épaisseur : l'espace où tout se tient."),
    DURATION("La durée", "Le volume à chaque instant, de sa naissance à sa fin, mis bout à bout : le temps."),
    LOOP("La boucle", "La durée se referme sur elle-même : ce qui passe repasse, au même endroit, en rythme."),
    BRANE("La brane", "La boucle se tend en une membrane : tirée puis relâchée, elle ondule jusqu'à ses bords, et rebondit."),
    CLICKER("Le clicker", "La membrane se resserre en une surface qui répond au doigt : chaque clic y fait jaillir une étincelle."),
    GRID("La grille", "L'espace se découpe en cases : des points s'y cachent, et chaque sondage dit s'il brûle ou s'il gèle."),
    MIRROR("Le miroir", "La grille se replie sur elle-même : ce qui s'allume d'un côté doit se refaire, retourné, de l'autre."),
    MIX("Le mélange", "Toutes les dimensions à la fois : leurs mécaniques se mêlent, une à une, en un seul jeu.");

    /** Les dimensions qui existent dans le jeu, avec leur logique et leur ressource : toutes, de la ligne au mélange. */
    public static final int PLAYABLE = 10;

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
