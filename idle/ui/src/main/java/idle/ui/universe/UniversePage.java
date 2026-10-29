package idle.ui.universe;

import idle.core.Game;
import idle.core.math.BigNum;
import idle.core.universe.UniverseUpgrade;
import idle.ui.common.CanvasPane;
import idle.ui.common.Card;
import idle.ui.common.Format;
import idle.ui.common.Labels;
import idle.ui.common.SubTabs;
import idle.ui.common.TileGrid;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * L'onglet « Univers » : la partie secondaire du jeu, qui commence quand l'univers est formé
 * (version 0.080, refaite en 0.081). Trois sous-onglets :
 * <ul>
 *   <li>« Univers » : les univers en bulles, les graines d'univers, les quatre améliorations (Brane,
 *       Bulk, Paysage, Omnivers) et les outils d'essai ;</li>
 *   <li>« Graine d'univers » (version 0.083) : la ressource principale, en grand, et son visuel ;</li>
 *   <li>« Dimensions », dès le premier univers (version 0.100) : dix points autour d'un onzième, une dimension
 *       de plus par univers ; chacune ouvre sa fenêtre ;</li>
 * </ul>
 * Le sous-onglet « Vide » est parti à la version 0.142 : le vide se joue dans son onglet, et son cumul, dans la graine,
 * paie les bulles de l'arbre.
 */
public final class UniversePage extends VBox {

    /** La couleur de l'univers : un bleu pervenche, à part des autres onglets. */
    public static final String COLOR = "#a8b8ff";
    /** La couleur du Vide : un cyan pâle. */
    public static final String VOID_COLOR = "#9fe6ff";

    static final int MAIN = 0;
    static final int DIMENSIONS = 1;
    /** Version 0.142 : plus de sous-onglet « Vide » ; le vide se joue dans son onglet, son cumul est dans la graine. */
    static final int SEED = 2;
    /** La couleur de la graine d'univers : un or de paille. */
    static final String SEED_COLOR = "#e6d27a";

    private static final String TOOL_STYLE = "-fx-font-size: 12px; -fx-padding: 4 12; -fx-cursor: hand; -fx-background-radius: 6;"
            + " -fx-border-radius: 6; -fx-text-fill: #ffd27f; -fx-background-color: #1f1a10; -fx-border-color: #6b5a2e;";

    private final Game game;
    private final Button mainTab = new Button("Univers");
    private final Button dimensionsTab = new Button("Dimensions");
    private final Button seedTab = new Button("Graine d'univers");
    private final HBox subTabs = new HBox(mainTab, seedTab, dimensionsTab);
    private int selected = MAIN;

    // Sous-onglet « Univers »
    private final UniverseView view = new UniverseView();
    private final Label resources = new Label();
    private final Map<UniverseUpgrade, Card> upgradeCards = new EnumMap<>(UniverseUpgrade.class);
    private final Button addUniverse = tool("+1 univers");
    private final Button removeUniverse = tool("−1 univers");
    private final Button addSeeds = tool("+10 graines d'univers");
    private final Button addVoid = tool("+10 énergie du vide");
    private final Button replay = tool("Revoir la naissance");
    private final Button oldTabs = tool("Montrer les anciens onglets");
    private final Button reset = tool("Tout remettre à zéro");
    /** Rouvrir les paliers (version 0.087) : jouer la partie qui formera l'univers suivant. */
    private final Button next = new Button();
    private final Label nextLine = new Label();
    private Runnable onNextUniverse = () -> { };
    /** Ce qui précède le lancement de l'univers suivant : le guide de transition (version 0.127). */
    private java.util.function.Consumer<Runnable> confirmLaunch = Runnable::run;

    /** Le guide à montrer avant de lancer l'univers suivant. */
    public void setLaunchConfirm(java.util.function.Consumer<Runnable> confirm) {
        confirmLaunch = confirm == null ? Runnable::run : confirm;
    }
    /** Le lancement de l'univers suivant (version 0.111), joué par-dessus la page. */
    private final LaunchView launch = new LaunchView();
    private double pulse;
    private final VBox mainPane;

    // Sous-onglet « Dimensions »
    private final DimensionsView dimensions = new DimensionsView();
    private final Label dimensionsLine = new Label();
    private final VBox dimensionsPane;

    // Sous-onglet « Graine d'univers »
    private final SeedView seedView = new SeedView();
    private final Label seedLine = new Label();
    private final Label voidCumul = new Label();
    /**
     * Où en est la graine (version 0.137 ; avant, le bouton qui la faisait germer puis l'arrosait) : l'arrosoir le
     * remplace, sur son étagère de la vue.
     */
    private final Label seedStatus = new Label();
    /** Les améliorations de la graine et le chrono (version 0.139), à gauche de sa vue. */
    private final SeedUpgradesPane seedUpgrades;
    private double upgradesTimer;
    /** Le bouton posé sur l'étagère, qui débloque l'arrosoir (version 0.137). */
    private final Button unlockCan = new Button("Débloquer l'arrosoir");
    /** Ce que l'arrosoir avait versé à la dernière image : pour saluer la brindille et rafraîchir la page. */
    private long lastWaterings = -1;
    private final VBox seedPane;

    private final StackPane pages;
    private boolean showOldTabs;
    private Consumer<Boolean> onOldTabs = shown -> { };
    private IntConsumer onOpenDimension = number -> { };

    public UniversePage(Game game) {
        super(6);
        this.game = game;
        setAlignment(Pos.TOP_CENTER);
        setPadding(new Insets(0, 16, 10, 16));

        SubTabs.wire(true, this::select, this::refresh, mainTab, dimensionsTab, seedTab);
        subTabs.setAlignment(Pos.CENTER);

        // « Univers » : les bulles, les ressources, les améliorations, les outils.
        resources.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: " + COLOR + ";");
        CanvasPane holder = CanvasPane.filling(view);
        holder.setMinHeight(220);
        VBox.setVgrow(holder, Priority.ALWAYS);
        TileGrid upgrades = new TileGrid(220, 4, 10);
        for (UniverseUpgrade upgrade : UniverseUpgrade.values()) {
            Card card = new Card(COLOR);
            card.setOnAction(() -> {
                game.testRaiseUniverseUpgrade(upgrade);
                refresh();
            });
            upgradeCards.put(upgrade, card);
            upgrades.add(card);
        }
        Label toolsTitle = new Label("Outils d'essai, sans logique : pour voir l'onglet avant que la suite existe");
        toolsTitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #b8a06a;");
        FlowPane tools = new FlowPane(8, 6, addUniverse, removeUniverse, addSeeds, addVoid, replay, oldTabs, reset);
        tools.setAlignment(Pos.CENTER);
        VBox toolsBox = new VBox(6, toolsTitle, tools);
        toolsBox.setAlignment(Pos.CENTER);
        toolsBox.setStyle("-fx-padding: 8 10; -fx-border-color: #4a4026; -fx-border-style: dashed; -fx-border-radius: 6;");
        addUniverse.setOnAction(event -> {
            if (game.testAddUniverse()) {
                view.setUniverses(game.universes());
                view.playBirth();
            }
            refresh();
        });
        removeUniverse.setOnAction(event -> {
            game.testRemoveUniverse();
            refresh();
        });
        addSeeds.setOnAction(event -> {
            game.testAddUniverseSeeds(BigNum.of(10));
            refresh();
        });
        addVoid.setOnAction(event -> {
            game.testAddVoidEnergy(BigNum.of(10));
            refresh();
        });
        replay.setOnAction(event -> view.playBirth());
        oldTabs.setOnAction(event -> {
            showOldTabs = !showOldTabs;
            onOldTabs.accept(showOldTabs);
            refresh();
        });
        reset.setOnAction(event -> {
            game.testResetUniverses();
            if (selected != MAIN) select(MAIN);
            refresh();
        });
        next.setFocusTraversable(false);
        // Version 0.111 : comme le bouton du tout début de la partie, il respire, et il faut cliquer pour lancer
        // l'univers. Le clic joue le lancement ; à son éclat, l'univers part et l'onglet Particules revient.
        next.setStyle("-fx-font-size: 16px; -fx-padding: 12 28; -fx-cursor: hand; -fx-text-fill: #e8f4ff;"
                + " -fx-background-color: #16202e; -fx-background-radius: 8; -fx-border-color: " + COLOR + "; -fx-border-radius: 8;");
        next.setOnAction(event -> {
            if (!game.canOpenNextUniverse() || launch.isPlaying()) return;
            // Version 0.127 : le guide de transition d'abord ; le lancement ne part que si le joueur confirme.
            confirmLaunch.accept(() -> {
                if (!game.canOpenNextUniverse() || launch.isPlaying()) return;
                launch.play(() -> {
                    if (game.canOpenNextUniverse() && game.startNextUniverse()) onNextUniverse.run();
                    refresh();
                });
                refresh();
            });
        });
        Labels.centered(nextLine, "-fx-font-size: 12px; -fx-text-fill: #c8d4ea;");
        nextLine.setMaxWidth(640);
        VBox nextBox = new VBox(4, nextLine);
        nextBox.setAlignment(Pos.CENTER);
        mainPane = new VBox(8, resources, nextBox, holder, upgrades, toolsBox);
        mainPane.setAlignment(Pos.TOP_CENTER);

        // « Dimensions » : la carte, et une ligne.
        dimensionsLine.setStyle("-fx-font-size: 13px; -fx-text-fill: #c8d4ea;");
        dimensions.setOnOpen(number -> onOpenDimension.accept(number));
        CanvasPane dimensionsHolder = CanvasPane.filling(dimensions);
        VBox.setVgrow(dimensionsHolder, Priority.ALWAYS);
        dimensionsPane = new VBox(8, dimensionsLine, dimensionsHolder);
        dimensionsPane.setAlignment(Pos.TOP_CENTER);

        // « Graine d'univers » : le nombre, en grand, et la graine.
        seedLine.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + SEED_COLOR + ";");
        Label seedHint = new Label("La graine de l'univers, la seule. Débloquez l'arrosoir sur l'étagère, puis glissez-le sur la"
                + " graine : penché sur elle, il l'arrose. Le premier arrosage la fait germer, les suivants la font grandir, et"
                + " chaque palier ouvre un univers. Les ressources des dimensions paient ses améliorations, à gauche.");
        seedHint.setWrapText(true);
        seedHint.setMaxWidth(560);
        seedHint.setStyle("-fx-font-size: 12px; -fx-text-fill: #b8ab80; -fx-text-alignment: center;");
        CanvasPane seedHolder = CanvasPane.filling(seedView);
        VBox.setVgrow(seedHolder, Priority.ALWAYS);
        seedStatus.setStyle("-fx-font-size: 14px; -fx-text-fill: " + SEED_COLOR + "; -fx-text-alignment: center; -fx-alignment: center;");
        seedStatus.setWrapText(true);
        seedStatus.setMaxWidth(560);
        unlockCan.setStyle("-fx-font-size: 13px; -fx-padding: 6 14; -fx-cursor: hand; -fx-background-radius: 6; -fx-border-radius: 6;"
                + " -fx-text-fill: #10151f; -fx-background-color: " + SEED_COLOR + "; -fx-border-color: #fff6d2;");
        unlockCan.setFocusTraversable(false);
        unlockCan.setOnAction(event -> {
            if (game.unlockWateringCan()) refresh();
        });
        // L'étagère est dessinée dans la vue ; son bouton se tient au-dessus d'elle, au même endroit.
        Pane canLayer = new Pane(unlockCan);
        canLayer.setPickOnBounds(false);
        seedHolder.setMinSize(0, 0);
        StackPane seedStage = new StackPane(seedHolder, canLayer);
        // Les bulles de l'arbre (version 0.142) : un clic sur l'une l'achète, avec l'énergie du vide accumulée.
        seedView.setOnBubble(this::clickBubble);
        // Le rond de croissance (version 0.153) : un clic le cueille, et l'arrosage accélère.
        seedView.setOnBloom(() -> {
            if (game.catchBloom()) refresh();
        });
        VBox.setVgrow(seedStage, Priority.ALWAYS);
        // Le cumul du vide (version 0.129) : ce qu'il restait d'énergie du vide à la formation de chaque univers.
        voidCumul.setStyle("-fx-font-size: 13px; -fx-text-fill: " + VOID_COLOR + "; -fx-text-alignment: center;");
        voidCumul.setWrapText(true);
        voidCumul.setMaxWidth(620);
        seedUpgrades = new SeedUpgradesPane(game);
        javafx.scene.control.ScrollPane upgradesScroll = new javafx.scene.control.ScrollPane(seedUpgrades);
        upgradesScroll.setFitToWidth(true);
        upgradesScroll.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        upgradesScroll.setMinWidth(280);
        upgradesScroll.setPrefWidth(340);
        upgradesScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        HBox.setHgrow(seedStage, Priority.ALWAYS);
        HBox seedRow = new HBox(10, upgradesScroll, seedStage);
        VBox.setVgrow(seedRow, Priority.ALWAYS);
        seedPane = new VBox(8, seedLine, seedHint, voidCumul, seedStatus, seedRow);
        seedPane.setAlignment(Pos.TOP_CENTER);

        // Par-dessus les sous-pages : le lancement, et le bouton qui le déclenche, au centre comme celui du tout
        // début, visible de chacune. La lumière du lancement naît là où il était.
        CanvasPane launchHolder = CanvasPane.filling(launch);
        launchHolder.setMouseTransparent(true);
        pages = new StackPane(mainPane, dimensionsPane, seedPane, launchHolder, next);
        StackPane.setAlignment(next, Pos.CENTER);
        VBox.setVgrow(pages, Priority.ALWAYS);
        getChildren().addAll(subTabs, pages);
        select(MAIN);
    }

    private static Button tool(String text) {
        Button button = new Button(text);
        button.setFocusTraversable(false);
        button.setStyle(TOOL_STYLE);
        return button;
    }

    /** La barre des sous-onglets : la disposition à gauche la place en haut de la fenêtre. */
    public HBox subTabs() {
        return subTabs;
    }

    /** Ce que fait l'outil « Montrer les anciens onglets » : vrai pour les montrer. */
    public void setOnOldTabs(Consumer<Boolean> action) {
        onOldTabs = action == null ? shown -> { } : action;
    }

    /** Ce que fait l'application quand la partie de l'univers suivant commence : montrer les anciens onglets. */
    public void setOnNextUniverse(Runnable action) {
        onNextUniverse = action == null ? () -> { } : action;
    }

    /** Le bouton « Rouvrir les paliers » : pour les vérifications. */
    public Button nextButton() {
        return next;
    }

    /** Ce que fait un clic sur une dimension ouverte : son numéro, de 1 à 10. */
    public void setOnOpenDimension(IntConsumer action) {
        onOpenDimension = action == null ? number -> { } : action;
    }

    /** Vrai si l'outil d'essai a fait revenir les anciens onglets. */
    public boolean showsOldTabs() {
        return showOldTabs;
    }

    /** La ligne du cumul du vide, dans le sous-onglet Graine : pour les vérifications. */
    public Label voidCumulLabel() {
        return voidCumul;
    }

    /** L'outil d'essai repasse à « cacher » : une nouvelle partie, un chargement. */
    public void forgetOldTabs() {
        showOldTabs = false;
    }

    public void select(int index) {
        if (index == DIMENSIONS && !game.isDimensionsUnlocked()) index = MAIN;
        selected = index;
        mainPane.setVisible(index == MAIN);
        dimensionsPane.setVisible(index == DIMENSIONS);
        seedPane.setVisible(index == SEED);
        mainTab.setStyle(SubTabs.style(index == MAIN, COLOR));
        seedTab.setStyle(SubTabs.style(index == SEED, SEED_COLOR));
        dimensionsTab.setStyle(SubTabs.style(index == DIMENSIONS, COLOR));
    }

    public int selected() {
        return selected;
    }

    /** Passe au sous-onglet suivant ou précédent, en boucle, parmi ceux qui sont ouverts. */
    public void step(int direction) {
        java.util.List<Integer> shown = new java.util.ArrayList<>(java.util.List.of(MAIN, SEED));
        if (game.isDimensionsUnlocked()) shown.add(DIMENSIONS);
        int at = Math.max(0, shown.indexOf(selected));
        select(shown.get(Math.floorMod(at + direction, shown.size())));
        refresh();
    }

    /** Recopie l'état de la partie secondaire. */
    public void refresh() {
        int universes = Math.max(1, game.universes());
        view.setUniverses(universes);
        boolean dims = game.isDimensionsUnlocked();
        dimensionsTab.setVisible(dims);
        dimensionsTab.setManaged(dims);
        if (selected == DIMENSIONS && !dims) select(MAIN);
        resources.setText(universes + " univers"
                + (universes > UniverseView.MAX_BUBBLES ? " (" + UniverseView.MAX_BUBBLES + " dessinés)" : "")
                // Le bonus permanent des univers formés (version 0.112).
                + (universes > 0 ? "   |   matière noire et sa croissance " + Format.multiplier(BigNum.of(game.universeDarkMatterFactor())) : ""));
        for (Map.Entry<UniverseUpgrade, Card> entry : upgradeCards.entrySet()) {
            UniverseUpgrade upgrade = entry.getKey();
            int level = game.universeUpgradeLevel(upgrade);
            entry.getValue().show(Card.State.READY, level > 0 ? String.valueOf(level) : "", "", upgrade.label(),
                    upgrade.flavor(), "Pas encore d'effet : un clic monte son niveau, pour essayer.", "Essai : +1 niveau", "");
        }
        removeUniverse.setDisable(universes <= 1);
        boolean replaying = game.isReplaying();
        // Le deuxième univers (version 0.106) attend la brindille de la graine ; il s'ouvre alors tout seul.
        boolean nextOpen = game.canOpenNextUniverse();
        next.setVisible(nextOpen && !launch.isPlaying());
        next.setText("Lancer l'univers " + (universes + 1));
        nextLine.setText(replaying
                ? "La partie de l'univers " + (universes + 1) + " est en cours, dans les anciens onglets : former l'univers, à la fin, le crée."
                : !nextOpen ? nextStageLine(game.nextUniverseStage())
                : launch.isPlaying() ? "L'univers " + (universes + 1) + " se lance…"
                : "La graine a atteint son palier " + game.nextUniverseStage() + ", " + Game.SEED_STAGE_NAMES.get(game.nextUniverseStage() - 1)
                        + " : cliquez « Lancer l'univers " + (universes + 1) + " ». " + "Les anciens onglets reviennent pour une partie neuve. Former l'univers à la fin de cette partie crée le suivant, et remet tout à zéro."
                        + (game.isSkinsUnlocked() ? "" : " Dès la cinquième fusion de cette partie, l'onglet Skins s'ouvre."));
        oldTabs.setText(showOldTabs ? "Cacher les anciens onglets" : "Montrer les anciens onglets");
        int open = game.dimensionsUnlocked();
        dimensions.setUnlocked(open);
        dimensions.setMarked(1, game.isLineThresholdReached());
        dimensions.setMarked(2, game.isPlaneThresholdReached());
        dimensions.setMarked(3, game.isVolumeThresholdReached());
        dimensions.setMarked(4, game.isDurationThresholdReached());
        dimensions.setMarked(5, game.isLoopThresholdReached());
        dimensions.setMarked(6, game.isDimensionThresholdReached(6));
        dimensions.setMarked(7, game.isDimensionThresholdReached(7));
        dimensions.setMarked(8, game.isDimensionThresholdReached(8));
        dimensions.setMarked(9, game.isDimensionThresholdReached(9));
        dimensionsLine.setText(open + " / " + Game.MAX_DIMENSIONS + " dimensions ouvertes : une de plus à chaque univers, jusqu'au dixième."
                + " Cliquez une dimension ouverte pour y entrer.");
        java.util.Map<String, Double> history = game.state().voidHistory();
        boolean bubbles = !game.treeBubblesShown().isEmpty();
        voidCumul.setVisible(!history.isEmpty() || bubbles);
        voidCumul.setManaged(voidCumul.isVisible());
        if (history.isEmpty()) {
            voidCumul.setText("Énergie du vide accumulée : 0. Ce qu'il reste d'énergie du vide à la formation d'un univers "
                    + "s'ajoute ici, et paie les bulles de l'arbre.");
        } else {
            StringBuilder each = new StringBuilder();
            history.forEach((universe, stock) -> each.append(each.length() == 0 ? "" : ", ").append("univers ").append(universe)
                    .append(" : ").append(Format.count(BigNum.of(stock))));
            // Version 0.142 : elle paie les bulles de l'arbre ; la ligne dit ce qu'il en reste à dépenser.
            voidCumul.setText("Énergie du vide accumulée : " + Format.count(BigNum.of(game.voidAccumulated())) + " (" + each
                    + "), ce qu'il restait à la formation de chaque univers. À dépenser pour les bulles de l'arbre : "
                    + Format.count(BigNum.of(game.voidAvailable())) + ".");
        }
        // Il n'y a qu'une graine (version 0.105) : pas de compteur, la graine est là dès que l'univers est formé.
        seedView.setSeeds(game.isUniverseFormed() ? 1 : 0);
        seedView.setSprout(game.state().seedSprouted(), game.state().seedWaterings());
        seedView.setCanUnlocked(game.isWateringCanUnlocked());
        unlockCan.setVisible(game.canUnlockWateringCan());
        long waterings = game.state().seedWaterings();
        int stage = game.seedStage();
        seedView.setStage(stage);
        long nextWaterings = Game.seedStageWaterings(Math.min(Game.SEED_STAGES, stage + 1));
        seedLine.setText("La graine d'univers : palier " + stage + " sur " + Game.SEED_STAGES + ", " + Game.SEED_STAGE_NAMES.get(stage - 1));
        String can = game.isWateringCanUnlocked() ? "" : "Débloquez l'arrosoir sur l'étagère. ";
        String pace = " (arrosoir : " + Format.darkMatter(BigNum.of(game.wateringCanRate())) + " par seconde"
                + (game.hasHose() ? ", tuyau : " + Format.darkMatter(BigNum.of(game.hoseRate())) + " par seconde" : "")
                + (game.seedSoilFactor() > 1 ? ", chaque arrosage compte " + game.seedSoilFactor() : "")
                // La graine profonde (version 0.156) : ce qu'elle multiplie, une fois achetée.
                + (game.deepSeed() > 1 + 1e-9 ? ", graine profonde " + Format.multiplier(BigNum.of(game.deepSeed())) : "") + ")";
        seedStatus.setText(stage >= Game.SEED_STAGES ? "L'arbre-monde : la graine a fini de grandir"
                : stage >= 2
                ? can + "Arroser " + Game.SEED_STAGE_NAMES.get(stage - 1) + " : " + waterings + " sur " + nextWaterings + " arrosages, palier "
                        + (stage + 1) + " : " + Game.SEED_STAGE_NAMES.get(stage) + pace
                : game.state().seedSprouted()
                ? can + "Arroser le germe : " + waterings + (waterings > 1 ? " arrosages" : " arrosage") + " sur " + Game.SEED_TWIG_WATERINGS + pace
                : can + "Le premier arrosage fait germer la graine");
        seedUpgrades.refresh();
        seedTab.setText("Graine d'univers");
        mainTab.setText("Univers (" + universes + ")");
        dimensionsTab.setText("Dimensions (" + open + "/" + Game.MAX_DIMENSIONS + ")");
    }

    /** Ce qui manque à la graine pour ouvrir l'univers {@code number} : son palier, et ce qu'il demande (version 0.120). */
    private String nextStageLine(int number) {
        int stage = Math.min(number, Game.SEED_STAGES);
        String name = Game.SEED_STAGE_NAMES.get(stage - 1);
        int dimension = stage - 1;
        boolean threshold = game.isDimensionThresholdReached(dimension);
        long need = Game.seedStageWaterings(stage);
        return "L'univers " + number + " s'ouvre quand la graine atteint son palier " + stage + ", " + name + " : "
                + (!threshold ? "il faut d'abord le seuil de la dimension " + dimension
                        + (dimension <= Dimension.PLAYABLE ? "" : ", qui n'existe pas encore") + ", puis "
                        : "")
                + Math.min(game.state().seedWaterings(), need) + " / " + need + " arrosages"
                + (game.state().seedSprouted() ? "." : ", une fois la graine germée.");
    }

    /** Joue la naissance d'un univers : il vient de se former. */
    public void playBirth() {
        view.setUniverses(Math.max(1, game.universes()));
        view.playBirth();
    }

    /** Vrai tant que la naissance se joue. */
    public boolean isBirthPlaying() {
        return view.isBirthPlaying();
    }

    /** Fait avancer l'animation du sous-onglet affiché ; {@code moving} faux (réglage coupé) la laisse immobile. */
    public void frame(double elapsed, boolean moving) {
        // Le lancement avance même animations coupées : c'est lui qui lance l'univers.
        boolean wasPlaying = launch.isPlaying();
        launch.frame(elapsed);
        if (wasPlaying && !launch.isPlaying()) refresh();
        pulse += Math.min(elapsed, 0.1);
        double scale = moving && next.isVisible() ? 1 + 0.03 * (1 + Math.sin(pulse * Math.PI / 1.1)) : 1;
        next.setScaleX(scale);
        next.setScaleY(scale);
        // L'arrosoir (version 0.137) : penché sur la graine, il arrose ; le jeu compte, la page suit.
        game.setWatering(selected == SEED && seedView.isPouring());
        long waterings = game.state().seedWaterings() + (game.state().seedSprouted() ? 1 : 0);
        if (lastWaterings >= 0 && waterings != lastWaterings) {
            // La brindille (version 0.106) ouvre le deuxième univers : un éclat la salue.
            if (game.isSeedTwig() && game.state().seedWaterings() == Game.SEED_TWIG_WATERINGS) seedView.celebrate();
            refresh();
        }
        lastWaterings = waterings;
        switch (selected) {
            case DIMENSIONS -> dimensions.frame(elapsed, moving);
            case SEED -> {
                // Le tuyau et le chrono (version 0.139) : ce que la vue en montre suit le jeu à chaque image.
                seedView.setHose(game.hasHose(), game.hasHose() && game.canTendSeed());
                seedView.setChrono(game.isChronoUnlocked(), game.chronoCapacity() > 0 ? game.chronoStored() / game.chronoCapacity() : 0,
                        game.isChronoRunning());
                seedView.setBubbles(bubbles());
                // Le rond de croissance (version 0.153) : là où le jeu le fait pousser, et son effet en cours.
                seedView.setBloom(game.isBloomVisible(), game.bloomX(), game.bloomY(), game.bloomVisibleLeft(), Game.BLOOM_VISIBLE);
                seedView.setBloomEffect(game.bloomActiveLeft(), game.bloomGain());
                // La météo (version 0.155) : le temps qu'il fait, et son effet sur l'arrosage.
                seedView.setWeather(game.isWeatherUnlocked() ? game.weather() : null, game.weatherLeft(), game.weatherFactor());
                // Les saisons (version 0.157) : la saison, ce qu'il lui reste, et ce qu'un arrosage verse au chrono.
                seedView.setSeason(game.areSeasonsUnlocked() ? game.season() : null, game.seasonLeft(), game.seasonFillPerWatering());
                seedView.frame(elapsed, moving);
                placeUnlockCan();
                upgradesTimer += elapsed;
                if (upgradesTimer >= 0.25) {
                    upgradesTimer = 0;
                    seedUpgrades.refresh();
                }
            }
            default -> view.frame(elapsed, moving);
        }
    }

    /** La vue des univers : pour les vérifications. */
    public UniverseView view() {
        return view;
    }

    /** Le lancement de l'univers suivant : pour les vérifications. */
    public LaunchView launchView() {
        return launch;
    }

    /** Le bouton de l'étagère pose l'arrosoir au-dessus de la planche, au milieu. */
    private void placeUnlockCan() {
        if (!unlockCan.isVisible()) return;
        double width = unlockCan.prefWidth(-1), height = unlockCan.prefHeight(width);
        unlockCan.resizeRelocate(Math.max(4, Math.min(seedView.getWidth() - width - 4, seedView.shelfX() - width / 2)),
                Math.max(4, seedView.shelfMiddleY() - height / 2), width, height);
    }

    /** Ce que dit la page de la graine, et le bouton qui débloque l'arrosoir : pour les vérifications. */
    public Label seedStatus() {
        return seedStatus;
    }

    /** Les bulles de l'arbre (version 0.142), comme la vue les dessine : leur place, leur prix, leur état, leurs liens. */
    private java.util.List<SeedView.Bubble> bubbles() {
        java.util.List<Game.TreeBubble> shown = game.treeBubblesShown();
        java.util.List<SeedView.Bubble> list = new java.util.ArrayList<>();
        for (Game.TreeBubble bubble : shown) {
            // Version 0.148 : celles qui se rachètent restent à acheter jusqu'à leur dernier niveau.
            int level = game.treeBubbleLevel(bubble);
            boolean many = bubble.maxLevel() > 1;
            SeedView.BubbleState state = game.isTreeBubbleMaxed(bubble) ? SeedView.BubbleState.BOUGHT
                    : !game.isTreeBubbleOpen(bubble) ? SeedView.BubbleState.CLOSED
                    : game.canBuyTreeBubble(bubble) ? SeedView.BubbleState.BUY : SeedView.BubbleState.SHORT;
            double cost = game.treeBubbleCost(bubble);
            String levels = many ? "Niveau " + level + " / " + bubble.maxLevel() + ". " : "";
            String price = switch (state) {
                case BOUGHT -> many ? "Niveau " + level + " / " + bubble.maxLevel() + " : au plus haut"
                        : bubble.isComingSoon() ? "Achetée : son effet arrive avec son onglet" : "Achetée";
                case CLOSED -> shownLinks(bubble, shown).size() == 1 ? "Il faut d'abord : " + shownLinks(bubble, shown).get(0).label()
                        : "Il faut d'abord une bulle reliée à elle";
                case BUY -> levels + "Prix : " + Format.count(BigNum.of(cost)) + " d'énergie du vide";
                case SHORT -> levels + "Prix : " + Format.count(BigNum.of(cost)) + " d'énergie du vide (il en manque "
                        + Format.count(BigNum.of(Math.ceil(cost - game.voidAvailable()))) + ")";
            };
            java.util.List<Integer> below = new java.util.ArrayList<>();
            for (Game.TreeBubble under : bubble.below()) if (shown.contains(under)) below.add(shown.indexOf(under));
            list.add(new SeedView.Bubble(bubble.ordinal(), bubble.label(), bubble.effect(), price, state, below));
        }
        return list;
    }

    /** Les bulles nées reliées à celle-ci : celles qui peuvent l'ouvrir. */
    private static java.util.List<Game.TreeBubble> shownLinks(Game.TreeBubble bubble, java.util.List<Game.TreeBubble> shown) {
        java.util.List<Game.TreeBubble> links = new java.util.ArrayList<>(bubble.links());
        links.retainAll(shown);
        return links;
    }

    /** Un clic sur une bulle : achetée si elle peut l'être. */
    private void clickBubble(int index) {
        java.util.List<Game.TreeBubble> shown = game.treeBubblesShown();
        if (index < 0 || index >= shown.size()) return;
        if (game.buyTreeBubble(shown.get(index))) refresh();
        seedView.setBubbles(bubbles());
    }

    public SeedUpgradesPane seedUpgrades() {
        return seedUpgrades;
    }

    public Button unlockCanButton() {
        return unlockCan;
    }

    /** La graine d'univers : pour les vérifications. */

    public SeedView seedView() {
        return seedView;
    }

    /** La carte des dimensions : pour les vérifications. */
    public DimensionsView dimensionsView() {
        return dimensions;
    }

    /** Les outils d'essai, par leur texte : pour les vérifications. */
    Button tool(int index) {
        return new Button[] {addUniverse, removeUniverse, addSeeds, addVoid, replay, oldTabs, reset}[index];
    }

    /** Un outil d'essai, par sa place (0 : +1 univers, 1 : −1, 2 : graines, 3 : vide, 4 : naissance, 5 : anciens onglets, 6 : zéro). */
    public void pressTool(int index) {
        tool(index).fire();
    }

    /** La carte d'une amélioration : pour les vérifications. */
    public Card card(UniverseUpgrade upgrade) {
        return upgradeCards.get(upgrade);
    }
}
