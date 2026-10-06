package idle.ui;

import idle.core.Body;
import idle.core.Game;
import java.util.List;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

/**
 * La galaxie de la sous-page « Galaxie » : une image de ce que devient tout ce que le joueur a
 * créé une fois rassemblé autour du trou noir supermassif ({@link Game#formGalaxy()}).
 *
 * <p>C'est une galaxie spirale vue de face ({@link Spiral}), faite de points qui prennent la
 * couleur des molécules du joueur, prises à intervalles réguliers dans l'ordre où il les a créées.
 * Tant que la galaxie n'est pas formée, elle est pâle : un aperçu. Formée, elle brille, et le trou
 * noir supermassif est en son centre. La vraie, avec chaque molécule et chaque astre, est dans
 * l'expansion de la matière.
 */
final class GalaxyView extends Canvas {

    private static final int DOTS = 1600;
    private static final int STARS = 60;
    private static final double GOLDEN_ANGLE = Math.PI * (3 - Math.sqrt(5));
    /** Vitesse de rotation, en radians par seconde : un tour en deux minutes environ. */
    private static final double SPIN = 0.05;
    private static final Color VOID = Color.web("#05060a");
    private static final Color STAR = Color.web("#c9d6ea");
    private static final Color TEXT = Color.web("#8fa3b8");
    /** Les couleurs des points tant que le joueur n'a créé aucune molécule : celles des états de la matière. */
    private static final Color[] PALE = {Color.web("#bfe6f2"), Color.web("#4d8fe0"), Color.web("#d9b36c"),
            Color.web("#b48cf2"), Color.web("#aeb6c4")};
    private static final Stop[] GLOW = {new Stop(0, Color.web("#fff4d8", 0.9)), new Stop(0.08, Color.web("#ffe2a8", 0.6)),
            new Stop(0.25, Color.web("#ffcf90", 0.25)), new Stop(0.6, Color.web("#8fa8ff", 0.1)), new Stop(1, Color.web("#8fa8ff", 0))};
    /** Où va chaque point : sa distance au centre, en parts du rayon, et le cosinus et le sinus de son angle dans les bras. */
    private static final double[] AWAY = new double[DOTS];
    private static final double[] COS = new double[DOTS];
    private static final double[] SIN = new double[DOTS];

    static {
        for (int dot = 0; dot < DOTS; dot++) {
            AWAY[dot] = Math.sqrt((dot + 0.5) / DOTS);
            double angle = Spiral.bend(AWAY[dot], dot * GOLDEN_ANGLE);
            COS[dot] = Math.cos(angle);
            SIN[dot] = Math.sin(angle);
        }
    }

    private final Game game;
    private final Color[] colors = new Color[DOTS];
    private int colored = -1;
    private double time = 0;
    /** Comme les autres vues qui bougent lentement : une image sur deux tant que rien ne change. */
    private int beat = 0;
    private double drawnWidth = -1;
    private double drawnHeight = -1;
    private boolean drawnFormed = false;

    GalaxyView(Game game) {
        super(600, 230);
        this.game = game;
    }

    /** Fait avancer le temps de la vue et la redessine. */
    void frame(double elapsed) {
        time += elapsed;
        beat++;
        boolean changed = getWidth() != drawnWidth || getHeight() != drawnHeight || game.hasGalaxy() != drawnFormed
                || colored != game.state().moleculesVersion();
        if (!changed && beat % 2 != 0) return;
        redraw();
    }

    /** Dessine la galaxie telle qu'elle est à cet instant. */
    private void redraw() {
        double w = getWidth();
        double h = getHeight();
        if (w <= 0 || h <= 0) return;
        drawnWidth = w;
        drawnHeight = h;
        boolean formed = game.hasGalaxy();
        drawnFormed = formed;
        color();
        GraphicsContext g = getGraphicsContext2D();
        g.clearRect(0, 0, w, h);
        g.setGlobalAlpha(1);
        g.setFill(VOID);
        g.fillRect(0, 0, w, h);
        g.setFill(STAR);
        for (int star = 0; star < STARS; star++) {
            g.setGlobalAlpha(0.2 + 0.2 * fraction(star * 5.1));
            g.fillOval(fraction(star * 12.9898) * w, fraction(star * 78.233) * h, 1, 1);
        }
        double cx = w / 2;
        double cy = h / 2;
        double radius = 0.46 * Math.min(w, h);
        // Pâle tant qu'elle n'est qu'un aperçu.
        double strength = formed ? 1 : 0.4;
        g.setGlobalAlpha(strength);
        g.setFill(new RadialGradient(0, 0, cx, cy, radius, false, CycleMethod.NO_CYCLE, GLOW));
        g.fillOval(cx - radius, cy - radius, 2 * radius, 2 * radius);
        // Les bras traînent derrière la rotation.
        double cos = Math.cos(-time * SPIN);
        double sin = Math.sin(-time * SPIN);
        g.setGlobalAlpha(0.9 * strength);
        for (int dot = 0; dot < DOTS; dot++) {
            double out = AWAY[dot] * radius;
            double x = cx + out * (COS[dot] * cos - SIN[dot] * sin);
            double y = cy + out * (SIN[dot] * cos + COS[dot] * sin);
            double size = dot % 11 == 0 ? 1.5 : 0.9;
            g.setFill(colors[dot]);
            g.fillOval(x - size, y - size, 2 * size, 2 * size);
        }
        g.setGlobalAlpha(1);
        // Le trou noir supermassif, dès qu'il est formé.
        for (Body body : game.bodies()) {
            if (body.tier() == Body.Tier.CORE && game.hasBody(body.id())) BodyArt.draw(g, body, cx, cy, 0.035 * radius + 2);
        }
        if (!formed) {
            g.setFill(TEXT);
            g.setFont(Font.font(11));
            g.setTextAlign(TextAlignment.CENTER);
            g.fillText("Un aperçu : la galaxie que formera tout ce que vous avez créé", cx, h - 8);
            g.setTextAlign(TextAlignment.LEFT);
        }
    }

    /** Donne à chaque point la couleur d'une molécule du joueur, quand leur liste a changé. */
    private void color() {
        if (colored == game.state().moleculesVersion()) return;
        colored = game.state().moleculesVersion();
        List<String> log = game.state().moleculeLog();
        for (int dot = 0; dot < DOTS; dot++) {
            colors[dot] = log.isEmpty() ? PALE[dot % PALE.length]
                    : MoleculeArt.of(game.molecule(log.get((int) ((long) dot * log.size() / DOTS)))).dominant();
        }
    }

    private static double fraction(double seed) {
        double value = Math.sin(seed) * 43758.5453;
        return value - Math.floor(value);
    }
}
