package idle.ui;

import idle.core.Absence;
import idle.core.BigNum;
import idle.core.Challenge;
import idle.core.Effect;
import idle.core.Game;
import idle.core.Resource;
import idle.core.SaveCodec;
import idle.core.Upgrade;
import java.util.EnumMap;
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
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.TextAlignment;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * La fenêtre du jeu : les générateurs animés, le compteur de particules,
 * la production, et une carte par amélioration.
 *
 * <p>Une partie neuve s'ouvre sur un écran de démarrage : un fond de poussières et un
 * bouton qui crée le premier générateur. Quand tous les générateurs sont débloqués,
 * un bouton permet de les fusionner en un atome.
 *
 * <p>En haut de la fenêtre, des onglets. Quatre sont là dès le début : « Particules » (les
 * générateurs et leurs améliorations), « Succès » ({@link AchievementsPage}), « Statistiques »
 * ({@link StatsPage}) et « Réglages » ({@link SettingsPage}). Les autres apparaissent avec la partie : « Atomes » ({@link AtomsPage})
 * au premier atome, « Automatisation » ({@link AutomationPage}) dès qu'un automatisme est
 * débloqué. Le jeu continue de tourner quel que soit l'onglet affiché, sauf en pause.
 *
 * <p>Quand les 118 éléments du tableau périodique sont découverts, un bouton apparaît au-dessus des onglets : il
 * déclenche une explosion qui remet toute la partie à zéro et fait apparaître l'onglet
 * « Matière noire » ({@link DarkMatterPage}). Au bout de son arbre, le Big Bang efface tout de
 * nouveau et fait apparaître l'onglet « Big Bang » ({@link BigBangPage}).
 *
 * <p>Lancé avec l'argument {@code --test} (tâche Gradle {@code runTest}), le jeu affiche en
 * plus une {@link DebugBar} pour accélérer le temps et s'ajouter des ressources.
 *
 * <p>Les améliorations payées en particules s'achètent par un, par dix ou « au maximum »
 * (réglage sous le compteur), au clic ou au clavier : voir {@link #onKey(KeyCode)}. Chacune est
 * une {@link Card} : son niveau, sa touche, ce qu'elle rapporte en quelques mots, son prix et le
 * temps qu'il reste à attendre.
 *
 * <p>Partout dans le jeu, les explications complètes n'apparaissent qu'en mode détails
 * ({@link Detail}) : tant que le joueur tient la touche de détail, réglable ({@link Settings#detailKey()}).
 * Un rappel de cette touche reste affiché sous les onglets.
 *
 * <p>Sous les onglets, une ligne rappelle l'objectif du moment ({@link Goals}). Par-dessus tout
 * le reste, les notifications ({@link ToastPane}) annoncent ce que {@link Notifier} a repéré :
 * nouvel élément, succès, déblocage. Toute la fenêtre passe par {@link ScaledPane} (la taille de
 * l'interface) et, pour le thème clair, par {@link LightTheme}.
 *
 * <p>La partie est sauvegardée dans un fichier ({@link SaveStore}) : relue au lancement, écrite
 * toutes les trente secondes et à la fermeture. Au retour du joueur, une part du temps passé jeu
 * fermé est rejouée ({@link Absence}), sous un panneau qui en montre l'avancée puis le bilan
 * ({@link AbsencePane}). La fenêtre retrouve aussi sa place, sa taille et son onglet.
 *
 * <p>La fenêtre ne contient aucune règle : elle appelle {@code game.tick()},
 * affiche l'état, et transmet les clics à {@code game.buy()}.
 */
public final class GameApp extends Application {

    /** Largeur maximale de la rangée des améliorations, et de la carte de fusion en dessous. */
    private static final double UPGRADES_WIDTH = 760;
    private static final double FUSE_WIDTH = 380;
    /** Style des trois boutons de quantité ; celui qui est choisi est éclairé. */
    private static final String AMOUNT_STYLE = "-fx-font-size: 12px; -fx-padding: 3 12; -fx-cursor: hand;"
            + " -fx-background-radius: 6; -fx-border-radius: 6;";
    /** Nombre maximal de générateurs dessinés ; les suivants produisent sans être affichés. */
    private static final int MAX_VISIBLE_GENERATORS = 30;

    /** Style commun aux onglets ; la couleur dépend de l'onglet et de son état, les marges de la largeur de la fenêtre. */
    private static final String TAB_STYLE = "-fx-cursor: hand; -fx-background-radius: 0; -fx-border-width: 0 0 2 0;";
    private static final String WIDE_TAB = " -fx-font-size: 14px; -fx-padding: 10 11;";
    private static final String NARROW_TAB = " -fx-font-size: 12px; -fx-padding: 8 10;";
    /** En dessous de cette largeur de fenêtre, les onglets se resserrent pour tenir sur une ligne. */
    private static final double NARROW_WINDOW = 780;
    /** La même limite une fois l'onglet « Big Bang » apparu : huit onglets au lieu de sept. */
    private static final double NARROW_WINDOW_WITH_BIG_BANG = 920;
    static final String PARTICLES_COLOR = "#9fd0ff";
    static final String ATOMS_COLOR = "#ffd27f";
    static final String AUTOMATION_COLOR = "#9be7a8";
    static final String DARK_MATTER_COLOR = "#c9a6ff";
    /** La couleur du Big Bang : le blanc chaud d'un éclair, à part des quatre couleurs de ressources. */
    static final String BIG_BANG_COLOR = "#fff1c9";
    static final String ACHIEVEMENTS_COLOR = "#ff9eb5";
    static final String STATS_COLOR = "#7fe0d4";
    private static final String SETTINGS_COLOR = "#d6dde6";
    /** Temps laissé pour le second clic qui confirme l'explosion, en secondes. */
    private static final double EXPLOSION_CONFIRM_SECONDS = 5;

    /** Les onglets du jeu. */
    private enum Tab { PARTICLES, ATOMS, AUTOMATION, DARK_MATTER, BIG_BANG, ACHIEVEMENTS, STATS, SETTINGS }

    private final Game game = new Game();
    private final Settings settings = Settings.load();
    /**
     * Le fichier de sauvegarde. Celui que désignent les arguments du jeu n'est connu qu'au
     * lancement ({@link #start(Stage)}) : d'ici là, rien ne s'écrit. Déclaré avant les pages, qui
     * l'interrogent dès leur construction.
     */
    private SaveStore store = SaveStore.of(List.of("--sans-sauvegarde"));
    private final GeneratorPane generators = new GeneratorPane();
    private final Label particlesLabel = new Label();
    private final Label productionLabel = new Label();
    /** Au survol de la production : d'où elle vient, un facteur par ligne. */
    private final Breakdown productionBreakdown = Breakdown.attach(productionLabel, game, Breakdown.Of.PARTICLES);
    private final Card fuseCard = new Card(ATOMS_COLOR);

    // Les onglets, visibles à partir du premier atome.
    private final Button particlesTab = new Button("Particules");
    private final Button atomsTab = new Button();
    private final Button automationTab = new Button("Automatisation");
    private final Button darkMatterTab = new Button();
    private final Button bigBangTab = new Button("Big Bang");
    private final Button achievementsTab = new Button("Succès");
    private final Button statsTab = new Button("Statistiques");
    private final Button settingsTab = new Button("Réglages");
    private final AtomView atomsTabIcon = new AtomView(22);
    private final HBox tabBar = new HBox(particlesTab, atomsTab, automationTab, darkMatterTab, bigBangTab,
            achievementsTab, statsTab, settingsTab);
    private final BorderPane particlesPage = new BorderPane();
    private final AtomsPage atomsPage = new AtomsPage(game, settings);
    private final AutomationPage automationPage = new AutomationPage(game);
    private final DarkMatterPage darkMatterPage = new DarkMatterPage(game);
    private final BigBangPage bigBangPage = new BigBangPage(game);
    private final AchievementsPage achievementsPage = new AchievementsPage(game);
    private final StatsPage statsPage = new StatsPage(game);
    private final SettingsPage settingsPage = new SettingsPage(settings, this::resetGame, this::applyAppearance, new SaveActions());
    /** La page des automatismes défile quand la fenêtre est trop basse pour toutes ses cartes. */
    private final ScrollPane automationScroll = new ScrollPane(automationPage);
    private final ScrollPane settingsScroll = new ScrollPane(settingsPage);
    private final ScrollPane achievementsScroll = new ScrollPane(achievementsPage);
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
    /** Rappel du défi en cours, entre le bouton d'explosion et les onglets. */
    private final Label challengeLabel = new Label();
    /** L'objectif du moment, sous les onglets. */
    private final Label goalLabel = new Label();
    /** Le rappel de la touche de détail, à droite de l'objectif. */
    private final Label detailHint = new Label();
    /** Vrai tant que la touche de détail est enfoncée : pour ne pas basculer à chaque répétition du clavier. */
    private boolean detailKeyDown = false;
    // Les notifications, par-dessus tout le reste, et ce qui repère les nouvelles à annoncer.
    private final ToastPane toasts = new ToastPane();
    private final Notifier notifier = new Notifier(game);
    // L'apparence : la taille de l'interface et le thème clair s'appliquent à toute la fenêtre.
    private ScaledPane scaled;
    private LightTheme lightTheme;
    private final StackPane flash = new StackPane();
    private final Circle shockwave = new Circle(24, Color.web(DARK_MATTER_COLOR));
    private final StackPane blast = new StackPane(flash, shockwave);
    private boolean exploding = false;
    /** Secondes restantes pour confirmer l'explosion par un second clic ; 0 quand elle n'est pas demandée. */
    private double explosionArmed = 0;
    /** Atomes créés à la dernière image, pour repérer l'arrivée d'un nouvel atome ; −1 avant la première image. */
    private double lastTotalAtoms = -1;
    /** Le nombre de Big Bangs au dernier affichage, pour remarquer qu'il vient d'y en avoir un ; −1 avant le premier affichage. */
    private int lastBigBangs = -1;
    /** Barre du profil de test, ou {@code null} en jeu normal. */
    private DebugBar debugBar;
    // La sauvegarde : le temps écoulé depuis la dernière écriture, et le retour après une absence.
    private double sinceSave = 0;
    private final AbsencePane absencePane = new AbsencePane(this::save);
    /** Temps réel donné au rattrapage d'une absence à chaque image, en nanosecondes : assez peu pour que la fenêtre reste vive. */
    private static final long CATCH_UP_NANOS = 10_000_000;
    private Stage stage;
    private boolean closed = false;
    private final Map<Upgrade, Card> upgradeCards = new LinkedHashMap<>();
    /** Les améliorations côte à côte ; dans une fenêtre étroite, elles se rangent sur plusieurs rangées. */
    private final TileGrid upgradeGrid = new TileGrid(170, 4, 8);
    /** Les trois quantités d'achat : ×1, ×10, max. */
    private final Map<Settings.BuyAmount, Button> amountButtons = new EnumMap<>(Settings.BuyAmount.class);
    /** Raccourci → amélioration payée en particules qu'il achète. */
    private final Map<Shortcuts.Action, Upgrade> upgradeActions = new EnumMap<>(Shortcuts.Action.class);
    /** L'aide « Comment jouer », par-dessus la fenêtre, et le bouton qui l'ouvre, sous les onglets. */
    private final HelpPane helpPane = new HelpPane(game, settings);
    private final Button helpButton = new Button();
    /** Ce qui est prêt quelque part, pour la pastille des onglets. */
    private final Readiness readiness = Readiness.of(game);
    private final Label shortcutsLabel = new Label();

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        // Une erreur imprévue ne ferme pas le jeu : elle est gardée pour le rapport de bug, et écrite dans la console.
        Thread.currentThread().setUncaughtExceptionHandler((thread, error) -> {
            Crash.note(error);
            error.printStackTrace();
        });
        // La partie sauvegardée est reprise avant que rien ne s'affiche.
        List<String> arguments = getParameters().getRaw();
        store = SaveStore.of(arguments);
        SaveStore.Loaded loaded = store.load(game);

        particlesLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #e8f4ff;");
        productionLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #8fa3b8;");

        // En bas : le compteur et les améliorations. Cachés tant que la partie n'a pas démarré,
        // mais leur place est réservée pour que rien ne bouge à leur apparition.
        controls.setVisible(game.isStarted());
        controls.setPadding(new Insets(8, 24, 12, 24));
        controls.setSpacing(8);
        controls.setAlignment(Pos.TOP_CENTER);

        // La quantité achetée à chaque clic : un niveau, dix, ou tout ce qui est à portée.
        Label amountLabel = new Label("Quantité par achat :");
        amountLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #8fa3b8;");
        HBox amounts = new HBox(6, amountLabel);
        amounts.setAlignment(Pos.CENTER);
        for (Settings.BuyAmount amount : Settings.BuyAmount.values()) {
            Button button = new Button(amount.label());
            button.setFocusTraversable(false);
            button.setOnAction(event -> {
                settings.setBuyAmount(amount);
                refresh();
            });
            amountButtons.put(amount, button);
            amounts.getChildren().add(button);
        }
        controls.getChildren().add(amounts);

        // Une carte par amélioration payée en particules : en ajouter une dans core suffit à la faire apparaître.
        for (Upgrade upgrade : game.upgrades(Resource.PARTICLES)) {
            Card card = new Card(PARTICLES_COLOR);
            card.setOnAction(() -> {
                game.buy(upgrade.id(), settings.buyAmount().count());
                refresh();
            });
            upgradeCards.put(upgrade, card);
            upgradeGrid.add(card);
            Shortcuts.Action action = actionOf(upgrade);
            if (action != null) upgradeActions.putIfAbsent(action, upgrade);
        }
        upgradeGrid.setMaxWidth(UPGRADES_WIDTH);
        controls.getChildren().add(upgradeGrid);
        controls.getChildren().add(fuseCard());
        shortcutsLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #6f8296;");
        shortcutsLabel.setWrapText(true);
        shortcutsLabel.setTextAlignment(TextAlignment.CENTER);
        controls.getChildren().add(shortcutsLabel);

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
        achievementsScroll.setFitToWidth(true);
        achievementsScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        achievementsScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        StackPane pages = new StackPane(particlesPage, atomsPage, automationScroll, darkMatterPage, bigBangPage,
                achievementsScroll, statsPage, settingsScroll);
        particlesTab.setOnAction(event -> open(Tab.PARTICLES));
        atomsTab.setOnAction(event -> open(Tab.ATOMS));
        automationTab.setOnAction(event -> open(Tab.AUTOMATION));
        darkMatterTab.setOnAction(event -> open(Tab.DARK_MATTER));
        bigBangTab.setOnAction(event -> open(Tab.BIG_BANG));
        achievementsTab.setOnAction(event -> open(Tab.ACHIEVEMENTS));
        statsTab.setOnAction(event -> open(Tab.STATS));
        settingsTab.setOnAction(event -> open(Tab.SETTINGS));
        atomsTab.setGraphic(atomsTabIcon);
        tabBar.setAlignment(Pos.CENTER);
        selectTab(Tab.PARTICLES);

        // En haut : la barre d'onglets, et au-dessus la barre du profil de test si le jeu est lancé avec --test.
        // Au-dessus des onglets : le bouton d'explosion, quand tous les éléments sont découverts.
        explosionButton.setMaxWidth(Double.MAX_VALUE);
        explosionButton.setWrapText(true);
        explosionButton.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-padding: 10 20; -fx-cursor: hand;"
                + " -fx-text-fill: #1a0f2e; -fx-background-color: " + DARK_MATTER_COLOR + "; -fx-background-radius: 0;");
        explosionButton.setOnAction(event -> requestExplosion());
        challengeLabel.setMaxWidth(Double.MAX_VALUE);
        challengeLabel.setWrapText(true);
        challengeLabel.setAlignment(Pos.CENTER);
        challengeLabel.setTextAlignment(TextAlignment.CENTER);
        challengeLabel.setStyle("-fx-font-size: 12px; -fx-padding: 5 16; -fx-text-fill: #efe4ff; -fx-background-color: #2a2040;");
        goalLabel.setMaxWidth(Double.MAX_VALUE);
        goalLabel.setMinWidth(0);
        goalLabel.setWrapText(true);
        goalLabel.setAlignment(Pos.CENTER);
        goalLabel.setTextAlignment(TextAlignment.CENTER);
        goalLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #8fa3b8;");
        HBox.setHgrow(goalLabel, Priority.ALWAYS);
        // Sous les onglets : l'objectif du moment, et à droite le rappel de la touche de détail.
        helpButton.setFocusTraversable(false);
        helpButton.setOnAction(event -> {
            helpPane.toggle();
            refresh();
        });
        HBox infoBar = new HBox(10, goalLabel, helpButton, detailHint);
        infoBar.setAlignment(Pos.CENTER);
        infoBar.setStyle("-fx-padding: 3 12; -fx-background-color: #0e131b;");
        VBox top = new VBox(explosionButton, challengeLabel, tabBar, infoBar);
        boolean testProfile = arguments.contains("--test");
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
        StackPane window = new StackPane(root, blast, toasts, helpPane, absencePane);
        StackPane.setAlignment(toasts, Pos.BOTTOM_RIGHT);
        scaled = new ScaledPane(window);
        lightTheme = new LightTheme(window);

        // Boucle de jeu : appelée à chaque image (~60 fois par seconde) sur le thread JavaFX.
        new AnimationTimer() {
            private long previous = 0;

            @Override
            public void handle(long now) {
                double dt = previous == 0 ? 0 : (now - previous) / 1e9; // nanosecondes → secondes
                previous = now;
                // Au retour d'une absence, le jeu rattrape d'abord le temps perdu : rien d'autre n'avance.
                if (absencePane.isCatchingUp()) {
                    catchUp();
                    return;
                }
                sinceSave += dt;
                if (sinceSave >= SaveStore.EVERY_SECONDS) save();
                // En pause, le temps du jeu s'arrête ; celui de l'interface continue (confirmations, réglages).
                if (!settings.paused()) {
                    game.tick(dt * timeFactor());
                    animate(dt);
                }
                // Le clic tenu sur une carte qui se répète (une molécule, une fois la création continue acquise).
                Card.repeatHeld(dt);
                explosionArmed = Math.max(0, explosionArmed - dt);
                if (selectedTab == Tab.SETTINGS) settingsPage.frame(dt);
                announce(dt);
                refresh();
            }
        }.start();

        // L'onglet de la dernière fois, s'il existe encore dans cette partie : sinon refresh() revient aux particules.
        try {
            if (loaded.found() && !settings.lastTab().isEmpty()) selectTab(Tab.valueOf(settings.lastTab()));
        } catch (IllegalArgumentException unknown) {
            // un onglet d'une autre version du jeu : on reste sur le premier
        }
        animate(0);
        refresh();
        if (!loaded.notice().isEmpty()) toasts.show(loaded.notice(), SETTINGS_COLOR);
        if (loaded.found()) {
            Absence absence = new Absence(game, (System.currentTimeMillis() - loaded.savedAt()) / 1000.0);
            if (!absence.isDone()) absencePane.open(absence);
        }
        stage.setTitle(testProfile ? "Idle [profil de test]" : "Idle");
        Scene scene = new Scene(scaled, 800, 640);
        applyAppearance();
        // Les touches sont lues avant les boutons : sinon, Espace irait d'abord au bouton qui a le focus.
        // Sauf quand le joueur écrit dans une barre de recherche : ses lettres ne sont pas des raccourcis.
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (typing(scene) && !event.getCode().isModifierKey()) return;
            if (keyPressed(event.getCode())) event.consume();
        });
        scene.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
            if (typing(scene) && !event.getCode().isModifierKey()) return;
            if (keyReleased(event.getCode())) event.consume();
        });
        // La fenêtre perd la main touche enfoncée : le relâchement ne viendra jamais, on l'anticipe.
        stage.focusedProperty().addListener((observable, before, focused) -> {
            if (!focused) releaseDetailKey();
        });
        stage.setScene(scene);
        stage.setMinWidth(480);
        stage.setMinHeight(460);
        placeWindow();
        stage.setOnCloseRequest(event -> closing());
        stage.show();
    }

    /** Le jeu se ferme : la partie et la place de la fenêtre sont écrites une dernière fois. */
    @Override
    public void stop() {
        closing();
    }

    private void closing() {
        if (closed) return;
        closed = true;
        save();
        settings.setLastTab(selectedTab.name());
        if (stage == null) return;
        // Agrandie au maximum, la fenêtre garde en mémoire la place qu'elle avait avant.
        double[] before = settings.window();
        if (stage.isMaximized() && before != null) {
            settings.setWindow(before[0], before[1], before[2], before[3], true);
        } else {
            settings.setWindow(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight(), stage.isMaximized());
        }
    }

    /** Remet la fenêtre où elle était à la dernière fermeture, si cet endroit est encore sur un écran. */
    private void placeWindow() {
        double[] place = settings.window();
        if (place == null) return;
        try {
            if (Screen.getScreensForRectangle(place[0], place[1], place[2], place[3]).isEmpty()) return;
        } catch (RuntimeException unavailable) {
            return;     // pas d'écran à interroger : on garde la place par défaut
        }
        stage.setX(place[0]);
        stage.setY(place[1]);
        stage.setWidth(Math.max(480, place[2]));
        stage.setHeight(Math.max(460, place[3]));
        if (place[4] == 1) stage.setMaximized(true);
    }

    /** Écrit la partie dans son fichier. */
    private void save() {
        sinceSave = 0;
        store.save(game);
    }

    /**
     * Une image du rattrapage d'une absence : le jeu rejoue autant de temps qu'il peut sans figer
     * la fenêtre. Ce qui se passe pendant ce temps n'est pas annoncé message par message : le
     * panneau en fait le bilan à la fin.
     */
    private void catchUp() {
        absencePane.absence().advanceFor(CATCH_UP_NANOS);
        notifier.poll();
        // Arrivé au bout, le panneau fait écrire la partie, et se ferme s'il n'a rien à annoncer.
        absencePane.refresh();
        refresh();
    }

    /**
     * La partie vient d'être remplacée par une autre (importée) : la fenêtre oublie ce qu'elle
     * avait retenu de la précédente, comme après une remise à zéro.
     */
    private void afterLoad() {
        toasts.clear();
        absencePane.close();
        exploding = false;
        explosionArmed = 0;
        blast.setVisible(false);
        lastTotalAtoms = -1;
        lastBigBangs = -1;
        generators.setCount(0);
        darkMatterPage.reset();
        bigBangPage.reset();
        notifier.forget();
        EventLog.of(game).clear();
        if (game.isStarted()) {
            if (startButton != null) center.getChildren().remove(startButton);
            startButton = null;
            controls.setOpacity(1);
            controls.setVisible(true);
        } else {
            controls.setVisible(false);
            showStartButton();
        }
        refresh();
    }

    /** Ce que la page des réglages demande pour la sauvegarde : chaque geste rend la phrase qui dit ce qu'il a donné. */
    private final class SaveActions implements SettingsPage.Saves {

        @Override
        public String status() {
            return store.status();
        }

        @Override
        public String saveNow() {
            save();
            return store.problem().isEmpty() ? (store.enabled() ? "Partie sauvegardée." : "Jeu lancé sans sauvegarde.")
                    : "La sauvegarde a échoué : " + store.problem();
        }

        @Override
        public String copyExport() {
            String text = store.export(game);
            return copy(text) ? "Partie copiée dans le presse-papiers (" + Format.whole(text.length()) + " caractères)."
                    : "Le presse-papiers n'est pas accessible.";
        }

        @Override
        public String pasteImport() {
            String text;
            try {
                text = Clipboard.getSystemClipboard().getString();
            } catch (RuntimeException unavailable) {
                return "Le presse-papiers n'est pas accessible.";
            }
            try {
                int dropped = store.importText(game, text);
                afterLoad();
                return "Partie importée : " + Format.duration(game.state().timePlayed()) + " de jeu."
                        + (dropped > 0 ? " " + dropped + " chose(s) que ce jeu ne connaît pas ont été écartées." : "");
            } catch (SaveCodec.Unreadable unreadable) {
                return "Rien n'a changé. " + unreadable.getMessage();
            }
        }

        @Override
        public String records() {
            int known = 0;
            for (idle.core.GameStats.Step step : idle.core.GameStats.Step.values()) {
                if (!Double.isNaN(game.recordOf(step))) known++;
            }
            if (known == 0) return "Aucun record pour l'instant : le premier s'inscrit à la première fusion.";
            idle.core.GameStats.Step next = game.nextStep();
            return known + (known > 1 ? " étapes ont un record" : " étape a un record")
                    + (next == null || Double.isNaN(game.recordOf(next)) ? "."
                    : ". Prochaine : " + next.label() + ", " + Format.duration(game.recordOf(next)) + ".");
        }

        @Override
        public void clearRecords() {
            game.clearRecords();
            store.save(game);
        }

        @Override
        public String copyBugReport() {
            return copy(BugReport.of(game, store)) ? "Rapport de bug copié dans le presse-papiers : il n'y a plus qu'à le coller."
                    : "Le presse-papiers n'est pas accessible.";
        }

        private boolean copy(String text) {
            try {
                ClipboardContent content = new ClipboardContent();
                content.putString(text);
                return Clipboard.getSystemClipboard().setContent(content);
            } catch (RuntimeException unavailable) {
                return false;
            }
        }
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
        game.restored();
        save();
        absencePane.close();
        helpPane.close();
        toasts.clear();
        exploding = false;
        explosionArmed = 0;
        blast.setVisible(false);
        lastTotalAtoms = -1;
        generators.setCount(0);
        darkMatterPage.reset();
        bigBangPage.reset();
        lastBigBangs = 0;
        controls.setVisible(false);
        showStartButton();
        selectTab(Tab.PARTICLES);
        refresh();
    }

    /**
     * Carte de fusion : elle n'apparaît que lorsque tous les générateurs sont débloqués.
     * Les générateurs se regroupent au centre, puis le jeu applique la fusion.
     */
    private Card fuseCard() {
        fuseCard.setPrefWidth(FUSE_WIDTH);
        fuseCard.setMaxWidth(FUSE_WIDTH);
        fuseCard.setVisible(false); // sa place reste réservée pour que rien ne bouge à son apparition

        ScaleTransition pulse = new ScaleTransition(Duration.seconds(1.1), fuseCard);
        pulse.setFromX(1);
        pulse.setFromY(1);
        pulse.setToX(1.04);
        pulse.setToY(1.04);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.play();

        fuseCard.setOnAction(this::fuse);
        return fuseCard;
    }

    /** La fusion demandée par le joueur, au bouton ou au clavier : les générateurs se regroupent, puis le jeu fusionne. */
    private void fuse() {
        if (!game.canFuse() || generators.isFusing()) return;
        if (selectedTab != Tab.PARTICLES) {
            game.fuse();     // l'onglet n'est pas affiché : personne ne verrait l'animation
            refresh();
            return;
        }
        generators.fuse(() -> {
            game.fuse(); // un seul générateur renaît à l'image suivante, là où les autres ont fusionné
            refresh();   // fait apparaître les onglets si c'est le premier atome
        });
        refresh();
    }

    /** Le raccourci qui achète cette amélioration, ou {@code null} si elle n'en a pas. */
    private static Shortcuts.Action actionOf(Upgrade upgrade) {
        return switch (upgrade.effect()) {
            case Effect.MultiplySpeed speed -> Shortcuts.Action.SPEED;
            case Effect.MultiplyByGenerators coupling -> Shortcuts.Action.COUPLING;
            case Effect.AddGenerator generator -> Shortcuts.Action.GENERATOR;
            default -> null;
        };
    }

    /** Touche du clavier qui achète cette amélioration, ou {@code null} si elle n'en a pas. */
    private KeyCode keyOf(Upgrade upgrade) {
        Shortcuts.Action action = actionOf(upgrade);
        return action == null ? null : settings.key(action);
    }

    /**
     * Une touche vient d'être enfoncée. Dans l'ordre : la page des réglages l'attend peut-être
     * pour en faire la touche de détail ; sinon c'est la touche de détail elle-même ; sinon un
     * raccourci.
     *
     * @return vrai si la touche a servi à l'interface et ne doit pas aller plus loin
     */
    /**
     * Vrai si le clavier est dans une zone de saisie visible : une barre de recherche. Une zone
     * cachée avec sa page ne compte pas, même si la fenêtre lui a laissé le clavier.
     */
    private static boolean typing(Scene scene) {
        javafx.scene.Node owner = scene.getFocusOwner();
        if (!(owner instanceof javafx.scene.control.TextInputControl)) return false;
        for (javafx.scene.Node node = owner; node != null; node = node.getParent()) {
            if (!node.isVisible()) return false;
        }
        return true;
    }

    private boolean keyPressed(KeyCode code) {
        if (settingsPage.isCapturing()) {
            settingsPage.capture(code);
            return true;
        }
        // L'aide ouverte prend le clavier : Échap ou son raccourci la ferment, le reste attend.
        if (helpPane.isOpen() && code != settings.detailKey()) {
            if (code == KeyCode.ESCAPE || code == settings.key(Shortcuts.Action.HELP)) {
                helpPane.close();
                refresh();
            }
            return true;
        }
        if (code == settings.detailKey()) {
            // Tenue, la touche se répète : seul le premier appui compte.
            if (!detailKeyDown) {
                detailKeyDown = true;
                Detail.set(settings.detailToggle() ? !Detail.shown() : true);
                refresh();
            }
            return true;
        }
        return onKey(code);
    }

    /** Une touche vient d'être relâchée : si c'est la touche de détail et qu'elle se tient, les détails se referment. */
    private boolean keyReleased(KeyCode code) {
        if (code != settings.detailKey()) return false;
        releaseDetailKey();
        return true;
    }

    private void releaseDetailKey() {
        detailKeyDown = false;
        if (!settings.detailToggle() && Detail.shown()) {
            Detail.set(false);
            refresh();
        }
    }

    /**
     * Les raccourcis clavier ({@link Shortcuts}), actifs depuis n'importe quel onglet, avec les
     * touches que le joueur leur a données ({@link Settings#key}). Un raccourci dont l'action
     * n'est pas possible en ce moment ne fait rien.
     *
     * @return vrai si la touche portait un raccourci : elle ne va alors pas plus loin
     */
    private boolean onKey(KeyCode code) {
        Shortcuts.Action action = settings.actionOf(code);
        if (action == null || !settings.shortcuts() || exploding || absencePane.isOpen()) return false;
        switch (action) {
            case PAUSE -> {
                settings.setPaused(!settings.paused());
                settingsPage.refresh();
            }
            case HELP -> helpPane.toggle();
            case PREVIOUS_TAB -> stepTab(-1);
            case NEXT_TAB -> stepTab(1);
            case PREVIOUS_SUB_TAB -> stepSubTab(-1);
            case NEXT_SUB_TAB -> stepSubTab(1);
            default -> {
                if (game.isStarted()) play(action);
            }
        }
        refresh();
        return true;
    }

    /** Les raccourcis qui jouent : acheter, fusionner, synthétiser, exploser, couper les automatismes. */
    private void play(Shortcuts.Action action) {
        switch (action) {
            case FUSE -> fuse();
            case BUY_ALL -> game.buyAllWithParticles();
            case SYNTHESIZE -> game.synthesize();
            case EXPLODE -> {
                if (game.canExplode()) requestExplosion();
            }
            case BIG_BANG -> {
                // Le Big Bang se déclenche sur sa case, au bout de l'arbre : le raccourci y mène et la presse.
                if (game.canBigBang()) {
                    selectTab(Tab.DARK_MATTER);
                    darkMatterPage.pressBigBang();
                }
            }
            case AUTOMATION -> {
                if (game.hasAnyAutomation()) game.setAutomationPaused(!game.isAutomationPaused());
            }
            default -> {
                Upgrade upgrade = upgradeActions.get(action);
                if (upgrade != null) game.buy(upgrade.id(), settings.buyAmount().count());
            }
        }
    }

    /** Passe à l'onglet visible suivant ou précédent, en boucle. */
    private void stepTab(int direction) {
        Button[] buttons = {particlesTab, atomsTab, automationTab, darkMatterTab, bigBangTab, achievementsTab, statsTab, settingsTab};
        Tab[] tabs = Tab.values();
        int index = selectedTab.ordinal();
        for (int tries = 0; tries < tabs.length; tries++) {
            index = Math.floorMod(index + direction, tabs.length);
            if (buttons[index].isVisible()) {
                selectTab(tabs[index]);
                return;
            }
        }
    }

    /** Passe au sous-onglet suivant ou précédent de l'onglet affiché, s'il en a. */
    private void stepSubTab(int direction) {
        switch (selectedTab) {
            case ATOMS -> atomsPage.step(direction);
            case DARK_MATTER -> darkMatterPage.step(direction);
            case BIG_BANG -> bigBangPage.step(direction);
            case STATS -> statsPage.step(direction);
            default -> { }
        }
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

    /** Applique la taille de l'interface et le thème choisis dans les réglages. */
    private void applyAppearance() {
        if (scaled == null) return;      // la fenêtre n'est pas encore construite
        scaled.setScale(settings.scale().factor());
        lightTheme.apply(settings.theme() == Settings.Theme.LIGHT);
    }

    /**
     * Annonce ce qui vient de se passer dans le jeu, et fait vieillir les messages déjà affichés.
     * Coupées dans les réglages, les notifications sont tout de même relevées, pour ne pas toutes
     * tomber d'un coup le jour où on les rallume.
     */
    private void announce(double dt) {
        List<String> news = notifier.poll();
        // Le journal garde tout, que les notifications soient affichées ou non.
        for (String text : news) EventLog.of(game).add(game, text);
        if (settings.notifications()) {
            for (String text : news) toasts.show(text, noticeColor(text));
        } else if (toasts.count() > 0) {
            toasts.clear();
        }
        toasts.frame(dt);
    }

    /** La couleur du liseré d'une notification : celle de l'onglet dont elle parle. */
    private static String noticeColor(String text) {
        if (text.startsWith("Big Bang") || text.startsWith("Molécules") || text.startsWith("Espace")
                || text.startsWith("Rassemblement automatique") || text.startsWith("Formation automatique")) return BIG_BANG_COLOR;
        if (text.startsWith("Succès")) return ACHIEVEMENTS_COLOR;
        if (text.startsWith("Explosion") || text.startsWith("Défi") || text.startsWith("Nouveau défi")
                || text.contains("matière noire") || text.startsWith("Palier de taille") || text.contains("paliers de taille")
                || text.contains("exploser")) return DARK_MATTER_COLOR;
        if (text.contains("utomatis")) return AUTOMATION_COLOR;
        if (text.startsWith("Palier de vitesse")) return PARTICLES_COLOR;
        return ATOMS_COLOR;
    }

    private static String lowerFirst(String text) {
        return text.isEmpty() ? text : Character.toLowerCase(text.charAt(0)) + text.substring(1);
    }

    /** Un clic sur un onglet : il s'affiche, et son contenu est recopié aussitôt, puisqu'il ne l'était plus tant qu'il était caché. */
    private void open(Tab tab) {
        selectTab(tab);
        refresh();
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
        bigBangPage.setVisible(tab == Tab.BIG_BANG);
        achievementsScroll.setVisible(tab == Tab.ACHIEVEMENTS);
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
        bigBangTab.setStyle(tabStyle(BIG_BANG_COLOR, selectedTab == Tab.BIG_BANG));
        achievementsTab.setStyle(tabStyle(ACHIEVEMENTS_COLOR, selectedTab == Tab.ACHIEVEMENTS));
        statsTab.setStyle(tabStyle(STATS_COLOR, selectedTab == Tab.STATS));
        settingsTab.setStyle(tabStyle(SETTINGS_COLOR, selectedTab == Tab.SETTINGS));
        // Sept ou huit onglets ne tiennent pas en toutes lettres dans une fenêtre étroite.
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
        if (selectedTab == Tab.BIG_BANG) {
            bigBangPage.frame(dt);
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
        // Avec l'onglet « Big Bang » ils sont huit : la fenêtre de 800 px ne suffit plus aux noms entiers.
        boolean bigBang = game.isBigBangUnlocked();
        boolean nowNarrow = root.getWidth() > 0 && root.getWidth() < (bigBang ? NARROW_WINDOW_WITH_BIG_BANG : NARROW_WINDOW);
        if (nowNarrow != narrow) {
            narrow = nowNarrow;
            styleTabs();
        }
        if (debugBar != null) debugBar.refresh();

        // L'onglet « Particules » : ses compteurs, ses cartes et sa fusion ne sont recopiés que lorsqu'il est affiché.
        if (selectedTab == Tab.PARTICLES) {
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
            productionBreakdown.refresh();

            upgradeCards.forEach(this::show);
            amountButtons.forEach((amount, button) -> button.setStyle(AMOUNT_STYLE + (amount == settings.buyAmount()
                    ? " -fx-text-fill: #0b0e14; -fx-background-color: " + PARTICLES_COLOR + "; -fx-border-color: #ffffff;"
                    : " -fx-text-fill: " + PARTICLES_COLOR + "; -fx-background-color: #16202e; -fx-border-color: #3a4a5e;")));
            // Les autres raccourcis, ceux qui ne sont écrits sur aucune carte : en mode détails seulement.
            boolean keys = settings.shortcuts() && Detail.shown();
            shortcutsLabel.setText(shortcutsHint());
            shortcutsLabel.setVisible(keys);
            shortcutsLabel.setManaged(keys);

            // Au plafond d'atomes, la carte reste affichée mais éteinte, pour expliquer pourquoi rien ne se passe.
            boolean capped = game.isAtomCapReached();
            boolean table = game.isPeriodicTableUnlocked() && !game.isPeriodicTableComplete();
            BigNum gain = game.atomsPerFusion();
            fuseCard.show(capped ? Card.State.WAITING : Card.State.READY,
                    String.valueOf(game.generatorCount()),
                    settings.shortcuts() && settings.key(Shortcuts.Action.FUSE) != null ? Shortcuts.name(settings.key(Shortcuts.Action.FUSE)) : "",
                    game.generatorCount() > game.generatorsPerAtom()
                            ? "Fusionner " + game.generatorCount() + " générateurs"
                            : "Fusionner les " + game.generatorsPerAtom() + " générateurs",
                    capped ? "Maximum de " + Format.count(game.atomCap()) + " atomes atteint"
                            : "+" + Format.amount(gain) + (gain.gt(BigNum.ONE) ? " atomes" : " atome"),
                    capped ? "Dépensez des atomes" + (table ? " ou synthétisez un élément" : "") + " pour fusionner de nouveau."
                            : "Les générateurs fusionnent : il n'en reste qu'un, et les particules repartent de zéro. "
                                    + (game.keepsUpgradesOnFusion()
                                            ? "Les améliorations payées en particules sont gardées (Persistance)."
                                            : "Les améliorations payées en particules aussi."),
                    "", "");
            fuseCard.setVisible(game.hasAllGenerators() && !generators.isFusing());
        }

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
        readiness.frame();
        atomsTab.setText("Atomes (" + Format.count(game.state().atoms()) + ")");
        atomsPage.refresh(selectedTab == Tab.ATOMS);

        // L'onglet « Automatisation » n'existe qu'une fois un automatisme débloqué, ordinaire ou de matière noire.
        boolean automation = AutomationPage.hasContent(game);
        automationTab.setVisible(automation);
        automationTab.setManaged(automation);
        automationTab.setText((narrow ? "Auto." : "Automatisation") + (game.isAutomationPaused() && game.hasAnyAutomation() ? " (coupée)" : ""));
        if (selectedTab == Tab.AUTOMATION) automationPage.refresh();

        // L'onglet « Matière noire » n'existe qu'après la première explosion.
        darkMatterTab.setVisible(darkMatter);
        darkMatterTab.setManaged(darkMatter);
        darkMatterTab.setText((narrow ? "M. noire (" : "Matière noire (") + Format.count(game.state().darkMatter()) + ")"
                + Readiness.dot(readiness.darkMatter()));
        darkMatterPage.refresh(selectedTab == Tab.DARK_MATTER);

        // L'onglet « Big Bang » n'existe qu'après le premier Big Bang. Il vient d'y en avoir un : on y va,
        // comme une explosion mène à l'onglet « Matière noire ».
        bigBangTab.setVisible(bigBang);
        bigBangTab.setManaged(bigBang);
        bigBangTab.setText((narrow ? "Big Bang" : "Big Bang (" + game.bigBangs() + ")") + Readiness.dot(readiness.bigBang()));
        if (lastBigBangs >= 0 && game.bigBangs() > lastBigBangs) selectTab(Tab.BIG_BANG);
        lastBigBangs = game.bigBangs();
        if (selectedTab == Tab.BIG_BANG) bigBangPage.refresh();

        // Les succès et les statistiques ne se recalculent que lorsqu'on les regarde. L'onglet des succès
        // n'existe que si le jeu en a.
        boolean achievements = !game.achievements().isEmpty();
        achievementsTab.setVisible(achievements);
        achievementsTab.setManaged(achievements);
        if (selectedTab == Tab.ACHIEVEMENTS) achievementsPage.refresh();
        if (selectedTab == Tab.STATS) statsPage.refresh();
        settingsTab.setText(!settings.paused() ? "Réglages" : narrow ? "En pause" : "Réglages (pause)");

        // Un onglet qui vient de disparaître (après une explosion) ne peut pas rester affiché.
        if ((selectedTab == Tab.ATOMS && !hasAtoms) || (selectedTab == Tab.AUTOMATION && !automation)
                || (selectedTab == Tab.DARK_MATTER && !darkMatter) || (selectedTab == Tab.BIG_BANG && !bigBang)) {
            selectTab(Tab.PARTICLES);
            // L'onglet « Particules » n'était plus recopié tant qu'il était caché : il l'est maintenant, avec tout le reste.
            refresh();
            return;
        }

        // Le bouton d'explosion n'apparaît que lorsque les 118 éléments sont découverts.
        Challenge challenge = game.activeChallenge();
        boolean canExplode = game.canExplode() && !exploding;
        // Son texte n'est composé que lorsqu'il se voit.
        if (canExplode) {
            explosionButton.setText(explosionArmed > 0
                    ? "Cliquer encore pour confirmer l'explosion : tout repart de zéro (" + (int) Math.ceil(explosionArmed) + " s)"
                    : game.isChallengeReplay()
                    ? "Terminer le défi « " + challenge.name() + " » en " + Format.duration(game.stats().runTime())
                            + Detail.only(". Déjà réussi, il ne compte que pour son temps : ni matière noire, ni tableau plus lourd.")
                    : "Faire exploser le tableau : +" + Format.count(game.nextExplosionDarkMatter()) + " matière noire"
                            + (game.earnsSpeedPrime() ? " (prime de vitesse : encore " + Format.wait(Math.max(1, game.speedPrimeLeft())) + ")" : "")
                            + (challenge == null ? "" : ", défi « " + challenge.name() + " » réussi")
                            + NextReset.explosionSuffix(game)
                            + Detail.only(". Tout repart de zéro : particules, atomes, éléments. "
                                    + (game.state().tableWeightLevel() < Game.TABLE_WEIGHT_MAX_LEVEL
                                            ? "Le tableau suivant sera " + ElementText.number(Game.TABLE_WEIGHT_GROWTH)
                                                    + " fois plus lourd : plafond d'atomes et prix maximal d'une synthèse. "
                                            : "Le tableau a atteint sa masse maximale : il ne s'alourdira plus. ")
                                    + "Rien n'oblige à attendre les exemplaires manquants : ils renforcent les éléments, "
                                    + "pas l'explosion."));
        }
        // Le thème clair suit la taille de la fenêtre.
        if (settings.theme() == Settings.Theme.LIGHT) lightTheme.fit();
        // L'objectif du moment, sous les onglets. Caché, il laisse sa place vide : le rappel reste à droite.
        boolean goal = settings.showGoal() && game.isStarted();
        // Après un premier Big Bang, deux objectifs se suivent de front : celui de la partie, et celui du troisième acte.
        String act = goal ? Goals.act(game) : "";
        // Entre les deux, l'avancée du prochain Big Bang : ses cinq conditions, sans ouvrir l'arbre.
        String bang = goal ? NextReset.bigBangProgress(game) : "";
        // Le mode chrono : l'écart au record sur la prochaine étape, quand il y a un record à battre.
        String chrono = goal && settings.chrono() ? Goals.chrono(game) : "";
        goalLabel.setText(goal ? "Objectif : " + Goals.withEstimate(game, game.stats())
                + (bang.isEmpty() ? "" : "   |   Prochain Big Bang : " + bang)
                + (act.isEmpty() ? "" : "   |   Troisième acte : " + act)
                + (chrono.isEmpty() ? "" : "   |   " + chrono) : "");
        String detailKey = Detail.keyName(settings.detailKey());
        detailHint.setText(Detail.shown()
                ? (settings.detailToggle() ? detailKey + " : cacher les détails" : "Détails affichés")
                : detailKey + " : détails");
        detailHint.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 1 8; -fx-background-radius: 3;"
                + " -fx-border-radius: 3;" + (Detail.shown()
                ? " -fx-text-fill: #0b0e14; -fx-background-color: " + SETTINGS_COLOR + "; -fx-border-color: #ffffff;"
                : " -fx-text-fill: " + SETTINGS_COLOR + "; -fx-background-color: transparent; -fx-border-color: #3a4a5e;"));
        KeyCode helpKey = settings.shortcuts() ? settings.key(Shortcuts.Action.HELP) : null;
        helpButton.setText(helpPane.isOpen() ? "Fermer l'aide" : helpKey == null ? "Aide" : Shortcuts.name(helpKey) + " : aide");
        helpButton.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 1 8; -fx-cursor: hand; -fx-background-radius: 3;"
                + " -fx-border-radius: 3;" + (helpPane.isOpen()
                ? " -fx-text-fill: #0b0e14; -fx-background-color: " + SETTINGS_COLOR + "; -fx-border-color: #ffffff;"
                : " -fx-text-fill: " + SETTINGS_COLOR + "; -fx-background-color: transparent; -fx-border-color: #3a4a5e;"));
        if (selectedTab == Tab.SETTINGS) settingsPage.refresh();
        // Le défi en cours reste sous les yeux, quel que soit l'onglet.
        idle.core.BangChallenge bangChallenge = game.activeBangChallenge();
        challengeLabel.setVisible(challenge != null || bangChallenge != null);
        challengeLabel.setManaged(challenge != null || bangChallenge != null);
        if (challenge != null || bangChallenge != null) {
            String ordinary = challenge == null ? "" : "Défi « " + challenge.name() + " » depuis " + Format.duration(game.stats().runTime())
                    + " : " + (Detail.shown() ? ChallengesPane.rule(challenge) : ChallengesPane.shortRule(challenge));
            String constraint = bangChallenge == null ? "" : "Défi de Big Bang « " + bangChallenge.label() + " » depuis "
                    + Format.duration(game.bangChallengeTime()) + " : " + (Detail.shown() ? bangChallenge.rule()
                    : lowerFirst(bangChallenge.brief()) + ", jusqu'au prochain Big Bang");
            challengeLabel.setText(constraint + (constraint.isEmpty() || ordinary.isEmpty() ? "" : "\n") + ordinary);
        }
        explosionButton.setVisible(canExplode);
        explosionButton.setManaged(canExplode);
    }

    /**
     * Remplit la carte d'une amélioration payée en particules : son niveau, sa touche, ce qu'elle
     * rapporte, ce que le clic achèterait et à quel prix, et le temps qu'il reste à attendre.
     */
    private void show(Upgrade upgrade, Card card) {
        String id = upgrade.id();
        KeyCode key = settings.shortcuts() ? keyOf(upgrade) : null;
        boolean generator = upgrade.effect() instanceof Effect.AddGenerator;
        // Les générateurs se comptent : c'est leur nombre qui décide de la fusion.
        String level = generator ? game.generatorCount() + "/" + game.maxGeneratorCount()
                : game.levelOf(id) + (upgrade.hasLimit() ? "/" + upgrade.maxLevel() : "");
        String mark = key == null ? "" : Shortcuts.name(key);
        if (game.isMaxed(id)) {
            card.show(Card.State.DONE, level, "max", upgrade.name(), effectOf(upgrade), detailOf(upgrade), "", "");
            return;
        }
        // Ce que le clic achèterait : la quantité choisie, ramenée à ce qui est à portée, un niveau au moins.
        int count = Math.max(1, game.affordableLevels(id, settings.buyAmount().count()));
        BigNum cost = game.costOf(id, count);
        double wait = game.secondsUntilParticles(cost);
        boolean affordable = game.canBuy(id);
        card.show(affordable ? Card.State.READY : game.levelOf(id) > 0 ? Card.State.STARTED : Card.State.WAITING,
                level, mark, upgrade.name(), effectOf(upgrade), detailOf(upgrade),
                (count > 1 ? "×" + count + " pour " : "") + Format.count(cost)
                        + (cost.gt(BigNum.ONE) ? " particules" : " particule"),
                wait <= 0 || Double.isInfinite(wait) ? "" : Format.wait(wait));
    }

    /** Ce que rapporte une amélioration payée en particules, en quelques mots. */
    private String effectOf(Upgrade upgrade) {
        return switch (upgrade.effect()) {
            case Effect.MultiplySpeed speed -> {
                String text = "+" + percent(speed.perLevel() - 1 + game.speedExtraPerLevel()) + " % de vitesse";
                if (!speed.hasMilestones()) yield text;
                int left = game.nextSpeedMilestone(upgrade.id()) - game.speedLevels(upgrade.id());
                yield text + "\nPalier " + Format.multiplier(BigNum.of(game.speedMilestoneFactor(upgrade.id())))
                        + " dans " + left + (left > 1 ? " niveaux" : " niveau");
            }
            case Effect.MultiplyByGenerators coupling -> {
                int level = game.levelOf(upgrade.id());
                yield "+" + percent(coupling.perGenerator()) + " % par autre générateur"
                        + (level > 0 ? "\nActuellement " + Format.multiplier(game.particlesMultiplier(upgrade.id(), level)) : "");
            }
            case Effect.AddGenerator generator -> game.generatorCount() < game.generatorsPerAtom()
                    ? "Il en faut " + game.generatorsPerAtom() + " pour fusionner"
                    : game.isMaxed(upgrade.id()) ? "Tous réunis : prêts à fusionner"
                    : "Chaque groupe de " + game.generatorsPerAtom() + " compte à la fusion";
            default -> "";
        };
    }

    /** L'explication complète d'une amélioration payée en particules, pour le mode détails. */
    private String detailOf(Upgrade upgrade) {
        return switch (upgrade.effect()) {
            case Effect.MultiplySpeed speed -> {
                String text = "Chaque niveau accélère tous les générateurs.";
                if (!speed.hasMilestones()) yield text;
                yield text + " Tous les " + speed.milestoneEvery() + " niveaux, un palier multiplie les particules créées par "
                        + ElementText.number(game.speedMilestoneFactor(upgrade.id())) + ". Paliers atteints : "
                        + game.speedMilestones() + ", particules " + Format.multiplier(game.speedMilestoneMultiplier()) + ".";
            }
            case Effect.MultiplyByGenerators coupling ->
                    "Chaque niveau ajoute " + percent(coupling.perGenerator()) + " % de particules par autre générateur "
                            + "possédé : plus il y a de générateurs, plus chaque niveau compte.";
            case Effect.AddGenerator generator -> game.maxGeneratorCount() > game.generatorsPerAtom()
                    ? "Chaque générateur crée des particules. La fusion rapporte ses atomes une fois par groupe de "
                            + game.generatorsPerAtom() + ", avec une prime par groupe au-delà du premier."
                    : "Chaque générateur crée des particules. Réunis, les " + game.generatorsPerAtom()
                            + " générateurs fusionnent en atomes.";
            default -> "";
        };
    }

    /** 0.1 → « 10 », 0.125 → « 12.5 ». */
    private static String percent(double fraction) {
        String text = String.format(Locale.ROOT, "%.1f", fraction * 100);
        return text.endsWith(".0") ? text.substring(0, text.length() - 2) : text;
    }

    /** La ligne qui rappelle les raccourcis qu'aucune carte ne porte, sous les cartes. */
    private String shortcutsHint() {
        KeyCode all = settings.key(Shortcuts.Action.BUY_ALL);
        KeyCode pause = settings.key(Shortcuts.Action.PAUSE);
        KeyCode help = settings.key(Shortcuts.Action.HELP);
        return "Autres raccourcis : " + (all == null ? "" : Shortcuts.name(all) + " achète tout ce qui est à portée, ")
                + (pause == null ? "" : Shortcuts.name(pause) + " met en pause, ")
                + (help == null ? "" : Shortcuts.name(help) + " ouvre l'aide et la liste complète. ")
                + "Les touches des cartes achètent la quantité choisie, depuis n'importe quel onglet.";
    }
}
