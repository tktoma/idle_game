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
 *       Les atomes se dépensent en améliorations définitives, qui multiplient les
 *       particules obtenues à chaque création.</li>
 * </ul>
 *
 * <p>Une fois l'amélioration de persistance achetée, le joueur peut confier au jeu l'achat
 * des améliorations payées en particules ({@link #setAutomated(String, boolean)}).
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

    /**
     * Pas de calcul maximal, en secondes, quand un bonus dépend du temps ou qu'un achat
     * automatique est actif : un long tick (retour après une absence) est alors découpé pour
     * que le bonus monte progressivement et que les achats se fassent au fil de l'eau.
     */
    private static final double MAX_STEP = 1.0;

    /**
     * Nombre maximal de niveaux qu'un achat automatique prend d'un coup. Sans cette limite,
     * un catalogue mal équilibré (gain plus fort que la hausse du coût) figerait le jeu.
     */
    private static final int MAX_AUTO_BUYS_PER_STEP = 100;

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
     * complète, {@link #particlesPerCreation()} particules sont ajoutées.
     *
     * <p>Sert aussi à la progression hors-ligne : au chargement, on appelle
     * {@code tick(secondesÉcoulées)}. Un seul gros tick donne le même résultat que
     * beaucoup de petits, à la seconde près quand un bonus dépend du temps.
     */
    public void tick(double dt) {
        if (dt < 0 || Double.isNaN(dt) || Double.isInfinite(dt)) {
            throw new IllegalArgumentException("Durée invalide : " + dt);
        }
        boolean automation = hasActiveAutomation();
        if (!automation && !hasTimeDependentBonus()) {
            step(dt);
            return;
        }
        double remaining = dt;
        do {
            double part = Math.min(remaining, MAX_STEP);
            step(part);
            if (automation) runAutomation();
            remaining -= part;
        } while (remaining > 0);
    }

    /** Avance de {@code dt} secondes en considérant tous les bonus comme constants. */
    private void step(double dt) {
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
        if (state.started()) {
            state.setTimeSinceFusion(state.timeSinceFusion() + dt);
        }
    }

    private boolean hasTimeDependentBonus() {
        for (Upgrade upgrade : upgrades.values()) {
            if (upgrade.effect() instanceof Effect.MultiplyByRunTime && state.levelOf(upgrade.id()) > 0) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------
    // L'automatisation
    // ------------------------------------------------------------------

    /** Vrai une fois l'amélioration de persistance achetée : les achats automatiques deviennent réglables. */
    public boolean isAutomationUnlocked() {
        return keepsUpgradesOnFusion();
    }

    /** Vrai si l'achat automatique de cette amélioration est activé. */
    public boolean isAutomated(String upgradeId) {
        return state.isAutomated(upgrade(upgradeId).id());
    }

    /**
     * Active ou coupe l'achat automatique d'une amélioration. Seules les améliorations payées
     * en particules peuvent être automatisées, et seulement une fois l'automatisation débloquée.
     *
     * @return {@code true} si le réglage a été pris en compte
     */
    public boolean setAutomated(String upgradeId, boolean automated) {
        Upgrade upgrade = upgrade(upgradeId);
        if (upgrade.resource() != Resource.PARTICLES || !isAutomationUnlocked()) return false;
        state.setAutomated(upgrade.id(), automated);
        return true;
    }

    private boolean hasActiveAutomation() {
        if (!isAutomationUnlocked()) return false;
        for (Upgrade upgrade : upgrades.values()) {
            if (state.isAutomated(upgrade.id())) return true;
        }
        return false;
    }

    /** Achète, dans l'ordre du catalogue, tout ce que les achats automatiques activés peuvent payer. */
    private void runAutomation() {
        for (Upgrade upgrade : upgrades.values()) {
            if (!state.isAutomated(upgrade.id())) continue;
            int bought = 0;
            while (bought < MAX_AUTO_BUYS_PER_STEP && buy(upgrade.id())) {
                bought++;
            }
        }
    }

    // ------------------------------------------------------------------
    // La production
    // ------------------------------------------------------------------

    /** Vitesse d'un générateur, en créations par seconde. */
    public BigNum speed() {
        double extra = speedExtraPerLevel();
        BigNum speed = BASE_SPEED;
        for (Upgrade upgrade : upgrades.values()) {
            if (upgrade.effect() instanceof Effect.MultiplySpeed multiply) {
                speed = speed.multiply(BigNum.of(multiply.perLevel() + extra).pow(state.levelOf(upgrade.id())));
            }
        }
        return speed;
    }

    /** Ce que les améliorations en atomes ajoutent au gain de chaque niveau de vitesse (0.02 = +2 points). */
    public double speedExtraPerLevel() {
        double extra = 0;
        for (Upgrade upgrade : upgrades.values()) {
            if (upgrade.effect() instanceof Effect.StrengthenSpeed strengthen) {
                extra += strengthen.extraPerLevel() * state.levelOf(upgrade.id());
            }
        }
        return extra;
    }

    /** Facteur appliqué au coût des générateurs par les améliorations en atomes (0.64 = −36 %). */
    public double generatorCostFactor() {
        double factor = 1;
        for (Upgrade upgrade : upgrades.values()) {
            if (upgrade.effect() instanceof Effect.DiscountGenerators discount) {
                factor *= Math.pow(discount.factorPerLevel(), state.levelOf(upgrade.id()));
            }
        }
        return factor;
    }

    /** Vrai si la fusion conserve les améliorations payées en particules (hors générateurs). */
    public boolean keepsUpgradesOnFusion() {
        for (Upgrade upgrade : upgrades.values()) {
            if (upgrade.effect() instanceof Effect.KeepUpgradesOnFusion && state.levelOf(upgrade.id()) > 0) {
                return true;
            }
        }
        return false;
    }

    /** Nombre de générateurs actifs : aucun avant {@link #start()}, puis le premier plus ceux achetés. */
    public int generatorCount() {
        if (!state.started()) return 0;
        int count = 1;
        for (Upgrade upgrade : upgrades.values()) {
            if (upgrade.effect() instanceof Effect.AddGenerator) {
                count += state.levelOf(upgrade.id());
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

    /** Particules obtenues à chaque création : une de base, multipliée par tous les bonus achetés. */
    public BigNum particlesPerCreation() {
        BigNum result = BigNum.ONE;
        for (Upgrade upgrade : upgrades.values()) {
            result = result.multiply(particlesMultiplier(upgrade.id(), state.levelOf(upgrade.id())));
        }
        return result;
    }

    /**
     * Multiplicateur de particules qu'apporte une amélioration à un niveau donné, dans la
     * situation actuelle (atomes créés, temps écoulé). Vaut 1 au niveau 0, et toujours 1
     * pour une amélioration qui n'agit pas sur les particules par création.
     *
     * <p>L'interface s'en sert pour afficher l'effet actuel d'une amélioration, ou ce
     * qu'elle donnerait une fois achetée.
     */
    public BigNum particlesMultiplier(String upgradeId, int level) {
        if (level <= 0) return BigNum.ONE;
        BigNum perLevel = switch (upgrade(upgradeId).effect()) {
            case Effect.MultiplyParticles multiply -> BigNum.of(multiply.perLevel());
            case Effect.MultiplyByAtoms byAtoms ->
                    BigNum.ONE.add(state.totalAtoms().multiply(byAtoms.perAtom()));
            case Effect.MultiplyByRunTime byTime ->
                    BigNum.of(1 + byTime.factor() * Math.sqrt(state.timeSinceFusion() / 60));
            case Effect.MultiplySpeed speed -> BigNum.ONE;
            case Effect.AddGenerator generator -> BigNum.ONE;
            case Effect.StrengthenSpeed strengthen -> BigNum.ONE;
            case Effect.DiscountGenerators discount -> BigNum.ONE;
            case Effect.KeepUpgradesOnFusion keep -> BigNum.ONE;
        };
        return perLevel.pow(level);
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
     * plus aucune particule, plus aucune amélioration payée en particules, sauf s'il possède
     * une amélioration {@link Effect.KeepUpgradesOnFusion}. Les améliorations payées en atomes
     * sont toujours conservées.
     *
     * @return {@code true} si la fusion a eu lieu
     */
    public boolean fuse() {
        if (!canFuse()) return false;
        state.setAtoms(state.atoms().add(ATOMS_PER_FUSION));
        state.setTotalAtoms(state.totalAtoms().add(ATOMS_PER_FUSION));
        state.setParticles(BigNum.ZERO);
        state.clearFormations();
        state.setTimeSinceFusion(0);
        boolean keep = keepsUpgradesOnFusion();
        for (Upgrade upgrade : upgrades.values()) {
            if (upgrade.resource() != Resource.PARTICLES) continue;
            // Les générateurs sont consommés par la fusion, quoi qu'il arrive.
            boolean fused = upgrade.effect() instanceof Effect.AddGenerator;
            if (fused || !keep) {
                state.setLevel(upgrade.id(), 0);
            }
        }
        return true;
    }

    // ------------------------------------------------------------------
    // Les améliorations
    // ------------------------------------------------------------------

    /** Toutes les améliorations, dans l'ordre du catalogue. */
    public Collection<Upgrade> upgrades() {
        return upgrades.values();
    }

    /** Les améliorations qui se paient avec une ressource donnée, dans l'ordre du catalogue. */
    public List<Upgrade> upgrades(Resource resource) {
        return upgrades.values().stream().filter(upgrade -> upgrade.resource() == resource).toList();
    }

    public int levelOf(String upgradeId) {
        return state.levelOf(upgrade(upgradeId).id());
    }

    /** Vrai quand l'amélioration a atteint son niveau maximal. */
    public boolean isMaxed(String upgradeId) {
        Upgrade upgrade = upgrade(upgradeId);
        return state.levelOf(upgrade.id()) >= upgrade.maxLevel();
    }

    /** Coût du prochain niveau de cette amélioration, dans sa ressource. */
    public BigNum costOf(String upgradeId) {
        Upgrade upgrade = upgrade(upgradeId);
        BigNum cost = upgrade.costAt(state.levelOf(upgrade.id()));
        if (upgrade.effect() instanceof Effect.AddGenerator) {
            double factor = generatorCostFactor();
            if (factor != 1) {
                cost = Upgrade.roundUp(cost.multiply(factor)).max(BigNum.ONE);
            }
        }
        return cost;
    }

    public boolean canBuy(String upgradeId) {
        Upgrade upgrade = upgrade(upgradeId);
        return state.started() && !isMaxed(upgradeId) && balance(upgrade.resource()).gte(costOf(upgradeId));
    }

    /**
     * Achète un niveau si la partie a démarré, que le joueur a de quoi payer
     * et que le maximum n'est pas atteint.
     *
     * @return {@code true} si l'achat a eu lieu
     */
    public boolean buy(String upgradeId) {
        if (!canBuy(upgradeId)) return false;
        Upgrade upgrade = upgrade(upgradeId);
        // max(0) : la soustraction de deux double proches peut donner -1e-15 au lieu de 0
        BigNum left = balance(upgrade.resource()).subtract(costOf(upgradeId)).max(BigNum.ZERO);
        switch (upgrade.resource()) {
            case PARTICLES -> state.setParticles(left);
            case ATOMS -> state.setAtoms(left);
        }
        state.setLevel(upgradeId, state.levelOf(upgradeId) + 1);
        return true;
    }

    /** Quantité disponible d'une ressource. */
    public BigNum balance(Resource resource) {
        return switch (resource) {
            case PARTICLES -> state.particles();
            case ATOMS -> state.atoms();
        };
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
