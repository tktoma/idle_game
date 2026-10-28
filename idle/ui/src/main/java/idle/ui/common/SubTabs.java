package idle.ui.common;

import javafx.scene.control.Button;

/**
 * Les barres de sous-onglets des pages (atomes, matière noire, Big Bang, univers, collisionneur, vide, statistiques,
 * réglages) : le même bouton plat souligné de la couleur de la page quand il est choisi, gris sinon, et le même
 * câblage « choisir la sous-page, puis la recopier ».
 */
public final class SubTabs {

    /** Le style commun des sous-onglets. */
    public static final String STYLE = "-fx-font-size: 13px; -fx-padding: 6 20; -fx-cursor: hand;"
            + " -fx-background-radius: 0; -fx-border-width: 0 0 2 0; -fx-background-color: transparent;";
    /** Le même, aux boutons plus serrés : les statistiques et les réglages, qui en ont beaucoup. */
    public static final String NARROW_STYLE = "-fx-font-size: 13px; -fx-padding: 6 16; -fx-cursor: hand;"
            + " -fx-background-radius: 0; -fx-border-width: 0 0 2 0; -fx-background-color: transparent;";

    /** Le style d'un sous-onglet : souligné de {@code color} s'il est choisi, gris sinon. */
    public static String style(boolean selected, String color) {
        return style(STYLE, selected, color);
    }

    /** Le même, sur une autre base ({@link #NARROW_STYLE}). */
    public static String style(String base, boolean selected, String color) {
        return base + (selected
                ? " -fx-text-fill: " + color + "; -fx-border-color: transparent transparent " + color + " transparent;"
                : " -fx-text-fill: #8fa3b8; -fx-border-color: transparent;");
    }

    /**
     * Câble les onglets : le i-ième choisit la sous-page i puis la recopie (une sous-page n'est recopiée que lorsqu'elle
     * est affichée : elle l'est donc dès qu'on la choisit). {@code focusless} leur retire le parcours au clavier.
     */
    public static void wire(boolean focusless, java.util.function.IntConsumer select, Runnable refresh, Button... tabs) {
        for (int i = 0; i < tabs.length; i++) {
            int index = i;
            if (focusless) tabs[i].setFocusTraversable(false);
            tabs[i].setOnAction(event -> {
                select.accept(index);
                refresh.run();
            });
        }
    }

    private SubTabs() {}
}
