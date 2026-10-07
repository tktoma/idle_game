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
 * L'image de la sous-page « Univers » : ce que devient tout ce que le joueur a créé, à la plus
 * grande échelle qu'il a formée ({@link Game#cosmosFormed()}). D'abord sa galaxie ; puis l'amas de
 * galaxies, la sienne au milieu des autres ; enfin l'univers, une toile de filaments où son amas
 * n'est plus qu'un nœud.
 *
 * <p>C'est une galaxie spirale vue de face ({@link Spiral}), faite de points qui prennent la
 * couleur des molécules du joueur, prises à intervalles réguliers dans l'ordre où il les a créées.
 * Tant que la galaxie n'est pas formée, elle est pâle : un aperçu. Formée, elle brille, et le trou
 * noir supermassif est en son centre. La vraie, avec chaque molécule et chaque astre, est dans
 * l'expansion de la matière.
 */
final class GalaxyView extends Canvas {

    /** Nombre de points d'une galaxie : autant de couleurs à fournir à {@link #cluster}. */
    static final int DOTS = 1600;
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
    private int drawnLevel = -1;

    /** Les autres galaxies de l'amas : huit, plus petites, chacune avec sa place, sa taille et son inclinaison. */
    private static final int NEIGHBOURS = 8;
    /** La toile de l'univers : des nœuds sur une grille bousculée, reliés à leurs voisins par des filaments de points. */
    private static final int WEB_COLUMNS = 7, WEB_ROWS = 3, WEB_PER_THREAD = 120, WEB_PER_NODE = 44;
    private static final double[] WEB_X, WEB_Y, WEB_SIZE;
    private static final int[] WEB_TINT;
    private static final Color[] WEB_COLORS = {Color.web("#c9d6ea"), Color.web("#8fa8ff"), Color.web("#b48cf2"), Color.web("#ffe2a8")};
    /** Le nœud où se trouve l'amas du joueur : au milieu de la toile. */
    private static final int HOME = WEB_COLUMNS * (WEB_ROWS / 2) + WEB_COLUMNS / 2;

    static {
        java.util.List<double[]> points = new java.util.ArrayList<>();
        double[] nodeX = new double[WEB_COLUMNS * WEB_ROWS], nodeY = new double[WEB_COLUMNS * WEB_ROWS];
        for (int node = 0; node < nodeX.length; node++) {
            int column = node % WEB_COLUMNS, row = node / WEB_COLUMNS;
            nodeX[node] = (column + 0.1 + 0.8 * fraction(node * 12.9898 + 3)) / WEB_COLUMNS;
            nodeY[node] = (row + 0.1 + 0.8 * fraction(node * 78.233 + 7)) / WEB_ROWS;
        }
        int seed = 0;
        for (int node = 0; node < nodeX.length; node++) {
            int column = node % WEB_COLUMNS, row = node / WEB_COLUMNS;
            // Un amas dense à chaque nœud.
            for (int k = 0; k < WEB_PER_NODE; k++, seed++) {
                double away = 0.035 * Math.sqrt(fraction(seed * 3.31 + 1)), angle = 2 * Math.PI * fraction(seed * 7.77 + 2);
                points.add(new double[] {nodeX[node] + away * Math.cos(angle) * 0.45, nodeY[node] + away * Math.sin(angle), 1.8, 3});
            }
            // Des filaments vers le nœud de droite, celui du dessous, et parfois en diagonale.
            int[] towards = {column + 1 < WEB_COLUMNS ? node + 1 : -1, row + 1 < WEB_ROWS ? node + WEB_COLUMNS : -1,
                    column + 1 < WEB_COLUMNS && row + 1 < WEB_ROWS && node % 2 == 0 ? node + WEB_COLUMNS + 1 : -1};
            for (int other : towards) {
                if (other < 0) continue;
                for (int k = 0; k < WEB_PER_THREAD; k++, seed++) {
                    double along = fraction(seed * 5.13 + 4), aside = (fraction(seed * 9.71 + 5) - 0.5) * 0.05 * Math.sin(Math.PI * along) + (fraction(seed * 2.19 + 6) - 0.5) * 0.012;
                    double dx = nodeX[other] - nodeX[node], dy = nodeY[other] - nodeY[node], length = Math.hypot(dx, dy);
                    points.add(new double[] {nodeX[node] + along * dx - aside * dy / length * 0.45, nodeY[node] + along * dy + aside * dx / length,
                            1.1 + 0.8 * fraction(seed * 1.37), seed % 3});
                }
            }
        }
        WEB_X = new double[points.size()];
        WEB_Y = new double[points.size()];
        WEB_SIZE = new double[points.size()];
        WEB_TINT = new int[points.size()];
        for (int i = 0; i < WEB_X.length; i++) {
            WEB_X[i] = points.get(i)[0];
            WEB_Y[i] = points.get(i)[1];
            WEB_SIZE[i] = points.get(i)[2];
            WEB_TINT[i] = (int) points.get(i)[3];
        }
        HOME_X = nodeX[HOME];
        HOME_Y = nodeY[HOME];
    }

    private static final double HOME_X, HOME_Y;

    GalaxyView(Game game) {
        super(600, 230);
        this.game = game;
    }

    /** Fait avancer le temps de la vue et la redessine. */
    void frame(double elapsed) {
        time += elapsed;
        beat++;
        boolean changed = getWidth() != drawnWidth || getHeight() != drawnHeight || game.cosmosFormed() != drawnLevel
                || colored != game.state().moleculesVersion();
        // La toile de l'univers ne bouge pas : inutile de la redessiner tant que rien ne change.
        if (!changed && (beat % 2 != 0 || drawnLevel >= 3)) return;
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
        drawnLevel = game.cosmosFormed();
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
        if (drawnLevel >= 3) {
            universe(g, w, h);
            return;
        }
        if (drawnLevel == 2) {
            cluster(g, w, h, time, colors, game);
            return;
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

    /**
     * L'amas de galaxies : la galaxie du joueur au milieu, plus petite, et huit autres autour d'elle,
     * chacune vue sous son angle. Elles tournent toutes, lentement. La vue de l'expansion de la
     * matière s'en sert aussi ({@link SpaceView}).
     *
     * @param time   secondes écoulées : ce qui fait tourner les galaxies
     * @param colors la couleur de chacun des {@link #DOTS} points d'une galaxie ({@link #tint(Game, Color[])})
     */
    static void cluster(GraphicsContext g, double w, double h, double time, Color[] colors, Game game) {
        double cx = w / 2, cy = h / 2, unit = Math.min(w, h);
        // Le halo de l'amas : le gaz chaud qui baigne ses galaxies.
        g.setGlobalAlpha(0.35);
        g.setFill(new RadialGradient(0, 0, cx, cy, 0.5 * w, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#8fa8ff", 0.35)), new Stop(0.5, Color.web("#b48cf2", 0.12)), new Stop(1, Color.web("#8fa8ff", 0))));
        g.fillRect(0, 0, w, h);
        for (int other = 0; other < NEIGHBOURS; other++) {
            double side = other % 2 == 0 ? -1 : 1;
            // Les voisines se rangent des deux côtés de la galaxie du joueur, sans la recouvrir.
            double radius = (0.09 + 0.09 * fraction(other * 5.3 + 3)) * unit;
            // Au plus près, elle frôle la galaxie du joueur ; au plus loin, le bord de l'image.
            double nearest = 0.32 * unit + radius, farthest = Math.max(nearest, 0.5 * w - radius - 6);
            double x = cx + side * (nearest + fraction(other * 3.7 + 1) * (farthest - nearest));
            double y = cy + (fraction(other * 9.1 + 2) - 0.5) * 0.72 * h;
            double turn = (other % 3 == 0 ? 1 : -1) * time * SPIN * (0.6 + 0.8 * fraction(other * 2.9));
            spiral(g, colors, x, y, radius, turn + other, 0.3 + 0.7 * fraction(other * 6.1 + 4), Math.PI * fraction(other * 4.3 + 5), 7, 0.75);
        }
        spiral(g, colors, cx, cy, 0.3 * unit, -time * SPIN, 1, 0, 2, 1);
        g.setGlobalAlpha(1);
        for (Body body : game.bodies()) {
            if (body.tier() == Body.Tier.CORE && game.hasBody(body.id())) BodyArt.draw(g, body, cx, cy, 0.012 * unit + 2);
        }
    }

    /**
     * Une galaxie spirale : sa lueur, puis un point sur {@code step} pris dans les bras, tournés de
     * {@code turn}, écrasés de {@code squash} (1 = vue de face) le long d'un axe incliné de {@code tilt}.
     */
    private static void spiral(GraphicsContext g, Color[] colors, double cx, double cy, double radius, double turn, double squash,
                               double tilt, int step, double strength) {
        g.setGlobalAlpha(0.8 * strength * squash);
        g.setFill(new RadialGradient(0, 0, cx, cy, radius * 0.8, false, CycleMethod.NO_CYCLE, GLOW));
        g.fillOval(cx - radius * 0.8, cy - radius * 0.8, 1.6 * radius, 1.6 * radius);
        double cos = Math.cos(turn), sin = Math.sin(turn), across = Math.cos(tilt), down = Math.sin(tilt);
        double size = radius > 40 ? 0.9 : 0.65;
        g.setGlobalAlpha(0.9 * strength);
        for (int dot = 0; dot < DOTS; dot += step) {
            double out = AWAY[dot] * radius;
            double px = out * (COS[dot] * cos - SIN[dot] * sin);
            double py = out * (SIN[dot] * cos + COS[dot] * sin) * squash;
            g.setFill(colors[dot]);
            g.fillOval(cx + px * across - py * down - size, cy + px * down + py * across - size, 2 * size, 2 * size);
        }
    }

    /**
     * L'univers : une toile de filaments, des amas à leurs croisements, et du vide entre eux. Celui
     * du joueur est entouré, au milieu : à cette échelle une galaxie n'est plus qu'un point. La vue de
     * l'expansion de la matière s'en sert aussi ({@link SpaceView}).
     */
    static void universe(GraphicsContext g, double w, double h) {
        double left = 0.02 * w, top = 0.04 * h, wide = 0.96 * w, high = 0.92 * h;
        for (int i = 0; i < WEB_X.length; i++) {
            g.setGlobalAlpha(WEB_TINT[i] == 3 ? 0.95 : 0.5 + 0.4 * fraction(i * 1.93));
            g.setFill(WEB_COLORS[WEB_TINT[i]]);
            double size = WEB_SIZE[i];
            g.fillOval(left + WEB_X[i] * wide - size / 2, top + WEB_Y[i] * high - size / 2, size, size);
        }
        double hx = left + HOME_X * wide, hy = top + HOME_Y * high;
        g.setGlobalAlpha(0.9);
        g.setStroke(Color.web(GameApp.BIG_BANG_COLOR));
        g.setLineWidth(1);
        g.strokeOval(hx - 11, hy - 11, 22, 22);
        g.setGlobalAlpha(1);
        g.setFill(TEXT);
        g.setFont(Font.font(11));
        g.setTextAlign(TextAlignment.LEFT);
        g.fillText("Votre amas de galaxies", hx + 16, hy + 4);
    }

    /** Donne à chaque point la couleur d'une molécule du joueur, quand leur liste a changé. */
    private void color() {
        if (colored == game.state().moleculesVersion()) return;
        colored = game.state().moleculesVersion();
        tint(game, colors);
    }

    /** Donne à chacun des {@link #DOTS} points la couleur d'une molécule du joueur, prises à intervalles réguliers dans l'ordre où il les a créées. */
    static void tint(Game game, Color[] colors) {
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
