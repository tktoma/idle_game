package idle.ui;

import javafx.scene.canvas.Canvas;
import javafx.scene.layout.Pane;

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
final class CanvasPane extends Pane {

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
    static CanvasPane filling(Canvas canvas) {
        return new CanvasPane(canvas, false, Double.MAX_VALUE);
    }

    /** Le dessin reste carré, centré dans la zone, sans dépasser {@code maxSize} pixels de côté. */
    static CanvasPane square(Canvas canvas, double maxSize) {
        return new CanvasPane(canvas, true, maxSize);
    }

    @Override
    protected void layoutChildren() {
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
}
