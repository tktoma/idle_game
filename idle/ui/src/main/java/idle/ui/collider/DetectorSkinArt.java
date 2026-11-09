package idle.ui.collider;

import idle.core.universe.Skin;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

/**
 * Le reste du collisionneur (version 0.195), dessiné à côté de {@link EnergyView} pour la garder lisible : le détecteur,
 * les éclairs du croisement, les étincelles et les quarks qui s'envolent, un visuel chacun.
 * <ul>
 * <li>Le détecteur : « Œil » (une paupière qui s'ouvre au croisement, l'iris doré quand il est à capter), « Lanterne »
 * (une lanterne de fer à la vitre ambrée, qui brille au croisement), « Cloche » (une cloche de bronze qui se balance
 * au croisement), « Appareil photo » (un boîtier à soufflet, dont le flash éclaire toute la vue au croisement) ;</li>
 * <li>les éclairs : « Fils d'or » (des fils fins et dorés, à peine courbés, semés de nœuds), « Fissures de glace »
 * (des brisures blanches et bleu pâle, qui se ramifient), « Racines » (des racines brunes qui descendent, de plus en
 * plus fines) ;</li>
 * <li>les étincelles : « Pétales » (roses, qui tombent lentement en tournant), « Confettis » (des rectangles de toutes
 * les couleurs), « Braises » (des points orange qui montent et rougissent) ;</li>
 * <li>les quarks : « Ballons » (un ballon de la couleur de la saveur, sa lettre dessus, qui monte au bout de sa
 * ficelle), « Papillons » (deux ailes qui battent, la lettre sur le corps), « Lettres de Scrabble » (un carreau de bois
 * clair, la lettre de la saveur et sa valeur en coin, qui tombe).</li>
 * </ul>
 */
final class DetectorSkinArt {

    private static final Color IRON = Color.web("#3a3f48");
    private static final Color AMBER = Color.web("#ffb347");
    private static final Color BRONZE = Color.web("#b8863b");
    private static final Color BRONZE_LIGHT = Color.web("#e8c070");
    private static final Color GOLD_THREAD = Color.web("#ffd36b");
    private static final Color ICE = Color.web("#dff6ff");
    private static final Color ICE_DEEP = Color.web("#7fc8ff");
    private static final Color ROOT = Color.web("#8a5a2a");
    private static final Color ROOT_LIGHT = Color.web("#c08a4a");
    private static final Color PETAL = Color.web("#ffb3c6");
    private static final Color PETAL_LIGHT = Color.web("#ffe0ea");
    private static final Color EMBER = Color.web("#ff8a3a");
    private static final Color EMBER_HOT = Color.web("#fff0a0");
    private static final Color TILE = Color.web("#f0dcb0");
    private static final Color TILE_EDGE = Color.web("#b89560");
    private static final Color TILE_INK = Color.web("#2a2018");
    private static final Color[] CONFETTI = {Color.web("#ff5a5a"), Color.web("#ffd23f"), Color.web("#3fd05a"), Color.web("#4aa3ff"),
        Color.web("#ff7ad9"), Color.web("#ffffff")};
    private static final Font TILE_LETTER = Font.font("System", FontWeight.BOLD, 11);
    private static final Font TILE_VALUE = Font.font(6);

    /** Combien de temps dure le flash de l'appareil photo, en secondes. */
    static final double FLASH_SECONDS = 0.35;

    private final EnergyView view;
    /** L'âge du dernier flash de l'appareil photo ; au-delà de {@link #FLASH_SECONDS}, il est fini. */
    private double flashAge = FLASH_SECONDS;
    /** Le balancement de la cloche : son âge depuis le dernier croisement. */
    private double swingAge = 10;
    private boolean wasCrossing;

    DetectorSkinArt(EnergyView view) {
        this.view = view;
    }

    /** Vrai pour les détecteurs dessinés ici. */
    static boolean drawsDetector(Skin detector) {
        return detector == Skin.DETECTOR_EYE || detector == Skin.DETECTOR_LANTERN || detector == Skin.DETECTOR_BELL || detector == Skin.DETECTOR_CAMERA;
    }

    /** Vrai pour les éclairs dessinés ici. */
    static boolean drawsLightning(Skin lightning) {
        return lightning == Skin.LIGHTNING_GOLD || lightning == Skin.LIGHTNING_ICE || lightning == Skin.LIGHTNING_ROOTS;
    }

    /** Vrai pour les étincelles dessinées ici. */
    static boolean drawsSparks(Skin sparks) {
        return sparks == Skin.SPARKS_PETALS || sparks == Skin.SPARKS_CONFETTI || sparks == Skin.SPARKS_EMBERS;
    }

    /** Vrai pour les quarks dessinés ici. */
    static boolean drawsQuarks(Skin flight) {
        return flight == Skin.QUARK_FLIGHT_BALLOONS || flight == Skin.QUARK_FLIGHT_BUTTERFLIES || flight == Skin.QUARK_FLIGHT_SCRABBLE;
    }

    /** Le flash de l'appareil photo encore visible : pour les vérifications. */
    boolean flashing() {
        return flashAge < FLASH_SECONDS;
    }

    // ------------------------------------------------------------------
    // Le détecteur
    // ------------------------------------------------------------------

    /**
     * Le détecteur, en bas de l'anneau. Appelé à chaque image, qu'il y ait croisement ou non : il tient le flash et le
     * balancement à jour.
     *
     * @param catchable vrai quand le croisement est à cliquer
     * @param crossing  vrai pendant que les faisceaux se croisent
     * @param elapsed   les secondes de l'image
     */
    void detector(GraphicsContext g, Skin detector, boolean catchable, boolean crossing, double elapsed) {
        if (crossing && !wasCrossing) {
            flashAge = 0;
            swingAge = 0;
        }
        wasCrossing = crossing;
        if (EnergyView.motion()) {
            flashAge += elapsed;
            swingAge += elapsed;
        }
        double x = view.px(Math.PI), y = view.py(Math.PI);
        switch (detector) {
            case DETECTOR_EYE -> eye(g, x, y, catchable, crossing);
            case DETECTOR_LANTERN -> lantern(g, x, y, catchable, crossing);
            case DETECTOR_BELL -> bell(g, x, y, catchable);
            default -> camera(g, x, y, catchable);
        }
    }

    /** Le flash de l'appareil photo, par-dessus tout : un voile blanc qui s'éteint en {@link #FLASH_SECONDS}. */
    void flash(GraphicsContext g, Skin detector, double w, double h) {
        if (detector != Skin.DETECTOR_CAMERA || !flashing()) return;
        double fade = 1 - flashAge / FLASH_SECONDS;
        g.setGlobalAlpha(0.85 * fade * fade);
        g.setFill(Color.WHITE);
        g.fillRect(0, 0, w, h);
        g.setGlobalAlpha(1);
    }

    /** L'œil : une amande blanche, l'iris bleu (doré à capter), la pupille ; la paupière se ferme hors du croisement. */
    private void eye(GraphicsContext g, double x, double y, boolean catchable, boolean crossing) {
        double open = crossing || catchable ? 1 : 0.45 + 0.1 * Math.sin(view.time() * 1.3);
        double hw = 16, hh = 9 * open;
        g.setFill(Color.web("#f4f4f8"));
        g.fillOval(x - hw, y - hh, hw * 2, hh * 2);
        Color iris = catchable ? Color.web("#ffd27f") : Color.web("#4a8ad8");
        g.setFill(iris);
        g.fillOval(x - 6, y - Math.min(6, hh), 12, Math.min(12, hh * 2));
        g.setFill(Color.web("#101018"));
        g.fillOval(x - 2.8, y - Math.min(2.8, hh), 5.6, Math.min(5.6, hh * 2));
        g.setFill(Color.WHITE);
        g.fillOval(x + 1, y - Math.min(3.5, hh), 2, Math.min(2, hh * 2));
        g.setStroke(Color.web("#2c4866"));
        g.setLineWidth(1.5);
        g.strokeOval(x - hw, y - hh, hw * 2, hh * 2);
    }

    /** La lanterne : un boîtier de fer à vitre ambrée, l'anneau en haut ; la flamme grandit au croisement. */
    private void lantern(GraphicsContext g, double x, double y, boolean catchable, boolean crossing) {
        g.setStroke(IRON);
        g.setLineWidth(1.5);
        g.strokeOval(x - 3, y - 15, 6, 5);
        g.setFill(IRON);
        g.fillRect(x - 9, y - 11, 18, 3);
        g.fillRect(x - 9, y + 7, 18, 3);
        double glow = catchable ? 1 : crossing ? 0.8 : 0.4 + 0.1 * Math.sin(view.time() * 5);
        g.setFill(Color.color(AMBER.getRed(), AMBER.getGreen(), AMBER.getBlue(), 0.55 + 0.45 * glow));
        g.fillRect(x - 7, y - 8, 14, 15);
        EnergyView.glow(g, x, y, 10 + 12 * glow, AMBER, 0.5 * glow);
        g.setFill(Color.web("#fff3b0"));
        double fh = 4 + 4 * glow;
        g.fillOval(x - 2, y + 3 - fh, 4, fh);
        g.setStroke(IRON);
        g.setLineWidth(1);
        g.strokeLine(x, y - 8, x, y + 7);
    }

    /** La cloche : un bronze qui se balance une seconde après le croisement, le battant dessous. */
    private void bell(GraphicsContext g, double x, double y, boolean catchable) {
        double swing = swingAge < 1.5 ? Math.sin(swingAge * 14) * 0.35 * (1 - swingAge / 1.5) : 0;
        g.save();
        g.translate(x, y - 12);
        g.rotate(Math.toDegrees(swing));
        g.setFill(catchable ? BRONZE_LIGHT : BRONZE);
        g.fillArc(-11, 0, 22, 26, 0, 180, ArcType.ROUND);
        g.fillRect(-11, 13, 22, 4);
        g.fillOval(-3, -3, 6, 6);
        g.setFill(Color.web("#5a3a14"));
        g.fillOval(-2.5, 15, 5, 5);
        g.setStroke(Color.web("#fff0c0"));
        g.setLineWidth(1);
        g.strokeArc(-8, 3, 16, 18, 20, 70, ArcType.OPEN);
        g.restore();
    }

    /** L'appareil photo ancien : un boîtier noir à soufflet, l'objectif rond, le réflecteur du flash au-dessus. */
    private void camera(GraphicsContext g, double x, double y, boolean catchable) {
        g.setFill(Color.web("#1c1c22"));
        g.fillRoundRect(x - 14, y - 7, 28, 16, 3, 3);
        g.setStroke(Color.web("#6a6a74"));
        g.setLineWidth(1);
        for (int k = 0; k < 4; k++) g.strokeLine(x - 6 + k * 4, y - 7, x - 6 + k * 4, y + 9);
        g.setFill(Color.web("#8a8a94"));
        g.fillOval(x - 6, y - 4, 12, 12);
        g.setFill(catchable ? Color.web("#ffd27f") : Color.web("#203050"));
        g.fillOval(x - 3.5, y - 1.5, 7, 7);
        // Le réflecteur : une coupe argentée au bout de sa tige, blanche pendant le flash.
        g.setStroke(Color.web("#8a8a94"));
        g.strokeLine(x + 9, y - 7, x + 9, y - 13);
        g.setFill(flashing() ? Color.WHITE : Color.web("#c8c8d0"));
        g.fillArc(x + 3, y - 20, 12, 12, 0, 180, ArcType.ROUND);
    }

    // ------------------------------------------------------------------
    // Les éclairs du croisement
    // ------------------------------------------------------------------

    /**
     * Les éclairs du croisement sous ce skin : entre les deux boules, puis autour, comme les arcs classiques, mais en
     * fils d'or, en fissures de glace ou en racines.
     *
     * @return le nombre de traits dessinés
     */
    int lightning(GraphicsContext g, Skin lightning, double angle, double heat) {
        double ax = view.px(angle), ay = view.py(angle), bx = view.px(-angle), by = view.py(-angle);
        double mx = (ax + bx) / 2, my = (ay + by) / 2;
        long tick = (long) Math.floor(view.time() * (lightning == Skin.LIGHTNING_ROOTS ? 4 : 20));
        int count = 3 + (int) Math.round(3 * heat);
        for (int k = 0; k < count; k++) {
            double fromX, fromY, toX, toY;
            if (k == 0) {
                fromX = ax; fromY = ay; toX = bx; toY = by;
            } else {
                // Les racines descendent ; les autres partent vers le haut et les côtés.
                double direction = lightning == Skin.LIGHTNING_ROOTS ? Math.PI * (0.15 + 0.7 * EnergyView.random(tick * 31 + k * 7))
                        : -Math.PI * (0.05 + 0.9 * EnergyView.random(tick * 31 + k * 7));
                double reach = (16 + 22 * EnergyView.random(tick * 31 + k * 7 + 1)) * (1 + 0.6 * heat);
                fromX = mx; fromY = my;
                toX = mx + Math.cos(direction) * reach;
                toY = my + Math.sin(direction) * reach;
            }
            if (Math.hypot(toX - fromX, toY - fromY) < 2) {
                toX = fromX + 10;
                toY = fromY - 6;
            }
            switch (lightning) {
                case LIGHTNING_GOLD -> goldThread(g, fromX, fromY, toX, toY, tick * 131 + k * 17);
                case LIGHTNING_ICE -> iceCrack(g, fromX, fromY, toX, toY, tick * 131 + k * 17, heat);
                default -> root(g, fromX, fromY, toX, toY, tick * 131 + k * 17, 1.6 + 1.2 * heat);
            }
        }
        g.setGlobalAlpha(1);
        Color glow = lightning == Skin.LIGHTNING_GOLD ? GOLD_THREAD : lightning == Skin.LIGHTNING_ICE ? ICE_DEEP : ROOT_LIGHT;
        EnergyView.glow(g, mx, my, 18 + 10 * heat, glow, 0.45);
        return count;
    }

    /** Un fil d'or : une courbe douce, fine et brillante, trois nœuds dessus. */
    private static void goldThread(GraphicsContext g, double x1, double y1, double x2, double y2, long seed) {
        double length = Math.hypot(x2 - x1, y2 - y1), nx = -(y2 - y1) / length, ny = (x2 - x1) / length;
        double bow = (EnergyView.random(seed) - 0.5) * length * 0.5;
        int points = 10;
        double[] xs = new double[points + 1], ys = new double[points + 1];
        for (int p = 0; p <= points; p++) {
            double t = p / (double) points, arch = Math.sin(Math.PI * t) * bow;
            xs[p] = x1 + (x2 - x1) * t + nx * arch;
            ys[p] = y1 + (y2 - y1) * t + ny * arch;
        }
        g.setStroke(Color.web("#b8860b"));
        g.setLineWidth(2.2);
        g.setGlobalAlpha(0.5);
        g.strokePolyline(xs, ys, points + 1);
        g.setStroke(GOLD_THREAD);
        g.setLineWidth(1);
        g.setGlobalAlpha(0.95);
        g.strokePolyline(xs, ys, points + 1);
        g.setFill(Color.web("#fff8dc"));
        for (int k = 1; k <= 3; k++) {
            int p = (int) Math.round(points * k / 4.0);
            g.fillOval(xs[p] - 1.5, ys[p] - 1.5, 3, 3);
        }
    }

    /** Une fissure de glace : un trait blanc à cassures nettes, bordé de bleu pâle, avec une ramification. */
    private static void iceCrack(GraphicsContext g, double x1, double y1, double x2, double y2, long seed, double heat) {
        double length = Math.hypot(x2 - x1, y2 - y1), nx = -(y2 - y1) / length, ny = (x2 - x1) / length;
        int points = 6;
        double[] xs = new double[points + 1], ys = new double[points + 1];
        double jag = (2 + 3 * heat) * Math.min(1, length / 20);
        for (int p = 0; p <= points; p++) {
            double t = p / (double) points;
            double offset = p == 0 || p == points ? 0 : (EnergyView.random(seed + p) - 0.5) * 2 * jag;
            xs[p] = x1 + (x2 - x1) * t + nx * offset;
            ys[p] = y1 + (y2 - y1) * t + ny * offset;
        }
        g.setStroke(ICE_DEEP);
        g.setLineWidth(3);
        g.setGlobalAlpha(0.35);
        g.strokePolyline(xs, ys, points + 1);
        g.setStroke(ICE);
        g.setLineWidth(1.2);
        g.setGlobalAlpha(0.95);
        g.strokePolyline(xs, ys, points + 1);
        // La ramification : du milieu, un éclat court en biais.
        int mid = points / 2;
        double side = EnergyView.random(seed + 50) < 0.5 ? 1 : -1;
        g.setLineWidth(0.8);
        g.strokeLine(xs[mid], ys[mid], xs[mid] + nx * side * 7 + (x2 - x1) / length * 5, ys[mid] + ny * side * 7 + (y2 - y1) / length * 5);
    }

    /** Une racine : brune, épaisse au départ et fine au bout, qui ondule et se divise en deux radicelles. */
    private static void root(GraphicsContext g, double x1, double y1, double x2, double y2, long seed, double width) {
        double length = Math.hypot(x2 - x1, y2 - y1), nx = -(y2 - y1) / length, ny = (x2 - x1) / length;
        int points = 8;
        double px = x1, py = y1;
        g.setGlobalAlpha(0.9);
        for (int p = 1; p <= points; p++) {
            double t = p / (double) points;
            double wave = Math.sin(t * Math.PI * 2 + seed % 7) * 3 * Math.sin(Math.PI * t);
            double x = x1 + (x2 - x1) * t + nx * wave, y = y1 + (y2 - y1) * t + ny * wave;
            g.setStroke(p % 2 == 0 ? ROOT : ROOT_LIGHT);
            g.setLineWidth(Math.max(0.5, width * (1 - 0.8 * t)));
            g.strokeLine(px, py, x, y);
            px = x;
            py = y;
        }
        g.setStroke(ROOT_LIGHT);
        g.setLineWidth(0.6);
        double ex = (x2 - x1) / length * 6, ey = (y2 - y1) / length * 6;
        g.strokeLine(x2, y2, x2 + ex + nx * 4, y2 + ey + ny * 4);
        g.strokeLine(x2, y2, x2 + ex - nx * 4, y2 + ey - ny * 4);
    }

    // ------------------------------------------------------------------
    // Les étincelles
    // ------------------------------------------------------------------

    /** La palette d'une gerbe sous ce skin : les pétales roses, les confettis de toutes les couleurs, les braises orange. */
    static Color[] sparkPalette(Skin sparks, boolean caught) {
        return switch (sparks) {
            case SPARKS_PETALS -> caught ? new Color[] {PETAL, PETAL_LIGHT, Color.WHITE} : new Color[] {PETAL, PETAL_LIGHT};
            case SPARKS_CONFETTI -> CONFETTI;
            default -> caught ? new Color[] {EMBER, EMBER_HOT, Color.web("#ff4a1c")} : new Color[] {EMBER, Color.web("#ff4a1c")};
        };
    }

    /** La pesanteur d'une étincelle sous ce skin, en pixels par seconde carrée : les pétales et les confettis tombent doucement, les braises montent. */
    static double sparkGravity(Skin sparks) {
        return switch (sparks) {
            case SPARKS_PETALS -> 20;
            case SPARKS_CONFETTI -> 35;
            default -> -25;
        };
    }

    /** Une étincelle : un pétale qui tourne, un confetti rectangulaire, une braise ronde qui rougit en vieillissant. */
    static void spark(GraphicsContext g, Skin sparks, double x, double y, Color color, double fade, double time, int index) {
        switch (sparks) {
            case SPARKS_PETALS -> {
                g.save();
                g.translate(x, y);
                g.rotate(time * 180 + index * 40);
                g.setFill(color);
                g.fillOval(-3.5, -2, 7, 4);
                g.restore();
            }
            case SPARKS_CONFETTI -> {
                g.save();
                g.translate(x, y);
                g.rotate(time * 240 + index * 70);
                g.setFill(color);
                g.fillRect(-3, -1.5, 6, 3);
                g.restore();
            }
            default -> {
                Color ember = fade > 0.6 ? EMBER_HOT : fade > 0.3 ? color : Color.web("#a02010");
                g.setFill(ember);
                double size = 2 + 2.5 * fade;
                g.fillOval(x - size / 2, y - size / 2, size, size);
            }
        }
    }

    // ------------------------------------------------------------------
    // Les quarks qui s'envolent
    // ------------------------------------------------------------------

    /** La pesanteur d'un quark sous ce skin : les ballons montent, les papillons flottent, les carreaux tombent. */
    static double quarkGravity(Skin flight) {
        return switch (flight) {
            case QUARK_FLIGHT_BALLOONS -> -30;
            case QUARK_FLIGHT_BUTTERFLIES -> -4;
            default -> 90;
        };
    }

    /** La valeur Scrabble de la lettre d'une saveur : u 1, d 2, s 1, c 3, b 3, t 1 (les valeurs françaises). */
    static int scrabbleValue(String symbol) {
        return switch (symbol.charAt(0)) {
            case 'd' -> 2;
            case 'c', 'b' -> 3;
            default -> 1;
        };
    }

    /** Un quark : sa lettre sur un ballon, un papillon ou un carreau de Scrabble, de la couleur de sa saveur. */
    static void quark(GraphicsContext g, Skin flight, double x, double y, double size, Color color, String symbol, String count, double time) {
        g.setTextAlign(TextAlignment.CENTER);
        switch (flight) {
            case QUARK_FLIGHT_BALLOONS -> {
                double w = size * 0.9, h = size * 1.1;
                g.setStroke(Color.web("#d8d8e0"));
                g.setLineWidth(0.8);
                g.strokeLine(x, y + h / 2, x + 3 * Math.sin(time * 4), y + h / 2 + size * 0.9);
                g.setFill(color);
                g.fillOval(x - w / 2, y - h / 2, w, h);
                g.fillPolygon(new double[] {x - 2.5, x + 2.5, x}, new double[] {y + h / 2 + 3, y + h / 2 + 3, y + h / 2 - 1}, 3);
                g.setFill(Color.color(1, 1, 1, 0.5));
                g.fillOval(x - w * 0.3, y - h * 0.35, w * 0.22, h * 0.3);
                g.setFill(Color.web("#060b14"));
                g.setFont(Font.font(size * 0.55));
                g.fillText(symbol, x, y + size * 0.2);
            }
            case QUARK_FLIGHT_BUTTERFLIES -> {
                double flap = 0.35 + 0.65 * Math.abs(Math.sin(time * 9));
                double w = size * 0.75 * flap, h = size * 0.5;
                g.setFill(color);
                g.fillOval(x - w, y - h, w, h);
                g.fillOval(x, y - h, w, h);
                g.fillOval(x - w * 0.8, y - 1, w * 0.8, h * 0.8);
                g.fillOval(x, y - 1, w * 0.8, h * 0.8);
                g.setFill(Color.color(1, 1, 1, 0.45));
                g.fillOval(x - w * 0.7, y - h * 0.8, w * 0.4, h * 0.4);
                g.fillOval(x + w * 0.3, y - h * 0.8, w * 0.4, h * 0.4);
                g.setFill(Color.web("#2a2030"));
                g.fillOval(x - 1.5, y - h, 3, h * 1.8);
                g.setFill(Color.WHITE);
                g.setFont(Font.font("System", FontWeight.BOLD, size * 0.4));
                g.fillText(symbol, x, y + size * 0.15);
            }
            default -> {
                double s = size * 1.1;
                g.setFill(TILE_EDGE);
                g.fillRoundRect(x - s / 2 + 1, y - s / 2 + 1, s, s, 2, 2);
                g.setFill(TILE);
                g.fillRoundRect(x - s / 2, y - s / 2, s, s, 2, 2);
                g.setStroke(color);
                g.setLineWidth(1.2);
                g.strokeRoundRect(x - s / 2, y - s / 2, s, s, 2, 2);
                g.setFill(TILE_INK);
                g.setFont(TILE_LETTER);
                g.fillText(symbol.toUpperCase(java.util.Locale.ROOT), x - 1, y + 4);
                g.setFont(TILE_VALUE);
                g.setTextAlign(TextAlignment.RIGHT);
                g.fillText(String.valueOf(scrabbleValue(symbol)), x + s / 2 - 1.5, y + s / 2 - 1.5);
                g.setTextAlign(TextAlignment.CENTER);
            }
        }
        if (count != null) {
            g.setFill(color);
            g.setFont(Font.font(12));
            g.fillText(count, x + size * 0.9, y - size * 0.4);
        }
        g.setTextAlign(TextAlignment.LEFT);
    }
}
