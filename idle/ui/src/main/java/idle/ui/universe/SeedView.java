package idle.ui.universe;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * La graine d'univers (version 0.83), le visuel de son sous-onglet : une graine en amande, dorée,
 * qui respire au centre d'un halo, une pousse qui sort de sa pointe et grandit avec le nombre de
 * graines, des ondes de germination qui partent d'elle, et une poussière qui monte. Les graines de
 * plus tournent autour d'elle, plus petites, jusqu'à {@link #MAX_ORBITING}.
 */
public final class SeedView extends Canvas {

    /** Graines dessinées autour de la première, au plus. */
    public static final int MAX_ORBITING = 24;

    private static final Color BACK = Color.web("#07060a");
    private static final Color HALO = Color.web("#e6d27a");
    private static final Color SHELL = Color.web("#b8963c");
    private static final Color FLESH = Color.web("#f0dc8c");
    private static final Color SEAM = Color.web("#fff6d2");
    private static final Color SPROUT = Color.web("#9fe08a");
    private static final Color MOTE = Color.web("#f7e7a8");
    private static final int MOTES = 60;
    private static final int WAVES = 3;
    private static final double WAVE_SECONDS = 4.5;

    private double time;
    private double seeds;

    public SeedView() {
        super(800, 400);
    }

    /** Le nombre de graines à montrer (un réel : il peut être très grand). */
    public void setSeeds(double count) {
        seeds = Math.max(0, count);
    }

    /** Graines dessinées autour de la première : pour les vérifications. */
    public int orbiting() {
        return (int) Math.min(MAX_ORBITING, Math.max(0, Math.floor(seeds) - 1));
    }

    /** La longueur de la pousse, de 0 à 1 : elle grandit d'un dixième à chaque décade de graines. */
    public double sprout() {
        if (seeds < 1) return 0;
        return Math.min(1, 0.15 + 0.1 * Math.log10(seeds));
    }

    public void frame(double elapsed, boolean moving) {
        if (moving) time += Math.min(elapsed, 0.1);
        draw();
    }

    private void draw() {
        double w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        GraphicsContext g = getGraphicsContext2D();
        g.setGlobalAlpha(1);
        g.clearRect(0, 0, w, h);
        g.setFill(BACK);
        g.fillRect(0, 0, w, h);
        double cx = w / 2, cy = h * 0.55;
        double size = Math.min(w, h) * 0.16;
        boolean any = seeds >= 1;

        // La poussière qui monte, lentement, et s'éteint en haut.
        for (int k = 0; k < MOTES; k++) {
            double speed = 8 + 14 * UniverseView.noise(k * 7 + 1);
            double y = h - ((UniverseView.noise(k * 7 + 2) * h + time * speed) % h);
            double x = w * UniverseView.noise(k * 7 + 3) + Math.sin(time * 0.6 + k) * 6;
            double fade = y / h;
            g.setGlobalAlpha(0.15 + 0.45 * fade * UniverseView.noise(k * 7 + 4));
            g.setFill(MOTE);
            double r = 0.8 + 1.4 * UniverseView.noise(k * 7 + 5);
            g.fillOval(x - r, y - r, 2 * r, 2 * r);
        }
        g.setGlobalAlpha(1);

        if (!any) {
            // Pas encore de graine : un creux vide, à peine marqué.
            g.setStroke(SHELL);
            g.setGlobalAlpha(0.35);
            g.setLineWidth(1.5);
            seedPath(g, cx, cy, size, 0, false);
            g.setGlobalAlpha(1);
            return;
        }

        // Les ondes de germination.
        g.setStroke(HALO);
        g.setLineWidth(1.2);
        for (int k = 0; k < WAVES; k++) {
            double phase = ((time / WAVE_SECONDS) + k / (double) WAVES) % 1;
            double r = size * (1 + 2.6 * phase);
            g.setGlobalAlpha(0.35 * (1 - phase));
            g.strokeOval(cx - r, cy - r * 0.9, 2 * r, 1.8 * r);
        }
        g.setGlobalAlpha(1);

        // Le halo, qui respire.
        double breath = 1 + 0.035 * Math.sin(time * 1.7);
        UniverseView.glow(g, cx, cy, size * 2.4 * breath, HALO, 0.30);
        UniverseView.glow(g, cx, cy, size * 1.2 * breath, SEAM, 0.25);

        // Les graines de plus, en orbite : derrière (en haut) plus petites et plus pâles, devant plus grandes.
        int count = orbiting();
        for (int k = 0; k < count; k++) {
            double angle = 2 * Math.PI * k / Math.max(1, count) + time * (0.12 + 0.05 * (k % 3));
            double ring = size * (2.0 + 0.6 * UniverseView.noise(k * 11 + 1));
            double x = cx + Math.cos(angle) * ring * 1.3;
            double y = cy + Math.sin(angle) * ring * 0.45;
            double depth = 0.5 + 0.5 * Math.sin(angle);
            double s = size * (0.16 + 0.12 * UniverseView.noise(k * 11 + 2)) * (0.75 + 0.35 * depth);
            UniverseView.glow(g, x, y, s * 2.2, HALO, 0.25 + 0.2 * depth);
            g.setGlobalAlpha(0.55 + 0.45 * depth);
            g.setFill(FLESH);
            seedPath(g, x, y, s, angle * 0.7, true);
        }
        g.setGlobalAlpha(1);

        // La pousse, une tige courbe qui sort de la pointe, et sa feuille.
        double grow = sprout();
        if (grow > 0) {
            double tipX = cx + Math.sin(-0.35) * size * 1.0, tipY = cy - Math.cos(-0.35) * size * 1.0;
            int n = 14;
            double[] xs = new double[n], ys = new double[n];
            for (int i = 0; i < n; i++) {
                double t = i / (double) (n - 1);
                double sway = Math.sin(time * 1.1) * 4 * t;
                xs[i] = tipX - size * 0.5 * grow * Math.sin(t * 1.4) + sway;
                ys[i] = tipY - size * 1.6 * grow * t;
            }
            g.setStroke(SPROUT);
            g.setLineWidth(Math.max(1.5, size * 0.05));
            g.setGlobalAlpha(0.9);
            g.strokePolyline(xs, ys, n);
            double lx = xs[n - 1], ly = ys[n - 1];
            double leaf = size * 0.35 * grow;
            double[] fx = {lx, lx + leaf, lx + leaf * 1.4, lx + leaf * 0.6};
            double[] fy = {ly, ly - leaf * 0.5, ly - leaf * 0.1, ly + leaf * 0.25};
            g.setFill(SPROUT);
            g.setGlobalAlpha(0.8);
            g.fillPolygon(fx, fy, 4);
            g.setGlobalAlpha(1);
        }

        // La graine elle-même : la coque, la chair, la couture.
        double s = size * breath;
        g.setFill(SHELL);
        seedPath(g, cx, cy, s * 1.06, -0.35, true);
        g.setFill(FLESH);
        seedPath(g, cx, cy, s, -0.35, true);
        g.setStroke(SEAM);
        g.setLineWidth(Math.max(1, size * 0.03));
        g.setGlobalAlpha(0.8);
        double ax = Math.sin(-0.35), ay = -Math.cos(-0.35);
        g.strokeLine(cx - ax * s * 0.75, cy - ay * s * 0.75, cx + ax * s * 0.85, cy + ay * s * 0.85);
        g.setGlobalAlpha(1);
        UniverseView.glow(g, cx - s * 0.25, cy - s * 0.15, s * 0.45, SEAM, 0.45);
    }

    /**
     * Une amande, pointe en haut, tournée de {@code turn} radians : remplie, ou seulement son contour.
     * Calculée point par point, sans rotation du dessin.
     */
    private static void seedPath(GraphicsContext g, double cx, double cy, double size, double turn, boolean fill) {
        int n = 32;
        // Un point de plus, le premier répété, pour que le contour se referme.
        double[] xs = new double[n + 1], ys = new double[n + 1];
        double cos = Math.cos(turn), sin = Math.sin(turn);
        for (int i = 0; i < n; i++) {
            double t = 2 * Math.PI * i / n;
            double x = size * 0.62 * Math.sin(t) * (0.7 - 0.3 * Math.cos(t));
            double y = -size * Math.cos(t);
            xs[i] = cx + x * cos - y * sin;
            ys[i] = cy + x * sin + y * cos;
        }
        xs[n] = xs[0];
        ys[n] = ys[0];
        if (fill) g.fillPolygon(xs, ys, n);
        else g.strokePolyline(xs, ys, n + 1);
    }
}
