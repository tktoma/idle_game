package idle.core;

import idle.core.achievements.Achievement;
import idle.core.darkmatter.DarkUpgrade;
import idle.core.upgrades.Automation;
import idle.core.upgrades.Upgrade;
import java.util.List;
import java.util.random.RandomGenerator;

/**
 * Le défi final (version 0.160), que la bulle des racines ouvre : un palier à atteindre dans chacune des dix
 * dimensions, en ressource gagnée en tout. La progression vers chaque palier se compte en puissances de dix, de 0 à 1 :
 * log10(1 + gagné) / log10(1 + palier). Quand les dix paliers sont atteints, la partie est finie : l'interface le
 * montre par une animation, puis l'écran de fin.
 */
public abstract class GameFinale extends GameMix {

    GameFinale(GameState state, List<Upgrade> catalog, List<Automation> automationCatalog, List<DarkUpgrade> darkCatalog,
            List<Achievement> achievementCatalog, RandomGenerator random) {
        super(state, catalog, automationCatalog, darkCatalog, achievementCatalog, random);
    }

    /**
     * Les paliers, de la ligne au mélange : cent fois le seuil de chaque dimension, et dix millions de fragments pour le
     * mélange (choix arbitraires, à régler une fois mesurés).
     */
    private static final double[] FINAL_GOALS = {
        100 * LINE_THRESHOLD, 100 * PLANE_THRESHOLD, 100 * VOLUME_THRESHOLD, 100 * DURATION_THRESHOLD, 100 * LOOP_THRESHOLD,
        100 * BRANE_THRESHOLD, 100 * CLICKER_THRESHOLD, 100 * GRID_THRESHOLD, 100 * MIRROR_THRESHOLD, 1e7};

    /** Vrai une fois la bulle des racines achetée : les paliers s'affichent. */
    public boolean isFinalChallengeOpen() {
        return isTreeBubbleBought(TreeBubble.ROOTS);
    }

    /** Le palier de la dimension {@code dimension} (1 à 10), en ressource gagnée en tout. */
    public static double finalGoal(int dimension) {
        return FINAL_GOALS[dimension - 1];
    }

    /**
     * Le jalon des racines (version 0.169) : dix paliers de gluons, comptés une fois la bulle des racines achetée. Chacun
     * abaisse les paliers du défi final de 2^(1/10) (environ −7 %) : ÷2 avec les dix. Les huit premiers suivent les
     * gluons de la fin de partie ; les deux derniers rejoignent les grands paliers de gluons (150 000 et 400 000), longs
     * à atteindre. Un plus, jamais nécessaire : le défi final se finit sans lui.
     */
    public static final long[] ROOT_GLUON_TIERS = {80_000, 88_000, 97_000, 107_000, 118_000, 130_000, 145_000, 160_000, 250_000, 400_000};

    /** Ce que chaque palier du jalon des racines laisse des paliers du défi final : 2^(−1/10). */
    public static final double ROOT_TIER_FACTOR = Math.pow(2, -0.1);

    /** Les paliers du jalon des racines atteints, de 0 à 10 : aucun sans la bulle des racines. */
    public int rootTiersReached() {
        if (!isTreeBubbleBought(TreeBubble.ROOTS)) return 0;
        long gluons = gluons();
        int reached = 0;
        for (long tier : ROOT_GLUON_TIERS) if (gluons >= tier) reached++;
        return reached;
    }

    /** Le palier de la dimension {@code dimension} pour cette partie : celui du défi final, abaissé par le jalon des racines. */
    public double finalGoalFor(int dimension) {
        return finalGoal(dimension) * Math.pow(ROOT_TIER_FACTOR, rootTiersReached());
    }

    /** La ressource gagnée en tout dans la dimension {@code dimension} (1 à 10). */
    public double dimensionEarned(int dimension) {
        return switch (dimension) {
            case 1 -> state.dimensionShardsEarned();
            case 2 -> state.planeShardsEarned();
            case 3 -> state.volumeShardsEarned();
            case 4 -> state.durationShardsEarned();
            case 5 -> state.loopShardsEarned();
            case 6 -> state.braneShardsEarned();
            case 7 -> state.clickerShardsEarned();
            case 8 -> state.gridShardsEarned();
            case 9 -> state.mirrorShardsEarned();
            case 10 -> state.mixShardsEarned();
            default -> 0;
        };
    }

    /** La progression vers le palier, de 0 à 1, en puissances de dix : log10(1 + gagné) / log10(1 + palier). */
    public double finalProgress(int dimension) {
        double earned = Math.max(0, dimensionEarned(dimension));
        return Math.min(1, Math.log10(1 + earned) / Math.log10(1 + finalGoalFor(dimension)));
    }

    public boolean isFinalTierReached(int dimension) {
        return dimensionEarned(dimension) >= finalGoalFor(dimension) - 1e-9;
    }

    /** Les paliers atteints, de 0 à 10. */
    public int finalTiersReached() {
        int reached = 0;
        for (int d = 1; d <= MAX_DIMENSIONS; d++) if (isFinalTierReached(d)) reached++;
        return reached;
    }

    /** Vrai quand le défi est ouvert et les dix paliers atteints. */
    public boolean isFinalChallengeComplete() {
        return isFinalChallengeOpen() && finalTiersReached() == MAX_DIMENSIONS;
    }

    /** Vrai une fois la partie finie : noté au premier pas où les dix paliers sont atteints, et sauvegardé. */
    public boolean isGameFinished() {
        return state.gameFinished();
    }

    /** Le pas des relevés des moments de la partie secondaire (version 0.168), en secondes de jeu. */
    static final double MOMENTS_EVERY = 1;
    private double momentsTimer = MOMENTS_EVERY;

    /**
     * Relève, au plus une fois par seconde de jeu, les moments de la partie secondaire (version 0.168) : chaque dimension
     * ouverte, son premier automatisme, son seuil, son palier final ; les paliers de la graine ; le défi final ouvert ; la
     * fin. Ils ne servent qu'à l'onglet Statistiques.
     */
    void noteMoments(double dt) {
        momentsTimer += dt;
        if (momentsTimer < MOMENTS_EVERY) return;
        momentsTimer = 0;
        GameStats stats = state.stats();
        double t = state.timePlayed();
        for (int d = 1; d <= Math.min(MAX_DIMENSIONS, dimensionsUnlocked()); d++) {
            stats.noteMoment("dimension." + d + ".ouverte", t);
            if (isDimensionThresholdReached(d)) stats.noteMoment("dimension." + d + ".seuil", t);
            if (isFinalTierReached(d)) stats.noteMoment("dimension." + d + ".final", t);
        }
        for (idle.core.universe.DimensionAutomation automation : idle.core.universe.DimensionAutomation.values()) {
            if (ownsDimensionAutomation(automation)) stats.noteMoment("dimension." + automation.dimension() + ".automatisme", t);
        }
        for (int stage = 1; stage <= Math.min(SEED_STAGES, seedStage()); stage++) stats.noteMoment("graine." + stage, t);
        if (isFinalChallengeOpen()) stats.noteMoment("defiFinal.ouvert", t);
        if (isGameFinished()) stats.noteMoment("fin", t);
        stats.endMomentsCatchUp();
    }

    /**
     * La fenêtre de la dimension {@code number} est restée ouverte {@code seconds} secondes (version 0.168) : le temps où
     * le joueur l'a jouée, pour l'onglet Statistiques.
     */
    public void noteDimensionPlayed(int number, double seconds) {
        if (number >= 1 && number <= MAX_DIMENSIONS && Double.isFinite(seconds)) state.stats().addDimensionPlay(number, Math.min(seconds, 1));
    }

    /** Note la fin de la partie, une fois. */
    void runFinale() {
        if (!state.gameFinished() && isFinalChallengeComplete()) state.setGameFinished(true);
    }
}
