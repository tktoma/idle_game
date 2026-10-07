package idle.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Les statistiques de la partie : des compteurs que {@link Game} tient à jour, et que rien
 * dans les règles ne lit. Ils ne servent qu'à être affichés (et sauvegardés avec
 * {@link GameState}, qui les porte).
 *
 * <p>Deux durées de vie :
 * <ul>
 *   <li>la plupart des compteurs courent <b>depuis le début du jeu</b> : ni la fusion, ni
 *       l'explosion, ni le Big Bang ne les remettent à zéro ;</li>
 *   <li>ceux dont le nom commence par {@code run} courent <b>depuis la dernière explosion</b>
 *       (depuis le début tant qu'il n'y en a pas eu) : {@link #endRun()} les remet à zéro.</li>
 * </ul>
 *
 * <p>Seul {@link Game} modifie ces compteurs : les méthodes qui écrivent ne sont pas publiques.
 */
public final class GameStats {

    // ----- Particules -----
    private BigNum particlesCreated = BigNum.ZERO;
    private BigNum particlesSpent = BigNum.ZERO;
    private BigNum bestProduction = BigNum.ZERO;
    private long particleUpgradesBought = 0;
    private long generatorsBought = 0;
    private BigNum runParticlesCreated = BigNum.ZERO;

    // ----- Atomes -----
    private long fusions = 0;
    private long runFusions = 0;
    private BigNum atomsCreated = BigNum.ZERO;
    private BigNum atomsSpent = BigNum.ZERO;
    private BigNum bestAtomsPerFusion = BigNum.ZERO;
    private double lastFusionTime = 0;
    private double fastestFusionTime = 0;
    private long atomUpgradesBought = 0;

    // ----- Automatisation -----
    private long automationActions = 0;
    private long darkAutomationActions = 0;

    // ----- Tableau périodique -----
    private long syntheses = 0;
    private long elementsObtained = 0;
    private long doubleDraws = 0;
    private long targetTries = 0;
    private long targetedSyntheses = 0;

    // ----- Matière noire -----
    private double holdTime = 0;
    private double runTime = 0;
    private double lastExplosionTime = 0;
    private double fastestExplosionTime = 0;
    private long timedExplosions = 0;
    private final List<Double> explosionTimes = new ArrayList<>();

    // ----- Big Bang -----
    private double bigBangTime = 0;
    private double lastBigBangTime = 0;
    private double fastestBigBangTime = 0;
    private long timedBigBangs = 0;
    private long lastBigBangExplosions = 0;
    private final List<Double> bigBangTimes = new ArrayList<>();
    private long moleculeCreations = 0;
    private long autoMoleculeCreations = 0;
    private long moleculesAttracted = 0;
    private int biggestCreation = 0;
    private long moleculeElementsSpent = 0;
    private final Map<Step, Double> steps = new EnumMap<>(Step.class);

    // ----- Historique et mémoire des déblocages -----
    private final StatsHistory history = new StatsHistory();
    private final StatsHistory runHistory = new StatsHistory();
    private final Set<String> unlockedOnce = new HashSet<>();

    /** Nombre maximal de durées d'explosion gardées : au-delà, les plus anciennes sont oubliées. */
    public static final int MAX_EXPLOSION_TIMES = 60;

    /** Nombre maximal de durées de Big Bang gardées : au-delà, les plus anciennes sont oubliées. */
    public static final int MAX_BIG_BANG_TIMES = 60;

    /** Les grandes premières du troisième acte, dont le jeu retient l'instant ({@link #reachedAt(Step)}). */
    public enum Step {
        FIRST_BIG_BANG("Premier Big Bang"),
        FIRST_MOLECULE("Première molécule"),
        FIRST_GATHERING("Premier rassemblement"),
        FIRST_ASSEMBLY("Premier assemblage"),
        FIRST_BODY("Premier astre"),
        GALAXY("Galaxie formée"),
        CLUSTER("Amas de galaxies formé"),
        UNIVERSE("Univers formé");

        private final String label;

        Step(String label) {
            this.label = label;
        }

        /** Nom affiché. */
        public String label() {
            return label;
        }
    }

    // ------------------------------------------------------------------
    // Lecture
    // ------------------------------------------------------------------

    /** Particules créées par les générateurs depuis le début du jeu. */
    public BigNum particlesCreated() {
        return particlesCreated;
    }

    /** Particules créées depuis la dernière explosion. */
    public BigNum runParticlesCreated() {
        return runParticlesCreated;
    }

    /** Particules dépensées en améliorations (celles perdues à la fusion ne comptent pas). */
    public BigNum particlesSpent() {
        return particlesSpent;
    }

    /** La plus forte production de particules par seconde jamais atteinte. */
    public BigNum bestProduction() {
        return bestProduction;
    }

    /** Niveaux d'améliorations payées en particules achetés, à la main ou par un automatisme. */
    public long particleUpgradesBought() {
        return particleUpgradesBought;
    }

    /** Générateurs achetés (le premier de chaque partie, offert, ne compte pas). */
    public long generatorsBought() {
        return generatorsBought;
    }

    /** Fusions faites depuis le début du jeu. */
    public long fusions() {
        return fusions;
    }

    /** Fusions faites depuis la dernière explosion. */
    public long runFusions() {
        return runFusions;
    }

    /** Atomes créés depuis le début du jeu, toutes explosions confondues. */
    public BigNum atomsCreated() {
        return atomsCreated;
    }

    /** Atomes dépensés : améliorations, automatismes, cadences, synthèses, arbre. */
    public BigNum atomsSpent() {
        return atomsSpent;
    }

    /** Le plus d'atomes jamais rapportés par une seule fusion. */
    public BigNum bestAtomsPerFusion() {
        return bestAtomsPerFusion;
    }

    /** Durée de la dernière partie entre deux fusions, en secondes (0 tant qu'il n'y a pas eu de fusion). */
    public double lastFusionTime() {
        return lastFusionTime;
    }

    /** Durée de la plus courte partie entre deux fusions, en secondes (0 tant qu'il n'y a pas eu de fusion). */
    public double fastestFusionTime() {
        return fastestFusionTime;
    }

    /** Niveaux d'améliorations payées en atomes achetés. */
    public long atomUpgradesBought() {
        return atomUpgradesBought;
    }

    /** Actions faites par les automatismes ordinaires : achats, fusions, synthèses. */
    public long automationActions() {
        return automationActions;
    }

    /** Actions faites par les automatismes de matière noire. */
    public long darkAutomationActions() {
        return darkAutomationActions;
    }

    /** Synthèses faites depuis le début du jeu. */
    public long syntheses() {
        return syntheses;
    }

    /** Exemplaires d'éléments obtenus depuis le début du jeu. */
    public long elementsObtained() {
        return elementsObtained;
    }

    /** Synthèses qui ont donné un élément de plus grâce à la chance de tirage double. */
    public long doubleDraws() {
        return doubleDraws;
    }

    /** Synthèses payées pour préparer une synthèse ciblée, depuis le début du jeu. */
    public long targetTries() {
        return targetTries;
    }

    /** Synthèses ciblées abouties depuis le début du jeu. */
    public long targetedSyntheses() {
        return targetedSyntheses;
    }

    /** Temps passé à maintenir la matière noire appuyée (ou verrouillée), en secondes. */
    public double holdTime() {
        return holdTime;
    }

    /** Temps de jeu depuis la dernière explosion, en secondes. */
    public double runTime() {
        return runTime;
    }

    /** Temps de jeu qu'a demandé la dernière explosion, en secondes (0 tant qu'il n'y en a pas eu). */
    public double lastExplosionTime() {
        return lastExplosionTime;
    }

    /** Temps de jeu qu'a demandé l'explosion la plus rapide, en secondes (0 tant qu'il n'y en a pas eu). */
    public double fastestExplosionTime() {
        return fastestExplosionTime;
    }

    /**
     * Temps de jeu qu'a demandé chacune des dernières explosions, de la plus ancienne à la plus
     * récente, en secondes ({@link #MAX_EXPLOSION_TIMES} au plus).
     */
    public List<Double> explosionTimes() {
        return Collections.unmodifiableList(explosionTimes);
    }

    /** Nombre d'explosions dont la durée a été notée : la dernière de {@link #explosionTimes()} porte ce numéro. */
    public long timedExplosions() {
        return timedExplosions;
    }

    // ----- Big Bang -----

    /** Temps de jeu depuis le dernier Big Bang, en secondes (depuis le début du jeu tant qu'il n'y en a pas eu). */
    public double bigBangTime() {
        return bigBangTime;
    }

    /** Temps de jeu qu'a demandé le dernier Big Bang, en secondes (0 tant qu'il n'y en a pas eu). */
    public double lastBigBangTime() {
        return lastBigBangTime;
    }

    /** Temps de jeu qu'a demandé le Big Bang le plus rapide, en secondes (0 tant qu'il n'y en a pas eu). */
    public double fastestBigBangTime() {
        return fastestBigBangTime;
    }

    /**
     * Temps de jeu qu'a demandé chacun des derniers Big Bangs, du plus ancien au plus récent, en
     * secondes ({@link #MAX_BIG_BANG_TIMES} au plus).
     */
    public List<Double> bigBangTimes() {
        return Collections.unmodifiableList(bigBangTimes);
    }

    /** Nombre de Big Bangs dont la durée a été notée : le dernier de {@link #bigBangTimes()} porte ce numéro. */
    public long timedBigBangs() {
        return timedBigBangs;
    }

    /** Nombre d'explosions qu'a demandé le dernier Big Bang (0 tant qu'il n'y en a pas eu). */
    public long lastBigBangExplosions() {
        return lastBigBangExplosions;
    }

    /** Créations de molécules depuis le début du jeu, à la main ou par l'automatisme : une création peut ajouter plusieurs molécules. */
    public long moleculeCreations() {
        return moleculeCreations;
    }

    /** Créations de molécules faites par l'automatisme « Création automatique ». */
    public long autoMoleculeCreations() {
        return autoMoleculeCreations;
    }

    /** Molécules qu'ont attirées les amas : tout ce qu'une création a ajouté au-delà de sa première molécule. */
    public long moleculesAttracted() {
        return moleculesAttracted;
    }

    /** Le plus de molécules jamais ajoutées par une seule création (0 tant qu'il n'y en a pas eu). */
    public int biggestCreation() {
        return biggestCreation;
    }

    /** Exemplaires d'éléments du tableau périodique employés à créer des molécules. */
    public long moleculeElementsSpent() {
        return moleculeElementsSpent;
    }

    /** Vrai une fois cette première atteinte. */
    public boolean reached(Step step) {
        return steps.containsKey(step);
    }

    /** Temps de jeu auquel cette première a été atteinte, en secondes ({@code NaN} tant qu'elle ne l'est pas). */
    public double reachedAt(Step step) {
        return steps.getOrDefault(step, Double.NaN);
    }

    /** L'historique du jeu entier : ses instants sont comptés en temps de jeu depuis le début. */
    public StatsHistory history() {
        return history;
    }

    /** L'historique de la partie en cours : ses instants sont comptés depuis la dernière explosion. */
    public StatsHistory runHistory() {
        return runHistory;
    }

    /**
     * Vrai si ce qui porte cet identifiant (un automatisme) a déjà été débloqué au moins une
     * fois depuis le début du jeu, même s'il est de nouveau verrouillé après une explosion.
     */
    public boolean wasUnlocked(String id) {
        return unlockedOnce.contains(id);
    }

    // ------------------------------------------------------------------
    // Sauvegarde
    // ------------------------------------------------------------------

    /**
     * Range tous les compteurs dans une sauvegarde, sous ce préfixe. Tout champ ajouté à cette
     * classe doit être écrit ici et relu dans {@link #load(SaveData, String)}.
     */
    void save(SaveData data, String prefix) {
        data.put(prefix + "particlesCreated", particlesCreated);
        data.put(prefix + "particlesSpent", particlesSpent);
        data.put(prefix + "bestProduction", bestProduction);
        data.put(prefix + "particleUpgradesBought", particleUpgradesBought);
        data.put(prefix + "generatorsBought", generatorsBought);
        data.put(prefix + "runParticlesCreated", runParticlesCreated);
        data.put(prefix + "fusions", fusions);
        data.put(prefix + "runFusions", runFusions);
        data.put(prefix + "atomsCreated", atomsCreated);
        data.put(prefix + "atomsSpent", atomsSpent);
        data.put(prefix + "bestAtomsPerFusion", bestAtomsPerFusion);
        data.put(prefix + "lastFusionTime", lastFusionTime);
        data.put(prefix + "fastestFusionTime", fastestFusionTime);
        data.put(prefix + "atomUpgradesBought", atomUpgradesBought);
        data.put(prefix + "automationActions", automationActions);
        data.put(prefix + "darkAutomationActions", darkAutomationActions);
        data.put(prefix + "syntheses", syntheses);
        data.put(prefix + "elementsObtained", elementsObtained);
        data.put(prefix + "doubleDraws", doubleDraws);
        data.put(prefix + "targetTries", targetTries);
        data.put(prefix + "targetedSyntheses", targetedSyntheses);
        data.put(prefix + "holdTime", holdTime);
        data.put(prefix + "runTime", runTime);
        data.put(prefix + "lastExplosionTime", lastExplosionTime);
        data.put(prefix + "fastestExplosionTime", fastestExplosionTime);
        data.put(prefix + "timedExplosions", timedExplosions);
        data.putDoubleList(prefix + "explosionTimes", explosionTimes);
        data.put(prefix + "bigBangTime", bigBangTime);
        data.put(prefix + "lastBigBangTime", lastBigBangTime);
        data.put(prefix + "fastestBigBangTime", fastestBigBangTime);
        data.put(prefix + "timedBigBangs", timedBigBangs);
        data.put(prefix + "lastBigBangExplosions", lastBigBangExplosions);
        data.putDoubleList(prefix + "bigBangTimes", bigBangTimes);
        data.put(prefix + "moleculeCreations", moleculeCreations);
        data.put(prefix + "autoMoleculeCreations", autoMoleculeCreations);
        data.put(prefix + "moleculesAttracted", moleculesAttracted);
        data.put(prefix + "biggestCreation", biggestCreation);
        data.put(prefix + "moleculeElementsSpent", moleculeElementsSpent);
        Map<String, Double> reached = new java.util.LinkedHashMap<>();
        steps.forEach((step, time) -> reached.put(step.name(), time));
        data.putDoubles(prefix + "steps", reached);
        data.putList(prefix + "unlockedOnce", new java.util.TreeSet<>(unlockedOnce));
        history.save(data, prefix + "history.");
        runHistory.save(data, prefix + "runHistory.");
    }

    /** Relit les compteurs d'une sauvegarde. Une clé absente laisse le compteur à zéro ; une première inconnue est ignorée. */
    void load(SaveData data, String prefix) {
        particlesCreated = data.big(prefix + "particlesCreated", BigNum.ZERO);
        particlesSpent = data.big(prefix + "particlesSpent", BigNum.ZERO);
        bestProduction = data.big(prefix + "bestProduction", BigNum.ZERO);
        particleUpgradesBought = data.count(prefix + "particleUpgradesBought", 0);
        generatorsBought = data.count(prefix + "generatorsBought", 0);
        runParticlesCreated = data.big(prefix + "runParticlesCreated", BigNum.ZERO);
        fusions = data.count(prefix + "fusions", 0);
        runFusions = data.count(prefix + "runFusions", 0);
        atomsCreated = data.big(prefix + "atomsCreated", BigNum.ZERO);
        atomsSpent = data.big(prefix + "atomsSpent", BigNum.ZERO);
        bestAtomsPerFusion = data.big(prefix + "bestAtomsPerFusion", BigNum.ZERO);
        lastFusionTime = data.real(prefix + "lastFusionTime", 0);
        fastestFusionTime = data.real(prefix + "fastestFusionTime", 0);
        atomUpgradesBought = data.count(prefix + "atomUpgradesBought", 0);
        automationActions = data.count(prefix + "automationActions", 0);
        darkAutomationActions = data.count(prefix + "darkAutomationActions", 0);
        syntheses = data.count(prefix + "syntheses", 0);
        elementsObtained = data.count(prefix + "elementsObtained", 0);
        doubleDraws = data.count(prefix + "doubleDraws", 0);
        targetTries = data.count(prefix + "targetTries", 0);
        targetedSyntheses = data.count(prefix + "targetedSyntheses", 0);
        holdTime = data.real(prefix + "holdTime", 0);
        runTime = data.real(prefix + "runTime", 0);
        lastExplosionTime = data.real(prefix + "lastExplosionTime", 0);
        fastestExplosionTime = data.real(prefix + "fastestExplosionTime", 0);
        timedExplosions = data.count(prefix + "timedExplosions", 0);
        explosionTimes.clear();
        explosionTimes.addAll(data.doubleList(prefix + "explosionTimes"));
        bigBangTime = data.real(prefix + "bigBangTime", 0);
        lastBigBangTime = data.real(prefix + "lastBigBangTime", 0);
        fastestBigBangTime = data.real(prefix + "fastestBigBangTime", 0);
        timedBigBangs = data.count(prefix + "timedBigBangs", 0);
        lastBigBangExplosions = data.count(prefix + "lastBigBangExplosions", 0);
        bigBangTimes.clear();
        bigBangTimes.addAll(data.doubleList(prefix + "bigBangTimes"));
        moleculeCreations = data.count(prefix + "moleculeCreations", 0);
        autoMoleculeCreations = data.count(prefix + "autoMoleculeCreations", 0);
        moleculesAttracted = data.count(prefix + "moleculesAttracted", 0);
        biggestCreation = data.whole(prefix + "biggestCreation", 0);
        moleculeElementsSpent = data.count(prefix + "moleculeElementsSpent", 0);
        steps.clear();
        data.doubles(prefix + "steps").forEach((name, time) -> {
            for (Step step : Step.values()) {
                if (step.name().equals(name)) steps.put(step, time);
            }
        });
        unlockedOnce.clear();
        unlockedOnce.addAll(data.list(prefix + "unlockedOnce"));
        history.load(data, prefix + "history.");
        runHistory.load(data, prefix + "runHistory.");
    }

    // ------------------------------------------------------------------
    // Écriture, réservée à Game
    // ------------------------------------------------------------------

    void noteUnlocked(String id) {
        unlockedOnce.add(id);
    }

    void addTime(double seconds) {
        runTime += seconds;
        bigBangTime += seconds;
    }

    /** Une première du troisième acte vient d'être atteinte, à ce temps de jeu : seule la première fois compte. */
    void noteStep(Step step, double timePlayed) {
        steps.putIfAbsent(step, timePlayed);
    }

    /** Un Big Bang vient d'avoir lieu : note sa durée et le nombre d'explosions qu'il a demandé. */
    void noteBigBang(long explosions, double timePlayed) {
        timedBigBangs++;
        lastBigBangTime = bigBangTime;
        if (timedBigBangs == 1 || bigBangTime < fastestBigBangTime) fastestBigBangTime = bigBangTime;
        bigBangTimes.add(bigBangTime);
        if (bigBangTimes.size() > MAX_BIG_BANG_TIMES) bigBangTimes.remove(0);
        lastBigBangExplosions = explosions;
        bigBangTime = 0;
        noteStep(Step.FIRST_BIG_BANG, timePlayed);
    }

    /**
     * Une création de molécules vient d'avoir lieu.
     *
     * @param created   molécules ajoutées
     * @param elements  exemplaires d'éléments employés
     * @param automatic vrai si c'est l'automatisme qui l'a faite
     */
    void noteMoleculeCreation(int created, long elements, boolean automatic) {
        moleculeCreations++;
        if (automatic) autoMoleculeCreations++;
        moleculesAttracted += Math.max(0, created - 1);
        biggestCreation = Math.max(biggestCreation, created);
        moleculeElementsSpent += elements;
    }

    void addParticles(BigNum created) {
        particlesCreated = particlesCreated.add(created);
        runParticlesCreated = runParticlesCreated.add(created);
    }

    void noteProduction(BigNum perSecond) {
        bestProduction = bestProduction.max(perSecond);
    }

    void noteParticleUpgrade(BigNum cost, boolean generator) {
        particlesSpent = particlesSpent.add(cost);
        particleUpgradesBought++;
        if (generator) generatorsBought++;
    }

    void noteAtomUpgrade(BigNum cost) {
        atomsSpent = atomsSpent.add(cost);
        atomUpgradesBought++;
    }

    void addParticlesSpent(BigNum cost) {
        particlesSpent = particlesSpent.add(cost);
    }

    void addAtomsSpent(BigNum cost) {
        atomsSpent = atomsSpent.add(cost);
    }

    void noteFusion(BigNum atoms, double seconds) {
        fusions++;
        runFusions++;
        atomsCreated = atomsCreated.add(atoms);
        bestAtomsPerFusion = bestAtomsPerFusion.max(atoms);
        lastFusionTime = seconds;
        if (fusions == 1 || seconds < fastestFusionTime) fastestFusionTime = seconds;
    }

    void noteAutomationAction() {
        automationActions++;
    }

    void noteDarkAutomationAction() {
        darkAutomationActions++;
    }

    void noteSynthesis(int elements, boolean doubleDraw) {
        syntheses++;
        elementsObtained += elements;
        if (doubleDraw) doubleDraws++;
    }

    /** Une synthèse payée pour une synthèse ciblée, qui n'a encore rien donné. */
    void noteSynthesisTry() {
        syntheses++;
        targetTries++;
    }

    /** Une synthèse ciblée vient d'aboutir. */
    void noteTargetedSynthesis() {
        targetedSyntheses++;
    }

    void addHoldTime(double seconds) {
        holdTime += seconds;
    }

    /** La partie recommence sans explosion (défi commencé ou abandonné) : ses compteurs repartent de zéro. */
    void restartRun() {
        runTime = 0;
        runParticlesCreated = BigNum.ZERO;
        runFusions = 0;
        runHistory.clear();
    }

    /** Une explosion vient d'avoir lieu : note sa durée et remet à zéro les compteurs de la partie. */
    void endRun() {
        timedExplosions++;
        lastExplosionTime = runTime;
        if (timedExplosions == 1 || runTime < fastestExplosionTime) fastestExplosionTime = runTime;
        explosionTimes.add(runTime);
        if (explosionTimes.size() > MAX_EXPLOSION_TIMES) explosionTimes.remove(0);
        runTime = 0;
        runParticlesCreated = BigNum.ZERO;
        runFusions = 0;
        runHistory.clear();
    }
}
