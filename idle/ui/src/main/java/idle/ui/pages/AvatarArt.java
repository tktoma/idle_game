package idle.ui.pages;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;

/**
 * Les huit avatars de la version 0.198, dessinés dans la manière des douze premiers ({@link AvatarView#paint}) : un
 * halo déjà posé, la couleur de l'avatar pour les pleins et les traits, quelques rehauts clairs. Le hadron est trois
 * quarks serrés dans leur sac ; la sonde une capsule à antenne et panneaux ; la toile des nœuds reliés ; la bulle une
 * sphère irisée ; l'arrosoir un corps, un bec et sa pomme qui goutte ; le chrono un cadran à bouton et aiguille ; le
 * miroir un ovale à cadre et son reflet ; le kaléidoscope un hexagone en six quartiers.
 */
final class AvatarArt {

    private AvatarArt() {
    }

    /** Dessine un avatar de la 0.198 sur un carré de côté {@code s} ; {@code color} est la couleur de l'avatar. */
    static void paint(GraphicsContext g, Avatar avatar, double s, Color color) {
        double c = s / 2;
        g.setLineCap(StrokeLineCap.ROUND);
        g.setGlobalAlpha(1);
        g.setFill(color);
        g.setStroke(color);
        switch (avatar) {
            case HADRON -> hadron(g, c, s, color);
            case PROBE -> probe(g, c, s, color);
            case COSMIC_WEB -> web(g, c, s, color);
            case BUBBLE -> bubble(g, c, s, color);
            case WATERING_CAN -> wateringCan(g, c, s, color);
            case CHRONO -> chrono(g, c, s, color);
            case MIRROR -> mirror(g, c, s, color);
            case KALEIDOSCOPE -> kaleidoscope(g, c, s, color);
            default -> AvatarView.dot(g, c, c, s * 0.2);     // un avatar d'avant la 0.198 ne passe pas ici : un rond plein, pour ne rien laisser vide
        }
        g.setGlobalAlpha(1);
    }

    /** Le hadron : le sac, un rond de la couleur de l'avatar à peine rempli, et trois quarks colorés serrés dedans. */
    private static void hadron(GraphicsContext g, double c, double s, Color color) {
        g.setGlobalAlpha(0.25);
        AvatarView.dot(g, c, c, s * 0.3);
        g.setGlobalAlpha(1);
        g.setLineWidth(s / 40);
        g.strokeOval(c - s * 0.3, c - s * 0.3, s * 0.6, s * 0.6);
        String[] colors = {"#ff6b6b", "#6bdc8b", "#6ba8ff"};
        double[][] at = {{c, c - s * 0.1}, {c - s * 0.1, c + s * 0.08}, {c + s * 0.1, c + s * 0.08}};
        for (int k = 0; k < 3; k++) {
            g.setFill(Color.web(colors[k]));
            AvatarView.dot(g, at[k][0], at[k][1], s * 0.09);
            g.setFill(Color.WHITE);
            g.setGlobalAlpha(0.5);
            AvatarView.dot(g, at[k][0] - s * 0.025, at[k][1] - s * 0.03, s * 0.025);
            g.setGlobalAlpha(1);
        }
    }

    /** La sonde : une capsule penchée, deux panneaux solaires, une antenne-parabole, et le trait de sa route. */
    private static void probe(GraphicsContext g, double c, double s, Color color) {
        g.save();
        g.translate(c, c);
        g.rotate(-30);
        // Les panneaux, bleu nuit quadrillés de la couleur de l'avatar.
        g.setFill(Color.web("#2a4a8a"));
        g.fillRect(-s * 0.42, -s * 0.07, s * 0.26, s * 0.14);
        g.fillRect(s * 0.16, -s * 0.07, s * 0.26, s * 0.14);
        g.setStroke(color);
        g.setLineWidth(Math.max(1, s / 90));
        for (int k = 0; k <= 3; k++) {
            double x = -s * 0.42 + k * s * 0.0867;
            g.strokeLine(x, -s * 0.07, x, s * 0.07);
            g.strokeLine(x + s * 0.58, -s * 0.07, x + s * 0.58, s * 0.07);
        }
        g.strokeLine(-s * 0.42, 0, s * 0.42, 0);
        // Le corps, et sa parabole tournée vers le haut.
        g.setFill(color);
        g.fillRoundRect(-s * 0.14, -s * 0.12, s * 0.28, s * 0.24, s * 0.06, s * 0.06);
        g.setFill(Color.web("#8a93a3"));
        g.fillRect(-s * 0.02, -s * 0.3, s * 0.04, s * 0.18);
        g.setStroke(color);
        g.setLineWidth(s / 30);
        g.strokeArc(-s * 0.14, -s * 0.42, s * 0.28, s * 0.2, 0, 180, ArcType.OPEN);
        g.setFill(Color.web("#ffd27f"));
        AvatarView.dot(g, 0, -s * 0.3, s * 0.025);
        g.restore();
        // Une route pointillée derrière la sonde.
        g.setStroke(color);
        g.setGlobalAlpha(0.45);
        g.setLineWidth(Math.max(1, s / 70));
        for (int k = 0; k < 4; k++) {
            double t = 0.2 + k * 0.08;
            g.strokeLine(c - s * (t + 0.03), c + s * (t + 0.03) * 0.9, c - s * t, c + s * t * 0.9);
        }
        g.setGlobalAlpha(1);
    }

    /** La toile de l'univers : sept nœuds reliés par des filaments, le plus gros au milieu. */
    private static void web(GraphicsContext g, double c, double s, Color color) {
        double[][] nodes = {{0, 0, 0.07}, {-0.3, -0.2, 0.045}, {0.26, -0.28, 0.04}, {0.33, 0.1, 0.05}, {-0.1, 0.32, 0.045},
            {-0.34, 0.14, 0.035}, {0.12, -0.08, 0.03}};
        int[][] links = {{0, 1}, {0, 2}, {0, 3}, {0, 4}, {0, 5}, {1, 5}, {2, 3}, {3, 4}, {1, 2}, {6, 2}, {6, 3}};
        g.setLineWidth(Math.max(1, s / 60));
        g.setGlobalAlpha(0.6);
        for (int[] link : links) {
            double[] a = nodes[link[0]], b = nodes[link[1]];
            g.strokeLine(c + s * a[0], c + s * a[1], c + s * b[0], c + s * b[1]);
        }
        g.setGlobalAlpha(1);
        for (double[] node : nodes) {
            g.setFill(color);
            AvatarView.dot(g, c + s * node[0], c + s * node[1], s * node[2]);
            g.setFill(Color.WHITE);
            g.setGlobalAlpha(0.7);
            AvatarView.dot(g, c + s * node[0], c + s * node[1], s * node[2] * 0.4);
            g.setGlobalAlpha(1);
        }
    }

    /** La bulle d'univers : une sphère transparente au bord irisé, un reflet en haut à gauche, un ciel étoilé dedans. */
    private static void bubble(GraphicsContext g, double c, double s, Color color) {
        double r = s * 0.34;
        g.setGlobalAlpha(0.18);
        AvatarView.dot(g, c, c, r);
        g.setGlobalAlpha(1);
        g.setFill(Color.web("#fff1c9"));
        double[][] stars = {{-0.12, -0.05, 0.018}, {0.1, 0.1, 0.014}, {0.14, -0.14, 0.012}, {-0.05, 0.18, 0.01}, {0.02, -0.2, 0.011}};
        for (double[] star : stars) AvatarView.dot(g, c + s * star[0], c + s * star[1], s * star[2]);
        // Le bord irisé : trois arcs de couleurs qui se suivent sur le tour.
        g.setLineWidth(s / 36);
        String[] rim = {"#ff9fd0", "#9fe6ff", "#c9ffb0"};
        for (int k = 0; k < 3; k++) {
            g.setStroke(Color.web(rim[k]));
            g.setGlobalAlpha(0.8);
            g.strokeArc(c - r, c - r, 2 * r, 2 * r, 90 + k * 120, 120, ArcType.OPEN);
        }
        g.setGlobalAlpha(1);
        g.setStroke(color);
        g.setLineWidth(Math.max(1, s / 80));
        g.strokeOval(c - r, c - r, 2 * r, 2 * r);
        // Le reflet.
        g.setStroke(Color.WHITE);
        g.setLineWidth(s / 30);
        g.setGlobalAlpha(0.75);
        g.strokeArc(c - r * 0.72, c - r * 0.72, 1.44 * r, 1.44 * r, 110, 50, ArcType.OPEN);
        g.setGlobalAlpha(1);
    }

    /** L'arrosoir : le corps, son anse, son bec incliné et la pomme d'où tombent trois gouttes. */
    private static void wateringCan(GraphicsContext g, double c, double s, Color color) {
        g.setFill(color);
        g.fillRoundRect(c - s * 0.28, c - s * 0.1, s * 0.34, s * 0.32, s * 0.05, s * 0.05);
        g.setStroke(color);
        g.setLineWidth(s / 22);
        g.strokeArc(c - s * 0.2, c - s * 0.26, s * 0.18, s * 0.26, 0, 180, ArcType.OPEN);
        // Le bec, un trait épais qui monte vers la droite, et la pomme au bout.
        g.strokeLine(c + s * 0.04, c, c + s * 0.3, c - s * 0.2);
        g.setLineWidth(s / 16);
        g.strokeLine(c + s * 0.26, c - s * 0.26, c + s * 0.34, c - s * 0.14);
        // Le reflet du corps, et les gouttes.
        g.setFill(Color.WHITE);
        g.setGlobalAlpha(0.35);
        g.fillRoundRect(c - s * 0.24, c - s * 0.06, s * 0.05, s * 0.2, s * 0.03, s * 0.03);
        g.setGlobalAlpha(1);
        g.setFill(Color.web("#9fd0ff"));
        AvatarView.dot(g, c + s * 0.38, c - s * 0.04, s * 0.03);
        AvatarView.dot(g, c + s * 0.33, c + s * 0.08, s * 0.025);
        AvatarView.dot(g, c + s * 0.4, c + s * 0.18, s * 0.022);
    }

    /** Le chrono : un cadran au cerne épais, son bouton en haut, douze repères, une aiguille qui pointe vers deux heures. */
    private static void chrono(GraphicsContext g, double c, double s, Color color) {
        double r = s * 0.3;
        g.setFill(color);
        g.fillRoundRect(c - s * 0.05, c - r - s * 0.12, s * 0.1, s * 0.1, s * 0.03, s * 0.03);
        g.fillRect(c - s * 0.025, c - r - s * 0.06, s * 0.05, s * 0.06);
        g.setFill(Color.web("#0e1420"));
        AvatarView.dot(g, c, c, r);
        g.setStroke(color);
        g.setLineWidth(s / 22);
        g.strokeOval(c - r, c - r, 2 * r, 2 * r);
        g.setLineWidth(Math.max(1, s / 80));
        for (int k = 0; k < 12; k++) {
            double a = k * Math.PI / 6;
            double inner = k % 3 == 0 ? r * 0.78 : r * 0.87;
            g.strokeLine(c + inner * Math.cos(a), c + inner * Math.sin(a), c + r * 0.93 * Math.cos(a), c + r * 0.93 * Math.sin(a));
        }
        g.setStroke(Color.web("#ff8a75"));
        g.setLineWidth(s / 45);
        double a = -Math.PI / 2 + Math.PI / 3;
        g.strokeLine(c, c, c + r * 0.75 * Math.cos(a), c + r * 0.75 * Math.sin(a));
        g.setFill(Color.web("#ff8a75"));
        AvatarView.dot(g, c, c, s * 0.025);
    }

    /** Le miroir : un ovale au cadre de la couleur de l'avatar sur pied, le verre bleuté barré d'un reflet en biais. */
    private static void mirror(GraphicsContext g, double c, double s, Color color) {
        g.setStroke(color);
        g.setLineWidth(s / 24);
        g.strokeLine(c, c + s * 0.3, c, c + s * 0.4);
        g.strokeLine(c - s * 0.14, c + s * 0.4, c + s * 0.14, c + s * 0.4);
        g.setFill(Color.web("#2a3a5a"));
        g.fillOval(c - s * 0.24, c - s * 0.36, s * 0.48, s * 0.66);
        g.setFill(Color.web("#7fb0ff"));
        g.setGlobalAlpha(0.35);
        g.fillOval(c - s * 0.2, c - s * 0.32, s * 0.4, s * 0.58);
        g.setGlobalAlpha(0.7);
        g.setStroke(Color.WHITE);
        g.setLineWidth(s / 30);
        g.strokeLine(c + s * 0.1, c - s * 0.26, c - s * 0.1, c + s * 0.16);
        g.setLineWidth(s / 60);
        g.strokeLine(c + s * 0.15, c - s * 0.14, c + s * 0.03, c + s * 0.12);
        g.setGlobalAlpha(1);
        g.setStroke(color);
        g.setLineWidth(s / 22);
        g.strokeOval(c - s * 0.24, c - s * 0.36, s * 0.48, s * 0.66);
    }

    /** Le kaléidoscope : un hexagone en six quartiers qui alternent trois couleurs, un petit hexagone clair au centre. */
    private static void kaleidoscope(GraphicsContext g, double c, double s, Color color) {
        double r = s * 0.36;
        String[] tones = {"#f0a8c0", "#ffd27f", "#9fd0ff"};
        double[] xs = new double[3], ys = new double[3];
        for (int k = 0; k < 6; k++) {
            double a0 = -Math.PI / 2 + k * Math.PI / 3, a1 = a0 + Math.PI / 3;
            xs[0] = c;
            ys[0] = c;
            xs[1] = c + r * Math.cos(a0);
            ys[1] = c + r * Math.sin(a0);
            xs[2] = c + r * Math.cos(a1);
            ys[2] = c + r * Math.sin(a1);
            g.setFill(Color.web(tones[k % 3]));
            g.setGlobalAlpha(k % 2 == 0 ? 0.9 : 0.6);
            g.fillPolygon(xs, ys, 3);
        }
        g.setGlobalAlpha(1);
        g.setStroke(Color.web("#0e1420"));
        g.setLineWidth(Math.max(1, s / 70));
        for (int k = 0; k < 6; k++) {
            double a = -Math.PI / 2 + k * Math.PI / 3;
            g.strokeLine(c, c, c + r * Math.cos(a), c + r * Math.sin(a));
        }
        g.setStroke(color);
        g.setLineWidth(s / 30);
        double[] hx = new double[6], hy = new double[6];
        for (int k = 0; k < 6; k++) {
            double a = -Math.PI / 2 + k * Math.PI / 3;
            hx[k] = c + r * Math.cos(a);
            hy[k] = c + r * Math.sin(a);
        }
        g.strokePolygon(hx, hy, 6);
        g.setFill(Color.WHITE);
        g.setGlobalAlpha(0.8);
        AvatarView.dot(g, c, c, s * 0.05);
        g.setGlobalAlpha(1);
    }
}
