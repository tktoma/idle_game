package idle.core;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.random.RandomGenerator;

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
 * <p>Une fois l'amélioration de persistance achetée, le joueur peut acheter des automatismes
 * ({@link Automation}) : le jeu achète alors lui-même les améliorations payées en particules,
 * voire fusionne tout seul.
 *
 * <p>Enfin, le joueur peut dépenser des atomes pour {@linkplain #synthesize() synthétiser}
 * un élément du tableau périodique tiré au sort. Le prix double à chaque synthèse, jusqu'à
 * atteindre {@link #MAX_ATOMS}.
 * Chaque élément possédé améliore un aspect du jeu ({@link #elementPower(Aspect)}).
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

    /**
     * Nombre maximal d'atomes qu'on peut posséder en même temps : le nombre d'éléments du
     * tableau périodique. Arrivé là, il faut en dépenser pour pouvoir fusionner de nouveau.
     */
    public static final BigNum MAX_ATOMS = BigNum.of(118);

    /**
     * Prix de la première synthèse d'élément, en atomes. Il double à chaque synthèse
     * (2, 4, 8, 16, 32, 64) jusqu'à être plafonné par {@link #MAX_ATOMS}.
     */
    public static final BigNum SYNTHESIS_BASE_COST = BigNum.of(2);

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

    /**
     * Rythme d'un automatisme « sans délai » : il agit dix fois par seconde de jeu. Sans ce
     * rythme fixe, il agirait une fois par image, et le jeu irait plus ou moins vite selon
     * la machine (et bien plus lentement hors-ligne qu'en direct).
     */
    private static final double INSTANT_AUTOMATION_STEP = 0.1;

    private final GameState state;
    private final Map<String, Upgrade> upgrades = new LinkedHashMap<>();
    private final Map<String, Automation> automations = new LinkedHashMap<>();
    private final RandomGenerator random;

    /** Nouvelle partie avec les catalogues par défaut. */
    public Game() {
        this(new GameState(), Upgrades.DEFAULT, Automations.DEFAULT);
    }

    /** Partie sans aucun automatisme au catalogue. */
    public Game(GameState state, List<Upgrade> catalog) {
        this(state, catalog, List.of());
    }

    /** Partie reprise depuis un état existant, avec un hasard imprévisible pour la synthèse. */
    public Game(GameState state, List<Upgrade> catalog, List<Automation> automationCatalog) {
        this(state, catalog, automationCatalog, new Random());
    }

    /**
     * Partie reprise depuis un état existant (sauvegarde, test…).
     *
     * @param random source de hasard de la synthèse ; les tests en passent une reproductible
     */
    public Game(GameState state, List<Upgrade> catalog, List<Automation> automationCatalog,
                RandomGenerator random) {
        this.state = state;
        this.random = random;
        for (Upgrade upgrade : catalog) {
            if (upgrades.put(upgrade.id(), upgrade) != null) {
                throw new IllegalArgumentException("Amélioration en double : " + upgrade.id());
            }
        }
        for (Automation automation : automationCatalog) {
            if (!automation.isFusion() && !upgrades.containsKey(automation.upgradeId())) {
                throw new IllegalArgumentException("Automatisme sans amélioration : " + automation.upgradeId());
            }
            if (automations.put(automation.id(), automation) != null) {
                throw new IllegalArgumentException("Automatisme en double : " + automation.id());
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
        double maxStep = automation ? INSTANT_AUTOMATION_STEP : MAX_STEP;
        double remaining = dt;
        do {
            double part = Math.min(remaining, maxStep);
            step(part);
            if (automation) runAutomation(part);
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

    /** Tous les automatismes, dans l'ordre du catalogue. */
    public Collection<Automation> automations() {
        return automations.values();
    }

    /** Vrai une fois l'amélioration de persistance achetée : les automatismes deviennent achetables. */
    public boolean isAutomationUnlocked() {
        return keepsUpgradesOnFusion();
    }

    /** Vrai si le joueur a acheté cet automatisme. */
    public boolean ownsAutomation(String automationId) {
        return state.ownsAutomation(automation(automationId).id());
    }

    public boolean canBuyAutomation(String automationId) {
        Automation automation = automation(automationId);
        return isAutomationUnlocked() && !state.ownsAutomation(automation.id())
                && state.atoms().gte(automation.cost());
    }

    /**
     * Achète un automatisme avec des atomes. Il est mis en marche aussitôt.
     *
     * @return {@code true} si l'achat a eu lieu
     */
    public boolean buyAutomation(String automationId) {
        if (!canBuyAutomation(automationId)) return false;
        Automation automation = automation(automationId);
        state.setAtoms(state.atoms().subtract(automation.cost()).max(BigNum.ZERO));
        state.addAutomation(automation.id());
        state.setAutomationEnabled(automation.id(), true);
        return true;
    }

    /** Vrai si cet automatisme est acheté et en marche. */
    public boolean isAutomationEnabled(String automationId) {
        Automation automation = automation(automationId);
        return state.ownsAutomation(automation.id()) && state.isAutomationEnabled(automation.id());
    }

    /**
     * Met en marche ou coupe un automatisme déjà acheté.
     *
     * @return {@code true} si le réglage a été pris en compte
     */
    public boolean setAutomationEnabled(String automationId, boolean enabled) {
        Automation automation = automation(automationId);
        if (!state.ownsAutomation(automation.id())) return false;
        state.setAutomationEnabled(automation.id(), enabled);
        return true;
    }

    private boolean hasActiveAutomation() {
        for (Automation automation : automations.values()) {
            if (isAutomationEnabled(automation.id())) return true;
        }
        return false;
    }

    /** Niveau de cadence acheté pour cet automatisme. */
    public int automationSpeedLevel(String automationId) {
        return state.automationSpeedLevel(automation(automationId).id());
    }

    /**
     * Délai actuel entre deux actions de cet automatisme, en secondes, bonus des éléments
     * compris. 0 = sans délai : il agit dès qu'il le peut.
     */
    public double automationInterval(String automationId) {
        Automation automation = automation(automationId);
        return automation.intervalAt(state.automationSpeedLevel(automation.id())) / elementPower(Aspect.AUTOMATION);
    }

    /** Vrai quand la cadence de cet automatisme ne peut plus être améliorée : il n'a plus de délai. */
    public boolean isAutomationInstant(String automationId) {
        Automation automation = automation(automationId);
        return state.automationSpeedLevel(automation.id()) >= automation.instantLevel();
    }

    /** Prix du prochain niveau de cadence de cet automatisme, en atomes. */
    public BigNum automationSpeedCost(String automationId) {
        Automation automation = automation(automationId);
        return automation.speedCostAt(state.automationSpeedLevel(automation.id()));
    }

    public boolean canSpeedUpAutomation(String automationId) {
        Automation automation = automation(automationId);
        return state.ownsAutomation(automation.id()) && !isAutomationInstant(automationId)
                && state.atoms().gte(automationSpeedCost(automationId));
    }

    /**
     * Achète un niveau de cadence pour un automatisme déjà possédé : son délai est divisé par
     * deux, puis supprimé au dernier niveau.
     *
     * @return {@code true} si l'achat a eu lieu
     */
    public boolean speedUpAutomation(String automationId) {
        if (!canSpeedUpAutomation(automationId)) return false;
        Automation automation = automation(automationId);
        state.setAtoms(state.atoms().subtract(automationSpeedCost(automationId)).max(BigNum.ZERO));
        state.setAutomationSpeedLevel(automation.id(), state.automationSpeedLevel(automation.id()) + 1);
        return true;
    }

    /**
     * Fait agir, dans l'ordre du catalogue, les automatismes en marche dont le délai est écoulé.
     * Un automatisme prêt qui n'a rien à faire (pas assez de particules) reste prêt : il agira
     * dès que ce sera possible, puis son délai repartira.
     */
    private void runAutomation(double dt) {
        for (Automation automation : automations.values()) {
            if (!isAutomationEnabled(automation.id())) continue;
            double interval = automationInterval(automation.id());
            double waited = state.automationTimer(automation.id()) + dt;
            if (interval <= 0) {
                // Sans délai : dix passes par seconde. À chaque passe, un achat automatique prend
                // tout ce qu'il peut payer ; une fusion automatique fusionne une fois.
                if (waited >= INSTANT_AUTOMATION_STEP) {
                    waited = Math.min(waited - INSTANT_AUTOMATION_STEP, INSTANT_AUTOMATION_STEP);
                    int actions = 0;
                    int limit = automation.isFusion() ? 1 : MAX_AUTO_BUYS_PER_STEP;
                    while (actions < limit && act(automation)) {
                        actions++;
                    }
                }
                state.setAutomationTimer(automation.id(), waited);
                continue;
            }
            int actions = 0;
            while (waited >= interval && actions < MAX_AUTO_BUYS_PER_STEP) {
                if (!act(automation)) {
                    waited = interval;
                    break;
                }
                waited -= interval;
                actions++;
            }
            state.setAutomationTimer(automation.id(), waited);
        }
    }

    /** Fait faire à l'automatisme une action ; faux s'il ne peut rien faire pour l'instant. */
    private boolean act(Automation automation) {
        return automation.isFusion() ? fuse() : buy(automation.upgradeId());
    }

    // ------------------------------------------------------------------
    // Le tableau périodique
    // ------------------------------------------------------------------

    /**
     * Prix de la prochaine synthèse, en atomes : {@link #SYNTHESIS_BASE_COST}, doublé à chaque
     * synthèse déjà faite, sans jamais dépasser {@link #MAX_ATOMS} puisqu'on ne peut pas posséder plus.
     */
    public BigNum synthesisCost() {
        return SYNTHESIS_BASE_COST.multiply(BigNum.of(2).pow(state.synthesisCount())).min(MAX_ATOMS);
    }

    /** Vrai quand le joueur a de quoi payer la prochaine synthèse. */
    public boolean canSynthesize() {
        return state.started() && state.atoms().gte(synthesisCost());
    }

    /**
     * Consomme {@link #synthesisCost()} atomes pour créer un élément du tableau périodique tiré au
     * sort : d'abord la famille, selon {@link ElementCategory#chance()}, puis un élément de
     * cette famille. Un élément déjà possédé gagne un exemplaire, et son bonus grandit d'autant.
     * Rien d'autre n'est perdu : améliorations et automatismes sont conservés.
     *
     * @return l'élément obtenu, ou {@code null} si la synthèse est impossible
     */
    public Element synthesize() {
        if (!canSynthesize()) return null;
        Element element = drawElement();
        state.setAtoms(state.atoms().subtract(synthesisCost()).max(BigNum.ZERO));
        state.setSynthesisCount(state.synthesisCount() + 1);
        state.setElementCount(element.number(), state.elementCount(element.number()) + 1);
        return element;
    }

    private Element drawElement() {
        double roll = random.nextDouble();
        ElementCategory chosen = ElementCategory.values()[ElementCategory.values().length - 1];
        double cumulative = 0;
        for (ElementCategory category : ElementCategory.values()) {
            cumulative += category.chance();
            if (roll < cumulative) {
                chosen = category;
                break;
            }
        }
        List<Element> family = PeriodicTable.elements(chosen);
        return family.get(random.nextInt(family.size()));
    }

    /** Nombre d'exemplaires possédés d'un élément. */
    public int elementCount(int atomicNumber) {
        return state.elementCount(PeriodicTable.element(atomicNumber).number());
    }

    /** Nombre d'éléments différents possédés, sur 118. */
    public int discoveredElements() {
        return state.elements().size();
    }

    /**
     * Puissance qu'apportent les éléments possédés à un aspect du jeu : 1 sans élément, puis
     * +{@link ElementCategory#bonusPerCopy()} par exemplaire des familles concernées. Les familles
     * qui améliorent {@link Aspect#ALL} comptent pour tous les aspects.
     *
     * <p>Pour une production (particules, vitesse, atomes), la valeur est multipliée par cette
     * puissance ; pour un coût ou un délai, elle est divisée par elle.
     */
    public double elementPower(Aspect aspect) {
        double power = 1;
        for (Map.Entry<Integer, Integer> owned : state.elements().entrySet()) {
            ElementCategory category = PeriodicTable.element(owned.getKey()).category();
            if (category.aspect() == aspect || category.aspect() == Aspect.ALL) {
                power += category.bonusPerCopy() * owned.getValue();
            }
        }
        return power;
    }

    // ------------------------------------------------------------------
    // La production
    // ------------------------------------------------------------------

    /** Vitesse d'un générateur, en créations par seconde. */
    public BigNum speed() {
        double extra = speedExtraPerLevel();
        BigNum speed = BASE_SPEED.multiply(elementPower(Aspect.SPEED));
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

    /**
     * Particules obtenues à chaque création : une de base, multipliée par tous les bonus achetés
     * et par celui des éléments.
     */
    public BigNum particlesPerCreation() {
        BigNum result = BigNum.of(elementPower(Aspect.PARTICLES));
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

    /**
     * Atomes gagnés à la prochaine fusion : {@link #ATOMS_PER_FUSION}, multiplié par le bonus des
     * éléments. Le résultat peut être fractionnaire : les fractions s'accumulent d'une fusion à l'autre.
     */
    public BigNum atomsPerFusion() {
        return ATOMS_PER_FUSION.multiply(elementPower(Aspect.ATOMS));
    }

    /**
     * Vrai quand tous les générateurs sont débloqués et qu'il reste de la place pour un atome :
     * ils peuvent fusionner.
     */
    public boolean canFuse() {
        return hasAllGenerators() && !isAtomCapReached();
    }

    /** Vrai quand tous les générateurs sont débloqués, que la fusion soit possible ou non. */
    public boolean hasAllGenerators() {
        return state.started() && generatorCount() >= maxGeneratorCount();
    }

    /** Vrai quand le joueur possède déjà {@link #MAX_ATOMS} atomes : aucune fusion tant qu'il n'en dépense pas. */
    public boolean isAtomCapReached() {
        return state.atoms().gte(MAX_ATOMS);
    }

    /**
     * Fusionne tous les générateurs en un atome.
     *
     * <p>Impossible tant qu'il manque des générateurs, ou si le joueur possède déjà
     * {@link #MAX_ATOMS} atomes. Sinon il gagne {@link #atomsPerFusion()} et repart du début : un seul générateur,
     * plus aucune particule, plus aucune amélioration payée en particules, sauf s'il possède
     * une amélioration {@link Effect.KeepUpgradesOnFusion}. Les améliorations payées en atomes
     * sont toujours conservées.
     *
     * @return {@code true} si la fusion a eu lieu
     */
    public boolean fuse() {
        if (!canFuse()) return false;
        BigNum gained = atomsPerFusion();
        state.setAtoms(state.atoms().add(gained).min(MAX_ATOMS));
        state.setTotalAtoms(state.totalAtoms().add(gained));
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
        double factor = 1;
        if (upgrade.effect() instanceof Effect.AddGenerator) {
            factor = generatorCostFactor() / elementPower(Aspect.GENERATOR_COST);
        } else if (upgrade.effect() instanceof Effect.MultiplySpeed) {
            factor = 1 / elementPower(Aspect.SPEED_COST);
        }
        if (factor != 1) {
            cost = Upgrade.roundUp(cost.multiply(factor)).max(BigNum.ONE);
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

    private Automation automation(String automationId) {
        Automation automation = automations.get(automationId);
        if (automation == null) throw new IllegalArgumentException("Automatisme inconnu : " + automationId);
        return automation;
    }

    private Upgrade upgrade(String upgradeId) {
        Upgrade upgrade = upgrades.get(upgradeId);
        if (upgrade == null) throw new IllegalArgumentException("Amélioration inconnue : " + upgradeId);
        return upgrade;
    }
}
