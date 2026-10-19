package idle.ui.universe;

import java.util.function.IntConsumer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

/**
 * La carte des dimensions (version 0.81) : dix points en cercle, et un onzième au milieu, relié à
 * tous. Les dimensions ouvertes brillent, leur numéro et leur nom dessous ; les autres restent
 * éteintes, un point d'interrogation dedans. Des éclats partent du milieu vers celles qui sont
 * ouvertes. Un clic sur une dimension ouverte l'ouvre dans sa fenêtre.
 */
public final class DimensionsView extends Canvas {

    private static final Color BACK = Color.web("#05060e");
    private static final Color LINE = Color.web("#a8b8ff");
    private static final Color ON = Color.web("#d6c2ff");
    private static final Color OFF = Color.web("#3a4160");
    private static final Color CENTER = Color.web("#ffe2b0");

    private double time;
    private int unlocked;
    private IntConsumer onOpen = number -> { };

    public DimensionsView() {
        super(800, 440);
        setOnMouseClicked(event -> {
            int number = dimensionAt(event.getX(), event.getY());
            if (number > 0 && number <= unlocked) onOpen.accept(number);
        });
    }

    /** Ce que fait un clic sur une dimension ouverte : son numéro, de 1 à 10. */
    public void setOnOpen(IntConsumer open) {
        onOpen = open == null ? number -> { } : open;
    }

    /** Le nombre de dimensions ouvertes (0 à 10). */
    public void setUnlocked(int count) {
        unlocked = Math.max(0, Math.min(Dimension.values().length, count));
    }

    public int unlocked() {
        return unlocked;
    }

    private double cx() {
        return getWidth() / 2;
    }

    private double cy() {
        return getHeight() / 2;
    }

    private double radius() {
        return Math.min(getWidth() * 0.4, getHeight() * 0.36);
    }

    /** La place du point de cette dimension (1 à 10) : en cercle, la première en haut. */
    public double x(int number) {
        return cx() + radius() * Math.cos(angle(number));
    }

    public double y(int number) {
        return cy() + radius() * Math.sin(angle(number));
    }

    private static double angle(int number) {
        return -Math.PI / 2 + 2 * Math.PI * (number - 1) / Dimension.values().length;
    }

    private double dot() {
        return Math.max(10, Math.min(getWidth(), getHeight()) * 0.035);
    }

    /** Le numéro de la dimension sous ce point de la vue, 0 s'il n'y en a pas. */
    public int dimensionAt(double x, double y) {
        for (int number = 1; number <= Dimension.values().length; number++) {
            if (Math.hypot(x - x(number), y - y(number)) <= dot() * 1.8) return number;
        }
        return 0;
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
        g.setFill(Color.web("#dfe6ff"));
        for (int i = 0; i < 120; i++) {
            g.setGlobalAlpha(0.1 + 0.25 * (0.5 + 0.5 * Math.sin(time * 0.5 + i)));
            g.fillOval(UniverseView.noise(i * 2 + 5) * w, UniverseView.noise(i * 2 + 6) * h, 1.3, 1.3);
        }
        int count = Dimension.values().length;
        double dot = dot();
        // Le cercle, d'un point au suivant, et les rayons du milieu vers chacun.
        for (int number = 1; number <= count; number++) {
            int next = number % count + 1;
            boolean both = number <= unlocked && next <= unlocked;
            g.setStroke(LINE);
            g.setLineWidth(1);
            g.setGlobalAlpha(both ? 0.35 : 0.1);
            g.strokeLine(x(number), y(number), x(next), y(next));
            boolean open = number <= unlocked;
            g.setLineWidth(open ? 2 : 1);
            g.setGlobalAlpha(open ? 0.45 : 0.12);
            g.strokeLine(cx(), cy(), x(number), y(number));
            if (open) {
                // Un éclat qui va du milieu vers la dimension.
                double t = (time * 0.4 + number * 0.13) % 1;
                UniverseView.glow(g, cx() + (x(number) - cx()) * t, cy() + (y(number) - cy()) * t, 5, ON, 0.8);
            }
        }
        // Le point du milieu, qui les relie tous.
        UniverseView.glow(g, cx(), cy(), dot * 3.2, CENTER, 0.35 + 0.1 * Math.sin(time * 2));
        g.setGlobalAlpha(1);
        g.setFill(CENTER);
        g.fillOval(cx() - dot * 0.9, cy() - dot * 0.9, dot * 1.8, dot * 1.8);
        // Les dix points.
        g.setTextAlign(TextAlignment.CENTER);
        for (int number = 1; number <= count; number++) {
            double x = x(number), y = y(number);
            boolean open = number <= unlocked;
            if (open) UniverseView.glow(g, x, y, dot * 2.4, ON, 0.4 + 0.15 * Math.sin(time * 2 + number));
            g.setGlobalAlpha(1);
            g.setFill(open ? ON : OFF);
            g.fillOval(x - dot, y - dot, 2 * dot, 2 * dot);
            g.setFill(open ? Color.web("#1a1430") : Color.web("#8a91b0"));
            g.setFont(Font.font(dot * 1.05));
            g.fillText(open ? String.valueOf(number) : "?", x, y + dot * 0.38);
            // Le nom, à l'extérieur du cercle.
            double out = 1 + (dot * 2.6) / radius();
            double lx = cx() + (x - cx()) * out, ly = cy() + (y - cy()) * out + 4;
            g.setFont(Font.font(12));
            g.setFill(open ? ON : OFF);
            g.fillText(open ? Dimension.of(number).label() : "Dimension " + number, lx, ly);
        }
        g.setTextAlign(TextAlignment.LEFT);
        g.setGlobalAlpha(1);
    }
}
