package idle.ui;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleConsumer;

/**
 * Moteur d'avancement des bots indépendant de JavaFX : le contrôleur JavaFX lui donne
 * un petit budget de pas à chaque impulsion au lieu de bloquer le fil d'interface.
 * Toutes les mutations du jeu restent donc sur le fil JavaFX, sans concurrence.
 */
public final class BotFastForwardJob {
    public enum End { RUNNING, FINISHED, TARGET_REACHED, CANCELLED, BOT_ERROR }

    private final double maxSeconds;
    private final double stepSeconds;
    private final DoubleConsumer advance;
    private final BooleanSupplier decision;
    private final BooleanSupplier targetReached;
    private double advanced;
    private End status = End.RUNNING;

    public BotFastForwardJob(double maxSeconds, double stepSeconds, DoubleConsumer advance,
                             BooleanSupplier decision, BooleanSupplier targetReached) {
        if (!Double.isFinite(maxSeconds) || maxSeconds <= 0 || !Double.isFinite(stepSeconds) || stepSeconds <= 0)
            throw new IllegalArgumentException("La durée et le pas doivent être positifs et finis");
        this.maxSeconds = maxSeconds;
        this.stepSeconds = stepSeconds;
        this.advance = Objects.requireNonNull(advance);
        this.decision = Objects.requireNonNull(decision);
        this.targetReached = Objects.requireNonNull(targetReached);
    }

    /** Exécute AU PLUS maxSteps pas ; à appeler régulièrement depuis une Timeline JavaFX. */
    public End runBatch(int maxSteps) {
        if (maxSteps <= 0) throw new IllegalArgumentException("Budget non positif");
        for (int i = 0; i < maxSteps && status == End.RUNNING; i++) {
            if (targetReached.getAsBoolean()) { status = End.TARGET_REACHED; break; }
            double dt = Math.min(stepSeconds, maxSeconds - advanced);
            if (dt <= 1e-9) { status = End.FINISHED; break; }
            advance.accept(dt);
            advanced += dt;
            if (!decision.getAsBoolean()) { status = End.BOT_ERROR; break; }
            if (targetReached.getAsBoolean()) status = End.TARGET_REACHED;
            else if (maxSeconds - advanced <= 1e-9) status = End.FINISHED;
        }
        return status;
    }

    public void cancel() {
        if (status == End.RUNNING) status = End.CANCELLED;
    }

    public End status() { return status; }
    public double advancedSeconds() { return advanced; }
    public double totalSeconds() { return maxSeconds; }
    public double progress() { return Math.min(1.0, advanced / maxSeconds); }
    public boolean running() { return status == End.RUNNING; }
}
