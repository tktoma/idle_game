package idle.ui.common;

import javafx.scene.control.*;
import javafx.scene.text.TextAlignment;

/** Les habillages de texte que toutes les pages répètent : la note discrète, le paragraphe centré, la zone qui défile sans fond. */
public final class Labels {
    /** Le style d'une note : petite et grise. */
    public static final String NOTE_STYLE = "-fx-font-size: 12px; -fx-text-fill: #8fa3b8;";

    /** Une phrase d'explication : discrète, centrée, qui passe à la ligne. */
    public static void note(Label label) { centered(label, NOTE_STYLE); }

    /** La même, pas plus large que {@code maxWidth}. */
    public static void note(Label label, double maxWidth) {
        note(label);
        label.setMaxWidth(maxWidth);
    }

    /** Un texte dans ce style, centré, qui passe à la ligne. */
    public static void centered(Label label, String style) {
        label.setStyle(style);
        label.setWrapText(true);
        label.setTextAlignment(TextAlignment.CENTER);
    }

    /**
     * Une bulle de survol qui passe à la ligne (version 0.315) : elle reçoit les explications retirées des pages.
     * Vide, elle ne s'affiche pas.
     */
    public static Tooltip tip(String text) {
        Tooltip tip = new Tooltip(text);
        tip.setWrapText(true);
        tip.setMaxWidth(460);
        return tip;
    }

    /** Pose ou met à jour la bulle de {@code control} ; un texte vide la retire. */
    public static void tip(Control control, String text) {
        if (text == null || text.isEmpty()) {
            if (control.getTooltip() != null) control.setTooltip(null);
        } else if (control.getTooltip() == null) {
            control.setTooltip(tip(text));
        } else if (!text.equals(control.getTooltip().getText())) {
            control.getTooltip().setText(text);
        }
    }

    /** Une zone qui défile en hauteur seulement, sans fond propre. */
    public static void transparent(ScrollPane scroll) {
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
    }

    private Labels() {}
}
