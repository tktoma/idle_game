package idle.ui;

import javafx.geometry.Insets;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Un panneau par-dessus la fenêtre : un voile sombre sur tout le jeu, une carte au milieu, caché au départ.
 * L'absence, la transition d'univers, la fin, l'aide et les nouveautés ({@link PopupPane}) sont bâtis dessus ;
 * chacun choisit la bordure, le rembourrage et le bouton de sa carte.
 */
abstract class Overlay extends StackPane {

    /** Le voile ordinaire ; les nouveautés ont le leur, un peu plus clair. */
    static final String VEIL = "-fx-background-color: #0b0e14cc;";

    /** Le style d'une carte : fond sombre, coins ronds, bordure de cette couleur (épaisse ou fine), ce rembourrage. */
    static String cardStyle(String border, boolean thick, String padding) {
        return "-fx-background-color: #16202e; -fx-background-radius: 10; -fx-border-radius: 10;"
                + (thick ? " -fx-border-width: 2;" : "") + " -fx-border-color: " + border + "; -fx-padding: " + padding + ";";
    }

    /** Le style d'un bouton de carte : texte sombre sur cette couleur, ce rembourrage. */
    static String buttonStyle(String padding, String color) {
        return "-fx-font-size: 13px; -fx-padding: " + padding + "; -fx-cursor: hand; -fx-background-radius: 6;"
                + " -fx-border-radius: 6; -fx-text-fill: #0b0e14; -fx-background-color: " + color + "; -fx-border-color: #ffffff;";
    }

    Overlay(String veil) {
        setStyle(veil);
        setPadding(new Insets(24));
        setVisible(false);
    }

    /** Pose la carte au milieu du voile. */
    final void mount(VBox card) {
        getChildren().add(card);
    }
}
