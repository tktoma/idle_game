package idle.ui;

import idle.core.Game;
import idle.core.universe.UniverseTransition;
import idle.ui.common.Labels;
import java.util.List;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Le guide de transition entre univers (version 0.127), par-dessus la fenêtre : avant de former l'univers ou de
 * lancer l'univers suivant, ce qui repart de zéro, ce qui reste, et ce que la transition apporte. Le moteur dresse
 * la liste ({@link Game#universeTransition(boolean)}) ; ici, on la montre, puis on attend la réponse du joueur.
 * « Plus tard » ne change rien.
 */
final class TransitionPane extends Overlay {

    private static final String CARD_STYLE = cardStyle("#c8a2ff", true, "22 28");
    private static final String GO_STYLE = buttonStyle("8 20", "#c8a2ff");
    private static final String LATER_STYLE = "-fx-font-size: 13px; -fx-padding: 8 20; -fx-cursor: hand; -fx-background-radius: 6;"
            + " -fx-border-radius: 6; -fx-text-fill: #c9d1d9; -fx-background-color: #16202e; -fx-border-color: #3a4a5e;";

    private final Game game;
    private final Label title = new Label();
    private final Label intro = new Label();
    private final VBox lostColumn = new VBox(4);
    private final VBox keptColumn = new VBox(4);
    private final Label gainedLabel = new Label();
    private final Button go = new Button();
    private final Button later = new Button("Plus tard");
    private Runnable onGo = () -> { };

    TransitionPane(Game game) {
        super(VEIL);
        this.game = game;
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #c8a2ff;");
        Labels.centered(intro, "-fx-font-size: 13px; -fx-text-fill: #e8f4ff;");
        Labels.centered(gainedLabel, "-fx-font-size: 13px; -fx-text-fill: #ffd27f;");
        HBox columns = new HBox(18, column("Repart de zéro", "#ff8a75", lostColumn), column("Reste", "#9be7a8", keptColumn));
        columns.setAlignment(Pos.TOP_CENTER);
        go.setStyle(GO_STYLE);
        later.setStyle(LATER_STYLE);
        go.setFocusTraversable(false);
        later.setFocusTraversable(false);
        go.setOnAction(event -> confirm());
        later.setOnAction(event -> close());
        HBox buttons = new HBox(12, later, go);
        buttons.setAlignment(Pos.CENTER);
        VBox card = new VBox(14, title, intro, columns, gainedLabel, buttons);
        card.setAlignment(Pos.CENTER);
        card.setStyle(CARD_STYLE);
        card.setMaxWidth(760);
        card.setMaxHeight(USE_PREF_SIZE);
        mount(card);
    }

    private static VBox column(String heading, String color, VBox lines) {
        Label head = new Label(heading);
        head.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        VBox box = new VBox(8, head, lines);
        box.setPrefWidth(340);
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    /** Vrai tant que le guide est affiché. */
    boolean isOpen() {
        return isVisible();
    }

    /**
     * Ouvre le guide pour cette transition ; {@code action} n'a lieu que si le joueur confirme.
     *
     * @param launching vrai pour « Lancer l'univers », faux pour la formation de l'univers
     */
    void open(boolean launching, Runnable action) {
        UniverseTransition transition = game.universeTransition(launching);
        onGo = action == null ? () -> { } : action;
        title.setText(launching ? "Lancer l'univers suivant" : "Former l'univers");
        intro.setText(launching
                ? "Une partie neuve commence, celle de l'univers suivant. Voici ce qui repart de zéro et ce qui reste."
                : "Former l'univers termine la partie : tout ce qui y a mené repart de zéro. Voici ce qui reste.");
        fill(lostColumn, transition.lost());
        fill(keptColumn, transition.kept());
        gainedLabel.setText((transition.gained().isEmpty() ? "" : "Et vous gagnez : " + String.join(" ", transition.gained()))
                + "\nEntrée : confirmer · Échap : plus tard.");
        go.setText(launching ? "Lancer l'univers" : "Former l'univers");
        setVisible(true);
    }

    private static void fill(VBox box, List<String> lines) {
        box.getChildren().clear();
        for (String line : lines) {
            Label label = new Label("· " + line);
            label.setWrapText(true);
            label.setStyle("-fx-font-size: 12px; -fx-text-fill: #e8f4ff;");
            box.getChildren().add(label);
        }
    }

    /** Confirme la transition : comme un clic sur le bouton mis en avant (Entrée au clavier). */
    void confirm() {
        if (!isOpen()) return;
        Runnable action = onGo;
        close();
        action.run();
    }

    void close() {
        setVisible(false);
        onGo = () -> { };
    }
}
