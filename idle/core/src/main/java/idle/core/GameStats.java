package idle.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Les statistiques de la partie : des compteurs que {@link Game} tient à jour, et que rien
 * dans les règles ne lit. Ils ne servent qu'à être affichés (et sauvegardés avec
 * {@link GameState}, qui les porte).
 *
 * <p>Deux durées de vie :
 * <ul>
 *   <li>la plupart des compteurs courent <b>depuis le début du jeu</b> : ni la fusion ni
 *       l'explosion ne les remettent à zéro ;</li>
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

    // ----- Matière noire -----
    private double holdTime = 0;
    private double runTime = 0;
    private double lastExplosionTime = 0;
    private double fastestExplosionTime = 0;
    private long timedExplosions = 0;
    private final List<Double> explosionTimes = new ArrayList<>();

    // ----- Historique et mémoire des déblocages -----
    private final StatsHistory history = new StatsHistory();
    private final StatsHistory runHistory = new StatsHistory();
    private final Set<String> unlockedOnce = new HashSet<>();

    /** Nombre maximal de durées d'explosion gardées : au-delà, les plus anciennes sont oubliées. */
    public static final int MAX_EXPLOSION_TIMES = 60;

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
    // Écriture, réservée à Game
    // ------------------------------------------------------------------

    void noteUnlocked(String id) {
        unlockedOnce.add(id);
    }

    void addTime(double seconds) {
        runTime += seconds;
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

    void addHoldTime(double seconds) {
        holdTime += seconds;
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
