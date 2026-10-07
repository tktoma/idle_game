package idle.ui;

import idle.core.Automation;
import idle.core.BigBangMilestone;
import idle.core.BigNum;
import idle.core.SpaceUpgrade;
import idle.core.Molecule;
import idle.core.Body;
import idle.core.Cosmos;
import idle.core.DarkUpgrade;
import idle.core.Element;
import idle.core.ElementCategory;
import idle.core.ElementEffect;
import idle.core.Game;
import idle.core.GameStats;
import idle.core.Landmark;
import idle.core.PeriodicTable;
import idle.core.Resource;
import idle.core.SizeScale;
import idle.core.StatsHistory;
import idle.core.Upgrade;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

/**
 * Contenu de l'onglet « Statistiques » : tout ce que le jeu compte, rangé en sous-pages, une par
 * ressource. Chaque sous-page n'apparaît qu'avec la ressource dont elle parle :
 * <ul>
 *   <li>« Général » : toujours là ;</li>
 *   <li>« Particules » : dès le premier générateur ;</li>
 *   <li>« Atomes » : dès la première fusion ;</li>
 *   <li>« Automatisation » : dès qu'un automatisme est débloqué ;</li>
 *   <li>« Tableau périodique » : dès que le tableau est ouvert ;</li>
 *   <li>« Matière noire » : dès la première explosion.</li>
 * </ul>
 * Une sous-page débloquée le reste, même si une explosion fait disparaître la ressource. À
 * l'intérieur d'une sous-page, certaines lignes n'apparaissent elles aussi qu'en avançant : le
 * multiplicateur des éléments n'a pas de sens avant le tableau périodique.
 *
 * <p>Chaque sous-page commence par ses courbes : un menu déroulant propose toutes les
 * statistiques de la sous-page qui évoluent avec le temps, et le graphique ({@link ChartPane})
 * trace celle qui est choisie. Les courbes viennent de l'historique que tient le jeu
 * ({@link StatsHistory}), qui relève une valeur par statistique suivie. Une courbe n'est
 * proposée qu'une fois sa statistique apparue dans la partie. Après la première explosion, deux
 * boutons choisissent la période : tout le jeu, ou la partie en cours.
 *
 * <p>Les succès ont leur propre onglet ({@link AchievementsPage}) ; seule leur courbe est ici,
 * dans la vue d'ensemble de « Général ».
 *
 * <p>Sous les courbes, les valeurs du moment, une ligne chacune. Une ligne dont la statistique
 * a une courbe se clique : le menu passe à cette courbe. Ce qui ne se trace pas reste en lignes
 * seulement : les records, les réglages, le détail par famille.
 *
 * <p>Rien n'est calculé ici : les compteurs viennent de {@link GameStats}, les valeurs du moment
 * de {@link Game}. Seule la sous-page affichée est mise à jour.
 */
final class StatsPage extends VBox {

    private static final String STATS_COLOR = "#7fe0d4";
    private static final String SUB_TAB_STYLE = "-fx-font-size: 13px; -fx-padding: 6 16; -fx-cursor: hand;"
            + " -fx-background-radius: 0; -fx-border-width: 0 0 2 0; -fx-background-color: transparent;";
    private static final String NAME_STYLE = "-fx-font-size: 13px; -fx-text-fill: #b9c7d6;";
    private static final String VALUE_STYLE = "-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #e8f4ff;";
    private static final String ROW_STYLE = "-fx-padding: 5 12; -fx-background-color: #121821; -fx-background-radius: 4;";
    /** Une ligne qui mène à une courbe, sous la souris. */
    private static final String ROW_HOVER_STYLE = "-fx-padding: 5 12; -fx-background-color: #1c2735; -fx-background-radius: 4;"
            + " -fx-cursor: hand;";
    private static final double MAX_WIDTH = 640;

    /** Graduations d'un axe de durées en échelle logarithmique : 1 s, 10 s, 1 min, 10 min, 1 h, 1 jour, 10 jours. */
    private static final double[] DURATION_TICKS = {0, 1, Math.log10(60), Math.log10(600), Math.log10(3_600),
            Math.log10(86_400), Math.log10(864_000)};

    /** Graduations d'un axe de délais d'automatisme, en échelle logarithmique : 0,01 s, 0,1 s, 1 s, 10 s, 1 min. */
    private static final double[] DELAY_TICKS = {-2, -1, 0, 1, Math.log10(60)};

    /**
     * Une courbe que le menu déroulant propose.
     *
     * <p>Son graphique n'est construit que la première fois qu'on la choisit : la plupart des
     * courbes ne sont jamais regardées, et chaque graphique porte une zone de dessin.
     */
    private static final class Graph {
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
    private record Row(String name, Node box, Label value, Supplier<String> text, BooleanSupplier visible) {}

    /** Une sous-page : son onglet, son contenu, et ce qui la débloque. */
    private static final class SubPage {
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

    private final Game game;
    private final List<SubPage> pages = new ArrayList<>();
    private final FlowPane subTabs = new FlowPane();
    private final StackPane stack = new StackPane();
    private final Label nextLabel = new Label();
    // Le choix de la période des graphiques, proposé après la première explosion.
    private final Button wholeButton = new Button("Tout le jeu");
    private final Button runButton = new Button("Depuis la dernière explosion");
    private final HBox rangeRow = new HBox(8, wholeButton, runButton);
    /** Vrai si les graphiques ne montrent que la partie en cours. */
    private boolean runOnly = false;
    private SubPage selected;
    /** La sous-page en cours de construction : {@link #section} et {@link #row} s'y ajoutent. */
    private SubPage building;
    private GameStats current;
    /** Vrai pendant que la page remplit elle-même un menu déroulant : ce n'est pas un choix du joueur. */
    private boolean updating = false;

    StatsPage(Game game) {
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

        VBox.setVgrow(stack, Priority.ALWAYS);
        getChildren().add(title);
        getChildren().add(subTabs);
        getChildren().add(rangeRow);
        getChildren().add(stack);
        getChildren().add(nextLabel);
        select(pages.get(0));
    }

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
                () -> multiplier(game.elementMultiplier(ElementEffect.Stat.PARTICLES)), this::tableSeen));
        charted("Ce que les éléments multiplient", row("Éléments : vitesse",
                () -> multiplier(game.elementMultiplier(ElementEffect.Stat.SPEED)), this::tableSeen));
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
        charted("Rendement de fusion", row("Rendement de fusion", () -> multiplier(game.fusionYield())));
        charted("Ce que les éléments multiplient",
                row("Éléments", () -> multiplier(game.elementMultiplier(ElementEffect.Stat.ATOMS)), this::tableSeen));
        row("Arbre de matière noire", () -> "+" + ElementText.number(game.darkAtomsPerFusion()) + " atome de base", this::exploded);
        row("Améliorations en matière noire", () -> multiplier(game.darkAtomsMultiplier()), this::exploded);
        row("Groupes de générateurs fusionnés", () -> String.valueOf(game.fusionGroups()),
                () -> game.maxGeneratorCount() > game.generatorsPerAtom());
        row("Prime de groupe", () -> multiplier(game.fusionGroupBonus()),
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
                row("Particules", () -> multiplier(game.elementMultiplier(ElementEffect.Stat.PARTICLES))));
        charted("Ce que les éléments multiplient",
                row("Vitesse", () -> multiplier(game.elementMultiplier(ElementEffect.Stat.SPEED))));
        row("Atomes par fusion", () -> multiplier(game.elementMultiplier(ElementEffect.Stat.ATOMS)));
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
        row("Matière noire disponible", () -> Format.count(game.state().darkMatter()));
        charted("Matière noire gagnée", row("Gagnée depuis le début", () -> Format.count(game.darkMatterEarned())));
        row("Dépensée en améliorations", () -> Format.count(game.state().darkMatterSpent()));
        row("Par explosion", () -> "+" + Format.count(game.darkMatterPerExplosion()));

        section("Explosions");
        charted("Explosions", row("Explosions", () -> Format.whole(game.state().explosions())));
        charted("Durée de chaque explosion", row("Durée de la dernière", () -> Format.duration(stats().lastExplosionTime())));
        row("La plus rapide", () -> Format.duration(stats().fastestExplosionTime()));
        row("Partie en cours", () -> Format.duration(stats().runTime()));
        row("Masse du tableau périodique", () -> Format.multiplier(game.tableWeight()) + " : synthèses jusqu'à "
                + Format.count(game.atomCap()) + " atomes");
        row("Défis réussis", () -> game.completedChallenges() + " / " + game.challenges().size());
        row("Défi en cours", () -> game.activeChallenge().name(), game::inChallenge);

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
        row("Paliers : atomes par fusion", () -> multiplier(game.landmarkAtomsMultiplier()), () -> game.landmarksReached() > 1);
        row("Paliers : croissance", () -> multiplier(game.landmarkExpansionMultiplier()), () -> game.landmarksReached() > 2);

        section("Croissance");
        charted("Croissance par seconde d'appui", row("Par seconde d'appui", () -> {
            BigNum growth = game.darkMatterGrowthPerSecond();
            return growth.lt(BigNum.of(2)) ? "+" + ElementText.percent(growth.toDouble() - 1) : Format.multiplier(growth);
        }));
        row("Avancement de la partie", () -> multiplier(game.darkMatterProgressFactor()));
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

    // ------------------------------------------------------------------
    // Les courbes proposées sur plusieurs sous-pages : la principale de chaque ressource
    // ------------------------------------------------------------------

    /**
     * Toutes les ressources sur un même graphique : ce que le joueur a en main au fil du temps.
     *
     * <p>Les particules montent à 1e3000 quand les molécules se comptent en milliers : sur une
     * échelle logarithmique ordinaire, tout sauf les particules serait écrasé en bas. L'échelle
     * est donc logarithmique deux fois : la hauteur suit le logarithme de l'exposant, et les
     * graduations vont de 1 à 10, 1000, 1e10, 1e100, 1e1000. Une ressource n'a sa courbe qu'à
     * partir du moment où elle existe. Chaque relevé garde le plus haut atteint depuis le
     * précédent : une fusion, une explosion ou un Big Bang n'y creusent donc un trou que s'ils
     * durent. La matière noire est celle gagnée, dépensée ou non : la réserve seule retombe à
     * zéro à chaque achat dans l'arbre.
     */
    private void resources() {
        building.graphs.add(new Graph("Toutes les ressources", game::isStarted,
                () -> new ChartPane("Toutes les ressources", "ce que vous avez en main, matière noire dépensée comprise · échelle "
                        + "logarithmique double", true, Format::clock, StatsPage::doubleLog,
                        false, 0, Double.NaN).withTicks(DOUBLE_LOG_TICKS).withHeight(340),
                () -> {
                    List<StatsHistory.Sample> samples = history().samples();
                    List<ChartPane.Series> series = new ArrayList<>();
                    for (Held each : RESOURCES) {
                        double[] x = new double[samples.size()];
                        double[] y = new double[samples.size()];
                        boolean seen = false;
                        for (int i = 0; i < x.length; i++) {
                            x[i] = samples.get(i).time();
                            double exponent = samples.get(i).value(each.stat());
                            if (each.counted()) exponent = exponent >= 1 ? Math.log10(exponent) : Double.NaN;
                            y[i] = Double.isNaN(exponent) ? Double.NaN : Math.log10(1 + Math.max(0, exponent));
                            seen |= !Double.isNaN(y[i]);
                        }
                        // Une ressource qui n'existe pas encore n'encombre pas la légende.
                        if (seen) series.add(new ChartPane.Series(each.name(), each.color(), x, y));
                    }
                    return series;
                }, true));
    }

    /**
     * Une ressource du graphique d'ensemble.
     *
     * @param counted vrai si le relevé garde un nombre (des molécules), faux s'il garde déjà une puissance de dix
     */
    private record Held(String name, String color, StatsHistory.Stat stat, boolean counted) {}

    /** Les ressources du graphique d'ensemble, dans l'ordre où le jeu les fait découvrir. Les couleurs voisines restent distinctes. */
    private static final List<Held> RESOURCES = List.of(
            new Held("Particules", ChartPane.BLUE, StatsHistory.Stat.PARTICLES, false),
            new Held("Atomes", ChartPane.YELLOW, StatsHistory.Stat.ATOMS, false),
            new Held("Éléments", ChartPane.MAGENTA, StatsHistory.Stat.ELEMENT_COPIES, true),
            new Held("Matière noire", ChartPane.VIOLET, StatsHistory.Stat.DARK_MATTER, false),
            new Held("Espace", ChartPane.ORANGE, StatsHistory.Stat.SPACE, false),
            new Held("Molécules", ChartPane.AQUA, StatsHistory.Stat.MOLECULES, true));

    /** Graduations de l'échelle logarithmique double : 1, 10, 1000, 1e10, 1e30, 1e100, 1e300, 1e1000… */
    private static final double[] DOUBLE_LOG_TICKS = doubleLogTicks(0, 1, 3, 10, 30, 100, 300, 1_000, 3_000, 10_000, 100_000, 1_000_000);

    private static double[] doubleLogTicks(double... exponents) {
        double[] ticks = new double[exponents.length];
        for (int i = 0; i < ticks.length; i++) ticks[i] = Math.log10(1 + exponents[i]);
        return ticks;
    }

    /** Une hauteur de l'échelle logarithmique double, écrite comme la quantité qu'elle représente : 0 → « 1 », 2 → « 1e99 ». */
    private static String doubleLog(double height) {
        double exponent = Math.pow(10, height) - 1;
        if (Math.abs(exponent - Math.rint(exponent)) < 1e-6) exponent = Math.rint(exponent);
        return power(exponent);
    }

    private void production() {
        curve("Production de particules", game::isStarted,
                () -> new ChartPane("Production de particules", "par seconde · échelle logarithmique",
                        true, Format::clock, StatsPage::power, true, Double.NaN, Double.NaN),
                new Line("Production", ChartPane.BLUE, StatsHistory.Sample::production));
    }

    private void atomsPerFusion() {
        curve("Atomes par fusion", this::fused,
                () -> new ChartPane("Atomes par fusion", "échelle logarithmique",
                        true, Format::clock, StatsPage::power, true, 0, Double.NaN),
                new Line("Atomes par fusion", ChartPane.YELLOW, StatsHistory.Sample::atomsPerFusion));
    }

    private void tableProgress() {
        curve("Avancement du tableau périodique", this::tableSeen,
                () -> new ChartPane("Avancement du tableau périodique", "en % du total",
                        true, Format::clock, value -> ElementText.number(value) + " %", false, 0, 100),
                new Line("Éléments découverts", ChartPane.BLUE, sample -> 100 * sample.elementsShare()),
                new Line("Exemplaires", ChartPane.ORANGE, sample -> 100 * sample.copiesShare()));
    }

    private void darkMatterSize() {
        curve("Taille de la matière noire", this::exploded,
                () -> new ChartPane("Taille de la matière noire", "échelle logarithmique",
                        true, Format::clock, value -> Format.length(BigNum.pow10(value)), true, Double.NaN, Double.NaN),
                new Line("Taille", ChartPane.VIOLET, StatsHistory.Sample::darkMatterSize));
    }

    private void achievementCount() {
        count("Succès obtenus", "sur " + game.achievements().size(), ChartPane.TEAL, StatsHistory.Stat.ACHIEVEMENTS,
                () -> !game.achievements().isEmpty());
    }

    // ------------------------------------------------------------------
    // Ce que les lignes ont besoin de savoir
    // ------------------------------------------------------------------

    /** Les compteurs, relevés une fois par mise à jour de la page ({@link #refresh()}). */
    private GameStats stats() {
        if (current == null) current = game.stats();
        return current;
    }

    private boolean fused() {
        return stats().fusions() > 0 || game.state().totalAtoms().sign() > 0;
    }

    private boolean exploded() {
        return game.isDarkMatterUnlocked();
    }

    private boolean tableSeen() {
        return game.isPeriodicTableUnlocked() || stats().syntheses() > 0 || game.discoveredElements() > 0;
    }

    private boolean automationSeen() {
        return AutomationPage.hasContent(game) || stats().automationActions() > 0 || stats().darkAutomationActions() > 0;
    }

    /** Nombre d'améliorations de matière noire possédées (au moins un niveau), dans l'arbre ou hors de l'arbre. */
    private int owned(boolean inTree) {
        int owned = 0;
        for (DarkUpgrade upgrade : game.darkUpgrades()) {
            if (upgrade.branch().inTree() == inTree && game.darkLevelOf(upgrade.id()) > 0) owned++;
        }
        return owned;
    }

    private int count(boolean inTree) {
        int count = 0;
        for (DarkUpgrade upgrade : game.darkUpgrades()) {
            if (upgrade.branch().inTree() == inTree) count++;
        }
        return count;
    }

    /** Le prochain grand pas de la partie en cours, en une phrase. */
    private String nextGoal() {
        return Goals.next(game);
    }

    /** Le troisième acte : l'espace, les molécules, ce qu'elles forment, et ce que tout cela multiplie. */
    private void bigBang() {
        page("Big Bang", GameApp.BIG_BANG_COLOR, game::isBigBangUnlocked, "au premier Big Bang");
        curves();
        log("Espace créé", "par l'expansion de la matière", ChartPane.YELLOW, StatsHistory.Stat.SPACE, () -> true);
        log("Espace utilisé", "par les molécules et leurs lieux de rassemblement", ChartPane.ORANGE,
                StatsHistory.Stat.SPACE_USED, () -> game.moleculesCreated() > 0);
        // Une abscisse par Big Bang, et non par instant : cette courbe ne dépend pas de la période choisie.
        building.graphs.add(new Graph("Durée de chaque Big Bang", () -> true,
                () -> new ChartPane("Durée de chaque Big Bang", "temps de jeu entre deux Big Bangs · échelle logarithmique",
                        false, value -> "n° " + Math.round(value), value -> Format.clock(Math.pow(10, value)),
                        false, Double.NaN, Double.NaN).withTicks(DURATION_TICKS).withMarkers(),
                this::bigBangSeries, false));
        count("Big Bangs", "depuis le début du jeu", ChartPane.YELLOW, StatsHistory.Stat.BIG_BANGS, () -> true);
        log("Espace par seconde", "ce que l'expansion ajoute chaque seconde", ChartPane.YELLOW, StatsHistory.Stat.SPACE_RATE, () -> true);
        curve("Expansion multipliée par la matière noire", this::darkSpace,
                () -> new ChartPane("Expansion multipliée par la matière noire", "selon la matière noire en réserve",
                        true, Format::clock, StatsPage::multiplier, false, 1, Double.NaN),
                new Line("Matière noire", ChartPane.VIOLET, sample -> sample.value(StatsHistory.Stat.DARK_SPACE)));
        count("Molécules créées", "", ChartPane.YELLOW, StatsHistory.Stat.MOLECULES, () -> game.moleculesCreated() > 0);
        curve("Créations de molécules", () -> stats().moleculeCreations() > 0,
                () -> new ChartPane("Créations de molécules", "depuis le début du jeu, à la main et automatiques",
                        true, Format::clock, value -> Format.whole(Math.round(value)), true, 0, Double.NaN),
                new Line("Toutes", ChartPane.YELLOW, sample -> sample.value(StatsHistory.Stat.MOLECULE_CREATIONS)),
                new Line("Automatiques", ChartPane.GREEN, sample -> sample.value(StatsHistory.Stat.AUTO_MOLECULE_CREATIONS)));
        count("Sortes rassemblées", "", ChartPane.ORANGE, StatsHistory.Stat.SUBSTANCES, () -> game.substancesFormed() > 0);
        count("Assemblages et astres formés", "galaxie, amas de galaxies et univers compris", ChartPane.TEAL, StatsHistory.Stat.SKY,
                () -> game.assembliesFormed() > 0);
        curve("Ce que la matière multiplie : espace et matière noire", this::boosted,
                () -> new ChartPane("Ce que la matière multiplie", "espace par seconde et croissance de la matière noire · échelle logarithmique",
                        true, Format::clock, value -> Format.multiplier(BigNum.pow10(value)), false, 0, Double.NaN),
                new Line("Espace", ChartPane.YELLOW, sample -> sample.value(StatsHistory.Stat.MOLECULE_SPACE)),
                new Line("Matière noire", ChartPane.VIOLET, sample -> sample.value(StatsHistory.Stat.MOLECULE_DARK)));
        curve("Ce que la matière multiplie : particules et atomes", this::boosted,
                () -> new ChartPane("Ce que la matière multiplie", "particules et atomes par fusion · échelle logarithmique",
                        true, Format::clock, value -> Format.multiplier(BigNum.pow10(value)), false, 0, Double.NaN),
                new Line("Particules", ChartPane.BLUE, sample -> sample.value(StatsHistory.Stat.MOLECULE_PARTICLES)),
                new Line("Atomes", ChartPane.ORANGE, sample -> sample.value(StatsHistory.Stat.MOLECULE_ATOMS)));

        section("Avancée");
        row("Prochain pas", () -> Goals.act(game));
        charted("Big Bangs", row("Big Bangs", () -> Format.whole(game.bigBangs())));
        row("Paliers de Big Bang", () -> game.bigBangMilestonesReached() + " / " + game.bigBangMilestones().size());
        row("Améliorations d'espace prises", () -> {
            int owned = 0;
            for (SpaceUpgrade upgrade : game.spaceUpgrades()) {
                if (game.ownsSpaceUpgrade(upgrade.id())) owned++;
            }
            return owned + " / " + game.spaceUpgrades().size();
        });
        row("Appui automatique sur la matière noire", () -> game.isAutoHoldEnabled() ? "en marche" : "coupé", game::isAutoHoldUnlocked);
        row("Création automatique des molécules", () -> (game.isMoleculeAutomationEnabled() ? "en marche, " : "coupée, ")
                + game.automatedMolecules() + (game.automatedMolecules() > 1 ? " amas confiés" : " amas confié"),
                game::isMoleculeAutomationUnlocked);
        charted("Expansion multipliée par la matière noire", row("Expansion multipliée par la matière noire",
                () -> multiplier(game.darkMatterSpaceBoost()), () -> game.darkMatterSpaceBoost() > 1));

        section("Big Bangs");
        charted("Durée de chaque Big Bang", row("Durée du dernier", () -> Format.duration(stats().lastBigBangTime())));
        row("Le plus rapide", () -> Format.duration(stats().fastestBigBangTime()));
        row("Depuis le dernier Big Bang", () -> Format.duration(stats().bigBangTime()));
        row("Explosions du dernier Big Bang", () -> Format.whole(stats().lastBigBangExplosions()));
        row("Explosions depuis le dernier Big Bang", () -> Format.whole(game.state().explosions()));
        row("Arbre de matière noire au prochain Big Bang", () -> game.bigBangKeepsDarkTree() ? "il reste" : "il repart de zéro");

        section("Les premières");
        for (GameStats.Step step : GameStats.Step.values()) {
            row(step.label(), () -> reached(step), () -> stats().reached(step));
        }

        section("Ce que donnent les paliers");
        row("Matière noire des explosions", () -> multiplier(game.bigBangDarkMatterFactor()), () -> game.bigBangDarkMatterFactor() > 1);
        row("Espace par seconde", () -> multiplier(game.bigBangMilestoneBoost(Molecule.Stat.SPACE)),
                () -> game.bigBangMilestoneBoost(Molecule.Stat.SPACE) > 1);
        row("Ce que les amas attirent", () -> multiplier(game.accretionFactor()), () -> game.accretionFactor() > 1);

        section("Expansion de la matière");
        charted("Espace créé", row("Espace créé", () -> Format.count(game.state().space())));
        charted("Espace par seconde", row("Espace par seconde", () -> "+" + Format.amount(game.spacePerSecond())));
        charted("Espace utilisé", row("Occupé par les molécules", () -> Format.count(game.occupiedSpace()), () -> game.moleculesCreated() > 0));
        row("Réservé aux rassemblements", () -> Format.count(game.reservedSpace()), () -> game.substancesFormed() > 0);
        row("Espace libre", () -> Format.count(game.freeSpace()));

        section("D'où vient l'expansion");
        row("Un par Big Bang", () -> "+" + ElementText.number(Game.SPACE_PER_SECOND * game.bigBangs()) + " par seconde");
        row("Molécules, assemblages et astres", () -> multiplier(game.matterBoost(Molecule.Stat.SPACE)));
        row("Améliorations d'espace", () -> multiplier(game.spaceUpgradeBoost(Molecule.Stat.SPACE)));
        row("Paliers", () -> multiplier(game.bigBangMilestoneBoost(Molecule.Stat.SPACE)));
        row("Matière noire en réserve", () -> multiplier(game.darkMatterSpaceBoost()), this::darkSpace);

        section("Molécules");
        charted("Molécules créées", row("Molécules créées", () -> Format.whole(game.moleculesCreated())));
        charted("Créations de molécules", row("Créations", () -> Format.whole(stats().moleculeCreations()),
                () -> stats().moleculeCreations() > 0));
        row("dont automatiques", () -> Format.whole(stats().autoMoleculeCreations()), game::isMoleculeAutomationUnlocked);
        row("Molécules attirées par les amas", () -> Format.whole(stats().moleculesAttracted()),
                () -> stats().moleculesAttracted() > 0);
        row("La plus grosse création", () -> "×" + Format.whole(stats().biggestCreation()), () -> stats().biggestCreation() > 1);
        row("Le plus gros amas", this::biggestGathering, () -> game.substancesFormed() > 0);
        row("Exemplaires d'éléments employés", () -> Format.whole(stats().moleculeElementsSpent()),
                () -> stats().moleculeElementsSpent() > 0);
        row("Sortes de molécules créées", () -> {
            int sorts = 0;
            for (Molecule molecule : game.molecules()) {
                if (game.moleculeCount(molecule.id()) > 0) sorts++;
            }
            return sorts + " / " + game.molecules().size();
        });
        row("Rayons ouverts", () -> game.moleculeKindsUnlocked() + " / " + Molecule.Kind.values().length);
        charted("Sortes rassemblées", row("Sortes rassemblées", () -> Format.whole(game.substancesFormed()), game::isStatesUnlocked));
        for (Molecule.State state : Molecule.State.values()) {
            row(BigBangPage.placeTitle(state) + " rassemblés", () -> Format.whole(game.gatheredInState(state)),
                    () -> game.gatheredInState(state) > 0);
        }

        section("Assemblages et astres");
        charted("Assemblages et astres formés", row("Assemblages formés",
                () -> game.assembliesFormed() + " / " + game.assemblies().size(), game::isAssembliesUnlocked));
        row("Astres formés", () -> game.bodiesFormed() + " / " + game.bodies().size(), game::isBodiesUnlocked);
        for (Body.Tier tier : Body.Tier.values()) {
            row(tier.label(), () -> game.bodiesFormed(tier) + " / " + game.bodiesIn(tier),
                    () -> game.isBodiesUnlocked() && (game.bodiesFormed(tier) > 0 || tier == Body.Tier.RUBBLE));
        }
        row("Galaxie", () -> game.hasGalaxy() ? "formée" : game.canFormGalaxy() ? "prête à être formée" : "pas encore",
                game::isGalaxyUnlocked);
        for (Cosmos scale : new Cosmos[] {Cosmos.CLUSTER, Cosmos.UNIVERSE}) {
            row(scale.label(), () -> game.hasCosmos(scale) ? "formé" : game.canFormCosmos(scale) ? "prêt à être formé"
                    : game.cosmosConditionsMet(scale) + " / " + game.cosmosConditions(scale) + " conditions",
                    () -> game.isCosmosUnlocked(scale));
        }

        section("Ce que la matière multiplie");
        row("Particules", () -> multiplier(game.moleculeBoost(Molecule.Stat.PARTICLES)), this::boosted);
        row("Atomes par fusion", () -> multiplier(game.moleculeBoost(Molecule.Stat.ATOMS)), this::boosted);
        row("Espace par seconde", () -> multiplier(game.moleculeBoost(Molecule.Stat.SPACE)), this::boosted);
        row("Croissance de la matière noire", () -> multiplier(game.moleculeBoost(Molecule.Stat.DARK_GROWTH)), this::boosted);
        row("dont les améliorations d'espace", () -> "atomes " + multiplier(game.spaceUpgradeBoost(Molecule.Stat.ATOMS))
                + ", particules " + multiplier(game.spaceUpgradeBoost(Molecule.Stat.PARTICLES))
                + ", espace " + multiplier(game.spaceUpgradeBoost(Molecule.Stat.SPACE)), this::upgraded);
    }

    /** Vrai une fois atteint le palier qui fait dépendre l'expansion de la matière noire : il peut valoir ×1, réserve vide. */
    private boolean darkSpace() {
        for (BigBangMilestone milestone : game.bigBangMilestones()) {
            if (!game.isBigBangMilestoneReached(milestone)) continue;
            for (BigBangMilestone.Effect effect : milestone.effects()) {
                if (effect instanceof BigBangMilestone.DarkMatterSpace) return true;
            }
        }
        return false;
    }

    /** Quand une première du troisième acte a été atteinte : en temps de jeu, et comptée depuis le premier Big Bang. */
    private String reached(GameStats.Step step) {
        double at = stats().reachedAt(step);
        double first = stats().reachedAt(GameStats.Step.FIRST_BIG_BANG);
        String text = "après " + Format.duration(at) + " de jeu";
        if (step == GameStats.Step.FIRST_BIG_BANG || Double.isNaN(first) || at < first) return text;
        return text + " (" + Format.duration(at - first) + " après le premier Big Bang)";
    }

    /** La sorte rassemblée qui compte le plus de molécules : « Eau ×1 240 ». */
    private String biggestGathering() {
        Molecule biggest = null;
        int most = 0;
        for (Molecule molecule : game.molecules()) {
            int gathered = game.gatheredMolecules(molecule.id());
            if (gathered > most) {
                most = gathered;
                biggest = molecule;
            }
        }
        return biggest == null ? "aucun" : biggest.name() + " ×" + Format.whole(most);
    }

    /** La durée de chaque Big Bang gardée, numérotée depuis le premier du jeu. */
    private List<ChartPane.Series> bigBangSeries() {
        List<Double> times = stats().bigBangTimes();
        double[] x = new double[times.size()];
        double[] y = new double[times.size()];
        long first = stats().timedBigBangs() - times.size() + 1;
        for (int i = 0; i < x.length; i++) {
            x[i] = first + i;
            y[i] = Math.log10(Math.max(0.1, times.get(i)));
        }
        return List.of(new ChartPane.Series("Durée", ChartPane.YELLOW, x, y));
    }

    /** Vrai dès que le troisième acte multiplie quelque chose : une molécule créée, ou une amélioration d'espace qui multiplie. */
    private boolean boosted() {
        return game.moleculesCreated() > 0 || upgraded();
    }

    /** Vrai si une amélioration d'espace acquise multiplie une grandeur. */
    private boolean upgraded() {
        for (Molecule.Stat stat : Molecule.Stat.values()) {
            if (game.spaceUpgradeBoost(stat) > 1) return true;
        }
        return false;
    }

    private static String multiplier(double value) {
        return Format.multiplier(BigNum.of(value));
    }

    /** Une puissance de dix, écrite comme une quantité : 0 → « 1 », 3 → « 1000 », 12,3 → « 2.00e12 ». */
    private static String power(double exponent) {
        if ((exponent >= 6 || exponent < -2) && exponent == Math.rint(exponent)) return Format.powerOfTen((long) exponent);
        BigNum value = BigNum.pow10(exponent);
        return value.lt(BigNum.of(1000)) ? Format.amount(value) : Format.count(value);
    }

    /** Un délai d'automatisme, donné en puissance de dix de secondes : −1 → « 0.1 s », 2 → « 1 min 40 s ». */
    private static String delay(double exponent) {
        double seconds = Math.pow(10, exponent);
        return seconds < 60 ? AutomationPage.seconds(seconds) : Format.clock(seconds);
    }

    /** Une courbe d'un graphique qui suit le temps : son nom, sa couleur, et la valeur à lire dans chaque relevé. */
    private record Line(String name, String color, ToDoubleFunction<StatsHistory.Sample> value) {}

    /** L'historique affiché : celui de la partie en cours si le joueur l'a choisi, sinon celui du jeu entier. */
    private StatsHistory history() {
        return runOnly && exploded() ? stats().runHistory() : stats().history();
    }

    /** La durée de chaque explosion gardée, numérotée depuis la première du jeu. */
    private List<ChartPane.Series> explosionSeries() {
        List<Double> times = stats().explosionTimes();
        double[] x = new double[times.size()];
        double[] y = new double[times.size()];
        long first = stats().timedExplosions() - times.size() + 1;
        for (int i = 0; i < x.length; i++) {
            x[i] = first + i;
            y[i] = Math.log10(Math.max(0.1, times.get(i)));
        }
        return List.of(new ChartPane.Series("Durée", ChartPane.VIOLET, x, y));
    }

    // ------------------------------------------------------------------
    // Construction et affichage
    // ------------------------------------------------------------------

    private void page(String name, String color, BooleanSupplier unlocked, String howToUnlock) {
        SubPage page = new SubPage(name, color, unlocked, howToUnlock);
        page.content.setAlignment(Pos.TOP_CENTER);
        page.content.setPadding(new Insets(8, 0, 12, 0));
        page.scroll.setFitToWidth(true);
        page.scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        page.scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        page.tab.setOnAction(event -> {
            select(page);
            refresh();
        });
        pages.add(page);
        subTabs.getChildren().add(page.tab);
        stack.getChildren().add(page.scroll);
        building = page;
    }

    private void section(String title) {
        Label header = new Label(title);
        header.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-padding: 12 0 2 0; -fx-text-fill: " + building.color + ";");
        header.setMaxWidth(MAX_WIDTH);
        building.content.getChildren().add(header);
    }

    /**
     * Ouvre la section des courbes de la sous-page en construction : le menu déroulant, puis la
     * place du graphique choisi. Les courbes s'y ajoutent ensuite avec {@link #curve}, {@link #log}
     * et {@link #count}, dans l'ordre où le menu les proposera ; la première est celle affichée d'abord.
     */
    private void curves() {
        section("Évolution");
        SubPage page = building;
        // Le menu prend les couleurs du jeu : un fond sombre, et la couleur de la sous-page pour le choix en cours.
        page.choice.setStyle("-fx-font-size: 13px; -fx-base: #16202e; -fx-background: #16202e;"
                + " -fx-control-inner-background: #121821; -fx-control-inner-background-alt: #121821;"
                + " -fx-accent: " + page.color + "; -fx-focus-color: " + page.color + "; -fx-faint-focus-color: transparent;"
                + " -fx-selection-bar: " + page.color + "; -fx-selection-bar-non-focused: " + page.color + ";");
        page.choice.setPrefWidth(360);
        page.choice.setMaxWidth(MAX_WIDTH);
        page.choice.setVisibleRowCount(14);
        page.choice.setFocusTraversable(false);    // les flèches du clavier ne doivent pas changer de courbe par accident
        page.choice.setOnAction(event -> {
            if (updating) return;
            show(page, page.choice.getValue());
        });
        Label label = new Label("Courbe");
        label.setStyle(NAME_STYLE);
        page.chooser.getChildren().add(label);
        page.chooser.getChildren().add(page.choice);
        page.chooser.setAlignment(Pos.CENTER);
        page.holder.setMaxWidth(MAX_WIDTH);
        page.content.getChildren().add(page.chooser);
        page.content.getChildren().add(page.holder);
    }

    /** Affiche la courbe de ce nom sur une sous-page, si elle en a une ; le haut de la page, où elle est, revient à l'écran. */
    private void show(SubPage page, String name) {
        for (Graph graph : page.graphs) {
            if (!graph.name.equals(name)) continue;
            page.shown = graph;
            page.scroll.setVvalue(0);
            refresh();
            return;
        }
    }

    /**
     * Ajoute une courbe qui suit le temps : ses séries sont lues dans l'historique de la période choisie.
     *
     * @param name      son nom dans le menu déroulant
     * @param available vrai quand elle a quelque chose à montrer ; avant, le menu ne la propose pas
     * @param maker     comment construire son graphique, le jour où elle est choisie
     * @param lines     une ou deux séries, lues dans chaque relevé
     */
    private void curve(String name, BooleanSupplier available, Supplier<ChartPane> maker, Line... lines) {
        building.graphs.add(new Graph(name, available, maker, () -> {
            List<StatsHistory.Sample> samples = history().samples();
            List<ChartPane.Series> series = new ArrayList<>();
            for (Line line : lines) {
                double[] x = new double[samples.size()];
                double[] y = new double[samples.size()];
                for (int i = 0; i < x.length; i++) {
                    x[i] = samples.get(i).time();
                    y[i] = line.value().applyAsDouble(samples.get(i));
                }
                series.add(new ChartPane.Series(line.name(), line.color(), x, y));
            }
            return series;
        }, true));
    }

    /** Une courbe en échelle logarithmique, pour une quantité que l'historique note en puissance de dix. */
    private void log(String name, String unit, String color, StatsHistory.Stat stat, BooleanSupplier available) {
        curve(name, available,
                () -> new ChartPane(name, (unit.isEmpty() ? "" : unit + " · ") + "échelle logarithmique",
                        true, Format::clock, StatsPage::power, true, Double.NaN, Double.NaN),
                new Line(name, color, sample -> sample.value(stat)));
    }

    /** Une courbe pour un compteur : des entiers, à partir de zéro. */
    private void count(String name, String unit, String color, StatsHistory.Stat stat, BooleanSupplier available) {
        curve(name, available,
                () -> new ChartPane(name, unit, true, Format::clock, value -> Format.whole(Math.round(value)),
                        true, 0, Double.NaN),
                new Line(name, color, sample -> sample.value(stat)));
    }

    private Row row(String name, Supplier<String> text) {
        return row(name, text, () -> true);
    }

    private Row row(String name, Supplier<String> text, BooleanSupplier visible) {
        Label label = new Label(name);
        label.setStyle(NAME_STYLE);
        label.setWrapText(true);
        label.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(label, Priority.ALWAYS);
        Label value = new Label();
        value.setStyle(VALUE_STYLE);
        value.setWrapText(true);
        value.setTextAlignment(TextAlignment.RIGHT);
        HBox box = new HBox(12, label, value);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setStyle(ROW_STYLE);
        box.setMaxWidth(MAX_WIDTH);
        Row row = new Row(name, box, value, text, visible);
        building.rows.add(row);
        building.content.getChildren().add(box);
        return row;
    }

    /**
     * Relie une ligne à la courbe de sa statistique : un clic sur la ligne choisit cette courbe
     * dans le menu. La ligne s'éclaire sous la souris pour le faire savoir.
     */
    private void charted(String graph, Row row) {
        SubPage page = building;
        Node box = row.box();
        box.setStyle(ROW_STYLE + " -fx-cursor: hand;");
        box.setOnMouseEntered(event -> box.setStyle(ROW_HOVER_STYLE));
        box.setOnMouseExited(event -> box.setStyle(ROW_STYLE + " -fx-cursor: hand;"));
        box.setOnMouseClicked(event -> show(page, graph));
        Tooltip.install(box, new Tooltip("Voir la courbe : " + graph));
    }

    private void select(SubPage page) {
        selected = page;
        for (SubPage each : pages) {
            each.scroll.setVisible(each == page);
            each.tab.setStyle(SUB_TAB_STYLE + (each == page
                    ? " -fx-text-fill: " + each.color + "; -fx-border-color: transparent transparent " + each.color + " transparent;"
                    : " -fx-text-fill: #8fa3b8; -fx-border-color: transparent;"));
        }
    }

    private static String rangeStyle(boolean chosen) {
        return "-fx-font-size: 12px; -fx-padding: 4 12; -fx-cursor: hand; -fx-background-radius: 12; -fx-border-radius: 12;"
                + (chosen ? " -fx-text-fill: #0b0e14; -fx-background-color: " + STATS_COLOR + "; -fx-border-color: " + STATS_COLOR + ";"
                : " -fx-text-fill: #b9c7d6; -fx-background-color: #16202e; -fx-border-color: #3a4a5e;");
    }

    /**
     * Met à jour le menu déroulant d'une sous-page et trace la courbe choisie. Le menu ne propose
     * que les courbes dont la statistique existe déjà ; si celle qui était affichée disparaît
     * (après une remise à zéro), la première disponible la remplace.
     */
    private void refreshCurves(SubPage page) {
        List<String> names = new ArrayList<>();
        Graph first = null;
        boolean keep = false;
        for (Graph graph : page.graphs) {
            if (!graph.available.getAsBoolean()) continue;
            names.add(graph.name);
            if (first == null) first = graph;
            keep |= graph == page.shown;
        }
        if (!keep) page.shown = first;
        boolean any = page.shown != null;
        page.chooser.setVisible(any);
        page.chooser.setManaged(any);
        page.holder.setVisible(any);
        page.holder.setManaged(any);
        if (!any) return;

        // Remplir le menu ou y reporter le choix déclenche son action : ce n'est pas le joueur qui choisit.
        updating = true;
        if (!names.equals(page.offered)) {
            page.offered = names;
            page.choice.getItems().setAll(names);
        }
        if (!Objects.equals(page.choice.getValue(), page.shown.name)) page.choice.setValue(page.shown.name);
        updating = false;

        Graph graph = page.shown;
        if (graph.pane == null) graph.pane = graph.maker.get();
        if (page.holder.getChildren().size() != 1 || page.holder.getChildren().get(0) != graph.pane) {
            page.holder.getChildren().clear();
            page.holder.getChildren().add(graph.pane);
        }
        graph.pane.show(graph.series.get());
    }

    /** Affiche les sous-pages débloquées et met à jour la courbe et les lignes de celle qui est à l'écran. */
    void refresh() {
        current = game.stats();   // relève au passage le record de production
        SubPage nextLocked = null;
        for (SubPage page : pages) {
            boolean unlocked = page.unlocked.getAsBoolean();
            page.tab.setVisible(unlocked);
            page.tab.setManaged(unlocked);
            if (!unlocked && nextLocked == null) nextLocked = page;
        }
        // Une remise à zéro peut refermer la sous-page affichée.
        if (!selected.unlocked.getAsBoolean()) select(pages.get(0));

        refreshCurves(selected);
        // Le choix de la période n'a de sens qu'après une explosion, et pour une courbe qui suit le temps.
        boolean range = exploded() && selected.shown != null && selected.shown.timeline;
        rangeRow.setVisible(range);
        rangeRow.setManaged(range);
        wholeButton.setStyle(rangeStyle(!runOnly));
        runButton.setStyle(rangeStyle(runOnly));

        for (Row row : selected.rows) {
            boolean visible = row.visible().getAsBoolean();
            row.box().setVisible(visible);
            row.box().setManaged(visible);
            if (visible) row.value().setText(row.text().get());
        }
        for (Runnable updater : selected.updaters) updater.run();

        boolean more = nextLocked != null;
        nextLabel.setVisible(more);
        nextLabel.setManaged(more);
        if (more) {
            nextLabel.setText("D'autres statistiques apparaîtront avec votre avancée. Prochaines : « "
                    + nextLocked.tab.getText() + " », " + nextLocked.howToUnlock + ".");
        }
    }
}
