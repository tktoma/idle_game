package idle.ui.probes;

import idle.core.Game;
import idle.core.probes.Probe;
import idle.core.probes.ProbeDestination;
import java.util.List;
import java.util.function.LongConsumer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

/**
 * Les sondes dessinées : le hangar à gauche, une trajectoire par sonde vers sa destination à
 * droite, et la sonde qui avance au fil de son voyage. Arrivée, elle brille au-dessus de sa
 * destination : un clic dessus ouvre son butin.
 */
public final class ProbesView extends Canvas {

    private static final Color BACK = Color.web("#0a0806");
    private static final Color PATH = Color.web("#6b5a3e");
    private static final Color LIT = Color.web("#ffcf8a");
    private static final Color TEXT = Color.web("#f3e2c6");
    private static final Color DIM = Color.web("#8a7a62");
    /** Voies dessinées au plus : le hangar et le palier du septième Big Bang en donnent quatre. */
    static final int LANES = 4;
    private static final int STEPS = 40;
    private static final double[] XS = new double[STEPS + 1];
    private static final double[] YS = new double[STEPS + 1];

    private final Game game;
    private double time;

    /** @param onLanded ce que fait un clic sur une sonde arrivée : ouvrir son butin, par son numéro */
    ProbesView(Game game, LongConsumer onLanded) {
        super(780, 240);
        this.game = game;
        setOnMouseClicked(event -> {
            Probe probe = probeAt(event.getX(), event.getY());
            if (probe != null && probe.landed()) onLanded.accept(probe.id());
        });
    }

    /** La hauteur qu'il faut pour ce nombre de voies. */
    public static double heightFor(int lanes) {
        return 70 + 52 * Math.max(1, lanes);
    }

    private double laneY(int lane, int lanes) {
        double h = getHeight();
        double span = h - 40;
        return 20 + span * (lane + 0.5) / Math.max(1, lanes);
    }

    private double baseX() {
        return 62;
    }

    private double targetX() {
        return getWidth() - 110;
    }

    /** La sonde la plus proche de ce point, sur sa voie : pour le clic. */
    Probe probeAt(double x, double y) {
        List<Probe> probes = game.state().probes();
        int lanes = Math.min(LANES, Math.max(1, game.probeSlots()));
        Probe best = null;
        double nearest = 40;
        for (int i = 0; i < Math.min(probes.size(), LANES); i++) {
            double[] at = point(probes.get(i).progress(), laneY(i, lanes));
            double d = Math.hypot(at[0] - x, at[1] - y);
            if (d < nearest) {
                nearest = d;
                best = probes.get(i);
            }
            // Arrivée, toute sa destination se clique.
            if (probes.get(i).landed() && Math.abs(y - laneY(i, lanes)) < 24 && x > targetX() - 40) best = probes.get(i);
        }
        return best;
    }

    /** Un point de la trajectoire, de 0 au hangar à 1 à la destination : une courbe qui monte puis redescend. */
    private double[] point(double t, double laneY) {
        double h = getHeight();
        double x0 = baseX(), y0 = h / 2, x2 = targetX(), y2 = laneY;
        double x1 = (x0 + x2) / 2, y1 = Math.min(y0, y2) - 46;
        double u = 1 - t;
        return new double[] {u * u * x0 + 2 * u * t * x1 + t * t * x2, u * u * y0 + 2 * u * t * y1 + t * t * y2};
    }

    void frame(double elapsed) {
        time += elapsed;
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
        int lanes = Math.min(LANES, Math.max(1, game.probeSlots()));
        List<Probe> probes = game.state().probes();
        hangar(g, baseX(), h / 2);
        for (int i = 0; i < lanes; i++) {
            double y = laneY(i, lanes);
            if (i < probes.size()) lane(g, probes.get(i), y);
            else {
                g.setFill(DIM);
                g.setFont(Font.font(11));
                g.setTextAlign(TextAlignment.RIGHT);
                g.fillText("Place libre", targetX() + 60, y + 4);
                g.setTextAlign(TextAlignment.LEFT);
            }
        }
    }

    private void stars(GraphicsContext g, double w, double h) {
        g.setFill(Color.web("#e8d8c0"));
        for (int i = 0; i < 90; i++) {
            double x = (i * 0.754877666 % 1) * w, y = (i * 0.569840291 % 1) * h;
            g.setGlobalAlpha(0.12 + 0.3 * (0.5 + 0.5 * Math.sin(time * 0.5 + i * 1.7)));
            double size = i % 7 == 0 ? 2 : 1.2;
            g.fillOval(x, y, size, size);
        }
        g.setGlobalAlpha(1);
    }

    /** Le hangar : une petite planète-base, avec son anneau de quai. */
    private void hangar(GraphicsContext g, double x, double y) {
        glow(g, x, y, 34, LIT, 0.18);
        g.setFill(new RadialGradient(0, 0, x - 6, y - 6, 22, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#d9c2a0")), new Stop(1, Color.web("#5a4630"))));
        g.fillOval(x - 18, y - 18, 36, 36);
        g.setStroke(LIT);
        g.setGlobalAlpha(0.6);
        g.setLineWidth(1.5);
        g.strokeOval(x - 30, y - 8, 60, 16);
        g.setGlobalAlpha(1);
        g.setFill(TEXT);
        g.setFont(Font.font(11));
        g.setTextAlign(TextAlignment.CENTER);
        g.fillText("Hangar", x, y + 34);
        g.setTextAlign(TextAlignment.LEFT);
    }

    private void lane(GraphicsContext g, Probe probe, double y) {
        double t = probe.progress();
        // La trajectoire : en pointillé devant la sonde, pleine derrière elle.
        for (int k = 0; k <= STEPS; k++) {
            double[] at = point((double) k / STEPS, y);
            XS[k] = at[0];
            YS[k] = at[1];
        }
        g.setStroke(PATH);
        g.setLineWidth(1.5);
        for (int k = 0; k < STEPS; k += 2) g.strokeLine(XS[k], YS[k], XS[k + 1], YS[k + 1]);
        int done = (int) Math.floor(t * STEPS);
        if (done > 0) {
            g.setStroke(LIT);
            g.setGlobalAlpha(0.7);
            g.setLineWidth(2);
            g.strokePolyline(XS, YS, done + 1);
            g.setGlobalAlpha(1);
        }
        destination(g, probe.destination(), targetX() + 22, y, probe.landed());
        double[] at = point(t, y), ahead = point(Math.min(1, t + 0.01), y);
        double angle = Math.atan2(ahead[1] - at[1], ahead[0] - at[0]);
        if (probe.landed()) {
            // Arrivée : elle tourne au-dessus de sa destination, et appelle le clic.
            double pulse = 0.5 + 0.5 * Math.sin(time * 4);
            glow(g, targetX() + 22, y, 30 + 6 * pulse, LIT, 0.35 + 0.3 * pulse);
            double orbit = time * 1.6;
            craft(g, targetX() + 22 + 20 * Math.cos(orbit), y + 9 * Math.sin(orbit), orbit + Math.PI / 2);
            g.setFill(LIT);
            g.setFont(Font.font(12));
            g.setTextAlign(TextAlignment.RIGHT);
            g.fillText("Butin prêt · cliquer", targetX() - 16, y + 4);
            g.setTextAlign(TextAlignment.LEFT);
        } else {
            // En vol : une traîne, puis la sonde, pointée le long de sa route.
            for (int k = 1; k <= 8; k++) {
                double[] back = point(Math.max(0, t - k * 0.008), y);
                g.setGlobalAlpha(0.35 * (1 - k / 9.0));
                g.setFill(LIT);
                g.fillOval(back[0] - 2, back[1] - 2, 4, 4);
            }
            g.setGlobalAlpha(1);
            craft(g, at[0], at[1], angle);
            g.setFill(DIM);
            g.setFont(Font.font(11));
            g.setTextAlign(TextAlignment.RIGHT);
            g.fillText("arrivée dans " + ProbesPane.minutes(probe.left()), targetX() - 16, y + 4);
            g.setTextAlign(TextAlignment.LEFT);
        }
    }

    /** La sonde : un petit corps lumineux, ses deux panneaux, pointée selon {@code angle}. */
    private static void craft(GraphicsContext g, double x, double y, double angle) {
        glow(g, x, y, 12, LIT, 0.55);
        double c = Math.cos(angle), s = Math.sin(angle);
        double[] px = {x + 7 * c, x - 5 * c - 4 * s, x - 3 * c, x - 5 * c + 4 * s};
        double[] py = {y + 7 * s, y - 5 * s + 4 * c, y - 3 * s, y - 5 * s - 4 * c};
        g.setFill(Color.WHITE);
        g.fillPolygon(px, py, 4);
        g.setStroke(Color.web("#8ab8ff"));
        g.setLineWidth(2);
        g.strokeLine(x - 2 * c - 7 * s, y - 2 * s + 7 * c, x - 2 * c + 7 * s, y - 2 * s - 7 * c);
    }

    /** Une destination : un astre ou un nuage à sa couleur, et son nom dessous. */
    private void destination(GraphicsContext g, ProbeDestination where, double x, double y, boolean reached) {
        Color[] colors = colors(where);
        switch (where) {
            case VOID -> {
                // Le vide : quelques étoiles lointaines qui tournent lentement.
                for (int k = 0; k < 7; k++) {
                    double a = time * 0.3 + k * 0.9;
                    g.setFill(colors[k % 2]);
                    g.setGlobalAlpha(0.8);
                    g.fillOval(x + 14 * Math.cos(a) * (0.4 + k * 0.1) - 1.5, y + 10 * Math.sin(a) * (0.4 + k * 0.1) - 1.5, 3, 3);
                }
                g.setGlobalAlpha(1);
            }
            case GAS_CLOUD, NEBULA, SUPERNOVA -> {
                for (int k = 0; k < 5; k++) {
                    double a = k * 1.3 + time * 0.15;
                    glow(g, x + 8 * Math.cos(a), y + 5 * Math.sin(a), 16, colors[k % 2], 0.45);
                }
            }
            case COMET -> {
                g.setStroke(colors[1]);
                g.setGlobalAlpha(0.5);
                g.setLineWidth(5);
                g.strokeLine(x, y, x + 26, y - 10);
                g.setGlobalAlpha(1);
                glow(g, x, y, 10, colors[0], 0.8);
                g.setFill(Color.WHITE);
                g.fillOval(x - 3, y - 3, 6, 6);
            }
            case ASTEROIDS -> {
                g.setFill(colors[0]);
                for (int k = 0; k < 6; k++) {
                    double a = k * 1.05 + time * 0.2;
                    double size = 4 + k % 3 * 2;
                    g.fillOval(x + 16 * Math.cos(a) - size / 2, y + 6 * Math.sin(a) - size / 2, size, size);
                }
            }
            default -> {
                glow(g, x, y, 18, colors[1], 0.35);
                g.setFill(new RadialGradient(0, 0, x - 4, y - 4, 14, false, CycleMethod.NO_CYCLE,
                        new Stop(0, colors[1]), new Stop(1, colors[0])));
                g.fillOval(x - 11, y - 11, 22, 22);
            }
        }
        g.setFill(reached ? LIT : TEXT);
        g.setFont(Font.font(10));
        g.setTextAlign(TextAlignment.CENTER);
        g.fillText(where.label(), x, y + 26);
        g.setTextAlign(TextAlignment.LEFT);
    }

    /** Deux couleurs par destination : la teinte, et la lumière. */
    private static Color[] colors(ProbeDestination where) {
        return switch (where) {
            case VOID -> new Color[] {Color.web("#cfd8ff"), Color.web("#ffe9c2")};
            case GAS_CLOUD -> new Color[] {Color.web("#8ac8ff"), Color.web("#d0ecff")};
            case COMET -> new Color[] {Color.web("#bfe8ff"), Color.web("#8ad0ff")};
            case SALT_MOON -> new Color[] {Color.web("#7a7a8a"), Color.web("#f0f0ff")};
            case ASTEROIDS -> new Color[] {Color.web("#9a8a7a"), Color.web("#d0c0a0")};
            case DEAD_STAR -> new Color[] {Color.web("#5a5a7a"), Color.web("#e6f0ff")};
            case NEBULA -> new Color[] {Color.web("#e08ad0"), Color.web("#7ad8c8")};
            case OCEAN_WORLD -> new Color[] {Color.web("#1a4a9a"), Color.web("#7ac8ff")};
            case SUPERNOVA -> new Color[] {Color.web("#ff7a5a"), Color.web("#ffd27f")};
        };
    }

    private static void glow(GraphicsContext g, double x, double y, double size, Color color, double strength) {
        g.setFill(new RadialGradient(0, 0, x, y, size, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.color(color.getRed(), color.getGreen(), color.getBlue(), strength)),
                new Stop(1, Color.color(color.getRed(), color.getGreen(), color.getBlue(), 0))));
        g.fillOval(x - size, y - size, 2 * size, 2 * size);
    }
}
