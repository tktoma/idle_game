package idle.core;

import idle.core.achievements.Achievement;
import idle.core.achievements.Achievements;
import idle.core.bigbang.BangAchievement;
import idle.core.bigbang.BangChallenge;
import idle.core.bigbang.BigBangCondition;
import idle.core.bigbang.BigBangMilestone;
import idle.core.bigbang.BigBangMilestones;
import idle.core.bigbang.SpaceUpgrade;
import idle.core.bigbang.SpaceUpgrades;
import idle.core.collider.ColliderAchievement;
import idle.core.collider.ColliderGenerator;
import idle.core.collider.ColliderUpgrade;
import idle.core.collider.Collision;
import idle.core.collider.CollisionTier;
import idle.core.collider.Quark;
import idle.core.darkmatter.Challenge;
import idle.core.darkmatter.Challenges;
import idle.core.darkmatter.DarkAutomation;
import idle.core.darkmatter.DarkAutomations;
import idle.core.darkmatter.DarkEffect;
import idle.core.darkmatter.DarkUpgrade;
import idle.core.darkmatter.DarkUpgrades;
import idle.core.darkmatter.SizeScale;
import idle.core.elements.Element;
import idle.core.elements.ElementBonuses;
import idle.core.elements.ElementCategory;
import idle.core.elements.ElementEffect;
import idle.core.elements.ElementSet;
import idle.core.elements.ElementSets;
import idle.core.elements.PeriodicTable;
import idle.core.math.BigNum;
import idle.core.molecules.KindCollection;
import idle.core.molecules.KindCollections;
import idle.core.molecules.Molecule;
import idle.core.molecules.Molecules;
import idle.core.molecules.ShoppingList;
import idle.core.probes.Probe;
import idle.core.probes.ProbeDestination;
import idle.core.probes.ProbeReward;
import idle.core.probes.ProbeTrip;
import idle.core.probes.ProbeUpgrade;
import idle.core.sky.Assemblies;
import idle.core.sky.Assembly;
import idle.core.sky.Bodies;
import idle.core.sky.Body;
import idle.core.sky.Cosmos;
import idle.core.sky.SolarSystem;
import idle.core.upgrades.Automation;
import idle.core.upgrades.Automations;
import idle.core.upgrades.Effect;
import idle.core.upgrades.Upgrade;
import idle.core.upgrades.Upgrades;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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

    /**
     * Le tempo du jeu : ce par quoi la vitesse de départ des générateurs a été multipliée pour que
     * la première explosion vienne en trois heures au lieu de vingt-quatre. Ce qui se mesurait à
     * l'aune de la production (la croissance de la matière noire, le seuil du rendement de fusion)
     * se compte en production de départ : ces règles gardent ainsi l'allure qu'elles avaient.
     */
    public static final double TEMPO = 8;

    /** Vitesse de départ d'un générateur : deux particules par seconde. */
    public static final BigNum BASE_SPEED = BigNum.of(0.25 * TEMPO);
    /**
     * Le premier élan (version 0.95, par paliers depuis la 0.96) : dans la toute première partie,
     * avant la première explosion, la création prend son élan peu à peu. Elle va
     * {@code FIRST_RUN_PACE} fois moins vite jusqu'au premier atome (une heure environ), puis
     * {@link #FIRST_ATOMS_PACE} fois moins vite jusqu'au cinquième atome créé (deux heures), puis
     * {@link #FIRST_GAME_PACE} fois moins vite jusqu'au trentième, où s'ouvrent l'automatisation et le
     * tableau périodique : de là, elle a tout son élan. Rien ne ralentit plus ensuite, ni après une
     * explosion, ni dans l'univers suivant. Seule une partie neuve repasse par là.
     */
    public static final double FIRST_RUN_PACE = 13;
    public static final double FIRST_ATOMS_PACE = 9;
    public static final double FIRST_GAME_PACE = 2.8;
    /** Atomes créés où la création passe au palier suivant de son élan : 1, 5, puis 30. */
    public static final int FIRST_ATOMS_UNTIL = 5;
    /**
     * Les tests unitaires qui vérifient une règle sur une partie neuve, sans la regarder durer, coupent le
     * premier élan avec la propriété système {@code idle.sansPremierElan}, et avec lui la matière noire réduite
     * du premier cycle ({@link #FIRST_CYCLE_DARK_MATTER}) ; le jeu, les robots et les tests de rythme les gardent.
     */
    private static final boolean NO_FIRST_RUN_PACE = Boolean.getBoolean("idle.sansPremierElan");
    private double firstRunPace = NO_FIRST_RUN_PACE ? 1 : FIRST_RUN_PACE;

    /** Atomes gagnés à chaque fusion, avant les bonus. */
    public static final BigNum ATOMS_PER_FUSION = BigNum.ONE;

    /**
     * Nombre maximal d'atomes qu'on peut posséder en même temps : le nombre d'éléments du
     * tableau périodique. Arrivé là, il faut en dépenser pour pouvoir fusionner de nouveau.
     */
    public static final BigNum MAX_ATOMS = BigNum.of(118);

    /**
     * Particules à avoir en main pour le premier Big Bang : 10³⁸⁰⁰⁰ depuis la version 0.97 (10³²⁰⁰⁰ avant). C'est ce
     * qui fait durer le deuxième acte six heures en jeu parfait : à la fin de l'arbre, les parties qu'on laisse
     * tourner gagnent quelques milliers d'ordres de grandeur par heure, et les six cases du seuil du Big Bang
     * (de 10¹⁴⁰⁰⁰ à 10³⁴⁰⁰⁰) jalonnent la montée.
     */
    public static final BigNum BIG_BANG_PARTICLES = BigNum.of(1, 38000);

    /**
     * Particules des Big Bangs suivants, au troisième acte : 10³⁰⁰⁰, comme avant la 0.73. Chaque cycle
     * du troisième acte refait un deuxième acte en raccourci ; il n'a pas à redurer cinq heures.
     */
    public static final BigNum BIG_BANG_PARTICLES_LATER = BigNum.of(1, 3000);

    /** Les particules que demande le prochain Big Bang : beaucoup pour le premier, moins pour les suivants. */
    public BigNum bigBangParticles() {
        return state.bigBangs() == 0 ? BIG_BANG_PARTICLES : BIG_BANG_PARTICLES_LATER;
    }

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
     * Masse du tableau pendant les défis « Présence » et « Absence » (version 0.73) : un tableau très
     * lourd, qu'on ne remplit vite qu'avec les voies du bon style.
     */
    public static final BigNum STYLE_CHALLENGE_WEIGHT = BigNum.of(3, 6);

    /**
     * Masse du tableau pendant les huit premiers défis (version 0.97 ; 1 avant) : ils demandent quelques minutes
     * chacun au lieu de quelques secondes, et s'étalent sur la deuxième demi-heure de la matière noire.
     */
    public static final BigNum CHALLENGE_WEIGHT = BigNum.of(5000);

    /**
     * La matière noire de chaque explosion avant le premier Big Bang (version 0.97) : ×0,8. Le deuxième acte
     * s'étale ainsi vers six heures, au lieu de cinq.
     */
    public static final double FIRST_CYCLE_DARK_MATTER = 0.8;

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
    private final Map<Molecule.State, Long> gatheredByState = new java.util.EnumMap<>(Molecule.State.class);
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
            if (dark.requires() != null && dark.requiredFork() == null && !dark.isLink() && !darkUpgrades.containsKey(dark.requires())) {
                throw new IllegalArgumentException("Amélioration de matière noire sans sa case du dessus : " + dark.id());
            }
            if (dark.requiredFork() != null && darkCatalog.stream().noneMatch(other -> dark.requiredFork().equals(other.fork()))) {
                throw new IllegalArgumentException("Amélioration de matière noire après une bifurcation inconnue : " + dark.id());
            }
            for (String linked : dark.linkedIds()) {
                if (darkCatalog.stream().noneMatch(other -> other.id().equals(linked) && other.isFork())) {
                    throw new IllegalArgumentException("Liaison de matière noire vers une voie inconnue : " + dark.id() + " → " + linked);
                }
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

    /**
     * L'état de la partie vient d'être remplacé par celui d'une sauvegarde : tout ce que le jeu
     * avait calculé d'avance est oublié, et ce que la sauvegarde nomme sans que le jeu le
     * connaisse (une molécule disparue depuis) est écarté.
     *
     * @return le nombre d'identifiants inconnus écartés ; 0 pour une sauvegarde de cette version du jeu
     */
    public int restored() {
        int dropped = state.retainKnown(new GameState.Known(upgrades.keySet(), automations.keySet(), darkUpgrades.keySet(),
                darkAutomations.keySet(), challengeIds(), achievements.keySet(), molecules.keySet(), assemblies.keySet(),
                bodies.keySet(), spaceUpgrades.keySet(), PeriodicTable.ELEMENTS.size()));
        newAchievements.clear();
        newRecords.clear();
        // Une sauvegarde d'avant la 0.83 dont l'univers est formé : la remise à zéro de l'univers ne s'était pas faite.
        if (state.universeFormed() && state.cosmosLevel() >= Cosmos.values().length) resetForUniverse();
        forgetDerived();
        return dropped;
    }

    /** Oublie tout ce que le jeu avait calculé d'avance : l'état vient de changer d'un coup. */
    private void forgetDerived() {
        gatheredByStateVersion = -1;
        spaceByStateVersion = -1;
        occupiedSpaceVersion = -1;
        reservedSpaceVersion = -1;
        moleculeBonusesVersion = -1;
        achievementBonusesFor = -1;
        elementBonusesVersion = -1;
        newSpaceUpgrades.clear();
        cometAppeared = false;
        speedPrimePaid = false;
        kindCountsVersion = -1;
        sortsByStateVersion = -1;
        autoFormed.clear();
        autoGathered = 0;
        autoFormTimer = 0;
        shoppingVersion = Long.MIN_VALUE;
        shoppingSpaceKey = -1;
        nextAssemblyKey = Long.MIN_VALUE;
        finishedCollisions.clear();
        achievementTimer = 0;
        moleculeAutomationTimer = 0;
        moleculeAutomationTurn = 0;
    }

    private static java.util.Set<String> challengeIds() {
        java.util.Set<String> ids = new java.util.HashSet<>();
        for (Challenge challenge : Challenges.DEFAULT) ids.add(challenge.id());
        return ids;
    }

    // ------------------------------------------------------------------
    // L'absence
    // ------------------------------------------------------------------

    /** Part du temps d'absence qui compte au premier acte, avant la première explosion. */
    public static final double OFFLINE_RATE_PARTICLES = 0.10;
    /** Part du temps d'absence qui compte au deuxième acte, de la première explosion au premier Big Bang. */
    public static final double OFFLINE_RATE_DARK_MATTER = 0.40;
    /** Part du temps d'absence qui compte au troisième acte, du premier Big Bang à l'univers. */
    public static final double OFFLINE_RATE_BIG_BANG = 0.70;
    /** Part du temps d'absence qui compte une fois l'univers formé : tout. */
    public static final double OFFLINE_RATE_UNIVERSE = 1.0;

    /** Vrai pendant que {@link Absence} rejoue le temps passé jeu fermé. */
    private boolean away = false;

    /**
     * Fait avancer le jeu comme {@link #tick(double)}, mais pendant une absence rejouée : l'accord
     * d'absence (version 0.71) peut alors y tenter les défis ouverts.
     */
    public void tickAway(double dt) {
        away = true;
        try {
            tick(dt);
        } finally {
            away = false;
        }
    }

    /**
     * En absence (version 0.73) : une bifurcation ouverte où le joueur n'a pas choisi prend sa voie
     * d'absence, si elle est payable. Qui ne choisit pas joue le style de l'absence.
     */
    private void takeAbsenceVoies() {
        if (inChallenge() || !isDarkMatterUnlocked()) return;
        for (DarkUpgrade dark : darkUpgrades.values()) {
            if (!dark.isFork() || darkStyle(dark) != Style.ABSENCE || state.darkLevelOf(dark.id()) > 0) continue;
            if (darkForkRival(dark.id()) == null && canBuyDark(dark.id())) {
                buyDark(dark.id());
                awayVoies.add(dark.name());
            }
        }
    }

    /**
     * Les sondes en absence (version 0.86) : une sonde arrivée vide son butin et repart pour le même
     * voyage, comme le ferait le joueur. Joueur présent, rien ne change : le butin attend son clic,
     * et les sondes restent un à-côté, jamais une obligation.
     */
    private void runProbesWhileAway() {
        if (!isProbesUnlocked() || state.probes().isEmpty()) return;
        for (Probe probe : new ArrayList<>(state.probes())) {
            if (!probe.landed() || probe.id() < 0) continue;
            claimAllProbeRewards(probe.id());
            if (launchProbe(probe.destination(), probe.trip())) awayProbeTrips++;
        }
    }

    private int awayProbeTrips = 0;

    /** Les voyages de sonde que l'absence rejouée a relancés seule, puis les oublie. */
    public int takeAwayProbeTrips() {
        int trips = awayProbeTrips;
        awayProbeTrips = 0;
        return trips;
    }

    /** Les voies prises seules pendant l'absence en cours de rejeu, et les défis qu'elle a tentés et réussis. */
    private final List<String> awayVoies = new ArrayList<>();
    private final List<String> awayChallengesStarted = new ArrayList<>();

    /** Ce que l'absence rejouée a fait à la place du joueur (version 0.73), puis l'oublie. */
    public List<String> takeAwayVoies() {
        List<String> taken = new ArrayList<>(awayVoies);
        awayVoies.clear();
        return taken;
    }

    /** Les défis que l'accord d'absence a lancés pendant l'absence rejouée, puis les oublie. */
    public List<String> takeAwayChallenges() {
        List<String> started = new ArrayList<>(awayChallengesStarted);
        awayChallengesStarted.clear();
        return started;
    }

    /** Une partie qui commence, en absence, avec l'accord d'absence : elle devient le prochain défi à réussir. */
    private void startChallengeWhileAway() {
        if (inChallenge() || !hasDark(DarkEffect.ChallengesWhileAway.class) || state.stats().runTime() > 1) return;
        for (Challenge challenge : Challenges.DEFAULT) {
            if (isChallengeUnlocked(challenge.id()) && !isChallengeCompleted(challenge.id())) {
                if (startChallenge(challenge.id())) awayChallengesStarted.add(challenge.name());
                return;
            }
        }
    }

    /**
     * Part du temps passé jeu fermé qui est rejouée au retour ({@link Absence}) : un dixième au
     * début, et de plus en plus à mesure que les actes passent, jusqu'à tout le temps une fois
     * l'univers formé. Le jeu fermé avance donc toujours moins vite que le jeu ouvert, sauf à la fin.
     */
    public double offlineRate() {
        if (hasCosmos(Cosmos.UNIVERSE)) return OFFLINE_RATE_UNIVERSE;
        // Les voies d'absence de l'arbre (version 0.70) en rendent une part de plus, jusqu'à tout le temps.
        if (state.bigBangs() > 0) return Math.min(1, OFFLINE_RATE_BIG_BANG + darkOfflineBonus());
        if (isDarkMatterUnlocked()) return Math.min(1, OFFLINE_RATE_DARK_MATTER + darkOfflineBonus());
        return OFFLINE_RATE_PARTICLES;
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
     * <p>Sert aussi au retour après une absence ({@link Absence}), qui rejoue par morceaux
     * une part du temps passé jeu fermé. Un seul gros tick donne le même résultat que
     * beaucoup de petits, à la seconde près quand un bonus dépend du temps.
     */
    public void tick(double dt) {
        if (dt < 0 || Double.isNaN(dt) || Double.isInfinite(dt)) {
            throw new IllegalArgumentException("Durée invalide : " + dt);
        }
        if (away) {
            takeAbsenceVoies();
            startChallengeWhileAway();
            runProbesWhileAway();
        }
        // La matière noire qui grossit seule : une petite part de ce que donnerait un appui.
        double autoShare = darkAutoExpansionShare();
        if (autoShare > 0) expandDarkMatter(dt * autoShare);
        // L'appui verrouillé : elle grossit comme si le joueur tenait le clic, quel que soit l'onglet affiché.
        if (isHoldLocked()) growDarkMatter(dt * HOLD_LOCK_SHARE);
        // L'appui automatique, acquis avec l'espace : il tient le clic en entier, ou ce que le verrou laisse.
        if (isAutoHolding()) growDarkMatter(dt * (isHoldLocked() ? 1 - HOLD_LOCK_SHARE : 1));

        expandSpace(dt);
        takeSpaceUpgrades();
        runComet(dt);
        refillPulses(dt);
        runCollider(dt);
        runProbes(dt);
        runMaturation(dt);
        runMoleculeAutomation(dt);
        runAutoForm(dt);
        runPlans(dt);
        runSecondUniverse(dt);
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
        // Quelques valeurs dépendent de l'historique : chacun retient ses propres sommets.
        if (whole) stats.history().add(new StatsHistory.Sample(state.timePlayed(), withPeaks(values, stats.history()), delays));
        if (run) stats.runHistory().add(new StatsHistory.Sample(stats.runTime(), withPeaks(values, stats.runHistory()), delays));
    }

    /**
     * Les relevés d'un historique, suivis d'un relevé de l'instant (version 0.64). Les relevés
     * s'espacent avec le temps de jeu (plusieurs minutes après quelques heures) : sans ce dernier
     * point, une courbe toute neuve, ou une statistique apparue depuis le dernier relevé, resterait
     * vide jusqu'au suivant. Le relevé de l'instant n'est pas gardé.
     *
     * @param run vrai pour l'historique de la partie en cours, faux pour celui de tout le jeu
     */
    public List<StatsHistory.Sample> samplesWithNow(boolean run) {
        GameStats stats = state.stats();
        StatsHistory history = run ? stats.runHistory() : stats.history();
        List<StatsHistory.Sample> samples = new ArrayList<>(history.samples());
        if (!state.started()) return samples;
        double time = run ? stats.runTime() : state.timePlayed();
        if (!samples.isEmpty() && samples.get(samples.size() - 1).time() >= time) return samples;
        Map<String, Double> delays = new java.util.HashMap<>();
        for (Automation automation : automations.values()) {
            if (ownsAutomation(automation.id())) delays.put(automation.id(), automationInterval(automation.id()));
        }
        samples.add(new StatsHistory.Sample(time, withPeaks(measure(), history), delays));
        return samples;
    }

    /** Ces mesures, où chaque statistique qui retient un sommet ({@link StatsHistory.Stat#peaks()}) prend celui de cet historique. */
    private static double[] withPeaks(double[] values, StatsHistory history) {
        double[] copy = values.clone();
        for (StatsHistory.Stat stat : StatsHistory.Stat.values()) {
            if (stat.peaks()) copy[stat.ordinal()] = history.peak(stat, values[stat.ordinal()]);
        }
        return copy;
    }

    /**
     * Ce que le joueur a en main va retomber (fusion, explosion, défi, Big Bang) : les historiques
     * retiennent le sommet que chaque quantité vient d'atteindre.
     */
    private void noteHeldPeaks() {
        double particles = power(state.particles());
        double atoms = power(state.atoms());
        double darkMatter = power(darkMatterEarned());
        double copies = ownedCopies();
        for (StatsHistory history : List.of(state.stats().history(), state.stats().runHistory())) {
            history.notePeak(StatsHistory.Stat.PARTICLES, particles);
            history.notePeak(StatsHistory.Stat.ATOMS, atoms);
            history.notePeak(StatsHistory.Stat.DARK_MATTER, darkMatter);
            history.notePeak(StatsHistory.Stat.ELEMENT_COPIES, copies);
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
                case SKY -> assembliesFormed() + bodiesFormed() + state.cosmosLevel();
                case BIG_BANGS -> state.bigBangs();
                case SPACE_RATE -> isBigBangUnlocked() ? power(spacePerSecond()) : Double.NaN;
                case DARK_SPACE -> darkMatterSpaceBoost();
                case MOLECULE_CREATIONS -> state.stats().moleculeCreations();
                case AUTO_MOLECULE_CREATIONS -> state.stats().autoMoleculeCreations();
                case SUBSTANCES -> substancesFormed();
                case COLLIDER_ENERGY -> isColliderUnlocked() ? power(BigNum.of(state.colliderEnergy())) : Double.NaN;
                case COLLIDER_ENERGY_PRODUCED -> isColliderUnlocked() ? power(BigNum.of(state.colliderEnergyProduced())) : Double.NaN;
                case COLLIDER_ENERGY_RATE -> isColliderUnlocked() ? power(BigNum.of(colliderEnergyPerSecond())) : Double.NaN;
                case COLLISIONS -> isCollisionsUnlocked() ? state.collisionRuns() : Double.NaN;
                case QUARKS -> isCollisionsUnlocked() ? totalQuarks() : Double.NaN;
                case PARTICLES -> power(state.particles());
                case ATOMS -> power(state.atoms());
                case ELEMENT_COPIES -> ownedCopies();
                case DARK_MATTER -> power(darkMatterEarned());
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

    /**
     * Vrai tant que le joueur a coupé d'un geste tous les automatismes : ceux de l'onglet
     * Automatisation, ceux de matière noire, l'appui automatique et la création automatique.
     * Aucun n'agit, mais chacun garde son propre réglage et reprend quand la coupure est levée.
     * Le verrou de l'appui n'est pas un automatisme : il continue.
     */
    public boolean isAutomationPaused() {
        return state.automationPaused();
    }

    /** Coupe ou relance d'un geste tous les automatismes. Aucune remise à zéro ne change ce réglage. */
    public void setAutomationPaused(boolean paused) {
        state.setAutomationPaused(paused);
    }

    /** Vrai si le joueur a au moins un automatisme, de quelque sorte que ce soit : il y a alors quelque chose à couper. */
    public boolean hasAnyAutomation() {
        if (!state.ownedAutomations().isEmpty() || isAutoHoldUnlocked() || isMoleculeAutomationUnlocked()
                || isAutoGatherUnlocked() || isAutoFormUnlocked()) return true;
        for (DarkAutomation automation : darkAutomations.values()) {
            if (isDarkAutomationUnlocked(automation.id())) return true;
        }
        return false;
    }

    private boolean hasActiveAutomation() {
        if (state.automationPaused()) return false;
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
        return darkMemo("minInterval", () -> {
            double min = MIN_AUTOMATION_INTERVAL;
            for (DarkUpgrade dark : darkUpgrades.values()) {
                if (dark.effect() instanceof DarkEffect.FasterAutomations faster && effectLevel(dark) > 0) {
                    min = Math.min(min, faster.minInterval());
                }
            }
            return min;
        });
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
            case UPGRADE -> bulkBuy(automation);
            case FUSION -> generatorCount() >= fusionThreshold() && fuse();
            // La réserve du joueur : la synthèse automatique n'entame pas les atomes mis de côté.
            case SYNTHESIS -> state.atoms().gte(synthesisCost().add(state.synthesisReserve())) && paySynthesis();
        };
    }

    /**
     * L'achat d'un automatisme : un niveau par action, sauf celui des générateurs, qui prend d'un
     * coup tous ceux que les particules permettent. Une fois les automatismes rapides, c'est leur
     * cadence et non la production qui fixait la durée d'une partie : neuf achats de générateurs,
     * un par action, avant chaque fusion. Pris d'un coup, la fusion revient au rythme d'une action.
     */
    private boolean bulkBuy(Automation automation) {
        if (!buysInBulk(automation)) return buy(automation.upgradeId());
        Upgrade upgrade = upgrades.get(automation.upgradeId());
        if (upgrade.resource() != Resource.PARTICLES || !state.started()) {
            boolean any = false;
            for (int bought = 0; bought < MAX_GENERATORS_PER_ACTION && buy(upgrade.id()); bought++) any = true;
            return any;
        }
        // Le même résultat qu'autant d'achats un à un, sans refaire chaque fois tous les calculs :
        // la réduction des générateurs ne dépend pas de leur nombre, elle se calcule une fois. En fin de
        // partie, une action achète des dizaines de générateurs, vingt fois par seconde de jeu.
        int room = Math.min(MAX_GENERATORS_PER_ACTION, maxGeneratorCount() - generatorCount());
        double factor = generatorCostFactor();
        int level = state.levelOf(upgrade.id());
        BigNum particles = state.particles();
        int bought = 0;
        // Le prix brut du suivant, multiplié par la croissance à chaque achat plutôt que recalculé par une puissance.
        BigNum growth = BigNum.of(upgrade.costGrowth());
        BigNum raw = room > 0 ? upgrade.baseCost().multiply(growth.pow(level)) : BigNum.ZERO;
        while (bought < room) {
            BigNum cost = Upgrade.roundUp(raw);
            raw = raw.multiply(growth);
            if (factor != 1) cost = Upgrade.roundUp(cost.multiply(factor)).max(BigNum.ONE);
            if (particles.lt(cost)) break;
            particles = particles.subtract(cost).max(BigNum.ZERO);
            state.stats().noteParticleUpgrade(cost, true);
            bought++;
        }
        if (bought == 0) return false;
        state.setParticles(particles);
        state.setLevel(upgrade.id(), level + bought);
        return true;
    }

    /** Vrai pour l'automatisme qui achète les générateurs : une action prend tous ceux que les particules permettent. */
    public boolean buysInBulk(Automation automation) {
        return automation.kind() == Automation.Kind.UPGRADE
                && upgrades.get(automation.upgradeId()).effect() instanceof Effect.AddGenerator;
    }

    /** Garde-fou : le plus grand nombre de générateurs que leur automatisme achète en une action. */
    private static final int MAX_GENERATORS_PER_ACTION = 1_000;

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
     * fois au plus. Un défi se joue sur un tableau de masse {@link #CHALLENGE_WEIGHT} (1 avant la version 0.97).
     */
    public BigNum tableWeight() {
        if (ruled(Challenge.Rule.PRESENCE_ONLY) || ruled(Challenge.Rule.ABSENCE_ONLY)) return STYLE_CHALLENGE_WEIGHT;
        return inChallenge() ? CHALLENGE_WEIGHT
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
                * (1 + achievementBonus(Achievement.Bonus.SYNTHESIS_COST))
                / (cycleChoice() == idle.core.universe.CycleChoice.SYNTHESIS ? 1 - idle.core.universe.CycleChoice.SYNTHESIS_SAVING : 1);
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
        int known = discoveredElements();
        // Le garde-fou du hasard : après trop de synthèses sans rien de nouveau, celle-ci donne un élément inconnu.
        boolean rescue = target == null && !guaranteed && LUCK_FLOOR > 0 && state.drySyntheses() >= LUCK_FLOOR;
        obtained.add(target != null ? drawTargeted(target) : rescue ? drawUnknown() : drawElement(guaranteed));
        // La préférence graduée (version 0.87) : chaque tirage au hasard la renforce un peu.
        if (target == null && preferredFamily() != null) state.setPreferenceDraws(state.preferenceDraws() + 1);
        // Les éléments en plus de la matière noire, tant qu'il en reste à obtenir.
        for (int extra = elementsPerSynthesis() - 1; extra > 0 && !isPeriodicTableComplete(); extra--) {
            obtained.add(drawElement(false));
        }
        boolean doubleDraw = random.nextDouble() < doubleChance && !isPeriodicTableComplete();
        if (doubleDraw) obtained.add(drawElement(false));
        state.stats().noteSynthesis(obtained.size(), doubleDraw);
        // Rien de nouveau si le tableau compte autant d'éléments connus qu'avant le tirage.
        state.setDrySyntheses(discoveredElements() > known ? 0 : state.drySyntheses() + 1);
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

    /** Vrai une fois acquis l'achat groupé des améliorations payées en atomes : par dix, ou tout ce qui est à portée. */
    public boolean isBulkAtomBuyUnlocked() {
        if (hasDark(DarkEffect.BulkAtomUpgrades.class)) return true;
        for (Upgrade upgrade : upgrades.values()) {
            if (upgrade.effect() instanceof Effect.BulkAtomBuy && state.levelOf(upgrade.id()) > 0) return true;
        }
        return false;
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
     * Nombre de synthèses de suite sans élément nouveau au bout duquel la suivante en donne un à
     * coup sûr, tant qu'il en reste à découvrir. C'est ce qui borne la malchance : le dernier
     * élément du tableau ne se fait plus attendre des dizaines de synthèses.
     */
    public static final int LUCK_FLOOR = 2;

    /**
     * Vrai si la prochaine synthèse ordinaire donnera à coup sûr un élément que le joueur n'a pas :
     * les {@link #LUCK_FLOOR} dernières n'ont rien apporté de nouveau, aucune famille n'est visée
     * et il reste des éléments à découvrir.
     */
    public boolean isNextSynthesisNew() {
        return LUCK_FLOOR > 0 && state.drySyntheses() >= LUCK_FLOOR && isPeriodicTableUnlocked()
                && synthesisTarget() == null && discoveredElements() < PeriodicTable.ELEMENTS.size();
    }

    /**
     * Tire un élément que le joueur n'a pas encore, avec les mêmes poids de familles que le tirage
     * ordinaire, parmi les familles qui en ont. S'il n'en reste aucun à découvrir, c'est un tirage ordinaire.
     */
    private Element drawUnknown() {
        double total = 0;
        Map<ElementCategory, List<Element>> unknown = new java.util.EnumMap<>(ElementCategory.class);
        for (ElementCategory category : ElementCategory.values()) {
            if (drawWeight(category) <= 0) continue;
            List<Element> pool = availableElements(category).stream().filter(element -> state.elementCount(element.number()) == 0).toList();
            if (pool.isEmpty()) continue;
            unknown.put(category, pool);
            total += drawWeight(category);
        }
        if (unknown.isEmpty()) return drawElement(false);
        double roll = random.nextDouble() * total;
        List<Element> chosen = null;
        for (Map.Entry<ElementCategory, List<Element>> entry : unknown.entrySet()) {
            chosen = entry.getValue();
            roll -= drawWeight(entry.getKey());
            if (roll < 0) break;
        }
        Element element = chosen.get(random.nextInt(chosen.size()));
        state.setElementCount(element.number(), 1);
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
        return category.chance() * (category.rare() ? bonuses().luckMultiplier() : 1) * preferenceMultiplier(category);
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
        // La prime de vitesse : la première explosion depuis le Big Bang pose le record, les suivantes le battent de la marge ou non.
        boolean primed = earnsSpeedPrime();
        double duration = state.stats().runTime();
        if (!replay && (primed || state.explosionRecord() <= 0)) state.setExplosionRecord(duration);
        if (primed) {
            speedPrimePaid = true;
            state.stats().noteSpeedPrime();
        }
        boolean firstExplosion = state.stats().timedExplosions() == 0;
        // Un défi en cours est réussi : il laisse sa récompense, et son meilleur temps est noté.
        Challenge challenge = activeChallenge();
        if (challenge != null) {
            double time = state.stats().runTime();
            Double best = state.challengeTimes().get(challenge.id());
            if (best == null || time < best) state.setChallengeTime(challenge.id(), time);
            state.addCompletedChallenge(challenge.id());
            state.setActiveChallenge(null);
        }
        noteHeldPeaks();
        state.stats().noteProduction(productionAtFusion());
        // Une explosion rapide ne compte qu'à partir de la deuxième : la première n'a pas de précédente.
        if (state.explosions() >= 1 && state.stats().runTime() < Achievements.FAST_EXPLOSION_SECONDS) {
            award(Achievements.FAST_EXPLOSION);
        }
        state.setDarkMatter(state.darkMatter().add(reward));
        state.setExplosions(state.explosions() + 1);
        // Un défi rejoué ne compte que pour son temps : ni matière noire, ni tableau plus lourd.
        if (!replay) state.setTableWeightLevel(state.tableWeightLevel() + 1);
        // L'appui verrouillé est un réglage du joueur : l'explosion ne le défait pas.
        clearRun();
        state.stats().endRun();
        if (firstExplosion) noteStep(GameStats.Step.FIRST_EXPLOSION);
        // La contraction douce (version 0.88) : l'explosion la réussit.
        if (inTrial(idle.core.universe.Trial.SOFT_CONTRACTION)) completeTrial();
        // Demandé par le joueur (version 0.84) : les bifurcations se rouvrent avec cette explosion.
        if (state.forkResetArmed()) {
            if (!hasSuperposition()) reopenForks();
            state.setForkResetArmed(false);
        }
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
        noteHeldPeaks();
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
        if (rewardsVersion != state.challengesVersion()) {
            rewards.clear();
            for (Challenge challenge : Challenges.DEFAULT) {
                if (state.completedChallenges().contains(challenge.id())) rewards.add(challenge.reward());
            }
            rewardsVersion = state.challengesVersion();
        }
        return rewards.contains(reward);
    }

    /** Les récompenses des défis réussis, recalculées seulement quand la liste des défis change. */
    private final java.util.EnumSet<Challenge.Reward> rewards = java.util.EnumSet.noneOf(Challenge.Reward.class);
    private int rewardsVersion = -1;

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
        // Lu à chaque instant par plusieurs bonus : le compte n'est refait que si les succès ont changé.
        if (achievementCountVersion != state.achievementsVersion()) {
            int count = 0;
            for (String id : state.achievements()) {
                if (achievements.containsKey(id)) count++;
            }
            achievementCountCache = count;
            achievementCountVersion = state.achievementsVersion();
        }
        return achievementCountCache;
    }

    private int achievementCountCache = 0;
    private int achievementCountVersion = -1;

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
     * Verrouille ou libère l'appui. C'est un réglage : ni l'explosion ni le Big Bang ne le changent,
     * et il ressert dès que la case « Verrou » de l'arbre est de nouveau acquise.
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
        return isAutoHoldUnlocked() && state.autoHold() && isDarkMatterUnlocked() && !state.automationPaused()
                && !bangRuled(BangChallenge.BY_HAND);
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
        if (isChallengeReplay()) return BigNum.ZERO;
        return earnsSpeedPrime() ? darkMatterPerExplosion().add(speedPrime()) : darkMatterPerExplosion();
    }

    /**
     * Part du record qu'il faut battre pour toucher la prime de vitesse : exploser en moins de
     * 90 % du temps de la dernière explosion primée. Sans cette marge, une explosion automatique
     * qui gagne une fraction de seconde à chaque fois toucherait la prime sans fin.
     */
    public static final double SPEED_PRIME_MARGIN = 0.9;
    /** La prime de vitesse, avant les paliers de Big Bang : une matière noire de plus. */
    public static final BigNum SPEED_PRIME = BigNum.ONE;
    private boolean speedPrimePaid = false;

    /**
     * Le temps à battre pour la prime de vitesse, en secondes : {@link #SPEED_PRIME_MARGIN} du
     * record ({@link #explosionRecord()}). 0 tant qu'aucune explosion n'a eu lieu depuis le dernier
     * Big Bang : la première pose le record, sans prime.
     */
    public double speedPrimeTime() {
        return state.explosionRecord() * SPEED_PRIME_MARGIN;
    }

    /** Durée de l'explosion qui sert de record depuis le dernier Big Bang, en secondes ; 0 s'il n'y en a pas encore. */
    public double explosionRecord() {
        return state.explosionRecord();
    }

    /** Vrai si une explosion déclenchée maintenant toucherait la prime de vitesse : le record est battu de la marge voulue, hors défi rejoué. */
    public boolean earnsSpeedPrime() {
        return !isChallengeReplay() && state.explosionRecord() > 0 && state.stats().runTime() <= speedPrimeTime();
    }

    /** Secondes qu'il reste pour toucher la prime de vitesse ; 0 quand il est trop tard ou qu'il n'y a pas de record. */
    public double speedPrimeLeft() {
        return state.explosionRecord() > 0 ? Math.max(0, speedPrimeTime() - state.stats().runTime()) : 0;
    }

    /** Ce que vaut la prime de vitesse : {@link #SPEED_PRIME}, multipliée comme la matière noire d'une explosion. */
    public BigNum speedPrime() {
        return SPEED_PRIME.multiply(bigBangDarkMatterFactor() * bangReward(BangChallenge.Reward.DARK_MATTER));
    }

    /** Vrai si la dernière explosion a touché la prime de vitesse : pour l'annoncer. Remis à faux à chaque lecture. */
    public boolean takeSpeedPrime() {
        boolean paid = speedPrimePaid;
        speedPrimePaid = false;
        return paid;
    }

    /**
     * Matière noire que laisse une explosion : {@link #DARK_MATTER_PER_EXPLOSION}, plus ce qu'ajoute
     * l'arbre, le tout multiplié par les paliers de Big Bang ({@link #bigBangDarkMatterFactor()}).
     */
    public BigNum darkMatterPerExplosion() {
        BigNum amount = DARK_MATTER_PER_EXPLOSION;
        for (DarkUpgrade dark : darkUpgrades.values()) {
            if (dark.effect() instanceof DarkEffect.AddDarkMatterPerExplosion add) {
                amount = amount.add(BigNum.of(add.perLevel() * effectLevel(dark)));
            }
        }
        if (rewarded(Challenge.Reward.EXTRA_DARK_MATTER)) amount = amount.add(BigNum.ONE);
        double factor = (state.bigBangs() == 0 && firstRunPace > 1 ? FIRST_CYCLE_DARK_MATTER : 1) * bigBangDarkMatterFactor() * bangReward(BangChallenge.Reward.DARK_MATTER)
                * (cycleChoice() == idle.core.universe.CycleChoice.RESERVE ? 1 + idle.core.universe.CycleChoice.RESERVE_GAIN : 1);
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
     * {@code 1 + ordres de grandeur de la production + ordres de grandeur des atomes créés}. La
     * production se compte à partir de {@link #TEMPO} particules par seconde, celle d'une partie
     * qui commence. Avec 8 × 10¹² particules par seconde et 1 000 atomes créés depuis la dernière
     * explosion, le facteur vaut 1 + 12 + 3 = 16. L'élan de la matière noire suit ainsi le rythme des particules
     * et des atomes : faible au début d'une partie, de plus en plus fort ensuite.
     *
     * <p>La production comptée est celle de tous les générateurs réunis
     * ({@link #productionAtFusion()}), pour ne pas retomber à chaque fusion.
     */
    public double darkMatterProgressFactor() {
        double production = Math.max(0, productionAtFusion().divide(TEMPO).log10());
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
                * moleculeBoost(Molecule.Stat.DARK_GROWTH)
                * colliderDarkGrowthMultiplier()
                * bangAchievementMultiplier(BangAchievement.Reward.DARK_GROWTH);
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
            case PARTICLES -> darkBalance(DarkUpgrade.Branch.PARTICLES).gte(bigBangParticles());
            case ATOMS -> state.atoms().gte(BIG_BANG_ATOMS);
            case DARK_MATTER -> state.darkMatter().gte(BIG_BANG_DARK_MATTER);
            case ACHIEVEMENTS -> achievementCount() >= BIG_BANG_ACHIEVEMENTS;
            case CHALLENGES -> !challenges().isEmpty() && completedChallenges() >= challenges().size();
            case SKY -> bodiesMature() >= bigBangSkyRequirement();
        };
    }

    /** Astres mûrs que demandent les Big Bangs six, sept et huit : le ciel doit avancer avant les derniers paliers. */
    public static final int[] BIG_BANG_SKY = {18, 24, 28};

    /** Astres mûrs que demande le prochain Big Bang : rien avant le sixième ni après le huitième. */
    public int bigBangSkyRequirement() {
        int next = state.bigBangs() + 1;
        return next >= 6 && next <= 5 + BIG_BANG_SKY.length ? BIG_BANG_SKY[next - 6] : 0;
    }

    /** Les conditions du prochain Big Bang : les cinq de toujours, et le ciel pour les Big Bangs six à huit. */
    public List<BigBangCondition> bigBangConditions() {
        List<BigBangCondition> all = new ArrayList<>(List.of(BigBangCondition.values()));
        if (bigBangSkyRequirement() == 0) all.remove(BigBangCondition.SKY);
        return all;
    }

    /** Nombre de conditions du Big Bang remplies en ce moment, sur les cinq. */
    public int bigBangConditionsMet() {
        int met = 0;
        for (BigBangCondition condition : bigBangConditions()) {
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
                && bigBangConditionsMet() == bigBangConditions().size();
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
     * de matière noire reste aussi, avec ses automatismes : seuls partent la réserve, la taille et
     * la masse du tableau. Les défis réussis, eux, restent dès un palier plus tôt
     * ({@link #bigBangKeepsChallenges()}).
     *
     * @return {@code true} si le Big Bang a eu lieu
     */
    public boolean bigBang() {
        if (!canBigBang()) return false;
        // Le Big Bang à froid (version 0.88) : le Big Bang le réussit.
        if (inTrial(idle.core.universe.Trial.COLD_BANG)) completeTrial();
        noteHeldPeaks();
        state.stats().noteProduction(productionAtFusion());
        // Un défi de Big Bang en cours est réussi : sa récompense est acquise, son temps noté.
        BangChallenge challenge = activeBangChallenge();
        if (challenge != null) {
            double time = bangChallengeTime();
            Double best = state.bangChallengeTimes().get(challenge.name());
            if (best == null || time < best) state.setBangChallengeTime(challenge.name(), time);
            state.addCompletedBangChallenge(challenge.name());
            state.setActiveBangChallenge(null);
        }
        boolean first = !state.stats().reached(GameStats.Step.FIRST_BIG_BANG);
        state.stats().noteBigBang(state.explosions(), state.timePlayed());
        if (first) noteRecord(GameStats.Step.FIRST_BIG_BANG);
        clearForBigBang();
        state.setBigBangs(state.bigBangs() + 1);
        state.stats().restartRun();
        return true;
    }

    /**
     * Rouvre les bifurcations : leurs voies et les liaisons qui en dépendent repartent de zéro, et la
     * matière noire des liaisons cesse d'être comptée comme dépensée. Ce que fait chaque Big Bang ; et
     * l'explosion qui suit « Rouvrir les bifurcations » (version 0.84).
     */
    private void reopenForks() {
        for (DarkUpgrade dark : darkUpgrades.values()) {
            if (dark.isLink()) {
                BigNum paid = BigNum.ZERO;
                for (int level = 0; level < state.darkLevelOf(dark.id()); level++) paid = paid.add(dark.costAt(level));
                state.setDarkMatterSpent(state.darkMatterSpent().subtract(paid).max(BigNum.ZERO));
            }
            if (dark.isFork() || dark.isLink()) state.setDarkLevel(dark.id(), 0);
        }
    }

    /** Efface ce qu'un Big Bang efface, en gardant ce que les paliers atteints (ou celui qu'il atteint) font garder. */
    private void clearForBigBang() {
        // Le petit arbre du deuxième univers (version 0.87) : le Big Bang rend le choix, gratuitement.
        state.setCycleChoice(null);
        boolean keepsTree = bigBangKeepsDarkTree();
        java.util.Set<String> keptChallenges = bigBangKeepsChallenges()
                ? new java.util.LinkedHashSet<>(state.completedChallenges()) : java.util.Set.of();
        state.clearMatter();
        if (keepsTree) {
            state.clearDarkMatterKeepingTree();
            // Les bifurcations se rouvrent à chaque Big Bang, même quand l'arbre reste : le joueur peut changer de voie.
            // Les liaisons, qui en dépendent, repartent avec elles ; la matière noire qu'elles avaient coûté
            // cesse d'être comptée comme dépensée. Avec la Superposition (0.70), il n'y a plus de choix à refaire :
            // voies et liaisons restent.
            if (!hasSuperposition()) reopenForks();
        } else {
            state.clearDarkMatter();
        }
        for (String kept : keptChallenges) state.addCompletedChallenge(kept);
        // Le record d'explosion repart : après un Big Bang, la première explosion redemande des heures.
        state.setExplosionRecord(0);
    }

    // ------------------------------------------------------------------
    // Les défis de Big Bang
    // ------------------------------------------------------------------

    /** Les défis de Big Bang, dans l'ordre du catalogue. */
    public List<BangChallenge> bangChallenges() {
        return List.of(BangChallenge.values());
    }

    /** Big Bangs qui ouvrent les défis de Big Bang : le cinquième, comme avant les paliers six à huit. */
    public static final int BANG_CHALLENGES_BIG_BANGS = 5;

    /** Vrai à partir du cinquième Big Bang : les défis de Big Bang s'ouvrent, pour donner un sens aux Big Bangs suivants. */
    public boolean isBangChallengesUnlocked() {
        return state.bigBangs() >= BANG_CHALLENGES_BIG_BANGS;
    }

    /** Le défi de Big Bang en cours, ou {@code null}. */
    public BangChallenge activeBangChallenge() {
        String name = state.activeBangChallenge();
        if (name == null) return null;
        for (BangChallenge challenge : BangChallenge.values()) {
            if (challenge.name().equals(name)) return challenge;
        }
        return null;
    }

    /** Vrai si ce défi de Big Bang a été réussi : sa récompense agit. */
    public boolean isBangChallengeCompleted(BangChallenge challenge) {
        return state.completedBangChallenges().contains(challenge.name());
    }

    /** Nombre de défis de Big Bang réussis. */
    public int completedBangChallenges() {
        int done = 0;
        for (BangChallenge challenge : BangChallenge.values()) {
            if (isBangChallengeCompleted(challenge)) done++;
        }
        return done;
    }

    /** Le meilleur temps d'un défi de Big Bang réussi, en secondes, ou {@code NaN} s'il ne l'a jamais été. */
    public double bangChallengeBest(BangChallenge challenge) {
        return state.bangChallengeTimes().getOrDefault(challenge.name(), Double.NaN);
    }

    /** Temps de jeu écoulé depuis le début du défi de Big Bang en cours, en secondes ; 0 hors défi. */
    public double bangChallengeTime() {
        return state.activeBangChallenge() == null ? 0 : Math.max(0, state.timePlayed() - state.bangChallengeStarted());
    }

    /** Vrai si un défi de Big Bang peut commencer : ils sont ouverts, aucun n'est en cours, et aucun défi d'explosion non plus. */
    public boolean canStartBangChallenge() {
        return state.started() && isBangChallengesUnlocked() && state.activeBangChallenge() == null && !inChallenge();
    }

    /**
     * Commence un défi de Big Bang : la partie repart comme après un Big Bang (mêmes choses
     * gardées), sans en compter un, et la contrainte tient jusqu'au Big Bang suivant.
     *
     * @return {@code true} si le défi a commencé
     */
    public boolean startBangChallenge(BangChallenge challenge) {
        if (challenge == null || !canStartBangChallenge()) return false;
        noteHeldPeaks();
        clearForBigBang();
        state.stats().restartRun();
        state.setActiveBangChallenge(challenge.name());
        state.setBangChallengeStarted(state.timePlayed());
        return true;
    }

    /** Abandonne le défi de Big Bang en cours : la contrainte tombe, la partie continue, rien n'est gagné. */
    public boolean abandonBangChallenge() {
        if (state.activeBangChallenge() == null) return false;
        state.setActiveBangChallenge(null);
        return true;
    }

    /** Vrai si cette contrainte de défi de Big Bang s'applique en ce moment. */
    private boolean bangRuled(BangChallenge challenge) {
        return challenge.name().equals(state.activeBangChallenge());
    }

    /** Ce que les défis de Big Bang réussis multiplient pour cette récompense : 1 sans défi réussi. */
    public double bangReward(BangChallenge.Reward reward) {
        double factor = 1;
        for (BangChallenge challenge : BangChallenge.values()) {
            if (challenge.reward() == reward && isBangChallengeCompleted(challenge)) factor *= BANG_REWARDS.get(reward);
        }
        return factor;
    }

    /** Ce que vaut chaque récompense de défi de Big Bang. */
    public static final Map<BangChallenge.Reward, Double> BANG_REWARDS = Map.of(
            BangChallenge.Reward.SPACE, 2.0, BangChallenge.Reward.ACCRETION, 1.5, BangChallenge.Reward.DARK_MATTER, 2.0);

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
        if (bangRuled(BangChallenge.FORGOTTEN)) return 1;
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
        if (bangRuled(BangChallenge.FORGOTTEN)) return 1;
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
     * Vrai si le prochain Big Bang laissera les défis réussis ({@link BigBangMilestone.KeepChallenges}) :
     * le palier qui le donne est atteint, ou ce Big Bang l'atteindra. Ce Big Bang-là en profite donc déjà.
     */
    public boolean bigBangKeepsChallenges() {
        for (BigBangMilestone milestone : BigBangMilestones.DEFAULT) {
            if (milestone.bigBangs() > state.bigBangs() + 1) continue;
            for (BigBangMilestone.Effect effect : milestone.effects()) {
                if (effect instanceof BigBangMilestone.KeepChallenges) return true;
            }
        }
        return false;
    }

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
        return isMoleculeAutomationUnlocked() && state.autoMolecules()
                && (!state.automatedMolecules().isEmpty() || state.autoFollowList() || state.autoFollowMatter())
                && !state.automationPaused() && !bangRuled(BangChallenge.BY_HAND);
    }

    /** Vrai si la création automatique suit la liste de courses (le réglage par défaut). */
    public boolean isAutoFollowingList() {
        return state.autoFollowList();
    }

    /** Fait suivre la liste de courses à la création automatique, ou non ; faux tant qu'elle n'est pas acquise. */
    public boolean setAutoFollowList(boolean follow) {
        if (!isMoleculeAutomationUnlocked()) return false;
        state.setAutoFollowList(follow);
        return true;
    }

    /** Mode facultatif, éteint par défaut : entretient les substances utiles aux besoins de matière. */
    public boolean isAutoFollowingMatter() { return state.autoFollowMatter(); }

    public boolean setAutoFollowMatter(boolean enabled) {
        if (!isMoleculeAutomationUnlocked() || !isStatesUnlocked()) return false;
        state.setAutoFollowMatter(enabled);
        return true;
    }

    /**
     * Pour chaque état dont le prochain objectif demande plus de matière rassemblée, sélectionne
     * une substance existante peu encombrante. Ne découvre ni élément ni nouvelle substance ;
     * les choix manuels et la liste de courses restent prioritaires.
     */
    public List<String> autoMatterTargets() {
        if (!isStatesUnlocked() || !isMoleculeAutomationUnlocked()) return List.of();
        List<String> selected = new ArrayList<>();
        for (Map.Entry<Molecule.State, Integer> need : shoppingList().matter().entrySet()) {
            if (need.getValue() <= 0) continue;
            Molecule best = null;
            BigNum least = null;
            for (Molecule candidate : moleculeList) {
                if (!candidate.hasState() || candidate.state() != need.getKey()
                        || !state.hasSubstance(candidate.id()) || !isMoleculeKindUnlocked(candidate.kind())) continue;
                BigNum volume = moleculeVolume(candidate.id());
                if (best == null || volume.lt(least)) { best = candidate; least = volume; }
            }
            if (best != null) selected.add(best.id());
        }
        return selected;
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
        double interval = moleculeAutomationInterval();
        int rounds = (int) Math.min(MOLECULE_AUTOMATION_CATCH_UP, Math.floor(moleculeAutomationTimer / interval + 1e-9));
        if (rounds <= 0) return;
        moleculeAutomationTimer = Math.max(0, Math.min(interval, moleculeAutomationTimer - rounds * interval));
        double reserve = state.autoMoleculeReserve();
        List<String> chosen = state.automatedMolecules();
        for (int round = 0; round < rounds; round++) {
            boolean created = false;
            // D'abord ce que demande le prochain pas : une création par sorte qui manque, dans l'ordre de la liste.
            if (state.autoFollowList()) {
                for (ShoppingList.Item item : shoppingList().items()) {
                    if (item.missing() <= 0) continue;
                    String id = item.molecule().id();
                    if (reserve > 0 && !leavesReserve(id, reserve)) continue;
                    if (!withinStateCap(id)) continue;
                    created |= createMolecule(id, true);
                }
            }
            // Répare les besoins de matière sans monopoliser les amas manuels, et sans les doubler.
            if (state.autoFollowMatter()) {
                for (String id : autoMatterTargets()) {
                    if (chosen.contains(id)) continue; // le joueur gère déjà cette substance lui-même
                    if (reserve > 0 && !leavesReserve(id, reserve)) continue;
                    if (!withinStateCap(id)) continue;
                    created |= createMolecule(id, true);
                }
            }
            if (chosen.isEmpty()) {
                if (!created) break;
                continue;
            }
            int first = Math.floorMod(moleculeAutomationTurn++, chosen.size());
            for (int offset = 0; offset < chosen.size(); offset++) {
                String id = chosen.get((first + offset) % chosen.size());
                if (!molecules.containsKey(id)) continue;
                // La réserve : la création n'a lieu que si elle laisse libre la part d'espace que le joueur veut garder.
                if (reserve > 0 && !leavesReserve(id, reserve)) continue;
                if (!withinStateCap(id)) continue;
                created |= createMolecule(id, true);
            }
            if (!created) break;
        }
    }

    // ------------------------------------------------------------------
    // Planifier la matière (version 0.85)
    // ------------------------------------------------------------------

    /** Astres formés qu'il faut pour planifier plusieurs astres à la fois. */
    public static final int BODY_PLANNING_BODIES = 5;
    /** Astres planifiés ensemble, au plus. */
    public static final int MAX_PLANNED_BODIES = 3;

    /** Vrai une fois {@link #BODY_PLANNING_BODIES} astres formés : la planification de plusieurs astres s'ouvre. */
    public boolean isBodyPlanningUnlocked() {
        return isBodiesUnlocked() && bodiesFormed() >= BODY_PLANNING_BODIES;
    }

    /** Les astres planifiés qui ne sont pas encore formés, dans l'ordre du choix. */
    public List<Body> plannedBodies() {
        List<Body> planned = new ArrayList<>();
        for (String id : state.plannedBodies()) {
            Body body = bodies.get(id);
            if (body != null && !state.hasBody(id)) planned.add(body);
        }
        return planned;
    }

    /** Vrai si cet astre est planifié. */
    public boolean isBodyPlanned(String bodyId) {
        return state.plannedBodies().contains(bodyId) && !state.hasBody(bodyId);
    }

    /**
     * Ajoute cet astre au plan, ou l'en retire. Faux si la planification n'est pas ouverte, si
     * l'astre est déjà formé, ou s'il y en a déjà {@link #MAX_PLANNED_BODIES}.
     */
    public boolean togglePlannedBody(String bodyId) {
        Body body = body(bodyId);
        List<String> planned = new ArrayList<>();
        for (Body kept : plannedBodies()) planned.add(kept.id());
        if (planned.remove(body.id())) {
            state.setPlannedBodies(planned);
            return true;
        }
        if (!isBodyPlanningUnlocked() || state.hasBody(body.id()) || planned.size() >= MAX_PLANNED_BODIES) return false;
        planned.add(body.id());
        state.setPlannedBodies(planned);
        return true;
    }

    /** Ce que demande le plan de plusieurs astres, une ligne par exigence. */
    public record PlanNeed(Kind kind, String id, long needed, long have, boolean gather, boolean gathered, List<String> bodies) {

        /** Ce qu'est l'exigence. */
        public enum Kind {
            /** Un astre plus petit, à avoir formé et mûri. */
            BODY,
            /** Un assemblage à former. */
            ASSEMBLY,
            /** Des molécules d'une sorte, en nombre, rassemblées ou non. */
            MOLECULE,
            /** De la matière rassemblée dans un état. */
            MATTER
        }

        public PlanNeed {
            bodies = List.copyOf(bodies);
        }

        /** Vrai si c'est déjà réuni. */
        public boolean satisfied() {
            return have >= needed && (!gather || gathered);
        }

        /** Vrai si plusieurs astres du plan la demandent en même temps : elle ne se compte qu'une fois, ou s'ajoute (assemblages). */
        public boolean shared() {
            return bodies.size() > 1;
        }

        /** Ce qu'il manque, en nombre. */
        public long missing() {
            return Math.max(0, needed - have);
        }
    }

    /**
     * Les besoins réunis des astres planifiés ({@link #plannedBodies()}) : les astres plus petits, les
     * assemblages, les molécules et la matière. Les seuils (molécules d'un astre, matière) servent à
     * tous les astres à la fois : le plus haut compte. Les ingrédients des assemblages s'ajoutent,
     * puisque deux assemblages ne se partagent pas leurs molécules. Les exigences déjà réunies
     * viennent après les autres.
     */
    public List<PlanNeed> plannedNeeds() {
        Map<String, Long> smaller = new LinkedHashMap<>(), assembled = new LinkedHashMap<>();
        Map<String, long[]> wanted = new LinkedHashMap<>();       // sorte → {ingrédients des assemblages, seuil de l'astre, à rassembler}
        Map<Molecule.State, Long> matter = new java.util.EnumMap<>(Molecule.State.class);
        Map<String, List<String>> askers = new java.util.HashMap<>();
        for (Body body : plannedBodies()) {
            for (String id : body.bodies()) {
                smaller.put(id, 1L);
                askers.computeIfAbsent("B" + id, key -> new ArrayList<>()).add(body.id());
            }
            for (String id : body.assemblies()) {
                assembled.put(id, 1L);
                askers.computeIfAbsent("A" + id, key -> new ArrayList<>()).add(body.id());
                if (state.hasAssembly(id)) continue;
                for (Map.Entry<String, Integer> ingredient : assemblies.get(id).ingredients().entrySet()) {
                    long[] want = wanted.computeIfAbsent(ingredient.getKey(), key -> new long[3]);
                    want[0] += ingredient.getValue();
                    want[2] = 1;
                    List<String> who = askers.computeIfAbsent("M" + ingredient.getKey(), key -> new ArrayList<>());
                    if (!who.contains(body.id())) who.add(body.id());
                }
            }
            for (Map.Entry<String, Integer> needed : body.molecules().entrySet()) {
                long[] want = wanted.computeIfAbsent(needed.getKey(), key -> new long[3]);
                want[1] = Math.max(want[1], needed.getValue());
                List<String> who = askers.computeIfAbsent("M" + needed.getKey(), key -> new ArrayList<>());
                if (!who.contains(body.id())) who.add(body.id());
            }
            for (Map.Entry<Molecule.State, Integer> needed : body.matter().entrySet()) {
                matter.merge(needed.getKey(), (long) needed.getValue(), Math::max);
                askers.computeIfAbsent("S" + needed.getKey().name(), key -> new ArrayList<>()).add(body.id());
            }
        }
        List<PlanNeed> needs = new ArrayList<>();
        for (String id : smaller.keySet()) {
            needs.add(new PlanNeed(PlanNeed.Kind.BODY, id, 1, isBodyMature(id) ? 1 : 0, false, false, askers.get("B" + id)));
        }
        for (String id : assembled.keySet()) {
            needs.add(new PlanNeed(PlanNeed.Kind.ASSEMBLY, id, 1, state.hasAssembly(id) ? 1 : 0, false, false, askers.get("A" + id)));
        }
        for (Map.Entry<String, long[]> want : wanted.entrySet()) {
            String id = want.getKey();
            if (!molecules.containsKey(id)) continue;
            long[] w = want.getValue();
            long needed = Math.max(w[1], w[0] > 0 ? assembledMolecules(id) + w[0] : 0);
            if (w[2] == 1) needed = Math.max(needed, SUBSTANCE_MOLECULES);
            needs.add(new PlanNeed(PlanNeed.Kind.MOLECULE, id, needed, state.moleculeCount(id), w[2] == 1, state.hasSubstance(id),
                    askers.get("M" + id)));
        }
        for (Map.Entry<Molecule.State, Long> need : matter.entrySet()) {
            needs.add(new PlanNeed(PlanNeed.Kind.MATTER, need.getKey().name(), need.getValue(), gatheredInState(need.getKey()), false, false,
                    askers.get("S" + need.getKey().name())));
        }
        List<PlanNeed> ordered = new ArrayList<>();
        for (PlanNeed need : needs) if (!need.satisfied()) ordered.add(need);
        for (PlanNeed need : needs) if (need.satisfied()) ordered.add(need);
        return ordered;
    }

    /** Vrai une fois les plafonds par état ouverts : la création automatique et les états de la matière sont acquis. */
    public boolean isStateCapUnlocked() {
        return isMoleculeAutomationUnlocked() && isStatesUnlocked();
    }

    /** La part de l'espace que la création automatique laisse aux molécules de cet état, de 0,05 à 1 (sans plafond). */
    public double stateCap(Molecule.State matter) {
        return state.stateCap(matter.name());
    }

    /** Règle le plafond d'un état, ramené entre 0,05 et 1 ; 1 l'efface. */
    public void setStateCap(Molecule.State matter, double cap) {
        state.setStateCap(matter.name(), Double.isNaN(cap) ? 1 : Math.max(0.05, Math.min(1, cap)));
    }

    private final Map<Molecule.State, Double> spaceByState = new java.util.EnumMap<>(Molecule.State.class);
    private long spaceByStateVersion = -1;

    /** L'espace qu'occupent les molécules de cet état, rassemblées ou non. */
    public BigNum spaceInState(Molecule.State matter) {
        if (spaceByStateVersion != state.moleculesVersion()) {
            spaceByStateVersion = state.moleculesVersion();
            spaceByState.clear();
            for (Molecule molecule : moleculeList) {
                if (!molecule.hasState()) continue;
                int count = state.moleculeCount(molecule.id());
                if (count > 0) spaceByState.merge(molecule.state(), count * volumeOf(molecule), Double::sum);
            }
        }
        return BigNum.of(spaceByState.getOrDefault(matter, 0.0));
    }

    /** La part de l'espace créé qu'occupent les molécules de cet état, de 0 à 1. */
    public double stateShare(Molecule.State matter) {
        double space = state.space().toDouble();
        return space > 0 ? Math.min(1, spaceInState(matter).toDouble() / space) : 0;
    }

    /** Vrai si la création automatique peut créer cette molécule sans dépasser le plafond de son état. */
    private boolean withinStateCap(String moleculeId) {
        Molecule molecule = molecules.get(moleculeId);
        if (molecule == null || !molecule.hasState()) return true;
        double cap = stateCap(molecule.state());
        if (cap >= 1 || !isStateCapUnlocked()) return true;
        BigNum after = spaceInState(molecule.state()).add(moleculeVolume(moleculeId).multiply(moleculesNextCreation(moleculeId)));
        return after.lte(state.space().multiply(cap));
    }

    /** Vrai une fois le premier assemblage formé : la réservation par recette s'ouvre. */
    public boolean isRecipeReservationUnlocked() {
        return assembliesFormed() > 0;
    }

    /** Vrai si la réservation par recette est ouverte et allumée. */
    public boolean isRecipeReservationOn() {
        return isRecipeReservationUnlocked() && state.recipeReservation();
    }

    public void setRecipeReservation(boolean on) {
        state.setRecipeReservation(on);
    }

    /**
     * Le prochain assemblage à former : le premier de l'astre visé ({@link #nextBody()}) qui ne l'est
     * pas encore, ou celui de la liste de courses avant les astres. {@code null} s'il n'y en a pas.
     */
    public Assembly nextAssembly() {
        // Les cartes des molécules le demandent chacune : il n'est recalculé que lorsque la liste de courses peut changer.
        long key = shoppingKey();
        if (key != nextAssemblyKey) {
            nextAssemblyKey = key;
            nextAssembly = computeNextAssembly();
        }
        return nextAssembly;
    }

    private Assembly nextAssembly;
    private long nextAssemblyKey = Long.MIN_VALUE;

    private Assembly computeNextAssembly() {
        Body body = nextBody();
        if (body != null) {
            for (String id : body.assemblies()) if (!state.hasAssembly(id)) return assemblies.get(id);
            return null;
        }
        ShoppingList list = shoppingList();
        if (list.kind() != ShoppingList.Kind.ASSEMBLY) return null;
        for (Assembly assembly : assemblies.values()) if (assembly.name().equals(list.target())) return assembly;
        return null;
    }

    /**
     * Molécules de cette sorte qu'il faut encore créer pour le prochain assemblage ; −1 si elle n'en
     * est pas un ingrédient. Celles des assemblages déjà formés n'y comptent pas.
     */
    public int recipeRemaining(String moleculeId) {
        Assembly next = nextAssembly();
        if (next == null) return -1;
        Integer count = next.ingredients().get(moleculeId);
        if (count == null) return -1;
        return Math.max(0, assembledMolecules(moleculeId) + count - state.moleculeCount(moleculeId));
    }

    /**
     * Vrai si créer cette sorte maintenant serait une production saturante inutile : la réservation
     * par recette est allumée, la sorte a déjà ce qu'il faut au prochain assemblage, et l'espace libre
     * ne suffirait plus à ce que demande encore la liste de courses.
     */
    public boolean isSaturating(String moleculeId) {
        if (!isRecipeReservationOn() || recipeRemaining(moleculeId) != 0) return false;
        return freeSpace().subtract(moleculeNextSpace(moleculeId)).lt(shoppingSpace());
    }

    /** Vrai une fois le premier Big Bang fait : le filtre « utiles au prochain astre » du tableau s'ouvre. */
    public boolean isNextBodyFilterUnlocked() {
        return state.bigBangs() > 0;
    }

    /**
     * Les éléments qu'il faut aux recettes des molécules utiles au prochain pas
     * ({@link ShoppingList#wants(Molecule)}), parmi celles que le jeu offre déjà : leurs numéros atomiques.
     */
    public java.util.Set<Integer> elementsForNextStep() {
        java.util.Set<Integer> numbers = new java.util.TreeSet<>();
        ShoppingList list = shoppingList();
        if (list.kind() == ShoppingList.Kind.NONE) return numbers;
        for (Molecule molecule : moleculeList) {
            if (!isMoleculeKindUnlocked(molecule.kind()) || !list.wants(molecule)) continue;
            // Une matière qui manque dans un état : seules les sortes déjà rassemblées comptent, sinon tout l'état passerait.
            boolean listed = false;
            for (ShoppingList.Item item : list.items()) listed |= item.molecule().id().equals(molecule.id());
            if (!listed && !state.hasSubstance(molecule.id())) continue;
            numbers.addAll(molecule.recipe().keySet());
        }
        return numbers;
    }

    /** Vrai si créer cette molécule maintenant laisserait libre au moins cette part de l'espace créé. */
    private boolean leavesReserve(String moleculeId, double share) {
        BigNum cost = moleculeVolume(moleculeId).multiply(moleculesNextCreation(moleculeId));
        return freeSpace().subtract(cost).gte(state.space().multiply(share));
    }

    /**
     * Délai entre deux passages de la création automatique, en secondes :
     * {@link #MOLECULE_AUTOMATION_SECONDS}, ou moins une fois acquise une amélioration d'espace qui
     * le raccourcit ({@link SpaceUpgrade.CreationPace}).
     */
    public double moleculeAutomationInterval() {
        double interval = MOLECULE_AUTOMATION_SECONDS;
        for (SpaceUpgrade upgrade : spaceUpgrades.values()) {
            if (upgrade.effect() instanceof SpaceUpgrade.CreationPace pace && state.ownsSpaceUpgrade(upgrade.id())) {
                interval = Math.min(interval, pace.seconds());
            }
        }
        return interval;
    }

    /** Part de l'espace créé que la création automatique laisse toujours libre, de 0 à 1 : la création à la main n'en tient pas compte. */
    public double autoMoleculeReserve() {
        return state.autoMoleculeReserve();
    }

    /** Règle la part d'espace que la création automatique laisse libre ; ramenée entre 0 et 0,95. */
    public void setAutoMoleculeReserve(double share) {
        state.setAutoMoleculeReserve(Math.max(0, Math.min(0.95, Double.isNaN(share) ? 0 : share)));
    }

    /**
     * Confie à la création automatique toutes les sortes rassemblées, ou les lui retire toutes.
     *
     * @return le nombre de sortes dont le réglage a changé ; 0 tant que l'automatisme n'est pas acquis
     */
    public int setAllMoleculesAutomated(boolean automated) {
        if (!isMoleculeAutomationUnlocked()) return 0;
        int changed = 0;
        for (String id : automated ? state.substances() : state.automatedMolecules()) {
            if (!molecules.containsKey(id) || state.isMoleculeAutomated(id) == automated) continue;
            state.setMoleculeAutomated(id, automated);
            changed++;
        }
        return changed;
    }

    // ------------------------------------------------------------------
    // Le rassemblement et la formation automatiques
    // ------------------------------------------------------------------

    /** Délai entre deux passages du rassemblement et de la formation automatiques, en secondes. */
    public static final double AUTO_FORM_SECONDS = 1;
    private double autoFormTimer = 0;
    private int autoGathered = 0;
    private final List<String> autoFormed = new ArrayList<>();

    /** Vrai une fois acquise l'amélioration d'espace « Rassemblement automatique » ({@link SpaceUpgrade.AutoGather}). */
    public boolean isAutoGatherUnlocked() {
        return ownsSpaceEffect(SpaceUpgrade.AutoGather.class);
    }

    /** Vrai si le rassemblement automatique est en marche (il ne sert qu'une fois acquis). */
    public boolean isAutoGatherEnabled() {
        return state.autoGather();
    }

    /** Met en marche ou coupe le rassemblement automatique ; faux s'il n'est pas acquis. */
    public boolean setAutoGatherEnabled(boolean enabled) {
        if (!isAutoGatherUnlocked()) return false;
        state.setAutoGather(enabled);
        return true;
    }

    /** Vrai si les sortes se rassemblent d'elles-mêmes en ce moment : l'automatisme est acquis, en marche, et rien n'est coupé. */
    public boolean isAutoGathering() {
        return isAutoGatherUnlocked() && state.autoGather() && !state.automationPaused() && !bangRuled(BangChallenge.BY_HAND);
    }

    /** Vrai une fois acquise l'amélioration d'espace « Formation automatique » ({@link SpaceUpgrade.AutoForm}). */
    public boolean isAutoFormUnlocked() {
        return ownsSpaceEffect(SpaceUpgrade.AutoForm.class);
    }

    /** Vrai si la formation automatique est en marche (elle ne sert qu'une fois acquise). */
    public boolean isAutoFormEnabled() {
        return state.autoForm();
    }

    /** Met en marche ou coupe la formation automatique ; faux si elle n'est pas acquise. */
    public boolean setAutoFormEnabled(boolean enabled) {
        if (!isAutoFormUnlocked()) return false;
        state.setAutoForm(enabled);
        return true;
    }

    /** Vrai si les assemblages et les astres se forment d'eux-mêmes en ce moment. */
    public boolean isAutoForming() {
        return isAutoFormUnlocked() && state.autoForm() && !state.automationPaused() && !bangRuled(BangChallenge.BY_HAND);
    }

    /**
     * Fait passer le temps du rassemblement et de la formation automatiques : une fois par
     * seconde, tout ce qui peut être rassemblé l'est, puis tout assemblage et tout astre dont les
     * conditions sont réunies est formé. Les échelles du cosmos restent au joueur.
     */
    private void runAutoForm(double dt) {
        boolean gathering = isAutoGathering();
        boolean forming = isAutoForming();
        if (dt <= 0 || !state.started() || (!gathering && !forming)) {
            autoFormTimer = 0;
            return;
        }
        autoFormTimer += dt;
        if (autoFormTimer < AUTO_FORM_SECONDS) return;
        autoFormTimer = 0;
        if (gathering) {
            for (String id : state.molecules().keySet()) {
                if (molecules.containsKey(id) && formSubstance(id)) autoGathered++;
            }
        }
        if (!forming) return;
        boolean again;
        do {
            again = false;
            for (Assembly assembly : assemblies.values()) {
                if (formAssembly(assembly.id())) {
                    autoFormed.add(assembly.name());
                    again = true;
                }
            }
            for (Body body : bodies.values()) {
                if (formBody(body.id())) {
                    autoFormed.add(body.name());
                    again = true;
                }
            }
        } while (again);
    }

    /** Nombre de sortes que l'automatisme a rassemblées depuis le dernier appel : pour l'annoncer. Remis à zéro à chaque lecture. */
    public int takeAutoGathered() {
        int gathered = autoGathered;
        autoGathered = 0;
        return gathered;
    }

    /** Les noms des assemblages et des astres que l'automatisme a formés depuis le dernier appel, dans l'ordre. La liste est vidée à chaque lecture. */
    public List<String> takeAutoFormed() {
        List<String> formed = List.copyOf(autoFormed);
        autoFormed.clear();
        return formed;
    }

    /** Ce par quoi les paliers atteints multiplient la matière noire de chaque explosion ({@link BigBangMilestone.DarkMatter}). */
    public double bigBangDarkMatterFactor() {
        if (bangRuled(BangChallenge.FORGOTTEN)) return 1;
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
        // La récompense d'un défi de Big Bang reste, même pendant celui qui fait taire les paliers.
        double factor = bangReward(BangChallenge.Reward.ACCRETION);
        if (bangRuled(BangChallenge.FORGOTTEN)) return factor;
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
    public long moleculesCreated() {
        return state.moleculeTotal();
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

    /** Vrai si cette sorte a atteint {@link #MAX_MOLECULES} molécules : on ne peut plus en créer, quoi qu'on ait. */
    public boolean isMoleculeKindFull(String moleculeId) {
        return state.moleculeCount(molecule(moleculeId).id()) >= MAX_MOLECULES;
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
     * Crée une molécule du rayon, tirée au hasard parmi celles qui peuvent l'être maintenant : d'abord
     * parmi celles que demande le prochain pas ({@link #shoppingList()}), sinon parmi toutes. C'est
     * l'achat simple : un clic par rayon, sans avoir à choisir la sorte.
     *
     * @return la molécule créée, ou {@code null} si aucune du rayon ne peut l'être
     */
    public Molecule createRandomMolecule(Molecule.Kind kind) {
        if (!isMoleculeKindUnlocked(kind)) return null;
        ShoppingList shopping = shoppingList();
        List<Molecule> wanted = new ArrayList<>();
        List<Molecule> any = new ArrayList<>();
        for (Molecule molecule : molecules.values()) {
            if (molecule.kind() != kind || !canCreateMolecule(molecule.id())) continue;
            any.add(molecule);
            if (shopping.wants(molecule)) wanted.add(molecule);
        }
        List<Molecule> pool = wanted.isEmpty() ? any : wanted;
        if (pool.isEmpty()) return null;
        Molecule chosen = pool.get(random.nextInt(pool.size()));
        return createMolecule(chosen.id()) ? chosen : null;
    }

    /** Vrai si au moins une molécule du rayon peut être créée maintenant. */
    public boolean canCreateInKind(Molecule.Kind kind) {
        if (!isMoleculeKindUnlocked(kind)) return false;
        for (Molecule molecule : molecules.values()) {
            if (molecule.kind() == kind && canCreateMolecule(molecule.id())) return true;
        }
        return false;
    }

    /**
     * Ce que le prochain pas demande, rayon par rayon : le nombre de molécules qu'il reste à créer
     * dans chaque rayon d'après la liste de courses. Les rayons dont rien ne manque n'y sont pas.
     */
    public Map<Molecule.Kind, Integer> missingByKind() {
        Map<Molecule.Kind, Integer> missing = new java.util.EnumMap<>(Molecule.Kind.class);
        for (ShoppingList.Item item : shoppingList().items()) {
            int count = item.missing();
            if (count > 0) missing.merge(item.molecule().kind(), count, Integer::sum);
        }
        return missing;
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
        return createMolecule(moleculeId, false);
    }

    /** {@link #createMolecule(String)}, en notant pour les statistiques si c'est l'automatisme qui crée. */
    private boolean createMolecule(String moleculeId, boolean automatic) {
        if (!canCreateMolecule(moleculeId)) return false;
        Molecule molecule = molecule(moleculeId);
        int created = moleculesNextCreation(moleculeId);
        created += moleculeDoubling(moleculeId, created);
        long elements = 0;
        for (Map.Entry<Integer, Integer> atom : nextMoleculeCost(moleculeId).entrySet()) {
            state.setElementCount(atom.getKey(), state.elementCount(atom.getKey()) - atom.getValue());
            elements += atom.getValue();
        }
        state.addMolecules(molecule.id(), created);
        state.stats().noteMoleculeCreation(created, elements, automatic);
        noteStep(GameStats.Step.FIRST_MOLECULE);
        return true;
    }

    /** Chance qu'une création de molécule en donne deux fois plus : le dédoublement du collisionneur (5 % par niveau). */
    public double moleculeDoublingChance() {
        return 0.05 * colliderUpgradeLevel(ColliderUpgrade.MOLECULES);
    }

    /**
     * Les molécules de plus que donne le dédoublement à cette création : autant qu'elle en crée,
     * dans la limite de l'espace libre et des plafonds ; 0 s'il ne joue pas cette fois. Le hasard est
     * tiré du nombre de créations, sans toucher à celui de la synthèse.
     */
    private int moleculeDoubling(String moleculeId, int created) {
        double chance = moleculeDoublingChance();
        if (chance <= 0) return 0;
        double mix = Math.sin(state.stats().moleculeCreations() * 12.9898 + 61 * 78.233) * 43758.5453;
        if (mix - Math.floor(mix) >= chance) return 0;
        Molecule molecule = molecule(moleculeId);
        double room = state.space().multiply(1 + 1e-9).subtract(usedSpace()).toDouble() / volumeOf(molecule) - created;
        long caps = (long) MAX_MOLECULES - state.moleculeCount(molecule.id()) - created;
        return (int) Math.max(0, Math.min(created, Math.min(Math.floor(room), caps)));
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
        return matterBoost(stat) * upgradeBoosts.getOrDefault(stat, 1.0) * bigBangMilestoneBoost(stat) * collectionBoost(stat)
                * skinBoost();
    }

    /**
     * La part de {@link #moleculeBoost(Molecule.Stat)} qui vient de la matière elle-même : les
     * molécules créées, les assemblages, les astres et la galaxie, sans les améliorations d'espace
     * ni les paliers de Big Bang. Vaut 1 tant que rien n'est créé.
     */
    public double matterBoost(Molecule.Stat stat) {
        // Dans l'univers vide, la matière ne multiplie plus rien.
        if (bangRuled(BangChallenge.VOID)) return 1;
        refreshMoleculeBonuses();
        return (1 + moleculeBoosts.getOrDefault(stat, 0.0)) * cosmosFactor() * Math.pow(1 + COSMOS_MILESTONE_GAIN, cosmosMilestones());
    }

    /** Ce par quoi la galaxie, l'amas et l'univers formés multiplient toutes les grandeurs de la matière (version 0.68). */
    public double cosmosFactor() {
        double factor = 1;
        for (Cosmos scale : Cosmos.values()) if (hasCosmos(scale)) factor *= scale.factor();
        return factor;
    }

    /**
     * Ce qu'ajoute chaque jalon du cosmos ({@link #cosmosMilestones()}) à toutes les grandeurs de la
     * matière : +10 %, cumulés en multipliant. Toutes, et pas seulement l'espace : en fin de partie,
     * l'espace abonde, ce sont les éléments des explosions qui manquent.
     */
    public static final double COSMOS_MILESTONE_GAIN = 0.10;

    /**
     * Les jalons de la fin de partie (version 0.68) : chaque état de la matière réuni pour l'amas de
     * galaxies, puis pour l'univers, une fois l'échelle en vue. Une échelle formée compte tous les
     * siens. Dix en tout.
     */
    public int cosmosMilestones() {
        int count = 0;
        for (Cosmos scale : Cosmos.values()) {
            if (scale == Cosmos.GALAXY) continue;
            if (hasCosmos(scale)) {
                count += scale.matter().size();
                continue;
            }
            if (!isCosmosUnlocked(scale)) continue;
            for (Map.Entry<Molecule.State, Integer> need : scale.matter().entrySet()) {
                if (gatheredInState(need.getKey()) >= need.getValue()) count++;
            }
        }
        return count;
    }

    /** Vrai si cet état de la matière est réuni pour cette échelle du cosmos : un jalon (version 0.68). */
    public boolean isCosmosMilestoneMet(Cosmos scale, Molecule.State matter) {
        Integer need = scale.matter().get(matter);
        return need != null && (hasCosmos(scale) || isCosmosUnlocked(scale) && gatheredInState(matter) >= need);
    }

    // ------------------------------------------------------------------
    // Les collections : la moitié d'un rayon, puis le rayon entier
    // ------------------------------------------------------------------

    private final Map<Molecule.Kind, int[]> kindCounts = new java.util.EnumMap<>(Molecule.Kind.class);
    private int kindCountsVersion = -1;

    private int[] kindCount(Molecule.Kind kind) {
        if (kindCountsVersion != state.moleculesVersion()) {
            kindCountsVersion = state.moleculesVersion();
            kindCounts.clear();
            for (Molecule molecule : molecules.values()) {
                int[] count = kindCounts.computeIfAbsent(molecule.kind(), each -> new int[2]);
                count[1]++;
                if (state.moleculeCount(molecule.id()) > 0) count[0]++;
            }
        }
        return kindCounts.getOrDefault(kind, new int[2]);
    }

    /** Les collections des rayons, dans l'ordre du catalogue ({@link KindCollections}). */
    public List<KindCollection> collections() {
        return KindCollections.DEFAULT;
    }

    /** Nombre de sortes de ce rayon dont le joueur a créé au moins une molécule. */
    public int kindCreated(Molecule.Kind kind) {
        return kindCount(kind)[0];
    }

    /** Nombre de sortes de ce rayon dans le catalogue. */
    public int kindSize(Molecule.Kind kind) {
        return kindCount(kind)[1];
    }

    /** Nombre de sortes à créer pour la moitié de la collection de ce rayon : la moitié du rayon, arrondie au-dessus. */
    public int collectionHalf(Molecule.Kind kind) {
        return (kindSize(kind) + 1) / 2;
    }

    /** Où en est la collection d'un rayon : 0 tant que la moitié n'y est pas, 1 à la moitié, 2 une fois complète. */
    public int collectionLevel(Molecule.Kind kind) {
        int size = kindSize(kind);
        int created = kindCreated(kind);
        if (size == 0 || !isBigBangUnlocked()) return 0;
        return created >= size ? 2 : created >= collectionHalf(kind) ? 1 : 0;
    }

    /** Nombre de collections au moins à moitié réunies, et nombre de collections complètes. */
    public int collectionsAt(int level) {
        int count = 0;
        for (KindCollection collection : KindCollections.DEFAULT) {
            if (collectionLevel(collection.kind()) >= level) count++;
        }
        return count;
    }

    /** Ce par quoi les collections réunies multiplient une grandeur : leurs facteurs se multiplient entre eux. Vaut 1 sans aucune. */
    public double collectionBoost(Molecule.Stat stat) {
        double factor = 1;
        for (KindCollection collection : KindCollections.DEFAULT) {
            if (collection.stat() == stat) factor *= collection.factor(collectionLevel(collection.kind()));
        }
        return factor;
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
            // Un astre qui mûrit donne déjà une part de son bonus.
            double share = state.maturingLeft(id) > 0 ? MATURING_SHARE : 1;
            if (body != null) moleculeBoosts.merge(body.boost().stat(), body.boost().perMolecule() * share, Double::sum);
        }
        // Chaque système solaire complet augmente toutes les grandeurs, selon son harmonie.
        for (SolarSystem system : state.solarSystems()) {
            if (!isSystemComplete(system)) continue;
            double gain = SYSTEM_GAIN * systemHarmony(system);
            for (Molecule.Stat stat : Molecule.Stat.values()) moleculeBoosts.merge(stat, gain, Double::sum);
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
     * Vrai si cette amélioration sera acquise dès que l'espace créé suffira : le premier Big Bang
     * a eu lieu, et celle dont elle dépend est déjà acquise.
     */
    public boolean isSpaceUpgradeAvailable(String spaceUpgradeId) {
        SpaceUpgrade upgrade = spaceUpgrade(spaceUpgradeId);
        return isBigBangUnlocked() && (upgrade.requires() == null || state.ownsSpaceUpgrade(upgrade.requires()));
    }

    /**
     * Vrai si cette amélioration peut être acquise maintenant : elle est accessible, pas encore
     * acquise, et l'expansion a créé en tout autant d'espace qu'elle en demande
     * ({@link SpaceUpgrade#space()}). L'espace libre n'entre pas en compte.
     */
    public boolean canBuySpaceUpgrade(String spaceUpgradeId) {
        SpaceUpgrade upgrade = spaceUpgrade(spaceUpgradeId);
        return state.started() && isSpaceUpgradeAvailable(spaceUpgradeId) && !state.ownsSpaceUpgrade(upgrade.id())
                && hasCreatedSpace(upgrade.space());
    }

    /** Les améliorations d'espace acquises depuis le dernier appel de {@link #takeNewSpaceUpgrades()}. */
    private final List<SpaceUpgrade> newSpaceUpgrades = new ArrayList<>();

    /**
     * Acquiert toutes les améliorations d'espace dont le seuil est atteint : elles ne coûtent rien,
     * il n'y a donc rien à décider. Une amélioration peut en ouvrir une autre, déjà à portée : on
     * recommence tant qu'il s'en prend.
     */
    private void takeSpaceUpgrades() {
        if (!state.started() || !isBigBangUnlocked() || state.spaceUpgradeCount() >= spaceUpgrades.size()) return;
        boolean taken;
        do {
            taken = false;
            for (SpaceUpgrade upgrade : spaceUpgrades.values()) {
                if (!canBuySpaceUpgrade(upgrade.id())) continue;
                state.addSpaceUpgrade(upgrade.id());
                newSpaceUpgrades.add(upgrade);
                taken = true;
            }
        } while (taken);
    }

    /**
     * Les améliorations d'espace que le temps a apportées depuis le dernier appel, dans l'ordre :
     * l'interface s'en sert pour les annoncer. La liste est vidée à chaque lecture.
     */
    public List<SpaceUpgrade> takeNewSpaceUpgrades() {
        List<SpaceUpgrade> taken = List.copyOf(newSpaceUpgrades);
        newSpaceUpgrades.clear();
        return taken;
    }

    private boolean ownsSpaceEffect(Class<? extends SpaceUpgrade.Effect> type) {
        if (!isBigBangUnlocked()) return false;
        for (SpaceUpgrade upgrade : spaceUpgrades.values()) {
            if (type.isInstance(upgrade.effect()) && state.ownsSpaceUpgrade(upgrade.id())) return true;
        }
        return false;
    }

    /** Vrai une fois acquise l'amélioration d'espace « Création continue » : tenir le clic sur une molécule répète sa création. */
    public boolean isHoldCreateUnlocked() {
        return ownsSpaceEffect(SpaceUpgrade.HoldCreate.class);
    }

    /** Vrai une fois acquise l'amélioration d'espace « Gestes groupés » : tout rassembler, tout assembler, former tous les astres prêts. */
    public boolean isBulkFormUnlocked() {
        return ownsSpaceEffect(SpaceUpgrade.BulkForm.class);
    }

    /**
     * Rassemble toutes les sortes de molécules qui peuvent l'être, dans l'ordre du catalogue, tant
     * que l'espace libre suffit ({@link #formSubstance(String)}). Il faut les gestes groupés.
     *
     * @return le nombre de sortes rassemblées
     */
    public int formAllSubstances() {
        if (!isBulkFormUnlocked()) return 0;
        int formed = 0;
        for (Molecule molecule : moleculeList) {
            if (formSubstance(molecule.id())) formed++;
        }
        return formed;
    }

    /**
     * Forme tous les assemblages qui peuvent l'être, dans l'ordre du catalogue
     * ({@link #formAssembly(String)}). Il faut les gestes groupés.
     *
     * @return le nombre d'assemblages formés
     */
    public int formAllAssemblies() {
        if (!isBulkFormUnlocked()) return 0;
        int formed = 0;
        for (Assembly assembly : assemblies.values()) {
            if (formAssembly(assembly.id())) formed++;
        }
        return formed;
    }

    /**
     * Forme tous les astres qui peuvent l'être ({@link #formBody(String)}). Un astre formé peut en
     * rendre un plus grand possible : on recommence tant qu'il s'en forme. Il faut les gestes groupés.
     *
     * @return le nombre d'astres formés
     */
    public int formAllBodies() {
        if (!isBulkFormUnlocked()) return 0;
        int formed = 0;
        boolean again;
        do {
            again = false;
            for (Body body : bodies.values()) {
                if (formBody(body.id())) {
                    formed++;
                    again = true;
                }
            }
        } while (again);
        return formed;
    }

    /** Nombre de sortes qui peuvent être rassemblées en ce moment, chacune prise seule. */
    public int substancesReady() {
        if (!isStatesUnlocked()) return 0;
        int ready = 0;
        for (String id : state.molecules().keySet()) {
            if (molecules.containsKey(id) && canFormSubstance(id)) ready++;
        }
        return ready;
    }

    /** Nombre d'assemblages qui peuvent être formés en ce moment. */
    public int assembliesReady() {
        if (!isAssembliesUnlocked()) return 0;
        int ready = 0;
        for (Assembly assembly : assemblies.values()) {
            if (canFormAssembly(assembly.id())) ready++;
        }
        return ready;
    }

    /** Nombre d'astres qui peuvent être formés en ce moment. */
    public int bodiesReady() {
        if (!isBodiesUnlocked()) return 0;
        int ready = 0;
        for (Body body : bodies.values()) {
            if (canFormBody(body.id())) ready++;
        }
        return ready;
    }

    // ------------------------------------------------------------------
    // Les molécules favorites et la liste de courses
    // ------------------------------------------------------------------

    /** Vrai si le joueur a épinglé cette molécule. */
    public boolean isMoleculeFavorite(String moleculeId) {
        return state.isMoleculeFavorite(molecule(moleculeId).id());
    }

    /** Épingle une molécule du catalogue, ou la retire des favorites. */
    public void setMoleculeFavorite(String moleculeId, boolean favorite) {
        state.setMoleculeFavorite(molecule(moleculeId).id(), favorite);
    }

    /** Les molécules épinglées, dans l'ordre où elles l'ont été. */
    public List<Molecule> favoriteMolecules() {
        List<Molecule> favorites = new ArrayList<>();
        for (String id : state.favoriteMolecules()) {
            Molecule molecule = molecules.get(id);
            if (molecule != null) favorites.add(molecule);
        }
        return favorites;
    }

    private ShoppingList shopping = ShoppingList.NONE;
    private long shoppingVersion = Long.MIN_VALUE;

    /**
     * Ce qu'il manque pour le prochain pas du troisième acte ({@link ShoppingList}) : l'astre à
     * portée le plus avancé, l'assemblage le plus avancé tant que les astres ne sont pas ouverts,
     * ou la prochaine échelle du cosmos une fois tous les astres formés.
     */
    public ShoppingList shoppingList() {
        long key = shoppingKey();
        if (shoppingVersion != key) {
            shoppingVersion = key;
            shopping = computeShoppingList();
        }
        return shopping;
    }

    /**
     * Ce dont dépend la liste de courses. Les molécules ne suffisaient pas : un assemblage ou un astre
     * formé, un rassemblement, une explosion qui vide le tableau, une amélioration d'espace ou un Big
     * Bang la changent aussi. Avant la 0.47, elle pouvait rester sur un astre déjà formé et ne plus
     * rien montrer.
     */
    private long shoppingKey() {
        long key = state.moleculesVersion();
        for (int part : new int[] {state.elementsVersion(), assembliesFormed(), bodiesFormed(), substancesFormed(),
                state.spaceUpgradeCount(), state.cosmosLevel(), state.bigBangs(), state.started() ? 1 : 0}) {
            key = key * 1_000_003L + part;
        }
        return key;
    }

    private ShoppingList computeShoppingList() {
        if (!state.started() || !isBigBangUnlocked()) return ShoppingList.NONE;
        if (isBodiesUnlocked()) {
            Body best = nextBody();
            if (best != null) return shoppingFor(best);
        } else if (isAssembliesUnlocked()) {
            // L'assemblage entamé auquel il manque le moins d'ingrédients ; à défaut, le premier du catalogue.
            Assembly best = null;
            int bestMissing = Integer.MAX_VALUE;
            for (Assembly assembly : assemblies.values()) {
                if (state.hasAssembly(assembly.id())) continue;
                int missing = 0;
                boolean started = false;
                for (Map.Entry<String, Integer> ingredient : assembly.ingredients().entrySet()) {
                    if (spareMolecules(ingredient.getKey()) < ingredient.getValue()) missing++;
                    started |= state.moleculeCount(ingredient.getKey()) > 0;
                }
                if (best == null || (started && missing < bestMissing)) {
                    best = assembly;
                    bestMissing = started ? missing : Integer.MAX_VALUE;
                }
            }
            if (best != null) {
                Map<String, int[]> wanted = new LinkedHashMap<>();
                Map<String, String> purposes = new java.util.HashMap<>();
                wantAssembly(best, wanted, purposes);
                return new ShoppingList(ShoppingList.Kind.ASSEMBLY, best.name(), canFormAssembly(best.id()),
                        shoppingItems(wanted, purposes), Map.of(), canFormAssembly(best.id()) ? List.of(best) : List.of());
            }
        }
        Cosmos next = nextCosmos();
        if (next == null || !isCosmosUnlocked(next)) return ShoppingList.NONE;
        Map<Molecule.State, Integer> matter = new java.util.EnumMap<>(Molecule.State.class);
        for (Map.Entry<Molecule.State, Integer> need : next.matter().entrySet()) {
            int missing = (int) Math.max(0, need.getValue() - gatheredInState(need.getKey()));
            if (missing > 0) matter.put(need.getKey(), missing);
        }
        Map<Molecule.State, Integer> sorts = new java.util.EnumMap<>(Molecule.State.class);
        for (Map.Entry<Molecule.State, Integer> need : next.sorts().entrySet()) {
            int missing = need.getValue() - sortsInState(need.getKey());
            if (missing > 0) sorts.put(need.getKey(), missing);
        }
        return new ShoppingList(ShoppingList.Kind.COSMOS, next.phrase(), canFormCosmos(next), List.of(), matter, List.of(), sorts);
    }

    /**
     * L'astre que le joueur a le plus de chances de former ensuite : parmi ceux qui sont à portée
     * (leurs astres plus petits sont formés), celui auquel il manque le moins de conditions ; à
     * égalité, le premier du catalogue. {@code null} avant l'ouverture des astres, ou une fois tous formés.
     */
    public Body nextBody() {
        if (!state.started() || !isBodiesUnlocked()) return null;
        Body best = null;
        int bestMissing = Integer.MAX_VALUE;
        for (Body body : bodies.values()) {
            if (state.hasBody(body.id())) continue;
            boolean reachable = true;
            for (String smaller : body.bodies()) reachable &= isBodyMature(smaller);
            if (!reachable) continue;
            int missing = bodyConditions(body.id()) - bodyConditionsMet(body.id());
            if (missing < bestMissing) {
                best = body;
                bestMissing = missing;
            }
        }
        return best;
    }

    /**
     * Espace que demande encore la liste de courses ({@link #shoppingList()}) : le volume des
     * molécules qui manquent, celui des lieux de rassemblement à ouvrir, et, pour la matière qui
     * manque dans un état, le volume de la plus petite molécule de cet état que le joueur peut
     * créer. C'est une estimation par le bas : elle ne compte ni les exemplaires d'éléments à
     * regarnir, ni les molécules plus grosses que le joueur choisirait.
     */
    public BigNum shoppingSpace() {
        // Le calcul parcourt le catalogue : il n'est refait que lorsque les molécules ou les rayons ouverts ont changé.
        long key = shoppingKey();
        if (key == shoppingSpaceKey && shoppingSpace != null) return shoppingSpace;
        shoppingSpaceKey = key;
        shoppingSpace = computeShoppingSpace();
        return shoppingSpace;
    }

    private BigNum shoppingSpace = null;
    private long shoppingSpaceKey = -1;

    private BigNum computeShoppingSpace() {
        ShoppingList list = shoppingList();
        double space = 0;
        Map<Molecule.State, Integer> covered = new java.util.EnumMap<>(Molecule.State.class);
        for (ShoppingList.Item item : list.items()) {
            Molecule molecule = item.molecule();
            double volume = volumeOf(molecule);
            space += item.missing() * volume;
            if (item.gather() && molecule.hasState()) space += volume * SUBSTANCE_MOLECULES;
            // Rassemblées, ces molécules compteront aussi dans la matière de leur état.
            if (molecule.hasState() && (item.gather() || state.hasSubstance(molecule.id()))) {
                covered.merge(molecule.state(), item.missing(), Integer::sum);
            }
        }
        for (Map.Entry<Molecule.State, Integer> lacking : list.matter().entrySet()) {
            int missing = lacking.getValue() - covered.getOrDefault(lacking.getKey(), 0);
            if (missing > 0) space += missing * smallestVolume(lacking.getKey());
        }
        // Une sorte de plus à rassembler : ses premières molécules, et son lieu, qui en vaut autant.
        for (Map.Entry<Molecule.State, Integer> lacking : list.sorts().entrySet()) {
            space += lacking.getValue() * 2.0 * SUBSTANCE_MOLECULES * smallestVolume(lacking.getKey());
        }
        return BigNum.of(space);
    }

    /** Le volume de la plus petite molécule de cet état dans les rayons ouverts ; d'abord parmi les sortes déjà rassemblées. */
    private double smallestVolume(Molecule.State matter) {
        double gathered = Double.POSITIVE_INFINITY;
        double any = Double.POSITIVE_INFINITY;
        for (Molecule molecule : molecules.values()) {
            if (!molecule.hasState() || molecule.state() != matter || !isMoleculeKindUnlocked(molecule.kind())) continue;
            double volume = volumeOf(molecule);
            any = Math.min(any, volume);
            if (state.hasSubstance(molecule.id())) gathered = Math.min(gathered, volume);
        }
        return Double.isInfinite(gathered) ? (Double.isInfinite(any) ? 0 : any) : gathered;
    }

    /**
     * Secondes d'expansion avant que l'espace libre suffise à la liste de courses
     * ({@link #shoppingSpace()}) : 0 s'il suffit déjà ou s'il n'y a rien à viser, l'infini si
     * l'espace ne grandit pas.
     */
    public double secondsUntilShopping() {
        BigNum needed = shoppingSpace();
        BigNum free = freeSpace();
        if (needed.isZero() || free.gte(needed)) return 0;
        double rate = spacePerSecond().toDouble();
        return rate > 0 ? needed.subtract(free).toDouble() / rate : Double.POSITIVE_INFINITY;
    }

    // ------------------------------------------------------------------
    // Le meilleur achat : ce qu'une création rapporte pour l'espace qu'elle prend
    // ------------------------------------------------------------------

    /** Pour combien compterait cette sorte avec {@code count} molécules, rassemblée ou non comme elle l'est aujourd'hui. */
    private double effectiveAt(Molecule molecule, int count) {
        double doublings = doublings(count);
        if (!molecule.hasState() || !state.hasSubstance(molecule.id())) return doublings;
        return Math.pow(doublings, molecule.state().exponent());
    }

    /**
     * Ce que la prochaine création dans cette sorte ajouterait aux grandeurs du jeu, en proportion
     * de ce qu'elles valent : 0,02 pour « +2 % ». Les gains sur plusieurs grandeurs s'ajoutent (le
     * bonus de la molécule, et ce que son état ajoute une fois rassemblée). Une molécule qui relève
     * un plafond d'exemplaires ne multiplie rien : elle vaut 0 ici.
     */
    public double moleculeGain(String moleculeId) {
        Molecule molecule = molecule(moleculeId);
        if (isMoleculeKindFull(moleculeId)) return 0;     // une sorte pleine ne peut plus rien rapporter
        int count = state.moleculeCount(molecule.id());
        int after = count + moleculesPerCreation(moleculeId);
        double more = effectiveAt(molecule, after) - effectiveAt(molecule, count);
        if (more <= 0) return 0;
        refreshMoleculeBonuses();
        double gain = 0;
        if (molecule.bonus() instanceof Molecule.Boost boost) {
            gain += boost.perMolecule() * more / (1 + moleculeBoosts.getOrDefault(boost.stat(), 0.0));
        }
        if (molecule.hasState() && state.hasSubstance(molecule.id())) {
            Molecule.State matter = molecule.state();
            gain += matter.particles() * more / (1 + moleculeBoosts.getOrDefault(Molecule.Stat.PARTICLES, 0.0));
            gain += matter.atoms() * more / (1 + moleculeBoosts.getOrDefault(Molecule.Stat.ATOMS, 0.0));
        }
        return gain;
    }

    /** Espace que prendrait la prochaine création dans cette sorte, quand la place ne manque pas : le volume de toutes les molécules qu'elle ajoute. */
    public BigNum moleculeNextSpace(String moleculeId) {
        return BigNum.of(volumeOf(molecule(moleculeId)) * moleculesPerCreation(moleculeId));
    }

    /**
     * Le rendement d'une sorte : ce que sa prochaine création rapporte ({@link #moleculeGain(String)})
     * par unité d'espace qu'elle prend ({@link #moleculeNextSpace(String)}). C'est ce qui permet
     * de comparer deux molécules : la meilleure est celle dont le rendement est le plus haut.
     */
    public double moleculeYield(String moleculeId) {
        double space = moleculeNextSpace(moleculeId).toDouble();
        return space > 0 ? moleculeGain(moleculeId) / space : 0;
    }

    /**
     * Le meilleur achat du moment : parmi les molécules qui peuvent être créées maintenant, celle
     * dont le rendement ({@link #moleculeYield(String)}) est le plus haut. {@code null} si aucune
     * création possible ne rapporte quoi que ce soit.
     */
    public Molecule bestMolecule() {
        if (!state.started() || !isBigBangUnlocked()) return null;
        Molecule best = null;
        double bestYield = 0;
        for (Molecule molecule : molecules.values()) {
            if (!isMoleculeKindUnlocked(molecule.kind())) continue;
            double yield = moleculeYield(molecule.id());
            if (yield <= bestYield || !canCreateMolecule(molecule.id())) continue;
            best = molecule;
            bestYield = yield;
        }
        return best;
    }

    /** La liste de courses d'un astre : les ingrédients de ses assemblages pas encore formés, ses molécules, sa matière. */
    private ShoppingList shoppingFor(Body body) {
        Map<String, int[]> wanted = new LinkedHashMap<>();       // sorte → {à avoir en tout, 1 s'il faut la rassembler}
        Map<String, String> purposes = new java.util.HashMap<>();
        List<Assembly> ready = new ArrayList<>();
        for (String id : body.assemblies()) {
            if (state.hasAssembly(id)) continue;
            Assembly assembly = assemblies.get(id);
            if (canFormAssembly(id)) ready.add(assembly);
            else wantAssembly(assembly, wanted, purposes);
        }
        for (Map.Entry<String, Integer> needed : body.molecules().entrySet()) {
            int[] want = wanted.computeIfAbsent(needed.getKey(), each -> new int[2]);
            want[0] = Math.max(want[0], needed.getValue());
            purposes.putIfAbsent(needed.getKey(), "");
        }
        Map<Molecule.State, Integer> matter = new java.util.EnumMap<>(Molecule.State.class);
        for (Map.Entry<Molecule.State, Integer> needed : body.matter().entrySet()) {
            int missing = (int) Math.max(0, needed.getValue() - gatheredInState(needed.getKey()));
            if (missing > 0) matter.put(needed.getKey(), missing);
        }
        return new ShoppingList(ShoppingList.Kind.BODY, body.name(), canFormBody(body.id()), shoppingItems(wanted, purposes), matter, ready);
    }

    /**
     * Ajoute à la liste ce que demande un assemblage : chaque ingrédient doit être rassemblé, et
     * il en faut le nombre demandé hors de tout autre assemblage, donc en plus de ce que les
     * assemblages déjà formés (et ceux déjà comptés dans cette liste) ont pris.
     */
    private void wantAssembly(Assembly assembly, Map<String, int[]> wanted, Map<String, String> purposes) {
        for (Map.Entry<String, Integer> ingredient : assembly.ingredients().entrySet()) {
            String id = ingredient.getKey();
            int[] want = wanted.computeIfAbsent(id, each -> new int[] {assembledMolecules(each), 0});
            want[0] = Math.max(want[0], assembledMolecules(id)) + ingredient.getValue();
            want[0] = Math.max(want[0], SUBSTANCE_MOLECULES);
            want[1] = 1;
            purposes.merge(id, assembly.name(), (before, name) -> before.isEmpty() || before.contains(name) ? name : before + ", " + name);
        }
    }

    private List<ShoppingList.Item> shoppingItems(Map<String, int[]> wanted, Map<String, String> purposes) {
        List<ShoppingList.Item> items = new ArrayList<>();
        for (Map.Entry<String, int[]> want : wanted.entrySet()) {
            Molecule molecule = molecules.get(want.getKey());
            if (molecule == null) continue;
            int owned = state.moleculeCount(molecule.id());
            boolean gather = want.getValue()[1] == 1 && !state.hasSubstance(molecule.id());
            if (owned >= want.getValue()[0] && !gather) continue;
            items.add(new ShoppingList.Item(molecule, owned, Math.max(owned, want.getValue()[0]), gather,
                    purposes.getOrDefault(molecule.id(), "")));
        }
        return items;
    }

    /**
     * Acquiert une amélioration d'espace dont le seuil est atteint. Le jeu le fait de lui-même à
     * chaque {@link #tick(double)} : cette méthode ne sert qu'à l'acquérir sans attendre. Rien n'est
     * dépensé : c'est l'espace gagné qui l'ouvre, et il reste entièrement disponible pour les
     * molécules et leurs rassemblements. Elle est définitive, ni l'explosion ni le Big Bang ne la
     * reprennent.
     *
     * @return {@code true} si l'amélioration vient d'être acquise
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
        noteStep(GameStats.Step.FIRST_GATHERING);
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
        noteStep(GameStats.Step.FIRST_ASSEMBLY);
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
    public long gatheredInState(Molecule.State matter) {
        if (gatheredByStateVersion != state.moleculesVersion()) {
            gatheredByStateVersion = state.moleculesVersion();
            gatheredByState.clear();
            for (String id : state.substances()) {
                Molecule molecule = molecules.get(id);
                if (molecule != null && molecule.hasState()) gatheredByState.merge(molecule.state(), (long) state.moleculeCount(id), Long::sum);
            }
        }
        return gatheredByState.getOrDefault(matter, 0L);
    }

    /** Nombre de conditions de cet astre déjà réunies, sur {@link #bodyConditions(String)}. */
    public int bodyConditionsMet(String bodyId) {
        Body body = body(bodyId);
        int met = 0;
        for (String needed : body.bodies()) {
            if (isBodyMature(needed)) met++;
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
        if (needsColliderEnergy(body) && hasColliderEnergyFor(body)) met++;
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
                && bodyConditionsMet(bodyId) == bodyConditions(bodyId) && hasMaturationPlace()
                && (body.tier() != Body.Tier.EXOTIC || isExoticUnlocked());
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
        state.setMaturing(body(bodyId).id(), maturationSeconds(body(bodyId)));
        noteStep(GameStats.Step.FIRST_BODY);
        return true;
    }

    // ------------------------------------------------------------------
    // La galaxie : tous les astres, rassemblés autour du trou noir supermassif
    // ------------------------------------------------------------------

    /** Ce que la galaxie ajoute à chacune des grandeurs une fois formée (1 024 = +102 400 %) : le double du trou noir supermassif. */
    public static final double GALAXY_FACTOR = Cosmos.GALAXY.factor();

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
        return state.started() && isBodiesUnlocked() && !state.hasGalaxy() && bodiesMature() == bodies.size()
                && solarSystemsComplete() >= SYSTEMS_FOR_GALAXY;
    }

    /**
     * Forme la galaxie : tout ce que le joueur a créé, molécules, amas, assemblages et astres, se
     * met à tourner autour du trou noir supermassif. C'est un achat unique, qui ne consomme rien :
     * les astres restent formés et gardent leur bonus. La galaxie multiplie par {@link #GALAXY_FACTOR}
     * chacune des grandeurs que les molécules augmentent, et rien ne la défait, ni l'explosion ni
     * le Big Bang.
     *
     * @return {@code true} si la galaxie a été formée
     */
    public boolean formGalaxy() {
        return formCosmos(Cosmos.GALAXY);
    }

    // ------------------------------------------------------------------
    // Le cosmos : la galaxie, l'amas de galaxies, l'univers
    // ------------------------------------------------------------------

    // ------------------------------------------------------------------
    // La partie secondaire (version 0.81) : les univers, sans logique pour l'instant
    // ------------------------------------------------------------------

    /** Nombre de dimensions à ouvrir, une par univers jusqu'au dixième. */
    public static final int MAX_DIMENSIONS = 10;

    /** Univers créés : 0 avant l'univers, 1 une fois formé, plus ceux d'après (créés pour l'instant par les outils d'essai). */
    public int universes() {
        return isUniverseFormed() ? 1 + state.extraUniverses() : 0;
    }

    /** Vrai une fois un univers formé, même pendant la partie qui mène au suivant (version 0.87). */
    public boolean isUniverseFormed() {
        return state.universeFormed() || state.cosmosLevel() >= Cosmos.values().length;
    }

    /** Vrai à partir du deuxième univers : la sous-page des dimensions s'ouvre. */
    public boolean isDimensionsUnlocked() {
        return universes() >= 2;
    }

    /** Dimensions ouvertes : les deux premières dès le deuxième univers, puis une par univers, dix au plus. */
    public int dimensionsUnlocked() {
        return isDimensionsUnlocked() ? Math.min(MAX_DIMENSIONS, universes()) : 0;
    }

    /** Les améliorations de l'univers, dans l'ordre. */
    public List<idle.core.universe.UniverseUpgrade> universeUpgrades() {
        return List.of(idle.core.universe.UniverseUpgrade.values());
    }

    /** Niveau d'une amélioration de l'univers. */
    public int universeUpgradeLevel(idle.core.universe.UniverseUpgrade upgrade) {
        return state.universeUpgradeLevel(upgrade.id());
    }

    /** Outil d'essai : un univers de plus, une fois le premier formé. Faux s'il n'y en a pas encore. */
    public boolean testAddUniverse() {
        if (!isUniverseFormed()) return false;
        state.setExtraUniverses(state.extraUniverses() + 1);
        return true;
    }

    /** Outil d'essai : un univers de moins, sans descendre sous le premier. */
    public boolean testRemoveUniverse() {
        if (state.extraUniverses() <= 0) return false;
        state.setExtraUniverses(state.extraUniverses() - 1);
        return true;
    }

    /** Outil d'essai : des graines d'univers en plus. */
    public void testAddUniverseSeeds(BigNum amount) {
        state.setUniverseSeeds(state.universeSeeds().add(amount));
    }

    /** Outil d'essai : de l'énergie du vide en plus. */
    public void testAddVoidEnergy(BigNum amount) {
        state.setVoidEnergy(state.voidEnergy().add(amount));
    }

    /** Outil d'essai : un niveau de plus à une amélioration de l'univers, sans coût ni effet. */
    public void testRaiseUniverseUpgrade(idle.core.universe.UniverseUpgrade upgrade) {
        state.setUniverseUpgradeLevel(upgrade.id(), state.universeUpgradeLevel(upgrade.id()) + 1);
    }

    /** Outil d'essai : la partie secondaire revient à son début (un seul univers, rien en réserve). */
    public void testResetUniverses() {
        state.setExtraUniverses(0);
        state.setUniverseSeeds(BigNum.ZERO);
        state.setVoidEnergy(BigNum.ZERO);
        for (idle.core.universe.UniverseUpgrade upgrade : idle.core.universe.UniverseUpgrade.values()) {
            state.setUniverseUpgradeLevel(upgrade.id(), 0);
        }
    }

    /** Vrai si cette échelle du cosmos est formée. */
    public boolean hasCosmos(Cosmos scale) {
        // L'univers reste formé après la remise à zéro qui l'accompagne (version 0.83), quand le cosmos repart de rien.
        // Pendant la partie du deuxième univers (version 0.87), seul compte ce que cette partie a formé.
        if (scale == Cosmos.UNIVERSE && state.universeFormed() && !state.replaying()) return true;
        return state.cosmosLevel() > scale.ordinal();
    }

    /** Nombre d'échelles du cosmos formées : 0 avant la galaxie, 3 une fois l'univers formé. */
    public int cosmosFormed() {
        return state.cosmosLevel();
    }

    /** La prochaine échelle du cosmos à former, ou {@code null} une fois l'univers formé. */
    public Cosmos nextCosmos() {
        return state.cosmosLevel() >= Cosmos.values().length ? null : Cosmos.values()[state.cosmosLevel()];
    }

    /**
     * Vrai une fois cette échelle en vue : la galaxie à la première étoile ({@link #isGalaxyUnlocked()}),
     * les suivantes dès que la précédente est formée.
     */
    public boolean isCosmosUnlocked(Cosmos scale) {
        return scale == Cosmos.GALAXY ? isGalaxyUnlocked() : hasCosmos(scale.previous());
    }

    /**
     * Vrai si cette échelle peut être formée maintenant : la précédente l'est, elle ne l'est pas
     * encore, et ce qu'elle demande est réuni. La galaxie demande tous les astres
     * ({@link #canFormGalaxy()}) ; les suivantes, assez de matière rassemblée dans chaque état
     * ({@link Cosmos#matter()}).
     */
    public boolean canFormCosmos(Cosmos scale) {
        if (scale == Cosmos.GALAXY) return canFormGalaxy();
        if (!state.started() || hasCosmos(scale) || !hasCosmos(scale.previous())) return false;
        if (cosmosSettleRemaining(scale) > 0) return false;
        for (Map.Entry<Molecule.State, Integer> need : scale.matter().entrySet()) {
            if (gatheredInState(need.getKey()) < need.getValue()) return false;
        }
        for (Map.Entry<Molecule.State, Integer> need : scale.sorts().entrySet()) {
            if (sortsInState(need.getKey()) < need.getValue()) return false;
        }
        return true;
    }

    /**
     * Temps de jeu qu'il faut laisser à l'échelle d'avant pour se rassembler avant de former l'amas,
     * puis l'univers (version 0.73) : 45 minutes chacun. Un joueur qui réunit la matière plus vite
     * attend ; l'infatigable, qui met déjà près d'une heure à chaque échelle, ne le voit pas.
     */
    public static final double COSMOS_SETTLE_SECONDS = 45 * 60;

    /** Secondes de jeu qu'il reste avant que cette échelle puisse se former, 0 si le délai est passé. */
    public double cosmosSettleRemaining(Cosmos scale) {
        if (scale == Cosmos.GALAXY || !hasCosmos(scale.previous())) return 0;
        double since = state.stats().reachedAt(scale.previous() == Cosmos.GALAXY ? GameStats.Step.GALAXY : GameStats.Step.CLUSTER);
        if (Double.isNaN(since)) return 0;
        return Math.max(0, since + COSMOS_SETTLE_SECONDS - state.timePlayed());
    }

    private final Map<Molecule.State, Integer> sortsByState = new java.util.EnumMap<>(Molecule.State.class);
    private int sortsByStateVersion = -1;

    /** Nombre de sortes de molécules rassemblées dans cet état : la variété que demandent l'amas de galaxies et l'univers ({@link Cosmos#sorts()}). */
    public int sortsInState(Molecule.State matter) {
        if (sortsByStateVersion != state.moleculesVersion()) {
            sortsByStateVersion = state.moleculesVersion();
            sortsByState.clear();
            for (String id : state.substances()) {
                Molecule molecule = molecules.get(id);
                if (molecule != null && molecule.hasState()) sortsByState.merge(molecule.state(), 1, Integer::sum);
            }
        }
        return sortsByState.getOrDefault(matter, 0);
    }

    /** Nombre de conditions de cette échelle déjà réunies, sur {@link #cosmosConditions(Cosmos)}. */
    public int cosmosConditionsMet(Cosmos scale) {
        if (scale == Cosmos.GALAXY) {
            return hasGalaxy() ? bodies.size() + 1 : bodiesMature() + (solarSystemsComplete() >= SYSTEMS_FOR_GALAXY ? 1 : 0);
        }
        int met = hasCosmos(scale.previous()) ? 1 : 0;
        for (Map.Entry<Molecule.State, Integer> need : scale.matter().entrySet()) {
            if (gatheredInState(need.getKey()) >= need.getValue()) met++;
        }
        for (Map.Entry<Molecule.State, Integer> need : scale.sorts().entrySet()) {
            if (sortsInState(need.getKey()) >= need.getValue()) met++;
        }
        return met;
    }

    /** Nombre de conditions de cette échelle : les astres du catalogue pour la galaxie ; sinon l'échelle d'avant, plus un état de la matière par ligne. */
    public int cosmosConditions(Cosmos scale) {
        return scale == Cosmos.GALAXY ? bodies.size() + 1 : 1 + scale.matter().size() + scale.sorts().size();
    }

    /**
     * Forme cette échelle du cosmos. C'est un achat unique, qui ne consomme rien : la matière reste
     * rassemblée, les astres restent formés. Elle multiplie par {@link Cosmos#factor()} chacune des
     * grandeurs que la matière augmente, et rien ne la défait, ni l'explosion ni le Big Bang.
     *
     * @return {@code true} si elle a été formée
     */
    public boolean formCosmos(Cosmos scale) {
        if (!canFormCosmos(scale)) return false;
        state.setCosmosLevel(scale.ordinal() + 1);
        noteStep(switch (scale) {
            case GALAXY -> GameStats.Step.GALAXY;
            case CLUSTER -> GameStats.Step.CLUSTER;
            case UNIVERSE -> GameStats.Step.UNIVERSE;
        });
        if (scale == Cosmos.UNIVERSE) formUniverse();
        return true;
    }

    // ------------------------------------------------------------------
    // Le deuxième univers (version 0.87)
    // ------------------------------------------------------------------

    /** Fusions du deuxième univers qu'il faut pour ouvrir l'onglet Skins. */
    public static final int SKINS_FUSIONS = 5;
    /** Générateurs qu'il faut pour le régime des générateurs. */
    public static final int REGIME_GENERATORS = 3;
    /** Ce que le régime rapide ajoute à la cadence des générateurs, et combien de temps. */
    public static final double REGIME_GAIN = 0.15;
    public static final double REGIME_SECONDS = 20;
    /** Le repos qui suit le régime rapide, avant de pouvoir le relancer. */
    public static final double REGIME_REST_SECONDS = 40;
    /** Générateurs qu'il faut pour la synergie par rang. */
    public static final int SYNERGY_GENERATORS = 10;
    /** Ce que la synergie ajoute, au plus, à chaque générateur : +0,5 %. */
    public static final double SYNERGY_MAX = 0.005;
    /** Fusions de suite sans le plafond d'atomes pour la prime de cycle parfait, sa durée et son gain. */
    public static final int PERFECT_FUSIONS = 3;
    public static final double PERFECT_SECONDS = 60;
    public static final double PERFECT_GAIN = 0.10;
    /** Ce que chaque tirage ajoute à la famille préférée, et le nombre de tirages qui comptent au plus (×2). */
    public static final double PREFERENCE_STEP = 0.05;
    public static final int PREFERENCE_MAX_DRAWS = 20;
    /** Un élément gratuit à choisir tous les tant d'éléments découverts. */
    public static final int FREE_ELEMENT_EVERY = 10;

    /**
     * Vrai dans le deuxième univers et au-delà : un univers est déjà formé. C'est là que s'ouvre
     * tout ce que la liste des idées range sous « tout se débloque lors du deuxième univers ».
     */
    public boolean isSecondUniverse() {
        return state.universeFormed();
    }

    /** Vrai pendant la partie qui mène à l'univers suivant : les anciens onglets et leurs paliers sont revenus. */
    public boolean isReplaying() {
        return state.universeFormed() && state.replaying();
    }

    /** Vrai si l'on peut rouvrir les paliers pour jouer l'univers suivant. */
    public boolean canStartNextUniverse() {
        return state.universeFormed() && !state.replaying();
    }

    /**
     * Rouvre les paliers (version 0.87) : les anciens onglets reviennent, pour une partie neuve, celle
     * de l'univers suivant. La partie a déjà été remise à zéro par la formation de l'univers. Former
     * l'univers à la fin de cette partie crée l'univers suivant, avec une remise à zéro et une graine de plus.
     */
    public boolean startNextUniverse() {
        if (!canStartNextUniverse()) return false;
        state.setReplaying(true);
        return true;
    }

    /** Fusions faites depuis que le dernier univers s'est formé. */
    public long fusionsSinceUniverse() {
        return Math.max(0, state.stats().fusions() - state.fusionsAtUniverse());
    }

    /** Vrai une fois l'onglet Skins ouvert : cinq fusions dans le deuxième univers. */
    public boolean isSkinsUnlocked() {
        return state.skinsOpen();
    }

    /** Vrai si ce skin est débloqué. */
    public boolean hasSkin(idle.core.universe.Skin skin) {
        return state.unlockedSkins().contains(skin.id());
    }

    /** Nombre de skins débloqués. */
    public int skinsUnlocked() {
        int count = 0;
        for (idle.core.universe.Skin skin : idle.core.universe.Skin.values()) if (hasSkin(skin)) count++;
        return count;
    }

    /** Débloque un skin ; vrai s'il ne l'était pas. */
    public boolean unlockSkin(idle.core.universe.Skin skin) {
        if (hasSkin(skin)) return false;
        state.unlockSkin(skin.id());
        moleculeBonusesVersion = -1;
        return true;
    }

    /** Le skin choisi pour ce visuel, ou {@code null} pour l'apparence classique. */
    public idle.core.universe.Skin chosenSkin(idle.core.universe.SkinVisual visual) {
        idle.core.universe.Skin skin = idle.core.universe.Skin.byId(state.chosenSkins().get(visual.name()));
        return skin != null && skin.visual() == visual && hasSkin(skin) ? skin : null;
    }

    /** Choisit un skin débloqué pour son visuel ; {@code null} revient à l'apparence classique de ce visuel. */
    public boolean chooseSkin(idle.core.universe.SkinVisual visual, idle.core.universe.Skin skin) {
        if (skin != null && (skin.visual() != visual || !hasSkin(skin))) return false;
        state.chooseSkin(visual.name(), skin == null ? null : skin.id());
        return true;
    }

    /** Ce que les skins débloqués multiplient, sur toutes les ressources d'en dessous de l'univers. */
    public double skinBoost() {
        return 1 + idle.core.universe.Skin.BONUS * skinsUnlocked();
    }

    public boolean isRegimeUnlocked() {
        return state.regimeOpen();
    }

    /** Vrai si le régime rapide peut être lancé : ouvert, ni en cours ni au repos. */
    public boolean canStartRegime() {
        return isRegimeUnlocked() && state.regimeLeft() <= 0 && state.regimeRest() <= 0;
    }

    /** Lance le régime rapide : +15 % de cadence pendant 20 s, puis 40 s de repos. Un geste à la main, jamais automatique. */
    public boolean startRegime() {
        if (!canStartRegime()) return false;
        state.setRegimeLeft(REGIME_SECONDS);
        return true;
    }

    /** La cadence du régime rapide : ×1,15 pendant qu'il dure. */
    public double regimeMultiplier() {
        return state.regimeLeft() > 0 ? 1 + REGIME_GAIN : 1;
    }

    public boolean isSynergyUnlocked() {
        return state.synergyOpen();
    }

    /** La synergie par rang : +0,5 % au plus à chaque générateur, selon les autres rangs possédés dans le cycle. */
    public double synergyMultiplier() {
        if (!isSynergyUnlocked()) return 1;
        int others = Math.max(0, generatorCount() - 1);
        return 1 + SYNERGY_MAX * Math.min(1, others / (double) (SYNERGY_GENERATORS - 1));
    }

    /** Vrai une fois la prime de cycle parfait ouverte : Persistance achetée, dans le deuxième univers. */
    public boolean isPerfectCycleUnlocked() {
        return isSecondUniverse() && keepsUpgradesOnFusion();
    }

    /** Ce que la prime de cycle parfait multiplie dans les particules, pendant qu'elle dure : elle ne se cumule pas. */
    public double perfectCycleMultiplier() {
        return state.perfectLeft() > 0 ? 1 + PERFECT_GAIN : 1;
    }

    private void notePerfectCycle(boolean capped) {
        if (!isPerfectCycleUnlocked()) return;
        if (capped) {
            state.setPerfectStreak(0);
            return;
        }
        state.setPerfectStreak(state.perfectStreak() + 1);
        if (state.perfectStreak() >= PERFECT_FUSIONS) {
            state.setPerfectStreak(0);
            state.setPerfectLeft(PERFECT_SECONDS);
        }
    }

    public boolean isCycleChoiceUnlocked() {
        return isSecondUniverse() && state.bigBangs() > 0;
    }

    /** Le choix du petit arbre pour ce cycle, ou {@code null}. */
    public idle.core.universe.CycleChoice cycleChoice() {
        if (!isCycleChoiceUnlocked() || state.cycleChoice() == null) return null;
        try {
            return idle.core.universe.CycleChoice.valueOf(state.cycleChoice());
        } catch (IllegalArgumentException unknown) {
            return null;
        }
    }

    /** Fait le choix du cycle : un seul, jusqu'au prochain Big Bang. */
    public boolean chooseCycle(idle.core.universe.CycleChoice choice) {
        if (!isCycleChoiceUnlocked() || choice == null || cycleChoice() != null) return false;
        state.setCycleChoice(choice.name());
        return true;
    }

    public boolean isFamilyPreferenceUnlocked() {
        return isSecondUniverse() && isTargetedSynthesisUnlocked();
    }

    /** La famille préférée, ou {@code null}. */
    public ElementCategory preferredFamily() {
        if (!isFamilyPreferenceUnlocked() || state.preferredFamily() == null) return null;
        try {
            return ElementCategory.valueOf(state.preferredFamily());
        } catch (IllegalArgumentException unknown) {
            return null;
        }
    }

    /** Choisit la famille préférée ({@code null} : aucune) ; la préférence repart de zéro. */
    public boolean setPreferredFamily(ElementCategory family) {
        if (!isFamilyPreferenceUnlocked()) return false;
        state.setPreferredFamily(family == null ? null : family.name());
        state.setPreferenceDraws(0);
        return true;
    }

    /** Ce par quoi la préférence multiplie les chances de sa famille : +5 % par tirage au hasard, ×2 au plus. */
    public double preferenceMultiplier(ElementCategory category) {
        ElementCategory preferred = preferredFamily();
        if (preferred == null || preferred != category) return 1;
        return 1 + PREFERENCE_STEP * Math.min(PREFERENCE_MAX_DRAWS, state.preferenceDraws());
    }

    /** Éléments gratuits à choisir maintenant : un tous les dix éléments découverts, dans le deuxième univers. */
    public int freeElementsAvailable() {
        if (!isSecondUniverse()) return 0;
        return Math.max(0, discoveredElements() / FREE_ELEMENT_EVERY - state.freeElementsUsed());
    }

    /** Prend gratuitement un élément pas encore découvert. */
    public boolean claimFreeElement(int atomicNumber) {
        if (freeElementsAvailable() <= 0 || elementCount(atomicNumber) > 0) return false;
        state.setElementCount(PeriodicTable.element(atomicNumber).number(), 1);
        state.setFreeElementsUsed(state.freeElementsUsed() + 1);
        return true;
    }

    /** Outil d'essai : forme l'univers sur-le-champ, comme si ses conditions étaient réunies (remise à zéro comprise). */
    public void testFormUniverse() {
        state.setCosmosLevel(Cosmos.values().length);
        noteStep(GameStats.Step.UNIVERSE);
        formUniverse();
    }

    /** Outil d'essai : compte des fusions de plus dans les statistiques, sans rien changer d'autre. */
    public void testAddFusions(int count) {
        for (int i = 0; i < count; i++) state.stats().noteFusion(BigNum.ONE, 1);
    }

    /** Outil d'essai : débloque tous les skins, ou les reverrouille tous. */
    public void testAllSkins(boolean unlocked) {
        for (idle.core.universe.Skin skin : idle.core.universe.Skin.values()) {
            if (unlocked) state.unlockSkin(skin.id());
            else state.lockSkin(skin.id());
        }
        moleculeBonusesVersion = -1;
    }

    /** Ce qui s'ouvre et s'écoule dans le deuxième univers, à chaque instant. */
    private void runSecondUniverse(double dt) {
        if (!isSecondUniverse()) return;
        if (!state.skinsOpen() && fusionsSinceUniverse() >= SKINS_FUSIONS) state.setSkinsOpen(true);
        if (!state.regimeOpen() && generatorCount() >= REGIME_GENERATORS) state.setRegimeOpen(true);
        if (!state.synergyOpen() && generatorCount() >= SYNERGY_GENERATORS) state.setSynergyOpen(true);
        if (dt <= 0) return;
        if (state.regimeLeft() > 0) {
            double left = state.regimeLeft() - dt;
            state.setRegimeLeft(left);
            if (left <= 0) state.setRegimeRest(REGIME_REST_SECONDS + Math.min(0, left));
        } else if (state.regimeRest() > 0) {
            state.setRegimeRest(state.regimeRest() - dt);
        }
        if (state.perfectLeft() > 0) state.setPerfectLeft(state.perfectLeft() - dt);
        noteSkinUnlocks();
        runTrial(dt);
    }

    // ------------------------------------------------------------------
    // Les skins à obtenir (version 0.88)
    // ------------------------------------------------------------------

    /** Le record de reconstruction qui donne le skin « Éclair » : de la fusion au dixième générateur en une minute au plus. */
    public static final double REBUILD_FAST_SECONDS = 60;
    /** Le prix de la recherche « Fusion prismatique », en particules. */
    public static final BigNum PRISM_COST = BigNum.of(1, 50);
    /** Atomes créés qu'il faut pour la recherche sur le noyau dense, et le prix de ses quatre paliers, en atomes. */
    public static final BigNum DENSE_UNLOCK_ATOMS = BigNum.of(1, 6);
    public static final BigNum[] DENSE_COSTS = {BigNum.of(100), BigNum.of(1, 3), BigNum.of(1, 5), BigNum.of(1, 8)};
    private static final idle.core.universe.Skin[] DENSE_SKINS = {idle.core.universe.Skin.ATOM_DENSE, idle.core.universe.Skin.TABLE_LEAD,
            idle.core.universe.Skin.ATOM_STELLAR, idle.core.universe.Skin.TABLE_GOLD};
    /** Les paliers de la matière noire (gagnée dans le deuxième univers) et leurs skins. */
    public static final BigNum[] DARK_TIERS = {BigNum.of(100), BigNum.of(1, 3), BigNum.of(1, 4), BigNum.of(1, 5)};
    private static final idle.core.universe.Skin[] DARK_SKINS = {idle.core.universe.Skin.DARK_OBSIDIAN, idle.core.universe.Skin.DARK_VORTEX,
            idle.core.universe.Skin.DARK_NEBULA, idle.core.universe.Skin.DARK_PULSAR};
    /** Les paliers du Big Bang (Big Bangs du deuxième univers) et leurs skins. */
    public static final int[] BANG_TIERS = {1, 3};
    private static final idle.core.universe.Skin[] BANG_SKINS = {idle.core.universe.Skin.BANG_AURORA, idle.core.universe.Skin.BANG_SUPERNOVA};

    /** Les paliers de la matière noire atteints, de 0 à 4. */
    public int darkSkinTiers() {
        int tiers = 0;
        for (BigNum tier : DARK_TIERS) if (darkMatterEarned().gte(tier)) tiers++;
        return tiers;
    }

    /** Les paliers du Big Bang atteints, de 0 à 2. */
    public int bangSkinTiers() {
        int tiers = 0;
        for (int tier : BANG_TIERS) if (state.bigBangs() >= tier) tiers++;
        return tiers;
    }

    /** Les skins des paliers et du record se débloquent seuls, dès que l'onglet Skins est ouvert. */
    private void noteSkinUnlocks() {
        if (!isSkinsUnlocked()) return;
        for (int i = 0; i < darkSkinTiers(); i++) unlockSkin(DARK_SKINS[i]);
        for (int i = 0; i < bangSkinTiers(); i++) unlockSkin(BANG_SKINS[i]);
        if (state.rebuildPending() && generatorCount() >= generatorsPerAtom()) {
            double time = state.timeSinceFusion();
            state.setRebuildPending(false);
            if (state.rebuildRecord() <= 0 || time < state.rebuildRecord()) state.setRebuildRecord(time);
            if (time <= REBUILD_FAST_SECONDS) unlockSkin(idle.core.universe.Skin.FUSION_FLASH);
        }
    }

    /** Le record de reconstruction, en secondes ; 0 sans record. */
    public double rebuildRecord() {
        return state.rebuildRecord();
    }

    /** Vrai si la recherche « Fusion prismatique » est faite. */
    public boolean hasPrismResearch() {
        return state.researches().contains("prism");
    }

    public boolean canBuyPrismResearch() {
        return isSkinsUnlocked() && !hasPrismResearch() && state.particles().gte(PRISM_COST);
    }

    /** La recherche facultative en particules : elle donne le skin « Prisme » de la fusion. */
    public boolean buyPrismResearch() {
        if (!canBuyPrismResearch()) return false;
        state.setParticles(state.particles().subtract(PRISM_COST).max(BigNum.ZERO));
        state.stats().addParticlesSpent(PRISM_COST);
        state.addResearch("prism");
        unlockSkin(idle.core.universe.Skin.FUSION_PRISM);
        return true;
    }

    /** Vrai une fois la recherche sur le noyau dense ouverte : un million d'atomes créés, dans le deuxième univers. */
    public boolean isDenseResearchUnlocked() {
        return isSkinsUnlocked() && state.stats().atomsCreated().gte(DENSE_UNLOCK_ATOMS);
    }

    /** Paliers de la recherche sur le noyau dense déjà faits, de 0 à 4. */
    public int denseTiers() {
        int tiers = 0;
        while (tiers < DENSE_COSTS.length && state.researches().contains("dense" + (tiers + 1))) tiers++;
        return tiers;
    }

    public boolean canBuyDenseTier() {
        int next = denseTiers();
        return isDenseResearchUnlocked() && next < DENSE_COSTS.length && state.atoms().gte(DENSE_COSTS[next]);
    }

    /** Le palier suivant de la recherche sur le noyau dense : un skin d'atome ou de tableau à chaque palier. */
    public boolean buyDenseTier() {
        if (!canBuyDenseTier()) return false;
        int next = denseTiers();
        state.setAtoms(state.atoms().subtract(DENSE_COSTS[next]).max(BigNum.ZERO));
        state.stats().addAtomsSpent(DENSE_COSTS[next]);
        state.addResearch("dense" + (next + 1));
        unlockSkin(DENSE_SKINS[next]);
        return true;
    }

    /** L'épreuve en cours, ou {@code null}. */
    public idle.core.universe.Trial activeTrial() {
        if (state.activeTrial() == null) return null;
        try {
            return idle.core.universe.Trial.valueOf(state.activeTrial());
        } catch (IllegalArgumentException unknown) {
            return null;
        }
    }

    /** Vrai pendant cette épreuve. */
    public boolean inTrial(idle.core.universe.Trial trial) {
        return trial.name().equals(state.activeTrial());
    }

    /** Vrai si cette épreuve a déjà été réussie. */
    public boolean isTrialCompleted(idle.core.universe.Trial trial) {
        return state.completedTrials().contains(trial.name());
    }

    /** Vrai si cette épreuve est ouverte : l'onglet Skins, et ce qu'elle demande. */
    public boolean isTrialUnlocked(idle.core.universe.Trial trial) {
        if (!isSkinsUnlocked()) return false;
        return switch (trial) {
            case LONE_GENERATOR -> state.explosions() > 0;
            case SOFT_CONTRACTION -> isDarkMatterUnlocked();
            case COLD_BANG -> state.bigBangs() > 0;
            case DETECTOR -> isColliderUnlocked() && colliderUpgradeLevel(ColliderUpgrade.WINDOW) >= 3;
            case NO_OVERHEAT, FULL_CHARGE, LONE_COLLIDER -> isColliderUnlocked();
        };
    }

    /** Vrai si l'épreuve peut commencer : ouverte, aucune autre en cours, et pour le premier générateur, un seul générateur. */
    public boolean canStartTrial(idle.core.universe.Trial trial) {
        if (!isTrialUnlocked(trial) || activeTrial() != null) return false;
        return trial != idle.core.universe.Trial.LONE_GENERATOR || generatorCount() == 1;
    }

    /** Commence une épreuve. */
    public boolean startTrial(idle.core.universe.Trial trial) {
        if (!canStartTrial(trial)) return false;
        state.setActiveTrial(trial.name());
        state.setTrialStart(state.timePlayed());
        state.setTrialCount(0);
        state.setTrialBase(trial == idle.core.universe.Trial.FULL_CHARGE
                ? colliderEnergyProduced() + colliderEnergyPerSecond() * idle.core.universe.Trial.FULL_CHARGE_SECONDS : 0);
        return true;
    }

    /** Abandonne l'épreuve en cours : rien n'est perdu, rien n'est gagné. */
    public boolean abandonTrial() {
        if (activeTrial() == null) return false;
        state.setActiveTrial(null);
        return true;
    }

    /** Secondes qu'il reste à l'épreuve en cours ; l'infini sans limite, 0 sans épreuve. */
    public double trialLeft() {
        idle.core.universe.Trial trial = activeTrial();
        if (trial == null) return 0;
        if (trial.seconds() <= 0) return Double.POSITIVE_INFINITY;
        return Math.max(0, trial.seconds() - (state.timePlayed() - state.trialStart()));
    }

    /** Où en est l'épreuve en cours, de 0 à 1. */
    public double trialProgress() {
        idle.core.universe.Trial trial = activeTrial();
        if (trial == null) return 0;
        return switch (trial) {
            case LONE_GENERATOR -> Math.min(1, Math.max(0, state.particles().log10() / idle.core.universe.Trial.LONE_GOAL_LOG10));
            case SOFT_CONTRACTION, COLD_BANG -> 0;
            case FULL_CHARGE -> state.trialBase() <= 0 ? 0 : Math.min(1, colliderEnergyProduced() / state.trialBase());
            default -> Math.min(1, state.trialCount() / (double) trial.goal());
        };
    }

    /** Ce que la contraction douce ajoute aux atomes de chaque fusion : +5 % par fusion du cycle, ×2 au plus. */
    public double contractionAtomsMultiplier() {
        if (!inTrial(idle.core.universe.Trial.SOFT_CONTRACTION)) return 1;
        return Math.min(idle.core.universe.Trial.CONTRACTION_MAX, 1 + idle.core.universe.Trial.CONTRACTION_FUSION_GAIN * state.trialCount());
    }

    private void completeTrial() {
        idle.core.universe.Trial trial = activeTrial();
        if (trial == null) return;
        state.setActiveTrial(null);
        state.addCompletedTrial(trial.name());
        unlockSkin(trial.skin());
    }

    private void failTrial() {
        state.setActiveTrial(null);
    }

    /** L'épreuve en cours : sa contrainte au fil du temps, son but, sa limite. */
    private void runTrial(double dt) {
        idle.core.universe.Trial trial = activeTrial();
        if (trial == null) return;
        if (trial == idle.core.universe.Trial.SOFT_CONTRACTION && dt > 0) {
            state.setParticles(state.particles().multiply(Math.pow(1 - idle.core.universe.Trial.CONTRACTION_LOSS, dt / idle.core.universe.Trial.CONTRACTION_EVERY)));
        }
        if (trial == idle.core.universe.Trial.NO_OVERHEAT && state.overheatLeft() > 0) {
            failTrial();
            return;
        }
        boolean done = switch (trial) {
            case LONE_GENERATOR -> state.particles().log10() >= idle.core.universe.Trial.LONE_GOAL_LOG10;
            case SOFT_CONTRACTION, COLD_BANG -> false;
            case FULL_CHARGE -> state.trialBase() > 0 && colliderEnergyProduced() >= state.trialBase();
            default -> state.trialCount() >= trial.goal();
        };
        if (done) {
            completeTrial();
            return;
        }
        if (trial.seconds() > 0 && state.timePlayed() - state.trialStart() >= trial.seconds()) failTrial();
    }

    /** Graines d'univers que donne la formation de l'univers (version 0.83). */
    public static final int SEEDS_PER_UNIVERSE = 1;

    /**
     * L'univers se forme (version 0.83) : tout ce qui y a mené est remis à zéro
     * ({@link GameState#startUniverse()}), et le joueur reçoit {@link #SEEDS_PER_UNIVERSE} graine
     * d'univers. Les succès gagnés par la formation sont relevés avant.
     */
    private void formUniverse() {
        checkAchievements();
        resetForUniverse();
        forgetDerived();
    }

    /** La remise à zéro de l'univers, son bilan relevé avant, et sa graine. */
    private void resetForUniverse() {
        // Un univers de plus (version 0.87) : celui-ci était déjà formé, la partie rejouée en forme un autre.
        boolean another = state.universeFormed();
        state.stats().noteUniverseTally(state.bigBangs(), moleculesCreated(), state.collisionRuns(), state.cometsCaught());
        state.startUniverse();
        if (another) state.setExtraUniverses(state.extraUniverses() + 1);
        state.setUniverseSeeds(state.universeSeeds().add(BigNum.of(SEEDS_PER_UNIVERSE)));
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
        return steadySpacePerSecond().multiply(cometBoost());
    }

    /** L'espace par seconde sans le sillage d'une comète : ce que l'expansion donne en régime ordinaire. */
    public BigNum steadySpacePerSecond() {
        if (!isBigBangUnlocked()) return BigNum.ZERO;
        return BigNum.of(SPACE_PER_SECOND * state.bigBangs() * moleculeBoost(Molecule.Stat.SPACE) * darkMatterSpaceBoost()
                * (inTrial(idle.core.universe.Trial.COLD_BANG) ? idle.core.universe.Trial.COLD_EXPANSION : 1)
                * bangReward(BangChallenge.Reward.SPACE) * colliderSpaceMultiplier() * probeBoostMultiplier()
                * bangAchievementMultiplier(BangAchievement.Reward.SPACE));
    }

    /**
     * Fait passer le temps de l'expansion : l'espace grandit de {@link #spacePerSecond()} par
     * seconde de jeu, quoi que fasse le joueur. Rien ne le reprend, ni l'explosion ni le Big Bang ;
     * les molécules en occupent une part ({@link #occupiedSpace()}), leurs lieux de rassemblement en réservent une autre
     * ({@link #reservedSpace()}).
     */
    private void expandSpace(double dt) {
        if (dt <= 0 || !state.started() || !isBigBangUnlocked()) return;
        // Le sillage d'une comète ne compte que pour le temps qu'il lui reste, même si le pas de temps est plus long.
        double boosted = Math.min(dt, state.cometBoost());
        state.setSpace(state.space().add(steadySpacePerSecond().multiply(dt + boosted * (COMET_BOOST - 1))));
    }

    // ------------------------------------------------------------------
    // Le collisionneur de quarks (version 0.50, refait en 0.59) : un jeu en parallèle, dont rien
    // ne se perd ni à l'explosion ni au Big Bang ; seule la remise à zéro le reprend
    // ------------------------------------------------------------------

    /** Période des faisceaux, en secondes : ils se croisent une fois par tour. */
    public static final double RESONANCE_PERIOD = 2;
    /** La part du tour pendant laquelle les faisceaux se croisent : le moment où un clic les capte. */
    public static final double RESONANCE_WINDOW = 0.16;
    /** Le quark top se désintègre une minute après son arrivée ; utilisé à temps, il donne dix minutes de surchauffe. */
    public static final double TOP_SECONDS = 60;
    public static final double OVERHEAT_SECONDS = 600;
    public static final double OVERHEAT_FACTOR = 10;
    /** Quarks beaux rendus quand le top se désintègre sans servir : rater le moment ne coûte rien. */
    public static final int TOP_CONSOLATION_BOTTOM = 3;
    /** Chance qu'un croisement donne une collision, sans les améliorations. */
    public static final double COLLISION_CHANCE = 0.15;
    /** Ce que chaque niveau de focalisation ajoute à cette chance. */
    public static final double LUCK_CHANCE = 0.03;
    /** Secondes de production que rapporte un croisement capté d'un clic. */
    public static final double CROSSING_SECONDS = 3;
    /** Au-delà de ce nombre de croisements d'un coup (une longue absence), le butin se compte en moyenne. */
    static final int CROSSINGS_ONE_BY_ONE = 400;
    /** Les collisions gardées pour le dessin des faisceaux. */
    private static final int RECENT_COLLISIONS = 24;

    /**
     * Ce qu'une collision a donné : une saveur et son nombre de quarks. {@code caught} : le croisement a été capté (d'un clic ou par le déclencheur), le butin
     * est doublé. {@code serial} : le numéro de la collision, pour que le dessin ne la montre qu'une fois.
     */
    public record CollisionResult(long serial, Quark quark, int count, boolean caught) {}

    private final List<CollisionResult> finishedCollisions = new ArrayList<>();
    private final java.util.ArrayDeque<CollisionResult> recentCollisions = new java.util.ArrayDeque<>();

    /** Le collisionneur s'ouvre à la première explosion et ne se referme plus. */
    public boolean isColliderUnlocked() {
        return state.started() && (isDarkMatterUnlocked() || state.bigBangs() > 0 || state.collisionRuns() > 0
                || state.colliderGeneratorsBought(ColliderGenerator.BEAM) > 0);
    }

    /**
     * Énergie gagnée par seconde : les faisceaux croisés, un d'office et ceux qu'on achète ou que
     * produit l'aimant ({@link ColliderGenerator}), multipliés par la production d'énergie et les
     * succès du collisionneur. Le collisionneur est un jeu à part : il ne dépend pas de la production
     * de particules.
     */
    public double colliderEnergyPerSecond() {
        if (!isColliderUnlocked()) return 0;
        return (1 + state.colliderGenerators(ColliderGenerator.BEAM)) * ColliderGenerator.BEAM.power() * colliderEnergyMultiplier()
                * colliderOutputMultiplier(ColliderGenerator.BEAM);
    }

    /**
     * Ce par quoi un générateur multiplie sa production : son succès de production (×2), le succès
     * des 250 générateurs (×1,5), et pour ceux qui produisent un autre générateur, les générateurs
     * renforcés (×2 par niveau).
     */
    public double colliderOutputMultiplier(ColliderGenerator generator) {
        double multiplier = hasColliderAchievement(ColliderAchievement.of(generator, ColliderAchievement.Kind.OUTPUT)) ? 2 : 1;
        if (hasColliderAchievement(ColliderAchievement.QUARTER_THOUSAND)) multiplier *= 1.5;
        if (generator != ColliderGenerator.BEAM) multiplier *= Math.pow(2, colliderUpgradeLevel(ColliderUpgrade.GENERATOR_POWER));
        return multiplier;
    }

    // Les succès du Big Bang (version 0.61).

    /** Vrai une fois le premier Big Bang passé : les succès du Big Bang se montrent. */
    public boolean isBangAchievementsUnlocked() {
        return state.bigBangs() > 0;
    }

    /** Où en est ce succès du Big Bang : ce qui est réuni, borné à ce qu'il demande. */
    public int bangAchievementProgress(BangAchievement achievement) {
        long have = switch (achievement.stat()) {
            case BIG_BANGS -> state.bigBangs();
            case MOLECULES -> moleculesCreated();
            case BODIES -> bodiesFormed();
            case GALAXY -> hasGalaxy() ? 1 : 0;
            case SYSTEMS -> solarSystemsComplete();
            case PROBES -> state.probeRuns();
            case COMETS -> state.cometsCaught();
        };
        return (int) Math.min(achievement.threshold(), Math.max(0, have));
    }

    public boolean hasBangAchievement(BangAchievement achievement) {
        return bangAchievementProgress(achievement) >= achievement.threshold();
    }

    public int bangAchievements() {
        int count = 0;
        for (BangAchievement achievement : BangAchievement.values()) if (hasBangAchievement(achievement)) count++;
        return count;
    }

    /** Ce par quoi les succès du Big Bang gagnés multiplient cette grandeur : 1 + la somme de leurs bonus. */
    public double bangAchievementMultiplier(BangAchievement.Reward reward) {
        double sum = 0;
        for (BangAchievement achievement : BangAchievement.values()) {
            if (achievement.reward() == reward && hasBangAchievement(achievement)) sum += achievement.amount();
        }
        return 1 + sum;
    }

    // Les succès du collisionneur (version 0.60).

    /** Générateurs achetés en tout, toutes sortes confondues. */
    public int colliderGeneratorsBoughtTotal() {
        int total = 0;
        for (ColliderGenerator generator : ColliderGenerator.values()) total += state.colliderGeneratorsBought(generator);
        return total;
    }

    /** Où en est ce succès : ce qui est réuni (borné à ce qu'il demande). */
    public int colliderAchievementProgress(ColliderAchievement achievement) {
        long have;
        if (achievement.generator() != null) have = state.colliderGeneratorsBought(achievement.generator());
        else if (achievement.kind() == ColliderAchievement.Kind.CHANCE) have = state.collisionRuns();
        else have = colliderGeneratorsBoughtTotal();
        return (int) Math.min(achievement.threshold(), have);
    }

    /** Vrai si ce succès du collisionneur est gagné : il se déduit des achats et des collisions. */
    public boolean hasColliderAchievement(ColliderAchievement achievement) {
        return colliderAchievementProgress(achievement) >= achievement.threshold();
    }

    /** Nombre de succès du collisionneur gagnés. */
    public int colliderAchievements() {
        int count = 0;
        for (ColliderAchievement achievement : ColliderAchievement.values()) if (hasColliderAchievement(achievement)) count++;
        return count;
    }

    /** Ce par quoi les améliorations multiplient l'énergie. */
    public double colliderEnergyMultiplier() {
        double multiplier = Math.pow(2, colliderUpgradeLevel(ColliderUpgrade.ENERGY))
                * (hasColliderAchievement(ColliderAchievement.HUNDRED) ? 1.5 : 1);
        return multiplier;
    }

    /** Ce que produit un générateur du collisionneur par seconde : des exemplaires du précédent, ou de l'énergie pour le premier. */
    public double colliderGeneratorOutput(ColliderGenerator generator) {
        // L'épreuve « Un seul générateur » (version 0.88) : la chaîne s'arrête, seul le premier produit.
        if (generator.previous() != null && inTrial(idle.core.universe.Trial.LONE_COLLIDER)) return 0;
        return state.colliderGenerators(generator) * generator.power() * colliderOutputMultiplier(generator);
    }

    /** Prix du prochain exemplaire de ce générateur. */
    public double colliderGeneratorCost(ColliderGenerator generator) {
        double cost = generator.cost(state.colliderGeneratorsBought(generator));
        return hasColliderAchievement(ColliderAchievement.of(generator, ColliderAchievement.Kind.COST)) ? cost / 2 : cost;
    }

    /** Un générateur s'offre dès que le précédent existe : le premier tout de suite. */
    public boolean isColliderGeneratorOpen(ColliderGenerator generator) {
        ColliderGenerator previous = generator.previous();
        return isColliderUnlocked() && (previous == null || state.colliderGeneratorsBought(previous) > 0);
    }

    public boolean canBuyColliderGenerator(ColliderGenerator generator) {
        return isColliderGeneratorOpen(generator) && state.colliderEnergy() >= colliderGeneratorCost(generator) - 1e-9;
    }

    /** Achète un exemplaire de ce générateur avec l'énergie. */
    public boolean buyColliderGenerator(ColliderGenerator generator) {
        if (!canBuyColliderGenerator(generator)) return false;
        state.setColliderEnergy(state.colliderEnergy() - colliderGeneratorCost(generator));
        state.setColliderGeneratorsBought(generator, state.colliderGeneratorsBought(generator) + 1);
        state.setColliderGenerators(generator, state.colliderGenerators(generator) + 1);
        return true;
    }

    /** Achète autant d'exemplaires de ce générateur que l'énergie le permet. Rend le nombre achetés. */
    public int buyMaxColliderGenerator(ColliderGenerator generator) {
        int bought = 0;
        while (bought < 10_000 && buyColliderGenerator(generator)) bought++;
        return bought;
    }

    public double colliderEnergy() {
        return state.colliderEnergy();
    }

    /** Ajoute de l'énergie, et la compte dans l'énergie produite depuis le début. */
    private void addColliderEnergy(double gain) {
        if (!(gain > 0)) return;
        state.setColliderEnergy(state.colliderEnergy() + gain);
        state.setColliderEnergyProduced(state.colliderEnergyProduced() + gain);
    }

    /** L'énergie produite depuis le début : ce qui ouvre les deux trous noirs (version 0.62). */
    public double colliderEnergyProduced() {
        return state.colliderEnergyProduced();
    }

    // Les croisements et leur hasard.

    /** Secondes entre deux croisements : {@link #RESONANCE_PERIOD}, moins avec les faisceaux rapides. */
    public double colliderPeriod() {
        return RESONANCE_PERIOD / (1 + 0.2 * colliderUpgradeLevel(ColliderUpgrade.SPEED));
    }

    /** La part du tour où les faisceaux se croisent : {@link #RESONANCE_WINDOW}, plus avec le détecteur élargi. */
    public double colliderWindow() {
        return RESONANCE_WINDOW * (1 + 0.25 * colliderUpgradeLevel(ColliderUpgrade.WINDOW));
    }

    /** La phase des faisceaux, de 0 à 1 : ils se croisent dans le détecteur à 0,5. */
    public double colliderBeamPhase() {
        return state.colliderPhase();
    }

    /** Vrai si la phase tombe dans le moment du croisement, de la largeur de départ. */
    public static boolean inResonance(double phase) {
        return Math.abs(phase - 0.5) <= RESONANCE_WINDOW / 2;
    }

    /** Vrai pendant que les faisceaux se croisent, détecteur élargi compris. */
    public boolean isCrossing() {
        return Math.abs(state.colliderPhase() - 0.5) <= colliderWindow() / 2;
    }

    /** Le numéro du croisement en cours : celui qui arrive avant le milieu du tour, celui qui vient de passer après. */
    private long currentCrossing() {
        return state.colliderCrossings() + (state.colliderPhase() < 0.5 ? 1 : 0);
    }

    /** Vrai si le croisement en cours n'a pas encore été capté. */
    public boolean canCatchCrossing() {
        return isColliderUnlocked() && isCrossing() && state.colliderCaught() != currentCrossing();
    }

    /**
     * Un clic quand les faisceaux se croisent : {@link #CROSSING_SECONDS} secondes de production
     * d'énergie d'un coup, et une collision assurée au butin doublé. Un seul par croisement ; à
     * côté, rien.
     *
     * @return l'énergie gagnée, 0 si le clic tombe à côté
     */
    public double catchCrossing() {
        if (!canCatchCrossing()) return 0;
        long crossing = currentCrossing();
        state.setColliderCaught(crossing);
        catchStreak++;
        // Les épreuves du collisionneur (version 0.88) comptent les croisements captés à la main.
        if (activeTrial() != null && activeTrial().isCollider()) state.setTrialCount(state.trialCount() + 1);
        return caught(crossing);
    }

    /** Croisements captés à la main de suite, sans en laisser passer un : rien ne le garde, il repart de zéro au lancement. */
    private int catchStreak;

    /** Croisements captés à la main de suite, depuis le dernier manqué (ou le lancement du jeu). */
    public int catchStreak() {
        return catchStreak;
    }

    /** Ce que donne un croisement capté : l'énergie, et la collision assurée. */
    private double caught(long crossing) {
        double gain = colliderEnergyPerSecond() * CROSSING_SECONDS;
        addColliderEnergy(gain);
        if (isCollisionsUnlocked()) collide(crossing, true);
        return gain;
    }

    /** Vrai une fois l'amélioration « Collisions » prise : les croisements ont leur chance de collision. */
    public boolean isCollisionsUnlocked() {
        return isColliderUnlocked() && colliderUpgradeLevel(ColliderUpgrade.COLLISIONS) > 0;
    }

    /**
     * Chance qu'un croisement donne une collision : aucune avant l'amélioration « Collisions » ;
     * ensuite 15 %, plus la focalisation et le succès des mille collisions.
     */
    public double collisionChance() {
        if (!isCollisionsUnlocked()) return 0;
        double chance = COLLISION_CHANCE
                + LUCK_CHANCE * colliderUpgradeLevel(ColliderUpgrade.LUCK)
                + (hasColliderAchievement(ColliderAchievement.THOUSAND_COLLISIONS) ? 0.02 : 0);
        return Math.min(1, chance);
    }

    /** Vrai si cette énergie est ouverte : son amélioration est prise. */
    public boolean isCollisionTierOpen(CollisionTier tier) {
        return isColliderUnlocked() && colliderUpgradeLevel(tier.upgrade()) > 0;
    }

    /** L'énergie la plus haute ouverte. */
    public CollisionTier colliderTier() {
        CollisionTier top = CollisionTier.LOW;
        for (CollisionTier tier : CollisionTier.values()) if (isCollisionTierOpen(tier)) top = tier;
        return top;
    }

    /** Vrai si un croisement peut donner cette saveur. */
    public boolean isQuarkOpen(Quark quark) {
        return isCollisionTierOpen(CollisionTier.of(quark));
    }

    /** Le poids d'une saveur dans le tirage, 0 si elle n'est pas ouverte : les lourdes sont rares. */
    public double quarkWeight(Quark quark) {
        if (!isQuarkOpen(quark)) return 0;
        double heavy = Math.pow(1.4, colliderUpgradeLevel(ColliderUpgrade.HEAVY));
        return switch (quark) {
            case UP, DOWN -> 45;
            case STRANGE -> 14 * heavy;
            case CHARM -> 6 * heavy;
            case BOTTOM -> 2 * heavy;
            case TOP -> 0.15 * heavy;
        };
    }

    /** La part des collisions qui donne cette saveur, de 0 à 1. */
    public double quarkShare(Quark quark) {
        double total = 0;
        for (Quark each : Quark.values()) total += quarkWeight(each);
        return total <= 0 ? 0 : quarkWeight(quark) / total;
    }

    /** Un nombre de 0 à 1, toujours le même pour un croisement et un tirage donnés : le hasard du collisionneur ne touche pas à celui de la synthèse. */
    private static double roll(long crossing, int draw) {
        double mix = Math.sin(crossing * 12.9898 + draw * 78.233) * 43758.5453;
        return mix - Math.floor(mix);
    }

    /** Un croisement : la chance d'une collision, ou une collision assurée s'il est capté. */
    private void collide(long crossing, boolean caught) {
        if (!caught && roll(crossing, 1) >= collisionChance()) return;
        Quark quark = drawQuark(roll(crossing, caught ? 7 : 2));
        int count = caught ? 2 : 1;
        give(quark, count, caught);
    }

    /** Tire une saveur parmi celles qui sont ouvertes, selon leur poids. */
    private Quark drawQuark(double unit) {
        double total = 0;
        for (Quark quark : Quark.values()) total += quarkWeight(quark);
        double at = unit * total;
        for (Quark quark : Quark.values()) {
            at -= quarkWeight(quark);
            if (at < 0) return quark;
        }
        return Quark.UP;
    }

    /** Ajoute le butin d'une collision, et la retient pour les annonces et le dessin. */
    private void give(Quark quark, int count, boolean caught) {
        long serial = state.collisionRuns() + 1;
        state.setCollisionRuns(serial);
        if (quark == Quark.TOP) {
            state.setTopLeft(Math.max(state.topLeft(), topSeconds()));
        } else {
            state.setQuarks(quark, state.quarks(quark) + count);
        }
        CollisionResult result = new CollisionResult(serial, quark, quark == Quark.TOP ? 1 : count, caught);
        if (finishedCollisions.size() < 200) finishedCollisions.add(result);
        recentCollisions.addLast(result);
        while (recentCollisions.size() > RECENT_COLLISIONS) recentCollisions.removeFirst();
    }

    /** Les dernières collisions, la plus récente au bout : pour le dessin des faisceaux. */
    public List<CollisionResult> recentCollisions() {
        return List.copyOf(recentCollisions);
    }

    /** Les collisions depuis le dernier appel : pour les annoncer une fois. */
    public List<CollisionResult> takeFinishedCollisions() {
        List<CollisionResult> taken = List.copyOf(finishedCollisions);
        finishedCollisions.clear();
        return taken;
    }

    /** Tous les combien de croisements le déclencheur automatique capte : 5, 3 puis 1 ; 0 sans lui. */
    public int autoCatchEvery() {
        return switch (colliderUpgradeLevel(ColliderUpgrade.AUTO_CATCH)) {
            case 0 -> 0;
            case 1 -> 5;
            case 2 -> 3;
            default -> 1;
        };
    }

    /**
     * Les croisements d'un pas de jeu. Un à un d'habitude ; au retour d'une longue absence, au-delà
     * de {@link #CROSSINGS_ONE_BY_ONE}, le butin se compte en moyenne pour ne pas ralentir le jeu.
     */
    private void runCrossings(long count) {
        if (count <= 0) return;
        long first = state.colliderCrossings() + 1;
        state.setColliderCrossings(state.colliderCrossings() + count);
        int every = autoCatchEvery();
        if (count <= CROSSINGS_ONE_BY_ONE) {
            for (long crossing = first; crossing < first + count; crossing++) {
                if (state.colliderCaught() == crossing) continue;      // déjà capté d'un clic, juste avant le milieu
                catchStreak = 0;
                if (inTrial(idle.core.universe.Trial.DETECTOR)) failTrial();      // un croisement laissé passer
                if (every > 0 && crossing % every == 0) {
                    state.setColliderCaught(crossing);
                    caught(crossing);
                } else {
                    collide(crossing, false);
                }
            }
            return;
        }
        // En moyenne : les captés du déclencheur, puis la chance sur les autres.
        catchStreak = 0;
        if (inTrial(idle.core.universe.Trial.DETECTOR)) failTrial();
        long autoCaught = every > 0 ? count / every : 0;
        double free = count - autoCaught;
        addColliderEnergy(colliderEnergyPerSecond() * CROSSING_SECONDS * autoCaught);
        if (!isCollisionsUnlocked()) return;
        double collisions = free * collisionChance() + autoCaught;
        double quarks = free * collisionChance() + 2.0 * autoCaught;
        for (Quark quark : Quark.values()) {
            double share = quarkShare(quark);
            if (share <= 0) continue;
            if (quark == Quark.TOP) {
                if (collisions * share >= 1 || roll(first, 11) < collisions * share) state.setTopLeft(Math.max(state.topLeft(), topSeconds()));
                continue;
            }
            double expected = quarks * share;
            int whole = (int) Math.min(Integer.MAX_VALUE / 4, Math.floor(expected) + (roll(first, 20 + quark.ordinal()) < expected - Math.floor(expected) ? 1 : 0));
            state.setQuarks(quark, (int) Math.min(Integer.MAX_VALUE / 2, (long) state.quarks(quark) + whole));
        }
        state.setCollisionRuns(state.collisionRuns() + (long) collisions);
    }

    // Les améliorations du collisionneur.

    /** Vrai quand les huit générateurs sont là : les améliorations du collisionneur s'ouvrent. */
    public boolean isColliderUpgradesUnlocked() {
        return isColliderUnlocked() && state.colliderGeneratorsBought(ColliderGenerator.PLANCK) > 0
                || !colliderUpgradesBought().isEmpty();
    }

    private List<ColliderUpgrade> colliderUpgradesBought() {
        List<ColliderUpgrade> bought = new ArrayList<>();
        for (ColliderUpgrade upgrade : ColliderUpgrade.values()) if (state.colliderUpgradeLevel(upgrade) > 0) bought.add(upgrade);
        return bought;
    }

    public int colliderUpgradeLevel(ColliderUpgrade upgrade) {
        return state.colliderUpgradeLevel(upgrade);
    }

    /** Vrai si cette amélioration s'offre : la première dès l'ouverture, les autres quand celle qui les ouvre a le niveau voulu. */
    public boolean isColliderUpgradeOpen(ColliderUpgrade upgrade) {
        if (!isColliderUpgradesUnlocked()) return false;
        // La Superposition attend aussi le cinquième Big Bang (version 0.73) : jusque-là, les choix de l'arbre comptent.
        if (upgrade == ColliderUpgrade.SUPERPOSITION && state.bigBangs() < SUPERPOSITION_BIG_BANGS) return false;
        return upgrade.requires() == null || state.colliderUpgradeLevel(upgrade.requires()) >= upgrade.requiredLevel();
    }

    /** Big Bangs qu'il faut avoir déclenchés pour que la Superposition s'offre : cinq, le milieu du troisième acte. */
    public static final int SUPERPOSITION_BIG_BANGS = 5;

    public double colliderUpgradeCost(ColliderUpgrade upgrade) {
        return upgrade.cost(state.colliderUpgradeLevel(upgrade));
    }

    /** Les quarks que demande le prochain niveau de cette amélioration (la chaîne, version 0.62) ; 0 pour les autres. */
    public int colliderUpgradeQuarks(ColliderUpgrade upgrade) {
        return upgrade.quarkCost(state.colliderUpgradeLevel(upgrade));
    }

    public boolean canBuyColliderUpgrade(ColliderUpgrade upgrade) {
        return isColliderUpgradeOpen(upgrade) && state.colliderUpgradeLevel(upgrade) < upgrade.maxLevel()
                && state.colliderEnergy() >= colliderUpgradeCost(upgrade) - 1e-9
                && (upgrade.quark() == null || state.quarks(upgrade.quark()) >= colliderUpgradeQuarks(upgrade));
    }

    /** Prend un niveau de cette amélioration, payé en énergie, et en quarks pour la chaîne. */
    public boolean buyColliderUpgrade(ColliderUpgrade upgrade) {
        if (!canBuyColliderUpgrade(upgrade)) return false;
        state.setColliderEnergy(state.colliderEnergy() - colliderUpgradeCost(upgrade));
        if (upgrade.quark() != null) state.setQuarks(upgrade.quark(), state.quarks(upgrade.quark()) - colliderUpgradeQuarks(upgrade));
        state.setColliderUpgradeLevel(upgrade, state.colliderUpgradeLevel(upgrade) + 1);
        return true;
    }

    /** Vrai si cet automate est acheté et que le joueur le laisse faire. */
    public boolean isColliderAutomationRunning(ColliderUpgrade upgrade) {
        return state.colliderUpgradeLevel(upgrade) > 0 && !state.isColliderPaused(upgrade);
    }

    /** Met en pause un automate, ou le relance. */
    public void toggleColliderAutomation(ColliderUpgrade upgrade) {
        if (state.colliderUpgradeLevel(upgrade) <= 0) return;
        state.setColliderPaused(upgrade, !state.isColliderPaused(upgrade));
    }

    /** Les automates : du générateur le plus haut au plus bas, chacun achète tout ce qu'il peut. */
    private void runColliderAutomations() {
        ColliderGenerator[] generators = ColliderGenerator.values();
        for (int k = generators.length - 1; k >= 0; k--) {
            ColliderUpgrade automation = ColliderUpgrade.automationOf(generators[k]);
            if (isColliderAutomationRunning(automation)) buyMaxColliderGenerator(generators[k]);
        }
    }

    /** L'énergie produite qu'il faut au trou noir (version 0.62), puis au trou noir supermassif, le dernier avant la galaxie. */
    public static final double BLACK_HOLE_ENERGY = 1e12;
    public static final double SUPERMASSIVE_ENERGY = 1e15;

    /**
     * L'énergie du collisionneur que demande cet astre : produite depuis le début, pour les deux trous
     * noirs ; 0 pour les autres. Le collisionneur fait ainsi partie du chemin vers la galaxie.
     */
    public double colliderEnergyNeeded(Body body) {
        return switch (body.tier()) {
            case BLACK_HOLE -> BLACK_HOLE_ENERGY;
            case CORE -> SUPERMASSIVE_ENERGY;
            default -> 0;
        };
    }

    /** Vrai si cet astre demande de l'énergie du collisionneur. */
    public boolean needsColliderEnergy(Body body) {
        return colliderEnergyNeeded(body) > 0;
    }

    /** Vrai si l'énergie produite suffit à cet astre. */
    public boolean hasColliderEnergyFor(Body body) {
        return state.colliderEnergyProduced() >= colliderEnergyNeeded(body) - 1e-6;
    }

    /** Nombre de conditions de cet astre : celles du catalogue, plus l'énergie du collisionneur s'il en demande. */
    public int bodyConditions(String bodyId) {
        Body body = body(bodyId);
        return body.conditions() + (needsColliderEnergy(body) ? 1 : 0);
    }

    /** Quarks en réserve, toutes saveurs ensemble. */
    public long totalQuarks() {
        long total = 0;
        for (Quark quark : Quark.values()) total += state.quarks(quark);
        return total;
    }

        public int quarks(Quark quark) {
        return state.quarks(quark);
    }

    /** Vrai tant qu'un quark top attend d'être utilisé. */
    public boolean hasTop() {
        return state.topLeft() > 0;
    }

    public double topLeft() {
        return state.topLeft();
    }

    /** Combien de temps le quark top attend : une minute, deux fois plus par niveau de top stable. */
    public double topSeconds() {
        return TOP_SECONDS * Math.pow(2, colliderUpgradeLevel(ColliderUpgrade.TOP_LIFE));
    }

    /** Durée de la surchauffe d'un quark top : dix minutes, 50 % de plus par niveau de top stable. */
    public double overheatSeconds() {
        return OVERHEAT_SECONDS * (1 + 0.5 * colliderUpgradeLevel(ColliderUpgrade.TOP_LIFE));
    }

    /** Utilise le quark top : la surchauffe, toutes les particules ×10. */
    public boolean useTop() {
        if (!hasTop()) return false;
        state.setTopLeft(0);
        state.setOverheatLeft(state.overheatLeft() + overheatSeconds());
        return true;
    }

    public double overheatLeft() {
        return state.overheatLeft();
    }

    /** Ce par quoi le collisionneur multiplie les particules : la chaîne, et la surchauffe d'un quark top. */
    public double colliderParticlesMultiplier() {
        return (state.overheatLeft() > 0 ? OVERHEAT_FACTOR : 1) * Math.pow(1.5, colliderUpgradeLevel(ColliderUpgrade.PARTICLES));
    }

    /** Ce par quoi la chaîne multiplie les atomes de chaque fusion. */
    public double colliderAtomsMultiplier() {
        return 1 + 0.1 * colliderUpgradeLevel(ColliderUpgrade.ATOMS);
    }

    /** Ce par quoi la chaîne multiplie la croissance de la matière noire. */
    public double colliderDarkGrowthMultiplier() {
        return 1 + 0.2 * colliderUpgradeLevel(ColliderUpgrade.DARK_MATTER);
    }

    /** Ce par quoi la chaîne multiplie l'espace par seconde. */
    public double colliderSpaceMultiplier() {
        return 1 + 0.25 * colliderUpgradeLevel(ColliderUpgrade.SPACE);
    }

    private void runCollider(double dt) {
        if (dt <= 0 || !isColliderUnlocked()) return;
        finishOldCollisions();
        // La chaîne des générateurs : chacun produit le précédent, le premier l'énergie, au début de ce pas.
        double[] output = new double[ColliderGenerator.values().length];
        for (ColliderGenerator generator : ColliderGenerator.values()) output[generator.ordinal()] = colliderGeneratorOutput(generator);
        addColliderEnergy(colliderEnergyPerSecond() * dt);
        for (ColliderGenerator generator : ColliderGenerator.values()) {
            ColliderGenerator previous = generator.previous();
            if (previous != null && output[generator.ordinal()] > 0) {
                state.setColliderGenerators(previous, state.colliderGenerators(previous) + output[generator.ordinal()] * dt);
            }
        }
        // Les faisceaux avancent ; chaque passage au milieu du tour est un croisement.
        double before = state.colliderPhase();
        double after = before + dt / colliderPeriod();
        long crossings = (long) (Math.floor(after - 0.5) - Math.floor(before - 0.5));
        state.setColliderPhase(after);
        runCrossings(crossings);
        if (state.overheatLeft() > 0) state.setOverheatLeft(state.overheatLeft() - dt);
        if (state.topLeft() > 0) {
            state.setTopLeft(state.topLeft() - dt);
            if (state.topLeft() <= 0) state.setQuarks(Quark.BOTTOM, state.quarks(Quark.BOTTOM) + TOP_CONSOLATION_BOTTOM);
        }
        runColliderAutomations();
    }

    /** Les collisions d'une sauvegarde d'avant la version 0.59 se terminent d'un coup, avec leur butin d'alors. */
    private void finishOldCollisions() {
        if (state.collisions().isEmpty()) return;
        for (Collision collision : state.collisions()) {
            int factor = collision.doubled() ? 2 : 1;
            switch (collision.tier()) {
                case LOW -> {
                    addQuarks(Quark.UP, 3 * factor);
                    addQuarks(Quark.DOWN, 3 * factor);
                }
                case MEDIUM -> {
                    addQuarks(Quark.STRANGE, factor);
                    addQuarks(Quark.UP, factor);
                    addQuarks(Quark.DOWN, factor);
                }
                case HIGH -> {
                    addQuarks(Quark.CHARM, factor);
                    addQuarks(Quark.STRANGE, factor);
                }
                case VERY_HIGH -> {
                    addQuarks(Quark.BOTTOM, factor);
                    addQuarks(Quark.CHARM, factor);
                }
                case EXTREME -> {
                    addQuarks(Quark.BOTTOM, 2 * factor);
                    state.setTopLeft(Math.max(state.topLeft(), topSeconds()));
                }
            }
        }
        state.setCollisions(List.of());
    }

    private void addQuarks(Quark quark, int count) {
        state.setQuarks(quark, state.quarks(quark) + count);
    }

    /** Un entier de {@code low} à {@code high}, toujours le même pour un tirage donné. */
    private static int between(long run, int draw, int low, int high) {
        double mix = Math.sin(run * 12.9898 + draw * 78.233) * 43758.5453;
        double unit = mix - Math.floor(mix);
        return low + (int) Math.floor(unit * (high - low + 1));
    }

    // ------------------------------------------------------------------
    // L'impulsion : le clic du joueur actif (version 0.48)
    // ------------------------------------------------------------------

    /** Charges d'impulsion au plus : deux minutes d'avance, pour qui revient un instant. */
    public static final int PULSE_MAX_CHARGES = 40;
    /** Une charge revient toutes les trois secondes de jeu. */
    public static final double PULSE_REFILL_SECONDS = 3;
    /** Une impulsion donne d'un coup l'espace de dix secondes d'expansion. */
    public static final double PULSE_SECONDS = 10;

    /**
     * Vrai une fois le premier Big Bang passé : l'impulsion pousse l'expansion. Au milieu du
     * troisième acte, c'est l'espace qui manque ; le joueur présent l'accélère d'un clic (jusqu'à
     * quatre fois plus vite en cliquant sans arrêt), celui qui laisse tourner n'y perd rien.
     */
    public boolean isPulseUnlocked() {
        return state.started() && isBigBangUnlocked();
    }

    /** Charges entières disponibles. */
    public int pulseCharges() {
        return (int) Math.floor(state.pulseCharges() + 1e-9);
    }

    /** Espace que donnerait une impulsion maintenant. */
    public BigNum pulseGain() {
        return isPulseUnlocked() ? steadySpacePerSecond().multiply(PULSE_SECONDS) : BigNum.ZERO;
    }

    /** Secondes avant la prochaine charge (0 si la réserve est pleine). */
    public double secondsUntilPulse() {
        double charges = state.pulseCharges();
        if (charges >= PULSE_MAX_CHARGES) return 0;
        return (Math.floor(charges + 1e-9) + 1 - charges) * PULSE_REFILL_SECONDS;
    }

    /** Dépense une charge pour dix secondes d'expansion d'un coup. Faux sans charge ou avant le premier Big Bang. */
    public boolean pulse() {
        if (!isPulseUnlocked() || state.pulseCharges() < 1 - 1e-9) return false;
        state.setPulseCharges(state.pulseCharges() - 1);
        state.setSpace(state.space().add(pulseGain()));
        state.setPulses(state.pulses() + 1);
        return true;
    }

    /** Impulsions données depuis le début du jeu. */
    public long pulses() {
        return state.pulses();
    }

    private void refillPulses(double dt) {
        if (dt <= 0 || !isPulseUnlocked()) return;
        state.setPulseCharges(Math.min(PULSE_MAX_CHARGES, state.pulseCharges() + dt / PULSE_REFILL_SECONDS));
    }

    // ------------------------------------------------------------------
    // La comète : un passage à saisir
    // ------------------------------------------------------------------

    /** Attente la plus courte et la plus longue entre deux comètes, en secondes de jeu. */
    public static final double COMET_MIN_WAIT = 8 * 60;
    public static final double COMET_MAX_WAIT = 16 * 60;
    /** Temps pendant lequel une comète reste à saisir, en secondes. */
    public static final double COMET_VISIBLE_SECONDS = 45;
    /** Durée du sillage d'une comète saisie, en secondes, et ce par quoi il multiplie l'expansion. */
    public static final double COMET_BOOST_SECONDS = 120;
    public static final double COMET_BOOST = 2;
    private boolean cometAppeared = false;

    /**
     * Fait passer le temps de la comète. Après le premier Big Bang, une comète traverse l'expansion
     * de temps en temps ; elle reste {@link #COMET_VISIBLE_SECONDS} secondes, puis s'en va. Saisie
     * ({@link #catchComet()}), elle laisse un sillage qui double l'expansion deux minutes. Qui
     * n'est pas là n'y perd rien : elle n'enlève jamais quoi que ce soit.
     */
    private void runComet(double dt) {
        if (dt <= 0 || !state.started() || !isBigBangUnlocked()) return;
        state.setCometBoost(state.cometBoost() - dt);
        double left = dt;
        while (left > 1e-12) {
            if (state.cometVisible() > 0) {
                double used = Math.min(left, state.cometVisible());
                state.setCometVisible(state.cometVisible() - used);
                left -= used;
                if (state.cometVisible() <= 1e-9) {
                    state.setCometVisible(0);
                    state.setCometWait(nextCometWait());
                }
            } else {
                if (state.cometWait() <= 0) state.setCometWait(nextCometWait());
                double used = Math.min(left, state.cometWait());
                state.setCometWait(state.cometWait() - used);
                left -= used;
                if (state.cometWait() <= 1e-9) {
                    state.setCometWait(0);
                    state.setCometVisible(COMET_VISIBLE_SECONDS);
                    cometAppeared = true;
                }
            }
        }
    }

    /**
     * L'attente avant la prochaine comète, entre {@link #COMET_MIN_WAIT} et {@link #COMET_MAX_WAIT}.
     * Elle se déduit du temps de jeu et du nombre de comètes saisies, sans toucher au hasard du
     * jeu : les tirages de la synthèse ne dépendent pas de ce que fait la comète.
     */
    private double nextCometWait() {
        double mix = Math.sin(state.timePlayed() * 12.9898 + state.cometsCaught() * 78.233) * 43758.5453;
        double share = mix - Math.floor(mix);
        return COMET_MIN_WAIT + (COMET_MAX_WAIT - COMET_MIN_WAIT) * share;
    }

    /** Vrai tant qu'une comète traverse l'expansion et peut être saisie. */
    public boolean isCometVisible() {
        return isBigBangUnlocked() && state.cometVisible() > 0;
    }

    /** Secondes pendant lesquelles la comète reste encore à saisir ; 0 s'il n'y en a pas. */
    public double cometVisibleFor() {
        return isCometVisible() ? state.cometVisible() : 0;
    }

    /** Secondes de jeu avant la prochaine comète ; 0 pendant qu'une comète est là, ou avant le premier Big Bang. */
    public double cometWait() {
        return isBigBangUnlocked() && state.cometVisible() <= 0 ? state.cometWait() : 0;
    }

    /**
     * Saisit la comète : son sillage multiplie l'expansion par {@link #COMET_BOOST} pendant
     * {@link #COMET_BOOST_SECONDS} secondes. Une comète saisie pendant un sillage le relance pour sa durée entière.
     *
     * @return {@code true} s'il y avait une comète à saisir
     */
    public boolean catchComet() {
        if (!isCometVisible()) return false;
        state.setCometVisible(0);
        state.setCometsCaught(state.cometsCaught() + 1);
        state.setCometBoost(COMET_BOOST_SECONDS);
        state.setCometWait(nextCometWait());
        return true;
    }

    /** Ce par quoi le sillage d'une comète multiplie l'expansion en ce moment : {@link #COMET_BOOST}, ou 1. */
    public double cometBoost() {
        return state.cometBoost() > 0 ? COMET_BOOST : 1;
    }

    /** Secondes de sillage qu'il reste. */
    public double cometBoostLeft() {
        return state.cometBoost();
    }

    /** Nombre de comètes saisies depuis le début du jeu. */
    public int cometsCaught() {
        return state.cometsCaught();
    }

    /** Vrai si une comète vient d'apparaître depuis le dernier appel : pour l'annoncer. Remis à faux à chaque lecture. */
    public boolean takeCometAppeared() {
        boolean appeared = cometAppeared;
        cometAppeared = false;
        return appeared;
    }

    // ------------------------------------------------------------------
    // Les records : le meilleur temps de chaque étape, d'une partie à l'autre
    // ------------------------------------------------------------------

    private final List<GameStats.Step> newRecords = new ArrayList<>();

    /** Note qu'une étape vient d'être atteinte ; la première fois, son temps est comparé au record. */
    private void noteStep(GameStats.Step step) {
        boolean first = !state.stats().reached(step);
        state.stats().noteStep(step, state.timePlayed());
        if (first) noteRecord(step);
    }

    /** Retient le temps de jeu comme record de cette étape s'il bat le précédent, ou s'il n'y en avait pas. */
    private void noteRecord(GameStats.Step step) {
        double time = state.timePlayed();
        Double best = state.records().get(step.name());
        if (best == null || time < best) {
            // Un premier record n'est pas une nouvelle : il n'y avait rien à battre.
            if (best != null) newRecords.add(step);
            state.setRecord(step.name(), time);
        }
    }

    /**
     * Le record d'une étape : le temps de jeu le plus court auquel elle a été atteinte, toutes
     * parties confondues, en secondes. {@code NaN} si elle ne l'a jamais été. La remise à zéro du
     * jeu garde les records : ce sont eux qu'on cherche à battre à la partie suivante.
     */
    public double recordOf(GameStats.Step step) {
        return state.records().getOrDefault(step.name(), Double.NaN);
    }

    /** La prochaine étape datée que cette partie n'a pas encore atteinte, ou {@code null} quand elles le sont toutes. */
    public GameStats.Step nextStep() {
        for (GameStats.Step step : GameStats.Step.values()) {
            if (state.stats().reached(step)) continue;
            // Une sauvegarde d'avant ces deux étapes les a passées sans les dater : elles ne sont plus « à venir ».
            if (step == GameStats.Step.FIRST_FUSION && state.stats().fusions() > 0) continue;
            if (step == GameStats.Step.FIRST_EXPLOSION && state.stats().timedExplosions() > 0) continue;
            return step;
        }
        return null;
    }

    /**
     * L'écart au record sur la prochaine étape, en secondes : négatif tant que le joueur est en
     * avance sur son record, positif une fois qu'il l'a dépassé. {@code NaN} quand il n'y a pas de
     * prochaine étape, ou pas encore de record pour elle.
     */
    public double recordGap() {
        GameStats.Step next = nextStep();
        if (next == null) return Double.NaN;
        double record = recordOf(next);
        return Double.isNaN(record) ? Double.NaN : state.timePlayed() - record;
    }

    /** Oublie tous les records. */
    public void clearRecords() {
        state.clearRecords();
    }

    /** Les étapes dont le record vient d'être battu depuis le dernier appel : pour l'annoncer. Vidé à chaque lecture. */
    public List<GameStats.Step> takeNewRecords() {
        List<GameStats.Step> taken = new ArrayList<>(newRecords);
        newRecords.clear();
        return taken;
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
        DarkAutomation automation = darkAutomation(darkAutomationId);
        // Celui qui rachète l'arbre ne vient qu'après un premier Big Bang : avant, l'arbre se découvre à la main.
        if (automation.kind() == DarkAutomation.Kind.DARK_TREE && state.bigBangs() == 0) return false;
        return darkMatterEarned().gte(automation.darkMatter());
    }

    /** Vrai si cet automatisme de matière noire est débloqué et en marche. */
    public boolean isDarkAutomationEnabled(String darkAutomationId) {
        DarkAutomation automation = darkAutomation(darkAutomationId);
        return state.isDarkAutomationEnabled(automation.id()) && isDarkAutomationUnlocked(darkAutomationId)
                && !bangRuled(BangChallenge.BY_HAND);
    }

    /** Vrai si le défi de Big Bang en cours interdit les automatismes de matière noire et du troisième acte : leurs réglages restent, ils ne font rien. */
    public boolean isHandOnly() {
        return bangRuled(BangChallenge.BY_HAND);
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

    /** Délai actuel entre deux actions de cet automatisme de matière noire, en secondes. */
    public double darkAutomationInterval(String darkAutomationId) {
        double interval = darkAutomation(darkAutomationId).interval();
        return ruled(Challenge.Rule.RUSTY_AUTOMATIONS) ? Math.max(RUSTY_MIN_INTERVAL, interval * RUSTY_FACTOR) : interval;
    }

    /** Fait agir les automatismes de matière noire en marche dont le délai est écoulé : une action par délai. */
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
            case DARK_TREE -> buyCheapestDarkUpgrade();
        };
    }

    /**
     * Achète, dans l'ordre du catalogue, la première chose payable parmi : les cases de l'arbre de
     * matière noire et les améliorations payées en matière noire dont le nombre de niveaux est limité.
     * Une case sans dernier niveau est prise d'abord à son premier niveau, celui qui ouvre la case du
     * dessous ; ses niveaux suivants ne le sont que lorsqu'ils coûtent au plus 1 % de la branche
     * ({@link #TREE_REPEAT_SHARE}), pour ne pas avaler ce qu'il faut réunir pour le Big Bang. Les
     * améliorations de la boutique sans limite restent entièrement au joueur.
     */
    private boolean buyCheapestDarkUpgrade() {
        if (inChallenge()) return false;
        for (DarkUpgrade upgrade : darkUpgrades.values()) {
            if (!upgrade.hasLimit() && (!upgrade.inTree() || state.darkLevelOf(upgrade.id()) >= 1)) continue;
            // Les bifurcations restent au joueur : c'est lui qui choisit sa voie (version 0.69).
            if (upgrade.isFork()) continue;
            if (canBuyDark(upgrade.id()) && buyDark(upgrade.id())) return true;
        }
        // Les niveaux suivants des cases sans fin (Densité, Noyau lourd…), seulement quand ils ne coûtent
        // presque rien : au plus TREE_REPEAT_SHARE de ce que possède la branche. Leur prix grandit à chaque
        // niveau, si bien que l'automatisme n'entame jamais vraiment ce qu'il faut réunir pour le Big Bang.
        for (DarkUpgrade upgrade : darkUpgrades.values()) {
            if (upgrade.hasLimit() || !upgrade.branch().inTree() || state.darkLevelOf(upgrade.id()) < 1) continue;
            if (!canBuyDark(upgrade.id())) continue;
            BigNum cost = darkCostOf(upgrade.id());
            if (cost.lte(darkBalance(upgrade.branch()).multiply(TREE_REPEAT_SHARE)) && buyDark(upgrade.id())) return true;
        }
        return false;
    }

    /** Part de la branche au-delà de laquelle l'automatisme de l'arbre ne prend plus un niveau de case sans fin : 1 %. */
    public static final double TREE_REPEAT_SHARE = 0.01;

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
        // Les défis gardent cet automatisme (version 0.47) : sans lui, chaque défi obligeait à racheter
        // les automatismes à la main. Les autres mémoires de la matière noire restent sans effet en défi.
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

    /**
     * Les améliorations payées en atomes dans l'ordre où l'automatisme les achète quand le joueur a
     * choisi le sien : son ordre, complété par celles qu'il n'a pas rangées, dans l'ordre du catalogue.
     */
    public List<Upgrade> atomUpgradeOrder() {
        List<Upgrade> ordered = new ArrayList<>();
        for (String id : state.atomUpgradeOrder()) {
            Upgrade upgrade = upgrades.get(id);
            if (upgrade != null && upgrade.resource() == Resource.ATOMS && !ordered.contains(upgrade)) ordered.add(upgrade);
        }
        for (Upgrade upgrade : upgrades.values()) {
            if (upgrade.resource() == Resource.ATOMS && !ordered.contains(upgrade)) ordered.add(upgrade);
        }
        return ordered;
    }

    /** Vrai si l'automatisme « Améliorations en atomes » suit l'ordre du joueur plutôt que « la moins chère d'abord ». */
    public boolean isAtomUpgradeOrdered() {
        return state.atomUpgradeOrdered();
    }

    public void setAtomUpgradeOrdered(boolean ordered) {
        state.setAtomUpgradeOrdered(ordered);
    }

    /**
     * Fait monter ou descendre une amélioration d'un rang dans l'ordre d'achat.
     *
     * @param direction −1 pour la faire monter (achetée plus tôt), +1 pour la faire descendre
     * @return vrai si elle a bougé ; faux au bout de la liste
     */
    public boolean moveAtomUpgrade(String upgradeId, int direction) {
        List<String> ids = new ArrayList<>();
        for (Upgrade upgrade : atomUpgradeOrder()) ids.add(upgrade.id());
        if (!moved(ids, upgrade(upgradeId).id(), direction)) return false;
        state.setAtomUpgradeOrder(ids);
        return true;
    }

    private static boolean moved(List<String> ids, String id, int direction) {
        int at = ids.indexOf(id);
        int to = at + Integer.signum(direction);
        if (at < 0 || to < 0 || to >= ids.size() || to == at) return false;
        java.util.Collections.swap(ids, at, to);
        return true;
    }

    /**
     * Dans l'ordre du joueur : la première amélioration qui n'est pas au maximum est la seule
     * achetée, et l'automatisme l'attend s'il le faut. Une amélioration dont le prix dépasse le
     * plafond d'atomes est sautée : l'attendre bloquerait tout.
     */
    private boolean buyNextAtomUpgrade() {
        for (Upgrade upgrade : atomUpgradeOrder()) {
            if (isMaxed(upgrade.id())) continue;
            if (!isAtomCapLifted() && costOf(upgrade.id()).gt(atomCap())) continue;
            return canBuy(upgrade.id()) && buy(upgrade.id());
        }
        return false;
    }

    /** Achète l'amélioration payée en atomes la moins chère que le joueur peut s'offrir, ou la suivante dans son ordre s'il en a choisi un. */
    private boolean buyCheapestAtomUpgrade() {
        if (state.atomUpgradeOrdered()) return buyNextAtomUpgrade();
        Upgrade cheapest = null;
        for (Upgrade upgrade : upgrades.values()) {
            if (upgrade.resource() != Resource.ATOMS || !canBuy(upgrade.id())) continue;
            if (cheapest == null || costOf(upgrade.id()).lt(costOf(cheapest.id()))) cheapest = upgrade;
        }
        return cheapest != null && buy(cheapest.id());
    }

    /**
     * Les automatismes ordinaires dans l'ordre où l'automatisme les achète quand le joueur a choisi
     * le sien : son ordre, complété par ceux qu'il n'a pas rangés, dans l'ordre du catalogue.
     */
    public List<Automation> automationOrder() {
        List<Automation> ordered = new ArrayList<>();
        for (String id : state.automationOrder()) {
            Automation automation = automations.get(id);
            if (automation != null && !ordered.contains(automation)) ordered.add(automation);
        }
        for (Automation automation : automations.values()) {
            if (!ordered.contains(automation)) ordered.add(automation);
        }
        return ordered;
    }

    /** Vrai si l'automatisme « Achat des automatismes » suit l'ordre du joueur plutôt que « le moins cher d'abord ». */
    public boolean isAutomationOrdered() {
        return state.automationOrdered();
    }

    public void setAutomationOrdered(boolean ordered) {
        state.setAutomationOrdered(ordered);
    }

    /**
     * Fait monter ou descendre un automatisme d'un rang dans l'ordre d'achat.
     *
     * @param direction −1 pour le faire monter (acheté plus tôt), +1 pour le faire descendre
     * @return vrai s'il a bougé ; faux au bout de la liste
     */
    public boolean moveAutomation(String automationId, int direction) {
        List<String> ids = new ArrayList<>();
        for (Automation automation : automationOrder()) ids.add(automation.id());
        if (!moved(ids, automation(automationId).id(), direction)) return false;
        state.setAutomationOrder(ids);
        return true;
    }

    /**
     * Dans l'ordre du joueur : le premier automatisme qui n'est pas acheté, ou pas encore à sa
     * cadence maximale, est le seul servi, et l'automatisme l'attend s'il le faut. Un automatisme
     * pas encore débloqué, ou dont le prix dépasse le plafond d'atomes, est sauté.
     */
    private boolean buyNextAutomation() {
        for (Automation automation : automationOrder()) {
            String id = automation.id();
            if (!isAutomationAvailable(id)) continue;
            boolean owned = state.ownsAutomation(id);
            if (owned && isAutomationMaxed(id)) continue;
            BigNum cost = owned ? automationSpeedCost(id) : automation.cost();
            if (!isAtomCapLifted() && cost.gt(atomCap())) continue;
            if (!owned) return canBuyAutomation(id) && buyAutomation(id);
            return canSpeedUpAutomation(id) && speedUpAutomation(id);
        }
        return false;
    }

    /** Achète l'automatisme ordinaire, ou le niveau de cadence, le moins cher que le joueur peut s'offrir, ou le suivant dans son ordre s'il en a choisi un. */
    private boolean buyCheapestAutomation() {
        if (state.automationOrdered()) return buyNextAutomation();
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

    /** Les améliorations de matière noire d'une branche, dans l'ordre du catalogue, sans les liaisons. */
    public List<DarkUpgrade> darkUpgrades(DarkUpgrade.Branch branch) {
        List<DarkUpgrade> result = new ArrayList<>();
        for (DarkUpgrade dark : darkUpgrades.values()) {
            if (dark.branch() == branch && !dark.isLink()) result.add(dark);
        }
        return result;
    }

    /** Les liaisons entre branches (version 0.70), dans l'ordre du catalogue. */
    public List<DarkUpgrade> darkLinks() {
        List<DarkUpgrade> result = new ArrayList<>();
        for (DarkUpgrade dark : darkUpgrades.values()) if (dark.isLink()) result.add(dark);
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
     * Vrai si la case est accessible : la matière noire existe et la case du dessus est acquise
     * (s'il y en a une). Depuis la version 0.70, aucune case n'attend plus un nombre de matières noires
     * gagnées : seuls les prix rythment l'arbre.
     */
    public boolean isDarkAvailable(String darkUpgradeId) {
        DarkUpgrade dark = darkUpgrade(darkUpgradeId);
        return isDarkMatterUnlocked() && hasDarkParent(dark) && darkForkRival(darkUpgradeId) == null;
    }

    /** Vrai si la case du dessus est acquise : une case ordinaire, ou l'une des voies d'une bifurcation. */
    private boolean hasDarkParent(DarkUpgrade dark) {
        if (dark.requires() == null) return true;
        String fork = dark.requiredFork();
        if (dark.isAccord()) {
            for (String linked : dark.linkedIds()) if (state.darkLevelOf(linked) == 0) return false;
            return true;
        }
        if (dark.isLink()) {
            for (String linked : dark.linkedIds()) if (state.darkLevelOf(linked) > 0) return true;
            return false;
        }
        if (fork == null) return state.darkLevelOf(dark.requires()) > 0;
        for (DarkUpgrade other : darkUpgrades.values()) {
            if (fork.equals(other.fork()) && state.darkLevelOf(other.id()) > 0) return true;
        }
        return false;
    }

    /**
     * L'autre voie de la bifurcation, si le joueur l'a prise : elle ferme celle-ci jusqu'au prochain
     * Big Bang (version 0.69). {@code null} si la case n'est pas une voie, ou si l'autre n'est pas prise.
     */
    public DarkUpgrade darkForkRival(String darkUpgradeId) {
        DarkUpgrade dark = darkUpgrade(darkUpgradeId);
        if (!dark.isFork() || hasSuperposition()) return null;
        for (DarkUpgrade other : darkUpgrades.values()) {
            if (other != dark && dark.fork().equals(other.fork()) && state.darkLevelOf(other.id()) > 0) return other;
        }
        return null;
    }

    /**
     * Vrai une fois la « Superposition » du collisionneur prise (version 0.70) : les deux voies de chaque
     * bifurcation se prennent, et elles restent, avec les liaisons, d'un Big Bang à l'autre.
     */
    public boolean hasSuperposition() {
        return colliderUpgradeLevel(ColliderUpgrade.SUPERPOSITION) > 0;
    }

    // ------------------------------------------------------------------
    // Les plans de l'arbre et des systèmes solaires (version 0.84)
    // ------------------------------------------------------------------

    /** Le début d'un code de l'arbre. */
    public static final String TREE_CODE_PREFIX = "arbre:";
    /** Le début d'un code des systèmes solaires. */
    public static final String SYSTEMS_CODE_PREFIX = "systemes:";
    /** Intervalle, en secondes de jeu, entre deux passages des plans. */
    public static final double PLAN_CHECK_SECONDS = 1;

    private double planTimer;

    /** Ce qu'un code a donné : les identifiants reconnus, et ceux que le jeu ne connaît pas (écartés). */
    public record PlanLoad(int known, int unknown) {
        /** Vrai si le code ne contenait rien d'utilisable. */
        public boolean empty() {
            return known == 0;
        }
    }

    /** L'état d'une case du plan de l'arbre. */
    public enum PlanStep {
        /** Prise. */
        TAKEN,
        /** Pas encore payable, ou sa case du dessus pas encore prise : elle sera prise dès que possible. */
        WAITING,
        /** L'autre voie de sa bifurcation est prise : elle attend que les bifurcations se rouvrent. */
        BLOCKED
    }

    /**
     * Le code des voies et des liaisons prises dans l'arbre : « arbre: » puis leurs identifiants,
     * dans l'ordre de l'arbre. Il se recolle avec {@link #loadDarkPlan(String)}.
     */
    public String darkTreeCode() {
        List<String> ids = new ArrayList<>();
        for (DarkUpgrade dark : darkUpgrades.values()) {
            if ((dark.isFork() || dark.isLink()) && state.darkLevelOf(dark.id()) > 0) ids.add(dark.id());
        }
        return TREE_CODE_PREFIX + String.join(",", ids);
    }

    /**
     * Fait d'un code de l'arbre le plan suivi : ses voies et ses liaisons se prennent dès qu'elles
     * sont payables, maintenant et après chaque Big Bang, jusqu'à ce que le joueur l'oublie. Un code
     * vide ou illisible ne change rien.
     */
    public PlanLoad loadDarkPlan(String code) {
        if (code == null) return new PlanLoad(0, 0);
        String text = code.trim();
        if (text.startsWith(TREE_CODE_PREFIX)) text = text.substring(TREE_CODE_PREFIX.length());
        java.util.Set<String> wanted = new java.util.LinkedHashSet<>();
        int unknown = 0;
        for (String id : text.split("[,;\\s]+")) {
            if (id.isBlank()) continue;
            DarkUpgrade dark = darkUpgrades.get(id);
            if (dark == null || !(dark.isFork() || dark.isLink())) unknown++;
            else wanted.add(id);
        }
        if (wanted.isEmpty()) return new PlanLoad(0, unknown);
        // Dans l'ordre de l'arbre : une liaison vient après les voies qu'elle relie.
        List<String> ordered = new ArrayList<>();
        for (DarkUpgrade dark : darkUpgrades.values()) if (wanted.contains(dark.id())) ordered.add(dark.id());
        state.setDarkPlan(ordered);
        applyDarkPlan();
        return new PlanLoad(ordered.size(), unknown);
    }

    /** Les cases du plan de l'arbre, dans l'ordre ; vide sans plan. */
    public List<DarkUpgrade> darkPlan() {
        List<DarkUpgrade> plan = new ArrayList<>();
        for (String id : state.darkPlan()) {
            DarkUpgrade dark = darkUpgrades.get(id);
            if (dark != null) plan.add(dark);
        }
        return plan;
    }

    /** Où en est une case du plan de l'arbre. */
    public PlanStep darkPlanStep(DarkUpgrade dark) {
        if (state.darkLevelOf(dark.id()) > 0) return PlanStep.TAKEN;
        return darkForkRival(dark.id()) != null ? PlanStep.BLOCKED : PlanStep.WAITING;
    }

    /** Oublie le plan de l'arbre : rien de pris n'est rendu. */
    public void forgetDarkPlan() {
        state.setDarkPlan(List.of());
    }

    /** Garde le code de l'arbre actuel dans le premier emplacement. */
    public void saveDarkPlan() {
        saveDarkPlan(0);
    }

    /** Garde le code de l'arbre actuel dans cet emplacement (version 0.86), pour le recharger plus tard. */
    public void saveDarkPlan(int slot) {
        state.setSavedDarkPlan(slot, darkTreeCode());
    }

    /** Recharge le code du premier emplacement. */
    public PlanLoad loadSavedDarkPlan() {
        return loadSavedDarkPlan(0);
    }

    /** Recharge le code gardé dans cet emplacement. */
    public PlanLoad loadSavedDarkPlan(int slot) {
        return loadDarkPlan(state.savedDarkPlan(slot));
    }

    /** Ce que contient un code de l'arbre : {voies, liaisons et accords}, d'après le catalogue. */
    public int[] describeTreeCode(String code) {
        int[] counts = new int[2];
        if (code == null) return counts;
        String text = code.trim();
        if (text.startsWith(TREE_CODE_PREFIX)) text = text.substring(TREE_CODE_PREFIX.length());
        for (String id : text.split("[,;\\s]+")) {
            DarkUpgrade dark = darkUpgrades.get(id);
            if (dark == null) continue;
            if (dark.isFork()) counts[0]++;
            else if (dark.isLink()) counts[1]++;
        }
        return counts;
    }

    /** Ce que contient un code des systèmes : {systèmes, astres placés autour des étoiles}. */
    public int[] describeSystemsCode(String code) {
        int[] counts = new int[2];
        if (code == null) return counts;
        String text = code.trim();
        if (text.startsWith(SYSTEMS_CODE_PREFIX)) text = text.substring(SYSTEMS_CODE_PREFIX.length());
        for (String line : text.split("[;\\n]+")) {
            SolarSystem system = SolarSystem.parse(line);
            if (system == null) continue;
            counts[0]++;
            counts[1] += system.planets().size() + system.moons().size();
        }
        return counts;
    }

    /** Prend tout ce que le plan de l'arbre permet maintenant ; renvoie le nombre de cases prises. */
    public int applyDarkPlan() {
        int taken = 0;
        for (String id : state.darkPlan()) {
            if (!darkUpgrades.containsKey(id) || state.darkLevelOf(id) > 0) continue;
            if (buyDark(id)) taken++;
        }
        return taken;
    }

    /** Vrai si « Rouvrir les bifurcations à la prochaine explosion » a un sens : une voie prise, sans la Superposition. */
    public boolean canArmForkReset() {
        if (!isDarkMatterUnlocked() || hasSuperposition()) return false;
        for (DarkUpgrade dark : darkUpgrades.values()) {
            if (dark.isFork() && state.darkLevelOf(dark.id()) > 0) return true;
        }
        return false;
    }

    /** Arme ou désarme la réouverture des bifurcations à la prochaine explosion. */
    public void setForkResetArmed(boolean armed) {
        state.setForkResetArmed(armed && (canArmForkReset() || !armed));
    }

    /**
     * Le code de la disposition des systèmes solaires : « systemes: » puis un système par morceau,
     * séparés par « ; » ; chacun « étoile|planètes|lunes », les planètes de la plus proche à la plus lointaine.
     */
    public String systemsCode() {
        List<String> lines = new ArrayList<>();
        for (SolarSystem system : state.solarSystems()) lines.add(system.line());
        return SYSTEMS_CODE_PREFIX + String.join(";", lines);
    }

    /**
     * Fait d'un code des systèmes le plan suivi : chaque système se fonde, et chaque astre s'y place
     * à son orbite, dès qu'il est mûr et libre. Un astre déjà dans un autre système y reste. Les
     * identifiants inconnus, ou d'une autre échelle (une planète à la place d'une étoile), sont écartés.
     */
    public PlanLoad loadSystemPlan(String code) {
        if (code == null) return new PlanLoad(0, 0);
        String text = code.trim();
        if (text.startsWith(SYSTEMS_CODE_PREFIX)) text = text.substring(SYSTEMS_CODE_PREFIX.length());
        List<SolarSystem> plan = new ArrayList<>();
        java.util.Set<String> used = new java.util.HashSet<>();
        int known = 0, unknown = 0;
        for (String line : text.split("[;\\n]+")) {
            if (line.isBlank()) continue;
            SolarSystem read = SolarSystem.parse(line);
            if (read == null || !isTier(read.star(), Body.Tier.STAR) || !used.add(read.star())) {
                unknown++;
                continue;
            }
            known++;
            List<String> planets = new ArrayList<>(), moons = new ArrayList<>();
            for (String id : read.planets()) {
                if (isTier(id, Body.Tier.PLANET) && used.add(id)) {
                    planets.add(id);
                    known++;
                } else unknown++;
            }
            for (String id : read.moons()) {
                if (isTier(id, Body.Tier.MOON) && used.add(id) && moons.size() < planets.size()) {
                    moons.add(id);
                    known++;
                } else unknown++;
            }
            plan.add(new SolarSystem(read.star(), planets, moons));
        }
        if (plan.isEmpty()) return new PlanLoad(0, unknown);
        state.setSystemPlan(plan);
        applySystemPlan();
        return new PlanLoad(known, unknown);
    }

    private boolean isTier(String bodyId, Body.Tier tier) {
        Body body = bodies.get(bodyId);
        return body != null && body.tier() == tier;
    }

    /** Le plan des systèmes solaires ; vide sans plan. */
    public List<SolarSystem> systemPlan() {
        return state.systemPlan();
    }

    /** Astres du plan des systèmes déjà à leur place, et astres qu'il compte en tout (étoiles comprises). */
    public int[] systemPlanProgress() {
        int placed = 0, total = 0;
        for (SolarSystem plan : state.systemPlan()) {
            int index = systemIndexOf(plan.star());
            SolarSystem now = index < 0 ? null : state.solarSystems().get(index);
            total += 1 + plan.planets().size() + plan.moons().size();
            if (now == null) continue;
            placed++;
            for (String id : plan.planets()) if (now.planets().contains(id)) placed++;
            for (String id : plan.moons()) if (now.moons().contains(id)) placed++;
        }
        return new int[] {placed, total};
    }

    /** Oublie le plan des systèmes : rien de placé ne bouge. */
    public void forgetSystemPlan() {
        state.setSystemPlan(List.of());
    }

    /** Garde la disposition actuelle des systèmes dans le premier emplacement. */
    public void saveSystemPlan() {
        saveSystemPlan(0);
    }

    /** Garde la disposition actuelle des systèmes dans cet emplacement (version 0.86). */
    public void saveSystemPlan(int slot) {
        state.setSavedSystemPlan(slot, systemsCode());
    }

    /** Recharge la disposition du premier emplacement. */
    public PlanLoad loadSavedSystemPlan() {
        return loadSavedSystemPlan(0);
    }

    /** Recharge la disposition gardée dans cet emplacement. */
    public PlanLoad loadSavedSystemPlan(int slot) {
        return loadSystemPlan(state.savedSystemPlan(slot));
    }

    /** La place du système de cette étoile, ou −1. */
    public int systemIndexOf(String starId) {
        List<SolarSystem> systems = state.solarSystems();
        for (int i = 0; i < systems.size(); i++) if (systems.get(i).star().equals(starId)) return i;
        return -1;
    }

    /** Place tout ce que le plan des systèmes permet maintenant ; renvoie le nombre d'astres placés. */
    public int applySystemPlan() {
        if (state.systemPlan().isEmpty() || !isSolarSystemsUnlocked()) return 0;
        int placed = 0;
        for (SolarSystem plan : state.systemPlan()) {
            int index = systemIndexOf(plan.star());
            if (index < 0) {
                if (!foundSystem(plan.star())) continue;
                index = systemIndexOf(plan.star());
                placed++;
            }
            for (String planet : plan.planets()) {
                if (!state.solarSystems().get(index).planets().contains(planet) && addPlanet(index, planet)) placed++;
            }
            // Les planètes du plan à leurs orbites, dans son ordre ; celles que le joueur a ajoutées en plus, après.
            SolarSystem now = state.solarSystems().get(index);
            List<String> ordered = new ArrayList<>();
            for (String planet : plan.planets()) if (now.planets().contains(planet)) ordered.add(planet);
            for (String planet : now.planets()) if (!ordered.contains(planet)) ordered.add(planet);
            if (!ordered.equals(now.planets())) replaceSystem(index, now.withPlanets(ordered));
            for (String moon : plan.moons()) {
                if (!state.solarSystems().get(index).moons().contains(moon) && addMoon(index, moon)) placed++;
            }
        }
        return placed;
    }

    /** Les plans, une fois par seconde de jeu : ce qu'ils permettent se prend, joueur présent ou absent. */
    private void runPlans(double dt) {
        if (state.darkPlan().isEmpty() && state.systemPlan().isEmpty()) return;
        planTimer += dt;
        if (planTimer < PLAN_CHECK_SECONDS) return;
        planTimer = 0;
        applyDarkPlan();
        applySystemPlan();
    }

    /** Les voies d'une bifurcation, dans l'ordre du catalogue. */
    public List<DarkUpgrade> darkForkOptions(String fork) {
        List<DarkUpgrade> options = new ArrayList<>();
        for (DarkUpgrade dark : darkUpgrades.values()) if (fork.equals(dark.fork())) options.add(dark);
        return options;
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
     * Le niveau qui agit (version 0.70) : celui que possède le joueur, éteint par certains défis
     * (« Présence », « Absence », « Branche coupée », « Tronc nu ») et doublé par certaines récompenses.
     * Tous les effets de l'arbre passent par lui ; les prix et l'accès suivent le niveau possédé.
     */
    private int effectLevel(DarkUpgrade dark) {
        int level = state.darkLevelOf(dark.id());
        if (level == 0) return 0;
        Style style = darkStyle(dark);
        // « Présence » et « Absence » (corsés en 0.73) : seules agissent les voies et les accords du style, tout le
        // reste de l'arbre est éteint. Avec les bonnes voies, le défi reste court ; sans, c'est presque un premier acte.
        if (ruled(Challenge.Rule.PRESENCE_ONLY) && dark.branch() != DarkUpgrade.Branch.DARK_MATTER | dark.isLink()
                && style != Style.PRESENCE) return 0;
        if (ruled(Challenge.Rule.ABSENCE_ONLY) && dark.branch() != DarkUpgrade.Branch.DARK_MATTER | dark.isLink()
                && style != Style.ABSENCE) return 0;
        if (ruled(Challenge.Rule.CUT_BRANCH) && dark.branch() == DarkUpgrade.Branch.PARTICLES) return 0;
        if (ruled(Challenge.Rule.BARE_TREE) && (dark.isFork() || dark.isLink())) return 0;
        if (style == Style.PRESENCE && rewarded(Challenge.Reward.STRONGER_PRESENCE)) level *= 2;
        if (style == Style.ABSENCE && rewarded(Challenge.Reward.STRONGER_ABSENCE)) level *= 2;
        if (dark.isLink() && rewarded(Challenge.Reward.STRONGER_LINKS)) level *= 2;
        return level;
    }

    /**
     * Les deux styles de l'arbre (version 0.70). Dans chaque bifurcation, la voie de gauche est celle
     * de la présence : la plus forte tant que le joueur est là. Celle de droite est celle de l'absence :
     * un peu moins forte, mais chacune rend {@link #ABSENCE_VOIE_BONUS} de plus du temps passé jeu
     * fermé ({@link #offlineRate()}). Un accord a le style de ses voies.
     */
    public enum Style { PRESENCE, ABSENCE, NONE }

    /** Points de temps d'absence rejoué en plus que rend chaque voie d'absence prise : cinq. */
    public static final double ABSENCE_VOIE_BONUS = 0.05;

    /** Le style d'une case : celui de sa place dans sa bifurcation, ou de toutes les voies d'un accord. */
    public Style darkStyle(DarkUpgrade dark) {
        if (dark.isFork()) return darkForkOptions(dark.fork()).indexOf(dark) == 0 ? Style.PRESENCE : Style.ABSENCE;
        if (dark.isAccord()) {
            Style style = null;
            for (String id : dark.linkedIds()) {
                Style each = darkStyle(darkUpgrade(id));
                if (style == null) style = each;
                else if (style != each) return Style.NONE;
            }
            return style == null ? Style.NONE : style;
        }
        return Style.NONE;
    }

    /** Nombre de voies de ce style que possède le joueur. */
    public int darkVoiesOwned(Style style) {
        int count = 0;
        for (DarkUpgrade dark : darkUpgrades.values()) {
            if (dark.isFork() && darkStyle(dark) == style && state.darkLevelOf(dark.id()) > 0) count++;
        }
        return count;
    }

    /** Ce que l'arbre ajoute à la part du temps d'absence rejouée : les voies d'absence, et l'accord d'absence. */
    public double darkOfflineBonus() {
        return this.<Double>darkMemo("offlineBonus", () -> {
            double bonus = 0;
            for (DarkUpgrade dark : darkUpgrades.values()) {
                int level = effectLevel(dark);
                if (level == 0) continue;
                if (dark.isFork() && darkStyle(dark) == Style.ABSENCE) bonus += ABSENCE_VOIE_BONUS * level;
                if (dark.effect() instanceof DarkEffect.OfflineBonus more) bonus += more.points() * level;
            }
            return bonus;
        });
    }

    /**
     * Somme, sur les améliorations d'un type, de {@code par unité × niveau × matière noire gagnée} :
     * la forme commune des effets payés en matière noire.
     */
    private double darkMatterScaling(String key, java.util.function.ToDoubleFunction<DarkEffect> perUnit) {
        double total = this.<Double>darkMemo(key, () -> {
            double sum = 0;
            for (DarkUpgrade dark : darkUpgrades.values()) {
                int level = effectLevel(dark);
                if (level > 0) sum += perUnit.applyAsDouble(dark.effect()) * level;
            }
            return sum;
        });
        return total == 0 ? 0 : total * darkMatterUnits();
    }

    /** Ce par quoi les améliorations payées en matière noire multiplient les atomes de chaque fusion. */
    public double darkAtomsMultiplier() {
        return 1 + darkMatterScaling("atomsByDarkMatter", effect -> effect instanceof DarkEffect.AtomsByDarkMatter by ? by.perUnit() : 0);
    }

    /** Ce par quoi les améliorations payées en matière noire divisent le délai des automatismes ordinaires. */
    public double darkAutomationDivisor() {
        return 1 + darkMatterScaling("automationsByDarkMatter", effect -> effect instanceof DarkEffect.AutomationsByDarkMatter by ? by.perUnit() : 0);
    }

    /** Ce par quoi les améliorations payées en matière noire divisent le prix de la synthèse. */
    public double darkSynthesisDivisor() {
        return 1 + darkMatterScaling("synthesisByDarkMatter", effect -> effect instanceof DarkEffect.SynthesisByDarkMatter by ? by.perUnit() : 0);
    }

    /**
     * Atomes déjà comptés comme créés au départ de chaque partie, grâce aux améliorations payées
     * en matière noire : jamais plus que {@link #UNLOCK_TOTAL_ATOMS}.
     */
    public BigNum darkHeadStartAtoms() {
        if (inChallenge()) return BigNum.ZERO;
        double atoms = Math.floor(darkMatterScaling("headStart",
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
        return darkMemo(type, () -> {
            for (DarkUpgrade dark : darkUpgrades.values()) {
                if (type.isInstance(dark.effect()) && effectLevel(dark) > 0) return true;
            }
            return false;
        });
    }

    /** Les cases possédées dont l'effet sur les particules suit la matière noire ou sa taille : peu nombreuses, parcourues à chaque calcul. */
    private List<DarkUpgrade> darkScaled() {
        return darkMemo("scaledParticles", () -> darkUpgrades.values().stream()
                .filter(dark -> effectLevel(dark) > 0
                        && (dark.effect() instanceof DarkEffect.ParticlesByDarkMatter || dark.effect() instanceof DarkEffect.ParticlesByLightYears))
                .toList());
    }

    // Les effets de l'arbre qui ne dépendent que de ses niveaux : calculés une fois par version des
    // niveaux. Sans ce cache, chaque appel parcourait tout l'arbre, des milliers de fois par seconde
    // de jeu une fois les automatismes rapides : c'était le gros du temps du rattrapage d'absence.
    private final Map<Object, Object> darkMemo = new HashMap<>();
    private long darkMemoVersion = Long.MIN_VALUE;

    @SuppressWarnings("unchecked")
    private <T> T darkMemo(Object key, java.util.function.Supplier<T> compute) {
        // Depuis la version 0.70, les défis et leurs récompenses changent ce que valent les cases : ils comptent aussi.
        long stamp = state.darkVersion() * 1_000_003L + state.challengesVersion() * 31L
                + (state.activeChallenge() == null ? 0 : state.activeChallenge().hashCode());
        if (darkMemoVersion != stamp) {
            darkMemo.clear();
            darkMemoVersion = stamp;
        }
        Object value = darkMemo.get(key);
        if (value == null) {
            value = compute.get();
            darkMemo.put(key, value);
        }
        return (T) value;
    }

    /** Atomes de base ajoutés à chaque fusion par l'arbre. */
    public double darkAtomsPerFusion() {
        return darkMemo("atomsPerFusion", () -> {
            double added = 0;
            for (DarkUpgrade dark : darkUpgrades.values()) {
                if (dark.effect() instanceof DarkEffect.AddAtomsPerFusion add) {
                    added += add.perLevel() * effectLevel(dark);
                }
            }
            return added;
        });
    }

    /** Nombre d'éléments tirés à chaque synthèse, avant le tirage double : un, plus ceux de l'arbre. */
    public int elementsPerSynthesis() {
        int count = 1 + this.<Integer>darkMemo("elementsPerSynthesis", () -> {
            int added = 0;
            for (DarkUpgrade dark : darkUpgrades.values()) {
                if (dark.effect() instanceof DarkEffect.AddElementsPerSynthesis add) {
                    added += add.perLevel() * effectLevel(dark);
                }
            }
            return added;
        });
        if (rewarded(Challenge.Reward.EXTRA_ELEMENT)) count += REWARD_EXTRA_ELEMENTS;
        return count;
    }

    /**
     * Ce que l'arbre multiplie dans les particules de chaque création : le multiplicateur de base,
     * et la taille de la matière noire en années-lumière (à partir d'une année-lumière).
     */
    public BigNum darkParticlesMultiplier() {
        BigNum result = this.<BigNum>darkMemo("baseParticles", () -> {
            BigNum base = BigNum.ONE;
            for (DarkUpgrade dark : darkUpgrades.values()) {
                int level = effectLevel(dark);
                if (level > 0 && dark.effect() instanceof DarkEffect.MultiplyBaseParticles multiply) {
                    base = base.multiply(BigNum.of(multiply.perLevel()).pow(level));
                }
            }
            return base;
        });
        for (DarkUpgrade dark : darkScaled()) {
            int level = effectLevel(dark);
            if (level == 0) continue;
            if (dark.effect() instanceof DarkEffect.ParticlesByDarkMatter byDarkMatter) {
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
        return darkMemo("expansion", () -> {
            BigNum result = BigNum.ONE;
            for (DarkUpgrade dark : darkUpgrades.values()) {
                if (dark.effect() instanceof DarkEffect.MultiplyExpansion multiply) {
                    result = result.multiply(BigNum.of(multiply.perLevel()).pow(effectLevel(dark)));
                }
            }
            return result;
        });
    }

    /** Part de la vitesse d'appui à laquelle la matière noire grossit seule (0 sans l'amélioration). */
    public double darkAutoExpansionShare() {
        return darkMemo("autoExpansion", () -> {
            double share = 0;
            for (DarkUpgrade dark : darkUpgrades.values()) {
                int level = effectLevel(dark);
                if (dark.effect() instanceof DarkEffect.AutoExpansion auto && level > 0) {
                    share += auto.share() * Math.pow(auto.growth(), level - 1);
                }
            }
            return share;
        });
    }

    /** Secondes d'appui que vaut chaque fusion pour la matière noire (0 sans l'amélioration). */
    public double darkFusionPulse() {
        return darkMemo("fusionPulse", () -> {
            double seconds = 0;
            for (DarkUpgrade dark : darkUpgrades.values()) {
                if (dark.effect() instanceof DarkEffect.FusionPulse pulse) {
                    seconds += pulse.seconds() * effectLevel(dark);
                }
            }
            return seconds;
        });
    }

    /** Niveaux de vitesse offerts par l'arbre : ils s'ajoutent à ceux achetés et ne se perdent jamais. */
    public int startingSpeedLevels() {
        if (inChallenge()) return 0;
        return this.<Integer>darkMemo("startingSpeedLevels", () -> {
            int levels = 0;
            for (DarkUpgrade dark : darkUpgrades.values()) {
                if (dark.effect() instanceof DarkEffect.StartingSpeedLevels start) {
                    levels += start.perLevel() * effectLevel(dark);
                }
            }
            return levels;
        });
    }

    /** Rang de la synthèse qui donne un élément unique à coup sûr, tant qu'aucun n'est sorti. */
    public int guaranteedUniqueSynthesis() {
        return darkMemo("guaranteedUnique", () -> {
            int rank = GUARANTEED_UNIQUE_SYNTHESIS;
            for (DarkUpgrade dark : darkUpgrades.values()) {
                if (dark.effect() instanceof DarkEffect.EarlierGuaranteedUnique earlier && effectLevel(dark) > 0) {
                    rank = Math.min(rank, earlier.rank());
                }
            }
            return rank;
        });
    }

    /**
     * Nombre maximal d'exemplaires d'un élément de cette famille, arbre de matière noire compris.
     * Les familles uniques restent à un exemplaire ; pour les autres, c'est la force maximale
     * (la racine du maximum) qui gagne un cran par niveau : 9 → 16 → 25.
     */
    public int maxCopiesOf(ElementCategory category) {
        if (category.unique()) return 1;
        int steps = this.<Integer>darkMemo("copySteps", () -> {
            int sum = 0;
            for (DarkUpgrade dark : darkUpgrades.values()) {
                if (dark.effect() instanceof DarkEffect.IncreaseMaxCopies more) {
                    sum += more.perLevel() * effectLevel(dark);
                }
            }
            return sum;
        });
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
        double slowdown = firstRunSlowdown();
        if (slowdown > 1) speed = speed.divide(slowdown);
        return speed;
    }

    /** Pour les tests de rythme : remet (ou coupe, avec 1) le premier élan, quelle que soit la propriété système. */
    void setFirstRunPace(double pace) {
        firstRunPace = Math.max(1, pace);
    }

    /**
     * Ce par quoi la création est divisée en ce moment par le premier élan : {@link #FIRST_RUN_PACE}
     * avant le premier atome, {@link #FIRST_ATOMS_PACE} jusqu'au cinquième, {@link #FIRST_GAME_PACE}
     * jusqu'au trentième, 1 ensuite et dès qu'une explosion a eu lieu.
     */
    public double firstRunSlowdown() {
        if (firstRunPace <= 1 || state.stats().timedExplosions() > 0 || state.explosions() > 0) return 1;
        if (isFirstRun()) return FIRST_RUN_PACE;
        if (state.stats().atomsCreated().lt(BigNum.of(FIRST_ATOMS_UNTIL))) return FIRST_ATOMS_PACE;
        if (!hasCreatedEnoughAtoms()) return FIRST_GAME_PACE;
        return 1;
    }

    /** Vrai avant la toute première fusion de la partie : la création n'a pas encore son élan (version 0.95). */
    public boolean isFirstRun() {
        return state.stats().fusions() == 0;
    }

    /** Vitesse d'un générateur (numéroté à partir de 0), en créations par seconde. */
    public BigNum speed(int generator) {
        double second = regimeMultiplier() * synergyMultiplier();
        BigNum speed = speed().multiply(generatorSpeedMultiplier(generator));
        return second == 1 ? speed : speed.multiply(second);
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
        return speed.milestoneFactor() + (rewarded(Challenge.Reward.STRONGER_MILESTONES) ? REWARD_MILESTONE_EXTRA : 0)
                + darkMilestoneExtra();
    }

    /** Ce que l'arbre ajoute au facteur de chaque palier de vitesse (version 0.73) : les voies de présence de l'élan. */
    public double darkMilestoneExtra() {
        return this.<Double>darkMemo("milestoneExtra", () -> {
            double extra = 0;
            for (DarkUpgrade dark : darkUpgrades.values()) {
                if (dark.effect() instanceof DarkEffect.StrongerMilestones more) extra += more.extra() * effectLevel(dark);
            }
            return extra;
        });
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
        int bonus = this.<Integer>darkMemo("startingGenerators", () -> {
            int sum = 0;
            for (DarkUpgrade dark : darkUpgrades.values()) {
                if (dark.effect() instanceof DarkEffect.StartingGenerators start) {
                    sum += start.perLevel() * effectLevel(dark);
                }
            }
            return sum;
        });
        return Math.min(bonus, generatorsPerAtom() - 1);
    }

    /**
     * Nombre maximal de générateurs : celui qu'il faut pour fusionner, ou la limite repoussée par
     * l'arbre de matière noire.
     */
    public int maxGeneratorCount() {
        int uncapped = this.<Integer>darkMemo("maxGenerators", () -> {
            int limit = 0;
            for (DarkUpgrade dark : darkUpgrades.values()) {
                if (dark.effect() instanceof DarkEffect.UncapGenerators uncap && effectLevel(dark) > 0) {
                    limit = Math.max(limit, uncap.limit());
                }
            }
            return limit;
        });
        return Math.max(generatorsPerAtom(), uncapped);
    }

    /** Générateurs du catalogue (le premier compris) : le catalogue ne change pas, on ne les compte qu'une fois. */
    private int catalogGenerators = 0;

    /**
     * Nombre de générateurs qu'il faut pour fusionner, et que consomme chaque atome de base :
     * le premier, plus tous ceux des améliorations du catalogue (dix dans le jeu).
     */
    public int generatorsPerAtom() {
        if (catalogGenerators == 0) {
            int count = 1;
            for (Upgrade upgrade : upgrades.values()) {
                if (upgrade.effect() instanceof Effect.AddGenerator && upgrade.hasLimit()) {
                    count += upgrade.maxLevel();
                }
            }
            catalogGenerators = count;
        }
        int count = catalogGenerators;
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
                .multiply(speedMilestoneMultiplier())
                .multiply(colliderParticlesMultiplier())
                .multiply(bangAchievementMultiplier(BangAchievement.Reward.PARTICLES))
                .multiply(perfectCycleMultiplier());
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
                    BigNum.of(1 + coupling.perGenerator() * level * (inTrial(idle.core.universe.Trial.LONE_GENERATOR)
                            ? 2 * (generatorsPerAtom() - 1) : Math.max(0, generators - 1)));
            // Les paliers de vitesse sont comptés à part : ils tiennent compte des niveaux offerts.
            case Effect.MultiplySpeed speed -> BigNum.ONE;
            case Effect.AddGenerator generator -> BigNum.ONE;
            case Effect.StrengthenSpeed strengthen -> BigNum.ONE;
            case Effect.DiscountGenerators discount -> BigNum.ONE;
            case Effect.KeepUpgradesOnFusion keep -> BigNum.ONE;
            case Effect.BulkAtomBuy bulk -> BigNum.ONE;
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
                .multiply(colliderAtomsMultiplier())
                .multiply(bangAchievementMultiplier(BangAchievement.Reward.ATOMS))
                .multiply(fusionYield())
                .multiply(fusionGroups() * fusionGroupBonus())
                .multiply(cycleChoice() == idle.core.universe.CycleChoice.FUSION ? 1 + idle.core.universe.CycleChoice.FUSION_GAIN : 1)
                .multiply(contractionAtomsMultiplier());
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
        noteHeldPeaks();
        state.stats().noteProduction(production);
        // La production va retomber : l'historique retient le sommet qu'elle vient d'atteindre.
        state.stats().history().notePeak(production.log10());
        state.stats().runHistory().notePeak(production.log10());
        BigNum gained = atomsPerFusion();
        noteFusionFeats();
        state.stats().noteFusion(gained, state.timeSinceFusion());
        // La toute première fusion du jeu : une partie reprise d'une ancienne sauvegarde ne la date pas après coup.
        if (state.stats().fusions() == 1) noteStep(GameStats.Step.FIRST_FUSION);
        BigNum atoms = state.atoms().add(gained);
        state.setAtoms(isAtomCapLifted() ? atoms : atoms.min(atomCap()));
        notePerfectCycle(!isAtomCapLifted() && atoms.gte(atomCap()));
        // Les skins (version 0.88) : la reconstruction se chronomètre, la contraction compte ses fusions.
        if (isSkinsUnlocked()) state.setRebuildPending(true);
        if (inTrial(idle.core.universe.Trial.SOFT_CONTRACTION)) state.setTrialCount(state.trialCount() + 1);
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
        // L'épreuve du premier générateur seul (version 0.88) : pas d'autre générateur.
        if (upgrade.effect() instanceof Effect.AddGenerator && inTrial(idle.core.universe.Trial.LONE_GENERATOR)) return false;
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
    // ------------------------------------------------------------------
    // Les sondes (version 0.52) : des voyages dont le butin se réclame d'un clic
    // ------------------------------------------------------------------

    /** Secondes d'expansion que vaut une récompense d'espace de valeur 1 (un saut court). */
    public static final double PROBE_SPACE_SECONDS = 120;
    /** Exemplaires d'élément que vaut une récompense d'éléments de valeur 1. */
    public static final double PROBE_ELEMENTS = 3;
    /** Durée d'un sursaut d'expansion de valeur 1, avant les réserves d'énergie. */
    public static final double PROBE_BOOST_SECONDS = 300;
    /** Ce par quoi un sursaut multiplie l'espace par seconde. */
    public static final double PROBE_BOOST_FACTOR = 2;

    private final List<ProbeReward> returnedProbes = new ArrayList<>();

    /** Vrai une fois les sondes ouvertes : au palier du troisième Big Bang. */
    public boolean isProbesUnlocked() {
        for (BigBangMilestone milestone : BigBangMilestones.DEFAULT) {
            if (!isBigBangMilestoneReached(milestone)) continue;
            for (BigBangMilestone.Effect effect : milestone.effects()) {
                if (effect instanceof BigBangMilestone.Probes) return true;
            }
        }
        return false;
    }

    /** Sondes en vol à la fois : une, plus le hangar. */
    public int probeSlots() {
        int slots = 1 + state.probeUpgradeLevel(ProbeUpgrade.HANGAR);
        for (BigBangMilestone.MorePlaces more : reachedMorePlaces()) slots += more.probes();
        return slots;
    }

    /** Vrai si cette destination est ouverte : le vide toujours, les autres avec leur rayon. */
    public boolean isProbeDestinationOpen(ProbeDestination destination) {
        return destination.kind() == null || isMoleculeKindUnlocked(destination.kind());
    }

    /** Durée d'un voyage, moteurs compris. */
    public double probeDuration(ProbeTrip trip) {
        return trip.seconds() * Math.pow(0.9, state.probeUpgradeLevel(ProbeUpgrade.ENGINE));
    }

    /** Nombre de récompenses que rapporte ce voyage, soute comprise ; une de plus avec le charme, à partir d'une heure. */
    public int probeRewardCount(ProbeTrip trip) {
        if (trip.isUrgent()) return 1;      // un seul petit lot, ni soute ni charme
        return trip.rewards() + state.probeUpgradeLevel(ProbeUpgrade.CARGO)
                + (colliderUpgradeLevel(ColliderUpgrade.FLAVOR_CHARM) > 0 && trip.ordinal() >= ProbeTrip.HOUR.ordinal() ? 1 : 0);
    }

    /** Ce que valent les récompenses de ce voyage : sa valeur, instruments compris. */
    public double probeValue(ProbeTrip trip) {
        return trip.value() * (1 + 0.25 * state.probeUpgradeLevel(ProbeUpgrade.LOOT));
    }

    public boolean canLaunchProbe(ProbeDestination destination, ProbeTrip trip) {
        if (trip.isUrgent()) return canLaunchUrgentWave();
        return isProbesUnlocked() && isProbeDestinationOpen(destination) && state.probes().size() < probeSlots();
    }

    /** Big Bangs qu'il faut pour l'onde d'urgence chimique. */
    public static final int URGENT_WAVE_BIG_BANGS = 3;

    /** Vrai une fois l'onde d'urgence chimique ouverte : les sondes, et le troisième Big Bang. */
    public boolean isUrgentWaveUnlocked() {
        return isProbesUnlocked() && state.bigBangs() >= URGENT_WAVE_BIG_BANGS;
    }

    /** Vrai si l'onde d'urgence peut partir : ouverte, une place libre, et un élément découvert à rapporter. */
    public boolean canLaunchUrgentWave() {
        return isUrgentWaveUnlocked() && state.probes().size() < probeSlots() && urgentElement() > 0;
    }

    /** Envoie l'onde d'urgence chimique. */
    public boolean launchUrgentWave() {
        return launchProbe(ProbeDestination.VOID, ProbeTrip.URGENT);
    }

    /**
     * L'élément que l'astre visé demande le plus : pour chaque molécule qui lui manque, ses
     * exemplaires de chaque élément, fois le nombre de molécules qui manquent. Seuls comptent les
     * éléments déjà découverts ; à égalité, le plus petit numéro. 0 s'il n'y en a pas.
     */
    public int urgentElement() {
        Map<Integer, Long> demand = new java.util.TreeMap<>();
        for (ShoppingList.Item item : shoppingList().items()) {
            long missing = Math.max(item.missing(), item.gather() ? 1 : 0);
            if (missing <= 0) continue;
            for (Map.Entry<Integer, Integer> part : item.molecule().recipe().entrySet()) {
                if (state.elementCount(part.getKey()) > 0) demand.merge(part.getKey(), missing * part.getValue(), Long::sum);
            }
        }
        int best = 0;
        long most = 0;
        for (Map.Entry<Integer, Long> entry : demand.entrySet()) {
            if (entry.getValue() > most) {
                best = entry.getKey();
                most = entry.getValue();
            }
        }
        return best;
    }

    /** Envoie une sonde. */
    public boolean launchProbe(ProbeDestination destination, ProbeTrip trip) {
        if (!canLaunchProbe(destination, trip)) return false;
        List<Probe> flying = new ArrayList<>(state.probes());
        double seconds = probeDuration(trip);
        long id = state.probeLaunches();
        state.setProbeLaunches(id + 1);
        flying.add(new Probe(destination, trip, seconds, seconds, id));
        state.setProbes(flying);
        return true;
    }

    /** Les sondes arrivées dont le butin attend, dans l'ordre de leur départ. */
    public List<Probe> landedProbes() {
        List<Probe> landed = new ArrayList<>();
        for (Probe probe : state.probes()) if (probe.landed()) landed.add(probe);
        return landed;
    }

    /** Le butin qu'il reste à ouvrir de cette sonde, dans l'ordre. */
    public List<ProbeReward> probeRewardsOf(long probe) {
        List<ProbeReward> rewards = new ArrayList<>();
        for (ProbeReward reward : state.probeRewards()) if (reward.probe() == probe) rewards.add(reward);
        return rewards;
    }

    /** Ouvre la prochaine récompense de cette sonde ; la dernière ouverte, la sonde rentre au hangar. */
    public ProbeReward claimProbeReward(long probe) {
        for (int index = 0; index < state.probeRewards().size(); index++) {
            if (state.probeRewards().get(index).probe() == probe) return claimProbeRewardAt(index);
        }
        dropEmptyProbes();
        return null;
    }

    /** Ouvre d'un coup tout le butin de cette sonde : les récompenses ouvertes, dans l'ordre. */
    public List<ProbeReward> claimAllProbeRewards(long probe) {
        List<ProbeReward> opened = new ArrayList<>();
        for (ProbeReward reward = claimProbeReward(probe); reward != null; reward = claimProbeReward(probe)) opened.add(reward);
        return opened;
    }

    /** Les sondes arrivées dont tout le butin est ouvert rentrent au hangar : leur place se libère. */
    private void dropEmptyProbes() {
        boolean any = false;
        for (Probe probe : state.probes()) if (probe.landed() && probeRewardsOf(probe.id()).isEmpty()) any = true;
        if (!any) return;
        List<Probe> kept = new ArrayList<>();
        for (Probe probe : state.probes()) if (!probe.landed() || !probeRewardsOf(probe.id()).isEmpty()) kept.add(probe);
        state.setProbes(kept);
    }

    private void runProbes(double dt) {
        if (dt <= 0) return;
        if (state.probeBoostLeft() > 0) state.setProbeBoostLeft(state.probeBoostLeft() - dt);
        if (state.probes().isEmpty()) return;
        List<Probe> flying = new ArrayList<>();
        List<ProbeReward> waiting = new ArrayList<>(state.probeRewards());
        boolean landed = false;
        for (Probe probe : state.probes()) {
            if (probe.landed()) {
                // Arrivée : elle attend qu'on ouvre son butin. Sans numéro (une sauvegarde d'avant), elle rentre aussitôt.
                if (probe.id() >= 0) flying.add(probe);
                continue;
            }
            double left = probe.left() - dt;
            if (left > 0) {
                flying.add(new Probe(probe.destination(), probe.trip(), left, probe.total(), probe.id()));
            } else {
                List<ProbeReward> loot = probeLoot(probe);
                waiting.addAll(loot);
                returnedProbes.add(loot.isEmpty() ? null : loot.get(0));
                landed = true;
                if (probe.id() >= 0) flying.add(new Probe(probe.destination(), probe.trip(), 0, probe.total(), probe.id()));
            }
        }
        state.setProbes(flying);
        if (landed) state.setProbeRewards(waiting);
    }

    /** Le butin d'une sonde qui revient : fixé par le numéro du voyage, sans toucher au hasard du jeu. */
    private List<ProbeReward> probeLoot(Probe probe) {
        long run = state.probeRuns();
        state.setProbeRuns(run + 1);
        double value = probeValue(probe.trip());
        ProbeDestination where = probe.destination();
        // L'onde d'urgence (version 0.86) : un seul lot, de l'élément le plus demandé à son retour ; rien s'il n'y en a plus.
        if (probe.trip().isUrgent()) {
            int element = urgentElement();
            return element > 0 ? List.of(new ProbeReward(ProbeReward.Type.ELEMENTS, String.valueOf(element), value, where, probe.id()))
                    : List.of(new ProbeReward(ProbeReward.Type.SAMPLES, "", value, where, probe.id()));
        }
        List<Integer> elements = probeElements(where);
        int count = probeRewardCount(probe.trip());
        boolean quarks = colliderUpgradeLevel(ColliderUpgrade.FLAVOR_CHARM) > 0 && probe.trip().ordinal() >= ProbeTrip.HOUR.ordinal();
        List<ProbeReward> loot = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            if (i == count - 1) {
                loot.add(new ProbeReward(ProbeReward.Type.SAMPLES, "", value, where, probe.id()));
            } else if (quarks && i == count - 2) {
                loot.add(new ProbeReward(ProbeReward.Type.QUARKS, "", value, where, probe.id()));
            } else if (i == 0) {
                loot.add(elements.isEmpty() ? new ProbeReward(ProbeReward.Type.SPACE, "", value, where, probe.id())
                        : new ProbeReward(ProbeReward.Type.ELEMENTS, String.valueOf(elements.get(between(run, 10, 0, elements.size() - 1))), value, where, probe.id()));
            } else {
                int roll = between(run, 20 + i, 0, elements.isEmpty() ? 1 : 2);
                if (roll == 2) {
                    loot.add(new ProbeReward(ProbeReward.Type.ELEMENTS, String.valueOf(elements.get(between(run, 30 + i, 0, elements.size() - 1))), value, where, probe.id()));
                } else {
                    loot.add(new ProbeReward(roll == 0 ? ProbeReward.Type.SPACE : ProbeReward.Type.BOOST, "", value, where, probe.id()));
                }
            }
        }
        return loot;
    }

    /**
     * Les éléments que peut rapporter une destination : ceux des molécules de son rayon, déjà
     * découverts dans la partie, par ordre de numéro. Aucun pour le vide.
     */
    public List<Integer> probeElements(ProbeDestination destination) {
        if (destination.kind() == null) return List.of();
        java.util.TreeSet<Integer> found = new java.util.TreeSet<>();
        for (Molecule molecule : molecules()) {
            if (molecule.kind() != destination.kind()) continue;
            for (Integer element : molecule.recipe().keySet()) {
                if (state.elementCount(element) > 0) found.add(element);
            }
        }
        return new ArrayList<>(found);
    }

    /** La prochaine récompense à réclamer, ou {@code null}. */
    public ProbeReward nextProbeReward() {
        return state.probeRewards().isEmpty() ? null : state.probeRewards().get(0);
    }

    /** Exemplaires d'élément que donne cette récompense. */
    public int probeElementsOf(ProbeReward reward) {
        return (int) Math.ceil(PROBE_ELEMENTS * reward.value());
    }

    /** Espace que donne cette récompense, à l'expansion du moment. */
    public BigNum probeSpaceOf(ProbeReward reward) {
        return steadySpacePerSecond().multiply(PROBE_SPACE_SECONDS * reward.value());
    }

    /** Secondes de sursaut que donne cette récompense, réserves d'énergie comprises. */
    public double probeBoostOf(ProbeReward reward) {
        return PROBE_BOOST_SECONDS * Math.sqrt(reward.value()) * (1 + 0.5 * state.probeUpgradeLevel(ProbeUpgrade.BOOST));
    }

    /** Échantillons que donne cette récompense. */
    public long probeSamplesOf(ProbeReward reward) {
        return Math.max(1, Math.round(2 * Math.pow(reward.value(), 0.8)));
    }

    /** Quarks charmés que donne cette récompense ; les quarks beaux sont la moitié, arrondie au-dessous. */
    public int probeCharmOf(ProbeReward reward) {
        return Math.max(1, (int) Math.round(Math.pow(reward.value(), 0.5)));
    }

    /**
     * Réclame la prochaine récompense : elle s'applique, et la suivante prend sa place. Un élément
     * qui n'est plus découvert (un Big Bang a eu lieu entre-temps) devient de l'espace.
     *
     * @return la récompense réclamée, ou {@code null} s'il n'y en avait pas
     */
    public ProbeReward claimProbeReward() {
        if (state.probeRewards().isEmpty()) {
            dropEmptyProbes();
            return null;
        }
        return claimProbeRewardAt(0);
    }

    private ProbeReward claimProbeRewardAt(int index) {
        ProbeReward reward = state.probeRewards().get(index);
        List<ProbeReward> waiting = new ArrayList<>(state.probeRewards());
        waiting.remove(index);
        state.setProbeRewards(waiting);
        dropEmptyProbes();
        ProbeReward.Type type = reward.type();
        if (type == ProbeReward.Type.ELEMENTS) {
            int element = Integer.parseInt(reward.target());
            if (state.elementCount(element) > 0) {
                state.setElementCount(element, state.elementCount(element) + probeElementsOf(reward));
                return reward;
            }
            reward = new ProbeReward(ProbeReward.Type.SPACE, "", reward.value(), reward.destination());
            type = ProbeReward.Type.SPACE;
        }
        switch (type) {
            case SPACE -> {
                if (isBigBangUnlocked()) state.setSpace(state.space().add(probeSpaceOf(reward)));
            }
            case BOOST -> state.setProbeBoostLeft(state.probeBoostLeft() + probeBoostOf(reward));
            case SAMPLES -> state.setProbeSamples(state.probeSamples() + probeSamplesOf(reward));
            case QUARKS -> {
                int charm = probeCharmOf(reward);
                state.setQuarks(Quark.CHARM, state.quarks(Quark.CHARM) + charm);
                state.setQuarks(Quark.BOTTOM, state.quarks(Quark.BOTTOM) + charm / 2);
            }
            default -> { }
        }
        return reward;
    }

    /** Ce par quoi un sursaut d'expansion multiplie l'espace par seconde. */
    public double probeBoostMultiplier() {
        return state.probeBoostLeft() > 0 ? PROBE_BOOST_FACTOR : 1;
    }

    public long probeUpgradeCost(ProbeUpgrade upgrade) {
        return upgrade.cost(state.probeUpgradeLevel(upgrade));
    }

    public boolean canBuyProbeUpgrade(ProbeUpgrade upgrade) {
        int level = state.probeUpgradeLevel(upgrade);
        return isProbesUnlocked() && level < upgrade.maxLevel() && state.probeSamples() >= upgrade.cost(level);
    }

    public boolean buyProbeUpgrade(ProbeUpgrade upgrade) {
        if (!canBuyProbeUpgrade(upgrade)) return false;
        state.setProbeSamples(state.probeSamples() - probeUpgradeCost(upgrade));
        state.setProbeUpgradeLevel(upgrade, state.probeUpgradeLevel(upgrade) + 1);
        return true;
    }

    /** Les sondes revenues depuis le dernier appel, par leur première récompense : pour les annoncer une fois. */
    public List<ProbeReward> takeReturnedProbes() {
        List<ProbeReward> taken = new ArrayList<>(returnedProbes);
        returnedProbes.clear();
        return taken;
    }
    // ------------------------------------------------------------------
    // La maturation des astres (version 0.53) : un astre formé mûrit avant de compter pleinement
    // ------------------------------------------------------------------

    /** Part de son bonus que donne un astre pendant qu'il mûrit. */
    public static final double MATURING_SHARE = 0.25;
    /** Places de maturation : autant d'astres qui mûrissent à la fois. */
    public static final int MATURATION_PLACES = 3;
    /** Ce qu'une charge d'impulsion fait gagner à une maturation. */
    public static final double MATURATION_HASTE_SECONDS = 60;

    private final List<String> maturedBodies = new ArrayList<>();

    /**
     * Durée de maturation d'un astre, selon son échelle : 4 minutes pour un amas de roches, 25
     * pour une planète, 50 pour une étoile, 2 heures pour le trou noir supermassif.
     */
    public double maturationSeconds(Body body) {
        double minutes = switch (body.tier()) {
            case RUBBLE -> 4;
            case COMET -> 6;
            case ASTEROID -> 10;
            case MOON -> 15;
            case PLANET -> 25;
            case STAR -> 50;
            case REMNANT -> 70;
            case BLACK_HOLE -> 90;
            case EXOTIC -> 100;
            case CORE -> 120;
        };
        return minutes * 60;
    }

    /** Vrai si cet astre est formé et a fini de mûrir : il compte pour les astres plus gros et pour la galaxie. */
    public boolean isBodyMature(String bodyId) {
        String id = body(bodyId).id();
        return state.hasBody(id) && state.maturingLeft(id) <= 0;
    }

    /** Vrai si cet astre est formé mais mûrit encore. */
    public boolean isBodyMaturing(String bodyId) {
        return state.maturingLeft(body(bodyId).id()) > 0;
    }

    /** Secondes de maturation qu'il reste à cet astre. */
    public double maturationLeft(String bodyId) {
        return state.maturingLeft(body(bodyId).id());
    }

    /** Nombre d'astres mûrs. */
    public int bodiesMature() {
        return state.bodies().size() - state.maturing().size();
    }

    /** Places de maturation. */
    public int maturationPlaces() {
        int places = MATURATION_PLACES;
        for (BigBangMilestone.MorePlaces more : reachedMorePlaces()) places += more.maturation();
        return places;
    }

    /** Les paliers « des places de plus » atteints. */
    private List<BigBangMilestone.MorePlaces> reachedMorePlaces() {
        List<BigBangMilestone.MorePlaces> found = new ArrayList<>();
        for (BigBangMilestone milestone : BigBangMilestones.DEFAULT) {
            if (!isBigBangMilestoneReached(milestone)) continue;
            for (BigBangMilestone.Effect effect : milestone.effects()) {
                if (effect instanceof BigBangMilestone.MorePlaces more) found.add(more);
            }
        }
        return found;
    }

    /** Vrai si un palier atteint donne cet effet. */
    private boolean milestoneGives(Class<? extends BigBangMilestone.Effect> kind) {
        for (BigBangMilestone milestone : BigBangMilestones.DEFAULT) {
            if (!isBigBangMilestoneReached(milestone)) continue;
            for (BigBangMilestone.Effect effect : milestone.effects()) {
                if (kind.isInstance(effect)) return true;
            }
        }
        return false;
    }

    /** Vrai une fois les matières exotiques ouvertes, au huitième Big Bang : leurs astres peuvent se former. */
    public boolean isExoticUnlocked() {
        return milestoneGives(BigBangMilestone.ExoticMatter.class);
    }

    /** Vrai s'il reste une place pour faire mûrir un astre de plus. */
    public boolean hasMaturationPlace() {
        return state.maturing().size() < maturationPlaces();
    }

    /** Les astres qui mûrissent, dans l'ordre de leur formation. */
    public List<String> maturingBodies() {
        return List.copyOf(state.maturing().keySet());
    }

    /** Dépense une charge d'impulsion pour avancer d'une minute la maturation de cet astre. */
    public boolean hastenMaturation(String bodyId) {
        String id = body(bodyId).id();
        if (state.maturingLeft(id) <= 0 || state.pulseCharges() < 1 - 1e-9) return false;
        state.setPulseCharges(state.pulseCharges() - 1);
        state.setPulses(state.pulses() + 1);
        state.setMaturing(id, state.maturingLeft(id) - MATURATION_HASTE_SECONDS);
        if (state.maturingLeft(id) <= 0) maturedBodies.add(id);
        return true;
    }

    private void runMaturation(double dt) {
        if (dt <= 0 || state.maturing().isEmpty()) return;
        for (String id : List.copyOf(state.maturing().keySet())) {
            state.setMaturing(id, state.maturingLeft(id) - dt);
            if (state.maturingLeft(id) <= 0) maturedBodies.add(id);
        }
    }

    /** Les astres qui ont fini de mûrir depuis le dernier appel : pour les annoncer une fois. */
    public List<String> takeMaturedBodies() {
        List<String> taken = List.copyOf(maturedBodies);
        maturedBodies.clear();
        return taken;
    }
    // ------------------------------------------------------------------
    // Les systèmes solaires (version 0.54) : une étoile, ses planètes, ses lunes
    // ------------------------------------------------------------------

    /** Systèmes solaires complets que demande la galaxie. */
    public static final int SYSTEMS_FOR_GALAXY = 3;
    /** Planètes qu'il faut à un système pour être complet. */
    public static final int SYSTEM_MIN_PLANETS = 2;
    /** Ce que chaque point d'harmonie d'un système complet ajoute à toutes les grandeurs (8 = +800 %). */
    public static final double SYSTEM_GAIN = 8;

    /** Vrai une fois les systèmes solaires ouverts, au sixième Big Bang. */
    public boolean isSolarSystemsUnlocked() {
        return milestoneGives(BigBangMilestone.SolarSystems.class);
    }

    /** Vrai si ce système a assez de planètes. */
    public boolean isSystemComplete(SolarSystem system) {
        return system.planets().size() >= SYSTEM_MIN_PLANETS;
    }

    /** Nombre de systèmes complets. */
    public int solarSystemsComplete() {
        int count = 0;
        for (SolarSystem system : state.solarSystems()) if (isSystemComplete(system)) count++;
        return count;
    }

    /** Vrai si cet astre est déjà dans un système. */
    public boolean isInSystem(String bodyId) {
        for (SolarSystem system : state.solarSystems()) {
            if (system.star().equals(bodyId) || system.planets().contains(bodyId) || system.moons().contains(bodyId)) return true;
        }
        return false;
    }

    /** Les astres mûrs de cette échelle qui ne sont dans aucun système : ce qu'on peut encore y placer. */
    public List<String> freeForSystems(Body.Tier tier) {
        List<String> free = new ArrayList<>();
        for (Body body : bodies.values()) {
            if (body.tier() == tier && isBodyMature(body.id()) && !isInSystem(body.id())) free.add(body.id());
        }
        return free;
    }

    /** Fonde un système autour de cette étoile, mûre et libre. */
    public boolean foundSystem(String starId) {
        Body star = body(starId);
        if (!isSolarSystemsUnlocked() || star.tier() != Body.Tier.STAR || !isBodyMature(star.id()) || isInSystem(star.id())) return false;
        List<SolarSystem> systems = new ArrayList<>(state.solarSystems());
        systems.add(new SolarSystem(star.id(), List.of(), List.of()));
        state.setSolarSystems(systems);
        return true;
    }

    /** Défait un système : ses astres redeviennent libres. */
    public boolean dissolveSystem(int index) {
        if (index < 0 || index >= state.solarSystems().size()) return false;
        List<SolarSystem> systems = new ArrayList<>(state.solarSystems());
        systems.remove(index);
        state.setSolarSystems(systems);
        return true;
    }

    /** Place une planète mûre et libre sur l'orbite la plus lointaine de ce système. */
    public boolean addPlanet(int index, String planetId) {
        Body planet = body(planetId);
        if (index < 0 || index >= state.solarSystems().size() || planet.tier() != Body.Tier.PLANET
                || !isBodyMature(planet.id()) || isInSystem(planet.id())) return false;
        SolarSystem system = state.solarSystems().get(index);
        List<String> planets = new ArrayList<>(system.planets());
        planets.add(planet.id());
        replaceSystem(index, system.withPlanets(planets));
        return true;
    }

    /** Ajoute une lune mûre et libre à ce système : une par planète au plus. */
    public boolean addMoon(int index, String moonId) {
        Body moon = body(moonId);
        if (index < 0 || index >= state.solarSystems().size() || moon.tier() != Body.Tier.MOON
                || !isBodyMature(moon.id()) || isInSystem(moon.id())) return false;
        SolarSystem system = state.solarSystems().get(index);
        if (system.moons().size() >= system.planets().size()) return false;
        List<String> moons = new ArrayList<>(system.moons());
        moons.add(moon.id());
        replaceSystem(index, system.withMoons(moons));
        return true;
    }

    /** Retire une planète ou une lune de ce système ; une lune de trop part avec la planète. */
    public boolean removeFromSystem(int index, String bodyId) {
        if (index < 0 || index >= state.solarSystems().size()) return false;
        SolarSystem system = state.solarSystems().get(index);
        List<String> planets = new ArrayList<>(system.planets());
        List<String> moons = new ArrayList<>(system.moons());
        if (!planets.remove(bodyId) && !moons.remove(bodyId)) return false;
        while (moons.size() > planets.size()) moons.remove(moons.size() - 1);
        replaceSystem(index, new SolarSystem(system.star(), planets, moons));
        return true;
    }

    /** Rapproche (−1) ou éloigne (+1) une planète de son étoile. */
    public boolean movePlanet(int index, String planetId, int direction) {
        if (index < 0 || index >= state.solarSystems().size()) return false;
        SolarSystem system = state.solarSystems().get(index);
        List<String> planets = new ArrayList<>(system.planets());
        int at = planets.indexOf(planetId), to = at + Integer.signum(direction);
        if (at < 0 || to < 0 || to >= planets.size()) return false;
        java.util.Collections.swap(planets, at, to);
        replaceSystem(index, system.withPlanets(planets));
        return true;
    }

    private void replaceSystem(int index, SolarSystem system) {
        List<SolarSystem> systems = new ArrayList<>(state.solarSystems());
        systems.set(index, system);
        state.setSolarSystems(systems);
    }

    /** Une planète rocheuse : roche, lave ou océan ; sa place est près de l'étoile. */
    private static boolean telluric(Body planet) {
        return planet.look() == Body.Look.ROCKY || planet.look() == Body.Look.MOLTEN || planet.look() == Body.Look.OCEAN;
    }

    /** Vrai pour une planète tellurique (roche, lave, océan), dont la place est près de l'étoile. */
    public boolean isTelluric(String planetId) {
        return telluric(body(planetId));
    }

    /**
     * Vrai si la planète de cette orbite est bien placée : une tellurique n'a que des telluriques plus
     * près de l'étoile qu'elle, une glacée ou gazeuse n'a que des glacées ou gazeuses plus loin.
     */
    public boolean isPlanetWellPlaced(SolarSystem system, int orbit) {
        List<String> planets = system.planets();
        if (orbit < 0 || orbit >= planets.size()) return false;
        boolean placed = true;
        if (telluric(body(planets.get(orbit)))) {
            for (int inner = 0; inner < orbit; inner++) placed &= telluric(body(planets.get(inner)));
        } else {
            for (int outer = orbit + 1; outer < planets.size(); outer++) placed &= !telluric(body(planets.get(outer)));
        }
        return placed;
    }

    /** Vrai si l'étoile de ce système a une zone habitable : un Soleil ou une naine rouge, aux orbites 2 et 3. */
    public static boolean hasHabitableZone(SolarSystem system) {
        return system.star().equals("yellow_sun") || system.star().equals("red_dwarf");
    }

    /** Vrai si cette orbite (comptée depuis 0) est dans la zone habitable du système. */
    public static boolean isHabitableOrbit(SolarSystem system, int orbit) {
        return hasHabitableZone(system) && (orbit == 1 || orbit == 2);
    }

    /** Ce qui fait l'harmonie d'un système, une raison par point, pour l'afficher. */
    public List<String> systemHarmonyReasons(SolarSystem system) {
        List<String> reasons = new ArrayList<>();
        List<String> planets = system.planets();
        for (int orbit = 0; orbit < planets.size(); orbit++) {
            Body planet = body(planets.get(orbit));
            if (isPlanetWellPlaced(system, orbit)) {
                reasons.add(planet.name() + (telluric(planet) ? " : tellurique, près de l'étoile" : " : glacée ou gazeuse, au loin"));
            }
            if (planet.look() == Body.Look.OCEAN && isHabitableOrbit(system, orbit)) {
                reasons.add(planet.name() + " : dans la zone habitable (+2)");
                reasons.add("");
            }
        }
        for (String moon : system.moons()) reasons.add(body(moon).name() + " : une lune");
        if (system.star().equals("blue_giant") || system.star().equals("red_giant")) reasons.add(body(system.star()).name() + " : une étoile géante");
        return reasons;
    }

    /**
     * Le meilleur ordre des planètes de ce système (version 0.59) : celui qui donne le plus
     * d'harmonie, et à égalité celui qui bouge le moins de planètes. Les lunes ne changent pas.
     */
    public SolarSystem bestArrangement(SolarSystem system) {
        List<String> planets = system.planets();
        if (planets.size() > 8) return system;
        SolarSystem[] best = {system};
        int[] bestScore = {systemHarmony(system)};
        int[] bestMoves = {0};
        permute(new ArrayList<>(planets), 0, order -> {
            SolarSystem candidate = system.withPlanets(order);
            int score = systemHarmony(candidate);
            int moves = 0;
            for (int i = 0; i < order.size(); i++) if (!order.get(i).equals(planets.get(i))) moves++;
            if (score > bestScore[0] || (score == bestScore[0] && moves < bestMoves[0])) {
                best[0] = candidate;
                bestScore[0] = score;
                bestMoves[0] = moves;
            }
        });
        return best[0];
    }

    private static void permute(List<String> items, int from, java.util.function.Consumer<List<String>> each) {
        if (from >= items.size() - 1) {
            each.accept(List.copyOf(items));
            return;
        }
        for (int i = from; i < items.size(); i++) {
            java.util.Collections.swap(items, from, i);
            permute(items, from + 1, each);
            java.util.Collections.swap(items, from, i);
        }
    }

    /** Vrai si ranger autrement les planètes de ce système lui donnerait plus d'harmonie. */
    public boolean canArrangeSystem(int index) {
        if (index < 0 || index >= state.solarSystems().size()) return false;
        SolarSystem system = state.solarSystems().get(index);
        return systemHarmony(bestArrangement(system)) > systemHarmony(system);
    }

    /** Range les planètes de ce système dans leur meilleur ordre. */
    public boolean arrangeSystem(int index) {
        if (!canArrangeSystem(index)) return false;
        replaceSystem(index, bestArrangement(state.solarSystems().get(index)));
        return true;
    }

    /** Points d'harmonie d'un système : une planète bien placée, une lune, la zone habitable, une géante. */
    public int systemHarmony(SolarSystem system) {
        return systemHarmonyReasons(system).size();
    }
}
