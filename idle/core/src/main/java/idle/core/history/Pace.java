package idle.core.history;

import idle.core.Game;
import idle.core.GameStats;
import idle.core.Multipliers;
import idle.core.bigbang.BigBangCondition;
import idle.core.darkmatter.DarkUpgrade;
import idle.core.elements.PeriodicTable;
import idle.core.sky.Cosmos;
import java.util.ArrayDeque;

/**
 * « Prochaine étape dans… » (version 0.165) : le temps qu'il reste avant la prochaine étape datée de la partie
 * ({@link Game#nextStep()}), au rythme des dix dernières minutes. Chaque étape a une mesure d'avancée, de 0 à 1, en
 * ordres de grandeur quand la ressource grandit vite ; le rythme est la pente de cette avancée sur la fenêtre, et le
 * temps restant (1 − avancée) / pente. Les formations du cosmos qui ne font qu'attendre que l'échelle d'avant se
 * rassemble donnent leur temps exact. Sans mesure (les premières molécules, rassemblements, assemblages, astres) ou
 * sans rythme (rien n'avance), pas d'estimation.
 */
public final class Pace {

    /** La fenêtre du rythme, en secondes de jeu. */
    public static final double WINDOW = 600;
    /** Le rythme ne se calcule qu'au-delà d'une minute de relevés. */
    static final double MIN_SPAN = 60;
    /** Un relevé toutes les cinq secondes de jeu, au plus. */
    static final double SAMPLE_EVERY = 5;
    /** Au-delà de deux jours de jeu, la pente est trop faible pour dire quelque chose : pas d'estimation. */
    static final double MAX_LEFT = 48 * 3600;

    private final ArrayDeque<double[]> samples = new ArrayDeque<>();
    /** Faux quand le jeu ne sait pas mesurer l'avancée vers l'étape visée. */
    private boolean measured = true;
    private GameStats.Step step;
    private double exact = Double.NaN;

    /** Ce qui avance vers une étape : de 0 (rien) à 1 (atteinte) ; NaN quand le jeu ne sait pas le mesurer. */
    static double progress(Game game, GameStats.Step step) {
        return switch (step) {
            case FIRST_FUSION -> Math.min(1, (double) game.generatorCount() / Math.max(1, game.generatorsPerAtom()));
            case FIRST_EXPLOSION -> {
                if (!game.isPeriodicTableUnlocked()) {
                    double atoms = game.state().totalAtoms().toDouble();
                    yield 0.5 * Math.min(1, Math.log10(1 + atoms) / Math.log10(1 + Game.UNLOCK_TOTAL_ATOMS.toDouble()));
                }
                yield 0.5 + 0.5 * game.discoveredElements() / PeriodicTable.ELEMENTS.size();
            }
            case FIRST_BIG_BANG -> {
                int count = 0;
                double met = 0;
                for (BigBangCondition c : game.bigBangConditions()) {
                    count++;
                    if (game.isBigBangConditionMet(c)) met++;
                    else met += part(game, c);
                }
                yield count == 0 ? Double.NaN : met / count;
            }
            case GALAXY, CLUSTER, UNIVERSE -> {
                Cosmos scale = game.nextCosmos();
                if (scale == null) yield 1;
                int conditions = game.cosmosConditions(scale);
                if (conditions == 0) yield Double.NaN;
                double met = game.cosmosConditionsMet(scale);
                // La galaxie compte ses astres mûrs : ceux qui mûrissent comptent pour la part déjà faite.
                if (scale == Cosmos.GALAXY) met += maturing(game);
                yield Math.min(1, met / conditions);
            }
            default -> Double.NaN;
        };
    }

    /**
     * La part déjà faite d'une condition du Big Bang qui n'est pas remplie : les
     * particules de la branche de matière noire en ordres de grandeur, comme les compte la condition ; les astres mûrs du
     * ciel ; rien pour les autres, qui viennent d'un coup.
     */
    static double part(Game game, BigBangCondition condition) {
        return switch (condition) {
            case PARTICLES -> {
                double have = game.darkBalance(DarkUpgrade.Branch.PARTICLES).log10(), need = game.bigBangParticles().log10();
                yield need > 0 && Double.isFinite(have) ? Math.max(0, Math.min(1, have / need)) : 0;
            }
            case SKY -> {
                int need = game.bigBangSkyRequirement();
                yield need > 0 ? Math.min(1, (game.bodiesMature() + maturing(game)) / need) : 0;
            }
            default -> 0;
        };
    }

    /** Les astres qui mûrissent encore, chacun pour la part de sa maturation déjà faite. */
    static double maturing(Game game) {
        double done = 0;
        for (var entry : game.state().maturing().entrySet()) {
            double full = game.maturationSeconds(game.body(entry.getKey()));
            if (full > 0) done += Math.max(0, Math.min(1, 1 - entry.getValue() / full));
        }
        return done;
    }

    /** Un relevé de la partie : à appeler régulièrement (l'interface le fait à chaque image). */
    public void observe(Game game) {
        GameStats.Step next = game.nextStep();
        if (next != step) {
            step = next;
            samples.clear();
        }
        exact = Double.NaN;
        if (step == null) return;
        // Une formation du cosmos qui n'attend plus que le rassemblement de l'échelle d'avant : son temps est connu.
        if (step == GameStats.Step.GALAXY || step == GameStats.Step.CLUSTER || step == GameStats.Step.UNIVERSE) {
            Cosmos scale = game.nextCosmos();
            if (scale != null && game.cosmosConditionsMet(scale) == game.cosmosConditions(scale)) {
                double settle = game.cosmosSettleRemaining(scale);
                if (settle > 0) exact = settle;
            }
        }
        double t = game.state().timePlayed();
        if (!samples.isEmpty() && t - samples.peekLast()[0] < SAMPLE_EVERY) return;
        double p = progress(game, step);
        measured = !Double.isNaN(p);
        if (!measured) {
            samples.clear();
            return;
        }
        // Une avancée qui recule (une remise à zéro) repart de là.
        if (!samples.isEmpty() && p < samples.peekLast()[1] - 1e-9) samples.clear();
        samples.addLast(new double[] {t, p});
        while (samples.size() > 2 && t - samples.peekFirst()[0] > WINDOW) samples.removeFirst();
    }

    /** L'étape visée, ou {@code null} quand toutes sont atteintes dans cette partie. */
    public GameStats.Step step() {
        return step;
    }

    /** Le temps qu'il reste avant l'étape, en secondes de jeu ; NaN sans estimation. */
    public double secondsLeft() {
        if (!Double.isNaN(exact)) return exact;
        if (samples.size() < 3) return Double.NaN;
        double t0 = samples.peekFirst()[0], t1 = samples.peekLast()[0];
        if (t1 - t0 < MIN_SPAN) return Double.NaN;
        // La pente des moindres carrés : un relevé de travers ne la fait pas basculer.
        double n = samples.size(), st = 0, sp = 0, stt = 0, stp = 0;
        for (double[] s : samples) {
            double x = s[0] - t0;
            st += x;
            sp += s[1];
            stt += x * x;
            stp += x * s[1];
        }
        // Une avancée qui n'a pas bougé sur la fenêtre ne dit rien du temps qu'il reste : pas d'estimation.
        if (samples.peekLast()[1] - samples.peekFirst()[1] < 1e-9) return Double.NaN;
        double slope = (n * stp - st * sp) / (n * stt - st * st);
        if (!(slope > 0)) return Double.NaN;
        double p = samples.peekLast()[1];
        double left = Math.max(0, (1 - p) / slope);
        return left > MAX_LEFT ? Double.NaN : left;
    }

    /** Pourquoi il n'y a pas d'estimation, en quelques mots ; vide quand il y en a une. */
    public String why() {
        if (step == null || !Double.isNaN(secondsLeft())) return "";
        if (!measured) return "cette étape ne se mesure pas";
        if (samples.size() < 3 || samples.peekLast()[0] - samples.peekFirst()[0] < MIN_SPAN) return "il faut quelques minutes d'avancée";
        return "l'avancée ne bouge pas assez en ce moment";
    }

    /**
     * La grandeur qui fait avancer vers l'étape : celle dont un achat raccourcit l'attente. La production jusqu'à
     * l'explosion, la matière noire jusqu'au Big Bang, l'espace ensuite.
     */
    public static Multipliers.Quantity engine(GameStats.Step step) {
        if (step == null) return Multipliers.Quantity.SPACE;
        return switch (step) {
            case FIRST_FUSION, FIRST_EXPLOSION -> Multipliers.Quantity.PRODUCTION;
            case FIRST_BIG_BANG -> Multipliers.Quantity.DARK_MATTER;
            default -> Multipliers.Quantity.SPACE;
        };
    }
}
