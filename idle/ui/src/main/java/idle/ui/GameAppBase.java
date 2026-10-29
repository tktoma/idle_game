package idle.ui;

import idle.core.Absence;
import idle.core.Game;
import idle.core.GameStats;
import idle.core.Resource;
import idle.core.save.SaveCodec;
import idle.core.upgrades.Upgrade;
import idle.ui.atoms.AtomView;
import idle.ui.atoms.AtomsPage;
import idle.ui.automation.AutomationPage;
import idle.ui.bigbang.BigBangPage;
import idle.ui.collider.ColliderPage;
import idle.ui.collider.ColliderPane;
import idle.ui.common.Breakdown;
import idle.ui.common.BuyAmountRow;
import idle.ui.common.Card;
import idle.ui.common.Format;
import idle.ui.common.Labels;
import idle.ui.common.StableHeight;
import idle.ui.common.TabIcon;
import idle.ui.common.TileGrid;
import idle.ui.darkmatter.DarkMatterPage;
import idle.ui.universe.Dimension;
import idle.ui.universe.DimensionWindow;
import idle.ui.universe.UniversePage;
import idle.ui.pages.AchievementsPage;
import idle.ui.pages.ProfilePage;
import idle.ui.pages.SettingsPage;
import idle.ui.pages.StatsPage;
import idle.ui.particles.GeneratorPane;
import idle.ui.probes.LootPane;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javafx.animation.AnimationTimer;
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
import javafx.stage.Stage;

/**
 * L'application, première partie : ses onglets, ses pages, et la construction de la fenêtre.
 */
public abstract class GameAppBase extends Application {

    /** L'application entière : chaque partie de la chaîne est une {@link GameApp}. */
    final GameApp self() {
        return (GameApp) this;
    }

    /** Seul {@link GameApp} prolonge la chaîne. */
    GameAppBase() {
    }

    // Ce que fournissent les parties suivantes.
    abstract void closing();
    abstract void applyMinWindow();
    abstract boolean windowSizeFits(Settings.WindowSize size);
    abstract Settings.WindowSize currentWindowSize();
    abstract void applyWindowSize(Settings.WindowSize size);
    abstract void placeWindow();
    abstract void save();
    abstract void catchUp();
    abstract void afterLoad();
    abstract void showStartButton();
    abstract void resetGame();
    abstract Card fuseCard();
    abstract void forgetOldPages();
    abstract boolean keyPressed(KeyCode code);
    abstract boolean keyReleased(KeyCode code);
    abstract void repeatHeldKeys(double dt);
    abstract void releaseDetailKey();
    abstract void layoutTabs();
    abstract void makeDraggable(Tab tab);
    abstract void requestExplosion();
    abstract void applyAppearance();
    abstract void dock(Tab tab, javafx.scene.layout.Pane page, javafx.scene.layout.Pane bar);
    abstract boolean universePhase();
    abstract void checkPopups();
    abstract void announce(double dt);
    abstract void open(Tab tab);
    abstract void selectTab(Tab tab);
    abstract void animate(double dt);
    abstract int framesPerSecond();
    abstract double timeFactor();
    abstract void refresh();


    /** Largeur maximale de la rangée des améliorations, et de la carte de fusion en dessous. */
    private static final double UPGRADES_WIDTH = 760;
    static final double FUSE_WIDTH = 380;
    /** Nombre maximal de générateurs dessinés ; les suivants produisent sans être affichés. */
    static final int MAX_VISIBLE_GENERATORS = 30;

    /** Taille et marges des onglets du haut, selon la largeur de la fenêtre ; leur allure vient de leur rôle ({@link #tabStyle}). */
    static final String WIDE_TAB = " -fx-font-size: 14px; -fx-padding: 9 10;";
    static final String NARROW_TAB = " -fx-font-size: 12px; -fx-padding: 8 10;";
    /** En dessous de cette largeur de fenêtre, les onglets se resserrent pour tenir sur une ligne. */
    static final double NARROW_WINDOW = 1140;
    /** La même limite une fois l'onglet « Big Bang » apparu : huit onglets au lieu de sept. */
    static final double NARROW_WINDOW_WITH_BIG_BANG = 1280;
    /** Hauteur de la barre des sous-onglets, dans la disposition à gauche. */
    private static final double SUB_TAB_HEIGHT = 42;
    static final String PARTICLES_COLOR = "#9fd0ff";
    public static final String ATOMS_COLOR = "#ffd27f";
    public static final String AUTOMATION_COLOR = "#9be7a8";
    public static final String DARK_MATTER_COLOR = "#c9a6ff";
    /** La couleur du Big Bang : le blanc chaud d'un éclair, à part des quatre couleurs de ressources. */
    public static final String BIG_BANG_COLOR = "#fff1c9";
    public static final String ACHIEVEMENTS_COLOR = "#ff9eb5";
    static final String STATS_COLOR = "#7fe0d4";
    static final String SETTINGS_COLOR = "#d6dde6";
    /** Temps laissé pour le second clic qui confirme l'explosion, en secondes. */
    static final double EXPLOSION_CONFIRM_SECONDS = 5;

    /** Les onglets du jeu. */
    enum Tab { PARTICLES, ATOMS, AUTOMATION, DARK_MATTER, COLLIDER, BIG_BANG, UNIVERSE, VOID, HADRONS, PROFILE, ACHIEVEMENTS, STATS, SETTINGS, SKINS }

    /** Le nom d'un onglet, sans compteur : sur les fenêtres d'explication, et dans le rapport de bug (version 0.127). */
    static String tabName(Tab tab) {
        return switch (tab) {
            case PARTICLES -> "Particules";
            case ATOMS -> "Atomes";
            case AUTOMATION -> "Automatisation";
            case DARK_MATTER -> "Matière noire";
            case COLLIDER -> "Collisionneur";
            case BIG_BANG -> "Big Bang";
            case UNIVERSE -> "Univers";
            case VOID -> "Le vide";
            case HADRONS -> "Hadrons";
            case PROFILE -> "Profil";
            case ACHIEVEMENTS -> "Succès";
            case STATS -> "Statistiques";
            case SETTINGS -> "Réglages";
            case SKINS -> "Skins";
        };
    }

    /** La graine du hasard de cette session (version 0.127) : tirée au lancement, notée dans le rapport de bug. */
    final long randomSeed = new java.security.SecureRandom().nextLong();
    final Game game = new Game(randomSeed);
    /** Vrai dans le profil de test ({@code --test}) : connu au lancement. */
    boolean testProfile;
    final Settings settings = Settings.load();
    /**
     * Le fichier de sauvegarde. Celui que désignent les arguments du jeu n'est connu qu'au
     * lancement ({@link #start(Stage)}) : d'ici là, rien ne s'écrit. Déclaré avant les pages, qui
     * l'interrogent dès leur construction.
     */
    SaveStore store = SaveStore.of(List.of("--sans-sauvegarde"));
    final GeneratorPane generators = new GeneratorPane();
    final Label particlesLabel = new Label();
    final Label productionLabel = new Label();
    /** Au survol de la production : d'où elle vient, un facteur par ligne. */
    final Breakdown productionBreakdown = Breakdown.attach(productionLabel, game, Breakdown.Of.PARTICLES);
    final Card fuseCard = new Card(ATOMS_COLOR);

    // Les onglets, visibles à partir du premier atome.
    final Button particlesTab = new Button("Particules");
    final Button atomsTab = new Button();
    final Button automationTab = new Button("Automatisation");
    final Button darkMatterTab = new Button();
    final Button colliderTab = new Button("Collisionneur");
    final Button bigBangTab = new Button("Big Bang");
    /** L'onglet de l'univers (version 0.080), qui s'ouvre quand l'univers est formé : le quatrième acte. */
    final Button universeTab = new Button("Univers");
    /** L'onglet du vide (version 0.099), deuxième acte de la partie secondaire : au premier Big Bang du troisième univers (version 0.116). */
    final Button voidTab = new Button("Le vide");
    /** L'onglet des hadrons (version 0.149) : il se propose dans le collisionneur avec la bulle de l'arbuste, au milieu. */
    final Button hadronsTab = new Button("Hadrons");
    /** L'onglet du profil (version 0.147) : le joueur, ses titres, ses albums et le journal ; là dès le premier générateur. */
    final Button profileTab = new Button("Profil");
    final Button achievementsTab = new Button("Succès");
    final Button statsTab = new Button("Statistiques");
    final Button settingsTab = new Button("Réglages");
    /** Le régime rapide des générateurs (version 0.087), et la ligne de la synergie et de la prime. */
    final Button regimeButton = new Button();
    final Label secondLabel = new Label();
    final HBox secondRow = new HBox(10, regimeButton, secondLabel);
    /** L'onglet des skins (version 0.087), sous les réglages : il s'ouvre dans le deuxième univers, après cinq fusions. */
    final Button skinsTab = new Button("Skins");
    final AtomView atomsTabIcon = new AtomView(22);
    /** Les icônes animées des autres onglets (version 0.059), chacune dans la couleur de son onglet. */
    final TabIcon particlesIcon = new TabIcon(TabIcon.Kind.PARTICLES, PARTICLES_COLOR, 20);
    final TabIcon automationIcon = new TabIcon(TabIcon.Kind.AUTOMATION, AUTOMATION_COLOR, 20);
    final TabIcon darkMatterIcon = new TabIcon(TabIcon.Kind.DARK_MATTER, DARK_MATTER_COLOR, 20);
    final TabIcon colliderIcon = new TabIcon(TabIcon.Kind.COLLIDER, ColliderPane.COLOR, 20);
    final TabIcon bigBangIcon = new TabIcon(TabIcon.Kind.BIG_BANG, BIG_BANG_COLOR, 20);
    final TabIcon universeIcon = new TabIcon(TabIcon.Kind.UNIVERSE, UniversePage.COLOR, 20);
    final TabIcon voidIcon = new TabIcon(TabIcon.Kind.VOID, idle.ui.vacuum.VoidPage.COLOR, 20);
    final TabIcon hadronsIcon = new TabIcon(TabIcon.Kind.HADRONS, idle.ui.hadrons.HadronsPage.COLOR, 20);
    final TabIcon profileIcon = new TabIcon(TabIcon.Kind.PROFILE, ProfilePage.COLOR, 20);
    final TabIcon achievementsIcon = new TabIcon(TabIcon.Kind.ACHIEVEMENTS, ACHIEVEMENTS_COLOR, 20);
    final TabIcon statsIcon = new TabIcon(TabIcon.Kind.STATS, STATS_COLOR, 20);
    final TabIcon settingsIcon = new TabIcon(TabIcon.Kind.SETTINGS, SETTINGS_COLOR, 20);
    final TabIcon skinsIcon = new TabIcon(TabIcon.Kind.SKINS, idle.ui.pages.SkinsPage.COLOR, 20);
    final TabIcon[] tabIcons = {particlesIcon, automationIcon, darkMatterIcon, colliderIcon, bigBangIcon, universeIcon, voidIcon,
            hadronsIcon, profileIcon, achievementsIcon, statsIcon, settingsIcon, skinsIcon};
    /**
     * Le rôle d'un onglet, qui décide de son allure : les actes du jeu (cadre plein), ce qui tourne
     * à côté (cadre en pointillés), et les pages pour s'y retrouver (sans cadre, rangées à part).
     */
    enum TabRole { ACT, SIDE, META }
    /** Les onglets, dans l'ordre, et l'écart qui sépare les pages pour s'y retrouver des autres. */
    final Button[] tabButtons = {particlesTab, atomsTab, automationTab, darkMatterTab, colliderTab, bigBangTab,
            universeTab, voidTab, hadronsTab, profileTab, achievementsTab, statsTab, settingsTab, skinsTab};
    final javafx.scene.layout.Region tabGap = new javafx.scene.layout.Region();
    /** À gauche, l'espace qui pousse les pages pour s'y retrouver en bas de la colonne. */
    final javafx.scene.layout.Region sideSpacer = new javafx.scene.layout.Region();
    /** L'ordre des onglets, que le joueur change en les glissant ; gardé dans les réglages. */
    final List<Tab> tabOrder = GameApp.readTabOrder(settings.tabOrder());
    /** Le glisser-déposer : l'onglet saisi, le moment de l'appui, et vrai si l'appui a servi à glisser (pas à cliquer). */
    Tab draggedTab;
    long pressedAt;
    boolean justDragged;
    final HBox tabBar = new HBox(4, particlesTab, atomsTab, automationTab, darkMatterTab, colliderTab, bigBangTab,
            universeTab, voidTab, hadronsTab, tabGap, profileTab, achievementsTab, statsTab, settingsTab, skinsTab);
    /** À gauche, le nom de l'onglet affiché quand il n'a pas de sous-onglets : la barre du haut garde sa hauteur. */
    final Label tabTitle = new Label();
    /** À gauche : la colonne de droite, avec le haut de la fenêtre et les pages ; la colonne des onglets descend jusqu'en bas. */
    final BorderPane mainColumn = new BorderPane();
    StackPane pages;
    final BorderPane particlesPage = new BorderPane();
    final AtomsPage atomsPage = new AtomsPage(game, settings);
    final AutomationPage automationPage = new AutomationPage(game);
    final DarkMatterPage darkMatterPage = new DarkMatterPage(game);
    final ColliderPage colliderPage = new ColliderPage(game);
    final BigBangPage bigBangPage = new BigBangPage(game);
    final UniversePage universePage = new UniversePage(game);
    final idle.ui.vacuum.VoidPage voidPage = new idle.ui.vacuum.VoidPage(game);
    final idle.ui.hadrons.HadronsPage hadronsPage = new idle.ui.hadrons.HadronsPage(game);
    /** La fenêtre d'une dimension (version 0.081), par-dessus tout le jeu, avec son bouton de retour. */
    final DimensionWindow dimensionWindow = new DimensionWindow(game, settings::freeCamera, settings::setFreeCamera);
    final AchievementsPage achievementsPage = new AchievementsPage(game);
    final StatsPage statsPage = new StatsPage(game);
    /** Le profil : l'image importée se copie dans le dossier des données, connu seulement au lancement. */
    final ProfilePage profilePage = new ProfilePage(game, () -> store.enabled() ? store.folder() : null);
    final SettingsPage settingsPage = new SettingsPage(settings, this::resetGame, this::applyAppearance, new SaveActions());
    final idle.ui.pages.SkinsPage skinsPage = new idle.ui.pages.SkinsPage(game);
    final ScrollPane skinsScroll = new ScrollPane(skinsPage);
    final ScrollPane settingsScroll = new ScrollPane(settingsPage);
    final ScrollPane achievementsScroll = new ScrollPane(achievementsPage);
    /** Le centre de l'onglet « Particules » : les générateurs, et le bouton de démarrage tant que la partie n'a pas commencé. */
    final StackPane center = new StackPane(generators);
    /** Le compteur et les améliorations, sous les générateurs. */
    final VBox controls = new VBox(12, particlesLabel, productionLabel);
    Button startButton;
    /** Le retour de l'onglet Particules (version 0.111) : son âge en secondes, négatif hors animation. */
    double particlesTabPop = -1;
    /** Durée de ce retour. */
    static final double PARTICLES_TAB_POP_SECONDS = 0.9;
    final BorderPane root = new BorderPane();
    /** La disposition à gauche (version 0.057) : les onglets en colonne, et en haut les sous-onglets de l'onglet affiché. */
    final VBox sideBar = new VBox(2);
    final StackPane subTabHolder = new StackPane();
    /** Une barre de sous-onglets, et d'où elle vient dans sa page : la disposition d'avant l'y remet. */
    record SubTabDock(Tab tab, javafx.scene.layout.Pane page, javafx.scene.layout.Pane bar, int index) {}
    final List<SubTabDock> docks = new java.util.ArrayList<>();
    VBox topBox;
    boolean tabsLeftApplied = false;
    Tab selectedTab = Tab.PARTICLES;
    /**
     * Le dernier onglet de jeu affiché avant les réglages (version 0.127) : le rapport de bug se copie depuis les
     * réglages, c'est donc celui-là qu'il nomme, là où le joueur jouait.
     */
    Tab gameTab = Tab.PARTICLES;
    boolean narrow = false;

    // L'explosion : le bouton au-dessus des onglets, et ce qui recouvre la fenêtre pendant qu'elle a lieu.
    final Button explosionButton = new Button();
    /** Rappel du défi en cours, entre le bouton d'explosion et les onglets. */
    final Label challengeLabel = new Label();
    /** L'objectif du moment, sous les onglets. */
    final Label goalLabel = new Label();
    /** Le rappel de la touche de détail, à droite de l'objectif. */
    final Label detailHint = new Label();
    /** Vrai tant que la touche de détail est enfoncée : pour ne pas basculer à chaque répétition du clavier. */
    boolean detailKeyDown = false;
    // Les notifications, par-dessus tout le reste, et ce qui repère les nouvelles à annoncer.
    final ToastPane toasts = new ToastPane();
    final Notifier notifier = new Notifier(game);
    /** Le guide de transition entre univers (version 0.127). */
    final TransitionPane transitionPane = new TransitionPane(game);
    {
        bigBangPage.galaxyPane().setUniverseConfirm(go -> transitionPane.open(false, go));
        universePage.setLaunchConfirm(go -> transitionPane.open(true, go));
        // Les anomalies du ciel (version 0.127) : montrées et annoncées seulement si le joueur le veut.
        bigBangPage.showSkyEvents(settings::skyEvents);
        notifier.showSkyEvents(settings::skyEvents);
    }
    final Sound sound = new Sound();
    // L'apparence : la taille de l'interface et le thème clair s'appliquent à toute la fenêtre.
    ScaledPane scaled;
    ThemeFilter themeFilter;
    final StackPane flash = new StackPane();
    final Circle shockwave = new Circle(24, Color.web(DARK_MATTER_COLOR));
    final StackPane blast = new StackPane(flash, shockwave);
    /** Les animations plein écran des skins : l'explosion sous Vortex ou Pulsar, le Big Bang sous ses trois skins (version 0.089). */
    final idle.ui.common.BurstView burst = new idle.ui.common.BurstView();
    /** Toute la fenêtre : l'animation des skins en prend la taille. */
    StackPane windowPane;
    boolean exploding = false;
    /** Secondes restantes pour confirmer l'explosion par un second clic ; 0 quand elle n'est pas demandée. */
    double explosionArmed = 0;
    /** Atomes créés à la dernière image, pour repérer l'arrivée d'un nouvel atome ; −1 avant la première image. */
    double lastTotalAtoms = -1;
    /** Le nombre de Big Bangs au dernier affichage, pour remarquer qu'il vient d'y en avoir un ; −1 avant le premier affichage. */
    int lastBigBangs = -1;
    /** L'univers à la dernière image : 1 formé, 0 pas encore, −1 inconnu (au chargement, sans naissance à jouer). */
    int hadUniverse = -1;
    /** Barre du profil de test, ou {@code null} en jeu normal. */
    DebugBar debugBar;
    /** Guette les conditions des thèmes et des notations à débloquer. */
    private final Cosmetics cosmetics = new Cosmetics(game, settings);
    // La sauvegarde : le temps écoulé depuis la dernière écriture, et le retour après une absence.
    double sinceSave = 0;
    final AbsencePane absencePane = new AbsencePane(this::save);
    /** Les fenêtres d'explication : un onglet qui s'ouvre, une nouveauté du deuxième univers (version 0.089). */
    final PopupPane popupPane = new PopupPane(game);
    List<PopupPane.Entry> popupEntries;
    /** L'écran de fin (version 0.068) : une fois par partie, quand l'univers se forme. */
    final EndPane endPane = new EndPane(game);
    /** Le butin d'une sonde, par-dessus la fenêtre : une carte à la fois. */
    final LootPane lootPane = new LootPane(game, this::refresh);
    /** Temps réel donné au rattrapage d'une absence à chaque image, en nanosecondes : assez peu pour que la fenêtre reste vive. */
    static final long CATCH_UP_NANOS = 10_000_000;
    Stage stage;
    boolean closed = false;
    final Map<Upgrade, Card> upgradeCards = new LinkedHashMap<>();
    /** Les améliorations côte à côte ; dans une fenêtre étroite, elles se rangent sur plusieurs rangées. */
    private final TileGrid upgradeGrid = new TileGrid(170, 4, 8);
    /** Les trois quantités d'achat : ×1, ×10, max. */
    final BuyAmountRow amountRow = new BuyAmountRow(settings, this::refresh);
    /** Raccourci → amélioration payée en particules qu'il achète. */
    final Map<Shortcuts.Action, Upgrade> upgradeActions = new EnumMap<>(Shortcuts.Action.class);
    /** L'aide « Comment jouer », par-dessus la fenêtre, et le bouton qui l'ouvre, sous les onglets. */
    final HelpPane helpPane = new HelpPane(game, settings);
    final Button helpButton = new Button();
    /** Ce qui est prêt quelque part, pour la pastille des onglets. */
    final Readiness readiness = Readiness.of(game);
    final Label shortcutsLabel = new Label();

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        settingsPage.setSoundTest(() -> sound.test(settings.volume()));
        // La boucle (version 0.153) : les touches de ses cercles et son métronome se rangent dans les réglages.
        dimensionWindow.setLoopSettings(new DimensionWindow.LoopSettings() {
            @Override
            public javafx.scene.input.KeyCode loopKey(int ring) {
                return settings.loopKey(ring);
            }

            @Override
            public boolean metronome() {
                return settings.loopMetronome();
            }

            @Override
            public void setMetronome(boolean on) {
                settings.setLoopMetronome(on);
            }

            @Override
            public void beat() {
                if (settings.sound()) sound.beat(settings.volume());
            }
        });
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
        controls.getChildren().add(amountRow);
        // Le deuxième univers (version 0.087) : le régime des générateurs, la synergie et la prime de cycle parfait.
        regimeButton.setFocusTraversable(false);
        regimeButton.setOnAction(event -> {
            game.startRegime();
            refresh();
        });
        secondLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #cfe8ff;");
        secondRow.setAlignment(Pos.CENTER);
        controls.getChildren().add(secondRow);

        // Une carte par amélioration payée en particules : en ajouter une dans core suffit à la faire apparaître.
        for (Upgrade upgrade : game.upgrades(Resource.PARTICLES)) {
            Card card = new Card(PARTICLES_COLOR);
            card.setOnAction(() -> {
                game.buy(upgrade.id(), settings.buyAmount().count());
                refresh();
            });
            upgradeCards.put(upgrade, card);
            upgradeGrid.add(card);
            Shortcuts.Action action = GameApp.actionOf(upgrade);
            if (action != null) upgradeActions.putIfAbsent(action, upgrade);
        }
        upgradeGrid.setMaxWidth(UPGRADES_WIDTH);
        controls.getChildren().add(upgradeGrid);
        controls.getChildren().add(fuseCard());
        Labels.centered(shortcutsLabel, "-fx-font-size: 11px; -fx-text-fill: #6f8296;");
        controls.getChildren().add(shortcutsLabel);

        // Au centre : les générateurs, qui occupent toute la place restante.
        if (!game.isStarted()) showStartButton();

        // Onglet « Particules » : les générateurs au-dessus, le compteur et les améliorations en dessous.
        particlesPage.setCenter(center);
        particlesPage.setBottom(controls);

        // Les onglets sont empilés au même endroit ; un seul est visible à la fois.
        Labels.transparent(settingsScroll);
        Labels.transparent(achievementsScroll);
        pages = new StackPane(particlesPage, atomsPage, automationPage, darkMatterPage, colliderPage, bigBangPage,
                universePage, voidPage, hadronsPage, profilePage, achievementsScroll, statsPage, settingsScroll, skinsScroll);
        Labels.transparent(skinsScroll);
        particlesTab.setOnAction(event -> open(Tab.PARTICLES));
        atomsTab.setOnAction(event -> open(Tab.ATOMS));
        automationTab.setOnAction(event -> open(Tab.AUTOMATION));
        darkMatterTab.setOnAction(event -> open(Tab.DARK_MATTER));
        colliderTab.setOnAction(event -> open(Tab.COLLIDER));
        bigBangTab.setOnAction(event -> open(Tab.BIG_BANG));
        universeTab.setOnAction(event -> open(Tab.UNIVERSE));
        voidTab.setOnAction(event -> open(Tab.VOID));
        hadronsTab.setOnAction(event -> open(Tab.HADRONS));
        profileTab.setOnAction(event -> open(Tab.PROFILE));
        achievementsTab.setOnAction(event -> open(Tab.ACHIEVEMENTS));
        statsTab.setOnAction(event -> open(Tab.STATS));
        settingsTab.setOnAction(event -> open(Tab.SETTINGS));
        skinsTab.setOnAction(event -> open(Tab.SKINS));
        atomsTab.setGraphic(atomsTabIcon);
        particlesTab.setGraphic(particlesIcon);
        automationTab.setGraphic(automationIcon);
        darkMatterTab.setGraphic(darkMatterIcon);
        colliderTab.setGraphic(colliderIcon);
        bigBangTab.setGraphic(bigBangIcon);
        universeTab.setGraphic(universeIcon);
        voidTab.setGraphic(voidIcon);
        hadronsTab.setGraphic(hadronsIcon);
        profileTab.setGraphic(profileIcon);
        achievementsTab.setGraphic(achievementsIcon);
        statsTab.setGraphic(statsIcon);
        settingsTab.setGraphic(settingsIcon);
        skinsTab.setGraphic(skinsIcon);
        for (Button tab : tabButtons) {
            tab.setGraphicTextGap(6);
            tab.setFocusTraversable(false);
        }
        tabGap.setMinWidth(18);
        tabGap.setMinHeight(18);
        VBox.setVgrow(sideSpacer, Priority.ALWAYS);
        // Un appui long sur un onglet le laisse glisser à une autre place.
        for (Tab tab : Tab.values()) makeDraggable(tab);
        layoutTabs();
        tabBar.setAlignment(Pos.CENTER);
        tabBar.setStyle("-fx-padding: 6 8 0 8;");
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
        StableHeight.of(goalLabel);   // la ligne d'objectif change sans cesse : sa hauteur ne doit pas faire sauter la page
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
        topBox = top;
        sideBar.setStyle("-fx-background-color: #0e131b; -fx-padding: 10 8; -fx-spacing: 6; -fx-border-color: transparent #1d2633 transparent transparent;");
        sideBar.setMinWidth(194);
        sideBar.setPrefWidth(194);
        subTabHolder.setStyle("-fx-background-color: #0e131b; -fx-padding: 4 0; -fx-border-color: transparent transparent #1d2633 transparent;");
        // Toujours la même hauteur, sous-onglets ou non : rien ne bouge quand on change d'onglet.
        subTabHolder.setMinHeight(SUB_TAB_HEIGHT);
        subTabHolder.setPrefHeight(SUB_TAB_HEIGHT);
        subTabHolder.setMaxHeight(SUB_TAB_HEIGHT);
        tabTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");
        subTabHolder.getChildren().add(tabTitle);
        dock(Tab.ATOMS, atomsPage, atomsPage.subTabs());
        // L'automatisation (version 0.150) : les automatismes de toujours, et ceux des dimensions.
        dock(Tab.AUTOMATION, automationPage, automationPage.subTabs());
        dock(Tab.DARK_MATTER, darkMatterPage, darkMatterPage.subTabs());
        dock(Tab.COLLIDER, colliderPage, colliderPage.subTabs());
        dock(Tab.BIG_BANG, bigBangPage, bigBangPage.subTabs());
        dock(Tab.UNIVERSE, universePage, universePage.subTabs());
        // Le vide (version 0.129) : ses paires, et ses améliorations à part.
        dock(Tab.VOID, voidPage, voidPage.subTabs());
        // Les hadrons (version 0.149) : les gluons ; les paliers et la liaison (version 0.151).
        dock(Tab.HADRONS, hadronsPage, hadronsPage.subTabs());
        universePage.setOnOpenDimension(number -> dimensionWindow.open(Dimension.of(number)));
        universePage.setOnOldTabs(shown -> refresh());
        // Rouvrir les paliers (version 0.087) : les anciens onglets reviennent, la partie de l'univers suivant commence.
        universePage.setOnNextUniverse(() -> {
            // Les trois actes repartent de zéro (version 0.108) : les pages oublient ce qu'elles montraient.
            forgetOldPages();
            refresh();
            // Version 0.111 : l'onglet Particules réapparaît en grandissant ; on y va une fois qu'il est là.
            particlesTabPop = 0;
            particlesTab.setScaleX(0.2);
            particlesTab.setScaleY(0.2);
        });
        dock(Tab.PROFILE, profilePage, profilePage.subTabs());
        dock(Tab.STATS, statsPage, statsPage.subTabs());
        dock(Tab.SETTINGS, settingsPage, settingsPage.subTabs());
        testProfile = arguments.contains("--test");
        if (testProfile) {
            debugBar = new DebugBar(game);
            debugBar.setCosmetics(new DebugBar.CosmeticTools() {
                @Override
                public void unlockAll() {
                    for (Cosmetic cosmetic : Cosmetic.values()) settings.unlock(cosmetic);
                    settingsPage.refresh();
                }

                @Override
                public void lockAll() {
                    settings.lockCosmetics();
                    applyAppearance();
                    settingsPage.refresh();
                }

                @Override
                public void toggle(Cosmetic cosmetic) {
                    if (settings.hasCosmetic(cosmetic)) settings.lock(cosmetic);
                    else settings.unlock(cosmetic);
                    applyAppearance();
                    settingsPage.refresh();
                }
            });
            top.getChildren().add(0, debugBar);
        }

        root.setTop(top);
        root.setCenter(pages);
        root.setStyle("-fx-background-color: #0b0e14;");

        // L'explosion recouvre toute la fenêtre : un éclair blanc et une onde qui part du centre.
        flash.setStyle("-fx-background-color: #ffffff;");
        flash.setOpacity(0);
        blast.setVisible(false);
        StackPane window = windowPane = new StackPane(root, dimensionWindow, blast, burst, toasts, lootPane, helpPane, endPane, popupPane,
                transitionPane, absencePane);
        bigBangPage.probesPane().setOpener(lootPane::open);
        StackPane.setAlignment(toasts, Pos.BOTTOM_RIGHT);
        scaled = new ScaledPane(window);
        themeFilter = new ThemeFilter(window);

        // Boucle de jeu : appelée à chaque image (~60 fois par seconde) sur le thread JavaFX.
        new AnimationTimer() {
            private long previous = 0;
            /** Le temps des images sautées : il est rendu au jeu à l'image suivante, rien ne se perd. */
            private double pending = 0;

            @Override
            public void handle(long now) {
                double elapsed = previous == 0 ? 0 : (now - previous) / 1e9; // nanosecondes → secondes
                previous = now;
                // Au retour d'une absence, le jeu rattrape d'abord le temps perdu : rien d'autre n'avance.
                if (absencePane.isCatchingUp()) {
                    catchUp();
                    return;
                }
                // Images par seconde plafonnées, et bien moins quand la fenêtre n'est pas au premier plan.
                pending += elapsed;
                if (pending < 1.0 / framesPerSecond() - GameApp.FRAME_TOLERANCE) return;
                double dt = pending;
                pending = 0;
                sinceSave += dt;
                if (sinceSave >= settings.saveDelay().seconds()) save();
                // En pause, le temps du jeu s'arrête ; celui de l'interface continue (confirmations, réglages).
                if (!settings.paused()) {
                    // Pendant l'avance rapide, la Timeline pilote déjà le temps du jeu.
                    if (debugBar == null || !debugBar.isFastForwarding()) {
                        game.tick(dt * timeFactor());
                        if (debugBar != null) debugBar.frame(dt * timeFactor());
                    }
                    animate(dt);
                }
                // Le clic tenu sur une carte qui se répète (une molécule, une fois la création continue acquise).
                Card.repeatHeld(dt);
                repeatHeldKeys(dt);
                explosionArmed = Math.max(0, explosionArmed - dt);
                if (selectedTab == Tab.SETTINGS) settingsPage.frame(dt);
                announce(dt);
                // L'écran de fin attend que la naissance de l'univers soit jouée.
                boolean bornNow = hadUniverse == 0 && game.hasCosmos(idle.core.sky.Cosmos.UNIVERSE);
                if (!bornNow && !(selectedTab == Tab.UNIVERSE && universePage.isBirthPlaying())) endPane.check();
                checkPopups();
                // Les thèmes et notations à débloquer : annoncés après le reste, pour ne pas être chassés par lui.
                // Version 0.146 : leur catégorie se coupe dans les réglages, comme les autres notifications.
                for (Cosmetic opened : cosmetics.frame(dt, settings.paused())) {
                    if (settings.notice(Notice.COSMETICS)) toasts.show(Cosmetics.announcement(opened), SETTINGS_COLOR);
                }
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
        if (!loaded.notice().isEmpty() && settings.notice(Notice.GAME)) toasts.show(loaded.notice(), SETTINGS_COLOR);
        // Le temps passé jeu fermé n'est rejoué que si le joueur le veut (réglage « Hors ligne »).
        if (loaded.found() && settings.offline()) {
            Absence absence = new Absence(game, (System.currentTimeMillis() - loaded.savedAt()) / 1000.0);
            if (!absence.isDone() && !universePhase()) absencePane.open(absence);
        }
        stage.setTitle(testProfile ? "Idle [profil de test]" : "Idle");
        Scene scene = new Scene(scaled, Settings.WindowSize.SMALL.width(), Settings.WindowSize.SMALL.height());
        applyAppearance();
        // Les touches sont lues avant les boutons : sinon, Espace irait d'abord au bouton qui a le focus.
        // Sauf quand le joueur écrit dans une barre de recherche : ses lettres ne sont pas des raccourcis.
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            cosmetics.input();
            if (GameApp.typing(scene) && !event.getCode().isModifierKey()) return;
            if (keyPressed(event.getCode())) event.consume();
        });
        scene.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
            // Une touche tenue cesse de se répéter même si son relâchement arrive dans une barre de recherche.
            heldKeys.remove(event.getCode());
            if (GameApp.typing(scene) && !event.getCode().isModifierKey()) return;
            if (keyReleased(event.getCode())) event.consume();
        });
        // Toucher à la souris remet à zéro le temps passé sans rien faire (le thème « Sépia »).
        scene.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, event -> cosmetics.input());
        scene.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_MOVED, event -> cosmetics.input());
        scene.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_DRAGGED, event -> cosmetics.input());
        scene.addEventFilter(javafx.scene.input.ScrollEvent.SCROLL, event -> cosmetics.input());
        // La fenêtre perd la main touche enfoncée : le relâchement ne viendra jamais, on l'anticipe.
        stage.focusedProperty().addListener((observable, before, focused) -> {
            if (!focused) {
                heldKeys.clear();    // les relâchements n'arriveront pas : plus rien ne se répète
                releaseDetailKey();
            }
        });
        stage.setScene(scene);
        applyMinWindow();
        settingsPage.setWindowSizes(this::applyWindowSize, this::windowSizeFits, this::currentWindowSize);
        placeWindow();
        stage.setOnCloseRequest(event -> closing());
        stage.show();
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
        public String previewImport() {
            String text;
            try {
                text = Clipboard.getSystemClipboard().getString();
            } catch (RuntimeException unavailable) {
                return "Rien à importer : le presse-papiers n'est pas accessible.";
            }
            try {
                SaveCodec.Preview preview = SaveCodec.preview(text);
                String newer = !preview.meta().gameVersion().isEmpty() && SaveStore.compareVersions(preview.meta().gameVersion(), Version.CURRENT) > 0
                        ? " Elle vient d'une version plus récente du jeu : ce qu'il ne connaît pas sera écarté." : "";
                return "Dans le presse-papiers : " + SaveStore.describe(preview) + "." + newer
                        + " Cliquer encore pour remplacer la partie en cours.";
            } catch (SaveCodec.Unreadable unreadable) {
                return "Rien à importer. " + unreadable.getMessage();
            }
        }

        @Override
        public int checkpointSlots() {
            return SaveStore.MAX_CHECKPOINTS;
        }

        @Override
        public String checkpointLine(int slot) {
            SaveStore.Checkpoint point = store.checkpoint(slot);
            if (point.used()) return SaveStore.describe(point.preview());
            return point.problem().isEmpty() ? "vide" : "illisible : " + point.problem();
        }

        @Override
        public boolean checkpointUsed(int slot) {
            return store.checkpoint(slot).used();
        }

        @Override
        public String saveCheckpoint(int slot, String name) {
            if (!store.enabled()) return "Jeu lancé sans sauvegarde : aucun point ne s'écrit.";
            return store.saveCheckpoint(game, slot, name) ? "Point " + slot + " enregistré." : "Le point n'a pas pu s'écrire : " + store.checkpointProblem();
        }

        @Override
        public String loadCheckpoint(int slot) {
            try {
                int dropped = store.loadCheckpoint(game, slot);
                afterLoad();
                return "Partie reprise : " + Format.duration(game.state().timePlayed()) + " de jeu."
                        + " La partie d'avant est mise de côté (« Avant la dernière reprise ») : la reprendre les échange de nouveau."
                        + (dropped > 0 ? " " + dropped + " chose(s) que ce jeu ne connaît pas ont été écartées." : "");
            } catch (SaveCodec.Unreadable unreadable) {
                return "Rien n'a changé. " + unreadable.getMessage();
            }
        }

        @Override
        public String deleteCheckpoint(int slot) {
            if (store.deleteCheckpoint(slot)) return "Point effacé.";
            return store.checkpointProblem().isEmpty() ? "Rien à effacer." : "Le point n'a pas pu s'effacer : " + store.checkpointProblem();
        }

        @Override
        public String records() {
            int known = 0;
            for (GameStats.Step step : GameStats.Step.values()) {
                if (!Double.isNaN(game.recordOf(step))) known++;
            }
            if (known == 0) return "Aucun record pour l'instant : le premier s'inscrit à la première fusion.";
            GameStats.Step next = game.nextStep();
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
            return copy(BugReport.of(game, store, new BugReport.Session(randomSeed,
                    tabName(selectedTab == Tab.SETTINGS ? gameTab : selectedTab), testProfile))) ? "Rapport de bug copié dans le presse-papiers : il n'y a plus qu'à le coller."
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
     * Les touches de raccourci tenues, avec le temps avant leur prochaine répétition. Plusieurs à la
     * fois se répètent ensemble : tenir M et F achète tout et fusionne en boucle.
     */
    final java.util.Map<KeyCode, Double> heldKeys = new java.util.HashMap<>();
}
