package idle.ui.bigbang;

import idle.core.universe.Skin;
import idle.core.universe.SkinVisual;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.StrokeLineCap;

/**
 * Les skins du cosmos (version 0.194) que montrent {@link GalaxyView} et, de loin, {@link SpaceView} :
 * la galaxie ({@link SkinVisual#GALAXY}), l'amas de galaxies ({@link SkinVisual#GALAXY_CLUSTER}) et la
 * toile de l'univers ({@link SkinVisual#COSMIC_WEB}). Chaque skin garde la place et la couleur des
 * points de la vue classique (les molécules du joueur, {@link GalaxyView#tint}) et les redessine autrement.
 *
 * <p><b>La galaxie</b> ({@link #galaxy}) : « Nautile », une coquille enroulée en spirale logarithmique,
 * ivoire cernée de brun, à loges séparées de cloisons, les points semés le long de la spire ; « Pelote de
 * laine », une boule de brins qui s'entrecroisent, chaque brin un arc de la couleur d'un point ; « Empreinte
 * digitale », des crêtes d'encre claire concentriques, interrompues, à même le ciel ; « Tourbillon », une eau
 * bleu-vert qui s'enroule vers une bonde noire, les points étirés en filets d'écume ; « Roue de feu
 * d'artifice », des étincelles chaudes qui filent le long des bras, chacune avec sa traînée.
 *
 * <p><b>L'amas</b> ({@link #clusterBackdrop}, {@link #clusterMember}) : « Grappe de raisin », les galaxies
 * en grains sur leur rafle ; « Lampions », en lanternes de papier pendues à un fil ; « Essaim d'abeilles »,
 * en abeilles rayées à deux ailes, qui vibrent.
 *
 * <p><b>La toile</b> ({@link #web}) : à partir des nœuds et des filaments de la vue classique : « Neurones »,
 * des corps cellulaires roses à dendrites, reliés par des axones ; « Dentelle », des fils blancs doubles à
 * picots, des rosaces aux nœuds ; « Toile au matin », des fils de soie gris chargés de gouttes ;
 * « Métro », des lignes épaisses de quatre couleurs et des stations cerclées de blanc.
 */
final class GalaxySkinArt {

    // Nautile
    private static final Color SHELL = Color.web("#f1e4cf");
    private static final Color SHELL_EDGE = Color.web("#8a5a3a");
    private static final Color SHELL_SEPTUM = Color.web("#b58a68");
    // Empreinte digitale
    /** Version 0.203 : l'encre claire de l'empreinte, posée sur le ciel ; la carte pâle derrière elle est retirée. */
    private static final Color PRINT = Color.web("#d9d4ea");
    private static final Color RIDGE = Color.web("#2a2a35");
    // Tourbillon
    private static final Color WATER = Color.web("#1f6f7a");
    private static final Color WATER_DEEP = Color.web("#0a2a33");
    private static final Color FOAM = Color.web("#dff6ff");
    // Roue de feu d'artifice
    private static final Color[] SPARKS = {Color.web("#fff3b0"), Color.web("#ffb347"), Color.web("#ff6b3d"), Color.web("#ffffff")};
    // Grappe de raisin
    private static final Color GRAPE = Color.web("#6a2c7a");
    private static final Color GRAPE_LIGHT = Color.web("#c48ad8");
    private static final Color GRAPE_GREEN = Color.web("#8cb34a");
    private static final Color STEM = Color.web("#7a5a2a");
    // Lampions
    private static final Color LANTERN = Color.web("#e8402a");
    private static final Color LANTERN_LIGHT = Color.web("#ffb060");
    private static final Color LANTERN_RIB = Color.web("#7a1a10");
    private static final Color STRING = Color.web("#c9b89a");
    // Essaim d'abeilles
    private static final Color BEE = Color.web("#f2c230");
    private static final Color BEE_STRIPE = Color.web("#1a1208");
    private static final Color WING = Color.web("#d8ecff");
    // Toile
    private static final Color SOMA = Color.web("#f2a0b8");
    private static final Color AXON = Color.web("#d87a98");
    private static final Color LACE = Color.web("#f4f1ea");
    private static final Color SILK = Color.web("#9aa3ad");
    private static final Color DEW = Color.web("#e6f6ff");
    private static final Color[] METRO = {Color.web("#e4002b"), Color.web("#ffbe00"), Color.web("#00a650"), Color.web("#0066b3")};
    private static final Color STATION = Color.web("#ffffff");

    private GalaxySkinArt() {}

    /** Vrai si ce skin est de l'un des trois visuels du cosmos : {@code null} ou un autre visuel, c'est le Classique. */
    static boolean draws(Skin skin) {
        return skin != null && (skin.visual() == SkinVisual.GALAXY || skin.visual() == SkinVisual.GALAXY_CLUSTER
                || skin.visual() == SkinVisual.COSMIC_WEB);
    }

    // ------------------------------------------------------------------
    // La galaxie
    // ------------------------------------------------------------------

    /**
     * La galaxie dans ce skin, centrée en ({@code cx}, {@code cy}) dans un rayon de {@code radius} pixels.
     *
     * @param turn     l'angle dont elle a tourné
     * @param colors   la couleur de chaque point, {@code away}, {@code cos} et {@code sin} sa place dans les bras
     * @param strength 1 formée, moins pour un aperçu
     */
    static void galaxy(GraphicsContext g, Skin skin, double cx, double cy, double radius, double turn, Color[] colors, double[] away,
                       double[] cos, double[] sin, double strength) {
        g.setGlobalAlpha(strength);
        switch (skin) {
            case GALAXY_NAUTILUS -> nautilus(g, cx, cy, radius, turn, colors);
            case GALAXY_YARN -> yarn(g, cx, cy, radius, turn, colors);
            case GALAXY_FINGERPRINT -> fingerprint(g, cx, cy, radius, turn);
            case GALAXY_WHIRLPOOL -> whirlpool(g, cx, cy, radius, turn, colors, away, cos, sin);
            case GALAXY_FIREWORK -> firework(g, cx, cy, radius, turn, colors, away, cos, sin);
            default -> { }
        }
        g.setGlobalAlpha(1);
    }

    /** Nautile : trois tours de spirale logarithmique, la coquille entre deux tours, ses cloisons, et les points semés dedans. */
    private static void nautilus(GraphicsContext g, double cx, double cy, double radius, double turn, Color[] colors) {
        int steps = 180;
        double turns = 3;
        // r = radius × e^(k(θ − θmax)) : au bord à la fin du dernier tour, au centre au début.
        double k = Math.log(0.06) / (-turns * 2 * Math.PI);
        double[] xs = new double[steps + 1], ys = new double[steps + 1];
        for (int i = 0; i <= steps; i++) {
            double theta = turns * 2 * Math.PI * i / steps;
            double r = radius * Math.exp(k * (theta - turns * 2 * Math.PI));
            xs[i] = cx + r * Math.cos(theta + turn);
            ys[i] = cy + r * Math.sin(theta + turn);
        }
        // La coquille : une bande ivoire entre la spire et la précédente, large d'un tour, puis le bord brun.
        g.setLineCap(StrokeLineCap.ROUND);
        g.setStroke(SHELL);
        for (int i = 1; i <= steps; i++) {
            double theta = turns * 2 * Math.PI * i / steps;
            double r = radius * Math.exp(k * (theta - turns * 2 * Math.PI));
            double inner = r * Math.exp(-k * 2 * Math.PI);
            g.setLineWidth(Math.max(1, r - inner));
            double mx = cx + (r + inner) / 2 * Math.cos(theta + turn), my = cy + (r + inner) / 2 * Math.sin(theta + turn);
            double px = cx + (r + inner) / 2 * Math.cos(theta - 2 * Math.PI / steps * turns + turn);
            double py = cy + (r + inner) / 2 * Math.sin(theta - 2 * Math.PI / steps * turns + turn);
            g.strokeLine(px, py, mx, my);
        }
        g.setStroke(SHELL_EDGE);
        g.setLineWidth(Math.max(1, radius * 0.012));
        g.strokePolyline(xs, ys, steps + 1);
        // Les cloisons : tous les quinze degrés, de la spire à la précédente, courbées vers l'avant.
        g.setStroke(SHELL_SEPTUM);
        g.setLineWidth(Math.max(0.7, radius * 0.006));
        for (int i = 0; i <= steps; i += steps / (int) (turns * 24)) {
            double theta = turns * 2 * Math.PI * i / steps;
            double r = radius * Math.exp(k * (theta - turns * 2 * Math.PI));
            double inner = r * Math.exp(-k * 2 * Math.PI);
            if (r - inner < 3) continue;
            double a = theta + turn;
            g.strokeLine(cx + r * Math.cos(a), cy + r * Math.sin(a), cx + inner * Math.cos(a + 0.12), cy + inner * Math.sin(a + 0.12));
        }
        // Les points : dans la bande, à parts égales le long de la spire, en petit.
        double size = Math.max(0.6, radius * 0.0045);
        for (int dot = 0; dot < colors.length; dot += 3) {
            double share = (dot + 0.5) / colors.length;
            double theta = turns * 2 * Math.PI * share;
            double r = radius * Math.exp(k * (theta - turns * 2 * Math.PI));
            double inner = r * Math.exp(-k * 2 * Math.PI);
            double at = inner + (r - inner) * (0.2 + 0.6 * fraction(dot * 7.31));
            g.setFill(colors[dot]);
            g.fillOval(cx + at * Math.cos(theta + turn) - size, cy + at * Math.sin(theta + turn) - size, 2 * size, 2 * size);
        }
        g.setLineCap(StrokeLineCap.SQUARE);
    }

    /** Pelote de laine : une boule ronde, des brins en arcs qui la traversent dans tous les sens, chacun de la couleur d'un point. */
    private static void yarn(GraphicsContext g, double cx, double cy, double radius, double turn, Color[] colors) {
        double ball = radius * 0.88;
        g.setFill(new RadialGradient(0, 0, cx - ball * 0.3, cy - ball * 0.3, ball * 1.4, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#3a2e3a")), new Stop(1, Color.web("#12101a"))));
        g.fillOval(cx - ball, cy - ball, 2 * ball, 2 * ball);
        g.setLineCap(StrokeLineCap.ROUND);
        g.setLineWidth(Math.max(1.2, radius * 0.018));
        int strands = 70;
        for (int strand = 0; strand < strands; strand++) {
            // Un brin : un arc d'ellipse inscrit dans la boule, incliné à sa façon, qui tourne lentement avec elle.
            double tilt = fraction(strand * 3.17) * Math.PI + turn * (strand % 2 == 0 ? 1 : -1) * 0.3;
            double squash = 0.15 + 0.8 * fraction(strand * 5.71);
            double from = fraction(strand * 1.93) * 2 * Math.PI, span = 1.2 + 1.6 * fraction(strand * 7.13);
            int steps = 18;
            double[] xs = new double[steps + 1], ys = new double[steps + 1];
            for (int i = 0; i <= steps; i++) {
                double a = from + span * i / steps;
                double px = ball * 0.96 * Math.cos(a), py = ball * 0.96 * Math.sin(a) * squash;
                xs[i] = cx + px * Math.cos(tilt) - py * Math.sin(tilt);
                ys[i] = cy + px * Math.sin(tilt) + py * Math.cos(tilt);
            }
            g.setStroke(colors[(strand * 23) % colors.length].interpolate(Color.WHITE, 0.15));
            g.strokePolyline(xs, ys, steps + 1);
        }
        // Le bout du fil, qui dépasse.
        g.setStroke(colors[0]);
        g.strokeLine(cx + ball * 0.7, cy + ball * 0.6, cx + radius * 1.02, cy + radius * 0.75);
        g.setLineCap(StrokeLineCap.SQUARE);
    }

    /**
     * Empreinte digitale : des crêtes d'encre claire concentriques, ondulées et interrompues çà et là, à même le ciel.
     * Version 0.203 : la carte crème qui les portait (un carré jaune pâle au milieu du ciel noir) est retirée.
     */
    private static void fingerprint(GraphicsContext g, double cx, double cy, double radius, double turn) {
        // Une lueur douce sous les crêtes, pour que l'empreinte se détache du ciel.
        g.setFill(new RadialGradient(0, 0, cx, cy, radius * 1.05, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.color(0.55, 0.5, 0.85, 0.28)), new Stop(1, Color.color(0.55, 0.5, 0.85, 0))));
        g.fillOval(cx - radius * 1.05, cy - radius * 1.05, 2.1 * radius, 2.1 * radius);
        g.setStroke(PRINT);
        g.setLineWidth(Math.max(0.9, radius * 0.014));
        g.setLineCap(StrokeLineCap.ROUND);
        int ridges = 16;
        int steps = 96;
        for (int ridge = 1; ridge <= ridges; ridge++) {
            double base = radius * 0.95 * ridge / ridges;
            // Chaque crête est tracée par morceaux : un segment sur cinq manque, décalé d'une crête à l'autre.
            for (int piece = 0; piece < steps; piece++) {
                if ((piece + ridge * 3) % 7 == 0) continue;
                double a0 = 2 * Math.PI * piece / steps, a1 = 2 * Math.PI * (piece + 1) / steps;
                double r0 = base * (1 + 0.07 * Math.sin(3 * a0 + ridge * 0.4) + 0.04 * Math.sin(5 * a0 - ridge));
                double r1 = base * (1 + 0.07 * Math.sin(3 * a1 + ridge * 0.4) + 0.04 * Math.sin(5 * a1 - ridge));
                g.strokeLine(cx + r0 * Math.cos(a0 + turn), cy + r0 * 0.92 * Math.sin(a0 + turn),
                        cx + r1 * Math.cos(a1 + turn), cy + r1 * 0.92 * Math.sin(a1 + turn));
            }
        }
        g.setLineCap(StrokeLineCap.SQUARE);
    }

    /** Tourbillon : une eau bleu-vert, les points étirés en filets d'écume le long des bras, une bonde sombre au centre. */
    private static void whirlpool(GraphicsContext g, double cx, double cy, double radius, double turn, Color[] colors, double[] away,
                                  double[] cos, double[] sin) {
        g.setFill(new RadialGradient(0, 0, cx, cy, radius, false, CycleMethod.NO_CYCLE,
                new Stop(0, WATER_DEEP), new Stop(0.3, WATER), new Stop(1, WATER.interpolate(WATER_DEEP, 0.6))));
        g.fillOval(cx - radius, cy - radius, 2 * radius, 2 * radius);
        double c = Math.cos(turn), s = Math.sin(turn);
        g.setLineCap(StrokeLineCap.ROUND);
        g.setLineWidth(Math.max(0.8, radius * 0.006));
        for (int dot = 0; dot < colors.length; dot++) {
            double out = away[dot] * radius;
            if (out < radius * 0.08) continue;
            double x = cx + out * (cos[dot] * c - sin[dot] * s);
            double y = cy + out * (sin[dot] * c + cos[dot] * s);
            // Le filet : tangent au cercle, long d'un dixième du rayon, blanc d'écume mêlé à la couleur du point.
            double tx = -(sin[dot] * c + cos[dot] * s), ty = cos[dot] * c - sin[dot] * s;
            double length = radius * (0.04 + 0.08 * fraction(dot * 2.13));
            g.setStroke(FOAM.interpolate(colors[dot], 0.35));
            g.strokeLine(x, y, x + tx * length, y + ty * length);
        }
        double drain = radius * 0.09;
        g.setFill(Color.web("#020608"));
        g.fillOval(cx - drain, cy - drain, 2 * drain, 2 * drain);
        g.setStroke(FOAM);
        g.setLineWidth(1);
        g.strokeOval(cx - drain, cy - drain, 2 * drain, 2 * drain);
        g.setLineCap(StrokeLineCap.SQUARE);
    }

    /** Roue de feu d'artifice : chaque point une étincelle chaude au bout d'une traînée qui part vers le centre, les plus loin les plus vives. */
    private static void firework(GraphicsContext g, double cx, double cy, double radius, double turn, Color[] colors, double[] away,
                                 double[] cos, double[] sin) {
        double c = Math.cos(turn), s = Math.sin(turn);
        g.setLineCap(StrokeLineCap.ROUND);
        for (int dot = 0; dot < colors.length; dot += 2) {
            double out = away[dot] * radius;
            double x = cx + out * (cos[dot] * c - sin[dot] * s);
            double y = cy + out * (sin[dot] * c + cos[dot] * s);
            Color spark = SPARKS[dot % SPARKS.length];
            // La traînée : vers l'arrière le long du bras, donc vers le centre et en retard sur la rotation.
            double tail = radius * (0.03 + 0.06 * fraction(dot * 3.37));
            double bx = cx + (out - tail) * (cos[dot] * c - sin[dot] * s) + tail * 0.6 * (sin[dot] * c + cos[dot] * s);
            double by = cy + (out - tail) * (sin[dot] * c + cos[dot] * s) - tail * 0.6 * (cos[dot] * c - sin[dot] * s);
            g.setStroke(spark.interpolate(Color.TRANSPARENT, 0.5));
            g.setLineWidth(Math.max(0.6, radius * 0.004));
            g.strokeLine(bx, by, x, y);
            double size = (dot % 9 == 0 ? 1.8 : 1) * Math.max(0.8, radius * 0.006) * (0.6 + away[dot]);
            g.setFill(spark);
            g.fillOval(x - size, y - size, 2 * size, 2 * size);
        }
        // Le cœur : une lueur blanche qui bat.
        double core = radius * 0.08;
        g.setFill(new RadialGradient(0, 0, cx, cy, core, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.WHITE), new Stop(1, Color.web("#ffb347", 0))));
        g.fillOval(cx - core, cy - core, 2 * core, 2 * core);
        g.setLineCap(StrokeLineCap.SQUARE);
    }

    // ------------------------------------------------------------------
    // L'amas de galaxies
    // ------------------------------------------------------------------

    /**
     * Version 0.203 : range les membres de l'amas en grappe pour le skin « Grappe de raisin ». Les grains se touchent, par
     * rangs de plus en plus courts vers le bas (trois, trois, deux, un pour neuf membres) ; celui du joueur, le premier,
     * prend le milieu du deuxième rang. Les autres skins gardent les places que la vue leur donne.
     */
    static void bunch(double[] xs, double[] ys, double[] radii, double w, double h) {
        int count = xs.length, first = 1;
        while (first * (first + 1) / 2 < count) first++;
        double grain = Math.min(0.78 * h / (2 + 1.7 * (first - 1)), 0.45 * w / first);
        double top = h - 0.04 * h - (2 + 1.7 * (first - 1)) * grain;
        // Les grains qui manquent pour un triangle plein manquent au rang du haut, raccourci : la pointe du bas reste.
        int missing = first * (first + 1) / 2 - count;
        int other = 1;
        for (int row = 0; row < first; row++) {
            int inRow = first - row - (row == 0 ? missing : 0);
            for (int k = 0; k < inRow; k++) {
                // Le grain du joueur : au milieu du deuxième rang (du seul rang, s'il n'y en a qu'un).
                int member = row == Math.min(1, first - 1) && k == (inRow - 1) / 2 ? 0 : other++;
                if (member >= count) continue;
                // Un rang du haut raccourci d'un nombre impair de grains se décale d'un demi-grain : il reste en quinconce.
                double shift = row == 0 && missing % 2 == 1 && first > 1 ? 0.98 * grain : 0;
                xs[member] = w / 2 + shift + (k - (inRow - 1) / 2.0) * 1.96 * grain;
                ys[member] = top + grain + row * 1.7 * grain;
                radii[member] = member == 0 ? grain * 1.04 : grain * (0.9 + 0.1 * fraction(member * 3.1));
            }
        }
    }

    /**
     * Le fond de l'amas dans ce skin, avant ses membres : la rafle de la grappe (une tige depuis le haut,
     * une feuille et une vrille), le fil des lampions, le vol zigzagant des abeilles.
     *
     * @param xs les centres des membres, {@code radii} leurs rayons ; le premier est la galaxie du joueur
     */
    static void clusterBackdrop(GraphicsContext g, Skin skin, double w, double h, double[] xs, double[] ys, double[] radii, double time) {
        g.setGlobalAlpha(1);
        switch (skin) {
            case CLUSTER_GRAPES -> {
                // Version 0.203 : une vraie grappe ({@link #bunch} a serré les grains). La rafle descend du haut jusqu'aux
                // premiers grains, une feuille à trois lobes et une vrille à son départ. Avant : des grains épars reliés
                // au centre par des traits droits.
                double top = Double.MAX_VALUE, grain = radii[0];
                for (int member = 0; member < xs.length; member++) top = Math.min(top, ys[member] - radii[member]);
                // La rafle part du milieu des grains du haut.
                double stemX = 0;
                int topmost = 0;
                for (int member = 0; member < xs.length; member++) {
                    if (ys[member] - radii[member] < top + grain * 0.5) {
                        stemX += xs[member];
                        topmost++;
                    }
                }
                stemX = topmost == 0 ? w / 2 : stemX / topmost;
                g.setLineCap(StrokeLineCap.ROUND);
                g.setStroke(STEM);
                g.setLineWidth(Math.max(3, grain * 0.22));
                g.beginPath();
                g.moveTo(stemX + grain * 0.5, 0);
                g.quadraticCurveTo(stemX - grain * 0.3, top * 0.5, stemX, top + grain * 0.5);
                g.stroke();
                // La feuille : trois lobes pointus autour d'une nervure, à droite de la rafle.
                double lx = stemX + grain * 0.35, ly = Math.max(2, top * 0.35), leaf = grain * 1.7;
                g.setFill(GRAPE_GREEN.deriveColor(0, 1, 0.75, 1));
                g.beginPath();
                g.moveTo(lx, ly);
                g.bezierCurveTo(lx + leaf * 0.2, ly - leaf * 0.55, lx + leaf * 0.7, ly - leaf * 0.5, lx + leaf * 0.75, ly - leaf * 0.15);
                g.bezierCurveTo(lx + leaf * 1.1, ly - leaf * 0.25, lx + leaf * 1.35, ly + leaf * 0.05, lx + leaf * 1.3, ly + leaf * 0.3);
                g.bezierCurveTo(lx + leaf * 1.05, ly + leaf * 0.35, lx + leaf * 0.95, ly + leaf * 0.6, lx + leaf * 0.7, ly + leaf * 0.75);
                g.bezierCurveTo(lx + leaf * 0.45, ly + leaf * 0.55, lx + leaf * 0.15, ly + leaf * 0.4, lx, ly);
                g.closePath();
                g.fill();
                g.setStroke(GRAPE_GREEN);
                g.setLineWidth(1.2);
                g.strokeLine(lx, ly, lx + leaf * 1.1, ly + leaf * 0.22);
                g.strokeLine(lx + leaf * 0.35, ly + leaf * 0.07, lx + leaf * 0.7, ly - leaf * 0.25);
                g.strokeLine(lx + leaf * 0.4, ly + leaf * 0.08, lx + leaf * 0.68, ly + leaf * 0.55);
                // La vrille : une boucle qui s'enroule, à gauche.
                g.setLineWidth(1.4);
                g.beginPath();
                double vx = stemX - grain * 0.1, vy = Math.max(3, top * 0.45);
                g.moveTo(vx, vy);
                for (int k = 1; k <= 28; k++) {
                    double a = k * 0.42, r = grain * (0.16 + 0.03 * k) * 0.55;
                    g.lineTo(vx - grain * 0.045 * k * 1.6 + r * Math.cos(a) - r, vy + r * Math.sin(a));
                }
                g.stroke();
                g.setLineCap(StrokeLineCap.SQUARE);
            }
            case CLUSTER_LANTERNS -> {
                // Un fil en guirlande d'un bord à l'autre, qui pend un peu, et de chaque lampion un fil jusqu'à lui.
                g.setStroke(STRING);
                g.setLineWidth(1.2);
                int steps = 40;
                double[] px = new double[steps + 1], py = new double[steps + 1];
                for (int i = 0; i <= steps; i++) {
                    double t = (double) i / steps;
                    px[i] = w * t;
                    py[i] = 10 + 0.08 * h * Math.sin(Math.PI * t);
                }
                g.strokePolyline(px, py, steps + 1);
                for (int member = 0; member < xs.length; member++) {
                    double hang = 10 + 0.08 * h * Math.sin(Math.PI * xs[member] / w);
                    g.strokeLine(xs[member], hang, xs[member], ys[member] - radii[member]);
                }
            }
            case CLUSTER_BEES -> {
                g.setStroke(WING.interpolate(Color.TRANSPARENT, 0.6));
                g.setLineWidth(1);
                for (int member = 1; member < xs.length; member++) {
                    // Le vol : un zigzag pointillé derrière chaque abeille, qui vibre avec elle.
                    double x = xs[member], y = ys[member];
                    for (int k = 0; k < 6; k++) {
                        double x0 = x - 8 * k - 6, x1 = x0 - 6;
                        double y0 = y + 4 * ((k + (int) (time * 6)) % 2 == 0 ? 1 : -1), y1 = y - 4 * ((k + (int) (time * 6)) % 2 == 0 ? 1 : -1);
                        g.strokeLine(x0, y0, x1, y1);
                    }
                }
            }
            default -> { }
        }
    }

    /**
     * Un membre de l'amas dans ce skin : un grain de raisin, un lampion ou une abeille, centré en
     * ({@code cx}, {@code cy}) dans un rayon de {@code radius} pixels ; {@code own} vrai pour la galaxie du
     * joueur, dessinée un peu plus vive ; {@code rank} le rang du membre, pour varier les grains et les ailes.
     */
    static void clusterMember(GraphicsContext g, Skin skin, double cx, double cy, double radius, boolean own, int rank, double time) {
        g.setGlobalAlpha(1);
        switch (skin) {
            case CLUSTER_GRAPES -> {
                // Version 0.203 : tous les grains du même violet, plus ou moins mûr (plus de grains verts), cernés d'une
                // ombre, la pruine en voile bleuté et un reflet ; celui du joueur, plus clair, a un liseré doré.
                Color body = rank % 3 == 1 ? GRAPE.interpolate(Color.web("#3a2a7a"), 0.45) : rank % 3 == 2 ? GRAPE.interpolate(Color.web("#8a2a5a"), 0.4) : GRAPE;
                g.setFill(Color.color(0, 0, 0, 0.35));
                g.fillOval(cx - radius * 1.04, cy - radius * 0.98, 2.08 * radius, 2.08 * radius);
                g.setFill(new RadialGradient(0, 0, cx - radius * 0.3, cy - radius * 0.35, radius * 1.35, false, CycleMethod.NO_CYCLE,
                        new Stop(0, body.interpolate(GRAPE_LIGHT, own ? 0.65 : 0.35)), new Stop(0.55, body), new Stop(1, body.deriveColor(0, 1, 0.45, 1))));
                g.fillOval(cx - radius, cy - radius, 2 * radius, 2 * radius);
                g.setFill(new RadialGradient(0, 0, cx + radius * 0.2, cy + radius * 0.3, radius * 0.9, false, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.color(0.75, 0.8, 1, 0.16)), new Stop(1, Color.color(0.75, 0.8, 1, 0))));
                g.fillOval(cx - radius, cy - radius, 2 * radius, 2 * radius);
                g.setFill(Color.WHITE);
                g.setGlobalAlpha(0.75);
                g.fillOval(cx - radius * 0.52, cy - radius * 0.58, radius * 0.3, radius * 0.2);
                g.setGlobalAlpha(1);
                if (own) {
                    g.setStroke(Color.web("#ffd98a"));
                    g.setLineWidth(1.5);
                    g.strokeOval(cx - radius, cy - radius, 2 * radius, 2 * radius);
                }
            }
            case CLUSTER_LANTERNS -> {
                double wide = radius * 0.85, tall = radius;
                // La lueur sous le lampion, puis le papier, ses côtes, ses deux bouchons et sa frange.
                g.setFill(new RadialGradient(0, 0, cx, cy, radius * 1.8, false, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.color(1, 0.6, 0.3, own ? 0.45 : 0.3)), new Stop(1, Color.color(1, 0.6, 0.3, 0))));
                g.fillOval(cx - radius * 1.8, cy - radius * 1.8, radius * 3.6, radius * 3.6);
                g.setFill(new RadialGradient(0, 0, cx, cy, radius, false, CycleMethod.NO_CYCLE,
                        new Stop(0, LANTERN_LIGHT), new Stop(1, LANTERN)));
                g.fillOval(cx - wide, cy - tall, 2 * wide, 2 * tall);
                g.setStroke(LANTERN_RIB);
                g.setLineWidth(1);
                for (int rib = -2; rib <= 2; rib++) {
                    double rx = wide * rib / 3.0;
                    g.strokeOval(cx - Math.abs(rx) - 0.5, cy - tall, 2 * Math.abs(rx) + 1, 2 * tall);
                }
                g.setFill(LANTERN_RIB);
                g.fillRect(cx - wide * 0.4, cy - tall - 3, wide * 0.8, 3);
                g.fillRect(cx - wide * 0.4, cy + tall, wide * 0.8, 3);
                g.setStroke(LANTERN_LIGHT);
                g.strokeLine(cx, cy + tall + 3, cx, cy + tall + 3 + radius * 0.4);
            }
            case CLUSTER_BEES -> {
                double body = radius * 0.7, tall = radius * 0.45;
                double flap = Math.sin(time * 18 + rank) * 0.3;
                // Les ailes, qui battent, puis le corps rayé et la tête.
                g.setFill(WING);
                g.setGlobalAlpha(0.6);
                g.fillOval(cx - body * 0.5, cy - tall - radius * 0.6 * (1 + flap), body * 0.6, radius * 0.6 * (1 + flap));
                g.fillOval(cx - body * 0.1, cy - tall - radius * 0.55 * (1 - flap), body * 0.6, radius * 0.55 * (1 - flap));
                g.setGlobalAlpha(1);
                g.setFill(BEE);
                g.fillOval(cx - body, cy - tall, 2 * body, 2 * tall);
                g.setFill(BEE_STRIPE);
                for (int stripe = -1; stripe <= 1; stripe++) {
                    double sx = cx + stripe * body * 0.45;
                    double half = tall * Math.sqrt(Math.max(0, 1 - Math.pow(stripe * 0.45, 2)));
                    g.fillRect(sx - body * 0.09, cy - half, body * 0.18, 2 * half);
                }
                g.fillOval(cx + body * 0.85, cy - tall * 0.6, tall * 1.2, tall * 1.2);
                if (own) {
                    g.setStroke(BEE);
                    g.setLineWidth(1);
                    g.strokeOval(cx - radius * 1.3, cy - radius, radius * 2.6, radius * 2);
                }
            }
            default -> { }
        }
    }

    // ------------------------------------------------------------------
    // La toile de l'univers
    // ------------------------------------------------------------------

    /**
     * La toile dans ce skin, dans le rectangle ({@code left}, {@code top}, {@code wide}, {@code high}) : les
     * nœuds en parts de ce rectangle ({@code nodeX}, {@code nodeY}), les filaments par paires de nœuds
     * ({@code edges}), le nœud du joueur ({@code home}).
     */
    static void web(GraphicsContext g, Skin skin, double left, double top, double wide, double high, double[] nodeX, double[] nodeY,
                    int[] edges, int home) {
        g.setGlobalAlpha(1);
        g.setLineCap(StrokeLineCap.ROUND);
        switch (skin) {
            case WEB_NEURONS -> {
                g.setStroke(AXON);
                g.setLineWidth(1.4);
                g.setGlobalAlpha(0.8);
                for (int edge = 0; edge < edges.length; edge += 2) {
                    double x0 = left + nodeX[edges[edge]] * wide, y0 = top + nodeY[edges[edge]] * high;
                    double x1 = left + nodeX[edges[edge + 1]] * wide, y1 = top + nodeY[edges[edge + 1]] * high;
                    // Un axone légèrement courbe : par le milieu décalé.
                    double mx = (x0 + x1) / 2 + (y1 - y0) * 0.12, my = (y0 + y1) / 2 - (x1 - x0) * 0.12;
                    g.strokePolyline(new double[] {x0, mx, x1}, new double[] {y0, my, y1}, 3);
                }
                g.setGlobalAlpha(1);
                for (int node = 0; node < nodeX.length; node++) {
                    double x = left + nodeX[node] * wide, y = top + nodeY[node] * high;
                    double soma = node == home ? 9 : 6 + 2 * fraction(node * 3.3);
                    g.setStroke(SOMA);
                    g.setLineWidth(1);
                    for (int dendrite = 0; dendrite < 5; dendrite++) {
                        double a = 2 * Math.PI * dendrite / 5 + fraction(node * 1.7 + dendrite) * 0.8;
                        double length = soma * (1.6 + 1.2 * fraction(node * 2.9 + dendrite * 1.3));
                        g.strokeLine(x + soma * 0.7 * Math.cos(a), y + soma * 0.7 * Math.sin(a), x + length * Math.cos(a + 0.3), y + length * Math.sin(a + 0.3));
                    }
                    g.setFill(new RadialGradient(0, 0, x, y, soma * 1.1, false, CycleMethod.NO_CYCLE,
                            new Stop(0, Color.WHITE.interpolate(SOMA, 0.4)), new Stop(1, SOMA.deriveColor(0, 1, 0.7, 1))));
                    g.fillOval(x - soma, y - soma, 2 * soma, 2 * soma);
                }
            }
            case WEB_LACE -> {
                g.setStroke(LACE);
                for (int edge = 0; edge < edges.length; edge += 2) {
                    double x0 = left + nodeX[edges[edge]] * wide, y0 = top + nodeY[edges[edge]] * high;
                    double x1 = left + nodeX[edges[edge + 1]] * wide, y1 = top + nodeY[edges[edge + 1]] * high;
                    double length = Math.hypot(x1 - x0, y1 - y0), nx = -(y1 - y0) / length, ny = (x1 - x0) / length;
                    // Deux fils parallèles, et des picots ronds entre eux, tous les douze pixels.
                    g.setLineWidth(0.9);
                    g.strokeLine(x0 + 2 * nx, y0 + 2 * ny, x1 + 2 * nx, y1 + 2 * ny);
                    g.strokeLine(x0 - 2 * nx, y0 - 2 * ny, x1 - 2 * nx, y1 - 2 * ny);
                    for (double along = 12; along < length - 6; along += 12) {
                        double t = along / length;
                        g.strokeOval(x0 + t * (x1 - x0) - 2, y0 + t * (y1 - y0) - 2, 4, 4);
                    }
                }
                for (int node = 0; node < nodeX.length; node++) {
                    double x = left + nodeX[node] * wide, y = top + nodeY[node] * high;
                    double petal = node == home ? 7 : 5;
                    g.setLineWidth(1);
                    for (int k = 0; k < 6; k++) {
                        double a = Math.PI * k / 3;
                        g.strokeOval(x + petal * 0.9 * Math.cos(a) - petal * 0.55, y + petal * 0.9 * Math.sin(a) - petal * 0.55, petal * 1.1, petal * 1.1);
                    }
                    g.setFill(LACE);
                    g.fillOval(x - 2, y - 2, 4, 4);
                }
            }
            case WEB_DEW -> {
                g.setStroke(SILK);
                g.setLineWidth(0.8);
                g.setGlobalAlpha(0.8);
                for (int edge = 0; edge < edges.length; edge += 2) {
                    double x0 = left + nodeX[edges[edge]] * wide, y0 = top + nodeY[edges[edge]] * high;
                    double x1 = left + nodeX[edges[edge + 1]] * wide, y1 = top + nodeY[edges[edge + 1]] * high;
                    // Le fil pend un peu sous son poids d'eau.
                    double mx = (x0 + x1) / 2, my = (y0 + y1) / 2 + 6;
                    g.strokePolyline(new double[] {x0, mx, x1}, new double[] {y0, my, y1}, 3);
                }
                g.setGlobalAlpha(1);
                for (int edge = 0; edge < edges.length; edge += 2) {
                    double x0 = left + nodeX[edges[edge]] * wide, y0 = top + nodeY[edges[edge]] * high;
                    double x1 = left + nodeX[edges[edge + 1]] * wide, y1 = top + nodeY[edges[edge + 1]] * high;
                    double length = Math.hypot(x1 - x0, y1 - y0);
                    for (double along = 9; along < length - 4; along += 9 + 6 * fraction(edge * 1.3 + along)) {
                        double t = along / length;
                        double drop = 1.2 + 1.6 * fraction(edge * 2.7 + along * 0.31);
                        double x = x0 + t * (x1 - x0), y = y0 + t * (y1 - y0) + 6 * Math.sin(Math.PI * t);
                        g.setFill(DEW);
                        g.fillOval(x - drop, y - drop, 2 * drop, 2 * drop);
                        g.setFill(Color.WHITE);
                        g.fillOval(x - drop * 0.5, y - drop * 0.6, drop * 0.5, drop * 0.5);
                    }
                }
                for (int node = 0; node < nodeX.length; node++) {
                    double x = left + nodeX[node] * wide, y = top + nodeY[node] * high;
                    double drop = node == home ? 5 : 3.5;
                    g.setFill(DEW);
                    g.fillOval(x - drop, y - drop, 2 * drop, 2 * drop);
                    g.setFill(Color.WHITE);
                    g.fillOval(x - drop * 0.5, y - drop * 0.6, drop * 0.6, drop * 0.6);
                }
            }
            case WEB_METRO -> {
                g.setLineWidth(5);
                for (int edge = 0; edge < edges.length; edge += 2) {
                    double x0 = left + nodeX[edges[edge]] * wide, y0 = top + nodeY[edges[edge]] * high;
                    double x1 = left + nodeX[edges[edge + 1]] * wide, y1 = top + nodeY[edges[edge + 1]] * high;
                    // Une ligne de métro va droit, puis tourne à 45° : un coude au milieu.
                    double dx = x1 - x0, dy = y1 - y0;
                    double kneeX = Math.abs(dx) > Math.abs(dy) ? x1 - Math.signum(dx) * Math.abs(dy) : x0;
                    double kneeY = Math.abs(dx) > Math.abs(dy) ? y0 : y1 - Math.signum(dy) * Math.abs(dx);
                    g.setStroke(METRO[(edge / 2) % METRO.length]);
                    g.strokePolyline(new double[] {x0, kneeX, x1}, new double[] {y0, kneeY, y1}, 3);
                }
                for (int node = 0; node < nodeX.length; node++) {
                    double x = left + nodeX[node] * wide, y = top + nodeY[node] * high;
                    double station = node == home ? 7 : 4.5;
                    g.setFill(STATION);
                    g.fillOval(x - station, y - station, 2 * station, 2 * station);
                    g.setStroke(RIDGE);
                    g.setLineWidth(2);
                    g.strokeOval(x - station, y - station, 2 * station, 2 * station);
                }
            }
            default -> { }
        }
        g.setLineCap(StrokeLineCap.SQUARE);
    }

    private static double fraction(double seed) {
        double value = Math.sin(seed) * 43758.5453;
        return value - Math.floor(value);
    }
}
