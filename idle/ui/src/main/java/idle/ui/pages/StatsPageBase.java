package idle.ui.pages;

import idle.core.Game;
import idle.core.GameStats;
import idle.core.Resource;
import idle.core.StatsHistory;
import idle.core.darkmatter.Landmark;
import idle.core.darkmatter.SizeScale;
import idle.core.elements.Element;
import idle.core.elements.ElementCategory;
import idle.core.elements.ElementEffect;
import idle.core.elements.PeriodicTable;
import idle.core.math.BigNum;
import idle.core.upgrades.Automation;
import idle.core.upgrades.Upgrade;
import idle.ui.GameApp;
import idle.ui.automation.AutomationPage;
import idle.ui.common.ElementText;
import idle.ui.common.Format;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

/**
 * La page des statistiques, première partie : sa construction et ses sous-pages.
 */
public abstract class StatsPageBase extends VBox {

    /** La page entière : chaque partie de la chaîne est une {@link StatsPage}. */
    final StatsPage self() {
        return (StatsPage) this;
    }

    StatsPageBase(Game game) {
        super(8);
        this.game = game;
        setAlignment(Pos.TOP_CENTER);
        setPadding(new Insets(16, 24, 16, 24));

        Label title = new Label("Statistiques");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + STATS_COLOR + ";");
        subTabs.setAlignment(Pos.CENTER);
        nextLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #8fa3b8;");
        nextLabel.setWrapText(true);
        nextLabel.setTextAlignment(TextAlignment.CENTER);

        rangeRow.setAlignment(Pos.CENTER);
        wholeButton.setOnAction(event -> {
            runOnly = false;
            refresh();
        });
        runButton.setOnAction(event -> {
            runOnly = true;
            refresh();
        });

        general();
        particles();
        atoms();
        automation();
        periodicTable();
        darkMatter();
        bigBang();
        collider();
        records();
        journal();

        VBox.setVgrow(stack, Priority.ALWAYS);
        getChildren().add(title);
        getChildren().add(subTabs);
        getChildren().add(rangeRow);
        getChildren().add(stack);
        getChildren().add(nextLabel);
        select(pages.get(0));
    }

    // Ce que fournissent les parties suivantes.
    abstract void resources();
    abstract void production();
    abstract void atomsPerFusion();
    abstract void tableProgress();
    abstract void darkMatterSize();
    abstract void colliderEnergy();
    abstract void achievementCount();
    abstract GameStats stats();
    abstract boolean fused();
    abstract boolean exploded();
    abstract boolean tableSeen();
    abstract boolean automationSeen();
    abstract int owned(boolean inTree);
    abstract int count(boolean inTree);
    abstract String nextGoal();
    abstract void bigBang();
    abstract List<ChartPane.Series> explosionSeries();
    abstract void collider();
    abstract void records();
    abstract void journal();
    abstract void page(String name, String color, BooleanSupplier unlocked, String howToUnlock);
    abstract void section(String title);
    abstract void curves();
    abstract void curve(String name, BooleanSupplier available, Supplier<ChartPane> maker, Line... lines);
    abstract void log(String name, String unit, String color, StatsHistory.Stat stat, BooleanSupplier available);
    abstract void count(String name, String unit, String color, StatsHistory.Stat stat, BooleanSupplier available);
    abstract Row row(String name, Supplier<String> text);
    abstract Row row(String name, Supplier<String> text, BooleanSupplier visible);
    abstract void charted(String graph, Row row);
    abstract void select(SubPage page);
    public abstract void refresh();


    static final String STATS_COLOR = "#7fe0d4";
    static final String SUB_TAB_STYLE = "-fx-font-size: 13px; -fx-padding: 6 16; -fx-cursor: hand;"
            + " -fx-background-radius: 0; -fx-border-width: 0 0 2 0; -fx-background-color: transparent;";
    static final String NAME_STYLE = "-fx-font-size: 13px; -fx-text-fill: #b9c7d6;";
    static final String VALUE_STYLE = "-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #e8f4ff;";
    static final String ROW_STYLE = "-fx-padding: 5 12; -fx-background-color: #121821; -fx-background-radius: 4;";
    /** Une ligne qui mène à une courbe, sous la souris. */
    static final String ROW_HOVER_STYLE = "-fx-padding: 5 12; -fx-background-color: #1c2735; -fx-background-radius: 4;"
            + " -fx-cursor: hand;";
    static final double MAX_WIDTH = 640;

    /** Graduations d'un axe de durées en échelle logarithmique : 1 s, 10 s, 1 min, 10 min, 1 h, 1 jour, 10 jours. */
    static final double[] DURATION_TICKS = {0, 1, Math.log10(60), Math.log10(600), Math.log10(3_600),
            Math.log10(86_400), Math.log10(864_000)};

    /** Graduations d'un axe de délais d'automatisme, en échelle logarithmique : 0,01 s, 0,1 s, 1 s, 10 s, 1 min. */
    private static final double[] DELAY_TICKS = {-2, -1, 0, 1, Math.log10(60)};

    /**
     * Une courbe que le menu déroulant propose.
     *
     * <p>Son graphique n'est construit que la première fois qu'on la choisit : la plupart des
     * courbes ne sont jamais regardées, et chaque graphique porte une zone de dessin.
     */
    static final class Graph {
        final String name;
        /** Vrai quand la statistique existe dans la partie : avant, la courbe n'est pas proposée. */
        final BooleanSupplier available;
        final Supplier<ChartPane> maker;
        final Supplier<List<ChartPane.Series>> series;
        /** Vrai si la courbe suit le temps : le choix de la période la concerne. */
        final boolean timeline;
        ChartPane pane;

        Graph(String name, BooleanSupplier available, Supplier<ChartPane> maker,
              Supplier<List<ChartPane.Series>> series, boolean timeline) {
            this.name = name;
            this.available = available;
            this.maker = maker;
            this.series = series;
            this.timeline = timeline;
        }
    }

    /** Une ligne : un intitulé à gauche, une valeur à droite, et la condition pour qu'elle apparaisse. */
    record Row(String name, Node box, Label value, Supplier<String> text, BooleanSupplier visible) {}

    /** Une sous-page : son onglet, son contenu, et ce qui la débloque. */
    static final class SubPage {
        final Button tab;
        final VBox content = new VBox(4);
        final ScrollPane scroll = new ScrollPane(content);
        final String color;
        final BooleanSupplier unlocked;
        /** Ce qu'il faut faire pour la voir apparaître, à dire tant qu'elle est cachée. */
        final String howToUnlock;
        final List<Row> rows = new ArrayList<>();
        // Les courbes : le menu déroulant, la place du graphique, et la courbe affichée.
        final List<Graph> graphs = new ArrayList<>();
        final ComboBox<String> choice = new ComboBox<>();
        final HBox chooser = new HBox(8);
        final StackPane holder = new StackPane();
        /** Les noms que le menu propose en ce moment, pour ne le remplir que lorsqu'ils changent. */
        List<String> offered = List.of();
        Graph shown;
        /** Ce qu'il faut encore mettre à jour quand la sous-page est à l'écran, en plus des lignes et des graphiques. */
        final List<Runnable> updaters = new ArrayList<>();

        SubPage(String name, String color, BooleanSupplier unlocked, String howToUnlock) {
            this.tab = new Button(name);
            this.color = color;
            this.unlocked = unlocked;
            this.howToUnlock = howToUnlock;
        }
    }

    final Game game;
    final List<SubPage> pages = new ArrayList<>();
    final FlowPane subTabs = new FlowPane();

    /** La barre des sous-onglets : la disposition à gauche la place en haut de la fenêtre. */
    public FlowPane subTabs() {
        return subTabs;
    }
    final StackPane stack = new StackPane();
    final Label nextLabel = new Label();
    // Le choix de la période des graphiques, proposé après la première explosion.
    final Button wholeButton = new Button("Tout le jeu");
    final Button runButton = new Button("Depuis la dernière explosion");
    final HBox rangeRow = new HBox(8, wholeButton, runButton);
    /** Vrai si les graphiques ne montrent que la partie en cours. */
    boolean runOnly = false;
    SubPage selected;
    /** La sous-page en cours de construction : {@link #section} et {@link #row} s'y ajoutent. */
    SubPage building;
    GameStats current;
    /** Vrai pendant que la page remplit elle-même un menu déroulant : ce n'est pas un choix du joueur. */
    boolean updating = false;

    // ------------------------------------------------------------------
    // Les sous-pages
    // ------------------------------------------------------------------

    private void general() {
        page("Général", STATS_COLOR, () -> true, "");
        // La vue d'ensemble : toutes les ressources sur un même graphique, puis la courbe principale de chacune, et les succès.
        curves();
        resources();
        production();
        atomsPerFusion();
        tableProgress();
        darkMatterSize();
        colliderEnergy();
        achievementCount();
        section("Temps");
        row("Temps de jeu", () -> Format.duration(game.state().timePlayed()));
        row("Depuis le dernier Big Bang", () -> Format.duration(stats().bigBangTime()), game::isBigBangUnlocked);
        row("Depuis la dernière explosion", () -> Format.duration(stats().runTime()), this::exploded);
        row("Depuis la dernière fusion", () -> Format.duration(game.state().timeSinceFusion()), this::fused);

        section("Avancée");
        row("Prochain objectif", this::nextGoal);
        row("Générateurs", () -> game.generatorCount() + " / " + game.maxGeneratorCount(), game::isStarted);
        row("Fusions", () -> Format.whole(stats().fusions()), this::fused);
        charted("Avancement du tableau périodique", row("Éléments découverts",
                () -> game.discoveredElements() + " / " + PeriodicTable.ELEMENTS.size(), this::tableSeen));
        row("Explosions", () -> Format.whole(game.state().explosions()), this::exploded);
        row("Big Bangs", () -> Format.whole(game.bigBangs()), game::isBigBangUnlocked);
        row("Molécules créées", () -> Format.whole(game.moleculesCreated()), () -> game.moleculesCreated() > 0);
        row("Astres formés", () -> game.bodiesFormed() + " / " + game.bodies().size(), () -> game.bodiesFormed() > 0);
        charted("Succès obtenus", row("Succès", () -> game.achievementCount() + " / " + game.achievements().size(),
                () -> !game.achievements().isEmpty()));

        section("Achats");
        row("Niveaux d'améliorations en particules", () -> Format.whole(stats().particleUpgradesBought()), game::isStarted);
        row("Niveaux d'améliorations en atomes", () -> Format.whole(stats().atomUpgradesBought()), this::fused);
        row("Actions faites par les automatismes",
                () -> Format.whole(stats().automationActions() + stats().darkAutomationActions()), this::automationSeen);
        row("Créations de molécules", () -> Format.whole(stats().moleculeCreations()), () -> stats().moleculeCreations() > 0);
    }

    private void particles() {
        page("Particules", "#9fd0ff", game::isStarted, "en créant le premier générateur");
        curves();
        production();
        log("Particules créées", "depuis le début du jeu", ChartPane.BLUE, StatsHistory.Stat.PARTICLES_CREATED, () -> true);
        log("Particules dépensées", "en améliorations, depuis le début du jeu", ChartPane.ORANGE,
                StatsHistory.Stat.PARTICLES_SPENT, () -> true);
        log("Créations par seconde", "pour un générateur", ChartPane.BLUE, StatsHistory.Stat.SPEED, () -> true);
        log("Particules par création", "", ChartPane.BLUE, StatsHistory.Stat.PARTICLES_PER_CREATION, () -> true);
        count("Niveaux de vitesse", "ils repartent de zéro à chaque fusion, jusqu'à Persistance", ChartPane.BLUE,
                StatsHistory.Stat.SPEED_LEVELS, () -> true);
        count("Paliers de vitesse", "", ChartPane.BLUE, StatsHistory.Stat.SPEED_MILESTONES, () -> true);
        count("Niveaux achetés en particules", "depuis le début du jeu, générateurs compris", ChartPane.BLUE,
                StatsHistory.Stat.PARTICLE_UPGRADES_BOUGHT, () -> true);
        curve("Ce que les éléments multiplient", this::tableSeen,
                () -> new ChartPane("Ce que les éléments multiplient", "particules et vitesse · échelle logarithmique",
                        true, Format::clock, value -> Format.multiplier(BigNum.pow10(value)), false, 0, Double.NaN),
                new Line("Particules", ChartPane.BLUE, sample -> sample.value(StatsHistory.Stat.ELEMENT_PARTICLES)),
                new Line("Vitesse", ChartPane.ORANGE, sample -> sample.value(StatsHistory.Stat.ELEMENT_SPEED)));
        curve("Ce que l'arbre multiplie", this::exploded,
                () -> new ChartPane("Ce que l'arbre de matière noire multiplie", "particules · échelle logarithmique",
                        true, Format::clock, value -> Format.multiplier(BigNum.pow10(value)), true, 0, Double.NaN),
                new Line("Particules", ChartPane.VIOLET, sample -> sample.value(StatsHistory.Stat.DARK_PARTICLES)));

        section("Quantités");
        row("Particules possédées", () -> Format.count(game.state().particles()));
        charted("Particules créées", row("Créées depuis le début du jeu", () -> Format.count(stats().particlesCreated())));
        row("Créées depuis la dernière explosion", () -> Format.count(stats().runParticlesCreated()), this::exploded);
        charted("Particules dépensées", row("Dépensées en améliorations", () -> Format.count(stats().particlesSpent())));

        section("Production");
        charted("Production de particules",
                row("Production actuelle", () -> Format.amount(game.productionPerSecond()) + " par seconde"));
        row("Par minute", () -> Format.perMinute(game.productionPerSecond()));
        row("Production avec tous les générateurs", () -> Format.amount(game.productionAtFusion()) + " par seconde");
        row("Record de production", () -> Format.amount(stats().bestProduction()) + " par seconde");

        section("D'où vient la production");
        row("Générateurs en activité", () -> game.generatorCount() + " / " + game.maxGeneratorCount());
        charted("Créations par seconde",
                row("Créations par seconde et par générateur", () -> Format.amount(game.speed())));
        charted("Particules par création", row("Particules par création", () -> Format.amount(game.particlesPerCreation())));
        charted("Paliers de vitesse", row("Paliers de vitesse atteints", () -> game.speedMilestones() + " : particules "
                + Format.multiplier(game.speedMilestoneMultiplier()), () -> game.speedMilestones() > 0));
        charted("Ce que les éléments multiplient", row("Éléments : particules",
                () -> StatsPage.multiplier(game.elementMultiplier(ElementEffect.Stat.PARTICLES)), this::tableSeen));
        charted("Ce que les éléments multiplient", row("Éléments : vitesse",
                () -> StatsPage.multiplier(game.elementMultiplier(ElementEffect.Stat.SPEED)), this::tableSeen));
        charted("Ce que l'arbre multiplie", row("Arbre de matière noire : particules",
                () -> Format.multiplier(game.darkParticlesMultiplier()), this::exploded));

        section("Améliorations");
        // Une ligne par amélioration payée en particules : en ajouter une dans core suffit à la faire apparaître.
        for (Upgrade upgrade : game.upgrades(Resource.PARTICLES)) {
            row(upgrade.name(), () -> "niveau " + game.levelOf(upgrade.id())
                    + (game.isMaxed(upgrade.id()) ? " (maximum)" : ", prochain : " + Format.count(game.costOf(upgrade.id()))));
        }
        charted("Niveaux achetés en particules",
                row("Niveaux achetés depuis le début", () -> Format.whole(stats().particleUpgradesBought())));
        row("Générateurs achetés depuis le début", () -> Format.whole(stats().generatorsBought()));
    }

    private void atoms() {
        page("Atomes", "#ffd27f", this::fused, "à la première fusion");
        curves();
        atomsPerFusion();
        curve("Durée d'une partie", () -> true,
                () -> new ChartPane("Durée d'une partie", "temps entre deux fusions · échelle logarithmique",
                        true, Format::clock, value -> Format.clock(Math.pow(10, value)), false, Double.NaN, Double.NaN)
                        .withTicks(DURATION_TICKS),
                new Line("Durée", ChartPane.YELLOW,
                        sample -> sample.fusionTime() > 0 ? Math.log10(Math.max(0.1, sample.fusionTime())) : Double.NaN));
        log("Atomes créés", "depuis le début du jeu", ChartPane.YELLOW, StatsHistory.Stat.ATOMS_CREATED, () -> true);
        log("Atomes dépensés", "depuis le début du jeu", ChartPane.ORANGE, StatsHistory.Stat.ATOMS_SPENT, () -> true);
        count("Fusions", "depuis le début du jeu", ChartPane.YELLOW, StatsHistory.Stat.FUSIONS, () -> true);
        curve("Rendement de fusion", () -> true,
                () -> new ChartPane("Rendement de fusion", "ce que la production multiplie aux atomes",
                        true, Format::clock, value -> "×" + ElementText.number(value), false, 1, Double.NaN),
                new Line("Rendement", ChartPane.YELLOW, sample -> sample.value(StatsHistory.Stat.FUSION_YIELD)));
        curve("Ce que les éléments multiplient", this::tableSeen,
                () -> new ChartPane("Ce que les éléments multiplient", "atomes par fusion · échelle logarithmique",
                        true, Format::clock, value -> Format.multiplier(BigNum.pow10(value)), false, 0, Double.NaN),
                new Line("Atomes par fusion", ChartPane.YELLOW, sample -> sample.value(StatsHistory.Stat.ELEMENT_ATOMS)));
        count("Niveaux achetés en atomes", "depuis le début du jeu", ChartPane.YELLOW,
                StatsHistory.Stat.ATOM_UPGRADES_BOUGHT, () -> true);

        section("Quantités");
        row("Atomes disponibles", () -> Format.amount(game.state().atoms())
                + (game.isAtomCapLifted() ? "" : " / " + Format.count(game.atomCap())));
        row("Créés depuis la dernière explosion", () -> Format.amount(game.state().totalAtoms()));
        charted("Atomes créés", row("Créés depuis le début du jeu", () -> Format.amount(stats().atomsCreated()), this::exploded));
        charted("Atomes dépensés", row("Dépensés", () -> Format.amount(stats().atomsSpent())));

        section("Fusions");
        charted("Fusions", row("Fusions depuis le début du jeu", () -> Format.whole(stats().fusions())));
        row("Fusions depuis la dernière explosion", () -> Format.whole(stats().runFusions()), this::exploded);
        charted("Durée d'une partie", row("Durée de la dernière partie", () -> Format.duration(stats().lastFusionTime())));
        row("Partie la plus courte", () -> Format.duration(stats().fastestFusionTime()));
        row("Partie en cours", () -> Format.duration(game.state().timeSinceFusion()));

        section("Atomes par fusion");
        charted("Atomes par fusion", row("Actuellement", () -> Format.amount(game.atomsPerFusion())));
        row("Record", () -> Format.amount(stats().bestAtomsPerFusion()));
        charted("Rendement de fusion", row("Rendement de fusion", () -> StatsPage.multiplier(game.fusionYield())));
        charted("Ce que les éléments multiplient",
                row("Éléments", () -> StatsPage.multiplier(game.elementMultiplier(ElementEffect.Stat.ATOMS)), this::tableSeen));
        row("Arbre de matière noire", () -> "+" + ElementText.number(game.darkAtomsPerFusion()) + " atome de base", this::exploded);
        row("Améliorations en matière noire", () -> StatsPage.multiplier(game.darkAtomsMultiplier()), this::exploded);
        row("Groupes de générateurs fusionnés", () -> String.valueOf(game.fusionGroups()),
                () -> game.maxGeneratorCount() > game.generatorsPerAtom());
        row("Prime de groupe", () -> StatsPage.multiplier(game.fusionGroupBonus()),
                () -> game.maxGeneratorCount() > game.generatorsPerAtom());

        section("Améliorations");
        // Une ligne par amélioration payée en atomes.
        for (Upgrade upgrade : game.upgrades(Resource.ATOMS)) {
            row(upgrade.name(), () -> (upgrade.maxLevel() == 1
                    ? (game.levelOf(upgrade.id()) > 0 ? "acquise" : "à acheter : " + Format.count(game.costOf(upgrade.id())))
                    : "niveau " + game.levelOf(upgrade.id())
                            + (game.isMaxed(upgrade.id()) ? " (maximum)" : ", prochain : " + Format.count(game.costOf(upgrade.id())))));
        }
        charted("Niveaux achetés en atomes",
                row("Niveaux achetés depuis le début", () -> Format.whole(stats().atomUpgradesBought())));
    }

    private void automation() {
        page("Automatisation", "#9be7a8", this::automationSeen,
                "à " + Format.count(Game.UNLOCK_TOTAL_ATOMS) + " atomes créés");
        curves();
        log("Actions des automatismes", "depuis le début du jeu, ordinaires et de matière noire", ChartPane.GREEN,
                StatsHistory.Stat.AUTOMATION_ACTIONS, () -> true);
        // Une courbe par automatisme du catalogue : son délai, du jour où il est acheté.
        for (Automation automation : game.automations()) {
            String name = "Délai : " + automation.name();
            curve(name, () -> game.ownsAutomation(automation.id()) || stats().wasUnlocked(automation.id()),
                    () -> new ChartPane(name, "temps entre deux actions · échelle logarithmique",
                            true, Format::clock, StatsPage::delay, false, Double.NaN, Double.NaN).withTicks(DELAY_TICKS),
                    new Line("Délai", ChartPane.GREEN, sample -> Math.log10(sample.delay(automation.id()))));
        }
        section("Activité");
        charted("Actions des automatismes",
                row("Actions des automatismes ordinaires", () -> Format.whole(stats().automationActions())));
        row("Actions des automatismes de matière noire", () -> Format.whole(stats().darkAutomationActions()), this::exploded);
        row("Créations automatiques de molécules", () -> Format.whole(stats().autoMoleculeCreations()),
                game::isMoleculeAutomationUnlocked);
        row("Automatismes possédés", () -> game.state().ownedAutomations().size() + " / " + game.automations().size());
        row("Délai le plus court possible", () -> AutomationPage.seconds(game.minAutomationInterval()));
        row("Améliorations en matière noire", () -> "délais ÷" + ElementText.number(game.darkAutomationDivisor()), this::exploded);

        section("Chaque automatisme");
        for (Automation automation : game.automations()) {
            charted("Délai : " + automation.name(), row(automation.name(), () -> !game.ownsAutomation(automation.id())
                            ? "pas encore acheté"
                            : "une action toutes les " + AutomationPage.seconds(game.automationInterval(automation.id()))
                                    + ", cadence " + game.automationSpeedLevel(automation.id()) + "/" + automation.maxSpeedLevel()
                                    + (game.isAutomationEnabled(automation.id()) ? "" : " (coupé)"),
                    () -> game.isAutomationAvailable(automation.id()) || game.ownsAutomation(automation.id())));
        }
    }

    private void periodicTable() {
        page("Tableau périodique", "#f0a8c0", this::tableSeen,
                "à " + Format.count(Game.UNLOCK_TOTAL_ATOMS) + " atomes créés");
        curves();
        tableProgress();
        count("Synthèses", "depuis le début du jeu", ChartPane.ORANGE, StatsHistory.Stat.SYNTHESES, () -> true);
        log("Prix de la prochaine synthèse", "en atomes", ChartPane.YELLOW, StatsHistory.Stat.SYNTHESIS_COST, () -> true);
        curve("Chance de tirage double", () -> true,
                () -> new ChartPane("Chance de tirage double", "chance qu'une synthèse donne deux éléments",
                        true, Format::clock, value -> ElementText.number(value) + " %", false, 0, Double.NaN),
                new Line("Chance", ChartPane.BLUE, sample -> 100 * sample.value(StatsHistory.Stat.DOUBLE_DRAW)));
        curve("Ensembles réunis", () -> true,
                () -> new ChartPane("Ensembles réunis", "sur " + game.elementSets().size() + " : dix familles et sept périodes",
                        true, Format::clock, value -> Format.whole(Math.round(value)), true, 0, Double.NaN),
                new Line("Complets", ChartPane.BLUE, sample -> sample.value(StatsHistory.Stat.SETS)),
                new Line("À moitié", ChartPane.ORANGE, sample -> sample.value(StatsHistory.Stat.HALF_SETS)));
        curve("Ce que les éléments multiplient", () -> true,
                () -> new ChartPane("Ce que les éléments multiplient", "particules et vitesse · échelle logarithmique",
                        true, Format::clock, value -> Format.multiplier(BigNum.pow10(value)), false, 0, Double.NaN),
                new Line("Particules", ChartPane.BLUE, sample -> sample.value(StatsHistory.Stat.ELEMENT_PARTICLES)),
                new Line("Vitesse", ChartPane.ORANGE, sample -> sample.value(StatsHistory.Stat.ELEMENT_SPEED)));

        section("Collection");
        charted("Avancement du tableau périodique",
                row("Éléments découverts", () -> game.discoveredElements() + " / " + PeriodicTable.ELEMENTS.size()));
        charted("Avancement du tableau périodique",
                row("Exemplaires possédés", () -> game.ownedCopies() + " / " + game.maxTotalCopies()));
        row("Élément unique obtenu", () -> game.isSynthesisAutomationUnlocked() ? "oui" : "pas encore");
        charted("Ensembles réunis", row("Ensembles complets", () -> game.completedSets() + " / " + game.elementSets().size()
                + (game.halfSets() > 0 ? " (et " + game.halfSets() + " à moitié)" : "")));

        section("Synthèses");
        row("Depuis la dernière explosion", () -> Format.whole(game.state().synthesisCount()));
        charted("Synthèses", row("Depuis le début du jeu", () -> Format.whole(stats().syntheses()), this::exploded));
        row("Exemplaires obtenus depuis le début", () -> Format.whole(stats().elementsObtained()));
        row("Tirages doubles réussis", () -> Format.whole(stats().doubleDraws()));
        charted("Prix de la prochaine synthèse", row("Prix de la prochaine synthèse",
                () -> game.isPeriodicTableComplete() ? "tableau complet" : Format.count(game.synthesisCost()) + " atomes"));
        charted("Chance de tirage double",
                row("Chance de tirage double", () -> ElementText.percent(Math.min(1, game.doubleDrawChance()))));
        row("Synthèses ciblées abouties", () -> Format.whole(stats().targetedSyntheses()) + " (pour "
                + Format.whole(stats().targetTries()) + " étapes payées sans rien obtenir)", () -> stats().targetedSyntheses() > 0
                || stats().targetTries() > 0);
        row("Réserve de la synthèse automatique", () -> Format.count(game.synthesisReserve()) + " atomes",
                () -> game.synthesisReserve().sign() > 0);
        row("Éléments par synthèse", () -> String.valueOf(game.elementsPerSynthesis()), this::exploded);
        row("Élément unique garanti à la synthèse n°", () -> String.valueOf(game.guaranteedUniqueSynthesis()),
                () -> !game.isSynthesisAutomationUnlocked());

        section("Ce que rapportent les éléments");
        charted("Ce que les éléments multiplient",
                row("Particules", () -> StatsPage.multiplier(game.elementMultiplier(ElementEffect.Stat.PARTICLES))));
        charted("Ce que les éléments multiplient",
                row("Vitesse", () -> StatsPage.multiplier(game.elementMultiplier(ElementEffect.Stat.SPEED))));
        row("Atomes par fusion", () -> StatsPage.multiplier(game.elementMultiplier(ElementEffect.Stat.ATOMS)));
        row("Prix de la vitesse", () -> "÷" + ElementText.number(game.elementCostDivisor(ElementEffect.CostTarget.SPEED_UPGRADES)));
        row("Prix des améliorations en atomes",
                () -> "÷" + ElementText.number(game.elementCostDivisor(ElementEffect.CostTarget.ATOM_UPGRADES)));
        row("Prix de la synthèse", () -> "÷" + ElementText.number(game.elementCostDivisor(ElementEffect.CostTarget.SYNTHESIS)));

        section("Par famille : éléments découverts, exemplaires");
        for (ElementCategory category : ElementCategory.values()) {
            List<Element> members = PeriodicTable.elements(category);
            row(category.label(), () -> {
                int discovered = 0, copies = 0, most = 0;
                for (Element element : members) {
                    int count = game.elementCount(element.number());
                    if (count > 0) discovered++;
                    copies += count;
                    most += game.maxCopiesOf(element);     // les molécules relèvent certains plafonds un à un
                }
                return discovered + " / " + members.size() + ", " + copies + " / "
                        + most + " ex.   (chance "
                        + ElementText.percent(game.categoryChance(category)) + ")";
            });
        }
    }

    private void darkMatter() {
        page("Matière noire", GameApp.DARK_MATTER_COLOR, this::exploded, "à la première explosion");
        curves();
        darkMatterSize();
        // Une abscisse par explosion, et non par instant : cette courbe ne dépend pas de la période choisie.
        building.graphs.add(new Graph("Durée de chaque explosion", () -> true,
                () -> new ChartPane("Durée de chaque explosion", "temps de jeu entre deux explosions · échelle logarithmique",
                        false, value -> "n° " + Math.round(value), value -> Format.clock(Math.pow(10, value)),
                        false, Double.NaN, Double.NaN).withTicks(DURATION_TICKS).withMarkers(),
                this::explosionSeries, false));
        count("Matière noire gagnée", "depuis le début du jeu", ChartPane.VIOLET, StatsHistory.Stat.DARK_MATTER_EARNED, () -> true);
        count("Explosions", "depuis le début du jeu", ChartPane.VIOLET, StatsHistory.Stat.EXPLOSIONS, () -> true);
        curve("Croissance par seconde d'appui", () -> true,
                () -> new ChartPane("Croissance par seconde d'appui", "échelle logarithmique",
                        true, Format::clock, value -> "+" + ElementText.percent(Math.pow(10, value)), false, Double.NaN, Double.NaN),
                new Line("Croissance", ChartPane.VIOLET, sample -> sample.value(StatsHistory.Stat.DARK_GROWTH)));
        log("Résistance", "ce qui divise la croissance", ChartPane.ORANGE, StatsHistory.Stat.DARK_RESISTANCE, () -> true);
        curve("Temps d'appui", () -> true,
                () -> new ChartPane("Temps d'appui", "cumulé depuis le début du jeu",
                        true, Format::clock, Format::clock, false, 0, Double.NaN),
                new Line("Appui", ChartPane.VIOLET, sample -> sample.value(StatsHistory.Stat.HOLD_TIME)));
        count("Paliers de taille", "sur " + SizeScale.MILESTONES.size(), ChartPane.TEAL, StatsHistory.Stat.LANDMARKS, () -> true);

        section("Quantités");
        row("Matière noire disponible", () -> Format.darkMatter(game.state().darkMatter()));
        charted("Matière noire gagnée", row("Gagnée depuis le début", () -> Format.darkMatter(game.darkMatterEarned())));
        row("Dépensée en améliorations", () -> Format.darkMatter(game.state().darkMatterSpent()));
        row("Par explosion", () -> "+" + Format.darkMatter(game.darkMatterPerExplosion()));

        section("Explosions");
        charted("Explosions", row("Explosions", () -> Format.whole(game.state().explosions())));
        charted("Durée de chaque explosion", row("Durée de la dernière", () -> Format.duration(stats().lastExplosionTime())));
        row("La plus rapide", () -> Format.duration(stats().fastestExplosionTime()));
        row("Partie en cours", () -> Format.duration(stats().runTime()));
        row("Masse du tableau périodique", () -> Format.multiplier(game.tableWeight()) + " : synthèses jusqu'à "
                + Format.count(game.atomCap()) + " atomes");
        row("Défis réussis", () -> game.completedChallenges() + " / " + game.challenges().size());
        row("Défi en cours", () -> game.activeChallenge().name(), game::inChallenge);
        row("Primes de vitesse touchées", () -> Format.whole(stats().speedPrimes()), () -> game.explosionRecord() > 0 || stats().speedPrimes() > 0);
        row("Prime de vitesse : exploser en moins de", () -> Format.duration(game.speedPrimeTime())
                + (game.earnsSpeedPrime() ? " (encore " + Format.wait(Math.max(1, game.speedPrimeLeft())) + ")" : " (trop tard pour cette partie)"),
                () -> game.explosionRecord() > 0);

        section("Taille");
        charted("Taille de la matière noire", row("Taille actuelle", () -> Format.length(game.state().darkMatterSize())));
        row("En années-lumière", () -> Format.amount(game.darkMatterLightYears()),
                () -> game.darkMatterLightYears().gte(BigNum.of(0.1)));
        row("Dernier repère dépassé", () -> {
            Landmark reached = SizeScale.reached(game.state().darkMatterSize());
            return reached == null ? "aucun" : reached.name();
        });
        row("Prochain repère", () -> {
            Landmark next = SizeScale.next(game.state().darkMatterSize());
            return next == null ? "plus grand que tous les repères" : next.name() + " (" + Format.length(next.size()) + ")";
        });
        charted("Temps d'appui", row("Temps d'appui", () -> Format.duration(stats().holdTime())));
        charted("Paliers de taille",
                row("Paliers de taille atteints", () -> game.landmarksReached() + " / " + SizeScale.MILESTONES.size()));
        row("Paliers : particules", () -> Format.multiplier(game.landmarkParticlesMultiplier()), () -> game.landmarksReached() > 0);
        row("Paliers : atomes par fusion", () -> StatsPage.multiplier(game.landmarkAtomsMultiplier()), () -> game.landmarksReached() > 1);
        row("Paliers : croissance", () -> StatsPage.multiplier(game.landmarkExpansionMultiplier()), () -> game.landmarksReached() > 2);

        section("Croissance");
        charted("Croissance par seconde d'appui", row("Par seconde d'appui", () -> {
            BigNum growth = game.darkMatterGrowthPerSecond();
            return growth.lt(BigNum.of(2)) ? "+" + ElementText.percent(growth.toDouble() - 1) : Format.multiplier(growth);
        }));
        row("Avancement de la partie", () -> StatsPage.multiplier(game.darkMatterProgressFactor()));
        row("Arbre : élan", () -> Format.multiplier(game.darkExpansionMultiplier()));
        charted("Résistance", row("Résistance", () -> "÷" + Format.amount(game.darkMatterResistance())));
        row("Croissance spontanée", () -> ElementText.percent(game.darkAutoExpansionShare()) + " de la vitesse d'appui",
                () -> game.darkAutoExpansionShare() > 0);

        section("Améliorations");
        row("Cases de l'arbre acquises", () -> owned(true) + " / " + count(true));
        row("Améliorations en matière noire acquises", () -> owned(false) + " / " + count(false));
        row("Atomes comptés au départ de chaque partie", () -> Format.count(game.darkHeadStartAtoms()),
                () -> game.darkHeadStartAtoms().sign() > 0);
    }

    /** Une courbe d'un graphique qui suit le temps : son nom, sa couleur, et la valeur à lire dans chaque relevé. */
    record Line(String name, String color, ToDoubleFunction<StatsHistory.Sample> value) {}

    List<StatsHistory.Sample> liveSamples;
    boolean liveRun;
    double liveAt;

    final VBox journalList = new VBox(3);
    final Label journalNote = new Label();
    int journalVersion = -1;
}
