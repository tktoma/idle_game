package idle.ui.bigbang;

import idle.core.Game;
import idle.core.bigbang.BigBangMilestone;
import idle.core.bigbang.SpaceUpgrade;
import idle.core.molecules.Molecule;
import idle.core.sky.Assembly;
import idle.core.sky.Cosmos;
import idle.ui.GameApp;
import idle.ui.Readiness;
import idle.ui.common.Breakdown;
import idle.ui.common.CanvasPane;
import idle.ui.common.Card;
import idle.ui.common.FilterBar;
import idle.ui.common.Labels;
import idle.ui.common.MatterText;
import idle.ui.common.StableHeight;
import idle.ui.common.TileGrid;
import idle.ui.probes.ProbesPane;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
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

/**
 * La page du Big Bang, première partie : ses sous-pages, ses cartes et sa construction.
 */
public abstract class BigBangPageBase extends VBox {

    /** La page entière : chaque partie de la chaîne est une {@link BigBangPage}. */
    final BigBangPage self() {
        return (BigBangPage) this;
    }

    BigBangPageBase(Game game) {
        super(8);
        this.game = game;
        this.spaceView = new SpaceView(game);
        this.shoppingPane = new ShoppingPane(game, this::refresh);
        this.rateBreakdown = Breakdown.attach(rateLabel, game, Breakdown.Of.SPACE);
        this.bangChallenges = new BangChallengesPane(game, this::refresh);
        this.bodiesPane = new BodiesPane(game, this::refresh);
        this.bodiesScroll = new ScrollPane(bodiesPane);
        Labels.transparent(bodiesScroll);
        this.probesPane = new ProbesPane(game, this::refresh);
        this.probesScroll = new ScrollPane(probesPane);
        Labels.transparent(probesScroll);
        this.systemsPane = new SystemsPane(game, this::refresh);
        this.systemsScroll = new ScrollPane(systemsPane);
        Labels.transparent(systemsScroll);
        this.galaxyPane = new GalaxyPane(game, this::refresh, () -> {
            // La plus grande échelle formée : la galaxie, ou plus loin, l'amas de galaxies, l'univers.
            spaceView.showGalaxy(true);
            spaceView.showCosmos(game.cosmosFormed() >= 2 ? Cosmos.values()[game.cosmosFormed() - 1] : null);
            spaceView.fit();
            select(1);
            refresh();
        });
        setAlignment(Pos.TOP_CENTER);
        setPadding(new Insets(16, 24, 16, 24));

        countLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + GameApp.BIG_BANG_COLOR + ";");
        Labels.centered(sinceLabel, "-fx-font-size: 13px; -fx-text-fill: #8fa3b8;");

        moleculesTab.setOnAction(event -> select(0));
        expansionTab.setOnAction(event -> select(1));
        upgradesTab.setOnAction(event -> select(2));
        statesTab.setOnAction(event -> select(3));
        assembliesTab.setOnAction(event -> select(4));
        bodiesTab.setOnAction(event -> select(5));
        galaxyTab.setOnAction(event -> select(6));
        milestonesTab.setOnAction(event -> select(7));
        probesTab.setOnAction(event -> {
            select(8);
            refresh();
        });
        systemsTab.setOnAction(event -> {
            select(9);
            refresh();
        });
        for (Button tab : new Button[] {moleculesTab, expansionTab, upgradesTab, milestonesTab, probesTab, statesTab, assembliesTab, bodiesTab, systemsTab, galaxyTab}) {
            tab.setFocusTraversable(false);
        }
        subTabs = new FlowPane(0, 0, moleculesTab, expansionTab, upgradesTab, milestonesTab, probesTab, statesTab, assembliesTab, bodiesTab, systemsTab, galaxyTab);
        subTabs.setAlignment(Pos.CENTER);

        // Les molécules : une phrase, les rayons du catalogue, puis les cartes du rayon choisi.
        moleculesPane.setAlignment(Pos.TOP_CENTER);
        moleculesPane.setPadding(new Insets(10, 0, 10, 0));
        Labels.note(moleculesIntro, 760);
        FlowPane chips = new FlowPane(6, 6);
        chips.setAlignment(Pos.CENTER);
        favoritesChip.setFocusTraversable(false);
        favoritesChip.setOnAction(event -> {
            selectFavorites();
            refresh();
        });
        chips.getChildren().add(favoritesChip);
        for (Molecule.Kind each : Molecule.Kind.values()) {
            Button chip = new Button();
            chip.setFocusTraversable(false);
            chip.setOnAction(event -> selectKind(each));
            kindChips.put(each, chip);
            chips.getChildren().add(chip);
        }
        chips.setMaxWidth(820);
        moleculeFilter = new FilterBar("Rechercher une molécule : nom ou formule", GameApp.BIG_BANG_COLOR, self(), this::moleculeFilterChanged);
        Map<Molecule.State, String> stateNames = new LinkedHashMap<>();
        for (Molecule.State state : Molecule.State.values()) stateNames.put(state, MatterText.signed(state));
        stateFilter = moleculeFilter.group("Tous les types", stateNames);
        Map<Gives, String> givesNames = new LinkedHashMap<>();
        givesNames.put(Gives.SPACE, "Espace");
        givesNames.put(Gives.PARTICLES, "Particules");
        givesNames.put(Gives.ATOMS, "Atomes");
        givesNames.put(Gives.DARK_GROWTH, "Matière noire");
        givesNames.put(Gives.UNCAP, "Plafond d'un élément");
        givesFilter = moleculeFilter.group("Toutes les améliorations", givesNames);
        Map<Use, String> useNames = new LinkedHashMap<>();
        useNames.put(Use.CREATABLE, "Créables maintenant");
        useNames.put(Use.WANTED, "Utiles au prochain astre");
        useFilter = moleculeFilter.group("Toutes", useNames);
        Labels.note(moleculesFound, 760);
        Labels.centered(nextKindLabel, "-fx-font-size: 12px; -fx-text-fill: " + GameApp.BIG_BANG_COLOR + ";");
        nextKindLabel.setMaxWidth(760);
        gridHolder.setMaxWidth(900);
        moleculesPane.getChildren().add(moleculesIntro);
        moleculesPane.getChildren().add(moleculeFilter);
        Labels.centered(needsLabel, "-fx-font-size: 13px; -fx-text-fill: " + GameApp.BIG_BANG_COLOR + ";");
        needsLabel.setMaxWidth(760);
        moleculesPane.getChildren().add(needsLabel);
        moleculesPane.getChildren().add(chips);
        bestLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #ffe9c2;");
        bestButton.setStyle(ZOOM_STYLE);
        bestButton.setFocusTraversable(false);
        bestButton.setOnAction(event -> {
            if (best != null) game.createMolecule(best.id());
            bestAt = 0;
            refresh();
        });
        randomButton.setStyle(ZOOM_STYLE);
        randomButton.setFocusTraversable(false);
        randomButton.setMinWidth(BigBangPage.RANDOM_BUTTON_WIDTH);
        randomButton.setOnAction(event -> {
            game.createRandomMolecule(kind);
            bestAt = 0;
            refresh();
        });
        sortButton.setStyle(ZOOM_STYLE);
        sortButton.setFocusTraversable(false);
        sortButton.setOnAction(event -> {
            sortedByYield = !sortedByYield;
            sortShownGrid();
            refresh();
        });
        bestBar.setAlignment(Pos.TOP_LEFT);
        bestBar.setMaxWidth(BigBangPage.BEST_BAR_WIDTH);
        bestButton.setMinWidth(BigBangPage.BEST_BUTTON_WIDTH);
        bestLabel.setWrapText(true);
        bestLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(bestLabel, Priority.ALWAYS);
        StableHeight.of(bestBar);
        Labels.note(collectionLabel, 760);
        moleculesPane.getChildren().add(collectionLabel);
        pulseBar.setAlignment(Pos.CENTER);
        moleculesPane.getChildren().add(pulseBar);
        moleculesPane.getChildren().add(bestBar);
        reservationButton.setStyle(ZOOM_STYLE);
        reservationButton.setFocusTraversable(false);
        reservationButton.setOnAction(event -> {
            game.setRecipeReservation(!game.state().recipeReservation());
            refresh();
        });
        reservationBar.setAlignment(Pos.CENTER);
        moleculesPane.getChildren().add(reservationBar);
        createShownButton.setStyle(ZOOM_STYLE);
        createShownButton.setFocusTraversable(false);
        createShownButton.setOnAction(event -> {
            createAllShown();
            refresh();
        });
        foundRow.setAlignment(Pos.CENTER);
        moleculesPane.getChildren().add(foundRow);
        moleculesPane.getChildren().add(nextKindLabel);
        moleculesPane.getChildren().add(gridHolder);
        Labels.transparent(moleculesScroll);

        // L'expansion : les nombres, la vue de l'espace, et de quoi s'en approcher ou s'en éloigner.
        expansionPane.setAlignment(Pos.TOP_CENTER);
        expansionPane.setPadding(new Insets(10, 0, 0, 0));
        spaceLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #fffaf0;");
        Labels.centered(rateLabel, "-fx-font-size: 13px; -fx-text-fill: " + GameApp.BIG_BANG_COLOR + ";");
        Labels.note(expansionNote, 760);
        zoomOut.setOnAction(event -> spaceView.zoom(1 / SpaceView.ZOOM_STEP));
        zoomIn.setOnAction(event -> spaceView.zoom(SpaceView.ZOOM_STEP));
        zoomAll.setOnAction(event -> spaceView.fit());
        viewToggle.setOnAction(event -> {
            // Depuis l'amas de galaxies ou l'univers, ce bouton ramène à la galaxie ; sinon il alterne galaxie et matière.
            if (spaceView.cosmosShown() != null) {
                spaceView.showCosmos(null);
                spaceView.showGalaxy(true);
            } else {
                spaceView.showGalaxy(!spaceView.showsGalaxy());
            }
            refresh();
        });
        clusterView.setOnAction(event -> {
            spaceView.showCosmos(Cosmos.CLUSTER);
            refresh();
        });
        universeView.setOnAction(event -> {
            spaceView.showCosmos(Cosmos.UNIVERSE);
            refresh();
        });
        for (Button button : new Button[] {zoomOut, zoomIn, zoomAll, viewToggle, clusterView, universeView}) {
            button.setStyle(ZOOM_STYLE);
            button.setFocusTraversable(false);
        }
        FlowPane zoomRow = new FlowPane(6, 6, zoomOut, zoomIn, zoomAll, viewToggle, clusterView, universeView);
        zoomRow.setAlignment(Pos.CENTER);
        zoomRow.setPrefWrapLength(760);
        // La vue s'étire avec la fenêtre ; les textes gardent leur hauteur.
        CanvasPane viewPane = CanvasPane.filling(spaceView);
        VBox.setVgrow(viewPane, Priority.ALWAYS);
        spaceView.setStyle("-fx-cursor: open-hand;");
        expansionPane.getChildren().add(spaceLabel);
        expansionPane.getChildren().add(rateLabel);
        cometButton.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 6 16; -fx-cursor: hand; -fx-background-radius: 6;"
                + " -fx-border-radius: 6; -fx-text-fill: #10151f; -fx-background-color: #bfe6f2; -fx-border-color: #ffffff;");
        cometButton.setFocusTraversable(false);
        cometButton.setOnAction(event -> {
            game.catchComet();
            Readiness.of(game).compute();
            refresh();
        });
        expansionPane.getChildren().add(expansionNote);
        expansionPane.getChildren().add(cometButton);
        skyEventButton.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 6 16; -fx-cursor: hand; -fx-background-radius: 6;"
                + " -fx-border-radius: 6; -fx-text-fill: #10151f; -fx-background-color: #d9c8ff; -fx-border-color: #ffffff;");
        skyEventButton.setFocusTraversable(false);
        skyEventButton.setOnAction(event -> {
            game.catchSkyEvent();
            refresh();
        });
        expansionPane.getChildren().add(skyEventButton);
        expansionPane.getChildren().add(pulseButton);
        expansionPane.getChildren().add(viewPane);
        expansionPane.getChildren().add(zoomRow);

        // Les améliorations d'espace : une carte chacune, montrée quand elle devient accessible.
        upgradesPane.setAlignment(Pos.TOP_CENTER);
        upgradesPane.setPadding(new Insets(10, 0, 10, 0));
        Labels.note(upgradesIntro, 760);
        for (SpaceUpgrade upgrade : game.spaceUpgrades()) {
            // Une tuile, pas un bouton : l'amélioration est acquise d'elle-même quand son seuil est atteint.
            Card card = new Card(GameApp.BIG_BANG_COLOR);
            upgradeCards.put(upgrade, card);
            upgradesGrid.add(card);
        }
        upgradesGrid.setMaxWidth(820);
        upgradesPane.getChildren().add(upgradesIntro);
        upgradesPane.getChildren().add(upgradesGrid);
        Labels.transparent(upgradesScroll);

        // Les paliers de Big Bang : une carte chacun, toujours montrée ; depuis la version 0.124, un clic paie le palier ouvert.
        milestonesPane.setAlignment(Pos.TOP_CENTER);
        milestonesPane.setPadding(new Insets(10, 0, 10, 0));
        Labels.note(milestonesIntro, 760);
        for (BigBangMilestone milestone : game.bigBangMilestones()) {
            Card card = new Card(GameApp.BIG_BANG_COLOR);
            card.setOnAction(() -> {
                game.buyBigBangMilestone(milestone);
                refresh();
            });
            milestoneCards.put(milestone, card);
            milestonesGrid.add(card);
        }
        milestonesGrid.setMaxWidth(820);
        milestonesPane.getChildren().add(milestonesIntro);
        milestonesPane.getChildren().add(milestonesGrid);
        milestonesPane.getChildren().add(bangChallenges);
        Labels.transparent(milestonesScroll);

        // Les états de la matière : un lieu par état, et dans chacun une carte par molécule qui s'y
        // rassemble. Une carte n'est construite que lorsque le joueur a créé sa molécule ({@link #stateCard}).
        statesPane.setAlignment(Pos.TOP_CENTER);
        statesPane.setPadding(new Insets(10, 0, 10, 0));
        Labels.note(statesIntro, 760);
        statesPane.getChildren().add(statesIntro);
        bulk(gatherAll, () -> game.formAllSubstances());
        // Tous confiés : le bouton les retire tous ; sinon il confie ceux qui ne le sont pas encore.
        bulk(entrustAll, () -> game.setAllMoleculesAutomated(game.automatedMolecules() < game.state().substances().size()));
        HBox stateButtons = new HBox(8, gatherAll, entrustAll);
        stateButtons.setAlignment(Pos.CENTER);
        statesPane.getChildren().add(stateButtons);
        for (Molecule.State matter : Molecule.State.values()) {
            Label title = new Label();
            title.setStyle("-fx-font-size: 14px; -fx-text-fill: " + GameApp.BIG_BANG_COLOR + "; -fx-padding: 8 0 0 0;");
            TileGrid grid = new TileGrid(230, 3, 8);
            grid.setMaxWidth(820);
            placeTitles.put(matter, title);
            placeGrids.put(matter, grid);
            statesPane.getChildren().add(title);
            statesPane.getChildren().add(grid);
        }
        Labels.transparent(statesScroll);

        // Les assemblages : une section par famille, et dans chacune une carte par assemblage, construite
        // quand le joueur a créé un de ses ingrédients ({@link #assemblyCard}).
        assembliesPane.setAlignment(Pos.TOP_CENTER);
        assembliesPane.setPadding(new Insets(10, 0, 10, 0));
        Labels.note(assembliesIntro, 760);
        assembliesPane.getChildren().add(assembliesIntro);
        bulk(assembleAll, () -> game.formAllAssemblies());
        assembliesPane.getChildren().add(assembleAll);
        for (Assembly.Family family : Assembly.Family.values()) {
            Label title = new Label();
            title.setStyle("-fx-font-size: 14px; -fx-text-fill: " + GameApp.BIG_BANG_COLOR + "; -fx-padding: 8 0 0 0;");
            TileGrid grid = new TileGrid(230, 3, 8);
            grid.setMaxWidth(820);
            familyTitles.put(family, title);
            familyGrids.put(family, grid);
            assembliesPane.getChildren().add(title);
            assembliesPane.getChildren().add(grid);
        }
        Labels.transparent(assembliesScroll);

        // Les sous-pages sont empilées au même endroit ; une seule est visible à la fois.
        StackPane pages = new StackPane(moleculesScroll, expansionPane, upgradesScroll, statesScroll, assembliesScroll, bodiesScroll,
                galaxyPane, milestonesScroll, probesScroll, systemsScroll);
        VBox.setVgrow(pages, Priority.ALWAYS);

        getChildren().add(StableHeight.of(countLabel));
        getChildren().add(StableHeight.of(sinceLabel));
        // Le petit arbre du cycle (version 0.087) : trois choix, un seul jusqu'au prochain Big Bang.
        cycleTitle.setStyle("-fx-font-size: 12px; -fx-text-fill: " + GameApp.BIG_BANG_COLOR + ";");
        cycleRow.setAlignment(Pos.CENTER);
        cycleRow.getChildren().add(cycleTitle);
        for (idle.core.universe.CycleChoice choice : idle.core.universe.CycleChoice.values()) {
            Button button = new Button(choice.label());
            button.setFocusTraversable(false);
            button.setOnAction(event -> {
                game.chooseCycle(choice);
                refresh();
            });
            cycleButtons.put(choice, button);
            cycleRow.getChildren().add(button);
        }
        getChildren().add(subTabs);
        getChildren().add(cycleRow);
        getChildren().add(shoppingPane);
        getChildren().add(pages);
        selectKind(kind);
        select(0);
    }

    // Ce que fournissent les parties suivantes.
    abstract void bulk(Button button, Runnable action);
    public abstract void select(int index);
    public abstract void selectKind(Molecule.Kind wanted);
    public abstract void selectFavorites();
    abstract void moleculeFilterChanged();
    abstract void sortShownGrid();
    public abstract void refresh();
    abstract int createAllShown();
    abstract Button pulseButton();


    /** La barre des sous-onglets : la disposition à gauche la place en haut de la fenêtre. */
    private FlowPane subTabs;

    public FlowPane subTabs() {
        return subTabs;
    }

    static final String CHIP_STYLE = "-fx-font-size: 12px; -fx-padding: 3 10; -fx-cursor: hand;"
            + " -fx-background-radius: 12; -fx-border-radius: 12;";
    static final String ZOOM_STYLE = "-fx-font-size: 12px; -fx-padding: 3 10; -fx-cursor: hand;"
            + " -fx-background-radius: 4; -fx-border-radius: 4; -fx-text-fill: " + GameApp.BIG_BANG_COLOR + ";"
            + " -fx-background-color: #121923; -fx-border-color: " + GameApp.BIG_BANG_COLOR + "66;";
    /** Taille du dessin d'une molécule sur sa carte. */
    static final double PICTURE_WIDTH = 150;
    static final double PICTURE_HEIGHT = 80;

    final Game game;
    final Label countLabel = new Label();
    final Label cycleTitle = new Label();
    final HBox cycleRow = new HBox(8);
    final Map<idle.core.universe.CycleChoice, Button> cycleButtons = new java.util.EnumMap<>(idle.core.universe.CycleChoice.class);
    final Label sinceLabel = new Label();
    final Button moleculesTab = new Button("Molécules");
    final Button expansionTab = new Button("Expansion de la matière");
    final Button upgradesTab = new Button("Améliorations");
    final Button statesTab = new Button("États de la matière");
    final Button assembliesTab = new Button("Assemblages");
    final Button bodiesTab = new Button("Astres");
    /** Sous-page « Astres ». */
    final BodiesPane bodiesPane;
    /** La page des astres défile d'un bloc : le ciel, puis toutes les familles d'astres. */
    final ScrollPane bodiesScroll;
    /** Sous-page « Univers » : la galaxie, l'amas de galaxies, l'univers. */
    final Button galaxyTab = new Button("Univers");
    /** Les paliers de Big Bang : rangé à côté des améliorations, mais numéroté après la galaxie ({@link #select(int)}). */
    final Button milestonesTab = new Button("Paliers");
    final Button probesTab = new Button("Sondes");
    final Button systemsTab = new Button("Systèmes solaires");
    final SystemsPane systemsPane;
    final ScrollPane systemsScroll;
    final ProbesPane probesPane;
    final ScrollPane probesScroll;
    final GalaxyPane galaxyPane;
    int selected = 0;

    // Sous-page « Molécules »
    private final VBox moleculesPane = new VBox(10);
    final ScrollPane moleculesScroll = new ScrollPane(moleculesPane);
    final Label moleculesIntro = new Label();
    final Label nextKindLabel = new Label();
    final Map<Molecule.Kind, Button> kindChips = new EnumMap<>(Molecule.Kind.class);
    final Map<Molecule.Kind, TileGrid> grids = new EnumMap<>(Molecule.Kind.class);
    final Map<Molecule, Card> cards = new LinkedHashMap<>();
    final StackPane gridHolder = new StackPane();
    Molecule.Kind kind = Molecule.Kind.SIMPLE;
    /** Les molécules épinglées : une pastille à part, devant les rayons, et leurs cartes à elles. */
    final Button favoritesChip = new Button();
    final TileGrid favoriteGrid = new TileGrid(200, 4, 8);
    final Map<Molecule, Card> favoriteCards = new LinkedHashMap<>();
    boolean favoritesShown = false;
    /** Ce qu'on peut faire d'une molécule tout de suite, pour le filtre. */
    public enum Use { CREATABLE, WANTED }
    /** Ce que donne une molécule, pour le filtre : une des quatre grandeurs, ou un plafond relevé. */
    public enum Gives { SPACE, PARTICLES, ATOMS, DARK_GROWTH, UNCAP }
    /** La recherche et les filtres des molécules : par état, et par ce qu'elles donnent. */
    final FilterBar moleculeFilter;
    final FilterBar.Group<Molecule.State> stateFilter;
    final FilterBar.Group<Gives> givesFilter;
    final FilterBar.Group<Use> useFilter;
    final Label moleculesFound = new Label();
    /** Crée le plus possible de chaque molécule que montrent les filtres (ou les favorites). */
    final Button createShownButton = new Button("Créer le maximum");
    final HBox foundRow = new HBox(10, moleculesFound, createShownButton);
    /** La collection du rayon affiché : où elle en est, et ce qu'elle rapporte. */
    final Label collectionLabel = new Label();
    /** Les défis de Big Bang, sous les paliers. */
    final BangChallengesPane bangChallenges;
    /** La comète : le bouton qui la saisit, montré le temps qu'elle traverse l'expansion. */
    final Button cometButton = new Button();
    /** L'anomalie du ciel à saisir (version 0.127), sous la comète. */
    final Button skyEventButton = new Button();
    /** Vrai si le joueur veut voir les anomalies du ciel (un réglage). */
    java.util.function.BooleanSupplier skyEventsShown = () -> true;
    /** L'impulsion, sur la page Expansion et au-dessus des molécules : deux boutons, la même action. */
    final Button pulseButton = pulseButton();
    final Button moleculesPulseButton = pulseButton();
    /** La liste de courses du prochain pas, au-dessus des assemblages, des astres et de l'univers. */
    final ShoppingPane shoppingPane;
    // Le meilleur achat : la création qui rapporte le plus pour l'espace qu'elle prend, et le tri des cartes par rendement.
    final Label bestLabel = new Label();
    final Button bestButton = new Button("Créer");
    final Button sortButton = new Button();
    // Les boutons d'abord, le texte ensuite : le bouton « Créer » ne bouge plus quand le texte change de longueur,
    // et peut se cliquer à la chaîne.
    /** L'achat simple : une molécule du rayon affiché, tirée au hasard, utile au prochain pas d'abord. */
    final Button randomButton = new Button("Au hasard dans ce rayon");
    private final HBox bestBar = new HBox(8, bestButton, randomButton, sortButton, bestLabel);
    /** La réservation par recette (version 0.085) : ce qu'il faut encore au prochain assemblage, sur chaque carte. */
    final Button reservationButton = new Button();
    final HBox reservationBar = new HBox(reservationButton);
    /** Ce que demande le prochain pas, rayon par rayon et état par état. */
    final Label needsLabel = new Label();
    private final HBox pulseBar = new HBox(moleculesPulseButton);
    boolean sortedByYield = false;
    Molecule best = null;
    Molecule bestHere = null;
    long bestAt = 0;
    int bestVersion = -1;
    static final long BEST_EVERY_NANOS = 500_000_000L;
    /** Le nom, la formule et l'identifiant de chaque molécule, tels que la recherche les compare. */
    final Map<Molecule, String> searchTexts = new java.util.HashMap<>();

    // Sous-page « Expansion de la matière »
    final VBox expansionPane = new VBox(6);
    final Label spaceLabel = new Label();
    final Label rateLabel = new Label();
    /** Au survol : d'où vient l'espace créé chaque seconde. */
    final Breakdown rateBreakdown;
    final Label expansionNote = new Label();
    final SpaceView spaceView;
    final Button zoomOut = new Button("Reculer");
    final Button zoomIn = new Button("Approcher");
    final Button zoomAll = new Button("Tout voir");
    /** Une fois la galaxie formée : pour revoir la matière telle qu'elle était rangée, puis revenir à la galaxie. */
    final Button viewToggle = new Button("Voir la matière");
    /** Une fois l'amas de galaxies formé, puis l'univers : pour reculer la vue jusqu'à eux. */
    final Button clusterView = new Button("Voir l'amas de galaxies");
    final Button universeView = new Button("Voir l'univers");

    // Sous-page « Améliorations »
    private final VBox upgradesPane = new VBox(10);
    final ScrollPane upgradesScroll = new ScrollPane(upgradesPane);
    final Label upgradesIntro = new Label();
    final TileGrid upgradesGrid = new TileGrid(230, 3, 8);
    final Map<SpaceUpgrade, Card> upgradeCards = new LinkedHashMap<>();

    // Sous-page « Paliers » : les paliers de Big Bang
    private final VBox milestonesPane = new VBox(10);
    final ScrollPane milestonesScroll = new ScrollPane(milestonesPane);
    final Label milestonesIntro = new Label();
    private final TileGrid milestonesGrid = new TileGrid(230, 3, 8);
    final Map<BigBangMilestone, Card> milestoneCards = new LinkedHashMap<>();

    // Sous-page « États de la matière »
    private final VBox statesPane = new VBox(10);
    final ScrollPane statesScroll = new ScrollPane(statesPane);
    final Label statesIntro = new Label();
    /** Les gestes groupés, une fois acquis : un bouton en haut des états, des assemblages et des astres. */
    final Button gatherAll = new Button();
    final Button assembleAll = new Button();
    /** Confie tous les amas à la création automatique, ou les lui retire tous. */
    final Button entrustAll = new Button();
    /** Un lieu par état : son titre, et la grille des molécules qui s'y rassemblent. */
    final Map<Molecule.State, Label> placeTitles = new EnumMap<>(Molecule.State.class);
    final Map<Molecule.State, TileGrid> placeGrids = new EnumMap<>(Molecule.State.class);
    final Map<Molecule, Card> stateCards = new LinkedHashMap<>();

    // Sous-page « Assemblages »
    private final VBox assembliesPane = new VBox(10);
    final ScrollPane assembliesScroll = new ScrollPane(assembliesPane);
    final Label assembliesIntro = new Label();
    /** Une section par famille : son titre, et la grille de ses assemblages. */
    final Map<Assembly.Family, Label> familyTitles = new EnumMap<>(Assembly.Family.class);
    final Map<Assembly.Family, TileGrid> familyGrids = new EnumMap<>(Assembly.Family.class);
    final Map<Assembly, Card> assemblyCards = new LinkedHashMap<>();
}
