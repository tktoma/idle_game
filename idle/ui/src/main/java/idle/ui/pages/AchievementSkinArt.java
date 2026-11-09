package idle.ui.pages;

import idle.core.universe.Skin;
import idle.core.universe.SkinVisual;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

/**
 * Les insignes des tuiles de succès (version 0.198) : un petit dessin en tête de la tuile, à gauche de son numéro. Un
 * succès obtenu prend celui du skin des tuiles ({@link SkinVisual#ACHIEVEMENT_TILE}) : une médaille de bronze, d'argent
 * ou d'or selon son palier, un badge de scout brodé au point de croix, une étoile, un trophée, un timbre ; un succès pas
 * encore obtenu celui du skin des succès non obtenus ({@link SkinVisual#ACHIEVEMENT_LOCKED}) : une silhouette grise de
 * médaille, un point d'interrogation dans son rond, une pièce de puzzle qui manque, en pointillé.
 */
final class AchievementSkinArt {

    /** Le côté de l'insigne, en pixels : la hauteur d'une ligne d'en-tête. */
    static final double SIDE = 20;
    private static final Color[] METALS = {Color.web("#cd7f32"), Color.web("#c8ccd4"), Color.web("#ffd700")};
    private static final Color[] METAL_SHADES = {Color.web("#7a4a1a"), Color.web("#6e7480"), Color.web("#9a7400")};
    private static final Color RIBBON = Color.web("#c0392b"), RIBBON_2 = Color.web("#2a5db0");
    private static final Color CLOTH = Color.web("#2f4a3a"), THREAD = Color.web("#f0e6c8");
    private static final Color PAPER = Color.web("#f4f1e8"), INK = Color.web("#3b4a7a");
    private static final Color GREY = Color.web("#3a4452"), GREY_LIGHT = Color.web("#6a7686");

    private AchievementSkinArt() {
    }

    /**
     * L'insigne d'une tuile, ou {@code null} quand aucun skin ne la concerne (le Classique n'a pas d'insigne).
     *
     * @param tiles  le skin des tuiles, pour un succès obtenu ; {@code null} pour le Classique
     * @param locked le skin des succès non obtenus ; {@code null} pour le Classique
     * @param owned  vrai si le succès est obtenu
     * @param tier   0, 1 ou 2 : bronze, argent, or ; le palier du succès dans sa liste
     * @param accent la couleur de la tuile, « #rrggbb »
     */
    static Node icon(Skin tiles, Skin locked, boolean owned, int tier, String accent) {
        Skin skin = owned ? tiles : locked;
        if (skin == null || skin.visual() != (owned ? SkinVisual.ACHIEVEMENT_TILE : SkinVisual.ACHIEVEMENT_LOCKED)) return null;
        Canvas canvas = new Canvas(SIDE, SIDE);
        GraphicsContext g = canvas.getGraphicsContext2D();
        Color color = Color.web(accent);
        int metal = Math.max(0, Math.min(2, tier));
        switch (skin) {
            case TILES_MEDALS -> medal(g, METALS[metal], METAL_SHADES[metal]);
            case TILES_BADGES -> badge(g, color);
            case TILES_STARS -> star(g, METALS[metal]);
            case TILES_TROPHIES -> trophy(g, METALS[metal], METAL_SHADES[metal]);
            case TILES_STAMPS -> stamp(g, color);
            case LOCKED_SILHOUETTE -> silhouette(g);
            case LOCKED_QUESTION -> question(g);
            case LOCKED_PUZZLE -> puzzle(g);
            default -> { }     // un skin d'un autre visuel ne peut pas arriver ici : les deux visuels sont vérifiés plus haut
        }
        canvas.setMouseTransparent(true);
        return canvas;
    }

    /** Le palier d'un succès dans sa liste, 0 à 2 : les tiers de la liste, dans l'ordre où on les obtient. */
    static int tier(int rank, int total) {
        if (total <= 0) return 0;
        return Math.min(2, (rank - 1) * 3 / total);
    }

    /** La médaille : deux pans de ruban, le disque de métal cerné d'ombre, une étoile gravée. */
    private static void medal(GraphicsContext g, Color metal, Color shade) {
        g.setFill(RIBBON);
        g.fillPolygon(new double[] {6, 10, 8, 3}, new double[] {0, 0, 9, 9}, 4);
        g.setFill(RIBBON_2);
        g.fillPolygon(new double[] {10, 14, 17, 12}, new double[] {0, 0, 9, 9}, 4);
        g.setFill(shade);
        g.fillOval(3.5, 7.5, 13, 13);
        g.setFill(metal);
        g.fillOval(4, 7, 12, 12);
        g.setFill(shade);
        starShape(g, 10, 13, 3.6, 1.6);
    }

    /** Le badge brodé : un écusson de drap sombre, bordé d'un fil clair au point de croix, la couleur de la tuile au centre. */
    private static void badge(GraphicsContext g, Color color) {
        double[] xs = {2, 18, 18, 10, 2}, ys = {2, 2, 11, 19, 11};
        g.setFill(CLOTH);
        g.fillPolygon(xs, ys, 5);
        g.setStroke(THREAD);
        g.setLineWidth(1.4);
        g.setLineDashes(1.6, 1.6);
        g.strokePolygon(xs, ys, 5);
        g.setLineDashes();
        g.setFill(color);
        g.fillOval(6.5, 5.5, 7, 7);
        g.setStroke(color.darker());
        g.setLineWidth(1);
        g.setLineDashes(1.2, 1.2);
        g.strokeOval(6.5, 5.5, 7, 7);
        g.setLineDashes();
    }

    /** L'étoile à cinq branches, de métal, un reflet blanc sur la branche haute. */
    private static void star(GraphicsContext g, Color metal) {
        g.setFill(metal);
        starShape(g, 10, 10.5, 9, 4);
        g.setFill(Color.rgb(255, 255, 255, 0.55));
        g.fillOval(8, 4, 3, 3);
    }

    /** Le trophée : la coupe et ses deux anses, le pied, le socle sombre. */
    private static void trophy(GraphicsContext g, Color metal, Color shade) {
        g.setFill(metal);
        g.fillArc(5, 1, 10, 12, 180, 180, ArcType.ROUND);
        g.fillRect(5, 1, 10, 3);
        g.setStroke(metal);
        g.setLineWidth(1.5);
        g.strokeArc(1.5, 3, 5, 6, 90, 180, ArcType.OPEN);
        g.strokeArc(13.5, 3, 5, 6, -90, 180, ArcType.OPEN);
        g.setFill(shade);
        g.fillRect(9, 12, 2, 4);
        g.fillRoundRect(5, 16, 10, 3.5, 1.5, 1.5);
        g.setFill(Color.rgb(255, 255, 255, 0.5));
        g.fillOval(7, 3, 2, 4);
    }

    /** Le timbre : un carré crème aux bords dentelés, le centre de la couleur de la tuile sous un filet d'encre. */
    private static void stamp(GraphicsContext g, Color color) {
        g.setFill(PAPER);
        g.fillRect(2, 2, 16, 16);
        // Les dents : des encoches aux quatre bords, du fond de la tuile ; ici, des ronds effacés.
        g.setFill(Color.web("#10151f"));
        for (int k = 0; k <= 4; k++) {
            double at = 2 + k * 4;
            g.fillOval(at - 1.2, 0.8, 2.4, 2.4);
            g.fillOval(at - 1.2, 16.8, 2.4, 2.4);
            g.fillOval(0.8, at - 1.2, 2.4, 2.4);
            g.fillOval(16.8, at - 1.2, 2.4, 2.4);
        }
        g.setFill(color);
        g.fillRect(5, 5, 10, 10);
        g.setStroke(INK);
        g.setLineWidth(1);
        g.strokeRect(5.5, 5.5, 9, 9);
    }

    /** La silhouette grise : la médaille et son ruban d'un seul gris, sans relief. */
    private static void silhouette(GraphicsContext g) {
        g.setFill(GREY);
        g.fillPolygon(new double[] {6, 14, 12, 8}, new double[] {0, 0, 9, 9}, 4);
        g.fillOval(4, 7, 12, 12);
    }

    /** Le point d'interrogation dans son rond gris. */
    private static void question(GraphicsContext g) {
        g.setStroke(GREY_LIGHT);
        g.setLineWidth(1.5);
        g.strokeOval(2, 2, 16, 16);
        g.setFill(GREY_LIGHT);
        g.setFont(Font.font("System", FontWeight.BOLD, 13));
        g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(VPos.CENTER);
        g.fillText("?", 10, 10.5);
    }

    /** La pièce de puzzle qui manque : son contour en pointillé, avec une bosse en haut et un creux à droite. */
    private static void puzzle(GraphicsContext g) {
        g.setStroke(GREY_LIGHT);
        g.setLineWidth(1.3);
        g.setLineDashes(2.5, 2);
        g.beginPath();
        g.moveTo(3, 5);
        g.lineTo(7.5, 5);
        g.arc(10, 4, 2.5, 2.5, 200, -220);
        g.lineTo(17, 5);
        g.lineTo(17, 9);
        g.arc(16.5, 12, 2.5, 2.5, 90, -180);
        g.lineTo(17, 18);
        g.lineTo(3, 18);
        g.closePath();
        g.stroke();
        g.setLineDashes();
    }

    /** Une étoile à cinq branches, pleine, centrée en ({@code cx}, {@code cy}). */
    private static void starShape(GraphicsContext g, double cx, double cy, double outer, double inner) {
        double[] xs = new double[10], ys = new double[10];
        for (int k = 0; k < 10; k++) {
            double a = -Math.PI / 2 + k * Math.PI / 5;
            double r = k % 2 == 0 ? outer : inner;
            xs[k] = cx + r * Math.cos(a);
            ys[k] = cy + r * Math.sin(a);
        }
        g.fillPolygon(xs, ys, 10);
    }
}
