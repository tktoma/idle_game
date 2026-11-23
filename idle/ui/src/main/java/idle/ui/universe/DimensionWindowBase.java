package idle.ui.universe;

import idle.core.Game;
import idle.core.universe.DimensionAutomation;
import idle.core.universe.SkinVisual;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.KeyCode;
import javafx.scene.text.TextAlignment;
import idle.ui.common.CanvasPane;
import idle.ui.common.Detail;
import idle.ui.common.ElementText;
import idle.ui.common.Format;
import idle.ui.common.Labels;
import idle.core.math.BigNum;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/** Une partie de {@link DimensionWindow} (découpée en 0.293 pour rester sous 1 000 lignes) : ses champs, la construction de la fenêtre et ce qui se rafraîchit pour toutes les dimensions. */
public abstract class DimensionWindowBase extends BorderPane {
    // Ce que fournissent les parties suivantes.
    abstract <U extends Enum<U>> void paintUpgrades(Map<U, Button> buttons, Function<U, String> describe, ToIntFunction<U> level, ToIntFunction<U> max, ToDoubleFunction<U> cost, Predicate<U> canBuy, String unit, String accent);
    abstract DimensionView shown(Dimension dimension);
    abstract String describe(Game.BraneUpgrade upgrade);
    abstract String describe(Game.ClickerUpgrade upgrade);
    abstract String describe(Game.DurationUpgrade upgrade);
    abstract String describe(Game.GridUpgrade upgrade);
    abstract String describe(Game.LineUpgrade upgrade);
    abstract String describe(Game.LoopUpgrade upgrade);
    abstract String describe(Game.MirrorUpgrade upgrade);
    abstract String describe(Game.MixUpgrade upgrade);
    abstract String describe(Game.PlaneUpgrade upgrade);
    abstract String describe(Game.VolumeUpgrade upgrade);
    abstract String loopKeysText();
    abstract String mirrorPaceText();
    abstract boolean changed(Object node, String key);
    abstract boolean mixKey(KeyCode code, boolean down);
    abstract int loopRingOf(KeyCode code);
    abstract public String mixStageText();
    abstract public void close();
    abstract void refreshBrane();
    abstract void refreshClicker();
    abstract void refreshDuration();
    abstract void refreshGrid();
    abstract void refreshLoop();
    abstract void refreshMirror();
    abstract void refreshMix();
    abstract void refreshPlane();
    abstract void refreshVolume();
    abstract void restyle();
    abstract void selectFromFields();
    abstract void steer();

    static final String BUTTON_STYLE = "-fx-font-size: 13px; -fx-padding: 8 16; -fx-cursor: hand; -fx-background-radius: 6;"
            + " -fx-border-radius: 6; -fx-text-fill: " + UniversePage.COLOR + "; -fx-background-color: #141a2c; -fx-border-color: #3b4670;";

    static final String UPGRADE_STYLE = "-fx-font-size: 13px; -fx-background-radius: 6; -fx-border-radius: 6; -fx-opacity: 1;";

    /**
     * La couleur de la dimension {@code number} (1 à 10), celle de ses éclats : ses améliorations achetables la
     * prennent ; celle de l'univers pour la ligne et au-delà. Celle de la palette choisie ({@link DimensionPalette}).
     */
    public static String color(int number) { return DimensionPalette.hex(number < 1 || number > 10 ? 1 : number); }

    /**
     * Les couleurs d'un bouton d'amélioration (version 0.140 : plus visibles, même format). Achetable, il prend la couleur
     * de sa dimension : bordure épaisse, halo, texte blanc. Sinon il reste lisible (JavaFX éteint à 40 % ce qui est
     * désactivé, le bouton garde son opacité). Au plus haut, il passe au vert. La bordure plus épaisse prend sur le
     * retrait du texte : le bouton ne change pas de taille.
     */
    static void paintUpgrade(Button button, boolean buyable, boolean full, String accent) {
        String look = full
                ? " -fx-padding: 8 16; -fx-border-width: 1; -fx-text-fill: #9be7a8; -fx-background-color: #14261b; -fx-border-color: #4f9a5c;"
                : buyable
                ? " -fx-padding: 7 15; -fx-border-width: 2; -fx-cursor: hand; -fx-text-fill: #ffffff;"
                        + " -fx-background-color: linear-gradient(#27314f, #1a2240); -fx-border-color: " + accent + ";"
                        + " -fx-effect: dropshadow(gaussian, " + accent + "66, 10, 0.25, 0, 0);"
                : " -fx-padding: 8 16; -fx-border-width: 1; -fx-text-fill: #9aa3bd; -fx-background-color: #10141f; -fx-border-color: #2c3450;";
        button.setStyle(UPGRADE_STYLE + look);
    }

    /** Les touches des sept cercles de la boucle tant que le joueur n'en a pas choisi d'autres. */
    public static final List<KeyCode> DEFAULT_LOOP_KEYS = List.of(KeyCode.S, KeyCode.D, KeyCode.F,
            KeyCode.SPACE, KeyCode.J, KeyCode.K,
            KeyCode.L);

    /**
     * Ce que la boucle demande au jeu : la touche de chaque cercle et le choix du métronome, rangés dans
     * les réglages, et le tic qu'il joue, au volume et selon le réglage des sons.
     */
    public interface LoopSettings {
        /** La touche du cercle {@code ring} (0 à 6), ou {@code null} s'il n'en a pas. */
        KeyCode loopKey(int ring);

        boolean metronome();

        void setMetronome(boolean on);

        /** Un battement du métronome : le tic, si les sons sont actifs. */
        void beat();
    }

    /** Les réglages de la boucle d'une fenêtre seule (pour les vérifications) : les touches d'origine, un métronome muet. */
    static final class LocalLoopSettings implements LoopSettings {
        private boolean metronome;

        @Override
        public KeyCode loopKey(int ring) {
            return ring >= 0 && ring < DEFAULT_LOOP_KEYS.size() ? DEFAULT_LOOP_KEYS.get(ring) : null;
        }

        @Override
        public boolean metronome() { return metronome; }

        @Override
        public void setMetronome(boolean on) { metronome = on; }

        @Override
        public void beat() {
        }
    }

    final Game game;

    final DimensionView view;

    /** La ligne : ses éclats, et ses cinq améliorations. */
    final Label shards = new Label();

    final Label threshold = new Label();

    final Map<Game.LineUpgrade, Button> buttons = new EnumMap<>(Game.LineUpgrade.class);

    final VBox linePane;

    /** Le plan : ses éclats, la manière de jouer, et ses cinq améliorations. */
    final Label planeShards = new Label();

    final Label planeThreshold = new Label();

    final Label planeHint = new Label();

    final Map<Game.PlaneUpgrade, Button> planeButtons = new EnumMap<>(Game.PlaneUpgrade.class);

    final VBox planePane;

    /** Le volume : ses éclats, son seuil, ce qu'on y fait, et ses cinq améliorations, dehors. */
    final Label volumeShards = new Label();

    final Label volumeThreshold = new Label();

    final Label volumeHint = new Label();

    final Label volumeDetail = new Label();

    /** Version 0.291 : le mode détails de la ligne et du plan, comme celui des autres dimensions. */
    final Label lineDetail = new Label(), planeDetail = new Label();

    final Map<Game.VolumeUpgrade, Button> volumeButtons = new EnumMap<>(Game.VolumeUpgrade.class);

    final FlowPane volumeRow = new FlowPane(12, 10);

    final VBox volumePane;

    /** La durée : ses éclats, son seuil, ce qu'on y fait, et ses quatre améliorations. */
    final Label durationShards = new Label();

    final Label durationThreshold = new Label();

    final Label durationHint = new Label();

    final Label durationDetail = new Label();

    final Map<Game.DurationUpgrade, Button> durationButtons = new EnumMap<>(Game.DurationUpgrade.class);

    final VBox durationPane;

    /** La boucle : ses nœuds, son seuil, ce qu'on y fait, son métronome, et ses six améliorations. */
    final Label loopShards = new Label();

    final Label loopThreshold = new Label();

    final Label loopHint = new Label();

    final Label loopDetail = new Label();

    final Button metronomeButton = new Button();

    final Map<Game.LoopUpgrade, Button> loopButtons = new EnumMap<>(Game.LoopUpgrade.class);

    final VBox loopPane;

    LoopSettings loopSettings = new LocalLoopSettings();

    /** La brane : ses ondes, son seuil, ce qu'on y fait, et ses cinq améliorations. */
    final Label braneShards = new Label();

    final Label braneThreshold = new Label();

    final Label braneHint = new Label();

    final Label braneDetail = new Label();

    final Map<Game.BraneUpgrade, Button> braneButtons = new EnumMap<>(Game.BraneUpgrade.class);

    final VBox branePane;

    /** Le clicker : ses étincelles, son seuil, ce qu'on y fait, et ses huit améliorations. */
    final Label clickerShards = new Label();

    final Label clickerThreshold = new Label();

    final Label clickerHint = new Label();

    final Label clickerDetail = new Label();

    final Map<Game.ClickerUpgrade, Button> clickerButtons = new EnumMap<>(Game.ClickerUpgrade.class);

    final VBox clickerPane;

    /** La grille : ses repères, son seuil, ce qu'on y fait, la couche montrée, et ses cinq améliorations. */
    final Label gridShards = new Label();

    final Label gridThreshold = new Label();

    final Label gridHint = new Label();

    final Label gridDetail = new Label();

    final Label gridLayerLabel = new Label();

    final Button gridDown = new Button("▼ Couche du dessous");

    final Button gridUp = new Button("Couche du dessus ▲");

    final Map<Game.GridUpgrade, Button> gridButtons = new EnumMap<>(Game.GridUpgrade.class);

    /** La taille de la grille, la case visée (x, y, z), le bouton qui l'essaie, et le zoom de la carte. */
    final Label gridSize = new Label();

    final TextField gridX = new TextField(), gridY = new TextField(), gridZ = new TextField();

    final Button gridTry = new Button("Essayer");

    final Button gridZoomIn = new Button("Zoom +"), gridZoomOut = new Button("Zoom −");

    final VBox gridPane;

    /** Le miroir : ses reflets, son seuil, ce qu'on y fait, ses compteurs, et ses cinq améliorations. */
    final Label mirrorShards = new Label();

    final Label mirrorThreshold = new Label();

    final Label mirrorHint = new Label();

    final Label mirrorCounts = new Label();

    /** Version 0.158 : la taille du plateau, et les réussites qu'il manque pour l'agrandir. */
    final Label mirrorBoard = new Label();

    final Label mirrorDetail = new Label();

    final Map<Game.MirrorUpgrade, Button> mirrorButtons = new EnumMap<>(Game.MirrorUpgrade.class);

    final VBox mirrorPane;

    /** La barre de vitesse du motif du miroir, et le temps par case qu'elle donne. */
    final Slider mirrorPace = new Slider();

    final Label mirrorPaceText = new Label();

    /** Ce que devient la vitesse du motif choisie : les réglages la gardent. */
    java.util.function.DoubleConsumer onMirrorPace = pace -> { };

    /**
     * Le mélange : au-dessus de l'arène, l'étape, sa barre de progression et le bouton qui mêle la
     * dimension suivante ; dessous, ses fragments, son seuil, ses compteurs (malus, multiplicateurs, charge, cadence),
     * ce qu'on y fait à l'étape en cours, le détail et ses onze améliorations.
     */
    final Label mixStage = new Label();

    final Label mixProgress = new Label();

    final Region mixBarFill = new Region(), mixBarRest = new Region();

    final Button mixAdvance = new Button("Mêler la dimension suivante");

    final VBox mixStageBox;

    final Label mixShards = new Label();

    final Label mixThreshold = new Label();

    final Label mixInfo = new Label();

    /** Les noms des trois axes de la grille, colorés de sa couleur : repris quand la palette change. */
    final List<Label> gridAxisNames = new ArrayList<>();

    /** Le cadre des mini-jeux : un calque sur l'arène, et la boîte qui lui fait sa marge. */
    final Canvas frameLayer = new Canvas();

    final StackPane framed;

    double frameTime;

    final Label mixHint = new Label();

    final Label mixDetail = new Label();

    final Map<Game.MixUpgrade, Button> mixButtons = new EnumMap<>(Game.MixUpgrade.class);

    final VBox mixPane;

    /** La clé de la rangée des améliorations du mélange, dans {@link #painted} : elle compte aussi l'étape. */
    final Object mixRowKey = new Object();

    /** La largeur de la barre de progression de l'étape. */
    static final double MIX_BAR_WIDTH = 420;

    /** Les touches de cercles tenues : une touche tenue se répète, mais ne rejoue pas son cercle. */
    final Set<KeyCode> loopKeysHeld = EnumSet.noneOf(KeyCode.class);

    /** La barre d'espace tient le bouton de la durée, ou la charge du mélange, comme la souris. */
    boolean spaceHeld;

    /** La caméra libre : le bouton, et où se range le choix (les réglages du jeu, ou la fenêtre seule). */
    final Button freeCameraButton = new Button();

    final BooleanSupplier freeCamera;

    final Consumer<Boolean> setFreeCamera;

    /** Les touches de direction tenues : Z, Q, S, D et les flèches. */
    final Set<KeyCode> held = EnumSet.noneOf(KeyCode.class);

    final Label title = new Label();

    /** La mention « automatique », à droite du titre : l'automatisation joue la dimension. */
    final Label autoBadge = new Label("automatique");

    final Label line = new Label();

    final Button back = new Button("← Retour");

    /** L'en-tête : le retour, le titre, la phrase, et depuis la version 0.203 la ligne du chrono ({@link #setChrono}). */
    VBox head;

    /**
     * Ce que chaque rangée de boutons et chaque explication a montré en dernier (niveaux, achetabilité, détails demandés) :
     * {@link #refresh()} tourne à chaque image, les longs textes ne se refont que si ça change.
     */
    final Map<Object, String> painted = new HashMap<>();

    /** Une fenêtre qui garde seule le choix de la caméra libre : pour les vérifications. */
    public DimensionWindowBase(Game game) { this(game, new boolean[1]); }

    /** Version 0.203 : range la ligne du chrono dans l'en-tête, sous la phrase ; elle se montre et se cache seule. */
    public void setChrono(Node chrono) { head.getChildren().add(chrono); }

    DimensionWindowBase(Game game, boolean[] free) { this(game, () -> free[0], value -> free[0] = value); }

    /** La fenêtre des dimensions ; le choix de la caméra libre du volume se lit et s'écrit par ces deux fonctions. */
    public DimensionWindowBase(Game game, BooleanSupplier freeCamera, Consumer<Boolean> setFreeCamera) {
        this.game = game;
        this.freeCamera = freeCamera;
        this.setFreeCamera = setFreeCamera;
        this.view = new DimensionView(game);
        setStyle("-fx-background-color: #05060e;");
        setPadding(new Insets(12, 16, 16, 16));
        back.setStyle("-fx-font-size: 13px; -fx-padding: 6 16; -fx-cursor: hand; -fx-background-radius: 6; -fx-border-radius: 6;"
                + " -fx-text-fill: " + UniversePage.COLOR + "; -fx-background-color: #141a2c; -fx-border-color: #3b4670;");
        back.setFocusTraversable(false);
        back.setOnAction(event -> close());
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + UniversePage.COLOR + ";");
        line.setStyle("-fx-font-size: 13px; -fx-text-fill: #c8d4ea;");
        line.setWrapText(true);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        // Une marge à droite aussi large que le bouton, pour que le titre reste au milieu ; la mention « automatique » s'y loge.
        autoBadge.setMinWidth(110);
        autoBadge.setAlignment(Pos.CENTER_RIGHT);
        autoBadge.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #9be7a8;");
        autoBadge.setVisible(false);
        Region after = new Region();
        HBox.setHgrow(after, Priority.ALWAYS);
        HBox top = new HBox(12, back, spacer, title, after, autoBadge);
        top.setAlignment(Pos.CENTER_LEFT);
        head = new VBox(6, top, line);
        head.setAlignment(Pos.CENTER);
        line.setMaxWidth(Double.MAX_VALUE);
        line.setAlignment(Pos.CENTER);
        head.setPadding(new Insets(0, 0, 10, 0));
        setTop(head);
        // Au-dessus de la vue, l'étape du mélange, cachée dans les autres dimensions.
        Labels.centered(mixStage, "-fx-font-size: 15px; -fx-font-weight: bold;");
        mixProgress.setStyle("-fx-font-size: 12px; -fx-text-fill: #c8d4ea;");
        mixBarRest.setStyle("-fx-background-color: #1a2032; -fx-background-radius: 0 4 4 0;");
        for (Region part : new Region[] {mixBarFill, mixBarRest}) {
            part.setMinHeight(10);
            part.setMaxHeight(10);
        }
        HBox mixBar = new HBox(0, mixBarFill, mixBarRest);
        mixBar.setAlignment(Pos.CENTER_LEFT);
        mixBar.setMaxWidth(MIX_BAR_WIDTH);
        mixAdvance.setStyle(BUTTON_STYLE);
        mixAdvance.setFocusTraversable(false);
        mixAdvance.setOnAction(event -> {
            game.advanceMix();
            refresh();
        });
        HBox mixBarRow = new HBox(12, mixBar, mixProgress, mixAdvance);
        mixBarRow.setAlignment(Pos.CENTER);
        mixStageBox = new VBox(6, mixStage, mixBarRow);
        mixStageBox.setAlignment(Pos.CENTER);
        mixStageBox.setPadding(new Insets(0, 0, 8, 0));
        CanvasPane holder = CanvasPane.filling(view);
        // Le cadre : un calque transparent aux clics, hors de la mise en page, de la taille de la boîte.
        frameLayer.setMouseTransparent(true);
        frameLayer.setManaged(false);
        framed = new StackPane(holder, frameLayer);
        frameLayer.widthProperty().bind(framed.widthProperty());
        frameLayer.heightProperty().bind(framed.heightProperty());
        VBox.setVgrow(framed, Priority.ALWAYS);
        VBox middle = new VBox(mixStageBox, framed);
        middle.setAlignment(Pos.CENTER);
        setCenter(middle);
        threshold.setStyle("-fx-font-size: 13px; -fx-text-fill: #9be7a8;");
        FlowPane row = new FlowPane(12, 10);
        row.setAlignment(Pos.CENTER);
        for (Game.LineUpgrade upgrade : Game.LineUpgrade.values()) {
            Button button = upgradeButton(250, () -> game.buyLine(upgrade));
            buttons.put(upgrade, button);
            row.getChildren().add(button);
        }
        Labels.note(lineDetail, 820);
        linePane = new VBox(10, shards, threshold, lineDetail, row);
        linePane.setAlignment(Pos.CENTER);
        linePane.setPadding(new Insets(10, 0, 0, 0));
        planeHint.setStyle("-fx-font-size: 13px; -fx-text-fill: #c8d4ea;");
        FlowPane planeRow = new FlowPane(12, 10);
        planeRow.setAlignment(Pos.CENTER);
        for (Game.PlaneUpgrade upgrade : Game.PlaneUpgrade.values()) {
            Button button = upgradeButton(250, () -> game.buyPlane(upgrade));
            planeButtons.put(upgrade, button);
            planeRow.getChildren().add(button);
        }
        planeThreshold.setStyle("-fx-font-size: 13px; -fx-text-fill: #9be7a8;");
        Labels.note(planeDetail, 820);
        planePane = new VBox(10, planeShards, planeThreshold, planeHint, planeDetail, planeRow);
        planePane.setAlignment(Pos.CENTER);
        planePane.setPadding(new Insets(10, 0, 0, 0));
        volumeThreshold.setStyle("-fx-font-size: 13px; -fx-text-fill: #9be7a8;");
        volumeHint.setStyle("-fx-font-size: 13px; -fx-text-fill: #c8d4ea;");
        volumeHint.setWrapText(true);
        Labels.note(volumeDetail, 820);
        volumeRow.setAlignment(Pos.CENTER);
        for (Game.VolumeUpgrade upgrade : Game.VolumeUpgrade.values()) {
            Button button = upgradeButton(200, () -> game.buyVolume(upgrade));   // les cinq tiennent sur une ligne : la vue du cube garde sa hauteur
            volumeButtons.put(upgrade, button);
            volumeRow.getChildren().add(button);
        }
        freeCameraButton.setStyle(BUTTON_STYLE);
        freeCameraButton.setFocusTraversable(false);
        freeCameraButton.setOnAction(event -> {
            this.setFreeCamera.accept(!this.freeCamera.getAsBoolean());
            view.setFreeCamera(this.freeCamera.getAsBoolean());
            refresh();
        });
        volumePane = new VBox(10, volumeShards, volumeThreshold, volumeHint, freeCameraButton, volumeDetail, volumeRow);
        volumePane.setAlignment(Pos.CENTER);
        volumePane.setPadding(new Insets(10, 0, 0, 0));
        durationThreshold.setStyle("-fx-font-size: 13px; -fx-text-fill: #9be7a8;");
        Labels.centered(durationHint, "-fx-font-size: 13px; -fx-text-fill: #c8d4ea;");
        durationHint.setMaxWidth(820);
        durationHint.setAlignment(Pos.CENTER);
        // Deux lignes toujours : le rond ne bouge pas quand le texte change, bouton tenu ou relâché.
        durationHint.setMinHeight(38);
        durationHint.setPrefHeight(38);
        Labels.note(durationDetail, 820);
        FlowPane durationRow = new FlowPane(12, 10);
        durationRow.setAlignment(Pos.CENTER);
        for (Game.DurationUpgrade upgrade : Game.DurationUpgrade.values()) {
            Button button = upgradeButton(230, () -> game.buyDuration(upgrade));
            durationButtons.put(upgrade, button);
            durationRow.getChildren().add(button);
        }
        durationPane = new VBox(10, durationShards, durationThreshold, durationHint, durationDetail, durationRow);
        durationPane.setAlignment(Pos.CENTER);
        durationPane.setPadding(new Insets(10, 0, 0, 0));
        loopThreshold.setStyle("-fx-font-size: 13px; -fx-text-fill: #9be7a8;");
        Labels.centered(loopHint, "-fx-font-size: 13px; -fx-text-fill: #c8d4ea;");
        loopHint.setMaxWidth(820);
        loopHint.setAlignment(Pos.CENTER);
        // Deux lignes toujours, comme pour la durée : les cercles ne bougent pas quand le texte change.
        loopHint.setMinHeight(38);
        loopHint.setPrefHeight(38);
        Labels.note(loopDetail, 820);
        metronomeButton.setStyle(BUTTON_STYLE);
        metronomeButton.setFocusTraversable(false);
        metronomeButton.setOnAction(event -> {
            loopSettings.setMetronome(!loopSettings.metronome());
            refresh();
        });
        FlowPane loopRow = new FlowPane(12, 10);
        loopRow.setAlignment(Pos.CENTER);
        for (Game.LoopUpgrade upgrade : Game.LoopUpgrade.values()) {
            Button button = upgradeButton(250, () -> game.buyLoop(upgrade));
            loopButtons.put(upgrade, button);
            loopRow.getChildren().add(button);
        }
        loopPane = new VBox(10, loopShards, loopThreshold, loopHint, metronomeButton, loopDetail, loopRow);
        loopPane.setAlignment(Pos.CENTER);
        loopPane.setPadding(new Insets(10, 0, 0, 0));
        braneThreshold.setStyle("-fx-font-size: 13px; -fx-text-fill: #9be7a8;");
        Labels.centered(braneHint, "-fx-font-size: 13px; -fx-text-fill: #c8d4ea;");
        braneHint.setMaxWidth(820);
        braneHint.setAlignment(Pos.CENTER);
        // Deux lignes toujours : la membrane ne bouge pas quand le texte change, tenue ou relâchée.
        braneHint.setMinHeight(38);
        braneHint.setPrefHeight(38);
        Labels.note(braneDetail, 820);
        FlowPane braneRow = new FlowPane(12, 10);
        braneRow.setAlignment(Pos.CENTER);
        for (Game.BraneUpgrade upgrade : Game.BraneUpgrade.values()) {
            Button button = upgradeButton(250, () -> game.buyBrane(upgrade));
            braneButtons.put(upgrade, button);
            braneRow.getChildren().add(button);
        }
        branePane = new VBox(10, braneShards, braneThreshold, braneHint, braneDetail, braneRow);
        branePane.setAlignment(Pos.CENTER);
        branePane.setPadding(new Insets(10, 0, 0, 0));
        clickerThreshold.setStyle("-fx-font-size: 13px; -fx-text-fill: #9be7a8;");
        Labels.centered(clickerHint, "-fx-font-size: 13px; -fx-text-fill: #c8d4ea;");
        clickerHint.setMaxWidth(820);
        clickerHint.setAlignment(Pos.CENTER);
        // Deux lignes toujours : la surface ne bouge pas quand la cadence change.
        clickerHint.setMinHeight(38);
        clickerHint.setPrefHeight(38);
        Labels.note(clickerDetail, 820);
        FlowPane clickerRow = new FlowPane(10, 8);
        clickerRow.setAlignment(Pos.CENTER);
        for (Game.ClickerUpgrade upgrade : Game.ClickerUpgrade.values()) {
            // Huit sur deux lignes : la surface garde sa hauteur.
            Button button = upgradeButton(200, () -> game.buyClicker(upgrade));
            clickerButtons.put(upgrade, button);
            clickerRow.getChildren().add(button);
        }
        clickerPane = new VBox(10, clickerShards, clickerThreshold, clickerHint, clickerDetail, clickerRow);
        clickerPane.setAlignment(Pos.CENTER);
        clickerPane.setPadding(new Insets(10, 0, 0, 0));
        gridThreshold.setStyle("-fx-font-size: 13px; -fx-text-fill: #9be7a8;");
        Labels.centered(gridHint, "-fx-font-size: 13px; -fx-text-fill: #c8d4ea;");
        gridHint.setMaxWidth(820);
        gridHint.setAlignment(Pos.CENTER);
        // Deux lignes toujours : la grille ne bouge pas quand le dernier essai change le texte.
        gridHint.setMinHeight(38);
        gridHint.setPrefHeight(38);
        Labels.note(gridDetail, 820);
        gridLayerLabel.setMinWidth(120);
        gridLayerLabel.setAlignment(Pos.CENTER);
        for (Button button : new Button[] {gridDown, gridUp}) {
            button.setStyle(BUTTON_STYLE);
            button.setFocusTraversable(false);
        }
        gridDown.setOnAction(event -> {
            view.shiftGridLayer(-1);
            refresh();
        });
        gridUp.setOnAction(event -> {
            view.shiftGridLayer(1);
            refresh();
        });
        gridLayerLabel.setMinWidth(200);
        HBox gridLayers = new HBox(10, gridDown, gridLayerLabel, gridUp);
        gridLayers.setAlignment(Pos.CENTER);
        gridSize.setStyle("-fx-font-size: 13px; -fx-text-fill: #c8d4ea;");
        HBox gridAim = new HBox(8);
        gridAim.setAlignment(Pos.CENTER);
        String[] axes = {"x", "y", "z"};
        TextField[] fields = {gridX, gridY, gridZ};
        for (int axis = 0; axis < 3; axis++) {
            TextField field = fields[axis];
            field.setPrefWidth(70);
            field.setOnAction(event -> {
                selectFromFields();
                view.tryGrid();
                refresh();
            });
            field.textProperty().addListener((obs, was, now) -> { if (field.isFocused()) selectFromFields(); });
            Label name = new Label(axes[axis]);
            gridAxisNames.add(name);
            gridAim.getChildren().addAll(name, field);
        }
        for (Button button : new Button[] {gridTry, gridZoomIn, gridZoomOut}) {
            button.setStyle(BUTTON_STYLE);
            button.setFocusTraversable(false);
        }
        gridTry.setOnAction(event -> {
            selectFromFields();
            view.tryGrid();
            refresh();
        });
        gridZoomIn.setOnAction(event -> {
            view.zoomGrid(1);
            refresh();
        });
        gridZoomOut.setOnAction(event -> {
            view.zoomGrid(-1);
            refresh();
        });
        gridAim.getChildren().addAll(gridTry, gridZoomOut, gridZoomIn);
        FlowPane gridRow = new FlowPane(12, 10);
        gridRow.setAlignment(Pos.CENTER);
        for (Game.GridUpgrade upgrade : Game.GridUpgrade.values()) {
            Button button = upgradeButton(230, () -> game.buyGrid(upgrade));
            gridButtons.put(upgrade, button);
            gridRow.getChildren().add(button);
        }
        gridPane = new VBox(8, gridShards, gridThreshold, gridSize, gridHint, gridLayers, gridAim, gridDetail, gridRow);
        gridPane.setAlignment(Pos.CENTER);
        gridPane.setPadding(new Insets(10, 0, 0, 0));
        mirrorThreshold.setStyle("-fx-font-size: 13px; -fx-text-fill: #9be7a8;");
        mirrorCounts.setStyle("-fx-font-size: 13px; -fx-text-fill: #c8d4ea;");
        Labels.centered(mirrorHint, "-fx-font-size: 13px; -fx-text-fill: #c8d4ea;");
        mirrorHint.setMaxWidth(820);
        mirrorHint.setAlignment(Pos.CENTER);
        // Deux lignes toujours : le plateau ne bouge pas quand la phase change le texte.
        mirrorHint.setMinHeight(38);
        mirrorHint.setPrefHeight(38);
        Labels.note(mirrorDetail, 820);
        FlowPane mirrorRow = new FlowPane(12, 10);
        mirrorRow.setAlignment(Pos.CENTER);
        for (Game.MirrorUpgrade upgrade : Game.MirrorUpgrade.values()) {
            Button button = upgradeButton(230, () -> game.buyMirror(upgrade));
            mirrorButtons.put(upgrade, button);
            mirrorRow.getChildren().add(button);
        }
        // Version 0.247 : la vitesse du motif, de la mémorisation entière (à gauche) au plus rapide (à droite).
        mirrorPace.setMin(0);
        mirrorPace.setMax(1);
        mirrorPace.setPrefWidth(260);
        mirrorPace.setFocusTraversable(false);
        mirrorPace.valueProperty().addListener((observable, before, pace) -> {
            game.setMirrorPace(pace.doubleValue());
            onMirrorPace.accept(game.mirrorPace());
            mirrorPaceText.setText(mirrorPaceText());
        });
        mirrorPaceText.setStyle("-fx-font-size: 13px; -fx-text-fill: #c8d4ea;");
        mirrorPaceText.setMinWidth(230);
        HBox mirrorPaceRow = new HBox(10, mirrorPaceText, mirrorPace);
        mirrorPaceRow.setAlignment(Pos.CENTER);
        mirrorPane = new VBox(10, mirrorShards, mirrorThreshold, mirrorBoard, mirrorCounts, mirrorHint, mirrorPaceRow, mirrorDetail, mirrorRow);
        mirrorPane.setAlignment(Pos.CENTER);
        mirrorPane.setPadding(new Insets(10, 0, 0, 0));
        mixThreshold.setStyle("-fx-font-size: 13px; -fx-text-fill: #9be7a8;");
        Labels.centered(mixInfo, "-fx-font-size: 13px;");
        mixInfo.setMaxWidth(860);
        Labels.centered(mixHint, "-fx-font-size: 13px; -fx-text-fill: #c8d4ea;");
        mixHint.setMaxWidth(860);
        mixHint.setAlignment(Pos.CENTER);
        Labels.note(mixDetail, 860);
        FlowPane mixRow = new FlowPane(10, 8);
        mixRow.setAlignment(Pos.CENTER);
        for (Game.MixUpgrade upgrade : Game.MixUpgrade.values()) {
            Button button = upgradeButton(210, () -> game.buyMix(upgrade));
            mixButtons.put(upgrade, button);
            mixRow.getChildren().add(button);
        }
        mixPane = new VBox(8, mixShards, mixThreshold, mixInfo, mixHint, mixDetail, mixRow);
        mixPane.setAlignment(Pos.CENTER);
        mixPane.setPadding(new Insets(10, 0, 0, 0));
        setBottom(bottom = new VBox(linePane, planePane, volumePane, durationPane, loopPane, branePane, clickerPane, gridPane, mirrorPane, mixPane));
        restyle();
        setVisible(false);
    }

    /** Le bas de la fenêtre : les commandes de chaque dimension, puis la bande du profil de test s'il y en a une. */
    VBox bottom;

    /** La bande du profil de test, tout en bas de la fenêtre : elle suit la dimension ouverte. */
    public void setTestBar(Node bar) { bottom.getChildren().add(bar); }

    /** Les réglages de la boucle : où se lisent les touches des cercles et le métronome, et ce qui joue son tic. */
    public void setLoopSettings(LoopSettings settings) {
        loopSettings = settings == null ? new LocalLoopSettings() : settings;
    }

    /** Ce qui suit un geste du joueur dans le mini-jeu, à la souris comme au clavier : son son. */
    IntConsumer onGesture = dimension -> { };

    /** Branche le son des gestes : la vue le reçoit pour la souris, la fenêtre le garde pour les touches. */
    public void setOnGesture(IntConsumer gesture) {
        onGesture = gesture == null ? dimension -> { } : gesture;
        view.setOnGesture(onGesture);
    }

    /** Un bouton d'amélioration : large de {@code width}, haut de deux lignes, qui achète puis rafraîchit. */
    Button upgradeButton(double width, Runnable buy) {
        Button button = new Button();
        button.setStyle(BUTTON_STYLE);
        button.setFocusTraversable(false);
        button.setWrapText(true);
        button.setPrefWidth(width);
        button.setMinHeight(64);
        button.setTextAlignment(TextAlignment.CENTER);
        button.setOnAction(event -> {
            buy.run();
            refresh();
        });
        return button;
    }

    /**
     * Vrai si l'automatisation joue la dimension montrée : son automatisme de jeu tourne (la visée, la plongée, l'appui) ;
     * pour la ligne, qui n'en a pas, un de ses achats automatiques.
     */
    /** La cadence du palier qui joue cette dimension, en mots : « toutes les 5 s », « sans attente ». */
    String cadence(int dimension) {
        DimensionAutomation tier = game.dimensionPlayTier(dimension);
        return tier == null ? "" : tier.cadence();
    }

    boolean automatic() {
        for (DimensionAutomation automation : DimensionAutomation.values()) {
            if (automation.dimension() != view.dimension().number() || !game.isDimensionAutomationRunning(automation)) continue;
            if (automation.kind() == DimensionAutomation.Kind.PLAY || automation.dimension() == 1) return true;
        }
        return false;
    }

    /** Recopie la dimension montrée (de la ligne au mélange) : ses éclats, et ce que coûtent ses améliorations. */
    void refresh() {
        // La palette des dimensions : lue à chaque image ; changée, les étiquettes colorées sont reprises.
        DimensionPalette.setSkin(game.chosenSkin(SkinVisual.DIMENSION_PALETTE));
        if (changed(this, "palette " + DimensionPalette.skin())) restyle();
        autoBadge.setVisible(automatic());
        boolean mix = view.dimension() == Dimension.MIX && game.isMixOpen();
        mixPane.setVisible(mix);
        mixPane.setManaged(mix);
        mixStageBox.setVisible(mix);
        mixStageBox.setManaged(mix);
        if (mix) refreshMix();
        boolean mirror = view.dimension() == Dimension.MIRROR && game.isMirrorOpen();
        mirrorPane.setVisible(mirror);
        mirrorPane.setManaged(mirror);
        if (mirror) refreshMirror();
        boolean grid = view.dimension() == Dimension.GRID && game.isGridOpen();
        gridPane.setVisible(grid);
        gridPane.setManaged(grid);
        if (grid) refreshGrid();
        boolean clicker = view.dimension() == Dimension.CLICKER && game.isClickerOpen();
        clickerPane.setVisible(clicker);
        clickerPane.setManaged(clicker);
        if (clicker) refreshClicker();
        boolean brane = view.dimension() == Dimension.BRANE && game.isBraneOpen();
        branePane.setVisible(brane);
        branePane.setManaged(brane);
        if (brane) refreshBrane();
        boolean loop = view.dimension() == Dimension.LOOP && game.isLoopOpen();
        loopPane.setVisible(loop);
        loopPane.setManaged(loop);
        view.setOnBeat(loop && loopSettings.metronome() ? loopSettings::beat : null);
        if (loop) refreshLoop();
        boolean duration = view.dimension() == Dimension.DURATION && game.isDurationOpen();
        durationPane.setVisible(duration);
        durationPane.setManaged(duration);
        if (duration) refreshDuration();
        boolean volume = view.dimension() == Dimension.VOLUME && game.isVolumeOpen();
        volumePane.setVisible(volume);
        volumePane.setManaged(volume);
        if (volume) refreshVolume();
        boolean plane = view.dimension() == Dimension.PLANE && game.isPlaneOpen();
        planePane.setVisible(plane);
        planePane.setManaged(plane);
        if (plane) refreshPlane();
        boolean line = view.dimension() == Dimension.LINE && game.isLineOpen();
        linePane.setVisible(line);
        linePane.setManaged(line);
        if (!line) return;
        shards.setText("Segments de la ligne : " + shardText(game.state().dimensionShards())
                + "   (+" + rate(game.lineShardsPerSecond()) + " par seconde)");
        threshold(threshold, game.isLineThresholdReached(), game.state().dimensionShardsEarned(), Game.LINE_THRESHOLD, "segments gagnés", 1);
        if (changed(lineDetail, Detail.shown() + "/" + game.lineTicks() + "/" + game.lineBalls() + "/" + game.lineCount() + "/"
                + game.lineSpeed() + "/" + game.lineShardsPerTouch())) {
            lineDetail.setText(Detail.only("Chaque boule va d'un bout à l'autre de sa ligne en " + seconds(game.lineHalfTrip())
                    + " s, puis revient ; à chaque aller, elle touche tous les traits sauf celui d'où elle part, et chaque trait "
                    + "touché donne " + shardText(game.lineShardsPerTouch()) + " segment. " + game.lineCount()
                    + (game.lineCount() > 1 ? " lignes" : " ligne") + " de " + game.lineTicks() + " traits, " + game.lineBalls()
                    + (game.lineBalls() > 1 ? " boules" : " boule") + " sur chacune. La ligne joue seule, fenêtre fermée et pendant "
                    + "l'absence, comme le reste du jeu. Les segments, le seuil et les améliorations de la ligne traversent les univers ; le seuil "
                    + "marque la dimension d'un rond vert, et les segments paient aussi le débit de l'arrosoir et le "
                    + "remplissage du chrono, dans les améliorations de la graine."));
            lineDetail.setVisible(!lineDetail.getText().isEmpty());
            lineDetail.setManaged(lineDetail.isVisible());
        }
        paintUpgrades(buttons, this::describe, game::lineLevel, game::lineMaxLevel, game::lineCost, game::canBuyLine,
                " segment", color(1));
    }

    /** Le seuil d'une dimension : atteint, elle porte son rond vert ; sinon, ce qu'on a gagné sur ce qu'il faut. */
    static void threshold(Label label, boolean reached, double earned, double limit, String gained, int number) {
        label.setText(reached
                ? "Seuil atteint : " + shardText(limit) + " " + gained + ", la dimension " + number + " porte son rond vert"
                : "Seuil : " + shardText(earned) + " / " + shardText(limit) + " " + gained);
    }

    /** Le nombre d'un champ de coordonnée, ou −1 s'il n'en est pas un. */
    static int parsed(TextField field) {
        try {
            return Integer.parseInt(field.getText().trim());
        } catch (NumberFormatException notANumber) {
            return -1;
        }
    }

    /** Ce que montre l'affichage du point à ce niveau. */
    static String revealText(int level) {
        return switch (level) {
            case 0 -> "La grille ne montre que le gain de chaque essai.";
            case 1 -> "La grille montre la distance de chaque essai au point visé, et colore les cases du froid au chaud.";
            case 2 -> "La grille montre la distance de chaque essai, et la couche du point visé par le dernier, tant qu'il lui reste un essai.";
            default -> "La grille montre la distance de chaque essai, et la couche et la colonne du point visé par le dernier, "
                    + "tant qu'il lui reste un essai.";
        };
    }

    /** Les dimensions mêlées, de la ligne à la dernière : « ligne + plan + volume ». */
    static final String[] MIX_NAMES = {"ligne", "plan", "volume", "durée", "boucle", "brane", "clicker", "grille", "miroir"};

    /** « Étape 4 sur 9 : ligne + plan + volume + durée ». */
    static String mixStageText(int stage) {
        return "Étape " + stage + " sur " + Game.MIX_STAGES + " : " + String.join(" + ", Arrays.asList(MIX_NAMES).subList(0, stage));
    }

    /** Ce qu'on fait à cette étape du mélange : ce qui vient d'y être mêlé d'abord. */
    static String mixHintFor(int stage) {
        return switch (stage) {
            case 1 -> "La boule va et vient sur la ligne : chaque graduation qu'elle passe donne des fragments. Achetez des "
                    + "graduations et réduisez le malus.";
            case 2 -> "Chaque graduation pose un éclat : menez le point dessus pour le ramasser (clic ou souris tenue sur "
                    + "l'arène, ou Z Q S D et les flèches).";
            case 3 -> "Les éclats sont à des profondeurs (les violets au fond) : la molette, ou A et E, font plonger le point "
                    + "ou le remontent ; il ramasse ceux qu'entoure un anneau.";
            case 4 -> "Tenez le bouton droit (ou la barre d'espace) : ce que vous ramassez va dans la charge, qui grandit "
                    + "jusqu'à ×2 en 8 s ; relâchez pour la verser.";
            case 5 -> "Relâchez la charge quand le point du cercle, à gauche, est dans sa zone : au centre, ×3.";
            case 6 -> "Chaque relâchement lance une onde depuis le point : elle ramasse les éclats qu'elle touche, davantage "
                    + "après un rebond sur les bords.";
            case 7 -> "Cliquez l'arène, et vite : la cadence multiplie tout. Les éclats ramassés dans une zone ×2 valent le "
                    + "double, par zone qui les couvre.";
            case 8 -> "Un trésor est caché dans une case de la grille : le halo du point chauffe quand il s'en approche ; "
                    + "entrez dans sa case pour le trouver.";
            default -> "Les éclats viennent par paires, de part et d'autre du miroir : ramassez un éclat puis son reflet, "
                    + "juste après, pour le multiplier.";
        };
    }

    /** Le nom d'une amélioration du mélange, sans ses chiffres. */
    static String mixName(Game.MixUpgrade upgrade) {
        return switch (upgrade) {
            case GAIN -> "Gain";
            case MALUS -> "Réduction du malus";
            case TICKS -> "Graduations";
            case SPEED -> "Vitesse du point";
            case DEPTH -> "Profondeur";
            case CHARGE -> "Charge";
            case RHYTHM -> "Zone du rythme";
            case REACH -> "Portée de l'onde";
            case ZONES -> "Zones ×2";
            case TREASURE -> "Trésor";
            case PAIRS -> "Paires";
        };
    }

    /** Une part en pour cent : « 1 % », « 2,56 % » (deux décimales au plus). */
    static String percentOf(double share) { return ElementText.number(share * 100) + " %"; }

    /** Une longueur sur la membrane (de côté 2) : deux décimales. */
    static String reach(double value) { return String.format(Locale.ROOT, "%.2f", value); }

    static String seconds(double value) { return String.format(Locale.ROOT, "%.1f", value); }

    /** Un pas d'amélioration en pourcentage : 0,25 donne « 25 % ». */
    static String percent(double step) { return Math.round(step * 100) + " %"; }

    static String shardText(double value) { return Format.darkMatter(value); }

    /** Un débit d'éclats : deux décimales sous 1, une au-delà. */
    static String rate(double perSecond) {
        if (perSecond < 1) return String.format(Locale.ROOT, "%.2f", Math.floor(perSecond * 100 + 1e-9) / 100);
        return Format.darkMatter(BigNum.of(perSecond));
    }
}
