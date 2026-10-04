package idle.ui;

import idle.core.Game;
import idle.core.Resource;
import idle.core.Upgrade;
import java.util.LinkedHashMap;
import java.util.Map;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

/**
 * Contenu de l'onglet « Automatisation » : un interrupteur par amélioration payée en
 * particules. Quand il est activé, le jeu achète cette amélioration tout seul dès que
 * les particules le permettent.
 *
 * <p>L'onglet n'apparaît qu'une fois l'amélioration « Persistance » achetée.
 */
final class AutomationPage extends VBox {

    private static final String SWITCH_STYLE = "-fx-font-size: 14px; -fx-padding: 12 16; -fx-cursor: hand;"
            + " -fx-background-radius: 8; -fx-border-radius: 8;";
    private static final String ON_STYLE = SWITCH_STYLE
            + " -fx-text-fill: #c8f7d0; -fx-background-color: #14261a; -fx-border-color: #9be7a8;";
    private static final String OFF_STYLE = SWITCH_STYLE
            + " -fx-text-fill: #8fa3b8; -fx-background-color: #16202e; -fx-border-color: #3a4a5e;";

    private final Game game;
    private final Map<Upgrade, Button> switches = new LinkedHashMap<>();

    AutomationPage(Game game) {
        super(12);
        this.game = game;
        setAlignment(Pos.TOP_CENTER);
        setPadding(new Insets(24));

        Label title = new Label("Achats automatiques");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #9be7a8;");
        Label hint = new Label("Le jeu achète pour vous les améliorations activées, dès que les particules le permettent.");
        hint.setStyle("-fx-font-size: 13px; -fx-text-fill: #8fa3b8;");
        hint.setWrapText(true);
        hint.setTextAlignment(TextAlignment.CENTER);
        getChildren().add(title);
        getChildren().add(hint);

        // Un interrupteur par amélioration payée en particules : en ajouter une dans core suffit.
        for (Upgrade upgrade : game.upgrades(Resource.PARTICLES)) {
            Button button = new Button();
            button.setPrefWidth(420);
            button.setTextAlignment(TextAlignment.CENTER);
            button.setOnAction(event -> {
                game.setAutomated(upgrade.id(), !game.isAutomated(upgrade.id()));
                refresh();
            });
            switches.put(upgrade, button);
            getChildren().add(button);
        }
    }

    /** Recopie l'état des interrupteurs. */
    void refresh() {
        switches.forEach((upgrade, button) -> {
            boolean on = game.isAutomated(upgrade.id());
            button.setText(upgrade.name() + "\nAchat automatique : " + (on ? "activé" : "désactivé"));
            button.setStyle(on ? ON_STYLE : OFF_STYLE);
        });
    }
}
