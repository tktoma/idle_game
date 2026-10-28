package idle.ui.common;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

/**
 * Zone qui donne à un dessin ({@link Canvas}) la place disponible dans la fenêtre.
 *
 * <p>Un {@code Canvas} a une taille fixe : il ne suit pas la fenêtre tout seul. Cette zone, elle,
 * s'étire et rétrécit librement ; à chaque changement de taille, elle redimensionne le dessin :
 * <ul>
 *   <li>soit pour remplir toute la zone ({@link #filling(Canvas)}) ;</li>
 *   <li>soit en gardant un carré centré, le plus grand possible sans dépasser une taille
 *       maximale ({@link #square(Canvas, double)}).</li>
 * </ul>
 * Le dessin doit se redessiner à chaque image d'après {@code getWidth()} et {@code getHeight()}.
 */
public final class CanvasPane extends Pane {

    private final Canvas canvas;
    private final boolean square;
    private final double maxSize;

    private CanvasPane(Canvas canvas, boolean square, double maxSize) {
        this.canvas = canvas;
        this.square = square;
        this.maxSize = maxSize;
        setMinSize(0, 0);                                   // la zone peut rétrécir librement avec la fenêtre
        setPrefSize(canvas.getWidth(), canvas.getHeight()); // taille de départ : celle du dessin
        getChildren().add(canvas);
    }

    /** Le dessin occupe toute la zone. */
    public static CanvasPane filling(Canvas canvas) {
        return new CanvasPane(canvas, false, Double.MAX_VALUE);
    }

    /** Le dessin reste carré, centré dans la zone, sans dépasser {@code maxSize} pixels de côté. */
    public static CanvasPane square(Canvas canvas, double maxSize) {
        return new CanvasPane(canvas, true, maxSize);
    }

    @Override
    public void layoutChildren() {
        double width = getWidth();
        double height = getHeight();
        if (width <= 0 || height <= 0) return;
        if (square) {
            double size = Math.floor(Math.min(maxSize, Math.min(width, height)));
            canvas.setWidth(size);
            canvas.setHeight(size);
            canvas.relocate(Math.floor((width - size) / 2), Math.floor((height - size) / 2));
        } else {
            canvas.setWidth(width);
            canvas.setHeight(height);
            canvas.relocate(0, 0);
        }
    }

    /**
     * Commence une image des vues de l'univers : rend l'opacité, efface le canevas et le peint de ce fond.
     * Rien (null) tant que le canevas n'a pas de taille : il n'y a alors rien à dessiner.
     */
    public static GraphicsContext begin(Canvas canvas, Color back) {
        double w = canvas.getWidth(), h = canvas.getHeight();
        if (w <= 0 || h <= 0) return null;
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setGlobalAlpha(1);
        g.clearRect(0, 0, w, h);
        g.setFill(back);
        g.fillRect(0, 0, w, h);
        return g;
    }
}
