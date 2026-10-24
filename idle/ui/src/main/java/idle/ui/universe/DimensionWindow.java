package idle.ui.universe;

import idle.core.Game;
import idle.ui.common.CanvasPane;
import idle.ui.common.Format;
import idle.core.math.BigNum;
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
 * la version 0.128 : on plonge dans le cube, et ses améliorations s'achètent ici, une fois ressorti. Les sept
 * autres restent des visuels.
 */
public final class DimensionWindow extends BorderPane {

    private static final String BUTTON_STYLE = "-fx-font-size: 13px; -fx-padding: 8 16; -fx-cursor: hand; -fx-background-radius: 6;"
            + " -fx-border-radius: 6; -fx-text-fill: " + UniversePage.COLOR + "; -fx-background-color: #141a2c; -fx-border-color: #3b4670;";

    private final Game game;
    private final DimensionView view;
    /** La ligne : ses éclats, et ses deux améliorations. */
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
    /** Les touches de direction tenues : Z, Q, S, D et les flèches. */
    private final java.util.Set<javafx.scene.input.KeyCode> held = java.util.EnumSet.noneOf(javafx.scene.input.KeyCode.class);
    private final Label title = new Label();
    private final Label line = new Label();
    private final Button back = new Button("← Retour");

    public DimensionWindow(Game game) {
        this.game = game;
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
            Button button = new Button();
            button.setStyle(BUTTON_STYLE);
            button.setFocusTraversable(false);
            button.setWrapText(true);
            button.setPrefWidth(250);
            button.setMinHeight(64);
            button.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
            button.setOnAction(event -> {
                game.buyLine(upgrade);
                refresh();
            });
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
            Button button = new Button();
            button.setStyle(BUTTON_STYLE);
            button.setFocusTraversable(false);
            button.setWrapText(true);
            button.setPrefWidth(250);
            button.setMinHeight(64);
            button.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
            button.setOnAction(event -> {
                game.buyPlane(upgrade);
                refresh();
            });
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
        volumeDetail.setStyle("-fx-font-size: 12px; -fx-text-fill: #8fa3b8;");
        volumeDetail.setWrapText(true);
        volumeDetail.setMaxWidth(820);
        volumeDetail.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        volumeRow.setAlignment(Pos.CENTER);
        for (Game.VolumeUpgrade upgrade : Game.VolumeUpgrade.values()) {
            Button button = new Button();
            button.setStyle(BUTTON_STYLE);
            button.setFocusTraversable(false);
            button.setWrapText(true);
            button.setPrefWidth(200);   // les cinq tiennent sur une ligne : la vue du cube garde sa hauteur
            button.setMinHeight(64);
            button.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
            button.setOnAction(event -> {
                game.buyVolume(upgrade);
                refresh();
            });
            volumeButtons.put(upgrade, button);
            volumeRow.getChildren().add(button);
        }
        volumePane = new VBox(10, volumeShards, volumeThreshold, volumeHint, volumeDetail, volumeRow);
        volumePane.setAlignment(Pos.CENTER);
        volumePane.setPadding(new Insets(10, 0, 0, 0));
        setBottom(new VBox(linePane, planePane, volumePane));
        setVisible(false);
    }

    /** Recopie la ligne ou le plan : leurs éclats, et ce que coûtent leurs améliorations. */
    private void refresh() {
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
        shards.setText("Éclats de la ligne : " + shardText(game.state().dimensionShards())
                + "   (+" + rate(game.lineShardsPerSecond()) + " par seconde)");
        threshold.setText(game.isLineThresholdReached()
                ? "Seuil atteint : " + shardText(Game.LINE_THRESHOLD) + " éclats de la ligne gagnés, la dimension 1 porte son rond vert"
                : "Seuil : " + shardText(game.state().dimensionShardsEarned()) + " / " + shardText(Game.LINE_THRESHOLD) + " éclats de la ligne gagnés");
        for (Game.LineUpgrade upgrade : Game.LineUpgrade.values()) {
            Button button = buttons.get(upgrade);
            boolean full = game.lineLevel(upgrade) >= game.lineMaxLevel(upgrade);
            button.setText(describe(upgrade) + "\n" + (full ? "Au maximum" : "Prix : " + shardText(game.lineCost(upgrade)) + (game.lineCost(upgrade) < 2 ? " éclat" : " éclats")));
            button.setDisable(!game.canBuyLine(upgrade));
        }
    }

    private void refreshPlane() {
        planeShards.setText("Éclats du plan : " + shardText(game.state().planeShards()));
        // Le seuil du plan (version 0.120), comme celui de la ligne : il ouvre le palier 3 de la graine.
        planeThreshold.setText(game.isPlaneThresholdReached()
                ? "Seuil atteint : " + shardText(Game.PLANE_THRESHOLD) + " éclats du plan gagnés, la dimension 2 porte son rond vert"
                : "Seuil : " + shardText(game.state().planeShardsEarned()) + " / " + shardText(Game.PLANE_THRESHOLD) + " éclats du plan gagnés");
        for (Game.PlaneUpgrade upgrade : Game.PlaneUpgrade.values()) {
            Button button = planeButtons.get(upgrade);
            boolean full = game.planeLevel(upgrade) >= upgrade.max();
            double cost = game.planeCost(upgrade);
            button.setText(describe(upgrade) + "\n" + (full ? "Au maximum" : "Prix : " + shardText(cost) + (cost < 2 ? " éclat du plan" : " éclats du plan")));
            button.setDisable(!game.canBuyPlane(upgrade));
        }
    }

    /**
     * Le volume : ses éclats et son seuil, toujours ; ses améliorations seulement dehors, puisqu'elles ne
     * s'achètent pas pendant une plongée ; dedans, ce qu'on y fait et le temps qu'il reste.
     */
    private void refreshVolume() {
        volumeShards.setText("Éclats du volume : " + shardText(game.state().volumeShards()));
        volumeThreshold.setText(game.isVolumeThresholdReached()
                ? "Seuil atteint : " + shardText(Game.VOLUME_THRESHOLD) + " éclats du volume gagnés, la dimension 3 porte son rond vert"
                : "Seuil : " + shardText(game.state().volumeShardsEarned()) + " / " + shardText(Game.VOLUME_THRESHOLD) + " éclats du volume gagnés");
        boolean outside = view.dive() == DimensionView.Dive.OUTSIDE;
        volumeHint.setText(switch (view.dive()) {
            case OUTSIDE -> "Cliquez le cube pour y plonger, " + seconds(game.volumeDiveSeconds()) + " s par plongée. "
                    + "Les améliorations s'achètent ici, hors du cube.";
            case ENTERING -> "Vous plongez dans le cube…";
            case INSIDE -> "Clic gauche tenu : regarder autour de soi. Clic droit : cueillir la boule visée. "
                    + "Le cube vous rejette dans " + seconds(game.volumeTimeLeft()) + " s.";
            case LEAVING -> "Le cube vous rejette : les améliorations reviennent.";
        });
        volumeDetail.setText(idle.ui.common.Detail.only("Une plongée commence quand la vue arrive au centre du cube. Une "
                + "boule apparaît aussitôt, puis une toutes les " + seconds(game.volumeSpawnSeconds()) + " s, "
                + game.volumeMaxBalls() + " à la fois au plus, entre " + shardText(Game.VOLUME_SPAWN_NEAR) + " et "
                + shardText(Game.VOLUME_SPAWN_FAR) + " du centre. Le clic droit cueille la plus proche que traverse le "
                + "regard ; une boule plus grosse se vise mieux. À la fin du minuteur, ou si le jeu passe en absence, la "
                + "plongée s'arrête et les boules restantes s'en vont. Les éclats du volume, son seuil et ses améliorations "
                + "traversent les univers ; le seuil ouvre le palier 4 de la graine."));
        volumeDetail.setVisible(!volumeDetail.getText().isEmpty());
        volumeDetail.setManaged(volumeDetail.isVisible());
        // Dedans, la rangée se cache mais garde sa place : le cube ne se déplace pas pendant la plongée.
        volumeRow.setVisible(outside);
        for (Game.VolumeUpgrade upgrade : Game.VolumeUpgrade.values()) {
            Button button = volumeButtons.get(upgrade);
            boolean full = game.volumeLevel(upgrade) >= upgrade.max();
            double cost = game.volumeCost(upgrade);
            button.setText(describe(upgrade) + "\n" + (full ? "Au maximum" : "Prix : " + shardText(cost) + (cost < 2 ? " éclat du volume" : " éclats du volume")));
            button.setDisable(!outside || !game.canBuyVolume(upgrade));
        }
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
                    + ") : 20 % plus grosses, elles se visent mieux";
        };
    }

    /** Une amélioration du plan : son nom, où elle en est, et ce que donne le niveau suivant. */
    private String describe(Game.PlaneUpgrade upgrade) {
        return switch (upgrade) {
            case VALUE -> "Valeur : chaque éclat ramassé donne " + shardText(game.planeValue()) + " → " + shardText(game.planeValue() + 1);
            case SPAWN -> "Réapparition : un éclat toutes les " + seconds(game.planeSpawnSeconds()) + " s, 15 % plus vite";
            case COUNT -> "Nombre : " + game.planeMaxShards() + " éclats à la fois au plus, un de plus";
            case SPEED -> "Vitesse du point : 20 % plus vite (×" + shardText(game.planeSpeed() / Game.PLANE_SPEED) + ")";
            case SIZE -> "Taille du point (×" + shardText(game.planePlayerRadius() / Game.PLANE_PLAYER_RADIUS) + ") : 25 % plus gros, il ramasse de plus loin";
        };
    }

    private static String seconds(double value) {
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    /**
     * Une touche, quand la fenêtre du plan est ouverte : Z Q S D et les flèches déplacent le point.
     * Vrai si la touche a servi (le reste du jeu ne la voit pas).
     */
    public boolean key(javafx.scene.input.KeyCode code, boolean down) {
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
                    + " → " + shardText(game.lineShardsPerTouch() + Game.LINE_SHARD) + " éclat par trait touché";
            case TICKS -> "Graduation (" + game.lineTicks() + " traits) : un trait de plus, tous à égale distance";
            case SPEED -> "Élan (vitesse ×" + shardText(game.lineSpeed()) + ") : les boules vont 25 % plus vite";
            case BALLS -> "Boules (" + game.lineBalls() + " par ligne) : une boule de plus sur chaque ligne";
            case LINES -> "Lignes (" + game.lineCount() + ") : une ligne de plus, avec ses traits et ses boules";
        };
    }

    private static String shardText(double value) {
        return Format.darkMatter(BigNum.of(value));
    }

    /** Un débit d'éclats : deux décimales sous 1, une au-delà. */
    private static String rate(double perSecond) {
        if (perSecond < 1) return String.format(java.util.Locale.ROOT, "%.2f", Math.floor(perSecond * 100 + 1e-9) / 100);
        return Format.darkMatter(BigNum.of(perSecond));
    }

    /** Ouvre la fenêtre de cette dimension. */
    public void open(Dimension dimension) {
        view.show(dimension);
        title.setText("Dimension " + dimension.number() + " : " + dimension.label());
        held.clear();
        game.setPlaneInput(0, 0);
        line.setText(switch (dimension) {
            case LINE -> dimension.line() + " Chaque trait que touche la boule donne des éclats de la ligne.";
            case PLANE -> dimension.line() + " Chaque éclat ramassé donne des éclats du plan, propres à cette dimension.";
            case VOLUME -> dimension.line() + " Plongez dans le cube : chaque boule cueillie de son centre donne des éclats du volume.";
            default -> dimension.line() + " Il n'y a rien à y faire pour l'instant.";
        });
        setVisible(true);
        refresh();
    }

    /** Referme la fenêtre : retour au jeu. Une plongée en cours s'arrête là, le joueur ressort du cube. */
    public void close() {
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
