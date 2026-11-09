package idle.ui.universe;

import idle.core.universe.Skin;
import idle.ui.universe.SeedView.BubbleState;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Les bulles de l'arbre et leurs liens sous un skin (version 0.197, {@link idle.core.universe.SkinVisual#TREE_BUBBLES} et
 * {@link idle.core.universe.SkinVisual#TREE_BUBBLE_LINKS}). Les bulles deviennent des fruits (achetée : mûre et rouge ;
 * achetable ou trop chère : verte ; fermée : un bourgeon), des lanternes de papier (allumées une fois achetées), des
 * gouttes de rosée, des pierres précieuses taillées (brutes et grises tant qu'elles sont fermées) ou des graines (germées
 * une fois achetées). Les liens deviennent des branches de bois (feuillues entre deux bulles achetées), des fils de lumière
 * (des éclats y courent) ou des racines (de la sève y monte). Sans skin, {@link SeedView} dessine comme avant.
 */
final class TreeBubbleSkinArt {

    private TreeBubbleSkinArt() {
    }

    private static final Color RIPE = Color.web("#e8453c"), RIPE_LIGHT = Color.web("#ff9a6b"), GREEN = Color.web("#7cc45a"), GREEN_DARK = Color.web("#3f7a30");
    private static final Color BUD = Color.web("#6a5a3a"), BUD_LIGHT = Color.web("#9a8a5a"), STEM = Color.web("#4a3a22"), LEAF = Color.web("#4f9a4f");
    private static final Color PAPER = Color.web("#ff9b4a"), PAPER_LIT = Color.web("#ffd27f"), PAPER_DARK = Color.web("#5a3a2a"), RIB = Color.web("#8a3a1a");
    private static final Color DEW = Color.web("#9fe6ff"), DEW_DEEP = Color.web("#3a8fb8"), DEW_DULL = Color.web("#4a6a7a");
    private static final Color GEM = Color.web("#c77dff"), GEM_LIGHT = Color.web("#efd6ff"), GEM_DEEP = Color.web("#6a2aa8"), ROUGH = Color.web("#5a5f66");
    private static final Color SEED = Color.web("#8a6a3e"), SEED_LIGHT = Color.web("#c9a070"), SPROUT = Color.web("#8ee06a");
    private static final Color BRANCH = Color.web("#6e5236"), BRANCH_DARK = Color.web("#3e2c1c"), LIGHT = Color.web("#fff1c9"), SAP = Color.web("#9fffe0");

    /** Vrai pour un skin de ce visuel : les bulles se dessinent ici, le fond sombre et l'aura des racines restent à la vue. */
    static boolean bubbles(Skin skin) {
        return skin != null && skin.visual() == idle.core.universe.SkinVisual.TREE_BUBBLES;
    }

    static boolean links(Skin skin) {
        return skin != null && skin.visual() == idle.core.universe.SkinVisual.TREE_BUBBLE_LINKS;
    }

    /** Une bulle de rayon {@code r} en {@code x, y}, dans l'état {@code state} ; {@code pulse} bat de 0 à 1 pour celles qui s'achètent. */
    static void bubble(GraphicsContext g, double x, double y, double r, BubbleState state, double pulse, double time, int index, Skin skin) {
        boolean bought = state == BubbleState.BOUGHT, buy = state == BubbleState.BUY, closed = state == BubbleState.CLOSED;
        switch (skin) {
            case BUBBLES_FRUITS -> {
                if (closed) {
                    // Le bourgeon : une goutte fermée, brune, serrée dans ses écailles.
                    g.setGlobalAlpha(0.8);
                    g.setFill(BUD);
                    g.fillPolygon(new double[] {x, x + r * 0.6, x, x - r * 0.6}, new double[] {y - r * 0.9, y + r * 0.1, y + r * 0.7, y + r * 0.1}, 4);
                    g.setStroke(BUD_LIGHT);
                    g.setLineWidth(1.2);
                    g.strokeLine(x, y - r * 0.9, x, y + r * 0.7);
                } else {
                    Color body = bought ? RIPE : GREEN, light = bought ? RIPE_LIGHT : GREEN_DARK;
                    if (bought || buy) UniverseView.glow(g, x, y, r * (bought ? 2.2 : 1.8 + 0.5 * pulse), body, bought ? 0.45 : 0.2 + 0.25 * pulse);
                    g.setGlobalAlpha(bought ? 0.95 : buy ? 0.75 + 0.2 * pulse : 0.6);
                    g.setFill(body);
                    g.fillOval(x - r * 0.95, y - r * 0.85, r * 1.9, r * 1.8);
                    g.setFill(light);
                    g.setGlobalAlpha(bought ? 0.6 : 0.35);
                    g.fillOval(x - r * 0.55, y - r * 0.6, r * 0.5, r * 0.35);
                }
                // La queue et sa feuille, pour tous.
                g.setGlobalAlpha(closed ? 0.5 : 0.9);
                g.setStroke(STEM);
                g.setLineWidth(Math.max(1.5, r * 0.12));
                g.strokeLine(x, y - r * 0.85, x + r * 0.15, y - r * 1.25);
                g.setFill(LEAF);
                g.fillOval(x + r * 0.1, y - r * 1.35, r * 0.6, r * 0.3);
            }
            case BUBBLES_LANTERNS -> {
                double w = r * 1.6, h = r * 1.9;
                if (bought) UniverseView.glow(g, x, y, r * 2.6, PAPER_LIT, 0.55 + 0.1 * Math.sin(time * 3 + index));
                else if (buy) UniverseView.glow(g, x, y, r * 2, PAPER, 0.15 + 0.3 * pulse);
                g.setGlobalAlpha(closed ? 0.45 : 0.95);
                g.setFill(bought ? PAPER_LIT : buy ? PAPER.interpolate(PAPER_LIT, 0.5 * pulse) : closed ? PAPER_DARK : PAPER.darker());
                g.fillRoundRect(x - w / 2, y - h / 2, w, h, r, r);
                g.setStroke(RIB);
                g.setLineWidth(1);
                g.setGlobalAlpha(closed ? 0.3 : 0.6);
                for (int k = 1; k < 4; k++) g.strokeLine(x - w / 2 + 1, y - h / 2 + h * k / 4, x + w / 2 - 1, y - h / 2 + h * k / 4);
                g.setFill(PAPER_DARK);
                g.setGlobalAlpha(0.9);
                g.fillRect(x - w * 0.3, y - h / 2 - r * 0.2, w * 0.6, r * 0.2);
                g.fillRect(x - w * 0.3, y + h / 2, w * 0.6, r * 0.2);
                if (bought) {
                    g.setFill(Color.WHITE);
                    g.setGlobalAlpha(0.5);
                    g.fillOval(x - r * 0.25, y - r * 0.3, r * 0.5, r * 0.7);
                }
            }
            case BUBBLES_DEW -> {
                Color body = closed ? DEW_DULL : DEW;
                double rr = closed ? r * 0.7 : r;
                if (bought) UniverseView.glow(g, x, y, r * 2.2, DEW, 0.5);
                else if (buy) UniverseView.glow(g, x, y, r * (1.7 + 0.5 * pulse), DEW, 0.2 + 0.25 * pulse);
                g.setGlobalAlpha(bought ? 0.85 : buy ? 0.5 + 0.3 * pulse : closed ? 0.3 : 0.45);
                g.setFill(new javafx.scene.paint.RadialGradient(0, 0, x - rr * 0.3, y, rr * 1.2, false, javafx.scene.paint.CycleMethod.NO_CYCLE,
                        new javafx.scene.paint.Stop(0, Color.web("#f0fdff")), new javafx.scene.paint.Stop(0.6, body), new javafx.scene.paint.Stop(1, DEW_DEEP)));
                // La goutte : une pointe en haut, un rond en bas.
                g.fillOval(x - rr, y - rr * 0.6, 2 * rr, 2 * rr * 0.8);
                g.fillPolygon(new double[] {x - rr * 0.8, x, x + rr * 0.8}, new double[] {y - rr * 0.2, y - rr * 1.3, y - rr * 0.2}, 3);
                g.setFill(Color.WHITE);
                g.setGlobalAlpha(closed ? 0.25 : 0.7);
                g.fillOval(x - rr * 0.5, y - rr * 0.2, rr * 0.3, rr * 0.45);
            }
            case BUBBLES_GEMS -> {
                double[] xs = new double[6], ys = new double[6];
                for (int k = 0; k < 6; k++) {
                    double a = Math.PI / 6 + k * Math.PI / 3, rr = closed ? r * 0.85 : r;
                    xs[k] = x + Math.cos(a) * rr * (closed ? 0.9 + 0.2 * UniverseView.noise(index * 7 + k) : 1);
                    ys[k] = y + Math.sin(a) * rr * (closed ? 0.9 + 0.2 * UniverseView.noise(index * 7 + k + 3) : 1);
                }
                if (bought) UniverseView.glow(g, x, y, r * 2.4, GEM, 0.55);
                else if (buy) UniverseView.glow(g, x, y, r * (1.8 + 0.5 * pulse), GEM, 0.2 + 0.3 * pulse);
                g.setGlobalAlpha(closed ? 0.7 : bought ? 0.95 : 0.7);
                g.setFill(closed ? ROUGH : bought ? GEM : GEM_DEEP);
                g.fillPolygon(xs, ys, 6);
                if (!closed) {
                    // Les facettes : la table claire au milieu, et ses arêtes.
                    g.setFill(bought ? GEM_LIGHT : GEM);
                    g.setGlobalAlpha(bought ? 0.8 : 0.4 + 0.3 * pulse * (buy ? 1 : 0));
                    g.fillPolygon(new double[] {x - r * 0.45, x + r * 0.45, x + r * 0.3, x - r * 0.3}, new double[] {y - r * 0.1, y - r * 0.1, y + r * 0.4, y + r * 0.4}, 4);
                    g.setStroke(GEM_LIGHT);
                    g.setLineWidth(1);
                    g.setGlobalAlpha(0.6);
                    for (int k = 0; k < 6; k++) g.strokeLine(xs[k], ys[k], x + (xs[k] - x) * 0.4, y + (ys[k] - y) * 0.4);
                }
            }
            default -> {
                // Les graines : une amande brune ; achetée, elle a germé ; achetable, elle se fend.
                if (bought) UniverseView.glow(g, x, y, r * 2, SPROUT, 0.4);
                g.setGlobalAlpha(closed ? 0.4 : 0.95);
                g.setFill(closed ? SEED.darker() : SEED);
                g.fillOval(x - r * 0.7, y - r * 0.95, r * 1.4, r * 1.9);
                g.setFill(SEED_LIGHT);
                g.setGlobalAlpha(closed ? 0.3 : 0.6);
                g.fillOval(x - r * 0.4, y - r * 0.7, r * 0.35, r * 0.9);
                if (buy) {
                    g.setStroke(SPROUT);
                    g.setLineWidth(1.5);
                    g.setGlobalAlpha(0.5 + 0.5 * pulse);
                    g.strokeLine(x, y - r * 0.9, x + r * 0.1, y + r * 0.1);
                }
                if (bought) {
                    g.setStroke(SPROUT);
                    g.setLineWidth(Math.max(1.5, r * 0.14));
                    g.setGlobalAlpha(0.95);
                    g.strokeLine(x, y - r * 0.8, x + r * 0.2, y - r * 1.5);
                    g.setFill(SPROUT);
                    g.fillOval(x + r * 0.15, y - r * 1.75, r * 0.5, r * 0.3);
                    g.fillOval(x - r * 0.45, y - r * 1.6, r * 0.5, r * 0.3);
                }
            }
        }
        g.setGlobalAlpha(1);
    }

    /**
     * Un lien de bord à bord, de {@code x0, y0} à {@code x1, y1} (déjà reculés du rayon {@code r}) : {@code lit} entre deux
     * bulles achetées, {@code open} dès qu'une l'est.
     */
    static void link(GraphicsContext g, double x0, double y0, double x1, double y1, double r, boolean lit, boolean open, double time, int index, Skin skin) {
        double dx = x1 - x0, dy = y1 - y0, len = Math.max(1e-6, Math.hypot(dx, dy)), nx = -dy / len, ny = dx / len;
        switch (skin) {
            case BUBBLE_LINKS_BRANCHES -> {
                g.setGlobalAlpha(open ? 0.95 : 0.4);
                g.setStroke(BRANCH_DARK);
                g.setLineWidth(lit ? 5 : open ? 4 : 3);
                g.strokeLine(x0, y0, x1, y1);
                g.setStroke(BRANCH);
                g.setLineWidth(lit ? 2.5 : open ? 2 : 1.2);
                g.strokeLine(x0, y0, x1, y1);
                // Les brindilles, et des feuilles quand les deux bouts sont achetés.
                for (int k = 1; k <= 3; k++) {
                    double t = k / 4.0, px = x0 + dx * t, py = y0 + dy * t, side = k % 2 == 0 ? 1 : -1;
                    g.setStroke(BRANCH);
                    g.setLineWidth(1.2);
                    g.strokeLine(px, py, px + (nx * side + dx / len * 0.6) * r * 0.5, py + (ny * side + dy / len * 0.6) * r * 0.5);
                    if (lit) {
                        g.setFill(LEAF);
                        g.fillOval(px + nx * side * r * 0.45 - r * 0.18, py + ny * side * r * 0.45 - r * 0.12, r * 0.36, r * 0.24);
                    }
                }
            }
            case BUBBLE_LINKS_LIGHT -> {
                g.setStroke(LIGHT);
                g.setLineWidth(open ? 1.2 : 0.8);
                g.setGlobalAlpha(lit ? 0.9 : open ? 0.5 : 0.2);
                if (!open) g.setLineDashes(3, 6);
                g.strokeLine(x0, y0, x1, y1);
                g.setLineDashes();
                if (open) {
                    int count = lit ? 3 : 1;
                    for (int k = 0; k < count; k++) {
                        double t = ((time * 0.35 + k / (double) count + index * 0.17) % 1), px = x0 + dx * t, py = y0 + dy * t;
                        UniverseView.glow(g, px, py, r * 0.5, LIGHT, 0.6);
                        g.setFill(Color.WHITE);
                        g.setGlobalAlpha(0.9);
                        g.fillOval(px - 1.5, py - 1.5, 3, 3);
                    }
                }
            }
            default -> {
                // Les racines : une ligne qui ondule, épaisse au départ, fine au bout ; la sève y monte quand elles sont allumées.
                int n = 8;
                double[] xs = new double[n + 1], ys = new double[n + 1];
                for (int k = 0; k <= n; k++) {
                    double t = k / (double) n, wave = Math.sin(t * Math.PI * 2 + index) * r * 0.25 * (1 - Math.abs(2 * t - 1));
                    xs[k] = x0 + dx * t + nx * wave;
                    ys[k] = y0 + dy * t + ny * wave;
                }
                g.setGlobalAlpha(open ? 0.9 : 0.35);
                g.setStroke(BRANCH_DARK);
                g.setLineWidth(lit ? 4 : open ? 3 : 2);
                g.strokePolyline(xs, ys, n + 1);
                g.setStroke(SEED);
                g.setLineWidth(lit ? 1.8 : 1.2);
                g.strokePolyline(xs, ys, n + 1);
                if (lit) {
                    double t = (time * 0.4 + index * 0.23) % 1;
                    int k = (int) (t * n);
                    double f = t * n - k, px = xs[k] + (xs[Math.min(n, k + 1)] - xs[k]) * f, py = ys[k] + (ys[Math.min(n, k + 1)] - ys[k]) * f;
                    UniverseView.glow(g, px, py, r * 0.6, SAP, 0.5);
                    g.setFill(SAP);
                    g.setGlobalAlpha(0.9);
                    g.fillOval(px - 2, py - 2, 4, 4);
                }
            }
        }
        g.setGlobalAlpha(1);
    }
}
