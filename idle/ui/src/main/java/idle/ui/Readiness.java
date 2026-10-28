package idle.ui;

import idle.core.Game;
import idle.core.darkmatter.Challenge;
import idle.core.darkmatter.DarkUpgrade;
import idle.core.sky.Cosmos;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Ce qui est prêt quelque part dans le jeu, pour la pastille des onglets et des sous-onglets : une
 * case de l'arbre payable, un défi ouvert et jamais réussi, le Big Bang possible, une sorte à
 * rassembler, un assemblage, un astre ou une échelle du cosmos à former.
 *
 * <p>Ce ne sont que des choses que le joueur fait une fois et qui l'attendent ailleurs que là où
 * il regarde : ce qui est prêt en permanence (une amélioration à acheter, une molécule à créer)
 * n'a pas de pastille, elle serait toujours allumée.
 *
 * <p>Le calcul parcourt des catalogues entiers : il n'est refait que deux fois par seconde, ou
 * dès que les molécules, les amas ou les astres ont changé, et toutes les pages lisent le même
 * résultat ({@link #of(Game)}).
 */
public final class Readiness {

    /** Le signe ajouté au nom d'un onglet où quelque chose est prêt. */
    static final String DOT = " ●";

    /**
     * Les sortes de pastilles (version 0.146), que les réglages montrent ou cachent une par une. Une pastille cachée ne
     * s'allume ni sur son sous-onglet ni sur l'onglet qui le porte.
     */
    public enum Kind {
        TREE("Arbre de matière noire", "Une case de l'arbre payable, ou le Big Bang possible."),
        CHALLENGES("Défis de matière noire", "Un défi ouvert, jamais réussi, quand aucun n'est en cours."),
        COLLIDER("Collisionneur", "Le quark top, une amélioration payable, le vide à ouvrir."),
        MILESTONES("Paliers du Big Bang", "Un palier ouvert qui peut se payer."),
        SKY("Ciel à former", "Une sorte à rassembler, un assemblage, un astre ou une échelle du cosmos à former."),
        COMET("Comète", "Une comète dans l'expansion : la seule pastille qui s'éteint seule."),
        PROBES("Sondes", "Un butin à prendre, ou une place libre pour une sonde."),
        SYSTEMS("Systèmes solaires", "Une planète et une étoile libres pour un système."),
        VOID("Le vide", "Une amélioration du vide payable.");

        private final String label;
        private final String detail;

        Kind(String label, String detail) {
            this.label = label;
            this.detail = detail;
        }

        public String label() {
            return label;
        }

        public String detail() {
            return detail;
        }
    }

    /** Les pastilles cachées dans les réglages : un réglage commun à toutes les pages, comme les signes des cartes. */
    private static final java.util.EnumSet<Kind> HIDDEN = java.util.EnumSet.noneOf(Kind.class);

    /** Recopie les pastilles cachées des réglages. */
    public static void setHidden(java.util.Set<Kind> hidden) {
        synchronized (HIDDEN) {
            HIDDEN.clear();
            HIDDEN.addAll(hidden);
        }
    }

    /** Vrai si cette pastille est allumée : quelque chose est prêt, et elle n'est pas cachée. */
    public static boolean shown(Kind kind, boolean ready) {
        if (!ready) return false;
        synchronized (HIDDEN) {
            return !HIDDEN.contains(kind);
        }
    }

    /** {@link #DOT} si quelque chose de cette sorte est prêt et que sa pastille n'est pas cachée, rien sinon. */
    public static String dot(Kind kind, boolean ready) {
        return dot(shown(kind, ready));
    }
    private static final long EVERY_NANOS = 500_000_000L;
    private static final Map<Game, Readiness> SHARED = new WeakHashMap<>();

    private final Game game;
    private long computedAt = 0;
    private boolean computed = false;
    /** L'état des molécules au dernier calcul : un rassemblement ou un astre formé le change, et le calcul est refait aussitôt. */
    private int moleculesVersion = -1;
    private boolean tree;
    private boolean challenges;
    private boolean bang;
    private boolean states;
    private boolean assemblies;
    private boolean bodies;
    private boolean cosmos;
    private boolean comet;

    Readiness(Game game) {
        this.game = game;
        synchronized (SHARED) {
            SHARED.put(game, this);
        }
    }

    /** Ce qui est prêt dans cette partie : le même objet pour toutes les pages. */
    public static Readiness of(Game game) {
        synchronized (SHARED) {
            Readiness shared = SHARED.get(game);
            return shared != null ? shared : new Readiness(game);
        }
    }

    /** {@link #DOT} si quelque chose est prêt, rien sinon. */
    public static String dot(boolean ready) {
        return ready ? DOT : "";
    }

    /** À appeler à chaque image : le calcul n'est refait que deux fois par seconde. */
    void frame() {
        long now = System.nanoTime();
        if (computed && now - computedAt < EVERY_NANOS && moleculesVersion == game.state().moleculesVersion()
                && comet == game.isCometVisible()) return;
        computedAt = now;
        compute();
    }

    /** Refait le calcul tout de suite : après un clic qui a changé ce qui est prêt. */
    public void compute() {
        computed = true;
        moleculesVersion = game.state().moleculesVersion();
        tree = false;
        challenges = false;
        bang = false;
        if (game.isDarkMatterUnlocked()) {
            for (DarkUpgrade upgrade : game.darkUpgrades()) {
                if (upgrade.inTree() && game.canBuyDark(upgrade.id())) tree = true;
            }
            if (game.activeChallenge() == null) {
                for (Challenge challenge : game.challenges()) {
                    if (game.isChallengeUnlocked(challenge.id()) && !game.isChallengeCompleted(challenge.id())) challenges = true;
                }
            }
            bang = game.canBigBang();
        }
        states = false;
        assemblies = false;
        bodies = false;
        cosmos = false;
        if (game.isBigBangUnlocked()) {
            states = game.substancesReady() > 0;
            assemblies = game.assembliesReady() > 0;
            bodies = game.bodiesReady() > 0;
            Cosmos next = game.nextCosmos();
            cosmos = next != null && game.canFormCosmos(next);
        }
        comet = game.isCometVisible();
    }

    /** Une case de l'arbre de matière noire est payable, ou le Big Bang est possible. */
    public boolean tree() {
        return tree || bang;
    }

    /** Un défi est ouvert, jamais réussi, et aucun n'est en cours. */
    public boolean challenges() {
        return challenges;
    }

    /** Quelque chose est prêt dans l'onglet Matière noire, et sa pastille n'est pas cachée. */
    boolean darkMatter() {
        return shown(Kind.TREE, tree || bang) || shown(Kind.CHALLENGES, challenges);
    }

    /** Une sorte de molécules peut être rassemblée. */
    public boolean states() {
        return states;
    }

    /** Un assemblage peut être formé. */
    public boolean assemblies() {
        return assemblies;
    }

    /** Un astre peut être formé. */
    public boolean bodies() {
        return bodies;
    }

    /** La prochaine échelle du cosmos peut être formée. */
    public boolean cosmos() {
        return cosmos;
    }

    /** Une comète traverse l'expansion : elle ne reste que quelques secondes, c'est la seule pastille qui s'éteint seule. */
    public boolean comet() {
        return comet;
    }

    /** Quelque chose est prêt dans l'onglet Big Bang, et sa pastille n'est pas cachée. */
    boolean bigBang() {
        return shown(Kind.SKY, states || assemblies || bodies || cosmos) || shown(Kind.COMET, comet);
    }
}
