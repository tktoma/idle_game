package idle.ui.bigbang;

import idle.core.Game;
import idle.core.molecules.Molecule;
import idle.core.sky.Assembly;
import idle.core.sky.Body;
import idle.core.sky.Cosmos;
import idle.ui.GameApp;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

/**
 * La vue de l'espace, première partie : ses réglages, son état, le zoom et le cadrage.
 */
public abstract class SpaceViewBase extends Canvas {

    /** La vue entière : chaque partie de la chaîne est une {@link SpaceView}. */
    final SpaceView self() {
        return (SpaceView) this;
    }

    SpaceViewBase(Game game) {
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
            moved = true;
        });
        setOnMouseReleased(event -> dragX = Double.NaN);
    }


    /** Échelle la plus rapprochée, en pixels par unité de longueur : une molécule d'eau y fait une centaine de pixels de large. */
    static final double CLOSEST = 10;
    /** Part de la plus petite dimension de la vue qu'occupe le rayon de l'espace quand on le voit en entier. */
    private static final double FIT_SHARE = 0.44;
    /** Un cran de molette ou un clic sur un bouton multiplie l'échelle par ce nombre. */
    public static final double ZOOM_STEP = 1.5;
    /**
     * Rayon de la place d'une molécule, en parts du rayon du cœur divisé par la racine du nombre de
     * molécules : à 0,8, deux voisines de la spirale se touchent à peine.
     */
    static final double CELL_SHARE = 0.8;
    /** Part de sa place qu'occupe le dessin de la plus petite molécule ; la plus grosse l'occupe toute. */
    static final double SMALLEST = 0.55;
    /** Nombre d'atomes à partir duquel un dessin occupe toute sa place. */
    static final int BIGGEST_ATOMS = 24;
    /** L'angle d'or, en radians : d'une molécule à la suivante sur la spirale. */
    static final double GOLDEN_ANGLE = Math.PI * (3 - Math.sqrt(5));
    /** Vitesse de rotation de l'ensemble, en radians par seconde : un tour en dix minutes environ. */
    static final double DRIFT = 0.01;
    static final int STARS = 90;

    static final Color VOID = Color.web("#05060a");
    static final Color SPACE = Color.web("#0e1320");
    static final Color EDGE = Color.web(GameApp.BIG_BANG_COLOR);
    static final Color CORE = Color.web("#3b4a66");
    static final Color STAR = Color.web("#c9d6ea");
    static final Color TEXT = Color.web("#8fa3b8");
    /** Le contour d'un bloc d'assemblage : clair, pour le détacher des amas, dont le liseré a la couleur de leur état. */
    static final Color BLOCK_EDGE = Color.web("#fff6dc");
    /** Épaisseur du contour d'un bloc, en rayons de place : le double du liseré d'un amas. */
    static final double BLOCK_RIM = 0.22;
    /** Taille du grain qu'une molécule devient dans un bloc, en rayons de place. */
    static final double GRAIN_SHARE = 0.5;
    /** Taille d'une place à l'écran, en pixels, à partir de laquelle un bloc laisse revoir ses molécules à la place des grains. */
    static final double GRAINS_BELOW = 26;
    /** Largeur d'un bloc à l'écran, en pixels, à partir de laquelle son nom est écrit dessus. */
    static final double NAME_FROM = 70;
    /** Les états de la matière, du plus serré au plus lâche : l'ordre dans lequel leurs amas prennent place depuis le centre. */
    static final Molecule.State[] PLACES = {Molecule.State.METAL, Molecule.State.CRYSTAL, Molecule.State.SOLID,
            Molecule.State.LIQUID, Molecule.State.GAS};
    /** Part de la couleur d'un état dans le fond d'un amas, avant que la couleur de la molécule s'y ajoute. */
    static final double PLACE_TINT = 0.16;
    /** Épaisseur du liseré qui cerne un amas de la couleur de son état, en rayons de place. */
    static final double RIM = 0.1;
    /** Part de sa place qu'occupe le dessin d'une molécule rassemblée : le reste laisse voir son amas. */
    static final double GATHERED_SHARE = 0.8;
    /**
     * Rayon le plus grand du rond qu'une molécule rassemblée apporte à l'amas de sa sorte, en
     * rayons de place : à 1,25, les ronds de deux voisines de la même sorte se recouvrent et
     * l'amas est d'un seul tenant. Un rond est plus petit dès qu'il rencontre un autre amas, une
     * molécule isolée ou le bord du cœur ({@link #collide}).
     */
    static final double BLOB_SHARE = 1.25;
    /** Demi-largeur la plus grande d'un pont entre deux molécules d'un même amas, en rayons de place. */
    static final double BRIDGE_SHARE = 1;
    /** En dessous de cette demi-largeur, en rayons de place, un pont coincé entre deux autres amas n'est pas dessiné. */
    static final double THINNEST_BRIDGE = 0.2;
    /**
     * Ce qui garde un amas ramassé quand il choisit ses places : une place compte d'autant moins
     * qu'elle est loin de la première de l'amas, à raison de cette part de la distance. À 0, l'amas
     * s'étirerait en ruban ; trop haut, il laisserait des places isolées derrière lui.
     */
    static final double COMPACT = 0.25;
    /**
     * Distance en deçà de laquelle deux places de la spirale sont voisines, en rayons de place, avant
     * le resserrement qui garde tout dans le cœur : deux voisines sont à 2,4 rayons l'une de l'autre.
     */
    static final double NEIGHBOUR = 3.1;
    /**
     * Distance en deçà de laquelle deux molécules d'un même amas sont soudées par un pont, dans la
     * même unité : assez pour relier aussi les deux coins opposés de quatre places en carré, ce qui
     * ne laisse aucun trou au milieu.
     */
    static final double JOIN = 3.6;
    /** Taille d'une place à l'écran, en pixels, en dessous de laquelle la matière commence à se fondre en gaz. */
    static final double MIST_FROM = 5;
    /** Taille d'une place à l'écran, en pixels, en dessous de laquelle il ne reste que du gaz : plus aucun point. */
    static final double MIST_FULL = 3;
    /** Rayon de la bouffée qu'une molécule apporte au gaz, en rayons de place : assez pour recouvrir ses voisines. */
    static final double PUFF_SHARE = 2.4;
    /** Ce qui reste de la couleur d'une bouffée une fois posée : en dessous de 1, les bouffées voisines se mêlent. */
    static final double PUFF_ALPHA = 0.45;
    /** Flou du gaz, en rayons de place, borné à ce que sait faire {@link GaussianBlur} (63 pixels). */
    static final double BLUR_SHARE = 3.5;
    /** Flou le plus faible du gaz, en pixels : même de très loin, les bouffées restent fondues. */
    static final double LEAST_BLUR = 4;
    /** Jusqu'où une bouffée s'écarte de sa place en remuant, en rayons de place. */
    static final double CHURN = 0.7;
    /** Écart, en rayons de place, au-delà duquel deux ronds ne peuvent pas se toucher : deux fois le plus grand rond. */
    static final double FAR = 2 * BLOB_SHARE;
    /** Marge des comparaisons faites sur des carrés avant une mesure exacte : bien plus que les arrondis d'un calcul. */
    static final double SURELY = 1 + 1e-9;
    /** Ce qu'un point de la galaxie prend de la couleur des étoiles, dorée au bulbe et bleutée dans les bras : le reste est la couleur de sa molécule. */
    static final double TONE_SHARE = 0.5;
    static final Color BULGE_TONE = Color.web("#ffe6b0");
    static final Color ARM_TONE = Color.web("#d6e4ff");
    /** Nombre d'anneaux de couleur, du centre au bord. */
    static final int TONES = 12;
    /** Dans la galaxie, une bouffée de gaz est plus petite et plus pâle : les bras, plus denses, en sortent plus clairs que les couloirs. */
    static final double SWIRL_PUFF = 1.3;
    static final double SWIRL_ALPHA = 0.42;
    /** Le plus gros point qu'une molécule devient dans la galaxie, en rayons de place. */
    static final double DOT_SHARE = 0.55;
    /** Taille d'un point à l'écran, en pixels, à partir de laquelle la molécule y est redessinée. */
    static final double DRAWN_FROM = 7;
    /** La galaxie tourne plus vite que la matière ne dérivait : un tour en deux minutes et demie environ. */
    static final double GALAXY_SPIN = 4;
    /** La lueur de la galaxie : vive et chaude au bulbe, pâle et bleue dans les bras, éteinte au bord. */
    static final javafx.scene.paint.Stop[] GALAXY_GLOW = {
            new javafx.scene.paint.Stop(0, Color.web("#fff4d8", 0.85)), new javafx.scene.paint.Stop(0.06, Color.web("#ffe2a8", 0.6)),
            new javafx.scene.paint.Stop(0.2, Color.web("#ffcf90", 0.26)), new javafx.scene.paint.Stop(0.55, Color.web("#8fa8ff", 0.1)),
            new javafx.scene.paint.Stop(1, Color.web("#8fa8ff", 0))};

    /**
     * Part du rayon de sa clairière qu'un astre garde pour son dessin : le reste est la couronne où
     * se rangent les blocs de ses assemblages.
     */
    static final double HEART = 0.7;
    /** Part des places d'un astre que tient sa clairière quand rien ne la gêne : un peu moins que toutes, pour qu'aucune étrangère n'y reste. */
    static final double CLEARING = 0.96;
    /** Ce dont les clairières rétrécissent toutes ensemble, à chaque essai, tant qu'elles ne tiennent pas dans le cœur. */
    static final double TIGHTER = 0.94;
    /** Directions essayées autour d'un astre déjà posé pour y accoler le suivant. */
    static final int BEARINGS = 72;
    /** Vide laissé entre le dessin d'un astre et ce qui l'entoure, en rayons de place. */
    static final double BODY_GAP = 0.2;
    /** Rayon d'un astre à l'écran, en pixels, en dessous duquel il n'est plus qu'un point de sa couleur. */
    static final double DOT_BELOW = 2.5;
    /** Largeur d'un astre à l'écran, en pixels, à partir de laquelle ses assemblages sont écrits sous son nom. */
    static final double MADE_FROM = 240;

    final Game game;
    /** Pixels par unité de longueur ; {@code NaN} avant la première image. */
    double scale = Double.NaN;
    /** Vrai tant que la vue suit l'expansion : elle montre tout l'espace, quel que soit sa taille. */
    boolean following = true;
    /** Le point de l'espace au centre de la vue. */
    double centerX = 0;
    double centerY = 0;
    double time = 0;
    private double dragX = Double.NaN;
    private double dragY = Double.NaN;

    /**
     * Les molécules placées : pour chacune, sa distance au centre (en parts du rayon du cœur occupé),
     * son angle, et le rayon de son dessin (en parts de sa place).
     */
    double[] distances = new double[0];
    double[] angles = new double[0];
    double[] radii = new double[0];
    MoleculeArt.Shape[] shapes = new MoleculeArt.Shape[0];
    /** Pour chaque place : l'état de la molécule rassemblée qui l'occupe, ou {@code null} si elle ne l'est pas. */
    Molecule.State[] states = new Molecule.State[0];
    /** Pour chaque état de {@link #PLACES} : le nombre de molécules rassemblées dans cet état. */
    final long[] gathered = new long[PLACES.length];
    /**
     * Les corps, du centre vers le bord : d'abord un bloc par assemblage formé, puis un amas par sorte
     * de molécule rassemblée. Pour chacun, les places de ses molécules.
     */
    int[][] clusters = new int[0][];
    /** Pour chaque corps : son assemblage si c'est un bloc, {@code null} si c'est un amas. */
    Assembly[] blocks = new Assembly[0];
    /** Pour chaque place : vrai si sa molécule est dans un bloc d'assemblage. */
    boolean[] assembled = new boolean[0];
    /** Pour chaque place : vrai si sa molécule est dans un astre. Elle n'est pas dessinée : l'astre l'est. */
    boolean[] hidden = new boolean[0];
    /**
     * Les astres formés, du plus gros au plus petit : ce sont les premiers corps de {@link #clusters}.
     * Pour chacun : le centre de sa clairière (distance au centre en rayons de place, et angle), le
     * rayon de cette clairière, et celui du cercle où tient son dessin une fois ses voisins mesurés.
     */
    Body[] sky = new Body[0];
    double[] skyAway = new double[0];
    double[] skyAngle = new double[0];
    double[] skyRound = new double[0];
    double[] skyRadius = new double[0];
    Color[] skyTints = new Color[0];
    /** Le centre de chaque astre à l'écran, à la dernière image. */
    double[] skyX = new double[0];
    double[] skyY = new double[0];
    /**
     * Ce qui ne change qu'avec la mise en place, calculé une fois plutôt qu'à chaque image : le
     * cosinus et le sinus de l'angle de chaque place, et ceux des deux phases dont sa bouffée de gaz
     * remue ; pour chaque corps, ses deux couleurs, le milieu de ses places et le rayon du cercle qui
     * le contient tout entier, en rayons de place.
     */
    double[] cosines = new double[0];
    double[] sines = new double[0];
    double[] swayCos = new double[0];
    double[] swaySin = new double[0];
    double[] heaveCos = new double[0];
    double[] heaveSin = new double[0];
    Color[] rims = new Color[0];
    Color[] inks = new Color[0];
    double[] middleX = new double[0];
    double[] middleY = new double[0];
    /** Où s'écrit le nom de chaque bloc : son milieu, ou le milieu de son arc pour un bloc rangé autour d'un astre. */
    double[] labelX = new double[0];
    double[] labelY = new double[0];
    double[] rounds = new double[0];
    /** La position de chaque place à l'écran, à la dernière image : gardée d'une image à l'autre pour ne rien allouer. */
    double[] xs = new double[0];
    double[] ys = new double[0];
    /**
     * Une image sur {@link #EVERY} seulement est dessinée tant que rien ne change : la dérive de
     * l'ensemble et la rotation des molécules sont trop lentes pour que cela se voie. Dès que la vue
     * bouge (molette, boutons, souris), que sa taille change ou qu'une molécule s'ajoute, l'image
     * suivante est dessinée sans attendre.
     */
    static final int EVERY = 2;
    int beat = 0;
    boolean moved = true;
    double drawnWidth = -1;
    double drawnHeight = -1;
    /**
     * La galaxie ({@link Game#formGalaxy()}) : pour chaque place, le cosinus et le sinus de l'angle où
     * l'enroulement en bras l'emmène ({@link Spiral}), et le rayon du point qu'elle y devient, en
     * rayons de place : la moitié de l'écart à sa plus proche voisine, pour que deux points ne se
     * recouvrent pas. Pour chaque astre : où il se range le long d'un bras, et le rayon de son dessin.
     */
    double[] swirlCos = new double[0];
    double[] swirlSin = new double[0];
    double[] rooms = new double[0];
    /** La couleur de chaque point de la galaxie : celle de sa molécule, tirée vers le blanc doré au centre et le blanc bleuté dans les bras. */
    Color[] swirlTints = new Color[0];
    double[] swirlAway = new double[0];
    double[] swirlAngle = new double[0];
    double[] swirlRadius = new double[0];
    /** Vrai tant que le joueur veut voir la galaxie une fois formée ; faux s'il a demandé à revoir la matière telle quelle. */
    boolean asGalaxy = true;
    /** Vrai si la dernière image montrait la galaxie. */
    boolean swirled = false;
    /** Le rayon du dessin de chaque astre dans la dernière image : celui de sa clairière, ou celui qu'il a dans la galaxie. */
    double[] shown = new double[0];
    /** Les cadres des noms d'astres écrits pendant l'image en cours. */
    double[] labels = new double[0];
    /** Ce dont les clairières ont dû rétrécir pour tenir toutes dans le cœur : 1 quand elles ont toute leur place. */
    double tightened = 1;
    /** Pour chaque place d'un bloc d'assemblage : la couleur de sa matière ; {@code null} ailleurs. De loin, c'est celle de sa bouffée de gaz. */
    Color[] tints = new Color[0];
    /** Les quatre pointes du grain en cours de dessin, gardées d'une image à l'autre pour ne rien allouer. */
    final double[] grainX = new double[4];
    final double[] grainY = new double[4];
    /**
     * Les ponts de chaque amas : des paires de places (début, fin) de deux de ses molécules. Toutes
     * les voisines y sont reliées deux à deux ({@link #JOIN}), plus de quoi rattacher une molécule
     * restée à l'écart : avec eux, les ronds d'un amas forment une seule forme.
     */
    int[][] bridges = new int[0][];
    /**
     * Les triangles pleins de chaque amas : des triplets de places reliées deux à deux par des
     * ponts. Ils bouchent le creux qui resterait entre trois ronds voisins.
     */
    int[][] fills = new int[0][];
    /** La demi-largeur de chaque pont, en rayons de place, dans le même ordre : 0 quand il n'a pas la place de passer. */
    double[][] widths = new double[0][];
    /** Pour chaque place : le rayon du rond de sa molécule dans son amas, en rayons de place ; 0 si elle n'est pas rassemblée. */
    double[] blobs = new double[0];
    /** Le plus grand chevauchement entre deux ronds d'amas différents, en rayons de place : nul ou négatif si rien ne se chevauche. */
    double overlap = 0;
    /** Nombre de paires de ronds d'amas différents qui se touchent exactement. */
    int contacts = 0;
    /** Les trois pointes du triangle en cours de dessin, gardées d'une image à l'autre pour ne rien allouer. */
    final double[] cornerX = new double[3];
    final double[] cornerY = new double[3];
    /** Le flou du gaz, gardé d'une image à l'autre : seul son rayon change. */
    final GaussianBlur blur = new GaussianBlur();
    /** Part de gaz dans la dernière image : 0 de près, 1 quand il ne reste que du gaz. */
    double mist = 0;
    int placedVersion = -1;
    /** Distance au centre du bord du dessin le plus éloigné, à la dernière image, en unités de longueur. */
    double reach = 0;

    /** Rayon de l'espace créé, en unités de longueur : le disque dont la surface vaut l'espace. */
    double spaceRadius() {
        // Borné : au-delà, ni l'écran ni un double n'y gagnent rien.
        double space = Math.min(game.state().space().toDouble(), 1e30);
        return Math.sqrt(Math.max(space, 0) / Math.PI);
    }

    /**
     * Rayon du cœur utilisé, en unités de longueur : le disque dont la surface vaut l'espace utilisé,
     * par les molécules et leurs lieux de rassemblement. Jamais plus grand que l'espace lui-même,
     * même si les outils de test y ont mis plus de molécules qu'il n'en contient.
     */
    double coreRadius() {
        double occupied = Math.min(game.usedSpace().toDouble(), 1e30);
        return Math.min(spaceRadius(), Math.sqrt(Math.max(occupied, 0) / Math.PI));
    }

    /** L'échelle à laquelle tout l'espace tient dans la vue. Jamais plus rapprochée que {@link #CLOSEST}. */
    double fitScale() {
        double radius = spaceRadius();
        double room = FIT_SHARE * Math.min(getWidth(), getHeight());
        return radius <= 0 || room <= 0 ? CLOSEST : Math.min(CLOSEST, room / radius);
    }

    /**
     * Rapproche ({@code factor} > 1) ou éloigne ({@code factor} < 1) la vue. Elle ne recule jamais
     * plus loin que ce qu'il faut pour voir tout l'espace, ni n'avance plus près que
     * {@link #CLOSEST}. Revenue à la vue d'ensemble, elle se remet à suivre l'expansion.
     */
    public void zoom(double factor) {
        // De si loin, il n'y a ni à s'approcher ni à reculer : les boutons de vue changent d'échelle.
        if (cosmosShown() != null) return;
        if (!(factor > 0) || Double.isNaN(scale)) return;
        double fit = fitScale();
        scale = Math.max(fit, Math.min(CLOSEST, scale * factor));
        moved = true;
        following = scale <= fit * 1.0001;
        if (following) {
            centerX = 0;
            centerY = 0;
        }
    }

    /** Revient à la vue d'ensemble : tout l'espace, centré, et la vue suit de nouveau l'expansion. */
    public void fit() {
        moved = true;
        following = true;
        centerX = 0;
        centerY = 0;
        scale = fitScale();
    }

    /** Vrai si la vue ne peut pas reculer davantage : tout l'espace est déjà visible. */
    public boolean isFitted() {
        return following;
    }

    /** Vrai si la vue ne peut pas s'approcher davantage. */
    public boolean isClosest() {
        return !Double.isNaN(scale) && scale >= CLOSEST * 0.9999;
    }

    /** Le grossissement par rapport à la vue d'ensemble : 1 quand tout l'espace est visible. */
    public double magnification() {
        return Double.isNaN(scale) ? 1 : scale / fitScale();
    }

    /** Jusqu'où va le dessin le plus éloigné du centre, à la dernière image, en parts du rayon de l'espace : pour les vérifications. */
    public double reachShare() {
        double radius = spaceRadius();
        return radius <= 0 ? 0 : reach / radius;
    }

    /**
     * Part de gaz dans la dernière image : 0 tant qu'une place fait au moins {@link #MIST_FROM}
     * pixels, 1 quand elle en fait moins de {@link #MIST_FULL} et qu'il ne reste que des nuages.
     */
    public double mist() {
        return mist;
    }

    /** Nombre de points placés dans la vue, un par molécule tant qu'il y en a moins de {@link #MOST_DOTS} : pour les vérifications. */
    public int placed() {
        return shapes.length;
    }

    /**
     * Le plus grand nombre de points que la vue place. Un amas attire la matière par centaines : au-delà
     * de ce nombre de molécules, un point en vaut plusieurs ({@link #perDot()}), et la vue ne pèse jamais plus.
     */
    public static final int MOST_DOTS = 30_000;

    /** Nombre de molécules que vaut un point de la vue : 1 tant qu'il y en a moins de {@link #MOST_DOTS}. */
    int perDot = 1;

    /** Nombre de molécules que vaut un point de la vue, à la dernière image. */
    public int perDot() {
        return perDot;
    }

    /** Nombre de points qui montrent {@code molecules} molécules : autant, ou moins quand un point en vaut plusieurs. */
    int dots(int molecules) {
        return perDot == 1 ? molecules : (molecules + perDot - 1) / perDot;
    }

    /** Nombre de molécules rassemblées dans cet état, à la dernière image : pour les vérifications. */
    public long gathered(Molecule.State place) {
        for (int index = 0; index < PLACES.length; index++) {
            if (PLACES[index] == place) return gathered[index];
        }
        return 0;
    }

    /** Nombre de ponts dessinés entre molécules rassemblées d'une même sorte : pour les vérifications. */
    public int bridges() {
        int total = 0;
        for (double[] each : widths) {
            for (double width : each) {
                if (width > 0) total++;
            }
        }
        return total;
    }

    /** Nombre de triangles pleins entre molécules voisines d'un même amas : pour les vérifications. */
    public int fills() {
        int total = 0;
        for (int[] each : fills) total += each.length / 3;
        return total;
    }

    /** Nombre de ponts qui n'ont pas la place de passer entre deux autres amas : pour les vérifications. */
    public int blockedBridges() {
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
    public double overlap() {
        return overlap;
    }

    /** Nombre de paires de ronds d'amas différents qui se touchent exactement : pour les vérifications. */
    public int contacts() {
        return contacts;
    }

    /** Nombre de corps, blocs d'assemblage et amas confondus : pour les vérifications. */
    public int clusters() {
        return clusters.length;
    }

    /** Nombre de blocs d'assemblage placés dans la vue : pour les vérifications. */
    public int blocks() {
        int total = 0;
        for (Assembly block : blocks) {
            if (block != null) total++;
        }
        return total;
    }

    /**
     * Choisit ce que montre la vue une fois la galaxie formée : la galaxie, ou la matière telle
     * qu'elle était rangée avant. Sans galaxie, cela ne change rien.
     */
    void showGalaxy(boolean galaxy) {
        asGalaxy = galaxy;
        moved = true;
    }

    /**
     * L'échelle au-dessus de la galaxie que la vue montre : l'amas de galaxies, l'univers, ou
     * {@code null} pour l'espace lui-même. Elle n'est montrée qu'une fois formée.
     */
    private Cosmos far = null;
    /** Les couleurs des points d'une galaxie vue de loin, celles des molécules du joueur, et la liste de molécules dont elles viennent. */
    private final Color[] farColors = new Color[GalaxyView.DOTS];
    private int farColored = -1;
    boolean farDrawn = false;

    /**
     * Recule la vue au-delà de la galaxie : {@link Cosmos#CLUSTER} montre la galaxie du
     * joueur au milieu des autres, {@link Cosmos#UNIVERSE} la toile de l'univers. {@code null},
     * ou la galaxie, ramène à l'espace. Une échelle qui n'est pas formée ne change rien à la vue.
     */
    void showCosmos(Cosmos scale) {
        far = scale == Cosmos.GALAXY ? null : scale;
        moved = true;
    }

    /** L'échelle au-dessus de la galaxie que la vue montre en ce moment, ou {@code null} si elle montre l'espace. */
    public Cosmos cosmosShown() {
        return far != null && game.hasCosmos(far) ? far : null;
    }

    /** Dessine l'amas de galaxies ou l'univers, à la place de l'espace, avec en haut à gauche ce que l'on regarde. */
    void drawFar(GraphicsContext g, double w, double h, Cosmos scale) {
        g.clearRect(0, 0, w, h);
        g.setGlobalAlpha(1);
        g.setFill(VOID);
        g.fillRect(0, 0, w, h);
        if (scale == Cosmos.UNIVERSE) {
            GalaxyView.universe(g, w, h);
        } else {
            if (farColored != game.state().moleculesVersion()) {
                farColored = game.state().moleculesVersion();
                GalaxyView.tint(game, farColors);
            }
            GalaxyView.cluster(g, w, h, time, farColors, game);
        }
        String title = scale == Cosmos.UNIVERSE ? "Univers : votre amas de galaxies, entouré, parmi tous les autres"
                : "Amas de galaxies : la vôtre au centre, et huit autres";
        g.setGlobalAlpha(0.72);
        g.setFill(VOID);
        g.fillRoundRect(4, 4, 14 + title.length() * 5.6, 22, 8, 8);
        g.setGlobalAlpha(1);
        g.setFill(TEXT);
        g.setFont(Font.font(11));
        g.setTextAlign(TextAlignment.LEFT);
        g.fillText(title, 10, 19);
    }

    /** Vrai si la vue montre la galaxie, ou la montrera dès qu'elle sera formée. */
    public boolean showsGalaxy() {
        return asGalaxy;
    }

    /** Vrai si la dernière image montrait la galaxie : pour les vérifications. */
    public boolean swirled() {
        return swirled;
    }

    /** Nombre d'astres placés dans la vue : pour les vérifications. */
    public int bodies() {
        return sky.length;
    }

    /** Nombre de molécules que les astres contiennent, et qui ne sont donc pas dessinées : pour les vérifications. */
    public int bodyCells() {
        int total = 0;
        for (boolean each : hidden) {
            if (each) total++;
        }
        return total;
    }

    /** Ce dont les clairières des astres ont rétréci pour tenir dans le cœur, 1 si elles ont toute leur place : pour les vérifications. */
    public double tightened() {
        return tightened;
    }

    /** Le rayon du cercle où tient le dessin d'un astre, en rayons de place : pour les vérifications. */
    public double bodyRadius(int index) {
        return skyRadius[index];
    }

    /** Nombre de molécules placées dans des blocs d'assemblage : pour les vérifications. */
    public int assembledCells() {
        int total = 0;
        for (boolean each : assembled) {
            if (each) total++;
        }
        return total;
    }

    /** Installe une molécule à une place de la spirale, dans le lieu de {@code state} ou hors de tout lieu ({@code null}). */
    /** Places restées vides à la dernière mise en place, reprises par une sorte possédée : pour les vérifications. */
    int refilled;
    double sincePlaced = SpaceView.CALM_SECONDS;
    boolean calm = false;
}
