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
 * Les univers de l'onglet du même nom. Version 0.80 : une toile d'amas ; version 0.81 : chaque
 * univers est une bulle ronde, un nuage coloré qui dérive à l'intérieur, une poussière d'étoiles et
 * un anneau incliné qui dit sous quel angle on la voit.
 *
 * <p>Comme les copies de l'atome : le premier univers est au centre, en grand ; les suivants
 * viennent autour, plus petits (jamais plus grands que lui), de tailles variées, jusqu'à
 * {@link #MAX_BUBBLES}, où la vue est bien chargée. Dès qu'il y en a plusieurs, des lignes
 * semi-transparentes les relient, chacun à ses voisins.
 *
 * <p>La naissance ({@link #playBirth()}) se joue à chaque nouvel univers : un point de lumière qui
 * grossit jusqu'à l'éclair blanc, une onde et des traînées qui partent du centre, les bulles qui
 * s'allument à mesure que l'expansion les atteint, puis les mots.
 */
public final class UniverseView extends Canvas {

    /** Durée de la naissance, en secondes. */
    public static final double BIRTH_SECONDS = 8;

    /** Nombre maximal de bulles dessinées : au-delà, la vue est pleine et n'en montre pas plus. */
    public static final int MAX_BUBBLES = 30;

    /** Taille d'une bulle, au plus, par rapport à celle du premier univers. */
    static final double MAX_BUBBLE_SCALE = 0.6;

    private static final Color BACK = Color.web("#04050c");
    private static final Color STAR = Color.web("#dfe6ff");
    private static final Color LINK = Color.web("#a8b8ff");
    private static final Color RIM = Color.web("#c4ceff");
    private static final Color WARM = Color.web("#ffe2b0");
    private static final Color COLD = Color.web("#8fb0ff");
    private static final Color PINK = Color.web("#ff9fd6");
    private static final Color VIOLET = Color.web("#b48cff");
    private static final Color TEAL = Color.web("#7fe0d4");
    private static final Color[] CLOUD = {PINK, COLD, VIOLET, WARM, TEAL};
    private static final int STARS = 200;
    private static final int STREAKS = 90;

    /** Une bulle : sa place, sa taille (par rapport à la première), son anneau, sa graine. */
    record Bubble(double x, double y, double scale, double turn, double tilt, int seed) {
    }

    private double time;
    /** Temps écoulé depuis le début de la naissance ; {@link #BIRTH_SECONDS} ou plus quand elle est finie. */
    private double birth = BIRTH_SECONDS;
    private int universes = 1;
    /** Le nombre d'univers quand la naissance a commencé : la dernière bulle est celle qui naît. */
    private int bornAt = 1;

    private List<Bubble> layout = List.of();
    private double layoutWidth = -1;
    private double layoutHeight = -1;
    private List<int[]> links = List.of();

    public UniverseView() {
        super(800, 440);
    }

    /** Le nombre d'univers à montrer (au moins un). */
    public void setUniverses(int count) {
        universes = Math.max(1, count);
    }

    /** Lance la naissance, depuis le début : celle du dernier univers créé. */
    public void playBirth() {
        birth = 0;
        bornAt = universes;
    }

    /** Vrai tant que la naissance se joue. */
    public boolean isBirthPlaying() {
        return birth < BIRTH_SECONDS;
    }

    /** Où en est la naissance, de 0 à 1 : pour les vérifications. */
    public double birthProgress() {
        return Math.min(1, birth / BIRTH_SECONDS);
    }

    /** Nombre de bulles dessinées : une par univers, {@link #MAX_BUBBLES} au plus. */
    public int bubbles() {
        return Math.min(MAX_BUBBLES, universes);
    }

    /** Nombre de lignes entre les bulles dessinées : pour les vérifications. */
    public int shownLinks() {
        int count = 0;
        for (int[] link : links(getWidth(), getHeight())) if (link[0] < bubbles() && link[1] < bubbles()) count++;
        return count;
    }

    /** Nombre de bulles allumées à cette image : pour les vérifications. */
    public int litBubbles() {
        List<Bubble> all = layout(getWidth(), getHeight());
        int lit = 0;
        for (int k = 0; k < bubbles(); k++) if (reveal(all.get(k)) > 0) lit++;
        return lit;
    }

    /** Les bulles dessinées : pour les vérifications. */
    List<Bubble> shownBubbles() {
        return layout(getWidth(), getHeight()).subList(0, bubbles());
    }

    /**
     * Avance l'animation de {@code elapsed} secondes et redessine. Sans animation ({@code moving}
     * faux, réglage coupé), tout reste immobile et la naissance passe d'un coup.
     */
    public void frame(double elapsed, boolean moving) {
        double dt = Math.min(elapsed, 0.1);
        if (moving) time += dt;
        birth = moving ? birth + dt : BIRTH_SECONDS;
        draw();
    }

    /** Rayon de la première bulle, pour cette taille de vue. */
    private static double mainRadius(double width, double height) {
        return Math.min(210, Math.min(width, height) * 0.4);
    }

    /**
     * Place les bulles une fois pour toutes pour cette taille : la première au centre, les autres
     * tirées au hasard (toujours le même) autour, en évitant la première et en se serrant peu à peu
     * contre les autres. Les premières sont les plus grandes ; ajouter un univers ne déplace jamais
     * les précédents.
     */
    private List<Bubble> layout(double width, double height) {
        if (width == layoutWidth && height == layoutHeight) return layout;
        layoutWidth = width;
        layoutHeight = height;
        double cx = width / 2, cy = height / 2;
        double main = mainRadius(width, height);
        List<Bubble> bubbles = new ArrayList<>();
        bubbles.add(new Bubble(cx, cy, 1, 0.4, 0.32, 0));
        int seed = 0;
        for (int k = 1; k < MAX_BUBBLES; k++) {
            double scale = MAX_BUBBLE_SCALE * (0.35 + 0.65 * noise(k * 11 + 1)) * (1 - 0.55 * k / (double) MAX_BUBBLES);
            double radius = main * scale;
            Bubble placed = null;
            for (int attempt = 0; attempt < 400 && placed == null; attempt++, seed++) {
                double looseness = 1.15 - 0.85 * attempt / 400.0;
                double x = width * noise(seed * 3 + 101), y = height * noise(seed * 3 + 102);
                boolean free = true;
                for (Bubble other : bubbles) {
                    double room = (radius + main * other.scale()) * looseness;
                    if (Math.hypot(x - other.x(), y - other.y()) < room) {
                        free = false;
                        break;
                    }
                }
                if (free) placed = new Bubble(x, y, scale, Math.PI * noise(k * 11 + 2), 0.2 + 0.5 * noise(k * 11 + 3), k);
            }
            if (placed == null) {
                double angle = 2 * Math.PI * noise(k * 11 + 5), far = main + radius;
                placed = new Bubble(cx + far * Math.cos(angle), cy + far * Math.sin(angle), scale,
                        Math.PI * noise(k * 11 + 2), 0.2 + 0.5 * noise(k * 11 + 3), k);
            }
            bubbles.add(placed);
        }
        layout = List.copyOf(bubbles);
        links = null;
        return layout;
    }

    /** Les lignes : chaque bulle se relie à la plus proche de celles d'avant, et à la deuxième plus proche. */
    private List<int[]> links(double width, double height) {
        List<Bubble> all = layout(width, height);
        if (links != null) return links;
        List<int[]> made = new ArrayList<>();
        for (int k = 1; k < all.size(); k++) {
            int first = -1, second = -1;
            double best = Double.MAX_VALUE, next = Double.MAX_VALUE;
            for (int j = 0; j < k; j++) {
                double d = Math.hypot(all.get(k).x() - all.get(j).x(), all.get(k).y() - all.get(j).y());
                if (d < best) {
                    next = best;
                    second = first;
                    best = d;
                    first = j;
                } else if (d < next) {
                    next = d;
                    second = j;
                }
            }
            made.add(new int[] {first, k});
            if (second >= 0) made.add(new int[] {second, k});
        }
        links = List.copyOf(made);
        return links;
    }

    /** L'onde de l'expansion pendant la naissance, en part de la demi-diagonale. */
    private double front() {
        if (!isBirthPlaying()) return 2;
        double t = (birth - 1.0) / (BIRTH_SECONDS - 3.0);
        return t <= 0 ? 0 : 1.2 * Math.min(1, t);
    }

    /**
     * À quel point une bulle est allumée, de 0 à 1. Pendant la naissance du premier univers, tout
     * s'allume au passage de l'onde ; pour les suivants, seule la nouvelle bulle attend l'onde.
     */
    private double reveal(Bubble bubble) {
        if (!isBirthPlaying()) return 1;
        boolean newcomer = bubble.seed() == Math.min(MAX_BUBBLES, bornAt) - 1;
        if (bornAt > 1 && !newcomer) return 1;
        double w = getWidth(), h = getHeight();
        double distance = Math.hypot(bubble.x() - w / 2, bubble.y() - h / 2) / Math.hypot(w / 2, h / 2);
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
        boolean first = bornAt <= 1;

        // Le fond d'étoiles, qui scintille doucement ; à la première naissance, il apparaît derrière l'onde.
        g.setFill(STAR);
        for (int i = 0; i < STARS; i++) {
            double x = noise(i * 3 + 700) * w, y = noise(i * 3 + 701) * h;
            if (isBirthPlaying() && first && Math.hypot(x - cx, y - cy) / half > front) continue;
            double size = 0.6 + 1.3 * noise(i * 3 + 702);
            g.setGlobalAlpha(0.15 + 0.4 * (0.5 + 0.5 * Math.sin(time * (0.4 + noise(i) * 1.2) + i)));
            g.fillOval(x - size / 2, y - size / 2, size, size);
        }

        List<Bubble> all = layout(w, h);
        int shown = bubbles();
        double main = mainRadius(w, h);

        // Les lignes entre les univers, sous les bulles : semi-transparentes, avec un éclat qui les parcourt.
        if (shown > 1) {
            for (int[] link : links(w, h)) {
                if (link[0] >= shown || link[1] >= shown) continue;
                Bubble a = all.get(link[0]), b = all.get(link[1]);
                double lit = Math.min(reveal(a), reveal(b));
                if (lit <= 0) continue;
                g.setStroke(LINK);
                g.setLineWidth(3);
                g.setGlobalAlpha(0.07 * lit);
                g.strokeLine(a.x(), a.y(), b.x(), b.y());
                g.setLineWidth(1);
                g.setGlobalAlpha(0.3 * lit);
                g.strokeLine(a.x(), a.y(), b.x(), b.y());
                double t = (time * 0.25 + noise(link[1] * 7 + link[0])) % 1;
                double px = a.x() + (b.x() - a.x()) * t, py = a.y() + (b.y() - a.y()) * t;
                glow(g, px, py, 4, LINK, 0.7 * lit);
            }
        }

        // Les bulles, des plus petites aux plus grandes : la première passe devant toutes.
        List<Bubble> order = new ArrayList<>(all.subList(0, shown));
        order.sort((x, y) -> Double.compare(x.scale(), y.scale()));
        for (Bubble bubble : order) {
            double lit = reveal(bubble);
            if (lit <= 0) continue;
            double pop = 1 + 0.5 * Math.max(0, 1 - lit) * lit * 4;
            bubble(g, bubble, main * bubble.scale() * pop, lit);
        }

        if (isBirthPlaying()) drawBirth(g, w, h, cx, cy, half);
        g.setGlobalAlpha(1);
    }

    /**
     * Une bulle d'univers : un halo, une sphère sombre, un nuage coloré qui dérive dedans, des
     * étoiles, un anneau incliné, et le bord, éclairé d'un côté.
     */
    private void bubble(GraphicsContext g, Bubble bubble, double r, double lit) {
        double x = bubble.x(), y = bubble.y();
        int seed = bubble.seed();
        Color tint = CLOUD[seed % CLOUD.length];
        glow(g, x, y, r * 1.35, tint, 0.18 * lit);
        // La sphère : sombre au centre, bleutée au bord.
        g.setGlobalAlpha(lit);
        g.setFill(new RadialGradient(0, 0, x - r * 0.25, y - r * 0.3, r * 1.3, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#151a36")), new Stop(0.7, Color.web("#0a0d1f")), new Stop(1, Color.web("#1d2350"))));
        g.fillOval(x - r, y - r, 2 * r, 2 * r);
        // Le nuage : des voiles de couleur qui tournent lentement, sans sortir de la bulle.
        int veils = seed == 0 ? 11 : 6;
        for (int k = 0; k < veils; k++) {
            double a = 2 * Math.PI * noise(seed * 53 + k) + time * (0.05 + 0.08 * noise(seed * 53 + k + 20)) * (k % 2 == 0 ? 1 : -1);
            double d = r * 0.45 * noise(seed * 53 + k + 40);
            double size = r * (0.3 + 0.3 * noise(seed * 53 + k + 60));
            Color color = CLOUD[(seed + k) % CLOUD.length];
            glow(g, x + d * Math.cos(a), y + d * Math.sin(a) * 0.8, size, color, 0.28 * lit);
        }
        // Une poussière d'étoiles, qui tourne avec le nuage.
        g.setFill(STAR);
        int stars = seed == 0 ? 40 : 14;
        for (int k = 0; k < stars; k++) {
            double a = 2 * Math.PI * noise(seed * 97 + k) + time * 0.04;
            double d = r * 0.85 * Math.sqrt(noise(seed * 97 + k + 300));
            double dot = Math.max(0.5, r * 0.012 * (0.5 + noise(seed * 97 + k + 600)));
            g.setGlobalAlpha(lit * (0.4 + 0.5 * noise(seed * 97 + k + 900)));
            g.fillOval(x + d * Math.cos(a) - dot, y + d * Math.sin(a) - dot, 2 * dot, 2 * dot);
        }
        // L'anneau incliné : il dit sous quel angle on voit cette bulle.
        double cos = Math.cos(bubble.turn()), sin = Math.sin(bubble.turn());
        int points = 48;
        double[] xs = new double[points + 1], ys = new double[points + 1];
        for (int p = 0; p <= points; p++) {
            double t = 2 * Math.PI * p / points + time * 0.1;
            double ux = r * 0.92 * Math.cos(t), uy = r * 0.92 * bubble.tilt() * Math.sin(t);
            xs[p] = x + ux * cos - uy * sin;
            ys[p] = y + ux * sin + uy * cos;
        }
        g.setStroke(tint);
        g.setLineWidth(1);
        g.setGlobalAlpha(0.35 * lit);
        g.strokePolyline(xs, ys, points + 1);
        // Le bord, plus clair en haut à gauche, comme une bulle de savon.
        g.setStroke(RIM);
        g.setLineWidth(seed == 0 ? 2 : 1.2);
        g.setGlobalAlpha(0.55 * lit);
        g.strokeOval(x - r, y - r, 2 * r, 2 * r);
        g.setStroke(Color.WHITE);
        g.setGlobalAlpha(0.5 * lit);
        double[] hx = new double[13], hy = new double[13];
        for (int p = 0; p <= 12; p++) {
            double a = Math.toRadians(200 + 50 * p / 12.0);
            hx[p] = x + r * 0.86 * Math.cos(a);
            hy[p] = y + r * 0.86 * Math.sin(a);
        }
        g.strokePolyline(hx, hy, 13);
        g.setGlobalAlpha(1);
    }

    /** L'éclair de la naissance : le point qui grossit, l'écran blanc, l'onde, les traînées, puis les mots. */
    private void drawBirth(GraphicsContext g, double w, double h, double cx, double cy, double half) {
        double t = birth;
        if (t < 1.0) {
            double grow = t / 1.0;
            glow(g, cx, cy, 8 + 120 * grow * grow, Color.WHITE, 0.6 + 0.4 * grow);
            g.setFill(Color.WHITE);
            g.setGlobalAlpha(1);
            double core = 2 + 10 * grow;
            g.fillOval(cx - core, cy - core, 2 * core, 2 * core);
        }
        if (t >= 0.9 && t < 2.6) {
            double fade = t < 1.05 ? (t - 0.9) / 0.15 : 1 - (t - 1.05) / 1.55;
            g.setFill(Color.WHITE);
            g.setGlobalAlpha(Math.max(0, Math.min(1, fade)) * 0.95);
            g.fillRect(0, 0, w, h);
        }
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
        if (t >= BIRTH_SECONDS - 3.2) {
            double in = Math.min(1, (t - (BIRTH_SECONDS - 3.2)) / 0.8);
            double out = Math.min(1, (BIRTH_SECONDS - t) / 0.8);
            g.setGlobalAlpha(Math.max(0, Math.min(in, out)));
            g.setFill(WARM);
            g.setTextAlign(TextAlignment.CENTER);
            g.setFont(Font.font(Math.max(22, Math.min(w, h) * 0.08)));
            g.fillText(bornAt > 1 ? "Un nouvel univers est né" : "L'univers est né", cx, cy + Math.min(w, h) * 0.03);
            g.setTextAlign(TextAlignment.LEFT);
        }
    }

    static void glow(GraphicsContext g, double x, double y, double size, Color color, double strength) {
        if (size <= 0 || strength <= 0) return;
        g.setGlobalAlpha(1);
        g.setFill(new RadialGradient(0, 0, x, y, size, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.color(color.getRed(), color.getGreen(), color.getBlue(), Math.min(1, strength))),
                new Stop(1, Color.color(color.getRed(), color.getGreen(), color.getBlue(), 0))));
        g.fillOval(x - size, y - size, 2 * size, 2 * size);
    }

    /** Un nombre entre 0 et 1, toujours le même pour une même graine. */
    static double noise(int seed) {
        long x = seed * 0x9E3779B97F4A7C15L;
        x ^= x >>> 31;
        x *= 0xBF58476D1CE4E5B9L;
        x ^= x >>> 29;
        return (x >>> 11) / (double) (1L << 53);
    }
}
