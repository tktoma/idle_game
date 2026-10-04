package idle.ui;

import idle.core.BigNum;
import idle.core.Game;
import idle.core.Resource;
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
 * un bouton permet de les fusionner en un atome.
 *
 * <p>À partir du premier atome, des onglets apparaissent en haut de la fenêtre :
 * « Particules » (les générateurs et leurs améliorations) et « Atomes » ({@link AtomsPage}),
 * puis « Automatisation » ({@link AutomationPage}) une fois l'amélioration Persistance achetée.
 * Le jeu continue de tourner quel que soit l'onglet affiché.
 *
 * <p>Lancé avec l'argument {@code --test} (tâche Gradle {@code runTest}), le jeu affiche en
 * plus une {@link DebugBar} pour accélérer le temps et s'ajouter des ressources.
 *
 * <p>La fenêtre ne contient aucune règle : elle appelle {@code game.tick()},
 * affiche l'état, et transmet les clics à {@code game.buy()}.
 */
public final class GameApp extends Application {

    private static final double BUTTON_WIDTH = 420;

    /** Style commun aux deux onglets ; la couleur dépend de l'onglet et de son état. */
    private static final String TAB_STYLE = "-fx-font-size: 14px; -fx-padding: 10 28; -fx-cursor: hand;"
            + " -fx-background-radius: 0; -fx-border-width: 0 0 2 0;";
    private static final String PARTICLES_COLOR = "#9fd0ff";
    private static final String ATOMS_COLOR = "#ffd27f";
    private static final String AUTOMATION_COLOR = "#9be7a8";

    /** Les onglets du jeu. */
    private enum Tab { PARTICLES, ATOMS, AUTOMATION }

    private final Game game = new Game();
    private final GeneratorPane generators = new GeneratorPane();
    private final Label particlesLabel = new Label();
    private final Label productionLabel = new Label();
    private final Button fuseButton = new Button();

    // Les onglets, visibles à partir du premier atome.
    private final Button particlesTab = new Button("Particules");
    private final Button atomsTab = new Button();
    private final Button automationTab = new Button("Automatisation");
    private final AtomView atomsTabIcon = new AtomView(22);
    private final HBox tabBar = new HBox(particlesTab, atomsTab, automationTab);
    private final BorderPane particlesPage = new BorderPane();
    private final AtomsPage atomsPage = new AtomsPage(game);
    private final AutomationPage automationPage = new AutomationPage(game);
    private Tab selectedTab = Tab.PARTICLES;
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

        // Un bouton par amélioration payée en particules : en ajouter une dans core suffit à la faire apparaître.
        for (Upgrade upgrade : game.upgrades(Resource.PARTICLES)) {
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

        // Au centre : les générateurs, qui occupent toute la place restante.
        StackPane center = new StackPane(generators);
        if (!game.isStarted()) {
            center.getChildren().add(startButton(center, controls));
        }

        // Onglet « Particules » : les générateurs au-dessus, le compteur et les améliorations en dessous.
        particlesPage.setCenter(center);
        particlesPage.setBottom(controls);

        // Les onglets sont empilés au même endroit ; un seul est visible à la fois.
        StackPane pages = new StackPane(particlesPage, atomsPage, automationPage);
        particlesTab.setOnAction(event -> selectTab(Tab.PARTICLES));
        atomsTab.setOnAction(event -> selectTab(Tab.ATOMS));
        automationTab.setOnAction(event -> selectTab(Tab.AUTOMATION));
        atomsTab.setGraphic(atomsTabIcon);
        tabBar.setAlignment(Pos.CENTER);
        selectTab(Tab.PARTICLES);

        // En haut : la barre d'onglets, et au-dessus la barre du profil de test si le jeu est lancé avec --test.
        VBox top = new VBox(tabBar);
        boolean testProfile = getParameters().getRaw().contains("--test");
        if (testProfile) {
            debugBar = new DebugBar(game);
            top.getChildren().add(0, debugBar);
        }

        BorderPane root = new BorderPane();
        root.setTop(top);
        root.setCenter(pages);
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
                refresh();   // fait apparaître les onglets si c'est le premier atome

                // L'atome arrive : l'onglet « Atomes » bat quelques fois pour attirer l'œil.
                ScaleTransition notice = new ScaleTransition(Duration.seconds(0.35), atomsTab);
                notice.setFromX(1);
                notice.setFromY(1);
                notice.setToX(1.15);
                notice.setToY(1.15);
                notice.setAutoReverse(true);
                notice.setCycleCount(6);
                notice.play();
                atomsPage.celebrate();
            });
            refresh();
        });
        return fuseButton;
    }

    /** Affiche un onglet et masque les autres. */
    private void selectTab(Tab tab) {
        selectedTab = tab;
        particlesPage.setVisible(tab == Tab.PARTICLES);
        atomsPage.setVisible(tab == Tab.ATOMS);
        automationPage.setVisible(tab == Tab.AUTOMATION);
        particlesTab.setStyle(tabStyle(PARTICLES_COLOR, tab == Tab.PARTICLES));
        atomsTab.setStyle(tabStyle(ATOMS_COLOR, tab == Tab.ATOMS));
        automationTab.setStyle(tabStyle(AUTOMATION_COLOR, tab == Tab.AUTOMATION));
    }

    /** L'onglet affiché est souligné et éclairé dans sa couleur ; l'autre est estompé. */
    private static String tabStyle(String color, boolean selected) {
        return TAB_STYLE + (selected
                ? " -fx-text-fill: " + color + "; -fx-background-color: #16202e;"
                        + " -fx-border-color: transparent transparent " + color + " transparent;"
                : " -fx-text-fill: #8fa3b8; -fx-background-color: transparent; -fx-border-color: transparent;");
    }

    /** Affiche les générateurs existants et fait avancer les animations de l'onglet visible. */
    private void animate(double dt) {
        generators.setCount(game.generatorCount());
        atomsTabIcon.frame(dt);
        if (selectedTab == Tab.ATOMS) {
            atomsPage.frame(dt);
        }
        if (selectedTab != Tab.PARTICLES) {
            return;
        }
        generators.animateBackground(dt);
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
        particlesLabel.setText(Format.count(particles) + (particles.gt(BigNum.ONE) ? " particules" : " particule"));

        double rate = Math.min(game.productionPerSecond().toDouble(), 1e9);
        String perSecond = rate <= 0 ? "Aucun générateur"
                : rate < 1
                ? String.format(Locale.ROOT, "1 particule toutes les %.1f s", 1 / rate)
                : "+" + game.productionPerSecond().format() + " particules par seconde";
        productionLabel.setText(perSecond + "   |   " + Format.perMinute(game.productionPerSecond()) + " p/m");

        upgradeButtons.forEach((upgrade, button) -> {
            button.setText(label(upgrade));
            button.setDisable(!game.canBuy(upgrade.id()));
        });

        fuseButton.setText("Fusionner les " + game.maxGeneratorCount() + " générateurs en 1 atome");
        fuseButton.setVisible(game.canFuse() && !generators.isFusing());

        // Les onglets n'existent qu'à partir du premier atome ; avant, la barre ne prend aucune place.
        boolean hasAtoms = game.state().totalAtoms().sign() > 0;
        tabBar.setVisible(hasAtoms);
        tabBar.setManaged(hasAtoms);
        atomsTab.setText("Atomes (" + Format.count(game.state().atoms()) + ")");
        atomsPage.refresh();

        // L'onglet « Automatisation » n'existe qu'une fois la Persistance achetée.
        boolean automation = game.isAutomationUnlocked();
        automationTab.setVisible(automation);
        automationTab.setManaged(automation);
        automationPage.refresh();
    }

    private String label(Upgrade upgrade) {
        int level = game.levelOf(upgrade.id());
        if (game.isMaxed(upgrade.id())) {
            return upgrade.name() + " (maximum atteint)";
        }
        String levelText = upgrade.hasLimit() ? level + "/" + upgrade.maxLevel() : String.valueOf(level);
        return upgrade.name() + " (niveau " + levelText + ") : "
                + Format.count(game.costOf(upgrade.id())) + " particules";
    }
}
