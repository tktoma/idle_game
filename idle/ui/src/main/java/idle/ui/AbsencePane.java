package idle.ui;

import idle.core.Absence;
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
 * Le retour après une absence, par-dessus la fenêtre : d'abord l'avancée du rattrapage, avec un
 * bouton pour passer la suite ; puis ce que l'absence a rapporté, avec un bouton pour continuer.
 *
 * <p>Le calcul est dans {@link Absence} ; ce panneau ne fait que l'afficher. Tant qu'il est
 * ouvert, il prend les clics : la fenêtre dessous est assombrie.
 */
final class AbsencePane extends StackPane {

    private static final String CARD_STYLE = "-fx-background-color: #16202e; -fx-background-radius: 10; -fx-border-radius: 10;"
            + " -fx-border-color: #3a4a5e; -fx-padding: 22 28;";
    private static final String BUTTON_STYLE = "-fx-font-size: 13px; -fx-padding: 8 18; -fx-cursor: hand; -fx-background-radius: 6;"
            + " -fx-border-radius: 6; -fx-text-fill: #0b0e14; -fx-background-color: #d6dde6; -fx-border-color: #ffffff;";

    private final Label title = new Label("Pendant votre absence");
    private final Label summary = new Label();
    private final Label body = new Label();
    private final Button button = new Button();
    private final VBox card = new VBox(12, title, summary, body, button);
    private Absence absence;
    /** Ce qu'il faut faire quand le rattrapage se termine, qu'il soit allé au bout ou que le joueur l'ait passé. */
    private final Runnable onSettled;
    private boolean settled = false;

    /** @param onSettled appelé une fois par absence, quand il ne reste plus rien à rejouer */
    AbsencePane(Runnable onSettled) {
        this.onSettled = onSettled;
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #e8f4ff;");
        summary.setStyle("-fx-font-size: 13px; -fx-text-fill: #8fa3b8;");
        summary.setWrapText(true);
        summary.setTextAlignment(TextAlignment.CENTER);
        body.setStyle("-fx-font-size: 14px; -fx-text-fill: #e8f4ff;");
        body.setWrapText(true);
        body.setTextAlignment(TextAlignment.CENTER);
        button.setStyle(BUTTON_STYLE);
        button.setOnAction(event -> press());
        card.setAlignment(Pos.CENTER);
        card.setStyle(CARD_STYLE);
        card.setMaxWidth(460);
        card.setMaxHeight(USE_PREF_SIZE);
        setStyle("-fx-background-color: #0b0e14cc;");
        setPadding(new Insets(24));
        getChildren().add(card);
        setVisible(false);
    }

    /** Ouvre le panneau sur une absence à rattraper. */
    void open(Absence absence) {
        this.absence = absence;
        settled = false;
        setVisible(true);
        refresh();
    }

    /** Vrai tant que le panneau est affiché. */
    boolean isOpen() {
        return absence != null;
    }

    /** Vrai tant qu'il reste du temps à rejouer : la fenêtre ne fait alors rien d'autre. */
    boolean isCatchingUp() {
        return absence != null && !absence.isDone();
    }

    /** L'absence affichée, ou {@code null}. */
    Absence absence() {
        return absence;
    }

    /** Ferme le panneau. */
    void close() {
        absence = null;
        setVisible(false);
    }

    /** Le bouton : pendant le rattrapage il passe la suite, ensuite il ferme. */
    private void press() {
        if (absence == null) return;
        if (!absence.isDone()) {
            absence.skip();
            refresh();
        } else {
            close();
        }
    }

    /**
     * Recopie l'avancée du rattrapage, ou son bilan. À l'instant où le rattrapage se termine, la
     * fenêtre en est prévenue ; et s'il n'a rien rapporté du tout, le panneau se ferme de lui-même :
     * il n'y a rien à annoncer.
     */
    void refresh() {
        if (absence == null) return;
        if (absence.isDone() && !settled) {
            settled = true;
            onSettled.run();
            if (absence.report().isEmpty()) {
                close();
                return;
            }
        }
        Absence.Report report = absence.report();
        summary.setText(summary(report, absence.isDone() ? -1 : absence.total()));
        if (!absence.isDone()) {
            body.setText("Rattrapage : " + (int) Math.floor(absence.progress() * 100) + " %  ("
                    + Format.duration(absence.done()) + " sur " + Format.duration(absence.total()) + ")");
            button.setText("Passer la suite");
        } else {
            List<String> lines = lines(report);
            body.setText(lines.isEmpty() ? "Rien de nouveau." : String.join("\n", lines));
            button.setText("Continuer");
        }
    }

    /**
     * La première phrase : combien de temps, et quelle part en est rejouée.
     *
     * @param toReplay temps de jeu à rattraper en tout, tant que le rattrapage est en cours ; négatif une fois
     *                 fini : la phrase dit alors ce qui a été rejoué
     */
    static String summary(Absence.Report report, double toReplay) {
        String away = Format.duration(report.away());
        String capped = report.away() > report.counted() ? ", dont " + Format.duration(report.counted()) + " comptées" : "";
        String start = "Absence de " + away + capped + ". " + ElementText.percent(report.rate()) + " du temps est rejoué"
                + (report.rate() < 1 ? " à ce stade de la partie" : "") + " : ";
        if (toReplay >= 0) return start + Format.duration(toReplay) + " de jeu à rattraper.";
        return start + Format.duration(report.played()) + " de jeu" + (report.skipped() ? " (la suite a été passée)." : ".");
    }

    /** Ce que l'absence a rapporté, une ligne par chose ; ce qui n'a pas bougé n'est pas dit. */
    static List<String> lines(Absence.Report report) {
        List<String> lines = new ArrayList<>();
        if (!report.particles().isZero()) lines.add("+" + Format.count(report.particles()) + " particules créées");
        if (report.fusions() > 0) {
            lines.add(Format.whole(report.fusions()) + (report.fusions() > 1 ? " fusions" : " fusion") + " : +"
                    + Format.count(report.atoms()) + " atomes");
        }
        if (report.syntheses() > 0) {
            lines.add(Format.whole(report.syntheses()) + (report.syntheses() > 1 ? " synthèses" : " synthèse") + " : "
                    + Format.whole(report.elements()) + (report.elements() > 1 ? " exemplaires d'éléments" : " exemplaire d'élément"));
        }
        if (report.explosions() > 0) {
            lines.add(Format.whole(report.explosions()) + (report.explosions() > 1 ? " explosions" : " explosion"));
        }
        if (!report.space().isZero()) lines.add("+" + Format.count(report.space()) + " d'espace");
        if (report.moleculeCreations() > 0) {
            lines.add(Format.whole(report.moleculeCreations()) + (report.moleculeCreations() > 1 ? " créations de molécules" : " création de molécules"));
        }
        if (report.achievements() > 0) {
            lines.add(report.achievements() + (report.achievements() > 1 ? " succès obtenus" : " succès obtenu"));
        }
        return lines;
    }
}
