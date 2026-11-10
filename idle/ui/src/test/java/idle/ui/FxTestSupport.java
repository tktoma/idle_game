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
}
