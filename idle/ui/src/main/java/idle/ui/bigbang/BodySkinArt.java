package idle.ui.bigbang;

import idle.core.sky.Body;
import idle.core.universe.Skin;
import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

/**
 * Les trois skins des astres (version 0.186, {@link Skin#BODIES}) : une autre apparence pour chacun
 * des astres que {@link BodyArt} dessine, dans la même emprise que lui ({@link BodyArt#span},
 * {@link BodyArt#reachDown}…), sans ses couches de détail, qui sont faites pour le Classique.
 *
 * <p><b>Planétarium</b> : chaque astre est une sphère posée sur une fine tige de laiton qui descend
 * jusqu'au bas de son emprise, sur un petit socle. Les mondes solides, les cailloux et le noyau
 * d'une comète sont de laiton ({@link #BRASS}), les gaz, les nébuleuses et les étoiles de verre
 * dépoli ({@link #GLASS}) teinté de leur couleur, lumineux pour ce qui brille ; le trou noir est une
 * sphère d'obsidienne cerclée de laiton. Les textures laissent la place à deux ou trois reflets
 * nets. Les accessoires gardent la silhouette de chaque échelle : les anneaux d'une géante et le
 * disque d'un trou noir deviennent un anneau de laiton plat, les queues d'une comète un arc de
 * laiton gravé, les faisceaux d'une étoile à neutrons et les jets du cœur des aiguilles de laiton,
 * la chevelure d'une comète un globe de verre autour du noyau. Un globe sans accessoire n'a pas de
 * place sous lui : sa sphère est un peu plus petite que lui pour laisser passer la tige.
 *
 * <p><b>Gravure</b> : du noir ({@link #INK}) sur du blanc cassé ({@link #PAPER}), comme une planche
 * du XIX<sup>e</sup> siècle. Un astre est un disque de papier cerné d'un trait d'un pixel et demi,
 * modelé par des hachures parallèles qui se resserrent vers l'ombre, en bas à droite, et se croisent
 * dans le plus sombre ; cratères, continents, fissures et bandes sont des traits. Ce qui s'écarte du
 * disque est tracé en blanc cassé, pour se voir sur le ciel : les rayons d'une étoile, les queues
 * d'une comète en traits fins qui s'écartent, les anneaux d'une géante en ellipses et en hachures,
 * l'anneau de hachures circulaires autour du disque noir d'un trou noir, le cercle pointillé du halo
 * des mondes à air. Une nébuleuse est un disque de papier pointillé.
 *
 * <p><b>Cartographie</b> : des globes vus un peu d'en haut, de la couleur de l'astre éclaircie, à
 * méridiens et parallèles ({@link #LINE}), l'équateur plus épais ; un graticule doré pour les
 * étoiles, clair sur fond sombre pour les trous noirs. Les anneaux et le disque restent, en traits ;
 * une comète a une flèche de trajectoire pointillée vers ses queues. Sous l'équateur, dans le globe,
 * un cartouche porte le nom de l'astre en petites capitales, quand le globe est assez grand
 * ({@link #NAME_FROM}) et le nom assez court pour y tenir.
 *
 * <p>Rien n'est alloué par image : les gradients sont proportionnels à la forme qu'ils remplissent
 * et construits une fois, les pointes des polygones vivent dans des tableaux statiques, les polices
 * sont gardées, le nom en capitales est dans la palette de l'astre.
 */
final class BodySkinArt {

    /** Le laiton du Planétarium : sa couleur, son ombre, son reflet ; le verre dépoli ; l'obsidienne. */
    static final Color BRASS = Color.web("#c9a24a");
    static final Color BRASS_SHADE = Color.web("#6e5420");
    static final Color BRASS_LIGHT = Color.web("#fff2c8");
    static final Color GLASS = Color.web("#dfe8f0");
    private static final Color OBSIDIAN = Color.web("#16161c");
    /** Le papier et l'encre de la Gravure. */
    static final Color PAPER = Color.web("#f1ead8");
    static final Color INK = Color.web("#1a1612");
    /** Le trait de la Cartographie, le même pâli pour les globes sombres, et l'or des étoiles. */
    static final Color LINE = Color.web("#2c3e50");
    private static final Color LINE_PALE = BodyArt.fade(GLASS, 0.85);
    private static final Color GOLD = BRASS;
    /** Rayon du globe, en pixels, à partir duquel la Cartographie écrit le nom de l'astre. */
    static final double NAME_FROM = 14;
    /** L'inclinaison du globe de la Cartographie vers le spectateur, en radians : on voit un peu le pôle. */
    private static final double TILT = 0.4;
    /** La sphère d'un globe sans accessoire du Planétarium, en rayons : le reste est pour la tige. */
    private static final double BALL = 0.82;
    /** La chevelure d'une comète du Planétarium : un globe de verre, en rayons de son dessin. */
    private static final double COMA_GLASS = 1.1;
    /** Le bout de l'arc de laiton d'une comète et de la flèche de la Cartographie, en rayons, le long des queues. */
    private static final double TAIL_END = 4.1;

    private static final Color GLINT = BodyArt.fade(Color.WHITE, 0.82);
    private static final Color GLINT_SOFT = BodyArt.fade(BRASS_LIGHT, 0.45);
    private static final Color GLASS_RIM = BodyArt.fade(GLASS, 0.7);
    private static final Color GLASS_GLINT = BodyArt.fade(Color.WHITE, 0.7);
    private static final Color GLASS_GLINT_SOFT = BodyArt.fade(Color.WHITE, 0.3);
    private static final Color AIR_LINE = BodyArt.fade(LINE, 0.55);
    /** Les gradients du laiton, proportionnels à la forme : une sphère éclairée d'en haut à gauche, une pièce plate polie, une tige. */
    private static final RadialGradient BRASS_BALL = new RadialGradient(0, 0, 0.36, 0.34, 0.76, true, CycleMethod.NO_CYCLE,
            new Stop(0, BRASS_LIGHT), new Stop(0.22, BRASS), new Stop(1, BRASS_SHADE));
    private static final RadialGradient OBSIDIAN_BALL = new RadialGradient(0, 0, 0.36, 0.34, 0.76, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#4a4a56")), new Stop(0.3, OBSIDIAN), new Stop(1, Color.BLACK));
    private static final LinearGradient BRASS_FLAT = new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
            new Stop(0, BRASS_SHADE), new Stop(0.3, BRASS), new Stop(0.55, BRASS_LIGHT), new Stop(0.8, BRASS), new Stop(1, BRASS_SHADE));
    private static final LinearGradient BRASS_ROD = new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
            new Stop(0, BRASS_SHADE), new Stop(0.35, BRASS_LIGHT), new Stop(0.6, BRASS), new Stop(1, BRASS_SHADE));

    /** Les pointes des polygones en cours de dessin : assez pour les deux bords d'un demi-anneau. */
    private static final double[] PX = new double[2 * BodyArt.ARC + 2];
    private static final double[] PY = new double[2 * BodyArt.ARC + 2];
    /** Les polices du cartouche, de 9 à 11 pixels, construites à la première demande : la boîte à outils doit être lancée. */
    private static final Font[] SERIF = new Font[3];

    private BodySkinArt() {}

    /** Dessine un astre centré en (x, y), de rayon {@code r} (au sens de {@link BodyArt#draw}), dans l'un des trois skins. */
    static void draw(GraphicsContext g, Body body, BodyArt.Palette palette, double x, double y, double r, Skin skin) {
        g.setLineCap(StrokeLineCap.ROUND);
        g.setLineJoin(StrokeLineJoin.ROUND);
        switch (skin) {
            case BODY_PLANETARIUM -> planetarium(g, body, palette, x, y, r);
            case BODY_ENGRAVING -> engraving(g, body, palette, x, y, r);
            default -> cartography(g, body, palette, x, y, r);
        }
        g.setLineCap(StrokeLineCap.SQUARE);
        g.setLineJoin(StrokeLineJoin.MITER);
    }

    /** Vrai pour un astre fait de gaz : de verre au Planétarium. */
    private static boolean gaseous(Body.Look look) {
        return look == Body.Look.HAZY || look == Body.Look.BANDED || look == Body.Look.RINGED;
    }

    // ------------------------------------------------------------------ Planétarium

    private static void planetarium(GraphicsContext g, Body body, BodyArt.Palette palette, double x, double y, double r) {
        Body.Look look = body.look();
        int seed = palette.seed;
        double bottom = y + BodyArt.reachDown(body) * r;
        switch (body.tier()) {
            case RUBBLE -> {
                stand(g, x, y + 0.5 * r, bottom, r);
                // Les mêmes cailloux qu'en Classique, en billes de laiton : six autour, le plus gros au milieu, par-dessus.
                for (int stone = 6; stone >= 0; stone--) {
                    double away = stone == 0 ? 0 : (0.55 + 0.18 * BodyArt.chance(seed, stone * 3)) * r;
                    double angle = stone * Math.PI / 3 + BodyArt.chance(seed, stone * 3 + 1);
                    double size = (stone == 0 ? 0.5 : 0.3 + 0.16 * BodyArt.chance(seed, stone * 3 + 2)) * r;
                    brass(g, x + away * Math.cos(angle), y + away * Math.sin(angle), size);
                }
            }
            case COMET -> {
                brassArc(g, x, y, r);
                stand(g, x, y + COMA_GLASS * r, bottom, r);
                brass(g, x, y, 0.6 * r);
                glass(g, palette.glass, x, y, COMA_GLASS * r);
            }
            case ASTEROID -> {
                stand(g, x, y + BALL * r, bottom, r);
                brass(g, x, y, BALL * r);
            }
            case MOON, PLANET -> {
                boolean ringed = look == Body.Look.RINGED;
                if (ringed) flatRing(g, x, y, BodyArt.RINGS[0] * r, BodyArt.RINGS[2] * r, BodyArt.RING_TILT, Math.PI);
                stand(g, x, y + BALL * r, bottom, r);
                if (gaseous(look)) glass(g, palette.glass, x, y, BALL * r); else brass(g, x, y, BALL * r);
                if (ringed) flatRing(g, x, y, BodyArt.RINGS[0] * r, BodyArt.RINGS[2] * r, BodyArt.RING_TILT, 0);
            }
            case STAR, REMNANT -> {
                if (look == Body.Look.BEAMING) {
                    brassPulsar(g, palette, x, y, r, bottom);
                } else {
                    stand(g, x, y + r, bottom, r);
                    glass(g, palette.lamp, x, y, r);
                }
            }
            case BLACK_HOLE -> brassHole(g, x, y, r, bottom, false);
            case CORE -> brassHole(g, x, y, r, bottom, true);
            case EXOTIC -> {
                switch (look) {
                    case BEAMING -> brassPulsar(g, palette, x, y, r, bottom);
                    case NEBULA -> {
                        stand(g, x, y + r, bottom, r);
                        glass(g, palette.lamp, x, y, r);
                    }
                    default -> brassHole(g, x, y, r, bottom, true);
                }
            }
        }
    }

    /** Une étoile à neutrons : deux aiguilles de laiton le long de ses faisceaux, et une petite lampe de verre entre elles. */
    private static void brassPulsar(GraphicsContext g, BodyArt.Palette palette, double x, double y, double r, double bottom) {
        needle(g, x, y, 0.5 * r, (BodyArt.BEAM_LENGTH - 0.1) * r, 0.12 * r, BodyArt.BEAM);
        needle(g, x, y, 0.5 * r, (BodyArt.BEAM_LENGTH - 0.1) * r, 0.12 * r, BodyArt.BEAM + Math.PI);
        stand(g, x, y + 0.5 * r, bottom, r);
        glass(g, palette.lamp, x, y, 0.5 * r);
    }

    /**
     * Un trou noir : son disque en anneau de laiton plat, la moitié derrière puis la moitié devant,
     * une sphère d'obsidienne cerclée de laiton ; pour le cœur de galaxie, une aiguille vers le haut,
     * et la tige, qui descend aussi loin, fait l'autre jet.
     */
    private static void brassHole(GraphicsContext g, double x, double y, double r, double bottom, boolean jets) {
        flatRing(g, x, y, BodyArt.DISC[0] * r, BodyArt.DISC[3] * r, BodyArt.DISC_TILT, Math.PI);
        if (jets) needle(g, x, y, r, (BodyArt.JET_LENGTH - 0.05) * r, 0.14 * r, -Math.PI / 2);
        stand(g, x, y + r, bottom, r);
        g.setFill(OBSIDIAN_BALL);
        g.fillOval(x - r, y - r, 2 * r, 2 * r);
        g.setFill(GLINT);
        g.fillOval(x - 0.6 * r, y - 0.64 * r, 0.26 * r, 0.14 * r);
        g.setStroke(BRASS);
        g.setLineWidth(Math.max(1, 0.07 * r));
        g.strokeOval(x - 1.06 * r, y - 1.06 * r, 2.12 * r, 2.12 * r);
        flatRing(g, x, y, BodyArt.DISC[0] * r, BodyArt.DISC[3] * r, BodyArt.DISC_TILT, 0);
    }

    /** Une sphère de laiton de rayon {@code r} : son modelé, un reflet net en haut à gauche, un reflet doux au bord opposé. */
    private static void brass(GraphicsContext g, double x, double y, double r) {
        g.setFill(BRASS_BALL);
        g.fillOval(x - r, y - r, 2 * r, 2 * r);
        g.setFill(GLINT);
        g.fillOval(x - 0.6 * r, y - 0.64 * r, 0.3 * r, 0.17 * r);
        if (r < 3) return;
        g.setStroke(GLINT_SOFT);
        g.setLineWidth(Math.max(0.75, 0.06 * r));
        g.strokeArc(x - 0.88 * r, y - 0.88 * r, 1.76 * r, 1.76 * r, -15, -85, ArcType.OPEN);
    }

    /**
     * Une sphère de verre dépoli de rayon {@code r} : son remplissage, teinté ou lumineux selon le
     * gradient, un fin bord de verre, un reflet net et un reflet doux.
     */
    private static void glass(GraphicsContext g, Paint fill, double x, double y, double r) {
        g.setFill(fill);
        g.fillOval(x - r, y - r, 2 * r, 2 * r);
        g.setStroke(GLASS_RIM);
        g.setLineWidth(Math.max(0.75, 0.03 * r));
        g.strokeOval(x - r, y - r, 2 * r, 2 * r);
        g.setFill(GLASS_GLINT);
        g.fillOval(x - 0.58 * r, y - 0.66 * r, 0.34 * r, 0.2 * r);
        if (r < 3) return;
        g.setFill(GLASS_GLINT_SOFT);
        g.fillOval(x + 0.2 * r, y + 0.3 * r, 0.12 * r, 0.12 * r);
    }

    /**
     * La tige de laiton et son socle, de {@code top} (le bas de la sphère) à {@code bottom} (le bas
     * de l'emprise) ; rien s'il n'y a pas un pixel de place.
     */
    private static void stand(GraphicsContext g, double x, double top, double bottom, double r) {
        double high = bottom - top;
        if (high < 1) return;
        double stem = Math.max(1, 0.05 * r);
        double plinth = Math.min(0.14 * r, 0.6 * high);
        double wide = Math.max(stem, Math.min(0.7 * r, 5 * plinth));
        g.setFill(BRASS_ROD);
        g.fillRect(x - stem / 2, top - 1, stem, high - plinth / 2 + 1);
        g.setFill(BRASS_FLAT);
        g.fillOval(x - wide / 2, bottom - plinth, wide, plinth);
        g.setStroke(BRASS_SHADE);
        g.setLineWidth(0.75);
        g.strokeOval(x - wide / 2, bottom - plinth, wide, plinth);
    }

    /**
     * La moitié, à partir de l'angle {@code from}, d'un anneau de laiton plat et aplati, de
     * {@code inner} à {@code outer} : un polygone fermé par ses deux bords.
     */
    private static void flatRing(GraphicsContext g, double x, double y, double inner, double outer, double tilt, double from) {
        int arc = BodyArt.ARC;
        for (int step = 0; step <= arc; step++) {
            double angle = from + Math.PI * step / arc;
            PX[step] = x + outer * Math.cos(angle);
            PY[step] = y + tilt * outer * Math.sin(angle);
            PX[2 * arc + 1 - step] = x + inner * Math.cos(angle);
            PY[2 * arc + 1 - step] = y + tilt * inner * Math.sin(angle);
        }
        g.setFill(BRASS_FLAT);
        g.fillPolygon(PX, PY, 2 * arc + 2);
        g.setStroke(BRASS_SHADE);
        g.setLineWidth(0.75);
        g.strokePolygon(PX, PY, 2 * arc + 2);
    }

    /** Une aiguille de laiton, de {@code near} à {@code far} du centre dans la direction {@code angle}, large de {@code width} à sa base. */
    private static void needle(GraphicsContext g, double x, double y, double near, double far, double width, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        PX[0] = x + near * cos - width / 2 * -sin;
        PY[0] = y + near * sin - width / 2 * cos;
        PX[1] = x + far * cos;
        PY[1] = y + far * sin;
        PX[2] = x + near * cos + width / 2 * -sin;
        PY[2] = y + near * sin + width / 2 * cos;
        g.setFill(BRASS);
        g.fillPolygon(PX, PY, 3);
        g.setStroke(BRASS_SHADE);
        g.setLineWidth(0.6);
        g.strokePolygon(PX, PY, 3);
    }

    /**
     * Les queues d'une comète : un arc de laiton qui part du noyau le long des queues, bombé du côté
     * de la queue de poussière, de plus en plus fin, gravé d'un sillon au milieu. Il reste dans les
     * deux queues du Classique.
     */
    private static void brassArc(GraphicsContext g, double x, double y, double r) {
        double cos = Math.cos(BodyArt.TAIL);
        double sin = Math.sin(BodyArt.TAIL);
        int steps = 12;
        for (int step = 0; step <= steps; step++) {
            double along = step / (double) steps;
            double far = (0.95 + (TAIL_END - 0.95) * along) * r;
            double bulge = 0.3 * r * Math.sin(Math.PI * along);
            double half = (0.3 - 0.24 * along) * r;
            double cx = x + far * cos - bulge * sin;
            double cy = y + far * sin + bulge * cos;
            PX[step] = cx - half * sin;
            PY[step] = cy + half * cos;
            PX[2 * steps + 1 - step] = cx + half * sin;
            PY[2 * steps + 1 - step] = cy - half * cos;
        }
        g.setFill(BRASS_FLAT);
        g.fillPolygon(PX, PY, 2 * steps + 2);
        g.setStroke(BRASS_SHADE);
        g.setLineWidth(0.75);
        g.strokePolygon(PX, PY, 2 * steps + 2);
        // Le sillon : le milieu des deux bords.
        for (int step = 0; step <= steps; step++) {
            PX[step] = (PX[step] + PX[2 * steps + 1 - step]) / 2;
            PY[step] = (PY[step] + PY[2 * steps + 1 - step]) / 2;
        }
        g.strokePolyline(PX, PY, steps + 1);
    }

    // ------------------------------------------------------------------ Gravure

    private static void engraving(GraphicsContext g, Body body, BodyArt.Palette palette, double x, double y, double r) {
        Body.Look look = body.look();
        int seed = palette.seed;
        switch (body.tier()) {
            case RUBBLE -> {
                g.setFill(PAPER);
                for (int pebble = 0; pebble < 5; pebble++) {
                    double away = (0.95 + 0.1 * BodyArt.chance(seed, 50 + pebble)) * r;
                    double angle = 2 * Math.PI * BodyArt.chance(seed, 60 + pebble);
                    double size = (0.07 + 0.05 * BodyArt.chance(seed, 70 + pebble)) * r;
                    g.fillOval(x + away * Math.cos(angle) - size, y + away * Math.sin(angle) - size, 2 * size, 2 * size);
                }
                for (int stone = 6; stone >= 0; stone--) {
                    double away = stone == 0 ? 0 : (0.55 + 0.18 * BodyArt.chance(seed, stone * 3)) * r;
                    double angle = stone * Math.PI / 3 + BodyArt.chance(seed, stone * 3 + 1);
                    double size = (stone == 0 ? 0.5 : 0.3 + 0.16 * BodyArt.chance(seed, stone * 3 + 2)) * r;
                    hatched(g, x + away * Math.cos(angle), y + away * Math.sin(angle), size);
                }
                marks(g, look, seed, x, y, 0.3 * r);
            }
            case COMET -> {
                // Les queues : des traits fins qui s'écartent, de la chevelure jusque dans les deux queues du Classique.
                g.setStroke(PAPER);
                g.setLineWidth(Math.max(0.6, 0.03 * r));
                for (int line = 0; line < 6; line++) {
                    double angle = BodyArt.TAIL - 0.17 + 0.1 * line;
                    double far = (3.2 + 0.9 * BodyArt.chance(seed, 1200 + line)) * r;
                    g.strokeLine(x + 0.8 * r * Math.cos(angle), y + 0.8 * r * Math.sin(angle), x + far * Math.cos(angle), y + far * Math.sin(angle));
                }
                dots(g, PAPER, x, y, 1.2 * r, 1.2 * r, r);
                hatched(g, x, y, 0.6 * r);
                marks(g, look, seed, x, y, 0.36 * r);
            }
            case ASTEROID -> {
                hatched(g, x, y, r);
                marks(g, look, seed, x, y, 0.6 * r);
            }
            case MOON, PLANET -> {
                boolean ringed = look == Body.Look.RINGED;
                double air = BodyArt.air(look) * r;
                if (air > 0) dots(g, PAPER, x, y, air, air, 1.5 * r);
                if (ringed) inkRings(g, x, y, r, Math.PI);
                hatched(g, x, y, r);
                marks(g, look, seed, x, y, r);
                if (ringed) inkRings(g, x, y, r, 0);
            }
            case STAR, REMNANT -> {
                if (look == Body.Look.BEAMING) inkPulsar(g, x, y, r); else inkStar(g, look, seed, x, y, r);
            }
            case BLACK_HOLE -> inkHole(g, x, y, r, false);
            case CORE -> inkHole(g, x, y, r, true);
            case EXOTIC -> {
                switch (look) {
                    case BEAMING -> inkPulsar(g, x, y, r);
                    case NEBULA -> stipple(g, seed, x, y, r);
                    default -> inkHole(g, x, y, r, true);
                }
            }
        }
    }

    /** Un disque de papier modelé de hachures, cerné d'un trait d'un pixel et demi ; trop petit pour des hachures, il reste blanc. */
    private static void hatched(GraphicsContext g, double x, double y, double r) {
        g.setFill(PAPER);
        g.fillOval(x - r, y - r, 2 * r, 2 * r);
        if (r >= 4) hatch(g, x, y, r);
        g.setStroke(INK);
        g.setLineWidth(1.5);
        g.strokeOval(x - r, y - r, 2 * r, 2 * r);
    }

    /**
     * Les hachures d'un disque : des traits parallèles, perpendiculaires à la lumière (qui vient d'en
     * haut à gauche), qui commencent un peu avant le milieu et se resserrent vers le bord sombre, en
     * bas à droite ; dans l'ombre, d'autres les croisent.
     */
    private static void hatch(GraphicsContext g, double x, double y, double r) {
        double d = Math.sqrt(0.5);     // la direction de l'ombre : (d, d) ; celle des traits : (d, -d)
        g.setStroke(INK);
        g.setLineWidth(Math.max(0.6, 0.02 * r));
        int lines = (int) Math.min(18, Math.max(4, Math.round(r / 2.2)));
        for (int line = 0; line < lines; line++) {
            double u = line / (double) (lines - 1);
            double t = r * (-0.35 + 1.3 * Math.pow(u, 0.75));
            double half = Math.sqrt(Math.max(0, r * r - t * t));
            g.strokeLine(x + t * d - half * d, y + t * d + half * d, x + t * d + half * d, y + t * d - half * d);
        }
        int cross = (int) Math.min(12, Math.max(3, Math.round(r / 3)));
        for (int line = 0; line < cross; line++) {
            double v = r * (-0.9 + 1.8 * line / (cross - 1.0));
            double reach = Math.sqrt(Math.max(0, r * r - v * v));
            double start = 0.45 * r;
            if (reach <= start) continue;
            g.strokeLine(x + start * d + v * d, y + start * d - v * d, x + reach * d + v * d, y + reach * d - v * d);
        }
    }

    /** Les marques de l'aspect d'un astre, en traits d'encre, dans le disque de rayon {@code r}. */
    private static void marks(GraphicsContext g, Body.Look look, int seed, double x, double y, double r) {
        if (r < 2) return;
        g.setStroke(INK);
        g.setFill(INK);
        switch (look) {
            case ROCKY -> {
                // Les cratères du Classique, en cercles, avec un arc d'ombre dans leur creux.
                g.setLineWidth(Math.max(0.6, 0.02 * r));
                for (int crater = 0; crater < 6; crater++) {
                    double size = (0.07 + 0.09 * BodyArt.chance(seed, 300 + crater)) * r;
                    if (size < 1) continue;
                    double away = 0.75 * r * Math.sqrt(BodyArt.chance(seed, 310 + crater));
                    double angle = 2 * Math.PI * BodyArt.chance(seed, 320 + crater);
                    double cx = x + away * Math.cos(angle);
                    double cy = y + away * Math.sin(angle);
                    g.strokeOval(cx - size, cy - size, 2 * size, 2 * size);
                    g.strokeArc(cx - 0.7 * size, cy - 0.7 * size, 1.4 * size, 1.4 * size, 100, 130, ArcType.OPEN);
                }
            }
            case ICY -> BodyArt.cracks(g, seed, INK, 0.02, x, y, r, 5, 0);
            case MOLTEN -> {
                BodyArt.cracks(g, seed, INK, 0.03, x, y, r, 5, 0);
                BodyArt.spots(g, seed, 400, 4, 0.03, 0.04, 0.7, x, y, r);
            }
            case DARK -> BodyArt.spots(g, seed, 500, 10, 0.02, 0.04, 0.85, x, y, r);
            case HAZY -> lines(g, x, y, r, -0.5, 0.5, 3);
            case OCEAN -> {
                // Les continents du Classique, en contours, et deux calottes en arcs.
                g.setLineWidth(Math.max(0.6, 0.025 * r));
                for (int land = 0; land < 4; land++) {
                    double away = 0.5 * r * BodyArt.chance(seed, 600 + land);
                    double angle = 2 * Math.PI * BodyArt.chance(seed, 610 + land);
                    double size = (0.2 + 0.14 * BodyArt.chance(seed, 620 + land)) * r;
                    outline(g, seed + 7 * land, x + away * Math.cos(angle), y + away * Math.sin(angle), size, 8);
                }
                g.strokeArc(x - 0.6 * r, y - 0.6 * r, 1.2 * r, 1.2 * r, 55, 70, ArcType.OPEN);
                g.strokeArc(x - 0.6 * r, y - 0.6 * r, 1.2 * r, 1.2 * r, 235, 70, ArcType.OPEN);
            }
            case BANDED -> lines(g, x, y, r, -0.7, 0.28, 6);
            case RINGED -> lines(g, x, y, r, -0.6, 0.3, 5);
            case GLOWING, BEAMING, VOID, NEBULA -> { }
        }
    }

    /** Des traits horizontaux à travers le disque, de la hauteur {@code top} (en rayons) tous les {@code step}, jamais plus longs que sa corde. */
    private static void lines(GraphicsContext g, double x, double y, double r, double top, double step, int count) {
        g.setLineWidth(Math.max(0.6, 0.02 * r));
        for (int line = 0; line < count; line++) {
            double dy = (top + line * step) * r;
            double half = Math.sqrt(Math.max(0, r * r - dy * dy)) - 0.06 * r;
            if (half <= 0) continue;
            g.strokeLine(x - half, y + dy, x + half, y + dy);
        }
    }

    /** Le contour d'un bloc cabossé : les mêmes pointes que celles de {@code BodyArt.lump}, tracées sans remplissage. */
    private static void outline(GraphicsContext g, int seed, double x, double y, double r, int points) {
        for (int point = 0; point < points; point++) {
            double angle = 2 * Math.PI * point / points + 0.3 * BodyArt.chance(seed, 100 + point);
            double away = (0.72 + 0.28 * BodyArt.chance(seed, 200 + point)) * r;
            PX[point] = x + away * Math.cos(angle);
            PY[point] = y + away * Math.sin(angle);
        }
        g.strokePolygon(PX, PY, points);
    }

    /** Une étoile : un disque blanc cerné, et des rayons hachurés dans sa couronne ; une naine brune garde ses hachures et ses bandes. */
    private static void inkStar(GraphicsContext g, Body.Look look, int seed, double x, double y, double r) {
        g.setStroke(PAPER);
        g.setLineWidth(Math.max(0.6, 0.025 * r));
        for (int ray = 0; ray < 24; ray++) {
            double angle = 2 * Math.PI * ray / 24;
            double far = (ray % 2 == 0 ? BodyArt.CORONA - 0.05 : 1.22) * r;
            g.strokeLine(x + 1.04 * r * Math.cos(angle), y + 1.04 * r * Math.sin(angle), x + far * Math.cos(angle), y + far * Math.sin(angle));
        }
        if (look == Body.Look.BANDED) {
            hatched(g, x, y, r);
            marks(g, look, seed, x, y, r);
            return;
        }
        g.setFill(PAPER);
        g.fillOval(x - r, y - r, 2 * r, 2 * r);
        g.setStroke(INK);
        g.setLineWidth(1.5);
        g.strokeOval(x - r, y - r, 2 * r, 2 * r);
    }

    /** Une étoile à neutrons : trois traits fins par faisceau, qui s'écartent à peine, et une petite bille blanche cernée. */
    private static void inkPulsar(GraphicsContext g, double x, double y, double r) {
        g.setStroke(PAPER);
        g.setLineWidth(Math.max(0.6, 0.02 * r));
        for (int side = 0; side < 2; side++) {
            for (int line = -1; line <= 1; line++) {
                double angle = BodyArt.BEAM + side * Math.PI + 0.06 * line;
                double far = (BodyArt.BEAM_LENGTH - 0.15 - 0.3 * Math.abs(line)) * r;
                g.strokeLine(x + 0.45 * r * Math.cos(angle), y + 0.45 * r * Math.sin(angle), x + far * Math.cos(angle), y + far * Math.sin(angle));
            }
        }
        double ball = 0.36 * r;
        g.setFill(PAPER);
        g.fillOval(x - ball, y - ball, 2 * ball, 2 * ball);
        g.setStroke(INK);
        g.setLineWidth(1.5);
        g.strokeOval(x - ball, y - ball, 2 * ball, 2 * ball);
    }

    /**
     * Un trou noir : l'anneau de hachures circulaires de son disque, la moitié derrière, l'arc de
     * lumière courbée au-dessus, le disque noir plein cerné de blanc, la moitié devant ; pour le cœur
     * de galaxie, trois traits fins par jet.
     */
    private static void inkHole(GraphicsContext g, double x, double y, double r, boolean jets) {
        g.setStroke(PAPER);
        if (jets) {
            g.setLineWidth(Math.max(0.6, 0.02 * r));
            for (int side = -1; side <= 1; side += 2) {
                for (int line = -1; line <= 1; line++) {
                    double far = (BodyArt.JET_LENGTH - 0.15 - 0.3 * Math.abs(line)) * r;
                    g.strokeLine(x + 0.08 * r * line, y + side * 1.05 * r, x + 0.2 * r * line, y + side * far);
                }
            }
        }
        ticks(g, x, y, BodyArt.DISC[0] * r, BodyArt.DISC[3] * r, BodyArt.DISC_TILT, Math.PI, 1);
        g.setLineWidth(Math.max(0.75, 0.03 * r));
        g.strokeArc(x - BodyArt.LENSED * r, y - BodyArt.LENSED * r, 2 * BodyArt.LENSED * r, 2 * BodyArt.LENSED * r, 0, 180, ArcType.OPEN);
        g.setFill(INK);
        g.fillOval(x - r, y - r, 2 * r, 2 * r);
        g.setLineWidth(1.5);
        g.strokeOval(x - r, y - r, 2 * r, 2 * r);
        ticks(g, x, y, BodyArt.DISC[0] * r, BodyArt.DISC[3] * r, BodyArt.DISC_TILT, 0, 1);
    }

    /** La moitié des anneaux d'une géante, en ellipses d'encre blanche et en hachures entre la première et la dernière. */
    private static void inkRings(GraphicsContext g, double x, double y, double r, double from) {
        g.setStroke(PAPER);
        g.setLineWidth(Math.max(0.6, 0.02 * r));
        for (double ring : BodyArt.RINGS) {
            for (int step = 0; step <= BodyArt.ARC; step++) {
                double angle = from + Math.PI * step / BodyArt.ARC;
                PX[step] = x + ring * r * Math.cos(angle);
                PY[step] = y + BodyArt.RING_TILT * ring * r * Math.sin(angle);
            }
            g.strokePolyline(PX, PY, BodyArt.ARC + 1);
        }
        ticks(g, x, y, BodyArt.RINGS[0] * r, BodyArt.RINGS[2] * r, BodyArt.RING_TILT, from, 2);
    }

    /** La moitié d'un anneau de hachures : un trait de l'ellipse intérieure à l'extérieure tous les {@code stride} pas d'arc. */
    private static void ticks(GraphicsContext g, double x, double y, double inner, double outer, double tilt, double from, int stride) {
        g.setLineWidth(Math.max(0.6, 0.015 * outer));
        for (int step = 0; step <= 2 * BodyArt.ARC; step += stride) {
            double angle = from + Math.PI * step / (2 * BodyArt.ARC);
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            g.strokeLine(x + inner * cos, y + tilt * inner * sin, x + outer * cos, y + tilt * outer * sin);
        }
    }

    /** Une nébuleuse : un disque de papier pointillé d'encre, plus dense au cœur, au bord en pointillé. */
    private static void stipple(GraphicsContext g, int seed, double x, double y, double r) {
        g.setFill(PAPER);
        g.fillOval(x - r, y - r, 2 * r, 2 * r);
        dots(g, INK, x, y, r, r, r);
        g.setFill(INK);
        int count = (int) Math.min(80, Math.max(12, 2 * r));
        for (int dot = 0; dot < count; dot++) {
            double away = 0.9 * r * BodyArt.chance(seed, 1300 + dot) * BodyArt.chance(seed, 1400 + dot);
            double angle = 2 * Math.PI * BodyArt.chance(seed, 1500 + dot);
            double size = Math.max(0.4, 0.015 * r);
            g.fillOval(x + away * Math.cos(angle) - size, y + away * Math.sin(angle) - size, 2 * size, 2 * size);
        }
    }

    /**
     * Un cercle (ou une ellipse) en pointillé : des points de la couleur donnée, espacés d'environ
     * trois pixels, ou moins quand {@code scale} est petit.
     */
    private static void dots(GraphicsContext g, Color color, double x, double y, double rx, double ry, double scale) {
        g.setFill(color);
        int count = (int) Math.min(96, Math.max(8, Math.round(2 * Math.PI * rx / 3)));
        double size = Math.max(0.4, Math.min(0.8, 0.03 * scale));
        for (int dot = 0; dot < count; dot++) {
            double angle = 2 * Math.PI * dot / count;
            g.fillOval(x + rx * Math.cos(angle) - size, y + ry * Math.sin(angle) - size, 2 * size, 2 * size);
        }
    }

    // ------------------------------------------------------------------ Cartographie

    private static void cartography(GraphicsContext g, Body body, BodyArt.Palette palette, double x, double y, double r) {
        Body.Look look = body.look();
        int seed = palette.seed;
        switch (body.tier()) {
            case RUBBLE -> {
                g.setFill(LINE);
                for (int pebble = 0; pebble < 5; pebble++) {
                    double away = (0.95 + 0.1 * BodyArt.chance(seed, 50 + pebble)) * r;
                    double angle = 2 * Math.PI * BodyArt.chance(seed, 60 + pebble);
                    double size = (0.07 + 0.05 * BodyArt.chance(seed, 70 + pebble)) * r;
                    g.fillOval(x + away * Math.cos(angle) - size, y + away * Math.sin(angle) - size, 2 * size, 2 * size);
                }
                for (int stone = 6; stone >= 0; stone--) {
                    double away = stone == 0 ? 0 : (0.55 + 0.18 * BodyArt.chance(seed, stone * 3)) * r;
                    double angle = stone * Math.PI / 3 + BodyArt.chance(seed, stone * 3 + 1);
                    double size = (stone == 0 ? 0.5 : 0.3 + 0.16 * BodyArt.chance(seed, stone * 3 + 2)) * r;
                    globe(g, x + away * Math.cos(angle), y + away * Math.sin(angle), size, palette.mapBase, LINE);
                }
                name(g, palette, x, y, 0.5 * r, palette.mapBase, LINE);
            }
            case COMET -> {
                arrow(g, x, y, r);
                globe(g, x, y, 0.7 * r, palette.mapBase, LINE);
                name(g, palette, x, y, 0.7 * r, palette.mapBase, LINE);
            }
            case ASTEROID -> {
                globe(g, x, y, r, palette.mapBase, LINE);
                name(g, palette, x, y, r, palette.mapBase, LINE);
            }
            case MOON, PLANET -> {
                boolean ringed = look == Body.Look.RINGED;
                double air = BodyArt.air(look) * r;
                if (ringed) lineRings(g, x, y, r, Math.PI);
                if (air > 0) {
                    g.setStroke(AIR_LINE);
                    g.setLineWidth(Math.max(0.6, 0.02 * r));
                    g.strokeOval(x - air, y - air, 2 * air, 2 * air);
                }
                globe(g, x, y, r, palette.mapBase, LINE);
                name(g, palette, x, y, r, palette.mapBase, LINE);
                if (ringed) lineRings(g, x, y, r, 0);
            }
            case STAR, REMNANT -> {
                if (look == Body.Look.BEAMING) {
                    linePulsar(g, palette, x, y, r);
                } else {
                    globe(g, x, y, r, palette.mapBase, GOLD);
                    name(g, palette, x, y, r, palette.mapBase, LINE);
                }
            }
            case BLACK_HOLE -> lineHole(g, palette, x, y, r, false);
            case CORE -> lineHole(g, palette, x, y, r, true);
            case EXOTIC -> {
                switch (look) {
                    case BEAMING -> linePulsar(g, palette, x, y, r);
                    case NEBULA -> {
                        dots(g, LINE, x, y, 1.3 * r, 1.3 * r, r);
                        globe(g, x, y, r, palette.mapBase, LINE);
                        name(g, palette, x, y, r, palette.mapBase, LINE);
                    }
                    default -> lineHole(g, palette, x, y, r, true);
                }
            }
        }
    }

    /** Une étoile à neutrons : ses faisceaux en pointillé, et un petit globe doré. */
    private static void linePulsar(GraphicsContext g, BodyArt.Palette palette, double x, double y, double r) {
        g.setFill(LINE);
        double size = Math.max(0.4, Math.min(0.8, 0.03 * r));
        for (int side = 0; side < 2; side++) {
            double angle = BodyArt.BEAM + side * Math.PI;
            for (int dot = 0; dot < 10; dot++) {
                double far = (0.65 + 0.19 * dot) * r;
                g.fillOval(x + far * Math.cos(angle) - size, y + far * Math.sin(angle) - size, 2 * size, 2 * size);
            }
        }
        globe(g, x, y, 0.5 * r, palette.mapBase, GOLD);
        name(g, palette, x, y, 0.5 * r, palette.mapBase, LINE);
    }

    /** Un trou noir : son disque en ellipses de traits, la moitié derrière puis devant, un globe sombre au graticule clair ; les jets du cœur en pointillé. */
    private static void lineHole(GraphicsContext g, BodyArt.Palette palette, double x, double y, double r, boolean jets) {
        if (jets) {
            g.setFill(LINE_PALE);
            double size = Math.max(0.4, Math.min(0.8, 0.03 * r));
            for (int side = -1; side <= 1; side += 2) {
                for (int dot = 0; dot < 8; dot++) {
                    double far = (1.1 + 0.16 * dot) * r;
                    g.fillOval(x - size, y + side * far - size, 2 * size, 2 * size);
                }
            }
        }
        lineDisc(g, x, y, r, Math.PI);
        globe(g, x, y, r, palette.mapDark, LINE_PALE);
        name(g, palette, x, y, r, palette.mapDark, LINE_PALE);
        lineDisc(g, x, y, r, 0);
    }

    /** La moitié du disque d'un trou noir : ses quatre anneaux en ellipses de traits pâles. */
    private static void lineDisc(GraphicsContext g, double x, double y, double r, double from) {
        g.setStroke(LINE_PALE);
        g.setLineWidth(Math.max(0.6, 0.02 * r));
        for (double disc : BodyArt.DISC) ellipse(g, x, y, disc * r, BodyArt.DISC_TILT * disc * r, from);
    }

    /** La moitié des anneaux d'une géante : trois ellipses de traits. */
    private static void lineRings(GraphicsContext g, double x, double y, double r, double from) {
        g.setStroke(LINE);
        g.setLineWidth(Math.max(0.6, 0.02 * r));
        for (double ring : BodyArt.RINGS) ellipse(g, x, y, ring * r, BodyArt.RING_TILT * ring * r, from);
    }

    /** La moitié d'une ellipse, à partir de l'angle {@code from}, en un trait. */
    private static void ellipse(GraphicsContext g, double x, double y, double rx, double ry, double from) {
        for (int step = 0; step <= BodyArt.ARC; step++) {
            double angle = from + Math.PI * step / BodyArt.ARC;
            PX[step] = x + rx * Math.cos(angle);
            PY[step] = y + ry * Math.sin(angle);
        }
        g.strokePolyline(PX, PY, BodyArt.ARC + 1);
    }

    /** La trajectoire d'une comète : une flèche en pointillé le long de ses queues, avec sa pointe au bout. */
    private static void arrow(GraphicsContext g, double x, double y, double r) {
        double cos = Math.cos(BodyArt.TAIL);
        double sin = Math.sin(BodyArt.TAIL);
        g.setFill(LINE);
        double size = Math.max(0.4, Math.min(0.8, 0.03 * r));
        for (int dot = 0; dot < 12; dot++) {
            double far = (1 + (TAIL_END - 1.3) * dot / 11.0) * r;
            g.fillOval(x + far * cos - size, y + far * sin - size, 2 * size, 2 * size);
        }
        g.setStroke(LINE);
        g.setLineWidth(Math.max(0.75, 0.03 * r));
        double tipX = x + TAIL_END * r * cos;
        double tipY = y + TAIL_END * r * sin;
        for (int side = -1; side <= 1; side += 2) {
            double angle = BodyArt.TAIL + Math.PI + side * 0.45;
            g.strokeLine(tipX, tipY, tipX + 0.35 * r * Math.cos(angle), tipY + 0.35 * r * Math.sin(angle));
        }
    }

    /** Un globe de rayon {@code r} : son fond, son graticule s'il fait cinq pixels, son contour. */
    private static void globe(GraphicsContext g, double x, double y, double r, Color base, Color line) {
        g.setFill(base);
        g.fillOval(x - r, y - r, 2 * r, 2 * r);
        g.setStroke(line);
        if (r >= 5) graticule(g, x, y, r);
        g.setLineWidth(Math.max(0.75, 0.03 * r));
        g.strokeOval(x - r, y - r, 2 * r, 2 * r);
    }

    /**
     * Le graticule d'un globe incliné de {@link #TILT} vers nous : les parallèles tous les 30°, en
     * arcs, l'équateur plus épais ; les méridiens tous les 30°, en ellipses vues de biais. Seule la
     * moitié tournée vers nous de chaque ligne est tracée, d'une pièce.
     */
    private static void graticule(GraphicsContext g, double x, double y, double r) {
        double thin = Math.max(0.6, 0.02 * r);
        double cos = Math.cos(TILT);
        double sin = Math.sin(TILT);
        for (int parallel = -2; parallel <= 2; parallel++) {
            double lat = parallel * Math.PI / 6;
            g.setLineWidth(parallel == 0 ? 1.8 * thin : thin);
            int count = 0;
            for (int step = 0; step <= 2 * BodyArt.ARC; step++) {
                double lon = -Math.PI + 2 * Math.PI * step / (2 * BodyArt.ARC);
                count = plot(x, y, r, lat, lon, cos, sin, count);
            }
            if (count >= 2) g.strokePolyline(PX, PY, count);
        }
        g.setLineWidth(thin);
        for (int meridian = 0; meridian < 12; meridian++) {
            double lon = meridian * Math.PI / 6;
            int count = 0;
            for (int step = 0; step <= BodyArt.ARC; step++) {
                double lat = -Math.PI / 2 + Math.PI * step / BodyArt.ARC;
                count = plot(x, y, r, lat, lon, cos, sin, count);
            }
            if (count >= 2) g.strokePolyline(PX, PY, count);
        }
    }

    /** Ajoute aux pointes le point (lat, lon) du globe s'il est tourné vers nous ; rend le nombre de pointes. */
    private static int plot(double x, double y, double r, double lat, double lon, double cos, double sin, int count) {
        double px = Math.cos(lat) * Math.sin(lon);
        double py = Math.sin(lat);
        double pz = Math.cos(lat) * Math.cos(lon);
        double toward = py * sin + pz * cos;
        if (toward <= 0) return count;
        PX[count] = x + px * r;
        PY[count] = y - (py * cos - pz * sin) * r;
        return count + 1;
    }

    /**
     * Le cartouche d'un globe : son nom en petites capitales, sous l'équateur, dans le globe, quand le
     * globe fait {@link #NAME_FROM} pixels et que le nom tient dans sa corde à cette hauteur.
     */
    private static void name(GraphicsContext g, BodyArt.Palette palette, double x, double y, double r, Color base, Color line) {
        if (r < NAME_FROM) return;
        int size = (int) Math.round(Math.min(11, Math.max(9, 0.32 * r)));
        double wide = 0.62 * size * palette.caps.length() + 6;
        double middle = 0.66 * r;
        double low = middle + 0.6 * size + 1;
        if (low >= r) return;
        double room = 2 * Math.sqrt(r * r - low * low) - 2;
        if (wide > room) return;
        double high = 1.2 * size + 2;
        g.setFill(base);
        g.fillRect(x - wide / 2, y + middle - high / 2, wide, high);
        g.setStroke(line);
        g.setLineWidth(0.8);
        g.strokeRect(x - wide / 2, y + middle - high / 2, wide, high);
        TextAlignment align = g.getTextAlign();
        VPos baseline = g.getTextBaseline();
        Font font = g.getFont();
        g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(VPos.CENTER);
        g.setFont(serif(size));
        g.setFill(line);
        g.fillText(palette.caps, x, y + middle);
        g.setTextAlign(align);
        g.setTextBaseline(baseline);
        g.setFont(font);
    }

    /** La police serif de cette taille, de 9 à 11 pixels, construite une fois. */
    private static Font serif(int size) {
        int slot = size - 9;
        if (SERIF[slot] == null) SERIF[slot] = Font.font("serif", size);
        return SERIF[slot];
    }
}
