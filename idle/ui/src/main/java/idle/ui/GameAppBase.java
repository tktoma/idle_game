package idle.ui;

import idle.core.Absence;
import idle.core.Enums;
import idle.core.Game;
import idle.core.Resource;
import idle.core.state.GameStats;
import idle.core.save.SaveCodec;
import idle.core.upgrades.Upgrade;
import idle.ui.atoms.AtomView;
import idle.ui.atoms.AtomsPage;
import idle.ui.automation.AutomationPage;
import idle.ui.bigbang.BigBangPage;
import idle.ui.collider.ColliderPage;
import idle.ui.collider.ColliderPane;
import idle.ui.common.Breakdown;
import idle.ui.common.BurstView;
import idle.ui.common.BuyAmountRow;
import idle.ui.common.Card;
import idle.ui.common.Format;
import idle.ui.common.Labels;
import idle.ui.common.StableHeight;
import idle.ui.common.TabIcon;
import idle.ui.common.TileGrid;
import idle.ui.darkmatter.DarkMatterPage;
import idle.ui.universe.ChronoCorner;
import idle.ui.universe.Dimension;
import idle.ui.universe.DimensionWindow;
import idle.ui.universe.UniversePage;
import idle.ui.pages.AchievementsPage;
import idle.ui.pages.ProfilePage;
import idle.ui.pages.SettingsPage;
import idle.ui.pages.SkinsPage;
import idle.ui.pages.StatsPage;
import idle.ui.particles.GeneratorPane;
import idle.ui.particles.ParticleView;
import idle.ui.probes.LootPane;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

/** L'application, première partie : ses onglets, ses pages, et la construction de la fenêtre. */
public abstract class GameAppBase extends Application {
    /** L'application entière : chaque partie de la chaîne est une {@link GameApp}. */
    final GameApp self() { return (GameApp) this; }

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
    abstract void hideStartButton();
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
    abstract void celebrateNewAtom();

    /** Ce que font, pour le panneau de test, les outils qui touchent à l'affichage et au son. */
    TestTools.Hooks testHooks() {
        return new TestTools.Hooks() {
            @Override
            public void explosion() { playExplosion(false); }

            @Override
            public void bigBang() { playBigBang(); }

            @Override
            public boolean birth() {
                if (game.universes() <= 0) return false;
                dimensionWindow.close();
                selectTab(Tab.UNIVERSE);
                universePage.playBirth();
                return true;
            }

            @Override
            public boolean launch() {
                if (game.universes() <= 0) return false;
                dimensionWindow.close();
                selectTab(Tab.UNIVERSE);
                universePage.launchView().play(null);
                return true;
            }

            @Override
            public boolean curtain() {
                idle.core.universe.Skin chosen = game.chosenSkin(idle.core.universe.SkinVisual.TRANSITION);
                if (chosen != null) GameAppBase.this.curtain.play(chosen);
                return chosen != null;
            }

            @Override
            public boolean finale() {
                if (!game.isDimensionsUnlocked()) return false;
                dimensionWindow.close();
                selectTab(Tab.UNIVERSE);
                universePage.playFinale();
                return true;
            }

            @Override
            public void fusion() {
                selectTab(Tab.PARTICLES);
                soundWithAnimation(Sound.Cue.ATOM);
                generators.fuse(() -> { });
            }

            @Override
            public void newAtom() {
                selectTab(Tab.ATOMS);
                celebrateNewAtom();
                if (settings.sound()) sound.click(Sound.Cue.NEW_ATOM, settings.volume());
            }

            @Override
            public void sound(Sound.Cue cue) {
                if (cue == Sound.Cue.BEAT) sound.beat(Math.max(1, settings.volume()));
                else sound.preview(cue, Math.max(1, settings.volume()));
            }

            @Override
            public void particle(int stage) { sound.particle(stage, Math.max(1, settings.volume())); }

            @Override
            public void gesture(int dimension) { sound.gesture(dimension, Math.max(1, settings.volume())); }

            @Override
            public void clicker(int stack) { sound.clicker(stack, Math.max(1, settings.volume())); }

            @Override
            public void gain(int dimension) { sound.gain(dimension, Math.max(1, settings.volume())); }

            @Override
            public void piano(int note) { sound.piano(note, Math.max(1, settings.volume())); }

            @Override
            public void season(int season) { sound.season(season, Math.max(1, settings.volume())); }

            @Override
            public boolean toggleHum(TestTools.TestHum hum) {
                if (!forcedHums.remove(hum)) forcedHums.add(hum);
                return forcedHums.contains(hum);
            }

            @Override
            public void unlockCosmetics() {
                for (Cosmetic cosmetic : Cosmetic.values()) settings.unlock(cosmetic);
                settingsPage.refresh();
            }

            @Override
            public void lockCosmetics() {
                settings.lockCosmetics();
                applyAppearance();
                settingsPage.refresh();
            }

            @Override
            public void toggleCosmetic(Cosmetic cosmetic) {
                if (settings.hasCosmetic(cosmetic)) settings.lock(cosmetic);
                else settings.unlock(cosmetic);
                applyAppearance();
                settingsPage.refresh();
            }

            @Override
            public void newGame() { resetGame(); }
        };
    }
    /**
     * Vrai si un clic sur {@code target} doit rendre le clavier au jeu : le joueur écrivait dans un champ, et clique
     * ailleurs que dans ce champ, sa rangée, ou un autre champ.
     */
    public static boolean releasesTyping(javafx.scene.Node owner, Object target) {
        if (!(owner instanceof javafx.scene.control.TextInputControl)) return false;
        if (!(target instanceof javafx.scene.Node node)) return true;
        javafx.scene.Node row = owner.getParent();
        for (javafx.scene.Node at = node; at != null; at = at.getParent()) {
            if (at == owner || at == row || at instanceof javafx.scene.control.TextInputControl) return false;
        }
        return true;
    }

    abstract void playExplosion(boolean real);

    /** L'animation plein écran du Big Bang : celle du skin choisi, ou la classique sans skin (version 0.246). */
    void playBigBang() {
        idle.core.universe.Skin bang = game.chosenSkin(idle.core.universe.SkinVisual.BIG_BANG);
        if (bang != null) burst.play(bang, BIG_BANG_SECONDS);
        else burst.playBigBang(BIG_BANG_SECONDS);
    }

    /** La durée de l'animation du Big Bang, en secondes. */
    static final double BIG_BANG_SECONDS = 3.5;
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

    /** Le nom d'un onglet, sans compteur : sur les fenêtres d'explication, et dans le rapport de bug. */
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

    /** La graine du hasard de cette session : tirée au lancement, notée dans le rapport de bug. */
    final long randomSeed = new java.security.SecureRandom().nextLong();
    final Game game = new Game(randomSeed);
    /** Vrai dans le profil de test ({@code --test}) : connu au lancement. */
    boolean testProfile;
    final Settings settings = Settings.load();
    /**
     * Le fichier de sauvegarde. Celui que désignent les arguments du jeu n'est connu qu'au lancement ({@link
     * #start(Stage)}) : d'ici là, rien ne s'écrit. Déclaré avant les pages, qui l'interrogent dès leur construction.
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
    /** L'onglet de l'univers, qui s'ouvre quand l'univers est formé : le quatrième acte. */
    final Button universeTab = new Button("Univers");
    /** L'onglet du vide, deuxième acte de la partie secondaire : au premier Big Bang du troisième univers. */
    final Button voidTab = new Button("Le vide");
    /** L'onglet des hadrons : il se propose dans le collisionneur avec la bulle de l'arbuste, au milieu. */
    final Button hadronsTab = new Button("Hadrons");
    /** L'onglet du profil : le joueur, ses titres, ses albums et le journal ; là dès le premier générateur. */
    final Button profileTab = new Button("Profil");
    final Button achievementsTab = new Button("Succès");
    final Button statsTab = new Button("Statistiques");
    final Button settingsTab = new Button("Réglages");
    /** Le régime rapide des générateurs, et la ligne de la synergie et de la prime. */
    final Button regimeButton = new Button();
    final Label secondLabel = new Label();
    final HBox secondRow = new HBox(10, regimeButton, secondLabel);
    /** L'onglet des skins, sous les réglages : il s'ouvre dans le deuxième univers, après cinq fusions. */
    final Button skinsTab = new Button("Skins");
    final AtomView atomsTabIcon = new AtomView(22);
    /** Les icônes animées des autres onglets, chacune dans la couleur de son onglet. */
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
    /** La fenêtre d'une dimension, par-dessus tout le jeu, avec son bouton de retour. */
    final DimensionWindow dimensionWindow = new DimensionWindow(game, settings::freeCamera, settings::setFreeCamera);
    final AchievementsPage achievementsPage = new AchievementsPage(game);
    /**
     * Vos achats, notés entre une action et le pas de temps suivant ({@link PurchaseHistory}), et le
     * compteur de la prochaine étape ({@link Pace}).
     */
    final idle.core.history.PurchaseHistory purchaseHistory = new idle.core.history.PurchaseHistory();
    final idle.core.history.Pace pace = new idle.core.history.Pace();
    final StatsPage statsPage = new StatsPage(game, purchaseHistory, pace);
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
    /** Le retour de l'onglet Particules : son âge en secondes, négatif hors animation. */
    double particlesTabPop = -1;
    /** Durée de ce retour. */
    static final double PARTICLES_TAB_POP_SECONDS = 0.9;
    final BorderPane root = new BorderPane();
    /** La disposition à gauche : les onglets en colonne, et en haut les sous-onglets de l'onglet affiché. */
    final VBox sideBar = new VBox(2);
    final StackPane subTabHolder = new StackPane();
    /** Une barre de sous-onglets, et d'où elle vient dans sa page : la disposition d'avant l'y remet. */
    record SubTabDock(Tab tab, javafx.scene.layout.Pane page, javafx.scene.layout.Pane bar, int index) {}
    final List<SubTabDock> docks = new java.util.ArrayList<>();
    VBox topBox;
    boolean tabsLeftApplied = false;
    Tab selectedTab = Tab.PARTICLES;
    /**
     * Le dernier onglet de jeu affiché avant les réglages : le rapport de bug se copie depuis les
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
    /** Le chrono dans un coin, en bas à gauche, hors de la sous-page Graine. */
    final idle.ui.universe.ChronoCorner chronoCorner = new idle.ui.universe.ChronoCorner(game);
    /** Version 0.203 : le même chrono, dans l'en-tête de la fenêtre d'une dimension, qui recouvre tout le reste. */
    final idle.ui.universe.ChronoCorner dimensionChrono = new idle.ui.universe.ChronoCorner(game);
    final Notifier notifier = new Notifier(game);
    /** Le guide de transition entre univers. */
    final TransitionPane transitionPane = new TransitionPane(game);
    {
        bigBangPage.galaxyPane().setUniverseConfirm(go -> transitionPane.open(false, go));
        universePage.setLaunchConfirm(go -> transitionPane.open(true, go));
        // Les anomalies du ciel : montrées et annoncées seulement si le joueur le veut.
        bigBangPage.showSkyEvents(settings::skyEvents);
        notifier.showSkyEvents(settings::skyEvents);
    }
    final Sound sound = new Sound();
    /** La musique de fond, qui suit la partie. */
    final Music music = new Music();
    /**
     * Les sons qui partent au début de l'animation d'une action du joueur : la fusion, l'explosion du tableau, le Big
     * Bang. Leur annonce ne les joue jamais ; ce que font les automatismes (fusion ou explosion automatique) ne sonne que
     * si le réglage « Sons des automatismes » est activé.
     */
    static final Set<Sound.Cue> PLAYER_SOUNDS = EnumSet.of(Sound.Cue.ATOM, Sound.Cue.EXPLOSION, Sound.Cue.BIG_BANG);

    /** Les explosions du joueur dont l'annonce n'est pas encore arrivée : les autres viennent d'un automatisme. */
    int playerExplosions;

    /** Joue ce son au début de l'animation d'une action du joueur. */
    void soundWithAnimation(Sound.Cue cue) {
        if (settings.sound()) sound.play(cue, settings.volume());
    }

    /** Le grondement de la matière noire qui grossit, de plus en plus vite avec son élan. */
    final Hum darkMatterHum = new Hum(Hum.DARK_MATTER, "matière noire");
    final Hum colliderHum = new Hum(Hum.COLLIDER, "collisionneur");
    final Hum expansionHum = new Hum(Hum.EXPANSION, "expansion");
    final Hum braneHum = new Hum(Hum.BRANE, "brane");
    final Hum durationHum = new Hum(Hum.DURATION, "durée");
    final Hum chronoHum = new Hum(Hum.CHRONO, "chrono");
    final Hum wateringCanHum = new Hum(Hum.WATERING_CAN, "arrosoir");
    final Hum hoseHum = new Hum(Hum.HOSE, "tuyau");
    final Hum stormHum = new Hum(Hum.STORM, "orage");
    final Hum sunHum = new Hum(Hum.SUN, "beau temps");
    final Hum voidPairsHum = new Hum(Hum.VOID_PAIRS, "paires du vide");
    /** Les bruitages relevés image par image. */
    final SoundEvents soundEvents = new SoundEvents();
    /** La marque d'un bouton d'onglet : son changement d'onglet a son son, il ne fait pas en plus celui d'un clic. */
    static final String TAB_BUTTON = "ongletDuJeu";
    /** Le dernier relâchement de la souris, en nanosecondes : un bouton actionné juste après l'a été à la souris (0.262). */
    private long mouseReleasedAt = Long.MIN_VALUE / 2;
    // L'apparence : la taille de l'interface et le thème clair s'appliquent à toute la fenêtre.
    ScaledPane scaled;
    ThemeFilter themeFilter;
    final StackPane flash = new StackPane();
    final Circle shockwave = new Circle(24, Color.web(DARK_MATTER_COLOR));
    final StackPane blast = new StackPane(flash, shockwave);
    /** Les animations plein écran des skins : l'explosion sous Vortex ou Pulsar, le Big Bang sous ses trois skins. */
    final idle.ui.common.BurstView burst = new idle.ui.common.BurstView();
    /** Toute la fenêtre : l'animation des skins en prend la taille. */
    StackPane windowPane;
    /** Le fond de la fenêtre, sous tout le jeu : rien sans skin, un décor dans les marges avec. */
    final Chrome chrome = new Chrome();
    /** Le rideau de la transition entre univers, joué quand le guide est confirmé sous un skin. */
    final TransitionSkinArt curtain = new TransitionSkinArt();
    /** La traînée du curseur « Étoile filante », tout au-dessus de la fenêtre. */
    final CursorSkinArt.Trail cursorTrail = new CursorSkinArt.Trail();
    /** Le rond d'aide au clic, au-dessus de tout (version 0.261). */
    final ClickRings clickRings = new ClickRings();
    /** Le skin du curseur posé sur la fenêtre, ou {@code null} : {@code refresh()} le compare à celui à montrer. */
    idle.core.universe.Skin cursorSkin = null;
    /** Le skin des onglets appliqué par {@link #styleTabs}, ou {@code null} : {@code refresh()} le compare au choisi. */
    idle.core.universe.Skin tabSkin = null;
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
    /** Le panneau du profil de test et la bande des dimensions ; {@code null} hors du profil de test. */
    TestPanel testPanel;
    DimensionTestBar dimensionTestBar;
    /** Le panneau du profil test UI ({@code --test-ui}, version 0.248), ou {@code null}. */
    UiTestPanel uiTestPanel;
    /** Les fonds sonores que le panneau de test fait jouer partout, pour les écouter. */
    final java.util.EnumSet<TestTools.TestHum> forcedHums = java.util.EnumSet.noneOf(TestTools.TestHum.class);
    /** Guette les conditions des thèmes et des notations à débloquer. */
    private final Cosmetics cosmetics = new Cosmetics(game, settings);
    // La sauvegarde : le temps écoulé depuis la dernière écriture, et le retour après une absence.
    double sinceSave = 0;
    final AbsencePane absencePane = new AbsencePane(this::save);
    /** Les fenêtres d'explication : un onglet qui s'ouvre, une nouveauté du deuxième univers. */
    final PopupPane popupPane = new PopupPane(game);
    List<PopupPane.Entry> popupEntries;
    /** L'écran de fin : une fois par partie, quand l'univers se forme. */
    final EndPane endPane = new EndPane(game);
    /** L'écran de la fin de la partie, au bout de l'animation de la carte des dimensions. */
    final FinalePane finalePane = new FinalePane(game);
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

    /** Le fond, le rideau et la traînée prennent la taille de la fenêtre (version 0.199 pour les deux derniers). */
    void fitLayers(double width, double height) {
        chrome.fit(width, height);
        curtain.fit(width, height);
        cursorTrail.fit(width, height);
        clickRings.fit(width, height);
    }

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        settingsPage.setSoundTest(() -> sound.test(settings.volume()));
        // Les catégories de sons coupées dans les réglages ne sonnent pas.
        sound.setAllowed(settings::soundKind);
        // Un achat ou un interrupteur sur une carte, et chaque geste dans le mini-jeu d'une dimension, sonnent.
        // Une particule récupérée (l'onde atteint le noyau) : le son de son palier de vitesse.
        ParticleView.setOnCollected(speedStage -> { if (settings.sound()) sound.particle(speedStage, settings.volume()); });
        // Version 0.278 : chaque carte de récompense d'une sonde ouverte fait le son d'achat ; « Tout ouvrir », une suite.
        lootPane.setOnOpened(cards -> { if (settings.sound()) sound.purchases(cards, settings.volume()); });
        Card.setOnActed(state -> {
            if (!settings.sound()) return;
            boolean toggle = state == Card.State.ON || state == Card.State.OFF;
            sound.click(toggle ? Sound.Cue.TOGGLE : Sound.Cue.PURCHASE, settings.volume());
        });
        dimensionWindow.setOnGesture(dimension -> {
            if (!settings.sound()) return;
            // Le miroir sonne par son piano (les cases reproduites), pas par un geste.
            if (dimension == 9) return;
            // La membrane relâchée : plus la tirée était forte, plus elle part fort et aigu.
            if (dimension == 6) {
                double pull = dimensionWindow.view().lastBranePull();
                sound.gesture(6, (int) Math.round(settings.volume() * (0.35 + 0.65 * pull)), 0.8 + 0.5 * pull);
            } else if (dimension == 7) {
                // Le clic du clicker : le tapotement, en accord dans une zone ×2 (une note de plus par zone superposée).
                sound.clicker(dimensionWindow.view().lastClickerStack(), settings.volume());
            } else sound.gesture(dimension, settings.volume());
        });
        // Le miroir : la vitesse de son motif se range dans les réglages.
        dimensionWindow.setMirrorPace(settings.mirrorPace(), settings::setMirrorPace);
        // La boucle : les touches de ses cercles et son métronome se rangent dans les réglages.
        dimensionWindow.setLoopSettings(new DimensionWindow.LoopSettings() {
            @Override
            public javafx.scene.input.KeyCode loopKey(int ring) { return settings.loopKey(ring); }

            @Override
            public boolean metronome() { return settings.loopMetronome(); }

            @Override
            public void setMetronome(boolean on) { settings.setLoopMetronome(on); }

            @Override
            public void beat() { if (settings.sound()) sound.beat(settings.volume()); }
        });
        // Une erreur imprévue ne ferme pas le jeu : elle est gardée pour le rapport de bug, et écrite dans la console.
        Thread.currentThread().setUncaughtExceptionHandler((thread, error) -> {
            Crash.note(error);
            error.printStackTrace();
            // Le profil test UI relève l'erreur à l'étape montrée.
            if (uiTestPanel != null) uiTestPanel.error(error);
        });
        // La partie sauvegardée est reprise avant que rien ne s'affiche.
        List<String> arguments = getParameters().getRaw();
        store = SaveStore.of(arguments);
        SaveStore.Loaded loaded = store.load(game);
        // Le profil test UI part d'une partie préparée (chaque acte ouvert, tous les skins), jamais sauvegardée.
        boolean uiTesting = arguments.contains("--test-ui");
        if (uiTesting) {
            UiTestPlan.prepare(game);
            // Ses thèmes et ses notations, essayés à chaque étape, ne vont pas dans les réglages du joueur (0.252).
            settings.forget();
        }

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
        // Le deuxième univers : le régime des générateurs, la synergie et la prime de cycle parfait.
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
            tab.getProperties().put(TAB_BUTTON, true);
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

        // En haut : la barre d'onglets ; à droite, le panneau du profil de test si le jeu est lancé avec --test.
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
        // Le chrono a sa ligne, sous celle de l'objectif : il ne flotte plus par-dessus les pages.
        VBox top = new VBox(explosionButton, challengeLabel, tabBar, infoBar, chronoCorner);
        topBox = top;
        sideBar.setStyle("-fx-background-color: #0e131b; -fx-padding: 10 8; -fx-spacing: 6; -fx-border-color: transparent #1d2633 transparent transparent;");
        sideBar.setMinWidth(GameAppTabs.SIDE_BAR);
        sideBar.setPrefWidth(GameAppTabs.SIDE_BAR);
        subTabHolder.setStyle("-fx-background-color: #0e131b; -fx-padding: 4 0; -fx-border-color: transparent transparent #1d2633 transparent;");
        // Toujours la même hauteur, sous-onglets ou non : rien ne bouge quand on change d'onglet.
        subTabHolder.setMinHeight(SUB_TAB_HEIGHT);
        subTabHolder.setPrefHeight(SUB_TAB_HEIGHT);
        subTabHolder.setMaxHeight(SUB_TAB_HEIGHT);
        tabTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");
        subTabHolder.getChildren().add(tabTitle);
        dock(Tab.ATOMS, atomsPage, atomsPage.subTabs());
        // L'automatisation : les automatismes de toujours, et ceux des dimensions.
        dock(Tab.AUTOMATION, automationPage, automationPage.subTabs());
        dock(Tab.DARK_MATTER, darkMatterPage, darkMatterPage.subTabs());
        dock(Tab.COLLIDER, colliderPage, colliderPage.subTabs());
        dock(Tab.BIG_BANG, bigBangPage, bigBangPage.subTabs());
        dock(Tab.UNIVERSE, universePage, universePage.subTabs());
        // Le vide : ses paires, et ses améliorations à part.
        dock(Tab.VOID, voidPage, voidPage.subTabs());
        // Les hadrons : les gluons ; les paliers et la liaison.
        dock(Tab.HADRONS, hadronsPage, hadronsPage.subTabs());
        universePage.setOnOpenDimension(number -> dimensionWindow.open(Dimension.of(number)));
        universePage.setOnFinale(finalePane::open);
        finalePane.setOnRestart(this::resetGame);
        // Version 0.270 : « Revoir la fin » rejoue aussi son son et l'élan de la musique, comme la première fois.
        universePage.setOnReplay(() -> {
            soundWithAnimation(Sound.Cue.GAME_END);
            music.accent();
        });
        // Rouvrir les paliers : les anciens onglets reviennent, la partie de l'univers suivant commence.
        universePage.setOnNextUniverse(() -> {
            // Les trois actes repartent de zéro : les pages oublient ce qu'elles montraient.
            forgetOldPages();
            refresh();
            // L'onglet Particules réapparaît en grandissant ; on y va une fois qu'il est là.
            particlesTabPop = 0;
            particlesTab.setScaleX(0.2);
            particlesTab.setScaleY(0.2);
        });
        dock(Tab.PROFILE, profilePage, profilePage.subTabs());
        dock(Tab.STATS, statsPage, statsPage.subTabs());
        dock(Tab.SETTINGS, settingsPage, settingsPage.subTabs());
        // Les skins : la collection, puis une catégorie par sous-onglet (version 0.249).
        dock(Tab.SKINS, skinsPage, skinsPage.subTabs());
        testProfile = arguments.contains("--test");
        if (testProfile) {
            testPanel = new TestPanel(game, testHooks());
            // La bande de chaque dimension, en bas de sa fenêtre : elle suit la dimension ouverte.
            dimensionTestBar = new DimensionTestBar(game, testPanel.actions(), testPanel.bots(),
                    () -> dimensionWindow.isOpen() ? dimensionWindow.dimension().number() : 0, testPanel::note, testPanel::refresh);
            dimensionWindow.setTestBar(dimensionTestBar);
            root.setRight(testPanel);
        } else if (uiTesting) {
            UiTestStage uiStage = new UiTestStage(this, testHooks());
            // Les images par seconde de chaque combinaison, dans le dossier des données du jeu (0.253).
            UiTestFpsFile fpsFile = new UiTestFpsFile(SaveStore.defaultFolder().resolve(UiTestFpsFile.NAME), this::framesPerSecond);
            uiTestPanel = new UiTestPanel(uiStage::show, uiStage::check, uiStage.settingsAxes(), fpsFile);
            uiStage.setPanel(uiTestPanel);
            root.setRight(uiTestPanel);
        }

        root.setTop(top);
        root.setCenter(pages);
        // La fenêtre ne se laisse plus pousser par une page plus haute qu'elle. Avant, le contenu, centré,
        // dépassait en haut comme en bas : dans une petite fenêtre, le haut sortait de l'écran.
        root.setMinSize(0, 0);
        StackPane.setAlignment(root, Pos.TOP_LEFT);
        // Une page plus haute que la place qui reste dépasse en bas, coupée, au lieu de passer sur les onglets.
        pages.setMinSize(0, 0);
        javafx.scene.shape.Rectangle pagesClip = new javafx.scene.shape.Rectangle();
        pagesClip.widthProperty().bind(pages.widthProperty());
        pagesClip.heightProperty().bind(pages.heightProperty());
        pages.setClip(pagesClip);
        for (javafx.scene.Node page : pages.getChildren()) if (StackPane.getAlignment(page) == null) StackPane.setAlignment(page, Pos.TOP_CENTER);
        root.setStyle(Chrome.CLASSIC_ROOT_STYLE);

        // L'explosion recouvre toute la fenêtre : un éclair blanc et une onde qui part du centre.
        flash.setStyle("-fx-background-color: #ffffff;");
        flash.setOpacity(0);
        blast.setVisible(false);
        // Le fond skinné est tout en dessous ; il prend la taille de la fenêtre et se redessine avec elle.
        StackPane window = windowPane = new StackPane(chrome, root, dimensionWindow, blast, burst, toasts, lootPane, helpPane, endPane, finalePane, popupPane,
                transitionPane, absencePane, curtain, cursorTrail, clickRings);
        window.widthProperty().addListener((observable, before, width) -> fitLayers(width.doubleValue(), window.getHeight()));
        window.heightProperty().addListener((observable, before, height) -> fitLayers(window.getWidth(), height.doubleValue()));
        curtain.setVisible(false);
        transitionPane.setOnConfirmed(curtain::play);
        bigBangPage.probesPane().setOpener(lootPane::open);
        StackPane.setAlignment(toasts, Pos.BOTTOM_RIGHT);
        dimensionWindow.setChrono(dimensionChrono);
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
                // Ce que le joueur a acheté depuis l'image d'avant, avant que le temps ne repasse.
                purchaseHistory.after(game, pace);
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
                long workStart = System.nanoTime();
                sinceSave += dt;
                if (sinceSave >= settings.saveDelay().seconds()) save();
                // En pause, le temps du jeu s'arrête ; celui de l'interface continue (confirmations, réglages).
                if (!settings.paused()) {
                    // Pendant l'avance rapide, la Timeline pilote déjà le temps du jeu.
                    // Le profil test UI : la partie reste figée, sauf sur les états qui bougent.
                    boolean frozen = uiTestPanel != null && !uiTestPanel.live();
                    if ((testPanel == null || !testPanel.bots().isFastForwarding()) && !frozen) {
                        game.tick(dt * timeFactor());
                        if (testPanel != null) testPanel.bots().frame(dt * timeFactor());
                    }
                    pace.observe(game);
                    animate(dt);
                    if (uiTestPanel != null) uiTestPanel.frame(dt);
                }
                // Le clic tenu sur une carte qui se répète (une molécule, une fois la création continue acquise).
                // Ce qu'il achète compte comme un achat du joueur.
                if (Card.held() != null || !heldKeys.isEmpty()) purchaseHistory.before(game);
                Card.repeatHeld(dt);
                repeatHeldKeys(dt);
                explosionArmed = Math.max(0, explosionArmed - dt);
                if (selectedTab == Tab.SETTINGS) settingsPage.frame(dt);
                announce(dt);
                // La musique suit le moment de la partie, et la fenêtre d'une dimension ouverte.
                int openDimension = dimensionWindow.isOpen() ? dimensionWindow.dimension().number() : 0;
                music.update(Music.moodOf(game, openDimension), settings.music(), settings.musicVolume());
                // Le grondement de la matière noire : sur son onglet, tant qu'elle grossit (en pause, elle ne grossit pas).
                boolean darkGrowing = selectedTab == Tab.DARK_MATTER && !settings.paused() && !dimensionWindow.isOpen()
                        && darkMatterPage.isGrowing();
                darkMatterHum.update(darkGrowing || forcedHums.contains(TestTools.TestHum.DARK_MATTER), Hum.darkMatterRate(game.darkMatterMomentum()),
                        settings.sound() && settings.soundKind(SoundKind.DARK_MATTER), settings.volume());
                // Le fond du collisionneur : sur son sous-onglet Énergie, où tournent les faisceaux, à leur vitesse.
                boolean beams = selectedTab == Tab.COLLIDER && colliderPage.selected() == ColliderPane.ENERGY && !settings.paused() && !dimensionWindow.isOpen() && game.isColliderUnlocked();
                colliderHum.update(beams || forcedHums.contains(TestTools.TestHum.COLLIDER), Hum.colliderRate(game.colliderPeriod()),
                        settings.sound() && settings.soundKind(SoundKind.COLLIDER), settings.volume());
                // Le fond de l'expansion de la matière : sur son sous-onglet du Big Bang, où l'espace grandit.
                boolean expanding = selectedTab == Tab.BIG_BANG && bigBangPage.selected() == BigBangPage.EXPANSION && !settings.paused()
                        && !dimensionWindow.isOpen();
                expansionHum.update(expanding || forcedHums.contains(TestTools.TestHum.EXPANSION), 1, settings.sound() && settings.soundKind(SoundKind.EXPANSION), settings.volume());
                // La corde de la brane, tant qu'on la tire : plus tendue, plus aiguë et plus forte ; l'appui de la durée,
                // qui monte à mesure qu'on approche du plus long appui possible.
                boolean dimensionSounds = settings.sound() && settings.soundKind(SoundKind.DIMENSIONS) && (!settings.paused() || !forcedHums.isEmpty());
                double pull = dimensionWindow.view().braneHeld() ? dimensionWindow.view().branePull() : 0;
                braneHum.update(openDimension == 6 && dimensionWindow.view().braneHeld() || forcedHums.contains(TestTools.TestHum.BRANE), 0.8 + 1.4 * pull, dimensionSounds,
                        settings.volume(), 0.3 + 0.7 * pull);
                double held = game.durationMaxSeconds() > 0 ? Math.min(1, game.durationHeldSeconds() / game.durationMaxSeconds()) : 0;
                durationHum.update(openDimension == 4 && game.isDurationHeld() || forcedHums.contains(TestTools.TestHum.DURATION), 1 + 2 * held, dimensionSounds, settings.volume());
                // La graine, quand on la voit : l'arrosoir qu'on tient, le tuyau qui arrose, la météo.
                boolean seedShown = selectedTab == Tab.UNIVERSE && universePage.selected() == idle.ui.universe.UniversePage.SEED
                        && !dimensionWindow.isOpen() && !settings.paused();
                boolean seedSounds = settings.sound() && settings.soundKind(SoundKind.SEED);
                wateringCanHum.update(game.isWatering() && !settings.paused() || forcedHums.contains(TestTools.TestHum.WATERING_CAN), 1,
                        seedSounds, settings.volume());
                hoseHum.update(seedShown && game.hoseRate() > 0 && game.canTendSeed() || forcedHums.contains(TestTools.TestHum.HOSE), 1,
                        seedSounds, settings.volume());
                stormHum.update(seedShown && game.weather() == Game.Weather.STORM || forcedHums.contains(TestTools.TestHum.STORM), 1,
                        seedSounds, settings.volume());
                sunHum.update(seedShown && game.weather() == Game.Weather.SUN || forcedHums.contains(TestTools.TestHum.SUN), 1,
                        seedSounds, settings.volume());
                // Le fond des paires du vide (version 0.272) : sur leur sous-onglet, tant que le jeu tourne.
                boolean pairsShown = selectedTab == Tab.VOID && voidPage.selected() == idle.ui.vacuum.VoidPage.PAIRS
                        && !dimensionWindow.isOpen() && !settings.paused();
                // Version 0.279 : plus fort à mesure que le trou noir du vide se remplit.
                double holeFill = game.isVoidHoleOpen() ? game.voidMass() / idle.core.Game.VOID_MAX_MASS : 0;
                voidPairsHum.update(pairsShown || forcedHums.contains(TestTools.TestHum.VOID_PAIRS), 1,
                        settings.sound() && settings.soundKind(SoundKind.VOID), settings.volume(), Hum.voidPairsLevel(holeFill));
                // Le chrono, où qu'on soit, tant qu'il accélère le jeu.
                chronoHum.update(game.isChronoRunning() && !settings.paused() || forcedHums.contains(TestTools.TestHum.CHRONO),
                        Hum.chronoRate(game.chronoSpeed()), settings.sound() && settings.soundKind(SoundKind.CHRONO), settings.volume());
                // Les bruitages de ce qui vient de se passer, là où on le voit.
                for (SoundEvents.Heard heard : soundEvents.frame(game, SoundEvents.heardTab(selectedTab.name(), voidPage.selected() == idle.ui.vacuum.VoidPage.PAIRS,
                        colliderPage.selected() == ColliderPane.ENERGY), openDimension)) {
                    if (!settings.sound()) continue;
                    if (heard.cue() == null) sound.gain(heard.dimension(), settings.volume());
                    else if (heard.cue() == Sound.Cue.MIRROR) sound.piano(heard.note(), settings.volume());
                    else if (heard.cue() == Sound.Cue.SEASON) sound.season(heard.note(), settings.volume());
                    else sound.click(heard.cue(), settings.volume());
                }
                // L'écran de fin attend que la naissance de l'univers soit jouée.
                boolean bornNow = hadUniverse == 0 && game.hasCosmos(idle.core.sky.Cosmos.UNIVERSE);
                if (!bornNow && !(selectedTab == Tab.UNIVERSE && universePage.isBirthPlaying())) endPane.check();
                // Le défi final : la partie vient d'être finie, la fin se joue sur la carte des dimensions.
                // La fenêtre d'une dimension ouverte se ferme : la carte doit se voir.
                if (universePage.checkFinale()) {
                    dimensionWindow.close();
                    selectTab(Tab.UNIVERSE);
                }
                // Le profil test UI n'ouvre pas les fenêtres d'explication : elles couvriraient ce qu'il montre.
                if (uiTestPanel == null) checkPopups();
                // Les thèmes et notations à débloquer : annoncés après le reste, pour ne pas être chassés par lui.
                // Leur catégorie se coupe dans les réglages, comme les autres notifications.
                for (Cosmetic opened : cosmetics.frame(dt, settings.paused())) {
                    if (settings.notice(Notice.COSMETICS)) toasts.show(Cosmetics.announcement(opened), SETTINGS_COLOR);
                }
                // Le rideau de la transition et la traînée du curseur suivent le temps de l'interface.
                curtain.frame(dt);
                cursorTrail.frame(dt);
                clickRings.frame(dt);
                refresh();
                // Le profil test UI mesure chaque image : son intervalle, et le calcul du jeu pour elle (0.253).
                if (uiTestPanel != null) uiTestPanel.measure(dt, (System.nanoTime() - workStart) / 1e9);
            }
        }.start();

        // L'onglet de la dernière fois, s'il existe encore dans cette partie : sinon refresh() revient aux particules.
        // Un onglet d'une autre version du jeu : on reste sur le premier.
        Tab lastTab = idle.core.Enums.parse(Tab.class, settings.lastTab());
        if (loaded.found() && lastTab != null) selectTab(lastTab);
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
        // Un bouton actionné au clavier (Espace, Entrée) fait le clic, sauf un onglet (qui a son propre son) ; à la souris, le
        // clic a déjà sonné en appuyant (0.262).
        scene.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_RELEASED, event -> mouseReleasedAt = System.nanoTime());
        scene.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            boolean byMouse = System.nanoTime() - mouseReleasedAt < 100_000_000L;
            if (!byMouse && event.getTarget() instanceof Button button && !button.getProperties().containsKey(TAB_BUTTON) && settings.sound()) {
                sound.click(Sound.Cue.CLICK, settings.volume());
            }
        });
        applyAppearance();
        // Les touches sont lues avant les boutons : sinon, Espace irait d'abord au bouton qui a le focus.
        // Sauf quand le joueur écrit dans une barre de recherche : ses lettres ne sont pas des raccourcis.
        // La photo d'avant une action du joueur, prise avant que l'action ne s'applique (les filtres
        // passent avant les boutons) ; la comparaison se fait à l'image suivante, avant le pas de temps.
        scene.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, event -> purchaseHistory.before(game));
        scene.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_RELEASED, event -> purchaseHistory.before(game));
        scene.addEventFilter(javafx.event.ActionEvent.ACTION, event -> purchaseHistory.before(game));
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            purchaseHistory.before(game);
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
        // Version 0.247 : un clic hors du champ où le joueur écrivait (la recherche du panneau de test, la boîte des
        // chiffres x, y, z de la grille…) le désélectionne et rend le clavier au jeu ; sinon les touches de la boucle et
        // les raccourcis restaient muets.
        scene.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, event -> {
            if (!releasesTyping(scene.getFocusOwner(), event.getTarget())) return;
            ((javafx.scene.control.TextInputControl) scene.getFocusOwner()).deselect();
            windowPane.requestFocus();
        });
        scene.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_MOVED, event -> cosmetics.input());
        // Version 0.261 : chaque clic fait un rond là où il tombe (réglage « Rond au clic »). Version 0.262 : chaque clic fait
        // le son du clic, qu'il tombe sur rien ou qu'il déclenche une action ; seuls les onglets et les mini-jeux des
        // dimensions, qui ont leur propre son à l'appui, ne le doublent pas.
        scene.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, event -> {
            if (settings.effect(Settings.Effect.CLICK_RING)) {
                javafx.geometry.Point2D local = clickRings.sceneToLocal(event.getSceneX(), event.getSceneY());
                if (local != null) clickRings.add(local.getX(), local.getY());
            }
            if (settings.sound() && !ClickRings.hasOwnSound(event.getPickResult().getIntersectedNode(), TAB_BUTTON)) {
                sound.click(Sound.Cue.CLICK, settings.volume());
            }
        });
        // La traînée de l'étoile filante suit la souris, en coordonnées de son calque.
        scene.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_MOVED, event -> {
            if (!cursorTrail.isActive()) return;
            javafx.geometry.Point2D local = cursorTrail.sceneToLocal(event.getSceneX(), event.getSceneY());
            if (local != null) cursorTrail.move(local.getX(), local.getY());
        });
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
        if (uiTestPanel != null) uiTestPanel.begin();
    }

    /** Ce que la page des réglages demande pour la sauvegarde : chaque geste rend la phrase qui dit ce qu'il a donné. */
    private final class SaveActions implements SettingsPage.Saves {
        @Override
        public String status() { return store.status(); }

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
        public int checkpointSlots() { return SaveStore.MAX_CHECKPOINTS; }

        @Override
        public String checkpointLine(int slot) {
            SaveStore.Checkpoint point = store.checkpoint(slot);
            if (point.used()) return SaveStore.describe(point.preview());
            return point.problem().isEmpty() ? "vide" : "illisible : " + point.problem();
        }

        @Override
        public boolean checkpointUsed(int slot) { return store.checkpoint(slot).used(); }

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
