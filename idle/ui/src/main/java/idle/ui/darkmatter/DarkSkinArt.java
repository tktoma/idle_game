package idle.ui.darkmatter;

import idle.core.universe.Skin;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;

/**
 * Les skins de la matière noire de la version 0.191, dessinés à côté de {@link DarkMatterView} pour la
 * garder lisible : quatre boules et deux animations (version 0.203 : l'œuf, la pluie et le feu d'artifice retirés).
 * <ul>
 * <li>« Goutte de mercure » : une sphère miroir gris argent au reflet déformé, qui tremble à chaque appui ;</li>
 * <li>« Trou noir » : un disque noir net, un anneau de lumière orange plus brillant en haut, l'espace
 * étiré autour ;</li>
 * <li>« Nœud » : une corde violette nouée en trèfle, qui se resserre à l'appui ;</li>
 * <li>« Lanterne » : du papier translucide à nervures, et dedans une flamme qui grandit avec la matière ;</li>
 * <li>« Neige » : des flocons lents qui tourbillonnent, attirés vers la boule quand elle grossit ;</li>
 * <li>« Sablier » : un sablier en filigrane dont les grains coulent par le goulot vers la boule.</li>
 * </ul>
 */
final class DarkSkinArt {

    private static final Color ROPE = Color.web("#8a4cff");
    private static final Color ROPE_DARK = Color.web("#3d1a7a");
    private static final Color PAPER = Color.web("#f6d9a0");
    private static final Color RIB = Color.web("#8a5a2a");
    private static final Color FLAME = Color.web("#ffb347");
    private static final Color FLAKE = Color.web("#eef6ff");
    private static final Color SAND = Color.web("#e8c87a");
    private static final Color GLASS = Color.web("#8f7fb8");

    private DarkSkinArt() {
    }

    /** Vrai pour les boules dessinées ici : celles de la version 0.191. */
    static boolean draws(Skin skin) {
        return skin == Skin.DARK_MERCURY || skin == Skin.DARK_HOLE
                || skin == Skin.DARK_KNOT || skin == Skin.DARK_LANTERN;
    }

    /** Vrai pour les animations dessinées ici : celles de la version 0.191. */
    static boolean animates(Skin skin) {
        return skin == Skin.DARK_SNOW || skin == Skin.DARK_HOURGLASS;
    }

    /** Un nombre entre 0 et 1, toujours le même pour une même graine. */
    private static double fraction(double seed) {
        double value = Math.sin(seed * 12.9898 + 78.233) * 43758.5453;
        return value - Math.floor(value);
    }

    private static Color tint(Color c, double alpha) {
        return Color.color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(1, alpha)));
    }

    private static void glow(GraphicsContext g, double x, double y, double size, Color color, double strength) {
        if (!(size > 0)) return;
        g.setGlobalAlpha(1);
        g.setFill(new RadialGradient(0, 0, x, y, size, false, CycleMethod.NO_CYCLE,
                new Stop(0, tint(color, strength)), new Stop(1, tint(color, 0))));
        g.fillOval(x - size, y - size, 2 * size, 2 * size);
    }

    // ------------------------------------------------------------------
    // Les boules
    // ------------------------------------------------------------------

    /**
     * Dessine la boule sous ce skin.
     *
     * @param r       le rayon de la boule à l'écran, 3 au moins
     * @param growing vrai tant que le joueur maintient le point appuyé
     */
    static void draw(Skin skin, GraphicsContext g, DarkMatterView view, double cx, double cy, double r, boolean growing) {
        switch (skin) {
            case DARK_MERCURY -> mercury(g, view, cx, cy, r);
            case DARK_HOLE -> hole(g, view, cx, cy, r, growing);
            case DARK_KNOT -> knot(g, view, cx, cy, r);
            default -> lantern(g, view, cx, cy, r, growing);
        }
        g.setGlobalAlpha(1);
    }

    /** La goutte de mercure : un miroir gris argent qui tremble et se déforme à chaque appui. */
    private static void mercury(GraphicsContext g, DarkMatterView view, double cx, double cy, double r) {
        double time = view.time(), press = view.press();
        double shake = press * Math.sin(time * 38);
        double x = cx + shake * r * 0.08, y = cy + press * Math.cos(time * 29) * r * 0.06;
        double rx = r * (1 + 0.12 * shake), ry = r * (1 - 0.12 * shake);
        // L'ombre portée et le corps, un dégradé décentré vers le reflet de la lumière.
        g.setGlobalAlpha(0.35);
        g.setFill(Color.BLACK);
        g.fillOval(x - rx * 0.95, y - ry * 0.75 + ry * 0.3, rx * 1.9, ry * 1.5);
        g.setGlobalAlpha(1);
        g.setFill(new RadialGradient(0, 0, x - rx * 0.35, y - ry * 0.4, r * 1.25, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#ffffff")), new Stop(0.25, Color.web("#d8dce4")), new Stop(0.7, Color.web("#7a8290")),
                new Stop(1, Color.web("#2a2f38"))));
        g.fillOval(x - rx, y - ry, 2 * rx, 2 * ry);
        // Le reflet déformé de la pièce : des arcs sombres et clairs qui ondulent sur la sphère.
        g.setLineWidth(Math.max(1, r * 0.06));
        for (int k = 0; k < 5; k++) {
            double level = -0.55 + 0.28 * k + 0.06 * Math.sin(time * 1.3 + k * 2);
            double half = Math.sqrt(Math.max(0, 1 - level * level));
            int steps = 16;
            double[] xs = new double[steps], ys = new double[steps];
            for (int i = 0; i < steps; i++) {
                double u = -half + 2 * half * i / (steps - 1);
                xs[i] = x + u * rx * 0.92;
                ys[i] = y + (level + 0.08 * Math.sin(u * 6 + time * 2 + k) * (1 + 3 * press)) * ry * 0.92;
            }
            g.setStroke(k % 2 == 0 ? Color.web("#1b1f27") : Color.web("#f4f6fa"));
            g.setGlobalAlpha(k % 2 == 0 ? 0.35 : 0.25);
            g.strokePolyline(xs, ys, steps);
        }
        // Le point de lumière, net.
        g.setGlobalAlpha(0.95);
        g.setFill(Color.WHITE);
        g.fillOval(x - rx * 0.55, y - ry * 0.62, rx * 0.3, ry * 0.18);
    }

    /** Le trou noir : un disque noir, l'anneau de photons lentillé, et l'espace étiré tout autour. */
    private static void hole(GraphicsContext g, DarkMatterView view, double cx, double cy, double r, boolean growing) {
        double time = view.time();
        // L'étirement : des arcs de plus en plus écrasés, qui glissent vers le disque.
        g.setStroke(Color.web("#ffb060"));
        for (int k = 0; k < 6; k++) {
            double phase = (time * 0.25 + k / 6.0) % 1;
            double rr = r * (1.25 + 1.2 * (1 - phase));
            g.setLineWidth(1);
            g.setGlobalAlpha(0.08 + 0.25 * phase);
            g.strokeOval(cx - rr, cy - rr * (0.25 + 0.45 * (1 - phase)), 2 * rr, 2 * rr * (0.25 + 0.45 * (1 - phase)));
        }
        // Le disque d'accrétion, vu de côté, derrière le disque noir.
        g.setGlobalAlpha(1);
        g.setFill(new RadialGradient(0, 0, cx, cy, r * 1.9, false, CycleMethod.NO_CYCLE,
                new Stop(0, tint(Color.web("#ffd27f"), 0.9)), new Stop(0.55, tint(Color.web("#ff7a1a"), 0.55)), new Stop(1, tint(Color.web("#ff4a00"), 0))));
        g.fillOval(cx - r * 1.9, cy - r * 0.32, r * 3.8, r * 0.64);
        // L'anneau de lumière : plus brillant et plus épais en haut, comme courbé par la lentille.
        int steps = 72;
        for (int k = 0; k < steps; k++) {
            double a0 = 2 * Math.PI * k / steps, a1 = 2 * Math.PI * (k + 1) / steps;
            double up = 0.5 - 0.5 * Math.sin((a0 + a1) / 2);          // 1 en haut, 0 en bas
            double rr = r * (1.04 + 0.1 * up);
            g.setStroke(up > 0.6 ? Color.web("#fff1c0") : Color.web("#ff9a3c"));
            g.setLineWidth(Math.max(1.2, r * (0.05 + 0.12 * up)) * (growing ? 1.2 : 1));
            g.setGlobalAlpha(0.35 + 0.65 * up);
            g.strokeLine(cx + rr * Math.cos(a0), cy + rr * Math.sin(a0), cx + rr * Math.cos(a1), cy + rr * Math.sin(a1));
        }
        g.setGlobalAlpha(1);
        g.setFill(Color.BLACK);
        g.fillOval(cx - r, cy - r, 2 * r, 2 * r);
    }

    /** Le nœud : une corde violette en trèfle, trois boucles, qui se resserre tant qu'on appuie. */
    private static void knot(GraphicsContext g, DarkMatterView view, double cx, double cy, double r) {
        double time = view.time(), hold = view.hold();
        double scale = r * 0.34 * (1 - 0.18 * hold - 0.06 * view.press());
        double turn = time * 0.2;
        int steps = 120;
        double[] xs = new double[steps + 1], ys = new double[steps + 1];
        for (int k = 0; k <= steps; k++) {
            double t = 2 * Math.PI * k / steps;
            double x = Math.sin(t) + 2 * Math.sin(2 * t), y = Math.cos(t) - 2 * Math.cos(2 * t);
            xs[k] = cx + scale * (x * Math.cos(turn) - y * Math.sin(turn));
            ys[k] = cy + scale * (x * Math.sin(turn) + y * Math.cos(turn));
        }
        glow(g, cx, cy, r * 1.3, ROPE, 0.2 + 0.25 * hold);
        double rope = Math.max(2, r * (0.16 + 0.05 * hold));
        g.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        g.setGlobalAlpha(1);
        g.setStroke(ROPE_DARK);
        g.setLineWidth(rope * 1.35);
        g.strokePolyline(xs, ys, steps + 1);
        g.setStroke(ROPE);
        g.setLineWidth(rope);
        g.strokePolyline(xs, ys, steps + 1);
        // Les torons : des traits obliques le long de la corde, qui glissent quand elle se resserre.
        g.setStroke(Color.web("#c9a6ff"));
        g.setLineWidth(Math.max(0.8, rope * 0.18));
        g.setGlobalAlpha(0.7);
        for (int k = 0; k < steps; k += 3) {
            double dx = xs[k + 1] - xs[k], dy = ys[k + 1] - ys[k];
            double len = Math.max(1e-9, Math.hypot(dx, dy));
            double nx = -dy / len * rope * 0.4, ny = dx / len * rope * 0.4;
            double slide = (time * 2 * (1 + 2 * hold)) % 3;
            int i = (int) Math.min(steps, k + slide);
            g.strokeLine(xs[i] - nx + dx * 0.4, ys[i] - ny + dy * 0.4, xs[i] + nx - dx * 0.4, ys[i] + ny - dy * 0.4);
        }
        g.setLineCap(javafx.scene.shape.StrokeLineCap.SQUARE);
        // Un reflet par-dessus.
        g.setStroke(Color.WHITE);
        g.setLineWidth(Math.max(0.6, rope * 0.15));
        g.setGlobalAlpha(0.35);
        g.strokePolyline(xs, ys, steps + 1);
    }

    /** La lanterne : du papier qui laisse passer la lumière, des nervures, et une flamme qui grandit avec la matière. */
    private static void lantern(GraphicsContext g, DarkMatterView view, double cx, double cy, double r, boolean growing) {
        double time = view.time(), growth = view.growth();
        double flicker = 0.85 + 0.15 * Math.sin(time * 9) * Math.sin(time * 5.3);
        double flame = r * (0.2 + 0.6 * growth) * flicker;
        double rx = r * 0.92, ry = r;
        glow(g, cx, cy, r * (1.3 + growth), FLAME, 0.25 + 0.4 * growth * flicker);
        // Le papier, chauffé de l'intérieur.
        g.setGlobalAlpha(0.9);
        g.setFill(new RadialGradient(0, 0, cx, cy + ry * 0.1, r * 1.1, false, CycleMethod.NO_CYCLE,
                new Stop(0, tint(Color.web("#ffd27f"), 0.95)), new Stop(0.5, tint(PAPER, 0.85)), new Stop(1, tint(Color.web("#c9a070"), 0.8))));
        g.fillOval(cx - rx, cy - ry, 2 * rx, 2 * ry);
        // La flamme et sa mèche, vues au travers.
        g.setGlobalAlpha(0.85);
        g.setFill(Color.web("#ff8a2a"));
        g.fillOval(cx - flame * 0.5, cy + ry * 0.15 - flame * 1.3, flame, flame * 1.6);
        g.setFill(Color.web("#fff6d8"));
        g.fillOval(cx - flame * 0.25, cy + ry * 0.15 - flame * 0.8, flame * 0.5, flame);
        g.setStroke(Color.web("#3a2a1a"));
        g.setLineWidth(Math.max(1, r * 0.03));
        g.setGlobalAlpha(0.8);
        g.strokeLine(cx, cy + ry * 0.15, cx, cy + ry * 0.35);
        // Les nervures : des méridiens et deux cercles de bambou.
        g.setStroke(RIB);
        g.setLineWidth(Math.max(0.8, r * 0.025));
        g.setGlobalAlpha(0.55);
        for (int k = 0; k < 7; k++) {
            double share = Math.cos(Math.PI * (k + 0.5) / 7);
            g.strokeOval(cx - rx * Math.abs(share), cy - ry, 2 * rx * Math.abs(share), 2 * ry);
        }
        g.strokeLine(cx - rx * 0.97, cy - ry * 0.25, cx + rx * 0.97, cy - ry * 0.25);
        g.strokeLine(cx - rx * 0.97, cy + ry * 0.25, cx + rx * 0.97, cy + ry * 0.25);
        // Le couvercle et le pied, et l'anse qui se balance quand on appuie.
        g.setGlobalAlpha(1);
        g.setFill(RIB);
        g.fillRoundRect(cx - rx * 0.4, cy - ry * 1.08, rx * 0.8, ry * 0.14, r * 0.05, r * 0.05);
        g.fillRoundRect(cx - rx * 0.35, cy + ry * 0.94, rx * 0.7, ry * 0.12, r * 0.05, r * 0.05);
        double sway = (growing ? 0.25 : 0.08) * Math.sin(time * 3);
        g.setStroke(RIB);
        g.setLineWidth(Math.max(1, r * 0.04));
        g.strokeArc(cx - rx * 0.35 + sway * r, cy - ry * 1.45, rx * 0.7, ry * 0.75, 0, 180, javafx.scene.shape.ArcType.OPEN);
    }

    // ------------------------------------------------------------------
    // Les animations
    // ------------------------------------------------------------------

    /**
     * Dessine l'animation de ce skin, sous la boule.
     *
     * @param growing vrai tant que le joueur maintient le point appuyé
     */
    static void animate(Skin skin, GraphicsContext g, DarkMatterView view, double cx, double cy, double w, double h, boolean growing) {
        double strength = Math.max(view.fall(), growing ? 0.6 : 0);
        switch (skin) {
            case DARK_SNOW -> snow(g, view, cx, cy, w, h, strength);
            default -> hourglass(g, view, cx, cy, w, h, strength);
        }
        g.setGlobalAlpha(1);
    }

    /** La neige : des flocons lents qui tourbillonnent, et que la boule attire quand elle grossit. */
    private static void snow(GraphicsContext g, DarkMatterView view, double cx, double cy, double w, double h, double strength) {
        double time = view.time();
        int flakes = 70;
        g.setStroke(FLAKE);
        for (int i = 0; i < flakes; i++) {
            double fall = ((fraction(i * 4.7) + time * (0.03 + 0.03 * fraction(i * 2.9))) % 1);
            double x = (fraction(i * 1.3) + 0.08 * Math.sin(time * 0.6 + i)) * w;
            double y = fall * h;
            // Attirés vers la boule : plus la matière grossit, plus ils s'enroulent autour d'elle.
            double pull = strength * 0.6;
            double a = Math.atan2(y - cy, x - cx) + time * 0.5 * pull;
            double d = Math.hypot(x - cx, y - cy) * (1 - pull * 0.5);
            x = cx + (x - cx) * (1 - pull) + d * Math.cos(a) * pull;
            y = cy + (y - cy) * (1 - pull) + d * Math.sin(a) * pull;
            double size = 1.5 + 2.5 * fraction(i * 6.1);
            double spin = time * (0.4 + fraction(i)) + i;
            g.setLineWidth(0.9);
            g.setGlobalAlpha(0.35 + 0.5 * fraction(i * 7.7));
            for (int k = 0; k < 3; k++) {
                double b = spin + Math.PI * k / 3;
                g.strokeLine(x - size * Math.cos(b), y - size * Math.sin(b), x + size * Math.cos(b), y + size * Math.sin(b));
            }
        }
        // Un manteau de neige en bas de la vue.
        g.setGlobalAlpha(0.25);
        g.setFill(FLAKE);
        g.fillRect(0, h - 5, w, 5);
    }

    /** Le sablier : en filigrane autour de la boule ; les grains du haut coulent par le goulot jusqu'à elle. */
    private static void hourglass(GraphicsContext g, DarkMatterView view, double cx, double cy, double w, double h, double strength) {
        double time = view.time(), ball = Math.max(view.blobRadius(), 4), growth = view.growth();
        double tall = Math.min(w, h) * 0.46, wide = tall * 0.6;
        double neckY = cy - ball * 1.3;
        // Le filigrane : deux ampoules, le goulot au-dessus de la boule, et les plateaux.
        g.setStroke(GLASS);
        g.setLineWidth(1.2);
        g.setGlobalAlpha(0.3);
        g.strokePolyline(new double[] {cx - wide, cx - wide * 0.1, cx - wide * 0.1, cx - wide},
                new double[] {cy - tall, neckY - tall * 0.05, neckY + tall * 0.05, cy + tall}, 4);
        g.strokePolyline(new double[] {cx + wide, cx + wide * 0.1, cx + wide * 0.1, cx + wide},
                new double[] {cy - tall, neckY - tall * 0.05, neckY + tall * 0.05, cy + tall}, 4);
        g.setLineWidth(4);
        g.setGlobalAlpha(0.45);
        g.strokeLine(cx - wide * 1.1, cy - tall, cx + wide * 1.1, cy - tall);
        g.strokeLine(cx - wide * 1.1, cy + tall, cx + wide * 1.1, cy + tall);
        // Le sable restant en haut : il baisse à mesure que la matière grossit.
        double level = (1 - growth) * (neckY - tall * 0.05 - (cy - tall)) * 0.8;
        if (level > 1) {
            double top = neckY - tall * 0.05 - level;
            double halfTop = wide * 0.1 + (wide - wide * 0.1) * (level / (neckY - tall * 0.05 - (cy - tall)));
            g.setGlobalAlpha(0.35);
            g.setFill(SAND);
            g.fillPolygon(new double[] {cx - halfTop, cx + halfTop, cx + wide * 0.1, cx - wide * 0.1},
                    new double[] {top, top, neckY - tall * 0.05, neckY - tall * 0.05}, 4);
        }
        // Les grains : ils glissent vers le goulot puis tombent droit sur la boule.
        g.setFill(SAND);
        int grains = (int) (24 + 40 * strength);
        for (int i = 0; i < grains; i++) {
            double phase = (fraction(i * 2.3) + time * (0.35 + 0.5 * strength) * (0.7 + 0.6 * fraction(i))) % 1;
            double x, y;
            if (phase < 0.45) {
                // Dans l'ampoule du haut, de plus en plus serrés vers le goulot.
                double u = phase / 0.45;
                double spread = (1 - u) * (1 - u);
                x = cx + (fraction(i * 5.1) - 0.5) * 2 * wide * 0.7 * spread;
                y = cy - tall * 0.8 + (neckY - (cy - tall * 0.8)) * u;
            } else {
                double u = (phase - 0.45) / 0.55;
                x = cx + (fraction(i * 5.1) - 0.5) * wide * 0.12;
                y = neckY + (cy - ball - neckY) * u * u;
            }
            g.setGlobalAlpha(0.5 + 0.5 * fraction(i * 8.3));
            g.fillOval(x - 1.2, y - 1.2, 2.4, 2.4);
        }
    }

}
