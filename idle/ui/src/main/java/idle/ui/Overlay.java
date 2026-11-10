package idle.ui;

import idle.core.universe.Skin;
import java.util.Map;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Un panneau par-dessus la fenêtre : un voile sombre sur tout le jeu, une carte au milieu, caché au départ.
 * L'absence, la transition d'univers, la fin, l'aide et les nouveautés ({@link PopupPane}) sont bâtis dessus ;
 * chacun choisit la bordure, le rembourrage et le bouton de sa carte.
 *
 * <p>Version 0.199 : un skin ({@link #setSkin}) habille le voile, la carte et ses étiquettes ({@link PanelSkinStyles}) ;
 * il s'applique quand le panneau se montre ({@link #reveal}) ou change pendant qu'il est ouvert, et le classique rend
 * à chaque chose son style d'origine.
 */
abstract class Overlay extends StackPane {

    /** Le voile ordinaire ; les nouveautés ont le leur, un peu plus clair. */
    static final String VEIL = "-fx-background-color: #0b0e14cc;";

    private final String classicVeil;
    private VBox card;
    private String classicCard = "";
    private Skin skin;
    /** Vrai tant que la carte porte un skin : le classique a alors quelque chose à rendre. */
    private boolean dressed;
    private final Map<Label, String> classicLabels = PanelSkinStyles.classics();

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
        classicVeil = veil;
        setStyle(veil);
        setPadding(new Insets(24));
        setVisible(false);
    }

    /** Pose la carte au milieu du voile ; son style d'alors est celui que le classique rend. */
    final void mount(VBox card) {
        this.card = card;
        classicCard = card.getStyle();
        getChildren().add(card);
    }

    /** Le skin du panneau (version 0.199), {@code null} pour le classique ; un panneau ouvert change sur-le-champ. */
    final void setSkin(Skin skin) {
        if (skin == this.skin) return;
        this.skin = skin;
        if (isVisible()) dress();
    }

    final Skin skin() {
        return skin;
    }

    /** Montre le panneau, habillé de son skin : à appeler une fois la carte remplie. */
    final void reveal() {
        dress();
        setVisible(true);
        if (card != null) PanelSkinStyles.enter(card, skin);
    }

    /** La carte, pour les vérifications. */
    final VBox card() {
        return card;
    }

    /** Vrai si ce nœud est encore sous la carte. */
    private boolean under(javafx.scene.Node node) {
        for (javafx.scene.Node up = node; up != null; up = up.getParent()) if (up == card) return true;
        return false;
    }

    private void dress() {
        if (card == null) return;
        if (!PanelSkinStyles.handles(skin)) {
            if (!dressed) return;
            dressed = false;
            setStyle(classicVeil);
            card.setStyle(classicCard);
            card.setRotate(0);
            PanelSkinStyles.inkLabels(card, null, classicLabels);
            classicLabels.clear();
            return;
        }
        // Les étiquettes parties de la carte (l'aide se réécrit à chaque ouverture) n'ont plus rien à rendre.
        classicLabels.keySet().removeIf(label -> !under(label));
        dressed = true;
        setStyle(PanelSkinStyles.veil(skin));
        card.setStyle(PanelSkinStyles.card(skin, classicCard));
        card.setRotate(PanelSkinStyles.rotation(skin));
        PanelSkinStyles.inkLabels(card, skin, classicLabels);
    }
}
