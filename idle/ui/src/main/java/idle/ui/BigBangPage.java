package idle.ui;

import idle.core.Assembly;
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

    // Sous-page « Expansion de la matière »
    private final VBox expansionPane = new VBox(6);
    private final Label spaceLabel = new Label();
    private final Label rateLabel = new Label();
    private final Label expansionNote = new Label();
    private final SpaceView spaceView;
    private final Button zoomOut = new Button("Reculer");
    private final Button zoomIn = new Button("Approcher");
    private final Button zoomAll = new Button("Tout voir");

    // Sous-page « Améliorations »
    private final VBox upgradesPane = new VBox(10);
    private final ScrollPane upgradesScroll = new ScrollPane(upgradesPane);
    private final Label upgradesIntro = new Label();
    private final TileGrid upgradesGrid = new TileGrid(230, 3, 8);
    private final Map<SpaceUpgrade, Card> upgradeCards = new LinkedHashMap<>();

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
        for (Button tab : new Button[] {moleculesTab, expansionTab, upgradesTab, statesTab, assembliesTab, bodiesTab}) {
            tab.setFocusTraversable(false);
        }
        FlowPane subTabs = new FlowPane(0, 0, moleculesTab, expansionTab, upgradesTab, statesTab, assembliesTab, bodiesTab);
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
        nextKindLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + GameApp.BIG_BANG_COLOR + ";");
        nextKindLabel.setWrapText(true);
        nextKindLabel.setTextAlignment(TextAlignment.CENTER);
        nextKindLabel.setMaxWidth(760);
        gridHolder.setMaxWidth(900);
        moleculesPane.getChildren().add(moleculesIntro);
        moleculesPane.getChildren().add(chips);
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
        for (Button button : new Button[] {zoomOut, zoomIn, zoomAll}) {
            button.setStyle(ZOOM_STYLE);
            button.setFocusTraversable(false);
        }
        HBox zoomRow = new HBox(6, zoomOut, zoomIn, zoomAll);
        zoomRow.setAlignment(Pos.CENTER);
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
        StackPane pages = new StackPane(moleculesScroll, expansionPane, upgradesScroll, statesScroll, assembliesScroll, bodiesPane);
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
     * 3 = les états de la matière, 4 = les assemblages, 5 = les astres.
     */
    void select(int index) {
        selected = index;
        moleculesScroll.setVisible(index == 0);
        expansionPane.setVisible(index == 1);
        upgradesScroll.setVisible(index == 2);
        statesScroll.setVisible(index == 3);
        assembliesScroll.setVisible(index == 4);
        bodiesPane.setVisible(index == 5);
        moleculesTab.setStyle(subTabStyle(index == 0));
        expansionTab.setStyle(subTabStyle(index == 1));
        upgradesTab.setStyle(subTabStyle(index == 2));
        statesTab.setStyle(subTabStyle(index == 3));
        assembliesTab.setStyle(subTabStyle(index == 4));
        bodiesTab.setStyle(subTabStyle(index == 5));
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
    }

    /** Recopie l'état du jeu dans les textes. */
    void refresh() {
        int count = game.bigBangs();
        countLabel.setText(count + (count > 1 ? " Big Bangs" : " Big Bang"));
        sinceLabel.setText("Partie en cours depuis " + Format.duration(game.stats().runTime())
                + ". Le prochain se déclenche au bout de l'arbre de matière noire.");
        int created = game.moleculesCreated();
        moleculesTab.setText(created > 0 ? "Molécules (" + created + ")" : "Molécules");
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
        switch (selected) {
            case 0 -> refreshMolecules();
            case 1 -> refreshExpansion();
            case 2 -> refreshUpgrades();
            case 3 -> refreshStates();
            case 4 -> refreshAssemblies();
            default -> bodiesPane.refresh();
        }
    }

    private void refreshUpgrades() {
        upgradesIntro.setText("Une amélioration se prend quand l'expansion a créé assez d'espace ("
                + Format.count(game.state().space()) + " en ce moment). Elle ne coûte rien."
                + Detail.only(" C'est l'espace gagné depuis le premier Big Bang qui compte, pas celui qui reste libre : "
                        + "remplir l'espace de molécules et de leurs lieux de rassemblement ne retarde aucune amélioration. Les "
                        + "rayons de molécules s'ouvrent l'un après l'autre ; chacun n'est proposé qu'une fois le précédent "
                        + "pris. Ni l'explosion ni le Big Bang ne reprennent une amélioration."));
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
                case SpaceUpgrade.OpenBodies bodies -> "Former des astres, de l'amas de roches à la planète";
            };
            String detail = switch (upgrade.effect()) {
                case SpaceUpgrade.OpenKind open -> "Le rayon « " + open.kind().label() + " » apparaît dans la sous-page "
                        + "Molécules. Chacune de ses molécules donne un léger bonus ou relève le plafond d'un de ses "
                        + "éléments, et a son état : gaz, liquide, solide, cristal ou métal.";
                case SpaceUpgrade.OpenStates states -> "Ouvre la sous-page « États de la matière » : à partir de "
                        + Game.SUBSTANCE_MOLECULES + " molécules d'une même sorte, un achat unique les rassemble dans le "
                        + "lieu de leur état : gaz, liquides, solides, cristaux ou métaux. Aucune n'est consommée ; leur bonus est un peu "
                        + "renforcé et chacune ajoute des particules ou des atomes.";
                case SpaceUpgrade.OpenAssemblies assemblies -> "Ouvre la sous-page « Assemblages » : plusieurs sortes de "
                        + "molécules rassemblées, par centaines, forment une matière de la nature : une roche, un minerai, "
                        + "une pierre précieuse, une eau, un gaz ou un hydrocarbure. Rien de vivant, rien de fabriqué. "
                        + "Aucune molécule n'est consommée.";
                case SpaceUpgrade.OpenBodies bodies -> "Ouvre la sous-page « Astres » : des assemblages, de la matière rassemblée et "
                        + "des molécules se cumulent pour former un amas de roches, puis une comète, un astéroïde, une lune, une "
                        + "planète, chacun de plusieurs types et chacun parti des plus petits.";
            };
            card.show(owned ? Card.State.DONE : ready ? Card.State.READY : Card.State.WAITING,
                    "", owned ? "acquise" : upgrade.effect() instanceof SpaceUpgrade.OpenKind ? "rayon" : "",
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
            BigNum space = game.substanceSpace(molecule.id());
            String power = ElementText.number(matter.exponent());
            // Un plafond ne se relève que d'exemplaires entiers : la carte montre ce qui compte vraiment.
            double effective = game.effectiveMolecules(molecule.id());
            String counted = ElementText.number(molecule.bonus() instanceof Molecule.Uncap ? Math.floor(effective + 1e-9) : effective);
            String whole = molecule.bonus() instanceof Molecule.Uncap ? ", arrondi en dessous pour un plafond" : "";
            String line = gathered
                    ? molecules + (molecules > 1 ? " molécules comptées pour " : " molécule comptée pour ") + counted
                            + "\n" + capitalized(generation(matter, Math.max(1, molecules)))
                    : "Compte ses molécules à la puissance " + power + "\n" + capitalized(generation(matter, 1)) + " par molécule";
            String detail = gathered
                    ? "Rassemblées dans " + placeName(matter) + ". Leur bonus (" + bonus(molecule) + ") compte leur nombre à la "
                            + "puissance " + power + whole + ". Chacune ajoute " + generation(matter, 1)
                            + ". Les prochaines " + molecule.formula() + " créées les rejoindront d'elles-mêmes."
                    : "Un seul achat : toutes vos " + molecule.formula() + " vont se ranger dans " + placeName(matter)
                            + ", sans être consommées. Leur bonus (" + bonus(molecule) + ") compte alors leur nombre à la puissance "
                            + power + whole + " : 3 comptent pour " + ElementText.number(Math.pow(3, matter.exponent()))
                            + ", 10 pour " + ElementText.number(Math.pow(10, matter.exponent()))
                            + ". Chacune ajoute " + generation(matter, 1) + ". Celles créées ensuite les rejoindront.";
            String price = gathered ? ""
                    : ready ? "Rassembler : " + Format.count(space) + " d'espace"
                    : molecules < Game.SUBSTANCE_MOLECULES ? "Il faut " + Game.SUBSTANCE_MOLECULES + " " + molecule.formula()
                            + " (" + molecules + ")"
                    : "Il manque " + Format.count(space.subtract(game.freeSpace()).max(BigNum.ONE)) + " d'espace";
            card.show(gathered ? Card.State.DONE : ready ? Card.State.READY : Card.State.WAITING,
                    "×" + molecules, gathered ? "rassemblées" : "", molecule.name(), line, detail, price, "");
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
                        + "de leur état : plus vous en avez, plus il rapporte (" + Format.count(game.freeSpace())
                        + " d'espace libre)."
                        + Detail.only(" Aucune molécule n'est consommée : elles se rangent toutes dans le lieu de leur état, "
                                + "visible dans l'expansion de la matière, les plus serrés au plus près du centre : les "
                                + "métaux, les cristaux, les solides, les liquides, puis les gaz. Le lieu réserve de l'espace une fois "
                                + "pour toutes, d'après la place de " + Game.SUBSTANCE_MOLECULES + " molécules : trois fois "
                                + "pour un gaz, une fois et demie pour un liquide, une fois pour un solide ou un cristal, les "
                                + "trois quarts pour un métal. Ce que donne le rassemblement suit le nombre d'exemplaires, y "
                                + "compris ceux créés ensuite, et l'état le plus serré compte le mieux ses molécules : "
                                + "puissance " + ElementText.number(Molecule.State.GAS.exponent()) + " pour un gaz, "
                                + ElementText.number(Molecule.State.METAL.exponent()) + " pour un métal. Seules les molécules "
                                + "que vous avez déjà créées sont montrées."));
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
            card.setOnAction(() -> {
                game.formSubstance(molecule.id());
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
                    (formed ? "Dans le bloc : " : "Ce que vous avez : ") + have + ". Fait de " + names + ". Les molécules ne sont "
                            + "pas consommées : elles quittent l'amas de leur sorte pour former un seul bloc, visible dans "
                            + "l'expansion de la matière, et gardent leur bonus. Un autre assemblage ne peut pas compter sur "
                            + "les mêmes molécules.",
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

    /** Ce qu'ajoutent {@code molecules} molécules rassemblées dans un état : « +15 % de particules et +6 % d'atomes ». */
    private static String generation(Molecule.State matter, int molecules) {
        String particles = matter.particles() > 0 ? "+" + ElementText.percent(matter.particles() * molecules) + " de particules" : "";
        String atoms = matter.atoms() > 0 ? "+" + ElementText.percent(matter.atoms() * molecules) + " d'atomes" : "";
        return particles.isEmpty() ? atoms : atoms.isEmpty() ? particles : particles + " et " + atoms;
    }

    private static String capitalized(String text) {
        return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    private void refreshMolecules() {
        moleculesIntro.setText("Une molécule se crée avec des exemplaires du tableau périodique et de l'espace libre ("
                + Format.count(game.freeSpace()) + " en ce moment), et reste pour toujours."
                + Detail.only(" Les exemplaires quittent le tableau avec ce qu'ils y apportaient ; un exemplaire de "
                        + "chaque élément y reste, pour que le tableau puisse toujours exploser. Chaque molécule créée "
                        + "double ce que demande la suivante de la même sorte : sa formule une fois, deux fois, quatre "
                        + "fois. Elle occupe " + ElementText.number(Game.SPACE_PER_PROTON) + " unités d'espace par "
                        + "proton, que l'expansion de la matière doit avoir créées : plus elle est complexe ou faite "
                        + "d'éléments lourds, plus elle en prend. Ni l'explosion ni le Big Bang ne reprennent une "
                        + "molécule. Chaque molécule créée donne quelque chose : un léger bonus, ou un exemplaire de "
                        + "plus au maximum d'un de ses éléments dans le tableau périodique."));
        // Les rayons : combien de leurs molécules ont été créées au moins une fois.
        Map<Molecule.Kind, int[]> totals = new EnumMap<>(Molecule.Kind.class);
        for (Molecule molecule : game.molecules()) {
            int[] total = totals.computeIfAbsent(molecule.kind(), each -> new int[2]);
            total[1]++;
            if (game.moleculeCount(molecule.id()) > 0) total[0]++;
        }
        // Un rayon fermé n'est pas proposé du tout ; celui qu'on regardait a pu se refermer avec une remise à zéro.
        if (!game.isMoleculeKindUnlocked(kind)) selectKind(Molecule.Kind.SIMPLE);
        kindChips.forEach((each, chip) -> {
            int[] total = totals.getOrDefault(each, new int[2]);
            chip.setText(each.label() + "  " + total[0] + "/" + total[1]);
            boolean open = game.isMoleculeKindUnlocked(each);
            chip.setVisible(open);
            chip.setManaged(open);
        });
        nextKindLabel.setText(nextKind());
        nextKindLabel.setVisible(!nextKindLabel.getText().isEmpty());
        nextKindLabel.setManaged(!nextKindLabel.getText().isEmpty());
        cards.forEach((molecule, card) -> {
            if (molecule.kind() != kind) return;      // seul le rayon affiché se met à jour
            int count = game.moleculeCount(molecule.id());
            boolean reachable = game.isMoleculeWithinReach(molecule.id());
            boolean elements = game.hasElementsForMolecule(molecule.id());
            boolean ready = game.canCreateMolecule(molecule.id());
            Map<Integer, Integer> cost = game.nextMoleculeCost(molecule.id());
            BigNum volume = game.moleculeVolume(molecule.id());
            String detail = molecule.atoms() + (molecule.atoms() > 1 ? " atomes, " : " atome, ") + molecule.protons()
                    + " protons. Formule : " + recipe(molecule.recipe(), false) + "."
                    + (count > 0 ? " La prochaine demande " + (1L << Math.min(20, count)) + " fois la formule." : "")
                    + given(molecule, count);
            String price = ready ? "Créer : " + recipe(cost, false)
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
        rateLabel.setText("+" + Format.amount(game.spacePerSecond()) + " par seconde  ·  "
                + Format.count(game.occupiedSpace()) + " occupées par " + game.moleculesCreated()
                + (game.moleculesCreated() > 1 ? " molécules" : " molécule")
                + (reserved.isZero() ? "" : "  ·  " + Format.count(reserved) + " réservées aux rassemblements")
                + "  ·  " + Format.count(game.freeSpace()) + " libres");
        String next = nextKind();
        expansionNote.setText("L'espace grandit avec le temps de jeu, et les molécules s'y logent."
                + (next.isEmpty() ? " Tous les rayons de molécules sont ouverts." : " " + next)
                + Detail.only(" Chaque Big Bang ajoute " + ElementText.number(Game.SPACE_PER_SECOND)
                        + " unité par seconde, et les molécules qui agissent sur l'espace multiplient le "
                        + "tout (par " + ElementText.number(game.moleculeBoost(Molecule.Stat.SPACE))
                        + " en ce moment). L'espace grandit quel que soit l'onglet affiché, sauf en pause ; ni "
                        + "l'explosion ni le Big Bang ne le reprennent, et une molécule garde sa place pour toujours. "
                        + "L'espace libre sert aux molécules et à leurs lieux de rassemblement ; les améliorations, "
                        + "elles, ne regardent que l'espace créé en tout. Dans la vue, le cercle clair est le bord de "
                        + "l'espace créé, le cercle sombre le cœur utilisé ; les molécules rassemblées s'y rangent par "
                        + "état, les plus serrés au plus près du centre : métaux, cristaux, solides, liquides, gaz. "
                        + "Celles d'une même sorte partagent un amas de leur couleur, cerné de celle de son état ; les "
                        + "amas se touchent sans se chevaucher. De très loin, la matière se fond en nuages de gaz. "
                        + "La molette ou les boutons rapprochent et éloignent : on ne recule pas plus loin que l'espace "
                        + "créé. Tirer avec la souris déplace la vue."));
        zoomOut.setDisable(spaceView.isFitted());
        zoomAll.setDisable(spaceView.isFitted());
        zoomIn.setDisable(spaceView.isClosest());
    }

    /** Ce que donne chaque molécule créée, en quelques mots : « Espace +5 % par molécule ». Vide si elle ne donne rien. */
    static String bonus(Molecule molecule) {
        return switch (molecule.bonus()) {
            case Molecule.Boost boost -> what(boost.stat()) + " +" + ElementText.percent(boost.perMolecule()) + " par molécule";
            case Molecule.Uncap uncap -> PeriodicTable.element(uncap.element()).name() + " : +1 exemplaire au plus par molécule";
            case null -> "";
        };
    }

    /** Ce que donnent en tout les molécules déjà créées d'une sorte, pour le mode détails. Vide si elles ne donnent rien. */
    private String given(Molecule molecule, int count) {
        return switch (molecule.bonus()) {
            case Molecule.Boost boost -> count == 0 ? "" : " Déjà +"
                    + ElementText.percent(boost.perMolecule() * game.effectiveMolecules(molecule.id()))
                    + " avec celles-ci" + (game.hasSubstance(molecule.id()) ? ", rassemblées" : "") + " ; toutes les molécules réunies multiplient " + reach(boost.stat()) + " par "
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
