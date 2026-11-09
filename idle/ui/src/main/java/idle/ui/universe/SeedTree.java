package idle.ui.universe;

import idle.core.universe.Skin;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Les arbres de la graine (version 0.121), du bonsaï (palier 5) à l'arbre-monde (palier 11), et leurs effets.
 *
 * <p>Chaque palier remplit davantage la vue : l'arbre grandit bien plus vite que la vue ne s'éloigne. Le tronc
 * s'effile en polygones, porte son écorce, puis ses nœuds ; les rameaux se divisent de plus en plus finement ;
 * le feuillage est fait de feuilles une à une, en trois teintes. Les effets montent avec l'avancement : halo,
 * poussière, lucioles, feuilles qui tombent, sève qui monte dans le tronc, rayons, puis pour l'arbre-monde des
 * étoiles, des mondes en orbite et des racines de lumière. Tout est tiré d'un bruit fixe : rien ne clignote d'une
 * image à l'autre, sauf ce qui doit bouger.
 *
 * <p>Version 0.185 : les essences. Le skin choisi pour l'arbre ({@link Skin#TREES}) change l'écorce, les feuilles et
 * la silhouette à tous les paliers : une essence est un jeu de paramètres ({@link Essence}), et le Classique est celui
 * d'aujourd'hui, tous ses facteurs à 1, ses décalages à 0, de sorte que sans skin le dessin ne bouge pas d'un pixel.
 * Trois essences ont leur propre silhouette : le baobab (bouteille et couronne courte), le séquoia (colonne et
 * couches étagées), le palmier (un stipe et un bouquet de palmes, sans touffes).
 *
 * <p>Version 0.197 : la saison figée, la fantaisie et le sol ({@link TreeSkinArt}) se combinent avec l'essence. La
 * fantaisie remplace le feuillage et le bois (teintes, bois, forme des feuilles ; l'arbre inversé est dessiné la tête en
 * bas) ; le sol remplace le pied ; la saison fige ce que l'arbre montre du temps : ses teintes et ses fleurs sans
 * fantaisie, et dans tous les cas ce qui tombe, les lucioles et ce qui couvre le pied. Sans ces skins, rien ne change.
 */
final class SeedTree {

    private SeedTree() {
    }

    /**
     * Hauteur de l'arbre, en part de la hauteur de la vue, du palier 5 au palier 11. Version 0.143 : le jeune arbre un
     * peu plus grand (0,46 avant), il paraissait plus petit que l'arbuste d'avant.
     */
    private static final double[] HEIGHT = {0.36, 0.40, 0.50, 0.51, 0.54, 0.56, 0.58};
    /** Largeur du tronc au pied, en part de la hauteur de l'arbre. */
    private static final double[] TRUNK = {0.075, 0.035, 0.055, 0.08, 0.11, 0.13, 0.15};
    /** Profondeur des ramifications. */
    private static final int[] DEPTH = {4, 3, 5, 6, 6, 6, 6};
    /** Écart des rameaux : le bonsaï s'étale, le jeune arbre monte, le géant s'ouvre. */
    private static final double[] SPREAD = {0.75, 0.55, 0.46, 0.50, 0.52, 0.48, 0.50};
    /** Torsion : l'arbre ancien se tord, le jeune pousse droit. */
    private static final double[] TWIST = {0.35, 0.25, 0.12, 0.20, 0.45, 0.25, 0.30};
    /** Taille des touffes de feuilles, en part de la hauteur. */
    private static final double[] LEAF = {0.085, 0.07, 0.07, 0.075, 0.07, 0.075, 0.08};

    private static final Color[][] FOLIAGE = {
            {Color.web("#2f6b3a"), Color.web("#4f9a4f"), Color.web("#86cf72")},   // bonsaï
            {Color.web("#2f7a3c"), Color.web("#56b259"), Color.web("#9be07e")},   // arbuste
            {Color.web("#3c8f45"), Color.web("#6cc65f"), Color.web("#b5ef8c")},   // jeune arbre
            {Color.web("#245e33"), Color.web("#3f8f46"), Color.web("#7cc464")},   // arbre mûr
            {Color.web("#5a4a22"), Color.web("#8a7a32"), Color.web("#d0a84a")},   // arbre ancien, mordoré
            {Color.web("#1d4f34"), Color.web("#2f7f4c"), Color.web("#68c27a")},   // arbre géant
            {Color.web("#1f6f73"), Color.web("#46c7b8"), Color.web("#b8fff0")}};  // arbre-monde, lumineux
    private static final Color BARK_DARK = Color.web("#3e2c1c"), BARK = Color.web("#6e5236"), BARK_LIGHT = Color.web("#9c7a52");
    private static final Color SOIL = Color.web("#2a2016"), GRASS = Color.web("#4f8f45"), MOSS = Color.web("#6fae5a");
    private static final Color GLOW = Color.web("#fff6d2"), FIREFLY = Color.web("#f7f0a0"), SAP = Color.web("#9fffe0");
    private static final Color BLOSSOM = Color.web("#ffb7d0"), FRUIT = Color.web("#ffcf5a"), STAR = Color.web("#e8f4ff");
    /** Les pommes de pin et les noix de coco. */
    private static final Color CONE = Color.web("#5a3a22"), COCONUT = Color.web("#5c3d1e"), COCONUT_LIGHT = Color.web("#8a6a3e");

    /** Comment l'écorce est marquée : les lignes d'aujourd'hui, ou celles d'une essence. */
    enum Bark {
        /** Des lignes dans le sens du bois (le Classique). */
        LINES,
        /** Des traits courts en travers, çà et là : les lenticelles du bouleau et du cerisier. */
        LENTICELS,
        /** Rien : l'écorce lisse du baobab. */
        SMOOTH,
        /** Des stries serrées dans le sens du bois, sombres et claires : le séquoia. */
        FIBROUS,
        /** Des lignes et des traits en travers qui découpent des plaques : le pin. */
        PLATES,
        /** Des anneaux en travers, tout le long : le stipe du palmier. */
        RINGS
    }

    /** La forme d'une feuille. */
    enum LeafShape {
        /** L'amande d'aujourd'hui. */
        ALMOND,
        /** Un petit rond : bouleau, cerisier. */
        ROUND,
        /** Longue et étroite : le saule, les folioles du palmier. */
        NARROW,
        /** Une palme à cinq pointes : l'érable. */
        PALMATE,
        /** Des aiguilles, par quatre : le pin. */
        NEEDLES
    }

    /**
     * Une essence : un jeu de paramètres pour l'arbre. {@code foliage} à {@code null} garde les trois teintes du palier ;
     * les facteurs ({@code height}, {@code trunk}, {@code spread}, {@code twist}, {@code length}, {@code tail},
     * {@code leaf}, {@code density}) multiplient les constantes du palier ; {@code flat} à 0 garde l'aplatissement des
     * touffes du palier ; {@code droop} fait retomber les rameaux fins, de ce nombre de radians par niveau.
     */
    enum Essence {
        CLASSIC(null, BARK_DARK, BARK, BARK_LIGHT, BARK_DARK, Bark.LINES, LeafShape.ALMOND, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0),
        BIRCH(tones("#6f8f2a", "#a3c93a", "#d8ec7a"), Color.web("#cfc8b8"), Color.web("#e9e4d6"), Color.web("#f2eee4"), Color.web("#2a2420"),
                Bark.LENTICELS, LeafShape.ROUND, 1.05, 0.7, 0.8, 0.8, 1, 1, 0.8, 1.1, 0, 0),
        WILLOW(tones("#4b6e52", "#6f9f78", "#9fc9a0"), Color.web("#3f3a33"), Color.web("#6b6458"), Color.web("#8a8275"), Color.web("#3f3a33"),
                Bark.LINES, LeafShape.NARROW, 1, 1, 1, 1, 1, 1.5, 1, 1, 0, 0.5),
        MAPLE(tones("#b3341c", "#e0552a", "#ffa35c"), Color.web("#3a2f2a"), Color.web("#6b5a4e"), Color.web("#8d7b6c"), Color.web("#3a2f2a"),
                Bark.LINES, LeafShape.PALMATE, 1, 1, 1.1, 1, 1, 1, 1.1, 1, 0, 0),
        CHERRY(tones("#f3a7c0", "#fbc8da", "#fff0f5"), Color.web("#2e2423"), Color.web("#4a3b3a"), Color.web("#6a5756"), Color.web("#c9b8a8"),
                Bark.LENTICELS, LeafShape.ROUND, 1, 1, 1.15, 1, 1, 1, 0.85, 1.2, 0, 0),
        BAOBAB(tones("#4f5f48", "#7a8f6a","#a8b998"), Color.web("#6a5c50"), Color.web("#9a8a7a"), Color.web("#b3a494"), Color.web("#6a5c50"),
                Bark.SMOOTH, LeafShape.ALMOND, 1, 2.5, 1.3, 1, 0.55, 1, 0.6, 0.6, 0, 0),
        SEQUOIA(tones("#1f4a2c", "#2f6b3a", "#4f9a4f"), Color.web("#4a2518"), Color.web("#8a4a30"), Color.web("#a35a3c"), Color.web("#b06a4c"),
                Bark.FIBROUS, LeafShape.NARROW, 1.1, 1, 1, 0.3, 1, 1, 0.9, 1, 0.4, 0),
        PINE(tones("#2f6f5a", "#4e9a78", "#78bf96"), Color.web("#3d281c"), Color.web("#6e4b36"), Color.web("#8a6448"), Color.web("#3d281c"),
                Bark.PLATES, LeafShape.NEEDLES, 1, 1, 1.4, 1, 1, 1, 1, 1, 0.7, 0),
        PALM(tones("#3f7a3a", "#5fae4a", "#9fd978"), Color.web("#5c4832"), Color.web("#8c7250"), Color.web("#a88a62"), Color.web("#5c4832"),
                Bark.RINGS, LeafShape.NARROW, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0);

        final Color[] foliage;
        final Color barkDark, bark, barkLight, mark;
        final Bark barkStyle;
        final LeafShape leafShape;
        final double height, trunk, spread, twist, length, tail, leaf, density, flat, droop;

        Essence(Color[] foliage, Color barkDark, Color bark, Color barkLight, Color mark, Bark barkStyle, LeafShape leafShape,
                double height, double trunk, double spread, double twist, double length, double tail, double leaf, double density,
                double flat, double droop) {
            this.foliage = foliage;
            this.barkDark = barkDark;
            this.bark = bark;
            this.barkLight = barkLight;
            this.mark = mark;
            this.barkStyle = barkStyle;
            this.leafShape = leafShape;
            this.height = height;
            this.trunk = trunk;
            this.spread = spread;
            this.twist = twist;
            this.length = length;
            this.tail = tail;
            this.leaf = leaf;
            this.density = density;
            this.flat = flat;
            this.droop = droop;
        }

        private static Color[] tones(String shade, String mid, String light) {
            return new Color[] {Color.web(shade), Color.web(mid), Color.web(light)};
        }

        /** L'essence du skin choisi pour l'arbre : {@code null} ou un autre visuel, le Classique. */
        static Essence of(Skin skin) {
            if (skin == null) return CLASSIC;
            return switch (skin) {
                case TREE_BIRCH -> BIRCH;
                case TREE_WILLOW -> WILLOW;
                case TREE_MAPLE -> MAPLE;
                case TREE_CHERRY -> CHERRY;
                case TREE_BAOBAB -> BAOBAB;
                case TREE_SEQUOIA -> SEQUOIA;
                case TREE_PINE -> PINE;
                case TREE_PALM -> PALM;
                default -> CLASSIC;
            };
        }
    }

    /** Touffes de feuilles (ou palmes) du dernier arbre dessiné : pour les vérifications (le coût du dessin). */
    static int lastTips;

    /**
     * Les skins de la saison, de la fantaisie et du sol de l'arbre en cours de dessin (version 0.197), posés par
     * {@link #draw} et rendus au Classique à la fin : le dessin se fait sur le fil JavaFX, un arbre à la fois.
     */
    private static TreeSkinArt.Look look = TreeSkinArt.Look.CLASSIC;

    /** Un bout de rameau : sa position, sa direction, et s'il porte des feuilles. */
    private record Tip(double x, double y, double angle, int index) {
    }

    /** Un segment de tronc ou de rameau, pour la sève qui monte : du pied vers le bout. */
    private record Segment(double x0, double y0, double x1, double y1, double width, int depth) {
    }

    /** Ce que l'arbre laisse aux effets : le centre et la taille du feuillage, ses segments, ses bouts. */
    static final class Shape {
        double crownX, crownY, crownRadius;
        final List<Segment> segments = new ArrayList<>();
        final List<Tip> tips = new ArrayList<>();
    }

    /**
     * L'intensité des effets, de 0 au palier 4 à 1 à l'arbre-monde : chaque palier en montre un peu plus.
     */
    static double prominence(int stage) {
        return Math.max(0, Math.min(1, (stage - 4) / 7.0));
    }

    /**
     * Dessine l'arbre Classique du palier {@code stage} (5 à 11) dont le pied est en {@code baseX, baseY}, et ses effets.
     * {@code growth}, de 0 à 1, le fait grandir pendant le passage. Rend le centre du feuillage.
     */
    static double[] draw(GraphicsContext g, double w, double h, double baseX, double baseY, int stage, double growth, double time) {
        return draw(g, w, h, baseX, baseY, stage, growth, time, null);
    }

    /**
     * Dessine l'arbre de l'essence du skin {@code skin} ({@code null} : le Classique), du palier {@code stage} (5 à 11),
     * dont le pied est en {@code baseX, baseY}, et ses effets. {@code growth}, de 0 à 1, le fait grandir pendant le
     * passage. Rend le centre du feuillage et son rayon.
     */
    static double[] draw(GraphicsContext g, double w, double h, double baseX, double baseY, int stage, double growth, double time,
            Skin skin) {
        return draw(g, w, h, baseX, baseY, stage, growth, time, skin, null, null, null);
    }

    /**
     * Version 0.197 : l'arbre de l'essence {@code skin}, avec sa saison figée, sa fantaisie et son sol ({@code null} :
     * le Classique de chacun). L'arbre inversé est dessiné la tête en bas, autour du milieu de sa hauteur ; le centre
     * du feuillage rendu est celui que l'on voit.
     */
    static double[] draw(GraphicsContext g, double w, double h, double baseX, double baseY, int stage, double growth, double time,
            Skin skin, Skin seasonSkin, Skin fantasySkin, Skin groundSkin) {
        look = TreeSkinArt.Look.of(seasonSkin, fantasySkin, groundSkin);
        Essence e = Essence.of(skin);
        int i = Math.max(0, Math.min(6, stage - 5));
        double height = h * HEIGHT[i] * e.height * (0.35 + 0.65 * growth);
        boolean inverted = look.fantasy() == TreeSkinArt.Fantasy.INVERTED;
        double pivot = baseY - height * 0.5;
        if (inverted) {
            g.save();
            g.translate(0, 2 * pivot);
            g.scale(1, -1);
        }
        try {
            double[] crown = tree(g, w, h, baseX, baseY, stage, growth, time, e, i, height);
            if (inverted) crown[1] = 2 * pivot - crown[1];
            return crown;
        } finally {
            if (inverted) g.restore();
            look = TreeSkinArt.Look.CLASSIC;
        }
    }

    /** L'arbre lui-même, du halo aux effets, dans le repère où son pied est en {@code baseX, baseY}. */
    private static double[] tree(GraphicsContext g, double w, double h, double baseX, double baseY, int stage, double growth, double time,
            Essence e, int i, double height) {
        double p = prominence(stage);
        Color[] tones = look.tones(e.foliage != null ? e.foliage : FOLIAGE[i]);
        Shape shape = new Shape();
        shape.crownX = baseX;
        shape.crownY = baseY - height * 0.72;
        shape.crownRadius = height * (stage == 5 ? 0.45 : stage == 6 ? 0.42 : 0.5);

        // Derrière l'arbre : le halo, puis les rayons qui descendent du feuillage.
        UniverseView.glow(g, shape.crownX, shape.crownY, shape.crownRadius * (1.4 + 1.6 * p), tones[2], 0.10 + 0.30 * p);
        if (stage >= 9) rays(g, shape, h, stage, time);
        if (stage >= 11) cosmos(g, w, h, shape, time);

        if (look.ground() == TreeSkinArt.Ground.NONE) {
            ground(g, w, baseX, baseY, height, stage, time);
            TreeSkinArt.groundSeason(g, baseX, baseY, height, look.season(), height * (0.55 + 0.08 * (stage - 5)) * 0.95, tones);
        } else {
            double half = TreeSkinArt.ground(g, baseX, baseY, height, stage, time, look.ground());
            TreeSkinArt.groundSeason(g, baseX, baseY, height, look.season(), half, tones);
        }
        if (stage >= 8) roots(g, baseX, baseY, height, stage, time);

        // Le tronc et les rameaux.
        double trunk = height * TRUNK[i] * e.trunk * (0.7 + 0.3 * growth);
        double sway = Math.sin(time * 0.7) * 0.025;
        switch (e) {
            case PALM -> palm(g, shape, baseX, baseY, height, trunk, stage, i, time, tones, e);
            case BAOBAB -> baobab(g, shape, baseX, baseY, height, trunk, sway, stage, i, time, e);
            case SEQUOIA -> column(g, shape, baseX, baseY, height, trunk, sway, stage, i, time, e);
            default -> {
                if (stage == 6) {
                    // L'arbuste : plusieurs tiges qui partent du sol, en éventail.
                    for (int k = 0; k < 6; k++) {
                        double a = -0.75 + 0.3 * k + sway + (UniverseView.noise(k * 31 + 5) - 0.5) * 0.15;
                        branch(g, shape, baseX + (k - 2.5) * trunk * 0.6, baseY, height * (0.38 + 0.08 * UniverseView.noise(k * 7 + 1)) * e.length, a,
                                trunk, DEPTH[i], k * 13 + 3, i, time, 0, e);
                    }
                } else if (stage == 5) {
                    // Le bonsaï : un tronc en S, court et épais, puis des plateaux de feuillage.
                    double mx = baseX + height * 0.14, my = baseY - height * 0.30;
                    taper(g, e, baseX, baseY, mx, my, trunk, trunk * 0.8, e.bark, 6, 71);
                    shape.segments.add(new Segment(baseX, baseY, mx, my, trunk, 0));
                    double nx = baseX - height * 0.06, ny = baseY - height * 0.52;
                    taper(g, e, mx, my, nx, ny, trunk * 0.8, trunk * 0.62, e.bark, 6, 73);
                    shape.segments.add(new Segment(mx, my, nx, ny, trunk * 0.8, 0));
                    branch(g, shape, nx, ny, height * 0.30 * e.length, -0.15 + sway, trunk * 0.6, DEPTH[i], 5, i, time, 1, e);
                } else {
                    branch(g, shape, baseX, baseY, height * 0.34 * e.length, sway, trunk, DEPTH[i], stage * 7, i, time, 0, e);
                }
            }
        }

        if (e != Essence.PALM) foliage(g, shape, height, stage, i, time, tones, e);
        lastTips = shape.tips.size();

        // Devant : les feuilles qui tombent, les lucioles, la sève qui monte, les étincelles. La saison figée en décide :
        // l'automne fait tomber dès le bonsaï et deux fois plus, le printemps des pétales, l'hiver rien, et sans lucioles.
        TreeSkinArt.Season season = look.season();
        if (season == TreeSkinArt.Season.AUTUMN || season == TreeSkinArt.Season.SPRING || (season == TreeSkinArt.Season.NONE && stage >= 7)) {
            fallingLeaves(g, shape, baseY, height, stage, time, tones, e);
        }
        if (stage >= 6 && season != TreeSkinArt.Season.WINTER) fireflies(g, shape, stage, time);
        if (stage >= 8) sap(g, shape, stage, time);
        // Le centre du feuillage et son rayon : les bulles de l'arbre s'y posent (version 0.142).
        return new double[] {shape.crownX, shape.crownY, shape.crownRadius};
    }

    // ------------------------------------------------------------------
    // Le sol et les racines
    // ------------------------------------------------------------------

    private static void ground(GraphicsContext g, double w, double baseX, double baseY, double height, int stage, double time) {
        double width = height * (0.55 + 0.08 * (stage - 5));
        g.setFill(SOIL);
        g.setGlobalAlpha(0.95);
        g.fillOval(baseX - width, baseY - height * 0.035, width * 2, height * 0.09);
        // Les herbes, puis la mousse et les pierres pour les vieux arbres.
        int blades = 18 + 6 * (stage - 5);
        g.setStroke(GRASS);
        for (int k = 0; k < blades; k++) {
            double x = baseX - width * 0.95 + 2 * width * 0.95 * UniverseView.noise(k * 3 + 11);
            double len = height * (0.018 + 0.03 * UniverseView.noise(k * 3 + 12));
            double bend = Math.sin(time * 1.3 + k) * len * 0.25;
            g.setLineWidth(Math.max(1, height * 0.004));
            g.setGlobalAlpha(0.8);
            g.strokeLine(x, baseY, x + bend, baseY - len);
        }
        if (stage >= 8) {
            g.setFill(MOSS);
            for (int k = 0; k < 10 + 2 * (stage - 8); k++) {
                double x = baseX - width * 0.8 + 1.6 * width * UniverseView.noise(k * 5 + 41), r = height * (0.008 + 0.012 * UniverseView.noise(k * 5 + 42));
                g.setGlobalAlpha(0.7);
                g.fillOval(x - r, baseY - r * 0.6, r * 2, r * 1.2);
            }
            g.setFill(Color.web("#5a5f66"));
            for (int k = 0; k < 4; k++) {
                double x = baseX + (k < 2 ? -1 : 1) * width * (0.55 + 0.25 * UniverseView.noise(k + 60)), r = height * (0.015 + 0.01 * k % 2);
                g.setGlobalAlpha(0.85);
                g.fillOval(x - r * 1.3, baseY - r * 0.8, r * 2.6, r * 1.4);
            }
        }
        g.setGlobalAlpha(1);
    }

    private static void roots(GraphicsContext g, double baseX, double baseY, double height, int stage, double time) {
        int count = 4 + 2 * (stage - 8);
        for (int k = 0; k < count; k++) {
            double side = k % 2 == 0 ? -1 : 1, reach = height * (0.18 + 0.12 * UniverseView.noise(k * 9 + 2)) * (stage >= 11 ? 1.6 : 1);
            double[] xs = new double[6], ys = new double[6];
            for (int s = 0; s < 6; s++) {
                double t = s / 5.0;
                xs[s] = baseX + side * reach * t;
                ys[s] = baseY - height * 0.01 + height * 0.035 * Math.sin(t * Math.PI) * (0.5 + UniverseView.noise(k * 9 + s)) + height * 0.02 * t;
            }
            g.setStroke(stage >= 11 ? SAP : BARK_DARK);
            g.setLineWidth(Math.max(1.2, height * (0.014 - 0.0012 * (k / 2))));
            g.setGlobalAlpha(stage >= 11 ? 0.55 + 0.35 * Math.sin(time * 1.5 + k) : 0.9);
            g.strokePolyline(xs, ys, 6);
        }
        g.setGlobalAlpha(1);
    }

    // ------------------------------------------------------------------
    // Le tronc, les rameaux, le feuillage
    // ------------------------------------------------------------------

    /**
     * Un rameau effilé, puis ses rameaux, plus courts et plus fins. Les bouts vont dans {@code shape.tips}, les
     * segments dans {@code shape.segments}. Le saule fait retomber ses rameaux fins et allonge les derniers ; le pin ne
     * laisse aucun rameau descendre.
     */
    private static void branch(GraphicsContext g, Shape shape, double x, double y, double length, double angle, double width,
            int depth, int seed, int i, double time, int level, Essence e) {
        double wiggle = (UniverseView.noise(seed * 7 + 1) - 0.5) * TWIST[i] * e.twist;
        double a = angle + wiggle * 0.5 + Math.sin(time * 0.9 + level + seed % 7) * 0.012 * level;
        if (e.droop > 0 && level >= 2) {
            a += (a < 0 ? -1 : 1) * e.droop * Math.min(3, level - 1);
            a = Math.max(-2.7, Math.min(2.7, a));
        }
        if (e == Essence.PINE) a = Math.max(-1.35, Math.min(1.35, a));
        if (depth <= 1) length *= e.tail;
        double ex = x + Math.sin(a) * length, ey = y - Math.cos(a) * length;
        double end = width * 0.68;
        taper(g, e, x, y, ex, ey, width, end, level <= 1 ? e.bark : e.barkLight, level <= 1 ? 5 : 2, seed);
        shape.segments.add(new Segment(x, y, ex, ey, width, level));
        // Les nœuds de l'arbre ancien.
        if (i == 4 && level <= 2 && UniverseView.noise(seed * 3 + 9) > 0.6) {
            g.setFill(e.barkDark);
            g.setGlobalAlpha(0.8);
            double kx = x + (ex - x) * 0.5, ky = y + (ey - y) * 0.5, r = width * 0.35;
            g.fillOval(kx - r, ky - r * 0.7, r * 2, r * 1.4);
            g.setGlobalAlpha(1);
        }
        if (depth <= 0) {
            shape.tips.add(new Tip(ex, ey, a, shape.tips.size()));
            return;
        }
        // Les touffes poussent aussi le long des rameaux fins, pas seulement au bout.
        if (depth <= 2) shape.tips.add(new Tip(x + (ex - x) * 0.6, y + (ey - y) * 0.6, a, shape.tips.size()));
        int children = depth >= 4 && UniverseView.noise(seed * 11 + 3) > 0.45 ? 3 : 2;
        for (int c = 0; c < children; c++) {
            double spread = SPREAD[i] * e.spread * (children == 3 ? (c - 1) : (c == 0 ? -0.8 : 0.8));
            double jitter = (UniverseView.noise(seed * 3 + c) - 0.5) * 0.4;
            double shrink = 0.70 + 0.14 * UniverseView.noise(seed + c * 5);
            // L'arbre se redresse : les rameaux du haut reviennent vers le ciel.
            double lift = -a * 0.18;
            branch(g, shape, ex, ey, length * shrink, a + spread + jitter + lift, end, depth - 1, seed * 5 + c + 1, i, time, level + 1, e);
        }
    }

    /**
     * Un segment de bois qui s'effile, de {@code w0} à {@code w1}, avec son ombre et les marques d'écorce de l'essence ;
     * {@code barkLines} en donne la densité, {@code seed} place celles qui sont semées au hasard.
     */
    private static void taper(GraphicsContext g, Essence e, double x0, double y0, double x1, double y1, double w0, double w1, Color wood,
            int barkLines, int seed) {
        double dx = x1 - x0, dy = y1 - y0, len = Math.max(1e-6, Math.hypot(dx, dy));
        double nx = -dy / len, ny = dx / len;
        double[] xs = {x0 + nx * w0 / 2, x1 + nx * w1 / 2, x1 - nx * w1 / 2, x0 - nx * w0 / 2};
        double[] ys = {y0 + ny * w0 / 2, y1 + ny * w1 / 2, y1 - ny * w1 / 2, y0 - ny * w0 / 2};
        g.setGlobalAlpha(1);
        g.setFill(look.barkDark(e.barkDark));
        g.fillPolygon(xs, ys, 4);
        // La face éclairée : un peu plus étroite, décalée vers la gauche.
        double[] lx = {x0 + nx * w0 * 0.42, x1 + nx * w1 * 0.42, x1 - nx * w1 * 0.15, x0 - nx * w0 * 0.15};
        double[] ly = {y0 + ny * w0 * 0.42, y1 + ny * w1 * 0.42, y1 - ny * w1 * 0.15, y0 - ny * w0 * 0.15};
        g.setFill(look.wood(wood, e.bark));
        g.fillPolygon(lx, ly, 4);
        TreeSkinArt.wood(g, look.fantasy(), x0, y0, x1, y1, w0, w1, seed);
        if (w0 > 4 && e.barkStyle != Bark.SMOOTH) {
            g.setStroke(e.mark);
            g.setLineWidth(Math.max(0.8, w0 * 0.05));
            g.setGlobalAlpha(0.55);
            switch (e.barkStyle) {
                case LINES, PLATES -> {
                    for (int k = 0; k < barkLines; k++) {
                        double off = (k + 0.5) / barkLines - 0.5;
                        g.strokeLine(x0 + nx * w0 * off * 0.8, y0 + ny * w0 * off * 0.8, x1 + nx * w1 * off * 0.8, y1 + ny * w1 * off * 0.8);
                    }
                    // Les plaques : des traits en travers, décalés d'une ligne à l'autre.
                    if (e.barkStyle == Bark.PLATES) {
                        for (int k = 0; k < barkLines; k++) {
                            double t = (k + UniverseView.noise(seed * 13 + k)) / barkLines, side = (k % 2 == 0 ? -1 : 1) * 0.4;
                            double cx = x0 + dx * t, cy = y0 + dy * t, cw = (w0 + (w1 - w0) * t) * 0.4;
                            g.strokeLine(cx + nx * cw * side, cy + ny * cw * side, cx, cy);
                        }
                    }
                }
                case LENTICELS -> {
                    // Des traits courts en travers du bois, semés le long du segment.
                    g.setLineWidth(Math.max(1, w0 * 0.08));
                    g.setGlobalAlpha(0.8);
                    for (int k = 0; k < barkLines; k++) {
                        double t = (k + UniverseView.noise(seed * 17 + k)) / barkLines, off = (UniverseView.noise(seed * 17 + k + 50) - 0.5) * 0.6;
                        double cx = x0 + dx * t, cy = y0 + dy * t, cw = (w0 + (w1 - w0) * t) * (0.12 + 0.2 * UniverseView.noise(seed * 17 + k + 100));
                        g.strokeLine(cx + nx * (off * w0 - cw), cy + ny * (off * w0 - cw), cx + nx * (off * w0 + cw), cy + ny * (off * w0 + cw));
                    }
                }
                case FIBROUS -> {
                    // Des stries serrées, une sombre et une claire.
                    g.setLineWidth(Math.max(0.8, w0 * 0.03));
                    int lines = barkLines * 2;
                    for (int k = 0; k < lines; k++) {
                        double off = (k + 0.5) / lines - 0.5;
                        g.setStroke(k % 2 == 0 ? e.barkDark : e.mark);
                        g.setGlobalAlpha(k % 2 == 0 ? 0.6 : 0.35);
                        g.strokeLine(x0 + nx * w0 * off * 0.9, y0 + ny * w0 * off * 0.9, x1 + nx * w1 * off * 0.9, y1 + ny * w1 * off * 0.9);
                    }
                }
                case RINGS -> {
                    // Les anneaux du stipe, en travers, sur toute la largeur.
                    g.setLineWidth(Math.max(1, w0 * 0.07));
                    g.setGlobalAlpha(0.6);
                    for (int k = 0; k < barkLines; k++) {
                        double t = (k + 0.5) / barkLines, cw = (w0 + (w1 - w0) * t) * 0.5;
                        double cx = x0 + dx * t, cy = y0 + dy * t;
                        g.strokeLine(cx + nx * cw, cy + ny * cw, cx - nx * cw, cy - ny * cw);
                    }
                }
            }
            g.setGlobalAlpha(1);
        }
    }

    /**
     * Le baobab : une bouteille, un tronc énorme qui monte à mi-hauteur, puis des rameaux courts et épais qui partent
     * d'en haut seulement, en éventail.
     */
    private static void baobab(GraphicsContext g, Shape shape, double baseX, double baseY, double height, double trunk, double sway,
            int stage, int i, double time, Essence e) {
        double mx = baseX + sway * height * 0.15, my = baseY - height * 0.28;
        double tx = baseX + sway * height * 0.4, ty = baseY - height * 0.52;
        taper(g, e, baseX, baseY, mx, my, trunk, trunk * 0.85, e.bark, 6, 81);
        shape.segments.add(new Segment(baseX, baseY, mx, my, trunk, 0));
        taper(g, e, mx, my, tx, ty, trunk * 0.85, trunk * 0.45, e.bark, 6, 83);
        shape.segments.add(new Segment(mx, my, tx, ty, trunk * 0.85, 0));
        int arms = 5;
        for (int k = 0; k < arms; k++) {
            double a = -1.2 + 2.4 * k / (arms - 1) + sway + (UniverseView.noise(k * 29 + stage) - 0.5) * 0.3;
            double ax = tx + (k - (arms - 1) / 2.0) * trunk * 0.16;
            branch(g, shape, ax, ty, height * 0.34 * e.length, a, trunk * 0.3, Math.max(2, DEPTH[i] - 1), k * 17 + stage * 3, i, time, 1, e);
        }
    }

    /**
     * Le séquoia : une colonne droite et très haute, et des étages de rameaux courts, presque horizontaux, de plus en
     * plus courts vers la cime ; les touffes aplaties de l'essence en font des couches.
     */
    private static void column(GraphicsContext g, Shape shape, double baseX, double baseY, double height, double trunk, double sway,
            int stage, int i, double time, Essence e) {
        double topX = baseX + sway * height * 0.5, topY = baseY - height * 0.95;
        int parts = 3;
        double px = baseX, py = baseY, pw = trunk;
        for (int k = 0; k < parts; k++) {
            double t = (k + 1.0) / parts;
            double nx = baseX + (topX - baseX) * t * t, ny = baseY + (topY - baseY) * t, nw = trunk * (1 - 0.85 * t);
            taper(g, e, px, py, nx, ny, pw, nw, e.bark, 8, 91 + k);
            shape.segments.add(new Segment(px, py, nx, ny, pw, 0));
            px = nx;
            py = ny;
            pw = nw;
        }
        int tiers = 4 + i;
        int depth = i >= 2 ? 2 : 1;
        for (int t = 0; t < tiers; t++) {
            double f = (t + 1.0) / (tiers + 1);
            double y = baseY + (topY - baseY) * (0.22 + 0.72 * f), x = baseX + (topX - baseX) * Math.pow(0.22 + 0.72 * f, 2);
            double len = height * 0.24 * (1 - 0.7 * f) * e.length;
            for (int side = -1; side <= 1; side += 2) {
                double a = side * (1.3 - 0.3 * f) + (UniverseView.noise(t * 7 + side + 40) - 0.5) * 0.2;
                branch(g, shape, x, y, len, a, trunk * 0.2 * (1 - 0.5 * f), depth, t * 11 + side + 5, i, time, 2, e);
            }
        }
        shape.tips.add(new Tip(topX, topY, 0, shape.tips.size()));
    }

    /**
     * Le palmier : un stipe courbe, sans ramification, en segments annelés ; au sommet un bouquet de sept à neuf palmes
     * arquées, chacune une nervure et ses folioles de part et d'autre, et quelques noix de coco. Pas de touffes : le
     * centre et le rayon du feuillage sont ceux du bouquet.
     */
    private static void palm(GraphicsContext g, Shape shape, double baseX, double baseY, double height, double trunk, int stage, int i,
            double time, Color[] tones, Essence e) {
        double sway = Math.sin(time * 0.7) * 0.03;
        double lean = (UniverseView.noise(stage * 3 + 1) - 0.5) * 0.35;
        int parts = 9;
        double px = baseX, py = baseY, pw = trunk;
        for (int k = 0; k < parts; k++) {
            double t = (k + 1.0) / parts;
            double nx = baseX + height * 0.9 * (lean * t * t + sway * t), ny = baseY - height * 0.9 * t, nw = trunk * (1 - 0.45 * t);
            taper(g, e, px, py, nx, ny, pw, nw, e.bark, 3, k * 7 + stage);
            shape.segments.add(new Segment(px, py, nx, ny, pw, 0));
            px = nx;
            py = ny;
            pw = nw;
        }
        int fronds = 7 + Math.min(2, i / 2);
        double len = height * (0.36 + 0.02 * i);
        // Les palmes du fond d'abord, plus sombres, puis celles de devant.
        for (int pass = 0; pass < 2; pass++) {
            for (int f = 0; f < fronds; f++) {
                if ((f % 2 == 0) != (pass == 0)) continue;
                double a = -2.6 + 5.2 * (f + 0.5) / fronds + (UniverseView.noise(f * 5 + stage) - 0.5) * 0.3 + Math.sin(time * 0.8 + f) * 0.03;
                double l = len * (0.8 + 0.3 * UniverseView.noise(f * 5 + 2));
                frond(g, px, py, l, a, trunk, pass == 0 ? tones[0] : tones[1], pass == 0 ? tones[1] : tones[2]);
                shape.tips.add(new Tip(px + Math.sin(a) * l, py - Math.cos(a) * l + l * 0.55, a, shape.tips.size()));
            }
        }
        // Les noix de coco, sous le bouquet.
        for (int k = 0; k < 3; k++) {
            double r = trunk * 0.3, x = px + (k - 1) * r * 1.6, y = py + r * 1.1;
            g.setGlobalAlpha(1);
            g.setFill(COCONUT);
            g.fillOval(x - r, y - r, 2 * r, 2 * r);
            g.setFill(COCONUT_LIGHT);
            g.fillOval(x - r * 0.5, y - r * 0.6, r * 0.6, r * 0.5);
        }
        // L'arbre-monde : ses palmes brillent par le bout.
        if (stage >= 11) {
            for (Tip tip : shape.tips) {
                double pulse = 0.5 + 0.5 * Math.sin(time * 1.7 + tip.index());
                UniverseView.glow(g, tip.x(), tip.y(), len * (0.15 + 0.1 * pulse), tones[2], 0.18 + 0.2 * pulse);
            }
        }
        g.setGlobalAlpha(1);
        shape.crownX = px;
        shape.crownY = py - len * 0.2;
        shape.crownRadius = len * 0.85;
    }

    /**
     * Une palme : une nervure qui part de {@code x, y} dans la direction {@code angle} (depuis la verticale) et retombe
     * sous son poids, et ses folioles de part et d'autre, de plus en plus courtes vers le bout.
     */
    private static void frond(GraphicsContext g, double x, double y, double length, double angle, double trunk, Color rib, Color leaflet) {
        int n = 10;
        double sx = Math.sin(angle), cy = Math.cos(angle);
        double[] xs = new double[n + 1], ys = new double[n + 1];
        for (int k = 0; k <= n; k++) {
            double t = k / (double) n;
            xs[k] = x + sx * length * t;
            ys[k] = y - cy * length * t + length * 0.55 * t * t;
        }
        g.setGlobalAlpha(0.9);
        g.setStroke(leaflet);
        g.setLineWidth(Math.max(1, length * 0.012));
        for (int k = 1; k <= n; k++) {
            double t = k / (double) n;
            double tx = xs[k] - xs[k - 1], ty = ys[k] - ys[k - 1], tl = Math.max(1e-6, Math.hypot(tx, ty));
            double nx = -ty / tl, ny = tx / tl, size = length * 0.15 * (1 - 0.6 * t);
            // Les folioles pointent vers le bout, et pendent un peu.
            double fx = (tx / tl) * size * 0.5, fy = (ty / tl) * size * 0.5 + size * 0.3;
            g.strokeLine(xs[k], ys[k], xs[k] + nx * size + fx, ys[k] + ny * size + fy);
            g.strokeLine(xs[k], ys[k], xs[k] - nx * size + fx, ys[k] - ny * size + fy);
        }
        g.setStroke(rib);
        g.setLineWidth(Math.max(1.2, trunk * 0.1));
        g.strokePolyline(xs, ys, n + 1);
    }

    /**
     * Le feuillage : chaque bout porte une touffe de feuilles, une à une, ombre d'abord, lumière ensuite, dans les
     * teintes et la forme de l'essence ; le pin porte ses pommes à quelques bouts.
     */
    private static void foliage(GraphicsContext g, Shape shape, double height, int stage, int i, double time, Color[] tones, Essence e) {
        double radius = height * LEAF[i] * e.leaf;
        int perTip = (int) Math.round((7 + Math.min(3, stage - 5)) * e.density);
        double sumX = 0, sumY = 0;
        boolean bare = look.bare(), clouds = look.fantasy() == TreeSkinArt.Fantasy.CLOUDS;
        for (int pass = 0; pass < 3; pass++) {
            for (Tip tip : shape.tips) {
                double r = radius * (0.65 + 0.5 * UniverseView.noise(tip.index() * 5 + stage)) * (stage == 5 ? 1.25 : 1);
                double flat = e.flat > 0 ? e.flat : stage == 5 ? 0.5 : 0.85;
                if (pass == 0) {
                    sumX += tip.x();
                    sumY += tip.y();
                    if (bare) continue;
                    // La masse d'ombre, d'un seul tenant, derrière les feuilles.
                    g.setFill(tones[0]);
                    g.setGlobalAlpha(0.9);
                    g.fillOval(tip.x() - r, tip.y() - r * flat, 2 * r, 2 * r * flat);
                    continue;
                }
                if (bare) {
                    // L'hiver figé : pas de feuilles, du givre au bout des rameaux.
                    if (pass == 1) TreeSkinArt.frost(g, tip.x(), tip.y(), r, tip.angle(), time, tip.index());
                    continue;
                }
                if (clouds) {
                    // Les nuages : des bouffées à la place des feuilles une à une.
                    if (pass == 1) TreeSkinArt.cloud(g, tip.x(), tip.y(), r, flat, tones[1], tones[2], tip.index());
                    continue;
                }
                Color tone = pass == 1 ? tones[1] : tones[2];
                int leaves = pass == 1 ? perTip : perTip / 2;
                for (int k = 0; k < leaves; k++) {
                    int n = tip.index() * 53 + k * 7 + pass * 3;
                    double ang = 2 * Math.PI * UniverseView.noise(n + 1), dist = r * (pass == 1 ? 0.85 : 0.55) * Math.sqrt(UniverseView.noise(n + 2));
                    double lx = tip.x() + Math.cos(ang) * dist, ly = tip.y() + Math.sin(ang) * dist * flat - (pass == 2 ? r * 0.15 : 0);
                    double flutter = Math.sin(time * 2.1 + n) * 0.25;
                    leaf(g, e, lx, ly, r * (0.28 + 0.12 * UniverseView.noise(n + 3)), ang + flutter, tone, pass == 1 ? 0.95 : 0.9);
                }
            }
        }
        // Des fleurs pour l'arbuste et le jeune arbre, des fruits pour l'arbre mûr, de l'or pour l'ancien ; le printemps
        // figé fleurit chaque bout, à tous les paliers ; l'hiver figé ne porte rien.
        Color dot = bare ? null : stage == 6 || stage == 7 ? BLOSSOM : stage == 8 ? FRUIT : stage == 9 ? Color.web("#f0b45a") : null;
        if (look.blossoms()) {
            for (Tip tip : shape.tips) {
                double ox = (UniverseView.noise(tip.index() * 17 + 5) - 0.5) * radius;
                TreeSkinArt.blossom(g, tip.x() + ox, tip.y() + radius * 0.2, radius * 0.16);
            }
            dot = null;
        }
        if (dot != null) {
            g.setFill(dot);
            for (Tip tip : shape.tips) {
                if (UniverseView.noise(tip.index() * 17 + 4) < 0.45) continue;
                double r = radius * 0.13, ox = (UniverseView.noise(tip.index() * 17 + 5) - 0.5) * radius;
                g.setGlobalAlpha(0.95);
                g.fillOval(tip.x() + ox - r, tip.y() - r + radius * 0.2, 2 * r, 2 * r);
            }
        }
        // Les pommes de pin, brunes, pendues à quelques bouts.
        if (e == Essence.PINE) {
            for (Tip tip : shape.tips) {
                if (UniverseView.noise(tip.index() * 23 + 6) < 0.72) continue;
                double r = radius * 0.16, x = tip.x() + (UniverseView.noise(tip.index() * 23 + 7) - 0.5) * radius * 0.6, y = tip.y() + radius * 0.5;
                g.setGlobalAlpha(0.95);
                g.setFill(CONE);
                g.fillOval(x - r * 0.6, y - r, r * 1.2, r * 2);
                g.setStroke(COCONUT_LIGHT);
                g.setLineWidth(Math.max(0.8, r * 0.15));
                g.setGlobalAlpha(0.7);
                for (int k = -1; k <= 1; k++) g.strokeLine(x - r * 0.45, y + k * r * 0.5, x + r * 0.45, y + k * r * 0.5 + r * 0.2);
            }
        }
        // L'arbre-monde : son feuillage brille par endroits, comme des villes vues de loin.
        if (stage >= 11) {
            for (Tip tip : shape.tips) {
                if (tip.index() % 3 != 0) continue;
                double pulse = 0.5 + 0.5 * Math.sin(time * 1.7 + tip.index());
                UniverseView.glow(g, tip.x(), tip.y(), radius * (0.9 + 0.5 * pulse), tones[2], 0.18 + 0.2 * pulse);
            }
        }
        g.setGlobalAlpha(1);
        if (!shape.tips.isEmpty()) {
            shape.crownX = sumX / shape.tips.size();
            shape.crownY = sumY / shape.tips.size();
        }
    }

    /**
     * Une feuille de l'essence, tournée de {@code angle} : l'amande du Classique, un rond, une feuille étroite, une
     * palme à cinq pointes, ou quatre aiguilles en éventail.
     */
    private static void leaf(GraphicsContext g, Essence e, double x, double y, double size, double angle, Color tone, double alpha) {
        double c = Math.cos(angle), s = Math.sin(angle);
        TreeSkinArt.Fantasy fantasy = look.fantasy();
        if (fantasy == TreeSkinArt.Fantasy.CRYSTAL || fantasy == TreeSkinArt.Fantasy.CORAL || fantasy == TreeSkinArt.Fantasy.CIRCUIT
                || fantasy == TreeSkinArt.Fantasy.PIXEL) {
            TreeSkinArt.leaf(g, fantasy, x, y, size, angle, tone, alpha);
            return;
        }
        g.setFill(tone);
        g.setGlobalAlpha(alpha);
        switch (e.leafShape) {
            case ROUND -> g.fillOval(x - size * 0.8, y - size * 0.7, size * 1.6, size * 1.4);
            case NARROW -> {
                double l = size * 1.6, hw = 0.18;
                double[] xs = {x - c * l, x - s * l * hw, x + c * l, x + s * l * hw};
                double[] ys = {y - s * l, y + c * l * hw, y + s * l, y - c * l * hw};
                g.fillPolygon(xs, ys, 4);
            }
            case PALMATE -> {
                double[] xs = new double[10], ys = new double[10];
                for (int k = 0; k < 10; k++) {
                    double a = angle + k * Math.PI / 5, r = k % 2 == 0 ? size * 1.1 : size * 0.5;
                    xs[k] = x + Math.cos(a) * r;
                    ys[k] = y + Math.sin(a) * r;
                }
                g.fillPolygon(xs, ys, 10);
            }
            case NEEDLES -> {
                g.setStroke(tone);
                g.setLineWidth(Math.max(0.8, size * 0.12));
                for (int k = 0; k < 4; k++) {
                    double a = angle - 0.45 + 0.3 * k, l = size * 1.4;
                    g.strokeLine(x, y, x + Math.cos(a) * l, y + Math.sin(a) * l);
                }
            }
            default -> {
                double[] xs = {x - c * size, x - s * size * 0.45, x + c * size, x + s * size * 0.45};
                double[] ys = {y - s * size, y + c * size * 0.45, y + s * size, y - c * size * 0.45};
                g.fillPolygon(xs, ys, 4);
            }
        }
    }

    // ------------------------------------------------------------------
    // Les effets, de plus en plus présents
    // ------------------------------------------------------------------

    /**
     * Les feuilles qui tombent du feuillage, en se balançant, jusqu'au sol, dans les teintes de l'essence ; du cerisier
     * tombent des pétales, de petits ronds.
     */
    private static void fallingLeaves(GraphicsContext g, Shape shape, double baseY, double height, int stage, double time, Color[] tones,
            Essence e) {
        int count = Math.max(3, 3 + 3 * (stage - 7));
        if (look.season() == TreeSkinArt.Season.AUTUMN) count *= 2;
        boolean petals = e == Essence.CHERRY || look.season() == TreeSkinArt.Season.SPRING;
        for (int k = 0; k < count; k++) {
            double period = 7 + 5 * UniverseView.noise(k * 13 + 1);
            double t = ((time / period) + UniverseView.noise(k * 13 + 2)) % 1;
            double startX = shape.crownX + (UniverseView.noise(k * 13 + 3) - 0.5) * shape.crownRadius * 2;
            double x = startX + Math.sin(t * Math.PI * 4 + k) * height * 0.04;
            double y = shape.crownY + (baseY - shape.crownY) * t;
            Color tone = tones[t < 0.5 ? 1 : 2];
            double alpha = 0.85 * (1 - t * 0.6);
            if (petals) {
                double r = height * 0.007;
                g.setFill(tone);
                g.setGlobalAlpha(alpha);
                g.fillOval(x - r, y - r * 0.8, 2 * r, 1.6 * r);
            } else {
                leaf(g, e, x, y, height * 0.012, t * 9 + k, tone, alpha);
            }
        }
        g.setGlobalAlpha(1);
    }

    /** Les lucioles : elles flottent autour du feuillage, de plus en plus nombreuses. */
    private static void fireflies(GraphicsContext g, Shape shape, int stage, double time) {
        int count = 6 + 7 * (stage - 6);
        for (int k = 0; k < count; k++) {
            double a = 2 * Math.PI * UniverseView.noise(k * 19 + 1) + time * (0.15 + 0.2 * UniverseView.noise(k * 19 + 2));
            double d = shape.crownRadius * (0.5 + 1.1 * UniverseView.noise(k * 19 + 3));
            double x = shape.crownX + Math.cos(a) * d + Math.sin(time * 1.3 + k) * 6;
            double y = shape.crownY + Math.sin(a) * d * 0.7 + Math.cos(time * 1.1 + k) * 5;
            double blink = 0.5 + 0.5 * Math.sin(time * (2 + UniverseView.noise(k * 19 + 4) * 3) + k);
            UniverseView.glow(g, x, y, 6 + 6 * blink, FIREFLY, 0.25 + 0.45 * blink);
            g.setFill(FIREFLY);
            g.setGlobalAlpha(0.6 + 0.4 * blink);
            g.fillOval(x - 1.5, y - 1.5, 3, 3);
        }
        g.setGlobalAlpha(1);
    }

    /** La sève : des points de lumière qui montent dans le tronc et les grosses branches. */
    private static void sap(GraphicsContext g, Shape shape, int stage, double time) {
        int per = 1 + (stage - 8);
        for (int s = 0; s < shape.segments.size(); s++) {
            Segment seg = shape.segments.get(s);
            if (seg.depth() > (stage >= 10 ? 3 : 2)) continue;
            for (int k = 0; k < per; k++) {
                double t = ((time * 0.35 + UniverseView.noise(s * 7 + k)) % 1);
                double x = seg.x0() + (seg.x1() - seg.x0()) * t, y = seg.y0() + (seg.y1() - seg.y0()) * t;
                double r = Math.max(1.5, seg.width() * 0.12);
                UniverseView.glow(g, x, y, r * 4, SAP, 0.35);
                g.setFill(SAP);
                g.setGlobalAlpha(0.85);
                g.fillOval(x - r, y - r, 2 * r, 2 * r);
            }
        }
        g.setGlobalAlpha(1);
    }

    /** Les rayons qui tombent du feuillage, de plus en plus larges. */
    private static void rays(GraphicsContext g, Shape shape, double h, int stage, double time) {
        int count = 5 + 2 * (stage - 9);
        for (int k = 0; k < count; k++) {
            double off = (k - (count - 1) / 2.0) / count * shape.crownRadius * 2.4;
            double width = shape.crownRadius * (0.10 + 0.06 * UniverseView.noise(k * 3 + 7));
            double x0 = shape.crownX + off, y0 = shape.crownY - shape.crownRadius * 0.2, x1 = x0 + off * 0.45, y1 = h;
            g.setFill(GLOW);
            g.setGlobalAlpha(0.04 + 0.05 * (0.5 + 0.5 * Math.sin(time * 0.6 + k)));
            g.fillPolygon(new double[] {x0 - width * 0.4, x0 + width * 0.4, x1 + width, x1 - width},
                    new double[] {y0, y0, y1, y1}, 4);
        }
        g.setGlobalAlpha(1);
    }

    /** Pour l'arbre-monde : un ciel d'étoiles qui tourne, et de petits mondes en orbite autour du feuillage. */
    private static void cosmos(GraphicsContext g, double w, double h, Shape shape, double time) {
        for (int k = 0; k < 90; k++) {
            double x = w * UniverseView.noise(k * 5 + 101), y = h * 0.85 * UniverseView.noise(k * 5 + 102);
            double twinkle = 0.5 + 0.5 * Math.sin(time * (1 + UniverseView.noise(k * 5 + 103) * 2) + k);
            double r = 0.6 + 1.2 * UniverseView.noise(k * 5 + 104);
            g.setFill(STAR);
            g.setGlobalAlpha(0.25 + 0.6 * twinkle);
            g.fillOval(x - r, y - r, 2 * r, 2 * r);
        }
        Color[] worlds = {Color.web("#8fd3ff"), Color.web("#ffcf5a"), Color.web("#c9a8ff"), Color.web("#9ff0b0"), Color.web("#ff9f8a")};
        for (int k = 0; k < worlds.length; k++) {
            double a = time * (0.12 + 0.05 * k) + k * 1.3;
            double rx = shape.crownRadius * (1.25 + 0.22 * k), ry = rx * 0.32;
            double x = shape.crownX + Math.cos(a) * rx, y = shape.crownY + Math.sin(a) * ry;
            g.setStroke(STAR);
            g.setLineWidth(0.8);
            g.setGlobalAlpha(0.12);
            g.strokeOval(shape.crownX - rx, shape.crownY - ry, 2 * rx, 2 * ry);
            double r = shape.crownRadius * (0.035 + 0.012 * k);
            UniverseView.glow(g, x, y, r * 3, worlds[k], 0.35);
            g.setFill(worlds[k]);
            g.setGlobalAlpha(0.95);
            g.fillOval(x - r, y - r, 2 * r, 2 * r);
        }
        g.setGlobalAlpha(1);
    }

    /**
     * Le passage vers un palier d'arbre, de plus en plus marqué : des ondes qui partent du pied, plus nombreuses et plus
     * grandes à chaque palier, et une gerbe de lumière. {@code age} en secondes depuis le début du passage.
     */
    static void evolveBurst(GraphicsContext g, double w, double h, double x, double y, int stage, double age) {
        double p = 0.35 + 0.65 * prominence(stage);
        double t = Math.max(0, Math.min(1, age / SeedView.EVOLVE_SECONDS));
        int waves = 1 + (int) Math.round(4 * p);
        for (int k = 0; k < waves; k++) {
            double local = t * 1.4 - k * 0.12;
            if (local <= 0 || local >= 1) continue;
            double r = Math.max(w, h) * (0.15 + 0.9 * p) * local;
            g.setStroke(GLOW);
            g.setLineWidth(1.5 + 4 * p * (1 - local));
            g.setGlobalAlpha(0.7 * (1 - local));
            g.strokeOval(x - r, y - r * 0.55, 2 * r, 1.1 * r);
        }
        int sparks = (int) (20 + 120 * p);
        for (int k = 0; k < sparks; k++) {
            double a = -Math.PI * UniverseView.noise(k * 3 + 201), speed = h * (0.25 + 0.75 * UniverseView.noise(k * 3 + 202)) * p;
            double sx = x + Math.cos(a) * speed * t, sy = y + Math.sin(a) * speed * t + h * 0.15 * t * t;
            double r = 1 + 2.5 * UniverseView.noise(k * 3 + 203) * p;
            g.setFill(k % 3 == 0 ? SAP : GLOW);
            g.setGlobalAlpha(Math.max(0, 0.9 * (1 - t)));
            g.fillOval(sx - r, sy - r, 2 * r, 2 * r);
        }
        g.setGlobalAlpha(Math.max(0, 0.35 * p * (1 - t * 3)));
        g.setFill(GLOW);
        g.fillRect(0, 0, w, h);
        g.setGlobalAlpha(1);
    }
}
