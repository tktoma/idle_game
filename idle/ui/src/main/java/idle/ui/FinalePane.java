package idle.ui;

import idle.core.Game;
import idle.core.state.GameStats;
import idle.core.math.BigNum;
import idle.ui.common.Format;
import java.util.List;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

/**
 * L'écran de la fin de la partie, par-dessus la fenêtre, quand le défi final est réussi : les dix
 * paliers des dimensions atteints. Il vient au bout de l'animation de la carte des dimensions, et le bouton « Revoir la
 * fin » le ramène. Il dit le temps de jeu, les univers formés et quelques chiffres de toute la partie, remercie, puis
 * laisse choisir (version 0.267) : « Recommencer de zéro » efface toute la partie, comme dans les réglages (un second clic
 * le confirme), « Continuer la sauvegarde » la garde et ferme l'écran ; le jeu tourne toujours derrière.
 */
public final class FinalePane extends Overlay {
    private static final String GOLD = "#ffd27f";
    private static final String CARD_STYLE = cardStyle(GOLD, true, "28 34");
    private static final String BUTTON_STYLE = buttonStyle("8 22", GOLD);
    /** Recommencer de zéro, et le même prêt à tout effacer au second clic : les couleurs de la remise à zéro des réglages. */
    private static final String RESTART_STYLE = buttonStyle("8 22", "#2a1614") + " -fx-text-fill: #ffb4a8; -fx-border-color: #b8584a;";
    private static final String RESTART_ARMED_STYLE = buttonStyle("8 22", "#ff8a75") + " -fx-text-fill: #1a0a08;";
    static final String RESTART = "Recommencer de zéro", RESTART_ARMED = "Cliquer encore pour tout effacer", CONTINUE = "Continuer la sauvegarde";

    private final Game game;
    private final Label title = new Label("Toutes les dimensions sont mêlées");
    private final Label summary = new Label();
    private final Label numbers = new Label();
    private final Label thanks = new Label("Merci d'avoir joué jusqu'ici. Le jeu continue : rien ne s'arrête, et tout ce que "
            + "vous avez bâti reste à faire grandir.");
    private final Button restart = new Button(RESTART);
    private final Button button = new Button(CONTINUE);
    private boolean restartArmed;
    private Runnable onRestart = () -> { };

    public FinalePane(Game game) {
        super(VEIL);
        this.game = game;
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + GOLD + ";");
        for (Label label : List.of(summary, numbers, thanks)) {
            label.setWrapText(true);
            label.setTextAlignment(TextAlignment.CENTER);
        }
        summary.setStyle("-fx-font-size: 15px; -fx-text-fill: #e8f4ff;");
        numbers.setStyle("-fx-font-size: 13px; -fx-text-fill: #c8d4ea;");
        thanks.setStyle("-fx-font-size: 13px; -fx-font-style: italic; -fx-text-fill: #9be7a8;");
        button.setStyle(BUTTON_STYLE);
        button.setOnAction(event -> close());
        restart.setOnAction(event -> {
            if (!restartArmed) {
                arm(true);
                return;
            }
            close();
            onRestart.run();
        });
        arm(false);
        HBox choices = new HBox(14, restart, button);
        choices.setAlignment(Pos.CENTER);
        VBox card = new VBox(16, title, summary, numbers, thanks, choices);
        card.setAlignment(Pos.CENTER);
        card.setStyle(CARD_STYLE);
        card.setMaxWidth(560);
        card.setMaxHeight(USE_PREF_SIZE);
        mount(card);
    }

    /** Ce que fait « Recommencer de zéro », confirmé : effacer toute la partie. */
    public void setOnRestart(Runnable action) { onRestart = action == null ? () -> { } : action; }

    /** Montre l'écran, ses chiffres pris à cet instant ; « Recommencer de zéro » attend de nouveau ses deux clics. */
    public void open() {
        arm(false);
        fill();
        reveal();
    }

    public void close() {
        arm(false);
        setVisible(false);
    }

    /** Prépare (ou non) « Recommencer de zéro » à tout effacer au prochain clic. */
    private void arm(boolean on) {
        restartArmed = on;
        restart.setText(on ? RESTART_ARMED : RESTART);
        restart.setStyle(on ? RESTART_ARMED_STYLE : RESTART_STYLE);
    }

    public boolean isOpen() { return isVisible(); }

    /** Les textes : le temps de jeu, les univers, et quelques chiffres de toute la partie. */
    void fill() {
        GameStats stats = game.stats();
        int universes = Math.max(1, game.universes());
        summary.setText("Les dix paliers du défi final sont atteints : de la ligne au mélange, toutes les dimensions "
                + "brillent ensemble. Temps de jeu : " + Format.duration(game.state().timePlayed()) + ", " + universes
                + (universes > 1 ? " univers formés." : " univers formé."));
        double shards = 0;
        for (int d = 1; d <= Game.MAX_DIMENSIONS; d++) shards += game.dimensionEarned(d);
        numbers.setText(Format.count(stats.particlesCreated()) + " particules créées, " + Format.whole(stats.fusions())
                + " fusions, " + Format.whole(stats.timedExplosions()) + " explosions, "
                + Format.whole(game.state().seedWaterings()) + " arrosages de la graine, "
                + Format.count(BigNum.of(shards)) + " éclats gagnés dans les dimensions, "
                + Format.whole(game.dimensionAutomationsOwned()) + " automatismes des dimensions.");
    }

    /** Le titre, le résumé, les chiffres, le remerciement et le bouton : pour les vérifications. */
    public String titleText() { return title.getText(); }

    public String summaryText() { return summary.getText(); }

    public String thanksText() { return thanks.getText(); }

    public Button continueButton() { return button; }

    public Button restartButton() { return restart; }
}
