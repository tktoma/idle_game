package idle.ui.bigbang;

import idle.core.universe.Skin;
import idle.core.universe.SkinVisual;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;

/**
 * Les skins du ciel des astres (version 0.194, {@link SkinVisual#SKY}) : le fond, ce qui porte chaque
 * ligne d'astres et la façon d'écrire leur nom ; les astres eux-mêmes gardent leur skin
 * ({@link SkinVisual#BODY}).
 * <ul>
 *   <li>« Planche naturaliste » ({@link Skin#SKY_PLATE}) : un fond parchemin à fibres, un cadre à double
 *       trait, et chaque astre épinglé d'une tête de laiton ombrée, son nom en italique d'encre brune
 *       souligné d'un trait tremblé ;</li>
 *   <li>« Mobile » ({@link Skin#SKY_MOBILE}) : au-dessus de chaque ligne une tige de bois, d'où chaque
 *       astre pend à un fil, et tous se balancent ({@link #sway}) ;</li>
 *   <li>« Vitrine » ({@link Skin#SKY_SHOWCASE}) : sous chaque ligne une étagère de verre, éclairée par le
 *       bas d'une lumière froide qui monte vers les astres, et les noms sur une plaquette.</li>
 * </ul>
 */
final class SkySkinArt {

    private static final Color PARCHMENT = Color.web("#e9dcc0");
    private static final Color FIBRE = Color.web("#c9b995");
    private static final Color INK = Color.web("#4a3520");
    private static final Color BRASS = Color.web("#d8b05a");
    private static final Color BRASS_DARK = Color.web("#6a4a18");
    private static final Color NIGHT = Color.web("#07070c");
    private static final Color ROD = Color.web("#8a5a30");
    private static final Color ROD_LIGHT = Color.web("#c08a50");
    private static final Color THREAD = Color.web("#d8d2c4");
    private static final Color SHOWROOM = Color.web("#0a0f16");
    private static final Color GLASS = Color.web("#9fe0ff");
    private static final Color PLAQUE = Color.web("#1b2a38");
    private static final Color PLAQUE_TEXT = Color.web("#d6ecf8");
    private static final Color MOBILE_TEXT = Color.web("#c9c0b0");
    /** Mobile : l'amplitude du balancement, en pixels à l'échelle 1, et son rythme. */
    private static final double SWAY = 6;
    private static final double SWAY_PACE = 0.8;
    /** Mobile : la longueur du fil au-dessus du haut de l'astre, à l'échelle 1. */
    private static final double THREAD_LENGTH = 18;

    private SkySkinArt() {}

    /** Vrai si ce skin habille le ciel : {@code null} ou un autre visuel, c'est le Classique. */
    static boolean draws(Skin skin) {
        return skin != null && skin.visual() == SkinVisual.SKY;
    }

    /** La couleur des textes du ciel (le message du ciel vide) : l'encre sur le parchemin, un gris chaud sur le mobile, la plaquette de la vitrine. */
    static Color textColor(Skin skin) {
        return switch (skin) {
            case SKY_PLATE -> INK;
            case SKY_MOBILE -> MOBILE_TEXT;
            default -> PLAQUE_TEXT;
        };
    }

    /** La place à réserver au-dessus d'une ligne pour ce qui la porte : la tige et le fil du mobile ; rien sinon. */
    static double above(Skin skin, double shrink) {
        return skin == Skin.SKY_MOBILE ? (THREAD_LENGTH + 6) * shrink : 0;
    }

    /** La place à réserver sous une ligne, noms compris : l'étagère de la vitrine ; rien sinon. */
    static double below(Skin skin, double shrink) {
        return skin == Skin.SKY_SHOWCASE ? 8 * shrink : 0;
    }

    /** Le fond du ciel, sur toute la vue : parchemin, nuit, ou salle d'exposition. */
    static void background(GraphicsContext g, double w, double h, Skin skin, double time) {
        switch (skin) {
            case SKY_PLATE -> {
                g.setFill(PARCHMENT);
                g.fillRect(0, 0, w, h);
                // Des fibres : de longs traits presque horizontaux, à peine plus sombres.
                g.setStroke(FIBRE);
                g.setLineWidth(1);
                for (int fibre = 0; fibre < 60; fibre++) {
                    double y = fraction(fibre * 7.13) * h;
                    double x = fraction(fibre * 3.71) * w;
                    double length = (20 + 80 * fraction(fibre * 1.37)) * w / 600;
                    g.setGlobalAlpha(0.25 + 0.3 * fraction(fibre * 9.31));
                    g.strokeLine(x, y, Math.min(w, x + length), y + 2 * fraction(fibre * 5.9) - 1);
                }
                g.setGlobalAlpha(1);
                g.setStroke(INK);
                g.setLineWidth(1.5);
                g.strokeRect(6, 6, w - 12, h - 12);
                g.setLineWidth(0.6);
                g.strokeRect(10, 10, w - 20, h - 20);
            }
            case SKY_MOBILE -> {
                g.setFill(NIGHT);
                g.fillRect(0, 0, w, h);
                g.setFill(Color.web("#c9d6ea"));
                for (int star = 0; star < 50; star++) {
                    g.setGlobalAlpha(0.15 + 0.2 * (0.5 + 0.5 * Math.sin(time * 0.5 + star)));
                    g.fillOval(fraction(star * 12.9898) * w, fraction(star * 78.233) * h, 1.2, 1.2);
                }
                g.setGlobalAlpha(1);
            }
            default -> {
                g.setFill(SHOWROOM);
                g.fillRect(0, 0, w, h);
            }
        }
    }

    /**
     * Ce qui porte une ligne d'astres, dessiné avant eux : pour le mobile, la tige de bois à {@code top} ;
     * pour la vitrine, l'étagère de verre à {@code bottom} et sa lumière qui monte jusqu'à {@code top}.
     * La planche n'a rien.
     */
    static void line(GraphicsContext g, Skin skin, double left, double right, double top, double bottom, double shrink) {
        switch (skin) {
            case SKY_MOBILE -> {
                double thick = Math.max(2, 4 * shrink);
                g.setFill(new LinearGradient(0, top, 0, top + thick, false, CycleMethod.NO_CYCLE,
                        new Stop(0, ROD_LIGHT), new Stop(1, ROD)));
                g.fillRoundRect(left - 10 * shrink, top, right - left + 20 * shrink, thick, thick, thick);
            }
            case SKY_SHOWCASE -> {
                double glow = Math.max(0, bottom - top);
                if (glow > 0) {
                    g.setFill(new LinearGradient(0, bottom, 0, top, false, CycleMethod.NO_CYCLE,
                            new Stop(0, Color.color(GLASS.getRed(), GLASS.getGreen(), GLASS.getBlue(), 0.28)),
                            new Stop(1, Color.color(GLASS.getRed(), GLASS.getGreen(), GLASS.getBlue(), 0))));
                    g.fillRect(left - 12 * shrink, top, right - left + 24 * shrink, glow);
                }
                g.setFill(Color.color(GLASS.getRed(), GLASS.getGreen(), GLASS.getBlue(), 0.55));
                g.fillRect(left - 12 * shrink, bottom, right - left + 24 * shrink, Math.max(1.5, 3 * shrink));
                g.setFill(Color.WHITE);
                g.setGlobalAlpha(0.6);
                g.fillRect(left - 12 * shrink, bottom, right - left + 24 * shrink, 1);
                g.setGlobalAlpha(1);
            }
            default -> { }
        }
    }

    /** Le déplacement horizontal d'un astre qui se balance au bout de son fil (le mobile) ; nul pour les autres skins. */
    static double sway(Skin skin, double time, int index, double shrink) {
        return skin == Skin.SKY_MOBILE ? SWAY * shrink * Math.sin(time * SWAY_PACE + index * 1.3) : 0;
    }

    /**
     * Ce qui tient un astre, dessiné avant lui : le fil du mobile depuis la tige en {@code top} jusqu'au
     * haut de l'astre, l'épingle de la planche au-dessus de lui. {@code cx} et {@code cy} sont le centre du
     * dessin, {@code up} ce dont il monte au-dessus de ce centre.
     */
    static void holder(GraphicsContext g, Skin skin, double cx, double cy, double up, double top, double shrink) {
        switch (skin) {
            case SKY_MOBILE -> {
                g.setStroke(THREAD);
                g.setLineWidth(1);
                g.setGlobalAlpha(0.8);
                g.strokeLine(cx, top, cx, cy - up);
                g.setGlobalAlpha(1);
            }
            case SKY_PLATE -> {
                double head = Math.max(2.5, 4 * shrink);
                double y = cy - up - head * 0.4;
                g.setFill(Color.color(0, 0, 0, 0.25));
                g.fillOval(cx - head + 1.5, y - head + 1.5, 2 * head, 2 * head);
                g.setFill(BRASS);
                g.fillOval(cx - head, y - head, 2 * head, 2 * head);
                g.setStroke(BRASS_DARK);
                g.setLineWidth(0.8);
                g.strokeOval(cx - head, y - head, 2 * head, 2 * head);
            }
            default -> { }
        }
    }

    /** Le nom d'un astre, centré en {@code x}, la ligne de base en {@code y} : manuscrit et souligné, en clair sur la nuit, ou sur une plaquette. */
    static void name(GraphicsContext g, Skin skin, String name, double x, double y) {
        switch (skin) {
            case SKY_PLATE -> {
                g.setFont(SCRIPT);
                g.setFill(INK);
                g.fillText(name, x, y);
                double half = 0.5 * 5.6 * name.length();
                g.setStroke(INK);
                g.setLineWidth(0.7);
                g.strokeLine(x - half, y + 3, x + half, y + 3.6);
            }
            case SKY_SHOWCASE -> {
                double wide = 5.8 * name.length() + 10;
                g.setFill(PLAQUE);
                g.fillRoundRect(x - wide / 2, y - 10, wide, 14, 3, 3);
                g.setStroke(GLASS);
                g.setLineWidth(0.6);
                g.strokeRoundRect(x - wide / 2, y - 10, wide, 14, 3, 3);
                g.setFont(SMALL);
                g.setFill(PLAQUE_TEXT);
                g.fillText(name, x, y);
            }
            default -> {
                g.setFont(SMALL);
                g.setFill(MOBILE_TEXT);
                g.fillText(name, x, y);
            }
        }
    }

    private static final Font SCRIPT = Font.font("Serif", FontWeight.NORMAL, FontPosture.ITALIC, 12);
    private static final Font SMALL = Font.font(11);

    private static double fraction(double seed) {
        double value = Math.sin(seed) * 43758.5453;
        return value - Math.floor(value);
    }
}
