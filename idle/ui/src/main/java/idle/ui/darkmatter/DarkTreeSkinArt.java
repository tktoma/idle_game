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
 * Version 0.203 : chaque skin a aussi son fond sous l'arbre ({@link #backdrop}) et ses en-têtes ({@link #header}) ;
 * avant, seuls les traits et les vignettes changeaient, et l'arbre restait presque le même.
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

    /** Le côté des vignettes, en pixels (version 0.203 : 26, 18 avant, trop petites pour se voir). */
    static final double ART = 26;

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

    // ------------------------------------------------------------------
    // Version 0.203 : le fond de l'arbre et ses en-têtes
    // ------------------------------------------------------------------

    /** Le côté du motif du fond, en pixels : il se répète sous tout l'arbre. */
    static final int TILE = 160;
    private static final java.util.Map<Skin, javafx.scene.layout.Background> BACKDROPS = new java.util.EnumMap<>(Skin.class);

    private static double fraction(double seed) {
        double value = Math.sin(seed * 12.9898 + 78.233) * 43758.5453;
        return value - Math.floor(value);
    }

    /**
     * Le fond de l'arbre sous ce skin (version 0.203), {@code null} pour le Classique : une couleur, et un motif de
     * {@link #TILE} pixels dessiné une fois puis répété (il ne coûte rien à l'image). « Racines » : la terre, ses cailloux
     * et ses radicelles ; « Circuit » : le vernis vert d'une carte, son quadrillage, ses pastilles et quelques pistes ;
     * « Constellation » : le ciel et ses étoiles ; « Métro » : le papier sombre d'un plan, son carroyage et ses croix.
     */
    static javafx.scene.layout.Background backdrop(Skin skin) {
        if (!draws(skin)) return null;
        return BACKDROPS.computeIfAbsent(skin, DarkTreeSkinArt::buildBackdrop);
    }

    private static javafx.scene.layout.Background buildBackdrop(Skin skin) {
        Canvas canvas = new Canvas(TILE, TILE);
        GraphicsContext g = canvas.getGraphicsContext2D();
        Color base = tile(skin, g);
        javafx.scene.SnapshotParameters parameters = new javafx.scene.SnapshotParameters();
        parameters.setFill(Color.TRANSPARENT);
        javafx.scene.image.Image image = canvas.snapshot(parameters, null);
        return new javafx.scene.layout.Background(
                new javafx.scene.layout.BackgroundFill[] {new javafx.scene.layout.BackgroundFill(base, new javafx.scene.layout.CornerRadii(10), javafx.geometry.Insets.EMPTY)},
                new javafx.scene.layout.BackgroundImage[] {new javafx.scene.layout.BackgroundImage(image, javafx.scene.layout.BackgroundRepeat.REPEAT,
                        javafx.scene.layout.BackgroundRepeat.REPEAT, javafx.scene.layout.BackgroundPosition.DEFAULT, javafx.scene.layout.BackgroundSize.DEFAULT)});
    }

    /** Dessine le motif de ce skin (rien ne touche le bord : il se répète sans couture) et rend la couleur du fond. */
    static Color tile(Skin skin, GraphicsContext g) {
        double t = TILE;
        g.clearRect(0, 0, t, t);
        g.setGlobalAlpha(1);
        g.setLineCap(StrokeLineCap.ROUND);
        switch (skin) {
            case DARK_TREE_ROOTS -> {
                for (int k = 0; k < 14; k++) {
                    double x = 10 + fraction(k * 3.1) * (t - 30), y = 10 + fraction(k * 5.7 + 1) * (t - 24), wide = 3 + 7 * fraction(k * 2.3 + 2);
                    g.setGlobalAlpha(0.25 + 0.3 * fraction(k * 7.9));
                    g.setFill(k % 3 == 0 ? Color.web("#6b5a48") : Color.web("#3a2a1a"));
                    g.fillOval(x, y, wide, wide * 0.65);
                }
                g.setStroke(ROOT_OFF);
                for (int k = 0; k < 7; k++) {
                    double x = 14 + fraction(k * 4.3 + 9) * (t - 40), y = 12 + fraction(k * 6.1 + 4) * (t - 44), length = 10 + 14 * fraction(k * 1.7);
                    g.setGlobalAlpha(0.5);
                    g.setLineWidth(1.2);
                    g.strokeLine(x, y, x + length * 0.4, y + length);
                    g.setLineWidth(0.8);
                    g.strokeLine(x + length * 0.2, y + length * 0.5, x + length * 0.2 + 5, y + length * 0.5 + 6);
                }
                g.setGlobalAlpha(1);
                return Color.web("#17110b");
            }
            case DARK_TREE_CIRCUIT -> {
                g.setStroke(Color.web("#17301f"));
                g.setLineWidth(1);
                for (int k = 1; k < 4; k++) {
                    g.strokeLine(4, k * 40, t - 4, k * 40);
                    g.strokeLine(k * 40, 4, k * 40, t - 4);
                }
                // Les pastilles : un rond de cuivre terni et son trou.
                for (int k = 0; k < 6; k++) {
                    double x = 40 * (1 + (int) (fraction(k * 2.9) * 3)), y = 40 * (1 + (int) (fraction(k * 4.7 + 3) * 3));
                    g.setFill(Color.web("#3d5a3a"));
                    g.fillOval(x - 4, y - 4, 8, 8);
                    g.setFill(Color.web("#0c1a12"));
                    g.fillOval(x - 1.5, y - 1.5, 3, 3);
                }
                // Deux pistes coudées à 45°.
                g.setStroke(Color.web("#24452e"));
                g.setLineWidth(2);
                g.strokePolyline(new double[] {20, 60, 80, 140}, new double[] {20, 20, 40, 40}, 4);
                g.strokePolyline(new double[] {20, 20, 60, 60}, new double[] {140, 100, 60, 60}, 4);
                g.strokePolyline(new double[] {100, 140, 140}, new double[] {140, 100, 60}, 3);
                return Color.web("#0c1a12");
            }
            case DARK_TREE_CONSTELLATION -> {
                for (int k = 0; k < 34; k++) {
                    double x = 4 + fraction(k * 3.7) * (t - 8), y = 4 + fraction(k * 9.1 + 2) * (t - 8), size = 0.8 + 1.6 * fraction(k * 5.3 + 1);
                    g.setGlobalAlpha(0.25 + 0.55 * fraction(k * 1.3 + 7));
                    g.setFill(k % 5 == 0 ? Color.web("#c9b8ff") : STARLIGHT);
                    g.fillOval(x, y, size, size);
                }
                // Deux étoiles à quatre branches.
                g.setStroke(STARLIGHT);
                g.setLineWidth(1);
                for (double[] star : new double[][] {{44, 52, 5}, {118, 106, 4}}) {
                    g.setGlobalAlpha(0.6);
                    g.strokeLine(star[0] - star[2], star[1], star[0] + star[2], star[1]);
                    g.strokeLine(star[0], star[1] - star[2], star[0], star[1] + star[2]);
                }
                g.setGlobalAlpha(1);
                return Color.web("#060814");
            }
            default -> {
                g.setStroke(Color.web("#1c2230"));
                g.setLineWidth(1);
                for (int k = 1; k < 4; k++) {
                    g.strokeLine(4, k * 40, t - 4, k * 40);
                    g.strokeLine(k * 40, 4, k * 40, t - 4);
                }
                g.setStroke(Color.web("#3a4458"));
                for (double[] cross : new double[][] {{40, 40}, {120, 40}, {80, 120}}) {
                    g.strokeLine(cross[0] - 4, cross[1], cross[0] + 4, cross[1]);
                    g.strokeLine(cross[0], cross[1] - 4, cross[0], cross[1] + 4);
                }
                // Le fleuve du plan : une bande bleue pâle, en biais.
                g.setStroke(Color.web("#1d3350"));
                g.setLineWidth(7);
                g.setGlobalAlpha(0.7);
                g.strokeLine(18, 148, 62, 132);
                g.strokeLine(98, 26, 142, 12);
                g.setGlobalAlpha(1);
                return Color.web("#10131a");
            }
        }
    }

    /**
     * Le style d'un en-tête de l'arbre sous ce skin (version 0.203), à ajouter à son style de toujours ; vide pour le
     * Classique. {@code color} est la couleur de la branche (ou celle de la matière noire, pour la racine).
     * « Racines » : une étiquette de jardin, bois clair bordé de brun ; « Circuit » : une sérigraphie, fond sombre, texte
     * et bord de cuivre, police à chasse fixe, coins droits ; « Constellation » : un cartouche sans fond, le nom dans la
     * couleur de la branche, bord fin ; « Métro » : un panneau de station blanc, à large bande de la couleur de la ligne.
     */
    static String header(Skin skin, String color) {
        if (!draws(skin)) return "";
        return switch (skin) {
            case DARK_TREE_ROOTS -> " -fx-background-color: linear-gradient(to bottom, #d9b98a, #b08a5a); -fx-text-fill: #2a1a0a;"
                    + " -fx-border-color: #5a3a1a; -fx-border-width: 2; -fx-background-radius: 14 14 4 4; -fx-border-radius: 14 14 4 4;";
            case DARK_TREE_CIRCUIT -> " -fx-background-color: #101f16; -fx-text-fill: #e0b070; -fx-font-family: monospace;"
                    + " -fx-border-color: #b87333; -fx-border-width: 2; -fx-background-radius: 0; -fx-border-radius: 0;";
            case DARK_TREE_CONSTELLATION -> " -fx-background-color: rgba(10, 12, 28, 0.85); -fx-text-fill: " + color + ";"
                    + " -fx-border-color: " + color + "; -fx-border-width: 1; -fx-border-style: segments(2, 4); -fx-background-radius: 3; -fx-border-radius: 3;";
            default -> " -fx-background-color: #f4f4f4; -fx-text-fill: #10131a; -fx-border-color: " + color + ";"
                    + " -fx-border-width: 0 0 0 10; -fx-background-radius: 3; -fx-border-radius: 3;";
        };
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
        // Dessinée pour 18 pixels de côté : mise à l'échelle de la vignette, autour de son centre.
        g.save();
        double zoom = s / 18;
        g.scale(zoom, zoom);
        s = 18;
        c = 9;
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
        g.restore();
    }
}
