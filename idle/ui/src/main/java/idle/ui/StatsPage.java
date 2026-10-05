package idle.ui;

import idle.core.Automation;
import idle.core.BigNum;
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
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
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
 * <p>Les sous-pages des ressources commencent par des graphiques ({@link ChartPane}) : la
 * production, les atomes par fusion, l'avancement du tableau, la taille de la matière noire…
 * Ils sont tracés d'après l'historique que tient le jeu ({@link StatsHistory}). Après la première
 * explosion, deux boutons choisissent la période : tout le jeu, ou la partie en cours.
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
    private static final double MAX_WIDTH = 640;

    /** Graduations d'un axe de durées en échelle logarithmique : 1 s, 10 s, 1 min, 10 min, 1 h, 1 jour, 10 jours. */
    private static final double[] DURATION_TICKS = {0, 1, Math.log10(60), Math.log10(600), Math.log10(3_600),
            Math.log10(86_400), Math.log10(864_000)};

    /** Un graphique et la façon de calculer ses séries. */
    private record Chart(ChartPane pane, Supplier<List<ChartPane.Series>> series) {}

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
        final List<Chart> charts = new ArrayList<>();
        /** Vrai si un graphique de la sous-page suit le temps : le choix de la période la concerne. */
        boolean hasTimeline = false;

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
        section("Temps");
        row("Temps de jeu", () -> Format.duration(game.state().timePlayed()));
        row("Depuis la dernière explosion", () -> Format.duration(stats().runTime()), this::exploded);
        row("Depuis la dernière fusion", () -> Format.duration(game.state().timeSinceFusion()), this::fused);

        section("Avancée");
        row("Prochain objectif", this::nextGoal);
        row("Générateurs", () -> game.generatorCount() + " / " + game.maxGeneratorCount(), game::isStarted);
        row("Fusions", () -> Format.whole(stats().fusions()), this::fused);
        row("Éléments découverts", () -> game.discoveredElements() + " / " + PeriodicTable.ELEMENTS.size(), this::tableSeen);
        row("Explosions", () -> Format.whole(game.state().explosions()), this::exploded);

        section("Achats");
        row("Niveaux d'améliorations en particules", () -> Format.whole(stats().particleUpgradesBought()), game::isStarted);
        row("Niveaux d'améliorations en atomes", () -> Format.whole(stats().atomUpgradesBought()), this::fused);
        row("Actions faites par les automatismes",
                () -> Format.whole(stats().automationActions() + stats().darkAutomationActions()), this::automationSeen);
    }

    private void particles() {
        page("Particules", "#9fd0ff", game::isStarted, "en créant le premier générateur");
        section("Évolution");
        timeline(new ChartPane("Production de particules", "par seconde · échelle logarithmique",
                        true, Format::clock, StatsPage::power, true, Double.NaN, Double.NaN),
                new Line("Production", ChartPane.BLUE, StatsHistory.Sample::production));
        section("Quantités");
        row("Particules possédées", () -> Format.count(game.state().particles()));
        row("Créées depuis le début du jeu", () -> Format.count(stats().particlesCreated()));
        row("Créées depuis la dernière explosion", () -> Format.count(stats().runParticlesCreated()), this::exploded);
        row("Dépensées en améliorations", () -> Format.count(stats().particlesSpent()));

        section("Production");
        row("Production actuelle", () -> Format.amount(game.productionPerSecond()) + " par seconde");
        row("Par minute", () -> Format.perMinute(game.productionPerSecond()));
        row("Production avec tous les générateurs", () -> Format.amount(game.productionAtFusion()) + " par seconde");
        row("Record de production", () -> Format.amount(stats().bestProduction()) + " par seconde");

        section("D'où vient la production");
        row("Générateurs en activité", () -> game.generatorCount() + " / " + game.maxGeneratorCount());
        row("Créations par seconde et par générateur", () -> Format.amount(game.speed()));
        row("Particules par création", () -> Format.amount(game.particlesPerCreation()));
        row("Éléments : particules", () -> multiplier(game.elementMultiplier(ElementEffect.Stat.PARTICLES)), this::tableSeen);
        row("Éléments : vitesse", () -> multiplier(game.elementMultiplier(ElementEffect.Stat.SPEED)), this::tableSeen);
        row("Arbre de matière noire : particules", () -> Format.multiplier(game.darkParticlesMultiplier()), this::exploded);

        section("Améliorations");
        // Une ligne par amélioration payée en particules : en ajouter une dans core suffit à la faire apparaître.
        for (Upgrade upgrade : game.upgrades(Resource.PARTICLES)) {
            row(upgrade.name(), () -> "niveau " + game.levelOf(upgrade.id())
                    + (game.isMaxed(upgrade.id()) ? " (maximum)" : ", prochain : " + Format.count(game.costOf(upgrade.id()))));
        }
        row("Niveaux achetés depuis le début", () -> Format.whole(stats().particleUpgradesBought()));
        row("Générateurs achetés depuis le début", () -> Format.whole(stats().generatorsBought()));
    }

    private void atoms() {
        page("Atomes", "#ffd27f", this::fused, "à la première fusion");
        section("Évolution");
        timeline(new ChartPane("Atomes par fusion", "échelle logarithmique",
                        true, Format::clock, StatsPage::power, true, 0, Double.NaN),
                new Line("Atomes par fusion", ChartPane.YELLOW, StatsHistory.Sample::atomsPerFusion));
        timeline(new ChartPane("Durée d'une partie", "temps entre deux fusions · échelle logarithmique",
                        true, Format::clock, value -> Format.clock(Math.pow(10, value)), false, Double.NaN, Double.NaN)
                        .withTicks(DURATION_TICKS),
                new Line("Durée", ChartPane.YELLOW,
                        sample -> sample.fusionTime() > 0 ? Math.log10(Math.max(0.1, sample.fusionTime())) : Double.NaN));
        section("Quantités");
        row("Atomes disponibles", () -> Format.amount(game.state().atoms())
                + (game.isAtomCapLifted() ? "" : " / " + Format.count(Game.MAX_ATOMS)));
        row("Créés depuis la dernière explosion", () -> Format.amount(game.state().totalAtoms()));
        row("Créés depuis le début du jeu", () -> Format.amount(stats().atomsCreated()), this::exploded);
        row("Dépensés", () -> Format.amount(stats().atomsSpent()));

        section("Fusions");
        row("Fusions depuis le début du jeu", () -> Format.whole(stats().fusions()));
        row("Fusions depuis la dernière explosion", () -> Format.whole(stats().runFusions()), this::exploded);
        row("Durée de la dernière partie", () -> Format.duration(stats().lastFusionTime()));
        row("Partie la plus courte", () -> Format.duration(stats().fastestFusionTime()));
        row("Partie en cours", () -> Format.duration(game.state().timeSinceFusion()));

        section("Atomes par fusion");
        row("Actuellement", () -> Format.amount(game.atomsPerFusion()));
        row("Record", () -> Format.amount(stats().bestAtomsPerFusion()));
        row("Rendement de fusion", () -> multiplier(game.fusionYield()));
        row("Éléments", () -> multiplier(game.elementMultiplier(ElementEffect.Stat.ATOMS)), this::tableSeen);
        row("Arbre de matière noire", () -> "+" + ElementText.number(game.darkAtomsPerFusion()) + " atome de base", this::exploded);
        row("Améliorations en matière noire", () -> multiplier(game.darkAtomsMultiplier()), this::exploded);
        row("Groupes de générateurs fusionnés", () -> String.valueOf(game.fusionGroups()),
                () -> game.maxGeneratorCount() > game.generatorsPerAtom());

        section("Améliorations");
        // Une ligne par amélioration payée en atomes.
        for (Upgrade upgrade : game.upgrades(Resource.ATOMS)) {
            row(upgrade.name(), () -> (upgrade.maxLevel() == 1
                    ? (game.levelOf(upgrade.id()) > 0 ? "acquise" : "à acheter : " + Format.count(game.costOf(upgrade.id())))
                    : "niveau " + game.levelOf(upgrade.id())
                            + (game.isMaxed(upgrade.id()) ? " (maximum)" : ", prochain : " + Format.count(game.costOf(upgrade.id())))));
        }
        row("Niveaux achetés depuis le début", () -> Format.whole(stats().atomUpgradesBought()));
    }

    private void automation() {
        page("Automatisation", "#9be7a8", this::automationSeen,
                "à " + Format.count(Game.UNLOCK_TOTAL_ATOMS) + " atomes créés");
        section("Activité");
        row("Actions des automatismes ordinaires", () -> Format.whole(stats().automationActions()));
        row("Actions des automatismes de matière noire", () -> Format.whole(stats().darkAutomationActions()), this::exploded);
        row("Automatismes possédés", () -> game.state().ownedAutomations().size() + " / " + game.automations().size());
        row("Délai le plus court possible", () -> AutomationPage.seconds(game.minAutomationInterval()));
        row("Améliorations en matière noire", () -> "délais ÷" + ElementText.number(game.darkAutomationDivisor()), this::exploded);

        section("Chaque automatisme");
        for (Automation automation : game.automations()) {
            row(automation.name(), () -> !game.ownsAutomation(automation.id()) ? "pas encore acheté"
                    : "une action toutes les " + AutomationPage.seconds(game.automationInterval(automation.id()))
                            + ", cadence " + game.automationSpeedLevel(automation.id()) + "/" + automation.maxSpeedLevel()
                            + (game.isAutomationEnabled(automation.id()) ? "" : " (coupé)"),
                    () -> game.isAutomationAvailable(automation.id()) || game.ownsAutomation(automation.id()));
        }
    }

    private void periodicTable() {
        page("Tableau périodique", "#f0a8c0", this::tableSeen,
                "à " + Format.count(Game.UNLOCK_TOTAL_ATOMS) + " atomes créés");
        section("Évolution");
        timeline(new ChartPane("Avancement du tableau périodique", "en % du total",
                        true, Format::clock, value -> ElementText.number(value) + " %", false, 0, 100),
                new Line("Éléments découverts", ChartPane.BLUE, sample -> 100 * sample.elementsShare()),
                new Line("Exemplaires", ChartPane.ORANGE, sample -> 100 * sample.copiesShare()));
        section("Collection");
        row("Éléments découverts", () -> game.discoveredElements() + " / " + PeriodicTable.ELEMENTS.size());
        row("Exemplaires possédés", () -> game.ownedCopies() + " / " + game.maxTotalCopies());
        row("Élément unique obtenu", () -> game.isSynthesisAutomationUnlocked() ? "oui" : "pas encore");

        section("Synthèses");
        row("Depuis la dernière explosion", () -> Format.whole(game.state().synthesisCount()));
        row("Depuis le début du jeu", () -> Format.whole(stats().syntheses()), this::exploded);
        row("Exemplaires obtenus depuis le début", () -> Format.whole(stats().elementsObtained()));
        row("Tirages doubles réussis", () -> Format.whole(stats().doubleDraws()));
        row("Prix de la prochaine synthèse", () -> game.isPeriodicTableComplete() ? "tableau complet"
                : Format.count(game.synthesisCost()) + " atomes");
        row("Chance de tirage double", () -> ElementText.percent(Math.min(1, game.doubleDrawChance())));
        row("Éléments par synthèse", () -> String.valueOf(game.elementsPerSynthesis()), this::exploded);
        row("Élément unique garanti à la synthèse n°", () -> String.valueOf(game.guaranteedUniqueSynthesis()),
                () -> !game.isSynthesisAutomationUnlocked());

        section("Ce que rapportent les éléments");
        row("Particules", () -> multiplier(game.elementMultiplier(ElementEffect.Stat.PARTICLES)));
        row("Vitesse", () -> multiplier(game.elementMultiplier(ElementEffect.Stat.SPEED)));
        row("Atomes par fusion", () -> multiplier(game.elementMultiplier(ElementEffect.Stat.ATOMS)));
        row("Prix de la vitesse", () -> "÷" + ElementText.number(game.elementCostDivisor(ElementEffect.CostTarget.SPEED_UPGRADES)));
        row("Prix des améliorations en atomes",
                () -> "÷" + ElementText.number(game.elementCostDivisor(ElementEffect.CostTarget.ATOM_UPGRADES)));
        row("Prix de la synthèse", () -> "÷" + ElementText.number(game.elementCostDivisor(ElementEffect.CostTarget.SYNTHESIS)));

        section("Par famille : éléments découverts, exemplaires");
        for (ElementCategory category : ElementCategory.values()) {
            List<Element> members = PeriodicTable.elements(category);
            row(category.label(), () -> {
                int discovered = 0, copies = 0;
                for (Element element : members) {
                    int count = game.elementCount(element.number());
                    if (count > 0) discovered++;
                    copies += count;
                }
                return discovered + " / " + members.size() + ", " + copies + " / "
                        + members.size() * game.maxCopiesOf(category) + " ex.   (chance "
                        + ElementText.percent(game.categoryChance(category)) + ")";
            });
        }
    }

    private void darkMatter() {
        page("Matière noire", GameApp.DARK_MATTER_COLOR, this::exploded, "à la première explosion");
        section("Évolution");
        timeline(new ChartPane("Taille de la matière noire", "échelle logarithmique",
                        true, Format::clock, value -> Format.length(BigNum.pow10(value)), true, Double.NaN, Double.NaN),
                new Line("Taille", ChartPane.VIOLET, StatsHistory.Sample::darkMatterSize));
        // Une abscisse par explosion, et non par instant : ce graphique ne dépend pas de la période choisie.
        chart(new ChartPane("Durée de chaque explosion", "temps de jeu entre deux explosions · échelle logarithmique",
                        false, value -> "n° " + Math.round(value), value -> Format.clock(Math.pow(10, value)),
                        false, Double.NaN, Double.NaN).withTicks(DURATION_TICKS).withMarkers(),
                this::explosionSeries);
        section("Quantités");
        row("Matière noire disponible", () -> Format.count(game.state().darkMatter()));
        row("Gagnée depuis le début", () -> Format.count(game.darkMatterEarned()));
        row("Dépensée en améliorations", () -> Format.count(game.state().darkMatterSpent()));
        row("Par explosion", () -> "+" + Format.count(game.darkMatterPerExplosion()));

        section("Explosions");
        row("Explosions", () -> Format.whole(game.state().explosions()));
        row("Durée de la dernière", () -> Format.duration(stats().lastExplosionTime()));
        row("La plus rapide", () -> Format.duration(stats().fastestExplosionTime()));
        row("Partie en cours", () -> Format.duration(stats().runTime()));

        section("Taille");
        row("Taille actuelle", () -> Format.length(game.state().darkMatterSize()));
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
        row("Temps d'appui", () -> Format.duration(stats().holdTime()));

        section("Croissance");
        row("Par seconde d'appui", () -> {
            BigNum growth = game.darkMatterGrowthPerSecond();
            return growth.lt(BigNum.of(2)) ? "+" + ElementText.percent(growth.toDouble() - 1) : Format.multiplier(growth);
        });
        row("Avancement de la partie", () -> multiplier(game.darkMatterProgressFactor()));
        row("Arbre : élan", () -> Format.multiplier(game.darkExpansionMultiplier()));
        row("Résistance", () -> "÷" + Format.amount(game.darkMatterResistance()));
        row("Croissance spontanée", () -> ElementText.percent(game.darkAutoExpansionShare()) + " de la vitesse d'appui",
                () -> game.darkAutoExpansionShare() > 0);

        section("Améliorations");
        row("Cases de l'arbre acquises", () -> owned(true) + " / " + count(true));
        row("Améliorations en matière noire acquises", () -> owned(false) + " / " + count(false));
        row("Atomes comptés au départ de chaque partie", () -> Format.count(game.darkHeadStartAtoms()),
                () -> game.darkHeadStartAtoms().sign() > 0);
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
        if (!game.isStarted()) return "créer le premier générateur";
        if (game.canExplode()) return "faire exploser le tableau périodique";
        if (game.isPeriodicTableUnlocked()) {
            if (!game.isSynthesisAutomationUnlocked()) return "obtenir un élément unique ★ pour automatiser la synthèse";
            if (game.discoveredElements() < PeriodicTable.ELEMENTS.size()) {
                return "découvrir les " + PeriodicTable.ELEMENTS.size() + " éléments (" + game.discoveredElements() + " pour l'instant)";
            }
            return "porter chaque élément à son maximum (" + game.ownedCopies() + " / " + game.maxTotalCopies() + " exemplaires)";
        }
        if (game.state().totalAtoms().sign() > 0) {
            return "créer " + Format.count(Game.UNLOCK_TOTAL_ATOMS) + " atomes pour ouvrir l'automatisation et le tableau périodique ("
                    + Format.count(game.state().totalAtoms()) + " pour l'instant)";
        }
        if (!game.hasAllGenerators()) return "débloquer les " + game.generatorsPerAtom() + " générateurs";
        return "fusionner les générateurs en un premier atome";
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

    /** Ajoute un graphique qui suit le temps : ses courbes sont lues dans l'historique de la période choisie. */
    private void timeline(ChartPane pane, Line... lines) {
        building.hasTimeline = true;
        chart(pane, () -> {
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
        });
    }

    private void chart(ChartPane pane, Supplier<List<ChartPane.Series>> series) {
        building.charts.add(new Chart(pane, series));
        building.content.getChildren().add(pane);
    }

    private void row(String name, Supplier<String> text) {
        row(name, text, () -> true);
    }

    private void row(String name, Supplier<String> text, BooleanSupplier visible) {
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
        building.rows.add(new Row(name, box, value, text, visible));
        building.content.getChildren().add(box);
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

    /** Affiche les sous-pages débloquées et met à jour les graphiques et les lignes de celle qui est à l'écran. */
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

        // Le choix de la période n'a de sens qu'après une explosion, et sur une sous-page qui a des courbes.
        boolean range = exploded() && selected.hasTimeline;
        rangeRow.setVisible(range);
        rangeRow.setManaged(range);
        wholeButton.setStyle(rangeStyle(!runOnly));
        runButton.setStyle(rangeStyle(runOnly));
        for (Chart chart : selected.charts) chart.pane().show(chart.series().get());

        for (Row row : selected.rows) {
            boolean visible = row.visible().getAsBoolean();
            row.box().setVisible(visible);
            row.box().setManaged(visible);
            if (visible) row.value().setText(row.text().get());
        }

        boolean more = nextLocked != null;
        nextLabel.setVisible(more);
        nextLabel.setManaged(more);
        if (more) {
            nextLabel.setText("D'autres statistiques apparaîtront avec votre avancée. Prochaines : « "
                    + nextLocked.tab.getText() + " », " + nextLocked.howToUnlock + ".");
        }
    }
}
