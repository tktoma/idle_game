package idle.ui.common;

import idle.core.Game;
import idle.core.universe.Skin;
import idle.core.universe.SkinVisual;
import javafx.scene.Node;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.paint.Color;

/**
 * L'apparence des skins (version 0.88) : chaque skin est un changement de palette du visuel qu'il
 * habille — une teinte, une saturation, une luminosité — posé sur sa vue, ou les couleurs mêmes
 * de ce qui se dessine (le faisceau, l'onde de l'explosion, l'éclair). Le classique ne pose rien.
 */
public final class SkinLook {

    /** Le skin déjà posé sur chaque vue, pour ne pas refaire l'effet à chaque image. */
    private static final java.util.Map<Node, String> SHOWN = new java.util.WeakHashMap<>();

    private SkinLook() {
    }

    /** Teinte (−1 à 1), saturation et luminosité (−1 à 1) de chaque skin. */
    static double[] shift(Skin skin) {
        return switch (skin) {
            case FUSION_FLASH -> new double[] {0.12, 0.4, 0.15};
            case FUSION_PRISM -> new double[] {0.55, 0.6, 0.05};
            case FUSION_LONE -> new double[] {0, -0.85, -0.05};
            case ATOM_DENSE -> new double[] {-0.25, 0.3, -0.15};
            case ATOM_STELLAR -> new double[] {0.2, 0.5, 0.2};
            case TABLE_LEAD -> new double[] {0, -0.7, -0.1};
            case TABLE_GOLD -> new double[] {0.15, 0.55, 0.1};
            case DARK_OBSIDIAN -> new double[] {0, -0.6, -0.35};
            case DARK_NEBULA -> new double[] {-0.45, 0.5, 0.1};
            case DARK_CONTRACTED -> new double[] {0.3, 0.2, -0.1};
            case BANG_AURORA -> new double[] {-0.35, 0.4, 0.05};
            case BANG_SUPERNOVA -> new double[] {0.1, 0.6, 0.15};
            case BANG_CRYSTAL -> new double[] {-0.6, -0.2, 0.25};
            case COLLIDER_NEON -> new double[] {0.6, 0.7, 0.1};
            case COLLIDER_PLASMA -> new double[] {0.25, 0.5, 0.05};
            case COLLIDER_ICE -> new double[] {-0.5, -0.3, 0.2};
            // Ceux-là changent des couleurs dessinées, pas la vue entière.
            case DARK_VORTEX, DARK_PULSAR, BEAM_GOLD -> new double[] {0, 0, 0};
        };
    }

    /** Pose sur la vue l'effet du skin choisi pour ce visuel ; ne refait rien si le choix n'a pas changé. */
    public static void apply(Node node, Game game, SkinVisual visual) {
        Skin skin = game.chosenSkin(visual);
        String shown = SHOWN.get(node);
        String id = skin == null ? "" : skin.id();
        if (id.equals(shown)) return;
        SHOWN.put(node, id);
        node.setEffect(effect(skin));
    }

    /** L'effet d'un skin, ou {@code null} pour le classique et les skins qui changent leurs couleurs dessinées. */
    public static ColorAdjust effect(Skin skin) {
        if (skin == null) return null;
        double[] shift = shift(skin);
        if (shift[0] == 0 && shift[1] == 0 && shift[2] == 0) return null;
        ColorAdjust adjust = new ColorAdjust();
        adjust.setHue(shift[0]);
        adjust.setSaturation(shift[1]);
        adjust.setBrightness(shift[2]);
        return adjust;
    }

    /** Les deux couleurs du faisceau du collisionneur. */
    public static Color[] beam(Game game, Color a, Color b) {
        if (game.chosenSkin(SkinVisual.COLLIDER_BEAM) == Skin.BEAM_GOLD) {
            return new Color[] {Color.web("#ffd36b"), Color.web("#ff9b3d")};
        }
        return new Color[] {a, b};
    }

    /** La couleur de l'onde de l'explosion de matière noire, et celle de l'éclair. */
    public static Color[] explosion(Game game, Color wave) {
        Skin skin = game.chosenSkin(SkinVisual.DARK_ANIMATION);
        if (skin == Skin.DARK_VORTEX) return new Color[] {Color.web("#7a3cff"), Color.web("#d9c8ff")};
        if (skin == Skin.DARK_PULSAR) return new Color[] {Color.web("#3cf0ff"), Color.web("#e8fdff")};
        return new Color[] {wave, Color.WHITE};
    }

    /** Une couleur écrite pour un style : {@code #rrggbb}. */
    public static String css(Color color) {
        return String.format(java.util.Locale.ROOT, "#%02x%02x%02x", (int) Math.round(color.getRed() * 255),
                (int) Math.round(color.getGreen() * 255), (int) Math.round(color.getBlue() * 255));
    }
}
