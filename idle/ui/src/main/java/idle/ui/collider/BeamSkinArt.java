package idle.ui.collider;

import idle.core.universe.Skin;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Les quatre skins du faisceau du collisionneur de la version 0.191, dessinés à côté de {@link EnergyView}
 * pour la garder lisible. Chaque vue a le sien, qui garde ce que le croisement laisse derrière lui.
 * <ul>
 * <li>« Comète » : deux boules à longue queue de glace bleu pâle semée de cristaux ; au croisement, il neige
 * sur le détecteur ;</li>
 * <li>« Encre » : deux traits de plume noire sur fond clair, de l'injecteur à la plume ; le croisement laisse
 * une tache qui s'étale puis sèche ;</li>
 * <li>« Laser » : des rayons fins et droits, par segments, sans traînée ; les étincelles sont des pixels ;</li>
 * <li>« Lucioles » : une nuée de points verts qui clignotent autour de chaque tête ; au croisement, toutes
 * s'allument d'un coup.</li>
 * </ul>
 */
final class BeamSkinArt {

    /** Le fond clair du skin « Encre ». */
    static final Color PAPER = Color.web("#f3ecd8");
    private static final Color INK = Color.web("#141418");
    private static final Color ICE = Color.web("#cfefff");
    private static final Color ICE_DEEP = Color.web("#7fc8ff");
    private static final Color LASER = Color.web("#ff3040");
    private static final Color FIREFLY = Color.web("#a8ff5a");

    private final EnergyView view;
    /** Les flocons de la comète : x, y, vitesse x, vitesse y, âge. */
    private final List<double[]> snow = new ArrayList<>();
    /** Les taches d'encre : x, y, âge. */
    private final List<double[]> stains = new ArrayList<>();
    private boolean wasCrossing;

    BeamSkinArt(EnergyView view) {
        this.view = view;
    }

    /** Vrai pour les faisceaux dessinés ici : ceux de la version 0.191. */
    static boolean draws(Skin beam) {
        return beam == Skin.BEAM_COMET || beam == Skin.BEAM_INK || beam == Skin.BEAM_LASER || beam == Skin.BEAM_FIREFLIES;
    }

    /** La couleur des arcs électriques du croisement sous ce faisceau. */
    static Color lightningColor(Skin beam) {
        return switch (beam) {
            case BEAM_COMET -> ICE;
            case BEAM_INK -> INK;
            case BEAM_LASER -> LASER;
            default -> FIREFLY;
        };
    }

    /** Ce que la vue garde d'un croisement : pour les vérifications. */
    int remains() {
        return snow.size() + stains.size();
    }

    /** Oublie ce qui restait : le joueur a changé de faisceau. */
    void clear() {
        snow.clear();
        stains.clear();
        wasCrossing = false;
    }

    /**
     * Dessine un faisceau.
     *
     * @param angle     l'angle de sa tête sur l'anneau
     * @param direction 1 pour le faisceau qui part à droite, −1 pour l'autre
     * @param crossing  vrai pendant que les deux se croisent
     */
    void beam(GraphicsContext g, Skin beam, double angle, int direction, boolean crossing) {
        switch (beam) {
            case BEAM_COMET -> comet(g, angle, direction);
            case BEAM_INK -> ink(g, angle, direction);
            case BEAM_LASER -> laser(g, angle, direction);
            default -> fireflies(g, angle, direction, crossing);
        }
        double heat = view.flameHeat();
        if (heat > 0) view.flame(g, angle, direction, heat);
        g.setGlobalAlpha(1);
    }

    /** Une fois les deux faisceaux dessinés : ce que le croisement sème (neige, tache), avancé et dessiné. */
    void after(GraphicsContext g, Skin beam, double elapsed, boolean crossing) {
        double x = view.px(Math.PI), y = view.py(Math.PI);
        if (beam == Skin.BEAM_COMET) {
            if (crossing && EnergyView.motion() && snow.size() < 200) {
                for (int k = 0; k < 3; k++) {
                    double seed = EnergyView.random((long) (view.time() * 1000) + k * 17);
                    snow.add(new double[] {x + (seed - 0.5) * 60, y - 10 - 20 * seed, (seed - 0.5) * 12, 10 + 15 * seed, 0});
                }
            }
            g.setStroke(ICE);
            g.setLineWidth(0.9);
            for (double[] flake : snow) {
                if (EnergyView.motion()) {
                    flake[0] += flake[2] * elapsed + Math.sin(view.time() * 3 + flake[1]) * 0.3;
                    flake[1] += flake[3] * elapsed;
                    flake[4] += elapsed;
                }
                double fade = Math.max(0, 1 - flake[4] / 2.5);
                g.setGlobalAlpha(fade);
                double size = 2 + 1.5 * fade, spin = view.time() * 2 + flake[0];
                for (int k = 0; k < 3; k++) {
                    double b = spin + Math.PI * k / 3;
                    g.strokeLine(flake[0] - size * Math.cos(b), flake[1] - size * Math.sin(b), flake[0] + size * Math.cos(b), flake[1] + size * Math.sin(b));
                }
            }
            snow.removeIf(flake -> flake[4] >= 2.5);
        } else if (beam == Skin.BEAM_INK) {
            if (crossing && !wasCrossing) stains.add(new double[] {x, y, 0});
            for (double[] stain : stains) {
                if (EnergyView.motion()) stain[2] += elapsed;
                double age = stain[2];
                double grow = Math.min(1, age / 0.6), dry = Math.max(0, 1 - age / 3);
                // Une tache au bord irrégulier, et des gouttelettes autour.
                int points = 24;
                double[] xs = new double[points], ys = new double[points];
                for (int k = 0; k < points; k++) {
                    double a = 2 * Math.PI * k / points;
                    double r = (10 + 14 * grow) * (1 + 0.3 * EnergyView.random(k * 7 + (long) (stain[0] * 13)));
                    xs[k] = stain[0] + r * Math.cos(a);
                    ys[k] = stain[1] + r * 0.6 * Math.sin(a);
                }
                g.setGlobalAlpha(0.85 * dry);
                g.setFill(INK);
                g.fillPolygon(xs, ys, points);
                for (int k = 0; k < 7; k++) {
                    double a = EnergyView.random(k * 31 + (long) (stain[1] * 7)) * 2 * Math.PI;
                    double d = (18 + 22 * EnergyView.random(k * 53)) * grow;
                    double size = 1.5 + 2 * EnergyView.random(k * 71);
                    g.fillOval(stain[0] + d * Math.cos(a) - size, stain[1] + d * 0.6 * Math.sin(a) - size, 2 * size, 2 * size);
                }
            }
            stains.removeIf(stain -> stain[2] >= 3);
        }
        wasCrossing = crossing;
        g.setGlobalAlpha(1);
    }

    /** La comète : une longue queue de glace qui s'évase, des cristaux dedans, un noyau blanc bleuté. */
    private void comet(GraphicsContext g, double angle, int direction) {
        int tail = 40;
        for (int k = tail; k >= 1; k--) {
            double u = k / (double) tail;
            double a = angle - direction * k * 0.045;
            double x = view.px(a), y = view.py(a);
            // La queue s'écarte de l'anneau vers l'extérieur, comme soufflée.
            double nx = x - view.cx(), ny = y - view.cy(), length = Math.max(1e-9, Math.hypot(nx, ny));
            x += nx / length * 6 * u;
            y += ny / length * 6 * u;
            double size = 9 * (1 - 0.7 * u);
            g.setGlobalAlpha(0.35 * (1 - u));
            g.setFill(u < 0.5 ? ICE : ICE_DEEP);
            g.fillOval(x - size / 2, y - size / 2, size, size);
            if (k % 5 == 0) {
                g.setGlobalAlpha(0.8 * (1 - u));
                g.setFill(Color.WHITE);
                double c = 2.5 * (1 - 0.5 * u);
                g.fillPolygon(new double[] {x, x + c, x, x - c}, new double[] {y - c, y, y + c, y}, 4);
            }
        }
        double hx = view.px(angle), hy = view.py(angle);
        EnergyView.glow(g, hx, hy, 16, ICE_DEEP, 0.8);
        g.setGlobalAlpha(1);
        g.setFill(Color.WHITE);
        g.fillOval(hx - 4, hy - 4, 8, 8);
    }

    /** L'encre : un trait de plume de l'injecteur à la tête, plus ou moins chargé, et la plume au bout. */
    private void ink(GraphicsContext g, double angle, int direction) {
        int steps = Math.max(2, (int) Math.ceil(Math.abs(angle) / (2 * Math.PI) * 72));
        g.setStroke(INK);
        for (int k = 0; k < steps; k++) {
            double a0 = angle * k / steps, a1 = angle * (k + 1) / steps;
            double u = (k + 1) / (double) steps;
            double load = 0.5 + 0.5 * Math.sin(u * 23 + direction);
            g.setLineWidth(1 + 2.5 * u * load);
            g.setGlobalAlpha(0.55 + 0.45 * load);
            g.strokeLine(view.px(a0), view.py(a0), view.px(a1), view.py(a1));
        }
        // La plume : un bec en pointe dans le sens de la marche.
        double hx = view.px(angle), hy = view.py(angle);
        double bx = view.px(angle - direction * 0.08), by = view.py(angle - direction * 0.08);
        double dx = hx - bx, dy = hy - by, length = Math.max(1e-9, Math.hypot(dx, dy));
        dx /= length;
        dy /= length;
        g.setGlobalAlpha(1);
        g.setFill(INK);
        g.fillPolygon(new double[] {hx + dx * 6, hx - dx * 8 - dy * 4, hx - dx * 8 + dy * 4}, new double[] {hy + dy * 6, hy - dy * 8 + dx * 4, hy - dy * 8 - dx * 4}, 3);
        g.setStroke(PAPER);
        g.setLineWidth(0.8);
        g.strokeLine(hx + dx * 5, hy + dy * 5, hx - dx * 4, hy - dy * 4);
    }

    /** Le laser : quatre segments droits derrière la tête, un trait rouge au cœur blanc, et une tête carrée. */
    private void laser(GraphicsContext g, double angle, int direction) {
        int segments = 4;
        for (int k = 0; k < segments; k++) {
            double a0 = angle - direction * k * 0.11, a1 = angle - direction * (k + 1) * 0.11;
            double fade = 1 - k / (double) segments;
            g.setStroke(LASER);
            g.setLineWidth(3);
            g.setGlobalAlpha(0.3 * fade);
            g.strokeLine(view.px(a0), view.py(a0), view.px(a1), view.py(a1));
            g.setLineWidth(1.2);
            g.setGlobalAlpha(0.95 * fade);
            g.strokeLine(view.px(a0), view.py(a0), view.px(a1), view.py(a1));
            g.setStroke(Color.WHITE);
            g.setLineWidth(0.5);
            g.setGlobalAlpha(0.9 * fade);
            g.strokeLine(view.px(a0), view.py(a0), view.px(a1), view.py(a1));
        }
        double hx = view.px(angle), hy = view.py(angle);
        g.setGlobalAlpha(1);
        g.setFill(Color.WHITE);
        g.fillRect(hx - 3, hy - 3, 6, 6);
        g.setFill(LASER);
        g.fillRect(hx - 1.5, hy - 1.5, 3, 3);
    }

    /** Les lucioles : seize points verts qui voltigent autour de la tête et clignotent chacun à son rythme. */
    private void fireflies(GraphicsContext g, double angle, int direction, boolean crossing) {
        double hx = view.px(angle), hy = view.py(angle), time = view.time();
        for (int k = 0; k < 16; k++) {
            double seed = EnergyView.random(k * 13 + direction * 7);
            double reach = 6 + 12 * seed;
            double a = time * (0.8 + seed) * (k % 2 == 0 ? 1 : -1) + k;
            double x = hx + reach * Math.cos(a) - direction * k * 0.8, y = hy + reach * 0.7 * Math.sin(a) + Math.sin(time * 2 + k) * 2;
            double blink = Math.max(0, Math.sin(time * (2 + 2 * seed) + k * 1.7));
            double lit = crossing ? 1 : blink * blink;
            if (lit < 0.05) continue;
            EnergyView.glow(g, x, y, 4 + 4 * lit, FIREFLY, 0.6 * lit);
            g.setGlobalAlpha(0.5 + 0.5 * lit);
            g.setFill(Color.web("#f0ffc0"));
            g.fillOval(x - 1.2, y - 1.2, 2.4, 2.4);
        }
        if (crossing) EnergyView.glow(g, hx, hy, 24, FIREFLY, 0.5);
    }
}
