package idle.ui.bigbang;

import idle.core.universe.Skin;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;

/**
 * Les six skins du Big Bang de la version 0.191, dessinés à côté de {@link SpaceView} pour la garder
 * lisible : le vide, le ciel de toute la vue, l'espace créé, son bord et les étoiles.
 * <ul>
 * <li>« Encre de Chine » : un fond blanc cassé aux fibres de papier, des étoiles en points d'encre, l'espace en
 * lavis gris, le bord d'un coup de pinceau irrégulier ;</li>
 * <li>« Vitrail » : un fond noir, le ciel en verres colorés cernés de plomb, l'espace en rosace ;</li>
 * <li>« Abysses » : un bleu très sombre, des rais de lumière qui tombent d'en haut, des étoiles en plancton
 * lumineux, l'espace en méduse qui pulse ;</li>
 * <li>« Papier plié » : une feuille crème aux plis visibles, l'espace à facettes planes, des étoiles en confettis ;</li>
 * <li>« Carte ancienne » : du parchemin taché, des lignes de rhumb, les étoiles reliées en constellations, l'espace
 * en rose des vents ;</li>
 * <li>« Néon de ville » : l'asphalte mouillé où se reflètent les enseignes, l'espace en enseigne qui clignote,
 * les étoiles en fenêtres allumées.</li>
 * </ul>
 */
final class SpaceSkinArt {

    private static final Color INK = Color.web("#1a1a1e");
    private static final Color PAPER = Color.web("#f4efe3");
    private static final Color LEAD = Color.web("#2a2a2e");
    private static final Color[] GLASS = {Color.web("#d62839"), Color.web("#f4a259"), Color.web("#f7d354"),
        Color.web("#4fa36b"), Color.web("#2f6fb5"), Color.web("#7a3fa0")};
    private static final Color PLANKTON = Color.web("#6bffd0");
    private static final Color JELLY = Color.web("#b08aff");
    private static final Color FOLD = Color.web("#a08a68");
    private static final Color PARCHMENT = Color.web("#e8d7b0");
    private static final Color SEPIA = Color.web("#5a3a1a");
    private static final Color PINK = Color.web("#ff4fd8");
    private static final Color CYAN = Color.web("#3ff0ff");
    private static final Color WINDOW = Color.web("#ffd27f");

    private SpaceSkinArt() {
    }

    /** Vrai pour les skins du Big Bang dessinés ici : ceux de la version 0.191. */
    static boolean draws(Skin skin) {
        return skin == Skin.BANG_INK || skin == Skin.BANG_STAINED || skin == Skin.BANG_ABYSS
                || skin == Skin.BANG_ORIGAMI || skin == Skin.BANG_MAP || skin == Skin.BANG_NEON;
    }

    /** Un nombre entre 0 et 1, toujours le même pour une même graine. */
    private static double fraction(double seed) {
        double value = Math.sin(seed) * 43758.5453;
        return value - Math.floor(value);
    }

    private static Color tint(Color c, double alpha) {
        return Color.color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(1, alpha)));
    }

    /** La couleur du vide sous ce skin. */
    static Color voidColor(Skin look) {
        return switch (look) {
            case BANG_INK -> PAPER;
            case BANG_STAINED -> Color.BLACK;
            case BANG_ABYSS -> Color.web("#020814");
            case BANG_ORIGAMI -> Color.web("#f1e9d8");
            case BANG_MAP -> PARCHMENT;
            default -> Color.web("#15171a");
        };
    }

    /** Le ciel de toute la vue, par-dessus le vide. */
    static void sky(GraphicsContext g, double w, double h, Skin look, double time) {
        double small = Math.min(w, h);
        switch (look) {
            case BANG_INK -> {
                // Les fibres du papier : de longs traits à peine visibles.
                g.setStroke(Color.web("#c9c0ae"));
                g.setLineWidth(0.8);
                for (int k = 0; k < 60; k++) {
                    double x = fraction(k * 1.31) * w, y = fraction(k * 2.77) * h;
                    double a = (fraction(k * 4.1) - 0.5) * 0.6, len = small * (0.05 + 0.15 * fraction(k * 5.3));
                    g.setGlobalAlpha(0.25 + 0.3 * fraction(k * 7.7));
                    g.strokeLine(x, y, x + len * Math.cos(a), y + len * Math.sin(a));
                }
            }
            case BANG_STAINED -> {
                // Des verres en quadrillage irrégulier, chacun de sa couleur, cernés de plomb.
                double cell = Math.max(40, small / 6);
                int columns = (int) Math.ceil(w / cell) + 1, rows = (int) Math.ceil(h / cell) + 1;
                for (int i = 0; i < columns; i++) {
                    for (int j = 0; j < rows; j++) {
                        double[] xs = new double[4], ys = new double[4];
                        for (int c = 0; c < 4; c++) {
                            int ci = i + (c == 1 || c == 2 ? 1 : 0), cj = j + (c >= 2 ? 1 : 0);
                            xs[c] = ci * cell + (fraction(ci * 3.1 + cj * 7.3) - 0.5) * cell * 0.5;
                            ys[c] = cj * cell + (fraction(ci * 5.7 + cj * 1.9) - 0.5) * cell * 0.5;
                        }
                        Color glass = GLASS[(int) (fraction(i * 2.3 + j * 4.7) * GLASS.length)];
                        double light = 0.18 + 0.1 * Math.sin(time * 0.3 + i * 1.7 + j * 2.3);
                        g.setGlobalAlpha(light);
                        g.setFill(glass);
                        g.fillPolygon(xs, ys, 4);
                        g.setGlobalAlpha(0.9);
                        g.setStroke(LEAD);
                        g.setLineWidth(Math.max(2, cell * 0.05));
                        g.strokePolygon(xs, ys, 4);
                    }
                }
            }
            case BANG_ABYSS -> {
                g.setGlobalAlpha(1);
                g.setFill(new LinearGradient(0, 0, 0, h, false, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.web("#0a2a4a")), new Stop(0.5, Color.web("#041426")), new Stop(1, Color.web("#010610"))));
                g.fillRect(0, 0, w, h);
                // Les rais de lumière qui tombent de la surface et ondulent.
                for (int k = 0; k < 9; k++) {
                    double x = w * (0.05 + 0.9 * fraction(k * 1.7)) + Math.sin(time * 0.2 + k) * w * 0.03;
                    double wide = w * (0.02 + 0.04 * fraction(k * 3.3));
                    g.setGlobalAlpha(0.06 + 0.05 * Math.sin(time * 0.5 + k * 2));
                    g.setFill(Color.web("#7fd8ff"));
                    g.fillPolygon(new double[] {x - wide, x + wide, x + wide * 3, x - wide * 3}, new double[] {0, 0, h * 0.7, h * 0.7}, 4);
                }
            }
            case BANG_ORIGAMI -> {
                // Les plis : des diagonales qui partagent la feuille en facettes, les unes à l'ombre, les autres au jour.
                double cell = Math.max(60, small / 4);
                int columns = (int) Math.ceil(w / cell) + 1, rows = (int) Math.ceil(h / cell) + 1;
                for (int i = 0; i < columns; i++) {
                    for (int j = 0; j < rows; j++) {
                        boolean slash = fraction(i * 2.9 + j * 6.1) > 0.5;
                        double x = i * cell, y = j * cell;
                        for (int half = 0; half < 2; half++) {
                            double shade = 0.9 + 0.08 * ((half + i + j) % 2 == 0 ? 1 : -1) * (0.6 + 0.4 * fraction(i * 7.1 + j * 3.3 + half));
                            g.setGlobalAlpha(0.6);
                            g.setFill(Color.color(Math.min(1, shade * 0.98), Math.min(1, shade * 0.93), Math.min(1, shade * 0.84)));
                            double[] xs = slash ? (half == 0 ? new double[] {x, x + cell, x} : new double[] {x + cell, x + cell, x})
                                    : (half == 0 ? new double[] {x, x + cell, x + cell} : new double[] {x, x + cell, x});
                            double[] ys = slash ? (half == 0 ? new double[] {y, y, y + cell} : new double[] {y, y + cell, y + cell})
                                    : (half == 0 ? new double[] {y, y, y + cell} : new double[] {y, y + cell, y + cell});
                            g.fillPolygon(xs, ys, 3);
                        }
                        g.setGlobalAlpha(0.35);
                        g.setStroke(FOLD);
                        g.setLineWidth(1);
                        if (slash) g.strokeLine(x + cell, y, x, y + cell);
                        else g.strokeLine(x, y, x + cell, y + cell);
                        g.strokeLine(x, y, x + cell, y);
                        g.strokeLine(x, y, x, y + cell);
                    }
                }
            }
            case BANG_MAP -> {
                // Les taches du parchemin, les lignes de rhumb depuis un coin, et le cadre.
                for (int k = 0; k < 14; k++) {
                    double x = fraction(k * 1.31) * w, y = fraction(k * 2.77) * h, size = small * (0.05 + 0.15 * fraction(k * 4.1));
                    g.setGlobalAlpha(1);
                    g.setFill(new RadialGradient(0, 0, x, y, size, false, CycleMethod.NO_CYCLE,
                            new Stop(0, tint(Color.web("#b89a60"), 0.25)), new Stop(1, tint(Color.web("#b89a60"), 0))));
                    g.fillOval(x - size, y - size, 2 * size, 2 * size);
                }
                g.setStroke(SEPIA);
                g.setLineWidth(0.8);
                double ox = w * 0.12, oy = h * 0.85, far = Math.hypot(w, h);
                for (int k = 0; k < 16; k++) {
                    double a = -Math.PI / 2 + Math.PI / 2 * k / 15;
                    g.setGlobalAlpha(k % 4 == 0 ? 0.3 : 0.15);
                    g.strokeLine(ox, oy, ox + far * Math.cos(a), oy + far * Math.sin(a));
                }
                g.setGlobalAlpha(0.6);
                g.setLineWidth(2);
                g.strokeRect(6, 6, w - 12, h - 12);
                g.setLineWidth(0.8);
                g.strokeRect(10, 10, w - 20, h - 20);
            }
            default -> {
                g.setGlobalAlpha(1);
                g.setFill(new LinearGradient(0, 0, 0, h, false, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.web("#0b0d10")), new Stop(0.6, Color.web("#15171a")), new Stop(1, Color.web("#23262b"))));
                g.fillRect(0, 0, w, h);
                // Les reflets des enseignes sur l'asphalte mouillé : des traînées verticales qui tremblent.
                for (int k = 0; k < 24; k++) {
                    double x = fraction(k * 1.9) * w;
                    double tall = h * (0.15 + 0.25 * fraction(k * 3.7)) * (0.9 + 0.1 * Math.sin(time * 3 + k));
                    g.setGlobalAlpha(0.08 + 0.05 * Math.sin(time * 2 + k * 1.3));
                    g.setFill(k % 3 == 0 ? PINK : k % 3 == 1 ? CYAN : WINDOW);
                    g.fillRect(x, h - tall, Math.max(2, w * 0.006), tall);
                }
            }
        }
        g.setGlobalAlpha(1);
    }

    /** L'espace créé : un lavis, une rosace, une méduse, des facettes, une rose des vents, une enseigne. */
    static void space(GraphicsContext g, double x, double y, double r, Skin look, double time) {
        g.setGlobalAlpha(1);
        switch (look) {
            case BANG_INK -> {
                // Un lavis : du gris dilué, plus dense au centre et par taches.
                g.setFill(new RadialGradient(0, 0, x, y, r, false, CycleMethod.NO_CYCLE,
                        new Stop(0, tint(INK, 0.35)), new Stop(0.6, tint(INK, 0.2)), new Stop(1, tint(INK, 0.3))));
                g.fillOval(x - r, y - r, 2 * r, 2 * r);
                for (int k = 0; k < 8; k++) {
                    double a = k * 2.39996, d = r * 0.6 * fraction(k * 1.7), size = r * (0.15 + 0.2 * fraction(k * 3.1));
                    g.setFill(new RadialGradient(0, 0, x + d * Math.cos(a), y + d * Math.sin(a), size, false, CycleMethod.NO_CYCLE,
                            new Stop(0, tint(INK, 0.18)), new Stop(1, tint(INK, 0))));
                    g.fillOval(x + d * Math.cos(a) - size, y + d * Math.sin(a) - size, 2 * size, 2 * size);
                }
            }
            case BANG_STAINED -> {
                // La rosace : deux couronnes de verres autour d'un cœur doré, cernés de plomb.
                for (int ring = 0; ring < 2; ring++) {
                    int panes = 8 + 8 * ring;
                    double inner = r * (0.2 + 0.4 * ring), outer = r * (0.2 + 0.4 * (ring + 1));
                    for (int k = 0; k < panes; k++) {
                        double a0 = 2 * Math.PI * k / panes + ring * 0.2, a1 = 2 * Math.PI * (k + 1) / panes + ring * 0.2;
                        double[] xs = {x + inner * Math.cos(a0), x + outer * Math.cos(a0), x + outer * Math.cos(a1), x + inner * Math.cos(a1)};
                        double[] ys = {y + inner * Math.sin(a0), y + outer * Math.sin(a0), y + outer * Math.sin(a1), y + inner * Math.sin(a1)};
                        g.setGlobalAlpha(0.6 + 0.25 * Math.sin(time * 0.6 + k + ring * 3));
                        g.setFill(GLASS[(k + ring) % GLASS.length]);
                        g.fillPolygon(xs, ys, 4);
                        g.setGlobalAlpha(1);
                        g.setStroke(LEAD);
                        g.setLineWidth(Math.max(1.5, r * 0.02));
                        g.strokePolygon(xs, ys, 4);
                    }
                }
                g.setGlobalAlpha(1);
                g.setFill(new RadialGradient(0, 0, x, y, r * 0.2, false, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.web("#fff6d8")), new Stop(1, Color.web("#f4a259"))));
                g.fillOval(x - r * 0.2, y - r * 0.2, r * 0.4, r * 0.4);
            }
            case BANG_ABYSS -> {
                // La méduse : une ombrelle translucide qui pulse, et ses tentacules qui ondulent sous elle.
                double pulse = 1 + 0.04 * Math.sin(time * 2.5);
                double rx = r * pulse, ry = r * (2 - pulse);
                g.setFill(new RadialGradient(0, 0, x, y - ry * 0.2, r, false, CycleMethod.NO_CYCLE,
                        new Stop(0, tint(Color.web("#dcd0ff"), 0.5)), new Stop(0.7, tint(JELLY, 0.3)), new Stop(1, tint(JELLY, 0.15))));
                g.fillOval(x - rx, y - ry, 2 * rx, 2 * ry);
                g.setStroke(Color.web("#dcd0ff"));
                g.setLineWidth(Math.max(1, r * 0.012));
                for (int k = 0; k < 7; k++) {
                    double x0 = x - rx * 0.7 + rx * 1.4 * k / 6;
                    int points = 8;
                    double[] xs = new double[points], ys = new double[points];
                    for (int p = 0; p < points; p++) {
                        double u = p / (double) (points - 1);
                        xs[p] = x0 + r * 0.12 * Math.sin(time * 2 + k + u * 4) * u;
                        ys[p] = y + ry * 0.6 + ry * 0.7 * u;
                    }
                    g.setGlobalAlpha(0.25 * (1 - 0.5 * (k % 2)));
                    g.strokePolyline(xs, ys, points);
                }
            }
            case BANG_ORIGAMI -> {
                // Des facettes planes : dix triangles du centre au bord, à l'ombre ou au jour tour à tour.
                int sides = 10;
                for (int k = 0; k < sides; k++) {
                    double a0 = 2 * Math.PI * k / sides, a1 = 2 * Math.PI * (k + 1) / sides;
                    double shade = 0.7 + 0.22 * ((k % 2 == 0 ? 1 : 0) * 0.6 + 0.4 * fraction(k * 3.7));
                    g.setFill(Color.color(shade * 0.95, shade * 0.88, shade * 0.75));
                    g.fillPolygon(new double[] {x, x + r * Math.cos(a0), x + r * Math.cos(a1)}, new double[] {y, y + r * Math.sin(a0), y + r * Math.sin(a1)}, 3);
                    g.setStroke(FOLD);
                    g.setLineWidth(Math.max(0.8, r * 0.006));
                    g.setGlobalAlpha(0.7);
                    g.strokeLine(x, y, x + r * Math.cos(a0), y + r * Math.sin(a0));
                    g.setGlobalAlpha(1);
                }
            }
            case BANG_MAP -> {
                // La rose des vents : un disque de parchemin foncé, seize branches sépia et rouges.
                g.setFill(Color.web("#d8c49a"));
                g.fillOval(x - r, y - r, 2 * r, 2 * r);
                for (int k = 0; k < 16; k++) {
                    double a = -Math.PI / 2 + Math.PI * k / 8;
                    double len = r * (k % 4 == 0 ? 0.92 : k % 2 == 0 ? 0.62 : 0.4);
                    g.setFill(k % 2 == 0 ? SEPIA : Color.web("#b02a2a"));
                    g.fillPolygon(new double[] {x, x + len * Math.cos(a), x + len * 0.18 * Math.cos(a + 0.4)},
                            new double[] {y, y + len * Math.sin(a), y + len * 0.18 * Math.sin(a + 0.4)}, 3);
                    g.setFill(k % 2 == 0 ? Color.web("#8a6a3a") : Color.web("#d85a5a"));
                    g.fillPolygon(new double[] {x, x + len * Math.cos(a), x + len * 0.18 * Math.cos(a - 0.4)},
                            new double[] {y, y + len * Math.sin(a), y + len * 0.18 * Math.sin(a - 0.4)}, 3);
                }
                g.setStroke(SEPIA);
                g.setLineWidth(Math.max(1, r * 0.01));
                g.strokeOval(x - r * 0.5, y - r * 0.5, r, r);
            }
            default -> {
                // L'enseigne : un disque sombre, et un anneau de néon rose qui clignote de temps en temps.
                g.setFill(Color.web("#1c1f24"));
                g.fillOval(x - r, y - r, 2 * r, 2 * r);
                boolean off = Math.sin(time * 7) > 0.96 || Math.sin(time * 1.3) > 0.995;
                double lit = off ? 0.15 : 0.7 + 0.1 * Math.sin(time * 13);
                g.setGlobalAlpha(1);
                g.setFill(new RadialGradient(0, 0, x, y, r, false, CycleMethod.NO_CYCLE,
                        new Stop(0.7, tint(PINK, 0)), new Stop(1, tint(PINK, 0.35 * lit))));
                g.fillOval(x - r, y - r, 2 * r, 2 * r);
                g.setStroke(CYAN);
                g.setLineWidth(Math.max(1, r * 0.015));
                g.setGlobalAlpha(0.6 * lit);
                g.strokeOval(x - r * 0.85, y - r * 0.85, r * 1.7, r * 1.7);
            }
        }
        g.setGlobalAlpha(1);
    }

    /** Le bord de l'espace : un coup de pinceau, un cerne de plomb, une lueur, des arêtes, une graduation, un tube de néon. */
    static void edge(GraphicsContext g, double x, double y, double r, Skin look, double time) {
        int steps = 120;
        double[] xs = new double[steps + 1], ys = new double[steps + 1];
        switch (look) {
            case BANG_INK -> {
                // Un trait de pinceau : l'épaisseur et la charge d'encre varient tout le long, et il bave par endroits.
                for (int k = 0; k < steps; k++) {
                    double a0 = 2 * Math.PI * k / steps, a1 = 2 * Math.PI * (k + 1) / steps;
                    double load = 0.5 + 0.5 * Math.sin(a0 * 3 + 1) * Math.cos(a0 * 7);
                    double rr = r * (1 + 0.01 * Math.sin(a0 * 11));
                    g.setStroke(INK);
                    g.setLineWidth(Math.max(1.5, r * 0.012) * (0.5 + load));
                    g.setGlobalAlpha(0.55 + 0.4 * load);
                    g.strokeLine(x + rr * Math.cos(a0), y + rr * Math.sin(a0), x + rr * Math.cos(a1), y + rr * Math.sin(a1));
                }
            }
            case BANG_STAINED -> {
                g.setStroke(LEAD);
                g.setLineWidth(Math.max(3, r * 0.03));
                g.setGlobalAlpha(1);
                g.strokeOval(x - r, y - r, 2 * r, 2 * r);
                g.setStroke(Color.web("#6a6a70"));
                g.setLineWidth(1);
                g.setGlobalAlpha(0.6);
                g.strokeOval(x - r * 0.985, y - r * 0.985, 2 * r * 0.985, 2 * r * 0.985);
            }
            case BANG_ABYSS -> {
                double pulse = 0.5 + 0.5 * Math.sin(time * 2.5);
                g.setStroke(Color.web("#dcd0ff"));
                g.setLineWidth(Math.max(1.5, r * 0.01));
                g.setGlobalAlpha(0.3 + 0.4 * pulse);
                g.strokeOval(x - r, y - r, 2 * r, 2 * r);
            }
            case BANG_ORIGAMI -> {
                int sides = 10;
                for (int k = 0; k <= sides; k++) {
                    double a = 2 * Math.PI * k / sides;
                    xs[k] = x + r * Math.cos(a);
                    ys[k] = y + r * Math.sin(a);
                }
                g.setStroke(Color.web("#6a5a40"));
                g.setLineWidth(Math.max(1.5, r * 0.008));
                g.setGlobalAlpha(0.9);
                g.strokePolyline(xs, ys, sides + 1);
            }
            case BANG_MAP -> {
                // Deux cercles et une graduation en degrés, les dizaines plus longues.
                g.setStroke(SEPIA);
                g.setLineWidth(Math.max(1.5, r * 0.01));
                g.setGlobalAlpha(0.9);
                g.strokeOval(x - r, y - r, 2 * r, 2 * r);
                g.setLineWidth(0.8);
                g.strokeOval(x - r * 1.04, y - r * 1.04, 2 * r * 1.04, 2 * r * 1.04);
                for (int k = 0; k < 72; k++) {
                    double a = 2 * Math.PI * k / 72;
                    double tick = k % 9 == 0 ? 0.04 : 0.02;
                    g.strokeLine(x + r * Math.cos(a), y + r * Math.sin(a), x + r * (1 + tick) * Math.cos(a), y + r * (1 + tick) * Math.sin(a));
                }
            }
            default -> {
                boolean off = Math.sin(time * 7) > 0.96 || Math.sin(time * 1.3) > 0.995;
                double lit = off ? 0.2 : 1;
                g.setStroke(PINK);
                g.setLineWidth(Math.max(4, r * 0.04));
                g.setGlobalAlpha(0.3 * lit);
                g.strokeOval(x - r, y - r, 2 * r, 2 * r);
                g.setLineWidth(Math.max(1.5, r * 0.012));
                g.setGlobalAlpha(0.95 * lit);
                g.strokeOval(x - r, y - r, 2 * r, 2 * r);
                g.setStroke(Color.WHITE);
                g.setLineWidth(1);
                g.setGlobalAlpha(0.7 * lit);
                g.strokeOval(x - r, y - r, 2 * r, 2 * r);
            }
        }
        g.setGlobalAlpha(1);
    }

    /**
     * Les étoiles : des points d'encre, des éclats de verre, du plancton, des confettis, des constellations ou
     * des fenêtres allumées.
     *
     * @param starX la place de chaque étoile, en part de la largeur ; {@code starY} de la hauteur ; {@code pace} son rythme
     */
    static void stars(GraphicsContext g, double w, double h, Skin look, double time, double[] starX, double[] starY, double[] pace) {
        int count = starX.length;
        if (look == Skin.BANG_MAP) {
            // Les constellations : chaque étoile est reliée à la plus proche qui la suit dans la liste, si elle n'est pas trop loin.
            double reach = Math.min(w, h) * 0.18;
            g.setStroke(SEPIA);
            g.setLineWidth(0.8);
            g.setGlobalAlpha(0.45);
            for (int star = 0; star < count; star++) {
                double x = starX[star] * w, y = starY[star] * h, best = reach * reach;
                int near = -1;
                for (int other = star + 1; other < count; other++) {
                    double dx = starX[other] * w - x, dy = starY[other] * h - y, d = dx * dx + dy * dy;
                    if (d < best) {
                        best = d;
                        near = other;
                    }
                }
                if (near >= 0) g.strokeLine(x, y, starX[near] * w, starY[near] * h);
            }
        }
        for (int star = 0; star < count; star++) {
            double x = starX[star] * w, y = starY[star] * h;
            double twinkle = Math.max(0.05, Math.min(1, 0.5 + 0.4 * Math.sin(time * pace[star] * 1.5 + star)));
            switch (look) {
                case BANG_INK -> {
                    double size = star % 9 == 0 ? 2.6 : 1.2 + 0.8 * fraction(star * 3.3);
                    g.setGlobalAlpha(0.6 + 0.3 * fraction(star * 1.7));
                    g.setFill(INK);
                    g.fillOval(x - size, y - size * (0.8 + 0.3 * fraction(star)), 2 * size, 2 * size * (0.8 + 0.3 * fraction(star)));
                }
                case BANG_STAINED -> {
                    double size = star % 9 == 0 ? 3 : 1.6;
                    g.setGlobalAlpha(twinkle);
                    g.setFill(GLASS[star % GLASS.length].interpolate(Color.WHITE, 0.5));
                    g.fillRect(x - size / 2, y - size / 2, size, size);
                }
                case BANG_ABYSS -> {
                    double size = 1.2 + 1.8 * twinkle;
                    g.setGlobalAlpha(0.4 + 0.6 * twinkle);
                    g.setFill(star % 3 == 0 ? PLANKTON : Color.web("#5ad0ff"));
                    g.fillOval(x - size / 2, y + Math.sin(time * 0.3 + star) * 3 - size / 2, size, size);
                }
                case BANG_ORIGAMI -> {
                    double size = star % 9 == 0 ? 4 : 2.5;
                    double spin = star * 1.3 + time * 0.1;
                    g.setGlobalAlpha(0.85);
                    g.setFill(GLASS[star % GLASS.length]);
                    g.fillPolygon(new double[] {x + size * Math.cos(spin), x + size * Math.cos(spin + 1.6), x + size * Math.cos(spin + 3.1), x + size * Math.cos(spin + 4.7)},
                            new double[] {y + size * 0.6 * Math.sin(spin), y + size * 0.6 * Math.sin(spin + 1.6), y + size * 0.6 * Math.sin(spin + 3.1), y + size * 0.6 * Math.sin(spin + 4.7)}, 4);
                }
                case BANG_MAP -> {
                    double size = star % 9 == 0 ? 3 : 1.6;
                    g.setGlobalAlpha(0.85);
                    g.setFill(SEPIA);
                    g.fillPolygon(new double[] {x, x + size / 2, x, x - size / 2}, new double[] {y - size, y, y + size, y}, 4);
                }
                default -> {
                    // Une fenêtre : allumée la plupart du temps, éteinte quand sa lampe « clignote ».
                    boolean lit = Math.sin(time * pace[star] * 0.5 + star * 2.1) > -0.7;
                    g.setGlobalAlpha(lit ? 0.6 + 0.3 * fraction(star * 2.3) : 0.1);
                    g.setFill(star % 7 == 0 ? Color.web("#9fd0ff") : WINDOW);
                    g.fillRect(x - 1.5, y - 2, 3, 4);
                }
            }
        }
        g.setGlobalAlpha(1);
    }
}
