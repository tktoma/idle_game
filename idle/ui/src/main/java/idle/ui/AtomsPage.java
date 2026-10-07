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
 *   <li>« Améliorations » : ce qu'on peut acheter en dépensant des atomes, une {@link Card}
 *       chacune, rangées en colonnes selon la largeur de la fenêtre. Chaque carte dit le
 *       niveau, ce que l'amélioration vaut déjà, ce qu'elle fait en quelques mots, son prix
 *       et le nombre de fusions qu'il reste à faire ; l'explication complète vient en mode
 *       détails ({@link Detail}) ;</li>
 *   <li>« Tableau périodique » : la synthèse d'éléments et leurs effets ({@link PeriodicTablePage}).
 *       Cette sous-page n'apparaît qu'une fois assez d'atomes créés, comme l'automatisation.</li>
 * </ul>
 */
final class AtomsPage extends VBox {

    private static final String SUB_TAB_STYLE = "-fx-font-size: 13px; -fx-padding: 6 20; -fx-cursor: hand;"
            + " -fx-background-radius: 0; -fx-border-width: 0 0 2 0; -fx-background-color: transparent;";

    private static final String AMOUNT_STYLE = "-fx-font-size: 12px; -fx-padding: 3 12; -fx-cursor: hand;"
            + " -fx-background-radius: 6; -fx-border-radius: 6;";

    private final Game game;
    private final Settings settings;
    private final Label balanceLabel = new Label();
    private final Label bonusLabel = new Label();
    /** Au survol : d'où viennent les atomes d'une fusion, un facteur par ligne. */
    private final Breakdown bonusBreakdown;

    private final Button atomTab = new Button("Atome");
    private final Button upgradesTab = new Button("Améliorations");
    private final Button tableTab = new Button("Tableau périodique");
    /** Sous-page affichée : 0 = atome, 1 = améliorations, 2 = tableau périodique. */
    private int selected = 0;

    // Sous-page « Atome »
    private final AtomModelView atomView = new AtomModelView(240);
    /** L'atome prend la place disponible, en restant carré, jusqu'à 440 pixels de côté. */
    private final CanvasPane atomHolder = CanvasPane.square(atomView, 440);
    private final Label orbsLabel = new Label();
    private final Label hintLabel = new Label();
    private final VBox atomPane = new VBox(10, atomHolder, orbsLabel, hintLabel);

    // Sous-page « Améliorations »
    private final Map<Upgrade, Card> upgradeCards = new LinkedHashMap<>();
    private final TileGrid upgradeGrid = new TileGrid(200, 4, 10);
    /** La quantité achetée à chaque clic, une fois l'achat groupé acquis : la même que pour les particules. */
    private final Map<Settings.BuyAmount, Button> amountButtons = new java.util.EnumMap<>(Settings.BuyAmount.class);
    private final HBox amountRow = new HBox(6);
    private final Label amountHint = new Label();
    private final VBox upgradesBox = new VBox(8, amountRow, amountHint, upgradeGrid);
    private final ScrollPane upgradesPane = new ScrollPane(upgradesBox);

    // Sous-page « Tableau périodique »
    private final PeriodicTablePage tablePage;
    private final ScrollPane tablePane;

    AtomsPage(Game game, Settings settings) {
        super(10);
        this.game = game;
        this.settings = settings;
        this.tablePage = new PeriodicTablePage(game);
        this.bonusBreakdown = Breakdown.attach(bonusLabel, game, Breakdown.Of.ATOMS);
        this.tablePane = new ScrollPane(tablePage);
        setAlignment(Pos.TOP_CENTER);
        setPadding(new Insets(16, 24, 24, 24));

        balanceLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #ffd27f;");
        bonusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #ffe9c2;");
        orbsLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #8fa3b8;");
        hintLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #8fa3b8;");

        // Une sous-page n'est recopiée que lorsqu'elle est affichée : elle l'est donc dès qu'on la choisit.
        atomTab.setOnAction(event -> {
            select(0);
            refresh();
        });
        upgradesTab.setOnAction(event -> {
            select(1);
            refresh();
        });
        tableTab.setOnAction(event -> {
            select(2);
            refresh();
        });
        HBox subTabs = new HBox(atomTab, upgradesTab, tableTab);
        subTabs.setAlignment(Pos.CENTER);

        atomPane.setAlignment(Pos.TOP_CENTER);
        VBox.setVgrow(atomHolder, Priority.ALWAYS);
        hintLabel.setWrapText(true);
        hintLabel.setTextAlignment(TextAlignment.CENTER);
        bonusLabel.setWrapText(true);
        bonusLabel.setTextAlignment(TextAlignment.CENTER);

        // Les cartes passent à la ligne selon la largeur ; si elles dépassent en hauteur, on fait défiler.
        upgradeGrid.setPadding(new Insets(4, 0, 12, 0));
        upgradesBox.setAlignment(Pos.TOP_CENTER);
        upgradesBox.setPadding(new Insets(10, 0, 0, 0));
        Label amountLabel = new Label("Quantité par achat :");
        amountLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #8fa3b8;");
        amountRow.getChildren().add(amountLabel);
        amountRow.setAlignment(Pos.CENTER);
        for (Settings.BuyAmount amount : Settings.BuyAmount.values()) {
            Button button = new Button(amount.label());
            button.setFocusTraversable(false);
            button.setOnAction(event -> {
                settings.setBuyAmount(amount);
                refresh();
            });
            amountButtons.put(amount, button);
            amountRow.getChildren().add(button);
        }
        amountHint.setStyle("-fx-font-size: 12px; -fx-text-fill: #8fa3b8;");
        amountHint.setWrapText(true);
        amountHint.setTextAlignment(TextAlignment.CENTER);
        upgradesPane.setFitToWidth(true);
        upgradesPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        upgradesPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        tablePane.setFitToWidth(true);
        tablePane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        tablePane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        // Une carte par amélioration payée en atomes : en ajouter une dans core suffit à la faire apparaître.
        for (Upgrade upgrade : game.upgrades(Resource.ATOMS)) {
            Card card = new Card(GameApp.ATOMS_COLOR);
            card.setOnAction(() -> {
                game.buy(upgrade.id(), amount());
                refresh();
            });
            upgradeCards.put(upgrade, card);
            upgradeGrid.add(card);
        }

        // Les sous-pages sont empilées au même endroit ; une seule est visible à la fois.
        StackPane pages = new StackPane(atomPane, upgradesPane, tablePane);
        VBox.setVgrow(pages, Priority.ALWAYS);

        getChildren().add(balanceLabel);
        getChildren().add(bonusLabel);
        getChildren().add(subTabs);
        getChildren().add(pages);
        select(0);
    }

    /** Affiche une sous-page : 0 = atome, 1 = améliorations, 2 = tableau périodique. */
    /** Niveaux demandés à chaque clic : la quantité choisie une fois l'achat groupé acquis, un seul sinon. */
    private int amount() {
        return game.isBulkAtomBuyUnlocked() ? settings.buyAmount().count() : 1;
    }

    /** Passe à la sous-page suivante ou précédente, en boucle ; le tableau périodique seulement s'il est ouvert. */
    void step(int direction) {
        int count = game.isPeriodicTableUnlocked() ? 3 : 2;
        select(Math.floorMod(selected + direction, count));
        refresh();
    }

    /** La sous-page affichée : 0 l'atome, 1 les améliorations, 2 le tableau périodique. Pour les vérifications. */
    int selected() {
        return selected;
    }

    private void select(int index) {
        selected = index;
        atomPane.setVisible(index == 0);
        upgradesPane.setVisible(index == 1);
        tablePane.setVisible(index == 2);
        atomTab.setStyle(subTabStyle(index == 0));
        upgradesTab.setStyle(subTabStyle(index == 1));
        tableTab.setStyle(subTabStyle(index == 2));
    }

    private static String subTabStyle(boolean selected) {
        return SUB_TAB_STYLE + (selected
                ? " -fx-text-fill: #ffd27f; -fx-border-color: transparent transparent #ffd27f transparent;"
                : " -fx-text-fill: #8fa3b8; -fx-border-color: transparent;");
    }

    /** Fait avancer l'animation de l'atome, quand il est visible. */
    void frame(double dt) {
        if (selected == 0) atomView.frame(dt);
    }

    /** Recopie l'état du jeu dans les textes et les boutons. */
    void refresh() {
        refresh(true);
    }

    /**
     * Recopie l'état du jeu dans la page : son en-tête, et la sous-page affichée.
     *
     * @param visible faux quand l'onglet « Atomes » n'est pas à l'écran : rien n'est alors recopié. Seul
     *                continue ce qui a une mémoire, le nombre d'orbes de l'atome et le suivi du tableau
     *                périodique, pour que la page soit au retour comme si elle était restée affichée.
     */
    void refresh(boolean visible) {
        BigNum atoms = game.state().atoms();
        // Une orbe par atome disponible, jusqu'au nombre d'éléments du tableau périodique :
        // un atome dépensé quitte le visuel.
        int orbs = (int) Math.min(AtomModelView.MAX_ORBS, atoms.toDouble());
        atomView.setOrbs(orbs);
        // Le tableau périodique n'apparaît qu'une fois assez d'atomes créés, comme l'onglet Automatisation.
        boolean tableUnlocked = game.isPeriodicTableUnlocked();
        if (!tableUnlocked && selected == 2) select(0);
        tablePage.refresh(visible && selected == 2);
        if (!visible) return;

        BigNum created = game.state().totalAtoms();
        // Le plafond n'est rappelé que lorsqu'il a bougé : après une explosion, le tableau est plus lourd.
        boolean heavier = game.tableWeight().gt(BigNum.ONE) && !game.isAtomCapLifted();
        balanceLabel.setText(Format.count(atoms) + (heavier ? " / " + Format.count(game.atomCap()) : "")
                + (atoms.gt(BigNum.ONE) ? " atomes disponibles" : " atome disponible"));
        BigNum perFusion = game.atomsPerFusion();
        bonusLabel.setText("Chaque création donne " + Format.amount(game.particlesPerCreation())
                + " particules   |   chaque fusion donne " + Format.amount(perFusion)
                + (perFusion.gt(BigNum.ONE) ? " atomes" : " atome"));
        bonusBreakdown.refresh();

        tableTab.setVisible(tableUnlocked);
        tableTab.setManaged(tableUnlocked);
        if (selected == 1) {
            boolean bulk = game.isBulkAtomBuyUnlocked();
            amountRow.setVisible(bulk);
            amountRow.setManaged(bulk);
            amountButtons.forEach((amount, button) -> button.setStyle(AMOUNT_STYLE + (amount == settings.buyAmount()
                    ? " -fx-text-fill: #0b0e14; -fx-background-color: " + GameApp.ATOMS_COLOR + "; -fx-border-color: #ffffff;"
                    : " -fx-text-fill: " + GameApp.ATOMS_COLOR + "; -fx-background-color: #16202e; -fx-border-color: #3a4a5e;")));
            // Tant que l'achat groupé n'est pas acquis, une ligne dit où le trouver, à qui a déjà de la matière noire.
            String hint = bulk ? Detail.only("La quantité choisie vaut aussi pour les améliorations payées en particules.")
                    : game.isDarkMatterUnlocked() ? "L'achat par dix ou « max » s'ouvre avec « Achat groupé », dans les améliorations payées en matière noire."
                    : "";
            amountHint.setText(hint);
            amountHint.setVisible(!hint.isEmpty());
            amountHint.setManaged(!hint.isEmpty());
            upgradeCards.forEach(this::show);
        }
        if (selected != 0) return;
        orbsLabel.setText(orbs + " / " + AtomModelView.MAX_ORBS + (orbs > 1 ? " orbes" : " orbe")
                + (atoms.gt(Game.MAX_ATOMS) ? " (l'atome n'en montre pas plus)" : "")
                + "   |   " + Format.count(created) + (created.gt(BigNum.ONE) ? " atomes créés" : " atome créé")
                + " depuis le début");
        hintLabel.setText(game.isAtomCapReached()
                ? (tableUnlocked && !game.isPeriodicTableComplete()
                        ? "Maximum atteint : dépensez des atomes ou synthétisez un élément."
                        : "Maximum atteint : dépensez des atomes pour fusionner de nouveau.")
                : tableUnlocked ? Detail.only("Fusionnez les générateurs pour créer un atome de plus.")
                : "Automatisation et tableau périodique à " + Format.count(Game.UNLOCK_TOTAL_ATOMS)
                        + " atomes créés (vous : " + Format.count(created) + ")");
    }

    /** Remplit la carte d'une amélioration : niveau, valeur actuelle, effet, prix, fusions restantes. */
    private void show(Upgrade upgrade, Card card) {
        String id = upgrade.id();
        int level = game.levelOf(id);
        boolean oneTime = upgrade.maxLevel() == 1;
        boolean maxed = game.isMaxed(id);
        String corner = oneTime ? "" : level + (upgrade.hasLimit() ? "/" + upgrade.maxLevel() : "");
        String mark = oneTime ? (maxed ? "acquis" : "") : level > 0 ? current(upgrade) : "";
        if (maxed) {
            card.show(Card.State.DONE, corner, mark, upgrade.name(), brief(upgrade), describe(upgrade), "", "");
            return;
        }
        // Ce que le clic achèterait : la quantité choisie, ramenée à ce qui est à portée, un niveau au moins.
        int count = Math.max(1, game.affordableLevels(id, amount()));
        BigNum cost = game.costOf(id, count);
        card.show(game.canBuy(id) ? Card.State.READY : level > 0 ? Card.State.STARTED : Card.State.WAITING,
                corner, mark, upgrade.name(), brief(upgrade), describe(upgrade),
                (count > 1 ? "×" + count + " pour " : "") + Format.count(cost) + (cost.gt(BigNum.ONE) ? " atomes" : " atome"),
                remaining(cost));
    }

    /** Ce que l'amélioration vaut déjà, en un nombre : « ×8 », « +3 points », « −27 % ». */
    private String current(Upgrade upgrade) {
        int level = game.levelOf(upgrade.id());
        return switch (upgrade.effect()) {
            case Effect.StrengthenSpeed strengthen -> "+" + trim(game.speedExtraPerLevel() * 100) + " pts";
            case Effect.DiscountGenerators discount -> "−" + trim((1 - game.generatorCostFactor()) * 100) + " %";
            case Effect.MultiplyAtomsByProduction byProduction -> Format.multiplier(BigNum.of(game.fusionYield()));
            case Effect.KeepUpgradesOnFusion keep -> "";
            default -> Format.multiplier(game.particlesMultiplier(upgrade.id(), level));
        };
    }

    /** Ce que fait l'amélioration, en quelques mots : la ligne de sa carte. */
    private String brief(Upgrade upgrade) {
        return switch (upgrade.effect()) {
            case Effect.MultiplyParticles multiply -> "Particules ×" + trim(multiply.perLevel()) + " par niveau";
            case Effect.MultiplyByAtoms byAtoms -> "+" + trim(byAtoms.perAtom() * 100) + " % de particules par atome créé";
            case Effect.MultiplyByRunTime byTime -> "Plus de particules avec le temps";
            case Effect.StrengthenSpeed strengthen ->
                    "Vitesse : +" + trim(strengthen.extraPerLevel() * 100) + " point par niveau";
            case Effect.DiscountGenerators discount ->
                    "Générateurs −" + trim((1 - discount.factorPerLevel()) * 100) + " % par niveau";
            case Effect.KeepUpgradesOnFusion keep -> "La fusion garde vitesse et couplage";
            case Effect.Overload overload -> "Particules ×" + trim(overload.perLevel()) + " par niveau, sans limite";
            case Effect.MultiplyByGenerators coupling -> "Chaque générateur renforce les autres";
            case Effect.MultiplyAtomsByProduction byProduction ->
                    "+" + trim(game.fusionYieldPerDecade(byProduction) * 100) + " % d'atomes par ×10 de production";
            case Effect.MultiplySpeed speed -> "Accélère la création";
            case Effect.AddGenerator generator -> "Ajoute un générateur";
        };
    }

    /** L'explication complète d'une amélioration, avec sa valeur actuelle : pour le mode détails. */
    private String describe(Upgrade upgrade) {
        int level = game.levelOf(upgrade.id());

        // Pour un bonus pas encore acheté, on montre ce qu'il donnerait tout de suite.
        String value = Format.multiplier(game.particlesMultiplier(upgrade.id(), Math.max(level, 1)));
        String now = level > 0 ? "Actuellement " + value : "Donnerait " + value;
        String effect = switch (upgrade.effect()) {
            case Effect.MultiplyParticles multiply ->
                    "Multiplie par " + trim(multiply.perLevel()) + " les particules créées, à chaque niveau. "
                            + (level > 0 ? now : "");
            case Effect.MultiplyByAtoms byAtoms ->
                    "+" + trim(byAtoms.perAtom() * 100) + " % de particules par atome créé depuis le début, jusqu'à "
                            + Format.count(Game.MAX_ATOMS) + ". " + now;
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
                    "La fusion ne remet plus à zéro les améliorations payées en particules (vitesse, couplage). "
                            + "Les générateurs, eux, fusionnent toujours.";
            case Effect.Overload overload ->
                    "Multiplie par " + trim(overload.perLevel()) + " les particules créées, à chaque niveau, sans "
                            + "limite de niveau. " + (level > 0 ? now : "");
            case Effect.MultiplyByGenerators coupling -> "Chaque générateur renforce les autres.";
            case Effect.MultiplyAtomsByProduction byProduction -> {
                // Pas encore acheté : ce qu'il donnerait avec la production actuelle.
                double perDecade = game.fusionYieldPerDecade(byProduction);
                double decades = Math.max(0, game.productionAtFusion().divide(byProduction.threshold()).log10());
                double multiplier = level > 0 ? game.fusionYield() : 1 + perDecade * decades;
                yield "+" + trim(perDecade * 100) + " % d'atomes par fusion chaque fois que la production est "
                        + "multipliée par 10, à partir de " + Format.whole(Math.round(byProduction.threshold()))
                        + " particules par seconde. " + (level > 0 ? "Actuellement " : "Donnerait ")
                        + Format.multiplier(BigNum.of(multiplier));
            }
            case Effect.MultiplySpeed speed -> "Accélère la création.";
            case Effect.AddGenerator generator -> "Ajoute un générateur.";
        };

        return effect.trim();
    }

    /** Ce qu'il reste à attendre avant de pouvoir payer {@code cost} atomes : un nombre de fusions, ou rien. */
    private String remaining(BigNum cost) {
        long fusions = game.fusionsUntilAtoms(cost);
        if (fusions == 0) return "";
        if (fusions < 0) return "au-dessus du plafond";
        return Format.whole(fusions) + (fusions > 1 ? " fusions" : " fusion");
    }

    /** Écrit 2.0 comme « 2 » et 2.5 comme « 2.5 ». */
    private static String trim(double value) {
        return ElementText.number(value);
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
