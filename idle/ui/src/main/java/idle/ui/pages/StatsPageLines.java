package idle.ui.pages;

import idle.core.Game;
import idle.core.GameStats;
import idle.core.StatsHistory;
import idle.core.bigbang.BangChallenge;
import idle.core.bigbang.BigBangMilestone;
import idle.core.bigbang.SpaceUpgrade;
import idle.core.collider.GluonMilestone;
import idle.core.collider.Hadron;
import idle.core.darkmatter.DarkUpgrade;
import idle.core.math.BigNum;
import idle.core.molecules.Molecule;
import idle.core.sky.Body;
import idle.core.sky.Cosmos;
import idle.core.universe.DimensionAutomation;
import idle.ui.GameApp;
import idle.ui.Goals;
import idle.ui.automation.AutomationPage;
import idle.ui.bigbang.BigBangPage;
import idle.ui.common.ElementText;
import idle.ui.common.Format;
import idle.ui.universe.UniversePage;
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
            new Held("Énergie du collisionneur", ChartPane.GREEN, StatsHistory.Stat.COLLIDER_ENERGY, false),
            new Held("Énergie du vide", ChartPane.TEAL, StatsHistory.Stat.VOID_ENERGY, false));

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
                        true, Format::clock, Format::multiplier, false, 1, Double.NaN),
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
                () -> Format.multiplier(game.darkMatterSpaceBoost()), () -> game.darkMatterSpaceBoost() > 1));

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
        row("Sillage de comète", () -> "espace " + Format.multiplier(game.cometBoost()) + ", encore " + Format.wait(game.cometBoostLeft()),
                () -> game.cometBoost() > 1);
        row("Défis de Big Bang réussis", () -> game.completedBangChallenges() + " / " + game.bangChallenges().size(),
                game::isBangChallengesUnlocked);
        row("Défi de Big Bang en cours", () -> game.activeBangChallenge().label() + ", depuis " + Format.duration(game.bangChallengeTime()),
                () -> game.activeBangChallenge() != null);

        section("Ce que donnent les paliers");
        row("Matière noire des explosions", () -> Format.multiplier(game.bigBangDarkMatterFactor()), () -> game.bigBangDarkMatterFactor() > 1);
        row("Univers formés : matière noire et sa croissance", () -> Format.multiplier(game.universeDarkMatterFactor()), () -> game.universeDarkMatterFactor() > 1);
        row("Espace par seconde", () -> Format.multiplier(game.bigBangMilestoneBoost(Molecule.Stat.SPACE)),
                () -> game.bigBangMilestoneBoost(Molecule.Stat.SPACE) > 1);
        row("Ce que les amas attirent", () -> Format.multiplier(game.accretionFactor()), () -> game.accretionFactor() > 1);

        section("Expansion de la matière");
        charted("Espace créé", row("Espace créé", () -> Format.count(game.state().space())));
        charted("Espace par seconde", row("Espace par seconde", () -> "+" + Format.amount(game.spacePerSecond())));
        charted("Espace utilisé", row("Occupé par les molécules", () -> Format.count(game.occupiedSpace()), () -> game.moleculesCreated() > 0));
        row("Réservé aux rassemblements", () -> Format.count(game.reservedSpace()), () -> game.substancesFormed() > 0);
        row("Espace libre", () -> Format.count(game.freeSpace()));

        section("D'où vient l'expansion");
        row("Un par Big Bang", () -> "+" + ElementText.number(Game.SPACE_PER_SECOND * game.bigBangs()) + " par seconde");
        row("Molécules, assemblages et astres", () -> Format.multiplier(game.matterBoost(Molecule.Stat.SPACE)));
        row("Améliorations d'espace", () -> Format.multiplier(game.spaceUpgradeBoost(Molecule.Stat.SPACE)));
        row("Paliers", () -> Format.multiplier(game.bigBangMilestoneBoost(Molecule.Stat.SPACE)));
        row("Matière noire en réserve", () -> Format.multiplier(game.darkMatterSpaceBoost()), this::darkSpace);
        row("Collections", () -> Format.multiplier(game.collectionBoost(Molecule.Stat.SPACE)), () -> game.collectionBoost(Molecule.Stat.SPACE) > 1);
        row("Défis de Big Bang", () -> Format.multiplier(game.bangReward(BangChallenge.Reward.SPACE)),
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
        row("Particules", () -> Format.multiplier(game.moleculeBoost(Molecule.Stat.PARTICLES)), this::boosted);
        row("Atomes par fusion", () -> Format.multiplier(game.moleculeBoost(Molecule.Stat.ATOMS)), this::boosted);
        row("Espace par seconde", () -> Format.multiplier(game.moleculeBoost(Molecule.Stat.SPACE)), this::boosted);
        row("Croissance de la matière noire", () -> Format.multiplier(game.moleculeBoost(Molecule.Stat.DARK_GROWTH)), this::boosted);
        row("dont les améliorations d'espace", () -> "atomes " + Format.multiplier(game.spaceUpgradeBoost(Molecule.Stat.ATOMS))
                + ", particules " + Format.multiplier(game.spaceUpgradeBoost(Molecule.Stat.PARTICLES))
                + ", espace " + Format.multiplier(game.spaceUpgradeBoost(Molecule.Stat.SPACE)), this::upgraded);
    }

    /**
     * Les univers (version 0.144) : ce que les univers suivants ajoutent, et qui n'avait pas de statistiques — les
     * univers formés et la graine, les dimensions et leurs éclats (la boucle et le rond de croissance depuis la version
     * 0.153, la brane et la lumière astrale depuis la version 0.154, le clicker et la météo depuis la version 0.155, la grille
     * et la graine profonde depuis la version 0.156, le miroir et les saisons depuis la version 0.157, le mélange depuis la version
     * 0.158, le défi final et la fin de la partie depuis la version 0.160), le vide, les bulles de l'arbre, les hadrons (version 0.149) et leurs gluons (version 0.151), et les règles du
     * deuxième univers (régime, cycle, épreuves, skins, recherches).
     */
    void universe() {
        page("Univers", UniversePage.COLOR, () -> game.universes() > 0, "au premier univers formé");
        curves();
        count("Univers formés", "depuis le début du jeu", ChartPane.YELLOW, StatsHistory.Stat.UNIVERSES, () -> true);
        count("Arrosages de la graine", "reçus par la graine d'univers", ChartPane.GREEN, StatsHistory.Stat.SEED_WATERINGS,
                () -> game.state().seedWaterings() > 0);
        log("Segments de la ligne", "gagnés en tout", ChartPane.BLUE, StatsHistory.Stat.LINE_SHARDS, game::isLineOpen);
        log("Facettes du plan", "gagnées en tout", ChartPane.MAGENTA, StatsHistory.Stat.PLANE_SHARDS, game::isPlaneOpen);
        log("Sphères du volume", "gagnées en tout", ChartPane.AQUA, StatsHistory.Stat.VOLUME_SHARDS, game::isVolumeOpen);
        log("Instants de la durée", "gagnés en tout", ChartPane.ORANGE, StatsHistory.Stat.DURATION_SHARDS, game::isDurationOpen);
        log("Nœuds de la boucle", "gagnés en tout", ChartPane.VIOLET, StatsHistory.Stat.LOOP_SHARDS, game::isLoopOpen);
        log("Ondes de la brane", "gagnées en tout", ChartPane.TEAL, StatsHistory.Stat.BRANE_SHARDS, game::isBraneOpen);
        log("Étincelles du clicker", "gagnées en tout", ChartPane.ORANGE, StatsHistory.Stat.CLICKER_SHARDS, game::isClickerOpen);
        log("Repères de la grille", "gagnés en tout", ChartPane.BLUE, StatsHistory.Stat.GRID_SHARDS, game::isGridOpen);
        log("Reflets du miroir", "gagnés en tout", ChartPane.MAGENTA, StatsHistory.Stat.MIRROR_SHARDS, game::isMirrorOpen);
        log("Fragments du mélange", "gagnés en tout", ChartPane.YELLOW, StatsHistory.Stat.MIX_SHARDS, game::isMixOpen);
        log("Énergie du vide", "en réserve", ChartPane.TEAL, StatsHistory.Stat.VOID_ENERGY, game::isVoidUnlocked);
        log("Énergie du vide par seconde", "ce que donnent les paires", ChartPane.VIOLET, StatsHistory.Stat.VOID_ENERGY_RATE, game::isVoidUnlocked);

        section("Univers et graine");
        charted("Univers formés", row("Univers formés", () -> Format.whole(game.universes())));
        row("Univers en cours", () -> "le " + (game.universes() + 1) + (game.isReplaying() ? "ᵉ, rejoué depuis le début" : "ᵉ"),
                () -> !game.isUniverseFormed());
        row("Prochain univers", () -> game.canOpenNextUniverse() ? "prêt à s'ouvrir"
                : "au palier " + game.nextUniverseStage() + " de la graine", game::isUniverseFormed);
        row("Palier de la graine", () -> game.seedStage() + " / " + Game.SEED_STAGES, () -> game.state().seedSprouted());
        charted("Arrosages de la graine", row("Arrosages reçus", () -> Format.whole(game.state().seedWaterings()),
                () -> game.state().seedWaterings() > 0));
        row("Arrosoir", () -> game.isWatering() ? "tenu, " + ElementText.number(game.wateringCanRate()) + " arrosages par seconde"
                : ElementText.number(game.wateringCanRate()) + " arrosages par seconde quand il est tenu", game::isWateringCanUnlocked);
        row("Tuyau d'arrosage", () -> ElementText.number(game.hoseRate()) + " arrosages par seconde, tout seul", game::hasHose);
        row("Chrono", () -> (game.isChronoRunning() ? "en marche, ×" + ElementText.number(game.chronoSpeed()) + ", " : "")
                + Format.duration(game.chronoStored()) + " en réserve sur " + Format.duration(game.chronoCapacity()), game::isChronoUnlocked);
        row("Fusions depuis l'univers", () -> Format.whole(game.fusionsSinceUniverse()), () -> game.universes() > 0);

        section("Dimensions");
        row("Dimensions ouvertes", () -> game.dimensionsUnlocked() + " / " + Game.MAX_DIMENSIONS, game::isDimensionsUnlocked);
        dimension(1, "Ligne", Game.LINE_THRESHOLD, game::isLineOpen, () -> game.state().dimensionShardsEarned(), "Segments de la ligne");
        row("Segments par seconde", () -> ElementText.number(game.lineShardsPerSecond()), game::isLineOpen);
        dimension(2, "Plan", Game.PLANE_THRESHOLD, game::isPlaneOpen, () -> game.state().planeShardsEarned(), "Facettes du plan");
        row("Facettes ramassées", () -> Format.whole(game.planeCollected()), game::isPlaneOpen);
        dimension(3, "Volume", Game.VOLUME_THRESHOLD, game::isVolumeOpen, () -> game.state().volumeShardsEarned(), "Sphères du volume");
        row("Plongées dans le volume", () -> Format.whole(game.volumeDives()) + ", " + Format.whole(game.volumeCollected()) + " sphères touchées",
                game::isVolumeOpen);
        dimension(4, "Durée", Game.DURATION_THRESHOLD, game::isDurationOpen, () -> game.state().durationShardsEarned(), "Instants de la durée");
        row("Lâchers de la durée", () -> Format.whole(game.durationReleases()), game::isDurationOpen);
        dimension(5, "Boucle", Game.LOOP_THRESHOLD, game::isLoopOpen, () -> game.state().loopShardsEarned(), "Nœuds de la boucle");
        row("Cercles de la boucle", () -> game.loopRings() + " / " + Game.MAX_RINGS + ", " + game.loopZoneCount()
                + (game.loopZoneCount() > 1 ? " zones" : " zone") + " chacun, un tour en "
                + ElementText.number(1 / game.loopTurnsPerSecond()) + " s", game::isLoopOpen);
        row("Clics justes de la boucle", () -> Format.whole(game.loopHits()) + " depuis le lancement", game::isLoopOpen);
        row("Rond de croissance", () -> (game.bloomActiveLeft() > 0
                ? "en cours, arrosage +" + Math.round(game.bloomGain() * 100) + " %, encore " + Format.wait(game.bloomActiveLeft())
                : game.isBloomVisible() ? "à cueillir, encore " + Format.wait(game.bloomVisibleLeft())
                : "+" + Math.round(game.bloomGain() * 100) + " % pendant " + ElementText.number(game.bloomSeconds()) + " s")
                + ", " + Format.whole(game.bloomsCaught()) + " cueilli" + (game.bloomsCaught() > 1 ? "s" : "") + " depuis le lancement",
                game::isBloomUnlocked);
        dimension(6, "Brane", Game.BRANE_THRESHOLD, game::isBraneOpen, () -> game.state().braneShardsEarned(), "Ondes de la brane");
        row("Ondes tirées sur la brane", () -> Format.whole(game.braneWavesSent()) + " depuis le lancement, "
                + game.braneWaves().size() + " en cours", game::isBraneOpen);
        row("Points touchés de la brane", () -> Format.whole(game.braneTouches()) + " passages, dont "
                + Format.whole(game.braneBounces()) + " après un rebond", game::isBraneOpen);
        row("Rebonds en un passage", () -> "au plus " + game.braneMostBounces() + " depuis le lancement", game::isBraneOpen);
        row("Lumière astrale", () -> "arrosage par seconde " + Format.multiplier(game.astralLight()),
                () -> game.seedUpgradeLevel(Game.SeedUpgrade.ASTRAL_LIGHT) > 0);
        dimension(7, "Clicker", Game.CLICKER_THRESHOLD, game::isClickerOpen, () -> game.state().clickerShardsEarned(), "Étincelles du clicker");
        row("Clics du clicker", () -> Format.whole(game.clickerClicks()) + " depuis le lancement, dont "
                + Format.whole(game.clickerAutoClicks()) + " automatiques", game::isClickerOpen);
        row("Cadence du clicker", () -> ElementText.number(game.clickerRate()) + " clics par seconde, "
                + Format.multiplier(game.clickerSpeedFactor(game.clickerRate())), game::isClickerOpen);
        row("Zones superposées", () -> "au plus " + game.clickerBestStack() + " sous un clic depuis le lancement",
                game::areClickerZonesUnlocked);
        row("Météo de la graine", () -> game.weather().label() + " encore " + Format.wait(game.weatherLeft()) + ", arrosage "
                + Format.multiplier(game.weatherFactor()) + " ; " + Format.whole(game.sunnySpells()) + " beau"
                + (game.sunnySpells() > 1 ? "x" : "") + " temps et " + Format.whole(game.storms()) + " orage"
                + (game.storms() > 1 ? "s" : "") + " depuis le lancement", game::isWeatherUnlocked);
        dimension(8, "Grille", Game.GRID_THRESHOLD, game::isGridOpen, () -> game.state().gridShardsEarned(), "Repères de la grille");
        row("Essais de la grille", () -> Format.whole(game.gridProbes()) + " depuis le lancement, dont "
                + Format.whole(game.gridAutoProbes()) + " automatiques", game::isGridOpen);
        row("Points trouvés", () -> Format.whole(game.gridFound()) + " depuis le lancement, " + game.gridPointCount()
                + " caché" + (game.gridPointCount() > 1 ? "s" : "") + " dans " + Format.whole(game.gridDivisions()) + "³ cases, "
                + game.gridAttempts() + (game.gridAttempts() > 1 ? " essais" : " essai") + " chacun", game::isGridOpen);
        row("Graine profonde", () -> "tout l'arrosage " + Format.multiplier(game.deepSeed()) + ", "
                + Format.multiplier(game.deepSeedFull()) + " au niveau " + Game.SeedUpgrade.DEEP_SEED.max(),
                () -> game.seedUpgradeLevel(Game.SeedUpgrade.DEEP_SEED) > 0);
        dimension(9, "Miroir", Game.MIRROR_THRESHOLD, game::isMirrorOpen, () -> game.state().mirrorShardsEarned(), "Reflets du miroir");
        row("Motifs du miroir", () -> Format.whole(game.mirrorSuccesses()) + " réussis, " + Format.whole(game.mirrorFails())
                + " manqués depuis le lancement", game::isMirrorOpen);
        row("Plus long motif réussi", () -> game.mirrorBest() + " cases (le motif en cours : " + game.mirrorLength() + ")", game::isMirrorOpen);
        row("Plateau du miroir", () -> game.mirrorColumns() + " × " + game.mirrorRows() + ", agrandi " + game.mirrorGrowth() + " fois sur "
                + game.mirrorMaxGrowth(), game::isMirrorOpen);
        row("Saison de la graine", () -> game.season().label() + " " + Format.multiplier(game.season().fill()) + ", encore "
                + Format.wait(game.seasonLeft()) + " ; " + ElementText.number(game.seasonFilled()) + " s versées au chrono depuis le lancement",
                game::areSeasonsUnlocked);

        dimension(10, "Mélange", Game.MIX_THRESHOLD, game::isMixOpen, () -> game.state().mixShardsEarned(), "Fragments du mélange");
        row("Étape du mélange", () -> game.mixStage() + " sur " + Game.MIX_STAGES + (game.mixStage() < Game.MIX_STAGES
                ? ", " + Format.darkMatter(BigNum.of(game.state().mixStageEarned())) + " / " + Format.darkMatter(BigNum.of(game.mixStageGoal()))
                        + " fragments gagnés pour la suivante" : ", tout est mêlé"), game::isMixOpen);
        row("Charges versées", () -> Format.whole(game.mixReleases()), () -> game.isMixOpen() && game.isMixing(4));
        row("Trésors du mélange", () -> Format.whole(game.mixTreasures()) + " trouvés", () -> game.isMixOpen() && game.isMixing(8));
        row("Paires du mélange", () -> Format.whole(game.mixPairStreak()) + " de suite", () -> game.isMixOpen() && game.isMixing(9));
        // Le défi final (version 0.160) : les paliers, l'avancée de chaque dimension, et la fin de la partie.
        row("Paliers du défi final", () -> game.finalTiersReached() + " / " + Game.MAX_DIMENSIONS + " atteints", game::isFinalChallengeOpen);
        for (int d = 1; d <= Game.MAX_DIMENSIONS; d++) {
            int number = d;
            row("Défi final, " + idle.ui.universe.Dimension.of(number).label().toLowerCase(java.util.Locale.ROOT),
                    () -> Math.round(game.finalProgress(number) * 100) + " % (" + Format.darkMatter(BigNum.of(game.dimensionEarned(number)))
                            + " / " + Format.darkMatter(BigNum.of(Game.finalGoal(number))) + ")"
                            + (game.isFinalTierReached(number) ? ", palier atteint" : ""), game::isFinalChallengeOpen);
        }
        row("Fin de la partie", () -> "atteinte : les dix paliers du défi final", game::isGameFinished);

        section("Le vide");
        charted("Énergie du vide", row("Énergie du vide", () -> Format.count(game.voidEnergy()) + " (équilibre : "
                + Format.count(BigNum.of(game.voidEquilibrium())) + ")", game::isVoidUnlocked));
        charted("Énergie du vide par seconde", row("Gagnée par seconde", () -> ElementText.number(game.voidGainPerSecond())
                + ", borné par " + (game.voidLimit().equals("rythme") ? "le rythme" : "l'" + game.voidLimit()), game::isVoidUnlocked));
        row("Paires vivantes", () -> Format.whole(Math.round(game.voidPairs())) + " (" + Format.whole(Math.round(game.voidPairsBorn()))
                + " nées depuis l'ouverture)", game::isVoidUnlocked);
        row("Niveaux d'amélioration du vide", () -> Format.whole(VoidPage.levelsOwned(game)), game::isVoidUnlocked);
        row("Ce que le vide raccourcit", () -> VoidPage.actThreeCuts(game), game::isVoidUnlocked);
        row("Masse du trou noir", () -> ElementText.number(game.voidMass()), () -> game.isVoidUnlocked() && game.isVoidHoleOpen());
        row("Énergie du vide gardée des univers formés", () -> Format.count(BigNum.of(game.voidAvailable())) + " disponible",
                () -> game.state().voidAccumulated() > 0);
        row("Bulles de l'arbre", () -> {
            int bought = 0;
            for (Game.TreeBubble bubble : Game.TreeBubble.values()) if (game.isTreeBubbleBought(bubble)) bought++;
            return bought + " / " + game.treeBubblesShown().size() + " achetées";
        }, () -> !game.treeBubblesShown().isEmpty());
        // Ce que les bulles produisent seules (version 0.148).
        row("Versé au cumul par le géant de gauche", () -> Format.count(BigNum.of(game.state().voidTrickled())) + " ("
                + Math.round(100 * game.voidTrickleShare()) + " % du gain du vide)", () -> game.voidTrickleShare() > 0);
        row("Atomes par seconde, sans fusion", () -> Format.count(game.bubbleAtomsPerSecond()),
                () -> game.isTreeBubbleBought(Game.TreeBubble.CROWN_RIGHT));
        row("Matière noire par seconde, sans explosion", () -> Format.count(game.bubbleDarkMatterPerSecond()),
                () -> game.isTreeBubbleBought(Game.TreeBubble.GIANT_RIGHT_HIGH));

        // Les hadrons (version 0.149) : ce que les gluons ont lié, tant que leur onglet est ouvert.
        section("Les hadrons");
        row("Hadrons assemblés", () -> Format.whole(game.hadronsMade()), game::isHadronsUnlocked);
        row("Protons", () -> Format.whole(game.hadrons(Hadron.PROTON)), game::isHadronsUnlocked);
        row("Neutrons", () -> Format.whole(game.hadrons(Hadron.NEUTRON)), game::isHadronsUnlocked);
        row("Particules exotiques", () -> {
            long exotic = 0;
            for (Hadron hadron : Hadron.values()) if (hadron.isExotic()) exotic += game.hadrons(hadron);
            return game.isExoticHadronsUnlocked() || exotic > 0 ? Format.whole(exotic) : "pas encore ouvertes";
        }, game::isHadronsUnlocked);
        // Version 0.151 : les gluons, un par quark lié, et les paliers qu'ils ont atteints.
        row("Gluons", () -> Format.whole(game.gluons()), game::isHadronsUnlocked);
        row("Paliers de gluons atteints", () -> {
            int reached = 0;
            for (GluonMilestone milestone : GluonMilestone.values()) if (game.isGluonMilestoneReached(milestone)) reached++;
            return reached + " / " + GluonMilestone.values().length;
        }, game::isHadronsUnlocked);

        // L'automatisation des dimensions (version 0.150) : avec la bulle du sommet de l'arbre-monde.
        section("L'automatisation des dimensions");
        row("Automatismes des dimensions", () -> game.dimensionAutomationsOwned() + " / " + DimensionAutomation.values().length
                + " achetés", game::isDimensionAutomationUnlocked);

        section("Les règles du deuxième univers");
        row("Régime rapide", () -> game.regimeMultiplier() > 1 ? "en cours, ×" + ElementText.number(game.regimeMultiplier())
                : game.canStartRegime() ? "prêt" : "pas encore prêt", game::isRegimeUnlocked);
        row("Synergie des rangs", () -> Format.multiplier(game.synergyMultiplier()), game::isSynergyUnlocked);
        row("Cycle parfait", () -> Format.multiplier(game.perfectCycleMultiplier()), game::isPerfectCycleUnlocked);
        row("Choix de cycle", () -> game.cycleChoice() == null ? "pas encore choisi" : game.cycleChoice().label(), game::isCycleChoiceUnlocked);
        row("Famille préférée", () -> game.preferredFamily() == null ? "aucune" : game.preferredFamily().label(), game::isFamilyPreferenceUnlocked);
        row("Épreuve en cours", () -> game.activeTrial().label() + ", encore " + Format.wait(game.trialLeft()), () -> game.activeTrial() != null);
        row("Épreuves réussies", () -> {
            int done = 0;
            for (idle.core.universe.Trial trial : idle.core.universe.Trial.values()) if (game.isTrialCompleted(trial)) done++;
            return done + " / " + idle.core.universe.Trial.values().length;
        }, () -> game.universes() > 0);
        row("Skins débloqués", () -> game.skinsUnlocked() + " / " + idle.core.universe.Skin.values().length, game::isSkinsUnlocked);
        row("Recherche du prisme", () -> game.hasPrismResearch() ? "faite" : "pas encore", () -> game.universes() > 0);
        row("Paliers de la recherche dense", () -> Format.whole(game.denseTiers()), game::isDenseResearchUnlocked);
    }

    /** Une dimension : sa ressource en réserve, ce qu'elle a gagné en tout et où en est son seuil. */
    private void dimension(int number, String name, double threshold, java.util.function.BooleanSupplier open,
            java.util.function.DoubleSupplier earned, String graph) {
        charted(graph, row(name + " : " + Game.dimensionResourceName(number), () -> ElementText.number(game.dimensionResource(number))
                + " en réserve, " + ElementText.number(earned.getAsDouble()) + " gagnés en tout", open));
        row(name + " : seuil", () -> game.isDimensionThresholdReached(number) ? "atteint (" + ElementText.number(threshold) + ")"
                : ElementText.number(earned.getAsDouble()) + " / " + ElementText.number(threshold), open);
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
