package idle.ui.universe;

import idle.core.Game;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

/**
 * Le visuel d'une dimension, dans sa fenêtre (version 0.081) : une tentative, sans logique, pour
 * chacune des dix. De la ligne (un point qui va et vient) au point qui contient tout, en passant
 * par le plan, le cube qui tourne, ses instants mis bout à bout, les embranchements du temps,
 * tous les débuts, et d'autres lois.
 *
 * <p>La ligne a sa logique depuis la version 0.101 : la boule est celle du jeu ({@link Game#linePosition()}),
 * un trait à chaque bout et, avec l'amélioration, d'autres à égale distance entre les deux. Chaque trait
 * touché s'allume et laisse monter les éclats qu'il donne.
 */
public final class DimensionView extends Canvas {

    private static final Color BACK = Color.web("#05060e");
    private static final Color[] PALETTE = {
            Color.web("#9fd0ff"), Color.web("#ffd27f"), Color.web("#c9a6ff"), Color.web("#9be7a8"), Color.web("#ff9eb5"),
            Color.web("#7fe0d4"), Color.web("#ffb36b"), Color.web("#a8b8ff"), Color.web("#ff7ad9"), Color.web("#fff1c9")};

    /** Un trait qui vient d'être touché : son éclat et les éclats qui montent. */
    private static final class Touch {
        double at, age;
    }

    private final Game game;
    private final List<Touch> touches = new ArrayList<>();
    /** La route de la boule à l'image d'avant, pour voir les traits passés depuis. */
    private double lastPhase = Double.NaN;
    private Dimension dimension = Dimension.LINE;
    private double time;

    public DimensionView(Game game) {
        super(800, 480);
        this.game = game;
    }

    /** La dimension à montrer ; son animation repart du début. */
    public void show(Dimension dimension) {
        this.dimension = dimension;
        time = 0;
        touches.clear();
        lastPhase = Double.NaN;
    }

    /** Les traits allumés à l'instant : pour les vérifications. */
    public int touchesShown() {
        return touches.size();
    }

    public Dimension dimension() {
        return dimension;
    }

    public void frame(double elapsed, boolean moving) {
        if (moving) time += Math.min(elapsed, 0.1);
        if (dimension == Dimension.LINE) noteTouches(Math.min(elapsed, 0.1), moving);
        draw();
    }

    /** Les traits que la boule a touchés depuis l'image d'avant : chacun s'allume. */
    private void noteTouches(double elapsed, boolean moving) {
        for (Touch touch : touches) touch.age += elapsed;
        touches.removeIf(touch -> touch.age > 1.2);
        double phase = game.state().linePhase();
        if (!Double.isNaN(lastPhase) && moving) {
            double to = phase < lastPhase ? phase + 2 : phase;
            // Plus d'un aller depuis la dernière image (la fenêtre était fermée) : rien à rejouer.
            if (to - lastPhase < 1) {
                int gaps = game.lineTicks() - 1;
                long first = (long) Math.floor(lastPhase * gaps + 1e-9) + 1, last = (long) Math.floor(to * gaps + 1e-9);
                for (long m = first; m <= last; m++) {
                    double unfolded = (double) (m % (2L * gaps)) / gaps;
                    Touch touch = new Touch();
                    touch.at = unfolded <= 1 ? unfolded : 2 - unfolded;
                    touches.add(touch);
                }
            }
        }
        lastPhase = phase;
    }

    private Color color() {
        return PALETTE[dimension.ordinal()];
    }

    private void draw() {
        double w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        GraphicsContext g = getGraphicsContext2D();
        g.setGlobalAlpha(1);
        g.clearRect(0, 0, w, h);
        g.setFill(BACK);
        g.fillRect(0, 0, w, h);
        double cx = w / 2, cy = h / 2, u = Math.min(w, h);
        switch (dimension) {
            case LINE -> line(g, w, cx, cy, u);
            case PLANE -> plane(g, w, h, cx, cy, u);
            case VOLUME -> volume(g, cx, cy, u * 0.22, time * 0.6, 1);
            case DURATION -> duration(g, w, cx, cy, u);
            case BRANCHES -> branches(g, w * 0.1, cy, w * 0.8, u * 0.32, 6, 1);
            case POSSIBLES -> possibles(g, w, cx, cy, u);
            case BEGINNINGS -> beginnings(g, w, h, u);
            case WEAVE -> weave(g, cx, cy, u);
            case LAWS -> laws(g, cx, cy, u);
            case POINT -> point(g, w, h, cx, cy, u);
        }
        g.setGlobalAlpha(1);
    }

    // 1. La ligne (version 0.101) : la boule du jeu va et vient, un trait à chaque bout et d'autres à égale
    // distance ; chaque trait touché s'allume, et ses éclats montent.
    private void line(GraphicsContext g, double w, double cx, double cy, double u) {
        Color c = color();
        double x0 = w * 0.1, x1 = w * 0.9;
        g.setStroke(c);
        g.setLineWidth(2);
        g.setGlobalAlpha(0.35);
        g.strokeLine(x0, cy, x1, cy);
        int gaps = game.lineTicks() - 1;
        for (int k = 0; k <= gaps; k++) {
            double at = (double) k / gaps, x = x0 + (x1 - x0) * at;
            double lit = 0;
            for (Touch touch : touches) if (Math.abs(touch.at - at) < 1e-9) lit = Math.max(lit, 1 - touch.age / 0.6);
            boolean end = k == 0 || k == gaps;
            double half = (end ? 16 : 10) + 6 * Math.max(0, lit);
            g.setLineWidth(end ? 3 : 2);
            g.setStroke(lit > 0 ? Color.WHITE : c);
            g.setGlobalAlpha(0.55 + 0.45 * Math.max(0, lit));
            g.strokeLine(x, cy - half, x, cy + half);
            if (lit > 0) UniverseView.glow(g, x, cy, u * 0.035 * lit + 4, c, 0.8 * lit);
        }
        // Les éclats de chaque trait touché montent et s'effacent.
        g.setFont(Font.font(13));
        g.setTextAlign(TextAlignment.CENTER);
        String gain = "+" + idle.ui.common.Format.darkMatter(idle.core.math.BigNum.of(game.lineShardsPerTouch()));
        for (Touch touch : touches) {
            double x = x0 + (x1 - x0) * touch.at;
            g.setFill(Color.WHITE);
            g.setGlobalAlpha(Math.max(0, 1 - touch.age / 1.2));
            g.fillText(gain, x, cy - 26 - touch.age * 30);
        }
        // La boule, et sa traînée le long de la route qu'elle vient de faire.
        double phase = game.state().linePhase();
        for (int k = 14; k >= 1; k--) {
            double back = phase - k * 0.012;
            if (back < 0) back += 2;
            double pos = back <= 1 ? back : 2 - back;
            UniverseView.glow(g, x0 + (x1 - x0) * pos, cy, u * 0.03 * (1 - k / 16.0), c, 0.8 * (1 - k / 15.0));
        }
        double x = x0 + (x1 - x0) * game.linePosition();
        UniverseView.glow(g, x, cy, u * 0.06, c, 0.9);
        g.setGlobalAlpha(1);
        g.setFill(Color.WHITE);
        g.fillOval(x - 5, cy - 5, 10, 10);
    }

    // 2. Une grille vue en perspective, et un point qui y dessine une courbe.
    private void plane(GraphicsContext g, double w, double h, double cx, double cy, double u) {
        Color c = color();
        double top = h * 0.25, bottom = h * 0.9;
        g.setStroke(c);
        g.setLineWidth(1);
        for (int k = 0; k <= 12; k++) {
            double t = k / 12.0;
            double y = top + (bottom - top) * t * t;
            double half = w * (0.18 + 0.32 * t * t);
            g.setGlobalAlpha(0.12 + 0.25 * t);
            g.strokeLine(cx - half, y, cx + half, y);
        }
        for (int k = -8; k <= 8; k++) {
            g.setGlobalAlpha(0.2);
            g.strokeLine(cx + k * w * 0.0225, top, cx + k * w * 0.0625, bottom);
        }
        int steps = 90;
        double[] xs = new double[steps], ys = new double[steps];
        for (int k = 0; k < steps; k++) {
            double t = time - k * 0.02;
            double px = Math.sin(t * 1.1), py = 0.5 + 0.5 * Math.sin(t * 1.7 + 1);
            double y = top + (bottom - top) * py * py;
            double half = w * (0.18 + 0.32 * py * py);
            xs[k] = cx + px * half * 0.9;
            ys[k] = y;
        }
        g.setStroke(Color.WHITE);
        g.setLineWidth(2);
        g.setGlobalAlpha(0.7);
        g.strokePolyline(xs, ys, steps);
        UniverseView.glow(g, xs[0], ys[0], u * 0.04, c, 0.9);
    }

    // 3. Un cube qui tourne, et un plus petit dedans, dans l'autre sens.
    private void volume(GraphicsContext g, double cx, double cy, double size, double angle, double alpha) {
        cube(g, cx, cy, size, angle, angle * 0.7, color(), alpha);
        cube(g, cx, cy, size * 0.45, -angle * 1.3, angle * 0.5, Color.WHITE, alpha * 0.8);
    }

    private static void cube(GraphicsContext g, double cx, double cy, double size, double a, double b, Color c, double alpha) {
        double[][] p = new double[8][2];
        for (int k = 0; k < 8; k++) {
            double x = (k & 1) == 0 ? -1 : 1, y = (k & 2) == 0 ? -1 : 1, z = (k & 4) == 0 ? -1 : 1;
            double x1 = x * Math.cos(a) - z * Math.sin(a), z1 = x * Math.sin(a) + z * Math.cos(a);
            double y1 = y * Math.cos(b) - z1 * Math.sin(b), z2 = y * Math.sin(b) + z1 * Math.cos(b);
            double depth = 5 / (5 + z2);
            p[k][0] = cx + x1 * size * depth;
            p[k][1] = cy + y1 * size * depth;
        }
        g.setStroke(c);
        g.setLineWidth(1.5);
        g.setGlobalAlpha(0.8 * alpha);
        for (int k = 0; k < 8; k++) {
            for (int bit : new int[] {1, 2, 4}) {
                int j = k ^ bit;
                if (j > k) g.strokeLine(p[k][0], p[k][1], p[j][0], p[j][1]);
            }
        }
        g.setFill(c);
        for (double[] q : p) g.fillOval(q[0] - 2.5, q[1] - 2.5, 5, 5);
    }

    // 4. Le cube à chaque instant, mis bout à bout le long du temps ; « maintenant » avance.
    private void duration(GraphicsContext g, double w, double cx, double cy, double u) {
        int copies = 9;
        double now = (time * 0.15) % 1;
        g.setStroke(color());
        g.setLineWidth(1);
        g.setGlobalAlpha(0.3);
        g.strokeLine(w * 0.06, cy + u * 0.2, w * 0.94, cy + u * 0.2);
        for (int k = 0; k < copies; k++) {
            double t = k / (copies - 1.0);
            double x = w * (0.1 + 0.8 * t);
            double near = 1 - Math.min(1, Math.abs(t - now) * 4);
            cube(g, x, cy, u * 0.05 * (0.8 + 0.4 * near), t * 4, t * 2, color(), 0.25 + 0.75 * near);
        }
        double x = w * (0.1 + 0.8 * now);
        g.setStroke(Color.WHITE);
        g.setGlobalAlpha(0.8);
        g.strokeLine(x, cy - u * 0.15, x, cy + u * 0.22);
    }

    // 5. Un arbre qui pousse de gauche à droite : à chaque nœud, le temps se sépare.
    private void branches(GraphicsContext g, double x0, double y0, double length, double spread, int depth, double alpha) {
        double grow = Math.min(1, (time % 9) / 3.5);
        branch(g, x0, y0, length / depth, spread, depth, 0, grow, alpha, 1);
    }

    private void branch(GraphicsContext g, double x, double y, double step, double spread, int left, int level, double grow, double alpha, int seed) {
        if (left == 0) return;
        double shown = Math.max(0, Math.min(1, grow * 6 - level));
        if (shown <= 0) return;
        for (int side = -1; side <= 1; side += 2) {
            double dy = side * spread * (0.4 + 0.3 * UniverseView.noise(seed * 7 + side + 3));
            double tx = x + step * shown, ty = y + dy * shown;
            g.setStroke(PALETTE[(level + seed) % PALETTE.length]);
            g.setLineWidth(Math.max(0.6, 2.5 - level * 0.35));
            g.setGlobalAlpha(alpha * (0.9 - level * 0.08));
            g.strokeLine(x, y, tx, ty);
            if (shown >= 1) branch(g, tx, ty, step, spread * 0.55, left - 1, level + 1, grow, alpha, seed * 2 + (side > 0 ? 1 : 0));
        }
        g.setFill(Color.WHITE);
        g.setGlobalAlpha(alpha);
        g.fillOval(x - 2, y - 2, 4, 4);
    }

    // 6. Toutes les suites d'un même début, en éventail, et des passages qui sautent de l'une à l'autre.
    private void possibles(GraphicsContext g, double w, double cx, double cy, double u) {
        double x0 = w * 0.1;
        int count = 22;
        double[] ends = new double[count];
        for (int k = 0; k < count; k++) {
            double t = k / (count - 1.0);
            double end = cy + (t - 0.5) * u * 0.8;
            ends[k] = end;
            int steps = 24;
            double[] xs = new double[steps + 1], ys = new double[steps + 1];
            for (int s = 0; s <= steps; s++) {
                double f = s / (double) steps;
                xs[s] = x0 + (w * 0.8) * f;
                ys[s] = cy + (end - cy) * f + Math.sin(f * 9 + k + time) * 4 * f;
            }
            g.setStroke(PALETTE[k % PALETTE.length]);
            g.setLineWidth(1);
            g.setGlobalAlpha(0.45);
            g.strokePolyline(xs, ys, steps + 1);
        }
        // Les passages : des arcs qui relient deux suites, et un éclat qui les emprunte.
        for (int k = 0; k < 6; k++) {
            int a = (int) (UniverseView.noise(k * 5 + 1) * count), b = (int) (UniverseView.noise(k * 5 + 2) * count);
            double x = x0 + w * 0.8 * (0.4 + 0.5 * UniverseView.noise(k * 5 + 3));
            double ya = cy + (ends[a] - cy) * (x - x0) / (w * 0.8), yb = cy + (ends[b] - cy) * (x - x0) / (w * 0.8);
            int steps = 16;
            double[] xs = new double[steps + 1], ys = new double[steps + 1];
            for (int s = 0; s <= steps; s++) {
                double f = s / (double) steps;
                xs[s] = x + Math.sin(f * Math.PI) * u * 0.08;
                ys[s] = ya + (yb - ya) * f;
            }
            g.setStroke(Color.WHITE);
            g.setLineWidth(1.2);
            g.setGlobalAlpha(0.6);
            g.strokePolyline(xs, ys, steps + 1);
            double f = (time * 0.5 + k * 0.17) % 1;
            UniverseView.glow(g, x + Math.sin(f * Math.PI) * u * 0.08, ya + (yb - ya) * f, 5, Color.WHITE, 0.9);
        }
        UniverseView.glow(g, x0, cy, u * 0.05, color(), 0.9);
    }

    // 7. Plusieurs débuts, chacun avec son éventail de possibles.
    private void beginnings(GraphicsContext g, double w, double h, double u) {
        int roots = 5;
        for (int r = 0; r < roots; r++) {
            double y = h * (0.15 + 0.7 * r / (roots - 1.0));
            double x = w * (0.08 + 0.04 * Math.sin(time * 0.6 + r));
            Color c = PALETTE[(r * 3) % PALETTE.length];
            for (int k = 0; k < 9; k++) {
                double t = k / 8.0 - 0.5;
                int steps = 18;
                double[] xs = new double[steps + 1], ys = new double[steps + 1];
                for (int s = 0; s <= steps; s++) {
                    double f = s / (double) steps;
                    xs[s] = x + w * 0.82 * f;
                    ys[s] = y + t * h * 0.16 * f + Math.sin(f * 7 + time * 0.8 + k + r) * 3 * f;
                }
                g.setStroke(c);
                g.setLineWidth(1);
                g.setGlobalAlpha(0.4);
                g.strokePolyline(xs, ys, steps + 1);
            }
            UniverseView.glow(g, x, y, u * 0.035, c, 0.9);
        }
    }

    // 8. Tous les débuts en cercle, chacun son éventail tourné vers le milieu, et tous reliés.
    private void weave(GraphicsContext g, double cx, double cy, double u) {
        int roots = 12;
        double r = u * 0.38;
        for (int k = 0; k < roots; k++) {
            double a = 2 * Math.PI * k / roots + time * 0.08;
            double x = cx + r * Math.cos(a), y = cy + r * Math.sin(a);
            Color c = PALETTE[k % PALETTE.length];
            for (int j = 0; j < 5; j++) {
                double spread = (j - 2) * 0.12;
                double tx = cx + r * 0.25 * Math.cos(a + Math.PI + spread), ty = cy + r * 0.25 * Math.sin(a + Math.PI + spread);
                g.setStroke(c);
                g.setLineWidth(1);
                g.setGlobalAlpha(0.35);
                g.strokeLine(x, y, tx, ty);
            }
            int next = (k + 5) % roots;
            double b = 2 * Math.PI * next / roots + time * 0.08;
            g.setStroke(Color.WHITE);
            g.setGlobalAlpha(0.12);
            g.strokeLine(x, y, cx + r * Math.cos(b), cy + r * Math.sin(b));
            UniverseView.glow(g, x, y, u * 0.03, c, 0.9);
        }
        UniverseView.glow(g, cx, cy, u * 0.12, color(), 0.35);
    }

    // 9. Des anneaux, chacun avec ses lois : on tourne, on s'arrête, on spirale, on tremble.
    private void laws(GraphicsContext g, double cx, double cy, double u) {
        int rings = 6;
        for (int ring = 0; ring < rings; ring++) {
            double r = u * (0.08 + 0.065 * ring);
            Color c = PALETTE[(ring * 2 + 1) % PALETTE.length];
            g.setStroke(c);
            g.setLineWidth(1);
            g.setGlobalAlpha(0.25);
            g.strokeOval(cx - r, cy - r, 2 * r, 2 * r);
            int dots = 6 + ring * 3;
            for (int k = 0; k < dots; k++) {
                double base = 2 * Math.PI * k / dots;
                double a, d = r;
                switch (ring % 4) {
                    case 0 -> a = base + time * 0.8;                                      // ça tourne
                    case 1 -> a = base;                                                   // rien ne bouge
                    case 2 -> {                                                           // ça spirale
                        a = base + time * 0.4;
                        d = r * (0.85 + 0.15 * Math.sin(time * 2 + k));
                    }
                    default -> {                                                          // ça tremble
                        a = base - time * 0.3;
                        d = r + 3 * Math.sin(time * 17 + k * 3);
                    }
                }
                double size = 2 + (ring % 3);
                g.setFill(c);
                g.setGlobalAlpha(0.9);
                g.fillOval(cx + d * Math.cos(a) - size / 2, cy + d * Math.sin(a) - size / 2, size, size);
            }
        }
        UniverseView.glow(g, cx, cy, u * 0.06, color(), 0.8);
    }

    // 10. Tout en un point : les motifs des neuf autres tournent en s'y resserrant, et la lumière en sort.
    private void point(GraphicsContext g, double w, double h, double cx, double cy, double u) {
        for (int k = 0; k < 48; k++) {
            double a = 2 * Math.PI * k / 48 + time * 0.05;
            g.setStroke(PALETTE[k % PALETTE.length]);
            g.setLineWidth(1);
            g.setGlobalAlpha(0.15 + 0.1 * Math.sin(time * 2 + k));
            g.strokeLine(cx, cy, cx + Math.cos(a) * Math.max(w, h), cy + Math.sin(a) * Math.max(w, h));
        }
        for (int k = 0; k < 9; k++) {
            double phase = (time * 0.12 + k / 9.0) % 1;
            double r = u * 0.42 * (1 - phase);
            double a = 2 * Math.PI * k / 9 + time * 0.4 + phase * 3;
            double x = cx + r * Math.cos(a), y = cy + r * Math.sin(a);
            Color c = PALETTE[k];
            if (k == 2) cube(g, x, y, u * 0.025 * (1 - phase) + 2, time, time * 0.7, c, 1 - phase * 0.5);
            else UniverseView.glow(g, x, y, u * 0.03 * (1 - phase) + 3, c, 0.9 * (1 - phase * 0.5));
        }
        UniverseView.glow(g, cx, cy, u * (0.1 + 0.02 * Math.sin(time * 3)), color(), 1);
        g.setGlobalAlpha(1);
        g.setFill(Color.WHITE);
        g.fillOval(cx - 4, cy - 4, 8, 8);
    }
}
