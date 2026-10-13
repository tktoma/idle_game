package idle.ui.common;

import javafx.scene.layout.Region;

/**
 * Empêche un texte qui change souvent de faire sauter ce qui est en dessous. Quand un nombre
 * s'allonge ou raccourcit vingt fois par seconde, sa ligne peut passer d'une à deux lignes et
 * retour : tout ce qui suit monte et descend, et un clic sur un sous-onglet tombe à côté. Une
 * zone stable garde la plus grande hauteur qu'elle a prise, tant que sa largeur ne change pas ;
 * une nouvelle largeur (fenêtre redimensionnée) repart de zéro.
 */
public final class StableHeight {

    private StableHeight() {}

    public static <T extends Region> T of(T region) {
        region.heightProperty().addListener((observable, before, height) -> {
            if (height.doubleValue() > region.getMinHeight()) region.setMinHeight(height.doubleValue());
        });
        region.widthProperty().addListener((observable, before, width) -> {
            if (Math.abs(width.doubleValue() - before.doubleValue()) >= 1) region.setMinHeight(Region.USE_COMPUTED_SIZE);
        });
        return region;
    }
}
