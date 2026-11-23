package idle.ui.universe;

import idle.core.Game;
import idle.core.math.BigNum;
import idle.ui.common.Format;
import java.util.function.IntConsumer;
import javafx.geometry.*;
import javafx.scene.Cursor;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.*;
import javafx.scene.robot.Robot;
import javafx.scene.shape.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import javafx.scene.canvas.*;
import javafx.scene.text.*;

/** Une partie de {@link DimensionView} (découpée en 0.293 pour rester sous 1 000 lignes) : ses champs, ses types, ses couleurs et ce qui sert à toutes les dimensions. */
public abstract class DimensionViewBase extends Canvas {
    // Ce que fournissent les parties suivantes.
    abstract Cursor cursorAt(double x, double y);
    abstract boolean dragMix(double x, double y);
    abstract boolean freeInside();
    abstract boolean gestured(boolean done);
    abstract boolean pressBrane(double x, double y);
    abstract boolean pressClicker(double x, double y);
    abstract boolean pressDuration(double x, double y);
    abstract boolean pressGrid(double x, double y);
    abstract boolean pressLoop(double x, double y);
    abstract boolean pressMirror(double x, double y);
    abstract boolean pressMix(double x, double y, boolean secondary);
    abstract boolean windowFocused();
    abstract double insideFocal();
    abstract public Game.MirrorCell mirrorCellAt(double x, double y);
    abstract public boolean dragBrane(double x, double y);
    abstract public boolean dragTo(double x, double y);
    abstract public boolean freeLook(double x, double y);
    abstract public boolean overBrane(double x, double y);
    abstract public boolean overClicker(double x, double y);
    abstract public boolean overDurationButton(double x, double y);
    abstract public boolean press(double x, double y, boolean secondary);
    abstract public boolean releaseBrane();
    abstract public boolean releaseHold();
    abstract public boolean releaseMix(boolean secondary);
    abstract public double[] mixAt(double sx, double sy);
    abstract public int loopRingAt(double x, double y);
    abstract public int[] gridCellAt(double x, double y);
    abstract public void cancelBrane();
    abstract public void resetDive();
    abstract public void shiftGridLayer(int step);
    abstract public void shiftMixDepth(int steps);
    abstract void braneScene(GraphicsContext g, double cx, double cy);
    abstract void clickerScene(GraphicsContext g, double cx, double cy);
    abstract void draw();
    abstract void durationPickup(double reward);
    abstract void durationScene(GraphicsContext g, double cx, double cy, double u);
    abstract void gesture();
    abstract void gridScene(GraphicsContext g, double w, double h);
    abstract void hoverClicker(double x, double y);
    abstract void hoverGrid(double x, double y);
    abstract void loopScene(GraphicsContext g, double cx, double cy);
    abstract void mirrorScene(GraphicsContext g, double w, double h);
    abstract void mixScene(GraphicsContext g, double w, double h, double cx, double cy, double u);
    abstract void notePickups(double elapsed);
    abstract void noteTouches(double elapsed, boolean moving);
    abstract void recenter();
    abstract void runBrane(double elapsed);
    abstract void runClicker(double elapsed);
    abstract void runDive(double elapsed);
    abstract void runGrid(double elapsed);
    abstract void runLoop(double elapsed);
    abstract void runMirror(double elapsed);
    abstract void runMix(double dt);

    static final Color BACK = Color.web("#05060e");

    // ------------------------------------------------------------------
    // Les skins des dix mini-jeux : fonds, décors et couleurs, en cache
    // ------------------------------------------------------------------

    /** Le fond de chaque dimension sous son skin, dans l'ordre des dimensions. */
    static final Color[] SKIN_BACKS = {
            Color.web("#101a24"), Color.web("#15120e"), Color.web("#0b3d4a"), Color.web("#1c1a18"), Color.web("#3f6a33"),
            Color.web("#0a0d12"), Color.web("#bfe3ff"), Color.web("#c0c0c0"), Color.web("#0d0b10"), Color.BLACK};

    /** Les rails et wagons : l'acier des rails, le bois des traverses, le ciel de nuit sur la terre. */
    static final Color RAIL = Color.web("#9aa3ad"), SLEEPER = Color.web("#5a4634"), IRON = Color.web("#1e1e22");

    static final LinearGradient RAILS_SKY = vertical("#101a24", "#2a3a2a");

    /** La feuille de cahier : le papier (plus sombre au loin), les carreaux, la marge, l'encre du stylo. */
    static final Color PAPER = Color.web("#f6f1e3"), PAPER_FAR = Color.web("#b9b3a2");

    static final Color PAPER_SQUARE = Color.web("#b8cfe8"), PAPER_MARGIN = Color.web("#d9534f"), INK = Color.web("#1f3f8f"),
            NIB = Color.web("#c9c9d2");

    /** L'aquarium : l'eau du haut vers le fond, les arêtes claires, le sable, les bulles, et les robes des poissons. */
    static final LinearGradient WATER = vertical("#0b3d4a", "#126b6b");

    static final Color AQUA_EDGE = Color.web("#cfe9ee"), SAND = Color.web("#d8c08a"), BUBBLE = Color.web("#e6f7ff");

    static final Color[] FISH = {Color.web("#ff9a3c"), Color.web("#ffd23c"), Color.web("#4fc3f7"), Color.web("#ff6b6b")};

    /** Le vinyle : le disque, ses sillons, le sillon lu, l'aiguille et son bras. */
    static final Color VINYL = Color.web("#141416"), GROOVE = Color.web("#2e2e33"), GROOVE_READ = Color.web("#8a8a90"),
            TONEARM = Color.web("#d0d0d4"), CARTRIDGE = Color.web("#2a2a2e");

    /** La cible : ses anneaux du dehors au centre (blanc, noir, bleu, rouge, or), les trois parts d'une zone sur chacun, la paille. */
    static final Color[] TARGET = {
            Color.web("#f4f1e8"), Color.web("#1a1a1a"), Color.web("#2f7fd0"), Color.web("#d8362a"), Color.web("#f2c744")};

    static final Color[][] TARGET_ZONES = {
            {Color.web("#d6d2c4"), Color.web("#e6e2d6"), Color.WHITE}, {Color.web("#3a3a3a"), Color.web("#5a5a5a"), Color.web("#8a8a8a")},
            {Color.web("#5a9ae0"), Color.web("#86b6ec"), Color.web("#bcd8f6")}, {Color.web("#e4675c"), Color.web("#ee9088"), Color.web("#f8bcb6")},
            {Color.web("#f6d56a"), Color.web("#f9e39a"), Color.web("#fdf2cc")}};

    static final Color STRAW = Color.web("#c9a86a"), STRAW_EDGE = Color.web("#8a6f3a"), ARROW_WOOD = Color.web("#5a3a1a"),
            FLETCH = Color.web("#d8362a");

    static final LinearGradient GRASS = vertical("#5a8a3c", "#2f4d26");

    /** La toile : ses fils argentés, la rosée, l'araignée. */
    static final Color THREAD = Color.web("#cfd6dd"), DEW = Color.web("#e8f4ff"), SPIDER = Color.web("#2b2b30");

    /** Les fils radiaux de la toile, et le pas d'échantillonnage de chaque fil. */
    static final int WEB_RADIALS = 16, WEB_STEPS = 24;

    /** Les bulles de savon : le ciel, les teintes irisées qui tournent, l'encre bleue des textes sur le ciel, le vert des clics automatiques. */
    static final LinearGradient SKY = vertical("#bfe3ff", "#eaf6ff");

    static final Color[] BUBBLE_HUES = new Color[24];

    static final RadialGradient BUBBLE_FILL = new RadialGradient(0, 0, 0.35, 0.35, 0.65, true,
            CycleMethod.NO_CYCLE, new Stop(0, Color.color(1, 1, 1, 0.05)),
            new Stop(0.85, Color.color(1, 1, 1, 0.1)), new Stop(1, Color.color(0.62, 0.82, 1, 0.5)));

    static final Color SKY_INK = Color.web("#1f3f5f"), SKY_AUTO = Color.web("#2e7d4a");

    /** Le démineur : la case, son biseau clair et son biseau sombre, le texte, la mine, les chiffres de 1 à 8. */
    static final Color MINE_FACE = Color.web("#c0c0c0"), MINE_LIGHT = Color.WHITE, MINE_DARK = Color.web("#808080"),
            MINE_TEXT = Color.web("#1a1a1a"), MINE_GRAY = Color.web("#505050"), MINE_HIT = Color.web("#ff3b30"), FLAG = Color.web("#d8362a"),
            MINE_SUNK = Color.web("#b0b0b0"), MINE_TOP = Color.web("#e0e0e0"), MINE_SIDE = Color.web("#a0a0a0");

    static final Color[] MINE_DIGITS = {
            Color.web("#0000ff"), Color.web("#008000"), Color.web("#ff0000"), Color.web("#000080"), Color.web("#800000"),
            Color.web("#008080"), Color.web("#000000"), Color.web("#808080")};

    /** Le vitrail : le plomb, les verres éteints et les mêmes verres éclairés, la lumière qui traverse. */
    static final Color LEAD = Color.web("#1a1a1a"), LEAD_SHINE = Color.web("#8a8a90"), DAYLIGHT = Color.web("#fff2d0");

    static final Color[] GLASS_DARK = {
            Color.web("#1c2a5a"), Color.web("#5a1c22"), Color.web("#1c4a2e"), Color.web("#3a1c4e"), Color.web("#5a3a12")};

    static final Color[] GLASS_LIT = {
            Color.web("#4f7fe0"), Color.web("#e04a4a"), Color.web("#4fc96a"), Color.web("#b060e0"), Color.web("#ffb640")};

    /** Le kaléidoscope : le fond de la lunette, le contour, et les couleurs saturées des cinq copies du décor. */
    static final RadialGradient LENS = new RadialGradient(0, 0, 0.5, 0.5, 0.5, true,
            CycleMethod.NO_CYCLE, new Stop(0, Color.web("#1a1030")), new Stop(1, Color.web("#050308")));

    static final Color LENS_RIM = Color.web("#444444");

    static final Color[] KALEIDO = {
            Color.web("#ff2d55"), Color.web("#ff9f0a"), Color.web("#ffd60a"), Color.web("#30d158"), Color.web("#0a84ff")};

    static {
        for (int i = 0; i < BUBBLE_HUES.length; i++) BUBBLE_HUES[i] = Color.hsb(i * 360.0 / BUBBLE_HUES.length, 0.45, 1);
    }

    /** Un dégradé vertical du haut vers le bas, en proportion de la forme remplie : constant, il se garde en cache. */
    static LinearGradient vertical(String top, String bottom) {
        return new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web(top)), new Stop(1, Color.web(bottom)));
    }

    /** Vrai à l'image en cours quand la dimension montrée a un skin choisi ; faux pour le Classique. */
    boolean skinned;

    /** Le contour du sable de l'aquarium, rempli à chaque image sans rien allouer. */
    final double[] sandX = new double[14], sandY = new double[14];

    /** Un trait qui vient d'être touché : son éclat et les éclats qui montent. */
    static final class Touch {
        int line;
        double at, age;
    }

    /** Un éclat du plan qui vient d'être ramassé. */
    static final class Pickup {
        double x, y, age;
        String text;
    }

    static final Color SHARD = Color.web("#ffd27f");

    final Game game;

    final List<Touch> touches = new ArrayList<>();

    final List<Pickup> pickups = new ArrayList<>();

    long lastCollected = -1;

    /** La traînée du point du plan : ses dernières positions, la plus récente en tête. */
    static final int TRAIL = 90;

    final double[] trailX = new double[TRAIL], trailY = new double[TRAIL];

    int trailHead, trailCount;

    /** La route de la boule à l'image d'avant, pour voir les traits passés depuis. */
    double lastPhase = Double.NaN;

    Dimension dimension = Dimension.LINE;

    double time;

    // Le volume.
    /** Où en est la vue : dehors (le cube tourne), en train de plonger, dedans, en train de ressortir. */
    public enum Dive { OUTSIDE, ENTERING, INSIDE, LEAVING }

    /** Durées de la plongée et de la sortie, en secondes. */
    public static final double DIVE_IN_SECONDS = 0.9, DIVE_OUT_SECONDS = 0.7;

    /** Le cube du dehors : sa taille relative à l'écran, et la distance d'où on le regarde. */
    static final double OUTSIDE_SIZE = 0.22, OUTSIDE_DISTANCE = 5;

    /** La focale du dedans, relative à l'écran : un regard large, comme dans une pièce. */
    static final double INSIDE_FOCAL = 0.6;

    final VolumeCamera camera = new VolumeCamera();

    Dive dive = Dive.OUTSIDE;

    /** Avancement de la plongée ou de la sortie, de 0 à 1. */
    double diveProgress;

    /** L'orientation au début de la plongée ou de la sortie, d'où la vue part. */
    double fromYaw, fromPitch;

    /** Le regard dedans : la caméra y revient après chaque image d'animation. */
    double lookYaw, lookPitch;

    double dragX, dragY;

    boolean dragging;

    /** Quand chaque boule est apparue, pour la faire grossir en naissant. */
    final Map<Game.VolumeBall, Double> born = new IdentityHashMap<>();

    /** Les clics droits qui n'ont rien touché : un petit cercle qui s'efface. */
    final List<Pickup> misses = new ArrayList<>();

    final List<Pickup> volumePickups = new ArrayList<>();

    boolean motion = true;

    /** La durée : les éclats donnés au relâchement, qui montent du bouton. */
    final List<Pickup> durationPickups = new ArrayList<>();

    /**
     * Les boules cueillies et les relâchements de la durée que la vue a déjà montrés : ce que la partie
     * compte de plus vient de l'automatisation des dimensions. −1 tant que la vue n'a rien vu.
     */
    long lastVolumeCollected = -1, lastReleases = -1;

    /** Les tours qu'une spirale dessine au plus : au-delà, le rond serait plein ; elle en représente alors davantage. */
    public static final double DURATION_DRAWN_TURNS = 8;

    /** Le rond et le bouton, relatifs au plus petit côté de la vue. */
    static final double DURATION_RING = 0.4, DURATION_BUTTON = 0.08;

    // La caméra libre : dedans, le regard suit la souris sans clic, comme dans un jeu à la première personne.
    /** Vrai quand le joueur a choisi la caméra libre (un pixel de souris tourne le regard d'un pixel d'écran au centre). */
    boolean freeCamera;

    /** Où était la souris à son dernier mouvement sur le volume ; NaN quand elle n'y est pas. */
    double mouseX = Double.NaN, mouseY = Double.NaN;

    /** De quoi ramener la souris au centre ; {@code null} tant qu'il n'a pas servi, ou si le système le refuse. */
    Robot robot;

    boolean robotRefused;

    /** Échap a libéré la souris : le regard ne la suit plus jusqu'au prochain clic dans la vue. */
    boolean mouseReleased;

    /** La fenêtre du jeu avait-elle la main à l'image d'avant : au retour, la souris revient au centre sans tourner le regard. */
    boolean hadFocus = true;

    /** Contre un bord, en caméra libre, le regard tourne seul à cette vitesse (radians par seconde). */
    public static final double EDGE_TURN = 1.8;

    /** La part de l'écran, de chaque côté, où la souris fait tourner le regard en continu. */
    public static final double EDGE_BAND = 0.04;

    public DimensionViewBase(Game game) {
        super(800, 480);
        this.game = game;
        // Le volume se joue à la souris : ces gestes ne servent à rien dans les autres dimensions.
        setOnMousePressed(event -> {
            if (press(event.getX(), event.getY(), event.getButton() == MouseButton.SECONDARY)) event.consume();
            setCursor(cursorAt(event.getX(), event.getY()));
        });
        setOnMouseDragged(event -> {
            if (freeLook(event.getX(), event.getY()) || dragBrane(event.getX(), event.getY()) || dragMix(event.getX(), event.getY())
                    || event.isPrimaryButtonDown() && dragTo(event.getX(), event.getY())) event.consume();
        });
        setOnMouseReleased(event -> {
            // La durée : relâcher le bouton, où que soit la souris, donne les éclats.
            releaseHold();
            // La brane : relâcher la membrane, où que soit la souris, lance l'onde.
            if (gestured(releaseBrane())) event.consume();
            // Le mélange : le bouton droit relâché verse la charge ; le gauche lâche le point.
            if (gestured(releaseMix(event.getButton() == MouseButton.SECONDARY))) event.consume();
            dragging = false;
            setCursor(cursorAt(event.getX(), event.getY()));
        });
        setOnMouseMoved(event -> {
            freeLook(event.getX(), event.getY());
            hoverClicker(event.getX(), event.getY());
            hoverGrid(event.getX(), event.getY());
            setCursor(cursorAt(event.getX(), event.getY()));
        });
        setOnMouseExited(event -> {
            // La caméra libre retient la souris dans la vue : sortie trop vite, elle compte son mouvement et revient.
            if (freeInside() && windowFocused()) {
                freeLook(event.getX(), event.getY());
                recenter();
                return;
            }
            mouseX = Double.NaN;
            mouseY = Double.NaN;
            clickerMouseX = Double.NaN;
            clickerMouseY = Double.NaN;
            gridHoverI = -1;
            gridHoverJ = -1;
        });
        // La grille : la molette monte ou descend d'une couche.
        setOnScroll(event -> {
            if (dimension == Dimension.GRID && game.isGridOpen() && event.getDeltaY() != 0) shiftGridLayer(event.getDeltaY() > 0 ? 1 : -1);
            // Le mélange : la molette vers soi plonge le point, vers l'avant le fait remonter.
            if (dimension == Dimension.MIX && event.getDeltaY() != 0) shiftMixDepth(event.getDeltaY() > 0 ? -1 : 1);
        });
    }

    static double wrap(double angle) { return VolumeCamera.closest(angle, 0); }

    static double ease(double t) { return t * t * (3 - 2 * t); }

    /** Un poisson : un ovale, une queue en triangle derrière, un œil devant ; {@code right} s'il regarde à droite. */
    static void fish(GraphicsContext g, double x, double y, double r, boolean right, Color robe) {
        double d = right ? 1 : -1, wag = 0.15 * Math.sin(x * 0.05 + y * 0.03);
        g.setFill(robe);
        g.fillPolygon(new double[] {x - d * r * 0.9, x - d * r * 1.8, x - d * r * 1.8}, new double[] {y, y - r * (0.7 + wag), y + r * (0.7 - wag)}, 3);
        g.fillOval(x - r, y - r * 0.6, 2 * r, 1.2 * r);
        double e = Math.max(1.2, r * 0.2);
        g.setFill(Color.WHITE);
        g.fillOval(x + d * r * 0.45 - e, y - r * 0.2 - e, 2 * e, 2 * e);
        g.setFill(IRON);
        g.fillOval(x + d * r * 0.5 - e * 0.5, y - r * 0.2 - e * 0.5, e, e);
    }

    /** La croix de la caméra libre, au centre : c'est elle qui vise. */
    static void crosshair(GraphicsContext g, double x, double y) {
        g.setStroke(Color.WHITE);
        g.setLineWidth(1.5);
        g.setGlobalAlpha(0.85);
        g.strokeLine(x - 10, y, x - 4, y);
        g.strokeLine(x + 4, y, x + 10, y);
        g.strokeLine(x, y - 10, x, y - 4);
        g.strokeLine(x, y + 4, x, y + 10);
        g.setGlobalAlpha(1);
    }

    /** Un coin du cube, de 0 à 7 : un bit par axe. */
    static double[] corner(int k) {
        return new double[] {(k & 1) == 0 ? -1 : 1, (k & 2) == 0 ? -1 : 1, (k & 4) == 0 ? -1 : 1};
    }

    /** Un point d'une face du cube : la face {@code side} de l'axe {@code axis}, aux coordonnées (p, q) des deux autres. */
    static double[] face(int axis, int side, double p, double q) {
        return switch (axis) {
            case 0 -> new double[] {side, p, q};
            case 1 -> new double[] {p, side, q};
            default -> new double[] {p, q, side};
        };
    }

    /** Une tache d'encre : un rond et deux gouttes à côté, toujours les mêmes pour un même éclat. */
    static void inkBlot(GraphicsContext g, double x, double y, double r, int seed) {
        g.fillOval(x - r, y - r * 0.85, r * 2, r * 1.7);
        double a = UniverseView.noise(seed) * Math.PI * 2, b = a + 2.2;
        g.fillOval(x + Math.cos(a) * r * 1.2 - r * 0.35, y + Math.sin(a) * r * 1.2 - r * 0.35, r * 0.7, r * 0.7);
        g.fillOval(x + Math.cos(b) * r * 1.1 - r * 0.25, y + Math.sin(b) * r * 1.1 - r * 0.25, r * 0.5, r * 0.5);
    }

    /** Le stylo, pointe posée au point du joueur, corps vers le haut à droite. */
    static void pen(GraphicsContext g, double x, double y) {
        g.setStroke(INK);
        g.setLineWidth(5);
        g.setLineCap(StrokeLineCap.ROUND);
        g.strokeLine(x + 8, y - 10, x + 26, y - 34);
        g.setStroke(SHARD);
        g.setLineWidth(5);
        g.strokeLine(x + 24, y - 31, x + 28, y - 36);
        g.setLineCap(StrokeLineCap.SQUARE);
        g.setFill(NIB);
        g.fillPolygon(new double[] {x, x + 9, x + 5}, new double[] {y, y - 5, y - 11}, 3);
        g.setFill(INK);
        g.fillOval(x - 1.5, y - 1.5, 3, 3);
    }

    /**
     * Un point du plan (−1 à 1 dans les deux sens) sur la grille en perspective : x et y à l'écran,
     * et l'échelle à cette profondeur (1 au premier plan).
     */
    static double[] onPlane(double w, double h, double cx, double x, double y) {
        double top = h * 0.25, bottom = h * 0.9;
        double depth = (y + 1) / 2;
        double half = w * (0.18 + 0.32 * depth * depth);
        return new double[] {cx + x * half * 0.9, top + (bottom - top) * depth * depth, half / (w * 0.5)};
    }

    static void cube(GraphicsContext g, double cx, double cy, double size, double a, double b, Color c, double alpha) {
        double[][] p = new double[8][2];
        for (int k = 0; k < 8; k++) {
            double x = (k & 1) == 0 ? -1 : 1, y = (k & 2) == 0 ? -1 : 1, z = (k & 4) == 0 ? -1 : 1;
            double x1 = x * Math.cos(a) - z * Math.sin(a), z1 = x * Math.sin(a) + z * Math.cos(a);
            double y1 = y * Math.cos(b) - z1 * Math.sin(b), z2 = y * Math.sin(b) + z1 * Math.cos(b);
            double depth = 5 / (5 + z2);
            p[k][0] = cx + x1 * size * depth;
            p[k][1] = cy + y1 * size * depth;
        }
        g.setStroke(c);
        g.setLineWidth(1.5);
        g.setGlobalAlpha(0.8 * alpha);
        for (int k = 0; k < 8; k++) {
            for (int bit : new int[] {1, 2, 4}) {
                int j = k ^ bit;
                if (j > k) g.strokeLine(p[k][0], p[k][1], p[j][0], p[j][1]);
            }
        }
        g.setFill(c);
        for (double[] q : p) g.fillOval(q[0] - 2.5, q[1] - 2.5, 5, 5);
    }

    // ------------------------------------------------------------------
    // La boucle : les cercles, leurs points, leurs zones
    // ------------------------------------------------------------------

    /** Le premier cercle (dehors) et le dernier possible (dedans), relatifs au plus petit côté de la vue. */
    static final double LOOP_OUTER = 0.42, LOOP_INNER = 0.1;

    /** Les couleurs des trois parts d'une zone : le bord « mauvais », le « bien », le centre « excellent ». */
    public static final Color LOOP_BAD = Color.web("#b0566e"), LOOP_GOOD = Color.web("#ff9eb5"), LOOP_EXCELLENT = Color.web("#fff1c9");

    static final Color LOOP_MISS = Color.web("#8a93b0");

    /** Durée de vie du mot d'un clic, qui monte du point et s'efface, en secondes. */
    static final double LOOP_MARK_SECONDS = 0.9;

    /** Le mot d'un clic sur un cercle (ou le gain du rythme automatique, au centre) : où il est né, et son âge. */
    static final class LoopMark {
        double x, y, age;
        String text, gain;
        Game.LoopHit hit;
    }

    final List<LoopMark> loopMarks = new ArrayList<>();

    /** Les clics qui ont rapporté, que la vue a déjà montrés : ce que la partie compte de plus vient du rythme automatique. */
    long lastLoopHits = -1;

    /** Où était le point du premier cercle à l'image d'avant : pour entendre ses passages sur un battement. */
    double lastBeatPhase = Double.NaN;

    /** Où tombent les battements du métronome : la place de la première zone quand la fenêtre s'est ouverte. */
    double beatOffset;

    /** Le métronome : ce qu'il joue à chaque battement ; {@code null} quand il est coupé. */
    Runnable onBeat;

    /** Ce qui suit un geste du joueur dans le mini-jeu : son son, avec le numéro de la dimension. */
    IntConsumer onGesture = dimension -> { };

    long beats;

    /** L'éclat de chaque cercle juste joué, de 1 à 0, et ce que le clic a valu. */
    final double[] ringFlash = new double[Game.MAX_RINGS];

    final Game.LoopHit[] ringFlashHit = new Game.LoopHit[Game.MAX_RINGS];

    /**
     * Les clics du rythme automatique que la vue a déjà montrés, −1 tant qu'elle n'a rien vu, et
     * l'attente de chaque cercle à l'image d'avant : un cercle dont l'attente remonte vient d'être joué par lui.
     */
    long lastLoopAuto = -1;

    final double[] lastLoopWait = new double[Game.MAX_RINGS];

    long loopAutoFlashes;

    /** Le mot d'un clic : « Excellent », « Bien », « Mauvais », « Raté » ; rien pour un clic qui attendait. */
    public static String hitWord(Game.LoopHit hit) {
        return switch (hit) {
            case EXCELLENT -> "Excellent";
            case GOOD -> "Bien";
            case BAD -> "Mauvais";
            case MISS -> "Raté";
            case WAIT -> "";
        };
    }

    static Color hitColor(Game.LoopHit hit) {
        return switch (hit) {
            case EXCELLENT -> LOOP_EXCELLENT;
            case GOOD -> LOOP_GOOD;
            case BAD -> LOOP_BAD;
            case MISS, WAIT -> LOOP_MISS;
        };
    }

    /** L'anneau de la cible du cercle {@code ring} parmi {@code rings} : blanc dehors, or au centre, les cinq couleurs réparties. */
    static int targetColor(int ring, int rings) {
        return Math.min(TARGET.length - 1, ring * TARGET.length / Math.max(1, rings));
    }

    /** La flèche qui tourne : la pointe au point du cercle, le fût derrière elle le long du cercle, l'empennage au bout. */
    static void arrow(GraphicsContext g, double x, double y, double a, double length) {
        // Le point tourne dans le sens des aiguilles d'une montre : la tangente de l'angle croissant va vers l'avant.
        double tx = -Math.sin(a), ty = Math.cos(a), nx = Math.cos(a), ny = Math.sin(a);
        g.setStroke(ARROW_WOOD);
        g.setLineWidth(2.5);
        g.strokeLine(x - tx * length, y - ty * length, x, y);
        g.setStroke(FLETCH);
        g.setLineWidth(2);
        for (int side = -1; side <= 1; side += 2) {
            double bx = x - tx * length, by = y - ty * length;
            g.strokeLine(bx, by, bx - tx * 7 + nx * side * 5, by - ty * 7 + ny * side * 5);
            g.strokeLine(bx + tx * 6, by + ty * 6, bx - tx * 1 + nx * side * 5, by - ty * 1 + ny * side * 5);
        }
        g.setFill(Color.WHITE);
        g.fillPolygon(new double[] {x + tx * 8, x - nx * 4, x + nx * 4}, new double[] {y + ty * 8, y - ny * 4, y + ny * 4}, 3);
    }

    /** Une flèche plantée dans la cible, vue de face : le bout du fût et son empennage en trois plumes, qui s'efface. */
    static void plantedArrow(GraphicsContext g, double x, double y, double alpha) {
        g.setGlobalAlpha(Math.max(0, alpha));
        g.setStroke(FLETCH);
        g.setLineWidth(2);
        for (int k = 0; k < 3; k++) {
            double a = -Math.PI / 2 + k * Math.PI * 2 / 3;
            g.strokeLine(x, y, x + Math.cos(a) * 8, y + Math.sin(a) * 8);
        }
        g.setFill(ARROW_WOOD);
        g.fillOval(x - 3, y - 3, 6, 6);
    }

    /** Un arc de zone : de {@code center − half} à {@code center + half} tours, dans le sens du point. */
    static void zoneArc(GraphicsContext g, double cx, double cy, double r, double center, double half, double band,
            Color color, double alpha) {
        // Le point part du haut et tourne dans le sens des aiguilles d'une montre ; l'arc de JavaFX compte à l'envers.
        double start = 90 - (center - half) * 360, extent = -half * 2 * 360;
        g.setStroke(color);
        g.setLineWidth(band);
        g.setGlobalAlpha(alpha);
        g.setLineCap(StrokeLineCap.BUTT);
        g.strokeArc(cx - r, cy - r, r * 2, r * 2, start, extent, ArcType.OPEN);
    }

    // ------------------------------------------------------------------
    // La brane : la membrane, ses ondes, leurs rebonds, ses points
    // ------------------------------------------------------------------

    /** La demi-largeur de la membrane, relative au plus petit côté de la vue. */
    static final double BRANE_HALF = 0.42;

    /** Le glissé, en unités de la membrane (de côté 2), qui la tire à fond. */
    public static final double BRANE_FULL_PULL = 0.6;

    /** Le quadrillage de la membrane : tant de cases sur chaque axe. */
    static final int BRANE_GRID = 12;

    /** Un point touché par l'onde qui court. */
    public static final Color BRANE_STRUCK = Color.web("#fff1c9");

    /**
     * La couleur d'un front selon ses rebonds : blanc en direct, orangé après un bord, puis rose, bleu,
     * vert pour quatre rebonds et plus. Les gains des passages prennent la même.
     */
    static final Color[] BRANE_BOUNCE_COLORS = {
            Color.WHITE, Color.web("#ffb36b"), Color.web("#ff7ad9"), Color.web("#a8b8ff"), Color.web("#9be7a8")};

    /** Durée de vie du gain d'un passage, qui monte du point et s'efface, en secondes ; et de l'éclat d'un bord. */
    static final double BRANE_MARK_SECONDS = 1.1, BRANE_FLASH_SECONDS = 0.6;

    /** La couleur d'un front ou d'un passage après {@code bounces} rebonds. */
    public static Color bounceColor(int bounces) {
        return BRANE_BOUNCE_COLORS[Math.max(0, Math.min(BRANE_BOUNCE_COLORS.length - 1, bounces))];
    }

    /** Le gain d'un passage, ou l'éclat d'un bord que le front touche : où, son âge, et ses rebonds. */
    static final class BraneMark {
        double x, y, age;
        String text;
        int bounces;
        /** Pour l'éclat d'un bord : le bord est vertical (gauche ou droit), horizontal, ou c'est un coin. */
        boolean vertical, horizontal;
    }

    /** La membrane tenue : où on l'a appuyée et où est la souris, en unités de la membrane (−1 à 1). */
    boolean braneHeld;

    double braneFromX, braneFromY, braneToX, braneToY;

    final List<BraneMark> braneMarks = new ArrayList<>();

    final List<BraneMark> braneFlashes = new ArrayList<>();

    long braneEdgeFlashes;

    /**
     * Ce que la vue a vu de chaque onde à l'image d'avant : son rayon et ses passages. Entre deux images, chaque front
     * (l'onde et ses images) passé sur un point est un passage : son gain monte de ce point.
     */
    final Map<Game.BraneWave, double[]> braneSeen = new IdentityHashMap<>();

    /** Les points de l'image d'avant : une onde qui s'éteint emporte ceux qu'elle a touchés. */
    List<Game.BranePoint> braneSeenPoints = List.of();

    /** Les passages sur chaque point encore sur la membrane : il les affiche jusqu'à partir avec l'onde. */
    final Map<Game.BranePoint, Integer> branePointPasses = new HashMap<>();

    /** Les passages que la vue a déjà montrés, −1 tant qu'elle n'a rien vu. */
    long lastBraneTouches = -1;

    /** Les fronts d'onde dessinés à la dernière image, rebonds compris. */
    int braneFronts;

    double lastBranePull;

    /** « +3 », « +4.5, rebond », « +9, 2 rebonds » : le gain d'un passage et ses rebonds. */
    public static String passText(double gain, int bounces) {
        return "+" + Format.darkMatter(BigNum.of(gain))
                + (bounces <= 0 ? "" : bounces == 1 ? ", rebond" : ", " + bounces + " rebonds");
    }

    /** Vrai si le cercle de centre ({@code x}, {@code y}) et de rayon {@code r} passe dans le carré de la membrane. */
    static boolean reachesSquare(double x, double y, double r, double cx, double cy, double half) {
        double dx = Math.max(0, Math.abs(x - cx) - half), dy = Math.max(0, Math.abs(y - cy) - half);
        return Math.hypot(dx, dy) < r;
    }

    /** L'angle du fil radial {@code i} : réparti autour du centre, un peu décalé par le bruit, comme un fil tissé à la main. */
    static double webAngle(int i) {
        return Math.PI * 2 * i / WEB_RADIALS + (UniverseView.noise(i + 700) - 0.5) * 0.12;
    }

    // ------------------------------------------------------------------
    // Le clicker : la surface, ses clics, ses zones qui se superposent
    // ------------------------------------------------------------------

    /** La demi-largeur de la surface, relative au plus petit côté de la vue. */
    static final double CLICKER_HALF = 0.4;

    /** Les cases où se colore la superposition des zones : tant sur chaque axe (des cases de quatre pixels environ). */
    static final int CLICKER_CELLS = 96;

    /** La couleur d'un endroit couvert par 1 à 8 zones : du doré au blanc, en passant par l'orange, le rose et le bleu. */
    static final Color[] CLICKER_STACK_COLORS = {
            Color.web("#ffd27f"), Color.web("#ffb36b"), Color.web("#ff7ad9"), Color.web("#c9a6ff"),
            Color.web("#a8b8ff"), Color.web("#7fe0d4"), Color.web("#9be7a8"), Color.web("#fff1c9")};

    /** Un clic automatique : son éclat est vert, comme la mention « automatique ». */
    static final Color CLICKER_AUTO = Color.web("#9be7a8");

    /** Durée de vie du gain d'un clic et de son onde, en secondes. */
    static final double CLICKER_MARK_SECONDS = 0.8;

    /** La couleur d'un endroit que couvrent {@code stack} zones (1 à 8, au-delà la dernière). */
    public static Color stackColor(int stack) {
        return CLICKER_STACK_COLORS[Math.max(0, Math.min(CLICKER_STACK_COLORS.length, stack) - 1)];
    }

    /** Le gain d'un clic : où il monte, son âge, et s'il vient des clics automatiques. */
    static final class ClickerMark {
        double x, y, age;
        String text;
        boolean auto;
        int stack;
    }

    final List<ClickerMark> clickerMarks = new ArrayList<>();

    /** La souris sur la surface, en unités de la surface (0 à 1) ; NaN quand elle n'y est pas. */
    double clickerMouseX = Double.NaN, clickerMouseY = Double.NaN;

    /** Les clics automatiques que la vue a déjà montrés, −1 tant qu'elle n'a rien vu. */
    long lastClickerAuto = -1;

    long clickerAutoFlashes;

    int lastClickerStack;

    // ------------------------------------------------------------------
    // La grille : la carte de la couche, la case visée, le cube vu de biais
    // ------------------------------------------------------------------

    /** Une case essayée, au plus loin (bleu), à mi-chemin (jaune), tout près (rouge) ; trouvée, dorée ; sans l'affichage, neutre. */
    static final Color GRID_COLD = Color.web("#5a8cff"), GRID_MILD = Color.web("#ffd27f"), GRID_HOT = Color.web("#ff5a4a"),
            GRID_FOUND = Color.web("#fff1c9"), GRID_PROBED = Color.web("#59607a");

    /** La couche ou la colonne du point visé, quand l'affichage la montre. */
    static final Color GRID_HINT = Color.web("#ff7ad9");

    /** Durée de vie du gain d'un essai, qui monte de la case, en secondes. */
    static final double GRID_MARK_SECONDS = 1.2;

    /** Jusqu'à tant de cases par côté, la carte les montre toutes, et un clic essaie la case. */
    public static final int GRID_CLICK_CELLS = 24;

    /** Chaque cran de zoom montre quatre fois moins de cases par côté. */
    static final int GRID_ZOOM = 4;

    /** Le gain d'un essai : sa case, son âge, son texte, s'il a trouvé le point et s'il vient de l'automatisme. */
    static final class GridMark {
        int i, j, k;
        double age;
        String text;
        boolean found, auto;
    }

    /** La case visée : sa colonne x et sa ligne y dans la couche montrée z. */
    int gridSelX, gridSelY, gridLayer;

    /** Le zoom de la carte : 0 montre toute la couche, chaque cran quatre fois moins de cases par côté. */
    int gridZoom;

    /** Les cases déjà essayées, et ce qu'elles ont donné ; oubliées quand la grille change de taille. */
    final Map<Long, Game.GridProbe> gridProbed = new HashMap<>();

    int gridProbedDivisions = -1;

    final List<GridMark> gridMarks = new ArrayList<>();

    /** Les essais que la vue a déjà montrés, −1 tant qu'elle n'a rien vu : ceux de plus viennent de l'automatisme. */
    long lastGridProbes = -1;

    long gridAutoFlashes;

    /** La case sous la souris dans la couche montrée, ou −1. */
    int gridHoverI = -1, gridHoverJ = -1;

    static long gridKey(int i, int j, int k) { return ((long) i * 4096 + j) * 4096 + k; }

    /**
     * Un biseau de démineur sur le carré de côté {@code size} en ({@code x}, {@code y}) : clair en haut et à gauche, sombre
     * en bas et à droite ; {@code raised} pour une case en relief, sinon un cadre creusé (les côtés s'inversent).
     */
    static void bevel(GraphicsContext g, double x, double y, double size, boolean raised) {
        double width = raised ? Math.max(1, Math.min(3, size * 0.08)) : 3, inset = width / 2;
        g.setGlobalAlpha(1);
        g.setLineWidth(width);
        g.setStroke(raised ? MINE_LIGHT : MINE_DARK);
        g.strokeLine(x + inset, y + inset, x + size - inset, y + inset);
        g.strokeLine(x + inset, y + inset, x + inset, y + size - inset);
        g.setStroke(raised ? MINE_DARK : MINE_LIGHT);
        g.strokeLine(x + size - inset, y + inset, x + size - inset, y + size - inset);
        g.strokeLine(x + inset, y + size - inset, x + size - inset, y + size - inset);
    }

    /** « Couche z = 3 (de 0 à 999) » : la couche montrée, et toutes celles de la grille. */
    public static String layerText(int layer, int cells) {
        return "Couche z = " + layer + " (de 0 à " + (cells - 1) + ")";
    }

    /** Un point du cube (0 à 1 sur chaque axe, z vers le haut) vu de biais, autour de ({@code ox}, {@code oy}). */
    static double[] iso(double ox, double oy, double s, double x, double y, double z) {
        return new double[] {ox + (x - y) * s * 0.87, oy + (x + y - 1) * s * 0.5 - (z - 0.5) * s};
    }

    /**
     * Les trois faces visibles du cube de biais en démineur : le dessus clair, la face de gauche et celle de droite plus
     * sombres, chacune découpée en cases biseautées (dix par côté au plus, pour qu'elles se voient).
     */
    static void mineFaces(GraphicsContext g, double ox, double oy, double s, int n) {
        int cells = Math.min(n, 10);
        g.setGlobalAlpha(1);
        for (int face = 0; face < 3; face++) {
            g.setFill(face == 0 ? MINE_TOP : face == 1 ? MINE_FACE : MINE_SIDE);
            double[] a = faceCorner(ox, oy, s, face, 0, 0), b = faceCorner(ox, oy, s, face, 1, 0), d = faceCorner(ox, oy, s, face, 1, 1),
                    e = faceCorner(ox, oy, s, face, 0, 1);
            g.fillPolygon(new double[] {a[0], b[0], d[0], e[0]}, new double[] {a[1], b[1], d[1], e[1]}, 4);
            g.setLineWidth(1);
            for (int k = 1; k < cells; k++) {
                double f = (double) k / cells;
                double[] p = faceCorner(ox, oy, s, face, f, 0), q = faceCorner(ox, oy, s, face, f, 1);
                double[] r = faceCorner(ox, oy, s, face, 0, f), t = faceCorner(ox, oy, s, face, 1, f);
                g.setStroke(MINE_LIGHT);
                g.setGlobalAlpha(0.7);
                g.strokeLine(p[0], p[1], q[0], q[1]);
                g.setStroke(MINE_DARK);
                g.strokeLine(r[0], r[1], t[0], t[1]);
            }
            g.setGlobalAlpha(1);
        }
    }

    /** Un point ({@code p}, {@code q}) de 0 à 1 d'une face visible du cube de biais : le dessus (0), la face de gauche (1), celle de droite (2). */
    static double[] faceCorner(double ox, double oy, double s, int face, double p, double q) {
        return switch (face) {
            case 0 -> iso(ox, oy, s, p, q, 1);
            case 1 -> iso(ox, oy, s, p, 1, q);
            default -> iso(ox, oy, s, 1, p, q);
        };
    }

    /** Une couche du cube de biais, à la hauteur {@code z} : une plaque pleine, ou son seul contour. */
    static void slab(GraphicsContext g, double ox, double oy, double s, double z, Color color, double alpha, boolean outline) {
        double[] a = iso(ox, oy, s, 0, 0, z), b = iso(ox, oy, s, 1, 0, z), d = iso(ox, oy, s, 1, 1, z), e = iso(ox, oy, s, 0, 1, z);
        double[] xs = {a[0], b[0], d[0], e[0], a[0]}, ys = {a[1], b[1], d[1], e[1], a[1]};
        g.setGlobalAlpha(alpha);
        if (outline) {
            g.setStroke(color);
            g.setLineWidth(2);
            g.strokePolyline(xs, ys, 5);
        } else {
            g.setFill(color);
            g.fillPolygon(xs, ys, 4);
        }
    }

    // ------------------------------------------------------------------
    // Le miroir : le plateau, le motif qui s'allume, sa reproduction en miroir
    // ------------------------------------------------------------------

    /** La case du motif qui s'allume, une case reproduite, un clic juste, une erreur. */
    static final Color MIRROR_LIT = Color.web("#fff1c9"), MIRROR_DONE = Color.web("#9be7a8"), MIRROR_WRONG = Color.web("#ff5a4a");

    /** Durée de l'éclat d'un clic, en secondes. */
    static final double MIRROR_FLASH_SECONDS = 0.5;

    /** L'éclat d'un clic : sa case, son âge, s'il était juste, et s'il vient de l'automatisme. */
    static final class MirrorFlash {
        int column, row;
        double age;
        boolean right, auto;
    }

    final List<MirrorFlash> mirrorFlashes = new ArrayList<>();

    /** Ce que la partie avait compté à l'image d'avant (cases reproduites, erreurs, motifs) : ce qui a bougé sans clic vient de l'automatisme. */
    long lastMirrorCount = -1;

    int lastMirrorMistakes;

    long mirrorAutoFlashes;

    /** Les colonnes du plateau à l'image d'avant : un plateau agrandi efface les éclats, dont les cases ont bougé. */
    int mirrorShownColumns = -1;

    /** L'éclat de l'agrandissement du plateau : son âge, −1 sans éclat ; et les agrandissements vus depuis l'ouverture. */
    double mirrorGrowAge = -1;

    long mirrorGrowths;

    static final double MIRROR_GROW_SECONDS = 1.5;

    /** Ce que la dernière image a montré du motif : ses cases allumées à gauche, les cases reproduites à droite. */
    int mirrorPatternDrawn, mirrorReproducedDrawn;

    /** Le verre de la case ({@code column}, {@code row}) : l'un des cinq, toujours le même pour une même case. */
    static int glassIndex(int column, int row) {
        return (int) (UniverseView.noise(column * 131 + row * 17 + 5) * GLASS_DARK.length);
    }

    /** Une pièce de verre du vitrail : sa couleur, un reflet en biais en haut à gauche, et son plomb épais autour. */
    static void glass(GraphicsContext g, double x, double y, double cell, Color tint) {
        g.setGlobalAlpha(1);
        g.setFill(tint);
        g.fillRect(x, y, cell, cell);
        g.setGlobalAlpha(0.1);
        g.setFill(Color.WHITE);
        g.fillPolygon(new double[] {x, x + cell * 0.7, x}, new double[] {y, y, y + cell * 0.7}, 3);
        g.setGlobalAlpha(1);
        g.setStroke(LEAD);
        g.setLineWidth(2);
        g.strokeRect(x + 1, y + 1, cell - 2, cell - 2);
    }

    // ------------------------------------------------------------------
    // Le mélange : une arène où se mêlent, étape par étape, les mécaniques des neuf autres
    // ------------------------------------------------------------------

    /** Un gain qui monte de l'arène (relâchement, trésor), ou l'éclat d'un éclat ramassé : où (dans l'arène), son âge. */
    static final class MixMark {
        double x, y, age;
        String text;
        Color tint;
    }

    /** Durée de vie d'un gain qui monte, et de l'éclat d'un éclat ramassé, en secondes. */
    static final double MIX_MARK_SECONDS = 1.3, MIX_FLASH_SECONDS = 0.5;

    /** Les cases de la carte des zones ×2 : chaque case prend la couleur du nombre de zones qui la couvrent. */
    static final int MIX_ZONE_CELLS = 50;

    /** Ce que la molette, ou une touche de profondeur, fait plonger ou remonter le point. */
    public static final double MIX_DEPTH_STEP = 0.1;

    static final Color MIX_HOT = Color.web("#ff5a3c"), MIX_COLD = Color.web("#4fa3ff");

    static final Color MIX_SHALLOW = Color.web("#fff1c9"), MIX_DEEP = Color.web("#5a4fd8");

    final List<MixMark> mixMarks = new ArrayList<>();

    final List<MixMark> mixFlashes = new ArrayList<>();

    /** Les éclats de l'image d'avant : ceux qui ont disparu depuis ont été ramassés, et s'éclairent. */
    final Map<Long, Game.MixShard> mixSeen = new HashMap<>();

    long lastMixReleases = -1, lastMixTreasures = -1;

    int lastMixStage = -1;

    /** Le bouton droit tient la charge ; le gauche, tenu, mène le point. */
    boolean mixHeld, mixSteering;

    /** La profondeur visée par la molette et les touches ; NaN tant qu'elles n'ont pas servi. */
    double mixDepthAim = Double.NaN;

    /** Le mot d'un rythme, sous le cercle et dans le gain d'un relâchement. */
    public static String rhythmWord(Game.MixRhythm rhythm) {
        return switch (rhythm) {
            case EXCELLENT -> "Excellent ! ×3";
            case GOOD -> "Bien ×1.5";
            case BAD -> "Mauvais ×1";
            case MISS -> "Raté ×0.5";
            case NONE -> "";
        };
    }

    static MixMark mixMark(double x, double y, String text, Color tint) {
        MixMark mark = new MixMark();
        mark.x = x;
        mark.y = y;
        mark.text = text;
        mark.tint = tint;
        return mark;
    }

    /** La couleur d'une profondeur : claire en surface, violette au fond. */
    static Color shardColor(double depth) {
        return MIX_SHALLOW.interpolate(MIX_DEEP, Math.max(0, Math.min(1, depth)));
    }
}
