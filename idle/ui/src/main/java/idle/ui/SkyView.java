package idle.ui;

import idle.core.Body;
import idle.core.Game;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

/**
 * Le ciel : les astres que le joueur a formés ({@link Game#formBody(String)}), côte à côte, du plus
 * petit au plus grand, comme sur une planche qui compare leurs tailles.
 *
 * <p>Chaque échelle a son rayon ({@link #radius(Body)}) : un amas de roches est petit, une planète
 * grande, une géante plus grande encore. Ces tailles ne sont pas les vraies, qui ne tiendraient
 * dans aucune fenêtre ; elles gardent seulement l'ordre. Quand les astres ne tiennent plus sur une
 * ligne, ils passent à la ligne suivante ; quand ils ne tiennent plus dans le ciel, tous
 * rapetissent ensemble, et tant que le ciel est presque vide ils sont agrandis. Un petit astre garde autour de lui la place de son nom, pour que deux noms
 * ne se chevauchent pas.
 *
 * <p>Chaque astre porte son nom et flotte à peine, à son rythme. Les étoiles du fond scintillent.
 */
final class SkyView extends Canvas {

    private static final Color VOID = Color.web("#05060a");
    private static final Color STAR = Color.web("#c9d6ea");
    private static final Color TEXT = Color.web("#8fa3b8");
    private static final int STARS = 70;
    /** Écart entre deux astres voisins, en pixels, avant réduction. */
    private static final double GAP = 22;
    /** Hauteur réservée au nom d'un astre, sous lui. */
    private static final double NAME = 16;
    /** En dessous de cette réduction, les noms ne sont plus écrits : ils prendraient plus de place que les astres. */
    private static final double NAMES_FROM = 0.55;
    /** L'agrandissement le plus fort, quand le ciel est presque vide : un amas de roches seul reste lisible. */
    private static final double LARGEST = 2.4;
    /** Largeur d'une lettre d'un nom, en pixels, à peu près : de quoi réserver sa place sous l'astre. */
    private static final double LETTER = 5.8;

    private final Game game;
    private double time = 0;
    /** L'échelle de la dernière image : {@link #LARGEST} quand le ciel est presque vide, moins de 1 quand il est plein. */
    private double shrink = LARGEST;
    /** Nombre d'astres dessinés à la dernière image, et de lignes qu'ils occupaient. */
    private int drawn = 0;
    private int rows = 0;
    /**
     * Une image sur {@link #EVERY} seulement est dessinée tant que rien ne change : les astres flottent
     * de moins d'un pixel par seconde. Un astre de plus, ou un ciel qui change de taille, et l'image
     * suivante est dessinée sans attendre.
     */
    private static final int EVERY = 2;
    private int beat = 0;
    private double drawnWidth = -1;
    private double drawnHeight = -1;

    SkyView(Game game) {
        super(600, 330);
        this.game = game;
    }

    /** Le rayon d'un astre dans le ciel, en pixels, à l'échelle 1 : il grandit avec son échelle. */
    static double radius(Body body) {
        return switch (body.tier()) {
            case RUBBLE -> 9;
            case COMET -> 8;
            case ASTEROID -> 15;
            case MOON -> 24;
            case PLANET -> body.look() == Body.Look.BANDED || body.look() == Body.Look.RINGED ? 54 : 40;
            // Une naine brune dépasse à peine une géante gazeuse ; une géante, rouge ou bleue, écrase le Soleil.
            case STAR -> body.id().equals("brown_dwarf") ? 30 : body.id().endsWith("_giant") ? 48 : body.id().equals("red_dwarf") ? 34 : 40;
            case REMNANT -> 12;
            case BLACK_HOLE -> 15;
            case CORE -> 21;
        };
    }

    /** Nombre d'astres dessinés à la dernière image : pour les vérifications. */
    int drawn() {
        return drawn;
    }

    /** Nombre de lignes occupées à la dernière image : pour les vérifications. */
    int rows() {
        return rows;
    }

    /** L'échelle de la dernière image, de {@link #LARGEST} (ciel presque vide) à moins de 1 (ciel plein) : pour les vérifications. */
    double shrink() {
        return shrink;
    }

    /**
     * Fait avancer le temps du ciel et le redessine.
     *
     * @param elapsed secondes écoulées depuis l'image précédente
     */
    void frame(double elapsed) {
        time += elapsed;
        beat++;
        boolean changed = getWidth() != drawnWidth || getHeight() != drawnHeight || game.bodiesFormed() != drawn;
        if (!changed && beat % EVERY != 0) return;
        redraw();
    }

    /** Dessine le ciel tel qu'il est à cet instant. */
    private void redraw() {
        double w = getWidth();
        double h = getHeight();
        if (w <= 0 || h <= 0) return;
        drawnWidth = w;
        drawnHeight = h;
        GraphicsContext g = getGraphicsContext2D();
        g.clearRect(0, 0, w, h);
        g.setGlobalAlpha(1);
        g.setFill(VOID);
        g.fillRect(0, 0, w, h);
        drawStars(g, w, h);

        List<Body> formed = new ArrayList<>();
        for (Body body : game.bodies()) {
            if (game.hasBody(body.id())) formed.add(body);
        }
        drawn = formed.size();
        if (formed.isEmpty()) {
            rows = 0;
            shrink = LARGEST;
            g.setFill(TEXT);
            g.setFont(Font.font(12));
            g.setTextAlign(TextAlignment.CENTER);
            g.fillText("Le ciel est vide : formez un premier amas de roches.", w / 2, h / 2);
            g.setTextAlign(TextAlignment.LEFT);
            return;
        }

        // La plus grande taille à laquelle tous les astres tiennent, ligne après ligne : agrandis quand ils sont
        // peu nombreux, réduits quand le ciel se remplit.
        shrink = LARGEST;
        List<int[]> lines = layout(formed, w, shrink);
        while (height(formed, lines, shrink) > h - 8 && shrink > 0.2) {
            shrink *= 0.92;
            lines = layout(formed, w, shrink);
        }
        rows = lines.size();
        boolean named = shrink >= NAMES_FROM;
        double top = (h - height(formed, lines, shrink)) / 2;
        g.setFont(Font.font(11));
        g.setTextAlign(TextAlignment.CENTER);
        for (int[] line : lines) {
            double up = 0;
            double down = 0;
            double wide = -GAP * shrink;
            for (int index = line[0]; index < line[1]; index++) {
                Body body = formed.get(index);
                double r = radius(body) * shrink;
                up = Math.max(up, BodyArt.reachUp(body) * r);
                down = Math.max(down, BodyArt.reachDown(body) * r);
                wide += cell(body, shrink) + GAP * shrink;
            }
            double x = (w - wide) / 2;
            double middle = top + up;
            for (int index = line[0]; index < line[1]; index++) {
                Body body = formed.get(index);
                double r = radius(body) * shrink;
                double place = cell(body, shrink);
                // L'astre est au milieu de sa place, queue ou anneaux compris ; son nom, au milieu aussi.
                double cx = x + (place - (BodyArt.reachLeft(body) + BodyArt.reachRight(body)) * r) / 2 + BodyArt.reachLeft(body) * r;
                // Chaque astre flotte à peine, à son rythme.
                double cy = middle + 1.5 * Math.sin(time * 0.6 + index * 1.9);
                BodyArt.draw(g, body, cx, cy, r);
                if (named) {
                    g.setFill(TEXT);
                    g.fillText(body.name(), x + place / 2, middle + down + 12);
                }
                x += place + GAP * shrink;
            }
            top += up + down + (named ? NAME : 4) + 6 * shrink;
        }
        g.setTextAlign(TextAlignment.LEFT);
    }

    /** Range les astres en lignes qui tiennent dans la largeur : pour chaque ligne, le premier astre et celui d'après le dernier. */
    private static List<int[]> layout(List<Body> formed, double width, double shrink) {
        List<int[]> lines = new ArrayList<>();
        int start = 0;
        double used = 0;
        for (int index = 0; index < formed.size(); index++) {
            Body body = formed.get(index);
            double wide = cell(body, shrink);
            if (index > start && used + GAP * shrink + wide > width - 16) {
                lines.add(new int[] {start, index});
                start = index;
                used = 0;
            }
            used += (index > start ? GAP * shrink : 0) + wide;
        }
        lines.add(new int[] {start, formed.size()});
        return lines;
    }

    /** La largeur de la place d'un astre : la sienne, ou celle de son nom s'il est écrit et plus large que lui. */
    private static double cell(Body body, double shrink) {
        double wide = (BodyArt.reachLeft(body) + BodyArt.reachRight(body)) * radius(body) * shrink;
        return shrink >= NAMES_FROM ? Math.max(wide, LETTER * body.name().length()) : wide;
    }

    /** La hauteur que prennent ces lignes, noms compris. */
    private static double height(List<Body> formed, List<int[]> lines, double shrink) {
        double total = 0;
        for (int[] line : lines) {
            double up = 0;
            double down = 0;
            for (int index = line[0]; index < line[1]; index++) {
                Body body = formed.get(index);
                up = Math.max(up, BodyArt.reachUp(body) * radius(body) * shrink);
                down = Math.max(down, BodyArt.reachDown(body) * radius(body) * shrink);
            }
            total += up + down + (shrink >= NAMES_FROM ? NAME : 4) + 6 * shrink;
        }
        return total;
    }

    /** Un fond d'étoiles fixes, les mêmes d'une image à l'autre, qui scintillent à peine. */
    private void drawStars(GraphicsContext g, double w, double h) {
        g.setFill(STAR);
        for (int star = 0; star < STARS; star++) {
            double x = fraction(star * 12.9898) * w;
            double y = fraction(star * 78.233) * h;
            double twinkle = 0.25 + 0.2 * Math.sin(time * (0.4 + fraction(star * 3.7)) + star);
            g.setGlobalAlpha(Math.max(0.05, Math.min(1, twinkle)));
            double size = star % 9 == 0 ? 1.6 : 1;
            g.fillOval(x - size / 2, y - size / 2, size, size);
        }
        g.setGlobalAlpha(1);
    }

    private static double fraction(double seed) {
        double value = Math.sin(seed) * 43758.5453;
        return value - Math.floor(value);
    }
}
