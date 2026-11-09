package idle.ui.bigbang;

import idle.core.sky.Body;
import idle.core.universe.Skin;
import idle.core.universe.SkinVisual;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.StrokeLineCap;

/**
 * Les skins du dessin d'un système solaire (version 0.194, {@link SkinVisual#SYSTEM}, {@link SystemView}) :
 * le fond, les orbites, l'étoile et les planètes.
 * <ul>
 *   <li>« Orrery » ({@link Skin#SYSTEM_ORRERY}) : un fond de bois sombre, des orbites en cercles de laiton,
 *       chaque planète au bout d'une tige de laiton qui part de l'étoile, un socle à engrenage en bas ;</li>
 *   <li>« Dessin d'enfant » ({@link Skin#SYSTEM_CRAYON}) : une feuille de cahier quadrillée, des orbites
 *       au crayon de couleur qui tremblent, un Soleil à rayons jaunes autour de l'étoile ;</li>
 *   <li>« Radar » ({@link Skin#SYSTEM_RADAR}) : un écran vert, des orbites en cercles de balayage, un
 *       faisceau qui tourne, et les planètes et leurs lunes en échos : un point vif cerclé, plus pâle à
 *       mesure que le faisceau s'éloigne. Les planètes n'y sont pas dessinées : un écho les remplace.</li>
 * </ul>
 */
final class SystemSkinArt {

    private static final Color WOOD = Color.web("#1e1510");
    private static final Color BRASS = Color.web("#d6b25a");
    private static final Color BRASS_DARK = Color.web("#7a5a1e");
    private static final Color ORRERY_TEXT = Color.web("#e8d6a8");
    private static final Color ORRERY_MUTED = Color.web("#8a7a5a");
    private static final Color PAPER = Color.web("#fdf6e3");
    private static final Color GRID = Color.web("#bcd4e8");
    private static final Color CRAYON_GOOD = Color.web("#3aa655");
    private static final Color CRAYON_BAD = Color.web("#f28c28");
    private static final Color CRAYON_FREE = Color.web("#9a9a9a");
    private static final Color CRAYON_TEXT = Color.web("#2a3a5a");
    private static final Color SUN_RAY = Color.web("#f7d23e");
    private static final Color SCREEN = Color.web("#031a0a");
    private static final Color PHOSPHOR = Color.web("#39ff6a");
    private static final Color PHOSPHOR_DIM = Color.web("#1d8a3a");
    /** Radar : tours du faisceau par seconde. */
    private static final double SWEEP = 0.25;

    private SystemSkinArt() {}

    /** Vrai si ce skin habille les systèmes : {@code null} ou un autre visuel, c'est le Classique. */
    static boolean draws(Skin skin) {
        return skin != null && skin.visual() == SkinVisual.SYSTEM;
    }

    /** La couleur des textes principaux (noms des planètes). */
    static Color text(Skin skin) {
        return switch (skin) {
            case SYSTEM_ORRERY -> ORRERY_TEXT;
            case SYSTEM_CRAYON -> CRAYON_TEXT;
            default -> PHOSPHOR;
        };
    }

    /** La couleur des textes discrets (« orbite 2 », « orbite 3 libre »). */
    static Color muted(Skin skin) {
        return switch (skin) {
            case SYSTEM_ORRERY -> ORRERY_MUTED;
            case SYSTEM_CRAYON -> CRAYON_FREE;
            default -> PHOSPHOR_DIM;
        };
    }

    /** La couleur d'une orbite : occupée et bien placée, occupée et mal placée, ou libre. */
    static Color orbit(Skin skin, boolean has, boolean placed) {
        return switch (skin) {
            case SYSTEM_ORRERY -> has ? BRASS : BRASS_DARK;
            case SYSTEM_CRAYON -> !has ? CRAYON_FREE : placed ? CRAYON_GOOD : CRAYON_BAD;
            default -> has ? PHOSPHOR : PHOSPHOR_DIM;
        };
    }

    /** La couleur du voile et du texte de la zone habitable. */
    static Color zone(Skin skin) {
        return switch (skin) {
            case SYSTEM_ORRERY -> BRASS;
            case SYSTEM_CRAYON -> CRAYON_GOOD;
            default -> PHOSPHOR;
        };
    }

    /** Le fond du dessin : bois, feuille quadrillée, ou écran vert avec ses cercles de distance et son faisceau tournant depuis l'étoile. */
    static void background(GraphicsContext g, Skin skin, double w, double h, double starX, double cy, double time) {
        g.setGlobalAlpha(1);
        switch (skin) {
            case SYSTEM_ORRERY -> {
                g.setFill(WOOD);
                g.fillRect(0, 0, w, h);
                // Le fil du bois : des traits horizontaux à peine plus clairs.
                g.setStroke(Color.web("#2c2016"));
                g.setLineWidth(1);
                for (int grain = 0; grain < 18; grain++) {
                    double y = h * (grain + 0.5) / 18 + 3 * Math.sin(grain * 2.3);
                    g.strokeLine(0, y, w, y + 2 * Math.sin(grain));
                }
                // Le socle : une plaque de laiton en bas, et un engrenage sous l'étoile.
                g.setFill(BRASS_DARK);
                g.fillRect(0, h - 6, w, 6);
                g.setStroke(BRASS);
                g.setLineWidth(1);
                g.strokeLine(0, h - 6, w, h - 6);
                gear(g, starX, h - 14, 9, time * 0.4);
                g.strokeLine(starX, cy, starX, h - 14);
            }
            case SYSTEM_CRAYON -> {
                g.setFill(PAPER);
                g.fillRect(0, 0, w, h);
                g.setStroke(GRID);
                g.setLineWidth(0.8);
                for (double x = 20; x < w; x += 20) g.strokeLine(x, 0, x, h);
                for (double y = 20; y < h; y += 20) g.strokeLine(0, y, w, y);
                g.setStroke(Color.web("#e88a8a"));
                g.setLineWidth(1.2);
                g.strokeLine(36, 0, 36, h);
            }
            default -> {
                g.setFill(SCREEN);
                g.fillRect(0, 0, w, h);
                // Les cercles de distance, et le faisceau : une traînée qui pâlit derrière la ligne qui tourne.
                g.setStroke(PHOSPHOR_DIM);
                g.setLineWidth(0.8);
                g.setGlobalAlpha(0.5);
                for (int ring = 1; ring <= 6; ring++) {
                    double r = ring * Math.max(w, h) / 6;
                    g.strokeOval(starX - r, cy - r, 2 * r, 2 * r);
                }
                double reach = Math.hypot(w, h);
                double sweep = time * SWEEP * 2 * Math.PI;
                for (int trail = 0; trail < 24; trail++) {
                    double a = sweep - trail * 0.035;
                    g.setGlobalAlpha(0.35 * (1 - trail / 24.0));
                    g.setStroke(trail == 0 ? PHOSPHOR : PHOSPHOR_DIM);
                    g.setLineWidth(trail == 0 ? 1.5 : 3);
                    g.strokeLine(starX, cy, starX + reach * Math.cos(a), cy + reach * Math.sin(a));
                }
                g.setGlobalAlpha(1);
            }
        }
    }

    /** Un engrenage de laiton : une couronne dentée qui tourne. */
    private static void gear(GraphicsContext g, double x, double y, double r, double turn) {
        g.setFill(BRASS_DARK);
        g.fillOval(x - r, y - r, 2 * r, 2 * r);
        g.setStroke(BRASS);
        g.setLineWidth(1.5);
        g.strokeOval(x - r * 0.7, y - r * 0.7, 1.4 * r, 1.4 * r);
        g.setLineWidth(2.5);
        for (int tooth = 0; tooth < 8; tooth++) {
            double a = turn + tooth * Math.PI / 4;
            g.strokeLine(x + r * 0.8 * Math.cos(a), y + r * 0.8 * Math.sin(a), x + r * 1.2 * Math.cos(a), y + r * 1.2 * Math.sin(a));
        }
    }

    /**
     * Une orbite dans ce skin, par les points de son arc : un cercle de laiton à bord sombre, un trait de
     * crayon qui tremble (repassé deux fois, un peu décalé), ou un cercle de balayage.
     */
    static void orbit(GraphicsContext g, Skin skin, double[] xs, double[] ys, int points, Color color, boolean has) {
        g.setLineCap(StrokeLineCap.ROUND);
        switch (skin) {
            case SYSTEM_ORRERY -> {
                g.setGlobalAlpha(has ? 1 : 0.5);
                g.setStroke(BRASS_DARK);
                g.setLineWidth(has ? 4 : 2);
                g.strokePolyline(xs, ys, points);
                g.setStroke(color);
                g.setLineWidth(has ? 2 : 1);
                g.strokePolyline(xs, ys, points);
            }
            case SYSTEM_CRAYON -> {
                g.setGlobalAlpha(has ? 0.85 : 0.45);
                g.setStroke(color);
                g.setLineWidth(has ? 3 : 2);
                for (int pass = 0; pass < 2; pass++) {
                    for (int k = 1; k < points; k++) {
                        double wobble = 1.5 * Math.sin(k * 2.7 + pass * 4.1) + pass * 1.2;
                        g.strokeLine(xs[k - 1] + wobble, ys[k - 1] - wobble, xs[k] + wobble, ys[k] - wobble);
                    }
                }
            }
            default -> {
                g.setGlobalAlpha(has ? 0.7 : 0.3);
                g.setStroke(color);
                g.setLineWidth(1);
                g.strokePolyline(xs, ys, points);
            }
        }
        g.setGlobalAlpha(1);
        g.setLineCap(StrokeLineCap.SQUARE);
    }

    /** L'étoile dans ce skin : dans l'orrery et le dessin d'enfant, l'astre lui-même, avec ses rayons de crayon ; au radar, le centre de l'écran. */
    static void star(GraphicsContext g, Skin skin, Body star, double x, double y, Skin bodySkin, double time) {
        switch (skin) {
            case SYSTEM_CRAYON -> {
                g.setStroke(SUN_RAY);
                g.setLineWidth(3);
                g.setLineCap(StrokeLineCap.ROUND);
                for (int ray = 0; ray < 12; ray++) {
                    double a = ray * Math.PI / 6 + 0.1 * Math.sin(time + ray);
                    g.strokeLine(x + 32 * Math.cos(a), y + 32 * Math.sin(a), x + (44 + 4 * (ray % 2)) * Math.cos(a), y + (44 + 4 * (ray % 2)) * Math.sin(a));
                }
                g.setLineCap(StrokeLineCap.SQUARE);
                BodyArt.draw(g, star, x, y, 28, bodySkin);
            }
            case SYSTEM_RADAR -> {
                g.setFill(PHOSPHOR);
                g.fillOval(x - 4, y - 4, 8, 8);
                g.setStroke(PHOSPHOR);
                g.setLineWidth(1);
                g.strokeOval(x - 9, y - 9, 18, 18);
            }
            default -> {
                g.setFill(new RadialGradient(0, 0, x, y, 70, false, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.color(BRASS.getRed(), BRASS.getGreen(), BRASS.getBlue(), 0.25)),
                        new Stop(1, Color.color(BRASS.getRed(), BRASS.getGreen(), BRASS.getBlue(), 0))));
                g.fillOval(x - 70, y - 70, 140, 140);
                BodyArt.draw(g, star, x, y, 28, bodySkin);
            }
        }
    }

    /**
     * Une planète (ou une lune, {@code radius} plus petit) dans ce skin, centrée en ({@code x}, {@code y}) ;
     * {@code starX} et {@code cy} sont le centre de l'étoile. L'orrery pose d'abord sa tige depuis l'étoile ; le
     * radar la remplace par un écho, plus vif juste après le passage du faisceau.
     */
    static void planet(GraphicsContext g, Skin skin, Body body, double x, double y, double radius, double starX, double cy, Skin bodySkin,
                       double time) {
        switch (skin) {
            case SYSTEM_ORRERY -> {
                g.setStroke(BRASS);
                g.setLineWidth(radius > 8 ? 2 : 1);
                g.strokeLine(starX, cy, x, y);
                BodyArt.draw(g, body, x, y, radius, bodySkin);
            }
            case SYSTEM_RADAR -> {
                // L'écho : il s'allume quand le faisceau le balaie, puis s'éteint jusqu'au tour suivant.
                double sweep = time * SWEEP * 2 * Math.PI;
                double bearing = Math.atan2(y - cy, x - starX);
                double since = ((sweep - bearing) % (2 * Math.PI) + 2 * Math.PI) % (2 * Math.PI) / (2 * Math.PI);
                double bright = 0.35 + 0.65 * (1 - since);
                g.setGlobalAlpha(bright);
                g.setFill(PHOSPHOR);
                g.fillOval(x - radius * 0.4, y - radius * 0.4, radius * 0.8, radius * 0.8);
                g.setStroke(PHOSPHOR);
                g.setLineWidth(1);
                g.strokeOval(x - radius, y - radius, 2 * radius, 2 * radius);
                g.setGlobalAlpha(1);
            }
            default -> BodyArt.draw(g, body, x, y, radius, bodySkin);
        }
    }
}
