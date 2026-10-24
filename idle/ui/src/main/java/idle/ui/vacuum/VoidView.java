package idle.ui.vacuum;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;

/**
 * L'écume quantique du vide (version 0.099, branchée sur la partie en 0.129). Des paires de particules y naissent d'un
 * point, s'écartent un instant, puis se rejoignent et s'annulent dans un petit éclair. Depuis la 0.129, ce sont les
 * paires de la partie : elles naissent au rythme du jeu ({@link #follow}) et vivent ce que vit une paire ; le trou
 * noir, une fois ouvert, se dessine au centre, d'autant plus gros que sa masse l'est. Derrière, le champ ondule.
 */
public final class VoidView extends Canvas {

    private static final Color BACK = Color.web("#03060c");
    private static final Color GLOW = Color.web(VoidPage.COLOR);
    private static final Color MATTER = Color.web(VoidPage.COLOR);
    private static final Color ANTIMATTER = Color.web("#d9a6ff");
    /** Les paires dessinées au plus : au-delà, chaque paire dessinée en représente plusieurs. */
    static final int MAX_DRAWN = 240;
    /** Les ondes lentes du champ : leurs centres bougent à peine. */
    private static final int SWELLS = 8;

    /** Une paire de particules virtuelles : née d'un point, elle s'écarte puis se referme. */
    private static final class Pair {
        double x, y, angle, reach, life, age;
    }

    private final Random random = new Random(1999);
    private final List<Pair> pairs = new ArrayList<>();
    private final double[] swellX = new double[SWELLS], swellY = new double[SWELLS], swellPhase = new double[SWELLS];
    private double time;
    /** Les paires nées dans la partie au dernier relevé : celles d'après sont à faire naître. */
    private double seenBorn = Double.NaN;
    /** Ce que vit une paire, et la masse du trou noir (négative tant qu'il n'est pas ouvert). */
    private double life = 6, mass = -1;
    /** Combien de paires de la partie chaque paire dessinée représente. */
    private double scale = 1;

    VoidView() {
        super(780, 360);
        for (int i = 0; i < SWELLS; i++) {
            swellX[i] = random.nextDouble();
            swellY[i] = random.nextDouble();
            swellPhase[i] = random.nextDouble() * Math.PI * 2;
        }
        draw();
    }

    private Pair born() {
        Pair pair = new Pair();
        pair.x = 0.04 + 0.92 * random.nextDouble();
        pair.y = 0.06 + 0.88 * random.nextDouble();
        pair.angle = random.nextDouble() * Math.PI * 2;
        pair.reach = 6 + random.nextDouble() * 16;
        pair.life = life * (0.85 + 0.3 * random.nextDouble());
        return pair;
    }

    /**
     * Suit la partie : {@code bornTotal} paires nées en tout (un compte qui ne fait que monter), {@code alive} vivantes,
     * chacune vivant {@code pairLife} secondes ; {@code holeMass} négatif tant que le trou noir n'est pas ouvert.
     */
    public void follow(double bornTotal, double alive, double pairLife, double holeMass) {
        life = Math.max(0.2, pairLife);
        mass = holeMass;
        scale = Math.max(1, alive / MAX_DRAWN);
        if (Double.isNaN(seenBorn) || bornTotal < seenBorn || (bornTotal - seenBorn) / scale > MAX_DRAWN) {
            // Première image, une partie rechargée, un retour sur l'onglet : les paires vivantes, à des âges différents.
            pairs.clear();
            int shown = (int) Math.min(MAX_DRAWN, Math.round(alive));
            for (int i = 0; i < shown; i++) {
                Pair pair = born();
                pair.age = random.nextDouble() * pair.life;
                pairs.add(pair);
            }
            seenBorn = bornTotal;
            return;
        }
        int fresh = (int) Math.floor((bornTotal - seenBorn) / scale);
        if (fresh <= 0) return;
        seenBorn += fresh * scale;
        for (int i = 0; i < Math.min(fresh, MAX_DRAWN); i++) {
            if (pairs.size() >= MAX_DRAWN) pairs.remove(0);
            pairs.add(born());
        }
    }

    /** Les paires dessinées en ce moment : pour les vérifications. */
    public int drawnPairs() {
        return pairs.size();
    }

    /** Une image : l'écume avance si l'animation est allumée, sinon elle reste figée. */
    public void frame(double elapsed, boolean motion) {
        if (motion) {
            double dt = Math.min(elapsed, 0.1);
            time += dt;
            for (Pair pair : pairs) pair.age += dt;
            pairs.removeIf(pair -> pair.age >= pair.life);
        }
        draw();
    }

    void draw() {
        double w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        GraphicsContext g = getGraphicsContext2D();
        g.setFill(BACK);
        g.fillRect(0, 0, w, h);
        // Le champ : quelques halos très pâles qui respirent.
        for (int i = 0; i < SWELLS; i++) {
            double breath = 0.5 + 0.5 * Math.sin(time * 0.35 + swellPhase[i]);
            double cx = w * (swellX[i] + 0.03 * Math.sin(time * 0.11 + i)), cy = h * (swellY[i] + 0.05 * Math.cos(time * 0.13 + i));
            double r = Math.min(w, h) * (0.35 + 0.15 * breath);
            g.setFill(new RadialGradient(0, 0, cx, cy, r, false, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.color(GLOW.getRed(), GLOW.getGreen(), GLOW.getBlue(), 0.05 + 0.04 * breath)),
                    new Stop(1, Color.TRANSPARENT)));
            g.fillOval(cx - r, cy - r, r * 2, r * 2);
        }
        // La trame : un grain régulier, à peine visible, qui ondule avec le champ.
        g.setFill(Color.color(GLOW.getRed(), GLOW.getGreen(), GLOW.getBlue(), 0.10));
        for (double x = 12; x < w; x += 24) {
            for (double y = 12; y < h; y += 24) {
                double shift = 2 * Math.sin(time * 0.8 + x * 0.03 + y * 0.05);
                g.fillOval(x + shift - 0.8, y - shift - 0.8, 1.6, 1.6);
            }
        }
        if (mass >= 0) hole(g, w / 2, h / 2);
        // Les paires : elles s'écartent jusqu'à mi-vie, puis se rejoignent ; à la fin, un éclair.
        for (Pair pair : pairs) {
            double t = pair.age / pair.life;
            double open = Math.sin(Math.PI * Math.min(1, t / 0.92));
            double cx = pair.x * w, cy = pair.y * h;
            double dx = Math.cos(pair.angle) * pair.reach * open, dy = Math.sin(pair.angle) * pair.reach * open;
            double alpha = Math.min(1, t * 6) * 0.9;
            g.setStroke(Color.color(GLOW.getRed(), GLOW.getGreen(), GLOW.getBlue(), 0.18 * alpha));
            g.setLineWidth(1);
            g.strokeLine(cx - dx, cy - dy, cx + dx, cy + dy);
            dot(g, cx + dx, cy + dy, 2.2, MATTER, alpha);
            dot(g, cx - dx, cy - dy, 2.2, ANTIMATTER, alpha);
            if (t > 0.9) {
                double flash = (t - 0.9) / 0.1, r = 3 + 9 * flash;
                g.setFill(new RadialGradient(0, 0, cx, cy, r, false, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.color(1, 1, 1, 0.8 * (1 - flash))), new Stop(1, Color.TRANSPARENT)));
                g.fillOval(cx - r, cy - r, r * 2, r * 2);
            }
        }
    }

    /** Le trou noir : un disque noir cerclé d'un anneau orangé qui tourne, d'autant plus large que la masse est grande. */
    private void hole(GraphicsContext g, double cx, double cy) {
        double r = 10 + 3 * mass;
        g.setFill(new RadialGradient(0, 0, cx, cy, r * 3, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.color(1, 0.6, 0.23, 0.35)), new Stop(0.4, Color.color(1, 0.6, 0.23, 0.08)), new Stop(1, Color.TRANSPARENT)));
        g.fillOval(cx - r * 3, cy - r * 3, r * 6, r * 6);
        g.setStroke(Color.color(1, 0.75, 0.4, 0.75));
        g.setLineWidth(2);
        double spin = time * 40;
        g.strokeArc(cx - r * 1.6, cy - r * 0.55, r * 3.2, r * 1.1, spin % 360, 250, javafx.scene.shape.ArcType.OPEN);
        g.setFill(Color.BLACK);
        g.fillOval(cx - r, cy - r, r * 2, r * 2);
    }

    private static void dot(GraphicsContext g, double x, double y, double r, Color color, double alpha) {
        g.setFill(Color.color(color.getRed(), color.getGreen(), color.getBlue(), 0.25 * alpha));
        g.fillOval(x - r * 2.2, y - r * 2.2, r * 4.4, r * 4.4);
        g.setFill(Color.color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
        g.fillOval(x - r, y - r, r * 2, r * 2);
    }
}
