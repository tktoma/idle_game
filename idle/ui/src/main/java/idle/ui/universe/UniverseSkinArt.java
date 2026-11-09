package idle.ui.universe;

import idle.core.universe.Skin;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;

/**
 * Les skins des univers en bulles (version 0.196), dessinés à côté de {@link UniverseView} pour la garder lisible : trois
 * visuels.
 * <ul>
 * <li>Les bulles : « Bulle de savon » (une sphère presque transparente, irisée d'arcs-en-ciel qui tournent, deux reflets),
 * « Ballon noué » (un ballon de baudruche d'une couleur franche, son nœud et sa ficelle), « Boule à neige » (un globe de
 * verre sur un socle de bois, un sapin dedans et la neige qui tombe), « Globe d'orrery » (un globe de verre sur son pied de
 * laiton, un méridien, des planètes qui tournent sur leurs anneaux), « Planète-fruit » (une orange grenue, ses quartiers,
 * sa feuille), « Œuf » (une coquille pâle et tachetée, pointe en haut) ;</li>
 * <li>la naissance : « Éclosion » (un œuf qui se fend, dont les deux moitiés s'écartent sur la lumière), « Bourgeon »
 * (une tige, puis un bourgeon dont les pétales s'ouvrent), « Feu d'artifice » (une fusée qui monte, puis une gerbe
 * d'étincelles qui retombent), « Page tournée » (une page blanche qui se tourne de droite à gauche et découvre le ciel) ;
 * </li>
 * <li>les liens : « Fil » (un fil beige qui pend un peu, piqué de points de couture), « Lierre » (une tige verte et ses
 * feuilles en cœur, de part et d'autre), « Circuit » (une piste de cuivre à angle droit, ses pastilles, une impulsion qui
 * la parcourt), « Constellation » (un trait pointillé, une étoile à quatre branches à chaque bout et au milieu).</li>
 * </ul>
 */
final class UniverseSkinArt {

    private static final Color GLASS = Color.web("#dff4ff");
    private static final Color BRASS = Color.web("#d2aa55");
    private static final Color BRASS_DARK = Color.web("#8a6a2a");
    private static final Color WOOD = Color.web("#6b4a2a");
    private static final Color WOOD_LIGHT = Color.web("#9a7046");
    private static final Color ORANGE = Color.web("#ff9a2e");
    private static final Color ORANGE_DEEP = Color.web("#c85f10");
    private static final Color LEAF = Color.web("#5fbf5a");
    private static final Color SHELL = Color.web("#f3e8d2");
    private static final Color SPECKLE = Color.web("#a88a62");
    private static final Color THREAD = Color.web("#e6d2a8");
    private static final Color IVY = Color.web("#4f9b4a");
    private static final Color IVY_LIGHT = Color.web("#8fd38a");
    private static final Color COPPER = Color.web("#d98a3a");
    private static final Color COPPER_LIGHT = Color.web("#ffd9a0");
    private static final Color STAR = Color.web("#fff6d2");
    private static final Color PAGE = Color.web("#f6f1e4");
    private static final Color PAGE_SHADE = Color.web("#b8ad98");
    private static final Color[] BALLOONS = {Color.web("#ff5a6e"), Color.web("#4a9bff"), Color.web("#ffd23f"), Color.web("#5fd36a"),
        Color.web("#c47aff"), Color.web("#ff9a3a")};
    private static final Color[] FIREWORK = {Color.web("#ffd23f"), Color.web("#ff6b8a"), Color.web("#6bd4ff"), Color.web("#9fff8a")};

    private UniverseSkinArt() {
    }

    // ------------------------------------------------------------------
    // Les bulles
    // ------------------------------------------------------------------

    /** Une bulle d'univers habillée du skin, centrée en ({@code x}, {@code y}), de rayon {@code r}, allumée à {@code lit}. */
    static void bubble(GraphicsContext g, double x, double y, double r, int seed, double lit, double time, Skin skin) {
        g.setGlobalAlpha(lit);
        switch (skin) {
            case BUBBLE_SOAP -> soap(g, x, y, r, seed, lit, time);
            case BUBBLE_BALLOON -> balloon(g, x, y, r, seed, lit, time);
            case BUBBLE_SNOWGLOBE -> snowGlobe(g, x, y, r, seed, lit, time);
            case BUBBLE_ORRERY -> orrery(g, x, y, r, seed, lit, time);
            case BUBBLE_FRUIT -> fruit(g, x, y, r, seed, lit);
            default -> egg(g, x, y, r, seed, lit);
        }
        g.setGlobalAlpha(1);
    }

    private static void soap(GraphicsContext g, double x, double y, double r, int seed, double lit, double time) {
        // Une pellicule presque invisible : un voile très pâle, et l'irisation sur le bord, en arcs de toutes les teintes.
        g.setFill(new RadialGradient(0, 0, x, y, r, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.color(1, 1, 1, 0.02)), new Stop(0.85, Color.color(1, 1, 1, 0.05)), new Stop(1, Color.color(1, 1, 1, 0.22))));
        g.setGlobalAlpha(lit);
        g.fillOval(x - r, y - r, 2 * r, 2 * r);
        int bands = 12;
        g.setLineCap(StrokeLineCap.BUTT);
        for (int k = 0; k < bands; k++) {
            double hue = (k * 360.0 / bands + time * 25 + seed * 40) % 360;
            double start = k * 360.0 / bands + time * 12 * (seed % 2 == 0 ? 1 : -1);
            g.setStroke(Color.hsb(hue, 0.55, 1));
            g.setLineWidth(Math.max(1.5, r * 0.07));
            g.setGlobalAlpha(lit * 0.5);
            g.strokeArc(x - r * 0.95, y - r * 0.95, r * 1.9, r * 1.9, start, 360.0 / bands + 2, ArcType.OPEN);
            g.setLineWidth(Math.max(1, r * 0.03));
            g.setGlobalAlpha(lit * 0.35);
            g.strokeArc(x - r * 0.8, y - r * 0.8, r * 1.6, r * 1.6, -start * 0.6 + 90, 360.0 / bands + 2, ArcType.OPEN);
        }
        // Les deux reflets : une grande virgule en haut à gauche, un point en bas à droite.
        g.setStroke(Color.WHITE);
        g.setLineCap(StrokeLineCap.ROUND);
        g.setLineWidth(Math.max(1.5, r * 0.06));
        g.setGlobalAlpha(lit * 0.85);
        g.strokeArc(x - r * 0.78, y - r * 0.78, r * 1.56, r * 1.56, 110, 45, ArcType.OPEN);
        g.setFill(Color.WHITE);
        g.setGlobalAlpha(lit * 0.6);
        double dot = r * 0.08;
        g.fillOval(x + r * 0.5 - dot, y + r * 0.55 - dot, 2 * dot, 2 * dot);
        g.setStroke(GLASS);
        g.setLineWidth(1);
        g.setGlobalAlpha(lit * 0.4);
        g.strokeOval(x - r, y - r, 2 * r, 2 * r);
    }

    private static void balloon(GraphicsContext g, double x, double y, double r, int seed, double lit, double time) {
        Color color = BALLOONS[seed % BALLOONS.length];
        double sway = Math.sin(time * 0.8 + seed) * r * 0.04;
        // La ficelle, qui ondule sous le nœud.
        g.setStroke(Color.web("#e8e8f0"));
        g.setLineWidth(1);
        g.setGlobalAlpha(lit * 0.7);
        g.beginPath();
        g.moveTo(x + sway, y + r * 1.08);
        g.bezierCurveTo(x + sway - r * 0.15, y + r * 1.3, x + sway + r * 0.2, y + r * 1.45, x + sway - r * 0.05, y + r * 1.7);
        g.stroke();
        // Le ballon, un peu plus haut que large, et son nœud.
        g.setGlobalAlpha(lit);
        g.setFill(new RadialGradient(0, 0, x - r * 0.3, y - r * 0.4, r * 1.4, false, CycleMethod.NO_CYCLE,
                new Stop(0, color.interpolate(Color.WHITE, 0.45)), new Stop(0.6, color), new Stop(1, color.darker().darker())));
        g.fillOval(x - r * 0.88, y - r, r * 1.76, r * 2.02);
        g.setFill(color.darker());
        g.fillPolygon(new double[] {x + sway - r * 0.1, x + sway + r * 0.1, x + sway + r * 0.16, x + sway - r * 0.16},
                new double[] {y + r * 0.97, y + r * 0.97, y + r * 1.12, y + r * 1.12}, 4);
        // Le reflet allongé, en haut à gauche.
        g.setFill(Color.WHITE);
        g.setGlobalAlpha(lit * 0.55);
        g.save();
        g.translate(x - r * 0.4, y - r * 0.5);
        g.rotate(-35);
        g.fillOval(-r * 0.1, -r * 0.3, r * 0.2, r * 0.6);
        g.restore();
    }

    private static void snowGlobe(GraphicsContext g, double x, double y, double r, int seed, double lit, double time) {
        // Le socle de bois, sous le globe.
        g.setGlobalAlpha(lit);
        g.setFill(WOOD);
        g.fillRoundRect(x - r * 0.75, y + r * 0.72, r * 1.5, r * 0.42, r * 0.12, r * 0.12);
        g.setFill(WOOD_LIGHT);
        g.fillRect(x - r * 0.75, y + r * 0.72, r * 1.5, r * 0.08);
        // Le globe, un verre bleuté, et la neige au fond.
        g.setFill(new RadialGradient(0, 0, x - r * 0.3, y - r * 0.3, r * 1.3, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#2a3a60")), new Stop(1, Color.web("#0c1430"))));
        g.fillOval(x - r, y - r, 2 * r, 2 * r);
        g.setFill(Color.web("#eef4ff"));
        g.setGlobalAlpha(lit * 0.9);
        g.fillArc(x - r * 0.95, y - r * 0.95, r * 1.9, r * 1.9, 200, 140, ArcType.CHORD);
        // Un sapin, posé sur la neige.
        g.setGlobalAlpha(lit);
        g.setFill(Color.web("#2f7a3a"));
        for (int layer = 0; layer < 3; layer++) {
            double top = y + r * (0.05 - 0.28 * (2 - layer)), width = r * (0.22 + 0.14 * layer);
            g.fillPolygon(new double[] {x, x - width, x + width}, new double[] {top - r * 0.28, top + r * 0.05, top + r * 0.05}, 3);
        }
        g.setFill(WOOD);
        g.fillRect(x - r * 0.05, y + r * 0.1, r * 0.1, r * 0.2);
        // La neige qui tombe, toujours la même, qui revient en haut.
        g.setFill(Color.WHITE);
        for (int k = 0; k < 26; k++) {
            double fall = (time * (0.08 + 0.06 * UniverseView.noise(seed * 31 + k)) + UniverseView.noise(seed * 31 + k + 50)) % 1;
            double a = UniverseView.noise(seed * 31 + k + 100) * Math.PI * 2;
            double d = r * 0.85 * Math.sqrt(UniverseView.noise(seed * 31 + k + 150));
            double sx = x + Math.cos(a) * d, sy = y - r * 0.9 + fall * r * 1.8;
            if (Math.hypot(sx - x, sy - y) > r * 0.9) continue;
            double dot = Math.max(0.8, r * 0.025);
            g.setGlobalAlpha(lit * (0.5 + 0.5 * UniverseView.noise(seed * 31 + k + 200)));
            g.fillOval(sx - dot, sy - dot, 2 * dot, 2 * dot);
        }
        // Le verre : son bord et un reflet.
        g.setStroke(GLASS);
        g.setLineWidth(Math.max(1.2, r * 0.04));
        g.setGlobalAlpha(lit * 0.7);
        g.strokeOval(x - r, y - r, 2 * r, 2 * r);
        g.setGlobalAlpha(lit * 0.5);
        g.strokeArc(x - r * 0.8, y - r * 0.8, r * 1.6, r * 1.6, 115, 40, ArcType.OPEN);
    }

    private static void orrery(GraphicsContext g, double x, double y, double r, int seed, double lit, double time) {
        // Le pied de laiton : une tige et une base.
        g.setGlobalAlpha(lit);
        g.setFill(BRASS_DARK);
        g.fillRect(x - r * 0.06, y + r * 0.95, r * 0.12, r * 0.35);
        g.setFill(BRASS);
        g.fillOval(x - r * 0.5, y + r * 1.22, r, r * 0.18);
        // Le globe de verre, à peine teinté.
        g.setFill(new RadialGradient(0, 0, x - r * 0.3, y - r * 0.3, r * 1.3, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.color(0.6, 0.75, 1, 0.18)), new Stop(1, Color.color(0.2, 0.3, 0.6, 0.35))));
        g.fillOval(x - r, y - r, 2 * r, 2 * r);
        // Le soleil au centre, et les planètes sur leurs anneaux inclinés.
        UniverseView.glow(g, x, y, r * 0.3, Color.web("#ffd27f"), 0.9 * lit);
        g.setGlobalAlpha(lit);
        g.setFill(Color.web("#ffe6a0"));
        double sun = r * 0.1;
        g.fillOval(x - sun, y - sun, 2 * sun, 2 * sun);
        int planets = 3 + seed % 2;
        for (int k = 0; k < planets; k++) {
            double ring = r * (0.3 + 0.2 * k), tilt = 0.35 + 0.1 * k;
            g.setStroke(BRASS);
            g.setLineWidth(1);
            g.setGlobalAlpha(lit * 0.6);
            g.strokeOval(x - ring, y - ring * tilt, 2 * ring, 2 * ring * tilt);
            double a = time * (0.9 - 0.2 * k) + k * 2 + seed;
            double px = x + Math.cos(a) * ring, py = y + Math.sin(a) * ring * tilt;
            double size = r * (0.045 + 0.02 * k);
            g.setGlobalAlpha(lit);
            g.setFill(BALLOONS[(seed + k) % BALLOONS.length]);
            g.fillOval(px - size, py - size, 2 * size, 2 * size);
        }
        // Le méridien de laiton, et le bord du verre.
        g.setStroke(BRASS);
        g.setLineWidth(Math.max(1.5, r * 0.06));
        g.setGlobalAlpha(lit * 0.9);
        g.strokeOval(x - r * 0.3, y - r, r * 0.6, 2 * r);
        g.setStroke(GLASS);
        g.setLineWidth(Math.max(1, r * 0.03));
        g.setGlobalAlpha(lit * 0.6);
        g.strokeOval(x - r, y - r, 2 * r, 2 * r);
        g.setGlobalAlpha(lit * 0.45);
        g.strokeArc(x - r * 0.82, y - r * 0.82, r * 1.64, r * 1.64, 120, 35, ArcType.OPEN);
    }

    private static void fruit(GraphicsContext g, double x, double y, double r, int seed, double lit) {
        g.setGlobalAlpha(lit);
        g.setFill(new RadialGradient(0, 0, x - r * 0.3, y - r * 0.35, r * 1.35, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#ffc06a")), new Stop(0.65, ORANGE), new Stop(1, ORANGE_DEEP)));
        g.fillOval(x - r, y - r, 2 * r, 2 * r);
        // La peau grenue : des pores, plus serrés au bord.
        g.setFill(ORANGE_DEEP);
        for (int k = 0; k < 70; k++) {
            double a = UniverseView.noise(seed * 17 + k) * Math.PI * 2, d = r * 0.95 * Math.sqrt(UniverseView.noise(seed * 17 + k + 300));
            double dot = Math.max(0.6, r * 0.018);
            g.setGlobalAlpha(lit * 0.35);
            g.fillOval(x + Math.cos(a) * d - dot, y + Math.sin(a) * d - dot, 2 * dot, 2 * dot);
        }
        // Les quartiers, à peine marqués, comme des méridiens.
        g.setStroke(ORANGE_DEEP);
        g.setLineWidth(1);
        g.setGlobalAlpha(lit * 0.3);
        for (int k = 0; k < 3; k++) {
            double squeeze = 0.25 + 0.3 * k;
            g.strokeOval(x - r * squeeze, y - r * 0.98, r * squeeze * 2, r * 1.96);
        }
        // La feuille et son pédoncule, en haut.
        g.setGlobalAlpha(lit);
        g.setFill(Color.web("#7a4a1a"));
        g.fillOval(x - r * 0.08, y - r * 1.05, r * 0.16, r * 0.14);
        g.setFill(LEAF);
        g.beginPath();
        g.moveTo(x + r * 0.05, y - r * 1.0);
        g.bezierCurveTo(x + r * 0.2, y - r * 1.4, x + r * 0.6, y - r * 1.35, x + r * 0.55, y - r * 1.1);
        g.bezierCurveTo(x + r * 0.4, y - r * 0.95, x + r * 0.2, y - r * 0.95, x + r * 0.05, y - r * 1.0);
        g.fill();
        g.setFill(Color.WHITE);
        g.setGlobalAlpha(lit * 0.3);
        g.fillOval(x - r * 0.6, y - r * 0.65, r * 0.3, r * 0.2);
    }

    private static void egg(GraphicsContext g, double x, double y, double r, int seed, double lit) {
        Color shell = SHELL.interpolate(BALLOONS[seed % BALLOONS.length], 0.18);
        g.setGlobalAlpha(lit);
        // L'œuf : plus étroit en haut, plus large en bas, en deux ovales qui se recouvrent.
        g.setFill(new RadialGradient(0, 0, x - r * 0.3, y - r * 0.2, r * 1.5, false, CycleMethod.NO_CYCLE,
                new Stop(0, shell.interpolate(Color.WHITE, 0.5)), new Stop(0.7, shell), new Stop(1, shell.darker())));
        eggPath(g, x, y, r);
        g.fill();
        // Les taches, plus nombreuses vers le bas.
        g.setFill(SPECKLE);
        for (int k = 0; k < 24; k++) {
            double a = UniverseView.noise(seed * 23 + k) * Math.PI * 2, d = Math.sqrt(UniverseView.noise(seed * 23 + k + 300));
            double sx = x + Math.cos(a) * d * r * 0.72, sy = y + 0.1 * r + Math.sin(a) * d * r * 0.78;
            double dot = r * (0.02 + 0.04 * UniverseView.noise(seed * 23 + k + 600));
            g.setGlobalAlpha(lit * 0.45);
            g.fillOval(sx - dot, sy - dot * 0.8, 2 * dot, 1.6 * dot);
        }
        g.setStroke(shell.darker());
        g.setLineWidth(1);
        g.setGlobalAlpha(lit * 0.5);
        eggPath(g, x, y, r);
        g.stroke();
        g.setFill(Color.WHITE);
        g.setGlobalAlpha(lit * 0.35);
        g.fillOval(x - r * 0.5, y - r * 0.6, r * 0.25, r * 0.4);
    }

    /** Le tracé d'un œuf, pointe en haut, dans un cercle de rayon {@code r}. */
    private static void eggPath(GraphicsContext g, double x, double y, double r) {
        g.beginPath();
        g.moveTo(x, y - r);
        g.bezierCurveTo(x + r * 0.55, y - r, x + r * 0.95, y - r * 0.1, x + r * 0.9, y + r * 0.35);
        g.bezierCurveTo(x + r * 0.85, y + r * 0.85, x + r * 0.45, y + r, x, y + r);
        g.bezierCurveTo(x - r * 0.45, y + r, x - r * 0.85, y + r * 0.85, x - r * 0.9, y + r * 0.35);
        g.bezierCurveTo(x - r * 0.95, y - r * 0.1, x - r * 0.55, y - r, x, y - r);
        g.closePath();
    }

    // ------------------------------------------------------------------
    // La naissance
    // ------------------------------------------------------------------

    /**
     * Les effets de la naissance habillée du skin, à {@code t} secondes de son début (jusqu'à {@link UniverseView#BIRTH_SECONDS}) ;
     * {@code front} est l'onde de l'expansion, en part de la demi-diagonale {@code half}. Les mots restent à la vue.
     */
    static void birth(GraphicsContext g, double w, double h, double cx, double cy, double half, double t, double front, Skin skin) {
        double size = Math.min(w, h);
        switch (skin) {
            case BIRTH_HATCHING -> hatching(g, cx, cy, size, t);
            case BIRTH_BUD -> bud(g, cx, cy, size, t);
            case BIRTH_FIREWORKS -> fireworks(g, w, h, cx, cy, half, t, front);
            default -> page(g, w, h, t);
        }
        g.setGlobalAlpha(1);
    }

    private static void hatching(GraphicsContext g, double cx, double cy, double size, double t) {
        double r = size * 0.16;
        double fade = Math.max(0, Math.min(1, (UniverseView.BIRTH_SECONDS - 1.5 - t) / 1.5));
        if (t < 1.0) {
            // L'œuf tremble, et une fissure court dessus.
            double shake = Math.sin(t * 60) * r * 0.05 * t;
            g.setGlobalAlpha(1);
            g.setFill(SHELL);
            eggPath(g, cx + shake, cy, r);
            g.fill();
            g.setStroke(Color.web("#3a2a18"));
            g.setLineWidth(Math.max(1.5, r * 0.05));
            double crack = Math.min(1, t / 0.9);
            int points = 2 + (int) (crack * 6);
            double[] xs = new double[points], ys = new double[points];
            for (int p = 0; p < points; p++) {
                double k = p / 7.0;
                xs[p] = cx + shake - r * 0.6 + k * r * 1.2;
                ys[p] = cy + (p % 2 == 0 ? -1 : 1) * r * 0.12;
            }
            g.strokePolyline(xs, ys, points);
            UniverseView.glow(g, cx, cy, r * 0.8 * crack, Color.WHITE, 0.5 * crack);
            return;
        }
        // La lumière sort, et les deux moitiés s'écartent puis s'éteignent.
        double open = Math.min(1, (t - 1.0) / 1.6);
        UniverseView.glow(g, cx, cy, r * (1 + 3 * open) * 1.5, Color.WHITE, 0.9 * fade);
        g.setGlobalAlpha(fade * (1 - 0.6 * open));
        g.setFill(SHELL);
        double apart = r * 1.6 * open, drop = r * 0.9 * open * open;
        for (int side = -1; side <= 1; side += 2) {
            g.save();
            g.translate(cx + side * apart, cy + drop);
            g.rotate(side * 40 * open);
            g.beginPath();
            if (side < 0) {
                g.moveTo(0, -r);
                g.bezierCurveTo(-r * 0.55, -r, -r * 0.95, -r * 0.1, -r * 0.9, r * 0.35);
                g.bezierCurveTo(-r * 0.85, r * 0.85, -r * 0.45, r, 0, r);
            } else {
                g.moveTo(0, -r);
                g.bezierCurveTo(r * 0.55, -r, r * 0.95, -r * 0.1, r * 0.9, r * 0.35);
                g.bezierCurveTo(r * 0.85, r * 0.85, r * 0.45, r, 0, r);
            }
            for (int p = 0; p <= 6; p++) g.lineTo((p % 2 == 0 ? 0 : -side * r * 0.12), r - p * r * 2 / 6);
            g.closePath();
            g.fill();
            g.restore();
        }
        // Les éclats de coquille, qui volent.
        g.setFill(SHELL);
        for (int i = 0; i < 24; i++) {
            double a = Math.PI * 2 * UniverseView.noise(i * 7 + 900), speed = 0.5 + UniverseView.noise(i * 7 + 901);
            double d = open * size * 0.45 * speed, fall = open * open * size * 0.15;
            double chip = r * 0.08;
            g.setGlobalAlpha(fade * (1 - open * 0.7));
            g.fillOval(cx + Math.cos(a) * d - chip, cy + Math.sin(a) * d * 0.7 + fall - chip, 2 * chip, 1.4 * chip);
        }
    }

    private static void bud(GraphicsContext g, double cx, double cy, double size, double t) {
        double fade = Math.max(0, Math.min(1, (UniverseView.BIRTH_SECONDS - 1.5 - t) / 1.5));
        double stem = Math.min(1, t / 1.0), open = Math.max(0, Math.min(1, (t - 1.0) / 2.0));
        double base = cy + size * 0.45, top = cy + size * 0.1;
        // La tige monte, en s'inclinant un peu.
        g.setGlobalAlpha(fade);
        g.setStroke(IVY);
        g.setLineWidth(Math.max(3, size * 0.015));
        g.setLineCap(StrokeLineCap.ROUND);
        g.beginPath();
        g.moveTo(cx, base);
        g.bezierCurveTo(cx - size * 0.04, base - (base - top) * 0.5 * stem, cx + size * 0.03, base - (base - top) * 0.8 * stem, cx, base - (base - top) * stem);
        g.stroke();
        if (stem < 1) return;
        // Les pétales, du bourgeon serré à la fleur ouverte, autour de la lumière.
        double r = size * 0.1;
        UniverseView.glow(g, cx, top, r * (0.5 + 3 * open), Color.web("#fff0b0"), 0.9 * open * fade);
        int petals = 6;
        for (int k = 0; k < petals; k++) {
            double a = -Math.PI / 2 + (k - (petals - 1) / 2.0) * (0.25 + 1.0 * open);
            double len = r * (1.4 + 0.8 * open), wide = r * (0.35 + 0.4 * open);
            g.save();
            g.translate(cx, top);
            g.rotate(Math.toDegrees(a) + 90);
            g.setGlobalAlpha(fade * 0.95);
            g.setFill(Color.web("#ff9fd6").interpolate(Color.web("#ffe0f0"), open * 0.6));
            g.fillOval(-wide, -len, 2 * wide, len);
            g.restore();
        }
        // Les sépales verts à la base du bourgeon.
        g.setFill(IVY_LIGHT);
        g.setGlobalAlpha(fade);
        for (int side = -1; side <= 1; side += 2) {
            g.fillPolygon(new double[] {cx, cx + side * r * (0.6 + 0.6 * open), cx + side * r * 0.2},
                    new double[] {top + r * 0.2, top - r * (0.3 - 0.3 * open), top + r * 0.9}, 3);
        }
        g.setFill(Color.web("#fff6c0"));
        double heart = r * 0.35 * open;
        g.fillOval(cx - heart, top - heart, 2 * heart, 2 * heart);
    }

    private static void fireworks(GraphicsContext g, double w, double h, double cx, double cy, double half, double t, double front) {
        if (t < 1.0) {
            // La fusée monte du bas, sa traînée derrière elle.
            double y = h + 10 - (h + 10 - cy) * t * (2 - t);
            g.setStroke(Color.web("#ffd27f"));
            g.setLineWidth(2);
            for (int k = 0; k < 10; k++) {
                g.setGlobalAlpha(0.6 * (1 - k / 10.0));
                g.strokeLine(cx + Math.sin(t * 40 + k) * 2, y + k * 9, cx + Math.sin(t * 40 + k + 1) * 2, y + k * 9 + 7);
            }
            UniverseView.glow(g, cx, y, 10, Color.WHITE, 0.9);
            g.setGlobalAlpha(1);
            g.setFill(Color.WHITE);
            g.fillOval(cx - 3, y - 3, 6, 6);
            return;
        }
        double since = t - 1.0, fade = Math.max(0, 1 - since / (UniverseView.BIRTH_SECONDS - 2.5));
        if (since < 0.25) {
            g.setGlobalAlpha((1 - since / 0.25) * 0.6);
            g.setFill(Color.WHITE);
            g.fillRect(0, 0, w, h);
        }
        // La gerbe : les étincelles partent en étoile, ralentissent et retombent, chacune de sa couleur.
        double gravity = half * 0.25;
        g.setLineCap(StrokeLineCap.ROUND);
        for (int i = 0; i < 110; i++) {
            double a = Math.PI * 2 * UniverseView.noise(i * 7 + 500), speed = half * (0.35 + 0.55 * UniverseView.noise(i * 7 + 501));
            double reach = speed * (1 - Math.exp(-since * 1.5)) / 1.5;
            double x = cx + Math.cos(a) * reach, y = cy + Math.sin(a) * reach * 0.85 + gravity * since * since * 0.5;
            double px = cx + Math.cos(a) * reach * 0.92, py = cy + Math.sin(a) * reach * 0.85 * 0.92 + gravity * Math.max(0, since - 0.08) * Math.max(0, since - 0.08) * 0.5;
            Color color = FIREWORK[i % FIREWORK.length];
            g.setStroke(color);
            g.setLineWidth(1.5 + UniverseView.noise(i * 7 + 502));
            g.setGlobalAlpha(fade * (0.6 + 0.4 * Math.sin(since * 12 + i)));
            g.strokeLine(px, py, x, y);
        }
        // L'onde, en écho à celle du Classique : un cercle pâle qui s'élargit.
        double radius = front * half;
        g.setStroke(Color.web("#ffd27f"));
        g.setLineWidth(1.5);
        g.setGlobalAlpha(0.35 * fade);
        g.strokeOval(cx - radius, cy - radius * 0.8, 2 * radius, 1.6 * radius);
    }

    private static void page(GraphicsContext g, double w, double h, double t) {
        // La page : elle couvre tout, puis se tourne de droite à gauche en se recourbant, et laisse voir le ciel.
        double turn = t < 0.8 ? 0 : Math.min(1, (t - 0.8) / (UniverseView.BIRTH_SECONDS - 3.5));
        double eased = turn * turn * (3 - 2 * turn);
        double edge = w * (1 - eased);
        if (edge <= 0) return;
        g.setGlobalAlpha(1);
        g.setFill(PAGE);
        g.fillRect(0, 0, edge, h);
        // Les lignes d'écriture pâles, et le coin qui se soulève : une ombre portée, et le dos de la page replié.
        g.setStroke(PAGE_SHADE);
        g.setLineWidth(1);
        g.setGlobalAlpha(0.35);
        for (double y = h * 0.15; y < h * 0.9; y += Math.max(14, h * 0.06)) g.strokeLine(w * 0.08, y, Math.min(edge - 10, w * 0.92), y);
        double curl = Math.min(w * 0.18, edge) * (0.3 + 0.7 * Math.sin(Math.PI * eased));
        g.setGlobalAlpha(0.5);
        g.setFill(new javafx.scene.paint.LinearGradient(edge - curl, 0, edge, 0, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.color(0, 0, 0, 0)), new Stop(1, Color.color(0, 0, 0, 0.6))));
        g.fillRect(edge - curl, 0, curl, h);
        g.setGlobalAlpha(0.95);
        g.setFill(PAGE_SHADE.interpolate(Color.WHITE, 0.4));
        g.fillPolygon(new double[] {edge, edge + curl * 0.6, edge + curl * 0.5, edge}, new double[] {0, h * 0.15, h * 0.85, h}, 4);
        g.setGlobalAlpha(1);
    }

    // ------------------------------------------------------------------
    // Les liens
    // ------------------------------------------------------------------

    /** Un lien habillé du skin, de ({@code ax}, {@code ay}) à ({@code bx}, {@code by}), allumé à {@code lit}, {@code seed} propre au lien. */
    static void link(GraphicsContext g, double ax, double ay, double bx, double by, double lit, double time, int seed, Skin skin) {
        switch (skin) {
            case LINKS_THREAD -> thread(g, ax, ay, bx, by, lit, seed);
            case LINKS_IVY -> ivy(g, ax, ay, bx, by, lit, time, seed);
            case LINKS_CIRCUIT -> circuit(g, ax, ay, bx, by, lit, time, seed);
            default -> constellation(g, ax, ay, bx, by, lit, time, seed);
        }
        g.setGlobalAlpha(1);
        g.setLineDashes();
    }

    private static void thread(GraphicsContext g, double ax, double ay, double bx, double by, double lit, int seed) {
        // Un fil qui pend un peu, et des points de couture en travers.
        double mx = (ax + bx) / 2, my = (ay + by) / 2 + Math.hypot(bx - ax, by - ay) * 0.12;
        g.setStroke(THREAD);
        g.setLineWidth(1.4);
        g.setGlobalAlpha(0.55 * lit);
        g.beginPath();
        g.moveTo(ax, ay);
        g.quadraticCurveTo(mx, my, bx, by);
        g.stroke();
        g.setLineWidth(1);
        g.setGlobalAlpha(0.6 * lit);
        for (int k = 1; k < 6; k++) {
            double t = k / 6.0, u = 1 - t;
            double x = u * u * ax + 2 * u * t * mx + t * t * bx, y = u * u * ay + 2 * u * t * my + t * t * by;
            double dx = 2 * u * (mx - ax) + 2 * t * (bx - mx), dy = 2 * u * (my - ay) + 2 * t * (by - my), len = Math.hypot(dx, dy);
            if (len <= 0) continue;
            double nx = -dy / len * 3, ny = dx / len * 3;
            g.strokeLine(x - nx, y - ny, x + nx, y + ny);
        }
        if (seed % 2 == 0) {
            g.setFill(THREAD);
            g.fillOval(mx - 2, my - 2, 4, 4);
        }
    }

    private static void ivy(GraphicsContext g, double ax, double ay, double bx, double by, double lit, double time, int seed) {
        double dx = bx - ax, dy = by - ay, len = Math.hypot(dx, dy);
        if (len <= 0) return;
        double nx = -dy / len, ny = dx / len;
        g.setStroke(IVY);
        g.setLineWidth(2);
        g.setGlobalAlpha(0.7 * lit);
        g.setLineCap(StrokeLineCap.ROUND);
        // La tige ondule de part et d'autre du trait.
        int n = 16;
        double[] xs = new double[n], ys = new double[n];
        for (int i = 0; i < n; i++) {
            double t = i / (double) (n - 1), wave = Math.sin(t * Math.PI * 3 + seed) * 5;
            xs[i] = ax + dx * t + nx * wave;
            ys[i] = ay + dy * t + ny * wave;
        }
        g.strokePolyline(xs, ys, n);
        // Les feuilles en cœur, une de chaque côté, qui frémissent.
        g.setFill(IVY_LIGHT);
        int leaves = Math.max(2, (int) (len / 45));
        for (int k = 1; k <= leaves; k++) {
            double t = k / (leaves + 1.0), side = k % 2 == 0 ? 1 : -1;
            double x = ax + dx * t + nx * Math.sin(t * Math.PI * 3 + seed) * 5, y = ay + dy * t + ny * Math.sin(t * Math.PI * 3 + seed) * 5;
            double leaf = 5 + Math.sin(time * 2 + k) * 0.5;
            double lx = x + nx * side * leaf * 1.2, ly = y + ny * side * leaf * 1.2;
            g.setGlobalAlpha(0.8 * lit);
            g.fillOval(lx - leaf * 0.9 + nx * side * leaf * 0.2, ly - leaf * 0.6, leaf * 1.2, leaf * 1.2);
            g.fillOval(lx - leaf * 0.3, ly - leaf * 0.6, leaf * 1.2, leaf * 1.2);
            g.fillPolygon(new double[] {lx - leaf * 0.8, lx + leaf * 0.8, lx}, new double[] {ly + leaf * 0.1, ly + leaf * 0.1, ly + leaf * 1.3}, 3);
        }
    }

    private static void circuit(GraphicsContext g, double ax, double ay, double bx, double by, double lit, double time, int seed) {
        // Une piste à angle droit : horizontale puis verticale, ou l'inverse selon le lien.
        boolean horizontalFirst = seed % 2 == 0;
        double kx = horizontalFirst ? bx : ax, ky = horizontalFirst ? ay : by;
        g.setStroke(COPPER);
        g.setLineWidth(2.2);
        g.setLineCap(StrokeLineCap.ROUND);
        g.setGlobalAlpha(0.6 * lit);
        g.strokePolyline(new double[] {ax, kx, bx}, new double[] {ay, ky, by}, 3);
        // Les pastilles aux bouts et au coude.
        g.setFill(COPPER_LIGHT);
        g.setGlobalAlpha(0.8 * lit);
        g.fillRect(kx - 3, ky - 3, 6, 6);
        g.fillOval(ax - 3, ay - 3, 6, 6);
        g.fillOval(bx - 3, by - 3, 6, 6);
        g.setFill(Color.web("#12161c"));
        g.fillOval(ax - 1.2, ay - 1.2, 2.4, 2.4);
        g.fillOval(bx - 1.2, by - 1.2, 2.4, 2.4);
        // L'impulsion, qui suit la piste.
        double first = Math.hypot(kx - ax, ky - ay), second = Math.hypot(bx - kx, by - ky), total = first + second;
        if (total <= 0) return;
        double along = ((time * 0.4 + UniverseView.noise(seed * 7)) % 1) * total;
        double px, py;
        if (along <= first) {
            double t = first > 0 ? along / first : 0;
            px = ax + (kx - ax) * t;
            py = ay + (ky - ay) * t;
        } else {
            double t = second > 0 ? (along - first) / second : 0;
            px = kx + (bx - kx) * t;
            py = ky + (by - ky) * t;
        }
        UniverseView.glow(g, px, py, 6, Color.web("#9fffe0"), 0.9 * lit);
    }

    private static void constellation(GraphicsContext g, double ax, double ay, double bx, double by, double lit, double time, int seed) {
        g.setStroke(STAR);
        g.setLineWidth(1);
        g.setLineDashes(3, 6);
        g.setGlobalAlpha(0.4 * lit);
        g.strokeLine(ax, ay, bx, by);
        g.setLineDashes();
        double mx = (ax + bx) / 2, my = (ay + by) / 2;
        double twinkle = 0.6 + 0.4 * Math.sin(time * 2.5 + seed);
        star(g, ax, ay, 5, lit * 0.9);
        star(g, bx, by, 5, lit * 0.9);
        star(g, mx, my, 3.5 * twinkle, lit * twinkle);
    }

    /** Une étoile à quatre branches, en traits. */
    private static void star(GraphicsContext g, double x, double y, double r, double alpha) {
        g.setStroke(STAR);
        g.setLineWidth(1.2);
        g.setGlobalAlpha(alpha);
        g.strokeLine(x - r, y, x + r, y);
        g.strokeLine(x, y - r, x, y + r);
        UniverseView.glow(g, x, y, r * 1.6, STAR, alpha * 0.6);
    }
}
