package idle.ui.universe;

import java.util.ArrayList;
import java.util.List;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Les arbres de la graine (version 0.121), du bonsaï (palier 5) à l'arbre-monde (palier 11), et leurs effets.
 *
 * <p>Chaque palier remplit davantage la vue : l'arbre grandit bien plus vite que la vue ne s'éloigne. Le tronc
 * s'effile en polygones, porte son écorce, puis ses nœuds ; les rameaux se divisent de plus en plus finement ;
 * le feuillage est fait de feuilles une à une, en trois teintes. Les effets montent avec l'avancement : halo,
 * poussière, lucioles, feuilles qui tombent, sève qui monte dans le tronc, rayons, puis pour l'arbre-monde des
 * étoiles, des mondes en orbite et des racines de lumière. Tout est tiré d'un bruit fixe : rien ne clignote d'une
 * image à l'autre, sauf ce qui doit bouger.
 */
final class SeedTree {

    private SeedTree() {
    }

    /**
     * Hauteur de l'arbre, en part de la hauteur de la vue, du palier 5 au palier 11. Version 0.143 : le jeune arbre un
     * peu plus grand (0,46 avant), il paraissait plus petit que l'arbuste d'avant.
     */
    private static final double[] HEIGHT = {0.36, 0.40, 0.50, 0.51, 0.54, 0.56, 0.58};
    /** Largeur du tronc au pied, en part de la hauteur de l'arbre. */
    private static final double[] TRUNK = {0.075, 0.035, 0.055, 0.08, 0.11, 0.13, 0.15};
    /** Profondeur des ramifications. */
    private static final int[] DEPTH = {4, 3, 5, 6, 6, 6, 6};
    /** Écart des rameaux : le bonsaï s'étale, le jeune arbre monte, le géant s'ouvre. */
    private static final double[] SPREAD = {0.75, 0.55, 0.46, 0.50, 0.52, 0.48, 0.50};
    /** Torsion : l'arbre ancien se tord, le jeune pousse droit. */
    private static final double[] TWIST = {0.35, 0.25, 0.12, 0.20, 0.45, 0.25, 0.30};
    /** Taille des touffes de feuilles, en part de la hauteur. */
    private static final double[] LEAF = {0.085, 0.07, 0.07, 0.075, 0.07, 0.075, 0.08};

    private static final Color[][] FOLIAGE = {
            {Color.web("#2f6b3a"), Color.web("#4f9a4f"), Color.web("#86cf72")},   // bonsaï
            {Color.web("#2f7a3c"), Color.web("#56b259"), Color.web("#9be07e")},   // arbuste
            {Color.web("#3c8f45"), Color.web("#6cc65f"), Color.web("#b5ef8c")},   // jeune arbre
            {Color.web("#245e33"), Color.web("#3f8f46"), Color.web("#7cc464")},   // arbre mûr
            {Color.web("#5a4a22"), Color.web("#8a7a32"), Color.web("#d0a84a")},   // arbre ancien, mordoré
            {Color.web("#1d4f34"), Color.web("#2f7f4c"), Color.web("#68c27a")},   // arbre géant
            {Color.web("#1f6f73"), Color.web("#46c7b8"), Color.web("#b8fff0")}};  // arbre-monde, lumineux
    private static final Color BARK_DARK = Color.web("#3e2c1c"), BARK = Color.web("#6e5236"), BARK_LIGHT = Color.web("#9c7a52");
    private static final Color SOIL = Color.web("#2a2016"), GRASS = Color.web("#4f8f45"), MOSS = Color.web("#6fae5a");
    private static final Color GLOW = Color.web("#fff6d2"), FIREFLY = Color.web("#f7f0a0"), SAP = Color.web("#9fffe0");
    private static final Color BLOSSOM = Color.web("#ffb7d0"), FRUIT = Color.web("#ffcf5a"), STAR = Color.web("#e8f4ff");

    /** Touffes de feuilles du dernier arbre dessiné : pour les vérifications (le coût du dessin). */
    static int lastTips;

    /** Un bout de rameau : sa position, sa direction, et s'il porte des feuilles. */
    private record Tip(double x, double y, double angle, int index) {
    }

    /** Un segment de tronc ou de rameau, pour la sève qui monte : du pied vers le bout. */
    private record Segment(double x0, double y0, double x1, double y1, double width, int depth) {
    }

    /** Ce que l'arbre laisse aux effets : le centre et la taille du feuillage, ses segments, ses bouts. */
    static final class Shape {
        double crownX, crownY, crownRadius;
        final List<Segment> segments = new ArrayList<>();
        final List<Tip> tips = new ArrayList<>();
    }

    /**
     * L'intensité des effets, de 0 au palier 4 à 1 à l'arbre-monde : chaque palier en montre un peu plus.
     */
    static double prominence(int stage) {
        return Math.max(0, Math.min(1, (stage - 4) / 7.0));
    }

    /**
     * Dessine l'arbre du palier {@code stage} (5 à 11) dont le pied est en {@code baseX, baseY}, et ses effets.
     * {@code growth}, de 0 à 1, le fait grandir pendant le passage. Rend le centre du feuillage.
     */
    static double[] draw(GraphicsContext g, double w, double h, double baseX, double baseY, int stage, double growth, double time) {
        int i = Math.max(0, Math.min(6, stage - 5));
        double p = prominence(stage);
        double height = h * HEIGHT[i] * (0.35 + 0.65 * growth);
        Shape shape = new Shape();
        shape.crownX = baseX;
        shape.crownY = baseY - height * 0.72;
        shape.crownRadius = height * (stage == 5 ? 0.45 : stage == 6 ? 0.42 : 0.5);

        // Derrière l'arbre : le halo, puis les rayons qui descendent du feuillage.
        UniverseView.glow(g, shape.crownX, shape.crownY, shape.crownRadius * (1.4 + 1.6 * p), FOLIAGE[i][2], 0.10 + 0.30 * p);
        if (stage >= 9) rays(g, shape, h, stage, time);
        if (stage >= 11) cosmos(g, w, h, shape, time);

        ground(g, w, baseX, baseY, height, stage, time);
        if (stage >= 8) roots(g, baseX, baseY, height, stage, time);

        // Le tronc et les rameaux.
        double trunk = height * TRUNK[i] * (0.7 + 0.3 * growth);
        double sway = Math.sin(time * 0.7) * 0.025;
        if (stage == 6) {
            // L'arbuste : plusieurs tiges qui partent du sol, en éventail.
            for (int k = 0; k < 6; k++) {
                double a = -0.75 + 0.3 * k + sway + (UniverseView.noise(k * 31 + 5) - 0.5) * 0.15;
                branch(g, shape, baseX + (k - 2.5) * trunk * 0.6, baseY, height * (0.38 + 0.08 * UniverseView.noise(k * 7 + 1)), a,
                        trunk, DEPTH[i], k * 13 + 3, i, time, 0);
            }
        } else if (stage == 5) {
            // Le bonsaï : un tronc en S, court et épais, puis des plateaux de feuillage.
            double mx = baseX + height * 0.14, my = baseY - height * 0.30;
            taper(g, baseX, baseY, mx, my, trunk, trunk * 0.8, BARK, 6);
            shape.segments.add(new Segment(baseX, baseY, mx, my, trunk, 0));
            double nx = baseX - height * 0.06, ny = baseY - height * 0.52;
            taper(g, mx, my, nx, ny, trunk * 0.8, trunk * 0.62, BARK, 6);
            shape.segments.add(new Segment(mx, my, nx, ny, trunk * 0.8, 0));
            branch(g, shape, nx, ny, height * 0.30, -0.15 + sway, trunk * 0.6, DEPTH[i], 5, i, time, 1);
        } else {
            branch(g, shape, baseX, baseY, height * 0.34, sway, trunk, DEPTH[i], stage * 7, i, time, 0);
        }

        foliage(g, shape, height, stage, i, time);
        lastTips = shape.tips.size();

        // Devant : les feuilles qui tombent, les lucioles, la sève qui monte, les étincelles.
        if (stage >= 7) fallingLeaves(g, shape, baseY, height, stage, i, time);
        if (stage >= 6) fireflies(g, shape, stage, time);
        if (stage >= 8) sap(g, shape, stage, time);
        // Le centre du feuillage et son rayon : les bulles de l'arbre s'y posent (version 0.142).
        return new double[] {shape.crownX, shape.crownY, shape.crownRadius};
    }

    // ------------------------------------------------------------------
    // Le sol et les racines
    // ------------------------------------------------------------------

    private static void ground(GraphicsContext g, double w, double baseX, double baseY, double height, int stage, double time) {
        double width = height * (0.55 + 0.08 * (stage - 5));
        g.setFill(SOIL);
        g.setGlobalAlpha(0.95);
        g.fillOval(baseX - width, baseY - height * 0.035, width * 2, height * 0.09);
        // Les herbes, puis la mousse et les pierres pour les vieux arbres.
        int blades = 18 + 6 * (stage - 5);
        g.setStroke(GRASS);
        for (int k = 0; k < blades; k++) {
            double x = baseX - width * 0.95 + 2 * width * 0.95 * UniverseView.noise(k * 3 + 11);
            double len = height * (0.018 + 0.03 * UniverseView.noise(k * 3 + 12));
            double bend = Math.sin(time * 1.3 + k) * len * 0.25;
            g.setLineWidth(Math.max(1, height * 0.004));
            g.setGlobalAlpha(0.8);
            g.strokeLine(x, baseY, x + bend, baseY - len);
        }
        if (stage >= 8) {
            g.setFill(MOSS);
            for (int k = 0; k < 10 + 2 * (stage - 8); k++) {
                double x = baseX - width * 0.8 + 1.6 * width * UniverseView.noise(k * 5 + 41), r = height * (0.008 + 0.012 * UniverseView.noise(k * 5 + 42));
                g.setGlobalAlpha(0.7);
                g.fillOval(x - r, baseY - r * 0.6, r * 2, r * 1.2);
            }
            g.setFill(Color.web("#5a5f66"));
            for (int k = 0; k < 4; k++) {
                double x = baseX + (k < 2 ? -1 : 1) * width * (0.55 + 0.25 * UniverseView.noise(k + 60)), r = height * (0.015 + 0.01 * k % 2);
                g.setGlobalAlpha(0.85);
                g.fillOval(x - r * 1.3, baseY - r * 0.8, r * 2.6, r * 1.4);
            }
        }
        g.setGlobalAlpha(1);
    }

    private static void roots(GraphicsContext g, double baseX, double baseY, double height, int stage, double time) {
        int count = 4 + 2 * (stage - 8);
        for (int k = 0; k < count; k++) {
            double side = k % 2 == 0 ? -1 : 1, reach = height * (0.18 + 0.12 * UniverseView.noise(k * 9 + 2)) * (stage >= 11 ? 1.6 : 1);
            double[] xs = new double[6], ys = new double[6];
            for (int s = 0; s < 6; s++) {
                double t = s / 5.0;
                xs[s] = baseX + side * reach * t;
                ys[s] = baseY - height * 0.01 + height * 0.035 * Math.sin(t * Math.PI) * (0.5 + UniverseView.noise(k * 9 + s)) + height * 0.02 * t;
            }
            g.setStroke(stage >= 11 ? SAP : BARK_DARK);
            g.setLineWidth(Math.max(1.2, height * (0.014 - 0.0012 * (k / 2))));
            g.setGlobalAlpha(stage >= 11 ? 0.55 + 0.35 * Math.sin(time * 1.5 + k) : 0.9);
            g.strokePolyline(xs, ys, 6);
        }
        g.setGlobalAlpha(1);
    }

    // ------------------------------------------------------------------
    // Le tronc, les rameaux, le feuillage
    // ------------------------------------------------------------------

    /**
     * Un rameau effilé, puis ses rameaux, plus courts et plus fins. Les bouts vont dans {@code shape.tips}, les
     * segments dans {@code shape.segments}.
     */
    private static void branch(GraphicsContext g, Shape shape, double x, double y, double length, double angle, double width,
            int depth, int seed, int i, double time, int level) {
        double wiggle = (UniverseView.noise(seed * 7 + 1) - 0.5) * TWIST[i];
        double a = angle + wiggle * 0.5 + Math.sin(time * 0.9 + level + seed % 7) * 0.012 * level;
        double ex = x + Math.sin(a) * length, ey = y - Math.cos(a) * length;
        double end = width * 0.68;
        taper(g, x, y, ex, ey, width, end, level <= 1 ? BARK : BARK_LIGHT, level <= 1 ? 5 : 2);
        shape.segments.add(new Segment(x, y, ex, ey, width, level));
        // Les nœuds de l'arbre ancien.
        if (i == 4 && level <= 2 && UniverseView.noise(seed * 3 + 9) > 0.6) {
            g.setFill(BARK_DARK);
            g.setGlobalAlpha(0.8);
            double kx = x + (ex - x) * 0.5, ky = y + (ey - y) * 0.5, r = width * 0.35;
            g.fillOval(kx - r, ky - r * 0.7, r * 2, r * 1.4);
            g.setGlobalAlpha(1);
        }
        if (depth <= 0) {
            shape.tips.add(new Tip(ex, ey, a, shape.tips.size()));
            return;
        }
        // Les touffes poussent aussi le long des rameaux fins, pas seulement au bout.
        if (depth <= 2) shape.tips.add(new Tip(x + (ex - x) * 0.6, y + (ey - y) * 0.6, a, shape.tips.size()));
        int children = depth >= 4 && UniverseView.noise(seed * 11 + 3) > 0.45 ? 3 : 2;
        for (int c = 0; c < children; c++) {
            double spread = SPREAD[i] * (children == 3 ? (c - 1) : (c == 0 ? -0.8 : 0.8));
            double jitter = (UniverseView.noise(seed * 3 + c) - 0.5) * 0.4;
            double shrink = 0.70 + 0.14 * UniverseView.noise(seed + c * 5);
            // L'arbre se redresse : les rameaux du haut reviennent vers le ciel.
            double lift = -a * 0.18;
            branch(g, shape, ex, ey, length * shrink, a + spread + jitter + lift, end, depth - 1, seed * 5 + c + 1, i, time, level + 1);
        }
    }

    /**
     * Un segment de bois qui s'effile, de {@code w0} à {@code w1}, avec son ombre et ses lignes d'écorce.
     */
    private static void taper(GraphicsContext g, double x0, double y0, double x1, double y1, double w0, double w1, Color wood,
            int barkLines) {
        double dx = x1 - x0, dy = y1 - y0, len = Math.max(1e-6, Math.hypot(dx, dy));
        double nx = -dy / len, ny = dx / len;
        double[] xs = {x0 + nx * w0 / 2, x1 + nx * w1 / 2, x1 - nx * w1 / 2, x0 - nx * w0 / 2};
        double[] ys = {y0 + ny * w0 / 2, y1 + ny * w1 / 2, y1 - ny * w1 / 2, y0 - ny * w0 / 2};
        g.setGlobalAlpha(1);
        g.setFill(BARK_DARK);
        g.fillPolygon(xs, ys, 4);
        // La face éclairée : un peu plus étroite, décalée vers la gauche.
        double[] lx = {x0 + nx * w0 * 0.42, x1 + nx * w1 * 0.42, x1 - nx * w1 * 0.15, x0 - nx * w0 * 0.15};
        double[] ly = {y0 + ny * w0 * 0.42, y1 + ny * w1 * 0.42, y1 - ny * w1 * 0.15, y0 - ny * w0 * 0.15};
        g.setFill(wood);
        g.fillPolygon(lx, ly, 4);
        if (w0 > 4) {
            g.setStroke(BARK_DARK);
            g.setLineWidth(Math.max(0.8, w0 * 0.05));
            g.setGlobalAlpha(0.55);
            for (int k = 0; k < barkLines; k++) {
                double off = (k + 0.5) / barkLines - 0.5;
                g.strokeLine(x0 + nx * w0 * off * 0.8, y0 + ny * w0 * off * 0.8, x1 + nx * w1 * off * 0.8, y1 + ny * w1 * off * 0.8);
            }
            g.setGlobalAlpha(1);
        }
    }

    /** Le feuillage : chaque bout porte une touffe de feuilles, une à une, ombre d'abord, lumière ensuite. */
    private static void foliage(GraphicsContext g, Shape shape, double height, int stage, int i, double time) {
        Color[] tones = FOLIAGE[i];
        double radius = height * LEAF[i];
        int perTip = 7 + Math.min(3, stage - 5);
        double sumX = 0, sumY = 0;
        for (int pass = 0; pass < 3; pass++) {
            for (Tip tip : shape.tips) {
                double r = radius * (0.65 + 0.5 * UniverseView.noise(tip.index() * 5 + stage)) * (stage == 5 ? 1.25 : 1);
                double flat = stage == 5 ? 0.5 : 0.85;
                if (pass == 0) {
                    // La masse d'ombre, d'un seul tenant, derrière les feuilles.
                    g.setFill(tones[0]);
                    g.setGlobalAlpha(0.9);
                    g.fillOval(tip.x() - r, tip.y() - r * flat, 2 * r, 2 * r * flat);
                    sumX += tip.x();
                    sumY += tip.y();
                    continue;
                }
                Color tone = pass == 1 ? tones[1] : tones[2];
                int leaves = pass == 1 ? perTip : perTip / 2;
                for (int k = 0; k < leaves; k++) {
                    int n = tip.index() * 53 + k * 7 + pass * 3;
                    double ang = 2 * Math.PI * UniverseView.noise(n + 1), dist = r * (pass == 1 ? 0.85 : 0.55) * Math.sqrt(UniverseView.noise(n + 2));
                    double lx = tip.x() + Math.cos(ang) * dist, ly = tip.y() + Math.sin(ang) * dist * flat - (pass == 2 ? r * 0.15 : 0);
                    double flutter = Math.sin(time * 2.1 + n) * 0.25;
                    leaf(g, lx, ly, r * (0.28 + 0.12 * UniverseView.noise(n + 3)), ang + flutter, tone, pass == 1 ? 0.95 : 0.9);
                }
            }
        }
        // Des fleurs pour l'arbuste et le jeune arbre, des fruits pour l'arbre mûr, de l'or pour l'ancien.
        Color dot = stage == 6 || stage == 7 ? BLOSSOM : stage == 8 ? FRUIT : stage == 9 ? Color.web("#f0b45a") : null;
        if (dot != null) {
            g.setFill(dot);
            for (Tip tip : shape.tips) {
                if (UniverseView.noise(tip.index() * 17 + 4) < 0.45) continue;
                double r = radius * 0.13, ox = (UniverseView.noise(tip.index() * 17 + 5) - 0.5) * radius;
                g.setGlobalAlpha(0.95);
                g.fillOval(tip.x() + ox - r, tip.y() - r + radius * 0.2, 2 * r, 2 * r);
            }
        }
        // L'arbre-monde : son feuillage brille par endroits, comme des villes vues de loin.
        if (stage >= 11) {
            for (Tip tip : shape.tips) {
                if (tip.index() % 3 != 0) continue;
                double pulse = 0.5 + 0.5 * Math.sin(time * 1.7 + tip.index());
                UniverseView.glow(g, tip.x(), tip.y(), radius * (0.9 + 0.5 * pulse), tones[2], 0.18 + 0.2 * pulse);
            }
        }
        g.setGlobalAlpha(1);
        if (!shape.tips.isEmpty()) {
            shape.crownX = sumX / shape.tips.size();
            shape.crownY = sumY / shape.tips.size();
        }
    }

    /** Une feuille en amande, tournée de {@code angle}. */
    private static void leaf(GraphicsContext g, double x, double y, double size, double angle, Color tone, double alpha) {
        double c = Math.cos(angle), s = Math.sin(angle);
        double[] xs = {x - c * size, x - s * size * 0.45, x + c * size, x + s * size * 0.45};
        double[] ys = {y - s * size, y + c * size * 0.45, y + s * size, y - c * size * 0.45};
        g.setFill(tone);
        g.setGlobalAlpha(alpha);
        g.fillPolygon(xs, ys, 4);
    }

    // ------------------------------------------------------------------
    // Les effets, de plus en plus présents
    // ------------------------------------------------------------------

    /** Les feuilles qui tombent du feuillage, en se balançant, jusqu'au sol. */
    private static void fallingLeaves(GraphicsContext g, Shape shape, double baseY, double height, int stage, int i, double time) {
        int count = 3 + 3 * (stage - 7);
        for (int k = 0; k < count; k++) {
            double period = 7 + 5 * UniverseView.noise(k * 13 + 1);
            double t = ((time / period) + UniverseView.noise(k * 13 + 2)) % 1;
            double startX = shape.crownX + (UniverseView.noise(k * 13 + 3) - 0.5) * shape.crownRadius * 2;
            double x = startX + Math.sin(t * Math.PI * 4 + k) * height * 0.04;
            double y = shape.crownY + (baseY - shape.crownY) * t;
            leaf(g, x, y, height * 0.012, t * 9 + k, FOLIAGE[i][t < 0.5 ? 1 : 2], 0.85 * (1 - t * 0.6));
        }
        g.setGlobalAlpha(1);
    }

    /** Les lucioles : elles flottent autour du feuillage, de plus en plus nombreuses. */
    private static void fireflies(GraphicsContext g, Shape shape, int stage, double time) {
        int count = 6 + 7 * (stage - 6);
        for (int k = 0; k < count; k++) {
            double a = 2 * Math.PI * UniverseView.noise(k * 19 + 1) + time * (0.15 + 0.2 * UniverseView.noise(k * 19 + 2));
            double d = shape.crownRadius * (0.5 + 1.1 * UniverseView.noise(k * 19 + 3));
            double x = shape.crownX + Math.cos(a) * d + Math.sin(time * 1.3 + k) * 6;
            double y = shape.crownY + Math.sin(a) * d * 0.7 + Math.cos(time * 1.1 + k) * 5;
            double blink = 0.5 + 0.5 * Math.sin(time * (2 + UniverseView.noise(k * 19 + 4) * 3) + k);
            UniverseView.glow(g, x, y, 6 + 6 * blink, FIREFLY, 0.25 + 0.45 * blink);
            g.setFill(FIREFLY);
            g.setGlobalAlpha(0.6 + 0.4 * blink);
            g.fillOval(x - 1.5, y - 1.5, 3, 3);
        }
        g.setGlobalAlpha(1);
    }

    /** La sève : des points de lumière qui montent dans le tronc et les grosses branches. */
    private static void sap(GraphicsContext g, Shape shape, int stage, double time) {
        int per = 1 + (stage - 8);
        for (int s = 0; s < shape.segments.size(); s++) {
            Segment seg = shape.segments.get(s);
            if (seg.depth() > (stage >= 10 ? 3 : 2)) continue;
            for (int k = 0; k < per; k++) {
                double t = ((time * 0.35 + UniverseView.noise(s * 7 + k)) % 1);
                double x = seg.x0() + (seg.x1() - seg.x0()) * t, y = seg.y0() + (seg.y1() - seg.y0()) * t;
                double r = Math.max(1.5, seg.width() * 0.12);
                UniverseView.glow(g, x, y, r * 4, SAP, 0.35);
                g.setFill(SAP);
                g.setGlobalAlpha(0.85);
                g.fillOval(x - r, y - r, 2 * r, 2 * r);
            }
        }
        g.setGlobalAlpha(1);
    }

    /** Les rayons qui tombent du feuillage, de plus en plus larges. */
    private static void rays(GraphicsContext g, Shape shape, double h, int stage, double time) {
        int count = 5 + 2 * (stage - 9);
        for (int k = 0; k < count; k++) {
            double off = (k - (count - 1) / 2.0) / count * shape.crownRadius * 2.4;
            double width = shape.crownRadius * (0.10 + 0.06 * UniverseView.noise(k * 3 + 7));
            double x0 = shape.crownX + off, y0 = shape.crownY - shape.crownRadius * 0.2, x1 = x0 + off * 0.45, y1 = h;
            g.setFill(GLOW);
            g.setGlobalAlpha(0.04 + 0.05 * (0.5 + 0.5 * Math.sin(time * 0.6 + k)));
            g.fillPolygon(new double[] {x0 - width * 0.4, x0 + width * 0.4, x1 + width, x1 - width},
                    new double[] {y0, y0, y1, y1}, 4);
        }
        g.setGlobalAlpha(1);
    }

    /** Pour l'arbre-monde : un ciel d'étoiles qui tourne, et de petits mondes en orbite autour du feuillage. */
    private static void cosmos(GraphicsContext g, double w, double h, Shape shape, double time) {
        for (int k = 0; k < 90; k++) {
            double x = w * UniverseView.noise(k * 5 + 101), y = h * 0.85 * UniverseView.noise(k * 5 + 102);
            double twinkle = 0.5 + 0.5 * Math.sin(time * (1 + UniverseView.noise(k * 5 + 103) * 2) + k);
            double r = 0.6 + 1.2 * UniverseView.noise(k * 5 + 104);
            g.setFill(STAR);
            g.setGlobalAlpha(0.25 + 0.6 * twinkle);
            g.fillOval(x - r, y - r, 2 * r, 2 * r);
        }
        Color[] worlds = {Color.web("#8fd3ff"), Color.web("#ffcf5a"), Color.web("#c9a8ff"), Color.web("#9ff0b0"), Color.web("#ff9f8a")};
        for (int k = 0; k < worlds.length; k++) {
            double a = time * (0.12 + 0.05 * k) + k * 1.3;
            double rx = shape.crownRadius * (1.25 + 0.22 * k), ry = rx * 0.32;
            double x = shape.crownX + Math.cos(a) * rx, y = shape.crownY + Math.sin(a) * ry;
            g.setStroke(STAR);
            g.setLineWidth(0.8);
            g.setGlobalAlpha(0.12);
            g.strokeOval(shape.crownX - rx, shape.crownY - ry, 2 * rx, 2 * ry);
            double r = shape.crownRadius * (0.035 + 0.012 * k);
            UniverseView.glow(g, x, y, r * 3, worlds[k], 0.35);
            g.setFill(worlds[k]);
            g.setGlobalAlpha(0.95);
            g.fillOval(x - r, y - r, 2 * r, 2 * r);
        }
        g.setGlobalAlpha(1);
    }

    /**
     * Le passage vers un palier d'arbre, de plus en plus marqué : des ondes qui partent du pied, plus nombreuses et plus
     * grandes à chaque palier, et une gerbe de lumière. {@code age} en secondes depuis le début du passage.
     */
    static void evolveBurst(GraphicsContext g, double w, double h, double x, double y, int stage, double age) {
        double p = 0.35 + 0.65 * prominence(stage);
        double t = Math.max(0, Math.min(1, age / SeedView.EVOLVE_SECONDS));
        int waves = 1 + (int) Math.round(4 * p);
        for (int k = 0; k < waves; k++) {
            double local = t * 1.4 - k * 0.12;
            if (local <= 0 || local >= 1) continue;
            double r = Math.max(w, h) * (0.15 + 0.9 * p) * local;
            g.setStroke(GLOW);
            g.setLineWidth(1.5 + 4 * p * (1 - local));
            g.setGlobalAlpha(0.7 * (1 - local));
            g.strokeOval(x - r, y - r * 0.55, 2 * r, 1.1 * r);
        }
        int sparks = (int) (20 + 120 * p);
        for (int k = 0; k < sparks; k++) {
            double a = -Math.PI * UniverseView.noise(k * 3 + 201), speed = h * (0.25 + 0.75 * UniverseView.noise(k * 3 + 202)) * p;
            double sx = x + Math.cos(a) * speed * t, sy = y + Math.sin(a) * speed * t + h * 0.15 * t * t;
            double r = 1 + 2.5 * UniverseView.noise(k * 3 + 203) * p;
            g.setFill(k % 3 == 0 ? SAP : GLOW);
            g.setGlobalAlpha(Math.max(0, 0.9 * (1 - t)));
            g.fillOval(sx - r, sy - r, 2 * r, 2 * r);
        }
        g.setGlobalAlpha(Math.max(0, 0.35 * p * (1 - t * 3)));
        g.setFill(GLOW);
        g.fillRect(0, 0, w, h);
        g.setGlobalAlpha(1);
    }
}
