package idle.ui.darkmatter;

import idle.core.universe.Skin;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.text.TextAlignment;

/**
 * Les skins de l'échelle des grandeurs (version 0.193), dessinés à côté de {@link DarkMatterView} pour la garder
 * lisible : la règle graduée en bas à gauche, et les cercles des objets connus.
 * <ul>
 * <li>« Règle de bois » : une latte brune au grain visible, dix graduations gravées, le texte brûlé au fer ;</li>
 * <li>« Règle d'architecte » : une réglette de plastique jaune translucide, biseautée, aux traits noirs fins ;</li>
 * <li>« Repères célestes » : les cercles des objets connus deviennent les objets eux-mêmes, en silhouette : le grain
 * de sable, la balle, la Terre, le Soleil, la Voie lactée… ({@link #silhouette}).</li>
 * </ul>
 */
final class RulerSkinArt {

    private static final Color WOOD = Color.web("#8a6440");
    private static final Color WOOD_GRAIN = Color.web("#6e4c2e");
    private static final Color WOOD_EDGE = Color.web("#4a3018");
    private static final Color BURN = Color.web("#2a1808");
    private static final Color BURN_INK = Color.web("#e8c89a");
    private static final Color PLASTIC = Color.web("#ffd84a");
    private static final Color PLASTIC_LIGHT = Color.web("#fff1a8");
    private static final Color PLASTIC_DARK = Color.web("#b89a2a");
    private static final Color GLOW = Color.web("#c9a6ff");
    private static final Color CORE = Color.web("#efe4ff");

    /** Hauteur de la latte de bois et de la réglette, en pixels. */
    private static final double WOOD_HEIGHT = 14;
    private static final double PLASTIC_HEIGHT = 16;

    private RulerSkinArt() {
    }

    /** Vrai pour les skins qui redessinent la règle elle-même : le bois et le plastique ; les repères célestes la laissent. */
    static boolean drawsRuler(Skin ruler) {
        return ruler == Skin.RULER_WOOD || ruler == Skin.RULER_ARCHITECT;
    }

    /**
     * La règle sous un skin : de {@code x} à {@code x + length}, centrée sur {@code y}, avec son texte au-dessus.
     *
     * @param length la longueur de la règle, en pixels (de 24 à 120)
     * @param text   ce que vaut cette longueur, déjà mis en mots
     */
    static void drawRuler(Skin ruler, GraphicsContext g, double x, double y, double length, String text) {
        g.setGlobalAlpha(1);
        g.setTextAlign(TextAlignment.LEFT);
        if (ruler == Skin.RULER_WOOD) {
            double top = y - WOOD_HEIGHT / 2;
            g.setFill(WOOD);
            g.fillRect(x, top, length, WOOD_HEIGHT);
            // Le grain : trois veines qui ondulent le long de la latte.
            g.setStroke(WOOD_GRAIN);
            g.setLineWidth(1);
            for (int vein = 0; vein < 3; vein++) {
                double vy = top + 3 + vein * 4;
                g.beginPath();
                g.moveTo(x, vy);
                for (double vx = x; vx <= x + length; vx += 6) g.lineTo(vx, vy + Math.sin(vx * 0.4 + vein * 2) * 0.8);
                g.stroke();
            }
            g.setStroke(WOOD_EDGE);
            g.setLineWidth(1.2);
            g.strokeRect(x, top, length, WOOD_HEIGHT);
            // Dix graduations gravées, plus longues aux bouts et au milieu.
            g.setStroke(BURN);
            for (int tick = 0; tick <= 10; tick++) {
                double tx = x + length * tick / 10;
                double depth = tick == 0 || tick == 10 ? WOOD_HEIGHT : tick == 5 ? 9 : 5;
                g.strokeLine(tx, top, tx, top + depth);
            }
            // Le texte brûlé au fer : une ombre sombre, puis la marque claire.
            g.setFill(BURN);
            g.fillText(text, x + 1, top - 4);
            g.setFill(BURN_INK);
            g.fillText(text, x, top - 5);
        } else {
            double top = y - PLASTIC_HEIGHT / 2;
            g.setGlobalAlpha(0.75);
            g.setFill(PLASTIC);
            g.fillRect(x, top, length, PLASTIC_HEIGHT);
            g.setGlobalAlpha(1);
            // Le biseau : un trait clair en haut, un trait sombre en bas.
            g.setLineWidth(1.5);
            g.setStroke(PLASTIC_LIGHT);
            g.strokeLine(x, top + 0.75, x + length, top + 0.75);
            g.setStroke(PLASTIC_DARK);
            g.strokeLine(x, top + PLASTIC_HEIGHT - 0.75, x + length, top + PLASTIC_HEIGHT - 0.75);
            // Vingt traits noirs fins : les dixièmes plus longs, les demis plus courts.
            g.setStroke(Color.BLACK);
            g.setLineWidth(0.8);
            for (int tick = 0; tick <= 20; tick++) {
                double tx = x + length * tick / 20;
                double depth = tick % 10 == 0 ? PLASTIC_HEIGHT - 2 : tick % 2 == 0 ? 7 : 4;
                g.strokeLine(tx, top + PLASTIC_HEIGHT - 1, tx, top + PLASTIC_HEIGHT - 1 - depth);
            }
            g.setFill(Color.BLACK);
            g.fillText(text, x + 3, top + 10);
            g.setFill(PLASTIC_LIGHT);
            g.fillText(text, x, top - 5);
        }
        g.setGlobalAlpha(1);
    }

    /**
     * L'objet connu lui-même, en silhouette violette translucide, à la place de son cercle : la forme dépend de
     * son nom ({@link idle.core.darkmatter.SizeScale#LANDMARKS}), un disque uni pour un nom inconnu.
     *
     * @param radius le rayon qu'aurait son cercle, en pixels
     * @param time   le temps de la vue, pour un lent balancement
     */
    static void silhouette(GraphicsContext g, double cx, double cy, double radius, String name, double time) {
        double r = radius;
        g.setLineWidth(1.2);
        g.setStroke(GLOW);
        g.setFill(GLOW);
        g.setGlobalAlpha(0.14);
        switch (name) {
            case "proton", "noyau d'uranium" -> {
                // Des boules serrées : trois pour le proton, bien plus pour le noyau.
                int balls = name.equals("proton") ? 3 : 12;
                for (int k = 0; k < balls; k++) {
                    double a = k * 2.39996, d = balls == 3 ? r * 0.42 : r * 0.62 * Math.sqrt((k + 0.5) / balls);
                    double s = balls == 3 ? r * 0.55 : r * 0.3;
                    g.fillOval(cx + d * Math.cos(a) - s, cy + d * Math.sin(a) - s, 2 * s, 2 * s);
                    g.setGlobalAlpha(0.6);
                    g.strokeOval(cx + d * Math.cos(a) - s, cy + d * Math.sin(a) - s, 2 * s, 2 * s);
                    g.setGlobalAlpha(0.14);
                }
            }
            case "atome d'hydrogène" -> {
                double core = Math.max(2, r * 0.08);
                g.setGlobalAlpha(0.8);
                g.fillOval(cx - core, cy - core, 2 * core, 2 * core);
                g.setGlobalAlpha(0.6);
                g.strokeOval(cx - r, cy - r * 0.35, 2 * r, r * 0.7);
                g.strokeOval(cx - r * 0.35, cy - r, r * 0.7, 2 * r);
                double a = time * 0.8;
                g.setGlobalAlpha(0.9);
                g.fillOval(cx + r * Math.cos(a) - 2, cy + r * 0.35 * Math.sin(a) - 2, 4, 4);
            }
            case "hélice d'ADN" -> {
                g.setGlobalAlpha(0.6);
                for (int strand = 0; strand < 2; strand++) {
                    g.beginPath();
                    for (int k = 0; k <= 40; k++) {
                        double u = k / 40.0, px = cx - r + 2 * r * u, py = cy + r * 0.3 * Math.sin(u * 4 * Math.PI + strand * Math.PI);
                        if (k == 0) g.moveTo(px, py); else g.lineTo(px, py);
                    }
                    g.stroke();
                }
                for (int k = 1; k < 10; k++) {
                    double u = k / 10.0, px = cx - r + 2 * r * u, dy = r * 0.3 * Math.sin(u * 4 * Math.PI);
                    g.strokeLine(px, cy + dy, px, cy - dy);
                }
            }
            case "virus" -> {
                double body = r * 0.7;
                g.fillOval(cx - body, cy - body, 2 * body, 2 * body);
                g.setGlobalAlpha(0.6);
                g.strokeOval(cx - body, cy - body, 2 * body, 2 * body);
                for (int k = 0; k < 16; k++) {
                    double a = k * Math.PI / 8;
                    g.strokeLine(cx + body * Math.cos(a), cy + body * Math.sin(a), cx + r * Math.cos(a), cy + r * Math.sin(a));
                    g.fillOval(cx + r * Math.cos(a) - 2, cy + r * Math.sin(a) - 2, 4, 4);
                }
            }
            case "bactérie" -> {
                double h = r * 0.5;
                g.fillRoundRect(cx - r * 0.8, cy - h / 2, r * 1.6, h, h, h);
                g.setGlobalAlpha(0.6);
                g.strokeRoundRect(cx - r * 0.8, cy - h / 2, r * 1.6, h, h, h);
                // Les flagelles, qui ondulent.
                for (int k = -1; k <= 1; k += 2) {
                    g.beginPath();
                    g.moveTo(cx + r * 0.8, cy + k * h * 0.2);
                    for (int s = 1; s <= 8; s++) g.lineTo(cx + r * 0.8 + s * r * 0.025, cy + k * h * 0.2 + Math.sin(s * 1.3 + time * 3) * h * 0.15);
                    g.stroke();
                }
            }
            case "globule rouge" -> {
                g.fillOval(cx - r, cy - r, 2 * r, 2 * r);
                g.setGlobalAlpha(0.6);
                g.strokeOval(cx - r, cy - r, 2 * r, 2 * r);
                g.strokeOval(cx - r * 0.45, cy - r * 0.45, r * 0.9, r * 0.9);
            }
            case "cheveu" -> {
                g.setGlobalAlpha(0.7);
                g.setLineWidth(Math.max(1.5, r * 0.08));
                g.beginPath();
                for (int k = 0; k <= 30; k++) {
                    double u = k / 30.0, px = cx - r + 2 * r * u, py = cy + Math.sin(u * 3 + time * 0.5) * r * 0.25;
                    if (k == 0) g.moveTo(px, py); else g.lineTo(px, py);
                }
                g.stroke();
            }
            case "grain de sable" -> {
                int sides = 7;
                double[] xs = new double[sides], ys = new double[sides];
                for (int k = 0; k < sides; k++) {
                    double a = 2 * Math.PI * k / sides, rr = r * (0.75 + 0.25 * Math.sin(k * 2.3 + 1));
                    xs[k] = cx + rr * Math.cos(a);
                    ys[k] = cy + rr * Math.sin(a);
                }
                g.fillPolygon(xs, ys, sides);
                g.setGlobalAlpha(0.6);
                g.strokePolygon(xs, ys, sides);
            }
            case "fourmi" -> {
                double[] parts = {-0.6, 0, 0.55}, sizes = {0.3, 0.2, 0.4};
                for (int k = 0; k < 3; k++) {
                    double px = cx + parts[k] * r, s = sizes[k] * r;
                    g.fillOval(px - s, cy - s * 0.75, 2 * s, 1.5 * s);
                    g.setGlobalAlpha(0.6);
                    g.strokeOval(px - s, cy - s * 0.75, 2 * s, 1.5 * s);
                    g.setGlobalAlpha(0.14);
                }
                g.setGlobalAlpha(0.6);
                for (int k = -1; k <= 1; k++) {
                    for (int side = -1; side <= 1; side += 2) {
                        g.strokeLine(cx + k * r * 0.2, cy, cx + k * r * 0.35, cy + side * r * 0.6);
                    }
                }
            }
            case "balle de tennis" -> {
                g.fillOval(cx - r, cy - r, 2 * r, 2 * r);
                g.setGlobalAlpha(0.6);
                g.strokeOval(cx - r, cy - r, 2 * r, 2 * r);
                g.setLineWidth(2);
                g.strokeArc(cx - r * 1.6, cy - r, r * 1.6, 2 * r, -60, 120, ArcType.OPEN);
                g.strokeArc(cx, cy - r, r * 1.6, 2 * r, 120, 120, ArcType.OPEN);
            }
            case "être humain" -> {
                double head = r * 0.14;
                g.setGlobalAlpha(0.6);
                g.fillOval(cx - head, cy - r, 2 * head, 2 * head);
                g.setLineWidth(Math.max(2, r * 0.06));
                g.strokeLine(cx, cy - r + 2 * head, cx, cy + r * 0.15);
                g.strokeLine(cx - r * 0.35, cy - r * 0.2, cx + r * 0.35, cy - r * 0.2);
                g.strokeLine(cx, cy + r * 0.15, cx - r * 0.25, cy + r);
                g.strokeLine(cx, cy + r * 0.15, cx + r * 0.25, cy + r);
            }
            case "baleine bleue" -> {
                g.fillOval(cx - r * 0.9, cy - r * 0.3, r * 1.5, r * 0.6);
                g.fillPolygon(new double[] {cx + r * 0.55, cx + r, cx + r}, new double[] {cy, cy - r * 0.35, cy + r * 0.35}, 3);
                g.setGlobalAlpha(0.6);
                g.strokeOval(cx - r * 0.9, cy - r * 0.3, r * 1.5, r * 0.6);
                g.strokePolygon(new double[] {cx + r * 0.55, cx + r, cx + r}, new double[] {cy, cy - r * 0.35, cy + r * 0.35}, 3);
            }
            case "tour Eiffel" -> {
                g.setGlobalAlpha(0.6);
                g.strokeLine(cx - r * 0.5, cy + r, cx - r * 0.05, cy - r);
                g.strokeLine(cx + r * 0.5, cy + r, cx + r * 0.05, cy - r);
                g.strokeArc(cx - r * 0.5, cy + r * 0.2, r, r * 0.8, 0, 180, ArcType.OPEN);
                for (double level : new double[] {0.55, 0.1, -0.5}) {
                    double half = r * 0.5 * (level + 1) / 2;
                    g.strokeLine(cx - half, cy + level * r, cx + half, cy + level * r);
                }
            }
            case "mont Everest" -> {
                g.fillPolygon(new double[] {cx - r, cx - r * 0.1, cx + r}, new double[] {cy + r * 0.8, cy - r, cy + r * 0.8}, 3);
                g.setGlobalAlpha(0.6);
                g.strokePolygon(new double[] {cx - r, cx - r * 0.1, cx + r}, new double[] {cy + r * 0.8, cy - r, cy + r * 0.8}, 3);
                g.setFill(CORE);
                g.fillPolygon(new double[] {cx - r * 0.3, cx - r * 0.1, cx + r * 0.12, cx + r * 0.02, cx - r * 0.12},
                        new double[] {cy - r * 0.55, cy - r, cy - r * 0.55, cy - r * 0.6, cy - r * 0.48}, 5);
            }
            case "Lune" -> {
                g.fillOval(cx - r, cy - r, 2 * r, 2 * r);
                g.setGlobalAlpha(0.6);
                g.strokeOval(cx - r, cy - r, 2 * r, 2 * r);
                for (int k = 0; k < 6; k++) {
                    double a = k * 1.9 + 0.4, d = r * (0.2 + 0.5 * ((k * 0.618034) % 1)), s = r * (0.06 + 0.1 * ((k * 0.381966) % 1));
                    g.strokeOval(cx + d * Math.cos(a) - s, cy + d * Math.sin(a) - s, 2 * s, 2 * s);
                }
            }
            case "Terre" -> {
                g.fillOval(cx - r, cy - r, 2 * r, 2 * r);
                g.setGlobalAlpha(0.6);
                g.strokeOval(cx - r, cy - r, 2 * r, 2 * r);
                // Les continents : des taches aux bords irréguliers.
                g.setGlobalAlpha(0.3);
                for (int c = 0; c < 4; c++) {
                    double a = c * 1.6 + 0.3, d = r * 0.45;
                    double px = cx + d * Math.cos(a), py = cy + d * Math.sin(a);
                    int sides = 8;
                    double[] xs = new double[sides], ys = new double[sides];
                    for (int k = 0; k < sides; k++) {
                        double b = 2 * Math.PI * k / sides, rr = r * (0.22 + 0.14 * Math.sin(k * 2.1 + c));
                        xs[k] = px + rr * Math.cos(b) * 1.4;
                        ys[k] = py + rr * Math.sin(b);
                    }
                    g.fillPolygon(xs, ys, sides);
                }
            }
            case "Jupiter" -> {
                g.fillOval(cx - r, cy - r, 2 * r, 2 * r);
                g.setGlobalAlpha(0.6);
                g.strokeOval(cx - r, cy - r, 2 * r, 2 * r);
                g.setGlobalAlpha(0.35);
                for (double band : new double[] {-0.65, -0.35, 0, 0.3, 0.6}) {
                    double half = Math.sqrt(Math.max(0, 1 - band * band)) * r;
                    g.setLineWidth(Math.max(2, r * 0.07));
                    g.strokeLine(cx - half, cy + band * r, cx + half, cy + band * r);
                }
                g.fillOval(cx + r * 0.25, cy + r * 0.3, r * 0.3, r * 0.18);
            }
            case "Soleil" -> {
                g.setGlobalAlpha(0.25);
                g.fillOval(cx - r * 0.8, cy - r * 0.8, r * 1.6, r * 1.6);
                g.setGlobalAlpha(0.6);
                g.strokeOval(cx - r * 0.8, cy - r * 0.8, r * 1.6, r * 1.6);
                for (int k = 0; k < 12; k++) {
                    double a = k * Math.PI / 6 + time * 0.1, len = k % 2 == 0 ? r : r * 0.92;
                    g.strokeLine(cx + r * 0.84 * Math.cos(a), cy + r * 0.84 * Math.sin(a), cx + len * Math.cos(a), cy + len * Math.sin(a));
                }
            }
            case "distance de la Terre au Soleil" -> {
                g.setGlobalAlpha(0.6);
                g.strokeLine(cx - r, cy, cx + r, cy);
                double sun = Math.max(4, r * 0.09), earth = Math.max(2.5, r * 0.03);
                g.fillOval(cx - r - sun, cy - sun, 2 * sun, 2 * sun);
                g.fillOval(cx + r - earth, cy - earth, 2 * earth, 2 * earth);
                g.strokeOval(cx - r, cy - r, 2 * r, 2 * r);
            }
            case "système solaire" -> {
                g.setGlobalAlpha(0.5);
                for (int k = 1; k <= 5; k++) {
                    double rr = r * k / 5;
                    g.strokeOval(cx - rr, cy - rr, 2 * rr, 2 * rr);
                    double a = time * 0.3 / k + k;
                    g.fillOval(cx + rr * Math.cos(a) - 2.5, cy + rr * Math.sin(a) - 2.5, 5, 5);
                }
                g.setGlobalAlpha(0.9);
                g.fillOval(cx - 4, cy - 4, 8, 8);
            }
            case "nuage de Oort" -> {
                g.setGlobalAlpha(0.6);
                for (int k = 0; k < 90; k++) {
                    double a = k * 2.39996, d = r * (0.75 + 0.25 * ((k * 0.618034) % 1));
                    g.fillOval(cx + d * Math.cos(a) - 1.2, cy + d * Math.sin(a) - 1.2, 2.4, 2.4);
                }
            }
            case "distance à l'étoile la plus proche" -> {
                g.setGlobalAlpha(0.6);
                g.setLineDashes(6, 6);
                g.strokeLine(cx - r, cy, cx + r, cy);
                g.setLineDashes();
                star(g, cx - r, cy, Math.max(5, r * 0.08));
                star(g, cx + r, cy, Math.max(4, r * 0.05));
                g.strokeOval(cx - r, cy - r, 2 * r, 2 * r);
            }
            case "Voie lactée" -> spiral(g, cx, cy, r, time);
            case "distance à la galaxie d'Andromède" -> {
                g.setGlobalAlpha(0.6);
                g.setLineDashes(6, 6);
                g.strokeLine(cx - r, cy, cx + r, cy);
                g.setLineDashes();
                g.strokeOval(cx - r, cy - r, 2 * r, 2 * r);
                spiral(g, cx - r, cy, Math.max(6, r * 0.1), time);
                spiral(g, cx + r, cy, Math.max(7, r * 0.12), -time);
            }
            case "superamas de la Vierge" -> {
                g.setGlobalAlpha(0.6);
                for (int k = 0; k < 14; k++) {
                    double a = k * 2.39996, d = r * 0.85 * Math.sqrt((k + 0.5) / 14);
                    spiral(g, cx + d * Math.cos(a), cy + d * Math.sin(a), Math.max(3, r * (0.05 + 0.04 * ((k * 0.618034) % 1))), time + k);
                }
            }
            case "univers observable" -> {
                g.setGlobalAlpha(0.6);
                g.strokeOval(cx - r, cy - r, 2 * r, 2 * r);
                // La toile cosmique : des points reliés par des fils.
                double[] xs = new double[24], ys = new double[24];
                for (int k = 0; k < 24; k++) {
                    double a = k * 2.39996, d = r * 0.92 * Math.sqrt((k + 0.5) / 24);
                    xs[k] = cx + d * Math.cos(a);
                    ys[k] = cy + d * Math.sin(a);
                    g.fillOval(xs[k] - 1.5, ys[k] - 1.5, 3, 3);
                }
                g.setGlobalAlpha(0.3);
                for (int k = 1; k < 24; k++) g.strokeLine(xs[k - 1], ys[k - 1], xs[k], ys[k]);
            }
            default -> {
                g.fillOval(cx - r, cy - r, 2 * r, 2 * r);
                g.setGlobalAlpha(0.6);
                g.strokeOval(cx - r, cy - r, 2 * r, 2 * r);
            }
        }
        g.setLineWidth(1);
        g.setGlobalAlpha(1);
    }

    /** Une étoile à quatre pointes. */
    private static void star(GraphicsContext g, double x, double y, double size) {
        g.fillPolygon(new double[] {x, x + size * 0.25, x + size, x + size * 0.25, x, x - size * 0.25, x - size, x - size * 0.25},
                new double[] {y - size, y - size * 0.25, y, y + size * 0.25, y + size, y + size * 0.25, y, y - size * 0.25}, 8);
    }

    /** Une galaxie spirale à deux bras, qui tourne lentement. */
    private static void spiral(GraphicsContext g, double x, double y, double size, double time) {
        g.setGlobalAlpha(0.6);
        for (int arm = 0; arm < 2; arm++) {
            g.beginPath();
            for (int k = 0; k <= 24; k++) {
                double u = k / 24.0, a = arm * Math.PI + u * 3.2 + time * 0.05, d = size * u;
                if (k == 0) g.moveTo(x + d * Math.cos(a), y + d * Math.sin(a) * 0.7);
                else g.lineTo(x + d * Math.cos(a), y + d * Math.sin(a) * 0.7);
            }
            g.stroke();
        }
        double core = Math.max(1.5, size * 0.15);
        g.fillOval(x - core, y - core, 2 * core, 2 * core);
    }
}
