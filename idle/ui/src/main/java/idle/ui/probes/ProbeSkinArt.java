package idle.ui.probes;

import idle.core.universe.Skin;
import idle.core.universe.SkinVisual;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.StrokeLineCap;

/**
 * Les skins des sondes (version 0.194, {@link SkinVisual#PROBE}) : la sonde, sa trajectoire et le ciel
 * qu'elle traverse ({@link ProbesView}), et le dos de la carte de son butin ({@link LootPane}). Chaque
 * skin a sa trajectoire, qui le suit : il n'y a pas de visuel à part pour elle.
 * <ul>
 *   <li>« Avion de papier » ({@link Skin#PROBE_PAPER}) : un ciel de papier bleu pâle à nuages, une
 *       trajectoire en pointillés bruns de carte au trésor qui finit par une croix, un avion plié blanc
 *       à plis gris ;</li>
 *   <li>« Montgolfière » ({@link Skin#PROBE_BALLOON}) : une aube orangée, une traînée de condensation
 *       blanche qui s'élargit et pâlit derrière, une enveloppe rayée rouge et jaune sous laquelle pend
 *       une nacelle ;</li>
 *   <li>« Bouteille à la mer » ({@link Skin#PROBE_BOTTLE}) : une mer sombre à vagues, un fil d'Ariane
 *       rouge qui ondule depuis le hangar, une bouteille de verre penchée, son bouchon et son message ;</li>
 *   <li>« Pigeon » ({@link Skin#PROBE_PIGEON}) : un crépuscule violet, des plumes semées le long du
 *       chemin, un pigeon gris aux ailes qui battent.</li>
 * </ul>
 */
final class ProbeSkinArt {

    // Avion de papier
    private static final Color PAPER_SKY = Color.web("#cfe6f7");
    private static final Color CLOUD = Color.web("#ffffff");
    private static final Color MAP_INK = Color.web("#6b4a2a");
    private static final Color PAPER_TEXT = Color.web("#2a3a4a");
    private static final Color PAPER_DIM = Color.web("#6a7a8a");
    private static final Color PLANE = Color.web("#fbfbf6");
    private static final Color CREASE = Color.web("#9aa0a8");
    // Montgolfière
    private static final Color DAWN_TOP = Color.web("#2a3a6a");
    private static final Color DAWN_BOTTOM = Color.web("#f0a060");
    private static final Color CONTRAIL = Color.web("#ffffff");
    private static final Color BALLOON_RED = Color.web("#d8382a");
    private static final Color BALLOON_YELLOW = Color.web("#f2c230");
    private static final Color BASKET = Color.web("#7a5a2a");
    private static final Color DAWN_TEXT = Color.web("#fff2dc");
    private static final Color DAWN_DIM = Color.web("#d8c0a8");
    // Bouteille à la mer
    private static final Color SEA = Color.web("#0b3550");
    private static final Color WAVE = Color.web("#3d7fa8");
    private static final Color THREAD = Color.web("#e0402a");
    private static final Color GLASS = Color.web("#a8e0d8");
    private static final Color CORK = Color.web("#b08050");
    private static final Color MESSAGE = Color.web("#f4e8c8");
    private static final Color SEA_TEXT = Color.web("#dff0f8");
    private static final Color SEA_DIM = Color.web("#8fb8cc");
    // Pigeon
    private static final Color DUSK = Color.web("#2b2440");
    private static final Color DUSK_LOW = Color.web("#6a3a5a");
    private static final Color FEATHER = Color.web("#d8d0e0");
    private static final Color PIGEON = Color.web("#8a8a98");
    private static final Color PIGEON_DARK = Color.web("#4a4a58");
    private static final Color PIGEON_NECK = Color.web("#5aa08a");
    private static final Color DUSK_TEXT = Color.web("#f0e8f8");
    private static final Color DUSK_DIM = Color.web("#a898b8");

    private ProbeSkinArt() {}

    /** Vrai si ce skin habille les sondes : {@code null} ou un autre visuel, c'est le Classique. */
    static boolean draws(Skin skin) {
        return skin != null && skin.visual() == SkinVisual.PROBE;
    }

    /** La couleur des textes vifs (butin prêt, nom d'une destination atteinte, « Hangar »). */
    static Color text(Skin skin) {
        return switch (skin) {
            case PROBE_PAPER -> PAPER_TEXT;
            case PROBE_BALLOON -> DAWN_TEXT;
            case PROBE_BOTTLE -> SEA_TEXT;
            default -> DUSK_TEXT;
        };
    }

    /** La couleur des textes discrets (temps restant, place libre). */
    static Color dim(Skin skin) {
        return switch (skin) {
            case PROBE_PAPER -> PAPER_DIM;
            case PROBE_BALLOON -> DAWN_DIM;
            case PROBE_BOTTLE -> SEA_DIM;
            default -> DUSK_DIM;
        };
    }

    /** Le ciel de la vue : papier à nuages, aube, mer à vagues, crépuscule. */
    static void background(GraphicsContext g, Skin skin, double w, double h, double time) {
        g.setGlobalAlpha(1);
        switch (skin) {
            case PROBE_PAPER -> {
                g.setFill(PAPER_SKY);
                g.fillRect(0, 0, w, h);
                g.setFill(CLOUD);
                g.setGlobalAlpha(0.8);
                for (int cloud = 0; cloud < 7; cloud++) {
                    double x = ((cloud * 0.37 + time * 0.004) % 1) * w, y = (0.1 + 0.8 * fraction(cloud * 3.1)) * h;
                    double size = 10 + 8 * fraction(cloud * 1.7);
                    g.fillOval(x - size, y - size * 0.5, 2 * size, size);
                    g.fillOval(x - size * 0.4, y - size * 0.9, size * 1.2, size * 1.1);
                    g.fillOval(x + size * 0.2, y - size * 0.6, size, size * 0.9);
                }
                g.setGlobalAlpha(1);
            }
            case PROBE_BALLOON -> {
                g.setFill(new LinearGradient(0, 0, 0, h, false, CycleMethod.NO_CYCLE, new Stop(0, DAWN_TOP), new Stop(1, DAWN_BOTTOM)));
                g.fillRect(0, 0, w, h);
                g.setFill(Color.web("#fff0c0"));
                g.setGlobalAlpha(0.35);
                g.fillOval(w * 0.5 - 60, h - 30, 120, 60);
                g.setGlobalAlpha(1);
            }
            case PROBE_BOTTLE -> {
                g.setFill(SEA);
                g.fillRect(0, 0, w, h);
                g.setStroke(WAVE);
                g.setLineWidth(1);
                g.setGlobalAlpha(0.5);
                for (int row = 0; row < 7; row++) {
                    double y = h * (row + 0.5) / 7;
                    for (double x = 0; x < w; x += 24) {
                        double phase = time * 1.2 + row + x * 0.05;
                        g.strokeLine(x, y + 2 * Math.sin(phase), x + 12, y + 2 * Math.sin(phase + 1.5));
                    }
                }
                g.setGlobalAlpha(1);
            }
            default -> {
                g.setFill(new LinearGradient(0, 0, 0, h, false, CycleMethod.NO_CYCLE, new Stop(0, DUSK), new Stop(1, DUSK_LOW)));
                g.fillRect(0, 0, w, h);
                g.setFill(FEATHER);
                for (int star = 0; star < 40; star++) {
                    g.setGlobalAlpha(0.1 + 0.25 * (0.5 + 0.5 * Math.sin(time * 0.5 + star * 1.7)));
                    g.fillOval(fraction(star * 12.9898) * w, fraction(star * 78.233) * h * 0.6, 1.3, 1.3);
                }
                g.setGlobalAlpha(1);
            }
        }
    }

    /**
     * La trajectoire dans ce skin, par ses points, {@code done} étant le nombre de pas déjà parcourus :
     * pointillés de carte au trésor et croix à l'arrivée ; traînée de condensation derrière la sonde ; fil
     * d'Ariane rouge, tendu derrière et lâche devant ; plumes semées derrière.
     */
    static void path(GraphicsContext g, Skin skin, double[] xs, double[] ys, int steps, int done, double time) {
        g.setLineCap(StrokeLineCap.ROUND);
        switch (skin) {
            case PROBE_PAPER -> {
                g.setStroke(MAP_INK);
                g.setLineWidth(2);
                g.setGlobalAlpha(0.85);
                for (int k = 0; k < steps; k += 2) g.strokeLine(xs[k], ys[k], (xs[k] + xs[k + 1]) / 2, (ys[k] + ys[k + 1]) / 2);
                g.setLineWidth(2.5);
                g.strokeLine(xs[steps] - 5, ys[steps] - 5, xs[steps] + 5, ys[steps] + 5);
                g.strokeLine(xs[steps] - 5, ys[steps] + 5, xs[steps] + 5, ys[steps] - 5);
            }
            case PROBE_BALLOON -> {
                // La traînée s'élargit et pâlit en s'éloignant de la sonde.
                g.setStroke(CONTRAIL);
                for (int k = 1; k <= done && k <= steps; k++) {
                    double age = (done - k) / (double) steps;
                    g.setLineWidth(2 + 8 * age);
                    g.setGlobalAlpha(0.6 * (1 - age));
                    g.strokeLine(xs[k - 1], ys[k - 1], xs[k], ys[k]);
                }
            }
            case PROBE_BOTTLE -> {
                g.setStroke(THREAD);
                g.setLineWidth(1.2);
                g.setGlobalAlpha(0.9);
                for (int k = 1; k <= steps; k++) {
                    // Tendu derrière la bouteille, le fil ondule à peine ; devant, il n'est pas encore déroulé : à peine visible.
                    boolean behind = k <= done;
                    double wave = behind ? 1.5 * Math.sin(k * 1.3 + time * 2) : 0;
                    g.setGlobalAlpha(behind ? 0.9 : 0.15);
                    g.strokeLine(xs[k - 1], ys[k - 1] + wave, xs[k], ys[k] + wave);
                }
            }
            default -> {
                g.setFill(FEATHER);
                g.setStroke(FEATHER);
                g.setLineWidth(0.8);
                for (int k = 2; k <= done && k <= steps; k += 3) {
                    // Une plume : une petite ellipse penchée, qui descend lentement, et sa hampe.
                    double fall = 6 * ((time * 0.5 + k * 0.37) % 1);
                    double x = xs[k] + 3 * Math.sin(k), y = ys[k] + 6 + fall;
                    g.setGlobalAlpha(0.7 - fall * 0.08);
                    g.fillOval(x - 2, y - 4, 4, 8);
                    g.strokeLine(x, y - 5, x, y + 5);
                }
            }
        }
        g.setGlobalAlpha(1);
        g.setLineCap(StrokeLineCap.SQUARE);
    }

    /** La sonde dans ce skin, centrée en ({@code x}, {@code y}), pointée selon {@code angle} ; {@code time} fait battre les ailes. */
    static void craft(GraphicsContext g, Skin skin, double x, double y, double angle, double time) {
        double c = Math.cos(angle), s = Math.sin(angle);
        switch (skin) {
            case PROBE_PAPER -> {
                // Un avion plié : la pointe devant, deux ailes en arrière, un pli au milieu.
                double[] px = {x + 12 * c, x - 8 * c - 7 * s, x - 5 * c, x - 8 * c + 7 * s};
                double[] py = {y + 12 * s, y - 8 * s + 7 * c, y - 5 * s, y - 8 * s - 7 * c};
                g.setFill(PLANE);
                g.fillPolygon(px, py, 4);
                g.setStroke(CREASE);
                g.setLineWidth(1);
                g.strokePolygon(px, py, 4);
                g.strokeLine(x + 12 * c, y + 12 * s, x - 5 * c, y - 5 * s);
            }
            case PROBE_BALLOON -> {
                // L'enveloppe rayée, droite quel que soit le cap, et la nacelle pendue à ses cordes.
                g.setFill(BALLOON_RED);
                g.fillOval(x - 11, y - 20, 22, 24);
                g.setFill(BALLOON_YELLOW);
                g.fillOval(x - 4, y - 20, 8, 24);
                g.setStroke(BASKET);
                g.setLineWidth(1);
                g.strokeLine(x - 6, y + 2, x - 4, y + 10);
                g.strokeLine(x + 6, y + 2, x + 4, y + 10);
                g.setFill(BASKET);
                g.fillRect(x - 5, y + 10, 10, 6);
            }
            case PROBE_BOTTLE -> {
                // Une bouteille penchée dans le sens de la route : le corps, le col, le bouchon, et le message roulé dedans.
                g.setFill(GLASS);
                g.setGlobalAlpha(0.75);
                double[] bx = {x - 10 * c - 5 * s, x + 4 * c - 5 * s, x + 4 * c + 5 * s, x - 10 * c + 5 * s};
                double[] by = {y - 10 * s + 5 * c, y + 4 * s + 5 * c, y + 4 * s - 5 * c, y - 10 * s - 5 * c};
                g.fillPolygon(bx, by, 4);
                g.fillRect(Math.min(x + 4 * c - 2.5 * s, x + 10 * c + 2.5 * s) - 2, Math.min(y + 4 * s + 2.5 * c, y + 10 * s - 2.5 * c) - 2, 6, 6);
                g.setGlobalAlpha(1);
                g.setFill(CORK);
                g.fillOval(x + 10 * c - 3, y + 10 * s - 3, 6, 6);
                g.setFill(MESSAGE);
                g.fillOval(x - 3 * c - 2, y - 3 * s - 2.5, 7, 5);
            }
            default -> {
                // Un pigeon : le corps gris, la tête au cou vert, deux ailes qui battent.
                double flap = Math.sin(time * 12) * 6;
                g.setFill(PIGEON_DARK);
                g.fillOval(x - 8 * c - 3 - flap * s * 0.3, y - 8 * s - 3 + flap * c * 0.3, 10, 6);
                g.setStroke(PIGEON_DARK);
                g.setLineWidth(2.5);
                g.setLineCap(StrokeLineCap.ROUND);
                g.strokeLine(x - 2 * c, y - 2 * s, x - 2 * c - 9 * s, y - 2 * s + 9 * c - flap);
                g.strokeLine(x - 2 * c, y - 2 * s, x - 2 * c + 9 * s, y - 2 * s - 9 * c + flap);
                g.setLineCap(StrokeLineCap.SQUARE);
                g.setFill(PIGEON);
                g.fillOval(x - 7, y - 4, 14, 8);
                g.setFill(PIGEON_NECK);
                g.fillOval(x + 6 * c - 3, y + 6 * s - 3, 6, 6);
                g.setFill(BALLOON_YELLOW);
                g.fillPolygon(new double[] {x + 9 * c, x + 12 * c - 1.5 * s, x + 12 * c + 1.5 * s}, new double[] {y + 9 * s, y + 12 * s + 1.5 * c, y + 12 * s - 1.5 * c}, 3);
            }
        }
    }

    /** Le fond du dos de la carte de butin : papier plié, toile de ballon, verre, plumage. */
    static String cardBack(Skin skin) {
        return switch (skin) {
            case PROBE_PAPER -> "#e8eef4";
            case PROBE_BALLOON -> "#6a2a22";
            case PROBE_BOTTLE -> "#10404c";
            default -> "#3a3448";
        };
    }

    /** La marque sur le dos de la carte : un avion, un ballon, une bouteille, une plume. */
    static String cardMark(Skin skin) {
        return switch (skin) {
            case PROBE_PAPER -> "✈";
            case PROBE_BALLOON -> "◍";
            case PROBE_BOTTLE -> "⌾";
            default -> "❧";
        };
    }

    private static double fraction(double seed) {
        double value = Math.sin(seed) * 43758.5453;
        return value - Math.floor(value);
    }
}
