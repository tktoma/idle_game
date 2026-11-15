package idle.ui;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;

/** Démarrage partagé du toolkit pour les tests JavaFX sous Xvfb. */
public final class FxTestSupport {
    private static boolean started;
    private FxTestSupport() {}

    public static synchronized void start() {
        if (started) return;
        CountDownLatch latch = new CountDownLatch(1);
        try { Platform.startup(latch::countDown); }
        catch (IllegalStateException already) { latch.countDown(); }
        try { if (!latch.await(10, TimeUnit.SECONDS)) throw new AssertionError("JavaFX ne démarre pas"); }
        catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new AssertionError(interrupted); }
        started = true;
    }

    public static <T> T fx(java.util.concurrent.Callable<T> action) {
        start();
        if (Platform.isFxApplicationThread()) {
            try { return action.call(); } catch (Exception e) { throw new AssertionError(e); }
        }
        FutureTask<T> task = new FutureTask<>(action);
        Platform.runLater(task);
        // Deux minutes (20 s avant la 0.203) : les tests qui dessinent chaque skin à chaque palier dépassent vingt secondes
        // sur une machine lente, et le test resté en file faisait échouer tous les suivants.
        try { return task.get(120, TimeUnit.SECONDS); }
        catch (Exception e) { throw new AssertionError(e); }
    }

    /**
     * Ouvre les automatismes des dimensions 1 à {@code dimension} comme le jeu les ouvre depuis la 0.182 : la graine à
     * l'arbre-monde et un univers de plus que la dimension (avant, la bulle du sommet suffisait ; les tests des pages des
     * dimensions qui ne posaient qu'elle échouaient depuis).
     */
    public static void openDimensionAutomations(idle.core.Game game, int dimension) {
        while (game.universes() < idle.core.Game.dimensionAutomationUniverses(dimension)) {
            if (!game.testAddUniverse()) throw new AssertionError("univers " + (game.universes() + 1) + " impossible à ajouter");
        }
        game.state().setSeedSprouted(true);
        game.state().setSeedWaterings(Math.max(game.state().seedWaterings(), idle.core.Game.seedStageWaterings(idle.core.Game.SEED_STAGES)));
        if (!game.isDimensionAutomationOpen(dimension)) throw new AssertionError("automatismes de la dimension " + dimension + " fermés");
    }

    /** Donne à la dimension de quoi tout payer, dans sa propre ressource (version 0.209). */
    public static void fillDimension(idle.core.Game game, int dimension, double amount) {
        idle.core.GameState state = game.state();
        switch (dimension) {
            case 1 -> state.setDimensionShards(amount);
            case 2 -> state.setPlaneShards(amount);
            case 3 -> state.setVolumeShards(amount);
            case 4 -> state.setDurationShards(amount);
            case 5 -> state.setLoopShards(amount);
            case 6 -> state.setBraneShards(amount);
            case 7 -> state.setClickerShards(amount);
            case 8 -> state.setGridShards(amount);
            case 9 -> state.setMirrorShards(amount);
            default -> state.setMixShards(amount);
        }
    }

    /**
     * Achète, dans l'ordre, les automatismes de la dimension jusqu'à celui-ci (version 0.209 : ceux qui jouent viennent après
     * ceux qui achètent), avec la ressource de la dimension, puis coupe ceux qui achètent et rend la ressource d'avant : seul
     * celui qui joue tourne, comme avant la 0.209.
     */
    public static void buyPlay(idle.core.Game game, idle.core.universe.DimensionAutomation last) {
        int dimension = last.dimension();
        double before = game.dimensionResource(dimension);
        fillDimension(game, dimension, 1e300);
        for (idle.core.universe.DimensionAutomation automation : idle.core.universe.DimensionAutomation.values()) {
            if (automation.dimension() != dimension) continue;
            if (!game.ownsDimensionAutomation(automation) && !game.buyDimensionAutomation(automation)) throw new AssertionError(automation.toString());
            if (automation.kind() == idle.core.universe.DimensionAutomation.Kind.UPGRADE) game.setDimensionAutomationEnabled(automation, false);
            if (automation == last) break;
        }
        fillDimension(game, dimension, before);
    }

    /** Achète ceux qui achètent dans cette dimension, coupés, pour que le premier palier de celui qui joue s'offre (version 0.209). */
    public static void buyUpgradesOnly(idle.core.Game game, int dimension) {
        for (idle.core.universe.DimensionAutomation automation : idle.core.universe.DimensionAutomation.values()) {
            if (automation.dimension() == dimension && automation.kind() == idle.core.universe.DimensionAutomation.Kind.PLAY) {
                buyPlay(game, automation.previous());
                return;
            }
        }
    }
}
