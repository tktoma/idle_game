package idle.ui.universe;

import idle.ui.common.CanvasPane;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * L'onglet « Univers » (version 0.80), qui s'ouvre quand l'univers est formé : le début du
 * quatrième acte. Pour l'instant, il n'y a rien à y faire : la vue de l'univers, sa naissance jouée
 * à l'ouverture, et un bouton pour la revoir.
 */
public final class UniversePage extends VBox {

    /** La couleur de l'univers : un bleu pervenche, à part des autres onglets. */
    public static final String COLOR = "#a8b8ff";

    private final UniverseView view = new UniverseView();
    private final Label title = new Label("L'univers");
    private final Label line = new Label("Le quatrième acte commence ici. Tout ce que vous avez rassemblé y tient ; la suite viendra.");
    private final Button replay = new Button("Revoir sa naissance");

    public UniversePage() {
        super(10);
        setAlignment(Pos.TOP_CENTER);
        setPadding(new Insets(12, 16, 12, 16));
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + COLOR + ";");
        line.setStyle("-fx-font-size: 13px; -fx-text-fill: #c8d4ea;");
        line.setWrapText(true);
        replay.setStyle("-fx-font-size: 12px; -fx-padding: 5 14; -fx-cursor: hand; -fx-background-radius: 6; -fx-border-radius: 6;"
                + " -fx-text-fill: " + COLOR + "; -fx-background-color: #141a2c; -fx-border-color: #3b4670;");
        replay.setFocusTraversable(false);
        replay.setOnAction(event -> view.playBirth());
        CanvasPane holder = CanvasPane.filling(view);
        VBox.setVgrow(holder, Priority.ALWAYS);
        HBox bottom = new HBox(replay);
        bottom.setAlignment(Pos.CENTER);
        getChildren().addAll(title, line, holder, bottom);
    }

    /** Joue la naissance de l'univers : il vient de se former. */
    public void playBirth() {
        view.playBirth();
    }

    /** Vrai tant que la naissance se joue. */
    public boolean isBirthPlaying() {
        return view.isBirthPlaying();
    }

    /** Fait avancer l'animation ; {@code moving} faux (réglage coupé) la laisse immobile. */
    public void frame(double elapsed, boolean moving) {
        view.frame(elapsed, moving);
    }

    /** La vue de l'univers : pour les vérifications. */
    public UniverseView view() {
        return view;
    }
}
