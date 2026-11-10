package idle.ui.common;

import idle.core.universe.Skin;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;

/**
 * Les skins des icônes d'onglet (version 0.192), dessinés à côté de {@link TabIcon} pour le garder lisible.
 * Chaque icône se réduit à un petit modèle, des points et les traits qui les relient ({@link Model}), que
 * chaque skin rend à sa façon, le mouvement du classique compris :
 * <ul>
 * <li>« Trait de craie » : un seul trait blanc tremblé sur chaque lien, des ronds de craie aux points, un peu de
 * poussière ;</li>
 * <li>« Pixel art » : tout sur une grille de 12 × 12 cases, l'animation en deux images seulement, la couleur de
 * l'onglet ramenée à huit valeurs par canal ;</li>
 * <li>« Constellation » : les points deviennent des étoiles à quatre rayons qui scintillent, reliées par des
 * fils fins ;</li>
 * <li>« Emoji » : des ronds pleins et vifs, cernés de sombre, un reflet blanc, les liens en traits épais.</li>
 * </ul>
 * « Figées » ne change pas le dessin : elle arrête le temps, dans {@link TabIcon}.
 */
public final class TabIconSkinArt {

    /** Le côté de la grille du pixel art, en cases. */
    public static final int PIXEL_GRID = 12;
    /** Les deux images par seconde du pixel art : le temps n'avance que par pas d'une demi-seconde. */
    public static final double PIXEL_STEP = 0.5;

    private static final Color CHALK = Color.web("#f4f1e8");
    private static final Color STAR = Color.web("#fff7dd");
    private static final Color OUTLINE = Color.web("#2a1a10");

    private TabIconSkinArt() {
    }

    /** Vrai pour les skins qui redessinent les icônes : tous ceux des icônes d'onglet sauf « Figées ». */
    public static boolean draws(Skin skin) {
        return skin == Skin.ICONS_CHALK || skin == Skin.ICONS_PIXEL || skin == Skin.ICONS_CONSTELLATION || skin == Skin.ICONS_EMOJI;
    }

    /**
     * Le modèle d'une icône : des points, en fractions du côté autour du centre (de −0,5 à 0,5), et des liens
     * entre eux, par paires d'indices. Il se construit à chaque image, avec le temps.
     */
    public static final class Model {
        private final double[] xs = new double[64];
        private final double[] ys = new double[64];
        private final int[] links = new int[128];
        private int points = 0;
        private int linked = 0;

        /** Ajoute un point et rend son indice. */
        public int point(double x, double y) {
            if (points >= xs.length) return points - 1;
            xs[points] = x;
            ys[points] = y;
            return points++;
        }

        /** Relie deux points. */
        public void link(int from, int to) {
            if (linked + 1 >= links.length) return;
            links[linked++] = from;
            links[linked++] = to;
        }

        /** Un anneau de {@code count} points de rayon {@code r}, reliés de proche en proche, tourné de {@code angle} ; rend le premier indice. */
        public int ring(double cx, double cy, double rx, double ry, int count, double angle) {
            int first = points;
            for (int k = 0; k < count; k++) {
                double a = angle + k * 2 * Math.PI / count;
                point(cx + rx * Math.cos(a), cy + ry * Math.sin(a));
            }
            for (int k = 0; k < count; k++) link(first + k, first + (k + 1) % count);
            return first;
        }

        public int points() {
            return points;
        }

        public int linked() {
            return linked / 2;
        }

        double x(int index) {
            return xs[index];
        }

        double y(int index) {
            return ys[index];
        }

        int from(int link) {
            return links[2 * link];
        }

        int to(int link) {
            return links[2 * link + 1];
        }
    }

    /** Le modèle de cette icône à cet instant : les mêmes mouvements que le dessin classique, en points et en traits. */
    public static Model model(TabIcon.Kind kind, double time) {
        Model m = new Model();
        switch (kind) {
            case PARTICLES -> {
                int center = m.point(0, 0);
                for (int k = 0; k < 3; k++) {
                    double a = time * (1.6 + 0.5 * k) + k * 2.1;
                    double r = 0.22 + 0.08 * k;
                    m.link(center, m.point(r * Math.cos(a), r * Math.sin(a)));
                }
            }
            case AUTOMATION -> {
                int inner = m.ring(0, 0, 0.24, 0.24, 8, time * 0.9);
                for (int k = 0; k < 8; k++) {
                    double a = time * 0.9 + k * Math.PI / 4;
                    m.link(inner + k, m.point(0.42 * Math.cos(a), 0.42 * Math.sin(a)));
                }
            }
            case DARK_MATTER -> {
                int center = m.point(0, 0);
                for (int arm = 0; arm < 2; arm++) {
                    int previous = center;
                    for (int k = 1; k <= 5; k++) {
                        double t = k / 5.0;
                        double a = -time * 1.4 + arm * Math.PI + t * 3.2;
                        int next = m.point(t * 0.44 * Math.cos(a), t * 0.44 * Math.sin(a));
                        m.link(previous, next);
                        previous = next;
                    }
                }
            }
            case COLLIDER -> {
                m.ring(0, 0, 0.42, 0.26, 10, 0);
                double a = time * 2.4;
                m.point(0.42 * Math.sin(a), -0.26 * Math.cos(a));
                m.point(-0.42 * Math.sin(a), -0.26 * Math.cos(a));
            }
            case BIG_BANG -> {
                double pulse = 0.5 + 0.5 * Math.sin(time * 2.2);
                int center = m.point(0, 0);
                for (int k = 0; k < 8; k++) {
                    double a = k * Math.PI / 4 + time * 0.3;
                    double outer = 0.3 + 0.14 * (k % 2 == 0 ? pulse : 1 - pulse);
                    m.link(center, m.point(outer * Math.cos(a), outer * Math.sin(a)));
                }
            }
            case UNIVERSE -> {
                int center = m.point(0, 0);
                for (int arm = 0; arm < 2; arm++) {
                    int previous = center;
                    for (int p = 1; p <= 4; p++) {
                        double t = p / 4.0;
                        double a = time * 0.8 + arm * Math.PI + t * 3.4;
                        int next = m.point(0.44 * t * Math.cos(a), 0.44 * t * Math.sin(a) * 0.75);
                        m.link(previous, next);
                        previous = next;
                    }
                }
            }
            case VOID -> {
                double cycle = (time * 0.7) % 1;
                double open = Math.sin(Math.PI * Math.min(1, cycle / 0.85));
                double a = 0.6 + Math.floor(time * 0.7) * 1.9;
                double dx = Math.cos(a) * 0.3 * open, dy = Math.sin(a) * 0.3 * open;
                m.link(m.point(dx, dy), m.point(-dx, -dy));
            }
            case HADRONS -> m.ring(0, 0, 0.3, 0.3, 3, time * 1.1 - Math.PI / 2);
            case PROFILE -> {
                m.point(0, -0.2);
                int left = m.point(-0.32, 0.36);
                int top = m.point(0, 0.08);
                int right = m.point(0.32, 0.36);
                m.link(left, top);
                m.link(top, right);
                m.link(left, right);
                double a = time * 1.3;
                m.point(0.36 * Math.cos(a), -0.18 + 0.16 * Math.sin(a));
            }
            case ACHIEVEMENTS -> {
                double twinkle = 0.85 + 0.15 * Math.sin(time * 3);
                int first = m.points();
                for (int k = 0; k < 5; k++) {
                    double a = -Math.PI / 2 + k * 2 * Math.PI / 5;
                    m.point(0.44 * twinkle * Math.cos(a), 0.44 * twinkle * Math.sin(a));
                }
                for (int k = 0; k < 5; k++) m.link(first + k, first + (k + 2) % 5);
                double a = time * 1.7;
                m.point(0.4 * Math.cos(a), 0.4 * Math.sin(a));
            }
            case STATS -> {
                for (int k = 0; k < 3; k++) {
                    double h = 0.3 + 0.2 * k + 0.12 * Math.sin(time * 2 + k * 1.3);
                    double x = -0.29 + k * 0.29;
                    m.link(m.point(x, 0.38), m.point(x, 0.38 - h));
                }
            }
            case SETTINGS -> {
                for (int k = 0; k < 3; k++) {
                    double y = -0.25 + 0.25 * k;
                    m.link(m.point(-0.38, y), m.point(0.38, y));
                    m.point(0.3 * Math.sin(time * 0.9 + k * 2), y);
                }
            }
            case SKINS -> {
                m.ring(0, 0, 0.4, 0.32, 8, 0);
                // Les trois pastilles montent et descendent tour à tour, au rythme où elles brillent dans l'icône classique
                // (version 0.203 : sous un skin, la palette était la seule icône immobile).
                for (int k = 0; k < 3; k++) {
                    m.point(-0.21 + 0.17 * k, -0.12 + (k == 1 ? -0.04 : 0.02) + 0.03 * Math.sin(time * 1.6 + k * 2.1));
                }
            }
        }
        return m;
    }

    /**
     * Dessine le modèle sous ce skin, sur une icône déjà effacée.
     *
     * @param s      le côté de l'icône
     * @param color  la couleur de l'onglet
     * @param alpha  1 pour l'onglet affiché, moins pour les autres
     * @param time   le temps de l'animation, pour le scintillement
     */
    public static void draw(Skin skin, GraphicsContext g, Model m, double s, Color color, double alpha, double time) {
        double c = s / 2;
        g.setLineCap(StrokeLineCap.ROUND);
        switch (skin) {
            case ICONS_CHALK -> drawChalk(g, m, s, c, alpha);
            case ICONS_PIXEL -> drawPixel(g, m, s, color, alpha);
            case ICONS_CONSTELLATION -> drawConstellation(g, m, s, c, color, alpha, time);
            case ICONS_EMOJI -> drawEmoji(g, m, s, c, color, alpha);
            default -> { }   // « Figées » et les skins d'autres visuels ne passent pas ici
        }
        g.setGlobalAlpha(1);
    }

    /** Un trait de craie : une ligne blanche qui tremble, en quatre morceaux légèrement décalés. */
    private static void drawChalk(GraphicsContext g, Model m, double s, double c, double alpha) {
        g.setStroke(CHALK);
        g.setFill(CHALK);
        g.setLineWidth(1.5);
        g.setGlobalAlpha(alpha * 0.9);
        for (int l = 0; l < m.linked(); l++) {
            double x0 = c + m.x(m.from(l)) * s, y0 = c + m.y(m.from(l)) * s;
            double x1 = c + m.x(m.to(l)) * s, y1 = c + m.y(m.to(l)) * s;
            double px = x0, py = y0;
            for (int k = 1; k <= 4; k++) {
                double t = k / 4.0;
                // Le tremblement dépend de la place du morceau, pas du temps : le trait ne grouille pas.
                double wobble = k < 4 ? 0.9 * Math.sin(l * 2.3 + k * 1.7 + x0 * 0.4) : 0;
                double nx = x0 + (x1 - x0) * t + wobble, ny = y0 + (y1 - y0) * t - wobble;
                g.strokeLine(px, py, nx, ny);
                px = nx;
                py = ny;
            }
        }
        for (int p = 0; p < m.points(); p++) {
            double x = c + m.x(p) * s, y = c + m.y(p) * s;
            g.fillOval(x - 1.6, y - 1.6, 3.2, 3.2);
            // La poussière de craie, à côté de chaque point.
            g.setGlobalAlpha(alpha * 0.3);
            g.fillOval(x + 2.2 * Math.cos(p * 2.1), y + 2.2 * Math.sin(p * 2.1), 1.2, 1.2);
            g.setGlobalAlpha(alpha * 0.9);
        }
    }

    /** Du pixel art : chaque point et chaque lien posés sur la grille, en cases pleines. */
    private static void drawPixel(GraphicsContext g, Model m, double s, Color color, double alpha) {
        double cell = s / PIXEL_GRID;
        g.setFill(eightLevels(color));
        g.setGlobalAlpha(alpha);
        boolean[] lit = new boolean[PIXEL_GRID * PIXEL_GRID];
        for (int l = 0; l < m.linked(); l++) {
            double x0 = m.x(m.from(l)), y0 = m.y(m.from(l)), x1 = m.x(m.to(l)), y1 = m.y(m.to(l));
            int steps = 2 * PIXEL_GRID;
            for (int k = 0; k <= steps; k++) {
                double t = k / (double) steps;
                light(lit, x0 + (x1 - x0) * t, y0 + (y1 - y0) * t);
            }
        }
        for (int p = 0; p < m.points(); p++) light(lit, m.x(p), m.y(p));
        for (int i = 0; i < lit.length; i++) {
            if (lit[i]) g.fillRect((i % PIXEL_GRID) * cell, (i / PIXEL_GRID) * cell, cell, cell);
        }
    }

    /** Allume la case de la grille où tombe ce point du modèle. */
    private static void light(boolean[] lit, double x, double y) {
        int column = (int) Math.floor((x + 0.5) * PIXEL_GRID);
        int row = (int) Math.floor((y + 0.5) * PIXEL_GRID);
        column = Math.max(0, Math.min(PIXEL_GRID - 1, column));
        row = Math.max(0, Math.min(PIXEL_GRID - 1, row));
        lit[row * PIXEL_GRID + column] = true;
    }

    /** La couleur ramenée à huit valeurs par canal : pour les vérifications et le pixel art. */
    public static Color eightLevels(Color color) {
        return Color.color(level(color.getRed()), level(color.getGreen()), level(color.getBlue()));
    }

    private static double level(double channel) {
        return Math.round(channel * 7) / 7.0;
    }

    /** Une constellation : des fils fins entre des étoiles à quatre rayons qui scintillent. */
    private static void drawConstellation(GraphicsContext g, Model m, double s, double c, Color color, double alpha, double time) {
        g.setStroke(color);
        g.setLineWidth(0.8);
        g.setGlobalAlpha(alpha * 0.55);
        for (int l = 0; l < m.linked(); l++) {
            g.strokeLine(c + m.x(m.from(l)) * s, c + m.y(m.from(l)) * s, c + m.x(m.to(l)) * s, c + m.y(m.to(l)) * s);
        }
        g.setStroke(STAR);
        g.setFill(STAR);
        for (int p = 0; p < m.points(); p++) {
            double x = c + m.x(p) * s, y = c + m.y(p) * s;
            double twinkle = 0.6 + 0.4 * Math.sin(time * 2.6 + p * 1.9);
            double ray = 1.6 + 2.2 * twinkle;
            g.setGlobalAlpha(alpha * (0.5 + 0.5 * twinkle));
            g.setLineWidth(0.9);
            g.strokeLine(x - ray, y, x + ray, y);
            g.strokeLine(x, y - ray, x, y + ray);
            g.fillOval(x - 1.1, y - 1.1, 2.2, 2.2);
        }
    }

    /** Des emoji : des ronds pleins et vifs cernés de sombre, un reflet blanc, les liens en traits épais. */
    private static void drawEmoji(GraphicsContext g, Model m, double s, double c, Color color, double alpha) {
        Color vivid = color.deriveColor(0, 1.8, 1.1, 1);
        g.setGlobalAlpha(alpha);
        g.setStroke(OUTLINE);
        g.setLineWidth(s * 0.16);
        for (int l = 0; l < m.linked(); l++) {
            g.strokeLine(c + m.x(m.from(l)) * s, c + m.y(m.from(l)) * s, c + m.x(m.to(l)) * s, c + m.y(m.to(l)) * s);
        }
        g.setStroke(vivid);
        g.setLineWidth(s * 0.1);
        for (int l = 0; l < m.linked(); l++) {
            g.strokeLine(c + m.x(m.from(l)) * s, c + m.y(m.from(l)) * s, c + m.x(m.to(l)) * s, c + m.y(m.to(l)) * s);
        }
        double r = Math.max(2.4, s * 0.12);
        for (int p = 0; p < m.points(); p++) {
            double x = c + m.x(p) * s, y = c + m.y(p) * s;
            g.setFill(OUTLINE);
            g.fillOval(x - r - 1, y - r - 1, 2 * r + 2, 2 * r + 2);
            g.setFill(vivid);
            g.fillOval(x - r, y - r, 2 * r, 2 * r);
            g.setFill(Color.WHITE);
            g.setGlobalAlpha(alpha * 0.8);
            g.fillOval(x - r * 0.55, y - r * 0.6, r * 0.5, r * 0.5);
            g.setGlobalAlpha(alpha);
        }
    }
}
