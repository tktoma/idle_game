package idle.ui;

import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.transform.Scale;

/**
 * Zone qui agrandit ou réduit tout ce qu'elle contient d'un même facteur : c'est le réglage
 * « taille de l'interface ».
 *
 * <p>Le contenu est mis en page dans une fenêtre fictive, plus petite ou plus grande que la
 * vraie, puis étiré pour la remplir exactement. À 120 %, une fenêtre de 960 pixels de large est
 * donc mise en page comme si elle en faisait 800 : les textes grossissent, et les pages qui
 * s'adaptent à une fenêtre étroite le font un peu plus tôt.
 */
final class ScaledPane extends Pane {

    private final Region content;
    private final Scale transform = new Scale(1, 1, 0, 0);
    private double scale = 1;

    ScaledPane(Region content) {
        this.content = content;
        content.getTransforms().add(transform);
        getChildren().add(content);
    }

    /** Change le facteur d'agrandissement : 1 pour la taille normale. */
    void setScale(double scale) {
        if (!(scale > 0)) throw new IllegalArgumentException("Facteur d'agrandissement invalide : " + scale);
        this.scale = scale;
        transform.setX(scale);
        transform.setY(scale);
        requestLayout();
    }

    double scale() {
        return scale;
    }

    @Override
    protected void layoutChildren() {
        content.resizeRelocate(0, 0, getWidth() / scale, getHeight() / scale);
    }
}
