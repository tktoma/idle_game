package idle.core;

import idle.core.collider.ColliderGenerator;
import idle.core.collider.ColliderUpgrade;
import idle.core.collider.Collision;
import idle.core.collider.CollisionTier;
import idle.core.collider.Quark;
import idle.core.elements.ElementCategory;
import idle.core.math.BigNum;
import idle.core.probes.Probe;
import idle.core.probes.ProbeDestination;
import idle.core.probes.ProbeReward;
import idle.core.probes.ProbeTrip;
import idle.core.probes.ProbeUpgrade;
import idle.core.save.SaveData;
import idle.core.sky.Cosmos;
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
 * Tout ce qui change pendant une partie, et rien d'autre.
 * C'est cet objet qui est écrit dans la sauvegarde ({@link SaveCodec}).
 *
 * <p>Aucune règle du jeu ici : les règles sont dans {@link Game}.
 */
public final class GameState {

    private boolean started = false;
    private BigNum particles = BigNum.ZERO;
    private BigNum atoms = BigNum.ZERO;
    private BigNum totalAtoms = BigNum.ZERO;
    private double timeSinceFusion = 0;
    private final List<Double> formations = new ArrayList<>();
    private double timePlayed = 0;
    private final Map<String, Integer> upgradeLevels = new HashMap<>();
    private final Set<String> ownedAutomations = new HashSet<>();
    private final Set<String> enabledAutomations = new HashSet<>();
    private final Map<String, Integer> automationSpeedLevels = new HashMap<>();
    private final Map<String, Double> automationTimers = new HashMap<>();
    private final Map<Integer, Integer> elements = new TreeMap<>();
    private int elementsVersion = 0;
    /** Augmente à chaque changement des niveaux de l'arbre de matière noire : le jeu s'en sert pour ne recalculer leurs effets qu'alors. */
    private int darkVersion = 0;
    private int synthesisCount = 0;
    private ElementCategory synthesisTarget = null;
    private int synthesisTries = 0;
    /** Nombre de synthèses de suite qui n'ont donné aucun élément nouveau : au-delà d'un seuil, la suivante en donne un. */
    private int drySyntheses = 0;
    private BigNum darkMatter = BigNum.ZERO;
    private BigNum darkMatterSpent = BigNum.ZERO;
    private BigNum darkMatterSize = Game.DARK_MATTER_START_SIZE;
    private int explosions = 0;
    private int tableWeightLevel = 0;
    private final Map<String, Integer> darkUpgradeLevels = new HashMap<>();
    private int fusionThreshold = 0;
    private BigNum synthesisReserve = BigNum.ZERO;
    private boolean holdLocked = false;
    private String activeChallenge = null;
    private final Set<String> completedChallenges = new HashSet<>();
    private final Set<String> completedChallengesView = Collections.unmodifiableSet(completedChallenges);
    /** Augmente à chaque changement des défis réussis : le jeu s'en sert pour ne recalculer leurs récompenses qu'alors. */
    private int challengesVersion = 0;
    private final Map<String, Double> challengeTimes = new HashMap<>();
    private double decayDebt = 0;
    private final Set<String> achievements = new java.util.LinkedHashSet<>();
    /** Change à chaque modification des succès : {@link Game} y garde son compte sans reparcourir la liste. Non sauvegardé. */
    private int achievementsVersion = 0;
    private final Set<String> enabledDarkAutomations = new HashSet<>();
    private final Map<String, Double> darkAutomationTimers = new HashMap<>();
    private int bigBangs = 0;
    private final Map<String, Integer> molecules = new HashMap<>();
    /**
     * Les molécules dans l'ordre de leur création, par suites : la sorte de chaque suite, et le rang
     * où elle finit. Une création ajoute des centaines de molécules d'un coup ; on ne garde donc pas
     * une entrée par molécule.
     */
    private final List<String> logKinds = new ArrayList<>();
    private long[] logEnds = new long[16];
    private final List<String> moleculeLog = new MoleculeLog();
    private int moleculesVersion = 0;
    private final Set<String> substances = new java.util.LinkedHashSet<>();
    private final Set<String> assemblies = new java.util.LinkedHashSet<>();
    private final Set<String> bodies = new java.util.LinkedHashSet<>();
    /** Les systèmes solaires (version 0.54). */
    private final List<SolarSystem> solarSystems = new ArrayList<>();
    /** Les astres formés qui mûrissent encore (version 0.53), avec les secondes qu'il leur reste. */
    private final Map<String, Double> maturing = new java.util.LinkedHashMap<>();
    /**
     * Les dernières listes rendues par {@link #substances()}, {@link #assemblies()} et {@link #bodies()} :
     * elles sont demandées bien plus souvent qu'elles ne changent, et ne sont recopiées qu'après un changement.
     */
    /** Nombre d'échelles du cosmos formées, dans l'ordre de {@link Cosmos} : 0 = aucune, 1 = la galaxie, 2 = l'amas de galaxies, 3 = l'univers. */
    private int cosmos;
    /** Vrai tant que l'appui automatique sur la matière noire est en marche, une fois acquis ({@link Game#isAutoHolding()}). */
    private boolean autoHold = true;
    /** Vrai tant que la création automatique des molécules est en marche, une fois acquise ({@link Game#isAutoCreatingMolecules()}). */
    private boolean autoMolecules = true;
    /** La création automatique suit aussi la liste de courses : elle crée ce que le prochain pas demande. */
    private boolean autoFollowList = true;
    /** Option facultative : entretien des substances utiles aux besoins de matière du prochain objectif. */
    private boolean autoFollowMatter = false;
    /** Les sortes de molécules que la création automatique entretient, dans l'ordre où le joueur les a choisies. */
    private final Set<String> automatedMolecules = new java.util.LinkedHashSet<>();
    private List<String> substancesSeen;
    private List<String> assembliesSeen;
    private List<String> bodiesSeen;
    private final Set<String> spaceUpgrades = new java.util.LinkedHashSet<>();
    /** Vrai tant que le joueur a coupé d'un geste tous les automatismes : aucun n'agit, et chacun garde son propre réglage. */
    private boolean automationPaused = false;
    /** Vrai tant que le rassemblement automatique est en marche, une fois acquis ({@link Game#isAutoGathering()}). */
    private boolean autoGather = true;
    /** Vrai tant que la formation automatique des assemblages et des astres est en marche, une fois acquise ({@link Game#isAutoForming()}). */
    private boolean autoForm = true;
    /** Part de l'espace créé que la création automatique laisse toujours libre (0,25 pour un quart). */
    private double autoMoleculeReserve = 0;
    /** Durée de l'explosion la plus rapide qui a valu une prime depuis le dernier Big Bang, en secondes ; 0 tant qu'il n'y en a pas. */
    private double explosionRecord = 0;
    // La comète : le temps avant la prochaine, le temps qu'elle reste à l'écran, le temps que dure encore son sillage.
    private double cometWait = 0;
    private double cometVisible = 0;
    private double cometBoost = 0;
    private int cometsCaught = 0;
    /** Vrai une fois l'écran de fin vu (version 0.68) : il ne s'ouvre qu'une fois par partie. */
    private boolean endSeen = false;
    // La partie secondaire (version 0.81), sans logique pour l'instant : les univers créés après le
    // premier, les graines d'univers, l'énergie du vide, et le niveau des améliorations de l'univers.
    private int extraUniverses = 0;
    private boolean universeFormed = false;
    // Les plans de l'arbre et des systèmes solaires (version 0.84).
    private final List<String> darkPlan = new ArrayList<>();
    /** Les emplacements de sauvegarde des plans (version 0.86) : {@link #PLAN_SLOTS} chacun, vides au départ. */
    public static final int PLAN_SLOTS = 5;
    private final List<String> savedDarkPlans = emptySlots();
    private final List<SolarSystem> systemPlan = new ArrayList<>();
    private final List<String> savedSystemPlans = emptySlots();

    /** Relit des emplacements ; une sauvegarde de la 0.84 ou 0.85 n'en avait qu'un, sous l'ancienne clé. */
    private static void readSlots(SaveData data, String key, String oldKey, List<String> slots) {
        java.util.Collections.fill(slots, "");
        List<String> read = data.has(key) ? data.list(key) : List.of(String.join("", data.list(oldKey)));
        for (int i = 0; i < Math.min(PLAN_SLOTS, read.size()); i++) slots.set(i, read.get(i) == null ? "" : read.get(i));
    }

    private static List<String> emptySlots() {
        return new ArrayList<>(java.util.Collections.nCopies(PLAN_SLOTS, ""));
    }
    private boolean forkResetArmed = false;
    // Planifier la matière (version 0.85).
    private final List<String> plannedBodies = new ArrayList<>();
    private final Map<String, Double> stateCaps = new TreeMap<>();
    private boolean recipeReservation = false;
    private BigNum universeSeeds = BigNum.ZERO;
    private BigNum voidEnergy = BigNum.ZERO;
    private final Map<String, Integer> universeUpgrades = new HashMap<>();
    /** Charges d'impulsion en réserve (fractions comprises) et impulsions données depuis le début. */
    private double pulseCharges = 0;
    // Le collisionneur de quarks (version 0.50) : rien ne s'y perd à l'explosion ni au Big Bang.
    private double colliderEnergy = 0;
    /** Les générateurs du collisionneur (version 0.57) : ce qu'on en a, et combien ont été achetés. */
    private final java.util.EnumMap<ColliderGenerator, Double> colliderGenerators = new java.util.EnumMap<>(ColliderGenerator.class);
    private final java.util.EnumMap<ColliderGenerator, Integer> colliderGeneratorsBought = new java.util.EnumMap<>(ColliderGenerator.class);
    /**
     * Les faisceaux (version 0.59) : leur phase, de 0 à 1 (ils se croisent à 0,5), le nombre de
     * croisements depuis le début, et le numéro du dernier croisement capté d'un clic.
     */
    private double colliderPhase = 0;
    private long colliderCrossings = 0;
    private long colliderCaught = -1;
    /** Les améliorations du collisionneur (version 0.59) et les automates mis en pause par le joueur. */
    private final java.util.EnumMap<ColliderUpgrade, Integer> colliderUpgrades = new java.util.EnumMap<>(ColliderUpgrade.class);
    private final java.util.EnumSet<ColliderUpgrade> colliderPaused = java.util.EnumSet.noneOf(ColliderUpgrade.class);
    private final java.util.EnumMap<Quark, Integer> quarks = new java.util.EnumMap<>(Quark.class);
    /** L'énergie produite depuis le début (version 0.62) : elle ouvre les deux trous noirs. */
    private double colliderEnergyProduced = 0;
    private final List<Collision> collisions = new ArrayList<>();
    private long collisionRuns = 0;
    private double topLeft = 0;
    private double overheatLeft = 0;
    // Les sondes (version 0.52) : comme le collisionneur, elles traversent l'explosion et le Big Bang.
    private final List<Probe> probes = new ArrayList<>();
    private final List<ProbeReward> probeRewards = new ArrayList<>();
    private final java.util.EnumMap<ProbeUpgrade, Integer> probeUpgrades = new java.util.EnumMap<>(ProbeUpgrade.class);
    private long probeRuns = 0;
    /** Numéros donnés aux sondes au départ. */
    private long probeLaunches = 0;
    private long probeSamples = 0;
    private double probeBoostLeft = 0;
    private long pulses = 0;
    /** Les records du joueur, étape par étape : le temps de jeu le plus court auquel chacune a été atteinte. La remise à zéro les garde. */
    private final Map<String, Double> records = new HashMap<>();
    // Les défis de Big Bang : celui en cours, ceux qui sont réussis, et leur meilleur temps.
    private String activeBangChallenge = null;
    private final Set<String> completedBangChallenges = new HashSet<>();
    private final Map<String, Double> bangChallengeTimes = new HashMap<>();
    /** Le temps de jeu auquel le défi de Big Bang en cours a commencé. */
    private double bangChallengeStarted = 0;
    /** L'ordre dans lequel l'automatisme achète les améliorations en atomes, quand le joueur a choisi le sien. */
    private final List<String> atomUpgradeOrder = new ArrayList<>();
    private boolean atomUpgradeOrdered = false;
    /** L'ordre dans lequel l'automatisme achète les automatismes ordinaires et leurs cadences, quand le joueur a choisi le sien. */
    private final List<String> automationOrder = new ArrayList<>();
    private boolean automationOrdered = false;
    /** Les molécules épinglées par le joueur, dans l'ordre où il les a choisies. */
    private final Set<String> favoriteMolecules = new java.util.LinkedHashSet<>();
    private BigNum space = BigNum.ZERO;
    private GameStats stats = new GameStats();

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
        if (particles.sign() < 0) throw new IllegalArgumentException("Le nombre de particules ne peut pas être négatif");
        this.particles = particles;
    }

    /** Atomes disponibles : la deuxième ressource, gagnée par fusion et dépensée en améliorations. */
    public BigNum atoms() {
        return atoms;
    }

    public void setAtoms(BigNum atoms) {
        if (atoms.sign() < 0) throw new IllegalArgumentException("Le nombre d'atomes ne peut pas être négatif");
        this.atoms = atoms;
    }

    /** Atomes créés depuis le début du jeu, dépensés ou non. Ne diminue jamais. */
    public BigNum totalAtoms() {
        return totalAtoms;
    }

    public void setTotalAtoms(BigNum totalAtoms) {
        if (totalAtoms.sign() < 0) throw new IllegalArgumentException("Le nombre d'atomes ne peut pas être négatif");
        this.totalAtoms = totalAtoms;
    }

    /** Temps écoulé depuis la dernière fusion (ou depuis le démarrage), en secondes. */
    public double timeSinceFusion() {
        return timeSinceFusion;
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
        if (darkMatter.sign() < 0) throw new IllegalArgumentException("La quantité de matière noire ne peut pas être négative");
        this.darkMatter = darkMatter;
    }

    /** Taille de la matière noire, en mètres : elle grossit tant que le joueur la maintient appuyée. */
    /**
     * Matière noire dépensée en améliorations depuis le début du jeu. Ajoutée à celle qui reste
     * ({@link #darkMatter()}), elle donne la matière noire gagnée en tout.
     */
    public BigNum darkMatterSpent() {
        return darkMatterSpent;
    }

    public void setDarkMatterSpent(BigNum darkMatterSpent) {
        if (darkMatterSpent.sign() < 0) throw new IllegalArgumentException("La matière noire dépensée ne peut pas être négative");
        this.darkMatterSpent = darkMatterSpent;
    }

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
     * Le plan de l'arbre (version 0.84) : les voies et les liaisons que le jeu prend dès qu'elles
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

    /** Le plan des systèmes solaires (version 0.84) : ce que le jeu place dès que les astres sont mûrs et libres. */
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

    /** Les astres planifiés ensemble (version 0.85), trois au plus, dans l'ordre du choix. */
    public List<String> plannedBodies() {
        return Collections.unmodifiableList(plannedBodies);
    }

    public void setPlannedBodies(List<String> ids) {
        plannedBodies.clear();
        for (String id : ids) if (id != null && !id.isBlank() && !plannedBodies.contains(id)) plannedBodies.add(id);
    }

    /**
     * Le plafond de la création automatique pour un état de la matière (version 0.85) : la part de
     * l'espace que ses molécules peuvent occuper, de 0 à 1 ; 1 (sans plafond) s'il n'est pas réglé.
     */
    public double stateCap(String matter) {
        return stateCaps.getOrDefault(matter, 1.0);
    }

    /** Règle un plafond ; 1 ou plus l'efface. */
    public void setStateCap(String matter, double cap) {
        if (Double.isNaN(cap) || cap >= 1) stateCaps.remove(matter);
        else stateCaps.put(matter, Math.max(0, cap));
    }

    /** Les plafonds réglés, état par état. */
    public Map<String, Double> stateCaps() {
        return Collections.unmodifiableMap(stateCaps);
    }

    /** Vrai si la réservation par recette est allumée (version 0.85). */
    public boolean recipeReservation() {
        return recipeReservation;
    }

    public void setRecipeReservation(boolean on) {
        recipeReservation = on;
    }

    /** Vrai si les bifurcations se rouvriront à la prochaine explosion (version 0.84). */
    public boolean forkResetArmed() {
        return forkResetArmed;
    }

    public void setForkResetArmed(boolean armed) {
        forkResetArmed = armed;
    }

    public void setSolarSystems(List<SolarSystem> systems) {
        solarSystems.clear();
        solarSystems.addAll(systems);
        moleculesVersion++;
    }

    /** Les astres qui mûrissent encore, avec les secondes qu'il leur reste, dans l'ordre de leur formation. */
    public Map<String, Double> maturing() {
        return Collections.unmodifiableMap(maturing);
    }

    /** Secondes de maturation qu'il reste à cet astre ; 0 s'il est mûr ou pas formé. */
    public double maturingLeft(String bodyId) {
        return maturing.getOrDefault(bodyId, 0.0);
    }

    /** Fixe ce qu'il reste à mûrir à un astre ; à 0, il est mûr, et ses bonus se recalculent. */
    public void setMaturing(String bodyId, double seconds) {
        boolean was = maturing.containsKey(bodyId);
        if (Double.isNaN(seconds) || seconds <= 0) {
            maturing.remove(bodyId);
        } else {
            maturing.put(bodyId, seconds);
        }
        if (was != maturing.containsKey(bodyId)) moleculesVersion++;
    }

    /** Les astres formés, dans l'ordre où ils l'ont été. */
    public List<String> bodies() {
        if (bodiesSeen == null || bodiesSeen.size() != bodies.size()) bodiesSeen = List.copyOf(bodies);
        return bodiesSeen;
    }

    /** Vrai si l'appui automatique est en marche (il ne sert qu'une fois acquis). Ni l'explosion ni le Big Bang ne le coupent. */
    public boolean autoHold() {
        return autoHold;
    }

    /** Met en marche ou coupe l'appui automatique. */
    public void setAutoHold(boolean enabled) {
        autoHold = enabled;
    }

    /** Vrai si la création automatique des molécules est en marche (elle ne sert qu'une fois acquise). Ni l'explosion ni le Big Bang ne la coupent. */
    public boolean autoFollowList() {
        return autoFollowList;
    }

    public void setAutoFollowList(boolean follow) {
        autoFollowList = follow;
    }

    public boolean autoFollowMatter() { return autoFollowMatter; }
    public void setAutoFollowMatter(boolean enabled) { autoFollowMatter = enabled; }

    public boolean autoMolecules() {
        return autoMolecules;
    }

    /** Met en marche ou coupe la création automatique des molécules. */
    public void setAutoMolecules(boolean enabled) {
        autoMolecules = enabled;
    }

    /** Vue en lecture seule des sortes que la création automatique entretient, dans l'ordre où elles ont été choisies. */
    public List<String> automatedMolecules() {
        return List.copyOf(automatedMolecules);
    }

    /** Vrai si la création automatique entretient cette sorte. */
    public boolean isMoleculeAutomated(String moleculeId) {
        return automatedMolecules.contains(moleculeId);
    }

    /** Confie une sorte à la création automatique, ou la lui retire. Ni l'explosion ni le Big Bang ne changent ce choix. */
    public void setMoleculeAutomated(String moleculeId, boolean automated) {
        if (moleculeId == null || moleculeId.isBlank()) throw new IllegalArgumentException("Molécule sans identifiant");
        if (automated) automatedMolecules.add(moleculeId);
        else automatedMolecules.remove(moleculeId);
    }

    /** Vrai si la galaxie est formée. Ni l'explosion ni le Big Bang ne la défont. */
    public boolean hasGalaxy() {
        return cosmos >= 1;
    }

    /** Note la galaxie comme formée, ou défaite : défaite, les échelles au-dessus le sont aussi. */
    public void setGalaxy(boolean formed) {
        setCosmosLevel(formed ? Math.max(cosmos, 1) : 0);
    }

    /** Nombre d'échelles du cosmos formées, dans l'ordre de {@link Cosmos} : 0 = aucune, 3 = jusqu'à l'univers. */
    public int cosmosLevel() {
        return cosmos;
    }

    /** Fixe le nombre d'échelles du cosmos formées. */
    public void setCosmosLevel(int level) {
        if (level < 0 || level > Cosmos.values().length) throw new IllegalArgumentException("Échelle invalide : " + level);
        if (cosmos != level) moleculesVersion++;
        cosmos = level;
    }

    /** Vrai si cette amélioration d'espace est acquise. Ni l'explosion ni le Big Bang ne la reprennent. */
    public boolean ownsSpaceUpgrade(String spaceUpgradeId) {
        return spaceUpgrades.contains(spaceUpgradeId);
    }

    /** Note une amélioration d'espace comme acquise. */
    public void addSpaceUpgrade(String spaceUpgradeId) {
        spaceUpgrades.add(spaceUpgradeId);
        moleculesVersion++;
    }

    /** Nombre d'améliorations d'espace acquises. */
    public int spaceUpgradeCount() {
        return spaceUpgrades.size();
    }

    /** Vrai tant que tous les automatismes sont coupés d'un geste ({@link Game#setAutomationPaused(boolean)}). Aucune remise à zéro ne change ce réglage. */
    public boolean automationPaused() {
        return automationPaused;
    }

    public void setAutomationPaused(boolean paused) {
        automationPaused = paused;
    }

    /** Vrai si le rassemblement automatique est en marche (il ne sert qu'une fois acquis). Aucune remise à zéro ne le coupe. */
    public boolean autoGather() {
        return autoGather;
    }

    public void setAutoGather(boolean enabled) {
        autoGather = enabled;
    }

    /** Vrai si la formation automatique est en marche (elle ne sert qu'une fois acquise). Aucune remise à zéro ne la coupe. */
    public boolean autoForm() {
        return autoForm;
    }

    public void setAutoForm(boolean enabled) {
        autoForm = enabled;
    }

    /** Part de l'espace créé que la création automatique laisse toujours libre, de 0 à 1. C'est un réglage. */
    public double autoMoleculeReserve() {
        return autoMoleculeReserve;
    }

    /** Durée de l'explosion qui sert de record depuis le dernier Big Bang, en secondes ; 0 tant qu'aucune n'a eu lieu. */
    public double explosionRecord() {
        return explosionRecord;
    }

    public void setExplosionRecord(double seconds) {
        explosionRecord = Double.isNaN(seconds) ? 0 : Math.max(0, seconds);
    }

    /** Secondes de jeu avant la prochaine comète ; 0 tant qu'aucune n'est attendue. */
    public double cometWait() {
        return cometWait;
    }

    public void setCometWait(double seconds) {
        cometWait = Double.isNaN(seconds) ? 0 : Math.max(0, seconds);
    }

    /** Secondes pendant lesquelles la comète reste encore à l'écran ; 0 quand il n'y en a pas. */
    public double cometVisible() {
        return cometVisible;
    }

    public void setCometVisible(double seconds) {
        cometVisible = Double.isNaN(seconds) ? 0 : Math.max(0, seconds);
    }

    /** Secondes pendant lesquelles le sillage de la dernière comète saisie accélère encore l'expansion. */
    public double cometBoost() {
        return cometBoost;
    }

    public void setCometBoost(double seconds) {
        cometBoost = Double.isNaN(seconds) ? 0 : Math.max(0, seconds);
    }

    /** Nombre de comètes saisies depuis le début du jeu. */
    public double colliderEnergy() {
        return colliderEnergy;
    }

    public void setColliderEnergy(double energy) {
        colliderEnergy = Double.isNaN(energy) ? 0 : Math.max(0, energy);
    }

    public double colliderGenerators(ColliderGenerator generator) {
        return colliderGenerators.getOrDefault(generator, 0.0);
    }

    public void setColliderGenerators(ColliderGenerator generator, double amount) {
        if (Double.isNaN(amount) || amount <= 0) colliderGenerators.remove(generator);
        else colliderGenerators.put(generator, amount);
    }

    public int colliderGeneratorsBought(ColliderGenerator generator) {
        return colliderGeneratorsBought.getOrDefault(generator, 0);
    }

    public void setColliderGeneratorsBought(ColliderGenerator generator, int bought) {
        if (bought <= 0) colliderGeneratorsBought.remove(generator);
        else colliderGeneratorsBought.put(generator, bought);
    }

    /** La phase des faisceaux, de 0 à 1 : ils partent ensemble de l'injecteur à 0 et se croisent à 0,5. */
    public double colliderPhase() {
        return colliderPhase;
    }

    public void setColliderPhase(double phase) {
        colliderPhase = Double.isNaN(phase) ? 0 : phase - Math.floor(phase);
    }

    /** Nombre de croisements des faisceaux depuis le début. */
    public long colliderCrossings() {
        return colliderCrossings;
    }

    public void setColliderCrossings(long count) {
        colliderCrossings = Math.max(0, count);
    }

    /** Le numéro du dernier croisement capté d'un clic ; −1 avant le premier. */
    public long colliderCaught() {
        return colliderCaught;
    }

    public void setColliderCaught(long crossing) {
        colliderCaught = crossing;
    }

    public int colliderUpgradeLevel(ColliderUpgrade upgrade) {
        return colliderUpgrades.getOrDefault(upgrade, 0);
    }

    public void setColliderUpgradeLevel(ColliderUpgrade upgrade, int level) {
        if (level <= 0) colliderUpgrades.remove(upgrade);
        else colliderUpgrades.put(upgrade, Math.min(level, upgrade.maxLevel()));
    }

    /** Vrai si le joueur a mis cet automate en pause. */
    public boolean isColliderPaused(ColliderUpgrade upgrade) {
        return colliderPaused.contains(upgrade);
    }

    public void setColliderPaused(ColliderUpgrade upgrade, boolean paused) {
        if (paused) colliderPaused.add(upgrade);
        else colliderPaused.remove(upgrade);
    }

    public int quarks(Quark quark) {
        return quarks.getOrDefault(quark, 0);
    }

    public void setQuarks(Quark quark, int count) {
        if (count <= 0) quarks.remove(quark);
        else quarks.put(quark, count);
    }

    public double colliderEnergyProduced() {
        return colliderEnergyProduced;
    }

    public void setColliderEnergyProduced(double energy) {
        colliderEnergyProduced = Double.isNaN(energy) ? 0 : Math.max(0, energy);
    }

    /** Les collisions en cours, dans l'ordre de leur lancement. */
    public List<Collision> collisions() {
        return Collections.unmodifiableList(collisions);
    }

    public void setCollisions(List<Collision> running) {
        collisions.clear();
        collisions.addAll(running);
    }

    public long collisionRuns() {
        return collisionRuns;
    }

    public void setCollisionRuns(long count) {
        collisionRuns = Math.max(0, count);
    }

    /** Secondes avant que le quark top ne se désintègre ; 0 s'il n'y en a pas. */
    public double topLeft() {
        return topLeft;
    }

    public void setTopLeft(double seconds) {
        topLeft = Double.isNaN(seconds) ? 0 : Math.max(0, seconds);
    }

    /** Secondes de surchauffe qu'il reste. */
    public double overheatLeft() {
        return overheatLeft;
    }

    public void setOverheatLeft(double seconds) {
        overheatLeft = Double.isNaN(seconds) ? 0 : Math.max(0, seconds);
    }

    /** Les sondes en vol, dans l'ordre de leur départ. */
    public List<Probe> probes() {
        return Collections.unmodifiableList(probes);
    }

    public void setProbes(List<Probe> flying) {
        probes.clear();
        probes.addAll(flying);
    }

    /** Les récompenses rapportées, qui attendent d'être réclamées, la prochaine en tête. */
    public List<ProbeReward> probeRewards() {
        return Collections.unmodifiableList(probeRewards);
    }

    public void setProbeRewards(List<ProbeReward> waiting) {
        probeRewards.clear();
        probeRewards.addAll(waiting);
    }

    public int probeUpgradeLevel(ProbeUpgrade upgrade) {
        return probeUpgrades.getOrDefault(upgrade, 0);
    }

    public void setProbeUpgradeLevel(ProbeUpgrade upgrade, int level) {
        if (level <= 0) probeUpgrades.remove(upgrade);
        else probeUpgrades.put(upgrade, Math.min(level, upgrade.maxLevel()));
    }

    /** Nombre de voyages terminés depuis le début : il fixe le butin de chacun, sans toucher au hasard du jeu. */
    public long probeRuns() {
        return probeRuns;
    }

    public void setProbeRuns(long runs) {
        probeRuns = Math.max(0, runs);
    }

    public long probeLaunches() {
        return probeLaunches;
    }

    public void setProbeLaunches(long launches) {
        probeLaunches = Math.max(0, launches);
    }

    public long probeSamples() {
        return probeSamples;
    }

    public void setProbeSamples(long samples) {
        probeSamples = Math.max(0, samples);
    }

    /** Secondes de sursaut d'expansion qu'il reste. */
    public double probeBoostLeft() {
        return probeBoostLeft;
    }

    public void setProbeBoostLeft(double seconds) {
        probeBoostLeft = Double.isNaN(seconds) ? 0 : Math.max(0, seconds);
    }

    public double pulseCharges() {
        return pulseCharges;
    }

    public void setPulseCharges(double charges) {
        pulseCharges = Double.isNaN(charges) ? 0 : Math.max(0, charges);
    }

    public long pulses() {
        return pulses;
    }

    public void setPulses(long count) {
        pulses = Math.max(0, count);
    }

    public int cometsCaught() {
        return cometsCaught;
    }

    /** Vrai une fois l'écran de fin vu : il ne s'ouvre qu'une fois par partie. */
    public boolean endSeen() {
        return endSeen;
    }

    public void setEndSeen(boolean seen) {
        endSeen = seen;
    }

    /**
     * Vrai une fois le premier univers formé (version 0.83) : il reste formé après la remise à
     * zéro qui l'accompagne ({@link #startUniverse()}), quand l'échelle du cosmos repart de rien.
     */
    public boolean universeFormed() {
        return universeFormed;
    }

    public void setUniverseFormed(boolean formed) {
        universeFormed = formed;
    }

    /**
     * Le premier univers se forme (version 0.83) : tout ce qui y a mené est effacé, comme pour une
     * nouvelle partie ({@link #reset()}). Restent les succès, le temps de jeu, les statistiques,
     * l'écran de fin déjà vu, et tout ce qui appartient à l'onglet Univers : les univers, leurs
     * graines, l'énergie du vide et les quatre améliorations. La partie reste commencée, pour que
     * l'écran de démarrage ne revienne pas.
     */
    public void startUniverse() {
        java.util.Set<String> keptAchievements = new java.util.LinkedHashSet<>(achievements);
        double played = timePlayed;
        GameStats keptStats = stats;
        boolean seen = endSeen;
        int extra = extraUniverses;
        BigNum seeds = universeSeeds;
        BigNum energy = voidEnergy;
        Map<String, Integer> upgrades = new TreeMap<>(universeUpgrades);
        List<String> treeCodes = new ArrayList<>(savedDarkPlans), systemsCodes = new ArrayList<>(savedSystemPlans);
        reset();
        // Les plans gardés par « Sauvegarder » (version 0.84) servent d'une partie à l'autre.
        for (int i = 0; i < PLAN_SLOTS; i++) {
            savedDarkPlans.set(i, treeCodes.get(i));
            savedSystemPlans.set(i, systemsCodes.get(i));
        }
        started = true;
        timePlayed = played;
        stats = keptStats;
        endSeen = seen;
        achievements.addAll(keptAchievements);
        achievementsVersion++;
        universeFormed = true;
        extraUniverses = extra;
        universeSeeds = seeds;
        voidEnergy = energy;
        universeUpgrades.putAll(upgrades);
    }

    /** Univers créés après le premier (version 0.81). */
    public int extraUniverses() {
        return extraUniverses;
    }

    public void setExtraUniverses(int count) {
        extraUniverses = Math.max(0, count);
    }

    /** Graines d'univers : la ressource principale de l'onglet Univers (version 0.81). */
    public BigNum universeSeeds() {
        return universeSeeds;
    }

    public void setUniverseSeeds(BigNum seeds) {
        universeSeeds = seeds == null || seeds.sign() < 0 ? BigNum.ZERO : seeds;
    }

    /** Énergie du vide : la ressource du deuxième acte de la partie secondaire, le Vide (version 0.81). */
    public BigNum voidEnergy() {
        return voidEnergy;
    }

    public void setVoidEnergy(BigNum energy) {
        voidEnergy = energy == null || energy.sign() < 0 ? BigNum.ZERO : energy;
    }

    /** Niveau d'une amélioration de l'univers. */
    public int universeUpgradeLevel(String id) {
        return universeUpgrades.getOrDefault(id, 0);
    }

    public void setUniverseUpgradeLevel(String id, int level) {
        if (level <= 0) universeUpgrades.remove(id);
        else universeUpgrades.put(id, level);
    }

    public void setCometsCaught(int count) {
        cometsCaught = Math.max(0, count);
    }

    /** Les records du joueur : nom d'une étape → temps de jeu le plus court auquel elle a été atteinte. */
    public Map<String, Double> records() {
        return Collections.unmodifiableMap(records);
    }

    /** Note un record ; un temps négatif ou qui n'est pas un nombre est refusé. */
    public void setRecord(String step, double seconds) {
        if (step == null || step.isBlank() || Double.isNaN(seconds) || seconds < 0) return;
        records.put(step, seconds);
    }

    /** Oublie tous les records : la seule chose que la remise à zéro du jeu ne fait pas d'elle-même. */
    public void clearRecords() {
        records.clear();
    }

    /** Le défi de Big Bang en cours, ou {@code null}. */
    public String activeBangChallenge() {
        return activeBangChallenge;
    }

    public void setActiveBangChallenge(String challenge) {
        activeBangChallenge = challenge == null || challenge.isBlank() ? null : challenge;
    }

    /** Les défis de Big Bang réussis. */
    public Set<String> completedBangChallenges() {
        return Collections.unmodifiableSet(completedBangChallenges);
    }

    public void addCompletedBangChallenge(String challenge) {
        if (challenge != null && !challenge.isBlank()) completedBangChallenges.add(challenge);
    }

    /** Le meilleur temps de chaque défi de Big Bang réussi, en secondes. */
    public Map<String, Double> bangChallengeTimes() {
        return Collections.unmodifiableMap(bangChallengeTimes);
    }

    /** Le temps de jeu auquel le défi de Big Bang en cours a commencé, en secondes. */
    public double bangChallengeStarted() {
        return bangChallengeStarted;
    }

    public void setBangChallengeStarted(double timePlayed) {
        bangChallengeStarted = Double.isNaN(timePlayed) ? 0 : Math.max(0, timePlayed);
    }

    public void setBangChallengeTime(String challenge, double seconds) {
        if (challenge == null || challenge.isBlank() || Double.isNaN(seconds) || seconds < 0) return;
        bangChallengeTimes.put(challenge, seconds);
    }

    public void setAutoMoleculeReserve(double share) {
        if (!(share >= 0 && share < 1)) throw new IllegalArgumentException("Part invalide : " + share);
        autoMoleculeReserve = share;
    }

    /** L'ordre d'achat choisi par le joueur pour les améliorations en atomes : des identifiants, du premier au dernier. */
    public List<String> atomUpgradeOrder() {
        return List.copyOf(atomUpgradeOrder);
    }

    public void setAtomUpgradeOrder(List<String> order) {
        atomUpgradeOrder.clear();
        atomUpgradeOrder.addAll(order);
    }

    /** Vrai si l'automatisme suit l'ordre du joueur plutôt que « le moins cher d'abord ». */
    public boolean atomUpgradeOrdered() {
        return atomUpgradeOrdered;
    }

    public void setAtomUpgradeOrdered(boolean ordered) {
        atomUpgradeOrdered = ordered;
    }

    /** L'ordre d'achat choisi par le joueur pour les automatismes ordinaires : des identifiants, du premier au dernier. */
    public List<String> automationOrder() {
        return List.copyOf(automationOrder);
    }

    public void setAutomationOrder(List<String> order) {
        automationOrder.clear();
        automationOrder.addAll(order);
    }

    /** Vrai si l'automatisme suit l'ordre du joueur plutôt que « le moins cher d'abord ». */
    public boolean automationOrdered() {
        return automationOrdered;
    }

    public void setAutomationOrdered(boolean ordered) {
        automationOrdered = ordered;
    }

    /** Vrai si le joueur a épinglé cette molécule. */
    public boolean isMoleculeFavorite(String moleculeId) {
        return favoriteMolecules.contains(moleculeId);
    }

    /** Épingle une molécule, ou la retire des favorites. Ni l'explosion ni le Big Bang ne changent ce choix. */
    public void setMoleculeFavorite(String moleculeId, boolean favorite) {
        if (moleculeId == null || moleculeId.isBlank()) throw new IllegalArgumentException("Molécule sans identifiant");
        if (favorite) favoriteMolecules.add(moleculeId);
        else favoriteMolecules.remove(moleculeId);
    }

    /** Vue en lecture seule des molécules épinglées, dans l'ordre où elles l'ont été. */
    public List<String> favoriteMolecules() {
        return List.copyOf(favoriteMolecules);
    }

    /** Vue en lecture seule des améliorations d'espace acquises, pour la sauvegarde. */
    public Set<String> spaceUpgrades() {
        return Set.copyOf(spaceUpgrades);
    }

    /** Nombre de sortes de molécules différentes créées. */
    public int moleculeKinds() {
        return molecules.size();
    }

    /** Change à chaque molécule ajoutée ou retirée : permet de ne recalculer ce qui en dépend que lorsqu'il le faut. */
    public int moleculesVersion() {
        return moleculesVersion;
    }

    /** Vrai si aucune molécule n'a été créée. */
    public boolean hasNoMolecule() {
        return molecules.isEmpty();
    }

    /** Espace : ce que l'expansion de la matière accumule avec le temps, depuis le premier Big Bang. */
    public BigNum space() {
        return space;
    }

    public void setSpace(BigNum space) {
        if (space.sign() < 0) throw new IllegalArgumentException("Espace négatif : " + space);
        this.space = space;
    }

    /** Nombre de Big Bangs déclenchés. Aucun Big Bang ne le remet à zéro. */
    public int bigBangs() {
        return bigBangs;
    }

    public void setBigBangs(int bigBangs) {
        if (bigBangs < 0) throw new IllegalArgumentException("Nombre de Big Bangs négatif : " + bigBangs);
        this.bigBangs = bigBangs;
    }

    /**
     * Nombre de générateurs que la fusion automatique attend avant de fusionner, choisi par le
     * joueur ; 0 tant qu'il n'a rien réglé. C'est un réglage : aucune explosion ne l'efface.
     */
    public int fusionThreshold() {
        return fusionThreshold;
    }

    public void setFusionThreshold(int fusionThreshold) {
        if (fusionThreshold < 0) throw new IllegalArgumentException("Seuil négatif");
        this.fusionThreshold = fusionThreshold;
    }

    /**
     * Atomes que la synthèse automatique laisse toujours au joueur. C'est un réglage : ni la
     * fusion ni l'explosion ne le changent.
     */
    public BigNum synthesisReserve() {
        return synthesisReserve;
    }

    public void setSynthesisReserve(BigNum synthesisReserve) {
        if (synthesisReserve.sign() < 0) throw new IllegalArgumentException("Réserve négative");
        this.synthesisReserve = synthesisReserve;
    }

    /** Vrai si l'appui sur la matière noire est verrouillé : elle grossit sans que le joueur tienne le clic. */
    public boolean holdLocked() {
        return holdLocked;
    }

    public void setHoldLocked(boolean holdLocked) {
        this.holdLocked = holdLocked;
    }

    /** Identifiant du défi en cours, ou {@code null} en partie ordinaire. */
    public String activeChallenge() {
        return activeChallenge;
    }

    public void setActiveChallenge(String activeChallenge) {
        this.activeChallenge = activeChallenge;
    }

    /** Vue en lecture seule des défis réussis. */
    public Set<String> completedChallenges() {
        return completedChallengesView;
    }

    public void addCompletedChallenge(String challengeId) {
        if (completedChallenges.add(challengeId)) challengesVersion++;
    }

    /** Version des défis réussis : change dès qu'un défi est ajouté ou que la liste est vidée. */
    public int challengesVersion() {
        return challengesVersion;
    }

    /** Meilleur temps de chaque défi réussi, en secondes de jeu (identifiant → durée de la partie). */
    public Map<String, Double> challengeTimes() {
        return Collections.unmodifiableMap(challengeTimes);
    }

    public void setChallengeTime(String challengeId, double seconds) {
        if (seconds < 0 || Double.isNaN(seconds)) throw new IllegalArgumentException("Durée invalide : " + seconds);
        challengeTimes.put(challengeId, seconds);
    }

    /** Vue en lecture seule des succès obtenus, dans l'ordre où ils l'ont été. Aucune explosion ne les reprend. */
    public Set<String> achievements() {
        return Collections.unmodifiableSet(achievements);
    }

    /** @return {@code true} si le succès vient d'être ajouté, faux s'il était déjà obtenu */
    public boolean addAchievement(String achievementId) {
        boolean added = achievements.add(achievementId);
        if (added) achievementsVersion++;
        return added;
    }

    /** Numéro de version des succès : il change dès qu'un succès est ajouté ou retiré. */
    public int achievementsVersion() {
        return achievementsVersion;
    }

    /** Fraction d'exemplaire qui attend de se désintégrer, pendant le défi de la désintégration. */
    public double decayDebt() {
        return decayDebt;
    }

    public void setDecayDebt(double decayDebt) {
        this.decayDebt = Math.max(0, decayDebt);
    }

    /** Vrai si le joueur a mis cet automatisme de matière noire en marche. Aucune explosion ne le coupe. */
    public boolean isDarkAutomationEnabled(String darkAutomationId) {
        return enabledDarkAutomations.contains(darkAutomationId);
    }

    public void setDarkAutomationEnabled(String darkAutomationId, boolean enabled) {
        if (enabled) {
            enabledDarkAutomations.add(darkAutomationId);
        } else {
            enabledDarkAutomations.remove(darkAutomationId);
        }
    }

    /** Vue en lecture seule des automatismes de matière noire en marche, pour la sauvegarde. */
    public Set<String> enabledDarkAutomations() {
        return Set.copyOf(enabledDarkAutomations);
    }

    /** Temps écoulé depuis la dernière action d'un automatisme de matière noire, en secondes. */
    public double darkAutomationTimer(String darkAutomationId) {
        return darkAutomationTimers.getOrDefault(darkAutomationId, 0.0);
    }

    public void setDarkAutomationTimer(String darkAutomationId, double seconds) {
        if (!(seconds >= 0)) throw new IllegalArgumentException("Durée invalide : " + seconds);
        darkAutomationTimers.put(darkAutomationId, seconds);
    }

    /** Niveau possédé pour une amélioration de matière noire (0 si jamais achetée). */
    public int darkLevelOf(String darkUpgradeId) {
        return darkUpgradeLevels.getOrDefault(darkUpgradeId, 0);
    }

    public void setDarkLevel(String darkUpgradeId, int level) {
        if (level < 0) throw new IllegalArgumentException("Niveau négatif");
        darkUpgradeLevels.put(darkUpgradeId, level);
        darkVersion++;
    }

    /** Vue en lecture seule des niveaux des améliorations de matière noire, pour la sauvegarde. */
    /** Version des niveaux de l'arbre : change dès qu'un niveau change. */
    public int darkVersion() {
        return darkVersion;
    }

    public Map<String, Integer> darkUpgradeLevels() {
        return Map.copyOf(darkUpgradeLevels);
    }

    /**
     * Efface absolument tout, matière noire, succès, Big Bangs et statistiques compris : l'état
     * redevient celui d'une partie jamais commencée.
     */
    public void reset() {
        clearMatter();
        clearDarkMatter();
        fusionThreshold = 0;
        holdLocked = false;
        enabledDarkAutomations.clear();
        started = false;
        timePlayed = 0;
        challengeTimes.clear();
        achievements.clear();
        achievementsVersion++;
        bigBangs = 0;
        molecules.clear();
        logKinds.clear();
        substances.clear();
        assemblies.clear();
        bodies.clear();
        maturing.clear();
        solarSystems.clear();
        substancesSeen = null;
        assembliesSeen = null;
        bodiesSeen = null;
        cosmos = 0;
        autoHold = true;
        autoMolecules = true;
        autoFollowList = true;
        autoFollowMatter = false;
        automatedMolecules.clear();
        spaceUpgrades.clear();
        automationPaused = false;
        favoriteMolecules.clear();
        autoGather = true;
        autoForm = true;
        autoMoleculeReserve = 0;
        explosionRecord = 0;
        cometWait = 0;
        cometVisible = 0;
        cometBoost = 0;
        cometsCaught = 0;
        endSeen = false;
        extraUniverses = 0;
        universeFormed = false;
        darkPlan.clear();
        java.util.Collections.fill(savedDarkPlans, "");
        systemPlan.clear();
        java.util.Collections.fill(savedSystemPlans, "");
        forkResetArmed = false;
        plannedBodies.clear();
        stateCaps.clear();
        recipeReservation = false;
        universeSeeds = BigNum.ZERO;
        voidEnergy = BigNum.ZERO;
        universeUpgrades.clear();
        pulseCharges = 0;
        pulses = 0;
        colliderEnergy = 0;
        colliderGenerators.clear();
        colliderGeneratorsBought.clear();
        colliderPhase = 0;
        colliderCrossings = 0;
        colliderCaught = -1;
        colliderUpgrades.clear();
        colliderPaused.clear();
        quarks.clear();
        colliderEnergyProduced = 0;
        collisions.clear();
        collisionRuns = 0;
        topLeft = 0;
        overheatLeft = 0;
        probes.clear();
        probeRewards.clear();
        probeUpgrades.clear();
        probeRuns = 0;
        probeLaunches = 0;
        probeSamples = 0;
        probeBoostLeft = 0;
        // Les records restent : ils sont ce que le joueur cherche à battre à la partie suivante.
        activeBangChallenge = null;
        completedBangChallenges.clear();
        bangChallengeTimes.clear();
        bangChallengeStarted = 0;
        atomUpgradeOrder.clear();
        atomUpgradeOrdered = false;
        automationOrder.clear();
        automationOrdered = false;
        moleculesVersion++;
        space = BigNum.ZERO;
        stats = new GameStats();
    }

    /**
     * Efface tout ce qui vient de l'explosion : la matière noire, gagnée comme dépensée, sa taille,
     * son arbre, la masse du tableau, le nombre d'explosions et les défis réussis. Les réglages du
     * joueur restent, pour resservir dès que l'arbre les rouvre : les automatismes de matière noire
     * qu'il avait mis en marche, le verrou de l'appui, le seuil de fusion. Seule la réserve de la
     * synthèse automatique repart de zéro : sous le plafond d'atomes revenu, elle la bloquerait.
     * Les records des défis restent, comme
     * les succès, le temps de jeu et les statistiques, et ce qui appartient à l'acte du Big Bang :
     * les molécules, les substances, les assemblages, les astres, l'espace et ses améliorations. C'est ce que fait un Big Bang, en plus de
     * {@link #clearMatter()}.
     */
    public void clearDarkMatter() {
        darkMatter = BigNum.ZERO;
        darkMatterSpent = BigNum.ZERO;
        darkMatterSize = Game.DARK_MATTER_START_SIZE;
        explosions = 0;
        tableWeightLevel = 0;
        darkUpgradeLevels.clear();
        darkVersion++;
        synthesisReserve = BigNum.ZERO;
        activeChallenge = null;
        completedChallenges.clear();
        challengesVersion++;
        darkAutomationTimers.clear();
    }

    /**
     * Comme {@link #clearDarkMatter()}, mais l'arbre reste : ses cases, la matière noire qu'elles
     * ont coûté, ses automatismes et les réglages qu'il avait ouverts. Partent la réserve, la
     * taille, la masse du tableau, le nombre d'explosions et les défis réussis. C'est ce que fait
     * un Big Bang une fois atteint le palier qui garde l'arbre.
     */
    public void clearDarkMatterKeepingTree() {
        darkMatter = BigNum.ZERO;
        darkMatterSize = Game.DARK_MATTER_START_SIZE;
        explosions = 0;
        tableWeightLevel = 0;
        activeChallenge = null;
        completedChallenges.clear();
        challengesVersion++;
        darkAutomationTimers.clear();
    }

    /**
     * Efface toute la matière : particules, atomes, améliorations, automatismes et éléments.
     * Il ne reste que le premier générateur, la matière noire, sa taille et ses améliorations,
     * le nombre d'explosions et le temps de jeu.
     */
    public void clearMatter() {
        particles = BigNum.ZERO;
        atoms = BigNum.ZERO;
        totalAtoms = BigNum.ZERO;
        timeSinceFusion = 0;
        formations.clear();
        upgradeLevels.clear();
        ownedAutomations.clear();
        enabledAutomations.clear();
        automationSpeedLevels.clear();
        automationTimers.clear();
        elements.clear();
        elementsVersion++;
        synthesisCount = 0;
        synthesisTarget = null;
        synthesisTries = 0;
        drySyntheses = 0;
        decayDebt = 0;
    }

    /**
     * Efface tout le collisionneur : son énergie, produite comme dépensée, ses générateurs, ses
     * améliorations et leurs pauses, les quarks, les collisions en cours, le quark top et la
     * surchauffe. Rien d'autre ne bouge. Aucune étape du jeu ne le fait (le collisionneur
     * traverse les actes) : c'est un outil de test.
     */
    public void clearCollider() {
        colliderEnergy = 0;
        colliderEnergyProduced = 0;
        colliderGenerators.clear();
        colliderGeneratorsBought.clear();
        colliderPhase = 0;
        colliderCrossings = 0;
        colliderCaught = -1;
        colliderUpgrades.clear();
        colliderPaused.clear();
        quarks.clear();
        collisions.clear();
        collisionRuns = 0;
        topLeft = 0;
        overheatLeft = 0;
    }

    /**
     * Efface l'acte du Big Bang : les Big Bangs, les molécules et leurs automatismes, les
     * substances, les assemblages, les astres, les systèmes solaires, l'espace et ses
     * améliorations, le cosmos, les comètes, les sondes et les défis du Big Bang. La matière, la
     * matière noire et le collisionneur restent ; les succès et les records aussi. Un outil de
     * test : aucune étape du jeu ne revient en arrière ainsi.
     */
    public void clearBigBangAct() {
        bigBangs = 0;
        plannedBodies.clear();
        molecules.clear();
        substances.clear();
        assemblies.clear();
        bodies.clear();
        maturing.clear();
        solarSystems.clear();
        substancesSeen = null;
        assembliesSeen = null;
        bodiesSeen = null;
        cosmos = 0;
        automatedMolecules.clear();
        spaceUpgrades.clear();
        favoriteMolecules.clear();
        cometWait = 0;
        cometVisible = 0;
        cometBoost = 0;
        probes.clear();
        probeRewards.clear();
        probeUpgrades.clear();
        probeRuns = 0;
        probeBoostLeft = 0;
        activeBangChallenge = null;
        completedBangChallenges.clear();
        bangChallengeStarted = 0;
        moleculesVersion++;
        space = BigNum.ZERO;
    }

    // ------------------------------------------------------------------
    // Sauvegarde
    // ------------------------------------------------------------------

    /**
     * Range toute la partie dans une sauvegarde. Ce qui se recalcule (les compteurs de version,
     * les listes gardées pour aller vite) n'y est pas ; les ensembles sans ordre sont triés, pour
     * que deux parties identiques donnent le même texte.
     *
     * <p>Tout champ ajouté à cette classe doit être écrit ici et relu dans {@link #load(SaveData)} :
     * un test compare champ par champ une partie à sa copie relue, et échoue sinon.
     */
    public void save(SaveData data) {
        data.put("started", started);
        data.put("particles", particles);
        data.put("atoms", atoms);
        data.put("totalAtoms", totalAtoms);
        data.put("timeSinceFusion", timeSinceFusion);
        data.putDoubleList("formations", formations);
        data.put("timePlayed", timePlayed);
        data.putInts("upgradeLevels", new TreeMap<>(upgradeLevels));
        data.putList("ownedAutomations", new java.util.TreeSet<>(ownedAutomations));
        data.putList("enabledAutomations", new java.util.TreeSet<>(enabledAutomations));
        data.putInts("automationSpeedLevels", new TreeMap<>(automationSpeedLevels));
        data.putDoubles("automationTimers", new TreeMap<>(automationTimers));
        Map<String, Integer> copies = new java.util.LinkedHashMap<>();
        elements.forEach((number, count) -> copies.put(String.valueOf(number), count));
        data.putInts("elements", copies);
        data.put("synthesisCount", synthesisCount);
        data.put("synthesisTarget", synthesisTarget == null ? null : synthesisTarget.name());
        data.put("synthesisTries", synthesisTries);
        data.put("drySyntheses", drySyntheses);
        data.put("darkMatter", darkMatter);
        data.put("darkMatterSpent", darkMatterSpent);
        data.put("darkMatterSize", darkMatterSize);
        data.put("explosions", explosions);
        data.put("tableWeightLevel", tableWeightLevel);
        data.putInts("darkUpgradeLevels", new TreeMap<>(darkUpgradeLevels));
        data.put("fusionThreshold", fusionThreshold);
        data.put("synthesisReserve", synthesisReserve);
        data.put("holdLocked", holdLocked);
        data.put("activeChallenge", activeChallenge == null ? null : SaveData.escape(activeChallenge));
        data.putList("completedChallenges", new java.util.TreeSet<>(completedChallenges));
        data.putDoubles("challengeTimes", new TreeMap<>(challengeTimes));
        data.put("decayDebt", decayDebt);
        data.putList("achievements", achievements);
        data.putList("enabledDarkAutomations", new java.util.TreeSet<>(enabledDarkAutomations));
        data.putDoubles("darkAutomationTimers", new TreeMap<>(darkAutomationTimers));
        data.put("bigBangs", bigBangs);
        // Les molécules, par suites dans l'ordre de leur création : « sorte:nombre ». Le total par sorte s'en déduit.
        StringBuilder log = new StringBuilder();
        for (int run = 0; run < logKinds.size(); run++) {
            if (run > 0) log.append(',');
            log.append(SaveData.escape(logKinds.get(run))).append(':').append(logEnds[run] - (run == 0 ? 0 : logEnds[run - 1]));
        }
        data.put("moleculeLog", log.toString());
        data.putList("substances", substances);
        data.putList("assemblies", assemblies);
        data.putList("bodies", bodies);
        data.putDoubles("maturing", new TreeMap<>(maturing));
        List<String> systems = new ArrayList<>();
        for (SolarSystem system : solarSystems) {
            systems.add(system.star() + "|" + String.join(",", system.planets()) + "|" + String.join(",", system.moons()));
        }
        data.putList("solarSystems", systems);
        data.put("cosmos", cosmos);
        data.put("autoHold", autoHold);
        data.put("autoMolecules", autoMolecules);
        data.put("autoFollowList", autoFollowList);
        data.put("autoFollowMatter", autoFollowMatter);
        data.putList("automatedMolecules", automatedMolecules);
        data.putList("spaceUpgrades", spaceUpgrades);
        data.putList("darkPlan", darkPlan);
        data.putList("savedDarkPlans", savedDarkPlans);
        List<String> plannedSystems = new ArrayList<>();
        for (SolarSystem system : systemPlan) plannedSystems.add(system.line());
        data.putList("systemPlan", plannedSystems);
        data.putList("savedSystemPlans", savedSystemPlans);
        data.put("forkResetArmed", forkResetArmed);
        data.putList("plannedBodies", plannedBodies);
        data.putDoubles("stateCaps", new TreeMap<>(stateCaps));
        data.put("recipeReservation", recipeReservation);
        data.put("automationPaused", automationPaused);
        data.putList("favoriteMolecules", favoriteMolecules);
        data.put("autoGather", autoGather);
        data.put("autoForm", autoForm);
        data.put("autoMoleculeReserve", autoMoleculeReserve);
        data.put("explosionRecord", explosionRecord);
        data.put("cometWait", cometWait);
        data.put("cometVisible", cometVisible);
        data.put("cometBoost", cometBoost);
        data.put("cometsCaught", cometsCaught);
        data.put("endSeen", endSeen);
        data.put("universeFormed", universeFormed);
        data.put("extraUniverses", extraUniverses);
        data.put("universeSeeds", universeSeeds);
        data.put("voidEnergy", voidEnergy);
        data.putInts("universeUpgrades", new TreeMap<>(universeUpgrades));
        data.put("pulseCharges", pulseCharges);
        data.put("pulses", pulses);
        data.put("colliderEnergy", colliderEnergy);
        Map<String, Double> generatorAmounts = new TreeMap<>();
        Map<String, Integer> generatorsBought = new TreeMap<>();
        colliderGenerators.forEach((generator, amount) -> generatorAmounts.put(generator.name(), amount));
        colliderGeneratorsBought.forEach((generator, bought) -> generatorsBought.put(generator.name(), bought));
        data.putDoubles("colliderGenerators", generatorAmounts);
        data.putInts("colliderGeneratorsBought", generatorsBought);
        data.put("colliderPhase", colliderPhase);
        data.put("colliderCrossings", colliderCrossings);
        data.put("colliderCaught", colliderCaught);
        Map<String, Integer> colliderLevels = new TreeMap<>();
        colliderUpgrades.forEach((upgrade, level) -> colliderLevels.put(upgrade.name(), level));
        data.putInts("colliderUpgrades", colliderLevels);
        List<String> paused = new ArrayList<>();
        for (ColliderUpgrade upgrade : colliderPaused) paused.add(upgrade.name());
        data.putList("colliderPaused", paused);
        Map<String, Integer> quarkCounts = new TreeMap<>();
        quarks.forEach((quark, count) -> quarkCounts.put(quark.name(), count));
        data.putInts("quarks", quarkCounts);
        data.put("colliderEnergyProduced", colliderEnergyProduced);
        List<String> running = new ArrayList<>();
        for (Collision collision : collisions) {
            running.add(collision.tier().name() + ";" + collision.left() + ";" + collision.resonated() + ";" + collision.doubled());
        }
        data.putList("collisions", running);
        data.put("collisionRuns", collisionRuns);
        data.put("topLeft", topLeft);
        data.put("overheatLeft", overheatLeft);
        List<String> flying = new ArrayList<>();
        for (Probe probe : probes) {
            flying.add(probe.destination().name() + ";" + probe.trip().name() + ";" + probe.left() + ";" + probe.total() + ";" + probe.id());
        }
        data.putList("probes", flying);
        List<String> waiting = new ArrayList<>();
        for (ProbeReward reward : probeRewards) {
            waiting.add(reward.type().name() + ";" + reward.target() + ";" + reward.value() + ";" + reward.destination().name() + ";" + reward.probe());
        }
        data.putList("probeRewards", waiting);
        Map<String, Integer> upgradeLevels = new TreeMap<>();
        probeUpgrades.forEach((upgrade, level) -> upgradeLevels.put(upgrade.name(), level));
        data.putInts("probeUpgrades", upgradeLevels);
        data.put("probeRuns", probeRuns);
        data.put("probeLaunches", probeLaunches);
        data.put("probeSamples", probeSamples);
        data.put("probeBoostLeft", probeBoostLeft);
        data.putDoubles("records", new TreeMap<>(records));
        data.put("activeBangChallenge", activeBangChallenge == null ? null : SaveData.escape(activeBangChallenge));
        data.putList("completedBangChallenges", new java.util.TreeSet<>(completedBangChallenges));
        data.putDoubles("bangChallengeTimes", new TreeMap<>(bangChallengeTimes));
        data.put("bangChallengeStarted", bangChallengeStarted);
        data.putList("atomUpgradeOrder", atomUpgradeOrder);
        data.put("atomUpgradeOrdered", atomUpgradeOrdered);
        data.putList("automationOrder", automationOrder);
        data.put("automationOrdered", automationOrdered);
        data.put("space", space);
        stats.save(data, "stats.");
    }

    /**
     * Remplace toute la partie par celle d'une sauvegarde. Une clé absente laisse la valeur d'une
     * partie neuve : une sauvegarde plus ancienne que le jeu se relit donc sans rien casser.
     *
     * @throws RuntimeException si une valeur est illisible ou impossible (nombre négatif, texte à la
     *                          place d'un nombre) : l'état est alors à moitié rempli, et ne doit pas servir
     */
    public void load(SaveData data) {
        reset();
        started = data.flag("started", false);
        setParticles(data.big("particles", BigNum.ZERO));
        setAtoms(data.big("atoms", BigNum.ZERO));
        setTotalAtoms(data.big("totalAtoms", BigNum.ZERO));
        setTimeSinceFusion(data.real("timeSinceFusion", 0));
        List<Double> made = data.doubleList("formations");
        for (int generator = 0; generator < made.size(); generator++) setFormation(generator, made.get(generator));
        timePlayed = Math.max(0, data.real("timePlayed", 0));
        data.ints("upgradeLevels").forEach(this::setLevel);
        ownedAutomations.addAll(data.list("ownedAutomations"));
        enabledAutomations.addAll(data.list("enabledAutomations"));
        data.ints("automationSpeedLevels").forEach(this::setAutomationSpeedLevel);
        data.doubles("automationTimers").forEach(this::setAutomationTimer);
        data.ints("elements").forEach((number, count) -> setElementCount(Integer.parseInt(number), count));
        setSynthesisCount(data.whole("synthesisCount", 0));
        String target = data.text("synthesisTarget", null);
        synthesisTarget = target == null ? null : ElementCategory.valueOf(target);
        setSynthesisTries(data.whole("synthesisTries", 0));
        setDrySyntheses(data.whole("drySyntheses", 0));
        setDarkMatter(data.big("darkMatter", BigNum.ZERO));
        setDarkMatterSpent(data.big("darkMatterSpent", BigNum.ZERO));
        setDarkMatterSize(data.big("darkMatterSize", Game.DARK_MATTER_START_SIZE));
        setExplosions(data.whole("explosions", 0));
        setTableWeightLevel(data.whole("tableWeightLevel", 0));
        data.ints("darkUpgradeLevels").forEach(this::setDarkLevel);
        setFusionThreshold(data.whole("fusionThreshold", 0));
        setSynthesisReserve(data.big("synthesisReserve", BigNum.ZERO));
        holdLocked = data.flag("holdLocked", false);
        String challenge = data.text("activeChallenge", null);
        activeChallenge = challenge == null ? null : SaveData.unescape(challenge);
        completedChallenges.addAll(data.list("completedChallenges"));
        challengesVersion++;
        data.doubles("challengeTimes").forEach(this::setChallengeTime);
        setDecayDebt(data.real("decayDebt", 0));
        achievements.addAll(data.list("achievements"));
        achievementsVersion++;
        enabledDarkAutomations.addAll(data.list("enabledDarkAutomations"));
        data.doubles("darkAutomationTimers").forEach(this::setDarkAutomationTimer);
        setBigBangs(data.whole("bigBangs", 0));
        String log = data.text("moleculeLog", "");
        if (!log.isEmpty()) {
            for (String run : log.split(",", -1)) {
                int colon = run.lastIndexOf(':');
                addMolecules(SaveData.unescape(run.substring(0, colon)), Integer.parseInt(run.substring(colon + 1)));
            }
        }
        substances.addAll(data.list("substances"));
        assemblies.addAll(data.list("assemblies"));
        bodies.addAll(data.list("bodies"));
        solarSystems.clear();
        for (String line : data.list("solarSystems")) {
            String[] parts = line.split("\\|", -1);
            if (parts.length != 3 || parts[0].isBlank()) continue;
            solarSystems.add(new SolarSystem(parts[0], parts[1].isEmpty() ? List.of() : List.of(parts[1].split(",")),
                    parts[2].isEmpty() ? List.of() : List.of(parts[2].split(","))));
        }
        maturing.clear();
        data.doubles("maturing").forEach((id, left) -> {
            if (bodies.contains(id) && left > 0) maturing.put(id, left);
        });
        setCosmosLevel(data.whole("cosmos", 0));
        autoHold = data.flag("autoHold", true);
        autoMolecules = data.flag("autoMolecules", true);
        autoFollowList = data.flag("autoFollowList", true);
        autoFollowMatter = data.flag("autoFollowMatter", false);
        automatedMolecules.addAll(data.list("automatedMolecules"));
        spaceUpgrades.addAll(data.list("spaceUpgrades"));
        setDarkPlan(data.list("darkPlan"));
        readSlots(data, "savedDarkPlans", "savedDarkPlan", savedDarkPlans);
        systemPlan.clear();
        for (String line : data.list("systemPlan")) {
            SolarSystem system = SolarSystem.parse(line);
            if (system != null) systemPlan.add(system);
        }
        readSlots(data, "savedSystemPlans", "savedSystemPlan", savedSystemPlans);
        forkResetArmed = data.flag("forkResetArmed", false);
        setPlannedBodies(data.list("plannedBodies"));
        stateCaps.clear();
        data.doubles("stateCaps").forEach(this::setStateCap);
        recipeReservation = data.flag("recipeReservation", false);
        automationPaused = data.flag("automationPaused", false);
        favoriteMolecules.addAll(data.list("favoriteMolecules"));
        autoGather = data.flag("autoGather", true);
        autoForm = data.flag("autoForm", true);
        setAutoMoleculeReserve(data.real("autoMoleculeReserve", 0));
        setExplosionRecord(data.real("explosionRecord", 0));
        setCometWait(data.real("cometWait", 0));
        setCometVisible(data.real("cometVisible", 0));
        setCometBoost(data.real("cometBoost", 0));
        setCometsCaught(data.whole("cometsCaught", 0));
        // Une sauvegarde d'avant la 0.68 dont l'univers est déjà formé ne rouvre pas l'écran de fin.
        endSeen = data.flag("endSeen", data.whole("cosmos", 0) >= 3);
        // Une sauvegarde d'avant la 0.83 dont l'univers est formé : la remise à zéro se fera au chargement (Game.restored).
        universeFormed = data.flag("universeFormed", data.whole("cosmos", 0) >= 3);
        setExtraUniverses(data.whole("extraUniverses", 0));
        setUniverseSeeds(data.big("universeSeeds", BigNum.ZERO));
        setVoidEnergy(data.big("voidEnergy", BigNum.ZERO));
        universeUpgrades.clear();
        data.ints("universeUpgrades").forEach(this::setUniverseUpgradeLevel);
        setPulseCharges(data.real("pulseCharges", 0));
        setPulses((long) data.real("pulses", 0));
        setColliderEnergy(data.real("colliderEnergy", 0));
        colliderGenerators.clear();
        colliderGeneratorsBought.clear();
        data.doubles("colliderGenerators").forEach((name, amount) -> {
            try {
                setColliderGenerators(ColliderGenerator.valueOf(name), amount);
            } catch (IllegalArgumentException unknown) {
                // un générateur d'une autre version : écarté
            }
        });
        data.ints("colliderGeneratorsBought").forEach((name, bought) -> {
            try {
                setColliderGeneratorsBought(ColliderGenerator.valueOf(name), bought);
            } catch (IllegalArgumentException unknown) {
                // un générateur d'une autre version : écarté
            }
        });
        setColliderPhase(data.real("colliderPhase", 0));
        setColliderCrossings((long) data.real("colliderCrossings", 0));
        colliderCaught = (long) data.real("colliderCaught", -1);
        colliderUpgrades.clear();
        data.ints("colliderUpgrades").forEach((name, level) -> {
            try {
                setColliderUpgradeLevel(ColliderUpgrade.valueOf(name), level);
            } catch (IllegalArgumentException unknown) {
                // une amélioration d'une autre version : écartée
            }
        });
        colliderPaused.clear();
        for (String name : data.list("colliderPaused")) {
            try {
                colliderPaused.add(ColliderUpgrade.valueOf(name));
            } catch (IllegalArgumentException unknown) {
                // un automate d'une autre version : écarté
            }
        }
        quarks.clear();
        data.ints("quarks").forEach((name, count) -> {
            try {
                setQuarks(Quark.valueOf(name), count);
            } catch (IllegalArgumentException unknown) {
                // une saveur d'une autre version : écartée
            }
        });
        // Avant la version 0.62, l'énergie produite n'était pas comptée : l'énergie en réserve en tient lieu.
        setColliderEnergyProduced(data.real("colliderEnergyProduced", colliderEnergy));
        collisions.clear();
        for (String line : data.list("collisions")) {
            String[] parts = line.split(";");
            try {
                collisions.add(new Collision(CollisionTier.valueOf(parts[0]), Math.max(0, Double.parseDouble(parts[1])),
                        Boolean.parseBoolean(parts[2]), Boolean.parseBoolean(parts[3])));
            } catch (RuntimeException unreadable) {
                // une collision illisible : perdue, sans autre dommage
            }
        }
        setCollisionRuns((long) data.real("collisionRuns", 0));
        setTopLeft(data.real("topLeft", 0));
        setOverheatLeft(data.real("overheatLeft", 0));
        probes.clear();
        for (String line : data.list("probes")) {
            String[] parts = line.split(";");
            try {
                probes.add(new Probe(ProbeDestination.valueOf(parts[0]), ProbeTrip.valueOf(parts[1]),
                        Math.max(0, Double.parseDouble(parts[2])), Math.max(1, Double.parseDouble(parts[3])),
                        parts.length > 4 ? Long.parseLong(parts[4]) : -1));
            } catch (RuntimeException unreadable) {
                // une sonde illisible : perdue, sans autre dommage
            }
        }
        probeRewards.clear();
        for (String line : data.list("probeRewards")) {
            String[] parts = line.split(";", -1);
            try {
                probeRewards.add(new ProbeReward(ProbeReward.Type.valueOf(parts[0]), parts[1], Double.parseDouble(parts[2]),
                        ProbeDestination.valueOf(parts[3]), parts.length > 4 ? Long.parseLong(parts[4]) : -1));
            } catch (RuntimeException unreadable) {
                // une récompense illisible : perdue
            }
        }
        probeUpgrades.clear();
        data.ints("probeUpgrades").forEach((name, level) -> {
            try {
                setProbeUpgradeLevel(ProbeUpgrade.valueOf(name), level);
            } catch (IllegalArgumentException unknown) {
                // une amélioration d'une autre version : écartée
            }
        });
        setProbeRuns((long) data.real("probeRuns", 0));
        setProbeLaunches((long) data.real("probeLaunches", 0));
        setProbeSamples((long) data.real("probeSamples", 0));
        setProbeBoostLeft(data.real("probeBoostLeft", 0));
        records.clear();
        data.doubles("records").forEach(this::setRecord);
        String bangChallenge = data.text("activeBangChallenge", null);
        setActiveBangChallenge(bangChallenge == null ? null : SaveData.unescape(bangChallenge));
        data.list("completedBangChallenges").forEach(this::addCompletedBangChallenge);
        data.doubles("bangChallengeTimes").forEach(this::setBangChallengeTime);
        setBangChallengeStarted(data.real("bangChallengeStarted", 0));
        atomUpgradeOrder.addAll(data.list("atomUpgradeOrder"));
        atomUpgradeOrdered = data.flag("atomUpgradeOrdered", false);
        automationOrder.addAll(data.list("automationOrder"));
        automationOrdered = data.flag("automationOrdered", false);
        setSpace(data.big("space", BigNum.ZERO));
        stats.load(data, "stats.");
        substancesSeen = null;
        assembliesSeen = null;
        bodiesSeen = null;
        elementsVersion++;
        moleculesVersion++;
    }

    /**
     * Ce que le jeu connaît : les identifiants de ses catalogues. Une sauvegarde écrite par une autre
     * version du jeu peut en nommer d'autres ; {@link #retainKnown(Known)} les oublie.
     * Un catalogue vide n'écarte rien : c'est celui d'une partie de test, qui n'en a pas.
     */
    record Known(Set<String> upgrades, Set<String> automations, Set<String> darkUpgrades, Set<String> darkAutomations,
                 Set<String> challenges, Set<String> achievements, Set<String> molecules, Set<String> assemblies,
                 Set<String> bodies, Set<String> spaceUpgrades, int elements) {
    }

    /**
     * Oublie tout ce qui porte un identifiant que le jeu ne connaît pas : après une mise à jour,
     * une sauvegarde peut nommer une amélioration ou une molécule disparue, et les règles ne
     * sauraient qu'en faire.
     *
     * @return le nombre d'identifiants oubliés
     */
    int retainKnown(Known known) {
        int before = identifiers();
        if (!known.upgrades().isEmpty()) {
            upgradeLevels.keySet().retainAll(known.upgrades());
            atomUpgradeOrder.retainAll(known.upgrades());
        }
        if (!known.automations().isEmpty()) automationOrder.retainAll(known.automations());
        if (!known.automations().isEmpty()) {
            ownedAutomations.retainAll(known.automations());
            enabledAutomations.retainAll(known.automations());
            automationSpeedLevels.keySet().retainAll(known.automations());
            automationTimers.keySet().retainAll(known.automations());
        }
        if (elements.keySet().removeIf(number -> number < 1 || number > known.elements())) elementsVersion++;
        if (!known.darkUpgrades().isEmpty() && darkUpgradeLevels.keySet().retainAll(known.darkUpgrades())) darkVersion++;
        if (!known.darkAutomations().isEmpty()) {
            enabledDarkAutomations.retainAll(known.darkAutomations());
            darkAutomationTimers.keySet().retainAll(known.darkAutomations());
        }
        if (!known.challenges().isEmpty()) {
            if (activeChallenge != null && !known.challenges().contains(activeChallenge)) activeChallenge = null;
            if (completedChallenges.retainAll(known.challenges())) challengesVersion++;
            challengeTimes.keySet().retainAll(known.challenges());
        }
        if (!known.achievements().isEmpty() && achievements.retainAll(known.achievements())) achievementsVersion++;
        if (!known.molecules().isEmpty()) {
            for (String id : List.copyOf(molecules.keySet())) {
                if (!known.molecules().contains(id)) setMoleculeCount(id, 0);
            }
            substances.retainAll(known.molecules());
            automatedMolecules.retainAll(known.molecules());
            favoriteMolecules.retainAll(known.molecules());
        }
        if (!known.assemblies().isEmpty()) assemblies.retainAll(known.assemblies());
        if (!known.bodies().isEmpty()) bodies.retainAll(known.bodies());
        maturing.keySet().retainAll(bodies);
        solarSystems.removeIf(system -> !bodies.contains(system.star()) || !bodies.containsAll(system.planets())
                || !bodies.containsAll(system.moons()));
        if (!known.spaceUpgrades().isEmpty()) spaceUpgrades.retainAll(known.spaceUpgrades());
        substancesSeen = null;
        assembliesSeen = null;
        bodiesSeen = null;
        moleculesVersion++;
        return before - identifiers();
    }

    /** Nombre d'identifiants que porte la partie, tous catalogues confondus. */
    private int identifiers() {
        return upgradeLevels.size() + ownedAutomations.size() + enabledAutomations.size() + automationSpeedLevels.size()
                + automationTimers.size() + elements.size() + darkUpgradeLevels.size() + enabledDarkAutomations.size()
                + darkAutomationTimers.size() + (activeChallenge == null ? 0 : 1) + completedChallenges.size()
                + challengeTimes.size() + achievements.size() + molecules.size() + substances.size()
                + automatedMolecules.size() + favoriteMolecules.size() + atomUpgradeOrder.size() + automationOrder.size() + assemblies.size() + bodies.size() + spaceUpgrades.size();
    }

    /** Vue en lecture seule des éléments possédés (numéro atomique → exemplaires), par numéro croissant. */
    public Map<Integer, Integer> elements() {
        return Collections.unmodifiableMap(elements);
    }

    /** Vue en lecture seule des automatismes achetés, pour la sauvegarde. */
    public Set<String> ownedAutomations() {
        return Set.copyOf(ownedAutomations);
    }

    /** Vue en lecture seule des automatismes en marche, pour la sauvegarde. */
    public Set<String> enabledAutomations() {
        return Set.copyOf(enabledAutomations);
    }

    /** Vue en lecture seule des niveaux, pour la sauvegarde. */
    public Map<String, Integer> upgradeLevels() {
        return Map.copyOf(upgradeLevels);
    }
}
