package idle.ui;

import idle.core.Game;
import idle.core.Molecule;
import java.util.List;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

/**
 * L'espace que l'expansion de la matière a créé, vu de dessus, avec les molécules qui l'occupent.
 *
 * <p>L'espace est un disque dont la surface vaut l'espace créé ({@code GameState.space()}) : son
 * bord recule à mesure que le temps passe. Les molécules s'y rangent en spirale depuis le centre,
 * dans l'ordre de leur création, à l'intérieur du cœur occupé : le disque dont la surface vaut
 * l'espace qu'elles occupent toutes ensemble. Le cœur grossit donc avec les molécules, le bord
 * avec le temps, et l'anneau vide entre les deux est l'espace libre.
 *
 * <p>Le cœur est juste, les dessins ne le sont pas : les molécules s'y partagent la place à parts
 * égales, quelle que soit la taille de chacune. Une molécule lourde n'est donc pas dessinée
 * beaucoup plus grande qu'une légère, et aucun dessin ne dépasse du cœur, donc de l'espace. Des cercles en pointillés marquent les paliers d'espace, ceux qui ouvrent
 * les rayons de molécules ({@link Molecule.Kind#space()}) : le bord de l'espace les franchit un à un.
 *
 * <p>La vue se règle comme une caméra :
 * <ul>
 *   <li>la molette, ou {@link #zoom(double)}, rapproche ou éloigne ;</li>
 *   <li>on ne peut pas reculer plus loin que ce qu'il faut pour voir tout l'espace : plus il y en
 *       a, plus on peut dézoomer ({@link #fit()} y va d'un coup) ;</li>
 *   <li>tirer avec la souris déplace la vue, sans sortir de l'espace.</li>
 * </ul>
 * De loin, une molécule est un point de sa couleur ; de près, on voit ses atomes puis ses liaisons
 * et leurs symboles ({@link MoleculeArt}). Tant que le joueur n'a pas rapproché la vue, elle suit
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
    private static final Color MILESTONE = Color.web("#5b6f8f");
    private static final Color STAR = Color.web("#c9d6ea");
    private static final Color TEXT = Color.web("#8fa3b8");

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
     * Rayon du cœur occupé, en unités de longueur : le disque dont la surface vaut l'espace que les
     * molécules occupent. Jamais plus grand que l'espace lui-même, même si les outils de test y ont
     * mis plus de molécules qu'il n'en contient.
     */
    private double coreRadius() {
        double occupied = Math.min(game.occupiedSpace().toDouble(), 1e30);
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

    /** Nombre de molécules placées dans la vue : pour les vérifications. */
    int placed() {
        return shapes.length;
    }

    /**
     * Range les molécules créées sur la spirale, quand leur liste a changé. Les places sont comptées
     * en parts du cœur occupé, pas en unités d'espace : elles se resserrent ou s'écartent avec lui,
     * et aucune ne peut se retrouver hors de l'espace.
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
        for (int index = 0; index < count; index++) {
            Molecule molecule = game.molecule(log.get(index));
            // La spirale du tournesol : la k-ième place est à la racine de sa part du disque.
            distances[index] = count == 1 ? 0 : Math.sqrt((index + 0.5) / count);
            angles[index] = index * GOLDEN_ANGLE;
            // Une grosse molécule est dessinée un peu plus grande qu'une petite, sans dépasser sa place.
            radii[index] = SMALLEST + (1 - SMALLEST) * Math.min(1, Math.sqrt(molecule.atoms() / (double) BIGGEST_ATOMS));
            shapes[index] = MoleculeArt.of(molecule);
        }
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

        // L'espace créé : un disque un peu plus clair que le vide, et son bord.
        double edge = radius * scale;
        if (edge > 0.5) {
            g.setFill(SPACE);
            g.fillOval(originX - edge, originY - edge, 2 * edge, 2 * edge);
        }
        drawStars(g, w, h);
        // Le cœur occupé : un cercle discret autour des molécules.
        double core = coreRadius() * scale;
        if (core > 4) {
            g.setStroke(CORE);
            g.setLineWidth(1);
            g.strokeOval(originX - core, originY - core, 2 * core, 2 * core);
        }
        drawMilestones(g, originX, originY, w, h);
        if (edge > 0.5) {
            g.setStroke(EDGE);
            g.setLineWidth(1.5);
            g.strokeOval(originX - edge, originY - edge, 2 * edge, 2 * edge);
        }

        // Les molécules, celles qui tombent dans la vue seulement. Elles se partagent le cœur occupé à
        // parts égales : la taille d'un dessin vient de sa place, pas du volume de sa molécule, et le
        // dessin le plus éloigné s'arrête au bord du cœur.
        double heart = coreRadius();
        double cell = shapes.length == 0 ? 0 : CELL_SHARE * heart / Math.sqrt(shapes.length);
        reach = 0;
        double turn = time * DRIFT;
        for (int index = 0; index < shapes.length; index++) {
            double angle = angles[index] + turn;
            double out = distances[index] * (heart - cell);
            reach = Math.max(reach, out + radii[index] * cell);
            double x = originX + out * Math.cos(angle) * scale;
            double y = originY + out * Math.sin(angle) * scale;
            double size = radii[index] * cell * scale;
            if (x + size < 0 || x - size > w || y + size < 0 || y - size > h) continue;
            // Chacune tourne sur elle-même, à son rythme et dans son sens.
            double spin = time * (0.05 + 0.04 * ((index * 7) % 5)) * (index % 2 == 0 ? 1 : -1) + index;
            shapes[index].draw(g, x, y, size, spin);
        }

        g.setFill(TEXT);
        g.setFont(Font.font(11));
        g.setTextAlign(TextAlignment.LEFT);
        double grown = magnification();
        g.fillText(following ? "Tout l'espace" : "Grossi " + (grown >= 10 ? String.valueOf(Math.round(grown))
                : ElementText.number(Math.round(grown * 10) / 10.0)) + " fois", 10, h - 10);
    }

    /**
     * Les paliers d'espace, en pointillés : un cercle par rayon de molécules, là où le bord de
     * l'espace l'a ouvert ou l'ouvrira. Ceux qui sont franchis sont discrets ; le prochain, encore
     * hors de l'espace, est plus marqué et porte son nom.
     */
    private void drawMilestones(GraphicsContext g, double originX, double originY, double w, double h) {
        Molecule.Kind next = game.nextMoleculeKind();
        g.setFont(Font.font(10));
        g.setTextAlign(TextAlignment.LEFT);
        for (Molecule.Kind kind : Molecule.Kind.values()) {
            if (kind.space() <= 0) continue;
            boolean reached = game.isMoleculeKindUnlocked(kind);
            if (!reached && kind != next) continue;       // un seul palier à venir à la fois
            double ring = Math.sqrt(kind.space() / Math.PI) * scale;
            // Trop petit pour se lire, ou si grand qu'il n'est plus qu'une droite hors de la vue.
            if (ring < 12 || ring > 6 * Math.max(w, h)) continue;
            g.setFill(reached ? MILESTONE : EDGE);
            g.setGlobalAlpha(reached ? 0.55 : 0.8);
            int dots = (int) Math.max(24, Math.min(720, 2 * Math.PI * ring / 7));
            for (int dot = 0; dot < dots; dot++) {
                double angle = 2 * Math.PI * dot / dots;
                double x = originX + ring * Math.cos(angle);
                double y = originY + ring * Math.sin(angle);
                if (x < 0 || x > w || y < 0 || y > h) continue;
                g.fillOval(x - 0.8, y - 0.8, 1.6, 1.6);
            }
            g.setGlobalAlpha(1);
            // Le nom du prochain palier, à droite de son cercle, quand ce point est dans la vue. Les paliers
            // franchis n'en portent pas : ils sont au milieu des molécules.
            double labelX = originX + ring * Math.cos(-0.5);
            double labelY = originY + ring * Math.sin(-0.5);
            if (!reached && labelX > 0 && labelX < w - 60 && labelY > 12 && labelY < h - 4) {
                g.setFill(EDGE);
                g.fillText(kind.label(), labelX + 5, labelY);
            }
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
