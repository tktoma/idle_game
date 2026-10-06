package idle.ui;

import idle.core.Body;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;

/**
 * Le dessin d'un astre ({@link Body}).
 *
 * <p>Son échelle décide de sa silhouette : un amas de roches est une poignée de cailloux, une
 * comète un petit noyau dans sa chevelure avec ses deux queues, un astéroïde un bloc cabossé, une
 * lune et une planète des disques. Son aspect ({@link Body.Look}) décide de sa surface : cratères,
 * fissures de glace, veines qui luisent, brume, mers et nuages, bandes de gaz, anneaux.
 *
 * <p>Tout est tiré d'une suite fixe qui ne dépend que de l'identifiant de l'astre : un astre a
 * toujours les mêmes cailloux et les mêmes cratères, sur sa carte comme dans le ciel.
 */
final class BodyArt {

    /** Direction des queues d'une comète, en radians : vers le haut à droite. */
    private static final double TAIL = -Math.PI / 6;
    private static final Color SHADE = Color.web("#05060a");
    private static final Color LIGHT = Color.web("#ffffff");
    private static final Color ION_TAIL = Color.web("#9fd0ff");
    private static final Color LAND = Color.web("#a8905e");

    private BodyArt() {}

    /** De combien un astre dépasse à droite de son centre, en rayons : la queue d'une comète, les anneaux d'une géante. */
    static double reachRight(Body body) {
        if (body.tier() == Body.Tier.COMET) return 5.2;
        if (body.look() == Body.Look.RINGED) return 2;
        return body.tier() == Body.Tier.RUBBLE ? 1.15 : 1;
    }

    /** De combien un astre dépasse à gauche de son centre, en rayons. */
    static double reachLeft(Body body) {
        if (body.tier() == Body.Tier.COMET) return 1.7;
        if (body.look() == Body.Look.RINGED) return 2;
        return body.tier() == Body.Tier.RUBBLE ? 1.15 : 1;
    }

    /** De combien un astre dépasse au-dessus de son centre, en rayons. */
    static double reachUp(Body body) {
        return body.tier() == Body.Tier.COMET ? 3.2 : body.tier() == Body.Tier.RUBBLE ? 1.15 : 1;
    }

    /** De combien un astre dépasse en dessous de son centre, en rayons. */
    static double reachDown(Body body) {
        return body.tier() == Body.Tier.COMET ? 1.7 : body.tier() == Body.Tier.RUBBLE ? 1.15 : 1;
    }

    /**
     * Dessine un astre centré en (x, y).
     *
     * @param r son rayon : celui du disque d'une lune ou d'une planète, du bloc d'un astéroïde, du
     *          tas d'un amas, du noyau d'une comète avec un peu de sa chevelure
     */
    static void draw(GraphicsContext g, Body body, double x, double y, double r) {
        int seed = body.id().hashCode();
        Color tint = Color.web(body.tint());
        Color accent = Color.web(body.accent());
        g.setGlobalAlpha(1);
        switch (body.tier()) {
            case RUBBLE -> rubble(g, body, seed, tint, accent, x, y, r);
            case COMET -> comet(g, body, seed, tint, accent, x, y, r);
            case ASTEROID -> {
                lump(g, seed, tint, x, y, r, 14);
                surface(g, body.look(), seed, tint, accent, x, y, 0.62 * r);
            }
            case MOON, PLANET -> sphere(g, body, seed, tint, accent, x, y, r);
        }
        g.setGlobalAlpha(1);
    }

    /** Un amas de roches : sept cailloux serrés, de trois teintes. */
    private static void rubble(GraphicsContext g, Body body, int seed, Color tint, Color accent, double x, double y, double r) {
        Color dark = tint.interpolate(SHADE, 0.3);
        for (int stone = 0; stone < 7; stone++) {
            // Un au milieu, six autour.
            double away = stone == 0 ? 0 : (0.55 + 0.18 * chance(seed, stone * 3)) * r;
            double angle = stone * Math.PI / 3 + chance(seed, stone * 3 + 1);
            double size = (stone == 0 ? 0.5 : 0.3 + 0.16 * chance(seed, stone * 3 + 2)) * r;
            Color color = stone % 3 == 0 ? tint : stone % 3 == 1 ? dark : tint.interpolate(accent, 0.6);
            lump(g, seed + 31 * stone, color, x + away * Math.cos(angle), y + away * Math.sin(angle), size, 7);
        }
        surface(g, body.look(), seed, tint, accent, x, y, 0.3 * r);
    }

    /** Une comète : deux queues, une chevelure, et son noyau. */
    private static void comet(GraphicsContext g, Body body, int seed, Color tint, Color accent, double x, double y, double r) {
        // La queue de gaz, droite et fine ; la queue de poussière, plus large et un peu déviée.
        tail(g, ION_TAIL, 0.22, x, y, 5.6 * r, 0.5 * r, TAIL - 0.12);
        tail(g, accent, 0.3, x, y, 4.6 * r, 1.1 * r, TAIL + 0.14);
        g.setFill(accent);
        g.setGlobalAlpha(0.16);
        g.fillOval(x - 1.6 * r, y - 1.6 * r, 3.2 * r, 3.2 * r);
        g.setGlobalAlpha(0.3);
        g.fillOval(x - 1.05 * r, y - 1.05 * r, 2.1 * r, 2.1 * r);
        g.setGlobalAlpha(1);
        lump(g, seed, tint, x, y, 0.6 * r, 9);
        surface(g, body.look(), seed, tint, accent, x, y, 0.36 * r);
    }

    /** Une queue de comète : un triangle translucide qui part du noyau et s'élargit. */
    private static void tail(GraphicsContext g, Color color, double alpha, double x, double y, double length, double width, double angle) {
        double endX = x + length * Math.cos(angle);
        double endY = y + length * Math.sin(angle);
        double sideX = -Math.sin(angle) * width;
        double sideY = Math.cos(angle) * width;
        g.setFill(color);
        g.setGlobalAlpha(alpha);
        g.fillPolygon(new double[] {x - 0.25 * sideX, endX - sideX, endX + sideX, x + 0.25 * sideX},
                new double[] {y - 0.25 * sideY, endY - sideY, endY + sideY, y + 0.25 * sideY}, 4);
        g.setGlobalAlpha(1);
    }

    /** Un bloc cabossé : un polygone dont chaque pointe est plus ou moins loin du centre, jamais plus que {@code r}. */
    private static void lump(GraphicsContext g, int seed, Color color, double x, double y, double r, int points) {
        double[] xs = new double[points];
        double[] ys = new double[points];
        for (int point = 0; point < points; point++) {
            double angle = 2 * Math.PI * point / points + 0.3 * chance(seed, 100 + point);
            double away = (0.72 + 0.28 * chance(seed, 200 + point)) * r;
            xs[point] = x + away * Math.cos(angle);
            ys[point] = y + away * Math.sin(angle);
        }
        g.setFill(color);
        g.fillPolygon(xs, ys, points);
    }

    /** Une lune ou une planète : un disque, sa surface, un peu d'ombre et de lumière, et parfois des anneaux. */
    private static void sphere(GraphicsContext g, Body body, int seed, Color tint, Color accent, double x, double y, double r) {
        boolean ringed = body.look() == Body.Look.RINGED;
        if (ringed) ring(g, accent, x, y, r, 0, 2 * Math.PI);
        // Une atmosphère : un halo fin autour des mondes qui en ont une.
        if (body.look() == Body.Look.OCEAN || body.look() == Body.Look.HAZY || body.look() == Body.Look.BANDED || ringed) {
            g.setFill(accent);
            g.setGlobalAlpha(0.22);
            g.fillOval(x - 1.07 * r, y - 1.07 * r, 2.14 * r, 2.14 * r);
            g.setGlobalAlpha(1);
        }
        g.setFill(tint);
        g.fillOval(x - r, y - r, 2 * r, 2 * r);
        surface(g, body.look(), seed, tint, accent, x, y, r);
        // La lumière vient d'en haut à gauche : un reflet de ce côté, une ombre de l'autre, tous deux dans le disque.
        g.setFill(LIGHT);
        g.setGlobalAlpha(0.12);
        g.fillOval(x - 0.85 * r, y - 0.85 * r, 1.2 * r, 1.2 * r);
        g.setFill(SHADE);
        g.setGlobalAlpha(0.2);
        g.fillOval(x - 0.56 * r, y - 0.56 * r, 1.48 * r, 1.48 * r);
        g.setGlobalAlpha(1);
        // La moitié des anneaux qui passe devant la planète.
        if (ringed) ring(g, accent, x, y, r, 0, Math.PI);
    }

    /** Un arc d'anneaux autour d'une planète de rayon {@code r} : une ellipse très aplatie, tracée par petits segments. */
    private static void ring(GraphicsContext g, Color color, double x, double y, double r, double from, double to) {
        g.setStroke(color);
        g.setGlobalAlpha(0.85);
        g.setLineWidth(Math.max(1.5, 0.16 * r));
        int steps = 28;
        for (int step = 0; step < steps; step++) {
            double start = from + (to - from) * step / steps;
            double end = from + (to - from) * (step + 1) / steps;
            g.strokeLine(x + 1.85 * r * Math.cos(start), y + 0.42 * r * Math.sin(start),
                    x + 1.85 * r * Math.cos(end), y + 0.42 * r * Math.sin(end));
        }
        g.setGlobalAlpha(1);
    }

    /**
     * Les détails d'une surface, tous à l'intérieur du disque de rayon {@code r} centré en (x, y).
     * Pour un astre qui n'est pas rond, {@code r} est plus petit que lui : les détails restent sur lui.
     */
    private static void surface(GraphicsContext g, Body.Look look, int seed, Color tint, Color accent, double x, double y, double r) {
        switch (look) {
            case ROCKY -> {
                // Des cratères : des ronds plus sombres, avec un fond un peu plus clair.
                for (int crater = 0; crater < 6; crater++) {
                    double size = (0.08 + 0.12 * chance(seed, 300 + crater)) * r;
                    double away = 0.68 * r * Math.sqrt(chance(seed, 310 + crater));
                    double angle = 2 * Math.PI * chance(seed, 320 + crater);
                    double cx = x + away * Math.cos(angle);
                    double cy = y + away * Math.sin(angle);
                    g.setFill(tint.interpolate(SHADE, 0.32));
                    g.fillOval(cx - size, cy - size, 2 * size, 2 * size);
                    g.setFill(tint.interpolate(accent, 0.35));
                    g.fillOval(cx - 0.55 * size, cy - 0.4 * size, 1.3 * size, 1.3 * size);
                }
            }
            case ICY -> cracks(g, seed, accent, 0.05, x, y, r, 6);
            case MOLTEN -> {
                cracks(g, seed, accent, 0.07, x, y, r, 5);
                g.setFill(accent);
                for (int spot = 0; spot < 4; spot++) {
                    double size = (0.05 + 0.06 * chance(seed, 400 + spot)) * r;
                    double away = 0.7 * r * Math.sqrt(chance(seed, 410 + spot));
                    double angle = 2 * Math.PI * chance(seed, 420 + spot);
                    g.fillOval(x + away * Math.cos(angle) - size, y + away * Math.sin(angle) - size, 2 * size, 2 * size);
                }
            }
            case DARK -> {
                g.setFill(accent);
                g.setGlobalAlpha(0.55);
                for (int speck = 0; speck < 8; speck++) {
                    double size = (0.04 + 0.05 * chance(seed, 500 + speck)) * r;
                    double away = 0.78 * r * Math.sqrt(chance(seed, 510 + speck));
                    double angle = 2 * Math.PI * chance(seed, 520 + speck);
                    g.fillOval(x + away * Math.cos(angle) - size, y + away * Math.sin(angle) - size, 2 * size, 2 * size);
                }
                g.setGlobalAlpha(1);
            }
            case HAZY -> {
                // Des voiles de brume : de grands ronds pâles qui se recouvrent, et deux traînées.
                g.setFill(accent);
                g.setGlobalAlpha(0.2);
                g.fillOval(x - 0.9 * r, y - 0.75 * r, 1.5 * r, 1.5 * r);
                g.fillOval(x - 0.5 * r, y - 0.55 * r, 1.4 * r, 1.4 * r);
                g.setGlobalAlpha(1);
                bands(g, new Color[] {accent}, 0.35, x, y, r, new double[] {-0.35, 0.3}, 0.1);
            }
            case OCEAN -> {
                // Des terres nues, puis des nuages qui passent par-dessus.
                for (int land = 0; land < 3; land++) {
                    double away = 0.5 * r * chance(seed, 600 + land);
                    double angle = 2 * Math.PI * chance(seed, 610 + land);
                    lump(g, seed + 7 * land, LAND, x + away * Math.cos(angle), y + away * Math.sin(angle), (0.2 + 0.14 * chance(seed, 620 + land)) * r, 8);
                }
                bands(g, new Color[] {accent}, 0.75, x, y, r, new double[] {-0.55, -0.15, 0.25, 0.6}, 0.08);
            }
            case BANDED, RINGED -> {
                Color pale = tint.interpolate(LIGHT, 0.35);
                bands(g, new Color[] {accent, pale}, look == Body.Look.BANDED ? 0.8 : 0.4, x, y, r,
                        new double[] {-0.68, -0.4, -0.1, 0.2, 0.48, 0.72}, look == Body.Look.BANDED ? 0.2 : 0.12);
                if (look == Body.Look.BANDED) {
                    // Une tempête : une tache ovale sur une bande.
                    g.setFill(accent.interpolate(SHADE, 0.25));
                    g.fillOval(x + 0.15 * r, y + 0.12 * r, 0.42 * r, 0.24 * r);
                }
            }
        }
    }

    /** Des fissures ou des veines : des traits entre deux points pris dans le disque. */
    private static void cracks(GraphicsContext g, int seed, Color color, double width, double x, double y, double r, int count) {
        g.setStroke(color);
        g.setLineWidth(Math.max(1, width * r));
        g.setLineCap(StrokeLineCap.ROUND);
        for (int crack = 0; crack < count; crack++) {
            double from = 2 * Math.PI * chance(seed, 700 + crack);
            double to = from + Math.PI * (0.5 + chance(seed, 710 + crack));
            double start = 0.8 * r * (0.4 + 0.6 * chance(seed, 720 + crack));
            double end = 0.8 * r * (0.4 + 0.6 * chance(seed, 730 + crack));
            g.strokeLine(x + start * Math.cos(from), y + start * Math.sin(from), x + end * Math.cos(to), y + end * Math.sin(to));
        }
        g.setLineCap(StrokeLineCap.SQUARE);
    }

    /**
     * Des bandes horizontales en travers d'un disque : chacune est un trait aux bouts ronds, juste
     * assez court pour ne pas dépasser du disque à sa hauteur.
     *
     * @param heights la hauteur de chaque bande, en rayons, de -1 (en haut) à 1 (en bas)
     * @param thick   l'épaisseur d'une bande, en rayons
     */
    private static void bands(GraphicsContext g, Color[] colors, double alpha, double x, double y, double r, double[] heights, double thick) {
        g.setLineWidth(Math.max(1, thick * r));
        g.setLineCap(StrokeLineCap.ROUND);
        g.setGlobalAlpha(alpha);
        for (int band = 0; band < heights.length; band++) {
            double dy = heights[band] * r;
            double half = Math.sqrt(Math.max(0, r * r - dy * dy)) - 0.75 * thick * r;
            if (half <= 0) continue;
            g.setStroke(colors[band % colors.length]);
            g.strokeLine(x - half, y + dy, x + half, y + dy);
        }
        g.setGlobalAlpha(1);
        g.setLineCap(StrokeLineCap.SQUARE);
    }

    /** Un nombre entre 0 et 1 qui ne dépend que de l'astre et du rang demandé. */
    private static double chance(int seed, int rank) {
        double value = Math.sin(seed * 12.9898 + rank * 78.233) * 43758.5453;
        return value - Math.floor(value);
    }
}
