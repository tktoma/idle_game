package idle.core;

import java.util.ArrayList;
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
 *       forme les siennes à son rythme, indépendamment des autres. Une partie neuve
 *       n'en a aucun : le joueur crée le premier avec {@link #start()}, les suivants s'achètent ;</li>
 *   <li>les <b>atomes</b>, obtenus avec {@link #fuse()} : une fois tous les générateurs
 *       débloqués, ils fusionnent en un atome et la partie repart d'un seul générateur.
 *       Les atomes se dépensent en améliorations définitives.</li>
 * </ul>
 *
 * <p>Une fois l'amélioration de persistance achetée, le joueur peut acheter des automatismes
 * ({@link Automation}) : le jeu achète alors lui-même les améliorations payées en particules,
 * voire fusionne tout seul. Un automatisme agit à intervalles réguliers ; sa cadence s'améliore
 * avec des atomes jusqu'à un plafond, puis avec des éléments.
 *
 * <p>Enfin, une fois ces automatismes à leur cadence maximale, le tableau périodique se
 * débloque : le joueur dépense des atomes pour {@linkplain #synthesize() synthétiser} des
 * éléments tirés au sort. Chaque élément a son propre effet ({@link ElementEffect}), et le
 * premier élément unique obtenu débloque la synthèse automatique. Chaque élément a un nombre
 * maximal d'exemplaires : quand tous l'ont atteint, le tableau est complet.
 *
 * <p>Un tableau complet peut {@linkplain #explode() exploser} : toute la matière disparaît, la
 * partie repart du premier générateur, et il reste de la <b>matière noire</b>, la troisième ressource.
 * Le joueur la fait grossir ({@link #growDarkMatter(double)}), et elle ouvre un arbre
 * d'améliorations définitives ({@link DarkUpgrade}), payées en particules, en atomes, ou
 * débloquées par sa taille.
 *
 * <p>Les particules gardent leur intérêt jusqu'au bout grâce au rendement de fusion
 * ({@link #fusionYield()}) : plus la production est forte au moment de fusionner, plus la
 * fusion rapporte d'atomes.
 *
 * <p>L'interface n'a besoin que de trois choses :
 * <ul>
 *   <li>appeler {@link #tick(double)} régulièrement ;</li>
 *   <li>lire l'état pour l'afficher ({@link #state()}, {@link #speed(int)}, {@link #generatorCount()}) ;</li>
 *   <li>envoyer les actions du joueur ({@link #start()}, {@link #buy(String)}, {@link #fuse()},
 *       {@link #synthesize()}…).</li>
 * </ul>
 */
public final class Game {

    /** Vitesse de départ d'un générateur : une particule toutes les 4 secondes. */
    public static final BigNum BASE_SPEED = BigNum.of(0.25);

    /** Atomes gagnés à chaque fusion, avant les bonus. */
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

    /**
     * Délai minimal d'un automatisme, en secondes, quels que soient ses bonus : il n'agit jamais
     * plus de dix fois par seconde. C'est ce qui empêche le jeu de s'emballer en fin de partie,
     * et ce qui le fait tourner à la même vitesse sur toutes les machines.
     */
    public static final double MIN_AUTOMATION_INTERVAL = 0.1;

    /**
     * Tant qu'aucun élément unique n'a été obtenu, la synthèse de ce rang en donne un à coup sûr :
     * la synthèse automatique, qu'il débloque, ne dépend ainsi pas que de la chance.
     */
    public static final int GUARANTEED_UNIQUE_SYNTHESIS = 8;

    /**
     * Ce que les éléments peuvent ajouter, au plus, au gain d'un niveau de vitesse (0.10 = 10 points).
     * Si un niveau de vitesse rapportait plus que la hausse de son prix (+27 %), chaque niveau
     * paierait le suivant et la production s'emballerait sans fin. Avec les maximums d'exemplaires
     * actuels, les éléments ajoutent au plus 4,5 points : ce plafond est un garde-fou, au cas où
     * le tableau périodique changerait.
     */
    public static final double MAX_ELEMENT_SPEED_EXTRA = 0.10;

    /** Matière noire laissée par chaque explosion du tableau périodique. */
    public static final BigNum DARK_MATTER_PER_EXPLOSION = BigNum.ONE;

    /** Taille de départ de la matière noire, en mètres : celle d'un proton. */
    public static final BigNum DARK_MATTER_START_SIZE = BigNum.of(1, -15);

    /**
     * Élan que chaque unité de matière noire donne à la croissance de sa taille, par seconde
     * d'appui, avant le facteur d'avancement (0.003 = +0,3 % par seconde tant qu'elle a la taille
     * d'un proton). Voir {@link #darkMatterGrowthPerSecond()}. Réglé pour des parties d'un jour :
     * l'année-lumière est atteinte vers la cinquième explosion.
     */
    public static final double DARK_MATTER_GROWTH_PER_UNIT = 0.003;

    /**
     * Résistance de la matière noire à sa propre croissance : la vitesse est divisée par sa
     * taille (comptée en protons) élevée à cette puissance. Avec 0,1, elle est divisée par deux
     * chaque fois que la taille est multipliée par mille. C'est ce qui étale la croissance, du
     * proton à l'univers, sur toute la durée du jeu : doubler l'élan (matière noire, arbre,
     * avancement) ou le temps d'appui fait gagner trois ordres de grandeur, pas davantage.
     */
    public static final double DARK_MATTER_RESISTANCE = 0.1;

    /** Au-delà de ce nombre de particules par tick, on ne les compte plus une par une. */
    private static final long WHOLE_PARTICLES_MAX_EXPONENT = 15;

    /**
     * Pas de calcul maximal, en secondes, quand un bonus dépend du temps : un long tick
     * (retour après une absence) est alors découpé pour que le bonus monte progressivement.
     */
    private static final double MAX_STEP = 1.0;

    /** Nombre maximal d'actions d'un automatisme par pas de calcul : un garde-fou contre les boucles sans fin. */
    private static final int MAX_AUTO_ACTIONS_PER_STEP = 100;

    private final GameState state;
    private final Map<String, Upgrade> upgrades = new LinkedHashMap<>();
    private final Map<String, Automation> automations = new LinkedHashMap<>();
    private final Map<String, DarkUpgrade> darkUpgrades = new LinkedHashMap<>();
    private final Map<String, DarkAutomation> darkAutomations = new LinkedHashMap<>();
    private final RandomGenerator random;

    /** Bonus des éléments, recalculés seulement quand la collection change. */
    private ElementBonuses elementBonuses = ElementBonuses.of(Map.of());
    private int elementBonusesVersion = -1;

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
        this(state, catalog, automationCatalog, DarkUpgrades.DEFAULT, random);
    }

    /** Comme le précédent, avec un arbre d'améliorations de matière noire sur mesure. */
    public Game(GameState state, List<Upgrade> catalog, List<Automation> automationCatalog,
                List<DarkUpgrade> darkCatalog, RandomGenerator random) {
        this.state = state;
        this.random = random;
        for (Upgrade upgrade : catalog) {
            if (upgrades.put(upgrade.id(), upgrade) != null) {
                throw new IllegalArgumentException("Amélioration en double : " + upgrade.id());
            }
        }
        for (Automation automation : automationCatalog) {
            if (automation.kind() == Automation.Kind.UPGRADE && !upgrades.containsKey(automation.upgradeId())) {
                throw new IllegalArgumentException("Automatisme sans amélioration : " + automation.upgradeId());
            }
            if (automations.put(automation.id(), automation) != null) {
                throw new IllegalArgumentException("Automatisme en double : " + automation.id());
            }
        }
        for (DarkAutomation automation : DarkAutomations.DEFAULT) {
            darkAutomations.put(automation.id(), automation);
        }
        for (DarkUpgrade dark : darkCatalog) {
            if (dark.requires() != null && !darkUpgrades.containsKey(dark.requires())) {
                throw new IllegalArgumentException("Amélioration de matière noire sans sa case du dessus : " + dark.id());
            }
            if (darkUpgrades.put(dark.id(), dark) != null) {
                throw new IllegalArgumentException("Amélioration de matière noire en double : " + dark.id());
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
     * complète, {@link #particlesPerCreation(int)} particules sont ajoutées.
     *
     * <p>Sert aussi à la progression hors-ligne : au chargement, on appelle
     * {@code tick(secondesÉcoulées)}. Un seul gros tick donne le même résultat que
     * beaucoup de petits, à la seconde près quand un bonus dépend du temps.
     */
    public void tick(double dt) {
        if (dt < 0 || Double.isNaN(dt) || Double.isInfinite(dt)) {
            throw new IllegalArgumentException("Durée invalide : " + dt);
        }
        // La matière noire qui grossit seule : une petite part de ce que donnerait un appui.
        double autoShare = darkAutoExpansionShare();
        if (autoShare > 0) growDarkMatter(dt * autoShare);

        boolean automation = hasActiveAutomation();
        if (!automation && !hasTimeDependentBonus()) {
            step(dt);
            return;
        }
        // Avec un automatisme en marche, on avance au rythme de son délai minimal : il agit ainsi
        // au même moment que le jeu soit affiché à 20 ou 200 images par seconde, ou rattrapé hors-ligne.
        double maxStep = automation ? minAutomationInterval() : MAX_STEP;
        double remaining = dt;
        do {
            double part = Math.min(remaining, maxStep);
            step(part);
            if (automation) {
                runAutomation(part);
                runDarkAutomation(part);
            }
            remaining -= part;
        } while (remaining > 0);
    }

    /** Avance de {@code dt} secondes en considérant tous les bonus comme constants. */
    private void step(double dt) {
        int generators = generatorCount();
        // Calculés une fois pour tous les générateurs : seuls leurs bonus individuels diffèrent.
        BigNum commonSpeed = generators > 0 ? speed() : BigNum.ZERO;
        BigNum commonParticles = null;
        BigNum gain = BigNum.ZERO;
        for (int generator = 0; generator < generators; generator++) {
            BigNum creations = commonSpeed.multiply(generatorSpeedMultiplier(generator) * dt);
            double completed;
            if (creations.exponent() < WHOLE_PARTICLES_MAX_EXPONENT) {
                double total = state.formation(generator) + creations.toDouble();
                completed = Math.floor(total);
                state.setFormation(generator, Math.min(total - completed, Math.nextDown(1.0)));
                if (completed <= 0) continue;
            } else {
                completed = -1; // vitesse gigantesque : la création en cours n'a plus de sens
            }
            if (commonParticles == null) commonParticles = particlesPerCreation();
            BigNum each = commonParticles.multiply(generatorParticlesMultiplier(generator));
            gain = gain.add(completed < 0 ? creations.multiply(each) : each.multiply(completed));
        }
        if (!gain.isZero()) {
            state.setParticles(state.particles().add(gain));
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
        // Des automatismes conservés après une explosion restent utilisables sans racheter la persistance.
        return keepsUpgradesOnFusion() || !state.ownedAutomations().isEmpty();
    }

    /** Vrai une fois un élément unique obtenu : la synthèse automatique devient achetable. */
    public boolean isSynthesisAutomationUnlocked() {
        for (int number : state.elements().keySet()) {
            if (PeriodicTable.element(number).category().unique()) return true;
        }
        return false;
    }

    /**
     * Vrai si cet automatisme peut être acheté : la persistance pour tous, et en plus un élément
     * unique pour la synthèse automatique.
     */
    public boolean isAutomationAvailable(String automationId) {
        Automation automation = automation(automationId);
        if (!isAutomationUnlocked()) return false;
        return automation.kind() != Automation.Kind.SYNTHESIS || isSynthesisAutomationUnlocked();
    }

    /** Vrai si le joueur a acheté cet automatisme. */
    public boolean ownsAutomation(String automationId) {
        return state.ownsAutomation(automation(automationId).id());
    }

    public boolean canBuyAutomation(String automationId) {
        Automation automation = automation(automationId);
        return isAutomationAvailable(automationId) && !state.ownsAutomation(automation.id())
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
        for (DarkAutomation automation : darkAutomations.values()) {
            if (isDarkAutomationEnabled(automation.id())) return true;
        }
        return false;
    }

    /** Niveau de cadence acheté pour cet automatisme. */
    public int automationSpeedLevel(String automationId) {
        return state.automationSpeedLevel(automation(automationId).id());
    }

    /**
     * Délai actuel entre deux actions de cet automatisme, en secondes : celui de son niveau de
     * cadence, divisé par le bonus des éléments, sans jamais descendre sous {@link #minAutomationInterval()}.
     */
    public double automationInterval(String automationId) {
        Automation automation = automation(automationId);
        double interval = automation.intervalAt(state.automationSpeedLevel(automation.id()))
                / bonuses().automationDivisor(target(automation));
        return Math.max(minAutomationInterval(), interval);
    }

    /**
     * Délai en dessous duquel aucun automatisme ne descend : {@link #MIN_AUTOMATION_INTERVAL},
     * ou moins si l'arbre de matière noire l'a abaissé.
     */
    public double minAutomationInterval() {
        double min = MIN_AUTOMATION_INTERVAL;
        for (DarkUpgrade dark : darkUpgrades.values()) {
            if (dark.effect() instanceof DarkEffect.FasterAutomations faster && state.darkLevelOf(dark.id()) > 0) {
                min = Math.min(min, faster.minInterval());
            }
        }
        return min;
    }

    /** Vrai quand tous les niveaux de cadence de cet automatisme sont achetés. */
    public boolean isAutomationMaxed(String automationId) {
        Automation automation = automation(automationId);
        return state.automationSpeedLevel(automation.id()) >= automation.maxSpeedLevel();
    }

    /** Prix du prochain niveau de cadence de cet automatisme, en atomes. */
    public BigNum automationSpeedCost(String automationId) {
        Automation automation = automation(automationId);
        return automation.speedCostAt(state.automationSpeedLevel(automation.id()));
    }

    public boolean canSpeedUpAutomation(String automationId) {
        Automation automation = automation(automationId);
        return state.ownsAutomation(automation.id()) && !isAutomationMaxed(automationId)
                && state.atoms().gte(automationSpeedCost(automationId));
    }

    /**
     * Achète un niveau de cadence pour un automatisme déjà possédé : son délai est divisé par deux.
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

    /** L'automatisme vu par les éléments qui accélèrent les automatismes. */
    private ElementEffect.AutomationTarget target(Automation automation) {
        return switch (automation.kind()) {
            case FUSION -> ElementEffect.AutomationTarget.FUSION;
            case SYNTHESIS -> ElementEffect.AutomationTarget.SYNTHESIS;
            case UPGRADE -> upgrades.get(automation.upgradeId()).effect() instanceof Effect.AddGenerator
                    ? ElementEffect.AutomationTarget.GENERATORS
                    : ElementEffect.AutomationTarget.SPEED_UPGRADES;
        };
    }

    /**
     * Fait agir, dans l'ordre du catalogue, les automatismes en marche dont le délai est écoulé :
     * une action par délai. Un automatisme prêt qui n'a rien à faire (pas assez de particules)
     * reste prêt : il agira dès que ce sera possible, puis son délai repartira.
     */
    private void runAutomation(double dt) {
        for (Automation automation : automations.values()) {
            if (!isAutomationEnabled(automation.id())) continue;
            double interval = automationInterval(automation.id());
            double waited = state.automationTimer(automation.id()) + dt;
            int actions = 0;
            // La petite marge absorbe les erreurs d'arrondi : dix pas de 0,1 s font bien 1 s.
            while (waited >= interval - 1e-9 && actions < MAX_AUTO_ACTIONS_PER_STEP) {
                if (!act(automation)) {
                    waited = interval;
                    break;
                }
                waited = Math.max(0, waited - interval);
                actions++;
            }
            state.setAutomationTimer(automation.id(), waited);
        }
    }

    /** Fait faire à l'automatisme une action ; faux s'il ne peut rien faire pour l'instant. */
    private boolean act(Automation automation) {
        return switch (automation.kind()) {
            case UPGRADE -> buy(automation.upgradeId());
            case FUSION -> generatorCount() >= fusionThreshold() && fuse();
            case SYNTHESIS -> !synthesize().isEmpty();
        };
    }

    // ------------------------------------------------------------------
    // Le tableau périodique
    // ------------------------------------------------------------------

    /**
     * Prix de la prochaine synthèse, en atomes : {@link #SYNTHESIS_BASE_COST}, doublé à chaque
     * synthèse déjà faite, plafonné par {@link #MAX_ATOMS}, puis réduit par les éléments qui
     * baissent ce prix.
     */
    public BigNum synthesisCost() {
        BigNum cost = SYNTHESIS_BASE_COST.multiply(BigNum.of(2).pow(state.synthesisCount())).min(MAX_ATOMS);
        double divisor = bonuses().costDivisor(ElementEffect.CostTarget.SYNTHESIS);
        return divisor == 1 ? cost : Upgrade.roundUp(cost.divide(divisor)).max(BigNum.ONE);
    }

    /**
     * Vrai une fois achetés, et portés à leur cadence maximale, tous les automatismes autres que
     * la synthèse automatique : le tableau périodique devient accessible. Comme un automatisme ne
     * se perd pas et qu'une cadence ne redescend pas, le tableau reste débloqué pour de bon.
     */
    public boolean isPeriodicTableUnlocked() {
        boolean any = false;
        for (Automation automation : automations.values()) {
            if (automation.kind() == Automation.Kind.SYNTHESIS) continue;
            any = true;
            if (!state.ownsAutomation(automation.id()) || !isAutomationMaxed(automation.id())) {
                return false;
            }
        }
        return !any || isAutomationUnlocked();
    }

    /** Vrai quand le tableau périodique est débloqué et que le joueur a de quoi payer la prochaine synthèse. */
    public boolean canSynthesize() {
        return state.started() && isPeriodicTableUnlocked() && state.atoms().gte(synthesisCost())
                && !isPeriodicTableComplete();
    }

    /**
     * Vrai quand tous les éléments sont à leur nombre maximal d'exemplaires
     * ({@link #maxCopiesOf(ElementCategory)}) : il n'y a plus rien à synthétiser.
     */
    public boolean isPeriodicTableComplete() {
        for (ElementCategory category : ElementCategory.values()) {
            if (!availableElements(category).isEmpty()) return false;
        }
        return true;
    }

    /** Vrai quand cet élément a atteint son nombre maximal d'exemplaires : il ne sortira plus. */
    public boolean isElementMaxed(int atomicNumber) {
        Element element = PeriodicTable.element(atomicNumber);
        return state.elementCount(element.number()) >= maxCopiesOf(element.category());
    }

    /** Nombre total d'exemplaires possédés, tous éléments confondus, sans dépasser le maximum de chacun. */
    public int ownedCopies() {
        int copies = 0;
        for (Map.Entry<Integer, Integer> entry : state.elements().entrySet()) {
            copies += Math.min(entry.getValue(), maxCopiesOf(PeriodicTable.element(entry.getKey()).category()));
        }
        return copies;
    }

    /** Nombre total d'exemplaires du tableau de base, avant ce que l'arbre de matière noire y ajoute ({@link #maxTotalCopies()}). */
    public static int maxCopies() {
        int copies = 0;
        for (Element element : PeriodicTable.ELEMENTS) copies += element.category().maxCopies();
        return copies;
    }

    /**
     * Consomme {@link #synthesisCost()} atomes pour créer un élément du tableau périodique tiré au
     * sort : d'abord la famille, selon {@link #categoryChance(ElementCategory)}, puis un élément de
     * cette famille. Un élément déjà possédé gagne un exemplaire ; un élément qui a atteint son
     * maximum d'exemplaires (un seul pour un élément unique) ne peut pas retomber. Avec de la
     * chance ({@link #doubleDrawChance()}), un second élément est tiré, s'il en reste à obtenir.
     * Si aucun élément unique n'est sorti avant, la synthèse n° {@link #guaranteedUniqueSynthesis()}
     * en donne un à coup sûr.
     * Rien d'autre n'est perdu : améliorations et automatismes sont conservés.
     *
     * @return les éléments obtenus (un, parfois deux, davantage avec la matière noire), ou une liste vide si la synthèse est
     *         impossible (tableau verrouillé, tableau complet ou pas assez d'atomes)
     */
    public List<Element> synthesize() {
        if (!canSynthesize()) return List.of();
        state.setAtoms(state.atoms().subtract(synthesisCost()).max(BigNum.ZERO));
        state.setSynthesisCount(state.synthesisCount() + 1);

        double doubleChance = doubleDrawChance();   // la chance d'avant le tirage, pas celle qu'il donnerait
        boolean guaranteed = !isSynthesisAutomationUnlocked()
                && state.synthesisCount() >= guaranteedUniqueSynthesis();
        List<Element> obtained = new ArrayList<>();
        obtained.add(drawElement(guaranteed));
        // Les éléments en plus de la matière noire, tant qu'il en reste à obtenir.
        for (int extra = elementsPerSynthesis() - 1; extra > 0 && !isPeriodicTableComplete(); extra--) {
            obtained.add(drawElement(false));
        }
        if (random.nextDouble() < doubleChance && !isPeriodicTableComplete()) {
            obtained.add(drawElement(false));
        }
        return obtained;
    }

    /**
     * Tire un élément et l'ajoute à la collection.
     *
     * @param uniqueOnly vrai pour ne tirer que parmi les familles uniques
     */
    private Element drawElement(boolean uniqueOnly) {
        double total = 0;
        for (ElementCategory category : ElementCategory.values()) {
            if (!uniqueOnly || category.unique()) total += drawWeight(category);
        }
        double roll = random.nextDouble() * total;
        ElementCategory chosen = null;
        for (ElementCategory category : ElementCategory.values()) {
            double weight = uniqueOnly && !category.unique() ? 0 : drawWeight(category);
            if (weight <= 0) continue;
            chosen = category;
            roll -= weight;
            if (roll < 0) break;
        }
        List<Element> pool = availableElements(chosen);
        Element element = pool.get(random.nextInt(pool.size()));
        state.setElementCount(element.number(), state.elementCount(element.number()) + 1);
        return element;
    }

    /** Les éléments d'une famille qui peuvent encore sortir : ceux qui n'ont pas atteint leur maximum d'exemplaires. */
    private List<Element> availableElements(ElementCategory category) {
        return PeriodicTable.elements(category).stream()
                .filter(element -> state.elementCount(element.number()) < maxCopiesOf(category))
                .toList();
    }

    /** Poids d'une famille dans le tirage : sa chance de base, augmentée si elle est rare, nulle si elle est épuisée. */
    private double drawWeight(ElementCategory category) {
        if (availableElements(category).isEmpty()) return 0;
        return category.chance() * (category.rare() ? bonuses().luckMultiplier() : 1);
    }

    /** Probabilité actuelle qu'une synthèse tombe dans cette famille, chance et familles épuisées comprises. */
    public double categoryChance(ElementCategory category) {
        double total = 0;
        for (ElementCategory each : ElementCategory.values()) total += drawWeight(each);
        return total > 0 ? drawWeight(category) / total : 0;   // tableau complet : plus rien ne sort
    }

    /** Chance qu'une synthèse donne un second élément, entre 0 et 1. */
    public double doubleDrawChance() {
        return bonuses().doubleDrawChance();
    }

    /** Nombre d'exemplaires possédés d'un élément. */
    public int elementCount(int atomicNumber) {
        return state.elementCount(PeriodicTable.element(atomicNumber).number());
    }

    /** Nombre d'éléments différents possédés, sur 118. */
    public int discoveredElements() {
        return state.elements().size();
    }

    /** Le cumul des effets des éléments possédés, recalculé seulement quand la collection change. */
    private ElementBonuses bonuses() {
        if (elementBonusesVersion != state.elementsVersion()) {
            elementBonuses = ElementBonuses.of(state.elements());
            elementBonusesVersion = state.elementsVersion();
        }
        return elementBonuses;
    }

    /**
     * Ce que les éléments apportent à une grandeur, synergies comprises : le nombre par lequel
     * elle est multipliée (1 sans élément).
     */
    public double elementMultiplier(ElementEffect.Stat stat) {
        double synergy = 0;
        for (ElementBonuses.SynergyTerm term : bonuses().synergies()) {
            if (term.stat() == stat) synergy += term.perUnit() * synergyUnits(term.source());
        }
        return bonuses().stat(stat) * (1 + synergy);
    }

    /** Ce qu'une synergie compte en ce moment. */
    private double synergyUnits(ElementEffect.SynergySource source) {
        return switch (source) {
            case DISTINCT_ELEMENTS -> state.elements().size();
            case GENERATORS -> generatorCount();
            case AVAILABLE_ATOMS -> Math.floor(state.atoms().min(MAX_ATOMS).toDouble() + 1e-9);
            case AUTOMATIONS -> state.ownedAutomations().size();
            case SPEED_LEVELS -> {
                int levels = 0;
                for (Upgrade upgrade : upgrades.values()) {
                    if (upgrade.effect() instanceof Effect.MultiplySpeed) levels += state.levelOf(upgrade.id());
                }
                yield levels;
            }
        };
    }

    /** Ce que les éléments ajoutent à la vitesse d'un générateur précis (numéroté à partir de 0). */
    public double generatorSpeedMultiplier(int generator) {
        return bonuses().generatorSpeed(generator + 1);
    }

    /** Ce que les éléments ajoutent aux particules d'un générateur précis (numéroté à partir de 0). */
    public double generatorParticlesMultiplier(int generator) {
        return bonuses().generatorParticles(generator + 1);
    }

    /** Diviseur qu'appliquent les éléments à un prix (1 sans élément). */
    public double elementCostDivisor(ElementEffect.CostTarget target) {
        return bonuses().costDivisor(target);
    }

    // ------------------------------------------------------------------
    // L'explosion
    // ------------------------------------------------------------------

    /** Vrai quand le tableau périodique est complet : il peut exploser. */
    public boolean canExplode() {
        if (!state.started()) return false;
        if (isPeriodicTableComplete()) return true;
        // Avec la bonne case de l'arbre, il suffit d'avoir découvert tous les éléments.
        return hasDark(DarkEffect.ExplodeWhenDiscovered.class)
                && state.elements().size() == PeriodicTable.ELEMENTS.size();
    }

    /**
     * Fait exploser le tableau périodique complet.
     *
     * <p>Tout ce qui est fait de matière disparaît : particules, atomes, améliorations (même
     * celles payées en atomes), automatismes et éléments. La partie repart d'un seul générateur,
     * comme au tout début, sauf ce que l'arbre de matière noire permet de garder. En échange, le
     * joueur gagne {@link #darkMatterPerExplosion()} de matière noire, qu'aucune explosion ne lui
     * reprendra.
     *
     * @return {@code true} si l'explosion a eu lieu
     */
    public boolean explode() {
        if (!canExplode()) return false;
        // Ce que la matière noire permet de garder, mis de côté avant de tout effacer.
        Map<String, Integer> keptLevels = hasDark(DarkEffect.KeepUpgradesOnExplosion.class)
                ? state.upgradeLevels() : Map.of();
        boolean keepAutomations = hasDark(DarkEffect.KeepAutomationsOnExplosion.class);
        java.util.Set<String> owned = keepAutomations ? state.ownedAutomations() : java.util.Set.of();
        java.util.Set<String> enabled = keepAutomations ? state.enabledAutomations() : java.util.Set.of();
        List<Integer> keptElements = new ArrayList<>();
        if (hasDark(DarkEffect.KeepUniqueElementsOnExplosion.class)) {
            for (int number : state.elements().keySet()) {
                if (PeriodicTable.element(number).category().unique()) keptElements.add(number);
            }
        }
        Map<String, Integer> cadences = keepAutomations ? state.automationSpeedLevels() : Map.of();

        state.setDarkMatter(state.darkMatter().add(darkMatterPerExplosion()));
        state.setExplosions(state.explosions() + 1);
        state.clearMatter();

        for (Map.Entry<String, Integer> level : keptLevels.entrySet()) {
            Upgrade upgrade = upgrades.get(level.getKey());
            // Les générateurs sont de la matière : ils disparaissent toujours.
            if (upgrade != null && !(upgrade.effect() instanceof Effect.AddGenerator)) {
                state.setLevel(level.getKey(), level.getValue());
            }
        }
        for (String id : owned) {
            state.addAutomation(id);
            state.setAutomationEnabled(id, enabled.contains(id));
        }
        cadences.forEach(state::setAutomationSpeedLevel);
        for (int number : keptElements) state.setElementCount(number, 1);
        return true;
    }

    /** Matière noire que laissera la prochaine explosion : {@link #DARK_MATTER_PER_EXPLOSION}, plus ce qu'ajoute l'arbre. */
    public BigNum darkMatterPerExplosion() {
        BigNum amount = DARK_MATTER_PER_EXPLOSION;
        for (DarkUpgrade dark : darkUpgrades.values()) {
            if (dark.effect() instanceof DarkEffect.AddDarkMatterPerExplosion add) {
                amount = amount.add(BigNum.of(add.perLevel() * state.darkLevelOf(dark.id())));
            }
        }
        return amount;
    }

    /** Vrai une fois la première explosion déclenchée : la matière noire existe. */
    public boolean isDarkMatterUnlocked() {
        return state.explosions() > 0;
    }

    /**
     * Ce que l'avancement de la partie en cours apporte à la croissance de la matière noire :
     * {@code 1 + ordres de grandeur de la production + ordres de grandeur des atomes créés}.
     * Avec 10¹² particules par seconde et 1 000 atomes créés depuis la dernière explosion, le
     * facteur vaut 1 + 12 + 3 = 16. L'élan de la matière noire suit ainsi le rythme des particules
     * et des atomes : faible au début d'une partie, de plus en plus fort ensuite.
     *
     * <p>La production comptée est celle de tous les générateurs réunis
     * ({@link #productionAtFusion()}), pour ne pas retomber à chaque fusion.
     */
    public double darkMatterProgressFactor() {
        double production = Math.max(0, productionAtFusion().log10());
        double atoms = Math.max(0, state.totalAtoms().log10());
        return 1 + production + atoms;
    }

    /**
     * Élan de la croissance de la matière noire :
     * {@code DARK_MATTER_GROWTH_PER_UNIT × matière noire possédée × facteur d'avancement ×
     * multiplicateur de l'arbre}. C'est son taux de croissance par seconde d'appui quand elle a
     * la taille d'un proton ; ensuite sa résistance le divise.
     */
    public double darkMatterMomentum() {
        // Bornée pour rester calculable, bien au-delà de ce qu'un joueur peut posséder.
        double units = state.darkMatter().min(BigNum.of(1, 15)).toDouble();
        return DARK_MATTER_GROWTH_PER_UNIT * units * darkMatterProgressFactor()
                * darkExpansionMultiplier().min(BigNum.of(1, 100)).toDouble();
    }

    /**
     * Ce qui freine la croissance : la taille, comptée en protons, à la puissance
     * {@link #DARK_MATTER_RESISTANCE}. Vaut 1 au départ, 2 après trois ordres de grandeur, 4 après six.
     */
    public BigNum darkMatterResistance() {
        return state.darkMatterSize().divide(DARK_MATTER_START_SIZE).max(BigNum.ONE).pow(DARK_MATTER_RESISTANCE);
    }

    /**
     * Ce par quoi la taille de la matière noire est multipliée, en ce moment, à chaque seconde
     * d'appui : l'élan divisé par la résistance donne son taux de croissance. Plus elle est
     * grande, plus ce nombre se rapproche de 1.
     */
    public BigNum darkMatterGrowthPerSecond() {
        if (state.darkMatter().isZero()) return BigNum.ONE;
        double rate = BigNum.of(darkMatterMomentum()).divide(darkMatterResistance()).toDouble();
        return BigNum.pow10(rate / Math.log(10));     // e^taux, même quand le taux est énorme
    }

    /** Taille de la matière noire en années-lumière : la distance qu'elle a « parcourue » en grossissant. */
    public BigNum darkMatterLightYears() {
        return state.darkMatterSize().divide(SizeScale.LIGHT_YEAR);
    }

    /**
     * Fait grossir la matière noire comme si le joueur la maintenait appuyée pendant {@code seconds}
     * secondes. L'interface l'appelle à chaque image tant que le clic est maintenu. La taille ne
     * diminue jamais, et aucune explosion ne la remet à zéro.
     *
     * @return {@code true} si la matière noire a grossi (il en faut au moins un peu pour cela)
     */
    public boolean growDarkMatter(double seconds) {
        if (seconds < 0 || Double.isNaN(seconds) || Double.isInfinite(seconds)) {
            throw new IllegalArgumentException("Durée invalide : " + seconds);
        }
        if (!isDarkMatterUnlocked() || state.darkMatter().isZero() || seconds == 0) return false;
        // La résistance grandit avec la taille pendant l'appui. Le calcul est exact : la résistance
        // augmente de (puissance × élan) par seconde, et la taille s'en déduit. Un long appui donne
        // donc le même résultat que beaucoup de petits.
        BigNum resistance = darkMatterResistance()
                .add(BigNum.of(DARK_MATTER_RESISTANCE * darkMatterMomentum() * seconds));
        state.setDarkMatterSize(DARK_MATTER_START_SIZE.multiply(resistance.pow(1 / DARK_MATTER_RESISTANCE))
                .max(state.darkMatterSize()));
        return true;
    }

    // ------------------------------------------------------------------
    // Les automatismes de matière noire
    // ------------------------------------------------------------------

    /** Tous les automatismes de matière noire, dans l'ordre du catalogue. */
    public Collection<DarkAutomation> darkAutomations() {
        return darkAutomations.values();
    }

    /** Vrai quand le joueur possède assez de matière noire pour cet automatisme. Rien n'est dépensé. */
    public boolean isDarkAutomationUnlocked(String darkAutomationId) {
        return state.darkMatter().gte(darkAutomation(darkAutomationId).darkMatter());
    }

    /** Vrai si cet automatisme de matière noire est débloqué et en marche. */
    public boolean isDarkAutomationEnabled(String darkAutomationId) {
        DarkAutomation automation = darkAutomation(darkAutomationId);
        return state.isDarkAutomationEnabled(automation.id()) && isDarkAutomationUnlocked(darkAutomationId);
    }

    /**
     * Met en marche ou coupe un automatisme de matière noire débloqué.
     *
     * @return {@code true} si le réglage a été pris en compte
     */
    public boolean setDarkAutomationEnabled(String darkAutomationId, boolean enabled) {
        if (!isDarkAutomationUnlocked(darkAutomationId)) return false;
        state.setDarkAutomationEnabled(darkAutomation(darkAutomationId).id(), enabled);
        return true;
    }

    /** Fait agir les automatismes de matière noire en marche dont le délai est écoulé : une action par délai. */
    private void runDarkAutomation(double dt) {
        for (DarkAutomation automation : darkAutomations.values()) {
            if (!isDarkAutomationEnabled(automation.id())) continue;
            double waited = state.darkAutomationTimer(automation.id()) + dt;
            if (waited >= automation.interval() - 1e-9) {
                // Rien à faire pour l'instant : il reste prêt, et agira dès que ce sera possible.
                waited = act(automation) ? Math.max(0, waited - automation.interval()) : automation.interval();
            }
            state.setDarkAutomationTimer(automation.id(), waited);
        }
    }

    /** Fait faire à l'automatisme de matière noire une action ; faux s'il ne peut rien faire pour l'instant. */
    private boolean act(DarkAutomation automation) {
        return switch (automation.kind()) {
            case PARTICLE_UPGRADES -> playEarlyGame();
            case ATOM_UPGRADES -> buyCheapestAtomUpgrade();
            case AUTOMATIONS -> buyCheapestAutomation();
            case EXPLOSION -> explode();
        };
    }

    /**
     * Une action à la place du joueur : synthétiser, fusionner, sinon acheter une amélioration
     * payée en particules. Il laisse faire les automatismes ordinaires qui sont en marche.
     */
    private boolean playEarlyGame() {
        boolean fusionAutomated = false;
        boolean synthesisAutomated = false;
        java.util.Set<String> automated = new java.util.HashSet<>();
        for (Automation automation : automations.values()) {
            if (!isAutomationEnabled(automation.id())) continue;
            if (automation.kind() == Automation.Kind.FUSION) fusionAutomated = true;
            if (automation.kind() == Automation.Kind.SYNTHESIS) synthesisAutomated = true;
            if (automation.kind() == Automation.Kind.UPGRADE) automated.add(automation.upgradeId());
        }
        // Les premières synthèses, tant que la synthèse automatique n'est pas débloquée et en marche.
        if (!synthesisAutomated && !synthesize().isEmpty()) return true;
        if (!fusionAutomated && generatorCount() >= fusionThreshold() && fuse()) return true;
        // Les générateurs d'abord : ce sont eux qui mènent à la fusion.
        for (Upgrade upgrade : upgrades.values()) {
            if (upgrade.resource() == Resource.PARTICLES && upgrade.effect() instanceof Effect.AddGenerator
                    && !automated.contains(upgrade.id()) && buy(upgrade.id())) return true;
        }
        for (Upgrade upgrade : upgrades.values()) {
            if (upgrade.resource() == Resource.PARTICLES && !automated.contains(upgrade.id()) && buy(upgrade.id())) return true;
        }
        return false;
    }

    /** Achète l'amélioration payée en atomes la moins chère que le joueur peut s'offrir. */
    private boolean buyCheapestAtomUpgrade() {
        Upgrade cheapest = null;
        for (Upgrade upgrade : upgrades.values()) {
            if (upgrade.resource() != Resource.ATOMS || !canBuy(upgrade.id())) continue;
            if (cheapest == null || costOf(upgrade.id()).lt(costOf(cheapest.id()))) cheapest = upgrade;
        }
        return cheapest != null && buy(cheapest.id());
    }

    /** Achète l'automatisme ordinaire, ou le niveau de cadence, le moins cher que le joueur peut s'offrir. */
    private boolean buyCheapestAutomation() {
        Automation cheapest = null;
        BigNum cheapestCost = null;
        for (Automation automation : automations.values()) {
            BigNum cost;
            if (canBuyAutomation(automation.id())) {
                cost = automation.cost();
            } else if (canSpeedUpAutomation(automation.id())) {
                cost = automationSpeedCost(automation.id());
            } else {
                continue;
            }
            if (cheapestCost == null || cost.lt(cheapestCost)) {
                cheapest = automation;
                cheapestCost = cost;
            }
        }
        if (cheapest == null) return false;
        return state.ownsAutomation(cheapest.id()) ? speedUpAutomation(cheapest.id()) : buyAutomation(cheapest.id());
    }

    // ------------------------------------------------------------------
    // L'arbre de matière noire
    // ------------------------------------------------------------------

    /** Toutes les améliorations de matière noire, dans l'ordre du catalogue. */
    public Collection<DarkUpgrade> darkUpgrades() {
        return darkUpgrades.values();
    }

    public int darkLevelOf(String darkUpgradeId) {
        return state.darkLevelOf(darkUpgrade(darkUpgradeId).id());
    }

    public boolean isDarkMaxed(String darkUpgradeId) {
        DarkUpgrade dark = darkUpgrade(darkUpgradeId);
        return state.darkLevelOf(dark.id()) >= dark.maxLevel();
    }

    /**
     * Vrai si la case est accessible : la matière noire existe, le joueur en possède assez pour
     * cette case, et la case du dessus est acquise (s'il y en a une).
     */
    public boolean isDarkAvailable(String darkUpgradeId) {
        DarkUpgrade dark = darkUpgrade(darkUpgradeId);
        return isDarkMatterUnlocked() && hasDarkMatterFor(darkUpgradeId)
                && (dark.requires() == null || state.darkLevelOf(dark.requires()) > 0);
    }

    /** Vrai si le joueur possède la matière noire que demande cette case. */
    public boolean hasDarkMatterFor(String darkUpgradeId) {
        return state.darkMatter().gte(BigNum.of(darkUpgrade(darkUpgradeId).darkMatter()));
    }

    /** Prix du prochain niveau, dans l'unité de sa branche (particules, atomes, ou mètres à atteindre). */
    public BigNum darkCostOf(String darkUpgradeId) {
        DarkUpgrade dark = darkUpgrade(darkUpgradeId);
        return dark.costAt(state.darkLevelOf(dark.id()));
    }

    /** Ce que le joueur a dans l'unité d'une branche : particules, atomes disponibles, ou taille de la matière noire en mètres. */
    public BigNum darkBalance(DarkUpgrade.Branch branch) {
        return switch (branch) {
            case PARTICLES -> state.particles();
            case ATOMS -> state.atoms();
            case SIZE -> state.darkMatterSize();
        };
    }

    public boolean canBuyDark(String darkUpgradeId) {
        DarkUpgrade dark = darkUpgrade(darkUpgradeId);
        return isDarkAvailable(darkUpgradeId) && !isDarkMaxed(darkUpgradeId)
                && darkBalance(dark.branch()).gte(darkCostOf(darkUpgradeId));
    }

    /**
     * Achète un niveau d'une amélioration de matière noire. Les particules et les atomes sont
     * dépensés ; une taille à atteindre ne coûte rien, la matière noire ne rétrécit pas.
     *
     * @return {@code true} si l'achat a eu lieu
     */
    public boolean buyDark(String darkUpgradeId) {
        if (!canBuyDark(darkUpgradeId)) return false;
        DarkUpgrade dark = darkUpgrade(darkUpgradeId);
        BigNum cost = darkCostOf(darkUpgradeId);
        switch (dark.branch()) {
            case PARTICLES -> state.setParticles(state.particles().subtract(cost).max(BigNum.ZERO));
            case ATOMS -> state.setAtoms(state.atoms().subtract(cost).max(BigNum.ZERO));
            case SIZE -> { /* un seuil, pas un prix */ }
        }
        state.setDarkLevel(dark.id(), state.darkLevelOf(dark.id()) + 1);
        return true;
    }

    /** Vrai si le joueur possède au moins un niveau d'une amélioration de matière noire de ce type. */
    private boolean hasDark(Class<? extends DarkEffect> type) {
        for (DarkUpgrade dark : darkUpgrades.values()) {
            if (type.isInstance(dark.effect()) && state.darkLevelOf(dark.id()) > 0) return true;
        }
        return false;
    }

    /** Atomes de base ajoutés à chaque fusion par l'arbre. */
    public double darkAtomsPerFusion() {
        double added = 0;
        for (DarkUpgrade dark : darkUpgrades.values()) {
            if (dark.effect() instanceof DarkEffect.AddAtomsPerFusion add) {
                added += add.perLevel() * state.darkLevelOf(dark.id());
            }
        }
        return added;
    }

    /** Nombre d'éléments tirés à chaque synthèse, avant le tirage double : un, plus ceux de l'arbre. */
    public int elementsPerSynthesis() {
        int count = 1;
        for (DarkUpgrade dark : darkUpgrades.values()) {
            if (dark.effect() instanceof DarkEffect.AddElementsPerSynthesis add) {
                count += add.perLevel() * state.darkLevelOf(dark.id());
            }
        }
        return count;
    }

    /**
     * Ce que l'arbre multiplie dans les particules de chaque création : le multiplicateur de base,
     * et la taille de la matière noire en années-lumière (à partir d'une année-lumière).
     */
    public BigNum darkParticlesMultiplier() {
        BigNum result = BigNum.ONE;
        for (DarkUpgrade dark : darkUpgrades.values()) {
            int level = state.darkLevelOf(dark.id());
            if (level == 0) continue;
            if (dark.effect() instanceof DarkEffect.MultiplyBaseParticles multiply) {
                result = result.multiply(BigNum.of(multiply.perLevel()).pow(level));
            } else if (dark.effect() instanceof DarkEffect.ParticlesByDarkMatter byDarkMatter) {
                // Borné pour rester calculable, bien au-delà de ce qu'un joueur peut posséder.
                double units = state.darkMatter().min(BigNum.of(1, 15)).toDouble();
                result = result.multiply(BigNum.of(byDarkMatter.perUnit()).pow(units * level));
            } else if (dark.effect() instanceof DarkEffect.ParticlesByLightYears byLightYears) {
                BigNum lightYears = darkMatterLightYears();
                if (lightYears.gt(BigNum.ONE)) result = result.multiply(lightYears.pow(byLightYears.exponent() * level));
            }
        }
        return result;
    }

    /** Ce que l'arbre multiplie dans la vitesse de croissance de la matière noire. */
    public BigNum darkExpansionMultiplier() {
        BigNum result = BigNum.ONE;
        for (DarkUpgrade dark : darkUpgrades.values()) {
            if (dark.effect() instanceof DarkEffect.MultiplyExpansion multiply) {
                result = result.multiply(BigNum.of(multiply.perLevel()).pow(state.darkLevelOf(dark.id())));
            }
        }
        return result;
    }

    /** Part de la vitesse d'appui à laquelle la matière noire grossit seule (0 sans l'amélioration). */
    public double darkAutoExpansionShare() {
        double share = 0;
        for (DarkUpgrade dark : darkUpgrades.values()) {
            int level = state.darkLevelOf(dark.id());
            if (dark.effect() instanceof DarkEffect.AutoExpansion auto && level > 0) {
                share += auto.share() * Math.pow(auto.growth(), level - 1);
            }
        }
        return share;
    }

    /** Secondes d'appui que vaut chaque fusion pour la matière noire (0 sans l'amélioration). */
    public double darkFusionPulse() {
        double seconds = 0;
        for (DarkUpgrade dark : darkUpgrades.values()) {
            if (dark.effect() instanceof DarkEffect.FusionPulse pulse) {
                seconds += pulse.seconds() * state.darkLevelOf(dark.id());
            }
        }
        return seconds;
    }

    /** Niveaux de vitesse offerts par l'arbre : ils s'ajoutent à ceux achetés et ne se perdent jamais. */
    public int startingSpeedLevels() {
        int levels = 0;
        for (DarkUpgrade dark : darkUpgrades.values()) {
            if (dark.effect() instanceof DarkEffect.StartingSpeedLevels start) {
                levels += start.perLevel() * state.darkLevelOf(dark.id());
            }
        }
        return levels;
    }

    /** Rang de la synthèse qui donne un élément unique à coup sûr, tant qu'aucun n'est sorti. */
    public int guaranteedUniqueSynthesis() {
        int rank = GUARANTEED_UNIQUE_SYNTHESIS;
        for (DarkUpgrade dark : darkUpgrades.values()) {
            if (dark.effect() instanceof DarkEffect.EarlierGuaranteedUnique earlier && state.darkLevelOf(dark.id()) > 0) {
                rank = Math.min(rank, earlier.rank());
            }
        }
        return rank;
    }

    /**
     * Nombre maximal d'exemplaires d'un élément de cette famille, arbre de matière noire compris.
     * Les familles uniques restent à un exemplaire ; pour les autres, c'est la force maximale
     * (la racine du maximum) qui gagne un cran par niveau : 9 → 16 → 25.
     */
    public int maxCopiesOf(ElementCategory category) {
        if (category.unique()) return 1;
        int steps = 0;
        for (DarkUpgrade dark : darkUpgrades.values()) {
            if (dark.effect() instanceof DarkEffect.IncreaseMaxCopies more) {
                steps += more.perLevel() * state.darkLevelOf(dark.id());
            }
        }
        if (steps == 0) return category.maxCopies();
        int strength = (int) Math.round(Math.sqrt(category.maxCopies())) + steps;
        return strength * strength;
    }

    /** Nombre total d'exemplaires qu'on peut posséder en ce moment : le tableau est complet quand on les a tous. */
    public int maxTotalCopies() {
        int copies = 0;
        for (Element element : PeriodicTable.ELEMENTS) copies += maxCopiesOf(element.category());
        return copies;
    }

    /** Vrai quand l'arbre a débloqué le verrou de l'appui sur la matière noire. */
    public boolean isHoldLockUnlocked() {
        return hasDark(DarkEffect.HoldLock.class);
    }

    /** Vrai quand l'arbre a débloqué le réglage du seuil de fusion. */
    public boolean isFusionThresholdUnlocked() {
        return hasDark(DarkEffect.FusionThreshold.class);
    }

    /**
     * Nombre de générateurs que la fusion automatique attend avant de fusionner : celui qu'il
     * faut pour fusionner, ou le réglage du joueur une fois le seuil débloqué, ramené entre ce
     * minimum et la limite de générateurs. La fusion à la main, elle, reste possible dès le minimum.
     */
    public int fusionThreshold() {
        int minimum = generatorsPerAtom();
        if (!isFusionThresholdUnlocked() || state.fusionThreshold() == 0) return minimum;
        return Math.max(minimum, Math.min(state.fusionThreshold(), maxGeneratorCount()));
    }

    /**
     * Règle le seuil de la fusion automatique.
     *
     * @return {@code true} si le réglage a été pris en compte (il faut l'avoir débloqué)
     */
    public boolean setFusionThreshold(int generators) {
        if (!isFusionThresholdUnlocked()) return false;
        state.setFusionThreshold(Math.max(generatorsPerAtom(), Math.min(generators, maxGeneratorCount())));
        return true;
    }

    /** Vrai quand l'arbre a levé le plafond d'atomes. */
    public boolean isAtomCapLifted() {
        return hasDark(DarkEffect.UncapAtoms.class);
    }

    // ------------------------------------------------------------------
    // La production
    // ------------------------------------------------------------------

    /** Vitesse commune à tous les générateurs, en créations par seconde, avant leurs bonus individuels. */
    public BigNum speed() {
        double extra = speedExtraPerLevel();
        BigNum speed = BASE_SPEED.multiply(elementMultiplier(ElementEffect.Stat.SPEED));
        for (Upgrade upgrade : upgrades.values()) {
            if (upgrade.effect() instanceof Effect.MultiplySpeed multiply) {
                speed = speed.multiply(BigNum.of(multiply.perLevel() + extra)
                        .pow(state.levelOf(upgrade.id()) + startingSpeedLevels()));
            }
        }
        return speed;
    }

    /** Vitesse d'un générateur (numéroté à partir de 0), en créations par seconde. */
    public BigNum speed(int generator) {
        return speed().multiply(generatorSpeedMultiplier(generator));
    }

    /**
     * Ce qui s'ajoute au gain de chaque niveau de vitesse (0.02 = +2 points), grâce aux
     * améliorations en atomes et aux éléments.
     */
    public double speedExtraPerLevel() {
        double fromUpgrades = 0;
        double fromElements = bonuses().upgradeBoost(ElementEffect.UpgradeTarget.SPEED);
        double catalystBoost = bonuses().upgradeBoost(ElementEffect.UpgradeTarget.CATALYST);
        for (Upgrade upgrade : upgrades.values()) {
            if (upgrade.effect() instanceof Effect.StrengthenSpeed strengthen) {
                fromUpgrades += strengthen.extraPerLevel() * state.levelOf(upgrade.id());
                fromElements += catalystBoost * state.levelOf(upgrade.id());
            }
        }
        return fromUpgrades + Math.min(fromElements, MAX_ELEMENT_SPEED_EXTRA);
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
        int count = 1 + startingGenerators();
        for (Upgrade upgrade : upgrades.values()) {
            if (upgrade.effect() instanceof Effect.AddGenerator) {
                count += state.levelOf(upgrade.id());
            }
        }
        return Math.min(count, maxGeneratorCount());
    }

    /**
     * Générateurs offerts au départ de chaque partie par l'arbre de matière noire, en plus du
     * premier. Jamais plus qu'il n'en faut pour fusionner.
     */
    public int startingGenerators() {
        int bonus = 0;
        for (DarkUpgrade dark : darkUpgrades.values()) {
            if (dark.effect() instanceof DarkEffect.StartingGenerators start) {
                bonus += start.perLevel() * state.darkLevelOf(dark.id());
            }
        }
        return Math.min(bonus, generatorsPerAtom() - 1);
    }

    /**
     * Nombre maximal de générateurs : celui qu'il faut pour fusionner, ou la limite repoussée par
     * l'arbre de matière noire.
     */
    public int maxGeneratorCount() {
        int limit = generatorsPerAtom();
        for (DarkUpgrade dark : darkUpgrades.values()) {
            if (dark.effect() instanceof DarkEffect.UncapGenerators uncap && state.darkLevelOf(dark.id()) > 0) {
                limit = Math.max(limit, uncap.limit());
            }
        }
        return limit;
    }

    /**
     * Nombre de générateurs qu'il faut pour fusionner, et que consomme chaque atome de base :
     * le premier, plus tous ceux des améliorations du catalogue (dix dans le jeu).
     */
    public int generatorsPerAtom() {
        int count = 1;
        for (Upgrade upgrade : upgrades.values()) {
            if (upgrade.effect() instanceof Effect.AddGenerator && upgrade.hasLimit()) {
                count += upgrade.maxLevel();
            }
        }
        return count;
    }

    /**
     * Particules obtenues à chaque création, avant les bonus propres à chaque générateur :
     * une de base, multipliée par tous les bonus achetés et par ceux des éléments.
     */
    public BigNum particlesPerCreation() {
        BigNum result = BigNum.of(elementMultiplier(ElementEffect.Stat.PARTICLES)).multiply(darkParticlesMultiplier());
        for (Upgrade upgrade : upgrades.values()) {
            result = result.multiply(particlesMultiplier(upgrade.id(), state.levelOf(upgrade.id())));
        }
        return result;
    }

    /** Particules obtenues à chaque création d'un générateur précis (numéroté à partir de 0). */
    public BigNum particlesPerCreation(int generator) {
        return particlesPerCreation().multiply(generatorParticlesMultiplier(generator));
    }

    /**
     * Multiplicateur de particules qu'apporte une amélioration à un niveau donné, dans la
     * situation actuelle (atomes créés, temps écoulé, éléments qui la renforcent). Vaut 1 au
     * niveau 0, et toujours 1 pour une amélioration qui n'agit pas sur les particules par création.
     *
     * <p>L'interface s'en sert pour afficher l'effet actuel d'une amélioration, ou ce
     * qu'elle donnerait une fois achetée.
     */
    public BigNum particlesMultiplier(String upgradeId, int level) {
        if (level <= 0) return BigNum.ONE;
        ElementBonuses bonuses = bonuses();
        BigNum perLevel = switch (upgrade(upgradeId).effect()) {
            case Effect.MultiplyParticles multiply ->
                    BigNum.of(multiply.perLevel() + bonuses.upgradeBoost(ElementEffect.UpgradeTarget.DOUBLING));
            // Plafonné comme les atomes eux-mêmes : sans cela, le bonus grandirait sans fin
            // une fois les fusions automatisées.
            case Effect.MultiplyByAtoms byAtoms -> BigNum.ONE.add(state.totalAtoms().min(MAX_ATOMS).multiply(
                    byAtoms.perAtom() + bonuses.upgradeBoost(ElementEffect.UpgradeTarget.MASS)));
            case Effect.MultiplyByRunTime byTime -> BigNum.of(1
                    + (byTime.factor() + bonuses.upgradeBoost(ElementEffect.UpgradeTarget.PATIENCE))
                    * Math.sqrt(state.timeSinceFusion() / 60));
            case Effect.MultiplySpeed speed -> BigNum.ONE;
            case Effect.AddGenerator generator -> BigNum.ONE;
            case Effect.StrengthenSpeed strengthen -> BigNum.ONE;
            case Effect.DiscountGenerators discount -> BigNum.ONE;
            case Effect.KeepUpgradesOnFusion keep -> BigNum.ONE;
            case Effect.MultiplyAtomsByProduction byProduction -> BigNum.ONE;
        };
        return perLevel.pow(level);
    }

    /** Particules créées par seconde, tous générateurs confondus. */
    public BigNum productionPerSecond() {
        return production(generatorCount());
    }

    /**
     * Particules créées par seconde une fois tous les générateurs débloqués : la production
     * au moment d'une fusion. C'est elle que regarde {@link #fusionYield()}.
     */
    public BigNum productionAtFusion() {
        return production(Math.max(generatorCount(), generatorsPerAtom()));
    }

    /** Production des {@code generators} premiers générateurs, avec les bonus actuels. */
    private BigNum production(int generators) {
        if (generators <= 0) return BigNum.ZERO;
        // Calculés une fois : seuls les bonus propres à chaque générateur diffèrent.
        BigNum common = speed().multiply(particlesPerCreation());
        double individual = 0;
        for (int generator = 0; generator < generators; generator++) {
            individual += generatorSpeedMultiplier(generator) * generatorParticlesMultiplier(generator);
        }
        return common.multiply(individual);
    }

    // ------------------------------------------------------------------
    // La fusion
    // ------------------------------------------------------------------

    /**
     * Atomes gagnés à la prochaine fusion : {@link #ATOMS_PER_FUSION} plus ceux de l'arbre de
     * matière noire, multipliés par le bonus des éléments, par le
     * {@linkplain #fusionYield() rendement de fusion} et par le nombre de groupes de générateurs
     * ({@link #fusionGroups()}). Le résultat peut être fractionnaire : les fractions s'accumulent
     * d'une fusion à l'autre.
     */
    public BigNum atomsPerFusion() {
        return ATOMS_PER_FUSION.add(BigNum.of(darkAtomsPerFusion()))
                .multiply(elementMultiplier(ElementEffect.Stat.ATOMS))
                .multiply(fusionYield())
                .multiply(fusionGroups());
    }

    /**
     * Nombre de groupes complets de générateurs qu'une fusion consommerait maintenant : un tant
     * que leur nombre est limité, deux avec vingt générateurs, etc. Au moins un.
     */
    public int fusionGroups() {
        return Math.max(1, generatorCount() / generatorsPerAtom());
    }

    /**
     * Ce que la production de particules ajoute aux atomes d'une fusion, grâce aux améliorations
     * {@link Effect.MultiplyAtomsByProduction} : un multiplicateur, 1 sans elles ou sous leur seuil.
     */
    public double fusionYield() {
        double multiplier = 1;
        BigNum production = null;
        for (Upgrade upgrade : upgrades.values()) {
            if (upgrade.effect() instanceof Effect.MultiplyAtomsByProduction byProduction
                    && state.levelOf(upgrade.id()) > 0) {
                if (production == null) production = productionAtFusion();
                double decades = production.divide(byProduction.threshold()).log10();
                if (decades > 0) {
                    multiplier *= 1 + fusionYieldPerDecade(byProduction) * state.levelOf(upgrade.id()) * decades;
                }
            }
        }
        return multiplier;
    }

    /** Ce que rapporte chaque ×10 de production à une amélioration de rendement, éléments compris (0.5 = +50 %). */
    public double fusionYieldPerDecade(Effect.MultiplyAtomsByProduction byProduction) {
        return byProduction.perDecade() + bonuses().upgradeBoost(ElementEffect.UpgradeTarget.YIELD);
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
        return state.started() && generatorCount() >= generatorsPerAtom();
    }

    /**
     * Vrai quand le joueur possède déjà {@link #MAX_ATOMS} atomes : aucune fusion tant qu'il n'en
     * dépense pas. Toujours faux une fois le plafond levé par l'arbre de matière noire.
     */
    public boolean isAtomCapReached() {
        return !isAtomCapLifted() && state.atoms().gte(MAX_ATOMS);
    }

    /**
     * Fusionne tous les générateurs en un atome.
     *
     * <p>Impossible tant qu'il manque des générateurs, ou si le joueur possède déjà
     * {@link #MAX_ATOMS} atomes. Sinon il gagne {@link #atomsPerFusion()} et repart du début : un
     * seul générateur, plus aucune particule, plus aucune amélioration payée en particules, sauf
     * s'il possède une amélioration {@link Effect.KeepUpgradesOnFusion}. Les améliorations payées
     * en atomes sont toujours conservées.
     *
     * @return {@code true} si la fusion a eu lieu
     */
    public boolean fuse() {
        if (!canFuse()) return false;
        // L'onde de fusion : la matière noire grossit un peu, avec la production d'avant la remise à zéro.
        double pulse = darkFusionPulse();
        if (pulse > 0) growDarkMatter(pulse);
        BigNum gained = atomsPerFusion();
        BigNum atoms = state.atoms().add(gained);
        state.setAtoms(isAtomCapLifted() ? atoms : atoms.min(MAX_ATOMS));
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
        // Les générateurs : la limite est celle du moment, que la matière noire peut repousser.
        if (upgrade.effect() instanceof Effect.AddGenerator) return generatorCount() >= maxGeneratorCount();
        return state.levelOf(upgrade.id()) >= upgrade.maxLevel();
    }

    /** Coût du prochain niveau de cette amélioration, dans sa ressource, toutes réductions comprises. */
    public BigNum costOf(String upgradeId) {
        Upgrade upgrade = upgrade(upgradeId);
        BigNum cost = upgrade.costAt(state.levelOf(upgrade.id()));
        ElementBonuses bonuses = bonuses();
        double factor = 1;
        if (upgrade.effect() instanceof Effect.AddGenerator) {
            factor = generatorCostFactor();
        } else if (upgrade.effect() instanceof Effect.MultiplySpeed) {
            factor = 1 / bonuses.costDivisor(ElementEffect.CostTarget.SPEED_UPGRADES);
        } else if (upgrade.resource() == Resource.ATOMS) {
            factor = 1 / bonuses.costDivisor(ElementEffect.CostTarget.ATOM_UPGRADES);
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

    private DarkAutomation darkAutomation(String darkAutomationId) {
        DarkAutomation automation = darkAutomations.get(darkAutomationId);
        if (automation == null) throw new IllegalArgumentException("Automatisme de matière noire inconnu : " + darkAutomationId);
        return automation;
    }

    private DarkUpgrade darkUpgrade(String darkUpgradeId) {
        DarkUpgrade dark = darkUpgrades.get(darkUpgradeId);
        if (dark == null) throw new IllegalArgumentException("Amélioration de matière noire inconnue : " + darkUpgradeId);
        return dark;
    }

    private Upgrade upgrade(String upgradeId) {
        Upgrade upgrade = upgrades.get(upgradeId);
        if (upgrade == null) throw new IllegalArgumentException("Amélioration inconnue : " + upgradeId);
        return upgrade;
    }
}
