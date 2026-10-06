package idle.ui;

import idle.core.Body;
import java.util.HashMap;
import java.util.Map;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

/**
 * Le dessin d'un astre ({@link Body}).
 *
 * <p>Son échelle décide de sa silhouette : un amas de roches est une poignée de cailloux dans leur
 * poussière, une comète un petit noyau dans sa chevelure avec ses deux queues qui s'effacent, un
 * astéroïde un bloc cabossé à facettes, une lune et une planète des globes, une étoile une boule
 * qui brille dans sa couronne, une étoile à neutrons un point entre ses deux faisceaux, un trou
 * noir un disque noir dans l'anneau de ce qui y tombe. Son aspect
 * ({@link Body.Look}) décide de sa surface : mers et cratères, plaques et fissures de glace,
 * veines et foyers qui luisent, brume, continents, calottes et nuages, bandes de gaz et tempête,
 * anneaux qui passent derrière puis devant.
 *
 * <p>La lumière vient d'en haut à gauche, pour tous : un globe est clair de ce côté et s'éteint de
 * l'autre, un bloc aussi, et un cratère a son bord éclairé à l'opposé. Les mondes qui ont un air
 * sont entourés d'un halo qui s'efface.
 *
 * <p>Tout est tiré d'une suite fixe qui ne dépend que de l'identifiant de l'astre : un astre a
 * toujours les mêmes cailloux et les mêmes cratères, sur sa carte, dans le ciel et dans l'espace.
 */
final class BodyArt {

    /** Direction des queues d'une comète, en radians : vers le haut à droite. */
    private static final double TAIL = -Math.PI / 6;
    private static final Color SHADE = Color.web("#05060a");
    private static final Color LIGHT = Color.web("#ffffff");
    private static final Color ION_TAIL = Color.web("#9fd0ff");
    private static final Color LAND = Color.web("#a8905e");
    private static final Color GRASS = Color.web("#5f8f55");

    /** La chevelure d'une comète, en rayons de son dessin. */
    private static final double COMA = 1.7;
    /** La queue de gaz d'une comète : sa longueur, sa demi-largeur au bout et sa direction. */
    private static final double ION_LENGTH = 5.2;
    private static final double ION_WIDTH = 0.45;
    private static final double ION_ANGLE = TAIL - 0.12;
    /** Sa queue de poussière, plus courte, plus large et un peu déviée. */
    private static final double DUST_LENGTH = 4.4;
    private static final double DUST_WIDTH = 1;
    private static final double DUST_ANGLE = TAIL + 0.14;
    /** Jusqu'où vont les queues d'une comète à droite et en haut, et le cercle qui la contient toute : son centre, son rayon. */
    private static final double COMET_RIGHT;
    private static final double COMET_UP;
    private static final double COMET_CENTER;
    private static final double COMET_SPAN;

    /** Les anneaux d'une géante : leur rayon et leur épaisseur, en rayons de la planète, et leur aplatissement. */
    private static final double[] RINGS = {1.3, 1.5, 1.67};
    private static final double[] RING_WIDTHS = {0.1, 0.14, 0.06};
    private static final double RING_TILT = 0.26;
    private static final double RING_REACH = 1.75;
    /** Un amas de roches et sa poussière. */
    private static final double RUBBLE_REACH = 1.2;
    /** Nombre de segments d'un demi-anneau. */
    private static final int ARC = 24;
    /** Jusqu'où va la couronne d'une étoile, en rayons. */
    private static final double CORONA = 1.45;
    /** Les faisceaux d'une étoile à neutrons : leur direction, leur longueur et leur demi-largeur au bout, en rayons. */
    private static final double BEAM = -Math.PI / 3;
    private static final double BEAM_LENGTH = 2.6;
    private static final double BEAM_WIDTH = 0.2;
    /** Le disque d'un trou noir : le rayon et l'épaisseur de ses anneaux, en rayons de l'horizon, et son aplatissement. */
    private static final double[] DISC = {1.35, 1.62, 1.9, 2.18};
    private static final double[] DISC_WIDTHS = {0.26, 0.28, 0.28, 0.24};
    private static final double DISC_TILT = 0.3;
    private static final double DISC_REACH = 2.35;
    /** L'arc que fait, au-dessus d'un trou noir, la lumière du disque passé derrière lui. */
    private static final double LENSED = 1.22;
    /** Les jets d'un trou noir supermassif : leur longueur et leur demi-largeur au bout, en rayons. */
    private static final double JET_LENGTH = 2.3;
    private static final double JET_WIDTH = 0.26;
    private static final Color BLACK = Color.web("#000000");
    private static final Color JET_COLOR = Color.web("#9fc8ff");

    static {
        double right = COMA;
        double up = COMA;
        double[][] tails = {{ION_LENGTH, ION_WIDTH, ION_ANGLE}, {DUST_LENGTH, DUST_WIDTH, DUST_ANGLE}};
        for (double[] tail : tails) {
            for (int side = -1; side <= 1; side += 2) {
                right = Math.max(right, tail[0] * Math.cos(tail[2]) - side * tail[1] * Math.sin(tail[2]));
                up = Math.max(up, -(tail[0] * Math.sin(tail[2]) + side * tail[1] * Math.cos(tail[2])));
            }
        }
        COMET_RIGHT = right;
        COMET_UP = up;
        // Le plus petit cercle centré sur l'axe des queues qui contient la chevelure et le bout des deux queues.
        double best = 0;
        double least = Double.POSITIVE_INFINITY;
        for (double along = 0; along <= 3; along += 0.01) {
            double needed = along + COMA;
            for (double[] tail : tails) {
                for (int side = -1; side <= 1; side += 2) {
                    double x = tail[0] * Math.cos(tail[2]) - side * tail[1] * Math.sin(tail[2]) - along * Math.cos(TAIL);
                    double y = tail[0] * Math.sin(tail[2]) + side * tail[1] * Math.cos(tail[2]) - along * Math.sin(TAIL);
                    needed = Math.max(needed, Math.hypot(x, y));
                }
            }
            if (needed < least) {
                least = needed;
                best = along;
            }
        }
        COMET_CENTER = best;
        COMET_SPAN = least;
    }

    /** Les arrêts des dégradés qui ne dépendent pas de l'astre : l'ombre d'un globe, son reflet, les queues de gaz. */
    private static final Stop[] SHADOW = {new Stop(0, fade(SHADE, 0)), new Stop(0.55, fade(SHADE, 0)),
            new Stop(0.82, fade(SHADE, 0.38)), new Stop(1, fade(SHADE, 0.72))};
    private static final Stop[] GLINT = {new Stop(0, fade(LIGHT, 0.32)), new Stop(1, fade(LIGHT, 0))};
    private static final Stop[] ION = {new Stop(0, fade(ION_TAIL, 0.5)), new Stop(1, fade(ION_TAIL, 0))};
    private static final Color MARE = fade(SHADE, 0.18);
    private static final Color PATCH = fade(SHADE, 0.28);
    private static final Color FACET = fade(SHADE, 0.22);
    private static final Color FROST = fade(LIGHT, 0.6);
    private static final Color CAP = fade(LIGHT, 0.85);
    private static final Color CLOUD = fade(LIGHT, 0.7);
    private static final Stop[] SHINE = {new Stop(0, fade(LIGHT, 0.6)), new Stop(1, fade(LIGHT, 0))};
    private static final Stop[] JET = {new Stop(0, fade(JET_COLOR.interpolate(LIGHT, 0.5), 0.85)), new Stop(1, fade(JET_COLOR, 0))};

    /**
     * Les couleurs d'un astre, calculées une fois : sa teinte et son accent, éclaircis, assombris ou
     * rendus translucides de toutes les façons dont le dessin a besoin.
     */
    private static final class Skin {
        final int seed;
        final Color tint;
        final Color accent;
        /** Trois roches : la teinte, la même assombrie, et un mélange avec l'accent. Pour chacune : claire, de base, sombre. */
        final Stop[][] rocks = new Stop[3][];
        final Stop[] land;
        final Stop[] grass;
        final Stop[] globe;
        final Stop[] air;
        final Stop[] dust;
        final Stop[] coma;
        final Stop[] dustTail;
        final Stop[] ember;
        final Color pale;
        final Color dark;
        final Color floor;
        final Color rim;
        final Color veil;
        final Color speck;
        final Color glow;
        final Color vein;
        final Color fissure;
        final Color storm;
        final Color stormEye;
        final Color[] bands;
        final Color[] rings;
        /** Pour une étoile : sa couronne, sa surface, ses taches et leur pénombre, ses plages claires, ses protubérances, ses faisceaux. */
        final Stop[] corona;
        final Stop[] photosphere;
        final Stop[] beam;
        final Color spot;
        final Color penumbra;
        final Color facula;
        final Color flare;
        /** Pour un trou noir : sa lueur, et les anneaux de son disque, du plus chaud au plus froid. */
        final Stop[] halo;
        final Color[] discs;

        Skin(Body body) {
            seed = body.id().hashCode();
            tint = Color.web(body.tint());
            accent = Color.web(body.accent());
            pale = tint.interpolate(LIGHT, 0.35);
            dark = tint.interpolate(SHADE, 0.25);
            rocks[0] = rock(tint);
            rocks[1] = rock(tint.interpolate(SHADE, 0.3));
            rocks[2] = rock(tint.interpolate(accent, 0.6));
            land = rock(LAND);
            grass = rock(GRASS);
            globe = new Stop[] {new Stop(0, tint.interpolate(LIGHT, 0.22)), new Stop(0.55, tint), new Stop(1, dark)};
            Color haze = body.look() == Body.Look.MOLTEN || body.look() == Body.Look.HAZY ? accent : tint.interpolate(LIGHT, 0.45);
            air = new Stop[] {new Stop(0, fade(haze, 0.6)), new Stop(0.86, fade(haze, 0.42)), new Stop(1, fade(haze, 0))};
            dust = new Stop[] {new Stop(0, fade(accent, 0.3)), new Stop(1, fade(accent, 0))};
            coma = new Stop[] {new Stop(0, fade(accent.interpolate(LIGHT, 0.5), 0.75)), new Stop(0.35, fade(accent, 0.4)),
                    new Stop(1, fade(accent, 0))};
            dustTail = new Stop[] {new Stop(0, fade(accent, 0.5)), new Stop(1, fade(accent, 0))};
            ember = new Stop[] {new Stop(0, fade(accent.interpolate(LIGHT, 0.6), 0.95)), new Stop(1, fade(accent, 0))};
            floor = tint.interpolate(SHADE, 0.36);
            rim = fade(tint.interpolate(LIGHT, 0.4), 0.75);
            veil = fade(accent, 0.3);
            speck = fade(accent, 0.55);
            glow = fade(accent, 0.35);
            vein = accent.interpolate(LIGHT, 0.45);
            fissure = fade(accent.interpolate(SHADE, 0.25), 0.7);
            storm = accent.interpolate(SHADE, 0.3);
            stormEye = accent.interpolate(LIGHT, 0.3);
            bands = new Color[] {accent, pale, accent.interpolate(SHADE, 0.2)};
            rings = new Color[] {accent.interpolate(tint, 0.45), accent, accent.interpolate(LIGHT, 0.4)};
            corona = new Stop[] {new Stop(0, fade(tint, 0.8)), new Stop(1 / CORONA, fade(tint, 0.5)), new Stop(1, fade(tint, 0))};
            photosphere = new Stop[] {new Stop(0, accent.interpolate(LIGHT, 0.55)), new Stop(0.6, accent.interpolate(tint, 0.5)),
                    new Stop(1, tint.interpolate(SHADE, 0.12))};
            beam = new Stop[] {new Stop(0, fade(accent.interpolate(LIGHT, 0.5), 0.9)), new Stop(1, fade(accent, 0))};
            spot = fade(tint.interpolate(SHADE, 0.75), 0.85);
            penumbra = fade(tint.interpolate(SHADE, 0.45), 0.55);
            facula = fade(accent.interpolate(LIGHT, 0.4), 0.3);
            flare = fade(accent, 0.85);
            halo = new Stop[] {new Stop(0, fade(tint, 0.6)), new Stop(0.5, fade(tint, 0.28)), new Stop(1, fade(tint, 0))};
            discs = new Color[] {accent.interpolate(LIGHT, 0.5), accent, tint, tint.interpolate(Color.web("#5a1a0a"), 0.55)};
        }

        private static Stop[] rock(Color base) {
            return new Stop[] {new Stop(0, base.interpolate(LIGHT, 0.32)), new Stop(0.5, base),
                    new Stop(1, base.interpolate(SHADE, 0.5))};
        }
    }

    private static final Map<String, Skin> SKINS = new HashMap<>();
    /** Les pointes de la forme en cours de dessin, gardées d'un dessin à l'autre pour ne rien allouer. */
    private static final double[] XS = new double[ARC + 1];
    private static final double[] YS = new double[ARC + 1];

    private BodyArt() {}

    private static Skin skin(Body body) {
        Skin skin = SKINS.get(body.id());
        if (skin == null) {
            skin = new Skin(body);
            SKINS.put(body.id(), skin);
        }
        return skin;
    }

    private static Color fade(Color color, double opacity) {
        return Color.color(color.getRed(), color.getGreen(), color.getBlue(), opacity);
    }

    /** De combien un astre dépasse à droite de son centre, en rayons : la queue d'une comète, les anneaux d'une géante, le disque d'un trou noir. */
    static double reachRight(Body body) {
        return switch (body.tier()) {
            case COMET -> COMET_RIGHT;
            case RUBBLE -> RUBBLE_REACH;
            case ASTEROID -> 1;
            case MOON, PLANET -> body.look() == Body.Look.RINGED ? RING_REACH : 1;
            case STAR, REMNANT -> body.look() == Body.Look.BEAMING ? BEAM_LENGTH * Math.cos(BEAM) + BEAM_WIDTH : CORONA;
            case BLACK_HOLE, CORE -> DISC_REACH;
        };
    }

    /** De combien un astre dépasse à gauche de son centre, en rayons. */
    static double reachLeft(Body body) {
        return body.tier() == Body.Tier.COMET ? COMA : reachRight(body);
    }

    /** De combien un astre dépasse au-dessus de son centre, en rayons. */
    static double reachUp(Body body) {
        return switch (body.tier()) {
            case COMET -> COMET_UP;
            case RUBBLE -> RUBBLE_REACH;
            case ASTEROID, MOON, PLANET -> 1;
            case STAR, REMNANT -> body.look() == Body.Look.BEAMING ? -BEAM_LENGTH * Math.sin(BEAM) + BEAM_WIDTH : CORONA;
            case BLACK_HOLE -> LENSED + 0.1;
            case CORE -> JET_LENGTH;
        };
    }

    /** De combien un astre dépasse en dessous de son centre, en rayons. */
    static double reachDown(Body body) {
        return body.tier() == Body.Tier.COMET ? COMA : reachUp(body);
    }

    /**
     * Le rayon du plus petit cercle qui contient tout le dessin d'un astre, halo compris, en rayons
     * de l'astre. Son centre n'est pas toujours celui de l'astre ({@link #centerX}, {@link #centerY}).
     */
    static double span(Body body) {
        return switch (body.tier()) {
            case COMET -> COMET_SPAN;
            case RUBBLE -> RUBBLE_REACH;
            case ASTEROID -> 1;
            case MOON, PLANET -> body.look() == Body.Look.RINGED ? RING_REACH : Math.max(1, air(body.look()));
            case STAR, REMNANT -> body.look() == Body.Look.BEAMING ? BEAM_LENGTH + BEAM_WIDTH : CORONA;
            case BLACK_HOLE -> DISC_REACH;
            case CORE -> Math.max(DISC_REACH, JET_LENGTH + 0.05);
        };
    }

    /** Où est le centre de ce cercle par rapport à celui de l'astre, en rayons : décalé le long des queues pour une comète. */
    static double centerX(Body body) {
        return body.tier() == Body.Tier.COMET ? COMET_CENTER * Math.cos(TAIL) : 0;
    }

    static double centerY(Body body) {
        return body.tier() == Body.Tier.COMET ? COMET_CENTER * Math.sin(TAIL) : 0;
    }

    /** Jusqu'où va le halo d'air d'un globe, en rayons : 0 pour un monde sans air. */
    private static double air(Body.Look look) {
        return switch (look) {
            case OCEAN -> 1.1;
            case HAZY -> 1.12;
            case BANDED, RINGED -> 1.08;
            case MOLTEN -> 1.06;
            case ROCKY, ICY, DARK, GLOWING, BEAMING, VOID -> 0;
        };
    }

    /**
     * Dessine un astre centré en (x, y).
     *
     * @param r son rayon : celui du globe d'une lune ou d'une planète, du bloc d'un astéroïde, du
     *          tas d'un amas, du noyau d'une comète avec un peu de sa chevelure
     */
    static void draw(GraphicsContext g, Body body, double x, double y, double r) {
        Skin skin = skin(body);
        g.setGlobalAlpha(1);
        switch (body.tier()) {
            case RUBBLE -> rubble(g, body, skin, x, y, r);
            case COMET -> comet(g, body, skin, x, y, r);
            case ASTEROID -> {
                lump(g, skin.seed, skin.rocks[0], x, y, r, 14, true);
                rough(g, body.look(), skin, x, y, 0.6 * r);
            }
            case MOON, PLANET -> globe(g, body, skin, x, y, r);
            case STAR, REMNANT -> {
                if (body.look() == Body.Look.BEAMING) pulsar(g, skin, x, y, r); else star(g, body, skin, x, y, r);
            }
            case BLACK_HOLE -> hole(g, skin, x, y, r, false);
            case CORE -> hole(g, skin, x, y, r, true);
        }
        g.setGlobalAlpha(1);
    }

    /**
     * Une étoile : sa couronne qui s'efface, quelques protubérances au bord, sa surface plus claire
     * au centre, des plages claires et des taches avec leur pénombre. Une naine brune, trop froide
     * pour en avoir, garde des bandes de gaz.
     *
     * @param r le rayon de sa surface ; la couronne va jusqu'à {@link #CORONA} fois plus loin
     */
    private static void star(GraphicsContext g, Body body, Skin skin, double x, double y, double r) {
        int seed = skin.seed;
        double corona = CORONA * r;
        g.setFill(new RadialGradient(0, 0, x, y, corona, false, CycleMethod.NO_CYCLE, skin.corona));
        g.fillOval(x - corona, y - corona, 2 * corona, 2 * corona);
        // Les protubérances : de petites boucles qui partent du bord et y reviennent.
        g.setStroke(skin.flare);
        g.setLineWidth(Math.max(0.75, 0.035 * r));
        g.setLineCap(StrokeLineCap.ROUND);
        g.setLineJoin(StrokeLineJoin.ROUND);
        for (int loop = 0; loop < 4; loop++) {
            double middle = 2 * Math.PI * chance(seed, 850 + loop);
            double wide = 0.1 + 0.08 * chance(seed, 860 + loop);
            double high = 0.12 + 0.14 * chance(seed, 870 + loop);
            for (int step = 0; step <= 8; step++) {
                double along = step / 8.0;
                double angle = middle + wide * (2 * along - 1);
                double away = (1 + high * 4 * along * (1 - along)) * r;
                XS[step] = x + away * Math.cos(angle);
                YS[step] = y + away * Math.sin(angle);
            }
            g.strokePolyline(XS, YS, 9);
        }
        g.setLineJoin(StrokeLineJoin.MITER);
        g.setLineCap(StrokeLineCap.SQUARE);
        g.setFill(new RadialGradient(0, 0, x, y, r, false, CycleMethod.NO_CYCLE, skin.photosphere));
        g.fillOval(x - r, y - r, 2 * r, 2 * r);
        if (body.look() == Body.Look.BANDED) {
            bands(g, skin.bands, 0.3, x, y, r, -0.7, 0.28, 6, 0.14, 0.1);
        } else {
            g.setFill(skin.facula);
            spots(g, seed, 880, 6, 0.1, 0.12, 0.6, x, y, r);
            for (int stain = 0; stain < 3; stain++) {
                double size = (0.04 + 0.05 * chance(seed, 900 + stain)) * r;
                double away = 0.62 * r * Math.sqrt(chance(seed, 910 + stain));
                double angle = 2 * Math.PI * chance(seed, 920 + stain);
                double cx = x + away * Math.cos(angle);
                double cy = y + away * Math.sin(angle);
                g.setFill(skin.penumbra);
                g.fillOval(cx - 1.8 * size, cy - 1.8 * size, 3.6 * size, 3.6 * size);
                g.setFill(skin.spot);
                g.fillOval(cx - size, cy - size, 2 * size, 2 * size);
            }
        }
        // Le cœur, plus vif que le bord.
        g.setFill(new RadialGradient(0, 0, x, y, 0.75 * r, false, CycleMethod.NO_CYCLE, SHINE));
        g.fillOval(x - r, y - r, 2 * r, 2 * r);
    }

    /**
     * Une étoile à neutrons : une bille très vive, minuscule dans sa lueur, les boucles de son champ
     * magnétique et ses deux faisceaux opposés.
     *
     * @param r le rayon de sa lueur ; la bille est bien plus petite, les faisceaux bien plus longs
     */
    private static void pulsar(GraphicsContext g, Skin skin, double x, double y, double r) {
        tail(g, skin.beam, x, y, BEAM_LENGTH * r, BEAM_WIDTH * r, BEAM);
        tail(g, skin.beam, x, y, BEAM_LENGTH * r, BEAM_WIDTH * r, BEAM + Math.PI);
        g.setFill(new RadialGradient(0, 0, x, y, r, false, CycleMethod.NO_CYCLE, skin.halo));
        g.fillOval(x - r, y - r, 2 * r, 2 * r);
        // Deux boucles du champ, de part et d'autre de l'axe des faisceaux.
        g.setStroke(skin.flare);
        g.setLineWidth(Math.max(0.75, 0.03 * r));
        g.setLineJoin(StrokeLineJoin.ROUND);
        double cos = Math.cos(BEAM);
        double sin = Math.sin(BEAM);
        for (int side = -1; side <= 1; side += 2) {
            for (int step = 0; step <= ARC; step++) {
                double angle = 2 * Math.PI * step / ARC;
                // Une ellipse qui passe par l'étoile : son grand axe le long des faisceaux, décalée d'un côté.
                double along = 0.62 * r * Math.sin(angle);
                double across = side * 0.34 * r * (1 - Math.cos(angle));
                XS[step] = x + along * cos - across * sin;
                YS[step] = y + along * sin + across * cos;
            }
            g.strokePolyline(XS, YS, ARC + 1);
        }
        g.setLineJoin(StrokeLineJoin.MITER);
        double ball = 0.36 * r;
        g.setFill(new RadialGradient(0, 0, x - 0.2 * ball, y - 0.2 * ball, 1.3 * ball, false, CycleMethod.NO_CYCLE, skin.photosphere));
        g.fillOval(x - ball, y - ball, 2 * ball, 2 * ball);
        g.setFill(new RadialGradient(0, 0, x, y, 0.8 * ball, false, CycleMethod.NO_CYCLE, SHINE));
        g.fillOval(x - ball, y - ball, 2 * ball, 2 * ball);
    }

    /**
     * Un trou noir : sa lueur, la moitié de son disque qui passe derrière, l'arc de lumière que ce
     * disque fait au-dessus de lui, l'horizon tout noir cerné d'un fil clair, puis la moitié du
     * disque qui passe devant. Le trou noir supermassif crache en plus deux jets, vers le haut et
     * vers le bas.
     *
     * @param r le rayon de l'horizon ; le disque va jusqu'à {@link #DISC_REACH} fois plus loin
     */
    private static void hole(GraphicsContext g, Skin skin, double x, double y, double r, boolean jets) {
        if (jets) {
            tail(g, JET, x, y, JET_LENGTH * r, JET_WIDTH * r, -Math.PI / 2);
            tail(g, JET, x, y, JET_LENGTH * r, JET_WIDTH * r, Math.PI / 2);
        }
        double glow = 2 * r;
        g.setFill(new RadialGradient(0, 0, x, y, glow, false, CycleMethod.NO_CYCLE, skin.halo));
        g.fillOval(x - glow, y - glow, 2 * glow, 2 * glow);
        disc(g, skin, x, y, r, Math.PI, 0.7);
        // La lumière du disque, courbée par-dessus le trou : un grand arc en haut, un plus fin en bas.
        g.setLineCap(StrokeLineCap.BUTT);
        g.setLineJoin(StrokeLineJoin.ROUND);
        for (int half = 0; half < 2; half++) {
            double radius = (half == 0 ? LENSED : 1.14) * r;
            for (int step = 0; step <= ARC; step++) {
                double angle = (half == 0 ? Math.PI : 0) + Math.PI * step / ARC;
                XS[step] = x + radius * Math.cos(angle);
                YS[step] = y + radius * Math.sin(angle);
            }
            g.setStroke(skin.discs[1]);
            g.setGlobalAlpha(half == 0 ? 0.9 : 0.45);
            g.setLineWidth(Math.max(1, (half == 0 ? 0.16 : 0.07) * r));
            g.strokePolyline(XS, YS, ARC + 1);
        }
        g.setGlobalAlpha(1);
        g.setLineJoin(StrokeLineJoin.MITER);
        g.setLineCap(StrokeLineCap.SQUARE);
        g.setFill(BLACK);
        g.fillOval(x - r, y - r, 2 * r, 2 * r);
        g.setStroke(skin.discs[0]);
        g.setLineWidth(Math.max(0.75, 0.045 * r));
        g.strokeOval(x - 1.03 * r, y - 1.03 * r, 2.06 * r, 2.06 * r);
        disc(g, skin, x, y, r, 0, 1);
    }

    /** La moitié du disque d'un trou noir qui commence à l'angle {@code from} : quatre anneaux aplatis, du plus chaud au plus froid. */
    private static void disc(GraphicsContext g, Skin skin, double x, double y, double r, double from, double alpha) {
        g.setGlobalAlpha(alpha);
        g.setLineCap(StrokeLineCap.BUTT);
        g.setLineJoin(StrokeLineJoin.ROUND);
        for (int ring = DISC.length - 1; ring >= 0; ring--) {
            for (int step = 0; step <= ARC; step++) {
                double angle = from + Math.PI * step / ARC;
                XS[step] = x + DISC[ring] * r * Math.cos(angle);
                YS[step] = y + DISC_TILT * DISC[ring] * r * Math.sin(angle);
            }
            g.setStroke(skin.discs[ring]);
            g.setLineWidth(Math.max(1, DISC_WIDTHS[ring] * r));
            g.strokePolyline(XS, YS, ARC + 1);
        }
        g.setLineJoin(StrokeLineJoin.MITER);
        g.setLineCap(StrokeLineCap.SQUARE);
        g.setGlobalAlpha(1);
    }

    /** Un amas de roches : sept cailloux serrés de trois teintes, quelques gravillons, et leur poussière. */
    private static void rubble(GraphicsContext g, Body body, Skin skin, double x, double y, double r) {
        int seed = skin.seed;
        double cloud = RUBBLE_REACH * r;
        g.setFill(new RadialGradient(0, 0, x, y, cloud, false, CycleMethod.NO_CYCLE, skin.dust));
        g.fillOval(x - cloud, y - cloud, 2 * cloud, 2 * cloud);
        for (int pebble = 0; pebble < 5; pebble++) {
            double away = (0.95 + 0.1 * chance(seed, 50 + pebble)) * r;
            double angle = 2 * Math.PI * chance(seed, 60 + pebble);
            double size = (0.07 + 0.05 * chance(seed, 70 + pebble)) * r;
            lump(g, seed + 17 * pebble, skin.rocks[pebble % 3], x + away * Math.cos(angle), y + away * Math.sin(angle), size, 5, false);
        }
        // Six cailloux autour, puis le plus gros au milieu, par-dessus.
        for (int stone = 6; stone >= 0; stone--) {
            double away = stone == 0 ? 0 : (0.55 + 0.18 * chance(seed, stone * 3)) * r;
            double angle = stone * Math.PI / 3 + chance(seed, stone * 3 + 1);
            double size = (stone == 0 ? 0.5 : 0.3 + 0.16 * chance(seed, stone * 3 + 2)) * r;
            lump(g, seed + 31 * stone, skin.rocks[stone % 3], x + away * Math.cos(angle), y + away * Math.sin(angle), size, 7, true);
        }
        rough(g, body.look(), skin, x, y, 0.3 * r);
    }

    /** Une comète : deux queues qui s'effacent, une chevelure, et son noyau. */
    private static void comet(GraphicsContext g, Body body, Skin skin, double x, double y, double r) {
        // La queue de gaz, droite et fine ; la queue de poussière, plus large et un peu déviée.
        tail(g, ION, x, y, ION_LENGTH * r, ION_WIDTH * r, ION_ANGLE);
        tail(g, skin.dustTail, x, y, DUST_LENGTH * r, DUST_WIDTH * r, DUST_ANGLE);
        double coma = COMA * r;
        g.setFill(new RadialGradient(0, 0, x, y, coma, false, CycleMethod.NO_CYCLE, skin.coma));
        g.fillOval(x - coma, y - coma, 2 * coma, 2 * coma);
        lump(g, skin.seed, skin.rocks[0], x, y, 0.6 * r, 9, true);
        rough(g, body.look(), skin, x, y, 0.36 * r);
    }

    /** Une queue de comète : un triangle qui part du noyau, s'élargit, et s'efface en s'éloignant. */
    private static void tail(GraphicsContext g, Stop[] stops, double x, double y, double length, double width, double angle) {
        double endX = x + length * Math.cos(angle);
        double endY = y + length * Math.sin(angle);
        double sideX = -Math.sin(angle) * width;
        double sideY = Math.cos(angle) * width;
        XS[0] = x - 0.25 * sideX;
        YS[0] = y - 0.25 * sideY;
        XS[1] = endX - sideX;
        YS[1] = endY - sideY;
        XS[2] = endX + sideX;
        YS[2] = endY + sideY;
        XS[3] = x + 0.25 * sideX;
        YS[3] = y + 0.25 * sideY;
        g.setFill(new LinearGradient(x, y, endX, endY, false, CycleMethod.NO_CYCLE, stops));
        g.fillPolygon(XS, YS, 4);
    }

    /**
     * Un bloc cabossé : un polygone dont chaque pointe est plus ou moins loin du centre, jamais plus
     * que {@code r}, éclairé d'en haut à gauche. Assez gros, il montre ses facettes.
     *
     * @param rock ses trois couleurs : claire, de base, sombre
     */
    private static void lump(GraphicsContext g, int seed, Stop[] rock, double x, double y, double r, int points, boolean facets) {
        for (int point = 0; point < points; point++) {
            double angle = 2 * Math.PI * point / points + 0.3 * chance(seed, 100 + point);
            double away = (0.72 + 0.28 * chance(seed, 200 + point)) * r;
            XS[point] = x + away * Math.cos(angle);
            YS[point] = y + away * Math.sin(angle);
        }
        g.setFill(new LinearGradient(x - 0.7 * r, y - 0.7 * r, x + 0.7 * r, y + 0.7 * r, false, CycleMethod.NO_CYCLE, rock));
        g.fillPolygon(XS, YS, points);
        if (!facets || r < 5) return;
        // Les arêtes : d'un sommet un peu à l'écart du centre vers une pointe sur trois.
        g.setStroke(FACET);
        g.setLineWidth(Math.max(0.6, 0.05 * r));
        g.setLineCap(StrokeLineCap.ROUND);
        for (int point = seed & 1; point < points; point += 3) {
            g.strokeLine(x - 0.15 * r, y - 0.12 * r, XS[point], YS[point]);
        }
        g.setLineCap(StrokeLineCap.SQUARE);
    }

    /** Une lune ou une planète : son air, son globe, sa surface, son ombre et son reflet, et parfois des anneaux. */
    private static void globe(GraphicsContext g, Body body, Skin skin, double x, double y, double r) {
        Body.Look look = body.look();
        boolean ringed = look == Body.Look.RINGED;
        // La moitié des anneaux qui passe derrière la planète.
        if (ringed) rings(g, skin, x, y, r, Math.PI, 0.5);
        double air = air(look) * r;
        if (air > 0) {
            g.setFill(new RadialGradient(0, 0, x, y, air, false, CycleMethod.NO_CYCLE, skin.air));
            g.fillOval(x - air, y - air, 2 * air, 2 * air);
        }
        g.setFill(new RadialGradient(0, 0, x - 0.3 * r, y - 0.32 * r, 1.35 * r, false, CycleMethod.NO_CYCLE, skin.globe));
        g.fillOval(x - r, y - r, 2 * r, 2 * r);
        surface(g, look, skin, x, y, r);
        // La lumière vient d'en haut à gauche : le globe s'éteint vers le bas à droite, et brille un peu de l'autre côté.
        g.setFill(new RadialGradient(0, 0, x - 0.28 * r, y - 0.3 * r, 1.42 * r, false, CycleMethod.NO_CYCLE, SHADOW));
        g.fillOval(x - r, y - r, 2 * r, 2 * r);
        g.setFill(new RadialGradient(0, 0, x - 0.38 * r, y - 0.4 * r, 0.6 * r, false, CycleMethod.NO_CYCLE, GLINT));
        g.fillOval(x - r, y - r, 2 * r, 2 * r);
        // La moitié des anneaux qui passe devant.
        if (ringed) rings(g, skin, x, y, r, 0, 0.9);
    }

    /** La moitié des anneaux d'une planète de rayon {@code r} qui commence à l'angle {@code from} : trois ellipses très aplaties. */
    private static void rings(GraphicsContext g, Skin skin, double x, double y, double r, double from, double alpha) {
        g.setGlobalAlpha(alpha);
        g.setLineCap(StrokeLineCap.BUTT);
        g.setLineJoin(StrokeLineJoin.ROUND);
        for (int ring = 0; ring < RINGS.length; ring++) {
            for (int step = 0; step <= ARC; step++) {
                double angle = from + Math.PI * step / ARC;
                XS[step] = x + RINGS[ring] * r * Math.cos(angle);
                YS[step] = y + RING_TILT * RINGS[ring] * r * Math.sin(angle);
            }
            g.setStroke(skin.rings[ring]);
            g.setLineWidth(Math.max(1, RING_WIDTHS[ring] * r));
            g.strokePolyline(XS, YS, ARC + 1);
        }
        g.setLineJoin(StrokeLineJoin.MITER);
        g.setLineCap(StrokeLineCap.SQUARE);
        g.setGlobalAlpha(1);
    }

    /** La surface d'un globe de rayon {@code r} centré en (x, y) : tout y reste à l'intérieur. */
    private static void surface(GraphicsContext g, Body.Look look, Skin skin, double x, double y, double r) {
        int seed = skin.seed;
        switch (look) {
            case ROCKY -> {
                // Des mers, de grandes taches sombres, puis des cratères.
                g.setFill(MARE);
                spots(g, seed, 900, 3, 0.22, 0.14, 0.5, x, y, r);
                craters(g, skin, 8, 0.05, 0.09, 0.8, x, y, r);
            }
            case ICY -> {
                g.setFill(skin.veil);
                spots(g, seed, 910, 4, 0.18, 0.14, 0.55, x, y, r);
                cracks(g, seed, skin.fissure, 0.016, x, y, r, 6, 0);
                cracks(g, seed, FROST, 0.014, x, y, r, 3, 40);
                // Une calotte, en haut.
                g.setFill(FROST);
                g.fillOval(x - 0.46 * r, y - 0.91 * r, 0.92 * r, 0.34 * r);
            }
            case MOLTEN -> {
                g.setFill(PATCH);
                spots(g, seed, 920, 3, 0.2, 0.12, 0.5, x, y, r);
                veins(g, skin, x, y, r, 6);
                embers(g, skin, 4, 0.1, 0.08, 0.7, x, y, r);
            }
            case DARK -> {
                g.setFill(PATCH);
                spots(g, seed, 930, 4, 0.2, 0.1, 0.5, x, y, r);
                g.setFill(skin.speck);
                spots(g, seed, 500, 10, 0.03, 0.05, 0.85, x, y, r);
            }
            case HAZY -> {
                // Des voiles de brume, et de larges bandes floues.
                g.setFill(skin.veil);
                g.fillOval(x - 0.9 * r, y - 0.75 * r, 1.5 * r, 1.5 * r);
                g.fillOval(x - 0.5 * r, y - 0.55 * r, 1.4 * r, 1.4 * r);
                bands(g, skin.bands, 0.3, x, y, r, -0.75, 0.3, 6, 0.15, 0.15);
            }
            case OCEAN -> {
                // Des terres et leurs plaines, deux calottes, puis les nuages qui passent par-dessus.
                for (int land = 0; land < 4; land++) {
                    double away = 0.5 * r * chance(seed, 600 + land);
                    double angle = 2 * Math.PI * chance(seed, 610 + land);
                    double size = (0.2 + 0.14 * chance(seed, 620 + land)) * r;
                    double cx = x + away * Math.cos(angle);
                    double cy = y + away * Math.sin(angle);
                    lump(g, seed + 7 * land, skin.land, cx, cy, size, 8, false);
                    lump(g, seed + 11 * land, skin.grass, cx + 0.15 * size, cy + 0.1 * size, 0.5 * size, 6, false);
                }
                g.setFill(CAP);
                g.fillOval(x - 0.36 * r, y - 0.92 * r, 0.72 * r, 0.24 * r);
                g.fillOval(x - 0.3 * r, y + 0.72 * r, 0.6 * r, 0.2 * r);
                clouds(g, seed, x, y, r, 7);
            }
            case BANDED -> {
                bands(g, skin.bands, 0.85, x, y, r, -0.8, 0.2, 9, 0.17, 0.11);
                // Une tempête : une tache ovale sur une bande, et son œil.
                g.setFill(skin.storm);
                g.fillOval(x - 0.04 * r, y + 0.1 * r, 0.48 * r, 0.26 * r);
                g.setFill(skin.stormEye);
                g.fillOval(x + 0.07 * r, y + 0.165 * r, 0.26 * r, 0.13 * r);
            }
            case RINGED -> bands(g, skin.bands, 0.4, x, y, r, -0.72, 0.24, 7, 0.12, 0.12);
            case GLOWING, BEAMING, VOID -> { }
        }
    }

    /**
     * Les détails d'un astre qui n'est pas rond, tous dans le disque de rayon {@code r} centré en
     * (x, y), plus petit que lui : ils restent sur lui.
     */
    private static void rough(GraphicsContext g, Body.Look look, Skin skin, double x, double y, double r) {
        switch (look) {
            case ROCKY -> craters(g, skin, 5, 0.1, 0.14, 0.7, x, y, r);
            case ICY -> {
                cracks(g, skin.seed, skin.fissure, 0.05, x, y, r, 4, 0);
                g.setFill(FROST);
                spots(g, skin.seed, 940, 4, 0.06, 0.08, 0.8, x, y, r);
            }
            case MOLTEN -> {
                veins(g, skin, x, y, r, 4);
                embers(g, skin, 2, 0.12, 0.1, 0.65, x, y, r);
            }
            case DARK -> {
                g.setFill(skin.speck);
                spots(g, skin.seed, 500, 8, 0.04, 0.05, 0.78, x, y, r);
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
    private static void spots(GraphicsContext g, int seed, int rank, int count, double least, double more, double spread,
            double x, double y, double r) {
        for (int spot = 0; spot < count; spot++) {
            double size = (least + more * chance(seed, rank + spot)) * r;
            double away = spread * r * Math.sqrt(chance(seed, rank + 10 + spot));
            double angle = 2 * Math.PI * chance(seed, rank + 20 + spot);
            g.fillOval(x + away * Math.cos(angle) - size, y + away * Math.sin(angle) - size, 2 * size, 2 * size);
        }
    }

    /** Des cratères : un bord éclairé, en bas à droite, sous un fond sombre. */
    private static void craters(GraphicsContext g, Skin skin, int count, double least, double more, double spread,
            double x, double y, double r) {
        for (int crater = 0; crater < count; crater++) {
            double size = (least + more * chance(skin.seed, 300 + crater)) * r;
            double away = spread * r * Math.sqrt(chance(skin.seed, 310 + crater));
            double angle = 2 * Math.PI * chance(skin.seed, 320 + crater);
            double cx = x + away * Math.cos(angle);
            double cy = y + away * Math.sin(angle);
            g.setFill(skin.rim);
            g.fillOval(cx - 0.82 * size, cy - 0.82 * size, 2 * size, 2 * size);
            g.setFill(skin.floor);
            g.fillOval(cx - size, cy - size, 1.76 * size, 1.76 * size);
        }
    }

    /** Des foyers : des ronds qui brillent au centre et s'effacent au bord. */
    private static void embers(GraphicsContext g, Skin skin, int count, double least, double more, double spread,
            double x, double y, double r) {
        for (int spot = 0; spot < count; spot++) {
            double size = (least + more * chance(skin.seed, 400 + spot)) * r;
            double away = spread * r * Math.sqrt(chance(skin.seed, 410 + spot));
            double angle = 2 * Math.PI * chance(skin.seed, 420 + spot);
            double cx = x + away * Math.cos(angle);
            double cy = y + away * Math.sin(angle);
            g.setFill(new RadialGradient(0, 0, cx, cy, size, false, CycleMethod.NO_CYCLE, skin.ember));
            g.fillOval(cx - size, cy - size, 2 * size, 2 * size);
        }
    }

    /** Des veines qui luisent : chaque fissure deux fois, large et sourde puis fine et vive. */
    private static void veins(GraphicsContext g, Skin skin, double x, double y, double r, int count) {
        cracks(g, skin.seed, skin.glow, 0.09, x, y, r, count, 0);
        cracks(g, skin.seed, skin.vein, 0.028, x, y, r, count, 0);
    }

    /** Des fissures : des traits coudés entre deux points pris dans le disque. */
    private static void cracks(GraphicsContext g, int seed, Color color, double width, double x, double y, double r, int count, int rank) {
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
    private static void bands(GraphicsContext g, Color[] colors, double alpha, double x, double y, double r, double top, double step,
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

    /** Des nuages : de courtes traînées claires, chacune à sa hauteur et pas plus longue que le disque n'est large à cet endroit. */
    private static void clouds(GraphicsContext g, int seed, double x, double y, double r, int count) {
        g.setStroke(CLOUD);
        g.setLineWidth(Math.max(1, 0.07 * r));
        g.setLineCap(StrokeLineCap.ROUND);
        for (int cloud = 0; cloud < count; cloud++) {
            double dy = (-0.62 + 1.24 * (cloud + chance(seed, 800 + cloud)) / count) * r;
            double half = Math.sqrt(Math.max(0, r * r - dy * dy)) - 0.08 * r;
            if (half <= 0) continue;
            double length = (0.3 + 0.4 * chance(seed, 810 + cloud)) * half;
            double middle = (2 * chance(seed, 820 + cloud) - 1) * (half - length);
            g.strokeLine(x + middle - length, y + dy, x + middle + length, y + dy);
        }
        g.setLineCap(StrokeLineCap.SQUARE);
    }

    /** Un nombre entre 0 et 1 qui ne dépend que de l'astre et du rang demandé. */
    private static double chance(int seed, int rank) {
        double value = Math.sin(seed * 12.9898 + rank * 78.233) * 43758.5453;
        return value - Math.floor(value);
    }
}
