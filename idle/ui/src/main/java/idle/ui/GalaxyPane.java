package idle.ui;

import idle.core.Body;
import idle.core.Game;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

/**
 * La sous-page « Galaxie » de l'onglet Big Bang : le dernier rassemblement, celui de tous les
 * astres autour du trou noir supermassif ({@link Game#formGalaxy()}).
 *
 * <p>En haut, une image de la galaxie ({@link GalaxyView}). En dessous, une seule carte : ce
 * qu'elle donne, et où en est chaque échelle d'astres, puisqu'il les faut tous. Une fois la
 * galaxie formée, un bouton mène à l'expansion de la matière, où tout ce que le joueur a créé
 * tourne désormais en bras de spirale.
 */
final class GalaxyPane extends VBox {

    private static final double VIEW_HEIGHT = 230;

    private final Game game;
    private final GalaxyView view;
    private final Label intro = new Label();
    private final Card card = new Card(GameApp.BIG_BANG_COLOR);
    private final Button look = new Button("Voir la galaxie dans l'expansion de la matière");

    /**
     * @param changed ce qu'il faut refaire quand la galaxie vient d'être formée : rafraîchir toute la page
     * @param show    ce que fait le bouton : aller voir la galaxie dans l'expansion de la matière
     */
    GalaxyPane(Game game, Runnable changed, Runnable show) {
        super(10);
        this.game = game;
        this.view = new GalaxyView(game);
        setAlignment(Pos.TOP_CENTER);
        setPadding(new Insets(10, 0, 0, 0));

        intro.setStyle("-fx-font-size: 12px; -fx-text-fill: #8fa3b8;");
        intro.setWrapText(true);
        intro.setTextAlignment(TextAlignment.CENTER);
        intro.setMaxWidth(760);

        CanvasPane viewPane = CanvasPane.filling(view);
        viewPane.setMinHeight(VIEW_HEIGHT);
        viewPane.setPrefHeight(VIEW_HEIGHT);
        viewPane.setMaxHeight(VIEW_HEIGHT);
        viewPane.setMaxWidth(820);

        card.setMaxWidth(460);
        card.setOnAction(() -> {
            game.formGalaxy();
            changed.run();
        });
        look.setStyle("-fx-font-size: 12px; -fx-padding: 4 12; -fx-cursor: hand; -fx-background-radius: 4;"
                + " -fx-border-radius: 4; -fx-text-fill: " + GameApp.BIG_BANG_COLOR + "; -fx-background-color: #121923;"
                + " -fx-border-color: " + GameApp.BIG_BANG_COLOR + "66;");
        look.setFocusTraversable(false);
        look.setOnAction(event -> show.run());

        getChildren().add(intro);
        getChildren().add(viewPane);
        getChildren().add(card);
        getChildren().add(look);
    }

    /** L'image de la galaxie : pour les vérifications. */
    GalaxyView view() {
        return view;
    }

    /** La carte de la galaxie : pour les vérifications. */
    Card card() {
        return card;
    }

    /** Fait avancer l'image de la galaxie. */
    void frame(double elapsed) {
        view.frame(elapsed);
    }

    /** Recopie l'état du jeu dans la carte. */
    void refresh() {
        boolean formed = game.hasGalaxy();
        boolean ready = game.canFormGalaxy();
        int all = game.bodies().size();
        int have = game.bodiesFormed();
        // Une ligne par échelle : un rond plein quand tous ses astres sont formés.
        StringBuilder lines = new StringBuilder("Particules, atomes, espace et matière noire +"
                + ElementText.percent(Game.GALAXY_GAIN));
        for (Body.Tier tier : Body.Tier.values()) {
            int done = game.bodiesFormed(tier);
            int wanted = game.bodiesIn(tier);
            lines.append('\n').append(formed || done == wanted ? "● " : "○ ").append(tier.label()).append(' ')
                    .append(formed ? wanted : done).append('/').append(wanted);
        }
        card.show(formed ? Card.State.DONE : ready ? Card.State.READY : have > 0 ? Card.State.STARTED : Card.State.WAITING,
                formed ? "" : have + "/" + all, formed ? "formée" : ready ? "prête" : "", "Galaxie", lines.toString(),
                "Le dernier rassemblement : tous les astres, du premier amas de roches au trou noir supermassif, se mettent "
                        + "à tourner autour de lui, et avec eux toutes vos molécules. Un achat unique, qui ne consomme rien : "
                        + "les astres restent formés et gardent leur bonus. La galaxie augmente d'un coup les quatre "
                        + "grandeurs que la matière multiplie, du double de ce que donne le trou noir supermassif. Ni "
                        + "l'explosion ni le Big Bang ne la défont.",
                formed ? "" : ready ? "Former la galaxie" : "Il manque " + (all - have) + (all - have > 1 ? " astres" : " astre"), "");
        look.setVisible(formed);
        look.setManaged(formed);
        intro.setText(formed
                ? "La galaxie est formée : tout ce que vous avez créé tourne autour du trou noir supermassif."
                : "Quand tous les astres seront formés, ils pourront se rassembler en une galaxie, autour du trou noir supermassif."
                        + Detail.only(" Dans l'expansion de la matière, les molécules s'enroulent alors en bras de spirale, "
                                + "chacune réduite à un point de sa couleur, et les astres se rangent le long des bras, les "
                                + "plus gros près du centre. Un bouton y permet de revoir la matière telle qu'elle était rangée."));
    }
}
