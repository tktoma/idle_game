package idle.ui.darkmatter;

import idle.core.darkmatter.Challenge;
import idle.core.universe.Skin;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

/**
 * Les skins des défis (version 0.193), dessinés à côté de {@link ChallengesPane} pour la garder lisible : une
 * vignette par carte de défi, posée en haut (tarot, affiche) ou dans l'en-tête (sceau).
 * <ul>
 * <li>« Tarot » : chaque défi est une lame, bordure dorée sur fond crème, son numéro en chiffres romains et un
 * motif pour sa contrainte ; réussie, la lame est à l'endroit, à tenter elle reste de dos (un dos bleu à losanges) ;
 * l'arrivée de la fuite les retourne toutes ({@link ChallengesPane});</li>
 * <li>« Wanted » : une affiche de papier déchiré, « WANTED » en haut, le portrait du défi en silhouette d'encre
 * brune ; réussi, un tampon rouge « CAPTURÉ » en travers ;</li>
 * <li>« Sceaux » : un sceau de cire rouge dans l'en-tête du défi réussi, la place vide en pointillé pour les autres.</li>
 * </ul>
 */
final class ChallengesSkinArt {

    /** Les dimensions des vignettes : la lame et l'affiche en haut de la carte, le sceau dans l'en-tête. */
    static final double TAROT_WIDTH = 40, TAROT_HEIGHT = 60;
    static final double WANTED_WIDTH = 64, WANTED_HEIGHT = 52;
    static final double SEAL = 20;

    private static final Color CREAM = Color.web("#f3ead6");
    private static final Color GOLD = Color.web("#c9a24a");
    private static final Color INK = Color.web("#2a2218");
    private static final Color BACK = Color.web("#2a3f8a");
    private static final Color BACK_LIGHT = Color.web("#5a74c8");
    private static final Color PAPER = Color.web("#d8c49a");
    private static final Color PAPER_DARK = Color.web("#a88c5a");
    private static final Color BROWN = Color.web("#4a3018");
    private static final Color STAMP = Color.web("#c0392b");
    private static final Color WAX = Color.web("#b3202a");
    private static final Color WAX_DARK = Color.web("#7a1218");
    private static final Color WAX_LIGHT = Color.web("#e06a6a");
    private static final Color EMPTY = Color.web("#5d6e80");

    private static final String[] ROMAN = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII"};

    private ChallengesSkinArt() {
    }

    /** Vrai pour les skins dessinés ici : les trois des défis. */
    static boolean draws(Skin skin) {
        return skin == Skin.CHALLENGES_TAROT || skin == Skin.CHALLENGES_WANTED || skin == Skin.CHALLENGES_SEALS;
    }

    /** Vrai si la vignette de ce skin se pose dans l'en-tête de la carte plutôt qu'en haut. */
    static boolean inline(Skin skin) {
        return skin == Skin.CHALLENGES_SEALS;
    }

    /** Une vignette vide aux dimensions de ce skin. */
    static Canvas canvas(Skin skin) {
        return switch (skin) {
            case CHALLENGES_TAROT -> new Canvas(TAROT_WIDTH, TAROT_HEIGHT);
            case CHALLENGES_WANTED -> new Canvas(WANTED_WIDTH, WANTED_HEIGHT);
            default -> new Canvas(SEAL, SEAL);
        };
    }

    /**
     * Dessine la vignette d'un défi.
     *
     * @param index    le rang du défi (0 pour le premier des dix, 10 pour la fuite)
     * @param done     vrai si le défi est réussi
     * @param unlocked vrai si le défi est ouvert
     */
    static void paint(Skin skin, Canvas canvas, Challenge challenge, int index, boolean done, boolean unlocked) {
        GraphicsContext g = canvas.getGraphicsContext2D();
        double w = canvas.getWidth(), h = canvas.getHeight();
        g.clearRect(0, 0, w, h);
        g.setGlobalAlpha(1);
        switch (skin) {
            case CHALLENGES_TAROT -> tarot(g, w, h, challenge, index, done, unlocked);
            case CHALLENGES_WANTED -> wanted(g, w, h, challenge, done, unlocked);
            default -> seal(g, w, h, done);
        }
    }

    /** La lame : à l'endroit une fois le défi réussi, de dos sinon. */
    private static void tarot(GraphicsContext g, double w, double h, Challenge challenge, int index, boolean done, boolean unlocked) {
        if (!done) {
            // Le dos : bleu à losanges, cerné d'or ; grisé tant que le défi est fermé.
            g.setFill(unlocked ? BACK : Color.web("#3a3f4e"));
            g.fillRoundRect(0, 0, w, h, 5, 5);
            g.setStroke(unlocked ? BACK_LIGHT : Color.web("#6a7080"));
            g.setLineWidth(1);
            for (double x = -h; x < w + h; x += 8) {
                g.strokeLine(x, 0, x + h, h);
                g.strokeLine(x + h, 0, x, h);
            }
            g.setStroke(GOLD);
            g.setLineWidth(1.5);
            g.strokeRoundRect(1.5, 1.5, w - 3, h - 3, 4, 4);
            return;
        }
        g.setFill(CREAM);
        g.fillRoundRect(0, 0, w, h, 5, 5);
        g.setStroke(GOLD);
        g.setLineWidth(1.5);
        g.strokeRoundRect(1.5, 1.5, w - 3, h - 3, 4, 4);
        g.strokeRect(5, 12, w - 10, h - 24);
        g.setFill(INK);
        g.setTextAlign(TextAlignment.CENTER);
        g.setFont(Font.font("Serif", FontWeight.BOLD, 9));
        g.fillText(ROMAN[Math.min(index, ROMAN.length - 1)], w / 2, 10);
        motif(g, w / 2, h / 2, Math.min(w, h - 24) * 0.32, challenge, INK);
        g.setFont(Font.getDefault());
    }

    /** L'affiche : du papier déchiré, le titre, le portrait, et le tampon du défi réussi. */
    private static void wanted(GraphicsContext g, double w, double h, Challenge challenge, boolean done, boolean unlocked) {
        // Le bord déchiré : un polygone irrégulier sur tout le tour.
        int steps = 10;
        double[] xs = new double[4 * steps], ys = new double[4 * steps];
        int n = 0;
        for (int k = 0; k < steps; k++) { xs[n] = w * k / steps; ys[n++] = 1 + 2 * ((k * 7) % 3); }
        for (int k = 0; k < steps; k++) { xs[n] = w - 1 - 2 * ((k * 5) % 3); ys[n++] = h * k / steps; }
        for (int k = 0; k < steps; k++) { xs[n] = w - w * k / steps; ys[n++] = h - 1 - 2 * ((k * 3) % 3); }
        for (int k = 0; k < steps; k++) { xs[n] = 1 + 2 * ((k * 11) % 3); ys[n++] = h - h * k / steps; }
        g.setFill(unlocked ? PAPER : Color.web("#8a8070"));
        g.fillPolygon(xs, ys, n);
        g.setStroke(PAPER_DARK);
        g.setLineWidth(1);
        g.strokePolygon(xs, ys, n);
        g.setFill(BROWN);
        g.setTextAlign(TextAlignment.CENTER);
        g.setFont(Font.font("Serif", FontWeight.BOLD, 9));
        g.fillText("WANTED", w / 2, 12);
        // Le portrait : un buste dans un cadre, et le motif du défi en guise de visage.
        g.setStroke(BROWN);
        g.strokeRect(w / 2 - 14, 16, 28, h - 22);
        g.setFill(BROWN);
        g.fillOval(w / 2 - 6, 20, 12, 13);
        g.fillRoundRect(w / 2 - 11, 33, 22, h - 39, 8, 8);
        motif(g, w / 2, 26.5, 3.5, challenge, PAPER);
        if (done) {
            g.save();
            g.translate(w / 2, h / 2);
            g.rotate(-18);
            g.setStroke(STAMP);
            g.setLineWidth(1.5);
            g.strokeRect(-24, -7, 48, 14);
            g.setFill(STAMP);
            g.setFont(Font.font("Serif", FontWeight.BOLD, 9));
            g.fillText("CAPTURÉ", 0, 3.5);
            g.restore();
        }
        g.setFont(Font.getDefault());
    }

    /** Le sceau de cire du défi réussi ; sa place vide, en pointillé, pour les autres. */
    private static void seal(GraphicsContext g, double w, double h, boolean done) {
        double c = w / 2;
        if (!done) {
            g.setStroke(EMPTY);
            g.setLineWidth(1);
            g.setLineDashes(2, 2);
            g.strokeOval(c - 7, c - 7, 14, 14);
            g.setLineDashes();
            return;
        }
        // Une galette de cire aux bords irréguliers, un cachet rond en creux, un reflet.
        int sides = 12;
        double[] xs = new double[sides], ys = new double[sides];
        for (int k = 0; k < sides; k++) {
            double a = 2 * Math.PI * k / sides, r = 8.5 + 1.2 * Math.sin(k * 2.7);
            xs[k] = c + r * Math.cos(a);
            ys[k] = c + r * Math.sin(a);
        }
        g.setFill(WAX);
        g.fillPolygon(xs, ys, sides);
        g.setStroke(WAX_DARK);
        g.setLineWidth(1);
        g.strokePolygon(xs, ys, sides);
        g.strokeOval(c - 5, c - 5, 10, 10);
        g.setFill(WAX_DARK);
        g.fillOval(c - 1.5, c - 1.5, 3, 3);
        g.setFill(WAX_LIGHT);
        g.setGlobalAlpha(0.7);
        g.fillOval(c - 5, c - 6, 4, 2.5);
        g.setGlobalAlpha(1);
    }

    /** Le motif d'un défi, d'après sa contrainte : un petit dessin de rayon {@code r} centré en ({@code x}, {@code y}). */
    private static void motif(GraphicsContext g, double x, double y, double r, Challenge challenge, Color ink) {
        g.setStroke(ink);
        g.setFill(ink);
        g.setLineWidth(Math.max(1, r * 0.15));
        switch (challenge.rule()) {
            case RUSTY_AUTOMATIONS -> {
                // Une roue dentée grippée.
                g.strokeOval(x - r * 0.6, y - r * 0.6, r * 1.2, r * 1.2);
                for (int k = 0; k < 8; k++) {
                    double a = k * Math.PI / 4;
                    g.strokeLine(x + r * 0.6 * Math.cos(a), y + r * 0.6 * Math.sin(a), x + r * Math.cos(a), y + r * Math.sin(a));
                }
            }
            case HEAVY_FUSION -> {
                // Un poids.
                g.fillRect(x - r * 0.8, y - r * 0.3, r * 1.6, r * 1.1);
                g.strokeArc(x - r * 0.35, y - r * 0.9, r * 0.7, r * 0.8, 0, 180, javafx.scene.shape.ArcType.OPEN);
            }
            case VOLATILE_PARTICLES -> {
                // Des bulles qui s'échappent.
                g.strokeOval(x - r * 0.9, y + r * 0.2, r * 0.8, r * 0.8);
                g.strokeOval(x, y - r * 0.4, r * 0.6, r * 0.6);
                g.strokeOval(x - r * 0.3, y - r, r * 0.4, r * 0.4);
            }
            case AMNESIA -> {
                // Un nuage troué.
                g.strokeOval(x - r, y - r * 0.3, r * 2, r * 1.1);
                g.strokeLine(x - r * 0.4, y - r * 0.5, x + r * 0.4, y + r * 0.6);
            }
            case INERT_ELEMENTS -> {
                // Une case barrée.
                g.strokeRect(x - r * 0.7, y - r * 0.7, r * 1.4, r * 1.4);
                g.strokeLine(x - r * 0.7, y - r * 0.7, x + r * 0.7, y + r * 0.7);
            }
            case DECAY -> {
                // Un trèfle de radioactivité.
                for (int k = 0; k < 3; k++) {
                    double a = -90 + k * 120;
                    g.fillArc(x - r, y - r, 2 * r, 2 * r, a - 30, 60, javafx.scene.shape.ArcType.ROUND);
                }
            }
            case PRESENCE_ONLY -> {
                // Un soleil : la présence.
                g.fillOval(x - r * 0.4, y - r * 0.4, r * 0.8, r * 0.8);
                for (int k = 0; k < 8; k++) {
                    double a = k * Math.PI / 4;
                    g.strokeLine(x + r * 0.6 * Math.cos(a), y + r * 0.6 * Math.sin(a), x + r * Math.cos(a), y + r * Math.sin(a));
                }
            }
            case ABSENCE_ONLY -> {
                // Une lune : l'absence.
                g.fillOval(x - r * 0.8, y - r * 0.8, r * 1.6, r * 1.6);
                g.setFill(ink == INK ? CREAM : BROWN);
                g.fillOval(x - r * 0.4, y - r * 0.9, r * 1.5, r * 1.5);
            }
            case CUT_BRANCH -> {
                // Une branche coupée.
                g.strokeLine(x, y + r, x, y - r * 0.2);
                g.strokeLine(x, y + r * 0.2, x - r * 0.8, y - r * 0.6);
                g.strokeLine(x - r * 0.9, y - r * 0.3, x - r * 0.4, y - r * 0.9);
            }
            case BARE_TREE -> {
                // Un tronc nu.
                g.strokeLine(x, y + r, x, y - r);
                g.strokeLine(x - r * 0.6, y - r * 0.9, x + r * 0.6, y - r * 0.9);
            }
            default -> {
                // La fuite : une goutte qui tombe.
                g.fillOval(x - r * 0.5, y - r * 0.2, r, r);
                g.fillPolygon(new double[] {x - r * 0.45, x, x + r * 0.45}, new double[] {y + r * 0.1, y - r, y + r * 0.1}, 3);
            }
        }
    }
}
