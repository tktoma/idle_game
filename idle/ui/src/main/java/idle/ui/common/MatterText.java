package idle.ui.common;

import idle.core.molecules.Molecule;

/**
 * Les cinq états de la matière tels qu'on les écrit : leur nom, et le signe qui les distingue
 * sans la couleur. Chaque état a une forme à lui, reprise partout où il est nommé et dans la
 * légende de la vue de l'espace : trois points pour un gaz (dispersé), des vagues pour un liquide,
 * un carré pour un solide, un losange pour un cristal, un triangle pour un métal.
 */
public final class MatterText {

    private MatterText() {
    }

    /** Le signe d'un état : « ∴ », « ≈ », « ■ », « ◆ », « ▲ ». */
    public static String sign(Molecule.State matter) {
        return switch (matter) {
            case GAS -> "∴";
            case LIQUID -> "≈";
            case SOLID -> "■";
            case CRYSTAL -> "◆";
            case METAL -> "▲";
        };
    }

    /** Le titre d'un lieu de rassemblement, sans signe : « Gaz », « Liquides ». */
    public static String title(Molecule.State matter) {
        return switch (matter) {
            case GAS -> "Gaz";
            case LIQUID -> "Liquides";
            case SOLID -> "Solides";
            case CRYSTAL -> "Cristaux";
            case METAL -> "Métaux";
        };
    }

    /** Le titre précédé de son signe : « ∴ Gaz ». */
    public static String signed(Molecule.State matter) {
        return sign(matter) + " " + title(matter);
    }

    /** Au pluriel, dans une phrase, avec le signe : « ∴ gaz », « ≈ liquides ». */
    public static String plural(Molecule.State matter) {
        return sign(matter) + " " + title(matter).toLowerCase(java.util.Locale.ROOT);
    }
}
