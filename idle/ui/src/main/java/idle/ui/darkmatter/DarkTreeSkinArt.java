package idle.ui.darkmatter;

import idle.core.universe.Skin;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.StrokeLineCap;

/**
 * Les skins de l'arbre de matière noire (version 0.193), dessinés à côté de {@link DarkMatterTreePane} pour la garder
 * lisible : les traits entre les cases ({@link #dress}) et une petite vignette au coin de chaque case ({@link #paint}).
 * <ul>
 * <li>« Racines » : des racines brunes, plus épaisses à mesure que la case monte en niveau, bouts arrondis ; chaque
 * case porte un bulbe, qui germe une fois achetée et feuille au maximum ;</li>
 * <li>« Circuit » : des pistes de cuivre à angles nets, vertes de patine quand elles sont éteintes ; chaque case est
 * une puce à broches, dont la pastille s'allume à l'achat ;</li>
 * <li>« Constellation » : des fils d'étoiles, fins et pointillés ; chaque case est une étoile, d'autant plus brillante
 * que la case est avancée ;</li>
 * <li>« Métro » : des lignes épaisses de la couleur de la branche, arrondies ; chaque case est une station, un rond
 * blanc cerclé, plein une fois achetée, double au terminus (le maximum).</li>
 * </ul>
 * Sans skin, {@link #dress} rend aux traits leur largeur de toujours (2 px), sans pointillés ni bouts arrondis : le
 * Classique reste le même au pixel.
 */
final class DarkTreeSkinArt {

    /** Le côté des vignettes, en pixels. */
    static final double ART = 18;

    private static final Color ROOT = Color.web("#8a5a2a");
    private static final Color ROOT_OFF = Color.web("#4a3520");
    private static final Color BULB = Color.web("#c08a4a");
    private static final Color SPROUT = Color.web("#7fd07a");
    private static final Color COPPER = Color.web("#b87333");
    private static final Color PATINA = Color.web("#3f6a4a");
    private static final Color CHIP = Color.web("#1f2a1f");
    private static final Color PIN = Color.web("#d9a066");
    private static final Color LED = Color.web("#6bff8a");
    private static final Color STARLIGHT = Color.web("#e8f0ff");
    private static final Color STAR_OFF = Color.web("#55607a");
    private static final Color STATION = Color.web("#f4f4f4");
    private static final Color STATION_OFF = Color.web("#6a6a78");

    private DarkTreeSkinArt() {
    }

    /** Vrai pour les skins dessinés ici : les quatre de l'arbre. */
    static boolean draws(Skin skin) {
        return skin == Skin.DARK_TREE_ROOTS || skin == Skin.DARK_TREE_CIRCUIT || skin == Skin.DARK_TREE_CONSTELLATION
                || skin == Skin.DARK_TREE_METRO;
    }

    /**
     * Habille un trait de l'arbre.
     *
     * @param color la couleur du trait sans skin : celle de la branche, ou la couleur éteinte
     * @param on    vrai si le trait est allumé (la case qu'il mène est accessible)
     * @param level le niveau de la case qu'il mène (ou une épaisseur de principe pour le tronc), pour les racines
     */
    static void dress(Skin skin, Line line, Color color, boolean on, int level) {
        if (!draws(skin)) {
            line.setStroke(color);
            line.setStrokeWidth(2);
            line.getStrokeDashArray().clear();
            line.setStrokeLineCap(StrokeLineCap.SQUARE);
            return;
        }
        line.getStrokeDashArray().clear();
        switch (skin) {
            case DARK_TREE_ROOTS -> {
                line.setStroke(on ? ROOT : ROOT_OFF);
                line.setStrokeWidth(2 + 0.8 * Math.min(6, level));
                line.setStrokeLineCap(StrokeLineCap.ROUND);
            }
            case DARK_TREE_CIRCUIT -> {
                line.setStroke(on ? COPPER : PATINA);
                line.setStrokeWidth(3);
                line.setStrokeLineCap(StrokeLineCap.BUTT);
            }
            case DARK_TREE_CONSTELLATION -> {
                line.setStroke(on ? STARLIGHT : STAR_OFF);
                line.setStrokeWidth(1);
                line.getStrokeDashArray().addAll(2.0, 4.0);
                line.setStrokeLineCap(StrokeLineCap.ROUND);
            }
            default -> {
                // Métro : la couleur de la branche, en ligne épaisse ; éteinte, elle reste visible mais grise.
                line.setStroke(on ? color : STATION_OFF);
                line.setStrokeWidth(6);
                line.setStrokeLineCap(StrokeLineCap.ROUND);
            }
        }
    }

    /**
     * La vignette d'une case.
     *
     * @param accent    la couleur de la branche
     * @param level     le niveau acheté
     * @param maxed     vrai au maximum
     * @param available vrai si la case est accessible
     */
    static void paint(Skin skin, Canvas canvas, Color accent, int level, boolean maxed, boolean available) {
        GraphicsContext g = canvas.getGraphicsContext2D();
        double s = canvas.getWidth(), c = s / 2;
        g.clearRect(0, 0, s, canvas.getHeight());
        g.setGlobalAlpha(1);
        g.setLineWidth(1.2);
        switch (skin) {
            case DARK_TREE_ROOTS -> {
                g.setFill(available || level > 0 ? BULB : ROOT_OFF);
                g.fillOval(c - 6, c - 3, 12, 10);
                g.setStroke(ROOT);
                g.strokeOval(c - 6, c - 3, 12, 10);
                // Deux radicelles sous le bulbe.
                g.strokeLine(c - 3, c + 6, c - 5, c + 9);
                g.strokeLine(c + 3, c + 6, c + 5, c + 9);
                if (level > 0) {
                    g.setStroke(SPROUT);
                    g.setLineWidth(1.5);
                    g.strokeLine(c, c - 3, c, c - 8);
                    if (maxed) {
                        g.setFill(SPROUT);
                        g.fillOval(c, c - 9, 5, 3);
                        g.fillOval(c - 5, c - 7, 5, 3);
                    }
                }
            }
            case DARK_TREE_CIRCUIT -> {
                g.setFill(CHIP);
                g.fillRect(c - 5, c - 6, 10, 12);
                g.setStroke(available || level > 0 ? COPPER : PATINA);
                g.strokeRect(c - 5, c - 6, 10, 12);
                g.setStroke(PIN);
                for (int k = 0; k < 4; k++) {
                    double y = c - 4.5 + k * 3;
                    g.strokeLine(c - 8, y, c - 5, y);
                    g.strokeLine(c + 5, y, c + 8, y);
                }
                if (level > 0) {
                    g.setFill(maxed ? LED : COPPER);
                    g.fillOval(c - 2, c - 2, 4, 4);
                }
            }
            case DARK_TREE_CONSTELLATION -> {
                double bright = maxed ? 1 : level > 0 ? 0.8 : available ? 0.55 : 0.3;
                double size = 3 + 5 * bright;
                if (maxed) {
                    g.setGlobalAlpha(0.35);
                    g.setFill(accent);
                    g.fillOval(c - 8, c - 8, 16, 16);
                    g.setGlobalAlpha(1);
                }
                g.setFill(level > 0 || available ? STARLIGHT : STAR_OFF);
                g.setGlobalAlpha(0.4 + 0.6 * bright);
                g.fillPolygon(new double[] {c, c + size * 0.25, c + size, c + size * 0.25, c, c - size * 0.25, c - size, c - size * 0.25},
                        new double[] {c - size, c - size * 0.25, c, c + size * 0.25, c + size, c + size * 0.25, c, c - size * 0.25}, 8);
                g.setGlobalAlpha(1);
            }
            default -> {
                // Métro : la station.
                Color ring = available || level > 0 ? accent : STATION_OFF;
                g.setFill(STATION);
                g.fillOval(c - 6, c - 6, 12, 12);
                g.setStroke(ring);
                g.setLineWidth(2);
                g.strokeOval(c - 6, c - 6, 12, 12);
                if (maxed) g.strokeOval(c - 8.5, c - 8.5, 17, 17);
                if (level > 0) {
                    g.setFill(ring);
                    g.fillOval(c - 3, c - 3, 6, 6);
                }
            }
        }
    }
}
