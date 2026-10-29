package idle.ui.universe;

import idle.core.Game;
import idle.ui.common.CanvasPane;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * La graine d'univers (version 0.083), le visuel de son sous-onglet : une graine en amande, dorée,
 * qui respire au centre d'un halo, des ondes qui partent d'elle, et une poussière qui monte. Les
 * graines de plus tournent autour d'elle, plus petites, jusqu'à {@link #MAX_ORBITING}.
 *
 * <p>Version 0.103 : la graine ne germe plus seule. Le bouton de la sous-page la fait germer une fois
 * l'objectif de la dimension 1 atteint ; ensuite, chaque clic fait tomber une goutte d'eau sur le germe,
 * qui grandit et prend des feuilles.
 *
 * <p>Version 0.137 : plus de bouton, un arrosoir. Il attend sur une étagère de bois, en bas à droite ; un bouton posé sur
 * l'étagère le débloque. On le prend à la souris et on le glisse sur la graine : il se penche, l'eau coule du bec, et
 * le jeu compte un arrosage par seconde ({@code Game#setWatering}). Relâché, il revient tout seul sur l'étagère.
 *
 * <p>Version 0.153 : avec la boucle, le rond de croissance. De temps en temps, un rond vert pousse quelque part dans la
 * vue et bat doucement, son anneau et ses secondes comptant ce qu'il reste à le cueillir ; un clic le cueille. Pendant
 * son effet, l'arrosage va plus vite, et une ligne au-dessus de l'étagère le dit : « Croissance +45 % : 23 s ».
 *
 * <p>Version 0.155 : avec le clicker, la météo. En haut à gauche, le ciel de la graine : un soleil qui rayonne par beau
 * temps, un soleil voilé d'un nuage par temps variable, un nuage sombre qui pleut et s'éclaire d'un éclair par orage ;
 * dessous, son effet sur l'arrosage et le temps qu'il lui reste (« Beau temps +30 % : 32 s »). Par beau temps, une
 * lumière chaude tombe sur la vue ; par orage, la pluie la traverse.
 */
public final class SeedView extends Canvas {

    /** Graines dessinées autour de la première, au plus. */
    public static final int MAX_ORBITING = 24;

    private static final Color BACK = Color.web("#07060a");
    private static final Color HALO = Color.web("#e6d27a");
    private static final Color SHELL = Color.web("#b8963c");
    private static final Color FLESH = Color.web("#f0dc8c");
    private static final Color SEAM = Color.web("#fff6d2");
    private static final Color SPROUT = Color.web("#9fe08a");
    private static final Color MOTE = Color.web("#f7e7a8");
    private static final int MOTES = 60;
    private static final int WAVES = 3;
    private static final double WAVE_SECONDS = 4.5;

    private static final Color WATER = Color.web("#8fd3ff");
    /** Secondes que met une goutte pour tomber sur le germe. */
    private static final double DROP_SECONDS = 0.7;

    /** Une goutte qui tombe, puis éclabousse. */
    private static final class Drop {
        double age;
        final double drift;

        Drop(double drift) {
            this.drift = drift;
        }
    }

    private double time;
    private double seeds;
    private boolean sprouted;
    private long waterings;
    /** La longueur montrée du germe : elle rejoint doucement celle que donnent les arrosages. */
    private double shownSprout;
    private final java.util.List<Drop> drops = new java.util.ArrayList<>();
    private int dropCount;
    /** Le germe devenu brindille (version 0.106) : une tige de bois, plus épaisse, et ses rameaux. */
    private boolean twig;
    /** L'éclat qui salue la brindille, de 1 à 0. */
    private double flash;
    private static final Color WOOD = Color.web("#a07a4c");
    private static final Color CRACK = Color.web("#4a3412");

    /**
     * Le palier de la graine (version 0.120), de 1 à 11 : graine, tige, graine craquelée, graine brisée, bonsaï,
     * arbuste, jeune arbre, arbre mûr, arbre ancien, arbre géant, arbre-monde. Plus elle grandit, plus la vue
     * s'éloigne pour la montrer en entier.
     */
    private int stage = 1;
    /** Le passage d'un palier au suivant : son âge en secondes, négatif hors animation, et le palier d'où il part. */
    private double evolveAge = -1;
    private int evolveFrom = 1;
    /** Durée du passage : la vue s'éloigne, la graine craque, la plante grandit. */
    public static final double EVOLVE_SECONDS = 3.2;
    /** Ce que la vue garde de sa taille à chaque palier : elle s'éloigne pour que la plante tienne. */
    private static final double[] ZOOM = {1, 1, 1, 0.85, 0.62, 0.55, 0.46, 0.39, 0.34, 0.29, 0.25};

    // ------------------------------------------------------------------
    // L'arrosoir (version 0.137)
    // ------------------------------------------------------------------

    private static final Color CAN = Color.web("#5f93ab");
    private static final Color CAN_DARK = Color.web("#3c6478");
    private static final Color CAN_LIGHT = Color.web("#a9d4e6");
    private static final Color SHELF = Color.web("#8a6440");
    private static final Color SHELF_EDGE = Color.web("#5a3f26");
    /** L'inclinaison de l'arrosoir penché, en degrés : le bec descend vers la graine. */
    public static final double CAN_TILT_DEGREES = 62;

    /** Une goutte qui coule du bec : où elle est, sa vitesse, et son âge. */
    private static final class Stream {
        double x, y, vx, vy, age;
        /** Où elle touche la plante : au moins un peu sous le bec, pour qu'on la voie tomber. */
        double land;
        boolean splashed;
    }

    // Le tuyau d'arrosage et le chrono (version 0.139).
    private static final Color HOSE = Color.web("#3f8f4a");
    private static final Color HOSE_LIGHT = Color.web("#7cc985");
    private static final Color CHRONO_FACE = Color.web("#14202c");
    private static final Color CHRONO_TIME = Color.web("#8fd3ff");
    private static final Color BRASS = Color.web("#c9a24a");
    private boolean hoseShown, hoseFlowing;
    private double hoseTimer;
    private final java.util.List<Stream> hoseSpray = new java.util.ArrayList<>();
    private boolean chronoShown, chronoRunning;
    private double chronoFill, chronoHand;

    /** Le tuyau d'arrosage : posé dans la vue une fois acheté, et s'il verse en ce moment. */
    public void setHose(boolean shown, boolean flowing) {
        hoseShown = shown;
        hoseFlowing = shown && flowing;
    }

    public boolean isHoseShown() {
        return hoseShown;
    }

    /** Les gouttes du tuyau : pour les vérifications. */
    public int hoseDrops() {
        return hoseSpray.size();
    }

    /** Le chrono sur l'étage du haut : là ou non, sa part remplie (0 à 1), et s'il accélère le jeu. */
    public void setChrono(boolean shown, double fill, boolean running) {
        chronoShown = shown;
        chronoFill = Math.max(0, Math.min(1, fill));
        chronoRunning = shown && running;
    }

    public boolean isChronoShown() {
        return chronoShown;
    }

    // Les bulles de l'arbre (version 0.142) : un arbre d'améliorations posé sur l'arbre, payé avec l'énergie du vide.
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
     * fait (version 0.148), son prix, son état, et les bulles d'avant auxquelles elle est reliée (leurs rangs dans la liste).
     */
    public record Bubble(int place, String name, String effect, String price, BubbleState state, java.util.List<Integer> below) { }

    /** Au-delà, l'effet d'une bulle se coupe en deux lignes. */
    private static final int EFFECT_LINE = 56;

    /** L'effet d'une bulle en lignes : aucune s'il n'y en a pas, deux, coupées à un espace, s'il est long. */
    static java.util.List<String> effectLines(String effect) {
        if (effect == null || effect.isBlank()) return java.util.List.of();
        if (effect.length() <= EFFECT_LINE) return java.util.List.of(effect);
        int middle = effect.length() / 2, cut = -1;
        for (int d = 0; d < middle && cut < 0; d++) {
            if (effect.charAt(middle + d) == ' ') cut = middle + d;
            else if (effect.charAt(middle - d) == ' ') cut = middle - d;
        }
        if (cut < 0) return java.util.List.of(effect);
        return java.util.List.of(effect.substring(0, cut), effect.substring(cut + 1));
    }

    /** La bulle {@code index} telle que la vue la connaît : pour les vérifications. */
    public Bubble bubble(int index) {
        return bubbles.get(index);
    }

    private static final Color BUBBLE = Color.web("#9fe6ff");
    private static final Color BUBBLE_DEEP = Color.web("#3a8fb8");
    private java.util.List<Bubble> bubbles = java.util.List.of();
    private double[] bubbleX = new double[0], bubbleY = new double[0];
    private double bubbleR;
    private int hovered = -1;
    private java.util.function.IntConsumer onBubble = index -> { };
    /** Ce que le dernier dessin a vu : la graine (centre, taille) et le feuillage (centre, rayon ; 0 sans arbre). */
    private double seedX, seedY, seedSize, crownX, crownY, crownR;

    /** Les bulles de l'arbre, dans l'ordre où l'application les connaît. */
    public void setBubbles(java.util.List<Bubble> shown) {
        bubbles = java.util.List.copyOf(shown);
        if (hovered >= bubbles.size()) hovered = -1;
    }

    /** Ce que fait un clic sur une bulle : son rang dans la liste donnée. */
    public void setOnBubble(java.util.function.IntConsumer action) {
        onBubble = action == null ? index -> { } : action;
    }

    public int bubbleCount() {
        return bubbles.size();
    }

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
    public int hoveredBubble() {
        return hovered;
    }

    /** Survole ce point : l'étiquette de la bulle qui s'y trouve s'affiche. */
    public void hover(double x, double y) {
        hovered = bubbleAt(x, y);
    }

    /** Un clic à ce point : vrai s'il tombe sur une bulle, qui est alors achetée si elle peut l'être. */
    public boolean clickBubble(double x, double y) {
        int hit = bubbleAt(x, y);
        if (hit < 0) return false;
        hovered = hit;
        onBubble.accept(hit);
        return true;
    }

    // Le rond de croissance (version 0.153).
    private static final Color BLOOM = Color.web("#9fe08a");
    private static final Color BLOOM_LIGHT = Color.web("#e4ffd6");
    private boolean bloomShown;
    /** Où il est, de 0 à 1 sur chaque axe de la vue ; ce qu'il lui reste à attendre, sur ce qu'il attend en tout. */
    private double bloomX, bloomY, bloomLeft, bloomTotal = 1;
    /** Son effet : ce qu'il lui reste, en secondes, et ce qu'il ajoute à l'arrosage (0,45 pour +45 %). */
    private double bloomActive, bloomGain;
    private Runnable onBloom = () -> { };

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

    public boolean isBloomShown() {
        return bloomShown;
    }

    /** Le rayon du rond, sans son battement : à la mesure de la vue. */
    public double bloomRadius() {
        return Math.max(14, Math.min(getWidth(), getHeight()) * 0.05);
    }

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
    private void drawBloom() {
        if (!bloomShown || getWidth() <= 0 || getHeight() <= 0) return;
        GraphicsContext g = getGraphicsContext2D();
        double[] at = bloomPoint();
        double beat = 0.5 + 0.5 * Math.sin(time * 5), r = bloomRadius() * (1 + 0.12 * beat);
        UniverseView.glow(g, at[0], at[1], r * (2.2 + 0.6 * beat), BLOOM, 0.5 + 0.3 * beat);
        g.setGlobalAlpha(1);
        g.setFill(new javafx.scene.paint.RadialGradient(0, 0, at[0] - r * 0.3, at[1] - r * 0.3, r * 1.3, false,
                javafx.scene.paint.CycleMethod.NO_CYCLE, new javafx.scene.paint.Stop(0, BLOOM_LIGHT), new javafx.scene.paint.Stop(1, BLOOM)));
        g.fillOval(at[0] - r, at[1] - r, 2 * r, 2 * r);
        double ring = r + 6, share = Math.min(1, bloomLeft / bloomTotal);
        g.setStroke(BLOOM);
        g.setLineWidth(1.5);
        g.setGlobalAlpha(0.3);
        g.strokeOval(at[0] - ring, at[1] - ring, ring * 2, ring * 2);
        g.setGlobalAlpha(0.95);
        g.setLineWidth(3);
        g.strokeArc(at[0] - ring, at[1] - ring, ring * 2, ring * 2, 90, 360 * share, javafx.scene.shape.ArcType.OPEN);
        g.setFill(Color.WHITE);
        g.setFont(javafx.scene.text.Font.font(12));
        g.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
        g.fillText((long) Math.ceil(bloomLeft - 1e-9) + " s", at[0], at[1] + ring + 14);
        g.setGlobalAlpha(1);
    }

    // La météo (version 0.155).
    private static final Color SUN = Color.web("#ffd27f");
    private static final Color CLOUD = Color.web("#c8d0e0");
    private static final Color STORM_CLOUD = Color.web("#59607a");
    private static final Color RAIN = Color.web("#8fc9ff");
    /** Le temps qu'il fait, {@code null} tant que la météo n'est pas là ; ce qu'il lui reste ; son effet sur l'arrosage (1,3 pour +30 %). */
    private Game.Weather weather;
    private double weatherLeft, weatherFactor = 1;

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
    private void drawWeather() {
        if (weather == null || getWidth() <= 0 || getHeight() <= 0) return;
        GraphicsContext g = getGraphicsContext2D();
        double w = getWidth(), h = getHeight(), r = Math.max(12, Math.min(w, h) * 0.04), x = r * 2.4, y = r * 2.4;
        switch (weather) {
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
        g.setFont(javafx.scene.text.Font.font(12));
        g.setTextAlign(javafx.scene.text.TextAlignment.LEFT);
        g.fillText(weatherText(), x - r * 1.6, y + r * 2.6);
    }

    /** Un soleil : un disque et ses rayons, qui tournent doucement. */
    private void sun(GraphicsContext g, double x, double y, double r) {
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
    private static void cloud(GraphicsContext g, double x, double y, double r, Color color, double alpha) {
        g.setGlobalAlpha(alpha);
        g.setFill(color);
        g.fillOval(x - r * 1.3, y - r * 0.3, r * 1.3, r);
        g.fillOval(x - r * 0.7, y - r * 0.8, r * 1.4, r * 1.4);
        g.fillOval(x + r * 0.1, y - r * 0.4, r * 1.2, r * 1.05);
        g.fillRoundRect(x - r * 1.3, y, r * 2.6, r * 0.6, r * 0.6, r * 0.6);
        g.setGlobalAlpha(1);
    }

    private boolean canUnlocked;
    /** Où est l'arrosoir (son centre, en points de la vue) ; NaN : sur l'étagère, avant le premier dessin. */
    private double canX = Double.NaN, canY = Double.NaN;
    private boolean dragging;
    private double grabX, grabY;
    /** De 0 (droit) à 1 (penché) : il suit en douceur. */
    private double tilt;
    private boolean overPlant;
    private double streamTimer;
    private final java.util.List<Stream> stream = new java.util.ArrayList<>();
    /** Ce que le dernier dessin a vu de la plante : son axe, sa demi-largeur, son sommet et son pied. */
    private double plantX, plantHalf, plantTop, plantBottom;

    public SeedView() {
        super(800, 400);
        setOnMousePressed(event -> {
            // Le rond de croissance d'abord (version 0.153), puis une bulle (version 0.142) : sinon, l'arrosoir.
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
            setCursor(buy || overBloom(event.getX(), event.getY()) ? javafx.scene.Cursor.HAND : canUnlocked && overCan(event.getX(), event.getY())
                    ? javafx.scene.Cursor.OPEN_HAND : javafx.scene.Cursor.DEFAULT);
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

    public boolean isCanUnlocked() {
        return canUnlocked;
    }

    /** La taille de l'arrosoir, et celle de l'étagère : à la mesure de la vue. */
    private double canSize() {
        return Math.max(26, Math.min(getWidth(), getHeight()) * 0.11);
    }

    /** Le milieu de l'étagère, sur sa planche : là où l'arrosoir se pose, et où se tient le bouton qui le débloque. */
    public double shelfX() {
        // En bas à droite de la vue.
        return getWidth() - Math.max(canSize() * 1.6, getWidth() * 0.12);
    }

    /** Le dessus de la planche de l'étagère. */
    public double shelfTop() {
        return getHeight() - Math.max(canSize() * 0.55, getHeight() * 0.07);
    }

    /** Le milieu de l'étagère en hauteur, entre ses deux planches : là où se tient le bouton qui débloque l'arrosoir. */
    public double shelfMiddleY() {
        return shelfTop() - canSize() * 0.68;
    }

    /** Où l'arrosoir se pose sur l'étagère. */
    private double homeX() {
        return shelfX();
    }

    private double homeY() {
        return shelfTop() - canSize() * 0.42;
    }

    public double canX() {
        return Double.isNaN(canX) ? homeX() : canX;
    }

    public double canY() {
        return Double.isNaN(canY) ? homeY() : canY;
    }

    /** De 0 à 1 : penché ou non. */
    public double tilt() {
        return tilt;
    }

    private boolean overCan(double x, double y) {
        double s = canSize();
        return Math.abs(x - canX()) <= s * 0.75 && Math.abs(y - canY()) <= s * 0.6;
    }

    /** Prend l'arrosoir si le point est dessus. Rend vrai s'il est pris. */
    public boolean grab(double x, double y) {
        if (!canUnlocked || !overCan(x, y)) return false;
        dragging = true;
        grabX = x - canX();
        grabY = y - canY();
        setCursor(javafx.scene.Cursor.CLOSED_HAND);
        return true;
    }

    /** L'arrosoir suit la souris, sans sortir de la vue. */
    public void drag(double x, double y) {
        if (!dragging) return;
        double s = canSize();
        canX = Math.max(s * 0.6, Math.min(getWidth() - s, x - grabX));
        canY = Math.max(s * 0.6, Math.min(getHeight() - s * 0.5, y - grabY));
        overPlant = pourPointOverPlant();
    }

    /** Relâché : il se redresse et revient sur l'étagère. */
    public void release() {
        if (!dragging) return;
        dragging = false;
        overPlant = false;
        setCursor(javafx.scene.Cursor.DEFAULT);
    }

    /** Là où tombe l'eau de l'arrosoir penché : un peu à droite de son centre, sous le bec. */
    private double pourX() {
        return canX() + canSize() * 0.95;
    }

    private boolean pourPointOverPlant() {
        if (plantHalf <= 0) return false;
        double x = pourX(), y = canY();
        return Math.abs(x - plantX) <= plantHalf && y <= plantBottom && y >= plantTop - getHeight() * 0.4;
    }

    /** Vrai tant que l'arrosoir, tenu au-dessus de la graine, est assez penché pour verser : le jeu compte alors ses arrosages. */
    public boolean isPouring() {
        return canUnlocked && dragging && overPlant && tilt > 0.6 && seeds >= 1;
    }

    /** Les gouttes qui coulent du bec : pour les vérifications. */
    public int streamDrops() {
        return stream.size();
    }

    /** Le bout du bec, en points de la vue, l'arrosoir penché comme il l'est. */
    private double[] spout() {
        double s = canSize(), a = Math.toRadians(CAN_TILT_DEGREES * tilt);
        double lx = s * 0.95, ly = -s * 0.42;
        return new double[] {canX() + lx * Math.cos(a) - ly * Math.sin(a), canY() + lx * Math.sin(a) + ly * Math.cos(a)};
    }

    /** L'arrosoir avance : il suit la main ou revient sur l'étagère, se penche ou se redresse, et l'eau coule. */
    private void moveCan(double dt) {
        if (!dragging && !Double.isNaN(canX)) {
            double k = Math.min(1, dt * 7);
            canX += (homeX() - canX) * k;
            canY += (homeY() - canY) * k;
            if (Math.hypot(homeX() - canX, homeY() - canY) < 0.5) canX = canY = Double.NaN;
        }
        if (dragging) overPlant = pourPointOverPlant();
        double target = dragging && overPlant ? 1 : 0;
        tilt += (target - tilt) * Math.min(1, dt * 6);
        if (isPouring()) {
            streamTimer += dt;
            // Un filet de gouttes qui sort du bec dans son axe, en éventail léger, puis tombe.
            double a = Math.toRadians(CAN_TILT_DEGREES * tilt - 35);
            while (streamTimer >= 0.02) {
                streamTimer -= 0.02;
                double[] tip = spout();
                Stream drop = new Stream();
                double spread = (UniverseView.noise(++dropCount * 5 + 1) - 0.5) * 0.5;
                double speed = canSize() * (1.4 + 0.6 * UniverseView.noise(dropCount * 5 + 2));
                drop.x = tip[0];
                drop.y = tip[1];
                drop.vx = Math.cos(a + spread) * speed;
                drop.vy = Math.sin(a + spread) * speed;
                drop.land = Math.max(plantTop + (plantBottom - plantTop) * 0.1, tip[1] + canSize() * (0.9 + 0.4 * UniverseView.noise(dropCount * 5 + 3)));
                stream.add(drop);
            }
        } else streamTimer = 0;
        // Le tuyau arrose seul : un jet en cloche de sa lance vers le pied de la plante.
        if (hoseFlowing && plantHalf > 0) {
            hoseTimer += dt;
            while (hoseTimer >= 0.05) {
                hoseTimer -= 0.05;
                double[] tip = hoseNozzle();
                Stream drop = new Stream();
                // Une cloche qui retombe sur la plante en un peu plus d'une demi-seconde.
                double spread = (UniverseView.noise(++dropCount * 5 + 4) - 0.5) * 0.3;
                double flight = 0.6 * (1 + spread), fall = Math.max(300, getHeight() * 1.6);
                double reach = Math.max(1, plantX - tip[0]) * (0.75 + 0.2 * UniverseView.noise(dropCount * 5 + 5));
                drop.x = tip[0];
                drop.y = tip[1];
                drop.land = plantBottom - canSize() * 0.1;
                drop.vx = reach / flight;
                drop.vy = (drop.land - tip[1] - 0.5 * fall * flight * flight) / flight;
                hoseSpray.add(drop);
            }
        } else hoseTimer = 0;
        if (chronoShown) chronoHand += dt * (chronoRunning ? 5 : 0.4);
        double gravity = Math.max(300, getHeight() * 1.6);
        for (Stream drop : hoseSpray) {
            drop.age += dt;
            if (drop.splashed) continue;
            drop.vy += gravity * dt;
            drop.x += drop.vx * dt;
            drop.y += drop.vy * dt;
            if (drop.vy > 0 && drop.y >= drop.land || drop.y >= getHeight()) {
                drop.splashed = true;
                drop.age = 0;
            }
        }
        hoseSpray.removeIf(drop -> drop.splashed ? drop.age > 0.35 : drop.age > 3);
        for (Stream drop : stream) {
            drop.age += dt;
            if (drop.splashed) continue;
            drop.vy += gravity * dt;
            drop.x += drop.vx * dt;
            drop.y += drop.vy * dt;
            if (drop.y >= drop.land || drop.y >= getHeight()) {
                drop.splashed = true;
                drop.age = 0;
            }
        }
        stream.removeIf(drop -> drop.splashed ? drop.age > 0.35 : drop.age > 3);
    }

    /** Le nombre de graines à montrer (un réel : il peut être très grand). */
    public void setSeeds(double count) {
        seeds = Math.max(0, count);
    }

    /** Graines dessinées autour de la première : pour les vérifications. */
    public int orbiting() {
        return (int) Math.min(MAX_ORBITING, Math.max(0, Math.floor(seeds) - 1));
    }

    /** La graine a-t-elle germé, et combien de fois son germe a-t-il été arrosé. */
    public void setSprout(boolean sprouted, long waterings) {
        if (sprouted && !this.sprouted && shownSprout <= 0) shownSprout = 0.02;
        this.sprouted = sprouted;
        this.waterings = Math.max(0, waterings);
        if (!sprouted) shownSprout = 0;
    }

    /**
     * Le palier de la graine, de 1 à 11. Un palier de plus qu'avant joue le passage ({@link #evolve()}) ; un palier de
     * moins (une nouvelle partie) se pose sans animation.
     */
    public void setStage(int value) {
        int next = Math.max(1, Math.min(11, value));
        if (next > stage && stage >= 2) evolve(stage);
        else if (next < stage) evolveAge = -1;
        stage = next;
        twig = stage >= 2;
    }

    /** Le palier dessiné : pour les vérifications. */
    public int stage() {
        return stage;
    }

    /** Joue le passage depuis le palier {@code from} : la vue s'éloigne, une fissure craque dans la graine, la tige grossit. */
    private void evolve(int from) {
        evolveFrom = from;
        evolveAge = 0;
        flash = 1;
    }

    /** Vrai pendant le passage d'un palier : pour les vérifications. */
    public boolean isEvolving() {
        return evolveAge >= 0;
    }

    private static double ease(double t) {
        double x = Math.max(0, Math.min(1, t));
        return x * x * (3 - 2 * x);
    }

    /** La part de la vue gardée en ce moment : celle du palier, ou en route d'un palier à l'autre. */
    private double zoom() {
        double target = ZOOM[stage - 1];
        if (evolveAge < 0) return target;
        return ZOOM[evolveFrom - 1] + (target - ZOOM[evolveFrom - 1]) * ease((evolveAge - 0.3) / 1.9);
    }

    /** La croissance de ce que le palier ajoute : de 0,55 à 1 pendant le passage, 1 ensuite. */
    private double growth() {
        return evolveAge < 0 ? 1 : 0.55 + 0.45 * ease((evolveAge - 0.6) / 2.0);
    }

    /** Salue la brindille d'un éclat de lumière. */
    public void celebrate() {
        flash = 1;
    }

    /** Une goutte d'eau tombe sur le germe. */
    public void water() {
        if (drops.size() < 12) drops.add(new Drop(UniverseView.noise(++dropCount * 3 + 7) - 0.5));
    }

    /** Les gouttes en train de tomber : pour les vérifications. */
    public int drops() {
        return drops.size();
    }

    /**
     * La longueur que visent les arrosages, de 0 à 1 : 0,15 à la germination, puis de plus en plus
     * lentement vers 1.
     */
    public double sprout() {
        if (!sprouted || seeds < 1) return 0;
        return Math.min(1, 0.15 + 0.85 * (1 - Math.exp(-waterings / 30.0)));
    }

    /** Les feuilles du germe : une à la germination, une de plus tous les quinze arrosages, cinq au plus. */
    public int leaves() {
        return sprouted ? (int) Math.min(5, 1 + waterings / 15) : 0;
    }

    public void frame(double elapsed, boolean moving) {
        double dt = Math.min(elapsed, 0.1);
        if (moving) time += dt;
        // Le germe rejoint sa longueur en une seconde ou deux, même animations coupées.
        shownSprout += (sprout() - shownSprout) * Math.min(1, dt * 2.5);
        flash = Math.max(0, flash - dt * 0.6);
        if (evolveAge >= 0) {
            evolveAge += dt;
            if (evolveAge >= EVOLVE_SECONDS) evolveAge = -1;
        }
        for (Drop drop : drops) drop.age += dt;
        drops.removeIf(drop -> drop.age > DROP_SECONDS + 0.6);
        moveCan(dt);
        draw();
        drawBubbles();
        drawShelfAndCan();
        drawBloom();
        drawWeather();
        drawBubbleLabel();
    }

    private void draw() {
        double w = getWidth(), h = getHeight();
        GraphicsContext g = CanvasPane.begin(this, BACK);
        if (g == null) return;
        double zoom = zoom();
        // Le passage secoue un instant la graine, quand elle craque.
        double shake = evolveAge >= 0 && evolveAge < 0.6 ? Math.sin(evolveAge * 70) * 3 * (1 - evolveAge / 0.6) : 0;
        // Plus la vue s'éloigne, plus la graine descend : l'arbre (version 0.121) monte jusqu'en haut de la vue.
        double cx = w / 2 + shake, cy = h * (0.55 + 0.33 * Math.min(1, (1 - zoom) / 0.75));
        double size = Math.min(w, h) * 0.16 * zoom;
        boolean any = seeds >= 1;
        plantHalf = 0;
        seedX = cx;
        seedY = cy;
        seedSize = any ? size : 0;
        crownR = 0;

        // La poussière qui monte, lentement, et s'éteint en haut ; de plus en plus dense avec les paliers.
        int motes = MOTES + (stage > 4 ? 30 * (stage - 4) : 0);
        for (int k = 0; k < motes; k++) {
            double speed = 8 + 14 * UniverseView.noise(k * 7 + 1);
            double y = h - ((UniverseView.noise(k * 7 + 2) * h + time * speed) % h);
            double x = w * UniverseView.noise(k * 7 + 3) + Math.sin(time * 0.6 + k) * 6;
            double fade = y / h;
            g.setGlobalAlpha(Math.min(1, (0.15 + 0.45 * fade * UniverseView.noise(k * 7 + 4)) * (1 + SeedTree.prominence(stage))));
            g.setFill(MOTE);
            double r = 0.8 + 1.4 * UniverseView.noise(k * 7 + 5);
            g.fillOval(x - r, y - r, 2 * r, 2 * r);
        }
        g.setGlobalAlpha(1);

        if (!any) {
            // Pas encore de graine : un creux vide, à peine marqué.
            g.setStroke(SHELL);
            g.setGlobalAlpha(0.35);
            g.setLineWidth(1.5);
            seedPath(g, cx, cy, size, 0, false);
            g.setGlobalAlpha(1);
            return;
        }

        // Les ondes de germination.
        g.setStroke(HALO);
        g.setLineWidth(1.2);
        for (int k = 0; k < WAVES; k++) {
            double phase = ((time / WAVE_SECONDS) + k / (double) WAVES) % 1;
            double r = size * (1 + 2.6 * phase);
            g.setGlobalAlpha(0.35 * (1 - phase));
            g.strokeOval(cx - r, cy - r * 0.9, 2 * r, 1.8 * r);
        }
        g.setGlobalAlpha(1);

        // Le halo, qui respire.
        double breath = 1 + 0.035 * Math.sin(time * 1.7);
        UniverseView.glow(g, cx, cy, size * 2.4 * breath, HALO, 0.30);
        UniverseView.glow(g, cx, cy, size * 1.2 * breath, SEAM, 0.25);

        // Les graines de plus, en orbite : derrière (en haut) plus petites et plus pâles, devant plus grandes.
        int count = orbiting();
        for (int k = 0; k < count; k++) {
            double angle = 2 * Math.PI * k / Math.max(1, count) + time * (0.12 + 0.05 * (k % 3));
            double ring = size * (2.0 + 0.6 * UniverseView.noise(k * 11 + 1));
            double x = cx + Math.cos(angle) * ring * 1.3;
            double y = cy + Math.sin(angle) * ring * 0.45;
            double depth = 0.5 + 0.5 * Math.sin(angle);
            double s = size * (0.16 + 0.12 * UniverseView.noise(k * 11 + 2)) * (0.75 + 0.35 * depth);
            UniverseView.glow(g, x, y, s * 2.2, HALO, 0.25 + 0.2 * depth);
            g.setGlobalAlpha(0.55 + 0.45 * depth);
            g.setFill(FLESH);
            seedPath(g, x, y, s, angle * 0.7, true);
        }
        g.setGlobalAlpha(1);

        // Le germe, une tige courbe qui sort de la pointe, et ses feuilles.
        double grow = shownSprout * (stage >= 4 ? 1.3 : 1) * (stage >= 3 ? 0.85 + 0.15 * growth() : 1);
        double tipX = cx + Math.sin(-0.35) * size * 1.0, tipY = cy - Math.cos(-0.35) * size * 1.0;
        double topX = tipX, topY = tipY;
        // Du bonsaï à l'arbre-monde (version 0.120) : un arbre dessiné de la graine brisée.
        if (stage >= 5) {
            double[] top = SeedTree.draw(g, w, h, cx, cy - size * 0.2, stage, growth(), time);
            topX = top[0];
            topY = top[1];
            crownX = top[0];
            crownY = top[1];
            crownR = top[2];
            grow = 0;
        }
        // Plus épaisse à chaque palier : la tige craquelée puis brisée (version 0.120).
        double thick = stage >= 4 ? 2.6 : stage >= 3 ? 1.8 : 1;
        if (stage >= 3) thick = 1 + (thick - 1) * growth();
        if (grow > 0.005) {
            int n = 14;
            double[] xs = new double[n], ys = new double[n];
            for (int i = 0; i < n; i++) {
                double t = i / (double) (n - 1);
                double sway = Math.sin(time * 1.1) * 4 * t;
                xs[i] = tipX - size * 0.5 * grow * Math.sin(t * 1.4) + sway;
                ys[i] = tipY - size * 1.6 * grow * t;
            }
            // Brindille : la tige devient bois, plus épaisse, et deux rameaux en partent.
            g.setStroke(twig ? WOOD : SPROUT);
            g.setLineWidth(Math.max(1.5, size * (twig ? 0.09 : 0.05) * thick));
            g.setGlobalAlpha(0.95);
            g.strokePolyline(xs, ys, n);
            if (twig) {
                for (int b = 0; b < 2; b++) {
                    int at = b == 0 ? n / 2 : (3 * n) / 4;
                    double side = b == 0 ? -1 : 1, len = size * 0.45 * grow;
                    double bx = xs[at] + side * len, by = ys[at] - len * 0.6;
                    g.setLineWidth(Math.max(1, size * 0.04));
                    g.strokeLine(xs[at], ys[at], bx, by);
                    double leaf = size * 0.2;
                    g.setFill(SPROUT);
                    g.setGlobalAlpha(0.85);
                    g.fillPolygon(new double[] {bx, bx + side * leaf, bx + side * leaf * 1.3, bx + side * leaf * 0.5},
                            new double[] {by, by - leaf * 0.5, by - leaf * 0.05, by + leaf * 0.25}, 4);
                    g.setGlobalAlpha(0.95);
                }
            }
            topX = xs[n - 1];
            topY = ys[n - 1];
            int leaves = leaves();
            for (int k = 0; k < leaves; k++) {
                // La première au bout ; les autres le long de la tige, de chaque côté.
                int at = k == 0 ? n - 1 : Math.max(2, n - 1 - k * 2);
                double lx = xs[at], ly = ys[at], side = k % 2 == 0 ? 1 : -1;
                double leaf = size * (k == 0 ? 0.35 : 0.24) * grow;
                double[] fx = {lx, lx + side * leaf, lx + side * leaf * 1.4, lx + side * leaf * 0.6};
                double[] fy = {ly, ly - leaf * 0.5, ly - leaf * 0.1, ly + leaf * 0.25};
                g.setFill(SPROUT);
                g.setGlobalAlpha(0.8);
                g.fillPolygon(fx, fy, 4);
            }
            g.setGlobalAlpha(1);
        }

        // Où l'arrosoir verse (version 0.137) : la plante, de son pied à son sommet.
        plantX = cx;
        plantTop = Math.min(topY, cy - size);
        plantBottom = cy + size * 0.5;
        plantHalf = Math.max(size * 1.6, (cy - plantTop) * 0.45);

        // Les gouttes : elles tombent du haut sur le germe, puis éclaboussent.
        for (Drop drop : drops) {
            double targetX = topX + drop.drift * size * 0.2;
            if (drop.age < DROP_SECONDS) {
                double t = drop.age / DROP_SECONDS;
                double y = -10 + (topY + 10) * t * t, r = Math.max(3, size * 0.06);
                g.setFill(WATER);
                g.setGlobalAlpha(0.9);
                double[] px = {targetX, targetX + r, targetX, targetX - r};
                double[] py = {y - r * 2.2, y, y + r, y};
                g.fillPolygon(px, py, 4);
                g.fillOval(targetX - r, y - r * 0.2, r * 2, r * 1.4);
            } else {
                double t = (drop.age - DROP_SECONDS) / 0.6;
                g.setFill(WATER);
                g.setGlobalAlpha(0.8 * (1 - t));
                for (int k = 0; k < 6; k++) {
                    double a = Math.PI + Math.PI * k / 5;
                    double d = size * 0.35 * t;
                    double r = Math.max(1.5, size * 0.025);
                    g.fillOval(targetX + Math.cos(a) * d - r, topY + Math.sin(a) * d * 0.6 - r + size * 0.2 * t * t, 2 * r, 2 * r);
                }
                UniverseView.glow(g, targetX, topY, size * 0.4 * (1 - t) + 2, WATER, 0.5 * (1 - t));
            }
        }
        g.setGlobalAlpha(1);

        if (flash > 0) UniverseView.glow(g, cx, cy - size, size * (2 + 4 * (1 - flash)), SEAM, 0.8 * flash);

        // La graine elle-même : la coque, la chair, la couture. Brisée à partir du palier 4 (version 0.120) : ses deux
        // moitiés s'écartent au pied de la plante.
        double s = size * breath;
        if (stage >= 4) {
            double apart = size * (0.35 + 0.25 * growth());
            for (int side = -1; side <= 1; side += 2) {
                double hx = cx + side * apart, tilt = -0.35 + side * 0.9;
                g.setFill(SHELL);
                seedPath(g, hx, cy + size * 0.15, s * 0.75, tilt, true);
                g.setFill(FLESH);
                g.setGlobalAlpha(0.8);
                seedPath(g, hx + side * s * 0.08, cy + size * 0.12, s * 0.55, tilt, true);
                g.setGlobalAlpha(1);
            }
            if (flash > 0) UniverseView.glow(g, cx, cy - size, size * (2 + 4 * (1 - flash)), SEAM, 0.8 * flash);
            if (evolveAge >= 0) SeedTree.evolveBurst(g, w, h, cx, cy, stage, evolveAge);
            return;
        }
        g.setFill(SHELL);
        seedPath(g, cx, cy, s * 1.06, -0.35, true);
        g.setFill(FLESH);
        seedPath(g, cx, cy, s, -0.35, true);
        if (stage >= 3) {
            // Les fissures : elles partent de la pointe, et brillent un instant quand la graine craque.
            double bright = evolveAge >= 0 && evolveAge < 1.2 ? 1 - evolveAge / 1.2 : 0;
            g.setStroke(bright > 0 ? SEAM : CRACK);
            g.setLineWidth(Math.max(1.2, size * 0.035));
            for (int c = 0; c < 3; c++) {
                double[] xs = new double[5], ys = new double[5];
                double angle = -0.35 + (c - 1) * 0.5;
                for (int k = 0; k < 5; k++) {
                    double t = k / 4.0, jag = (k % 2 == 0 ? 1 : -1) * size * 0.08;
                    xs[k] = cx + Math.sin(angle) * s * (0.95 - 0.9 * t) + Math.cos(angle) * jag;
                    ys[k] = cy - Math.cos(angle) * s * (0.95 - 0.9 * t) + Math.sin(angle) * jag;
                }
                g.setGlobalAlpha(0.9);
                g.strokePolyline(xs, ys, 5);
            }
            g.setGlobalAlpha(1);
            if (bright > 0) UniverseView.glow(g, cx, cy - s * 0.4, s * 1.4, SEAM, 0.6 * bright);
        }
        g.setStroke(SEAM);
        g.setLineWidth(Math.max(1, size * 0.03));
        g.setGlobalAlpha(0.8);
        double ax = Math.sin(-0.35), ay = -Math.cos(-0.35);
        g.strokeLine(cx - ax * s * 0.75, cy - ay * s * 0.75, cx + ax * s * 0.85, cy + ay * s * 0.85);
        g.setGlobalAlpha(1);
        UniverseView.glow(g, cx - s * 0.25, cy - s * 0.15, s * 0.45, SEAM, 0.45);
        if (evolveAge >= 0) SeedTree.evolveBurst(g, w, h, cx, cy, stage, evolveAge);
    }

    /**
     * L'étagère de bois et l'arrosoir (version 0.137), par-dessus le reste : avant le déblocage, l'étagère est vide et
     * l'arrosoir n'y est qu'une ombre, sous le bouton qui le débloque.
     */
    private void drawShelfAndCan() {
        double w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0 || seeds < 1) return;
        GraphicsContext g = getGraphicsContext2D();
        double s = canSize(), x = shelfX(), top = shelfTop(), plank = s * 2.4, thick = Math.max(5, s * 0.14);
        // Une petite étagère de bois : deux montants, un fond plus sombre, une planche du haut et celle où se pose
        // l'arrosoir, posée sur deux pieds.
        // Version 0.139 : un étage de plus pour le chrono, au-dessus de l'arrosoir.
        double level = s * 1.35, mid = top - level, high = top - level * (chronoShown ? 2 : 1);
        double left = x - plank / 2, right = x + plank / 2, post = Math.max(4, s * 0.1);
        drawHose(g, s);
        g.setGlobalAlpha(0.55);
        g.setFill(SHELF_EDGE);
        g.fillRect(left + post, high, plank - 2 * post, top - high);
        g.setGlobalAlpha(1);
        g.setFill(SHELF);
        g.fillRect(left, high - thick, post, top + thick * 2 - high);
        g.fillRect(right - post, high - thick, post, top + thick * 2 - high);
        for (double board : chronoShown ? new double[] {top, mid, high - thick} : new double[] {top, high - thick}) {
            g.setFill(SHELF);
            g.fillRoundRect(left - thick * 0.4, board, plank + thick * 0.8, thick, 3, 3);
            g.setFill(SHELF_EDGE);
            g.fillRect(left - thick * 0.4, board + thick * 0.65, plank + thick * 0.8, thick * 0.35);
        }
        if (chronoShown) drawChrono(g, x, mid - level / 2, s * 0.45);
        // L'effet du rond de croissance (version 0.153), au-dessus de l'étagère : l'arrosage va plus vite.
        String boost = bloomText();
        if (!boost.isEmpty()) {
            g.setFont(javafx.scene.text.Font.font(13));
            g.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
            g.setGlobalAlpha(0.85 + 0.15 * Math.sin(time * 4));
            g.setFill(BLOOM);
            g.fillText(boost, Math.min(w - plank * 0.6, x), high - thick - 10);
            g.setGlobalAlpha(1);
        }
        // Le fil du bois.
        g.setStroke(SHELF_EDGE);
        g.setGlobalAlpha(0.35);
        g.setLineWidth(1);
        g.strokeLine(left + plank * 0.05, top + thick * 0.35, x + plank * 0.1, top + thick * 0.3);
        g.strokeLine(x - plank * 0.1, high - thick * 0.6, right - plank * 0.05, high - thick * 0.65);
        g.setGlobalAlpha(1);

        // L'eau qui coule du bec, puis éclabousse.
        for (Stream drop : stream) {
            double r = Math.max(2, s * 0.045);
            g.setFill(WATER);
            if (drop.splashed) {
                double t = drop.age / 0.35;
                g.setGlobalAlpha(0.7 * (1 - t));
                for (int k = 0; k < 3; k++) {
                    double a = Math.PI + Math.PI * (k + 0.5) / 3, d = s * 0.25 * t;
                    g.fillOval(drop.x + Math.cos(a) * d - r, drop.y + Math.sin(a) * d * 0.5 - r, 2 * r, 2 * r);
                }
            } else {
                g.setGlobalAlpha(0.9);
                g.fillOval(drop.x - r, drop.y - r * 1.4, 2 * r, 2.8 * r);
                UniverseView.glow(g, drop.x, drop.y, r * 2.5, WATER, 0.25);
            }
        }
        for (Stream drop : hoseSpray) {
            double r = Math.max(1.6, s * 0.035);
            g.setFill(WATER);
            if (drop.splashed) {
                g.setGlobalAlpha(0.6 * (1 - drop.age / 0.35));
                g.fillOval(drop.x - r * 1.5, drop.y - r * 0.5, r * 3, r);
            } else {
                g.setGlobalAlpha(0.85);
                g.fillOval(drop.x - r, drop.y - r, 2 * r, 2 * r);
            }
        }
        g.setGlobalAlpha(1);

        if (!canUnlocked) {
            // Une ombre sur l'étagère : ce que le bouton débloque.
            g.setGlobalAlpha(0.22);
            drawCan(g, homeX(), homeY(), s, 0, true);
            g.setGlobalAlpha(1);
            return;
        }
        drawCan(g, canX(), canY(), s, CAN_TILT_DEGREES * tilt, false);
    }

    /** La lance du tuyau : près du pied de la plante, à droite, tournée vers elle. */
    private double[] hoseNozzle() {
        double s = canSize();
        return new double[] {plantX - Math.max(plantHalf * 0.75, s * 1.6), plantBottom - s * 0.15};
    }

    /**
     * Le tuyau d'arrosage : il vient du bord gauche, loin de l'étagère, court au sol et finit par sa lance, au pied de
     * la plante.
     */
    /**
     * Le schéma des bulles (version 0.143), repris des dessins des paliers 7 à 11 et rendu symétrique : pour chaque place
     * (le rang de la bulle dans le jeu), sa position en unités du dessin, la base à l'origine, vers la droite et vers le
     * haut. Les racines sont sous la base.
     */
    private static final double[][] SCHEMA = {
            {0, 0},
            {-130, 182}, {130, 182},
            {-268, 390}, {0, 390}, {268, 390},
            {-156, 567}, {156, 567},
            {-436, 480}, {436, 480},
            {-336, 638}, {336, 638},
            {-626, 582}, {-500, 295}, {626, 582}, {500, 295},
            {0, -158}, {0, 574}, {0, 725}};

    /**
     * Les positions des bulles, avant qu'elles flottent : la base sur la graine, et la rangée la plus haute des bulles
     * nées en haut du feuillage. Le schéma garde ses proportions ; s'il est trop large pour la vue, il rétrécit. Les
     * racines, sous la graine, restent dans la vue.
     */
    private void placeBubbles() {
        int n = bubbles.size();
        double baseX = seedX, baseY = seedY + seedSize * 0.05;
        double top = 0, wide = 0;
        for (Bubble bubble : bubbles) {
            double[] at = SCHEMA[Math.max(0, Math.min(SCHEMA.length - 1, bubble.place()))];
            top = Math.max(top, at[1]);
            wide = Math.max(wide, Math.abs(at[0]));
        }
        double k = 0, kx = 0;
        if (top > 0 && crownR > 0) {
            // Le bonsaï n'a que sa rangée du feuillage : elle se pose dans ses plateaux, pas au-dessus.
            boolean bonsai = top < SCHEMA[3][1];
            double crownTop = bonsai ? crownY + crownR * 0.1 : crownY - crownR * 0.35;
            k = Math.max(0, baseY - crownTop) / top;
            if (wide > 0) k = Math.min(k, (getWidth() * 0.47 - bubbleR) / wide);
            k = Math.min(k, (baseY - bubbleR * 1.6) / top);
            kx = bonsai && wide > 0 ? Math.min(k, crownR * 0.75 / wide) : k;
        }
        for (int i = 0; i < n; i++) {
            double[] at = SCHEMA[Math.max(0, Math.min(SCHEMA.length - 1, bubbles.get(i).place()))];
            bubbleX[i] = baseX + at[0] * kx;
            bubbleY[i] = Math.min(getHeight() - bubbleR * 1.2, baseY - at[1] * k);
        }
        // Les deux bulles du bas à l'arbre-monde (version 0.144) : la graine et les racines remontent un peu et
        // s'écartent, au lieu d'être serrées au bas de la vue.
        int seed = -1, roots = -1;
        for (int i = 0; i < n; i++) {
            if (bubbles.get(i).place() == 0) seed = i;
            if (bubbles.get(i).place() == ROOTS_PLACE) roots = i;
        }
        if (seed >= 0 && roots >= 0) {
            double gap = bubbleR * 3.6;
            double rootsY = Math.min(baseY + gap - bubbleR, getHeight() - bubbleR * 2.0);
            bubbleY[roots] = rootsY;
            bubbleY[seed] = rootsY - gap;
        }
    }

    /** La place des racines dans le schéma : la bulle tout en bas, sous la graine, à l'aura dorée. */
    static final int ROOTS_PLACE = 16;
    private static final Color ROOTS_GOLD = Color.web("#ffd27f");

    /** Le rayon des bulles au dernier dessin : pour les vérifications. */
    public double bubbleRadius() {
        return bubbleR;
    }

    /**
     * Les bulles de l'arbre (version 0.142) et leurs liens : chaque bulle reliée aux bulles d'en dessous qui l'ouvrent.
     * Elles flottent un peu. Achetable, une bulle pulse ; achetée, elle reste allumée ; sinon elle n'est qu'un reflet,
     * en pointillé tant qu'aucune bulle reliée n'est achetée.
     */
    private void drawBubbles() {
        int n = bubbles.size();
        if (bubbleX.length != n) {
            bubbleX = new double[n];
            bubbleY = new double[n];
        }
        if (n == 0 || seedSize <= 0) return;
        GraphicsContext g = getGraphicsContext2D();
        // Une taille tenue par la vue, pas par la graine (qui rapetisse quand l'arbre grandit) : lisible à tous les paliers.
        bubbleR = Math.max(13, Math.min(getWidth(), getHeight()) * 0.036);
        placeBubbles();
        for (int i = 0; i < n; i++) bubbleY[i] += Math.sin(time * 1.3 + i * 2.1) * bubbleR * 0.12;
        // Les liens d'abord, derrière : de bord à bord, allumés quand les deux bulles sont achetées.
        for (int i = 0; i < n; i++) {
            for (int j : bubbles.get(i).below()) {
                if (j < 0 || j >= n) continue;
                double dx = bubbleX[i] - bubbleX[j], dy = bubbleY[i] - bubbleY[j], len = Math.hypot(dx, dy);
                if (len <= 2 * bubbleR) continue;
                double ux = dx / len, uy = dy / len;
                boolean lit = bubbles.get(i).state() == BubbleState.BOUGHT && bubbles.get(j).state() == BubbleState.BOUGHT;
                // Un lien vaut dans les deux sens (version 0.143) : plein dès qu'un de ses bouts est acheté.
                boolean open = bubbles.get(j).state() == BubbleState.BOUGHT || bubbles.get(i).state() == BubbleState.BOUGHT;
                g.setStroke(BUBBLE);
                g.setLineWidth(lit ? 2.4 : 1.5);
                g.setGlobalAlpha(lit ? 0.9 : open ? 0.55 : 0.25);
                if (!open) g.setLineDashes(5, 5);
                g.strokeLine(bubbleX[j] + ux * bubbleR, bubbleY[j] + uy * bubbleR, bubbleX[i] - ux * bubbleR, bubbleY[i] - uy * bubbleR);
                g.setLineDashes();
            }
        }
        for (int i = 0; i < n; i++) {
            Bubble bubble = bubbles.get(i);
            double x = bubbleX[i], y = bubbleY[i];
            double r = bubbleR * (i == hovered ? 1.08 : 1);
            double pulse = 0.5 + 0.5 * Math.sin(time * 3 + i);
            // Les racines (version 0.144) : une aura dorée qui respire, achetées ou non.
            if (bubble.place() == ROOTS_PLACE) {
                double breath = 0.5 + 0.5 * Math.sin(time * 1.6);
                UniverseView.glow(g, x, y, r * (2.8 + 0.6 * breath), ROOTS_GOLD, 0.65 + 0.25 * breath);
                UniverseView.glow(g, x, y, r * 1.6, Color.web("#fff1b8"), 0.5);
                g.setStroke(ROOTS_GOLD);
                g.setLineWidth(2.2);
                g.setGlobalAlpha(0.55 + 0.35 * breath);
                g.strokeOval(x - r * 1.3, y - r * 1.3, r * 2.6, r * 2.6);
            }
            // Un fond sombre derrière la bulle : elle se détache du feuillage.
            g.setFill(Color.web("#06121a"));
            g.setGlobalAlpha(0.6);
            g.fillOval(x - r * 1.08, y - r * 1.08, r * 2.16, r * 2.16);
            switch (bubble.state()) {
                case BOUGHT -> {
                    UniverseView.glow(g, x, y, r * 2.4, BUBBLE, 0.55);
                    g.setFill(new javafx.scene.paint.RadialGradient(0, 0, x - r * 0.3, y - r * 0.3, r * 1.3, false,
                            javafx.scene.paint.CycleMethod.NO_CYCLE, new javafx.scene.paint.Stop(0, Color.web("#e8fbff")),
                            new javafx.scene.paint.Stop(0.55, BUBBLE), new javafx.scene.paint.Stop(1, BUBBLE_DEEP)));
                    g.setGlobalAlpha(0.85);
                    g.fillOval(x - r, y - r, 2 * r, 2 * r);
                }
                case BUY -> {
                    UniverseView.glow(g, x, y, r * (1.8 + 0.6 * pulse), BUBBLE, 0.25 + 0.25 * pulse);
                    g.setFill(BUBBLE);
                    g.setGlobalAlpha(0.18);
                    g.fillOval(x - r, y - r, 2 * r, 2 * r);
                }
                default -> {
                    g.setFill(BUBBLE);
                    g.setGlobalAlpha(0.07);
                    g.fillOval(x - r, y - r, 2 * r, 2 * r);
                }
            }
            // Le bord, et le reflet en haut à gauche.
            g.setStroke(BUBBLE);
            g.setLineWidth(bubble.state() == BubbleState.BUY ? 2.2 : 1.4);
            g.setGlobalAlpha(switch (bubble.state()) {
                case BOUGHT -> 0.95;
                case BUY -> 0.6 + 0.4 * pulse;
                case SHORT -> 0.45;
                case CLOSED -> 0.22;
            });
            if (bubble.state() == BubbleState.CLOSED) g.setLineDashes(4, 4);
            g.strokeOval(x - r, y - r, 2 * r, 2 * r);
            g.setLineDashes();
            g.setFill(Color.WHITE);
            g.setGlobalAlpha(bubble.state() == BubbleState.CLOSED ? 0.2 : 0.55);
            g.fillOval(x - r * 0.55, y - r * 0.62, r * 0.42, r * 0.26);
        }
        g.setGlobalAlpha(1);
    }

    /** L'étiquette de la bulle survolée : son nom et son prix, au-dessus d'elle, par-dessus le reste. */
    private void drawBubbleLabel() {
        if (hovered < 0 || hovered >= bubbles.size() || hovered >= bubbleX.length) return;
        Bubble bubble = bubbles.get(hovered);
        GraphicsContext g = getGraphicsContext2D();
        g.save();
        javafx.scene.text.Font name = javafx.scene.text.Font.font(null, javafx.scene.text.FontWeight.BOLD, 13);
        javafx.scene.text.Font price = javafx.scene.text.Font.font(12);
        javafx.scene.text.Text measure = new javafx.scene.text.Text(bubble.name());
        measure.setFont(name);
        double width = measure.getLayoutBounds().getWidth();
        // Ce qu'elle fait (version 0.148), entre le nom et le prix : sur deux lignes quand elle est longue.
        java.util.List<String> effect = effectLines(bubble.effect());
        measure.setFont(price);
        for (String line : effect) {
            measure.setText(line);
            width = Math.max(width, measure.getLayoutBounds().getWidth());
        }
        measure.setText(bubble.price());
        width = Math.min(getWidth() - 8, Math.max(width, measure.getLayoutBounds().getWidth()) + 20);
        double height = 42 + 17 * effect.size();
        double x = Math.max(4, Math.min(getWidth() - width - 4, bubbleX[hovered] - width / 2));
        double y = Math.max(4, bubbleY[hovered] - bubbleR - height - 8);
        g.setGlobalAlpha(0.92);
        g.setFill(Color.web("#0d1a24"));
        g.fillRoundRect(x, y, width, height, 10, 10);
        g.setStroke(BUBBLE);
        g.setLineWidth(1.2);
        g.strokeRoundRect(x, y, width, height, 10, 10);
        g.setGlobalAlpha(1);
        g.setTextAlign(javafx.scene.text.TextAlignment.LEFT);
        g.setTextBaseline(javafx.geometry.VPos.TOP);
        g.setFont(name);
        g.setFill(Color.web("#e8fbff"));
        g.fillText(bubble.name(), x + 10, y + 6);
        g.setFont(price);
        g.setFill(Color.web("#c9dcff"));
        for (int i = 0; i < effect.size(); i++) g.fillText(effect.get(i), x + 10, y + 23 + 17 * i);
        g.setFill(switch (bubble.state()) {
            case BUY, BOUGHT -> Color.web("#a8f0b0");
            case SHORT -> Color.web("#ffb48a");
            case CLOSED -> Color.web("#9aa3bd");
        });
        g.fillText(bubble.price(), x + 10, y + 23 + 17 * effect.size());
        g.restore();
    }

    private void drawHose(GraphicsContext g, double s) {
        if (!hoseShown || plantHalf <= 0) return;
        double h = getHeight();
        double[] tip = hoseNozzle();
        double floor = h - s * 0.12;
        g.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        for (int pass = 0; pass < 2; pass++) {
            g.setStroke(pass == 0 ? HOSE : HOSE_LIGHT);
            g.setLineWidth(pass == 0 ? Math.max(4, s * 0.16) : Math.max(1.5, s * 0.05));
            g.setGlobalAlpha(pass == 0 ? 1 : 0.6);
            g.beginPath();
            g.moveTo(-s, floor);
            g.bezierCurveTo(s * 3, floor + s * 0.05, tip[0] - s * 2.5, floor, tip[0] - s * 0.55, tip[1] + s * 0.05);
            g.stroke();
        }
        g.setGlobalAlpha(1);
        // La lance, de laiton, tournée vers la plante.
        g.save();
        g.translate(tip[0], tip[1]);
        g.rotate(-20);
        g.setFill(BRASS);
        g.fillRoundRect(-s * 0.05, -s * 0.08, s * 0.55, s * 0.16, 4, 4);
        g.fillOval(-s * 0.1, -s * 0.11, s * 0.16, s * 0.22);
        g.restore();
    }

    /**
     * Le chrono de l'étage du haut : un cadran sombre que remplit le temps gardé (en bleu), une aiguille qui tourne
     * vite quand il accélère le jeu, lentement sinon, et un remontoir de laiton.
     */
    private void drawChrono(GraphicsContext g, double x, double y, double r) {
        if (chronoRunning) UniverseView.glow(g, x, y, r * 2.2, CHRONO_TIME, 0.35);
        g.setFill(BRASS);
        g.fillRect(x - r * 0.12, y - r * 1.25, r * 0.24, r * 0.3);
        g.fillOval(x - r * 0.22, y - r * 1.4, r * 0.44, r * 0.22);
        g.setFill(CHRONO_FACE);
        g.fillOval(x - r, y - r, 2 * r, 2 * r);
        g.setFill(CHRONO_TIME);
        g.setGlobalAlpha(0.8);
        g.fillArc(x - r * 0.85, y - r * 0.85, r * 1.7, r * 1.7, 90, -360 * chronoFill, javafx.scene.shape.ArcType.ROUND);
        g.setGlobalAlpha(1);
        g.setStroke(BRASS);
        g.setLineWidth(Math.max(2, r * 0.14));
        g.strokeOval(x - r, y - r, 2 * r, 2 * r);
        g.setLineWidth(Math.max(1, r * 0.05));
        for (int k = 0; k < 12; k++) {
            double a = Math.PI * 2 * k / 12;
            g.strokeLine(x + Math.cos(a) * r * 0.78, y + Math.sin(a) * r * 0.78, x + Math.cos(a) * r * 0.9, y + Math.sin(a) * r * 0.9);
        }
        double a = chronoHand - Math.PI / 2;
        g.setStroke(SEAM);
        g.setLineWidth(Math.max(1.5, r * 0.08));
        g.strokeLine(x, y, x + Math.cos(a) * r * 0.7, y + Math.sin(a) * r * 0.7);
        g.setFill(BRASS);
        g.fillOval(x - r * 0.1, y - r * 0.1, r * 0.2, r * 0.2);
    }

    /** Un arrosoir de fer peint, centré en ({@code x}, {@code y}), penché de {@code degrees} : seulement sa silhouette si {@code ghost}. */
    private static void drawCan(GraphicsContext g, double x, double y, double s, double degrees, boolean ghost) {
        g.save();
        g.translate(x, y);
        g.rotate(degrees);
        // L'anse, à l'arrière : elle part du rebord, monte en boucle et redescend se fixer au dos du corps. Dessinée avant
        // le corps, pour que ses deux bouts s'y enfoncent.
        g.setStroke(ghost ? MOTE : CAN_DARK);
        g.setLineWidth(Math.max(2, s * 0.09));
        g.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
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
    private static void seedPath(GraphicsContext g, double cx, double cy, double size, double turn, boolean fill) {
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
