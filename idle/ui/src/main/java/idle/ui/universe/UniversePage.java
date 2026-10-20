package idle.ui.universe;

import idle.core.Game;
import idle.core.math.BigNum;
import idle.core.universe.UniverseUpgrade;
import idle.ui.common.CanvasPane;
import idle.ui.common.Card;
import idle.ui.common.Format;
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * L'onglet « Univers » : la partie secondaire du jeu, qui commence quand l'univers est formé
 * (version 0.080, refaite en 0.081). Quatre sous-onglets, sans logique pour l'instant :
 * <ul>
 *   <li>« Univers » : les univers en bulles, les graines d'univers, les quatre améliorations (Brane,
 *       Bulk, Paysage, Omnivers) et les outils d'essai ;</li>
 *   <li>« Graine d'univers » (version 0.083) : la ressource principale, en grand, et son visuel ;</li>
 *   <li>« Dimensions », dès le premier univers (version 0.100) : dix points autour d'un onzième, une dimension
 *       de plus par univers ; chacune ouvre sa fenêtre ;</li>
 *   <li>« Vide », le deuxième acte de la partie secondaire, dès qu'il y a de l'énergie du vide.</li>
 * </ul>
 */
public final class UniversePage extends VBox {

    /** La couleur de l'univers : un bleu pervenche, à part des autres onglets. */
    public static final String COLOR = "#a8b8ff";
    /** La couleur du Vide : un cyan pâle. */
    public static final String VOID_COLOR = "#9fe6ff";

    static final int MAIN = 0;
    static final int DIMENSIONS = 1;
    static final int VOID = 2;
    static final int SEED = 3;
    /** La couleur de la graine d'univers : un or de paille. */
    static final String SEED_COLOR = "#e6d27a";

    private static final String SUB_TAB_STYLE = "-fx-font-size: 13px; -fx-padding: 6 20; -fx-cursor: hand;"
            + " -fx-background-radius: 0; -fx-border-width: 0 0 2 0; -fx-background-color: transparent;";
    private static final String TOOL_STYLE = "-fx-font-size: 12px; -fx-padding: 4 12; -fx-cursor: hand; -fx-background-radius: 6;"
            + " -fx-border-radius: 6; -fx-text-fill: #ffd27f; -fx-background-color: #1f1a10; -fx-border-color: #6b5a2e;";

    private final Game game;
    private final Button mainTab = new Button("Univers");
    private final Button dimensionsTab = new Button("Dimensions");
    private final Button voidTab = new Button("Vide");
    private final Button seedTab = new Button("Graine d'univers");
    private final HBox subTabs = new HBox(mainTab, seedTab, dimensionsTab, voidTab);
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
    private final VBox mainPane;

    // Sous-onglet « Dimensions »
    private final DimensionsView dimensions = new DimensionsView();
    private final Label dimensionsLine = new Label();
    private final VBox dimensionsPane;

    // Sous-onglet « Vide »
    private final VoidView voidView = new VoidView();
    private final Label voidLine = new Label();
    private final VBox voidPane;

    // Sous-onglet « Graine d'univers »
    private final SeedView seedView = new SeedView();
    private final Label seedLine = new Label();
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

        Button[] tabs = {mainTab, dimensionsTab, voidTab, seedTab};
        for (int i = 0; i < tabs.length; i++) {
            int index = i;
            tabs[i].setFocusTraversable(false);
            tabs[i].setOnAction(event -> {
                select(index);
                refresh();
            });
        }
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
        next.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 8 18; -fx-cursor: hand; -fx-background-radius: 8;"
                + " -fx-border-radius: 8; -fx-text-fill: #0b0e14; -fx-background-color: " + COLOR + "; -fx-border-color: #ffffff;");
        next.setOnAction(event -> {
            if (game.startNextUniverse()) onNextUniverse.run();
            refresh();
        });
        nextLine.setStyle("-fx-font-size: 12px; -fx-text-fill: #c8d4ea;");
        nextLine.setWrapText(true);
        nextLine.setMaxWidth(640);
        nextLine.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        VBox nextBox = new VBox(4, next, nextLine);
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

        // « Vide » : l'énergie du vide, et son visuel.
        voidLine.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: " + VOID_COLOR + ";");
        Label voidHint = new Label("Le deuxième acte de la partie secondaire. Il n'y a rien à y faire pour l'instant.");
        voidHint.setStyle("-fx-font-size: 12px; -fx-text-fill: #8fa3b8;");
        CanvasPane voidHolder = CanvasPane.filling(voidView);
        VBox.setVgrow(voidHolder, Priority.ALWAYS);
        voidPane = new VBox(8, voidLine, voidHint, voidHolder);
        voidPane.setAlignment(Pos.TOP_CENTER);

        // « Graine d'univers » : le nombre, en grand, et la graine.
        seedLine.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + SEED_COLOR + ";");
        Label seedHint = new Label("La ressource principale de l'univers. Former l'univers en donne une, et remet à zéro tout ce qui y a mené."
                + " Elle ne sert à rien pour l'instant : la suite viendra.");
        seedHint.setWrapText(true);
        seedHint.setMaxWidth(560);
        seedHint.setStyle("-fx-font-size: 12px; -fx-text-fill: #b8ab80; -fx-text-alignment: center;");
        CanvasPane seedHolder = CanvasPane.filling(seedView);
        VBox.setVgrow(seedHolder, Priority.ALWAYS);
        seedPane = new VBox(8, seedLine, seedHint, seedHolder);
        seedPane.setAlignment(Pos.TOP_CENTER);

        pages = new StackPane(mainPane, dimensionsPane, voidPane, seedPane);
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

    /** L'outil d'essai repasse à « cacher » : une nouvelle partie, un chargement. */
    public void forgetOldTabs() {
        showOldTabs = false;
    }

    public void select(int index) {
        if (index == DIMENSIONS && !game.isDimensionsUnlocked()) index = MAIN;
        if (index == VOID && !isVoidOpen()) index = MAIN;
        selected = index;
        mainPane.setVisible(index == MAIN);
        dimensionsPane.setVisible(index == DIMENSIONS);
        voidPane.setVisible(index == VOID);
        seedPane.setVisible(index == SEED);
        mainTab.setStyle(style(index == MAIN, COLOR));
        seedTab.setStyle(style(index == SEED, SEED_COLOR));
        dimensionsTab.setStyle(style(index == DIMENSIONS, COLOR));
        voidTab.setStyle(style(index == VOID, VOID_COLOR));
    }

    public int selected() {
        return selected;
    }

    /** Passe au sous-onglet suivant ou précédent, en boucle, parmi ceux qui sont ouverts. */
    public void step(int direction) {
        java.util.List<Integer> shown = new java.util.ArrayList<>(java.util.List.of(MAIN, SEED));
        if (game.isDimensionsUnlocked()) shown.add(DIMENSIONS);
        if (isVoidOpen()) shown.add(VOID);
        int at = Math.max(0, shown.indexOf(selected));
        select(shown.get(Math.floorMod(at + direction, shown.size())));
        refresh();
    }

    /** Le Vide s'ouvre dès qu'il y a de l'énergie du vide (pour l'instant, par l'outil d'essai). */
    private boolean isVoidOpen() {
        return game.state().voidEnergy().sign() > 0;
    }

    private static String style(boolean selected, String color) {
        return SUB_TAB_STYLE + (selected
                ? " -fx-text-fill: " + color + "; -fx-border-color: transparent transparent " + color + " transparent;"
                : " -fx-text-fill: #8fa3b8; -fx-border-color: transparent;");
    }

    /** Recopie l'état de la partie secondaire. */
    public void refresh() {
        int universes = Math.max(1, game.universes());
        view.setUniverses(universes);
        boolean dims = game.isDimensionsUnlocked();
        dimensionsTab.setVisible(dims);
        dimensionsTab.setManaged(dims);
        boolean voidOpen = isVoidOpen();
        voidTab.setVisible(voidOpen);
        voidTab.setManaged(voidOpen);
        if ((selected == DIMENSIONS && !dims) || (selected == VOID && !voidOpen)) select(MAIN);
        resources.setText(Format.count(game.state().universeSeeds())
                + (game.state().universeSeeds().gt(BigNum.ONE) ? " graines d'univers" : " graine d'univers")
                + "   |   " + universes + (universes > 1 ? " univers" : " univers")
                + (universes > UniverseView.MAX_BUBBLES ? " (" + UniverseView.MAX_BUBBLES + " dessinés)" : ""));
        for (Map.Entry<UniverseUpgrade, Card> entry : upgradeCards.entrySet()) {
            UniverseUpgrade upgrade = entry.getKey();
            int level = game.universeUpgradeLevel(upgrade);
            entry.getValue().show(Card.State.READY, level > 0 ? String.valueOf(level) : "", "", upgrade.label(),
                    upgrade.flavor(), "Pas encore d'effet : un clic monte son niveau, pour essayer.", "Essai : +1 niveau", "");
        }
        removeUniverse.setDisable(universes <= 1);
        boolean replaying = game.isReplaying();
        next.setVisible(!replaying);
        next.setManaged(!replaying);
        next.setText("Rouvrir les paliers : jouer l'univers " + (universes + 1));
        nextLine.setText(replaying
                ? "La partie de l'univers " + (universes + 1) + " est en cours, dans les anciens onglets : former l'univers, à la fin, le crée."
                : "Les anciens onglets reviennent pour une partie neuve. Former l'univers à la fin de cette partie crée le suivant : une remise à zéro et une graine de plus."
                        + (game.isSkinsUnlocked() ? "" : " Dès la cinquième fusion de cette partie, l'onglet Skins s'ouvre."));
        oldTabs.setText(showOldTabs ? "Cacher les anciens onglets" : "Montrer les anciens onglets");
        int open = game.dimensionsUnlocked();
        dimensions.setUnlocked(open);
        dimensionsLine.setText(open + " / " + Game.MAX_DIMENSIONS + " dimensions ouvertes : une de plus à chaque univers, jusqu'au dixième."
                + " Cliquez une dimension ouverte pour y entrer.");
        voidLine.setText("Énergie du vide : " + Format.count(game.state().voidEnergy()));
        BigNum seeds = game.state().universeSeeds();
        seedLine.setText(Format.count(seeds) + (seeds.gt(BigNum.ONE) ? " graines d'univers" : " graine d'univers"));
        seedView.setSeeds(seeds.toDouble());
        seedTab.setText("Graine d'univers (" + Format.count(seeds) + ")");
        mainTab.setText("Univers (" + universes + ")");
        dimensionsTab.setText("Dimensions (" + open + "/" + Game.MAX_DIMENSIONS + ")");
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
        switch (selected) {
            case DIMENSIONS -> dimensions.frame(elapsed, moving);
            case VOID -> voidView.frame(elapsed, moving);
            case SEED -> seedView.frame(elapsed, moving);
            default -> view.frame(elapsed, moving);
        }
    }

    /** La vue des univers : pour les vérifications. */
    public UniverseView view() {
        return view;
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
