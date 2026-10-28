package idle.core;

import idle.core.collider.ColliderGenerator;
import idle.core.collider.ColliderUpgrade;
import idle.core.collider.Collision;
import idle.core.collider.Quark;
import idle.core.elements.ElementCategory;
import idle.core.math.BigNum;
import idle.core.probes.Probe;
import idle.core.probes.ProbeReward;
import idle.core.probes.ProbeUpgrade;
import idle.core.save.SaveData;
import idle.core.sky.SolarSystem;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * L'état de la partie, première partie : tous ses champs, puis les ressources, les achats, les éléments,
 * la matière noire, les molécules, les astres et les plans.
 */
public abstract class GameStateBase {

    /** Seul {@link GameState} prolonge la chaîne. */
    GameStateBase() {
    }


    boolean started = false;
    BigNum particles = BigNum.ZERO;
    BigNum atoms = BigNum.ZERO;
    BigNum totalAtoms = BigNum.ZERO;
    double timeSinceFusion = 0;
    /** Secondes de jeu depuis la dernière amélioration achetée (version 0.114) : la fuite et le calme du Big Bang. */
    double upgradeCalm = 0;
    /** Secondes de jeu depuis le dernier achat d'amélioration, joueur ou automatisme (version 0.124) : le calme du Big Bang. */
    double bangCalm = 0;
    final List<Double> formations = new ArrayList<>();
    double timePlayed = 0;
    final Map<String, Integer> upgradeLevels = new HashMap<>();
    final Set<String> ownedAutomations = new HashSet<>();
    final Set<String> enabledAutomations = new HashSet<>();
    final Map<String, Integer> automationSpeedLevels = new HashMap<>();
    final Map<String, Double> automationTimers = new HashMap<>();
    final Map<Integer, Integer> elements = new TreeMap<>();
    int elementsVersion = 0;
    /** Augmente à chaque changement des niveaux de l'arbre de matière noire : le jeu s'en sert pour ne recalculer leurs effets qu'alors. */
    int darkVersion = 0;
    int synthesisCount = 0;
    ElementCategory synthesisTarget = null;
    int synthesisTries = 0;
    /** Nombre de synthèses de suite qui n'ont donné aucun élément nouveau : au-delà d'un seuil, la suivante en donne un. */
    int drySyntheses = 0;
    BigNum darkMatter = BigNum.ZERO;
    BigNum darkMatterSpent = BigNum.ZERO;
    BigNum darkMatterSize = Game.DARK_MATTER_START_SIZE;
    int explosions = 0;
    int tableWeightLevel = 0;
    final Map<String, Integer> darkUpgradeLevels = new HashMap<>();
    int fusionThreshold = 0;
    BigNum synthesisReserve = BigNum.ZERO;
    boolean holdLocked = false;
    String activeChallenge = null;
    final Set<String> completedChallenges = new HashSet<>();
    final Set<String> completedChallengesView = Collections.unmodifiableSet(completedChallenges);
    /** Augmente à chaque changement des défis réussis : le jeu s'en sert pour ne recalculer leurs récompenses qu'alors. */
    int challengesVersion = 0;
    final Map<String, Double> challengeTimes = new HashMap<>();
    double decayDebt = 0;
    final Set<String> achievements = new java.util.LinkedHashSet<>();
    /** Change à chaque modification des succès : {@link Game} y garde son compte sans reparcourir la liste. Non sauvegardé. */
    int achievementsVersion = 0;
    final Set<String> enabledDarkAutomations = new HashSet<>();
    final Map<String, Double> darkAutomationTimers = new HashMap<>();
    int bigBangs = 0;
    final Map<String, Integer> molecules = new HashMap<>();
    /**
     * Les molécules dans l'ordre de leur création, par suites : la sorte de chaque suite, et le rang
     * où elle finit. Une création ajoute des centaines de molécules d'un coup ; on ne garde donc pas
     * une entrée par molécule.
     */
    final List<String> logKinds = new ArrayList<>();
    long[] logEnds = new long[16];
    private final List<String> moleculeLog = new MoleculeLog();
    int moleculesVersion = 0;
    final Set<String> substances = new java.util.LinkedHashSet<>();
    final Set<String> assemblies = new java.util.LinkedHashSet<>();
    final Set<String> bodies = new java.util.LinkedHashSet<>();
    /** Les systèmes solaires (version 0.054). */
    final List<SolarSystem> solarSystems = new ArrayList<>();
    /** Les astres formés qui mûrissent encore (version 0.053), avec les secondes qu'il leur reste. */
    final Map<String, Double> maturing = new java.util.LinkedHashMap<>();
    /** Nombre d'échelles du cosmos formées, dans l'ordre de {@link idle.core.sky.Cosmos} : 0 = aucune, 1 = la galaxie, 2 = l'amas de galaxies, 3 = l'univers. */
    int cosmos;
    /** Vrai tant que l'appui automatique sur la matière noire est en marche, une fois acquis ({@link Game#isAutoHolding()}). */
    boolean autoHold = true;
    /** Vrai tant que la création automatique des molécules est en marche, une fois acquise ({@link Game#isAutoCreatingMolecules()}). */
    boolean autoMolecules = true;
    /** La création automatique suit aussi la liste de courses : elle crée ce que le prochain pas demande. */
    boolean autoFollowList = true;
    /** Option facultative : entretien des substances utiles aux besoins de matière du prochain objectif. */
    boolean autoFollowMatter = false;
    /** Les sortes de molécules que la création automatique entretient, dans l'ordre où le joueur les a choisies. */
    final Set<String> automatedMolecules = new java.util.LinkedHashSet<>();
    /**
     * Les dernières listes rendues par {@link #substances()}, {@link #assemblies()} et {@link #bodies()} :
     * elles sont demandées bien plus souvent qu'elles ne changent, et ne sont recopiées qu'après un changement.
     */
    List<String> substancesSeen;
    List<String> assembliesSeen;
    List<String> bodiesSeen;
    final Set<String> spaceUpgrades = new java.util.LinkedHashSet<>();
    /** Vrai tant que le joueur a coupé d'un geste tous les automatismes : aucun n'agit, et chacun garde son propre réglage. */
    boolean automationPaused = false;
    /** Vrai tant que le rassemblement automatique est en marche, une fois acquis ({@link Game#isAutoGathering()}). */
    boolean autoGather = true;
    /** Vrai tant que la formation automatique des assemblages et des astres est en marche, une fois acquise ({@link Game#isAutoForming()}). */
    boolean autoForm = true;
    /** Part de l'espace créé que la création automatique laisse toujours libre (0,25 pour un quart). */
    double autoMoleculeReserve = 0;
    /** Durée de l'explosion la plus rapide qui a valu une prime depuis le dernier Big Bang, en secondes ; 0 tant qu'il n'y en a pas. */
    double explosionRecord = 0;
    // La comète : le temps avant la prochaine, le temps qu'elle reste à l'écran, le temps que dure encore son sillage.
    double cometWait = 0;
    double cometVisible = 0;
    double cometBoost = 0;
    int cometsCaught = 0;
    // Les anomalies du ciel (version 0.127) : celle qui est à saisir, combien de temps encore, l'attente avant la
    // suivante, le vent de particules en cours, et combien en ont été saisies.
    String skyEvent = null;
    double skyEventVisible = 0;
    double skyEventWait = 0;
    double windLeft = 0;
    int skyEventsCaught = 0;
    /** Vrai une fois l'écran de fin vu (version 0.068) : il ne s'ouvre qu'une fois par partie. */
    boolean endSeen = false;
    // La partie secondaire (version 0.081), sans logique pour l'instant : les univers créés après le
    // premier, les graines d'univers, l'énergie du vide, et le niveau des améliorations de l'univers.
    int extraUniverses = 0;
    boolean universeFormed = false;
    // Les plans de l'arbre et des systèmes solaires (version 0.084).
    final List<String> darkPlan = new ArrayList<>();
    /** Les emplacements de sauvegarde des plans (version 0.086) : {@link #PLAN_SLOTS} chacun, vides au départ. */
    public static final int PLAN_SLOTS = 5;
    final List<String> savedDarkPlans = emptySlots();
    final List<SolarSystem> systemPlan = new ArrayList<>();
    final List<String> savedSystemPlans = emptySlots();

    /** Relit des emplacements ; une sauvegarde de la 0.084 ou 0.085 n'en avait qu'un, sous l'ancienne clé. */
    static void readSlots(SaveData data, String key, String oldKey, List<String> slots) {
        java.util.Collections.fill(slots, "");
        List<String> read = data.has(key) ? data.list(key) : List.of(String.join("", data.list(oldKey)));
        for (int i = 0; i < Math.min(PLAN_SLOTS, read.size()); i++) slots.set(i, read.get(i) == null ? "" : read.get(i));
    }

    private static List<String> emptySlots() {
        return new ArrayList<>(java.util.Collections.nCopies(PLAN_SLOTS, ""));
    }
    boolean forkResetArmed = false;
    // Planifier la matière (version 0.085).
    final List<String> plannedBodies = new ArrayList<>();
    final Map<String, Double> stateCaps = new TreeMap<>();
    boolean recipeReservation = false;
    // Le deuxième univers (version 0.087).
    boolean replaying = false;
    long fusionsAtUniverse = 0;
    /** Explosions déjà faites quand l'univers en cours a commencé (version 0.124) : la maturité du tableau se refait à chaque univers. */
    long explosionsAtUniverse = 0;
    boolean skinsOpen = false;
    final Set<String> unlockedSkins = new java.util.TreeSet<>();
    final Map<String, String> chosenSkins = new TreeMap<>();
    boolean regimeOpen = false;
    double regimeLeft = 0;
    double regimeRest = 0;
    boolean synergyOpen = false;
    int perfectStreak = 0;
    double perfectLeft = 0;
    String cycleChoice = null;
    String preferredFamily = null;
    int preferenceDraws = 0;
    int freeElementsUsed = 0;
    // Les skins à obtenir (version 0.088).
    String activeTrial = null;
    double trialStart = 0;
    double trialBase = 0;
    int trialCount = 0;
    final Set<String> completedTrials = new java.util.TreeSet<>();
    /** Les fenêtres d'explication déjà montrées (version 0.089) : onglets ouverts et nouveautés du deuxième univers. */
    final Set<String> seenPopups = new java.util.TreeSet<>();
    /** Vrai une fois les fenêtres d'une partie d'avant la 0.089 marquées vues, sans les montrer. */
    boolean popupsPrimed = false;
    double rebuildRecord = 0;
    boolean rebuildPending = false;
    final Set<String> researches = new java.util.TreeSet<>();
    BigNum universeSeeds = BigNum.ZERO;
    BigNum voidEnergy = BigNum.ZERO;
    /**
     * Le vide (version 0.129) : ouvert (payé en énergie du collisionneur), son trou noir ouvert, la part de l'expansion
     * qu'on lui donne, les particules sacrifiées au trou noir, et le niveau de ses huit améliorations. Tout repart de
     * zéro à chaque univers, sauf ce qu'il reste à la formation de chacun ({@link #voidHistory}).
     */
    boolean voidOpen = false, voidHoleOpen = false;
    double voidShare = 0.25;
    /** Le prix d'ouverture du vide, fixé quand il se propose (0 tant qu'il ne s'est pas proposé dans cet univers). */
    double voidPrice = 0;
    BigNum voidSacrificed = BigNum.ZERO;
    /** Le nombre d'améliorations du vide, celles de la sauvegarde, et leurs niveaux dans l'ordre de {@code Game.VoidUpgrade}. */
    static final int VOID_UPGRADES = 8;
    final int[] voidLevels = new int[VOID_UPGRADES];
    /** L'énergie du vide qu'il restait à la formation de chaque univers où le vide était ouvert : numéro de l'univers → stock. */
    final Map<String, Double> voidHistory = new java.util.LinkedHashMap<>();
    /**
     * La ligne, première dimension (version 0.101) : ses éclats, où en est la boule (0 au début de la
     * ligne, 1 au bout, 2 de retour au début), le niveau de ses cinq améliorations dans l'ordre de
     * {@code Game.LineUpgrade} (éclats, traits, vitesse, boules, lignes) et tous les éclats gagnés, dépensés ou non.
     */
    double dimensionShards = 0;
    double linePhase = 0;
    static final int LINE_UPGRADES = 5;
    final int[] lineLevels = new int[LINE_UPGRADES];
    double dimensionShardsEarned = 0;
    /** La graine d'univers (version 0.103) : germée ou non, et combien de fois son germe a été arrosé. */
    boolean seedSprouted = false;
    long seedWaterings = 0;
    /** L'arrosoir de la graine (version 0.137) : débloqué sur son étagère, il remplace le bouton d'arrosage. */
    boolean wateringCan = false;
    /** Vrai une fois que la brindille a ouvert la partie du deuxième univers (version 0.109) : elle ne l'ouvre qu'une fois. */
    boolean twigOpened = false;
    /** Univers que la graine a ouverts (version 0.120) : le palier N + 1 ouvre le suivant. */
    int seedUniversesOpened = 0;
    /**
     * Le plan, deuxième dimension (version 0.104) : ses éclats, propres à lui, tous ceux gagnés, et le
     * niveau de ses cinq améliorations (valeur, réapparition, nombre, vitesse, taille), dans cet ordre.
     */
    double planeShards = 0;
    double planeShardsEarned = 0;
    static final int PLANE_UPGRADES = 5;
    final int[] planeLevels = new int[PLANE_UPGRADES];
    /**
     * Le volume, troisième dimension (version 0.128) : ses éclats, tous ceux gagnés, et le niveau de ses cinq
     * améliorations (durée, valeur, réapparition, nombre, taille), dans cet ordre.
     */
    double volumeShards = 0;
    double volumeShardsEarned = 0;
    static final int VOLUME_UPGRADES = 5;
    final int[] volumeLevels = new int[VOLUME_UPGRADES];
    /**
     * La durée, quatrième dimension (version 0.134) : ses éclats, tous ceux gagnés, et le niveau de ses quatre
     * améliorations (vitesse, nombre de spirales, valeur, temps au plus), dans cet ordre.
     */
    double durationShards = 0;
    double durationShardsEarned = 0;
    static final int DURATION_UPGRADES = 4;
    final int[] durationLevels = new int[DURATION_UPGRADES];
    /**
     * Les améliorations de la graine (version 0.139), dans l'ordre de {@code Game.SeedUpgrade} : débit de l'arrosoir,
     * tuyau, terre fertile, étage du chrono, puis remplissage, réserve, accélération et pertes du chrono. Et le chrono :
     * le temps qu'il garde, en secondes, et l'accélération choisie (×1 : arrêté).
     */
    static final int SEED_UPGRADES = 8;
    final int[] seedUpgradeLevels = new int[SEED_UPGRADES];
    double chronoStored = 0;
    double chronoSpeed = 1;
    /**
     * Les bulles de l'arbre (version 0.142) : le niveau de chacune, dans l'ordre de {@code Game.TreeBubble} (0 : pas
     * achetée ; version 0.148 : certaines s'achètent plusieurs fois), et l'énergie du vide accumulée (celle qui restait à
     * la formation de chaque univers) déjà dépensée pour elles. Et ce que la bulle du géant de gauche verse au cumul,
     * seconde après seconde (version 0.148).
     */
    /**
     * Les hadrons (version 0.149) : l'onglet ouvert dans cet univers, son prix fixé quand il s'est proposé, et ce
     * qu'on en a assemblé, par sorte.
     */
    boolean hadronsOpen = false;
    double hadronsPrice = 0;
    final java.util.EnumMap<idle.core.collider.Hadron, Long> hadrons = new java.util.EnumMap<>(idle.core.collider.Hadron.class);
    static final int TREE_BUBBLES = 31;
    final int[] treeBubbleLevels = new int[TREE_BUBBLES];
    double voidSpent = 0;
    double voidTrickled = 0;
    final Map<String, Integer> universeUpgrades = new HashMap<>();
    /** Charges d'impulsion en réserve (fractions comprises) et impulsions données depuis le début. */
    double pulseCharges = 0;
    // Le collisionneur de quarks (version 0.050) : rien ne s'y perd à l'explosion ni au Big Bang.
    double colliderEnergy = 0;
    /** Les générateurs du collisionneur (version 0.057) : ce qu'on en a, et combien ont été achetés. */
    final java.util.EnumMap<ColliderGenerator, Double> colliderGenerators = new java.util.EnumMap<>(ColliderGenerator.class);
    final java.util.EnumMap<ColliderGenerator, Integer> colliderGeneratorsBought = new java.util.EnumMap<>(ColliderGenerator.class);
    /**
     * Les faisceaux (version 0.059) : leur phase, de 0 à 1 (ils se croisent à 0,5), le nombre de
     * croisements depuis le début, et le numéro du dernier croisement capté d'un clic.
     */
    double colliderPhase = 0;
    long colliderCrossings = 0;
    long colliderCaught = -1;
    /** Les améliorations du collisionneur (version 0.059) et les automates mis en pause par le joueur. */
    final java.util.EnumMap<ColliderUpgrade, Integer> colliderUpgrades = new java.util.EnumMap<>(ColliderUpgrade.class);
    final java.util.EnumSet<ColliderUpgrade> colliderPaused = java.util.EnumSet.noneOf(ColliderUpgrade.class);
    final java.util.EnumMap<Quark, Integer> quarks = new java.util.EnumMap<>(Quark.class);
    /** L'énergie produite depuis le début (version 0.062) : elle ouvre les deux trous noirs. */
    double colliderEnergyProduced = 0;
    final List<Collision> collisions = new ArrayList<>();
    long collisionRuns = 0;
    double topLeft = 0;
    double overheatLeft = 0;
    // Les sondes (version 0.052) : comme le collisionneur, elles traversent l'explosion et le Big Bang.
    final List<Probe> probes = new ArrayList<>();
    final List<ProbeReward> probeRewards = new ArrayList<>();
    final java.util.EnumMap<ProbeUpgrade, Integer> probeUpgrades = new java.util.EnumMap<>(ProbeUpgrade.class);
    long probeRuns = 0;
    /** Numéros donnés aux sondes au départ. */
    long probeLaunches = 0;
    long probeSamples = 0;
    double probeBoostLeft = 0;
    long pulses = 0;
    /** Les records du joueur, étape par étape : le temps de jeu le plus court auquel chacune a été atteinte. La remise à zéro les garde. */
    final Map<String, Double> records = new HashMap<>();
    // Le profil du joueur (version 0.147) : comme les records, rien ne le reprend, pas même « Recommencer de zéro ».
    /** Le nom du joueur ; vide tant qu'il n'en a pas choisi. */
    String playerName = "";
    /** L'avatar choisi : l'identifiant d'un avatar de la galerie, ou {@code custom} pour une image importée. */
    String avatar = "";
    /** Le titre porté, par son nom ; vide pour le dernier obtenu. */
    String chosenTitle = "";
    /** Les titres obtenus, par leur nom, avec le temps de jeu auquel chacun l'a été. */
    final Map<String, Double> earnedTitles = new HashMap<>();
    /**
     * Les albums (version 0.147) : ce que le joueur a déjà obtenu au moins une fois, dans n'importe quel univers, avec le
     * temps de jeu de la première fois. Les molécules créées, les sortes rassemblées, les assemblages et les astres formés.
     */
    final Map<String, Double> foundMolecules = new HashMap<>();
    final Map<String, Double> foundSubstances = new HashMap<>();
    final Map<String, Double> foundAssemblies = new HashMap<>();
    final Map<String, Double> foundBodies = new HashMap<>();
    // Les défis de Big Bang : celui en cours, ceux qui sont réussis, et leur meilleur temps.
    String activeBangChallenge = null;
    final Set<String> completedBangChallenges = new HashSet<>();
    /** Les paliers de Big Bang payés (version 0.124), par leur nom. */
    final Set<String> boughtMilestones = new HashSet<>();
    final Map<String, Double> bangChallengeTimes = new HashMap<>();
    /** Le temps de jeu auquel le défi de Big Bang en cours a commencé. */
    double bangChallengeStarted = 0;
    /** L'ordre dans lequel l'automatisme achète les améliorations en atomes, quand le joueur a choisi le sien. */
    final List<String> atomUpgradeOrder = new ArrayList<>();
    boolean atomUpgradeOrdered = false;
    /** L'ordre dans lequel l'automatisme achète les automatismes ordinaires et leurs cadences, quand le joueur a choisi le sien. */
    final List<String> automationOrder = new ArrayList<>();
    boolean automationOrdered = false;
    /** Les molécules épinglées par le joueur, dans l'ordre où il les a choisies. */
    final Set<String> favoriteMolecules = new java.util.LinkedHashSet<>();
    BigNum space = BigNum.ZERO;
    GameStats stats = new GameStats();

    /** Les statistiques de la partie : des compteurs pour l'affichage, que les règles ne lisent pas. */
    public GameStats stats() {
        return stats;
    }

    /** Faux tant que le joueur n'a pas créé son premier générateur. */
    public boolean started() {
        return started;
    }

    public void setStarted(boolean started) {
        this.started = started;
    }

    /** Particules possédées : la ressource de base, créée par les générateurs. */
    public BigNum particles() {
        return particles;
    }

    public void setParticles(BigNum particles) {
        this.particles = nonNegative(particles, "Le nombre de particules ne peut pas être négatif");
    }

    /** Atomes disponibles : la deuxième ressource, gagnée par fusion et dépensée en améliorations. */
    public BigNum atoms() {
        return atoms;
    }

    public void setAtoms(BigNum atoms) {
        this.atoms = nonNegative(atoms, "Le nombre d'atomes ne peut pas être négatif");
    }

    /**
     * Atomes créés dans la partie en cours, dépensés ou non : depuis la dernière explosion, ou depuis le départ lancé
     * qu'elle accorde. Seules les remises à zéro le font redescendre ({@link GameState#clearMatter()}).
     */
    public BigNum totalAtoms() {
        return totalAtoms;
    }

    public void setTotalAtoms(BigNum totalAtoms) {
        this.totalAtoms = nonNegative(totalAtoms, "Le nombre d'atomes ne peut pas être négatif");
    }

    /** Temps écoulé depuis la dernière fusion (ou depuis le démarrage), en secondes. */
    public double timeSinceFusion() {
        return timeSinceFusion;
    }

    /** Secondes de jeu depuis la dernière amélioration achetée par le joueur (version 0.114) : la fuite. */
    public double upgradeCalm() {
        return upgradeCalm;
    }

    public void setUpgradeCalm(double seconds) {
        upgradeCalm = seconds >= 0 ? seconds : 0;
    }

    /** Secondes depuis le dernier achat d'amélioration, automatismes compris (version 0.124) : le calme du Big Bang. */
    public double bangCalm() {
        return bangCalm;
    }

    public void setBangCalm(double seconds) {
        bangCalm = seconds >= 0 ? seconds : 0;
    }

    public void setTimeSinceFusion(double timeSinceFusion) {
        if (!(timeSinceFusion >= 0)) throw new IllegalArgumentException("Durée invalide : " + timeSinceFusion);
        this.timeSinceFusion = timeSinceFusion;
    }

    /**
     * Avancement de la particule en cours sur un générateur, de 0 (rien) à 1 (exclu).
     * Arrivé à 1, la création est terminée : les particules sont ajoutées et on repart de 0.
     *
     * @param generator numéro du générateur, 0 étant le premier
     */
    public double formation(int generator) {
        return generator < formations.size() ? formations.get(generator) : 0;
    }

    public void setFormation(int generator, double formation) {
        if (!(formation >= 0 && formation < 1)) {
            throw new IllegalArgumentException("La formation doit être dans [0, 1[ : " + formation);
        }
        while (formations.size() <= generator) {
            formations.add(0.0);
        }
        formations.set(generator, formation);
    }

    /** Remet tous les générateurs à zéro (après une fusion). */
    public void clearFormations() {
        formations.clear();
    }

    /** Vue en lecture seule des avancements, pour la sauvegarde. */
    public List<Double> formations() {
        return List.copyOf(formations);
    }

    /** Temps de jeu cumulé, en secondes. */
    public double timePlayed() {
        return timePlayed;
    }

    public void setTimePlayed(double timePlayed) {
        this.timePlayed = timePlayed;
    }

    /** Niveau possédé pour une amélioration (0 si jamais achetée). */
    public int levelOf(String upgradeId) {
        return upgradeLevels.getOrDefault(upgradeId, 0);
    }

    public void setLevel(String upgradeId, int level) {
        if (level < 0) throw new IllegalArgumentException("Niveau négatif");
        upgradeLevels.put(upgradeId, level);
    }

    /** Vrai si le joueur a acheté cet automatisme. */
    public boolean ownsAutomation(String automationId) {
        return ownedAutomations.contains(automationId);
    }

    public void addAutomation(String automationId) {
        ownedAutomations.add(automationId);
    }

    /** Vrai si cet automatisme est en marche. Un automatisme acheté peut être coupé. */
    public boolean isAutomationEnabled(String automationId) {
        return enabledAutomations.contains(automationId);
    }

    public void setAutomationEnabled(String automationId, boolean enabled) {
        if (enabled) {
            enabledAutomations.add(automationId);
        } else {
            enabledAutomations.remove(automationId);
        }
    }

    /** Niveau de cadence d'un automatisme : plus il est haut, plus le délai entre deux actions est court. */
    public int automationSpeedLevel(String automationId) {
        return automationSpeedLevels.getOrDefault(automationId, 0);
    }

    public void setAutomationSpeedLevel(String automationId, int level) {
        if (level < 0) throw new IllegalArgumentException("Niveau négatif");
        automationSpeedLevels.put(automationId, level);
    }

    /** Temps écoulé depuis la dernière action d'un automatisme, en secondes. */
    public double automationTimer(String automationId) {
        return automationTimers.getOrDefault(automationId, 0.0);
    }

    public void setAutomationTimer(String automationId, double seconds) {
        if (!(seconds >= 0)) throw new IllegalArgumentException("Durée invalide : " + seconds);
        automationTimers.put(automationId, seconds);
    }

    /** Vue en lecture seule des niveaux de cadence, pour la sauvegarde. */
    public Map<String, Integer> automationSpeedLevels() {
        return Map.copyOf(automationSpeedLevels);
    }

    /** Nombre d'exemplaires possédés d'un élément du tableau périodique (0 si jamais obtenu). */
    public int elementCount(int atomicNumber) {
        return elements.getOrDefault(atomicNumber, 0);
    }

    public void setElementCount(int atomicNumber, int count) {
        if (count < 0) throw new IllegalArgumentException("Nombre d'exemplaires négatif");
        if (count == 0) {
            elements.remove(atomicNumber);
        } else {
            elements.put(atomicNumber, count);
        }
        elementsVersion++;
    }

    /** Compteur qui change à chaque modification des éléments possédés ; sert à savoir quand recalculer leurs bonus. */
    public int elementsVersion() {
        return elementsVersion;
    }

    /** Nombre de synthèses déjà faites : c'est lui qui fixe le prix de la suivante. */
    public int synthesisCount() {
        return synthesisCount;
    }

    public void setSynthesisCount(int synthesisCount) {
        if (synthesisCount < 0) throw new IllegalArgumentException("Nombre de synthèses négatif");
        this.synthesisCount = synthesisCount;
    }

    /** Famille visée par la synthèse ciblée, ou {@code null} quand la synthèse tire au hasard. */
    public ElementCategory synthesisTarget() {
        return synthesisTarget;
    }

    public void setSynthesisTarget(ElementCategory synthesisTarget) {
        this.synthesisTarget = synthesisTarget;
    }

    /** Synthèses déjà payées pour la synthèse ciblée en cours, sans avoir encore rien donné. */
    public int synthesisTries() {
        return synthesisTries;
    }

    /** Nombre de synthèses de suite qui n'ont donné aucun élément nouveau. */
    public int drySyntheses() {
        return drySyntheses;
    }

    public void setDrySyntheses(int count) {
        drySyntheses = Math.max(0, count);
    }

    public void setSynthesisTries(int synthesisTries) {
        if (synthesisTries < 0) throw new IllegalArgumentException("Nombre d'essais négatif");
        this.synthesisTries = synthesisTries;
    }

    /** Matière noire possédée : la troisième ressource, laissée par chaque explosion du tableau périodique. */
    public BigNum darkMatter() {
        return darkMatter;
    }

    public void setDarkMatter(BigNum darkMatter) {
        this.darkMatter = nonNegative(darkMatter, "La quantité de matière noire ne peut pas être négative");
    }

    /**
     * Matière noire dépensée en améliorations depuis le début du jeu. Ajoutée à celle qui reste
     * ({@link #darkMatter()}), elle donne la matière noire gagnée en tout.
     */
    public BigNum darkMatterSpent() {
        return darkMatterSpent;
    }

    public void setDarkMatterSpent(BigNum darkMatterSpent) {
        this.darkMatterSpent = nonNegative(darkMatterSpent, "La matière noire dépensée ne peut pas être négative");
    }

    /** Taille de la matière noire, en mètres : elle grossit tant que le joueur la maintient appuyée. */
    public BigNum darkMatterSize() {
        return darkMatterSize;
    }

    public void setDarkMatterSize(BigNum darkMatterSize) {
        if (darkMatterSize.sign() <= 0) throw new IllegalArgumentException("La taille de la matière noire doit être positive");
        this.darkMatterSize = darkMatterSize;
    }

    /** Nombre d'explosions déclenchées depuis le début du jeu. */
    public int explosions() {
        return explosions;
    }

    public void setExplosions(int explosions) {
        if (explosions < 0) throw new IllegalArgumentException("Nombre d'explosions négatif");
        this.explosions = explosions;
    }

    /**
     * Nombre de fois où le tableau périodique s'est alourdi : une par explosion, sauf celles qui
     * terminent un défi déjà réussi. Aucune explosion ne le remet à zéro.
     */
    public int tableWeightLevel() {
        return tableWeightLevel;
    }

    public void setTableWeightLevel(int tableWeightLevel) {
        if (tableWeightLevel < 0) throw new IllegalArgumentException("Masse du tableau négative");
        this.tableWeightLevel = tableWeightLevel;
    }

    /** Nombre de molécules de cette sorte créées. Ni l'explosion ni le Big Bang ne les reprennent. */
    public int moleculeCount(String moleculeId) {
        return molecules.getOrDefault(moleculeId, 0);
    }

    /** Ajoute une molécule de cette sorte, à la suite de celles déjà créées. */
    public void addMolecule(String moleculeId) {
        addMolecules(moleculeId, 1);
    }

    /** Ajoute d'un coup {@code count} molécules de cette sorte, à la suite de celles déjà créées. */
    public void addMolecules(String moleculeId, int count) {
        if (moleculeId == null || moleculeId.isBlank()) throw new IllegalArgumentException("Molécule sans identifiant");
        if (count < 0) throw new IllegalArgumentException("Nombre de molécules négatif : " + count);
        if (count == 0) return;
        molecules.merge(moleculeId, count, Integer::sum);
        int last = logKinds.size() - 1;
        if (last >= 0 && logKinds.get(last).equals(moleculeId)) {
            logEnds[last] += count;
        } else {
            if (logKinds.size() == logEnds.length) logEnds = java.util.Arrays.copyOf(logEnds, logEnds.length * 2);
            logEnds[logKinds.size()] = (last >= 0 ? logEnds[last] : 0) + count;
            logKinds.add(moleculeId);
        }
        moleculesVersion++;
    }

    /**
     * Fixe le nombre de molécules d'une sorte : celles qui manquent sont ajoutées à la suite, celles
     * en trop sont retirées en commençant par les dernières créées.
     */
    public void setMoleculeCount(String moleculeId, int count) {
        if (count < 0) throw new IllegalArgumentException("Nombre de molécules négatif : " + count);
        int owned = moleculeCount(moleculeId);
        if (count >= owned) {
            addMolecules(moleculeId, count - owned);
            return;
        }
        int extra = owned - count;
        // Les suites de cette sorte rétrécissent en partant de la fin, puis la liste des suites est refaite.
        int runs = logKinds.size();
        long[] lengths = new long[runs];
        for (int run = 0; run < runs; run++) lengths[run] = logEnds[run] - (run == 0 ? 0 : logEnds[run - 1]);
        for (int run = runs - 1; run >= 0 && extra > 0; run--) {
            if (!logKinds.get(run).equals(moleculeId)) continue;
            long taken = Math.min(extra, lengths[run]);
            lengths[run] -= taken;
            extra -= taken;
        }
        List<String> kinds = new ArrayList<>(logKinds);
        logKinds.clear();
        long end = 0;
        for (int run = 0; run < runs; run++) {
            if (lengths[run] == 0) continue;
            end += lengths[run];
            int last = logKinds.size() - 1;
            // Deux suites de la même sorte qui se retrouvent voisines n'en font plus qu'une.
            if (last >= 0 && logKinds.get(last).equals(kinds.get(run))) {
                logEnds[last] = end;
            } else {
                logEnds[logKinds.size()] = end;
                logKinds.add(kinds.get(run));
            }
        }
        if (count == 0) molecules.remove(moleculeId);
        else molecules.put(moleculeId, count);
        moleculesVersion++;
    }

    /** Vue en lecture seule des molécules créées (identifiant → nombre), pour la sauvegarde. */
    public Map<String, Integer> molecules() {
        return Map.copyOf(molecules);
    }

    /**
     * Les molécules créées, une entrée par molécule, dans l'ordre de leur création : c'est l'ordre
     * dans lequel elles ont pris place dans l'espace.
     */
    public List<String> moleculeLog() {
        return moleculeLog;
    }

    /** Nombre de molécules créées, toutes sortes confondues. Peut dépasser deux milliards. */
    public long moleculeTotal() {
        return logKinds.isEmpty() ? 0 : logEnds[logKinds.size() - 1];
    }

    /** La sorte de la molécule créée au rang {@code index} (à partir de 0), dans l'ordre de création. */
    public String moleculeAt(long index) {
        if (index < 0 || index >= moleculeTotal()) throw new IndexOutOfBoundsException("Molécule n° " + index + " sur " + moleculeTotal());
        // La première suite qui finit après ce rang.
        int low = 0;
        int high = logKinds.size() - 1;
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (logEnds[middle] > index) high = middle;
            else low = middle + 1;
        }
        return logKinds.get(low);
    }

    /**
     * Les molécules créées vues une à une, alors qu'elles sont gardées par suites. En lecture seule.
     * Une liste ne compte pas au-delà de deux milliards : passé ce nombre, elle s'arrête là, et
     * {@link #moleculeTotal()} et {@link #moleculeAt(long)} donnent le vrai compte.
     */
    private final class MoleculeLog extends java.util.AbstractList<String> {

        @Override
        public int size() {
            return (int) Math.min(Integer.MAX_VALUE, moleculeTotal());
        }

        @Override
        public String get(int index) {
            if (index < 0 || index >= size()) throw new IndexOutOfBoundsException("Molécule n° " + index + " sur " + size());
            return moleculeAt(index);
        }

        @Override
        public java.util.Iterator<String> iterator() {
            return new java.util.Iterator<>() {
                private int run = 0;
                private long index = 0;

                @Override
                public boolean hasNext() {
                    return index < size();
                }

                @Override
                public String next() {
                    if (!hasNext()) throw new java.util.NoSuchElementException();
                    while (logEnds[run] <= index) run++;
                    index++;
                    return logKinds.get(run);
                }
            };
        }
    }

    /**
     * Vrai si les molécules de cette sorte sont rassemblées en leur substance (gaz, liquide ou
     * solide). Ni l'explosion ni le Big Bang ne défont un rassemblement.
     */
    public boolean hasSubstance(String moleculeId) {
        return substances.contains(moleculeId);
    }

    /** Note les molécules de cette sorte comme rassemblées. */
    public void addSubstance(String moleculeId) {
        if (substances.add(moleculeId)) {
            moleculesVersion++;
            substancesSeen = null;
        }
    }

    /** Les sortes de molécules rassemblées, dans l'ordre où elles l'ont été. */
    public List<String> substances() {
        if (substancesSeen == null || substancesSeen.size() != substances.size()) substancesSeen = List.copyOf(substances);
        return substancesSeen;
    }

    /** Vrai si cet assemblage est formé. Ni l'explosion ni le Big Bang ne le défont. */
    public boolean hasAssembly(String assemblyId) {
        return assemblies.contains(assemblyId);
    }

    /** Note un assemblage comme formé. */
    public void addAssembly(String assemblyId) {
        if (assemblies.add(assemblyId)) {
            moleculesVersion++;
            assembliesSeen = null;
        }
    }

    /** Les assemblages formés, dans l'ordre où ils l'ont été. */
    public List<String> assemblies() {
        if (assembliesSeen == null || assembliesSeen.size() != assemblies.size()) assembliesSeen = List.copyOf(assemblies);
        return assembliesSeen;
    }

    /** Vrai si cet astre est formé. Ni l'explosion ni le Big Bang ne le défont. */
    public boolean hasBody(String bodyId) {
        return bodies.contains(bodyId);
    }

    /** Note un astre comme formé. */
    public void addBody(String bodyId) {
        if (bodies.add(bodyId)) {
            moleculesVersion++;
            bodiesSeen = null;
        }
    }

    /** Les systèmes solaires, dans l'ordre de leur fondation. */
    public List<SolarSystem> solarSystems() {
        return Collections.unmodifiableList(solarSystems);
    }

    /**
     * Le plan de l'arbre (version 0.084) : les voies et les liaisons que le jeu prend dès qu'elles
     * sont payables, dans cet ordre. Vide : pas de plan.
     */
    public List<String> darkPlan() {
        return Collections.unmodifiableList(darkPlan);
    }

    public void setDarkPlan(List<String> ids) {
        darkPlan.clear();
        for (String id : ids) if (id != null && !id.isBlank() && !darkPlan.contains(id)) darkPlan.add(id);
    }

    /** Le plan de l'arbre gardé par « Sauvegarder », sous forme de code ; vide s'il n'y en a pas. */
    public String savedDarkPlan() {
        return savedDarkPlan(0);
    }

    /** Le code gardé dans cet emplacement, de 0 à {@link #PLAN_SLOTS} − 1 ; vide s'il n'y en a pas. */
    public String savedDarkPlan(int slot) {
        return slot < 0 || slot >= PLAN_SLOTS ? "" : savedDarkPlans.get(slot);
    }

    public void setSavedDarkPlan(int slot, String code) {
        if (slot >= 0 && slot < PLAN_SLOTS) savedDarkPlans.set(slot, code == null ? "" : code);
    }

    public void setSavedDarkPlan(String code) {
        setSavedDarkPlan(0, code);
    }

    /** Le plan des systèmes solaires (version 0.084) : ce que le jeu place dès que les astres sont mûrs et libres. */
    public List<SolarSystem> systemPlan() {
        return Collections.unmodifiableList(systemPlan);
    }

    public void setSystemPlan(List<SolarSystem> systems) {
        systemPlan.clear();
        systemPlan.addAll(systems);
    }

    /** Le plan des systèmes gardé par « Sauvegarder », sous forme de code ; vide s'il n'y en a pas. */
    public String savedSystemPlan() {
        return savedSystemPlan(0);
    }

    /** La disposition gardée dans cet emplacement ; vide s'il n'y en a pas. */
    public String savedSystemPlan(int slot) {
        return slot < 0 || slot >= PLAN_SLOTS ? "" : savedSystemPlans.get(slot);
    }

    public void setSavedSystemPlan(int slot, String code) {
        if (slot >= 0 && slot < PLAN_SLOTS) savedSystemPlans.set(slot, code == null ? "" : code);
    }

    public void setSavedSystemPlan(String code) {
        setSavedSystemPlan(0, code);
    }

    /** Les astres planifiés ensemble (version 0.085), trois au plus, dans l'ordre du choix. */
    public List<String> plannedBodies() {
        return Collections.unmodifiableList(plannedBodies);
    }

    public void setPlannedBodies(List<String> ids) {
        plannedBodies.clear();
        for (String id : ids) if (id != null && !id.isBlank() && !plannedBodies.contains(id)) plannedBodies.add(id);
    }

    /** La valeur telle quelle si elle n'est pas négative ; {@link IllegalArgumentException} avec {@code message} sinon. */
    static BigNum nonNegative(BigNum value, String message) {
        if (value.sign() < 0) throw new IllegalArgumentException(message);
        return value;
    }

}
