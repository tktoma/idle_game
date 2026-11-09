package idle.ui.universe;

import idle.core.universe.Skin;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;

/**
 * Le cadre des mini-jeux (version 0.197, {@link idle.core.universe.SkinVisual#DIMENSION_FRAME}) : un calque transparent
 * posé sur l'arène de {@link DimensionWindow}, qui en dessine la bordure sur {@link #THICKNESS} pixels. Un hublot (un
 * anneau de métal rivé, aux coins arrondis), un cadre doré (deux filets d'or et des volutes aux coins), un écran cathodique
 * (un boîtier gris, une lueur dans les coins, de fines lignes de balayage sur l'image et une diode qui veille) ou un livre
 * ouvert (deux pages de papier, leur tranche et la couture au milieu, en haut et en bas). Sans skin, le calque reste vide.
 */
final class DimensionFrameArt {

    private DimensionFrameArt() {
    }

    /** L'épaisseur du cadre : la marge que la fenêtre ajoute autour de l'arène quand un cadre est choisi. */
    static final double THICKNESS = 18;

    private static final Color METAL = Color.web("#8a939c"), METAL_DARK = Color.web("#4a5058"), METAL_LIGHT = Color.web("#c9d1d8"), RIVET = Color.web("#2a2e34");
    private static final Color GOLD = Color.web("#d9a73a"), GOLD_DARK = Color.web("#7a5a14"), GOLD_LIGHT = Color.web("#ffe9a8");
    private static final Color CASE = Color.web("#b9b3a4"), CASE_DARK = Color.web("#6e6a60"), SCREEN_GLOW = Color.web("#9fd0ff"), LED = Color.web("#ff4a3c");
    private static final Color PAPER = Color.web("#f1e6cf"), PAPER_EDGE = Color.web("#c9b892"), THREAD = Color.web("#8a6a3e"), COVER = Color.web("#5a2e1e");

    /** Vrai pour un skin de ce visuel. */
    static boolean frames(Skin skin) {
        return skin != null && skin.visual() == idle.core.universe.SkinVisual.DIMENSION_FRAME;
    }

    /** Dessine le cadre du skin sur le calque, ou l'efface sans skin. */
    static void draw(Canvas canvas, Skin skin, double time) {
        double w = canvas.getWidth(), h = canvas.getHeight();
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setGlobalAlpha(1);
        g.clearRect(0, 0, w, h);
        if (!frames(skin) || w <= 2 * THICKNESS || h <= 2 * THICKNESS) return;
        double t = THICKNESS;
        switch (skin) {
            case FRAME_PORTHOLE -> {
                // L'anneau : un gros trait de métal, aux coins très arrondis, clair en haut, sombre en bas.
                g.setStroke(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, new Stop(0, METAL_LIGHT), new Stop(0.5, METAL), new Stop(1, METAL_DARK)));
                g.setLineWidth(t);
                g.strokeRoundRect(t / 2, t / 2, w - t, h - t, t * 2.5, t * 2.5);
                g.setStroke(RIVET);
                g.setLineWidth(1.5);
                g.strokeRoundRect(t, t, w - 2 * t, h - 2 * t, t * 1.8, t * 1.8);
                // Les rivets, tout autour.
                g.setFill(RIVET);
                double step = Math.max(40, Math.min(w, h) / 8), r = t * 0.18;
                for (double x = t * 1.5; x <= w - t * 1.5; x += step) {
                    g.fillOval(x - r, t / 2 - r, 2 * r, 2 * r);
                    g.fillOval(x - r, h - t / 2 - r, 2 * r, 2 * r);
                }
                for (double y = t * 1.5 + step; y <= h - t * 1.5 - step; y += step) {
                    g.fillOval(t / 2 - r, y - r, 2 * r, 2 * r);
                    g.fillOval(w - t / 2 - r, y - r, 2 * r, 2 * r);
                }
            }
            case FRAME_GILDED -> {
                g.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE, new Stop(0, GOLD_LIGHT), new Stop(0.5, GOLD), new Stop(1, GOLD_DARK)));
                g.fillRect(0, 0, w, t);
                g.fillRect(0, h - t, w, t);
                g.fillRect(0, 0, t, h);
                g.fillRect(w - t, 0, t, h);
                // Deux filets, un sombre à l'extérieur, un clair près de l'image.
                g.setStroke(GOLD_DARK);
                g.setLineWidth(2);
                g.strokeRect(3, 3, w - 6, h - 6);
                g.setStroke(GOLD_LIGHT);
                g.setLineWidth(1.5);
                g.strokeRect(t - 3, t - 3, w - 2 * t + 6, h - 2 * t + 6);
                // Les volutes des coins : trois petits arcs qui s'enroulent.
                g.setStroke(GOLD_DARK);
                g.setLineWidth(1.5);
                for (int corner = 0; corner < 4; corner++) {
                    double cx = corner % 2 == 0 ? t * 0.9 : w - t * 0.9, cy = corner < 2 ? t * 0.9 : h - t * 0.9;
                    for (int k = 1; k <= 3; k++) {
                        double r = t * 0.22 * k;
                        g.strokeOval(cx - r, cy - r, 2 * r, 2 * r);
                    }
                }
            }
            case FRAME_CRT -> {
                g.setFill(CASE);
                g.fillRoundRect(0, 0, w, h, t, t);
                g.setFill(CASE_DARK);
                g.fillRoundRect(t * 0.55, t * 0.55, w - t * 1.1, h - t * 1.1, t, t);
                // L'image reste visible : le boîtier ne couvre que le bord.
                g.clearRect(t, t, w - 2 * t, h - 2 * t);
                // Le verre bombé : une lueur dans chaque coin de l'image.
                for (int corner = 0; corner < 4; corner++) {
                    double cx = corner % 2 == 0 ? t * 1.6 : w - t * 1.6, cy = corner < 2 ? t * 1.6 : h - t * 1.6;
                    UniverseView.glow(g, cx, cy, t * 2.2, SCREEN_GLOW, 0.18);
                }
                // Les lignes de balayage, fines, qui défilent lentement sur l'image.
                g.setStroke(Color.BLACK);
                g.setLineWidth(1);
                g.setGlobalAlpha(0.12);
                double offset = (time * 12) % 4;
                for (double y = t + offset; y < h - t; y += 4) g.strokeLine(t, y, w - t, y);
                g.setGlobalAlpha(1);
                // La diode qui veille, en bas à droite.
                double pulse = 0.5 + 0.5 * Math.sin(time * 2);
                UniverseView.glow(g, w - t * 1.5, h - t / 2, t * 0.6, LED, 0.3 + 0.3 * pulse);
                g.setFill(LED);
                g.fillOval(w - t * 1.5 - 3, h - t / 2 - 3, 6, 6);
            }
            default -> {
                // Le livre ouvert : la couverture, les pages, leur tranche feuilletée, la couture au milieu.
                g.setFill(COVER);
                g.fillRoundRect(0, 0, w, h, 6, 6);
                g.setFill(PAPER);
                g.fillRect(t * 0.35, t * 0.35, w - t * 0.7, h - t * 0.7);
                g.setStroke(PAPER_EDGE);
                g.setLineWidth(1);
                for (int k = 1; k <= 4; k++) {
                    double d = t * 0.35 + k * 2.5;
                    g.strokeRect(d, d, w - 2 * d, h - 2 * d);
                }
                g.clearRect(t, t, w - 2 * t, h - 2 * t);
                g.setStroke(THREAD);
                g.setLineWidth(1.5);
                g.setLineDashes(4, 5);
                g.strokeLine(w / 2, 2, w / 2, t);
                g.strokeLine(w / 2, h - t, w / 2, h - 2);
                g.setLineDashes();
                // L'ombre de la pliure, sur l'image.
                g.setFill(new LinearGradient(0.46, 0, 0.54, 0, true, CycleMethod.NO_CYCLE, new Stop(0, Color.TRANSPARENT), new Stop(0.5, Color.color(0, 0, 0, 0.35)), new Stop(1, Color.TRANSPARENT)));
                g.fillRect(w * 0.46, t, w * 0.08, h - 2 * t);
            }
        }
        g.setGlobalAlpha(1);
    }
}
