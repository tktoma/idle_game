package idle.core;

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
    private int synthesisCount = 0;
    private ElementCategory synthesisTarget = null;
    private int synthesisTries = 0;
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
    private final Map<String, Double> challengeTimes = new HashMap<>();
    private double decayDebt = 0;
    private final Set<String> achievements = new java.util.LinkedHashSet<>();
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
    private int[] logEnds = new int[16];
    private final List<String> moleculeLog = new MoleculeLog();
    private int moleculesVersion = 0;
    private final Set<String> substances = new java.util.LinkedHashSet<>();
    private final Set<String> assemblies = new java.util.LinkedHashSet<>();
    private final Set<String> bodies = new java.util.LinkedHashSet<>();
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
        int[] lengths = new int[runs];
        for (int run = 0; run < runs; run++) lengths[run] = logEnds[run] - (run == 0 ? 0 : logEnds[run - 1]);
        for (int run = runs - 1; run >= 0 && extra > 0; run--) {
            if (!logKinds.get(run).equals(moleculeId)) continue;
            int taken = Math.min(extra, lengths[run]);
            lengths[run] -= taken;
            extra -= taken;
        }
        List<String> kinds = new ArrayList<>(logKinds);
        logKinds.clear();
        int end = 0;
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

    /** Les molécules créées vues une à une, alors qu'elles sont gardées par suites. En lecture seule. */
    private final class MoleculeLog extends java.util.AbstractList<String> {

        @Override
        public int size() {
            return logKinds.isEmpty() ? 0 : logEnds[logKinds.size() - 1];
        }

        @Override
        public String get(int index) {
            if (index < 0 || index >= size()) throw new IndexOutOfBoundsException("Molécule n° " + index + " sur " + size());
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

        @Override
        public java.util.Iterator<String> iterator() {
            return new java.util.Iterator<>() {
                private int run = 0;
                private int index = 0;

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
    public int cometsCaught() {
        return cometsCaught;
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
        return Collections.unmodifiableSet(completedChallenges);
    }

    public void addCompletedChallenge(String challengeId) {
        completedChallenges.add(challengeId);
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
        return achievements.add(achievementId);
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
    }

    /** Vue en lecture seule des niveaux des améliorations de matière noire, pour la sauvegarde. */
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
        bigBangs = 0;
        molecules.clear();
        logKinds.clear();
        substances.clear();
        assemblies.clear();
        bodies.clear();
        substancesSeen = null;
        assembliesSeen = null;
        bodiesSeen = null;
        cosmos = 0;
        autoHold = true;
        autoMolecules = true;
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
        synthesisReserve = BigNum.ZERO;
        activeChallenge = null;
        completedChallenges.clear();
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
        decayDebt = 0;
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
    void save(SaveData data) {
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
        data.put("cosmos", cosmos);
        data.put("autoHold", autoHold);
        data.put("autoMolecules", autoMolecules);
        data.putList("automatedMolecules", automatedMolecules);
        data.putList("spaceUpgrades", spaceUpgrades);
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
    void load(SaveData data) {
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
        data.doubles("challengeTimes").forEach(this::setChallengeTime);
        setDecayDebt(data.real("decayDebt", 0));
        achievements.addAll(data.list("achievements"));
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
        setCosmosLevel(data.whole("cosmos", 0));
        autoHold = data.flag("autoHold", true);
        autoMolecules = data.flag("autoMolecules", true);
        automatedMolecules.addAll(data.list("automatedMolecules"));
        spaceUpgrades.addAll(data.list("spaceUpgrades"));
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
        if (!known.darkUpgrades().isEmpty()) darkUpgradeLevels.keySet().retainAll(known.darkUpgrades());
        if (!known.darkAutomations().isEmpty()) {
            enabledDarkAutomations.retainAll(known.darkAutomations());
            darkAutomationTimers.keySet().retainAll(known.darkAutomations());
        }
        if (!known.challenges().isEmpty()) {
            if (activeChallenge != null && !known.challenges().contains(activeChallenge)) activeChallenge = null;
            completedChallenges.retainAll(known.challenges());
            challengeTimes.keySet().retainAll(known.challenges());
        }
        if (!known.achievements().isEmpty()) achievements.retainAll(known.achievements());
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
