package idle.ui.common;

import idle.core.universe.Skin;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

/**
 * Les animations plein écran des skins de la version 0.191, dessinées à côté de {@link BurstView} pour
 * la garder lisible : l'explosion de matière noire sous ses deux animations (version 0.203 : la pluie et le feu d'artifice retirés), et le Big Bang
 * sous ses six nouveaux skins, chacun dans ses couleurs.
 * <ul>
 * <li>Neige : des flocons qui tourbillonnent de partout vers le centre, un blanc total, puis tout se dissipe.</li>
 * <li>Sablier : un grand sablier en filigrane dont le sable coule en entier, puis qui se retourne.</li>
 * <li>Encre de Chine : une goutte d'encre tombe sur le papier et s'étale en lavis, éclaboussures autour.</li>
 * <li>Vitrail : des verres colorés s'allument l'un après l'autre autour du centre, cernés de plomb, en rosace.</li>
 * <li>Abysses : la nuit bleue, du plancton qui monte en s'allumant, une méduse qui pulse et grandit.</li>
 * <li>Papier plié : une feuille se déplie en facettes depuis le centre, des confettis tombent.</li>
 * <li>Carte ancienne : le vélin sombre, une rose des vents qui tourne et s'agrandit, des constellations qui se tracent.</li>
 * <li>Néon de ville : l'asphalte mouillé, une enseigne qui clignote avant de s'allumer, des fenêtres qui s'éclairent.</li>
 * </ul>
 */
final class BurstSkinArt {

    private static final Color[] GLASS = {Color.web("#d62839"), Color.web("#f4a259"), Color.web("#f7d354"),
        Color.web("#4fa36b"), Color.web("#2f6fb5"), Color.web("#7a3fa0")};
    // Version 0.203 : les mêmes teintes nuit que la vue de l'espace (SpaceSkinArt).
    private static final Color INK = Color.web("#e9e6dd");
    private static final Color PAPER = Color.web("#111215");
    private static final Color PARCHMENT = Color.web("#191510");
    private static final Color SEPIA = Color.web("#c9a468");

    private BurstSkinArt() {
    }

    /** Vrai pour les animations dessinées ici : celles de la version 0.191. */
    static boolean draws(Skin skin) {
        return switch (skin) {
            case DARK_SNOW, DARK_HOURGLASS, BANG_INK, BANG_STAINED, BANG_ABYSS, BANG_ORIGAMI, BANG_MAP, BANG_NEON -> true;
            default -> false;
        };
    }

    /**
     * Dessine une image de l'animation.
     *
     * @param t   l'avancement, de 0 à 1
     * @param age les secondes écoulées depuis le début
     */
    static void draw(Skin skin, GraphicsContext g, double w, double h, double cx, double cy, double far, double t, double age) {
        switch (skin) {
            case DARK_SNOW -> snow(g, w, h, cx, cy, far, t, age);
            case DARK_HOURGLASS -> hourglass(g, w, h, cx, cy, t);
            case BANG_INK -> ink(g, w, h, cx, cy, far, t);
            case BANG_STAINED -> stained(g, w, h, cx, cy, far, t);
            case BANG_ABYSS -> abyss(g, w, h, cx, cy, t, age);
            case BANG_ORIGAMI -> origami(g, w, h, cx, cy, far, t, age);
            case BANG_MAP -> map(g, w, h, cx, cy, t, age);
            default -> neon(g, w, h, cx, cy, t, age);
        }
        g.setGlobalAlpha(1);
    }

    /** Un nombre entre 0 et 1, toujours le même pour une même graine. */
    private static double fraction(double seed) {
        double value = Math.sin(seed * 12.9898 + 78.233) * 43758.5453;
        return value - Math.floor(value);
    }

    private static void snow(GraphicsContext g, double w, double h, double cx, double cy, double far, double t, double age) {
        double on = BurstView.envelope(t, 0.15, 0.8);
        BurstView.veil(g, w, h, Color.web("#0a1424"), 0.6 * on);
        g.setStroke(Color.web("#f4f8ff"));
        for (int i = 0; i < 260; i++) {
            // Chaque flocon part du bord et tourbillonne vers le centre, de plus en plus vite.
            double phase = (fraction(i * 4.7) + t * 1.6) % 1;
            double a = fraction(i * 1.3) * 2 * Math.PI + phase * 5 + age * 0.4;
            double r = far * 1.1 * (1 - phase) + far * 0.1 * Math.sin(age * 3 + i);
            double x = cx + r * Math.cos(a), y = cy + r * Math.sin(a);
            double size = (2 + 4 * fraction(i * 6.1)) * (1 + far * 0.0015);
            double spin = age * (1 + fraction(i)) + i;
            g.setLineWidth(1.2);
            g.setGlobalAlpha(on * (0.4 + 0.6 * fraction(i * 7.7)) * Math.sin(Math.PI * phase));
            for (int k = 0; k < 3; k++) {
                double b = spin + Math.PI * k / 3;
                g.strokeLine(x - size * Math.cos(b), y - size * Math.sin(b), x + size * Math.cos(b), y + size * Math.sin(b));
            }
        }
        // Le blanc total au cœur de la tempête, qui se dissipe.
        double white = t < 0.6 ? Math.max(0, (t - 0.35) / 0.25) : Math.max(0, 1 - (t - 0.6) / 0.4);
        BurstView.glow(g, cx, cy, far * (0.3 + 0.9 * t), Color.WHITE, white);
        BurstView.veil(g, w, h, Color.WHITE, 0.8 * white * white);
    }

    private static void hourglass(GraphicsContext g, double w, double h, double cx, double cy, double t) {
        double on = BurstView.envelope(t, 0.1, 0.85);
        BurstView.veil(g, w, h, Color.web("#1a1208"), 0.75 * on);
        double tall = h * 0.42, wide = Math.min(w, h) * 0.25;
        // Il se retourne à la fin : tout le dessin pivote.
        double flip = t > 0.7 ? Math.PI * Math.min(1, (t - 0.7) / 0.3) : 0;
        g.save();
        g.translate(cx, cy);
        g.rotate(Math.toDegrees(flip));
        g.translate(-cx, -cy);
        g.setStroke(Color.web("#e8c87a"));
        g.setLineWidth(Math.max(2, h * 0.004));
        g.setGlobalAlpha(0.6 * on);
        g.strokePolyline(new double[] {cx - wide, cx - wide * 0.08, cx - wide, cx + wide, cx + wide * 0.08, cx + wide, cx - wide},
                new double[] {cy - tall, cy, cy + tall, cy + tall, cy, cy - tall, cy - tall}, 7);
        g.setLineWidth(Math.max(5, h * 0.012));
        g.strokeLine(cx - wide * 1.15, cy - tall, cx + wide * 1.15, cy - tall);
        g.strokeLine(cx - wide * 1.15, cy + tall, cx + wide * 1.15, cy + tall);
        // Le sable du haut qui baisse, celui du bas qui monte, et le filet qui coule entre eux.
        double poured = Math.min(1, t / 0.7);
        g.setFill(Color.web("#e8c87a"));
        g.setGlobalAlpha(0.8 * on);
        double topLevel = tall * (1 - poured);
        if (topLevel > 1) {
            double half = wide * (0.08 + 0.92 * topLevel / tall);
            g.fillPolygon(new double[] {cx - half, cx + half, cx + wide * 0.08, cx - wide * 0.08},
                    new double[] {cy - topLevel, cy - topLevel, cy, cy}, 4);
        }
        double bottom = tall * poured;
        if (bottom > 1) {
            double half = wide * (0.08 + 0.92 * bottom / tall);
            g.fillPolygon(new double[] {cx - wide * 0.3, cx + wide * 0.3, cx + half, cx - half},
                    new double[] {cy + tall - bottom, cy + tall - bottom, cy + tall, cy + tall}, 4);
        }
        if (poured < 1) {
            g.setGlobalAlpha(0.9 * on);
            g.fillRect(cx - wide * 0.03, cy, wide * 0.06, tall - bottom);
            for (int i = 0; i < 40; i++) {
                double y = cy + ((fraction(i) + t * 6) % 1) * (tall - bottom);
                double x = cx + (fraction(i * 3.1) - 0.5) * wide * 0.1 * (y - cy) / tall;
                g.fillOval(x - 1.5, y - 1.5, 3, 3);
            }
        }
        g.restore();
        BurstView.glow(g, cx, cy, wide * 0.6, Color.web("#ffd27f"), 0.5 * on);
    }

    private static void ink(GraphicsContext g, double w, double h, double cx, double cy, double far, double t) {
        double on = BurstView.envelope(t, 0.1, 0.8);
        BurstView.veil(g, w, h, PAPER, 0.92 * on);
        // La goutte tombe pendant le premier dixième, puis s'étale en lavis au bord irrégulier.
        if (t < 0.1) {
            double y = cy * (t / 0.1);
            g.setGlobalAlpha(on);
            g.setFill(INK);
            g.fillOval(cx - far * 0.015, y - far * 0.02, far * 0.03, far * 0.04);
            return;
        }
        double u = (t - 0.1) / 0.9;
        double spread = far * (0.1 + 0.9 * Math.sqrt(u));
        int steps = 90;
        double[] xs = new double[steps], ys = new double[steps];
        for (int pass = 0; pass < 3; pass++) {
            double share = 1 - 0.3 * pass;
            for (int k = 0; k < steps; k++) {
                double a = 2 * Math.PI * k / steps;
                double rr = spread * share * (1 + 0.12 * Math.sin(a * 7 + pass) + 0.06 * Math.sin(a * 17 + u * 5));
                xs[k] = cx + rr * Math.cos(a);
                ys[k] = cy + rr * Math.sin(a);
            }
            g.setGlobalAlpha(on * (0.25 + 0.25 * pass) * (1 - 0.6 * u));
            g.setFill(INK);
            g.fillPolygon(xs, ys, steps);
        }
        // Les éclaboussures, projetées loin de la goutte.
        g.setFill(INK);
        for (int k = 0; k < 70; k++) {
            double a = fraction(k * 1.7) * 2 * Math.PI;
            double d = spread * (1.05 + 0.6 * fraction(k * 3.1)) * Math.min(1, u * 2);
            double size = far * (0.002 + 0.008 * fraction(k * 5.3));
            g.setGlobalAlpha(on * (0.4 + 0.5 * fraction(k * 7.9)) * (1 - 0.5 * u));
            g.fillOval(cx + d * Math.cos(a) - size, cy + d * Math.sin(a) * 0.85 - size, 2 * size, 2 * size);
        }
    }

    private static void stained(GraphicsContext g, double w, double h, double cx, double cy, double far, double t) {
        double on = BurstView.envelope(t, 0.1, 0.85);
        BurstView.veil(g, w, h, Color.BLACK, 0.9 * on);
        // La rosace : trois couronnes de verres, allumés l'un après l'autre à partir du centre.
        int rings = 3;
        for (int ring = 0; ring < rings; ring++) {
            int panes = 8 + 6 * ring;
            double inner = far * (0.08 + 0.3 * ring), outer = far * (0.08 + 0.3 * (ring + 1));
            for (int k = 0; k < panes; k++) {
                double lit = Math.max(0, Math.min(1, (t * 2.2 - ring * 0.4 - k / (double) panes * 0.3) * 3));
                if (lit <= 0) continue;
                double a0 = 2 * Math.PI * k / panes + ring * 0.2, a1 = 2 * Math.PI * (k + 1) / panes + ring * 0.2;
                double[] xs = {cx + inner * Math.cos(a0), cx + outer * Math.cos(a0), cx + outer * Math.cos(a1), cx + inner * Math.cos(a1)};
                double[] ys = {cy + inner * Math.sin(a0), cy + outer * Math.sin(a0), cy + outer * Math.sin(a1), cy + inner * Math.sin(a1)};
                g.setGlobalAlpha(on * (0.35 + 0.5 * lit));
                g.setFill(GLASS[(k + ring) % GLASS.length]);
                g.fillPolygon(xs, ys, 4);
                g.setStroke(Color.web("#2a2a2e"));
                g.setLineWidth(Math.max(2, far * 0.008));
                g.setGlobalAlpha(on);
                g.strokePolygon(xs, ys, 4);
            }
        }
        // Le cœur de la rosace, blanc or, et la lumière qui traverse tout à la fin.
        BurstView.glow(g, cx, cy, far * 0.12, Color.web("#fff1c0"), on);
        BurstView.veil(g, w, h, Color.web("#fff1c0"), 0.5 * Math.max(0, Math.sin(Math.PI * Math.max(0, (t - 0.6) / 0.4))));
    }

    private static void abyss(GraphicsContext g, double w, double h, double cx, double cy, double t, double age) {
        double on = BurstView.envelope(t, 0.15, 0.8);
        BurstView.veil(g, w, h, Color.web("#010614"), 0.85 * on);
        double small = Math.min(w, h);
        // Le plancton : des points qui montent et s'allument au passage de la méduse.
        for (int k = 0; k < 220; k++) {
            double x = fraction(k * 1.3) * w, y = ((fraction(k * 4.7) - age * 0.05 * (1 + fraction(k))) % 1 + 1) % 1 * h;
            double d = Math.hypot(x - cx, y - cy) / small;
            double lit = Math.max(0.2, 1 - Math.abs(d - t * 1.4) * 3);
            double size = 1.5 + 2.5 * fraction(k * 6.1);
            g.setGlobalAlpha(on * lit * (0.5 + 0.5 * Math.sin(age * 3 + k)));
            g.setFill(k % 3 == 0 ? Color.web("#6bffd0") : Color.web("#5ad0ff"));
            g.fillOval(x - size / 2, y - size / 2, size, size);
        }
        // La méduse : une ombrelle qui pulse et grandit, des tentacules qui ondulent dessous.
        double pulse = 1 + 0.08 * Math.sin(age * 4);
        double dome = small * (0.08 + 0.3 * t) * pulse;
        BurstView.glow(g, cx, cy, dome * 1.8, Color.web("#8a6bff"), 0.5 * on);
        g.setGlobalAlpha(0.55 * on);
        g.setFill(Color.web("#b08aff"));
        g.fillArc(cx - dome, cy - dome * 0.8, 2 * dome, 1.6 * dome, 0, 180, javafx.scene.shape.ArcType.ROUND);
        g.setStroke(Color.web("#dcd0ff"));
        g.setLineWidth(Math.max(1.5, dome * 0.03));
        g.setGlobalAlpha(0.7 * on);
        for (int k = 0; k < 9; k++) {
            double x0 = cx - dome * 0.8 + dome * 1.6 * k / 8;
            int points = 10;
            double[] xs = new double[points], ys = new double[points];
            for (int p = 0; p < points; p++) {
                double u = p / (double) (points - 1);
                xs[p] = x0 + dome * 0.15 * Math.sin(age * 3 + k + u * 5) * u;
                ys[p] = cy + dome * 1.6 * u;
            }
            g.strokePolyline(xs, ys, points);
        }
    }

    private static void origami(GraphicsContext g, double w, double h, double cx, double cy, double far, double t, double age) {
        double on = BurstView.envelope(t, 0.1, 0.8);
        BurstView.veil(g, w, h, Color.web("#141824"), 0.9 * on);
        // La feuille se déplie : des facettes triangulaires de plus en plus nombreuses à partir du centre.
        int facets = 4 + (int) (12 * Math.min(1, t * 1.5));
        double reach = far * Math.min(1, t * 1.4);
        for (int k = 0; k < facets; k++) {
            double a0 = 2 * Math.PI * k / facets, a1 = 2 * Math.PI * (k + 1) / facets;
            double r0 = reach * (0.7 + 0.3 * fraction(k * 2.1)), r1 = reach * (0.7 + 0.3 * fraction(k * 2.1 + 1));
            double shade = 0.78 + 0.2 * fraction(k * 3.7);
            g.setGlobalAlpha(on);
            g.setFill(Color.color(shade * 0.36, shade * 0.42, shade * 0.60));
            g.fillPolygon(new double[] {cx, cx + r0 * Math.cos(a0), cx + r1 * Math.cos(a1)},
                    new double[] {cy, cy + r0 * Math.sin(a0), cy + r1 * Math.sin(a1)}, 3);
            g.setStroke(Color.web("#5d6a8c"));
            g.setLineWidth(Math.max(1, far * 0.002));
            g.setGlobalAlpha(0.6 * on);
            g.strokeLine(cx, cy, cx + r0 * Math.cos(a0), cy + r0 * Math.sin(a0));
        }
        // Les confettis qui tombent de haut.
        for (int k = 0; k < 90; k++) {
            double x = fraction(k * 1.3) * w + w * 0.02 * Math.sin(age * 2 + k);
            double y = ((fraction(k * 4.7) + t * (0.6 + 0.6 * fraction(k * 2.9))) % 1) * h;
            double size = far * (0.006 + 0.008 * fraction(k * 6.1));
            double spin = age * (2 + fraction(k)) + k;
            g.setGlobalAlpha(0.85 * on);
            g.setFill(GLASS[k % GLASS.length]);
            g.fillPolygon(new double[] {x + size * Math.cos(spin), x + size * Math.cos(spin + 1.6), x + size * Math.cos(spin + 3.1), x + size * Math.cos(spin + 4.7)},
                    new double[] {y + size * 0.6 * Math.sin(spin), y + size * 0.6 * Math.sin(spin + 1.6), y + size * 0.6 * Math.sin(spin + 3.1), y + size * 0.6 * Math.sin(spin + 4.7)}, 4);
        }
    }

    private static void map(GraphicsContext g, double w, double h, double cx, double cy, double t, double age) {
        double on = BurstView.envelope(t, 0.1, 0.8);
        BurstView.veil(g, w, h, PARCHMENT, 0.92 * on);
        double small = Math.min(w, h);
        // Les constellations : des étoiles reliées à leur voisine, tracées l'une après l'autre.
        int stars = 60;
        double[] xs = new double[stars], ys = new double[stars];
        for (int k = 0; k < stars; k++) {
            xs[k] = fraction(k * 1.3) * w;
            ys[k] = fraction(k * 4.7) * h;
        }
        g.setStroke(SEPIA);
        g.setLineWidth(1);
        g.setFill(SEPIA);
        for (int k = 0; k < stars; k++) {
            double shown = Math.max(0, Math.min(1, (t * 1.3 - k / (double) stars) * 6));
            if (shown <= 0) continue;
            g.setGlobalAlpha(on * shown);
            g.fillOval(xs[k] - 2, ys[k] - 2, 4, 4);
            int next = (k + 1) % stars;
            if (Math.hypot(xs[next] - xs[k], ys[next] - ys[k]) < small * 0.5) {
                g.strokeLine(xs[k], ys[k], xs[k] + (xs[next] - xs[k]) * shown, ys[k] + (ys[next] - ys[k]) * shown);
            }
        }
        // La rose des vents : seize branches qui tournent puis s'arrêtent, de plus en plus grande.
        double size = small * (0.05 + 0.3 * Math.min(1, t * 1.5));
        double turn = Math.PI * 2 * (1 - Math.pow(1 - Math.min(1, t * 1.2), 3));
        for (int k = 0; k < 16; k++) {
            double a = turn + Math.PI * k / 8;
            double len = k % 4 == 0 ? size : k % 2 == 0 ? size * 0.65 : size * 0.4;
            g.setGlobalAlpha(on);
            g.setFill(k % 2 == 0 ? SEPIA : Color.web("#b02a2a"));
            g.fillPolygon(new double[] {cx, cx + len * Math.cos(a), cx + len * 0.18 * Math.cos(a + 0.4)},
                    new double[] {cy, cy + len * Math.sin(a), cy + len * 0.18 * Math.sin(a + 0.4)}, 3);
            g.setFill(k % 2 == 0 ? Color.web("#8a6a3a") : Color.web("#7a2020"));
            g.fillPolygon(new double[] {cx, cx + len * Math.cos(a), cx + len * 0.18 * Math.cos(a - 0.4)},
                    new double[] {cy, cy + len * Math.sin(a), cy + len * 0.18 * Math.sin(a - 0.4)}, 3);
        }
        g.setStroke(SEPIA);
        g.setLineWidth(Math.max(1.5, size * 0.02));
        g.setGlobalAlpha(on);
        g.strokeOval(cx - size * 1.1, cy - size * 1.1, size * 2.2, size * 2.2);
        g.setGlobalAlpha(0.3 * on * (0.5 + 0.5 * Math.sin(age * 2)));
        g.strokeOval(cx - size * 1.25, cy - size * 1.25, size * 2.5, size * 2.5);
    }

    private static void neon(GraphicsContext g, double w, double h, double cx, double cy, double t, double age) {
        double on = BurstView.envelope(t, 0.1, 0.85);
        BurstView.veil(g, w, h, Color.web("#0f1114"), 0.9 * on);
        double small = Math.min(w, h);
        // Les fenêtres des immeubles, qui s'éclairent de proche en proche.
        int columns = 24, rows = 14;
        for (int i = 0; i < columns; i++) {
            for (int j = 0; j < rows; j++) {
                double order = fraction(i * 7.1 + j * 3.3);
                if (order > t * 1.6) continue;
                double x = w * (i + 0.2) / columns, y = h * (j + 0.2) / rows;
                g.setGlobalAlpha(on * (0.4 + 0.5 * fraction(i + j * 11)));
                g.setFill(fraction(i * 3 + j) > 0.8 ? Color.web("#9fd0ff") : Color.web("#ffd27f"));
                g.fillRect(x, y, w / columns * 0.4, h / rows * 0.45);
            }
        }
        // Les reflets sur l'asphalte mouillé : des traînées verticales de la couleur de l'enseigne.
        Color pink = Color.web("#ff4fd8"), cyan = Color.web("#3ff0ff");
        for (int k = 0; k < 30; k++) {
            double x = fraction(k * 1.9) * w;
            g.setGlobalAlpha(0.12 * on);
            g.setFill(k % 2 == 0 ? pink : cyan);
            g.fillRect(x, h * 0.75, w * 0.01, h * 0.25);
        }
        // L'enseigne : elle grésille, s'allume franchement, puis s'éteint.
        boolean flicker = t < 0.35 ? fraction(Math.floor(age * 18)) > 0.45 : t < 0.8 || fraction(Math.floor(age * 12)) > 0.3;
        double lit = flicker ? on : 0.15 * on;
        double radius = small * 0.3;
        BurstView.glow(g, cx, cy, radius * 1.6, pink, 0.5 * lit);
        g.setStroke(pink);
        g.setLineWidth(Math.max(3, small * 0.012));
        g.setGlobalAlpha(lit);
        g.strokeRoundRect(cx - radius * 1.4, cy - radius * 0.6, radius * 2.8, radius * 1.2, radius * 0.3, radius * 0.3);
        g.setStroke(cyan);
        g.setLineWidth(Math.max(2, small * 0.006));
        g.strokeRoundRect(cx - radius * 1.3, cy - radius * 0.5, radius * 2.6, radius, radius * 0.25, radius * 0.25);
        g.setFill(Color.web("#fff6fb"));
        g.setFont(Font.font("Arial", javafx.scene.text.FontWeight.BOLD, radius * 0.45));
        g.setTextAlign(TextAlignment.CENTER);
        g.fillText("BIG BANG", cx, cy + radius * 0.16);
        g.setTextAlign(TextAlignment.LEFT);
    }
}
