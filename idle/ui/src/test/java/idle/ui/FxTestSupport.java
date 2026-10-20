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
        try { return task.get(20, TimeUnit.SECONDS); }
        catch (Exception e) { throw new AssertionError(e); }
    }
}
