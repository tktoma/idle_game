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
 * <p>Une fois {@link #UNLOCK_TOTAL_ATOMS} atomes créés, deux choses se débloquent ensemble.
 * D'abord les automatismes ({@link Automation}) : le jeu achète alors lui-même les améliorations
 * payées en particules, voire fusionne tout seul. Un automatisme agit à intervalles réguliers ;
 * sa cadence s'améliore avec des atomes jusqu'à un plafond, puis avec des éléments.
 *
 * <p>Ensuite le tableau périodique : le joueur dépense des atomes pour
 * {@linkplain #synthesize() synthétiser} des
 * éléments tirés au sort. Chaque élément a son propre effet ({@link ElementEffect}), et le
 * premier élément unique obtenu débloque la synthèse automatique. Chaque élément a un nombre
 * maximal d'exemplaires : quand tous l'ont atteint, le tableau est complet.
 *
 * <p>Un tableau dont les 118 éléments sont découverts peut {@linkplain #explode() exploser},
 * sans attendre d'être complet : toute la matière disparaît, la partie repart du premier
 * générateur, et il reste de la <b>matière noire</b>, la troisième ressource.
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
     * Particules à avoir en main pour le Big Bang. Arbre complet, les explosions s'enchaînent en
     * une demi-minute et la production gagne environ 75 ordres de grandeur à chacune : ce nombre
     * tombe une trentaine d'explosions après la fin de l'arbre.
     */
    public static final BigNum BIG_BANG_PARTICLES = BigNum.of(1, 3000);

    /**
     * Atomes à avoir en main pour le Big Bang : dix fois ce qu'une partie d'une demi-minute en
     * laisse quand l'arbre vient d'être fini. Il faut laisser tourner une partie, donc couper
     * l'explosion automatique.
     */
    public static final BigNum BIG_BANG_ATOMS = BigNum.of(1, 12);

    /**
     * Matière noire à avoir en réserve, sans la dépenser, pour le Big Bang : une cinquantaine
     * d'explosions à onze matières noires.
     */
    public static final BigNum BIG_BANG_DARK_MATTER = BigNum.of(500);

    /** Nombre de succès qu'il faut avoir obtenus pour le Big Bang, sur les cinquante du jeu. */
    public static final int BIG_BANG_ACHIEVEMENTS = 40;

    /** Espace que l'expansion de la matière ajoute chaque seconde, pour chaque Big Bang déclenché. */
    public static final double SPACE_PER_SECOND = 1;

    /**
     * Espace qu'occupe une molécule, par proton, avant ce que son état y ajoute
     * ({@link Molecule.State#spaceFactor()}) : une molécule de quartz (30 protons, un cristal) en
     * prend 300, une molécule d'eau (10 protons, un liquide) 200, une molécule de dihydrogène
     * (2 protons, un gaz) 400.
     */
    public static final double SPACE_PER_PROTON = 10;

    /** Nombre de molécules qu'une même sorte ne dépasse jamais : de quoi rester loin des limites de la machine. */
    public static final int MAX_MOLECULES = 1_000_000_000;

    /** Nombre de molécules d'une même sorte qu'il faut avoir créées pour les rassembler dans le lieu de leur état. */
    public static final int SUBSTANCE_MOLECULES = 3;

    /**
     * Ce par quoi chaque explosion multiplie la masse du tableau périodique : le plafond d'atomes
     * et le prix maximal d'une synthèse ({@link #atomCap()}). Sans cela, chaque partie demanderait
     * les mêmes 118 éléments au même prix pendant que l'arbre de matière noire multiplie tout, et
     * les parties tomberaient à une minute dès la cinquième explosion.
     */
    public static final double TABLE_WEIGHT_GROWTH = 3;

    /**
     * Nombre d'explosions au-delà duquel le tableau ne s'alourdit plus. Sans cette limite, un
     * joueur qui explose plus vite qu'il ne remplit l'arbre verrait chaque partie durer trois
     * fois plus que la précédente, sans fin. Dix : c'est à ce moment que l'arbre n'apporte plus
     * qu'une case par explosion, et un niveau de plus ferait passer une partie de trois heures
     * et demie à plus de huit.
     */
    public static final int TABLE_WEIGHT_MAX_LEVEL = 10;

    /**
     * Nombre d'atomes à avoir créés depuis le début de la partie (dépensés ou non) pour débloquer
     * à la fois l'automatisation et le tableau périodique.
     */
    public static final BigNum UNLOCK_TOTAL_ATOMS = BigNum.of(30);

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

    /** Ce que chaque groupe de générateurs au-delà du premier ajoute aux atomes d'une fusion (0.1 = +10 %). */
    public static final double FUSION_GROUP_BONUS = 0.10;

    /**
     * Part de la vitesse d'un appui que donne l'appui verrouillé : la matière noire grossit alors
     * seule, depuis n'importe quel onglet, mais moins vite que si le joueur tenait le clic.
     */
    public static final double HOLD_LOCK_SHARE = 0.6;

    /** Ce que chaque palier de taille « particules » multiplie dans les particules de chaque création. */
    public static final double LANDMARK_PARTICLES = 2;
    /** Ce que chaque palier de taille « atomes » ajoute aux atomes de chaque fusion (0.05 = +5 %). */
    public static final double LANDMARK_ATOMS = 0.05;
    /** Ce que chaque palier de taille « croissance » multiplie dans la croissance de la matière noire. */
    public static final double LANDMARK_EXPANSION = 1.1;

    /** Défi « Rouille » : facteur appliqué au délai de tous les automatismes. */
    public static final double RUSTY_FACTOR = 4;
    /** Défi « Rouille » : délai en dessous duquel aucun automatisme ne descend, en secondes. */
    public static final double RUSTY_MIN_INTERVAL = 0.5;
    /** Défi « Fusion lourde » : facteur appliqué au nombre de générateurs qu'il faut pour fusionner. */
    public static final int HEAVY_FUSION_FACTOR = 2;
    /** Défi « Particules volatiles » : part des particules possédées qui disparaît chaque seconde. */
    public static final double VOLATILE_LOSS_PER_SECOND = 0.50;
    /**
     * Défi « Désintégration » : chance qu'a chaque exemplaire de disparaître, par seconde. À
     * 0,02, sa demi-vie est de 35 secondes : un tableau complet de 811 exemplaires en perd seize
     * par seconde, et il faut synthétiser plus vite que cela pour le finir.
     */
    public static final double DECAY_RATE = 0.02;

    /** Récompense de « Rouille » : le délai des automatismes ordinaires est divisé par ce nombre. */
    public static final double REWARD_AUTOMATION_DIVISOR = 1.25;
    /** Récompense de « Fusion lourde » : ce que vaut alors la prime de groupe, à la place de {@link #FUSION_GROUP_BONUS}. */
    public static final double REWARD_GROUP_BONUS = 0.20;
    /** Récompense de « Particules volatiles » : ce qui s'ajoute au multiplicateur de chaque palier de vitesse. */
    public static final double REWARD_MILESTONE_EXTRA = 1;
    /** Récompense d'« Amnésie » : les améliorations payées en atomes coûtent ce nombre de fois moins. */
    public static final double REWARD_ATOM_UPGRADE_DIVISOR = 2;
    /** Récompense d'« Éléments inertes » : la force de chaque élément est multipliée par ce nombre. */
    public static final double REWARD_ELEMENT_STRENGTH = 1.15;
    /** Récompense de « Désintégration » : éléments en plus à chaque synthèse. */
    public static final int REWARD_EXTRA_ELEMENTS = 1;

    /** Ce que chaque succès obtenu ajoute aux particules de chaque création (0.01 = +1 %). Les succès s'additionnent. */
    public static final double ACHIEVEMENT_PARTICLES = 0.01;
    /** Secondes de jeu entre deux vérifications des succès : inutile de les regarder à chaque image. */
    private static final double ACHIEVEMENT_CHECK_SECONDS = 1;

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

    /** Nombre maximal de niveaux achetés par un achat groupé : un garde-fou, jamais atteint en jeu. */
    public static final int MAX_BULK = 10_000;

    private final GameState state;
    private final Map<String, Upgrade> upgrades = new LinkedHashMap<>();
    private final Map<String, Automation> automations = new LinkedHashMap<>();
    private final Map<String, DarkUpgrade> darkUpgrades = new LinkedHashMap<>();
    private final Map<String, Molecule> molecules = new LinkedHashMap<>();
    private final List<Molecule> moleculeList = Molecules.DEFAULT;
    private final Map<String, SpaceUpgrade> spaceUpgrades = new LinkedHashMap<>();
    private final Map<String, Assembly> assemblies = new LinkedHashMap<>();
    private final Map<String, Body> bodies = new LinkedHashMap<>();
    /** Pour chaque assemblage : l'astre qui est fait de lui. */
    private final Map<String, Body> bodyByAssembly = new java.util.HashMap<>();
    /** Les catalogues en listes, faites une fois : ils ne changent pas après la construction, et sont demandés à chaque image. */
    private List<SpaceUpgrade> spaceUpgradeList;
    private List<Assembly> assemblyList;
    private List<Body> bodyList;
    /** Le nombre de molécules rassemblées dans chaque état, recalculé seulement quand leur liste change. */
    private final Map<Molecule.State, Integer> gatheredByState = new java.util.EnumMap<>(Molecule.State.class);
    private int gatheredByStateVersion = -1;
    /** L'espace occupé par les molécules, recalculé seulement quand leur liste change. */
    private BigNum occupiedSpace = BigNum.ZERO;
    private int occupiedSpaceVersion = -1;
    private BigNum reservedSpace = BigNum.ZERO;
    private int reservedSpaceVersion = -1;
    /** Ce que donnent les molécules créées, recalculé seulement quand leur liste change. */
    private final Map<Molecule.Stat, Double> moleculeBoosts = new java.util.EnumMap<>(Molecule.Stat.class);
    private final Map<Integer, Integer> elementUncaps = new java.util.HashMap<>();
    private int moleculeBonusesVersion = -1;
    /** Ce par quoi les améliorations d'espace acquises multiplient chaque grandeur, recalculé avec les bonus des molécules. */
    private final Map<Molecule.Stat, Double> upgradeBoosts = new java.util.EnumMap<>(Molecule.Stat.class);
    private final Map<String, DarkAutomation> darkAutomations = new LinkedHashMap<>();
    private final Map<String, Achievement> achievements = new LinkedHashMap<>();
    /** Les succès obtenus depuis le dernier appel à {@link #takeNewAchievements()}, pour les annoncer. */
    private final List<Achievement> newAchievements = new ArrayList<>();
    private double achievementTimer = 0;
    /** Le cumul des bonus propres des succès, recalculé quand leur nombre change. */
    private final java.util.EnumMap<Achievement.Bonus, Double> achievementBonuses = new java.util.EnumMap<>(Achievement.Bonus.class);
    private int achievementBonusesFor = -1;
    private final RandomGenerator random;

    /** Bonus des éléments, recalculés seulement quand la collection change. */
    private ElementBonuses elementBonuses = ElementBonuses.of(Map.of());
    private int elementBonusesVersion = -1;
    private double elementBonusesStrength = 1;
    /** Les bonus d'une collection vide : ceux du défi où les éléments sont inertes. */
    private static final ElementBonuses NO_BONUSES = ElementBonuses.of(Map.of());

    /** Nouvelle partie avec les catalogues par défaut, succès compris. */
    public Game() {
        this(new GameState(), Upgrades.DEFAULT, Automations.DEFAULT, DarkUpgrades.DEFAULT, Achievements.DEFAULT, new Random());
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

    /** Comme le précédent, avec un arbre d'améliorations de matière noire sur mesure. Sans succès. */
    public Game(GameState state, List<Upgrade> catalog, List<Automation> automationCatalog,
                List<DarkUpgrade> darkCatalog, RandomGenerator random) {
        this(state, catalog, automationCatalog, darkCatalog, List.of(), random);
    }

    /**
     * Partie complète, avec son catalogue de succès. Les autres constructeurs n'en ont aucun :
     * leurs parties ne reçoivent donc aucun bonus de succès, ce qui garde les tests et les
     * simulations indépendants du catalogue.
     */
    public Game(GameState state, List<Upgrade> catalog, List<Automation> automationCatalog,
                List<DarkUpgrade> darkCatalog, List<Achievement> achievementCatalog, RandomGenerator random) {
        this.state = state;
        this.random = random;
        for (Achievement achievement : achievementCatalog) {
            if (achievements.put(achievement.id(), achievement) != null) {
                throw new IllegalArgumentException("Succès en double : " + achievement.id());
            }
        }
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
        for (SpaceUpgrade upgrade : SpaceUpgrades.DEFAULT) {
            if (upgrade.requires() != null && !spaceUpgrades.containsKey(upgrade.requires())) {
                throw new IllegalArgumentException("Amélioration d'espace sans celle dont elle dépend : " + upgrade.id());
            }
            if (spaceUpgrades.put(upgrade.id(), upgrade) != null) {
                throw new IllegalArgumentException("Amélioration d'espace en double : " + upgrade.id());
            }
        }
        for (Molecule molecule : Molecules.DEFAULT) {
            if (molecules.put(molecule.id(), molecule) != null) {
                throw new IllegalArgumentException("Molécule en double : " + molecule.id());
            }
        }
        for (Assembly assembly : Assemblies.DEFAULT) {
            if (assembly.ingredients().size() < 2) {
                throw new IllegalArgumentException("Un assemblage réunit au moins deux sortes de molécules : " + assembly.id());
            }
            for (String ingredient : assembly.ingredients().keySet()) {
                Molecule molecule = molecules.get(ingredient);
                if (molecule == null) throw new IllegalArgumentException(assembly.id() + " demande une molécule inconnue : " + ingredient);
                if (!molecule.hasState()) throw new IllegalArgumentException(assembly.id() + " demande une molécule qui ne se rassemble pas : " + ingredient);
            }
            if (assemblies.put(assembly.id(), assembly) != null) {
                throw new IllegalArgumentException("Assemblage en double : " + assembly.id());
            }
        }
        for (Body body : Bodies.DEFAULT) {
            // Un astre part d'astres plus petits, déjà décrits : le catalogue va donc du plus petit au plus grand.
            for (String needed : body.bodies()) {
                Body smaller = bodies.get(needed);
                if (smaller == null) throw new IllegalArgumentException(body.id() + " demande un astre inconnu ou décrit après lui : " + needed);
                if (smaller.tier().compareTo(body.tier()) >= 0) {
                    throw new IllegalArgumentException(body.id() + " demande un astre qui n'est pas plus petit que lui : " + needed);
                }
            }
            for (String needed : body.assemblies()) {
                if (!assemblies.containsKey(needed)) throw new IllegalArgumentException(body.id() + " demande un assemblage inconnu : " + needed);
                // Un assemblage entre dans un seul astre : c'est ce qui permet à l'astre de s'en faire un corps.
                if (bodyByAssembly.put(needed, body) != null) {
                    throw new IllegalArgumentException("L'assemblage " + needed + " est utilisé par deux astres, dont " + body.id());
                }
            }
            for (String needed : body.molecules().keySet()) {
                if (!molecules.containsKey(needed)) throw new IllegalArgumentException(body.id() + " demande une molécule inconnue : " + needed);
            }
            if (bodies.put(body.id(), body) != null) throw new IllegalArgumentException("Astre en double : " + body.id());
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

    /**
     * Recommence le jeu de zéro : tout est effacé, jusqu'à la matière noire, son arbre et les
     * statistiques. Il faudra créer de nouveau le premier générateur ({@link #start()}).
     */
    public void reset() {
        state.reset();
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
        if (autoShare > 0) expandDarkMatter(dt * autoShare);
        // L'appui verrouillé : elle grossit comme si le joueur tenait le clic, quel que soit l'onglet affiché.
        if (isHoldLocked()) growDarkMatter(dt * HOLD_LOCK_SHARE);
        // L'appui automatique, acquis avec l'espace : il tient le clic en entier, ou ce que le verrou laisse.
        if (isAutoHolding()) growDarkMatter(dt * (isHoldLocked() ? 1 - HOLD_LOCK_SHARE : 1));

        expandSpace(dt);
        runMoleculeAutomation(dt);
        advance(dt);
        decay(dt);
        achievementTimer += dt;
        if (achievementTimer >= ACHIEVEMENT_CHECK_SECONDS) {
            achievementTimer = 0;
            checkAchievements();
        }
        noteUnlocks();
        recordHistory();
    }

    /** Fait passer le temps : la production, puis les automatismes à leur rythme. */
    private void advance(double dt) {
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
        if (ruled(Challenge.Rule.VOLATILE_PARTICLES) && dt > 0) {
            // Les particules s'évaporent en continu. Ce qui reste de celles d'avant, plus ce qui reste
            // de celles créées pendant ce pas, comme si elles étaient arrivées régulièrement.
            double rate = -Math.log(1 - VOLATILE_LOSS_PER_SECOND);
            double kept = Math.exp(-rate * dt);
            state.setParticles(state.particles().multiply(kept).add(gain.multiply((1 - kept) / (rate * dt))));
            if (!gain.isZero()) state.stats().addParticles(gain);
        } else if (!gain.isZero()) {
            state.setParticles(state.particles().add(gain));
            state.stats().addParticles(gain);
        }
        state.setTimePlayed(state.timePlayed() + dt);
        state.stats().addTime(dt);
        if (state.started()) {
            state.setTimeSinceFusion(state.timeSinceFusion() + dt);
        }
    }

    /** Retient les automatismes disponibles en ce moment : une explosion peut les reverrouiller, pas les faire oublier. */
    private void noteUnlocks() {
        GameStats stats = state.stats();
        for (Automation automation : automations.values()) {
            if (!stats.wasUnlocked(automation.id()) && isAutomationAvailable(automation.id())) {
                stats.noteUnlocked(automation.id());
            }
        }
    }

    /**
     * Vrai si cet automatisme a déjà été disponible au moins une fois depuis le début du jeu,
     * même si une explosion l'a reverrouillé depuis.
     */
    public boolean wasAutomationEverAvailable(String automationId) {
        Automation automation = automation(automationId);
        if (isAutomationAvailable(automationId)) state.stats().noteUnlocked(automation.id());
        return state.stats().wasUnlocked(automation.id());
    }

    /** Ajoute un relevé aux historiques dont l'heure est venue. */
    private void recordHistory() {
        if (!state.started()) return;
        GameStats stats = state.stats();
        boolean whole = stats.history().isDue(state.timePlayed());
        boolean run = stats.runHistory().isDue(stats.runTime());
        if (!whole && !run) return;
        double[] values = measure();
        Map<String, Double> delays = new java.util.HashMap<>();
        for (Automation automation : automations.values()) {
            if (ownsAutomation(automation.id())) delays.put(automation.id(), automationInterval(automation.id()));
        }
        // La production est la seule valeur qui dépend de l'historique : chacun retient son propre sommet.
        int production = StatsHistory.Stat.PRODUCTION.ordinal();
        if (whole) {
            double[] copy = values.clone();
            copy[production] = stats.history().peak(values[production]);
            stats.history().add(new StatsHistory.Sample(state.timePlayed(), copy, delays));
        }
        if (run) {
            double[] copy = values.clone();
            copy[production] = stats.runHistory().peak(values[production]);
            stats.runHistory().add(new StatsHistory.Sample(stats.runTime(), copy, delays));
        }
    }

    /** Mesure tout ce qu'un relevé retient : une valeur par {@link StatsHistory.Stat}, dans l'ordre de l'énumération. */
    private double[] measure() {
        GameStats stats = state.stats();
        double[] values = new double[StatsHistory.Stat.values().length];
        for (StatsHistory.Stat stat : StatsHistory.Stat.values()) {
            values[stat.ordinal()] = switch (stat) {
                case PRODUCTION -> productionPerSecond().log10();
                case PARTICLES_CREATED -> power(stats.particlesCreated());
                case PARTICLES_SPENT -> power(stats.particlesSpent());
                case SPEED -> power(speed());
                case PARTICLES_PER_CREATION -> power(particlesPerCreation());
                case GENERATORS -> generatorCount();
                case SPEED_LEVELS -> totalSpeedLevels();
                case SPEED_MILESTONES -> speedMilestones();
                case PARTICLE_UPGRADES_BOUGHT -> stats.particleUpgradesBought();
                case DARK_PARTICLES -> power(darkParticlesMultiplier());
                case ATOMS_PER_FUSION -> atomsPerFusion().log10();
                case FUSION_TIME -> stats.fusions() == 0 ? Double.NaN : stats.lastFusionTime();
                case ATOMS_CREATED -> power(stats.atomsCreated());
                case ATOMS_SPENT -> power(stats.atomsSpent());
                case FUSIONS -> stats.fusions();
                case FUSION_YIELD -> fusionYield();
                case ATOM_UPGRADES_BOUGHT -> stats.atomUpgradesBought();
                case AUTOMATION_ACTIONS -> power(BigNum.of(stats.automationActions() + stats.darkAutomationActions()));
                case ELEMENTS_SHARE -> (double) discoveredElements() / PeriodicTable.ELEMENTS.size();
                case COPIES_SHARE -> (double) ownedCopies() / maxTotalCopies();
                case SYNTHESES -> stats.syntheses();
                case SYNTHESIS_COST -> isPeriodicTableComplete() ? Double.NaN : power(synthesisCost());
                case DOUBLE_DRAW -> Math.min(1, doubleDrawChance());
                case SETS -> completedSets();
                case HALF_SETS -> halfSets();
                case ELEMENT_PARTICLES -> Math.log10(elementMultiplier(ElementEffect.Stat.PARTICLES));
                case ELEMENT_SPEED -> Math.log10(elementMultiplier(ElementEffect.Stat.SPEED));
                case ELEMENT_ATOMS -> Math.log10(elementMultiplier(ElementEffect.Stat.ATOMS));
                case DARK_MATTER_SIZE -> state.darkMatterSize().log10();
                case DARK_MATTER_EARNED -> darkMatterEarned().toDouble();
                case EXPLOSIONS -> state.explosions();
                case DARK_GROWTH -> isDarkMatterUnlocked()
                        ? power(darkMatterGrowthPerSecond().subtract(BigNum.ONE)) : Double.NaN;
                case DARK_RESISTANCE -> power(darkMatterResistance());
                case HOLD_TIME -> stats.holdTime();
                case LANDMARKS -> landmarksReached();
                case ACHIEVEMENTS -> achievementCount();
                case SPACE -> isBigBangUnlocked() ? power(state.space()) : Double.NaN;
                case SPACE_USED -> isBigBangUnlocked() ? power(usedSpace()) : Double.NaN;
                case MOLECULES -> moleculesCreated();
                case MOLECULE_SPACE -> Math.log10(moleculeBoost(Molecule.Stat.SPACE));
                case MOLECULE_PARTICLES -> Math.log10(moleculeBoost(Molecule.Stat.PARTICLES));
                case MOLECULE_ATOMS -> Math.log10(moleculeBoost(Molecule.Stat.ATOMS));
                case MOLECULE_DARK -> Math.log10(moleculeBoost(Molecule.Stat.DARK_GROWTH));
                case SKY -> assembliesFormed() + bodiesFormed() + (hasGalaxy() ? 1 : 0);
            };
        }
        return values;
    }

    /** La puissance de dix d'une quantité, ou {@code NaN} tant qu'elle est nulle : la courbe ne commence qu'ensuite. */
    private static double power(BigNum value) {
        return value.sign() > 0 ? value.log10() : Double.NaN;
    }

    /** Niveaux de vitesse en tout, offerts compris : la somme sur les améliorations de vitesse du catalogue. */
    private int totalSpeedLevels() {
        int levels = 0;
        for (Upgrade upgrade : upgrades.values()) {
            if (upgrade.effect() instanceof Effect.MultiplySpeed) levels += speedLevels(upgrade.id());
        }
        return levels;
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

    /**
     * Vrai une fois {@link #UNLOCK_TOTAL_ATOMS} atomes créés depuis le début de la partie : les
     * automatismes deviennent achetables.
     */
    public boolean isAutomationUnlocked() {
        // Des automatismes conservés après une explosion restent utilisables tout de suite.
        return hasCreatedEnoughAtoms() || !state.ownedAutomations().isEmpty();
    }

    /** Vrai une fois {@link #UNLOCK_TOTAL_ATOMS} atomes créés depuis le début de la partie. */
    private boolean hasCreatedEnoughAtoms() {
        return state.totalAtoms().gte(UNLOCK_TOTAL_ATOMS);
    }

    /** Vrai une fois un élément unique obtenu : la synthèse automatique devient achetable. */
    public boolean isSynthesisAutomationUnlocked() {
        for (int number : state.elements().keySet()) {
            if (PeriodicTable.element(number).category().unique()) return true;
        }
        return false;
    }

    /**
     * Vrai si cet automatisme peut être acheté : assez d'atomes créés pour tous, et en plus un
     * élément unique pour la synthèse automatique.
     */
    public boolean isAutomationAvailable(String automationId) {
        Automation automation = automation(automationId);
        // Un automatisme possédé (offert par la matière noire, ou gardé après une explosion) est disponible d'office.
        if (state.ownsAutomation(automation.id())) return true;
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
        state.stats().addAtomsSpent(automation.cost());
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
                / bonuses().automationDivisor(target(automation)) / darkAutomationDivisor();
        if (rewarded(Challenge.Reward.FASTER_AUTOMATIONS)) interval /= REWARD_AUTOMATION_DIVISOR;
        interval /= 1 + achievementBonus(Achievement.Bonus.AUTOMATION);
        interval = Math.max(minAutomationInterval(), interval);
        // La rouille s'applique en dernier : elle ralentit même ce qui était au minimum.
        return ruled(Challenge.Rule.RUSTY_AUTOMATIONS) ? Math.max(RUSTY_MIN_INTERVAL, interval * RUSTY_FACTOR) : interval;
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
        BigNum cost = automationSpeedCost(automationId);
        state.setAtoms(state.atoms().subtract(cost).max(BigNum.ZERO));
        state.stats().addAtomsSpent(cost);
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
                state.stats().noteAutomationAction();
            }
            state.setAutomationTimer(automation.id(), waited);
        }
    }

    /** Fait faire à l'automatisme une action ; faux s'il ne peut rien faire pour l'instant. */
    private boolean act(Automation automation) {
        return switch (automation.kind()) {
            case UPGRADE -> buy(automation.upgradeId());
            case FUSION -> generatorCount() >= fusionThreshold() && fuse();
            // La réserve du joueur : la synthèse automatique n'entame pas les atomes mis de côté.
            case SYNTHESIS -> state.atoms().gte(synthesisCost().add(state.synthesisReserve())) && paySynthesis();
        };
    }

    /**
     * Fait une synthèse et dit si elle a eu lieu : une étape de synthèse ciblée ne donne aucun
     * élément, mais c'est bien une action, payée comme les autres.
     */
    private boolean paySynthesis() {
        int before = state.synthesisCount();
        synthesize();
        return state.synthesisCount() > before;
    }

    /** Atomes que la synthèse automatique laisse toujours au joueur (0 par défaut). */
    public BigNum synthesisReserve() {
        return state.synthesisReserve();
    }

    /**
     * Règle la réserve : la synthèse automatique n'agit que s'il reste au moins {@code atoms}
     * atomes après elle. Les synthèses faites à la main n'en tiennent pas compte. Une valeur
     * négative est ramenée à zéro.
     */
    public void setSynthesisReserve(BigNum atoms) {
        state.setSynthesisReserve(atoms.max(BigNum.ZERO));
    }

    /**
     * Vrai quand la réserve empêche à elle seule la synthèse automatique d'agir un jour : le prix
     * plus la réserve dépassent le plafond d'atomes, que rien ne permet d'atteindre.
     */
    public boolean isSynthesisReserveBlocking() {
        return state.synthesisReserve().sign() > 0 && !isAtomCapLifted() && !isPeriodicTableComplete()
                && synthesisCost().add(state.synthesisReserve()).gt(atomCap());
    }

    // ------------------------------------------------------------------
    // Le tableau périodique
    // ------------------------------------------------------------------

    /**
     * Masse du tableau périodique : 1 avant la première explosion, puis multipliée par
     * {@link #TABLE_WEIGHT_GROWTH} à chaque explosion qui compte, {@link #TABLE_WEIGHT_MAX_LEVEL}
     * fois au plus. Un défi se joue toujours avec un tableau de masse 1, comme une toute première partie.
     */
    public BigNum tableWeight() {
        return inChallenge() ? BigNum.ONE
                : BigNum.of(TABLE_WEIGHT_GROWTH).pow(Math.min(state.tableWeightLevel(), TABLE_WEIGHT_MAX_LEVEL));
    }

    /**
     * Nombre maximal d'atomes qu'on peut posséder en même temps, et prix maximal d'une synthèse :
     * {@link #MAX_ATOMS} fois la masse du tableau. Chaque explosion laisse donc un tableau plus
     * lourd à remplir : il faut produire plus que la fois d'avant pour exploser de nouveau.
     */
    public BigNum atomCap() {
        return Upgrade.roundUp(MAX_ATOMS.multiply(tableWeight()));
    }

    /**
     * Prix de la prochaine synthèse, en atomes : {@link #SYNTHESIS_BASE_COST}, doublé à chaque
     * synthèse déjà faite, plafonné par {@link #atomCap()}, puis réduit par les éléments qui
     * baissent ce prix.
     */
    public BigNum synthesisCost() {
        BigNum cost = SYNTHESIS_BASE_COST.multiply(BigNum.of(2).pow(state.synthesisCount())).min(atomCap());
        double divisor = bonuses().costDivisor(ElementEffect.CostTarget.SYNTHESIS) * darkSynthesisDivisor()
                * (1 + achievementBonus(Achievement.Bonus.SYNTHESIS_COST));
        return divisor == 1 ? cost : Upgrade.roundUp(cost.divide(divisor)).max(BigNum.ONE);
    }

    /**
     * Vrai une fois {@link #UNLOCK_TOTAL_ATOMS} atomes créés depuis le début de la partie : le
     * tableau périodique devient accessible, en même temps que l'automatisation. Le compte des
     * atomes créés ne redescend qu'à l'explosion : d'ici là, le tableau reste débloqué.
     */
    public boolean isPeriodicTableUnlocked() {
        return hasCreatedEnoughAtoms();
    }

    /** Vrai quand le tableau périodique est débloqué et que le joueur a de quoi payer la prochaine synthèse. */
    public boolean canSynthesize() {
        return state.started() && isPeriodicTableUnlocked() && state.atoms().gte(synthesisCost())
                && !isPeriodicTableComplete();
    }

    /**
     * Vrai quand tous les éléments sont à leur nombre maximal d'exemplaires
     * ({@link #maxCopiesOf(Element)}) : il n'y a plus rien à synthétiser.
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
        return state.elementCount(element.number()) >= maxCopiesOf(element);
    }

    /** Nombre total d'exemplaires possédés, tous éléments confondus, sans dépasser le maximum de chacun. */
    public int ownedCopies() {
        int copies = 0;
        for (Map.Entry<Integer, Integer> entry : state.elements().entrySet()) {
            copies += Math.min(entry.getValue(), maxCopiesOf(PeriodicTable.element(entry.getKey())));
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
     * <p>Avec une famille visée ({@link #setSynthesisTarget(ElementCategory)}), la synthèse est
     * ciblée : elle coûte plusieurs synthèses ({@link ElementCategory#targetTries()}). Les
     * premières sont payées comme les autres, font monter le prix comme les autres, mais ne
     * donnent rien ; la dernière donne un élément de la famille, en priorité un que le joueur
     * n'a pas encore.
     *
     * @return les éléments obtenus (un, parfois deux, davantage avec la matière noire), ou une
     *         liste vide si la synthèse est impossible (tableau verrouillé, tableau complet ou pas
     *         assez d'atomes) ou si elle n'était qu'une étape d'une synthèse ciblée
     */
    public List<Element> synthesize() {
        if (!canSynthesize()) return List.of();
        BigNum cost = synthesisCost();
        state.setAtoms(state.atoms().subtract(cost).max(BigNum.ZERO));
        state.stats().addAtomsSpent(cost);
        state.setSynthesisCount(state.synthesisCount() + 1);

        double doubleChance = doubleDrawChance();   // la chance d'avant le tirage, pas celle qu'il donnerait
        boolean guaranteed = !isSynthesisAutomationUnlocked()
                && state.synthesisCount() >= guaranteedUniqueSynthesis();
        // La garantie d'un élément unique passe avant la cible : cette synthèse-là est ordinaire.
        ElementCategory target = guaranteed ? null : synthesisTarget();
        if (target != null) {
            state.setSynthesisTries(state.synthesisTries() + 1);
            if (state.synthesisTries() < target.targetTries()) {
                state.stats().noteSynthesisTry();
                return List.of();
            }
            state.setSynthesisTries(0);
            state.stats().noteTargetedSynthesis();
            award(Achievements.TARGETED);
            if (target == ElementCategory.ACTINIDE) award(Achievements.TARGETED_ACTINIDE);
        }
        List<Element> obtained = new ArrayList<>();
        obtained.add(target != null ? drawTargeted(target) : drawElement(guaranteed));
        // Les éléments en plus de la matière noire, tant qu'il en reste à obtenir.
        for (int extra = elementsPerSynthesis() - 1; extra > 0 && !isPeriodicTableComplete(); extra--) {
            obtained.add(drawElement(false));
        }
        boolean doubleDraw = random.nextDouble() < doubleChance && !isPeriodicTableComplete();
        if (doubleDraw) obtained.add(drawElement(false));
        state.stats().noteSynthesis(obtained.size(), doubleDraw);
        if (doubleDraw) award(Achievements.DOUBLE_DRAW);
        if (obtained.size() >= 3) award(Achievements.TRIPLE_DRAW);
        return obtained;
    }

    /**
     * Famille visée par la synthèse ciblée, ou {@code null} si la synthèse tire au hasard. Une
     * famille dont tous les éléments sont à leur maximum d'exemplaires ne peut plus être visée :
     * la cible s'efface d'elle-même.
     */
    public ElementCategory synthesisTarget() {
        ElementCategory target = state.synthesisTarget();
        if (target != null && !isTargetedSynthesisUnlocked()) return null;
        if (target != null && availableElements(target).isEmpty()) {
            state.setSynthesisTarget(null);
            return null;
        }
        return target;
    }

    /**
     * Vise une famille pour les prochaines synthèses, à la main comme automatiques, ou revient
     * au hasard avec {@code null}. Les synthèses déjà payées pour une cible restent acquises si
     * la cible change : elles comptent pour la suivante.
     *
     * @return {@code true} si la cible a été retenue ; faux si la synthèse ciblée n'est pas
     *         débloquée ({@link #isTargetedSynthesisUnlocked()}), si le tableau périodique est
     *         verrouillé ou si la famille n'a plus rien à donner
     */
    public boolean setSynthesisTarget(ElementCategory category) {
        if (category != null && (!isTargetedSynthesisUnlocked() || !isPeriodicTableUnlocked()
                || availableElements(category).isEmpty())) return false;
        state.setSynthesisTarget(category);
        return true;
    }

    /** Vrai une fois la synthèse ciblée achetée avec de la matière noire : d'ici là, la synthèse tire toujours au hasard. */
    public boolean isTargetedSynthesisUnlocked() {
        return hasDark(DarkEffect.TargetedSynthesis.class);
    }

    /** Synthèses déjà payées pour la synthèse ciblée en cours. */
    public int synthesisTries() {
        return state.synthesisTries();
    }

    /**
     * Synthèses qu'il reste à payer, la prochaine comprise, avant que la synthèse ciblée donne
     * son élément ; 0 sans cible. Les essais déjà payés au-delà de ce que demande la cible ne
     * sont pas perdus : la prochaine synthèse aboutit.
     */
    public int synthesisTriesLeft() {
        ElementCategory target = synthesisTarget();
        return target == null ? 0 : Math.max(1, target.targetTries() - state.synthesisTries());
    }

    /**
     * Tire un élément de la famille visée et l'ajoute à la collection : un élément que le joueur
     * ne possède pas encore s'il en reste, sinon un de ceux qui peuvent encore gagner un exemplaire.
     */
    private Element drawTargeted(ElementCategory target) {
        List<Element> pool = availableElements(target);
        List<Element> unknown = pool.stream().filter(element -> state.elementCount(element.number()) == 0).toList();
        List<Element> from = unknown.isEmpty() ? pool : unknown;
        Element element = from.get(random.nextInt(from.size()));
        state.setElementCount(element.number(), state.elementCount(element.number()) + 1);
        return element;
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

    // ------------------------------------------------------------------
    // Les ensembles
    // ------------------------------------------------------------------

    /** Tous les ensembles du tableau périodique : les familles, puis les périodes. */
    public List<ElementSet> elementSets() {
        return ElementSets.DEFAULT;
    }

    /** Nombre d'éléments de cet ensemble que le joueur possède. */
    public int setProgress(ElementSet set) {
        return set.progress(state.elements());
    }

    /** Vrai si le joueur possède tous les éléments de cet ensemble : son bonus est actif. */
    public boolean isSetComplete(ElementSet set) {
        return set.isComplete(state.elements());
    }

    /**
     * Force du bonus de cet ensemble en ce moment : 0, {@link ElementSet#HALF_STRENGTH} à partir
     * de la moitié réunie, 1 quand il est complet.
     */
    public double setStrength(ElementSet set) {
        return set.strength(state.elements());
    }

    /** Nombre d'ensembles complets. */
    public int completedSets() {
        return bonuses().completedSets();
    }

    /** Nombre d'ensembles à moitié réunis, sans être complets. */
    public int halfSets() {
        return bonuses().halfSets();
    }

    /** Les éléments d'une famille qui peuvent encore sortir : ceux qui n'ont pas atteint leur maximum d'exemplaires. */
    private List<Element> availableElements(ElementCategory category) {
        return PeriodicTable.elements(category).stream()
                .filter(element -> state.elementCount(element.number()) < maxCopiesOf(element))
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
        if (ruled(Challenge.Rule.INERT_ELEMENTS)) return NO_BONUSES;
        double strength = rewarded(Challenge.Reward.STRONGER_ELEMENTS) ? REWARD_ELEMENT_STRENGTH : 1;
        if (elementBonusesVersion != state.elementsVersion() || elementBonusesStrength != strength) {
            elementBonuses = ElementBonuses.of(state.elements(), strength);
            elementBonusesVersion = state.elementsVersion();
            elementBonusesStrength = strength;
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

    /**
     * Vrai quand le tableau périodique peut exploser : les 118 éléments sont découverts. Un
     * exemplaire de chacun suffit ; les suivants renforcent les éléments, ils ne conditionnent rien.
     */
    public boolean canExplode() {
        return state.started() && state.elements().size() == PeriodicTable.ELEMENTS.size();
    }

    /**
     * Fait exploser le tableau périodique, une fois tous ses éléments découverts.
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
        BigNum reward = nextExplosionDarkMatter();
        boolean replay = isChallengeReplay();
        // Un défi en cours est réussi : il laisse sa récompense, et son meilleur temps est noté.
        Challenge challenge = activeChallenge();
        if (challenge != null) {
            double time = state.stats().runTime();
            Double best = state.challengeTimes().get(challenge.id());
            if (best == null || time < best) state.setChallengeTime(challenge.id(), time);
            state.addCompletedChallenge(challenge.id());
            state.setActiveChallenge(null);
        }
        state.stats().noteProduction(productionAtFusion());
        // Une explosion rapide ne compte qu'à partir de la deuxième : la première n'a pas de précédente.
        if (state.explosions() >= 1 && state.stats().runTime() < Achievements.FAST_EXPLOSION_SECONDS) {
            award(Achievements.FAST_EXPLOSION);
        }
        state.setDarkMatter(state.darkMatter().add(reward));
        state.setExplosions(state.explosions() + 1);
        // Un défi rejoué ne compte que pour son temps : ni matière noire, ni tableau plus lourd.
        if (!replay) state.setTableWeightLevel(state.tableWeightLevel() + 1);
        // L'appui verrouillé ne survit pas à l'explosion : il faut le remettre à chaque partie.
        state.setHoldLocked(false);
        clearRun();
        state.stats().endRun();
        return true;
    }

    /**
     * Efface la partie en cours comme le fait une explosion : toute la matière disparaît, sauf ce
     * que l'arbre de matière noire permet de garder, et le départ lancé est appliqué.
     */
    private void clearRun() {
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
        applyHeadStart();
    }

    // ------------------------------------------------------------------
    // Les défis d'explosion
    // ------------------------------------------------------------------

    /** Tous les défis, dans l'ordre du catalogue. */
    public List<Challenge> challenges() {
        return Challenges.DEFAULT;
    }

    private Challenge challenge(String challengeId) {
        for (Challenge challenge : Challenges.DEFAULT) {
            if (challenge.id().equals(challengeId)) return challenge;
        }
        throw new IllegalArgumentException("Défi inconnu : " + challengeId);
    }

    /** Le défi en cours, ou {@code null} en partie ordinaire. */
    public Challenge activeChallenge() {
        return state.activeChallenge() == null ? null : challenge(state.activeChallenge());
    }

    /** Vrai quand le joueur a gagné assez de matière noire pour tenter ce défi. Rien n'est dépensé. */
    public boolean isChallengeUnlocked(String challengeId) {
        Challenge challenge = challenge(challengeId);
        return isDarkMatterUnlocked() && darkMatterEarned().gte(BigNum.of(challenge.darkMatter()));
    }

    /** Vrai si ce défi a été réussi au moins une fois : sa récompense est acquise. */
    public boolean isChallengeCompleted(String challengeId) {
        return state.completedChallenges().contains(challenge(challengeId).id());
    }

    /** Nombre de défis réussis. */
    public int completedChallenges() {
        return state.completedChallenges().size();
    }

    /** Meilleur temps de ce défi, en secondes de jeu, ou −1 s'il n'a jamais été réussi. */
    public double challengeBestTime(String challengeId) {
        Double best = state.challengeTimes().get(challenge(challengeId).id());
        return best == null ? -1 : best;
    }

    /**
     * Commence un défi : la partie en cours est effacée, sans matière noire en échange, et la
     * contrainte du défi s'applique jusqu'à la prochaine explosion ou jusqu'à l'abandon. Un défi
     * déjà réussi peut être rejoué, pour le temps.
     *
     * <p>Un défi se joue sans les mémoires de la matière noire : rien n'est gardé de la partie
     * précédente, et tant qu'il dure il n'y a ni générateurs ni niveaux de vitesse offerts, ni
     * départ lancé, ni automatismes offerts. Tout ce que l'arbre multiplie reste acquis. C'est ce
     * qui fait d'un défi une vraie partie, à rejouer depuis le premier générateur.
     *
     * @return {@code true} si le défi a commencé ; faux s'il est verrouillé ou si un défi est déjà en cours
     */
    public boolean startChallenge(String challengeId) {
        Challenge challenge = challenge(challengeId);
        if (!state.started() || !isChallengeUnlocked(challengeId) || state.activeChallenge() != null) return false;
        state.setActiveChallenge(challenge.id());
        state.clearMatter();   // rien n'est gardé : ni automatismes, ni améliorations, ni éléments
        state.stats().restartRun();
        return true;
    }

    /**
     * Abandonne le défi en cours : la partie est effacée de nouveau et repart sans contrainte.
     *
     * @return {@code true} s'il y avait un défi à abandonner
     */
    public boolean abandonChallenge() {
        if (state.activeChallenge() == null) return false;
        state.setActiveChallenge(null);
        clearRun();
        state.stats().restartRun();
        award(Achievements.ABANDON);
        return true;
    }

    /** Vrai pendant un défi, quel qu'il soit. */
    public boolean inChallenge() {
        return state.activeChallenge() != null;
    }

    /** Vrai si le défi en cours impose cette contrainte. */
    private boolean ruled(Challenge.Rule rule) {
        String active = state.activeChallenge();
        return active != null && challenge(active).rule() == rule;
    }

    /** Vrai si le joueur a réussi un défi qui donne cette récompense. */
    private boolean rewarded(Challenge.Reward reward) {
        if (state.completedChallenges().isEmpty()) return false;
        for (Challenge challenge : Challenges.DEFAULT) {
            if (challenge.reward() == reward && state.completedChallenges().contains(challenge.id())) return true;
        }
        return false;
    }

    /**
     * La désintégration du défi du même nom : chaque exemplaire possédé a {@link #DECAY_RATE}
     * chance de disparaître par seconde. Les fractions s'accumulent : avec 50 exemplaires, il en
     * disparaît un par seconde.
     */
    private void decay(double dt) {
        if (!ruled(Challenge.Rule.DECAY) || dt <= 0) return;
        int copies = 0;
        for (int count : state.elements().values()) copies += count;
        if (copies == 0) return;
        double debt = state.decayDebt() + copies * DECAY_RATE * dt;
        while (debt >= 1 && copies > 0) {
            // Un exemplaire au hasard : un élément possédé en neuf exemplaires a neuf fois plus de chances d'en perdre un.
            int roll = random.nextInt(copies);
            for (Map.Entry<Integer, Integer> entry : state.elements().entrySet()) {
                roll -= entry.getValue();
                if (roll < 0) {
                    state.setElementCount(entry.getKey(), entry.getValue() - 1);
                    break;
                }
            }
            copies--;
            debt -= 1;
        }
        state.setDecayDebt(copies == 0 ? 0 : debt);
    }

    // ------------------------------------------------------------------
    // Les succès
    // ------------------------------------------------------------------

    /** Tous les succès du catalogue, dans son ordre. Vide pour une partie créée sans catalogue de succès. */
    public Collection<Achievement> achievements() {
        return achievements.values();
    }

    /** Vrai si ce succès est obtenu. */
    public boolean hasAchievement(String achievementId) {
        return state.achievements().contains(achievementId);
    }

    /** Nombre de succès obtenus parmi ceux du catalogue. */
    public int achievementCount() {
        int count = 0;
        for (String id : state.achievements()) {
            if (achievements.containsKey(id)) count++;
        }
        return count;
    }

    /**
     * Les succès obtenus depuis le dernier appel, dans l'ordre : l'interface s'en sert pour les
     * annoncer. La liste est vidée à chaque lecture.
     */
    public List<Achievement> takeNewAchievements() {
        List<Achievement> taken = List.copyOf(newAchievements);
        newAchievements.clear();
        return taken;
    }

    /** Ce que tous les succès obtenus multiplient dans les particules : le bonus commun, puis les bonus propres. */
    public BigNum achievementParticlesMultiplier() {
        int count = achievementCount();
        if (count == 0) return BigNum.ONE;
        return BigNum.of((1 + ACHIEVEMENT_PARTICLES * count) * (1 + achievementBonus(Achievement.Bonus.PARTICLES)));
    }

    /** Somme des bonus propres d'une sorte, pour les succès obtenus (0.02 = 2 %). */
    public double achievementBonus(Achievement.Bonus bonus) {
        if (achievements.isEmpty()) return 0;
        int count = state.achievements().size();
        if (achievementBonusesFor != count) {
            achievementBonuses.clear();
            for (String id : state.achievements()) {
                Achievement achievement = achievements.get(id);
                if (achievement != null && achievement.hasBonus()) {
                    achievementBonuses.merge(achievement.bonus(), achievement.amount(), Double::sum);
                }
            }
            achievementBonusesFor = count;
        }
        return achievementBonuses.getOrDefault(bonus, 0.0);
    }

    /** Accorde un succès du catalogue, s'il existe et n'est pas déjà obtenu. */
    private void award(String achievementId) {
        Achievement achievement = achievements.get(achievementId);
        if (achievement != null && state.addAchievement(achievementId)) newAchievements.add(achievement);
    }

    /** Accorde les succès dont la condition est remplie. */
    private void checkAchievements() {
        if (achievements.isEmpty() || !state.started()) return;
        for (Achievement achievement : achievements.values()) {
            if (achievement.condition() != null && !state.achievements().contains(achievement.id())
                    && achievement.condition().test(this)) {
                award(achievement.id());
            }
        }
    }

    /** Les succès qui se jouent au moment d'une fusion, avant qu'elle ne remette la partie à zéro. */
    private void noteFusionFeats() {
        if (achievements.isEmpty()) return;
        boolean bare = startingSpeedLevels() == 0;
        for (Upgrade upgrade : upgrades.values()) {
            boolean generator = upgrade.effect() instanceof Effect.AddGenerator;
            if (upgrade.resource() == Resource.PARTICLES && !generator && state.levelOf(upgrade.id()) > 0) bare = false;
        }
        if (bare) award(Achievements.BARE_FUSION);
        if (state.timeSinceFusion() >= Achievements.PATIENT_SECONDS) award(Achievements.PATIENT_FUSION);
        // Un sprint se mesure d'une fusion à l'autre : la première de la partie n'en est pas un.
        if (state.stats().runFusions() >= 1 && state.timeSinceFusion() < Achievements.FAST_FUSION_SECONDS) {
            award(Achievements.FAST_FUSION);
        }
        if (fusionGroups() >= Achievements.MASS_FUSION_GROUPS) award(Achievements.MASS_FUSION);
    }

    // ------------------------------------------------------------------
    // Les paliers de taille
    // ------------------------------------------------------------------

    /**
     * Nombre de paliers de taille atteints : les repères de l'échelle des grandeurs que la
     * matière noire a dépassés ({@link SizeScale#MILESTONES}). Aucun avant la première explosion.
     */
    public int landmarksReached() {
        return isDarkMatterUnlocked() ? SizeScale.milestonesReached(state.darkMatterSize()) : 0;
    }

    /** Nombre de paliers atteints qui donnent ce bonus. */
    public int landmarksReached(SizeScale.Bonus bonus) {
        int reached = landmarksReached();
        int count = 0;
        for (int index = 0; index < reached; index++) {
            if (SizeScale.bonusOf(index) == bonus) count++;
        }
        return count;
    }

    /** Ce que les paliers de taille multiplient dans les particules de chaque création. */
    public BigNum landmarkParticlesMultiplier() {
        return BigNum.of(LANDMARK_PARTICLES).pow(landmarksReached(SizeScale.Bonus.PARTICLES));
    }

    /** Ce que les paliers de taille multiplient dans les atomes de chaque fusion. */
    public double landmarkAtomsMultiplier() {
        return 1 + LANDMARK_ATOMS * landmarksReached(SizeScale.Bonus.ATOMS);
    }

    /** Ce que les paliers de taille multiplient dans la croissance de la matière noire. */
    public double landmarkExpansionMultiplier() {
        return Math.pow(LANDMARK_EXPANSION, landmarksReached(SizeScale.Bonus.EXPANSION));
    }

    /**
     * Vrai si l'appui sur la matière noire est verrouillé : elle grossit toute seule à chaque
     * {@link #tick(double)}, quel que soit l'onglet que regarde le joueur. Il faut la case
     * « Verrou » de l'arbre.
     */
    public boolean isHoldLocked() {
        return state.holdLocked() && isHoldLockUnlocked();
    }

    /**
     * Verrouille ou libère l'appui. Le verrou tient jusqu'à la prochaine explosion.
     *
     * @return {@code true} si le verrou a pris l'état demandé ; faux s'il n'est pas débloqué
     */
    public boolean setHoldLocked(boolean locked) {
        if (locked && !isHoldLockUnlocked()) return false;
        state.setHoldLocked(locked);
        if (locked) award(Achievements.HOLD_LOCK);
        return true;
    }

    /**
     * Vrai une fois acquise l'amélioration d'espace « Appui automatique » ({@link SpaceUpgrade.AutoHold}).
     * Comme toute amélioration d'espace, ni l'explosion ni le Big Bang ne la reprennent.
     */
    public boolean isAutoHoldUnlocked() {
        if (!isBigBangUnlocked()) return false;
        for (SpaceUpgrade upgrade : spaceUpgrades.values()) {
            if (upgrade.effect() instanceof SpaceUpgrade.AutoHold && state.ownsSpaceUpgrade(upgrade.id())) return true;
        }
        return false;
    }

    /** Vrai si l'appui automatique est en marche (il ne sert qu'une fois acquis). */
    public boolean isAutoHoldEnabled() {
        return state.autoHold();
    }

    /**
     * Met en marche ou coupe l'appui automatique.
     *
     * @return {@code true} s'il a pris l'état demandé ; faux s'il n'est pas acquis
     */
    public boolean setAutoHoldEnabled(boolean enabled) {
        if (!isAutoHoldUnlocked()) return false;
        state.setAutoHold(enabled);
        return true;
    }

    /**
     * Vrai si la matière noire grossit toute seule en ce moment, à pleine vitesse d'appui : l'appui
     * automatique est acquis, en marche, et la matière noire existe (il faut une première explosion
     * depuis le dernier Big Bang). Tenir le clic n'ajoute alors rien : c'est déjà fait.
     */
    public boolean isAutoHolding() {
        return isAutoHoldUnlocked() && state.autoHold() && isDarkMatterUnlocked();
    }

    /** Vrai si le défi en cours a déjà été réussi : on le rejoue pour son temps, sans rien y gagner d'autre. */
    public boolean isChallengeReplay() {
        Challenge challenge = activeChallenge();
        return challenge != null && state.completedChallenges().contains(challenge.id());
    }

    /**
     * Matière noire que laissera vraiment la prochaine explosion : {@link #darkMatterPerExplosion()},
     * ou rien si elle termine un défi déjà réussi.
     */
    public BigNum nextExplosionDarkMatter() {
        return isChallengeReplay() ? BigNum.ZERO : darkMatterPerExplosion();
    }

    /**
     * Matière noire que laisse une explosion : {@link #DARK_MATTER_PER_EXPLOSION}, plus ce qu'ajoute
     * l'arbre, le tout multiplié par les paliers de Big Bang ({@link #bigBangDarkMatterFactor()}).
     */
    public BigNum darkMatterPerExplosion() {
        BigNum amount = DARK_MATTER_PER_EXPLOSION;
        for (DarkUpgrade dark : darkUpgrades.values()) {
            if (dark.effect() instanceof DarkEffect.AddDarkMatterPerExplosion add) {
                amount = amount.add(BigNum.of(add.perLevel() * state.darkLevelOf(dark.id())));
            }
        }
        double factor = bigBangDarkMatterFactor();
        return factor == 1 ? amount : amount.multiply(factor);
    }

    /**
     * Vrai une fois la première explosion déclenchée : la matière noire existe. Vrai aussi dès le
     * début d'une partie quand l'arbre a traversé le Big Bang ({@link #bigBangKeepsDarkTree()}).
     */
    public boolean isDarkMatterUnlocked() {
        return state.explosions() > 0 || state.darkMatterSpent().sign() > 0;
    }

    /**
     * Matière noire gagnée depuis le début du jeu : celle qui reste, plus celle dépensée en
     * améliorations. C'est elle, et non ce qui reste, qui ouvre les cases de l'arbre et les
     * automatismes, qui donne son élan à la croissance et dont dépendent les effets « par matière
     * noire » : dépenser de la matière noire ne fait donc rien perdre.
     */
    public BigNum darkMatterEarned() {
        return state.darkMatter().add(state.darkMatterSpent());
    }

    /** La matière noire gagnée, bornée pour rester calculable, bien au-delà de ce qu'un joueur peut avoir. */
    private double darkMatterUnits() {
        return darkMatterEarned().min(BigNum.of(1, 15)).toDouble();
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
     * {@code DARK_MATTER_GROWTH_PER_UNIT × matière noire gagnée × facteur d'avancement ×
     * multiplicateur de l'arbre}. C'est son taux de croissance par seconde d'appui quand elle a
     * la taille d'un proton ; ensuite sa résistance le divise.
     */
    public double darkMatterMomentum() {
        return DARK_MATTER_GROWTH_PER_UNIT * darkMatterUnits() * darkMatterProgressFactor()
                * darkExpansionMultiplier().min(BigNum.of(1, 100)).toDouble() * landmarkExpansionMultiplier()
                * (1 + achievementBonus(Achievement.Bonus.EXPANSION))
                * moleculeBoost(Molecule.Stat.DARK_GROWTH);
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
        if (darkMatterEarned().isZero()) return BigNum.ONE;
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
        boolean grown = expandDarkMatter(seconds);
        if (grown) state.stats().addHoldTime(seconds);
        return grown;
    }

    /**
     * La croissance elle-même, qu'elle vienne d'un appui du joueur ({@link #growDarkMatter(double)}),
     * de l'expansion spontanée ou de l'onde de fusion : seul l'appui compte dans les statistiques.
     */
    private boolean expandDarkMatter(double seconds) {
        if (seconds < 0 || Double.isNaN(seconds) || Double.isInfinite(seconds)) {
            throw new IllegalArgumentException("Durée invalide : " + seconds);
        }
        if (!isDarkMatterUnlocked() || darkMatterEarned().isZero() || seconds == 0) return false;
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
    // Le Big Bang
    // ------------------------------------------------------------------

    /** Vrai si cette condition du Big Bang est remplie en ce moment. */
    public boolean isBigBangConditionMet(BigBangCondition condition) {
        return switch (condition) {
            case PARTICLES -> darkBalance(DarkUpgrade.Branch.PARTICLES).gte(BIG_BANG_PARTICLES);
            case ATOMS -> state.atoms().gte(BIG_BANG_ATOMS);
            case DARK_MATTER -> state.darkMatter().gte(BIG_BANG_DARK_MATTER);
            case ACHIEVEMENTS -> achievementCount() >= BIG_BANG_ACHIEVEMENTS;
            case CHALLENGES -> !challenges().isEmpty() && completedChallenges() >= challenges().size();
        };
    }

    /** Nombre de conditions du Big Bang remplies en ce moment, sur les cinq. */
    public int bigBangConditionsMet() {
        int met = 0;
        for (BigBangCondition condition : BigBangCondition.values()) {
            if (isBigBangConditionMet(condition)) met++;
        }
        return met;
    }

    /**
     * Vrai si le Big Bang peut être déclenché : les cinq conditions ({@link BigBangCondition}) sont
     * remplies au même moment, hors de tout défi. Les particules et les atomes se comptent dans la
     * partie en cours : une explosion les reprend, et il faut les réunir de nouveau.
     */
    public boolean canBigBang() {
        return state.started() && !inChallenge()
                && bigBangConditionsMet() == BigBangCondition.values().length;
    }

    /**
     * Déclenche le Big Bang : le passage à l'acte suivant.
     *
     * <p>Tout ce que les trois premiers actes ont construit disparaît : particules, atomes,
     * améliorations, automatismes, éléments, puis la matière noire, sa taille, son arbre, ses
     * automatismes, la masse du tableau et les défis réussis. La partie repart d'un seul
     * générateur, comme au premier jour. Restent les succès et leurs bonus, les records des
     * défis, le temps de jeu, les statistiques, et le nombre de Big Bangs ({@link #bigBangs()}).
     *
     * <p>Reste aussi tout ce qui appartient à l'acte que le Big Bang ouvre : l'espace et ses
     * améliorations, les molécules, leurs rassemblements, les assemblages, les astres et la galaxie.
     * Chaque Big Bang accélère l'expansion ({@link #spacePerSecond()}) ; en échange le tableau
     * périodique est vide, et il faut le regarnir avant de créer de nouvelles molécules.
     *
     * <p>À partir du Big Bang qui atteint le palier voulu ({@link #bigBangKeepsDarkTree()}), l'arbre
     * de matière noire reste aussi, avec ses automatismes : seuls partent la réserve, la taille, la
     * masse du tableau et les défis réussis.
     *
     * @return {@code true} si le Big Bang a eu lieu
     */
    public boolean bigBang() {
        if (!canBigBang()) return false;
        state.stats().noteProduction(productionAtFusion());
        boolean keepsTree = bigBangKeepsDarkTree();
        state.clearMatter();
        if (keepsTree) state.clearDarkMatterKeepingTree();
        else state.clearDarkMatter();
        state.setBigBangs(state.bigBangs() + 1);
        state.stats().restartRun();
        return true;
    }

    // ------------------------------------------------------------------
    // Les paliers de Big Bang
    // ------------------------------------------------------------------

    /** Les paliers de Big Bang, du premier au cinquième ({@link BigBangMilestones}). */
    public List<BigBangMilestone> bigBangMilestones() {
        return BigBangMilestones.DEFAULT;
    }

    /** Vrai si ce palier est atteint : assez de Big Bangs ont été déclenchés. Il agit dès cet instant, sans rien acheter. */
    public boolean isBigBangMilestoneReached(BigBangMilestone milestone) {
        return state.bigBangs() >= milestone.bigBangs();
    }

    /** Nombre de paliers de Big Bang atteints. */
    public int bigBangMilestonesReached() {
        int reached = 0;
        for (BigBangMilestone milestone : BigBangMilestones.DEFAULT) {
            if (isBigBangMilestoneReached(milestone)) reached++;
        }
        return reached;
    }

    /** Le prochain palier de Big Bang, ou {@code null} quand ils sont tous atteints. */
    public BigBangMilestone nextBigBangMilestone() {
        for (BigBangMilestone milestone : BigBangMilestones.DEFAULT) {
            if (!isBigBangMilestoneReached(milestone)) return milestone;
        }
        return null;
    }

    /** Ce par quoi les paliers de Big Bang atteints multiplient une grandeur ({@link BigBangMilestone.Boost}). Vaut 1 sans aucun. */
    public double bigBangMilestoneBoost(Molecule.Stat stat) {
        double factor = 1;
        for (BigBangMilestone milestone : BigBangMilestones.DEFAULT) {
            if (!isBigBangMilestoneReached(milestone)) continue;
            for (BigBangMilestone.Effect effect : milestone.effects()) {
                if (effect instanceof BigBangMilestone.Boost boost && boost.stat() == stat) factor *= boost.factor();
            }
        }
        return factor;
    }

    /**
     * Ce par quoi la matière noire en réserve multiplie l'expansion, une fois atteint le palier qui
     * le permet ({@link BigBangMilestone.DarkMatterSpace}) : {@code 1 + part × √(matière noire)}.
     * Avec 100 de matière noire et une part de 0,1 : le double ; avec 2 500 : six fois. Vaut 1 avant
     * le palier, et après un Big Bang tant que la matière noire n'est pas revenue.
     */
    public double darkMatterSpaceBoost() {
        double perRoot = 0;
        for (BigBangMilestone milestone : BigBangMilestones.DEFAULT) {
            if (!isBigBangMilestoneReached(milestone)) continue;
            for (BigBangMilestone.Effect effect : milestone.effects()) {
                if (effect instanceof BigBangMilestone.DarkMatterSpace dark) perRoot += dark.perRoot();
            }
        }
        if (perRoot == 0) return 1;
        return 1 + perRoot * Math.sqrt(Math.max(0, state.darkMatter().toDouble()));
    }

    // ------------------------------------------------------------------
    // La création automatique des molécules
    // ------------------------------------------------------------------

    /** Secondes entre deux passages de la création automatique : à chaque passage, une création par amas choisi. */
    public static final double MOLECULE_AUTOMATION_SECONDS = 5;

    /** Nombre de passages qu'un seul appel à {@link #tick(double)} rattrape au plus : un long temps hors-ligne ne bloque pas le jeu. */
    private static final int MOLECULE_AUTOMATION_CATCH_UP = 20;

    private double moleculeAutomationTimer = 0;
    private int moleculeAutomationTurn = 0;

    /**
     * Vrai si le prochain Big Bang laissera l'arbre de matière noire en place
     * ({@link BigBangMilestone.KeepDarkTree}) : le palier qui le donne est atteint, ou ce Big Bang
     * l'atteindra. Ce Big Bang-là en profite donc déjà.
     */
    public boolean bigBangKeepsDarkTree() {
        for (BigBangMilestone milestone : BigBangMilestones.DEFAULT) {
            if (milestone.bigBangs() > state.bigBangs() + 1) continue;
            for (BigBangMilestone.Effect effect : milestone.effects()) {
                if (effect instanceof BigBangMilestone.KeepDarkTree) return true;
            }
        }
        return false;
    }

    /** Vrai une fois atteint le palier de Big Bang qui donne la création automatique ({@link BigBangMilestone.AutoMolecules}). */
    public boolean isMoleculeAutomationUnlocked() {
        for (BigBangMilestone milestone : BigBangMilestones.DEFAULT) {
            if (!isBigBangMilestoneReached(milestone)) continue;
            for (BigBangMilestone.Effect effect : milestone.effects()) {
                if (effect instanceof BigBangMilestone.AutoMolecules) return true;
            }
        }
        return false;
    }

    /** Vrai si la création automatique est réglée sur « en marche » ; elle l'est par défaut dès qu'elle est acquise. */
    public boolean isMoleculeAutomationEnabled() {
        return state.autoMolecules();
    }

    /**
     * Met en marche ou coupe la création automatique.
     *
     * @return {@code true} si le réglage a été pris en compte (il faut l'avoir acquise)
     */
    public boolean setMoleculeAutomationEnabled(boolean enabled) {
        if (!isMoleculeAutomationUnlocked()) return false;
        state.setAutoMolecules(enabled);
        return true;
    }

    /** Vrai si la création automatique entretient cette sorte. */
    public boolean isMoleculeAutomated(String moleculeId) {
        return state.isMoleculeAutomated(molecule(moleculeId).id());
    }

    /**
     * Confie un amas à la création automatique, ou le lui retire. Seule une sorte rassemblée peut
     * lui être confiée : c'est là qu'une création ajoute plusieurs molécules.
     *
     * @return {@code true} si le choix a été pris en compte
     */
    public boolean setMoleculeAutomated(String moleculeId, boolean automated) {
        Molecule molecule = molecule(moleculeId);
        if (!isMoleculeAutomationUnlocked()) return false;
        if (automated && !state.hasSubstance(molecule.id())) return false;
        state.setMoleculeAutomated(molecule.id(), automated);
        return true;
    }

    /** Nombre d'amas confiés à la création automatique. */
    public int automatedMolecules() {
        return state.automatedMolecules().size();
    }

    /** Vrai si la création automatique agit en ce moment : acquise, en marche, et au moins un amas lui est confié. */
    public boolean isAutoCreatingMolecules() {
        return isMoleculeAutomationUnlocked() && state.autoMolecules() && !state.automatedMolecules().isEmpty();
    }

    /**
     * Fait passer le temps de la création automatique : toutes les {@link #MOLECULE_AUTOMATION_SECONDS}
     * secondes, une création dans chaque amas choisi qui peut en recevoir une, au même prix qu'à la
     * main ({@link #createMolecule(String)}). Quand l'espace manque pour tous, le premier servi
     * change à chaque passage : aucun amas n'est oublié.
     */
    private void runMoleculeAutomation(double dt) {
        if (dt <= 0 || !state.started() || !isAutoCreatingMolecules()) {
            moleculeAutomationTimer = 0;
            return;
        }
        moleculeAutomationTimer += dt;
        int rounds = (int) Math.min(MOLECULE_AUTOMATION_CATCH_UP, Math.floor(moleculeAutomationTimer / MOLECULE_AUTOMATION_SECONDS + 1e-9));
        if (rounds <= 0) return;
        moleculeAutomationTimer = Math.max(0, Math.min(MOLECULE_AUTOMATION_SECONDS,
                moleculeAutomationTimer - rounds * MOLECULE_AUTOMATION_SECONDS));
        List<String> chosen = state.automatedMolecules();
        for (int round = 0; round < rounds; round++) {
            boolean created = false;
            int first = Math.floorMod(moleculeAutomationTurn++, chosen.size());
            for (int offset = 0; offset < chosen.size(); offset++) {
                String id = chosen.get((first + offset) % chosen.size());
                if (molecules.containsKey(id)) created |= createMolecule(id);
            }
            if (!created) break;
        }
    }

    /** Ce par quoi les paliers atteints multiplient la matière noire de chaque explosion ({@link BigBangMilestone.DarkMatter}). */
    public double bigBangDarkMatterFactor() {
        double factor = 1;
        for (BigBangMilestone milestone : BigBangMilestones.DEFAULT) {
            if (!isBigBangMilestoneReached(milestone)) continue;
            for (BigBangMilestone.Effect effect : milestone.effects()) {
                if (effect instanceof BigBangMilestone.DarkMatter dark) factor *= dark.factor();
            }
        }
        return factor;
    }

    /** Ce par quoi les paliers atteints multiplient ce qu'un amas attire à chaque création ({@link BigBangMilestone.Accretion}). */
    public double accretionFactor() {
        double factor = 1;
        for (BigBangMilestone milestone : BigBangMilestones.DEFAULT) {
            if (!isBigBangMilestoneReached(milestone)) continue;
            for (BigBangMilestone.Effect effect : milestone.effects()) {
                if (effect instanceof BigBangMilestone.Accretion accretion) factor *= accretion.factor();
            }
        }
        return factor;
    }

    /** Vrai une fois le premier Big Bang déclenché : l'acte suivant existe. */
    public boolean isBigBangUnlocked() {
        return state.bigBangs() > 0;
    }

    /** Nombre de Big Bangs déclenchés depuis le début du jeu. */
    public int bigBangs() {
        return state.bigBangs();
    }

    // ------------------------------------------------------------------
    // Les molécules
    // ------------------------------------------------------------------

    /** Toutes les molécules, dans l'ordre du catalogue. */
    public List<Molecule> molecules() {
        return moleculeList;
    }

    /** La molécule qui porte cet identifiant. */
    public Molecule molecule(String moleculeId) {
        Molecule molecule = molecules.get(moleculeId);
        if (molecule == null) throw new IllegalArgumentException("Molécule inconnue : " + moleculeId);
        return molecule;
    }

    /** Nombre de molécules de cette sorte créées. */
    public int moleculeCount(String moleculeId) {
        return state.moleculeCount(molecule(moleculeId).id());
    }

    /** Nombre de molécules créées, toutes sortes confondues. */
    public int moleculesCreated() {
        return state.moleculeLog().size();
    }

    /** Nombre de sortes de molécules dont le joueur a créé au moins une. */
    public int moleculeKindsCreated() {
        return state.moleculeKinds();
    }

    /**
     * Ce que demande une création dans cette sorte : numéro atomique → exemplaires à y mettre, dans
     * l'ordre de la formule. C'est la formule, une fois, quel que soit le nombre de molécules déjà
     * créées : ce qui limite une sorte, c'est l'espace qu'elle occupe ({@link #moleculeVolume(String)}),
     * et le tableau périodique qu'il faut regarnir entre deux créations.
     */
    public Map<Integer, Integer> nextMoleculeCost(String moleculeId) {
        return molecule(moleculeId).recipe();
    }

    /**
     * Espace qu'occupe une molécule de cette sorte : {@link #SPACE_PER_PROTON} par proton, multiplié
     * par ce que demande son état ({@link Molecule.State#spaceFactor()}). Plus elle a d'atomes, et
     * plus ils sont lourds, plus elle prend de place ; et un gaz en prend vingt fois plus qu'un
     * cristal du même poids.
     */
    public BigNum moleculeVolume(String moleculeId) {
        return BigNum.of(volumeOf(molecule(moleculeId)));
    }

    private static double volumeOf(Molecule molecule) {
        return SPACE_PER_PROTON * molecule.protons() * (molecule.hasState() ? molecule.state().spaceFactor() : 1);
    }

    /** Espace qu'occupent toutes les molécules créées. */
    public BigNum occupiedSpace() {
        if (occupiedSpaceVersion != state.moleculesVersion()) {
            double volume = 0;
            for (Molecule molecule : moleculeList) {
                int count = state.moleculeCount(molecule.id());
                if (count > 0) volume += count * volumeOf(molecule);
            }
            occupiedSpace = BigNum.of(volume);
            occupiedSpaceVersion = state.moleculesVersion();
        }
        return occupiedSpace;
    }

    /**
     * Espace que réservent les lieux de rassemblement ouverts : la somme de ce qu'a demandé chaque
     * gaz, liquide ou solide formé ({@link #substanceSpace(String)}).
     */
    public BigNum reservedSpace() {
        if (reservedSpaceVersion != state.moleculesVersion()) {
            BigNum reserved = BigNum.ZERO;
            for (String id : state.substances()) {
                Molecule molecule = molecules.get(id);
                if (molecule != null && molecule.hasState()) reserved = reserved.add(substanceSpace(id));
            }
            reservedSpace = reserved;
            reservedSpaceVersion = state.moleculesVersion();
        }
        return reservedSpace;
    }

    /** Espace utilisé : celui que les molécules occupent, et celui que réservent les lieux de rassemblement. */
    public BigNum usedSpace() {
        return occupiedSpace().add(reservedSpace());
    }

    /** Espace encore libre : celui que l'expansion a créé, moins celui qui est utilisé ({@link #usedSpace()}). Jamais négatif. */
    public BigNum freeSpace() {
        BigNum used = usedSpace();
        return state.space().gt(used) ? state.space().subtract(used) : BigNum.ZERO;
    }

    /** Vrai s'il reste au moins {@code amount} unités d'espace libre. */
    private boolean hasFreeSpace(BigNum amount) {
        // Comparé du côté des additions, avec une marge infime : 120 − 100 ne vaut pas tout à fait 20 en virgule flottante.
        return state.space().multiply(1 + 1e-9).gte(usedSpace().add(amount));
    }

    /**
     * Secondes de jeu avant que l'expansion ait créé {@code total} unités d'espace en tout : 0 si
     * c'est déjà fait, l'infini avant le premier Big Bang.
     */
    public double secondsUntilSpace(BigNum total) {
        if (hasCreatedSpace(total)) return 0;
        double missing = total.subtract(state.space()).toDouble();
        double rate = spacePerSecond().toDouble();
        return rate > 0 ? Math.max(0, missing) / rate : Double.POSITIVE_INFINITY;
    }

    /** Vrai si l'expansion a créé au moins {@code total} unités d'espace depuis le premier Big Bang, libres ou non. */
    private boolean hasCreatedSpace(BigNum total) {
        // Même marge infime que pour l'espace libre : 2 000 secondes d'expansion doivent donner 2 000 d'espace.
        return state.space().multiply(1 + 1e-9).gte(total);
    }

    /**
     * Vrai si une molécule de cette sorte peut se payer un jour avec le nombre d'exemplaires par
     * élément que le joueur a le droit de posséder en ce moment ({@link #maxCopiesOf(Element)}).
     * Faux : il faut d'abord les Isotopes de l'arbre, ou relever le plafond d'un de ses éléments.
     */
    public boolean isMoleculeWithinReach(String moleculeId) {
        for (Map.Entry<Integer, Integer> atom : nextMoleculeCost(moleculeId).entrySet()) {
            if (atom.getValue() + 1 > maxCopiesOf(PeriodicTable.element(atom.getKey()))) return false;
        }
        return true;
    }

    /** Vrai si le tableau périodique contient ce que demande la prochaine molécule de cette sorte, plus un exemplaire de chaque élément. */
    public boolean hasElementsForMolecule(String moleculeId) {
        for (Map.Entry<Integer, Integer> atom : nextMoleculeCost(moleculeId).entrySet()) {
            if (state.elementCount(atom.getKey()) < atom.getValue() + 1) return false;
        }
        return true;
    }

    /** Vrai s'il reste assez d'espace libre pour une molécule de cette sorte. */
    public boolean hasSpaceForMolecule(String moleculeId) {
        return hasFreeSpace(moleculeVolume(moleculeId));
    }

    /**
     * Vrai si une molécule de cette sorte peut être créée maintenant : son rayon est ouvert
     * ({@link #isMoleculeKindUnlocked}), le tableau périodique contient les exemplaires demandés (plus un de chaque élément,
     * qui doit y rester), et l'expansion a créé assez d'espace libre pour la loger.
     */
    public boolean canCreateMolecule(String moleculeId) {
        return state.started() && isMoleculeKindUnlocked(molecule(moleculeId).kind())
                && state.moleculeCount(molecule(moleculeId).id()) < MAX_MOLECULES
                && hasElementsForMolecule(moleculeId) && hasSpaceForMolecule(moleculeId);
    }

    /**
     * Vrai si ce rayon du catalogue est ouvert : le premier Big Bang a eu lieu, et le joueur
     * possède l'amélioration d'espace qui l'ouvre ({@link SpaceUpgrade.OpenKind}). Les petites
     * molécules n'en demandent aucune.
     */
    public boolean isMoleculeKindUnlocked(Molecule.Kind kind) {
        if (!isBigBangUnlocked()) return false;
        if (kind == Molecule.Kind.SIMPLE) return true;
        for (SpaceUpgrade upgrade : spaceUpgrades.values()) {
            if (upgrade.effect() instanceof SpaceUpgrade.OpenKind open && open.kind() == kind) {
                return state.ownsSpaceUpgrade(upgrade.id());
            }
        }
        return false;
    }

    /** Nombre de rayons du catalogue ouverts. */
    public int moleculeKindsUnlocked() {
        int unlocked = 0;
        for (Molecule.Kind kind : Molecule.Kind.values()) {
            if (isMoleculeKindUnlocked(kind)) unlocked++;
        }
        return unlocked;
    }

    /** Le premier rayon encore fermé, dans l'ordre du catalogue, ou {@code null} s'ils sont tous ouverts. */
    public Molecule.Kind nextMoleculeKind() {
        for (Molecule.Kind kind : Molecule.Kind.values()) {
            if (!isMoleculeKindUnlocked(kind)) return kind;
        }
        return null;
    }

    /**
     * Crée une molécule : les exemplaires que demande sa formule quittent le tableau périodique,
     * avec ce qu'ils y apportaient, et la molécule prend sa place dans l'espace. Il reste toujours
     * au moins un exemplaire de chaque élément : le tableau reste découvert, et l'explosion reste
     * possible. La molécule, elle, est définitive : ni l'explosion ni le Big Bang ne la reprennent,
     * et l'espace qu'elle occupe ne se libère pas.
     *
     * <p>Dans une sorte rassemblée, l'amas attire en plus de la matière : la création y ajoute
     * {@link #moleculesPerCreation(String)} molécules pour le même prix, dans la limite de l'espace
     * libre. C'est ce qui permet de réunir les milliers de molécules que demande un astre.
     *
     * @return {@code true} si la molécule a été créée
     */
    public boolean createMolecule(String moleculeId) {
        if (!canCreateMolecule(moleculeId)) return false;
        Molecule molecule = molecule(moleculeId);
        int created = moleculesNextCreation(moleculeId);
        for (Map.Entry<Integer, Integer> atom : nextMoleculeCost(moleculeId).entrySet()) {
            state.setElementCount(atom.getKey(), state.elementCount(atom.getKey()) - atom.getValue());
        }
        state.addMolecules(molecule.id(), created);
        return true;
    }

    /**
     * Nombre de molécules qu'une création ajoute à cette sorte quand la place ne manque pas : une
     * seule tant que la sorte n'est pas rassemblée ; ensuite une, plus la racine carrée du nombre
     * de molécules de l'amas (4 pour un amas de 9, 11 pour un amas de 100, 101 pour un amas de
     * 10 000). Plus un amas est gros, plus il attire ; et le palier du troisième Big Bang double
     * cette racine ({@link #accretionFactor()}).
     */
    public int moleculesPerCreation(String moleculeId) {
        Molecule molecule = molecule(moleculeId);
        int count = state.moleculeCount(molecule.id());
        if (!molecule.hasState() || !state.hasSubstance(molecule.id())) return 1;
        int drawn = 1 + (int) Math.floor(accretionFactor() * Math.sqrt(count) + 1e-9);
        return Math.max(1, Math.min(drawn, MAX_MOLECULES - count));
    }

    /**
     * Nombre de molécules que la prochaine création ajouterait vraiment : {@link #moleculesPerCreation(String)},
     * ou moins si l'espace libre ne suffit pas à toutes. Au moins une, même quand il n'y a de place pour aucune :
     * c'est {@link #canCreateMolecule(String)} qui dit si la création est possible.
     */
    public int moleculesNextCreation(String moleculeId) {
        int drawn = moleculesPerCreation(moleculeId);
        if (drawn == 1) return 1;
        // Même marge infime que pour l'espace libre : ce qui tient tout juste doit tenir.
        double room = state.space().multiply(1 + 1e-9).subtract(usedSpace()).toDouble() / volumeOf(molecule(moleculeId));
        return (int) Math.max(1, Math.min(drawn, Math.floor(room)));
    }

    /**
     * Ce par quoi l'acte du Big Bang multiplie une grandeur : {@code 1 + la somme} des bonus des
     * molécules sur cette grandeur ({@link Molecule.Boost}), chaque sorte comptée pour
     * {@link #effectiveMolecules(String)}, plus ce que donnent leurs rassemblements, les
     * assemblages formés ({@link Assembly#boost()}) et les astres formés ({@link Body#boost()}) ;
     * le tout multiplié par les améliorations d'espace qui portent sur cette grandeur
     * ({@link #spaceUpgradeBoost(Molecule.Stat)}) et par les paliers de Big Bang atteints
     * ({@link #bigBangMilestoneBoost(Molecule.Stat)}). Vaut 1 sans molécule, amélioration ni palier.
     */
    public double moleculeBoost(Molecule.Stat stat) {
        refreshMoleculeBonuses();
        return (1 + moleculeBoosts.getOrDefault(stat, 0.0)) * upgradeBoosts.getOrDefault(stat, 1.0)
                * bigBangMilestoneBoost(stat);
    }

    /**
     * Ce par quoi les améliorations d'espace acquises multiplient une grandeur
     * ({@link SpaceUpgrade.Boost}) : leurs facteurs se multiplient entre eux. Vaut 1 sans aucune.
     */
    public double spaceUpgradeBoost(Molecule.Stat stat) {
        refreshMoleculeBonuses();
        return upgradeBoosts.getOrDefault(stat, 1.0);
    }

    /**
     * Nombre d'exemplaires que les molécules créées ajoutent au maximum de cet élément : la partie
     * entière de ce pour quoi compte chaque sorte dont c'est le bonus ({@link Molecule.Uncap}).
     */
    public int elementUncap(int atomicNumber) {
        refreshMoleculeBonuses();
        return elementUncaps.getOrDefault(atomicNumber, 0);
    }

    /**
     * Recalcule ce que donnent les molécules, rassemblées ou non, quand leur liste a changé. Une
     * sorte de molécule compte pour {@link #effectiveMolecules(String)} : un plafond relevé prend
     * la partie entière de ce nombre. Une sorte rassemblée ajoute en plus les particules et les
     * atomes de son état, comptés de la même façon.
     */
    private void refreshMoleculeBonuses() {
        if (moleculeBonusesVersion == state.moleculesVersion()) return;
        moleculeBonusesVersion = state.moleculesVersion();
        moleculeBoosts.clear();
        elementUncaps.clear();
        upgradeBoosts.clear();
        for (SpaceUpgrade upgrade : spaceUpgrades.values()) {
            if (upgrade.effect() instanceof SpaceUpgrade.Boost boost && state.ownsSpaceUpgrade(upgrade.id())) {
                upgradeBoosts.merge(boost.stat(), boost.factor(), (a, b) -> a * b);
            }
        }
        for (String id : state.molecules().keySet()) {
            Molecule molecule = molecules.get(id);
            if (molecule == null) continue;
            double count = effectiveMolecules(id);
            switch (molecule.bonus()) {
                case Molecule.Boost boost -> moleculeBoosts.merge(boost.stat(), boost.perMolecule() * count, Double::sum);
                case Molecule.Uncap uncap -> elementUncaps.merge(uncap.element(), (int) Math.floor(count + 1e-9), Integer::sum);
                case null -> { }
            }
            if (gatheredMolecules(id) > 0) {
                Molecule.State matter = molecule.state();
                if (matter.particles() > 0) moleculeBoosts.merge(Molecule.Stat.PARTICLES, matter.particles() * count, Double::sum);
                if (matter.atoms() > 0) moleculeBoosts.merge(Molecule.Stat.ATOMS, matter.atoms() * count, Double::sum);
            }
        }
        // Les assemblages formés ajoutent chacun leur part à la grandeur de leur famille, et les astres à la leur.
        for (String id : state.assemblies()) {
            Assembly assembly = assemblies.get(id);
            if (assembly != null) moleculeBoosts.merge(assembly.boost().stat(), assembly.boost().perMolecule(), Double::sum);
        }
        for (String id : state.bodies()) {
            Body body = bodies.get(id);
            if (body != null) moleculeBoosts.merge(body.boost().stat(), body.boost().perMolecule(), Double::sum);
        }
        // La galaxie augmente toutes les grandeurs à la fois.
        if (state.hasGalaxy()) {
            for (Molecule.Stat stat : Molecule.Stat.values()) moleculeBoosts.merge(stat, GALAXY_GAIN, Double::sum);
        }
    }

    // ------------------------------------------------------------------
    // Les améliorations d'espace
    // ------------------------------------------------------------------

    /** Toutes les améliorations liées à l'espace, dans l'ordre du catalogue. */
    public List<SpaceUpgrade> spaceUpgrades() {
        if (spaceUpgradeList == null) spaceUpgradeList = List.copyOf(spaceUpgrades.values());
        return spaceUpgradeList;
    }

    private SpaceUpgrade spaceUpgrade(String spaceUpgradeId) {
        SpaceUpgrade upgrade = spaceUpgrades.get(spaceUpgradeId);
        if (upgrade == null) throw new IllegalArgumentException("Amélioration d'espace inconnue : " + spaceUpgradeId);
        return upgrade;
    }

    /** Vrai si cette amélioration d'espace est acquise. */
    public boolean ownsSpaceUpgrade(String spaceUpgradeId) {
        return state.ownsSpaceUpgrade(spaceUpgrade(spaceUpgradeId).id());
    }

    /**
     * Vrai si cette amélioration peut se prendre dès que l'espace créé suffit : le premier Big Bang
     * a eu lieu, et celle dont elle dépend est déjà acquise.
     */
    public boolean isSpaceUpgradeAvailable(String spaceUpgradeId) {
        SpaceUpgrade upgrade = spaceUpgrade(spaceUpgradeId);
        return isBigBangUnlocked() && (upgrade.requires() == null || state.ownsSpaceUpgrade(upgrade.requires()));
    }

    /**
     * Vrai si cette amélioration peut être prise maintenant : elle est accessible, pas encore
     * acquise, et l'expansion a créé en tout autant d'espace qu'elle en demande
     * ({@link SpaceUpgrade#space()}). L'espace libre n'entre pas en compte.
     */
    public boolean canBuySpaceUpgrade(String spaceUpgradeId) {
        SpaceUpgrade upgrade = spaceUpgrade(spaceUpgradeId);
        return state.started() && isSpaceUpgradeAvailable(spaceUpgradeId) && !state.ownsSpaceUpgrade(upgrade.id())
                && hasCreatedSpace(upgrade.space());
    }

    /**
     * Prend une amélioration d'espace. Rien n'est dépensé : c'est l'espace gagné qui l'ouvre, et il
     * reste entièrement disponible pour les molécules et leurs rassemblements. Elle est définitive, ni
     * l'explosion ni le Big Bang ne la reprennent.
     *
     * @return {@code true} si l'amélioration a été prise
     */
    public boolean buySpaceUpgrade(String spaceUpgradeId) {
        if (!canBuySpaceUpgrade(spaceUpgradeId)) return false;
        state.addSpaceUpgrade(spaceUpgrade(spaceUpgradeId).id());
        return true;
    }

    // ------------------------------------------------------------------
    // Les états de la matière : gaz, liquides, solides
    // ------------------------------------------------------------------

    /** Vrai une fois acquise l'amélioration d'espace qui ouvre le rassemblement des molécules ({@link SpaceUpgrade.OpenStates}). */
    public boolean isStatesUnlocked() {
        if (!isBigBangUnlocked()) return false;
        for (SpaceUpgrade upgrade : spaceUpgrades.values()) {
            if (upgrade.effect() instanceof SpaceUpgrade.OpenStates && state.ownsSpaceUpgrade(upgrade.id())) return true;
        }
        return false;
    }

    /** Vrai si les molécules de cette sorte sont rassemblées en leur substance : un gaz, un liquide ou un solide. */
    public boolean hasSubstance(String moleculeId) {
        return state.hasSubstance(molecule(moleculeId).id());
    }

    /**
     * Espace que réserve le lieu où se rassemblent les molécules de cette sorte : le volume des
     * {@link #SUBSTANCE_MOLECULES} molécules qu'il faut pour l'ouvrir ({@link #moleculeVolume(String)},
     * donc bien plus pour un gaz que pour un cristal). Il se paie une seule fois, et ne grandit pas
     * avec le nombre de molécules rassemblées.
     *
     * @throws IllegalArgumentException si ces molécules ne se rassemblent pas
     */
    public BigNum substanceSpace(String moleculeId) {
        Molecule molecule = molecule(moleculeId);
        if (!molecule.hasState()) throw new IllegalArgumentException(moleculeId + " ne forme pas de substance");
        return BigNum.of(volumeOf(molecule) * SUBSTANCE_MOLECULES);
    }

    /**
     * Vrai si les molécules de cette sorte peuvent être rassemblées maintenant : le rassemblement
     * est ouvert, elles ont un état, elles ne sont pas déjà rassemblées, le joueur en possède
     * {@link #SUBSTANCE_MOLECULES}, et l'espace libre suffit à leur lieu ({@link #substanceSpace(String)}).
     */
    public boolean canFormSubstance(String moleculeId) {
        Molecule molecule = molecule(moleculeId);
        if (!state.started() || !isStatesUnlocked() || !molecule.hasState()) return false;
        if (state.hasSubstance(molecule.id())) return false;
        if (state.moleculeCount(molecule.id()) < SUBSTANCE_MOLECULES) return false;
        return hasFreeSpace(substanceSpace(moleculeId));
    }

    /**
     * Rassemble les molécules d'une sorte en leur substance : un gaz, un liquide ou un solide,
     * selon la molécule ({@link Molecule#state()}). C'est un achat unique par sorte de molécule :
     * il réserve un lieu dans l'espace ({@link #substanceSpace(String)}), et toutes les molécules
     * de la sorte s'y rangent, celles d'aujourd'hui comme celles qui seront créées ensuite.
     *
     * <p>Aucune molécule n'est consommée : elles gardent leur place. Le rassemblement fait trois
     * choses. La sorte compte davantage dans les bonus, à l'exposant de son état
     * ({@link #effectiveMolecules(String)}) ; elle ajoute sa part de particules ou d'atomes ; et
     * son amas attire la matière : chaque création y ajoute plus d'une molécule
     * ({@link #moleculesPerCreation(String)}). Le rassemblement est définitif.
     *
     * @return {@code true} si les molécules ont été rassemblées
     */
    public boolean formSubstance(String moleculeId) {
        if (!canFormSubstance(moleculeId)) return false;
        state.addSubstance(molecule(moleculeId).id());
        return true;
    }

    /** Nombre de molécules de cette sorte rangées dans leur lieu de rassemblement : toutes si elles sont rassemblées, aucune sinon. */
    public int gatheredMolecules(String moleculeId) {
        Molecule molecule = molecule(moleculeId);
        return molecule.hasState() && state.hasSubstance(molecule.id()) ? state.moleculeCount(molecule.id()) : 0;
    }

    /**
     * Pour combien cette sorte compte dans les bonus : le nombre de fois qu'elle a doublé
     * ({@link #doublings(int)}), élevé à la puissance de son état quand elle est rassemblée. Trois
     * molécules d'un liquide (deux doublements, exposant 1,25) comptent pour 2,38 ; mille (dix
     * doublements), pour 17,7. Les premières molécules d'une sorte comptent donc beaucoup, les
     * suivantes de moins en moins : il vaut mieux beaucoup de sortes qu'une seule énorme.
     */
    public double effectiveMolecules(String moleculeId) {
        Molecule molecule = molecule(moleculeId);
        double doublings = doublings(state.moleculeCount(molecule.id()));
        if (!molecule.hasState() || !state.hasSubstance(molecule.id())) return doublings;
        return Math.pow(doublings, molecule.state().exponent());
    }

    /**
     * Le nombre de doublements d'un amas de {@code count} molécules : 1 pour une molécule, 2 pour
     * trois, 3 pour sept, 4 pour quinze, et ainsi de suite ; entre deux, la valeur glisse de l'un à
     * l'autre. C'est le logarithme en base deux de {@code count + 1}.
     */
    public static double doublings(int count) {
        return count <= 0 ? 0 : Math.log1p(count) / Math.log(2);
    }

    /** Nombre de sortes de molécules rassemblées dans le lieu de leur état. */
    public int substancesFormed() {
        return state.substances().size();
    }

    // ------------------------------------------------------------------
    // Les assemblages : roches, minerais, pierres précieuses, eaux, gaz, hydrocarbures
    // ------------------------------------------------------------------

    /** Vrai une fois acquise l'amélioration d'espace qui ouvre les assemblages ({@link SpaceUpgrade.OpenAssemblies}). */
    public boolean isAssembliesUnlocked() {
        if (!isBigBangUnlocked()) return false;
        for (SpaceUpgrade upgrade : spaceUpgrades.values()) {
            if (upgrade.effect() instanceof SpaceUpgrade.OpenAssemblies && state.ownsSpaceUpgrade(upgrade.id())) return true;
        }
        return false;
    }

    /** Tous les assemblages, dans l'ordre du catalogue ({@link Assemblies}). */
    public List<Assembly> assemblies() {
        if (assemblyList == null) assemblyList = List.copyOf(assemblies.values());
        return assemblyList;
    }

    /**
     * L'assemblage de cet identifiant.
     *
     * @throws IllegalArgumentException si l'identifiant est inconnu
     */
    public Assembly assembly(String assemblyId) {
        Assembly assembly = assemblies.get(assemblyId);
        if (assembly == null) throw new IllegalArgumentException("Assemblage inconnu : " + assemblyId);
        return assembly;
    }

    /** Vrai si cet assemblage est formé. */
    public boolean hasAssembly(String assemblyId) {
        return state.hasAssembly(assembly(assemblyId).id());
    }

    /** Nombre d'assemblages formés. */
    public int assembliesFormed() {
        return state.assemblies().size();
    }

    /**
     * Nombre de molécules de cette sorte entrées dans des assemblages : la somme de ce qu'en
     * demande chaque assemblage formé. Elles ne sont plus dans l'amas de leur sorte, et aucun
     * autre assemblage ne peut compter dessus.
     */
    public int assembledMolecules(String moleculeId) {
        String id = molecule(moleculeId).id();
        int assembled = 0;
        for (String formed : state.assemblies()) {
            Assembly assembly = assemblies.get(formed);
            if (assembly != null) assembled += assembly.ingredients().getOrDefault(id, 0);
        }
        return assembled;
    }

    /**
     * Nombre de molécules rassemblées de cette sorte qui ne sont dans aucun assemblage : ce qui
     * reste dans leur amas, et ce qu'un nouvel assemblage peut prendre. Nul si elles ne sont pas rassemblées.
     */
    public int spareMolecules(String moleculeId) {
        return Math.max(0, gatheredMolecules(moleculeId) - assembledMolecules(moleculeId));
    }

    /**
     * Vrai si cet assemblage peut être formé maintenant : les assemblages sont ouverts, il n'est
     * pas déjà formé, et chacun de ses ingrédients est rassemblé, avec assez de molécules encore
     * hors de tout assemblage ({@link #spareMolecules(String)}).
     */
    public boolean canFormAssembly(String assemblyId) {
        Assembly assembly = assembly(assemblyId);
        if (!state.started() || !isAssembliesUnlocked() || state.hasAssembly(assembly.id())) return false;
        for (Map.Entry<String, Integer> ingredient : assembly.ingredients().entrySet()) {
            if (spareMolecules(ingredient.getKey()) < ingredient.getValue()) return false;
        }
        return true;
    }

    /**
     * Forme un assemblage : c'est un achat unique, qui ne consomme rien. Les molécules qu'il
     * demande quittent l'amas de leur sorte pour entrer dans son bloc ; elles comptent toujours
     * comme des molécules de leur sorte, avec leur bonus et celui de leur rassemblement. En plus,
     * l'assemblage augmente la grandeur de sa famille ({@link Assembly#boost()}). Il est
     * définitif : ni l'explosion ni le Big Bang ne le défont.
     *
     * @return {@code true} si l'assemblage a été formé
     */
    public boolean formAssembly(String assemblyId) {
        if (!canFormAssembly(assemblyId)) return false;
        state.addAssembly(assembly(assemblyId).id());
        return true;
    }

    // ------------------------------------------------------------------
    // Les astres : amas de roches, comètes, astéroïdes, lunes, planètes
    // ------------------------------------------------------------------

    /** Vrai une fois acquise l'amélioration d'espace qui ouvre les astres ({@link SpaceUpgrade.OpenBodies}). */
    public boolean isBodiesUnlocked() {
        if (!isBigBangUnlocked()) return false;
        for (SpaceUpgrade upgrade : spaceUpgrades.values()) {
            if (upgrade.effect() instanceof SpaceUpgrade.OpenBodies && state.ownsSpaceUpgrade(upgrade.id())) return true;
        }
        return false;
    }

    /** Tous les astres, du plus petit au plus grand ({@link Bodies}). */
    public List<Body> bodies() {
        if (bodyList == null) bodyList = List.copyOf(bodies.values());
        return bodyList;
    }

    /**
     * L'astre de cet identifiant.
     *
     * @throws IllegalArgumentException si l'identifiant est inconnu
     */
    public Body body(String bodyId) {
        Body body = bodies.get(bodyId);
        if (body == null) throw new IllegalArgumentException("Astre inconnu : " + bodyId);
        return body;
    }

    /** Vrai si cet astre est formé. */
    public boolean hasBody(String bodyId) {
        return state.hasBody(body(bodyId).id());
    }

    /** Nombre d'astres formés. */
    public int bodiesFormed() {
        return state.bodies().size();
    }

    /**
     * L'astre dont cet assemblage fait partie : celui qui en est fait, dans le catalogue. Il n'y en
     * a qu'un, ou aucun ({@code null}). L'astre peut ne pas être formé encore ({@link #hasBody(String)}).
     */
    public Body bodyOf(String assemblyId) {
        return bodyByAssembly.get(assembly(assemblyId).id());
    }

    /**
     * Nombre de molécules rassemblées dans cet état, toutes sortes confondues, qu'elles soient
     * dans leur amas ou dans un assemblage : c'est la matière que demande un astre ({@link Body#matter()}).
     */
    public int gatheredInState(Molecule.State matter) {
        if (gatheredByStateVersion != state.moleculesVersion()) {
            gatheredByStateVersion = state.moleculesVersion();
            gatheredByState.clear();
            for (String id : state.substances()) {
                Molecule molecule = molecules.get(id);
                if (molecule != null && molecule.hasState()) gatheredByState.merge(molecule.state(), state.moleculeCount(id), Integer::sum);
            }
        }
        return gatheredByState.getOrDefault(matter, 0);
    }

    /** Nombre de conditions de cet astre déjà réunies, sur {@link Body#conditions()}. */
    public int bodyConditionsMet(String bodyId) {
        Body body = body(bodyId);
        int met = 0;
        for (String needed : body.bodies()) {
            if (state.hasBody(needed)) met++;
        }
        for (String needed : body.assemblies()) {
            if (state.hasAssembly(needed)) met++;
        }
        for (Map.Entry<Molecule.State, Integer> needed : body.matter().entrySet()) {
            if (gatheredInState(needed.getKey()) >= needed.getValue()) met++;
        }
        for (Map.Entry<String, Integer> needed : body.molecules().entrySet()) {
            if (state.moleculeCount(needed.getKey()) >= needed.getValue()) met++;
        }
        return met;
    }

    /**
     * Vrai si cet astre peut être formé maintenant : les astres sont ouverts, il n'est pas déjà
     * formé, et toutes ses conditions sont réunies ({@link #bodyConditionsMet(String)}) : les
     * assemblages dont il est fait et ses astres plus petits sont formés, sa matière est rassemblée,
     * ses molécules sont créées.
     */
    public boolean canFormBody(String bodyId) {
        Body body = body(bodyId);
        return state.started() && isBodiesUnlocked() && !state.hasBody(body.id())
                && bodyConditionsMet(bodyId) == body.conditions();
    }

    /**
     * Forme un astre : c'est un achat unique. Ses assemblages entrent dans l'astre et en font le
     * corps ({@link #bodyOf(String)}) ; ils restent formés et gardent leur bonus. Le reste de ce
     * qu'il demande n'est ni consommé ni réservé, et peut servir à d'autres astres. Une fois formé,
     * il augmente sa grandeur de ce que vaut son échelle ({@link Body#boost()}). Il est définitif :
     * ni l'explosion ni le Big Bang ne le défont.
     *
     * @return {@code true} si l'astre a été formé
     */
    public boolean formBody(String bodyId) {
        if (!canFormBody(bodyId)) return false;
        state.addBody(body(bodyId).id());
        return true;
    }

    // ------------------------------------------------------------------
    // La galaxie : tous les astres, rassemblés autour du trou noir supermassif
    // ------------------------------------------------------------------

    /** Ce que la galaxie ajoute à chacune des grandeurs une fois formée (1 024 = +102 400 %) : le double du trou noir supermassif. */
    public static final double GALAXY_GAIN = 1024;

    /** Nombre d'astres de cette échelle dans le catalogue. */
    public int bodiesIn(Body.Tier tier) {
        int count = 0;
        for (Body body : bodies.values()) {
            if (body.tier() == tier) count++;
        }
        return count;
    }

    /** Nombre d'astres formés de cette échelle. */
    public int bodiesFormed(Body.Tier tier) {
        int count = 0;
        for (String id : state.bodies()) {
            Body body = bodies.get(id);
            if (body != null && body.tier() == tier) count++;
        }
        return count;
    }

    /**
     * Vrai une fois la galaxie en vue : les astres sont ouverts et une première étoile est formée,
     * ou la galaxie l'est déjà.
     */
    public boolean isGalaxyUnlocked() {
        if (!isBodiesUnlocked()) return false;
        if (state.hasGalaxy()) return true;
        for (String id : state.bodies()) {
            Body body = bodies.get(id);
            if (body != null && body.tier().ordinal() >= Body.Tier.STAR.ordinal()) return true;
        }
        return false;
    }

    /** Vrai si la galaxie est formée. */
    public boolean hasGalaxy() {
        return state.hasGalaxy();
    }

    /**
     * Vrai si la galaxie peut être formée maintenant : les astres sont ouverts, elle ne l'est pas
     * déjà, et tous les astres du catalogue sont formés, du premier amas de roches au trou noir
     * supermassif.
     */
    public boolean canFormGalaxy() {
        return state.started() && isBodiesUnlocked() && !state.hasGalaxy() && state.bodies().size() == bodies.size();
    }

    /**
     * Forme la galaxie : tout ce que le joueur a créé, molécules, amas, assemblages et astres, se
     * met à tourner autour du trou noir supermassif. C'est un achat unique, qui ne consomme rien :
     * les astres restent formés et gardent leur bonus. La galaxie ajoute {@link #GALAXY_GAIN} à
     * chacune des grandeurs que les molécules augmentent, et rien ne la défait, ni l'explosion ni
     * le Big Bang.
     *
     * @return {@code true} si la galaxie a été formée
     */
    public boolean formGalaxy() {
        if (!canFormGalaxy()) return false;
        state.setGalaxy(true);
        return true;
    }

    // ------------------------------------------------------------------
    // L'expansion de la matière
    // ------------------------------------------------------------------

    /**
     * Espace que l'expansion de la matière ajoute chaque seconde : {@link #SPACE_PER_SECOND} par
     * Big Bang déclenché, augmenté par les molécules, les assemblages et les astres qui
     * l'accélèrent, par les améliorations d'inflation, par les paliers de Big Bang, et, une fois le
     * palier atteint, par la matière noire en réserve ({@link #darkMatterSpaceBoost()}). Nul avant le
     * premier Big Bang.
     */
    public BigNum spacePerSecond() {
        if (!isBigBangUnlocked()) return BigNum.ZERO;
        return BigNum.of(SPACE_PER_SECOND * state.bigBangs() * moleculeBoost(Molecule.Stat.SPACE) * darkMatterSpaceBoost());
    }

    /**
     * Fait passer le temps de l'expansion : l'espace grandit de {@link #spacePerSecond()} par
     * seconde de jeu, quoi que fasse le joueur. Rien ne le reprend, ni l'explosion ni le Big Bang ;
     * les molécules en occupent une part ({@link #occupiedSpace()}), leurs lieux de rassemblement en réservent une autre
     * ({@link #reservedSpace()}).
     */
    private void expandSpace(double dt) {
        if (dt <= 0 || !state.started() || !isBigBangUnlocked()) return;
        state.setSpace(state.space().add(spacePerSecond().multiply(dt)));
    }

    // ------------------------------------------------------------------
    // Les automatismes de matière noire
    // ------------------------------------------------------------------

    /** Tous les automatismes de matière noire, dans l'ordre du catalogue. */
    public Collection<DarkAutomation> darkAutomations() {
        return darkAutomations.values();
    }

    /** Vrai quand le joueur a gagné assez de matière noire pour cet automatisme. Rien n'est dépensé. */
    public boolean isDarkAutomationUnlocked(String darkAutomationId) {
        return darkMatterEarned().gte(darkAutomation(darkAutomationId).darkMatter());
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
    /** Délai actuel entre deux actions de cet automatisme de matière noire, en secondes. */
    public double darkAutomationInterval(String darkAutomationId) {
        double interval = darkAutomation(darkAutomationId).interval();
        return ruled(Challenge.Rule.RUSTY_AUTOMATIONS) ? Math.max(RUSTY_MIN_INTERVAL, interval * RUSTY_FACTOR) : interval;
    }

    private void runDarkAutomation(double dt) {
        for (DarkAutomation automation : darkAutomations.values()) {
            if (!isDarkAutomationEnabled(automation.id())) continue;
            double waited = state.darkAutomationTimer(automation.id()) + dt;
            double interval = darkAutomationInterval(automation.id());
            if (waited >= interval - 1e-9) {
                // Rien à faire pour l'instant : il reste prêt, et agira dès que ce sera possible.
                boolean acted = act(automation);
                if (acted) state.stats().noteDarkAutomationAction();
                waited = acted ? Math.max(0, waited - interval) : interval;
            }
            state.setDarkAutomationTimer(automation.id(), waited);
        }
    }

    /** Fait faire à l'automatisme de matière noire une action ; faux s'il ne peut rien faire pour l'instant. */
    private boolean act(DarkAutomation automation) {
        return switch (automation.kind()) {
            case GRANT_AUTOMATIONS -> grantAutomation();
            case ATOM_UPGRADES -> buyCheapestAtomUpgrade();
            case AUTOMATIONS -> buyCheapestAutomation();
            case EXPLOSION -> explode();
        };
    }

    /**
     * Offre au joueur un automatisme ordinaire qu'il ne possède pas encore, mis en marche
     * aussitôt : un par action, dans l'ordre du catalogue. Ceux qui achètent et fusionnent sont
     * offerts tout de suite. La synthèse automatique ne l'est qu'une fois le tableau périodique
     * ouvert et les autres à leur cadence maximale : avant, les atomes servent mieux ailleurs, et
     * elle ne doit pas les dépenser dans le dos du joueur. Elle n'attend pas d'élément unique.
     *
     * @return {@code true} si un automatisme a été offert
     */
    private boolean grantAutomation() {
        if (inChallenge()) return false;   // un défi se joue sans les mémoires de la matière noire
        for (Automation automation : automations.values()) {
            if (state.ownsAutomation(automation.id())) continue;
            if (automation.kind() == Automation.Kind.SYNTHESIS
                    && !(isPeriodicTableUnlocked() && ordinaryAutomationsMaxed())) continue;
            state.addAutomation(automation.id());
            state.setAutomationEnabled(automation.id(), true);
            return true;
        }
        return false;
    }

    /** Vrai quand tous les automatismes ordinaires, hors synthèse, sont achetés et à leur cadence maximale. */
    private boolean ordinaryAutomationsMaxed() {
        for (Automation automation : automations.values()) {
            if (automation.kind() == Automation.Kind.SYNTHESIS) continue;
            if (!state.ownsAutomation(automation.id()) || !isAutomationMaxed(automation.id())) return false;
        }
        return true;
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

    /** Toutes les améliorations de matière noire, dans l'ordre du catalogue : l'arbre, puis celles payées en matière noire. */
    public Collection<DarkUpgrade> darkUpgrades() {
        return darkUpgrades.values();
    }

    /** Les améliorations de matière noire d'une branche, dans l'ordre du catalogue. */
    public List<DarkUpgrade> darkUpgrades(DarkUpgrade.Branch branch) {
        List<DarkUpgrade> result = new ArrayList<>();
        for (DarkUpgrade dark : darkUpgrades.values()) {
            if (dark.branch() == branch) result.add(dark);
        }
        return result;
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

    /** Vrai si le joueur a gagné la matière noire que demande cette case. */
    public boolean hasDarkMatterFor(String darkUpgradeId) {
        return darkMatterEarned().gte(BigNum.of(darkUpgrade(darkUpgradeId).darkMatter()));
    }

    /** Prix du prochain niveau, dans l'unité de sa branche (particules, atomes, ou mètres à atteindre). */
    public BigNum darkCostOf(String darkUpgradeId) {
        DarkUpgrade dark = darkUpgrade(darkUpgradeId);
        return dark.costAt(state.darkLevelOf(dark.id()));
    }

    /**
     * Ce que le joueur a dans l'unité d'une branche : particules, atomes disponibles, taille de la
     * matière noire en mètres, ou matière noire qui reste à dépenser. Pour les particules, c'est
     * ce qu'il possède ou ce qu'il produit en une seconde, le plus grand des deux.
     */
    public BigNum darkBalance(DarkUpgrade.Branch branch) {
        return switch (branch) {
            // Une seconde de production vaut des particules en main : sans cela, une fusion automatique
            // rapide, qui remet les particules à zéro vingt fois par seconde, rendrait la branche inachetable.
            case PARTICLES -> state.particles().max(productionPerSecond());
            case ATOMS -> state.atoms();
            case SIZE -> state.darkMatterSize();
            case DARK_MATTER -> state.darkMatter();
        };
    }

    public boolean canBuyDark(String darkUpgradeId) {
        DarkUpgrade dark = darkUpgrade(darkUpgradeId);
        return isDarkAvailable(darkUpgradeId) && !isDarkMaxed(darkUpgradeId)
                && darkBalance(dark.branch()).gte(darkCostOf(darkUpgradeId));
    }

    /**
     * Achète un niveau d'une amélioration de matière noire. Les particules, les atomes et la
     * matière noire sont dépensés ; une taille à atteindre ne coûte rien, la matière noire ne
     * rétrécit pas.
     *
     * @return {@code true} si l'achat a eu lieu
     */
    public boolean buyDark(String darkUpgradeId) {
        if (!canBuyDark(darkUpgradeId)) return false;
        DarkUpgrade dark = darkUpgrade(darkUpgradeId);
        BigNum cost = darkCostOf(darkUpgradeId);
        switch (dark.branch()) {
            case PARTICLES -> {
                state.setParticles(state.particles().subtract(cost).max(BigNum.ZERO));
                state.stats().addParticlesSpent(cost);
            }
            case ATOMS -> {
                state.setAtoms(state.atoms().subtract(cost).max(BigNum.ZERO));
                state.stats().addAtomsSpent(cost);
            }
            case SIZE -> { /* un seuil, pas un prix */ }
            case DARK_MATTER -> {
                // Dépensée, mais toujours comptée comme gagnée : rien de ce qu'elle ouvre ne se referme.
                state.setDarkMatter(state.darkMatter().subtract(cost).max(BigNum.ZERO));
                state.setDarkMatterSpent(state.darkMatterSpent().add(cost));
            }
        }
        state.setDarkLevel(dark.id(), state.darkLevelOf(dark.id()) + 1);
        applyHeadStart();
        return true;
    }

    /**
     * Somme, sur les améliorations d'un type, de {@code par unité × niveau × matière noire gagnée} :
     * la forme commune des effets payés en matière noire.
     */
    private double darkMatterScaling(java.util.function.ToDoubleFunction<DarkEffect> perUnit) {
        double total = 0;
        for (DarkUpgrade dark : darkUpgrades.values()) {
            int level = state.darkLevelOf(dark.id());
            if (level > 0) total += perUnit.applyAsDouble(dark.effect()) * level;
        }
        return total * darkMatterUnits();
    }

    /** Ce par quoi les améliorations payées en matière noire multiplient les atomes de chaque fusion. */
    public double darkAtomsMultiplier() {
        return 1 + darkMatterScaling(effect -> effect instanceof DarkEffect.AtomsByDarkMatter by ? by.perUnit() : 0);
    }

    /** Ce par quoi les améliorations payées en matière noire divisent le délai des automatismes ordinaires. */
    public double darkAutomationDivisor() {
        return 1 + darkMatterScaling(effect -> effect instanceof DarkEffect.AutomationsByDarkMatter by ? by.perUnit() : 0);
    }

    /** Ce par quoi les améliorations payées en matière noire divisent le prix de la synthèse. */
    public double darkSynthesisDivisor() {
        return 1 + darkMatterScaling(effect -> effect instanceof DarkEffect.SynthesisByDarkMatter by ? by.perUnit() : 0);
    }

    /**
     * Atomes déjà comptés comme créés au départ de chaque partie, grâce aux améliorations payées
     * en matière noire : jamais plus que {@link #UNLOCK_TOTAL_ATOMS}.
     */
    public BigNum darkHeadStartAtoms() {
        if (inChallenge()) return BigNum.ZERO;
        double atoms = Math.floor(darkMatterScaling(
                effect -> effect instanceof DarkEffect.HeadStartByDarkMatter by ? by.perUnit() : 0));
        return BigNum.of(atoms).min(UNLOCK_TOTAL_ATOMS);
    }

    /** Porte le compte des atomes créés au niveau du départ lancé, s'il est en dessous. */
    private void applyHeadStart() {
        BigNum headStart = darkHeadStartAtoms();
        if (state.totalAtoms().lt(headStart)) state.setTotalAtoms(headStart);
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
        if (rewarded(Challenge.Reward.EXTRA_ELEMENT)) count += REWARD_EXTRA_ELEMENTS;
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
                result = result.multiply(BigNum.of(byDarkMatter.perUnit()).pow(darkMatterUnits() * level));
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
        if (inChallenge()) return 0;
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
        for (Element element : PeriodicTable.ELEMENTS) copies += maxCopiesOf(element);
        return copies;
    }

    /**
     * Nombre maximal d'exemplaires de cet élément : celui de sa famille
     * ({@link #maxCopiesOf(ElementCategory)}), plus un par molécule créée qui relève son plafond
     * ({@link Molecule.Uncap}). Un élément unique reste unique.
     */
    public int maxCopiesOf(Element element) {
        if (element.category().unique()) return 1;
        return maxCopiesOf(element.category()) + elementUncap(element.number());
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
     * Niveaux de vitesse qui comptent pour une amélioration de vitesse : ceux achetés, plus ceux
     * offerts par l'arbre de matière noire.
     */
    public int speedLevels(String upgradeId) {
        return state.levelOf(upgrade(upgradeId).id()) + startingSpeedLevels();
    }

    /** Nombre de paliers de vitesse atteints, toutes améliorations de vitesse confondues. */
    public int speedMilestones() {
        int milestones = 0;
        for (Upgrade upgrade : upgrades.values()) {
            if (upgrade.effect() instanceof Effect.MultiplySpeed speed) {
                milestones += speed.milestonesAt(speedLevels(upgrade.id()));
            }
        }
        return milestones;
    }

    /**
     * Ce que les paliers de vitesse multiplient dans les particules de chaque création : ×2 tous
     * les 25 niveaux de « Vitesse de création » dans le jeu. Vaut 1 avant le premier palier.
     */
    public BigNum speedMilestoneMultiplier() {
        BigNum result = BigNum.ONE;
        for (Upgrade upgrade : upgrades.values()) {
            if (upgrade.effect() instanceof Effect.MultiplySpeed speed && speed.hasMilestones()) {
                result = result.multiply(BigNum.of(speedMilestoneFactor(upgrade.id())).pow(speed.milestonesAt(speedLevels(upgrade.id()))));
            }
        }
        return result;
    }

    /**
     * Ce que multiplie chaque palier de cette amélioration de vitesse : le facteur du catalogue,
     * plus ce qu'y ajoute le défi réussi ; 1 pour une amélioration sans palier.
     */
    public double speedMilestoneFactor(String upgradeId) {
        if (!(upgrade(upgradeId).effect() instanceof Effect.MultiplySpeed speed) || !speed.hasMilestones()) return 1;
        return speed.milestoneFactor() + (rewarded(Challenge.Reward.STRONGER_MILESTONES) ? REWARD_MILESTONE_EXTRA : 0);
    }

    /**
     * Niveau (offerts compris) auquel cette amélioration de vitesse atteindra son prochain palier,
     * ou 0 si elle n'a pas de palier.
     */
    public int nextSpeedMilestone(String upgradeId) {
        if (!(upgrade(upgradeId).effect() instanceof Effect.MultiplySpeed speed) || !speed.hasMilestones()) return 0;
        return (speed.milestonesAt(speedLevels(upgradeId)) + 1) * speed.milestoneEvery();
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
        return factor * Math.max(0.5, 1 - achievementBonus(Achievement.Bonus.GENERATOR_COST));
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
        if (inChallenge()) return 0;
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
        return ruled(Challenge.Rule.HEAVY_FUSION) ? count * HEAVY_FUSION_FACTOR : count;
    }

    /**
     * Particules obtenues à chaque création, avant les bonus propres à chaque générateur :
     * une de base, multipliée par tous les bonus achetés, par les paliers de vitesse et par les
     * bonus des éléments.
     */
    public BigNum particlesPerCreation() {
        return particlesWith(generatorCount());
    }

    /** Particules par création s'il y avait {@code generators} générateurs : seul le couplage en dépend. */
    private BigNum particlesWith(int generators) {
        BigNum result = BigNum.of(elementMultiplier(ElementEffect.Stat.PARTICLES))
                .multiply(darkParticlesMultiplier())
                .multiply(landmarkParticlesMultiplier())
                .multiply(achievementParticlesMultiplier())
                .multiply(moleculeBoost(Molecule.Stat.PARTICLES))
                .multiply(speedMilestoneMultiplier());
        for (Upgrade upgrade : upgrades.values()) {
            result = result.multiply(particlesMultiplier(upgrade, state.levelOf(upgrade.id()), generators));
        }
        return result;
    }

    /** Particules obtenues à chaque création d'un générateur précis (numéroté à partir de 0). */
    public BigNum particlesPerCreation(int generator) {
        return particlesPerCreation().multiply(generatorParticlesMultiplier(generator));
    }

    /**
     * Multiplicateur de particules qu'apporte une amélioration à un niveau donné, dans la
     * situation actuelle (atomes créés, temps écoulé, générateurs, éléments qui la renforcent).
     * Vaut 1 au niveau 0, et toujours 1 pour une amélioration qui n'agit pas sur les particules
     * par création.
     *
     * <p>L'interface s'en sert pour afficher l'effet actuel d'une amélioration, ou ce
     * qu'elle donnerait une fois achetée.
     */
    public BigNum particlesMultiplier(String upgradeId, int level) {
        return particlesMultiplier(upgrade(upgradeId), level, generatorCount());
    }

    private BigNum particlesMultiplier(Upgrade upgrade, int level, int generators) {
        if (level <= 0) return BigNum.ONE;
        ElementBonuses bonuses = bonuses();
        return switch (upgrade.effect()) {
            case Effect.MultiplyParticles multiply ->
                    BigNum.of(multiply.perLevel() + bonuses.upgradeBoost(ElementEffect.UpgradeTarget.DOUBLING)).pow(level);
            // Plafonné comme les atomes eux-mêmes : sans cela, le bonus grandirait sans fin
            // une fois les fusions automatisées.
            case Effect.MultiplyByAtoms byAtoms -> BigNum.ONE.add(state.totalAtoms().min(MAX_ATOMS).multiply(
                    byAtoms.perAtom() + bonuses.upgradeBoost(ElementEffect.UpgradeTarget.MASS))).pow(level);
            case Effect.MultiplyByRunTime byTime -> BigNum.of(1
                    + (byTime.factor() + bonuses.upgradeBoost(ElementEffect.UpgradeTarget.PATIENCE))
                    * Math.sqrt(state.timeSinceFusion() / 60)).pow(level);
            case Effect.Overload overload -> BigNum.of(overload.perLevel()).pow(level);
            // Additif, pas exponentiel : chaque niveau ajoute la même part par autre générateur.
            case Effect.MultiplyByGenerators coupling ->
                    BigNum.of(1 + coupling.perGenerator() * level * Math.max(0, generators - 1));
            // Les paliers de vitesse sont comptés à part : ils tiennent compte des niveaux offerts.
            case Effect.MultiplySpeed speed -> BigNum.ONE;
            case Effect.AddGenerator generator -> BigNum.ONE;
            case Effect.StrengthenSpeed strengthen -> BigNum.ONE;
            case Effect.DiscountGenerators discount -> BigNum.ONE;
            case Effect.KeepUpgradesOnFusion keep -> BigNum.ONE;
            case Effect.MultiplyAtomsByProduction byProduction -> BigNum.ONE;
        };
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
        BigNum common = speed().multiply(particlesWith(generators));
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
     * {@linkplain #fusionYield() rendement de fusion}, par le nombre de groupes de générateurs
     * ({@link #fusionGroups()}) et par leur {@linkplain #fusionGroupBonus() prime}. Le résultat peut être fractionnaire : les fractions s'accumulent
     * d'une fusion à l'autre.
     */
    public BigNum atomsPerFusion() {
        return ATOMS_PER_FUSION.add(BigNum.of(darkAtomsPerFusion()))
                .multiply(elementMultiplier(ElementEffect.Stat.ATOMS))
                .multiply(darkAtomsMultiplier())
                .multiply(landmarkAtomsMultiplier())
                .multiply(1 + achievementBonus(Achievement.Bonus.ATOMS))
                .multiply(moleculeBoost(Molecule.Stat.ATOMS))
                .multiply(fusionYield())
                .multiply(fusionGroups() * fusionGroupBonus());
    }

    /**
     * La prime de groupe : une fusion de plusieurs groupes de générateurs rapporte plus que la
     * somme des groupes, {@link #FUSION_GROUP_BONUS} de plus par groupe au-delà du premier
     * (×1,1 avec deux groupes, ×1,9 avec dix). C'est ce qui rend l'attente payante quand le
     * nombre de générateurs n'est plus limité à dix.
     */
    public double fusionGroupBonus() {
        return 1 + fusionGroupBonusPerGroup() * (fusionGroups() - 1);
    }

    /** Ce que chaque groupe au-delà du premier ajoute à la fusion : {@link #FUSION_GROUP_BONUS}, ou plus après le défi réussi. */
    public double fusionGroupBonusPerGroup() {
        return rewarded(Challenge.Reward.GROUP_BONUS) ? REWARD_GROUP_BONUS : FUSION_GROUP_BONUS;
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
        return !isAtomCapLifted() && state.atoms().gte(atomCap());
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
        if (pulse > 0) expandDarkMatter(pulse);
        BigNum production = productionPerSecond();
        state.stats().noteProduction(production);
        // La production va retomber : l'historique retient le sommet qu'elle vient d'atteindre.
        state.stats().history().notePeak(production.log10());
        state.stats().runHistory().notePeak(production.log10());
        BigNum gained = atomsPerFusion();
        noteFusionFeats();
        state.stats().noteFusion(gained, state.timeSinceFusion());
        BigNum atoms = state.atoms().add(gained);
        state.setAtoms(isAtomCapLifted() ? atoms : atoms.min(atomCap()));
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
        if (ruled(Challenge.Rule.AMNESIA)) {
            // Le défi : la fusion reprend aussi tout ce qui a été acheté en atomes, Persistance comprise.
            for (Upgrade upgrade : upgrades.values()) {
                if (upgrade.resource() == Resource.ATOMS) state.setLevel(upgrade.id(), 0);
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
        return costAt(upgrade, state.levelOf(upgrade.id()));
    }

    /** Coût du niveau acheté quand on en possède {@code level}, toutes réductions comprises. */
    private BigNum costAt(Upgrade upgrade, int level) {
        BigNum cost = upgrade.costAt(level);
        ElementBonuses bonuses = bonuses();
        double factor = 1;
        if (upgrade.effect() instanceof Effect.AddGenerator) {
            factor = generatorCostFactor();
        } else if (upgrade.effect() instanceof Effect.MultiplySpeed) {
            factor = 1 / bonuses.costDivisor(ElementEffect.CostTarget.SPEED_UPGRADES);
        } else if (upgrade.resource() == Resource.ATOMS) {
            factor = 1 / bonuses.costDivisor(ElementEffect.CostTarget.ATOM_UPGRADES);
            if (rewarded(Challenge.Reward.CHEAPER_ATOM_UPGRADES)) factor /= REWARD_ATOM_UPGRADE_DIVISOR;
        }
        if (factor != 1) {
            cost = Upgrade.roundUp(cost.multiply(factor)).max(BigNum.ONE);
        }
        return cost;
    }

    /** Niveaux qu'il reste à acheter avant le maximum de cette amélioration ({@link #MAX_BULK} au plus). */
    private int levelsLeft(Upgrade upgrade) {
        // Les générateurs : la limite est celle du moment, que la matière noire peut repousser.
        if (upgrade.effect() instanceof Effect.AddGenerator) {
            return state.started() ? Math.max(0, maxGeneratorCount() - generatorCount()) : 0;
        }
        return Math.min(MAX_BULK, Math.max(0, upgrade.maxLevel() - state.levelOf(upgrade.id())));
    }

    /**
     * Nombre de niveaux de cette amélioration que le joueur peut payer d'un coup, sans dépasser
     * {@code limit} ni le maximum de l'amélioration. C'est ce qu'achèterait {@link #buy(String, int)}.
     */
    public int affordableLevels(String upgradeId, int limit) {
        Upgrade upgrade = upgrade(upgradeId);
        if (!state.started()) return 0;
        int most = Math.min(Math.min(limit, MAX_BULK), levelsLeft(upgrade));
        int level = state.levelOf(upgrade.id());
        BigNum left = balance(upgrade.resource());
        int count = 0;
        while (count < most) {
            BigNum cost = costAt(upgrade, level + count);
            if (left.lt(cost)) break;
            left = left.subtract(cost);
            count++;
        }
        return count;
    }

    /**
     * Coût total des {@code count} prochains niveaux de cette amélioration, toutes réductions
     * comprises. Les niveaux au-delà du maximum ne sont pas comptés.
     */
    public BigNum costOf(String upgradeId, int count) {
        Upgrade upgrade = upgrade(upgradeId);
        int level = state.levelOf(upgrade.id());
        int levels = Math.min(Math.min(count, MAX_BULK), Math.max(1, levelsLeft(upgrade)));
        BigNum total = BigNum.ZERO;
        for (int i = 0; i < levels; i++) {
            total = total.add(costAt(upgrade, level + i));
        }
        return total;
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
        BigNum cost = costOf(upgradeId);
        BigNum left = balance(upgrade.resource()).subtract(cost).max(BigNum.ZERO);
        switch (upgrade.resource()) {
            case PARTICLES -> {
                state.setParticles(left);
                state.stats().noteParticleUpgrade(cost, upgrade.effect() instanceof Effect.AddGenerator);
            }
            case ATOMS -> {
                state.setAtoms(left);
                state.stats().noteAtomUpgrade(cost);
                if (left.isZero() && cost.toDouble() >= Achievements.EXACT_CHANGE_COST) award(Achievements.EXACT_CHANGE);
            }
        }
        state.setLevel(upgradeId, state.levelOf(upgradeId) + 1);
        return true;
    }

    /**
     * Achète jusqu'à {@code count} niveaux d'une amélioration : autant que le joueur peut en
     * payer, sans dépasser son maximum. Pour tout acheter, passer {@link Integer#MAX_VALUE}.
     *
     * @return le nombre de niveaux achetés, 0 si aucun n'était à portée
     */
    public int buy(String upgradeId, int count) {
        int bought = 0;
        while (bought < count && bought < MAX_BULK && buy(upgradeId)) {
            bought++;
        }
        if (bought >= Achievements.BULK_LEVELS) award(Achievements.BULK_PURCHASE);
        return bought;
    }

    /**
     * Achète tout ce qui peut l'être avec les particules, en prenant chaque fois l'amélioration
     * la moins chère : c'est elle qui retarde le moins les suivantes.
     *
     * @return le nombre de niveaux achetés
     */
    public int buyAllWithParticles() {
        int bought = 0;
        while (bought < MAX_BULK) {
            Upgrade cheapest = null;
            for (Upgrade upgrade : upgrades.values()) {
                if (upgrade.resource() != Resource.PARTICLES || !canBuy(upgrade.id())) continue;
                if (cheapest == null || costOf(upgrade.id()).lt(costOf(cheapest.id()))) cheapest = upgrade;
            }
            if (cheapest == null || !buy(cheapest.id())) break;
            bought++;
        }
        if (bought >= Achievements.BUY_EVERYTHING_LEVELS) award(Achievements.BUY_EVERYTHING);
        return bought;
    }

    /**
     * Secondes à attendre avant de pouvoir payer {@code cost} particules : 0 si le joueur les a
     * déjà, l'infini si rien ne produit.
     *
     * <p>Le calcul suit les créations une à une : il tient compte de celle que chaque générateur
     * a déjà commencée. L'attente annoncée diminue donc d'une seconde par seconde, au lieu de
     * rester figée entre deux particules puis de sauter. Ce n'est qu'une estimation au sens où
     * elle suppose que rien ne change d'ici là (ni achat, ni bonus qui grandit avec le temps).
     */
    public double secondsUntilParticles(BigNum cost) {
        BigNum missing = cost.subtract(state.particles());
        if (missing.sign() <= 0) return 0;
        int generators = generatorCount();
        BigNum production = productionPerSecond();
        if (generators == 0 || production.sign() <= 0) return Double.POSITIVE_INFINITY;
        double average = missing.divide(production).toDouble();

        // Les particules se comptent en « créations de base » : seuls les bonus propres à chaque
        // générateur les distinguent. Au-delà d'un grand nombre de créations, ou à très grande
        // vitesse, une création de plus ou de moins ne se voit plus : la moyenne suffit.
        BigNum commonSpeed = speed();
        BigNum needed = missing.divide(particlesPerCreation());
        if (commonSpeed.exponent() >= 6 || needed.exponent() >= 9) return average;
        double need = needed.toDouble();
        double[] speeds = new double[generators];
        double[] weights = new double[generators];
        double longest = 0;
        for (int generator = 0; generator < generators; generator++) {
            speeds[generator] = commonSpeed.toDouble() * generatorSpeedMultiplier(generator);
            weights[generator] = generatorParticlesMultiplier(generator);
            if (speeds[generator] > 0) longest = Math.max(longest, 1 / speeds[generator]);
        }
        // Le plus petit instant où assez de créations sont terminées. Au plus tard : l'attente
        // moyenne, plus la durée d'une création du générateur le plus lent.
        double low = 0;
        double high = average + longest;
        for (int i = 0; i < 60; i++) {
            double middle = (low + high) / 2;
            double made = 0;
            for (int generator = 0; generator < generators; generator++) {
                made += Math.floor(state.formation(generator) + speeds[generator] * middle + 1e-9) * weights[generator];
            }
            if (made >= need - 1e-9) high = middle; else low = middle;
        }
        return high;
    }

    /**
     * Nombre de fusions qu'il faut encore, au rythme actuel, avant de pouvoir payer {@code cost}
     * atomes : 0 si le joueur les a déjà, −1 si le prix dépasse le plafond d'atomes et ne peut
     * donc pas être atteint tant que ce plafond n'est pas levé.
     */
    public long fusionsUntilAtoms(BigNum cost) {
        BigNum missing = cost.subtract(state.atoms());
        if (missing.sign() <= 0) return 0;
        if (!isAtomCapLifted() && cost.gt(atomCap())) return -1;
        // La petite marge absorbe les erreurs d'arrondi : 3 atomes manquants à 1 par fusion font 3 fusions.
        return (long) Math.min(1e15, Math.ceil(missing.divide(atomsPerFusion()).toDouble() - 1e-9));
    }

    /** Quantité disponible d'une ressource. */
    public BigNum balance(Resource resource) {
        return switch (resource) {
            case PARTICLES -> state.particles();
            case ATOMS -> state.atoms();
        };
    }

    /**
     * Les statistiques de la partie, à jour : le record de production est relevé au passage, pour
     * qu'il tienne compte de la production du moment et pas seulement de celle des fusions.
     */
    public GameStats stats() {
        if (state.started()) state.stats().noteProduction(productionPerSecond());
        return state.stats();
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
