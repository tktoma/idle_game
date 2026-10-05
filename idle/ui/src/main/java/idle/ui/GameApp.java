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
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
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
 * <p>En haut de la fenêtre, des onglets. Trois sont là dès le début : « Particules » (les
 * générateurs et leurs améliorations), « Statistiques » ({@link StatsPage}) et « Réglages »
 * ({@link SettingsPage}). Les autres apparaissent avec la partie : « Atomes » ({@link AtomsPage})
 * au premier atome, « Automatisation » ({@link AutomationPage}) dès qu'un automatisme est
 * débloqué. Le jeu continue de tourner quel que soit l'onglet affiché, sauf en pause.
 *
 * <p>Quand le tableau périodique est complet, un bouton apparaît au-dessus des onglets : il
 * déclenche une explosion qui remet toute la partie à zéro et fait apparaître l'onglet
 * « Matière noire » ({@link DarkMatterPage}).
 *
 * <p>Lancé avec l'argument {@code --test} (tâche Gradle {@code runTest}), le jeu affiche en
 * plus une {@link DebugBar} pour accélérer le temps et s'ajouter des ressources.
 *
 * <p>La fenêtre ne contient aucune règle : elle appelle {@code game.tick()},
 * affiche l'état, et transmet les clics à {@code game.buy()}.
 */
public final class GameApp extends Application {

    private static final double BUTTON_WIDTH = 420;
    /** Nombre maximal de générateurs dessinés ; les suivants produisent sans être affichés. */
    private static final int MAX_VISIBLE_GENERATORS = 30;

    /** Style commun aux onglets ; la couleur dépend de l'onglet et de son état, les marges de la largeur de la fenêtre. */
    private static final String TAB_STYLE = "-fx-cursor: hand; -fx-background-radius: 0; -fx-border-width: 0 0 2 0;";
    private static final String WIDE_TAB = " -fx-font-size: 14px; -fx-padding: 10 14;";
    private static final String NARROW_TAB = " -fx-font-size: 12px; -fx-padding: 8 10;";
    /** En dessous de cette largeur de fenêtre, les onglets se resserrent pour tenir sur une ligne. */
    private static final double NARROW_WINDOW = 780;
    private static final String PARTICLES_COLOR = "#9fd0ff";
    private static final String ATOMS_COLOR = "#ffd27f";
    private static final String AUTOMATION_COLOR = "#9be7a8";
    static final String DARK_MATTER_COLOR = "#c9a6ff";
    private static final String STATS_COLOR = "#7fe0d4";
    private static final String SETTINGS_COLOR = "#d6dde6";
    /** Temps laissé pour le second clic qui confirme l'explosion, en secondes. */
    private static final double EXPLOSION_CONFIRM_SECONDS = 5;

    /** Les onglets du jeu. */
    private enum Tab { PARTICLES, ATOMS, AUTOMATION, DARK_MATTER, STATS, SETTINGS }

    private final Game game = new Game();
    private final Settings settings = Settings.load();
    private final GeneratorPane generators = new GeneratorPane();
    private final Label particlesLabel = new Label();
    private final Label productionLabel = new Label();
    private final Button fuseButton = new Button();

    // Les onglets, visibles à partir du premier atome.
    private final Button particlesTab = new Button("Particules");
    private final Button atomsTab = new Button();
    private final Button automationTab = new Button("Automatisation");
    private final Button darkMatterTab = new Button();
    private final Button statsTab = new Button("Statistiques");
    private final Button settingsTab = new Button("Réglages");
    private final AtomView atomsTabIcon = new AtomView(22);
    private final HBox tabBar = new HBox(particlesTab, atomsTab, automationTab, darkMatterTab, statsTab, settingsTab);
    private final BorderPane particlesPage = new BorderPane();
    private final AtomsPage atomsPage = new AtomsPage(game);
    private final AutomationPage automationPage = new AutomationPage(game);
    private final DarkMatterPage darkMatterPage = new DarkMatterPage(game);
    private final StatsPage statsPage = new StatsPage(game);
    private final SettingsPage settingsPage = new SettingsPage(settings, this::resetGame);
    /** La page des automatismes défile quand la fenêtre est trop basse pour toutes ses cartes. */
    private final ScrollPane automationScroll = new ScrollPane(automationPage);
    private final ScrollPane settingsScroll = new ScrollPane(settingsPage);
    /** Le centre de l'onglet « Particules » : les générateurs, et le bouton de démarrage tant que la partie n'a pas commencé. */
    private final StackPane center = new StackPane(generators);
    /** Le compteur et les améliorations, sous les générateurs. */
    private final VBox controls = new VBox(12, particlesLabel, productionLabel);
    private Button startButton;
    private final BorderPane root = new BorderPane();
    private Tab selectedTab = Tab.PARTICLES;
    private boolean narrow = false;

    // L'explosion : le bouton au-dessus des onglets, et ce qui recouvre la fenêtre pendant qu'elle a lieu.
    private final Button explosionButton = new Button();
    private final StackPane flash = new StackPane();
    private final Circle shockwave = new Circle(24, Color.web(DARK_MATTER_COLOR));
    private final StackPane blast = new StackPane(flash, shockwave);
    private boolean exploding = false;
    /** Secondes restantes pour confirmer l'explosion par un second clic ; 0 quand elle n'est pas demandée. */
    private double explosionArmed = 0;
    /** Atomes créés à la dernière image, pour repérer l'arrivée d'un nouvel atome ; −1 avant la première image. */
    private double lastTotalAtoms = -1;
    /** Barre du profil de test, ou {@code null} en jeu normal. */
    private DebugBar debugBar;
    private final Map<Upgrade, Button> upgradeButtons = new LinkedHashMap<>();

    @Override
    public void start(Stage stage) {
        particlesLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #e8f4ff;");
        productionLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #8fa3b8;");

        // En bas : le compteur et les améliorations. Cachés tant que la partie n'a pas démarré,
        // mais leur place est réservée pour que rien ne bouge à leur apparition.
        controls.setVisible(game.isStarted());
        controls.setPadding(new Insets(12, 24, 24, 24));
        controls.setAlignment(Pos.TOP_CENTER);

        // Un bouton par amélioration payée en particules : en ajouter une dans core suffit à la faire apparaître.
        for (Upgrade upgrade : game.upgrades(Resource.PARTICLES)) {
            Button button = new Button();
            button.setPrefWidth(BUTTON_WIDTH);
            button.setMinWidth(0);               // dans une fenêtre étroite, le bouton rétrécit avec elle
            button.setOnAction(event -> {
                game.buy(upgrade.id());
                refresh();
            });
            upgradeButtons.put(upgrade, button);
            controls.getChildren().add(button);
        }
        controls.getChildren().add(fuseButton());

        // Au centre : les générateurs, qui occupent toute la place restante.
        if (!game.isStarted()) showStartButton();

        // Onglet « Particules » : les générateurs au-dessus, le compteur et les améliorations en dessous.
        particlesPage.setCenter(center);
        particlesPage.setBottom(controls);

        // Les onglets sont empilés au même endroit ; un seul est visible à la fois.
        automationScroll.setFitToWidth(true);
        automationScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        automationScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        settingsScroll.setFitToWidth(true);
        settingsScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        settingsScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        StackPane pages = new StackPane(particlesPage, atomsPage, automationScroll, darkMatterPage, statsPage, settingsScroll);
        particlesTab.setOnAction(event -> selectTab(Tab.PARTICLES));
        atomsTab.setOnAction(event -> selectTab(Tab.ATOMS));
        automationTab.setOnAction(event -> selectTab(Tab.AUTOMATION));
        darkMatterTab.setOnAction(event -> selectTab(Tab.DARK_MATTER));
        statsTab.setOnAction(event -> selectTab(Tab.STATS));
        settingsTab.setOnAction(event -> selectTab(Tab.SETTINGS));
        atomsTab.setGraphic(atomsTabIcon);
        tabBar.setAlignment(Pos.CENTER);
        selectTab(Tab.PARTICLES);

        // En haut : la barre d'onglets, et au-dessus la barre du profil de test si le jeu est lancé avec --test.
        // Au-dessus des onglets : le bouton d'explosion, quand le tableau périodique est complet.
        explosionButton.setMaxWidth(Double.MAX_VALUE);
        explosionButton.setWrapText(true);
        explosionButton.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-padding: 10 20; -fx-cursor: hand;"
                + " -fx-text-fill: #1a0f2e; -fx-background-color: " + DARK_MATTER_COLOR + "; -fx-background-radius: 0;");
        explosionButton.setOnAction(event -> requestExplosion());
        VBox top = new VBox(explosionButton, tabBar);
        boolean testProfile = getParameters().getRaw().contains("--test");
        if (testProfile) {
            debugBar = new DebugBar(game);
            top.getChildren().add(0, debugBar);
        }

        root.setTop(top);
        root.setCenter(pages);
        root.setStyle("-fx-background-color: #0b0e14;");

        // L'explosion recouvre toute la fenêtre : un éclair blanc et une onde qui part du centre.
        flash.setStyle("-fx-background-color: #ffffff;");
        flash.setOpacity(0);
        blast.setVisible(false);
        StackPane window = new StackPane(root, blast);

        // Boucle de jeu : appelée à chaque image (~60 fois par seconde) sur le thread JavaFX.
        new AnimationTimer() {
            private long previous = 0;

            @Override
            public void handle(long now) {
                double dt = previous == 0 ? 0 : (now - previous) / 1e9; // nanosecondes → secondes
                previous = now;
                // En pause, le temps du jeu s'arrête ; celui de l'interface continue (confirmations, réglages).
                if (!settings.paused()) {
                    game.tick(dt * timeFactor());
                    animate(dt);
                }
                explosionArmed = Math.max(0, explosionArmed - dt);
                if (selectedTab == Tab.SETTINGS) settingsPage.frame(dt);
                refresh();
            }
        }.start();

        animate(0);
        refresh();
        stage.setTitle(testProfile ? "Idle [profil de test]" : "Idle");
        stage.setScene(new Scene(window, 800, 600));
        stage.setMinWidth(480);
        stage.setMinHeight(420);
        stage.show();
    }

    /**
     * Bouton de l'écran de démarrage : il crée le premier générateur, s'efface,
     * puis le compteur et les améliorations apparaissent.
     */
    private void showStartButton() {
        if (startButton != null) return;
        Button button = new Button("Créer le premier générateur");
        startButton = button;
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
            hideButton.setOnFinished(done -> {
                center.getChildren().remove(button);
                if (startButton == button) startButton = null;
            });
            hideButton.play();

            // Les commandes arrivent une fois le générateur allumé.
            controls.setOpacity(0);
            controls.setVisible(true);
            FadeTransition showControls = new FadeTransition(Duration.seconds(0.8), controls);
            showControls.setDelay(Duration.seconds(1.2));
            showControls.setToValue(1);
            showControls.play();
        });
        center.getChildren().add(button);
    }

    /**
     * Le joueur a confirmé qu'il recommence de zéro : le jeu efface tout, et la fenêtre revient à
     * l'écran de démarrage.
     */
    private void resetGame() {
        game.reset();
        exploding = false;
        explosionArmed = 0;
        blast.setVisible(false);
        lastTotalAtoms = -1;
        generators.setCount(0);
        darkMatterPage.reset();
        controls.setVisible(false);
        showStartButton();
        selectTab(Tab.PARTICLES);
        refresh();
    }

    /**
     * Bouton de fusion : il n'apparaît que lorsque tous les générateurs sont débloqués.
     * Les générateurs se regroupent au centre, puis le jeu applique la fusion.
     */
    private Button fuseButton() {
        fuseButton.setPrefWidth(BUTTON_WIDTH);
        fuseButton.setMinWidth(0);
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
            });
            refresh();
        });
        return fuseButton;
    }

    /**
     * Un clic sur le bouton d'explosion. Avec la confirmation des réglages, le premier clic arme
     * le bouton et c'est le second, dans les secondes qui suivent, qui déclenche l'explosion.
     */
    private void requestExplosion() {
        if (settings.confirmExplosion() && explosionArmed <= 0) {
            explosionArmed = EXPLOSION_CONFIRM_SECONDS;
            refresh();
            return;
        }
        explode();
    }

    /**
     * L'explosion du tableau périodique : une onde part du centre et la fenêtre vire au blanc ;
     * au plus fort de l'éclair, le jeu remet tout à zéro, puis l'écran revient sur l'onglet
     * « Matière noire ». La fenêtre ne réagit à aucun clic tant que l'explosion n'est pas finie.
     */
    private void explode() {
        if (exploding || !game.canExplode()) return;
        explosionArmed = 0;
        if (!settings.effects()) {
            // Sans effets visuels : pas d'onde ni d'éclair, l'explosion a lieu tout de suite.
            game.explode();
            selectTab(Tab.DARK_MATTER);
            refresh();
            return;
        }
        exploding = true;
        blast.setVisible(true);
        flash.setOpacity(0);
        shockwave.setVisible(true);

        ScaleTransition grow = new ScaleTransition(Duration.seconds(1.1), shockwave);
        grow.setFromX(0.1);
        grow.setFromY(0.1);
        grow.setToX(60);
        grow.setToY(60);
        grow.setInterpolator(Interpolator.EASE_IN); // départ lent, arrivée rapide
        FadeTransition whiten = new FadeTransition(Duration.seconds(0.7), flash);
        whiten.setDelay(Duration.seconds(0.4));
        whiten.setFromValue(0);
        whiten.setToValue(1);
        ParallelTransition bang = new ParallelTransition();
        bang.getChildren().add(grow);
        bang.getChildren().add(whiten);
        bang.setOnFinished(done -> {
            game.explode();
            shockwave.setVisible(false);
            selectTab(Tab.DARK_MATTER);
            refresh();

            FadeTransition clear = new FadeTransition(Duration.seconds(1.4), flash);
            clear.setFromValue(1);
            clear.setToValue(0);
            clear.setOnFinished(end -> {
                blast.setVisible(false);
                exploding = false;
            });
            clear.play();
        });
        bang.play();
    }

    /** Affiche un onglet et masque les autres. */
    private void selectTab(Tab tab) {
        selectedTab = tab;
        if (tab != Tab.DARK_MATTER) darkMatterPage.release();
        if (tab != Tab.SETTINGS) settingsPage.release();
        particlesPage.setVisible(tab == Tab.PARTICLES);
        atomsPage.setVisible(tab == Tab.ATOMS);
        automationScroll.setVisible(tab == Tab.AUTOMATION);
        darkMatterPage.setVisible(tab == Tab.DARK_MATTER);
        statsPage.setVisible(tab == Tab.STATS);
        settingsScroll.setVisible(tab == Tab.SETTINGS);
        styleTabs();
    }

    /** Colore les onglets : celui qui est affiché est éclairé, et tous se resserrent dans une fenêtre étroite. */
    private void styleTabs() {
        particlesTab.setStyle(tabStyle(PARTICLES_COLOR, selectedTab == Tab.PARTICLES));
        atomsTab.setStyle(tabStyle(ATOMS_COLOR, selectedTab == Tab.ATOMS));
        automationTab.setStyle(tabStyle(AUTOMATION_COLOR, selectedTab == Tab.AUTOMATION));
        darkMatterTab.setStyle(tabStyle(DARK_MATTER_COLOR, selectedTab == Tab.DARK_MATTER));
        statsTab.setStyle(tabStyle(STATS_COLOR, selectedTab == Tab.STATS));
        settingsTab.setStyle(tabStyle(SETTINGS_COLOR, selectedTab == Tab.SETTINGS));
        // Six onglets ne tiennent pas en toutes lettres dans une fenêtre étroite.
        automationTab.setText(narrow ? "Auto." : "Automatisation");
        statsTab.setText(narrow ? "Stats" : "Statistiques");
    }

    /** L'onglet affiché est souligné et éclairé dans sa couleur ; l'autre est estompé. */
    private String tabStyle(String color, boolean selected) {
        return TAB_STYLE + (narrow ? NARROW_TAB : WIDE_TAB) + (selected
                ? " -fx-text-fill: " + color + "; -fx-background-color: #16202e;"
                        + " -fx-border-color: transparent transparent " + color + " transparent;"
                : " -fx-text-fill: #8fa3b8; -fx-background-color: transparent; -fx-border-color: transparent;");
    }

    /** Un atome vient d'être créé : l'onglet « Atomes » bat quelques fois pour attirer l'œil. */
    private void celebrateNewAtom() {
        if (!settings.effects()) return;
        ScaleTransition notice = new ScaleTransition(Duration.seconds(0.35), atomsTab);
        notice.setFromX(1);
        notice.setFromY(1);
        notice.setToX(1.15);
        notice.setToY(1.15);
        notice.setAutoReverse(true);
        notice.setCycleCount(6);
        notice.play();
        atomsPage.celebrate();
    }

    /** Affiche les générateurs existants et fait avancer les animations de l'onglet visible. */
    private void animate(double dt) {
        // Moins de générateurs dans le jeu qu'à l'écran : une fusion automatique vient d'avoir lieu.
        // On joue la même animation que pour une fusion manuelle, puis les nouveaux générateurs naissent.
        if (Math.min(game.generatorCount(), MAX_VISIBLE_GENERATORS) < generators.views().size()
                && !generators.isFusing()) {
            generators.fuse(() -> { });
        }
        // Au-delà d'un certain nombre, les générateurs tournent sans être dessinés : ils deviendraient illisibles.
        generators.setCount(Math.min(game.generatorCount(), MAX_VISIBLE_GENERATORS));
        atomsTabIcon.frame(dt);
        if (selectedTab == Tab.ATOMS) {
            atomsPage.frame(dt);
        }
        if (selectedTab == Tab.DARK_MATTER) {
            darkMatterPage.frame(dt, timeFactor());
        }
        if (selectedTab != Tab.PARTICLES) {
            return;
        }
        if (settings.effects()) generators.animateBackground(dt);
        // Chaque générateur a sa propre vitesse : les éléments du tableau périodique en accélèrent certains.
        List<ParticleView> views = generators.views();
        for (int i = 0; i < views.size(); i++) {
            double speed = Math.min(game.speed(i).toDouble() * timeFactor(), 1e9);
            views.get(i).frame(dt, game.state().formation(i), speed);
        }
    }

    /** Accélération du temps : toujours 1 en jeu normal, réglable dans le profil de test. */
    private double timeFactor() {
        return debugBar == null ? 1 : debugBar.timeFactor();
    }

    /** Recopie l'état du jeu dans les composants. */
    private void refresh() {
        // Fenêtre étroite : les onglets se resserrent pour tenir sur une ligne.
        boolean nowNarrow = root.getWidth() > 0 && root.getWidth() < NARROW_WINDOW;
        if (nowNarrow != narrow) {
            narrow = nowNarrow;
            styleTabs();
        }

        BigNum particles = game.state().particles();
        particlesLabel.setText(Format.count(particles) + (particles.gt(BigNum.ONE) ? " particules" : " particule"));

        double rate = Math.min(game.productionPerSecond().toDouble(), 1e9);
        String perSecond = rate <= 0 ? "Aucun générateur"
                : rate < 1
                ? String.format(Locale.ROOT, "1 particule toutes les %.1f s", 1 / rate)
                : "+" + Format.big(game.productionPerSecond()) + " particules par seconde";
        int hidden = game.generatorCount() - MAX_VISIBLE_GENERATORS;
        productionLabel.setText(perSecond + "   |   " + Format.perMinute(game.productionPerSecond()) + " p/m"
                + (hidden > 0 ? "   |   " + game.generatorCount() + " générateurs (" + hidden + " non dessinés)" : ""));

        upgradeButtons.forEach((upgrade, button) -> {
            button.setText(label(upgrade));
            button.setDisable(!game.canBuy(upgrade.id()));
        });

        // Au plafond d'atomes, le bouton reste affiché mais grisé, pour expliquer pourquoi rien ne se passe.
        boolean capped = game.isAtomCapReached();
        fuseButton.setText(capped
                ? "Maximum de " + Format.count(Game.MAX_ATOMS) + " atomes atteint : dépensez-en"
                        + (game.isPeriodicTableUnlocked() && !game.isPeriodicTableComplete()
                                ? " ou synthétisez un élément" : " pour fusionner")
                : (game.generatorCount() > game.generatorsPerAtom()
                        ? "Fusionner " + game.generatorCount() + " générateurs : +"
                        : "Fusionner les " + game.generatorsPerAtom() + " générateurs : +")
                        + Format.amount(game.atomsPerFusion()) + (game.atomsPerFusion().gt(BigNum.ONE) ? " atomes" : " atome"));
        fuseButton.setDisable(capped);
        fuseButton.setVisible(game.hasAllGenerators() && !generators.isFusing());

        // Un atome de plus qu'à l'image précédente, par fusion manuelle ou automatique.
        double totalAtoms = game.state().totalAtoms().toDouble();
        if (lastTotalAtoms >= 0 && totalAtoms > lastTotalAtoms) {
            celebrateNewAtom();
        }
        lastTotalAtoms = totalAtoms;

        // Après une explosion, les atomes ont disparu mais l'onglet « Matière noire » reste.
        // L'onglet « Atomes » n'existe qu'à partir du premier atome.
        boolean hasAtoms = game.state().totalAtoms().sign() > 0;
        boolean darkMatter = game.isDarkMatterUnlocked();
        atomsTab.setVisible(hasAtoms);
        atomsTab.setManaged(hasAtoms);
        atomsTab.setText("Atomes (" + Format.count(game.state().atoms()) + ")");
        atomsPage.refresh();

        // L'onglet « Automatisation » n'existe qu'une fois un automatisme débloqué, ordinaire ou de matière noire.
        boolean automation = AutomationPage.hasContent(game);
        automationTab.setVisible(automation);
        automationTab.setManaged(automation);
        automationPage.refresh();

        // L'onglet « Matière noire » n'existe qu'après la première explosion.
        darkMatterTab.setVisible(darkMatter);
        darkMatterTab.setManaged(darkMatter);
        darkMatterTab.setText((narrow ? "M. noire (" : "Matière noire (") + Format.count(game.state().darkMatter()) + ")");
        darkMatterPage.refresh();

        // Les statistiques ne se recalculent que lorsqu'on les regarde.
        if (selectedTab == Tab.STATS) statsPage.refresh();
        settingsTab.setText(!settings.paused() ? "Réglages" : narrow ? "En pause" : "Réglages (pause)");

        // Un onglet qui vient de disparaître (après une explosion) ne peut pas rester affiché.
        if ((selectedTab == Tab.ATOMS && !hasAtoms) || (selectedTab == Tab.AUTOMATION && !automation)
                || (selectedTab == Tab.DARK_MATTER && !darkMatter)) {
            selectTab(Tab.PARTICLES);
        }

        // Le bouton d'explosion n'apparaît que lorsque le tableau périodique est complet.
        explosionButton.setText(explosionArmed > 0
                ? "Cliquer encore pour confirmer l'explosion : tout repart de zéro (" + (int) Math.ceil(explosionArmed) + " s)"
                : "Faire exploser le tableau périodique : tout repart de zéro, +"
                        + Format.count(game.darkMatterPerExplosion()) + " matière noire");
        boolean canExplode = game.canExplode() && !exploding;
        explosionButton.setVisible(canExplode);
        explosionButton.setManaged(canExplode);
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
