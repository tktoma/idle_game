package idle.ui;

import idle.core.Assembly;
import idle.core.BigBangMilestone;
import idle.core.BigNum;
import idle.core.Game;
import idle.core.Molecule;
import idle.core.PeriodicTable;
import idle.core.SpaceUpgrade;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;

/**
 * L'onglet « Big Bang » : l'acte qui s'ouvre au premier Big Bang ({@link Game#bigBang()}).
 *
 * <p>En haut, le nombre de Big Bangs déclenchés. En dessous, six sous-pages, dont les trois
 * dernières n'apparaissent qu'une fois ouvertes par une amélioration :
 * <ul>
 *   <li>« Molécules » : le catalogue ({@link Game#molecules()}), un rayon à la fois
 *       ({@link Molecule.Kind}). Seuls les rayons ouverts sont proposés : les petites molécules
 *       d'abord, puis un rayon de plus à chaque amélioration d'espace ; une ligne annonce le prochain. Chaque molécule est une {@link Card} qui porte son dessin
 *       ({@link MoleculeArt}), sa formule, l'espace qu'elle occupe, combien ont été créées et ce
 *       que demande la suivante ; un clic la crée, avec des exemplaires du tableau périodique ;</li>
 *   <li>« Expansion de la matière » : l'espace créé, ce qu'il gagne par seconde, ce que les
 *       molécules en occupent, et une vue de cet espace ({@link SpaceView}) où l'on peut
 *       s'approcher des molécules ou reculer jusqu'à tout voir ;</li>
 *   <li>« Améliorations » : ce que l'espace gagné ouvre ({@link Game#spaceUpgrades()}) : les
 *       rayons de molécules, un à un, et le rassemblement. Rien ne s'y dépense. Une amélioration
 *       n'est montrée que lorsque celle d'avant est achetée ;</li>
 *   <li>« États de la matière » : le rassemblement des molécules en gaz, liquides, solides, cristaux et métaux
 *       ({@link Game#formSubstance(String)}), un achat unique par sorte de molécule. La sous-page
 *       a un lieu par état, où se rangent les cartes de ses molécules ; une molécule n'y apparaît
 *       que lorsque le joueur en a créé au moins une ;</li>
 *   <li>« Assemblages » : les roches, minerais, pierres précieuses, eaux, gaz et hydrocarbures faits de
 *       plusieurs sortes de molécules ({@link Game#formAssembly(String)}), une section par famille.
 *       Un assemblage n'y apparaît que lorsque le joueur a créé au moins un de ses ingrédients ;</li>
 *   <li>« Astres » : le ciel et les corps qu'on y forme, de l'amas de roches à la planète
 *       ({@link BodiesPane}).</li>
 * </ul>
 * Les cartes d'un rayon ne sont construites que la première fois qu'on l'ouvre : le catalogue
 * compte plusieurs centaines de molécules.
 */
final class BigBangPage extends VBox {

    private static final String SUB_TAB_STYLE = "-fx-font-size: 13px; -fx-padding: 6 20; -fx-cursor: hand;"
            + " -fx-background-radius: 0; -fx-border-width: 0 0 2 0; -fx-background-color: transparent;";
    private static final String CHIP_STYLE = "-fx-font-size: 12px; -fx-padding: 3 10; -fx-cursor: hand;"
            + " -fx-background-radius: 12; -fx-border-radius: 12;";
    private static final String ZOOM_STYLE = "-fx-font-size: 12px; -fx-padding: 3 10; -fx-cursor: hand;"
            + " -fx-background-radius: 4; -fx-border-radius: 4; -fx-text-fill: " + GameApp.BIG_BANG_COLOR + ";"
            + " -fx-background-color: #121923; -fx-border-color: " + GameApp.BIG_BANG_COLOR + "66;";
    /** Taille du dessin d'une molécule sur sa carte. */
    private static final double PICTURE_WIDTH = 150;
    private static final double PICTURE_HEIGHT = 80;

    private final Game game;
    private final Label countLabel = new Label();
    private final Label sinceLabel = new Label();
    private final Button moleculesTab = new Button("Molécules");
    private final Button expansionTab = new Button("Expansion de la matière");
    private final Button upgradesTab = new Button("Améliorations");
    private final Button statesTab = new Button("États de la matière");
    private final Button assembliesTab = new Button("Assemblages");
    private final Button bodiesTab = new Button("Astres");
    /** Sous-page « Astres ». */
    private final BodiesPane bodiesPane;
    /** Sous-page « Univers » : la galaxie, l'amas de galaxies, l'univers. */
    private final Button galaxyTab = new Button("Univers");
    /** Les paliers de Big Bang : rangé à côté des améliorations, mais numéroté après la galaxie ({@link #select(int)}). */
    private final Button milestonesTab = new Button("Paliers");
    private final GalaxyPane galaxyPane;
    private int selected = 0;

    // Sous-page « Molécules »
    private final VBox moleculesPane = new VBox(10);
    private final ScrollPane moleculesScroll = new ScrollPane(moleculesPane);
    private final Label moleculesIntro = new Label();
    private final Label nextKindLabel = new Label();
    private final Map<Molecule.Kind, Button> kindChips = new EnumMap<>(Molecule.Kind.class);
    private final Map<Molecule.Kind, TileGrid> grids = new EnumMap<>(Molecule.Kind.class);
    private final Map<Molecule, Card> cards = new LinkedHashMap<>();
    private final StackPane gridHolder = new StackPane();
    private Molecule.Kind kind = Molecule.Kind.SIMPLE;
    /** Ce que donne une molécule, pour le filtre : une des quatre grandeurs, ou un plafond relevé. */
    enum Gives { SPACE, PARTICLES, ATOMS, DARK_GROWTH, UNCAP }
    /** La recherche et les filtres des molécules : par état, et par ce qu'elles donnent. */
    private final FilterBar moleculeFilter;
    private final FilterBar.Group<Molecule.State> stateFilter;
    private final FilterBar.Group<Gives> givesFilter;
    private final Label moleculesFound = new Label();
    /** Le nom, la formule et l'identifiant de chaque molécule, tels que la recherche les compare. */
    private final Map<Molecule, String> searchTexts = new java.util.HashMap<>();

    // Sous-page « Expansion de la matière »
    private final VBox expansionPane = new VBox(6);
    private final Label spaceLabel = new Label();
    private final Label rateLabel = new Label();
    private final Label expansionNote = new Label();
    private final SpaceView spaceView;
    private final Button zoomOut = new Button("Reculer");
    private final Button zoomIn = new Button("Approcher");
    private final Button zoomAll = new Button("Tout voir");
    /** Une fois la galaxie formée : pour revoir la matière telle qu'elle était rangée, puis revenir à la galaxie. */
    private final Button viewToggle = new Button("Voir la matière");
    /** Une fois l'amas de galaxies formé, puis l'univers : pour reculer la vue jusqu'à eux. */
    private final Button clusterView = new Button("Voir l'amas de galaxies");
    private final Button universeView = new Button("Voir l'univers");

    // Sous-page « Améliorations »
    private final VBox upgradesPane = new VBox(10);
    private final ScrollPane upgradesScroll = new ScrollPane(upgradesPane);
    private final Label upgradesIntro = new Label();
    private final TileGrid upgradesGrid = new TileGrid(230, 3, 8);
    private final Map<SpaceUpgrade, Card> upgradeCards = new LinkedHashMap<>();

    // Sous-page « Paliers » : les cinq paliers de Big Bang
    private final VBox milestonesPane = new VBox(10);
    private final ScrollPane milestonesScroll = new ScrollPane(milestonesPane);
    private final Label milestonesIntro = new Label();
    private final TileGrid milestonesGrid = new TileGrid(230, 3, 8);
    private final Map<BigBangMilestone, Card> milestoneCards = new LinkedHashMap<>();

    // Sous-page « États de la matière »
    private final VBox statesPane = new VBox(10);
    private final ScrollPane statesScroll = new ScrollPane(statesPane);
    private final Label statesIntro = new Label();
    /** Un lieu par état : son titre, et la grille des molécules qui s'y rassemblent. */
    private final Map<Molecule.State, Label> placeTitles = new EnumMap<>(Molecule.State.class);
    private final Map<Molecule.State, TileGrid> placeGrids = new EnumMap<>(Molecule.State.class);
    private final Map<Molecule, Card> stateCards = new LinkedHashMap<>();

    // Sous-page « Assemblages »
    private final VBox assembliesPane = new VBox(10);
    private final ScrollPane assembliesScroll = new ScrollPane(assembliesPane);
    private final Label assembliesIntro = new Label();
    /** Une section par famille : son titre, et la grille de ses assemblages. */
    private final Map<Assembly.Family, Label> familyTitles = new EnumMap<>(Assembly.Family.class);
    private final Map<Assembly.Family, TileGrid> familyGrids = new EnumMap<>(Assembly.Family.class);
    private final Map<Assembly, Card> assemblyCards = new LinkedHashMap<>();

    BigBangPage(Game game) {
        super(8);
        this.game = game;
        this.spaceView = new SpaceView(game);
        this.bodiesPane = new BodiesPane(game, this::refresh);
        this.galaxyPane = new GalaxyPane(game, this::refresh, () -> {
            // La plus grande échelle formée : la galaxie, ou plus loin, l'amas de galaxies, l'univers.
            spaceView.showGalaxy(true);
            spaceView.showCosmos(game.cosmosFormed() >= 2 ? idle.core.Cosmos.values()[game.cosmosFormed() - 1] : null);
            spaceView.fit();
            select(1);
            refresh();
        });
        setAlignment(Pos.TOP_CENTER);
        setPadding(new Insets(16, 24, 16, 24));

        countLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + GameApp.BIG_BANG_COLOR + ";");
        sinceLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #8fa3b8;");
        sinceLabel.setWrapText(true);
        sinceLabel.setTextAlignment(TextAlignment.CENTER);

        moleculesTab.setOnAction(event -> select(0));
        expansionTab.setOnAction(event -> select(1));
        upgradesTab.setOnAction(event -> select(2));
        statesTab.setOnAction(event -> select(3));
        assembliesTab.setOnAction(event -> select(4));
        bodiesTab.setOnAction(event -> select(5));
        galaxyTab.setOnAction(event -> select(6));
        milestonesTab.setOnAction(event -> select(7));
        for (Button tab : new Button[] {moleculesTab, expansionTab, upgradesTab, milestonesTab, statesTab, assembliesTab, bodiesTab, galaxyTab}) {
            tab.setFocusTraversable(false);
        }
        FlowPane subTabs = new FlowPane(0, 0, moleculesTab, expansionTab, upgradesTab, milestonesTab, statesTab, assembliesTab, bodiesTab, galaxyTab);
        subTabs.setAlignment(Pos.CENTER);

        // Les molécules : une phrase, les rayons du catalogue, puis les cartes du rayon choisi.
        moleculesPane.setAlignment(Pos.TOP_CENTER);
        moleculesPane.setPadding(new Insets(10, 0, 10, 0));
        note(moleculesIntro);
        FlowPane chips = new FlowPane(6, 6);
        chips.setAlignment(Pos.CENTER);
        for (Molecule.Kind each : Molecule.Kind.values()) {
            Button chip = new Button();
            chip.setFocusTraversable(false);
            chip.setOnAction(event -> selectKind(each));
            kindChips.put(each, chip);
            chips.getChildren().add(chip);
        }
        chips.setMaxWidth(820);
        moleculeFilter = new FilterBar("Rechercher une molécule : nom ou formule", GameApp.BIG_BANG_COLOR, this, this::moleculeFilterChanged);
        Map<Molecule.State, String> stateNames = new LinkedHashMap<>();
        for (Molecule.State state : Molecule.State.values()) stateNames.put(state, placeTitle(state));
        stateFilter = moleculeFilter.group("Tous les types", stateNames);
        Map<Gives, String> givesNames = new LinkedHashMap<>();
        givesNames.put(Gives.SPACE, "Espace");
        givesNames.put(Gives.PARTICLES, "Particules");
        givesNames.put(Gives.ATOMS, "Atomes");
        givesNames.put(Gives.DARK_GROWTH, "Matière noire");
        givesNames.put(Gives.UNCAP, "Plafond d'un élément");
        givesFilter = moleculeFilter.group("Toutes les améliorations", givesNames);
        note(moleculesFound);
        nextKindLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + GameApp.BIG_BANG_COLOR + ";");
        nextKindLabel.setWrapText(true);
        nextKindLabel.setTextAlignment(TextAlignment.CENTER);
        nextKindLabel.setMaxWidth(760);
        gridHolder.setMaxWidth(900);
        moleculesPane.getChildren().add(moleculesIntro);
        moleculesPane.getChildren().add(moleculeFilter);
        moleculesPane.getChildren().add(chips);
        moleculesPane.getChildren().add(moleculesFound);
        moleculesPane.getChildren().add(nextKindLabel);
        moleculesPane.getChildren().add(gridHolder);
        transparent(moleculesScroll);

        // L'expansion : les nombres, la vue de l'espace, et de quoi s'en approcher ou s'en éloigner.
        expansionPane.setAlignment(Pos.TOP_CENTER);
        expansionPane.setPadding(new Insets(10, 0, 0, 0));
        spaceLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #fffaf0;");
        rateLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + GameApp.BIG_BANG_COLOR + ";");
        rateLabel.setWrapText(true);
        rateLabel.setTextAlignment(TextAlignment.CENTER);
        note(expansionNote);
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
            spaceView.showCosmos(idle.core.Cosmos.CLUSTER);
            refresh();
        });
        universeView.setOnAction(event -> {
            spaceView.showCosmos(idle.core.Cosmos.UNIVERSE);
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
        expansionPane.getChildren().add(expansionNote);
        expansionPane.getChildren().add(viewPane);
        expansionPane.getChildren().add(zoomRow);

        // Les améliorations d'espace : une carte chacune, montrée quand elle devient accessible.
        upgradesPane.setAlignment(Pos.TOP_CENTER);
        upgradesPane.setPadding(new Insets(10, 0, 10, 0));
        note(upgradesIntro);
        for (SpaceUpgrade upgrade : game.spaceUpgrades()) {
            Card card = new Card(GameApp.BIG_BANG_COLOR);
            card.setOnAction(() -> {
                game.buySpaceUpgrade(upgrade.id());
                refresh();
            });
            upgradeCards.put(upgrade, card);
            upgradesGrid.add(card);
        }
        upgradesGrid.setMaxWidth(820);
        upgradesPane.getChildren().add(upgradesIntro);
        upgradesPane.getChildren().add(upgradesGrid);
        transparent(upgradesScroll);

        // Les paliers de Big Bang : cinq cartes, toujours montrées, qui ne se cliquent pas.
        milestonesPane.setAlignment(Pos.TOP_CENTER);
        milestonesPane.setPadding(new Insets(10, 0, 10, 0));
        note(milestonesIntro);
        for (BigBangMilestone milestone : game.bigBangMilestones()) {
            Card card = new Card(GameApp.BIG_BANG_COLOR);
            milestoneCards.put(milestone, card);
            milestonesGrid.add(card);
        }
        milestonesGrid.setMaxWidth(820);
        milestonesPane.getChildren().add(milestonesIntro);
        milestonesPane.getChildren().add(milestonesGrid);
        transparent(milestonesScroll);

        // Les états de la matière : un lieu par état, et dans chacun une carte par molécule qui s'y
        // rassemble. Une carte n'est construite que lorsque le joueur a créé sa molécule ({@link #stateCard}).
        statesPane.setAlignment(Pos.TOP_CENTER);
        statesPane.setPadding(new Insets(10, 0, 10, 0));
        note(statesIntro);
        statesPane.getChildren().add(statesIntro);
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
        transparent(statesScroll);

        // Les assemblages : une section par famille, et dans chacune une carte par assemblage, construite
        // quand le joueur a créé un de ses ingrédients ({@link #assemblyCard}).
        assembliesPane.setAlignment(Pos.TOP_CENTER);
        assembliesPane.setPadding(new Insets(10, 0, 10, 0));
        note(assembliesIntro);
        assembliesPane.getChildren().add(assembliesIntro);
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
        transparent(assembliesScroll);

        // Les sous-pages sont empilées au même endroit ; une seule est visible à la fois.
        StackPane pages = new StackPane(moleculesScroll, expansionPane, upgradesScroll, statesScroll, assembliesScroll, bodiesPane,
                galaxyPane, milestonesScroll);
        VBox.setVgrow(pages, Priority.ALWAYS);

        getChildren().add(countLabel);
        getChildren().add(sinceLabel);
        getChildren().add(subTabs);
        getChildren().add(pages);
        selectKind(kind);
        select(0);
    }

    /** Une zone qui défile en hauteur seulement, sans fond propre. */
    private static void transparent(ScrollPane scroll) {
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
    }

    /** Une phrase d'explication : discrète, centrée, qui passe à la ligne. */
    private static void note(Label label) {
        label.setStyle("-fx-font-size: 12px; -fx-text-fill: #8fa3b8;");
        label.setWrapText(true);
        label.setTextAlignment(TextAlignment.CENTER);
        label.setMaxWidth(760);
    }

    /**
     * Affiche une sous-page : 0 = les molécules, 1 = l'expansion de la matière, 2 = les améliorations,
     * 3 = les états de la matière, 4 = les assemblages, 5 = les astres, 6 = l'univers (galaxie, amas de galaxies, univers),
     * 7 = les paliers de Big Bang.
     */
    void select(int index) {
        selected = index;
        // Une recherche en cours d'écriture ne garde pas le clavier d'une sous-page à l'autre.
        if (moleculeFilter != null) moleculeFilter.release();
        moleculesScroll.setVisible(index == 0);
        expansionPane.setVisible(index == 1);
        upgradesScroll.setVisible(index == 2);
        statesScroll.setVisible(index == 3);
        assembliesScroll.setVisible(index == 4);
        bodiesPane.setVisible(index == 5);
        galaxyPane.setVisible(index == 6);
        milestonesScroll.setVisible(index == 7);
        moleculesTab.setStyle(subTabStyle(index == 0));
        expansionTab.setStyle(subTabStyle(index == 1));
        upgradesTab.setStyle(subTabStyle(index == 2));
        statesTab.setStyle(subTabStyle(index == 3));
        assembliesTab.setStyle(subTabStyle(index == 4));
        bodiesTab.setStyle(subTabStyle(index == 5));
        galaxyTab.setStyle(subTabStyle(index == 6));
        milestonesTab.setStyle(subTabStyle(index == 7));
    }

    /** La sous-page affichée, numérotée comme dans {@link #select(int)}. */
    int selected() {
        return selected;
    }

    private static String subTabStyle(boolean selected) {
        return SUB_TAB_STYLE + (selected
                ? " -fx-text-fill: " + GameApp.BIG_BANG_COLOR + "; -fx-border-color: transparent transparent "
                        + GameApp.BIG_BANG_COLOR + " transparent;"
                : " -fx-text-fill: #8fa3b8; -fx-border-color: transparent;");
    }

    /** Affiche un rayon du catalogue ; ses cartes sont construites à la première visite. */
    void selectKind(Molecule.Kind wanted) {
        kind = wanted;
        TileGrid grid = grids.computeIfAbsent(wanted, this::build);
        gridHolder.getChildren().clear();
        gridHolder.getChildren().add(grid);
        kindChips.forEach((each, chip) -> chip.setStyle(CHIP_STYLE + (each == wanted
                ? " -fx-text-fill: #10151f; -fx-background-color: " + GameApp.BIG_BANG_COLOR + "; -fx-border-color: #ffffff;"
                : " -fx-text-fill: " + GameApp.BIG_BANG_COLOR + "; -fx-background-color: #121923; -fx-border-color: "
                        + GameApp.BIG_BANG_COLOR + "66;")));
    }

    /** Vrai si cette molécule passe les filtres : son état, ce qu'elle donne, et le texte cherché. */
    boolean shown(Molecule molecule) {
        Molecule.State state = stateFilter.selected();
        if (state != null && molecule.state() != state) return false;
        Gives gives = givesFilter.selected();
        if (gives != null && gives != gives(molecule)) return false;
        return moleculeFilter.matches(searchTexts.computeIfAbsent(molecule,
                each -> FilterBar.normalized(each.name() + " " + each.formula() + " " + each.id())));
    }

    /** Ce que donne une molécule, pour le filtre ; {@code null} si elle ne donne rien. */
    private static Gives gives(Molecule molecule) {
        return switch (molecule.bonus()) {
            case Molecule.Boost boost -> switch (boost.stat()) {
                case SPACE -> Gives.SPACE;
                case PARTICLES -> Gives.PARTICLES;
                case ATOMS -> Gives.ATOMS;
                case DARK_GROWTH -> Gives.DARK_GROWTH;
            };
            case Molecule.Uncap uncap -> Gives.UNCAP;
            case null -> null;
        };
    }

    /** Nombre de molécules d'un rayon qui passent les filtres. */
    private int shownIn(Molecule.Kind wanted) {
        int found = 0;
        for (Molecule molecule : game.molecules()) {
            if (molecule.kind() == wanted && shown(molecule)) found++;
        }
        return found;
    }

    /**
     * Un filtre ou la recherche vient de changer. Si plus rien ne correspond dans le rayon affiché,
     * la page passe au premier rayon ouvert où quelque chose correspond : on cherche une molécule,
     * pas un rayon.
     */
    private void moleculeFilterChanged() {
        if (givesFilter == null) return;      // les pastilles se peignent pendant la construction de la page
        if (moleculeFilter.active() && shownIn(kind) == 0) {
            for (Molecule.Kind each : Molecule.Kind.values()) {
                if (game.isMoleculeKindUnlocked(each) && shownIn(each) > 0) {
                    selectKind(each);
                    break;
                }
            }
        }
        refresh();
    }

    /** La barre de recherche des molécules : pour les vérifications. */
    FilterBar moleculeFilter() {
        return moleculeFilter;
    }

    /** Le rayon affiché : pour les vérifications. */
    Molecule.Kind kind() {
        return kind;
    }

    /** Les cartes d'un rayon, dans l'ordre du catalogue, chacune avec le dessin de sa molécule. */
    private TileGrid build(Molecule.Kind wanted) {
        TileGrid grid = new TileGrid(200, 4, 8);
        for (Molecule molecule : game.molecules()) {
            if (molecule.kind() != wanted) continue;
            Card card = new Card(GameApp.BIG_BANG_COLOR);
            card.setPicture(picture(molecule));
            card.setOnAction(() -> {
                game.createMolecule(molecule.id());
                refresh();
            });
            cards.put(molecule, card);
            grid.add(card);
        }
        return grid;
    }

    /** Le dessin d'une molécule, fait une fois pour toutes, centré dans la largeur de sa carte. */
    private static HBox picture(Molecule molecule) {
        Canvas canvas = new Canvas(PICTURE_WIDTH, PICTURE_HEIGHT);
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.clearRect(0, 0, PICTURE_WIDTH, PICTURE_HEIGHT);
        MoleculeArt.Shape shape = MoleculeArt.of(molecule);
        // Les petites molécules ne sont pas gonflées jusqu'au bord : leurs atomes gardent une taille raisonnable.
        double radius = Math.min(PICTURE_HEIGHT / 2 - 3, shape.extent() * 24);
        shape.draw(g, PICTURE_WIDTH / 2, PICTURE_HEIGHT / 2, radius, -Math.PI / 6);
        HBox box = new HBox(canvas);
        box.setAlignment(Pos.CENTER);
        box.setMouseTransparent(true);     // le clic va à la carte
        return box;
    }

    /** La carte d'une molécule, ou {@code null} si son rayon n'a pas encore été ouvert : pour les vérifications. */
    Card card(Molecule molecule) {
        return cards.get(molecule);
    }

    /** La sous-page des astres : pour les vérifications. */
    BodiesPane bodiesPane() {
        return bodiesPane;
    }

    /** La sous-page de la galaxie : pour les vérifications. */
    GalaxyPane galaxyPane() {
        return galaxyPane;
    }

    /** La vue de l'espace : pour les vérifications. */
    SpaceView spaceView() {
        return spaceView;
    }

    /** Le jeu recommence de zéro : retour à la première sous-page et au premier rayon. */
    void reset() {
        selectKind(Molecule.Kind.SIMPLE);
        spaceView.fit();
        select(0);
    }

    /**
     * Fait avancer l'animation de la vue de l'espace, quand elle est affichée.
     *
     * @param elapsed secondes écoulées depuis l'image précédente
     */
    void frame(double elapsed) {
        if (selected == 1) spaceView.frame(elapsed);
        if (selected == 5) bodiesPane.frame(elapsed);
        if (selected == 6) galaxyPane.frame(elapsed);
    }

    /** Recopie l'état du jeu dans les textes. */
    void refresh() {
        int count = game.bigBangs();
        countLabel.setText(count + (count > 1 ? " Big Bangs" : " Big Bang"));
        sinceLabel.setText("Partie en cours depuis " + Format.duration(game.stats().runTime())
                + ". Le prochain se déclenche au bout de l'arbre de matière noire.");
        int created = game.moleculesCreated();
        moleculesTab.setText(created > 0 ? "Molécules (" + created + ")" : "Molécules");
        milestonesTab.setText("Paliers (" + game.bigBangMilestonesReached() + "/" + game.bigBangMilestones().size() + ")");
        // Le rassemblement n'a son onglet qu'une fois ouvert ; si on le regardait et qu'il se referme (remise à zéro), retour au début.
        boolean states = game.isStatesUnlocked();
        statesTab.setVisible(states);
        statesTab.setManaged(states);
        int formed = game.substancesFormed();
        statesTab.setText(formed > 0 ? "États de la matière (" + formed + ")" : "États de la matière");
        if (selected == 3 && !states) select(0);
        boolean assemblies = game.isAssembliesUnlocked();
        assembliesTab.setVisible(assemblies);
        assembliesTab.setManaged(assemblies);
        int assembled = game.assembliesFormed();
        assembliesTab.setText(assembled > 0 ? "Assemblages (" + assembled + ")" : "Assemblages");
        if (selected == 4 && !assemblies) select(0);
        boolean bodies = game.isBodiesUnlocked();
        bodiesTab.setVisible(bodies);
        bodiesTab.setManaged(bodies);
        int sky = game.bodiesFormed();
        bodiesTab.setText(sky > 0 ? "Astres (" + sky + ")" : "Astres");
        if (selected == 5 && !bodies) select(0);
        // La galaxie n'a son onglet qu'à la première étoile.
        boolean galaxy = game.isGalaxyUnlocked();
        galaxyTab.setVisible(galaxy);
        galaxyTab.setManaged(galaxy);
        galaxyTab.setText(game.cosmosFormed() == 0 ? "Univers" : game.nextCosmos() == null ? "Univers (formé)"
                : "Univers (" + game.cosmosFormed() + "/" + idle.core.Cosmos.values().length + ")");
        if (selected == 6 && !galaxy) select(0);
        switch (selected) {
            case 0 -> refreshMolecules();
            case 1 -> refreshExpansion();
            case 2 -> refreshUpgrades();
            case 3 -> refreshStates();
            case 4 -> refreshAssemblies();
            case 5 -> bodiesPane.refresh();
            case 6 -> galaxyPane.refresh();
            default -> refreshMilestones();
        }
    }

    /** Les cinq paliers de Big Bang : ceux qui sont atteints, le prochain, et ceux qui attendent. */
    private void refreshMilestones() {
        int count = game.bigBangs();
        BigBangMilestone next = game.nextBigBangMilestone();
        milestonesIntro.setText(next == null
                ? "Les " + game.bigBangMilestones().size() + " paliers sont atteints. Un Big Bang de plus ajoute encore "
                        + ElementText.number(Game.SPACE_PER_SECOND) + " unité d'espace par seconde, rien d'autre."
                : "Chaque Big Bang déclenché, jusqu'au cinquième, donne un gros bonus pour de bon, parfois deux (" + count
                        + (count > 1 ? " Big Bangs déclenchés" : " Big Bang déclenché") + ")."
                        + Detail.only(" Rien à acheter : un palier agit dès que son Big Bang a eu lieu, et ni l'explosion ni un "
                                + "autre Big Bang ne le reprennent. C'est ce qui paie le détour : après un Big Bang tout repart "
                                + "d'un seul générateur, et le tableau périodique reste vide quelques heures, donc sans nouvelle "
                                + "molécule. Chaque Big Bang ajoute aussi, comme toujours, "
                                + ElementText.number(Game.SPACE_PER_SECOND) + " unité d'espace par seconde."));
        milestoneCards.forEach((milestone, card) -> {
            boolean reached = game.isBigBangMilestoneReached(milestone);
            // Un même Big Bang peut donner deux paliers : tous ceux du prochain Big Bang sont « à venir ».
            boolean coming = next != null && milestone.bigBangs() == next.bigBangs();
            StringBuilder line = new StringBuilder();
            StringBuilder detail = new StringBuilder();
            for (BigBangMilestone.Effect effect : milestone.effects()) {
                if (line.length() > 0) line.append("\n");
                if (detail.length() > 0) detail.append(' ');
                line.append(milestoneLine(effect));
                detail.append(milestoneDetail(effect));
            }
            int missing = milestone.bigBangs() - count;
            card.show(reached ? Card.State.DONE : coming ? Card.State.WAITING : Card.State.LOCKED,
                    rank(milestone.bigBangs()) + " Big Bang", reached ? "atteint" : "", milestone.name(), line.toString(),
                    Detail.shown() ? detail.toString() : "",
                    reached ? "" : coming && missing == 1 ? "Au prochain Big Bang" : "Dans " + missing + " Big Bangs", "");
        });
    }

    /** « la racine carrée », ou « 2 fois la racine carrée » une fois le palier de la gravité atteint. */
    private String root() {
        double factor = game.accretionFactor();
        return factor == 1 ? "la racine carrée" : ElementText.number(factor) + " fois la racine carrée";
    }

    /** Ce qu'une création ajoute à un amas de {@code count} molécules, avec les paliers atteints. */
    private int drawnAt(int count) {
        return 1 + (int) Math.floor(game.accretionFactor() * Math.sqrt(count) + 1e-9);
    }

    /** « 1er », « 2e », « 3e ». */
    private static String rank(int number) {
        return number == 1 ? "1er" : number + "e";
    }

    /** Ce que donne un palier, en quelques mots : « Espace par seconde ×2 ». */
    static String milestoneLine(BigBangMilestone.Effect effect) {
        return switch (effect) {
            case BigBangMilestone.Boost boost -> what(boost.stat()) + " ×" + ElementText.number(boost.factor());
            case BigBangMilestone.DarkMatter dark -> "Matière noire des explosions ×" + ElementText.number(dark.factor());
            case BigBangMilestone.Accretion accretion -> "Les amas attirent ×" + ElementText.number(accretion.factor());
            case BigBangMilestone.DarkMatterSpace dark -> "La matière noire en réserve accélère l'expansion";
            case BigBangMilestone.AutoMolecules auto -> "Les amas choisis se créent tout seuls";
            case BigBangMilestone.KeepDarkTree keep -> "L'arbre de matière noire traverse le Big Bang";
        };
    }

    private String milestoneDetail(BigBangMilestone.Effect effect) {
        return switch (effect) {
            case BigBangMilestone.Boost boost -> switch (boost.stat()) {
                case SPACE -> "L'expansion crée " + ElementText.number(boost.factor()) + " fois plus d'espace chaque seconde. "
                        + "Cela se multiplie avec les inflations, les molécules, les assemblages et les astres.";
                case ATOMS -> "Chaque fusion donne " + ElementText.number(boost.factor()) + " fois plus d'atomes : le tableau "
                        + "périodique se regarnit bien plus vite après un Big Bang.";
                case PARTICLES -> "Chaque création donne " + ElementText.number(boost.factor()) + " fois plus de particules.";
                case DARK_GROWTH -> "La matière noire grossit " + ElementText.number(boost.factor()) + " fois plus vite.";
            };
            case BigBangMilestone.DarkMatter dark -> "Chaque explosion laisse " + ElementText.number(dark.factor())
                    + " fois plus de matière noire. L'arbre et le Big Bang suivant en demandent autant qu'avant : il faut donc "
                    + "bien moins d'explosions pour refaire le chemin.";
            case BigBangMilestone.Accretion accretion -> "Dans une sorte rassemblée, une création ajoute une molécule plus "
                    + ElementText.number(accretion.factor()) + " fois la racine carrée de l'amas, au lieu d'une fois : "
                    + "21 d'un coup à 100 molécules, 201 à 10 000.";
            case BigBangMilestone.DarkMatterSpace dark -> "L'espace par seconde est multiplié par 1 + "
                    + ElementText.number(dark.perRoot()) + " × la racine carrée de la matière noire en réserve : ×"
                    + ElementText.number(1 + dark.perRoot() * Math.sqrt(100)) + " avec 100, "
                    + "×" + ElementText.number(1 + dark.perRoot() * Math.sqrt(500)) + " avec 500, ×"
                    + ElementText.number(1 + dark.perRoot() * Math.sqrt(2_500)) + " avec 2 500"
                    + (game.darkMatterSpaceBoost() > 1 ? " (×" + ElementText.number(game.darkMatterSpaceBoost()) + " en ce moment)" : "")
                    + ". L'arbre fini, chaque explosion de plus sert donc encore ; dépenser la matière noire ralentit "
                    + "l'expansion, et un Big Bang la reprend : ce bonus repart alors de rien, le temps qu'elle revienne.";
            case BigBangMilestone.AutoMolecules auto -> "Donne l'automatisme « Création automatique », à régler dans l'onglet "
                    + "Automatisation. Dans la sous-page États de la matière, un clic sur un amas le lui confie : toutes les "
                    + ElementText.number(Game.MOLECULE_AUTOMATION_SECONDS) + " secondes, il y fait une création, au même prix "
                    + "qu'à la main, tant que le tableau périodique et l'espace le permettent.";
            case BigBangMilestone.KeepDarkTree keep -> "Dès ce Big Bang-là, et à tous les suivants, l'arbre de matière noire "
                    + "reste en place : ses cases, ses automatismes et leurs réglages. Le Big Bang ne reprend plus que la "
                    + "matière noire en réserve, sa taille, la masse du tableau et les défis réussis. Le chemin à refaire "
                    + "tombe de quelques heures à un quart d'heure, et toute la matière noire des explosions va à la "
                    + "réserve, donc à l'expansion.";
        };
    }

    private void refreshUpgrades() {
        upgradesIntro.setText("Une amélioration se prend quand l'expansion a créé assez d'espace ("
                + Format.count(game.state().space()) + " en ce moment). Elle ne coûte rien."
                + Detail.only(" C'est l'espace gagné depuis le premier Big Bang qui compte, pas celui qui reste libre : "
                        + "remplir l'espace de molécules et de leurs lieux de rassemblement ne retarde aucune amélioration. Les "
                        + "rayons de molécules s'ouvrent l'un après l'autre ; chacun n'est proposé qu'une fois le précédent "
                        + "pris. Les premières multiplient pour de bon les atomes, les particules et l'espace : après un Big "
                        + "Bang tout repart d'un seul générateur, et ce sont elles qui raccourcissent le chemin. Ni "
                        + "l'explosion ni le Big Bang ne reprennent une amélioration."));
        upgradeCards.forEach((upgrade, card) -> {
            boolean owned = game.ownsSpaceUpgrade(upgrade.id());
            boolean available = game.isSpaceUpgradeAvailable(upgrade.id());
            upgradesGrid.show(card, owned || available);
            if (!owned && !available) return;
            boolean ready = game.canBuySpaceUpgrade(upgrade.id());
            double wait = game.secondsUntilSpace(upgrade.space());
            String line = switch (upgrade.effect()) {
                case SpaceUpgrade.OpenKind open -> "Ouvre un rayon de " + kindSize(open.kind()) + " molécules";
                case SpaceUpgrade.OpenStates states -> "Rassembler les molécules selon leur état";
                case SpaceUpgrade.OpenAssemblies assemblies -> "Assembler des roches, des minerais, des eaux, des gaz";
                case SpaceUpgrade.OpenBodies bodies -> "Former des astres, de l'amas de roches au trou noir";
                case SpaceUpgrade.AutoHold hold -> "La matière noire grossit sans tenir le clic";
                case SpaceUpgrade.Boost boost -> what(boost.stat()) + " ×" + ElementText.number(boost.factor());
            };
            String detail = switch (upgrade.effect()) {
                case SpaceUpgrade.OpenKind open -> "Le rayon « " + open.kind().label() + " » apparaît dans la sous-page "
                        + "Molécules. Chacune de ses molécules donne un léger bonus ou relève le plafond d'un de ses "
                        + "éléments, et a son état : gaz, liquide, solide, cristal ou métal.";
                case SpaceUpgrade.OpenStates states -> "Ouvre la sous-page « États de la matière » : à partir de "
                        + Game.SUBSTANCE_MOLECULES + " molécules d'une même sorte, un achat unique les rassemble dans le "
                        + "lieu de leur état : gaz, liquides, solides, cristaux ou métaux. Aucune n'est consommée ; leur bonus est "
                        + "renforcé, la sorte ajoute des particules ou des atomes, et surtout son amas attire la matière : "
                        + "chaque création y ajoute plusieurs molécules d'un coup, de plus en plus.";
                case SpaceUpgrade.OpenAssemblies assemblies -> "Ouvre la sous-page « Assemblages » : plusieurs sortes de "
                        + "molécules rassemblées, par centaines, forment une matière de la nature : une roche, un minerai, "
                        + "une pierre précieuse, une eau, un gaz ou un hydrocarbure. Rien de vivant, rien de fabriqué. "
                        + "Aucune molécule n'est consommée.";
                case SpaceUpgrade.OpenBodies bodies -> "Ouvre la sous-page « Astres » : des assemblages, de la matière rassemblée et "
                        + "des molécules se cumulent pour former un amas de roches, puis une comète, un astéroïde, une lune, une "
                        + "planète, puis des étoiles et des trous noirs, chacun de plusieurs types et chacun parti des plus petits.";
                case SpaceUpgrade.AutoHold hold -> "Donne l'automatisme « Appui automatique », à régler dans l'onglet "
                        + "Automatisation : la matière noire grossit comme si vous teniez le clic sur son point, à pleine "
                        + "vitesse et depuis n'importe quel onglet. Il sert dès qu'il y a de la matière noire, donc après la "
                        + "première explosion qui suit un Big Bang, et ni l'explosion ni le Big Bang ne le reprennent.";
                case SpaceUpgrade.Boost boost -> switch (boost.stat()) {
                    case ATOMS -> "Chaque fusion donne " + ElementText.number(boost.factor()) + " fois plus d'atomes, pour de bon. "
                            + "Après un Big Bang il faut refaire tout le chemin depuis un seul générateur : avec ceci, le "
                            + "tableau périodique s'ouvre en quelques minutes et se remplit bien plus vite que la première fois.";
                    case PARTICLES -> "Chaque création donne " + ElementText.number(boost.factor()) + " fois plus de particules, "
                            + "pour de bon. Avec la Matière primordiale, c'est ce qui rend court le chemin jusqu'au Big Bang suivant.";
                    case SPACE -> "L'expansion crée " + ElementText.number(boost.factor()) + " fois plus d'espace chaque seconde, "
                            + "pour de bon. Les inflations se multiplient entre elles, puis avec les Big Bangs, les molécules, "
                            + "les assemblages et les astres qui agissent sur l'espace.";
                    case DARK_GROWTH -> "La matière noire grossit " + ElementText.number(boost.factor()) + " fois plus vite, pour de bon.";
                };
            };
            card.show(owned ? Card.State.DONE : ready ? Card.State.READY : Card.State.WAITING,
                    "", owned ? "acquise" : upgrade.effect() instanceof SpaceUpgrade.OpenKind ? "rayon"
                            : upgrade.effect() instanceof SpaceUpgrade.AutoHold ? "automatisme"
                            : upgrade.effect() instanceof SpaceUpgrade.Boost ? "pour de bon" : "",
                    upgrade.name(), line, detail,
                    owned ? "" : ready ? "Prendre" : "À " + Format.count(upgrade.space()) + " d'espace créé",
                    owned || ready || Double.isInfinite(wait) ? "" : "dans " + Format.wait(wait));
        });
    }

    /** Nombre de molécules d'un rayon du catalogue. */
    private int kindSize(Molecule.Kind wanted) {
        int size = 0;
        for (Molecule molecule : game.molecules()) {
            if (molecule.kind() == wanted) size++;
        }
        return size;
    }

    private void refreshStates() {
        Map<Molecule.State, int[]> places = new EnumMap<>(Molecule.State.class);    // par lieu : cartes montrées, sortes rassemblées, molécules rangées
        for (Molecule molecule : game.molecules()) {
            if (!molecule.hasState()) continue;
            Molecule.State matter = molecule.state();
            int molecules = game.moleculeCount(molecule.id());
            boolean gathered = game.hasSubstance(molecule.id());
            // Une molécule n'a sa carte ici que lorsque le joueur en a créé, ou les a déjà rassemblées.
            boolean visible = molecules > 0 || gathered;
            Card card = visible ? stateCard(molecule) : stateCards.get(molecule);
            if (card != null) placeGrids.get(matter).show(card, visible);
            if (!visible) continue;
            int[] place = places.computeIfAbsent(matter, each -> new int[3]);
            place[0]++;
            if (gathered) {
                place[1]++;
                place[2] += molecules;
            }
            boolean ready = game.canFormSubstance(molecule.id());
            // Ce qu'écrit la carte ne dépend que de ces quelques valeurs : tant qu'elles n'ont pas bougé, elle dit déjà ce
            // qu'il faut. Seul l'espace qui manque change à chaque image ; cette carte-là est toujours recomposée.
            boolean lacking = !gathered && !ready && molecules >= Game.SUBSTANCE_MOLECULES;
            // Ce qu'une création ajoute dépend aussi du palier de la gravité : il entre dans la clé.
            boolean automation = gathered && game.isMoleculeAutomationUnlocked();
            boolean automated = automation && game.isMoleculeAutomated(molecule.id());
            long key = lacking ? -1 : 64L * molecules + (gathered ? 1 : 0) + (ready ? 2 : 0) + (Detail.shown() ? 4 : 0)
                    + (game.accretionFactor() > 1 ? 8 : 0) + (automation ? 16 : 0) + (automated ? 32 : 0);
            if (card.filledWith(key) && !lacking) continue;
            BigNum space = game.substanceSpace(molecule.id());
            String power = ElementText.number(matter.exponent());
            // Un plafond ne se relève que d'exemplaires entiers : la carte montre ce qui compte vraiment.
            double effective = game.effectiveMolecules(molecule.id());
            String counted = ElementText.number(molecule.bonus() instanceof Molecule.Uncap ? Math.floor(effective + 1e-9) : effective);
            String whole = molecule.bonus() instanceof Molecule.Uncap ? ", arrondi en dessous pour un plafond" : "";
            int drawn = game.moleculesPerCreation(molecule.id());
            String line = gathered
                    ? molecules + (molecules > 1 ? " molécules comptées pour " : " molécule comptée pour ") + counted
                            + "\n" + capitalized(generation(matter, Math.max(1, effective)))
                            + "\nUne création en ajoute " + drawn
                    : "Renforce son bonus et attire la matière\n" + capitalized(generation(matter, 1)) + " par doublement";
            String doubled = " Une sorte compte pour ses doublements : 1 molécule compte pour 1, 3 pour 2, 7 pour 3, 15 pour 4, "
                    + "et ainsi de suite. ";
            String detail = !Detail.shown() ? "" : gathered
                    ? "Rassemblées dans " + placeName(matter) + "." + doubled + "Rassemblée, ce compte est élevé à la puissance "
                            + power + whole + " : c'est lui qui multiplie le bonus (" + bonus(molecule) + ") et ce que l'état "
                            + "ajoute (" + generation(matter, 1) + " par unité). L'amas attire la matière : chaque création y "
                            + "ajoute une molécule, plus " + root() + " de leur nombre, pour le prix d'une seule et s'il "
                            + "y a la place."
                    : "Un seul achat : toutes vos " + molecule.formula() + " vont se ranger dans " + placeName(matter)
                            + ", sans être consommées." + doubled + "Rassemblée, ce compte est élevé à la puissance " + power + whole
                            + " : 3 molécules comptent pour " + ElementText.number(Math.pow(Game.doublings(3), matter.exponent()))
                            + ", 100 pour " + ElementText.number(Math.pow(Game.doublings(100), matter.exponent()))
                            + ". La sorte ajoute aussi " + generation(matter, 1) + " par unité de ce compte. Enfin l'amas "
                            + "attire la matière : chaque création y ajoute une molécule, plus " + root() + " de leur "
                            + "nombre (" + drawnAt(9) + " d'un coup à 9 molécules, " + drawnAt(100) + " à 100, "
                            + drawnAt(10_000) + " à 10 000), pour le prix d'une seule.";
            String price = automated ? "Création automatique : retirer"
                    : automation ? "Confier à la création automatique"
                    : gathered ? ""
                    : ready ? "Rassembler : " + Format.count(space) + " d'espace"
                    : molecules < Game.SUBSTANCE_MOLECULES ? "Il faut " + Game.SUBSTANCE_MOLECULES + " " + molecule.formula()
                            + " (" + molecules + ")"
                    : "Il manque " + Format.count(space.subtract(game.freeSpace()).max(BigNum.ONE)) + " d'espace";
            card.setDoneClickable(automation);
            card.show(automated ? Card.State.ON : gathered ? Card.State.DONE : ready ? Card.State.READY : Card.State.WAITING,
                    "×" + molecules, automated ? "automatique" : gathered ? "rassemblées" : "", molecule.name(), line, detail, price, "");
        }
        int shown = 0;
        for (Molecule.State matter : Molecule.State.values()) {
            int[] place = places.getOrDefault(matter, new int[3]);
            shown += place[0];
            Label title = placeTitles.get(matter);
            title.setText(placeTitle(matter) + (place[1] == 0 ? " : rien de rassemblé"
                    : " : " + place[2] + (place[2] > 1 ? " molécules de " : " molécule de ") + place[1]
                            + (place[1] > 1 ? " sortes" : " sorte")));
            title.setVisible(place[0] > 0);
            title.setManaged(place[0] > 0);
        }
        statesIntro.setText(shown == 0
                ? "Créez d'abord une molécule : elle apparaîtra ici, dans le lieu où elle se rassemble."
                : "À partir de " + Game.SUBSTANCE_MOLECULES + " molécules d'une sorte, un achat unique les rassemble dans le lieu "
                        + "de leur état. Un amas attire la matière : chaque création y ajoute alors plusieurs molécules d'un "
                        + "coup (" + Format.count(game.freeSpace()) + " d'espace libre)."
                        + (!game.isMoleculeAutomationUnlocked() ? "" : " Un clic sur un amas le confie à la création automatique ("
                                + game.automatedMolecules() + (game.automatedMolecules() > 1 ? " amas confiés" : " amas confié")
                                + (game.isMoleculeAutomationEnabled() ? ")." : ", automatisme coupé)."))
                        + Detail.only(" Aucune molécule n'est consommée : elles se rangent toutes dans le lieu de leur état, "
                                + "visible dans l'expansion de la matière, les plus serrés au plus près du centre : les "
                                + "métaux, les cristaux, les solides, les liquides, puis les gaz. Le lieu réserve de l'espace une fois "
                                + "pour toutes : la place de " + Game.SUBSTANCE_MOLECULES + " de ses molécules. Une molécule de "
                                + "gaz occupe " + ElementText.number(Molecule.State.GAS.spaceFactor()) + " fois la place d'un "
                                + "cristal du même poids, un liquide " + ElementText.number(Molecule.State.LIQUID.spaceFactor())
                                + " fois, un métal les trois quarts. Une sorte compte pour ses doublements (1 molécule, puis "
                                + "3, 7, 15…), et rassemblée ce compte est élevé à la puissance de son état : "
                                + ElementText.number(Molecule.State.GAS.exponent()) + " pour un gaz, "
                                + ElementText.number(Molecule.State.METAL.exponent()) + " pour un métal. C'est surtout par "
                                + "l'amas que viennent les grands nombres : chaque création y ajoute une molécule, plus la "
                                + "racine carrée de celles qu'il contient. Seules les molécules que vous avez déjà créées "
                                + "sont montrées."));
    }

    /**
     * La carte d'une molécule dans le lieu de son état, construite la première fois qu'on en a
     * besoin : le catalogue compte plusieurs centaines de molécules, et le joueur n'en possède
     * jamais qu'une partie. Les cartes d'un lieu sont donc dans l'ordre où leurs molécules ont été créées.
     */
    private Card stateCard(Molecule molecule) {
        return stateCards.computeIfAbsent(molecule, each -> {
            Card card = new Card(GameApp.BIG_BANG_COLOR);
            card.setPicture(picture(molecule));
            // Pas encore rassemblée : le clic rassemble. Ensuite, avec la création automatique, il confie l'amas à l'automatisme.
            card.setOnAction(() -> {
                if (!game.hasSubstance(molecule.id())) game.formSubstance(molecule.id());
                else game.setMoleculeAutomated(molecule.id(), !game.isMoleculeAutomated(molecule.id()));
                refresh();
            });
            placeGrids.get(molecule.state()).add(card);
            return card;
        });
    }

    private void refreshAssemblies() {
        Map<Assembly.Family, int[]> families = new EnumMap<>(Assembly.Family.class);    // par famille : cartes montrées, assemblages formés
        for (Assembly assembly : game.assemblies()) {
            boolean formed = game.hasAssembly(assembly.id());
            // Un assemblage n'a sa carte que lorsque le joueur a créé au moins un de ses ingrédients, ou l'a déjà formé.
            boolean visible = formed;
            for (String ingredient : assembly.ingredients().keySet()) visible |= game.moleculeCount(ingredient) > 0;
            Card card = visible ? assemblyCard(assembly) : assemblyCards.get(assembly);
            if (card != null) familyGrids.get(assembly.family()).show(card, visible);
            if (!visible) continue;
            int[] family = families.computeIfAbsent(assembly.family(), each -> new int[2]);
            family[0]++;
            if (formed) family[1]++;
            boolean ready = game.canFormAssembly(assembly.id());
            // Ce que le joueur a de chaque ingrédient, hors des autres assemblages, et ce qui manque en premier.
            StringBuilder have = new StringBuilder();
            StringBuilder names = new StringBuilder();
            String missing = "";
            int complete = 0;
            for (Map.Entry<String, Integer> ingredient : assembly.ingredients().entrySet()) {
                Molecule molecule = game.molecule(ingredient.getKey());
                int need = ingredient.getValue();
                int spare = game.spareMolecules(molecule.id());
                if (have.length() > 0) have.append("  ·  ");
                have.append(molecule.formula()).append(' ').append(formed ? need : Math.min(spare, need) + "/" + need);
                if (names.length() > 0) names.append(", ");
                names.append(need).append(' ').append(molecule.name().toLowerCase());
                if (formed || spare >= need) {
                    complete++;
                } else if (missing.isEmpty()) {
                    missing = !game.hasSubstance(molecule.id())
                            ? (game.moleculeCount(molecule.id()) == 0 ? "Il faut créer " : "Il faut rassembler ") + molecule.formula()
                            : "Il manque " + (need - spare) + " " + molecule.formula();
                }
            }
            String gain = bonusOf(assembly);
            int sorts = assembly.ingredients().size();
            // La carte reste courte : le gain et la taille. La liste des ingrédients est pour le mode détails.
            card.show(formed ? Card.State.DONE : ready ? Card.State.READY : complete > 0 ? Card.State.STARTED : Card.State.WAITING,
                    formed ? "" : complete + "/" + sorts, formed ? "assemblé" : "", assembly.name(),
                    gain + "\n" + assembly.size() + " molécules de " + sorts + " sortes",
                    !Detail.shown() ? "" : (formed ? "Dans le bloc : " : "Ce que vous avez : ") + have + ". Fait de " + names + ". Les molécules ne sont "
                            + "pas consommées : elles quittent l'amas de leur sorte pour former un seul bloc, visible dans "
                            + "l'expansion de la matière, et gardent leur bonus. Un autre assemblage ne peut pas compter sur "
                            + "les mêmes molécules." + (game.isBodiesUnlocked() ? (game.hasBody(game.bodyOf(assembly.id()).id())
                                    ? " Ce bloc entoure maintenant un astre : " : " Il entre dans un astre : ")
                                    + game.bodyOf(assembly.id()).name().toLowerCase() + "." : ""),
                    formed ? "" : ready ? "Assembler" : missing, "");
        }
        int shown = 0;
        for (Assembly.Family family : Assembly.Family.values()) {
            int[] counts = families.getOrDefault(family, new int[2]);
            shown += counts[0];
            Label title = familyTitles.get(family);
            title.setText(family.label() + (counts[1] == 0 ? "" : " : " + counts[1] + (counts[1] > 1 ? " formés" : " formé")));
            title.setVisible(counts[0] > 0);
            title.setManaged(counts[0] > 0);
        }
        assembliesIntro.setText(shown == 0
                ? "Créez d'abord des molécules : les assemblages qu'elles permettent apparaîtront ici."
                : "Plusieurs sortes de molécules rassemblées, par centaines, forment une matière de la nature : une roche, un "
                        + "minerai, une eau, un gaz. Un achat unique, qui ne consomme rien."
                        + Detail.only(" Chaque ingrédient doit être rassemblé dans les états de la matière, et vous devez en "
                                + "avoir le nombre demandé hors de tout autre assemblage. Les proportions sont à peu près "
                                + "celles de la matière réelle : la base par centaines, les traces à l'unité. Un assemblage "
                                + "augmente la grandeur de sa famille de 100 % par "
                                + ElementText.number(Assembly.MOLECULES_PER_WHOLE) + " molécules. Rien de vivant ici, et rien de fabriqué. Seuls "
                                + "les assemblages dont vous avez créé au moins un ingrédient sont montrés."));
    }

    /** Ce que donne un assemblage, en quelques mots : « Atomes par fusion +135 % ». */
    static String bonusOf(Assembly assembly) {
        return what(assembly.boost().stat()) + " +" + ElementText.percent(assembly.boost().perMolecule());
    }

    /**
     * La carte d'un assemblage dans la section de sa famille, construite la première fois qu'on en
     * a besoin, avec un petit dessin de son bloc ({@link #blockPicture}).
     */
    private Card assemblyCard(Assembly assembly) {
        return assemblyCards.computeIfAbsent(assembly, each -> {
            Card card = new Card(GameApp.BIG_BANG_COLOR);
            card.setPicture(blockPicture(assembly));
            card.setOnAction(() -> {
                game.formAssembly(assembly.id());
                refresh();
            });
            familyGrids.get(assembly.family()).add(card);
            return card;
        });
    }

    /**
     * Le dessin d'un assemblage sur sa carte : un bloc de sa couleur, au contour clair, semé de
     * grains de la couleur de chacun de ses ingrédients, en proportion. C'est le même rendu que dans
     * la vue de l'espace ({@link SpaceView}), en petit.
     */
    private HBox blockPicture(Assembly assembly) {
        double width = PICTURE_WIDTH;
        double height = 46;
        Canvas canvas = new Canvas(width, height);
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.clearRect(0, 0, width, height);
        g.setFill(SpaceView.BLOCK_EDGE);
        g.fillRoundRect(8, 3, width - 16, height - 6, 22, 22);
        g.setFill(Color.web(assembly.tint()));
        g.fillRoundRect(10.5, 5.5, width - 21, height - 11, 18, 18);
        // Trois rangées de grains, dont les couleurs suivent les proportions de l'assemblage, mêlées d'une façon fixe.
        int columns = 13;
        int rows = 3;
        java.util.List<Color> grains = new java.util.ArrayList<>();
        for (Map.Entry<String, Integer> ingredient : assembly.ingredients().entrySet()) {
            Color color = MoleculeArt.of(game.molecule(ingredient.getKey())).dominant();
            int share = Math.max(1, (int) Math.round(columns * rows * ingredient.getValue() / (double) assembly.size()));
            for (int grain = 0; grain < share; grain++) grains.add(color);
        }
        java.util.Collections.shuffle(grains, new java.util.Random(assembly.id().hashCode()));
        double[] xs = new double[4];
        double[] ys = new double[4];
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                int index = row * columns + column;
                if (index >= grains.size()) break;
                double x = 21 + column * (width - 42) / (columns - 1) + (row % 2 == 0 ? 0 : 4);
                double y = 13 + row * 10;
                SpaceView.grain(g, grains.get(index), x, y, 3.2, index * 0.9, xs, ys);
            }
        }
        HBox box = new HBox(canvas);
        box.setAlignment(Pos.CENTER);
        box.setMouseTransparent(true);     // le clic va à la carte
        return box;
    }

    /** Le titre d'un lieu de rassemblement : « Gaz ». */
    static String placeTitle(Molecule.State matter) {
        return switch (matter) {
            case GAS -> "Gaz";
            case LIQUID -> "Liquides";
            case SOLID -> "Solides";
            case CRYSTAL -> "Cristaux";
            case METAL -> "Métaux";
        };
    }

    /** Le lieu d'un état, dans une phrase : « le lieu des gaz ». */
    private static String placeName(Molecule.State matter) {
        return switch (matter) {
            case GAS -> "le lieu des gaz";
            case LIQUID -> "le lieu des liquides";
            case SOLID -> "le lieu des solides";
            case CRYSTAL -> "le lieu des cristaux";
            case METAL -> "le lieu des métaux";
        };
    }

    /** Ce qu'ajoute une sorte rassemblée dans un état quand elle compte pour {@code counted} : « +15 % de particules et +6 % d'atomes ». */
    private static String generation(Molecule.State matter, double counted) {
        String particles = matter.particles() > 0 ? "+" + ElementText.percent(matter.particles() * counted) + " de particules" : "";
        String atoms = matter.atoms() > 0 ? "+" + ElementText.percent(matter.atoms() * counted) + " d'atomes" : "";
        return particles.isEmpty() ? atoms : atoms.isEmpty() ? particles : particles + " et " + atoms;
    }

    private static String capitalized(String text) {
        return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    private void refreshMolecules() {
        moleculesIntro.setText("Une molécule se crée avec des exemplaires du tableau périodique et de l'espace libre ("
                + Format.count(game.freeSpace()) + " en ce moment), et reste pour toujours."
                + Detail.only(" Les exemplaires quittent le tableau avec ce qu'ils y apportaient ; un exemplaire de "
                        + "chaque élément y reste, pour que le tableau puisse toujours exploser. Une création demande "
                        + "toujours la formule, une fois : il suffit de regarnir le tableau pour recommencer. Une molécule "
                        + "occupe " + ElementText.number(Game.SPACE_PER_PROTON) + " unités d'espace par proton, que "
                        + "l'expansion de la matière doit avoir créées : plus elle est complexe ou faite d'éléments lourds, "
                        + "plus elle en prend ; un liquide en prend le double, un gaz "
                        + ElementText.number(Molecule.State.GAS.spaceFactor()) + " fois plus. Ni l'explosion ni le Big Bang "
                        + "ne reprennent une molécule. Chaque sorte donne quelque chose : un léger bonus, ou un exemplaire "
                        + "de plus au maximum d'un de ses éléments. Elle le donne à la première molécule, puis chaque fois "
                        + "que leur nombre double (3, 7, 15…) : mieux vaut beaucoup de sortes qu'une seule énorme. Une sorte "
                        + "rassemblée (États de la matière) attire la matière : chaque création y ajoute plusieurs "
                        + "molécules d'un coup."));
        // Les rayons : combien de leurs molécules ont été créées au moins une fois.
        boolean filtered = moleculeFilter.active();
        Map<Molecule.Kind, int[]> totals = new EnumMap<>(Molecule.Kind.class);
        for (Molecule molecule : game.molecules()) {
            int[] total = totals.computeIfAbsent(molecule.kind(), each -> new int[3]);
            total[1]++;
            if (game.moleculeCount(molecule.id()) > 0) total[0]++;
            if (filtered && shown(molecule)) total[2]++;
        }
        // Un rayon fermé n'est pas proposé du tout ; celui qu'on regardait a pu se refermer avec une remise à zéro.
        if (!game.isMoleculeKindUnlocked(kind)) selectKind(Molecule.Kind.SIMPLE);
        kindChips.forEach((each, chip) -> {
            int[] total = totals.getOrDefault(each, new int[3]);
            // Pendant une recherche, chaque rayon dit combien de ses molécules correspondent.
            chip.setText(each.label() + "  " + (filtered ? total[2] + " sur " + total[1] : total[0] + "/" + total[1]));
            boolean open = game.isMoleculeKindUnlocked(each);
            chip.setVisible(open);
            chip.setManaged(open);
        });
        nextKindLabel.setText(nextKind());
        nextKindLabel.setVisible(!nextKindLabel.getText().isEmpty());
        nextKindLabel.setManaged(!nextKindLabel.getText().isEmpty());
        int[] here = totals.getOrDefault(kind, new int[3]);
        moleculesFound.setText(!filtered ? "" : here[2] == 0 ? "Aucune molécule ne correspond, dans aucun rayon ouvert."
                : here[2] + (here[2] > 1 ? " molécules sur " : " molécule sur ") + here[1] + " dans ce rayon");
        moleculesFound.setVisible(filtered);
        moleculesFound.setManaged(filtered);
        TileGrid grid = grids.get(kind);
        cards.forEach((molecule, card) -> {
            if (molecule.kind() != kind) return;      // seul le rayon affiché se met à jour
            boolean match = !filtered || shown(molecule);
            grid.show(card, match);
            if (!match) return;
            int count = game.moleculeCount(molecule.id());
            boolean reachable = game.isMoleculeWithinReach(molecule.id());
            boolean elements = game.hasElementsForMolecule(molecule.id());
            boolean ready = game.canCreateMolecule(molecule.id());
            Map<Integer, Integer> cost = game.nextMoleculeCost(molecule.id());
            BigNum volume = game.moleculeVolume(molecule.id());
            // Une sorte rassemblée attire la matière : une création y ajoute plusieurs molécules, s'il y a la place.
            int drawn = game.moleculesPerCreation(molecule.id());
            int next = ready ? game.moleculesNextCreation(molecule.id()) : drawn;
            // L'explication n'est composée que si elle va s'afficher.
            String detail = !Detail.shown() ? "" : molecule.atoms() + (molecule.atoms() > 1 ? " atomes, " : " atome, ") + molecule.protons()
                    + " protons. Formule : " + recipe(molecule.recipe(), false) + "."
                    + (drawn > 1 ? " Rassemblée : une création en ajoute " + drawn + " d'un coup, pour le prix d'une seule"
                            + (next < drawn ? " (" + next + " seulement, faute de place)." : ".") : "")
                    + given(molecule, count);
            String price = ready ? (next > 1 ? "Créer ×" + next + " : " : "Créer : ") + recipe(cost, false)
                    : !reachable ? "Hors de portée : " + recipe(cost, false)
                    : !elements ? "Il faut " + recipe(cost, true)
                    : "Il manque " + Format.count(volume.subtract(game.freeSpace()).max(BigNum.ONE)) + " d'espace";
            card.show(ready ? Card.State.READY : !reachable ? Card.State.LOCKED
                            : count > 0 ? Card.State.STARTED : Card.State.WAITING,
                    count > 0 ? "×" + count : "", molecule.formula(), molecule.name(),
                    (molecule.hasBonus() ? bonus(molecule) + "\n" : "") + "Occupe " + Format.count(volume) + " d'espace",
                    detail, price, "");
        });
    }

    /** « Prochain rayon : Sels, dans les améliorations (à 10 000 d'espace créé, dans 2 h 10). » Vide quand tous les rayons sont ouverts. */
    private String nextKind() {
        Molecule.Kind next = game.nextMoleculeKind();
        if (next == null) return "";
        for (SpaceUpgrade upgrade : game.spaceUpgrades()) {
            if (upgrade.effect() instanceof SpaceUpgrade.OpenKind open && open.kind() == next) {
                double wait = game.secondsUntilSpace(upgrade.space());
                return "Prochain rayon : " + next.label() + ", dans les améliorations (" + (wait <= 0 ? "à prendre"
                        : "à " + Format.count(upgrade.space()) + " d'espace créé"
                                + (Double.isInfinite(wait) ? "" : ", dans " + Format.wait(wait))) + ").";
            }
        }
        return "";
    }

    private void refreshExpansion() {
        BigNum space = game.state().space();
        spaceLabel.setText(Format.count(space) + (space.gt(BigNum.ONE) ? " unités d'espace" : " unité d'espace"));
        BigNum reserved = game.reservedSpace();
        rateLabel.setText("+" + Format.amount(game.spacePerSecond()) + " par seconde"
                + (game.darkMatterSpaceBoost() > 1 ? " (matière noire ×" + ElementText.number(game.darkMatterSpaceBoost()) + ")" : "") + "  ·  "
                + Format.count(game.occupiedSpace()) + " occupées par " + game.moleculesCreated()
                + (game.moleculesCreated() > 1 ? " molécules" : " molécule")
                + (reserved.isZero() ? "" : "  ·  " + Format.count(reserved) + " réservées aux rassemblements")
                + "  ·  " + Format.count(game.freeSpace()) + " libres");
        String next = nextKind();
        expansionNote.setText("L'espace grandit avec le temps de jeu, et les molécules s'y logent."
                + (next.isEmpty() ? " Tous les rayons de molécules sont ouverts." : " " + next)
                + Detail.only(" Chaque Big Bang ajoute " + ElementText.number(Game.SPACE_PER_SECOND)
                        + " unité par seconde, et les inflations, les molécules, les assemblages et les astres qui "
                        + "agissent sur l'espace multiplient le tout (par " + ElementText.number(game.moleculeBoost(Molecule.Stat.SPACE))
                        + " en ce moment). L'espace grandit quel que soit l'onglet affiché, sauf en pause ; ni "
                        + "l'explosion ni le Big Bang ne le reprennent, et une molécule garde sa place pour toujours. "
                        + "L'espace libre sert aux molécules et à leurs lieux de rassemblement ; les améliorations, "
                        + "elles, ne regardent que l'espace créé en tout. Dans la vue, le cercle clair est le bord de "
                        + "l'espace créé, le cercle sombre le cœur utilisé ; les molécules rassemblées s'y rangent par "
                        + "état, les plus serrés au plus près du centre : métaux, cristaux, solides, liquides, gaz. "
                        + "Celles d'une même sorte partagent un amas de leur couleur, cerné de celle de son état ; les "
                        + "amas se touchent sans se chevaucher. De très loin, la matière se fond en nuages de gaz. "
                        + "La molette ou les boutons rapprochent et éloignent : on ne recule pas plus loin que l'espace "
                        + "créé. Tirer avec la souris déplace la vue. Une fois l'amas de galaxies formé, puis l'univers, "
                        + "un bouton recule la vue jusqu'à eux."));
        idle.core.Cosmos far = spaceView.cosmosShown();
        zoomOut.setDisable(far != null || spaceView.isFitted());
        zoomAll.setDisable(far != null || spaceView.isFitted());
        zoomIn.setDisable(far != null || spaceView.isClosest());
        viewToggle.setVisible(game.hasGalaxy());
        viewToggle.setManaged(game.hasGalaxy());
        viewToggle.setText(far != null ? "Voir la galaxie" : spaceView.showsGalaxy() ? "Voir la matière" : "Voir la galaxie");
        boolean cluster = game.hasCosmos(idle.core.Cosmos.CLUSTER) && far != idle.core.Cosmos.CLUSTER;
        clusterView.setVisible(cluster);
        clusterView.setManaged(cluster);
        boolean universe = game.hasCosmos(idle.core.Cosmos.UNIVERSE) && far != idle.core.Cosmos.UNIVERSE;
        universeView.setVisible(universe);
        universeView.setManaged(universe);
    }

    /** Ce que donne une sorte de molécule, en quelques mots : « Espace +5 % par doublement ». Vide si elle ne donne rien. */
    static String bonus(Molecule molecule) {
        return switch (molecule.bonus()) {
            case Molecule.Boost boost -> what(boost.stat()) + " +" + ElementText.percent(boost.perMolecule()) + " par doublement";
            case Molecule.Uncap uncap -> PeriodicTable.element(uncap.element()).name() + " : +1 exemplaire au plus par doublement";
            case null -> "";
        };
    }

    /** Ce que donnent en tout les molécules déjà créées d'une sorte, pour le mode détails. Vide si elles ne donnent rien. */
    private String given(Molecule molecule, int count) {
        return switch (molecule.bonus()) {
            case Molecule.Boost boost -> count == 0 ? "" : " Déjà +"
                    + ElementText.percent(boost.perMolecule() * game.effectiveMolecules(molecule.id()))
                    + " avec celles-ci" + (game.hasSubstance(molecule.id()) ? ", rassemblées" : "") + " ; molécules, assemblages, astres et améliorations réunis multiplient " + reach(boost.stat()) + " par "
                    + ElementText.number(game.moleculeBoost(boost.stat())) + ".";
            case Molecule.Uncap uncap -> " " + PeriodicTable.element(uncap.element()).name() + " : "
                    + game.maxCopiesOf(PeriodicTable.element(uncap.element())) + " exemplaires au plus en ce moment.";
            case null -> "";
        };
    }

    static String what(Molecule.Stat stat) {
        return switch (stat) {
            case SPACE -> "Espace par seconde";
            case PARTICLES -> "Particules";
            case ATOMS -> "Atomes par fusion";
            case DARK_GROWTH -> "Croissance de la matière noire";
        };
    }

    /** Le milieu de « toutes les molécules réunies multiplient … par N ». */
    private static String reach(Molecule.Stat stat) {
        return switch (stat) {
            case SPACE -> "l'espace gagné";
            case PARTICLES -> "les particules";
            case ATOMS -> "les atomes";
            case DARK_GROWTH -> "la croissance";
        };
    }

    /**
     * Une liste d'éléments et de nombres : « 2 H, 1 O ». Avec {@code owned}, ce que le joueur peut
     * y mettre est ajouté entre parenthèses : ses exemplaires, moins celui qui doit rester.
     */
    private String recipe(Map<Integer, Integer> atoms, boolean owned) {
        StringBuilder text = new StringBuilder();
        for (Map.Entry<Integer, Integer> atom : atoms.entrySet()) {
            if (text.length() > 0) text.append(", ");
            text.append(atom.getValue()).append(' ').append(PeriodicTable.element(atom.getKey()).symbol());
            if (owned) text.append(" (").append(Math.max(0, game.elementCount(atom.getKey()) - 1)).append(')');
        }
        return text.toString();
    }
}
