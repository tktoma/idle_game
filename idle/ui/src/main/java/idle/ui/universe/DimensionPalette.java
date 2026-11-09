package idle.ui.universe;

import idle.core.universe.Skin;
import idle.core.universe.SkinVisual;
import javafx.scene.paint.Color;

/**
 * La palette des dix dimensions (version 0.197, {@link SkinVisual#DIMENSION_PALETTE}) : la couleur de chaque dimension,
 * que lisent l'arène ({@link DimensionView}), sa fenêtre ({@link DimensionWindow} : éclats, améliorations, étape du
 * mélange) et la carte des dimensions sous ses skins ({@link DimensionsMapSkinArt}). Cinq palettes libres, en bloc :
 * « Pastel » (les couleurs d'origine, tirées vers le blanc), « Daltonien sûr » (la palette d'Okabe et Ito, que
 * distinguent les daltoniens, complétée de deux gris), « Monochrome » (des bleus-gris de plus en plus clairs),
 * « Arc-en-ciel saturé » (dix teintes vives à égale distance) et « Sépia » (des bruns, du foncé au clair). Sans skin,
 * l'arène garde sa palette d'origine et la fenêtre ses couleurs d'origine : rien ne change.
 *
 * <p>Le skin choisi est posé par {@link #setSkin} à chaque image des fenêtres qui s'en servent, et vaut pour toutes.
 */
public final class DimensionPalette {

    private DimensionPalette() {
    }

    /** La palette d'origine de l'arène, dans l'ordre des dimensions. */
    private static final Color[] CLASSIC = {
            Color.web("#9fd0ff"), Color.web("#ffd27f"), Color.web("#c9a6ff"), Color.web("#9be7a8"), Color.web("#ff9eb5"),
            Color.web("#7fe0d4"), Color.web("#ffb36b"), Color.web("#a8b8ff"), Color.web("#ff7ad9"), Color.web("#fff1c9")};
    /** Les couleurs d'origine de la fenêtre : celle de l'univers pour la ligne, celle de ses éclats pour les autres. */
    private static final String[] CLASSIC_LABELS = {
            UniversePage.COLOR, "#ffd27f", "#c9a6ff", "#9be7a8", "#ff9eb5", "#7fe0d4", "#ffb36b", "#a8b8ff", "#ff7ad9", "#fff1c9"};

    private static final Color[] SAFE = web("#56b4e9", "#e69f00", "#cc79a7", "#009e73", "#d55e00", "#0072b2", "#f0e442", "#999999", "#ffffff", "#cccccc");
    private static final Color[] SEPIA = web("#5a3a1e", "#6e4a28", "#825a34", "#966a40", "#aa7c4e", "#be905e", "#cfa470", "#dfb886", "#ecce9e", "#f6e2bc");

    private static Color[] web(String... hex) {
        Color[] colors = new Color[hex.length];
        for (int i = 0; i < hex.length; i++) colors[i] = Color.web(hex[i]);
        return colors;
    }

    private static Skin skin;

    /** Le skin de la palette, {@code null} ou un skin d'un autre visuel pour la palette d'origine. */
    public static void setSkin(Skin chosen) {
        skin = chosen != null && chosen.visual() == SkinVisual.DIMENSION_PALETTE ? chosen : null;
    }

    public static Skin skin() {
        return skin;
    }

    /** La couleur de la dimension {@code number} (1 à 10) dans l'arène. */
    public static Color color(int number) {
        int i = Math.max(0, Math.min(9, number - 1));
        if (skin == null) return CLASSIC[i];
        return switch (skin) {
            case DIM_PALETTE_PASTEL -> CLASSIC[i].deriveColor(0, 0.6, 1.05, 1).interpolate(Color.WHITE, 0.35);
            case DIM_PALETTE_SAFE -> SAFE[i];
            case DIM_PALETTE_MONO -> Color.hsb(220, 0.18, 0.45 + 0.055 * i);
            case DIM_PALETTE_RAINBOW -> Color.hsb(i * 36, 0.95, 1);
            case DIM_PALETTE_SEPIA -> SEPIA[i];
            default -> CLASSIC[i];
        };
    }

    /** La couleur de la dimension {@code number} (1 à 10) dans la fenêtre et les feuilles de style, en « #rrggbb ». */
    public static String hex(int number) {
        if (skin == null) return CLASSIC_LABELS[Math.max(0, Math.min(9, number - 1))];
        return hex(color(number));
    }

    /** « #rrggbb » d'une couleur, sans son opacité. */
    static String hex(Color color) {
        return String.format("#%02x%02x%02x", (int) Math.round(color.getRed() * 255), (int) Math.round(color.getGreen() * 255),
                (int) Math.round(color.getBlue() * 255));
    }
}
