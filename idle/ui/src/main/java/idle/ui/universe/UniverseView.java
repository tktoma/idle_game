package idle.ui.universe;

import java.util.ArrayList;
import java.util.List;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

/**
 * L'univers de l'onglet du même nom (version 0.80) : la toile cosmique, des amas de galaxies
 * reliés par des filaments pâles, chacun quelques petites galaxies spirales qui tournent, sur un
 * fond d'étoiles.
 *
 * <p>Sa naissance ({@link #playBirth()}), jouée une fois quand l'univers se forme : un point de
 * lumière qui grossit jusqu'à l'éclair blanc, une onde et des traînées qui partent du centre, puis
 * les amas et les filaments qui s'allument à mesure que l'expansion les atteint. Enfin, les mots
 * « L'univers est né » apparaissent et s'effacent.
 */
public final class UniverseView extends Canvas {

    /** Durée de la naissance, en secondes. */
    public static final double BIRTH_SECONDS = 8;

    private static final Color BACK = Color.web("#04050c");
    private static final Color STAR = Color.web("#dfe6ff");
    private static final Color FILAMENT = Color.web("#8fa2ff");
    private static final Color WARM = Color.web("#ffe2b0");
    private static final Color COLD = Color.web("#a8c4ff");
    private static final Color PINK = Color.web("#ffb0dc");
    private static final int NODES = 34;
    private static final int STARS = 220;
    private static final int STREAKS = 90;

    /** Un amas de la toile : sa place (de 0 à 1 dans la vue), sa taille, sa teinte, sa vitesse. */
    private record Node(double x, double y, double size, Color tint, double spin, double turn) {
    }

    private double time;
    /** Temps écoulé depuis le début de la naissance ; {@link #BIRTH_SECONDS} ou plus quand elle est finie. */
    private double birth = BIRTH_SECONDS;
    private boolean moving = true;
    private final List<Node> nodes = new ArrayList<>();
    private final List<int[]> links = new ArrayList<>();

    public UniverseView() {
        super(800, 440);
        layout(NODES);
    }

    /** Lance la naissance de l'univers, depuis le début. */
    public void playBirth() {
        birth = 0;
    }

    /** Vrai tant que la naissance se joue. */
    public boolean isBirthPlaying() {
        return birth < BIRTH_SECONDS;
    }

    /** Où en est la naissance, de 0 à 1 : pour les vérifications. */
    public double birthProgress() {
        return Math.min(1, birth / BIRTH_SECONDS);
    }

    /** Nombre d'amas allumés à cette image : pour les vérifications. */
    public int litNodes() {
        int lit = 0;
        for (Node node : nodes) if (reveal(node) > 0) lit++;
        return lit;
    }

    /** Nombre d'amas de la toile. */
    public int nodes() {
        return nodes.size();
    }

    /**
     * Avance l'animation de {@code elapsed} secondes et redessine. Sans animation ({@code moving}
     * faux, réglage coupé), la toile reste immobile et la naissance passe d'un coup.
     */
    public void frame(double elapsed, boolean moving) {
        this.moving = moving;
        double dt = Math.min(elapsed, 0.1);
        if (moving) time += dt;
        birth = moving ? birth + dt : BIRTH_SECONDS;
        draw();
    }

    /** Les amas et leurs filaments, placés une fois pour toutes (toujours les mêmes). */
    private void layout(int count) {
        Color[] tints = {WARM, COLD, PINK, WARM, COLD};
        for (int k = 0; k < count; k++) {
            // Une graine par amas ; les premiers, plus gros, sont plus près du centre.
            double angle = 2 * Math.PI * noise(k * 5 + 1);
            double reach = 0.08 + 0.44 * Math.sqrt(noise(k * 5 + 2)) * (0.4 + 0.6 * k / (double) count);
            double x = 0.5 + reach * Math.cos(angle) * 1.15;
            double y = 0.5 + reach * Math.sin(angle) * 0.95;
            double size = 0.6 + 0.8 * noise(k * 5 + 3) * (1 - 0.5 * k / (double) count);
            nodes.add(new Node(x, y, size, tints[k % tints.length], (0.15 + 0.3 * noise(k * 5 + 4)) * (k % 2 == 0 ? 1 : -1),
                    Math.PI * noise(k * 5 + 5)));
        }
        // Chaque amas se relie à ses deux plus proches voisins : la toile cosmique.
        for (int a = 0; a < count; a++) {
            int first = -1, second = -1;
            double best = Double.MAX_VALUE, next = Double.MAX_VALUE;
            for (int b = 0; b < count; b++) {
                if (a == b) continue;
                double d = Math.hypot(nodes.get(a).x() - nodes.get(b).x(), nodes.get(a).y() - nodes.get(b).y());
                if (d < best) {
                    next = best;
                    second = first;
                    best = d;
                    first = b;
                } else if (d < next) {
                    next = d;
                    second = b;
                }
            }
            for (int b : new int[] {first, second}) {
                if (b < 0) continue;
                int lo = Math.min(a, b), hi = Math.max(a, b);
                boolean known = false;
                for (int[] link : links) known |= link[0] == lo && link[1] == hi;
                if (!known) links.add(new int[] {lo, hi});
            }
        }
    }

    /** L'onde de l'expansion pendant la naissance, en part de la demi-diagonale : de 0 à un peu plus de 1. */
    private double front() {
        if (!isBirthPlaying()) return 2;
        double t = (birth - 1.0) / (BIRTH_SECONDS - 3.0);
        return t <= 0 ? 0 : 1.2 * Math.min(1, t);
    }

    /** À quel point cet amas est allumé, de 0 à 1 : l'onde l'a atteint, et il s'allume en une demi-seconde. */
    private double reveal(Node node) {
        if (!isBirthPlaying()) return 1;
        double distance = Math.hypot(node.x() - 0.5, node.y() - 0.5) / Math.hypot(0.5, 0.5);
        double past = front() - distance;
        return past <= 0 ? 0 : Math.min(1, past * 6);
    }

    private void draw() {
        double w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        GraphicsContext g = getGraphicsContext2D();
        g.setGlobalAlpha(1);
        g.clearRect(0, 0, w, h);
        g.setFill(BACK);
        g.fillRect(0, 0, w, h);
        double cx = w / 2, cy = h / 2;
        double half = Math.hypot(cx, cy);
        double front = front();

        // Le fond d'étoiles, qui apparaît derrière l'onde et scintille doucement.
        g.setFill(STAR);
        for (int i = 0; i < STARS; i++) {
            double x = noise(i * 3 + 700) * w, y = noise(i * 3 + 701) * h;
            double distance = Math.hypot(x - cx, y - cy) / half;
            if (distance > front) continue;
            double size = 0.6 + 1.3 * noise(i * 3 + 702);
            g.setGlobalAlpha(0.15 + 0.45 * (0.5 + 0.5 * Math.sin(time * (0.4 + noise(i) * 1.2) + i)));
            g.fillOval(x - size / 2, y - size / 2, size, size);
        }

        // Les filaments, entre deux amas allumés.
        g.setStroke(FILAMENT);
        for (int[] link : links) {
            Node a = nodes.get(link[0]), b = nodes.get(link[1]);
            double shown = Math.min(reveal(a), reveal(b));
            if (shown <= 0) continue;
            double ax = a.x() * w, ay = a.y() * h, bx = b.x() * w, by = b.y() * h;
            // Un filament légèrement courbé, qui ondule à peine.
            double mx = (ax + bx) / 2 + (by - ay) * 0.12 * Math.sin(time * 0.2 + link[0]);
            double my = (ay + by) / 2 - (bx - ax) * 0.12 * Math.sin(time * 0.2 + link[0]);
            int steps = 16;
            double[] xs = new double[steps + 1], ys = new double[steps + 1];
            for (int s = 0; s <= steps; s++) {
                double t = s / (double) steps;
                xs[s] = (1 - t) * (1 - t) * ax + 2 * (1 - t) * t * mx + t * t * bx;
                ys[s] = (1 - t) * (1 - t) * ay + 2 * (1 - t) * t * my + t * t * by;
            }
            g.setLineWidth(3);
            g.setGlobalAlpha(0.06 * shown);
            g.strokePolyline(xs, ys, steps + 1);
            g.setLineWidth(1);
            g.setGlobalAlpha(0.22 * shown);
            g.strokePolyline(xs, ys, steps + 1);
        }

        // Les amas : un halo, et trois à cinq petites galaxies spirales qui tournent.
        double unit = Math.min(w, h);
        for (int k = 0; k < nodes.size(); k++) {
            Node node = nodes.get(k);
            double shown = reveal(node);
            if (shown <= 0) continue;
            double x = node.x() * w, y = node.y() * h;
            // À son arrivée, l'amas s'allume d'un coup de lumière qui retombe.
            double pop = 1 + 0.8 * Math.max(0, 1 - shown) * shown * 4;
            double r = unit * 0.05 * node.size() * pop;
            glow(g, x, y, r * 1.6, node.tint(), 0.22 * shown);
            int galaxies = 3 + (int) (noise(k * 13 + 9) * 3);
            for (int j = 0; j < galaxies; j++) {
                double a = node.turn() + 2 * Math.PI * j / galaxies + time * node.spin() * 0.3;
                double d = r * (0.35 + 0.5 * noise(k * 31 + j));
                spiral(g, x + d * Math.cos(a), y + d * Math.sin(a) * 0.7, r * (0.28 + 0.2 * noise(k * 17 + j)),
                        node.turn() + j, time * node.spin() * (j % 2 == 0 ? 1 : -1.3), node.tint(), shown);
            }
        }

        if (isBirthPlaying()) drawBirth(g, w, h, cx, cy, half);
        g.setGlobalAlpha(1);
    }

    /** L'éclair de la naissance : le point qui grossit, l'écran blanc, l'onde, les traînées, puis les mots. */
    private void drawBirth(GraphicsContext g, double w, double h, double cx, double cy, double half) {
        double t = birth;
        // Le point de lumière qui grossit, jusqu'à l'éclair.
        if (t < 1.0) {
            double grow = t / 1.0;
            glow(g, cx, cy, 8 + 120 * grow * grow, Color.WHITE, 0.6 + 0.4 * grow);
            g.setFill(Color.WHITE);
            g.setGlobalAlpha(1);
            double core = 2 + 10 * grow;
            g.fillOval(cx - core, cy - core, 2 * core, 2 * core);
        }
        // L'écran blanc, qui retombe.
        if (t >= 0.9 && t < 2.6) {
            double fade = t < 1.05 ? (t - 0.9) / 0.15 : 1 - (t - 1.05) / 1.55;
            g.setFill(Color.WHITE);
            g.setGlobalAlpha(Math.max(0, Math.min(1, fade)) * 0.95);
            g.fillRect(0, 0, w, h);
        }
        // L'onde et les traînées qui partent du centre.
        if (t >= 1.0 && t < BIRTH_SECONDS - 1.5) {
            double radius = front() * half;
            double fade = Math.max(0, 1 - (t - 1.0) / (BIRTH_SECONDS - 2.5));
            g.setStroke(WARM);
            g.setLineWidth(3);
            g.setGlobalAlpha(0.7 * fade);
            g.strokeOval(cx - radius, cy - radius * 0.8, 2 * radius, 1.6 * radius);
            g.setStroke(Color.WHITE);
            g.setLineWidth(1.2);
            g.setGlobalAlpha(0.9 * fade);
            g.strokeOval(cx - radius * 0.97, cy - radius * 0.78, 1.94 * radius, 1.56 * radius);
            for (int i = 0; i < STREAKS; i++) {
                double a = 2 * Math.PI * noise(i * 7 + 300);
                double speed = 0.6 + 0.6 * noise(i * 7 + 301);
                double head = radius * speed, tail = head * 0.7;
                g.setStroke(i % 3 == 0 ? COLD : i % 3 == 1 ? WARM : PINK);
                g.setLineWidth(1 + noise(i * 7 + 302));
                g.setGlobalAlpha(0.8 * fade);
                g.strokeLine(cx + tail * Math.cos(a), cy + tail * Math.sin(a) * 0.8, cx + head * Math.cos(a), cy + head * Math.sin(a) * 0.8);
            }
        }
        // Les mots, à la fin.
        if (t >= BIRTH_SECONDS - 3.2) {
            double in = Math.min(1, (t - (BIRTH_SECONDS - 3.2)) / 0.8);
            double out = Math.min(1, (BIRTH_SECONDS - t) / 0.8);
            g.setGlobalAlpha(Math.max(0, Math.min(in, out)));
            g.setFill(WARM);
            g.setTextAlign(TextAlignment.CENTER);
            g.setFont(Font.font(Math.max(22, Math.min(w, h) * 0.08)));
            g.fillText("L'univers est né", cx, cy + Math.min(w, h) * 0.03);
            g.setTextAlign(TextAlignment.LEFT);
        }
    }

    /** Une petite galaxie spirale : deux bras de points qui tournent, et un cœur lumineux. */
    private static void spiral(GraphicsContext g, double x, double y, double r, double turn, double spin, Color tint, double shown) {
        glow(g, x, y, r * 0.55, tint, 0.55 * shown);
        g.setFill(tint);
        for (int arm = 0; arm < 2; arm++) {
            for (int p = 1; p <= 7; p++) {
                double t = p / 7.0;
                double a = turn + spin + arm * Math.PI + t * 3.2;
                double d = r * t;
                g.setGlobalAlpha(shown * (0.85 - 0.6 * t));
                double dot = Math.max(0.6, r * 0.09 * (1.2 - t));
                g.fillOval(x + d * Math.cos(a) - dot, y + d * Math.sin(a) * 0.6 - dot, 2 * dot, 2 * dot);
            }
        }
        g.setFill(Color.WHITE);
        g.setGlobalAlpha(0.9 * shown);
        double core = Math.max(0.8, r * 0.1);
        g.fillOval(x - core, y - core, 2 * core, 2 * core);
    }

    private static void glow(GraphicsContext g, double x, double y, double size, Color color, double strength) {
        if (size <= 0 || strength <= 0) return;
        g.setGlobalAlpha(1);
        g.setFill(new RadialGradient(0, 0, x, y, size, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.color(color.getRed(), color.getGreen(), color.getBlue(), Math.min(1, strength))),
                new Stop(1, Color.color(color.getRed(), color.getGreen(), color.getBlue(), 0))));
        g.fillOval(x - size, y - size, 2 * size, 2 * size);
    }

    /** Un nombre entre 0 et 1, toujours le même pour une même graine. */
    private static double noise(int seed) {
        long x = seed * 0x9E3779B97F4A7C15L;
        x ^= x >>> 31;
        x *= 0xBF58476D1CE4E5B9L;
        x ^= x >>> 29;
        return (x >>> 11) / (double) (1L << 53);
    }
}
