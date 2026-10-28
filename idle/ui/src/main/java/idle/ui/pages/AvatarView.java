package idle.ui.pages;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;

/**
 * Un avatar dans son rond (version 0.147) : l'un de ceux que dessine le jeu ({@link Avatar}), ou l'image importée par
 * le joueur, recadrée au carré du milieu puis au cercle. Il ne se redessine que lorsqu'on lui donne autre chose à
 * montrer.
 */
public final class AvatarView extends Canvas {

    private static final String BACKGROUND = "#0e1420";

    private Avatar avatar = Avatar.PARTICLE;
    private Image image;

    public AvatarView(double size) {
        super(size, size);
        setMouseTransparent(true);
        draw();
    }

    /** Montre un avatar du jeu. */
    public void show(Avatar shown) {
        Avatar wanted = shown == null ? Avatar.PARTICLE : shown;
        if (image == null && wanted == avatar) return;
        avatar = wanted;
        image = null;
        draw();
    }

    /** Montre une image ; {@code null} ou une image illisible ramène l'avatar par défaut. */
    public void show(Image shown) {
        if (shown == null || shown.isError() || shown.getWidth() <= 0 || shown.getHeight() <= 0) {
            show(Avatar.PARTICLE);
            return;
        }
        if (shown == image) return;
        image = shown;
        draw();
    }

    /** Ce qui est montré : l'identifiant de l'avatar, ou {@link Avatar#CUSTOM} pour une image. */
    public String shown() {
        return image != null ? Avatar.CUSTOM : avatar.id();
    }

    private void draw() {
        double s = getWidth();
        GraphicsContext g = getGraphicsContext2D();
        g.clearRect(0, 0, s, s);
        g.save();
        g.beginPath();
        g.arc(s / 2, s / 2, s / 2 - 1, s / 2 - 1, 0, 360);
        g.closePath();
        g.clip();
        g.setFill(Color.web(BACKGROUND));
        g.fillRect(0, 0, s, s);
        if (image != null) {
            double side = Math.min(image.getWidth(), image.getHeight());
            g.drawImage(image, (image.getWidth() - side) / 2, (image.getHeight() - side) / 2, side, side, 0, 0, s, s);
        } else {
            paint(g, avatar, s);
        }
        g.restore();
        g.setGlobalAlpha(1);
        g.setStroke(Color.web(image != null ? "#d6dde6" : avatar.color()));
        g.setLineWidth(Math.max(1.5, s / 40));
        g.strokeOval(1, 1, s - 2, s - 2);
    }

    /** Dessine un avatar du jeu sur un carré de côté {@code s}, déjà découpé en rond. */
    static void paint(GraphicsContext g, Avatar avatar, double s) {
        Color color = Color.web(avatar.color());
        double c = s / 2;
        g.setLineCap(StrokeLineCap.ROUND);
        // Un halo de la couleur de l'avatar, au fond.
        for (int k = 0; k < 4; k++) {
            g.setGlobalAlpha(0.06);
            g.setFill(color);
            double r = s * (0.48 - 0.08 * k);
            g.fillOval(c - r, c - r, 2 * r, 2 * r);
        }
        g.setGlobalAlpha(1);
        g.setFill(color);
        g.setStroke(color);
        switch (avatar) {
            case PARTICLE -> {
                g.setLineWidth(s / 60);
                g.setGlobalAlpha(0.35);
                g.strokeOval(c - s * 0.3, c - s * 0.3, s * 0.6, s * 0.6);
                g.strokeOval(c - s * 0.18, c - s * 0.18, s * 0.36, s * 0.36);
                g.setGlobalAlpha(1);
                dot(g, c, c, s * 0.09);
                dot(g, c + s * 0.3, c, s * 0.04);
                dot(g, c - s * 0.13, c - s * 0.11, s * 0.035);
                dot(g, c - s * 0.21, c + s * 0.21, s * 0.04);
            }
            case ATOM -> {
                g.setLineWidth(s / 45);
                for (int k = 0; k < 3; k++) {
                    g.save();
                    g.translate(c, c);
                    g.rotate(k * 60);
                    g.strokeOval(-s * 0.36, -s * 0.13, s * 0.72, s * 0.26);
                    g.restore();
                }
                g.setFill(Color.web("#ff8a75"));
                dot(g, c - s * 0.035, c - s * 0.02, s * 0.05);
                dot(g, c + s * 0.03, c + s * 0.03, s * 0.05);
                g.setFill(Color.web("#9fd0ff"));
                dot(g, c + s * 0.035, c - s * 0.035, s * 0.05);
                dot(g, c - s * 0.03, c + s * 0.035, s * 0.05);
            }
            case MOLECULE -> {
                g.setLineWidth(s / 22);
                g.setStroke(Color.web("#8fa3b8"));
                g.strokeLine(c, c + s * 0.04, c - s * 0.24, c + s * 0.2);
                g.strokeLine(c, c + s * 0.04, c + s * 0.24, c + s * 0.2);
                g.setFill(Color.web("#ff8a75"));
                dot(g, c, c + s * 0.02, s * 0.16);
                g.setFill(color);
                dot(g, c - s * 0.25, c + s * 0.21, s * 0.1);
                dot(g, c + s * 0.25, c + s * 0.21, s * 0.1);
                g.setFill(Color.WHITE);
                g.setGlobalAlpha(0.5);
                dot(g, c - s * 0.05, c - s * 0.04, s * 0.04);
            }
            case DARK_MATTER -> {
                g.setLineWidth(s / 36);
                for (int arm = 0; arm < 2; arm++) {
                    double previousX = c, previousY = c;
                    for (int k = 1; k <= 24; k++) {
                        double t = k / 24.0;
                        double a = arm * Math.PI + t * 3.6;
                        double r = t * s * 0.42;
                        double x = c + r * Math.cos(a), y = c + r * Math.sin(a);
                        g.setGlobalAlpha(1 - 0.7 * t);
                        g.strokeLine(previousX, previousY, x, y);
                        previousX = x;
                        previousY = y;
                    }
                }
                g.setGlobalAlpha(1);
                g.setFill(Color.web("#1a0f2e"));
                dot(g, c, c, s * 0.09);
                g.setFill(color);
                dot(g, c, c, s * 0.04);
            }
            case QUARK -> {
                double[][] at = {{c, c - s * 0.2}, {c - s * 0.19, c + s * 0.13}, {c + s * 0.19, c + s * 0.13}};
                g.setLineWidth(s / 50);
                g.setGlobalAlpha(0.6);
                for (int k = 0; k < 3; k++) {
                    double[] from = at[k], to = at[(k + 1) % 3];
                    wave(g, from[0], from[1], to[0], to[1], s * 0.03);
                }
                g.setGlobalAlpha(1);
                String[] colors = {"#ff6b6b", "#6bdc8b", "#6ba8ff"};
                for (int k = 0; k < 3; k++) {
                    g.setFill(Color.web(colors[k]));
                    dot(g, at[k][0], at[k][1], s * 0.09);
                }
            }
            case COMET -> {
                double hx = c + s * 0.17, hy = c - s * 0.14;
                for (int k = 0; k < 6; k++) {
                    double t = k / 6.0;
                    g.setGlobalAlpha(0.5 * (1 - t));
                    double w = s * (0.1 - 0.012 * k);
                    double x = hx - s * 0.08 * (k + 1), y = hy + s * 0.07 * (k + 1);
                    g.fillOval(x - w, y - w, 2 * w, 2 * w);
                }
                g.setGlobalAlpha(1);
                dot(g, hx, hy, s * 0.1);
                g.setFill(Color.WHITE);
                dot(g, hx + s * 0.02, hy - s * 0.02, s * 0.04);
            }
            case STAR -> {
                double[] xs = new double[16], ys = new double[16];
                for (int k = 0; k < 16; k++) {
                    double a = -Math.PI / 2 + k * Math.PI / 8;
                    double r = k % 2 == 0 ? s * 0.4 : s * 0.22;
                    xs[k] = c + r * Math.cos(a);
                    ys[k] = c + r * Math.sin(a);
                }
                g.setGlobalAlpha(0.55);
                g.fillPolygon(xs, ys, 16);
                g.setGlobalAlpha(1);
                dot(g, c, c, s * 0.2);
                g.setFill(Color.WHITE);
                g.setGlobalAlpha(0.6);
                dot(g, c - s * 0.05, c - s * 0.05, s * 0.08);
            }
            case PLANET -> {
                g.setLineWidth(s / 30);
                g.setStroke(Color.web("#e6d27a"));
                g.save();
                g.translate(c, c);
                g.rotate(-18);
                g.strokeArc(-s * 0.42, -s * 0.11, s * 0.84, s * 0.22, 180, 180, ArcType.OPEN);
                g.restore();
                dot(g, c, c, s * 0.24);
                g.setFill(Color.web("#4f9a7a"));
                g.setGlobalAlpha(0.8);
                g.fillOval(c - s * 0.2, c - s * 0.08, s * 0.4, s * 0.06);
                g.fillOval(c - s * 0.22, c + s * 0.05, s * 0.44, s * 0.05);
                g.setGlobalAlpha(1);
                g.save();
                g.translate(c, c);
                g.rotate(-18);
                g.strokeArc(-s * 0.42, -s * 0.11, s * 0.84, s * 0.22, 0, 180, ArcType.OPEN);
                g.restore();
            }
            case BLACK_HOLE -> {
                g.setLineWidth(s / 16);
                g.setGlobalAlpha(0.8);
                g.strokeOval(c - s * 0.38, c - s * 0.13, s * 0.76, s * 0.26);
                g.setGlobalAlpha(1);
                g.setFill(Color.BLACK);
                dot(g, c, c, s * 0.17);
                g.setLineWidth(s / 60);
                g.setStroke(Color.web("#ffd8a8"));
                g.strokeOval(c - s * 0.18, c - s * 0.18, s * 0.36, s * 0.36);
            }
            case GALAXY -> {
                for (int arm = 0; arm < 3; arm++) {
                    for (int p = 1; p <= 12; p++) {
                        double t = p / 12.0;
                        double a = arm * 2 * Math.PI / 3 + t * 3.4;
                        double r = s * 0.42 * t;
                        double dot = s * (0.05 - 0.03 * t);
                        g.setGlobalAlpha(1 - 0.5 * t);
                        g.fillOval(c + r * Math.cos(a) - dot / 2, c + r * Math.sin(a) * 0.8 - dot / 2, dot, dot);
                    }
                }
                g.setGlobalAlpha(1);
                g.setFill(Color.WHITE);
                dot(g, c, c, s * 0.06);
            }
            case SEED -> {
                g.setLineWidth(s / 30);
                g.setStroke(Color.web("#9be7a8"));
                g.beginPath();
                g.moveTo(c, c - s * 0.02);
                g.bezierCurveTo(c - s * 0.02, c - s * 0.16, c + s * 0.06, c - s * 0.24, c + s * 0.02, c - s * 0.33);
                g.stroke();
                g.setFill(Color.web("#9be7a8"));
                g.fillOval(c + s * 0.02, c - s * 0.34, s * 0.16, s * 0.08);
                g.fillOval(c - s * 0.15, c - s * 0.25, s * 0.15, s * 0.07);
                g.setFill(color);
                g.fillOval(c - s * 0.17, c - s * 0.04, s * 0.34, s * 0.3);
                g.setFill(Color.web("#8a7a3a"));
                g.setGlobalAlpha(0.6);
                g.fillOval(c - s * 0.08, c + s * 0.04, s * 0.08, s * 0.12);
                g.setGlobalAlpha(1);
            }
            case WORLD_TREE -> {
                g.setStroke(Color.web("#b08a5a"));
                g.setLineWidth(s / 14);
                g.strokeLine(c, c + s * 0.3, c, c - s * 0.02);
                g.setLineWidth(s / 30);
                g.strokeLine(c, c + s * 0.06, c - s * 0.16, c - s * 0.1);
                g.strokeLine(c, c + s * 0.02, c + s * 0.17, c - s * 0.12);
                g.strokeLine(c, c + s * 0.3, c - s * 0.18, c + s * 0.38);
                g.strokeLine(c, c + s * 0.3, c + s * 0.18, c + s * 0.38);
                g.setFill(color);
                double[][] leaves = {{0, -0.2, 0.17}, {-0.17, -0.13, 0.12}, {0.18, -0.15, 0.12}, {-0.08, -0.3, 0.1}, {0.1, -0.3, 0.1}};
                for (double[] leaf : leaves) dot(g, c + s * leaf[0], c + s * leaf[1], s * leaf[2]);
                g.setFill(Color.web("#fff1c9"));
                for (double[] leaf : leaves) dot(g, c + s * leaf[0] + s * 0.03, c + s * leaf[1] - s * 0.02, s * 0.018);
            }
        }
        g.setGlobalAlpha(1);
    }

    /** Un rond plein de rayon {@code r} centré en ({@code x}, {@code y}). */
    private static void dot(GraphicsContext g, double x, double y, double r) {
        g.fillOval(x - r, y - r, 2 * r, 2 * r);
    }

    /** Un trait ondulé d'un point à l'autre : le gluon qui lie deux quarks. */
    private static void wave(GraphicsContext g, double x1, double y1, double x2, double y2, double amplitude) {
        double dx = x2 - x1, dy = y2 - y1;
        double length = Math.hypot(dx, dy);
        double nx = -dy / length, ny = dx / length;
        double previousX = x1, previousY = y1;
        for (int k = 1; k <= 16; k++) {
            double t = k / 16.0;
            double offset = amplitude * Math.sin(t * Math.PI * 6);
            double x = x1 + dx * t + nx * offset, y = y1 + dy * t + ny * offset;
            g.strokeLine(previousX, previousY, x, y);
            previousX = x;
            previousY = y;
        }
    }
}
