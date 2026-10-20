package idle.ui.particles;

import java.util.Random;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Fond animé : des poussières qui dérivent lentement en scintillant.
 * C'est la matière brute, visible dès l'écran de démarrage, que les générateurs attirent ensuite.
 * Depuis la version 0.061, elles suivent la production : plus elle est forte, plus elles filent
 * vite, en traînées, et se teintent comme les générateurs.
 */
public final class AmbientView extends Canvas {

    private static final int MOTE_COUNT = 90;
    private static final Color MOTE = Color.web("#9fd0ff");

    // Positions en fraction de la vue (0 à 1), pour suivre le redimensionnement de la fenêtre.
    private final double[] x = new double[MOTE_COUNT];
    private final double[] y = new double[MOTE_COUNT];
    private final double[] dx = new double[MOTE_COUNT];
    private final double[] dy = new double[MOTE_COUNT];
    private final double[] size = new double[MOTE_COUNT];
    private final double[] phase = new double[MOTE_COUNT];
    private double time = 0;
    /** La production ramenée de 0 à 1, adoucie. */
    private double intensity = 0;
    private double wanted = 0;

    /** La production du moment, de 0 à 1 ({@link ParticleView#intensity(double)}). */
    void setIntensity(double intensity) {
        wanted = Math.max(0, Math.min(1, intensity));
    }

    AmbientView() {
        super(1, 1);
        Random random = new Random();
        for (int i = 0; i < MOTE_COUNT; i++) {
            x[i] = random.nextDouble();
            y[i] = random.nextDouble();
            dx[i] = (random.nextDouble() - 0.5) * 0.02;   // au plus 1 % de la vue par seconde
            dy[i] = (random.nextDouble() - 0.5) * 0.02;
            size[i] = 1 + 1.5 * random.nextDouble();
            phase[i] = random.nextDouble() * 2 * Math.PI;
        }
    }

    /** Avance l'animation de {@code elapsed} secondes et redessine. */
    void frame(double elapsed) {
        double dt = Math.min(elapsed, 0.1);
        time += dt;
        intensity += (wanted - intensity) * Math.min(1, dt);
        double speed = 1 + 9 * intensity * intensity;

        GraphicsContext g = getGraphicsContext2D();
        g.setGlobalAlpha(1);
        g.clearRect(0, 0, getWidth(), getHeight());
        Color color = MOTE.interpolate(ParticleView.heat(intensity), 0.7);
        g.setFill(color);
        g.setStroke(color);
        for (int i = 0; i < MOTE_COUNT; i++) {
            x[i] = wrap(x[i] + dx[i] * dt * speed);
            y[i] = wrap(y[i] + dy[i] * dt * speed);
            double alpha = 0.10 + 0.22 * (0.5 + 0.5 * Math.sin(phase[i] + time * 1.3 * speed));
            double px = x[i] * getWidth(), py = y[i] * getHeight();
            if (intensity > 0.3) {
                // Vite : une traînée derrière chaque poussière, dans le sens de sa course.
                double tail = 0.6 * intensity;
                g.setGlobalAlpha(alpha * 0.5);
                g.setLineWidth(size[i] * 0.6);
                g.strokeLine(px, py, px - dx[i] * speed * tail * getWidth(), py - dy[i] * speed * tail * getHeight());
            }
            g.setGlobalAlpha(alpha);
            g.fillOval(px - size[i] / 2, py - size[i] / 2, size[i], size[i]);
        }
        g.setGlobalAlpha(1);
    }

    /** Une poussière qui sort d'un côté rentre par l'autre. */
    private static double wrap(double value) {
        return value - Math.floor(value);
    }
}
