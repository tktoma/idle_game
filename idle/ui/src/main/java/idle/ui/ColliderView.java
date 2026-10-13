package idle.ui;

import idle.core.Collision;
import idle.core.Game;
import java.util.List;
import java.util.function.IntConsumer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

/**
 * Le collisionneur dessiné : un anneau par place de collision. Deux faisceaux partent de
 * l'injecteur, en haut, font le tour chacun dans un sens et se croisent dans le détecteur, en
 * bas : c'est le moment de la résonance (la zone dorée). Un clic sur l'anneau, ou sur
 * « Résonance ! », tente la résonance ; réussie, l'anneau s'illumine et des quarks jaillissent
 * du détecteur. Un arc fin, autour, montre l'avancée de la collision.
 */
final class ColliderView extends Canvas {

    private static final Color BACK = Color.web("#070d17");
    private static final Color RING = Color.web("#1b3a5c");
    private static final Color RING_LIGHT = Color.web("#9ad0ff");
    private static final Color BEAM_A = Color.web("#6fe3ff");
    private static final Color BEAM_B = Color.web("#ff7ad9");
    private static final Color GOLD = Color.web("#ffd27f");
    private static final Color MISSED = Color.web("#ff6a5a");
    private static final Color TEXT = Color.web("#cfe8ff");
    private static final Color DIM = Color.web("#5a7088");
    /** Places dessinées au plus : autant que de places de collision possibles. */
    private static final int SLOTS = 5;
    private static final int TRAIL = 12;
    private static final int SPARKS = 28;
    private static final double BURST_SECONDS = 1.2;
    private static final int ARC_STEPS = 48;
    private static final double[] XS = new double[ARC_STEPS + 1];
    private static final double[] YS = new double[ARC_STEPS + 1];

    private final Game game;
    private double time;
    /** Pour chaque place : ce qu'on savait de sa résonance à l'image d'avant, pour lancer l'éclat au bon moment. */
    private final boolean[] resonated = new boolean[SLOTS];
    private final double[] burst = new double[SLOTS];
    private final boolean[] burstWon = new boolean[SLOTS];
    private final double[] burstSeed = new double[SLOTS];
    private int drawnSlots = -1;

    /** @param onRing ce que fait un clic sur un anneau : tenter sa résonance */
    ColliderView(Game game, IntConsumer onRing) {
        super(780, 210);
        this.game = game;
        setOnMouseClicked(event -> {
            int slot = slotAt(event.getX());
            if (slot >= 0) onRing.accept(slot);
        });
    }

    /** La place sous ce point, ou -1. */
    int slotAt(double x) {
        int slots = game.colliderSlots();
        double cell = cell(slots);
        double left = (getWidth() - cell * slots) / 2;
        int slot = (int) Math.floor((x - left) / cell);
        return slot >= 0 && slot < slots ? slot : -1;
    }

    private double cell(int slots) {
        return Math.min(220, getWidth() / Math.max(1, slots));
    }

    /** Un éclat est-il en cours sur cette place : pour les vérifications. */
    boolean bursting(int slot) {
        return burst[slot] > 0;
    }

    /** Avance l'animation et redessine. */
    void frame(double elapsed) {
        time += elapsed;
        List<Collision> running = game.state().collisions();
        for (int i = 0; i < SLOTS; i++) {
            boolean now = i < running.size() && running.get(i).resonated();
            if (now && !resonated[i]) {
                burst[i] = BURST_SECONDS;
                burstWon[i] = running.get(i).doubled();
                burstSeed[i] = time;
            }
            resonated[i] = now;
            if (burst[i] > 0) burst[i] = Math.max(0, burst[i] - elapsed);
        }
        draw();
    }

    void draw() {
        double w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        GraphicsContext g = getGraphicsContext2D();
        g.setGlobalAlpha(1);
        g.setFill(BACK);
        g.fillRect(0, 0, w, h);
        stars(g, w, h);
        int slots = game.colliderSlots();
        drawnSlots = slots;
        double cell = cell(slots);
        double left = (w - cell * slots) / 2;
        double radius = Math.min(cell * 0.34, h * 0.33);
        List<Collision> running = game.state().collisions();
        for (int i = 0; i < slots; i++) {
            double cx = left + cell * (i + 0.5), cy = h * 0.47;
            if (i < running.size()) ring(g, i, running.get(i), cx, cy, radius);
            else free(g, cx, cy, radius);
        }
        g.setGlobalAlpha(1);
    }

    /** Un ciel discret, toujours le même. */
    private void stars(GraphicsContext g, double w, double h) {
        g.setFill(Color.web("#9ab4d0"));
        for (int i = 0; i < 70; i++) {
            double x = (i * 137.508 % 1) * w, y = (i * 0.618034 * 97 % 1) * h;
            g.setGlobalAlpha(0.15 + 0.25 * (0.5 + 0.5 * Math.sin(time * 0.7 + i)));
            g.fillOval(x, y, 1.4, 1.4);
        }
        g.setGlobalAlpha(1);
    }

    /** Un point de l'anneau : l'angle 0 est l'injecteur, en haut, et l'angle π le détecteur, en bas. */
    private static double px(double cx, double r, double angle) {
        return cx + r * Math.sin(angle);
    }

    private static double py(double cy, double r, double angle) {
        return cy - r * Math.cos(angle);
    }

    private static void arc(GraphicsContext g, double cx, double cy, double r, double from, double to) {
        for (int k = 0; k <= ARC_STEPS; k++) {
            double a = from + (to - from) * k / ARC_STEPS;
            XS[k] = px(cx, r, a);
            YS[k] = py(cy, r, a);
        }
        g.strokePolyline(XS, YS, ARC_STEPS + 1);
    }

    private void ring(GraphicsContext g, int slot, Collision collision, double cx, double cy, double r) {
        double phase = game.resonancePhase(slot);
        boolean window = Game.inResonance(phase);
        boolean won = collision.resonated() && collision.doubled(), missed = collision.resonated() && !collision.doubled();
        // L'anneau, avec une lueur qui respire.
        g.setStroke(RING);
        g.setLineWidth(9);
        g.strokeOval(cx - r, cy - r, 2 * r, 2 * r);
        g.setStroke(won ? GOLD : missed ? MISSED : RING_LIGHT);
        g.setGlobalAlpha(won ? 0.75 : 0.25 + 0.1 * Math.sin(time * 2 + slot));
        g.setLineWidth(2);
        g.strokeOval(cx - r, cy - r, 2 * r, 2 * r);
        g.setGlobalAlpha(1);
        // La zone de résonance, autour du détecteur.
        double half = Math.PI * Game.RESONANCE_WINDOW;
        g.setStroke(GOLD);
        g.setGlobalAlpha(collision.resonated() ? 0.25 : window ? 1 : 0.45);
        g.setLineWidth(window && !collision.resonated() ? 7 : 5);
        arc(g, cx, cy, r, Math.PI - half, Math.PI + half);
        g.setGlobalAlpha(1);
        // L'avancée de la collision : un arc fin, autour, depuis l'injecteur.
        double total = collision.tier().seconds();
        double done = total <= 0 ? 1 : Math.max(0, Math.min(1, 1 - collision.left() / total));
        g.setStroke(won ? GOLD : RING_LIGHT);
        g.setGlobalAlpha(0.8);
        g.setLineWidth(2);
        arc(g, cx, cy, r + 10, 0, 2 * Math.PI * done);
        g.setGlobalAlpha(1);
        // L'injecteur en haut, le détecteur en bas.
        g.setFill(Color.web("#2c4866"));
        g.fillRoundRect(px(cx, r, 0) - 7, py(cy, r, 0) - 5, 14, 10, 3, 3);
        Color detector = won ? GOLD : window && !collision.resonated() ? GOLD : Color.web("#3b5578");
        g.setFill(detector);
        g.fillRoundRect(px(cx, r, Math.PI) - 10, py(cy, r, Math.PI) - 6, 20, 12, 4, 4);
        // Les deux faisceaux : partis ensemble de l'injecteur, ils se croisent dans le détecteur à mi-tour.
        double angle = 2 * Math.PI * phase;
        beam(g, cx, cy, r, angle, 1, BEAM_A);
        beam(g, cx, cy, r, -angle, -1, BEAM_B);
        if (window && !collision.resonated()) glow(g, px(cx, r, Math.PI), py(cy, r, Math.PI), r * 0.45, GOLD, 0.55);
        // L'éclat d'une résonance : des quarks qui jaillissent du détecteur, dorés si elle a réussi.
        if (burst[slot] > 0) sparks(g, slot, px(cx, r, Math.PI), py(cy, r, Math.PI), r);
        // Le texte : l'énergie, le temps, le résultat.
        g.setTextAlign(TextAlignment.CENTER);
        g.setFont(Font.font(12));
        g.setFill(TEXT);
        g.fillText(shortName(collision), cx, cy - 4);
        g.setFill(DIM);
        g.fillText(Format.wait(collision.left()), cx, cy + 12);
        if (collision.resonated()) {
            g.setFont(Font.font(13));
            g.setFill(won ? GOLD : MISSED);
            g.fillText(won ? "butin ×2" : "résonance manquée", cx, cy + r + 30);
        } else {
            g.setFont(Font.font(11));
            g.setFill(window ? GOLD : DIM);
            g.fillText(window ? "maintenant !" : "cliquer quand ils se croisent", cx, cy + r + 30);
        }
        g.setTextAlign(TextAlignment.LEFT);
    }

    private static String shortName(Collision collision) {
        return switch (collision.tier()) {
            case LOW -> "Basse";
            case MEDIUM -> "Moyenne";
            case HIGH -> "Haute";
            case VERY_HIGH -> "Très haute";
            case EXTREME -> "Extrême";
        };
    }

    /** Un faisceau : sa tête lumineuse, et sa traîne qui s'efface derrière elle. */
    private static void beam(GraphicsContext g, double cx, double cy, double r, double angle, int direction, Color color) {
        g.setFill(color);
        for (int k = TRAIL; k >= 1; k--) {
            double a = angle - direction * k * 0.07;
            g.setGlobalAlpha(0.5 * (1 - (double) k / (TRAIL + 1)));
            double size = 5 * (1 - 0.6 * k / TRAIL);
            g.fillOval(px(cx, r, a) - size / 2, py(cy, r, a) - size / 2, size, size);
        }
        g.setGlobalAlpha(1);
        glow(g, px(cx, r, angle), py(cy, r, angle), 10, color, 0.8);
        g.setFill(Color.WHITE);
        g.fillOval(px(cx, r, angle) - 3, py(cy, r, angle) - 3, 6, 6);
    }

    private static void glow(GraphicsContext g, double x, double y, double size, Color color, double strength) {
        g.setFill(new RadialGradient(0, 0, x, y, size, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.color(color.getRed(), color.getGreen(), color.getBlue(), strength)),
                new Stop(1, Color.color(color.getRed(), color.getGreen(), color.getBlue(), 0))));
        g.fillOval(x - size, y - size, 2 * size, 2 * size);
    }

    private void sparks(GraphicsContext g, int slot, double x, double y, double r) {
        double age = BURST_SECONDS - burst[slot];
        double fade = burst[slot] / BURST_SECONDS;
        Color color = burstWon[slot] ? GOLD : MISSED;
        int count = burstWon[slot] ? SPARKS : SPARKS / 3;
        glow(g, x, y, r * (0.4 + age), color, 0.6 * fade);
        Color[] palette = {GOLD, BEAM_A, BEAM_B, Color.WHITE};
        for (int k = 0; k < count; k++) {
            double seed = Math.sin((k + 1) * 12.9898 + burstSeed[slot] * 78.233) * 43758.5453;
            double unit = seed - Math.floor(seed);
            double a = 2 * Math.PI * k / count + unit;
            double speed = r * (0.8 + unit * 1.4);
            double sx = x + Math.cos(a) * speed * age, sy = y + Math.sin(a) * speed * age + 30 * age * age;
            g.setGlobalAlpha(fade);
            g.setFill(burstWon[slot] ? palette[k % palette.length] : MISSED);
            g.fillOval(sx - 2, sy - 2, 4, 4);
        }
        g.setGlobalAlpha(1);
    }

    /** Une place libre : un anneau en pointillé, qui attend une collision. */
    private void free(GraphicsContext g, double cx, double cy, double r) {
        g.setStroke(Color.web("#22374f"));
        g.setLineWidth(3);
        for (int k = 0; k < 24; k += 2) arc(g, cx, cy, r, 2 * Math.PI * k / 24 + time * 0.1, 2 * Math.PI * (k + 1) / 24 + time * 0.1);
        g.setTextAlign(TextAlignment.CENTER);
        g.setFont(Font.font(12));
        g.setFill(DIM);
        g.fillText("Place libre", cx, cy + 4);
        g.setTextAlign(TextAlignment.LEFT);
    }

    /** Nombre de places dessinées à la dernière image : pour les vérifications. */
    int drawnSlots() {
        return drawnSlots;
    }
}
