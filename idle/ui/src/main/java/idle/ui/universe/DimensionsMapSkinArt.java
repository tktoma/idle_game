package idle.ui.universe;

import idle.core.universe.Skin;
import idle.core.universe.SkinVisual;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;

/**
 * La carte des dimensions et le défi final sous un skin (version 0.197, {@link SkinVisual#DIMENSIONS_MAP} et
 * {@link SkinVisual#FINALE_FILL}). Les onze points deviennent les planètes d'un système (chacune de la couleur de sa
 * dimension, les paires avec un anneau, le soleil au milieu), les perles d'un collier (un fil passe par toutes), les
 * pierres d'un cercle de menhirs (debout, gravées d'un signe qui luit une fois la dimension ouverte), les fenêtres d'une
 * tour (en arc, éclairées quand la dimension est ouverte, la pierre autour) ou les cases d'un cadran (un cercle gradué,
 * chaque case une plaque numérotée). Le remplissage du défi final devient de l'eau qui monte (sa surface ondule), du
 * sable (sa surface est plate et grenue), de la lumière (une lueur qui monte, blanche au cœur) ou du lierre qui grimpe
 * (une tige et ses feuilles jusqu'au niveau). Sans skin, {@link DimensionsView} dessine comme avant.
 */
final class DimensionsMapSkinArt {

    private DimensionsMapSkinArt() {
    }

    private static final Color OFF = Color.web("#3a4160"), DIM_TEXT = Color.web("#8a91b0"), DARK_TEXT = Color.web("#1a1430");
    private static final Color PEARL = Color.web("#f4f1ff"), PEARL_SHADE = Color.web("#b9b2d8"), PEARL_OFF = Color.web("#4a4660"), STRING = Color.web("#c9b89a");
    private static final Color STONE = Color.web("#6d7078"), STONE_LIGHT = Color.web("#8e929b"), STONE_DARK = Color.web("#3a3c44"), RUNE = Color.web("#9fe6ff");
    private static final Color WALL = Color.web("#5a5560"), MORTAR = Color.web("#3a3640"), WINDOW_LIT = Color.web("#ffd27f"), WINDOW_DARK = Color.web("#1a1826");
    private static final Color DIAL = Color.web("#d9c9a0"), DIAL_DARK = Color.web("#6a5a3a"), PLATE = Color.web("#f4ead0"), PLATE_OFF = Color.web("#4a4650");
    private static final Color SUN = Color.web("#ffe2b0");
    private static final Color WATER = Color.web("#3d8fd6"), WATER_LIGHT = Color.web("#9fd0ff"), SAND = Color.web("#d9c28a"), SAND_DARK = Color.web("#a88a50");
    private static final Color LIGHT = Color.web("#fff1c9"), IVY = Color.web("#3ddc6a"), IVY_DARK = Color.web("#1f7a3a");

    /** Vrai pour un skin de la carte. */
    static boolean maps(Skin skin) {
        return skin != null && skin.visual() == SkinVisual.DIMENSIONS_MAP;
    }

    /** Vrai pour un skin du remplissage du défi final. */
    static boolean fills(Skin skin) {
        return skin != null && skin.visual() == SkinVisual.FINALE_FILL;
    }

    /** Ce que le skin trace sous les points, avant eux : le fil du collier, le cercle gradué du cadran. */
    static void ring(GraphicsContext g, double cx, double cy, double radius, double dot, int count, Skin skin) {
        if (skin == Skin.MAP_PEARLS) {
            g.setStroke(STRING);
            g.setLineWidth(Math.max(1.5, dot * 0.12));
            g.setGlobalAlpha(0.8);
            g.strokeOval(cx - radius, cy - radius, 2 * radius, 2 * radius);
        } else if (skin == Skin.MAP_DIAL) {
            g.setStroke(DIAL);
            g.setLineWidth(Math.max(1.5, dot * 0.1));
            g.setGlobalAlpha(0.7);
            g.strokeOval(cx - radius - dot * 1.5, cy - radius - dot * 1.5, 2 * (radius + dot * 1.5), 2 * (radius + dot * 1.5));
            g.strokeOval(cx - radius + dot * 1.5, cy - radius + dot * 1.5, 2 * (radius - dot * 1.5), 2 * (radius - dot * 1.5));
            // Les graduations : cinq entre deux cases.
            g.setLineWidth(1);
            g.setGlobalAlpha(0.5);
            for (int k = 0; k < count * 5; k++) {
                double a = -Math.PI / 2 + 2 * Math.PI * k / (count * 5), r0 = radius + dot * 1.5, r1 = radius + dot * (k % 5 == 0 ? 1 : 1.25);
                g.strokeLine(cx + Math.cos(a) * r0, cy + Math.sin(a) * r0, cx + Math.cos(a) * r1, cy + Math.sin(a) * r1);
            }
        } else if (skin == Skin.MAP_TOWER) {
            // La tour : un mur de pierre en anneau, derrière les fenêtres.
            g.setFill(WALL);
            g.setGlobalAlpha(0.9);
            g.setLineWidth(dot * 3.4);
            g.setStroke(WALL);
            g.strokeOval(cx - radius, cy - radius, 2 * radius, 2 * radius);
            g.setStroke(MORTAR);
            g.setLineWidth(1);
            g.setGlobalAlpha(0.6);
            for (int k = -1; k <= 1; k++) {
                double r = radius + k * dot * 1.1;
                g.strokeOval(cx - r, cy - r, 2 * r, 2 * r);
            }
            for (int k = 0; k < count * 4; k++) {
                double a = 2 * Math.PI * (k + 0.5) / (count * 4), r0 = radius - dot * 1.7, r1 = radius + dot * 1.7;
                g.strokeLine(cx + Math.cos(a) * r0, cy + Math.sin(a) * r0, cx + Math.cos(a) * r1, cy + Math.sin(a) * r1);
            }
        }
        g.setGlobalAlpha(1);
    }

    /** Le point du milieu : le soleil du système, le fermoir du collier, la pierre couchée, la porte de la tour, l'axe du cadran. */
    static void center(GraphicsContext g, double cx, double cy, double dot, double time, Skin skin) {
        switch (skin) {
            case MAP_PLANETS -> {
                UniverseView.glow(g, cx, cy, dot * 4, SUN, 0.5 + 0.1 * Math.sin(time * 2));
                g.setGlobalAlpha(1);
                g.setFill(new RadialGradient(0, 0, cx, cy, dot * 1.2, false, CycleMethod.NO_CYCLE, new Stop(0, Color.WHITE), new Stop(0.5, SUN), new Stop(1, Color.web("#ff9a3c"))));
                g.fillOval(cx - dot * 1.2, cy - dot * 1.2, dot * 2.4, dot * 2.4);
            }
            case MAP_PEARLS -> {
                g.setGlobalAlpha(1);
                g.setFill(STRING);
                g.fillRoundRect(cx - dot * 0.9, cy - dot * 0.45, dot * 1.8, dot * 0.9, dot * 0.4, dot * 0.4);
                g.setFill(PEARL);
                g.fillOval(cx - dot * 0.3, cy - dot * 0.3, dot * 0.6, dot * 0.6);
            }
            case MAP_MENHIRS -> {
                g.setGlobalAlpha(1);
                g.setFill(STONE_DARK);
                g.fillRoundRect(cx - dot * 1.5, cy - dot * 0.6, dot * 3, dot * 1.2, dot * 0.3, dot * 0.3);
                g.setFill(STONE);
                g.fillRoundRect(cx - dot * 1.4, cy - dot * 0.7, dot * 2.8, dot * 1, dot * 0.3, dot * 0.3);
            }
            case MAP_TOWER -> {
                g.setGlobalAlpha(1);
                g.setFill(MORTAR);
                g.fillRoundRect(cx - dot, cy - dot * 1.2, dot * 2, dot * 2.4, dot * 1.6, dot * 1.6);
                g.setFill(WINDOW_LIT);
                g.setGlobalAlpha(0.6 + 0.1 * Math.sin(time * 3));
                g.fillRoundRect(cx - dot * 0.7, cy - dot * 0.9, dot * 1.4, dot * 1.9, dot * 1.2, dot * 1.2);
            }
            default -> {
                g.setGlobalAlpha(1);
                g.setFill(DIAL_DARK);
                g.fillOval(cx - dot * 0.7, cy - dot * 0.7, dot * 1.4, dot * 1.4);
                g.setFill(DIAL);
                g.fillOval(cx - dot * 0.35, cy - dot * 0.35, dot * 0.7, dot * 0.7);
            }
        }
        g.setGlobalAlpha(1);
    }

    /**
     * Le point de la dimension {@code number}, de « rayon » {@code dot}, ouvert ou non, dans la couleur {@code tint} de sa
     * dimension ; {@code angle} est sa place sur le cercle (pour tourner les menhirs et les plaques vers le milieu). Rend la
     * couleur du numéro écrit dessus.
     */
    static Color point(GraphicsContext g, double x, double y, double dot, int number, boolean open, double angle, double time, Color tint, Skin skin) {
        g.setGlobalAlpha(1);
        switch (skin) {
            case MAP_PLANETS -> {
                Color body = open ? tint : OFF;
                if (open) UniverseView.glow(g, x, y, dot * 2.2, tint, 0.35);
                g.setGlobalAlpha(1);
                if (number % 2 == 0) {
                    g.setStroke(open ? tint.interpolate(Color.WHITE, 0.3) : OFF.brighter());
                    g.setLineWidth(Math.max(1.5, dot * 0.14));
                    g.setGlobalAlpha(0.8);
                    g.strokeOval(x - dot * 1.6, y - dot * 0.55, dot * 3.2, dot * 1.1);
                    g.setGlobalAlpha(1);
                }
                g.setFill(new RadialGradient(0, 0, x - dot * 0.4, y - dot * 0.4, dot * 1.3, false, CycleMethod.NO_CYCLE,
                        new Stop(0, body.interpolate(Color.WHITE, 0.35)), new Stop(0.7, body), new Stop(1, body.darker().darker())));
                g.fillOval(x - dot, y - dot, 2 * dot, 2 * dot);
                return open ? DARK_TEXT : DIM_TEXT;
            }
            case MAP_PEARLS -> {
                if (open) UniverseView.glow(g, x, y, dot * 2, PEARL, 0.3);
                g.setGlobalAlpha(1);
                g.setFill(open
                        ? new RadialGradient(0, 0, x - dot * 0.35, y - dot * 0.4, dot * 1.1, false, CycleMethod.NO_CYCLE,
                                new Stop(0, Color.WHITE), new Stop(0.5, PEARL), new Stop(0.85, tint.interpolate(PEARL_SHADE, 0.6)), new Stop(1, PEARL_SHADE))
                        : PEARL_OFF);
                g.fillOval(x - dot, y - dot, 2 * dot, 2 * dot);
                return open ? DARK_TEXT : DIM_TEXT;
            }
            case MAP_MENHIRS -> {
                double w = dot * 1.3, h = dot * 2.3;
                g.setFill(STONE_DARK);
                g.fillRoundRect(x - w / 2 + dot * 0.12, y - h / 2 + dot * 0.12, w, h, dot * 0.4, dot * 0.4);
                g.setFill(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE, new Stop(0, STONE_LIGHT), new Stop(1, STONE)));
                g.fillRoundRect(x - w / 2, y - h / 2, w, h, dot * 0.4, dot * 0.4);
                if (open) {
                    // Le signe gravé qui luit : un losange et son trait, dans la couleur de la dimension.
                    Color glow = tint.interpolate(RUNE, 0.3);
                    UniverseView.glow(g, x, y + dot * 0.5, dot, glow, 0.35 + 0.1 * Math.sin(time * 2 + number));
                    g.setStroke(glow);
                    g.setLineWidth(Math.max(1.2, dot * 0.1));
                    g.setGlobalAlpha(0.9);
                    g.strokePolygon(new double[] {x, x + dot * 0.3, x, x - dot * 0.3}, new double[] {y + dot * 0.2, y + dot * 0.55, y + dot * 0.9, y + dot * 0.55}, 4);
                    g.setGlobalAlpha(1);
                }
                return open ? PLATE : DIM_TEXT;
            }
            case MAP_TOWER -> {
                double w = dot * 1.4, h = dot * 2.4;
                g.setFill(MORTAR);
                g.fillRoundRect(x - w / 2 - 2, y - h / 2 - 2, w + 4, h + 4, w, w);
                if (open) {
                    UniverseView.glow(g, x, y, dot * 1.8, WINDOW_LIT, 0.35 + 0.1 * Math.sin(time * 2.5 + number));
                    g.setGlobalAlpha(1);
                    g.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, new Stop(0, WINDOW_LIT), new Stop(1, tint)));
                } else {
                    g.setFill(WINDOW_DARK);
                }
                g.fillRoundRect(x - w / 2, y - h / 2, w, h, w, w);
                // Les croisillons.
                g.setStroke(MORTAR);
                g.setLineWidth(1.5);
                g.strokeLine(x, y - h / 2 + 2, x, y + h / 2 - 2);
                g.strokeLine(x - w / 2, y, x + w / 2, y);
                return open ? DARK_TEXT : DIM_TEXT;
            }
            default -> {
                // Le cadran : une plaque tournée vers le milieu, sur l'anneau.
                g.save();
                g.translate(x, y);
                g.rotate(Math.toDegrees(angle) + 90);
                g.setFill(open ? PLATE : PLATE_OFF);
                g.fillRoundRect(-dot * 1.1, -dot * 0.8, dot * 2.2, dot * 1.6, dot * 0.3, dot * 0.3);
                g.setStroke(open ? tint : DIAL_DARK);
                g.setLineWidth(Math.max(1.5, dot * 0.12));
                g.strokeRoundRect(-dot * 1.1, -dot * 0.8, dot * 2.2, dot * 1.6, dot * 0.3, dot * 0.3);
                g.restore();
                if (open) {
                    // L'aiguille : un trait du milieu de la plaque vers l'intérieur, qui bat.
                    g.setStroke(tint);
                    g.setLineWidth(2);
                    g.setGlobalAlpha(0.5 + 0.3 * Math.sin(time * 2 + number));
                    g.strokeLine(x, y, x - Math.cos(angle) * dot * 1.6, y - Math.sin(angle) * dot * 1.6);
                    g.setGlobalAlpha(1);
                }
                return open ? DARK_TEXT : DIM_TEXT;
            }
        }
    }

    /**
     * Le remplissage du défi final d'un point, de bas en haut, de la part {@code share} de sa hauteur, découpé au disque de
     * rayon {@code r} : eau, sable, lumière ou lierre.
     */
    static void fill(GraphicsContext g, double x, double y, double r, double share, double time, int number, Skin skin) {
        if (share <= 0) return;
        double level = y + r - 2 * r * share;
        g.save();
        g.beginPath();
        g.arc(x, y, r, r, 0, 360);
        g.closePath();
        g.clip();
        switch (skin) {
            case FINALE_WATER -> {
                g.setGlobalAlpha(0.85);
                g.setFill(WATER);
                g.fillRect(x - r, level, 2 * r, y + r - level + 1);
                // La surface qui ondule, et un reflet dessous.
                g.setFill(WATER_LIGHT);
                g.setGlobalAlpha(0.8);
                g.beginPath();
                g.moveTo(x - r, level + 2);
                for (int k = 0; k <= 12; k++) {
                    double px = x - r + 2 * r * k / 12;
                    g.lineTo(px, level + Math.sin(time * 3 + number + k * 0.9) * r * 0.06);
                }
                g.lineTo(x + r, level + 2);
                g.closePath();
                g.fill();
            }
            case FINALE_SAND -> {
                g.setGlobalAlpha(0.95);
                g.setFill(SAND);
                g.fillRect(x - r, level, 2 * r, y + r - level + 1);
                g.setFill(SAND_DARK);
                for (int k = 0; k < 24; k++) {
                    double px = x - r + 2 * r * UniverseView.noise(k * 3 + number * 7 + 501), py = level + (y + r - level) * UniverseView.noise(k * 3 + number * 7 + 502);
                    g.fillOval(px - 0.8, py - 0.8, 1.6, 1.6);
                }
                g.setGlobalAlpha(0.6);
                g.fillRect(x - r, level, 2 * r, 1.5);
            }
            case FINALE_LIGHT -> {
                g.setGlobalAlpha(0.9);
                g.setFill(new LinearGradient(0, level, 0, y + r, false, CycleMethod.NO_CYCLE, new Stop(0, LIGHT.interpolate(Color.TRANSPARENT, 0.6)),
                        new Stop(0.35, LIGHT), new Stop(1, Color.WHITE)));
                g.fillRect(x - r, level - r * 0.15, 2 * r, y + r - level + r * 0.15 + 1);
                UniverseView.glow(g, x, (level + y + r) / 2, r * 0.9, LIGHT, 0.4 + 0.1 * Math.sin(time * 2 + number));
            }
            default -> {
                // Le lierre : une tige qui serpente du bas jusqu'au niveau, et ses feuilles de part et d'autre.
                g.setGlobalAlpha(0.85);
                g.setFill(IVY_DARK);
                g.fillRect(x - r, y + r - r * 0.25, 2 * r, r * 0.3);
                int n = 10;
                double[] xs = new double[n + 1], ys = new double[n + 1];
                for (int k = 0; k <= n; k++) {
                    double t = k / (double) n;
                    xs[k] = x + Math.sin(t * Math.PI * 3 + number) * r * 0.35;
                    ys[k] = y + r - (y + r - level) * t;
                }
                g.setStroke(IVY_DARK);
                g.setLineWidth(Math.max(1.5, r * 0.12));
                g.setGlobalAlpha(0.95);
                g.strokePolyline(xs, ys, n + 1);
                g.setFill(IVY);
                for (int k = 1; k <= n; k += 2) {
                    double side = k % 4 == 1 ? 1 : -1, lr = r * 0.22;
                    g.fillOval(xs[k] + side * lr * 0.8 - lr, ys[k] - lr * 0.6, lr * 2, lr * 1.2);
                }
            }
        }
        g.restore();
        g.setGlobalAlpha(1);
    }
}
