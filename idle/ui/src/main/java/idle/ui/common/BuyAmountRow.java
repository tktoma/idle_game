package idle.ui.common;

import idle.ui.Settings;
import java.util.EnumMap;
import java.util.Map;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

/**
 * La rangée « Quantité par achat » : un niveau, dix, ou tout ce qui est à portée. Les particules et les atomes ont
 * chacun la leur, qui règlent le même choix ({@link Settings#buyAmount()}) ; seule la couleur du bouton éclairé change.
 */
public final class BuyAmountRow extends HBox {

    /** Style des trois boutons de quantité ; celui qui est choisi est éclairé. */
    private static final String STYLE = "-fx-font-size: 12px; -fx-padding: 3 12; -fx-cursor: hand;"
            + " -fx-background-radius: 6; -fx-border-radius: 6;";

    private final Map<Settings.BuyAmount, Button> buttons = new EnumMap<>(Settings.BuyAmount.class);
    /** Le dernier choix éclairé, et sa couleur : les styles ne sont réécrits que s'ils changent. */
    private Settings.BuyAmount painted;
    private String paintedColor;

    /** Le libellé, puis un bouton par quantité, qui la choisit puis rafraîchit la page. */
    public BuyAmountRow(Settings settings, Runnable refresh) {
        super(6);
        Label label = new Label("Quantité par achat :");
        label.setStyle(Labels.NOTE_STYLE);
        getChildren().add(label);
        setAlignment(Pos.CENTER);
        for (Settings.BuyAmount amount : Settings.BuyAmount.values()) {
            Button button = new Button(amount.label());
            button.setFocusTraversable(false);
            button.setOnAction(event -> {
                settings.setBuyAmount(amount);
                refresh.run();
            });
            buttons.put(amount, button);
            getChildren().add(button);
        }
    }

    /** Les trois boutons, par quantité. */
    public Map<Settings.BuyAmount, Button> buttons() {
        return buttons;
    }

    /** Éclaire de cette couleur le bouton de la quantité choisie, les autres restent sombres. */
    public void paint(Settings.BuyAmount chosen, String color) {
        if (chosen == painted && color.equals(paintedColor)) return;
        painted = chosen;
        paintedColor = color;
        buttons.forEach((amount, button) -> button.setStyle(STYLE + (amount == chosen
                ? " -fx-text-fill: #0b0e14; -fx-background-color: " + color + "; -fx-border-color: #ffffff;"
                : " -fx-text-fill: " + color + "; -fx-background-color: #16202e; -fx-border-color: #3a4a5e;")));
    }
}
