package idle.ui;

import idle.core.universe.Skin;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.FillRule;

/**
 * Le rideau de la transition entre univers (version 0.199), à côté de {@link TransitionPane} : un calque sur toute la
 * fenêtre, qui ne prend aucun clic, et qui joue l'effet du skin choisi quand le joueur confirme la transition, pendant
 * que la partie suivante se met en place dessous. Sans skin, rien n'est dessiné : le classique n'a pas de rideau.
 * <ul>
 * <li>« Fondu au blanc » : tout blanchit vite, puis le blanc se dissipe ;</li>
 * <li>« Page tournée » : une feuille de papier balaie la fenêtre de droite à gauche, son ombre devant elle ;</li>
 * <li>« Diaphragme » : un iris noir se ferme sur le centre, puis se rouvre ;</li>
 * <li>« Trou noir » : un disque noir cerclé de violet grossit depuis le centre jusqu'à tout avaler, puis s'efface.</li>
 * </ul>
 */
final class TransitionSkinArt extends Canvas {

    /** La durée de l'effet, en secondes. */
    static final double SECONDS = 1.4;
    private static final Color PAPER = Color.web("#f6efdc");
    private static final Color RING = Color.web("#7a3cff");

    private Skin skin;
    /** Le temps écoulé depuis le début de l'effet, ou {@code -1} au repos. */
    private double time = -1;

    TransitionSkinArt() {
        setMouseTransparent(true);
    }

    /** Lance l'effet de ce skin ; {@code null} (le classique) n'en a pas. */
    void play(Skin skin) {
        if (skin == null || skin.visual() != idle.core.universe.SkinVisual.TRANSITION) return;
        this.skin = skin;
        time = 0;
        setVisible(true);
        draw();
    }

    /** Vrai tant que l'effet joue. */
    boolean isPlaying() {
        return time >= 0;
    }

    /** Fait avancer l'effet ; au bout, le calque s'efface. */
    void frame(double dt) {
        if (time < 0) return;
        time += dt;
        if (time >= SECONDS) {
            time = -1;
            getGraphicsContext2D().clearRect(0, 0, getWidth(), getHeight());
            setVisible(false);
            return;
        }
        draw();
    }

    /** Prend la taille de la fenêtre. */
    void fit(double width, double height) {
        setWidth(width);
        setHeight(height);
        if (time >= 0) draw();
    }

    private void draw() {
        double w = getWidth(), h = getHeight();
        GraphicsContext g = getGraphicsContext2D();
        g.clearRect(0, 0, w, h);
        if (w <= 0 || h <= 0) return;
        double t = time / SECONDS;
        double cx = w / 2, cy = h / 2;
        double reach = Math.hypot(cx, cy) + 4;
        switch (skin) {
            case TRANSITION_WHITE -> {
                // Montée en 30 % du temps, plein pendant 20 %, puis retombée.
                double alpha = t < 0.3 ? t / 0.3 : t < 0.5 ? 1 : 1 - (t - 0.5) / 0.5;
                g.setFill(Color.color(1, 1, 1, Math.max(0, Math.min(1, alpha))));
                g.fillRect(0, 0, w, h);
            }
            case TRANSITION_PAGE -> {
                // La feuille entre par la droite, couvre tout à mi-temps, puis sort par la gauche.
                double x = w - 2 * w * t;
                g.setFill(Color.color(0, 0, 0, 0.35));
                g.fillRect(x - 14, 0, 14, h);
                g.setFill(PAPER);
                g.fillRect(x, 0, w + 20, h);
                g.setStroke(Color.web("#c9b690"));
                g.setLineWidth(1);
                for (double y = 36; y < h; y += 28) g.strokeLine(x + 24, y, x + w - 24, y);
            }
            case TRANSITION_IRIS -> {
                double radius = reach * Math.abs(1 - 2 * t);
                g.setFill(Color.BLACK);
                g.setFillRule(FillRule.EVEN_ODD);
                g.beginPath();
                g.rect(0, 0, w, h);
                g.arc(cx, cy, radius, radius, 0, 360);
                g.closePath();
                g.fill();
                g.setFillRule(FillRule.NON_ZERO);
                g.setStroke(Color.color(1, 1, 1, 0.8));
                g.setLineWidth(3);
                g.strokeOval(cx - radius, cy - radius, 2 * radius, 2 * radius);
            }
            case TRANSITION_HOLE -> {
                // Le disque grossit pendant 60 % du temps, puis tout s'efface.
                double grow = Math.min(1, t / 0.6);
                double radius = reach * grow;
                double alpha = t < 0.6 ? 1 : 1 - (t - 0.6) / 0.4;
                g.setFill(Color.color(0, 0, 0, alpha));
                g.fillOval(cx - radius, cy - radius, 2 * radius, 2 * radius);
                g.setStroke(Color.color(RING.getRed(), RING.getGreen(), RING.getBlue(), alpha));
                g.setLineWidth(6 + 10 * grow);
                g.strokeOval(cx - radius, cy - radius, 2 * radius, 2 * radius);
            }
            default -> { }
        }
    }
}
