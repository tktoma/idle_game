package idle.ui.vacuum;

import idle.core.universe.Skin;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.ArcType;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

/**
 * Les skins du vide (version 0.195), dessinés à côté de {@link VoidView} pour la garder lisible.
 * <ul>
 * <li>L'écume : « Bulles de savon » (des bulles irisées qui montent lentement sur un fond nuit), « Mousse de bière »
 * (un fond ambré, des bulles qui montent, la mousse blanche en haut), « Pellicule » (un sépia granuleux, des rayures
 * verticales et des taches qui crépitent), « Neige de télévision » (un gris qui grésille, des lignes de balayage) ;</li>
 * <li>les paires : « Jumeaux » (deux petits bonshommes qui se tiennent la main et s'éloignent), « Aimants » (deux barres
 * rouge et bleue, N et S, que leur champ relie), « Yin et yang » (le disque noir et blanc qui tourne, coupé en deux
 * moitiés qui s'écartent) ;</li>
 * <li>le trou noir : « Bouche d'égout » (une plaque de fonte à grille), « Puits de pierre » (une margelle de pierres et
 * le noir au fond), « Iris » (un œil, l'iris coloré et strié, la pupille noire), « Nœud de bois » (des cernes bruns
 * autour d'un nœud sombre), « Tourbillon de thé » (une tasse vue de haut, le thé qui tourne vers son centre).</li>
 * </ul>
 */
final class VoidSkinArt {

    private static final Color NIGHT = Color.web("#0a1020");
    private static final Color BEER = Color.web("#c47a12");
    private static final Color BEER_DEEP = Color.web("#7a4a08");
    private static final Color FOAM = Color.web("#fff4dc");
    private static final Color SEPIA = Color.web("#3a3024");
    private static final Color SEPIA_LIGHT = Color.web("#6a5a44");
    private static final Color TV = Color.web("#5a5a5a");
    private static final Color IRON = Color.web("#3c3f44");
    private static final Color IRON_LIGHT = Color.web("#6a6e74");
    private static final Color STONE = Color.web("#8a8578");
    private static final Color STONE_DARK = Color.web("#4a4740");
    private static final Color IRIS = Color.web("#3a8ad8");
    private static final Color IRIS_DARK = Color.web("#1a4a88");
    private static final Color WOOD = Color.web("#a0683a");
    private static final Color WOOD_DARK = Color.web("#5a3418");
    private static final Color TEA = Color.web("#b86a20");
    private static final Color TEA_DEEP = Color.web("#6a3a08");
    private static final Color CUP = Color.web("#f0ece4");
    private static final Color MAGNET_N = Color.web("#e04a3a");
    private static final Color MAGNET_S = Color.web("#3a6ae0");
    private static final Font POLE = Font.font("System", FontWeight.BOLD, 7);

    private VoidSkinArt() {
    }

    static boolean drawsFoam(Skin foam) {
        return foam == Skin.FOAM_SOAP || foam == Skin.FOAM_BEER || foam == Skin.FOAM_FILM || foam == Skin.FOAM_TV;
    }

    static boolean drawsPairs(Skin pairs) {
        return pairs == Skin.PAIRS_TWINS || pairs == Skin.PAIRS_MAGNETS || pairs == Skin.PAIRS_YINYANG;
    }

    static boolean drawsHole(Skin hole) {
        return hole == Skin.VOID_HOLE_MANHOLE || hole == Skin.VOID_HOLE_WELL || hole == Skin.VOID_HOLE_IRIS || hole == Skin.VOID_HOLE_KNOT
                || hole == Skin.VOID_HOLE_TEA;
    }

    /** Un nombre entre 0 et 1, toujours le même pour une même graine. */
    private static double random(long seed) {
        double value = Math.sin(seed * 12.9898 + 78.233) * 43758.5453;
        return value - Math.floor(value);
    }

    // ------------------------------------------------------------------
    // L'écume : le fond et le champ
    // ------------------------------------------------------------------

    /** Le fond et le champ sous ce skin de l'écume, à la place du fond nuit, des halos et de la trame. */
    static void foam(GraphicsContext g, Skin foam, double w, double h, double time) {
        switch (foam) {
            case FOAM_SOAP -> soap(g, w, h, time);
            case FOAM_BEER -> beer(g, w, h, time);
            case FOAM_FILM -> film(g, w, h, time);
            default -> tv(g, w, h, time);
        }
    }

    /** Les bulles de savon : de grandes bulles translucides, irisées de rose et de vert, qui montent en dérivant. */
    private static void soap(GraphicsContext g, double w, double h, double time) {
        g.setFill(NIGHT);
        g.fillRect(0, 0, w, h);
        for (int i = 0; i < 18; i++) {
            double r = 12 + 28 * random(i * 3);
            double x = w * ((random(i * 3 + 1) + 0.04 * Math.sin(time * 0.3 + i)) % 1);
            double y = h * ((random(i * 3 + 2) - time * 0.015 * (1 + i % 3)) % 1 + 1) % h;
            g.setStroke(Color.color(1, 1, 1, 0.35));
            g.setLineWidth(1.2);
            g.strokeOval(x - r, y - r, r * 2, r * 2);
            g.setStroke(Color.color(1, 0.6, 0.8, 0.3));
            g.strokeArc(x - r, y - r, r * 2, r * 2, 20 + time * 20, 120, ArcType.OPEN);
            g.setStroke(Color.color(0.5, 1, 0.7, 0.3));
            g.strokeArc(x - r, y - r, r * 2, r * 2, 200 + time * 20, 100, ArcType.OPEN);
            g.setFill(Color.color(1, 1, 1, 0.6));
            g.fillOval(x - r * 0.5, y - r * 0.6, r * 0.3, r * 0.18);
        }
    }

    /** La mousse de bière : un verre ambré vu de face, des bulles fines qui montent, la mousse crémeuse sur le haut. */
    private static void beer(GraphicsContext g, double w, double h, double time) {
        g.setFill(new LinearGradient(0, 0, 0, h, false, CycleMethod.NO_CYCLE, new Stop(0, BEER), new Stop(1, BEER_DEEP)));
        g.fillRect(0, 0, w, h);
        g.setFill(Color.color(1, 0.95, 0.8, 0.55));
        for (int i = 0; i < 70; i++) {
            double x = w * random(i * 2), speed = 0.04 + 0.08 * random(i * 2 + 1);
            double y = h * (((random(i * 7) - time * speed) % 1 + 1) % 1);
            double r = 1 + 2 * random(i * 5);
            g.fillOval(x - r, y - r, r * 2, r * 2);
        }
        double foamH = h * 0.14;
        g.setFill(FOAM);
        g.fillRect(0, 0, w, foamH);
        for (double x = 0; x < w + 20; x += 22) g.fillOval(x - 12, foamH - 10 + 4 * Math.sin(x * 0.1 + time * 0.5), 24, 20);
        g.setFill(Color.color(0.9, 0.85, 0.7, 0.5));
        for (int i = 0; i < 30; i++) g.fillOval(w * random(i * 11) - 2, foamH * random(i * 11 + 1) - 2, 4, 4);
    }

    /** La pellicule : un sépia granuleux, des rayures verticales qui sautent, des taches blanches qui crépitent, le bord vignetté. */
    private static void film(GraphicsContext g, double w, double h, double time) {
        g.setFill(SEPIA);
        g.fillRect(0, 0, w, h);
        long frame = (long) Math.floor(time * 12);
        g.setFill(Color.color(0.9, 0.85, 0.7, 0.08));
        for (int i = 0; i < 160; i++) g.fillRect(w * random(frame * 7 + i * 3), h * random(frame * 7 + i * 3 + 1), 2, 2);
        g.setStroke(Color.color(SEPIA_LIGHT.getRed(), SEPIA_LIGHT.getGreen(), SEPIA_LIGHT.getBlue(), 0.6));
        g.setLineWidth(1);
        for (int i = 0; i < 3; i++) {
            double x = w * random(frame / 3 + i * 17);
            g.strokeLine(x, 0, x, h);
        }
        g.setFill(Color.color(1, 1, 0.9, 0.5));
        for (int i = 0; i < 4; i++) {
            if (random(frame * 13 + i) < 0.4) g.fillOval(w * random(frame * 5 + i * 2) - 2, h * random(frame * 5 + i * 2 + 1) - 2, 4, 4);
        }
        g.setFill(new RadialGradient(0, 0, w / 2, h / 2, Math.max(w, h) * 0.7, false, CycleMethod.NO_CYCLE,
                new Stop(0.6, Color.TRANSPARENT), new Stop(1, Color.color(0, 0, 0, 0.7))));
        g.fillRect(0, 0, w, h);
    }

    /** La neige de télévision : des carreaux gris qui changent trente fois par seconde, et les lignes de balayage. */
    private static void tv(GraphicsContext g, double w, double h, double time) {
        g.setFill(TV);
        g.fillRect(0, 0, w, h);
        long frame = (long) Math.floor(time * 30);
        int cell = 6;
        for (double y = 0; y < h; y += cell) {
            for (double x = 0; x < w; x += cell) {
                double v = random(frame * 1_000_003 + (long) (x / cell) * 977 + (long) (y / cell));
                g.setFill(Color.gray(0.15 + 0.7 * v));
                g.fillRect(x, y, cell, cell);
            }
        }
        g.setFill(Color.color(0, 0, 0, 0.35));
        for (double y = 0; y < h; y += 4) g.fillRect(0, y, w, 2);
        double band = ((time * 0.2) % 1) * h;
        g.setFill(Color.color(1, 1, 1, 0.08));
        g.fillRect(0, band, w, 14);
    }

    /** La couleur des traits et des textes sur ce fond : sombre sur la bière et la neige, claire ailleurs. */
    static Color pairLine(Skin foam) {
        return foam == Skin.FOAM_BEER || foam == Skin.FOAM_TV ? Color.color(0.1, 0.05, 0, 0.5) : Color.color(1, 1, 1, 0.3);
    }

    // ------------------------------------------------------------------
    // Les paires
    // ------------------------------------------------------------------

    /**
     * Une paire sous ce skin : la matière en ({@code x1}, {@code y1}), l'antimatière en ({@code x2}, {@code y2}), de force
     * {@code alpha} ; les deux se confondent à la naissance et à la fin.
     */
    static void pair(GraphicsContext g, Skin pairs, double x1, double y1, double x2, double y2, double alpha, double time) {
        switch (pairs) {
            case PAIRS_TWINS -> {
                // Les mains se tiennent : un trait entre les deux bonshommes, l'un bleu-vert, l'autre mauve.
                g.setStroke(Color.color(1, 1, 1, 0.5 * alpha));
                g.setLineWidth(1);
                g.strokeLine(x1, y1 + 2, x2, y2 + 2);
                twin(g, x1, y1, Color.web("#7fe3d0"), alpha);
                twin(g, x2, y2, Color.web("#d9a6ff"), alpha);
            }
            case PAIRS_MAGNETS -> {
                // Le champ : deux arcs entre les pôles qui se font face.
                g.setStroke(Color.color(1, 1, 1, 0.25 * alpha));
                g.setLineWidth(0.8);
                double mx = (x1 + x2) / 2, my = (y1 + y2) / 2, d = Math.hypot(x2 - x1, y2 - y1) / 2 + 2;
                g.strokeOval(mx - d, my - d * 0.5, d * 2, d);
                g.strokeOval(mx - d, my - d * 0.8, d * 2, d * 1.6);
                magnet(g, x1, y1, alpha);
                magnet(g, x2, y2, alpha);
            }
            default -> {
                // Le disque tourne ; ses deux moitiés, la noire et la blanche, s'écartent avec la paire, chacune avec son œil.
                double r = 5, angle = Math.toDegrees(Math.atan2(y1 - y2, x2 - x1)) + time * 90;
                for (int half = 0; half < 2; half++) {
                    double sx = half == 0 ? x1 : x2, sy = half == 0 ? y1 : y2;
                    Color body = half == 0 ? Color.color(0.05, 0.05, 0.08, alpha) : Color.color(1, 1, 1, alpha);
                    Color eye = half == 0 ? Color.color(1, 1, 1, alpha) : Color.color(0.05, 0.05, 0.08, alpha);
                    g.setFill(body);
                    g.fillArc(sx - r, sy - r, r * 2, r * 2, angle + half * 180, 180, ArcType.ROUND);
                    double ea = Math.toRadians(angle + half * 180 + 90);
                    g.setFill(eye);
                    g.fillOval(sx + Math.cos(ea) * r / 2 - 1.2, sy - Math.sin(ea) * r / 2 - 1.2, 2.4, 2.4);
                    g.setStroke(Color.color(1, 1, 1, 0.4 * alpha));
                    g.setLineWidth(0.8);
                    g.strokeArc(sx - r, sy - r, r * 2, r * 2, angle + half * 180, 180, ArcType.OPEN);
                }
            }
        }
    }

    /** Un petit bonhomme : la tête ronde, le corps en trait, les jambes. */
    private static void twin(GraphicsContext g, double x, double y, Color color, double alpha) {
        g.setFill(Color.color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
        g.fillOval(x - 2.2, y - 6.5, 4.4, 4.4);
        g.setStroke(Color.color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
        g.setLineWidth(1.4);
        g.strokeLine(x, y - 2, x, y + 3);
        g.strokeLine(x, y + 3, x - 2, y + 7);
        g.strokeLine(x, y + 3, x + 2, y + 7);
    }

    /** Un aimant droit : une barre rouge et bleue, N et S écrits dessus. */
    private static void magnet(GraphicsContext g, double x, double y, double alpha) {
        g.setFill(Color.color(MAGNET_N.getRed(), MAGNET_N.getGreen(), MAGNET_N.getBlue(), alpha));
        g.fillRect(x - 5, y - 3, 5, 6);
        g.setFill(Color.color(MAGNET_S.getRed(), MAGNET_S.getGreen(), MAGNET_S.getBlue(), alpha));
        g.fillRect(x, y - 3, 5, 6);
        g.setFill(Color.color(1, 1, 1, alpha));
        g.setFont(POLE);
        g.setTextAlign(TextAlignment.CENTER);
        g.fillText("N", x - 2.5, y + 2.5);
        g.fillText("S", x + 2.5, y + 2.5);
        g.setTextAlign(TextAlignment.LEFT);
    }

    // ------------------------------------------------------------------
    // Le trou noir
    // ------------------------------------------------------------------

    /** Le trou noir du vide sous ce skin, au centre, de rayon {@code r} (qui grandit avec la masse). */
    static void hole(GraphicsContext g, Skin hole, double cx, double cy, double r, double time) {
        switch (hole) {
            case VOID_HOLE_MANHOLE -> manhole(g, cx, cy, r);
            case VOID_HOLE_WELL -> well(g, cx, cy, r);
            case VOID_HOLE_IRIS -> iris(g, cx, cy, r, time);
            case VOID_HOLE_KNOT -> knot(g, cx, cy, r);
            default -> tea(g, cx, cy, r, time);
        }
    }

    /** La bouche d'égout : une plaque de fonte ronde, cerclée, barrée d'une grille dont les fentes laissent voir le noir. */
    private static void manhole(GraphicsContext g, double cx, double cy, double r) {
        double R = r * 1.6;
        g.setFill(IRON);
        g.fillOval(cx - R, cy - R, R * 2, R * 2);
        g.setStroke(IRON_LIGHT);
        g.setLineWidth(3);
        g.strokeOval(cx - R, cy - R, R * 2, R * 2);
        g.strokeOval(cx - R * 0.75, cy - R * 0.75, R * 1.5, R * 1.5);
        g.setFill(Color.BLACK);
        for (int k = -2; k <= 2; k++) {
            double y = cy + k * R * 0.28, half = Math.sqrt(Math.max(0, R * R * 0.5 - y * y + 2 * y * cy - cy * cy));
            g.fillRect(cx - half, y - R * 0.07, half * 2, R * 0.14);
        }
    }

    /** Le puits de pierre : une margelle de pierres grises autour d'un noir profond, une corde qui descend. */
    private static void well(GraphicsContext g, double cx, double cy, double r) {
        double R = r * 1.5;
        g.setFill(Color.BLACK);
        g.fillOval(cx - R, cy - R, R * 2, R * 2);
        int stones = 12;
        for (int k = 0; k < stones; k++) {
            double a0 = Math.PI * 2 * k / stones, a1 = Math.PI * 2 * (k + 0.9) / stones;
            double x0 = cx + Math.cos(a0) * R, y0 = cy + Math.sin(a0) * R, x1 = cx + Math.cos(a1) * R, y1 = cy + Math.sin(a1) * R;
            double x2 = cx + Math.cos(a1) * R * 1.4, y2 = cy + Math.sin(a1) * R * 1.4, x3 = cx + Math.cos(a0) * R * 1.4, y3 = cy + Math.sin(a0) * R * 1.4;
            g.setFill(k % 2 == 0 ? STONE : STONE_DARK);
            g.fillPolygon(new double[] {x0, x1, x2, x3}, new double[] {y0, y1, y2, y3}, 4);
        }
        g.setStroke(Color.web("#c8a860"));
        g.setLineWidth(1.5);
        g.strokeLine(cx + R * 0.3, cy - R * 1.4, cx + R * 0.25, cy + R * 0.2);
    }

    /** L'iris : l'œil, blanc en amande, l'iris bleu strié qui bat lentement, la pupille noire au centre. */
    private static void iris(GraphicsContext g, double cx, double cy, double r, double time) {
        double R = r * 1.6, pupil = r * (0.7 + 0.1 * Math.sin(time * 0.8));
        g.setFill(Color.web("#f4f4f8"));
        g.fillOval(cx - R * 2.2, cy - R * 1.1, R * 4.4, R * 2.2);
        g.setFill(new RadialGradient(0, 0, cx, cy, R, false, CycleMethod.NO_CYCLE, new Stop(0, IRIS), new Stop(1, IRIS_DARK)));
        g.fillOval(cx - R, cy - R, R * 2, R * 2);
        g.setStroke(Color.color(1, 1, 1, 0.3));
        g.setLineWidth(1);
        for (int k = 0; k < 24; k++) {
            double a = Math.PI * 2 * k / 24 + time * 0.1;
            g.strokeLine(cx + Math.cos(a) * pupil, cy + Math.sin(a) * pupil, cx + Math.cos(a) * R * 0.95, cy + Math.sin(a) * R * 0.95);
        }
        g.setFill(Color.BLACK);
        g.fillOval(cx - pupil, cy - pupil, pupil * 2, pupil * 2);
        g.setFill(Color.color(1, 1, 1, 0.8));
        g.fillOval(cx - R * 0.5, cy - R * 0.6, R * 0.3, R * 0.2);
    }

    /** Le nœud de bois : des cernes bruns, ovales et serrés, autour d'un nœud presque noir. */
    private static void knot(GraphicsContext g, double cx, double cy, double r) {
        double R = r * 2.2;
        g.setFill(WOOD);
        g.fillOval(cx - R * 1.6, cy - R * 1.1, R * 3.2, R * 2.2);
        g.setStroke(WOOD_DARK);
        for (int k = 1; k <= 6; k++) {
            double rx = R * 1.5 * k / 6, ry = R * k / 6;
            g.setLineWidth(k % 2 == 0 ? 1.6 : 0.8);
            g.strokeOval(cx - rx + 2 * Math.sin(k), cy - ry, rx * 2, ry * 2);
        }
        g.setFill(Color.web("#1a0e06"));
        g.fillOval(cx - r, cy - r * 0.8, r * 2, r * 1.6);
    }

    /** Le tourbillon de thé : une tasse blanche vue de haut, le thé ambré qui tourne en spirale vers le centre sombre. */
    private static void tea(GraphicsContext g, double cx, double cy, double r, double time) {
        double R = r * 2.4;
        g.setFill(CUP);
        g.fillOval(cx - R * 1.25, cy - R * 1.25, R * 2.5, R * 2.5);
        g.setFill(CUP);
        g.fillOval(cx + R * 1.1, cy - R * 0.5, R * 0.9, R);
        g.setFill(new RadialGradient(0, 0, cx, cy, R, false, CycleMethod.NO_CYCLE, new Stop(0, TEA_DEEP), new Stop(1, TEA)));
        g.fillOval(cx - R, cy - R, R * 2, R * 2);
        g.setStroke(Color.color(1, 0.9, 0.7, 0.5));
        g.setLineWidth(1.2);
        int points = 40;
        double[] xs = new double[points], ys = new double[points];
        for (int arm = 0; arm < 2; arm++) {
            for (int i = 0; i < points; i++) {
                double t = i / (double) (points - 1), a = t * Math.PI * 3 + time * 2 + arm * Math.PI, rad = R * 0.95 * (1 - t) + r * 0.3;
                xs[i] = cx + Math.cos(a) * rad;
                ys[i] = cy + Math.sin(a) * rad;
            }
            g.strokePolyline(xs, ys, points);
        }
        g.setFill(Color.web("#2a1404"));
        g.fillOval(cx - r * 0.5, cy - r * 0.5, r, r);
    }
}
