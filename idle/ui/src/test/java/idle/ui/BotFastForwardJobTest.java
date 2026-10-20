package idle.ui;

import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/** Tests sans affichage JavaFX : exécutables sur les runners CI headless. */
class BotFastForwardJobTest {
    @Test void travailleParPetitsLotsSansDepasserLeBudget() {
        AtomicReference<Double> clock = new AtomicReference<>(0.0);
        AtomicInteger decisions = new AtomicInteger();
        var job = new BotFastForwardJob(121, 20, t -> clock.updateAndGet(v -> v + t),
                () -> { decisions.incrementAndGet(); return true; }, () -> false);
        assertEquals(BotFastForwardJob.End.RUNNING, job.runBatch(2));
        assertEquals(40, job.advancedSeconds());
        assertEquals(2, decisions.get());
        assertEquals(BotFastForwardJob.End.FINISHED, job.runBatch(20));
        assertEquals(121.0, clock.get().doubleValue(), 1e-9);
        assertEquals(7, decisions.get());
        assertEquals(1.0, job.progress());
    }

    @Test void arretImmediatAuProchainBigBang() {
        AtomicInteger bangs = new AtomicInteger();
        var job = new BotFastForwardJob(3600, 20, t -> {}, () -> { bangs.incrementAndGet(); return true; },
                () -> bangs.get() >= 3);
        assertEquals(BotFastForwardJob.End.TARGET_REACHED, job.runBatch(100));
        assertEquals(60, job.advancedSeconds());
        assertEquals(3, bangs.get());
    }

    @Test void annulationEtErreurArretentLesActions() {
        AtomicInteger actions = new AtomicInteger();
        var cancel = new BotFastForwardJob(1000, 20, t -> {}, () -> { actions.incrementAndGet(); return true; }, () -> false);
        cancel.runBatch(2);
        cancel.cancel();
        assertEquals(BotFastForwardJob.End.CANCELLED, cancel.runBatch(50));
        assertEquals(2, actions.get());
        var error = new BotFastForwardJob(1000, 20, t -> {}, () -> false, () -> false);
        assertEquals(BotFastForwardJob.End.BOT_ERROR, error.runBatch(50));
        assertEquals(20, error.advancedSeconds());
    }

    @Test void refuseLesDureesInvalides() {
        assertThrows(IllegalArgumentException.class, () -> new BotFastForwardJob(-1,20,t->{},()->true,()->false));
        assertThrows(IllegalArgumentException.class, () -> new BotFastForwardJob(60,0,t->{},()->true,()->false));
    }
}
