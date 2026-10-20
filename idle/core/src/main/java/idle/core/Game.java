package idle.core;

import idle.core.achievements.Achievement;
import idle.core.achievements.Achievements;
import idle.core.darkmatter.Challenge;
import idle.core.darkmatter.Challenges;
import idle.core.darkmatter.DarkEffect;
import idle.core.darkmatter.DarkUpgrade;
import idle.core.darkmatter.DarkUpgrades;
import idle.core.elements.ElementEffect;
import idle.core.elements.PeriodicTable;
import idle.core.math.BigNum;
import idle.core.molecules.Molecule;
import idle.core.probes.Probe;
import idle.core.sky.Cosmos;
import idle.core.upgrades.Automation;
import idle.core.upgrades.Automations;
import idle.core.upgrades.Effect;
import idle.core.upgrades.Upgrade;
import idle.core.upgrades.Upgrades;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.random.RandomGenerator;

/**
 * Les règles du jeu : comment les ressources sont créées et ce que font les améliorations.
 *
 * <p>Il y a deux ressources :
 * <ul>
 *   <li>les <b>particules</b>, créées en continu par les générateurs. Chaque générateur
 *       forme les siennes à son rythme, indépendamment des autres. Une partie neuve
 *       n'en a aucun : le joueur crée le premier avec {@link #start()}, les suivants s'achètent ;</li>
 *   <li>les <b>atomes</b>, obtenus avec {@link #fuse()} : une fois tous les générateurs
 *       débloqués, ils fusionnent en un atome et la partie repart d'un seul générateur.
 *       Les atomes se dépensent en améliorations définitives.</li>
 * </ul>
 *
 * <p>Une fois {@link #UNLOCK_TOTAL_ATOMS} atomes créés, deux choses se débloquent ensemble.
 * D'abord les automatismes ({@link Automation}) : le jeu achète alors lui-même les améliorations
 * payées en particules, voire fusionne tout seul. Un automatisme agit à intervalles réguliers ;
 * sa cadence s'améliore avec des atomes jusqu'à un plafond, puis avec des éléments.
 *
 * <p>Ensuite le tableau périodique : le joueur dépense des atomes pour
 * {@linkplain #synthesize() synthétiser} des
 * éléments tirés au sort. Chaque élément a son propre effet ({@link ElementEffect}), et le
 * premier élément unique obtenu débloque la synthèse automatique. Chaque élément a un nombre
 * maximal d'exemplaires : quand tous l'ont atteint, le tableau est complet.
 *
 * <p>Un tableau dont les 118 éléments sont découverts peut {@linkplain #explode() exploser},
 * sans attendre d'être complet : toute la matière disparaît, la partie repart du premier
 * générateur, et il reste de la <b>matière noire</b>, la troisième ressource.
 * Le joueur la fait grossir ({@link #growDarkMatter(double)}), et elle ouvre un arbre
 * d'améliorations définitives ({@link DarkUpgrade}), payées en particules, en atomes, ou
 * débloquées par sa taille.
 *
 * <p>Les particules gardent leur intérêt jusqu'au bout grâce au rendement de fusion
 * ({@link #fusionYield()}) : plus la production est forte au moment de fusionner, plus la
 * fusion rapporte d'atomes.
 *
 * <p>L'interface n'a besoin que de trois choses :
 * <ul>
 *   <li>appeler {@link #tick(double)} régulièrement ;</li>
 *   <li>lire l'état pour l'afficher ({@link #state()}, {@link #speed(int)}, {@link #generatorCount()}) ;</li>
 *   <li>envoyer les actions du joueur ({@link #start()}, {@link #buy(String)}, {@link #fuse()},
 *       {@link #synthesize()}…).</li>
 * </ul>
 *
 * <p>Le code est rangé en parties (version 0.098) : une chaîne de classes abstraites, une par thème, dont
 * celle-ci est le bout ; le reste du programme ne voit que {@code Game}. De la base au bout :
 * {@link GameBase}, {@link GameSystems}, {@link GameDarkTree}, {@link GameCollider}, {@link GamePlans},
 * {@link GameAtoms}, {@link GameUniverse}, {@link GameMolecules}, {@link GameMatterAutomation},
 * {@link GameParticles}, {@link GameDarkMatter}, {@link GameBigBang}. Quand une partie a besoin d'une
 * méthode définie plus loin dans la chaîne, elle la déclare abstraite en tête, sous « Ce que fournissent
 * les parties suivantes ».
 */
public final class Game extends GameBigBang {

    /** Nouvelle partie avec les catalogues par défaut, succès compris. */
    public Game() {
        this(new GameState(), Upgrades.DEFAULT, Automations.DEFAULT, DarkUpgrades.DEFAULT, Achievements.DEFAULT, new Random());
    }

    /** Partie sans aucun automatisme au catalogue. */
    public Game(GameState state, List<Upgrade> catalog) {
        this(state, catalog, List.of());
    }

    /** Partie reprise depuis un état existant, avec un hasard imprévisible pour la synthèse. */
    public Game(GameState state, List<Upgrade> catalog, List<Automation> automationCatalog) {
        this(state, catalog, automationCatalog, new Random());
    }

    /**
     * Partie reprise depuis un état existant (sauvegarde, test…).
     *
     * @param random source de hasard de la synthèse ; les tests en passent une reproductible
     */
    public Game(GameState state, List<Upgrade> catalog, List<Automation> automationCatalog,
                RandomGenerator random) {
        this(state, catalog, automationCatalog, DarkUpgrades.DEFAULT, random);
    }

    /** Comme le précédent, avec un arbre d'améliorations de matière noire sur mesure. Sans succès. */
    public Game(GameState state, List<Upgrade> catalog, List<Automation> automationCatalog,
                List<DarkUpgrade> darkCatalog, RandomGenerator random) {
        this(state, catalog, automationCatalog, darkCatalog, List.of(), random);
    }

    /**
     * Partie complète, avec son catalogue de succès. Les autres constructeurs n'en ont aucun :
     * leurs parties ne reçoivent donc aucun bonus de succès, ce qui garde les tests et les
     * simulations indépendants du catalogue.
     */
    public Game(GameState state, List<Upgrade> catalog, List<Automation> automationCatalog,
                List<DarkUpgrade> darkCatalog, List<Achievement> achievementCatalog, RandomGenerator random) {
        super(state, catalog, automationCatalog, darkCatalog, achievementCatalog, random);
    }

    // ------------------------------------------------------------------
    // Le démarrage
    // ------------------------------------------------------------------

    public boolean isStarted() {
        return state.started();
    }

    /** Crée le premier générateur. Sans effet si c'est déjà fait. */
    public void start() {
        state.setStarted(true);
    }

    /**
     * Recommence le jeu de zéro : tout est effacé, jusqu'à la matière noire, son arbre et les
     * statistiques. Il faudra créer de nouveau le premier générateur ({@link #start()}).
     */
    public void reset() {
        state.reset();
    }

    /**
     * L'état de la partie vient d'être remplacé par celui d'une sauvegarde : tout ce que le jeu
     * avait calculé d'avance est oublié, et ce que la sauvegarde nomme sans que le jeu le
     * connaisse (une molécule disparue depuis) est écarté.
     *
     * @return le nombre d'identifiants inconnus écartés ; 0 pour une sauvegarde de cette version du jeu
     */
    public int restored() {
        int dropped = state.retainKnown(new GameState.Known(upgrades.keySet(), automations.keySet(), darkUpgrades.keySet(),
                darkAutomations.keySet(), challengeIds(), achievements.keySet(), molecules.keySet(), assemblies.keySet(),
                bodies.keySet(), spaceUpgrades.keySet(), PeriodicTable.ELEMENTS.size()));
        newAchievements.clear();
        newRecords.clear();
        // Une sauvegarde d'avant la 0.083 dont l'univers est formé : la remise à zéro de l'univers ne s'était pas faite.
        if (state.universeFormed() && state.cosmosLevel() >= Cosmos.values().length) resetForUniverse();
        forgetDerived();
        return dropped;
    }

    /** Oublie tout ce que le jeu avait calculé d'avance : l'état vient de changer d'un coup. */
    void forgetDerived() {
        gatheredByStateVersion = -1;
        spaceByStateVersion = -1;
        occupiedSpaceVersion = -1;
        reservedSpaceVersion = -1;
        moleculeBonusesVersion = -1;
        achievementBonusesFor = -1;
        elementBonusesVersion = -1;
        newSpaceUpgrades.clear();
        cometAppeared = false;
        speedPrimePaid = false;
        kindCountsVersion = -1;
        sortsByStateVersion = -1;
        autoFormed.clear();
        autoGathered = 0;
        autoFormTimer = 0;
        shoppingVersion = Long.MIN_VALUE;
        shoppingSpaceKey = -1;
        nextAssemblyKey = Long.MIN_VALUE;
        finishedCollisions.clear();
        achievementTimer = 0;
        moleculeAutomationTimer = 0;
        moleculeAutomationTurn = 0;
    }

    private static java.util.Set<String> challengeIds() {
        java.util.Set<String> ids = new java.util.HashSet<>();
        for (Challenge challenge : Challenges.DEFAULT) ids.add(challenge.id());
        return ids;
    }

    // ------------------------------------------------------------------
    // L'absence
    // ------------------------------------------------------------------

    /** Part du temps d'absence qui compte au premier acte, avant la première explosion. */
    public static final double OFFLINE_RATE_PARTICLES = 0.10;
    /** Part du temps d'absence qui compte au deuxième acte, de la première explosion au premier Big Bang. */
    public static final double OFFLINE_RATE_DARK_MATTER = 0.40;
    /** Part du temps d'absence qui compte au troisième acte, du premier Big Bang à l'univers. */
    public static final double OFFLINE_RATE_BIG_BANG = 0.70;
    /** Part du temps d'absence qui compte une fois l'univers formé : tout. */
    public static final double OFFLINE_RATE_UNIVERSE = 1.0;

    /** Vrai pendant que {@link Absence} rejoue le temps passé jeu fermé. */
    private boolean away = false;

    /**
     * Fait avancer le jeu comme {@link #tick(double)}, mais pendant une absence rejouée : l'accord
     * d'absence (version 0.071) peut alors y tenter les défis ouverts.
     */
    public void tickAway(double dt) {
        away = true;
        try {
            tick(dt);
        } finally {
            away = false;
        }
    }

    /**
     * En absence (version 0.073) : une bifurcation ouverte où le joueur n'a pas choisi prend sa voie
     * d'absence, si elle est payable. Qui ne choisit pas joue le style de l'absence.
     */
    private void takeAbsenceVoies() {
        if (inChallenge() || !isDarkMatterUnlocked()) return;
        for (DarkUpgrade dark : darkUpgrades.values()) {
            if (!dark.isFork() || darkStyle(dark) != Style.ABSENCE || state.darkLevelOf(dark.id()) > 0) continue;
            if (darkForkRival(dark.id()) == null && canBuyDark(dark.id())) {
                buyDark(dark.id());
                awayVoies.add(dark.name());
            }
        }
    }

    /**
     * Les sondes en absence (version 0.086) : une sonde arrivée vide son butin et repart pour le même
     * voyage, comme le ferait le joueur. Joueur présent, rien ne change : le butin attend son clic,
     * et les sondes restent un à-côté, jamais une obligation.
     */
    private void runProbesWhileAway() {
        if (!isProbesUnlocked() || state.probes().isEmpty()) return;
        for (Probe probe : new ArrayList<>(state.probes())) {
            if (!probe.landed() || probe.id() < 0) continue;
            claimAllProbeRewards(probe.id());
            if (launchProbe(probe.destination(), probe.trip())) awayProbeTrips++;
        }
    }

    private int awayProbeTrips = 0;

    /** Les voyages de sonde que l'absence rejouée a relancés seule, puis les oublie. */
    public int takeAwayProbeTrips() {
        int trips = awayProbeTrips;
        awayProbeTrips = 0;
        return trips;
    }

    /** Les voies prises seules pendant l'absence en cours de rejeu, et les défis qu'elle a tentés et réussis. */
    private final List<String> awayVoies = new ArrayList<>();
    private final List<String> awayChallengesStarted = new ArrayList<>();

    /** Ce que l'absence rejouée a fait à la place du joueur (version 0.073), puis l'oublie. */
    public List<String> takeAwayVoies() {
        List<String> taken = new ArrayList<>(awayVoies);
        awayVoies.clear();
        return taken;
    }

    /** Les défis que l'accord d'absence a lancés pendant l'absence rejouée, puis les oublie. */
    public List<String> takeAwayChallenges() {
        List<String> started = new ArrayList<>(awayChallengesStarted);
        awayChallengesStarted.clear();
        return started;
    }

    /** Une partie qui commence, en absence, avec l'accord d'absence : elle devient le prochain défi à réussir. */
    private void startChallengeWhileAway() {
        if (inChallenge() || !hasDark(DarkEffect.ChallengesWhileAway.class) || state.stats().runTime() > 1) return;
        for (Challenge challenge : Challenges.DEFAULT) {
            if (isChallengeUnlocked(challenge.id()) && !isChallengeCompleted(challenge.id())) {
                if (startChallenge(challenge.id())) awayChallengesStarted.add(challenge.name());
                return;
            }
        }
    }

    /**
     * Part du temps passé jeu fermé qui est rejouée au retour ({@link Absence}) : un dixième au
     * début, et de plus en plus à mesure que les actes passent, jusqu'à tout le temps une fois
     * l'univers formé. Le jeu fermé avance donc toujours moins vite que le jeu ouvert, sauf à la fin.
     */
    public double offlineRate() {
        if (hasCosmos(Cosmos.UNIVERSE)) return OFFLINE_RATE_UNIVERSE;
        // Les voies d'absence de l'arbre (version 0.070) en rendent une part de plus, jusqu'à tout le temps.
        if (state.bigBangs() > 0) return Math.min(1, OFFLINE_RATE_BIG_BANG + darkOfflineBonus());
        if (isDarkMatterUnlocked()) return Math.min(1, OFFLINE_RATE_DARK_MATTER + darkOfflineBonus());
        return OFFLINE_RATE_PARTICLES;
    }

    // ------------------------------------------------------------------
    // Le temps
    // ------------------------------------------------------------------

    /**
     * Fait avancer le jeu de {@code dt} secondes.
     *
     * <p>Les particules ne montent pas en continu : chaque générateur remplit sa
     * création en cours ({@link GameState#formation(int)}), et chaque fois qu'elle est
     * complète, {@link #particlesPerCreation(int)} particules sont ajoutées.
     *
     * <p>Sert aussi au retour après une absence ({@link Absence}), qui rejoue par morceaux
     * une part du temps passé jeu fermé. Un seul gros tick donne le même résultat que
     * beaucoup de petits, à la seconde près quand un bonus dépend du temps.
     */
    public void tick(double dt) {
        if (dt < 0 || Double.isNaN(dt) || Double.isInfinite(dt)) {
            throw new IllegalArgumentException("Durée invalide : " + dt);
        }
        if (away) {
            takeAbsenceVoies();
            startChallengeWhileAway();
            runProbesWhileAway();
        }
        // La matière noire qui grossit seule : une petite part de ce que donnerait un appui.
        double autoShare = darkAutoExpansionShare();
        if (autoShare > 0) expandDarkMatter(dt * autoShare);
        // L'appui verrouillé : elle grossit comme si le joueur tenait le clic, quel que soit l'onglet affiché.
        if (isHoldLocked()) growDarkMatter(dt * HOLD_LOCK_SHARE);
        // L'appui automatique, acquis avec l'espace : il tient le clic en entier, ou ce que le verrou laisse.
        if (isAutoHolding()) growDarkMatter(dt * (isHoldLocked() ? 1 - HOLD_LOCK_SHARE : 1));

        expandSpace(dt);
        takeSpaceUpgrades();
        runComet(dt);
        refillPulses(dt);
        runCollider(dt);
        runProbes(dt);
        runMaturation(dt);
        runMoleculeAutomation(dt);
        runAutoForm(dt);
        runPlans(dt);
        runSecondUniverse(dt);
        runLine(dt);
        advance(dt);
        decay(dt);
        achievementTimer += dt;
        if (achievementTimer >= ACHIEVEMENT_CHECK_SECONDS) {
            achievementTimer = 0;
            checkAchievements();
        }
        noteUnlocks();
        recordHistory();
    }

    /** Fait passer le temps : la production, puis les automatismes à leur rythme. */
    private void advance(double dt) {
        boolean automation = hasActiveAutomation();
        if (!automation && !hasTimeDependentBonus()) {
            step(dt);
            return;
        }
        // Avec un automatisme en marche, on avance au rythme de son délai minimal : il agit ainsi
        // au même moment que le jeu soit affiché à 20 ou 200 images par seconde, ou rattrapé hors-ligne.
        double maxStep = automation ? minAutomationInterval() : MAX_STEP;
        double remaining = dt;
        do {
            double part = Math.min(remaining, maxStep);
            step(part);
            if (automation) {
                runAutomation(part);
                runDarkAutomation(part);
            }
            remaining -= part;
        } while (remaining > 0);
    }

    /** Avance de {@code dt} secondes en considérant tous les bonus comme constants. */
    private void step(double dt) {
        int generators = generatorCount();
        // Calculés une fois pour tous les générateurs : seuls leurs bonus individuels diffèrent.
        BigNum commonSpeed = generators > 0 ? speed() : BigNum.ZERO;
        BigNum commonParticles = null;
        BigNum gain = BigNum.ZERO;
        for (int generator = 0; generator < generators; generator++) {
            BigNum creations = commonSpeed.multiply(generatorSpeedMultiplier(generator) * dt);
            double completed;
            if (creations.exponent() < WHOLE_PARTICLES_MAX_EXPONENT) {
                double total = state.formation(generator) + creations.toDouble();
                completed = Math.floor(total);
                state.setFormation(generator, Math.min(total - completed, Math.nextDown(1.0)));
                if (completed <= 0) continue;
            } else {
                completed = -1; // vitesse gigantesque : la création en cours n'a plus de sens
            }
            if (commonParticles == null) commonParticles = particlesPerCreation();
            BigNum each = commonParticles.multiply(generatorParticlesMultiplier(generator));
            gain = gain.add(completed < 0 ? creations.multiply(each) : each.multiply(completed));
        }
        if (ruled(Challenge.Rule.VOLATILE_PARTICLES) && dt > 0) {
            // Les particules s'évaporent en continu. Ce qui reste de celles d'avant, plus ce qui reste
            // de celles créées pendant ce pas, comme si elles étaient arrivées régulièrement.
            double rate = -Math.log(1 - VOLATILE_LOSS_PER_SECOND);
            double kept = Math.exp(-rate * dt);
            state.setParticles(state.particles().multiply(kept).add(gain.multiply((1 - kept) / (rate * dt))));
            if (!gain.isZero()) state.stats().addParticles(gain);
        } else if (!gain.isZero()) {
            state.setParticles(state.particles().add(gain));
            state.stats().addParticles(gain);
        }
        state.setTimePlayed(state.timePlayed() + dt);
        state.stats().addTime(dt);
        if (state.started()) {
            state.setTimeSinceFusion(state.timeSinceFusion() + dt);
        }
    }

    /** Retient les automatismes disponibles en ce moment : une explosion peut les reverrouiller, pas les faire oublier. */
    private void noteUnlocks() {
        GameStats stats = state.stats();
        for (Automation automation : automations.values()) {
            if (!stats.wasUnlocked(automation.id()) && isAutomationAvailable(automation.id())) {
                stats.noteUnlocked(automation.id());
            }
        }
    }

    /**
     * Vrai si cet automatisme a déjà été disponible au moins une fois depuis le début du jeu,
     * même si une explosion l'a reverrouillé depuis.
     */
    public boolean wasAutomationEverAvailable(String automationId) {
        Automation automation = automation(automationId);
        if (isAutomationAvailable(automationId)) state.stats().noteUnlocked(automation.id());
        return state.stats().wasUnlocked(automation.id());
    }

    /** Ajoute un relevé aux historiques dont l'heure est venue. */
    private void recordHistory() {
        if (!state.started()) return;
        GameStats stats = state.stats();
        boolean whole = stats.history().isDue(state.timePlayed());
        boolean run = stats.runHistory().isDue(stats.runTime());
        if (!whole && !run) return;
        double[] values = measure();
        Map<String, Double> delays = new java.util.HashMap<>();
        for (Automation automation : automations.values()) {
            if (ownsAutomation(automation.id())) delays.put(automation.id(), automationInterval(automation.id()));
        }
        // Quelques valeurs dépendent de l'historique : chacun retient ses propres sommets.
        if (whole) stats.history().add(new StatsHistory.Sample(state.timePlayed(), withPeaks(values, stats.history()), delays));
        if (run) stats.runHistory().add(new StatsHistory.Sample(stats.runTime(), withPeaks(values, stats.runHistory()), delays));
    }

    /**
     * Les relevés d'un historique, suivis d'un relevé de l'instant (version 0.064). Les relevés
     * s'espacent avec le temps de jeu (plusieurs minutes après quelques heures) : sans ce dernier
     * point, une courbe toute neuve, ou une statistique apparue depuis le dernier relevé, resterait
     * vide jusqu'au suivant. Le relevé de l'instant n'est pas gardé.
     *
     * @param run vrai pour l'historique de la partie en cours, faux pour celui de tout le jeu
     */
    public List<StatsHistory.Sample> samplesWithNow(boolean run) {
        GameStats stats = state.stats();
        StatsHistory history = run ? stats.runHistory() : stats.history();
        List<StatsHistory.Sample> samples = new ArrayList<>(history.samples());
        if (!state.started()) return samples;
        double time = run ? stats.runTime() : state.timePlayed();
        if (!samples.isEmpty() && samples.get(samples.size() - 1).time() >= time) return samples;
        Map<String, Double> delays = new java.util.HashMap<>();
        for (Automation automation : automations.values()) {
            if (ownsAutomation(automation.id())) delays.put(automation.id(), automationInterval(automation.id()));
        }
        samples.add(new StatsHistory.Sample(time, withPeaks(measure(), history), delays));
        return samples;
    }

    /** Ces mesures, où chaque statistique qui retient un sommet ({@link StatsHistory.Stat#peaks()}) prend celui de cet historique. */
    private static double[] withPeaks(double[] values, StatsHistory history) {
        double[] copy = values.clone();
        for (StatsHistory.Stat stat : StatsHistory.Stat.values()) {
            if (stat.peaks()) copy[stat.ordinal()] = history.peak(stat, values[stat.ordinal()]);
        }
        return copy;
    }

    /**
     * Ce que le joueur a en main va retomber (fusion, explosion, défi, Big Bang) : les historiques
     * retiennent le sommet que chaque quantité vient d'atteindre.
     */
    void noteHeldPeaks() {
        double particles = power(state.particles());
        double atoms = power(state.atoms());
        double darkMatter = power(darkMatterEarned());
        double copies = ownedCopies();
        for (StatsHistory history : List.of(state.stats().history(), state.stats().runHistory())) {
            history.notePeak(StatsHistory.Stat.PARTICLES, particles);
            history.notePeak(StatsHistory.Stat.ATOMS, atoms);
            history.notePeak(StatsHistory.Stat.DARK_MATTER, darkMatter);
            history.notePeak(StatsHistory.Stat.ELEMENT_COPIES, copies);
        }
    }

    /** Mesure tout ce qu'un relevé retient : une valeur par {@link StatsHistory.Stat}, dans l'ordre de l'énumération. */
    private double[] measure() {
        GameStats stats = state.stats();
        double[] values = new double[StatsHistory.Stat.values().length];
        for (StatsHistory.Stat stat : StatsHistory.Stat.values()) {
            values[stat.ordinal()] = switch (stat) {
                case PRODUCTION -> productionPerSecond().log10();
                case PARTICLES_CREATED -> power(stats.particlesCreated());
                case PARTICLES_SPENT -> power(stats.particlesSpent());
                case SPEED -> power(speed());
                case PARTICLES_PER_CREATION -> power(particlesPerCreation());
                case GENERATORS -> generatorCount();
                case SPEED_LEVELS -> totalSpeedLevels();
                case SPEED_MILESTONES -> speedMilestones();
                case PARTICLE_UPGRADES_BOUGHT -> stats.particleUpgradesBought();
                case DARK_PARTICLES -> power(darkParticlesMultiplier());
                case ATOMS_PER_FUSION -> atomsPerFusion().log10();
                case FUSION_TIME -> stats.fusions() == 0 ? Double.NaN : stats.lastFusionTime();
                case ATOMS_CREATED -> power(stats.atomsCreated());
                case ATOMS_SPENT -> power(stats.atomsSpent());
                case FUSIONS -> stats.fusions();
                case FUSION_YIELD -> fusionYield();
                case ATOM_UPGRADES_BOUGHT -> stats.atomUpgradesBought();
                case AUTOMATION_ACTIONS -> power(BigNum.of(stats.automationActions() + stats.darkAutomationActions()));
                case ELEMENTS_SHARE -> (double) discoveredElements() / PeriodicTable.ELEMENTS.size();
                case COPIES_SHARE -> (double) ownedCopies() / maxTotalCopies();
                case SYNTHESES -> stats.syntheses();
                case SYNTHESIS_COST -> isPeriodicTableComplete() ? Double.NaN : power(synthesisCost());
                case DOUBLE_DRAW -> Math.min(1, doubleDrawChance());
                case SETS -> completedSets();
                case HALF_SETS -> halfSets();
                case ELEMENT_PARTICLES -> Math.log10(elementMultiplier(ElementEffect.Stat.PARTICLES));
                case ELEMENT_SPEED -> Math.log10(elementMultiplier(ElementEffect.Stat.SPEED));
                case ELEMENT_ATOMS -> Math.log10(elementMultiplier(ElementEffect.Stat.ATOMS));
                case DARK_MATTER_SIZE -> state.darkMatterSize().log10();
                case DARK_MATTER_EARNED -> darkMatterEarned().toDouble();
                case EXPLOSIONS -> state.explosions();
                case DARK_GROWTH -> isDarkMatterUnlocked()
                        ? power(darkMatterGrowthPerSecond().subtract(BigNum.ONE)) : Double.NaN;
                case DARK_RESISTANCE -> power(darkMatterResistance());
                case HOLD_TIME -> stats.holdTime();
                case LANDMARKS -> landmarksReached();
                case ACHIEVEMENTS -> achievementCount();
                case SPACE -> isBigBangUnlocked() ? power(state.space()) : Double.NaN;
                case SPACE_USED -> isBigBangUnlocked() ? power(usedSpace()) : Double.NaN;
                case MOLECULES -> moleculesCreated();
                case MOLECULE_SPACE -> Math.log10(moleculeBoost(Molecule.Stat.SPACE));
                case MOLECULE_PARTICLES -> Math.log10(moleculeBoost(Molecule.Stat.PARTICLES));
                case MOLECULE_ATOMS -> Math.log10(moleculeBoost(Molecule.Stat.ATOMS));
                case MOLECULE_DARK -> Math.log10(moleculeBoost(Molecule.Stat.DARK_GROWTH));
                case SKY -> assembliesFormed() + bodiesFormed() + state.cosmosLevel();
                case BIG_BANGS -> state.bigBangs();
                case SPACE_RATE -> isBigBangUnlocked() ? power(spacePerSecond()) : Double.NaN;
                case DARK_SPACE -> darkMatterSpaceBoost();
                case MOLECULE_CREATIONS -> state.stats().moleculeCreations();
                case AUTO_MOLECULE_CREATIONS -> state.stats().autoMoleculeCreations();
                case SUBSTANCES -> substancesFormed();
                case COLLIDER_ENERGY -> isColliderUnlocked() ? power(BigNum.of(state.colliderEnergy())) : Double.NaN;
                case COLLIDER_ENERGY_PRODUCED -> isColliderUnlocked() ? power(BigNum.of(state.colliderEnergyProduced())) : Double.NaN;
                case COLLIDER_ENERGY_RATE -> isColliderUnlocked() ? power(BigNum.of(colliderEnergyPerSecond())) : Double.NaN;
                case COLLISIONS -> isCollisionsUnlocked() ? state.collisionRuns() : Double.NaN;
                case QUARKS -> isCollisionsUnlocked() ? totalQuarks() : Double.NaN;
                case PARTICLES -> power(state.particles());
                case ATOMS -> power(state.atoms());
                case ELEMENT_COPIES -> ownedCopies();
                case DARK_MATTER -> power(darkMatterEarned());
            };
        }
        return values;
    }

    /** La puissance de dix d'une quantité, ou {@code NaN} tant qu'elle est nulle : la courbe ne commence qu'ensuite. */
    private static double power(BigNum value) {
        return value.sign() > 0 ? value.log10() : Double.NaN;
    }

    /** Niveaux de vitesse en tout, offerts compris : la somme sur les améliorations de vitesse du catalogue. */
    private int totalSpeedLevels() {
        int levels = 0;
        for (Upgrade upgrade : upgrades.values()) {
            if (upgrade.effect() instanceof Effect.MultiplySpeed) levels += speedLevels(upgrade.id());
        }
        return levels;
    }

    private boolean hasTimeDependentBonus() {
        for (Upgrade upgrade : upgrades.values()) {
            if (upgrade.effect() instanceof Effect.MultiplyByRunTime && state.levelOf(upgrade.id()) > 0) {
                return true;
            }
        }
        return false;
    }
}
