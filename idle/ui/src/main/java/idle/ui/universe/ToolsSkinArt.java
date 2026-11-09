package idle.ui.universe;

import idle.core.universe.Skin;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;

/**
 * Les skins de l'établi de la graine (version 0.196), dessinés à côté de {@link SeedView} : l'arrosoir, l'étagère, le tuyau
 * et le chrono, un visuel chacun.
 * <ul>
 * <li>L'arrosoir : « Théière » (une panse ronde, son couvercle à bouton, une anse en boucle et un bec courbe), « Pichet »
 * (une cruche de grès haute, à l'anse plate et au large bec), « Nuage » (un nuage qu'on presse, dont la pluie coule par le
 * côté), « Bec d'oiseau » (un oiseau rond dont le bec verse), « Coquillage » (une coquille en éventail, striée, ouverte à
 * droite) ; chacun garde la taille, le centre et l'inclinaison de l'arrosoir, et verse au même endroit ;</li>
 * <li>l'étagère : « Pierre » (des dalles grises, épaisses, sur des piliers), « Atelier » (de la tôle sombre rivetée, des
 * montants perforés), « Bambou » (des tiges noueuses et des planches de lattes), « Nuage » (des planches de nuage qui
 * flottent, sans montants) ;</li>
 * <li>le tuyau : « Liane » (une tige verte, ses feuilles et une vrille au bout), « Fontaine » (une conduite de pierre et
 * une vasque au pied de la plante), « Rigole » (une tranchée creusée dans la terre, l'eau au fond), « Pipette » (un tube de
 * verre gradué, sa poire de caoutchouc à gauche) ;</li>
 * <li>le chrono : « Sablier » (deux ampoules dans leur bâti de bois, le sable en bas à hauteur du temps gardé, un filet
 * qui coule quand il tourne), « Montre à gousset » (un boîtier d'or à sa chaîne, le cadran blanc, l'aiguille), « Cadran
 * solaire » (un disque de pierre, son style et son ombre, le secteur éclairé à hauteur du temps gardé), « Métronome »
 * (une pyramide de bois, son balancier qui bat quand il tourne, la réserve en jauge sur le côté).</li>
 * </ul>
 */
final class ToolsSkinArt {

    private static final Color PORCELAIN = Color.web("#f0ece4");
    private static final Color PORCELAIN_BLUE = Color.web("#4a78b8");
    private static final Color STONEWARE = Color.web("#a8835a");
    private static final Color STONEWARE_DARK = Color.web("#6e5236");
    private static final Color CLOUD = Color.web("#dfe6f2");
    private static final Color CLOUD_SHADE = Color.web("#a9b6cc");
    private static final Color BIRD = Color.web("#5fa8d8");
    private static final Color BIRD_BELLY = Color.web("#dff0ff");
    private static final Color BEAK = Color.web("#ffb347");
    private static final Color SHELL = Color.web("#f2d8c0");
    private static final Color SHELL_RIDGE = Color.web("#c89a78");
    private static final Color GHOST = Color.web("#f7e7a8");
    private static final Color STONE = Color.web("#8a8d94");
    private static final Color STONE_DARK = Color.web("#565960");
    private static final Color STEEL = Color.web("#4a5160");
    private static final Color STEEL_LIGHT = Color.web("#8e98a8");
    private static final Color RIVET = Color.web("#c0c8d4");
    private static final Color BAMBOO = Color.web("#b8b85a");
    private static final Color BAMBOO_DARK = Color.web("#6e7a2a");
    private static final Color VINE = Color.web("#4f9b4a");
    private static final Color VINE_LIGHT = Color.web("#8fd38a");
    private static final Color WATER = Color.web("#8fd3ff");
    private static final Color EARTH = Color.web("#6b4a2a");
    private static final Color EARTH_DARK = Color.web("#3e2a16");
    private static final Color GLASS = Color.web("#dff4ff");
    private static final Color RUBBER = Color.web("#c84a3a");
    private static final Color WOOD = Color.web("#8a6440");
    private static final Color WOOD_DARK = Color.web("#5a3f26");
    private static final Color SAND = Color.web("#e8c878");
    private static final Color GOLD = Color.web("#e0b84a");
    private static final Color GOLD_DARK = Color.web("#8a6a2a");
    private static final Color DIAL = Color.web("#f6f1e4");
    private static final Color CHRONO_TIME = Color.web("#8fd3ff");
    private static final Color SEAM = Color.web("#fff6d2");

    private ToolsSkinArt() {
    }

    // ------------------------------------------------------------------
    // L'arrosoir
    // ------------------------------------------------------------------

    /**
     * L'arrosoir habillé du skin, centré en ({@code x}, {@code y}), de taille {@code s}, penché de {@code degrees} ; seulement
     * sa silhouette si {@code ghost}. Le bec reste à droite, en haut, là où le Classique verse.
     */
    static void can(GraphicsContext g, double x, double y, double s, double degrees, boolean ghost, Skin skin) {
        g.save();
        g.translate(x, y);
        g.rotate(degrees);
        switch (skin) {
            case CAN_TEAPOT -> teapot(g, s, ghost);
            case CAN_PITCHER -> pitcher(g, s, ghost);
            case CAN_CLOUD -> cloud(g, s, ghost);
            case CAN_BEAK -> bird(g, s, ghost);
            default -> shell(g, s, ghost);
        }
        g.restore();
    }

    private static void teapot(GraphicsContext g, double s, boolean ghost) {
        g.setLineCap(StrokeLineCap.ROUND);
        // L'anse en boucle, à gauche, puis le bec courbe à droite.
        g.setStroke(ghost ? GHOST : PORCELAIN);
        g.setLineWidth(Math.max(2, s * 0.09));
        g.beginPath();
        g.moveTo(-s * 0.35, -s * 0.15);
        g.bezierCurveTo(-s * 0.95, -s * 0.3, -s * 0.95, s * 0.35, -s * 0.35, s * 0.25);
        g.stroke();
        g.setLineWidth(Math.max(3, s * 0.13));
        g.beginPath();
        g.moveTo(s * 0.35, s * 0.05);
        g.bezierCurveTo(s * 0.7, -s * 0.05, s * 0.85, -s * 0.25, s * 0.97, -s * 0.43);
        g.stroke();
        // La panse, et son couvercle à bouton.
        g.setFill(ghost ? GHOST : PORCELAIN);
        g.fillOval(-s * 0.48, -s * 0.3, s * 0.96, s * 0.72);
        g.fillOval(-s * 0.3, -s * 0.4, s * 0.6, s * 0.22);
        g.fillOval(-s * 0.07, -s * 0.5, s * 0.14, s * 0.14);
        if (ghost) return;
        // Le décor bleu : une frise de points et une fleur.
        g.setFill(PORCELAIN_BLUE);
        for (int k = -2; k <= 2; k++) g.fillOval(k * s * 0.15 - s * 0.03, -s * 0.2, s * 0.06, s * 0.06);
        for (int k = 0; k < 5; k++) {
            double a = Math.PI * 2 * k / 5;
            g.fillOval(Math.cos(a) * s * 0.08 - s * 0.04, s * 0.08 + Math.sin(a) * s * 0.08 - s * 0.04, s * 0.08, s * 0.08);
        }
        g.setStroke(PORCELAIN_BLUE);
        g.setLineWidth(1);
        g.strokeOval(-s * 0.48, -s * 0.3, s * 0.96, s * 0.72);
    }

    private static void pitcher(GraphicsContext g, double s, boolean ghost) {
        // L'anse plate à gauche, le corps haut, le bec large en haut à droite.
        g.setStroke(ghost ? GHOST : STONEWARE_DARK);
        g.setLineWidth(Math.max(2, s * 0.1));
        g.setLineCap(StrokeLineCap.ROUND);
        g.beginPath();
        g.moveTo(-s * 0.3, -s * 0.35);
        g.bezierCurveTo(-s * 0.8, -s * 0.4, -s * 0.8, s * 0.25, -s * 0.35, s * 0.2);
        g.stroke();
        g.setFill(ghost ? GHOST : STONEWARE);
        g.beginPath();
        g.moveTo(-s * 0.3, -s * 0.5);
        g.lineTo(s * 0.3, -s * 0.5);
        g.lineTo(s * 0.97, -s * 0.5);
        g.lineTo(s * 0.4, -s * 0.25);
        g.bezierCurveTo(s * 0.5, s * 0.1, s * 0.48, s * 0.3, s * 0.42, s * 0.42);
        g.lineTo(-s * 0.42, s * 0.42);
        g.bezierCurveTo(-s * 0.48, s * 0.3, -s * 0.5, s * 0.1, -s * 0.4, -s * 0.25);
        g.closePath();
        g.fill();
        if (ghost) return;
        g.setFill(STONEWARE_DARK);
        g.fillRect(-s * 0.42, s * 0.3, s * 0.84, s * 0.12);
        g.setStroke(STONEWARE_DARK);
        g.setLineWidth(1.2);
        g.strokeLine(-s * 0.32, -s * 0.38, s * 0.32, -s * 0.38);
        g.setFill(Color.WHITE);
        g.setGlobalAlpha(g.getGlobalAlpha() * 0.35);
        g.fillRect(-s * 0.28, -s * 0.2, s * 0.08, s * 0.45);
        g.setGlobalAlpha(1);
    }

    private static void cloud(GraphicsContext g, double s, boolean ghost) {
        // Un nuage dodu ; sa pluie coule du côté droit, vers le bec du Classique.
        g.setFill(ghost ? GHOST : CLOUD_SHADE);
        g.fillOval(-s * 0.55, -s * 0.1, s * 1.3, s * 0.55);
        g.setFill(ghost ? GHOST : CLOUD);
        g.fillOval(-s * 0.6, -s * 0.2, s * 0.7, s * 0.6);
        g.fillOval(-s * 0.3, -s * 0.5, s * 0.8, s * 0.8);
        g.fillOval(s * 0.15, -s * 0.3, s * 0.65, s * 0.6);
        g.fillRoundRect(-s * 0.6, -s * 0.05, s * 1.4, s * 0.42, s * 0.4, s * 0.4);
        if (ghost) return;
        g.setFill(WATER);
        g.fillOval(s * 0.85, -s * 0.5, s * 0.12, s * 0.2);
        g.fillOval(s * 0.65, s * 0.3, s * 0.1, s * 0.16);
        g.fillOval(s * 0.35, s * 0.4, s * 0.1, s * 0.16);
    }

    private static void bird(GraphicsContext g, double s, boolean ghost) {
        // La queue à gauche, le corps rond, la tête vers la droite et le bec ouvert au bout.
        g.setFill(ghost ? GHOST : BIRD);
        g.fillPolygon(new double[] {-s * 0.4, -s * 0.85, -s * 0.8, -s * 0.35}, new double[] {s * 0.05, -s * 0.2, s * 0.2, s * 0.25}, 4);
        g.fillOval(-s * 0.5, -s * 0.3, s * 1.0, s * 0.75);
        g.fillOval(s * 0.2, -s * 0.6, s * 0.56, s * 0.5);
        g.setFill(ghost ? GHOST : BEAK);
        g.fillPolygon(new double[] {s * 0.7, s * 0.97, s * 0.72}, new double[] {-s * 0.45, -s * 0.43, -s * 0.3}, 3);
        g.fillPolygon(new double[] {s * 0.7, s * 0.95, s * 0.72}, new double[] {-s * 0.42, -s * 0.52, -s * 0.52}, 3);
        if (ghost) return;
        g.setFill(BIRD_BELLY);
        g.fillOval(-s * 0.3, -s * 0.05, s * 0.6, s * 0.45);
        g.setFill(BIRD.darker());
        g.fillOval(-s * 0.35, -s * 0.2, s * 0.5, s * 0.3);
        g.setFill(Color.WHITE);
        g.fillOval(s * 0.46, -s * 0.5, s * 0.14, s * 0.14);
        g.setFill(Color.BLACK);
        g.fillOval(s * 0.51, -s * 0.47, s * 0.07, s * 0.07);
        g.setStroke(BEAK);
        g.setLineWidth(Math.max(1.5, s * 0.05));
        g.strokeLine(-s * 0.15, s * 0.42, -s * 0.2, s * 0.6);
        g.strokeLine(s * 0.1, s * 0.42, s * 0.12, s * 0.6);
    }

    private static void shell(GraphicsContext g, double s, boolean ghost) {
        // Une coquille en éventail, la charnière en bas à gauche, ouverte vers le haut à droite.
        g.save();
        g.translate(-s * 0.2, s * 0.2);
        g.rotate(-35);
        g.setFill(ghost ? GHOST : SHELL);
        g.fillArc(-s * 0.75, -s * 0.75, s * 1.5, s * 1.5, 10, 160, ArcType.ROUND);
        g.fillRect(-s * 0.2, -s * 0.05, s * 0.4, s * 0.25);
        if (!ghost) {
            g.setStroke(SHELL_RIDGE);
            g.setLineWidth(Math.max(1, s * 0.03));
            for (int k = 0; k <= 8; k++) {
                double a = Math.toRadians(10 + 160 * k / 8.0);
                g.strokeLine(0, 0, Math.cos(a) * s * 0.75, -Math.sin(a) * s * 0.75);
            }
            g.setLineWidth(1.5);
            g.strokeArc(-s * 0.75, -s * 0.75, s * 1.5, s * 1.5, 10, 160, ArcType.OPEN);
            g.strokeArc(-s * 0.45, -s * 0.45, s * 0.9, s * 0.9, 10, 160, ArcType.OPEN);
        }
        g.restore();
        if (ghost) return;
        g.setFill(WATER);
        g.setGlobalAlpha(g.getGlobalAlpha() * 0.6);
        g.fillOval(s * 0.7, -s * 0.5, s * 0.25, s * 0.14);
        g.setGlobalAlpha(1);
    }

    // ------------------------------------------------------------------
    // L'étagère
    // ------------------------------------------------------------------

    /**
     * L'étagère habillée du skin, de {@code left} à {@code right}, sa planche du bas à {@code top}, celle du milieu à
     * {@code mid} (quand le chrono est là) et celle du haut à {@code high}, épaisses de {@code thick}, les montants larges de
     * {@code post}.
     */
    static void shelf(GraphicsContext g, double left, double right, double top, double mid, double high, double thick, double post,
            boolean chrono, Skin skin) {
        double[] boards = chrono ? new double[] {top, mid, high - thick} : new double[] {top, high - thick};
        double plank = right - left;
        switch (skin) {
            case SHELF_STONE -> {
                g.setGlobalAlpha(0.5);
                g.setFill(STONE_DARK);
                g.fillRect(left + post, high, plank - 2 * post, top - high);
                g.setGlobalAlpha(1);
                // Les piliers, en blocs empilés.
                g.setFill(STONE);
                for (double x : new double[] {left, right - post * 1.6}) {
                    g.fillRect(x, high - thick, post * 1.6, top + thick * 2 - high);
                    g.setStroke(STONE_DARK);
                    g.setLineWidth(1);
                    for (double y = high; y < top + thick; y += thick * 1.6) g.strokeLine(x, y, x + post * 1.6, y);
                }
                for (double board : boards) {
                    g.setFill(STONE);
                    g.fillRect(left - thick * 0.4, board, plank + thick * 0.8, thick * 1.3);
                    g.setFill(STONE_DARK);
                    g.fillRect(left - thick * 0.4, board + thick * 0.9, plank + thick * 0.8, thick * 0.4);
                    g.setStroke(STONE_DARK);
                    g.setGlobalAlpha(0.5);
                    g.strokeLine(left + plank * 0.2, board + thick * 0.3, left + plank * 0.35, board + thick * 0.7);
                    g.strokeLine(left + plank * 0.7, board + thick * 0.2, left + plank * 0.6, board + thick * 0.8);
                    g.setGlobalAlpha(1);
                }
            }
            case SHELF_WORKSHOP -> {
                g.setGlobalAlpha(0.6);
                g.setFill(Color.web("#262b36"));
                g.fillRect(left + post, high, plank - 2 * post, top - high);
                g.setGlobalAlpha(1);
                // Les montants perforés, en cornière.
                g.setFill(STEEL);
                for (double x : new double[] {left, right - post}) {
                    g.fillRect(x, high - thick, post, top + thick * 2 - high);
                    g.setFill(Color.web("#12161c"));
                    for (double y = high + thick; y < top; y += thick * 1.5) g.fillOval(x + post * 0.25, y, post * 0.5, post * 0.5);
                    g.setFill(STEEL);
                }
                for (double board : boards) {
                    g.setFill(new LinearGradient(0, board, 0, board + thick, false, CycleMethod.NO_CYCLE,
                            new Stop(0, STEEL_LIGHT), new Stop(1, STEEL)));
                    g.fillRect(left - thick * 0.4, board, plank + thick * 0.8, thick);
                    g.setFill(RIVET);
                    for (double x = left + post; x < right - post; x += plank * 0.18) g.fillOval(x, board + thick * 0.3, thick * 0.35, thick * 0.35);
                }
            }
            case SHELF_BAMBOO -> {
                g.setGlobalAlpha(0.45);
                g.setFill(BAMBOO_DARK);
                g.fillRect(left + post, high, plank - 2 * post, top - high);
                g.setGlobalAlpha(1);
                // Les tiges, avec leurs nœuds.
                for (double x : new double[] {left, right - post}) {
                    g.setFill(BAMBOO);
                    g.fillRoundRect(x, high - thick, post, top + thick * 2 - high, post, post);
                    g.setStroke(BAMBOO_DARK);
                    g.setLineWidth(Math.max(1.5, post * 0.3));
                    for (double y = high + thick * 0.5; y < top + thick; y += thick * 2.2) g.strokeLine(x, y, x + post, y);
                }
                for (double board : boards) {
                    // Des lattes côte à côte, liées de ficelle aux bouts.
                    int slats = 3;
                    for (int k = 0; k < slats; k++) {
                        g.setFill(k % 2 == 0 ? BAMBOO : BAMBOO.darker());
                        g.fillRoundRect(left - thick * 0.4, board + k * thick / slats, plank + thick * 0.8, thick / slats, 2, 2);
                    }
                    g.setStroke(WOOD_DARK);
                    g.setLineWidth(1.2);
                    g.strokeLine(left + post * 1.5, board, left + post * 1.5, board + thick);
                    g.strokeLine(right - post * 1.5, board, right - post * 1.5, board + thick);
                }
            }
            default -> {
                // Des planches de nuage, sans montants : des bosses blanches et une ombre bleutée dessous.
                for (double board : boards) {
                    g.setFill(CLOUD_SHADE);
                    g.setGlobalAlpha(0.8);
                    g.fillRoundRect(left - thick * 0.4, board + thick * 0.3, plank + thick * 0.8, thick * 0.9, thick, thick);
                    g.setGlobalAlpha(1);
                    g.setFill(CLOUD);
                    g.fillRoundRect(left - thick * 0.4, board, plank + thick * 0.8, thick, thick, thick);
                    int puffs = Math.max(3, (int) (plank / (thick * 2.2)));
                    for (int k = 0; k < puffs; k++) {
                        double px = left + plank * (k + 0.5) / puffs, pr = thick * (0.7 + 0.3 * UniverseView.noise(k * 3 + 77));
                        g.fillOval(px - pr, board - pr * 0.8, pr * 2, pr * 1.6);
                    }
                }
            }
        }
        g.setGlobalAlpha(1);
    }

    // ------------------------------------------------------------------
    // Le tuyau
    // ------------------------------------------------------------------

    /**
     * Le tuyau habillé du skin, à l'échelle {@code s} de l'arrosoir : il vient du bord gauche, court au sol à {@code floor} et
     * finit par sa lance en {@code tip}, tournée vers la plante.
     */
    static void hose(GraphicsContext g, double s, double[] tip, double floor, Skin skin) {
        g.setLineCap(StrokeLineCap.ROUND);
        switch (skin) {
            case HOSE_VINE -> {
                g.setStroke(VINE);
                g.setLineWidth(Math.max(3, s * 0.12));
                hosePath(g, s, tip, floor);
                g.stroke();
                // Les feuilles le long de la tige, et la vrille au bout.
                g.setFill(VINE_LIGHT);
                for (int k = 0; k < 6; k++) {
                    double t = (k + 0.5) / 6, x = -s + (tip[0] - s * 0.55 + s) * t, y = floor + (tip[1] + s * 0.05 - floor) * t * t;
                    double side = k % 2 == 0 ? -1 : 1, leaf = s * 0.22;
                    g.fillOval(x - leaf * 0.5, y + side * leaf * 0.9 - leaf * 0.5, leaf, leaf);
                    g.fillPolygon(new double[] {x - leaf * 0.5, x + leaf * 0.5, x}, new double[] {y + side * leaf * 0.9, y + side * leaf * 0.9, y + side * leaf * 1.8}, 3);
                }
                g.setStroke(VINE);
                g.setLineWidth(Math.max(1.5, s * 0.05));
                g.strokeArc(tip[0] - s * 0.1, tip[1] - s * 0.3, s * 0.3, s * 0.3, -90, 300, ArcType.OPEN);
                g.setFill(Color.web("#ff9fd6"));
                g.fillOval(tip[0] + s * 0.3, tip[1] - s * 0.14, s * 0.2, s * 0.2);
            }
            case HOSE_FOUNTAIN -> {
                // La conduite de pierre, au sol, et la vasque sur son pied.
                g.setStroke(STONE);
                g.setLineWidth(Math.max(4, s * 0.18));
                hosePath(g, s, tip, floor);
                g.stroke();
                g.setStroke(STONE_DARK);
                g.setLineWidth(1);
                hosePath(g, s, tip, floor);
                g.stroke();
                g.setFill(STONE);
                g.fillRect(tip[0] + s * 0.05, tip[1] - s * 0.1, s * 0.16, s * 0.4);
                g.fillOval(tip[0] - s * 0.15, tip[1] + s * 0.22, s * 0.56, s * 0.18);
                g.fillArc(tip[0] - s * 0.25, tip[1] - s * 0.35, s * 0.76, s * 0.5, 180, 180, ArcType.ROUND);
                g.setFill(WATER);
                g.setGlobalAlpha(0.8);
                g.fillOval(tip[0] - s * 0.18, tip[1] - s * 0.16, s * 0.62, s * 0.12);
                g.setGlobalAlpha(1);
            }
            case HOSE_CHANNEL -> {
                // La tranchée : un fossé brun, l'eau au fond, et la petite mare au bout.
                g.setStroke(EARTH_DARK);
                g.setLineWidth(Math.max(6, s * 0.26));
                hosePath(g, s, tip, floor);
                g.stroke();
                g.setStroke(EARTH);
                g.setLineWidth(Math.max(4, s * 0.18));
                hosePath(g, s, tip, floor);
                g.stroke();
                g.setStroke(WATER);
                g.setLineWidth(Math.max(2, s * 0.08));
                g.setGlobalAlpha(0.85);
                hosePath(g, s, tip, floor);
                g.stroke();
                g.setFill(EARTH);
                g.fillOval(tip[0] - s * 0.2, tip[1] - s * 0.12, s * 0.75, s * 0.3);
                g.setFill(WATER);
                g.fillOval(tip[0] - s * 0.12, tip[1] - s * 0.07, s * 0.6, s * 0.2);
                g.setGlobalAlpha(1);
            }
            default -> {
                // Le tube de verre, droit au sol, gradué ; la poire rouge à gauche, la pointe effilée à droite.
                double y = tip[1] + s * 0.05, x0 = s * 0.6, x1 = tip[0] - s * 0.1;
                g.setStroke(STONE_DARK);
                g.setLineWidth(1);
                g.setGlobalAlpha(0.6);
                g.strokeLine(-s, floor, x0, y);
                g.setGlobalAlpha(1);
                g.setFill(Color.color(0.87, 0.96, 1, 0.55));
                g.fillRoundRect(x0, y - s * 0.09, x1 - x0, s * 0.18, s * 0.08, s * 0.08);
                g.setFill(WATER);
                g.setGlobalAlpha(0.6);
                g.fillRect(x0 + s * 0.1, y - s * 0.05, (x1 - x0) * 0.6, s * 0.1);
                g.setGlobalAlpha(1);
                g.setStroke(GLASS);
                g.setLineWidth(1.2);
                g.strokeRoundRect(x0, y - s * 0.09, x1 - x0, s * 0.18, s * 0.08, s * 0.08);
                g.setStroke(STONE_DARK);
                g.setLineWidth(1);
                int marks = Math.max(4, (int) ((x1 - x0) / (s * 0.3)));
                for (int k = 1; k < marks; k++) {
                    double mx = x0 + (x1 - x0) * k / marks;
                    g.strokeLine(mx, y - s * 0.09, mx, y - s * (k % 2 == 0 ? 0.02 : 0.05));
                }
                g.setFill(RUBBER);
                g.fillOval(x0 - s * 0.45, y - s * 0.22, s * 0.5, s * 0.44);
                g.setFill(GLASS);
                g.fillPolygon(new double[] {x1, tip[0] + s * 0.5, x1}, new double[] {y - s * 0.09, y, y + s * 0.09}, 3);
            }
        }
    }

    /** Le tracé du tuyau du Classique : du bord gauche, au sol, jusqu'à la lance. */
    private static void hosePath(GraphicsContext g, double s, double[] tip, double floor) {
        g.beginPath();
        g.moveTo(-s, floor);
        g.bezierCurveTo(s * 3, floor + s * 0.05, tip[0] - s * 2.5, floor, tip[0] - s * 0.55, tip[1] + s * 0.05);
    }

    // ------------------------------------------------------------------
    // Le chrono
    // ------------------------------------------------------------------

    /**
     * Le chrono habillé du skin, centré en ({@code x}, {@code y}), de rayon {@code r} : {@code fill} sa part remplie (0 à 1),
     * {@code running} s'il accélère le jeu, {@code hand} l'angle de son aiguille, qui tourne vite quand il tourne.
     */
    static void chrono(GraphicsContext g, double x, double y, double r, double fill, boolean running, double hand, double time, Skin skin) {
        if (running) UniverseView.glow(g, x, y, r * 2.2, CHRONO_TIME, 0.35);
        g.setGlobalAlpha(1);
        switch (skin) {
            case CHRONO_HOURGLASS -> hourglass(g, x, y, r, fill, running, time);
            case CHRONO_POCKET_WATCH -> pocketWatch(g, x, y, r, fill, hand);
            case CHRONO_SUNDIAL -> sundial(g, x, y, r, fill, hand);
            default -> metronome(g, x, y, r, fill, running, hand);
        }
        g.setGlobalAlpha(1);
    }

    private static void hourglass(GraphicsContext g, double x, double y, double r, double fill, boolean running, double time) {
        // Le bâti : deux plateaux de bois et trois colonnettes.
        g.setFill(WOOD);
        g.fillRoundRect(x - r * 0.9, y - r * 1.25, r * 1.8, r * 0.22, 3, 3);
        g.fillRoundRect(x - r * 0.9, y + r * 1.03, r * 1.8, r * 0.22, 3, 3);
        g.setStroke(WOOD_DARK);
        g.setLineWidth(Math.max(1.5, r * 0.08));
        for (double dx : new double[] {-0.8, 0.8}) g.strokeLine(x + dx * r, y - r * 1.05, x + dx * r, y + r * 1.05);
        // Les deux ampoules de verre.
        g.setFill(Color.color(0.87, 0.96, 1, 0.3));
        glassPath(g, x, y, r);
        g.fill();
        // Le sable : en bas à hauteur du temps gardé, en haut ce qu'il en reste à tomber ; le filet quand il tourne.
        g.setFill(SAND);
        double bottom = r * 0.95 * Math.max(0.05, fill);
        g.fillRect(x - r * 0.62, y + r * 1.0 - bottom, r * 1.24, bottom);
        double topSand = r * 0.6 * (1 - fill);
        g.fillPolygon(new double[] {x - r * 0.55 * (1 - fill) - r * 0.1, x + r * 0.55 * (1 - fill) + r * 0.1, x}, new double[] {y - r * 0.1 - topSand, y - r * 0.1 - topSand, y - r * 0.05}, 3);
        if (running) {
            g.setStroke(SAND);
            g.setLineWidth(Math.max(1, r * 0.06));
            g.strokeLine(x, y, x, y + r * 1.0 - bottom);
            g.setFill(SAND);
            for (int k = 0; k < 4; k++) {
                double fallY = y + ((time * 2 + k * 0.25) % 1) * (r * 1.0 - bottom);
                g.fillOval(x - r * 0.05 + Math.sin(k) * r * 0.05, fallY, r * 0.08, r * 0.08);
            }
        }
        g.setStroke(GLASS);
        g.setLineWidth(1.2);
        glassPath(g, x, y, r);
        g.stroke();
    }

    /** Le tracé des deux ampoules du sablier, pincées au milieu. */
    private static void glassPath(GraphicsContext g, double x, double y, double r) {
        g.beginPath();
        g.moveTo(x - r * 0.7, y - r * 1.03);
        g.lineTo(x + r * 0.7, y - r * 1.03);
        g.bezierCurveTo(x + r * 0.7, y - r * 0.3, x + r * 0.1, y - r * 0.15, x + r * 0.08, y);
        g.bezierCurveTo(x + r * 0.1, y + r * 0.15, x + r * 0.7, y + r * 0.3, x + r * 0.7, y + r * 1.03);
        g.lineTo(x - r * 0.7, y + r * 1.03);
        g.bezierCurveTo(x - r * 0.7, y + r * 0.3, x - r * 0.1, y + r * 0.15, x - r * 0.08, y);
        g.bezierCurveTo(x - r * 0.1, y - r * 0.15, x - r * 0.7, y - r * 0.3, x - r * 0.7, y - r * 1.03);
        g.closePath();
    }

    private static void pocketWatch(GraphicsContext g, double x, double y, double r, double fill, double hand) {
        // La chaîne, qui pend à gauche, et la couronne en haut.
        g.setStroke(GOLD);
        g.setLineWidth(1.5);
        g.setLineDashes(2, 3);
        g.beginPath();
        g.moveTo(x - r * 0.3, y - r * 1.1);
        g.bezierCurveTo(x - r * 1.2, y - r * 1.6, x - r * 1.8, y - r * 0.6, x - r * 1.5, y + r * 0.3);
        g.stroke();
        g.setLineDashes();
        g.setFill(GOLD);
        g.fillRect(x - r * 0.14, y - r * 1.3, r * 0.28, r * 0.3);
        g.fillOval(x - r * 0.2, y - r * 1.42, r * 0.4, r * 0.2);
        g.fillOval(x - r * 0.1, y - r * 1.55, r * 0.2, r * 0.2);
        // Le boîtier d'or et le cadran blanc ; le temps gardé en bleu pâle sur le cadran.
        g.setFill(GOLD);
        g.fillOval(x - r, y - r, 2 * r, 2 * r);
        g.setFill(DIAL);
        g.fillOval(x - r * 0.85, y - r * 0.85, r * 1.7, r * 1.7);
        g.setFill(CHRONO_TIME);
        g.setGlobalAlpha(0.6);
        g.fillArc(x - r * 0.85, y - r * 0.85, r * 1.7, r * 1.7, 90, -360 * fill, ArcType.ROUND);
        g.setGlobalAlpha(1);
        g.setStroke(GOLD_DARK);
        g.setLineWidth(Math.max(1, r * 0.05));
        for (int k = 0; k < 12; k++) {
            double a = Math.PI * 2 * k / 12, in = k % 3 == 0 ? 0.65 : 0.74;
            g.strokeLine(x + Math.cos(a) * r * in, y + Math.sin(a) * r * in, x + Math.cos(a) * r * 0.82, y + Math.sin(a) * r * 0.82);
        }
        g.setStroke(GOLD_DARK);
        g.setLineWidth(Math.max(1.5, r * 0.08));
        g.setLineCap(StrokeLineCap.ROUND);
        double a = hand - Math.PI / 2;
        g.strokeLine(x, y, x + Math.cos(a) * r * 0.65, y + Math.sin(a) * r * 0.65);
        g.setLineWidth(Math.max(1, r * 0.05));
        g.strokeLine(x, y, x + Math.cos(a * 0.4) * r * 0.45, y + Math.sin(a * 0.4) * r * 0.45);
        g.setFill(GOLD_DARK);
        g.fillOval(x - r * 0.08, y - r * 0.08, r * 0.16, r * 0.16);
    }

    private static void sundial(GraphicsContext g, double x, double y, double r, double fill, double hand) {
        // Le disque de pierre, le secteur éclairé à hauteur du temps gardé, les heures gravées.
        g.setFill(STONE);
        g.fillOval(x - r, y - r, 2 * r, 2 * r);
        g.setFill(SAND);
        g.setGlobalAlpha(0.55);
        g.fillArc(x - r * 0.9, y - r * 0.9, r * 1.8, r * 1.8, 180, -180 * fill, ArcType.ROUND);
        g.setGlobalAlpha(1);
        g.setStroke(STONE_DARK);
        g.setLineWidth(Math.max(1, r * 0.05));
        for (int k = 0; k <= 12; k++) {
            double a = Math.PI + Math.PI * k / 12;
            g.strokeLine(x + Math.cos(a) * r * 0.72, y + Math.sin(a) * r * 0.72, x + Math.cos(a) * r * 0.88, y + Math.sin(a) * r * 0.88);
        }
        g.setLineWidth(1.5);
        g.strokeOval(x - r, y - r, 2 * r, 2 * r);
        g.strokeLine(x - r * 0.9, y, x + r * 0.9, y);
        // L'ombre du style, qui tourne comme l'aiguille, puis le style lui-même.
        double a = Math.PI + (hand % (Math.PI * 2)) / 2;
        g.setStroke(Color.color(0, 0, 0, 0.55));
        g.setLineWidth(Math.max(2, r * 0.1));
        g.setLineCap(StrokeLineCap.ROUND);
        g.strokeLine(x, y, x + Math.cos(a) * r * 0.8, y + Math.sin(a) * Math.abs(Math.sin(a)) * r * 0.8);
        g.setFill(GOLD_DARK);
        g.fillPolygon(new double[] {x - r * 0.06, x + r * 0.06, x}, new double[] {y, y, y - r * 0.55}, 3);
    }

    private static void metronome(GraphicsContext g, double x, double y, double r, double fill, boolean running, double hand) {
        // La pyramide de bois, et la fente où bat le balancier.
        g.setFill(WOOD);
        g.fillPolygon(new double[] {x - r * 0.45, x + r * 0.45, x + r * 0.85, x - r * 0.85}, new double[] {y - r * 1.1, y - r * 1.1, y + r * 1.0, y + r * 1.0}, 4);
        g.setFill(WOOD_DARK);
        g.fillRect(x - r * 0.08, y - r * 0.95, r * 0.16, r * 1.5);
        g.fillRect(x - r * 0.85, y + r * 1.0, r * 1.7, r * 0.18);
        // La jauge de la réserve, sur le flanc droit, en bleu.
        g.setFill(STONE_DARK);
        g.fillRect(x + r * 0.5, y - r * 0.4, r * 0.12, r * 1.2);
        g.setFill(CHRONO_TIME);
        g.fillRect(x + r * 0.5, y + r * 0.8 - r * 1.2 * fill, r * 0.12, r * 1.2 * fill);
        // Le balancier : il bat quand le chrono tourne, reste droit sinon ; son poids se règle sur la tige.
        double swing = running ? Math.sin(hand) * 0.45 : 0;
        double px = x + Math.sin(swing) * r * 1.3, py = y + r * 0.55 - Math.cos(swing) * r * 1.3;
        g.setStroke(GOLD);
        g.setLineWidth(Math.max(1.5, r * 0.07));
        g.setLineCap(StrokeLineCap.ROUND);
        g.strokeLine(x, y + r * 0.55, px, py);
        g.setFill(GOLD);
        double wx = x + Math.sin(swing) * r * 0.8, wy = y + r * 0.55 - Math.cos(swing) * r * 0.8;
        g.fillPolygon(new double[] {wx - r * 0.14, wx + r * 0.14, wx + r * 0.1, wx - r * 0.1}, new double[] {wy - r * 0.08, wy - r * 0.08, wy + r * 0.12, wy + r * 0.12}, 4);
        g.setStroke(SEAM);
        g.setLineWidth(1);
        for (int k = 0; k < 5; k++) g.strokeLine(x - r * 0.14, y - r * 0.8 + k * r * 0.3, x + r * 0.14, y - r * 0.8 + k * r * 0.3);
    }
}
