package idle.ui.particles;

import idle.core.universe.Skin;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Les sept skins du fond animé des particules (version 0.192), dessinés à côté de {@link AmbientView} pour le
 * garder lisible. Les quatre-vingt-dix poussières gardent leur place, leur taille et leur phase ; chaque skin
 * leur donne un autre mouvement et une autre forme, et toutes filent plus vite avec la production :
 * <ul>
 * <li>« Neige » : des flocons blancs qui tombent en se balançant, les gros en étoiles à six branches ;</li>
 * <li>« Pluie » : des traits bleutés qui tombent droit et vite ;</li>
 * <li>« Pollen » : des grains jaunes flous qui dérivent lentement en tournoyant ;</li>
 * <li>« Lucioles » : des points verts qui s'allument et s'éteignent, avec leur halo ;</li>
 * <li>« Bulles » : des ronds translucides qui montent, un reflet blanc en haut à gauche ;</li>
 * <li>« Poussière d'or » : des paillettes dorées qui scintillent, les plus vives en éclats à quatre rayons ;</li>
 * <li>« Cendres » : des flocons gris qui montent en se balançant, un sur cinq encore rougeoyant.</li>
 * </ul>
 * « Silence » n'est pas dessiné ici : le fond reste vide, dans {@link AmbientView}.
 */
final class AmbientSkinArt {

    private static final Color SNOW = Color.web("#f4f8ff");
    private static final Color RAIN = Color.web("#9fd0ff");
    private static final Color POLLEN = Color.web("#ffe08a");
    private static final Color FIREFLY = Color.web("#9be7a8");
    private static final Color BUBBLE = Color.web("#bfe8ff");
    private static final Color GOLD = Color.web("#ffd27f");
    private static final Color ASH = Color.web("#b8b0a8");
    private static final Color EMBER = Color.web("#ff8a3a");

    private AmbientSkinArt() {
    }

    /** Vrai pour les skins dessinés ici : les sept paliers de particules. */
    static boolean draws(Skin skin) {
        return skin == Skin.DUST_SNOW || skin == Skin.DUST_RAIN || skin == Skin.DUST_POLLEN || skin == Skin.DUST_FIREFLIES
                || skin == Skin.DUST_BUBBLES || skin == Skin.DUST_GOLD || skin == Skin.DUST_ASH;
    }

    /**
     * Fait avancer et dessine toutes les poussières sous ce skin, sur un fond déjà effacé. Les positions sont en
     * fractions de la vue et reviennent de l'autre côté quand elles en sortent.
     *
     * @param step  le temps de cette image multiplié par la vitesse due à la production
     * @param heat  la production ramenée de 0 à 1 : les traînées et le scintillement s'en renforcent
     */
    static void frame(Skin skin, GraphicsContext g, double w, double h, double[] x, double[] y, double[] dx, double[] dy,
            double[] size, double[] phase, double time, double step, double heat) {
        for (int i = 0; i < x.length; i++) {
            double sway = Math.sin(time * 0.8 + phase[i]) * 0.01;
            double vx;
            double vy;
            switch (skin) {
                case DUST_SNOW -> {
                    vx = dx[i] * 0.5 + sway;
                    vy = 0.03 + Math.abs(dy[i]);
                }
                case DUST_RAIN -> {
                    vx = 0.01;
                    vy = 0.3 + Math.abs(dy[i]) * 4;
                }
                case DUST_POLLEN -> {
                    vx = dx[i] + sway * 0.6;
                    vy = dy[i] * 0.5 + 0.004;
                }
                case DUST_BUBBLES -> {
                    vx = sway;
                    vy = -(0.03 + Math.abs(dy[i]));
                }
                case DUST_ASH -> {
                    vx = dx[i] + sway;
                    vy = -(0.015 + Math.abs(dy[i]) * 0.5);
                }
                default -> {       // les lucioles et la poussière d'or dérivent comme les poussières classiques, plus lentement
                    vx = dx[i] * 0.6;
                    vy = dy[i] * 0.6;
                }
            }
            x[i] = wrap(x[i] + vx * step);
            y[i] = wrap(y[i] + vy * step);
            double px = x[i] * w, py = y[i] * h;
            double s = size[i];
            double pulse = 0.5 + 0.5 * Math.sin(phase[i] + time * 1.3);
            switch (skin) {
                case DUST_SNOW -> {
                    g.setFill(SNOW);
                    g.setStroke(SNOW);
                    g.setGlobalAlpha(0.35 + 0.35 * pulse);
                    double r = s * 1.2;
                    if (s > 2) {
                        // Un gros flocon : six branches qui tournent lentement.
                        g.setLineWidth(1);
                        double a0 = time * 0.4 + phase[i];
                        for (int k = 0; k < 3; k++) {
                            double a = a0 + k * Math.PI / 3;
                            g.strokeLine(px - r * Math.cos(a), py - r * Math.sin(a), px + r * Math.cos(a), py + r * Math.sin(a));
                        }
                    } else {
                        g.fillOval(px - r / 2, py - r / 2, r, r);
                    }
                }
                case DUST_RAIN -> {
                    g.setStroke(RAIN);
                    g.setLineWidth(Math.max(0.8, s * 0.5));
                    g.setGlobalAlpha(0.18 + 0.25 * pulse);
                    double tail = (8 + 10 * heat) * (0.5 + s / 3);
                    g.strokeLine(px, py, px - tail * 0.03, py - tail);
                }
                case DUST_POLLEN -> {
                    g.setFill(POLLEN);
                    double r = s * 1.4;
                    g.setGlobalAlpha(0.08 + 0.1 * pulse);
                    g.fillOval(px - r, py - r, 2 * r, 2 * r);     // le flou
                    g.setGlobalAlpha(0.3 + 0.3 * pulse);
                    g.fillOval(px - r / 2, py - r / 2, r, r);
                }
                case DUST_FIREFLIES -> {
                    // Une luciole s'allume d'un coup et s'éteint doucement, chacune à son rythme.
                    double blink = Math.max(0, Math.sin(phase[i] + time * (0.7 + 0.3 * (i % 4))));
                    double lit = Math.pow(blink, 3);
                    g.setFill(FIREFLY);
                    double r = s * 1.1;
                    g.setGlobalAlpha(0.05 + 0.3 * lit);
                    g.fillOval(px - r * 2.5, py - r * 2.5, 5 * r, 5 * r);     // le halo
                    g.setGlobalAlpha(0.15 + 0.85 * lit);
                    g.fillOval(px - r / 2, py - r / 2, r, r);
                }
                case DUST_BUBBLES -> {
                    double r = s * 2.2;
                    g.setStroke(BUBBLE);
                    g.setLineWidth(1);
                    g.setGlobalAlpha(0.25 + 0.25 * pulse);
                    g.strokeOval(px - r, py - r, 2 * r, 2 * r);
                    g.setFill(Color.WHITE);
                    g.setGlobalAlpha(0.5 + 0.3 * pulse);
                    g.fillOval(px - r * 0.55, py - r * 0.65, r * 0.4, r * 0.3);     // le reflet
                }
                case DUST_GOLD -> {
                    // Une paillette scintille d'un coup, vive et brève.
                    double glint = Math.pow(pulse, 6);
                    g.setFill(GOLD);
                    g.setStroke(GOLD);
                    g.setGlobalAlpha(0.2 + 0.8 * glint);
                    double r = s * 0.7;
                    g.fillOval(px - r / 2, py - r / 2, r, r);
                    if (glint > 0.5) {
                        g.setLineWidth(0.8);
                        double ray = s * (1 + 3 * glint);
                        g.strokeLine(px - ray, py, px + ray, py);
                        g.strokeLine(px, py - ray, px, py + ray);
                    }
                }
                default -> {
                    // Les cendres : grises, un flocon sur cinq encore rougeoyant, qui faiblit en montant.
                    boolean ember = i % 5 == 0;
                    g.setFill(ember ? EMBER.interpolate(ASH, 1 - y[i]) : ASH);
                    g.setGlobalAlpha(ember ? 0.3 + 0.5 * pulse : 0.15 + 0.2 * pulse);
                    double r = s * 1.1;
                    g.save();
                    g.translate(px, py);
                    g.rotate(Math.toDegrees(time * 0.6 + phase[i]));
                    g.fillRect(-r / 2, -r / 3, r, r * 0.66);     // un petit flocon aplati qui tourne
                    g.restore();
                }
            }
        }
        g.setGlobalAlpha(1);
    }

    /** Une poussière qui sort d'un côté rentre par l'autre. */
    private static double wrap(double value) {
        return value - Math.floor(value);
    }
}
