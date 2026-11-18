package idle.ui;

import java.util.ArrayList;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Le rond d'aide au clic (version 0.261) : à chaque clic, un rond s'élargit et s'efface là où l'on a cliqué, pour voir
 * chaque clic (réglage « Rond au clic » des effets). Un calque transparent à la souris, au-dessus de tout.
 *
 * <p>Il dit aussi si un clic a déjà son propre son ({@link #hasOwnSound(Object, Object)}) : un onglet, ou le mini-jeu
 * d'une dimension. Tout autre clic fait le son du clic (version 0.262).
 */
final class ClickRings extends Canvas {
    /** La vie d'un rond, en secondes. */
    static final double LIFE = 0.4;
    /** Le rayon d'un rond à sa naissance et à sa fin, en pixels. */
    static final double FROM = 4, TO = 22;
    private static final int MAX_RINGS = 24;
    /** Les ronds : x, y, âge en secondes. */
    private final List<double[]> rings = new ArrayList<>();

    ClickRings() { setMouseTransparent(true); }

    /** Un clic là (en coordonnées du calque) : un rond de plus. */
    void add(double x, double y) {
        rings.add(new double[] {x, y, 0});
        while (rings.size() > MAX_RINGS) rings.remove(0);
    }

    /** Les ronds encore visibles. */
    int rings() { return rings.size(); }

    /** Prend la taille de la fenêtre. */
    void fit(double width, double height) {
        setWidth(width);
        setHeight(height);
    }

    /** Vieillit les ronds et les redessine : chacun s'élargit et s'efface, un trait sombre sous un trait clair (lisible sur tout thème). */
    void frame(double dt) {
        GraphicsContext g = getGraphicsContext2D();
        if (rings.isEmpty()) return;
        for (double[] ring : rings) ring[2] += dt;
        rings.removeIf(ring -> ring[2] >= LIFE);
        g.clearRect(0, 0, getWidth(), getHeight());
        for (double[] ring : rings) {
            double t = ring[2] / LIFE, radius = FROM + (TO - FROM) * (1 - (1 - t) * (1 - t)), fade = 1 - t;
            g.setLineWidth(3);
            g.setStroke(Color.color(0, 0, 0, 0.35 * fade));
            g.strokeOval(ring[0] - radius, ring[1] - radius, 2 * radius, 2 * radius);
            g.setLineWidth(1.5);
            g.setStroke(Color.color(1, 1, 1, 0.85 * fade));
            g.strokeOval(ring[0] - radius, ring[1] - radius, 2 * radius, 2 * radius);
        }
    }

    /**
     * Vrai si un clic sur {@code target} a déjà son propre son à l'appui, et ne doit pas faire le clic en plus (version
     * 0.262) : un onglet (marqué de la propriété {@code tabKey}), ou le mini-jeu d'une dimension (le clicker, la boucle…,
     * dont chaque geste sonne). Tout autre clic fait le son du clic, sur rien comme sur ce qui déclenche une action.
     */
    static boolean hasOwnSound(Object target, Object tabKey) {
        if (!(target instanceof Node node)) return false;
        for (Node at = node; at != null; at = at.getParent()) {
            if (at.getProperties().containsKey(tabKey) || at instanceof idle.ui.universe.DimensionView) return true;
        }
        return false;
    }
}
