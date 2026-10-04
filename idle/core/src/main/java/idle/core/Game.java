package idle.core;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Les règles du jeu : comment les ressources sont créées et ce que font les améliorations.
 *
 * <p>Il y a deux ressources :
 * <ul>
 *   <li>les <b>particules</b>, créées en continu par les générateurs. Chaque générateur
 *       forme les siennes à la même vitesse, indépendamment des autres. Une partie neuve
 *       n'en a aucun : le joueur crée le premier avec {@link #start()}, les suivants s'achètent ;</li>
 *   <li>les <b>atomes</b>, obtenus avec {@link #fuse()} : une fois tous les générateurs
 *       débloqués, ils fusionnent en un atome et la partie repart d'un seul générateur.
 *       Chaque atome fait créer une particule de plus à chaque création.</li>
 * </ul>
 *
 * <p>L'interface n'a besoin que de trois choses :
 * <ul>
 *   <li>appeler {@link #tick(double)} régulièrement ;</li>
 *   <li>lire l'état pour l'afficher ({@link #state()}, {@link #speed()}, {@link #generatorCount()}) ;</li>
 *   <li>envoyer les actions du joueur ({@link #start()}, {@link #buy(String)}, {@link #fuse()}).</li>
 * </ul>
 */
public final class Game {

    /** Vitesse de départ d'un générateur : une particule toutes les 4 secondes. */
    public static final BigNum BASE_SPEED = BigNum.of(0.25);

    /** Atomes gagnés à chaque fusion. */
    public static final BigNum ATOMS_PER_FUSION = BigNum.ONE;

    /** Au-delà de ce nombre de particules par tick, on ne les compte plus une par une. */
    private static final long WHOLE_PARTICLES_MAX_EXPONENT = 15;

    private final GameState state;
    private final Map<String, Upgrade> upgrades = new LinkedHashMap<>();

    /** Nouvelle partie avec le catalogue par défaut. */
    public Game() {
        this(new GameState(), Upgrades.DEFAULT);
    }

    /** Partie reprise depuis un état existant (sauvegarde, test…). */
    public Game(GameState state, List<Upgrade> catalog) {
        this.state = state;
        for (Upgrade upgrade : catalog) {
            if (upgrades.put(upgrade.id(), upgrade) != null) {
                throw new IllegalArgumentException("Amélioration en double : " + upgrade.id());
            }
        }
    }

    // ------------------------------------------------------------------
    // Le démarrage
    // ------------------------------------------------------------------

    public boolean isStarted() {
        return state.started();
    }

    /** Crée le premier générateur. Sans effet si c'est déjà fait. */
    public void start() {
        state.setStarted(true);
    }

    // ------------------------------------------------------------------
    // Le temps
    // ------------------------------------------------------------------

    /**
     * Fait avancer le jeu de {@code dt} secondes.
     *
     * <p>Les particules ne montent pas en continu : chaque générateur remplit sa
     * création en cours ({@link GameState#formation(int)}), et chaque fois qu'elle est
     * complète, {@link #particlesPerCreation()} particules entières sont ajoutées.
     *
     * <p>Sert aussi à la progression hors-ligne : au chargement, on appelle
     * {@code tick(secondesÉcoulées)}. Comme la vitesse ne change qu'à l'achat
     * d'une amélioration, un seul gros tick donne le même résultat que mille petits.
     */
    public void tick(double dt) {
        if (dt < 0 || Double.isNaN(dt) || Double.isInfinite(dt)) {
            throw new IllegalArgumentException("Durée invalide : " + dt);
        }
        BigNum perGenerator = speed().multiply(dt);
        int generators = generatorCount();
        if (perGenerator.exponent() < WHOLE_PARTICLES_MAX_EXPONENT) {
            double gained = perGenerator.toDouble();
            double completed = 0;
            for (int generator = 0; generator < generators; generator++) {
                double total = state.formation(generator) + gained;
                double whole = Math.floor(total);
                completed += whole;
                state.setFormation(generator, Math.min(total - whole, Math.nextDown(1.0)));
            }
            if (completed > 0) {
                state.setParticles(state.particles().add(particlesPerCreation().multiply(completed)));
            }
        } else {
            // Vitesse gigantesque : la création en cours n'a plus de sens.
            state.setParticles(state.particles().add(
                    perGenerator.multiply(generators).multiply(particlesPerCreation())));
        }
        state.setTimePlayed(state.timePlayed() + dt);
    }

    /** Vitesse d'un générateur, en créations par seconde. */
    public BigNum speed() {
        BigNum speed = BASE_SPEED;
        for (Upgrade upgrade : upgrades.values()) {
            int level = state.levelOf(upgrade.id());
            switch (upgrade.effect()) {
                case Effect.MultiplySpeed multiply ->
                        speed = speed.multiply(BigNum.of(multiply.perLevel()).pow(level));
                case Effect.AddGenerator add -> { /* n'agit pas sur la vitesse */ }
            }
        }
        return speed;
    }

    /** Nombre de générateurs actifs : aucun avant {@link #start()}, puis le premier plus ceux achetés. */
    public int generatorCount() {
        if (!state.started()) return 0;
        int count = 1;
        for (Upgrade upgrade : upgrades.values()) {
            switch (upgrade.effect()) {
                case Effect.AddGenerator add -> count += state.levelOf(upgrade.id());
                case Effect.MultiplySpeed multiply -> { /* n'ajoute pas de générateur */ }
            }
        }
        return count;
    }

    /** Nombre de générateurs une fois toutes les améliorations au maximum. */
    public int maxGeneratorCount() {
        int count = 1;
        for (Upgrade upgrade : upgrades.values()) {
            if (upgrade.effect() instanceof Effect.AddGenerator && upgrade.hasLimit()) {
                count += upgrade.maxLevel();
            }
        }
        return count;
    }

    /** Particules obtenues à chaque création : une de base, plus une par atome possédé. */
    public BigNum particlesPerCreation() {
        return BigNum.ONE.add(state.atoms());
    }

    /** Particules créées par seconde, tous générateurs confondus. */
    public BigNum productionPerSecond() {
        return speed().multiply(generatorCount()).multiply(particlesPerCreation());
    }

    // ------------------------------------------------------------------
    // La fusion
    // ------------------------------------------------------------------

    /** Vrai quand tous les générateurs sont débloqués : ils peuvent fusionner en un atome. */
    public boolean canFuse() {
        return state.started() && generatorCount() >= maxGeneratorCount();
    }

    /**
     * Fusionne tous les générateurs en un atome.
     *
     * <p>Le joueur gagne {@link #ATOMS_PER_FUSION} et repart du début : un seul générateur,
     * plus aucune amélioration, plus aucune particule. En échange, chaque atome augmente
     * définitivement {@link #particlesPerCreation()}, donc la partie suivante va plus vite.
     *
     * @return {@code true} si la fusion a eu lieu
     */
    public boolean fuse() {
        if (!canFuse()) return false;
        state.setAtoms(state.atoms().add(ATOMS_PER_FUSION));
        state.setParticles(BigNum.ZERO);
        state.clearFormations();
        state.clearUpgradeLevels();
        return true;
    }

    // ------------------------------------------------------------------
    // Les améliorations
    // ------------------------------------------------------------------

    /** Toutes les améliorations, dans l'ordre du catalogue. */
    public Collection<Upgrade> upgrades() {
        return upgrades.values();
    }

    public int levelOf(String upgradeId) {
        return state.levelOf(upgrade(upgradeId).id());
    }

    /** Vrai quand l'amélioration a atteint son niveau maximal. */
    public boolean isMaxed(String upgradeId) {
        Upgrade upgrade = upgrade(upgradeId);
        return state.levelOf(upgrade.id()) >= upgrade.maxLevel();
    }

    /** Coût du prochain niveau de cette amélioration. */
    public BigNum costOf(String upgradeId) {
        Upgrade upgrade = upgrade(upgradeId);
        return upgrade.costAt(state.levelOf(upgrade.id()));
    }

    public boolean canBuy(String upgradeId) {
        return state.started() && !isMaxed(upgradeId) && state.particles().gte(costOf(upgradeId));
    }

    /**
     * Achète un niveau si la partie a démarré, que le joueur a assez de particules
     * et que le maximum n'est pas atteint.
     *
     * @return {@code true} si l'achat a eu lieu
     */
    public boolean buy(String upgradeId) {
        if (!canBuy(upgradeId)) return false;
        BigNum cost = costOf(upgradeId);
        // max(0) : la soustraction de deux double proches peut donner -1e-15 au lieu de 0
        state.setParticles(state.particles().subtract(cost).max(BigNum.ZERO));
        state.setLevel(upgradeId, state.levelOf(upgradeId) + 1);
        return true;
    }

    public GameState state() {
        return state;
    }

    private Upgrade upgrade(String upgradeId) {
        Upgrade upgrade = upgrades.get(upgradeId);
        if (upgrade == null) throw new IllegalArgumentException("Amélioration inconnue : " + upgradeId);
        return upgrade;
    }
}