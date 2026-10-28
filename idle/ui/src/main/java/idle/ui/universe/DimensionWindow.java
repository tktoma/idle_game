package idle.ui.universe;

import idle.core.Game;
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
 * sienne depuis la version 0.134 : on tient le bouton du milieu, les spirales avancent, on relâche. Les six autres
 * restent des visuels.
 */
public final class DimensionWindow extends BorderPane {

    private static final String BUTTON_STYLE = "-fx-font-size: 13px; -fx-padding: 8 16; -fx-cursor: hand; -fx-background-radius: 6;"
            + " -fx-border-radius: 6; -fx-text-fill: " + UniversePage.COLOR + "; -fx-background-color: #141a2c; -fx-border-color: #3b4670;";

    /** La couleur de chaque dimension, celle de ses éclats : ses améliorations achetables la prennent (version 0.140). */
    private static final String PLANE_COLOR = "#ffd27f", VOLUME_COLOR = "#c9a6ff", DURATION_COLOR = "#9be7a8";
    private static final String UPGRADE_STYLE = "-fx-font-size: 13px; -fx-background-radius: 6; -fx-border-radius: 6; -fx-opacity: 1;";

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
    /** La barre d'espace tient le bouton de la durée, comme la souris. */
    private boolean spaceHeld;
    /** La caméra libre (version 0.128) : le bouton, et où se range le choix (les réglages du jeu, ou la fenêtre seule). */
    private final Button freeCameraButton = new Button();
    private final java.util.function.BooleanSupplier freeCamera;
    private final java.util.function.Consumer<Boolean> setFreeCamera;
    /** Les touches de direction tenues : Z, Q, S, D et les flèches. */
    private final java.util.Set<javafx.scene.input.KeyCode> held = java.util.EnumSet.noneOf(javafx.scene.input.KeyCode.class);
    private final Label title = new Label();
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
        // Une marge à droite aussi large que le bouton, pour que le titre reste au milieu.
        Region balance = new Region();
        balance.setMinWidth(110);
        Region after = new Region();
        HBox.setHgrow(after, Priority.ALWAYS);
        HBox top = new HBox(12, back, spacer, title, after, balance);
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
        Label planeHint = new Label("Z Q S D (ou les flèches) déplacent le point. Passez sur un éclat pour le ramasser.");
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
        setBottom(new VBox(linePane, planePane, volumePane, durationPane));
        setVisible(false);
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

    /** Recopie la dimension montrée (ligne, plan, volume ou durée) : ses éclats, et ce que coûtent ses améliorations. */
    private void refresh() {
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
        volumeHint.setText(switch (view.dive()) {
            case OUTSIDE -> "Cliquez le cube pour y plonger, " + seconds(game.volumeDiveSeconds()) + " s par plongée. "
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
        durationHint.setText(!held
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

    /** Une amélioration de la durée : son nom, où elle en est, et ce que donne le niveau suivant. */
    private String describe(Game.DurationUpgrade upgrade) {
        return switch (upgrade) {
            case SPEED -> "Vitesse des spirales : " + shardText(game.durationTurnsPerSecond()) + " tour par seconde, " + percent(Game.DURATION_SPEED_STEP) + " plus vite";
            case COUNT -> "Spirales : " + game.durationSpirals() + ", une de plus qui avance en même temps";
            case VALUE -> "Valeur : le premier tour donne " + shardText(game.durationValue()) + " → " + shardText(game.durationValue() + 1);
            case TIME -> "Temps au plus : " + seconds(game.durationMaxSeconds()) + " s → "
                    + seconds(game.durationMaxSeconds() + Game.DURATION_TIME_STEP) + " s, des tours de plus";
        };
    }

    /** Une amélioration du volume : son nom, où elle en est, et ce que donne le niveau suivant. */
    private String describe(Game.VolumeUpgrade upgrade) {
        return switch (upgrade) {
            case TIME -> "Durée : " + seconds(game.volumeDiveSeconds()) + " s par plongée → "
                    + seconds(game.volumeDiveSeconds() + Game.VOLUME_DIVE_STEP) + " s";
            case VALUE -> "Valeur : chaque boule cueillie donne " + shardText(game.volumeValue()) + " → " + shardText(game.volumeValue() + 1);
            case SPAWN -> "Réapparition : une boule toutes les " + seconds(game.volumeSpawnSeconds()) + " s, 15 % plus vite";
            case COUNT -> "Nombre : " + game.volumeMaxBalls() + " boules à la fois au plus, une de plus";
            case SIZE -> "Taille des boules (×" + shardText(game.volumeBallRadius() / Game.VOLUME_BALL_RADIUS)
                    + ") : " + percent(Game.VOLUME_SIZE_STEP) + " plus grosses, elles se visent mieux";
        };
    }

    /** Une amélioration du plan : son nom, où elle en est, et ce que donne le niveau suivant. */
    private String describe(Game.PlaneUpgrade upgrade) {
        return switch (upgrade) {
            case VALUE -> "Valeur : chaque éclat ramassé donne " + shardText(game.planeValue()) + " → " + shardText(game.planeValue() + 1) + " facettes";
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
     * Une touche, quand la fenêtre du plan est ouverte : Z Q S D et les flèches déplacent le point.
     * Vrai si la touche a servi (le reste du jeu ne la voit pas).
     */
    public boolean key(javafx.scene.input.KeyCode code, boolean down) {
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
                    + " → " + shardText(game.lineShardsPerTouch() + Game.LINE_SHARD) + " segment par trait touché";
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
        game.setPlaneInput(0, 0);
        line.setText(switch (dimension) {
            case LINE -> dimension.line() + " Chaque trait que touche la boule donne des segments.";
            case PLANE -> dimension.line() + " Chaque éclat ramassé donne des facettes, propres à cette dimension.";
            case VOLUME -> dimension.line() + " Plongez dans le cube : chaque boule cueillie de son centre donne des sphères.";
            default -> dimension.line() + (dimension == Dimension.DURATION && game.isDurationOpen()
                    ? " Tenez le bouton du milieu : plus les spirales vont loin vers le centre, plus elles donnent d'instants."
                    : " Il n'y a rien à y faire pour l'instant.");
        });
        setVisible(true);
        refresh();
    }

    /** Referme la fenêtre : retour au jeu. Une plongée en cours s'arrête là, le joueur ressort du cube. */
    public void close() {
        // La durée : fermer la fenêtre relâche le bouton, qui donne ce qu'il avait.
        view.releaseHold();
        spaceHeld = false;
        held.clear();
        game.setPlaneInput(0, 0);
        if (game.isInVolume()) game.leaveVolume();
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

    /** Le bouton de retour : pour les vérifications. */
    public Button backButton() {
        return back;
    }
}
