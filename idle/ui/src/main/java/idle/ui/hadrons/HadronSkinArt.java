package idle.ui.hadrons;

import idle.core.universe.Skin;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;

/**
 * Les skins de la liaison des hadrons (version 0.195), dessinés à côté de {@link BindingView} pour la garder lisible.
 * <ul>
 * <li>Les quarks : « Billes de verre » (une sphère translucide, son reflet et son ombre), « Fruits » (la saveur en
 * couleur de fruit, une feuille et une queue ; l'antiquark en tranche), « Notes de musique » (une noire de la couleur
 * de la saveur ; pendant la liaison, un accord de petites notes monte du hadron qui naît), « Puzzle » (une pièce à
 * tenon, l'antiquark à mortaise) ;</li>
 * <li>les gluons : « Ressorts » (une spirale de métal serrée), « Élastiques » (deux brins rouges tendus qui vibrent),
 * « Laine » (un fil beige duveteux qui boucle), « Éclairs » (un zigzag blanc bordé de bleu) ;</li>
 * <li>le fond : « Tableau noir » (vert sombre, des traits de craie à demi effacés et un cadre de bois), « Table de
 * laboratoire » (un plan gris carrelé, des ronds de verre et une règle), « Ciel d'orage » (des nuées grises qui
 * roulent et un éclair de temps en temps).</li>
 * </ul>
 */
final class HadronSkinArt {

    private static final Color CHALK = Color.web("#e8e4d8");
    private static final Color BOARD = Color.web("#1e3a2a");
    private static final Color FRAME = Color.web("#8a5a2a");
    private static final Color BENCH = Color.web("#4a5058");
    private static final Color BENCH_LINE = Color.web("#5e656e");
    private static final Color STORM = Color.web("#1a2130");
    private static final Color CLOUD = Color.web("#3a4458");
    private static final Color CLOUD_LIGHT = Color.web("#5a6478");
    private static final Color METAL = Color.web("#c8ccd4");
    private static final Color METAL_DARK = Color.web("#6a7080");
    private static final Color RUBBER = Color.web("#d04a3a");
    private static final Color WOOL = Color.web("#e8d8b8");
    private static final Color BOLT = Color.web("#9ad0ff");
    private static final Color LEAF = Color.web("#4caf50");
    private static final Color STEM = Color.web("#6a4a2a");

    private HadronSkinArt() {
    }

    static boolean drawsQuarks(Skin quarks) {
        return quarks == Skin.QUARKS_MARBLES || quarks == Skin.QUARKS_FRUITS || quarks == Skin.QUARKS_NOTES || quarks == Skin.QUARKS_PUZZLE;
    }

    static boolean drawsGluons(Skin gluons) {
        return gluons == Skin.GLUONS_SPRINGS || gluons == Skin.GLUONS_RUBBER || gluons == Skin.GLUONS_WOOL || gluons == Skin.GLUONS_LIGHTNING;
    }

    static boolean drawsBackground(Skin background) {
        return background == Skin.HADRON_BACK_BLACKBOARD || background == Skin.HADRON_BACK_LAB || background == Skin.HADRON_BACK_STORM;
    }

    // ------------------------------------------------------------------
    // Le fond
    // ------------------------------------------------------------------

    /** Le fond de la vue, sous le champ des quarks ({@code fieldH} de haut) et la rangée du bas. */
    static void background(GraphicsContext g, Skin background, double w, double h, double fieldH, double time) {
        switch (background) {
            case HADRON_BACK_BLACKBOARD -> blackboard(g, w, h, fieldH);
            case HADRON_BACK_LAB -> bench(g, w, h, fieldH);
            default -> storm(g, w, h, time);
        }
    }

    /** Le tableau noir : vert sombre, des traces de craie effacée, quelques traits et formules, le cadre de bois. */
    private static void blackboard(GraphicsContext g, double w, double h, double fieldH) {
        g.setFill(BOARD);
        g.fillRect(0, 0, w, h);
        g.setGlobalAlpha(0.08);
        g.setFill(CHALK);
        for (int i = 0; i < 12; i++) {
            double x = (i * 0.754877666 % 1) * w, y = (i * 0.569840291 % 1) * fieldH;
            g.fillOval(x - 40, y - 10, 80, 20);
        }
        g.setGlobalAlpha(0.35);
        g.setStroke(CHALK);
        g.setLineWidth(1.2);
        g.strokeLine(w * 0.08, fieldH * 0.2, w * 0.2, fieldH * 0.2);
        g.strokeOval(w * 0.82, fieldH * 0.15, 26, 26);
        g.strokeLine(w * 0.82 + 13, fieldH * 0.15, w * 0.82 + 13, fieldH * 0.15 + 26);
        g.strokeLine(w * 0.1, fieldH * 0.78, w * 0.1 + 30, fieldH * 0.78 - 18);
        g.strokeLine(w * 0.1 + 30, fieldH * 0.78 - 18, w * 0.1 + 60, fieldH * 0.78);
        g.setGlobalAlpha(1);
        g.setFill(FRAME);
        g.fillRect(0, 0, w, 5);
        g.fillRect(0, 0, 5, h);
        g.fillRect(w - 5, 0, 5, h);
        g.fillRect(0, h - 5, w, 5);
    }

    /** La table de laboratoire : un plan gris carrelé, des ronds laissés par des verres, une règle au bord. */
    private static void bench(GraphicsContext g, double w, double h, double fieldH) {
        g.setFill(BENCH);
        g.fillRect(0, 0, w, h);
        g.setStroke(BENCH_LINE);
        g.setLineWidth(1);
        for (double x = 0; x < w; x += 60) g.strokeLine(x, 0, x, h);
        for (double y = 0; y < h; y += 60) g.strokeLine(0, y, w, y);
        g.setStroke(Color.color(1, 1, 1, 0.12));
        g.setLineWidth(2);
        g.strokeOval(w * 0.15 - 14, fieldH * 0.3 - 14, 28, 28);
        g.strokeOval(w * 0.7 - 20, fieldH * 0.65 - 20, 40, 40);
        g.strokeOval(w * 0.85 - 10, fieldH * 0.2 - 10, 20, 20);
        g.setFill(Color.web("#e8d8a0"));
        g.fillRect(12, fieldH - 14, Math.min(220, w - 24), 9);
        g.setStroke(Color.web("#4a3a1a"));
        g.setLineWidth(1);
        for (int k = 0; k <= 20 && 12 + k * 11 < w - 12; k++) g.strokeLine(12 + k * 11, fieldH - 14, 12 + k * 11, fieldH - 14 + (k % 5 == 0 ? 7 : 4));
    }

    /** Le ciel d'orage : du gris-bleu, des nuées qui roulent lentement, un éclair toutes les quelques secondes. */
    private static void storm(GraphicsContext g, double w, double h, double time) {
        g.setFill(new LinearGradient(0, 0, 0, h, false, CycleMethod.NO_CYCLE, new Stop(0, STORM), new Stop(1, Color.web("#2a3348"))));
        g.fillRect(0, 0, w, h);
        for (int i = 0; i < 9; i++) {
            double x = ((i * 0.754877666 + time * 0.012 * (1 + i % 3)) % 1) * w, y = (0.05 + 0.5 * (i * 0.569840291 % 1)) * h;
            double r = 40 + 30 * (i % 4);
            g.setFill(new RadialGradient(0, 0, x, y, r, false, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.color(CLOUD_LIGHT.getRed(), CLOUD_LIGHT.getGreen(), CLOUD_LIGHT.getBlue(), 0.6)),
                    new Stop(0.6, Color.color(CLOUD.getRed(), CLOUD.getGreen(), CLOUD.getBlue(), 0.5)), new Stop(1, Color.TRANSPARENT)));
            g.fillOval(x - r * 1.6, y - r * 0.7, r * 3.2, r * 1.4);
        }
        // L'éclair : un dixième de seconde toutes les cinq secondes, le ciel blanchit et un zigzag tombe.
        double cycle = time % 5;
        if (cycle < 0.12) {
            g.setFill(Color.color(1, 1, 1, 0.18));
            g.fillRect(0, 0, w, h);
            double x = w * (0.2 + 0.6 * (Math.floor(time / 5) * 0.618 % 1));
            g.setStroke(Color.WHITE);
            g.setLineWidth(2);
            g.strokePolyline(new double[] {x, x - 8, x + 6, x - 4}, new double[] {0, h * 0.25, h * 0.45, h * 0.7}, 4);
        }
    }

    // ------------------------------------------------------------------
    // Les quarks
    // ------------------------------------------------------------------

    /**
     * Un quark de rayon {@code r} : une bille, un fruit, une note ou une pièce de puzzle, de la couleur de sa saveur ;
     * {@code anti} vrai pour un antiquark, dessiné en creux (anneau, tranche, note blanche, mortaise).
     */
    static void quark(GraphicsContext g, Skin quarks, double x, double y, double r, Color color, boolean anti, double alpha, double time) {
        if (alpha <= 0) return;
        switch (quarks) {
            case QUARKS_MARBLES -> marble(g, x, y, r, color, anti, alpha);
            case QUARKS_FRUITS -> fruit(g, x, y, r, color, anti, alpha);
            case QUARKS_NOTES -> note(g, x, y, r, color, anti, alpha);
            default -> puzzle(g, x, y, r, color, anti, alpha, time);
        }
    }

    private static Color tint(Color color, double alpha) {
        return Color.color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, Math.min(1, alpha)));
    }

    /** Une bille de verre : une sphère translucide, le reflet en haut à gauche, l'ombre dessous ; l'antiquark en bille creuse. */
    private static void marble(GraphicsContext g, double x, double y, double r, Color color, boolean anti, double alpha) {
        double R = r * 1.3;
        g.setFill(tint(Color.BLACK, 0.3 * alpha));
        g.fillOval(x - R * 0.8, y + R * 0.7, R * 1.6, R * 0.5);
        g.setFill(new RadialGradient(0, 0, x - R * 0.3, y - R * 0.3, R * 1.3, false, CycleMethod.NO_CYCLE,
                new Stop(0, tint(color.brighter(), (anti ? 0.35 : 0.95) * alpha)), new Stop(1, tint(color.darker(), (anti ? 0.5 : 0.9) * alpha))));
        g.fillOval(x - R, y - R, R * 2, R * 2);
        if (anti) {
            g.setStroke(tint(color, alpha));
            g.setLineWidth(1.5);
            g.strokeOval(x - R, y - R, R * 2, R * 2);
        }
        g.setFill(tint(Color.WHITE, 0.8 * alpha));
        g.fillOval(x - R * 0.55, y - R * 0.65, R * 0.5, R * 0.35);
    }

    /** Un fruit : une boule de la couleur de la saveur, une queue et une feuille ; l'antiquark en tranche cerclée. */
    private static void fruit(GraphicsContext g, double x, double y, double r, Color color, boolean anti, double alpha) {
        double R = r * 1.3;
        if (anti) {
            g.setFill(tint(color.brighter(), 0.9 * alpha));
            g.fillOval(x - R, y - R, R * 2, R * 2);
            g.setStroke(tint(color.darker(), alpha));
            g.setLineWidth(2);
            g.strokeOval(x - R, y - R, R * 2, R * 2);
            g.setStroke(tint(Color.WHITE, 0.7 * alpha));
            g.setLineWidth(1);
            for (int k = 0; k < 4; k++) {
                double a = Math.PI * k / 4;
                g.strokeLine(x - R * 0.8 * Math.cos(a), y - R * 0.8 * Math.sin(a), x + R * 0.8 * Math.cos(a), y + R * 0.8 * Math.sin(a));
            }
            return;
        }
        g.setFill(tint(color, alpha));
        g.fillOval(x - R, y - R * 0.95, R * 2, R * 1.9);
        g.setFill(tint(Color.WHITE, 0.35 * alpha));
        g.fillOval(x - R * 0.6, y - R * 0.7, R * 0.5, R * 0.4);
        g.setStroke(tint(STEM, alpha));
        g.setLineWidth(1.3);
        g.strokeLine(x, y - R * 0.9, x + R * 0.3, y - R * 1.6);
        g.setFill(tint(LEAF, alpha));
        g.fillOval(x + R * 0.2, y - R * 1.7, R * 0.9, R * 0.45);
    }

    /** Une noire : la tête ovale penchée, la hampe à droite ; l'antiquark en blanche (la tête vide). */
    private static void note(GraphicsContext g, double x, double y, double r, Color color, boolean anti, double alpha) {
        double R = r * 1.2;
        g.save();
        g.translate(x, y + R * 0.6);
        g.rotate(-20);
        if (anti) {
            g.setStroke(tint(color, alpha));
            g.setLineWidth(1.8);
            g.strokeOval(-R, -R * 0.7, R * 2, R * 1.4);
        } else {
            g.setFill(tint(color, alpha));
            g.fillOval(-R, -R * 0.7, R * 2, R * 1.4);
        }
        g.restore();
        g.setStroke(tint(color, alpha));
        g.setLineWidth(1.6);
        g.strokeLine(x + R * 0.9, y + R * 0.5, x + R * 0.9, y - R * 2.2);
    }

    /** Une pièce de puzzle : un carré à tenon en haut et à droite ; l'antiquark à mortaises. Elle tourne un peu en flottant. */
    private static void puzzle(GraphicsContext g, double x, double y, double r, Color color, boolean anti, double alpha, double time) {
        double s = r * 2.2, knob = s * 0.3;
        g.save();
        g.translate(x, y);
        g.rotate(8 * Math.sin(time * 0.7 + x * 0.01));
        g.setFill(tint(color, alpha));
        g.fillRoundRect(-s / 2, -s / 2, s, s, 2, 2);
        if (anti) {
            g.setFill(tint(Color.web("#0b0708"), alpha));
            g.fillOval(-knob / 2, -s / 2 - knob * 0.3, knob, knob);
            g.fillOval(s / 2 - knob * 0.7, -knob / 2, knob, knob);
        } else {
            g.fillOval(-knob / 2, -s / 2 - knob * 0.7, knob, knob);
            g.fillOval(s / 2 - knob * 0.3, -knob / 2, knob, knob);
        }
        g.setStroke(tint(Color.WHITE, 0.5 * alpha));
        g.setLineWidth(1);
        g.strokeRoundRect(-s / 2, -s / 2, s, s, 2, 2);
        g.restore();
    }

    /** L'accord joué par la liaison sous les notes de musique : trois petites notes qui montent du hadron, de force {@code shine}. */
    static void chord(GraphicsContext g, double cx, double cy, double shine, Color color) {
        for (int k = 0; k < 3; k++) {
            double x = cx - 14 + 14 * k, y = cy - 20 - 18 * shine - 4 * k;
            note(g, x, y, 3, color, k == 1, 0.9 * shine);
        }
    }

    // ------------------------------------------------------------------
    // Les gluons
    // ------------------------------------------------------------------

    /**
     * Un gluon d'un quark à l'autre : un ressort, un élastique, un fil de laine ou un éclair, à la place du trait ondulé
     * classique. {@code xs} et {@code ys} sont les tableaux de travail de la vue, de même taille.
     */
    static void gluon(GraphicsContext g, Skin gluons, double x1, double y1, double x2, double y2, double amplitude, double time, double[] xs, double[] ys) {
        double dx = x2 - x1, dy = y2 - y1, length = Math.hypot(dx, dy);
        if (length < 1) return;
        double nx = -dy / length, ny = dx / length;
        int n = xs.length;
        switch (gluons) {
            case GLUONS_SPRINGS -> {
                // La spirale : des aller-retour serrés, deux passes, l'ombre puis le métal.
                for (int i = 0; i < n; i++) {
                    double s = i / (double) (n - 1), side = i % 2 == 0 ? 1 : -1;
                    xs[i] = x1 + dx * s + nx * side * amplitude * 1.1;
                    ys[i] = y1 + dy * s + ny * side * amplitude * 1.1;
                }
                g.setStroke(METAL_DARK);
                g.setLineWidth(2.6);
                g.strokePolyline(xs, ys, n);
                g.setStroke(METAL);
                g.setLineWidth(1.2);
                g.strokePolyline(xs, ys, n);
            }
            case GLUONS_RUBBER -> {
                // Deux brins tendus, qui vibrent en sens contraires.
                g.setStroke(RUBBER);
                g.setLineWidth(2.2);
                for (int brin = -1; brin <= 1; brin += 2) {
                    for (int i = 0; i < n; i++) {
                        double s = i / (double) (n - 1);
                        double offset = brin * (2 + 0.6 * amplitude * Math.sin(time * 18 + brin)) * Math.sin(Math.PI * s);
                        xs[i] = x1 + dx * s + nx * offset;
                        ys[i] = y1 + dy * s + ny * offset;
                    }
                    g.strokePolyline(xs, ys, n);
                }
            }
            case GLUONS_WOOL -> {
                // Un fil épais qui boucle, et son duvet.
                for (int i = 0; i < n; i++) {
                    double s = i / (double) (n - 1);
                    double offset = amplitude * Math.sin(s * Math.PI * 4 + time * 3) * Math.sin(Math.PI * s);
                    xs[i] = x1 + dx * s + nx * offset;
                    ys[i] = y1 + dy * s + ny * offset;
                }
                g.setStroke(tint(WOOL, 0.35));
                g.setLineWidth(6);
                g.strokePolyline(xs, ys, n);
                g.setStroke(WOOL);
                g.setLineWidth(2.4);
                g.strokePolyline(xs, ys, n);
                g.setFill(tint(WOOL, 0.6));
                for (int i = 1; i < n - 1; i += 3) g.fillOval(xs[i] - 1 + nx * 3, ys[i] - 1 + ny * 3, 2, 2);
            }
            default -> {
                // Un zigzag qui change trente fois par seconde, blanc bordé de bleu.
                long tick = (long) Math.floor(time * 30);
                for (int i = 0; i < n; i++) {
                    double s = i / (double) (n - 1);
                    double seed = Math.sin((tick * 17 + i) * 12.9898 + 78.233) * 43758.5453;
                    double offset = i == 0 || i == n - 1 ? 0 : (seed - Math.floor(seed) - 0.5) * 2 * amplitude;
                    xs[i] = x1 + dx * s + nx * offset;
                    ys[i] = y1 + dy * s + ny * offset;
                }
                g.setStroke(tint(BOLT, 0.4));
                g.setLineWidth(3.5);
                g.strokePolyline(xs, ys, n);
                g.setStroke(Color.WHITE);
                g.setLineWidth(1.2);
                g.strokePolyline(xs, ys, n);
            }
        }
    }
}
