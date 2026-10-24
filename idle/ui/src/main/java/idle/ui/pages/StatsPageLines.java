package idle.ui.pages;

import idle.core.Game;
import idle.core.GameStats;
import idle.core.StatsHistory;
import idle.core.bigbang.BangChallenge;
import idle.core.bigbang.BigBangMilestone;
import idle.core.bigbang.SpaceUpgrade;
import idle.core.darkmatter.DarkUpgrade;
import idle.core.math.BigNum;
import idle.core.molecules.Molecule;
import idle.core.sky.Body;
import idle.core.sky.Cosmos;
import idle.ui.GameApp;
import idle.ui.Goals;
import idle.ui.automation.AutomationPage;
import idle.ui.bigbang.BigBangPage;
import idle.ui.common.ElementText;
import idle.ui.common.Format;
import idle.ui.vacuum.VoidPage;
import java.util.ArrayList;
import java.util.List;

/**
 * La page des statistiques : les courbes principales et ce que les lignes ont besoin de savoir.
 */
public abstract class StatsPageLines extends StatsPageBase {

    StatsPageLines(Game game) {
        super(game);
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
    void resources() {
        building.graphs.add(new Graph("Toutes les ressources", game::isStarted,
                () -> new ChartPane("Toutes les ressources", "ce que vous avez en main, matière noire dépensée comprise · échelle "
                        + "logarithmique double", true, Format::clock, StatsPage::doubleLog,
                        false, 0, Double.NaN).withTicks(DOUBLE_LOG_TICKS).withHeight(340),
                () -> {
                    List<StatsHistory.Sample> samples = samples();
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
            new Held("Molécules", ChartPane.AQUA, StatsHistory.Stat.MOLECULES, true),
            new Held("Énergie du collisionneur", ChartPane.GREEN, StatsHistory.Stat.COLLIDER_ENERGY, false));

    /** Graduations de l'échelle logarithmique double : 1, 10, 1000, 1e10, 1e30, 1e100, 1e300, 1e1000… */
    private static final double[] DOUBLE_LOG_TICKS = doubleLogTicks(0, 1, 3, 10, 30, 100, 300, 1_000, 3_000, 10_000, 100_000, 1_000_000);

    private static double[] doubleLogTicks(double... exponents) {
        double[] ticks = new double[exponents.length];
        for (int i = 0; i < ticks.length; i++) ticks[i] = Math.log10(1 + exponents[i]);
        return ticks;
    }

    /** Une hauteur de l'échelle logarithmique double, écrite comme la quantité qu'elle représente : 0 → « 1 », 2 → « 1e99 ». */
    static String doubleLog(double height) {
        double exponent = Math.pow(10, height) - 1;
        if (Math.abs(exponent - Math.rint(exponent)) < 1e-6) exponent = Math.rint(exponent);
        return power(exponent);
    }

    void production() {
        curve("Production de particules", game::isStarted,
                () -> new ChartPane("Production de particules", "par seconde · échelle logarithmique",
                        true, Format::clock, StatsPage::power, true, Double.NaN, Double.NaN),
                new Line("Production", ChartPane.BLUE, StatsHistory.Sample::production));
    }

    void atomsPerFusion() {
        curve("Atomes par fusion", this::fused,
                () -> new ChartPane("Atomes par fusion", "échelle logarithmique",
                        true, Format::clock, StatsPage::power, true, 0, Double.NaN),
                new Line("Atomes par fusion", ChartPane.YELLOW, StatsHistory.Sample::atomsPerFusion));
    }

    void tableProgress() {
        curve("Avancement du tableau périodique", this::tableSeen,
                () -> new ChartPane("Avancement du tableau périodique", "en % du total",
                        true, Format::clock, value -> ElementText.number(value) + " %", false, 0, 100),
                new Line("Éléments découverts", ChartPane.BLUE, sample -> 100 * sample.elementsShare()),
                new Line("Exemplaires", ChartPane.ORANGE, sample -> 100 * sample.copiesShare()));
    }

    void darkMatterSize() {
        curve("Taille de la matière noire", this::exploded,
                () -> new ChartPane("Taille de la matière noire", "échelle logarithmique",
                        true, Format::clock, value -> Format.length(BigNum.pow10(value)), true, Double.NaN, Double.NaN),
                new Line("Taille", ChartPane.VIOLET, StatsHistory.Sample::darkMatterSize));
    }

    /** L'énergie produite au collisionneur depuis le début : sa courbe principale, aussi dans la vue d'ensemble. */
    void colliderEnergy() {
        log("Énergie produite au collisionneur", "depuis le début", ChartPane.GREEN, StatsHistory.Stat.COLLIDER_ENERGY_PRODUCED,
                game::isColliderUnlocked);
    }

    void achievementCount() {
        count("Succès obtenus", "sur " + game.achievements().size(), ChartPane.TEAL, StatsHistory.Stat.ACHIEVEMENTS,
                () -> !game.achievements().isEmpty());
    }

    // ------------------------------------------------------------------
    // Ce que les lignes ont besoin de savoir
    // ------------------------------------------------------------------

    /** Les compteurs, relevés une fois par mise à jour de la page ({@link #refresh()}). */
    GameStats stats() {
        if (current == null) current = game.stats();
        return current;
    }

    boolean fused() {
        return stats().fusions() > 0 || game.state().totalAtoms().sign() > 0;
    }

    boolean exploded() {
        return game.isDarkMatterUnlocked();
    }

    boolean tableSeen() {
        return game.isPeriodicTableUnlocked() || stats().syntheses() > 0 || game.discoveredElements() > 0;
    }

    boolean automationSeen() {
        return AutomationPage.hasContent(game) || stats().automationActions() > 0 || stats().darkAutomationActions() > 0;
    }

    /** Nombre d'améliorations de matière noire possédées (au moins un niveau), dans l'arbre ou hors de l'arbre. */
    int owned(boolean inTree) {
        int owned = 0;
        for (DarkUpgrade upgrade : game.darkUpgrades()) {
            if (upgrade.inTree() == inTree && game.darkLevelOf(upgrade.id()) > 0) owned++;
        }
        return owned;
    }

    int count(boolean inTree) {
        int count = 0;
        java.util.Set<String> forks = new java.util.HashSet<>();
        for (DarkUpgrade upgrade : game.darkUpgrades()) {
            // Une bifurcation ne compte qu'une case : on n'en prend qu'une voie (version 0.069), sauf avec la Superposition (0.070).
            if (upgrade.inTree() == inTree && (!upgrade.isFork() || game.hasSuperposition() || forks.add(upgrade.fork()))) count++;
        }
        return count;
    }

    /** Le prochain grand pas de la partie en cours, en une phrase. */
    String nextGoal() {
        return Goals.next(game);
    }

    /** Le troisième acte : l'espace, les molécules, ce qu'elles forment, et ce que tout cela multiplie. */
    void bigBang() {
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
        row("Réserve d'espace de la création automatique", () -> game.autoMoleculeReserve() <= 0 ? "aucune"
                : ElementText.percent(game.autoMoleculeReserve()), game::isMoleculeAutomationUnlocked);
        row("Rassemblement automatique", () -> game.isAutoGatherEnabled() ? "en marche" : "coupé", game::isAutoGatherUnlocked);
        row("Formation automatique", () -> game.isAutoFormEnabled() ? "en marche" : "coupée", game::isAutoFormUnlocked);
        charted("Expansion multipliée par la matière noire", row("Expansion multipliée par la matière noire",
                () -> multiplier(game.darkMatterSpaceBoost()), () -> game.darkMatterSpaceBoost() > 1));
        // Le vide (version 0.132), une fois ouvert : sa réserve, ses améliorations et ce qu'il retire au troisième acte.
        row("Énergie du vide", () -> Format.count(game.voidEnergy()) + " (équilibre : "
                + Format.count(BigNum.of(game.voidEquilibrium())) + ")", game::isVoidUnlocked);
        row("Niveaux d'amélioration du vide", () -> Format.whole(VoidPage.levelsOwned(game)), game::isVoidUnlocked);
        row("Ce que le vide raccourcit", () -> VoidPage.actThreeCuts(game), game::isVoidUnlocked);
        row("Masse du trou noir", () -> ElementText.number(game.voidMass()), () -> game.isVoidUnlocked() && game.isVoidHoleOpen());

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

        section("Collections, comètes et défis");
        row("Collections à moitié réunies", () -> game.collectionsAt(1) + " / " + game.collections().size());
        row("Collections complètes", () -> game.collectionsAt(2) + " / " + game.collections().size());
        row("Comètes saisies", () -> Format.whole(game.cometsCaught()));
        row("Sillage de comète", () -> "espace " + multiplier(game.cometBoost()) + ", encore " + Format.wait(game.cometBoostLeft()),
                () -> game.cometBoost() > 1);
        row("Défis de Big Bang réussis", () -> game.completedBangChallenges() + " / " + game.bangChallenges().size(),
                game::isBangChallengesUnlocked);
        row("Défi de Big Bang en cours", () -> game.activeBangChallenge().label() + ", depuis " + Format.duration(game.bangChallengeTime()),
                () -> game.activeBangChallenge() != null);

        section("Ce que donnent les paliers");
        row("Matière noire des explosions", () -> multiplier(game.bigBangDarkMatterFactor()), () -> game.bigBangDarkMatterFactor() > 1);
        row("Univers formés : matière noire et sa croissance", () -> multiplier(game.universeDarkMatterFactor()), () -> game.universeDarkMatterFactor() > 1);
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
        row("Collections", () -> multiplier(game.collectionBoost(Molecule.Stat.SPACE)), () -> game.collectionBoost(Molecule.Stat.SPACE) > 1);
        row("Défis de Big Bang", () -> multiplier(game.bangReward(BangChallenge.Reward.SPACE)),
                () -> game.bangReward(BangChallenge.Reward.SPACE) > 1);

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

    static String multiplier(double value) {
        return Format.multiplier(BigNum.of(value));
    }

    /** Une puissance de dix, écrite comme une quantité : 0 → « 1 », 3 → « 1000 », 12,3 → « 2.00e12 ». */
    static String power(double exponent) {
        if ((exponent >= 6 || exponent < -2) && exponent == Math.rint(exponent)) return Format.powerOfTen((long) exponent);
        BigNum value = BigNum.pow10(exponent);
        return value.lt(BigNum.of(1000)) ? Format.amount(value) : Format.count(value);
    }

    /** Un délai d'automatisme, donné en puissance de dix de secondes : −1 → « 0.1 s », 2 → « 1 min 40 s ». */
    static String delay(double exponent) {
        double seconds = Math.pow(10, exponent);
        return seconds < 60 ? AutomationPage.seconds(seconds) : Format.clock(seconds);
    }

    /** L'historique affiché : celui de la partie en cours si le joueur l'a choisi, sinon celui du jeu entier. */
    private StatsHistory history() {
        return runOnly && exploded() ? stats().runHistory() : stats().history();
    }

    /** Les relevés de l'historique choisi, et celui de l'instant : recalculés au plus une fois par seconde de jeu. */
    List<StatsHistory.Sample> samples() {
        boolean run = runOnly && exploded();
        double now = game.state().timePlayed();
        if (liveSamples == null || liveRun != run || Math.abs(now - liveAt) >= 1 || now < liveAt) {
            liveSamples = game.samplesWithNow(run);
            liveRun = run;
            liveAt = now;
        }
        return liveSamples;
    }

    /** La durée de chaque explosion gardée, numérotée depuis la première du jeu. */
    List<ChartPane.Series> explosionSeries() {
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
}
