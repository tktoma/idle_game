package idle.ui.atoms;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Les dessins des trois skins de la page du tableau périodique hors cases (version 0.192), à côté de
 * {@link PeriodicTablePage} pour la garder lisible :
 * <ul>
 * <li>« Médailles » : chaque tuile d'ensemble porte une médaille à ruban, de bronze dès le premier élément, d'argent à
 * la moitié, d'or quand l'ensemble est complet ; un disque gris vide avant ;</li>
 * <li>« Creuset » : la carte de synthèse porte un creuset d'alchimiste sur son trépied, au-dessus de braises, dont la
 * fumée monte en volutes qui ondulent ;</li>
 * <li>« Spectre » : chaque ligne de la légende des familles devient une bande noire traversée des raies d'émission de
 * la couleur de la famille, le texte en blanc.</li>
 * </ul>
 */
final class TablePageSkinArt {

    /** Le côté de la médaille d'une tuile, en pixels. */
    static final double MEDAL_SIZE = 22;
    /** La taille du creuset sur la carte de synthèse, en pixels. */
    static final double CRUCIBLE_WIDTH = 120;
    static final double CRUCIBLE_HEIGHT = 64;

    private static final Color[] METALS = {Color.web("#5d6e80"), Color.web("#b87333"), Color.web("#c0c8d0"), Color.web("#ffd27f")};
    private static final Color[] METAL_SHADES = {Color.web("#3a4552"), Color.web("#7a4a1e"), Color.web("#7f8a96"), Color.web("#b8902a")};
    private static final Color RIBBON = Color.web("#b02a2a");
    private static final Color IRON = Color.web("#2b2b30");
    private static final Color IRON_LIGHT = Color.web("#5a5a64");
    private static final Color EMBER = Color.web("#ff8a3a");
    private static final Color SMOKE = Color.web("#d8dde6");

    private TablePageSkinArt() {
    }

    /** Dessine une médaille de ce palier : 0 aucune (un disque gris vide), 1 bronze, 2 argent, 3 or. */
    static void drawMedal(Canvas canvas, int tier) {
        double s = canvas.getWidth();
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.clearRect(0, 0, s, canvas.getHeight());
        double c = s / 2;
        // Le ruban, en deux pans derrière la médaille.
        g.setFill(tier == 0 ? Color.web("#3a4552") : RIBBON);
        g.fillPolygon(new double[] {c - s * 0.22, c - s * 0.05, c + s * 0.05, c + s * 0.22}, new double[] {0, s * 0.42, s * 0.42, 0}, 4);
        double r = s * 0.34;
        double y = s * 0.62;
        g.setFill(METAL_SHADES[tier]);
        g.fillOval(c - r, y - r, 2 * r, 2 * r);
        g.setFill(METALS[tier]);
        g.fillOval(c - r * 0.82, y - r * 0.82, 2 * r * 0.82, 2 * r * 0.82);
        if (tier > 0) {
            // L'éclat et l'étoile gravée.
            g.setFill(Color.WHITE);
            g.setGlobalAlpha(0.5);
            g.fillOval(c - r * 0.5, y - r * 0.6, r * 0.4, r * 0.3);
            g.setGlobalAlpha(1);
            g.setFill(METAL_SHADES[tier]);
            double[] xs = new double[10], ys = new double[10];
            for (int k = 0; k < 10; k++) {
                double a = -Math.PI / 2 + k * Math.PI / 5;
                double rr = (k % 2 == 0 ? r * 0.5 : r * 0.22);
                xs[k] = c + rr * Math.cos(a);
                ys[k] = y + rr * Math.sin(a);
            }
            g.fillPolygon(xs, ys, 10);
        }
    }

    /**
     * Dessine le creuset qui fume : le trépied, les braises, le pot et sa gueule, puis trois volutes de fumée qui
     * ondulent avec le temps, plus denses quand la synthèse est à portée.
     */
    static void drawCrucible(Canvas canvas, double time, boolean ready) {
        double w = canvas.getWidth(), h = canvas.getHeight();
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.clearRect(0, 0, w, h);
        double cx = w / 2;
        double base = h * 0.92;
        // Les braises sous le trépied.
        g.setFill(EMBER);
        for (int k = 0; k < 5; k++) {
            double glow = 0.5 + 0.5 * Math.sin(time * 3 + k * 1.7);
            g.setGlobalAlpha(0.4 + 0.6 * glow);
            g.fillOval(cx - 14 + k * 7 - 2.5, base - 5, 5, 4);
        }
        g.setGlobalAlpha(1);
        // Le trépied.
        g.setStroke(IRON);
        g.setLineWidth(2);
        g.strokeLine(cx - 16, base - 2, cx - 10, base - 22);
        g.strokeLine(cx + 16, base - 2, cx + 10, base - 22);
        g.strokeLine(cx, base - 1, cx, base - 20);
        // Le pot : un bol évasé, et sa gueule en ellipse plus claire.
        double top = base - 44;
        g.setFill(IRON);
        g.fillPolygon(new double[] {cx - 26, cx + 26, cx + 17, cx - 17}, new double[] {top, top, base - 20, base - 20}, 4);
        g.setFill(IRON_LIGHT);
        g.fillOval(cx - 26, top - 5, 52, 10);
        g.setFill(ready ? EMBER : IRON);
        g.setGlobalAlpha(ready ? 0.8 : 1);
        g.fillOval(cx - 20, top - 3, 40, 6);
        g.setGlobalAlpha(1);
        // La fumée : trois volutes qui montent en ondulant et s'évanouissent.
        g.setFill(SMOKE);
        for (int k = 0; k < 3; k++) {
            for (int step = 0; step < 6; step++) {
                double rise = ((time * 0.25 + k * 0.33 + step / 6.0) % 1);
                double y = top - 6 - rise * (top - 8);
                double x = cx + (k - 1) * 9 + 8 * Math.sin(rise * 5 + time * 1.5 + k);
                double r = 3 + 6 * rise;
                g.setGlobalAlpha((ready ? 0.45 : 0.3) * (1 - rise));
                g.fillOval(x - r, y - r, 2 * r, 2 * r);
            }
        }
        g.setGlobalAlpha(1);
    }

    /**
     * Le style d'une ligne de la légende en bande de spectre : fond noir traversé de raies d'émission de la couleur
     * de la famille, à des places qui dépendent d'elle, texte blanc.
     */
    static String spectrumStyle(String family) {
        // Les raies, aux places que donnent les chiffres de la couleur : chaque famille a son spectre.
        int seed = Integer.parseInt(family.substring(1, 7), 16);
        StringBuilder stops = new StringBuilder("#000000 0%");
        int at = 4;
        for (int k = 0; k < 7 && at < 95; k++) {
            at += 5 + (seed >> (k * 3)) % 9;
            String alpha = k % 3 == 0 ? "ff" : k % 3 == 1 ? "99" : "cc";
            stops.append(", #000000 ").append(at - 1).append("%, ").append(family).append(alpha).append(' ').append(at)
                    .append("%, #000000 ").append(at + 1).append('%');
        }
        stops.append(", #000000 100%");
        return "-fx-font-size: 12px; -fx-text-fill: #ffffff; -fx-padding: 4 10; -fx-background-radius: 3;"
                + " -fx-background-color: linear-gradient(to right, " + stops + "); -fx-border-color: " + family + "66; -fx-border-radius: 3;";
    }
}
