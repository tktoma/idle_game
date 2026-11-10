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
 * teinter le tout d'une couleur (multipliée : le noir reste noir), et depuis la version 0.200 voiler
 * les noirs d'une couleur (mélange « écran » : le blanc reste blanc, le noir prend la couleur du
 * voile, comme le bleu de Prusse d'un cyanotype). Pages, dessins et graphiques suivent donc tous
 * ensemble.
 *
 * <p>Avant la version 0.063, seul le thème clair existait, dans une classe {@code LightTheme}.
 */
final class ThemeFilter {

    private final Region region;
    /** Les couleurs pleines du filtre : elles doivent couvrir exactement la zone. */
    private ColorInput white;
    private ColorInput tint;
    private ColorInput shade;

    ThemeFilter(Region region) {
        this.region = region;
    }

    /** Applique le filtre d'un thème à la zone ; le thème sombre n'en a pas. */
    void apply(Settings.Theme theme) {
        white = null;
        tint = null;
        shade = null;
        region.setEffect(theme.isPlain() ? null : effect(theme));
        fit();
    }

    /** Le filtre d'un thème : l'inversion, puis le réglage des couleurs, puis la teinte, puis le voile des noirs. */
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
        Effect colored = adjust;
        if (theme.tint() != null) {
            tint = new ColorInput(0, 0, 1, 1, Color.web(theme.tint()));
            Blend multiply = new Blend(BlendMode.MULTIPLY);
            multiply.setBottomInput(adjust);
            multiply.setTopInput(tint);
            colored = multiply;
        }
        if (theme.shade() == null) return colored;
        // Version 0.200 : le voile en « écran » : le noir devient la couleur du voile, le blanc reste blanc.
        shade = new ColorInput(0, 0, 1, 1, Color.web(theme.shade()));
        Blend screen = new Blend(BlendMode.SCREEN);
        screen.setBottomInput(colored);
        screen.setTopInput(shade);
        return screen;
    }

    /** Les couleurs pleines doivent couvrir exactement la zone : à rappeler quand la fenêtre change de taille. */
    void fit() {
        for (ColorInput input : new ColorInput[] {white, tint, shade}) {
            if (input == null) continue;
            input.setWidth(Math.max(1, region.getWidth()));
            input.setHeight(Math.max(1, region.getHeight()));
        }
    }
}
