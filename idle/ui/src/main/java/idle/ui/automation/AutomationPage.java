package idle.ui.automation;

import idle.core.Game;
import idle.core.collider.ColliderUpgrade;
import idle.core.darkmatter.DarkAutomation;
import idle.core.math.BigNum;
import idle.core.upgrades.Automation;
import idle.core.upgrades.Upgrade;
import idle.ui.GameApp;
import idle.ui.collider.ColliderPane;
import idle.ui.common.Card;
import idle.ui.common.Detail;
import idle.ui.common.ElementText;
import idle.ui.common.Format;
import idle.ui.common.Labels;
import idle.ui.common.OrderPane;
import idle.ui.common.SubTabs;
import idle.ui.common.TileGrid;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

/**
 * Contenu de l'onglet « Automatisation » : tous les automatismes du jeu au même endroit, chacun
 * n'apparaissant qu'une fois débloqué pour la première fois. Ensuite il reste affiché : si une
 * explosion le reverrouille, sa carte dit ce qu'il faut pour le débloquer de nouveau.
 *
 * <p>D'abord les automatismes ordinaires, une ligne chacun, faite de deux {@link Card}. À gauche,
 * l'automatisme : tant qu'il n'est pas acheté, la carte affiche son prix en atomes et sert à
 * l'acheter ; ensuite elle devient un interrupteur pour le mettre en marche ou le couper. À
 * droite, sa cadence : le délai entre deux actions, et le prix pour le réduire en atomes. La cadence achetée en atomes a un
 * maximum ; au-delà, seuls les éléments du tableau périodique réduisent encore le délai. Ils
 * apparaissent une fois assez d'atomes créés ({@link Game#UNLOCK_TOTAL_ATOMS}) ; la synthèse
 * automatique, elle, attend le premier élément unique du tableau périodique.
 *
 * <p>Ensuite les automatismes de matière noire, qui font ce que les ordinaires laissent au
 * joueur : chacun apparaît quand le joueur a gagné assez de matière noire, puis se met en marche
 * ou se coupe d'un clic. Ils ne coûtent rien, et aucun ne refait le travail d'un automatisme
 * ordinaire : le premier se contente de les offrir au début de chaque partie.
 *
 * <p>Puis ceux du Big Bang (l'appui, la création, le rassemblement et la formation automatiques), avec leurs
 * réglages : réserve d'espace, liste de courses, besoins de matière, plafonds par état.
 *
 * <p>Depuis la version 0.147, tout ce qui agit seul s'y retrouve. Les huit automates du collisionneur, une carte
 * chacun, dès que ses améliorations s'ouvrent : pas encore acheté, la carte dit son prix et l'achète si le
 * collisionneur le propose déjà, sinon elle renvoie au collisionneur ; acheté, elle le met en pause ou le relance.
 * Une ligne dit où en est le déclencheur automatique (un croisement sur cinq, sur trois, puis tous), une autre ce que
 * verse le tuyau d'arrosage de la graine. Ces trois-là ne relèvent pas de la coupure générale : le moteur ne les
 * arrête pas quand « Couper tous les automatismes » est actif, et la note de la coupure le dit.
 *
 * <p>Sous chaque groupe, une ligne dit ce qui reste à découvrir, pour que le joueur sache qu'il
 * y a une suite. Chaque section n'apparaît qu'une fois son système ouvert.
 *
 * <p>L'onglet apparaît dès qu'un automatisme est débloqué, et reste là pour de bon une fois la
 * première matière noire obtenue.
 *
 * <p>Depuis la version 0.150, tout cela forme le sous-onglet « Automatismes » ; la bulle du sommet de l'arbre-monde
 * ouvre le second, « Dimensions » ({@link DimensionAutomationPane}), où s'achètent et se coupent les automatismes qui
 * jouent les dimensions et achètent leurs améliorations. Dans la partie secondaire, où les anciens onglets se cachent,
 * l'onglet ne revient qu'avec eux, et ne montre qu'eux.
 *
 * <p>Les cartes s'en tiennent à quelques mots ; les explications, sur les cartes comme entre
 * elles, n'apparaissent qu'en mode détails ({@link Detail}).
 */
public final class AutomationPage extends VBox {

    private static final String HINT_STYLE = "-fx-font-size: 13px; -fx-text-fill: #8fa3b8;";

    private final Game game;
    /** Les sous-onglets (version 0.150) : les automatismes de toujours, et ceux des dimensions. */
    public static final int ORDINARY = 0, DIMENSIONS = 1;
    private final Button ordinaryTab = new Button("Automatismes");
    private final Button dimensionsTab = new Button("Dimensions");
    private final HBox subTabs = new HBox(ordinaryTab, dimensionsTab);
    /** « Automatismes » : tout ce que la page montrait avant la version 0.150. */
    private final VBox ordinary = new VBox(12);
    private final ScrollPane ordinaryScroll = new ScrollPane(ordinary);
    private final DimensionAutomationPane dimensions;
    private final ScrollPane dimensionsScroll;
    private int selected = ORDINARY;
    /** Vrai dans la partie secondaire : les anciens onglets sont cachés, la page ne montre que les dimensions. */
    private boolean oldTabsHidden;
    private final Label balanceLabel = new Label();
    /** Coupe ou relance tous les automatismes d'un geste, et le dit tant qu'ils sont coupés. */
    private final Button pauseButton = new Button();
    private final Label pauseNote = new Label(PAUSE_NOTE);
    /** Version 0.126 : la coupure générale tient d'une remise à zéro à l'autre, comme les autres réglages. */
    private static final String PAUSE_NOTE = "Tous les automatismes sont coupés. Chacun garde son réglage, et repart quand "
            + "vous les relancez. Cette coupure tient d'une remise à zéro à l'autre ; seul « Recommencer de zéro » la lève. Le verrou de "
            + "l'appui, lui, continue : ce n'est pas un automatisme.";

    // Les automatismes ordinaires.
    private final Label ordinaryHint = new Label();
    private final Map<Automation, Card> cards = new LinkedHashMap<>();
    private final Map<Automation, Card> cadences = new LinkedHashMap<>();
    private final Map<Automation, HBox> rows = new LinkedHashMap<>();
    /** Ce qui reste à débloquer parmi les automatismes ordinaires. */
    private final Label ordinaryNext = new Label();
    // Le seuil de la fusion automatique, réglable une fois débloqué dans l'arbre de matière noire.
    private final Button lessGenerators = new Button("−");
    private final Button moreGenerators = new Button("+");
    private final Label thresholdLabel = new Label();
    private final HBox thresholdRow = new HBox(8, lessGenerators, thresholdLabel, moreGenerators);

    // La réserve d'atomes de la synthèse automatique, réglable dès qu'on la possède.
    private final Button lessReserve = new Button("−");
    private final Button moreReserve = new Button("+");
    private final Label reserveLabel = new Label();
    /** La création automatique suit la liste de courses, ou seulement les amas confiés. */
    private final Button followButton = new Button();
    /** Entretien facultatif des états de matière nécessaires au prochain astre ou au cosmos. */
    private final Button followMatterButton = new Button();
    private final Label followMatterHint = new Label("N'entretient que les substances déjà rassemblées. Respecte votre réserve d'espace et vos plafonds par état. N'est jamais activé sans votre choix.");
    private final VBox followMatterBox = new VBox(5, followMatterButton, followMatterHint);
    private final HBox reserveRow = new HBox(8, lessReserve, reserveLabel, moreReserve);
    /** Les réserves proposées, en atomes ; au-delà (plafond d'atomes levé), elles doublent. */
    private static final double[] RESERVES = {0, 5, 10, 15, 20, 25, 30, 40, 50, 60, 80, 100};

    /** Ajouté à la note de la coupure quand elle laisse tourner quelque chose (version 0.147). */
    private static final String UNCUT_NOTE = " Attention : la coupure n'arrête ni les automates du collisionneur, ni son "
            + "déclencheur automatique, ni le tuyau d'arrosage de la graine. Les automates se mettent en pause un par un, ici "
            + "ou dans le collisionneur.";

    // Les automates du collisionneur (version 0.147), son déclencheur automatique, et le tuyau d'arrosage de la graine.
    private final Label colliderTitle = new Label("Automates du collisionneur");
    private final Label colliderHint = new Label();
    private final Map<ColliderUpgrade, Card> colliderCards = new EnumMap<>(ColliderUpgrade.class);
    private final TileGrid colliderGrid = new TileGrid(230, 4, 8);
    private final Label catchLine = new Label();
    private final Label hoseLine = new Label();

    // Les automatismes de matière noire.
    private final Label darkTitle = new Label("Automatismes de matière noire");
    // L'automatisme acquis avec l'espace, après un Big Bang : une carte qui sert d'interrupteur.
    private final Label spaceTitle = new Label("Automatisme du Big Bang");
    private final Card holdCard = new Card(GameApp.BIG_BANG_COLOR);
    private final Card moleculesCard = new Card(GameApp.BIG_BANG_COLOR);
    private final Card gatherCard = new Card(GameApp.BIG_BANG_COLOR);
    private final Card formCard = new Card(GameApp.BIG_BANG_COLOR);
    // La part d'espace que la création automatique laisse libre, réglable dès qu'elle est acquise.
    private final Button lessSpace = new Button("−");
    private final Button moreSpace = new Button("+");
    private final Label spaceReserveLabel = new Label();
    private final HBox spaceReserveRow = new HBox(8, lessSpace, spaceReserveLabel, moreSpace);
    /** Les parts d'espace proposées à la réserve de la création automatique. */
    private static final double[] SPACE_RESERVES = {0, 0.10, 0.25, 0.50, 0.75, 0.90};
    // L'ordre d'achat des deux automatismes de matière noire qui achètent : le moins cher d'abord, ou celui du joueur.
    private final OrderPane upgradeOrder;
    private final OrderPane automationOrder;
    private final TileGrid spaceGrid = new TileGrid(230, 2, 8);
    private final Label darkHint = new Label();
    private final Map<DarkAutomation, Card> darkCards = new LinkedHashMap<>();
    private final TileGrid darkGrid = new TileGrid(230, 2, 8);
    /** Ce qui reste à débloquer parmi les automatismes de matière noire. */
    private final Label darkNext = new Label();

    public AutomationPage(Game game) {
        super(6);
        this.game = game;
        setAlignment(Pos.TOP_CENTER);
        setPadding(new Insets(10, 0, 0, 0));
        ordinary.setAlignment(Pos.TOP_CENTER);
        ordinary.setPadding(new Insets(24));

        Label title = new Label("Automatismes");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #9be7a8;");
        ordinaryHint.setText("Chaque automatisme s'achète une fois, en atomes, puis s'active ou se coupe d'un clic. "
                + "Il agit à intervalles réguliers : accélérez sa cadence en atomes jusqu'à son maximum ; "
                + "au-delà, seuls les éléments du tableau périodique réduisent encore le délai.");
        wrapped(ordinaryHint, HINT_STYLE);
        balanceLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #ffd27f;");
        pauseButton.setFocusTraversable(false);
        pauseButton.setOnAction(event -> {
            game.setAutomationPaused(!game.isAutomationPaused());
            refresh();
        });
        wrapped(pauseNote, "-fx-font-size: 13px; -fx-text-fill: #ffb4a8;");
        ordinary.getChildren().add(title);
        ordinary.getChildren().add(pauseButton);
        ordinary.getChildren().add(pauseNote);
        ordinary.getChildren().add(ordinaryHint);
        ordinary.getChildren().add(balanceLabel);

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
            row.setAlignment(Pos.CENTER);
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
        followButton.setStyle(spaceStyle);
        followButton.setFocusTraversable(false);
        followButton.setOnAction(event -> {
            game.setAutoFollowList(!game.isAutoFollowingList());
            refresh();
        });
        HBox followRow = new HBox(followButton);
        followRow.setAlignment(Pos.CENTER);
        this.followRow = followRow;
        ordinary.getChildren().add(followRow);
        followMatterBox.setAlignment(Pos.CENTER);
        followMatterButton.setStyle(spaceStyle);
        followMatterButton.setFocusTraversable(false);
        followMatterButton.setOnAction(event -> {
            game.setAutoFollowMatter(!game.isAutoFollowingMatter());
            refresh();
        });
        wrapped(followMatterHint, "-fx-font-size: 11px; -fx-text-fill: #93b4c8;");
        followMatterHint.setMaxWidth(690);
        ordinary.getChildren().add(followMatterBox);
        // Les plafonds par état (version 0.085) : une liste par état de la matière.
        capsTitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #cfe8ff;");
        capsTitle.setWrapText(true);
        capsTitle.setMaxWidth(760);
        capsRow.setAlignment(Pos.CENTER);
        capsRow.setMaxWidth(780);
        for (idle.core.molecules.Molecule.State matter : idle.core.molecules.Molecule.State.values()) {
            javafx.scene.control.ComboBox<String> choice = new javafx.scene.control.ComboBox<>();
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
        VBox caps = new VBox(6, capsTitle, capsRow);
        caps.setAlignment(Pos.CENTER);
        capsBox = caps;
        ordinary.getChildren().add(caps);

        // Les automates du collisionneur : une carte chacun, qui l'achète puis le met en pause ou le relance.
        colliderTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-padding: 16 0 0 0; -fx-text-fill: "
                + ColliderPane.COLOR + ";");
        wrapped(colliderHint, HINT_STYLE);
        colliderHint.setMaxWidth(760);
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
        wrapped(catchLine, "-fx-font-size: 13px; -fx-text-fill: " + ColliderPane.COLOR + ";");
        catchLine.setMaxWidth(760);
        wrapped(hoseLine, "-fx-font-size: 13px; -fx-text-fill: #e6d27a;");
        hoseLine.setMaxWidth(760);
        ordinary.getChildren().addAll(colliderTitle, colliderHint, colliderGrid, catchLine, hoseLine);

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
    public HBox subTabs() {
        return subTabs;
    }

    /** Choisit un sous-onglet : les automatismes de toujours, ou ceux des dimensions. */
    public void select(int index) {
        selected = index == DIMENSIONS ? DIMENSIONS : ORDINARY;
        ordinaryScroll.setVisible(selected == ORDINARY);
        dimensionsScroll.setVisible(selected == DIMENSIONS);
        ordinaryTab.setStyle(SubTabs.style(selected == ORDINARY, GameApp.AUTOMATION_COLOR));
        dimensionsTab.setStyle(SubTabs.style(selected == DIMENSIONS, GameApp.AUTOMATION_COLOR));
    }

    public int selected() {
        return selected;
    }

    /** Les sous-onglets qui se montrent : « Automatismes » hors de la partie secondaire, « Dimensions » avec la bulle. */
    private List<Integer> shownTabs() {
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
    public void setOldTabsHidden(boolean hidden) {
        oldTabsHidden = hidden;
    }

    /** La sous-page des automatismes des dimensions : pour les vérifications. */
    public DimensionAutomationPane dimensionsPane() {
        return dimensions;
    }

    public Button ordinaryTab() {
        return ordinaryTab;
    }

    public Button dimensionsTab() {
        return dimensionsTab;
    }

    /**
     * Vrai s'il y a quelque chose que « Couper tous les automatismes » arrête : un automatisme ordinaire, de matière
     * noire ou du Big Bang, ou un automatisme des dimensions acheté (version 0.150).
     */
    public static boolean anyToPause(Game game) {
        return game.hasAnyAutomation() || game.dimensionAutomationsOwned() > 0;
    }

    /** Les choix d'un plafond : sans plafond, puis de 90 % à 5 %. */
    static final java.util.List<String> CAP_CHOICES = java.util.List.of("sans plafond", "90 %", "80 %", "70 %", "60 %", "50 %",
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

    private final Label capsTitle = new Label();
    private final javafx.scene.layout.FlowPane capsRow = new javafx.scene.layout.FlowPane(14, 6);
    private final java.util.Map<idle.core.molecules.Molecule.State, javafx.scene.control.ComboBox<String>> capChoices =
            new java.util.EnumMap<>(idle.core.molecules.Molecule.State.class);
    private final java.util.Map<idle.core.molecules.Molecule.State, Label> capNames =
            new java.util.EnumMap<>(idle.core.molecules.Molecule.State.class);
    private VBox capsBox;

    /** La liste du plafond d'un état : pour les vérifications. */
    public javafx.scene.control.ComboBox<String> capChoice(idle.core.molecules.Molecule.State matter) {
        return capChoices.get(matter);
    }

    private HBox followRow;

    /** Le contrôle de suivi de la matière : pour les tests d'interface. */
    public Button followMatterButton() { return followMatterButton; }

    /** La carte du rassemblement automatique : pour les vérifications. */
    public Card gatherCard() {
        return gatherCard;
    }

    /** La carte de la formation automatique : pour les vérifications. */
    public Card formCard() {
        return formCard;
    }

    /** L'ordre d'achat des améliorations en atomes : pour les vérifications. */
    public OrderPane upgradeOrder() {
        return upgradeOrder;
    }

    /** L'ordre d'achat des automatismes : pour les vérifications. */
    public OrderPane automationOrder() {
        return automationOrder;
    }

    /** Le texte de la réserve d'espace de la création automatique : pour les vérifications. */
    public String spaceReserveText() {
        return spaceReserveRow.isVisible() ? spaceReserveLabel.getText() : "";
    }

    /** Clique le − ou le + de la réserve d'espace : pour les vérifications. */
    public void clickSpaceReserve(boolean more) {
        (more ? moreSpace : lessSpace).fire();
    }

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
    private List<OrderPane.Entry> upgradeEntries() {
        List<OrderPane.Entry> entries = new ArrayList<>();
        for (Upgrade upgrade : game.atomUpgradeOrder()) {
            entries.add(new OrderPane.Entry(upgrade.id(), upgrade.name(), game.isMaxed(upgrade.id())));
        }
        return entries;
    }

    /** Les automatismes ordinaires dans l'ordre du joueur ; finie, une ligne est achetée et à sa cadence maximale. */
    private List<OrderPane.Entry> automationEntries() {
        List<OrderPane.Entry> entries = new ArrayList<>();
        for (Automation automation : game.automationOrder()) {
            String id = automation.id();
            entries.add(new OrderPane.Entry(id, automation.name(), game.ownsAutomation(id) && game.isAutomationMaxed(id)));
        }
        return entries;
    }

    /** La carte de l'appui automatique : pour les vérifications. */
    public Card holdCard() {
        return holdCard;
    }

    /** La carte de la création automatique des molécules : pour les vérifications. */
    public Card moleculesCard() {
        return moleculesCard;
    }

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

    private static void wrapped(Label label, String style) {
        label.setStyle(style);
        label.setWrapText(true);
        label.setTextAlignment(TextAlignment.CENTER);
    }

    /** Affiche ou cache un composant ; caché, il ne laisse pas de trou dans la page. */
    private static void show(Node node, boolean visible) {
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
    private static boolean ownsColliderAutomate(Game game) {
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

    /** Recopie l'état des automatismes dans les cartes, et n'affiche que ceux qui sont débloqués. */
    private void refreshOrdinary() {
        BigNum atoms = game.state().atoms();
        balanceLabel.setText(Format.count(atoms) + (atoms.gt(BigNum.ONE) ? " atomes disponibles" : " atome disponible"));

        // La coupure générale : le bouton n'existe que s'il y a quelque chose à couper.
        boolean paused = game.isAutomationPaused();
        show(pauseButton, anyToPause(game));
        pauseButton.setText(paused ? "Relancer tous les automatismes" : "Couper tous les automatismes");
        pauseButton.setStyle("-fx-font-size: 13px; -fx-padding: 6 16; -fx-cursor: hand; -fx-background-radius: 6; -fx-border-radius: 6;"
                + (paused ? " -fx-text-fill: #1a0a08; -fx-background-color: #ff8a75; -fx-border-color: #ffffff;"
                : " -fx-text-fill: #9be7a8; -fx-background-color: #16202e; -fx-border-color: #3a4a5e;"));
        boolean handOnly = game.isHandOnly();
        show(pauseNote, (paused || handOnly) && anyToPause(game));
        // Ce que la coupure générale laisse tourner : le moteur n'arrête ni le collisionneur ni le tuyau.
        boolean uncut = ownsColliderAutomate(game) || game.colliderUpgradeLevel(ColliderUpgrade.AUTO_CATCH) > 0 || game.hasHose();
        pauseNote.setText(handOnly
                ? "Défi de Big Bang « À la main » : les automatismes de matière noire et ceux du Big Bang ne font rien jusqu'au "
                        + "prochain Big Bang. Leurs réglages restent."
                : PAUSE_NOTE + (uncut ? UNCUT_NOTE : ""));
        // Coupés, les automatismes s'estompent : leurs cartes disent toujours leur réglage, pas ce qu'ils font.
        double shade = paused ? 0.45 : 1;
        rows.values().forEach(row -> row.setOpacity(shade));
        darkGrid.setOpacity(shade);
        spaceGrid.setOpacity(shade);
        upgradeOrder.setOpacity(shade);
        automationOrder.setOpacity(shade);
        spaceReserveRow.setOpacity(shade);

        boolean ordinary = game.isAutomationUnlocked();

        boolean threshold = ordinary && game.isFusionThresholdUnlocked();
        show(thresholdRow, threshold);
        if (threshold) {
            int generators = game.fusionThreshold();
            thresholdLabel.setText("Fusion automatique à " + generators + " générateurs"
                    + Detail.only(" : elle attend d'en avoir autant, et rapporte "
                            + generators / game.generatorsPerAtom() + " fois les atomes, prime de groupe en plus"));
            lessGenerators.setDisable(generators <= game.generatorsPerAtom());
            moreGenerators.setDisable(generators + game.generatorsPerAtom() > game.maxGeneratorCount());
        }

        // La réserve d'atomes : réglable dès que le joueur possède la synthèse automatique.
        boolean reserve = false;
        for (Automation automation : game.automations()) {
            reserve |= automation.kind() == Automation.Kind.SYNTHESIS && game.ownsAutomation(automation.id());
        }
        show(reserveRow, reserve);
        if (reserve) {
            BigNum kept = game.synthesisReserve();
            BigNum needed = game.synthesisCost().add(kept);
            reserveLabel.setText(kept.sign() == 0
                    ? "Réserve de la synthèse : aucune" + Detail.only(". Elle dépense tous les atomes dès qu'elle le peut.")
                    : "Réserve de la synthèse : " + Format.count(kept) + " atomes"
                            + (game.isSynthesisReserveBlocking() ? ", au-dessus du plafond : baissez-la" : "")
                            + Detail.only(". Elle ne synthétise qu'à partir de " + Format.count(needed)
                                    + " atomes : la réserve reste pour vos achats."));
            lessReserve.setDisable(kept.sign() == 0);
            // Sous le plafond d'atomes, une réserve plus grande que lui ne pourrait jamais être respectée.
            moreReserve.setDisable(!game.isAtomCapLifted()
                    && reserveStep(kept.toDouble(), true) > game.atomCap().toDouble());
        }

        boolean anyShown = false;
        boolean someHidden = false;
        for (Automation automation : game.automations()) {
            boolean available = game.isAutomationAvailable(automation.id());
            // Déjà débloqué une fois : il reste affiché, avec ce qu'il faut pour le débloquer de nouveau.
            boolean known = available || game.wasAutomationEverAvailable(automation.id());
            show(rows.get(automation), known);
            if (!known) {
                someHidden = true;
                continue;
            }
            anyShown = true;
            if (available) {
                refreshCard(automation);
                refreshCadence(automation);
            } else {
                showLocked(automation);
            }
        }
        show(ordinaryHint, anyShown && Detail.shown());
        show(balanceLabel, anyShown);
        // Ce que le joueur n'a encore jamais débloqué reste caché ; une ligne annonce la suite.
        ordinaryNext.setText(!ordinary && !anyShown
                ? "Les automatismes ordinaires se débloquent à " + Format.count(Game.UNLOCK_TOTAL_ATOMS)
                        + " atomes créés (vous : " + Format.count(game.state().totalAtoms()) + ")."
                : "Un autre automatisme apparaîtra avec votre premier élément unique ★ du tableau périodique.");
        show(ordinaryNext, someHidden);

        // Les automatismes de matière noire : seulement ceux que la matière noire gagnée a débloqués.
        boolean anyDark = false;
        DarkAutomation next = null;
        boolean ordersUpgrades = false;
        boolean ordersAutomations = false;
        for (DarkAutomation automation : game.darkAutomations()) {
            Card card = darkCards.get(automation);
            boolean unlocked = game.isDarkAutomationUnlocked(automation.id());
            darkGrid.show(card, unlocked);
            ordersUpgrades |= unlocked && automation.kind() == DarkAutomation.Kind.ATOM_UPGRADES;
            ordersAutomations |= unlocked && automation.kind() == DarkAutomation.Kind.AUTOMATIONS;
            if (!unlocked) {
                if (next == null) next = automation;
                continue;
            }
            anyDark = true;
            boolean on = game.isDarkAutomationEnabled(automation.id());
            boolean suspended = false;   // depuis la 0.047, même l'automatisme qui offre les automatismes agit pendant les défis
            card.show(on && !suspended ? Card.State.ON : Card.State.OFF, "",
                    suspended ? "suspendu" : on ? "en marche" : "coupé",
                    automation.name(), brief(automation),
                    describe(automation) + "." + (suspended ? " Suspendu pendant le défi." : ""),
                    "", "toutes les " + seconds(game.darkAutomationInterval(automation.id())));
        }
        show(darkTitle, anyDark);
        show(darkHint, anyDark && Detail.shown());
        show(darkGrid, anyDark);
        show(upgradeOrder, ordersUpgrades);
        if (ordersUpgrades) upgradeOrder.refresh();
        show(automationOrder, ordersAutomations);
        if (ordersAutomations) automationOrder.refresh();
        // L'appui automatique, une fois acquis avec l'espace : il attend la matière noire s'il n'y en a pas encore.
        boolean hold = game.isAutoHoldUnlocked();
        boolean creation = game.isMoleculeAutomationUnlocked();
        boolean gather = game.isAutoGatherUnlocked();
        boolean form = game.isAutoFormUnlocked();
        int spaceCount = (hold ? 1 : 0) + (creation ? 1 : 0) + (gather ? 1 : 0) + (form ? 1 : 0);
        show(spaceTitle, spaceCount > 0);
        show(spaceGrid, spaceCount > 0);
        spaceTitle.setText(spaceCount > 1 ? "Automatismes du Big Bang" : "Automatisme du Big Bang");
        spaceGrid.show(holdCard, hold);
        spaceGrid.show(moleculesCard, creation);
        spaceGrid.show(gatherCard, gather);
        spaceGrid.show(formCard, form);
        show(spaceReserveRow, creation);
        show(followRow, creation);
        show(followMatterBox, creation && game.isStatesUnlocked());
        followMatterBox.setOpacity(shade);
        followMatterButton.setText(game.isAutoFollowingMatter()
                ? "Besoins de matière : suivi activé" : "Besoins de matière : suivi désactivé");
        followMatterButton.setDisable(!creation || paused);
        boolean caps = game.isStateCapUnlocked();
        show(capsBox, caps);
        if (caps) {
            capsTitle.setText("Plafond par état : la création automatique ne donne pas plus de cette part de l'espace créé "
                    + "aux molécules d'un état." + Detail.only(" La création à la main n'en tient pas compte. Entre parenthèses, "
                    + "la part qu'elles occupent maintenant."));
            for (idle.core.molecules.Molecule.State matter : idle.core.molecules.Molecule.State.values()) {
                javafx.scene.control.ComboBox<String> choice = capChoices.get(matter);
                String wanted = choiceOf(game.stateCap(matter));
                if (!wanted.equals(choice.getValue())) choice.setValue(wanted);
                capNames.get(matter).setText(idle.ui.common.MatterText.plural(matter) + " ("
                        + ElementText.percent(game.stateShare(matter)) + ")");
            }
        }
        if (creation) followButton.setText(game.isAutoFollowingList()
                ? "Suit la liste de courses : oui" : "Suit la liste de courses : non, seulement les amas confiés");
        if (creation) {
            double share = game.autoMoleculeReserve();
            spaceReserveLabel.setText(share <= 0
                    ? "Réserve d'espace de la création automatique : aucune"
                            + Detail.only(". Elle prend tout l'espace libre dès qu'elle le peut.")
                    : "Réserve d'espace de la création automatique : " + ElementText.percent(share)
                            + Detail.only(". Elle ne crée que si " + ElementText.percent(share) + " de tout l'espace créé reste "
                                    + "libre ensuite : cette part attend vos rassemblements et vos créations à la main."));
            lessSpace.setDisable(share <= 0);
            moreSpace.setDisable(share >= SPACE_RESERVES[SPACE_RESERVES.length - 1] - 1e-9);
        }
        if (gather) {
            boolean on = game.isAutoGatherEnabled();
            gatherCard.show(on ? Card.State.ON : Card.State.OFF, "", on ? "en marche" : "coupé",
                    "Rassemblement automatique", "Rassemble chaque sorte dès qu'elle est prête",
                    "Dès qu'une sorte compte " + Game.SUBSTANCE_MOLECULES + " molécules et que l'espace libre suffit, elle "
                            + "se rassemble d'elle-même, au même prix qu'à la main. Acquis avec l'espace, il traverse les "
                            + "explosions et les Big Bangs. Un clic " + (on ? "le coupe." : "le remet en marche."),
                    "", "toutes les " + seconds(Game.AUTO_FORM_SECONDS));
        }
        if (form) {
            boolean on = game.isAutoFormEnabled();
            formCard.show(on ? Card.State.ON : Card.State.OFF, "", on ? "en marche" : "coupé",
                    "Formation automatique", "Forme les assemblages et les astres prêts",
                    "Tout assemblage et tout astre dont les conditions sont réunies se forme de lui-même, et une "
                            + "notification l'annonce. La galaxie, l'amas de galaxies et l'univers restent à vous. Acquise "
                            + "avec l'espace, elle traverse les explosions et les Big Bangs. Un clic "
                            + (on ? "la coupe." : "la remet en marche."),
                    "", "toutes les " + seconds(Game.AUTO_FORM_SECONDS));
        }
        if (creation) {
            boolean on = game.isMoleculeAutomationEnabled();
            int chosen = game.automatedMolecules();
            boolean follow = game.isAutoFollowingList();
            boolean matter = game.isAutoFollowingMatter();
            boolean waiting = on && chosen == 0 && !follow && (!matter || game.autoMatterTargets().isEmpty());
            moleculesCard.show(on ? Card.State.ON : Card.State.OFF, "", waiting ? "en attente" : on ? "en marche" : "coupé",
                    "Création automatique", (follow ? "Suit la liste de courses" + (chosen > 0 ? ", et " : "") : "")
                            + (matter ? " + besoins de matière" : "")
                            + (chosen == 0 ? (follow ? "" : "Aucun amas ne lui est confié")
                            : chosen + (chosen > 1 ? " amas confiés" : " amas confié")),
                    (follow ? "D'abord une création de chaque sorte que demande le prochain pas (la liste de courses), "
                            + "sans rien avoir à choisir. Ensuite " : "")
                            + "Une création dans chaque amas confié, au même prix qu'à la main : la formule, prise dans le tableau "
                            + "périodique, où il reste toujours un exemplaire de chaque élément. Quand l'espace manque pour "
                            + "tous, le premier servi change à chaque passage. Les amas se confient d'un clic sur leur "
                            + "carte, ou tous d'un coup, dans la sous-page États de la matière du Big Bang. Sa réserve "
                            + "d'espace se règle sous ces cartes. Acquis au deuxième Big Bang, "
                            + "il traverse les explosions et les Big Bangs."
                            + (waiting ? " Il attend qu'un amas lui soit confié." : "")
                            + " Un clic " + (on ? "le coupe." : "le remet en marche."),
                    "", "toutes les " + seconds(game.moleculeAutomationInterval()));
        }
        if (hold) {
            boolean on = game.isAutoHoldEnabled();
            boolean waiting = on && !game.isAutoHolding();
            holdCard.show(on ? Card.State.ON : Card.State.OFF, "", waiting ? "en attente" : on ? "en marche" : "coupé",
                    "Appui automatique", "Tient l'appui sur la matière noire",
                    "La matière noire grossit comme si vous teniez le clic sur son point, à pleine vitesse d'appui et "
                            + "depuis n'importe quel onglet ; avec le verrou de l'arbre, il fournit ce que le verrou laisse. "
                            + "Acquis avec l'espace, il traverse les explosions et les Big Bangs."
                            + (waiting ? " Il attend la première explosion : sans matière noire, rien à faire grossir." : "")
                            + " Un clic " + (on ? "le coupe." : "le remet en marche."),
                    "", waiting ? "après la première explosion" : "");
        }
        refreshCollider();
        // La suite n'est annoncée qu'à qui connaît déjà la matière noire.
        boolean announce = next != null && game.isDarkMatterUnlocked();
        show(darkNext, announce);
        if (announce) {
            darkNext.setText((anyDark ? "Prochain automatisme de matière noire à "
                    : "Un automatisme de matière noire apparaîtra à ")
                    + Format.count(next.darkMatter()) + " matières noires gagnées (vous : "
                    + Format.darkMatter(game.darkMatterEarned()) + ").");
        }
    }

    /**
     * Les automates du collisionneur, son déclencheur automatique et le tuyau d'arrosage : chacun n'apparaît qu'une fois
     * son système ouvert. La coupure générale ne les estompe pas : elle ne les arrête pas.
     */
    private void refreshCollider() {
        boolean open = game.isColliderUpgradesUnlocked();
        show(colliderTitle, open);
        show(colliderHint, open);
        show(colliderGrid, open);
        if (open) {
            colliderHint.setText("Énergie du collisionneur : " + energy(game.colliderEnergy()) + ". Chaque automate achète tout "
                    + "seul les exemplaires de son générateur ; la coupure générale ne les arrête pas : un clic sur la carte "
                    + "met en pause ou relance."
                    + Detail.only(" Ils s'achètent une fois, ici ou dans le collisionneur, chacun après le précédent. Comme tout "
                            + "le collisionneur, ils traversent l'explosion et le Big Bang, et leurs pauses aussi."));
            for (Map.Entry<ColliderUpgrade, Card> entry : colliderCards.entrySet()) {
                ColliderUpgrade upgrade = entry.getKey();
                Card card = entry.getValue();
                if (game.colliderUpgradeLevel(upgrade) > 0) {
                    boolean running = game.isColliderAutomationRunning(upgrade);
                    card.show(running ? Card.State.ON : Card.State.OFF, "", running ? "en marche" : "en pause", upgrade.label(),
                            upgrade.brief(), upgrade.effect() + ". Un clic " + (running ? "le met en pause." : "le relance."), "", "");
                } else if (game.isColliderUpgradeOpen(upgrade)) {
                    card.show(game.canBuyColliderUpgrade(upgrade) ? Card.State.READY : Card.State.WAITING, "", "", upgrade.label(),
                            upgrade.brief(), upgrade.effect() + ". S'achète une fois, ici ou dans le collisionneur.",
                            energy(game.colliderUpgradeCost(upgrade)) + " énergie", "");
                } else {
                    card.show(Card.State.LOCKED, "", "", upgrade.label(), upgrade.brief(),
                            upgrade.effect() + ". S'ouvre avec « " + upgrade.requires().label() + " ».",
                            "à acheter dans le collisionneur", "");
                }
            }
        }
        int catchLevel = game.colliderUpgradeLevel(ColliderUpgrade.AUTO_CATCH);
        boolean catching = open && (catchLevel > 0 || game.isColliderUpgradeOpen(ColliderUpgrade.AUTO_CATCH));
        show(catchLine, catching);
        if (catching) {
            int max = ColliderUpgrade.AUTO_CATCH.maxLevel();
            String next = energy(game.colliderUpgradeCost(ColliderUpgrade.AUTO_CATCH)) + " énergie";
            catchLine.setText(catchLevel == 0
                    ? "Déclencheur automatique : pas encore acheté. Il s'achète dans le collisionneur, avec les faisceaux : " + next + "."
                    : "Déclencheur automatique, niveau " + catchLevel + "/" + max + " : il capte " + catchShare(catchLevel)
                            + (catchLevel < max ? ". Niveau suivant dans le collisionneur : " + next : "") + "."
                            + Detail.only(" Il capte comme un clic au bon moment. Il ne se met pas en pause, et la coupure "
                                    + "générale ne l'arrête pas."));
        }
        boolean hose = game.hasHose();
        show(hoseLine, hose);
        if (hose) {
            double rate = game.hoseRate();
            hoseLine.setText("Tuyau d'arrosage de la graine : il verse seul " + ElementText.number(rate)
                    + (rate >= 2 ? " arrosages" : " arrosage") + " par seconde, " + ElementText.percent(game.hoseShare())
                    + " de ce que verse l'arrosoir."
                    + Detail.only(" Il arrose tant que la graine peut l'être, sans pause : la coupure générale ne l'arrête pas. "
                            + "Ses niveaux s'achètent avec les améliorations de la graine, dans l'onglet Univers."));
        }
    }

    /** Ce que capte le déclencheur automatique à ce niveau. */
    private static String catchShare(int level) {
        return switch (level) {
            case 1 -> "un croisement sur cinq";
            case 2 -> "un croisement sur trois";
            default -> "tous les croisements";
        };
    }

    /** Une quantité d'énergie du collisionneur, comme dans son onglet. */
    private static String energy(double value) {
        return Format.big(BigNum.of(value));
    }

    /** La carte d'un automate du collisionneur : pour les vérifications. */
    public Card colliderCard(ColliderUpgrade upgrade) {
        return colliderCards.get(upgrade);
    }

    /** La ligne du déclencheur automatique, vide si elle est cachée : pour les vérifications. */
    public String catchText() {
        return catchLine.isVisible() ? catchLine.getText() : "";
    }

    /** La ligne du tuyau d'arrosage, vide si elle est cachée : pour les vérifications. */
    public String hoseText() {
        return hoseLine.isVisible() ? hoseLine.getText() : "";
    }

    /** La note de la coupure, vide si elle est cachée : pour les vérifications. */
    public String pauseNoteText() {
        return pauseNote.isVisible() ? pauseNote.getText() : "";
    }

    /** Ce que fait un automatisme ordinaire, en quelques mots : la ligne de sa carte. */
    private String brief(Automation automation) {
        return switch (automation.kind()) {
            case UPGRADE -> game.buysInBulk(automation) ? "Achète tous ceux qu'il peut payer" : "Achète dès que possible";
            case FUSION -> "Fusionne dès que possible";
            case SYNTHESIS -> "Synthétise dès que possible";
        };
    }

    /** Ce que fait un automatisme ordinaire, en une phrase : pour le mode détails. */
    private String describe(Automation automation) {
        return switch (automation.kind()) {
            case UPGRADE -> game.buysInBulk(automation)
                    ? "À chaque action, achète d'un coup tous les générateurs que les particules permettent."
                    : "Achète « " + automation.name() + " » dès qu'il y a assez de particules.";
            case FUSION -> "Fusionne dès que tous les générateurs sont débloqués.";
            case SYNTHESIS -> "Synthétise un élément dès qu'il y a assez d'atomes, réserve déduite.";
        };
    }

    /** Un automatisme déjà connu mais reverrouillé par une explosion : sa carte dit comment le débloquer. */
    private void showLocked(Automation automation) {
        boolean needsUnique = automation.kind() == Automation.Kind.SYNTHESIS && !game.isSynthesisAutomationUnlocked();
        cards.get(automation).show(Card.State.LOCKED, "", "verrouillé", automation.name(), brief(automation),
                describe(automation) + " " + condition(automation) + ".",
                !game.isAutomationUnlocked() ? "À " + Format.count(Game.UNLOCK_TOTAL_ATOMS) + " atomes créés"
                        : needsUnique ? "Avec un élément unique ★" : "", "");
        cadences.get(automation).show(Card.State.LOCKED, "", "", "Cadence", "", "", "", "");
    }

    /** Ce qu'il manque pour qu'un automatisme ordinaire soit de nouveau disponible. */
    private String condition(Automation automation) {
        boolean needsUnique = automation.kind() == Automation.Kind.SYNTHESIS && !game.isSynthesisAutomationUnlocked();
        if (!game.isAutomationUnlocked()) {
            return "Se débloque à " + Format.count(Game.UNLOCK_TOTAL_ATOMS) + " atomes créés (vous : "
                    + Format.count(game.state().totalAtoms()) + ")"
                    + (needsUnique ? ", avec un élément unique ★" : "");
        }
        return "Se débloque avec un élément unique ★ du tableau périodique"
                + (isGranting() ? ", ou sera offert une fois les autres automatismes à leur cadence maximale" : "");
    }

    /** Vrai si l'automatisme de matière noire qui offre les automatismes ordinaires est en marche. */
    private boolean isGranting() {
        for (DarkAutomation automation : game.darkAutomations()) {
            if (automation.kind() == DarkAutomation.Kind.GRANT_AUTOMATIONS && game.isDarkAutomationEnabled(automation.id())) {
                return true;
            }
        }
        return false;
    }

    private void refreshCard(Automation automation) {
        Card card = cards.get(automation);
        String id = automation.id();
        if (!game.ownsAutomation(id)) {
            card.show(game.canBuyAutomation(id) ? Card.State.READY : Card.State.WAITING, "", "",
                    automation.name(), brief(automation),
                    describe(automation) + " S'achète une fois, puis s'active ou se coupe d'un clic.",
                    Format.count(automation.cost()) + (automation.cost().gt(BigNum.ONE) ? " atomes" : " atome"),
                    remaining(automation.cost()));
        } else {
            boolean on = game.isAutomationEnabled(id);
            card.show(on ? Card.State.ON : Card.State.OFF, "", on ? "en marche" : "coupé",
                    automation.name(), brief(automation),
                    describe(automation) + " Un clic " + (on ? "le coupe." : "le remet en marche."), "", "");
        }
    }

    private void refreshCadence(Automation automation) {
        Card cadence = cadences.get(automation);
        String id = automation.id();
        int speedLevel = game.automationSpeedLevel(id);
        String level = speedLevel + "/" + automation.maxSpeedLevel();
        // La bulle du bonsaï, à droite (version 0.148) : plus de délai du tout.
        String delay = game.isAutomationInstant() ? "Sans délai (bulle du bonsaï)" : "Toutes les " + seconds(game.automationInterval(id));
        if (!game.ownsAutomation(id)) {
            // Pas encore acheté : sa cadence se lit, mais ne s'achète pas.
            cadence.show(Card.State.LOCKED, level, "", delay, "Après l'achat de l'automatisme", "", "", "");
        } else if (game.isAutomationMaxed(id)) {
            boolean floor = game.automationInterval(id) <= game.minAutomationInterval();
            cadence.show(Card.State.DONE, level, "max", delay, floor ? "Délai le plus court possible" : "Cadence maximale",
                    floor ? "" : "Les éléments du tableau périodique réduisent encore ce délai.", "", "");
        } else {
            BigNum cost = game.automationSpeedCost(id);
            double ratio = automation.intervalAt(speedLevel) / automation.intervalAt(speedLevel + 1);
            cadence.show(game.canSpeedUpAutomation(id) ? Card.State.READY
                            : speedLevel > 0 ? Card.State.STARTED : Card.State.WAITING,
                    level, "", delay, "Niveau suivant : délai ÷" + ElementText.number(ratio),
                    "Le délai entre deux actions. Chaque niveau le réduit, jusqu'au maximum de la cadence ; "
                            + "au-delà, seuls les éléments l'accélèrent.",
                    Format.count(cost) + (cost.gt(BigNum.ONE) ? " atomes" : " atome"), remaining(cost));
        }
    }

    /** Ce qu'il reste à attendre avant de pouvoir payer {@code cost} atomes : un nombre de fusions, ou rien. */
    private String remaining(BigNum cost) {
        long fusions = game.fusionsUntilAtoms(cost);
        if (fusions <= 0) return "";
        return Format.whole(fusions) + (fusions > 1 ? " fusions" : " fusion");
    }

    /** Ce que fait un automatisme de matière noire, en quelques mots : la ligne de sa carte. */
    private static String brief(DarkAutomation automation) {
        return switch (automation.kind()) {
            case GRANT_AUTOMATIONS -> "Offre les automatismes au départ";
            case ATOM_UPGRADES -> "Achète les améliorations en atomes";
            case AUTOMATIONS -> "Achète automatismes et cadences";
            case EXPLOSION -> "Fait exploser le tableau";
            case DARK_TREE -> "Achète l'arbre de matière noire";
        };
    }

    /** Ce que fait un automatisme de matière noire, en une phrase. */
    private static String describe(DarkAutomation automation) {
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
    public static String seconds(double value) {
        return ElementText.number(value) + " s";
    }
}
