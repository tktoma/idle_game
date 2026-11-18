package idle.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Où en est le profil test UI (version 0.248) : l'étape montrée, la lecture automatique et son allure, et les erreurs
 * relevées en chemin. Sans affichage : le panneau ({@link UiTestPanel}) le montre, le jeu fait ce qu'il demande.
 */
final class UiTestRun {
    /** Le temps par étape, en secondes, au plus court et au plus long de la barre d'allure. */
    static final double FASTEST = 0.2, SLOWEST = 6;
    /** Au plus ce nombre d'erreurs gardées : au-delà, seul le compte monte. */
    static final int MAX_ERRORS = 2000;

    /**
     * Les images par seconde d'une combinaison de skins (version 0.253), sur tous ses états montrés : combien d'états,
     * l'état où est tombée la pire image, combien d'images en combien de temps, la moyenne et le plus bas (la pire
     * image), le temps de calcul du jeu par image, et la mise en place (la pire image des {@link #SETTLE} premières
     * secondes de chaque état, qui ne comptent pas dans le reste).
     */
    record Fps(long combination, String group, String mode, String skins, int states, String slowestState, int frames,
               double seconds, double average, double lowest, double worstMs, double workMs, double setupMs) {}

    /** Les premières secondes de chaque état : le temps de le mettre en place, à part des images par seconde. */
    static final double SETTLE = 0.25;
    /** Au plus ce nombre de combinaisons mesurées gardées en mémoire (le fichier les a toutes). */
    static final int MAX_FPS = 5000;

    /** Une erreur relevée : l'étape, l'onglet, la combinaison et l'état, et ce qui s'est passé. */
    record Error(long step, String group, String combination, String state, String message) {
        String line() { return "Étape " + (step + 1) + " · " + group + " · " + state + " · " + combination + " : " + message; }
    }

    private UiTestPlan.Run run;
    private long step;
    private boolean playing;
    private double secondsPerStep = 1;
    private double shown;
    /** Vrai une fois la vue de l'étape montrée vérifiée ({@link #takeCheck()}). */
    private boolean checked;
    private final List<Error> errors = new ArrayList<>();
    /** La mesure de la combinaison montrée : sa combinaison, ses états vus, l'état de la pire image. */
    private long measuring = -1;
    private final java.util.Set<String> statesSeen = new java.util.LinkedHashSet<>();
    private String worstState = "";
    private int frames;
    private double measured, worst, work, setupWorst, sinceShown;
    /** Le moment présent, lissé : la durée d'une image et le calcul par image, en secondes. */
    private double liveDt, liveWork;
    private final List<Fps> fpsLog = new ArrayList<>();
    private Fps lastFps, slowest;
    /** Où part chaque étape mesurée : le fichier des images par seconde. */
    private java.util.function.Consumer<Fps> fpsSink = fps -> { };
    private long errorCount;

    UiTestRun(UiTestPlan.Run run) { this.run = run; }

    UiTestPlan.Run run() { return run; }

    /** Change de passage (autre onglet, autres visuels, autre façon de combiner) : on repart de la première étape. */
    void setRun(UiTestPlan.Run run) {
        closeCombination();
        this.run = run;
        step = 0;
        shown = 0;
        checked = false;
    }

    long step() { return step; }

    boolean playing() { return playing; }

    /** Lance ou arrête la lecture ; à la dernière étape, rien à lire. */
    void setPlaying(boolean on) { playing = on && step < run.total() - 1; }

    double secondsPerStep() { return secondsPerStep; }

    void setSecondsPerStep(double seconds) {
        secondsPerStep = Double.isFinite(seconds) ? Math.max(FASTEST, Math.min(SLOWEST, seconds)) : 1;
    }

    /** Vrai si l'état montré veut que le temps du jeu passe. */
    boolean live() { return run.stepOf(step).live(); }

    /**
     * Le temps passe ; vrai si la lecture vient de passer à l'étape suivante (à montrer). À la dernière, la lecture
     * s'arrête.
     */
    boolean frame(double dt) {
        if (!playing || !(dt > 0)) return false;
        shown += dt;
        if (shown < secondsPerStep) return false;
        if (step >= run.total() - 1) {
            playing = false;
            return false;
        }
        step++;
        stepChanged();
        shown = 0;
        checked = false;
        return true;
    }

    /**
     * Vrai une fois par étape, pendant la lecture, aux quatre cinquièmes de son temps : le moment de vérifier la vue
     * (elle a eu le temps de se dessiner, et l'étape est encore montrée).
     */
    boolean takeCheck() {
        if (!playing || checked || shown < 0.8 * secondsPerStep) return false;
        checked = true;
        return true;
    }

    /** L'étape suivante ; faux à la dernière. */
    boolean next() { return goTo(step + 1); }

    /** L'étape d'avant ; faux à la première. */
    boolean previous() { return goTo(step - 1); }

    /** Va à l'étape {@code target} ; faux hors des bornes. */
    boolean goTo(long target) {
        if (target < 0 || target >= run.total()) return false;
        step = target;
        stepChanged();
        shown = 0;
        checked = false;
        return true;
    }

    /** Où envoyer chaque étape mesurée (le fichier des images par seconde). */
    void setFpsSink(java.util.function.Consumer<Fps> sink) { fpsSink = sink == null ? fps -> { } : sink; }

    /**
     * Une image vient d'être montrée, {@code dt} secondes après la précédente, et le jeu a calculé {@code workSeconds}
     * pour elle. Les {@link #SETTLE} premières secondes d'une étape comptent à part : la mise en place.
     */
    void measure(double dt, double workSeconds) {
        if (!(dt > 0)) return;
        if (measuring < 0) measuring = run.combinationOf(step);
        statesSeen.add(run.stepOf(step).label());
        double spent = Double.isFinite(workSeconds) ? Math.max(0, workSeconds) : 0;
        liveDt = liveDt == 0 ? dt : 0.9 * liveDt + 0.1 * dt;
        liveWork = 0.9 * liveWork + 0.1 * spent;
        sinceShown += dt;
        if (sinceShown <= SETTLE) {
            setupWorst = Math.max(setupWorst, dt);
            return;
        }
        frames++;
        measured += dt;
        if (dt > worst) {
            worst = dt;
            worstState = run.stepOf(step).label();
        }
        work += spent;
    }

    /** Une autre étape se montre : sa mise en place repart ; une autre combinaison, et la mesure d'avant est finie. */
    private void stepChanged() {
        sinceShown = 0;
        if (measuring >= 0 && run.combinationOf(step) != measuring) closeCombination();
    }

    /** La combinaison mesurée est finie : sa mesure part au fichier et compte pour la plus lente. */
    private void closeCombination() {
        if (frames > 0 && measuring >= 0) {
            Fps fps = new Fps(measuring, run.group.name(), run.mode.label, run.describeCombination(measuring), statesSeen.size(), worstState,
                    frames, measured, frames / measured, 1 / worst, worst * 1000, work / frames * 1000, setupWorst * 1000);
            if (fpsLog.size() < MAX_FPS) fpsLog.add(fps);
            lastFps = fps;
            if (slowest == null || fps.average() < slowest.average()) slowest = fps;
            fpsSink.accept(fps);
        }
        measuring = -1;
        statesSeen.clear();
        worstState = "";
        frames = 0;
        measured = 0;
        worst = 0;
        work = 0;
        setupWorst = 0;
        sinceShown = 0;
    }

    /** Termine la mesure en cours (fin de séance) : elle part au fichier. */
    void flushFps() { closeCombination(); }

    /** Les images par seconde en ce moment, lissées ; 0 avant la première image. */
    double liveFps() { return liveDt > 0 ? 1 / liveDt : 0; }

    /** Le calcul du jeu par image en ce moment, lissé, en millisecondes. */
    double liveWorkMs() { return liveWork * 1000; }

    /** La dernière combinaison mesurée, la plus lente du passage (moyenne la plus basse), et toutes ; {@code null} avant la première. */
    Fps lastFps() { return lastFps; }

    Fps slowestFps() { return slowest; }

    List<Fps> fpsLog() { return List.copyOf(fpsLog); }

    /** Relève une erreur à l'étape montrée ; la même deux fois de suite à la même étape ne compte qu'une. */
    void error(String message) {
        String text = message == null || message.isBlank() ? "erreur sans message" : message;
        if (!errors.isEmpty()) {
            Error last = errors.get(errors.size() - 1);
            if (last.step() == step && last.message().equals(text) && last.group().equals(run.group.name())) return;
        }
        errorCount++;
        if (errors.size() < MAX_ERRORS) {
            errors.add(new Error(step, run.group.name(), run.describe(step), run.stepOf(step).label(), text));
        }
    }

    List<Error> errors() { return List.copyOf(errors); }

    long errorCount() { return errorCount; }

    void clearErrors() {
        errors.clear();
        errorCount = 0;
    }

    /** Le temps qu'il reste pour finir le passage à cette allure, en mots. */
    String remaining() {
        long left = run.total() - 1 - step;
        if (run.total() == Long.MAX_VALUE) return "sans fin à l'échelle humaine";
        double seconds = left * secondsPerStep;
        return duration(seconds);
    }

    /** Une durée en mots : « 42 s », « 12 min », « 3 h 20 », « 5 j », « 12 ans ». */
    static String duration(double seconds) {
        if (seconds < 60) return Math.round(seconds) + " s";
        if (seconds < 3600) return Math.round(seconds / 60) + " min";
        if (seconds < 86_400) {
            long minutes = Math.round(seconds / 60);
            return minutes / 60 + " h " + String.format(Locale.ROOT, "%02d", minutes % 60);
        }
        if (seconds < 365 * 86_400.0) return Math.round(seconds / 86_400) + " j";
        double years = seconds / (365 * 86_400.0);
        return years < 1e6 ? Math.round(years) + " ans" : String.format(Locale.FRANCE, "%.1e ans", years);
    }

    /** Le rapport des erreurs, à copier. */
    String report() {
        StringBuilder text = new StringBuilder("Profil test UI · ").append(Version.CURRENT).append(" · ").append(run.group.name())
                .append(" · ").append(run.mode.label).append(" · ").append(errorCount).append(errorCount > 1 ? " erreurs" : " erreur");
        if (errorCount > errors.size()) text.append(" (les ").append(errors.size()).append(" premières)");
        text.append('\n');
        for (Error error : errors) text.append(error.line()).append('\n');
        return text.toString();
    }
}
