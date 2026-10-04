package idle.ui;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Icône animée d'un atome : un noyau doré et trois électrons sur des orbites inclinées.
 * Elle représente la deuxième ressource du jeu, à côté de son compteur.
 */
final class AtomView extends Canvas {

    /** Inclinaison de chaque orbite, en degrés. */
    private static final double[] ORBIT_TILTS = {0, 60, 120};

    private static final Color NUCLEUS = Color.web("#ffd27f");
    private static final Color ORBIT = Color.web("#ffe9c2");
    private static final Color ELECTRON = Color.web("#fff4dd");

    private double time = 0;

    /** @param size côté de l'icône, en pixels */
    AtomView(double size) {
        super(size, size);
    }

    /** Avance l'animation de {@code elapsed} secondes et redessine. */
    void frame(double elapsed) {
        time += Math.min(elapsed, 0.1);

        GraphicsContext g = getGraphicsContext2D();
        double size = getWidth();
        double center = size / 2;
        double longRadius = size * 0.45;   // demi-grand axe des orbites
        double shortRadius = size * 0.17;  // demi-petit axe
        double electron = Math.max(1.5, size * 0.045);

        g.setGlobalAlpha(1);
        g.clearRect(0, 0, size, size);

        for (int i = 0; i < ORBIT_TILTS.length; i++) {
            // L'orbite : une ellipse tournée autour du centre.
            g.save();
            g.translate(center, center);
            g.rotate(ORBIT_TILTS[i]);
            g.setStroke(ORBIT);
            g.setLineWidth(1);
            g.setGlobalAlpha(0.45);
            g.strokeOval(-longRadius, -shortRadius, longRadius * 2, shortRadius * 2);
            g.restore();

            // L'électron : un point qui parcourt cette ellipse.
            double angle = time * 2.4 + i * 2.1;
            double x = longRadius * Math.cos(angle);
            double y = shortRadius * Math.sin(angle);
            double tilt = Math.toRadians(ORBIT_TILTS[i]);
            double ex = center + x * Math.cos(tilt) - y * Math.sin(tilt);
            double ey = center + x * Math.sin(tilt) + y * Math.cos(tilt);
            g.setGlobalAlpha(1);
            g.setFill(ELECTRON);
            g.fillOval(ex - electron, ey - electron, electron * 2, electron * 2);
        }

        // Le noyau, avec un halo qui respire doucement.
        double nucleus = size * 0.11;
        double halo = nucleus * (2.1 + 0.25 * Math.sin(time * 3));
        g.setFill(NUCLEUS);
        g.setGlobalAlpha(0.2);
        g.fillOval(center - halo, center - halo, halo * 2, halo * 2);
        g.setGlobalAlpha(1);
        g.fillOval(center - nucleus, center - nucleus, nucleus * 2, nucleus * 2);
    }
}
