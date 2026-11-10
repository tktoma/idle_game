package idle.ui;

import idle.core.universe.Skin;
import idle.core.universe.SkinVisual;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * Les skins des panneaux par-dessus la fenêtre (version 0.199), à côté d'{@link Overlay} pour le garder lisible : ceux des
 * panneaux (aide, absence, fin), de l'écran de fin et du guide de transition. Un skin remplace le voile et le fond de la
 * carte ; sur un fond clair, chaque étiquette garde sa teinte mais passe à l'encre (sombre), et revient à son style
 * d'origine au retour du classique.
 * <ul>
 * <li>« Page de livre » : couverture de cuir, page crème avec l'ombre de la reliure à gauche, lettres serif ;</li>
 * <li>« Écran de cinéma » : rideau rouge plissé en voile, écran blanc cadré de noir, lettres noires ;</li>
 * <li>« Tableau d'affichage » : cadre de bois, liège, feuille punaisée au milieu ;</li>
 * <li>« Générique qui défile » : tout noir, lettres à machine claires, la carte monte depuis le bas ;</li>
 * <li>« Certificat encadré » : vélin, double filet d'or, lettres serif ;</li>
 * <li>« Photo souvenir » : cadre blanc de polaroïd, large en bas, un peu de travers ;</li>
 * <li>« Fondu au blanc », « Page tournée », « Diaphragme », « Trou noir » : la carte du guide de transition, blanche,
 * en papier, ronde et noire, noire cerclée de violet.</li>
 * </ul>
 */
final class PanelSkinStyles {

    private static final Pattern PADDING = Pattern.compile("-fx-padding:\\s*([^;]+);");
    private static final Pattern TEXT_FILL = Pattern.compile("-fx-text-fill:\\s*(#[0-9a-fA-F]{6,8}|[a-zA-Z]+);");
    /** Le voile de rideau du cinéma : des plis rouges qui se répètent. */
    static final String CURTAIN = "-fx-background-color: linear-gradient(from 0px 0px to 48px 0px, repeat, #4a0a10 0%, #9c1c28 45%,"
            + " #c0303c 50%, #9c1c28 55%, #4a0a10 100%);";

    private PanelSkinStyles() {
    }

    /** Vrai pour les skins habillés ici : ceux des panneaux, de l'écran de fin et de la transition. */
    static boolean handles(Skin skin) {
        return skin != null && (skin.visual() == SkinVisual.PANEL || skin.visual() == SkinVisual.ENDING || skin.visual() == SkinVisual.TRANSITION);
    }

    /** Le style du voile sous ce skin. */
    static String veil(Skin skin) {
        return switch (skin) {
            case PANEL_BOOK -> "-fx-background-color: #1a120bd0;";
            case PANEL_CINEMA -> CURTAIN;
            case PANEL_BOARD -> "-fx-background-color: #0e0a06cc;";
            case ENDING_CREDITS -> "-fx-background-color: #000000f2;";
            case ENDING_CERTIFICATE -> "-fx-background-color: #1a1508cc;";
            case TRANSITION_WHITE -> "-fx-background-color: #e8ecf2b8;";
            case TRANSITION_PAGE -> "-fx-background-color: #2a2318cc;";
            case TRANSITION_IRIS, TRANSITION_HOLE -> "-fx-background-color: #000000d8;";
            default -> Overlay.VEIL;
        };
    }

    /**
     * Le style de la carte sous ce skin, à partir de son style classique (dont le rembourrage est gardé, élargi où le
     * cadre le demande).
     */
    static String card(Skin skin, String classic) {
        double[] pad = padding(classic);
        return switch (skin) {
            case PANEL_BOOK -> "-fx-background-color: #4a2f1a, #f4ead3, linear-gradient(to right, #c9b690 0%, #f4ead300 6%);"
                    + " -fx-background-insets: 0, 6, 6; -fx-background-radius: 4 12 12 4, 0 6 6 0, 0;" + padding(pad, 6, 8, 6);
            case PANEL_CINEMA -> "-fx-background-color: #050505, #f3f0e6; -fx-background-insets: 0, 12; -fx-background-radius: 2, 0;"
                    + " -fx-border-color: #c9c2a8; -fx-border-width: 1; -fx-border-insets: 12;" + padding(pad, 14, 14, 14);
            case PANEL_BOARD -> "-fx-background-color: #5a3a1e, linear-gradient(from 0px 0px to 7px 5px, repeat, #b5824f 0%, #a8733f 50%,"
                    + " #b98a55 100%), #fbf6e4; -fx-background-insets: 0, 6, 22; -fx-background-radius: 2, 0, 0;"
                    + " -fx-border-color: #e8d8a0; -fx-border-width: 1; -fx-border-insets: 22;" + padding(pad, 20, 20, 20);
            case ENDING_CREDITS -> "-fx-background-color: #000000; -fx-background-radius: 0;" + padding(pad, 10, 10, 10);
            case ENDING_CERTIFICATE -> "-fx-background-color: #b8860b, #fdf6dc; -fx-background-insets: 0, 4; -fx-background-radius: 0;"
                    + " -fx-border-color: #b8860b, #b8860b; -fx-border-width: 1, 3; -fx-border-insets: 12, 5; -fx-border-radius: 0;"
                    + padding(pad, 12, 16, 12);
            case ENDING_PHOTO -> "-fx-background-color: #fafafa; -fx-background-radius: 2; -fx-border-color: #d0d0d0; -fx-border-radius: 2;"
                    + padding(pad, 18, 18, 54);
            case TRANSITION_WHITE -> "-fx-background-color: #f8fbff; -fx-background-radius: 10; -fx-border-radius: 10; -fx-border-width: 2;"
                    + " -fx-border-color: #c8a2ff;" + padding(pad, 0, 0, 0);
            case TRANSITION_PAGE -> "-fx-background-color: #c9b690, #f6efdc; -fx-background-insets: 0, 0 0 4 4; -fx-background-radius: 0;"
                    + " -fx-border-color: #8a7a5a; -fx-border-width: 1; -fx-border-insets: 0 0 4 4;" + padding(pad, 4, 6, 8);
            case TRANSITION_IRIS -> "-fx-background-color: #ffffff, #0a0a0a; -fx-background-insets: 0, 3; -fx-background-radius: 48, 45;"
                    + padding(pad, 14, 30, 14);
            case TRANSITION_HOLE -> "-fx-background-color: #7a3cff, #000000; -fx-background-insets: 0, 3; -fx-background-radius: 32, 29;"
                    + padding(pad, 8, 14, 8);
            default -> classic;
        };
    }

    /** Le style d'une étiquette sous ce skin, à partir de son style classique : la couleur passe à l'encre sur un fond clair. */
    static String label(Skin skin, String classic) {
        String style = classic;
        Matcher matcher = TEXT_FILL.matcher(classic);
        if (matcher.find()) {
            Color ink = ink(skin, Color.web(matcher.group(1)));
            if (ink != null) style = matcher.replaceFirst("-fx-text-fill: " + hex(ink) + ";");
        }
        return style + family(skin);
    }

    /** La couleur d'encre pour cette couleur de texte sous ce skin, ou {@code null} si le texte garde sa couleur. */
    private static Color ink(Skin skin, Color light) {
        double hue = light.getHue(), sat = light.getSaturation();
        return switch (skin) {
            case PANEL_BOOK, TRANSITION_PAGE -> Color.hsb(hue, Math.min(1, sat * 1.4 + 0.25), 0.26);
            case PANEL_CINEMA -> Color.hsb(hue, sat * 0.5, 0.12);
            case PANEL_BOARD, ENDING_PHOTO -> Color.hsb(hue, sat * 0.8, 0.2);
            case ENDING_CERTIFICATE -> Color.hsb(hue, Math.min(1, sat * 0.9 + 0.2), 0.24);
            case TRANSITION_WHITE -> Color.hsb(hue, Math.min(1, sat + 0.3), 0.3);
            case ENDING_CREDITS -> Color.hsb(hue, sat * 0.3, 0.95);
            default -> null;
        };
    }

    /** La police ajoutée au style des étiquettes : serif sur le papier, à machine sur le générique. */
    private static String family(Skin skin) {
        return switch (skin) {
            case PANEL_BOOK, ENDING_CERTIFICATE, TRANSITION_PAGE -> " -fx-font-family: 'Serif';";
            case ENDING_CREDITS -> " -fx-font-family: 'Monospaced';";
            default -> "";
        };
    }

    /** L'inclinaison de la carte, en degrés : la photo souvenir est posée de travers. */
    static double rotation(Skin skin) {
        return skin == Skin.ENDING_PHOTO ? -2.5 : 0;
    }

    /** Joue l'entrée de la carte : le générique monte depuis le bas ; les autres restent en place. */
    static void enter(VBox card, Skin skin) {
        card.setTranslateY(0);
        if (skin != Skin.ENDING_CREDITS) return;
        card.setTranslateY(260);
        TranslateTransition rise = new TranslateTransition(Duration.seconds(2.5), card);
        rise.setToY(0);
        rise.play();
    }

    /**
     * Habille toutes les étiquettes sous la carte : chacune garde en {@code classics} son style d'origine la première fois,
     * pour le retrouver au retour du classique ({@code skin} nul).
     */
    static void inkLabels(Node node, Skin skin, Map<Label, String> classics) {
        if (node instanceof Label label) {
            if (skin == null) {
                String classic = classics.remove(label);
                if (classic != null) label.setStyle(classic);
            } else {
                String classic = classics.computeIfAbsent(label, Label::getStyle);
                label.setStyle(label(skin, classic));
            }
        }
        if (node instanceof ScrollPane scroll && scroll.getContent() != null) inkLabels(scroll.getContent(), skin, classics);
        if (node instanceof Parent parent) for (Node child : parent.getChildrenUnmodifiable()) inkLabels(child, skin, classics);
    }

    /** Une table d'identité pour les styles d'origine des étiquettes. */
    static Map<Label, String> classics() {
        return new IdentityHashMap<>();
    }

    /** Le rembourrage « haut droite bas gauche » d'un style classique, en pixels. */
    private static double[] padding(String classic) {
        Matcher matcher = PADDING.matcher(classic);
        double[] pad = {20, 26, 20, 26};
        if (!matcher.find()) return pad;
        String[] parts = matcher.group(1).trim().split("\\s+");
        try {
            if (parts.length == 1) java.util.Arrays.fill(pad, Double.parseDouble(parts[0]));
            else if (parts.length == 2) {
                pad[0] = pad[2] = Double.parseDouble(parts[0]);
                pad[1] = pad[3] = Double.parseDouble(parts[1]);
            } else if (parts.length >= 4) for (int i = 0; i < 4; i++) pad[i] = Double.parseDouble(parts[i]);
        } catch (NumberFormatException odd) {
            return new double[] {20, 26, 20, 26};
        }
        return pad;
    }

    private static String padding(double[] pad, double top, double side, double bottom) {
        return " -fx-padding: " + Math.round(pad[0] + top) + " " + Math.round(pad[1] + side) + " " + Math.round(pad[2] + bottom) + " "
                + Math.round(pad[3] + side) + ";";
    }

    private static String hex(Color color) {
        return String.format(java.util.Locale.ROOT, "#%02x%02x%02x", Math.round(color.getRed() * 255), Math.round(color.getGreen() * 255),
                Math.round(color.getBlue() * 255));
    }
}
