package idle.ui.universe;

import idle.core.Game;
import idle.core.universe.DimensionAutomation;
import idle.ui.common.CanvasPane;
import idle.ui.common.Format;
import idle.core.math.BigNum;
import idle.ui.common.Labels;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * La fenêtre d'une dimension (version 0.081) : elle couvre tout le jeu, avec son visuel, son nom et
 * une phrase, et un bouton « Retour » pour revenir au jeu. Le jeu continue de tourner derrière.
 *
 * <p>La première, la ligne, a sa logique depuis la version 0.101 : ses éclats de dimension, son seuil et ses
 * cinq améliorations (version 0.102) sous le visuel. Le plan a la sienne depuis la version 0.104, et le volume depuis
 * la version 0.128 : on plonge dans le cube, et ses améliorations s'achètent ici, une fois ressorti. La durée a la
 * sienne depuis la version 0.134 : on tient le bouton du milieu, les spirales avancent, on relâche. La boucle a la
 * sienne depuis la version 0.153 : on clique chaque cercle (ou sa touche) quand son point passe dans une zone ; ses
 * six améliorations, son métronome et le rappel des touches sont sous la vue. La brane a la sienne depuis la version
 * 0.154 : on appuie la membrane, on glisse pour la tendre, on relâche, et l'onde qui part rebondit sur ses bords ; ses
 * cinq améliorations sont sous la vue. Les quatre autres restent des visuels.
 *
 * <p>Depuis la version 0.150, le jeu peut jouer seul le plan, le volume et la durée (l'automatisation des dimensions) :
 * une mention « automatique », à droite du titre, le dit tant que l'automatisme de jeu de la dimension tourne (pour la
 * ligne, qui avance seule, tant qu'un de ses achats automatiques tourne), et les explications disent ce qu'il fait.
 * Fermer la fenêtre n'arrête alors ni la plongée ni l'appui : ils continuent derrière. Le rythme automatique de la
 * boucle (version 0.153) et la tirée automatique de la brane (version 0.154) jouent de même, fenêtre ouverte ou non.
 */
public final class DimensionWindow extends BorderPane {

    private static final String BUTTON_STYLE = "-fx-font-size: 13px; -fx-padding: 8 16; -fx-cursor: hand; -fx-background-radius: 6;"
            + " -fx-border-radius: 6; -fx-text-fill: " + UniversePage.COLOR + "; -fx-background-color: #141a2c; -fx-border-color: #3b4670;";

    /** La couleur de chaque dimension, celle de ses éclats : ses améliorations achetables la prennent (version 0.140). */
    private static final String PLANE_COLOR = "#ffd27f", VOLUME_COLOR = "#c9a6ff", DURATION_COLOR = "#9be7a8", LOOP_COLOR = "#ff9eb5",
            BRANE_COLOR = "#7fe0d4";
    private static final String UPGRADE_STYLE = "-fx-font-size: 13px; -fx-background-radius: 6; -fx-border-radius: 6; -fx-opacity: 1;";

    /** La couleur de la dimension {@code number} (1 à 6), celle de ses éclats ; celle de l'univers pour la ligne et au-delà. */
    public static String color(int number) {
        return switch (number) {
            case 2 -> PLANE_COLOR;
            case 3 -> VOLUME_COLOR;
            case 4 -> DURATION_COLOR;
            case 5 -> LOOP_COLOR;
            case 6 -> BRANE_COLOR;
            default -> UniversePage.COLOR;
        };
    }

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

    /** Les touches des sept cercles de la boucle tant que le joueur n'en a pas choisi d'autres (version 0.153). */
    public static final java.util.List<javafx.scene.input.KeyCode> DEFAULT_LOOP_KEYS = java.util.List.of(
            javafx.scene.input.KeyCode.S, javafx.scene.input.KeyCode.D, javafx.scene.input.KeyCode.F,
            javafx.scene.input.KeyCode.SPACE, javafx.scene.input.KeyCode.J, javafx.scene.input.KeyCode.K,
            javafx.scene.input.KeyCode.L);

    /**
     * Ce que la boucle demande au jeu (version 0.153) : la touche de chaque cercle et le choix du métronome, rangés dans
     * les réglages, et le tic qu'il joue, au volume et selon le réglage des sons.
     */
    public interface LoopSettings {
        /** La touche du cercle {@code ring} (0 à 6), ou {@code null} s'il n'en a pas. */
        javafx.scene.input.KeyCode loopKey(int ring);

        boolean metronome();

        void setMetronome(boolean on);

        /** Un battement du métronome : le tic, si les sons sont actifs. */
        void beat();
    }

    /** Les réglages de la boucle d'une fenêtre seule (pour les vérifications) : les touches d'origine, un métronome muet. */
    private static final class LocalLoopSettings implements LoopSettings {
        private boolean metronome;

        @Override
        public javafx.scene.input.KeyCode loopKey(int ring) {
            return ring >= 0 && ring < DEFAULT_LOOP_KEYS.size() ? DEFAULT_LOOP_KEYS.get(ring) : null;
        }

        @Override
        public boolean metronome() {
            return metronome;
        }

        @Override
        public void setMetronome(boolean on) {
            metronome = on;
        }

        @Override
        public void beat() {
        }
    }

    private final Game game;
    private final DimensionView view;
    /** La ligne : ses éclats, et ses cinq améliorations. */
    private final Label shards = new Label();
    private final Label threshold = new Label();
    private final java.util.Map<Game.LineUpgrade, Button> buttons = new java.util.EnumMap<>(Game.LineUpgrade.class);
    private final VBox linePane;
    /** Le plan (version 0.104) : ses éclats, la manière de jouer, et ses cinq améliorations. */
    private final Label planeShards = new Label();
    private final Label planeThreshold = new Label();
    private final Label planeHint = new Label();
    private final java.util.Map<Game.PlaneUpgrade, Button> planeButtons = new java.util.EnumMap<>(Game.PlaneUpgrade.class);
    private final VBox planePane;
    /** Le volume (version 0.128) : ses éclats, son seuil, ce qu'on y fait, et ses cinq améliorations, dehors. */
    private final Label volumeShards = new Label();
    private final Label volumeThreshold = new Label();
    private final Label volumeHint = new Label();
    private final Label volumeDetail = new Label();
    private final java.util.Map<Game.VolumeUpgrade, Button> volumeButtons = new java.util.EnumMap<>(Game.VolumeUpgrade.class);
    private final javafx.scene.layout.FlowPane volumeRow = new javafx.scene.layout.FlowPane(12, 10);
    private final VBox volumePane;
    /** La durée (version 0.134) : ses éclats, son seuil, ce qu'on y fait, et ses quatre améliorations. */
    private final Label durationShards = new Label();
    private final Label durationThreshold = new Label();
    private final Label durationHint = new Label();
    private final Label durationDetail = new Label();
    private final java.util.Map<Game.DurationUpgrade, Button> durationButtons = new java.util.EnumMap<>(Game.DurationUpgrade.class);
    private final VBox durationPane;
    /** La boucle (version 0.153) : ses nœuds, son seuil, ce qu'on y fait, son métronome, et ses six améliorations. */
    private final Label loopShards = new Label();
    private final Label loopThreshold = new Label();
    private final Label loopHint = new Label();
    private final Label loopDetail = new Label();
    private final Button metronomeButton = new Button();
    private final java.util.Map<Game.LoopUpgrade, Button> loopButtons = new java.util.EnumMap<>(Game.LoopUpgrade.class);
    private final VBox loopPane;
    private LoopSettings loopSettings = new LocalLoopSettings();
    /** La brane (version 0.154) : ses ondes, son seuil, ce qu'on y fait, et ses cinq améliorations. */
    private final Label braneShards = new Label();
    private final Label braneThreshold = new Label();
    private final Label braneHint = new Label();
    private final Label braneDetail = new Label();
    private final java.util.Map<Game.BraneUpgrade, Button> braneButtons = new java.util.EnumMap<>(Game.BraneUpgrade.class);
    private final VBox branePane;
    /** Les touches de cercles tenues : une touche tenue se répète, mais ne rejoue pas son cercle. */
    private final java.util.Set<javafx.scene.input.KeyCode> loopKeysHeld = java.util.EnumSet.noneOf(javafx.scene.input.KeyCode.class);
    /** La barre d'espace tient le bouton de la durée, comme la souris. */
    private boolean spaceHeld;
    /** La caméra libre (version 0.128) : le bouton, et où se range le choix (les réglages du jeu, ou la fenêtre seule). */
    private final Button freeCameraButton = new Button();
    private final java.util.function.BooleanSupplier freeCamera;
    private final java.util.function.Consumer<Boolean> setFreeCamera;
    /** Les touches de direction tenues : Z, Q, S, D et les flèches. */
    private final java.util.Set<javafx.scene.input.KeyCode> held = java.util.EnumSet.noneOf(javafx.scene.input.KeyCode.class);
    private final Label title = new Label();
    /** La mention « automatique » (version 0.150), à droite du titre : l'automatisation joue la dimension. */
    private final Label autoBadge = new Label("automatique");
    private final Label line = new Label();
    private final Button back = new Button("← Retour");
    /**
     * Ce que chaque rangée de boutons et chaque explication a montré en dernier (niveaux, achetabilité, détails demandés) :
     * {@link #refresh()} tourne à chaque image, les longs textes ne se refont que si ça change.
     */
    private final java.util.Map<Object, String> painted = new java.util.HashMap<>();

    /** Une fenêtre qui garde seule le choix de la caméra libre : pour les vérifications. */
    public DimensionWindow(Game game) {
        this(game, new boolean[1]);
    }

    private DimensionWindow(Game game, boolean[] free) {
        this(game, () -> free[0], value -> free[0] = value);
    }

    /** La fenêtre des dimensions ; le choix de la caméra libre du volume se lit et s'écrit par ces deux fonctions. */
    public DimensionWindow(Game game, java.util.function.BooleanSupplier freeCamera, java.util.function.Consumer<Boolean> setFreeCamera) {
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
        VBox head = new VBox(6, top, line);
        head.setAlignment(Pos.CENTER);
        line.setMaxWidth(Double.MAX_VALUE);
        line.setAlignment(Pos.CENTER);
        head.setPadding(new Insets(0, 0, 10, 0));
        setTop(head);
        CanvasPane holder = CanvasPane.filling(view);
        setCenter(holder);
        shards.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + UniversePage.COLOR + ";");
        threshold.setStyle("-fx-font-size: 13px; -fx-text-fill: #9be7a8;");
        javafx.scene.layout.FlowPane row = new javafx.scene.layout.FlowPane(12, 10);
        row.setAlignment(Pos.CENTER);
        for (Game.LineUpgrade upgrade : Game.LineUpgrade.values()) {
            Button button = upgradeButton(250, () -> game.buyLine(upgrade));
            buttons.put(upgrade, button);
            row.getChildren().add(button);
        }
        linePane = new VBox(10, shards, threshold, row);
        linePane.setAlignment(Pos.CENTER);
        linePane.setPadding(new Insets(10, 0, 0, 0));
        planeShards.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #ffd27f;");
        planeHint.setStyle("-fx-font-size: 13px; -fx-text-fill: #c8d4ea;");
        javafx.scene.layout.FlowPane planeRow = new javafx.scene.layout.FlowPane(12, 10);
        planeRow.setAlignment(Pos.CENTER);
        for (Game.PlaneUpgrade upgrade : Game.PlaneUpgrade.values()) {
            Button button = upgradeButton(250, () -> game.buyPlane(upgrade));
            planeButtons.put(upgrade, button);
            planeRow.getChildren().add(button);
        }
        planeThreshold.setStyle("-fx-font-size: 13px; -fx-text-fill: #9be7a8;");
        planePane = new VBox(10, planeShards, planeThreshold, planeHint, planeRow);
        planePane.setAlignment(Pos.CENTER);
        planePane.setPadding(new Insets(10, 0, 0, 0));
        volumeShards.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #c9a6ff;");
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
        durationShards.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #9be7a8;");
        durationThreshold.setStyle("-fx-font-size: 13px; -fx-text-fill: #9be7a8;");
        Labels.centered(durationHint, "-fx-font-size: 13px; -fx-text-fill: #c8d4ea;");
        durationHint.setMaxWidth(820);
        durationHint.setAlignment(Pos.CENTER);
        // Deux lignes toujours : le rond ne bouge pas quand le texte change, bouton tenu ou relâché.
        durationHint.setMinHeight(38);
        durationHint.setPrefHeight(38);
        Labels.note(durationDetail, 820);
        javafx.scene.layout.FlowPane durationRow = new javafx.scene.layout.FlowPane(12, 10);
        durationRow.setAlignment(Pos.CENTER);
        for (Game.DurationUpgrade upgrade : Game.DurationUpgrade.values()) {
            Button button = upgradeButton(230, () -> game.buyDuration(upgrade));
            durationButtons.put(upgrade, button);
            durationRow.getChildren().add(button);
        }
        durationPane = new VBox(10, durationShards, durationThreshold, durationHint, durationDetail, durationRow);
        durationPane.setAlignment(Pos.CENTER);
        durationPane.setPadding(new Insets(10, 0, 0, 0));
        loopShards.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + LOOP_COLOR + ";");
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
        javafx.scene.layout.FlowPane loopRow = new javafx.scene.layout.FlowPane(12, 10);
        loopRow.setAlignment(Pos.CENTER);
        for (Game.LoopUpgrade upgrade : Game.LoopUpgrade.values()) {
            Button button = upgradeButton(250, () -> game.buyLoop(upgrade));
            loopButtons.put(upgrade, button);
            loopRow.getChildren().add(button);
        }
        loopPane = new VBox(10, loopShards, loopThreshold, loopHint, metronomeButton, loopDetail, loopRow);
        loopPane.setAlignment(Pos.CENTER);
        loopPane.setPadding(new Insets(10, 0, 0, 0));
        braneShards.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + BRANE_COLOR + ";");
        braneThreshold.setStyle("-fx-font-size: 13px; -fx-text-fill: #9be7a8;");
        Labels.centered(braneHint, "-fx-font-size: 13px; -fx-text-fill: #c8d4ea;");
        braneHint.setMaxWidth(820);
        braneHint.setAlignment(Pos.CENTER);
        // Deux lignes toujours : la membrane ne bouge pas quand le texte change, tenue ou relâchée.
        braneHint.setMinHeight(38);
        braneHint.setPrefHeight(38);
        Labels.note(braneDetail, 820);
        javafx.scene.layout.FlowPane braneRow = new javafx.scene.layout.FlowPane(12, 10);
        braneRow.setAlignment(Pos.CENTER);
        for (Game.BraneUpgrade upgrade : Game.BraneUpgrade.values()) {
            Button button = upgradeButton(250, () -> game.buyBrane(upgrade));
            braneButtons.put(upgrade, button);
            braneRow.getChildren().add(button);
        }
        branePane = new VBox(10, braneShards, braneThreshold, braneHint, braneDetail, braneRow);
        branePane.setAlignment(Pos.CENTER);
        branePane.setPadding(new Insets(10, 0, 0, 0));
        setBottom(new VBox(linePane, planePane, volumePane, durationPane, loopPane, branePane));
        setVisible(false);
    }

    /** Les réglages de la boucle : où se lisent les touches des cercles et le métronome, et ce qui joue son tic. */
    public void setLoopSettings(LoopSettings settings) {
        loopSettings = settings == null ? new LocalLoopSettings() : settings;
    }

    /** Un bouton d'amélioration : large de {@code width}, haut de deux lignes, qui achète puis rafraîchit. */
    private Button upgradeButton(double width, Runnable buy) {
        Button button = new Button();
        button.setStyle(BUTTON_STYLE);
        button.setFocusTraversable(false);
        button.setWrapText(true);
        button.setPrefWidth(width);
        button.setMinHeight(64);
        button.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
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
    private boolean automatic() {
        for (DimensionAutomation automation : DimensionAutomation.values()) {
            if (automation.dimension() != view.dimension().number() || !game.isDimensionAutomationRunning(automation)) continue;
            if (automation.kind() == DimensionAutomation.Kind.PLAY || automation.dimension() == 1) return true;
        }
        return false;
    }

    /** Recopie la dimension montrée (de la ligne à la brane) : ses éclats, et ce que coûtent ses améliorations. */
    private void refresh() {
        autoBadge.setVisible(automatic());
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
        paintUpgrades(buttons, this::describe, game::lineLevel, game::lineMaxLevel, game::lineCost, game::canBuyLine,
                " segment", UniversePage.COLOR);
    }

    /** Vrai si {@code key} diffère de ce que {@code node} a montré en dernier ; la retient pour la prochaine image. */
    private boolean changed(Object node, String key) {
        return !key.equals(painted.put(node, key));
    }

    /** Le seuil d'une dimension : atteint, elle porte son rond vert ; sinon, ce qu'on a gagné sur ce qu'il faut. */
    private static void threshold(Label label, boolean reached, double earned, double limit, String gained, int number) {
        label.setText(reached
                ? "Seuil atteint : " + shardText(limit) + " " + gained + ", la dimension " + number + " porte son rond vert"
                : "Seuil : " + shardText(earned) + " / " + shardText(limit) + " " + gained);
    }

    /**
     * Les boutons d'une dimension : la description de chacun, son prix (au singulier sous 2) ou « Au maximum », et sa
     * couleur selon qu'il s'achète. {@code unit} est le singulier de l'éclat, précédé d'une espace ; le pluriel ajoute un s.
     * Tout ce qui s'affiche ne dépend que des niveaux et de ce qui est achetable : la rangée ne se refait que s'ils changent.
     */
    private <U extends Enum<U>> void paintUpgrades(java.util.Map<U, Button> buttons, java.util.function.Function<U, String> describe,
            java.util.function.ToIntFunction<U> level, java.util.function.ToIntFunction<U> max, java.util.function.ToDoubleFunction<U> cost,
            java.util.function.Predicate<U> canBuy, String unit, String accent) {
        StringBuilder key = new StringBuilder();
        for (U upgrade : buttons.keySet()) {
            key.append(level.applyAsInt(upgrade)).append(canBuy.test(upgrade) ? '+' : '-');
        }
        if (!changed(buttons, key.toString())) return;
        for (U upgrade : buttons.keySet()) {
            Button button = buttons.get(upgrade);
            boolean full = level.applyAsInt(upgrade) >= max.applyAsInt(upgrade);
            double price = cost.applyAsDouble(upgrade);
            button.setText(describe.apply(upgrade) + "\n" + (full ? "Au maximum" : "Prix : " + shardText(price) + unit + (price < 2 ? "" : "s")));
            button.setDisable(!canBuy.test(upgrade));
            paintUpgrade(button, canBuy.test(upgrade), full, accent);
        }
    }

    private void refreshPlane() {
        planeShards.setText("Facettes du plan : " + shardText(game.state().planeShards()));
        // Le seuil du plan (version 0.120), comme celui de la ligne : son rond vert (version 0.139 : il n'ouvre plus de palier de la graine).
        threshold(planeThreshold, game.isPlaneThresholdReached(), game.state().planeShardsEarned(), Game.PLANE_THRESHOLD, "facettes gagnées", 2);
        planeHint.setText("Z Q S D (ou les flèches) déplacent le point. Passez sur un éclat pour le ramasser."
                + (game.isDimensionAutomationRunning(DimensionAutomation.PLANE_AIM)
                        ? " Visée automatique : touches lâchées, le point file seul vers l'éclat le plus proche." : ""));
        paintUpgrades(planeButtons, this::describe, game::planeLevel, Game.PlaneUpgrade::max, game::planeCost, game::canBuyPlane, " facette", PLANE_COLOR);
    }

    /**
     * Le volume : ses éclats et son seuil, toujours ; ses améliorations seulement dehors, puisqu'elles ne
     * s'achètent pas pendant une plongée ; dedans, ce qu'on y fait et le temps qu'il reste.
     */
    private void refreshVolume() {
        volumeShards.setText("Sphères du volume : " + shardText(game.state().volumeShards()));
        threshold(volumeThreshold, game.isVolumeThresholdReached(), game.state().volumeShardsEarned(), Game.VOLUME_THRESHOLD, "sphères gagnées", 3);
        boolean outside = view.dive() == DimensionView.Dive.OUTSIDE;
        boolean diving = game.isDimensionAutomationRunning(DimensionAutomation.VOLUME_DIVE);
        volumeHint.setText((diving ? "Plongée automatique : elle replonge seule et cueille une boule toutes les "
                + seconds(Game.VOLUME_AUTO_SHOT_SECONDS) + " s ; vous pouvez cueillir avec elle. " : "") + switch (view.dive()) {
            case OUTSIDE -> diving ? "Les améliorations s'achètent ici, entre deux plongées."
                    : "Cliquez le cube pour y plonger, " + seconds(game.volumeDiveSeconds()) + " s par plongée. "
                    + "Les améliorations s'achètent ici, hors du cube.";
            case ENTERING -> "Vous plongez dans le cube…";
            case INSIDE -> (!view.freeCamera()
                    ? "Clic gauche tenu : regarder autour de soi. Clic droit : cueillir la boule visée. "
                    : view.mouseHeld()
                    ? "Caméra libre : bougez la souris pour regarder autour de vous. Un clic cueille la boule sous la croix. "
                            + "Échap rend la souris. "
                    : "Souris rendue : cliquez dans la vue pour reprendre la caméra libre. ")
                    + "Le cube vous rejette dans " + seconds(game.volumeTimeLeft()) + " s.";
            case LEAVING -> "Le cube vous rejette : les améliorations reviennent.";
        });
        if (changed(volumeDetail, idle.ui.common.Detail.shown() + "/" + game.volumeLevel(Game.VolumeUpgrade.SPAWN) + "/" + game.volumeLevel(Game.VolumeUpgrade.COUNT))) {
            volumeDetail.setText(idle.ui.common.Detail.only("Une plongée commence quand la vue arrive au centre du cube. Une "
                + "boule apparaît aussitôt, puis une toutes les " + seconds(game.volumeSpawnSeconds()) + " s, "
                + game.volumeMaxBalls() + " à la fois au plus, entre " + shardText(Game.VOLUME_SPAWN_NEAR) + " et "
                + shardText(Game.VOLUME_SPAWN_FAR) + " du centre. Le clic droit cueille la plus proche que traverse le "
                + "regard ; une boule plus grosse se vise mieux. À la fin du minuteur, ou si le jeu passe en absence, la "
                + "plongée s'arrête et les boules restantes s'en vont. Les sphères, le seuil et les améliorations du volume "
                + "traversent les univers ; le seuil marque la dimension d'un rond vert, et les sphères paient aussi des améliorations de la graine. En caméra libre, la souris disparaît "
                + "dans le cube et y reste retenue : chaque mouvement tourne le regard et elle revient seule au centre. "
                + "Échap la rend sans finir la plongée, un clic dans la vue la reprend ; quand le jeu n'a pas la main "
                + "(Alt+Tab), elle n'est jamais retenue. Si le système refuse de la déplacer, pousser un bord fait tourner "
                + "le regard sans fin."));
            volumeDetail.setVisible(!volumeDetail.getText().isEmpty());
            volumeDetail.setManaged(volumeDetail.isVisible());
        }
        freeCameraButton.setText(view.freeCamera() ? "Caméra libre : activée (le regard suit la souris)"
                : "Caméra libre : coupée (clic gauche tenu pour regarder)");
        // Dedans, la rangée se cache mais garde sa place : le cube ne se déplace pas pendant la plongée.
        volumeRow.setVisible(outside);
        paintUpgrades(volumeButtons, this::describe, game::volumeLevel, Game.VolumeUpgrade::max, game::volumeCost,
                u -> outside && game.canBuyVolume(u), " sphère", VOLUME_COLOR);
    }

    /** La durée : ses éclats, son seuil, ce qu'on y fait ; ses améliorations, bouton relâché. */
    private void refreshDuration() {
        durationShards.setText("Instants de la durée : " + shardText(game.state().durationShards()));
        threshold(durationThreshold, game.isDurationThresholdReached(), game.state().durationShardsEarned(), Game.DURATION_THRESHOLD, "instants gagnés", 4);
        boolean held = game.isDurationHeld();
        durationHint.setText(game.isDimensionAutomationRunning(DimensionAutomation.DURATION_HOLD)
                ? "Appui automatique : tenu " + seconds(game.durationHeldSeconds()) + " s sur " + seconds(game.durationMaxSeconds())
                        + " s, il relâche seul au centre (+" + shardText(game.durationRewardFor(game.durationMaxSeconds()))
                        + ") et recommence. Les améliorations s'achètent quand même, et valent dès le cycle en cours."
                : !held
                ? "Tenez le bouton du milieu (clic tenu, ou la barre d'espace) : les spirales avancent vers le centre, qu'elles "
                        + "atteignent en " + seconds(game.durationMaxSeconds()) + " s. Relâchez pour gagner leurs instants : plus on tient, plus chaque tour rapporte."
                        + (game.durationReleases() > 0 ? " Dernier relâchement : +" + shardText(game.durationLastReward()) + "." : "")
                : game.durationProgress() >= 1
                ? "Les spirales sont au centre : tenir plus ne rapporte plus rien. Relâchez pour gagner +" + shardText(game.durationPending()) + "."
                : "Tenu " + seconds(game.durationHeldSeconds()) + " s sur " + seconds(game.durationMaxSeconds())
                        + " : relâcher maintenant donnerait +" + shardText(game.durationPending()) + ".");
        StringBuilder levels = new StringBuilder().append(idle.ui.common.Detail.shown());
        for (Game.DurationUpgrade upgrade : Game.DurationUpgrade.values()) levels.append('/').append(game.durationLevel(upgrade));
        if (changed(durationDetail, levels.toString())) {
            durationDetail.setText(idle.ui.common.Detail.only("Chaque spirale fait " + shardText(game.durationTurnsPerSecond())
                + " tour par seconde tenue, jusqu'au centre, " + shardText(game.durationTurnsMax()) + " tours en "
                + seconds(game.durationMaxSeconds()) + " s. Au relâchement, chaque tour vaut un de plus que le précédent : n tours "
                + "donnent (n² + n) ÷ 2 fois la valeur (" + shardText(game.durationValue()) + "), pour chacune des "
                + game.durationSpirals() + " spirale" + (game.durationSpirals() > 1 ? "s" : "") + ". Tenir au-delà du centre ne "
                + "rapporte rien de plus et ne fait rien perdre. Les améliorations s'achètent bouton relâché. Une absence relâche "
                + "le bouton, qui donne ce qu'il avait. Les instants, le seuil et les améliorations de la durée traversent les "
                + "univers ; le seuil marque la dimension d'un rond vert, et les instants paient aussi des améliorations de la graine."));
            durationDetail.setVisible(!durationDetail.getText().isEmpty());
            durationDetail.setManaged(durationDetail.isVisible());
        }
        paintUpgrades(durationButtons, this::describe, game::durationLevel, Game.DurationUpgrade::max, game::durationCost,
                u -> !held && game.canBuyDuration(u), " instant", DURATION_COLOR);
    }

    /**
     * La boucle : ses nœuds, son seuil, comment on y joue (les touches des cercles en jeu) ou ce que fait le rythme
     * automatique, le métronome, le détail des parts d'une zone, et ses améliorations, qui s'achètent toujours.
     */
    private void refreshLoop() {
        loopShards.setText("Nœuds de la boucle : " + shardText(game.state().loopShards()));
        threshold(loopThreshold, game.isLoopThresholdReached(), game.state().loopShardsEarned(), Game.LOOP_THRESHOLD, "nœuds gagnés", 5);
        Game.LoopHit last = game.lastLoopHit();
        String lastText = game.loopHits() == 0 && last == Game.LoopHit.MISS ? "" : " Dernier clic : " + DimensionView.hitWord(last).toLowerCase(java.util.Locale.ROOT) + ".";
        loopHint.setText(game.isDimensionAutomationRunning(DimensionAutomation.LOOP_PLAY)
                ? "Rythme automatique : il clique « bien » à chaque passage d'un point dans une zone, sur chaque cercle. "
                        + "Vos clics comptent en plus ; les améliorations s'achètent quand même."
                : "Cliquez un cercle, ou sa touche (" + loopKeysText() + "), quand son point passe dans une zone : au centre "
                        + "« excellent », puis « bien », au bord « mauvais ». Après chaque clic, le cercle attend "
                        + seconds(Game.LOOP_COOLDOWN) + " s." + lastText);
        metronomeButton.setText(loopSettings.metronome() ? "Métronome : activé (un tic à chaque centre de zone du premier cercle)"
                : "Métronome : coupé");
        StringBuilder levels = new StringBuilder().append(idle.ui.common.Detail.shown());
        for (Game.LoopUpgrade upgrade : Game.LoopUpgrade.values()) levels.append('/').append(game.loopLevel(upgrade));
        if (changed(loopDetail, levels.toString())) {
            loopDetail.setText(idle.ui.common.Detail.only("Chaque point fait un tour en " + seconds(1 / game.loopTurnsPerSecond())
                + " s. Une zone couvre " + Math.round(game.loopZoneHalfWidth() * 200) + " % de son cercle : au centre, "
                + Math.round(Game.LOOP_EXCELLENT * 100) + " % de sa largeur, « excellent » donne "
                + shardText(game.loopReward(Game.LoopHit.EXCELLENT)) + " nœuds ; jusqu'à " + Math.round(Game.LOOP_GOOD * 100)
                + " %, « bien » en donne " + shardText(game.loopReward(Game.LoopHit.GOOD)) + " ; au bord, « mauvais », "
                + shardText(game.loopReward(Game.LoopHit.BAD)) + ". Un clic à côté ne donne rien. La zone touchée se pose "
                + "ailleurs sur son cercle. Après chaque clic, juste ou non, le cercle attend " + seconds(Game.LOOP_COOLDOWN)
                + " s : cliquer sans arrêt ne rapporte rien. Chaque cercle a sa touche, qui se change dans les réglages "
                + "(Clavier). Les nœuds, le seuil et les améliorations de la boucle traversent les univers ; le seuil marque "
                + "la dimension d'un rond vert, et les nœuds paient aussi le rond de croissance de la graine."));
            loopDetail.setVisible(!loopDetail.getText().isEmpty());
            loopDetail.setManaged(loopDetail.isVisible());
        }
        paintUpgrades(loopButtons, this::describe, game::loopLevel, Game.LoopUpgrade::max, game::loopCost, game::canBuyLoop,
                " nœud", LOOP_COLOR);
    }

    /**
     * La brane : ses ondes, son seuil, comment on la tire (la force et la portée pendant qu'on la tient) ou ce que fait la
     * tirée automatique, le gain du dernier passage, le détail des passages et des rebonds, et ses améliorations, qui
     * s'achètent toujours.
     */
    private void refreshBrane() {
        braneShards.setText("Ondes de la brane : " + shardText(game.state().braneShards()));
        threshold(braneThreshold, game.isDimensionThresholdReached(6), game.state().braneShardsEarned(), Game.BRANE_THRESHOLD, "ondes gagnées", 6);
        String last = game.braneTouches() == 0 ? "" : " Dernier passage : +" + shardText(game.braneLastGain()) + ".";
        braneHint.setText(view.braneHeld()
                ? "Tirée à " + Math.round(view.branePull() * 100) + " % : l'onde portera à " + reach(game.braneReach(view.branePull()))
                        + " (" + reach(game.braneMaxReach()) + " à fond). Relâchez pour la lancer."
                : game.isDimensionAutomationRunning(DimensionAutomation.BRANE_PULL)
                ? "Tirée automatique : toutes les " + seconds(Game.BRANE_AUTO_SECONDS) + " s, elle tire la membrane à fond au "
                        + "milieu des points. Vos tirées comptent en plus ; les améliorations s'achètent quand même." + last
                : !game.canPullBrane()
                ? "Trois ondes courent déjà : attendez qu'une s'éteigne pour tirer de nouveau." + last
                : "Appuyez sur la membrane, glissez pour la tendre (plus loin, plus fort), relâchez : l'onde part du point "
                        + "appuyé et rebondit sur les bords. Chaque point qu'elle touche donne des ondes." + last);
        StringBuilder levels = new StringBuilder().append(idle.ui.common.Detail.shown());
        for (Game.BraneUpgrade upgrade : Game.BraneUpgrade.values()) levels.append('/').append(game.braneLevel(upgrade));
        if (changed(braneDetail, levels.toString())) {
            braneDetail.setText(idle.ui.common.Detail.only("La membrane fait 2 de côté. Une onde tirée à fond porte à "
                + reach(game.braneMaxReach()) + ", à peine tirée à " + Math.round(Game.BRANE_MIN_PULL * 100) + " % de cela ; un glissé de "
                + reach(DimensionView.BRANE_FULL_PULL) + " la tire à fond. Son front avance de " + reach(Game.BRANE_WAVE_SPEED)
                + " par seconde et rebondit sur les bords : chaque passage sur un point compte, en direct puis après chaque "
                + "rebond qui le retrouve. Un passage donne " + shardText(game.braneValue()) + " onde"
                + (game.braneValue() >= 2 ? "s" : "") + ", " + Format.multiplier(game.braneBounceFactor())
                + " par bord rebondi, et " + percent(game.braneComboStep()) + " de plus par passage de plus de la même onde. "
                + "Les points touchés restent tant que l'onde court, puis partent avec elle ; un point paraît toutes les "
                + seconds(Game.BRANE_SPAWN_SECONDS) + " s, " + Game.BRANE_MAX_POINTS + " au plus, et " + Game.BRANE_MAX_WAVES
                + " ondes courent au plus à la fois. Les ondes, le seuil et les améliorations de la brane traversent les "
                + "univers ; le seuil marque la dimension d'un rond vert, et les ondes paient aussi la lumière astrale de la graine."));
            braneDetail.setVisible(!braneDetail.getText().isEmpty());
            braneDetail.setManaged(braneDetail.isVisible());
        }
        paintUpgrades(braneButtons, this::describe, game::braneLevel, Game.BraneUpgrade::max, game::braneCost, game::canBuyBrane,
                " onde", BRANE_COLOR);
    }

    /** Une longueur sur la membrane (de côté 2) : deux décimales. */
    private static String reach(double value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    /** Une amélioration de la brane : son nom, où elle en est, et ce que donne le niveau suivant. */
    private String describe(Game.BraneUpgrade upgrade) {
        int level = game.braneLevel(upgrade);
        return switch (upgrade) {
            case GAIN -> "Gain : chaque passage vaut " + shardText(game.braneValue()) + " → "
                    + shardText(game.braneValue() + game.hadronBoost(idle.core.collider.Hadron.OMEGA)) + " ondes";
            case ELASTICITY -> "Élasticité : l'onde porte à fond dès " + Math.round(100 / (1 + Game.BRANE_ELASTIC_STEP * level))
                    + " % de tirée → " + Math.round(100 / (1 + Game.BRANE_ELASTIC_STEP * (level + 1))) + " %";
            case SIZE -> "Grandeur des ondes : portée à fond " + reach(game.braneMaxReach()) + " → "
                    + reach(Game.BRANE_REACH * (1 + Game.BRANE_SIZE_STEP * (level + 1))) + " (côté 2)";
            case BOUNCE -> "Rebond : un passage après un bord vaut " + Format.multiplier(game.braneBounceFactor()) + " → "
                    + Format.multiplier(game.braneBounceFactor() + Game.BRANE_BOUNCE_STEP);
            case COMBO -> "Points en une onde : chaque passage de plus de la même onde +" + percent(game.braneComboStep())
                    + " → +" + percent(game.braneComboStep() + Game.BRANE_COMBO_STEP);
        };
    }

    /** Les touches des cercles en jeu, dans l'ordre : « S, D, F ». */
    private String loopKeysText() {
        StringBuilder keys = new StringBuilder();
        for (int ring = 0; ring < game.loopRings(); ring++) {
            javafx.scene.input.KeyCode key = loopSettings.loopKey(ring);
            keys.append(ring == 0 ? "" : ", ").append(key == null ? "aucune" : idle.ui.common.Detail.keyName(key));
        }
        return keys.toString();
    }

    /** Une amélioration de la boucle : son nom, où elle en est, et ce que donne le niveau suivant. */
    private String describe(Game.LoopUpgrade upgrade) {
        double value = game.loopValue(), next = value + game.hadronBoost(idle.core.collider.Hadron.OMEGA);
        int precision = game.loopLevel(Game.LoopUpgrade.PRECISION);
        return switch (upgrade) {
            case SPEED -> "Rapidité : un tour en " + seconds(1 / game.loopTurnsPerSecond()) + " s → "
                    + seconds(1 / (game.loopTurnsPerSecond() * Game.LOOP_SPEED_STEP)) + " s";
            case ZONES -> "Zones : " + game.loopZoneCount() + " par cercle, une de plus";
            case GAIN -> "Gain par zone : chaque zone vaut " + shardText(value) + " → " + shardText(next) + " nœuds";
            case PRECISION -> "Précision : le centre donne " + shardText(game.loopReward(Game.LoopHit.EXCELLENT)) + " → "
                    + shardText(value * Game.LOOP_EXCELLENT_SHARE * (1 + Game.LOOP_PRECISION_EXCELLENT * (precision + 1)))
                    + ", « bien » " + shardText(game.loopReward(Game.LoopHit.GOOD)) + " → "
                    + shardText(value * (1 + Game.LOOP_PRECISION_GOOD * (precision + 1)));
            case SIZE -> "Taille des zones (×" + shardText(game.loopZoneHalfWidth() / Game.LOOP_ZONE_HALF) + ") : "
                    + percent(Game.LOOP_SIZE_STEP) + " plus larges";
            case RINGS -> {
                javafx.scene.input.KeyCode key = loopSettings.loopKey(Math.min(Game.MAX_RINGS - 1, game.loopRings()));
                yield "Cercles : " + game.loopRings() + ", un de plus"
                        + (game.loopRings() < Game.MAX_RINGS && key != null ? ", sur la touche " + idle.ui.common.Detail.keyName(key) : "");
            }
        };
    }

    /** Une amélioration de la durée : son nom, où elle en est, et ce que donne le niveau suivant. */
    private String describe(Game.DurationUpgrade upgrade) {
        return switch (upgrade) {
            case SPEED -> "Vitesse des spirales : " + shardText(game.durationTurnsPerSecond()) + " tour par seconde, " + percent(Game.DURATION_SPEED_STEP) + " plus vite";
            case COUNT -> "Spirales : " + game.durationSpirals() + ", une de plus qui avance en même temps";
            case VALUE -> "Valeur : le premier tour donne " + shardText(game.durationValue()) + " → " + shardText(game.durationValue() + game.hadronBoost(idle.core.collider.Hadron.OMEGA));
            case TIME -> "Temps au plus : " + seconds(game.durationMaxSeconds()) + " s → "
                    + seconds(game.durationMaxSeconds() + Game.DURATION_TIME_STEP) + " s, des tours de plus";
        };
    }

    /** Une amélioration du volume : son nom, où elle en est, et ce que donne le niveau suivant. */
    private String describe(Game.VolumeUpgrade upgrade) {
        return switch (upgrade) {
            case TIME -> "Durée : " + seconds(game.volumeDiveSeconds()) + " s par plongée → "
                    + seconds(game.volumeDiveSeconds() + Game.VOLUME_DIVE_STEP) + " s";
            case VALUE -> "Valeur : chaque boule cueillie donne " + shardText(game.volumeValue()) + " → " + shardText(game.volumeValue() + game.hadronBoost(idle.core.collider.Hadron.OMEGA));
            case SPAWN -> "Réapparition : une boule toutes les " + seconds(game.volumeSpawnSeconds()) + " s, 15 % plus vite";
            case COUNT -> "Nombre : " + game.volumeMaxBalls() + " boules à la fois au plus, une de plus";
            case SIZE -> "Taille des boules (×" + shardText(game.volumeBallRadius() / Game.VOLUME_BALL_RADIUS)
                    + ") : " + percent(Game.VOLUME_SIZE_STEP) + " plus grosses, elles se visent mieux";
        };
    }

    /** Une amélioration du plan : son nom, où elle en est, et ce que donne le niveau suivant. */
    private String describe(Game.PlaneUpgrade upgrade) {
        return switch (upgrade) {
            case VALUE -> "Valeur : chaque éclat ramassé donne " + shardText(game.planeValue()) + " → " + shardText(game.planeValue() + game.hadronBoost(idle.core.collider.Hadron.OMEGA)) + " facettes";
            case SPAWN -> "Réapparition : un éclat toutes les " + seconds(game.planeSpawnSeconds()) + " s, 15 % plus vite";
            case COUNT -> "Nombre : " + game.planeMaxShards() + " éclats à la fois au plus, un de plus";
            case SPEED -> "Vitesse du point : 20 % plus vite (×" + shardText(game.planeSpeed() / Game.PLANE_SPEED) + ")";
            case SIZE -> "Taille du point (×" + shardText(game.planePlayerRadius() / Game.PLANE_PLAYER_RADIUS) + ") : " + percent(Game.PLANE_SIZE_STEP) + " plus gros, il ramasse de plus loin";
        };
    }

    private static String seconds(double value) {
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    /** Un pas d'amélioration en pourcentage : 0,25 donne « 25 % ». */
    private static String percent(double step) {
        return Math.round(step * 100) + " %";
    }

    /**
     * Une touche, quand la fenêtre d'une dimension est ouverte : dans le plan, Z Q S D et les flèches déplacent le point ;
     * dans la durée, la barre d'espace tient le bouton ; dans la boucle, la touche de chaque cercle le joue. Vrai si la
     * touche a servi (le reste du jeu ne la voit pas).
     */
    public boolean key(javafx.scene.input.KeyCode code, boolean down) {
        // La boucle (version 0.153) : la touche d'un cercle le joue à l'appui ; tenue, elle ne le rejoue pas. Les touches
        // des cercles pas encore achetés ne servent à rien, mais ne vont pas aux raccourcis : la fenêtre les garde.
        if (isVisible() && view.dimension() == Dimension.LOOP && game.isLoopOpen()) {
            int ring = loopRingOf(code);
            if (ring >= 0) {
                if (!down) loopKeysHeld.remove(code);
                else if (loopKeysHeld.add(code) && ring < game.loopRings()) view.clickRing(ring);
                return true;
            }
        }
        // Le volume (version 0.128) : en caméra libre, Échap rend la souris retenue ; la plongée continue.
        if (isVisible() && code == javafx.scene.input.KeyCode.ESCAPE && view.mouseHeld()) {
            if (down) view.releaseMouse();
            return true;
        }
        // La durée (version 0.134) : la barre d'espace tient le bouton du milieu, comme la souris.
        if (isVisible() && code == javafx.scene.input.KeyCode.SPACE && view.dimension() == Dimension.DURATION && game.isDurationOpen()) {
            if (down && !spaceHeld) {
                spaceHeld = true;
                game.holdDuration();
            } else if (!down) {
                spaceHeld = false;
                view.releaseHold();
            }
            return true;
        }
        if (!isVisible() || view.dimension() != Dimension.PLANE || !game.isPlaneOpen()) return false;
        switch (code) {
            case Z, Q, S, D, UP, DOWN, LEFT, RIGHT -> { }
            default -> {
                return false;
            }
        }
        if (down) held.add(code);
        else held.remove(code);
        steer();
        return true;
    }

    /** Le cercle qui porte cette touche, ou −1. */
    private int loopRingOf(javafx.scene.input.KeyCode code) {
        for (int ring = 0; ring < Game.MAX_RINGS; ring++) if (code != null && code == loopSettings.loopKey(ring)) return ring;
        return -1;
    }

    private void steer() {
        double dx = 0, dy = 0;
        if (held.contains(javafx.scene.input.KeyCode.Q) || held.contains(javafx.scene.input.KeyCode.LEFT)) dx -= 1;
        if (held.contains(javafx.scene.input.KeyCode.D) || held.contains(javafx.scene.input.KeyCode.RIGHT)) dx += 1;
        if (held.contains(javafx.scene.input.KeyCode.Z) || held.contains(javafx.scene.input.KeyCode.UP)) dy -= 1;
        if (held.contains(javafx.scene.input.KeyCode.S) || held.contains(javafx.scene.input.KeyCode.DOWN)) dy += 1;
        game.setPlaneInput(dx, dy);
    }

    /** Une amélioration de la ligne : son nom, où elle en est, et ce que donne le niveau suivant. */
    private String describe(Game.LineUpgrade upgrade) {
        return switch (upgrade) {
            case YIELD -> "Densité (niveau " + game.lineLevel(upgrade) + ") : " + shardText(game.lineShardsPerTouch())
                    + " → " + shardText(game.lineShardsPerTouch() + Game.LINE_SHARD * game.hadronBoost(idle.core.collider.Hadron.OMEGA)) + " segment par trait touché";
            case TICKS -> "Graduation (" + game.lineTicks() + " traits) : un trait de plus, tous à égale distance";
            case SPEED -> "Élan (vitesse ×" + shardText(game.lineSpeed()) + ") : les boules vont " + percent(Game.LINE_SPEED_STEP) + " plus vite";
            case BALLS -> "Boules (" + game.lineBalls() + " par ligne) : une boule de plus sur chaque ligne";
            case LINES -> "Lignes (" + game.lineCount() + ") : une ligne de plus, avec ses traits et ses boules";
        };
    }

    private static String shardText(double value) {
        return Format.darkMatter(value);
    }

    /** Un débit d'éclats : deux décimales sous 1, une au-delà. */
    private static String rate(double perSecond) {
        if (perSecond < 1) return String.format(java.util.Locale.ROOT, "%.2f", Math.floor(perSecond * 100 + 1e-9) / 100);
        return Format.darkMatter(BigNum.of(perSecond));
    }

    /** Ouvre la fenêtre de cette dimension. */
    public void open(Dimension dimension) {
        view.show(dimension);
        view.setFreeCamera(freeCamera.getAsBoolean());
        title.setText("Dimension " + dimension.number() + " : " + dimension.label());
        held.clear();
        loopKeysHeld.clear();
        game.setPlaneInput(0, 0);
        line.setText(switch (dimension) {
            case LINE -> dimension.line() + " Chaque trait que touche la boule donne des segments.";
            case PLANE -> dimension.line() + " Chaque éclat ramassé donne des facettes, propres à cette dimension.";
            case VOLUME -> dimension.line() + " Plongez dans le cube : chaque boule cueillie de son centre donne des sphères.";
            case LOOP -> dimension.line() + (game.isLoopOpen()
                    ? " Cliquez chaque cercle quand son point passe dans une zone : plus le clic est juste, plus il donne de nœuds."
                    : " Il n'y a rien à y faire pour l'instant.");
            case BRANE -> dimension.line() + (game.isBraneOpen()
                    ? " Tirez la membrane et relâchez-la : chaque point que touche l'onde, ou l'un de ses rebonds, donne des ondes."
                    : " Il n'y a rien à y faire pour l'instant.");
            default -> dimension.line() + (dimension == Dimension.DURATION && game.isDurationOpen()
                    ? " Tenez le bouton du milieu : plus les spirales vont loin vers le centre, plus elles donnent d'instants."
                    : " Il n'y a rien à y faire pour l'instant.");
        });
        setVisible(true);
        refresh();
    }

    /**
     * Referme la fenêtre : retour au jeu. Une plongée en cours s'arrête là, le joueur ressort du cube, sauf si la
     * plongée automatique la mène (version 0.150) : elle continue sans la fenêtre.
     */
    public void close() {
        // La durée : fermer la fenêtre relâche le bouton, qui donne ce qu'il avait ; l'appui automatique, lui, continue.
        view.releaseHold();
        // La brane : la membrane tenue se lâche sans onde.
        view.cancelBrane();
        spaceHeld = false;
        held.clear();
        loopKeysHeld.clear();
        game.setPlaneInput(0, 0);
        if (game.isInVolume() && !game.isDimensionAutomationRunning(DimensionAutomation.VOLUME_DIVE)) game.leaveVolume();
        view.resetDive();
        setVisible(false);
    }

    public boolean isOpen() {
        return isVisible();
    }

    /** La dimension montrée. */
    public Dimension dimension() {
        return view.dimension();
    }

    public String titleText() {
        return title.getText();
    }

    public void frame(double elapsed, boolean moving) {
        if (!isVisible()) return;
        view.frame(elapsed, moving);
        refresh();
    }

    /** Le visuel : pour les vérifications. */
    public DimensionView view() {
        return view;
    }

    /** Le bouton d'une amélioration de la ligne : pour les vérifications. */
    public Button lineButton(Game.LineUpgrade upgrade) {
        return buttons.get(upgrade);
    }

    public Button yieldButton() {
        return buttons.get(Game.LineUpgrade.YIELD);
    }

    public Button ticksButton() {
        return buttons.get(Game.LineUpgrade.TICKS);
    }

    /** Le bouton d'une amélioration du plan : pour les vérifications. */
    public Button planeButton(Game.PlaneUpgrade upgrade) {
        return planeButtons.get(upgrade);
    }

    /** Le bouton d'une amélioration du volume : pour les vérifications. */
    public Button volumeButton(Game.VolumeUpgrade upgrade) {
        return volumeButtons.get(upgrade);
    }

    public String volumeShardsText() {
        return volumeShards.getText();
    }

    /** Le bouton d'une amélioration de la durée : pour les vérifications. */
    public Button durationButton(Game.DurationUpgrade upgrade) {
        return durationButtons.get(upgrade);
    }

    public String durationShardsText() {
        return durationShards.getText();
    }

    public String durationHintText() {
        return durationHint.getText();
    }

    /** Le bouton d'une amélioration de la boucle : pour les vérifications. */
    public Button loopButton(Game.LoopUpgrade upgrade) {
        return loopButtons.get(upgrade);
    }

    public String loopShardsText() {
        return loopShards.getText();
    }

    public String loopThresholdText() {
        return loopThreshold.getText();
    }

    public String loopHintText() {
        return loopHint.getText();
    }

    /** Le bouton d'une amélioration de la brane : pour les vérifications. */
    public Button braneButton(Game.BraneUpgrade upgrade) {
        return braneButtons.get(upgrade);
    }

    public String braneShardsText() {
        return braneShards.getText();
    }

    public String braneThresholdText() {
        return braneThreshold.getText();
    }

    public String braneHintText() {
        return braneHint.getText();
    }

    /** Le détail de la brane, vide tant que la touche de détail n'est pas tenue : pour les vérifications. */
    public String braneDetailText() {
        return braneDetail.getText();
    }

    /** Le bouton du métronome : pour les vérifications. */
    public Button metronomeButton() {
        return metronomeButton;
    }

    /** Le bouton de la caméra libre : pour les vérifications. */
    public Button freeCameraButton() {
        return freeCameraButton;
    }

    public String volumeHintText() {
        return volumeHint.getText();
    }

    /** Vrai si les améliorations du volume se montrent : hors du cube seulement. */
    public boolean volumeUpgradesShown() {
        return volumeRow.isVisible();
    }

    public String planeShardsText() {
        return planeShards.getText();
    }

    public String thresholdText() {
        return threshold.getText();
    }

    public String shardsText() {
        return shards.getText();
    }

    /** La mention « automatique », vide quand elle est cachée : pour les vérifications. */
    public String autoBadgeText() {
        return autoBadge.isVisible() ? autoBadge.getText() : "";
    }

    public String planeHintText() {
        return planeHint.getText();
    }

    /** Le bouton de retour : pour les vérifications. */
    public Button backButton() {
        return back;
    }
}
