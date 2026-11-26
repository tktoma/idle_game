package idle.ui.automation;

import idle.ui.common.ResourceHead;
import idle.core.Game;
import idle.core.molecules.Molecule;
import idle.core.collider.ColliderUpgrade;
import idle.core.darkmatter.DarkAutomation;
import idle.core.math.BigNum;
import idle.core.upgrades.Automation;
import idle.core.upgrades.Upgrade;
import idle.ui.GameApp;
import idle.ui.collider.ColliderPane;
import idle.ui.common.Card;
import idle.ui.common.ElementText;
import idle.ui.common.Format;
import idle.ui.common.Labels;
import idle.ui.common.OrderPane;
import idle.ui.common.SubTabs;
import idle.ui.common.TileGrid;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.TextAlignment;

/** Une partie de {@link AutomationPage} (découpée en 0.293 pour rester sous 1 000 lignes) : ses champs, la construction des cartes et l'ordre d'achat. */
public abstract class AutomationPageBase extends VBox {
    // Ce que fournissent les parties suivantes.
    abstract String brief(Automation automation);
    abstract String describe(Automation automation);
    abstract void refreshOrdinary();

    static final String HINT_STYLE = "-fx-font-size: 13px; -fx-text-fill: #8fa3b8;";

    final Game game;

    /** Les sous-onglets : les automatismes de toujours, et ceux des dimensions. */
    public static final int ORDINARY = 0, DIMENSIONS = 1;

    final Button ordinaryTab = new Button("Automatismes");

    final Button dimensionsTab = new Button("Dimensions");

    final HBox subTabs = new HBox(ordinaryTab, dimensionsTab);

    /** « Automatismes » : tout ce que la page montrait avant la version 0.150. */
    final VBox ordinary = new VBox(12);

    final ScrollPane ordinaryScroll = new ScrollPane(ordinary);

    final DimensionAutomationPane dimensions;

    final ScrollPane dimensionsScroll;

    int selected = ORDINARY;

    /** Vrai dans la partie secondaire : les anciens onglets sont cachés, la page ne montre que les dimensions. */
    boolean oldTabsHidden;

    /**
     * Le titre de l'onglet, au centre du bandeau (version 0.315 ; les atomes y étaient depuis la 0.311, le titre en tête
     * de la page avant).
     */
    final ResourceHead head = ResourceHead.title("Automatismes", "#9be7a8");

    /** Coupe ou relance tous les automatismes d'un geste, et le dit tant qu'ils sont coupés. */
    final Button pauseButton = new Button();

    /** Version 0.126 : la coupure générale tient d'une remise à zéro à l'autre, comme les autres réglages. */
    static final String PAUSE_NOTE = "Tous les automatismes sont coupés. Chacun garde son réglage, et repart quand "
            + "vous les relancez. Cette coupure tient d'une remise à zéro à l'autre ; seul « Recommencer de zéro » la lève. Le verrou de "
            + "l'appui, lui, continue : ce n'est pas un automatisme.";

    // Les automatismes ordinaires.
    final Label ordinaryHint = new Label();

    final Map<Automation, Card> cards = new LinkedHashMap<>();

    final Map<Automation, Card> cadences = new LinkedHashMap<>();

    /** L'achat en lot de chaque automatisme qui achète, payé en matière noire. */
    final Map<Automation, Card> batches = new LinkedHashMap<>();

    final Map<Automation, HBox> rows = new LinkedHashMap<>();

    /** Ce qui reste à débloquer parmi les automatismes ordinaires. */
    final Label ordinaryNext = new Label();

    // Le seuil de la fusion automatique, réglable une fois débloqué dans l'arbre de matière noire.
    final Button lessGenerators = new Button("−");

    final Button moreGenerators = new Button("+");

    final Label thresholdLabel = new Label();

    final HBox thresholdRow = new HBox(8, lessGenerators, thresholdLabel, moreGenerators);

    // La réserve d'atomes de la synthèse automatique, réglable dès qu'on la possède.
    final Button lessReserve = new Button("−");

    final Button moreReserve = new Button("+");

    final Label reserveLabel = new Label();

    final HBox reserveRow = new HBox(8, lessReserve, reserveLabel, moreReserve);

    /** La largeur d'une ligne d'automatisme à trois cartes : la plus large, 520 + 250 + 200 et deux écarts. */
    static final double ROW_WIDTH = 986;

    /** Les réserves proposées, en atomes ; au-delà (plafond d'atomes levé), elles doublent. */
    static final double[] RESERVES = {0, 5, 10, 15, 20, 25, 30, 40, 50, 60, 80, 100};

    /**
     * Ajouté à la note de la coupure quand elle laisse tourner quelque chose (version 0.147 ; depuis la 0.167, elle arrête
     * aussi les automates du collisionneur).
     */
    static final String UNCUT_NOTE = " Attention : la coupure n'arrête ni le déclencheur automatique du "
            + "collisionneur, ni le tuyau d'arrosage de la graine.";

    // Les automates du collisionneur, son déclencheur automatique, et le tuyau d'arrosage de la graine.
    final Label colliderTitle = new Label("Automates du collisionneur");

    final Map<ColliderUpgrade, Card> colliderCards = new EnumMap<>(ColliderUpgrade.class);

    final TileGrid colliderGrid = new TileGrid(230, 4, 8);

    /** Coupe ou relance d'un geste tous les automates du collisionneur achetés. */
    final Button colliderPauseButton = new Button();

    /** Ce que disaient les lignes du déclencheur et du tuyau (version 0.315 : au survol du titre des automates). */
    String catchText = "";

    String hoseText = "";

    // Les automatismes de matière noire.
    final Label darkTitle = new Label("Automatismes de matière noire");

    // L'automatisme acquis avec l'espace, après un Big Bang : une carte qui sert d'interrupteur.
    final Label spaceTitle = new Label("Automatisme du Big Bang");

    final Card holdCard = new Card(GameApp.BIG_BANG_COLOR);

    final Card moleculesCard = new Card(GameApp.BIG_BANG_COLOR);

    final Card gatherCard = new Card(GameApp.BIG_BANG_COLOR);

    final Card formCard = new Card(GameApp.BIG_BANG_COLOR);

    // La part d'espace que la création automatique laisse libre, réglable dès qu'elle est acquise.
    final Button lessSpace = new Button("−");

    final Button moreSpace = new Button("+");

    final Label spaceReserveLabel = new Label();

    final HBox spaceReserveRow = new HBox(8, lessSpace, spaceReserveLabel, moreSpace);

    /** Les parts d'espace proposées à la réserve de la création automatique. */
    static final double[] SPACE_RESERVES = {0, 0.10, 0.25, 0.50, 0.75, 0.90};

    /**
     * Les amas confiés à la création automatique : une pastille par amas, qui le retire d'un clic. Depuis la version
     * 0.315, plus de phrase au-dessus : la règle est au survol des pastilles, et rien ne s'affiche sans amas confié.
     */
    final FlowPane entrustedRow = new FlowPane(6, 6);

    final VBox entrustedBox = new VBox(6, entrustedRow);

    final Map<String, Button> entrustedChips = new LinkedHashMap<>();

    // L'ordre d'achat des deux automatismes de matière noire qui achètent : le moins cher d'abord, ou celui du joueur.
    final OrderPane upgradeOrder;

    final OrderPane automationOrder;

    final TileGrid spaceGrid = new TileGrid(230, 2, 8);

    final Label darkHint = new Label();

    final Map<DarkAutomation, Card> darkCards = new LinkedHashMap<>();

    final TileGrid darkGrid = new TileGrid(230, 2, 8);

    /** Ce qui reste à débloquer parmi les automatismes de matière noire. */
    final Label darkNext = new Label();

    public AutomationPageBase(Game game) {
        super(6);
        this.game = game;
        setAlignment(Pos.TOP_CENTER);
        setPadding(new Insets(10, 0, 0, 0));
        ordinary.setAlignment(Pos.TOP_CENTER);
        ordinary.setPadding(new Insets(24));

        ordinaryHint.setText("Chaque automatisme s'achète une fois, en atomes, puis s'active ou se coupe d'un clic. "
                + "Il agit à intervalles réguliers : accélérez sa cadence en atomes jusqu'à son maximum ; "
                + "au-delà, seuls les éléments du tableau périodique réduisent encore le délai.");
        wrapped(ordinaryHint, HINT_STYLE);
        pauseButton.setFocusTraversable(false);
        pauseButton.setOnAction(event -> {
            game.setAutomationPaused(!game.isAutomationPaused());
            refresh();
        });
        ordinary.getChildren().add(pauseButton);
        ordinary.getChildren().add(ordinaryHint);

        // Une ligne par automatisme du catalogue : en ajouter un dans core suffit à le faire apparaître.
        for (Automation automation : game.automations()) {
            Card card = new Card(GameApp.AUTOMATION_COLOR);
            card.setPrefWidth(400);
            card.setMaxWidth(520);
            HBox.setHgrow(card, Priority.ALWAYS);
            card.setOnAction(() -> {
                if (game.ownsAutomation(automation.id())) {
                    game.setAutomationEnabled(automation.id(), !game.isAutomationEnabled(automation.id()));
                } else {
                    game.buyAutomation(automation.id());
                }
                refresh();
            });
            cards.put(automation, card);

            // La cadence se paie en atomes : sa carte en a la couleur.
            Card cadence = new Card(GameApp.ATOMS_COLOR);
            cadence.setPrefWidth(250);
            cadence.setMaxWidth(250);
            cadence.setOnAction(() -> {
                game.speedUpAutomation(automation.id());
                refresh();
            });
            cadences.put(automation, cadence);

            HBox row = new HBox(8, card, cadence);
            // L'achat en lot se paie en matière noire : sa carte en a la couleur. Seuls ceux qui achètent en ont une.
            if (automation.kind() == Automation.Kind.UPGRADE) {
                Card batch = new Card(GameApp.DARK_MATTER_COLOR);
                batch.setPrefWidth(200);
                batch.setMaxWidth(200);
                batch.setOnAction(() -> {
                    game.buyAutomationBatch(automation.id());
                    refresh();
                });
                batches.put(automation, batch);
                row.getChildren().add(batch);
            }
            // Version 0.315 : toutes les lignes ont la largeur de celles à trois cartes, et partent de la gauche : la
            // fusion et la synthèse, sans achat en lot, s'alignent sur les autres au lieu de se centrer.
            row.setAlignment(Pos.CENTER_LEFT);
            row.setMaxWidth(ROW_WIDTH);
            row.setFillHeight(true);     // les deux cartes ont la même hauteur, quelle qu'elle soit
            rows.put(automation, row);
            ordinary.getChildren().add(row);
            // Le réglage du seuil vient juste sous l'automatisme de fusion.
            if (automation.kind() == Automation.Kind.FUSION) ordinary.getChildren().add(thresholdRow);
            // Et celui de la réserve, sous la synthèse automatique.
            if (automation.kind() == Automation.Kind.SYNTHESIS) ordinary.getChildren().add(reserveRow);
        }
        reserveRow.setAlignment(Pos.CENTER);
        wrapped(reserveLabel, "-fx-font-size: 13px; -fx-text-fill: #ffe9c2;");
        String reserveStyle = "-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 2 12; -fx-cursor: hand;"
                + " -fx-text-fill: #ffd27f; -fx-background-color: #2a2113; -fx-background-radius: 6;"
                + " -fx-border-color: #ffd27f; -fx-border-radius: 6;";
        lessReserve.setStyle(reserveStyle);
        moreReserve.setStyle(reserveStyle);
        lessReserve.setFocusTraversable(false);
        moreReserve.setFocusTraversable(false);
        lessReserve.setOnAction(event -> {
            game.setSynthesisReserve(BigNum.of(reserveStep(game.synthesisReserve().toDouble(), false)));
            refresh();
        });
        moreReserve.setOnAction(event -> {
            game.setSynthesisReserve(BigNum.of(reserveStep(game.synthesisReserve().toDouble(), true)));
            refresh();
        });
        thresholdRow.setAlignment(Pos.CENTER);
        thresholdLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #c9a6ff;");
        String stepStyle = "-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 2 12; -fx-cursor: hand;"
                + " -fx-text-fill: #c9a6ff; -fx-background-color: #1c1630; -fx-background-radius: 6;"
                + " -fx-border-color: #c9a6ff; -fx-border-radius: 6;";
        lessGenerators.setStyle(stepStyle);
        moreGenerators.setStyle(stepStyle);
        lessGenerators.setFocusTraversable(false);
        moreGenerators.setFocusTraversable(false);
        // Un clic déplace le seuil d'un groupe de générateurs : 10, 20, 30…
        lessGenerators.setOnAction(event -> {
            game.setFusionThreshold(game.fusionThreshold() - game.generatorsPerAtom());
            refresh();
        });
        moreGenerators.setOnAction(event -> {
            game.setFusionThreshold(game.fusionThreshold() + game.generatorsPerAtom());
            refresh();
        });
        wrapped(ordinaryNext, "-fx-font-size: 13px; -fx-text-fill: #ffe9c2;");
        ordinary.getChildren().add(ordinaryNext);

        // Les automatismes de matière noire : une carte chacun, qui sert d'interrupteur.
        darkTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-padding: 16 0 0 0; -fx-text-fill: "
                + GameApp.DARK_MATTER_COLOR + ";");
        darkHint.setText("Ils font ce que les automatismes ordinaires laissent au joueur, et ne coûtent rien. "
                + "Aucun ne fait le travail d'un automatisme ordinaire : le premier les offre, les "
                + "autres achètent ou font exploser. Attention : l'explosion automatique n'attend pas que vous ayez "
                + "dépensé vos particules et vos atomes dans l'arbre.");
        wrapped(darkHint, HINT_STYLE);
        ordinary.getChildren().add(darkTitle);
        ordinary.getChildren().add(darkHint);
        for (DarkAutomation automation : game.darkAutomations()) {
            Card card = new Card(GameApp.DARK_MATTER_COLOR);
            card.setOnAction(() -> {
                game.setDarkAutomationEnabled(automation.id(), !game.isDarkAutomationEnabled(automation.id()));
                refresh();
            });
            darkCards.put(automation, card);
            darkGrid.add(card);
            darkGrid.show(card, false);
        }
        darkGrid.setMaxWidth(778);       // la largeur d'une carte ordinaire et de sa cadence réunies
        ordinary.getChildren().add(darkGrid);
        upgradeOrder = new OrderPane("Améliorations en atomes", "la moins chère d'abord",
                game::isAtomUpgradeOrdered, game::setAtomUpgradeOrdered, this::upgradeEntries,
                (id, direction) -> game.moveAtomUpgrade(id, direction), this::refresh);
        automationOrder = new OrderPane("Achat des automatismes", "le moins cher d'abord",
                game::isAutomationOrdered, game::setAutomationOrdered, this::automationEntries,
                (id, direction) -> game.moveAutomation(id, direction), this::refresh);
        ordinary.getChildren().add(upgradeOrder);
        ordinary.getChildren().add(automationOrder);
        wrapped(darkNext, "-fx-font-size: 13px; -fx-text-fill: #c9bfe0;");
        ordinary.getChildren().add(darkNext);

        // L'appui automatique : acquis dans les améliorations du Big Bang, réglé ici.
        spaceTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-padding: 16 0 0 0; -fx-text-fill: "
                + GameApp.BIG_BANG_COLOR + ";");
        holdCard.setOnAction(() -> {
            game.setAutoHoldEnabled(!game.isAutoHoldEnabled());
            refresh();
        });
        spaceGrid.add(holdCard);
        // La création automatique des molécules : acquise avec un palier de Big Bang, réglée ici.
        moleculesCard.setOnAction(() -> {
            game.setMoleculeAutomationEnabled(!game.isMoleculeAutomationEnabled());
            refresh();
        });
        spaceGrid.add(moleculesCard);
        // Le rassemblement et la formation automatiques : deux améliorations d'espace, réglées ici.
        gatherCard.setOnAction(() -> {
            game.setAutoGatherEnabled(!game.isAutoGatherEnabled());
            refresh();
        });
        spaceGrid.add(gatherCard);
        formCard.setOnAction(() -> {
            game.setAutoFormEnabled(!game.isAutoFormEnabled());
            refresh();
        });
        spaceGrid.add(formCard);
        spaceGrid.setMaxWidth(778);
        ordinary.getChildren().add(spaceTitle);
        ordinary.getChildren().add(spaceGrid);
        spaceReserveRow.setAlignment(Pos.CENTER);
        wrapped(spaceReserveLabel, "-fx-font-size: 13px; -fx-text-fill: #cfe8ff;");
        String spaceStyle = "-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 2 12; -fx-cursor: hand;"
                + " -fx-text-fill: " + GameApp.BIG_BANG_COLOR + "; -fx-background-color: #101c2b; -fx-background-radius: 6;"
                + " -fx-border-color: " + GameApp.BIG_BANG_COLOR + "; -fx-border-radius: 6;";
        lessSpace.setStyle(spaceStyle);
        moreSpace.setStyle(spaceStyle);
        lessSpace.setFocusTraversable(false);
        moreSpace.setFocusTraversable(false);
        lessSpace.setOnAction(event -> {
            game.setAutoMoleculeReserve(spaceReserveStep(game.autoMoleculeReserve(), false));
            refresh();
        });
        moreSpace.setOnAction(event -> {
            game.setAutoMoleculeReserve(spaceReserveStep(game.autoMoleculeReserve(), true));
            refresh();
        });
        ordinary.getChildren().add(spaceReserveRow);
        // Les plafonds par état : une liste par état de la matière, trois depuis la 0.178.
        capsRow.setAlignment(Pos.CENTER);
        capsRow.setMaxWidth(780);
        for (Molecule.State matter : Molecule.State.values()) {
            ComboBox<String> choice = new ComboBox<>();
            choice.getItems().setAll(CAP_CHOICES);
            choice.setVisibleRowCount(CAP_CHOICES.size());
            choice.setFocusTraversable(false);
            choice.setStyle("-fx-font-size: 12px;");
            choice.setOnAction(event -> {
                game.setStateCap(matter, capOf(choice.getValue()));
                refresh();
            });
            Label name = new Label();
            name.setStyle("-fx-font-size: 12px; -fx-text-fill: " + GameApp.BIG_BANG_COLOR + ";");
            capChoices.put(matter, choice);
            capNames.put(matter, name);
            HBox one = new HBox(6, name, choice);
            one.setAlignment(Pos.CENTER);
            capsRow.getChildren().add(one);
        }
        VBox caps = new VBox(6, capsRow);
        caps.setAlignment(Pos.CENTER);
        capsBox = caps;
        ordinary.getChildren().add(caps);
        // Les amas confiés : nommés ici, retirés d'un clic ; ils se confient dans les États de la matière du Big Bang.
        entrustedRow.setAlignment(Pos.CENTER);
        entrustedRow.setMaxWidth(780);
        entrustedBox.setAlignment(Pos.CENTER);
        ordinary.getChildren().add(entrustedBox);

        // Les automates du collisionneur : une carte chacun, qui l'achète puis le met en pause ou le relance.
        colliderTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-padding: 16 0 0 0; -fx-text-fill: "
                + ColliderPane.COLOR + ";");
        for (ColliderUpgrade upgrade : ColliderUpgrade.values()) {
            if (!upgrade.isPausable()) continue;
            Card card = new Card(ColliderPane.COLOR);
            card.setOnAction(() -> {
                if (game.colliderUpgradeLevel(upgrade) > 0) game.toggleColliderAutomation(upgrade);
                else game.buyColliderUpgrade(upgrade);
                refresh();
            });
            colliderCards.put(upgrade, card);
            colliderGrid.add(card);
        }
        colliderGrid.setMaxWidth(778);
        colliderPauseButton.setFocusTraversable(false);
        colliderPauseButton.setOnAction(event -> {
            game.setColliderAutomationsPaused(!game.anyColliderAutomationPaused());
            refresh();
        });
        ordinary.getChildren().addAll(colliderTitle, colliderPauseButton, colliderGrid);

        // Chaque sous-page défile quand la fenêtre est trop basse pour toutes ses cartes ; la barre reste en haut.
        dimensions = new DimensionAutomationPane(game);
        dimensionsScroll = new ScrollPane(dimensions);
        Labels.transparent(ordinaryScroll);
        Labels.transparent(dimensionsScroll);
        StackPane pages = new StackPane(ordinaryScroll, dimensionsScroll);
        pages.setAlignment(Pos.TOP_CENTER);
        VBox.setVgrow(pages, Priority.ALWAYS);
        SubTabs.wire(true, this::select, this::refresh, ordinaryTab, dimensionsTab);
        subTabs.setAlignment(Pos.CENTER);
        getChildren().addAll(subTabs, pages);
        select(ORDINARY);
    }

    /** La barre des sous-onglets : la disposition à gauche la place en haut de la fenêtre. */
    public HBox subTabs() { return subTabs; }

    /** Choisit un sous-onglet : les automatismes de toujours, ou ceux des dimensions. */
    public void select(int index) {
        selected = index == DIMENSIONS ? DIMENSIONS : ORDINARY;
        ordinaryScroll.setVisible(selected == ORDINARY);
        dimensionsScroll.setVisible(selected == DIMENSIONS);
        ordinaryTab.setStyle(SubTabs.style(selected == ORDINARY, GameApp.AUTOMATION_COLOR));
        dimensionsTab.setStyle(SubTabs.style(selected == DIMENSIONS, GameApp.AUTOMATION_COLOR));
    }

    public int selected() { return selected; }

    /** Les sous-onglets qui se montrent : « Automatismes » hors de la partie secondaire, « Dimensions » avec la bulle. */
    List<Integer> shownTabs() {
        List<Integer> shown = new ArrayList<>();
        if (!oldTabsHidden) shown.add(ORDINARY);
        if (game.isDimensionAutomationUnlocked()) shown.add(DIMENSIONS);
        return shown;
    }

    /** Passe au sous-onglet suivant ou précédent, en boucle, parmi ceux qui se montrent. */
    public void step(int direction) {
        List<Integer> shown = shownTabs();
        if (shown.isEmpty()) return;
        int at = Math.max(0, shown.indexOf(selected));
        select(shown.get(Math.floorMod(at + direction, shown.size())));
        refresh();
    }

    /**
     * Dit si la partie secondaire cache les anciens onglets : la page ne montre alors que les automatismes des
     * dimensions, qui servent dans l'onglet Univers.
     */
    public void setOldTabsHidden(boolean hidden) { oldTabsHidden = hidden; }

    /** La sous-page des automatismes des dimensions : pour les vérifications. */
    public DimensionAutomationPane dimensionsPane() { return dimensions; }

    public Button ordinaryTab() { return ordinaryTab; }

    public Button dimensionsTab() { return dimensionsTab; }

    /**
     * Vrai s'il y a quelque chose que « Couper tous les automatismes » arrête : un automatisme ordinaire, de matière
     * noire ou du Big Bang, ou un automatisme des dimensions acheté.
     */
    public static boolean anyToPause(Game game) {
        return game.hasAnyAutomation() || game.dimensionAutomationsOwned() > 0;
    }

    /** Les choix d'un plafond : sans plafond, puis de 90 % à 5 %. */
    static final List<String> CAP_CHOICES = List.of("sans plafond", "90 %", "80 %", "70 %", "60 %", "50 %",
            "40 %", "30 %", "20 %", "10 %", "5 %");

    /** Le plafond d'un choix de la liste : 1 sans plafond. */
    static double capOf(String choice) {
        if (choice == null || !choice.endsWith("%")) return 1;
        return Integer.parseInt(choice.replace("%", "").trim()) / 100.0;
    }

    /** Le choix de la liste le plus proche d'un plafond. */
    static String choiceOf(double cap) {
        if (cap >= 1) return CAP_CHOICES.get(0);
        String best = CAP_CHOICES.get(1);
        for (String choice : CAP_CHOICES) if (Math.abs(capOf(choice) - cap) < Math.abs(capOf(best) - cap)) best = choice;
        return best;
    }

    final FlowPane capsRow = new FlowPane(14, 6);

    final Map<Molecule.State, ComboBox<String>> capChoices = new EnumMap<>(Molecule.State.class);

    final Map<Molecule.State, Label> capNames = new EnumMap<>(Molecule.State.class);

    VBox capsBox;

    /** Les pastilles des amas confiés, par identifiant de molécule : pour les vérifications. */
    public Map<String, Button> entrustedChips() { return Collections.unmodifiableMap(entrustedChips); }

    /** La carte du rassemblement automatique : pour les vérifications. */
    public Card gatherCard() { return gatherCard; }

    /** La carte de la formation automatique : pour les vérifications. */
    public Card formCard() { return formCard; }

    /** L'ordre d'achat des améliorations en atomes : pour les vérifications. */
    public OrderPane upgradeOrder() { return upgradeOrder; }

    /** L'ordre d'achat des automatismes : pour les vérifications. */
    public OrderPane automationOrder() { return automationOrder; }

    /** La part d'espace suivante ou précédente parmi celles que la réserve propose. */
    static double spaceReserveStep(double share, boolean up) {
        if (up) {
            for (double step : SPACE_RESERVES) {
                if (step > share + 1e-9) return step;
            }
            return SPACE_RESERVES[SPACE_RESERVES.length - 1];
        }
        for (int i = SPACE_RESERVES.length - 1; i >= 0; i--) {
            if (SPACE_RESERVES[i] < share - 1e-9) return SPACE_RESERVES[i];
        }
        return 0;
    }

    /** Les améliorations en atomes dans l'ordre du joueur ; finie, une ligne est au maximum. */
    List<OrderPane.Entry> upgradeEntries() {
        List<OrderPane.Entry> entries = new ArrayList<>();
        for (Upgrade upgrade : game.atomUpgradeOrder()) {
            entries.add(new OrderPane.Entry(upgrade.id(), upgrade.name(), game.isMaxed(upgrade.id())));
        }
        return entries;
    }

    /** Les automatismes ordinaires dans l'ordre du joueur ; finie, une ligne est achetée et à sa cadence maximale. */
    List<OrderPane.Entry> automationEntries() {
        List<OrderPane.Entry> entries = new ArrayList<>();
        for (Automation automation : game.automationOrder()) {
            String id = automation.id();
            entries.add(new OrderPane.Entry(id, automation.name(), game.ownsAutomation(id) && game.isAutomationMaxed(id)));
        }
        return entries;
    }

    /** La carte de l'appui automatique : pour les vérifications. */
    public Card holdCard() { return holdCard; }

    /** La carte de la création automatique des molécules : pour les vérifications. */
    public Card moleculesCard() { return moleculesCard; }

    /**
     * La réserve suivante ou précédente : 0, 5, 10… 100 atomes, puis le double à chaque pas, ce qui
     * ne sert qu'une fois le plafond d'atomes levé.
     */
    static double reserveStep(double reserve, boolean up) {
        double top = RESERVES[RESERVES.length - 1];
        if (up) {
            for (double step : RESERVES) {
                if (step > reserve + 1e-9) return step;
            }
            return Math.min(reserve * 2, 1e300);
        }
        if (reserve > top + 1e-9) return Math.max(top, reserve / 2);
        for (int i = RESERVES.length - 1; i >= 0; i--) {
            if (RESERVES[i] < reserve - 1e-9) return RESERVES[i];
        }
        return 0;
    }

    static void wrapped(Label label, String style) {
        label.setStyle(style);
        label.setWrapText(true);
        label.setTextAlignment(TextAlignment.CENTER);
    }

    /** Affiche ou cache un composant ; caché, il ne laisse pas de trou dans la page. */
    static void show(Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }

    /**
     * Vrai si l'onglet doit être affiché : un automatisme ordinaire, de matière noire ou du Big Bang est
     * débloqué, ou la première matière noire a été obtenue (l'onglet ne disparaît alors plus), ou un automate du
     * collisionneur est acheté, ou la graine a son tuyau, ou la bulle du sommet a ouvert l'automatisation des dimensions.
     */
    public static boolean hasContent(Game game) {
        if (game.isAutomationUnlocked() || game.isDarkMatterUnlocked() || game.isAutoHoldUnlocked()
                || game.isMoleculeAutomationUnlocked() || game.isAutoGatherUnlocked() || game.isAutoFormUnlocked()
                || ownsColliderAutomate(game) || game.hasHose() || game.isDimensionAutomationUnlocked()) return true;
        for (DarkAutomation automation : game.darkAutomations()) {
            if (game.isDarkAutomationUnlocked(automation.id())) return true;
        }
        return false;
    }

    /** Vrai si au moins un automate du collisionneur est acheté. */
    static boolean ownsColliderAutomate(Game game) {
        for (ColliderUpgrade upgrade : ColliderUpgrade.values()) {
            if (upgrade.isPausable() && game.colliderUpgradeLevel(upgrade) > 0) return true;
        }
        return false;
    }

    /**
     * Recopie la partie dans le sous-onglet affiché, seul recopié ; montre « Dimensions » avec la bulle du sommet, et
     * cache « Automatismes » dans la partie secondaire.
     */
    public void refresh() {
        List<Integer> shown = shownTabs();
        show(ordinaryTab, shown.contains(ORDINARY));
        show(dimensionsTab, shown.contains(DIMENSIONS));
        if (!shown.isEmpty() && !shown.contains(selected)) select(shown.get(0));
        if (selected == DIMENSIONS) dimensions.refresh();
        else refreshOrdinary();
    }

    /** Ce que capte le déclencheur automatique à ce niveau. */
    static String catchShare(int level) {
        return switch (level) {
            case 1 -> "un croisement sur cinq";
            case 2 -> "un croisement sur trois";
            default -> "tous les croisements";
        };
    }

    /** Une quantité d'énergie du collisionneur, comme dans son onglet. */
    static String energy(double value) { return Format.big(BigNum.of(value)); }

    /** Ce que fait un automatisme de matière noire, en quelques mots : la ligne de sa carte. */
    static String brief(DarkAutomation automation) {
        return switch (automation.kind()) {
            case GRANT_AUTOMATIONS -> "Offre les automatismes au départ";
            case ATOM_UPGRADES -> "Achète les améliorations en atomes";
            case AUTOMATIONS -> "Achète automatismes et cadences";
            case EXPLOSION -> "Fait exploser le tableau";
            case DARK_TREE -> "Achète l'arbre de matière noire";
        };
    }

    /** Ce que fait un automatisme de matière noire, en une phrase. */
    static String describe(DarkAutomation automation) {
        return switch (automation.kind()) {
            case GRANT_AUTOMATIONS -> "Offre les automatismes ordinaires au début de chaque partie, sans attendre "
                    + Format.count(Game.UNLOCK_TOTAL_ATOMS) + " atomes créés ni payer. La synthèse automatique suit, une fois "
                    + "les autres à leur cadence maximale et le tableau périodique ouvert, sans attendre un élément unique. "
                    + "Il agit aussi pendant les défis";
            case ATOM_UPGRADES -> "Achète les améliorations en atomes, la moins chère d'abord";
            case AUTOMATIONS -> "Achète les automatismes ordinaires et leur cadence, le moins cher d'abord";
            case EXPLOSION -> "Fait exploser le tableau périodique dès que c'est possible";
            case DARK_TREE -> "Achète les cases de l'arbre de matière noire et les améliorations de la boutique qui "
                    + "ont un dernier niveau, dans l'ordre de l'arbre. Une case sans dernier niveau (Densité, Noyau lourd) "
                    + "est prise une fois pour ouvrir la suivante, puis chaque fois qu'un niveau coûte au plus "
                    + ElementText.percent(Game.TREE_REPEAT_SHARE) + " de ce que possède sa branche : le Big Bang n'en "
                    + "souffre pas. Les améliorations sans limite de la boutique et les bifurcations restent à votre main (les liaisons et les accords, eux, sont pris dès que leurs voies les ouvrent) : "
                    + "c'est vous qui choisissez votre voie, et l'automatisme ne va pas au-delà tant que vous ne l'avez pas fait. Rien n'est acheté "
                    + "pendant un défi";
        };
    }

    /** 4.0 → « 4 s », 0.5 → « 0.5 s », 1.333 → « 1.33 s ». */
    public static String seconds(double value) { return ElementText.number(value) + " s"; }
}
