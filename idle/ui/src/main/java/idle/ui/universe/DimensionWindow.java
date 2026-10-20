package idle.ui.universe;

import idle.core.Game;
import idle.ui.common.CanvasPane;
import idle.ui.common.Format;
import idle.core.math.BigNum;
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
 * La fenêtre d'une dimension (version 0.081) : elle couvre tout le jeu, avec son visuel, son nom et
 * une phrase, et un bouton « Retour » pour revenir au jeu. Le jeu continue de tourner derrière.
 *
 * <p>La première, la ligne, a sa logique depuis la version 0.101 : ses éclats de dimension, et ses deux
 * améliorations sous le visuel. Les neuf autres restent des visuels.
 */
public final class DimensionWindow extends BorderPane {

    private static final String BUTTON_STYLE = "-fx-font-size: 13px; -fx-padding: 8 16; -fx-cursor: hand; -fx-background-radius: 6;"
            + " -fx-border-radius: 6; -fx-text-fill: " + UniversePage.COLOR + "; -fx-background-color: #141a2c; -fx-border-color: #3b4670;";

    private final Game game;
    private final DimensionView view;
    /** La ligne : ses éclats, et ses deux améliorations. */
    private final Label shards = new Label();
    private final Button yieldButton = new Button();
    private final Button ticksButton = new Button();
    private final VBox linePane;
    private final Label title = new Label();
    private final Label line = new Label();
    private final Button back = new Button("← Retour");

    public DimensionWindow(Game game) {
        this.game = game;
        this.view = new DimensionView(game);
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
        shards.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + UniversePage.COLOR + ";");
        for (Button button : new Button[] {yieldButton, ticksButton}) {
            button.setStyle(BUTTON_STYLE);
            button.setFocusTraversable(false);
            button.setWrapText(true);
            button.setMaxWidth(360);
            button.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        }
        yieldButton.setOnAction(event -> {
            game.buyLineYield();
            refresh();
        });
        ticksButton.setOnAction(event -> {
            game.buyLineTicks();
            refresh();
        });
        HBox buttons = new HBox(14, yieldButton, ticksButton);
        buttons.setAlignment(Pos.CENTER);
        linePane = new VBox(10, shards, buttons);
        linePane.setAlignment(Pos.CENTER);
        linePane.setPadding(new Insets(10, 0, 0, 0));
        setBottom(linePane);
        setVisible(false);
    }

    /** Recopie la ligne : ses éclats, et ce que coûtent ses deux améliorations. */
    private void refresh() {
        boolean line = view.dimension() == Dimension.LINE && game.isLineOpen();
        linePane.setVisible(line);
        linePane.setManaged(line);
        if (!line) return;
        shards.setText("Éclats de dimension : " + Format.darkMatter(BigNum.of(game.state().dimensionShards()))
                + "   (+" + rate(game.lineShardsPerSecond()) + " par seconde)");
        yieldButton.setText("Densité (niveau " + game.state().lineYieldLevel() + ") : chaque trait touché donne "
                + Format.darkMatter(BigNum.of(game.lineShardsPerTouch() + Game.LINE_SHARD)) + " éclat au lieu de "
                + Format.darkMatter(BigNum.of(game.lineShardsPerTouch())) + "\nPrix : " + Format.darkMatter(BigNum.of(game.lineYieldCost())) + " éclats");
        yieldButton.setDisable(!game.canBuyLineYield());
        boolean full = game.state().lineTicksLevel() >= Game.LINE_MAX_EXTRA_TICKS;
        ticksButton.setText(full ? "Graduation : la ligne est pleine (" + game.lineTicks() + " traits)"
                : "Graduation (" + game.lineTicks() + " traits) : un trait de plus, tous à égale distance\nPrix : "
                        + Format.darkMatter(BigNum.of(game.lineTicksCost())) + " éclats");
        ticksButton.setDisable(!game.canBuyLineTicks());
    }

    /** Un débit d'éclats : deux décimales sous 1, une au-delà. */
    private static String rate(double perSecond) {
        if (perSecond < 1) return String.format(java.util.Locale.ROOT, "%.2f", Math.floor(perSecond * 100 + 1e-9) / 100);
        return Format.darkMatter(BigNum.of(perSecond));
    }

    /** Ouvre la fenêtre de cette dimension. */
    public void open(Dimension dimension) {
        view.show(dimension);
        title.setText("Dimension " + dimension.number() + " : " + dimension.label());
        line.setText(dimension == Dimension.LINE
                ? dimension.line() + " Chaque trait que touche la boule donne des éclats de dimension."
                : dimension.line() + " Il n'y a rien à y faire pour l'instant.");
        setVisible(true);
        refresh();
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
        if (!isVisible()) return;
        view.frame(elapsed, moving);
        refresh();
    }

    /** Le visuel : pour les vérifications. */
    public DimensionView view() {
        return view;
    }

    /** Les deux améliorations de la ligne : pour les vérifications. */
    public Button yieldButton() {
        return yieldButton;
    }

    public Button ticksButton() {
        return ticksButton;
    }

    public String shardsText() {
        return shards.getText();
    }

    /** Le bouton de retour : pour les vérifications. */
    public Button backButton() {
        return back;
    }
}
