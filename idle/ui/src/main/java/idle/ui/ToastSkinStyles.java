package idle.ui;

import idle.core.universe.Skin;
import java.util.Locale;

/**
 * Les cinq skins des notifications (version 0.199), à côté de {@link ToastPane} pour le garder lisible. Le liseré de
 * couleur du classique (celle de l'onglet dont la nouvelle parle) reste un repère sur chacun :
 * <ul>
 * <li>« Post-it » : un carré jaune un peu de travers, écriture sombre, le liseré en haut comme une bande collante ;</li>
 * <li>« Télégramme » : papier beige, lettres à machine en capitales, le texte se termine par « STOP » ;</li>
 * <li>« Bulle de BD » : blanc cerné de noir, très arrondi, lettres grasses ;</li>
 * <li>« Ticket de caisse » : papier blanc étroit, lettres à machine, bords crantés en pointillé ;</li>
 * <li>« Parchemin déroulé » : vélin à lettres serif, les deux bords roulés en bois sombre.</li>
 * </ul>
 */
final class ToastSkinStyles {

    private static final String BASE = "-fx-font-size: 13px; -fx-padding: 8 14;";

    private ToastSkinStyles() {
    }

    /** Le style d'un message sous ce skin, {@code color} le liseré du classique ; {@code null} rend le style classique. */
    static String style(Skin skin, String color) {
        if (skin == null) return ToastPane.STYLE + color + ";";
        return switch (skin) {
            case TOAST_POSTIT -> BASE + " -fx-text-fill: #3a3010; -fx-font-family: 'Serif'; -fx-background-color: #fff176;"
                    + " -fx-background-radius: 0; -fx-border-radius: 0; -fx-border-width: 6 0 0 0; -fx-border-color: " + color + "88;";
            case TOAST_TELEGRAM -> BASE + " -fx-text-fill: #2a2318; -fx-font-family: 'Monospaced'; -fx-background-color: #efe6c8;"
                    + " -fx-background-radius: 0; -fx-border-radius: 0; -fx-border-width: 1 1 1 6; -fx-border-color: #8a7a5a #8a7a5a #8a7a5a " + color + ";";
            case TOAST_COMIC -> BASE + " -fx-text-fill: #000000; -fx-font-weight: bold; -fx-background-color: #000000, #ffffff;"
                    + " -fx-background-insets: 0, 2; -fx-background-radius: 18, 16; -fx-border-radius: 18; -fx-border-width: 0 0 4 0;"
                    + " -fx-border-color: " + color + ";";
            case TOAST_RECEIPT -> BASE + " -fx-text-fill: #222222; -fx-font-family: 'Monospaced'; -fx-font-size: 12px;"
                    + " -fx-background-color: #fafafa; -fx-background-radius: 0; -fx-border-radius: 0; -fx-border-style: dashed;"
                    + " -fx-border-width: 2 0 2 0; -fx-border-color: " + color + " transparent " + color + " transparent;";
            case TOAST_SCROLL -> BASE + " -fx-padding: 8 22; -fx-text-fill: #3a2a10; -fx-font-family: 'Serif';"
                    + " -fx-background-color: #5a3a1e, #e9d8a6; -fx-background-insets: 0, 0 10 0 10; -fx-background-radius: 6, 0;"
                    + " -fx-border-radius: 0; -fx-border-width: 1 0 1 0; -fx-border-color: " + color + ";";
            default -> ToastPane.STYLE + color + ";";
        };
    }

    /** Le texte affiché : en capitales et terminé par « STOP » sur le télégramme, tel quel ailleurs. */
    static String text(Skin skin, String text) {
        if (skin == Skin.TOAST_TELEGRAM) return text.toUpperCase(Locale.FRENCH) + " STOP";
        return text;
    }

    /** L'inclinaison du message, en degrés : le post-it est collé un peu de travers. */
    static double rotation(Skin skin) {
        return skin == Skin.TOAST_POSTIT ? -1.5 : 0;
    }

    /** La largeur maximale d'un message : le ticket de caisse est étroit. */
    static double maxWidth(Skin skin) {
        return skin == Skin.TOAST_RECEIPT ? 300 : 440;
    }
}
