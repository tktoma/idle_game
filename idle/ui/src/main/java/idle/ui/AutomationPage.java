package idle.ui;

import idle.core.Automation;
import idle.core.BigNum;
import idle.core.Game;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

/**
 * Contenu de l'onglet « Automatisation » : une ligne par automatisme.
 * À gauche, sa carte : tant qu'il n'est pas acheté, elle affiche son prix en atomes et sert
 * à l'acheter ; ensuite elle devient un interrupteur pour le mettre en marche ou le couper.
 * À droite, sa cadence : le délai entre deux actions, et le bouton pour le réduire en
 * payant des atomes, jusqu'à ce qu'il n'y ait plus de délai du tout.
 *
 * <p>L'onglet n'apparaît qu'une fois l'amélioration « Persistance » achetée.
 */
final class AutomationPage extends VBox {

    private static final String CARD_STYLE = "-fx-font-size: 14px; -fx-padding: 12 16; -fx-cursor: hand;"
            + " -fx-background-radius: 8; -fx-border-radius: 8;";
    /** Automatisme pas encore acheté : couleur des atomes, puisqu'il se paie en atomes. */
    private static final String TO_BUY_STYLE = CARD_STYLE
            + " -fx-text-fill: #ffe9c2; -fx-background-color: #2a2113; -fx-border-color: #6b5a33;";
    private static final String ON_STYLE = CARD_STYLE
            + " -fx-text-fill: #c8f7d0; -fx-background-color: #14261a; -fx-border-color: #9be7a8;";
    private static final String OFF_STYLE = CARD_STYLE
            + " -fx-text-fill: #8fa3b8; -fx-background-color: #16202e; -fx-border-color: #3a4a5e;";

    private final Game game;
    private final Label balanceLabel = new Label();
    private final Map<Automation, Button> cards = new LinkedHashMap<>();
    private final Map<Automation, Button> cadences = new LinkedHashMap<>();

    AutomationPage(Game game) {
        super(12);
        this.game = game;
        setAlignment(Pos.TOP_CENTER);
        setPadding(new Insets(24));

        Label title = new Label("Automatismes");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #9be7a8;");
        Label hint = new Label("Chaque automatisme s'achète une fois, en atomes, puis s'active ou se coupe librement. "
                + "Il agit à intervalles réguliers : accélérez sa cadence pour réduire ce délai, jusqu'à le supprimer.");
        hint.setStyle("-fx-font-size: 13px; -fx-text-fill: #8fa3b8;");
        hint.setWrapText(true);
        hint.setTextAlignment(TextAlignment.CENTER);
        balanceLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #ffd27f;");
        getChildren().add(title);
        getChildren().add(hint);
        getChildren().add(balanceLabel);

        // Une carte par automatisme du catalogue : en ajouter un dans core suffit à le faire apparaître.
        for (Automation automation : game.automations()) {
            Button card = new Button();
            card.setPrefWidth(340);
            card.setPrefHeight(84);
            card.setTextAlignment(TextAlignment.CENTER);
            card.setOnAction(event -> {
                if (game.ownsAutomation(automation.id())) {
                    game.setAutomationEnabled(automation.id(), !game.isAutomationEnabled(automation.id()));
                } else {
                    game.buyAutomation(automation.id());
                }
                refresh();
            });
            cards.put(automation, card);

            Button cadence = new Button();
            cadence.setPrefWidth(200);
            cadence.setPrefHeight(84);
            cadence.setTextAlignment(TextAlignment.CENTER);
            cadence.setOnAction(event -> {
                game.speedUpAutomation(automation.id());
                refresh();
            });
            cadences.put(automation, cadence);

            HBox row = new HBox(8, card, cadence);
            row.setAlignment(Pos.CENTER);
            getChildren().add(row);
        }
    }

    /** Recopie l'état des automatismes dans les cartes. */
    void refresh() {
        BigNum atoms = game.state().atoms();
        balanceLabel.setText(Format.count(atoms) + (atoms.gt(BigNum.ONE) ? " atomes disponibles" : " atome disponible"));

        cards.forEach((automation, card) -> {
            String what = automation.isFusion()
                    ? "Fusionne dès que tous les générateurs sont débloqués"
                    : "Achète « " + automation.name() + " » dès que possible";
            if (!game.ownsAutomation(automation.id())) {
                card.setText(automation.name() + "\n" + what + "\nAcheter : " + Format.count(automation.cost())
                        + (automation.cost().gt(BigNum.ONE) ? " atomes" : " atome"));
                card.setStyle(TO_BUY_STYLE);
                card.setDisable(!game.canBuyAutomation(automation.id()));
            } else {
                boolean on = game.isAutomationEnabled(automation.id());
                card.setText(automation.name() + "\n" + what + "\n" + (on ? "En marche" : "Coupé"));
                card.setStyle(on ? ON_STYLE : OFF_STYLE);
                card.setDisable(false);
            }
        });

        cadences.forEach((automation, cadence) -> {
            double interval = game.automationInterval(automation.id());
            String delay = interval <= 0 ? "Sans délai" : "Une action toutes les " + seconds(interval);
            if (game.isAutomationInstant(automation.id())) {
                cadence.setText("Cadence maximale\n" + delay);
                cadence.setStyle(ON_STYLE);
                cadence.setDisable(true);
            } else {
                BigNum cost = game.automationSpeedCost(automation.id());
                cadence.setText(delay + "\nAccélérer : " + Format.count(cost)
                        + (cost.gt(BigNum.ONE) ? " atomes" : " atome"));
                cadence.setStyle(TO_BUY_STYLE);
                cadence.setDisable(!game.canSpeedUpAutomation(automation.id()));
            }
        });
    }

    /** 4.0 → « 4 s », 0.5 → « 0.5 s », 1.333 → « 1.33 s ». */
    private static String seconds(double value) {
        String text = String.format(Locale.ROOT, "%.2f", value);
        return text.replaceAll("0+$", "").replaceAll("\\.$", "") + " s";
    }
}
