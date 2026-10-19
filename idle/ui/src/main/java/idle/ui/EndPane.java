package idle.ui;

import idle.core.Game;
import idle.core.GameStats;
import idle.ui.common.Format;
import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

/**
 * L'écran de fin (version 0.68), par-dessus la fenêtre, une seule fois par partie : l'univers est
 * formé. Il dit le temps de jeu, la durée de chaque acte et ce que valent les records, quelques
 * chiffres de la partie, puis laisse continuer. Le jeu continue de tourner derrière.
 */
final class EndPane extends StackPane {

    private static final String CARD_STYLE = "-fx-background-color: #16202e; -fx-background-radius: 10; -fx-border-radius: 10;"
            + " -fx-border-color: #ffd27f; -fx-padding: 24 30;";
    private static final String BUTTON_STYLE = "-fx-font-size: 13px; -fx-padding: 8 18; -fx-cursor: hand; -fx-background-radius: 6;"
            + " -fx-border-radius: 6; -fx-text-fill: #0b0e14; -fx-background-color: #ffd27f; -fx-border-color: #ffffff;";

    private final Game game;
    private final Label title = new Label("L'univers est formé");
    private final Label summary = new Label();
    private final Label acts = new Label();
    private final Label numbers = new Label();
    private final Button button = new Button("Continuer");

    EndPane(Game game) {
        this.game = game;
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #ffd27f;");
        for (Label label : List.of(summary, acts, numbers)) {
            label.setWrapText(true);
            label.setTextAlignment(TextAlignment.CENTER);
        }
        summary.setStyle("-fx-font-size: 14px; -fx-text-fill: #e8f4ff;");
        acts.setStyle("-fx-font-size: 13px; -fx-text-fill: #e8f4ff;");
        numbers.setStyle("-fx-font-size: 12px; -fx-text-fill: #8fa3b8;");
        button.setStyle(BUTTON_STYLE);
        button.setOnAction(event -> close());
        VBox card = new VBox(14, title, summary, acts, numbers, button);
        card.setAlignment(Pos.CENTER);
        card.setStyle(CARD_STYLE);
        card.setMaxWidth(520);
        card.setMaxHeight(USE_PREF_SIZE);
        setStyle("-fx-background-color: #0b0e14cc;");
        setPadding(new Insets(24));
        getChildren().add(card);
        setVisible(false);
    }

    /** Vrai tant que l'écran est affiché. */
    boolean isOpen() {
        return isVisible();
    }

    /** Ouvre l'écran si l'univers vient d'être formé et qu'il n'a pas encore été vu dans cette partie. */
    boolean check() {
        if (isVisible() || game.state().endSeen() || !game.hasCosmos(idle.core.sky.Cosmos.UNIVERSE)) return false;
        game.state().setEndSeen(true);
        fill();
        setVisible(true);
        return true;
    }

    void close() {
        setVisible(false);
    }

    /** Les textes : le temps total, chaque acte, et quelques chiffres. */
    void fill() {
        GameStats stats = game.stats();
        double end = stats.reachedAt(GameStats.Step.UNIVERSE);
        if (Double.isNaN(end)) end = game.state().timePlayed();
        summary.setText("Tout ce que vous avez créé tient maintenant dans un univers. Temps de jeu : " + Format.duration(end)
                + record(GameStats.Step.UNIVERSE, end) + ".");
        acts.setText(String.join("\n", actLines(stats, end)));
        // Le bilan d'avant la remise à zéro de l'univers (version 0.83).
        long[] tally = stats.universeTally();
        numbers.setText(Format.whole(stats.fusions()) + " fusions, " + Format.whole(stats.timedExplosions()) + " explosions, "
                + Format.whole(tally[0]) + " Big Bangs, " + Format.whole(tally[1]) + " molécules, "
                + Format.whole(tally[2]) + " collisions, " + Format.whole(tally[3]) + " comètes saisies.\n"
                + "Tout ce qui y a mené repart de zéro. La suite se joue dans l'onglet Univers, avec votre première graine d'univers.");
    }

    /** Une ligne par acte : sa durée, de la fin du précédent à la sienne. */
    private static List<String> actLines(GameStats stats, double end) {
        List<String> lines = new ArrayList<>();
        double explosion = stats.reachedAt(GameStats.Step.FIRST_EXPLOSION);
        double bang = stats.reachedAt(GameStats.Step.FIRST_BIG_BANG);
        double galaxy = stats.reachedAt(GameStats.Step.GALAXY);
        line(lines, "Acte 1, jusqu'à la première explosion", 0, explosion);
        line(lines, "Acte 2, jusqu'au premier Big Bang", explosion, bang);
        line(lines, "Acte 3, jusqu'à la galaxie", bang, galaxy);
        line(lines, "Dernier chapitre, jusqu'à l'univers", galaxy, end);
        return lines;
    }

    private static void line(List<String> lines, String name, double from, double to) {
        if (Double.isNaN(from) || Double.isNaN(to) || to < from) return;
        lines.add(name + " : " + Format.duration(to - from));
    }

    /** Ce que vaut ce temps face au record de l'étape : nouveau record, ou l'écart au meilleur. */
    private String record(GameStats.Step step, double time) {
        double best = game.recordOf(step);
        if (Double.isNaN(best)) return "";
        if (time <= best + 1e-6) return " (record)";
        return " (record : " + Format.duration(best) + ")";
    }
}
