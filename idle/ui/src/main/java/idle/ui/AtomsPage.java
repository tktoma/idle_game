package idle.ui;

import idle.core.BigNum;
import idle.core.Effect;
import idle.core.Game;
import idle.core.Resource;
import idle.core.Upgrade;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

/**
 * Contenu de l'onglet « Atomes ». En haut, les atomes disponibles et le bonus en cours ;
 * en dessous, deux sous-pages :
 * <ul>
 *   <li>« Atome » : le grand atome animé, qui porte une orbe par atome disponible.
 *       Dépenser un atome lui retire donc une orbe ;</li>
 *   <li>« Améliorations » : ce qu'on peut acheter en dépensant des atomes, sous forme de
 *       cartes rangées en colonnes selon la largeur de la fenêtre.</li>
 * </ul>
 */
final class AtomsPage extends VBox {

    private static final String SUB_TAB_STYLE = "-fx-font-size: 13px; -fx-padding: 6 20; -fx-cursor: hand;"
            + " -fx-background-radius: 0; -fx-border-width: 0 0 2 0; -fx-background-color: transparent;";
    private static final String UPGRADE_STYLE = "-fx-font-size: 13px; -fx-padding: 10 16; -fx-cursor: hand;"
            + " -fx-text-fill: #ffe9c2; -fx-background-color: #2a2113; -fx-background-radius: 8;"
            + " -fx-border-color: #6b5a33; -fx-border-radius: 8;";

    private final Game game;
    private final Label balanceLabel = new Label();
    private final Label bonusLabel = new Label();

    private final Button atomTab = new Button("Atome");
    private final Button upgradesTab = new Button("Améliorations");
    private boolean upgradesSelected = false;

    // Sous-page « Atome »
    private final AtomModelView atomView = new AtomModelView(240);
    private final Label orbsLabel = new Label();
    private final Label hintLabel = new Label();
    private final VBox atomPane = new VBox(10, atomView, orbsLabel, hintLabel);

    // Sous-page « Améliorations »
    private final Map<Upgrade, Button> upgradeButtons = new LinkedHashMap<>();
    private final FlowPane upgradeCards = new FlowPane(12, 12);
    private final ScrollPane upgradesPane = new ScrollPane(upgradeCards);

    AtomsPage(Game game) {
        super(10);
        this.game = game;
        setAlignment(Pos.TOP_CENTER);
        setPadding(new Insets(16, 24, 24, 24));

        balanceLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #ffd27f;");
        bonusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #ffe9c2;");
        orbsLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #8fa3b8;");
        hintLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #8fa3b8;");

        atomTab.setOnAction(event -> select(false));
        upgradesTab.setOnAction(event -> select(true));
        HBox subTabs = new HBox(atomTab, upgradesTab);
        subTabs.setAlignment(Pos.CENTER);

        atomPane.setAlignment(Pos.TOP_CENTER);

        // Les cartes passent à la ligne selon la largeur ; si elles dépassent en hauteur, on fait défiler.
        upgradeCards.setAlignment(Pos.TOP_CENTER);
        upgradeCards.setPadding(new Insets(12, 0, 0, 0));
        upgradesPane.setFitToWidth(true);
        upgradesPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        upgradesPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        // Une carte par amélioration payée en atomes : en ajouter une dans core suffit à la faire apparaître.
        for (Upgrade upgrade : game.upgrades(Resource.ATOMS)) {
            Button button = new Button();
            button.setPrefWidth(350);
            button.setPrefHeight(112);
            button.setWrapText(true);
            button.setTextAlignment(TextAlignment.CENTER);
            button.setStyle(UPGRADE_STYLE);
            button.setOnAction(event -> {
                game.buy(upgrade.id());
                refresh();
            });
            upgradeButtons.put(upgrade, button);
            upgradeCards.getChildren().add(button);
        }

        // Les deux sous-pages sont empilées au même endroit ; une seule est visible à la fois.
        StackPane pages = new StackPane(atomPane, upgradesPane);
        VBox.setVgrow(pages, Priority.ALWAYS);

        getChildren().add(balanceLabel);
        getChildren().add(bonusLabel);
        getChildren().add(subTabs);
        getChildren().add(pages);
        select(false);
    }

    /** Affiche la sous-page « Améliorations » ({@code true}) ou « Atome » ({@code false}). */
    private void select(boolean upgrades) {
        upgradesSelected = upgrades;
        atomPane.setVisible(!upgrades);
        upgradesPane.setVisible(upgrades);
        atomTab.setStyle(subTabStyle(!upgrades));
        upgradesTab.setStyle(subTabStyle(upgrades));
    }

    private static String subTabStyle(boolean selected) {
        return SUB_TAB_STYLE + (selected
                ? " -fx-text-fill: #ffd27f; -fx-border-color: transparent transparent #ffd27f transparent;"
                : " -fx-text-fill: #8fa3b8; -fx-border-color: transparent;");
    }

    /** Fait avancer l'animation de l'atome, quand il est visible. */
    void frame(double dt) {
        if (!upgradesSelected) atomView.frame(dt);
    }

    /** Recopie l'état du jeu dans les textes et les boutons. */
    void refresh() {
        BigNum atoms = game.state().atoms();
        BigNum created = game.state().totalAtoms();
        balanceLabel.setText(Format.count(atoms)
                + (atoms.gt(BigNum.ONE) ? " atomes disponibles" : " atome disponible"));
        bonusLabel.setText("Chaque création donne " + Format.multiplier(game.particlesPerCreation())
                .substring(1) + " particules");

        // Une orbe par atome disponible, jusqu'au nombre d'éléments du tableau périodique :
        // un atome dépensé quitte le visuel.
        int orbs = (int) Math.min(AtomModelView.MAX_ORBS, atoms.toDouble());
        atomView.setOrbs(orbs);
        orbsLabel.setText(orbs + " / " + AtomModelView.MAX_ORBS + (orbs > 1 ? " orbes" : " orbe")
                + "   |   " + Format.count(created) + (created.gt(BigNum.ONE) ? " atomes créés" : " atome créé")
                + " depuis le début");
        hintLabel.setText("Fusionnez les " + game.maxGeneratorCount()
                + " générateurs pour créer un atome de plus.");

        upgradeButtons.forEach((upgrade, button) -> {
            button.setText(describe(upgrade));
            button.setDisable(!game.canBuy(upgrade.id()));
        });
    }

    /** Texte d'une carte : nom et niveau, effet avec sa valeur actuelle, coût. */
    private String describe(Upgrade upgrade) {
        int level = game.levelOf(upgrade.id());
        boolean oneTime = upgrade.maxLevel() == 1;
        boolean maxed = game.isMaxed(upgrade.id());

        String title = upgrade.name();
        if (oneTime) {
            if (maxed) title += " (acquis)";
        } else if (upgrade.hasLimit()) {
            title += " (niveau " + level + "/" + upgrade.maxLevel() + ")";
        } else {
            title += " (niveau " + level + ")";
        }

        // Pour un bonus pas encore acheté, on montre ce qu'il donnerait tout de suite.
        String value = Format.multiplier(game.particlesMultiplier(upgrade.id(), Math.max(level, 1)));
        String now = level > 0 ? "Actuellement " + value : "Donnerait " + value;
        String effect = switch (upgrade.effect()) {
            case Effect.MultiplyParticles multiply ->
                    "Multiplie par " + trim(multiply.perLevel()) + " les particules créées, à chaque niveau. "
                            + (level > 0 ? now : "");
            case Effect.MultiplyByAtoms byAtoms ->
                    "+" + trim(byAtoms.perAtom() * 100) + " % de particules par atome créé depuis le début. " + now;
            case Effect.MultiplyByRunTime byTime ->
                    "Plus de particules à mesure que le temps passe depuis la dernière fusion. " + now;
            case Effect.StrengthenSpeed strengthen ->
                    "Renforce « Vitesse de création » : +" + trim(strengthen.extraPerLevel() * 100)
                            + " point de vitesse en plus à chacun de ses niveaux. "
                            + (level > 0 ? "Actuellement +" + trim(game.speedExtraPerLevel() * 100) + " points" : "");
            case Effect.DiscountGenerators discount ->
                    "Générateurs " + trim((1 - discount.factorPerLevel()) * 100) + " % moins chers, à chaque niveau. "
                            + (level > 0 ? "Actuellement −" + trim((1 - game.generatorCostFactor()) * 100) + " %" : "");
            case Effect.KeepUpgradesOnFusion keep ->
                    "La fusion ne remet plus à zéro « Vitesse de création ». "
                            + "Les générateurs, eux, fusionnent toujours.";
            case Effect.MultiplySpeed speed -> "Accélère la création.";
            case Effect.AddGenerator generator -> "Ajoute un générateur.";
        };

        if (maxed) return title + "\n" + effect.trim();
        BigNum cost = game.costOf(upgrade.id());
        return title + "\n" + effect.trim() + "\nCoût : " + Format.count(cost)
                + (cost.gt(BigNum.ONE) ? " atomes" : " atome");
    }

    /** Écrit 2.0 comme « 2 » et 2.5 comme « 2.5 ». */
    private static String trim(double value) {
        String text = String.format(Locale.ROOT, "%.2f", value);
        return text.contains(".") ? text.replaceAll("0+$", "").replaceAll("\\.$", "") : text;
    }

    /** Un atome vient d'être créé : l'atome grossit d'un coup puis reprend sa taille. */
    void celebrate() {
        ScaleTransition arrive = new ScaleTransition(Duration.seconds(0.6), atomView);
        arrive.setFromX(1.6);
        arrive.setFromY(1.6);
        arrive.setToX(1);
        arrive.setToY(1);
        arrive.play();
    }
}
