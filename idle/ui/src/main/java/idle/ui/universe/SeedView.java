package idle.ui.universe;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * La graine d'univers (version 0.083), le visuel de son sous-onglet : une graine en amande, dorée,
 * qui respire au centre d'un halo, des ondes qui partent d'elle, et une poussière qui monte. Les
 * graines de plus tournent autour d'elle, plus petites, jusqu'à {@link #MAX_ORBITING}.
 *
 * <p>Version 0.103 : la graine ne germe plus seule. Le bouton de la sous-page la fait germer une fois
 * l'objectif de la dimension 1 atteint ; ensuite, chaque clic fait tomber une goutte d'eau sur le germe,
 * qui grandit et prend des feuilles.
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

    private static final Color WATER = Color.web("#8fd3ff");
    /** Secondes que met une goutte pour tomber sur le germe. */
    private static final double DROP_SECONDS = 0.7;

    /** Une goutte qui tombe, puis éclabousse. */
    private static final class Drop {
        double age;
        final double drift;

        Drop(double drift) {
            this.drift = drift;
        }
    }

    private double time;
    private double seeds;
    private boolean sprouted;
    private long waterings;
    /** La longueur montrée du germe : elle rejoint doucement celle que donnent les arrosages. */
    private double shownSprout;
    private final java.util.List<Drop> drops = new java.util.ArrayList<>();
    private int dropCount;
    /** Le germe devenu brindille (version 0.106) : une tige de bois, plus épaisse, et ses rameaux. */
    private boolean twig;
    /** L'éclat qui salue la brindille, de 1 à 0. */
    private double flash;
    private static final Color WOOD = Color.web("#a07a4c");

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

    /** La graine a-t-elle germé, et combien de fois son germe a-t-il été arrosé. */
    public void setSprout(boolean sprouted, long waterings) {
        if (sprouted && !this.sprouted && shownSprout <= 0) shownSprout = 0.02;
        this.sprouted = sprouted;
        this.waterings = Math.max(0, waterings);
        if (!sprouted) shownSprout = 0;
    }

    /** Le germe est-il devenu brindille. */
    public void setTwig(boolean twig) {
        this.twig = twig;
    }

    public boolean isTwig() {
        return twig;
    }

    /** Salue la brindille d'un éclat de lumière. */
    public void celebrate() {
        flash = 1;
    }

    /** Une goutte d'eau tombe sur le germe. */
    public void water() {
        if (drops.size() < 12) drops.add(new Drop(UniverseView.noise(++dropCount * 3 + 7) - 0.5));
    }

    /** Les gouttes en train de tomber : pour les vérifications. */
    public int drops() {
        return drops.size();
    }

    /**
     * La longueur que visent les arrosages, de 0 à 1 : 0,15 à la germination, puis de plus en plus
     * lentement vers 1.
     */
    public double sprout() {
        if (!sprouted || seeds < 1) return 0;
        return Math.min(1, 0.15 + 0.85 * (1 - Math.exp(-waterings / 30.0)));
    }

    /** Les feuilles du germe : une à la germination, une de plus tous les quinze arrosages, cinq au plus. */
    public int leaves() {
        return sprouted ? (int) Math.min(5, 1 + waterings / 15) : 0;
    }

    public void frame(double elapsed, boolean moving) {
        double dt = Math.min(elapsed, 0.1);
        if (moving) time += dt;
        // Le germe rejoint sa longueur en une seconde ou deux, même animations coupées.
        shownSprout += (sprout() - shownSprout) * Math.min(1, dt * 2.5);
        flash = Math.max(0, flash - dt * 0.6);
        for (Drop drop : drops) drop.age += dt;
        drops.removeIf(drop -> drop.age > DROP_SECONDS + 0.6);
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

        // Le germe, une tige courbe qui sort de la pointe, et ses feuilles.
        double grow = shownSprout;
        double tipX = cx + Math.sin(-0.35) * size * 1.0, tipY = cy - Math.cos(-0.35) * size * 1.0;
        double topX = tipX, topY = tipY;
        if (grow > 0.005) {
            int n = 14;
            double[] xs = new double[n], ys = new double[n];
            for (int i = 0; i < n; i++) {
                double t = i / (double) (n - 1);
                double sway = Math.sin(time * 1.1) * 4 * t;
                xs[i] = tipX - size * 0.5 * grow * Math.sin(t * 1.4) + sway;
                ys[i] = tipY - size * 1.6 * grow * t;
            }
            // Brindille : la tige devient bois, plus épaisse, et deux rameaux en partent.
            g.setStroke(twig ? WOOD : SPROUT);
            g.setLineWidth(Math.max(1.5, size * (twig ? 0.09 : 0.05)));
            g.setGlobalAlpha(0.95);
            g.strokePolyline(xs, ys, n);
            if (twig) {
                for (int b = 0; b < 2; b++) {
                    int at = b == 0 ? n / 2 : (3 * n) / 4;
                    double side = b == 0 ? -1 : 1, len = size * 0.45 * grow;
                    double bx = xs[at] + side * len, by = ys[at] - len * 0.6;
                    g.setLineWidth(Math.max(1, size * 0.04));
                    g.strokeLine(xs[at], ys[at], bx, by);
                    double leaf = size * 0.2;
                    g.setFill(SPROUT);
                    g.setGlobalAlpha(0.85);
                    g.fillPolygon(new double[] {bx, bx + side * leaf, bx + side * leaf * 1.3, bx + side * leaf * 0.5},
                            new double[] {by, by - leaf * 0.5, by - leaf * 0.05, by + leaf * 0.25}, 4);
                    g.setGlobalAlpha(0.95);
                }
            }
            topX = xs[n - 1];
            topY = ys[n - 1];
            int leaves = leaves();
            for (int k = 0; k < leaves; k++) {
                // La première au bout ; les autres le long de la tige, de chaque côté.
                int at = k == 0 ? n - 1 : Math.max(2, n - 1 - k * 2);
                double lx = xs[at], ly = ys[at], side = k % 2 == 0 ? 1 : -1;
                double leaf = size * (k == 0 ? 0.35 : 0.24) * grow;
                double[] fx = {lx, lx + side * leaf, lx + side * leaf * 1.4, lx + side * leaf * 0.6};
                double[] fy = {ly, ly - leaf * 0.5, ly - leaf * 0.1, ly + leaf * 0.25};
                g.setFill(SPROUT);
                g.setGlobalAlpha(0.8);
                g.fillPolygon(fx, fy, 4);
            }
            g.setGlobalAlpha(1);
        }

        // Les gouttes : elles tombent du haut sur le germe, puis éclaboussent.
        for (Drop drop : drops) {
            double targetX = topX + drop.drift * size * 0.2;
            if (drop.age < DROP_SECONDS) {
                double t = drop.age / DROP_SECONDS;
                double y = -10 + (topY + 10) * t * t, r = Math.max(3, size * 0.06);
                g.setFill(WATER);
                g.setGlobalAlpha(0.9);
                double[] px = {targetX, targetX + r, targetX, targetX - r};
                double[] py = {y - r * 2.2, y, y + r, y};
                g.fillPolygon(px, py, 4);
                g.fillOval(targetX - r, y - r * 0.2, r * 2, r * 1.4);
            } else {
                double t = (drop.age - DROP_SECONDS) / 0.6;
                g.setFill(WATER);
                g.setGlobalAlpha(0.8 * (1 - t));
                for (int k = 0; k < 6; k++) {
                    double a = Math.PI + Math.PI * k / 5;
                    double d = size * 0.35 * t;
                    double r = Math.max(1.5, size * 0.025);
                    g.fillOval(targetX + Math.cos(a) * d - r, topY + Math.sin(a) * d * 0.6 - r + size * 0.2 * t * t, 2 * r, 2 * r);
                }
                UniverseView.glow(g, targetX, topY, size * 0.4 * (1 - t) + 2, WATER, 0.5 * (1 - t));
            }
        }
        g.setGlobalAlpha(1);

        if (flash > 0) UniverseView.glow(g, cx, cy - size, size * (2 + 4 * (1 - flash)), SEAM, 0.8 * flash);

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
