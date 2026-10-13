package idle.ui.collider;

import idle.core.Game;
import idle.core.math.BigNum;
import idle.ui.common.Format;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleConsumer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

/**
 * L'anneau d'énergie du collisionneur (version 0.57) : un grand anneau ovale où deux faisceaux
 * tournent en sens contraires. Chaque fois qu'ils se croisent dans le détecteur, des étincelles
 * jaillissent : c'est l'énergie que donnent les faisceaux croisés. Un clic au moment du
 * croisement capte {@link Game#CROSSING_SECONDS} secondes de production d'un coup.
 */
public final class EnergyView extends Canvas {

    private static final Color BACK = Color.web("#060b14");
    private static final Color RING = Color.web("#173250");
    private static final Color LIGHT = Color.web("#9ad0ff");
    private static final Color BEAM_A = Color.web("#6fe3ff");
    private static final Color BEAM_B = Color.web("#ff7ad9");
    private static final Color GOLD = Color.web("#ffd27f");
    private static final int STEPS = 72;
    private static final double[] XS = new double[STEPS + 1];
    private static final double[] YS = new double[STEPS + 1];

    /** Une étincelle, ou un « +12 énergie » qui monte et s'efface. */
    private static final class Spark {
        double x, y, vx, vy, life, age;
        Color color;
        String text;
    }

    private final Game game;
    private final List<Spark> sparks = new ArrayList<>();
    private double time;
    private boolean wasCrossing;
    private double lastGain;

    /** @param onCatch ce qui suit un clic capté : la page se recopie (l'énergie gagnée en argument) */
    EnergyView(Game game, DoubleConsumer onCatch) {
        super(780, 170);
        this.game = game;
        setOnMouseClicked(event -> {
            double gain = game.catchCrossing();
            if (gain > 0) {
                lastGain = gain;
                burst(true, gain);
            }
            onCatch.accept(gain);
        });
    }

    /** L'énergie du dernier croisement capté : pour les vérifications. */
    public double lastGain() {
        return lastGain;
    }

    public int sparks() {
        return sparks.size();
    }

    private double cx() {
        return getWidth() / 2;
    }

    private double cy() {
        return getHeight() / 2 - 6;
    }

    private double rx() {
        return Math.min(getWidth() * 0.36, 300);
    }

    private double ry() {
        return getHeight() * 0.32;
    }

    /** Un point de l'anneau : l'angle 0 en haut (l'injecteur), π en bas (le détecteur). */
    private double px(double angle) {
        return cx() + rx() * Math.sin(angle);
    }

    private double py(double angle) {
        return cy() - ry() * Math.cos(angle);
    }

    public void frame(double elapsed) {
        time += elapsed;
        boolean crossing = Game.inResonance(game.colliderBeamPhase());
        // Chaque croisement fait jaillir des étincelles, même sans clic : les faisceaux produisent toujours.
        if (crossing && !wasCrossing) burst(false, 0);
        wasCrossing = crossing;
        for (Spark spark : sparks) {
            spark.age += elapsed;
            spark.x += spark.vx * elapsed;
            spark.y += spark.vy * elapsed;
            spark.vy += (spark.text == null ? 60 : -10) * elapsed;
        }
        sparks.removeIf(spark -> spark.age >= spark.life);
        draw();
    }

    /** Des étincelles au détecteur ; dorées et plus nombreuses quand le croisement est capté, avec ce qu'il rapporte. */
    private void burst(boolean caught, double gain) {
        double x = px(Math.PI), y = py(Math.PI);
        int count = caught ? 34 : 10;
        Color[] palette = caught ? new Color[] {GOLD, Color.WHITE, BEAM_A, BEAM_B} : new Color[] {BEAM_A, BEAM_B};
        for (int k = 0; k < count; k++) {
            Spark spark = new Spark();
            double seed = Math.sin((k + 1) * 12.9898 + time * 78.233) * 43758.5453;
            double unit = seed - Math.floor(seed);
            double angle = -Math.PI * unit;
            double speed = (caught ? 90 : 40) * (0.5 + unit);
            spark.x = x;
            spark.y = y;
            spark.vx = Math.cos(angle) * speed;
            spark.vy = Math.sin(angle) * speed;
            spark.life = caught ? 1.1 : 0.6;
            spark.color = palette[k % palette.length];
            sparks.add(spark);
        }
        if (caught) {
            Spark text = new Spark();
            text.x = x;
            text.y = y - 20;
            text.vy = -30;
            text.life = 1.4;
            text.color = GOLD;
            text.text = "+" + Format.count(BigNum.of(gain)) + " énergie";
            sparks.add(text);
        }
    }

    void draw() {
        double w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        GraphicsContext g = getGraphicsContext2D();
        g.setGlobalAlpha(1);
        g.setFill(BACK);
        g.fillRect(0, 0, w, h);
        g.setFill(Color.web("#9ab4d0"));
        for (int i = 0; i < 80; i++) {
            g.setGlobalAlpha(0.12 + 0.25 * (0.5 + 0.5 * Math.sin(time * 0.6 + i)));
            g.fillOval((i * 0.754877666 % 1) * w, (i * 0.569840291 % 1) * h, 1.4, 1.4);
        }
        g.setGlobalAlpha(1);
        double phase = game.colliderBeamPhase();
        boolean crossing = Game.inResonance(phase), catchable = game.canCatchCrossing();
        // L'anneau.
        ellipse(g, 0, 2 * Math.PI, RING, 10, 1);
        ellipse(g, 0, 2 * Math.PI, LIGHT, 2, 0.25 + 0.1 * Math.sin(time * 2));
        // La zone du croisement, autour du détecteur.
        double half = Math.PI * Game.RESONANCE_WINDOW;
        ellipse(g, Math.PI - half, Math.PI + half, GOLD, catchable ? 8 : 5, catchable ? 1 : 0.4);
        // L'injecteur et le détecteur.
        g.setFill(Color.web("#2c4866"));
        g.fillRoundRect(px(0) - 9, py(0) - 6, 18, 12, 3, 3);
        g.setFill(catchable ? GOLD : Color.web("#3b5578"));
        g.fillRoundRect(px(Math.PI) - 13, py(Math.PI) - 7, 26, 14, 4, 4);
        // Les deux faisceaux : partis ensemble de l'injecteur, ils se croisent dans le détecteur.
        double angle = 2 * Math.PI * phase;
        beam(g, angle, 1, BEAM_A);
        beam(g, -angle, -1, BEAM_B);
        if (catchable) glow(g, px(Math.PI), py(Math.PI), 46, GOLD, 0.5);
        // Les étincelles, et les gains qui montent.
        for (Spark spark : sparks) {
            double fade = 1 - spark.age / spark.life;
            g.setGlobalAlpha(Math.max(0, fade));
            if (spark.text != null) {
                g.setFont(Font.font(14));
                g.setTextAlign(TextAlignment.CENTER);
                g.setFill(spark.color);
                g.fillText(spark.text, spark.x, spark.y);
                g.setTextAlign(TextAlignment.LEFT);
            } else {
                g.setFill(spark.color);
                g.fillOval(spark.x - 2, spark.y - 2, 4, 4);
            }
        }
        g.setGlobalAlpha(1);
        g.setTextAlign(TextAlignment.CENTER);
        g.setFont(Font.font(13));
        g.setFill(LIGHT);
        g.fillText("+" + Format.count(BigNum.of(game.colliderEnergyPerSecond())) + " énergie par seconde", cx(), cy() + 5);
        g.setFont(Font.font(11));
        g.setFill(catchable ? GOLD : Color.web("#5a7088"));
        g.fillText(catchable ? "ils se croisent : cliquer !" : crossing ? "croisement capté" : "cliquer quand les faisceaux se croisent en bas",
                cx(), h - 8);
        g.setTextAlign(TextAlignment.LEFT);
    }

    private void ellipse(GraphicsContext g, double from, double to, Color color, double width, double alpha) {
        for (int k = 0; k <= STEPS; k++) {
            double a = from + (to - from) * k / STEPS;
            XS[k] = px(a);
            YS[k] = py(a);
        }
        g.setStroke(color);
        g.setLineWidth(width);
        g.setGlobalAlpha(alpha);
        g.strokePolyline(XS, YS, STEPS + 1);
        g.setGlobalAlpha(1);
    }

    private void beam(GraphicsContext g, double angle, int direction, Color color) {
        g.setFill(color);
        for (int k = 14; k >= 1; k--) {
            double a = angle - direction * k * 0.05;
            g.setGlobalAlpha(0.5 * (1 - k / 15.0));
            double size = 6 * (1 - 0.6 * k / 14);
            g.fillOval(px(a) - size / 2, py(a) - size / 2, size, size);
        }
        g.setGlobalAlpha(1);
        glow(g, px(angle), py(angle), 12, color, 0.8);
        g.setFill(Color.WHITE);
        g.fillOval(px(angle) - 3.5, py(angle) - 3.5, 7, 7);
    }

    private static void glow(GraphicsContext g, double x, double y, double size, Color color, double strength) {
        g.setFill(new RadialGradient(0, 0, x, y, size, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.color(color.getRed(), color.getGreen(), color.getBlue(), strength)),
                new Stop(1, Color.color(color.getRed(), color.getGreen(), color.getBlue(), 0))));
        g.fillOval(x - size, y - size, 2 * size, 2 * size);
    }
}
