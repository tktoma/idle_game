package idle.ui;

import idle.core.BigNum;
import idle.core.Landmark;
import idle.core.SizeScale;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;

/**
 * Le point de matière noire et l'échelle des grandeurs autour de lui.
 *
 * <p>La vue fonctionne comme une caméra qui recule : quand la matière noire grossit, le point enfle
 * un instant, puis la caméra dézoome pour le ramener à sa taille habituelle à l'écran. Ce qui
 * montre la croissance, ce sont donc les repères autour de lui :
 * <ul>
 *   <li>des cercles gradués, un par longueur « ronde » (1, 2 ou 5 × 10ⁿ), chacun avec sa taille ;</li>
 *   <li>des cercles plus marqués pour les repères de {@link SizeScale} (atome, virus, Terre…),
 *       avec leur nom ;</li>
 *   <li>une règle graduée en bas à gauche, qui indique ce que vaut une longueur à l'écran.</li>
 * </ul>
 * Tous ces cercles se resserrent vers le centre à mesure que la caméra recule, jusqu'à être
 * avalés par le point.
 * <ul>
 * </ul>
 *
 * <p>La vue se dessine d'après sa taille du moment : elle suit la fenêtre, et la caméra se
 * recale d'elle-même quand la vue change de taille.
 *
 * <p>Tous les calculs se font sur le logarithme des tailles : elles vont du proton à l'univers
 * et au-delà, bien plus qu'un nombre de pixels ne peut en contenir.
 */
final class DarkMatterView extends Canvas {

    /** Part de la plus petite dimension de la vue qu'occupe le rayon du point, au repos. */
    private static final double REST_RADIUS = 0.16;
    /** Rayon maximal du point à l'écran, même quand il grossit plus vite que la caméra ne recule. */
    private static final double MAX_RADIUS = 0.40;
    /** Vitesse à laquelle la caméra rattrape la taille du point : plus c'est grand, plus elle suit de près. */
    private static final double CAMERA_SPEED = 1.5;
    /** Rayon minimal de la zone cliquable, en pixels, pour que le point reste facile à viser. */
    private static final double MIN_HIT_RADIUS = 30;
    /** Longueur maximale de la règle graduée, en pixels. */
    private static final double RULER_MAX = 120;
    /** Place qu'il faut à droite d'un cercle pour y écrire son texte, en pixels. */
    private static final double LABEL_ROOM = 110;
    /** Les longueurs « rondes » d'une puissance de dix : 1, 2 et 5. */
    private static final double[] ROUND_VALUES = {1, 2, 5};

    private static final Color BACKGROUND = Color.web("#0d0a16");
    private static final Color CORE = Color.web("#efe4ff");
    private static final Color GLOW = Color.web("#c9a6ff");
    private static final Color RING = Color.web("#8f7fb8");
    private static final Color TEXT = Color.web("#c9bfe0");

    /** Logarithme décimal du nombre de mètres que représente un pixel ; {@code NaN} avant la première image. */
    private double logScale = Double.NaN;
    private double time = 0;
    private double blobRadius = 0;

    DarkMatterView(double width, double height) {
        super(width, height);
    }

    /** Vrai si ce point de la vue est sur la matière noire (avec un peu de marge quand elle est petite). */
    boolean isOnDarkMatter(double x, double y) {
        double dx = x - getWidth() / 2;
        double dy = y - getHeight() / 2;
        double reach = Math.max(blobRadius, MIN_HIT_RADIUS);
        return dx * dx + dy * dy <= reach * reach;
    }

    /**
     * Avance l'animation et redessine.
     *
     * @param elapsed secondes écoulées depuis l'image précédente
     * @param size    taille de la matière noire (son diamètre), en mètres
     * @param growing vrai tant que le joueur maintient le point appuyé
     */
    void frame(double elapsed, BigNum size, boolean growing) {
        double dt = Math.min(elapsed, 0.1);
        time += dt;
        double w = getWidth();
        double h = getHeight();
        double small = Math.min(w, h);
        if (small < 40) return;                              // fenêtre trop petite pour dessiner quoi que ce soit
        double logRadius = size.log10() - Math.log10(2);

        // La caméra : elle vise l'échelle où le point a sa taille de repos, et s'en approche peu à peu.
        double target = logRadius - Math.log10(small * REST_RADIUS);
        if (Double.isNaN(logScale)) logScale = target;
        logScale += (target - logScale) * Math.min(1, dt * CAMERA_SPEED);
        // Si le point grossit trop vite pour elle, elle recule quand même assez pour qu'il tienne dans la vue.
        logScale = Math.max(logScale, logRadius - Math.log10(small * MAX_RADIUS));
        blobRadius = Math.pow(10, logRadius - logScale);

        GraphicsContext g = getGraphicsContext2D();
        g.setGlobalAlpha(1);
        g.setFill(BACKGROUND);
        g.fillRect(0, 0, w, h);
        double cx = w / 2;
        double cy = h / 2;

        // L'échelle des grandeurs : des cercles gradués, et des cercles plus marqués pour les objets connus.
        g.setLineWidth(1);
        double minRadius = blobRadius * 1.08;
        double maxRadius = w / 2;
        double[] landmarkRadii = new double[SizeScale.LANDMARKS.size()];
        int landmarks = 0;
        for (Landmark landmark : SizeScale.LANDMARKS) {
            double logRing = landmark.size().log10() - Math.log10(2) - logScale;
            if (logRing > 4) continue;                       // des milliers de fois plus grand que la vue
            double radius = Math.pow(10, logRing);
            if (radius <= minRadius || radius > maxRadius) continue;
            landmarkRadii[landmarks++] = radius;
            drawRing(g, cx, cy, radius, landmark.name() + " (" + Format.length(landmark.size()) + ")", true);
        }
        // Les graduations : en mètres, ou en années-lumière quand les kilomètres ne suffisent plus.
        double logMeters = Math.log10(2 * minRadius) + logScale;
        double logUnit = logMeters >= Format.LIGHT_YEARS_FROM.log10() ? Format.LIGHT_YEAR.log10() : 0;
        for (double exponent = Math.floor(logMeters - logUnit); ; exponent++) {
            boolean beyond = false;
            for (double round : ROUND_VALUES) {
                double logSize = Math.log10(round) + exponent + logUnit;     // diamètre du cercle, en mètres
                double radius = Math.pow(10, logSize - Math.log10(2) - logScale);
                if (radius > maxRadius) { beyond = true; break; }
                if (radius <= minRadius) continue;
                boolean taken = false;                       // un repère occupe déjà cette place
                for (int i = 0; i < landmarks; i++) taken |= Math.abs(landmarkRadii[i] - radius) < 14;
                if (!taken) drawRing(g, cx, cy, radius, Format.length(BigNum.pow10(logSize)), false);
            }
            if (beyond) break;
        }

        // Le point : un halo qui bat quand on appuie, et un cœur clair.
        double pulse = growing ? 1 + 0.08 * Math.sin(time * 9) : 1;
        g.setFill(GLOW);
        g.setGlobalAlpha(growing ? 0.22 : 0.12);
        double halo = blobRadius * 1.5 * pulse;
        g.fillOval(cx - halo, cy - halo, halo * 2, halo * 2);
        g.setGlobalAlpha(growing ? 0.5 : 0.35);
        double mid = blobRadius * 1.18 * pulse;
        g.fillOval(cx - mid, cy - mid, mid * 2, mid * 2);
        g.setGlobalAlpha(1);
        g.setFill(growing ? CORE : GLOW);
        g.fillOval(cx - blobRadius, cy - blobRadius, blobRadius * 2, blobRadius * 2);

        drawRuler(g, h);
    }

    /**
     * Un cercle de l'échelle, avec son texte au-dessus (ou en haut à droite quand le haut du
     * cercle sort de la vue).
     *
     * @param landmark vrai pour un objet connu, dessiné plus net qu'une simple graduation
     */
    private void drawRing(GraphicsContext g, double cx, double cy, double radius, String text, boolean landmark) {
        g.setGlobalAlpha(landmark ? 0.9 : 0.35);
        g.setStroke(landmark ? GLOW : RING);
        g.strokeOval(cx - radius, cy - radius, radius * 2, radius * 2);
        g.setGlobalAlpha(landmark ? 1 : 0.6);
        g.setFill(landmark ? CORE : TEXT);
        double top = cy - radius - 5;
        double diagonal = radius * Math.sqrt(0.5);
        if (top > 12) {
            g.setTextAlign(TextAlignment.CENTER);
            g.fillText(text, cx, top);
        } else if (cy - diagonal - 5 > 12) {
            g.setTextAlign(TextAlignment.LEFT);
            g.fillText(text, cx + diagonal + 5, cy - diagonal - 5);
        } else if (cx + radius + 6 + LABEL_ROOM < getWidth()) {
            // Fenêtre large et basse : seul le côté du cercle est visible, le texte se met à sa droite.
            g.setTextAlign(TextAlignment.LEFT);
            g.fillText(text, cx + radius + 6, cy + 4);
        }
    }

    /** La règle graduée : la plus grande longueur « ronde » (1, 2 ou 5 × 10ⁿ mètres) qui tient en {@link #RULER_MAX} pixels. */
    private void drawRuler(GraphicsContext g, double h) {
        double logMax = Math.log10(RULER_MAX) + logScale;    // ce que valent RULER_MAX pixels, en mètres
        // Ronde dans l'unité affichée : en années-lumière quand les kilomètres ne suffisent plus.
        double logUnit = logMax >= Format.LIGHT_YEARS_FROM.log10() ? Format.LIGHT_YEAR.log10() : 0;
        double exponent = Math.floor(logMax - logUnit);
        double mantissa = Math.pow(10, logMax - logUnit - exponent);   // entre 1 et 10
        double round = mantissa >= 5 ? 5 : mantissa >= 2 ? 2 : 1;
        double logLength = Math.log10(round) + exponent + logUnit;     // en mètres
        double length = Math.pow(10, logLength - logScale);
        double x = 16;
        double y = h - 16;
        g.setGlobalAlpha(1);
        g.setStroke(TEXT);
        g.setLineWidth(1.5);
        g.strokeLine(x, y, x + length, y);
        g.strokeLine(x, y - 5, x, y + 5);
        g.strokeLine(x + length, y - 5, x + length, y + 5);
        g.setFill(TEXT);
        g.setTextAlign(TextAlignment.LEFT);
        g.fillText(Format.length(BigNum.pow10(logLength)), x, y - 9);
    }
}
