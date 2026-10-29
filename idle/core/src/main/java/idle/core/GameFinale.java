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
        return Math.min(1, Math.log10(1 + earned) / Math.log10(1 + finalGoal(dimension)));
    }

    public boolean isFinalTierReached(int dimension) {
        return dimensionEarned(dimension) >= finalGoal(dimension) - 1e-9;
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

    /** Note la fin de la partie, une fois. */
    void runFinale() {
        if (!state.gameFinished() && isFinalChallengeComplete()) state.setGameFinished(true);
    }
}
