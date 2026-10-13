package idle.ui.collider;

import idle.core.Game;
import idle.core.collider.ColliderGenerator;
import idle.core.collider.ColliderMilestone;
import idle.core.collider.Collision;
import idle.core.collider.CollisionTier;
import idle.core.collider.Hadron;
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
 * Le collisionneur de quarks, sous-page de la matière noire : l'énergie que donne la production,
 * les collisions à lancer, la barre de résonance de chaque collision en cours, les quarks, et les
 * hadrons à former. Rien de ce qui s'y gagne ne se perd à l'explosion ni au Big Bang.
 */
public final class ColliderPane extends VBox {

    public static final String COLOR = "#9ad0ff";
    private static final String BUTTON_STYLE = "-fx-font-size: 13px; -fx-padding: 6 14; -fx-cursor: hand; -fx-background-radius: 6;"
            + " -fx-border-radius: 6; -fx-text-fill: " + COLOR + "; -fx-background-color: #101c2b; -fx-border-color: " + COLOR + ";";
    /** Largeur de la barre de résonance, en caractères. */
    private static final int BAR = 25;

    private final Game game;
    private final Runnable changed;
    private final Label intro = new Label();
    private final Label energyLabel = new Label();
    private final FlowPane tiers = new FlowPane(8, 8);
    private final Map<CollisionTier, Button> tierButtons = new EnumMap<>(CollisionTier.class);
    private final VBox slots = new VBox(6);
    private final List<Label> slotLabels = new ArrayList<>();
    private final List<Button> slotButtons = new ArrayList<>();
    private final Label quarksLabel = new Label();
    private final Button topButton = new Button();
    private final Label overheatLabel = new Label();
    private final TileGrid hadronGrid = new TileGrid(230, 3, 8);
    private final Map<Hadron, Card> hadronCards = new EnumMap<>(Hadron.class);
    private final Label milestonesTitle = new Label("Paliers du collisionneur");
    private final Map<ColliderMilestone, Label> milestoneLabels = new EnumMap<>(ColliderMilestone.class);
    private final Button autoButton = new Button();
    private final ColliderView view;
    private final EnergyView energyView;
    private final Map<ColliderGenerator, Label> generatorNames = new EnumMap<>(ColliderGenerator.class);
    private final Map<ColliderGenerator, Label> generatorOutputs = new EnumMap<>(ColliderGenerator.class);
    private final Map<ColliderGenerator, Button> generatorBuys = new EnumMap<>(ColliderGenerator.class);
    private final Map<ColliderGenerator, Button> generatorMaxes = new EnumMap<>(ColliderGenerator.class);
    private final Map<ColliderGenerator, HBox> generatorRows = new EnumMap<>(ColliderGenerator.class);
    /** Les trois parties de la page, une par sous-onglet : l'énergie, les collisions, les hadrons. */
    private final VBox energyGroup = new VBox(10);
    private final VBox collisionGroup = new VBox(10);
    private final VBox hadronGroup = new VBox(10);
    private int section = 0;

    ColliderPane(Game game, Runnable changed) {
        super(10);
        this.game = game;
        this.changed = changed;
        this.view = new ColliderView(game, slot -> {
            game.resonate(slot);
            changed.run();
        });
        this.energyView = new EnergyView(game, gain -> changed.run());
        setAlignment(Pos.TOP_CENTER);
        setPadding(new Insets(10, 0, 10, 0));

        intro.setText("Les faisceaux croisés donnent l'énergie ; chacun des huit générateurs produit le précédent. "
                + "Un clic quand les faisceaux se croisent capte trois secondes de production d'un coup. L'énergie paie "
                + "les générateurs et les collisions ; les collisions rapportent des quarks, les quarks forment des "
                + "hadrons, dont les bonus sont définitifs : ni l'explosion ni le Big Bang ne les reprennent.");
        note(intro, "#8fa3b8");
        energyLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + COLOR + ";");
        StableHeight.of(energyLabel);

        tiers.setAlignment(Pos.CENTER);
        tiers.setMaxWidth(780);
        for (CollisionTier tier : CollisionTier.values()) {
            Button button = new Button();
            button.setStyle(BUTTON_STYLE);
            button.setFocusTraversable(false);
            button.setOnAction(event -> {
                game.startCollision(tier);
                changed.run();
            });
            tierButtons.put(tier, button);
            tiers.getChildren().add(button);
        }

        slots.setAlignment(Pos.CENTER);
        for (int i = 0; i < 5; i++) {
            int slot = i;
            Label label = new Label();
            label.setStyle("-fx-font-size: 13px; -fx-font-family: monospace; -fx-text-fill: #cfe8ff;");
            label.setMinWidth(300);
            Button button = new Button("Résonance !");
            button.setStyle(BUTTON_STYLE);
            button.setFocusTraversable(false);
            button.setMinWidth(220);
            button.setOnAction(event -> {
                game.resonate(slot);
                changed.run();
            });
            HBox row = new HBox(10, label, button);
            row.setAlignment(Pos.CENTER);
            slotLabels.add(label);
            slotButtons.add(button);
            slots.getChildren().add(row);
        }

        quarksLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #ffe9c2;");
        topButton.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-padding: 8 20; -fx-cursor: hand; -fx-background-radius: 6;"
                + " -fx-border-radius: 6; -fx-text-fill: #10151f; -fx-background-color: #ffd27f; -fx-border-color: #ffffff;");
        topButton.setFocusTraversable(false);
        topButton.setOnAction(event -> {
            game.useTop();
            changed.run();
        });
        overheatLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #ffd27f;");

        hadronGrid.setMaxWidth(780);
        for (Hadron hadron : Hadron.values()) {
            Card card = new Card(COLOR);
            card.setRepeats(true);
            card.setOnAction(() -> {
                game.formHadron(hadron);
                changed.run();
            });
            hadronCards.put(hadron, card);
            hadronGrid.add(card);
        }

        autoButton.setStyle(BUTTON_STYLE);
        autoButton.setFocusTraversable(false);
        autoButton.setOnAction(event -> {
            game.setColliderAuto(!game.state().colliderAuto());
            changed.run();
        });
        milestonesTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: " + COLOR + ";");
        VBox milestones = new VBox(4, milestonesTitle);
        milestones.setAlignment(Pos.CENTER);
        milestones.setMaxWidth(760);
        for (ColliderMilestone milestone : ColliderMilestone.values()) {
            Label label = new Label();
            label.setWrapText(true);
            label.setMaxWidth(760);
            milestoneLabels.put(milestone, label);
            milestones.getChildren().add(label);
        }

        CanvasPane viewPane = CanvasPane.filling(view);
        viewPane.setPrefHeight(210);
        viewPane.setMinHeight(210);
        viewPane.setMaxHeight(210);
        viewPane.setMaxWidth(780);
        CanvasPane energyPane = CanvasPane.filling(energyView);
        energyPane.setPrefHeight(170);
        energyPane.setMinHeight(170);
        energyPane.setMaxHeight(170);
        energyPane.setMaxWidth(780);
        VBox generators = new VBox(4);
        generators.setAlignment(Pos.CENTER);
        for (ColliderGenerator generator : ColliderGenerator.values()) {
            Label name = new Label();
            name.setStyle("-fx-font-size: 13px; -fx-text-fill: #cfe8ff;");
            name.setMinWidth(300);
            Label output = new Label();
            output.setStyle("-fx-font-size: 12px; -fx-text-fill: #8fa3b8;");
            output.setMinWidth(190);
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
            HBox row = new HBox(10, name, output, buy, max);
            row.setAlignment(Pos.CENTER);
            generatorNames.put(generator, name);
            generatorOutputs.put(generator, output);
            generatorBuys.put(generator, buy);
            generatorMaxes.put(generator, max);
            generatorRows.put(generator, row);
            generators.getChildren().add(row);
        }
        for (VBox group : new VBox[] {energyGroup, collisionGroup, hadronGroup}) group.setAlignment(Pos.TOP_CENTER);
        energyGroup.getChildren().addAll(energyPane, generators);
        collisionGroup.getChildren().addAll(tiers, autoButton, viewPane, slots, topButton, overheatLabel);
        hadronGroup.getChildren().addAll(quarksLabel, hadronGrid, milestones);
        getChildren().addAll(intro, energyLabel, energyGroup, collisionGroup, hadronGroup);
        select(0);
    }

    private static void note(Label label, String color) {
        label.setStyle("-fx-font-size: 12px; -fx-text-fill: " + color + ";");
        label.setWrapText(true);
        label.setMaxWidth(760);
        label.setTextAlignment(TextAlignment.CENTER);
    }

    /** Montre une partie de la page : 0 l'énergie, 1 les collisions, 2 les hadrons. */
    void select(int index) {
        section = index;
        VBox[] groups = {energyGroup, collisionGroup, hadronGroup};
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
        return value < 1e6 ? Format.whole((long) Math.floor(value)) : Format.count(BigNum.of(value));
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
                    + energy(amount) + (bought > 0 && Math.floor(amount) > bought ? " (" + bought + " achetés)" : ""));
            ColliderGenerator previous = generator.previous();
            double output = game.colliderGeneratorOutput(generator) + (previous == null ? generator.power() : 0);
            generatorOutputs.get(generator).setText(previous == null ? "+" + energy(output) + " énergie/s"
                    : "+" + ElementText.number(game.colliderGeneratorOutput(generator)) + " " + shortLabel(previous) + "/s");
            Button buy = generatorBuys.get(generator);
            buy.setText(open ? "Acheter : " + energy(game.colliderGeneratorCost(generator)) : "Après le précédent");
            buy.setDisable(!game.canBuyColliderGenerator(generator));
            generatorMaxes.get(generator).setDisable(!game.canBuyColliderGenerator(generator));
        }
    }

    private static String shortLabel(ColliderGenerator generator) {
        String label = generator.label();
        int space = label.indexOf(' ');
        return (space < 0 ? label : label.substring(0, space)).toLowerCase(java.util.Locale.ROOT);
    }

    /** Recopie l'état du collisionneur ; appelé à chaque image tant que la sous-page est affichée. */
    void refresh() {
        energyLabel.setText("Énergie : " + energy(game.colliderEnergy())
                + "   (+" + energy(game.colliderEnergyPerSecond()) + " par seconde)");
        refreshGenerators();
        intro.setVisible(Detail.shown());
        intro.setManaged(Detail.shown());
        for (CollisionTier tier : CollisionTier.values()) {
            Button button = tierButtons.get(tier);
            boolean open = game.isCollisionTierOpen(tier);
            button.setVisible(open);
            button.setManaged(open);
            if (!open) continue;
            button.setText(tier.label() + " : " + energy(tier.cost()) + " énergie, " + Format.wait(tier.seconds()));
            button.setDisable(!game.canStartCollision(tier));
        }
        List<Collision> running = game.state().collisions();
        for (int i = 0; i < slotLabels.size(); i++) {
            boolean exists = i < game.colliderSlots();
            HBox row = (HBox) slots.getChildren().get(i);
            row.setVisible(exists);
            row.setManaged(exists);
            if (!exists) continue;
            Label label = slotLabels.get(i);
            Button button = slotButtons.get(i);
            if (i >= running.size()) {
                label.setText("Place " + (i + 1) + " : libre");
                button.setVisible(false);
                continue;
            }
            Collision collision = running.get(i);
            label.setText(collision.tier().label() + ", encore " + Format.wait(collision.left()));
            button.setVisible(true);
            button.setDisable(collision.resonated());
            button.setText(!collision.resonated() ? "Résonance !"
                    : collision.doubled() ? "Résonance réussie : butin ×2" : "Résonance manquée");
        }
        StringBuilder quarks = new StringBuilder("Quarks :");
        for (Quark quark : Quark.values()) {
            if (quark == Quark.TOP) continue;
            quarks.append("  ").append(quark.symbol()).append(' ').append(game.quarks(quark));
        }
        quarksLabel.setText(quarks.toString());
        topButton.setVisible(game.hasTop());
        topButton.setManaged(game.hasTop());
        if (game.hasTop()) {
            topButton.setText("Quark top ! Le lancer dans les générateurs : particules ×" + (int) Game.OVERHEAT_FACTOR
                    + " pendant " + Format.wait(Game.OVERHEAT_SECONDS) + " (il se désintègre dans " + Format.wait(game.topLeft()) + ")");
        }
        boolean overheat = game.overheatLeft() > 0;
        overheatLabel.setVisible(overheat);
        overheatLabel.setManaged(overheat);
        if (overheat) overheatLabel.setText("Surchauffe : particules ×" + (int) Game.OVERHEAT_FACTOR + ", encore " + Format.wait(game.overheatLeft()));
        refreshMilestones();
        for (Hadron hadron : Hadron.values()) {
            Card card = hadronCards.get(hadron);
            int count = game.hadrons(hadron);
            boolean can = game.canFormHadron(hadron);
            card.show(can ? Card.State.READY : Card.State.WAITING, count > 0 ? String.valueOf(count) : "", "",
                    hadron.label(), recipe(hadron) + " : " + hadron.bonus(), "", can ? "Former" : "", "");
        }
    }

    /** Fait tourner les faisceaux : appelé à chaque image tant que la sous-page est affichée. */
    public void frame(double elapsed) {
        if (section == 0) energyView.frame(elapsed);
        if (section == 1) view.frame(elapsed);
    }

    public EnergyView energyView() {
        return energyView;
    }

    /** Le dessin : pour les vérifications. */
    public ColliderView view() {
        return view;
    }

    /** Les paliers : atteints en plein, le prochain avec sa progression, les autres en attente. */
    private void refreshMilestones() {
        for (ColliderMilestone milestone : ColliderMilestone.values()) {
            Label label = milestoneLabels.get(milestone);
            boolean reached = game.hasColliderMilestone(milestone);
            int[] progress = game.colliderMilestoneProgress(milestone);
            label.setText((reached ? "● " : "○ ") + milestone.label() + (reached ? "" : " (" + progress[0] + "/" + progress[1] + ")")
                    + " — " + milestone.condition() + " : " + Character.toLowerCase(milestone.reward().charAt(0)) + milestone.reward().substring(1) + ".");
            label.setStyle("-fx-font-size: 13px; -fx-text-fill: " + (reached ? "#ffe9c2" : "#8fa3b8") + ";");
        }
        boolean plasma = game.hasColliderMilestone(ColliderMilestone.PLASMA);
        autoButton.setVisible(plasma);
        autoButton.setManaged(plasma);
        if (plasma) autoButton.setText("Relance de la basse énergie : " + (game.state().colliderAuto() ? "oui" : "non"));
    }

    /** « 2 u + 1 d », dans l'ordre des saveurs. */
    static String recipe(Hadron hadron) {
        List<String> parts = new ArrayList<>();
        for (Quark quark : Quark.values()) {
            Integer count = hadron.recipe().get(quark);
            if (count != null) parts.add(count + " " + quark.symbol());
        }
        return String.join(" + ", parts);
    }

    /** La barre de résonance : un curseur qui passe ; la zone gagnante est entre les crochets. */
    static String bar(double phase) {
        char[] cells = new char[BAR];
        int cursor = (int) Math.min(BAR - 1, Math.floor(phase * BAR));
        int low = (int) Math.round((0.5 - Game.RESONANCE_WINDOW / 2) * BAR);
        int high = (int) Math.round((0.5 + Game.RESONANCE_WINDOW / 2) * BAR) - 1;
        for (int i = 0; i < BAR; i++) cells[i] = i >= low && i <= high ? '=' : '·';
        cells[cursor] = '◆';
        return "[" + new String(cells) + "]";
    }
}
