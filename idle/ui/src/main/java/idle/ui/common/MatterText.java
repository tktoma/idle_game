package idle.ui.common;

import idle.core.molecules.Molecule;
import idle.core.universe.Skin;

/**
 * Les trois états de la matière tels qu'on les écrit : leur nom, et le signe qui les distingue
 * sans la couleur. Chaque état a une forme à lui, reprise partout où il est nommé et dans la
 * légende de la vue de l'espace : trois points pour un gaz (dispersé), des vagues pour un liquide,
 * un losange pour un cristal.
 *
 * <p>Version 0.192 : les signes ont un skin ({@link #setSkin}), pris partout où l'état est nommé au prochain
 * remplissage. « Lettres » : G, L, C. « Gouttes et grains » : un nuage, une goutte ronde, un cube. « Alchimie » :
 * les dessins des symboles alchimiques de l'air (triangle barré), de l'eau (triangle renversé) et de la terre
 * (triangle renversé barré).
 */
public final class MatterText {

    /** Le skin des signes (version 0.192), commun à tout le jeu, ou {@code null} pour « ∴ ≈ ◆ ». */
    private static volatile Skin skin = null;

    private MatterText() {
    }

    /** Change la famille des signes des états de la matière ; les textes la prennent à leur prochain remplissage. */
    public static void setSkin(Skin chosen) {
        skin = chosen;
    }

    /** Le skin des signes, ou {@code null} pour les signes de toujours : pour les vérifications. */
    public static Skin skin() {
        return skin;
    }

    /** Le signe d'un état : « ∴ », « ≈ », « ◆ », ou ceux de son skin. */
    public static String sign(Molecule.State matter) {
        Skin look = skin;
        if (look == Skin.MATTER_LETTERS) {
            return switch (matter) {
                case GAS -> "G";
                case LIQUID -> "L";
                case CRYSTAL -> "C";
            };
        }
        if (look == Skin.MATTER_DROPS) {
            return switch (matter) {
                case GAS -> "☁";
                case LIQUID -> "◉";
                case CRYSTAL -> "▣";
            };
        }
        if (look == Skin.MATTER_ALCHEMY) {
            return switch (matter) {
                case GAS -> "⊼";
                case LIQUID -> "▽";
                case CRYSTAL -> "⊽";
            };
        }
        return switch (matter) {
            case GAS -> "∴";
            case LIQUID -> "≈";
            case CRYSTAL -> "◆";
        };
    }

    /** Le titre d'un lieu de rassemblement, sans signe : « Gaz », « Liquides », « Cristaux ». */
    public static String title(Molecule.State matter) {
        return switch (matter) {
            case GAS -> "Gaz";
            case LIQUID -> "Liquides";
            case CRYSTAL -> "Cristaux";
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
