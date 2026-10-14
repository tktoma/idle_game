package idle.ui.collider;

import idle.core.Game;
import idle.core.collider.ColliderGenerator;
import idle.core.collider.ColliderUpgrade;
import idle.core.collider.CollisionTier;
import idle.core.collider.Quark;
import idle.core.math.BigNum;
import idle.ui.common.CanvasPane;
import idle.ui.common.Card;
import idle.ui.common.Detail;
import idle.ui.common.ElementText;
import idle.ui.common.Format;
import idle.ui.common.StableHeight;
import idle.ui.common.TileGrid;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

/**
 * Le collisionneur de quarks (version 0.59) : trois parties, une par sous-onglet. L'énergie : l'anneau
 * où les faisceaux se croisent et où sortent les quarks, les saveurs ouvertes et leur part, les huit
 * générateurs. La chaîne : les quarks, et ce que l'énergie et les quarks renforcent ailleurs. Les améliorations, avec
 * le huitième générateur. Rien de ce qui s'y gagne ne se perd à l'explosion ni au Big Bang.
 */
public final class ColliderPane extends VBox {

    public static final String COLOR = "#9ad0ff";
    private static final String BUTTON_STYLE = "-fx-font-size: 13px; -fx-padding: 6 14; -fx-cursor: hand; -fx-background-radius: 6;"
            + " -fx-border-radius: 6; -fx-text-fill: " + COLOR + "; -fx-background-color: #101c2b; -fx-border-color: " + COLOR + ";";
    /** Les parties de la page : l'énergie, la chaîne, les améliorations. */
    public static final int ENERGY = 0, CHAIN = 1, UPGRADES = 2;

    private final Game game;
    private final Runnable changed;
    private final Label intro = new Label();
    private final Label energyLabel = new Label();
    private final Label flavorsTitle = new Label("Ce que peut donner une collision");
    private final FlowPane flavors = new FlowPane(8, 6);
    private final Map<Quark, Label> flavorLabels = new EnumMap<>(Quark.class);
    private final Label quarksLabel = new Label();
    /** Les quarks, dans le sous-onglet de la chaîne : elle les dépense (version 0.62). */
    private final Label quarksOnChain = new Label();
    private final Button topButton = new Button();
    private final Label overheatLabel = new Label();
    private final Label upgradesHint = new Label();
    /** Ce qui s'affiche dans le sous-onglet de la chaîne tant qu'elle n'est pas ouverte. */
    private final Label chainNote = new Label();
    private final EnergyView energyView;
    private final Map<ColliderGenerator, Label> generatorNames = new EnumMap<>(ColliderGenerator.class);
    private final Map<ColliderGenerator, Label> generatorOutputs = new EnumMap<>(ColliderGenerator.class);
    private final Map<ColliderGenerator, Button> generatorBuys = new EnumMap<>(ColliderGenerator.class);
    private final Map<ColliderGenerator, Button> generatorMaxes = new EnumMap<>(ColliderGenerator.class);
    private final Map<ColliderGenerator, Label> generatorAutos = new EnumMap<>(ColliderGenerator.class);
    private final Map<ColliderGenerator, HBox> generatorRows = new EnumMap<>(ColliderGenerator.class);
    private final Map<ColliderUpgrade.Section, TileGrid> upgradeGrids = new EnumMap<>(ColliderUpgrade.Section.class);
    private final Map<ColliderUpgrade.Section, Label> upgradeTitles = new EnumMap<>(ColliderUpgrade.Section.class);
    private final Map<ColliderUpgrade, Card> upgradeCards = new EnumMap<>(ColliderUpgrade.class);
    private final Label upgradesLeft = new Label();
    private final VBox energyGroup = new VBox(10);
    private final VBox chainGroup = new VBox(10);
    private final VBox upgradeGroup = new VBox(10);
    private int section = ENERGY;

    ColliderPane(Game game, Runnable changed) {
        super(10);
        this.game = game;
        this.changed = changed;
        this.energyView = new EnergyView(game, gain -> changed.run());
        setAlignment(Pos.TOP_CENTER);
        setPadding(new Insets(10, 0, 10, 0));

        intro.setText("Les faisceaux croisés donnent l'énergie ; chacun des huit générateurs produit le précédent. "
                + "À chaque croisement des faisceaux, une chance de collision : elle donne un quark. Un clic au moment du "
                + "croisement capte trois secondes de production et assure une collision au butin doublé. L'énergie et les "
                + "quarks paient la chaîne, qui renforce le reste du jeu ; l'énergie produite ouvre les deux trous noirs.");
        note(intro, "#8fa3b8");
        energyLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + COLOR + ";");
        StableHeight.of(energyLabel);

        flavorsTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #cfe8ff;");
        flavors.setAlignment(Pos.CENTER);
        flavors.setMaxWidth(780);
        for (Quark quark : Quark.values()) {
            Label label = new Label();
            label.setMinWidth(0);
            flavorLabels.put(quark, label);
            flavors.getChildren().add(label);
        }

        quarksLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #ffe9c2;");
        quarksOnChain.setStyle("-fx-font-size: 14px; -fx-text-fill: #ffe9c2;");
        topButton.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-padding: 8 20; -fx-cursor: hand; -fx-background-radius: 6;"
                + " -fx-border-radius: 6; -fx-text-fill: #10151f; -fx-background-color: #ffd27f; -fx-border-color: #ffffff;");
        topButton.setFocusTraversable(false);
        topButton.setWrapText(true);
        topButton.setMaxWidth(760);
        topButton.setOnAction(event -> {
            game.useTop();
            changed.run();
        });
        overheatLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #ffd27f;");
        note(upgradesHint, "#6f8296");

        note(chainNote, "#6f8296");

        CanvasPane energyPane = CanvasPane.filling(energyView);
        energyPane.setPrefHeight(230);
        energyPane.setMinHeight(230);
        energyPane.setMaxHeight(230);
        energyPane.setMaxWidth(780);
        VBox generators = new VBox(4);
        generators.setAlignment(Pos.CENTER);
        for (ColliderGenerator generator : ColliderGenerator.values()) {
            Label name = new Label();
            name.setStyle("-fx-font-size: 13px; -fx-text-fill: #cfe8ff;");
            name.setMinWidth(300);
            Label output = new Label();
            output.setStyle("-fx-font-size: 12px; -fx-text-fill: #8fa3b8;");
            output.setMinWidth(170);
            Button buy = new Button();
            buy.setStyle(BUTTON_STYLE);
            buy.setFocusTraversable(false);
            buy.setMinWidth(170);
            buy.setOnAction(event -> {
                game.buyColliderGenerator(generator);
                changed.run();
            });
            Button max = new Button("Max");
            max.setStyle(BUTTON_STYLE);
            max.setFocusTraversable(false);
            max.setOnAction(event -> {
                game.buyMaxColliderGenerator(generator);
                changed.run();
            });
            Label auto = new Label();
            auto.setStyle("-fx-font-size: 11px; -fx-text-fill: #9be7a8;");
            auto.setMinWidth(54);
            HBox row = new HBox(10, name, output, buy, max, auto);
            row.setAlignment(Pos.CENTER);
            generatorNames.put(generator, name);
            generatorOutputs.put(generator, output);
            generatorBuys.put(generator, buy);
            generatorMaxes.put(generator, max);
            generatorAutos.put(generator, auto);
            generatorRows.put(generator, row);
            generators.getChildren().add(row);
        }

        for (ColliderUpgrade.Section family : ColliderUpgrade.Section.values()) {
            Label title = new Label(family.label());
            title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + COLOR + "; -fx-padding: 6 0 0 0;");
            TileGrid grid = new TileGrid(230, 3, 8);
            grid.setMaxWidth(780);
            upgradeTitles.put(family, title);
            upgradeGrids.put(family, grid);
            // La chaîne a son sous-onglet (version 0.61) ; le reste vit dans les améliorations.
            if (family != ColliderUpgrade.Section.CHAIN) upgradeGroup.getChildren().addAll(title, grid);
        }
        for (ColliderUpgrade upgrade : ColliderUpgrade.values()) {
            Card card = new Card(COLOR);
            card.setDoneClickable(upgrade.isPausable());
            card.setOnAction(() -> {
                if (game.colliderUpgradeLevel(upgrade) >= upgrade.maxLevel() && upgrade.isPausable()) {
                    game.toggleColliderAutomation(upgrade);
                } else {
                    game.buyColliderUpgrade(upgrade);
                }
                changed.run();
            });
            upgradeCards.put(upgrade, card);
            upgradeGrids.get(upgrade.section()).add(card);
        }
        note(upgradesLeft, "#6f8296");

        for (VBox group : new VBox[] {energyGroup, chainGroup, upgradeGroup}) group.setAlignment(Pos.TOP_CENTER);
        energyGroup.getChildren().addAll(energyPane, flavorsTitle, flavors, quarksLabel, topButton, overheatLabel, generators, upgradesHint);
        chainGroup.getChildren().addAll(quarksOnChain, upgradeTitles.get(ColliderUpgrade.Section.CHAIN), upgradeGrids.get(ColliderUpgrade.Section.CHAIN),
                chainNote);
        upgradeGroup.getChildren().add(upgradesLeft);
        getChildren().addAll(intro, energyLabel, energyGroup, chainGroup, upgradeGroup);
        select(ENERGY);
    }

    private static void note(Label label, String color) {
        label.setStyle("-fx-font-size: 12px; -fx-text-fill: " + color + ";");
        label.setWrapText(true);
        label.setMaxWidth(760);
        label.setTextAlignment(TextAlignment.CENTER);
    }

    /** La couleur d'une saveur de quark. */
    public static String quarkColor(Quark quark) {
        return switch (quark) {
            case UP -> "#ff8f8f";
            case DOWN -> "#8fb8ff";
            case STRANGE -> "#9be7a8";
            case CHARM -> "#ffd27f";
            case BOTTOM -> "#c9a6ff";
            case TOP -> "#ffffff";
        };
    }

    /** Montre une partie de la page : {@link #ENERGY}, {@link #CHAIN} ou {@link #UPGRADES}. */
    void select(int index) {
        section = index;
        VBox[] groups = {energyGroup, chainGroup, upgradeGroup};
        for (int i = 0; i < groups.length; i++) {
            groups[i].setVisible(i == index);
            groups[i].setManaged(i == index);
        }
    }

    int section() {
        return section;
    }

    /** L'énergie en mots : « 14 371 », « 2,3 M »… */
    static String energy(double value) {
        return whole(value);
    }

    /**
     * Un nombre entier du collisionneur (énergie, générateurs, quarks) : tel quel sous mille, puis
     * dans la notation choisie, comme partout dans le jeu (version 0.64 ; avant, l'entier restait
     * écrit en entier jusqu'au million).
     */
    static String whole(double value) {
        double floor = Math.floor(value + 1e-9);
        return floor < 1000 ? Format.whole((long) floor) : Format.big(BigNum.of(floor));
    }

    /** Une production, qui peut être fractionnaire : « 0,5 », « 9,13 », puis dans la notation choisie. */
    static String rate(double value) {
        return value < 1000 ? ElementText.number(value) : Format.big(BigNum.of(value));
    }

    /** Les huit générateurs : ce qu'on en a, ce qu'ils produisent, leur prix. Le suivant d'un générateur absent reste caché. */
    private void refreshGenerators() {
        boolean nextShown = false;
        for (ColliderGenerator generator : ColliderGenerator.values()) {
            boolean open = game.isColliderGeneratorOpen(generator);
            // Le premier générateur fermé se montre, grisé, pour dire ce qui vient.
            boolean shown = open || !nextShown;
            if (!open) nextShown = true;
            HBox row = generatorRows.get(generator);
            row.setVisible(shown);
            row.setManaged(shown);
            if (!shown) continue;
            double amount = game.state().colliderGenerators(generator);
            int bought = game.state().colliderGeneratorsBought(generator);
            generatorNames.get(generator).setText((generator.ordinal() + 1) + ". " + generator.label() + " : "
                    + energy(amount) + (bought > 0 && Math.floor(amount) > bought ? " (" + whole(bought) + " achetés)" : ""));
            ColliderGenerator previous = generator.previous();
            generatorOutputs.get(generator).setText(previous == null ? "+" + energy(game.colliderEnergyPerSecond()) + " énergie/s"
                    : "+" + rate(game.colliderGeneratorOutput(generator)) + " " + shortLabel(previous) + "/s");
            Button buy = generatorBuys.get(generator);
            buy.setText(open ? "Acheter : " + energy(game.colliderGeneratorCost(generator)) : "Après le précédent");
            buy.setDisable(!game.canBuyColliderGenerator(generator));
            generatorMaxes.get(generator).setDisable(!game.canBuyColliderGenerator(generator));
            ColliderUpgrade automation = ColliderUpgrade.automationOf(generator);
            generatorAutos.get(generator).setText(game.colliderUpgradeLevel(automation) == 0 ? ""
                    : game.isColliderAutomationRunning(automation) ? "auto" : "auto (pause)");
        }
    }

    private static String shortLabel(ColliderGenerator generator) {
        String label = generator.label();
        int space = label.indexOf(' ');
        return (space < 0 ? label : label.substring(0, space)).toLowerCase(Locale.ROOT);
    }

    /** Les saveurs : leur part des collisions si elles sont ouvertes, sinon ce qui les ouvre. */
    private void refreshFlavors() {
        boolean collisions = game.isCollisionsUnlocked();
        flavorsTitle.setText(collisions ? "Ce que peut donner une collision"
                : Detail.shown() ? "Pas encore de collisions : elles s'ouvrent dans « Améliorations », avec le huitième générateur"
                : "Pas encore de collisions");
        flavors.setVisible(collisions);
        flavors.setManaged(collisions);
        boolean nextShown = false;
        for (Quark quark : Quark.values()) {
            Label label = flavorLabels.get(quark);
            boolean open = game.isQuarkOpen(quark);
            boolean shown = open || !nextShown;
            if (!open) nextShown = true;
            label.setVisible(shown);
            label.setManaged(shown);
            if (!shown) continue;
            String color = quarkColor(quark);
            if (open) {
                double share = game.quarkShare(quark) * 100;
                label.setText(quark.symbol() + "  " + quark.label() + " : " + (share >= 1 ? Math.round(share) : String.valueOf(Math.round(share * 10) / 10.0).replace('.', ',')) + " %");
                label.setStyle("-fx-font-size: 12px; -fx-padding: 3 10; -fx-background-radius: 10; -fx-border-radius: 10;"
                        + " -fx-text-fill: " + color + "; -fx-border-color: " + color + "; -fx-background-color: #101c2b;");
            } else {
                CollisionTier tier = CollisionTier.of(quark);
                label.setText(quark.symbol() + "  " + quark.label() + (Detail.shown() ? " : il faut " + opening(tier) : " : fermé"));
                label.setStyle("-fx-font-size: 12px; -fx-padding: 3 10; -fx-background-radius: 10; -fx-border-radius: 10;"
                        + " -fx-text-fill: #6f8296; -fx-border-color: #2c3a4c; -fx-border-style: dashed; -fx-background-color: transparent;");
            }
        }
    }

    /** Ce qui ouvre une énergie : son amélioration, et son prix. */
    private String opening(CollisionTier tier) {
        ColliderUpgrade upgrade = tier.upgrade();
        return "l'amélioration « " + upgrade.label() + " » (" + energy(upgrade.cost(0)) + " énergie)";
    }

    private String quarksText() {
        StringBuilder quarks = new StringBuilder("Quarks :");
        for (Quark quark : Quark.values()) {
            if (quark == Quark.TOP) continue;
            quarks.append("   ").append(quark.symbol()).append(' ').append(whole(game.quarks(quark)));
        }
        return quarks.toString();
    }

    /** Recopie l'état du collisionneur ; appelé à chaque image tant que la page est affichée. */
    void refresh() {
        energyLabel.setText("Énergie : " + energy(game.colliderEnergy())
                + "   (+" + energy(game.colliderEnergyPerSecond()) + " par seconde)");
        intro.setVisible(Detail.shown());
        intro.setManaged(Detail.shown());
        if (section == ENERGY) {
            refreshGenerators();
            refreshFlavors();
            quarksLabel.setText(quarksText());
            topButton.setVisible(game.hasTop());
            topButton.setManaged(game.hasTop());
            if (game.hasTop()) {
                topButton.setText(Detail.shown()
                        ? "Quark top ! Le lancer dans les générateurs : particules ×" + (int) Game.OVERHEAT_FACTOR
                                + " pendant " + Format.wait(game.overheatSeconds()) + " (il se désintègre dans " + Format.wait(game.topLeft()) + ")"
                        : "Quark top ! Particules ×" + (int) Game.OVERHEAT_FACTOR + " (encore " + Format.wait(game.topLeft()) + ")");
            }
            boolean overheat = game.overheatLeft() > 0;
            overheatLabel.setVisible(overheat);
            overheatLabel.setManaged(overheat);
            if (overheat) overheatLabel.setText("Surchauffe : particules ×" + (int) Game.OVERHEAT_FACTOR + ", encore " + Format.wait(game.overheatLeft()));
            boolean hint = !game.isColliderUpgradesUnlocked();
            upgradesHint.setVisible(hint);
            upgradesHint.setManaged(hint);
            upgradesHint.setText(Detail.shown() ? "Avec le huitième générateur, le collisionneur de Planck, s'ouvrent les améliorations du collisionneur."
                    : "Améliorations : au huitième générateur");
        }
        if (section == CHAIN) {
            quarksOnChain.setText(quarksText());
            refreshUpgrades();
            boolean chain = upgradeTitles.get(ColliderUpgrade.Section.CHAIN).isManaged();
            chainNote.setVisible(!chain);
            chainNote.setManaged(!chain);
            chainNote.setText(Detail.shown() ? "La chaîne énergie → particules → atomes → matière noire → espace → molécules s'ouvre ici avec "
                    + "l'amélioration « Collisions », dans « Améliorations » (huitième générateur). Elle se paie en énergie et en quarks."
                    : "La chaîne s'ouvre avec l'amélioration « Collisions »");
        }
        if (section == UPGRADES) refreshUpgrades();
    }

    /** Les améliorations : celles qui s'offrent et celles déjà prises ; les autres restent cachées. */
    private void refreshUpgrades() {
        int hidden = 0;
        for (ColliderUpgrade upgrade : ColliderUpgrade.values()) {
            Card card = upgradeCards.get(upgrade);
            int level = game.colliderUpgradeLevel(upgrade);
            boolean shown = game.isColliderUpgradeOpen(upgrade) || level > 0;
            card.setVisible(shown);
            card.setManaged(shown);
            if (!shown) {
                if (upgrade.section() != ColliderUpgrade.Section.CHAIN) hidden++;
                continue;
            }
            boolean full = level >= upgrade.maxLevel();
            Card.State state;
            String price;
            if (full && upgrade.isPausable()) {
                boolean running = game.isColliderAutomationRunning(upgrade);
                state = running ? Card.State.ON : Card.State.OFF;
                price = running ? (Detail.shown() ? "En marche : un clic le met en pause" : "En marche")
                        : (Detail.shown() ? "En pause : un clic le relance" : "En pause");
            } else if (full) {
                state = Card.State.DONE;
                price = "Au maximum";
            } else {
                state = game.canBuyColliderUpgrade(upgrade) ? Card.State.READY : level > 0 ? Card.State.STARTED : Card.State.WAITING;
                price = energy(game.colliderUpgradeCost(upgrade)) + " énergie"
                        + (upgrade.quark() == null ? "" : " + " + whole(game.colliderUpgradeQuarks(upgrade)) + " " + upgrade.quark().symbol());
            }
            String corner = upgrade.maxLevel() > 1 ? "niv. " + level + "/" + upgrade.maxLevel() : "";
            card.show(state, corner, "", upgrade.label(), upgrade.brief(), upgrade.effect(), price, "");
        }
        // Une famille dont rien ne s'offre encore reste cachée.
        for (ColliderUpgrade.Section family : ColliderUpgrade.Section.values()) {
            boolean any = false;
            for (ColliderUpgrade upgrade : ColliderUpgrade.values()) any |= upgrade.section() == family && upgradeCards.get(upgrade).isManaged();
            upgradeTitles.get(family).setVisible(any);
            upgradeTitles.get(family).setManaged(any);
            upgradeGrids.get(family).setVisible(any);
            upgradeGrids.get(family).setManaged(any);
        }
        upgradesLeft.setVisible(hidden > 0);
        upgradesLeft.setManaged(hidden > 0);
        upgradesLeft.setText((hidden == 1 ? "Encore une amélioration cachée" : "Encore " + hidden + " améliorations cachées")
                + (Detail.shown() ? " : chacune en ouvre d'autres." : ""));
    }

    /** Fait tourner les faisceaux : appelé à chaque image tant que la page est affichée. */
    public void frame(double elapsed) {
        if (section == ENERGY) energyView.frame(elapsed);
    }

    public EnergyView energyView() {
        return energyView;
    }

    /** La carte d'une amélioration : pour les vérifications. */
    public Card upgradeCard(ColliderUpgrade upgrade) {
        return upgradeCards.get(upgrade);
    }
}
