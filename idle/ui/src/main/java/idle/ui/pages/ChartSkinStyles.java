package idle.ui.pages;

import idle.core.universe.Skin;
import idle.core.universe.SkinVisual;
import java.util.Map;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Les skins des graphiques (version 0.198). Les courbes ({@link SkinVisual#CHART}) : « Papier millimétré », un
 * quadrillage fin bleu-vert sur du papier crème, un trait plus marqué tous les cinq ; « Oscilloscope », du vert sur
 * du noir, un réticule au centre et une rémanence : la courbe s'éclaire vers son bout et traîne un halo ;
 * « Crayon », du graphite sur du papier, les courbes tracées deux fois en léger décalage comme à main levée, le
 * quadrillage en tirets ; « Aquarelle », l'aire sous chaque courbe lavée de sa couleur, en deux passages, sur du
 * papier blanc. La palette des séries ({@link SkinVisual#CHART_PALETTE}) remplace les huit couleurs du graphique :
 * « Pastel », « Néon », « Niveaux de gris » ; la daltonien-sûre est celle d'origine.
 */
final class ChartSkinStyles {

    /** Les couleurs d'un graphique dans un skin : le fond, le quadrillage, le texte, le texte estompé, l'encadré du survol. */
    record Look(Color surface, Color grid, Color text, Color muted, Color box) {
    }

    private static final Look CLASSIC = new Look(Color.web("#121821"), Color.web("#232d3b"), Color.web("#e8f4ff"), Color.web("#8fa3b8"), Color.web("#0b0e14"));
    private static final Look GRAPH_PAPER = new Look(Color.web("#f7f3e3"), Color.web("#8fb8a8"), Color.web("#243040"), Color.web("#4a6070"), Color.web("#fffdf5"));
    private static final Look OSCILLOSCOPE = new Look(Color.web("#050a05"), Color.web("#0f3a12"), Color.web("#9cff8a"), Color.web("#4fb04a"), Color.web("#0a140a"));
    private static final Look PENCIL = new Look(Color.web("#f1ede2"), Color.web("#b8b2a4"), Color.web("#3a3a44"), Color.web("#6e6e78"), Color.web("#faf8f2"));
    private static final Look WATERCOLOR = new Look(Color.web("#f8f6f0"), Color.web("#e2ded2"), Color.web("#3a3a44"), Color.web("#7a7a84"), Color.web("#ffffff"));
    /** Le pas du quadrillage fin du papier millimétré, en pixels, et les verts de l'oscilloscope quand il y a plusieurs séries. */
    private static final double MILLIMETRE = 8;
    private static final String[] PHOSPHORS = {"#39ff14", "#b8ff80", "#1fb34a", "#e8ffd0", "#7fe070", "#c0ffb0", "#50c040", "#a0ff40"};

    private static final Map<String, String> PASTEL = Map.of(ChartPane.BLUE, "#9fc4f0", ChartPane.ORANGE, "#f2b08c", ChartPane.YELLOW, "#f0d690",
            ChartPane.VIOLET, "#c4bdf2", ChartPane.GREEN, "#a6dcc0", ChartPane.TEAL, "#9fd6d0", ChartPane.MAGENTA, "#f0a8c0", ChartPane.AQUA, "#9ad8bf");
    private static final Map<String, String> NEON = Map.of(ChartPane.BLUE, "#00c8ff", ChartPane.ORANGE, "#ff5a1f", ChartPane.YELLOW, "#ffe600",
            ChartPane.VIOLET, "#b56bff", ChartPane.GREEN, "#39ff14", ChartPane.TEAL, "#00ffd0", ChartPane.MAGENTA, "#ff2fd6", ChartPane.AQUA, "#00ff9c");
    private static final Map<String, String> GRAYSCALE = Map.of(ChartPane.BLUE, "#e8e8e8", ChartPane.ORANGE, "#a0a0a0", ChartPane.YELLOW, "#c8c8c8",
            ChartPane.VIOLET, "#8c8c8c", ChartPane.GREEN, "#b4b4b4", ChartPane.TEAL, "#787878", ChartPane.MAGENTA, "#dcdcdc", ChartPane.AQUA, "#969696");

    private ChartSkinStyles() {
    }

    /** Les couleurs du graphique dans ce skin ; {@code null} ou un skin d'un autre visuel : le Classique. */
    static Look look(Skin skin) {
        if (skin == null || skin.visual() != SkinVisual.CHART) return CLASSIC;
        return switch (skin) {
            case CHART_GRAPH_PAPER -> GRAPH_PAPER;
            case CHART_OSCILLOSCOPE -> OSCILLOSCOPE;
            case CHART_PENCIL -> PENCIL;
            case CHART_WATERCOLOR -> WATERCOLOR;
            default -> CLASSIC;
        };
    }

    /**
     * La couleur d'une série dans cette palette : l'une des huit couleurs du graphique remplacée par sa voisine de la
     * palette, une autre couleur rendue telle quelle ; {@code null} ou un skin d'un autre visuel : la couleur d'origine.
     */
    static String seriesColor(Skin palette, String classic) {
        if (palette == null || palette.visual() != SkinVisual.CHART_PALETTE) return classic;
        Map<String, String> map = switch (palette) {
            case CHART_PASTEL -> PASTEL;
            case CHART_NEON -> NEON;
            case CHART_GRAYSCALE -> GRAYSCALE;
            default -> Map.of();
        };
        return map.getOrDefault(classic, classic);
    }

    /**
     * La couleur du trait d'une série, dans le skin des courbes : l'oscilloscope passe tout au vert phosphore (une nuance
     * par série), le crayon au graphite teinté ; les autres gardent la couleur de la palette.
     */
    static Color strokeColor(Skin skin, String color, int index) {
        if (skin == Skin.CHART_OSCILLOSCOPE) return Color.web(PHOSPHORS[index % PHOSPHORS.length]);
        if (skin == Skin.CHART_PENCIL) return Color.web(color).deriveColor(0, 0.35, 0.55, 1);
        return Color.web(color);
    }

    /** Ce que le skin ajoute sous le quadrillage : le quadrillage fin du papier millimétré. */
    static void underGrid(GraphicsContext g, Skin skin, double left, double top, double width, double height) {
        if (skin != Skin.CHART_GRAPH_PAPER) return;
        g.setLineWidth(1);
        int k = 0;
        for (double x = left; x <= left + width + 0.1; x += MILLIMETRE, k++) {
            g.setStroke(k % 5 == 0 ? Color.web("#9fc8b8") : Color.web("#d2e6dc"));
            g.strokeLine(Math.floor(x) + 0.5, top, Math.floor(x) + 0.5, top + height);
        }
        k = 0;
        for (double y = top + height; y >= top - 0.1; y -= MILLIMETRE, k++) {
            g.setStroke(k % 5 == 0 ? Color.web("#9fc8b8") : Color.web("#d2e6dc"));
            g.strokeLine(left, Math.floor(y) + 0.5, left + width, Math.floor(y) + 0.5);
        }
    }

    /** Les tirets du quadrillage du crayon : posés avant les lignes, retirés après ({@link #plainGrid}). */
    static void dashedGrid(GraphicsContext g, Skin skin) {
        if (skin == Skin.CHART_PENCIL) g.setLineDashes(3, 3);
    }

    static void plainGrid(GraphicsContext g) {
        g.setLineDashes();
    }

    /** Ce que le skin ajoute par-dessus le quadrillage : le réticule de l'oscilloscope, deux axes plus clairs au milieu. */
    static void overGrid(GraphicsContext g, Skin skin, double left, double top, double width, double height) {
        if (skin != Skin.CHART_OSCILLOSCOPE) return;
        g.setStroke(Color.web("#1f6a26"));
        g.setLineWidth(1);
        double cx = Math.floor(left + width / 2) + 0.5, cy = Math.floor(top + height / 2) + 0.5;
        g.strokeLine(cx, top, cx, top + height);
        g.strokeLine(left, cy, left + width, cy);
        for (double x = left; x <= left + width; x += width / 10) g.strokeLine(Math.floor(x) + 0.5, cy - 3, Math.floor(x) + 0.5, cy + 3);
        for (double y = top; y <= top + height; y += height / 8) g.strokeLine(cx - 3, Math.floor(y) + 0.5, cx + 3, Math.floor(y) + 0.5);
    }

    /**
     * Trace une série dans le skin des courbes, à partir de ses points à l'écran ({@code ok[i]} faux pour un point
     * manquant : le trait s'interrompt). L'oscilloscope : un halo large et pâle, un trait moyen, puis le trait fin
     * dont l'alpha monte du début à la fin, la rémanence. Le crayon : deux traits décalés d'un demi-pixel, à 60 %.
     * L'aquarelle : l'aire jusqu'à {@code baseline} lavée deux fois (la seconde un peu plus haute et plus claire), puis
     * le trait. Le papier millimétré : le trait ordinaire.
     */
    static void series(GraphicsContext g, Skin skin, Color color, double[] xs, double[] ys, boolean[] ok, double baseline) {
        switch (skin) {
            case CHART_OSCILLOSCOPE -> {
                g.setStroke(color);
                g.setGlobalAlpha(0.15);
                g.setLineWidth(7);
                path(g, xs, ys, ok);
                g.setGlobalAlpha(0.4);
                g.setLineWidth(3);
                path(g, xs, ys, ok);
                g.setLineWidth(1.5);
                int n = xs.length;
                for (int i = 1; i < n; i++) {
                    if (!ok[i] || !ok[i - 1]) continue;
                    g.setGlobalAlpha(0.35 + 0.65 * i / Math.max(1, n - 1));
                    g.strokeLine(xs[i - 1], ys[i - 1], xs[i], ys[i]);
                }
                g.setGlobalAlpha(1);
            }
            case CHART_PENCIL -> {
                g.setStroke(color);
                g.setLineWidth(1.6);
                g.setGlobalAlpha(0.6);
                path(g, xs, ys, ok);
                g.translate(0.6, -0.5);
                path(g, xs, ys, ok);
                g.translate(-0.6, 0.5);
                g.setGlobalAlpha(1);
            }
            case CHART_WATERCOLOR -> {
                g.setFill(color);
                g.setGlobalAlpha(0.22);
                wash(g, xs, ys, ok, baseline, 0);
                g.setGlobalAlpha(0.14);
                wash(g, xs, ys, ok, baseline, -4);
                g.setGlobalAlpha(0.9);
                g.setStroke(color);
                g.setLineWidth(2.5);
                path(g, xs, ys, ok);
                g.setGlobalAlpha(1);
            }
            default -> {
                g.setStroke(color);
                g.setLineWidth(2);
                path(g, xs, ys, ok);
            }
        }
    }

    /** Le trait qui joint les points, interrompu aux points manquants. */
    private static void path(GraphicsContext g, double[] xs, double[] ys, boolean[] ok) {
        g.beginPath();
        boolean drawing = false;
        for (int i = 0; i < xs.length; i++) {
            if (!ok[i]) {
                drawing = false;
                continue;
            }
            if (drawing) g.lineTo(xs[i], ys[i]);
            else g.moveTo(xs[i], ys[i]);
            drawing = true;
        }
        g.stroke();
    }

    /** L'aire entre la courbe (relevée de {@code lift}) et la ligne de base, un morceau par suite de points présents. */
    private static void wash(GraphicsContext g, double[] xs, double[] ys, boolean[] ok, double baseline, double lift) {
        int start = -1;
        for (int i = 0; i <= xs.length; i++) {
            boolean present = i < xs.length && ok[i];
            if (present && start < 0) start = i;
            if (!present && start >= 0) {
                g.beginPath();
                g.moveTo(xs[start], baseline);
                for (int k = start; k < i; k++) g.lineTo(xs[k], Math.min(baseline, ys[k] + lift));
                g.lineTo(xs[i - 1], baseline);
                g.closePath();
                g.fill();
                start = -1;
            }
        }
    }
}
