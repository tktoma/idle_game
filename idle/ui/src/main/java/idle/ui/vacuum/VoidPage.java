package idle.ui.vacuum;

import idle.core.Game;
import idle.ui.common.CanvasPane;
import idle.ui.common.Format;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

/**
 * L'onglet du vide (version 0.099) : le deuxième acte de la partie secondaire. Il s'ouvre au premier Big
 * Bang du troisième univers (deuxième avant la 0.116), avec sa ressource, l'énergie du vide. Pour l'instant sans logique : l'écume
 * quantique se regarde, et rien ne s'y gagne.
 */
public final class VoidPage extends VBox {

    /** La couleur du vide : celle de son sous-onglet dans la partie secondaire. */
    public static final String COLOR = idle.ui.universe.UniversePage.VOID_COLOR;

    private final Game game;
    private final Label energy = new Label();
    private final VoidView view = new VoidView();

    public VoidPage(Game game) {
        super(12);
        this.game = game;
        setAlignment(Pos.TOP_CENTER);
        setPadding(new Insets(14, 16, 14, 16));
        energy.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + COLOR + ";");
        Label note = new Label("Le vide n'est jamais tout à fait vide : des paires de particules y naissent sans cesse et "
                + "s'annulent aussitôt. Son énergie est la ressource du deuxième acte de la partie secondaire. Pour l'instant, "
                + "elle se regarde seulement : rien ne s'y gagne encore.");
        note.setStyle("-fx-font-size: 13px; -fx-text-fill: #8fa3b8;");
        note.setWrapText(true);
        note.setMaxWidth(760);
        note.setTextAlignment(TextAlignment.CENTER);
        CanvasPane pane = CanvasPane.filling(view);
        pane.setMinHeight(220);
        pane.setMaxWidth(900);
        VBox.setVgrow(pane, Priority.ALWAYS);
        getChildren().addAll(energy, pane, note);
        refresh();
    }

    /** Recopie l'énergie du vide. */
    public void refresh() {
        energy.setText("Énergie du vide : " + Format.count(game.state().voidEnergy()));
    }

    /** Une image de l'écume, figée si les animations de l'univers sont coupées. */
    public void frame(double elapsed, boolean motion) {
        view.frame(elapsed, motion);
    }

    public VoidView view() {
        return view;
    }
}
