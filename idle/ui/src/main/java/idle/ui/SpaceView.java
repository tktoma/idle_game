package idle.ui;

import idle.core.Assembly;
import idle.core.Game;
import idle.core.Molecule;
import java.util.List;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

/**
 * L'espace que l'expansion de la matière a créé, vu de dessus, avec les molécules qui l'occupent.
 *
 * <p>L'espace est un disque dont la surface vaut l'espace créé ({@code GameState.space()}) : son
 * bord recule à mesure que le temps passe. Les molécules s'y rangent en spirale depuis le centre,
 * à l'intérieur du cœur utilisé : le disque dont la surface vaut l'espace qu'elles occupent et
 * celui que réservent leurs lieux de rassemblement ({@link Game#usedSpace()}). Le cœur grossit
 * donc avec ce que fait le joueur, le bord avec le temps, et l'anneau vide entre les deux est
 * l'espace libre.
 *
 * <p>Les molécules rassemblées ({@link Game#formSubstance(String)}) occupent le centre, et celles
 * d'une même sorte y forment un seul amas : une surface pleine, sans fente ni trou entre ses
 * molécules, de la couleur de la molécule et cernée de la couleur de son état. Les états les plus serrés sont servis les premiers,
 * donc au plus près du centre : les métaux, puis les cristaux, les solides, les liquides, et les
 * gaz au bord, comme une planète de son noyau à son atmosphère. Les amas se cognent sans se
 * chevaucher : chacun s'arrête là où il touche son voisin, comme des bulles serrées les unes
 * contre les autres. Les molécules qui ne sont pas rassemblées restent au-delà, dans l'ordre de
 * leur création.
 *
 * <p>Au cœur de tout, les assemblages formés ({@link Game#formAssembly(String)}) : chacun est un
 * bloc de plusieurs centaines de molécules, bien plus gros qu'un amas. Il ne se dessine pas comme
 * eux : son corps est opaque, de la couleur de sa matière, sous un contour clair et épais ; ses
 * molécules de plusieurs sortes y sont mêlées, chacune réduite à un grain anguleux de sa couleur ;
 * et il porte son nom. On ne revoit ses molécules qu'en s'approchant tout près.
 *
 * <p>Le cœur est juste, les dessins ne le sont pas : les molécules s'y partagent la place à parts
 * égales, quelle que soit la taille de chacune. Une molécule lourde n'est donc pas dessinée
 * beaucoup plus grande qu'une légère, et aucun dessin ne dépasse du cœur, donc de l'espace.
 *
 * <p>La vue se règle comme une caméra :
 * <ul>
 *   <li>la molette, ou {@link #zoom(double)}, rapproche ou éloigne ;</li>
 *   <li>on ne peut pas reculer plus loin que ce qu'il faut pour voir tout l'espace : plus il y en
 *       a, plus on peut dézoomer ({@link #fit()} y va d'un coup) ;</li>
 *   <li>tirer avec la souris déplace la vue, sans sortir de l'espace.</li>
 * </ul>
 * De très loin, la matière est un gaz : chaque molécule n'est plus qu'une bouffée de sa couleur,
 * et les bouffées se fondent en nuages qui remuent lentement ({@link #mist()}). En s'approchant,
 * le gaz se dissipe et laisse voir les amas et leurs molécules : d'abord des points, puis leurs
 * atomes, leurs liaisons et leurs symboles ({@link MoleculeArt}). Tant que le joueur n'a pas rapproché la vue, elle suit
 * l'expansion et montre toujours tout l'espace.
 *
 * <p>L'ensemble tourne très lentement, et chaque molécule sur elle-même : c'est une image de
 * l'espace, pas une mesure.
 */
final class SpaceView extends Canvas {

    /** Échelle la plus rapprochée, en pixels par unité de longueur : une molécule d'eau y fait une centaine de pixels de large. */
    private static final double CLOSEST = 10;
    /** Part de la plus petite dimension de la vue qu'occupe le rayon de l'espace quand on le voit en entier. */
    private static final double FIT_SHARE = 0.44;
    /** Un cran de molette ou un clic sur un bouton multiplie l'échelle par ce nombre. */
    static final double ZOOM_STEP = 1.5;
    /**
     * Rayon de la place d'une molécule, en parts du rayon du cœur divisé par la racine du nombre de
     * molécules : à 0,8, deux voisines de la spirale se touchent à peine.
     */
    private static final double CELL_SHARE = 0.8;
    /** Part de sa place qu'occupe le dessin de la plus petite molécule ; la plus grosse l'occupe toute. */
    private static final double SMALLEST = 0.55;
    /** Nombre d'atomes à partir duquel un dessin occupe toute sa place. */
    private static final int BIGGEST_ATOMS = 24;
    /** L'angle d'or, en radians : d'une molécule à la suivante sur la spirale. */
    private static final double GOLDEN_ANGLE = Math.PI * (3 - Math.sqrt(5));
    /** Vitesse de rotation de l'ensemble, en radians par seconde : un tour en dix minutes environ. */
    private static final double DRIFT = 0.01;
    private static final int STARS = 90;

    private static final Color VOID = Color.web("#05060a");
    private static final Color SPACE = Color.web("#0e1320");
    private static final Color EDGE = Color.web(GameApp.BIG_BANG_COLOR);
    private static final Color CORE = Color.web("#3b4a66");
    private static final Color STAR = Color.web("#c9d6ea");
    private static final Color TEXT = Color.web("#8fa3b8");
    /** Le contour d'un bloc d'assemblage : clair, pour le détacher des amas, dont le liseré a la couleur de leur état. */
    static final Color BLOCK_EDGE = Color.web("#fff6dc");
    /** Épaisseur du contour d'un bloc, en rayons de place : le double du liseré d'un amas. */
    private static final double BLOCK_RIM = 0.22;
    /** Taille du grain qu'une molécule devient dans un bloc, en rayons de place. */
    private static final double GRAIN_SHARE = 0.5;
    /** Taille d'une place à l'écran, en pixels, à partir de laquelle un bloc laisse revoir ses molécules à la place des grains. */
    private static final double GRAINS_BELOW = 26;
    /** Largeur d'un bloc à l'écran, en pixels, à partir de laquelle son nom est écrit dessus. */
    private static final double NAME_FROM = 70;
    /** Les états de la matière, du plus serré au plus lâche : l'ordre dans lequel leurs amas prennent place depuis le centre. */
    private static final Molecule.State[] PLACES = {Molecule.State.METAL, Molecule.State.CRYSTAL, Molecule.State.SOLID,
            Molecule.State.LIQUID, Molecule.State.GAS};
    /** Part de la couleur d'un état dans le fond d'un amas, avant que la couleur de la molécule s'y ajoute. */
    private static final double PLACE_TINT = 0.16;
    /** Épaisseur du liseré qui cerne un amas de la couleur de son état, en rayons de place. */
    private static final double RIM = 0.1;
    /** Part de sa place qu'occupe le dessin d'une molécule rassemblée : le reste laisse voir son amas. */
    private static final double GATHERED_SHARE = 0.8;
    /**
     * Rayon le plus grand du rond qu'une molécule rassemblée apporte à l'amas de sa sorte, en
     * rayons de place : à 1,25, les ronds de deux voisines de la même sorte se recouvrent et
     * l'amas est d'un seul tenant. Un rond est plus petit dès qu'il rencontre un autre amas, une
     * molécule isolée ou le bord du cœur ({@link #collide}).
     */
    private static final double BLOB_SHARE = 1.25;
    /** Demi-largeur la plus grande d'un pont entre deux molécules d'un même amas, en rayons de place. */
    private static final double BRIDGE_SHARE = 1;
    /** En dessous de cette demi-largeur, en rayons de place, un pont coincé entre deux autres amas n'est pas dessiné. */
    private static final double THINNEST_BRIDGE = 0.2;
    /**
     * Ce qui garde un amas ramassé quand il choisit ses places : une place compte d'autant moins
     * qu'elle est loin de la première de l'amas, à raison de cette part de la distance. À 0, l'amas
     * s'étirerait en ruban ; trop haut, il laisserait des places isolées derrière lui.
     */
    private static final double COMPACT = 0.25;
    /**
     * Distance en deçà de laquelle deux places de la spirale sont voisines, en rayons de place, avant
     * le resserrement qui garde tout dans le cœur : deux voisines sont à 2,4 rayons l'une de l'autre.
     */
    private static final double NEIGHBOUR = 3.1;
    /**
     * Distance en deçà de laquelle deux molécules d'un même amas sont soudées par un pont, dans la
     * même unité : assez pour relier aussi les deux coins opposés de quatre places en carré, ce qui
     * ne laisse aucun trou au milieu.
     */
    private static final double JOIN = 3.6;
    /** Taille d'une place à l'écran, en pixels, en dessous de laquelle la matière commence à se fondre en gaz. */
    private static final double MIST_FROM = 5;
    /** Taille d'une place à l'écran, en pixels, en dessous de laquelle il ne reste que du gaz : plus aucun point. */
    private static final double MIST_FULL = 3;
    /** Rayon de la bouffée qu'une molécule apporte au gaz, en rayons de place : assez pour recouvrir ses voisines. */
    private static final double PUFF_SHARE = 2.4;
    /** Ce qui reste de la couleur d'une bouffée une fois posée : en dessous de 1, les bouffées voisines se mêlent. */
    private static final double PUFF_ALPHA = 0.45;
    /** Flou du gaz, en rayons de place, borné à ce que sait faire {@link GaussianBlur} (63 pixels). */
    private static final double BLUR_SHARE = 3.5;
    /** Flou le plus faible du gaz, en pixels : même de très loin, les bouffées restent fondues. */
    private static final double LEAST_BLUR = 4;
    /** Jusqu'où une bouffée s'écarte de sa place en remuant, en rayons de place. */
    private static final double CHURN = 0.7;
    /** Écart, en rayons de place, au-delà duquel deux ronds ne peuvent pas se toucher : deux fois le plus grand rond. */
    private static final double FAR = 2 * BLOB_SHARE;

    private final Game game;
    /** Pixels par unité de longueur ; {@code NaN} avant la première image. */
    private double scale = Double.NaN;
    /** Vrai tant que la vue suit l'expansion : elle montre tout l'espace, quel que soit sa taille. */
    private boolean following = true;
    /** Le point de l'espace au centre de la vue. */
    private double centerX = 0;
    private double centerY = 0;
    private double time = 0;
    private double dragX = Double.NaN;
    private double dragY = Double.NaN;

    /**
     * Les molécules placées : pour chacune, sa distance au centre (en parts du rayon du cœur occupé),
     * son angle, et le rayon de son dessin (en parts de sa place).
     */
    private double[] distances = new double[0];
    private double[] angles = new double[0];
    private double[] radii = new double[0];
    private MoleculeArt.Shape[] shapes = new MoleculeArt.Shape[0];
    /** Pour chaque place : l'état de la molécule rassemblée qui l'occupe, ou {@code null} si elle ne l'est pas. */
    private Molecule.State[] states = new Molecule.State[0];
    /** Pour chaque état de {@link #PLACES} : le nombre de molécules rassemblées dans cet état. */
    private final int[] gathered = new int[PLACES.length];
    /**
     * Les corps, du centre vers le bord : d'abord un bloc par assemblage formé, puis un amas par sorte
     * de molécule rassemblée. Pour chacun, les places de ses molécules.
     */
    private int[][] clusters = new int[0][];
    /** Pour chaque corps : son assemblage si c'est un bloc, {@code null} si c'est un amas. */
    private Assembly[] blocks = new Assembly[0];
    /** Pour chaque place : vrai si sa molécule est dans un bloc d'assemblage. */
    private boolean[] assembled = new boolean[0];
    /** Pour chaque place d'un bloc d'assemblage : la couleur de sa matière ; {@code null} ailleurs. De loin, c'est celle de sa bouffée de gaz. */
    private Color[] tints = new Color[0];
    /** Les quatre pointes du grain en cours de dessin, gardées d'une image à l'autre pour ne rien allouer. */
    private final double[] grainX = new double[4];
    private final double[] grainY = new double[4];
    /**
     * Les ponts de chaque amas : des paires de places (début, fin) de deux de ses molécules. Toutes
     * les voisines y sont reliées deux à deux ({@link #JOIN}), plus de quoi rattacher une molécule
     * restée à l'écart : avec eux, les ronds d'un amas forment une seule forme.
     */
    private int[][] bridges = new int[0][];
    /**
     * Les triangles pleins de chaque amas : des triplets de places reliées deux à deux par des
     * ponts. Ils bouchent le creux qui resterait entre trois ronds voisins.
     */
    private int[][] fills = new int[0][];
    /** La demi-largeur de chaque pont, en rayons de place, dans le même ordre : 0 quand il n'a pas la place de passer. */
    private double[][] widths = new double[0][];
    /** Pour chaque place : le rayon du rond de sa molécule dans son amas, en rayons de place ; 0 si elle n'est pas rassemblée. */
    private double[] blobs = new double[0];
    /** Le plus grand chevauchement entre deux ronds d'amas différents, en rayons de place : nul ou négatif si rien ne se chevauche. */
    private double overlap = 0;
    /** Nombre de paires de ronds d'amas différents qui se touchent exactement. */
    private int contacts = 0;
    /** Les trois pointes du triangle en cours de dessin, gardées d'une image à l'autre pour ne rien allouer. */
    private final double[] cornerX = new double[3];
    private final double[] cornerY = new double[3];
    /** Le flou du gaz, gardé d'une image à l'autre : seul son rayon change. */
    private final GaussianBlur blur = new GaussianBlur();
    /** Part de gaz dans la dernière image : 0 de près, 1 quand il ne reste que du gaz. */
    private double mist = 0;
    private int placedVersion = -1;
    /** Distance au centre du bord du dessin le plus éloigné, à la dernière image, en unités de longueur. */
    private double reach = 0;

    SpaceView(Game game) {
        super(600, 360);
        this.game = game;
        setOnScroll(event -> zoom(event.getDeltaY() > 0 ? ZOOM_STEP : 1 / ZOOM_STEP));
        setOnMousePressed(event -> {
            dragX = event.getX();
            dragY = event.getY();
        });
        setOnMouseDragged(event -> {
            if (Double.isNaN(dragX) || Double.isNaN(scale)) return;
            centerX -= (event.getX() - dragX) / scale;
            centerY -= (event.getY() - dragY) / scale;
            dragX = event.getX();
            dragY = event.getY();
            following = false;
        });
        setOnMouseReleased(event -> dragX = Double.NaN);
    }

    /** Rayon de l'espace créé, en unités de longueur : le disque dont la surface vaut l'espace. */
    private double spaceRadius() {
        // Borné : au-delà, ni l'écran ni un double n'y gagnent rien.
        double space = Math.min(game.state().space().toDouble(), 1e30);
        return Math.sqrt(Math.max(space, 0) / Math.PI);
    }

    /**
     * Rayon du cœur utilisé, en unités de longueur : le disque dont la surface vaut l'espace utilisé,
     * par les molécules et leurs lieux de rassemblement. Jamais plus grand que l'espace lui-même,
     * même si les outils de test y ont mis plus de molécules qu'il n'en contient.
     */
    private double coreRadius() {
        double occupied = Math.min(game.usedSpace().toDouble(), 1e30);
        return Math.min(spaceRadius(), Math.sqrt(Math.max(occupied, 0) / Math.PI));
    }

    /** L'échelle à laquelle tout l'espace tient dans la vue. Jamais plus rapprochée que {@link #CLOSEST}. */
    private double fitScale() {
        double radius = spaceRadius();
        double room = FIT_SHARE * Math.min(getWidth(), getHeight());
        return radius <= 0 || room <= 0 ? CLOSEST : Math.min(CLOSEST, room / radius);
    }

    /**
     * Rapproche ({@code factor} > 1) ou éloigne ({@code factor} < 1) la vue. Elle ne recule jamais
     * plus loin que ce qu'il faut pour voir tout l'espace, ni n'avance plus près que
     * {@link #CLOSEST}. Revenue à la vue d'ensemble, elle se remet à suivre l'expansion.
     */
    void zoom(double factor) {
        if (!(factor > 0) || Double.isNaN(scale)) return;
        double fit = fitScale();
        scale = Math.max(fit, Math.min(CLOSEST, scale * factor));
        following = scale <= fit * 1.0001;
        if (following) {
            centerX = 0;
            centerY = 0;
        }
    }

    /** Revient à la vue d'ensemble : tout l'espace, centré, et la vue suit de nouveau l'expansion. */
    void fit() {
        following = true;
        centerX = 0;
        centerY = 0;
        scale = fitScale();
    }

    /** Vrai si la vue ne peut pas reculer davantage : tout l'espace est déjà visible. */
    boolean isFitted() {
        return following;
    }

    /** Vrai si la vue ne peut pas s'approcher davantage. */
    boolean isClosest() {
        return !Double.isNaN(scale) && scale >= CLOSEST * 0.9999;
    }

    /** Le grossissement par rapport à la vue d'ensemble : 1 quand tout l'espace est visible. */
    double magnification() {
        return Double.isNaN(scale) ? 1 : scale / fitScale();
    }

    /** Jusqu'où va le dessin le plus éloigné du centre, à la dernière image, en parts du rayon de l'espace : pour les vérifications. */
    double reachShare() {
        double radius = spaceRadius();
        return radius <= 0 ? 0 : reach / radius;
    }

    /**
     * Part de gaz dans la dernière image : 0 tant qu'une place fait au moins {@link #MIST_FROM}
     * pixels, 1 quand elle en fait moins de {@link #MIST_FULL} et qu'il ne reste que des nuages.
     */
    double mist() {
        return mist;
    }

    /** Nombre de molécules placées dans la vue : pour les vérifications. */
    int placed() {
        return shapes.length;
    }

    /** Nombre de molécules rassemblées dans cet état, à la dernière image : pour les vérifications. */
    int gathered(Molecule.State place) {
        for (int index = 0; index < PLACES.length; index++) {
            if (PLACES[index] == place) return gathered[index];
        }
        return 0;
    }

    /** Nombre de ponts dessinés entre molécules rassemblées d'une même sorte : pour les vérifications. */
    int bridges() {
        int total = 0;
        for (double[] each : widths) {
            for (double width : each) {
                if (width > 0) total++;
            }
        }
        return total;
    }

    /** Nombre de triangles pleins entre molécules voisines d'un même amas : pour les vérifications. */
    int fills() {
        int total = 0;
        for (int[] each : fills) total += each.length / 3;
        return total;
    }

    /** Nombre de ponts qui n'ont pas la place de passer entre deux autres amas : pour les vérifications. */
    int blockedBridges() {
        int total = 0;
        for (double[] each : widths) {
            for (double width : each) {
                if (width <= 0) total++;
            }
        }
        return total;
    }

    /**
     * Le plus grand chevauchement entre deux ronds d'amas différents, en rayons de place, à la
     * dernière mise en place : nul ou négatif quand aucun amas n'en recouvre un autre. Pour les vérifications.
     */
    double overlap() {
        return overlap;
    }

    /** Nombre de paires de ronds d'amas différents qui se touchent exactement : pour les vérifications. */
    int contacts() {
        return contacts;
    }

    /** Nombre de corps, blocs d'assemblage et amas confondus : pour les vérifications. */
    int clusters() {
        return clusters.length;
    }

    /** Nombre de blocs d'assemblage placés dans la vue : pour les vérifications. */
    int blocks() {
        int total = 0;
        for (Assembly block : blocks) {
            if (block != null) total++;
        }
        return total;
    }

    /** Nombre de molécules placées dans des blocs d'assemblage : pour les vérifications. */
    int assembledCells() {
        int total = 0;
        for (boolean each : assembled) {
            if (each) total++;
        }
        return total;
    }

    /**
     * Range les molécules créées sur la spirale, quand leur liste a changé. Les places sont comptées
     * en parts du cœur utilisé, pas en unités d'espace : elles se resserrent ou s'écartent avec lui,
     * et aucune ne peut se retrouver hors de l'espace.
     *
     * <p>Les assemblages choisissent d'abord : chacun prend au centre un bloc de places qui se
     * touchent, et y mêle les molécules de ses ingrédients. Les molécules rassemblées qui restent
     * choisissent ensuite. Chaque sorte prend des places qui se touchent, en partant de la place
     * libre la plus proche du centre ({@link Growth#gather}) ; les sortes passent dans l'ordre des
     * états, du plus serré au plus lâche, et les métaux se retrouvent donc près du cœur, les gaz
     * vers le bord. Les autres molécules prennent les places qui
     * restent, du centre vers le bord, dans l'ordre de leur création. Les ronds des amas sont
     * ensuite taillés pour ne pas se chevaucher ({@link #collide}).
     *
     * <p>Tout cela ne regarde jamais que les places proches les unes des autres ({@link Grid}) :
     * le temps passé grandit comme le nombre de molécules, pas comme son carré.
     */
    private void place() {
        if (placedVersion == game.state().moleculesVersion()) return;
        placedVersion = game.state().moleculesVersion();
        List<String> log = game.state().moleculeLog();
        int count = log.size();
        distances = new double[count];
        angles = new double[count];
        radii = new double[count];
        shapes = new MoleculeArt.Shape[count];
        states = new Molecule.State[count];
        // La position de chaque place, en rayons de place : c'est dans cette unité que tout se mesure ici.
        double span = count == 0 ? 0 : Math.sqrt(count) / CELL_SHARE;
        double[] x = new double[count];
        double[] y = new double[count];
        for (int index = 0; index < count; index++) {
            // La spirale du tournesol : la k-ième place est à la racine de sa part du disque.
            distances[index] = count == 1 ? 0 : Math.sqrt((index + 0.5) / count);
            angles[index] = index * GOLDEN_ANGLE;
            x[index] = distances[index] * (span - 1) * Math.cos(angles[index]);
            y[index] = distances[index] * (span - 1) * Math.sin(angles[index]);
        }
        Grid grid = new Grid(x, y, span);
        // Ce qu'il reste à placer de chaque sorte rassemblée, une fois servis les assemblages.
        java.util.Map<String, Integer> spare = new java.util.HashMap<>();
        for (String id : game.state().substances()) {
            if (game.gatheredMolecules(id) > 0) spare.put(id, game.gatheredMolecules(id));
        }
        // Les corps, dans l'ordre où ils prennent place : les blocs des assemblages, dans l'ordre où ils
        // ont été formés, puis les amas. Pour chacun : ses molécules, une par place.
        List<List<Molecule>> bodies = new java.util.ArrayList<>();
        List<Assembly> formed = new java.util.ArrayList<>();
        for (String id : game.state().assemblies()) {
            Assembly assembly = game.assembly(id);
            List<Molecule> mixed = new java.util.ArrayList<>();
            for (java.util.Map.Entry<String, Integer> ingredient : assembly.ingredients().entrySet()) {
                int taken = Math.min(ingredient.getValue(), spare.getOrDefault(ingredient.getKey(), 0));
                spare.merge(ingredient.getKey(), -taken, Integer::sum);
                for (int copy = 0; copy < taken; copy++) mixed.add(game.molecule(ingredient.getKey()));
            }
            if (mixed.isEmpty()) continue;
            // Les ingrédients sont mêlés, toujours de la même façon pour un assemblage donné.
            java.util.Collections.shuffle(mixed, new java.util.Random(assembly.id().hashCode()));
            bodies.add(mixed);
            formed.add(assembly);
        }
        for (int place = 0; place < PLACES.length; place++) {
            gathered[place] = 0;
            for (Molecule molecule : game.molecules()) {
                int left = spare.getOrDefault(molecule.id(), 0);
                if (molecule.state() != PLACES[place] || left == 0) continue;
                bodies.add(java.util.Collections.nCopies(left, molecule));
                formed.add(null);
                gathered[place] += left;
            }
        }
        int[] owner = new int[count];
        java.util.Arrays.fill(owner, -1);
        clusters = new int[bodies.size()][];
        bridges = new int[bodies.size()][];
        blocks = formed.toArray(new Assembly[0]);
        assembled = new boolean[count];
        tints = new Color[count];
        // Deux places sont voisines en deçà de cette distance : un peu plus que l'écart entre deux places de la spirale.
        double reachable = count == 0 ? 0 : NEIGHBOUR * (span - 1) / span;
        double join = count == 0 ? 0 : JOIN * (span - 1) / span;
        Growth growth = new Growth(x, y, owner, grid, reachable);
        int taken = 0;
        for (int cluster = 0; cluster < clusters.length; cluster++) {
            List<Molecule> body = bodies.get(cluster);
            int size = Math.min(body.size(), count - taken);
            int[] tree = new int[2 * Math.max(0, size - 1)];
            clusters[cluster] = growth.gather(size, cluster, tree);
            bridges[cluster] = weld(clusters[cluster], tree, x, y, join);
            Color tint = blocks[cluster] == null ? null : Color.web(blocks[cluster].tint());
            for (int index = 0; index < size; index++) {
                int cell = clusters[cluster][index];
                settle(cell, body.get(index), body.get(index).state());
                assembled[cell] = tint != null;
                tints[cell] = tint;
            }
            taken += size;
        }
        int next = 0;
        for (String id : log) {
            if (game.gatheredMolecules(id) > 0) continue;
            while (next < count && owner[next] >= 0) next++;
            if (next >= count) break;
            settle(next++, game.molecule(id), null);
        }
        collide(x, y, owner, span, grid);
    }

    /**
     * Les places rangées dans les cases d'un quadrillage, pour retrouver vite celles qui sont près
     * d'un endroit donné sans les passer toutes en revue. Une case fait {@link #FAR} de côté : ce qui
     * peut toucher un rond tient dans les cases voisines de la sienne.
     */
    private static final class Grid {
        private final double[] x;
        private final double[] y;
        private final double origin;
        private final int side;
        /** Pour chaque case : la première place qui s'y trouve, ou -1 ; pour chaque place : la suivante de sa case, ou -1. */
        private final int[] head;
        private final int[] next;
        /** Les places trouvées par la dernière recherche ({@link #near}). */
        private int[] found = new int[64];

        Grid(double[] x, double[] y, double span) {
            this.x = x;
            this.y = y;
            origin = -span - 1;
            side = Math.max(1, (int) Math.ceil(2 * (span + 1) / FAR));
            head = new int[side * side];
            next = new int[x.length];
            java.util.Arrays.fill(head, -1);
            for (int cell = 0; cell < x.length; cell++) {
                int box = box(x[cell]) + side * box(y[cell]);
                next[cell] = head[box];
                head[box] = cell;
            }
        }

        private int box(double coordinate) {
            return Math.max(0, Math.min(side - 1, (int) ((coordinate - origin) / FAR)));
        }

        /**
         * Cherche les places des cases que touche le rectangle donné : toutes celles qui sont dedans,
         * et quelques-unes autour. Elles sont dans {@link #found()}, valable jusqu'à la recherche suivante.
         *
         * @return le nombre de places trouvées
         */
        int near(double left, double top, double right, double bottom) {
            int count = 0;
            int lastColumn = box(right);
            int lastRow = box(bottom);
            for (int row = box(top); row <= lastRow; row++) {
                for (int column = box(left); column <= lastColumn; column++) {
                    for (int cell = head[column + side * row]; cell >= 0; cell = next[cell]) {
                        if (count == found.length) found = java.util.Arrays.copyOf(found, 2 * count);
                        found[count++] = cell;
                    }
                }
            }
            return count;
        }

        int[] found() {
            return found;
        }
    }

    /**
     * Le choix des places des amas, l'un après l'autre. Un amas part de la place libre la plus
     * proche du centre qui a encore des voisines libres ; chaque place suivante est la place libre
     * la plus proche de celles qu'il a déjà, avec une préférence pour celles qui restent près de la
     * première ({@link #COMPACT}). Les places d'un amas se touchent donc, tant qu'il en reste de
     * libres à côté. Une place libre isolée entre deux amas est laissée à une molécule qui n'est
     * pas rassemblée, sauf s'il n'y a plus que cela.
     */
    private static final class Growth {
        private final double[] x;
        private final double[] y;
        private final int[] owner;
        private final Grid grid;
        private final double reachable;
        /** Jusqu'où un amas regarde autour de ses places pour trouver la suivante. */
        private final double sight;
        /** La première place qui peut encore être libre : tout ce qui est avant est pris. */
        private int from = 0;
        /** Le bord de l'amas en cours : les places libres à portée de vue de l'une des siennes. */
        private int[] front = new int[64];
        private int fronted = 0;
        private final boolean[] inFront;
        /** Pour chaque place du bord : la plus proche de celles de l'amas, et à quelle distance. */
        private final double[] nearest;
        private final int[] anchor;

        Growth(double[] x, double[] y, int[] owner, Grid grid, double reachable) {
            this.x = x;
            this.y = y;
            this.owner = owner;
            this.grid = grid;
            this.reachable = reachable;
            this.sight = 1.5 * reachable;
            inFront = new boolean[x.length];
            nearest = new double[x.length];
            anchor = new int[x.length];
        }

        /**
         * Choisit les {@code size} places de l'amas numéro {@code cluster}, et les note dans {@code owner}.
         *
         * @param link reçoit les ponts qui ont construit l'amas, deux places par pont : chaque place y
         *             est reliée à celle de l'amas dont elle était la plus proche
         * @return les places choisies
         */
        int[] gather(int size, int cluster, int[] link) {
            int[] group = new int[size];
            if (size == 0) return group;
            int seed = seed(Math.min(size - 1, 2));
            group[0] = seed;
            owner[seed] = cluster;
            fronted = 0;
            look(seed);
            for (int step = 1; step < size; step++) {
                int chosen = -1;
                double least = Double.POSITIVE_INFINITY;
                int kept = 0;
                for (int index = 0; index < fronted; index++) {
                    int cell = front[index];
                    if (owner[cell] >= 0) {
                        inFront[cell] = false;
                        continue;
                    }
                    front[kept++] = cell;
                    double cost = nearest[cell] + COMPACT * Math.hypot(x[cell] - x[seed], y[cell] - y[seed]);
                    if (cost < least) {
                        least = cost;
                        chosen = cell;
                    }
                }
                fronted = kept;
                if (chosen < 0) chosen = stray(seed, group, step);
                group[step] = chosen;
                owner[chosen] = cluster;
                link[2 * step - 2] = anchor[chosen];
                link[2 * step - 1] = chosen;
                look(chosen);
            }
            for (int index = 0; index < fronted; index++) inFront[front[index]] = false;
            fronted = 0;
            return group;
        }

        /**
         * La première place d'un amas : la place libre la plus proche du centre qui a au moins
         * {@code wanted} voisines libres, ou à défaut la place libre la plus proche du centre.
         */
        private int seed(int wanted) {
            while (owner[from] >= 0) from++;
            for (int cell = from; cell < owner.length; cell++) {
                if (owner[cell] >= 0) continue;
                int free = 0;
                int close = grid.near(x[cell] - reachable, y[cell] - reachable, x[cell] + reachable, y[cell] + reachable);
                int[] found = grid.found();
                for (int index = 0; index < close && free < wanted; index++) {
                    int other = found[index];
                    if (other != cell && owner[other] < 0 && Math.hypot(x[other] - x[cell], y[other] - y[cell]) <= reachable) free++;
                }
                if (free >= wanted) return cell;
            }
            return from;
        }

        /** Ajoute au bord de l'amas les places libres à portée de vue de {@code member}, qui vient de le rejoindre. */
        private void look(int member) {
            int close = grid.near(x[member] - sight, y[member] - sight, x[member] + sight, y[member] + sight);
            int[] found = grid.found();
            for (int index = 0; index < close; index++) {
                int cell = found[index];
                if (owner[cell] >= 0) continue;
                double gap = Math.hypot(x[cell] - x[member], y[cell] - y[member]);
                if (gap > sight) continue;
                if (!inFront[cell]) {
                    inFront[cell] = true;
                    nearest[cell] = gap;
                    anchor[cell] = member;
                    if (fronted == front.length) front = java.util.Arrays.copyOf(front, 2 * fronted);
                    front[fronted++] = cell;
                } else if (gap < nearest[cell]) {
                    nearest[cell] = gap;
                    anchor[cell] = member;
                }
            }
        }

        /**
         * Quand l'amas est cerné et qu'il lui manque encore des places : la place libre la plus proche
         * de sa première place, où qu'elle soit, rattachée à la plus proche de ses molécules.
         */
        private int stray(int seed, int[] group, int members) {
            int chosen = -1;
            double least = Double.POSITIVE_INFINITY;
            for (int cell = from; cell < owner.length; cell++) {
                if (owner[cell] >= 0) continue;
                double gap = Math.hypot(x[cell] - x[seed], y[cell] - y[seed]);
                if (gap < least) {
                    least = gap;
                    chosen = cell;
                }
            }
            nearest[chosen] = Double.POSITIVE_INFINITY;
            for (int index = 0; index < members; index++) {
                double gap = Math.hypot(x[chosen] - x[group[index]], y[chosen] - y[group[index]]);
                if (gap < nearest[chosen]) {
                    nearest[chosen] = gap;
                    anchor[chosen] = group[index];
                }
            }
            return chosen;
        }
    }

    /**
     * Les ponts d'un amas : ceux qui l'ont construit ({@code tree}, un par molécule ajoutée), plus un
     * pont entre deux de ses molécules chaque fois qu'elles sont à moins de {@code join} l'une de
     * l'autre. Un amas n'est donc pas un chapelet de molécules reliées à la file, mais un tissu où
     * chacune tient à toutes ses voisines.
     *
     * @return les ponts, deux places par pont, sans doublon
     */
    private static int[] weld(int[] cells, int[] tree, double[] x, double[] y, double join) {
        java.util.Set<Long> seen = new java.util.HashSet<>();
        List<Integer> pairs = new java.util.ArrayList<>();
        for (int link = 0; link < tree.length; link += 2) {
            if (seen.add(pair(tree[link], tree[link + 1]))) {
                pairs.add(tree[link]);
                pairs.add(tree[link + 1]);
            }
        }
        for (int first = 0; first < cells.length; first++) {
            for (int second = first + 1; second < cells.length; second++) {
                int from = cells[first];
                int to = cells[second];
                if (Math.abs(x[to] - x[from]) > join || Math.abs(y[to] - y[from]) > join) continue;
                if (Math.hypot(x[to] - x[from], y[to] - y[from]) > join) continue;
                if (seen.add(pair(from, to))) {
                    pairs.add(from);
                    pairs.add(to);
                }
            }
        }
        int[] links = new int[pairs.size()];
        for (int index = 0; index < links.length; index++) links[index] = pairs.get(index);
        return links;
    }

    /** Une clé pour la paire de places (a, b), la même dans les deux sens. */
    private static long pair(int a, int b) {
        return ((long) Math.min(a, b) << 32) | Math.max(a, b);
    }

    /**
     * La collision des amas : taille le rond de chaque molécule rassemblée, puis chaque pont, pour
     * qu'aucun amas n'en recouvre un autre. Tout se mesure en rayons de place.
     *
     * <p>Un rond a le rayon {@link #BLOB_SHARE}, sauf s'il rencontre avant :
     * <ul>
     *   <li>le rond d'un autre amas : chacun s'arrête à mi-chemin, et les deux se touchent sans se
     *       recouvrir ;</li>
     *   <li>le dessin d'une molécule qui n'est pas rassemblée : le rond s'arrête à son bord ;</li>
     *   <li>le bord du cœur utilisé : aucun amas n'en dépasse.</li>
     * </ul>
     * Les ronds d'un même amas, eux, se recouvrent librement : c'est ce qui les soude. Un pont est
     * aussi large qu'il peut l'être sans toucher un rond ou un pont d'un autre amas ; s'il n'a pas
     * la place de passer, il n'est pas dessiné ({@link #THINNEST_BRIDGE}). Trois molécules reliées
     * deux à deux ferment un triangle, qui est rempli s'il ne contient rien d'étranger à l'amas.
     *
     * @param owner l'amas de chaque place, ou -1 si sa molécule n'est pas rassemblée
     * @param span  le rayon du cœur utilisé
     */
    private void collide(double[] x, double[] y, int[] owner, double span, Grid grid) {
        int count = x.length;
        blobs = new double[count];
        for (int index = 0; index < count; index++) {
            if (owner[index] < 0) continue;
            double radius = Math.min(BLOB_SHARE, span - Math.hypot(x[index], y[index]));
            // Au-delà de deux ronds entiers, rien ne peut se toucher : seules les places proches sont mesurées.
            int close = grid.near(x[index] - FAR, y[index] - FAR, x[index] + FAR, y[index] + FAR);
            int[] found = grid.found();
            for (int each = 0; each < close; each++) {
                int other = found[each];
                if (owner[other] == owner[index]) continue;
                double gap = Math.hypot(x[other] - x[index], y[other] - y[index]);
                radius = Math.min(radius, owner[other] < 0 ? gap - radii[other] : gap / 2);
            }
            blobs[index] = Math.max(0, radius);
        }
        overlap = Double.NEGATIVE_INFINITY;
        contacts = 0;
        for (int index = 0; index < count; index++) {
            if (owner[index] < 0) continue;
            int close = grid.near(x[index] - FAR, y[index] - FAR, x[index] + FAR, y[index] + FAR);
            int[] found = grid.found();
            for (int each = 0; each < close; each++) {
                int other = found[each];
                if (other <= index || owner[other] < 0 || owner[other] == owner[index]) continue;
                double excess = blobs[index] + blobs[other] - Math.hypot(x[other] - x[index], y[other] - y[index]);
                overlap = Math.max(overlap, excess);
                if (Math.abs(excess) < 1e-9) contacts++;
            }
        }
        if (overlap == Double.NEGATIVE_INFINITY) overlap = 0;

        // Les ponts déjà posés, retrouvés par leur place de départ : pour chaque place, le premier pont
        // qui en part, puis de pont en pont. Les rares ponts très longs sont gardés à part.
        int total = 0;
        for (int[] links : bridges) total += links.length / 2;
        int[] laidFrom = new int[total];
        int[] laidTo = new int[total];
        double[] laidWidth = new double[total];
        int[] laidCluster = new int[total];
        int[] laidNext = new int[total];
        int[] firstLaid = new int[count];
        java.util.Arrays.fill(firstLaid, -1);
        List<Integer> longOnes = new java.util.ArrayList<>();
        double longest = 2 * FAR;
        int laid = 0;

        widths = new double[bridges.length][];
        for (int cluster = 0; cluster < bridges.length; cluster++) {
            int[] links = bridges[cluster];
            widths[cluster] = new double[links.length / 2];
            for (int link = 0; link < links.length; link += 2) {
                int from = links[link];
                int to = links[link + 1];
                double width = Math.min(BRIDGE_SHARE, Math.min(blobs[from], blobs[to]));
                double left = Math.min(x[from], x[to]) - FAR;
                double right = Math.max(x[from], x[to]) + FAR;
                double top = Math.min(y[from], y[to]) - FAR;
                double bottom = Math.max(y[from], y[to]) + FAR;
                // Ce qui n'est pas de l'amas ne doit pas être touché : les ronds des autres, les molécules isolées...
                int close = grid.near(left, top, right, bottom);
                int[] found = grid.found();
                for (int each = 0; each < close && width > 0; each++) {
                    int other = found[each];
                    if (owner[other] == cluster) continue;
                    double body = owner[other] < 0 ? radii[other] : blobs[other];
                    width = Math.min(width, away(x[other], y[other], x[from], y[from], x[to], y[to]) - body);
                }
                // ... et les ponts déjà posés des autres amas : ceux qui partent d'une place proche, et les très longs.
                if (width > 0) {
                    close = grid.near(left - longest, top - longest, right + longest, bottom + longest);
                    found = grid.found();
                    for (int each = 0; each < close && width > 0; each++) {
                        for (int other = firstLaid[found[each]]; other >= 0 && width > 0; other = laidNext[other]) {
                            if (laidCluster[other] == cluster) continue;
                            width = Math.min(width, apart(x[from], y[from], x[to], y[to],
                                    x[laidFrom[other]], y[laidFrom[other]], x[laidTo[other]], y[laidTo[other]]) - laidWidth[other]);
                        }
                    }
                    for (int index = 0; index < longOnes.size() && width > 0; index++) {
                        int other = longOnes.get(index);
                        if (laidCluster[other] == cluster) continue;
                        width = Math.min(width, apart(x[from], y[from], x[to], y[to],
                                x[laidFrom[other]], y[laidFrom[other]], x[laidTo[other]], y[laidTo[other]]) - laidWidth[other]);
                    }
                }
                if (width < THINNEST_BRIDGE) width = 0;
                widths[cluster][link / 2] = width;
                if (width > 0) {
                    laidFrom[laid] = from;
                    laidTo[laid] = to;
                    laidWidth[laid] = width;
                    laidCluster[laid] = cluster;
                    if (Math.hypot(x[to] - x[from], y[to] - y[from]) > longest) {
                        laidNext[laid] = -1;
                        longOnes.add(laid);
                    } else {
                        laidNext[laid] = firstLaid[from];
                        firstLaid[from] = laid;
                    }
                    laid++;
                }
            }
        }

        fills = new int[bridges.length][];
        for (int cluster = 0; cluster < bridges.length; cluster++) fills[cluster] = triangles(cluster, x, y, owner, grid);
    }

    /**
     * Les triangles pleins d'un amas : chaque triplet de ses molécules reliées deux à deux par des
     * ponts dessinés, s'il ne contient aucune place étrangère à l'amas. Comme ses trois côtés sont
     * des ponts qui ont eu la place de passer, rien d'un autre amas ne peut y entrer.
     *
     * @return les triangles, trois places par triangle
     */
    private int[] triangles(int cluster, double[] x, double[] y, int[] owner, Grid grid) {
        int[] links = bridges[cluster];
        java.util.Map<Integer, List<Integer>> joined = new java.util.HashMap<>();
        java.util.Set<Long> drawn = new java.util.HashSet<>();
        for (int link = 0; link < links.length; link += 2) {
            if (widths[cluster][link / 2] <= 0) continue;
            drawn.add(pair(links[link], links[link + 1]));
            joined.computeIfAbsent(links[link], each -> new java.util.ArrayList<>()).add(links[link + 1]);
            joined.computeIfAbsent(links[link + 1], each -> new java.util.ArrayList<>()).add(links[link]);
        }
        List<Integer> kept = new java.util.ArrayList<>();
        for (int link = 0; link < links.length; link += 2) {
            int a = links[link];
            int b = links[link + 1];
            if (!drawn.contains(pair(a, b))) continue;
            for (int c : joined.get(a)) {
                // Chaque triangle une seule fois : par son côté dont la troisième pointe a le plus grand numéro.
                if (c <= Math.max(a, b) || !drawn.contains(pair(b, c))) continue;
                int close = grid.near(Math.min(x[a], Math.min(x[b], x[c])), Math.min(y[a], Math.min(y[b], y[c])),
                        Math.max(x[a], Math.max(x[b], x[c])), Math.max(y[a], Math.max(y[b], y[c])));
                int[] found = grid.found();
                boolean empty = true;
                for (int each = 0; each < close && empty; each++) {
                    int other = found[each];
                    if (owner[other] == cluster) continue;
                    double side1 = (x[b] - x[a]) * (y[other] - y[a]) - (y[b] - y[a]) * (x[other] - x[a]);
                    double side2 = (x[c] - x[b]) * (y[other] - y[b]) - (y[c] - y[b]) * (x[other] - x[b]);
                    double side3 = (x[a] - x[c]) * (y[other] - y[c]) - (y[a] - y[c]) * (x[other] - x[c]);
                    empty = !((side1 >= 0 && side2 >= 0 && side3 >= 0) || (side1 <= 0 && side2 <= 0 && side3 <= 0));
                }
                if (!empty) continue;
                kept.add(a);
                kept.add(b);
                kept.add(c);
            }
        }
        int[] result = new int[kept.size()];
        for (int index = 0; index < result.length; index++) result[index] = kept.get(index);
        return result;
    }

    /** Distance du point (px, py) au segment qui va de (ax, ay) à (bx, by). */
    private static double away(double px, double py, double ax, double ay, double bx, double by) {
        double dx = bx - ax;
        double dy = by - ay;
        double length = dx * dx + dy * dy;
        double along = length <= 0 ? 0 : Math.max(0, Math.min(1, ((px - ax) * dx + (py - ay) * dy) / length));
        return Math.hypot(px - (ax + along * dx), py - (ay + along * dy));
    }

    /** Distance entre le segment (a, b) et le segment (c, d) : nulle s'ils se croisent. */
    private static double apart(double ax, double ay, double bx, double by, double cx, double cy, double dx, double dy) {
        double side1 = (bx - ax) * (cy - ay) - (by - ay) * (cx - ax);
        double side2 = (bx - ax) * (dy - ay) - (by - ay) * (dx - ax);
        double side3 = (dx - cx) * (ay - cy) - (dy - cy) * (ax - cx);
        double side4 = (dx - cx) * (by - cy) - (dy - cy) * (bx - cx);
        if (side1 * side2 < 0 && side3 * side4 < 0) return 0;
        return Math.min(Math.min(away(ax, ay, cx, cy, dx, dy), away(bx, by, cx, cy, dx, dy)),
                Math.min(away(cx, cy, ax, ay, bx, by), away(dx, dy, ax, ay, bx, by)));
    }

    /**
     * La couleur de l'amas d'une sorte de molécule : celle de la molécule, fondue dans la teinte de
     * son état, d'autant plus présente que l'état est serré. Elle est opaque, pour que les ronds et
     * les ponts d'un même amas se recouvrent sans que leur teinte s'additionne.
     */
    private static Color blobColor(MoleculeArt.Shape shape, Molecule.State state) {
        double share = switch (state) {
            case GAS -> 0.18;
            case LIQUID -> 0.34;
            case SOLID -> 0.5;
            case CRYSTAL -> 0.6;
            case METAL -> 0.7;
        };
        return SPACE.interpolate(placeColor(state), PLACE_TINT).interpolate(shape.dominant(), share);
    }

    /** Installe une molécule à une place de la spirale, dans le lieu de {@code state} ou hors de tout lieu ({@code null}). */
    private void settle(int cell, Molecule molecule, Molecule.State state) {
        // Une grosse molécule est dessinée un peu plus grande qu'une petite, sans dépasser sa place.
        radii[cell] = SMALLEST + (1 - SMALLEST) * Math.min(1, Math.sqrt(molecule.atoms() / (double) BIGGEST_ATOMS));
        shapes[cell] = MoleculeArt.of(molecule);
        states[cell] = state;
    }

    /**
     * La couleur d'un état, celle du liseré de ses amas : pâle pour les gaz, bleue pour les liquides,
     * sable pour les solides, violette pour les cristaux, acier pour les métaux.
     */
    private static Color placeColor(Molecule.State place) {
        return switch (place) {
            case GAS -> Color.web("#bfe6f2");
            case LIQUID -> Color.web("#4d8fe0");
            case SOLID -> Color.web("#d9b36c");
            case CRYSTAL -> Color.web("#b48cf2");
            case METAL -> Color.web("#aeb6c4");
        };
    }

    /**
     * Fait avancer le temps de la vue et la redessine.
     *
     * @param elapsed secondes écoulées depuis l'image précédente
     */
    void frame(double elapsed) {
        time += elapsed;
        double w = getWidth();
        double h = getHeight();
        if (w <= 0 || h <= 0) return;
        place();
        double fit = fitScale();
        if (Double.isNaN(scale) || following) scale = fit;
        scale = Math.max(fit, Math.min(CLOSEST, scale));
        double radius = spaceRadius();
        // La vue ne sort pas de l'espace.
        double away = Math.hypot(centerX, centerY);
        if (away > radius && away > 0) {
            centerX *= radius / away;
            centerY *= radius / away;
        }

        GraphicsContext g = getGraphicsContext2D();
        g.clearRect(0, 0, w, h);
        g.setGlobalAlpha(1);
        g.setFill(VOID);
        g.fillRect(0, 0, w, h);
        double originX = w / 2 - centerX * scale;
        double originY = h / 2 - centerY * scale;

        // L'espace créé : un disque un peu plus clair que le vide. Son bord est tracé plus bas, par-dessus le gaz.
        double edge = radius * scale;
        if (edge > 0.5) {
            g.setFill(SPACE);
            g.fillOval(originX - edge, originY - edge, 2 * edge, 2 * edge);
        }
        // Les molécules se partagent le cœur utilisé à parts égales : la taille d'un dessin vient de sa
        // place, pas du volume de sa molécule, et rien ne dépasse du cœur, ni un dessin ni un amas.
        double heart = coreRadius();
        int count = shapes.length;
        double cell = count == 0 ? 0 : CELL_SHARE * heart / Math.sqrt(count);
        double turn = time * DRIFT;
        double[] xs = new double[count];
        double[] ys = new double[count];
        reach = 0;
        for (int index = 0; index < count; index++) {
            double angle = angles[index] + turn;
            double out = distances[index] * (heart - cell);
            reach = Math.max(reach, out + (states[index] != null ? blobs[index] : radii[index]) * cell);
            xs[index] = originX + out * Math.cos(angle) * scale;
            ys[index] = originY + out * Math.sin(angle) * scale;
        }
        double unit = cell * scale;
        // De loin, la matière se fond en gaz : pas du tout au-dessus de MIST_FROM pixels par place, entièrement sous MIST_FULL.
        mist = count == 0 ? 0 : Math.max(0, Math.min(1, (MIST_FROM - unit) / (MIST_FROM - MIST_FULL)));

        // Les amas d'abord, sous les molécules : pour chaque sorte rassemblée, un rond par molécule, un
        // pont entre voisines et un triangle plein entre trois voisines, donc une seule surface. Elle est
        // dessinée deux fois : en entier dans la couleur de son état, puis un peu rétrécie dans sa couleur
        // à elle, ce qui lui laisse un liseré tout autour. De très loin, le gaz les remplace. Ronds et ponts ont été taillés pour ne recouvrir aucun autre amas ({@link #collide}).
        double widest = BLOB_SHARE * unit;
        if (unit >= 2 && mist < 1) {
            // Un pont s'arrête net au centre de ses deux ronds : avec un bout carré, ses coins en dépasseraient.
            g.setLineCap(StrokeLineCap.BUTT);
            for (int cluster = 0; cluster < clusters.length; cluster++) {
                if (clusters[cluster].length == 0) continue;
                int first = clusters[cluster][0];
                Assembly block = blocks[cluster];
                // Un amas : sa couleur sous un fin liseré de la couleur de son état. Un bloc d'assemblage : la
                // couleur de sa matière sous un contour clair, deux fois plus épais. De loin, un contour plus
                // fin qu'un pixel n'est pas dessiné, et le corps est d'une seule couleur.
                double rim = block != null ? BLOCK_RIM : RIM;
                if (rim * unit < 0.75) rim = 0;
                for (int pass = rim > 0 ? 0 : 1; pass < 2; pass++) {
                    Color color = block != null ? (pass == 0 ? BLOCK_EDGE : Color.web(block.tint()))
                            : pass == 0 ? placeColor(states[first]) : blobColor(shapes[first], states[first]);
                    double inset = pass == 0 ? 0 : rim;
                    g.setFill(color);
                    g.setStroke(color);
                    for (int index : clusters[cluster]) {
                        double x = xs[index];
                        double y = ys[index];
                        double round = (blobs[index] - inset) * unit;
                        if (round <= 0 || x + round < 0 || x - round > w || y + round < 0 || y - round > h) continue;
                        g.fillOval(x - round, y - round, 2 * round, 2 * round);
                    }
                    int[] links = bridges[cluster];
                    for (int link = 0; link < links.length; link += 2) {
                        double width = widths[cluster][link / 2] - inset;
                        if (width <= 0) continue;
                        int from = links[link];
                        int to = links[link + 1];
                        if (Math.max(xs[from], xs[to]) + widest < 0 || Math.min(xs[from], xs[to]) - widest > w
                                || Math.max(ys[from], ys[to]) + widest < 0 || Math.min(ys[from], ys[to]) - widest > h) continue;
                        g.setLineWidth(2 * width * unit);
                        g.strokeLine(xs[from], ys[from], xs[to], ys[to]);
                    }
                    // Le creux entre trois voisines, rempli jusqu'à leurs centres : il est tout entier à
                    // l'intérieur de l'amas, et ne mord donc jamais sur le liseré.
                    int[] corners = fills[cluster];
                    for (int corner = 0; corner < corners.length; corner += 3) {
                        for (int point = 0; point < 3; point++) {
                            cornerX[point] = xs[corners[corner + point]];
                            cornerY[point] = ys[corners[corner + point]];
                        }
                        if (Math.max(cornerX[0], Math.max(cornerX[1], cornerX[2])) < 0
                                || Math.min(cornerX[0], Math.min(cornerX[1], cornerX[2])) > w
                                || Math.max(cornerY[0], Math.max(cornerY[1], cornerY[2])) < 0
                                || Math.min(cornerY[0], Math.min(cornerY[1], cornerY[2])) > h) continue;
                        g.fillPolygon(cornerX, cornerY, 3);
                    }
                }
            }
            g.setLineCap(StrokeLineCap.SQUARE);
        }

        // Le gaz : une bouffée par molécule, de sa couleur, qui s'écarte un peu de sa place et y revient.
        // Tout ce qui est dessiné jusqu'ici est ensuite flouté d'un coup, et les bouffées deviennent des nuages.
        if (mist > 0) {
            double puff = Math.max(1, PUFF_SHARE * unit);
            double churn = CHURN * unit * mist;
            g.setGlobalAlpha(PUFF_ALPHA * mist);
            for (int index = 0; index < count; index++) {
                double x = xs[index] + churn * Math.sin(time * 0.35 + index * 1.7);
                double y = ys[index] + churn * Math.cos(time * 0.28 + index * 2.3);
                if (x + puff < 0 || x - puff > w || y + puff < 0 || y - puff > h) continue;
                g.setFill(tints[index] != null ? tints[index] : shapes[index].dominant());
                g.fillOval(x - puff, y - puff, 2 * puff, 2 * puff);
            }
            g.setGlobalAlpha(1);
            blur.setRadius(Math.min(63, Math.max(LEAST_BLUR, BLUR_SHARE * unit) * mist));
            g.applyEffect(blur);
        }

        // Ce qui reste net par-dessus le gaz : les étoiles, le cercle discret du cœur utilisé, le bord de l'espace.
        drawStars(g, w, h);
        double core = coreRadius() * scale;
        if (core > 4) {
            g.setStroke(CORE);
            g.setLineWidth(1);
            g.strokeOval(originX - core, originY - core, 2 * core, 2 * core);
        }
        if (edge > 0.5) {
            g.setStroke(EDGE);
            g.setLineWidth(1.5);
            g.strokeOval(originX - edge, originY - edge, 2 * edge, 2 * edge);
        }

        // Puis les molécules, celles qui tombent dans la vue seulement : elles s'effacent à mesure que le gaz les remplace.
        if (mist < 1) {
            g.setGlobalAlpha(1 - mist);
            for (int index = 0; index < count; index++) {
                double x = xs[index];
                double y = ys[index];
                double size = radii[index] * cell * scale;
                if (x + size < 0 || x - size > w || y + size < 0 || y - size > h) continue;
                // Dans un bloc d'assemblage, une molécule n'est qu'un grain anguleux et fixe, tant qu'on n'est pas tout près.
                if (assembled[index] && unit < GRAINS_BELOW) {
                    if (unit >= 2.5) grain(g, shapes[index].dominant(), x, y, GRAIN_SHARE * unit, index * 0.9, grainX, grainY);
                    continue;
                }
                // Chacune tourne sur elle-même, à son rythme et dans son sens.
                double spin = time * (0.05 + 0.04 * ((index * 7) % 5)) * (index % 2 == 0 ? 1 : -1) + index;
                shapes[index].draw(g, x, y, states[index] != null ? size * GATHERED_SHARE : size, spin);
            }
            drawNames(g, xs, ys, unit, w, h);
            g.setGlobalAlpha(1);
        }
        drawLegend(g);

        g.setFill(TEXT);
        g.setFont(Font.font(11));
        g.setTextAlign(TextAlignment.LEFT);
        double grown = magnification();
        g.fillText(following ? "Tout l'espace" : "Grossi " + (grown >= 10 ? String.valueOf(Math.round(grown))
                : ElementText.number(Math.round(grown * 10) / 10.0)) + " fois", 10, h - 10);
    }

    /**
     * Un grain : un petit losange plein, de travers. C'est ce que devient une molécule dans un bloc
     * d'assemblage, là où un amas montre ses atomes ronds.
     *
     * @param xs deux tableaux de quatre nombres, pour ne rien allouer
     */
    static void grain(GraphicsContext g, Color color, double x, double y, double size, double angle, double[] xs, double[] ys) {
        double cos = Math.cos(angle) * size;
        double sin = Math.sin(angle) * size;
        // Un losange étroit : sa petite diagonale fait les six dixièmes de la grande.
        xs[0] = x + cos;
        ys[0] = y + sin;
        xs[1] = x - 0.6 * sin;
        ys[1] = y + 0.6 * cos;
        xs[2] = x - cos;
        ys[2] = y - sin;
        xs[3] = x + 0.6 * sin;
        ys[3] = y - 0.6 * cos;
        g.setFill(color);
        g.fillPolygon(xs, ys, 4);
    }

    /**
     * Écrit le nom de chaque bloc d'assemblage en son milieu, dès qu'il est assez large à l'écran :
     * sur une pastille de sa couleur, en clair ou en sombre selon ce qui s'y lit le mieux.
     */
    private void drawNames(GraphicsContext g, double[] xs, double[] ys, double unit, double w, double h) {
        g.setTextAlign(TextAlignment.CENTER);
        for (int cluster = 0; cluster < clusters.length; cluster++) {
            Assembly block = blocks[cluster];
            int size = clusters[cluster].length;
            if (block == null || size == 0) continue;
            // Un bloc de n places fait à peu près 2,4 × √n rayons de place de large.
            double across = 2.4 * Math.sqrt(size) * unit;
            if (across < NAME_FROM) continue;
            double x = 0;
            double y = 0;
            for (int cell : clusters[cluster]) {
                x += xs[cell];
                y += ys[cell];
            }
            x /= size;
            y /= size;
            double font = Math.max(11, Math.min(22, across / (0.62 * block.name().length() + 4)));
            double wide = 0.62 * font * block.name().length() + font;
            if (x + wide / 2 < 0 || x - wide / 2 > w || y + font < 0 || y - font > h) continue;
            Color tint = Color.web(block.tint());
            boolean pale = 0.299 * tint.getRed() + 0.587 * tint.getGreen() + 0.114 * tint.getBlue() > 0.6;
            g.setFill(tint);
            g.fillRoundRect(x - wide / 2, y - 0.8 * font, wide, 1.6 * font, 1.6 * font, 1.6 * font);
            g.setFill(pale ? VOID : BLOCK_EDGE);
            g.setFont(Font.font(font));
            g.fillText(block.name(), x, y + 0.35 * font);
        }
        g.setTextAlign(TextAlignment.LEFT);
    }

    /**
     * En haut à gauche : le nombre d'assemblages formés, puis les états qui ont des molécules
     * rassemblées hors des assemblages et combien, « Liquides : 5 », avec la couleur de leur liseré.
     * Rien tant qu'il n'y a ni l'un ni l'autre.
     */
    private void drawLegend(GraphicsContext g) {
        int formed = blocks();
        int lines = formed > 0 ? 1 : 0;
        for (int count : gathered) {
            if (count > 0) lines++;
        }
        if (lines == 0) return;
        // Un fond sombre : de près, une molécule peut passer dessous.
        g.setFill(VOID);
        g.setGlobalAlpha(0.72);
        g.fillRoundRect(4, 4, 130, 8 + 16 * lines, 8, 8);
        g.setGlobalAlpha(1);
        g.setFont(Font.font(11));
        g.setTextAlign(TextAlignment.LEFT);
        double y = 18;
        if (formed > 0) {
            grain(g, BLOCK_EDGE, 14, y - 4, 5, 0.6, grainX, grainY);
            g.setFill(TEXT);
            g.fillText("Assemblages : " + formed, 24, y);
            y += 16;
        }
        for (int place = 0; place < PLACES.length; place++) {
            if (gathered[place] == 0) continue;
            g.setFill(placeColor(PLACES[place]));
            g.fillOval(10, y - 8, 8, 8);
            g.setFill(TEXT);
            g.fillText(BigBangPage.placeTitle(PLACES[place]) + " : " + gathered[place], 24, y);
            y += 16;
        }
    }

    /** Un fond d'étoiles fixes, les mêmes d'une image à l'autre, qui scintillent à peine. */
    private void drawStars(GraphicsContext g, double w, double h) {
        g.setFill(STAR);
        for (int star = 0; star < STARS; star++) {
            // Une suite pseudo-aléatoire fixe : la position d'une étoile ne dépend que de son rang.
            double x = fraction(star * 12.9898) * w;
            double y = fraction(star * 78.233) * h;
            double twinkle = 0.25 + 0.2 * Math.sin(time * (0.4 + fraction(star * 3.7)) + star);
            g.setGlobalAlpha(Math.max(0.05, Math.min(1, twinkle)));
            double size = star % 9 == 0 ? 1.6 : 1;
            g.fillOval(x - size / 2, y - size / 2, size, size);
        }
        g.setGlobalAlpha(1);
    }

    private static double fraction(double seed) {
        double value = Math.sin(seed) * 43758.5453;
        return value - Math.floor(value);
    }
}
