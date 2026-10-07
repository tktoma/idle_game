package idle.ui;

import java.util.List;
import java.util.function.DoubleFunction;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

/**
 * Un graphique en courbes, dessiné à la main : une ou deux séries sur un seul axe vertical.
 *
 * <p>Ce qu'il montre :
 * <ul>
 *   <li>un titre et, s'il y a plusieurs séries, une légende ;</li>
 *   <li>un quadrillage discret, avec des graduations « rondes » sur les deux axes ;</li>
 *   <li>chaque série en trait de 2 px, terminée par un point et par sa dernière valeur ;</li>
 *   <li>au survol, un repère vertical qui s'accroche au relevé le plus proche et affiche ses
 *       valeurs.</li>
 * </ul>
 *
 * <p>Le graphique ne sait rien du jeu : on lui donne des points ({@link Series}) et deux façons
 * d'écrire une valeur, une par axe. Pour une échelle logarithmique, on lui donne directement les
 * puissances de dix, et l'écriture des graduations se charge de les afficher (« 1e12 »).
 *
 * <p>Les couleurs des séries ont été vérifiées sur le fond du graphique : contraste suffisant, et
 * deux séries d'un même graphique restent distinctes pour les daltoniens. Le texte, lui, n'est
 * jamais dans la couleur d'une série.
 */
final class ChartPane extends Pane {

    /** Couleurs de série, vérifiées sur {@link #SURFACE}. */
    static final String BLUE = "#3987e5";
    static final String ORANGE = "#d95926";
    static final String YELLOW = "#c98500";
    static final String VIOLET = "#9085e9";
    static final String GREEN = "#3aa76d";
    static final String TEAL = "#2a9d8f";
    static final String MAGENTA = "#d55181";
    static final String AQUA = "#199e70";

    private static final String SURFACE = "#121821";
    private static final String GRID = "#232d3b";
    private static final String TEXT = "#e8f4ff";
    private static final String TEXT_MUTED = "#8fa3b8";
    private static final double HEIGHT = 210;
    private static final double MAX_WIDTH = 640;
    private static final double LEFT = 62, RIGHT = 78, BOTTOM = 26;
    /** Haut de la zone des courbes : sous le titre et le sous-titre, et sous la légende quand il y en a une. */
    private static final double TOP_PLAIN = 44, TOP_WITH_LEGEND = 62;
    /** Pas « ronds » pour un axe du temps, en secondes : 10 s, 1 min, 1 h, 1 jour… */
    private static final double[] TIME_STEPS = {1, 2, 5, 10, 30, 60, 120, 300, 600, 1_800, 3_600, 7_200, 14_400,
            28_800, 43_200, 86_400, 172_800, 432_000, 864_000};

    /**
     * Une série de points, dans l'ordre des abscisses.
     *
     * @param name  nom affiché dans la légende et au survol
     * @param color couleur du trait
     * @param x     abscisses, croissantes
     * @param y     ordonnées ; {@code NaN} pour un point manquant (le trait s'interrompt)
     */
    record Series(String name, String color, double[] x, double[] y) {}

    private final Canvas canvas = new Canvas(MAX_WIDTH, HEIGHT);
    /** Hauteur du dessin : {@link #HEIGHT}, ou plus pour un graphique chargé ({@link #withHeight(double)}). */
    private double height = HEIGHT;
    private final String title;
    private final String subtitle;
    private final DoubleFunction<String> xLabel;
    private final DoubleFunction<String> yLabel;
    private final boolean timeAxis;
    private final boolean wholeY;
    private final double fixedMin, fixedMax;
    /** Graduations verticales imposées, croissantes, ou {@code null} pour des graduations rondes calculées. */
    private double[] ticks;
    /** Vrai pour marquer chaque point d'un rond : quand les points sont peu nombreux et que chacun compte. */
    private boolean markers = false;
    private List<Series> series = List.of();
    /** Abscisse de la souris sur le dessin, ou {@code NaN} quand elle n'y est pas. */
    private double hover = Double.NaN;

    /**
     * @param title    ce que montre le graphique
     * @param subtitle précision en plus petit (unité, échelle), ou une chaîne vide
     * @param timeAxis vrai si les abscisses sont des secondes : les graduations tombent alors sur
     *                 des durées rondes ; sinon ce sont des numéros, gradués en entiers
     * @param xLabel   comment écrire une abscisse
     * @param yLabel   comment écrire une ordonnée
     * @param wholeY   vrai si les graduations verticales doivent être entières (puissances de dix)
     * @param fixedMin bas de l'axe vertical, ou {@code NaN} pour le déduire des points
     * @param fixedMax haut de l'axe vertical, ou {@code NaN} pour le déduire des points
     */
    ChartPane(String title, String subtitle, boolean timeAxis, DoubleFunction<String> xLabel,
              DoubleFunction<String> yLabel, boolean wholeY, double fixedMin, double fixedMax) {
        this.title = title;
        this.subtitle = subtitle;
        this.timeAxis = timeAxis;
        this.xLabel = xLabel;
        this.yLabel = yLabel;
        this.wholeY = wholeY;
        this.fixedMin = fixedMin;
        this.fixedMax = fixedMax;
        setMinSize(0, HEIGHT);
        setPrefSize(MAX_WIDTH, HEIGHT);
        setMaxWidth(MAX_WIDTH);
        getChildren().add(canvas);
        canvas.setOnMouseMoved(event -> {
            hover = event.getX();
            draw();
        });
        canvas.setOnMouseExited(event -> {
            hover = Double.NaN;
            draw();
        });
    }

    /**
     * Impose les graduations verticales, quand les valeurs rondes ne sont pas des puissances de
     * dix : 1 s, 10 s, 1 min, 10 min, 1 h… L'axe va alors de la graduation juste sous les points à
     * celle juste au-dessus.
     */
    ChartPane withTicks(double... ticks) {
        this.ticks = ticks;
        return this;
    }

    /** Donne au dessin une autre hauteur que l'ordinaire : pour un graphique qui porte beaucoup de courbes. */
    ChartPane withHeight(double height) {
        this.height = height;
        canvas.setHeight(height);
        setMinSize(0, height);
        setPrefSize(MAX_WIDTH, height);
        return this;
    }

    /** Marque chaque point d'un rond, tant qu'il y en a peu : pour des valeurs comptées une à une, pas pour une courbe continue. */
    ChartPane withMarkers() {
        this.markers = true;
        return this;
    }

    /** Remplace les séries affichées, puis redessine. */
    void show(List<Series> series) {
        this.series = series;
        draw();
    }

    @Override
    protected void layoutChildren() {
        double width = Math.floor(Math.min(getWidth(), MAX_WIDTH));
        if (width <= 0) return;
        if (canvas.getWidth() != width) {
            canvas.setWidth(width);     // le dessin suit la largeur de la page
            draw();
        }
        canvas.relocate(0, 0);
    }

    // ------------------------------------------------------------------
    // Le dessin
    // ------------------------------------------------------------------

    private void draw() {
        double width = canvas.getWidth();
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.clearRect(0, 0, width, height);
        g.setFill(Color.web(SURFACE));
        g.fillRect(0, 0, width, height);

        g.setTextAlign(TextAlignment.LEFT);
        g.setFill(Color.web(TEXT));
        g.setFont(Font.font("System", FontWeight.BOLD, 13));
        g.fillText(title, 12, 19);
        g.setFont(Font.font(11));
        g.setFill(Color.web(TEXT_MUTED));
        if (!subtitle.isEmpty()) g.fillText(subtitle, 12, 34);

        double top = series.size() > 1 ? TOP_WITH_LEGEND : TOP_PLAIN;
        double plotWidth = width - LEFT - RIGHT;
        double plotHeight = height - top - BOTTOM;
        if (plotWidth < 40) return;     // fenêtre trop étroite pour un graphique lisible

        // L'étendue des points.
        double x0 = Double.POSITIVE_INFINITY, x1 = Double.NEGATIVE_INFINITY;
        double y0 = Double.POSITIVE_INFINITY, y1 = Double.NEGATIVE_INFINITY;
        for (Series each : series) {
            for (int i = 0; i < each.x().length; i++) {
                double y = each.y()[i];
                if (!isFinite(y)) continue;
                x0 = Math.min(x0, each.x()[i]);
                x1 = Math.max(x1, each.x()[i]);
                y0 = Math.min(y0, y);
                y1 = Math.max(y1, y);
            }
        }
        if (x0 > x1) {
            g.setTextAlign(TextAlignment.CENTER);
            g.fillText("Pas encore de relevé", LEFT + plotWidth / 2, top + plotHeight / 2);
            return;
        }
        if (x1 - x0 < 1e-9) x1 = x0 + 1;
        if (isFinite(fixedMin)) y0 = fixedMin;
        if (isFinite(fixedMax)) y1 = fixedMax;

        // Graduations verticales rondes ; l'axe s'étend jusqu'à la graduation qui englobe les points.
        double yStep = niceStep(Math.max(y1 - y0, wholeY ? 1 : 1e-9) / 4);
        if (wholeY) yStep = Math.max(1, Math.rint(yStep));
        double yMin = isFinite(fixedMin) ? fixedMin : Math.floor(y0 / yStep + 1e-9) * yStep;
        double yMax = isFinite(fixedMax) ? fixedMax : Math.ceil(y1 / yStep - 1e-9) * yStep;
        // Des puissances de dix à peine sous zéro (0,25 par seconde au tout début) ne valent pas une
        // graduation de plus quand le pas est grand : l'axe part alors de 1.
        if (wholeY && yStep > 1 && y0 < 0 && y0 >= -1 && !isFinite(fixedMin)) yMin = 0;
        if (yMax - yMin < yStep) yMax = yMin + yStep;
        double[] marks;
        if (ticks != null && ticks.length >= 2) {
            // Graduations imposées : de celle juste sous les points à celle juste au-dessus.
            int low = 0, high = ticks.length - 1;
            while (low < ticks.length - 2 && ticks[low + 1] <= y0) low++;
            while (high > low + 1 && ticks[high - 1] >= y1) high--;
            marks = java.util.Arrays.copyOfRange(ticks, low, high + 1);
            yMin = Math.min(marks[0], y0);
            yMax = Math.max(marks[marks.length - 1], y1);
        } else {
            marks = new double[(int) Math.round((yMax - yMin) / yStep) + 1];
            for (int i = 0; i < marks.length; i++) marks[i] = yMin + i * yStep;
        }

        // Le quadrillage, discret, et les graduations.
        g.setLineWidth(1);
        g.setStroke(Color.web(GRID));
        g.setFill(Color.web(TEXT_MUTED));
        g.setFont(Font.font(11));
        g.setTextAlign(TextAlignment.RIGHT);
        for (double value : marks) {
            double y = Math.floor(top + plotHeight - (value - yMin) / (yMax - yMin) * plotHeight) + 0.5;
            g.strokeLine(LEFT, y, LEFT + plotWidth, y);
            g.fillText(yLabel.apply(value), LEFT - 8, y + 4);
        }
        g.setTextAlign(TextAlignment.CENTER);
        double xStep = xStep(x1 - x0, Math.max(2, (int) (plotWidth / 90)));
        for (double value = Math.ceil(x0 / xStep - 1e-9) * xStep; value <= x1 + 1e-9; value += xStep) {
            double x = Math.floor(LEFT + (value - x0) / (x1 - x0) * plotWidth) + 0.5;
            g.strokeLine(x, top + plotHeight, x, top + plotHeight + 4);
            g.fillText(xLabel.apply(value), x, height - 8);
        }

        // La légende, seulement quand il y a plusieurs séries : sinon le titre suffit.
        if (series.size() > 1) {
            g.setTextAlign(TextAlignment.LEFT);
            double x = LEFT;
            for (Series each : series) {
                g.setStroke(Color.web(each.color()));
                g.setLineWidth(2);
                g.strokeLine(x, top - 13.5, x + 14, top - 13.5);
                g.setFill(Color.web(TEXT_MUTED));
                g.fillText(each.name(), x + 19, top - 10);
                x += 30 + each.name().length() * 6.2;
            }
        }

        // Les séries : un trait de 2 px, puis un point et la dernière valeur au bout.
        g.setLineCap(StrokeLineCap.ROUND);
        g.setLineJoin(StrokeLineJoin.ROUND);
        List<double[]> ends = new java.util.ArrayList<>();
        List<String> endTexts = new java.util.ArrayList<>();
        for (Series each : series) {
            g.setStroke(Color.web(each.color()));
            g.setLineWidth(2);
            g.beginPath();
            boolean drawing = false;
            int last = -1;
            for (int i = 0; i < each.x().length; i++) {
                double value = each.y()[i];
                if (!isFinite(value)) {
                    drawing = false;
                    continue;
                }
                double x = LEFT + (each.x()[i] - x0) / (x1 - x0) * plotWidth;
                double y = top + plotHeight - (clamp(value, yMin, yMax) - yMin) / (yMax - yMin) * plotHeight;
                if (drawing) g.lineTo(x, y);
                else g.moveTo(x, y);
                drawing = true;
                last = i;
            }
            g.stroke();
            if (last < 0) continue;
            if (markers && each.x().length <= 40) {
                for (int i = 0; i < last; i++) {
                    if (!isFinite(each.y()[i])) continue;
                    dot(g, LEFT + (each.x()[i] - x0) / (x1 - x0) * plotWidth,
                            top + plotHeight - (clamp(each.y()[i], yMin, yMax) - yMin) / (yMax - yMin) * plotHeight, each.color());
                }
            }
            double x = LEFT + (each.x()[last] - x0) / (x1 - x0) * plotWidth;
            double y = top + plotHeight - (clamp(each.y()[last], yMin, yMax) - yMin) / (yMax - yMin) * plotHeight;
            dot(g, x, y, each.color());
            ends.add(new double[] {x + 9, y + 4});
            endTexts.add(yLabel.apply(each.y()[last]));
        }
        g.setLineCap(StrokeLineCap.BUTT);
        // Les dernières valeurs, au bout des courbes. Trop proches, elles s'écartent pour rester lisibles :
        // de haut en bas, chacune au moins 13 px sous la précédente, puis le tout remonte s'il déborde en bas.
        Integer[] order = new Integer[ends.size()];
        for (int i = 0; i < order.length; i++) order[i] = i;
        java.util.Arrays.sort(order, java.util.Comparator.comparingDouble(i -> ends.get(i)[1]));
        for (int i = 1; i < order.length; i++) {
            double[] above = ends.get(order[i - 1]), here = ends.get(order[i]);
            if (here[1] - above[1] < 13) here[1] = above[1] + 13;
        }
        double floor = top + plotHeight + 4;
        for (int i = order.length - 1; i >= 0; i--) {
            double[] here = ends.get(order[i]);
            if (here[1] > floor) here[1] = floor;
            floor = here[1] - 13;
        }
        g.setFill(Color.web(TEXT));
        g.setTextAlign(TextAlignment.LEFT);
        for (int i = 0; i < ends.size(); i++) g.fillText(endTexts.get(i), ends.get(i)[0], ends.get(i)[1]);

        if (isFinite(hover)) drawHover(g, x0, x1, yMin, yMax, top, plotWidth, plotHeight);
    }

    /** Le repère du survol : un trait vertical sur le relevé le plus proche de la souris, et ses valeurs. */
    private void drawHover(GraphicsContext g, double x0, double x1, double yMin, double yMax,
                           double top, double plotWidth, double plotHeight) {
        if (hover < LEFT - 6 || hover > LEFT + plotWidth + 6) return;
        double wanted = x0 + (hover - LEFT) / plotWidth * (x1 - x0);
        // Le relevé le plus proche, sur la première série ; les autres séries partagent ses abscisses.
        Series first = series.get(0);
        int nearest = -1;
        for (int i = 0; i < first.x().length; i++) {
            if (nearest < 0 || Math.abs(first.x()[i] - wanted) < Math.abs(first.x()[nearest] - wanted)) nearest = i;
        }
        if (nearest < 0) return;
        double x = Math.floor(LEFT + (first.x()[nearest] - x0) / (x1 - x0) * plotWidth) + 0.5;
        g.setLineWidth(1);
        g.setStroke(Color.web(TEXT_MUTED));
        g.strokeLine(x, top, x, top + plotHeight);

        int lines = 1;
        for (Series each : series) {
            if (nearest < each.y().length && isFinite(each.y()[nearest])) {
                double y = top + plotHeight - (clamp(each.y()[nearest], yMin, yMax) - yMin) / (yMax - yMin) * plotHeight;
                dot(g, x, y, each.color());
                lines++;
            }
        }
        // L'encadré se place du côté où il y a de la place, sans jamais sortir de la zone des courbes.
        double boxWidth = Math.min(series.size() > 1 ? 186 : 130, plotWidth - 4), boxHeight = 8 + 15 * lines;
        double boxX = x + 10 + boxWidth <= LEFT + plotWidth ? x + 10 : x - 10 - boxWidth;
        boxX = clamp(boxX, LEFT + 2, LEFT + plotWidth - boxWidth - 2);
        g.setFill(Color.web("#0b0e14"));
        g.fillRect(boxX, top + 2, boxWidth, boxHeight);
        g.setStroke(Color.web(GRID));
        g.strokeLine(boxX, top + 2.5, boxX + boxWidth, top + 2.5);
        g.strokeLine(boxX, top + 1.5 + boxHeight, boxX + boxWidth, top + 1.5 + boxHeight);
        g.setTextAlign(TextAlignment.LEFT);
        g.setFont(Font.font(11));
        g.setFill(Color.web(TEXT_MUTED));
        double textY = top + 16;
        g.fillText(xLabel.apply(first.x()[nearest]), boxX + 8, textY);
        for (Series each : series) {
            if (nearest >= each.y().length || !isFinite(each.y()[nearest])) continue;
            textY += 15;
            // Une touche de couleur à côté du texte : c'est elle qui dit de quelle série il s'agit.
            g.setStroke(Color.web(each.color()));
            g.setLineWidth(2);
            g.strokeLine(boxX + 8, textY - 4, boxX + 18, textY - 4);
            g.setFill(Color.web(TEXT));
            g.fillText(yLabel.apply(each.y()[nearest]) + (series.size() > 1 ? "  " + each.name() : ""), boxX + 24, textY);
        }
    }

    /** Un point de 8 px dans la couleur de la série, cerclé de la couleur du fond pour rester lisible sur le trait. */
    private static void dot(GraphicsContext g, double x, double y, String color) {
        g.setFill(Color.web(SURFACE));
        g.fillOval(x - 6, y - 6, 12, 12);
        g.setFill(Color.web(color));
        g.fillOval(x - 4, y - 4, 8, 8);
    }

    // ------------------------------------------------------------------
    // Les graduations
    // ------------------------------------------------------------------

    /** Le pas « rond » (1, 2 ou 5 fois une puissance de dix) juste au-dessus de {@code rough}. */
    static double niceStep(double rough) {
        if (!(rough > 0) || Double.isInfinite(rough)) return 1;
        double power = Math.pow(10, Math.floor(Math.log10(rough)));
        double fraction = rough / power;
        return (fraction <= 1 ? 1 : fraction <= 2 ? 2 : fraction <= 5 ? 5 : 10) * power;
    }

    /** Le pas des graduations horizontales : une durée ronde pour un axe du temps, un entier sinon. */
    private double xStep(double span, int maxTicks) {
        if (!timeAxis) return Math.max(1, Math.rint(niceStep(span / maxTicks)));
        for (double step : TIME_STEPS) {
            if (span / step <= maxTicks) return step;
        }
        return niceStep(span / maxTicks);
    }

    private static boolean isFinite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
