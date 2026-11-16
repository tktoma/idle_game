package idle.ui;

import idle.core.Game;
import java.util.function.IntSupplier;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;

/**
 * La bande du profil de test en bas de la fenêtre d'une dimension : de quoi tester tout de suite la dimension ouverte,
 * sans quitter sa fenêtre. Sa ressource et son seuil, ses améliorations, sa remise à zéro, son bot (de la ligne à la
 * durée), et ce qu'elle a en propre : les étapes du mélange, le rond de croissance de la boucle, la météo du clicker.
 * Elle se refait quand une autre dimension s'ouvre.
 */
final class DimensionTestBar extends FlowPane {
    private final Game game;
    private final TestActions actions;
    private final TestBots bots;
    private final IntSupplier dimension;
    private final java.util.function.Consumer<String> note;
    private final Runnable after;
    private final Label title = new Label();
    private Button live;
    /** La dimension pour laquelle la bande est faite ; 0 tant qu'aucune. */
    private int shown;

    /**
     * @param dimension la dimension ouverte (1 à 10), 0 si aucune
     * @param note      où écrire ce qu'un outil a à dire
     * @param after     ce qui suit chaque outil (le résumé du panneau)
     */
    DimensionTestBar(Game game, TestActions actions, TestBots bots, IntSupplier dimension, java.util.function.Consumer<String> note,
            Runnable after) {
        super(5, 5);
        this.game = game;
        this.actions = actions;
        this.bots = bots;
        this.dimension = dimension;
        this.note = note;
        this.after = after;
        setPadding(new Insets(6, 10, 6, 10));
        setAlignment(Pos.CENTER);
        setStyle("-fx-background-color: #2a1416; -fx-border-color: " + TestPanel.ACCENT + "; -fx-border-width: 1 0 0 0;");
        title.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + TestPanel.ACCENT + ";");
    }

    /** À chaque image : refait la bande si une autre dimension est ouverte, et suit le bot en direct. */
    void refresh() {
        int number = dimension.getAsInt();
        if (number != shown) build(number);
        if (live != null) live.setText(bots.dimensionLive(number) ? "Bot en direct : en marche" : "Bot en direct : coupé");
    }

    private void build(int number) {
        shown = number;
        getChildren().clear();
        live = null;
        if (number < 1 || number > Game.MAX_DIMENSIONS) return;
        title.setText("TEST · " + TestActions.DIMENSIONS[number - 1] + " :");
        getChildren().add(title);
        double threshold = TestActions.dimensionThreshold(number);
        tool("+10 % du seuil", () -> actions.addDimensionShards(number, threshold / 10));
        tool("+100 % du seuil", () -> actions.addDimensionShards(number, threshold));
        tool("Seuil atteint", () -> actions.reachDimensionThreshold(number));
        tool("Améliorations au maximum", () -> actions.dimensionLevels(number, true));
        tool("Améliorations à zéro", () -> actions.dimensionLevels(number, false));
        tool("Dimension à zéro", () -> actions.resetDimension(number));
        if (TestBots.hasDimensionBot(number)) {
            live = tool("", () -> bots.toggleDimensionLive(number));
            tool("+1 min avec le bot", () -> bots.dimensionFastForward(number, 60, false));
            tool("+5 min avec le bot", () -> bots.dimensionFastForward(number, 300, false));
            tool("Jusqu'au rond vert (30 min au plus)", () -> bots.dimensionFastForward(number, 1800, true));
            tool("Annuler l'avance", bots::cancel);
        }
        switch (number) {
            case 5 -> tool("Rond de croissance", () -> { if (game.isBloomUnlocked()) game.testShowBloom(); });
            case 7 -> {
                tool("Beau temps", () -> actions.weather(Game.Weather.SUN));
                tool("Orage", () -> actions.weather(Game.Weather.STORM));
            }
            case 10 -> {
                tool("Fragments de l'étape", () -> { if (!actions.mixStageShards()) note.accept("Le mélange est à sa dernière étape, ou fermé."); });
                tool("Étape suivante", () -> { if (!actions.nextMixStage()) note.accept("Le mélange est à sa dernière étape, ou fermé."); });
                tool("Retour à l'étape 1", actions::mixStageOne);
            }
            default -> { }
        }
    }

    private Button tool(String label, Runnable action) {
        Button button = new Button(label);
        button.setStyle(TestPanel.TOOL_STYLE);
        button.setFocusTraversable(false);
        button.setOnAction(event -> {
            if (!game.isStarted()) return;
            action.run();
            after.run();
        });
        getChildren().add(button);
        return button;
    }

    /** La dimension pour laquelle la bande est faite : pour les vérifications. */
    int shown() { return shown; }
}
