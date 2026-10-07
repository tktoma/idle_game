package idle.ui;

import idle.core.Assembly;
import idle.core.Body;
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
 * <p>Et les astres formés ({@link Game#formBody(String)}), qui sont faits de leurs assemblages.
 * Chacun a une clairière ronde, de la taille de ce qu'il contient : l'astre lui-même
 * ({@link BodyArt}) en occupe le milieu, et les blocs de ses assemblages restent autour de lui, en
 * couronne, chacun avec sa couleur, son contour, ses grains et son nom. Une planète est un globe
 * au milieu de ses roches, de ses eaux et de ses airs ; une étoile, une boule de feu dans son
 * hydrogène. Le trou noir supermassif tient le centre de tout, les plus gros astres autour de
 * lui, et aucun ne touche ni un autre astre, ni un bloc, ni un amas. Un astre reste net de très
 * loin, quand tout le reste est devenu du gaz.
 *
 * <p>Enfin la galaxie ({@link Game#formGalaxy()}) : une fois formée, tout cela s'enroule en bras de
 * spirale ({@link Spiral}) autour du trou noir supermassif. Chaque molécule y devient un point, de
 * sa couleur mêlée à celle des étoiles ; les astres se rangent le long des bras, les plus gros
 * près du centre ; l'ensemble tourne, ses bras à la traîne. Le joueur peut toujours revenir à la
 * matière telle qu'elle était rangée ({@link #showGalaxy(boolean)}).
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
    /** Marge des comparaisons faites sur des carrés avant une mesure exacte : bien plus que les arrondis d'un calcul. */
    private static final double SURELY = 1 + 1e-9;
    /** Ce qu'un point de la galaxie prend de la couleur des étoiles, dorée au bulbe et bleutée dans les bras : le reste est la couleur de sa molécule. */
    private static final double TONE_SHARE = 0.5;
    private static final Color BULGE_TONE = Color.web("#ffe6b0");
    private static final Color ARM_TONE = Color.web("#d6e4ff");
    /** Nombre d'anneaux de couleur, du centre au bord. */
    private static final int TONES = 12;
    /** Dans la galaxie, une bouffée de gaz est plus petite et plus pâle : les bras, plus denses, en sortent plus clairs que les couloirs. */
    private static final double SWIRL_PUFF = 1.3;
    private static final double SWIRL_ALPHA = 0.42;
    /** Le plus gros point qu'une molécule devient dans la galaxie, en rayons de place. */
    private static final double DOT_SHARE = 0.55;
    /** Taille d'un point à l'écran, en pixels, à partir de laquelle la molécule y est redessinée. */
    private static final double DRAWN_FROM = 7;
    /** La galaxie tourne plus vite que la matière ne dérivait : un tour en deux minutes et demie environ. */
    private static final double GALAXY_SPIN = 4;
    /** La lueur de la galaxie : vive et chaude au bulbe, pâle et bleue dans les bras, éteinte au bord. */
    private static final javafx.scene.paint.Stop[] GALAXY_GLOW = {
            new javafx.scene.paint.Stop(0, Color.web("#fff4d8", 0.85)), new javafx.scene.paint.Stop(0.06, Color.web("#ffe2a8", 0.6)),
            new javafx.scene.paint.Stop(0.2, Color.web("#ffcf90", 0.26)), new javafx.scene.paint.Stop(0.55, Color.web("#8fa8ff", 0.1)),
            new javafx.scene.paint.Stop(1, Color.web("#8fa8ff", 0))};

    /**
     * Part du rayon de sa clairière qu'un astre garde pour son dessin : le reste est la couronne où
     * se rangent les blocs de ses assemblages.
     */
    private static final double HEART = 0.7;
    /** Part des places d'un astre que tient sa clairière quand rien ne la gêne : un peu moins que toutes, pour qu'aucune étrangère n'y reste. */
    private static final double CLEARING = 0.96;
    /** Ce dont les clairières rétrécissent toutes ensemble, à chaque essai, tant qu'elles ne tiennent pas dans le cœur. */
    private static final double TIGHTER = 0.94;
    /** Directions essayées autour d'un astre déjà posé pour y accoler le suivant. */
    private static final int BEARINGS = 72;
    /** Vide laissé entre le dessin d'un astre et ce qui l'entoure, en rayons de place. */
    private static final double BODY_GAP = 0.2;
    /** Rayon d'un astre à l'écran, en pixels, en dessous duquel il n'est plus qu'un point de sa couleur. */
    private static final double DOT_BELOW = 2.5;
    /** Largeur d'un astre à l'écran, en pixels, à partir de laquelle ses assemblages sont écrits sous son nom. */
    private static final double MADE_FROM = 240;

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
    /** Pour chaque place : vrai si sa molécule est dans un astre. Elle n'est pas dessinée : l'astre l'est. */
    private boolean[] hidden = new boolean[0];
    /**
     * Les astres formés, du plus gros au plus petit : ce sont les premiers corps de {@link #clusters}.
     * Pour chacun : le centre de sa clairière (distance au centre en rayons de place, et angle), le
     * rayon de cette clairière, et celui du cercle où tient son dessin une fois ses voisins mesurés.
     */
    private Body[] sky = new Body[0];
    private double[] skyAway = new double[0];
    private double[] skyAngle = new double[0];
    private double[] skyRound = new double[0];
    private double[] skyRadius = new double[0];
    private Color[] skyTints = new Color[0];
    /** Le centre de chaque astre à l'écran, à la dernière image. */
    private double[] skyX = new double[0];
    private double[] skyY = new double[0];
    /**
     * Ce qui ne change qu'avec la mise en place, calculé une fois plutôt qu'à chaque image : le
     * cosinus et le sinus de l'angle de chaque place, et ceux des deux phases dont sa bouffée de gaz
     * remue ; pour chaque corps, ses deux couleurs, le milieu de ses places et le rayon du cercle qui
     * le contient tout entier, en rayons de place.
     */
    private double[] cosines = new double[0];
    private double[] sines = new double[0];
    private double[] swayCos = new double[0];
    private double[] swaySin = new double[0];
    private double[] heaveCos = new double[0];
    private double[] heaveSin = new double[0];
    private Color[] rims = new Color[0];
    private Color[] inks = new Color[0];
    private double[] middleX = new double[0];
    private double[] middleY = new double[0];
    /** Où s'écrit le nom de chaque bloc : son milieu, ou le milieu de son arc pour un bloc rangé autour d'un astre. */
    private double[] labelX = new double[0];
    private double[] labelY = new double[0];
    private double[] rounds = new double[0];
    /** La position de chaque place à l'écran, à la dernière image : gardée d'une image à l'autre pour ne rien allouer. */
    private double[] xs = new double[0];
    private double[] ys = new double[0];
    /**
     * Une image sur {@link #EVERY} seulement est dessinée tant que rien ne change : la dérive de
     * l'ensemble et la rotation des molécules sont trop lentes pour que cela se voie. Dès que la vue
     * bouge (molette, boutons, souris), que sa taille change ou qu'une molécule s'ajoute, l'image
     * suivante est dessinée sans attendre.
     */
    private static final int EVERY = 2;
    private int beat = 0;
    private boolean moved = true;
    private double drawnWidth = -1;
    private double drawnHeight = -1;
    /**
     * La galaxie ({@link Game#formGalaxy()}) : pour chaque place, le cosinus et le sinus de l'angle où
     * l'enroulement en bras l'emmène ({@link Spiral}), et le rayon du point qu'elle y devient, en
     * rayons de place : la moitié de l'écart à sa plus proche voisine, pour que deux points ne se
     * recouvrent pas. Pour chaque astre : où il se range le long d'un bras, et le rayon de son dessin.
     */
    private double[] swirlCos = new double[0];
    private double[] swirlSin = new double[0];
    private double[] rooms = new double[0];
    /** La couleur de chaque point de la galaxie : celle de sa molécule, tirée vers le blanc doré au centre et le blanc bleuté dans les bras. */
    private Color[] swirlTints = new Color[0];
    private double[] swirlAway = new double[0];
    private double[] swirlAngle = new double[0];
    private double[] swirlRadius = new double[0];
    /** Vrai tant que le joueur veut voir la galaxie une fois formée ; faux s'il a demandé à revoir la matière telle quelle. */
    private boolean asGalaxy = true;
    /** Vrai si la dernière image montrait la galaxie. */
    private boolean swirled = false;
    /** Le rayon du dessin de chaque astre dans la dernière image : celui de sa clairière, ou celui qu'il a dans la galaxie. */
    private double[] shown = new double[0];
    /** Les cadres des noms d'astres écrits pendant l'image en cours. */
    private double[] labels = new double[0];
    /** Ce dont les clairières ont dû rétrécir pour tenir toutes dans le cœur : 1 quand elles ont toute leur place. */
    private double tightened = 1;
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
            moved = true;
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
    void fit() {
        moved = true;
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

    /** Nombre de points placés dans la vue, un par molécule tant qu'il y en a moins de {@link #MOST_DOTS} : pour les vérifications. */
    int placed() {
        return shapes.length;
    }

    /**
     * Le plus grand nombre de points que la vue place. Un amas attire la matière par centaines : au-delà
     * de ce nombre de molécules, un point en vaut plusieurs ({@link #perDot()}), et la vue ne pèse jamais plus.
     */
    static final int MOST_DOTS = 30_000;

    /** Nombre de molécules que vaut un point de la vue : 1 tant qu'il y en a moins de {@link #MOST_DOTS}. */
    private int perDot = 1;

    /** Nombre de molécules que vaut un point de la vue, à la dernière image. */
    int perDot() {
        return perDot;
    }

    /** Nombre de points qui montrent {@code molecules} molécules : autant, ou moins quand un point en vaut plusieurs. */
    private int dots(int molecules) {
        return perDot == 1 ? molecules : (molecules + perDot - 1) / perDot;
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
    private idle.core.Cosmos far = null;
    /** Les couleurs des points d'une galaxie vue de loin, celles des molécules du joueur, et la liste de molécules dont elles viennent. */
    private final Color[] farColors = new Color[GalaxyView.DOTS];
    private int farColored = -1;
    private boolean farDrawn = false;

    /**
     * Recule la vue au-delà de la galaxie : {@link idle.core.Cosmos#CLUSTER} montre la galaxie du
     * joueur au milieu des autres, {@link idle.core.Cosmos#UNIVERSE} la toile de l'univers. {@code null},
     * ou la galaxie, ramène à l'espace. Une échelle qui n'est pas formée ne change rien à la vue.
     */
    void showCosmos(idle.core.Cosmos scale) {
        far = scale == idle.core.Cosmos.GALAXY ? null : scale;
        moved = true;
    }

    /** L'échelle au-dessus de la galaxie que la vue montre en ce moment, ou {@code null} si elle montre l'espace. */
    idle.core.Cosmos cosmosShown() {
        return far != null && game.hasCosmos(far) ? far : null;
    }

    /** Dessine l'amas de galaxies ou l'univers, à la place de l'espace, avec en haut à gauche ce que l'on regarde. */
    private void drawFar(GraphicsContext g, double w, double h, idle.core.Cosmos scale) {
        g.clearRect(0, 0, w, h);
        g.setGlobalAlpha(1);
        g.setFill(VOID);
        g.fillRect(0, 0, w, h);
        if (scale == idle.core.Cosmos.UNIVERSE) {
            GalaxyView.universe(g, w, h);
        } else {
            if (farColored != game.state().moleculesVersion()) {
                farColored = game.state().moleculesVersion();
                GalaxyView.tint(game, farColors);
            }
            GalaxyView.cluster(g, w, h, time, farColors, game);
        }
        String title = scale == idle.core.Cosmos.UNIVERSE ? "Univers : votre amas de galaxies, entouré, parmi tous les autres"
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
    boolean showsGalaxy() {
        return asGalaxy;
    }

    /** Vrai si la dernière image montrait la galaxie : pour les vérifications. */
    boolean swirled() {
        return swirled;
    }

    /** Nombre d'astres placés dans la vue : pour les vérifications. */
    int bodies() {
        return sky.length;
    }

    /** Nombre de molécules que les astres contiennent, et qui ne sont donc pas dessinées : pour les vérifications. */
    int bodyCells() {
        int total = 0;
        for (boolean each : hidden) {
            if (each) total++;
        }
        return total;
    }

    /** Ce dont les clairières des astres ont rétréci pour tenir dans le cœur, 1 si elles ont toute leur place : pour les vérifications. */
    double tightened() {
        return tightened;
    }

    /** Le rayon du cercle où tient le dessin d'un astre, en rayons de place : pour les vérifications. */
    double bodyRadius(int index) {
        return skyRadius[index];
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
        if (placedVersion == game.state().moleculesVersion() || calm) return;
        placedVersion = game.state().moleculesVersion();
        sincePlaced = 0;
        List<String> log = game.state().moleculeLog();
        // Trop de molécules pour un point chacune : chaque sorte garde sa part, un point en valant plusieurs.
        perDot = log.size() <= MOST_DOTS ? 1 : (log.size() + MOST_DOTS - 1) / MOST_DOTS;
        int count = log.size();
        if (perDot > 1) {
            count = 0;
            for (Molecule molecule : game.molecules()) count += dots(game.moleculeCount(molecule.id()));
        }
        distances = new double[count];
        angles = new double[count];
        radii = new double[count];
        shapes = new MoleculeArt.Shape[count];
        states = new Molecule.State[count];
        // La position de chaque place, en rayons de place : c'est dans cette unité que tout se mesure ici.
        double span = count == 0 ? 0 : Math.sqrt(count) / CELL_SHARE;
        double[] x = new double[count];
        double[] y = new double[count];
        cosines = new double[count];
        sines = new double[count];
        swayCos = new double[count];
        swaySin = new double[count];
        heaveCos = new double[count];
        heaveSin = new double[count];
        xs = new double[count];
        ys = new double[count];
        for (int index = 0; index < count; index++) {
            // La spirale du tournesol : la k-ième place est à la racine de sa part du disque.
            distances[index] = count == 1 ? 0 : Math.sqrt((index + 0.5) / count);
            angles[index] = index * GOLDEN_ANGLE;
            cosines[index] = Math.cos(angles[index]);
            sines[index] = Math.sin(angles[index]);
            x[index] = distances[index] * (span - 1) * cosines[index];
            y[index] = distances[index] * (span - 1) * sines[index];
            swayCos[index] = Math.cos(index * 1.7);
            swaySin[index] = Math.sin(index * 1.7);
            heaveCos[index] = Math.cos(index * 2.3);
            heaveSin[index] = Math.sin(index * 2.3);
        }
        Grid grid = new Grid(x, y, span);
        // Ce qu'il reste à placer de chaque sorte rassemblée, une fois servis les assemblages.
        java.util.Map<String, Integer> spare = new java.util.HashMap<>();
        for (String id : game.state().substances()) {
            if (game.gatheredMolecules(id) > 0) spare.put(id, dots(game.gatheredMolecules(id)));
        }
        // Les astres passent avant tout, du plus gros au plus petit. Chacun réunit ses assemblages : leurs molécules,
        // mêlées comme dans un bloc, toujours de la même façon pour un assemblage donné.
        List<Body> launched = new java.util.ArrayList<>();
        List<List<Assembly>> parts = new java.util.ArrayList<>();
        List<List<List<Molecule>>> contents = new java.util.ArrayList<>();
        List<Integer> sizes = new java.util.ArrayList<>();
        java.util.Set<String> inside = new java.util.HashSet<>();
        for (String id : game.state().bodies()) {
            Body body = game.body(id);
            List<Assembly> made = new java.util.ArrayList<>();
            List<List<Molecule>> held = new java.util.ArrayList<>();
            int size = 0;
            for (String part : body.assemblies()) {
                if (!game.hasAssembly(part)) continue;
                inside.add(part);
                List<Molecule> mixed = new java.util.ArrayList<>();
                take(game.assembly(part), spare, mixed);
                if (mixed.isEmpty()) continue;
                java.util.Collections.shuffle(mixed, new java.util.Random(part.hashCode()));
                made.add(game.assembly(part));
                held.add(mixed);
                size += mixed.size();
            }
            if (size == 0) continue;
            // Le trou noir supermassif tient le centre, quelle que soit sa taille ; les autres suivent, du plus gros au plus petit.
            int rank = body.tier() == Body.Tier.CORE || launched.isEmpty() || launched.get(0).tier() != Body.Tier.CORE ? 0 : 1;
            if (body.tier() != Body.Tier.CORE) {
                while (rank < launched.size() && sizes.get(rank) >= size) rank++;
            }
            launched.add(rank, body);
            parts.add(rank, made);
            contents.add(rank, held);
            sizes.add(rank, size);
        }
        // Puis les blocs des assemblages qui ne sont dans aucun astre formé, dans l'ordre où ils ont été
        // formés, puis les amas. Pour chacun : ses molécules, une par place.
        List<List<Molecule>> bodies = new java.util.ArrayList<>();
        List<Assembly> formed = new java.util.ArrayList<>();
        for (String id : game.state().assemblies()) {
            if (inside.contains(id)) continue;
            Assembly assembly = game.assembly(id);
            List<Molecule> mixed = new java.util.ArrayList<>();
            take(assembly, spare, mixed);
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
                gathered[place] += left * perDot;
            }
        }
        int[] owner = new int[count];
        java.util.Arrays.fill(owner, -1);
        assembled = new boolean[count];
        hidden = new boolean[count];
        tints = new Color[count];
        // Deux places sont voisines en deçà de cette distance : un peu plus que l'écart entre deux places de la spirale.
        double reachable = count == 0 ? 0 : NEIGHBOUR * (span - 1) / span;
        double join = count == 0 ? 0 : JOIN * (span - 1) / span;
        // Pour souder les amas : le rang de chaque place dans l'amas en cours (-1 ailleurs), et les voisines trouvées.
        int[] rank = new int[count];
        java.util.Arrays.fill(rank, -1);
        int[] near = new int[count];
        // Les corps, à mesure qu'ils prennent place : leurs places, leurs ponts, et leur assemblage si ce sont des blocs.
        List<int[]> cells = new java.util.ArrayList<>();
        List<int[]> links = new java.util.ArrayList<>();
        List<Assembly> block = new java.util.ArrayList<>();
        List<double[]> marks = new java.util.ArrayList<>();
        int[][] groups = launch(launched, sizes, x, y, owner, grid, span);
        int taken = 0;
        for (int body = 0; body < groups.length; body++) {
            cells.add(null);
            links.add(new int[0]);
            block.add(null);
            marks.add(null);
            taken += groups[body].length;
        }
        // Chaque astre garde le milieu de sa clairière pour son dessin ; ses assemblages se rangent autour, en blocs.
        for (int body = 0; body < groups.length; body++) {
            double cx = skyAway[body] * Math.cos(skyAngle[body]);
            double cy = skyAway[body] * Math.sin(skyAngle[body]);
            double middle = HEART * skyRound[body];
            int[] group = groups[body];
            int[] heart = new int[group.length];
            int inner = 0;
            long[] around = new long[group.length];
            int outer = 0;
            for (int cell : group) {
                if (Math.hypot(x[cell] - cx, y[cell] - cy) <= middle) {
                    heart[inner++] = cell;
                } else {
                    // Rangées par leur angle autour de l'astre : l'angle d'abord, la place ensuite, dans un seul nombre.
                    float angle = (float) (Math.atan2(y[cell] - cy, x[cell] - cx) + Math.PI);
                    around[outer++] = ((long) Float.floatToIntBits(angle) << 32) | cell;
                }
            }
            java.util.Arrays.sort(around, 0, outer);
            List<Assembly> made = parts.get(body);
            List<List<Molecule>> held = contents.get(body);
            // La couronne se partage entre les assemblages, à proportion de leur taille ; le reste de leurs molécules est sous l'astre.
            int[] share = new int[made.size()];
            int given = 0;
            for (int part = 0; part < share.length; part++) {
                share[part] = (int) ((long) outer * held.get(part).size() / sizes.get(body));
                given += share[part];
            }
            for (int part = 0; given < outer && part < share.length; part++) {
                int more = Math.min(outer - given, held.get(part).size() - share[part]);
                share[part] += more;
                given += more;
            }
            List<Molecule> under = new java.util.ArrayList<>();
            int from = 0;
            for (int part = 0; part < share.length; part++) {
                List<Molecule> molecules = held.get(part);
                int[] sector = new int[share[part]];
                Color tint = Color.web(made.get(part).tint());
                for (int index = 0; index < sector.length; index++) {
                    int cell = (int) around[from + index];
                    sector[index] = cell;
                    owner[cell] = cells.size();
                    settle(cell, molecules.get(index), molecules.get(index).state());
                    assembled[cell] = true;
                    tints[cell] = tint;
                }
                under.addAll(molecules.subList(sector.length, molecules.size()));
                if (sector.length > 0) {
                    // Son nom s'écrit au milieu de son arc, à mi-épaisseur de la couronne : le milieu de ses places serait sous l'astre.
                    double first = Float.intBitsToFloat((int) (around[from] >>> 32)) - Math.PI;
                    double last = Float.intBitsToFloat((int) (around[from + sector.length - 1] >>> 32)) - Math.PI;
                    double ring = (middle + skyRound[body]) / 2;
                    marks.add(new double[] {cx + ring * Math.cos((first + last) / 2), cy + ring * Math.sin((first + last) / 2)});
                    cells.add(sector);
                    links.add(weld(sector, new int[0], x, y, join, grid, rank, near));
                    block.add(made.get(part));
                }
                from += sector.length;
            }
            // Ce qui resterait de la couronne sans molécule à y mettre rejoint le milieu.
            for (; from < outer; from++) heart[inner++] = (int) around[from];
            for (int index = 0; index < inner; index++) {
                Molecule molecule = under.isEmpty() ? held.get(0).get(0) : under.get(Math.min(index, under.size() - 1));
                settle(heart[index], molecule, molecule.state());
                hidden[heart[index]] = true;
            }
            cells.set(body, java.util.Arrays.copyOf(heart, inner));
            skyRound[body] = middle;
        }
        Growth growth = new Growth(x, y, owner, grid, reachable);
        for (int index = 0; index < bodies.size(); index++) {
            List<Molecule> body = bodies.get(index);
            int size = Math.min(body.size(), count - taken);
            int[] tree = new int[2 * Math.max(0, size - 1)];
            int cluster = cells.size();
            int[] got = growth.gather(size, cluster, tree);
            cells.add(got);
            links.add(weld(got, tree, x, y, join, grid, rank, near));
            block.add(formed.get(index));
            marks.add(null);
            Color tint = formed.get(index) == null ? null : Color.web(formed.get(index).tint());
            for (int each = 0; each < size; each++) {
                int cell = got[each];
                settle(cell, body.get(each), body.get(each).state());
                assembled[cell] = tint != null;
                tints[cell] = tint;
            }
            taken += size;
        }
        clusters = cells.toArray(new int[0][]);
        bridges = links.toArray(new int[0][]);
        blocks = block.toArray(new Assembly[0]);
        int next = 0;
        if (perDot == 1) {
            for (String id : log) {
                if (game.gatheredMolecules(id) > 0) continue;
                while (next < count && owner[next] >= 0) next++;
                if (next >= count) break;
                settle(next++, game.molecule(id), null);
            }
        } else {
            // Un point pour plusieurs molécules : l'ordre de création ne se lit plus, les sortes passent dans celui du catalogue.
            for (Molecule molecule : game.molecules()) {
                if (game.gatheredMolecules(molecule.id()) > 0) continue;
                for (int left = dots(game.moleculeCount(molecule.id())); left > 0; left--) {
                    while (next < count && owner[next] >= 0) next++;
                    if (next >= count) break;
                    settle(next++, molecule, null);
                }
            }
        }
        collide(x, y, owner, span, grid);
        clear(x, y, owner, span, grid);
        swirl(span);
        // Ce que chaque image redemanderait : les couleurs de chaque corps, et le cercle qui le contient.
        rims = new Color[clusters.length];
        inks = new Color[clusters.length];
        middleX = new double[clusters.length];
        middleY = new double[clusters.length];
        labelX = new double[clusters.length];
        labelY = new double[clusters.length];
        rounds = new double[clusters.length];
        for (int cluster = launched.size(); cluster < clusters.length; cluster++) {
            int[] members = clusters[cluster];
            if (members.length == 0) continue;
            int first = members[0];
            rims[cluster] = blocks[cluster] != null ? BLOCK_EDGE : placeColor(states[first]);
            inks[cluster] = blocks[cluster] != null ? Color.web(blocks[cluster].tint()) : blobColor(shapes[first], states[first]);
            double sumX = 0;
            double sumY = 0;
            for (int cell : members) {
                sumX += x[cell];
                sumY += y[cell];
            }
            middleX[cluster] = sumX / members.length;
            middleY[cluster] = sumY / members.length;
            double[] mark = marks.get(cluster);
            labelX[cluster] = mark == null ? middleX[cluster] : mark[0];
            labelY[cluster] = mark == null ? middleY[cluster] : mark[1];
            double farthest = 0;
            for (int cell : members) farthest = Math.max(farthest, Math.hypot(x[cell] - middleX[cluster], y[cell] - middleY[cluster]));
            rounds[cluster] = farthest + BLOB_SHARE;
        }
    }

    /**
     * Prépare la galaxie, si elle est formée : où chaque place va quand le cœur s'enroule en bras,
     * et où les astres se rangent le long de ces bras.
     *
     * <p>Une place garde sa distance au centre ; son angle est tiré vers le bras le plus proche
     * ({@link Spiral#bend}). Les places se resserrent donc dans les bras, et chacune n'y est plus
     * qu'un point, aussi gros que le permet sa plus proche voisine. Le trou noir supermassif reste
     * au centre ; les autres astres se suivent le long des deux bras, les plus gros près du centre,
     * sans se toucher. S'ils n'y tiennent pas tous, ils rapetissent ensemble.
     */
    private void swirl(double span) {
        int count = distances.length;
        if (!game.hasGalaxy() || count == 0) {
            swirlCos = new double[0];
            swirlSin = new double[0];
            rooms = new double[0];
            swirlTints = new Color[0];
            swirlAway = new double[0];
            swirlAngle = new double[0];
            swirlRadius = new double[0];
            return;
        }
        swirlCos = new double[count];
        swirlSin = new double[count];
        rooms = new double[count];
        swirlTints = new Color[count];
        // Une couleur ne se mêle qu'une fois par sorte de molécule et par anneau de la galaxie.
        java.util.Map<Color, Color[]> toned = new java.util.HashMap<>();
        for (int index = 0; index < count; index++) {
            Color own = tints[index] != null ? tints[index] : shapes[index].dominant();
            Color[] rings = toned.computeIfAbsent(own, each -> new Color[TONES]);
            int ring = Math.min(TONES - 1, (int) (distances[index] * TONES));
            if (rings[ring] == null) {
                double out = (ring + 0.5) / TONES;
                rings[ring] = own.interpolate(BULGE_TONE.interpolate(ARM_TONE, Math.min(1, out / 0.45)), TONE_SHARE);
            }
            swirlTints[index] = rings[ring];
        }
        double limit = span - 1;
        double[] gx = new double[count];
        double[] gy = new double[count];
        for (int index = 0; index < count; index++) {
            double angle = Spiral.bend(distances[index], angles[index]);
            swirlCos[index] = Math.cos(angle);
            swirlSin[index] = Math.sin(angle);
            gx[index] = distances[index] * limit * swirlCos[index];
            gy[index] = distances[index] * limit * swirlSin[index];
        }
        Grid grid = new Grid(gx, gy, span);
        for (int index = 0; index < count; index++) {
            if (hidden[index]) continue;
            double least = 2 * DOT_SHARE;
            int close = grid.near(gx[index] - least, gy[index] - least, gx[index] + least, gy[index] + least);
            int[] found = grid.found();
            for (int each = 0; each < close; each++) {
                int other = found[each];
                if (other == index || hidden[other]) continue;
                double dx = gx[other] - gx[index];
                double dy = gy[other] - gy[index];
                if (dx * dx + dy * dy < least * least) least = Math.sqrt(dx * dx + dy * dy);
            }
            rooms[index] = least / 2;
        }
        // Les astres : celui du centre s'il y est, puis les autres le long des bras, tour à tour sur chacun.
        int many = sky.length;
        swirlAway = new double[many];
        swirlAngle = new double[many];
        swirlRadius = new double[many];
        double size = 0.8;
        while (!line(size, limit) && size > 0.05) size *= 0.9;
    }

    /**
     * Range les astres le long des bras de la galaxie, chacun dessiné à {@code size} fois sa taille
     * d'avant, sans dépasser ce qu'un bras a de large.
     *
     * @return faux s'ils n'y tiennent pas tous
     */
    private boolean line(double size, double limit) {
        int first = 0;
        double[] lastX = new double[Spiral.ARMS];
        double[] lastY = new double[Spiral.ARMS];
        double[] lastRadius = new double[Spiral.ARMS];
        double[] at = new double[Spiral.ARMS];
        // Deux bras voisins sont à cette distance l'un de l'autre : un astre n'en prend pas plus des quatre dixièmes.
        double widest = 0.4 * limit * (2 * Math.PI / Spiral.ARMS) / Spiral.TWIST;
        if (sky.length > 0 && sky[0].tier() == Body.Tier.CORE) {
            swirlAway[0] = 0;
            swirlAngle[0] = 0;
            swirlRadius[0] = Math.min(size * skyRadius[0] / HEART, 0.5 * widest + 0.06 * limit);
            java.util.Arrays.fill(lastRadius, swirlRadius[0]);
            first = 1;
        }
        java.util.Arrays.fill(at, 0.1);
        for (int body = first; body < sky.length; body++) {
            int arm = (body - first) % Spiral.ARMS;
            double radius = Math.min(size * skyRadius[body] * share(sky[body].tier()), widest);
            double share = at[arm];
            double x;
            double y;
            // On avance le long du bras jusqu'à ne plus toucher l'astre d'avant.
            while (true) {
                double angle = Spiral.arm(share) + arm * 2 * Math.PI / Spiral.ARMS;
                x = share * limit * Math.cos(angle);
                y = share * limit * Math.sin(angle);
                if (Math.hypot(x - lastX[arm], y - lastY[arm]) >= radius + lastRadius[arm] + BODY_GAP) break;
                share += 0.002;
                if (share * limit + radius > 0.97 * limit) return false;
            }
            if (share * limit + radius > 0.97 * limit) return false;
            swirlAway[body] = share * limit;
            swirlAngle[body] = Spiral.arm(share) + arm * 2 * Math.PI / Spiral.ARMS;
            swirlRadius[body] = radius;
            lastX[arm] = x;
            lastY[arm] = y;
            lastRadius[arm] = radius;
            at[arm] = share;
        }
        return true;
    }

    /** Retire de {@code spare} les molécules d'un assemblage, et les ajoute à {@code into}, une par exemplaire. */
    private void take(Assembly assembly, java.util.Map<String, Integer> spare, List<Molecule> into) {
        for (java.util.Map.Entry<String, Integer> ingredient : assembly.ingredients().entrySet()) {
            int taken = Math.min(dots(ingredient.getValue()), spare.getOrDefault(ingredient.getKey(), 0));
            spare.merge(ingredient.getKey(), -taken, Integer::sum);
            for (int copy = 0; copy < taken; copy++) into.add(game.molecule(ingredient.getKey()));
        }
    }

    /**
     * Installe les astres : ce sont les premiers corps, et chacun prend autant de places qu'il a de
     * molécules.
     *
     * <p>Chaque astre a une clairière ronde, de la surface de ses places. Les clairières sont posées
     * l'une après l'autre, la plus grande au centre, chaque suivante accolée aux précédentes au plus
     * près du centre, sans qu'aucune en recouvre une autre ni ne sorte du cœur. Si elles n'y tiennent
     * pas toutes, elles rétrécissent ensemble jusqu'à y tenir ({@link #TIGHTER}) : un astre garde
     * alors autour de sa clairière les places qui n'y entrent plus. L'astre lui-même n'occupe que le
     * milieu de sa clairière ({@link #HEART}) : le reste est la couronne de ses assemblages.
     *
     * @return les places de chaque astre
     */
    private int[][] launch(List<Body> launched, List<Integer> wanted, double[] x, double[] y, int[] owner, Grid grid, double span) {
        int many = launched.size();
        int count = x.length;
        sky = launched.toArray(new Body[0]);
        skyAway = new double[many];
        skyAngle = new double[many];
        skyRound = new double[many];
        skyRadius = new double[many];
        skyTints = new Color[many];
        skyX = new double[many];
        skyY = new double[many];
        tightened = 1;
        if (many == 0) return new int[0][];
        // Les places tiennent dans le disque de rayon span - 1 : n places y occupent un disque de rayon (span - 1) × √(n / toutes).
        double limit = span - 1;
        double[] full = new double[many];
        int[] sizes = new int[many];
        int left = count;
        for (int body = 0; body < many; body++) {
            sizes[body] = Math.min(wanted.get(body), left);
            left -= sizes[body];
            full[body] = limit * Math.sqrt(CLEARING * sizes[body] / count);
        }
        double[] cx = new double[many];
        double[] cy = new double[many];
        while (!pack(full, tightened, limit, cx, cy, skyRound) && tightened > 0.02) tightened *= TIGHTER;
        // D'abord ce qui est dans chaque clairière, pour tous : aucune ne peut alors mordre sur une autre.
        int[][] groups = new int[many][];
        int[] held = new int[many];
        for (int body = 0; body < many; body++) {
            groups[body] = new int[sizes[body]];
            held[body] = claim(cx[body], cy[body], skyRound[body], sizes[body], body, groups[body], 0, x, y, owner, grid);
        }
        // Puis ce qui manque à chacun : les places libres les plus proches de sa clairière.
        for (int body = 0; body < many; body++) {
            double within = skyRound[body] + FAR;
            while (held[body] < sizes[body]) {
                held[body] = claim(cx[body], cy[body], within, sizes[body], body, groups[body], held[body], x, y, owner, grid);
                within = within * 1.3 + FAR;
            }
            skyAway[body] = Math.hypot(cx[body], cy[body]);
            skyAngle[body] = Math.atan2(cy[body], cx[body]);
            skyTints[body] = Color.web(sky[body].tint());
        }
        return groups;
    }

    /**
     * Pose les clairières l'une après l'autre : la première au centre, chaque suivante contre une de
     * celles qui sont posées, là où elle est le plus près du centre sans rien recouvrir.
     *
     * @param full  le rayon de chaque clairière quand elle a toute sa place ; {@code share} ce qu'il en reste
     * @param limit le rayon du disque où elles doivent tenir
     * @param round reçoit leurs rayons
     * @return faux si l'une d'elles n'a trouvé aucune place
     */
    private static boolean pack(double[] full, double share, double limit, double[] cx, double[] cy, double[] round) {
        for (int body = 0; body < full.length; body++) round[body] = share * full[body];
        for (int body = 0; body < full.length; body++) {
            double best = Double.POSITIVE_INFINITY;
            if (body == 0 && round[0] <= limit) {
                cx[0] = 0;
                cy[0] = 0;
                best = 0;
            }
            for (int host = 0; host < body; host++) {
                double away = round[host] + round[body];
                for (int bearing = 0; bearing < BEARINGS; bearing++) {
                    double angle = 2 * Math.PI * bearing / BEARINGS + host;
                    double px = cx[host] + away * Math.cos(angle);
                    double py = cy[host] + away * Math.sin(angle);
                    double out = Math.hypot(px, py);
                    if (out >= best || out + round[body] > limit) continue;
                    boolean free = true;
                    for (int other = 0; other < body && free; other++) {
                        free = other == host || Math.hypot(px - cx[other], py - cy[other]) >= round[other] + round[body] - 1e-9;
                    }
                    if (!free) continue;
                    best = out;
                    cx[body] = px;
                    cy[body] = py;
                }
            }
            if (best == Double.POSITIVE_INFINITY) return false;
        }
        return true;
    }

    /**
     * Donne à un astre les places libres à moins de {@code within} de (cx, cy), les plus proches
     * d'abord, jusqu'à ce qu'il en ait {@code wanted}.
     *
     * @param group reçoit les places, à la suite des {@code held} qu'il a déjà
     * @return le nombre de places qu'il a maintenant
     */
    private static int claim(double cx, double cy, double within, int wanted, int cluster, int[] group, int held,
            double[] x, double[] y, int[] owner, Grid grid) {
        if (held >= wanted) return held;
        int close = grid.near(cx - within, cy - within, cx + within, cy + within);
        int[] found = grid.found();
        // Chaque place libre, rangée par sa distance : la distance d'abord, la place ensuite, dans un seul nombre.
        long[] ranked = new long[close];
        int free = 0;
        for (int index = 0; index < close; index++) {
            int cell = found[index];
            if (owner[cell] >= 0) continue;
            double gap = Math.hypot(x[cell] - cx, y[cell] - cy);
            if (gap > within) continue;
            ranked[free++] = ((long) Float.floatToIntBits((float) gap) << 32) | cell;
        }
        java.util.Arrays.sort(ranked, 0, free);
        for (int index = 0; index < free && held < wanted; index++) {
            int cell = (int) ranked[index];
            owner[cell] = cluster;
            group[held++] = cell;
        }
        return held;
    }

    /**
     * Mesure le cercle où le dessin de chaque astre peut tenir : sa clairière, moins tout ce qui
     * s'en approche. Rien de ce qui est dessiné autour ne doit être touché : ni le rond ou le pont
     * d'un amas, ni une molécule isolée, ni le bord du cœur. Les autres astres n'ont pas à être
     * mesurés, leurs clairières ne se recouvrent pas.
     */
    private void clear(double[] x, double[] y, int[] owner, double span, Grid grid) {
        for (int body = 0; body < sky.length; body++) {
            double cx = skyAway[body] * Math.cos(skyAngle[body]);
            double cy = skyAway[body] * Math.sin(skyAngle[body]);
            double round = Math.min(skyRound[body], span - skyAway[body]);
            double look = skyRound[body] + FAR;
            int close = grid.near(cx - look, cy - look, cx + look, cy + look);
            int[] found = grid.found();
            for (int each = 0; each < close; each++) {
                int other = found[each];
                if (hidden[other]) continue;
                round = Math.min(round, Math.hypot(x[other] - cx, y[other] - cy) - (owner[other] < 0 ? radii[other] : blobs[other]));
            }
            for (int cluster = sky.length; cluster < bridges.length; cluster++) {
                int[] links = bridges[cluster];
                for (int link = 0; link < links.length; link += 2) {
                    double width = widths[cluster][link / 2];
                    if (width <= 0) continue;
                    int from = links[link];
                    int to = links[link + 1];
                    if (Math.min(x[from], x[to]) > cx + look || Math.max(x[from], x[to]) < cx - look
                            || Math.min(y[from], y[to]) > cy + look || Math.max(y[from], y[to]) < cy - look) continue;
                    round = Math.min(round, away(cx, cy, x[from], y[from], x[to], y[to]) - width);
                }
            }
            skyRadius[body] = Math.max(0, round - BODY_GAP);
        }
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
    private static int[] weld(int[] cells, int[] tree, double[] x, double[] y, double join, Grid grid, int[] rank, int[] near) {
        Pairs seen = new Pairs(4 * cells.length + 16);
        int[] pairs = new int[Math.max(16, 8 * cells.length)];
        int count = 0;
        for (int link = 0; link < tree.length; link += 2) {
            if (seen.add(pair(tree[link], tree[link + 1]))) {
                if (count + 2 > pairs.length) pairs = java.util.Arrays.copyOf(pairs, 2 * pairs.length);
                pairs[count++] = tree[link];
                pairs[count++] = tree[link + 1];
            }
        }
        // Le rang de chaque place dans l'amas : les ponts sont posés dans l'ordre des places, comme si on les comparait toutes deux à deux.
        for (int index = 0; index < cells.length; index++) rank[cells[index]] = index;
        for (int first = 0; first < cells.length; first++) {
            int from = cells[first];
            // Seules les places proches peuvent être à portée : celles de l'amas, plus loin que celle-ci dans son ordre.
            int close = grid.near(x[from] - join, y[from] - join, x[from] + join, y[from] + join);
            int[] found = grid.found();
            int kept = 0;
            for (int each = 0; each < close; each++) {
                int to = found[each];
                if (rank[to] <= first) continue;
                if (Math.abs(x[to] - x[from]) > join || Math.abs(y[to] - y[from]) > join) continue;
                if (Math.hypot(x[to] - x[from], y[to] - y[from]) > join) continue;
                near[kept++] = rank[to];
            }
            java.util.Arrays.sort(near, 0, kept);
            for (int each = 0; each < kept; each++) {
                int to = cells[near[each]];
                if (seen.add(pair(from, to))) {
                    if (count + 2 > pairs.length) pairs = java.util.Arrays.copyOf(pairs, 2 * pairs.length);
                    pairs[count++] = from;
                    pairs[count++] = to;
                }
            }
        }
        for (int cell : cells) rank[cell] = -1;
        return java.util.Arrays.copyOf(pairs, count);
    }

    /**
     * Un ensemble de paires de places ({@link #pair}), sans rien d'autre que deux tableaux : la mise
     * en place en range des dizaines de milliers.
     */
    private static final class Pairs {
        private final long[] keys;
        private final boolean[] used;
        private final int mask;

        Pairs(int room) {
            int size = Integer.highestOneBit(Math.max(16, 2 * room) - 1) << 1;
            keys = new long[size];
            used = new boolean[size];
            mask = size - 1;
        }

        private int slot(long key) {
            long mixed = key * 0x9E3779B97F4A7C15L;
            int slot = (int) (mixed >>> 40) & mask;
            while (used[slot] && keys[slot] != key) slot = (slot + 1) & mask;
            return slot;
        }

        /** Ajoute une paire. Faux si elle y était déjà. */
        boolean add(long key) {
            int slot = slot(key);
            if (used[slot]) return false;
            used[slot] = true;
            keys[slot] = key;
            return true;
        }

        boolean contains(long key) {
            return used[slot(key)];
        }
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
            // Une place d'astre n'a pas de rond : c'est l'astre qui est dessiné, et mesuré à part ({@link #clear}).
            if (owner[index] < 0 || hidden[index]) continue;
            double radius = Math.min(BLOB_SHARE, span - Math.hypot(x[index], y[index]));
            // Au-delà de deux ronds entiers, rien ne peut se toucher : seules les places proches sont mesurées.
            int close = grid.near(x[index] - FAR, y[index] - FAR, x[index] + FAR, y[index] + FAR);
            int[] found = grid.found();
            for (int each = 0; each < close; each++) {
                int other = found[each];
                if (owner[other] == owner[index]) continue;
                // Trop loin pour raccourcir ce rond : la mesure exacte, plus lente, n'apprendrait rien.
                double dx = x[other] - x[index];
                double dy = y[other] - y[index];
                double harmless = owner[other] < 0 ? radius + radii[other] : 2 * radius;
                if (dx * dx + dy * dy > harmless * harmless * SURELY) continue;
                double gap = Math.hypot(dx, dy);
                radius = Math.min(radius, owner[other] < 0 ? gap - radii[other] : gap / 2);
            }
            blobs[index] = Math.max(0, radius);
        }
        overlap = Double.NEGATIVE_INFINITY;
        contacts = 0;
        for (int index = 0; index < count; index++) {
            if (owner[index] < 0 || hidden[index]) continue;
            int close = grid.near(x[index] - FAR, y[index] - FAR, x[index] + FAR, y[index] + FAR);
            int[] found = grid.found();
            for (int each = 0; each < close; each++) {
                int other = found[each];
                if (other <= index || owner[other] < 0 || hidden[other] || owner[other] == owner[index]) continue;
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
                    // Trop loin pour rétrécir ce pont : inutile de mesurer exactement.
                    if (beyond(x[other], y[other], x[from], y[from], x[to], y[to], width + body)) continue;
                    width = Math.min(width, away(x[other], y[other], x[from], y[from], x[to], y[to]) - body);
                }
                // ... et les ponts déjà posés des autres amas : ceux qui partent d'une place proche, et les très longs.
                if (width > 0) {
                    close = grid.near(left - longest, top - longest, right + longest, bottom + longest);
                    found = grid.found();
                    for (int each = 0; each < close && width > 0; each++) {
                        for (int other = firstLaid[found[each]]; other >= 0 && width > 0; other = laidNext[other]) {
                            if (laidCluster[other] == cluster) continue;
                            if (aside(x[from], y[from], x[to], y[to], x[laidFrom[other]], y[laidFrom[other]],
                                    x[laidTo[other]], y[laidTo[other]], width + laidWidth[other])) continue;
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
        int[] start = new int[count];
        int[] next = new int[count];
        for (int cluster = 0; cluster < bridges.length; cluster++) fills[cluster] = triangles(cluster, x, y, owner, grid, start, next);
    }

    /**
     * Les triangles pleins d'un amas : chaque triplet de ses molécules reliées deux à deux par des
     * ponts dessinés, s'il ne contient aucune place étrangère à l'amas. Comme ses trois côtés sont
     * des ponts qui ont eu la place de passer, rien d'un autre amas ne peut y entrer.
     *
     * @return les triangles, trois places par triangle
     */
    private int[] triangles(int cluster, double[] x, double[] y, int[] owner, Grid grid, int[] start, int[] next) {
        int[] links = bridges[cluster];
        if (links.length == 0) return new int[0];
        // Pour chaque place : les places auxquelles un pont dessiné la relie, dans l'ordre des ponts. Elles sont rangées
        // à la suite dans un seul tableau ; start dit où commencent celles de chaque place.
        Pairs drawn = new Pairs(links.length);
        int[] cells = clusters[cluster];
        for (int cell : cells) next[cell] = 0;
        for (int link = 0; link < links.length; link += 2) {
            if (widths[cluster][link / 2] <= 0) continue;
            drawn.add(pair(links[link], links[link + 1]));
            next[links[link]]++;
            next[links[link + 1]]++;
        }
        int total = 0;
        for (int cell : cells) {
            start[cell] = total;
            total += next[cell];
            next[cell] = start[cell];
        }
        int[] joined = new int[total];
        for (int link = 0; link < links.length; link += 2) {
            if (widths[cluster][link / 2] <= 0) continue;
            joined[next[links[link]]++] = links[link + 1];
            joined[next[links[link + 1]]++] = links[link];
        }
        int[] kept = new int[Math.max(16, 3 * links.length)];
        int count = 0;
        for (int link = 0; link < links.length; link += 2) {
            int a = links[link];
            int b = links[link + 1];
            if (!drawn.contains(pair(a, b))) continue;
            for (int index = start[a]; index < next[a]; index++) {
                int c = joined[index];
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
                if (count + 3 > kept.length) kept = java.util.Arrays.copyOf(kept, 2 * kept.length);
                kept[count++] = a;
                kept[count++] = b;
                kept[count++] = c;
            }
        }
        return java.util.Arrays.copyOf(kept, count);
    }

    /**
     * Vrai si le point (px, py) est à coup sûr à plus de {@code reach} du segment qui va de (ax, ay)
     * à (bx, by) : le même calcul que {@link #away}, sans sa racine, avec une marge qui couvre les
     * arrondis. Faux dans le doute : l'appelant mesure alors exactement.
     */
    private static boolean beyond(double px, double py, double ax, double ay, double bx, double by, double reach) {
        if (reach <= 0) return false;
        double dx = bx - ax;
        double dy = by - ay;
        double length = dx * dx + dy * dy;
        double along = length <= 0 ? 0 : Math.max(0, Math.min(1, ((px - ax) * dx + (py - ay) * dy) / length));
        double offX = px - (ax + along * dx);
        double offY = py - (ay + along * dy);
        return offX * offX + offY * offY > reach * reach * SURELY;
    }

    /** Distance du point (px, py) au segment qui va de (ax, ay) à (bx, by). */
    private static double away(double px, double py, double ax, double ay, double bx, double by) {
        double dx = bx - ax;
        double dy = by - ay;
        double length = dx * dx + dy * dy;
        double along = length <= 0 ? 0 : Math.max(0, Math.min(1, ((px - ax) * dx + (py - ay) * dy) / length));
        return Math.hypot(px - (ax + along * dx), py - (ay + along * dy));
    }

    /**
     * Vrai si les segments (a, b) et (c, d) sont à coup sûr à plus de {@code reach} l'un de l'autre :
     * leurs cadres sont plus écartés que cela, en largeur ou en hauteur. Faux dans le doute :
     * l'appelant mesure alors exactement ({@link #apart}).
     */
    private static boolean aside(double ax, double ay, double bx, double by, double cx, double cy, double dx, double dy, double reach) {
        if (reach <= 0) return false;
        double far = reach * SURELY;
        return Math.min(cx, dx) - Math.max(ax, bx) > far || Math.min(ax, bx) - Math.max(cx, dx) > far
                || Math.min(cy, dy) - Math.max(ay, by) > far || Math.min(ay, by) - Math.max(cy, dy) > far;
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
            case GAS -> GAS_COLOR;
            case LIQUID -> LIQUID_COLOR;
            case SOLID -> SOLID_COLOR;
            case CRYSTAL -> CRYSTAL_COLOR;
            case METAL -> METAL_COLOR;
        };
    }

    private static final Color GAS_COLOR = Color.web("#bfe6f2");
    private static final Color LIQUID_COLOR = Color.web("#4d8fe0");
    private static final Color SOLID_COLOR = Color.web("#d9b36c");
    private static final Color CRYSTAL_COLOR = Color.web("#b48cf2");
    private static final Color METAL_COLOR = Color.web("#aeb6c4");

    /**
     * Fait avancer le temps de la vue et la redessine.
     *
     * @param elapsed secondes écoulées depuis l'image précédente
     */
    void frame(double elapsed) {
        time += elapsed;
        beat++;
        sincePlaced += elapsed;
        boolean stale = placedVersion != game.state().moleculesVersion();
        // La création automatique ajoute des molécules toutes les quelques secondes : une grosse vue ne
        // se remet en place que de loin en loin, sinon elle accrocherait à chaque passage.
        calm = stale && game.isAutoCreatingMolecules() && shapes.length >= CALM_FROM && sincePlaced < CALM_SECONDS;
        boolean changed = moved || getWidth() != drawnWidth || getHeight() != drawnHeight || (stale && !calm);
        idle.core.Cosmos scale = cosmosShown();
        if (scale != null) {
            // L'amas de galaxies tourne lentement ; la toile de l'univers ne bouge pas.
            boolean still = scale == idle.core.Cosmos.UNIVERSE && farDrawn;
            if (!moved && getWidth() == drawnWidth && getHeight() == drawnHeight && (still || beat % EVERY != 0)) return;
            redraw();
            return;
        }
        if (farDrawn) {
            // De retour dans l'espace : tout est à redessiner.
            farDrawn = false;
            changed = true;
        }
        if (!changed && beat % EVERY != 0) return;
        redraw();
        calm = false;
    }

    /** Nombre de points à partir duquel la vue attend, pendant la création automatique, avant de se remettre en place. */
    static final int CALM_FROM = 5_000;
    /** Secondes entre deux mises en place d'une grosse vue pendant la création automatique. */
    static final double CALM_SECONDS = 15;
    private double sincePlaced = CALM_SECONDS;
    private boolean calm = false;

    /** Dessine la vue telle qu'elle est à cet instant. */
    private void redraw() {
        double w = getWidth();
        double h = getHeight();
        if (w <= 0 || h <= 0) return;
        moved = false;
        drawnWidth = w;
        drawnHeight = h;
        // Au-delà de la galaxie, la vue ne montre plus l'espace : inutile d'y ranger les molécules.
        idle.core.Cosmos beyond = cosmosShown();
        if (beyond != null) {
            farDrawn = true;
            drawFar(getGraphicsContext2D(), w, h, beyond);
            return;
        }
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
        // Une fois la galaxie formée, tout est montré enroulé en bras, à moins que le joueur n'ait demandé à revoir la matière.
        swirled = asGalaxy && game.hasGalaxy() && swirlCos.length == count && count > 0;
        // La galaxie tourne dans le sens où ses bras traînent derrière elle, et plus vite que la matière ne dérivait.
        double turn = swirled ? -time * DRIFT * GALAXY_SPIN : time * DRIFT;
        // Le cosinus et le sinus de l'angle de chaque place sont connus : il ne reste qu'à les tourner tous du même angle.
        double cosTurn = Math.cos(turn);
        double sinTurn = Math.sin(turn);
        double[] cosOf = swirled ? swirlCos : cosines;
        double[] sinOf = swirled ? swirlSin : sines;
        reach = 0;
        for (int index = 0; index < count; index++) {
            double out = distances[index] * (heart - cell);
            if (!hidden[index]) reach = Math.max(reach, out + (swirled ? rooms[index] : states[index] != null ? blobs[index] : radii[index]) * cell);
            xs[index] = originX + out * (cosOf[index] * cosTurn - sinOf[index] * sinTurn) * scale;
            ys[index] = originY + out * (sinOf[index] * cosTurn + cosOf[index] * sinTurn) * scale;
        }
        double unit = cell * scale;
        double[] awayOf = swirled ? swirlAway : skyAway;
        double[] angleOf = swirled ? swirlAngle : skyAngle;
        shown = swirled ? swirlRadius : skyRadius;
        for (int body = 0; body < sky.length; body++) {
            reach = Math.max(reach, (awayOf[body] + shown[body]) * cell);
            skyX[body] = originX + awayOf[body] * unit * Math.cos(angleOf[body] + turn);
            skyY[body] = originY + awayOf[body] * unit * Math.sin(angleOf[body] + turn);
        }
        // De loin, la matière se fond en gaz : pas du tout au-dessus de MIST_FROM pixels par place, entièrement sous MIST_FULL.
        mist = count == 0 ? 0 : Math.max(0, Math.min(1, (MIST_FROM - unit) / (MIST_FROM - MIST_FULL)));

        // Les amas d'abord, sous les molécules : pour chaque sorte rassemblée, un rond par molécule, un
        // pont entre voisines et un triangle plein entre trois voisines, donc une seule surface. Elle est
        // dessinée deux fois : en entier dans la couleur de son état, puis un peu rétrécie dans sa couleur
        // à elle, ce qui lui laisse un liseré tout autour. De très loin, le gaz les remplace. Ronds et ponts ont été taillés pour ne recouvrir aucun autre amas ({@link #collide}).
        double widest = BLOB_SHARE * unit;
        if (swirled) {
            // La galaxie n'a plus ni amas ni blocs : sa lueur d'abord, sous ses points.
            double glow = heart * scale;
            if (glow > 1) {
                g.setFill(new javafx.scene.paint.RadialGradient(0, 0, originX, originY, glow, false,
                        javafx.scene.paint.CycleMethod.NO_CYCLE, GALAXY_GLOW));
                g.fillOval(originX - glow, originY - glow, 2 * glow, 2 * glow);
            }
        }
        if (unit >= 2 && mist < 1 && !swirled) {
            // Un pont s'arrête net au centre de ses deux ronds : avec un bout carré, ses coins en dépasseraient.
            g.setLineCap(StrokeLineCap.BUTT);
            for (int cluster = sky.length; cluster < clusters.length; cluster++) {
                if (clusters[cluster].length == 0) continue;
                // Un corps tout entier hors de la vue : aucun de ses ronds, de ses ponts ni de ses triangles n'y serait dessiné.
                double around = rounds[cluster] * unit;
                double midX = originX + (middleX[cluster] * cosTurn - middleY[cluster] * sinTurn) * unit;
                double midY = originY + (middleY[cluster] * cosTurn + middleX[cluster] * sinTurn) * unit;
                if (midX + around < 0 || midX - around > w || midY + around < 0 || midY - around > h) continue;
                Assembly block = blocks[cluster];
                // Un amas : sa couleur sous un fin liseré de la couleur de son état. Un bloc d'assemblage : la
                // couleur de sa matière sous un contour clair, deux fois plus épais. De loin, un contour plus
                // fin qu'un pixel n'est pas dessiné, et le corps est d'une seule couleur.
                double rim = block != null ? BLOCK_RIM : RIM;
                if (rim * unit < 0.75) rim = 0;
                for (int pass = rim > 0 ? 0 : 1; pass < 2; pass++) {
                    Color color = pass == 0 ? rims[cluster] : inks[cluster];
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
            double puff = Math.max(1, (swirled ? SWIRL_PUFF : PUFF_SHARE) * unit);
            double churn = CHURN * unit * mist;
            g.setGlobalAlpha((swirled ? SWIRL_ALPHA : PUFF_ALPHA) * mist);
            // sin(a + b) et cos(a + b), où a ne dépend que du temps et b que de la place.
            double swayTimeCos = Math.cos(time * 0.35);
            double swayTimeSin = Math.sin(time * 0.35);
            double heaveTimeCos = Math.cos(time * 0.28);
            double heaveTimeSin = Math.sin(time * 0.28);
            for (int index = 0; index < count; index++) {
                double x = xs[index] + churn * (swayTimeSin * swayCos[index] + swayTimeCos * swaySin[index]);
                double y = ys[index] + churn * (heaveTimeCos * heaveCos[index] - heaveTimeSin * heaveSin[index]);
                if (hidden[index] || x + puff < 0 || x - puff > w || y + puff < 0 || y - puff > h) continue;
                g.setFill(swirled ? swirlTints[index] : tints[index] != null ? tints[index] : shapes[index].dominant());
                g.fillOval(x - puff, y - puff, 2 * puff, 2 * puff);
            }
            g.setGlobalAlpha(1);
            blur.setRadius(Math.min(63, Math.max(LEAST_BLUR, BLUR_SHARE * unit) * mist));
            g.applyEffect(blur);
        }

        // Ce qui reste net par-dessus le gaz : les étoiles, le cercle discret du cœur utilisé, le bord de l'espace.
        drawStars(g, w, h);
        double core = coreRadius() * scale;
        if (core > 4 && !swirled) {
            g.setStroke(CORE);
            g.setLineWidth(1);
            g.strokeOval(originX - core, originY - core, 2 * core, 2 * core);
        }
        if (edge > 0.5) {
            g.setStroke(EDGE);
            g.setLineWidth(1.5);
            g.strokeOval(originX - edge, originY - edge, 2 * edge, 2 * edge);
        }

        // Les astres, nets même de très loin : chacun au milieu de sa clairière, aussi grand qu'elle le permet.
        for (int body = 0; body < sky.length; body++) {
            double round = shown[body] * unit;
            double x = skyX[body];
            double y = skyY[body];
            if (round <= 0 || x + round < 0 || x - round > w || y + round < 0 || y - round > h) continue;
            if (round < DOT_BELOW) {
                double dot = Math.max(0.75, round);
                g.setFill(skyTints[body]);
                g.fillOval(x - dot, y - dot, 2 * dot, 2 * dot);
                continue;
            }
            // Un petit astre n'occupe pas toute sa clairière : une planète reste plus grande qu'une lune, quoi qu'elles contiennent.
            // Dans la galaxie, ce rayon est déjà celui du dessin.
            double r = round * (swirled ? 1 : share(sky[body].tier())) / BodyArt.span(sky[body]);
            BodyArt.draw(g, sky[body], x - BodyArt.centerX(sky[body]) * r, y - BodyArt.centerY(sky[body]) * r, r);
        }

        // Puis les molécules, celles qui tombent dans la vue seulement : elles s'effacent à mesure que le gaz les remplace.
        if (mist < 1) {
            g.setGlobalAlpha(1 - mist);
            for (int index = 0; index < count && swirled; index++) {
                // Dans la galaxie, une molécule est un point de sa couleur ; de tout près, on la revoit, en petit.
                double x = xs[index];
                double y = ys[index];
                double dot = Math.max(0.8, rooms[index] * unit);
                if (hidden[index] || x + dot < 0 || x - dot > w || y + dot < 0 || y - dot > h) continue;
                if (dot >= DRAWN_FROM) {
                    shapes[index].draw(g, x, y, dot, time * (0.05 + 0.04 * ((index * 7) % 5)) * (index % 2 == 0 ? 1 : -1) + index);
                } else {
                    g.setFill(swirlTints[index]);
                    g.fillOval(x - dot, y - dot, 2 * dot, 2 * dot);
                }
            }
            for (int index = 0; index < count && !swirled; index++) {
                double x = xs[index];
                double y = ys[index];
                double size = radii[index] * cell * scale;
                if (hidden[index] || x + size < 0 || x - size > w || y + size < 0 || y - size > h) continue;
                // Dans un bloc d'assemblage, une molécule n'est qu'un grain anguleux et fixe, tant qu'on n'est pas tout près.
                if (assembled[index] && unit < GRAINS_BELOW) {
                    if (unit >= 2.5) grain(g, shapes[index].dominant(), x, y, GRAIN_SHARE * unit, index * 0.9, grainX, grainY);
                    continue;
                }
                // Chacune tourne sur elle-même, à son rythme et dans son sens.
                double spin = time * (0.05 + 0.04 * ((index * 7) % 5)) * (index % 2 == 0 ? 1 : -1) + index;
                shapes[index].draw(g, x, y, states[index] != null ? size * GATHERED_SHARE : size, spin);
            }
            if (!swirled) drawNames(g, originX, originY, cosTurn, sinTurn, unit, w, h);
            g.setGlobalAlpha(1);
        }
        drawBodyNames(g, unit, w, h);
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
    private void drawNames(GraphicsContext g, double originX, double originY, double cosTurn, double sinTurn, double unit, double w, double h) {
        g.setTextAlign(TextAlignment.CENTER);
        for (int cluster = 0; cluster < clusters.length; cluster++) {
            Assembly block = blocks[cluster];
            int size = clusters[cluster].length;
            if (block == null || size == 0) continue;
            // Un bloc de n places fait à peu près 2,4 × √n rayons de place de large.
            double across = 2.4 * Math.sqrt(size) * unit;
            if (across < NAME_FROM) continue;
            double x = originX + (labelX[cluster] * cosTurn - labelY[cluster] * sinTurn) * unit;
            double y = originY + (labelY[cluster] * cosTurn + labelX[cluster] * sinTurn) * unit;
            double font = Math.max(11, Math.min(22, across / (0.62 * block.name().length() + 4)));
            double wide = 0.62 * font * block.name().length() + font;
            if (x + wide / 2 < 0 || x - wide / 2 > w || y + font < 0 || y - font > h) continue;
            Color tint = inks[cluster];
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
     * Part de sa clairière qu'occupe le dessin d'un astre, selon son échelle : les clairières ont la
     * taille de ce que les astres contiennent, et une lune contient presque autant qu'une planète.
     */
    private static double share(Body.Tier tier) {
        return switch (tier) {
            case RUBBLE -> 0.7;
            case ASTEROID -> 0.6;
            case MOON -> 0.72;
            case REMNANT -> 0.5;
            case COMET, PLANET, STAR, BLACK_HOLE, CORE -> 1;
        };
    }

    /**
     * Écrit le nom de chaque astre au bas de sa clairière, dès qu'il est assez large à l'écran : en
     * clair sur une pastille sombre. Plus près encore, les assemblages dont il est fait sont écrits dessous.
     */
    private void drawBodyNames(GraphicsContext g, double unit, double w, double h) {
        g.setTextAlign(TextAlignment.CENTER);
        // Les noms déjà écrits, quatre nombres chacun : gauche, haut, droite, bas. Un nom qui en recouvrirait un autre n'est pas écrit.
        if (labels.length < 4 * sky.length) labels = new double[4 * sky.length];
        int written = 0;
        for (int body = 0; body < sky.length; body++) {
            double across = 2 * shown[body] * unit;
            if (across < NAME_FROM) continue;
            String name = sky[body].name();
            double font = Math.max(11, Math.min(18, across / (0.62 * name.length() + 4)));
            double wide = 0.62 * font * name.length() + font;
            String made = across >= MADE_FROM ? madeOf(sky[body]) : "";
            double small = 0.72 * font;
            // Les assemblages ne sont écrits que s'ils tiennent dans la largeur de l'astre.
            if (0.56 * small * made.length() + small > across) made = "";
            double widest = Math.max(wide, made.isEmpty() ? 0 : 0.56 * small * made.length() + small);
            double tall = made.isEmpty() ? 0 : 1.7 * small;
            // Au bas de la clairière ; si elle dépasse de la vue, au bas de ce qu'on en voit, tant que c'est encore sur elle.
            double x = Math.max(Math.min(widest / 2 + 6, w / 2), Math.min(Math.max(w - widest / 2 - 6, w / 2), skyX[body]));
            double y = Math.min(skyY[body] + across / 2 - 1.1 * font - tall, h - 1.4 * font - tall - 16);
            if (y - font < 0 || Math.hypot(x - skyX[body], y - skyY[body]) > across / 2 - 0.5 * font) continue;
            double left = x - widest / 2;
            double top = y - 0.8 * font;
            double right = x + widest / 2;
            double bottom = y + 0.8 * font + tall;
            // La légende, en haut à gauche, compte comme un nom déjà écrit.
            boolean covered = left < legendWidth() + 8 && top < 12 + 16 * legendLines();
            for (int other = 0; other < written && !covered; other += 4) {
                covered = left < labels[other + 2] + 4 && right > labels[other] - 4 && top < labels[other + 3] + 2 && bottom > labels[other + 1] - 2;
            }
            if (covered) continue;
            labels[written++] = left;
            labels[written++] = top;
            labels[written++] = right;
            labels[written++] = bottom;
            g.setFill(VOID);
            g.setGlobalAlpha(0.78);
            g.fillRoundRect(x - wide / 2, y - 0.8 * font, wide, 1.6 * font, 1.6 * font, 1.6 * font);
            if (!made.isEmpty()) {
                double below = 0.56 * small * made.length() + small;
                g.fillRoundRect(x - below / 2, y + 0.95 * font, below, 1.6 * small, 1.6 * small, 1.6 * small);
            }
            g.setGlobalAlpha(1);
            g.setFill(BLOCK_EDGE);
            g.setFont(Font.font(font));
            g.fillText(name, x, y + 0.35 * font);
            if (!made.isEmpty()) {
                g.setFill(TEXT);
                g.setFont(Font.font(small));
                g.fillText(made, x, y + 0.95 * font + 1.15 * small);
            }
        }
        g.setTextAlign(TextAlignment.LEFT);
    }

    /** « Fait de roche granitique, minerai de cuivre et atmosphère étrangère » : les assemblages d'un astre. */
    private String madeOf(Body body) {
        StringBuilder text = new StringBuilder("Fait de ");
        List<String> parts = body.assemblies();
        for (int index = 0; index < parts.size(); index++) {
            if (index > 0) text.append(index == parts.size() - 1 ? " et " : ", ");
            String name = game.assembly(parts.get(index)).name();
            text.append(Character.toLowerCase(name.charAt(0))).append(name, 1, name.length());
        }
        return text.toString();
    }

    /** Largeur du fond de la légende : un peu plus quand elle dit ce que vaut un point. */
    private double legendWidth() {
        return perDot > 1 ? 156 : 130;
    }

    /** Nombre de lignes de la légende : les astres, les assemblages, chaque état qui a des molécules rassemblées, et ce que vaut un point. */
    private int legendLines() {
        int lines = (blocks() > 0 ? 1 : 0) + (sky.length > 0 ? 1 : 0) + (perDot > 1 ? 1 : 0);
        for (int count : gathered) {
            if (count > 0) lines++;
        }
        return lines;
    }

    /**
     * En haut à gauche : le nombre d'astres, celui des assemblages qui ne sont dans aucun astre, puis
     * les états qui ont des molécules rassemblées hors des assemblages et combien, « Liquides : 5 »,
     * avec la couleur de leur liseré. Rien tant qu'il n'y a rien de tout cela.
     */
    private void drawLegend(GraphicsContext g) {
        int formed = blocks();
        int lines = legendLines();
        if (lines == 0) return;
        // Un fond sombre : de près, une molécule peut passer dessous.
        g.setFill(VOID);
        g.setGlobalAlpha(0.72);
        g.fillRoundRect(4, 4, legendWidth(), 8 + 16 * lines, 8, 8);
        g.setGlobalAlpha(1);
        g.setFont(Font.font(11));
        g.setTextAlign(TextAlignment.LEFT);
        double y = 18;
        if (sky.length > 0) {
            // Un petit globe : clair, avec son ombre.
            g.setFill(BLOCK_EDGE);
            g.fillOval(9, y - 9, 10, 10);
            g.setFill(VOID);
            g.setGlobalAlpha(0.5);
            g.fillOval(12, y - 6, 7, 7);
            g.setGlobalAlpha(1);
            g.setFill(TEXT);
            g.fillText(swirled ? "Galaxie : " + sky.length + " astres" : "Astres : " + sky.length, 24, y);
            y += 16;
        }
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
        if (perDot > 1) {
            g.setFill(TEXT);
            g.fillText("1 point = " + perDot + " molécules", 10, y);
        }
    }

    /** Un fond d'étoiles fixes, les mêmes d'une image à l'autre, qui scintillent à peine. */
    private void drawStars(GraphicsContext g, double w, double h) {
        g.setFill(STAR);
        for (int star = 0; star < STARS; star++) {
            double x = STAR_X[star] * w;
            double y = STAR_Y[star] * h;
            double twinkle = 0.25 + 0.2 * Math.sin(time * STAR_PACE[star] + star);
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

    /** Une suite pseudo-aléatoire fixe, tirée une fois : la place d'une étoile et son rythme ne dépendent que de son rang. */
    private static final double[] STAR_X = new double[STARS];
    private static final double[] STAR_Y = new double[STARS];
    private static final double[] STAR_PACE = new double[STARS];

    static {
        for (int star = 0; star < STARS; star++) {
            STAR_X[star] = fraction(star * 12.9898);
            STAR_Y[star] = fraction(star * 78.233);
            STAR_PACE[star] = 0.4 + fraction(star * 3.7);
        }
    }
}
