package idle.ui;

import idle.core.BigNum;
import idle.core.Game;
import idle.core.Upgrade;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javafx.animation.Animation;
import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Aperçu minimal du jeu : les générateurs animés, le compteur de particules,
 * la production, et un bouton par amélioration.
 *
 * <p>Une partie neuve s'ouvre sur un écran de démarrage : un fond de poussières et un
 * bouton qui crée le premier générateur. Quand tous les générateurs sont débloqués,
 * un bouton permet de les fusionner en un atome, affiché ensuite en haut de la fenêtre.
 *
 * <p>Lancé avec l'argument {@code --test} (tâche Gradle {@code runTest}), le jeu affiche en
 * plus une {@link DebugBar} pour accélérer le temps et s'ajouter des ressources.
 *
 * <p>La fenêtre ne contient aucune règle : elle appelle {@code game.tick()},
 * affiche l'état, et transmet les clics à {@code game.buy()}.
 */
public final class GameApp extends Application {

    private static final double BUTTON_WIDTH = 420;
    private static final BigNum THOUSAND = BigNum.of(1, 3);
    private static final BigNum MILLION = BigNum.of(1, 6);

    private final Game game = new Game();
    private final GeneratorPane generators = new GeneratorPane();
    private final Label particlesLabel = new Label();
    private final Label productionLabel = new Label();
    private final AtomView atomView = new AtomView(44);
    private final Label atomsLabel = new Label();
    private final HBox atomsBar = new HBox(12, atomView, atomsLabel);
    private final Button fuseButton = new Button();
    /** Barre du profil de test, ou {@code null} en jeu normal. */
    private DebugBar debugBar;
    private final Map<Upgrade, Button> upgradeButtons = new LinkedHashMap<>();

    @Override
    public void start(Stage stage) {
        particlesLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #e8f4ff;");
        productionLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #8fa3b8;");

        // En bas : le compteur et les améliorations. Cachés tant que la partie n'a pas démarré,
        // mais leur place est réservée pour que rien ne bouge à leur apparition.
        VBox controls = new VBox(12, particlesLabel, productionLabel);
        controls.setVisible(game.isStarted());
        controls.setPadding(new Insets(12, 24, 24, 24));
        controls.setAlignment(Pos.TOP_CENTER);

        // Un bouton par amélioration du catalogue : en ajouter une dans core suffit à la faire apparaître.
        for (Upgrade upgrade : game.upgrades()) {
            Button button = new Button();
            button.setPrefWidth(BUTTON_WIDTH);
            button.setOnAction(event -> {
                game.buy(upgrade.id());
                refresh();
            });
            upgradeButtons.put(upgrade, button);
            controls.getChildren().add(button);
        }
        controls.getChildren().add(fuseButton());

        // En haut : les atomes. La barre n'apparaît qu'après la première fusion.
        atomsLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #ffd27f;");
        atomsBar.setAlignment(Pos.CENTER);
        atomsBar.setPadding(new Insets(12, 24, 0, 24));

        // Au centre : les générateurs, qui occupent toute la place restante.
        StackPane center = new StackPane(generators);
        if (!game.isStarted()) {
            center.getChildren().add(startButton(center, controls));
        }

        BorderPane root = new BorderPane();
        // Tout en haut : la barre du profil de test, seulement si le jeu est lancé avec --test.
        VBox top = new VBox(atomsBar);
        boolean testProfile = getParameters().getRaw().contains("--test");
        if (testProfile) {
            debugBar = new DebugBar(game);
            top.getChildren().add(0, debugBar);
        }
        root.setTop(top);
        root.setCenter(center);
        root.setBottom(controls);
        root.setStyle("-fx-background-color: #0b0e14;");

        // Boucle de jeu : appelée à chaque image (~60 fois par seconde) sur le thread JavaFX.
        new AnimationTimer() {
            private long previous = 0;

            @Override
            public void handle(long now) {
                double dt = previous == 0 ? 0 : (now - previous) / 1e9; // nanosecondes → secondes
                previous = now;
                game.tick(dt * timeFactor());
                animate(dt);
                refresh();
            }
        }.start();

        animate(0);
        refresh();
        stage.setTitle(testProfile ? "Idle [profil de test]" : "Idle");
        stage.setScene(new Scene(root, 800, 600));
        stage.setMinWidth(480);
        stage.setMinHeight(420);
        stage.show();
    }

    /**
     * Bouton de l'écran de démarrage : il crée le premier générateur, s'efface,
     * puis le compteur et les améliorations apparaissent.
     */
    private Button startButton(StackPane center, VBox controls) {
        Button button = new Button("Créer le premier générateur");
        button.setStyle("-fx-font-size: 16px; -fx-padding: 12 28; -fx-text-fill: #e8f4ff;"
                + " -fx-background-color: #16202e; -fx-background-radius: 8;"
                + " -fx-border-color: #9fd0ff; -fx-border-radius: 8; -fx-cursor: hand;");

        // Le bouton « respire » pour attirer l'œil.
        ScaleTransition pulse = new ScaleTransition(Duration.seconds(1.1), button);
        pulse.setFromX(1);
        pulse.setFromY(1);
        pulse.setToX(1.06);
        pulse.setToY(1.06);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.play();

        button.setOnAction(event -> {
            pulse.stop();
            button.setDisable(true);
            game.start(); // le générateur apparaît à l'image suivante, avec son animation de naissance

            FadeTransition hideButton = new FadeTransition(Duration.seconds(0.3), button);
            hideButton.setToValue(0);
            hideButton.setOnFinished(done -> center.getChildren().remove(button));
            hideButton.play();

            // Les commandes arrivent une fois le générateur allumé.
            controls.setOpacity(0);
            controls.setVisible(true);
            FadeTransition showControls = new FadeTransition(Duration.seconds(0.8), controls);
            showControls.setDelay(Duration.seconds(1.2));
            showControls.setToValue(1);
            showControls.play();
        });
        return button;
    }

    /**
     * Bouton de fusion : il n'apparaît que lorsque tous les générateurs sont débloqués.
     * Les générateurs se regroupent au centre, puis le jeu applique la fusion.
     */
    private Button fuseButton() {
        fuseButton.setPrefWidth(BUTTON_WIDTH);
        fuseButton.setStyle("-fx-font-size: 14px; -fx-padding: 8 16; -fx-text-fill: #ffd27f;"
                + " -fx-background-color: #2a2113; -fx-background-radius: 8;"
                + " -fx-border-color: #ffd27f; -fx-border-radius: 8; -fx-cursor: hand;");
        fuseButton.setVisible(false); // sa place reste réservée pour que rien ne bouge à son apparition

        ScaleTransition pulse = new ScaleTransition(Duration.seconds(1.1), fuseButton);
        pulse.setFromX(1);
        pulse.setFromY(1);
        pulse.setToX(1.04);
        pulse.setToY(1.04);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.play();

        fuseButton.setOnAction(event -> {
            if (!game.canFuse()) return;
            generators.fuse(() -> {
                game.fuse(); // un seul générateur renaît à l'image suivante, là où les autres ont fusionné

                // L'atome arrive : son icône part grande et se pose à sa taille.
                ScaleTransition arrive = new ScaleTransition(Duration.seconds(0.6), atomView);
                arrive.setFromX(2.5);
                arrive.setFromY(2.5);
                arrive.setToX(1);
                arrive.setToY(1);
                arrive.play();
                refresh();
            });
            refresh();
        });
        return fuseButton;
    }

    /** Affiche les générateurs existants et fait avancer les animations. */
    private void animate(double dt) {
        generators.animateBackground(dt);
        atomView.frame(dt);
        generators.setCount(game.generatorCount());
        double speed = Math.min(game.speed().toDouble() * timeFactor(), 1e9);
        List<ParticleView> views = generators.views();
        for (int i = 0; i < views.size(); i++) {
            views.get(i).frame(dt, game.state().formation(i), speed);
        }
    }

    /** Accélération du temps : toujours 1 en jeu normal, réglable dans le profil de test. */
    private double timeFactor() {
        return debugBar == null ? 1 : debugBar.timeFactor();
    }

    /** Recopie l'état du jeu dans les composants. */
    private void refresh() {
        BigNum particles = game.state().particles();
        particlesLabel.setText(count(particles) + (particles.gt(BigNum.ONE) ? " particules" : " particule"));

        double rate = Math.min(game.productionPerSecond().toDouble(), 1e9);
        String perSecond = rate <= 0 ? "Aucun générateur"
                : rate < 1
                ? String.format(Locale.ROOT, "1 particule toutes les %.1f s", 1 / rate)
                : "+" + game.productionPerSecond().format() + " particules par seconde";
        productionLabel.setText(perSecond + "   |   " + perMinute(game.productionPerSecond()) + " p/m");

        upgradeButtons.forEach((upgrade, button) -> {
            button.setText(label(upgrade));
            button.setDisable(!game.canBuy(upgrade.id()));
        });

        fuseButton.setText("Fusionner les " + game.maxGeneratorCount() + " générateurs en 1 atome");
        fuseButton.setVisible(game.canFuse() && !generators.isFusing());

        BigNum atoms = game.state().atoms();
        boolean hasAtoms = atoms.sign() > 0;
        atomsBar.setVisible(hasAtoms);
        atomsBar.setManaged(hasAtoms); // tant qu'il n'y a pas d'atome, la barre ne prend aucune place
        atomsLabel.setText(count(atoms) + (atoms.gt(BigNum.ONE) ? " atomes" : " atome")
                + "   |   " + count(game.particlesPerCreation()) + " particules par création");
    }

    private String label(Upgrade upgrade) {
        int level = game.levelOf(upgrade.id());
        if (game.isMaxed(upgrade.id())) {
            return upgrade.name() + " (maximum atteint)";
        }
        String levelText = upgrade.hasLimit() ? level + "/" + upgrade.maxLevel() : String.valueOf(level);
        return upgrade.name() + " (niveau " + levelText + ") : "
                + count(game.costOf(upgrade.id())) + " particules";
    }

    /** Production par minute : une décimale tant que le nombre est petit, puis comme les autres nombres. */
    private static String perMinute(BigNum perSecond) {
        BigNum value = perSecond.multiply(60);
        if (value.lt(THOUSAND)) {
            String text = String.format(Locale.ROOT, "%.1f", value.toDouble());
            return text.endsWith(".0") ? text.substring(0, text.length() - 2) : text;
        }
        return count(value);
    }

    /** Nombre de particules : en entier tant que c'est lisible, puis en notation scientifique. */
    private static String count(BigNum value) {
        return value.lt(MILLION) ? String.valueOf(Math.round(value.toDouble())) : value.format();
    }
}