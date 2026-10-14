package idle.ui;

import javafx.scene.effect.Blend;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.ColorInput;
import javafx.scene.effect.Effect;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

/**
 * Les thèmes : toute la fenêtre du thème sombre, passée par un filtre ({@link Settings.Theme}).
 *
 * <p>Les couleurs du jeu sont écrites pour un fond sombre, dans chaque page et dans chaque
 * dessin. Plutôt que de tenir une palette par thème, chaque thème filtre l'image : le thème clair
 * l'inverse (le fond presque noir devient presque blanc, les textes clairs deviennent foncés),
 * puis fait tourner les teintes d'un demi-tour pour que chaque couleur retrouve la sienne ; les
 * autres tournent les teintes, désaturent, éclaircissent, renforcent le contraste, et peuvent
 * teinter le tout d'une couleur (multipliée : le noir reste noir). Pages, dessins et graphiques
 * suivent donc tous ensemble.
 *
 * <p>Avant la version 0.63, seul le thème clair existait, dans une classe {@code LightTheme}.
 */
final class ThemeFilter {

    private final Region region;
    /** Les couleurs pleines du filtre : elles doivent couvrir exactement la zone. */
    private ColorInput white;
    private ColorInput tint;

    ThemeFilter(Region region) {
        this.region = region;
    }

    /** Applique le filtre d'un thème à la zone ; le thème sombre n'en a pas. */
    void apply(Settings.Theme theme) {
        white = null;
        tint = null;
        region.setEffect(theme.isPlain() ? null : effect(theme));
        fit();
    }

    /** Le filtre d'un thème : l'inversion, puis le réglage des couleurs, puis la teinte. */
    Effect effect(Settings.Theme theme) {
        Effect input = null;
        if (theme.invert()) {
            // Différence avec du blanc : chaque couleur devient son contraire.
            white = new ColorInput(0, 0, 1, 1, Color.WHITE);
            Blend invert = new Blend(BlendMode.DIFFERENCE);
            invert.setTopInput(white);
            input = invert;
        }
        ColorAdjust adjust = new ColorAdjust();
        // Un demi-tour de teinte, pour le thème clair : le contraire d'un bleu est un orange, qui redevient bleu.
        adjust.setHue(theme.hue());
        adjust.setSaturation(theme.saturation());
        adjust.setBrightness(theme.brightness());
        adjust.setContrast(theme.contrast());
        if (input != null) adjust.setInput(input);
        if (theme.tint() == null) return adjust;
        tint = new ColorInput(0, 0, 1, 1, Color.web(theme.tint()));
        Blend multiply = new Blend(BlendMode.MULTIPLY);
        multiply.setBottomInput(adjust);
        multiply.setTopInput(tint);
        return multiply;
    }

    /** Les couleurs pleines doivent couvrir exactement la zone : à rappeler quand la fenêtre change de taille. */
    void fit() {
        for (ColorInput input : new ColorInput[] {white, tint}) {
            if (input == null) continue;
            input.setWidth(Math.max(1, region.getWidth()));
            input.setHeight(Math.max(1, region.getHeight()));
        }
    }
}
