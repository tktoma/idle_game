package idle.ui.common;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;

/**
 * La petite icône animée d'un onglet (version 0.59), dans la couleur de l'onglet : des particules
 * qui tournent, un engrenage, une spirale de matière noire, deux faisceaux qui se croisent, un
 * éclair de Big Bang, une étoile qui scintille, des barres qui montent, des curseurs qui glissent.
 * Elle ne bouge que si les effets sont permis ; immobile, elle reste lisible.
 */
public final class TabIcon extends Canvas {

    /** Ce que montre l'icône : un dessin par onglet. */
    public enum Kind { PARTICLES, AUTOMATION, DARK_MATTER, COLLIDER, BIG_BANG, ACHIEVEMENTS, STATS, SETTINGS }

    private final Kind kind;
    private final Color color;
    private double time;
    private boolean bright;
    /** La vitesse de l'animation : 1 d'habitude, plus quand ce que montre l'onglet s'accélère (version 0.61). */
    private double speed = 1;

    public void setSpeed(double speed) {
        this.speed = Double.isFinite(speed) ? Math.max(0.2, Math.min(speed, 8)) : 1;
    }

    public TabIcon(Kind kind, String color, double size) {
        super(size, size);
        this.kind = kind;
        this.color = Color.web(color);
        setMouseTransparent(true);
        draw();
    }

    public Kind kind() {
        return kind;
    }

    /** Le temps de l'animation : pour les vérifications. */
    public double time() {
        return time;
    }

    /** L'onglet affiché a son icône pleinement éclairée ; les autres la gardent plus sourde. */
    public void setBright(boolean bright) {
        if (this.bright == bright) return;
        this.bright = bright;
        draw();
    }

    /** Fait avancer l'animation ; {@code moving} faux (effets coupés) la laisse immobile. */
    public void frame(double elapsed, boolean moving) {
        if (moving) time += elapsed * speed;
        draw();
    }

    private void draw() {
        double s = getWidth();
        GraphicsContext g = getGraphicsContext2D();
        g.clearRect(0, 0, s, s);
        g.setGlobalAlpha(bright ? 1 : 0.7);
        g.setStroke(color);
        g.setFill(color);
        g.setLineCap(StrokeLineCap.ROUND);
        double c = s / 2;
        switch (kind) {
            case PARTICLES -> {
                g.fillOval(c - 2.5, c - 2.5, 5, 5);
                for (int k = 0; k < 3; k++) {
                    double a = time * (1.6 + 0.5 * k) + k * 2.1;
                    double r = s * (0.22 + 0.08 * k);
                    g.fillOval(c + r * Math.cos(a) - 1.6, c + r * Math.sin(a) - 1.6, 3.2, 3.2);
                }
            }
            case AUTOMATION -> {
                double a0 = time * 0.9;
                g.setLineWidth(s * 0.13);
                for (int k = 0; k < 8; k++) {
                    double a = a0 + k * Math.PI / 4;
                    g.strokeLine(c + s * 0.26 * Math.cos(a), c + s * 0.26 * Math.sin(a), c + s * 0.42 * Math.cos(a), c + s * 0.42 * Math.sin(a));
                }
                g.setLineWidth(s * 0.12);
                g.strokeOval(c - s * 0.24, c - s * 0.24, s * 0.48, s * 0.48);
            }
            case DARK_MATTER -> {
                g.setLineWidth(1.6);
                for (int arm = 0; arm < 2; arm++) {
                    double previousX = c, previousY = c;
                    for (int k = 1; k <= 14; k++) {
                        double t = k / 14.0;
                        double a = -time * 1.4 + arm * Math.PI + t * 3.2;
                        double r = t * s * 0.44;
                        double x = c + r * Math.cos(a), y = c + r * Math.sin(a);
                        g.setGlobalAlpha((bright ? 1 : 0.7) * (1 - 0.6 * t));
                        g.strokeLine(previousX, previousY, x, y);
                        previousX = x;
                        previousY = y;
                    }
                }
                g.setGlobalAlpha(bright ? 1 : 0.7);
                double pulse = 3 + 1.2 * Math.sin(time * 3);
                g.fillOval(c - pulse / 2, c - pulse / 2, pulse, pulse);
            }
            case COLLIDER -> {
                double rx = s * 0.42, ry = s * 0.26;
                g.setLineWidth(1.4);
                g.strokeOval(c - rx, c - ry, 2 * rx, 2 * ry);
                double a = time * 2.4;
                double meet = Math.abs(Math.cos(a));
                g.fillOval(c + rx * Math.sin(a) - 2, c - ry * Math.cos(a) - 2, 4, 4);
                g.fillOval(c - rx * Math.sin(a) - 2, c - ry * Math.cos(a) - 2, 4, 4);
                // Ils se croisent en bas : un éclat.
                if (Math.cos(a) < -0.9) {
                    g.setGlobalAlpha((meet - 0.9) * 10);
                    g.fillOval(c - 4, c + ry - 4, 8, 8);
                }
            }
            case BIG_BANG -> {
                double pulse = 0.5 + 0.5 * Math.sin(time * 2.2);
                g.setLineWidth(1.5);
                for (int k = 0; k < 8; k++) {
                    double a = k * Math.PI / 4 + time * 0.3;
                    double inner = s * 0.12, outer = s * (0.3 + 0.14 * (k % 2 == 0 ? pulse : 1 - pulse));
                    g.strokeLine(c + inner * Math.cos(a), c + inner * Math.sin(a), c + outer * Math.cos(a), c + outer * Math.sin(a));
                }
                g.fillOval(c - 2.5, c - 2.5, 5, 5);
            }
            case ACHIEVEMENTS -> {
                double twinkle = 0.85 + 0.15 * Math.sin(time * 3);
                double[] xs = new double[10], ys = new double[10];
                for (int k = 0; k < 10; k++) {
                    double a = -Math.PI / 2 + k * Math.PI / 5;
                    double r = (k % 2 == 0 ? s * 0.44 : s * 0.19) * twinkle;
                    xs[k] = c + r * Math.cos(a);
                    ys[k] = c + r * Math.sin(a);
                }
                g.fillPolygon(xs, ys, 10);
                // Un éclat qui fait le tour.
                double a = time * 1.7;
                g.setFill(Color.WHITE);
                g.setGlobalAlpha(0.8);
                g.fillOval(c + s * 0.4 * Math.cos(a) - 1.2, c + s * 0.4 * Math.sin(a) - 1.2, 2.4, 2.4);
            }
            case STATS -> {
                double w = s * 0.18;
                for (int k = 0; k < 3; k++) {
                    double h = s * (0.3 + 0.2 * k + 0.12 * Math.sin(time * 2 + k * 1.3));
                    g.fillRoundRect(s * 0.12 + k * (w + s * 0.1), s * 0.88 - h, w, h, 2, 2);
                }
            }
            case SETTINGS -> {
                g.setLineWidth(1.4);
                for (int k = 0; k < 3; k++) {
                    double y = s * (0.25 + 0.25 * k);
                    g.strokeLine(s * 0.12, y, s * 0.88, y);
                    double x = s * (0.5 + 0.3 * Math.sin(time * 0.9 + k * 2));
                    g.fillOval(x - 2.6, y - 2.6, 5.2, 5.2);
                }
            }
        }
        g.setGlobalAlpha(1);
    }
}
