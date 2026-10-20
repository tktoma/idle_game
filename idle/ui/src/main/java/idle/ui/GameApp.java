package idle.ui;

import idle.core.Absence;
import idle.core.Game;
import idle.core.GameStats;
import idle.core.Resource;
import idle.core.bigbang.BangChallenge;
import idle.core.darkmatter.Challenge;
import idle.core.elements.PeriodicTable;
import idle.core.math.BigNum;
import idle.core.save.SaveCodec;
import idle.core.upgrades.Effect;
import idle.core.upgrades.Upgrade;
import idle.ui.atoms.AtomView;
import idle.ui.atoms.AtomsPage;
import idle.ui.automation.AutomationPage;
import idle.ui.bigbang.BigBangPage;
import idle.ui.collider.ColliderPage;
import idle.ui.collider.ColliderPane;
import idle.ui.common.Breakdown;
import idle.ui.common.Card;
import idle.ui.common.Detail;
import idle.ui.common.ElementText;
import idle.ui.common.Format;
import idle.ui.common.StableHeight;
import idle.ui.common.TabIcon;
import idle.ui.common.TileGrid;
import idle.ui.darkmatter.ChallengesPane;
import idle.ui.darkmatter.DarkMatterPage;
import idle.ui.universe.Dimension;
import idle.ui.universe.DimensionWindow;
import idle.ui.universe.UniversePage;
import idle.ui.pages.AchievementsPage;
import idle.ui.pages.SettingsPage;
import idle.ui.pages.StatsPage;
import idle.ui.particles.GeneratorPane;
import idle.ui.particles.ParticleView;
import idle.ui.probes.LootPane;
import idle.ui.probes.ProbesPane;
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
 * l'interface) et par le filtre du thème choisi ({@link ThemeFilter}). Des thèmes et des notations
 * se débloquent en faisant des choses très précises ({@link Cosmetics}).
 *
 * <p>La partie est sauvegardée dans un fichier ({@link SaveStore}) : relue au lancement, écrite
 * au rythme choisi dans les réglages (toutes les trente secondes d'origine) et à la fermeture. Au retour du joueur, une part du temps passé jeu
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

    /** Taille et marges des onglets du haut, selon la largeur de la fenêtre ; leur allure vient de leur rôle ({@link #tabStyle}). */
    private static final String WIDE_TAB = " -fx-font-size: 14px; -fx-padding: 9 10;";
    private static final String NARROW_TAB = " -fx-font-size: 12px; -fx-padding: 8 10;";
    /** En dessous de cette largeur de fenêtre, les onglets se resserrent pour tenir sur une ligne. */
    private static final double NARROW_WINDOW = 1140;
    /** La même limite une fois l'onglet « Big Bang » apparu : huit onglets au lieu de sept. */
    private static final double NARROW_WINDOW_WITH_BIG_BANG = 1280;
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
    private static final String SETTINGS_COLOR = "#d6dde6";
    /** Temps laissé pour le second clic qui confirme l'explosion, en secondes. */
    private static final double EXPLOSION_CONFIRM_SECONDS = 5;

    /** Les onglets du jeu. */
    private enum Tab { PARTICLES, ATOMS, AUTOMATION, DARK_MATTER, COLLIDER, BIG_BANG, UNIVERSE, ACHIEVEMENTS, STATS, SETTINGS, SKINS }

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
    private final Button colliderTab = new Button("Collisionneur");
    private final Button bigBangTab = new Button("Big Bang");
    /** L'onglet de l'univers (version 0.80), qui s'ouvre quand l'univers est formé : le quatrième acte. */
    private final Button universeTab = new Button("Univers");
    private final Button achievementsTab = new Button("Succès");
    private final Button statsTab = new Button("Statistiques");
    private final Button settingsTab = new Button("Réglages");
    /** Le régime rapide des générateurs (version 0.87), et la ligne de la synergie et de la prime. */
    private final Button regimeButton = new Button();
    private final Label secondLabel = new Label();
    private final HBox secondRow = new HBox(10, regimeButton, secondLabel);
    /** L'onglet des skins (version 0.87), sous les réglages : il s'ouvre dans le deuxième univers, après cinq fusions. */
    private final Button skinsTab = new Button("Skins");
    private final AtomView atomsTabIcon = new AtomView(22);
    /** Les icônes animées des autres onglets (version 0.59), chacune dans la couleur de son onglet. */
    private final TabIcon particlesIcon = new TabIcon(TabIcon.Kind.PARTICLES, PARTICLES_COLOR, 20);
    private final TabIcon automationIcon = new TabIcon(TabIcon.Kind.AUTOMATION, AUTOMATION_COLOR, 20);
    private final TabIcon darkMatterIcon = new TabIcon(TabIcon.Kind.DARK_MATTER, DARK_MATTER_COLOR, 20);
    private final TabIcon colliderIcon = new TabIcon(TabIcon.Kind.COLLIDER, ColliderPane.COLOR, 20);
    private final TabIcon bigBangIcon = new TabIcon(TabIcon.Kind.BIG_BANG, BIG_BANG_COLOR, 20);
    private final TabIcon universeIcon = new TabIcon(TabIcon.Kind.UNIVERSE, UniversePage.COLOR, 20);
    private final TabIcon achievementsIcon = new TabIcon(TabIcon.Kind.ACHIEVEMENTS, ACHIEVEMENTS_COLOR, 20);
    private final TabIcon statsIcon = new TabIcon(TabIcon.Kind.STATS, STATS_COLOR, 20);
    private final TabIcon settingsIcon = new TabIcon(TabIcon.Kind.SETTINGS, SETTINGS_COLOR, 20);
    private final TabIcon skinsIcon = new TabIcon(TabIcon.Kind.SKINS, idle.ui.pages.SkinsPage.COLOR, 20);
    private final TabIcon[] tabIcons = {particlesIcon, automationIcon, darkMatterIcon, colliderIcon, bigBangIcon, universeIcon,
            achievementsIcon, statsIcon, settingsIcon, skinsIcon};
    /**
     * Le rôle d'un onglet, qui décide de son allure : les actes du jeu (cadre plein), ce qui tourne
     * à côté (cadre en pointillés), et les pages pour s'y retrouver (sans cadre, rangées à part).
     */
    private enum TabRole { ACT, SIDE, META }
    /** Les onglets, dans l'ordre, et l'écart qui sépare les pages pour s'y retrouver des autres. */
    private final Button[] tabButtons = {particlesTab, atomsTab, automationTab, darkMatterTab, colliderTab, bigBangTab,
            universeTab, achievementsTab, statsTab, settingsTab, skinsTab};
    private final javafx.scene.layout.Region tabGap = new javafx.scene.layout.Region();
    /** À gauche, l'espace qui pousse les pages pour s'y retrouver en bas de la colonne. */
    private final javafx.scene.layout.Region sideSpacer = new javafx.scene.layout.Region();
    /** L'ordre des onglets, que le joueur change en les glissant ; gardé dans les réglages. */
    private final List<Tab> tabOrder = readTabOrder(settings.tabOrder());
    /** Le glisser-déposer : l'onglet saisi, le moment de l'appui, et vrai si l'appui a servi à glisser (pas à cliquer). */
    private Tab draggedTab;
    private long pressedAt;
    private boolean justDragged;
    private final HBox tabBar = new HBox(4, particlesTab, atomsTab, automationTab, darkMatterTab, colliderTab, bigBangTab,
            universeTab, tabGap, achievementsTab, statsTab, settingsTab, skinsTab);
    /** À gauche, le nom de l'onglet affiché quand il n'a pas de sous-onglets : la barre du haut garde sa hauteur. */
    private final Label tabTitle = new Label();
    /** À gauche : la colonne de droite, avec le haut de la fenêtre et les pages ; la colonne des onglets descend jusqu'en bas. */
    private final BorderPane mainColumn = new BorderPane();
    private StackPane pages;
    private final BorderPane particlesPage = new BorderPane();
    private final AtomsPage atomsPage = new AtomsPage(game, settings);
    private final AutomationPage automationPage = new AutomationPage(game);
    private final DarkMatterPage darkMatterPage = new DarkMatterPage(game);
    private final ColliderPage colliderPage = new ColliderPage(game);
    private final BigBangPage bigBangPage = new BigBangPage(game);
    private final UniversePage universePage = new UniversePage(game);
    /** La fenêtre d'une dimension (version 0.81), par-dessus tout le jeu, avec son bouton de retour. */
    private final DimensionWindow dimensionWindow = new DimensionWindow();
    private final AchievementsPage achievementsPage = new AchievementsPage(game);
    private final StatsPage statsPage = new StatsPage(game);
    private final SettingsPage settingsPage = new SettingsPage(settings, this::resetGame, this::applyAppearance, new SaveActions());
    private final idle.ui.pages.SkinsPage skinsPage = new idle.ui.pages.SkinsPage(game);
    private final ScrollPane skinsScroll = new ScrollPane(skinsPage);
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
    /** La disposition à gauche (version 0.57) : les onglets en colonne, et en haut les sous-onglets de l'onglet affiché. */
    private final VBox sideBar = new VBox(2);
    private final StackPane subTabHolder = new StackPane();
    /** Une barre de sous-onglets, et d'où elle vient dans sa page : la disposition d'avant l'y remet. */
    private record SubTabDock(Tab tab, javafx.scene.layout.Pane page, javafx.scene.layout.Pane bar, int index) {}
    private final List<SubTabDock> docks = new java.util.ArrayList<>();
    private VBox topBox;
    private boolean tabsLeftApplied = false;
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
    private final Sound sound = new Sound();
    // L'apparence : la taille de l'interface et le thème clair s'appliquent à toute la fenêtre.
    private ScaledPane scaled;
    private ThemeFilter themeFilter;
    private final StackPane flash = new StackPane();
    private final Circle shockwave = new Circle(24, Color.web(DARK_MATTER_COLOR));
    private final StackPane blast = new StackPane(flash, shockwave);
    /** Les animations plein écran des skins : l'explosion sous Vortex ou Pulsar, le Big Bang sous ses trois skins (version 0.89). */
    private final idle.ui.common.BurstView burst = new idle.ui.common.BurstView();
    /** Toute la fenêtre : l'animation des skins en prend la taille. */
    private StackPane windowPane;
    private boolean exploding = false;
    /** Secondes restantes pour confirmer l'explosion par un second clic ; 0 quand elle n'est pas demandée. */
    private double explosionArmed = 0;
    /** Atomes créés à la dernière image, pour repérer l'arrivée d'un nouvel atome ; −1 avant la première image. */
    private double lastTotalAtoms = -1;
    /** Le nombre de Big Bangs au dernier affichage, pour remarquer qu'il vient d'y en avoir un ; −1 avant le premier affichage. */
    private int lastBigBangs = -1;
    /** L'univers à la dernière image : 1 formé, 0 pas encore, −1 inconnu (au chargement, sans naissance à jouer). */
    private int hadUniverse = -1;
    /** Barre du profil de test, ou {@code null} en jeu normal. */
    private DebugBar debugBar;
    /** Guette les conditions des thèmes et des notations à débloquer. */
    private final Cosmetics cosmetics = new Cosmetics(game, settings);
    // La sauvegarde : le temps écoulé depuis la dernière écriture, et le retour après une absence.
    private double sinceSave = 0;
    private final AbsencePane absencePane = new AbsencePane(this::save);
    /** Les fenêtres d'explication : un onglet qui s'ouvre, une nouveauté du deuxième univers (version 0.89). */
    private final PopupPane popupPane = new PopupPane(game);
    private List<PopupPane.Entry> popupEntries;
    /** L'écran de fin (version 0.68) : une fois par partie, quand l'univers se forme. */
    private final EndPane endPane = new EndPane(game);
    /** Le butin d'une sonde, par-dessus la fenêtre : une carte à la fois. */
    private final LootPane lootPane = new LootPane(game, this::refresh);
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
        settingsPage.setSoundTest(() -> sound.test(settings.volume()));
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
        // Le deuxième univers (version 0.87) : le régime des générateurs, la synergie et la prime de cycle parfait.
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
        pages = new StackPane(particlesPage, atomsPage, automationScroll, darkMatterPage, colliderPage, bigBangPage,
                universePage, achievementsScroll, statsPage, settingsScroll, skinsScroll);
        skinsScroll.setFitToWidth(true);
        skinsScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        skinsScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        particlesTab.setOnAction(event -> open(Tab.PARTICLES));
        atomsTab.setOnAction(event -> open(Tab.ATOMS));
        automationTab.setOnAction(event -> open(Tab.AUTOMATION));
        darkMatterTab.setOnAction(event -> open(Tab.DARK_MATTER));
        colliderTab.setOnAction(event -> open(Tab.COLLIDER));
        bigBangTab.setOnAction(event -> open(Tab.BIG_BANG));
        universeTab.setOnAction(event -> open(Tab.UNIVERSE));
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
        dock(Tab.DARK_MATTER, darkMatterPage, darkMatterPage.subTabs());
        dock(Tab.COLLIDER, colliderPage, colliderPage.subTabs());
        dock(Tab.BIG_BANG, bigBangPage, bigBangPage.subTabs());
        dock(Tab.UNIVERSE, universePage, universePage.subTabs());
        universePage.setOnOpenDimension(number -> dimensionWindow.open(Dimension.of(number)));
        universePage.setOnOldTabs(shown -> refresh());
        // Rouvrir les paliers (version 0.87) : les anciens onglets reviennent, la partie de l'univers suivant commence.
        universePage.setOnNextUniverse(() -> {
            refresh();
            selectTab(Tab.PARTICLES);
            refresh();
        });
        dock(Tab.STATS, statsPage, statsPage.subTabs());
        dock(Tab.SETTINGS, settingsPage, settingsPage.subTabs());
        boolean testProfile = arguments.contains("--test");
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
        StackPane window = windowPane = new StackPane(root, dimensionWindow, blast, burst, toasts, lootPane, helpPane, endPane, popupPane, absencePane);
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
                if (pending < 1.0 / framesPerSecond() - FRAME_TOLERANCE) return;
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
                for (Cosmetic opened : cosmetics.frame(dt, settings.paused())) {
                    toasts.show(Cosmetics.announcement(opened), SETTINGS_COLOR);
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
        if (!loaded.notice().isEmpty()) toasts.show(loaded.notice(), SETTINGS_COLOR);
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
            if (typing(scene) && !event.getCode().isModifierKey()) return;
            if (keyPressed(event.getCode())) event.consume();
        });
        scene.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
            // Une touche tenue cesse de se répéter même si son relâchement arrive dans une barre de recherche.
            heldKeys.remove(event.getCode());
            if (typing(scene) && !event.getCode().isModifierKey()) return;
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

    /**
     * La plus petite fenêtre (version 0.92) : en dessous, plus rien ne tient. Elle suit la taille de
     * l'interface, sans dépasser l'écran ; une fenêtre déjà plus petite est agrandie jusqu'à elle.
     */
    private void applyMinWindow() {
        if (stage == null) return;
        double[] least = Settings.minWindow(settings.scale());
        javafx.geometry.Rectangle2D screen = screenBounds();
        if (screen != null) {
            least[0] = Math.min(least[0], screen.getWidth());
            least[1] = Math.min(least[1], screen.getHeight());
        }
        stage.setMinWidth(least[0]);
        stage.setMinHeight(least[1]);
        if (!stage.isMaximized()) {
            if (stage.getWidth() > 0 && stage.getWidth() < least[0]) stage.setWidth(least[0]);
            if (stage.getHeight() > 0 && stage.getHeight() < least[1]) stage.setHeight(least[1]);
        }
    }

    /** La place utilisable de l'écran où est la fenêtre, ou {@code null} sans écran à interroger. */
    private javafx.geometry.Rectangle2D screenBounds() {
        try {
            List<Screen> screens = Screen.getScreensForRectangle(stage.getX(), stage.getY(), Math.max(1, stage.getWidth()), Math.max(1, stage.getHeight()));
            return (screens.isEmpty() ? Screen.getPrimary() : screens.get(0)).getVisualBounds();
        } catch (RuntimeException unavailable) {
            return null;
        }
    }

    /** Vrai si ce format tient sur l'écran. */
    private boolean windowSizeFits(Settings.WindowSize size) {
        if (size.width() <= 0) return true;
        javafx.geometry.Rectangle2D screen = screenBounds();
        return screen == null || (size.width() <= screen.getWidth() && size.height() <= screen.getHeight());
    }

    /** Le format de la fenêtre en ce moment, ou {@code null} si elle a été tirée à la main. */
    private Settings.WindowSize currentWindowSize() {
        if (stage == null) return null;
        if (stage.isFullScreen()) return Settings.WindowSize.FULL;
        if (stage.isMaximized()) return Settings.WindowSize.MAXIMIZED;
        for (Settings.WindowSize size : Settings.WindowSize.values()) {
            if (size.width() > 0 && Math.abs(stage.getWidth() - size.width()) < 2
                    && Math.abs(stage.getHeight() - size.height()) < 2) return size;
        }
        return null;
    }

    /** Donne à la fenêtre un format prédéfini, recentrée sur son écran (version 0.92, plein écrans en 0.93). */
    void applyWindowSize(Settings.WindowSize size) {
        if (stage == null) return;
        if (size == Settings.WindowSize.FULL) {
            stage.setFullScreenExitHint("Échap pour quitter le plein écran");
            stage.setFullScreen(true);
            return;
        }
        stage.setFullScreen(false);
        if (size == Settings.WindowSize.MAXIMIZED) {
            stage.setMaximized(true);
            return;
        }
        stage.setMaximized(false);
        javafx.geometry.Rectangle2D screen = screenBounds();
        double width = size.width(), height = size.height();
        if (screen != null) {
            width = Math.min(width, screen.getWidth());
            height = Math.min(height, screen.getHeight());
        }
        stage.setWidth(width);
        stage.setHeight(height);
        if (screen != null) {
            stage.setX(screen.getMinX() + (screen.getWidth() - width) / 2);
            stage.setY(screen.getMinY() + (screen.getHeight() - height) / 2);
        }
    }

    /** Remet la fenêtre où elle était à la dernière fermeture, si cet endroit est encore sur un écran. */
    private void placeWindow() {
        double[] place = settings.window();
        // Première ouverture (version 0.94) : le format Petit, recentré et ramené à l'écran s'il est plus petit.
        if (place == null) {
            applyWindowSize(Settings.WindowSize.SMALL);
            return;
        }
        try {
            if (Screen.getScreensForRectangle(place[0], place[1], place[2], place[3]).isEmpty()) {
                applyWindowSize(Settings.WindowSize.SMALL);     // l'ancienne place n'est plus sur un écran
                return;
            }
        } catch (RuntimeException unavailable) {
            return;     // pas d'écran à interroger : on garde la place par défaut
        }
        stage.setX(place[0]);
        stage.setY(place[1]);
        double[] least = Settings.minWindow(settings.scale());
        stage.setWidth(Math.max(least[0], place[2]));
        stage.setHeight(Math.max(least[1], place[3]));
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
        popupPane.forget();
        absencePane.close();
        endPane.close();
        exploding = false;
        explosionArmed = 0;
        blast.setVisible(false);
        lastTotalAtoms = -1;
        lastBigBangs = -1;
        hadUniverse = -1;
        universePage.forgetOldTabs();
        dimensionWindow.close();
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
        if (debugBar != null) debugBar.cancelFastForward();
        game.reset();
        game.restored();
        save();
        absencePane.close();
        endPane.close();
        helpPane.close();
        popupPane.forget();
        toasts.clear();
        forgetOldPages();
        hadUniverse = 0;
        universePage.forgetOldTabs();
        dimensionWindow.close();
        controls.setVisible(false);
        showStartButton();
        selectTab(Tab.PARTICLES);
        refresh();
    }

    /**
     * Les anciens onglets oublient ce qu'ils montraient : la partie vient de repartir de zéro, par
     * « Recommencer » ou par la formation de l'univers (version 0.83).
     */
    private void forgetOldPages() {
        exploding = false;
        explosionArmed = 0;
        blast.setVisible(false);
        lastTotalAtoms = -1;
        generators.setCount(0);
        darkMatterPage.reset();
        bigBangPage.reset();
        lastBigBangs = 0;
    }

    /** Le régime, la synergie et la prime de cycle parfait : cachés tant que rien n'est ouvert. */
    private void refreshSecondUniverse() {
        boolean regime = game.isRegimeUnlocked();
        boolean synergy = game.isSynergyUnlocked();
        boolean perfect = game.isPerfectCycleUnlocked();
        secondRow.setVisible(regime || synergy || perfect);
        secondRow.setManaged(regime || synergy || perfect);
        regimeButton.setVisible(regime);
        regimeButton.setManaged(regime);
        double left = game.state().regimeLeft(), rest = game.state().regimeRest();
        regimeButton.setText(left > 0 ? "Régime rapide : +" + Math.round(Game.REGIME_GAIN * 100) + " % encore " + Format.wait(left)
                : rest > 0 ? "Régime : repos " + Format.wait(rest) : "Régime rapide : +" + Math.round(Game.REGIME_GAIN * 100) + " % pendant "
                        + Math.round(Game.REGIME_SECONDS) + " s");
        regimeButton.setDisable(left <= 0 && !game.canStartRegime());     // en cours : éclairé, pas grisé
        regimeButton.setStyle("-fx-font-size: 12px; -fx-padding: 4 12; -fx-cursor: hand; -fx-background-radius: 6; -fx-border-radius: 6;"
                + (left > 0 ? " -fx-text-fill: #0b0e14; -fx-background-color: " + PARTICLES_COLOR + ";"
                        : " -fx-text-fill: " + PARTICLES_COLOR + "; -fx-background-color: #101c2b; -fx-border-color: " + PARTICLES_COLOR + "88;"));
        java.util.List<String> parts = new java.util.ArrayList<>();
        if (synergy) parts.add("Synergie des rangs : +" + ElementText.percent(game.synergyMultiplier() - 1) + " à chaque générateur");
        if (perfect) parts.add(game.perfectCycleMultiplier() > 1
                ? "Cycle parfait : particules +" + Math.round(Game.PERFECT_GAIN * 100) + " % encore " + Format.wait(game.state().perfectLeft())
                : "Cycle parfait : " + game.state().perfectStreak() + " / " + Game.PERFECT_FUSIONS + " fusions sans le plafond d'atomes");
        secondLabel.setText(String.join("   |   ", parts));
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
        // Une touche tenue se répète : le système renvoie des appuis, mais c'est la fenêtre qui fixe le
        // rythme, pour toutes les touches tenues à la fois (M et F ensemble, par exemple).
        Shortcuts.Action action = settings.actionOf(code);
        if (action != null && heldKeys.containsKey(code)) return settings.shortcuts();
        boolean used = onKey(code);
        if (used) heldKeys.put(code, REPEATABLE.contains(action) ? REPEAT_DELAY : Double.POSITIVE_INFINITY);
        return used;
    }

    /** Une touche vient d'être relâchée : elle cesse de se répéter ; si c'est la touche de détail tenue, les détails se referment. */
    private boolean keyReleased(KeyCode code) {
        if (heldKeys.remove(code) != null) return true;
        if (code != settings.detailKey()) return false;
        releaseDetailKey();
        return true;
    }

    /**
     * Les touches de raccourci tenues, avec le temps avant leur prochaine répétition. Plusieurs à la
     * fois se répètent ensemble : tenir M et F achète tout et fusionne en boucle.
     */
    private final java.util.Map<KeyCode, Double> heldKeys = new java.util.HashMap<>();
    /** Attente avant la première répétition, puis intervalle entre deux répétitions, en secondes. */
    static final double REPEAT_DELAY = 0.3;
    static final double REPEAT_INTERVAL = 0.08;
    /** Les raccourcis qui se répètent tant que la touche est tenue ; les autres (exploser, couper…) ne jouent qu'une fois. */
    static final java.util.Set<Shortcuts.Action> REPEATABLE = java.util.EnumSet.of(Shortcuts.Action.SPEED,
            Shortcuts.Action.COUPLING, Shortcuts.Action.GENERATOR, Shortcuts.Action.BUY_ALL, Shortcuts.Action.FUSE,
            Shortcuts.Action.SYNTHESIZE, Shortcuts.Action.PULSE);

    /** Répète les raccourcis tenus dont le délai est écoulé. */
    private void repeatHeldKeys(double dt) {
        if (heldKeys.isEmpty() || !settings.shortcuts() || exploding || absencePane.isOpen() || helpPane.isOpen()) return;
        boolean any = false;
        for (java.util.Map.Entry<KeyCode, Double> entry : heldKeys.entrySet()) {
            double wait = entry.getValue();
            if (Double.isInfinite(wait)) continue;
            wait -= dt;
            Shortcuts.Action action = settings.actionOf(entry.getKey());
            // Au plus quelques répétitions par image : une image en retard ne déclenche pas une rafale.
            for (int burst = 0; wait <= 0 && burst < 4; burst++) {
                if (action != null && game.isStarted()) play(action);
                wait += REPEAT_INTERVAL;
                any = true;
            }
            entry.setValue(Math.max(wait, 0));
        }
        if (any) refresh();
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
                // Dans la partie secondaire, les anciens onglets sont partis : leurs raccourcis ne jouent plus.
                if (game.isStarted() && !universePhase()) play(action);
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
            case PULSE -> game.pulse();
            case CLAIM -> {
                // Le butin ouvert : la carte suivante ; sinon, celui de la première sonde arrivée.
                if (lootPane.isOpen()) lootPane.next();
                else if (!lootPane.openFirst()) game.claimProbeReward();
            }
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
        // Dans l'ordre affiché, celui que le joueur a pu choisir en glissant les onglets.
        int index = tabOrder.indexOf(selectedTab);
        for (int tries = 0; tries < tabOrder.size(); tries++) {
            index = Math.floorMod(index + direction, tabOrder.size());
            Tab tab = tabOrder.get(index);
            if (tabButtons[tab.ordinal()].isVisible()) {
                selectTab(tab);
                return;
            }
        }
    }

    // ------------------------------------------------------------------
    // L'ordre des onglets, qu'on change en les glissant (version 0.60)
    // ------------------------------------------------------------------

    /** Temps d'appui avant qu'un onglet ne se laisse glisser, en nanosecondes : un clic bref reste un clic. */
    private static final long HOLD_NANOS = 300_000_000L;

    /** L'ordre des onglets lu dans les réglages ; les onglets qu'il ne cite pas suivent, dans l'ordre d'origine. */
    private static List<Tab> readTabOrder(String saved) {
        List<Tab> order = new java.util.ArrayList<>();
        for (String name : saved.split(",")) {
            try {
                Tab tab = Tab.valueOf(name.trim());
                if (!order.contains(tab)) order.add(tab);
            } catch (IllegalArgumentException unknown) {
                // un onglet d'une autre version, ou rien : écarté
            }
        }
        // Un onglet que l'ordre ne cite pas (un nouveau, comme l'univers en 0.80) se range après celui qui le
        // précède d'habitude, pas au bout : les pages pour s'y retrouver restent à part.
        Tab[] natural = Tab.values();
        for (int k = 0; k < natural.length; k++) {
            if (order.contains(natural[k])) continue;
            int at = 0;
            for (int before = k - 1; before >= 0; before--) {
                int place = order.indexOf(natural[before]);
                if (place >= 0) {
                    at = place + 1;
                    break;
                }
            }
            order.add(at, natural[k]);
        }
        return order;
    }

    /**
     * Range les boutons des onglets dans leur barre, dans l'ordre choisi. Les pages pour s'y retrouver
     * (succès, statistiques, réglages) restent à part, après un écart, tant qu'elles sont toutes au bout.
     */
    private void layoutTabs() {
        javafx.scene.layout.Pane holder = tabsLeftApplied ? sideBar : tabBar;
        holder.getChildren().clear();
        int firstMeta = tabOrder.size();
        while (firstMeta > 0 && roleOf(tabOrder.get(firstMeta - 1)) == TabRole.META) firstMeta--;
        for (int i = 0; i < tabOrder.size(); i++) {
            if (i == firstMeta && firstMeta > 0) holder.getChildren().add(tabsLeftApplied ? sideSpacer : tabGap);
            holder.getChildren().add(tabButtons[tabOrder.get(i).ordinal()]);
        }
    }

    /** Branche le glisser-déposer sur le bouton d'un onglet : un appui long, puis on le déplace. */
    private void makeDraggable(Tab tab) {
        Button button = tabButtons[tab.ordinal()];
        button.setOnMousePressed(event -> {
            pressedAt = System.nanoTime();
            justDragged = false;
        });
        button.setOnMouseDragged(event -> {
            if (draggedTab == null) {
                if (System.nanoTime() - pressedAt < HOLD_NANOS) return;
                startTabDrag(tab);
            }
            dragTab(tabsLeftApplied ? event.getY() : event.getX());
        });
        button.setOnMouseReleased(event -> endTabDrag());
    }

    /** Un onglet est saisi : il pâlit, et suivra la souris. */
    void startTabDrag(Tab tab) {
        draggedTab = tab;
        justDragged = true;
        tabButtons[tab.ordinal()].setOpacity(0.6);
    }

    /**
     * L'onglet saisi suit la souris : {@code offset} est la position de la souris le long de la
     * barre, comptée depuis le bord de l'onglet. Passé la moitié de son voisin, il prend sa place.
     */
    void dragTab(double offset) {
        if (draggedTab == null) return;
        Button button = tabButtons[draggedTab.ordinal()];
        int at = tabOrder.indexOf(draggedTab);
        Tab next = neighbour(at, 1), previous = neighbour(at, -1);
        double size = tabsLeftApplied ? button.getHeight() : button.getWidth();
        if (next != null && offset > size + extent(next) / 2) {
            moveTab(draggedTab, tabOrder.indexOf(next));
        } else if (previous != null && offset < -extent(previous) / 2) {
            moveTab(draggedTab, tabOrder.indexOf(previous));
        }
    }

    /** L'onglet lâché : son nouvel ordre est gardé dans les réglages. */
    void endTabDrag() {
        if (draggedTab == null) return;
        tabButtons[draggedTab.ordinal()].setOpacity(1);
        draggedTab = null;
        StringBuilder saved = new StringBuilder();
        for (Tab tab : tabOrder) saved.append(saved.length() == 0 ? "" : ",").append(tab.name());
        settings.setTabOrder(saved.toString());
    }

    /** Le premier onglet affiché dans ce sens, ou {@code null} au bout de la barre. */
    private Tab neighbour(int from, int direction) {
        for (int i = from + direction; i >= 0 && i < tabOrder.size(); i += direction) {
            if (tabButtons[tabOrder.get(i).ordinal()].isManaged()) return tabOrder.get(i);
        }
        return null;
    }

    private double extent(Tab tab) {
        Button button = tabButtons[tab.ordinal()];
        return tabsLeftApplied ? button.getHeight() : button.getWidth();
    }

    /** Déplace un onglet à cette place de l'ordre, et refait la barre. */
    void moveTab(Tab tab, int to) {
        tabOrder.remove(tab);
        tabOrder.add(Math.max(0, Math.min(to, tabOrder.size())), tab);
        layoutTabs();
    }

    /** Remet les onglets dans leur ordre d'origine. */
    void resetTabOrder() {
        tabOrder.clear();
        tabOrder.addAll(List.of(Tab.values()));
        settings.setTabOrder("");
        layoutTabs();
    }

    /** L'ordre des onglets : pour les vérifications. */
    List<String> tabOrderNames() {
        List<String> names = new java.util.ArrayList<>();
        for (Tab tab : tabOrder) names.add(tab.name());
        return names;
    }

    /** Passe au sous-onglet suivant ou précédent de l'onglet affiché, s'il en a. */
    private void stepSubTab(int direction) {
        switch (selectedTab) {
            case ATOMS -> atomsPage.step(direction);
            case DARK_MATTER -> darkMatterPage.step(direction);
            case COLLIDER -> colliderPage.step(direction);
            case BIG_BANG -> bigBangPage.step(direction);
            case UNIVERSE -> universePage.step(direction);
            case STATS -> statsPage.step(direction);
            default -> { }
        }
    }

    /**
     * Un clic sur le bouton d'explosion. Avec la confirmation des réglages, le premier clic arme
     * le bouton et c'est le second, dans les secondes qui suivent, qui déclenche l'explosion.
     */
    private void requestExplosion() {
        if (!game.canExplode() || exploding) return;
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
        if (!settings.effect(Settings.Effect.FLASH)) {
            // Sans l'éclair : pas d'onde ni d'éclair, l'explosion a lieu tout de suite.
            game.explode();
            selectTab(Tab.DARK_MATTER);
            refresh();
            return;
        }
        exploding = true;
        blast.setVisible(true);
        flash.setOpacity(0);
        // Le skin de l'animation (version 0.89) : une spirale ou un pulsar à la place de l'onde, et l'éclair à sa couleur.
        idle.core.universe.Skin look = game.chosenSkin(idle.core.universe.SkinVisual.DARK_ANIMATION);
        boolean skinned = look == idle.core.universe.Skin.DARK_VORTEX || look == idle.core.universe.Skin.DARK_PULSAR;
        flash.setStyle("-fx-background-color: " + (look == idle.core.universe.Skin.DARK_VORTEX ? "#12061f"
                : look == idle.core.universe.Skin.DARK_PULSAR ? "#e8fdff" : "#ffffff") + ";");
        shockwave.setVisible(!skinned);
        if (skinned) burst.play(look, 2.4);

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

    /** Applique la taille de l'interface, le thème et la disposition des onglets choisis dans les réglages. */
    private void applyAppearance() {
        if (scaled == null) return;      // la fenêtre n'est pas encore construite
        scaled.setScale(settings.scale().factor());
        applyMinWindow();
        themeFilter.apply(settings.theme());
        applyTabLayout();
        // L'ordre des onglets a pu être remis à l'origine dans les réglages.
        List<Tab> wanted = readTabOrder(settings.tabOrder());
        if (!wanted.equals(tabOrder)) {
            tabOrder.clear();
            tabOrder.addAll(wanted);
            layoutTabs();
        }
    }

    /** Retient une barre de sous-onglets et sa place dans sa page. */
    private void dock(Tab tab, javafx.scene.layout.Pane page, javafx.scene.layout.Pane bar) {
        docks.add(new SubTabDock(tab, page, bar, page.getChildren().indexOf(bar)));
    }

    /**
     * Range les onglets selon le réglage. À gauche : les onglets en colonne, et la barre du haut
     * prend les sous-onglets de l'onglet affiché. En haut : la disposition d'avant, chaque barre
     * de sous-onglets revenant à sa place dans sa page.
     */
    private void applyTabLayout() {
        boolean left = settings.tabsLeft();
        if (left == tabsLeftApplied || topBox == null) return;
        tabsLeftApplied = left;
        tabBar.getChildren().clear();
        sideBar.getChildren().clear();
        layoutTabs();
        if (left) {
            int at = topBox.getChildren().indexOf(tabBar);
            topBox.getChildren().set(at, subTabHolder);
            for (SubTabDock dock : docks) {
                dock.page().getChildren().remove(dock.bar());
                subTabHolder.getChildren().add(dock.bar());
            }
            // La colonne des onglets descend du haut au bas de la fenêtre : ni le bouton d'explosion ni la
            // barre du haut ne la font bouger.
            root.setTop(null);
            root.setCenter(null);
            mainColumn.setTop(topBox);
            mainColumn.setCenter(pages);
            root.setCenter(mainColumn);
            root.setLeft(sideBar);
        } else {
            int at = topBox.getChildren().indexOf(subTabHolder);
            topBox.getChildren().set(at, tabBar);
            for (SubTabDock dock : docks) {
                subTabHolder.getChildren().remove(dock.bar());
                dock.page().getChildren().add(Math.min(dock.index(), dock.page().getChildren().size()), dock.bar());
                dock.bar().setVisible(true);
                dock.bar().setManaged(true);
            }
            root.setLeft(null);
            mainColumn.setTop(null);
            mainColumn.setCenter(null);
            root.setCenter(pages);
            root.setTop(topBox);
        }
        showSubTabs();
        styleTabs();
    }

    /** Dans la disposition à gauche, seule la barre de l'onglet affiché se montre en haut. */
    private void showSubTabs() {
        if (!tabsLeftApplied) return;
        boolean any = false;
        for (SubTabDock dock : docks) {
            boolean shown = dock.tab() == selectedTab;
            dock.bar().setVisible(shown);
            dock.bar().setManaged(shown);
            any |= shown;
        }
        // Sans sous-onglets, la barre garde sa place et montre le nom de l'onglet.
        tabTitle.setVisible(!any);
        tabTitle.setText(tabName(selectedTab));
        tabTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: " + tabColor(selectedTab) + ";");
    }

    /**
     * Vrai dans la partie secondaire (version 0.81) : l'univers est formé, et les anciens onglets sont
     * cachés (sauf si l'outil d'essai les a fait revenir).
     */
    private boolean universePhase() {
        return game.hasCosmos(idle.core.sky.Cosmos.UNIVERSE) && !universePage.showsOldTabs();
    }

    /**
     * Les fenêtres d'explication (version 0.89) : chaque onglet qui s'ouvre, puis les nouveautés du
     * deuxième univers. Elles attendent qu'aucune autre fenêtre n'ait la main.
     */
    private void checkPopups() {
        if (popupEntries == null) {
            List<PopupPane.Entry> entries = new java.util.ArrayList<>();
            for (Tab tab : Tab.values()) {
                String text = PopupPane.tabText(tab.name());
                if (text == null) continue;
                Button button = tabButtons[tab.ordinal()];
                entries.add(new PopupPane.Entry("tab:" + tab.name(), "Nouvel onglet", tabName(tab), text, tabColor(tab),
                        () -> game.isStarted() && button.isVisible()));
            }
            entries.addAll(PopupPane.secondUniverse(game));
            popupEntries = List.copyOf(entries);
        }
        boolean busy = absencePane.isVisible() || endPane.isOpen() || exploding
                || (selectedTab == Tab.UNIVERSE && universePage.isBirthPlaying());
        popupPane.check(popupEntries, busy);
    }

    /** Les fenêtres d'explication : pour les vérifications. */
    PopupPane popupPane() {
        return popupPane;
    }

    /** Le nom d'un onglet, sans compteur. */
    private static String tabName(Tab tab) {
        return switch (tab) {
            case PARTICLES -> "Particules";
            case ATOMS -> "Atomes";
            case AUTOMATION -> "Automatisation";
            case DARK_MATTER -> "Matière noire";
            case COLLIDER -> "Collisionneur";
            case BIG_BANG -> "Big Bang";
            case UNIVERSE -> "Univers";
            case ACHIEVEMENTS -> "Succès";
            case STATS -> "Statistiques";
            case SETTINGS -> "Réglages";
            case SKINS -> "Skins";
        };
    }

    private static String tabColor(Tab tab) {
        return switch (tab) {
            case PARTICLES -> PARTICLES_COLOR;
            case ATOMS -> ATOMS_COLOR;
            case AUTOMATION -> AUTOMATION_COLOR;
            case DARK_MATTER -> DARK_MATTER_COLOR;
            case COLLIDER -> ColliderPane.COLOR;
            case BIG_BANG -> BIG_BANG_COLOR;
            case UNIVERSE -> UniversePage.COLOR;
            case ACHIEVEMENTS -> ACHIEVEMENTS_COLOR;
            case STATS -> STATS_COLOR;
            case SETTINGS -> SETTINGS_COLOR;
            case SKINS -> idle.ui.pages.SkinsPage.COLOR;
        };
    }

    /** Le nom de l'onglet affiché en haut, dans la disposition à gauche : pour les vérifications. */
    String tabTitleText() {
        return tabTitle.isVisible() ? tabTitle.getText() : "";
    }

    /** Vrai si les onglets sont à gauche : pour les vérifications. */
    boolean tabsOnLeft() {
        return tabsLeftApplied;
    }

    /**
     * Annonce ce qui vient de se passer dans le jeu, et fait vieillir les messages déjà affichés.
     * Coupées dans les réglages, les notifications sont tout de même relevées, pour ne pas toutes
     * tomber d'un coup le jour où on les rallume.
     */
    private void announce(double dt) {
        List<String> news = notifier.poll();
        // Dans la partie secondaire, ce qui se passe dans les anciens onglets, cachés, n'est plus annoncé :
        // seuls les succès le sont encore. Le journal, lui, garde tout.
        if (universePhase()) {
            for (String text : news) if (!text.startsWith("Succès")) EventLog.of(game).add(game, text);
            news = news.stream().filter(text -> text.startsWith("Succès")).toList();
        }
        sound.announce(news, settings.sound(), settings.volume());
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
        if (text.startsWith("Big Bang") || text.startsWith("Molécules") || text.startsWith("Espace") || text.startsWith("Astre mûr") || text.contains("astres mûrs")
                || text.startsWith("Rassemblement automatique") || text.startsWith("Formation automatique")) return BIG_BANG_COLOR;
        if (text.startsWith("Succès du Big Bang")) return BIG_BANG_COLOR;
        if (text.startsWith("Succès") && !text.startsWith("Succès du collisionneur")) return ACHIEVEMENTS_COLOR;
        if (text.startsWith("Skin débloqué")) return idle.ui.pages.SkinsPage.COLOR;
        if (text.startsWith("Sonde") || text.contains("sondes revenues")) return ProbesPane.COLOR;
        if (text.startsWith("Collision") || text.startsWith("Palier du collisionneur") || text.startsWith("Succès du collisionneur")) return ColliderPane.COLOR;
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
        // L'appui a servi à glisser l'onglet : on ne l'ouvre pas en le lâchant.
        if (justDragged) {
            justDragged = false;
            return;
        }
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
        colliderPage.setVisible(tab == Tab.COLLIDER);
        bigBangPage.setVisible(tab == Tab.BIG_BANG);
        universePage.setVisible(tab == Tab.UNIVERSE);
        achievementsScroll.setVisible(tab == Tab.ACHIEVEMENTS);
        statsPage.setVisible(tab == Tab.STATS);
        settingsScroll.setVisible(tab == Tab.SETTINGS);
        skinsScroll.setVisible(tab == Tab.SKINS);
        showSubTabs();
        styleTabs();
    }

    /** Colore les onglets : celui qui est affiché est éclairé, et tous se resserrent dans une fenêtre étroite. */
    private void styleTabs() {
        Tab[] tabs = Tab.values();
        for (int i = 0; i < tabs.length; i++) {
            tabButtons[i].setStyle(tabStyle(tabColor(tabs[i]), selectedTab == tabs[i], roleOf(tabs[i])));
        }
        for (TabIcon icon : tabIcons) icon.setBright(false);
        TabIcon shown = iconOf(selectedTab);
        if (shown != null) shown.setBright(true);
        // Sept ou huit onglets ne tiennent pas en toutes lettres dans une fenêtre étroite : en haut, ils
        // perdent alors leur icône et se serrent. À gauche, ils ont toujours la place.
        statsTab.setText(narrow ? "Stats" : "Statistiques");
        boolean icons = tabsLeftApplied || !narrow;
        javafx.scene.Node[] graphics = {particlesIcon, atomsTabIcon, automationIcon, darkMatterIcon, colliderIcon, bigBangIcon, universeIcon, achievementsIcon, statsIcon, settingsIcon, skinsIcon};
        for (int i = 0; i < tabButtons.length; i++) tabButtons[i].setGraphic(icons ? graphics[i] : null);
        tabBar.setSpacing(narrow ? 1 : 4);
        tabGap.setMinWidth(narrow ? 6 : 18);
    }

    private static TabRole roleOf(Tab tab) {
        return switch (tab) {
            case PARTICLES, ATOMS, DARK_MATTER, BIG_BANG, UNIVERSE -> TabRole.ACT;
            case AUTOMATION, COLLIDER -> TabRole.SIDE;
            case ACHIEVEMENTS, STATS, SETTINGS, SKINS -> TabRole.META;
        };
    }

    private TabIcon iconOf(Tab tab) {
        return switch (tab) {
            case PARTICLES -> particlesIcon;
            case ATOMS -> null;
            case AUTOMATION -> automationIcon;
            case DARK_MATTER -> darkMatterIcon;
            case COLLIDER -> colliderIcon;
            case BIG_BANG -> bigBangIcon;
            case UNIVERSE -> universeIcon;
            case ACHIEVEMENTS -> achievementsIcon;
            case STATS -> statsIcon;
            case SETTINGS -> settingsIcon;
            case SKINS -> skinsIcon;
        };
    }

    /**
     * L'allure d'un onglet selon son rôle (version 0.59). Les actes ont un cadre plein dans leur
     * couleur ; ce qui tourne à côté, un cadre en pointillés ; les pages pour s'y retrouver, aucun
     * cadre tant qu'elles ne sont pas affichées. L'onglet affiché est rempli d'un voile de sa couleur
     * et brille. L'épaisseur du cadre, les marges et la police ne changent jamais d'un état à
     * l'autre : rien ne bouge au clic.
     */
    private String tabStyle(String color, boolean selected, TabRole role) {
        String base = "-fx-cursor: hand; -fx-border-width: 1.5;" + (tabsLeftApplied
                ? " -fx-font-size: 14px; -fx-padding: 8 12; -fx-alignment: center-left; -fx-min-width: 176; -fx-max-width: 176;"
                        + " -fx-background-radius: 8; -fx-border-radius: 8;"
                : (narrow ? NARROW_TAB : WIDE_TAB) + " -fx-background-radius: 8 8 0 0; -fx-border-radius: 8 8 0 0;");
        String border = switch (role) {
            case ACT -> " -fx-border-style: solid; -fx-border-color: " + fade(color, selected ? 1 : 0.45) + ";";
            case SIDE -> " -fx-border-style: segments(5, 4); -fx-border-color: " + fade(color, selected ? 1 : 0.5) + ";";
            case META -> " -fx-border-style: solid; -fx-border-color: " + (selected ? fade(color, 0.8) : "transparent") + ";";
        };
        String fill = selected
                ? " -fx-text-fill: " + color + "; -fx-background-color: " + fade(color, 0.14) + ";"
                        + " -fx-effect: dropshadow(gaussian, " + fade(color, 0.55) + ", 12, 0.2, 0, 0);"
                : " -fx-text-fill: " + (role == TabRole.META ? "#7d8fa3" : "#a9bacb") + "; -fx-background-color: "
                        + (role == TabRole.META ? "transparent" : "#111824") + "; -fx-effect: null;";
        return base + border + fill;
    }

    /** Une couleur « #rrggbb » avec une transparence : « rgba(…) » pour le style. */
    private static String fade(String color, double alpha) {
        Color parsed = Color.web(color);
        return "rgba(" + Math.round(parsed.getRed() * 255) + ", " + Math.round(parsed.getGreen() * 255) + ", "
                + Math.round(parsed.getBlue() * 255) + ", " + alpha + ")";
    }

    /** Un atome vient d'être créé : l'onglet « Atomes » bat quelques fois pour attirer l'œil. */
    private void celebrateNewAtom() {
        if (!settings.effect(Settings.Effect.ATOM_PULSE)) return;
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
        if (windowPane != null) burst.frame(dt, windowPane.getWidth(), windowPane.getHeight());
        idle.ui.particles.ParticleView.setSkin(game.chosenSkin(idle.core.universe.SkinVisual.PARTICLE_FUSION));
        // L'atome de l'onglet tourne comme les autres icônes : immobile quand elles le sont.
        atomsTabIcon.frame(settings.effect(Settings.Effect.TAB_ICONS) ? dt : 0);
        // Les icônes suivent ce qu'elles montrent : les particules tournent avec la production, les
        // faisceaux du collisionneur avec leur vitesse.
        double production = Math.min(game.productionPerSecond().toDouble(), 1e12);
        particlesIcon.setSpeed(1 + 3 * ParticleView.intensity(production));
        colliderIcon.setSpeed(Game.RESONANCE_PERIOD / game.colliderPeriod());
        for (TabIcon icon : tabIcons) icon.frame(dt, settings.effect(Settings.Effect.TAB_ICONS));
        if (selectedTab == Tab.ATOMS) {
            atomsPage.frame(dt);
        }
        if (selectedTab == Tab.DARK_MATTER) {
            darkMatterPage.frame(dt, timeFactor());
        }
        if (selectedTab == Tab.COLLIDER) {
            colliderPage.frame(dt);
        }
        if (selectedTab == Tab.BIG_BANG) {
            bigBangPage.frame(dt);
        }
        if (selectedTab == Tab.UNIVERSE) {
            universePage.frame(dt, settings.effect(Settings.Effect.UNIVERSE_MOTION));
        }
        dimensionWindow.frame(dt, settings.effect(Settings.Effect.UNIVERSE_MOTION));
        lootPane.frame(dt);
        if (selectedTab != Tab.PARTICLES) {
            return;
        }
        if (settings.effect(Settings.Effect.AMBIENT)) generators.animateBackground(dt, Math.min(game.productionPerSecond().toDouble(), 1e12));
        else generators.clearBackground();
        // Chaque générateur a sa propre vitesse : les éléments du tableau périodique en accélèrent certains.
        List<ParticleView> views = generators.views();
        for (int i = 0; i < views.size(); i++) {
            double speed = Math.min(game.speed(i).toDouble() * timeFactor(), 1e9);
            views.get(i).frame(dt, game.state().formation(i), speed);
        }
    }

    /** Marge sur la durée d'une image : un écran à 60 Hz n'arrive pas pile à 1/60 s, et le plafond de 60 ne doit sauter aucune image. */
    private static final double FRAME_TOLERANCE = 0.004;
    /** Images par seconde quand la fenêtre est derrière une autre, et quand elle est réduite, avec l'économie d'énergie. */
    public static final int BACKGROUND_FPS = 4;
    public static final int ICONIFIED_FPS = 1;

    /** Images par seconde visées en ce moment : le réglage, ou moins si la fenêtre n'est pas au premier plan. */
    private int framesPerSecond() {
        int chosen = settings.frameRate().perSecond();
        if (!settings.saveEnergy() || stage == null) return chosen;
        if (stage.isIconified()) return Math.min(chosen, ICONIFIED_FPS);
        if (!stage.isFocused()) return Math.min(chosen, BACKGROUND_FPS);
        return chosen;
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
        // En colonne, les onglets ont toute leur largeur : pas de noms courts.
        boolean nowNarrow = !tabsLeftApplied && root.getWidth() > 0 && root.getWidth() < (bigBang ? NARROW_WINDOW_WITH_BIG_BANG : NARROW_WINDOW);
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
            refreshSecondUniverse();

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
        // La partie secondaire (version 0.81) : une fois l'univers formé, tous les anciens onglets disparaissent,
        // particules et collisionneur compris ; restent l'univers et les pages pour s'y retrouver.
        boolean phase = universePhase();
        particlesTab.setVisible(!phase);
        particlesTab.setManaged(!phase);
        boolean hasAtoms = game.state().totalAtoms().sign() > 0 && !phase;
        boolean darkMatter = game.isDarkMatterUnlocked() && !phase;
        atomsTab.setVisible(hasAtoms);
        atomsTab.setManaged(hasAtoms);
        readiness.frame();
        atomsTab.setText("Atomes (" + Format.count(game.state().atoms()) + ")");
        atomsPage.refresh(selectedTab == Tab.ATOMS);

        // L'onglet « Automatisation » n'existe qu'une fois un automatisme débloqué, ordinaire ou de matière noire.
        boolean automation = AutomationPage.hasContent(game) && !phase;
        automationTab.setVisible(automation);
        automationTab.setManaged(automation);
        automationTab.setText((narrow ? "Auto." : "Automatisation") + (game.isAutomationPaused() && game.hasAnyAutomation() ? " (coupée)" : ""));
        if (selectedTab == Tab.AUTOMATION) automationPage.refresh();

        // L'onglet « Matière noire » n'existe qu'après la première explosion.
        darkMatterTab.setVisible(darkMatter);
        darkMatterTab.setManaged(darkMatter);
        darkMatterTab.setText((narrow ? "M. noire (" : "Matière noire (") + Format.darkMatter(game.state().darkMatter()) + ")"
                + Readiness.dot(readiness.darkMatter()));
        darkMatterPage.refresh(selectedTab == Tab.DARK_MATTER);

        // L'onglet du collisionneur s'ouvre à la première explosion et ne se referme plus.
        boolean collider = game.isColliderUnlocked() && !phase;
        colliderTab.setVisible(collider);
        colliderTab.setManaged(collider);
        colliderTab.setText((narrow ? "Collis." : "Collisionneur") + Readiness.dot(ColliderPage.ready(game)));
        if (selectedTab == Tab.COLLIDER) colliderPage.refresh();

        // L'onglet « Big Bang » n'existe qu'après le premier Big Bang. Il vient d'y en avoir un : on y va,
        // comme une explosion mène à l'onglet « Matière noire ».
        bigBangTab.setVisible(bigBang && !phase);
        bigBangTab.setManaged(bigBang && !phase);
        bigBangTab.setText((narrow ? "Big Bang" : "Big Bang (" + game.bigBangs() + ")") + Readiness.dot(readiness.bigBang()));
        if (lastBigBangs >= 0 && game.bigBangs() > lastBigBangs) {
            selectTab(Tab.BIG_BANG);
            // Le skin du Big Bang (version 0.89) : son animation plein écran.
            idle.core.universe.Skin bang = game.chosenSkin(idle.core.universe.SkinVisual.BIG_BANG);
            if (bang != null && settings.effect(Settings.Effect.FLASH)) burst.play(bang, 3.5);
        }
        lastBigBangs = game.bigBangs();
        if (selectedTab == Tab.BIG_BANG) bigBangPage.refresh();
        if (selectedTab == Tab.UNIVERSE) universePage.refresh();

        // L'onglet « Univers » (version 0.80) s'ouvre quand l'univers est formé. S'il vient de se former,
        // on y va et sa naissance se joue ; l'écran de fin attend qu'elle soit finie.
        boolean universe = game.hasCosmos(idle.core.sky.Cosmos.UNIVERSE);
        // Pendant la partie de l'univers suivant (version 0.87), l'onglet reste : les univers déjà formés sont là.
        universeTab.setVisible(game.universes() > 0);
        universeTab.setManaged(game.universes() > 0);
        if (universe && hadUniverse == 0) {
            // Former l'univers a tout remis à zéro (version 0.83) : les anciens onglets, cachés, repartent de rien.
            forgetOldPages();
            selectTab(Tab.UNIVERSE);
            universePage.playBirth();
        }
        hadUniverse = universe ? 1 : 0;

        // Les succès et les statistiques ne se recalculent que lorsqu'on les regarde. L'onglet des succès
        // n'existe que si le jeu en a.
        boolean achievements = !game.achievements().isEmpty();
        achievementsTab.setVisible(achievements);
        achievementsTab.setManaged(achievements);
        if (selectedTab == Tab.ACHIEVEMENTS) achievementsPage.refresh();
        if (selectedTab == Tab.STATS) statsPage.refresh();
        settingsTab.setText(!settings.paused() ? "Réglages" : narrow ? "En pause" : "Réglages (pause)");
        // Les skins (version 0.87) : l'onglet s'ouvre après cinq fusions du deuxième univers, et ne se referme plus.
        boolean skins = game.isSkinsUnlocked();
        skinsTab.setVisible(skins);
        skinsTab.setManaged(skins);
        skinsTab.setText(narrow ? "Skins" : "Skins (" + game.skinsUnlocked() + ")");
        if (selectedTab == Tab.SKINS) skinsPage.refresh();

        // Un onglet qui vient de disparaître (après une explosion) ne peut pas rester affiché.
        if ((selectedTab == Tab.ATOMS && !hasAtoms) || (selectedTab == Tab.AUTOMATION && !automation)
                || (selectedTab == Tab.DARK_MATTER && !darkMatter) || (selectedTab == Tab.BIG_BANG && (!bigBang || phase))
                || (selectedTab == Tab.UNIVERSE && !universe) || (selectedTab == Tab.PARTICLES && phase)
                || (selectedTab == Tab.COLLIDER && !collider)) {
            selectTab(phase ? Tab.UNIVERSE : Tab.PARTICLES);
            // L'onglet « Particules » n'était plus recopié tant qu'il était caché : il l'est maintenant, avec tout le reste.
            refresh();
            return;
        }

        // Le bouton d'explosion s'allume quand les 118 éléments sont découverts. Après la première explosion,
        // il garde sa place, éteint, entre deux explosions : sans cela, quand les explosions s'enchaînent,
        // toute la fenêtre montait et descendait à chaque fois, et les clics tombaient à côté.
        Challenge challenge = game.activeChallenge();
        boolean canExplode = game.canExplode() && !exploding;
        boolean keepPlace = game.isDarkMatterUnlocked() && game.isStarted() && !canExplode;
        if (keepPlace) {
            int missing = PeriodicTable.ELEMENTS.size() - game.discoveredElements();
            explosionButton.setText(exploding ? "Explosion…"
                    : "Prochaine explosion : encore " + missing + (missing > 1 ? " éléments à découvrir" : " élément à découvrir"));
        }
        // Son texte n'est composé que lorsqu'il se voit.
        if (canExplode) {
            explosionButton.setText(explosionArmed > 0
                    ? "Cliquer encore pour confirmer l'explosion : tout repart de zéro (" + (int) Math.ceil(explosionArmed) + " s)"
                    : game.isChallengeReplay()
                    ? "Terminer le défi « " + challenge.name() + " » en " + Format.duration(game.stats().runTime())
                            + Detail.only(". Déjà réussi, il ne compte que pour son temps : ni matière noire, ni tableau plus lourd.")
                    : "Faire exploser le tableau : +" + Format.darkMatter(game.nextExplosionDarkMatter()) + " matière noire"
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
        themeFilter.fit();
        // L'objectif du moment, sous les onglets. Caché, il laisse sa place vide : le rappel reste à droite.
        boolean goal = settings.showGoal() && game.isStarted();
        // Après un premier Big Bang, deux objectifs se suivent de front : celui de la partie, et celui du troisième acte.
        String act = goal ? Goals.act(game) : "";
        // Entre les deux, l'avancée du prochain Big Bang : ses cinq conditions, sans ouvrir l'arbre.
        String bang = goal ? NextReset.bigBangProgress(game) : "";
        // Le mode chrono : l'écart au record sur la prochaine étape, quand il y a un record à battre.
        String chrono = goal && settings.chrono() ? Goals.chrono(game) : "";
        goalLabel.setText(goal && phase ? "Quatrième acte : l'univers. " + game.universes() + (game.universes() > 1 ? " univers" : " univers")
                + (game.isDimensionsUnlocked() ? ", " + game.dimensionsUnlocked() + " dimensions sur " + Game.MAX_DIMENSIONS : "")
                + ". La suite viendra."
                : goal ? "Objectif : " + Goals.withEstimate(game, game.stats())
                + (bang.isEmpty() ? "" : "   |   Prochain Big Bang : " + bang)
                + (act.isEmpty() ? "" : game.hasCosmos(idle.core.sky.Cosmos.UNIVERSE)
                        ? "   |   Quatrième acte : la suite viendra" : "   |   Troisième acte : " + act)
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
        BangChallenge bangChallenge = game.activeBangChallenge();
        challengeLabel.setVisible((challenge != null || bangChallenge != null) && !phase);
        challengeLabel.setManaged((challenge != null || bangChallenge != null) && !phase);
        if (challenge != null || bangChallenge != null) {
            String ordinary = challenge == null ? "" : "Défi « " + challenge.name() + " » depuis " + Format.duration(game.stats().runTime())
                    + " : " + (Detail.shown() ? ChallengesPane.rule(challenge) : ChallengesPane.shortRule(challenge));
            String constraint = bangChallenge == null ? "" : "Défi de Big Bang « " + bangChallenge.label() + " » depuis "
                    + Format.duration(game.bangChallengeTime()) + " : " + (Detail.shown() ? bangChallenge.rule()
                    : lowerFirst(bangChallenge.brief()) + ", jusqu'au prochain Big Bang");
            challengeLabel.setText(constraint + (constraint.isEmpty() || ordinary.isEmpty() ? "" : "\n") + ordinary);
        }
        explosionButton.setVisible((canExplode || keepPlace) && !phase);
        explosionButton.setManaged((canExplode || keepPlace) && !phase);
        explosionButton.setDisable(!canExplode);
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
