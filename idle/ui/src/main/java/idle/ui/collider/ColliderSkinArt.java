package idle.ui.collider;

import idle.core.universe.Skin;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/**
 * Les quatre skins de l'anneau du collisionneur de la version 0.191, dessinés à côté de {@link EnergyView}
 * pour la garder lisible : le fond, les poussières qui le peuplent et l'anneau lui-même.
 * <ul>
 * <li>« Bois et cuivre » : des lattes vernies cerclées de bandes de cuivre, de la sciure qui tombe ;</li>
 * <li>« Circuit imprimé » : une piste verte à pastilles dorées sur une carte vert sombre, des bits qui défilent ;</li>
 * <li>« Stade » : une piste d'athlétisme à huit couloirs sur la pelouse, le détecteur en ligne d'arrivée, les
 * projecteurs allumés ;</li>
 * <li>« Ruban de Möbius » : l'anneau est un ruban qui se tord une fois ; les faisceaux passent dessus sur une
 * moitié et dessous sur l'autre ({@link #under(double)}).</li>
 * </ul>
 */
final class ColliderSkinArt {

    private static final Color WOOD = Color.web("#8a5a2a");
    private static final Color WOOD_LIGHT = Color.web("#c08a4a");
    private static final Color COPPER = Color.web("#d98a4a");
    private static final Color SAWDUST = Color.web("#e8c88a");
    private static final Color TRACE = Color.web("#3fd05a");
    private static final Color PAD = Color.web("#ffd27f");
    private static final Color BIT = Color.web("#7fff9a");
    private static final Color TRACK = Color.web("#c0503a");
    private static final Color LANE = Color.web("#f4f4f4");
    private static final Color RIBBON_FRONT = Color.web("#9fb8ff");
    private static final Color RIBBON_BACK = Color.web("#3a4a8a");
    private static final Color RIBBON_EDGE = Color.web("#e8f0ff");

    /** Largeur du ruban de Möbius vu de face, en pixels. */
    private static final double RIBBON_WIDTH = 22;

    private ColliderSkinArt() {
    }

    /** Vrai pour les anneaux dessinés ici : ceux de la version 0.191. */
    static boolean draws(Skin look) {
        return look == Skin.COLLIDER_WOOD || look == Skin.COLLIDER_CIRCUIT || look == Skin.COLLIDER_STADIUM || look == Skin.COLLIDER_MOBIUS;
    }

    /** Vrai pour les anneaux qui remplacent les poussières du fond : bois, circuit et stade ; le ruban garde les étoiles. */
    static boolean dusts(Skin look) {
        return look == Skin.COLLIDER_WOOD || look == Skin.COLLIDER_CIRCUIT || look == Skin.COLLIDER_STADIUM;
    }

    /** La couleur du fond sous ce skin. */
    static Color background(Skin look) {
        return switch (look) {
            case COLLIDER_WOOD -> Color.web("#1a1008");
            case COLLIDER_CIRCUIT -> Color.web("#0b2a14");
            case COLLIDER_STADIUM -> Color.web("#0e2a12");
            default -> Color.web("#0a0c18");
        };
    }

    /** Les poussières du fond : de la sciure, des bits, ou les projecteurs et la foule du stade. */
    static void dust(GraphicsContext g, EnergyView view, Skin look) {
        double w = view.getWidth(), h = view.getHeight(), time = view.time();
        switch (look) {
            case COLLIDER_WOOD -> {
                g.setFill(SAWDUST);
                for (int i = 0; i < 50; i++) {
                    double x = (i * 0.754877666 % 1) * w + 6 * Math.sin(time * 0.8 + i);
                    double y = ((i * 0.569840291 + time * 0.04 * (1 + i % 3)) % 1) * h;
                    g.setGlobalAlpha(0.25 + 0.3 * (i % 4) / 4.0);
                    g.fillRect(x, y, 2.5, 1.2);
                }
            }
            case COLLIDER_CIRCUIT -> {
                g.setFill(BIT);
                g.setFont(Font.font("Monospaced", 9));
                for (int i = 0; i < 40; i++) {
                    double x = (i * 0.754877666 % 1) * w;
                    double y = ((i * 0.569840291 + time * 0.05 * (1 + i % 3)) % 1) * h;
                    g.setGlobalAlpha(0.15 + 0.3 * (0.5 + 0.5 * Math.sin(time * 2 + i)));
                    g.fillText(Math.sin(i * 7.3 + Math.floor(time)) > 0 ? "1" : "0", x, y);
                }
            }
            default -> {
                // Les projecteurs aux quatre coins, et la foule : des points de couleur dans les gradins.
                for (int k = 0; k < 4; k++) {
                    double x = k % 2 == 0 ? 10 : w - 10, y = k < 2 ? 10 : h - 10;
                    EnergyView.glow(g, x, y, 60, Color.web("#fff6d8"), 0.25);
                    g.setGlobalAlpha(1);
                    g.setFill(Color.web("#fff6d8"));
                    g.fillRect(x - 4, y - 3, 8, 6);
                }
                for (int i = 0; i < 90; i++) {
                    double x = (i * 0.754877666 % 1) * w, y = ((i * 0.569840291) % 1) * h;
                    if (Math.abs(x - view.cx()) < view.rx() + 30 && Math.abs(y - view.cy()) < view.ry() + 30) continue;
                    g.setGlobalAlpha(0.3 + 0.3 * (0.5 + 0.5 * Math.sin(time * 3 + i)));
                    g.setFill(i % 3 == 0 ? Color.web("#ff8a8a") : i % 3 == 1 ? Color.web("#8ab4ff") : Color.web("#ffe08a"));
                    g.fillOval(x, y, 2.2, 2.2);
                }
            }
        }
        g.setGlobalAlpha(1);
    }

    /** L'anneau sous ce skin. */
    static void ring(GraphicsContext g, EnergyView view, Skin look) {
        switch (look) {
            case COLLIDER_WOOD -> wood(g, view);
            case COLLIDER_CIRCUIT -> circuit(g, view);
            case COLLIDER_STADIUM -> stadium(g, view);
            default -> mobius(g, view);
        }
        g.setGlobalAlpha(1);
    }

    /** Un segment de l'anneau entre deux angles, à une distance {@code offset} de sa ligne (vers l'extérieur si positif). */
    private static void arc(GraphicsContext g, EnergyView view, double from, double to, double offset, Color color, double width, double alpha) {
        int steps = Math.max(2, (int) Math.ceil((to - from) / (2 * Math.PI) * 72));
        double[] xs = new double[steps + 1], ys = new double[steps + 1];
        for (int k = 0; k <= steps; k++) {
            double a = from + (to - from) * k / steps;
            double x = view.px(a), y = view.py(a);
            double nx = x - view.cx(), ny = y - view.cy(), length = Math.max(1e-9, Math.hypot(nx, ny));
            xs[k] = x + nx / length * offset;
            ys[k] = y + ny / length * offset;
        }
        g.setStroke(color);
        g.setLineWidth(width);
        g.setGlobalAlpha(alpha);
        g.strokePolyline(xs, ys, steps + 1);
    }

    /** Le bois : des lattes vernies, le fil du bois dessus, et une bande de cuivre entre chaque latte. */
    private static void wood(GraphicsContext g, EnergyView view) {
        int slats = 16;
        double time = view.time();
        for (int k = 0; k < slats; k++) {
            double from = 2 * Math.PI * k / slats + 0.03, to = 2 * Math.PI * (k + 1) / slats - 0.03;
            Color tone = k % 2 == 0 ? WOOD : WOOD_LIGHT;
            arc(g, view, from, to, 0, tone, 14, 1);
            // Le fil du bois : deux veines plus sombres, et le vernis qui brille quand la lumière passe.
            arc(g, view, from, to, -3, Color.web("#5a3414"), 1, 0.6);
            arc(g, view, from, to, 3.5, Color.web("#5a3414"), 0.8, 0.5);
            double shine = Math.max(0, Math.sin(time * 1.2 - k * 0.4));
            arc(g, view, from, to, 5, Color.WHITE, 1.5, 0.25 * shine);
        }
        for (int k = 0; k < slats; k++) {
            double a = 2 * Math.PI * k / slats;
            double x = view.px(a), y = view.py(a);
            double nx = x - view.cx(), ny = y - view.cy(), length = Math.max(1e-9, Math.hypot(nx, ny));
            nx /= length;
            ny /= length;
            g.setStroke(COPPER);
            g.setLineWidth(3.5);
            g.setGlobalAlpha(1);
            g.strokeLine(x - nx * 9, y - ny * 9, x + nx * 9, y + ny * 9);
            g.setStroke(Color.web("#fff0d8"));
            g.setLineWidth(1);
            g.setGlobalAlpha(0.6);
            g.strokeLine(x - nx * 9, y - ny * 9, x + nx * 9, y + ny * 9);
        }
    }

    /** Le circuit : une piste verte double, des pastilles dorées aux angles droits, des composants autour. */
    private static void circuit(GraphicsContext g, EnergyView view) {
        arc(g, view, 0, 2 * Math.PI, 0, Color.web("#1a5a2a"), 12, 1);
        arc(g, view, 0, 2 * Math.PI, -3, TRACE, 1.8, 0.9);
        arc(g, view, 0, 2 * Math.PI, 3, TRACE, 1.8, 0.9);
        // Le signal qui court sur la piste.
        double time = view.time();
        for (int k = 0; k < 3; k++) {
            double head = (time * 0.7 + k / 3.0) % 1 * 2 * Math.PI;
            arc(g, view, head - 0.25, head, 0, Color.web("#d8ffe0"), 2.5, 0.8);
        }
        int pads = 12;
        g.setGlobalAlpha(1);
        for (int k = 0; k < pads; k++) {
            double a = 2 * Math.PI * k / pads;
            double x = view.px(a), y = view.py(a);
            g.setFill(PAD);
            g.fillOval(x - 5, y - 5, 10, 10);
            g.setFill(Color.web("#0b2a14"));
            g.fillOval(x - 2, y - 2, 4, 4);
            // Une piste qui part de la pastille vers un composant.
            double nx = x - view.cx(), ny = y - view.cy(), length = Math.max(1e-9, Math.hypot(nx, ny));
            nx /= length;
            ny /= length;
            g.setStroke(TRACE);
            g.setLineWidth(1.5);
            g.setGlobalAlpha(0.7);
            g.strokeLine(x + nx * 5, y + ny * 5, x + nx * 18, y + ny * 18);
            g.strokeLine(x + nx * 18, y + ny * 18, x + nx * 18 - ny * 10, y + ny * 18 + nx * 10);
            g.setGlobalAlpha(1);
            g.setFill(k % 3 == 0 ? Color.web("#2a2a2a") : Color.web("#c8c8c8"));
            g.fillRect(x + nx * 18 - ny * 10 - 3, y + ny * 18 + nx * 10 - 3, 6, 6);
        }
    }

    /** Le stade : huit couloirs de piste, les lignes blanches entre eux, et la ligne d'arrivée au détecteur. */
    private static void stadium(GraphicsContext g, EnergyView view) {
        int lanes = 8;
        double laneWidth = 3.2;
        double outer = (lanes / 2.0) * laneWidth;
        arc(g, view, 0, 2 * Math.PI, 0, TRACK, lanes * laneWidth + 2, 1);
        for (int k = 0; k <= lanes; k++) {
            arc(g, view, 0, 2 * Math.PI, -outer + k * laneWidth, LANE, 0.8, k == 0 || k == lanes ? 0.95 : 0.6);
        }
        // La ligne d'arrivée : un damier en travers des couloirs, au détecteur.
        double x = view.px(Math.PI), y = view.py(Math.PI);
        double nx = x - view.cx(), ny = y - view.cy(), length = Math.max(1e-9, Math.hypot(nx, ny));
        nx /= length;
        ny /= length;
        g.setGlobalAlpha(1);
        for (int k = 0; k < lanes; k++) {
            for (int c = 0; c < 2; c++) {
                double d = -outer + k * laneWidth + laneWidth / 2;
                double along = (c - 0.5) * 2.5;
                g.setFill((k + c) % 2 == 0 ? Color.WHITE : Color.web("#202020"));
                g.fillRect(x + nx * d - ny * along - 1.4, y + ny * d + nx * along - laneWidth / 2, 2.8, laneWidth);
            }
        }
        // Les numéros des couloirs, à l'injecteur.
        g.setFill(Color.WHITE);
        g.setFont(Font.font(6));
        double sx = view.px(0.15), sy = view.py(0.15);
        double snx = (sx - view.cx()) / Math.max(1e-9, Math.hypot(sx - view.cx(), sy - view.cy()));
        double sny = (sy - view.cy()) / Math.max(1e-9, Math.hypot(sx - view.cx(), sy - view.cy()));
        for (int k = 0; k < lanes; k++) {
            double d = -outer + k * laneWidth + laneWidth / 2;
            g.setGlobalAlpha(0.8);
            g.fillText(String.valueOf(k + 1), sx + snx * d - 2, sy + sny * d + 2);
        }
    }

    /**
     * Vrai si, à cet angle, le faisceau passe sous le ruban de Möbius : le ruban montre son endroit sur la
     * moitié droite de l'anneau (de l'injecteur au détecteur en passant par la droite) et son envers sur la
     * moitié gauche ; entre les deux il se tord, de chant au détecteur, et il se croise à l'injecteur.
     */
    static boolean under(double angle) {
        return tilt(angle) < 0;
    }

    /** L'inclinaison du ruban à cet angle : 1 de face à l'endroit, −1 de face à l'envers, 0 de chant. */
    private static double tilt(double angle) {
        double a = angle % (2 * Math.PI);
        if (a < 0) a += 2 * Math.PI;
        return Math.cos(a / 2);
    }

    /** Le ruban : une bande dont la largeur apparente suit la torsion, l'endroit clair, l'envers sombre, et deux lisières. */
    private static void mobius(GraphicsContext g, EnergyView view) {
        int steps = 144;
        double time = view.time();
        for (int k = 0; k < steps; k++) {
            double a0 = 2 * Math.PI * k / steps, a1 = 2 * Math.PI * (k + 1) / steps;
            double t0 = tilt(a0), t1 = tilt(a1 - 1e-9);
            double x0 = view.px(a0), y0 = view.py(a0), x1 = view.px(a1), y1 = view.py(a1);
            double n0x = x0 - view.cx(), n0y = y0 - view.cy(), l0 = Math.max(1e-9, Math.hypot(n0x, n0y));
            double n1x = x1 - view.cx(), n1y = y1 - view.cy(), l1 = Math.max(1e-9, Math.hypot(n1x, n1y));
            double h0 = RIBBON_WIDTH / 2 * t0, h1 = RIBBON_WIDTH / 2 * t1;
            double[] xs = {x0 + n0x / l0 * h0, x1 + n1x / l1 * h1, x1 - n1x / l1 * h1, x0 - n0x / l0 * h0};
            double[] ys = {y0 + n0y / l0 * h0, y1 + n1y / l1 * h1, y1 - n1y / l1 * h1, y0 - n0y / l0 * h0};
            double face = (t0 + t1) / 2;
            Color tone = face >= 0 ? RIBBON_FRONT : RIBBON_BACK;
            double shade = 0.55 + 0.45 * Math.abs(face) + 0.08 * Math.sin(time * 2 + a0 * 3);
            g.setGlobalAlpha(Math.min(1, shade));
            g.setFill(tone);
            g.fillPolygon(xs, ys, 4);
            g.setStroke(RIBBON_EDGE);
            g.setLineWidth(1);
            g.setGlobalAlpha(0.8);
            g.strokeLine(xs[0], ys[0], xs[1], ys[1]);
            g.strokeLine(xs[3], ys[3], xs[2], ys[2]);
        }
        g.setGlobalAlpha(1);
    }
}
