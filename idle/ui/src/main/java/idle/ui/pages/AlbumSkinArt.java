package idle.ui.pages;

import idle.core.universe.Skin;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.FillRule;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.TextAlignment;

/**
 * Les dessins des cases d'album (version 0.198, {@link idle.core.universe.SkinVisual#ALBUM}). « Illustré » pose le
 * dessin nu de ce que la case garde, un « ? » éteint sur ce qui n'est pas trouvé. Les quatre autres l'habillent :
 * « Vignettes autocollantes », une pastille blanche aux bords épais, un peu penchée, avec son ombre (vide : le
 * contour en pointillé de la vignette à coller) ; « Timbres », un timbre crème dentelé, sa valeur en bas à droite et
 * un cachet (vide : une case de classeur grise, à peine encadrée) ; « Cartes de collection », un cadre coloré à bandeau,
 * des reflets en biais et des étoiles de rareté (vide : le dos de la carte, hachuré) ; « Herbier », une fiche de papier
 * ivoire tenue par deux bouts de ruban adhésif, lignée en bas (vide : une fiche sans rien, au ruban tout seul).
 */
final class AlbumSkinArt {

    private static final double WIDTH = 150, PLAIN_HEIGHT = 70, DRESSED_HEIGHT = 86;
    private static final Color DARK = Color.web("#0e1420"), MUTED = Color.web("#5a6a7e");
    private static final Color STICKER = Color.web("#fafafa"), STICKER_SHADOW = Color.rgb(0, 0, 0, 0.45);
    private static final Color PAPER = Color.web("#f4f1e8"), INK = Color.web("#3b4a7a"), BINDER = Color.web("#1a2230");
    private static final Color HOLO = Color.rgb(255, 255, 255, 0.16), BACK_RED = Color.web("#7a2a2a"), BACK_LINE = Color.web("#a84444");
    private static final Color IVORY = Color.web("#f6f0dc"), RULE = Color.web("#c9bfa4"), TAPE = Color.rgb(230, 210, 150, 0.55), SEPIA = Color.web("#6b5a3e");

    private AlbumSkinArt() {
    }

    /**
     * Le dessin d'une case dans ce skin, centré, qui laisse passer le clic à la carte.
     *
     * @param entry  la case ; son dessin peut manquer : la place reste alors vide dans le cadre
     * @param found  vrai si la case est trouvée : le dessin se montre ; sinon la case vide du skin
     * @param number le numéro de la case, écrit sur le timbre et la carte
     * @param skin   un skin des albums
     * @param accent la couleur de l'album, « #rrggbb »
     */
    static Node picture(AlbumPane.Entry entry, boolean found, int number, Skin skin, String accent) {
        double height = skin == Skin.ALBUM_ILLUSTRATED ? PLAIN_HEIGHT : DRESSED_HEIGHT;
        Canvas canvas = new Canvas(WIDTH, height);
        GraphicsContext g = canvas.getGraphicsContext2D();
        switch (skin) {
            case ALBUM_STICKERS -> sticker(g, entry, found, height);
            case ALBUM_STAMPS -> stamp(g, entry, found, number, height);
            case ALBUM_CARDS -> card(g, entry, found, number, height, Color.web(accent));
            case ALBUM_HERBARIUM -> herbarium(g, entry, found, height);
            default -> plain(g, entry, found, height);     // « Illustré » ; un skin d'un autre visuel ne peut pas arriver ici : {@code setLook} ne garde que les siens
        }
        HBox box = new HBox(canvas);
        box.setAlignment(Pos.CENTER);
        box.setMouseTransparent(true);
        return box;
    }

    /** « Illustré » : le dessin sur le fond de la carte ; une case pas trouvée porte un grand « ? » éteint. */
    private static void plain(GraphicsContext g, AlbumPane.Entry entry, boolean found, double height) {
        if (found) {
            draw(g, entry, 0, 0, WIDTH, height);
            return;
        }
        g.setFill(MUTED);
        g.setGlobalAlpha(0.6);
        g.setFont(Font.font("System", height * 0.6));
        g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(VPos.CENTER);
        g.fillText("?", WIDTH / 2, height / 2);
        g.setGlobalAlpha(1);
    }

    /** « Vignettes autocollantes » : une pastille blanche penchée, à bord épais, le dessin dedans ; vide, son contour pointillé. */
    private static void sticker(GraphicsContext g, AlbumPane.Entry entry, boolean found, double height) {
        double w = WIDTH * 0.74, h = height * 0.82, x = (WIDTH - w) / 2, y = (height - h) / 2;
        g.save();
        g.translate(WIDTH / 2, height / 2);
        g.rotate(-3);
        g.translate(-WIDTH / 2, -height / 2);
        if (!found) {
            g.setStroke(MUTED);
            g.setLineWidth(1.5);
            g.setLineDashes(5, 4);
            g.strokeRoundRect(x, y, w, h, 18, 18);
            g.restore();
            return;
        }
        g.setFill(STICKER_SHADOW);
        g.fillRoundRect(x + 3, y + 4, w, h, 18, 18);
        g.setFill(STICKER);
        g.fillRoundRect(x, y, w, h, 18, 18);
        double margin = 6;
        g.setFill(DARK);
        g.fillRoundRect(x + margin, y + margin, w - 2 * margin, h - 2 * margin, 12, 12);
        g.save();
        g.beginPath();
        g.rect(x + margin, y + margin, w - 2 * margin, h - 2 * margin);
        g.clip();
        draw(g, entry, x + margin, y + margin, w - 2 * margin, h - 2 * margin);
        g.restore();
        // Un coin qui se décolle, en bas à droite : un petit triangle plus gris.
        g.setFill(Color.web("#d8d8d8"));
        g.fillPolygon(new double[] {x + w, x + w - 14, x + w}, new double[] {y + h - 14, y + h, y + h}, 3);
        g.restore();
    }

    /** « Timbres » : un timbre crème aux bords dentelés, la valeur en bas à droite, un cachet d'encre ; vide, une case grise du classeur. */
    private static void stamp(GraphicsContext g, AlbumPane.Entry entry, boolean found, int number, double height) {
        double side = height - 6, x = (WIDTH - side * 1.25) / 2, y = 3, w = side * 1.25;
        if (!found) {
            g.setFill(BINDER);
            g.fillRect(x, y, w, side);
            g.setStroke(MUTED);
            g.setLineWidth(1);
            g.strokeRect(x + 0.5, y + 0.5, w - 1, side - 1);
            return;
        }
        int teethX = 10, teethY = 7;
        double pitchX = w / teethX, pitchY = side / teethY, bite = Math.min(pitchX, pitchY) * 0.3;
        g.save();
        g.setFillRule(FillRule.EVEN_ODD);
        g.beginPath();
        g.rect(x, y, w, side);
        for (int k = 0; k <= teethX; k++) {
            circle(g, x + k * pitchX, y, bite);
            circle(g, x + k * pitchX, y + side, bite);
        }
        for (int k = 0; k <= teethY; k++) {
            circle(g, x, y + k * pitchY, bite);
            circle(g, x + w, y + k * pitchY, bite);
        }
        g.clip();
        g.setFill(PAPER);
        g.fillRect(x, y, w, side);
        double margin = 8;
        g.setFill(DARK);
        g.fillRect(x + margin, y + margin, w - 2 * margin, side - 2 * margin);
        g.save();
        g.beginPath();
        g.rect(x + margin, y + margin, w - 2 * margin, side - 2 * margin);
        g.clip();
        draw(g, entry, x + margin, y + margin, w - 2 * margin, side - 2 * margin);
        g.restore();
        g.setStroke(INK);
        g.setLineWidth(1);
        g.strokeRect(x + margin, y + margin, w - 2 * margin, side - 2 * margin);
        g.setFill(INK);
        g.setFont(Font.font("serif", 8));
        g.setTextAlign(TextAlignment.RIGHT);
        g.setTextBaseline(VPos.BOTTOM);
        g.fillText(Integer.toString(number), x + w - 2, y + side - 1);
        // Le cachet, à cheval sur le coin haut droit.
        g.setGlobalAlpha(0.5);
        g.setLineWidth(1.2);
        double r = side * 0.22, cx = x + w - r * 0.7, cy = y + r * 0.9;
        g.strokeArc(cx - r, cy - r, 2 * r, 2 * r, 40, 220, ArcType.OPEN);
        g.strokeArc(cx - r * 0.75, cy - r * 0.75, 1.5 * r, 1.5 * r, 60, 170, ArcType.OPEN);
        g.setGlobalAlpha(1);
        g.restore();
    }

    /** « Cartes de collection » : un cadre de la couleur de l'album, un bandeau, des reflets en biais, des étoiles ; vide, le dos hachuré. */
    private static void card(GraphicsContext g, AlbumPane.Entry entry, boolean found, int number, double height, Color accent) {
        double w = height * 0.78, x = (WIDTH - w) / 2, y = 2, h = height - 4;
        if (!found) {
            g.setFill(BACK_RED);
            g.fillRoundRect(x, y, w, h, 6, 6);
            g.save();
            g.beginPath();
            g.rect(x + 4, y + 4, w - 8, h - 8);
            g.clip();
            g.setStroke(BACK_LINE);
            g.setLineWidth(1);
            for (double d = -h; d < w + h; d += 6) {
                g.strokeLine(x + d, y, x + d + h, y + h);
                g.strokeLine(x + d + h, y, x + d, y + h);
            }
            g.restore();
            g.setStroke(Color.web("#f0d8a0"));
            g.setLineWidth(1.5);
            g.strokeRoundRect(x + 3, y + 3, w - 6, h - 6, 4, 4);
            return;
        }
        g.setFill(accent);
        g.fillRoundRect(x, y, w, h, 6, 6);
        double band = 12, margin = 4;
        g.setFill(DARK);
        g.fillRect(x + margin, y + band, w - 2 * margin, h - band - margin - 10);
        g.save();
        g.beginPath();
        g.rect(x + margin, y + band, w - 2 * margin, h - band - margin - 10);
        g.clip();
        draw(g, entry, x + margin, y + band, w - 2 * margin, h - band - margin - 10);
        // Les reflets holographiques, deux bandes en biais.
        g.setFill(HOLO);
        g.fillPolygon(new double[] {x + w * 0.1, x + w * 0.3, x + w * 0.7, x + w * 0.5}, new double[] {y + h, y + h, y, y}, 4);
        g.fillPolygon(new double[] {x + w * 0.75, x + w * 0.85, x + w * 1.25, x + w * 1.15}, new double[] {y + h, y + h, y, y}, 4);
        g.restore();
        g.setFill(DARK);
        g.setFont(Font.font("System", javafx.scene.text.FontWeight.BOLD, 8));
        g.setTextAlign(TextAlignment.LEFT);
        g.setTextBaseline(VPos.CENTER);
        g.fillText("N° " + number, x + margin, y + band / 2);
        // Les étoiles de rareté, en bas : une à trois selon le rang dans l'album.
        int stars = 1 + (number % 3);
        g.setTextAlign(TextAlignment.RIGHT);
        g.fillText("★".repeat(stars), x + w - margin, y + h - 6);
    }

    /** « Herbier » : une fiche ivoire tenue par deux bouts de ruban, le dessin pressé dessus, des lignes en bas ; vide, la fiche nue. */
    private static void herbarium(GraphicsContext g, AlbumPane.Entry entry, boolean found, double height) {
        double w = WIDTH * 0.7, x = (WIDTH - w) / 2, y = 5, h = height - 8;
        g.setFill(Color.rgb(0, 0, 0, 0.35));
        g.fillRect(x + 2, y + 2, w, h);
        g.setFill(IVORY);
        g.fillRect(x, y, w, h);
        g.setStroke(RULE);
        g.setLineWidth(1);
        for (int k = 0; k < 3; k++) g.strokeLine(x + 8, y + h - 6 - 7 * k + 0.5, x + w - 8, y + h - 6 - 7 * k + 0.5);
        if (found) {
            double pictureH = h - 28;
            g.save();
            g.beginPath();
            g.rect(x + 6, y + 6, w - 12, pictureH);
            g.clip();
            draw(g, entry, x + 6, y + 6, w - 12, pictureH);
            g.restore();
            // L'étiquette, en écriture penchée sur la première ligne.
            g.setFill(SEPIA);
            g.setFont(Font.font("serif", FontPosture.ITALIC, 8));
            g.setTextAlign(TextAlignment.LEFT);
            g.setTextBaseline(VPos.BOTTOM);
            g.fillText(shorten(entry.name(), 20), x + 9, y + h - 20);
        } else {
            g.setStroke(RULE);
            g.setLineDashes(3, 3);
            g.strokeRect(x + 10, y + 8, w - 20, h - 36);
            g.setLineDashes();
        }
        // Les deux bouts de ruban adhésif, aux coins hauts, un peu de travers.
        g.setFill(TAPE);
        g.save();
        g.translate(x + 6, y + 2);
        g.rotate(-35);
        g.fillRect(-10, -3, 22, 7);
        g.restore();
        g.save();
        g.translate(x + w - 6, y + 2);
        g.rotate(35);
        g.fillRect(-12, -3, 22, 7);
        g.restore();
    }

    /** Dessine l'entrée dans ce cadre, si elle a un dessin ; le fond est déjà posé. */
    private static void draw(GraphicsContext g, AlbumPane.Entry entry, double x, double y, double width, double height) {
        if (entry.picture() == null) return;
        g.save();
        g.translate(x, y);
        entry.picture().draw(g, width, height);
        g.restore();
        g.setGlobalAlpha(1);
    }

    private static String shorten(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }

    private static void circle(GraphicsContext g, double x, double y, double r) {
        g.arc(x, y, r, r, 0, 360);
        g.closePath();
    }
}
