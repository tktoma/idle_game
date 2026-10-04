package idle.ui;

import java.util.Random;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Fond animé : des poussières qui dérivent lentement en scintillant.
 * C'est la matière brute, visible dès l'écran de démarrage, que les générateurs attirent ensuite.
 */
final class AmbientView extends Canvas {

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

        GraphicsContext g = getGraphicsContext2D();
        g.setGlobalAlpha(1);
        g.clearRect(0, 0, getWidth(), getHeight());
        g.setFill(MOTE);
        for (int i = 0; i < MOTE_COUNT; i++) {
            x[i] = wrap(x[i] + dx[i] * dt);
            y[i] = wrap(y[i] + dy[i] * dt);
            g.setGlobalAlpha(0.10 + 0.22 * (0.5 + 0.5 * Math.sin(phase[i] + time * 1.3)));
            g.fillOval(x[i] * getWidth() - size[i] / 2, y[i] * getHeight() - size[i] / 2, size[i], size[i]);
        }
        g.setGlobalAlpha(1);
    }

    /** Une poussière qui sort d'un côté rentre par l'autre. */
    private static double wrap(double value) {
        return value - Math.floor(value);
    }
}
