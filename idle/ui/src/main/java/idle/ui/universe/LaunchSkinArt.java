package idle.ui.universe;

import idle.core.universe.Skin;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.StrokeLineCap;

/**
 * Les skins du lancement de l'univers suivant (version 0.196), dessinés à côté de {@link LaunchView} : avant l'éclat
 * ({@link LaunchView#LAUNCH_AT}) quelque chose se prépare au centre, à l'éclat l'univers se lance, puis l'image s'efface.
 * <ul>
 * <li>« Fusée de papier » : une fusée pliée monte du bas jusqu'au centre, puis file vers les onglets en laissant une
 * traînée de ronds de fumée ;</li>
 * <li>« Graine qui germe » : une graine au centre, dont la tige monte et déplie deux feuilles, puis une volée de pollen
 * doré s'élève ;</li>
 * <li>« Pierre dans l'eau » : une pierre tombe du haut, touche l'eau au centre, et des ondes en ellipses s'écartent avec
 * des éclaboussures ;</li>
 * <li>« Lampe » : une ampoule dont le filament rougit, qui s'allume d'un coup et baigne la page de lumière chaude, en
 * rayons qui s'éteignent ;</li>
 * <li>« Rideau de théâtre » : deux pans de velours rouge se ferment sur la page, puis s'ouvrent sous leur lambrequin
 * doré, dans un rond de projecteur.</li>
 * </ul>
 */
final class LaunchSkinArt {

    private static final Color PAPER = Color.web("#f4efe2");
    private static final Color PAPER_FOLD = Color.web("#b9b0a0");
    private static final Color FLAME = Color.web("#ffb347");
    private static final Color SMOKE = Color.web("#d8dde8");
    private static final Color SEED = Color.web("#b8963c");
    private static final Color SPROUT = Color.web("#9fe08a");
    private static final Color POLLEN = Color.web("#ffe08a");
    private static final Color STONE = Color.web("#6e7078");
    private static final Color WATER = Color.web("#8fd3ff");
    private static final Color BULB = Color.web("#fff6d2");
    private static final Color FILAMENT = Color.web("#ff8a3a");
    private static final Color SOCKET = Color.web("#8a8f99");
    private static final Color VELVET = Color.web("#8c1a28");
    private static final Color VELVET_DARK = Color.web("#5a0f18");
    private static final Color GOLD = Color.web("#e0b84a");

    private LaunchSkinArt() {
    }

    /** Dessine le lancement habillé du skin, à {@code age} secondes du début ; la vue a déjà été effacée. */
    static void draw(GraphicsContext g, double w, double h, double age, Skin skin) {
        double cx = w / 2, cy = h / 2, size = Math.min(w, h);
        boolean before = age < LaunchView.LAUNCH_AT;
        double t = before ? age / LaunchView.LAUNCH_AT : (age - LaunchView.LAUNCH_AT) / (LaunchView.SECONDS - LaunchView.LAUNCH_AT);
        switch (skin) {
            case LAUNCH_ROCKET -> rocket(g, w, h, cx, cy, size, before, t);
            case LAUNCH_SPROUT -> sprout(g, cx, cy, size, before, t);
            case LAUNCH_STONE -> stone(g, w, cx, cy, size, before, t);
            case LAUNCH_LAMP -> lamp(g, w, h, cx, cy, size, before, t);
            default -> curtain(g, w, h, cx, cy, size, before, t);
        }
        g.setGlobalAlpha(1);
    }

    private static void rocket(GraphicsContext g, double w, double h, double cx, double cy, double size, boolean before, double t) {
        double s = size * 0.09;
        double y = before ? h + s * 2 - (h + s * 2 - cy) * t * (2 - t) : cy - (cy + s * 3) * t * t;
        // La fumée : des ronds qui restent derrière et s'élargissent.
        g.setFill(SMOKE);
        int puffs = before ? 8 : 16;
        for (int k = 0; k < puffs; k++) {
            double back = (k + 1) / (double) puffs;
            double py = y + s * 1.2 + back * (before ? s * 4 : (cy - y) + s * 4);
            double r = s * (0.25 + 0.5 * back), sway = Math.sin(k * 1.7 + t * 6) * s * 0.3;
            g.setGlobalAlpha(0.5 * (1 - back) * (before ? 1 : 1 - t));
            g.fillOval(cx + sway - r, py - r, 2 * r, 2 * r);
        }
        if (!before) {
            // L'éclat au départ, et des anneaux de papier.
            g.setGlobalAlpha(Math.max(0, 0.35 - t));
            g.setFill(BULB);
            g.fillRect(0, 0, w, h);
            g.setStroke(PAPER);
            g.setLineWidth(2);
            for (int k = 0; k < 3; k++) {
                double r = size * (0.05 + (0.5 + 0.2 * k) * Math.pow(t, 0.7));
                g.setGlobalAlpha(Math.max(0, (1 - t) * (0.7 - 0.2 * k)));
                g.strokeOval(cx - r, cy - r, 2 * r, 2 * r);
            }
        }
        g.setGlobalAlpha(before ? 1 : Math.max(0, 1 - t * 1.2));
        // La flamme, qui vacille.
        double flick = 0.8 + 0.4 * Math.sin(t * 50);
        g.setFill(FLAME);
        g.fillPolygon(new double[] {cx - s * 0.3, cx + s * 0.3, cx}, new double[] {y + s * 1.1, y + s * 1.1, y + s * (1.1 + 0.9 * flick)}, 3);
        // Le corps de papier, en triangle pointu et deux ailerons, avec ses plis.
        g.setFill(PAPER);
        g.fillPolygon(new double[] {cx, cx + s * 0.55, cx + s * 0.35, cx - s * 0.35, cx - s * 0.55},
                new double[] {y - s * 1.4, y + s * 0.6, y + s * 1.1, y + s * 1.1, y + s * 0.6}, 5);
        g.setStroke(PAPER_FOLD);
        g.setLineWidth(1);
        g.strokeLine(cx, y - s * 1.4, cx, y + s * 1.1);
        g.strokeLine(cx, y - s * 0.2, cx + s * 0.55, y + s * 0.6);
        g.strokeLine(cx, y - s * 0.2, cx - s * 0.55, y + s * 0.6);
        g.setFill(Color.web("#4a9bff"));
        double window = s * 0.16;
        g.fillOval(cx - window, y - s * 0.2 - window, 2 * window, 2 * window);
    }

    private static void sprout(GraphicsContext g, double cx, double cy, double size, boolean before, double t) {
        double s = size * 0.07;
        double fade = before ? 1 : Math.max(0, 1 - t);
        // La graine, en amande dorée, un peu sous le centre.
        g.setGlobalAlpha(fade);
        g.setFill(SEED);
        g.fillOval(cx - s * 0.6, cy - s * 0.4 + s * 0.6, s * 1.2, s * 1.6);
        // La tige monte en se courbant, et ses deux feuilles se déplient.
        double grow = before ? t : 1;
        g.setStroke(SPROUT);
        g.setLineWidth(Math.max(2.5, s * 0.14));
        g.setLineCap(StrokeLineCap.ROUND);
        g.beginPath();
        g.moveTo(cx, cy + s * 0.3);
        g.bezierCurveTo(cx + s * 0.4 * grow, cy - s * 0.8 * grow, cx - s * 0.5 * grow, cy - s * 1.6 * grow, cx, cy - s * 2.6 * grow);
        g.stroke();
        g.setFill(SPROUT);
        for (int side = -1; side <= 1; side += 2) {
            double leaf = s * 1.1 * Math.max(0, grow - 0.4) / 0.6, lx = cx, ly = cy - s * 2.4 * grow;
            if (leaf <= 0) continue;
            g.fillPolygon(new double[] {lx, lx + side * leaf * 0.9, lx + side * leaf * 1.4, lx + side * leaf * 0.5},
                    new double[] {ly, ly - leaf * 0.6, ly - leaf * 0.1, ly + leaf * 0.3}, 4);
        }
        if (before) return;
        // Le pollen s'élève, en nappe dorée qui s'écarte.
        UniverseView.glow(g, cx, cy - s * 2.4, size * 0.2 * (1 - t) + 1, POLLEN, 0.8 * (1 - t));
        g.setFill(POLLEN);
        for (int i = 0; i < 70; i++) {
            double spread = UniverseView.noise(i * 5 + 11) - 0.5, speed = 0.5 + UniverseView.noise(i * 5 + 12);
            double x = cx + spread * size * 1.2 * t * speed, y = cy - s * 2.4 - t * speed * (cy + s * 3) + Math.sin(t * 8 + i) * 4;
            double r = 1.2 + 2 * UniverseView.noise(i * 5 + 13);
            g.setGlobalAlpha(Math.max(0, 1 - t) * (0.6 + 0.4 * Math.sin(t * 10 + i)));
            g.fillOval(x - r, y - r, 2 * r, 2 * r);
        }
    }

    private static void stone(GraphicsContext g, double w, double cx, double cy, double size, boolean before, double t) {
        double s = size * 0.05;
        if (before) {
            // La pierre tombe du haut, en tournant, et son ombre grandit sur l'eau.
            double y = -s + (cy + s) * t * t;
            g.setFill(Color.color(0, 0, 0, 0.3 * t));
            g.fillOval(cx - s * 1.5 * t, cy - s * 0.4 * t, s * 3 * t, s * 0.8 * t);
            g.save();
            g.translate(cx, y);
            g.rotate(t * 240);
            g.setGlobalAlpha(1);
            g.setFill(STONE);
            g.fillPolygon(new double[] {-s, -s * 0.3, s * 0.9, s * 0.6, -s * 0.6}, new double[] {-s * 0.2, -s * 0.9, -s * 0.4, s * 0.7, s * 0.8}, 5);
            g.setFill(STONE.brighter());
            g.fillPolygon(new double[] {-s * 0.6, -s * 0.3, s * 0.3}, new double[] {-s * 0.3, -s * 0.8, -s * 0.5}, 3);
            g.restore();
            return;
        }
        // Les ondes : des ellipses qui s'écartent du point de chute, de plus en plus pâles.
        g.setStroke(WATER);
        g.setLineCap(StrokeLineCap.ROUND);
        for (int k = 0; k < 6; k++) {
            double start = k * 0.12;
            if (t < start) continue;
            double u = (t - start) / (1 - start);
            double r = size * 0.04 + w * 0.6 * u;
            g.setLineWidth(Math.max(1, 3.5 - k * 0.4));
            g.setGlobalAlpha(Math.max(0, (1 - u) * (0.9 - k * 0.1)));
            g.strokeOval(cx - r, cy - r * 0.32, 2 * r, r * 0.64);
        }
        // Les éclaboussures, qui montent puis retombent.
        g.setFill(WATER);
        for (int i = 0; i < 24; i++) {
            double a = Math.PI * (1 + UniverseView.noise(i * 3 + 20)), speed = 0.4 + 0.8 * UniverseView.noise(i * 3 + 21);
            double rise = Math.min(t * 2, 1.4);
            double x = cx + Math.cos(a) * size * 0.25 * speed * rise, y = cy + Math.sin(a) * size * 0.35 * speed * rise + size * 0.5 * rise * rise;
            double r = 1.5 + 2 * UniverseView.noise(i * 3 + 22);
            g.setGlobalAlpha(Math.max(0, 1 - t * 1.6));
            g.fillOval(x - r, y - r, 2 * r, 2 * r);
        }
        // Un éclat blanc à la chute.
        UniverseView.glow(g, cx, cy, size * 0.15, Color.WHITE, Math.max(0, 0.8 - t * 3));
    }

    private static void lamp(GraphicsContext g, double w, double h, double cx, double cy, double size, boolean before, double t) {
        double s = size * 0.09;
        double lit = before ? t * t : 1;
        double fade = before ? 1 : Math.max(0, 1 - t);
        if (!before) {
            // La lumière chaude baigne la page, puis s'éteint en rayons.
            g.setGlobalAlpha(Math.max(0, 0.55 - t * 0.8));
            g.setFill(BULB);
            g.fillRect(0, 0, w, h);
            g.setStroke(POLLEN);
            g.setLineWidth(2);
            for (int k = 0; k < 16; k++) {
                double a = Math.PI * 2 * k / 16 + 0.2, inner = s * 1.3 + size * 0.3 * t, outer = inner + size * (0.15 + 0.25 * UniverseView.noise(k * 3 + 30));
                g.setGlobalAlpha(0.6 * fade);
                g.strokeLine(cx + Math.cos(a) * inner, cy + Math.sin(a) * inner * 0.9, cx + Math.cos(a) * outer, cy + Math.sin(a) * outer * 0.9);
            }
        }
        UniverseView.glow(g, cx, cy, s * (1.5 + 3 * lit), BULB, 0.8 * lit * fade);
        g.setGlobalAlpha(fade);
        // L'ampoule : le globe de verre, le culot vissé, le filament.
        g.setFill(Color.color(1, 1, 1, 0.12 + 0.6 * lit));
        g.fillOval(cx - s, cy - s * 1.2, 2 * s, 2 * s);
        g.fillRect(cx - s * 0.45, cy + s * 0.4, s * 0.9, s * 0.6);
        g.setFill(SOCKET);
        g.fillRoundRect(cx - s * 0.45, cy + s * 0.95, s * 0.9, s * 0.55, 3, 3);
        g.setStroke(SOCKET.darker());
        g.setLineWidth(1);
        for (int k = 0; k < 3; k++) g.strokeLine(cx - s * 0.45, cy + s * (1.05 + 0.15 * k), cx + s * 0.45, cy + s * (1.05 + 0.15 * k));
        g.setStroke(FILAMENT.interpolate(Color.WHITE, lit));
        g.setLineWidth(Math.max(1.5, s * 0.08));
        g.beginPath();
        g.moveTo(cx - s * 0.3, cy + s * 0.5);
        g.lineTo(cx - s * 0.3, cy - s * 0.1);
        for (int k = 0; k < 5; k++) g.lineTo(cx - s * 0.3 + s * 0.6 * (k + 1) / 5, cy - s * (k % 2 == 0 ? 0.35 : 0.1));
        g.lineTo(cx + s * 0.3, cy + s * 0.5);
        g.stroke();
        g.setStroke(Color.color(1, 1, 1, 0.6));
        g.setLineWidth(1.2);
        g.strokeOval(cx - s, cy - s * 1.2, 2 * s, 2 * s);
    }

    private static void curtain(GraphicsContext g, double w, double h, double cx, double cy, double size, boolean before, double t) {
        // Les pans : fermés à l'éclat, ouverts à la fin.
        double closed = before ? t * t * (3 - 2 * t) : Math.max(0, 1 - t * t * (3 - 2 * t));
        double pan = w / 2 * closed;
        if (!before) {
            // Le rond de projecteur, sur ce que les rideaux découvrent.
            g.setGlobalAlpha(Math.max(0, 0.5 - t * 0.5));
            g.setFill(new RadialGradient(0, 0, cx, cy, size * 0.5, false, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.color(1, 0.96, 0.8, 0.8)), new Stop(1, Color.color(1, 0.96, 0.8, 0))));
            g.fillOval(cx - size * 0.5, cy - size * 0.5, size, size);
        }
        g.setGlobalAlpha(1);
        for (int side = 0; side < 2; side++) {
            double x = side == 0 ? 0 : w - pan;
            if (pan <= 0) continue;
            g.setFill(new LinearGradient(x, 0, x + pan, 0, false, CycleMethod.NO_CYCLE,
                    new Stop(0, side == 0 ? VELVET_DARK : VELVET), new Stop(1, side == 0 ? VELVET : VELVET_DARK)));
            g.fillRect(x, 0, pan, h);
            // Les plis du velours.
            g.setStroke(VELVET_DARK);
            g.setLineWidth(Math.max(2, pan * 0.03));
            g.setGlobalAlpha(0.6);
            int folds = Math.max(1, (int) (pan / 28));
            for (int k = 1; k < folds; k++) {
                double fx = x + pan * k / folds;
                g.strokeLine(fx, h * 0.08, fx + Math.sin(k) * 3, h);
            }
            g.setGlobalAlpha(1);
        }
        // Le lambrequin doré en haut, en festons, et sa tringle.
        g.setFill(GOLD);
        g.fillRect(0, 0, w, h * 0.05);
        int scallops = Math.max(4, (int) (w / 60));
        for (int k = 0; k < scallops; k++) {
            double sx = w * k / scallops, sw = w / scallops;
            g.fillOval(sx, h * 0.02, sw, h * 0.07);
        }
        g.setFill(GOLD.darker());
        g.fillRect(0, 0, w, Math.max(2, h * 0.012));
    }
}
