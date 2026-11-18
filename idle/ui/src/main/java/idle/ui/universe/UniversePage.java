package idle.ui.universe;

import idle.core.Game;
import idle.core.universe.SkinVisual;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import idle.core.math.BigNum;
import idle.ui.common.CanvasPane;
import idle.ui.common.Format;
import idle.ui.common.Labels;
import idle.ui.common.SubTabs;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * L'onglet « Univers » : la partie secondaire du jeu, qui commence quand l'univers est formé . Trois sous-onglets :
 * <ul>
 * <li>« Univers » : les univers en bulles et les graines d'univers (les quatre améliorations de la
 * version 0.081, Brane, Bulk, Paysage et Omnivers, sans prix ni effet, sont parties à la version 0.213) ;</li>
 * <li>« Graine d'univers » : la ressource principale, en grand, et son visuel ;</li>
 * <li>« Dimensions », dès le premier univers : dix points autour d'un onzième, une dimension de plus par univers ;
 * chacune ouvre sa fenêtre. Depuis la version 0.160, le défi final y montre l'avancée de chaque dimension vers son
 * palier ; les dix atteints pendant qu'on joue, la fin s'y joue seule, puis l'écran de fin ; « Revoir la fin » la
 * rejoue ;</li>
 * </ul> Le sous-onglet « Vide » est parti à la version 0.142 : le vide se joue dans son onglet, et son cumul, dans la
 * graine, paie les bulles de l'arbre.
 */
public final class UniversePage extends VBox {
    /** La couleur de l'univers : un bleu pervenche, à part des autres onglets. */
    public static final String COLOR = "#a8b8ff";
    /** La couleur du Vide : un cyan pâle. */
    public static final String VOID_COLOR = "#9fe6ff";

    static final int MAIN = 0;
    static final int DIMENSIONS = 1;
    /** Version 0.142 : plus de sous-onglet « Vide » ; le vide se joue dans son onglet, son cumul est dans la graine. */
    public static final int SEED = 2;
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
    /** Rouvrir les paliers : jouer la partie qui formera l'univers suivant. */
    private final Button next = new Button();
    private final Label nextLine = new Label();
    private Runnable onNextUniverse = () -> { };
    /** Ce qui précède le lancement de l'univers suivant : le guide de transition. */
    private Consumer<Runnable> confirmLaunch = Runnable::run;

    /** Le guide à montrer avant de lancer l'univers suivant. */
    public void setLaunchConfirm(Consumer<Runnable> confirm) {
        confirmLaunch = confirm == null ? Runnable::run : confirm;
    }
    /** Le lancement de l'univers suivant, joué par-dessus la page. */
    private final LaunchView launch = new LaunchView();
    private double pulse;
    private final VBox mainPane;

    // Sous-onglet « Dimensions »
    private final DimensionsView dimensions = new DimensionsView();
    private final Label dimensionsLine = new Label();
    private final VBox dimensionsPane;
    /** Le défi final : rejouer la fin, une fois la partie finie. */
    private final Button replayFinale = tool("Revoir la fin");
    private Runnable onReplay = () -> { };
    /** La partie était-elle finie au dernier relevé ; {@code null} tant qu'aucun relevé n'a été fait (un chargement). */
    private Boolean finishedSeen;

    // Sous-onglet « Graine d'univers »
    private final SeedView seedView = new SeedView();
    private final Label seedLine = new Label();
    private final Label voidCumul = new Label();
    /**
     * Où en est la graine (version 0.137 ; avant, le bouton qui la faisait germer puis l'arrosait) : l'arrosoir le
     * remplace, sur son étagère de la vue.
     */
    private final Label seedStatus = new Label();
    /** Les améliorations de la graine et le chrono, à gauche de sa vue. */
    private final SeedUpgradesPane seedUpgrades;
    private double upgradesTimer;
    /** Le bouton posé sur l'étagère, qui débloque l'arrosoir. */
    private final Button unlockCan = new Button("Débloquer l'arrosoir");
    /** Ce que l'arrosoir avait versé à la dernière image : pour saluer la brindille et rafraîchir la page. */
    private long lastWaterings = -1;
    private final VBox seedPane;

    private final StackPane pages;
    private IntConsumer onOpenDimension = number -> { };

    public UniversePage(Game game) {
        super(6);
        this.game = game;
        setAlignment(Pos.TOP_CENTER);
        setPadding(new Insets(0, 16, 10, 16));

        SubTabs.wire(true, this::select, this::refresh, mainTab, dimensionsTab, seedTab);
        subTabs.setAlignment(Pos.CENTER);

        // « Univers » : les bulles, les ressources, le lancement de l'univers suivant.
        resources.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: " + COLOR + ";");
        CanvasPane holder = CanvasPane.filling(view);
        holder.setMinHeight(220);
        VBox.setVgrow(holder, Priority.ALWAYS);
        next.setFocusTraversable(false);
        // Comme le bouton du tout début de la partie, il respire, et il faut cliquer pour lancer
        // l'univers. Le clic joue le lancement ; à son éclat, l'univers part et l'onglet Particules revient.
        next.setStyle("-fx-font-size: 16px; -fx-padding: 12 28; -fx-cursor: hand; -fx-text-fill: #e8f4ff;"
                + " -fx-background-color: #16202e; -fx-background-radius: 8; -fx-border-color: " + COLOR + "; -fx-border-radius: 8;");
        next.setOnAction(event -> {
            if (!game.canOpenNextUniverse() || launch.isPlaying()) return;
            // Le guide de transition d'abord ; le lancement ne part que si le joueur confirme.
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
        mainPane = new VBox(8, resources, nextBox, holder);
        mainPane.setAlignment(Pos.TOP_CENTER);

        // « Dimensions » : la carte, et une ligne.
        dimensionsLine.setStyle("-fx-font-size: 13px; -fx-text-fill: #c8d4ea;");
        dimensions.setOnOpen(number -> onOpenDimension.accept(number));
        replayFinale.setOnAction(event -> {
            playFinale();
            onReplay.run();
        });
        CanvasPane dimensionsHolder = CanvasPane.filling(dimensions);
        VBox.setVgrow(dimensionsHolder, Priority.ALWAYS);
        HBox dimensionsHead = new HBox(12, dimensionsLine, replayFinale);
        dimensionsHead.setAlignment(Pos.CENTER);
        dimensionsPane = new VBox(8, dimensionsHead, dimensionsHolder);
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
        unlockCan.setOnAction(event -> { if (game.unlockWateringCan()) refresh(); });
        // L'étagère est dessinée dans la vue ; son bouton se tient au-dessus d'elle, au même endroit.
        Pane canLayer = new Pane(unlockCan);
        canLayer.setPickOnBounds(false);
        seedHolder.setMinSize(0, 0);
        StackPane seedStage = new StackPane(seedHolder, canLayer);
        // Les bulles de l'arbre : un clic sur l'une l'achète, avec l'énergie du vide accumulée.
        seedView.setOnBubble(this::clickBubble);
        // Le rond de croissance : un clic le cueille, et l'arrosage accélère.
        seedView.setOnBloom(() -> { if (game.catchBloom()) refresh(); });
        VBox.setVgrow(seedStage, Priority.ALWAYS);
        // Le cumul du vide : ce qu'il restait d'énergie du vide à la formation de chaque univers.
        voidCumul.setStyle("-fx-font-size: 13px; -fx-text-fill: " + VOID_COLOR + "; -fx-text-alignment: center;");
        voidCumul.setWrapText(true);
        voidCumul.setMaxWidth(620);
        seedUpgrades = new SeedUpgradesPane(game);
        ScrollPane upgradesScroll = new ScrollPane(seedUpgrades);
        upgradesScroll.setFitToWidth(true);
        upgradesScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
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

    /** Un petit bouton doré : « Revoir la fin ». */
    private static Button tool(String text) {
        Button button = new Button(text);
        button.setFocusTraversable(false);
        button.setStyle(TOOL_STYLE);
        return button;
    }

    /** La barre des sous-onglets : la disposition à gauche la place en haut de la fenêtre. */
    public HBox subTabs() { return subTabs; }

    /** Ce que fait l'application quand la partie de l'univers suivant commence : montrer les anciens onglets. */
    public void setOnNextUniverse(Runnable action) {
        onNextUniverse = action == null ? () -> { } : action;
    }

    /** Ce que fait un clic sur une dimension ouverte : son numéro, de 1 à 10. */
    public void setOnOpenDimension(IntConsumer action) {
        onOpenDimension = action == null ? number -> { } : action;
    }

    /** La ligne du cumul du vide, dans le sous-onglet Graine : pour les vérifications. */
    public Label voidCumulLabel() { return voidCumul; }

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

    public int selected() { return selected; }

    /** Passe au sous-onglet suivant ou précédent, en boucle, parmi ceux qui sont ouverts. */
    public void step(int direction) {
        List<Integer> shown = new ArrayList<>(List.of(MAIN, SEED));
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
                // Le bonus permanent des univers formés.
                + (universes > 0 ? "   |   matière noire et sa croissance " + Format.multiplier(BigNum.of(game.universeDarkMatterFactor())) : ""));
        boolean replaying = game.isReplaying();
        // Le deuxième univers attend la brindille de la graine ; il s'ouvre alors tout seul.
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
        dimensions.setMarked(10, game.isDimensionThresholdReached(10));
        boolean challenge = game.isFinalChallengeOpen();
        dimensions.setFinalOpen(challenge);
        dimensions.setFinished(game.isGameFinished());
        dimensions.setFinalTiers(game.finalTiersReached());
        for (int number = 1; number <= Game.MAX_DIMENSIONS; number++) {
            dimensions.setFinalProgress(number, game.finalProgress(number), game.isFinalTierReached(number),
                    finalText(number));
        }
        replayFinale.setVisible(game.isGameFinished());
        replayFinale.setManaged(replayFinale.isVisible());
        dimensionsLine.setText(game.isGameFinished()
                ? "Le défi final est réussi : les dix paliers sont atteints, la partie est finie. Le jeu, lui, continue."
                : challenge
                ? "Le défi final : " + game.finalTiersReached() + " / " + Game.MAX_DIMENSIONS + " paliers atteints. Chaque "
                        + "dimension se remplit de vert vers son palier, en puissances de dix ; les dix atteints, la partie est finie."
                : open + " / " + Game.MAX_DIMENSIONS + " dimensions ouvertes : une de plus à chaque univers, jusqu'au dixième."
                        + " Cliquez une dimension ouverte pour y entrer.");
        Map<String, Double> history = game.state().voidHistory();
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
            // Elle paie les bulles de l'arbre ; la ligne dit ce qu'il en reste à dépenser.
            voidCumul.setText("Énergie du vide accumulée : " + Format.count(BigNum.of(game.voidAccumulated())) + " (" + each
                    + "), ce qu'il restait à la formation de chaque univers. À dépenser pour les bulles de l'arbre : "
                    + Format.count(BigNum.of(game.voidAvailable())) + ".");
        }
        // Il n'y a qu'une graine : pas de compteur, la graine est là dès que l'univers est formé.
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
                // La graine profonde : ce qu'elle multiplie, une fois achetée.
                + (game.deepSeed() > 1 + 1e-9 ? ", graine profonde " + Format.multiplier(BigNum.of(game.deepSeed())) : "") + ")";
        seedStatus.setText(stage >= Game.SEED_STAGES ? "L'arbre-monde : la graine a fini de grandir"
                : game.isSeedStageCapped()
                ? can + Game.SEED_STAGE_NAMES.get(stage - 1).substring(0, 1).toUpperCase() + Game.SEED_STAGE_NAMES.get(stage - 1).substring(1)
                        + " attend l'univers " + (game.universes() + 1) + " pour passer au palier " + (stage + 1) + " ; "
                        + waterings + " sur " + nextWaterings + " arrosages déjà versés" + pace
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

    /** « 3.2e6 / 2.5e7 », ou « Palier atteint : 2.5e7 » : l'avancée d'une dimension vers son palier. */
    private String finalText(int number) {
        String goal = Format.darkMatter(game.finalGoalFor(number));
        return game.isFinalTierReached(number) ? "Palier atteint : " + goal
                : Format.darkMatter(game.dimensionEarned(number)) + " / " + goal;
    }

    /**
     * Le défi final, à chaque image : quand la partie vient d'être finie pendant qu'on joue, la carte des dimensions se
     * montre et la fin s'y joue. Une partie déjà finie au chargement (le premier relevé, ou après {@link #forgetFinale})
     * ne la rejoue pas. Vrai quand la fin vient de commencer : l'application montre alors l'onglet Univers.
     */
    public boolean checkFinale() {
        boolean done = game.isGameFinished();
        boolean start = finishedSeen != null && !finishedSeen && done;
        finishedSeen = done;
        if (start) playFinale();
        return start;
    }

    /** Une partie chargée ou remise à zéro : le prochain relevé reprend la fin telle qu'elle est, sans la jouer. */
    public void forgetFinale() { finishedSeen = null; }

    /** Joue la fin sur la carte des dimensions, qui se montre. */
    public void playFinale() {
        if (!game.isDimensionsUnlocked()) return;
        select(DIMENSIONS);
        refresh();
        dimensions.playFinale();
    }

    /** Vrai tant que la fin se joue sur la carte. */
    public boolean isFinalePlaying() { return dimensions.isFinalePlaying(); }

    /** Ce que fait « Revoir la fin » en plus de la rejouer (version 0.270 : le son de la fin, que l'annonce ne rejoue pas). */
    public void setOnReplay(Runnable action) { onReplay = action == null ? () -> { } : action; }

    /** Ce qui suit la fin : l'écran de fin. */
    public void setOnFinale(Runnable action) { dimensions.setOnFinale(action); }

    /** Le bouton « Revoir la fin » : pour les vérifications. */
    public Button replayFinaleButton() { return replayFinale; }

    /** La ligne au-dessus de la carte des dimensions : pour les vérifications. */
    public String dimensionsLineText() { return dimensionsLine.getText(); }

    /**
     * Ce qui manque à la graine pour ouvrir l'univers {@code number} : son palier, et ce qu'il demande (version 0.120 ;
     * Seulement des arrosages, le seuil de la dimension ne retient plus la graine depuis la 0.139).
     */
    private String nextStageLine(int number) {
        int stage = Math.min(number, Game.SEED_STAGES);
        String name = Game.SEED_STAGE_NAMES.get(stage - 1);
        long need = Game.seedStageWaterings(stage);
        return "L'univers " + number + " s'ouvre quand la graine atteint son palier " + stage + ", " + name + " : "
                + Math.min(game.state().seedWaterings(), need) + " / " + need + " arrosages"
                + (game.state().seedSprouted() ? "." : ", une fois la graine germée.");
    }

    /** Joue la naissance d'un univers : il vient de se former. */
    public void playBirth() {
        view.setUniverses(Math.max(1, game.universes()));
        view.playBirth();
    }

    /** Vrai tant que la naissance se joue. */
    public boolean isBirthPlaying() { return view.isBirthPlaying(); }

    /** Fait avancer l'animation du sous-onglet affiché ; {@code moving} faux (réglage coupé) la laisse immobile. */
    public void frame(double elapsed, boolean moving) {
        // Le lancement avance même animations coupées : c'est lui qui lance l'univers.
        boolean wasPlaying = launch.isPlaying();
        launch.setSkin(game.chosenSkin(SkinVisual.LAUNCH));
        launch.frame(elapsed);
        if (wasPlaying && !launch.isPlaying()) refresh();
        pulse += Math.min(elapsed, 0.1);
        double scale = moving && next.isVisible() ? 1 + 0.03 * (1 + Math.sin(pulse * Math.PI / 1.1)) : 1;
        next.setScaleX(scale);
        next.setScaleY(scale);
        // L'arrosoir : penché sur la graine, il arrose ; le jeu compte, la page suit.
        game.setWatering(selected == SEED && seedView.isPouring());
        long waterings = game.state().seedWaterings() + (game.state().seedSprouted() ? 1 : 0);
        if (lastWaterings >= 0 && waterings != lastWaterings) {
            // La brindille ouvre le deuxième univers : un éclat la salue.
            if (game.isSeedTwig() && game.state().seedWaterings() == Game.SEED_TWIG_WATERINGS) seedView.celebrate();
            refresh();
        }
        lastWaterings = waterings;
        // La fin avance même si le joueur change de sous-onglet : l'écran de fin vient quand même.
        if (selected != DIMENSIONS && dimensions.isFinalePlaying()) dimensions.frame(elapsed, moving);
        switch (selected) {
            case DIMENSIONS -> {
                // La carte et le remplissage du défi final, et la palette que leurs points reprennent.
                DimensionPalette.setSkin(game.chosenSkin(SkinVisual.DIMENSION_PALETTE));
                dimensions.setSkins(game.chosenSkin(SkinVisual.DIMENSIONS_MAP), game.chosenSkin(SkinVisual.FINALE_FILL));
                dimensions.frame(elapsed, moving);
            }
            case SEED -> {
                // Le tuyau et le chrono : ce que la vue en montre suit le jeu à chaque image.
                seedView.setHose(game.hasHose(), game.hasHose() && game.canTendSeed());
                seedView.setChrono(game.isChronoUnlocked(), game.chronoCapacity() > 0 ? game.chronoStored() / game.chronoCapacity() : 0,
                        game.isChronoRunning());
                seedView.setBubbles(bubbles());
                // Le rond de croissance : là où le jeu le fait pousser, et son effet en cours.
                seedView.setBloom(game.isBloomVisible(), game.bloomX(), game.bloomY(), game.bloomVisibleLeft(), Game.BLOOM_VISIBLE);
                seedView.setBloomEffect(game.bloomActiveLeft(), game.bloomGain());
                // La météo : le temps qu'il fait, et son effet sur l'arrosage.
                seedView.setWeather(game.isWeatherUnlocked() ? game.weather() : null, game.weatherLeft(), game.weatherFactor());
                // Les saisons : la saison, ce qu'il lui reste, et ce qu'un arrosage verse au chrono.
                seedView.setSeason(game.areSeasonsUnlocked() ? game.season() : null, game.seasonLeft(), game.seasonFillPerWatering());
                // L'essence de l'arbre : le skin choisi dans l'onglet Skins, null pour le Classique.
                seedView.setSkin(game.chosenSkin(SkinVisual.TREE));
                // L'établi de la graine : la graine, l'arrosoir, l'étagère, le tuyau, le chrono, le rond, la météo, les saisons.
                for (SkinVisual visual : SeedView.ESTABLISHMENT) seedView.setVisualSkin(visual, game.chosenSkin(visual));
                // L'arbre au-delà de son essence : la saison figée, la fantaisie, le sol, les bulles et leurs liens.
                for (SkinVisual visual : SeedView.TREE_LOOKS) seedView.setVisualSkin(visual, game.chosenSkin(visual));
                seedView.frame(elapsed, moving);
                placeUnlockCan();
                upgradesTimer += elapsed;
                if (upgradesTimer >= 0.25) {
                    upgradesTimer = 0;
                    seedUpgrades.refresh();
                }
            }
            default -> {
                // Les skins des bulles, de la naissance et des liens, et celui du lancement.
                view.setSkins(game.chosenSkin(SkinVisual.UNIVERSE_BUBBLE), game.chosenSkin(SkinVisual.UNIVERSE_BIRTH),
                        game.chosenSkin(SkinVisual.UNIVERSE_LINKS));
                view.frame(elapsed, moving);
            }
        }
    }

    /** La vue des univers : pour les vérifications. */
    public UniverseView view() { return view; }

    /** Le lancement de l'univers suivant : pour les vérifications. */
    public LaunchView launchView() { return launch; }

    /** Le bouton de l'étagère pose l'arrosoir au-dessus de la planche, au milieu. */
    private void placeUnlockCan() {
        if (!unlockCan.isVisible()) return;
        double width = unlockCan.prefWidth(-1), height = unlockCan.prefHeight(width);
        unlockCan.resizeRelocate(Math.max(4, Math.min(seedView.getWidth() - width - 4, seedView.shelfX() - width / 2)),
                Math.max(4, seedView.shelfMiddleY() - height / 2), width, height);
    }

    /** Ce que dit la page de la graine, et le bouton qui débloque l'arrosoir : pour les vérifications. */
    public Label seedStatus() { return seedStatus; }

    /** Les bulles de l'arbre, comme la vue les dessine : leur place, leur prix, leur état, leurs liens. */
    private List<SeedView.Bubble> bubbles() {
        List<Game.TreeBubble> shown = game.treeBubblesShown();
        List<SeedView.Bubble> list = new ArrayList<>();
        for (Game.TreeBubble bubble : shown) {
            // Celles qui se rachètent restent à acheter jusqu'à leur dernier niveau.
            int level = game.treeBubbleLevel(bubble);
            boolean many = bubble.maxLevel() > 1;
            SeedView.BubbleState state = game.isTreeBubbleMaxed(bubble) ? SeedView.BubbleState.BOUGHT
                    : !game.isTreeBubbleOpen(bubble) ? SeedView.BubbleState.CLOSED
                    : game.canBuyTreeBubble(bubble) ? SeedView.BubbleState.BUY : SeedView.BubbleState.SHORT;
            double cost = game.treeBubbleCost(bubble);
            String levels = many ? "Niveau " + level + " / " + bubble.maxLevel() + ". " : "";
            String price = switch (state) {
                case BOUGHT -> many ? "Niveau " + level + " / " + bubble.maxLevel() + " : au plus haut" : "Achetée";
                // La bulle des racines s'ouvre par la dernière amélioration de la graine.
                case CLOSED -> bubble == Game.TreeBubble.ROOTS && game.seedUpgradeLevel(Game.SeedUpgrade.ROOT_BUBBLE) == 0
                        ? "Ouverte par l'amélioration de graine " + Game.SeedUpgrade.ROOT_BUBBLE.label()
                        : shownLinks(bubble, shown).size() == 1 ? "Il faut d'abord : " + shownLinks(bubble, shown).get(0).label()
                        : "Il faut d'abord une bulle reliée à elle";
                case BUY -> levels + "Prix : " + Format.count(BigNum.of(cost)) + " d'énergie du vide";
                case SHORT -> levels + "Prix : " + Format.count(BigNum.of(cost)) + " d'énergie du vide (il en manque "
                        + Format.count(BigNum.of(Math.ceil(cost - game.voidAvailable()))) + ")";
            };
            List<Integer> below = new ArrayList<>();
            for (Game.TreeBubble under : bubble.below()) if (shown.contains(under)) below.add(shown.indexOf(under));
            list.add(new SeedView.Bubble(bubble.ordinal(), bubble.label(), bubble.effect(), price, state, below));
        }
        return list;
    }

    /** Les bulles nées reliées à celle-ci : celles qui peuvent l'ouvrir. */
    private static List<Game.TreeBubble> shownLinks(Game.TreeBubble bubble, List<Game.TreeBubble> shown) {
        List<Game.TreeBubble> links = new ArrayList<>(bubble.links());
        links.retainAll(shown);
        return links;
    }

    /** Un clic sur une bulle : achetée si elle peut l'être. */
    private void clickBubble(int index) {
        List<Game.TreeBubble> shown = game.treeBubblesShown();
        if (index < 0 || index >= shown.size()) return;
        if (game.buyTreeBubble(shown.get(index))) refresh();
        seedView.setBubbles(bubbles());
    }

    public SeedUpgradesPane seedUpgrades() { return seedUpgrades; }

    public Button unlockCanButton() { return unlockCan; }

    /** La graine d'univers : pour les vérifications. */

    public SeedView seedView() { return seedView; }

    /** La carte des dimensions : pour les vérifications. */
    public DimensionsView dimensionsView() { return dimensions; }
}
