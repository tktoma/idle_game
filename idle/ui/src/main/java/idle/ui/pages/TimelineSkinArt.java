package idle.ui.pages;

import idle.core.GameStats;
import idle.core.universe.Skin;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;

/**
 * La frise des univers dans ses skins (version 0.198, {@link idle.core.universe.SkinVisual#TIMELINE}). Chaque univers
 * formé garde sa ligne : son titre, puis sa barre en cinq phases à l'échelle du plus long. « Musée » : une ligne de
 * temps verticale en laiton à gauche, un cartel de papier crème pour le titre, la barre dans un cadre doré sur velours
 * sombre. « Pellicule » : la barre devient un bout de film noir, perforé en haut et en bas, chaque phase un photogramme ;
 * le titre en marquage de bord, jaune sur noir. « Train » : une voie à traverses, un wagon par univers dont la caisse se
 * partage entre les phases, roues et attelage ; une locomotive tire le premier.
 */
final class TimelineSkinArt {

    /** La hauteur de la barre dessinée, selon le skin. */
    private static final double FILM_HEIGHT = 30, TRAIN_HEIGHT = 34, MUSEUM_HEIGHT = 18;
    private static final Color BRASS = Color.web("#c9a24a"), VELVET = Color.web("#2a2420"), PLAQUE = Color.web("#f4f1e8");
    private static final Color FILM = Color.web("#0a0a0c"), SPROCKET = Color.web("#e8e4d8");
    private static final Color RAIL = Color.web("#8a8f99"), SLEEPER = Color.web("#5a4634"), WHEEL = Color.web("#20242c"), LOCO = Color.web("#3a4a5e");

    private TimelineSkinArt() {
    }

    /**
     * La ligne d'un univers dans ce skin.
     *
     * @param r       l'univers et ses phases
     * @param title   « Univers 3 — 2 h 10 min »
     * @param longest la durée du plus long univers montré : l'échelle
     * @param first   vrai pour la première ligne : la locomotive du train
     */
    static VBox row(Skin skin, GameStats.UniverseRecord r, String title, double longest, boolean first) {
        double[] p = r.phases();
        double scale = longest > 0 ? TimelinePane.BAR_WIDTH / longest : 0;
        Label name = new Label(title);
        name.setWrapText(true);
        name.setMaxWidth(TimelinePane.WIDTH);
        Tooltip.install(name, new Tooltip(TimelinePane.phases(r)));
        return switch (skin) {
            case TIMELINE_FILM -> {
                name.setStyle("-fx-font-size: 12px; -fx-font-family: monospace; -fx-text-fill: #e0c060; -fx-background-color: #0a0a0c; -fx-padding: 1 6;");
                yield new VBox(0, name, film(p, scale));
            }
            case TIMELINE_TRAIN -> {
                name.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #10151f; -fx-background-color: #e8f4ff;"
                        + " -fx-background-radius: 3; -fx-padding: 1 8; -fx-border-color: #3a4a5e; -fx-border-radius: 3;");
                yield new VBox(2, name, train(p, scale, first));
            }
            default -> {     // « Musée » ; un skin d'un autre visuel ne peut pas arriver ici : {@code setSkin} ne garde que les siens
                name.setStyle("-fx-font-size: 12px; -fx-font-family: serif; -fx-font-style: italic; -fx-text-fill: #3a2f20;"
                        + " -fx-background-color: #f4f1e8; -fx-padding: 2 8; -fx-border-color: #c9a24a; -fx-border-width: 1.5;");
                Region line = new Region();
                line.setMinSize(3, 10);
                line.setPrefWidth(3);
                line.setMaxWidth(3);
                line.setStyle("-fx-background-color: #c9a24a;");
                Region bead = new Region();
                bead.setMinSize(11, 11);
                bead.setMaxSize(11, 11);
                bead.setStyle("-fx-background-color: #c9a24a; -fx-background-radius: 6;");
                VBox rail = new VBox(0, bead, line);
                rail.setAlignment(Pos.TOP_CENTER);
                VBox.setVgrow(line, javafx.scene.layout.Priority.ALWAYS);
                VBox exhibit = new VBox(4, name, museum(p, scale));
                exhibit.setAlignment(Pos.TOP_LEFT);
                HBox box = new HBox(10, rail, exhibit);
                box.setAlignment(Pos.TOP_LEFT);
                yield new VBox(box);
            }
        };
    }

    /** La barre du musée : les phases dans un cadre de laiton sur du velours, un filet clair en haut du cadre. */
    private static Canvas museum(double[] p, double scale) {
        double width = total(p, scale) + 8;
        Canvas canvas = new Canvas(Math.max(width, 12), MUSEUM_HEIGHT);
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setFill(BRASS);
        g.fillRect(0, 0, width, MUSEUM_HEIGHT);
        g.setFill(VELVET);
        g.fillRect(2, 2, width - 4, MUSEUM_HEIGHT - 4);
        double x = 4;
        for (int i = 0; i < p.length; i++) {
            double w = piece(p[i], scale);
            if (w <= 0) continue;
            g.setFill(Color.web(TimelinePane.PHASE_COLORS.get(i)));
            g.fillRect(x, 4, w, MUSEUM_HEIGHT - 8);
            x += w;
        }
        g.setStroke(PLAQUE);
        g.setLineWidth(1);
        g.strokeLine(1, 1.5, width - 1, 1.5);
        return canvas;
    }

    /** Le bout de film : fond noir, perforations en haut et en bas, un photogramme par phase, séparés d'un interstice. */
    private static Canvas film(double[] p, double scale) {
        double width = total(p, scale) + 12;
        Canvas canvas = new Canvas(Math.max(width, 16), FILM_HEIGHT);
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setFill(FILM);
        g.fillRect(0, 0, width, FILM_HEIGHT);
        g.setFill(SPROCKET);
        for (double x = 3; x + 5 <= width - 2; x += 9) {
            g.fillRoundRect(x, 2, 5, 3.5, 1, 1);
            g.fillRoundRect(x, FILM_HEIGHT - 5.5, 5, 3.5, 1, 1);
        }
        double x = 6;
        for (int i = 0; i < p.length; i++) {
            double w = piece(p[i], scale);
            if (w <= 0) continue;
            g.setFill(Color.web(TimelinePane.PHASE_COLORS.get(i)));
            g.fillRect(x + 0.5, 8, Math.max(0.5, w - 1), FILM_HEIGHT - 16);
            // Un reflet de pellicule sur le haut du photogramme.
            g.setFill(Color.rgb(255, 255, 255, 0.18));
            g.fillRect(x + 0.5, 8, Math.max(0.5, w - 1), 3);
            x += w;
        }
        return canvas;
    }

    /** Le wagon : la voie et ses traverses, la caisse partagée entre les phases, le toit, les roues, l'attelage ; la locomotive devant le premier. */
    private static Canvas train(double[] p, double scale, boolean first) {
        double loco = first ? 30 : 0;
        double body = total(p, scale);
        double width = loco + body + 16;
        Canvas canvas = new Canvas(Math.max(width, 20), TRAIN_HEIGHT);
        GraphicsContext g = canvas.getGraphicsContext2D();
        // La voie, sur toute la largeur de la frise.
        double railY = TRAIN_HEIGHT - 4;
        g.setFill(SLEEPER);
        for (double x = 2; x < TimelinePane.WIDTH; x += 14) g.fillRect(x, railY - 3, 7, 6);
        g.setStroke(RAIL);
        g.setLineWidth(2);
        g.strokeLine(0, railY - 1, TimelinePane.WIDTH, railY - 1);
        g.strokeLine(0, railY + 2, TimelinePane.WIDTH, railY + 2);
        if (first) {
            g.setFill(LOCO);
            g.fillRoundRect(2, 10, 18, 14, 3, 3);
            g.fillRect(14, 4, 12, 20);
            g.fillRect(4, 4, 5, 8);     // la cheminée
            g.setFill(Color.web("#e8f4ff"));
            g.fillRect(17, 8, 6, 5);     // la vitre
            g.setFill(Color.rgb(232, 244, 255, 0.35));
            g.fillOval(1, -1, 7, 6);     // la fumée
            wheels(g, 4, 24, 3);
            g.setStroke(WHEEL);
            g.setLineWidth(2);
            g.strokeLine(26, 18, 30, 18);     // l'attelage
        }
        double x = loco + 4;
        double top = 8, height = TRAIN_HEIGHT - 16;
        for (int i = 0; i < p.length; i++) {
            double w = piece(p[i], scale);
            if (w <= 0) continue;
            g.setFill(Color.web(TimelinePane.PHASE_COLORS.get(i)));
            g.fillRect(x, top, w, height);
            x += w;
        }
        if (body > 0) {
            g.setStroke(WHEEL);
            g.setLineWidth(1.5);
            g.strokeRect(loco + 4, top, body, height);
            g.setFill(WHEEL);
            g.fillRect(loco + 3, top - 3, body + 2, 3);     // le toit
            int count = Math.max(2, (int) (body / 60) + 1);
            for (int k = 0; k < count; k++) {
                double wx = loco + 8 + (body - 8) * k / Math.max(1, count - 1);
                g.setFill(WHEEL);
                g.fillOval(wx - 3.5, TRAIN_HEIGHT - 11, 7, 7);
                g.setStroke(RAIL);
                g.setLineWidth(1);
                g.strokeArc(wx - 2, TRAIN_HEIGHT - 9.5, 4, 4, 0, 360, ArcType.OPEN);
            }
            g.setStroke(WHEEL);
            g.setLineWidth(2);
            g.strokeLine(loco + body + 4, top + height / 2, loco + body + 10, top + height / 2);
        }
        return canvas;
    }

    private static void wheels(GraphicsContext g, double x, double y, int count) {
        g.setFill(WHEEL);
        for (int k = 0; k < count; k++) g.fillOval(x + k * 8 - 3.5, y, 7, 7);
    }

    /** La largeur d'une phase à l'échelle, 1 px au moins quand elle a duré, comme la barre d'origine. */
    private static double piece(double seconds, double scale) {
        return seconds > 0 && scale > 0 ? Math.max(1, seconds * scale) : 0;
    }

    private static double total(double[] p, double scale) {
        double total = 0;
        for (double seconds : p) total += piece(seconds, scale);
        return total;
    }
}
