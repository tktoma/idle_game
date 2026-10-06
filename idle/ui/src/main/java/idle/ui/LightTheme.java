package idle.ui;

import javafx.scene.effect.Blend;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.ColorInput;
import javafx.scene.effect.Effect;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

/**
 * Le thème clair : toute la fenêtre du thème sombre, passée par un filtre.
 *
 * <p>Les couleurs du jeu sont écrites pour un fond sombre, dans chaque page et dans chaque
 * dessin. Plutôt que de tenir deux palettes à jour, le thème clair inverse l'image (le fond
 * presque noir devient presque blanc, les textes clairs deviennent foncés), puis fait tourner
 * les teintes d'un demi-tour pour que chaque couleur retrouve la sienne : le jaune des atomes
 * reste jaune, en plus foncé. Pages, dessins et graphiques suivent donc tous ensemble.
 */
final class LightTheme {

    private final Region region;
    private final ColorInput white = new ColorInput(0, 0, 1, 1, Color.WHITE);
    private final Effect effect;

    LightTheme(Region region) {
        this.region = region;
        // Différence avec du blanc : chaque couleur devient son contraire.
        Blend invert = new Blend(BlendMode.DIFFERENCE);
        invert.setTopInput(white);
        // Un demi-tour de teinte : le contraire d'un bleu est un orange, qui redevient bleu.
        ColorAdjust hue = new ColorAdjust();
        hue.setHue(1.0);
        hue.setInput(invert);
        this.effect = hue;
    }

    /** Applique le filtre à la zone, ou le retire. */
    void apply(boolean light) {
        fit();
        region.setEffect(light ? effect : null);
    }

    /** Le blanc doit couvrir exactement la zone : à rappeler quand la fenêtre change de taille. */
    void fit() {
        white.setWidth(Math.max(1, region.getWidth()));
        white.setHeight(Math.max(1, region.getHeight()));
    }
}
