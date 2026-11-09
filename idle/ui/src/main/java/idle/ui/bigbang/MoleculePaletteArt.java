package idle.ui.bigbang;

import idle.core.elements.Element;
import idle.core.universe.Skin;
import idle.core.universe.SkinVisual;
import java.util.Map;
import javafx.scene.paint.Color;

/**
 * Les palettes des molécules (version 0.194, {@link SkinVisual#MOLECULE_PALETTE}) : la couleur de
 * chaque atome, à la place des couleurs CPK du Classique, quelle que soit la forme choisie pour les
 * molécules ({@link SkinVisual#MOLECULE}). Trois skins libres, des réglages d'accessibilité :
 * <ul>
 *   <li>« Pastel » ({@link Skin#PALETTE_PASTEL}) : les couleurs CPK, moins saturées et tirées vers le blanc ;</li>
 *   <li>« Daltonien sûr » ({@link Skin#PALETTE_SAFE}) : la palette d'Okabe et Ito, huit couleurs que distinguent les
 *       trois formes courantes de daltonisme, une par élément courant et une par famille pour les autres ;
 *       l'hydrogène reste blanc, le carbone prend le gris ;</li>
 *   <li>« Monochrome par rayon » ({@link Skin#PALETTE_MONO}) : des gris, d'autant plus clairs que l'atome est
 *       gros, l'hydrogène le plus sombre.</li>
 * </ul>
 * Le dominant d'une molécule, que lisent la vue de l'espace et la galaxie, suit la palette.
 */
final class MoleculePaletteArt {

    /** Les huit couleurs d'Okabe et Ito, plus le gris : orange, bleu ciel, vert bleuté, jaune, bleu, vermillon, pourpre, gris. */
    private static final Color ORANGE = Color.web("#e69f00");
    private static final Color SKY_BLUE = Color.web("#56b4e9");
    private static final Color BLUISH_GREEN = Color.web("#009e73");
    private static final Color YELLOW = Color.web("#f0e442");
    private static final Color BLUE = Color.web("#0072b2");
    private static final Color VERMILLION = Color.web("#d55e00");
    private static final Color PURPLE = Color.web("#cc79a7");
    private static final Color GREY = Color.web("#999999");

    /** Les éléments courants des molécules, chacun à sa couleur sûre ; les halogènes partagent le vert bleuté. */
    private static final Map<String, Color> SAFE_SYMBOLS = Map.ofEntries(
            Map.entry("H", Color.web("#f4f6f8")), Map.entry("C", GREY), Map.entry("N", BLUE), Map.entry("O", VERMILLION),
            Map.entry("S", YELLOW), Map.entry("P", ORANGE), Map.entry("F", BLUISH_GREEN), Map.entry("Cl", BLUISH_GREEN),
            Map.entry("Br", BLUISH_GREEN), Map.entry("I", BLUISH_GREEN));

    private MoleculePaletteArt() {}

    /** Vrai si ce skin est une palette des molécules : {@code null} ou un skin d'un autre visuel, c'est le Classique. */
    static boolean draws(Skin palette) {
        return palette != null && palette.visual() == SkinVisual.MOLECULE_PALETTE;
    }

    /** La couleur de cet élément dans cette palette, {@code classic} étant sa couleur CPK ou de famille. */
    static Color color(Skin palette, Element element, Color classic) {
        return switch (palette) {
            case PALETTE_PASTEL -> classic.deriveColor(0, 0.7, 1.05, 1).interpolate(Color.WHITE, 0.42);
            case PALETTE_SAFE -> safe(element);
            case PALETTE_MONO -> Color.gray(element.number() == 1 ? 0.5 : 0.66 + 0.3 * Math.min(1, element.number() / 60.0));
            default -> classic;
        };
    }

    /** Daltonien sûr : la couleur d'un élément courant, sinon celle de sa famille. */
    private static Color safe(Element element) {
        Color known = SAFE_SYMBOLS.get(element.symbol());
        if (known != null) return known;
        return switch (element.category()) {
            case TRANSITION_METAL -> SKY_BLUE;
            case POST_TRANSITION_METAL -> PURPLE;
            case ALKALI_METAL, ALKALINE_EARTH_METAL -> ORANGE;
            case METALLOID -> YELLOW;
            case LANTHANIDE, ACTINIDE -> BLUE;
            case NOBLE_GAS -> SKY_BLUE;
            case HALOGEN -> BLUISH_GREEN;
            case NONMETAL -> VERMILLION;
        };
    }
}
