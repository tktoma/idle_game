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
 * L'écume quantique du vide (version 0.099). Le vide n'est jamais tout à fait vide : des paires de
 * particules y naissent d'un point, s'écartent un instant, puis se rejoignent et s'annulent dans un
 * petit éclair. Derrière, le champ ondule doucement. Rien ne s'y gagne encore : c'est un visuel.
 */
public final class VoidView extends Canvas {

    private static final Color BACK = Color.web("#03060c");
    private static final Color GLOW = Color.web(VoidPage.COLOR);
    private static final Color MATTER = Color.web(VoidPage.COLOR);
    private static final Color ANTIMATTER = Color.web("#d9a6ff");
    /** Paires vivantes à la fois. */
    private static final int PAIRS = 60;
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

    VoidView() {
        super(780, 360);
        for (int i = 0; i < SWELLS; i++) {
            swellX[i] = random.nextDouble();
            swellY[i] = random.nextDouble();
            swellPhase[i] = random.nextDouble() * Math.PI * 2;
        }
        // Des paires déjà en cours, à des âges différents : l'écume est là dès la première image.
        for (int i = 0; i < PAIRS; i++) {
            Pair pair = born();
            pair.age = random.nextDouble() * pair.life;
            pairs.add(pair);
        }
        draw();
    }

    private Pair born() {
        Pair pair = new Pair();
        pair.x = random.nextDouble();
        pair.y = random.nextDouble();
        pair.angle = random.nextDouble() * Math.PI * 2;
        pair.reach = 6 + random.nextDouble() * 16;
        pair.life = 1.1 + random.nextDouble() * 1.6;
        return pair;
    }

    /** Une image : l'écume avance si l'animation est allumée, sinon elle reste figée. */
    public void frame(double elapsed, boolean motion) {
        if (motion) {
            double dt = Math.min(elapsed, 0.1);
            time += dt;
            for (int i = 0; i < pairs.size(); i++) {
                Pair pair = pairs.get(i);
                pair.age += dt;
                if (pair.age >= pair.life) pairs.set(i, born());
            }
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

    private static void dot(GraphicsContext g, double x, double y, double r, Color color, double alpha) {
        g.setFill(Color.color(color.getRed(), color.getGreen(), color.getBlue(), 0.25 * alpha));
        g.fillOval(x - r * 2.2, y - r * 2.2, r * 4.4, r * 4.4);
        g.setFill(Color.color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
        g.fillOval(x - r, y - r, r * 2, r * 2);
    }
}
