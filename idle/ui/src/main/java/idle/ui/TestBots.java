package idle.ui;

import idle.core.Game;
import idle.ui.common.Format;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

/**
 * Les bots et l'avance rapide du profil de test, partagés par le panneau ({@link TestPanel}) et la bande des
 * dimensions ({@link DimensionTestBar}) : le bot de la partie ({@link BotBridge}) en direct ou en avance rapide, celui
 * d'une dimension ({@link DimensionBotBridge}) de même, et l'avance rapide sans bot. L'avance rapide se fait par petites
 * tranches sur le fil JavaFX : la fenêtre ne gèle pas, et elle reste annulable.
 */
final class TestBots {
    /** Pas joués par impulsion pour un bot de dimension : 400 pas de 0,05 s, vingt secondes de jeu toutes les 25 ms. */
    static final int DIMENSION_BATCH = 400;
    /** Pas d'une avance rapide sans bot : une seconde de jeu, 200 par impulsion. */
    static final double PLAIN_STEP = 1;
    static final int PLAIN_BATCH = 200;

    private final Game game;
    private final Consumer<String> note;
    /** La politique du bot de la partie : DEPENDANCES d'abord, la meilleure des campagnes. */
    private String mode = BotBridge.modes().contains("DEPENDANCES") ? "DEPENDANCES" : BotBridge.modes().get(0);
    private BotBridge bot;
    private boolean live;
    private double sinceDecision;
    private DimensionBotBridge dimensionBot;
    private boolean dimensionLive;
    /** Vrai pendant une avance rapide d'un bot de dimension : l'annuler relâche ce qu'il tenait. */
    private boolean dimensionJob;
    private Timeline timeline;
    private BotFastForwardJob job;
    /** Ce qui suit un changement d'état (un bot qui démarre ou s'arrête, une avance rapide qui finit). */
    private Runnable onChange = () -> { };

    TestBots(Game game, Consumer<String> note) {
        this.game = game;
        this.note = note;
    }

    void setOnChange(Runnable action) { onChange = action == null ? () -> { } : action; }

    // ------------------------------------------------------------------
    // Le bot de la partie
    // ------------------------------------------------------------------

    String mode() { return mode; }

    void setMode(String chosen) {
        if (chosen == null || chosen.equals(mode) || !BotBridge.modes().contains(chosen)) return;
        mode = chosen;
        bot = null;
    }

    /** Le bot de la politique choisie, créé à la demande ; {@code null} si les bots ne sont pas là. */
    private BotBridge bot() {
        if (bot == null || !bot.mode().equals(mode)) bot = BotBridge.of(game, mode);
        if (bot == null) note.accept("Les bots ne sont pas là : lancez le jeu avec le module « bots » (runTest l'ajoute).");
        return bot;
    }

    boolean live() { return live; }

    void toggleLive() {
        if (!game.isStarted()) return;
        live = !live && bot() != null;
        sinceDecision = 0;
        onChange.run();
    }

    /** Le bot joue une décision, tout de suite. */
    void decideOnce() {
        BotBridge b = bot();
        if (b != null) note.accept(b.act() ? "Le bot a joué une décision." : "Le bot a échoué : il ne suit plus le jeu.");
    }

    /** Le bot joue {@code seconds} secondes en avance rapide, ou jusqu'au prochain Big Bang. */
    void fastForward(double seconds, boolean untilBigBang) {
        BotBridge b = bot();
        if (b == null || !game.isStarted()) return;
        stopEverything();
        int bangs = game.bigBangs();
        int explosions = game.state().explosions();
        run(new BotFastForwardJob(seconds, BotBridge.STEP, game::tick, b::act, () -> untilBigBang && game.bigBangs() != bangs), 10,
                "Bot", (end, elapsed) -> {
                    if (end == BotFastForwardJob.End.BOT_ERROR) note.accept("Bot arrêté : décision impossible.");
                    else if (end == BotFastForwardJob.End.TARGET_REACHED) note.accept("Big Bang atteint en " + Format.duration(elapsed) + ".");
                    else note.accept("Avance de " + Format.duration(elapsed) + " terminée : +" + (game.state().explosions() - explosions)
                            + " explosion(s), +" + (game.bigBangs() - bangs) + " Big Bang(s).");
                });
    }

    /** Le temps passe {@code seconds} secondes, sans bot : ce que font les automatismes seuls. */
    void advance(double seconds) {
        if (!game.isStarted()) return;
        stopEverything();
        run(new BotFastForwardJob(seconds, PLAIN_STEP, game::tick, () -> true, () -> false), PLAIN_BATCH, "Avance",
                (end, elapsed) -> note.accept("Avance de " + Format.duration(elapsed) + " terminée."));
    }

    // ------------------------------------------------------------------
    // Le bot d'une dimension (de la ligne à la durée)
    // ------------------------------------------------------------------

    /** Vrai si cette dimension a un bot. */
    static boolean hasDimensionBot(int number) { return number >= 1 && number <= DimensionBotBridge.LABELS.size(); }

    private DimensionBotBridge dimensionBot(int number) {
        if (dimensionBot == null || dimensionBot.number() != number) {
            if (dimensionLive) stopDimensionLive();
            dimensionBot = DimensionBotBridge.of(game, number);
        }
        if (dimensionBot == null) note.accept("Les bots ne sont pas là : lancez le jeu avec le module « bots » (runTest l'ajoute).");
        return dimensionBot;
    }

    /** Vrai si la dimension est ouverte ; sinon le dit. */
    private boolean open(int number) {
        if (game.dimensionsUnlocked() >= number) return true;
        note.accept("La dimension " + number + " n'est pas ouverte.");
        return false;
    }

    /** Vrai si le bot de cette dimension joue en direct. */
    boolean dimensionLive(int number) { return dimensionLive && dimensionBot != null && dimensionBot.number() == number; }

    void toggleDimensionLive(int number) {
        if (!game.isStarted()) return;
        if (dimensionLive(number)) stopDimensionLive();
        else {
            DimensionBotBridge b = dimensionBot(number);
            dimensionLive = b != null && open(number);
            if (dimensionLive) note.accept("Le bot " + b.of() + " joue en direct.");
        }
        onChange.run();
    }

    private void stopDimensionLive() {
        if (dimensionLive && dimensionBot != null) release(dimensionBot);
        dimensionLive = false;
    }

    /** Le bot s'arrête : le point du plan s'immobilise, le bouton de la durée se relâche (il donne ce qu'il avait). */
    private void release(DimensionBotBridge b) {
        if (b.number() == 2) game.setPlaneInput(0, 0);
        if (b.number() == 4 && game.isDurationHeld()) game.releaseDuration();
    }

    /** Le bot de la dimension joue {@code seconds} secondes, ou jusqu'au rond vert, en avance rapide. */
    void dimensionFastForward(int number, double seconds, boolean untilGreen) {
        DimensionBotBridge b = dimensionBot(number);
        if (b == null || !open(number)) return;
        if (untilGreen && b.reached()) {
            note.accept("Le rond vert " + b.of() + " est déjà là.");
            return;
        }
        stopEverything();
        int bought = b.purchases();
        dimensionJob = true;
        run(new BotFastForwardJob(seconds, DimensionBotBridge.STEP, game::tick, () -> b.decide(DimensionBotBridge.STEP),
                () -> untilGreen && b.reached()), DIMENSION_BATCH, "Bot " + b.of(), (end, elapsed) -> {
                    dimensionJob = false;
                    release(b);
                    int count = b.purchases() - bought;
                    if (end == BotFastForwardJob.End.BOT_ERROR) note.accept("Bot " + b.of() + " arrêté : la dimension s'est fermée.");
                    else if (end == BotFastForwardJob.End.TARGET_REACHED)
                        note.accept("Rond vert " + b.of() + " en " + Format.duration(elapsed) + " (" + count + " achats).");
                    else note.accept("Bot " + b.of() + " : " + Format.duration(elapsed) + " joués, " + count + " achats"
                            + (b.reached() ? ", rond vert là." : "."));
                });
    }

    // ------------------------------------------------------------------
    // Le temps de jeu
    // ------------------------------------------------------------------

    /**
     * Le temps de jeu qui vient de passer (vitesse comprise) : en direct, le bot de la partie décide toutes les
     * {@link BotBridge#STEP} secondes, celui de la dimension à chaque image.
     */
    void frame(double gameSeconds) {
        if (!game.isStarted()) return;
        if (dimensionLive && (dimensionBot == null || !dimensionBot.decide(gameSeconds))) {
            stopDimensionLive();
            note.accept("Le bot de dimension s'est arrêté : sa dimension n'est plus ouverte.");
            onChange.run();
        }
        if (!live) return;
        sinceDecision += gameSeconds;
        int decisions = 0;
        while (sinceDecision >= BotBridge.STEP && decisions < 50) {
            sinceDecision -= BotBridge.STEP;
            decisions++;
            BotBridge b = bot();
            if (b == null || !b.act()) {
                live = false;
                note.accept("Le bot s'est arrêté : il ne suit plus le jeu.");
                onChange.run();
                return;
            }
        }
        if (sinceDecision > BotBridge.STEP) sinceDecision = 0;      // une image très longue ne fait pas une rafale
    }

    /** Vrai tant que le jeu est avancé par tranches ; l'application suspend alors son pas ordinaire. */
    boolean isFastForwarding() { return job != null && job.running(); }

    /** Arrête l'avance rapide et rend la main tout de suite. */
    void cancel() {
        if (job != null) {
            job.cancel();
            note.accept("Avance rapide interrompue après " + Format.duration(job.advancedSeconds()) + ".");
        }
        if (timeline != null) timeline.stop();
        timeline = null;
        job = null;
        if (dimensionJob && dimensionBot != null) release(dimensionBot);
        dimensionJob = false;
        onChange.run();
    }

    /** Avant une avance rapide : plus rien d'autre ne joue (ni avance en cours, ni bot en direct). */
    private void stopEverything() {
        if (isFastForwarding()) cancel();
        live = false;
        stopDimensionLive();
    }

    /** Une avance rapide sur le fil JavaFX : {@code batch} pas à chaque impulsion de 25 ms ; {@code done} dit comment elle a fini. */
    private void run(BotFastForwardJob next, int batch, String who, BiConsumer<BotFastForwardJob.End, Double> done) {
        job = next;
        onChange.run();
        timeline = new Timeline(new KeyFrame(Duration.millis(25), event -> {
            if (job == null) return;
            BotFastForwardJob.End end;
            try {
                end = job.runBatch(batch);
            } catch (RuntimeException error) {
                cancel();
                note.accept("Erreur pendant l'avance rapide : " + error.getMessage());
                return;
            }
            if (end == BotFastForwardJob.End.RUNNING) {
                note.accept(who + " : " + Format.duration(job.advancedSeconds()) + " / " + Format.duration(job.totalSeconds()) + ".");
                return;
            }
            double elapsed = job.advancedSeconds();
            timeline.stop();
            timeline = null;
            job = null;
            done.accept(end, elapsed);
            onChange.run();
        }));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }
}
