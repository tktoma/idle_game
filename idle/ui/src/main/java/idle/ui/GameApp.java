package idle.ui;

import idle.core.Game;
import idle.core.Upgrade;
import java.util.LinkedHashMap;
import java.util.Map;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Aperçu minimal du jeu : la matière, la production, et un bouton par amélioration.
 *
 * <p>La fenêtre ne contient aucune règle : elle appelle {@code game.tick()},
 * affiche l'état, et transmet les clics à {@code game.buy()}.
 */
public final class GameApp extends Application {

    private final Game game = new Game();
    private final Label matterLabel = new Label();
    private final Label productionLabel = new Label();
    private final Map<Upgrade, Button> upgradeButtons = new LinkedHashMap<>();

    @Override
    public void start(Stage stage) {
        matterLabel.setStyle("-fx-font-size: 36px; -fx-font-weight: bold;");
        productionLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666666;");

        VBox root = new VBox(12, matterLabel, productionLabel);
        root.setPadding(new Insets(24));
        root.setAlignment(Pos.TOP_CENTER);

        // Un bouton par amélioration du catalogue : en ajouter une dans core suffit à la faire apparaître.
        for (Upgrade upgrade : game.upgrades()) {
            Button button = new Button();
            button.setMaxWidth(Double.MAX_VALUE);
            button.setOnAction(event -> {
                game.buy(upgrade.id());
                refresh();
            });
            upgradeButtons.put(upgrade, button);
            root.getChildren().add(button);
        }

        // Boucle de jeu : appelée à chaque image (~60 fois par seconde) sur le thread JavaFX.
        new AnimationTimer() {
            private long previous = 0;

            @Override
            public void handle(long now) {
                if (previous != 0) {
                    game.tick((now - previous) / 1e9); // nanosecondes → secondes
                }
                previous = now;
                refresh();
            }
        }.start();

        refresh();
        stage.setTitle("Idle");
        stage.setScene(new Scene(root, 420, 260));
        stage.show();
    }

    /** Recopie l'état du jeu dans les composants. */
    private void refresh() {
        matterLabel.setText(game.state().matter().format() + " matière");
        productionLabel.setText("+" + game.productionPerSecond().format() + " par seconde");
        upgradeButtons.forEach((upgrade, button) -> {
            button.setText(upgrade.name()
                    + " (niveau " + game.levelOf(upgrade.id()) + ")"
                    + " : " + game.costOf(upgrade.id()).format() + " matière");
            button.setDisable(!game.canBuy(upgrade.id()));
        });
    }
}