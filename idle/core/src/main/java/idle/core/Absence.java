package idle.core;

/**
 * Le retour du joueur après une absence : le jeu, fermé, n'a rien fait ; il rejoue maintenant une
 * part du temps écoulé, puis dit ce qu'elle a rapporté.
 *
 * <p>La part rejouée dépend de l'avancée de la partie ({@link Game#offlineRate()}) : un dixième au
 * premier acte, tout une fois l'univers formé. Elle est fixée au retour, pour toute l'absence.
 * Une absence de moins de {@link #MIN_SECONDS} ne compte pas, et rien n'est compté au-delà de
 * {@link #MAX_SECONDS}.
 *
 * <p>Le temps est rejoué par {@link Game#tick(double)}, exactement comme s'il passait jeu ouvert :
 * les automatismes agissent, la matière noire verrouillée grossit, l'espace s'étend. Rien n'est
 * fait à la place du joueur. Comme une longue absence demande du calcul, elle se rejoue par
 * morceaux ({@link #advanceFor(long)}), entre deux images, et le joueur peut passer la suite
 * ({@link #skip()}).
 */
public final class Absence {

    /** En dessous, l'absence ne compte pas : le joueur a seulement relancé le jeu. */
    public static final double MIN_SECONDS = 60;
    /** Au-delà, l'absence ne compte plus : vingt-quatre heures. */
    public static final double MAX_SECONDS = 24 * 3600;
    /** Durée de jeu rejouée d'un seul coup : assez court pour que les automatismes agissent comme jeu ouvert. */
    private static final double STEP = 10;

    /**
     * Ce qu'une absence a rapporté.
     *
     * @param away              durée de l'absence, en secondes
     * @param counted           durée retenue : l'absence, bornée à {@link #MAX_SECONDS}
     * @param rate              part du temps retenu qui a été rejouée (0,4 pour 40 %)
     * @param played            temps de jeu effectivement rejoué, en secondes
     * @param skipped           vrai si le joueur a passé la fin
     * @param particles         particules créées
     * @param atoms             atomes créés
     * @param fusions           fusions faites
     * @param syntheses         synthèses faites
     * @param elements          exemplaires d'éléments obtenus
     * @param explosions        explosions déclenchées par un automatisme
     * @param space             espace gagné
     * @param moleculeCreations créations de molécules
     * @param achievements      succès obtenus
     */
    public record Report(double away, double counted, double rate, double played, boolean skipped, BigNum particles,
                         BigNum atoms, long fusions, long syntheses, long elements, long explosions, BigNum space,
                         long moleculeCreations, int achievements) {

        /** Vrai si l'absence n'a rien rapporté du tout : il n'y a alors rien à annoncer. */
        public boolean isEmpty() {
            return particles.isZero() && atoms.isZero() && fusions == 0 && syntheses == 0 && explosions == 0
                    && space.isZero() && moleculeCreations == 0 && achievements == 0;
        }
    }

    private final Game game;
    private final double away;
    private final double rate;
    private final double total;
    private double done = 0;
    private boolean skipped = false;

    // Les compteurs au retour, pour dire à la fin ce que l'absence a ajouté.
    private final BigNum particlesBefore;
    private final BigNum atomsBefore;
    private final long fusionsBefore;
    private final long synthesesBefore;
    private final long elementsBefore;
    private final long explosionsBefore;
    private final BigNum spaceBefore;
    private final long creationsBefore;
    private final int achievementsBefore;

    /**
     * @param game        la partie, telle que la sauvegarde l'a rendue
     * @param awaySeconds temps écoulé depuis la sauvegarde, en secondes ; négatif (horloge reculée), il vaut zéro
     */
    public Absence(Game game, double awaySeconds) {
        this.game = game;
        this.away = Double.isNaN(awaySeconds) ? 0 : Math.max(0, awaySeconds);
        this.rate = game.offlineRate();
        this.total = counts(game, away) ? Math.min(away, MAX_SECONDS) * rate : 0;
        GameStats stats = game.stats();
        particlesBefore = stats.particlesCreated();
        atomsBefore = stats.atomsCreated();
        fusionsBefore = stats.fusions();
        synthesesBefore = stats.syntheses();
        elementsBefore = stats.elementsObtained();
        explosionsBefore = stats.timedExplosions();
        spaceBefore = game.state().space();
        creationsBefore = stats.moleculeCreations();
        achievementsBefore = game.state().achievements().size();
    }

    /** Vrai si une absence de cette durée compte pour cette partie : assez longue, et la partie commencée. */
    public static boolean counts(Game game, double awaySeconds) {
        return game.isStarted() && awaySeconds >= MIN_SECONDS;
    }

    /** Durée de l'absence, en secondes. */
    public double away() {
        return away;
    }

    /** Part du temps retenu qui est rejouée (0,4 pour 40 %). */
    public double rate() {
        return rate;
    }

    /** Temps de jeu à rejouer en tout, en secondes. */
    public double total() {
        return total;
    }

    /** Temps de jeu déjà rejoué, en secondes. */
    public double done() {
        return done;
    }

    /** Avancée du rattrapage, de 0 à 1. */
    public double progress() {
        return total <= 0 ? 1 : Math.min(1, done / total);
    }

    /** Vrai quand tout est rejoué, ou que le joueur a passé la fin. */
    public boolean isDone() {
        return skipped || done >= total;
    }

    /** Rejoue jusqu'à {@code seconds} secondes de jeu de plus. */
    public void advance(double seconds) {
        double target = Math.min(total, done + Math.max(0, seconds));
        while (!skipped && done < target) {
            double part = Math.min(STEP, target - done);
            game.tick(part);
            done += part;
        }
    }

    /**
     * Rejoue autant de temps de jeu que possible en {@code nanos} nanosecondes de temps réel : de
     * quoi rattraper l'absence entre deux images sans figer la fenêtre.
     */
    public void advanceFor(long nanos) {
        long end = System.nanoTime() + nanos;
        do {
            advance(STEP);
        } while (!isDone() && System.nanoTime() < end);
    }

    /** Le joueur ne veut pas attendre : ce qui reste à rejouer est abandonné. */
    public void skip() {
        skipped = done < total;
    }

    /** Ce que l'absence a rapporté jusqu'ici. */
    public Report report() {
        GameStats stats = game.stats();
        return new Report(away, Math.min(away, MAX_SECONDS), rate, done, skipped,
                gain(stats.particlesCreated(), particlesBefore), gain(stats.atomsCreated(), atomsBefore),
                stats.fusions() - fusionsBefore, stats.syntheses() - synthesesBefore,
                stats.elementsObtained() - elementsBefore, stats.timedExplosions() - explosionsBefore,
                gain(game.state().space(), spaceBefore), stats.moleculeCreations() - creationsBefore,
                Math.max(0, game.state().achievements().size() - achievementsBefore));
    }

    private static BigNum gain(BigNum now, BigNum before) {
        return now.gt(before) ? now.subtract(before) : BigNum.ZERO;
    }
}
