package idle.ui.bigbang;

import static idle.ui.bigbang.BodyArtSurfaces.*;

import idle.core.sky.Body;
import idle.core.universe.Skin;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.*;
import javafx.scene.shape.*;

/**
 * Le dessin d'un astre ({@link Body}).
 *
 * <p>Son échelle décide de sa silhouette : un amas de roches est une poignée de cailloux dans leur poussière, une
 * comète un petit noyau dans sa chevelure avec ses deux queues qui s'effacent, un astéroïde un bloc cabossé à facettes,
 * une lune et une planète des globes, une étoile une boule qui brille dans sa couronne, une étoile à neutrons un point
 * entre ses deux faisceaux, un trou noir un disque noir dans l'anneau de ce qui y tombe. Son aspect ({@link Body.Look})
 * décide de sa surface : mers et cratères, plaques et fissures de glace, veines et foyers qui luisent, brume,
 * continents, calottes et nuages, bandes de gaz et tempête, anneaux qui passent derrière puis devant.
 *
 * <p>La lumière vient d'en haut à gauche, pour tous : un globe est clair de ce côté et s'éteint de l'autre, un bloc
 * aussi, et un cratère a son bord éclairé à l'opposé. Les mondes qui ont un air sont entourés d'un halo qui s'efface.
 *
 * <p>Assez grand à l'écran ({@link #DETAIL_FROM}), un astre montre des couches de plus (version 0.183), toujours dans
 * la même emprise que sans elles : un globe a son limbe qui s'assombrit, un liseré d'air plus vif côté soleil s'il en a
 * un, un reflet sur ses mers ou sa glace, l'ombre de ses nuages, les creux de ses cratères ombrés d'un côté et éclairés
 * de l'autre ; une géante a des remous qui ondulent entre ses bandes et une tempête qui tourne, et ses anneaux portent
 * l'ombre de la planète comme la planète celle des anneaux ; une étoile bouillonne, lance plus de boucles et quelques
 * rayons fins ; une comète a des jets au noyau et des stries dans sa poussière ; un astéroïde a ses pans éclairés ou
 * dans l'ombre ; le disque d'un trou noir est plus brillant du côté qui vient vers nous, et un fil de lumière cerne
 * l'horizon. En dessous de ce rayon, ces couches ne feraient que brouiller un dessin de quelques pixels : il reste tel
 * qu'il était.
 *
 * <p>Tout est tiré d'une suite fixe qui ne dépend que de l'identifiant de l'astre : un astre a toujours les mêmes
 * cailloux et les mêmes cratères, sur sa carte, dans le ciel et dans l'espace.
 *
 * <p>Trois skins ({@link Skin#BODIES}) remplacent cette apparence classique par une autre, pour tous les astres, dans
 * la même emprise : le Planétarium, des sphères de laiton et de verre dépoli sur leur tige ; la Gravure, du noir sur
 * blanc cassé modelé de hachures ; la Cartographie, des globes à méridiens et parallèles, avec leur nom en capitales.
 * Ils sont dessinés par {@link BodySkinArt} ; sans skin ({@code null}), le dessin classique ne change pas d'un pixel,
 * ses couches de détail comprises, que les skins n'ont pas : elles sont faites pour lui.
 */
final class BodyArt {
    /**
     * Rayon à l'écran, en pixels, à partir duquel les couches de détail sont dessinées. Pour une
     * comète, c'est le rayon de son dessin (le noyau et un peu de chevelure), pas celui du noyau.
     */
    static final double DETAIL_FROM = 14;

    /** Direction des queues d'une comète, en radians : vers le haut à droite. */
    static final double TAIL = -Math.PI / 6;
    static final Color SHADE = Color.web("#05060a");
    static final Color LIGHT = Color.web("#ffffff");
    static final Color ION_TAIL = Color.web("#9fd0ff");
    static final Color LAND = Color.web("#a8905e");
    static final Color GRASS = Color.web("#5f8f55");

    /** La chevelure d'une comète, en rayons de son dessin. */
    static final double COMA = 1.7;
    /** La queue de gaz d'une comète : sa longueur, sa demi-largeur au bout et sa direction. */
    static final double ION_LENGTH = 5.2;
    static final double ION_WIDTH = 0.45;
    static final double ION_ANGLE = TAIL - 0.12;
    /** Sa queue de poussière, plus courte, plus large et un peu déviée. */
    static final double DUST_LENGTH = 4.4;
    static final double DUST_WIDTH = 1;
    static final double DUST_ANGLE = TAIL + 0.14;
    /** Jusqu'où vont les queues d'une comète à droite et en haut, et le cercle qui la contient toute : son centre, son rayon. */
    static final double COMET_RIGHT;
    static final double COMET_UP;
    static final double COMET_CENTER;
    static final double COMET_SPAN;

    /** Les anneaux d'une géante : leur rayon et leur épaisseur, en rayons de la planète, et leur aplatissement. */
    static final double[] RINGS = {1.3, 1.5, 1.67};
    static final double[] RING_WIDTHS = {0.1, 0.14, 0.06};
    static final double RING_TILT = 0.26;
    static final double RING_REACH = 1.75;
    /** Un amas de roches et sa poussière. */
    static final double RUBBLE_REACH = 1.2;
    /** Nombre de segments d'un demi-anneau. */
    static final int ARC = 24;
    /** Jusqu'où va la couronne d'une étoile, en rayons. */
    static final double CORONA = 1.45;
    /** Les faisceaux d'une étoile à neutrons : leur direction, leur longueur et leur demi-largeur au bout, en rayons. */
    static final double BEAM = -Math.PI / 3;
    static final double BEAM_LENGTH = 2.6;
    static final double BEAM_WIDTH = 0.2;
    /** Le disque d'un trou noir : le rayon et l'épaisseur de ses anneaux, en rayons de l'horizon, et son aplatissement. */
    static final double[] DISC = {1.35, 1.62, 1.9, 2.18};
    static final double[] DISC_WIDTHS = {0.26, 0.28, 0.28, 0.24};
    static final double DISC_TILT = 0.3;
    static final double DISC_REACH = 2.35;
    /** L'arc que fait, au-dessus d'un trou noir, la lumière du disque passé derrière lui. */
    static final double LENSED = 1.22;
    /** Les jets d'un trou noir supermassif : leur longueur et leur demi-largeur au bout, en rayons. */
    static final double JET_LENGTH = 2.3;
    static final double JET_WIDTH = 0.26;
    static final Color BLACK = Color.web("#000000");
    static final Color JET_COLOR = Color.web("#9fc8ff");

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
    static final Stop[] SHADOW = {new Stop(0, fade(SHADE, 0)), new Stop(0.55, fade(SHADE, 0)),
            new Stop(0.82, fade(SHADE, 0.38)), new Stop(1, fade(SHADE, 0.72))};
    static final Stop[] GLINT = {new Stop(0, fade(LIGHT, 0.32)), new Stop(1, fade(LIGHT, 0))};
    static final Stop[] ION = {new Stop(0, fade(ION_TAIL, 0.5)), new Stop(1, fade(ION_TAIL, 0))};
    static final Color MARE = fade(SHADE, 0.18);
    static final Color PATCH = fade(SHADE, 0.28);
    static final Color FACET = fade(SHADE, 0.22);
    static final Color FROST = fade(LIGHT, 0.6);
    static final Color CAP = fade(LIGHT, 0.85);
    static final Color CLOUD = fade(LIGHT, 0.7);
    static final Stop[] SHINE = {new Stop(0, fade(LIGHT, 0.6)), new Stop(1, fade(LIGHT, 0))};
    static final Stop[] JET = {new Stop(0, fade(JET_COLOR.interpolate(LIGHT, 0.5), 0.85)), new Stop(1, fade(JET_COLOR, 0))};
    /** Les couches de détail qui ne dépendent pas de l'astre : le limbe d'un globe, le reflet sur ses mers, les ombres portées. */
    static final Stop[] LIMB = {new Stop(0, fade(SHADE, 0)), new Stop(0.78, fade(SHADE, 0)), new Stop(1, fade(SHADE, 0.55))};
    static final Stop[] SPECULAR = {new Stop(0, fade(LIGHT, 0.7)), new Stop(1, fade(LIGHT, 0))};
    static final Color RING_SHADOW = fade(SHADE, 0.5);
    static final Color CRATER_SHADE = fade(SHADE, 0.35);
    static final Color CLOUD_SHADOW = fade(SHADE, 0.3);
    static final Color FACET_LIT = fade(LIGHT, 0.12);
    /** L'effet Doppler sur le disque d'un trou noir : brillant à gauche, où il vient vers nous, sombre à droite, où il s'éloigne. */
    static final Stop[] DOPPLER = {new Stop(0, fade(LIGHT, 0.45)), new Stop(0.45, fade(LIGHT, 0)),
            new Stop(0.55, fade(SHADE, 0)), new Stop(1, fade(SHADE, 0.5))};
    static final Color PHOTON = fade(LIGHT, 0.85);
    /** L'anneau de photons d'un trou noir : son rayon, en rayons de l'horizon, entre le fil de l'horizon et l'arc de lumière courbée. */
    static final double PHOTON_RING = 1.1;

    /**
     * Les couleurs d'un astre, calculées une fois : sa teinte et son accent, éclaircis, assombris ou
     * rendus translucides de toutes les façons dont le dessin a besoin.
     */
    static final class Palette {
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
        /**
         * Les couches de détail : le liseré d'air et son arc éclairé, les remous entre les bandes
         * d'une géante, le tour de sa tempête, le bord éclairé d'un cratère, la granulation et les
         * rayons d'une étoile, les stries et les jets d'une comète.
         */
        final Color airRim;
        final Color airLit;
        final Color ripple;
        final Color stormSwirl;
        final Color rimLit;
        final Color granule;
        final Color ray;
        final Color streak;
        final Color jetWide;
        final Color jetThin;
        /**
         * Pour les skins : le nom en capitales (Cartographie), le verre dépoli teinté d'un gaz et le verre lumineux
         * d'une étoile (Planétarium), proportionnels à la sphère qu'ils remplissent pour n'être construits qu'une fois,
         * le fond de carte clair et celui, sombre, d'un trou noir (Cartographie).
         */
        final String caps;
        final RadialGradient glass;
        final RadialGradient lamp;
        final Color mapBase;
        final Color mapDark;

        Palette(Body body) {
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
            airRim = fade(haze, 0.45);
            airLit = fade(haze.interpolate(LIGHT, 0.5), 0.8);
            ripple = fade(accent.interpolate(SHADE, 0.35), 0.45);
            stormSwirl = fade(stormEye.interpolate(LIGHT, 0.3), 0.7);
            rimLit = fade(tint.interpolate(LIGHT, 0.7), 0.9);
            granule = fade(tint.interpolate(SHADE, 0.5), 0.14);
            ray = fade(tint.interpolate(LIGHT, 0.4), 0.35);
            streak = fade(accent.interpolate(LIGHT, 0.45), 0.3);
            jetWide = fade(accent.interpolate(LIGHT, 0.5), 0.25);
            jetThin = fade(LIGHT, 0.55);
            caps = body.name().toUpperCase(Locale.ROOT);
            glass = new RadialGradient(0, 0, 0.38, 0.36, 0.74, true, CycleMethod.NO_CYCLE,
                    new Stop(0, fade(tint.interpolate(LIGHT, 0.7), 0.88)), new Stop(0.6, fade(tint.interpolate(BodySkinArt.GLASS, 0.3), 0.62)),
                    new Stop(1, fade(tint.interpolate(SHADE, 0.3), 0.82)));
            lamp = new RadialGradient(0, 0, 0.38, 0.36, 0.74, true, CycleMethod.NO_CYCLE,
                    new Stop(0, fade(LIGHT, 0.98)), new Stop(0.35, tint.interpolate(LIGHT, 0.45)), new Stop(0.85, tint),
                    new Stop(1, tint.interpolate(SHADE, 0.2)));
            mapBase = tint.interpolate(LIGHT, 0.5);
            mapDark = tint.interpolate(SHADE, 0.72);
        }

        private static Stop[] rock(Color base) {
            return new Stop[] {new Stop(0, base.interpolate(LIGHT, 0.32)), new Stop(0.5, base),
                    new Stop(1, base.interpolate(SHADE, 0.5))};
        }
    }

    static final Map<String, Palette> PALETTES = new HashMap<>();
    /** Les pointes de la forme en cours de dessin, gardées d'un dessin à l'autre pour ne rien allouer. */
    static final double[] XS = new double[ARC + 1];
    static final double[] YS = new double[ARC + 1];
    /** Les trois pointes du pan d'un bloc en cours de dessin : ses pointes à lui sont dans {@link #XS}. */
    static final double[] PAN_X = new double[3];
    static final double[] PAN_Y = new double[3];

    private BodyArt() {}

    /** Vrai si un astre de ce rayon à l'écran est assez grand pour ses couches de détail ({@link #DETAIL_FROM}). */
    static boolean detailed(double r) { return r >= DETAIL_FROM; }

    static Palette palette(Body body) {
        Palette palette = PALETTES.get(body.id());
        if (palette == null) {
            palette = new Palette(body);
            PALETTES.put(body.id(), palette);
        }
        return palette;
    }

    static Color fade(Color color, double opacity) {
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
            case EXOTIC -> exoticReach(body, BEAM_LENGTH * Math.cos(BEAM) + BEAM_WIDTH, DISC_REACH);
        };
    }

    /** De combien un astre dépasse à gauche de son centre, en rayons. */
    static double reachLeft(Body body) { return body.tier() == Body.Tier.COMET ? COMA : reachRight(body); }

    /** De combien un astre dépasse au-dessus de son centre, en rayons. */
    static double reachUp(Body body) {
        return switch (body.tier()) {
            case COMET -> COMET_UP;
            case RUBBLE -> RUBBLE_REACH;
            case ASTEROID, MOON, PLANET -> 1;
            case STAR, REMNANT -> body.look() == Body.Look.BEAMING ? -BEAM_LENGTH * Math.sin(BEAM) + BEAM_WIDTH : CORONA;
            case BLACK_HOLE -> LENSED + 0.1;
            case CORE -> JET_LENGTH;
            case EXOTIC -> exoticReach(body, -BEAM_LENGTH * Math.sin(BEAM) + BEAM_WIDTH, JET_LENGTH);
        };
    }

    /** De combien un astre dépasse en dessous de son centre, en rayons. */
    static double reachDown(Body body) { return body.tier() == Body.Tier.COMET ? COMA : reachUp(body); }

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
            case EXOTIC -> exoticReach(body, BEAM_LENGTH + BEAM_WIDTH, Math.max(DISC_REACH, JET_LENGTH + 0.05));
        };
    }

    /** Où est le centre de ce cercle par rapport à celui de l'astre, en rayons : décalé le long des queues pour une comète. */
    static double centerX(Body body) { return body.tier() == Body.Tier.COMET ? COMET_CENTER * Math.cos(TAIL) : 0; }

    static double centerY(Body body) { return body.tier() == Body.Tier.COMET ? COMET_CENTER * Math.sin(TAIL) : 0; }

    /** Jusqu'où va le halo d'air d'un globe, en rayons : 0 pour un monde sans air. */
    static double air(Body.Look look) {
        return switch (look) {
            case OCEAN -> 1.1;
            case HAZY -> 1.12;
            case BANDED, RINGED -> 1.08;
            case MOLTEN -> 1.06;
            case ROCKY, ICY, DARK, GLOWING, BEAMING, VOID, NEBULA -> 0;
        };
    }

    /**
     * Dessine un astre centré en (x, y).
     *
     * @param r son rayon : celui du globe d'une lune ou d'une planète, du bloc d'un astéroïde, du
     *          tas d'un amas, du noyau d'une comète avec un peu de sa chevelure
     */
    static void draw(GraphicsContext g, Body body, double x, double y, double r) { draw(g, body, x, y, r, null); }

    /**
     * Dessine un astre centré en (x, y) dans l'apparence d'un skin des astres ({@link Skin#BODIES}),
     * ou l'apparence classique si {@code skin} est {@code null} ou n'habille pas les astres.
     */
    static void draw(GraphicsContext g, Body body, double x, double y, double r, Skin skin) {
        Palette palette = palette(body);
        g.setGlobalAlpha(1);
        if (skin == Skin.BODY_PLANETARIUM || skin == Skin.BODY_ENGRAVING || skin == Skin.BODY_CARTOGRAPHY) {
            BodySkinArt.draw(g, body, palette, x, y, r, skin);
            g.setGlobalAlpha(1);
            return;
        }
        switch (body.tier()) {
            case RUBBLE -> rubble(g, body, palette, x, y, r);
            case COMET -> comet(g, body, palette, x, y, r);
            case ASTEROID -> {
                lump(g, palette.seed, palette.rocks[0], x, y, r, 14, true);
                rough(g, body.look(), palette, x, y, 0.6 * r, detailed(r));
            }
            case MOON, PLANET -> globe(g, body, palette, x, y, r);
            case STAR, REMNANT -> {
                if (body.look() == Body.Look.BEAMING) pulsar(g, palette, x, y, r); else star(g, body, palette, x, y, r);
            }
            case BLACK_HOLE -> hole(g, palette, x, y, r, false);
            case CORE -> hole(g, palette, x, y, r, true);
            case EXOTIC -> {
                switch (body.look()) {
                    case BEAMING -> pulsar(g, palette, x, y, r);
                    case NEBULA -> nebula(g, palette, x, y, r);
                    default -> hole(g, palette, x, y, r, true);
                }
            }
        }
        g.setGlobalAlpha(1);
    }

    /**
     * Une étoile : sa couronne qui s'efface, quelques protubérances au bord, sa surface plus claire
     * au centre, des plages claires et des taches avec leur pénombre. Une naine brune, trop froide
     * pour en avoir, garde des bandes de gaz. En grand, l'étoile lance plus de boucles, sa surface
     * bouillonne en cellules à peine plus sombres, et quelques rayons fins percent sa couronne.
     *
     * @param r le rayon de sa surface ; la couronne va jusqu'à {@link #CORONA} fois plus loin
     */
    static void star(GraphicsContext g, Body body, Palette palette, double x, double y, double r) {
        int seed = palette.seed;
        boolean detail = detailed(r);
        double corona = CORONA * r;
        g.setFill(new RadialGradient(0, 0, x, y, corona, false, CycleMethod.NO_CYCLE, palette.corona));
        g.fillOval(x - corona, y - corona, 2 * corona, 2 * corona);
        if (detail && body.look() != Body.Look.BANDED) {
            // Des rayons fins, du bord de la surface jusque dans la couronne, jamais au-delà.
            g.setStroke(palette.ray);
            g.setLineWidth(Math.max(0.75, 0.02 * r));
            g.setLineCap(StrokeLineCap.ROUND);
            for (int ray = 0; ray < 7; ray++) {
                double angle = 2 * Math.PI * chance(seed, 1100 + ray);
                double far = (1.15 + 0.25 * chance(seed, 1110 + ray)) * r;
                g.strokeLine(x + 1.02 * r * Math.cos(angle), y + 1.02 * r * Math.sin(angle), x + far * Math.cos(angle), y + far * Math.sin(angle));
            }
        }
        // Les protubérances : de petites boucles qui partent du bord et y reviennent ; en grand, presque deux fois plus.
        g.setStroke(palette.flare);
        g.setLineWidth(Math.max(0.75, 0.035 * r));
        g.setLineCap(StrokeLineCap.ROUND);
        g.setLineJoin(StrokeLineJoin.ROUND);
        for (int loop = 0; loop < (detail ? 7 : 4); loop++) {
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
        g.setFill(new RadialGradient(0, 0, x, y, r, false, CycleMethod.NO_CYCLE, palette.photosphere));
        g.fillOval(x - r, y - r, 2 * r, 2 * r);
        if (body.look() == Body.Look.BANDED) {
            bands(g, palette.bands, 0.3, x, y, r, -0.7, 0.28, 6, 0.14, 0.1);
            if (detail) ripples(g, palette, x, y, r, -0.56, 0.28, 5);
        } else {
            if (detail) {
                // La granulation : toute la surface est semée de petites cellules, à peine plus sombres.
                g.setFill(palette.granule);
                spots(g, seed, 930, 16, 0.025, 0.035, 0.9, x, y, r);
            }
            g.setFill(palette.facula);
            spots(g, seed, 880, 6, 0.1, 0.12, 0.6, x, y, r);
            for (int stain = 0; stain < 3; stain++) {
                double size = (0.04 + 0.05 * chance(seed, 900 + stain)) * r;
                double away = 0.62 * r * Math.sqrt(chance(seed, 910 + stain));
                double angle = 2 * Math.PI * chance(seed, 920 + stain);
                double cx = x + away * Math.cos(angle);
                double cy = y + away * Math.sin(angle);
                g.setFill(palette.penumbra);
                g.fillOval(cx - 1.8 * size, cy - 1.8 * size, 3.6 * size, 3.6 * size);
                g.setFill(palette.spot);
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
    /** Jusqu'où va une matière exotique : comme un pulsar, une nébuleuse, ou un quasar avec ses jets. */
    static double exoticReach(Body body, double beaming, double hole) {
        return body.look() == Body.Look.BEAMING ? beaming : body.look() == Body.Look.NEBULA ? NEBULA_REACH : hole;
    }

    /** Jusqu'où va une nébuleuse, en rayons. */
    static final double NEBULA_REACH = 1.35;

    /**
     * Une nébuleuse : des nuages qui se recouvrent, de la teinte vers l'accent, et une petite
     * étoile au cœur qui les éclaire.
     */
    static void nebula(GraphicsContext g, Palette palette, double x, double y, double r) {
        Random random = new Random(palette.seed);
        for (int puff = 0; puff < 9; puff++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double away = random.nextDouble() * 0.55 * r;
            double size = (0.45 + random.nextDouble() * 0.35) * r;
            double px = x + Math.cos(angle) * away, py = y + Math.sin(angle) * away;
            Color color = palette.tint.interpolate(palette.accent, puff / 8.0);
            g.setFill(new RadialGradient(0, 0, px, py, size, false, CycleMethod.NO_CYCLE,
                    new Stop(0, fade(color, 0.55)), new Stop(1, fade(color, 0))));
            g.fillOval(px - size, py - size, 2 * size, 2 * size);
        }
        double core = 0.12 * r;
        g.setFill(new RadialGradient(0, 0, x, y, 2.5 * core, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.WHITE), new Stop(0.4, fade(Color.WHITE, 0.6)), new Stop(1, fade(Color.WHITE, 0))));
        g.fillOval(x - 2.5 * core, y - 2.5 * core, 5 * core, 5 * core);
    }

    static void pulsar(GraphicsContext g, Palette palette, double x, double y, double r) {
        tail(g, palette.beam, x, y, BEAM_LENGTH * r, BEAM_WIDTH * r, BEAM);
        tail(g, palette.beam, x, y, BEAM_LENGTH * r, BEAM_WIDTH * r, BEAM + Math.PI);
        g.setFill(new RadialGradient(0, 0, x, y, r, false, CycleMethod.NO_CYCLE, palette.halo));
        g.fillOval(x - r, y - r, 2 * r, 2 * r);
        // Deux boucles du champ, de part et d'autre de l'axe des faisceaux.
        g.setStroke(palette.flare);
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
        g.setFill(new RadialGradient(0, 0, x - 0.2 * ball, y - 0.2 * ball, 1.3 * ball, false, CycleMethod.NO_CYCLE, palette.photosphere));
        g.fillOval(x - ball, y - ball, 2 * ball, 2 * ball);
        g.setFill(new RadialGradient(0, 0, x, y, 0.8 * ball, false, CycleMethod.NO_CYCLE, SHINE));
        g.fillOval(x - ball, y - ball, 2 * ball, 2 * ball);
    }

    /**
     * Un trou noir : sa lueur, la moitié de son disque qui passe derrière, l'arc de lumière que ce
     * disque fait au-dessus de lui, l'horizon tout noir cerné d'un fil clair, puis la moitié du
     * disque qui passe devant. Le trou noir supermassif crache en plus deux jets, vers le haut et
     * vers le bas. En grand, le disque est plus brillant à gauche, où il vient vers nous, et plus
     * sombre à droite, et un fil de lumière cerne l'horizon : l'anneau de photons.
     *
     * @param r le rayon de l'horizon ; le disque va jusqu'à {@link #DISC_REACH} fois plus loin
     */
    static void hole(GraphicsContext g, Palette palette, double x, double y, double r, boolean jets) {
        boolean detail = detailed(r);
        if (jets) {
            tail(g, JET, x, y, JET_LENGTH * r, JET_WIDTH * r, -Math.PI / 2);
            tail(g, JET, x, y, JET_LENGTH * r, JET_WIDTH * r, Math.PI / 2);
        }
        double glow = 2 * r;
        g.setFill(new RadialGradient(0, 0, x, y, glow, false, CycleMethod.NO_CYCLE, palette.halo));
        g.fillOval(x - glow, y - glow, 2 * glow, 2 * glow);
        disc(g, palette, x, y, r, Math.PI, 0.7, detail);
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
            g.setStroke(palette.discs[1]);
            g.setGlobalAlpha(half == 0 ? 0.9 : 0.45);
            g.setLineWidth(Math.max(1, (half == 0 ? 0.16 : 0.07) * r));
            g.strokePolyline(XS, YS, ARC + 1);
        }
        g.setGlobalAlpha(1);
        g.setLineJoin(StrokeLineJoin.MITER);
        g.setLineCap(StrokeLineCap.SQUARE);
        g.setFill(BLACK);
        g.fillOval(x - r, y - r, 2 * r, 2 * r);
        g.setStroke(palette.discs[0]);
        g.setLineWidth(Math.max(0.75, 0.045 * r));
        g.strokeOval(x - 1.03 * r, y - 1.03 * r, 2.06 * r, 2.06 * r);
        if (detail) {
            // L'anneau de photons : un fil blanc, entre le fil de l'horizon et l'arc de lumière courbée.
            g.setStroke(PHOTON);
            g.setLineWidth(Math.max(0.6, 0.018 * r));
            g.strokeOval(x - PHOTON_RING * r, y - PHOTON_RING * r, 2 * PHOTON_RING * r, 2 * PHOTON_RING * r);
        }
        disc(g, palette, x, y, r, 0, 1, detail);
    }

    /**
     * La moitié du disque d'un trou noir qui commence à l'angle {@code from} : quatre anneaux aplatis, du plus chaud au plus froid.
     *
     * @param doppler vrai pour repasser les anneaux d'un dégradé qui les éclaire à gauche et les assombrit à droite
     */
    static void disc(GraphicsContext g, Palette palette, double x, double y, double r, double from, double alpha, boolean doppler) {
        g.setGlobalAlpha(alpha);
        g.setLineCap(StrokeLineCap.BUTT);
        g.setLineJoin(StrokeLineJoin.ROUND);
        // Deux passes quand l'effet Doppler est demandé : les anneaux, puis le dégradé par-dessus eux.
        for (int pass = 0; pass < (doppler ? 2 : 1); pass++) {
            if (pass == 1) {
                g.setStroke(new LinearGradient(x - DISC_REACH * r, y, x + DISC_REACH * r, y, false, CycleMethod.NO_CYCLE, DOPPLER));
            }
            for (int ring = DISC.length - 1; ring >= 0; ring--) {
                for (int step = 0; step <= ARC; step++) {
                    double angle = from + Math.PI * step / ARC;
                    XS[step] = x + DISC[ring] * r * Math.cos(angle);
                    YS[step] = y + DISC_TILT * DISC[ring] * r * Math.sin(angle);
                }
                if (pass == 0) g.setStroke(palette.discs[ring]);
                g.setLineWidth(Math.max(1, DISC_WIDTHS[ring] * r));
                g.strokePolyline(XS, YS, ARC + 1);
            }
        }
        g.setLineJoin(StrokeLineJoin.MITER);
        g.setLineCap(StrokeLineCap.SQUARE);
        g.setGlobalAlpha(1);
    }

    /** Un amas de roches : sept cailloux serrés de trois teintes, quelques gravillons, et leur poussière. */
    static void rubble(GraphicsContext g, Body body, Palette palette, double x, double y, double r) {
        int seed = palette.seed;
        double cloud = RUBBLE_REACH * r;
        g.setFill(new RadialGradient(0, 0, x, y, cloud, false, CycleMethod.NO_CYCLE, palette.dust));
        g.fillOval(x - cloud, y - cloud, 2 * cloud, 2 * cloud);
        for (int pebble = 0; pebble < 5; pebble++) {
            double away = (0.95 + 0.1 * chance(seed, 50 + pebble)) * r;
            double angle = 2 * Math.PI * chance(seed, 60 + pebble);
            double size = (0.07 + 0.05 * chance(seed, 70 + pebble)) * r;
            lump(g, seed + 17 * pebble, palette.rocks[pebble % 3], x + away * Math.cos(angle), y + away * Math.sin(angle), size, 5, false);
        }
        // Six cailloux autour, puis le plus gros au milieu, par-dessus.
        for (int stone = 6; stone >= 0; stone--) {
            double away = stone == 0 ? 0 : (0.55 + 0.18 * chance(seed, stone * 3)) * r;
            double angle = stone * Math.PI / 3 + chance(seed, stone * 3 + 1);
            double size = (stone == 0 ? 0.5 : 0.3 + 0.16 * chance(seed, stone * 3 + 2)) * r;
            lump(g, seed + 31 * stone, palette.rocks[stone % 3], x + away * Math.cos(angle), y + away * Math.sin(angle), size, 7, true);
        }
        rough(g, body.look(), palette, x, y, 0.3 * r, detailed(r));
    }

    /**
     * Une comète : deux queues qui s'effacent, une chevelure, et son noyau. En grand, trois stries
     * courent dans la queue de poussière, et des jets de gaz jaillissent du noyau du côté du soleil,
     * à l'opposé des queues, avant de se perdre dans la chevelure.
     */
    static void comet(GraphicsContext g, Body body, Palette palette, double x, double y, double r) {
        int seed = palette.seed;
        boolean detail = detailed(r);
        // La queue de gaz, droite et fine ; la queue de poussière, plus large et un peu déviée.
        tail(g, ION, x, y, ION_LENGTH * r, ION_WIDTH * r, ION_ANGLE);
        tail(g, palette.dustTail, x, y, DUST_LENGTH * r, DUST_WIDTH * r, DUST_ANGLE);
        if (detail) {
            g.setStroke(palette.streak);
            g.setLineWidth(Math.max(0.75, 0.05 * r));
            g.setLineCap(StrokeLineCap.ROUND);
            double cos = Math.cos(DUST_ANGLE);
            double sin = Math.sin(DUST_ANGLE);
            for (int streak = 0; streak < 3; streak++) {
                // Chaque strie suit l'axe de la queue, une de chaque côté et une au milieu, à moins de la moitié de sa
                // demi-largeur, qui va du quart au départ à tout au bout : elle reste dans la queue, qui s'évase.
                double side = (streak - 1) * 0.45 * DUST_WIDTH * r;
                double from = (0.9 + 0.3 * chance(seed, 980 + streak)) * r;
                double to = (0.6 + 0.25 * chance(seed, 990 + streak)) * DUST_LENGTH * r;
                double offFrom = side * (0.25 + 0.75 * from / (DUST_LENGTH * r));
                double offTo = side * (0.25 + 0.75 * to / (DUST_LENGTH * r));
                g.strokeLine(x + from * cos - offFrom * sin, y + from * sin + offFrom * cos, x + to * cos - offTo * sin, y + to * sin + offTo * cos);
            }
            g.setLineCap(StrokeLineCap.SQUARE);
        }
        double coma = COMA * r;
        g.setFill(new RadialGradient(0, 0, x, y, coma, false, CycleMethod.NO_CYCLE, palette.coma));
        g.fillOval(x - coma, y - coma, 2 * coma, 2 * coma);
        if (detail) {
            // Les jets : trois fontaines, larges et pâles puis fines et vives, qui restent dans la chevelure.
            g.setLineCap(StrokeLineCap.ROUND);
            for (int jet = 0; jet < 3; jet++) {
                double angle = TAIL + Math.PI + 0.9 * (chance(seed, 1000 + jet) - 0.5);
                double far = (0.9 + 0.5 * chance(seed, 1010 + jet)) * r;
                double fromX = x + 0.3 * r * Math.cos(angle);
                double fromY = y + 0.3 * r * Math.sin(angle);
                double toX = x + far * Math.cos(angle);
                double toY = y + far * Math.sin(angle);
                g.setStroke(palette.jetWide);
                g.setLineWidth(Math.max(1.5, 0.16 * r));
                g.strokeLine(fromX, fromY, toX, toY);
                g.setStroke(palette.jetThin);
                g.setLineWidth(Math.max(0.75, 0.05 * r));
                g.strokeLine(fromX, fromY, toX, toY);
            }
            g.setLineCap(StrokeLineCap.SQUARE);
        }
        lump(g, seed, palette.rocks[0], x, y, 0.6 * r, 9, true);
        rough(g, body.look(), palette, x, y, 0.36 * r, detail);
    }

    /** Une queue de comète : un triangle qui part du noyau, s'élargit, et s'efface en s'éloignant. */
    static void tail(GraphicsContext g, Stop[] stops, double x, double y, double length, double width, double angle) {
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
     * que {@code r}, éclairé d'en haut à gauche. Assez gros, il montre ses facettes ; plus gros
     * encore ({@link #DETAIL_FROM}), ses pans s'éclairent vers le soleil et s'assombrissent à l'opposé.
     *
     * @param rock ses trois couleurs : claire, de base, sombre
     */
    static void lump(GraphicsContext g, int seed, Stop[] rock, double x, double y, double r, int points, boolean facets) {
        for (int point = 0; point < points; point++) {
            double angle = 2 * Math.PI * point / points + 0.3 * chance(seed, 100 + point);
            double away = (0.72 + 0.28 * chance(seed, 200 + point)) * r;
            XS[point] = x + away * Math.cos(angle);
            YS[point] = y + away * Math.sin(angle);
        }
        g.setFill(new LinearGradient(x - 0.7 * r, y - 0.7 * r, x + 0.7 * r, y + 0.7 * r, false, CycleMethod.NO_CYCLE, rock));
        g.fillPolygon(XS, YS, points);
        if (!facets || r < 5) return;
        // Le sommet : un peu à l'écart du centre, vers la lumière.
        double topX = x - 0.15 * r;
        double topY = y - 0.12 * r;
        if (detailed(r)) {
            // Les pans : un sur deux, du sommet à deux pointes voisines, plus clair s'il regarde vers le haut à gauche,
            // plus sombre s'il regarde vers le bas à droite ; ceux de profil restent tels quels.
            PAN_X[0] = topX;
            PAN_Y[0] = topY;
            for (int point = 0; point < points; point += 2) {
                int next = (point + 1) % points;
                double midX = (XS[point] + XS[next]) / 2 - x;
                double midY = (YS[point] + YS[next]) / 2 - y;
                double facing = -(midX + midY) / Math.max(1e-9, Math.hypot(midX, midY) * Math.sqrt(2));
                if (Math.abs(facing) < 0.3) continue;
                PAN_X[1] = XS[point];
                PAN_Y[1] = YS[point];
                PAN_X[2] = XS[next];
                PAN_Y[2] = YS[next];
                g.setFill(facing > 0 ? FACET_LIT : FACET);
                g.fillPolygon(PAN_X, PAN_Y, 3);
            }
        }
        // Les arêtes : du sommet vers une pointe sur trois.
        g.setStroke(FACET);
        g.setLineWidth(Math.max(0.6, 0.05 * r));
        g.setLineCap(StrokeLineCap.ROUND);
        for (int point = seed & 1; point < points; point += 3) {
            g.strokeLine(topX, topY, XS[point], YS[point]);
        }
        g.setLineCap(StrokeLineCap.SQUARE);
    }

    /** Un nombre entre 0 et 1 qui ne dépend que de l'astre et du rang demandé. */
    static double chance(int seed, int rank) {
        double value = Math.sin(seed * 12.9898 + rank * 78.233) * 43758.5453;
        return value - Math.floor(value);
    }
}
