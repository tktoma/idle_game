package idle.ui;

import idle.core.universe.Skin;
import idle.core.universe.SkinVisual;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javafx.scene.ImageCursor;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

/**
 * Les cinq skins du curseur (version 0.199) : une image de 32 pixels dessinée au code (pas de fichier image), posée sur
 * la fenêtre par un {@link ImageCursor}. Le curseur système reste le classique. Chaque curseur a sa pointe : le bout du
 * doigt, le cœur de l'étoile, le bec de l'arrosoir, le centre de la loupe, le bout de la baguette.
 * <ul>
 * <li>« Main de verre » : une main qui pointe, translucide et cernée de blanc bleuté ;</li>
 * <li>« Étoile filante » : une étoile d'or qui laisse une traînée d'étincelles ({@link Trail}) ;</li>
 * <li>« Arrosoir » : un petit arrosoir vert, trois gouttes au bec ; il ne se montre que sur l'onglet Univers ;</li>
 * <li>« Loupe » : une lentille cerclée et son manche ; elle ne se montre que sur l'onglet Big Bang ;</li>
 * <li>« Baguette » : une baguette sombre, une étoile à son bout et deux étincelles.</li>
 * </ul>
 */
final class CursorSkinArt {

    static final int SIZE = 32;
    private static final Map<Skin, ImageCursor> CURSORS = new EnumMap<>(Skin.class);

    private CursorSkinArt() {
    }

    /** Vrai pour les skins du curseur. */
    static boolean handles(Skin skin) {
        return skin != null && skin.visual() == SkinVisual.CURSOR;
    }

    /**
     * Le skin à montrer sur cet onglet : l'arrosoir seulement sur l'univers, la loupe seulement sur le Big Bang, les
     * autres partout ; {@code null} pour le curseur système.
     */
    static Skin shownOn(Skin skin, GameAppBase.Tab tab) {
        if (!handles(skin)) return null;
        if (skin == Skin.CURSOR_WATERING_CAN) return tab == GameAppBase.Tab.UNIVERSE ? skin : null;
        if (skin == Skin.CURSOR_MAGNIFIER) return tab == GameAppBase.Tab.BIG_BANG ? skin : null;
        return skin;
    }

    /** Le curseur de ce skin, dessiné une fois ; {@code null} pour un skin qui n'en a pas. */
    static ImageCursor cursor(Skin skin) {
        if (!handles(skin)) return null;
        return CURSORS.computeIfAbsent(skin, CursorSkinArt::build);
    }

    private static ImageCursor build(Skin skin) {
        Canvas canvas = new Canvas(SIZE, SIZE);
        draw(skin, canvas.getGraphicsContext2D());
        SnapshotParameters parameters = new SnapshotParameters();
        parameters.setFill(Color.TRANSPARENT);
        WritableImage image = canvas.snapshot(parameters, new WritableImage(SIZE, SIZE));
        double[] hotspot = hotspot(skin);
        return new ImageCursor(image, hotspot[0], hotspot[1]);
    }

    /** La pointe du curseur, en pixels de l'image. */
    static double[] hotspot(Skin skin) {
        return switch (skin) {
            case CURSOR_GLASS_HAND -> new double[] {11, 2};
            case CURSOR_SHOOTING_STAR -> new double[] {7, 7};
            case CURSOR_WATERING_CAN -> new double[] {3, 9};
            case CURSOR_MAGNIFIER -> new double[] {12, 12};
            default -> new double[] {3, 3};
        };
    }

    /** Dessine le curseur de ce skin sur un fond transparent de {@link #SIZE} pixels. */
    static void draw(Skin skin, GraphicsContext g) {
        g.clearRect(0, 0, SIZE, SIZE);
        switch (skin) {
            case CURSOR_GLASS_HAND -> {
                g.setFill(Color.color(0.78, 0.9, 1, 0.45));
                g.setStroke(Color.web("#e8f4ff"));
                g.setLineWidth(1.5);
                g.fillRoundRect(7, 13, 16, 14, 7, 7);        // la paume
                g.strokeRoundRect(7, 13, 16, 14, 7, 7);
                g.fillRoundRect(9, 2, 5, 14, 4, 4);          // l'index tendu
                g.strokeRoundRect(9, 2, 5, 14, 4, 4);
                g.fillRoundRect(15, 9, 4, 7, 3, 3);          // le majeur replié
                g.strokeRoundRect(15, 9, 4, 7, 3, 3);
                g.fillRoundRect(2, 15, 7, 4, 3, 3);          // le pouce
                g.strokeRoundRect(2, 15, 7, 4, 3, 3);
            }
            case CURSOR_SHOOTING_STAR -> {
                g.setStroke(Color.color(1, 0.85, 0.4, 0.55));
                g.setLineWidth(3);
                g.strokeLine(9, 9, 28, 28);
                g.setStroke(Color.color(1, 1, 1, 0.6));
                g.setLineWidth(1);
                g.strokeLine(9, 9, 24, 24);
                star(g, 7, 7, 6.5, 2.8, Color.web("#ffd23f"), Color.WHITE);
            }
            case CURSOR_WATERING_CAN -> {
                g.setFill(Color.web("#3fa65b"));
                g.setStroke(Color.web("#e9f7dc"));
                g.setLineWidth(1.2);
                g.fillRoundRect(11, 14, 15, 13, 3, 3);       // le corps
                g.strokeRoundRect(11, 14, 15, 13, 3, 3);
                g.setLineWidth(3);
                g.setStroke(Color.web("#3fa65b"));
                g.strokeLine(12, 17, 4, 10);                 // le bec
                g.setStroke(Color.web("#e9f7dc"));
                g.setLineWidth(1.5);
                g.strokeArc(14, 8, 10, 10, 0, 180, javafx.scene.shape.ArcType.OPEN);   // la poignée
                g.setFill(Color.web("#9fd0ff"));
                g.fillOval(1, 4, 3, 4);                      // trois gouttes
                g.fillOval(4, 2, 3, 4);
                g.fillOval(0, 9, 3, 4);
            }
            case CURSOR_MAGNIFIER -> {
                g.setFill(Color.color(0.8, 0.92, 1, 0.3));
                g.fillOval(3, 3, 18, 18);
                g.setStroke(Color.web("#16202e"));
                g.setLineWidth(3);
                g.strokeOval(3, 3, 18, 18);
                g.setStroke(Color.web("#e8f4ff"));
                g.setLineWidth(1);
                g.strokeOval(3, 3, 18, 18);
                g.setStroke(Color.web("#16202e"));
                g.setLineWidth(4);
                g.strokeLine(19, 19, 29, 29);                // le manche
                g.setStroke(Color.color(1, 1, 1, 0.8));
                g.setLineWidth(1.5);
                g.strokeArc(6, 6, 8, 8, 100, 60, javafx.scene.shape.ArcType.OPEN);     // le reflet
            }
            case CURSOR_WAND -> {
                g.setStroke(Color.web("#3a2a16"));
                g.setLineWidth(3.5);
                g.strokeLine(6, 6, 28, 28);
                g.setStroke(Color.web("#e8d8a0"));
                g.setLineWidth(1);
                g.strokeLine(8, 6, 28, 26);
                star(g, 4, 4, 4, 1.8, Color.web("#ffd27f"), Color.WHITE);
                g.setFill(Color.web("#c8a2ff"));
                g.fillOval(12, 2, 3, 3);
                g.fillOval(2, 12, 3, 3);
            }
            default -> { }
        }
    }

    /** Une étoile à cinq branches pleine, cernée. */
    private static void star(GraphicsContext g, double cx, double cy, double outer, double inner, Color fill, Color edge) {
        double[] xs = new double[10], ys = new double[10];
        for (int i = 0; i < 10; i++) {
            double r = i % 2 == 0 ? outer : inner;
            double a = -Math.PI / 2 + i * Math.PI / 5;
            xs[i] = cx + r * Math.cos(a);
            ys[i] = cy + r * Math.sin(a);
        }
        g.setFill(fill);
        g.fillPolygon(xs, ys, 10);
        g.setStroke(edge);
        g.setLineWidth(1);
        g.strokePolygon(xs, ys, 10);
    }

    /**
     * La traînée de l'étoile filante : un calque transparent sur toute la fenêtre, qui ne prend aucun clic, où chaque
     * mouvement de la souris laisse une étincelle qui s'éteint en {@link #LIFE} secondes.
     */
    static final class Trail extends Canvas {

        static final double LIFE = 0.6;
        private static final int MAX_SPARKS = 60;
        /** Les étincelles : x, y, âge en secondes. */
        private final List<double[]> sparks = new ArrayList<>();
        private boolean active;

        Trail() {
            setMouseTransparent(true);
        }

        /** Allume ou éteint la traînée ; éteinte, les étincelles restantes s'effacent. */
        void setActive(boolean active) {
            this.active = active;
            if (!active) {
                sparks.clear();
                getGraphicsContext2D().clearRect(0, 0, getWidth(), getHeight());
            }
        }

        boolean isActive() {
            return active;
        }

        /** La souris est passée là (en coordonnées du calque) : une étincelle de plus. */
        void move(double x, double y) {
            if (!active) return;
            sparks.add(new double[] {x, y, 0});
            while (sparks.size() > MAX_SPARKS) sparks.remove(0);
        }

        /** Les étincelles encore allumées. */
        int sparks() {
            return sparks.size();
        }

        /** Prend la taille de la fenêtre. */
        void fit(double width, double height) {
            setWidth(width);
            setHeight(height);
        }

        /** Vieillit les étincelles et redessine la traînée. */
        void frame(double dt) {
            if (sparks.isEmpty()) return;
            for (double[] spark : sparks) spark[2] += dt;
            sparks.removeIf(spark -> spark[2] >= LIFE);
            GraphicsContext g = getGraphicsContext2D();
            g.clearRect(0, 0, getWidth(), getHeight());
            for (double[] spark : sparks) {
                double life = 1 - spark[2] / LIFE;
                double size = 2 + 4 * life;
                g.setFill(Color.color(1, 0.86, 0.45, 0.85 * life));
                g.fillOval(spark[0] - size / 2, spark[1] - size / 2, size, size);
                g.setStroke(Color.color(1, 1, 1, 0.6 * life));
                g.setLineWidth(1);
                g.strokeLine(spark[0] - size, spark[1], spark[0] + size, spark[1]);
                g.strokeLine(spark[0], spark[1] - size, spark[0], spark[1] + size);
            }
        }
    }
}
