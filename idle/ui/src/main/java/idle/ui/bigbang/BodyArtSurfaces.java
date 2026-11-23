package idle.ui.bigbang;

import idle.core.sky.Body;
import idle.core.universe.Skin;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import static idle.ui.bigbang.BodyArt.*;

/**
 * Les surfaces des astres ronds de {@link BodyArt} (découpées en 0.293 pour rester sous 1 000 lignes) : le globe,
 * l'atmosphère, les anneaux, le relief, les veines, les fissures, les nuages et les vagues.
 */
final class BodyArtSurfaces {
    private BodyArtSurfaces() {
    }

    /**
     * Une lune ou une planète : son air, son globe, sa surface, son ombre et son reflet, et parfois
     * des anneaux. En grand : l'ombre des anneaux sur le globe, son limbe qui s'assombrit, un reflet
     * sur ses mers ou sa glace, le liseré de son air, et l'ombre de la planète sur ses anneaux.
     */
    static void globe(GraphicsContext g, Body body, Palette palette, double x, double y, double r) {
        Body.Look look = body.look();
        boolean ringed = look == Body.Look.RINGED;
        boolean detail = detailed(r);
        // La moitié des anneaux qui passe derrière la planète.
        if (ringed) rings(g, palette, x, y, r, Math.PI, 0.5, false);
        double air = air(look) * r;
        if (air > 0) {
            g.setFill(new RadialGradient(0, 0, x, y, air, false, CycleMethod.NO_CYCLE, palette.air));
            g.fillOval(x - air, y - air, 2 * air, 2 * air);
        }
        g.setFill(new RadialGradient(0, 0, x - 0.3 * r, y - 0.32 * r, 1.35 * r, false, CycleMethod.NO_CYCLE, palette.globe));
        g.fillOval(x - r, y - r, 2 * r, 2 * r);
        surface(g, look, palette, x, y, r, detail);
        if (detail && ringed) ringShadow(g, x, y, r);
        // La lumière vient d'en haut à gauche : le globe s'éteint vers le bas à droite, et brille un peu de l'autre côté.
        g.setFill(new RadialGradient(0, 0, x - 0.28 * r, y - 0.3 * r, 1.42 * r, false, CycleMethod.NO_CYCLE, SHADOW));
        g.fillOval(x - r, y - r, 2 * r, 2 * r);
        if (detail) {
            // Le limbe : le bord s'assombrit tout autour, ce qui arrondit le globe.
            g.setFill(new RadialGradient(0, 0, x, y, r, false, CycleMethod.NO_CYCLE, LIMB));
            g.fillOval(x - r, y - r, 2 * r, 2 * r);
            if (look == Body.Look.OCEAN || look == Body.Look.ICY) {
                // Le soleil se reflète sur les mers ou la glace : un point vif, en haut à gauche.
                double shine = 0.16 * r;
                g.setFill(new RadialGradient(0, 0, x - 0.42 * r, y - 0.44 * r, shine, false, CycleMethod.NO_CYCLE, SPECULAR));
                g.fillOval(x - 0.42 * r - shine, y - 0.44 * r - shine, 2 * shine, 2 * shine);
            }
            if (air > 0) atmosphere(g, palette, x, y, r);
        }
        g.setFill(new RadialGradient(0, 0, x - 0.38 * r, y - 0.4 * r, 0.6 * r, false, CycleMethod.NO_CYCLE, GLINT));
        g.fillOval(x - r, y - r, 2 * r, 2 * r);
        // La moitié des anneaux qui passe devant.
        if (ringed) rings(g, palette, x, y, r, 0, 0.9, detail);
    }

    /**
     * Le liseré d'air au bord d'un monde qui en a : un fil de sa brume tout autour du globe, et un
     * arc plus vif d'un tiers de tour du côté du soleil. Il reste dans le halo d'air ({@link #air}).
     */
    static void atmosphere(GraphicsContext g, Palette palette, double x, double y, double r) {
        g.setLineCap(StrokeLineCap.ROUND);
        g.setLineWidth(Math.max(1, 0.035 * r));
        g.setStroke(palette.airRim);
        g.strokeOval(x - 1.01 * r, y - 1.01 * r, 2.02 * r, 2.02 * r);
        g.setStroke(palette.airLit);
        g.strokeArc(x - 1.01 * r, y - 1.01 * r, 2.02 * r, 2.02 * r, 90, 120, ArcType.OPEN);
        g.setLineCap(StrokeLineCap.SQUARE);
    }

    /**
     * L'ombre des anneaux sur leur planète : une bande sombre qui suit, un peu au-dessus, les anneaux
     * qui passent devant, et reste dans le globe.
     */
    static void ringShadow(GraphicsContext g, double x, double y, double r) {
        for (int step = 0; step <= ARC; step++) {
            double angle = Math.PI * step / ARC;
            XS[step] = x + 0.94 * r * Math.cos(angle);
            YS[step] = y + RING_TILT * 0.94 * r * Math.sin(angle) - 0.07 * r;
        }
        g.setStroke(RING_SHADOW);
        g.setLineWidth(Math.max(1, 0.06 * r));
        g.setLineCap(StrokeLineCap.BUTT);
        g.setLineJoin(StrokeLineJoin.ROUND);
        g.strokePolyline(XS, YS, ARC + 1);
        g.setLineJoin(StrokeLineJoin.MITER);
        g.setLineCap(StrokeLineCap.SQUARE);
    }

    /**
     * La moitié des anneaux d'une planète de rayon {@code r} qui commence à l'angle {@code from} : trois ellipses très aplaties.
     *
     * @param shaded vrai pour y poser l'ombre de la planète : un tronçon sombre en bas à droite, à l'opposé du soleil
     */
    static void rings(GraphicsContext g, Palette palette, double x, double y, double r, double from, double alpha, boolean shaded) {
        g.setGlobalAlpha(alpha);
        g.setLineCap(StrokeLineCap.BUTT);
        g.setLineJoin(StrokeLineJoin.ROUND);
        for (int ring = 0; ring < RINGS.length; ring++) {
            for (int step = 0; step <= ARC; step++) {
                double angle = from + Math.PI * step / ARC;
                XS[step] = x + RINGS[ring] * r * Math.cos(angle);
                YS[step] = y + RING_TILT * RINGS[ring] * r * Math.sin(angle);
            }
            g.setStroke(palette.rings[ring]);
            g.setLineWidth(Math.max(1, RING_WIDTHS[ring] * r));
            g.strokePolyline(XS, YS, ARC + 1);
        }
        if (shaded) {
            // L'ombre : le même tracé, repassé en sombre, sur les trois dixièmes de tour qui suivent le bord droit de la planète.
            g.setStroke(RING_SHADOW);
            int steps = ARC / 3;
            for (int ring = 0; ring < RINGS.length; ring++) {
                for (int step = 0; step <= steps; step++) {
                    double angle = from + Math.PI * (0.12 + 0.3 * step / steps);
                    XS[step] = x + RINGS[ring] * r * Math.cos(angle);
                    YS[step] = y + RING_TILT * RINGS[ring] * r * Math.sin(angle);
                }
                g.setLineWidth(Math.max(1, RING_WIDTHS[ring] * r));
                g.strokePolyline(XS, YS, steps + 1);
            }
        }
        g.setLineJoin(StrokeLineJoin.MITER);
        g.setLineCap(StrokeLineCap.SQUARE);
        g.setGlobalAlpha(1);
    }

    /**
     * La surface d'un globe de rayon {@code r} centré en (x, y) : tout y reste à l'intérieur.
     *
     * @param detail vrai pour les couches de détail : le creux des cratères, l'ombre des nuages, les remous des géantes
     */
    static void surface(GraphicsContext g, Body.Look look, Palette palette, double x, double y, double r, boolean detail) {
        int seed = palette.seed;
        switch (look) {
            case ROCKY -> {
                // Des mers, de grandes taches sombres, puis des cratères.
                g.setFill(MARE);
                spots(g, seed, 900, 3, 0.22, 0.14, 0.5, x, y, r);
                craters(g, palette, 8, 0.05, 0.09, 0.8, x, y, r, detail);
            }
            case ICY -> {
                g.setFill(palette.veil);
                spots(g, seed, 910, 4, 0.18, 0.14, 0.55, x, y, r);
                cracks(g, seed, palette.fissure, 0.016, x, y, r, 6, 0);
                cracks(g, seed, FROST, 0.014, x, y, r, 3, 40);
                // Une calotte, en haut.
                g.setFill(FROST);
                g.fillOval(x - 0.46 * r, y - 0.91 * r, 0.92 * r, 0.34 * r);
            }
            case MOLTEN -> {
                g.setFill(PATCH);
                spots(g, seed, 920, 3, 0.2, 0.12, 0.5, x, y, r);
                veins(g, palette, x, y, r, 6);
                embers(g, palette, 4, 0.1, 0.08, 0.7, x, y, r);
            }
            case DARK -> {
                g.setFill(PATCH);
                spots(g, seed, 930, 4, 0.2, 0.1, 0.5, x, y, r);
                g.setFill(palette.speck);
                spots(g, seed, 500, 10, 0.03, 0.05, 0.85, x, y, r);
            }
            case HAZY -> {
                // Des voiles de brume, et de larges bandes floues.
                g.setFill(palette.veil);
                g.fillOval(x - 0.9 * r, y - 0.75 * r, 1.5 * r, 1.5 * r);
                g.fillOval(x - 0.5 * r, y - 0.55 * r, 1.4 * r, 1.4 * r);
                bands(g, palette.bands, 0.3, x, y, r, -0.75, 0.3, 6, 0.15, 0.15);
            }
            case OCEAN -> {
                // Des terres et leurs plaines, deux calottes, puis les nuages qui passent par-dessus.
                for (int land = 0; land < 4; land++) {
                    double away = 0.5 * r * chance(seed, 600 + land);
                    double angle = 2 * Math.PI * chance(seed, 610 + land);
                    double size = (0.2 + 0.14 * chance(seed, 620 + land)) * r;
                    double cx = x + away * Math.cos(angle);
                    double cy = y + away * Math.sin(angle);
                    lump(g, seed + 7 * land, palette.land, cx, cy, size, 8, false);
                    lump(g, seed + 11 * land, palette.grass, cx + 0.15 * size, cy + 0.1 * size, 0.5 * size, 6, false);
                }
                g.setFill(CAP);
                g.fillOval(x - 0.36 * r, y - 0.92 * r, 0.72 * r, 0.24 * r);
                g.fillOval(x - 0.3 * r, y + 0.72 * r, 0.6 * r, 0.2 * r);
                clouds(g, seed, x, y, r, 7, detail);
            }
            case BANDED -> {
                bands(g, palette.bands, 0.85, x, y, r, -0.8, 0.2, 9, 0.17, 0.11);
                if (detail) ripples(g, palette, x, y, r, -0.7, 0.2, 8);
                // Une tempête : une tache ovale sur une bande, et son œil.
                g.setFill(palette.storm);
                g.fillOval(x - 0.04 * r, y + 0.1 * r, 0.48 * r, 0.26 * r);
                g.setFill(palette.stormEye);
                g.fillOval(x + 0.07 * r, y + 0.165 * r, 0.26 * r, 0.13 * r);
                if (detail) {
                    // La tempête tourne : un arc clair presque fermé, entre son bord et son œil.
                    g.setStroke(palette.stormSwirl);
                    g.setLineWidth(Math.max(0.75, 0.02 * r));
                    g.setLineCap(StrokeLineCap.ROUND);
                    g.strokeArc(x - 0.005 * r, y + 0.125 * r, 0.41 * r, 0.21 * r, 200, 250, ArcType.OPEN);
                    g.setLineCap(StrokeLineCap.SQUARE);
                }
            }
            case RINGED -> {
                bands(g, palette.bands, 0.4, x, y, r, -0.72, 0.24, 7, 0.12, 0.12);
                if (detail) ripples(g, palette, x, y, r, -0.6, 0.24, 6);
            }
            case GLOWING, BEAMING, VOID -> { }
        }
    }

    /**
     * Les détails d'un astre qui n'est pas rond, tous dans le disque de rayon {@code r} centré en
     * (x, y), plus petit que lui : ils restent sur lui.
     *
     * @param detail vrai pour les couches de détail, décidé sur le rayon de l'astre entier, pas sur {@code r}
     */
    static void rough(GraphicsContext g, Body.Look look, Palette palette, double x, double y, double r, boolean detail) {
        switch (look) {
            case ROCKY -> craters(g, palette, 5, 0.1, 0.14, 0.7, x, y, r, detail);
            case ICY -> {
                cracks(g, palette.seed, palette.fissure, 0.05, x, y, r, 4, 0);
                g.setFill(FROST);
                spots(g, palette.seed, 940, 4, 0.06, 0.08, 0.8, x, y, r);
            }
            case MOLTEN -> {
                veins(g, palette, x, y, r, 4);
                embers(g, palette, 2, 0.12, 0.1, 0.65, x, y, r);
            }
            case DARK -> {
                g.setFill(palette.speck);
                spots(g, palette.seed, 500, 8, 0.04, 0.05, 0.78, x, y, r);
            }
            case HAZY, OCEAN, BANDED, RINGED, GLOWING, BEAMING, VOID -> { }
        }
    }

    /**
     * Des ronds de la couleur en cours, semés dans le disque de rayon {@code r}.
     *
     * @param least  le rayon du plus petit, en rayons du disque ; {@code more} ce que le plus gros a en plus
     * @param spread jusqu'où va le centre d'un rond, en rayons du disque
     */
    static void spots(GraphicsContext g, int seed, int rank, int count, double least, double more, double spread,
            double x, double y, double r) {
        for (int spot = 0; spot < count; spot++) {
            double size = (least + more * chance(seed, rank + spot)) * r;
            double away = spread * r * Math.sqrt(chance(seed, rank + 10 + spot));
            double angle = 2 * Math.PI * chance(seed, rank + 20 + spot);
            g.fillOval(x + away * Math.cos(angle) - size, y + away * Math.sin(angle) - size, 2 * size, 2 * size);
        }
    }

    /**
     * Des cratères : un bord éclairé, en bas à droite, sous un fond sombre. En détail, et dès qu'un
     * cratère fait deux pixels, le creux s'ombre du côté du soleil et le bord d'en face prend la lumière.
     */
    static void craters(GraphicsContext g, Palette palette, int count, double least, double more, double spread,
            double x, double y, double r, boolean detail) {
        g.setLineCap(StrokeLineCap.ROUND);
        for (int crater = 0; crater < count; crater++) {
            double size = (least + more * chance(palette.seed, 300 + crater)) * r;
            double away = spread * r * Math.sqrt(chance(palette.seed, 310 + crater));
            double angle = 2 * Math.PI * chance(palette.seed, 320 + crater);
            double cx = x + away * Math.cos(angle);
            double cy = y + away * Math.sin(angle);
            g.setFill(palette.rim);
            g.fillOval(cx - 0.82 * size, cy - 0.82 * size, 2 * size, 2 * size);
            g.setFill(palette.floor);
            g.fillOval(cx - size, cy - size, 1.76 * size, 1.76 * size);
            if (!detail || size < 2) continue;
            // L'ombre dans le creux, en haut à gauche, tout entière sur le fond ; puis l'arc éclairé, sur le bord d'en face.
            g.setFill(CRATER_SHADE);
            g.fillOval(cx - 0.98 * size, cy - 0.98 * size, 1.3 * size, 1.3 * size);
            g.setStroke(palette.rimLit);
            g.setLineWidth(Math.max(0.75, 0.16 * size));
            g.strokeArc(cx - 0.9 * size, cy - 0.9 * size, 1.8 * size, 1.8 * size, 270, 100, ArcType.OPEN);
        }
        g.setLineCap(StrokeLineCap.SQUARE);
    }

    /** Des foyers : des ronds qui brillent au centre et s'effacent au bord. */
    static void embers(GraphicsContext g, Palette palette, int count, double least, double more, double spread,
            double x, double y, double r) {
        for (int spot = 0; spot < count; spot++) {
            double size = (least + more * chance(palette.seed, 400 + spot)) * r;
            double away = spread * r * Math.sqrt(chance(palette.seed, 410 + spot));
            double angle = 2 * Math.PI * chance(palette.seed, 420 + spot);
            double cx = x + away * Math.cos(angle);
            double cy = y + away * Math.sin(angle);
            g.setFill(new RadialGradient(0, 0, cx, cy, size, false, CycleMethod.NO_CYCLE, palette.ember));
            g.fillOval(cx - size, cy - size, 2 * size, 2 * size);
        }
    }

    /** Des veines qui luisent : chaque fissure deux fois, large et sourde puis fine et vive. */
    static void veins(GraphicsContext g, Palette palette, double x, double y, double r, int count) {
        cracks(g, palette.seed, palette.glow, 0.09, x, y, r, count, 0);
        cracks(g, palette.seed, palette.vein, 0.028, x, y, r, count, 0);
    }

    /** Des fissures : des traits coudés entre deux points pris dans le disque. */
    static void cracks(GraphicsContext g, int seed, Color color, double width, double x, double y, double r, int count, int rank) {
        g.setStroke(color);
        g.setLineWidth(Math.max(0.75, width * r));
        g.setLineCap(StrokeLineCap.ROUND);
        g.setLineJoin(StrokeLineJoin.ROUND);
        for (int crack = rank; crack < rank + count; crack++) {
            double from = 2 * Math.PI * chance(seed, 700 + crack);
            double to = from + Math.PI * (0.5 + chance(seed, 710 + crack));
            double start = 0.8 * r * (0.4 + 0.6 * chance(seed, 720 + crack));
            double end = 0.8 * r * (0.4 + 0.6 * chance(seed, 730 + crack));
            XS[0] = x + start * Math.cos(from);
            YS[0] = y + start * Math.sin(from);
            XS[2] = x + end * Math.cos(to);
            YS[2] = y + end * Math.sin(to);
            // Le coude : le milieu, poussé de côté d'un quart de la longueur au plus.
            double bend = 0.5 * (chance(seed, 740 + crack) - 0.5);
            XS[1] = (XS[0] + XS[2]) / 2 - bend * (YS[2] - YS[0]);
            YS[1] = (YS[0] + YS[2]) / 2 + bend * (XS[2] - XS[0]);
            g.strokePolyline(XS, YS, 3);
        }
        g.setLineJoin(StrokeLineJoin.MITER);
        g.setLineCap(StrokeLineCap.SQUARE);
    }

    /**
     * Des bandes horizontales en travers d'un disque : chacune est un trait aux bouts ronds, juste
     * assez court pour ne pas dépasser du disque à sa hauteur.
     *
     * @param top   la hauteur de la première bande, en rayons, de -1 (en haut) à 1 (en bas) ; {@code step} l'écart entre deux bandes
     * @param thick l'épaisseur des bandes de rang pair, en rayons ; {@code thin} celle des autres
     */
    static void bands(GraphicsContext g, Color[] colors, double alpha, double x, double y, double r, double top, double step,
            int count, double thick, double thin) {
        g.setLineCap(StrokeLineCap.ROUND);
        g.setGlobalAlpha(alpha);
        for (int band = 0; band < count; band++) {
            double wide = (band % 2 == 0 ? thick : thin) * r;
            double dy = (top + band * step) * r;
            double half = Math.sqrt(Math.max(0, r * r - dy * dy)) - 0.75 * wide;
            if (half <= 0) continue;
            g.setLineWidth(Math.max(1, wide));
            g.setStroke(colors[band % colors.length]);
            g.strokeLine(x - half, y + dy, x + half, y + dy);
        }
        g.setGlobalAlpha(1);
        g.setLineCap(StrokeLineCap.SQUARE);
    }

    /**
     * Des nuages : de courtes traînées claires, chacune à sa hauteur et pas plus longue que le disque
     * n'est large à cet endroit. En détail, chacune porte d'abord son ombre sur la mer, un peu en bas
     * à droite, rognée s'il le faut pour rester dans le disque.
     */
    static void clouds(GraphicsContext g, int seed, double x, double y, double r, int count, boolean detail) {
        g.setLineWidth(Math.max(1, 0.07 * r));
        g.setLineCap(StrokeLineCap.ROUND);
        for (int pass = detail ? 0 : 1; pass < 2; pass++) {
            double shift = pass == 0 ? 0.06 * r : 0;
            g.setStroke(pass == 0 ? CLOUD_SHADOW : CLOUD);
            for (int cloud = 0; cloud < count; cloud++) {
                double dy = (-0.62 + 1.24 * (cloud + chance(seed, 800 + cloud)) / count) * r;
                double half = Math.sqrt(Math.max(0, r * r - dy * dy)) - 0.08 * r;
                if (half <= 0) continue;
                double length = (0.3 + 0.4 * chance(seed, 810 + cloud)) * half;
                double middle = (2 * chance(seed, 820 + cloud) - 1) * (half - length);
                // L'ombre : la même traînée, décalée, et pas plus large que le disque à sa propre hauteur.
                double room = Math.sqrt(Math.max(0, r * r - (dy + shift) * (dy + shift))) - 0.08 * r;
                double from = Math.max(-room, middle + shift - length);
                double to = Math.min(room, middle + shift + length);
                if (from >= to) continue;
                g.strokeLine(x + from, y + dy + shift, x + to, y + dy + shift);
            }
        }
        g.setLineCap(StrokeLineCap.SQUARE);
    }

    /**
     * Les remous entre les bandes d'une géante : de fins traits qui ondulent, chacun à sa hauteur,
     * jamais plus longs que le disque n'est large là où ils montent le plus.
     *
     * @param top la hauteur du premier, en rayons, de -1 (en haut) à 1 (en bas) ; {@code step} l'écart entre deux
     */
    static void ripples(GraphicsContext g, Palette palette, double x, double y, double r, double top, double step, int count) {
        g.setStroke(palette.ripple);
        g.setLineWidth(Math.max(0.75, 0.025 * r));
        g.setLineCap(StrokeLineCap.ROUND);
        g.setLineJoin(StrokeLineJoin.ROUND);
        int points = 12;
        double sway = 0.02 * r;
        for (int ripple = 0; ripple < count; ripple++) {
            double dy = (top + ripple * step) * r;
            double high = Math.abs(dy) + sway;
            double half = Math.sqrt(Math.max(0, r * r - high * high)) - 0.08 * r;
            if (half <= 0) continue;
            double phase = 2 * Math.PI * chance(palette.seed, 950 + ripple);
            for (int point = 0; point <= points; point++) {
                double along = 2.0 * point / points - 1;
                XS[point] = x + along * half;
                YS[point] = y + dy + sway * Math.sin(phase + along * (3 + ripple % 2) * Math.PI);
            }
            g.strokePolyline(XS, YS, points + 1);
        }
        g.setLineJoin(StrokeLineJoin.MITER);
        g.setLineCap(StrokeLineCap.SQUARE);
    }
}
