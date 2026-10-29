package idle.ui.universe;

import idle.ui.common.CanvasPane;
import java.util.function.IntConsumer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

/**
 * La carte des dimensions (version 0.081) : dix points en cercle, et un onzième au milieu, relié à
 * tous. Les dimensions ouvertes brillent, leur numéro et leur nom dessous ; les autres restent
 * éteintes, un point d'interrogation dedans. Des éclats partent du milieu vers celles qui sont
 * ouvertes. Un clic sur une dimension ouverte l'ouvre dans sa fenêtre.
 *
 * <p>Le défi final (version 0.160), une fois la bulle des racines achetée : chaque point se remplit de vert, du bas
 * vers le haut, selon l'avancée vers son palier (en puissances de dix), et devient entièrement vert au palier atteint ;
 * le seuil, lui, devient un anneau vert autour du point. Sous chaque nom, l'avancée (« 3.2e6 / 2.5e7 ») ; au milieu,
 * les paliers atteints (« 7 / 10 paliers »). Les dix paliers atteints, la fin se joue ({@link #playFinale()}) : les
 * traits deviennent verts en vague, avec une aura dorée, puis le point du milieu grandit jusqu'à couvrir la vue ; au
 * bout, l'écran de fin ({@link #setOnFinale}). La partie finie, les traits restent verts et dorés.
 */
public final class DimensionsView extends Canvas {

    private static final Color BACK = Color.web("#05060e");
    private static final Color LINE = Color.web("#a8b8ff");
    private static final Color ON = Color.web("#d6c2ff");
    private static final Color OFF = Color.web("#3a4160");
    private static final Color CENTER = Color.web("#ffe2b0");
    private static final Color MARK = Color.web("#3ddc6a");
    private static final Color GOLD = Color.web("#ffd27f");
    /** La fin (version 0.160) : la vague verte des traits, puis le point du milieu qui grandit, en secondes. */
    public static final double FINALE_WAVE_SECONDS = 4, FINALE_GROW_SECONDS = 2.5;
    /** Les traits de la carte : les dix rayons du milieu, puis les dix côtés du cercle. */
    private static final int LINKS = 20;

    private double time;
    private int unlocked;
    /** Les dimensions dont le seuil est atteint (version 0.102) : un rond vert dans leur point. */
    private final boolean[] marked = new boolean[Dimension.values().length + 1];
    private IntConsumer onOpen = number -> { };
    /** Le défi final (version 0.160) : ouvert, l'avancée de chaque dimension (0 à 1), ses paliers et ce qui s'écrit dessous. */
    private boolean finalOpen, finished;
    private final double[] progress = new double[Dimension.values().length + 1];
    private final boolean[] reached = new boolean[Dimension.values().length + 1];
    private final String[] progressText = new String[Dimension.values().length + 1];
    private int tiers;
    /** Le temps de la fin qui se joue, en secondes ; négatif quand elle ne se joue pas. */
    private double finale = -1;
    private Runnable onFinale = () -> { };

    public DimensionsView() {
        super(800, 440);
        setOnMouseClicked(event -> {
            if (isFinalePlaying()) return;
            int number = dimensionAt(event.getX(), event.getY());
            if (number > 0 && number <= unlocked) onOpen.accept(number);
        });
    }

    /** Ce que fait un clic sur une dimension ouverte : son numéro, de 1 à 10. */
    public void setOnOpen(IntConsumer open) {
        onOpen = open == null ? number -> { } : open;
    }

    /** Le nombre de dimensions ouvertes (0 à 10). */
    public void setUnlocked(int count) {
        unlocked = Math.max(0, Math.min(Dimension.values().length, count));
    }

    public int unlocked() {
        return unlocked;
    }

    /** Marque une dimension (1 à 10) d'un rond vert : son seuil est atteint. */
    public void setMarked(int number, boolean mark) {
        if (number >= 1 && number < marked.length) marked[number] = mark;
    }

    public boolean isMarked(int number) {
        return number >= 1 && number < marked.length && marked[number];
    }

    /** Le défi final est ouvert (la bulle des racines achetée) : les points se remplissent. */
    public void setFinalOpen(boolean open) {
        finalOpen = open;
    }

    public boolean isFinalOpen() {
        return finalOpen;
    }

    /** L'avancée d'une dimension (1 à 10) vers son palier, de 0 à 1 ; son palier atteint ; et le texte sous son nom. */
    public void setFinalProgress(int number, double share, boolean tier, String text) {
        if (number < 1 || number >= progress.length) return;
        progress[number] = Math.max(0, Math.min(1, share));
        reached[number] = tier;
        progressText[number] = text;
    }

    /** La part de ce point remplie de vert : pour les vérifications. */
    public double filled(int number) {
        return !finalOpen || number < 1 || number >= progress.length ? 0 : reached[number] ? 1 : progress[number];
    }

    /** Le texte de l'avancée sous le nom d'une dimension, ou {@code null} : pour les vérifications. */
    public String progressText(int number) {
        return finalOpen && number >= 1 && number < progressText.length ? progressText[number] : null;
    }

    /** Les paliers atteints, écrits au milieu. */
    public void setFinalTiers(int count) {
        tiers = count;
    }

    /** « 7 / 10 paliers », au milieu de la carte. */
    public String tiersText() {
        return tiers + " / " + Dimension.values().length + (tiers > 1 ? " paliers" : " palier");
    }

    /** La partie est finie : les traits restent verts, avec leur aura dorée. */
    public void setFinished(boolean done) {
        finished = done;
    }

    /** Ce qui suit la fin, une fois le point du milieu grandi : l'écran de fin. */
    public void setOnFinale(Runnable action) {
        onFinale = action == null ? () -> { } : action;
    }

    /** Joue la fin depuis son début : la vague verte des traits, puis le point du milieu qui grandit. */
    public void playFinale() {
        finale = 0;
    }

    public boolean isFinalePlaying() {
        return finale >= 0;
    }

    /** Le temps de la fin qui se joue, en secondes, ou −1 : pour les vérifications. */
    public double finaleTime() {
        return finale;
    }

    /** Les traits déjà verts : tous une fois la partie finie hors de la fin, sinon ceux que la vague a passés. */
    public int greenLinks() {
        if (finale < 0) return finished ? LINKS : 0;
        return (int) Math.min(LINKS, Math.floor(finale / FINALE_WAVE_SECONDS * LINKS + 1e-9));
    }

    /** Le rayon du point du milieu : il grandit à la fin jusqu'à couvrir la vue. */
    public double centerRadius() {
        double small = dot() * 0.9;
        if (finale < FINALE_WAVE_SECONDS) return small;
        double t = Math.min(1, (finale - FINALE_WAVE_SECONDS) / FINALE_GROW_SECONDS);
        double cover = Math.hypot(getWidth(), getHeight()) / 2 + 4;
        return small + (cover - small) * t * t;
    }

    /** Un trait que la fin a passé : vert, dans une aura dorée. */
    private void goldLink(GraphicsContext g, double x1, double y1, double x2, double y2) {
        for (int k = 0; k <= 4; k++) {
            double t = k / 4.0;
            UniverseView.glow(g, x1 + (x2 - x1) * t, y1 + (y2 - y1) * t, dot() * 1.6, GOLD, 0.25 + 0.05 * Math.sin(time * 2 + k));
        }
        g.setStroke(MARK);
        g.setLineWidth(3);
        g.setGlobalAlpha(0.95);
        g.strokeLine(x1, y1, x2, y2);
    }

    /**
     * Le remplissage vert d'un point, de bas en haut : la part {@code share} de sa hauteur, en segment de disque ; plein
     * au palier atteint.
     */
    private static void fill(GraphicsContext g, double x, double y, double r, double share) {
        if (share <= 0) return;
        g.setGlobalAlpha(0.85);
        g.setFill(MARK);
        if (share >= 1) {
            g.fillOval(x - r, y - r, 2 * r, 2 * r);
            g.setGlobalAlpha(1);
            return;
        }
        // Le niveau de l'eau : de y + r (vide) à y − r (plein) ; le segment va de −θ à θ autour du bas du disque.
        double level = y + r - 2 * r * share, theta = Math.acos(Math.max(-1, Math.min(1, (level - y) / r)));
        int steps = 32;
        double[] px = new double[steps + 1], py = new double[steps + 1];
        for (int i = 0; i <= steps; i++) {
            double phi = -theta + 2 * theta * i / steps;
            px[i] = x + r * Math.sin(phi);
            py[i] = y + r * Math.cos(phi);
        }
        g.fillPolygon(px, py, steps + 1);
        g.setGlobalAlpha(1);
    }

    private double cx() {
        return getWidth() / 2;
    }

    private double cy() {
        return getHeight() / 2;
    }

    private double radius() {
        return Math.min(getWidth() * 0.4, getHeight() * 0.36);
    }

    /** La place du point de cette dimension (1 à 10) : en cercle, la première en haut. */
    public double x(int number) {
        return cx() + radius() * Math.cos(angle(number));
    }

    public double y(int number) {
        return cy() + radius() * Math.sin(angle(number));
    }

    private static double angle(int number) {
        return -Math.PI / 2 + 2 * Math.PI * (number - 1) / Dimension.values().length;
    }

    private double dot() {
        return Math.max(10, Math.min(getWidth(), getHeight()) * 0.035);
    }

    /** Le numéro de la dimension sous ce point de la vue, 0 s'il n'y en a pas. */
    public int dimensionAt(double x, double y) {
        for (int number = 1; number <= Dimension.values().length; number++) {
            if (Math.hypot(x - x(number), y - y(number)) <= dot() * 1.8) return number;
        }
        return 0;
    }

    /**
     * Une image : la carte, et la fin si elle se joue. La fin avance même animations coupées, comme le lancement d'un
     * univers ; finie, elle appelle l'écran de fin.
     */
    public void frame(double elapsed, boolean moving) {
        if (moving) time += Math.min(elapsed, 0.1);
        if (finale >= 0) {
            finale += Math.min(elapsed, 0.1);
            if (finale >= FINALE_WAVE_SECONDS + FINALE_GROW_SECONDS) {
                finale = -1;
                draw();
                onFinale.run();
                return;
            }
        }
        draw();
    }

    private void draw() {
        double w = getWidth(), h = getHeight();
        GraphicsContext g = CanvasPane.begin(this, BACK);
        if (g == null) return;
        g.setFill(Color.web("#dfe6ff"));
        for (int i = 0; i < 120; i++) {
            g.setGlobalAlpha(0.1 + 0.25 * (0.5 + 0.5 * Math.sin(time * 0.5 + i)));
            g.fillOval(UniverseView.noise(i * 2 + 5) * w, UniverseView.noise(i * 2 + 6) * h, 1.3, 1.3);
        }
        int count = Dimension.values().length;
        double dot = dot();
        // Le cercle, d'un point au suivant, et les rayons du milieu vers chacun ; verts et dorés quand la fin les a passés.
        int green = greenLinks();
        for (int number = 1; number <= count; number++) {
            int next = number % count + 1;
            boolean both = number <= unlocked && next <= unlocked;
            if (number - 1 + count < green) {
                goldLink(g, x(number), y(number), x(next), y(next));
            } else {
                g.setStroke(LINE);
                g.setLineWidth(1);
                g.setGlobalAlpha(both ? 0.35 : 0.1);
                g.strokeLine(x(number), y(number), x(next), y(next));
            }
            boolean open = number <= unlocked;
            if (number - 1 < green) {
                goldLink(g, cx(), cy(), x(number), y(number));
                continue;
            }
            g.setStroke(LINE);
            g.setLineWidth(open ? 2 : 1);
            g.setGlobalAlpha(open ? 0.45 : 0.12);
            g.strokeLine(cx(), cy(), x(number), y(number));
            if (open) {
                // Un éclat qui va du milieu vers la dimension.
                double t = (time * 0.4 + number * 0.13) % 1;
                UniverseView.glow(g, cx() + (x(number) - cx()) * t, cy() + (y(number) - cy()) * t, 5, ON, 0.8);
            }
        }
        // Le point du milieu, qui les relie tous.
        UniverseView.glow(g, cx(), cy(), dot * 3.2, CENTER, 0.35 + 0.1 * Math.sin(time * 2));
        g.setGlobalAlpha(1);
        g.setFill(CENTER);
        g.fillOval(cx() - dot * 0.9, cy() - dot * 0.9, dot * 1.8, dot * 1.8);
        if (finalOpen) {
            g.setTextAlign(TextAlignment.CENTER);
            g.setFill(Color.web("#e8f4ff"));
            g.setFont(Font.font(13));
            g.fillText(tiersText(), cx(), cy() + dot * 2.4);
        }
        // Les dix points.
        g.setTextAlign(TextAlignment.CENTER);
        for (int number = 1; number <= count; number++) {
            double x = x(number), y = y(number);
            boolean open = number <= unlocked;
            if (open) UniverseView.glow(g, x, y, dot * 2.4, ON, 0.4 + 0.15 * Math.sin(time * 2 + number));
            g.setGlobalAlpha(1);
            g.setFill(open ? ON : OFF);
            g.fillOval(x - dot, y - dot, 2 * dot, 2 * dot);
            if (open && finalOpen) fill(g, x, y, dot, filled(number));
            if (open && marked[number]) {
                // Le seuil atteint : un rond vert dans le point, autour du numéro ; avec le défi final, un anneau autour
                // du point, que le remplissage laisse voir.
                double ring = finalOpen ? 1.3 : 0.72;
                g.setStroke(MARK);
                g.setLineWidth(Math.max(2, dot * 0.16));
                g.setGlobalAlpha(0.9 + 0.1 * Math.sin(time * 3));
                g.strokeOval(x - dot * ring, y - dot * ring, dot * 2 * ring, dot * 2 * ring);
                g.setGlobalAlpha(1);
            }
            g.setFill(open ? Color.web("#1a1430") : Color.web("#8a91b0"));
            g.setFont(Font.font(dot * 1.05));
            g.fillText(open ? String.valueOf(number) : "?", x, y + dot * 0.38);
            // Le nom, à l'extérieur du cercle.
            double out = 1 + (dot * 2.6) / radius();
            double lx = cx() + (x - cx()) * out, ly = cy() + (y - cy()) * out + 4;
            g.setFont(Font.font(12));
            g.setFill(open ? ON : OFF);
            g.fillText(open ? Dimension.of(number).label() : "Dimension " + number, lx, ly);
            if (open && finalOpen && progressText[number] != null) {
                // L'avancée, sous le nom ; en haut de la carte, au-dessus de lui, pour ne pas toucher le point. Sur les
                // côtés, à mi-hauteur, elle part du point vers l'extérieur.
                double side = x - cx();
                boolean aside = Math.abs(y - cy()) < radius() * 0.5;
                g.setTextAlign(!aside ? TextAlignment.CENTER : side > 0 ? TextAlignment.LEFT : TextAlignment.RIGHT);
                double px = aside ? x + Math.signum(side) * dot * 1.5 : lx, py = aside ? y + dot + 14 : y < cy() ? ly - 14 : ly + 14;
                g.setFont(Font.font(11));
                g.setFill(reached[number] ? MARK : Color.web("#c8d4ea"));
                g.fillText(progressText[number], px, py);
                g.setTextAlign(TextAlignment.CENTER);
            }
        }
        // La fin : le point du milieu grandit, doré, jusqu'à couvrir la vue.
        if (finale >= FINALE_WAVE_SECONDS) {
            double r = centerRadius();
            UniverseView.glow(g, cx(), cy(), r * 1.3, GOLD, 0.9);
            g.setGlobalAlpha(1);
            g.setFill(CENTER);
            g.fillOval(cx() - r, cy() - r, 2 * r, 2 * r);
        }
        g.setTextAlign(TextAlignment.LEFT);
        g.setGlobalAlpha(1);
    }
}
