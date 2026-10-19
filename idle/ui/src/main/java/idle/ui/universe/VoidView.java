package idle.ui.universe;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Le Vide (version 0.81), le deuxième acte de la partie secondaire, pour l'instant un simple
 * visuel : un noir presque parfait où des paires de grains naissent, s'écartent un instant et
 * s'annulent, comme l'écume du vide.
 */
public final class VoidView extends Canvas {

    private static final Color BACK = Color.web("#020208");
    private static final Color A = Color.web("#9fe6ff");
    private static final Color B = Color.web("#ff9fd6");
    private static final int PAIRS = 70;

    private double time;

    public VoidView() {
        super(800, 400);
    }

    public void frame(double elapsed, boolean moving) {
        if (moving) time += Math.min(elapsed, 0.1);
        draw();
    }

    private void draw() {
        double w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        GraphicsContext g = getGraphicsContext2D();
        g.setGlobalAlpha(1);
        g.clearRect(0, 0, w, h);
        g.setFill(BACK);
        g.fillRect(0, 0, w, h);
        for (int k = 0; k < PAIRS; k++) {
            double period = 1.5 + 2.5 * UniverseView.noise(k * 5 + 1);
            double phase = ((time + UniverseView.noise(k * 5 + 2) * period) % period) / period;
            // Chaque cycle, la paire réapparaît ailleurs.
            int cycle = (int) Math.floor((time + UniverseView.noise(k * 5 + 2) * period) / period);
            double x = w * UniverseView.noise(k * 97 + cycle * 13 + 3), y = h * UniverseView.noise(k * 97 + cycle * 13 + 4);
            double a = 2 * Math.PI * UniverseView.noise(k * 97 + cycle * 13 + 5);
            double apart = Math.sin(phase * Math.PI) * 10;
            double alpha = Math.sin(phase * Math.PI);
            UniverseView.glow(g, x + apart * Math.cos(a), y + apart * Math.sin(a), 4, A, 0.8 * alpha);
            UniverseView.glow(g, x - apart * Math.cos(a), y - apart * Math.sin(a), 4, B, 0.8 * alpha);
        }
        g.setGlobalAlpha(1);
    }
}
