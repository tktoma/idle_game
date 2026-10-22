package idle.ui.universe;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Le lancement de l'univers suivant (version 0.111), par-dessus la page Univers : un point de lumière
 * naît au centre, gonfle en une petite sphère, éclate en anneaux, et une gerbe de particules monte vers
 * les onglets, là où l'onglet « Particules » réapparaît. Le fond reste transparent : la page se voit derrière.
 *
 * <p>L'animation avance par {@link #frame(double)} et non par les transitions de JavaFX : au moment de
 * l'éclat ({@link #LAUNCH_AT}), elle lance l'univers par le {@link Runnable} reçu, une seule fois.
 */
public final class LaunchView extends Canvas {

    /** Durée de toute l'animation, en secondes. */
    public static final double SECONDS = 1.8;
    /** Le moment où l'univers se lance : la sphère éclate. */
    public static final double LAUNCH_AT = 0.75;

    private static final Color CORE = Color.web("#fff6d2");
    private static final Color GLOW = Color.web("#9fd0ff");
    private static final Color RING = Color.web("#c9a8ff");
    private static final int SPARKS = 48;

    private double age = -1;
    private Runnable onLaunch = () -> { };
    private boolean launched;

    public LaunchView() {
        super(800, 400);
        setMouseTransparent(true);
    }

    /** Commence l'animation ; {@code launch} est appelé à l'éclat. */
    public void play(Runnable launch) {
        onLaunch = launch == null ? () -> { } : launch;
        launched = false;
        age = 0;
    }

    /** Vrai tant que l'animation se joue. */
    public boolean isPlaying() {
        return age >= 0;
    }

    /** Vrai une fois l'éclat passé : pour les vérifications. */
    public boolean hasLaunched() {
        return launched;
    }

    /** Fait avancer l'animation (au plus un dixième de seconde à la fois, pour ne pas sauter l'éclat). */
    public void frame(double elapsed) {
        if (age < 0) return;
        age += Math.min(Math.max(0, elapsed), 0.1);
        if (!launched && age >= LAUNCH_AT) {
            launched = true;
            onLaunch.run();
        }
        if (age >= SECONDS) age = -1;
        draw();
    }

    private void draw() {
        double w = getWidth(), h = getHeight();
        GraphicsContext g = getGraphicsContext2D();
        g.clearRect(0, 0, w, h);
        if (age < 0) return;
        double cx = w / 2, cy = h / 2, size = Math.min(w, h);
        if (age < LAUNCH_AT) {
            // Le point de lumière gonfle, de plus en plus vite.
            double t = age / LAUNCH_AT, r = size * (0.01 + 0.07 * t * t);
            g.setGlobalAlpha(0.35 * t);
            g.setFill(GLOW);
            g.fillOval(cx - r * 2.2, cy - r * 2.2, r * 4.4, r * 4.4);
            g.setGlobalAlpha(0.5 + 0.5 * t);
            g.setFill(CORE);
            g.fillOval(cx - r, cy - r, r * 2, r * 2);
        } else {
            double t = (age - LAUNCH_AT) / (SECONDS - LAUNCH_AT), fade = 1 - t;
            // L'éclair, puis les anneaux qui s'écartent.
            g.setGlobalAlpha(Math.max(0, 0.45 - t * 1.5));
            g.setFill(CORE);
            g.fillRect(0, 0, w, h);
            g.setLineWidth(3);
            for (int k = 0; k < 3; k++) {
                double r = size * (0.08 + (0.6 + 0.2 * k) * Math.pow(t, 0.6 + 0.2 * k));
                g.setGlobalAlpha(Math.max(0, fade * (0.8 - 0.2 * k)));
                g.setStroke(k == 1 ? RING : GLOW);
                g.strokeOval(cx - r, cy - r, r * 2, r * 2);
            }
            // La gerbe : les particules montent vers les onglets, en s'ouvrant.
            for (int i = 0; i < SPARKS; i++) {
                double spread = UniverseView.noise(i * 7 + 3) - 0.5, speed = 0.6 + 0.6 * UniverseView.noise(i * 7 + 4);
                double rise = t * speed;
                double x = cx + spread * w * 0.9 * rise, y = cy - rise * (cy + 20);
                double r = 1.5 + 2.5 * UniverseView.noise(i * 7 + 5);
                g.setGlobalAlpha(Math.max(0, fade));
                g.setFill(i % 3 == 0 ? RING : i % 3 == 1 ? GLOW : CORE);
                g.fillOval(x - r, y - r, r * 2, r * 2);
            }
        }
        g.setGlobalAlpha(1);
    }
}
