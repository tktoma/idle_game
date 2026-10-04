package idle.ui;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Grand atome de l'onglet « Atomes » : un noyau entouré d'orbes qui tournent sur des
 * couches concentriques. Il y a une orbe par atome disponible : une orbe apparaît à chaque
 * atome gagné et disparaît à chaque atome dépensé.
 *
 * <p>Les couches se remplissent de l'intérieur vers l'extérieur, avec les capacités des
 * couches électroniques du dernier élément du tableau périodique : 2, 8, 18, 32, 32, 18, 8,
 * soit 118 orbes au maximum.
 */
final class AtomModelView extends Canvas {

    /** Nombre d'orbes que peut porter chaque couche, de la plus proche du noyau à la plus lointaine. */
    private static final int[] SHELL_CAPACITIES = {2, 8, 18, 32, 32, 18, 8};

    /** Nombre maximal d'orbes : le nombre d'éléments du tableau périodique. */
    static final int MAX_ORBS = 118;

    /** Durée de l'apparition d'une nouvelle orbe, en secondes. */
    private static final double POP_SECONDS = 0.9;

    private static final Color NUCLEUS = Color.web("#ffd27f");
    private static final Color SHELL = Color.web("#ffe9c2");
    private static final Color ORB = Color.web("#fff4dd");

    private int orbs = 0;
    private double time = 0;
    private double sinceLastOrb = POP_SECONDS; // temps écoulé depuis l'arrivée de la dernière orbe

    /** @param size côté de la vue, en pixels */
    AtomModelView(double size) {
        super(size, size);
    }

    /** Fixe le nombre d'orbes (plafonné à {@link #MAX_ORBS}). Une orbe ajoutée joue son apparition. */
    void setOrbs(int count) {
        int capped = Math.max(0, Math.min(MAX_ORBS, count));
        if (capped > orbs) sinceLastOrb = 0;
        orbs = capped;
    }

    /** Avance l'animation de {@code elapsed} secondes et redessine. */
    void frame(double elapsed) {
        double dt = Math.min(elapsed, 0.1);
        time += dt;
        sinceLastOrb += dt;

        GraphicsContext g = getGraphicsContext2D();
        double size = getWidth();
        double center = size / 2;
        double nucleus = size * 0.07;

        g.setGlobalAlpha(1);
        g.clearRect(0, 0, size, size);

        // Les couches occupées se partagent l'espace entre le noyau et le bord. Avec peu de
        // couches, l'atome reste compact ; à partir de quatre, il occupe toute la vue.
        int shells = occupiedShells();
        double innerRadius = nucleus * 1.6;
        double outerRadius = innerRadius + (size * 0.46 - innerRadius) * Math.min(1, shells / 4.0);
        double orbRadius = Math.max(2, size * 0.011);

        int remaining = orbs;
        for (int shell = 0; shell < shells; shell++) {
            int onShell = Math.min(remaining, SHELL_CAPACITIES[shell]);
            remaining -= onShell;
            double radius = innerRadius + (outerRadius - innerRadius) * (shell + 1) / shells;

            g.setStroke(SHELL);
            g.setLineWidth(1);
            g.setGlobalAlpha(0.3);
            g.strokeOval(center - radius, center - radius, radius * 2, radius * 2);

            // Chaque couche tourne à sa vitesse, une sur deux dans l'autre sens.
            double direction = shell % 2 == 0 ? 1 : -1;
            double rotation = direction * time * 0.9 / (shell + 1) + shell * 0.7;
            boolean lastShell = remaining == 0;
            for (int i = 0; i < onShell; i++) {
                double angle = rotation + 2 * Math.PI * i / onShell;
                double x = center + radius * Math.cos(angle);
                double y = center + radius * Math.sin(angle);
                boolean newest = lastShell && i == onShell - 1 && sinceLastOrb < POP_SECONDS;
                if (newest) {
                    // La dernière orbe arrivée : un halo qui se resserre sur elle.
                    double t = sinceLastOrb / POP_SECONDS;
                    double halo = orbRadius * (1 + 5 * (1 - t));
                    g.setFill(NUCLEUS);
                    g.setGlobalAlpha(0.5 * (1 - t));
                    g.fillOval(x - halo, y - halo, halo * 2, halo * 2);
                }
                g.setFill(ORB);
                g.setGlobalAlpha(1);
                g.fillOval(x - orbRadius, y - orbRadius, orbRadius * 2, orbRadius * 2);
            }
        }

        // Le noyau, avec un halo qui respire doucement.
        double halo = nucleus * (1.5 + 0.12 * Math.sin(time * 3));
        g.setFill(NUCLEUS);
        g.setGlobalAlpha(0.2);
        g.fillOval(center - halo, center - halo, halo * 2, halo * 2);
        g.setGlobalAlpha(1);
        g.fillOval(center - nucleus, center - nucleus, nucleus * 2, nucleus * 2);
    }

    /** Nombre de couches qui portent au moins une orbe. */
    private int occupiedShells() {
        int remaining = orbs;
        int shells = 0;
        while (remaining > 0 && shells < SHELL_CAPACITIES.length) {
            remaining -= SHELL_CAPACITIES[shells];
            shells++;
        }
        return shells;
    }
}