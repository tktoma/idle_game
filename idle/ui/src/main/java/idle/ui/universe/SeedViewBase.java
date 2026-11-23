package idle.ui.universe;

import idle.core.Game;
import idle.ui.common.ElementText;
import idle.ui.common.Format;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.function.IntConsumer;
import javafx.scene.Cursor;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.text.*;
import idle.core.universe.Skin;
import idle.core.universe.SkinVisual;
import javafx.scene.canvas.*;

/** Une partie de {@link SeedView} (découpée en 0.293 pour rester sous 1 000 lignes) : ses champs, les bulles, le rond de croissance, la météo et la saison. */
public abstract class SeedViewBase extends Canvas {
    // Ce que fournissent les parties suivantes.
    abstract boolean overCan(double x, double y);
    abstract double[] hoseNozzle();
    abstract public boolean grab(double x, double y);
    abstract public void drag(double x, double y);
    abstract public void release();
    abstract void draw();
    abstract void drawBubbleLabel();
    abstract void drawBubbles();
    abstract void drawChrono(GraphicsContext g, double x, double y, double r);
    abstract void drawHose(GraphicsContext g, double s);
    abstract void drawShelfAndCan();
    abstract void paintCan(GraphicsContext g, double x, double y, double s, double degrees, boolean ghost);

    /** Graines dessinées autour de la première, au plus. */
    public static final int MAX_ORBITING = 24;

    static final Color BACK = Color.web("#07060a");

    static final Color HALO = Color.web("#e6d27a");

    static final Color SHELL = Color.web("#b8963c");

    static final Color FLESH = Color.web("#f0dc8c");

    static final Color SEAM = Color.web("#fff6d2");

    static final Color SPROUT = Color.web("#9fe08a");

    static final Color MOTE = Color.web("#f7e7a8");

    static final int MOTES = 60;

    static final int WAVES = 3;

    static final double WAVE_SECONDS = 4.5;

    static final Color WATER = Color.web("#8fd3ff");

    /** Secondes que met une goutte pour tomber sur le germe. */
    static final double DROP_SECONDS = 0.7;

    /** Une goutte qui tombe, puis éclabousse. */
    static final class Drop {
        double age;
        final double drift;

        Drop(double drift) { this.drift = drift; }
    }

    double time;

    double seeds;

    boolean sprouted;

    long waterings;

    /** La longueur montrée du germe : elle rejoint doucement celle que donnent les arrosages. */
    double shownSprout;

    final List<Drop> drops = new ArrayList<>();

    int dropCount;

    /** Le germe devenu brindille : une tige de bois, plus épaisse, et ses rameaux. */
    boolean twig;

    /** L'éclat qui salue la brindille, de 1 à 0. */
    double flash;

    static final Color WOOD = Color.web("#a07a4c");

    static final Color CRACK = Color.web("#4a3412");

    /**
     * Le palier de la graine, de 1 à 11 : graine, tige, graine craquelée, graine brisée, bonsaï,
     * arbuste, jeune arbre, arbre mûr, arbre ancien, arbre géant, arbre-monde. Plus elle grandit, plus la vue
     * s'éloigne pour la montrer en entier.
     */
    int stage = 1;

    /** Le passage d'un palier au suivant : son âge en secondes, négatif hors animation, et le palier d'où il part. */
    double evolveAge = -1;

    int evolveFrom = 1;

    /** Durée du passage : la vue s'éloigne, la graine craque, la plante grandit. */
    public static final double EVOLVE_SECONDS = 3.2;

    /** Ce que la vue garde de sa taille à chaque palier : elle s'éloigne pour que la plante tienne. */
    static final double[] ZOOM = {1, 1, 1, 0.85, 0.62, 0.55, 0.46, 0.39, 0.34, 0.29, 0.25};

    // ------------------------------------------------------------------
    // L'arrosoir
    // ------------------------------------------------------------------

    static final Color CAN = Color.web("#5f93ab");

    static final Color CAN_DARK = Color.web("#3c6478");

    static final Color CAN_LIGHT = Color.web("#a9d4e6");

    static final Color SHELF = Color.web("#8a6440");

    static final Color SHELF_EDGE = Color.web("#5a3f26");

    /** L'inclinaison de l'arrosoir penché, en degrés : le bec descend vers la graine. */
    public static final double CAN_TILT_DEGREES = 62;

    /** Une goutte qui coule du bec : où elle est, sa vitesse, et son âge. */
    static final class Stream {
        double x, y, vx, vy, age;
        /** Où elle touche la plante : au moins un peu sous le bec, pour qu'on la voie tomber. */
        double land;
        boolean splashed;
    }

    // Le tuyau d'arrosage et le chrono.
    static final Color HOSE = Color.web("#3f8f4a");

    static final Color HOSE_LIGHT = Color.web("#7cc985");

    static final Color CHRONO_FACE = Color.web("#14202c");

    static final Color CHRONO_TIME = Color.web("#8fd3ff");

    static final Color BRASS = Color.web("#c9a24a");

    boolean hoseShown, hoseFlowing;

    double hoseTimer;

    final List<Stream> hoseSpray = new ArrayList<>();

    boolean chronoShown, chronoRunning;

    double chronoFill, chronoHand;

    /** Le tuyau d'arrosage : posé dans la vue une fois acheté, et s'il verse en ce moment. */
    public void setHose(boolean shown, boolean flowing) {
        hoseShown = shown;
        hoseFlowing = shown && flowing;
    }

    public boolean isHoseShown() { return hoseShown; }

    /** Les gouttes du tuyau : pour les vérifications. */
    public int hoseDrops() { return hoseSpray.size(); }

    /** Le chrono sur l'étage du haut : là ou non, sa part remplie (0 à 1), et s'il accélère le jeu. */
    public void setChrono(boolean shown, double fill, boolean running) {
        chronoShown = shown;
        chronoFill = Math.max(0, Math.min(1, fill));
        chronoRunning = shown && running;
    }

    public boolean isChronoShown() { return chronoShown; }

    // Les bulles de l'arbre : un arbre d'améliorations posé sur l'arbre, payé avec l'énergie du vide.
    /** L'état d'une bulle, qui décide de son éclat. */
    public enum BubbleState {
        /** Aucune bulle reliée à elle n'est achetée. */
        CLOSED,
        /** Ouverte, mais l'énergie du vide manque. */
        SHORT,
        /** S'achète maintenant : un clic. */
        BUY,
        /** Achetée : elle reste allumée. */
        BOUGHT
    }

    /**
     * Une bulle à dessiner : sa place (son rang dans le jeu, qui donne sa position dans le schéma), son nom, ce qu'elle
     * fait, son prix, son état, et les bulles d'avant auxquelles elle est reliée (leurs rangs dans la liste).
     */
    public record Bubble(int place, String name, String effect, String price, BubbleState state, List<Integer> below) { }

    /** Au-delà, l'effet d'une bulle se coupe en deux lignes. */
    static final int EFFECT_LINE = 56;

    /** L'effet d'une bulle en lignes : aucune s'il n'y en a pas, deux, coupées à un espace, s'il est long. */
    static List<String> effectLines(String effect) {
        if (effect == null || effect.isBlank()) return List.of();
        if (effect.length() <= EFFECT_LINE) return List.of(effect);
        int middle = effect.length() / 2, cut = -1;
        for (int d = 0; d < middle && cut < 0; d++) {
            if (effect.charAt(middle + d) == ' ') cut = middle + d;
            else if (effect.charAt(middle - d) == ' ') cut = middle - d;
        }
        if (cut < 0) return List.of(effect);
        return List.of(effect.substring(0, cut), effect.substring(cut + 1));
    }

    /** La bulle {@code index} telle que la vue la connaît : pour les vérifications. */
    public Bubble bubble(int index) { return bubbles.get(index); }

    static final Color BUBBLE = Color.web("#9fe6ff");

    static final Color BUBBLE_DEEP = Color.web("#3a8fb8");

    List<Bubble> bubbles = List.of();

    double[] bubbleX = new double[0], bubbleY = new double[0];

    double bubbleR;

    int hovered = -1;

    IntConsumer onBubble = index -> { };

    /** Ce que le dernier dessin a vu : la graine (centre, taille) et le feuillage (centre, rayon ; 0 sans arbre). */
    double seedX, seedY, seedSize, crownX, crownY, crownR;

    /** Les bulles de l'arbre, dans l'ordre où l'application les connaît. */
    public void setBubbles(List<Bubble> shown) {
        bubbles = List.copyOf(shown);
        if (hovered >= bubbles.size()) hovered = -1;
    }

    /** Ce que fait un clic sur une bulle : son rang dans la liste donnée. */
    public void setOnBubble(IntConsumer action) {
        onBubble = action == null ? index -> { } : action;
    }

    public int bubbleCount() { return bubbles.size(); }

    /** La bulle sous ce point (son rang), ou -1. */
    public int bubbleAt(double x, double y) {
        for (int i = 0; i < Math.min(bubbles.size(), bubbleX.length); i++) {
            if (Math.hypot(x - bubbleX[i], y - bubbleY[i]) <= bubbleR * 1.15) return i;
        }
        return -1;
    }

    /** Où est la bulle {@code index} au dernier dessin : pour les vérifications. */
    public double[] bubblePoint(int index) {
        return new double[] {bubbleX[index], bubbleY[index]};
    }

    /** La bulle survolée, dont l'étiquette s'affiche ; -1 sinon. */
    public int hoveredBubble() { return hovered; }

    /** Survole ce point : l'étiquette de la bulle qui s'y trouve s'affiche. */
    public void hover(double x, double y) { hovered = bubbleAt(x, y); }

    /** Un clic à ce point : vrai s'il tombe sur une bulle, qui est alors achetée si elle peut l'être. */
    public boolean clickBubble(double x, double y) {
        int hit = bubbleAt(x, y);
        if (hit < 0) return false;
        hovered = hit;
        onBubble.accept(hit);
        return true;
    }

    // Le rond de croissance.
    static final Color BLOOM = Color.web("#9fe08a");

    static final Color BLOOM_LIGHT = Color.web("#e4ffd6");

    boolean bloomShown;

    /** Où il est, de 0 à 1 sur chaque axe de la vue ; ce qu'il lui reste à attendre, sur ce qu'il attend en tout. */
    double bloomX, bloomY, bloomLeft, bloomTotal = 1;

    /** Son effet : ce qu'il lui reste, en secondes, et ce qu'il ajoute à l'arrosage (0,45 pour +45 %). */
    double bloomActive, bloomGain;

    Runnable onBloom = () -> { };

    /** Le rond de croissance à cueillir : là ou non, où (0 à 1 sur chaque axe), et ce qu'il lui reste sur {@code total} secondes. */
    public void setBloom(boolean shown, double x, double y, double left, double total) {
        bloomShown = shown;
        bloomX = Math.max(0, Math.min(1, x));
        bloomY = Math.max(0, Math.min(1, y));
        bloomLeft = Math.max(0, left);
        bloomTotal = Math.max(1e-9, total);
    }

    /** L'effet du rond cueilli : ce qu'il lui reste en secondes (0 sans effet), et ce qu'il ajoute à l'arrosage. */
    public void setBloomEffect(double left, double gain) {
        bloomActive = Math.max(0, left);
        bloomGain = Math.max(0, gain);
    }

    /** Ce que fait un clic sur le rond de croissance : le cueillir. */
    public void setOnBloom(Runnable action) {
        onBloom = action == null ? () -> { } : action;
    }

    public boolean isBloomShown() { return bloomShown; }

    /** Le rayon du rond, sans son battement : à la mesure de la vue. */
    public double bloomRadius() { return Math.max(14, Math.min(getWidth(), getHeight()) * 0.05); }

    /** Où est le rond dans la vue, en points : pour les vérifications. */
    public double[] bloomPoint() {
        return new double[] {bloomX * getWidth(), bloomY * getHeight()};
    }

    /** Vrai si ce point est sur le rond de croissance (un peu plus large que lui : il bat). */
    public boolean overBloom(double x, double y) {
        double[] at = bloomPoint();
        return bloomShown && Math.hypot(x - at[0], y - at[1]) <= bloomRadius() * 1.25;
    }

    /** Un clic à ce point : vrai s'il tombe sur le rond de croissance, qui est alors cueilli. */
    public boolean clickBloom(double x, double y) {
        if (!overBloom(x, y)) return false;
        onBloom.run();
        return true;
    }

    /** La ligne de l'effet, au-dessus de l'étagère : « Croissance +45 % : 23 s » ; rien sans effet. */
    public String bloomText() {
        return bloomActive <= 0 ? "" : "Croissance +" + Math.round(bloomGain * 100) + " % : " + (long) Math.ceil(bloomActive - 1e-9) + " s";
    }

    /** Le rond à cueillir : un halo qui bat, le rond vert, l'anneau qui se vide avec le temps, et ses secondes. */
    void drawBloom() {
        if (!bloomShown || getWidth() <= 0 || getHeight() <= 0) return;
        GraphicsContext g = getGraphicsContext2D();
        double[] at = bloomPoint();
        double beat = 0.5 + 0.5 * Math.sin(time * 5), r = bloomRadius() * (1 + 0.12 * beat);
        double ring = r + 6, share = Math.min(1, bloomLeft / bloomTotal);
        Skin ringSkin = skins.get(SkinVisual.GROWTH_RING);
        if (ringSkin != null) SeedSkinArt.bloom(g, at[0], at[1], r, share, time, ringSkin);
        else {
            UniverseView.glow(g, at[0], at[1], r * (2.2 + 0.6 * beat), BLOOM, 0.5 + 0.3 * beat);
            g.setGlobalAlpha(1);
            g.setFill(new RadialGradient(0, 0, at[0] - r * 0.3, at[1] - r * 0.3, r * 1.3, false,
                    CycleMethod.NO_CYCLE, new Stop(0, BLOOM_LIGHT), new Stop(1, BLOOM)));
            g.fillOval(at[0] - r, at[1] - r, 2 * r, 2 * r);
            g.setStroke(BLOOM);
            g.setLineWidth(1.5);
            g.setGlobalAlpha(0.3);
            g.strokeOval(at[0] - ring, at[1] - ring, ring * 2, ring * 2);
            g.setGlobalAlpha(0.95);
            g.setLineWidth(3);
            g.strokeArc(at[0] - ring, at[1] - ring, ring * 2, ring * 2, 90, 360 * share, ArcType.OPEN);
        }
        g.setGlobalAlpha(1);
        g.setFill(Color.WHITE);
        g.setFont(Font.font(12));
        g.setTextAlign(TextAlignment.CENTER);
        g.fillText((long) Math.ceil(bloomLeft - 1e-9) + " s", at[0], at[1] + ring + 14);
        g.setGlobalAlpha(1);
    }

    // L'essence de l'arbre.
    /** Le skin choisi pour l'arbre, {@code null} pour le Classique : SeedTree en tire l'essence. */
    Skin skin;

    /** L'essence de l'arbre : le skin choisi pour le visuel de l'arbre, {@code null} pour le Classique. */
    public void setSkin(Skin skin) { this.skin = skin; }

    // L'établi de la graine : la graine, l'arrosoir, l'étagère, le tuyau, le chrono, le rond, la météo, les saisons.
    /** Les skins choisis par visuel ; absent pour le Classique. {@link SeedSkinArt} et {@link ToolsSkinArt} les dessinent. */
    final EnumMap<SkinVisual, Skin> skins = new EnumMap<>(SkinVisual.class);

    /** Les visuels de l'établi, ceux que {@link #setVisualSkin} habille. */
    public static final SkinVisual[] ESTABLISHMENT = {SkinVisual.SEED, SkinVisual.WATERING_CAN, SkinVisual.SHELF, SkinVisual.HOSE,
        SkinVisual.CHRONO, SkinVisual.GROWTH_RING, SkinVisual.WEATHER, SkinVisual.SEASONS};

    /**
     * Les visuels de l'arbre au-delà de son essence et de ses bulles, que {@link #setVisualSkin} habille
     * aussi : la saison figée, la fantaisie, le sol ({@link TreeSkinArt}), les bulles et leurs liens ({@link TreeBubbleSkinArt}).
     */
    public static final SkinVisual[] TREE_LOOKS = {SkinVisual.TREE_SEASON, SkinVisual.TREE_FANTASY, SkinVisual.TREE_GROUND,
        SkinVisual.TREE_BUBBLES, SkinVisual.TREE_BUBBLE_LINKS};

    /** Le skin choisi pour un visuel de l'établi ou de l'arbre, {@code null} pour le Classique. */
    public void setVisualSkin(SkinVisual visual, Skin skin) {
        if (skin == null) skins.remove(visual);
        else skins.put(visual, skin);
    }

    /** Le skin choisi pour ce visuel, {@code null} pour le Classique. */
    public Skin visualSkin(SkinVisual visual) { return skins.get(visual); }

    // La météo.
    static final Color SUN = Color.web("#ffd27f");

    static final Color CLOUD = Color.web("#c8d0e0");

    static final Color STORM_CLOUD = Color.web("#59607a");

    static final Color RAIN = Color.web("#8fc9ff");

    /** Le temps qu'il fait, {@code null} tant que la météo n'est pas là ; ce qu'il lui reste ; son effet sur l'arrosage (1,3 pour +30 %). */
    Game.Weather weather;

    double weatherLeft, weatherFactor = 1;

    /** La météo de la graine : le temps qu'il fait ({@code null} sans météo), ce qu'il lui reste en secondes, et son effet. */
    public void setWeather(Game.Weather weather, double left, double factor) {
        this.weather = weather;
        weatherLeft = Math.max(0, left);
        weatherFactor = factor;
    }

    /** La ligne de la météo, sous le ciel : « Beau temps +30 % : 32 s », « Orage −10 % : 12 s », « Variable : 45 s » ; rien sans météo. */
    public String weatherText() {
        if (weather == null) return "";
        long percent = Math.round((weatherFactor - 1) * 100);
        String effect = percent > 0 ? " +" + percent + " %" : percent < 0 ? " −" + (-percent) + " %" : "";
        return weather.label() + effect + " : " + (long) Math.ceil(weatherLeft - 1e-9) + " s";
    }

    /**
     * Le ciel, en haut à gauche : le soleil, voilé ou non, ou le nuage d'orage et sa pluie, avec sa ligne dessous ; et ce
     * qu'il fait sur toute la vue, la lumière chaude du beau temps ou la pluie de l'orage.
     */
    void drawWeather() {
        if (weather == null || getWidth() <= 0 || getHeight() <= 0) return;
        GraphicsContext g = getGraphicsContext2D();
        double w = getWidth(), h = getHeight(), r = Math.max(12, Math.min(w, h) * 0.04), x = r * 2.4, y = r * 2.4;
        Skin weatherSkin = skins.get(SkinVisual.WEATHER);
        if (weatherSkin != null) SeedSkinArt.weather(g, w, h, x, y, r, weather, time, weatherSkin);
        else switch (weather) {
            case SUN -> {
                UniverseView.glow(g, x, y, Math.min(w, h) * 0.5, SUN, 0.12);
                sun(g, x, y, r);
            }
            case VARIABLE -> {
                sun(g, x - r * 0.4, y - r * 0.3, r * 0.85);
                cloud(g, x + r * 0.3, y + r * 0.35, r, CLOUD, 0.9);
            }
            case STORM -> {
                // La pluie sur toute la vue, qui tombe en biais.
                g.setStroke(RAIN);
                g.setLineWidth(1);
                g.setGlobalAlpha(0.28);
                for (int k = 0; k < 60; k++) {
                    double fall = (time * 0.9 + UniverseView.noise(k * 3 + 1)) % 1;
                    double rx = UniverseView.noise(k * 3 + 2) * (w + h * 0.2) - h * 0.1 * fall, ry = fall * h;
                    g.strokeLine(rx, ry, rx - 4, ry + 14);
                }
                cloud(g, x, y, r * 1.1, STORM_CLOUD, 1);
                g.setStroke(RAIN);
                g.setLineWidth(1.5);
                g.setGlobalAlpha(0.8);
                for (int k = 0; k < 5; k++) {
                    double fall = (time * 1.6 + k / 5.0) % 1, rx = x - r + k * r * 0.5;
                    g.strokeLine(rx, y + r * 0.8 + fall * r * 1.4, rx - 2, y + r * 1.2 + fall * r * 1.4);
                }
                // Un éclair, de temps en temps.
                if ((time * 0.7) % 1 < 0.05) {
                    g.setStroke(Color.WHITE);
                    g.setLineWidth(2);
                    g.setGlobalAlpha(0.95);
                    g.strokePolyline(new double[] {x + r * 0.2, x - r * 0.2, x + r * 0.15, x - r * 0.3},
                            new double[] {y + r * 0.6, y + r * 1.2, y + r * 1.3, y + r * 2}, 4);
                }
            }
        }
        g.setGlobalAlpha(1);
        g.setFill(weather == Game.Weather.SUN ? SUN : weather == Game.Weather.STORM ? RAIN : CLOUD);
        g.setFont(Font.font(12));
        g.setTextAlign(TextAlignment.LEFT);
        g.fillText(weatherText(), x - r * 1.6, y + r * 2.6);
    }

    /** Un soleil : un disque et ses rayons, qui tournent doucement. */
    void sun(GraphicsContext g, double x, double y, double r) {
        UniverseView.glow(g, x, y, r * 2.4, SUN, 0.6);
        g.setStroke(SUN);
        g.setLineWidth(2);
        g.setGlobalAlpha(0.85);
        for (int k = 0; k < 8; k++) {
            double a = time * 0.3 + Math.PI * k / 4;
            g.strokeLine(x + Math.cos(a) * r * 1.25, y + Math.sin(a) * r * 1.25, x + Math.cos(a) * r * 1.7, y + Math.sin(a) * r * 1.7);
        }
        g.setGlobalAlpha(1);
        g.setFill(SUN);
        g.fillOval(x - r, y - r, r * 2, r * 2);
    }

    /** Un nuage : trois bosses et une base, de cette couleur. */
    static void cloud(GraphicsContext g, double x, double y, double r, Color color, double alpha) {
        g.setGlobalAlpha(alpha);
        g.setFill(color);
        g.fillOval(x - r * 1.3, y - r * 0.3, r * 1.3, r);
        g.fillOval(x - r * 0.7, y - r * 0.8, r * 1.4, r * 1.4);
        g.fillOval(x + r * 0.1, y - r * 0.4, r * 1.2, r * 1.05);
        g.fillRoundRect(x - r * 1.3, y, r * 2.6, r * 0.6, r * 0.6, r * 0.6);
        g.setGlobalAlpha(1);
    }

    // Les saisons.
    /** Une couleur par saison : vert tendre, or, roux, bleu froid. */
    static final Color[] SEASON_COLORS = {Color.web("#9fe08a"), Color.web("#ffd27f"), Color.web("#ffb36b"), Color.web("#a8d8ff")};

    /** La saison, {@code null} tant qu'elles ne sont pas là ; ce qu'il lui reste ; ce qu'un arrosage verse au chrono. */
    Game.Season season;

    double seasonLeft, seasonFill;

    /** Les saisons de la graine : la saison ({@code null} sans elles), ce qu'il lui reste en secondes, ce qu'un arrosage verse au chrono. */
    public void setSeason(Game.Season season, double left, double fillPerWatering) {
        this.season = season;
        seasonLeft = Math.max(0, left);
        seasonFill = Math.max(0, fillPerWatering);
    }

    /** La ligne de la saison : « Printemps ×1,5 : 92 s · +0.3 s au chrono par arrosage » ; rien sans saisons. */
    public String seasonText() {
        if (season == null) return "";
        return season.label() + " " + Format.multiplier(season.fill()) + " : " + (long) Math.ceil(seasonLeft - 1e-9) + " s"
                + (seasonFill > 0 ? " · +" + ElementText.number(seasonFill) + " s au chrono par arrosage" : "");
    }

    boolean canUnlocked;

    /** Où est l'arrosoir (son centre, en points de la vue) ; NaN : sur l'étagère, avant le premier dessin. */
    double canX = Double.NaN, canY = Double.NaN;

    boolean dragging;

    double grabX, grabY;

    /** De 0 (droit) à 1 (penché) : il suit en douceur. */
    double tilt;

    boolean overPlant;

    double streamTimer;

    final List<Stream> stream = new ArrayList<>();

    /** Ce que le dernier dessin a vu de la plante : son axe, sa demi-largeur, son sommet et son pied. */
    double plantX, plantHalf, plantTop, plantBottom;

    public SeedViewBase() {
        super(800, 400);
        setOnMousePressed(event -> {
            // Le rond de croissance d'abord, puis une bulle : sinon, l'arrosoir.
            if (clickBloom(event.getX(), event.getY()) || clickBubble(event.getX(), event.getY()) || grab(event.getX(), event.getY())) event.consume();
        });
        setOnMouseDragged(event -> {
            if (dragging) {
                drag(event.getX(), event.getY());
                event.consume();
            }
        });
        setOnMouseReleased(event -> release());
        setOnMouseMoved(event -> {
            hover(event.getX(), event.getY());
            boolean buy = hovered >= 0 && bubbles.get(hovered).state() == BubbleState.BUY;
            setCursor(buy || overBloom(event.getX(), event.getY()) ? Cursor.HAND : canUnlocked && overCan(event.getX(), event.getY())
                    ? Cursor.OPEN_HAND : Cursor.DEFAULT);
        });
        setOnMouseExited(event -> hovered = -1);
    }

    /** L'arrosoir est-il débloqué : sinon, l'étagère attend, avec son bouton. */
    public void setCanUnlocked(boolean unlocked) {
        canUnlocked = unlocked;
        if (!unlocked) {
            dragging = false;
            tilt = 0;
        }
    }

    static double ease(double t) {
        double x = Math.max(0, Math.min(1, t));
        return x * x * (3 - 2 * x);
    }

    /**
     * Le tuyau d'arrosage : il vient du bord gauche, loin de l'étagère, court au sol et finit par sa lance, au pied de
     * la plante.
     */
    /**
     * Le schéma des bulles, repris des dessins des paliers 7 à 11 et rendu symétrique : pour chaque place
     * (le rang de la bulle dans le jeu), sa position en unités du dessin, la base à l'origine, vers la droite et vers le
     * haut. Les racines sont sous la base.
     */
    static final double[][] SCHEMA = {
            {0, 0},
            {-130, 182}, {130, 182},
            {-268, 390}, {0, 390}, {268, 390},
            {-156, 567}, {156, 567},
            {-436, 480}, {436, 480},
            {-336, 638}, {336, 638},
            {-626, 582}, {-500, 295}, {626, 582}, {500, 295},
            {0, -158}, {0, 574}, {0, 725}};

    /** La place des racines dans le schéma : la bulle tout en bas, sous la graine, à l'aura dorée. */
    static final int ROOTS_PLACE = 16;

    static final Color ROOTS_GOLD = Color.web("#ffd27f");

    /** Un arrosoir de fer peint, centré en ({@code x}, {@code y}), penché de {@code degrees} : seulement sa silhouette si {@code ghost}. */
    static void drawCan(GraphicsContext g, double x, double y, double s, double degrees, boolean ghost) {
        g.save();
        g.translate(x, y);
        g.rotate(degrees);
        // L'anse, à l'arrière : elle part du rebord, monte en boucle et redescend se fixer au dos du corps. Dessinée avant
        // le corps, pour que ses deux bouts s'y enfoncent.
        g.setStroke(ghost ? MOTE : CAN_DARK);
        g.setLineWidth(Math.max(2, s * 0.09));
        g.setLineCap(StrokeLineCap.ROUND);
        g.beginPath();
        g.moveTo(-s * 0.1, -s * 0.3);
        g.bezierCurveTo(-s * 0.15, -s * 0.95, -s * 0.95, -s * 0.85, -s * 0.42, s * 0.08);
        g.stroke();
        // Le bec, du bas du corps vers le haut à droite, et sa pomme.
        g.setFill(ghost ? MOTE : CAN_DARK);
        g.fillPolygon(new double[] {s * 0.3, s * 0.42, s * 1.0, s * 0.9},
                new double[] {s * 0.18, s * 0.02, -s * 0.36, -s * 0.46}, 4);
        g.save();
        g.translate(s * 0.97, -s * 0.43);
        g.rotate(-40);
        g.setFill(ghost ? MOTE : CAN);
        g.fillOval(-s * 0.06, -s * 0.13, s * 0.12, s * 0.26);
        g.restore();
        // Le corps, un peu plus large en bas, et son rebord.
        g.setFill(ghost ? MOTE : CAN);
        g.fillPolygon(new double[] {-s * 0.4, s * 0.4, s * 0.46, -s * 0.46}, new double[] {-s * 0.3, -s * 0.3, s * 0.38, s * 0.38}, 4);
        g.fillOval(-s * 0.46, s * 0.3, s * 0.92, s * 0.16);
        if (!ghost) {
            g.setFill(CAN_DARK);
            g.fillOval(-s * 0.4, -s * 0.36, s * 0.8, s * 0.14);
            // Un reflet le long du flanc.
            g.setFill(CAN_LIGHT);
            g.setGlobalAlpha(g.getGlobalAlpha() * 0.6);
            g.fillPolygon(new double[] {-s * 0.3, -s * 0.2, -s * 0.24, -s * 0.36}, new double[] {-s * 0.22, -s * 0.22, s * 0.3, s * 0.3}, 4);
            g.setGlobalAlpha(1);
        }
        g.restore();
    }

    /**
     * Une amande, pointe en haut, tournée de {@code turn} radians : remplie, ou seulement son contour.
     * Calculée point par point, sans rotation du dessin.
     */
    static void seedPath(GraphicsContext g, double cx, double cy, double size, double turn, boolean fill) {
        int n = 32;
        // Un point de plus, le premier répété, pour que le contour se referme.
        double[] xs = new double[n + 1], ys = new double[n + 1];
        double cos = Math.cos(turn), sin = Math.sin(turn);
        for (int i = 0; i < n; i++) {
            double t = 2 * Math.PI * i / n;
            double x = size * 0.62 * Math.sin(t) * (0.7 - 0.3 * Math.cos(t));
            double y = -size * Math.cos(t);
            xs[i] = cx + x * cos - y * sin;
            ys[i] = cy + x * sin + y * cos;
        }
        xs[n] = xs[0];
        ys[n] = ys[0];
        if (fill) g.fillPolygon(xs, ys, n);
        else g.strokePolyline(xs, ys, n + 1);
    }
}
