package idle.ui.universe;

import idle.ui.common.CanvasPane;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * La fenêtre d'une dimension (version 0.81) : elle couvre tout le jeu, avec son visuel, son nom et
 * une phrase, et un bouton « Retour » pour revenir au jeu. Le jeu continue de tourner derrière.
 * Sans logique pour l'instant.
 */
public final class DimensionWindow extends BorderPane {

    private final DimensionView view = new DimensionView();
    private final Label title = new Label();
    private final Label line = new Label();
    private final Button back = new Button("← Retour");

    public DimensionWindow() {
        setStyle("-fx-background-color: #05060e;");
        setPadding(new Insets(12, 16, 16, 16));
        back.setStyle("-fx-font-size: 13px; -fx-padding: 6 16; -fx-cursor: hand; -fx-background-radius: 6; -fx-border-radius: 6;"
                + " -fx-text-fill: " + UniversePage.COLOR + "; -fx-background-color: #141a2c; -fx-border-color: #3b4670;");
        back.setFocusTraversable(false);
        back.setOnAction(event -> close());
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + UniversePage.COLOR + ";");
        line.setStyle("-fx-font-size: 13px; -fx-text-fill: #c8d4ea;");
        line.setWrapText(true);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        // Une marge à droite aussi large que le bouton, pour que le titre reste au milieu.
        Region balance = new Region();
        balance.setMinWidth(110);
        Region after = new Region();
        HBox.setHgrow(after, Priority.ALWAYS);
        HBox top = new HBox(12, back, spacer, title, after, balance);
        top.setAlignment(Pos.CENTER_LEFT);
        VBox head = new VBox(6, top, line);
        head.setAlignment(Pos.CENTER);
        line.setMaxWidth(Double.MAX_VALUE);
        line.setAlignment(Pos.CENTER);
        head.setPadding(new Insets(0, 0, 10, 0));
        setTop(head);
        CanvasPane holder = CanvasPane.filling(view);
        setCenter(holder);
        setVisible(false);
    }

    /** Ouvre la fenêtre de cette dimension. */
    public void open(Dimension dimension) {
        view.show(dimension);
        title.setText("Dimension " + dimension.number() + " : " + dimension.label());
        line.setText(dimension.line() + " Il n'y a rien à y faire pour l'instant.");
        setVisible(true);
    }

    /** Referme la fenêtre : retour au jeu. */
    public void close() {
        setVisible(false);
    }

    public boolean isOpen() {
        return isVisible();
    }

    /** La dimension montrée. */
    public Dimension dimension() {
        return view.dimension();
    }

    public String titleText() {
        return title.getText();
    }

    public void frame(double elapsed, boolean moving) {
        if (isVisible()) view.frame(elapsed, moving);
    }

    /** Le visuel : pour les vérifications. */
    public DimensionView view() {
        return view;
    }

    /** Le bouton de retour : pour les vérifications. */
    public Button backButton() {
        return back;
    }
}
