package idle.ui.common;

import idle.core.universe.Skin;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;

/**
 * Les animations plein écran des skins (version 0.89), par-dessus toute la fenêtre et sans prendre
 * les clics : l'explosion de matière noire sous « Vortex » ou « Pulsar », et le Big Bang sous
 * « Aurore », « Supernova » ou « Cristal ». Le classique garde ses animations à lui.
 *
 * <p>Elle n'est pas rangée par sa fenêtre : à chaque image, elle prend la taille de la fenêtre et
 * tout s'y dessine en proportion, du plus petit écran au plus grand.
 *
 * <ul>
 *   <li>Vortex : l'écran s'assombrit de violet ; huit bras de spirale tournent de plus en plus vite,
 *       aspirent des débris et s'ouvrent jusqu'aux coins, puis tout est avalé au centre.</li>
 *   <li>Pulsar : deux grands faisceaux balaient tout l'écran en tournant, des anneaux battent du
 *       centre jusqu'aux bords.</li>
 *   <li>Aurore : la nuit tombe, des rideaux de lumière verte, bleue et violette montent sur toute la
 *       hauteur et ondulent, puis se dissipent.</li>
 *   <li>Supernova : une étoile enfle au centre, éclate en un éclair qui blanchit l'écran, et sa
 *       coquille de débris part jusqu'aux bords.</li>
 *   <li>Cristal : l'écran se fêle à partir du centre, puis vole en éclats qui partent en tournant.</li>
 * </ul>
 */
public final class BurstView extends Canvas {

    private Skin skin;
    private double age;
    private double length;

    public BurstView() {
        super(10, 10);
        setMouseTransparent(true);
        setManaged(false);
        setVisible(false);
    }

    /** Joue l'animation de ce skin pendant {@code seconds} secondes. */
    public void play(Skin chosen, double seconds) {
        skin = chosen;
        age = 0;
        length = seconds;
        setVisible(true);
    }

    /** Vrai pendant une animation. */
    public boolean isPlaying() {
        return skin != null;
    }

    /** Le skin qui joue, ou {@code null} : pour les vérifications. */
    public Skin playing() {
        return skin;
    }

    /** Avance l'animation et la redessine, à la taille de la fenêtre. */
    public void frame(double dt, double width, double height) {
        if (skin == null) return;
        age += Math.min(dt, 0.1);
        // Avant la première mise en page, la fenêtre n'a pas encore de taille : une taille d'attente.
        if (!(width > 0)) width = 1040;
        if (!(height > 0)) height = 640;
        relocate(0, 0);
        if (getWidth() != width) setWidth(width);
        if (getHeight() != height) setHeight(height);
        GraphicsContext g = getGraphicsContext2D();
        g.setGlobalAlpha(1);
        g.clearRect(0, 0, width, height);
        if (age >= length) {
            skin = null;
            setVisible(false);
            return;
        }
        double t = age / length;
        double cx = width / 2, cy = height / 2, far = Math.hypot(cx, cy);
        switch (skin) {
            case DARK_VORTEX -> vortex(g, width, height, cx, cy, far, t);
            case DARK_PULSAR -> pulsar(g, width, height, cx, cy, far, t);
            case BANG_AURORA -> aurora(g, width, height, t);
            case BANG_SUPERNOVA -> supernova(g, width, height, cx, cy, far, t);
            case BANG_CRYSTAL -> crystal(g, width, height, cx, cy, far, t);
            default -> { }
        }
        g.setGlobalAlpha(1);
    }

    /** Un voile sur tout l'écran. */
    private static void veil(GraphicsContext g, double w, double h, Color color, double alpha) {
        g.setGlobalAlpha(Math.max(0, Math.min(1, alpha)));
        g.setFill(color);
        g.fillRect(0, 0, w, h);
        g.setGlobalAlpha(1);
    }

    /** Monte, tient, puis redescend : 0 au début et à la fin, 1 entre {@code in} et {@code out}. */
    private static double envelope(double t, double in, double out) {
        if (t < in) return t / in;
        if (t > out) return Math.max(0, (1 - t) / (1 - out));
        return 1;
    }

    private void vortex(GraphicsContext g, double w, double h, double cx, double cy, double far, double t) {
        double dark = envelope(t, 0.2, 0.85);
        veil(g, w, h, Color.web("#0d0518"), 0.75 * dark);
        // Les bras : ils s'ouvrent jusqu'aux coins pendant les deux premiers tiers, puis se referment d'un coup.
        double open = t < 0.7 ? Math.sqrt(t / 0.7) : 1 - (t - 0.7) / 0.3 * 0.9;
        double reach = far * 1.1 * open;
        double turn = 18 * t * t;
        int arms = 8;
        for (int arm = 0; arm < arms; arm++) {
            int steps = 80;
            double[] xs = new double[steps], ys = new double[steps];
            for (int k = 0; k < steps; k++) {
                double u = k / (double) (steps - 1);
                double a = turn + arm * 2 * Math.PI / arms + u * 4.2;
                xs[k] = cx + reach * u * Math.cos(a);
                ys[k] = cy + reach * u * Math.sin(a);
            }
            g.setStroke(arm % 2 == 0 ? Color.web("#7a3cff") : Color.web("#d06bff"));
            g.setLineWidth(Math.max(2, far * 0.06 * (1 - 0.5 * t)));
            g.setGlobalAlpha(0.35 * dark);
            g.strokePolyline(xs, ys, steps);
            g.setLineWidth(Math.max(1.5, far * 0.012));
            g.setGlobalAlpha(0.9 * dark);
            g.setStroke(Color.web("#f3e8ff"));
            g.strokePolyline(xs, ys, steps);
        }
        // Les débris aspirés : ils tournent en se rapprochant du centre.
        g.setFill(Color.web("#efe4ff"));
        for (int k = 0; k < 160; k++) {
            double seed = (k * 0.618034) % 1;
            double phase = (seed + t * 2.5) % 1;
            double r = far * (1 - phase) * 1.05;
            double a = k * 2.39996 + phase * 6 + turn * 0.5;
            double size = 1.5 + 3 * (1 - phase) * ((k * 0.381966) % 1) + far * 0.002;
            g.setGlobalAlpha(0.8 * dark * Math.sin(Math.PI * phase));
            g.fillOval(cx + r * Math.cos(a) - size / 2, cy + r * Math.sin(a) - size / 2, size, size);
        }
        // Le cœur du vortex : noir cerclé de lumière, qui grossit puis avale tout.
        double core = far * (0.05 + 0.1 * t + (t > 0.7 ? (t - 0.7) * 2 : 0));
        glow(g, cx, cy, core * 2.2, Color.web("#b07bff"), 0.9 * dark);
        g.setGlobalAlpha(dark);
        g.setFill(Color.web("#05010a"));
        g.fillOval(cx - core, cy - core, core * 2, core * 2);
        g.setStroke(Color.web("#f3e8ff"));
        g.setLineWidth(Math.max(1.5, far * 0.006));
        g.strokeOval(cx - core, cy - core, core * 2, core * 2);
    }

    private void pulsar(GraphicsContext g, double w, double h, double cx, double cy, double far, double t) {
        double on = envelope(t, 0.1, 0.85);
        veil(g, w, h, Color.web("#020a12"), 0.6 * on);
        double turn = 12 * t;
        for (int side = 0; side < 2; side++) {
            double a = turn + side * Math.PI;
            double spread = 0.12 + 0.06 * Math.sin(age * 9);
            double[] xs = {cx, cx + far * 1.2 * Math.cos(a - spread), cx + far * 1.2 * Math.cos(a + spread)};
            double[] ys = {cy, cy + far * 1.2 * Math.sin(a - spread), cy + far * 1.2 * Math.sin(a + spread)};
            g.setFill(Color.web("#3cf0ff"));
            g.setGlobalAlpha(0.35 * on);
            g.fillPolygon(xs, ys, 3);
            g.setStroke(Color.WHITE);
            g.setLineWidth(Math.max(2, far * 0.008));
            g.setGlobalAlpha(0.95 * on);
            g.strokeLine(cx, cy, cx + far * 1.2 * Math.cos(a), cy + far * 1.2 * Math.sin(a));
        }
        g.setStroke(Color.web("#bff8ff"));
        for (int k = 0; k < 6; k++) {
            double phase = (t * 7 + k / 6.0) % 1;
            double r = far * phase;
            g.setLineWidth(Math.max(0.5, far * 0.015 * (1 - phase)));
            g.setGlobalAlpha(0.85 * (1 - phase) * on);
            g.strokeOval(cx - r, cy - r, r * 2, r * 2);
        }
        glow(g, cx, cy, far * (0.06 + 0.1 * Math.abs(Math.sin(age * 14))), Color.WHITE, on);
    }

    private void aurora(GraphicsContext g, double w, double h, double t) {
        double on = envelope(t, 0.2, 0.75);
        veil(g, w, h, Color.web("#010812"), 0.8 * on);
        Color[] tints = {Color.web("#5dffa8"), Color.web("#6fc8ff"), Color.web("#c98bff"), Color.web("#5dffa8"), Color.web("#9bffd8")};
        int steps = 120;
        double rise = h * (1 - 1.1 * Math.min(1, t * 1.6));
        for (int band = 0; band < tints.length; band++) {
            for (int k = 0; k < steps; k++) {
                double x = w * k / (steps - 1);
                double top = rise + h * 0.14 * band + Math.sin(x / w * 6 + age * (1 + band * 0.25) + band) * h * 0.1
                        + Math.sin(x / w * 23 + age * 2 + band) * h * 0.02;
                double tall = h * (0.4 + 0.15 * Math.sin(x / w * 9 + band * 2 + age));
                g.setFill(new LinearGradient(0, top, 0, top + tall, false, CycleMethod.NO_CYCLE,
                        new Stop(0, colorWith(tints[band], 0)), new Stop(0.2, colorWith(tints[band], 0.4 * on)),
                        new Stop(1, colorWith(tints[band], 0))));
                g.setGlobalAlpha(1);
                g.fillRect(x - w / steps, top, w / steps * 2, tall);
            }
        }
        // Les étoiles de la nuit.
        g.setFill(Color.WHITE);
        for (int k = 0; k < 120; k++) {
            double x = w * ((k * 0.754877666) % 1), y = h * ((k * 0.569840291) % 1);
            g.setGlobalAlpha(on * (0.3 + 0.5 * Math.abs(Math.sin(age * 2 + k))));
            g.fillOval(x - 1, y - 1, 2, 2);
        }
    }

    private void supernova(GraphicsContext g, double w, double h, double cx, double cy, double far, double t) {
        double small = Math.min(w, h);
        veil(g, w, h, Color.web("#0d0306"), 0.7 * envelope(t, 0.1, 0.8));
        // L'étoile enfle pendant le premier quart, de plus en plus vite, en tremblant.
        if (t < 0.25) {
            double u = t / 0.25;
            double r = small * (0.04 + 0.12 * u * u) * (1 + 0.05 * Math.sin(age * 40));
            glow(g, cx, cy, r * 3, Color.web("#ff8a2a"), 0.9);
            g.setGlobalAlpha(1);
            g.setFill(Color.web("#fff6d8"));
            g.fillOval(cx - r, cy - r, r * 2, r * 2);
            return;
        }
        double u = (t - 0.25) / 0.75;
        // L'éclair : l'écran blanchit d'un coup, puis s'éteint.
        veil(g, w, h, Color.web("#fff3d8"), Math.max(0, 0.9 - u * 3));
        glow(g, cx, cy, far * (0.3 + 1.0 * u), Color.web("#ff8a2a"), 0.9 * (1 - u));
        // Les branches de l'éclat, jusqu'aux bords.
        g.setFill(Color.web("#fff3b0"));
        int spikes = 16;
        for (int k = 0; k < spikes; k++) {
            double a = k * 2 * Math.PI / spikes + 0.2;
            double len = far * (0.4 + 0.9 * u) * (k % 2 == 0 ? 1 : 0.55);
            double base = small * 0.025;
            g.setGlobalAlpha(0.85 * (1 - u));
            g.fillPolygon(new double[] {cx + base * Math.cos(a + 1.5), cx + len * Math.cos(a), cx + base * Math.cos(a - 1.5)},
                    new double[] {cy + base * Math.sin(a + 1.5), cy + len * Math.sin(a), cy + base * Math.sin(a - 1.5)}, 3);
        }
        // La coquille de débris, qui part jusqu'aux coins.
        double shell = far * (0.1 + 1.0 * u);
        for (int k = 0; k < 220; k++) {
            double a = k * 2.39996;
            double r = shell * (0.85 + 0.2 * ((k * 0.618034) % 1));
            g.setFill(k % 3 == 0 ? Color.web("#ff5a3c") : k % 3 == 1 ? Color.web("#ffd27f") : Color.web("#d15aff"));
            g.setGlobalAlpha(0.9 * (1 - u));
            double size = small * (0.004 + 0.008 * ((k * 0.381966) % 1));
            g.fillOval(cx + r * Math.cos(a) - size, cy + r * Math.sin(a) - size, size * 2, size * 2);
        }
        g.setStroke(Color.web("#ff9a3c"));
        g.setLineWidth(Math.max(2, small * 0.01 * (1 - u)));
        g.setGlobalAlpha(0.7 * (1 - u));
        g.strokeOval(cx - shell, cy - shell, shell * 2, shell * 2);
    }

    private void crystal(GraphicsContext g, double w, double h, double cx, double cy, double far, double t) {
        double small = Math.min(w, h);
        // D'abord l'écran se fêle : des fissures qui partent du centre et se ramifient jusqu'aux bords.
        double crack = Math.min(1, t / 0.4);
        double fade = t > 0.75 ? (1 - t) / 0.25 : 1;
        veil(g, w, h, Color.web("#06101e"), 0.55 * fade);
        g.setStroke(Color.web("#e8f8ff"));
        int cracks = 14;
        for (int k = 0; k < cracks; k++) {
            double a = 2 * Math.PI * k / cracks + 0.3 * Math.sin(k * 7.1);
            double x = cx, y = cy;
            int segments = 8;
            for (int s = 1; s <= segments; s++) {
                if (s > segments * crack) break;
                double r = far * s / segments;
                double wobble = 0.12 * Math.sin(k * 3.3 + s * 2.1);
                double nx = cx + r * Math.cos(a + wobble), ny = cy + r * Math.sin(a + wobble);
                g.setLineWidth(Math.max(1, small * 0.004 * (1 - s / (double) segments) + 0.8));
                g.setGlobalAlpha(0.9 * fade);
                g.strokeLine(x, y, nx, ny);
                if (s % 3 == 0) {
                    double b = a + wobble + (k % 2 == 0 ? 0.5 : -0.5);
                    g.setLineWidth(1);
                    g.strokeLine(nx, ny, nx + far * 0.12 * Math.cos(b), ny + far * 0.12 * Math.sin(b));
                }
                x = nx;
                y = ny;
            }
        }
        if (t < 0.4) {
            glow(g, cx, cy, small * 0.2 * crack, Color.web("#dff4ff"), 0.8);
            return;
        }
        // Puis tout vole en éclats : des morceaux de verre de toutes tailles, qui partent en tournant.
        double u = (t - 0.4) / 0.6;
        veil(g, w, h, Color.WHITE, Math.max(0, 0.6 - u * 2.5));
        for (int k = 0; k < 120; k++) {
            double a = k * 2.39996;
            double start = far * 0.9 * ((k * 0.618034) % 1);
            double r = start + far * 0.8 * u * (0.5 + ((k * 0.381966) % 1));
            double x = cx + r * Math.cos(a), y = cy + r * Math.sin(a);
            double spin = age * (2 + k % 5) + k;
            double size = small * (0.02 + 0.05 * ((k * 0.2928932) % 1));
            double[] xs = {x + size * Math.cos(spin), x + size * 0.4 * Math.cos(spin + 2.1), x + size * 0.7 * Math.cos(spin + 3.7), x + size * 0.3 * Math.cos(spin + 5)};
            double[] ys = {y + size * Math.sin(spin), y + size * 0.4 * Math.sin(spin + 2.1), y + size * 0.7 * Math.sin(spin + 3.7), y + size * 0.3 * Math.sin(spin + 5)};
            g.setFill(k % 3 == 0 ? Color.web("#e8f8ff") : k % 3 == 1 ? Color.web("#9fd8ff") : Color.web("#c9b8ff"));
            g.setGlobalAlpha(0.75 * fade);
            g.fillPolygon(xs, ys, 4);
            g.setStroke(Color.WHITE);
            g.setLineWidth(1);
            g.setGlobalAlpha(0.9 * fade);
            g.strokeLine(xs[0], ys[0], xs[2], ys[2]);
        }
    }

    private static Color colorWith(Color c, double alpha) {
        return Color.color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(1, alpha)));
    }

    private static void glow(GraphicsContext g, double x, double y, double size, Color color, double strength) {
        if (!(size > 0)) return;
        g.setGlobalAlpha(1);
        g.setFill(new RadialGradient(0, 0, x, y, size, false, CycleMethod.NO_CYCLE,
                new Stop(0, colorWith(color, strength)), new Stop(1, colorWith(color, 0))));
        g.fillOval(x - size, y - size, 2 * size, 2 * size);
    }
}
